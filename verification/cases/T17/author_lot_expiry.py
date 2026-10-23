#!/usr/bin/env python3
"""coordinator가 선택한 T17 합성 LOT 만료 convention을 재현한다.

기존 업무/관측 oracle을 읽지 않는다. 원래 segment의 명시 만료일 또는
coordinator가 합성 값으로 정한 given 유효창만 쓴다.
모든 변경의 역변환을 기록한다.
"""
import copy
import json
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
META = 'lotExpiryAuthoring'
NORMAL = {'reserve-pick-dispatch', 'normal-consumed-delivery',
          'late-restriction-actual-delivery', 'claim-falseclaim',
          'claim-unidentifiedscope', 'claim-conflictingquantity',
          'claim-nopriordispatch', 'claim-scopeoverflow', 'replacement-no-revival'}


def at(root, path):
    keys = path.strip('/').split('/')
    for key in keys[:-1]: root = root[int(key)] if isinstance(root, list) else root[key]
    return root, int(keys[-1]) if isinstance(root, list) else keys[-1]


def timestamp(value):
    if not isinstance(value, str): raise ValueError('Explicit UTC expiry required')
    parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if parsed.tzinfo is None: raise ValueError('Expiry timezone required')
    return parsed


def validate(f):
    for name, segment in f['aliases'].items():
        if segment.get('type') != 'QuantitySegment': continue
        lot = f['aliases'][segment['lotAlias']]
        if 'expiresAt' in segment:
            declared = segment['expiresAt']
            if declared in (None, 'UNKNOWN'):
                if lot.get('expiresAt') is not None: raise ValueError(f'{name}: unknown expiry became known')
            elif lot.get('expiresAt') != declared:
                raise ValueError(f'{name}: segment expiry differs from LOT expiry')
        if lot.get('expiresAt') is not None: timestamp(lot['expiresAt'])
    for row in f['baseline'].get('sourceProvenance', []):
        if row['lotAlias'] != f['aliases'][row['segmentAlias']]['lotAlias']:
            raise ValueError('Supplier source LOT mismatch')
    for row in f['baseline'].get('works', []):
        segment = next((s for s,w in f['baseline']['t17SourceFacts']['workAliases'].items() if w == row['alias']), None)
        if segment and row['lotAlias'] != f['aliases'][segment]['lotAlias']:
            raise ValueError('Source Work LOT mismatch')
    meta = f['baseline']['policy'].get(META)
    if meta:
        for change in meta['changes']:
            parent, key = at(f, change['path'])
            if parent.get(key) != change['after']: raise ValueError('Authored LOT delta changed: ' + change['path'])


def revert(f):
    f = copy.deepcopy(f)
    meta = f['baseline']['policy'].pop(META, None)
    if meta:
        for change in reversed(meta['changes']):
            parent, key = at(f, change['path'])
            if change['hadBefore']: parent[key] = copy.deepcopy(change['before'])
            else: del parent[key]
    return f


