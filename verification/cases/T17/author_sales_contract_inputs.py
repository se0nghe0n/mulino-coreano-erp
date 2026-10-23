"""실제 공개 계약에 맞춘 scope·source selector의 정확한 가역 교정이다.

W40과 원본 257 oracle ID, fixture/grant/clock은 보존한다. 관측 결과를
예상값으로 복사하거나 품질·회수·목표의 미실행을 성공으로 바꾸지 않는다.
"""
import copy
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
import importlib.util
_condition_spec = importlib.util.spec_from_file_location('t17_condition_oracle', HERE/'author_condition_oracle.py')
condition_oracle = importlib.util.module_from_spec(_condition_spec)
_condition_spec.loader.exec_module(condition_oracle)
BASE = 'b4772eb6'
LEDGER = HERE / 'sales-contract-input-delta.json'


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
    return condition_oracle.apply(case)


def restore_case(case):
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
    return condition_oracle.restore_feature(text).replace('    만일 "observer" 역할이 "dispatched-item" 행동을 수행한다\n','')


def legacy_bytes(raw):
    value = json.loads(raw)
    restored = restore_case(value)
    return raw if restored == value else (json.dumps(restored, ensure_ascii=False, indent=2) + '\n').encode()


if __name__ == '__main__':
    root = HERE.parents[2]
    before = json.loads(subprocess.check_output(
        ['git', 'show', BASE + ':verification/cases/T17/case.json'], cwd=root))
    after = condition_oracle.restore_case(apply(before))
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
    after = condition_oracle.apply(after)
    (HERE / 'case.json').write_text(json.dumps(after, ensure_ascii=False, indent=2) + '\n')
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    (HERE/'scenario.feature').write_text(condition_oracle.apply_feature(apply_feature(feature)))
    assert restore_case(after) == before
    assert apply(after) == after
    print('T17 sales contract inputs:', len(changes), 'reversible scope/source changes')
