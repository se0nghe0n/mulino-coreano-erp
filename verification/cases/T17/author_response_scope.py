"""인도·조사의 열린 책임과 ITEM 전체 assignment를 함께 대조한다."""
import copy
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
LEDGER = HERE / 'response-scope-delta.json'
SID = 'late-restriction-actual-delivery'
ACTION = 'after-delivery-responses-db'


def apply(case):
    case = copy.deepcopy(case)
    ledger = json.loads(LEDGER.read_text())
    sub = next((s for s in case['subcases'] if s['id'] == SID), None)
    if sub is None:
        return case
    for change in ledger['changes']:
        rows = sub[change['section']]
        found = next((r for r in rows if r['id'] == change['id']), None)
        if found == change['after']:
            continue
        if found != change['before']:
            raise ValueError('Response scope input drift: ' + change['id'])
        if found is not None:
            rows[rows.index(found)] = copy.deepcopy(change['after'])
        else:
            rows.insert(change['index'], copy.deepcopy(change['after']))
    return case


def restore_case(case):
    case = copy.deepcopy(case)
    ledger = json.loads(LEDGER.read_text())
    sub = next((s for s in case['subcases'] if s['id'] == SID), None)
    if sub is None:
        return case
    additions = {c['id'] for c in ledger['changes'] if c['before'] is None}
    if not any(r['id'] in additions for k in ('actions', 'assertions') for r in sub[k]):
        original = next(c for c in ledger['changes'] if c['before'] is not None)
        if next(a for a in sub['assertions'] if a['id'] == original['id']) == original['after']:
            raise ValueError('Response scope additions missing')
        return case
    for change in reversed(ledger['changes']):
        rows = sub[change['section']]
        found = next((r for r in rows if r['id'] == change['id']), None)
        if found != change['after']:
            raise ValueError('Response scope inverse drift: ' + change['id'])
        index = rows.index(found)
        if change['before'] is None:
            rows.pop(index)
        else:
            rows[index] = copy.deepcopy(change['before'])
    return case


def feature_lines():
    ledger = json.loads(LEDGER.read_text())
    return ['    만일 "시스템" 역할이 "' + ACTION + '" 행동을 수행한다\n'] + [
        '    그러면 "' + c['id'] + '" assertion으로 "late-restriction-duty"를 확인한다\n'
        for c in ledger['changes'] if c['section'] == 'assertions' and c['before'] is None]


def apply_feature(text):
    added = feature_lines()
    if all(line in text for line in added):
        return text
    # Action lines can already be inserted by the full public generator.
    out = []
    current = False
    for line in text.splitlines(keepends=True):
        if '먼저 사례 파일 ' in line:
            current = '의 "' + SID + '"' in line
        if current and '역할이 "after-delivery-db" 행동을 수행한다' in line and added[0] not in text:
            out.append(added[0])
        out.append(line)
        if current and '"late-restriction-duty-26" assertion으로' in line:
            out.extend(x for x in added[1:] if x not in text)
    result = ''.join(out)
    if not all(line in result for line in added):
        raise ValueError('Response scope feature anchors missing')
    return result


def restore_feature(text):
    for line in feature_lines():
        text = text.replace(line, '')
    return text
