#!/usr/bin/env python3
"""T17의 확인된 성공 setup 예약에 실물 좌표를 명시한다.

일곱 fixture의 A는 계보 분할 전 독립 root 30 BOX다. 제품의 segment
좌표계는 [0, quantity)이므로 이 전체 물량 예약은 [0, 30)이다.
이는 누락 좌표의 일반 기본값이 아니다. 다른 예약/반례는 수정하지 않는다.
"""
import json
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
# 원본 shard-08 /cases/{41,42,43,44,45,46,48}/actions/reserve.
STARTS = dict.fromkeys((
    'normal-consumed-delivery', 'late-restriction-actual-delivery',
    'post-dispatch-expiry', 'claim-falseclaim', 'claim-unidentifiedscope',
    'claim-conflictingquantity', 'claim-scopeoverflow',
), '0')


def apply(case, root=ROOT):
    if case['caseId'] != 'T17':
        raise ValueError('T17 only')
    selected = {s['id']: s for s in case['subcases'] if s['id'] in STARTS}
    if set(selected) != set(STARTS):
        raise ValueError('Missing selected T17 subcase')
    changed = 0
    for sid, sub in selected.items():
        action, = (a for a in sub['actions'] if a['id'] == 'reserve')
        if action.get('capabilityId') != 'reserveQuantity':
            raise ValueError(f'{sid}: reserveQuantity required')
        request = action['request']
        slots = request['slots']
        fixture = json.loads((Path(root) / sub['fixtureRef']).read_text())
        alias = slots['segmentId']['$alias']
        segment = fixture['aliases'][alias]
        stock, = (s for s in fixture['baseline']['segments'] if s['alias'] == alias)
        # 이 allowlist의 기존 독립 root/동일 실물 선언을 대조한다.
        if (alias != 'A' or segment['type'] != 'QuantitySegment'
                or not segment['active'] or not stock['active']
                or segment['physicalScope'] != stock['physicalScope']
                or segment['quantity'] != stock['quantity']
                or segment['unit'] != stock['unit'] or slots['unit'] != stock['unit']
                or segment.get('startQuantity', '0') != STARTS[sid]
                or stock.get('startQuantity', '0') != STARTS[sid]):
            raise ValueError(f'{sid}: physical scope changed; review coordinate')
        start, quantity = Decimal(STARTS[sid]), Decimal(slots['quantity'])
        if quantity <= 0 or start + quantity > Decimal(stock['quantity']):
            raise ValueError(f'{sid}: reservation exceeds declared stock')
        scale = fixture['aliases'][segment['itemAlias']]['decimalScale']
        if any(v != v.quantize(Decimal(1).scaleb(-scale)) for v in (start, quantity)):
            raise ValueError(f'{sid}: quantity violates item unit scale')
        for target, value in ((slots, STARTS[sid]), (request['provenance'], 'CONTEXT')):
            if 'startQuantity' in target and target['startQuantity'] != value:
                raise ValueError(f'{sid}: conflicting authored coordinate/provenance')
            if 'startQuantity' not in target:
                target['startQuantity'] = value
                changed += 1
    return changed


def main():
    path = ROOT / 'verification/cases/T17/case.json'
    case = json.loads(path.read_text())
    changed = apply(case)
    if changed:
        path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + '\n')
    print(f'T17 reserve coordinates: {changed} fields authored')


if __name__ == '__main__':
    main()
