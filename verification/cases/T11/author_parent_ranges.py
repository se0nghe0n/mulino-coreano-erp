#!/usr/bin/env python3
"""T11의 명시된 serial 구간과 현재 revision 입력만 가역적으로 정규화한다."""
import copy
import hashlib
import json
import re
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BASE = '4f01e111ba9c0a6b4c18c266b5101289f2f1f0c9'
SELECTED = ('parent90-cancelled', 'parent90-impossible', 'parent90-superseded')


def baseline():
    return json.loads(subprocess.check_output(['git', 'show', BASE+':verification/cases/T11/case.json'], cwd=ROOT))


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def ref(action, pointer):
    return {'$result': {'actionId': action, 'pointer': pointer}}


def authored(original):
    result = copy.deepcopy(original)
    changes = []
    for sub in result['subcases']:
        if sub['id'] not in SELECTED:
            continue
        actions = {a['id']: a for a in sub['actions']}
        source = actions['receive90-event']['request']['slots']['payload']['$encode']['source']
        assert source['startQuantity'] == '0' and source['quantity'] == '90' and source['unit'] == 'BOX'
        assert source['rangeRootId'] == {'$alias': 'Q'}
        assert source == actions['receive90-event-document']['request']['slots']['contentBase64']['$encode']['source']
        output = []
        for action in sub['actions']:
            before = copy.deepcopy(action)
            cap = action.get('capabilityId')
            if cap == 'createWorkLink' and action['id'] in ('child30-link', 'child60-link'):
                request = action['request']; slots = request['slots']
                label = slots.pop('physicalScope')
                match = re.fullmatch(r'serial-boxes-(\d{3})-(\d{3})', label)
                assert match, label
                start, end = map(int, match.groups())
                assert 1 <= start <= end <= int(source['quantity'])
                assert slots['quantity'] == {'value': str(end-start+1), 'unit': source['unit']}
                slots['startQuantity'] = str(start-1)
                request['provenance']['startQuantity'] = request['provenance'].pop('physicalScope')
                work = slots['sourceWorkId']
            elif cap == 'closeWork':
                request = action['request']; slots = request['slots']; work = slots['workId']
                # The published command accepts Work + reason and binds its latest
                # assessment inside the server transaction. Old descriptive inputs
                # remain in this layer's reversible ledger, never as fake evidence.
                for field in ('assessmentId', 'decisionEvidenceId', 'reasonText', 'successorWorkId'):
                    slots.pop(field, None); request['provenance'].pop(field, None)
            else:
                output.append(action)
                continue
            query_id = action['id']+'-current-work'
            query = {'id': query_id, 'kind': 'query', 'actorRef': action['actorRef'], 'route': 'api',
                     'capabilityId': 'getWork', 'request': {'workId': copy.deepcopy(work),
                     'scope': {'organizationId': {'$alias': 'ORG'}}},
                     'evidenceRefs': [query_id+':actual-artifact']}
            request['expectedRevision'] = ref(query_id, '/response/data/revision')
            output.extend([query, action])
            changes.append({'subcaseId': sub['id'], 'actionId': action['id'], 'before': before,
                            'after': copy.deepcopy(action), 'insertedBefore': query})
        sub['actions'] = output
    return result, changes


def apply(value):
    old = baseline(); new, _ = authored(old)
    if value == new:
        return copy.deepcopy(value)
    if value != old:
        raise ValueError('T11 parent input drift outside the recorded authoring baseline')
    return new


def restore(value):
    old = baseline(); new, _ = authored(old)
    if value != new:
        raise ValueError('T11 strict inverse refuses unrecorded input/oracle drift')
    return old


def authored_feature():
    text = subprocess.check_output(['git', 'show', BASE+':verification/cases/T11/scenario.feature'], cwd=ROOT).decode()
    _, changes = authored(baseline())
    for subcase in SELECTED:
        start = text.index('"'+subcase+'"를 준비한다')
        end = text.find('\n  시나리오:', start)
        if end < 0: end = len(text)
        block = text[start:end]
        for change in changes:
            if change['subcaseId'] != subcase: continue
            actor = change['before']['actorRef']; aid = change['actionId']
            old = f'    만일 "{actor}" 역할이 "{aid}" 행동을 수행한다\n'
            assert block.count(old) == 1
            new = f'    만일 "{actor}" 역할이 "{aid}-current-work" 행동을 수행한다\n'+old
            block = block.replace(old, new)
        text = text[:start]+block+text[end:]
    return text


def restore_feature(value):
    if value != authored_feature():
        raise ValueError('T11 strict inverse refuses unrecorded feature drift')
    return subprocess.check_output(['git', 'show', BASE+':verification/cases/T11/scenario.feature'], cwd=ROOT).decode()


def main():
    path = HERE/'case.json'
    old = baseline(); value = apply(json.loads(path.read_text())); _, changes = authored(old)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')
    (HERE/'scenario.feature').write_text(authored_feature())
    ledger = {'recordType': 'T11_PARENT_RANGE_INPUT_AUTHORING', 'baseline': BASE,
              'selectedSubcases': list(SELECTED), 'assertionsUnchanged': 110,
              'beforeSha256': digest(old), 'afterSha256': digest(value),
              'changes': changes,
              'rangeBasis': 'Original receive90 source: startQuantity=0, quantity=90 BOX, rangeRootId=Q; serial-boxes-001-030 -> [0,30), serial-boxes-031-090 -> [30,90). Runtime canonical matching remains mandatory.',
              'noNewSourceFacts': True, 'businessOutcomesChanged': False}
    (HERE/'parent-range-input-ledger.json').write_text(json.dumps(ledger, ensure_ascii=False, indent=2)+'\n')


if __name__ == '__main__':
    main()
