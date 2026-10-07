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


    def test_effective_case_read_grant_cannot_expand(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'grants', {'readGrant': {'actions': ['READ', 'DISPATCH']}}),
            'effective READ grant boundary')

    def test_effective_case_target_scope_cannot_wildcard(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'grants', {'readGrant': {'targetScope': ['*']}}),
            'effective grant readGrant wildcard')

    def test_effective_case_role_cannot_wildcard(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'actors', {'readAgent': {'roles': ['*']}}), 'wildcard roles')

    def test_effective_case_cannot_blanket_approve(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'approvalFacts', [{'allActionsApproved': True}]), 'blanket approvalFacts')

    def test_effective_case_owner_must_remain_human(self):
        self.reject(lambda d: d['cases'][53]['fixture'].__setitem__(
            'actors', {'intakeOwner': {'human': False}}), 'explicit human owner')

    def test_query_whitelist_rejects_nonprotected_business_classes(self):
        for effect in ('PURCHASE_PROPOSAL', 'INVOICE', 'EVIDENCE_LINK', 'FUTURE_WRITE'):
            with self.subTest(effect=effect):
                d = copy.deepcopy(self.data)
                d['cases'][30]['turns'][0]['oracle']['allowedEffects'].append(
                    {'class': effect, 'maxNew': 1})
                errors = VALIDATOR.validate(d)
                self.assertTrue(any('QUERY permits only READ_AUDIT' in e for e in errors), errors)

    def test_needs_input_whitelist_rejects_invoice_write(self):
        self.reject(lambda d: d['cases'][20]['turns'][0]['oracle']['allowedEffects'].append(
            {'class': 'INVOICE', 'maxNew': 1}), 'NEEDS_INPUT permits only non-business audit')

    def test_unlisted_effects_cannot_be_implicitly_allowed(self):
        self.reject(lambda d: d['cases'][30]['turns'][0]['oracle'].__setitem__(
            'unlistedEffectPolicy', 'ALLOWED'), 'unlisted effect classes must be forbidden')

    def test_recall_counterexample_requires_authenticated_admin(self):
        self.reject(lambda d: d['cases'][56]['fixture']['authentication'].__setitem__(
            'actorRef', 'actor'), 'authenticated ADMIN required')

    def test_recall_counterexample_requires_scoped_close_capability(self):
        self.reject(lambda d: d['cases'][56]['fixture']['grants']['recallClosureGrant'].__setitem__(
            'actions', ['READ']), 'scoped RC1 RECALL_CLOSE')

    def test_correction_counterexample_requires_goal100(self):
        self.reject(lambda d: d['cases'][55]['fixture']['objects']['S1'].__setitem__(
            'quantity', '30'), 'S1 goal100')

    def test_correction_requires_delivery_goal_contribution(self):
        self.reject(lambda d: d['cases'][55]['fixture']['delivery'].__setitem__(
            'orderRef', 'O1'), 'S1 goal100')

    def test_context_source_must_resolve_exact_value(self):
        self.reject(lambda d: d['cases'][49]['turns'][0]['expectedIntent']['slots']['placeRef'].__setitem__(
            'sourceRef', 'fixture.objects.A.ref'), 'differs from effective fixture source')

    def test_context_source_cannot_be_generic_fixture(self):
        self.reject(lambda d: d['cases'][58]['turns'][0]['expectedIntent']['slots']['orderRef'].__setitem__(
            'sourceRef', 'fixture'), 'unresolved effective fixture source')

    def test_user_source_excerpt_must_be_in_actual_turn(self):
        self.reject(lambda d: d['cases'][0]['turns'][0]['expectedIntent']['slots']['itemRef'].__setitem__(
            'sourceText', 'invented request text'), 'verbatim sourceText')

    def test_approval_hash_cannot_be_changed_to120(self):
        self.reject(lambda d: d['cases'][44]['fixture']['proposal']['canonicalPayload'].__setitem__(
            'quantity', '120'), 'exact hash/revision approval100')

    def test_uat_can_preflight_without_direct_error(self):
        oracle = self.data['cases'][56]['turns'][0]['oracle']
        self.assertIn('EVIDENCED_PREFLIGHT_STOP', oracle['uatCompletion']['allowedPaths'])
        self.assertNotIn('response.errorCode', [a['path'] for a in oracle['assertions']])
        self.assertEqual('UNRESOLVED_RECALL_SCOPE',
                         oracle['sitDirectCommand']['assertions'][0]['expected'])
        self.assertEqual([], VALIDATOR.validate(self.data))

    def test_uat_cannot_force_server_error_into_common_oracle(self):
        self.reject(lambda d: d['cases'][56]['turns'][0]['oracle']['assertions'].append(
            {'path': 'response.errorCode', 'operator': 'eq', 'expected': 'UNRESOLVED_RECALL_SCOPE'}),
            'UAT common oracle must not force direct server error')

    def test_uat_model_explanation_never_suffices(self):
        self.reject(lambda d: d['cases'][56]['turns'][0]['oracle']['uatCompletion'].__setitem__(
            'modelExplanationSufficient', True), 'UAT cannot pass on model explanation')

    def test_uat_preflight_requires_independent_snapshot(self):
        self.reject(lambda d: d['cases'][56]['turns'][0]['oracle']['uatCompletion'].__setitem__(
            'requiresIndependentArtifacts', ['authenticated_constraint_read']),
            'independent observation artifacts')

    def test_uat_cannot_drop_common_safety_oracle(self):
        self.reject(lambda d: d['cases'][56]['turns'][0]['oracle']['uatCompletion'].__setitem__(
            'commonBusinessAssertionsRequired', False), 'drop safety oracle')


    def test_correction_baseline_must_not_seed_expected_shortfall(self):
        self.reject(lambda d: d['cases'][55]['fixture']['assessment'].__setitem__(
            'currentShortfall', '2'), 'must not seed expected shortfall2')

    def test_uat_preflight_cannot_create_any_business_effect(self):
        self.reject(lambda d: d['cases'][56]['turns'][0]['oracle']['uatCompletion'].__setitem__(
            'preflightBusinessEffectsMaximum', 1), 'preflight must have zero business effects')


    def test_effective_approval_policy_cannot_autoapprove(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'policy', {'purchaseApproval': 'AUTO_APPROVE'}), 'approval policy weakened')

    def test_effective_c3_must_preserve_write_role(self):
        self.reject(lambda d: d['cases'][30]['fixture'].__setitem__(
            'actors', {'readAgent': {'roles': ['READ']}}), 'WRITE role must remain distinct')


if __name__ == '__main__':
    unittest.main()
