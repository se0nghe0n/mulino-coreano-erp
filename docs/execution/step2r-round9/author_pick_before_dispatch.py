#!/usr/bin/env python3
"""Step 2 round 9: an explicit, authorized pick before every dispatch of a runtime reservation, and verification-basis originals.

Idempotent authoring of hand-authored cases (no generator owns T05 or T13). Evidence for
docs/execution/step2r-round9/README.md; ./verify prepare (ContractValidator.pickBeforeDispatchProblems and
receiptCustodyProblems) is the gate.

Product rule (read only): FulfillmentCommands.prepare rejects dispatchQuantity with INVALID 'Pick before dispatch required'
when the allocation has no pickedAt, FulfillmentStockPrimitives.dispatch checks the same, and only
FulfillmentStockPrimitives.pick sets pickedAt (and increments the allocation revision). The adapter never creates a pick
(verification/harness-guide.md "adapter가 승인·수령·예약을 암묵적으로 생성하지 않는다"). Step 2 closure review 6 (P2) found:
- T13 partial-excess-return-relocation: reserve -> dispatch-sale with no pick and no actor holding pickQuantity, so
  dispatch-sale-applied, the delivery/return chain, return-applied and returned-at-W fail on a correct product. sales (which
  already reserves and dispatches) gets pickQuantity in role and grant, a `pick` of the reserve allocation runs between
  reserve and dispatch-sale, dispatch-sale's expectedRevision chains to the pick, and pick-applied pins APPLIED.
- T05 manager-disposition: reserve -> dispatch with no pick and no picker, asserting the dispatch APPLIED. warehouse (the
  physical picker, already holding reserve/dispatch there) gets pickQuantity, a `pick` runs after reserved-db (so the
  EXECUTABLE reservation observation is unchanged), the ordinary dispatch chains its expectedRevision to the pick, and
  pick-before-dispatch-applied pins APPLIED. The pick is an execution step, not a decision, so
  new-human-approval-added-to-reservation-or-dispatch-18 (no RESERVE_OR_DISPATCH_APPROVAL) is unchanged.
Closure review 6 P3: the product reads receiving custodians only from the verified chains of the receipt's canonical
occurrence (ReceiptCommands.evidencedCustodians via TradeEvidence.verifiedCanonical), not from request-level evidenceRefs. T13
receipt60/40/5 cited their original warehouse-receipt only in request evidenceRefs, so they now also name it as the
verification basis in an evidenceId slot (request evidenceRefs unchanged).
Run: python3 -I docs/execution/step2r-round9/author_pick_before_dispatch.py
"""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
PICK = 'pickQuantity'


def load(p): return json.loads(p.read_text())
def save(p, d): p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
def alias(x): return {'$alias': x}
def result(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}
def sub_of(case, sid): return next(s for s in case['subcases'] if s['id'] == sid)
def action(sub, aid): return next(a for a in sub['actions'] if a['id'] == aid)
def assertion(sub, aid): return next(a for a in sub['assertions'] if a['id'] == aid)


def put_after(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor) + 1, item)


def put_before(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor), item)


def grant(fixture_path, actor):
    fixture = load(fixture_path)
    a = fixture['actors'][actor]
    for key in (a['roleCapabilities'], a['grant']['actions']):
        if PICK not in key:
            key.insert(key.index('dispatchQuantity'), PICK)
    save(fixture_path, fixture)


def feature_block(case_id, sub_id):
    path = ROOT / f'verification/cases/{case_id}/scenario.feature'
    lines = path.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{sub_id}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith('시나리오')), len(lines))
    return path, lines, start, end


def feature_action(case_id, sub_id, after, aid, actor):
    path, lines, start, end = feature_block(case_id, sub_id)
    block = [l for l in lines[start:end] if f'"{aid}" 행동' not in l]
    at = next(i for i, l in enumerate(block) if f'"{after}" 행동' in l)
    block.insert(at + 1, f'    만일 "{actor}" 역할이 "{aid}" 행동을 수행한다')
    lines[start:end] = block
    path.write_text('\n'.join(lines))


def feature_assertion(case_id, sub_id, before, aid, text):
    path, lines, start, end = feature_block(case_id, sub_id)
    block = [l for l in lines[start:end] if f'"{aid}" assertion' not in l]
    at = next(i for i, l in enumerate(block) if f'"{before}" assertion' in l)
    block.insert(at, f'    그러면 "{aid}" assertion으로 "{text}"를 확인한다')
    lines[start:end] = block
    path.write_text('\n'.join(lines))


def chain_dispatch(sub, dispatch_id):
    d = action(sub, dispatch_id)
    assert d['request']['slots']['allocationId'] in (result('reserve', '/response/allocationId'),
                                                       {'value': result('reserve', '/response/allocationId'), 'provenance': 'CONTEXT'}), dispatch_id
    d['request']['expectedRevision'] = result('pick', '/response/revision')


# ---- T13 -------------------------------------------------------------------------------------------------------------
T13_SUB = 'partial-excess-return-relocation'


