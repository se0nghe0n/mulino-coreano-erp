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


    def test_all_negative_paths_allow_scoped_read_audit(self):
        negatives = [t['oracle'] for c in self.data['cases'] for t in c['turns']
                     if 'uatCompletion' in t['oracle']]
        self.assertEqual(11, len(negatives))
        for oracle in negatives:
            audits = [e for e in oracle['allowedEffects'] if e['class'] == 'READ_AUDIT']
            self.assertEqual(1, len(audits))
            self.assertEqual(oracle['uatCompletion']['readAuditScope'], audits[0]['scope'])
            self.assertGreater(audits[0]['maxNew'], 0)
        self.assertEqual([], VALIDATOR.validate(self.data))

    def test_negative_preflight_without_read_audit_fails(self):
        def mutate(d):
            oracle = d['cases'][56]['turns'][0]['oracle']
            oracle['allowedEffects'] = [e for e in oracle['allowedEffects'] if e['class'] != 'READ_AUDIT']
        self.reject(mutate, 'must allow scoped READ_AUDIT')

    def test_read_audit_cannot_read_another_actor_scope(self):
        def mutate(d):
            oracle = d['cases'][56]['turns'][0]['oracle']
            oracle['uatCompletion']['readAuditScope']['actorRef'] = 'actor'
        self.reject(mutate, 'current actor-scoped READ authorization')

    def test_read_audit_cannot_expand_targets(self):
        def mutate(d):
            oracle = d['cases'][56]['turns'][0]['oracle']
            oracle['uatCompletion']['readAuditScope']['targetRefs'].append('otherOrg')
        self.reject(mutate, 'current actor-scoped READ authorization')

    def test_m47_preflight_preserves_existing_allocation_and_human_duty(self):
        case = self.data['cases'][46]
        paths = case['turns'][0]['oracle']['uatCompletion']['pathOracles']
        preflight = paths['EVIDENCED_PREFLIGHT_STOP']
        values = {a['path']: a['expected'] for a in preflight['assertions']}
        self.assertEqual(case['fixture']['allocation']['status'], values['state.allocation.status'])
        self.assertEqual(len(case['fixture']['obligations']), values['state.currentObligationCount'])
        self.assertEqual(0, preflight['businessEffectsMaximum'])
        self.assertEqual([], preflight['allowedEffects'])
        self.assertEqual('salesOwner', case['fixture']['obligations'][0]['ownerRef'])
        self.assertEqual([], VALIDATOR.validate(self.data))

    def test_m47_preflight_cannot_silently_create_suspension(self):
        self.reject(lambda d: d['cases'][46]['turns'][0]['oracle']['uatCompletion']['pathOracles'][
            'EVIDENCED_PREFLIGHT_STOP']['allowedEffects'].append(
                {'class': 'ALLOCATION_SUSPENSION', 'maxNew': 1}),
            'preflight must preserve allocation/duty')

    def test_m47_preflight_cannot_expect_suspension_from_read(self):
        def mutate(d):
            assertions = d['cases'][46]['turns'][0]['oracle']['uatCompletion']['pathOracles'][
                'EVIDENCED_PREFLIGHT_STOP']['assertions']
            for a in assertions:
                if a['path'] == 'state.allocation.status':
                    a['expected'] = 'SUSPENDED'
        self.reject(mutate, 'preflight must preserve allocation/duty')

    def test_m47_requires_existing_responsibility_before_preflight(self):
        self.reject(lambda d: d['cases'][46]['fixture'].__setitem__('obligations', []),
                    'existing human delivery responsibility')

    def test_m47_read_probe_cannot_restore_write_permission(self):
        self.reject(lambda d: d['cases'][46]['fixture']['grants']['readOnlyProbeGrant'][
            'actions'].append('DISPATCH'), 'must not revive revoked WRITE authorization')

    def test_m47_server_rejection_still_requires_new_reauthorization(self):
        self.reject(lambda d: d['cases'][46]['turns'][0]['oracle']['uatCompletion']['pathOracles'][
            'SERVER_REJECTION'].__setitem__('obligations', []),
            'server rejection must retain new duty')

    def test_m47_sit_cannot_drop_suspension_or_duty(self):
        self.reject(lambda d: d['cases'][46]['turns'][0]['oracle']['sitDirectCommand'].__setitem__(
            'assertions', []), 'SIT must retain the same mandatory server rejection oracle')

    def test_m47_safety_mutations_are_not_common_preflight_effects(self):
        self.reject(lambda d: d['cases'][46]['turns'][0]['oracle']['allowedEffects'].append(
            {'class': 'OBLIGATION', 'maxNew': 1}), 'safety transitions cannot be common preflight')

    def test_m47_cannot_drop_allocation_consumption_zero(self):
        def mutate(d):
            assertions = d['cases'][46]['turns'][0]['oracle']['assertions']
            for a in assertions:
                if a['path'] == 'effects.allocationConsumptionCount':
                    a['expected'] = 1
        self.reject(mutate, 'no unauthorized execution and original responsibility')

    def test_completion_cannot_union_both_path_effects(self):
        self.reject(lambda d: d['cases'][46]['turns'][0]['oracle']['uatCompletion'].__setitem__(
            'effectComposition', 'ALL_PATHS_COMBINED'), 'only selected completion path effects')


