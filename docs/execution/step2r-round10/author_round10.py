#!/usr/bin/env python3
"""Step 2 round 10 (closure review 7): dispatch transit place, dispatch occurrence time and delegated authority.

Idempotent authoring of hand-authored fixtures and cases. Generator-owned files are changed in their generators
instead and regenerated afterwards: T06/T22/T24 (verification/cases/T06/author_contracts.py), T26
(verification/cases/T26/author_review_fixes.py, which also re-derives its autonomous and forged fixtures from the
base fixtures written here) and the C3 rendering (verification/cases/C3/author_prerequisites.py re-renders the case
file this script edits; it does not own the dispatch actions). Evidence for docs/execution/step2r-round10/README.md
and precondition-audit.md; ./verify prepare (ContractValidator.dispatchTransitProblems, occurrenceTimeProblems,
grantAuthorityProblems) is the gate.

Product rules (read only):
1. FulfillmentCommands.prepare checks dispatchQuantity after the pick: uuid(slots,'transitPlaceId') (TYPE_INVALID),
   Place.kind TRANSIT ('Transit place required'), auth.authorizeScopes with PLACE [transit] (FORBIDDEN) and
   requireContinuousAuthority. Every dispatch that names an allocation therefore names a fixture TRANSIT place in the
   corpus slot cargoPlaceId (C4/E1/E2/T17/T18 convention; the Step 3 adapter maps it to transitPlaceId), and the
   fixture lists that place in the dispatching actor's grant place scope when the grant lists places.
2. requireContinuousAuthority rejects an occurrence before the authorized pick ('Dispatch occurrence cannot precede its
   authorized pick'); a runtime pick records the product clock (FulfillmentStockPrimitives.pick, pickedAt=knownAt).
   C2/T09 cumulative-versus-state and T09 exists-versus-end-throughout keep their historical dispatch instants: the
   fixture clock starts at the business start of the scenario and clock controls advance it, so the pick precedes the
   dispatch and the cumulative/state arithmetic is unchanged. T11 cancel-after-shipment asserts nothing about the
   dispatch instant, so its dispatch is dated at the pick (the fixture clock asOf).
3. IdentityAuthorization.permittedScopes checks a delegated grant against its delegator for the same capability and
   targets. A delegator that is a fixture actor holds every action it delegates (role and grant), plan section 7.1.
Run: python3 -I docs/execution/step2r-round10/author_round10.py
"""
import glob
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
TRANSIT_SLOT = 'cargoPlaceId'
TRANSIT_ALIAS = 'TRANSIT'
TRANSIT_NAME = '출고 운송 구간'
# Generator-owned outputs (their generators apply the same rules).
GENERATED_PREFIXES = ('verification/cases/T06/', 'verification/cases/T22/', 'verification/cases/T24/', 'verification/cases/T01/',
                      'verification/cases/T20/', 'verification/cases/T25/')
GENERATED_FILES = {f'verification/cases/T26/fixtures/{n}.json' for n in
                   ('safe-retry-forged-original-actor', 'due-wait-autonomous-loop', 'lot-expiry-autonomous-loop', 'orphan-intake-autonomous-loop')}
GENERATED_CASES = {'T06', 'T22', 'T24', 'T26', 'T01', 'T20', 'T25'}


def compact(value): return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def render_compact(case):
    """The one-action-per-line case layout of T08, V7 and C3 (C3/author_prerequisites.py render_case)."""
    def sub(s):
        lines = ['    {']
        for k, v in s.items():
            if k in ('actions', 'assertions'):
                lines.append('      ' + json.dumps(k) + ': [')
                lines.extend('        ' + compact(a) + (',' if i < len(v) - 1 else '') for i, a in enumerate(v))
                lines.append('      ],')
            else:
                lines.append('      ' + json.dumps(k) + ': ' + json.dumps(v, ensure_ascii=False) + ',')
        lines[-1] = lines[-1].rstrip(',')
        lines.append('    }')
        return '\n'.join(lines)
    head = ['{'] + ['  ' + json.dumps(k) + ': ' + json.dumps(v, ensure_ascii=False) + ',' for k, v in case.items() if k != 'subcases']
    return '\n'.join(head + ['  "subcases": [', ',\n'.join(sub(s) for s in case['subcases']), '  ]', '}']) + '\n'


def style(text):
    data = json.loads(text)
    for indent in (2, None):
        for newline in ('\n', ''):
            if json.dumps(data, ensure_ascii=False, indent=indent) + newline == text:
                return indent, newline
    if isinstance(data, dict) and 'subcases' in data and render_compact(data) == text:
        return 'compact', ''
    raise ValueError('unknown JSON layout')


def load(path):
    text = path.read_text()
    return json.loads(text), style(text)


def save(path, data, fmt):
    if fmt[0] == 'compact':
        path.write_text(render_compact(data))
    else:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=fmt[0]) + fmt[1])


def alias(x): return {'$alias': x}


