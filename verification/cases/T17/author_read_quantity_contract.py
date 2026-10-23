"""T17의 물리 원행과 공개 수량/전제 조회를 잇는 정확한 가역층이다.

관측기에 적격성/권한을 만들지 않는다. 301개의 기존 고정 oracle은
source/selector만 바꾸고, 동적 SELL 판정은 제품 응답에서 읽는다.
"""
import copy
import hashlib
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BASE = 'fb58d86663f28ec90dd38322399c384a42de4672'
LEDGER = HERE / 'read-quantity-contract-delta.json'
PREFIX = 'read-quantity-'


def alias(name):
    return {'$alias': name}


def ref(action, pointer):
    return {'$result': {'actionId': action, 'pointer': pointer}}


def source(action, pointer, field=None, where=None):
    result = dict(actionId=action, pointer=pointer)
    if field is not None:
        result['field'] = field
    if where is not None:
        result['where'] = where
    return result


def marked(case):
    return any(row['id'].startswith(PREFIX) for sub in case['subcases']
               for section in ('actions', 'assertions') for row in sub[section])


def restore_case(case):
    case = copy.deepcopy(case)
    if not marked(case):
        return case
    ledger = json.loads(LEDGER.read_text())
    for delta in reversed(ledger['changes']):
        sub = next((s for s in case['subcases'] if s['id'] == delta['subcase']), None)
        if sub is None:
            continue
        rows = sub[delta['section']]
        found = next((row for row in rows if row['id'] == delta['id']), None)
        if found != delta['after']:
            raise ValueError('Read quantity contract drift: ' + sub['id'] + '/' + delta['id'])
        if delta['before'] is None:
            rows.remove(found)
        else:
            rows[rows.index(found)] = copy.deepcopy(delta['before'])
    return case


