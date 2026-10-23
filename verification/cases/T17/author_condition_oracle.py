"""고정 업무 tuple과 독립 원천 identity를 결합하는 가역 T17 계약이다."""
import copy
import hashlib
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = 'd0885a8031eb754e42f35f998a6d7f970b059cee'
LEDGER = HERE / 'condition-oracle-delta.json'
FIELDS = ['segmentId', 'conditionId', 'status', 'evaluatorVersion']
SOURCE_FIELDS = ['segmentId', 'conditionId', 'evidenceRefs']
EVALUATOR = 'quality-eligibility-v1'


def result(pointer):
    return {'$result': {'actionId': 'setup', 'pointer': '/data/sourceFactEvidence/conditions/' + pointer}}


def alias(name):
    return {'$alias': name}


def restore_case(case):
    case = copy.deepcopy(case)
    if not LEDGER.exists():
        return case
    ledger = json.loads(LEDGER.read_text())
    for sub in case['subcases']:
        deltas = [d for d in ledger['changes'] if d['subcase'] == sub['id']]
        # Original untouched cases and pre-layer cases are valid inputs too.
        marked = any(a['id'].startswith('condition-source-') or a['id'] == 'eligibility-alt'
                     for section in ('actions', 'assertions') for a in sub[section])
        marked |= any(a['id'] == 'eligibility-response-1' and a['source']['actionId'] == 'eligibility'
                      for a in sub['assertions'])
        if not marked:
            continue
        for delta in reversed(deltas):
            rows = sub[delta['section']]
            found = next((r for r in rows if r['id'] == delta['id']), None)
            if found != delta['after']:
                raise ValueError('Condition oracle delta drift: ' + sub['id'] + '/' + delta['id'])
            if delta['before'] is None:
                rows.remove(found)
            else:
                rows[rows.index(found)] = copy.deepcopy(delta['before'])
    return case


