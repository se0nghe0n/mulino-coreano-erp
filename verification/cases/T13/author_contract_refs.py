#!/usr/bin/env python3
"""T13의 구매 요청 참조만 현재 제품 계약으로 재작성한다.

Baseline: 66e4d068017753147017f5d08062ba460e4e385c.
WorkLifecycle.result/createWork: goalVersionId와 최신 revision은 응답 최상위다.
PurchaseCommands.target/prepare: 공급자 답변은 orderId와 OrderLine revision을
사용한다. originalQuantity는 공개 slot이 아니며 기존 주문에서 읽는다.
수량·가격·시각·실행자·assertion·fixture 및 부정 probe는 변경하지 않는다.

이 파일은 hand-authored case의 idempotent 후처리다. 다른 case를 생성하는
기존 공유 helper를 실행하지 않는다. 실제 시나리오/인수 결과를 만들지 않는다.
Run: python3 verification/cases/T13/author_contract_refs.py
"""
import json
from pathlib import Path

DIRECTORY = Path(__file__).resolve().parent
BASELINE = '66e4d068017753147017f5d08062ba460e4e385c'
SUPPLIER_CASES = {'supplier-accept', 'supplier-reject', 'supplier-change'}


def result(action_id, pointer):
    return {'$result': {'actionId': action_id, 'pointer': pointer}}


def repair(case):
    assert case['caseId'] == 'T13'
    for sub in case['subcases']:
        for action in sub['actions']:
            if action.get('capabilityId') == 'proposePurchase':
                request = action['request']
                slots = request['slots']
                assert slots['workId'] == result('work', '/response/workId')
                slots['goalVersionId'] = result('work', '/response/goalVersionId')
                request['expectedRevision'] = result('work', '/response/revision')
                request['provenance']['goalVersionId'] = 'CONTEXT'
            if sub['id'] in SUPPLIER_CASES and action.get('capabilityId') == 'recordSupplierReply':
                request = action['request']
                # The original 100 BOX remains in the immutable order/proposal and
                # source document. Sending a second, caller-controlled copy is not
                # part of PurchaseCommands or the published slot allowlist.
                original = request['slots'].pop('originalQuantity', None)
                assert original is None or original == {'value': '100', 'unit': 'BOX'}
                request['provenance'].pop('originalQuantity', None)
                request['slots']['purchaseId'] = result('dispatch', '/response/orderId')
                request['expectedRevision'] = result('dispatch', '/response/revision')
                request['provenance']['purchaseId'] = 'CONTEXT'
    return case


def main():
    path = DIRECTORY / 'case.json'
    case = repair(json.loads(path.read_text()))
    path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
