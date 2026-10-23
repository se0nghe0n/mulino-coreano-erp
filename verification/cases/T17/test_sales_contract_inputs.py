import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
spec = importlib.util.spec_from_file_location('sales_inputs', HERE/'author_sales_contract_inputs.py')
INPUTS = importlib.util.module_from_spec(spec)
spec.loader.exec_module(INPUTS)


class SalesContractInputTest(unittest.TestCase):
    def test_exact_257_ids_oracles_fixtures_grants_and_clock_survive(self):
        before = json.loads(subprocess.check_output(
            ['git', 'show', INPUTS.BASE+':verification/cases/T17/case.json'], cwd=ROOT))
        actual = INPUTS.recall_inputs.restore_case(json.loads((HERE/'case.json').read_text()))
        self.assertEqual(INPUTS.apply(before), actual)
        self.assertEqual(before, INPUTS.restore_case(actual))
        self.assertEqual(actual, INPUTS.apply(actual))
        actual = INPUTS.condition_oracle.restore_case(actual)
        self.assertEqual(257, sum(len(s['assertions']) for s in actual['subcases']))
        for a, b in zip(before['subcases'], actual['subcases']):
            self.assertEqual([x['id'] for x in a['assertions']], [x['id'] for x in b['assertions']])
            self.assertEqual([x.get('expected') for x in a['assertions']], [x.get('expected') for x in b['assertions']])
            self.assertEqual([x['op'] for x in a['assertions']], [x['op'] for x in b['assertions']])
            self.assertEqual([x['oracleRef'] for x in a['assertions']], [x['oracleRef'] for x in b['assertions']])
            self.assertEqual([x.get('actorRef') for x in a['actions']], [x.get('actorRef') for x in b['actions'] if x['id'] in {r['id'] for r in a['actions']}])
        for path in sorted((HERE/'fixtures').glob('*.json')):
            restored = INPUTS.recall_inputs.bytes_of(INPUTS.recall_inputs.restore_fixture(INPUTS.restore_policy_fixture(json.loads(path.read_bytes()))))
            self.assertEqual(subprocess.check_output(['git', 'show', INPUTS.BASE+':'+str(path.relative_to(ROOT))], cwd=ROOT), restored)
        self.assertEqual(subprocess.check_output(['git', 'show', INPUTS.BASE+':verification/cases/T17/scenario.feature'], cwd=ROOT), INPUTS.restore_feature((HERE/'scenario.feature').read_text()).encode())

    def test_policy_layer_only_adds_seven_explicit_inputs_and_restores_301_baseline(self):
        baseline = INPUTS.POLICY_BASE
        raw = subprocess.check_output(['git', 'show', baseline+':verification/cases/T17/case.json'], cwd=ROOT)
        before = json.loads(raw)
        actual = INPUTS.recall_inputs.restore_case(json.loads((HERE/'case.json').read_text()))
        selected = ['normal-consumed-delivery', 'late-restriction-actual-delivery',
                    'post-dispatch-expiry', 'claim-falseclaim', 'claim-unidentifiedscope',
                    'claim-conflictingquantity', 'claim-scopeoverflow']
        ledger = json.loads(INPUTS.POLICY_LEDGER.read_text())
        self.assertEqual(selected, INPUTS.delivery_goal_subcases(before))
        self.assertEqual(selected, ledger['fixtureSubcases'])
        self.assertEqual(before, INPUTS.restore_policy_case(actual))
        self.assertEqual(actual, INPUTS.apply_policy_case(before))
        self.assertEqual(actual, INPUTS.apply_policy_case(actual))
        self.assertEqual(301, sum(len(s['assertions']) for s in actual['subcases']))
        for old, now in zip(before['subcases'], actual['subcases']):
            self.assertEqual(old['assertions'], now['assertions'])
            original_actions = {a['id']: a for a in old['actions']}
            for action in now['actions']:
                if action['id'] != 'complete-order-delivery-goal':
                    self.assertEqual(original_actions[action['id']], action)
            if now['id'] not in selected:
                self.assertEqual(old, now)
        for p in sorted((HERE/'fixtures').glob('*.json')):
            prior = subprocess.check_output(['git', 'show', baseline+':'+str(p.relative_to(ROOT))], cwd=ROOT)
            self.assertEqual(prior, INPUTS.policy_fixture_bytes(INPUTS.recall_inputs.bytes_of(INPUTS.recall_inputs.restore_fixture(json.loads(p.read_bytes())))))
            f = INPUTS.recall_inputs.restore_fixture(json.loads(p.read_text()))
            if p.stem not in selected:
                self.assertEqual(prior, p.read_bytes())
                continue
            previous = json.loads(prior)
            for key in ('actors', 'clock', 'evidence', 'responsibilities'):
                self.assertEqual(previous[key], f[key])
            self.assertEqual(previous['baseline']['sourceProfiles'], f['baseline']['sourceProfiles'])
            self.assertEqual(previous['baseline']['conditions'], f['baseline']['conditions'])
            self.assertEqual(set(previous['aliases']) | {INPUTS.POLICY_ALIAS}, set(f['aliases']))
            self.assertEqual(previous['aliases'], {k:v for k,v in f['aliases'].items() if k != INPUTS.POLICY_ALIAS})
            self.assertEqual('EVIDENCE', f['aliases'][INPUTS.POLICY_ALIAS]['kind'])
            self.assertEqual({}, f['aliases'][INPUTS.POLICY_ALIAS]['content'])
            self.assertEqual(f['clock']['asOf'], f['aliases'][INPUTS.POLICY_ALIAS]['effectiveFrom'])
            self.assertFalse(f['aliases'][INPUTS.POLICY_ALIAS]['operatingApprovalClaimed'])
        self.assertEqual(subprocess.check_output(['git', 'show', baseline+':verification/cases/T17/scenario.feature'], cwd=ROOT), INPUTS.recall_inputs.restore_feature((HERE/'scenario.feature').read_text()).encode())

    def test_policy_inverse_rejects_tampered_inputs_and_preserves_other_writers(self):
        case = json.loads((HERE/'case.json').read_text())
        for key in ('evidencePolicyVersion', 'defaultContext'):
            changed = copy.deepcopy(case)
            goal = next(a for a in changed['subcases'][1]['actions'] if a['id']=='complete-order-delivery-goal')['request']['slots']['goal']
            goal[key] = 'tampered'
            with self.assertRaises(ValueError): INPUTS.restore_policy_case(changed)
            with self.assertRaises(ValueError): INPUTS.apply_policy_case(changed)
        p = HERE/'fixtures/late-restriction-actual-delivery.json'
        fixture = json.loads(p.read_text())
        for key in ('version', 'content', 'effectiveFrom'):
            changed = copy.deepcopy(fixture)
            changed['aliases'][INPUTS.POLICY_ALIAS][key] = 'tampered'
            with self.assertRaises(ValueError): INPUTS.restore_policy_fixture(changed)
        for key in (INPUTS.POLICY_ALIAS, INPUTS.POLICY_META):
            changed = copy.deepcopy(fixture)
            del (changed['aliases'] if key == INPUTS.POLICY_ALIAS else changed['baseline']['policy'])[key]
            with self.assertRaises(ValueError): INPUTS.restore_policy_fixture(changed)
        fixture['aliases']['independentWriterWork'] = {'type':'Work'}
        restored = INPUTS.restore_policy_fixture(fixture)
        self.assertEqual({'type':'Work'}, restored['aliases']['independentWriterWork'])

    def test_warehouse_scope_and_report_source_are_explicit_and_open_duty_keeps_business_checks(self):
        case = json.loads((HERE/'case.json').read_text())
        for sub in case['subcases']:
            actions = {a['id']: a for a in sub['actions']}
            if sub['id'] == 'reserve-pick-dispatch':
                self.assertEqual({'$alias':'W'}, actions['dispatched']['request']['scope']['placeId'])
                self.assertNotIn('placeId', actions['dispatched-db']['observation']['scope'])
                self.assertNotIn('placeId', actions['dispatched-item']['request']['scope'])
                self.assertEqual('dispatched-item', actions['dispatched-db']['observation']['snapshotRef']['$result']['actionId'])
                self.assertEqual('40', next(a for a in sub['assertions'] if a['id']=='warehouse-after-dispatch-12')['expected'])
            if not sub['id'].startswith('claim-'):
                continue
            event = actions['claim']['request']['slots']['eventId']
            for assertion in sub['assertions']:
                source = assertion['source']
                if source.get('pointer') == '/data/rawRows/deliveryObservations':
                    self.assertEqual(event, source['where']['eventId'])
                if source.get('where',{}).get('kind') == 'DELIVERY_RECONCILIATION':
                    self.assertEqual(True, source['where']['current'])
                    self.assertEqual('OPEN', source['where']['status'])
            for field in ('ownerId','nextAction','nextCheckAt'):
                self.assertTrue(any(a['source'].get('field')==field and a['source'].get('where',{}).get('kind')=='DELIVERY_RECONCILIATION' for a in sub['assertions']))

    def test_unrecorded_source_selector_drift_fails_inverse(self):
        case = json.loads((HERE/'case.json').read_text())
        claim = next(s for s in case['subcases'] if s['id']=='claim-falseclaim')
        assertion = next(a for a in claim['assertions'] if a['id']=='claim-not-canonical-11')
        assertion['source']['where']['eventId'] = {'$alias':'agency-proof'}
        with self.assertRaises(ValueError):
            INPUTS.restore_case(case)


if __name__ == '__main__':
    unittest.main()
