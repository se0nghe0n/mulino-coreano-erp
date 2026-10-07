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
if __name__=='__main__':unittest.main()