class ReviewedCorpusPinTests(unittest.TestCase):
    """Count-preserving oracle weakening must not survive by regenerating bindings."""
    ROOT = HERE.resolve().parents[1]

    def copy_tree(self, temp):
        import shutil
        for ref in ['verification/model-corpus', 'verification/model-binding']:
            shutil.copytree(self.ROOT / ref, Path(temp) / ref, ignore=shutil.ignore_patterns('__pycache__'))
        (Path(temp) / 'verification/requirements').mkdir(parents=True)
        shutil.copy(self.ROOT / 'verification/requirements/normative-contract-lock.json',
                    Path(temp) / 'verification/requirements/normative-contract-lock.json')
        return Path(temp)

    def weaken(self, root, case_id, change):
        path = root / 'verification/model-corpus/corpus.json'
        data = json.loads(path.read_text())
        change(next(c for c in data['cases'] if c['id'] == case_id))
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
        return path, data

    def test_canonical_corpus_matches_reviewed_pin(self):
        self.assertEqual([], VALIDATOR.reviewed_pin_errors(HERE / 'corpus.json'))

    def test_m16_e1_quantity_weakening_is_structurally_valid_but_pin_rejected(self):
        import subprocess, sys, tempfile
        def held_100(case):
            for turn in case['turns']:
                for assertion in turn['oracle']['assertions']:
                    if assertion['path'] == 'response.onHand.value':
                        assertion['expected'] = '100'
        with tempfile.TemporaryDirectory() as temp:
            root = self.copy_tree(temp)
            path, data = self.weaken(root, 'M16', held_100)
            self.assertEqual([], VALIDATOR.validate(data))  # the structural validator alone misses it
            self.assertTrue(VALIDATOR.reviewed_pin_errors(path, root))
            registry = (root / 'verification/model-binding/registry.json').read_bytes()
            done = subprocess.run([sys.executable, '-I', str(root / 'verification/model-binding/generate.py')],
                                  capture_output=True, text=True)
            self.assertNotEqual(0, done.returncode)
            self.assertIn('REFUSED', done.stderr)
            self.assertEqual(registry, (root / 'verification/model-binding/registry.json').read_bytes())

    def test_effect_bound_and_allowed_class_weakening_is_pin_rejected(self):
        import tempfile
        mutations = {
            'M08': lambda case: [e.update(maxNew=e['maxNew'] + 1) for t in case['turns'] for e in t['oracle']['allowedEffects'] if e['class'] != 'READ_AUDIT'],
            'M03': lambda case: [t['oracle']['assertions'].pop() for t in case['turns'][:1]],
        }
        for case_id, change in mutations.items():
            with self.subTest(case=case_id), tempfile.TemporaryDirectory() as temp:
                root = self.copy_tree(temp)
                path, _ = self.weaken(root, case_id, change)
                self.assertTrue(VALIDATOR.reviewed_pin_errors(path, root))


if __name__ == '__main__':
    unittest.main()
