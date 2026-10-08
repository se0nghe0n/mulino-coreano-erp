#!/usr/bin/env python3
"""Step 2 round 8: transit receipts a conforming product can accept, a declared custody negative, and T13 move/return outcomes.

Idempotent authoring of hand-authored cases (no generator owns C2, T09, T13, T14, T16 or E1). Evidence for
docs/execution/step2r-round8/README.md; ./verify prepare (ContractValidator.receiptCustodyProblems) is the gate.

contracts/fixture-place-kinds.json transitReceipt: the product (ReceiptStockPrimitives.receive, read only) accepts a transit
receipt only for one identified leaf at a TRANSIT Place with the same item/LOT/unit and exactly the received quantity, and
partial cargo is split first (plan §4.2). Step 2 closure review 5 (P2) found receipts of EXTERNAL_PORT leaves and a partial
receipt of a 100 BOX leaf. So:
- C2 and T09 cumulative-versus-state: the leaves A60 and B40 move from PORT (EXTERNAL_PORT) to a TRANSIT Place alias;
  their internal custodian `warehouse` stays. No assertion read PORT.
- T16 provisional-holds: TRANSIT60 moves from PORT (IT-port, EXTERNAL_PORT) to a TRANSIT Place alias, keeping CUSTODIAN;
  the grant place scopes and receipt-transit-double-creation-7 (no active stock left at the leaf's place after the
  confirm) follow the place.
- T14 discrepancy-transit2 / discrepancy-unobserved2: warehouse splits Q100 into RECEIVED98 + REMAINDER2 before
  receive98, which then confirms exactly the 98 child; transit2 records the in-transit leg on the 2 child (it used the
  receipt's /response/remainder). The DB sums read active rows only (the split and the receipt retire their parents), and
  new assertions pin the split, the consumed 98 child and the active 2 child.
- T13 partial-excess-return-relocation (closure review 5 P3 c): moveQuantity moves a whole leaf, so warehouse first splits
  receipt40 into MOVE20 + STAY20; return-db and move-db also read segments, and the dispatch, return and move outcomes and
  the W / W-alt quantities are asserted before the sameAs contribution checks.
- E1 receipt-custody-unverified (closure review 5 P3 a): an E1 sibling whose receipt60 names `procurement` (internal, with
  receive authority at W) while the warehouse original names `receiver`; declared custodyControl EVIDENCE_UNVERIFIED,
  pinned HELD/EVIDENCE_UNVERIFIED and zero effect (no W stock, no procurement custody, no receipt row).
Run: python3 -I docs/execution/step2r-round8/author_transit_receipts.py
"""
import copy, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
TRANSIT_PLACE = {'type': 'Place', 'name': '이탈리아발 운송 구간', 'kind': 'TRANSIT'}


def load(p): return json.loads(p.read_text())
def save(p, d): p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
def alias(x): return {'$alias': x}
def result(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}
def typed(v, provenance='CONTEXT'): return {'value': v, 'provenance': provenance}


def rename_place(fixture, old, new, place):
    """Replace Place alias `old` by `new` (same position) and repoint every segment and grant place list."""
    aliases = fixture['aliases']
    if old in aliases:
        fixture['aliases'] = {(new if k == old else k): (copy.deepcopy(place) if k == old else v) for k, v in aliases.items()}
    for seg in list(fixture['aliases'].values()) + fixture.get('baseline', {}).get('segments', []):
        for key in ('locationAlias', 'placeAlias'):
            if seg.get(key) == old:
                seg[key] = new
    for actor in fixture.get('actors', {}).values():
        scope = actor.get('grant', {}).get('scope', {})
        for key in ('placeAliases', 'places'):
            if old in scope.get(key, []):
                scope[key] = [new if x == old else x for x in scope[key]]
    assert f'"{old}"' not in json.dumps(fixture, ensure_ascii=False), f'{old} still referenced'


def sub_of(case, sid): return next(s for s in case['subcases'] if s['id'] == sid)
def action(sub, aid): return next(a for a in sub['actions'] if a['id'] == aid)
def assertion(sub, aid): return next(a for a in sub['assertions'] if a['id'] == aid)


