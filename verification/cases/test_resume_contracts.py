"""Focused authoring regressions; none constitute product acceptance."""
import copy
import json
import unittest
from pathlib import Path
from author_resume_contracts import apply

ROOT = Path(__file__).resolve().parents[2]

class ResumeContracts(unittest.TestCase):
    def test_excluded_effects_keep_zero_effect_oracles(self):
        case = json.loads((ROOT / 'verification/cases/T01/case.json').read_text())
        excluded = [s for s in case['subcases'] if s['id'].startswith('excluded-')]
        self.assertEqual(6, len(excluded))
        for sub in excluded:
            assertions = {a['id']: a for a in sub['assertions']}
            self.assertEqual('HELD', assertions['unsupported-outcome']['expected'])
            self.assertEqual('VERSION_UNSUPPORTED', assertions['unsupported-code']['expected'])
            self.assertEqual(0, assertions['no-substitute-command']['expected'])
            self.assertIn('unsupported-effect', assertions)
            self.assertGreater(len(assertions), 5)

    def test_sales_preserves_effect_assertions_and_other_terms(self):
        case = {'caseId': 'T17', 'subcases': [{'actions': [{'request': {
            'capabilityId': 'createSalesOrder', 'slots': {'deliveryEndpoint': 'CUSTOMER_ACCEPTED', 'quantity': '30'},
            'provenance': {'deliveryEndpoint': 'USER', 'quantity': 'USER'}}}],
            'assertions': [{'expected': 'CUSTOMER_ACCEPTED', 'id': 'unrelated'}]}]}
        before = copy.deepcopy(case['subcases'][0]['assertions'])
        self.assertEqual({'salesEndpoint': 1, 'salesQuality': 1}, dict(apply(case)))
        self.assertEqual(before, case['subcases'][0]['assertions'])
        self.assertEqual('30', case['subcases'][0]['actions'][0]['request']['slots']['quantity'])
        self.assertFalse(apply(case))

    def test_missing_slot_counterexample_is_not_filled(self):
        case = {'caseId': 'T20', 'actions': [{'capabilityId': 'createDraft', 'slots': {
            'quantityMode': 'CUMULATIVE_EVENT', 'dueAt': '2026-10-09T09:00:00Z'}, 'provenance': {}}]}
        before = copy.deepcopy(case)
        self.assertFalse(apply(case))
        self.assertEqual(before, case)

    def test_authorization_period_is_explicit_and_idempotent(self):
        case = {'caseId': 'C3', 'request': {'capabilityId': 'createWork', 'slots': {
            'quantityMode': 'CUMULATIVE_EVENT', 'dueAt': '2026-10-09T09:00:00Z'}, 'provenance': {}}}
        self.assertEqual({'authorizationPeriod': 1}, dict(apply(case)))
        self.assertEqual({'start': '2026-10-07T09:00:00Z', 'end': '2026-10-09T09:00:00Z',
                          'startInclusive': True, 'endInclusive': True}, case['request']['slots']['period'])
        self.assertEqual('USER', case['request']['provenance']['period'])
        self.assertFalse(apply(case))

    def test_goal_form_only_renames_kind(self):
        case = {'caseId': 'V5', 'request': {'capabilityId': 'createWork', 'slots': {
            'goal': {'quantityMode': 'STATE_AT'}, 'workType': 'check'}, 'provenance': {'goal': 'USER', 'workType': 'USER'}}}
        self.assertEqual({'goalKind': 1}, dict(apply(case)))
        self.assertEqual({'goal': {'quantityMode': 'STATE_AT'}, 'kind': 'check'}, case['request']['slots'])
        self.assertNotIn('workType', case['request']['provenance'])

    def test_scoped_definition_grants_retain_other_constraints(self):
        for name in ('explicit-authorized-migration', 'pinned-meaning-current-policy'):
            fixture = json.loads((ROOT / 'verification/cases/T21/fixtures' / (name + '.json')).read_text())
            grant = fixture['actors']['fde']['grant']
            self.assertNotIn('items', grant['scope'])
            self.assertEqual('ORG', grant['scope']['organizationAlias'])
            self.assertIn('createDefinitionDraft', grant['actions'])
            self.assertEqual('supervisor', grant['delegatorAlias'])
            self.assertIn('items', fixture['actors']['owner']['grant']['scope'])

if __name__ == '__main__': unittest.main()
