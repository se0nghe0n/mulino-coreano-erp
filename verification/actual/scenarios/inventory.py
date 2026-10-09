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


APPROXIMATE = set(json.loads((ROOT / 'verification/actual/scenarios/observation-sources.json').read_text()).get('approximate', []))


def missing_fields(evidence, src):
    """Fields an assertion filters or reads that no observed row of that source carries (an observer projection gap)."""
    action = evidence.get('actions', {}).get(src.get('actionId')) or {}
    rows = ((action.get('data') or {}).get('rawRows') or {}).get(src.get('pointer', '').split('/')[3])
    if not isinstance(rows, list) or not rows:
        return []
    wanted = list((src.get('where') or {}).keys())
    f = src.get('field')
    wanted += f if isinstance(f, list) else [f] if f else []
    present = set().union(*(r.keys() for r in rows if isinstance(r, dict)))
    return [w for w in wanted if w not in present]


QUERY_FIELDS = set(re.findall(r'"([A-Za-z]+)"', re.search(r'FIELDS=Set\.of\(([^)]*)\)', (ROOT / 'backend/src/main/java/com/mulino/application/core/QueryRequests.java').read_text()).group(1)))
INTENT = json.loads((ROOT / 'contracts/intent.schema.json').read_text())


def find_action(actions, aid):
    for a in actions:
        if a.get('id') == aid:
            return a
        for b in a.get('branches', []):
            found = find_action(b.get('actions', []), aid)
            if found:
                return found
    return None


def intent_violation(action):
    if not action or action.get('kind') not in ('invoke', 'start'):
        return None
    req = (action.get('call') or action).get('request') or {}
    extra = sorted(set(req) - set(INTENT['properties']))
    missing = sorted(set(INTENT['required']) - set(req))
    if not extra and not missing:
        return None
    return 'missing ' + ','.join(missing) + ('; extra ' + ','.join(extra) if extra else '')


def install_state(evidence, subcase):
    for aid in action_order(subcase):
        a = evidence.get('actions', {}).get(aid)
        if a and a.get('actionKind', None) is None and isinstance(a.get('data'), dict) and 'aliasMap' in a.get('data', {}):
            d = a['data']
            return d.get('fixtureComplete', True), d.get('omittedFacts', [])
    return None, []


def first_rejection(evidence, subcase):
    for aid in action_order(subcase):
        a = evidence.get('actions', {}).get(aid) or {}
        r = a.get('response')
        if isinstance(r, dict) and r.get('outcome') in ('REJECTED', 'HELD', 'CONFLICT', 'NEEDS_INPUT') and isinstance(r.get('error'), dict):
            return {'actionId': aid, 'outcome': r.get('outcome'), 'code': r['error'].get('code'), 'message': r['error'].get('message')}
    return None


def first_problem(evidence, subcase):
    actions = evidence.get('actions', {})
    if evidence.get('harnessError'):
        msg = evidence['harnessError']
        m = re.search(r'Missing/null observed value at ([^/\s]+)/response/(\S+)', msg)
        upstream = actions.get(m.group(1)) if m else None
        r = (upstream or {}).get('response')
        if isinstance(r, dict) and isinstance(r.get('error'), dict):
            err = r['error']
            detail = {'kind': 'UPSTREAM_ACTION_REJECTED', 'actionId': m.group(1), 'missingPointer': '/response/' + m.group(2),
                      'outcome': r.get('outcome'), 'code': err.get('code'), 'message': err.get('message'),
                      'detail': 'upstream action rejected ' + str(err.get('code')) + ': ' + str(err.get('message')), 'harnessError': msg}
            if err.get('message') in ('Unsupported scope key', 'Unsupported inventory scope'):
                detail['note'] = 'case query scope key not defined by the plan/product query schema'
                return detail, 'TEST'
            if err.get('message') == 'Unsupported query field':
                extra = sorted(set(((find_action(subcase.get('actions', []), m.group(1)) or {}).get('request') or {})) - QUERY_FIELDS)
                detail['note'] = 'case query fields outside the product query envelope (no contract defines it): ' + ','.join(extra)
                return detail, 'TEST'
            violation = intent_violation(find_action(subcase.get('actions', []), m.group(1)))
            if violation and err.get('code') == 'TYPE_INVALID':
                detail['note'] = 'case request violates contracts/intent.schema.json: ' + violation
                return detail, 'TEST'
            return detail, 'PRODUCT'
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
            src = x.get('source') or {}
            pointer = src.get('pointer', '')
            detail['pointer'] = src.get('actionId', '') + pointer
            cls = 'PRODUCT'
            if pointer.startswith('/data/rawRows/'):
                name = pointer.split('/')[3]
                if name in APPROXIMATE:
                    cls = 'ADAPTER'; detail['note'] = 'observation source mapping is approximate'
                else:
                    missing = missing_fields(evidence, src)
                    if missing:
                        cls = 'ADAPTER'; detail['note'] = 'observer rows lack fields ' + ','.join(missing)
            return {'kind': 'ASSERTION_FAILED', **detail}, cls
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
    mode_file = pathlib.Path(argv[1]).parent / 'run-mode.txt'
    run_mode = dict(l.split('=', 1) for l in mode_file.read_text().split() if '=' in l) if mode_file.exists() else {}
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
        complete, omitted = install_state(e, sub)
        rejection = first_rejection(e, sub)
        if problem is not None and rejection is not None:
            problem['firstProductRejection'] = rejection
        # A failure on a partially installed world is first an installer gap, until the omitted facts are installed.
        if status == 'FAIL' and complete is False and cls == 'PRODUCT':
            cls = 'ADAPTER'
            problem['note'] = 'fixture installed partially; product verdict pending complete fixture'
        executed = sum(1 for a in e.get('actions', {}).values() if a.get('driverStatus') == 'EXECUTED')
        row = {'caseId': cid, 'subcaseId': sid, 'status': status, 'executedActions': executed,
               'declaredActions': len(action_order(sub)), 'fixtureComplete': complete, 'omittedFixtureFacts': omitted,
               'firstProblem': problem, 'classification': cls}
        subcases.append(row)
        totals[status] += 1
        if status == 'PASS' and complete is False:
            totals['PASS_WITH_PARTIAL_FIXTURE'] += 1
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
        'command': report.get('command'), 'reportTimestamp': report.get('timestamp'), 'runMode': run_mode,
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
