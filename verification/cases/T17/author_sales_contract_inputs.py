"""실제 공개 계약에 맞춘 scope·source selector의 정확한 가역 교정이다.

W40과 원본 257 oracle ID, fixture/grant/clock은 보존한다. 관측 결과를
예상값으로 복사하거나 품질·회수·목표의 미실행을 성공으로 바꾸지 않는다.
"""
import copy
import importlib.util
import hashlib
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
_read_spec = importlib.util.spec_from_file_location('t17_read_quantity', HERE/'author_read_quantity_contract.py')
read_quantity = importlib.util.module_from_spec(_read_spec)
_read_spec.loader.exec_module(read_quantity)

_condition_spec = importlib.util.spec_from_file_location('t17_condition_oracle', HERE/'author_condition_oracle.py')
condition_oracle = importlib.util.module_from_spec(_condition_spec)
_condition_spec.loader.exec_module(condition_oracle)
_recall_spec = importlib.util.spec_from_file_location('t17_recall_inputs', HERE/'author_recall_inputs.py')
recall_inputs = importlib.util.module_from_spec(_recall_spec)
_recall_spec.loader.exec_module(recall_inputs)
_response_spec = importlib.util.spec_from_file_location('t17_response_scope', HERE/'author_response_scope.py')
response_scope = importlib.util.module_from_spec(_response_spec)
_response_spec.loader.exec_module(response_scope)
BASE = 'b4772eb6'
LEDGER = HERE / 'sales-contract-input-delta.json'
POLICY_LEDGER = HERE / 'delivery-assessment-policy-delta.json'
POLICY_ALIAS = 't17DeliveryAssessmentPolicy'
POLICY_META = 'deliveryAssessmentPolicyAuthoring'
POLICY_VERSION = 't17-synthetic-v1'
POLICY_BASE = 'a6e1197705351dbfb69f283c2922a28e4825906d'


def policy_goal_hash(goal):
    return hashlib.sha256(json.dumps(goal, ensure_ascii=False, sort_keys=True,
                                    separators=(',', ':')).encode()).hexdigest()


def policy_goal_delta(before, after):
    paths = ['evidencePolicyVersion', 'defaultContext/evidencePolicyVersion',
             'defaultContext/evidencePolicyVersionId', 'defaultContext/' + POLICY_META,
             'defaultContext/fieldProvenance/evidencePolicyVersion/source']
    changes = []
    for path in paths:
        old, new = before, after
        for key in path.split('/')[:-1]:
            old, new = old[key], new[key]
        key = path.split('/')[-1]
        if key not in old or old[key] != new[key]:
            changes.append(dict(path=path, hadBefore=key in old,
                                before=old.get(key), after=new[key]))
    return dict(beforeSha256=policy_goal_hash(before), afterSha256=policy_goal_hash(after),
                changes=changes)


def delivery_goal_subcases(case):
    return [s['id'] for s in case['subcases']
            if any(a['id'] == 'complete-order-delivery-goal' for a in s['actions'])]


def apply_policy_case(case):
    case = restore_policy_case(case)
    for sub in case['subcases']:
        for action in sub['actions']:
            if action['id'] != 'complete-order-delivery-goal':
                continue
            goal = action['request']['slots']['goal']
            if goal['evidencePolicyVersion'] not in ('fixture-v1', POLICY_VERSION):
                raise ValueError('Unexpected delivery assessment policy version')
            goal['evidencePolicyVersion'] = POLICY_VERSION
            context = goal['defaultContext']
            context['evidencePolicyVersion'] = POLICY_VERSION
            context['evidencePolicyVersionId'] = {'$alias': POLICY_ALIAS}
            context[POLICY_META] = dict(
                synthetic=True, originalAuthorityClaimed=False,
                operatingApprovalClaimed=False,
                selection='Independent EVIDENCE policy; source identity policy remains unchanged',
                policyAlias=POLICY_ALIAS, policyKind='EVIDENCE',
                policyVersion=POLICY_VERSION, policyContentHash=hashlib.sha256(b'{}').hexdigest(),
                effectiveFrom=goal['periodStart'])
            context['fieldProvenance']['evidencePolicyVersion']['source'] = (
                'explicit synthetic EVIDENCE PolicyVersion ' + POLICY_ALIAS)
    return case


