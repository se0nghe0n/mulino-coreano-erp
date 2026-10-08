#!/usr/bin/env python3
"""Step 2 round 6: place-kind negative control (C1) and positive eligibility controls (T16, T17).

Idempotent authoring of hand-authored cases (no generator owns C1/T16/T17). Evidence for
docs/execution/step2r-round6/README.md; ./verify prepare is the gate.
- C1 `unrecognized-place-kind`: the same consignment 40 (all conditions ALLOWED, disposition
  CONFIRMED, internal custodian) once at INTERNAL_STORAGE W and once at a Place whose kind
  WAREHOUSE is outside contracts/fixture-place-kinds.json (kindControl=UNRECOGNIZED_PLACE_KIND).
  The first is eligible 40 ALLOWED, the second eligible 0 with status UNKNOWN, never a known 0
  (DENIED) or 40; item scope holds 80 and is eligible 40.
- T16 `provisional-holds`: after confirmation and before any hold the received 60 at W is
  eligible 60, so the later 0s come from the provisional status and the remaining RECALL hold.
- T17 `eligibility-*`: segment A is ALLOWED in the same DB observation in which the count of
  ALLOWED segments with an UNKNOWN regulatory condition is 0.
Run: python3 -I docs/execution/step2r-round6/author_place_kind_controls.py
"""
import copy, hashlib, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
C1 = ROOT / 'verification/cases/C1'
NEG = 'unrecognized-place-kind'
NEG_FIXTURE = 'verification/cases/C1/fixtures/unrecognized-place-kind.json'


def load(p): return json.loads(p.read_text())
def save(p, d): p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
def alias(x): return {'$alias': x}


def c1_fixture():
    f = load(C1 / 'fixtures/custody-not-sale.json')
    f['fixtureId'] = 'C1-unrecognized-place-kind'
    al = f['aliases']
    del al['CUS60']
    al['W-UNRECOGNIZED'] = {'type': 'Place', 'name': '자유 기재 창고', 'kind': 'WAREHOUSE', 'kindControl': 'UNRECOGNIZED_PLACE_KIND'}
    unk = copy.deepcopy(al['CON40'])
    unk.update(locationAlias='W-UNRECOGNIZED', physicalScope='UNK40-physical')
    al['UNK40'] = unk
    b = f['baseline']
    b['segments'] = [s for s in b['segments'] if s['alias'] != 'CUS60'] + [{'alias': 'UNK40', 'quantity': '40', 'unit': 'BOX'}]
    facts = [x for x in b['eligibilityFacts'] if x['segmentAlias'] == 'CON40']
    twin = copy.deepcopy(facts[0])
    twin['segmentAlias'] = 'UNK40'
    twin['basisProposalHash'] = hashlib.sha256(b'C1-unrecognized-place-kind UNK40 SELL basis v1').hexdigest()
    b['eligibilityFacts'] = facts + [twin]
    # step2r round 7 (closure review 4 P3): keep the source fixture's sale-source places and add the negative place, so the
    # place kind is the only difference between CON40 and UNK40 (no fixture policy confirms UNK40 false).
    sources = list(b['policies'].get('saleSourcePlaceAliases', []))
    b['policies']['saleSourcePlaceAliases'] = sources + ([] if 'W-UNRECOGNIZED' in sources else ['W-UNRECOGNIZED'])
    for actor in f['actors'].values():
        scope = actor['grant']['scope']
        for key, old, new in [('segmentAliases', 'CUS60', 'UNK40'), ('physicalRootAliases', 'CUS60', 'UNK40')]:
            if key in scope:
                scope[key] = [new if x == old else x for x in scope[key]]
        if 'placeAliases' in scope and 'W-UNRECOGNIZED' not in scope['placeAliases']:
            scope['placeAliases'].append('W-UNRECOGNIZED')
    for e in f['evidence']:
        e['externalEventId'] = e['externalEventId'].replace('C1-custody-not-sale', 'C1-unrecognized-place-kind')
    f['responsibilities'] = [r for r in f['responsibilities'] if 'CUS60' not in json.dumps(r)]
    return f