def collect(actions, out):
    for a in actions:
        out.append(a)
        if 'call' in a:
            out.append(a['call'])
        for b in a.get('branches', []):
            collect(b.get('actions', []), out)
    return out


def capability(a): return a.get('capabilityId') or a.get('request', {}).get('capabilityId')


def allocation_container(request):
    """The request object that carries the allocation of a dispatch (slots, else the request itself), or None."""
    if 'allocationId' in request.get('slots', {}):
        return request['slots']
    if 'allocationId' in request:
        return request
    return None


# ---- 1. transit place --------------------------------------------------------------------------------------------------
def ensure_transit(fixture):
    """A TRANSIT Place alias in the fixture and in the place scope of every actor that dispatches or delegates a dispatch."""
    aliases = fixture['aliases']
    transit = next((k for k, v in aliases.items() if v.get('type') == 'Place' and v.get('kind') == 'TRANSIT'), None)
    if transit is None:
        assert TRANSIT_ALIAS not in aliases, 'TRANSIT alias taken by a non-transit entry'
        place = {'type': 'Place', 'name': TRANSIT_NAME, 'kind': 'TRANSIT'}
        org = {v.get('organizationAlias') for v in aliases.values() if v.get('type') == 'Place' and v.get('kind') == 'INTERNAL_STORAGE'}
        if len(org) == 1 and None not in org:
            place['organizationAlias'] = org.pop()
        aliases[TRANSIT_ALIAS] = place
        transit = TRANSIT_ALIAS
    actors = fixture.get('actors', {})
    dispatchers = {k for k, v in actors.items() if 'dispatchQuantity' in v.get('grant', {}).get('actions', [])}
    changed = True
    while changed:  # delegators of dispatchers are checked for the same PLACE target (IdentityAuthorization)
        changed = False
        for k in list(dispatchers):
            d = actors[k].get('grant', {}).get('delegatorAlias')
            if d in actors and d not in dispatchers:
                dispatchers.add(d)
                changed = True
    for k in sorted(dispatchers):
        scope = actors[k].get('grant', {}).get('scope', {})
        for key in ('placeAliases', 'places'):
            if isinstance(scope.get(key), list) and transit not in scope[key]:
                scope[key].append(transit)
    return transit


def name_transit(case_path, case, dispatch_subcases):
    """cargoPlaceId on every dispatch that names an allocation (well-formed copy of the positive dispatch)."""
    for sub in case['subcases']:
        if sub['id'] not in dispatch_subcases:
            continue
        transit = dispatch_subcases[sub['id']]
        for a in collect(sub['actions'], []):
            if capability(a) != 'dispatchQuantity':
                continue
            box = allocation_container(a.get('request', {}))
            if box is not None and TRANSIT_SLOT not in box and 'transitPlaceId' not in box:
                box[TRANSIT_SLOT] = alias(transit)


def transit():
    by_case = {}
    for path in sorted(glob.glob(str(ROOT / 'verification/cases/*/case.json'))):
        case_path = Path(path)
        cid = case_path.parent.name
        if cid in GENERATED_CASES:
            continue
        case, fmt = load(case_path)
        subs = {}
        for sub in case['subcases']:
            if not any(capability(a) == 'dispatchQuantity' and allocation_container(a.get('request', {})) is not None for a in collect(sub['actions'], [])):
                continue
            fpath = ROOT / sub['fixtureRef']
            fixture, ffmt = load(fpath)
            if fixture.get('baseRefs'):
                # T13: the TRANSIT place and its scope are already in the fixture (round 7).
                found = next(k for k, v in fixture['aliases'].items() if v.get('type') == 'Place' and v.get('kind') == 'TRANSIT')
            else:
                found = ensure_transit(fixture)
                save(fpath, fixture, ffmt)
            subs[sub['id']] = found
        if subs:
            name_transit(case_path, case, subs)
            save(case_path, case, fmt)
            by_case[cid] = sorted(subs)
    return by_case


# ---- 2. dispatch occurrence after the pick -----------------------------------------------------------------------------
def by_id(items, i): return next(x for x in items if x['id'] == i)


def clock_action(aid, instant, scope):
    return {'id': aid, 'kind': 'control', 'evidenceRefs': [aid + ':actual-artifact'],
            'control': {'type': 'clock', 'operation': 'advanceTo',
                        'parameters': {'instant': instant, 'knownAt': instant, 'timezone': 'Asia/Seoul', 'scope': scope}}}


def insert_before(actions, anchor, action):
    actions[:] = [x for x in actions if x['id'] != action['id']]
    actions.insert([x['id'] for x in actions].index(anchor), action)


