"""Fault injection verifies corpus validation; these are not extra UAT cases."""
import copy
import importlib.util
import json
import unittest
from pathlib import Path

HERE = Path(__file__).parent
SPEC = importlib.util.spec_from_file_location('corpus_validator', HERE / 'validate.py')
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


class CorpusValidationTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads((HERE / 'corpus.json').read_text())

    def reject(self, mutate, fragment):
        mutate(self.data)
        errors = VALIDATOR.validate(self.data)
        self.assertTrue(any(fragment in e for e in errors), errors)

    def test_fixed_corpus_integrity_only(self):
        self.assertEqual([], VALIDATOR.validate(self.data))
        self.assertEqual('NOT_RUN', self.data['status'])
        self.assertIsNone(self.data['executionPlan']['usage'])

    def test_missing_case(self):
        self.reject(lambda d: d['cases'].pop(), 'exactly 60')

    def test_duplicate_case_id(self):
        self.reject(lambda d: d['cases'].__setitem__(1, copy.deepcopy(d['cases'][0])),
                    'duplicate case ID')

    def test_wrong_category(self):
        self.reject(lambda d: d['cases'][0].__setitem__('category', 'query_write_boundary'),
                    'fixed distribution')

    def test_repeat_not_three(self):
        self.reject(lambda d: d['cases'][0]['execution'].__setitem__('plannedRepeats', 1),
                    'plannedRepeats')

    def test_unknown_cost_cannot_be_zero(self):
        self.reject(lambda d: d['executionPlan'].__setitem__('totalCost', 0),
                    'totalCost must be null')

    def test_case_partial_usage_cannot_claim_complete(self):
        self.reject(lambda d: d['cases'][0]['execution'].__setitem__('usage', {'inputTokens': 1}),
                    'usage must be explicitly null')

    def test_runtime_pass_invention(self):
        self.reject(lambda d: d['cases'][0]['execution'].__setitem__('status', 'PASS'),
                    'no runtime result')

    def test_expected_intent_leak_in_model_input(self):
        self.reject(lambda d: d['cases'][0]['turns'][0]['input'].__setitem__(
            'expectedIntent', {'intentKind': 'QUERY'}), 'expected data leaked')

    def test_nested_slot_leak_in_model_input(self):
        self.reject(lambda d: d['cases'][0]['turns'][0]['input'].__setitem__(
            'metadata', {'slots': {'quantity': '100'}}), 'expected data leaked')

    def test_query_cannot_allow_write(self):
        def mutate(d):
            oracle = d['cases'][30]['turns'][0]['oracle']
            oracle['forbiddenEffects'].remove('QUANTITY')
            oracle['allowedEffects'].append({'class': 'QUANTITY', 'maxNew': 1})
        self.reject(mutate, 'QUERY allows protected write')

    def test_ambiguous_input_cannot_authorize_business_effect(self):
        def mutate(d):
            oracle = d['cases'][20]['turns'][0]['oracle']
            oracle['forbiddenEffects'].remove('WORK')
            oracle['allowedEffects'].append({'class': 'WORK', 'maxNew': 1})
        self.reject(mutate, 'ambiguity authorizes protected effects')

    def test_clarification_answer_not_business_approval(self):
        self.reject(lambda d: d['cases'][20]['turns'][1]['oracle']['clarification'].__setitem__(
            'approvalByAnswer', True), 'hidden business approval')

    def test_no_wildcard_grants(self):
        self.reject(lambda d: d['commonFixture']['grants']['grant'].__setitem__('actions', ['*']),
                    'wildcard')

    def test_read_grant_cannot_expand(self):
        self.reject(lambda d: d['commonFixture']['grants']['readGrant']['actions'].append('WRITE'),
                    'READ grant boundary')

    def test_missing_slot_provenance(self):
        self.reject(lambda d: d['cases'][0]['turns'][0]['expectedIntent']['slots']['itemRef'].pop(
            'provenance'), 'lacks value/provenance/source')

    def test_missing_responsible_owner(self):
        self.reject(lambda d: d['cases'][53]['turns'][0]['oracle']['obligations'][0].pop('ownerRef'),
                    'obligation lacks owner')

    def test_independent_oracle_cannot_drop_false_fulfillment(self):
        def mutate(d):
            for a in d['cases'][51]['turns'][0]['oracle']['assertions']:
                if a['path'] == 'state.work.O1.fulfilled':
                    a['expected'] = True
        self.reject(mutate, 'required semantic assertion state.work.O1.fulfilled')

    def test_distinct_sixty_not_two_document_sum(self):
        def mutate(d):
            for a in d['cases'][52]['turns'][0]['oracle']['assertions']:
                if a['path'] == 'state.receipt.cumulative.value':
                    a['expected'] = '120'
        self.reject(mutate, 'required semantic assertion state.receipt.cumulative.value')

    def test_recall_and_disposal_cannot_double_count(self):
        def mutate(d):
            for a in d['cases'][56]['turns'][0]['oracle']['assertions']:
                if a['path'] == 'state.recall.distinctProcessed.value':
                    a['expected'] = '50'
        self.reject(mutate, 'required semantic assertion state.recall.distinctProcessed.value')

    def test_foreign_cases_cannot_be_removed(self):
        self.reject(lambda d: [c.__setitem__('languages', ['ko']) for c in d['cases']],
                    'at least 10 foreign')

    def test_obligation_owner_must_be_human(self):
        self.reject(lambda d: d['cases'][53]['turns'][0]['oracle']['obligations'][0].__setitem__(
            'ownerRef', 'actor'), 'explicit human owner')


if __name__ == '__main__':
    unittest.main()
