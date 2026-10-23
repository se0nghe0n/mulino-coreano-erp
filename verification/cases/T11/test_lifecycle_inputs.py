import copy,json,sys,unittest
from pathlib import Path
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import author_lifecycle_inputs as layer

class LifecycleInputs(unittest.TestCase):
 def setUp(self):self.old=layer.baseline();self.new=json.loads((HERE/'case.json').read_text());self.subs={s['id']:s for s in self.new['subcases']}
 def test_exact_ids_business_fixtures_and_strict_inverse(self):
  self.assertEqual(layer.audit_changes(layer.authored(self.old)[1]),json.loads((HERE/'lifecycle-input-ledger.json').read_text())['changes'])
  self.assertEqual(self.old,layer.restore(self.new));self.assertEqual(self.new,layer.apply(self.old));self.assertEqual(self.new,layer.apply(self.new))
  self.assertEqual(110,sum(len(s['assertions']) for s in self.new['subcases']))
  self.assertEqual([(s['id'],[a['id'] for a in s['assertions']]) for s in self.old['subcases']],[(s['id'],[a['id'] for a in s['assertions']]) for s in self.new['subcases']])
  for p in (HERE/'fixtures').glob('*.json'):self.assertEqual(layer.original(str(p.relative_to(HERE))),p.read_bytes())
  self.assertEqual(layer.feature(),(HERE/'scenario.feature').read_text())
  mutated=copy.deepcopy(self.new);mutated['subcases'][0]['assertions'][0]['expected']='APPLIED'
  with self.assertRaises(ValueError):layer.restore(mutated)
 def test_every_changed_assertion_has_only_the_authoritative_semantic_delta(self):
  allowed={('unsupported-evaluator','activate-outcome'),('unsupported-evaluator','activate-denial-audit'),('wait-timeout','resume-denied'),('cancel-after-shipment','recipient-authenticated-acceptance'),('shared-distinct60','shared-scope-partition')}
  actual=set()
  for old,new in zip(self.old['subcases'],self.new['subcases']):
   for before,after in zip(old['assertions'],new['assertions']):
    if before!=after:actual.add((old['id'],before['id']))
  self.assertEqual(allowed,actual)
 def test_shortfall_uses_actual_assessment_and_explicit_recipient_acceptance(self):
  for sid in layer.PARENTS:
   a={a['id']:a for a in self.subs[sid]['actions']};transfer=a['deficit-transfer']['request'];slots=transfer['slots']
   self.assertEqual(layer.ref('parent-assessment-evaluate','/response/effects/responsibility/assignments/0/ID'),slots['assignmentId'])
   self.assertEqual('10',slots['quantity']);self.assertEqual(slots['nextCheckAt'],slots['expiresAt']);self.assertNotIn('acceptanceEvidenceId',slots)
   self.assertEqual('acceptHandover',a['deficit-accept']['capabilityId'])
   self.assertFalse(any(x.get('capabilityId') in ('openObligation','createObligation') for x in a.values()))
 def test_raw_source_and_explicit_new_goal_never_forge_a_canonical_occurrence(self):
  a=next(a for a in self.subs['closed-followup']['actions'] if a['id']=='followup');slots=a['request']['slots']
  self.assertEqual(layer.ref('temperature','/response/id'),slots['sourceEventId']);self.assertNotIn('sourceOccurrenceId',slots)
  self.assertEqual('STATE_AT',slots['draft']['goal']['quantityMode']);self.assertEqual('100',slots['draft']['goal']['targetQuantity'])
  self.assertEqual('TEMPERATURE_REVIEW',slots['draft']['kind'])
 def test_shared_physical_range_preserves_exact_nonoverlapping_thirties(self):
  a=next(a for a in self.subs['shared-distinct60']['assertions'] if a['id']=='shared-scope-partition')
  self.assertEqual(['workId','quantity','physicalRange'],a['source']['field'])
  for row,start,end in zip(a['expected'],('0','30'),('30','60')):
   self.assertEqual('30',row[1]);self.assertEqual(layer.alias('Q'),row[2]['physicalScopeId']);self.assertEqual(layer.ref('receive60-canonical','/response/id'),row[2]['occurrenceId']);self.assertEqual((start,end,'BOX'),(row[2]['startQuantity'],row[2]['endQuantity'],row[2]['unit']))
 def test_changed_commands_are_public_and_within_original_actor_actions(self):
  slots=json.loads((layer.ROOT/'contracts/capability-slots.json').read_text())['capabilities']
  for sub in self.new['subcases']:
   fixture=json.loads((layer.ROOT/sub['fixtureRef']).read_text())
   for a in sub['actions']:
    if a.get('kind')!='invoke':continue
    self.assertLessEqual(set(a['request']['slots']),set(slots[a['capabilityId']]['slots']),a['id'])
    actor=fixture['actors'][a['actorRef']];self.assertIn(a['capabilityId'],actor['roleCapabilities']);self.assertIn(a['capabilityId'],actor['grant']['actions'])

if __name__=='__main__':unittest.main()
