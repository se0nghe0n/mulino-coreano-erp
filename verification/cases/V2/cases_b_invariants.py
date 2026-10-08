#!/usr/bin/env python3
"""Structural self-check for the hand-maintained E1, E2, C4, V2 and V3 acceptance cases.

These case files have no generator. This check pins the review-closed invariants so a later
edit cannot silently reintroduce a vacuous or contradictory oracle. It reads files only and
never claims product behaviour. Exit 0 = all invariants hold, 1 = violations listed."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
CASES = ['E1', 'E2', 'C4', 'V2', 'V3']
CANONICAL_ERROR = '/response/error/code'
TEST_ARMING = ('testTransactionId', 'testParticipantId', 'testBarrierId', 'testBarrierPoint')
RACE_SUBCASES = {('V2', 'split-commits-first'), ('V2', 'reserve-commits-first'),
                 ('V3', 'hold-first'), ('V3', 'dispatch-first')}
problems = []


def load(path):
    return json.loads((ROOT / path).read_text())


def walk(node):
    if isinstance(node, dict):
        yield node
        for value in node.values():
            yield from walk(value)
    elif isinstance(node, list):
        for value in node:
            yield from walk(value)


def flat_actions(sub):
    out = []
    for action in sub['actions']:
        out.append(action)
        if action.get('kind') == 'start':
            out.append(action['call'])
    return out


def result_ref(value):
    return value.get('$result', {}).get('actionId') if isinstance(value, dict) else None


def fail(where, message):
    problems.append(f'{where}: {message}')


for cid in CASES:
    case = load(f'verification/cases/{cid}/case.json')
    for sub in case['subcases']:
        where = f'{cid}/{sub["id"]}'
        actions = {a['id']: a for a in flat_actions(sub)}
        await_of = {a['awaitActionId']: a['id'] for a in sub['actions'] if a.get('kind') == 'await'}
        # 1. one canonical structured error pointer (contracts/command-response.schema.json)
        for x in sub['assertions']:
            for key in ('source', 'baseline'):
                pointer = x.get(key, {}).get('pointer', '')
                if pointer.startswith('/response') and (pointer.endswith('/code') or pointer.endswith('errorCode')) \
                        and pointer != CANONICAL_ERROR:
                    fail(where, f'{x["id"]} reads error code at {pointer}, not {CANONICAL_ERROR}')
        # 2. a pinned REJECTED/CONFLICT outcome on a probe needs a pinned structured reason
        outcome = {x['source']['actionId'] for x in sub['assertions'] if x['op'] == 'equals'
                   and x['source']['pointer'] == '/response/outcome' and x['expected'] in ('REJECTED', 'CONFLICT')}
        reason = {x['source']['actionId'] for x in sub['assertions'] if x['op'] == 'equals'
                  and x['source']['pointer'] == CANONICAL_ERROR}
        for aid in sorted(outcome - reason):
            if (cid, sub['id'], aid) not in {('V2', 'split-commits-first', 'retired-parent'),
                                             ('V2', 'reserve-commits-first', 'retired-parent')}:
                fail(where, f'{aid} pins a rejection outcome without {CANONICAL_ERROR}')
        for action in flat_actions(sub):
            cap = action.get('capabilityId')
            request = action.get('request') or {}
            slots = request.get('slots') or {}
            # 3. test-only barrier arming stays outside business slots
            if 'testBarrier' in slots:
                fail(where, f'{action["id"]} arms a barrier inside business slots')
            # 4. dispatch probes in E1/E2/C4 are well-formed siblings of the accepted dispatch
            if cap == 'dispatchQuantity' and cid in ('E1', 'E2', 'C4'):
                for slot in ('allocationId', 'cargoPlaceId', 'originId', 'destinationId', 'quantity'):
                    if slot not in slots:
                        fail(where, f'{action["id"]} dispatch lacks {slot}')
                for plural in ('allocationIds', 'segmentIds'):
                    if plural in slots:
                        fail(where, f'{action["id"]} dispatch uses plural {plural}')
                alloc = json.dumps(slots.get('allocationId'), sort_keys=True)
                picked = [a for a in flat_actions(sub) if a.get('capabilityId') == 'pickQuantity'
                          and json.dumps(a['request']['slots'].get('allocationId'), sort_keys=True) == alloc]
                if not picked:
                    fail(where, f'{action["id"]} dispatches an allocation that was never picked')
            # 5. expectedRevision comes from the same aggregate, not a neighbouring command
            ref = result_ref(request.get('expectedRevision'))
            if ref and cap in ('dispatchQuantity', 'releaseHold', 'placeHold'):
                source = actions.get(ref) or actions.get(await_of.get(ref, ''), {})
                source_cap = source.get('capabilityId') or (source.get('call') or {}).get('capabilityId')
                if source.get('kind') == 'await':
                    source_cap = actions[source['awaitActionId']].get('call', {}).get('capabilityId')
                allowed = {'dispatchQuantity': {'pickQuantity', 'getObject'},
                           'releaseHold': {'placeHold', 'getObject'}, 'placeHold': {'getObject'}}[cap]
                if source_cap not in allowed:
                    fail(where, f'{action["id"]} {cap} takes expectedRevision from {ref} ({source_cap})')
        # 6. obligations.current is row validity independent of status (E1 and C4 agree)
        for x in sub['assertions']:
            w = x['source'].get('where', {})
            if x['op'] == 'count' and w.get('current') is True and w.get('status') in ('RESOLVED', 'WAIVED') \
                    and x['expected'] == 0 and x['source']['pointer'].endswith('/obligations'):
                fail(where, f'{x["id"]} treats a resolved obligation as non-current')
        # 7. race subcases carry independent lock-wait and re-validation evidence
        if (cid, sub['id']) in RACE_SUBCASES:
            starts = [a for a in sub['actions'] if a.get('kind') == 'start']
            if len(starts) != 2 or any(any(k not in a['call']['request'] for k in TEST_ARMING) for a in starts):
                fail(where, 'race needs two armed starts (contender and winner)')
            probe = actions.get('contender-waits', {}).get('observation', {})
            if not probe.get('scope', {}).get('lockProbe', {}).get('readOnly'):
                fail(where, 'race lacks the read-only pg_catalog lock probe')
            names = {x['id'] for x in sub['assertions']}
            for need in ('race-contender-waits-on-winner-lock', 'race-both-transactions-open-at-wait',
                         'race-contender-revalidated-after-lock', 'race-distinct-db-transactions'):
                if need not in names:
                    fail(where, f'race lacks {need}')
            order = [a['id'] for a in sub['actions']]
            if not all(k in order for k in ('resume', 'contender-waits', 'winner-resume')) or \
                    not order.index('resume') < order.index('contender-waits') < order.index('winner-resume'):
                fail(where, 'lock wait must be observed after the contender resumes and before the winner is released')
    # 8. E1/E2 oracles require MCP: the case declares the profile and reads through MCP
    if cid in ('E1', 'E2'):
        if 'mcp' not in case['profiles']:
            fail(cid, 'mcp profile missing although every oracle requires the MCP layer')
        for sub in case['subcases']:
            mcp = {a['id'] for a in sub['actions'] if a.get('route') == 'mcp'}
            if not mcp or not any(x['source']['actionId'] in mcp for x in sub['assertions']):
                fail(f'{cid}/{sub["id"]}', 'no assertion reads an MCP-route result')

# 9. V2 race fixtures never require over-reserving an order line
for sid in ('split-commits-first', 'reserve-commits-first'):
    fixture = load(f'verification/cases/V2/fixtures/{sid}.json')
    case = load('verification/cases/V2/case.json')
    sub = next(s for s in case['subcases'] if s['id'] == sid)
    lines = {k: v for k, v in fixture['aliases'].items() if v.get('type') == 'SalesOrderLine'}
    held = {}
    for alloc in fixture['baseline'].get('allocations', []):
        if alloc['state'] in ('EXECUTABLE', 'SUSPENDED'):
            line = next(k for k, v in lines.items() if v['workAlias'] == alloc['workAlias'])
            held[line] = held.get(line, 0) + float(alloc['quantity'])
    for action in flat_actions(sub):
        if action.get('capabilityId') == 'reserveQuantity':
            slots = action['request']['slots']
            line = slots['orderLineId'].get('$alias')
            want = float(slots['quantity']['value'])
            if action['id'] != 'retired-parent' and held.get(line, 0) + want > float(lines[line]['quantity']):
                problems.append(f'V2/{sid}: {action["id"]} over-reserves {line}')

# 10. canonical error pointer is declared in the command response contract
schema = load('contracts/command-response.schema.json')
if schema['properties']['error'].get('required') != ['code'] or '/error/code' not in schema.get('$comment', ''):
    problems.append('contracts/command-response.schema.json: error.code is not the declared canonical pointer')

# 11. V2 reserve-commits-first accepts APPLIED or CONFLICT only through the invariant bundle
#     (race-observation-contract.md "두 허용 결과"); neither branch may be pinned alone.
case = load('verification/cases/V2/case.json')
sub = next(s for s in case['subcases'] if s['id'] == 'reserve-commits-first')
pinned = [x['id'] for x in sub['assertions'] if x['source']['actionId'] == 'terminal'
          and x['source']['pointer'] in ('/response/outcome', CANONICAL_ERROR) and x['op'] == 'equals']
if pinned:
    problems.append(f'V2/reserve-commits-first: contender result pinned to one branch by {pinned}')
need = {'contender-reported-equals-recorded', 'contender-conflict-is-stale-revision',
        'contender-conflict-iff-converge-split-applied', 'raced-allocations-exactly-once',
        'final-allocations-exactly-once', 'final-children-40-20', 'final-no-allocation-on-retired-parent',
        # Per-child placement: each allocation sits on the child whose quantity it fills (no 40 on the 20 child).
        'final-alloc40-on-child40', 'final-winner20-on-child20',
        # The retired-parent probe targets an order line with outstanding quantity and pins the reason.
        'retired-parent-reconsumption-code'}
# Every command outcome of the published vocabulary except the two allowed branches is excluded.
VOCABULARY_OUTCOMES = [o['outcome'] for o in load('contracts/domain-vocabulary.json')['outcomes']]
need |= {'contender-outcome-not-' + o.lower().replace('_', '-') for o in VOCABULARY_OUTCOMES if o not in ('APPLIED', 'CONFLICT')}
probe = next(a for a in sub['actions'] if a['id'] == 'retired-parent')
if probe['request']['slots'].get('orderLineId') != {'$alias': 'ORDER3'}:
    problems.append('V2/reserve-commits-first: retired-parent probe must target ORDER3 (outstanding 10), not a fully covered line')
missing = need - {x['id'] for x in sub['assertions']}
if missing:
    problems.append(f'V2/reserve-commits-first: invariant oracle missing {sorted(missing)}')

for p in problems:
    print('FAIL', p)
print(f'cases-b invariants: {len(problems)} problem(s) across {", ".join(CASES)}')
sys.exit(1 if problems else 0)
