"""Publication changes tools metadata, never synthetic authority or business oracles."""
import copy
import hashlib
import json
import subprocess
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
BASE='ce0eecc1'
ADDED=['createObligation','decideQualityDutyWaiver','decideQuantityDutyWaiver','decideRecallDutyWaiver','expireHandover','openObligation']
def old_bytes(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def old_json(path):return json.loads(old_bytes(path))
def current(path):return json.loads((ROOT/path).read_text())

def restore_metadata(cid,value):
    """Only the documented public discovery fields may differ from the baseline."""
    result=copy.deepcopy(value);before=old_json(f'verification/cases/{cid}/case.json')
    changes=0
    for sub,old in zip(result['subcases'],before['subcases']):
        if cid=='V4' and sub['id']=='exposed-write-surface':
            a=next(a for a in sub['actions'] if a['id']=='enumerate')
            original=next(a for a in old['actions'] if a['id']=='enumerate')
            actual=hashlib.sha256((ROOT/'contracts/acceptance-capabilities.json').read_bytes()).hexdigest()
            if a['control']['parameters']['allowlistSha256']!=actual:raise ValueError('Wrong inventory source hash')
            a['control']['parameters']['allowlistSha256']=original['control']['parameters']['allowlistSha256']
        for assertion,prior in zip(sub['assertions'],old['assertions']):
            if assertion==prior:continue
            expected_id={'T20':'tool-schema-registry','C3':'tools-advertised','V4':'mcp-tools-equal-allowlist'}[cid]
            if assertion['id']!=expected_id:raise ValueError('Unrecorded business oracle mutation')
            if assertion['expected']!=prior['expected']+ADDED:raise ValueError('Discovery additions must be exactly six; no shrink or unrelated name')
            assertion['expected']=prior['expected'];changes+=1
    if result!=before:raise ValueError('Unrecorded action, oracle, actor or scope mutation')
    return result,changes

class PublicationPreservation(unittest.TestCase):
    def test_original_114_entries_and_exact_six_publication(self):
        path='contracts/acceptance-capabilities.json';old=old_json(path);new=current(path)
        self.assertEqual(114,len(old['capabilities']));self.assertEqual(old['capabilities'],new['capabilities'][:114])
        self.assertEqual(ADDED,[c['id'] for c in new['capabilities'][114:]])
        self.assertEqual(120,len({c['id'] for c in new['capabilities']}))
        self.assertEqual({k:v for k,v in old.items() if k!='capabilities'},{k:v for k,v in new.items() if k!='capabilities'})

    def test_every_original_assertion_id_count_and_business_value_preserved(self):
        for cid,count in [('T20',5),('C3',30),('V4',1)]:
            value=current(f'verification/cases/{cid}/case.json');restored,changed=restore_metadata(cid,value)
            self.assertEqual(count,changed)
            old=old_json(f'verification/cases/{cid}/case.json')
            self.assertEqual(old,restored)
            self.assertEqual([(s['id'],[a['id'] for a in s['assertions']]) for s in old['subcases']],[(s['id'],[a['id'] for a in s['assertions']]) for s in value['subcases']])

    def test_all_synthetic_fixtures_grants_clocks_and_features_byte_preserved(self):
        for cid in ('T20','C3','V4'):
            for path in (ROOT/f'verification/cases/{cid}').rglob('*'):
                if not path.is_file() or '__pycache__' in path.parts:continue
                if path.name in ('case.json','observation-bindings.json') or path.suffix=='.py':continue
                self.assertEqual(old_bytes(str(path.relative_to(ROOT))),path.read_bytes(),str(path))

    def test_binding_delta_is_only_exact_case_hash(self):
        for cid in ('C3','V4'):
            path=f'verification/cases/{cid}/observation-bindings.json';old=old_json(path);new=current(path)
            self.assertEqual(hashlib.sha256((ROOT/f'verification/cases/{cid}/case.json').read_bytes()).hexdigest(),new['caseHash'])
            old['caseHash']=new['caseHash'];self.assertEqual(old,new)

    def test_negative_mutations_do_not_disappear_in_metadata_restoration(self):
        for mutation in ('removeOld','addFake','actor','quantity','outcome'):
            cid='T20';v=current(f'verification/cases/{cid}/case.json')
            sub=next(s for s in v['subcases'] if any(a['id']=='tool-schema-registry' for a in s['assertions']))
            oracle=next(a for a in sub['assertions'] if a['id']=='tool-schema-registry')
            if mutation=='removeOld':oracle['expected'].pop(0)
            elif mutation=='addFake':oracle['expected'].append('fakeRegisteredEffect')
            elif mutation=='actor':sub['actions'][0]['actorRef']='supervisor'
            elif mutation=='quantity':sub['actions'][0]['fabricatedQuantity']='10'
            else:sub['assertions'][0]['expected']='MADE_UP'
            with self.assertRaises(ValueError):restore_metadata(cid,v)
        v=current('verification/cases/V4/case.json')
        target=next(a for s in v['subcases'] if s['id']=='exposed-write-surface' for a in s['actions'] if a['id']=='enumerate')
        target['control']['parameters']['allowlistSha256']='0'*64
        with self.assertRaises(ValueError):restore_metadata('V4',v)

if __name__=='__main__':unittest.main()
