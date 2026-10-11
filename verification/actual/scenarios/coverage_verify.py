#!/usr/bin/env python3
"""T25 verifyCoverage host command (Step 3 host adapter, verification/host-observation-guide.md "verifyCoverage 입력 snapshot").

Usage: coverage_verify.py --root REPO --request REQUEST.json --out REPORT.json

Reads exactly the input snapshot the request names (inputSnapshotKind) plus the registry, catalog and case files, applies
the requested mutation to an in-memory copy only, and writes one report. It never edits a repository file and never
computes a case oracle: the report states what the read inputs link, miss or claim. Supported snapshot kinds:

- PREPARATION: links every catalog oracle/observation to the case assertions that name it (oracleRef), reads the bound
  preparation report for its gate fields and commit/clean state, and records this checkout's git HEAD/status.
  Mutations: dropOracle, dropObservation, skipCase, mandatoryWaiver (scoped by mutationScope).
- MODEL_BINDING_PREPARATION: corpus identity, repetition plan, binding coverage, attempts/usage/acceptance state of the
  bound model-binding preparation report. Mutations: drop-binding, missing-attempt-usage, null-usage-as-zero,
  partial-cost-complete, unapproved-business-SLA.

REQUIRED_PATH_RUNTIME_EVIDENCE and APPROVED_MODEL_EXECUTION_EVIDENCE need a runtime manifest; when the requested file is
absent the command exits 4 without a report (no runtime evidence exists to verify).
"""
import argparse, copy, hashlib, json, pathlib, subprocess, sys

KINDS = {'PREPARATION': 'preparationReportPath', 'MODEL_BINDING_PREPARATION': 'modelManifestPath',
         'REQUIRED_PATH_RUNTIME_EVIDENCE': 'manifestPath', 'APPROVED_MODEL_EXECUTION_EVIDENCE': 'manifestPath'}
PREPARATION_MUTATIONS = {'none', 'dropOracle', 'dropObservation', 'skipCase', 'mandatoryWaiver'}
MODEL_MUTATIONS = {'none', 'drop-binding', 'missing-attempt-usage', 'null-usage-as-zero', 'partial-cost-complete', 'unapproved-business-SLA'}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(root, ref):
    path = root / ref
    if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):
        raise SystemExit(f'input not readable inside the repository: {ref}')
    return json.loads(path.read_text())


def git(root, *args):
    return subprocess.run(['git', *args], cwd=root, check=True, capture_output=True, text=True).stdout


# ---------------------------------------------------------------- PREPARATION

def case_documents(root, registry):
    return {c['caseId']: load(root, c['path']) for c in registry['cases']}


def mutate_preparation(mutation, scope, registry, cases):
    """Returns (registry, cases, waived) after applying the mutation to copies."""
    registry, cases, waived = copy.deepcopy(registry), copy.deepcopy(cases), set()
    case_id, oracle_id, observation = scope.get('caseId'), scope.get('oracleId'), scope.get('observationName')
    if mutation == 'skipCase':
        registry['cases'] = [c for c in registry['cases'] if c['caseId'] != case_id]
        cases.pop(case_id, None)
        return registry, cases, waived
    for sub in cases.get(case_id, {}).get('subcases', []):
        for assertion in sub.get('assertions', []):
            ref = assertion.get('oracleRef') or {}
            if ref.get('oracleId') != oracle_id:
                continue
            if mutation == 'dropOracle':
                assertion.pop('oracleRef', None)
            elif mutation == 'dropObservation':
                ref['observationNames'] = [n for n in ref.get('observationNames', []) if n != observation]
            elif mutation == 'mandatoryWaiver':
                waived.add((case_id, sub['id'], assertion['id']))
    return registry, cases, waived


def preparation_links(catalog, registry, cases, waived):
    present = {c['caseId'] for c in registry['cases']}
    links = {}
    for case_id, doc in cases.items():
        if case_id not in present:
            continue
        for sub in doc.get('subcases', []):
            for assertion in sub.get('assertions', []):
                ref = assertion.get('oracleRef')
                if not ref:
                    continue
                for name in ref.get('observationNames', []):
                    links.setdefault((ref.get('oracleId'), name), []).append({
                        'caseId': case_id, 'subcaseId': sub['id'], 'assertionId': assertion['id'], 'profile': 'scenarios',
                        'status': 'NOT_RUN', 'waived': (case_id, sub['id'], assertion['id']) in waived,
                        'evidenceRefs': assertion.get('evidenceRefs', [])})
    return present, links