def put_after(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor) + 1, item)


def put_before(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    items.insert([x['id'] for x in items].index(anchor), item)


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


def feature_assertion(case_id, sub_id, anchor, aid, text, before=False):
    path, lines, start, end = feature_block(case_id, sub_id)
    block = [l for l in lines[start:end] if f'"{aid}" assertion' not in l]
    at = next(i for i, l in enumerate(block) if f'"{anchor}" assertion' in l)
    block.insert(at if before else at + 1, f'    그러면 "{aid}" assertion으로 "{text}"를 확인한다')
    lines[start:end] = block
    path.write_text('\n'.join(lines))


def readme_row(case_id, prefix, add):
    """Append assertion references to the README oracle-table row that starts with prefix."""
    path = ROOT / f'verification/cases/{case_id}/README.md'
    lines = path.read_text().split('\n')
    for i, l in enumerate(lines):
        if l.startswith(prefix):
            cells = l.rstrip().rstrip('|').rstrip()
            for ref in add:
                if ref not in cells.split(' | ')[-1].split(', '):
                    cells += ', ' + ref
            lines[i] = cells + ' |'
            break
    else:
        raise SystemExit(f'{case_id} README has no row {prefix}')
    path.write_text('\n'.join(lines))


# ---- C2, T09: A60/B40 are transit leaves -----------------------------------------------------------------------------
def cumulative():
    for case_id in ('C2', 'T09'):
        path = ROOT / f'verification/cases/{case_id}/fixtures/cumulative-versus-state.json'
        fixture = load(path)
        rename_place(fixture, 'PORT', 'TRANSIT', TRANSIT_PLACE)
        save(path, fixture)


# ---- T16: TRANSIT60 is a transit leaf ---------------------------------------------------------------------------------
def provisional():
    path = ROOT / 'verification/cases/T16/fixtures/provisional-holds.json'
    fixture = load(path)
    rename_place(fixture, 'PORT', 'TRANSIT', TRANSIT_PLACE)
    save(path, fixture)
    cpath = ROOT / 'verification/cases/T16/case.json'
    case = load(cpath)
    sub = sub_of(case, 'provisional-holds')
    x = assertion(sub, 'receipt-transit-double-creation-7')
    x['source']['where']['locationId'] = alias('TRANSIT')
    x['oracleExplanation'] = ('확인 수령은 TRANSIT 장소의 운송 leaf TRANSIT60을 소비해 W로 옮긴다. 확인 뒤 독립 원행에서 그 운송 장소에 남은 '
                              '활성 실물은 0행이다. 운송 실물과 수령 실물이 함께 활성이면 같은 60이 두 번 생긴 것이다.')
    save(cpath, case)


# ---- T14: split 98+2 before the partial receipt ----------------------------------------------------------------------
T14_SPLIT = 'split98'
CHILD98 = '/response/children/RECEIVED98/segmentId'
CHILD2 = '/response/children/REMAINDER2/segmentId'


def t14():
    path = ROOT / 'verification/cases/T14/case.json'
    case = load(path)
    for sid in ('discrepancy-transit2', 'discrepancy-unobserved2'):
        sub = sub_of(case, sid)
        split = {'id': T14_SPLIT, 'kind': 'invoke', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': 'splitQuantity',
                 'request': {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': 'splitQuantity',
                             'subjectRefs': [{'type': 'TradeItem', 'id': alias('P')}],
                             'slots': {'segmentId': typed(alias('Q100')),
                                       'children': typed([{'alias': 'RECEIVED98', 'quantity': '98', 'unit': 'BOX'},
                                                          {'alias': 'REMAINDER2', 'quantity': '2', 'unit': 'BOX'}], 'USER'),
                                       'reason': typed('부분 수령 전 수령 범위 분리', 'USER')},
                             'expectedRevision': 1, 'commandIdempotencyKey': f'T14-{sid}-{T14_SPLIT}', 'evidenceRefs': ['warehouse-receipt']},
                 'evidenceRefs': [f'{T14_SPLIT}:actual-adapter-artifact']}
        put_after(sub['actions'], 'shipped', split)
        receive = action(sub, 'receive98')
        receive['request']['slots']['existingSegmentId'] = typed(result(T14_SPLIT, CHILD98))
        if sid == 'discrepancy-transit2':
            action(sub, 'transit2')['request']['slots']['segmentId'] = typed(result(T14_SPLIT, CHILD2))
        scope = assertion(sub, 'received98')['scope']
        for aid in ('received98', 'physical-total100') + (('transit2',) if sid == 'discrepancy-transit2' else ()):
            x = assertion(sub, aid)
            for key in ('source', 'unitSource'):
                x[key].setdefault('where', {})['active'] = True
        assertion(sub, 'received98')['oracleExplanation'] = '수령 뒤 W의 활성 실물은 분할 자식98을 수령한 98 BOX다.'
        assertion(sub, 'physical-total100')['oracleExplanation'] = ('분할·수령이 부모를 retire하므로 활성 실물만 더한다. W 98 + 운송 자식 2 = 100 BOX이며 '
                                                                     '수령 차이2를 손실로 지우거나 실물을 중복하지 않는다.')
        if sid == 'discrepancy-transit2':
            assertion(sub, 'transit2')['oracleExplanation'] = '운송 중으로 확인된 나머지는 TRANSIT의 활성 분할 자식 2 BOX다.'
        oracle = 'T14.arrival-discrepancy-not-loss'

        def new(aid, op, source, expected, explain, observation, unit=False):
            x = {'id': aid, 'op': op, 'source': source, 'expected': expected, 'requirementRefs': ['D14'],
                 'evidenceRefs': [source['actionId'] + ':actual-adapter-artifact'], 'scope': scope, 'oracleExplanation': explain,
                 'oracleRef': {'oracleId': oracle, 'observationNames': [observation]}}
            return x
        split_ok = new('split98-applied', 'equals', {'actionId': T14_SPLIT, 'pointer': '/response/outcome'}, 'APPLIED',
                       '부분 수령 전에 출하 실물 Q100을 수령 범위98과 나머지2로 나눈다(계획 §4.2 부분 이동은 먼저 분할). 제품은 운송 leaf의 '
                       '정확한 수량만 수령하므로 이 분할 없이는 수령98이 거부된다.', 'separate-observations')
        consumed = new('received-leaf-retired', 'count',
                       {'actionId': 'db', 'pointer': '/data/rawRows/segments', 'where': {'id': result(T14_SPLIT, CHILD98), 'active': True}}, 0,
                       '수령98은 분할 자식98 전체를 소비한다. 그 운송 자식이 활성으로 남으면 같은 98이 운송과 W에 두 번 있다.', 'received')
        remainder = new('remainder2-active', 'count',
                        {'actionId': 'db', 'pointer': '/data/rawRows/segments',
                         'where': {'id': result(T14_SPLIT, CHILD2), 'locationId': alias('TRANSIT'), 'active': True}}, 1,
                        '수령하지 않은 나머지2는 분할 자식 그대로 TRANSIT에 활성 1행으로 남는다. 손실이나 0으로 바꾸지 않는다.',
                        'transit' if sid == 'discrepancy-transit2' else 'separate-observations')
        put_before(sub['assertions'], 'received-api98', split_ok)
        put_after(sub['assertions'], 'receipt98', consumed)
        put_after(sub['assertions'], 'physical-total100', remainder)
        feature_action('T14', sid, 'shipped', T14_SPLIT, 'warehouse')
        feature_assertion('T14', sid, 'received-api98', 'split98-applied', '수령 전 분할 Q100 → 수령 범위98 + 나머지2 = APPLIED', before=True)
        feature_assertion('T14', sid, 'receipt98', 'received-leaf-retired', '수령98이 소비한 분할 자식98의 활성 행 = 0')
        feature_assertion('T14', sid, 'physical-total100', 'remainder2-active', 'TRANSIT의 활성 분할 자식2 = 1행')
    save(path, case)
    readme_row('T14', '| T14.arrival-discrepancy-not-loss / received |',
               ['discrepancy-transit2/received-leaf-retired', 'discrepancy-unobserved2/received-leaf-retired'])
    readme_row('T14', '| T14.arrival-discrepancy-not-loss / transit |', ['discrepancy-transit2/remainder2-active'])
    readme_row('T14', '| T14.arrival-discrepancy-not-loss / separate-observations |',
               ['discrepancy-transit2/split98-applied', 'discrepancy-unobserved2/split98-applied', 'discrepancy-unobserved2/remainder2-active'])


# ---- T13: split before the partial move, and observed return/move outcomes ------------------------------------------
T13_SUB = 'partial-excess-return-relocation'


def t13():
    path = ROOT / 'verification/cases/T13/case.json'
    case = load(path)
    sub = sub_of(case, T13_SUB)
    split = {'id': 'split40', 'kind': 'invoke', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': 'splitQuantity',
             'request': {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': 'splitQuantity',
                         'subjectRefs': [{'type': 'TradeItem', 'id': alias('P')}],
                         'slots': {'segmentId': typed(result('receipt40', '/response/segmentId')),
                                   'children': typed([{'alias': 'MOVE20', 'quantity': '20', 'unit': 'BOX'},
                                                      {'alias': 'STAY20', 'quantity': '20', 'unit': 'BOX'}], 'USER'),
                                   'reason': typed('부분 장소 이동 전 범위 분리', 'USER')},
                         'expectedRevision': 1, 'commandIdempotencyKey': f'T13-{T13_SUB}-split40', 'evidenceRefs': ['need']},
             'evidenceRefs': ['split40:actual-adapter-artifact']}
    put_after(sub['actions'], 'return-db', split)
    move = action(sub, 'move')
    move['request']['slots']['segmentId'] = typed(result('split40', '/response/children/MOVE20/segmentId'))
    for db in ('return-db', 'move-db'):
        sources = action(sub, db)['observation']['sources']
        if 'segments' not in sources:
            sources.append('segments')
    base = assertion(sub, 'return-no-new-contribution')
    scope = base['scope']

    def new(aid, op, source, expected, explain, observation, unit=False):
        x = {'id': aid, 'op': op, 'source': source, 'expected': expected, 'requirementRefs': ['D13'],
             'evidenceRefs': [source['actionId'] + ':actual-adapter-artifact'], 'scope': scope, 'oracleExplanation': explain,
             'oracleRef': {'oracleId': 'T13.partial-and-excess-contributions', 'observationNames': [observation]}}
        if unit:
            x['unit'] = 'BOX'
            x['unitSource'] = {'actionId': source['actionId'], 'pointer': source['pointer'], 'field': 'unit', 'where': source['where']}
        return x
    w_active = {'locationId': alias('W'), 'active': True}
    alt_active = {'locationId': alias('W-alt'), 'active': True}
    before_return = [
        new('dispatch-sale-applied', 'equals', {'actionId': 'dispatch-sale', 'pointer': '/response/outcome'}, 'APPLIED',
            '수령60에서 예약한 10 BOX가 실제로 출고됐다. 출고가 없으면 아래 반품은 원 구매 기여를 바꿀 기회가 없다.', 'return-added-to-purchase'),
        new('return-applied', 'equals', {'actionId': 'return', 'pointer': '/response/outcome'}, 'APPLIED',
            '고객 반품10 실물 접수가 실제로 적용됐다. 이 결과가 있어야 뒤의 기여 불변이 반품 뒤의 관찰이다.', 'return-added-to-purchase'),
        new('returned-at-W', 'sumEquals', {'actionId': 'return-db', 'pointer': '/data/rawRows/segments', 'field': 'quantity', 'where': w_active},
            '105', '수령 105 − 출고 10 + 반품 10 = W 활성 105 BOX다. 출고가 적용된 뒤이므로 반품10이 W에 실제로 들어온 것이다.',
            'return-added-to-purchase', unit=True),
    ]
    before_move = [
        new('move-applied', 'equals', {'actionId': 'move', 'pointer': '/response/outcome'}, 'APPLIED',
            '수령40에서 분할한 20 BOX의 W→W-alt 내부 이동이 실제로 적용됐다.', 'relocation-added-to-purchase'),
        new('relocated-at-W-alt', 'sumEquals', {'actionId': 'move-db', 'pointer': '/data/rawRows/segments', 'field': 'quantity', 'where': alt_active},
            '20', '이동 뒤 W-alt의 활성 실물은 수령40 계보의 20 BOX다.', 'relocation-added-to-purchase', unit=True),
        new('left-at-W', 'sumEquals', {'actionId': 'move-db', 'pointer': '/data/rawRows/segments', 'field': 'quantity', 'where': w_active},
            '85', '이동 뒤 W는 105 − 20 = 85 BOX다. 수령40의 나머지 20은 W에 남는다.', 'relocation-added-to-purchase', unit=True),
    ]
    for x in before_return:
        put_before(sub['assertions'], 'return-no-new-contribution', x)
    for x in before_move:
        put_before(sub['assertions'], 'relocation-no-new-contribution', x)
    save(path, case)
    feature_action('T13', T13_SUB, 'return-db', 'split40', 'warehouse')
    text = {'dispatch-sale-applied': '판매 출고10 = APPLIED', 'return-applied': '반품10 접수 = APPLIED', 'returned-at-W': 'W 활성 = 105 − 10 + 10 = 105 BOX',
            'move-applied': '분할 20의 W→W-alt 이동 = APPLIED', 'relocated-at-W-alt': 'W-alt 활성 = 20 BOX', 'left-at-W': 'W 활성 = 105 − 20 = 85 BOX'}
    for x in before_return:
        feature_assertion('T13', T13_SUB, 'return-no-new-contribution', x['id'], text[x['id']], before=True)
    for x in before_move:
        feature_assertion('T13', T13_SUB, 'relocation-no-new-contribution', x['id'], text[x['id']], before=True)
    readme_row('T13', '| T13.partial-and-excess-contributions / return-added-to-purchase |', [f'{T13_SUB}/{x["id"]}' for x in before_return])
    readme_row('T13', '| T13.partial-and-excess-contributions / relocation-added-to-purchase |', [f'{T13_SUB}/{x["id"]}' for x in before_move])


# ---- E1: declared custody negative -----------------------------------------------------------------------------------
E1_NEG = 'receipt-custody-unverified'
E1_PREFIX = ['setup', 'purchase', 'approve-purchase', 'transmit-po', 'supplier-accept', 'shipment', 'leg-departure']


def e1():
    path = ROOT / 'verification/cases/E1/case.json'
    case = load(path)
    case['subcases'] = [s for s in case['subcases'] if s['id'] != E1_NEG]
    full = sub_of(case, 'full-flow-quantities')
    actions = [copy.deepcopy(action(full, a)) for a in E1_PREFIX]
    for a in actions:
        key = a.get('request', {}).get('commandIdempotencyKey')
        if key:
            a['request']['commandIdempotencyKey'] = key.replace('E1-full-flow-quantities-', f'E1-{E1_NEG}-')
    receipt = copy.deepcopy(action(full, 'receipt60'))
    receipt['request']['slots']['receivingCustodianId'] = alias('procurement')
    receipt['request']['commandIdempotencyKey'] = f'E1-{E1_NEG}-receipt60'
    receipt['custodyControl'] = 'EVIDENCE_UNVERIFIED'
    query = copy.deepcopy(action(full, 'received-custody'))
    query['id'] = 'after-receipt'
    query['evidenceRefs'] = ['after-receipt:api-response']
    mcp = copy.deepcopy(action(full, 'receipt60-after-doc-mcp'))
    mcp['id'] = 'after-receipt-mcp'
    mcp['request']['snapshotRef'] = result('after-receipt', '/response/snapshotRevision')
    mcp['evidenceRefs'] = ['after-receipt-mcp:mcp-response', 'after-receipt-mcp:actual-mcp-artifact']
    db = copy.deepcopy(action(full, 'received-custody-db'))
    db['id'] = 'after-receipt-db'
    db['observation']['snapshotRef'] = result('after-receipt', '/response/snapshotRevision')
    db['observation']['sources'] = ['segments', 'receipts', 'movements']
    db['evidenceRefs'] = ['after-receipt-db:raw-rows', 'after-receipt-db:source-query', 'after-receipt-db:snapshot']
    actions += [receipt, query, mcp, db]
    base = assertion(full, 'received-custody-control')
    scope, refs = base['scope'], base['requirementRefs']

    def new(aid, op, source, expected, explain, observation, evidence):
        return {'id': aid, 'op': op, 'source': source, 'expected': expected, 'requirementRefs': refs, 'evidenceRefs': [evidence],
                'scope': scope, 'oracleExplanation': explain,
                'oracleRef': {'oracleId': 'E1.full-flow-quantities', 'observationNames': [observation]}}
    assertions = [
        new('custody-unverified-outcome', 'equals', {'actionId': 'receipt60', 'pointer': '/response/outcome'}, 'HELD',
            '반례: 수령60의 receivingCustodianId slot이 내부 actor procurement(W 수령 권한 있음)를 지명하지만 창고 원본 warehouse-60은 '
            'receiver를 지명한다. 보관자는 검증된 증거로만 알 수 있으므로(계획 §4.1–§4.2, §6) 제품은 수령을 HELD로 둔다. slot을 그대로 '
            '보관자로 쓰는 제품은 APPLIED를 내어 실패한다.', 'receipt60-doc-duplicate-effects', 'receipt60:api-response'),
        new('custody-unverified-code', 'equals', {'actionId': 'receipt60', 'pointer': '/response/error/code'}, 'EVIDENCE_UNVERIFIED',
            '보류 이유는 증거 미확인이다. 원본이 지명하지 않은 보관자다(권한 부족 SCOPE_INELIGIBLE이나 상충 EVIDENCE_CONFLICT가 아니다).',
            'receipt60-doc-duplicate-effects', 'receipt60:api-response'),
        new('custody-unverified-no-stock-at-W', 'count',
            {'actionId': 'after-receipt-db', 'pointer': '/data/rawRows/segments', 'where': {'locationId': alias('W'), 'active': True}}, 0,
            '초기 W는 0이고 보류된 수령은 실물을 만들지 않는다. W에 활성 실물이 생기면 미확인 보관으로 재고를 만든 것이다.',
            'current-sell-eligible', 'after-receipt-db:raw-rows'),
        new('custody-unverified-no-custody', 'count',
            {'actionId': 'after-receipt-db', 'pointer': '/data/rawRows/segments', 'where': {'custodianId': alias('procurement')}}, 0,
            'slot이 지명한 procurement를 보관자로 기록한 실물 행은 없다(활성·retired 모두).', 'current-sell-eligible', 'after-receipt-db:raw-rows'),
        new('custody-unverified-no-receipt', 'count', {'actionId': 'after-receipt-db', 'pointer': '/data/rawRows/receipts'}, 0,
            '보류된 수령은 수령 원장 행을 남기지 않는다. 같은 수령을 증거가 지명한 보관자로 다시 확인할 수 있다.',
            'receipt60-doc-duplicate-effects', 'after-receipt-db:raw-rows'),
        new('custody-unverified-mcp-same-snapshot', 'equals', {'actionId': 'after-receipt-mcp', 'pointer': '/response/snapshotRevision'},
            result('after-receipt', '/response/snapshotRevision'), 'MCP 조회는 보류 뒤 API 조회와 같은 snapshotRevision을 읽는다.',
            'receipt60-doc-duplicate-effects', 'after-receipt-mcp:mcp-response'),
    ]
    sub = {'id': E1_NEG, 'title': '수령 원본이 지명하지 않은 내부 보관자를 slot에 쓰면 수령60은 HELD이고 W에 실물·보관이 생기지 않는다',
           'fixtureRef': full['fixtureRef'], 'requiredAdapters': ['fixture', 'api', 'db', 'mcp'],
           'oracleExplanation': ('E1 full-flow-quantities fixture와 구매·출하 선행 명령을 그대로 쓰고, 수령60만 receivingCustodianId를 '
                                 'procurement로 바꾼 선언 반례(custodyControl EVIDENCE_UNVERIFIED)다. 원본 warehouse-60은 receiver를 지명하므로 '
                                 '제품은 HELD·EVIDENCE_UNVERIFIED이고 W 실물0·보관0·수령 원장0이다. 양성 대조는 full-flow-quantities의 '
                                 'received-custody-control이다.'),
           'actions': actions, 'assertions': assertions}
    case['subcases'].append(sub)
    save(path, case)
    fpath = ROOT / 'verification/cases/E1/scenario.feature'
    feature = fpath.read_text()
    marker = f'\n\n  시나리오: {sub["title"]}'
    if marker in feature:
        feature = feature[:feature.index(marker)] + '\n'
    lines = ['', f'  시나리오: {sub["title"]}', f'    먼저 사례 파일 "verification/cases/E1/case.json"의 "{E1_NEG}"를 준비한다']
    for a in actions:
        lines.append(f'    만일 "{a.get("actorRef", "시스템")}" 역할이 "{a["id"]}" 행동을 수행한다')
    text = {'custody-unverified-outcome': '원본이 지명하지 않은 보관자 slot의 수령60 = HELD',
            'custody-unverified-code': '보류 이유 = EVIDENCE_UNVERIFIED',
            'custody-unverified-no-stock-at-W': 'W 활성 실물 = 0행', 'custody-unverified-no-custody': 'procurement 보관 실물 = 0행',
            'custody-unverified-no-receipt': '수령 원장 = 0행', 'custody-unverified-mcp-same-snapshot': 'MCP 조회 snapshot = API 조회 snapshot'}
    for x in assertions:
        lines.append(f'    그러면 "{x["id"]}" assertion으로 "{text[x["id"]]}"를 확인한다')
    fpath.write_text(feature.rstrip('\n') + '\n' + '\n'.join(lines) + '\n')
    rpath = ROOT / 'verification/cases/registry.json'
    registry = load(rpath)
    entry = next(c for c in registry['cases'] if c['caseId'] == 'E1')
    if E1_NEG not in entry['subcaseIds']:
        entry['subcaseIds'].append(E1_NEG)
    registry['expectedSubcases'] = sum(len(load(ROOT / c['path'])['subcases']) for c in registry['cases'])
    save(rpath, registry)
    readme = ROOT / 'verification/cases/E1/README.md'
    text_r = readme.read_text()
    row = f'| {E1_NEG} | 수령60 slot이 원본이 지명하지 않은 procurement를 보관자로 쓰면 HELD·EVIDENCE_UNVERIFIED이고 W 실물·보관·수령 원장은 0이다 |'
    if row not in text_r:
        anchor = next(l for l in text_r.split('\n') if l.startswith('| whole-runtime-and-model-reference |'))
        text_r = text_r.replace(anchor, anchor + '\n' + row, 1)
    readme.write_text(text_r)
    readme_row('E1', '| E1.full-flow-quantities | receipt60-doc-duplicate-effects → ',
               [f'{E1_NEG}:{x}' for x in ('custody-unverified-outcome', 'custody-unverified-code', 'custody-unverified-no-receipt', 'custody-unverified-mcp-same-snapshot')])
    readme_row('E1', '| E1.full-flow-quantities | current-sell-eligible → ',
               [f'{E1_NEG}:{x}' for x in ('custody-unverified-no-stock-at-W', 'custody-unverified-no-custody')])


if __name__ == '__main__':
    cumulative(); provisional(); t14(); t13(); e1()
    print('authored C2/T09/T16 transit leaves, T14 split-before-receipt, T13 split/move/return outcomes, E1 ' + E1_NEG)
