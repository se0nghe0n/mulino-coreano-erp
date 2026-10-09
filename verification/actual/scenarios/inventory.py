#!/usr/bin/env python3
"""Builds the machine-readable run inventory from an actual scenarios report.

Usage: inventory.py <scenarios.json> <out.json> [--evidence-dir DIR]
Per subcase: PASS / FAIL / NOT_IMPLEMENTED, the first failing assertion or missing route, and a
classification guess (ADAPTER / PRODUCT / TEST). The guess is a triage heuristic, not a verdict.
"""
import collections, json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[3]

ADAPTER_PATTERNS = [
    r'fixture alias type', r'installer cannot', r'Fixture inheritance', r'fixture mapping pending', r'Multi-organization',
    r'actual query route/capability', r'actual command route', r'actual requested host/barrier control',
    r'raw protocol transport adapter', r'actual async route', r'RESULT_REVISION', r'Raw sources required',
    r'Raw segments require', r'installed control identity', r'no fixture organization installed',
    r'Identity is not explicitly bound', r'Actor organization differs', r'S3 final physical quantity',
    r'Unsupported fixture', r'NOT_IMPLEMENTED: unsupported', r'observer', r'fixture alias depends',
]
PRODUCT_PATTERNS = [r'actual HTTP endpoint', r'verification clock profile not installed']
CLIENT_PATTERNS = [r'actual client/model runner']


def action_order(subcase):
    out = []
    def walk(actions):
        for a in actions:
            out.append(a['id'])
            for b in a.get('branches', []):
                walk(b.get('actions', []))
    walk(subcase.get('actions', []))
    return out


def classify_reason(reason):
    if any(re.search(p, reason) for p in CLIENT_PATTERNS):
        return 'ADAPTER'
    if any(re.search(p, reason) for p in PRODUCT_PATTERNS):
        return 'PRODUCT'
    return 'ADAPTER'


def first_problem(evidence, subcase):
    actions = evidence.get('actions', {})
    if evidence.get('harnessError'):
        msg = evidence['harnessError']
        # An exception raised while validating an EXECUTED product response is adapter/harness plumbing until triaged.
        return {'kind': 'HARNESS_ERROR', 'detail': msg}, ('PRODUCT' if 'HTTP 5' in msg else 'ADAPTER')
    for aid in action_order(subcase):
        a = actions.get(aid)
        if a is None:
            continue
        if a.get('driverStatus') != 'EXECUTED':
            reason = a.get('reason') or ''
            if 'depends on' in reason:
                continue
            return {'kind': 'ACTION_NOT_EXECUTED', 'actionId': aid, 'detail': reason}, classify_reason(reason)
    for aid in action_order(subcase):
        a = actions.get(aid)
        if a is not None and a.get('driverStatus') != 'EXECUTED':
            return {'kind': 'ACTION_NOT_EXECUTED', 'actionId': aid, 'detail': a.get('reason')}, classify_reason(a.get('reason') or '')
    for x in evidence.get('assertions', []):
        if x.get('status') == 'FAIL':
            detail = {k: x.get(k) for k in ('assertionId', 'failureKind', 'reason', 'expected', 'observed') if k in x}
            obs = json.dumps(detail.get('observed'))
            if obs and len(obs) > 600:
                detail['observed'] = obs[:600] + '...'
            return {'kind': 'ASSERTION_FAILED', **detail}, 'PRODUCT'
    missing = evidence.get('missingAdapters') or []
    if missing:
        return {'kind': 'MISSING_ADAPTER', 'detail': missing}, 'ADAPTER'
    if evidence.get('uncompletedRuntimeTasks'):
        return {'kind': 'RUNTIME_TASK_INCOMPLETE', 'detail': evidence['uncompletedRuntimeTasks']}, 'ADAPTER'
    for x in evidence.get('assertions', []):
        if x.get('status') == 'NOT_RUN':
            return {'kind': 'ASSERTION_NOT_RUN', 'assertionId': x.get('assertionId'), 'detail': x.get('reason')}, 'ADAPTER'
    return None, None


