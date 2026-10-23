"""Synthetic goal completion must preserve every original oracle and prior layer."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('public',HERE/'author_public_contracts.py')
PUBLIC=importlib.util.module_from_spec(spec);spec.loader.exec_module(PUBLIC)
NEW=PUBLIC.goal

def original(path):return subprocess.check_output(['git','show',NEW.BASE+':'+str(path.relative_to(HERE.parents[2]))],cwd=HERE.parents[2])

class DeliveryGoalInputsTest(unittest.TestCase):
    def test_roundtrip_and_prior_bytes(self):
        before=json.loads(original(HERE/'case.json'));old={p.stem:json.loads(original(p)) for p in (HERE/'fixtures').glob('*.json')}
        case,fixtures=NEW.apply(copy.deepcopy(before),copy.deepcopy(old),PUBLIC)
        self.assertEqual(PUBLIC.sales_inputs.apply(case),PUBLIC.recall_inputs.restore_case(json.loads((HERE/'case.json').read_text())));self.assertEqual(before,NEW.restore_case(case))
        for sid,f in fixtures.items():
            self.assertEqual(f,PUBLIC.recall_inputs.restore_fixture(json.loads((HERE/'fixtures'/f'{sid}.json').read_text())));self.assertEqual(old[sid],NEW.restore_fixture(f))
            for field in ('clock','evidence','responsibilities'):self.assertEqual(old[sid][field],f[field])
            for field in ('segments','conditions','sourceProfiles','sourceProvenance'):self.assertEqual(old[sid]['baseline'][field],f['baseline'][field])
        from test_public_contracts import assertion_blocks
        self.assertEqual(assertion_blocks(original(HERE/'case.json')),assertion_blocks((HERE/'case.json').read_bytes()))
        self.assertEqual(257,sum(len(s['assertions']) for s in case['subcases']))
        for name in ('public-contract-delta.json','first-failure-input-delta.json','completion-input-delta.json','delivery-input-delta.json'):
            self.assertEqual(original(HERE/name),(HERE/name).read_bytes())
        self.assertEqual(original(HERE/'scenario.feature').decode(),NEW.restore_feature((HERE/'scenario.feature').read_text()))

    def test_exact_binding_original_goal_references_and_minimum_grant(self):
        for s in json.loads((HERE/'case.json').read_text())['subcases']:
            f=json.loads((HERE/'fixtures'/f'{s["id"]}.json').read_text());old=json.loads(original(HERE/'fixtures'/f'{s["id"]}.json'))
            if NEW.META not in f['baseline']['policy']:self.assertEqual(old,f);continue
            a={a['id']:a for a in s['actions']};goal=a['complete-order-delivery-goal']['request']['slots']['goal']
            for key in NEW.EXISTING:self.assertEqual(PUBLIC.ref('order-delivery-work','data/currentGoal/slots/'+key),goal[key])
            self.assertEqual(f['clock']['timezone'],goal['timezone']);self.assertTrue(goal['periodStartInclusive']);self.assertTrue(goal['periodEndInclusive'])
            self.assertEqual(f['clock']['asOf'],goal['periodStart']);self.assertEqual(a['order']['request']['slots']['dueAt'],goal['periodEnd'])
            definition,proof=NEW.binding(old)
            self.assertEqual(definition,f['aliases'][NEW.DEFINITION_ALIAS]['content'])
            self.assertEqual(proof['evaluatorVersion'],goal['evaluatorVersion']);self.assertEqual([{'id':proof['template']['name']}],goal['conditions'])
            self.assertEqual('fixture-v1',goal['evidencePolicyVersion']);self.assertNotEqual('evaluator-v1',goal['evaluatorVersion'])
            self.assertEqual(PUBLIC.ref('completed-order-delivery-work','data/revision'),a['activate-order-delivery-work']['request']['expectedRevision'])
            for actor,before in old['actors'].items():
                now=copy.deepcopy(f['actors'][actor])
                if actor=='t17Source':now['roleCapabilities'].remove('reviseGoal');now['grant']['actions'].remove('reviseGoal')
                self.assertEqual(before,now)
            self.assertFalse(goal['defaultContext']['operatingApprovalClaimed']);self.assertFalse(goal['defaultContext']['originalAuthorityClaimed'])

    def test_strict_mutation_rejected(self):
        for key,value in [('evaluatorVersion','evaluator-v1'),('conditions',[{'id':'unknown'}]),('periodEnd','2020-01-01T00:00:00Z')]:
            case=json.loads((HERE/'case.json').read_text());sub=next(s for s in case['subcases'] if s['id']=='normal-consumed-delivery');a=next(a for a in sub['actions'] if a['id']=='complete-order-delivery-goal');a['request']['slots']['goal'][key]=value
            with self.assertRaises(ValueError):NEW.restore_case(case)
        feature=(HERE/'scenario.feature').read_text()
        with self.assertRaises(ValueError):NEW.restore_feature(feature.replace('complete-order-delivery-goal','changed-goal',1))
        f=json.loads((HERE/'fixtures/normal-consumed-delivery.json').read_text());f['aliases'][NEW.DEFINITION_ALIAS]['content']['evaluatorVersion']='invented'
        with self.assertRaises(ValueError):NEW.restore_fixture(f)

if __name__=='__main__':unittest.main()