def preparation_validation(catalog, required_cases, present, links, waived):
    issues = []
    for case_id in required_cases:
        if case_id not in present:
            issues.append({'code': 'SKIPPED_REQUIRED_CASE', 'caseId': case_id})
    for oracle in catalog['oracles']:
        if oracle['caseId'] not in present:
            continue  # the skipped case is reported once, not again per oracle
        keys = [(oracle['oracleId'], o['name']) for o in oracle.get('expectedObservations', [])]
        linked = [k for k in keys if links.get(k)]
        if not linked:
            issues.append({'code': 'MISSING_ORACLE', 'oracleId': oracle['oracleId']})
            continue
        for key in keys:
            if not links.get(key):
                issues.append({'code': 'MISSING_OBSERVATION', 'oracleId': key[0], 'observationName': key[1]})
    # A waiver on a catalog (mandatory) oracle path is one issue per oracle, however many assertions it waives.
    for oracle in catalog['oracles']:
        waived_links = [l for o in oracle.get('expectedObservations', []) for l in links.get((oracle['oracleId'], o['name']), []) if l.get('waived')]
        if waived_links:
            issues.append({'code': 'MANDATORY_PATH_WAIVED', 'caseId': oracle['caseId'], 'oracleId': oracle['oracleId'], 'waivedAssertions': len({l['subcaseId'] + '/' + l['assertionId'] for l in waived_links})})
    return {'status': 'FAIL' if issues else 'PASS', 'issues': issues}


def preparation_gate(report, validation):
    regulatory = 'NOT_RUN'
    for gate in report.get('runtimeGates', []):
        if gate.get('profile') == 'regulatory' and gate.get('status') != 'NOT_RUN_GATED':
            regulatory = gate.get('status')
    runtime = report.get('runtimeStatus', 'NOT_RUN')
    return {'preparationStatus': report.get('status'), 'runtimeStatus': runtime, 'requiredPathStatus': 'NOT_RUN',
            'modelStatus': 'NOT_RUN', 'regulatoryStatus': regulatory, 'usageStatus': 'NOT_RUN', 'uatComplete': False,
            # A preparation report is never runtime acceptance (verification/coverage/README.md).
            'gateComplete': bool(report.get('gateComplete')) and validation['status'] == 'PASS' and runtime == 'PASS',
            'semanticOracleEquivalence': report.get('semanticOracleEquivalence')}


def preparation(root, request, report_ref):
    registry, catalog = load(root, request['registryPath']), load(root, request['catalogPath'])
    report = load(root, report_ref)
    cases = case_documents(root, registry)
    required = request.get('requiredCaseIds') or catalog.get('requiredCaseIds') or [c['caseId'] for c in registry['cases']]
    mutation, scope = request.get('mutation', 'none'), request.get('mutationScope', {})
    if mutation not in PREPARATION_MUTATIONS:
        raise SystemExit(f'unsupported PREPARATION mutation {mutation}')

    def evaluate(reg, docs, waived):
        present, links = preparation_links(catalog, reg, docs, waived)
        validation = preparation_validation(catalog, required, present, links, waived)
        return links, validation, preparation_gate(report, validation)

    links, validation, gate = evaluate(registry, cases, set())
    out = {'baseline': None}
    if mutation != 'none':
        baseline = {'validation': validation, 'gate': gate, 'runtimeArtifacts': []}
        links, validation, gate = evaluate(*mutate_preparation(mutation, scope, registry, cases))
        out['baseline'] = baseline
        out['mutatedInput'] = {'caseId': scope.get('caseId'), 'mutation': mutation, 'oracleId': scope.get('oracleId'),
                               'observationName': scope.get('observationName')}
    named, flat = [], []
    for oracle in catalog['oracles']:
        for observation in oracle.get('expectedObservations', []):
            entries = links.get((oracle['oracleId'], observation['name']), [])
            named.append({'oracleId': oracle['oracleId'], 'observationName': observation['name'], 'assertionLinks': entries, 'artifacts': []})
            flat.extend(entries)
    status = git(root, 'status', '--porcelain')
    out.update({
        'input': {'snapshotKind': 'PREPARATION', 'path': report_ref, 'sha256': sha256(root / report_ref),
                  'codeCommit': report.get('codeCommit'), 'workingTreeDirty': report.get('workingTreeDirty'),
                  'checkoutCommit': git(root, 'rev-parse', 'HEAD').strip(), 'checkoutDirty': bool(status.strip())},
        'requiredCases': [c['caseId'] for c in registry['cases']],
        'requirementIds': catalog.get('requirementIds', []),
        'catalogOracles': [{'oracleId': o['oracleId']} for o in catalog['oracles']],
        'catalogObservations': [{'oracleId': o['oracleId'], 'name': x['name'], 'type': x.get('type'), 'scope': x.get('scope'), 'operator': x.get('operator')}
                                for o in catalog['oracles'] for x in o.get('expectedObservations', [])],
        'namedObservations': named, 'assertionLinks': flat, 'runtimeArtifacts': [],
        'gate': gate, 'validation': validation, 'regulatoryReviews': [], 'modelAuthorizations': []})
    if out['baseline'] is None:
        del out['baseline']
    return out


