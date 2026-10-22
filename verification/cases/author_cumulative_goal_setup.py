#!/usr/bin/env python3
"""28개 실제 실패의 setup 목표만 보완한다. 제품/판정 계약은 바꾸지 않는다.

T09/T11/T12/T13은 hand-authored case다(과거 round7/8/9 author 주석).
이 postprocessor는 원본을 생성하지 않고 명시한 setup만 fixed point로 만든다.
기간은 기한과 독립이다. T13은 fixture 업무 시작부터 원래 구매 기한까지,
다른 case는 기존 기간을 보존한다. 시작 사건도 누적하므로 startInclusive다.
후속 reviseGoal/revisePurchase와 불완전 초안, 미지원 evaluator는 보존한다.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SELECTION = {
    'T09': {'lot-not-yet-required': ('draft',), 'receipt-missing-lot': ('draft',)},
    'T11': {
        'parent90-cancelled': ('parent', 'child30', 'child60', 'followup'),
        'parent90-impossible': ('parent', 'child30', 'child60', 'followup'),
        'parent90-superseded': ('parent', 'child30', 'child60', 'followup'),
        'unsupported-evaluator': ('draft',),
        'shared-distinct60': ('parent-one', 'parent-two'),
    },
    'T12': {s: ('work',) for s in (
        'no-receipt-evidence', 'insufficient-source-scope', 'wrong-warehouse',
        'conflicting-claims', 'deadline-revision-and-correction')},
    'T13': {s: ('work',) for s in (
        'changed-quantity', 'changed-price', 'changed-itemId', 'changed-supplierId',
        'changed-dueAt', 'changed-destinationId', 'changed-endpoint',
        'decision-reject', 'decision-conditional', 'decision-unauthorized',
        'decision-expired', 'policy-rechecked', 'supplier-accept', 'supplier-reject',
        'supplier-change', 'partial-excess-return-relocation')},
}


def repair(case, root=ROOT):
    selected = SELECTION[case['caseId']]
    for sub in case['subcases']:
        if sub['id'] not in selected:
            continue
        found = set()
        fixture = json.loads((root / sub['fixtureRef']).read_text())
        for action in sub['actions']:
            if action['id'] not in selected[sub['id']]:
                continue
            found.add(action['id'])
            assert action['capabilityId'] in ('createWork', 'createDraft')
            request = action['request']
            slots = request['slots']
            assert slots['quantityMode'] == 'CUMULATIVE_EVENT'
            if case['caseId'] == 'T13':
                # Original proposal's period ends at its original dueAt; later revisions
                # remain separate probes and cannot stretch this accumulation interval.
                additions = {
                    'period': {'start': fixture['clock']['asOf'], 'end': slots['dueAt'],
                               'startInclusive': True, 'endInclusive': fixture['clock']['deadlineInclusive']},
                    'eventKinds': ['CONFIRMED_RECEIPT'],
                    'distinctContributionScope': 'CANONICAL_PHYSICAL_FULFILMENT',
                }
                assert slots['endpoint'] == 'ARRIVED'
                for key, value in additions.items():
                    assert key not in slots or slots[key] == value, (sub['id'], key)
                    slots[key] = value
                    request['provenance'][key] = 'CONTEXT'
            else:
                assert 'startInclusive' not in slots['period'] or slots['period']['startInclusive'] is True
                slots['period']['startInclusive'] = True
        assert found == set(selected[sub['id']]), (sub['id'], found)
    return case


def main():
    for case_id in SELECTION:
        path = ROOT / 'verification/cases' / case_id / 'case.json'
        case = repair(json.loads(path.read_text()))
        path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