def apply(case):
    case = restore_case(case)
    for sub in case['subcases']:
        if not sub['id'].startswith('eligibility-'):
            continue
        f = json.loads((HERE / 'fixtures' / (sub['id'] + '.json')).read_text())
        actions = {a['id']: a for a in sub['actions']}
        original = next(a for a in sub['assertions'] if a['id'] == 'eligibility-response-1')
        tuples = copy.deepcopy(original['expected'])
        if original['source']['pointer'] != '/data/rawRows/conditions' or any(t[4] != 'evaluator-v1' for t in tuples):
            raise ValueError('Original heterogeneous mapping required')
        template = copy.deepcopy(original)
        extras = []

        def assertion(name, op, source, expected, item='P', baseline=None):
            a = copy.deepcopy(template)
            a.update(id=name, op=op, source=source, expected=expected,
                     oracleExplanation='원래 고정 업무 status와 실제 설치한 원천 identity를 독립 대조한다.')
            a['scope']['itemId'] = alias(item)
            a['evidenceRefs'] = [source['actionId'] + ':actual-response-or-host-artifact']
            if baseline is not None:
                a['baseline'] = baseline
            return a

        # Observer selection remains typed ITEM P. ALT receives its own explicit read.
        db = actions['after-reserve-db']['observation']
        for source in ('sellConditionSourceRefs', 'sellConditionSourceLinks'):
            if source not in db['sources']:
                db['sources'].append(source)
        items = ['P']
        if any(t[0] == alias('OTHERPACK') for t in tuples):
            items.append('ALT')
            query = copy.deepcopy(actions['eligibility'])
            query['id'] = 'eligibility-alt'
            query['request']['scope']['itemId'] = alias('ALT')
            query['evidenceRefs'] = ['eligibility-alt:api-response']
            observe = copy.deepcopy(actions['after-reserve-db'])
            observe['id'] = 'eligibility-alt-db'
            observe['observation']['scope']['itemId'] = alias('ALT')
            observe['observation']['sources'] = ['sellConditionSourceRefs', 'sellConditionSourceLinks']
            observe['observation']['snapshotRef'] = {'$result': {'actionId': query['id'], 'pointer': '/response/snapshotRevision'}}
            observe['evidenceRefs'] = ['eligibility-alt-db:raw-rows', 'eligibility-alt-db:source-query', 'eligibility-alt-db:snapshot']
            pos = sub['actions'].index(actions['eligibility']) + 1
            sub['actions'][pos:pos] = [query, observe]

        receipts = []
        uninstalled = []
        refs = {item: [] for item in items}
        receipt_index = 0
        for index, row in enumerate(f['baseline']['conditions']):
            item = f['aliases'][row['segmentAlias']]['itemAlias']
            pointer = '/baseline/conditions/' + str(index)
            conflict = row['conditionId'] == 'REGULATORY' and row['status'] == 'CONFLICT'
            if row['status'] != 'ALLOWED' and not conflict:
                uninstalled.append([pointer, row, 'Original ' + row['status'] + ' is not a positive authority'])
                continue
            receipts.append([pointer, row])
            base = str(receipt_index) + '/'
            if conflict:
                proof_ids = [result(base + 'decisionReceipts/' + str(n) + '/canonicalOccurrenceId') for n in range(2)]
                extras.append(assertion('condition-source-conflict-decisions', 'relationSet',
                    {'actionId': 'setup', 'pointer': '/data/sourceFactEvidence/conditions/' + base + 'decisionReceipts', 'field': ['givenEvidenceAlias', 'decision']},
                    [[row['evidenceAliases'][0], 'ALLOWED'], [row['evidenceAliases'][1], 'REVOKED']]))
                extras.append(assertion('condition-source-no-singleton-conflict', 'absent',
                    {'actionId': 'setup', 'pointer': '/data/sourceFactEvidence/conditions/' + base + 'canonicalOccurrenceId'}, None))
            else:
                field = 'documentId' if row['conditionId'] in ('QC', 'DISPOSITION', 'CUSTOMER_CONTRACT') else 'canonicalOccurrenceId'
                proof_ids = [result(base + field)]
            for proof in proof_ids:
                table = 'mulino_inventory_DispositionBases' if row['conditionId'] in ('QC', 'DISPOSITION', 'CUSTOMER_CONTRACT') else 'mulino_trade_regulatory_DecisionVersions' if row['conditionId']=='REGULATORY' else 'mulino_trade_regulatory_LabelVerifications'
                refs[item].append([alias(row['segmentAlias']), row['conditionId'], proof, table])
            receipt_index += 1
        # A permission and a current hold are independent sources for semantic QC DENIED.
        hold = actions['early-hold-source-document']
        hold_segment = actions['early-hold']['request']['slots']['segmentId']
        refs['P'].append([hold_segment, 'QC', {'$result': {'actionId': hold['id'], 'pointer': '/response/id'}}, 'mulino_inventory_Restrictions'])

        for item in items:
            query = 'eligibility' if item == 'P' else 'eligibility-alt'
            observation = 'after-reserve-db' if item == 'P' else 'eligibility-alt-db'
            selected = [t for t in tuples if f['aliases'][t[0]['$alias']]['itemAlias'] == item]
            expected = [t[:3] + [EVALUATOR] for t in selected]
            status = assertion('eligibility-response-1' if item == 'P' else 'condition-source-alt-business-status',
                'relationSet', {'actionId': query, 'pointer': '/response/data/conditionResults', 'field': FIELDS}, expected, item)
            same = assertion('eligibility-response-2' if item == 'P' else 'condition-source-alt-source-equivalence',
                'sameAs', {'actionId': query, 'pointer': '/response/data/conditionResults', 'field': SOURCE_FIELDS}, None, item,
                {'actionId': observation, 'pointer': '/data/rawRows/sellConditionSourceRefs', 'field': SOURCE_FIELDS})
            if item == 'P':
                for a in (status, same):
                    pos = next(i for i, old in enumerate(sub['assertions']) if old['id'] == a['id'])
                    sub['assertions'][pos] = a
            else:
                extras.extend([status, same])
            extras.append(assertion('condition-source-' + item.lower() + '-installed-identities', 'relationSet',
                {'actionId': observation, 'pointer': '/data/rawRows/sellConditionSourceLinks', 'field': ['segmentId', 'conditionId', 'evidenceId', 'sourceTable']}, refs[item], item))
            extras.append(assertion('condition-source-' + item.lower() + '-typed-links', 'fieldsPresent',
                {'actionId': observation, 'pointer': '/data/rawRows/sellConditionSourceLinks'},
                ['segmentId', 'conditionId', 'evidenceId', 'sourceTable', 'sourceId', 'sourceRow', 'verificationChains'], item))
            permissions = []
            for t in selected:
                if t[1] not in ('QC', 'DISPOSITION', 'CUSTOMER_CONTRACT'):
                    continue
                original_row = next(row for row in f['baseline']['conditions'] if row['segmentAlias'] == t[0]['$alias'] and row['conditionId'] == t[1])
                permissions.append([t[0], t[1], original_row['status'], 'DENIED' if t[0] == hold_segment and t[1] == 'QC' else 'ALLOWED'])
            for kind in ('QC', 'DISPOSITION', 'CUSTOMER_CONTRACT'):
                extras.append(assertion('condition-source-' + item.lower() + '-' + kind.lower() + '-permission-and-hold', 'relationSet',
                    {'actionId': query, 'pointer': '/response/data/conditionResults', 'where': {'conditionId': kind},
                     'field': ['segmentId', 'conditionId', 'permissionStatus', 'restrictionStatus']}, [r for r in permissions if r[1] == kind], item))
        extras.append(assertion('condition-source-receipt-original-pointers', 'relationSet',
            {'actionId': 'setup', 'pointer': '/data/sourceFactEvidence/conditions', 'field': ['pointer', 'given']}, receipts))
        extras.append(assertion('condition-source-unknown-uninstalled', 'relationSet',
            {'actionId': 'setup', 'pointer': '/data/sourceFactEvidence/uninstalledConditions', 'field': ['pointer', 'given', 'reason']}, uninstalled))
        sub['assertions'].extend(extras)
    return case


