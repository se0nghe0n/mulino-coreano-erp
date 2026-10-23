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
            restored = INPUTS.recall_inputs.bytes_of(INPUTS.recall_inputs.restore_fixture(json.loads(path.read_bytes())))
            self.assertEqual(subprocess.check_output(['git', 'show', INPUTS.BASE+':'+str(path.relative_to(ROOT))], cwd=ROOT), restored)
        self.assertEqual(subprocess.check_output(['git', 'show', INPUTS.BASE+':verification/cases/T17/scenario.feature'], cwd=ROOT), INPUTS.restore_feature((HERE/'scenario.feature').read_text()).encode())

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
