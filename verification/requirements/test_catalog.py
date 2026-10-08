"""Omission/weakening drift tests; these are not business implementation tests."""
import copy,json,pathlib,unittest
from validate_catalog import CatalogError,validate
HERE=pathlib.Path(__file__).resolve().parent
class NormativeCatalogTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):cls.catalog=json.loads((HERE/'mandatory-oracles.json').read_text())
 def oracle(self,catalog,key):return next(o for o in catalog['oracles'] if o['oracleId']==key)
 def rejected(self,change):
  altered=copy.deepcopy(self.catalog);change(altered)
  with self.assertRaises((CatalogError,KeyError)):validate(altered)
 def test_reference_is_not_run(self):
  result=validate(self.catalog)
  self.assertEqual(result['caseCount'],41);self.assertEqual(result['requirementCount'],26)
  self.assertEqual(result['runtimeCoverage'],'NOT_RUN')
 def test_drop_whole_normal_delivery_oracle(self):
  self.rejected(lambda c:c['oracles'].remove(self.oracle(c,'T17.normal-consumed-delivery')))
 def test_drop_late_delivery_quantity_observation(self):
  self.rejected(lambda c:self.oracle(c,'T17.late-restriction-actual-delivery')['expectedObservations'].pop(0))
 def test_weaken_actual_twenty_to_zero(self):
  self.rejected(lambda c:self.oracle(c,'T17.late-restriction-actual-delivery')['expectedObservations'][0]['expected'].update(value='0'))
 def test_drop_v7_auth_revoke_order(self):
  self.rejected(lambda c:c['oracles'].remove(self.oracle(c,'V7.authorization-then-revoke')))
 def test_drop_c4_no_resurrection(self):
  self.rejected(lambda c:c['oracles'].remove(self.oracle(c,'C4.resolved-debt-no-resurrection')))
 def test_remove_real_db_layer(self):
  self.rejected(lambda c:self.oracle(c,'V2.split-reserve-race')['requiredLayers'].remove('DB'))
 def test_drop_responsibility_next_check(self):
  def change(c):
   row=next(o for o in self.oracle(c,'C5.failed-link-responsibility')['expectedObservations'] if o['type']=='responsibility')
   del row['expected']['nextCheckAt']
  self.rejected(change)
 def test_dangling_cross_reference(self):
  self.rejected(lambda c:self.oracle(c,'T17.eligibility-fefo-contract')['sharedOracleRefs'].append('T17.missing'))
 def test_duplicate_oracle(self):self.rejected(lambda c:c['oracles'].append(copy.deepcopy(c['oracles'][0])))
 def test_drop_case_id(self):self.rejected(lambda c:c['requiredCaseIds'].remove('E2'))
 def test_drop_requirement(self):self.rejected(lambda c:c['requirementIds'].remove('D26'))
 def test_fake_source_hash(self):self.rejected(lambda c:c['oracles'][0]['sourceRefs'][0].update(sha256='0'*64))
 def test_reference_pass_forgery(self):self.rejected(lambda c:c['oracles'][0].update(status='PASS'))
 def observation(self,catalog,key,name):
  return next(o for o in self.oracle(catalog,key)['expectedObservations'] if o['name']==name)
 def test_drop_not_unknown_clause(self):
  self.rejected(lambda c:self.observation(c,'T07.typed-relations-and-predicates','predicate-three-valued')['expected'].pop('notUnknown'))
 def test_weaken_not_conflict_as_satisfied(self):
  self.rejected(lambda c:self.observation(c,'T07.typed-relations-and-predicates','predicate-three-valued')['expected'].update(notConflict='SATISFIED'))
 def test_drop_confirmed_negation_clause(self):
  self.rejected(lambda c:self.observation(c,'T07.typed-relations-and-predicates','predicate-three-valued')['expected'].pop('notFalse'))
 def test_drop_exists_in_result(self):
  self.rejected(lambda c:self.oracle(c,'T09.exists-in-versus-end-state')['expectedObservations'].remove(self.observation(c,'T09.exists-in-versus-end-state','exists-in-result')))
 def test_weaken_end_state_as_satisfied(self):
  self.rejected(lambda c:self.observation(c,'T09.exists-in-versus-end-state','end-state-at-result').update(expected='SATISFIED'))
 def test_drop_fully_observed_throughout(self):
  self.rejected(lambda c:c['oracles'].remove(self.oracle(c,'T09.throughout-fully-observed')))
 def test_drop_disposition_manager_negative(self):
  self.rejected(lambda c:self.oracle(c,'T05.disposition-manager-decision')['expectedObservations'].remove(self.observation(c,'T05.disposition-manager-decision','ordinary-write-confirmed-basis-effects')))
 def test_weaken_disposition_manager_positive(self):
  self.rejected(lambda c:self.observation(c,'T05.disposition-manager-decision','eligible-after-manager-confirmation')['expected'].update(value='0'))
 def test_drop_settlement_manager_negative(self):
  self.rejected(lambda c:self.oracle(c,'T19.settlement-manager-decision')['expectedObservations'].remove(self.observation(c,'T19.settlement-manager-decision','ordinary-write-confirmed-settlement-effects')))
 def test_weaken_settlement_manager_positive(self):
  self.rejected(lambda c:self.observation(c,'T19.settlement-manager-decision','authorized-confirmation-count').update(expected=0))
 def test_empty_or_falsy_lock_is_rejected_not_replaced(self):
  lock=json.loads((HERE/'normative-contract-lock.json').read_text())
  for bad in ({},[],0,'',False):
   with self.subTest(lock=bad),self.assertRaises(CatalogError):validate(self.catalog,bad)
  for key in ('oracleContracts','sourceFiles','requiredCaseIds'):
   broken=copy.deepcopy(lock);broken[key]={} if key=='oracleContracts' else []
   with self.subTest(key=key),self.assertRaises(CatalogError):validate(self.catalog,broken)
  weak=copy.deepcopy(lock);next(iter(weak['oracleContracts'].values()))['contractSha256']=''
  with self.assertRaises(CatalogError):validate(self.catalog,weak)
if __name__=='__main__':unittest.main()
