#!/usr/bin/env python3
"""Step 2 round 9 follow-up: picked fixture allocations and expiry negatives that only the expiry can reject.

Idempotent authoring of hand-authored fixtures and cases (C3 fixture-dispatchQuantity.json included: the C3 post-processor
only rewrites the dispatchPurchaseOrder/migrateWorkDefinition/transferObligation/recall fixtures). Generator-owned files are
changed in their generators instead: T24 audit-rollback-and-retry.json
(verification/cases/T06/author_contracts.py) and T26 (verification/cases/T26/author_review_fixes.py). Evidence for
docs/execution/step2r-round9/README.md; ./verify prepare (ContractValidator.pickBeforeDispatchProblems) is the gate.

Product rule (read only): FulfillmentCommands.prepare rejects a dispatch whose allocation has no pickedAt ('Pick before
dispatch required', REJECTED TYPE_INVALID), after the scope authorization (FORBIDDEN) and the suspended/current-sale checks
(INSUFFICIENT_ELIGIBLE_QUANTITY). Coordinator follow-up to closure review 6:
1. Fixture allocations. The subcases that dispatch an installed allocation test authorization (T08, V7), rollback fault
   points (T04), lock races (V3), a revoked disposition basis (C1) on an allocation that is already ready to dispatch. An
   explicit pick action would add a write, a fence and a revision change inside their before/after effect windows and
   races (C3 reader/delegator authorization on the same input too), so the fixture declares the allocation already picked: pickedAt (the fixture clock asOf, a past fact) and
   pickedByAlias (the actor that dispatches it there, which holds dispatchQuantity), beside the declaration that states the
   allocation state (alias, baseline.priorEntities entry or baseline.allocations row). FixtureInstaller installs it (Step 3).
2. T16 expiry-sweeper and expiry-delayed-sweep: warehouse (the dispatcher) gets pickQuantity, picks the reservation right
   after reserve (before the reserved-db baseline), the dispatch chains its expectedRevision to the pick, pick-applied pins
   APPLIED, and expiry-sweeper now pins the dispatch REJECTED / INSUFFICIENT_ELIGIBLE_QUANTITY (the sweeper suspended the
   allocation: 'Suspended allocation cannot execute'). Before, its dispatch had no pin at all, so a product without the
   sweeper also passed by rejecting the unpicked dispatch.
Run: python3 -I docs/execution/step2r-round9/author_pick_followup.py
"""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]

FIXTURE_PICKS = {  # fixture: {allocation alias: picker}
    'verification/cases/C1/fixtures/revoked-basis.json': {'ALLOC': 'warehouse'},
    'verification/cases/C3/fixture-dispatchQuantity.json': {'ALLOCATION': 'delegator'},  # not touched by C3/author_prerequisites.py
    'verification/cases/T04/fixtures/rollback-afterMovementBeforeAllocation.json': {'ALLOC': 'warehouse'},
    'verification/cases/T04/fixtures/rollback-afterAllocationBeforeAudit.json': {'ALLOC': 'warehouse'},
    'verification/cases/T04/fixtures/rollback-afterAuditBeforeOutbox.json': {'ALLOC': 'warehouse'},
    'verification/cases/T04/fixtures/rollback-afterOutboxBeforeCommittedResult.json': {'ALLOC': 'warehouse'},
    'verification/cases/T08/fixture.json': {'ALLOCATION': 'warehouse'},
    'verification/cases/V3/fixtures/hold-first.json': {'ALLOC': 'warehouse'},
    'verification/cases/V3/fixtures/dispatch-first.json': {'ALLOC': 'warehouse'},
    'verification/cases/V3/fixtures/late-v1-release.json': {'ALLOC': 'warehouse'},
    'verification/cases/V7/fixture.json': {'ALLOCATION': 'warehouse'},
    'verification/cases/V7/restart-fixture.json': {'ALLOCATION': 'warehouse', 'ALLOCATION2': 'warehouse'},
}


def load(p): return json.loads(p.read_text())
def save(p, d): p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
def alias(x): return {'$alias': x}
def result(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}


def declare_picked(fixture, name, picker):
    """pickedAt/pickedByAlias beside every declaration that states the allocation state, else on the alias."""
    b = fixture.get('baseline', {})
    places = [fixture['aliases'][name]]
    if name in b.get('priorEntities', {}):
        places.append(b['priorEntities'][name])
    places += [r for key in ('allocations', 'allocation') for r in b.get(key, []) if r.get('alias') == name]
    stated = [p for p in places if 'state' in p or 'status' in p] or [fixture['aliases'][name]]
    assert picker in fixture['actors'], (name, picker)
    for p in stated:
        p['pickedAt'] = fixture['clock']['asOf']
        p['pickedByAlias'] = picker


def fixtures():
    for ref, picks in FIXTURE_PICKS.items():
        path = ROOT / ref
        fixture = load(path)
        for name, picker in picks.items():
            assert fixture['aliases'][name]['type'] == 'Allocation', (ref, name)
            declare_picked(fixture, name, picker)
        save(path, fixture)


# ---- T16 ---------------------------------------------------------------------------------------------------------------
T16_SUBS = ('expiry-sweeper', 'expiry-delayed-sweep')


def put_after(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor) + 1, item)


def put_before(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor), item)


def by_id(items, i): return next(x for x in items if x['id'] == i)


def feature_block(sub_id):
    path = ROOT / 'verification/cases/T16/scenario.feature'
    lines = path.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{sub_id}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith('시나리오')), len(lines))
    return path, lines, start, end


