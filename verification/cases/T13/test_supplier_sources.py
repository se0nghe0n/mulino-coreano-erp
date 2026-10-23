"""원래 135 oracle/비대상 case 보존, typed 공개 입력과 strict delta를 검사한다."""
import copy
import json
import unittest
from pathlib import Path
import author_supplier_sources as a
from test_contract_refs import request_contract

class SupplierSources(unittest.TestCase):
 def test_original_case_and_every_assertion_preserved(self):
  old=a.original(a.HERE/'case.json');new=json.loads((a.HERE/'case.json').read_text())
  self.assertEqual(135,sum(len(s['assertions']) for s in new['subcases']))
  self.assertEqual(new,a.repair_case(copy.deepcopy(old)))
  self.assertEqual(old,a.restore_case(new))
  for x,y in zip(old['subcases'],new['subcases'],strict=True):
   self.assertEqual(x['assertions'],y['assertions'])
   if x['id'] not in a.SELECTED:self.assertEqual(x,y)
  self.assertEqual(a.original(a.HERE/'case.json')['subcases'][0],new['subcases'][0])
 def test_original_fixtures_descriptors_and_grants_restore(self):
  for sid in a.SELECTED:
   p=a.HERE/'fixtures'/f'{sid}.json';old=a.original(p);new=json.loads(p.read_text())
   self.assertEqual(new,a.repair_fixture(copy.deepcopy(old)))
   self.assertEqual(new,a.repair_fixture(copy.deepcopy(new)))
   self.assertEqual(old,a.restore_fixture(new))
   self.assertEqual(old['evidence'],new['evidence'])
   for actor in old['actors']:
    if actor!='source':self.assertEqual(old['actors'][actor],new['actors'][actor])
   self.assertEqual(old['actors']['source']['grant']['scope'],new['actors']['source']['grant']['scope'])
   self.assertEqual(['P'],new['actors'][a.ACTOR]['grant']['scope']['items'])
 def test_generator_does_not_use_expected_values(self):
  old=a.original(a.HERE/'case.json');changed=copy.deepcopy(old)
  for s in changed['subcases']:
   for assertion in s['assertions']:assertion['expected']='POISON'
  first=a.repair_case(old);second=a.repair_case(changed)
  self.assertEqual([s['actions'] for s in first['subcases']],[s['actions'] for s in second['subcases']])
 def test_strict_inverse_rejects_unlisted_delta(self):
  c=json.loads((a.HERE/'case.json').read_text());sub=next(s for s in c['subcases'] if s['id']=='supplier-accept');sub['assertions'][0]['expected']=999
  with self.assertRaises(AssertionError):a.restore_case(c)
 def test_public_slots(self):
  c=json.loads((a.HERE/'case.json').read_text());contracts=request_contract.Contracts(a.ROOT)
  for s in c['subcases']:
   if s['id'] not in a.SELECTED:continue
   for action in s['actions']:
    if action['kind']=='invoke':self.assertEqual([],contracts.command_problems(action,s)[0],action['id'])
 def test_three_original_scenario_sentences_unchanged(self):
  import subprocess
  path=a.HERE/'scenario.feature';self.assertEqual(subprocess.check_output(['git','show',f'{a.BASE}:{path.relative_to(a.ROOT)}'],cwd=a.ROOT),a.restore_feature(path.read_text()).encode())

if __name__=='__main__':unittest.main()