def feature_actions(cid, sid, case):
    """Rewrite the 만일 lines of one scenario from the action order (actor or 시스템)."""
    path = ROOT / f'verification/cases/{cid}/scenario.feature'
    lines = path.read_text().split('\n')
    start = next(i for i, l in enumerate(lines) if f'의 "{sid}"를 준비한다' in l)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip().startswith(('시나리오', '@'))), len(lines))
    sub = by_id(case['subcases'], sid)
    block = lines[start:end]
    first = next(i for i, l in enumerate(block) if l.strip().startswith('만일'))
    last = max(i for i, l in enumerate(block) if l.strip().startswith('만일'))
    indent = block[first][:len(block[first]) - len(block[first].lstrip())]
    acts = [f'{indent}만일 "{a.get("actorRef", "시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in sub['actions']]
    block[first:last + 1] = acts
    lines[start:end] = block
    path.write_text('\n'.join(lines))


RETIME = {
    # case, subcase: fixture clock (business start), [(anchor action, clock id, instant)]
    ('C2', 'cumulative-versus-state'): ('2026-10-05T09:00:00Z', [('dispatch60', 'clock-dispatch60', '2026-10-06T09:00:00Z'),
                                                                 ('receive40', 'clock-receive40', '2026-10-07T01:30:00Z'),
                                                                 ('cumulative-assessment', 'clock-known', '2026-10-07T04:00:00Z')]),
    ('T09', 'cumulative-versus-state'): ('2026-10-05T09:00:00Z', [('dispatch60', 'clock-dispatch60', '2026-10-06T09:00:00Z'),
                                                                  ('receive40', 'clock-receive40', '2026-10-07T01:30:00Z'),
                                                                  ('cumulative-assessment', 'clock-known', '2026-10-07T04:00:00Z')]),
    ('T09', 'exists-versus-end-throughout'): ('2026-10-07T00:00:00Z', [('dispatch', 'clock-dispatch', '2026-10-07T01:00:00Z'),
                                                                       ('exists-assessment', 'clock-known', '2026-10-07T04:00:00Z')]),
}


def retime():
    for (cid, sid), (start, advances) in RETIME.items():
        cpath = ROOT / f'verification/cases/{cid}/case.json'
        case, fmt = load(cpath)
        sub = by_id(case['subcases'], sid)
        fpath = ROOT / sub['fixtureRef']
        fixture, ffmt = load(fpath)
        fixture['clock']['asOf'] = start
        fixture['clock']['knownAt'] = start
        save(fpath, fixture, ffmt)
        scope = {'organizationId': alias('ORG'), 'caseId': cid, 'subcaseId': sid}
        for anchor, aid, instant in advances:
            insert_before(sub['actions'], anchor, clock_action(aid, instant, scope))
        save(cpath, case, fmt)
        feature_actions(cid, sid, case)
    # T11 cancel-after-shipment: nothing reads the dispatch instant; date it at the pick (the fixture clock asOf).
    cpath = ROOT / 'verification/cases/T11/case.json'
    case, fmt = load(cpath)
    sub = by_id(case['subcases'], 'cancel-after-shipment')
    clock = json.loads((ROOT / sub['fixtureRef']).read_text())['clock']['asOf']
    by_id(sub['actions'], 'dispatch')['request']['slots']['occurredAt'] = clock
    save(cpath, case, fmt)


# ---- 3. delegators hold what they delegate -----------------------------------------------------------------------------
def cover_delegations(fixture):
    actors = fixture.get('actors')
    if not isinstance(actors, dict):
        return False
    changed, any_change = True, False
    while changed:
        changed = False
        for name, actor in actors.items():
            grant = actor.get('grant', {})
            d = grant.get('delegatorAlias')
            if not d or d == name or d not in actors:
                continue
            owner = actors[d]
            og = owner.setdefault('grant', {})
            for cap in grant.get('actions', []):
                for key, target in (('roleCapabilities', owner.setdefault('roleCapabilities', [])), ('actions', og.setdefault('actions', []))):
                    if cap not in target:
                        target.append(cap)
                        changed = any_change = True
                ids = og.get('scope', {}).get('capabilityIds')
                if isinstance(ids, list) and cap not in ids:
                    ids.append(cap)
                    changed = any_change = True
    return any_change


def delegations():
    touched = []
    for path in sorted(glob.glob(str(ROOT / 'verification/**/*.json'), recursive=True)):
        rel = str(Path(path).relative_to(ROOT))
        if rel.startswith(GENERATED_PREFIXES) or rel in GENERATED_FILES or '/evidence/' in rel or rel.startswith(('verification/actual/', 'verification/harness/')):
            continue
        try:
            data, fmt = load(Path(path))
        except (ValueError, UnicodeDecodeError):
            continue
        if not isinstance(data, dict) or not isinstance(data.get('actors'), dict) or 'aliases' not in data:
            continue
        if cover_delegations(data):
            save(Path(path), data, fmt)
            touched.append(rel)
    return touched


if __name__ == '__main__':
    named = transit()
    retime()
    covered = delegations()
    print('dispatch transit named in', {k: len(v) for k, v in named.items()})
    print('delegators covered in', len(covered), 'hand-authored fixtures')
