#!/usr/bin/env python3
"""Step 2 round 12: hand-maintained cases and fixtures into the published request contracts.

Run-driven (AGENTS.md 2026-10-09): docs/execution/s5a-adapter/run-61bd0f8a.json rejected every api command and
most queries for envelope shape (TEST class, 613 of 802 subcases) and refused 111 subcases because one fixture
grant listing items, works and segments is an all-of match in the product. The rules live in
verification/cases/request_contract.py and contracts/request-contracts.json; this script only applies them to
the files no generator owns. Generator-owned files are rewritten by their generators, which call the same module:
  T01 T20 T25          verification/mcp-tests/author_cases.py
  T06 T22 T24          verification/cases/T06/author_contracts.py
  T23 V8               verification/platform-tests/build_cases.py
  C3, T26, V4 cases    their post-processors (author_prerequisites.py, author_review_fixes.py)
Idempotent: a second run changes nothing (verification/requirements/test_case_generators_reproduce.py).
Run: python3 -I docs/execution/step2r-round12/author_round12.py [--check]
"""
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
RC = importlib.util.module_from_spec(spec)
spec.loader.exec_module(RC)
GENERATED_CASES = {'T01', 'T20', 'T25', 'T06', 'T22', 'T24', 'T23', 'V8', 'C3', 'T26', 'V4'}
GENERATED_FIXTURE_DIRS = {'T01', 'T20', 'T25', 'T06', 'T22', 'T24', 'T23', 'V8'}
_c3 = importlib.util.spec_from_file_location('c3_author', ROOT / 'verification/cases/C3/author_prerequisites.py')
C3 = importlib.util.module_from_spec(_c3)
_c3.loader.exec_module(C3)
COMPACT = 'compact'  # one action/assertion per line (C3.render_case), the layout of T08, V6 and V7


def load(path):
    text = path.read_text()
    data = json.loads(text)
    if isinstance(data, dict) and "subcases" in data and C3.render_case(data) == text:
        return data, COMPACT, text
    for indent in (2, None):
        for newline in ('\n', ''):
            if json.dumps(data, ensure_ascii=False, indent=indent) + newline == text:
                return data, (indent, newline), text
    raise ValueError(f'{path}: unknown JSON layout')


def render(data, fmt):
    return C3.render_case(data) if fmt == COMPACT else json.dumps(data, ensure_ascii=False, indent=fmt[0]) + fmt[1]


def fixture_refs(case, contracts):
    refs, todo = set(), [s.get('fixtureRef') for s in case['subcases'] if s.get('fixtureRef')]
    for sub in case['subcases']:
        for action in RC.iter_actions(sub.get('actions', [])):
            if action.get('fixtureRef'):
                todo.append(action['fixtureRef'])
    while todo:
        ref = todo.pop()
        if ref in refs or not (ROOT / ref).is_file():
            continue
        refs.add(ref)
        todo += json.loads((ROOT / ref).read_text()).get('baseRefs') or []
    return refs


# The C4 duty reads hid an obligation kind in a non-contract 'filter' object, so check_vocabulary.py never saw it.
# Moved into the query, DELIVERY_DEFICIT is not a published kind; the case's own oracles name the product kind
# DELIVERY_CORRECTED_DEFICIT (contracts/domain-vocabulary.json obligationKinds, DeliveryDeficitResponsibilities).
STALE_SELECTORS = {('C4', 'resolved-no-resurrection-resolved', 'duty'), ('C4', 'resolved-no-resurrection-waived', 'duty')}


def fix_selectors(case):
    for sub in case['subcases']:
        for action in RC.iter_actions(sub.get('actions', [])):
            if (case['caseId'], sub['id'], action.get('id')) in STALE_SELECTORS:
                request = action['request']
                holder = request['filter'] if isinstance(request.get('filter'), dict) else request
                if holder.get('kind') == 'DELIVERY_DEFICIT':
                    holder['kind'] = 'DELIVERY_CORRECTED_DEFICIT'


def main():
    check = sys.argv[1:] == ['--check']
    contracts = RC.Contracts(ROOT)
    changed, cache = [], {}
    cases = sorted((ROOT / 'verification/cases').glob('*/case.json'))
    fixtures = set()
    for path in cases:
        case = json.loads(path.read_text())
        fixtures |= {r for r in fixture_refs(case, contracts) if r.split('/')[2] not in GENERATED_FIXTURE_DIRS}
    for ref in sorted(fixtures):
        path = ROOT / ref
        data, fmt, text = load(path)
        if 'actors' not in data:
            continue
        new = render(contracts.conform_fixture(data), fmt)
        if new != text:
            changed.append(ref)
            if not check:
                path.write_text(new)
    for path in cases:
        if path.parent.name in GENERATED_CASES:
            continue
        data, fmt, text = load(path)
        fix_selectors(data)
        new = render(contracts.conform_case(data, cache), fmt)
        if new != text:
            changed.append(str(path.relative_to(ROOT)))
            if not check:
                path.write_text(new)
    if check and changed:
        sys.exit('round 12 request contracts not applied: ' + ', '.join(changed[:20]) + (f' (+{len(changed) - 20})' if len(changed) > 20 else ''))
    print(('CURRENT' if check else 'WRITTEN') + f' round 12: {len(changed)} file(s) changed, {len(fixtures)} fixture(s) and '
          f'{len([p for p in cases if p.parent.name not in GENERATED_CASES])} hand-maintained case(s) checked')


if __name__ == '__main__':
    main()
