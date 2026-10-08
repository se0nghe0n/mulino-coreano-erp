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

Exit 0: no unexplained gap. Exit 1: an observation without layer evidence that is neither
an EXEMPTION (reviewed reason) nor a KNOWN_OPEN gap of another owner.
"""
import argparse, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
API_ROUTES = {'api', 'odata', 'batch', 'nested', 'projection', 'direct'}
MCP_ROUTES = {'mcp', 'wire'}
MCP_POINTERS = ('/toolTranscript', '/transcript', 'toolCalls', 'wireTranscripts', 'protocolTranscript')
SKILL_POINTERS = ('skillLoading', '/loading', 'referenceReads', 'loadingObservations', 'skillDiscoveryStatus', 'skillBodyStatus')

# Reviewed exemptions: the observation is not about what the MCP/SKILLS path returns.
EXEMPTIONS = {
    ('E1.whole-runtime-and-model-reference', 'model-reference', 'MCP'):
        'Reads the independent host model-gate inputs (catalog oracle, R8 status, model plan). The oracle MCP layer '
        'is exercised by actual-runtime-observation through runtime-mcp in the same subcase; an MCP business read '
        'cannot prove the model gate separation.',
}
# Gaps owned by other workers; reported, not failed, until the owner closes them.
KNOWN_OPEN = {
    ('T20.skills-real-loading-and-meaning', 'allowed-tools-as-server-authorization', 'SKILLS'): 'T20 owner (cases-a)',
    ('T20.skills-real-loading-and-meaning', 'document-instruction-authority', 'SKILLS'): 'T20 owner (cases-a)',
    ('T20.skills-real-loading-and-meaning', 'skill-hash-as-loading-proof', 'MCP'): 'T20 owner (cases-a)',
    ('V4.all-alternate-write-paths', 'mixed-batch-allowed-partial-effects', 'MCP'): 'V4 owner',
}


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
    unexplained = []
    for case_id, oracle_id, name, layer in gaps(args.root, ('MCP', 'SKILLS', 'DB') if args.include_db else ('MCP', 'SKILLS')):
        key = (oracle_id, name, layer)
        if key in EXEMPTIONS:
            status = 'EXEMPT'
        elif key in KNOWN_OPEN:
            status = 'KNOWN_OPEN ' + KNOWN_OPEN[key]
        elif layer == 'DB':
            status = 'INFO'
        else:
            status = 'GAP'
            unexplained.append(key)
        print(f'{status}\t{case_id}\t{oracle_id}/{name}\t{layer}')
    print(json.dumps({'status': 'FAIL' if unexplained else 'VALID', 'unexplainedGaps': len(unexplained), 'runtimeCoverage': 'NOT_RUN'}))
    return 1 if unexplained else 0


if __name__ == '__main__':
    sys.exit(main())
