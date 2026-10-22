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
    if cid in ("V5", "T26"):
        apply_recovery(case, counts)
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



def recovery_predicate():
    return {'operator': 'quantitySum', 'property': 'Receipt.quantity',
            'minimum': {'value': '20', 'unit': 'BOX'}, 'unit': 'BOX',
            'evidenceSelector': 'VERIFIED_DISTINCT'}


def apply_recovery(case, counts):
    # Only complete recovery subcases; leave arbitrary incomplete/negative inputs alone.
    for sub in case.get('subcases', []):
        creates = [a for a in sub.get('actions', []) if a.get('id') == 'create'
                   and a.get('capabilityId') == 'createWork']
        if not creates:
            continue
        goal = creates[0]['request']['slots']['goal']
        if goal.get('quantityMode') != 'STATE_AT' or goal.get('endpoint') != 'VERIFIED':
            continue
        additions = {'evaluationAt': goal['dueAt'], 'action': 'PHYSICAL', 'includeReserved': True,
                     'scope': {'itemId': {'$alias': 'P'}, 'placeId': {'$alias': 'W'}},
                     'evaluatorVersion': 'core-v1', 'conditions': ['verified-ready'],
                     'evidenceSource': {'sourceNamespace': 'synthetic-warehouse',
                                        'externalEventId': 'EV-READY', 'sourceVersion': '1'}}
        for key, value in additions.items():
            if key not in goal:
                goal[key] = value
                counts['recoveryGoalContract'] += 1
            elif goal[key] != value:
                raise ValueError(f"{case['caseId']}/{sub['id']}: conflicting recovery {key}")
        if any(a['id'] == 'create-activate' for a in sub['actions']):
            sub['actions'] = [a for a in sub['actions'] if a['id'] != 'create-activate']
            counts['alreadyActive'] += 1
        def fix(node):
            if isinstance(node, list):
                for child in node: fix(child)
            elif isinstance(node, dict):
                if node.get('actionId') == 'create-activate': node['actionId'] = 'create'
                if node.get('kind') == 'evidenceAvailable' and node.get('eventId') == 'EV-READY':
                    node.clear(); node.update(recovery_predicate()); counts['typedResume'] += 1
                for key, child in list(node.items()):
                    if child == 'evaluator-v1':
                        node[key] = 'core-v1'; counts['recoveryEvaluatorPin'] += 1
                    elif child == 'EVALUATOR:evaluator-v1':
                        node[key] = 'EVALUATOR:core-v1'; counts['recoveryEvaluatorPin'] += 1
                    elif isinstance(child, list):
                        for index, value in enumerate(child):
                            if value == 'evaluator-v1': child[index] = 'core-v1'; counts['recoveryEvaluatorPin'] += 1
                            elif value == 'EVALUATOR:evaluator-v1': child[index] = 'EVALUATOR:core-v1'; counts['recoveryEvaluatorPin'] += 1
                        fix(child)
                    else: fix(child)
        fix(sub)


def apply_recovery_fixture(fixture):
    contract = fixture.get('baseline', {}).get('definitionContract')
    if not contract:
        return False
    pins_changed = False
    for artifact in fixture['baseline'].get('definitionArtifacts', []):
        if artifact.get('definitionVersion') == 'definition-v1' and artifact.get('evaluatorVersion') == 'evaluator-v1':
            artifact['evaluatorVersion'] = 'core-v1'
            pins_changed = True
    if 'goalTemplate' in contract:
        return pins_changed
    if contract.get('predicate', {}).get('operator') != 'verifiedDistinctQuantity':
        raise ValueError('Unknown recovery definition contract')
    # These are authored test contracts, not expected assessment results.
    fixture['baseline']['definitionContract'] = {
        'definitionVersion': 'definition-v1', 'evaluatorVersion': 'core-v1',
        'goalTemplate': {'name': 'verified-ready', 'quantityMode': 'STATE_AT',
                         'endpoint': 'VERIFIED', 'evaluatorVersion': 'core-v1',
                         'predicate': recovery_predicate()},
        'evidenceSource': {'sourceNamespace': 'synthetic-warehouse',
                           'externalEventId': 'EV-READY', 'sourceVersion': '1'}}
    fixture['versions']['evaluator'] = 'core-v1'
    fixture['aliases']['RECOVERY_EVIDENCE_POLICY'] = {
        'type': 'PolicyVersion', 'kind': 'EVIDENCE', 'version': 'synthetic-evidence-v1',
        'state': 'ACTIVE', 'effectiveFrom': '2026-10-01T00:00:00Z',
        'content': {'source': 'synthetic recovery evidence policy'}}
    return True

def main():
    totals = Counter()
    for path in sorted((ROOT / 'verification/cases').glob('*/case.json')):
        case, fmt, old = layout.load(path)
        counts = apply(case)
        if counts:
            path.write_text(layout.render(case, fmt))
            totals.update(counts)
            print(case['caseId'], dict(counts))
    for cid in ('V5', 'T26'):
        feature = ROOT / f'verification/cases/{cid}/scenario.feature'
        old = feature.read_text()
        feature.write_text(''.join(line for line in old.splitlines(keepends=True)
                                   if '"create-activate" 행동' not in line))
        for path in sorted((ROOT / f'verification/cases/{cid}/fixtures').glob('*.json')):
            fixture, fmt, old = layout.load(path)
            if apply_recovery_fixture(fixture):
                path.write_text(layout.render(fixture, fmt))
                totals['recoveryDefinition'] += 1
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
