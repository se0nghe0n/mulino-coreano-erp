"""공개 인도 준비의 최소 권한과 원래 257개 oracle을 검증한다."""
import copy
import importlib.util
import json
import subprocess
import unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('public',HERE/'author_public_contracts.py')
PUBLIC=importlib.util.module_from_spec(spec);spec.loader.exec_module(PUBLIC)
NEW=PUBLIC.delivery

def original(path):return subprocess.check_output(['git','show',NEW.BASE+':'+str(path.relative_to(HERE.parents[2]))],cwd=HERE.parents[2])

class DeliveryInputsTest(unittest.TestCase):
    def test_exact_reproduction_inverse_and_prior_ledgers(self):
        before=json.loads(original(HERE/'case.json'));old={p.stem:json.loads(original(p)) for p in (HERE/'fixtures').glob('*.json')}
        case,fixtures=NEW.apply(copy.deepcopy(before),copy.deepcopy(old),PUBLIC)
        self.assertEqual(case,json.loads((HERE/'case.json').read_text()));self.assertEqual(before,NEW.restore_case(case))
        for sid,f in fixtures.items():
            self.assertEqual(f,json.loads((HERE/'fixtures'/f'{sid}.json').read_text()));self.assertEqual(old[sid],NEW.restore_fixture(f))
        self.assertEqual([s['assertions'] for s in before['subcases']],[s['assertions'] for s in case['subcases']])
        from test_public_contracts import assertion_blocks
        self.assertEqual(assertion_blocks(original(HERE/'case.json')),assertion_blocks((HERE/'case.json').read_bytes()))
        for name in ('public-contract-delta.json','first-failure-input-delta.json','completion-input-delta.json'):
            self.assertEqual(original(HERE/name),(HERE/name).read_bytes())
        self.assertEqual(original(HERE/'scenario.feature').decode(),NEW.restore_feature((HERE/'scenario.feature').read_text()))

    def test_minimum_preparation_and_typed_source_chain(self):
        count=0
        for s in json.loads((HERE/'case.json').read_text())['subcases']:
            f=json.loads((HERE/'fixtures'/f'{s["id"]}.json').read_text());old=json.loads(original(HERE/'fixtures'/f'{s["id"]}.json'))
            actions={a['id']:a for a in s['actions']}
            if NEW.META not in f['baseline']['policy']:
                self.assertEqual(old,f);continue
            count+=1;restored=copy.deepcopy(f['actors']['t17Source'])
            for field in ('roleCapabilities',):restored[field].remove('activateWork')
            restored['grant']['actions'].remove('activateWork');self.assertEqual(old['actors']['t17Source'],restored)
            for actor in old['actors']:
                if actor!='t17Source':self.assertEqual(old['actors'][actor],f['actors'][actor])
            self.assertEqual(PUBLIC.ref('order-delivery-work','data/revision'),actions['activate-order-delivery-work']['request']['expectedRevision'])
            self.assertEqual(PUBLIC.ref('order-delivery-work','data/ID'),actions['activate-order-delivery-work']['request']['slots']['workId'])
            name='claim' if s['id'].startswith('claim-') else 'delivery'
            content=actions[name+'-source-document']['request']['slots']['contentBase64']['$encode']['source']
            self.assertFalse(content['operatingApprovalClaimed']);self.assertFalse(content['originalAuthorityClaimed'])
            evidence=content['originalRequest']['sourceEvidenceId']['$alias']
            self.assertEqual(next(e for e in old['evidence'] if e['alias']==evidence),content['originalEvidenceDescriptor'])
            if name=='claim':self.assertNotIn('claim-source-canonical',actions)
            else:
                self.assertEqual(PUBLIC.ref('delivery-source-observation','data/ID'),actions['delivery-source-match']['request']['slots']['physicalScopeId'])
        self.assertEqual(7,count)

    def test_strict_drift_rejection(self):
        f=json.loads((HERE/'fixtures/normal-consumed-delivery.json').read_text());f['actors']['t17Source']['grant']['scope']['workAliases']=['arbitrary']
        with self.assertRaises(ValueError):NEW.restore_fixture(f)
        c=json.loads((HERE/'case.json').read_text());s=next(s for s in c['subcases'] if s['id']=='normal-consumed-delivery')
        next(a for a in s['actions'] if a['id']=='activate-order-delivery-work')['actorRef']='sales'
        with self.assertRaises(ValueError):NEW.restore_case(c)
        c=json.loads((HERE/'case.json').read_text());s=next(s for s in c['subcases'] if s['id']=='normal-consumed-delivery')
        s['actions']=[a for a in s['actions'] if a['id']!='activate-order-delivery-work']
        with self.assertRaises(ValueError):NEW.restore_case(c)

if __name__=='__main__':unittest.main()