def feature_line(sub_id, key, anchor, line, before=False):
    path, lines, start, end = feature_block(sub_id)
    block = [l for l in lines[start:end] if key not in l]
    at = next(i for i, l in enumerate(block) if anchor in l)
    block.insert(at if before else at + 1, line)
    lines[start:end] = block
    path.write_text('\n'.join(lines))


def observation_line(sub_id, aid, anchor_aid, text, oracle, name):
    path = ROOT / 'verification/cases/T16/observations.md'
    lines = path.read_text().split('\n')
    start = lines.index(f'## {sub_id}')
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith('## ')), len(lines))
    block = [l for l in lines[start:end] if not l.startswith(f'- `{aid}`')]
    at = next(i for i, l in enumerate(block) if l.startswith(f'- `{anchor_aid}`'))
    block.insert(at, f'- `{aid}` → `{oracle} / {name}`: {text}')
    lines[start:end] = block
    path.write_text('\n'.join(lines))


PICK_TEXT = ('만료 전 예약20을 warehouse가 pick한다. 그래서 뒤 출고가 거부되는 이유는 만료뿐이다. pick이 없으면 만료 처리가 없는 제품도 '
             'pick 누락(FulfillmentCommands "Pick before dispatch required")으로 출고를 거부해 이 subcase를 통과한다.')
CODE_TEXT = ('sweeper가 만료 경계에서 예약을 SUSPENDED로 바꿨으므로 pick된 예약의 출고도 거부되고 이유는 '
             'INSUFFICIENT_ELIGIBLE_QUANTITY다(FulfillmentCommands "Suspended allocation cannot execute"). pick 누락의 TYPE_INVALID가 아니다.')


def t16():
    path = ROOT / 'verification/cases/T16/case.json'
    case = load(path)
    for sid in T16_SUBS:
        sub = by_id(case['subcases'], sid)
        fpath = ROOT / sub['fixtureRef']
        fixture = load(fpath)
        w = fixture['actors']['warehouse']
        for key in (w['roleCapabilities'], w['grant']['actions']):
            if 'pickQuantity' not in key:
                key.insert(key.index('dispatchQuantity'), 'pickQuantity')
        save(fpath, fixture)
        reserve, dispatch = by_id(sub['actions'], 'reserve'), by_id(sub['actions'], 'dispatch')
        assert dispatch['actorRef'] == 'warehouse'
        pick = {'id': 'pick', 'kind': 'invoke', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': 'pickQuantity',
                'request': {'intentKind': 'COMMAND', 'definitionVersion': reserve['request']['definitionVersion'], 'capabilityId': 'pickQuantity',
                            'organizationId': alias('ORG'), 'subjectRefs': reserve['request']['subjectRefs'],
                            'slots': {'allocationId': result('reserve', '/response/allocationId')},
                            'expectedRevision': result('reserve', '/response/revision'),
                            'commandIdempotencyKey': f'T16-{sid}-pick', 'evidenceRefs': reserve['request']['evidenceRefs'], 'valueProvenance': 'USER'},
                'evidenceRefs': ['pick:actual-adapter-artifact']}
        put_after(sub['actions'], 'reserve', pick)
        dispatch['request']['expectedRevision'] = result('pick', '/response/revision')
        template = next(x for x in sub['assertions'] if x['oracleRef']['observationNames'] == ['boundary-recheck'])
        def new(aid, action, pointer, expected, text):
            return {'id': aid, 'op': 'equals', 'source': {'actionId': action, 'pointer': pointer}, 'expected': expected,
                    'requirementRefs': template['requirementRefs'], 'evidenceRefs': [action + ':actual-adapter-artifact'], 'scope': template['scope'],
                    'oracleExplanation': text, 'oracleRef': template['oracleRef']}
        first = next(x['id'] for x in sub['assertions'] if x['id'] not in ('pick-applied', 'dispatch-after-sweep-rejected', 'dispatch-after-sweep-code'))
        put_before(sub['assertions'], first, new('pick-applied', 'pick', '/response/outcome', 'APPLIED', PICK_TEXT))
        feature_line(sid, '"pick" 행동', '"reserve" 행동', '    만일 "warehouse" 역할이 "pick" 행동을 수행한다')
        feature_line(sid, '"pick-applied" assertion', f'"{first}" assertion',
                     '    그러면 "pick-applied" assertion으로 "만료 전 예약20 warehouse pick = APPLIED, 출고 거부 이유는 만료뿐"를 확인한다', before=True)
        observation_line(sid, 'pick-applied', first, PICK_TEXT, template['oracleRef']['oracleId'], 'boundary-recheck')
        if sid == 'expiry-sweeper':
            for aid, pointer, expected, line in (('dispatch-after-sweep-rejected', '/response/outcome', 'REJECTED', 'sweep 뒤 pick된 예약20 출고 = REJECTED'),
                                                 ('dispatch-after-sweep-code', '/response/error/code', 'INSUFFICIENT_ELIGIBLE_QUANTITY', '거부 code = INSUFFICIENT_ELIGIBLE_QUANTITY')):
                put_before(sub['assertions'], 'allocation-after-boundary-1', new(aid, 'dispatch', pointer, expected, CODE_TEXT))
                feature_line(sid, f'"{aid}" assertion', '"allocation-after-boundary-1" assertion', f'    그러면 "{aid}" assertion으로 "{line}"를 확인한다', before=True)
                observation_line(sid, aid, 'allocation-after-boundary-1', CODE_TEXT, template['oracleRef']['oracleId'], 'boundary-recheck')
    save(path, case)


if __name__ == '__main__':
    fixtures(); t16()
    print('authored picked fixture allocations (' + str(sum(len(v) for v in FIXTURE_PICKS.values())) + ') and T16 expiry pick/code pins')