def apply_feature(text):
    text = restore_feature(text)
    line = '    만일 "observer" 역할이 "eligibility-alt" 행동을 수행한다\n'
    marker = '    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-packaging-unapproved"를 준비한다'
    start = text.index(marker)
    point = text.index('    만일 "observer" 역할이 "eligibility" 행동을 수행한다\n', start)
    point += len('    만일 "observer" 역할이 "eligibility" 행동을 수행한다\n')
    text = text[:point] + line + '    만일 "시스템" 역할이 "eligibility-alt-db" 행동을 수행한다\n' + text[point:]
    current = apply(json.loads((HERE/'case.json').read_text()))
    before = restore_case(current)
    additions = {}
    for old, new in zip(before['subcases'], current['subcases']):
        if not new['id'].startswith('eligibility-'):
            continue
        additions[new['id']] = (old['assertions'][-1]['id'], [a['id'] for a in new['assertions'] if a['id'].startswith('condition-source-')])
    def explanation(name):
        if name.endswith('installed-identities'):
            return '실제 SELL 문서와 canonical 원본 ID가 설치 receipt와 정확히 같다'
        if name.endswith('typed-links'):
            return '실제 typed 원천 행과 검증 chain을 보존한다'
        if name.endswith('permission-and-hold'):
            return '허용 permission과 현재 제한의 ALLOWED·DENIED를 독립 검증한다'
        return {
            'condition-source-conflict-decisions': '원래 두 alias가 ALLOWED와 REVOKED의 두 독립 원본에 각각 연결된다',
            'condition-source-no-singleton-conflict': '상충한 두 결정에는 단일 canonical 근거를 만들지 않는다',
            'condition-source-alt-business-status': 'ALT OTHERPACK의 QC·규제·표시·처분·고객 조건 다섯 개는 ALLOWED다',
            'condition-source-alt-source-equivalence': 'ALT OTHERPACK 공개 조회의 실제 근거가 독립 원천과 같다',
            'condition-source-receipt-original-pointers': '원래 condition pointer·given과 설치 scope·수량·기간이 정확히 같다',
            'condition-source-unknown-uninstalled': 'UNKNOWN에는 양의 판매 허용 근거를 설치하지 않는다',
        }[name]
    output = []
    scenario = None
    for line in text.splitlines(keepends=True):
        if '먼저 사례 파일 ' in line:
            scenario = line.split('의 "', 1)[1].split('"', 1)[0]
        output.append(line)
        if scenario in additions and line.startswith('    그러면 "' + additions[scenario][0] + '" assertion'):
            output.extend(f'    그러면 "{name}" assertion으로 "{explanation(name)}"를 확인한다\n' for name in additions[scenario][1])
    return ''.join(output)


def restore_feature(text):
    for role, name in [('observer', 'eligibility-alt'), ('시스템', 'eligibility-alt-db')]:
        text = text.replace(f'    만일 "{role}" 역할이 "{name}" 행동을 수행한다\n', '')
    return ''.join(line for line in text.splitlines(keepends=True) if not line.startswith('    그러면 "condition-source-'))


if __name__ == '__main__':
    root = HERE.parents[2]
    before = json.loads(subprocess.check_output(['git', 'show', BASE + ':verification/cases/T17/case.json'], cwd=root))
    after = apply(before)
    changes = []
    for old, new in zip(before['subcases'], after['subcases']):
        for section in ('actions', 'assertions'):
            prior = {r['id']: r for r in old[section]}
            for row in new[section]:
                if prior.get(row['id']) != row:
                    changes.append(dict(subcase=old['id'], section=section, id=row['id'], before=prior.get(row['id']), after=row))
    fixture_hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((HERE/'fixtures').glob('*.json'))}
    ledger = dict(baseline=BASE, originalAssertionCount=257, originalBusinessStatusesChanged=False,
                  fixtureGrantClockBytesChanged=False, fixtureSha256=fixture_hashes, changes=changes)
    LEDGER.write_text(json.dumps(ledger, ensure_ascii=False, indent=2) + '\n')
    (HERE/'case.json').write_text(json.dumps(after, ensure_ascii=False, indent=2) + '\n')
    feature = subprocess.check_output(['git', 'show', BASE + ':verification/cases/T17/scenario.feature'], cwd=root).decode()
    (HERE/'scenario.feature').write_text(apply_feature(feature))
    assert restore_case(after) == before
    assert apply(after) == after
    print('T17 condition oracle:', len(changes), 'reversible deltas')
