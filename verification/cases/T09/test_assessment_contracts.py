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
            self.assertNotEqual([s['assertions'] for s in before['subcases']], [s['assertions'] for s in reverse(case_id, mutated)['subcases']])
            for old, new in zip(before['subcases'], current['subcases']):
                old_ids = [a['id'] for a in old['actions']]
                self.assertEqual(old_ids, [a['id'] for a in new['actions'] if a['id'] in old_ids])
                self.assertEqual([a.get('actorRef') for a in old['actions']], [a.get('actorRef') for a in new['actions'] if a['id'] in old_ids])
        self.assertEqual([94, 110, 65], counts)

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
            lines = (ROOT/relative).read_text().splitlines(keepends=True)
            restored = ''.join(line for line in lines if '"assessmentEvaluator" 역할이' not in line and '-evaluation-work" 행동' not in line)
            self.assertEqual(before, restored, relative)

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
                    read, evaluate = acts[index-2:index]
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
