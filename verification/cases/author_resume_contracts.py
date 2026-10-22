#!/usr/bin/env python3
"""Apply explicit Oct 12 case decisions; authoring only, never runtime evidence.

Leaves incomplete/invalid-input cases alone. C3/V4 periods are explicit fixture
choices for authorization controls, not adapter defaults or production policy.
"""
import importlib.util
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('round12_layout', ROOT / 'docs/execution/step2r-round12/author_round12.py')
layout = importlib.util.module_from_spec(spec)
spec.loader.exec_module(layout)


def apply(case):
    counts = Counter()
    cid = case['caseId']
    def visit(node):
        if isinstance(node, list):
            for child in node: visit(child)
        elif isinstance(node, dict):
            cap, slots = node.get('capabilityId'), node.get('slots')
            if isinstance(slots, dict):
                provenance = node.setdefault('provenance', {})
                if cap in ('createSalesOrder', 'reviseSalesOrder') and slots.get('deliveryEndpoint') == 'CUSTOMER_ACCEPTED':
                    slots['deliveryEndpoint'] = 'DELIVERED'
                    counts['salesEndpoint'] += 1
                    if 'qualityTerms' not in slots:
                        slots['qualityTerms'] = 'STANDARD'
                        provenance['qualityTerms'] = 'USER'
                        counts['salesQuality'] += 1
                if cid in ('V5', 'T26') and cap == 'createWork' and 'goal' in slots and 'workType' in slots:
                    slots['kind'] = slots.pop('workType')
                    provenance['kind'] = provenance.pop('workType')
                    counts['goalKind'] += 1
                if cid in ('V5', 'T26') and cap == 'createWork' and isinstance(slots.get('goal'), dict):
                    # Only lossless storage-name mappings. STATE_AT timing,
                    # evidence predicates and evaluator selection need their
                    # own explicit contract; do not invent activation defaults.
                    goal = slots['goal']
                    for old, new in (('quantity', 'targetQuantity'), ('destinationId', 'placeId')):
                        if old in goal:
                            if new in goal and goal[new] != goal[old]:
                                raise ValueError(f'{cid}: conflicting goal {old}/{new}')
                            goal[new] = goal.pop(old)
                            counts['goalStorageName'] += 1
                if cid in ('C3', 'V4') and cap in ('createWork', 'createDraft') and slots.get('quantityMode') == 'CUMULATIVE_EVENT' and 'period' not in slots:
                    slots['period'] = {'start': '2026-10-07T09:00:00Z', 'end': slots['dueAt'], 'startInclusive': True, 'endInclusive': True}
                    provenance['period'] = 'USER'
                    counts['authorizationPeriod'] += 1
            for child in list(node.values()): visit(child)
    visit(case)
    if cid == 'T17':
        for sub in case.get('subcases', []):
            for assertion in sub.get('assertions', []):
                source = assertion.get('source', {})
                if (assertion.get('id') == 'eligibility-response-5'
                        and source.get('pointer') == '/data/rawRows/orders'
                        and source.get('field') == 'deliveryEndpoint'
                        and assertion.get('expected') == ['CUSTOMER_ACCEPTED']):
                    assertion['expected'] = ['DELIVERED']
                    counts['salesEndpointAssertion'] += 1
                elif (assertion.get('id') == 'eligibility-response-6'
                        and source.get('pointer') == '/response/data/deliveryEndpoint'
                        and assertion.get('expected') == 'CUSTOMER_ACCEPTED'):
                    assertion['expected'] = 'DELIVERED'
                    counts['salesEndpointAssertion'] += 1
    return counts


def main():
    totals = Counter()
    for path in sorted((ROOT / 'verification/cases').glob('*/case.json')):
        case, fmt, old = layout.load(path)
        counts = apply(case)
        if counts:
            path.write_text(layout.render(case, fmt))
            totals.update(counts)
            print(case['caseId'], dict(counts))
    for name in ('explicit-authorized-migration', 'pinned-meaning-current-policy'):
        path = ROOT / 'verification/cases/T21/fixtures' / (name + '.json')
        fixture, fmt, old = layout.load(path)
        # Definition commands have no ITEM dimension. Keep the organization,
        # explicit capability list, validity and delegation chain unchanged.
        scope = fixture['actors']['fde']['grant']['scope']
        if 'items' in scope:
            del scope['items']
            path.write_text(layout.render(fixture, fmt))
            totals['definitionGrant'] += 1
    print('changes', dict(totals))

if __name__ == '__main__': main()
