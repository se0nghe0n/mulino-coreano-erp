"""늦은 제한의 명시적 RECALL Work와 실제 조사 명령을 가역적으로 작성한다.

조건 layer 뒤에 합성한다. 기존 책임·grant·257 oracle는 변경하지 않는다.
"""
import importlib.util
import copy
import hashlib
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
_read_spec = importlib.util.spec_from_file_location('t17_read_quantity', HERE/'author_read_quantity_contract.py')
read_quantity = importlib.util.module_from_spec(_read_spec)
_read_spec.loader.exec_module(read_quantity)
ROOT = HERE.parents[2]
BASE = 'd0885a8031eb754e42f35f998a6d7f970b059cee'
SID = 'late-restriction-actual-delivery'
WORK = 't17FormalRecallWork'
LEDGER = HERE / 'recall-input-delta.json'


def alias(name):
    return {'$alias': name}


def ref(action, pointer):
    return {'$result': {'actionId': action, 'pointer': '/response/' + pointer}}


def additions(fixture):
    responsibilities = [r for r in fixture['responsibilities']
                        if r['scope'] == {'organizationAlias': 'ORG', 'itemAlias': 'P', 'workKind': 'RECALL'}]
    if len(responsibilities) != 1:
        raise ValueError('One original ITEM P RECALL responsibility required')
    duty = responsibilities[0]
    slots = {'workId': alias(WORK), 'itemId': alias('P'), 'lotId': alias('L'),
             'segmentId': ref('late-hold-source-scope', 'data/ID'),
             'startQuantity': '0', 'quantity': '30', 'unit': 'BOX',
             'basisEvidenceId': ref('late-hold-source-event', 'id'),
             'occurredAt': fixture['clock']['asOf'],
             'nextAction': duty['nextAction'], 'nextCheckAt': duty['nextCheckAt']}
    command = {'id': 'open-recall-investigation', 'kind': 'invoke', 'actorRef': 'admin',
               'route': 'api', 'capabilityId': 'openInvestigation',
               'request': {'intentKind': 'COMMAND', 'definitionVersion': fixture['versions']['definition'],
                           'capabilityId': 'openInvestigation',
                           'subjectRefs': [{'type': 'TradeItem', 'id': alias('P')},
                                           {'type': 'Work', 'id': alias(WORK)}],
                           'slots': slots, 'provenance': {k: 'CONTEXT' for k in slots},
                           'expectedRevision': ref('late-hold', 'revision'),
                           'commandIdempotencyKey': 'T17-' + SID + '-open-recall-investigation'},
               'evidenceRefs': ['open-recall-investigation:actual-command-artifact']}
    def query(name, cap, identity, scope):
        return {'id': name, 'kind': 'query', 'actorRef': 'admin', 'route': 'api',
                'capabilityId': cap, 'request': {'id': identity, 'scope': scope,
                'asOf': fixture['clock']['asOf'], 'knownAt': fixture['clock']['knownAt']},
                'evidenceRefs': [name + ':actual-query-artifact']}
    scope = {'organizationId': alias('ORG'), 'itemId': alias('P')}
    actions = [command,
               query('formal-recall-work', 'getWork', alias(WORK), scope),
               query('formal-recall-basis', 'getEvidence', ref('late-hold-source-event', 'id'),
                     {'organizationId': alias('ORG'), 'kind': 'EVENT'}),
               query('formal-recall-investigation', 'getObject', ref('open-recall-investigation', 'effects/investigationId'),
                     dict(scope, objectType='Recall'))]
    work = {'alias': WORK, 'itemAlias': 'P', 'lotAlias': 'L',
            'ownerAlias': duty['ownerAlias'], 'supervisorAlias': duty['supervisorAlias'],
            'kind': 'RECALL', 'status': 'ACTIVE', 'revision': 1}
    return actions, work


def apply(case, fixtures):
    case, fixtures = copy.deepcopy(case), copy.deepcopy(fixtures)
    f = fixtures[SID]
    actions, work = additions(f)
    if WORK in f['aliases'] and f['aliases'][WORK] != {'type': 'Work'}:
        raise ValueError('Formal recall alias drift')
    f['aliases'][WORK] = {'type': 'Work'}
    existing = [w for w in f['baseline']['works'] if w['alias'] == WORK]
    if existing and existing != [work]:
        raise ValueError('Formal recall Work drift')
    if not existing:
        f['baseline']['works'].append(work)
    sub = next(s for s in case['subcases'] if s['id'] == SID)
    present = [a for a in sub['actions'] if a['id'] in {a['id'] for a in actions}]
    if present and present != actions:
        raise ValueError('Formal recall action drift')
    if not present:
        at = next(i for i, a in enumerate(sub['actions']) if a['id'] == 'late-hold') + 1
        sub['actions'][at:at] = actions
    return case, fixtures