def apply(case):
    case = restore_case(case)
    for sub in case['subcases']:
        sid = sub['id']
        if sid != 'reserve-pick-dispatch' and not sid.startswith('eligibility-') and sid != 'claim-nopriordispatch':
            continue
        actions = {a['id']: a for a in sub['actions']}
        original = {a['id']: a for a in sub['assertions']}
        template = next(iter(original.values()))
        extras = []

        def proof(name, op, selected, expected, unit=None, unit_source=None):
            a = copy.deepcopy(template)
            for key in ('baseline', 'unit', 'unitSource', 'baselineUnitSource'):
                a.pop(key, None)
            a.update(id=PREFIX + name, op=op, source=selected, expected=expected,
                     evidenceRefs=[selected['actionId'] + ':actual-response-or-host-artifact'],
                     oracleExplanation='공개 수량과 물리 원행·좌표·조건 출처·배분의 독립 대조를 유지한다.')
            if unit is not None:
                a.update(unit=unit, unitSource=unit_source)
            extras.append(a)
            return a

        if sid == 'claim-nopriordispatch':
            query = copy.deepcopy(actions['after-claim'])
            query['id'] = PREFIX + 'prior-dispatch'
            query['request']['id'] = ref('claim', '/response/effects/observationId')
            query['request']['scope']['objectType'] = 'DeliveryObservation'
            query['evidenceRefs'] = [query['id'] + ':api-response']
            sub['actions'].insert(sub['actions'].index(actions['after-claim-db']), query)
            a = original['no-prior-dispatch-24']
            assert a['expected'] == 'DENIED' and a['op'] == 'equals'
            a['source'] = source(query['id'], '/response/data/priorDispatchCheck/status')
            a['evidenceRefs'] = [query['id'] + ':api-response']
            proof('prior-check-exact', 'equals', source(query['id'], '/response/data/priorDispatchCheck'),
                  dict(status='DENIED', linkedDispatchId=None, salesLineId=None,
                       reasonCode='PRIOR_DISPATCH_MISSING', required=True))
            proof('prior-observation-identity', 'equals', source(query['id'], '/response/data/ID'),
                  actions['claim']['request']['slots']['observationId'])
            # This valid UUID is an explicit attempted confirmation input, not a
            # fabricated canonical fact. The missing Dispatch must stop earlier.
            attempt = dict(id=PREFIX + 'confirm-without-dispatch', kind='invoke', actorRef='receiver',
                           route='api', capabilityId='recordDelivery', evidenceRefs=[PREFIX + 'confirm-without-dispatch:api-response'])
            slots = dict(observationId=ref('claim', '/response/effects/observationId'),
                         canonicalOccurrenceId='b5bde020-c8dc-5e55-b793-b2649d95b7bb')
            attempt['request'] = dict(intentKind='RECORD', definitionVersion='definition-v1', capabilityId='recordDelivery',
                                     subjectRefs=[], slots=slots, expectedRevision=1,
                                     provenance={k: 'CONTEXT' for k in slots},
                                     commandIdempotencyKey='T17-claim-nopriordispatch-confirm-no-dispatch')
            # Keep the original before/after observation pair around the claim.
            # The extra command follows that pair, with its own fresh snapshot.
            sub['actions'].append(attempt)
            after = copy.deepcopy(actions['after-claim'])
            after['id'] = PREFIX + 'after-confirm';after['evidenceRefs'] = [after['id'] + ':api-response']
            sub['actions'].append(after)
            obs = copy.deepcopy(actions['after-claim-db'])
            obs['id'] = PREFIX + 'after-confirm-db'
            obs['observation']['snapshotRef'] = ref(after['id'], '/response/snapshotRevision')
            obs['evidenceRefs'] = [obs['id'] + ':raw-rows', obs['id'] + ':source-query', obs['id'] + ':snapshot']
            sub['actions'].append(obs)
            proof('confirmation-held', 'equals', source(attempt['id'], '/response/outcome'), 'HELD')
            proof('confirmation-code', 'equals', source(attempt['id'], '/response/error/code'), 'DELIVERY_UNMATCHED')
            for table in ('segments', 'movements', 'allocations', 'deliveries', 'receiptContributions', 'assessments'):
                a = proof('confirmation-' + table + '-unchanged', 'sameAs', source(obs['id'], '/data/rawRows/' + table), None)
                a['baseline'] = source('after-claim-db', '/data/rawRows/' + table)
            sub['assertions'].extend(extras)
            continue

        query = 'reserved' if sid == 'reserve-pick-dispatch' else 'after-reserve'
        observation = query + '-db'
        detail = '/response/data/segmentEligibility'
        if sid == 'reserve-pick-dispatch':
            for aid, field in [('eligible-after-reserve-4', 'eligibleQuantity'),
                               ('unreserved-after-reserve-6', 'unreservedEligibleQuantity')]:
                a = original[aid]
                a['source'] = source(query, detail, field, {'eligibilityStatus': 'ALLOWED'})
                a['unitSource'] = source(query, detail, 'unit', {'eligibilityStatus': 'ALLOWED'})
                a['evidenceRefs'] = [query + ':api-response']
        else:
            # The legacy compound selector was over a field that never existed
            # on SQL physical rows. U is a fixed original UNKNOWN candidate.
            # The complete UNKNOWN candidate set and every member's zero are
            # separately required below, so B in eligibility-unknown cannot hide.
            a = original['eligibility-response-3']
            a['source'] = source(query, detail, where={'segmentId': alias('U'), 'eligibilityStatus': 'ALLOWED'})
            a['evidenceRefs'] = [query + ':api-response']
            a = original['eligibility-positive-control']
            a['source'] = source(query, detail, where={'segmentId': alias('A'), 'eligibilityStatus': 'ALLOWED'})
            a['evidenceRefs'] = [query + ':api-response']
            unknown = ['B', 'U'] if sid == 'eligibility-unknown' else ['U']
            proof('unknown-candidate-set', 'exactSet',
                  source(query, '/response/data/conditionResults', 'segmentId', {'conditionId': 'REGULATORY', 'status': 'UNKNOWN'}),
                  [alias(n) for n in unknown])
            for name in unknown:
                proof(name.lower() + '-unknown-not-allowed', 'count',
                      source(query, detail, where={'segmentId': alias(name), 'eligibilityStatus': 'ALLOWED'}), 0)
                proof(name.lower() + '-unknown-ranges-empty', 'equals',
                      source(query, detail, 'allowedRanges', {'segmentId': alias(name)}), [[]])

        f = json.loads((HERE/'fixtures'/f'{sid}.json').read_text())
        physical = [(name, row) for name, row in f['aliases'].items()
                    if row['type'] == 'QuantitySegment' and row['itemAlias'] == 'P']
        proof('physical-identities', 'exactSet', source(query, detail, 'segmentId'), [alias(n) for n, _ in physical])
        tuples = [[alias(n), alias('P'), alias(row['lotAlias']), alias('W'), row['quantity'], row['unit'], True]
                  for n, row in physical]
        proof('raw-physical-tuples', 'relationSet',
              source(observation, '/data/rawRows/segments', ['id', 'itemId', 'lotId', 'placeId', 'quantity', 'unit', 'active']), tuples)
        for name, row in physical:
            proof(name.lower() + '-quantity-present', 'fieldsPresent', source(query, detail, where={'segmentId': alias(name)}),
                  ['segmentId', 'unit', 'eligibilityStatus', 'eligibleQuantity', 'unreservedEligibleQuantity', 'allowedRanges', 'conditionResults', 'evaluatorVersion'])
        amount = '60' if sid == 'reserve-pick-dispatch' else '20'
        proof('a-allowed-range', 'equals', source(query, detail, 'allowedRanges', {'segmentId': alias('A')}),
              [[dict(startQuantity='0', quantity=amount)]])
        proof('a-eligible-quantity', 'equals', source(query, detail, 'eligibleQuantity', {'segmentId': alias('A')}), [amount])
        proof('a-evaluator', 'equals', source(query, detail, 'evaluatorVersion', {'segmentId': alias('A')}), ['quality-eligibility-v1'])
        unit_source = source(observation, '/data/rawRows/segments', 'unit', {'id': alias('A')})
        proof('physical-held-conserved', 'sumEquals', source(observation, '/data/rawRows/segments', 'quantity', {'active': True}),
              '100' if sid == 'reserve-pick-dispatch' else '80', 'BOX', unit_source)
        for name in ('segments', 'allocations'):
            proof(name + '-source-complete', 'equals', source(observation, '/data/sourceEvidence/' + name + '/complete'), True)
            proof(name + '-source-sql', 'present', source(observation, '/data/sourceEvidence/' + name + '/sourceQuery/sql'), None)
        # Allocation is an independent SQL range; reservation cannot shrink the
        # physical leaf or the eligibility intersection.
        success = sid in ('reserve-pick-dispatch', 'eligibility-unknown', 'eligibility-conflict', 'eligibility-fefo-with-reason')
        target = 'B' if sid == 'eligibility-fefo-with-reason' else 'A'
        proof('allocation-range', 'relationSet', source(observation, '/data/rawRows/allocations',
              ['id', 'segmentId', 'startQuantity', 'quantity', 'unit', 'status']),
              [[ref('reserve', '/response/effects/allocationId'), alias(target), '0', amount, 'BOX', 'EXECUTABLE']] if success else [])
        if sid != 'reserve-pick-dispatch':
            for name in ('segments', 'movements'):
                a = proof('reserve-' + name + '-unchanged', 'sameAs', source(observation, '/data/rawRows/' + name), None)
                a['baseline'] = source('before-reserve-db', '/data/rawRows/' + name)
                if name == 'segments':
                    # Observer command attribution is not a physical column and
                    # can change after reserve even when the stored leaf does not.
                    columns = ['id', 'itemId', 'lotId', 'quantity', 'unit', 'placeId', 'custodianId', 'active']
                    a['source']['field'] = columns
                    a['baseline']['field'] = columns
        sub['assertions'].extend(extras)
    return case


