"""Preservation, strict inverse, explicit source contracts; no scenario execution."""
import base64
import copy
import hashlib
import importlib.util
import json
import re
import subprocess
import unittest
from author_sale_sources import *


class SaleSources(unittest.TestCase):
    def test_exact_original_94_oracles_actions_other_seven_and_grants(self):
        current=json.loads((HERE/'case.json').read_text())
        old=json.loads(baseline('verification/cases/T09/case.json'))
        self.assertEqual(old,restore(current))
        def blocks(text):
            return [json.JSONDecoder().raw_decode(text[m.end():])[0] for m in re.finditer(r'"assertions":\s*',text)]
        def raw(text):
            return [text[m.end():m.end()+json.JSONDecoder().raw_decode(text[m.end():])[1]] for m in re.finditer(r'"assertions":\s*',text)]
        self.assertEqual(raw(baseline('verification/cases/T09/case.json')),raw((HERE/'case.json').read_text()))
        self.assertEqual(94,sum(map(len,blocks((HERE/'case.json').read_text()))))
        for before,after in zip(old['subcases'],current['subcases']):
            original_ids={a['id'] for a in before['actions']}
            original_actions=[copy.deepcopy(a) for a in after['actions'] if a['id'] in original_ids]
            if before['id']==SELECTED[0]:
                action=next(a for a in original_actions if a['id']=='reserve60')
                self.assertEqual(ref('t09-sale-source-reserve60-current-segment','data/revision'),action['request']['expectedRevision'])
                action['request']['expectedRevision']=1
            for change in envelope_selectors():
                if change['subcase'] != before['id']: continue
                action=next(a for a in original_actions if a['id']==change['actionId'])
                target=action
                fields=change['field'].split('.')
                for field in fields[:-1]: target=target[field]
                self.assertEqual(change['after'],target[fields[-1]])
                target[fields[-1]]=change['before']
            self.assertEqual(before['actions'],original_actions)
            if before['id'] not in SELECTED:self.assertEqual(before,after)
            else:
                previous=json.loads(baseline(before['fixtureRef'])); f=json.loads((ROOT/before['fixtureRef']).read_text())
                self.assertEqual(previous,restore_fixture(f))
                for actor,v in previous['actors'].items():
                    self.assertEqual(v,f['actors'][actor])
                    marker='    '+json.dumps(actor)+': '
                    old_text=baseline(before['fixtureRef']);new_text=(ROOT/before['fixtureRef']).read_text()
                    old_start=old_text.index(marker)+len(marker);new_start=new_text.index(marker)+len(marker)
                    old_length=json.JSONDecoder().raw_decode(old_text[old_start:])[1]
                    new_length=json.JSONDecoder().raw_decode(new_text[new_start:])[1]
                    self.assertEqual(old_text[old_start:old_start+old_length],new_text[new_start:new_start+new_length])
                self.assertEqual(previous['baseline'].get('segments'),f['baseline'].get('segments'))
        self.assertEqual(baseline('verification/cases/T09/scenario.feature'),restore_feature((HERE/'scenario.feature').read_text()))

    def test_strict_mutations_fail(self):
        current=json.loads((HERE/'case.json').read_text())
        for mutate in [lambda v:v['subcases'][0]['assertions'][0].update(expected='FORGED'),
                       lambda v:v['subcases'][0]['actions'][1]['request']['slots'].update(quantity='999'),
                       lambda v:next(a for a in v['subcases'][0]['actions'] if a['id'].startswith('t09-sale-source-')).update(actorRef='warehouse')]:
            value=copy.deepcopy(current);mutate(value)
            with self.assertRaises(ValueError):restore(value)
        f=json.loads((HERE/'fixtures/exists-versus-end-throughout.json').read_text())
        for mutation in [lambda v:v['actors']['warehouse']['grant']['actions'].append('getEvidence'),
                         lambda v:v['aliases']['LOT'].update(expiresAt='2099-01-01T00:00:00Z'),
                         lambda v:v['aliases']['t09SalePolicySELL'].update(requiresLabel=True)]:
            value=copy.deepcopy(f);mutation(value)
            with self.assertRaises(ValueError):restore_fixture(value)
        with self.assertRaises(ValueError):restore_feature((HERE/'scenario.feature').read_text().replace('reserve60','forged',1))

    def test_protocol_revision_change_is_only_observed_metadata(self):
        new,_=authored(envelopes=False);old,_=authored(protocol=False,envelopes=False)
        for previous,current in zip(old['subcases'],new['subcases']):
            if current['id']!=SELECTED[0]:
                self.assertEqual(previous,current);continue
            restored=copy.deepcopy(current)
            query_id='t09-sale-source-reserve60-current-segment'
            query_action=next(a for a in restored['actions'] if a['id']==query_id)
            reserve=next(a for a in restored['actions'] if a['id']=='reserve60')
            self.assertEqual(reserve['request']['slots']['segmentId'],query_action['request']['id'])
            self.assertEqual('QuantitySegment',query_action['request']['scope']['objectType'])
            self.assertEqual(ACTOR,query_action['actorRef'])
            reserve['request']['expectedRevision']=1
            restored['actions']=[a for a in restored['actions'] if a['id']!=query_id]
            self.assertEqual(previous,restored)
        mutated=copy.deepcopy(authored()[0])
        next(a for a in mutated['subcases'][0]['actions'] if a['id']=='reserve60')['request']['expectedRevision']=0
        with self.assertRaises(ValueError):restore(mutated)

    def test_exact_envelope_changes_and_inverse(self):
        current,_=authored();previous,_=authored(envelopes=False)
        # Exact file hash from 1c2f40b2's strict ledger; no dependency on a
        # worker-only commit remaining reachable after coordinator integration.
        previous_bytes=(json.dumps(previous,ensure_ascii=False,indent=2)+'\n').encode()
        self.assertEqual('c0a6985876e4f8e4506945a65c1da0ead5836fe1493882feed935f475142c3eb',hashlib.sha256(previous_bytes).hexdigest())
        reverted=copy.deepcopy(current)
        self.assertEqual(4,len(envelope_selectors()))
        for change in envelope_selectors():
            sub=next(s for s in reverted['subcases'] if s['id']==change['subcase'])
            action=next(a for a in sub['actions'] if a['id']==change['actionId'])
            pointer=action['request']['slots']['allocationId']['$result']
            self.assertEqual(change['sourceActionId'],pointer['actionId'])
            self.assertEqual(change['after'],pointer['pointer'])
            pointer['pointer']=change['before']
            altered=copy.deepcopy(current)
            target=next(s for s in altered['subcases'] if s['id']==change['subcase'])
            next(a for a in target['actions'] if a['id']==change['actionId'])['request']['slots']['allocationId']['$result']['pointer']='/response/effects/counterfeitId'
            with self.assertRaises(ValueError):restore(altered)
        self.assertEqual(previous,reverted)

    def test_ledger_itself_rejects_tampering(self):
        ledger=expected_ledger()
        verify_ledger(ledger)
        for key,value in [('originalAssertions',93),('operatingApprovalClaimed',True),('requiresLabel',True)]:
            altered=copy.deepcopy(ledger);altered[key]=value
            with self.assertRaises(ValueError):verify_ledger(altered)

    def test_generator_reproduces_without_reading_oracles(self):
        case,fixtures=authored()
        self.assertEqual(case,json.loads((HERE/'case.json').read_text()))
        self.assertEqual(case,apply(case))
        for path,f in fixtures.items():self.assertEqual(f,json.loads((ROOT/path).read_text()))
        for sub in case['subcases']:
            if sub['id'] not in SELECTED:continue
            f=json.loads(baseline(sub['fixtureRef']))
            expected=source_actions(sub,f)
            sub['assertions']=[{'expected':'POISON'}]
            self.assertEqual(expected,source_actions(sub,f))

    def test_scope_policy_typed_graph_and_independent_actions(self):
        case,fixtures=authored()
        spec=importlib.util.spec_from_file_location('rc',ROOT/'verification/cases/request_contract.py')
        rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
        contract=rc.Contracts(ROOT)
        for sub in case['subcases']:
            if sub['id'] not in SELECTED:continue
            f=fixtures[sub['fixtureRef']]
            self.assertEqual(VERSION,f['aliases']['syntheticPolicyVersion']['version'])
            for name in (ACTOR,DELEGATOR):
                a=f['actors'][name];self.assertEqual(CAPABILITIES,a['roleCapabilities']);self.assertEqual(CAPABILITIES,a['grant']['actions'])
                self.assertEqual({'organizationAlias':'ORG','itemAliases':['P']},a['grant']['scope'])
            if sub['id']==SELECTED[1]:self.assertNotIn('getEvidence',f['actors']['warehouse']['grant']['actions'])
            for action in ('SELL','DISPATCH'):
                policy=f['aliases']['t09SalePolicy'+action]
                for k,v in FLAGS.items():self.assertEqual(v,policy[k])
                self.assertFalse(policy['requiresLabel']);self.assertEqual(NAMESPACE,policy['sourceNamespace'])
            actions=[a for a in sub['actions'] if a['id'].startswith('t09-sale-source-')]
            self.assertEqual(69 if sub['id']==SELECTED[0] else 68,len(actions))
            for a in actions:
                if a['kind']=='invoke':
                    problems,known=contract.command_problems(a,sub)
                    self.assertEqual(([],[]),(problems,known),a['id'])
            basis=[a for a in actions if a['capabilityId']=='recordDispositionBasis']
            self.assertEqual({(ac,cat) for ac in ('SELL','DISPATCH') for cat in ('QC','CUSTOMER','COMMERCIAL')},
                {(a['request']['slots']['action'],a['request']['slots']['category']) for a in basis})
            for a in basis:
                slots=a['request']['slots'];self.assertTrue(slots['evidenceId']['$result']['actionId'].endswith('-document'))
                self.assertEqual('sale60' if sub['id']==SELECTED[0] else 'sale',slots['workId']['$result']['actionId'])
            for a in actions:
                if a['capabilityId']=='recordActivity':
                    e=a['request']['slots'];doc=next(x for x in actions if x['id']==a['id'].removesuffix('-event')+'-document')
                    self.assertEqual(e['payload']['$encode']['source'],doc['request']['slots']['contentBase64']['$encode']['source'])
                    self.assertEqual(e['payload']['$encode']['source'],doc['request']['slots']['expectedHash']['$encode']['source'])

    def test_t11_t12_restoration_unchanged(self):
        from assessment_contracts import reverse
        for name in ('T11','T12'):
            path=f'verification/cases/{name}/case.json'
            before=json.loads(baseline(path));current=json.loads((ROOT/path).read_text())
            self.assertEqual(before,current)
            self.assertEqual(reverse(name,before),reverse(name,current))
        subprocess.run(['python3','-m','unittest','test_source_contracts'],cwd=HERE,check=True)

if __name__=='__main__':unittest.main()