def apply_policy_fixtures(case, fixtures):
    fixtures = copy.deepcopy(fixtures)
    selected = delivery_goal_subcases(case)
    for sid in selected:
        f = fixtures[sid]
        if any(a.get('type') == 'PolicyVersion' and a.get('kind') == 'EVIDENCE'
               for name, a in f['aliases'].items() if name != POLICY_ALIAS):
            raise ValueError('An existing EVIDENCE override needs explicit review')
        value = dict(type='PolicyVersion', kind='EVIDENCE', version=POLICY_VERSION,
                     content={}, activate=True, effectiveFrom=f['clock']['asOf'],
                     synthetic=True, authoredBy='coordinator-selected-fixture',
                     originalAuthorityClaimed=False, operatingApprovalClaimed=False)
        metadata = dict(synthetic=True, originalAuthorityClaimed=False,
                        operatingApprovalClaimed=False, policyAlias=POLICY_ALIAS,
                        policyKind='EVIDENCE', policyVersion=POLICY_VERSION,
                        contentSha256=hashlib.sha256(b'{}').hexdigest(),
                        effectiveFrom=f['clock']['asOf'],
                        sourceProfilePolicyChanged=False)
        for mapping, key, after in [(f['aliases'], POLICY_ALIAS, value),
                                    (f['baseline']['policy'], POLICY_META, metadata)]:
            if key in mapping and mapping[key] != after:
                raise ValueError('Delivery assessment policy input drift: ' + sid + '/' + key)
            mapping[key] = after
    return fixtures


def restore_policy_case(case):
    case = copy.deepcopy(case)
    if not POLICY_LEDGER.exists():
        return case
    deltas = json.loads(POLICY_LEDGER.read_text())['goalChanges']
    for sub in case['subcases']:
        if sub['id'] not in deltas:
            continue
        for action in sub['actions']:
            if action['id'] != 'complete-order-delivery-goal':
                continue
            goal = action['request']['slots']['goal']
            delta = deltas[sub['id']]
            if policy_goal_hash(goal) == delta['beforeSha256']:
                continue
            if policy_goal_hash(goal) != delta['afterSha256']:
                raise ValueError('Delivery assessment goal drift: ' + sub['id'])
            for change in reversed(delta['changes']):
                parent = goal
                for key in change['path'].split('/')[:-1]:
                    parent = parent[key]
                key = change['path'].split('/')[-1]
                if change['hadBefore']:
                    parent[key] = copy.deepcopy(change['before'])
                else:
                    del parent[key]
            if policy_goal_hash(goal) != delta['beforeSha256']:
                raise ValueError('Delivery assessment inverse mismatch: ' + sub['id'])
    return case


def restore_policy_fixture(f):
    f = copy.deepcopy(f)
    marked = POLICY_ALIAS in f['aliases'] or POLICY_META in f['baseline']['policy']
    if not marked:
        return f
    delta = json.loads(POLICY_LEDGER.read_text())['fixtureChanges'].get(f['fixtureId'])
    if delta is None or f['aliases'].get(POLICY_ALIAS) != delta['alias'] or f['baseline']['policy'].get(POLICY_META) != delta['metadata']:
        raise ValueError('Delivery assessment fixture policy drift: ' + f['fixtureId'])
    del f['aliases'][POLICY_ALIAS]
    del f['baseline']['policy'][POLICY_META]
    return f


def policy_fixture_bytes(raw):
    value = json.loads(raw)
    restored = restore_policy_fixture(value)
    return raw if restored == value else (json.dumps(restored, ensure_ascii=False, indent=2) + '\n').encode()


