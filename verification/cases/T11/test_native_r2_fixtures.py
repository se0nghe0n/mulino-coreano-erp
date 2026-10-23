import copy,json,sys,unittest,subprocess
from pathlib import Path
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import author_native_r2_fixtures as layer

class NativeR2Fixtures(unittest.TestCase):
 def test_explicit_completion_is_strictly_reversible_and_original_bytes_are_unchanged(self):
  ledger=json.loads((HERE/'native-r2-fixture-ledger.json').read_text())
  self.assertFalse(ledger['historicalRuntimePassClaimed'])
  for sid in layer.SELECTED:
   old,new=layer.completion(sid);actual=json.loads((HERE/'fixtures/native-r2'/f'{sid}.json').read_text())
   self.assertEqual(new,actual);self.assertEqual(old,layer.restore(sid,actual));self.assertEqual(old['clock'],new['clock'])
   for actor,value in old['actors'].items():self.assertEqual(value,new['actors'][actor])
   original=HERE/'fixtures'/f'{sid}.json';self.assertEqual(subprocess.check_output(['git','show','fb58d866:'+str(original.relative_to(layer.ROOT))],cwd=layer.ROOT),original.read_bytes())
   self.assertEqual(original.read_bytes(),(json.dumps(layer.restore(sid,actual),ensure_ascii=False,indent=2)+'\n').encode())
   entry=next(e for e in ledger['entries'] if e['subcaseId']==sid);self.assertEqual(old,entry['original']);self.assertEqual(new,entry['completed'])
   altered=copy.deepcopy(new);altered['clock']['asOf']='2026-10-08T00:00:00Z'
   with self.assertRaises(ValueError):layer.restore(sid,altered)
 def test_all_original_oracle_fields_and_business_meaning_are_identical(self):
  old=json.loads(subprocess.check_output(['git','show','fb58d866:verification/cases/T11/case.json'],cwd=layer.ROOT))
  new=json.loads((HERE/'case.json').read_text())
  self.assertEqual([(s['id'],s['assertions']) for s in old['subcases']],[(s['id'],s['assertions']) for s in new['subcases']])
  self.assertEqual(110,sum(len(s['assertions']) for s in new['subcases']))
 def test_history_is_declared_synthetic_without_replacing_original_evaluator_or_result(self):
  old,new=layer.completion('closed-followup');b=new['baseline'];x=b['historicalGoalCompletion']
  self.assertEqual(old['baseline']['works'],b['works']);self.assertEqual(old['baseline']['assessments'],b['assessments'])
  self.assertFalse(x['originalGoalModeProvided']);self.assertFalse(x['originalAuthorityClaimed']);self.assertFalse(x['historicalEvaluationExecuted']);self.assertTrue(x['coordinatorSelectedSynthetic'])
  self.assertEqual('evaluator-v1',b['assessments'][0]['evaluatorVersion']);self.assertEqual('SATISFIED',b['assessments'][0]['result'])
  self.assertEqual(layer.sha(x['template']),x['templateSha256']);self.assertEqual(layer.sha(x['instantiatedPredicate']),x['predicateSha256'])
  for actor in new['actors'].values():self.assertNotIn('assessGoal',actor['grant']['actions'])
 def test_new_authority_is_separate_and_has_no_assessment_or_duty_creation_capability(self):
  old,new=layer.completion('cancel-after-shipment');x=new['baseline']['nativeR2LifecycleCompletion']
  self.assertEqual(old['baseline']['obligations'],new['baseline']['obligations']);self.assertEqual(old['baseline']['assignments'],new['baseline']['assignments'])
  self.assertEqual({'nativeR2Source'},set(new['actors'])-set(old['actors']))
  actor=new['actors'][x['actorAlias']]
  for forbidden in ('assessGoal','openObligation','createGrant','assignCapability'):self.assertNotIn(forbidden,actor['grant']['actions'])
  self.assertEqual('nativeR2Source',actor['grant']['delegatorAlias']);self.assertEqual(['P'],actor['grant']['scope']['itemAliases'])
  doc=new['aliases'][x['sourceDocumentAlias']]['fixtureContent'];self.assertEqual(old['baseline']['obligations'][0],doc['originalObligation']);self.assertEqual('DOCUMENT',x['sourceKind']);self.assertEqual('baseline-shortfall-10',doc['sourceOccurrence'])
  expiry=x['lotExpiryCompletion'];self.assertFalse(expiry['originalExpiryProvided']);self.assertTrue(expiry['coordinatorSelectedSynthetic']);self.assertFalse(expiry['originalAuthorityClaimed']);self.assertEqual('2026-10-31T23:59:59Z',new['aliases']['LOT']['expiresAt']);self.assertNotIn('expiresAt',old['aliases']['LOT'])

if __name__=='__main__':unittest.main()
