#!/usr/bin/env python3
"""Step 2 round 7: explicit receiving custody for direct receipts whose stock is later reserved/dispatched/moved.

Idempotent authoring of hand-authored cases (no generator owns E1 or T13). Evidence for
docs/execution/step2r-round7/README.md; ./verify prepare (ContractValidator.receiptCustodyProblems) is the gate.

contracts/fixture-place-kinds.json directReceiptCustody: a confirmReceipt that does not confirm an existing fixture
QuantitySegment (a direct receipt, no transit leaf whose custody it could keep) creates stock with no custodian unless it
names one. The product (ReceiptCommands, read only) accepts the receivingCustodianId slot only for an internal HUMAN/AGENT
of the organization with current confirmReceipt authority for the place, and only when the verified receipt original
names the same custodian. So:
- E1 (all three subcases): receipt60/receipt40 name `receiver` (an internal Human, not the confirming `procurement`), the
  warehouse originals warehouse-60/warehouse-40 name the same alias (fixtureContent.receivingCustodianAlias, evidence
  sha256 recomputed over the canonical content) and `receiver` gets the confirmReceipt grant for W. A positive control
  observes, after both receipts and before the first QC hold, that the two received segments are active at W with
  custodian `receiver`, so the later eligible-0 assertions come from the QC/agency/return holds, not missing custody.
- T13 partial-excess-return-relocation: receipt60/40/5 name `warehouse` (the only actor with receive authority there);
  the shared original `warehouse-receipt` becomes a DocumentVersion alias whose content names it, and a DB control checks
  the three received segments at W carry that custodian before the reserve and the move to W-alt.
Run: python3 -I docs/execution/step2r-round7/author_receipt_custody.py
"""
import copy, hashlib, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
SLOT = 'receivingCustodianId'
FIELD = 'receivingCustodianAlias'


def load(p): return json.loads(p.read_text())
def save(p, d): p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
def alias(x): return {'$alias': x}
def result(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}
def canonical_sha(content): return hashlib.sha256(json.dumps(content, sort_keys=True, separators=(',', ':'), ensure_ascii=False).encode()).hexdigest()


def name_in_original(fixture, doc, custodian):
    """The receipt original names the receiving custodian; the evidence row hashes the canonical content."""
    a = fixture['aliases'][doc]
    assert a['type'] == 'DocumentVersion' and a.get('contentEncoding') == 'CANONICAL_JSON_UTF8', doc
    a['fixtureContent'][FIELD] = custodian
    rows = [e for e in fixture['evidence'] if e['alias'] == doc]
    assert len(rows) == 1, doc
    rows[0]['sha256'] = canonical_sha(a['fixtureContent'])


def grant_receive(fixture, actor):
    a = fixture['actors'][actor]
    for key in (a['roleCapabilities'], a['grant']['actions']):
        if 'confirmReceipt' not in key:
            key.append('confirmReceipt')


def insert_after(items, anchor, item):
    items[:] = [x for x in items if x['id'] != item['id']]
    at = [x['id'] for x in items].index(anchor)
    items.insert(at + 1, item)


def feature(case_id, sub_id, action_lines, assertion_anchor, assertion_line, action_anchor):
    """Insert the Gherkin lines of the new actions after action_anchor and of the assertion after assertion_anchor."""
    path = ROOT / f'verification/cases/{case_id}/scenario.feature'
    lines = path.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{sub_id}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith('시나리오')), len(lines))
    new_ids = [i for i, _ in action_lines] + [assertion_line[0]]
    block = [l for l in lines[start:end] if not any(f'"{i}" 행동' in l or f'"{i}" assertion' in l for i in new_ids)]
    at = next(i for i, l in enumerate(block) if f'"{action_anchor}" 행동' in l)
    for offset, (i, actor) in enumerate(action_lines):
        block.insert(at + 1 + offset, f'    만일 "{actor}" 역할이 "{i}" 행동을 수행한다')
    at = next(i for i, l in enumerate(block) if f'"{assertion_anchor}" assertion' in l)
    block.insert(at + 1, f'    그러면 "{assertion_line[0]}" assertion으로 "{assertion_line[1]}"를 확인한다')
    lines[start:end] = block
    path.write_text('\n'.join(lines))


