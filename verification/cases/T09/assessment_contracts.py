"""Explicit evaluation and reviewable structural bindings, never scenario execution.

The correction ledger pins old/new selectors separately from unchanged business
expectations. No HTTP/JDBC aliases or fallback projections are introduced.
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ACTOR = 'assessmentEvaluator'


def ref(action, pointer):
    return {'$result': {'actionId': action, 'pointer': pointer}}


def apply(case, fixture):
    for sub in case['subcases']:
        f = fixture(sub['fixtureRef'])
        # Authored fixture policy, same pinned version and same synthetic content.
        policies = f.get('baseline', {}).get('policies', [])
        if policies:
            policy = policies[0]
            f['aliases']['syntheticPolicyVersion'] = {
                'type': 'PolicyVersion', 'kind': 'EVIDENCE',
                'version': policy['version'], 'content': copy.deepcopy(policy),
                'effectiveFrom': '2026-10-01T00:00:00Z',
                'effectiveUntil': '2026-10-31T23:59:59Z', 'activate': True}
        queries = [a for a in sub['actions'] if a.get('capabilityId') == 'getAssessment']
        if not queries:
            continue
        template = f['actors']['A']
        f['aliases'][ACTOR] = {'type': 'Human'}
        f['actors'][ACTOR] = {
            'issuer': template['issuer'], 'subject': f['fixtureId']+'-assessment-evaluator',
            'audience': template['audience'], 'organizationAlias': template['organizationAlias'],
            'roleCapabilities': ['assessGoal'], 'grant': {
                'delegatorAlias': ACTOR, 'actions': ['assessGoal'],
                'scope': {'organizationAlias': 'ORG', 'itemAliases': ['P']},
                'validFrom': '2026-10-01T00:00:00Z', 'validUntil': '2026-10-31T23:59:59Z', 'revision': 1}}
        existing = {a['id'] for a in sub['actions']}
        output = []
        for action in sub['actions']:
            if action.get('capabilityId') == 'getAssessment':
                q = action['request']
                work = q.get('workId')
                if work is None:  # already transformed
                    output.append(action)
                    continue
                eid = action['id']+'-evaluate'
                # Source/canonical writes may advance Work revision. Read the
                # current revision explicitly, then pass it to the public command.
                read_id = action['id']+'-evaluation-work'
                revision = ref(read_id, '/response/data/revision')
                if eid not in existing:
                    output.append({'id': read_id, 'kind': 'query', 'actorRef': action['actorRef'], 'route': 'api',
                        'capabilityId': 'getWork', 'evidenceRefs': [read_id+':actual-artifact'],
                        'request': {'workId': copy.deepcopy(work), 'scope': copy.deepcopy(q['scope'])}})
                    output.append({'id': eid, 'kind': 'invoke', 'actorRef': ACTOR, 'route': 'api',
                        'capabilityId': 'assessGoal', 'evidenceRefs': [eid+':actual-artifact'],
                        'request': {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1',
                            'capabilityId': 'assessGoal', 'subjectRefs': [], 'slots': {'workId': copy.deepcopy(work)},
                            'provenance': {'workId': 'CONTEXT'}, 'expectedRevision': revision, 'commandIdempotencyKey': eid}})
                q['id'] = ref(eid, '/response/effects/assessmentId')
            output.append(action)
        if case['caseId'] == 'T09' and sub['id'] in ('cumulative-versus-state', 'exists-versus-end-throughout'):
            # All physical source writes are complete by 01:30 / 01:00. Assess
            # explicitly at the already authored 02:00 goal horizon, then let
            # the unchanged 04:00 query read that immutable earlier assessment.
            # No relevant late source is introduced between these two clocks.
            evaluation_ids = {q['id']+suffix for q in queries for suffix in ('-evaluation-work', '-evaluate')}
            evaluations = [a for a in output if a['id'] in evaluation_ids]
            output = [a for a in output if a['id'] not in evaluation_ids and a['id'] != 'clock-assessment']
            at = next(i for i, a in enumerate(output) if a['id'] == 'clock-known')
            horizon = queries[0]['request']['asOf']
            clock = copy.deepcopy(output[at])
            clock['id'] = 'clock-assessment'
            clock['control']['parameters']['instant'] = horizon
            clock['control']['parameters']['knownAt'] = horizon
            clock['evidenceRefs'] = ['clock-assessment:actual-artifact']
            output[at:at] = [clock, *evaluations]
        sub['actions'] = output
    return case


def reverse(case_id, value):
    """Validate and undo only the individually reviewed selector corrections."""
    result = copy.deepcopy(value)
    ledger = json.loads((Path(__file__).with_name('assessment-binding-corrections.json')).read_text())
    for change in reversed(ledger['changes']):
        if change['caseId'] != case_id:
            continue
        node = result
        path = list(change['path'])
        if 'actionId' in change:
            path[3] = next(i for i, a in enumerate(result['subcases'][path[1]]['actions']) if a['id'] == change['actionId'])
        for key in path[:-1]:
            node = node[key]
        key = path[-1]
        if node[key] != change['after']:
            raise AssertionError(f"Unreviewed binding drift: {case_id} {change['path']}")
        node[key] = copy.deepcopy(change['before'])
    return result


def original_view(case_id, value):
    """For pre-existing source preservation tests, remove this additive layer."""
    value = reverse(case_id, value)
    for sub in value['subcases']:
        sub['actions'] = [a for a in sub['actions'] if not ((a.get('actorRef') == ACTOR and a.get('capabilityId') == 'assessGoal') or a['id'].endswith('-evaluation-work') or a['id'] == 'clock-assessment')]
        for action in sub['actions']:
            if action.get('capabilityId') == 'getAssessment':
                action['request'].pop('id', None)
    return value


def original_fixture(value):
    value = copy.deepcopy(value)
    value['aliases'].pop('syntheticPolicyVersion', None)
    value['aliases'].pop(ACTOR, None)
    value['actors'].pop(ACTOR, None)
    return value
