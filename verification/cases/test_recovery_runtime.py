"""Input shape regression only; actual backend failures remain visible."""
import copy
import json
import unittest
from pathlib import Path
from author_recovery_runtime import apply

ROOT = Path(__file__).resolve().parents[2]

class RecoveryRuntimeInputs(unittest.TestCase):
    def test_only_missing_response_references_change_and_assertions_are_preserved(self):
        case = {'caseId': 'T26', 'subcases': [{'id': 'input-only', 'actions': [
            {'id': 'retry', 'capabilityId': 'retrySafeCommand', 'request': {
                'expectedRevision': {'$result': {'actionId': 'original', 'pointer': '/response/revision'}},
                'slots': {'commandId': {'$result': {'actionId': 'original', 'pointer': '/response/commandId'}}}}},
            {'id': 'evidence', 'request': {'subjectRefs': [{'id': {'$result': {'actionId': 'split', 'pointer': '/response/segmentId'}}}]}}
        ], 'assertions': [{'id': 'movement-once', 'expected': 1}, {'id': 'owner', 'expected': 'owner'}, {'id': 'negative', 'expected': 0}]}]}
        assertions = copy.deepcopy(case['subcases'][0]['assertions'])
        self.assertEqual(2, apply(case))
        self.assertNotIn('expectedRevision', case['subcases'][0]['actions'][0]['request'])
        self.assertEqual('/response/children/0/segmentId', case['subcases'][0]['actions'][1]['request']['subjectRefs'][0]['id']['$result']['pointer'])
        self.assertEqual(assertions, case['subcases'][0]['assertions'])
        self.assertEqual(0, apply(case))

    def test_single_application_stop_preserves_the_business_assertions(self):
        case = {'caseId': 'V5', 'subcases': [{'id': 'waiting-full-restart', 'actions': [
            {'id': 'stop-api', 'control': {'operation': 'stop', 'parameters': {'processId': 'api'}}},
            {'id': 'stop-worker', 'control': {'operation': 'stop', 'parameters': {'processId': 'worker-a'}}},
            {'id': 'restart-all-api', 'control': {'operation': 'restart', 'parameters': {'processId': 'api'}}},
        ], 'assertions': [{'id': 'one-duty', 'expected': 1}]}]}
        assertions = copy.deepcopy(case['subcases'][0]['assertions'])
        self.assertEqual(3, apply(case))
        self.assertEqual(['stop-api', 'restart-all-api'], [a['id'] for a in case['subcases'][0]['actions']])
        self.assertEqual('application', case['subcases'][0]['actions'][0]['control']['parameters']['processId'])
        self.assertEqual('start', case['subcases'][0]['actions'][1]['control']['operation'])
        self.assertEqual(assertions, case['subcases'][0]['assertions'])
        self.assertEqual(0, apply(case))

    def test_committed_cases_are_fixed_points_with_all_oracles(self):
        for cid, count in [('V5', 91), ('T26', 710)]:
            case = json.loads((ROOT / f'verification/cases/{cid}/case.json').read_text())
            self.assertEqual(count, sum(len(sub['assertions']) for sub in case['subcases']))
            self.assertEqual(0, apply(case))

if __name__ == '__main__':
    unittest.main()