# ---------------------------------------------------------------- MODEL_BINDING_PREPARATION

def model_validation(corpus, bindings, manifest):
    issues = []
    bound = {c['caseId'] for c in bindings.get('cases', [])}
    for case in corpus['cases']:
        if case['id'] not in bound:
            issues.append({'code': 'MISSING_MODEL_BINDING', 'caseId': case['id']})
    attempts = manifest.get('attempts') or []
    for attempt in attempts:
        if 'usage' not in attempt or attempt.get('usage') is None:
            if attempt.get('usageObservation') == 'COERCED_ZERO':
                issues.append({'code': 'MISSING_USAGE_COERCED_ZERO', 'attemptId': attempt.get('attemptId')})
            else:
                issues.append({'code': 'ATTEMPT_USAGE_MISSING', 'attemptId': attempt.get('attemptId')})
    usage = manifest.get('usage')
    partial = any(isinstance(a.get('usage'), dict) and any(v is None for v in a['usage'].values()) for a in attempts)
    if isinstance(usage, dict) and usage.get('totalCostState') == 'COMPLETE' and partial:
        issues.append({'code': 'INCOMPLETE_USAGE_PASS'})
    proposal = corpus.get('acceptanceProposal', {})
    if proposal.get('status') not in (None, 'PROPOSAL_NOT_BUSINESS_SLA') and not proposal.get('approvedInR8'):
        issues.append({'code': 'R8_ACCEPTANCE_UNCONFIRMED', 'status': proposal.get('status')})
    return {'status': 'FAIL' if issues else 'PASS', 'issues': issues}


def mutate_model(mutation, corpus, bindings, manifest):
    corpus, bindings, manifest = copy.deepcopy(corpus), copy.deepcopy(bindings), copy.deepcopy(manifest)
    first = corpus['cases'][0]['id']
    synthetic = {'attemptId': f'{first}-repeat-1-mutant', 'caseId': first, 'repeat': 1}
    if mutation == 'drop-binding':
        bindings['cases'] = [c for c in bindings['cases'] if c['caseId'] != first]
    elif mutation == 'missing-attempt-usage':
        manifest['attempts'] = [dict(synthetic)]
    elif mutation == 'null-usage-as-zero':
        # A provider usage that was never observed reported as zero tokens: the mutant keeps the unobserved marker.
        manifest['attempts'] = [dict(synthetic, usage=None, usageObservation='COERCED_ZERO', reportedUsage={'inputTokens': 0, 'outputTokens': 0})]
    elif mutation == 'partial-cost-complete':
        # Observed input tokens but no output tokens or cost, while the total claims to be complete.
        manifest['attempts'] = [dict(synthetic, usage={'inputTokens': 120, 'outputTokens': None, 'cost': None})]
        manifest['usage'] = {'totalCostState': 'COMPLETE'}
    elif mutation == 'unapproved-business-SLA':
        corpus['acceptanceProposal']['status'] = 'BUSINESS_SLA'
    return corpus, bindings, manifest