def restore_feature(text):
    return ''.join(line for line in text.splitlines(keepends=True) if not
                   ('역할이 "' + PREFIX in line or line.startswith('    그러면 "' + PREFIX)))


def apply_feature(text, before, after):
    text = restore_feature(text)
    lines = text.splitlines(keepends=True)
    for sub in after['subcases']:
        old = next(s for s in before['subcases'] if s['id'] == sub['id'])
        old_ids = {a['id'] for a in old['actions']}
        start = next(i for i, line in enumerate(lines) if '의 "' + sub['id'] + '"를 준비한다' in line)
        end = next((i for i in range(start + 1, len(lines)) if '먼저 사례 파일 ' in lines[i]), len(lines))
        block = lines[start:end]
        for pos, action in enumerate(sub['actions']):
            if action['id'] in old_ids:
                continue
            line = f'    만일 "{action.get("actorRef", "시스템")}" 역할이 "{action["id"]}" 행동을 수행한다\n'
            following = next((a for a in sub['actions'][pos + 1:] if a['id'] in old_ids), None)
            point = next((i for i, l in enumerate(block) if following and '역할이 "' + following['id'] + '" 행동' in l), None)
            if point is None:
                point = next(i for i, l in enumerate(block) if l.startswith('    그러면 '))
            block.insert(point, line)
        additions = [a for a in sub['assertions'] if a['id'].startswith(PREFIX)]
        point = max(i for i, l in enumerate(block) if l.startswith('    그러면 ')) + 1
        block[point:point] = [f'    그러면 "{a["id"]}" assertion으로 "{a["oracleExplanation"]}"를 확인한다\n' for a in additions]
        lines[start:end] = block
    return ''.join(lines)


def bytes_of(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


if __name__ == '__main__':
    old_bytes = subprocess.check_output(['git', 'show', BASE + ':verification/cases/T17/case.json'], cwd=ROOT)
    before = json.loads(old_bytes)
    after = apply(before)
    changes = []
    for old, new in zip(before['subcases'], after['subcases']):
        for section in ('actions', 'assertions'):
            prior = {a['id']: a for a in old[section]}
            for row in new[section]:
                if row != prior.get(row['id']):
                    changes.append(dict(subcase=old['id'], section=section, id=row['id'], before=prior.get(row['id']), after=row))
    ledger = dict(baseline=BASE, originalAssertionCount=301, changedOriginalAssertions=13,
                  expectedOperatorOracleUnitChanged=False, fixtureGrantClockBytesChanged=False,
                  baselineCaseSha256=hashlib.sha256(old_bytes).hexdigest(),
                  fixtureSha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((HERE/'fixtures').glob('*.json'))},
                  changes=changes)
    LEDGER.write_bytes(bytes_of(ledger))
    (HERE/'case.json').write_bytes(bytes_of(after))
    feature = subprocess.check_output(['git', 'show', BASE + ':verification/cases/T17/scenario.feature'], cwd=ROOT).decode()
    (HERE/'scenario.feature').write_text(apply_feature(feature, before, after))
    assert restore_case(after) == before
    assert bytes_of(restore_case(after)) == old_bytes
    assert apply(after) == after
    print('T17 read quantity contract:', len(changes), 'exact reversible row deltas')