def c1_subcase(template):
    scope = {'organizationId': alias('ORG'), 'itemId': alias('P'), 'caseKey': 'C1-' + NEG}
    t = {'asOf': '2026-10-07T09:00:00Z', 'knownAt': '2026-10-07T09:00:01Z'}
    def eligibility(i, seg):
        return {'id': i, 'kind': 'query', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': 'evaluateEligibility',
                'request': {'scope': {'organizationId': alias('ORG'), 'segmentId': alias(seg)}, 'action': 'SELL', 'customerId': alias('CUSTOMER'), **t},
                'evidenceRefs': [i + ':actual-adapter-artifact']}
    before_db = next(a for a in template['actions'] if a['id'] == 'before-db')
    db = copy.deepcopy(before_db)
    db['id'] = 'inventory-db'
    db['observation']['scope'] = scope
    db['observation']['snapshotRef'] = {'$result': {'actionId': 'inventory', 'pointer': '/response/snapshotRevision'}}
    db['evidenceRefs'] = ['inventory-db:actual-adapter-artifact']
    actions = [{'id': 'setup', 'kind': 'installFixture', 'fixtureRef': NEG_FIXTURE, 'evidenceRefs': ['setup:actual-adapter-artifact']},
               eligibility('known-kind', 'CON40'), eligibility('unrecognized-kind', 'UNK40'),
               {'id': 'inventory', 'kind': 'query', 'actorRef': 'warehouse', 'route': 'api', 'capabilityId': 'getInventory',
                'request': {'scope': scope, 'action': 'SELL', 'customerId': alias('CUSTOMER'), **t}, 'evidenceRefs': ['inventory:actual-adapter-artifact']},
               db]
    def a(i, op, src, ptr, expected, explain, obs, unit=False, **extra):
        x = {'id': i, 'op': op, 'source': {'actionId': src, 'pointer': ptr, **extra}, 'expected': expected, 'requirementRefs': ['D05'],
             'evidenceRefs': [src + ':actual-adapter-artifact'], 'scope': scope, 'oracleExplanation': explain,
             'oracleRef': {'oracleId': 'C1.initial-eligibility', 'observationNames': [obs]}}
        if unit:
            x['unit'] = 'BOX'
            x['unitSource'] = {'actionId': src, 'pointer': '/response/data/unit'}
        return x
    assertions = [
        a('known-kind-eligible-40', 'decimalEquals', 'known-kind', '/response/data/eligibleQuantity', '40',
          '양성 대조: INTERNAL_STORAGE W에서 내부 custodian이 보관하고 QC·규제·고객·처분이 모두 허용된 위탁40은 SELL 적격 40 BOX다.', 'sell-eligible', unit=True),
        a('known-kind-allowed', 'equals', 'known-kind', '/response/data/eligibilityStatus', 'ALLOWED',
          '같은 위탁40의 판정 상태는 ALLOWED다. 아래 0이 다른 조건 부족이 아니라 장소 종류 때문임을 보이는 대조다.', 'sell-eligible'),
        a('unrecognized-kind-eligible-0', 'decimalEquals', 'unrecognized-kind', '/response/data/eligibleQuantity', '0',
          '반례: 조건이 같은 UNK40이 계약 어휘 밖 Place.kind(WAREHOUSE, kindControl=UNRECOGNIZED_PLACE_KIND)에 있으면 적격 0 BOX다. 미확인 보관을 적격으로 세지 않는다.', 'sell-eligible', unit=True),
        a('unrecognized-kind-unknown', 'equals', 'unrecognized-kind', '/response/data/eligibilityStatus', 'UNKNOWN',
          '그 0은 확정 거부(DENIED)가 아니라 UNKNOWN이다. 인식하지 못한 장소 종류는 외부 보관의 증거가 아니다(계획 §3.1).', 'sell-eligible'),
        a('item-held-80', 'decimalEquals', 'inventory', '/response/data/heldQuantity', '80',
          '품목 범위 보유량은 두 실물 40+40=80 BOX다. 장소 종류를 모른다고 실물을 지우지 않는다.', 'sell-eligible', unit=True),
        a('item-eligible-40', 'decimalEquals', 'inventory', '/response/data/eligibleQuantity', '40',
          '품목 범위 판매 적격은 INTERNAL_STORAGE의 40 BOX뿐이다. 80이면 미확인 장소를 적격으로 센 것이다.', 'sell-eligible', unit=True),
        a('installed-physical-rows', 'relationSet', 'inventory-db', '/data/rawRows/segments',
          [[alias('CON40'), '40', 'BOX', alias('W')], [alias('UNK40'), '40', 'BOX', alias('W-UNRECOGNIZED')]],
          '독립 DB 원행에 두 실물의 ID·수량·단위·위치가 설치 그대로 있다. 반례의 0은 실물 누락이 아니다.', 'held',
          where={'active': True}, field=['id', 'quantity', 'unit', 'locationId']),
    ]
    assertions[-1]['evidenceRefs'] = ['inventory-db:actual-adapter-artifact']
    return {'id': NEG, 'title': '계약 어휘 밖 장소 종류의 위탁40은 적격0·UNKNOWN이고 내부 보관 위탁40만 적격이다',
            'fixtureRef': NEG_FIXTURE, 'requiredAdapters': ['fixture', 'api', 'db'],
            'oracleExplanation': '같은 조건의 위탁40 두 묶음 중 INTERNAL_STORAGE 내부 보관분만 SELL 적격40이고, 어휘 밖 장소 종류(WAREHOUSE)의 40은 적격0·UNKNOWN이다. 품목 보유80·적격40. contracts/fixture-place-kinds.json의 명시 반례다.',
            'actions': actions, 'assertions': assertions}