def restore_case(case):
    case = read_quantity.restore_case(case)
    case = copy.deepcopy(case)
    if not LEDGER.exists():
        return case
    changes = json.loads(LEDGER.read_text())['actionChanges']
    sub = next((s for s in case['subcases'] if s['id'] == SID), None)
    if sub is None:
        return case
    present = [a for a in sub['actions'] if a['id'] in {d['id'] for d in changes}]
    if present:
        if present != [d['after'] for d in changes]:
            raise ValueError('Formal recall action inverse drift')
        sub['actions'] = [a for a in sub['actions'] if a not in present]
    return case


def restore_fixture(f):
    f = copy.deepcopy(f)
    if f['fixtureId'] != 'T17-' + SID or WORK not in f['aliases']:
        return f
    _, work = additions(f)
    if f['aliases'][WORK] != {'type': 'Work'} or [w for w in f['baseline']['works'] if w['alias'] == WORK] != [work]:
        raise ValueError('Formal recall fixture inverse drift')
    del f['aliases'][WORK]
    f['baseline']['works'].remove(work)
    return f


def apply_feature(text):
    actions, _ = additions(json.loads((HERE / 'fixtures' / (SID + '.json')).read_text()))
    lines = ''.join(f'    만일 "admin" 역할이 "{a["id"]}" 행동을 수행한다\n' for a in actions)
    if lines in text:
        return text
    marker = '    만일 "admin" 역할이 "late-hold" 행동을 수행한다\n'
    if text.count(marker) != 1:
        raise ValueError('Unique late hold feature step required')
    return text.replace(marker, marker + lines)


def restore_feature(text):
    text = read_quantity.restore_feature(text)
    actions = json.loads(LEDGER.read_text())['actionChanges']
    lines = ''.join(f'    만일 "admin" 역할이 "{a["id"]}" 행동을 수행한다\n' for a in actions)
    return text.replace(lines, '')


def bytes_of(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def original(path):
    return subprocess.check_output(['git', 'show', BASE + ':' + str(path.relative_to(ROOT))], cwd=ROOT)


if __name__ == '__main__':
    before = json.loads(original(HERE / 'case.json'))
    fixtures = {p.stem: json.loads(original(p)) for p in sorted((HERE / 'fixtures').glob('*.json'))}
    baseline_after, changed = apply(before, fixtures)
    # Ledger는 승인된 d088 delta를 고정한다. 출력에는 현재 입력을 사용해
    # root가 먼저 합성한 다른 subcase/condition layer를 보존한다.
    current = json.loads((HERE / 'case.json').read_text())
    current_fixtures = {p.stem: json.loads(p.read_text()) for p in sorted((HERE / 'fixtures').glob('*.json'))}
    after, authored = apply(current, current_fixtures)
    if read_quantity.marked(current):
        after = read_quantity.apply(after)
    feature = (HERE / 'scenario.feature').read_text()
    actions, work = additions(fixtures[SID])
    ledger = {'baseline': BASE, 'subcase': SID, 'originalAssertionCount': 257,
              'businessAssertionsChanged': False, 'grantClockResponsibilitiesChanged': False,
              'composition': 'condition layer -> recall.apply(case, fixtures); recall inverse first',
              'actionChanges': [{'id': a['id'], 'before': None, 'after': a} for a in actions],
              'fixtureChanges': [{'pointer': '/aliases/' + WORK, 'before': None, 'after': {'type': 'Work'}},
                                 {'pointer': '/baseline/works/1', 'before': None, 'after': work}],
              'fixtureHashes': {sid: {'before': hashlib.sha256(original(HERE / 'fixtures' / (sid + '.json'))).hexdigest(),
                                     'after': hashlib.sha256(bytes_of(f)).hexdigest()}
                                for sid, f in changed.items()}}
    LEDGER.write_bytes(bytes_of(ledger))
    (HERE / 'case.json').write_bytes(bytes_of(after))
    (HERE / 'fixtures' / (SID + '.json')).write_bytes(bytes_of(authored[SID]))
    (HERE / 'scenario.feature').write_text(apply_feature(feature))
    assert restore_case(baseline_after) == before
    assert restore_fixture(changed[SID]) == fixtures[SID]
    assert restore_case(after) == restore_case(current)
    print('T17 formal recall: 4 actions, 1 alias + 1 ACTIVE Work; all original assertions preserved')