def model_rows(corpus, bindings, manifest, validation):
    categories = {}
    for case in corpus['cases']:
        categories[case.get('category')] = categories.get(case.get('category'), 0) + 1
    usage = manifest.get('usage')
    attempts = manifest.get('attempts') or []
    return {
        'corpus': {'ids': [c['id'] for c in corpus['cases']], 'repetitions': corpus.get('executionPlan', {}).get('repeatCount'),
                   'foreignOrMixedCount': str(sum(1 for c in corpus['cases'] if c.get('languages') != ['ko'])),
                   'categories': [{'category': k, 'count': v} for k, v in categories.items()],
                   'bindingCount': len(bindings.get('cases', []))},
        # Verbatim corpus acceptance proposal: the verifier does not rename or approve it.
        'acceptance': {'status': corpus.get('acceptanceProposal', {}).get('status'), 'proposed': corpus.get('acceptanceProposal', {})},
        'attempts': attempts, 'uatPasses': [],
        'usage': {'totalCostState': 'MISSING' if usage is None else usage.get('totalCostState', 'MISSING')},
        'gate': {'preparationStatus': manifest.get('preparationStatus', manifest.get('status')), 'runtimeStatus': manifest.get('runtimeStatus'),
                 'modelStatus': 'NOT_RUN' if not attempts or manifest.get('actualModelCalls', 0) == 0 else 'FAIL',
                 'usageStatus': 'NOT_RUN' if usage is None else 'FAIL', 'uatComplete': False, 'gateComplete': False},
        'validation': validation}


def model_preparation(root, request, manifest_ref):
    corpus, bindings, manifest = load(root, request['modelCorpusPath']), load(root, request['modelBindingPath']), load(root, manifest_ref)
    mutation = request.get('mutation', 'none')
    if mutation not in MODEL_MUTATIONS:
        raise SystemExit(f'unsupported MODEL_BINDING_PREPARATION mutation {mutation}')
    validation = model_validation(corpus, bindings, manifest)
    rows = model_rows(corpus, bindings, manifest, validation)
    if mutation != 'none':
        mutated = mutate_model(mutation, corpus, bindings, manifest)
        mutated_validation = model_validation(*mutated)
        baseline = {'validation': validation, 'gate': rows['gate']}
        rows = model_rows(*mutated, mutated_validation)
        rows['baseline'] = baseline
        rows['mutatedInput'] = {'mutation': mutation}
    rows['input'] = {'snapshotKind': 'MODEL_BINDING_PREPARATION', 'path': manifest_ref, 'sha256': sha256(root / manifest_ref)}
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=pathlib.Path, required=True)
    parser.add_argument('--request', type=pathlib.Path, required=True)
    parser.add_argument('--out', type=pathlib.Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    request = json.loads(args.request.read_text())
    kind = request.get('inputSnapshotKind')
    if kind not in KINDS:
        raise SystemExit(f'unsupported inputSnapshotKind {kind}')
    snapshot = request.get(KINDS[kind])
    if not snapshot or not (root / snapshot).is_file():
        print(f'INPUT_ABSENT {kind} {snapshot}', file=sys.stderr)
        sys.exit(4)
    if kind == 'PREPARATION':
        rows = preparation(root, request, snapshot)
    elif kind == 'MODEL_BINDING_PREPARATION':
        rows = model_preparation(root, request, snapshot)
    else:
        print(f'UNSUPPORTED_RUNTIME_SNAPSHOT {kind}: runtime manifest verification is not implemented', file=sys.stderr)
        sys.exit(5)
    report = {'schemaVersion': '1.0.0', 'operation': 'verifyCoverage', 'evidenceClass': 'ACTUAL_HOST', 'rows': rows}
    args.out.write_text(json.dumps(report, ensure_ascii=False, sort_keys=True) + '\n')
    print(json.dumps({'snapshotKind': kind, 'validation': rows.get('validation', {}).get('status'),
                      'issues': [i['code'] for i in rows.get('validation', {}).get('issues', [])]}))


if __name__ == '__main__':
    main()