def feature_block(sub):
    lines = ['', '  시나리오: ' + sub['title'], f'    먼저 사례 파일 "verification/cases/C1/case.json"의 "{sub["id"]}"를 준비한다']
    for a in sub['actions']:
        lines.append(f'    만일 "{a.get("actorRef", "시스템")}" 역할이 "{a["id"]}" 행동을 수행한다')
    explain = {'known-kind-eligible-40': '내부 보관 W의 위탁 판매 적격 = 40 BOX', 'known-kind-allowed': '내부 보관 위탁40의 판정 = ALLOWED',
               'unrecognized-kind-eligible-0': '어휘 밖 장소 종류(WAREHOUSE)의 위탁 판매 적격 = 0 BOX',
               'unrecognized-kind-unknown': '어휘 밖 장소 종류의 판정 = UNKNOWN(확정 거부 아님)',
               'item-held-80': '품목 보유 = 40+40 = 80 BOX', 'item-eligible-40': '품목 판매 적격 = 내부 보관 40 BOX만',
               'installed-physical-rows': '설치 실물 CON40 40 BOX@W, UNK40 40 BOX@W-UNRECOGNIZED'}
    for x in sub['assertions']:
        lines.append(f'    그러면 "{x["id"]}" assertion으로 "{explain[x["id"]]}"를 확인한다')
    return '\n'.join(lines) + '\n'


def author_c1():
    save(ROOT / NEG_FIXTURE, c1_fixture())
    case = load(C1 / 'case.json')
    case['subcases'] = [s for s in case['subcases'] if s['id'] != NEG]
    sub = c1_subcase(case['subcases'][0])
    case['subcases'].append(sub)
    save(C1 / 'case.json', case)
    feature = (C1 / 'scenario.feature').read_text()
    marker = f'"{NEG}"를 준비한다'
    if marker in feature:
        feature = feature[:feature.index('\n\n  시나리오: ' + sub['title'])] + '\n'
    (C1 / 'scenario.feature').write_text(feature.rstrip('\n') + '\n' + feature_block(sub))


def control(i, source, explain, oracle, observation, scope, op='decimalEquals', pointer='/response/data/eligibleQuantity', expected='60', unit=True, **extra):
    x = {'id': i, 'op': op, 'source': {'actionId': source, 'pointer': pointer, **extra}, 'expected': expected, 'requirementRefs': [],
         'evidenceRefs': [source + ':actual-adapter-artifact'], 'scope': scope, 'oracleExplanation': explain,
         'oracleRef': {'oracleId': oracle, 'observationNames': [observation]}}
    if unit:
        x['unit'] = 'BOX'
        x['unitSource'] = {'actionId': source, 'pointer': '/response/data/unit'}
    return x


