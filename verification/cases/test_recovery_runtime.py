"""Input shape regression only; actual backend failures remain visible."""
import copy
import json
import unittest
from pathlib import Path
from author_recovery_runtime import apply

ROOT = Path(__file__).resolve().parents[2]

class RecoveryRuntimeInputs(unittest.TestCase):
    def test_only_missing_response_references_change_and_assertions_are_preserved(self):
        case = {'caseId': 'T26', 'subcases': [{'actions': [
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

    def test_committed_cases_are_fixed_points_with_all_oracles(self):
        for cid, count in [('V5', 91), ('T26', 710)]:
            case = json.loads((ROOT / f'verification/cases/{cid}/case.json').read_text())
            self.assertEqual(count, sum(len(sub['assertions']) for sub in case['subcases']))
            self.assertEqual(0, apply(case))

if __name__ == '__main__':
    unittest.main()
