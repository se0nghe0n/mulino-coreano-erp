"""열린 후속 의무와 유효한 과거 assignment를 독립 범위로 대조한다."""
import copy
import hashlib
import json
import subprocess
import unittest
from pathlib import Path
import author_response_scope as SCOPE
import author_sales_contract_inputs as SALES

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]


class ResponseScopeTest(unittest.TestCase):
    def setUp(self):
        self.case=SALES.restore_policy_case(SALES.read_quantity.restore_case(json.loads((HERE/'case.json').read_text())))
        self.before=json.loads(subprocess.check_output(['git','show','fb58d866:verification/cases/T17/case.json'],cwd=ROOT))
        self.sub=next(s for s in self.case['subcases'] if s['id']==SCOPE.SID)

    def test_original_301_objects_restore_and_only_one_fixed_oracle_source_changes(self):
        self.assertEqual(self.before,SCOPE.restore_case(self.case))
        self.assertEqual(self.case,SCOPE.apply(self.before))
        self.assertEqual(self.case,SCOPE.apply(self.case))
        self.assertEqual(309,sum(len(s['assertions']) for s in self.case['subcases']))
        for old,new in zip(self.before['subcases'],self.case['subcases']):
            prior={a['id']:a for a in old['assertions']}
            self.assertEqual(list(prior),[a['id'] for a in new['assertions'] if a['id'] in prior])
            for a in new['assertions']:
                if a['id'] not in prior:continue
                b=prior[a['id']]
                for key in ('op','expected','oracleRef','requirementRefs','scope'):
                    self.assertEqual(b[key],a[key],a['id']+':'+key)
                if a['id']!='fact-not-permission-11':self.assertEqual(b,a)
        for p in (HERE/'fixtures').glob('*.json'):
            before=subprocess.check_output(['git','show','fb58d866:'+str(p.relative_to(ROOT))],cwd=ROOT)
            self.assertEqual(before,SALES.policy_fixture_bytes(p.read_bytes()))

    def test_scope_keeps_resolved_and_quality_assignments_and_actual_work_binding(self):
        action=next(a for a in self.sub['actions'] if a['id']==SCOPE.ACTION)
        scope=action['observation']['scope']
        self.assertEqual([{'$result':{'actionId':'order-delivery-work','pointer':'/response/data/ID'}},{'$alias':'t17FormalRecallWork'}],scope['workIds'])
        self.assertEqual(['obligations'],action['observation']['sources'])
        assertions={a['id']:a for a in self.sub['assertions']}
        self.assertEqual({'current':True,'status':'OPEN'},assertions['fact-not-permission-11']['source']['where'])
        self.assertEqual(['DELIVERY_RESTRICTION_RESPONSE','RECALL_INVESTIGATION'],assertions['fact-not-permission-11']['expected'])
        for name,expected in [('whole-item-row-count',6),('bounded-row-count',3),('resolved-regulatory-retained',2),('resolved-intake-retained',1),('open-quality-retained',1)]:
            self.assertEqual(expected,assertions['response-scope-'+name]['expected'])
        self.assertEqual('after-delivery-db',assertions['response-scope-whole-item-row-count']['source']['actionId'])
        self.assertEqual(3,len(assertions['response-scope-all-assignments']['expected']))

    def test_incomplete_or_tampered_scope_and_checks_fail_exact_inverse(self):
        for kind,identity in [('actions',SCOPE.ACTION),('assertions','response-scope-whole-item-row-count'),('assertions','fact-not-permission-11')]:
            broken=copy.deepcopy(self.case);sub=next(s for s in broken['subcases'] if s['id']==SCOPE.SID)
            sub[kind]=[r for r in sub[kind] if r['id']!=identity]
            with self.subTest(missing=identity),self.assertRaises((ValueError,StopIteration)):SCOPE.restore_case(broken)
        for field in ('foreign-work','open-is-current','hide-history'):
            broken=copy.deepcopy(self.case);sub=next(s for s in broken['subcases'] if s['id']==SCOPE.SID)
            if field=='foreign-work':next(a for a in sub['actions'] if a['id']==SCOPE.ACTION)['observation']['scope']['workIds'][1]={'$alias':'foreign'}
            elif field=='open-is-current':next(a for a in sub['assertions'] if a['id']=='fact-not-permission-11')['source']['where'].pop('status')
            else:next(a for a in sub['assertions'] if a['id']=='response-scope-whole-item-row-count')['expected']=2
            with self.subTest(tamper=field),self.assertRaises(ValueError):SCOPE.restore_case(broken)
        feature=SALES.read_quantity.restore_feature((HERE/'scenario.feature').read_text())
        self.assertEqual(feature,SCOPE.apply_feature(SCOPE.restore_feature(feature)))
        baseline=subprocess.check_output(['git','show','fb58d866:verification/cases/T17/scenario.feature'],cwd=ROOT).decode()
        self.assertEqual(baseline,SCOPE.restore_feature(feature))


if __name__=='__main__':unittest.main()
