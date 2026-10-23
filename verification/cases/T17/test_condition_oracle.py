import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
spec = importlib.util.spec_from_file_location('condition_oracle', HERE/'author_condition_oracle.py')
ORACLE = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ORACLE)
recall_spec = importlib.util.spec_from_file_location('recall_inputs', HERE/'author_recall_inputs.py')
RECALL = importlib.util.module_from_spec(recall_spec)
recall_spec.loader.exec_module(RECALL)


def original(path):
    return subprocess.check_output(['git', 'show', ORACLE.BASE + ':' + str(path.relative_to(ROOT))], cwd=ROOT)


class ConditionOracleTest(unittest.TestCase):
    def test_reversible_fixedpoint_all_257_original_ids_and_every_business_tuple(self):
        before = json.loads(original(HERE/'case.json'))
        actual = RECALL.restore_case(json.loads((HERE/'case.json').read_text()))
        self.assertEqual(actual, ORACLE.apply(before))
        self.assertEqual(actual, ORACLE.apply(actual))
        self.assertEqual(before, ORACLE.restore_case(actual))
        original_count = 0
        business_count = 0
        for old, new in zip(before['subcases'], actual['subcases']):
            ids = [a['id'] for a in old['assertions']]
            original_count += len(ids)
            self.assertEqual(ids, [a['id'] for a in new['assertions'] if a['id'] in ids])
            for a in old['assertions']:
                b = next(r for r in new['assertions'] if r['id'] == a['id'])
                self.assertEqual(a['op'], b['op'])
                self.assertEqual(a['oracleRef'], b['oracleRef'])
                if a['id'] not in ('eligibility-response-1', 'eligibility-response-2'):
                    self.assertEqual(a, b)
            if not old['id'].startswith('eligibility-'):
                continue
            expected = old['assertions'][0]['expected']
            now = new['assertions'][0]['expected']
            alt = [a for a in new['assertions'] if a['id'] == 'condition-source-alt-business-status']
            if alt:
                now = now + alt[0]['expected']
            self.assertEqual([r[:3] for r in expected], [r[:3] for r in now])
            self.assertEqual({ORACLE.EVALUATOR}, {r[3] for r in now})
            self.assertEqual({'QC','REGULATORY','LABEL','DISPOSITION','CUSTOMER_CONTRACT'}, {r[1] for r in now})
            business_count += len(now)
        self.assertEqual(257, original_count)
        self.assertEqual(105, business_count)

    def test_fixture_grant_clock_bytes_and_feature_inverse(self):
        ledger = json.loads(ORACLE.LEDGER.read_text())
        for p in sorted((HERE/'fixtures').glob('*.json')):
            restored = RECALL.bytes_of(RECALL.restore_fixture(json.loads(p.read_bytes())))
            self.assertEqual(original(p), restored)
            self.assertEqual(ledger['fixtureSha256'][p.name], hashlib.sha256(restored).hexdigest())
        feature = RECALL.restore_feature((HERE/'scenario.feature').read_text())
        self.assertEqual(original(HERE/'scenario.feature').decode(), ORACLE.restore_feature(feature))
        self.assertEqual(feature, ORACLE.apply_feature(feature))
        for line in feature.splitlines():
            if line.startswith('    그러면 "condition-source-'):
                self.assertNotIn('assertion으로 "eligibility-response"', line)

    def test_alt_and_permission_hold_remain_separate_and_no_live_status_references(self):
        case = json.loads((HERE/'case.json').read_text())
        for s in case['subcases']:
            if not s['id'].startswith('eligibility-'):
                continue
            actions = {a['id']: a for a in s['actions']}
            self.assertEqual({'$alias':'P'}, actions['after-reserve-db']['observation']['scope']['itemId'])
            a = next(a for a in s['assertions'] if a['id']=='eligibility-response-2')
            self.assertEqual(ORACLE.SOURCE_FIELDS, a['source']['field'])
            self.assertEqual('/data/rawRows/sellConditionSourceRefs', a['baseline']['pointer'])
            qc = next(a for a in s['assertions'] if a['id']=='condition-source-p-qc-permission-and-hold')
            self.assertIn([{'$alias':'EARLY'},'QC','ALLOWED','DENIED'], qc['expected'])
            if s['id']=='eligibility-packaging-unapproved':
                self.assertEqual({'$alias':'ALT'}, actions['eligibility-alt']['request']['scope']['itemId'])
                self.assertEqual({'$alias':'ALT'}, actions['eligibility-alt-db']['observation']['scope']['itemId'])
                self.assertEqual({'$alias':'OTHERPACK'},actions['reserve']['request']['slots']['segmentId'])
                self.assertEqual({'$alias':'P'},actions['order']['request']['slots']['itemId'])
                self.assertEqual(5,len(next(a for a in s['assertions'] if a['id']=='condition-source-alt-business-status')['expected']))
            def refs(value):
                if isinstance(value,dict):
                    if '$result' in value:
                        pointer=value['$result']['pointer']
                        self.assertTrue(pointer.endswith(('/documentId','/canonicalOccurrenceId','/response/id')), pointer)
                    for v in value.values():refs(v)
                elif isinstance(value,list):
                    for v in value:refs(v)
            for a in s['assertions']:
                if a['id'].startswith('condition-source-'):refs(a['expected'])

    def test_missing_or_tampered_added_status_source_and_receipt_deltas_fail_inverse(self):
        actual=json.loads((HERE/'case.json').read_text())
        for name in ('condition-source-p-installed-identities','condition-source-alt-business-status','condition-source-receipt-original-pointers'):
            c=copy.deepcopy(actual)
            s=next(s for s in c['subcases'] if s['id']=='eligibility-packaging-unapproved')
            s['assertions']=[a for a in s['assertions'] if a['id']!=name]
            with self.assertRaises(ValueError):ORACLE.restore_case(c)
        c=copy.deepcopy(actual)
        s=next(s for s in c['subcases'] if s['id']=='eligibility-conflict')
        s['assertions'][0]['expected'][6][2]='ALLOWED'
        with self.assertRaises(ValueError):ORACLE.restore_case(c)


if __name__=='__main__':unittest.main()
