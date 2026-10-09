#!/usr/bin/env python3
"""Preparation gate: every case request meets contracts/request-contracts.json (step2r round 12); never a runtime PASS.

Checks every invoke/query action of verification/cases/*/case.json and every fixture they install:
- command intent bodies (api, mcp, worker, management, bypass routes, batch operations, blob business actions and
  wire tools/call arguments) against contracts/intent.schema.json, one provenance entry per slot, the action's
  capability and the execution idempotency key;
- queries against the product query envelope (fields, scope keys, filters per operation);
- fixture grants that name two or more dimension kinds declare scopeComposition;
- action.harness.intentionalViolation equals the actual violations and the subcase pins the rejection.
A violation covered by a contracts/request-contracts.json knownOpen product gap prints one KNOWN_OPEN line per gap and
case (owner, count, first example) instead of failing; ./verify prepare copies those lines into its report.
Exit 0: no unrecorded violation. Exit 1: violations listed.
Run: python3 -I verification/cases/check_request_contracts.py [--check]
"""
import collections
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('request_contract', Path(__file__).with_name('request_contract.py'))
RC = importlib.util.module_from_spec(spec)
spec.loader.exec_module(RC)


def review(root=ROOT):
    contracts = RC.Contracts(root)
    problems, known, fixtures = [], collections.OrderedDict(), set()
    cases = sorted((Path(root) / 'verification/cases').glob('*/case.json'))
    for path in cases:
        case = json.loads(path.read_text())
        p, k = contracts.case_problems(case)
        problems += p
        for line in k:
            gap = line.split('KNOWN_OPEN ', 1)[1].split()[0]
            key = (gap, case['caseId'])
            known.setdefault(key, []).append(line)
        todo = [s.get('fixtureRef') for s in case.get('subcases', []) if s.get('fixtureRef')]
        while todo:
            ref = todo.pop()
            if ref in fixtures or not (Path(root) / ref).is_file():
                continue
            fixtures.add(ref)
            data = json.loads((Path(root) / ref).read_text())
            todo += data.get('baseRefs') or []
            problems += contracts.fixture_problems(ref, data)
    owners = {g['id']: g['owner'] for g in contracts.known}
    lines = [f'KNOWN_OPEN {gap} owner={owners[gap]} case={case_id} count={len(rows)} example={rows[0]}' for (gap, case_id), rows in known.items()]
    return problems, lines, len(cases), len(fixtures)


def main():
    problems, lines, cases, fixtures = review()
    for line in lines:
        print(line)
    for problem in problems:
        print('FAIL', problem)
    print(f'request contracts: {"VALID" if not problems else "INVALID"}, {cases} cases, {fixtures} fixtures, '
          f'problems {len(problems)}, knownOpen {sum(int(l.split("count=")[1].split()[0]) for l in lines)} in {len(lines)} line(s)')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
