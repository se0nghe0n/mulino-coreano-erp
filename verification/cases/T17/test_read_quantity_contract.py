import copy
import author_sales_contract_inputs as SALES
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('read_quantity', HERE/'author_read_quantity_contract.py')
READ = importlib.util.module_from_spec(spec)
spec.loader.exec_module(READ)


class ReadQuantityContractTest(unittest.TestCase):
    def test_exact_inverse_preserves_all_301_business_oracles_and_fixture_bytes(self):
        raw = subprocess.check_output(['git', 'show', READ.BASE+':verification/cases/T17/case.json'], cwd=READ.ROOT)
        before = json.loads(raw)
        current = SALES.restore_policy_case(SALES.response_scope.restore_case(json.loads((HERE/'case.json').read_text())))
        self.assertEqual(READ.apply(before), current)
        self.assertEqual(current, READ.apply(current))
        self.assertEqual(raw, READ.bytes_of(READ.restore_case(current)))
        count = changes = 0
        for old, new in zip(before['subcases'], current['subcases']):
            prior = {a['id']: a for a in old['assertions']}
            self.assertEqual(list(prior), [a['id'] for a in new['assertions'] if a['id'] in prior])
            for now in new['assertions']:
                if now['id'] not in prior:
                    self.assertTrue(now['id'].startswith(READ.PREFIX))
                    continue
                old_assertion = prior[now['id']]
                for key in ('id', 'op', 'expected', 'oracleRef', 'oracleExplanation', 'unit', 'requirementRefs', 'scope'):
                    self.assertEqual(old_assertion.get(key), now.get(key), (old['id'], now['id'], key))
                count += 1
                if old_assertion != now:
                    changes += 1
                    self.assertEqual({k: v for k, v in old_assertion.items() if k not in ('source', 'unitSource', 'evidenceRefs')},
                                     {k: v for k, v in now.items() if k not in ('source', 'unitSource', 'evidenceRefs')})
            actions = {a['id']: a for a in new['actions']}
            for a in old['actions']:
                self.assertEqual(a, actions[a['id']])
        self.assertEqual(301, count)
        self.assertEqual(13, changes)
        ledger = json.loads(READ.LEDGER.read_text())
        self.assertEqual(hashlib.sha256(raw).hexdigest(), ledger['baselineCaseSha256'])
        for path in (HERE/'fixtures').glob('*.json'):
            self.assertEqual(ledger['fixtureSha256'][path.name], hashlib.sha256(SALES.policy_fixture_bytes(path.read_bytes())).hexdigest())

    def test_feature_and_generator_roundtrip_and_partial_subcase_inverse(self):
        before = subprocess.check_output(['git', 'show', READ.BASE+':verification/cases/T17/scenario.feature'], cwd=READ.ROOT).decode()
        now = SALES.response_scope.restore_feature((HERE/'scenario.feature').read_text())
        self.assertEqual(before, READ.restore_feature(now))
        case = SALES.restore_policy_case(SALES.response_scope.restore_case(json.loads((HERE/'case.json').read_text())))
        self.assertEqual(now, READ.apply_feature(now, READ.restore_case(case), case))
        for sub in case['subcases']:
            self.assertEqual(READ.restore_case({'subcases': [sub]})['subcases'][0],
                             next(s for s in READ.restore_case(case)['subcases'] if s['id'] == sub['id']))

    def test_source_tampering_and_missing_added_proofs_fail_inverse(self):
        now = json.loads((HERE/'case.json').read_text())
        for name in ('eligibility-response-3', 'read-quantity-unknown-candidate-set', 'read-quantity-allocation-range'):
            wrong = copy.deepcopy(now)
            sub = next(s for s in wrong['subcases'] if s['id'] == 'eligibility-unknown')
            row = next(a for a in sub['assertions'] if a['id'] == name)
            row['source']['pointer'] = '/response/data/missing'
            with self.assertRaises(ValueError):
                READ.restore_case(wrong)
        wrong = copy.deepcopy(now)
        sub = next(s for s in wrong['subcases'] if s['id'] == 'eligibility-unknown')
        sub['assertions'] = [a for a in sub['assertions'] if a['id'] != 'read-quantity-unknown-candidate-set']
        with self.assertRaises(ValueError):
            READ.restore_case(wrong)

    def test_unknown_domain_and_no_prior_prerequisite_never_use_a_permission_constant(self):
        current = SALES.restore_policy_case(SALES.response_scope.restore_case(json.loads((HERE/'case.json').read_text())))
        for sub in current['subcases']:
            if sub['id'].startswith('eligibility-'):
                expected = [READ.alias('B'), READ.alias('U')] if sub['id'] == 'eligibility-unknown' else [READ.alias('U')]
                self.assertEqual(expected, next(a for a in sub['assertions'] if a['id'] == 'read-quantity-unknown-candidate-set')['expected'])
            if sub['id'] == 'claim-nopriordispatch':
                old = next(a for a in sub['assertions'] if a['id'] == 'no-prior-dispatch-24')
                self.assertEqual('DENIED', old['expected'])
                self.assertEqual('/response/data/priorDispatchCheck/status', old['source']['pointer'])
                own = [a for a in sub['assertions'] if a['id'].startswith(READ.PREFIX)]
                self.assertFalse(any('saleExecutionAuthorized' in json.dumps(a) or 'normalSaleAuthorization' in json.dumps(a) for a in own))


if __name__ == '__main__':
    unittest.main()
