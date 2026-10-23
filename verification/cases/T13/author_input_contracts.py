#!/usr/bin/env python3
"""T13 입력과 공개 승인 protocol, 실제 개정값 조회 참조를 정렬한다.

Baseline: 2c290f81cd3c2b32d4abf6c48f8072e9ccb624ef.
RuntimeRepository.enqueue는 externalOperationId에 UUID를 요구한다.
원래 입력 라벨 전체를 RFC 4122 URL namespace의 UUIDv5로 매핑한다.
이름은 urn:mulino:verification:external-operation:<원래 라벨>이다.
sourceVersion, source commit, attempt, token, 요청 본문은 매핑에 넣지 않는다.
따라서 같은 업무 작업의 replay/본문 변경도 같은 ID를 유지하며 제품이
충돌을 판정한다. 기존 업무 entity/문서 ID와 원문 증거는 바꾸지 않는다.

PolicyCommands.execute의 공개 ID는 /response/id이고 revision은 최상위다.
contentHash/policyHash는 생성 응답에 없다. PURCHASE_APPROVAL 및
MANAGER_NEW_DECISION을 소비하는 typed content 계약도 현재 없다.
이를 무시되는 metadata나 새 조건 primitive로 옮기지 않는다.
그 draft와 승인 hash 입력은 계약 공백으로 남기며 oracle는 보존한다.
Run: python3 verification/cases/T13/author_input_contracts.py
"""
import copy
import json
import uuid
from pathlib import Path

DIRECTORY = Path(__file__).resolve().parent
BASELINE = '2c290f81cd3c2b32d4abf6c48f8072e9ccb624ef'


def external_operation_id(label):
    assert label.startswith('T13-') and label.endswith('-external')
    return str(uuid.uuid5(uuid.NAMESPACE_URL,
                          'urn:mulino:verification:external-operation:' + label))


def repair_inputs(case):
    assert case['caseId'] == 'T13'
    for sub in case['subcases']:
        for action in sub['actions']:
            if action.get('capabilityId') == 'dispatchPurchaseOrder':
                label = 'T13-' + sub['id'] + '-external'
                slots = action['request']['slots']
                stable = external_operation_id(label)
                assert slots['externalOperationId'] in (label, stable)
                slots['externalOperationId'] = stable
            if sub['id'] == 'changed-itemId' and action['id'] == 'dispatch':
                action['request']['subjectRefs'] = [{'type': 'TradeItem', 'id': {'$alias': 'P-alt'}}]
            if sub['id'] == 'policy-rechecked' and action['id'] in ('policy-approved', 'policy-active'):
                ref = action['request']['slots']['id']['$result']
                assert ref['actionId'] == 'policy-draft'
                assert ref['pointer'] in ('/response/policyId', '/response/id')
                ref['pointer'] = '/response/id'
    return case


# Exactly these failures are published HELD approval guards, not TYPE_INVALID,
# FORBIDDEN or unsupported conditional selectors. Explicit codes prevent an
# unrelated denial from satisfying the original negative business scenario.
APPROVAL_CODES = {
    **{'changed-' + field: 'APPROVAL_NOT_CURRENT' for field in
       ('quantity', 'price', 'itemId', 'supplierId', 'dueAt', 'destinationId', 'endpoint')},
    'decision-reject': 'APPROVAL_REJECTED',
    'decision-expired': 'APPROVAL_EXPIRED',
    'policy-rechecked': 'APPROVAL_POLICY_CHANGED',
}


def repair(case):
    repair_inputs(case)
    for sub in case['subcases']:
        by_id = {a['id']: a for a in sub['assertions']}
        if sub['id'] in APPROVAL_CODES:
            code = APPROVAL_CODES[sub['id']]
            outcome = next(a for a in sub['assertions']
                           if a.get('source') == {'actionId': 'dispatch', 'pointer': '/response/outcome'})
            assert outcome['expected'] in ('REJECTED', 'HELD')
            outcome['expected'] = 'HELD'
            # Audit still requires the same actor/capability, count and policy
            # version; only the published outcome spelling changes.
            for a in sub['assertions']:
                source = a.get('source', {})
                if source.get('pointer') != '/data/rawRows/audit':
                    continue
                if source.get('where', {}).get('outcome') == 'REJECTED':
                    source['where']['outcome'] = 'HELD'
                if 'outcome' in source.get('field', []):
                    index = source['field'].index('outcome')
                    for row in a['expected']:
                        assert row[index] in ('REJECTED', 'HELD')
                        row[index] = 'HELD'
            guard = copy.deepcopy(outcome)
            guard['id'] = 'dispatch-approval-code'
            guard['source']['pointer'] = '/response/error/code'
            guard['expected'] = code
            guard['oracleExplanation'] = '공개 승인 code로 의도한 승인 guard가 실행됐는지 확인한다'
            if guard['id'] in by_id:
                assert by_id[guard['id']] == guard
            else:
                sub['assertions'].append(guard)
        if sub['id'].startswith('changed-'):
            field = sub['id'].removeprefix('changed-')
            after = next(a for a in sub['actions'] if a['id'] == 'after')
            # PurchaseQueries publishes the current immutable ProposalRevision.
            # Keep the existing action/clock; point its noun read at that proposal.
            after['request']['id'] = {'$result': {'actionId': 'revision', 'pointer': '/response/proposalId'}}
            after['request']['scope']['objectType'] = 'PurchaseProposal'
            after['request'].pop('type', None)
            if field == 'itemId':
                after['request']['scope']['itemId'] = {'$alias': 'P-alt'}
            a = by_id['changed-field-value']
            a['source'] = {'actionId': 'after', 'pointer': '/response/data/' + field}
            a['evidenceRefs'] = ['after:actual-adapter-artifact']
            if field in ('quantity', 'price'):
                dimension = 'unit' if field == 'quantity' else 'currency'
                # TransportValues normalizes BigDecimal (1.20 -> 1.2), so exact
                # decimal comparison retains the original authored value 1.20.
                original = a['expected'] if isinstance(a['expected'], dict) else {
                    'value': a['expected'], dimension: 'BOX' if field == 'quantity' else 'EUR'}
                a['expected'] = original['value']
                a['op'] = 'decimalEquals'
                unit = copy.deepcopy(a)
                unit['id'] = 'changed-field-' + dimension
                unit['op'] = 'equals'
                unit['source']['pointer'] = '/response/data/' + dimension
                unit['expected'] = original[dimension]
                unit['oracleExplanation'] = '개정값의 원래 단위 또는 통화를 정확히 보존한다'
                if unit['id'] in by_id:
                    assert by_id[unit['id']] == unit
                else:
                    sub['assertions'].append(unit)
    return case


def repair_feature(text, case):
    chunks = text.split('  시나리오:')
    for index, sub in enumerate(case['subcases'], 1):
        for assertion in sub['assertions']:
            if assertion['id'] not in ('dispatch-approval-code', 'changed-field-unit', 'changed-field-currency'):
                continue
            line = ('    그러면 "' + assertion['id'] + '" assertion으로 "' +
                    assertion['oracleExplanation'] + '"를 확인한다\n')
            if line not in chunks[index]:
                end = len(chunks[index].rstrip('\n'))
                chunks[index] = chunks[index][:end] + '\n' + line.rstrip('\n') + chunks[index][end:]
    return '  시나리오:'.join(chunks)

def main():
    path = DIRECTORY / 'case.json'
    case = repair(json.loads(path.read_text()))
    path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + '\n')
    feature = DIRECTORY / 'scenario.feature'
    feature.write_text(repair_feature(feature.read_text(), case))


if __name__ == '__main__':
    main()