# ---- E1 ----------------------------------------------------------------------------------------------------------
E1_CUSTODIAN = 'receiver'
E1_CONTROL = {
    'full-flow-quantities': ('current-sell-eligible-10', 'current-sell-eligible'),
    'independent-goals-and-owners': ('two-entry-anchor-eligibleQuantity', 'two-entry-snapshot'),
    'whole-runtime-and-model-reference': ('actual-runtime-observation-29', 'actual-runtime-observation'),
}


def e1():
    path = ROOT / 'verification/cases/E1/case.json'
    case = load(path)
    for sub in case['subcases']:
        if sub['id'] not in E1_CONTROL:  # step2r round 8: the declared custody negative receipt-custody-unverified keeps its slot
            continue
        fpath = ROOT / sub['fixtureRef']
        fixture = load(fpath)
        grant_receive(fixture, E1_CUSTODIAN)
        for doc in ('warehouse-60', 'warehouse-40'):
            name_in_original(fixture, doc, E1_CUSTODIAN)
        save(fpath, fixture)
        actions = sub['actions']
        for rid in ('receipt60', 'receipt40'):
            a = next(x for x in actions if x['id'] == rid)
            assert a['capabilityId'] == 'confirmReceipt'
            a['request']['slots'][SLOT] = alias(E1_CUSTODIAN)
        template = next(x for x in actions if x['id'] == 'receipt60-after-doc')
        template_db = next(x for x in actions if x['id'] == 'receipt60-after-doc-db')
        query = copy.deepcopy(template)
        query['id'] = 'received-custody'
        query['evidenceRefs'] = ['received-custody:api-response']
        db = copy.deepcopy(template_db)
        db['id'] = 'received-custody-db'
        db['observation']['snapshotRef'] = result('received-custody', '/response/snapshotRevision')
        db['observation']['sources'] = ['segments']
        db['evidenceRefs'] = ['received-custody-db:raw-rows', 'received-custody-db:source-query', 'received-custody-db:snapshot']
        insert_after(actions, 'receipt40', query)
        insert_after(actions, 'received-custody', db)
        anchor, observation = E1_CONTROL[sub['id']]
        base = next(x for x in sub['assertions'] if x['id'] == anchor)
        control = {
            'id': 'received-custody-control', 'op': 'relationSet',
            'source': {'actionId': 'received-custody-db', 'pointer': '/data/rawRows/segments',
                       'where': {'locationId': alias('W'), 'active': True},
                       'field': ['id', 'quantity', 'unit', 'locationId', 'custodianId']},
            'expected': [[result('receipt60', '/response/segmentId'), '60', 'BOX', alias('W'), alias(E1_CUSTODIAN)],
                         [result('receipt40', '/response/segmentId'), '40', 'BOX', alias('W'), alias(E1_CUSTODIAN)]],
            'requirementRefs': base['requirementRefs'], 'evidenceRefs': ['received-custody-db:raw-rows'], 'scope': base['scope'],
            'oracleExplanation': '양성 대조: 두 직접 수령 뒤·첫 QC 보류 전 독립 DB 원행에서 INTERNAL_STORAGE W의 활성 실물은 수령60·수령40 두 행뿐이고 '
                                 '둘 다 수령 원본과 명령 slot이 지명한 내부 보관자 receiver(확인한 procurement가 아님)가 보관한다. '
                                 '그래서 뒤의 판매 적격0은 보관 미확인이 아니라 QC·기관·반품 보류 때문이며 예약·출고30도 보관 검사에 막히지 않는다.',
            'oracleRef': {'oracleId': base['oracleRef']['oracleId'], 'observationNames': [observation]},
        }
        insert_after(sub['assertions'], anchor, control)
        feature('E1', sub['id'], [('received-custody', 'observer'), ('received-custody-db', '시스템')], anchor,
                ('received-custody-control', '보류 전 W 활성 실물 = 수령60 BOX·수령40 BOX, 둘 다 내부 보관자 receiver'), 'receipt40')
    save(path, case)


# ---- T13 ---------------------------------------------------------------------------------------------------------
T13_SUB = 'partial-excess-return-relocation'
T13_CUSTODIAN = 'warehouse'


