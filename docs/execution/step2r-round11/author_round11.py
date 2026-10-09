#!/usr/bin/env python3
"""Step 2 round 11 (closure review 8 P1 only): fixture recording time and reserve double booking.

Idempotent authoring of hand-authored fixtures and cases (no generator owns them). Evidence for
docs/execution/step2r-round11/README.md; ./verify prepare (ContractValidator.fixtureRecordTimeProblems and
reserveCapacityProblems, contracts/execution-preconditions.json clock.installation and reserveCapacity) is the gate.

Product rules (read only):
1. NF1. InventoryRepository and TradeEvidence read only rows with recordedAt <= the context knownAt, and every command
   context has asOf = knownAt = the product clock (ExecutionClock). The contract pins installation at or before the
   starting clock (fixture asOf). A fixture evidence row declared with a recordedAt later than the fixture knownAt is a
   late-known fact installed at that instant; a command that names it runs at or after it. T17 post-dispatch-expiry
   records its delivery at clock 09:06:00 with sourceEvidenceId delivery-20 declared recordedAt 09:06:01, so a correct
   product cannot see the source. The evidence is recorded at its occurrence (09:06:00), the clock of the delivery; the
   after-delivery query reads at knownAt 09:06:00, so the clock is not advanced instead.
2. NF2. FulfillmentCommands.prepare rejects a reservation that overlaps an EXECUTABLE/SUSPENDED allocation of the same
   segment (a null startQuantity overlaps every interval) with INSUFFICIENT_ELIGIBLE_QUANTITY, and one that exceeds the
   sales line remainder with SALES_LINE_QUANTITY_EXCEEDED. C3/fixture-reserveQuantity.json (shared by C3 and V4) held
   A20 and all of SALE-LINE in fixture ALLOCATION, so every authorized counter-call reserving A20 for SALE-LINE was a
   double booking; the reserve fixture drops that allocation (no reserve subcase reads it). V2 reserve-commits-first
   gives ALLOC its coordinate [0,40) and the winner reserves [40,60) of A60 for ORDER2.
Run: python3 -I docs/execution/step2r-round11/author_round11.py
"""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]


def load(path):
    text = path.read_text()
    data = json.loads(text)
    for indent in (2, None):
        for newline in ('\n', ''):
            if json.dumps(data, ensure_ascii=False, indent=indent) + newline == text:
                return data, (indent, newline)
    raise ValueError(f'{path}: unknown JSON layout')


def save(path, data, fmt):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=fmt[0]) + fmt[1])


def flat(actions, out):
    for a in actions:
        out.append(a)
        if 'call' in a:
            out.append(a['call'])
        for b in a.get('branches', []):
            flat(b.get('actions', []), out)
    return out


def edit(rel, change):
    path = ROOT / rel
    data, fmt = load(path)
    change(data)
    save(path, data, fmt)


# ---- NF1: late-known evidence read before it is recorded ---------------------------------------------------------------
def t17_delivery_evidence(fixture):
    row = next(e for e in fixture['evidence'] if e['alias'] == 'delivery-20')
    assert row['occurredAt'] == '2026-10-07T09:06:00Z'
    row['recordedAt'] = row['occurredAt']


# ---- NF2: reserve counter-calls that double-book a fixture allocation --------------------------------------------------
def c3_reserve_fixture(fixture):
    fixture['baseline']['priorEntities'].pop('ALLOCATION', None)


V2_NOTE = (' ALLOC40은 A60의 [0,40) 좌표를 가지고 신규 예약20은 startQuantity 40으로 [40,60)을 지명한다. 좌표 없는 배분은'
           ' 제품이 구간 전체와 겹친다고 보므로(FulfillmentCommands) 같은 실물을 두 번 예약하지 않는다.')


def v2_fixture(fixture):
    fixture['aliases']['ALLOC']['startQuantity'] = '0'
    for row in fixture['baseline']['allocations']:
        if row['alias'] == 'ALLOC':
            row['startQuantity'] = '0'
    if V2_NOTE not in fixture['baseline']['newDemandNote']:
        fixture['baseline']['newDemandNote'] += V2_NOTE


def v2_case(case):
    sub = next(s for s in case['subcases'] if s['id'] == 'reserve-commits-first')
    winner = next(a for a in flat(sub['actions'], []) if a['id'] == 'winner-call')
    slots = winner['request']['slots']
    rebuilt = {}
    for k, v in slots.items():
        if k == 'startQuantity':
            continue
        rebuilt[k] = v
        if k == 'quantity':
            rebuilt['startQuantity'] = {'value': '40', 'unit': 'BOX', 'provenance': 'USER'}
    winner['request']['slots'] = rebuilt


if __name__ == '__main__':
    edit('verification/cases/T17/fixtures/post-dispatch-expiry.json', t17_delivery_evidence)
    edit('verification/cases/C3/fixture-reserveQuantity.json', c3_reserve_fixture)
    edit('verification/cases/V2/fixtures/reserve-commits-first.json', v2_fixture)
    edit('verification/cases/V2/case.json', v2_case)
    print('round 11: T17 delivery-20 recorded at its occurrence; C3 reserve fixture without ALLOCATION; V2 ALLOC [0,40), winner [40,60)')
