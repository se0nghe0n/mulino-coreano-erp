#!/usr/bin/env python3
"""Observation-level layer route check for case declarations; never a runtime PASS.

The coverage assembler links every assertion of a subcase to every profile the case
declares, so a declared `mcp` or `skills` profile makes an observation "reachable" even
when no linked assertion reads an MCP or skill-loading result. This check looks one level
deeper: for every catalog observation whose oracle requires the MCP or SKILLS layer, at
least one linked assertion must read evidence produced on that layer.

Evidence of a layer (static classification of the declared source, not of runtime bytes):
- MCP: a query/invoke on route mcp or wire; an independent observe that follows an
  invoke on route mcp/wire in the same subcase (effects of an MCP write); an agent/host
  result read under a protocol transcript pointer (toolTranscript, transcript, toolCalls,
  wireTranscripts, protocolTranscript); a host coverage row filtered by profile mcp or
  artifactKind protocol_transcript.
- SKILLS: an agent/host result read under a skill loading pointer (skillLoading, loading,
  referenceReads, loadingObservations, skillDiscoveryStatus, skillBodyStatus); a host
  coverage row filtered by profile skills or artifactKind skill_loading_trace.
- DB (only with --include-db, informational): an observe action, or a host coverage row
  filtered by artifactKind db_snapshot or profile scenarios. The harness already enforces
  db_snapshot attribution (CatalogLinkValidator, sibling observations allowed).

Exceptions are recorded in layer-route-review.json, each with an owner and a reason: EXEMPT
(reviewed: the observation is not about that layer) or KNOWN_OPEN (a real gap of a named owner,
with a closing condition). ./verify prepare runs this check and lists KNOWN_OPEN rows; the coverage
assembler loads review() and keeps every KNOWN_OPEN observation NOT_RUN.

Exit 0: every gap is a recorded entry. Exit 1: an unexplained gap, a malformed entry, or an
entry that no longer matches a gap (the list must be exact, so a closed gap forces its deletion).
"""
import argparse, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
API_ROUTES = {'api', 'odata', 'batch', 'nested', 'projection', 'direct'}
MCP_ROUTES = {'mcp', 'wire'}
MCP_POINTERS = ('/toolTranscript', '/transcript', 'toolCalls', 'wireTranscripts', 'protocolTranscript')
SKILL_POINTERS = ('skillLoading', '/loading', 'referenceReads', 'loadingObservations', 'skillDiscoveryStatus', 'skillBodyStatus')

REVIEW = 'verification/requirements/layer-route-review.json'
REVIEW_STATUSES = ('EXEMPT', 'KNOWN_OPEN')
REQUIRED_ENTRY_FIELDS = ('status', 'oracleId', 'observationName', 'layer', 'owner', 'reason')


def load_review(root):
    """The recorded EXEMPT/KNOWN_OPEN list. Every entry names an owner and a reason; KNOWN_OPEN also a closing condition."""
    review = json.loads((root / REVIEW).read_text())
    entries, problems = {}, []
    if review.get('schemaVersion') != '1.0.0' or review.get('recordType') != 'LAYER_ROUTE_REVIEW' or not isinstance(review.get('entries'), list):
        return entries, ['layer-route review record has an unknown shape']
    for entry in review['entries']:
        if not isinstance(entry, dict) or any(not isinstance(entry.get(k), str) or not entry[k].strip() for k in REQUIRED_ENTRY_FIELDS):
            problems.append(f'layer-route review entry lacks {"/".join(REQUIRED_ENTRY_FIELDS)}: {entry}')
            continue
        if entry['status'] not in REVIEW_STATUSES or entry['layer'] not in ('MCP', 'SKILLS'):
            problems.append(f'layer-route review entry has unknown status/layer: {entry}')
            continue
        if entry['status'] == 'KNOWN_OPEN' and (not isinstance(entry.get('closeWhen'), str) or not entry['closeWhen'].strip()):
            problems.append(f'KNOWN_OPEN entry needs closeWhen: {entry["oracleId"]}/{entry["observationName"]}')
            continue
        key = (entry['oracleId'], entry['observationName'], entry['layer'])
        if key in entries:
            problems.append(f'duplicate layer-route review entry {key}')
        entries[key] = entry
    return entries, problems


def review(root):
    """Classify every MCP/SKILLS gap against the recorded list.

    Returns (rows, problems): rows are (status, caseId, oracleId, observationName, layer, owner) for every gap;
    problems are unexplained gaps, malformed entries and entries that no longer match a gap (a stale allowlist)."""
    entries, problems = load_review(root)
    rows, matched = [], set()
    for case_id, oracle_id, name, layer in gaps(root):
        key = (oracle_id, name, layer)
        entry = entries.get(key)
        if entry is None:
            rows.append(('GAP', case_id, oracle_id, name, layer, None))
            problems.append(f'unexplained layer route gap {case_id} {oracle_id}/{name} {layer}')
        else:
            matched.add(key)
            rows.append((entry['status'], case_id, oracle_id, name, layer, entry['owner']))
    for key in sorted(set(entries) - matched):
        problems.append(f'stale layer-route review entry (no longer a gap; delete it): {key[0]}/{key[1]} {key[2]}')
    return rows, problems