def apply(f):
    if META in f['baseline']['policy']:
        validate(f)
        return False
    if not f['fixtureId'].startswith('T17-') or not f['synthetic']:
        raise ValueError('Synthetic T17 fixture required')
    original = copy.deepcopy(f)
    changes = []
    def put(path, value):
        parent, key = at(f, path)
        if key in parent and parent[key] == value: return
        change = {'path': path, 'hadBefore': key in parent,
                  'before': copy.deepcopy(parent.get(key)), 'after': copy.deepcopy(value)}
        changes.append(change); parent[key] = copy.deepcopy(value)
    aliases = f['aliases']
    segments = {k:v for k,v in original['aliases'].items() if v.get('type') == 'QuantitySegment'}
    original_lot = original['aliases']['L']
    if 'expiresAt' in original_lot: raise ValueError('Existing LOT expiry needs explicit review')
    sid = f['fixtureId'][4:]
    explicit = {k:v['expiresAt'] for k,v in segments.items() if 'expiresAt' in v}
    if explicit:
        if 'A' not in explicit or explicit['A'] in (None, 'UNKNOWN'):
            raise ValueError('Original A must anchor the declared known expiry group')
        expiry = explicit['A']; timestamp(expiry)
        selection = 'ORIGINAL_SEGMENT_EXPIRY_GROUPS'
    elif sid == 'post-dispatch-expiry':
        expiry = '2026-10-07T09:05:00Z'
        selection = 'NEW_SYNTHETIC_0905_BOUNDARY_BETWEEN_0900_AND_0906'
    elif sid in NORMAL:
        conditions = f['baseline']['conditions']
        if not conditions or any(c['status'] != 'ALLOWED' for c in conditions):
            raise ValueError('Normal convention requires the original all-ALLOWED given')
        expiry = max((c['validUntil'] for c in conditions), key=timestamp)
        selection = 'NEW_SYNTHETIC_MAX_ALLOWED_CONDITION_VALID_UNTIL'
    else:
        raise ValueError('No authorized expiry convention for ' + sid)
    # The old L had no declared manufacturer/item. Give L and its split groups
    # one explicit synthetic manufacturer and the item already identified by A.
    manufacturer = original_lot.get('manufacturerAlias', 't17LotManufacturer')
    if manufacturer not in aliases:
        put('/aliases/'+manufacturer, {'type': 'Manufacturer', 'name': 'Synthetic T17 original-L manufacturer'})
    item = original_lot.get('itemAlias', segments['A']['itemAlias'])
    put('/aliases/L/manufacturerAlias', manufacturer)
    put('/aliases/L/itemAlias', item)
    put('/aliases/L/expiresAt', expiry)
    groups = {expiry: 'L'}
    mapping = []
    for name, segment in segments.items():
        old = segment['lotAlias']
        if old != 'L': raise ValueError('Unexpected pre-existing LOT scope')
        declared = explicit.get(name, expiry)
        if name in explicit and declared not in (None, 'UNKNOWN'): timestamp(declared)
        if declared not in groups:
            alias = 'L_' + name
            if alias in aliases: raise ValueError('New LOT alias collision')
            lot = copy.deepcopy(original_lot)
            lot.update(manufacturerAlias=manufacturer, itemAlias=item)
            if declared not in (None, 'UNKNOWN'): lot['expiresAt'] = declared
            groups[declared] = alias
            put('/aliases/'+alias, lot)
        target = groups[declared]
        put('/aliases/'+name+'/lotAlias', target)
        mapping.append({'segmentAlias': name, 'oldLotAlias': old, 'newLotAlias': target,
                        'oldDeclaredSegmentExpiry': explicit.get(name), 'newLotExpiry': aliases[target].get('expiresAt')})
    for index, row in enumerate(f['baseline'].get('sourceProvenance', [])):
        put(f'/baseline/sourceProvenance/{index}/lotAlias', aliases[row['segmentAlias']]['lotAlias'])
    for index, row in enumerate(f['baseline'].get('works', [])):
        segment = next((s for s,w in f['baseline']['t17SourceFacts']['workAliases'].items() if w == row['alias']), None)
        if segment: put(f'/baseline/works/{index}/lotAlias', aliases[segment]['lotAlias'])
    # Existing source documents without an exact segment remain unchanged; their
    # multi-segment source interpretation belongs to the later delivery unit.
    f['baseline']['policy'][META] = {'version': 1, 'synthetic': True, 'selection': selection,
        'operatingExpiryClaimed': False, 'originalLotAlias': 'L', 'segmentMapping': mapping,
        'changes': changes}
    validate(f)
    if revert(f) != original: raise AssertionError('LOT authoring is not exactly reversible')
    return True


if __name__ == '__main__':
    changed = 0
    for path in sorted((HERE/'fixtures').glob('*.json')):
        f = json.loads(path.read_text())
        if apply(f): path.write_text(json.dumps(f, ensure_ascii=False, indent=2)+'\n'); changed += 1
    print(f'T17 LOT expiry authoring: {changed} fixtures changed')