def apply(case):
    case = copy.deepcopy(case)
    for sub in case['subcases']:
        actions = {a['id']: a for a in sub['actions']}
        if sub['id'] == 'reserve-pick-dispatch':
            actions['dispatched']['request']['scope']['placeId'] = {'$alias': 'W'}
            actions['dispatched-db']['observation']['scope'].pop('placeId', None)
            item_query=copy.deepcopy(actions['dispatched'])
            item_query['id']='dispatched-item'
            item_query['request']['scope'].pop('placeId', None)
            item_query['evidenceRefs']=['dispatched-item:actual-query-artifact']
            if 'dispatched-item' not in actions:
                sub['actions'].insert(sub['actions'].index(actions['dispatched-db']), item_query)
            actions['dispatched-db']['observation']['snapshotRef']={'$result':{'actionId':'dispatched-item','pointer':'/response/snapshotRevision'}}
            for assertion in sub['assertions']:
                if assertion['id'] in ('in-transit-after-dispatch-14','delivered-before-observation-16','dispatch-allocation-18'):
                    assertion['source']['actionId']='dispatched-item'
        if sub['id'].startswith('claim-'):
            claim = actions['claim']
            # The public intake accepts the caller's actual follow-up action.
            # Preserve the independently authored Korean responsibility oracle.
            claim['request']['slots']['nextAction'] = '원천 주장과 실제 실물을 대조한다'
            event = copy.deepcopy(claim['request']['slots']['eventId'])
            observation=actions['after-claim-db']['observation']
            observation['sources']=['deliveryObservations' if source=='claims' else source for source in observation['sources']]
            for assertion in sub['assertions']:
                source = assertion.get('source', {})
                if source.get('pointer') in ('/data/rawRows/claims','/data/rawRows/deliveryObservations'):
                    source['pointer']='/data/rawRows/deliveryObservations'
                    # Only this report's source claim, not every quality and
                    # regulatory preparation claim about the item, is counted.
                    source.setdefault('where', {})['eventId'] = event
        for assertion in sub['assertions']:
            for key in ('source', 'baseline'):
                source = assertion.get(key, {})
                where = source.get('where', {})
                if where.get('kind') == 'RECONCILIATION':
                    where['kind'] = 'DELIVERY_RECONCILIATION'
                    where['status'] = 'OPEN'
    return apply_policy_case(condition_oracle.apply(case))


def restore_case(case):
    case = response_scope.restore_case(case)
    case = read_quantity.restore_case(case)
    case = restore_policy_case(case)
    case = recall_inputs.restore_case(case)
    case = condition_oracle.restore_case(case)
    if not LEDGER.exists():
        return case
    marked = any(
        any(a.get('source', {}).get('where', {}).get('kind') == 'DELIVERY_RECONCILIATION'
            or a.get('source', {}).get('where', {}).get('eventId') is not None
            for a in sub.get('assertions', []))
        or any(a.get('id') == 'dispatched' and a.get('request', {}).get('scope', {}).get('placeId') == {'$alias': 'W'}
               for a in sub.get('actions', [])) for sub in case['subcases'])
    if not marked:
        return case
    changes = json.loads(LEDGER.read_text())['changes']
    for sub in case['subcases']:
        for change in changes:
            if change['subcase'] != sub['id']:
                continue
            rows = sub[change['section']]
            row = next((r for r in rows if r['id'] == change['id']), None)
            if row not in (change['before'], change['after']):
                raise ValueError('Sales contract input drift: ' + sub['id'] + '/' + change['id'])
            if row == change['after']:
                if change['before'] is None:
                    rows.remove(row)
                else:
                    rows[rows.index(row)] = copy.deepcopy(change['before'])
    return case


def apply_feature(text):
    line='    만일 "observer" 역할이 "dispatched-item" 행동을 수행한다\n'
    if line in text:
        return text
    marker='    만일 "시스템" 역할이 "dispatched-db" 행동을 수행한다\n'
    if text.count(marker)!=1:
        raise ValueError('Unique ITEM observation step required')
    return text.replace(marker,line+marker)