def t13():
    path = ROOT / 'verification/cases/T13/case.json'
    case = load(path)
    sub = next(s for s in case['subcases'] if s['id'] == T13_SUB)
    fpath = ROOT / sub['fixtureRef']
    fixture = load(fpath)
    row = next(e for e in fixture['evidence'] if e['alias'] == 'warehouse-receipt')
    fixture['aliases']['warehouse-receipt'] = {
        'type': 'DocumentVersion',
        'fixtureContent': {'synthetic': True, 'sourceNamespace': row['sourceNamespace'], 'externalEventId': row['externalEventId'],
                           'sourceVersion': row['sourceVersion'], 'occurredAt': row['occurredAt'], 'itemAlias': 'P', 'lotAlias': 'L',
                           'witnessKind': 'warehouse-receipt', 'reportedQuantities': [{'value': q, 'unit': 'BOX'} for q in ('60', '40', '5')]},
        'contentEncoding': 'CANONICAL_JSON_UTF8'}
    name_in_original(fixture, 'warehouse-receipt', T13_CUSTODIAN)
    assert 'confirmReceipt' in fixture['actors'][T13_CUSTODIAN]['grant']['actions']
    save(fpath, fixture)
    for rid in ('receipt60', 'receipt40', 'receipt5'):
        a = next(x for x in sub['actions'] if x['id'] == rid)
        assert a['capabilityId'] == 'confirmReceipt' and a['request']['evidenceRefs'] == ['warehouse-receipt']
        a['request']['slots'][SLOT] = {'value': alias(T13_CUSTODIAN), 'provenance': 'CONTEXT'}
    base = next(x for x in sub['assertions'] if x['id'] == 'held-db105')
    control = {
        'id': 'received-custody-control', 'op': 'relationSet',
        'source': {'actionId': 'receipt-db', 'pointer': '/data/rawRows/segments', 'where': {'locationId': alias('W'), 'active': True},
                   'field': ['id', 'quantity', 'unit', 'custodianId']},
        'expected': [[result(r, '/response/segmentId'), q, 'BOX', alias(T13_CUSTODIAN)] for r, q in (('receipt60', '60'), ('receipt40', '40'), ('receipt5', '5'))],
        'requirementRefs': base['requirementRefs'], 'evidenceRefs': base['evidenceRefs'], 'scope': base['scope'],
        'oracleExplanation': '양성 대조: 세 직접 수령 뒤 W의 활성 실물 60·40·5 BOX는 수령 원본과 명령 slot이 지명한 내부 보관자 warehouse가 보관한다. '
                             '그래서 수령60의 예약·출고와 수령40의 W-alt 이동이 보관 미확인으로 막히지 않고, 이동 뒤 구매 기여 불변을 실제로 관찰한다.',
        'oracleRef': {'oracleId': base['oracleRef']['oracleId'], 'observationNames': ['relocation-added-to-purchase']},
    }
    insert_after(sub['assertions'], 'held-db105', control)
    save(path, case)
    fpath = ROOT / 'verification/cases/T13/scenario.feature'
    lines = fpath.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{T13_SUB}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith('시나리오')), len(lines))
    block = [l for l in lines[start:end] if '"received-custody-control" assertion' not in l]
    at = next(i for i, l in enumerate(block) if '"held-db105" assertion' in l)
    block.insert(at + 1, '    그러면 "received-custody-control" assertion으로 "W 활성 실물 60·40·5 BOX, 셋 다 내부 보관자 warehouse"를 확인한다')
    lines[start:end] = block
    fpath.write_text('\n'.join(lines))


def readme_e1():
    """Keep the E1 README oracle table in step with the new control assertion."""
    path = ROOT / 'verification/cases/E1/README.md'
    text = path.read_text()
    for sub, (anchor, observation) in E1_CONTROL.items():
        prefix = f'| E1.{sub} | {observation} → '
        lines = text.split('\n')
        for i, l in enumerate(lines):
            if l.startswith(prefix) and f'{sub}:received-custody-control' not in l:
                lines[i] = l.replace(f'{sub}:{anchor}', f'{sub}:{anchor}, {sub}:received-custody-control', 1)
        text = '\n'.join(lines)
    path.write_text(text)


def readme_t13():
    path = ROOT / 'verification/cases/T13/README.md'
    text = path.read_text()
    row = '| T13.partial-and-excess-contributions / relocation-added-to-purchase | partial-excess-return-relocation/relocation-no-new-contribution'
    if row + ' |' in text:
        text = text.replace(row + ' |', row + ', partial-excess-return-relocation/received-custody-control |')
    path.write_text(text)


if __name__ == '__main__':
    e1(); t13(); readme_e1(); readme_t13()
    print('authored E1 receipt custody (3 subcases) and T13 ' + T13_SUB + ' receipt custody')
