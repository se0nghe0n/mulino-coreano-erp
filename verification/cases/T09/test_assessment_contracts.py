"""Owned binding corrections preserve the 269 semantic oracles."""
import copy
import hashlib
import json
import subprocess
import unittest
import sys
from pathlib import Path
from assessment_contracts import ROOT, ACTOR, apply, reverse


class AssessmentContracts(unittest.TestCase):
    def test_original_semantic_oracles_survive_every_reviewed_binding_change(self):
        counts = []
        ledger = json.loads(Path(__file__).with_name('assessment-binding-corrections.json').read_text())
        for case_id in ('T09', 'T11', 'T12'):
            current = json.loads((ROOT/f'verification/cases/{case_id}/case.json').read_text())
            before = json.loads(subprocess.check_output(['git', 'show', ledger['baseline']+f':verification/cases/{case_id}/case.json'], cwd=ROOT))
            restored = reverse(case_id, current)
            self.assertEqual([s['assertions'] for s in before['subcases']], [s['assertions'] for s in restored['subcases']])
            counts.append(sum(len(s['assertions']) for s in current['subcases']))
            # Mutation control: a changed business expected value is never undone
            # by the structural binding ledger.
            mutated = copy.deepcopy(current)
            mutated['subcases'][0]['assertions'][0]['expected'] = 'FORGED'
            if case_id == 'T11':
                # The new whole-layer inverse is stricter: forged oracles are
                # rejected before the older structural selector ledger runs.
                with self.assertRaises(ValueError):
                    reverse(case_id, mutated)
            else:
                self.assertNotEqual([s['assertions'] for s in before['subcases']], [s['assertions'] for s in reverse(case_id, mutated)['subcases']])
            for old, new in zip(before['subcases'], current['subcases']):
                old_ids = [a['id'] for a in old['actions']]
                self.assertEqual(old_ids, [a['id'] for a in new['actions'] if a['id'] in old_ids])
                self.assertEqual([a.get('actorRef') for a in old['actions']], [a.get('actorRef') for a in new['actions'] if a['id'] in old_ids])
        self.assertEqual([94, 110, 65], counts)

    def test_t11_composed_inverse_rejects_unrecorded_inputs_and_actor_changes(self):
        current = json.loads((ROOT/'verification/cases/T11/case.json').read_text())
        for field, value in (('startQuantity', '1'), ('targetWorkId', {'$alias': 'FORGED'})):
            mutated = copy.deepcopy(current)
            action = next(a for a in mutated['subcases'][0]['actions'] if a['id'] == 'child30-link')
            action['request']['slots'][field] = value
            with self.assertRaises(ValueError):
                reverse('T11', mutated)
        mutated = copy.deepcopy(current)
        next(a for a in mutated['subcases'][0]['actions'] if a['id'] == 'child30-link')['actorRef'] = 'supervisor'
        with self.assertRaises(ValueError):
            reverse('T11', mutated)

    def test_correction_ledger_is_only_structural(self):
        ledger = json.loads(Path(__file__).with_name('assessment-binding-corrections.json').read_text())
        allowed = {('/response/id', '/response/data/ID'),
                   ('/response/data/result', '/response/data/outcome'),
                   ('/response/activityId', '/response/id'),
                   ('result', 'outcome'), ('quantity', 'targetQuantity')}
        for change in ledger['changes']:
            old, new = change['before'], change['after']
            if isinstance(old, dict):
                restored = copy.deepcopy(new)
                restored['version'] = restored.pop('goalVersion')
                self.assertEqual(old, restored)
                self.assertEqual('where', change['path'][-1])
            else:
                self.assertIn((old, new), allowed)
                self.assertIn(change['path'][-1], ('pointer', 'field'))
        self.assertEqual(126, len(ledger['changes']))

    def test_explicit_policy_matches_original_version_content_and_period(self):
        for case_id in ('T09', 'T11', 'T12'):
            for path in (ROOT/f'verification/cases/{case_id}/fixtures').glob('*.json'):
                fixture = json.loads(path.read_text())
                policy = fixture['baseline']['policies'][0]
                alias = fixture['aliases']['syntheticPolicyVersion']
                self.assertEqual(('PolicyVersion', 'EVIDENCE', policy['version']), (alias['type'], alias['kind'], alias['version']))
                self.assertEqual(policy, alias['content'])
                self.assertTrue(alias['activate'])
                self.assertLessEqual(alias['effectiveFrom'], fixture['clock']['asOf'])
                self.assertGreaterEqual(alias['effectiveUntil'], fixture['clock']['asOf'])

    def test_all_fixture_facts_and_original_feature_lines_are_unchanged(self):
        from assessment_contracts import original_fixture
        baseline = '2c290f81cd3c2b32d4abf6c48f8072e9ccb624ef'
        for case_id in ('T09', 'T11', 'T12'):
            for path in (ROOT/f'verification/cases/{case_id}/fixtures').glob('*.json'):
                relative = str(path.relative_to(ROOT))
                before = json.loads(subprocess.check_output(['git', 'show', baseline+':'+relative], cwd=ROOT))
                self.assertEqual(before, original_fixture(json.loads(path.read_text())), relative)
            relative = f'verification/cases/{case_id}/scenario.feature'
            before = subprocess.check_output(['git', 'show', baseline+':'+relative], cwd=ROOT).decode()
            feature = (ROOT/relative).read_text()
            if case_id == 'T11':
                import importlib.util
                spec = importlib.util.spec_from_file_location(
                    't11_parent_ranges', ROOT/'verification/cases/T11/author_parent_ranges.py')
                parent_ranges = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(parent_ranges)
                with self.assertRaises(ValueError):
                    parent_ranges.restore_feature(feature.replace('child30-link', 'FORGED', 1))
                feature = parent_ranges.restore_feature(feature)
            lines = feature.splitlines(keepends=True)
            restored = ''.join(line for line in lines if '"assessmentEvaluator" 역할이' not in line and '-evaluation-work" 행동' not in line and '"clock-assessment" 행동' not in line)
            self.assertEqual(before, restored, relative)

    def test_original_goal_horizon_is_assessed_before_later_knowledge_query(self):
        case = json.loads((ROOT/'verification/cases/T09/case.json').read_text())
        for sub in case['subcases']:
            if sub['id'] not in ('cumulative-versus-state', 'exists-versus-end-throughout'):
                continue
            now = json.loads((ROOT/sub['fixtureRef']).read_text())['clock']['asOf']
            evaluation_times = {}
            for action in sub['actions']:
                if action['kind'] == 'control' and action['control']['type'] == 'clock':
                    next_time = action['control']['parameters']['instant']
                    self.assertLessEqual(now, next_time, 'The product clock never moves backwards')
                    now = next_time
                if action.get('capabilityId') == 'assessGoal':
                    evaluation_times[action['id']] = now
                if action.get('capabilityId') == 'getAssessment':
                    self.assertEqual('2026-10-07T02:00:00Z', action['request']['asOf'])
                    self.assertEqual('2026-10-07T04:00:00Z', action['request']['knownAt'])
                    self.assertEqual(action['request']['asOf'], evaluation_times[action['id']+'-evaluate'])
            actions = sub['actions']
            first = next(i for i,a in enumerate(actions) if a['id']=='clock-assessment')
            late = next(i for i,a in enumerate(actions) if a['id']=='clock-known')
            self.assertTrue(all(a.get('capabilityId') in ('getWork','assessGoal') for a in actions[first+1:late]))
            # The later knowledge boundary introduces no source, receipt,
            # movement, policy or goal writes that would change these facts.
            self.assertTrue(all(a['kind'] in ('query','observe') for a in actions[late+1:]))

    def test_queries_bind_explicit_evaluation_and_current_revision(self):
        sys.path.insert(0, str(ROOT / "verification/cases"))
        from request_contract import Contracts
        contract = Contracts(ROOT)
        evaluations = 0
        for case_id in ('T09', 'T11', 'T12'):
            case = json.loads((ROOT/f'verification/cases/{case_id}/case.json').read_text())
            fixtures = {}
            def fixture(ref):
                return fixtures.setdefault(ref, json.loads((ROOT/ref).read_text()))
            self.assertEqual(case, apply(copy.deepcopy(case), fixture))
            for sub in case['subcases']:
                acts = sub['actions']
                for index, action in enumerate(acts):
                    if action.get('capabilityId') != 'getAssessment':
                        continue
                    read = next(a for a in acts if a['id'] == action['id']+'-evaluation-work')
                    evaluate = next(a for a in acts if a['id'] == action['id']+'-evaluate')
                    self.assertLess(acts.index(read), acts.index(evaluate))
                    self.assertLess(acts.index(evaluate), index)
                    self.assertEqual('getWork', read['capabilityId'])
                    self.assertEqual('assessGoal', evaluate['capabilityId'])
                    self.assertEqual(ACTOR, evaluate['actorRef'])
                    self.assertEqual(action['request']['workId'], evaluate['request']['slots']['workId'])
                    self.assertEqual({'$result': {'actionId': read['id'], 'pointer': '/response/data/revision'}}, evaluate['request']['expectedRevision'])
                    self.assertEqual({'$result': {'actionId': evaluate['id'], 'pointer': '/response/effects/assessmentId'}}, action['request']['id'])
                    problems, known = contract.command_problems(evaluate, sub)
                    self.assertEqual(([], []), (problems, known))
                    principal = fixture(sub['fixtureRef'])['actors'][ACTOR]
                    self.assertEqual(['assessGoal'], principal['roleCapabilities'])
                    self.assertEqual(['assessGoal'], principal['grant']['actions'])
                    self.assertEqual({'organizationAlias': 'ORG', 'itemAliases': ['P']}, principal['grant']['scope'])
                    evaluations += 1
        self.assertEqual(27, evaluations)


if __name__ == '__main__':
    unittest.main()