def t13():
    path = ROOT / 'verification/cases/T13/case.json'
    case = load(path)
    sub = sub_of(case, T13_SUB)
    grant(ROOT / sub['fixtureRef'], 'sales')
    pick = {'id': 'pick', 'kind': 'invoke', 'actorRef': 'sales', 'route': 'api', 'capabilityId': PICK,
            'request': {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': PICK,
                        'subjectRefs': [{'type': 'TradeItem', 'id': alias('P')}],
                        'slots': {'allocationId': {'value': result('reserve', '/response/allocationId'), 'provenance': 'CONTEXT'}},
                        'expectedRevision': result('reserve', '/response/revision'),
                        'commandIdempotencyKey': f'T13-{T13_SUB}-pick', 'evidenceRefs': ['need']},
            'evidenceRefs': ['pick:actual-adapter-artifact']}
    put_after(sub['actions'], 'reserve', pick)
    chain_dispatch(sub, 'dispatch-sale')
    base = assertion(sub, 'dispatch-sale-applied')
    applied = {'id': 'pick-applied', 'op': 'equals', 'source': {'actionId': 'pick', 'pointer': '/response/outcome'}, 'expected': 'APPLIED',
               'requirementRefs': base['requirementRefs'], 'evidenceRefs': ['pick:actual-adapter-artifact'], 'scope': base['scope'],
               'oracleExplanation': ('수령60에서 예약한 10 BOX를 출고 전에 실제로 pick한다. 제품은 pick되지 않은 배분의 출고를 거부하므로'
                                     '(FulfillmentCommands "Pick before dispatch required") 이 pick이 없으면 출고·인도·반품이 모두 실행되지 않는다. '
                                     'pick은 배분 revision을 올리므로 출고의 expectedRevision은 pick 결과를 쓴다.'),
               'oracleRef': base['oracleRef']}
    put_before(sub['assertions'], 'dispatch-sale-applied', applied)
    for rid in ('receipt60', 'receipt40', 'receipt5'):
        r = action(sub, rid)
        assert r['request']['evidenceRefs'] == ['warehouse-receipt'], rid
        r['request']['slots']['evidenceId'] = {'value': alias('warehouse-receipt'), 'provenance': 'CONTEXT'}
    save(path, case)
    feature_action('T13', T13_SUB, 'reserve', 'pick', 'sales')
    feature_assertion('T13', T13_SUB, 'dispatch-sale-applied', 'pick-applied', '출고 전 예약10 pick = APPLIED')
    readme = ROOT / 'verification/cases/T13/README.md'
    lines = readme.read_text().split('\n')
    prefix = '| T13.partial-and-excess-contributions / return-added-to-purchase |'
    ref = f'{T13_SUB}/pick-applied'
    i = next(i for i, l in enumerate(lines) if l.startswith(prefix))
    if ref not in lines[i]:
        lines[i] = lines[i].replace(f'{T13_SUB}/dispatch-sale-applied', f'{ref}, {T13_SUB}/dispatch-sale-applied', 1)
    readme.write_text('\n'.join(lines))


# ---- T05 -------------------------------------------------------------------------------------------------------------
T05_SUB = 'manager-disposition'
T05_PICK_ASSERTION = 'pick-before-dispatch-applied'
T05_TEXT = ('출고 전 예약10을 warehouse가 pick한다. 제품은 pick되지 않은 배분의 출고를 거부하므로(FulfillmentCommands "Pick before dispatch '
            'required") 이 pick이 있어야 일반 출고가 실행된다. pick은 실행 단계이며 새 인간 승인이 아니다.')


def t05():
    path = ROOT / 'verification/cases/T05/case.json'
    case = load(path)
    sub = sub_of(case, T05_SUB)
    grant(ROOT / sub['fixtureRef'], 'warehouse')
    reserve = action(sub, 'reserve')
    pick = {'id': 'pick', 'kind': 'invoke', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': PICK,
            'request': {'intentKind': 'COMMAND', 'definitionVersion': reserve['request']['definitionVersion'], 'capabilityId': PICK,
                        'organizationId': alias('ORG'), 'subjectRefs': reserve['request']['subjectRefs'],
                        'slots': {'allocationId': result('reserve', '/response/allocationId')},
                        'expectedRevision': result('reserve', '/response/revision'),
                        'commandIdempotencyKey': f'T05-{T05_SUB}-pick', 'evidenceRefs': reserve['request']['evidenceRefs'],
                        'valueProvenance': 'USER'},
            'evidenceRefs': ['pick:actual-adapter-artifact']}
    put_after(sub['actions'], 'reserved-db', pick)
    chain_dispatch(sub, 'dispatch')
    base = assertion(sub, 'ordinary-authorized-dispatch-after-confirmation-17')
    applied = {'id': T05_PICK_ASSERTION, 'op': 'equals', 'source': {'actionId': 'pick', 'pointer': '/response/outcome'}, 'expected': 'APPLIED',
               'requirementRefs': base['requirementRefs'], 'evidenceRefs': ['pick:actual-adapter-artifact'], 'scope': base['scope'],
               'oracleExplanation': T05_TEXT, 'oracleRef': base['oracleRef']}
    put_before(sub['assertions'], 'ordinary-authorized-dispatch-after-confirmation-16', applied)
    save(path, case)
    feature_action('T05', T05_SUB, 'reserved-db', 'pick', 'warehouse')
    feature_assertion('T05', T05_SUB, 'ordinary-authorized-dispatch-after-confirmation-16', T05_PICK_ASSERTION,
                      '출고 전 예약10 warehouse pick = APPLIED, 새 인간 승인이 아닌 실행 단계')
    obs = ROOT / 'verification/cases/T05/observations.md'
    lines = obs.read_text().split('\n')
    start = lines.index(f'## {T05_SUB}')
    lines = [l for l in lines if not l.startswith(f'- `{T05_PICK_ASSERTION}`')]
    at = next(i for i in range(start, len(lines)) if lines[i].startswith('- `ordinary-authorized-dispatch-after-confirmation-16`'))
    lines.insert(at, f'- `{T05_PICK_ASSERTION}` → `T05.disposition-manager-decision / ordinary-authorized-dispatch-after-confirmation`: {T05_TEXT}')
    obs.write_text('\n'.join(lines))


if __name__ == '__main__':
    t13(); t05()
    print('authored T13 ' + T13_SUB + ' and T05 ' + T05_SUB + ' pick before dispatch; T13 receipt verification-basis evidenceId')