def main(argv):
    if len(argv) < 3:
        print(__doc__, file=sys.stderr); return 3
    report = json.loads(pathlib.Path(argv[1]).read_text())
    out = pathlib.Path(argv[2])
    cases = {}
    for f in sorted((ROOT / 'verification/cases').glob('*/case.json')):
        c = json.loads(f.read_text())
        cases[c['caseId']] = c
    subcases = []
    totals = collections.Counter(); by_class = collections.Counter(); groups = collections.defaultdict(list)
    seen = set()
    for e in report.get('cases', []):
        cid, sid = e['caseId'], e['subcaseId']
        seen.add((cid, sid))
        sub = next(s for s in cases[cid]['subcases'] if s['id'] == sid)
        status = {'PASS': 'PASS', 'FAIL': 'FAIL'}.get(e['status'], 'NOT_IMPLEMENTED')
        problem, cls = (None, None) if status == 'PASS' else first_problem(e, sub)
        executed = sum(1 for a in e.get('actions', {}).values() if a.get('driverStatus') == 'EXECUTED')
        row = {'caseId': cid, 'subcaseId': sid, 'status': status, 'executedActions': executed,
               'declaredActions': len(action_order(sub)), 'firstProblem': problem, 'classification': cls}
        subcases.append(row)
        totals[status] += 1
        if cls:
            by_class[status + '/' + cls] += 1
            key = (problem or {}).get('detail') or (problem or {}).get('reason') or (problem or {}).get('kind')
            key = re.sub(r'[0-9a-f]{8}-[0-9a-f-]{27,}', '<uuid>', str(key))[:160]
            groups[(cls, key)].append(cid + '/' + sid)
    for cid, c in cases.items():
        if 'scenarios' in c.get('profiles', []):
            for s in c['subcases']:
                if (cid, s['id']) not in seen:
                    subcases.append({'caseId': cid, 'subcaseId': s['id'], 'status': 'NOT_IMPLEMENTED', 'firstProblem': {'kind': 'NOT_SELECTED'}, 'classification': 'ADAPTER'})
                    totals['NOT_IMPLEMENTED'] += 1; by_class['NOT_IMPLEMENTED/ADAPTER'] += 1
    per_case = collections.defaultdict(collections.Counter)
    for r in subcases:
        per_case[r['caseId']][r['status']] += 1
    top = sorted(groups.items(), key=lambda kv: -len(kv[1]))
    inventory = {
        'recordType': 'S5A_ACTUAL_SCENARIO_INVENTORY', 'schemaVersion': '1.0.0',
        'codeCommit': report.get('codeCommit'), 'workingTreeDirty': report.get('workingTreeDirty'),
        'command': report.get('command'), 'reportTimestamp': report.get('timestamp'),
        'harnessStatus': report.get('status'), 'harnessExitCode': report.get('exitCode'),
        'actualExecutedActions': report.get('actualExecutedActions'),
        'classificationNote': 'classification is a triage guess: ADAPTER harness/actual gap, PRODUCT backend differs from plan, TEST case oracle looks wrong. Assertion failures default to PRODUCT until triaged.',
        'totals': dict(totals), 'byStatusAndClass': dict(by_class),
        'perCase': {k: dict(v) for k, v in sorted(per_case.items())},
        'topFailureGroups': [{'classification': k[0], 'problem': k[1], 'count': len(v), 'subcases': v[:40]} for k, v in top[:40]],
        'subcases': subcases,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(inventory, indent=1, ensure_ascii=False) + '\n')
    print(json.dumps({'totals': inventory['totals'], 'byStatusAndClass': inventory['byStatusAndClass']}, indent=1))
    for g in inventory['topFailureGroups'][:15]:
        print(g['count'], g['classification'], g['problem'])
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
