import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('public',HERE/'author_public_contracts.py')
PUBLIC=importlib.util.module_from_spec(spec);spec.loader.exec_module(PUBLIC)
NEW=PUBLIC.completion

def original(path):return subprocess.check_output(['git','show',NEW.BASE+':'+str(path.relative_to(ROOT))],cwd=ROOT)
def blocks(raw):
    text=raw.decode();start=0;out=[];decoder=json.JSONDecoder()
    while True:
        start=text.find('"assertions": [',start)
        if start<0:return out
        start=text.index('[',start);_,size=decoder.raw_decode(text[start:]);out.append(text[start:start+size]);start+=size

class CompletionInputsTest(unittest.TestCase):
    def test_strict_roundtrip_composition_and_original_oracle_bytes(self):
        before=json.loads(original(HERE/'case.json'));fixtures={p.stem:json.loads(original(p)) for p in (HERE/'fixtures').glob('*.json')}
        generated,changed=NEW.apply(copy.deepcopy(before),copy.deepcopy(fixtures),PUBLIC)
        current=json.loads((HERE/'case.json').read_text())
        self.assertEqual(generated,current);self.assertEqual(before,NEW.restore_case(current))
        for sid,f in changed.items():
            self.assertEqual(f,json.loads((HERE/'fixtures'/f'{sid}.json').read_text()))
            self.assertEqual(fixtures[sid],NEW.restore_fixture(f))
            for key in ['clock','evidence','responsibilities','aliases']:self.assertEqual(fixtures[sid][key],f[key])
            for key in ['conditions','segments','sourceProvenance']:self.assertEqual(fixtures[sid]['baseline'][key],f['baseline'][key])
        self.assertEqual(blocks(original(HERE/'case.json')),blocks((HERE/'case.json').read_bytes()))
        self.assertEqual(original(HERE/'public-contract-delta.json'),(HERE/'public-contract-delta.json').read_bytes())
        self.assertEqual(original(HERE/'first-failure-input-delta.json'),(HERE/'first-failure-input-delta.json').read_bytes())
        self.assertEqual(original(HERE/'scenario.feature').decode(),NEW.restore_feature((HERE/'scenario.feature').read_text()))
        self.assertEqual(257,sum(len(s['assertions']) for s in current['subcases']))

    def test_only_missing_due_dates_and_minimal_category_capabilities_added(self):
        old=json.loads(original(HERE/'case.json'));new=json.loads((HERE/'case.json').read_text());due=[];grants=[]
        for prior,sub in zip(old['subcases'],new['subcases']):
            actions={a['id']:a for a in sub['actions']};f=json.loads((HERE/'fixtures'/f'{sub["id"]}.json').read_text());of=json.loads(original(HERE/'fixtures'/f'{sub["id"]}.json'))
            for a in prior['actions']:
                self.assertEqual(a.get('actorRef'),actions[a['id']].get('actorRef'))
                old_slots=a.get('request',{}).get('slots',{});new_slots=actions[a['id']].get('request',{}).get('slots',{})
                for field in ['quantity','unit','action','startQuantity','segmentId','customerId','validFrom','validUntil','dueAt','packageTerms','qualityTerms','reason']:
                    if field in old_slots:self.assertEqual(old_slots[field],new_slots[field],(sub['id'],a['id'],field))
                if a.get('capabilityId')!='createSalesOrder':continue
                now=actions[a['id']]['request']['slots']
                if 'dueAt' in a['request']['slots']:self.assertEqual(a['request']['slots']['dueAt'],now['dueAt'])
                else:
                    due.append((sub['id'],a['id']));self.assertEqual('2026-10-07T10:00:00Z',now['dueAt'])
                    self.assertGreater(NEW.instant(now['dueAt']),NEW.instant(f['clock']['asOf']))
                    for c in f['baseline']['conditions']:self.assertLess(NEW.instant(now['dueAt']),NEW.instant(c['validUntil']))
            for actor,old_actor in of['actors'].items():
                current=copy.deepcopy(f['actors'][actor]);extra=set(current['roleCapabilities'])-set(old_actor['roleCapabilities'])
                if extra:
                    self.assertEqual({'decideRecallRestriction'} if actor=='admin' else {'decideQuality'},extra)
                    self.assertIn(actor,['admin','qc']);grants.append((sub['id'],actor))
                    current['roleCapabilities']=[c for c in current['roleCapabilities'] if c not in extra]
                    current['grant']['actions']=[c for c in current['grant']['actions'] if c not in extra]
                self.assertEqual(old_actor,current)
            for d in f['baseline']['policy'].get(NEW.META,{}).get('decisions',[]):
                self.assertFalse(d['operatingApprovalClaimed']);self.assertFalse(d['originalAuthorityClaimed'])
        self.assertEqual(7,len(due));self.assertEqual(8,len(grants))

    def test_release_and_replacement_sources_keep_original_evidence_and_exact_ids(self):
        sub=next(s for s in json.loads((HERE/'case.json').read_text())['subcases'] if s['id']=='replacement-no-revival');actions={a['id']:a for a in sub['actions']}
        old=next(s for s in json.loads(original(HERE/'case.json'))['subcases'] if s['id']==sub['id']);prior={a['id']:a for a in old['actions']}
        f=json.loads((HERE/'fixtures/replacement-no-revival.json').read_text())
        for action,prefix in [('replace','replace-source'),('release-hold','release-hold-source')]:
            source=actions[prefix+'-document']['request']['slots']['contentBase64']['$encode']['source']
            self.assertEqual(prior[action]['request'],source['originalRequest'])
            self.assertEqual(source,actions[prefix+'-event']['request']['slots']['payload']['$encode']['source'])
            self.assertFalse(source['committedEffectClaimed']);self.assertTrue(source['proposalOnly'])
            alias=prior[action]['request']['slots']['evidenceId']['$alias']
            self.assertEqual(next(e for e in f['evidence'] if e['alias']==alias),source['originalEvidenceDescriptor'])
            self.assertEqual({'$alias':'A'},source['oldSegmentId']);self.assertEqual({'$alias':'B'},source['newSegmentId'])
            self.assertEqual(PUBLIC.ref('order','workId'),source['workId']);self.assertEqual(PUBLIC.ref('order','line/id'),source['salesLineId'])
            self.assertEqual('20',source['quantity']);self.assertEqual('BOX',source['unit'])
        self.assertEqual('replacement-proof',actions['replace-source-document']['request']['slots']['contentBase64']['$encode']['source']['witnessKind'])
        self.assertLessEqual(len(actions['release-hold']['request']['slots']['reason']),240)

    def test_strict_inverse_rejects_silent_scope_or_input_drift(self):
        f=json.loads((HERE/'fixtures/replacement-no-revival.json').read_text());f['actors']['qc']['grant']['actions'].append('dispatchQuantity')
        with self.assertRaises(ValueError):NEW.restore_fixture(f)
        c=json.loads((HERE/'case.json').read_text());s=next(s for s in c['subcases'] if s['id']=='replacement-no-revival');next(a for a in s['actions'] if a['id']=='order')['request']['slots']['dueAt']='2027-01-01T00:00:00Z'
        with self.assertRaises(ValueError):NEW.restore_case(c)

if __name__=='__main__':unittest.main()
