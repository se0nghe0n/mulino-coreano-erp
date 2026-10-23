import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('public',HERE/'author_public_contracts.py')
PUBLIC=importlib.util.module_from_spec(spec);spec.loader.exec_module(PUBLIC)
FIRST=PUBLIC.first_failure


def baseline(path):return subprocess.check_output(['git','show',FIRST.BASE+':'+str(path.relative_to(ROOT))],cwd=ROOT)


class FirstFailureInputsTest(unittest.TestCase):
    def test_historical_first_layer_preserves_original_grants_dates_and_quantities(self):
        before=json.loads(baseline(HERE/'case.json'))
        originals={p.stem:json.loads(baseline(p)) for p in (HERE/'fixtures').glob('*.json')}
        generated,fixtures=FIRST.apply(copy.deepcopy(before),copy.deepcopy(originals),PUBLIC)
        current=PUBLIC.completion.restore_case(PUBLIC.delivery.restore_case(PUBLIC.goal.restore_case(json.loads((HERE/'case.json').read_text()))))
        self.assertEqual(generated,current)
        self.assertEqual(before,FIRST.restore_case(current))
        for sid,f in fixtures.items():
            self.assertEqual(f,PUBLIC.completion.restore_fixture(PUBLIC.delivery.restore_fixture(PUBLIC.goal.restore_fixture(json.loads((HERE/'fixtures'/f'{sid}.json').read_text())))))
            self.assertEqual(originals[sid],FIRST.restore_fixture(f))
            for key in ['actors','evidence','responsibilities','clock']:
                self.assertEqual(originals[sid][key],f[key])
            for key in ['segments','conditions','sourceProvenance']:
                self.assertEqual(originals[sid]['baseline'][key],f['baseline'][key])
        def blocks(text):
            decoder=json.JSONDecoder();result=[];start=0
            while True:
                start=text.find('"assertions": [',start)
                if start<0:return result
                start=text.index('[',start);_,size=decoder.raw_decode(text[start:])
                result.append(text[start:start+size]);start+=size
        raw_original=blocks(baseline(HERE/'case.json').decode())
        self.assertEqual(raw_original,blocks(PUBLIC.sales_inputs.legacy_bytes((HERE/'case.json').read_bytes()).decode()))
        proof={'baseline':FIRST.BASE,'oracleCount':257,'rawAssertionBlockSha256':[hashlib.sha256(b.encode()).hexdigest() for b in raw_original],
               'publicLedgerSha256':hashlib.sha256((HERE/'public-contract-delta.json').read_bytes()).hexdigest(),
               'caseSha256':hashlib.sha256((HERE/'case.json').read_bytes()).hexdigest(),
               'featureSha256':hashlib.sha256((HERE/'scenario.feature').read_bytes()).hexdigest()}
        output=ROOT/'.local/t17-first-failure-preservation.json';output.parent.mkdir(exist_ok=True)
        output.write_text(json.dumps(proof,indent=2)+'\n')
        self.assertEqual(257,sum(len(s['assertions']) for s in current['subcases']))
        for original,current_sub in zip(before['subcases'],current['subcases']):
            self.assertEqual(original['assertions'],current_sub['assertions'])
        self.assertEqual(baseline(HERE/'scenario.feature').decode(),FIRST.restore_feature(PUBLIC.completion.restore_feature(PUBLIC.delivery.restore_feature(PUBLIC.goal.restore_feature((HERE/'scenario.feature').read_text())))))
        # The prior public ledger's full fifteen pointer corrections stay intact.
        self.assertEqual(baseline(HERE/'public-contract-delta.json'),(HERE/'public-contract-delta.json').read_bytes())

    def test_dispatch_inputs_are_new_explicit_and_preserve_negative_status(self):
        for p in (HERE/'fixtures').glob('*.json'):
            f=json.loads(p.read_text());spec=f['baseline']['t17SourceFacts']
            for i,row in enumerate(spec.get('dispatchInputs',[])):
                self.assertEqual('DISPATCH',row['action']);self.assertFalse(row['operatingApprovalClaimed'])
                self.assertFalse(row['originalAuthorityClaimed']);self.assertEqual('coordinator-selected-fixture',row['authoredBy'])
                self.assertEqual(f['baseline']['conditions'][int(row['originalConditionPointer'].rsplit('/',1)[1])],row['originalCondition'])
                self.assertIn(row['originalCondition']['segmentAlias'],spec['dispatchSegmentAliases'])
        reserved=json.loads((HERE/'fixtures/reserve-pick-dispatch.json').read_text())
        self.assertEqual(['A'],reserved['baseline']['t17SourceFacts']['dispatchSegmentAliases'])
        self.assertEqual({'A'},{r['originalCondition']['segmentAlias'] for r in reserved['baseline']['t17SourceFacts']['dispatchInputs']})
        f=json.loads((HERE/'fixtures/normal-consumed-delivery.json').read_text())
        f['baseline']['conditions'][0]['status']='UNKNOWN'
        original=FIRST.restore_fixture(PUBLIC.completion.restore_fixture(PUBLIC.delivery.restore_fixture(PUBLIC.goal.restore_fixture(json.loads((HERE/'fixtures/normal-consumed-delivery.json').read_text())))))
        original['baseline']['conditions'][0]['status']='UNKNOWN'
        case=json.loads(baseline(HERE/'case.json'));case['subcases']=[s for s in case['subcases'] if s['id']=='normal-consumed-delivery']
        for state in ['UNKNOWN','DENIED','CONFLICT']:
            sample=copy.deepcopy(original);sample['baseline']['conditions'][0]['status']=state
            _,fixtures=FIRST.apply(copy.deepcopy(case),{'normal-consumed-delivery':sample},PUBLIC)
            self.assertEqual(state,fixtures['normal-consumed-delivery']['baseline']['t17SourceFacts']['dispatchInputs'][0]['originalCondition']['status'])

    def test_hold_source_is_scoped_to_original_request_and_finite_new_window(self):
        case=json.loads((HERE/'case.json').read_text())
        for sub in case['subcases']:
            actions={a['id']:a for a in sub['actions']}
            for a in sub['actions']:
                if a.get('capabilityId')!='placeHold':continue
                source=actions[a['id']+'-source-document']['request']['slots']['contentBase64']['$encode']['source']
                self.assertFalse(source['operatingApprovalClaimed']);self.assertFalse(source['originalAuthorityClaimed'])
                from uuid import UUID
                self.assertEqual(source['sourceDecisionId'],str(UUID(source['sourceDecisionId'])))
                self.assertEqual(source['sourceDecisionId'],actions[a['id']+'-source-event']['request']['slots']['externalEventId'])
                self.assertTrue(actions[a['id']+'-source-match']['request']['slots']['sourceIdentity'].endswith(':'+source['sourceDecisionId']+':1'))
                self.assertEqual(source['originalRequest']['quantity'],source['quantity'])
                self.assertEqual(source['originalRequest']['action'],source['action'])
                self.assertEqual(source['originalRequest']['segmentId'],source['segmentId'])
                self.assertEqual('NEW_HOLD_WINDOW_FROM_ORIGINAL_QC_CONDITION',source['validityAuthoring']['selection'])
                self.assertEqual('2026-10-07T09:00:00Z',source['validFrom'])
                self.assertLess(source['validFrom'],source['validUntil'])
                if 'originalRequestJson' in source:
                    raw=json.loads(source['originalRequestJson'])
                    self.assertEqual({'$result':{'actionId':'dispatch','pointer':'/response/effects/transitSegmentId'}},raw['segmentId'])
                    self.assertEqual({'$result':{'actionId':a['id']+'-source-scope','pointer':'/response/data/ID'}},source['segmentId'])
                self.assertEqual(source['validityAuthoring']['originalCondition']['validUntil'],source['validUntil'])
                self.assertEqual('qc' if source['category']=='QC' else 'admin',a['actorRef'])

    def test_historical_absent_due_dates_remain_recorded(self):
        before=json.loads(baseline(HERE/'case.json'));actual=PUBLIC.completion.restore_case(PUBLIC.delivery.restore_case(PUBLIC.goal.restore_case(json.loads((HERE/'case.json').read_text()))))
        missing=[]
        for b,s in zip(before['subcases'],actual['subcases']):
            actions={a['id']:a for a in s['actions']}
            for a in b['actions']:
                if a.get('capabilityId')=='createSalesOrder':
                    self.assertEqual(a,actions[a['id']])
                    if 'dueAt' not in a['request']['slots']:missing.append((s['id'],a['id']))
        ledger=json.loads((HERE/'first-failure-input-delta.json').read_text())
        self.assertEqual(missing,[(x['subcase'],x['actionId']) for x in ledger['unresolvedDueAt']])
        self.assertEqual(7,len(missing))

if __name__=='__main__':unittest.main()