def add_assertion(case_id, sub_id, after_id, assertion, gherkin):
    path = ROOT / f'verification/cases/{case_id}/case.json'
    case = load(path)
    sub = next(s for s in case['subcases'] if s['id'] == sub_id)
    sub['assertions'] = [x for x in sub['assertions'] if x['id'] != assertion['id']]
    ids = [x['id'] for x in sub['assertions']]
    sub['assertions'].insert(ids.index(after_id) + 1, assertion)
    save(path, case)
    fpath = ROOT / f'verification/cases/{case_id}/scenario.feature'
    lines = fpath.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{sub_id}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith('시나리오')), len(lines))
    block = [l for l in lines[start:end] if f'"{assertion["id"]}" assertion' not in l]
    at = next(i for i, l in enumerate(block) if f'"{after_id}" assertion' in l)
    block.insert(at + 1, f'    그러면 "{assertion["id"]}" assertion으로 "{gherkin}"를 확인한다')
    lines[start:end] = block
    fpath.write_text('\n'.join(lines))


def author_t16():
    case = load(ROOT / 'verification/cases/T16/case.json')
    sub = next(s for s in case['subcases'] if s['id'] == 'provisional-holds')
    scope = next(x for x in sub['assertions'] if x['id'] == 'provisional-eligible-1')['scope']
    x = control('confirmed-eligible-control', 'confirmed-api',
                '양성 대조: 확인 수령 직후·보류 전에는 INTERNAL_STORAGE W에서 내부 custodian이 보관하는 TRANSIT60 유래 60 BOX가 모든 조건 허용이라 판매 적격 60 BOX다. 그래서 임시 접수의 0과 QC만 해제한 뒤의 0은 장소 종류가 아니라 임시 상태와 남은 RECALL60 때문이다.',
                'T16.provisional-and-independent-holds', 'eligible-after-QC-only-release', scope)
    x['requirementRefs'] = ['D16']
    add_assertion('T16', 'provisional-holds', 'receipt-transit-double-creation-4', x, '확인 수령 직후·보류 전 W 판매 적격 = 60 BOX(양성 대조)')


def author_t17():
    case = load(ROOT / 'verification/cases/T17/case.json')
    for sub in case['subcases']:
        if not sub['id'].startswith('eligibility-'):
            continue
        base = next(x for x in sub['assertions'] if x['id'] == 'eligibility-response-3')
        x = {'id': 'eligibility-positive-control', 'op': 'count',
             'source': {'actionId': base['source']['actionId'], 'pointer': '/data/rawRows/segments', 'where': {'id': alias('A'), 'eligibilityStatus': 'ALLOWED'}},
             'expected': 1, 'requirementRefs': ['D17'], 'evidenceRefs': base['evidenceRefs'], 'scope': base['scope'],
             'oracleExplanation': '양성 대조: 같은 DB 관찰에서 INTERNAL_STORAGE W·내부 custodian의 A는 ALLOWED 1행이다. 그래서 eligibility-response-3의 0(규제 UNKNOWN인데 ALLOWED인 실물 0)은 모든 실물이 장소 때문에 미확인인 공허한 0이 아니다.',
             'oracleRef': {'oracleId': 'T17.eligibility-fefo-contract', 'observationNames': ['eligibility-response']}}
        add_assertion('T17', sub['id'], 'eligibility-response-3', x, 'W 내부 보관 A의 판정 = ALLOWED 1행(양성 대조)')


def registry():
    path = ROOT / 'verification/cases/registry.json'
    r = load(path)
    c1 = next(c for c in r['cases'] if c['caseId'] == 'C1')
    if NEG not in c1['subcaseIds']:
        c1['subcaseIds'].append(NEG)
    r['expectedSubcases'] = sum(len(load(ROOT / c['path'])['subcases']) for c in r['cases'])
    save(path, r)


if __name__ == '__main__':
    author_c1(); author_t16(); author_t17(); registry()
    print('authored C1/' + NEG + ', T16 positive control, T17 positive controls')