def flatten(actions):
    for action in actions:
        yield action
        if action.get('call'):
            yield from flatten([action['call']])
        for branch in action.get('branches', []):
            yield from flatten(branch['actions'])


def references(node, out):
    if isinstance(node, dict):
        if isinstance(node.get('actionId'), str):
            out.append(node)
        for value in node.values():
            references(value, out)
    elif isinstance(node, list):
        for value in node:
            references(value, out)
    return out


def classifier(subcase):
    actions = list(flatten(subcase['actions']))
    position = {a['id']: i for i, a in enumerate(actions)}
    by_id = {a['id']: a for a in actions}
    mcp_writes = [i for i, a in enumerate(actions) if a['kind'] == 'invoke' and a.get('route') in MCP_ROUTES]

    def layers(source):
        action = by_id.get(source['actionId'])
        found = set()
        if action is None:
            return found
        kind, route = action['kind'], action.get('route')
        text = (source.get('pointer') or '') + json.dumps(source.get('field', ''))
        where = source.get('where') or {}
        if kind in ('query', 'invoke'):
            if route in MCP_ROUTES:
                found.add('MCP')
            if route in API_ROUTES:
                found.add('API')
        if kind == 'observe':
            found.add('DB')
            if any(i < position[action['id']] for i in mcp_writes):
                found.add('MCP')
        if kind in ('agent', 'control'):
            if any(marker in text for marker in MCP_POINTERS):
                found.add('MCP')
            if any(marker in text for marker in SKILL_POINTERS):
                found.add('SKILLS')
        profile, artifact_kind = where.get('profile'), where.get('artifactKind')
        if profile == 'mcp' or artifact_kind == 'protocol_transcript':
            found.add('MCP')
        if profile == 'skills' or artifact_kind == 'skill_loading_trace':
            found.add('SKILLS')
        if profile == 'scenarios' or artifact_kind == 'db_snapshot':
            found.add('DB')
        return found
    return layers


def gaps(root, layers=('MCP', 'SKILLS')):
    catalog = json.loads((root / 'verification/requirements/mandatory-oracles.json').read_text())
    required = {}
    for oracle in catalog['oracles']:
        for observation in oracle['expectedObservations']:
            required[(oracle['oracleId'], observation['name'])] = (oracle['caseId'], set(oracle['requiredLayers']) & set(layers))
    seen = {key: set() for key in required}
    for case_id in sorted(catalog['requiredCaseIds']):
        case = json.loads((root / f'verification/cases/{case_id}/case.json').read_text())
        for subcase in case['subcases']:
            layers_of = classifier(subcase)
            for assertion in subcase['assertions']:
                evidence = set()
                for ref in references({k: assertion.get(k) for k in ('source', 'baseline', 'unitSource', 'baselineUnitSource', 'expected')}, []):
                    evidence |= layers_of(ref)
                ref = assertion.get('oracleRef', {})
                for name in ref.get('observationNames', []):
                    if (ref.get('oracleId'), name) in seen:
                        seen[(ref['oracleId'], name)] |= evidence
    return [(case_id, oracle_id, name, layer) for (oracle_id, name), (case_id, need) in sorted(required.items())
            for layer in sorted(need - seen[(oracle_id, name)])]


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--root', type=pathlib.Path, default=ROOT)
    parser.add_argument('--include-db', action='store_true', help='also list DB layer gaps (informational)')
    args = parser.parse_args()
    rows, problems = review(args.root)
    for status, case_id, oracle_id, name, layer, owner in rows:
        print(f'{status}{" " + owner if status == "KNOWN_OPEN" else ""}\t{case_id}\t{oracle_id}/{name}\t{layer}')
    if args.include_db:
        for case_id, oracle_id, name, layer in gaps(args.root, ('DB',)):
            print(f'INFO\t{case_id}\t{oracle_id}/{name}\t{layer}')
    for problem in problems:
        print('PROBLEM\t' + problem)
    known_open = sum(1 for row in rows if row[0] == 'KNOWN_OPEN')
    print(json.dumps({'status': 'FAIL' if problems else 'VALID', 'unexplainedGaps': sum(1 for row in rows if row[0] == 'GAP'),
                      'knownOpen': known_open, 'problems': len(problems), 'runtimeCoverage': 'NOT_RUN'}))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