def restore_feature(text):
    text = response_scope.restore_feature(text)
    text = read_quantity.restore_feature(text)
    text = recall_inputs.restore_feature(text)
    return condition_oracle.restore_feature(text).replace('    만일 "observer" 역할이 "dispatched-item" 행동을 수행한다\n','')


def legacy_bytes(raw):
    value = json.loads(raw)
    restored = restore_case(value)
    return raw if restored == value else (json.dumps(restored, ensure_ascii=False, indent=2) + '\n').encode()


if __name__ == '__main__':
    import sys
    if '--assessment-policy' in sys.argv:
        case_path = HERE / 'case.json'
        before = restore_policy_case(json.loads(case_path.read_text()))
        fixtures = {p.stem: restore_policy_fixture(json.loads(p.read_text()))
                    for p in (HERE/'fixtures').glob('*.json')}
        after = apply_policy_case(before)
        authored = apply_policy_fixtures(after, fixtures)
        changes = {}
        for old, new in zip(before['subcases'], after['subcases']):
            old_goal = next((a['request']['slots']['goal'] for a in old['actions'] if a['id'] == 'complete-order-delivery-goal'), None)
            if old_goal is not None:
                new_goal = next(a['request']['slots']['goal'] for a in new['actions'] if a['id'] == 'complete-order-delivery-goal')
                changes[old['id']] = policy_goal_delta(old_goal, new_goal)
        fixture_deltas = {authored[sid]['fixtureId']: dict(alias=authored[sid]['aliases'][POLICY_ALIAS],
                           metadata=authored[sid]['baseline']['policy'][POLICY_META],
                           beforeSha256=hashlib.sha256((json.dumps(fixtures[sid], ensure_ascii=False, indent=2)+'\n').encode()).hexdigest())
                          for sid in delivery_goal_subcases(after)}
        record = dict(baseline=POLICY_BASE, fixtureSubcases=delivery_goal_subcases(after),
                      assertionsChanged=False, existingFixtureInputsChanged=False,
                      goalChanges=changes, fixtureChanges=fixture_deltas)
        POLICY_LEDGER.write_text(json.dumps(record, ensure_ascii=False, indent=2)+'\n')
        case_path.write_text(json.dumps(after, ensure_ascii=False, indent=2)+'\n')
        for sid in delivery_goal_subcases(after):
            (HERE/'fixtures'/f'{sid}.json').write_text(json.dumps(authored[sid], ensure_ascii=False, indent=2)+'\n')
        assert restore_policy_case(after) == before
        assert all(restore_policy_fixture(authored[sid]) == fixtures[sid] for sid in delivery_goal_subcases(after))
        print('T17 delivery assessment policy:', len(changes), 'exact reversible goal/fixture inputs')
        sys.exit(0)
    root = HERE.parents[2]
    before = json.loads(subprocess.check_output(
        ['git', 'show', BASE + ':verification/cases/T17/case.json'], cwd=root))
    after = condition_oracle.restore_case(restore_policy_case(apply(before)))
    changes = []
    for old, new in zip(before['subcases'], after['subcases']):
        for section in ('actions', 'assertions'):
            prior={row['id']:row for row in old[section]}
            for b in new[section]:
                a=prior.get(b['id'])
                if a != b:
                    changes.append({'subcase': old['id'], 'section': section,
                                    'id': b['id'], 'before': a, 'after': b})
    LEDGER.write_text(json.dumps({'baseline': BASE, 'assertionCount': 257,
        'expectedBusinessValuesChanged': False, 'fixtureGrantClockChanged': False,
        'changes': changes}, ensure_ascii=False, indent=2) + '\n')
    after = apply_policy_case(condition_oracle.apply(after))
    (HERE / 'case.json').write_text(json.dumps(after, ensure_ascii=False, indent=2) + '\n')
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    (HERE/'scenario.feature').write_text(condition_oracle.apply_feature(apply_feature(feature)))
    assert restore_case(after) == before
    assert apply(after) == after
    print('T17 sales contract inputs:', len(changes), 'reversible scope/source changes')
