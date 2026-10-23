import copy
import json
import sys
import unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import author_runtime_inputs as layer
import author_parent_ranges as parent
from test_parent_ranges import blocks

class RuntimeInputs(unittest.TestCase):
    def setUp(self):self.old=layer.baseline();self.new=layer.lifecycle_layer().restore(json.loads((HERE/'case.json').read_text()))

    def test_fixed_point_strict_inverse_and_complete_ledger(self):
        self.assertEqual(self.new,layer.apply(self.old));self.assertEqual(self.new,layer.apply(self.new))
        self.assertEqual(self.old,layer.restore(self.new))
        ledger=json.loads((HERE/'runtime-input-ledger.json').read_text())
        self.assertEqual(layer.digest(self.new),ledger['afterSha256'])
        self.assertEqual(layer.authored(self.old)[1],ledger['changes'])
        self.assertEqual(parent.baseline(),parent.restore(self.new))

    def test_110_assertion_bytes_all_fixtures_and_older_ledgers_unchanged(self):
        self.assertEqual(blocks(layer.original_bytes('case.json').decode()),blocks(layer.lifecycle_layer().original('case.json').decode()))
        self.assertEqual(110,sum(len(s['assertions']) for s in self.new['subcases']))
        self.assertEqual([s for s in self.old['subcases'] if s['id'] not in layer.SELECTED],
                         [s for s in self.new['subcases'] if s['id'] not in layer.SELECTED])
        for path in [*(HERE/'fixtures').glob('*.json'),HERE/'parent-range-input-ledger.json']:
            self.assertEqual(layer.original_bytes(str(path.relative_to(HERE))),path.read_bytes())

    def test_no_authorized_public_shortfall_alternate_is_fabricated(self):
        for old in self.old['subcases']:
            if old['id'] not in layer.PARENTS:continue
            current=next(s for s in self.new['subcases'] if s['id']==old['id'])
            self.assertEqual(old,current)
            self.assertFalse(any(a.get('capabilityId') in ('openObligation','createObligation') for a in current['actions']))
            fixture=json.loads((layer.ROOT/old['fixtureRef']).read_text())
            for actor in fixture['actors'].values():
                self.assertNotIn('openObligation',actor['roleCapabilities'])
                self.assertNotIn('openObligation',actor.get('grant',{}).get('actions',[]))

    def test_changed_and_added_inputs_use_existing_public_authorized_contracts(self):
        catalog=json.loads((layer.ROOT/'contracts/capability-slots.json').read_text())['capabilities']
        public={c['id'] for c in json.loads((layer.ROOT/'contracts/acceptance-capabilities.json').read_text())['capabilities']}
        subs={s['id']:s for s in self.new['subcases']}
        for change in layer.authored(self.old)[1]:
            sub=subs[change['subcaseId']];fixture=json.loads((layer.ROOT/sub['fixtureRef']).read_text())
            for a in [*change['insertedBefore'],change['after'],*change['insertedAfter']]:
                if a['kind']=='control':continue
                self.assertIn(a['capabilityId'],public)
                actor=fixture['actors'][a['actorRef']]
                self.assertIn(a['capabilityId'],actor['roleCapabilities'])
                self.assertIn(a['capabilityId'],actor['grant']['actions'])
                if a['kind']=='invoke':self.assertLessEqual(set(a['request']['slots']),set(catalog[a['capabilityId']]['slots']))
        wait=next(s for s in self.new['subcases'] if s['id']=='wait-timeout')
        resume=next(a for a in wait['actions'] if a['id']=='resume-without-proof')
        self.assertEqual({},resume['request']['slots']['evidence'])
        self.assertNotIn('resumeEvidenceIds',resume['request']['slots'])
        self.assertEqual(layer.ref('resume-without-proof-current-work','/response/data/revision'),resume['request']['expectedRevision'])
        close=next(a for s in self.new['subcases'] if s['id']=='pending-correction' for a in s['actions'] if a['id']=='close')
        self.assertEqual(layer.ref('close-current-work','/response/data/revision'),close['request']['expectedRevision'])

    def test_pending_correction_uses_actual_event_revision_and_truthful_98_payload(self):
        sub=next(s for s in self.new['subcases'] if s['id']=='pending-correction')
        actions={a['id']:a for a in sub['actions']};r=actions['correction']['request'];p=r['slots'];q='correction-original-event'
        self.assertEqual({'id':layer.ref('receipt-event','/response/id'),'scope':{'organizationId':{'$alias':'ORG'},'kind':'EVENT'}},actions[q]['request'])
        self.assertEqual(layer.ref(q,'/response/data/revision'),r['expectedRevision'])
        self.assertEqual(layer.ref(q,'/response/data/ID'),p['supersedesId'])
        self.assertEqual([{'type':'Work','id':layer.ref(q,'/response/data/subjectId')}],r['subjectRefs'])
        original=actions['receipt-event']['request']['slots'];payload=copy.deepcopy(original['payload'])
        payload['$encode']['source']['quantity']='98'
        self.assertEqual(payload,p['payload']);self.assertEqual(('98','BOX','2','EVENT'),(p['quantity'],p['unit'],p['sourceVersion'],p['evidenceType']))
        for key in ('effectiveFrom','timeZone','timePrecision','valueState'):self.assertEqual(original[key],p[key])
        self.assertNotIn('documentId',p);self.assertNotIn('canonicalOccurrenceId',p)
        fields=json.loads((layer.ROOT/'contracts/capability-slots.json').read_text())['capabilities']['correctEvidence']['forms']['EVENT']
        self.assertLessEqual(set(p),set(fields))
        for name in ('delay-reassessment','release-reassessment'):
            control=actions[name]['control'];params=control['parameters']
            self.assertEqual('evidence.correct.afterPendingBeforeAssessment',params['point'])
            self.assertEqual({'point','scope'},set(params));self.assertEqual({'$alias':'ORG'},params['scope']['organizationId'])
        self.assertEqual('release',actions['release-reassessment']['control']['operation'])

    def test_closed_followup_is_not_falsely_repaired_with_an_event_or_receipt_id(self):
        old=next(s for s in self.old['subcases'] if s['id']=='closed-followup')
        new=next(s for s in self.new['subcases'] if s['id']=='closed-followup')
        self.assertEqual(old,new)
        follow=next(a for a in new['actions'] if a['id']=='followup')
        self.assertEqual(layer.ref('temperature','/response/occurrenceId'),follow['request']['slots']['sourceOccurrenceId'])
        fixture=json.loads((layer.ROOT/new['fixtureRef']).read_text())
        self.assertEqual([],fixture['baseline']['obligations']);self.assertEqual([],fixture['baseline']['assignments'])

    def test_shared_ranges_keep_original_kind_quantity_source_and_fresh_fence(self):
        sub=next(s for s in self.new['subcases'] if s['id']=='shared-distinct60');actions={a['id']:a for a in sub['actions']}
        for name,start in [('parent-one-link','0'),('parent-two-link','30')]:
            r=actions[name]['request'];self.assertEqual('SHARES_ACTIVITY',r['slots']['linkKind'])
            self.assertEqual(start,r['slots']['startQuantity']);self.assertEqual({'value':'30','unit':'BOX'},r['slots']['quantity'])
            self.assertEqual(layer.ref('parent-one','/response/workId'),actions[name+'-current-work']['request']['workId'])
            self.assertEqual(layer.ref(name+'-current-work','/response/data/revision'),r['expectedRevision'])

    def test_negative_mutations_are_not_normalized_away(self):
        for kind in ('oracle','actor','quantity','source','command','inventProof','range','unselected','correctionSource','correctionDoc','correctionQuantity','faultPoint'):
            v=copy.deepcopy(self.new);sub=next(s for s in v['subcases'] if s['id']=='shared-distinct60')
            actions={a['id']:a for a in sub['actions']};link=actions['parent-two-link']
            if kind=='oracle':sub['assertions'][0]['expected']='APPLIED'
            elif kind=='actor':link['actorRef']='supervisor'
            elif kind=='quantity':link['request']['slots']['quantity']['value']='90'
            elif kind=='source':link['request']['slots']['occurrenceId']=layer.ref('receive60','/response/effects/receiptId')
            elif kind=='command':link['capabilityId']='openObligation'
            elif kind=='inventProof':next(a for s in v['subcases'] if s['id']=='wait-timeout' for a in s['actions'] if a['id']=='resume-without-proof')['request']['slots']['evidence']={'receipt':{'$alias':'DOC'}}
            elif kind=='range':link['request']['slots']['startQuantity']='31'
            elif kind=='unselected':v['subcases'][0]['actions'][0]['evidenceRefs']=[]
            else:
                pending=next(s for s in v['subcases'] if s['id']=='pending-correction')
                aa={a['id']:a for a in pending['actions']}
                if kind=='correctionSource':aa['correction']['request']['slots']['supersedesId']=layer.ref('receipt-canonical','/response/id')
                elif kind=='correctionDoc':aa['correction']['request']['slots']['documentId']=layer.ref('receipt-event-document','/response/id')
                elif kind=='correctionQuantity':aa['correction']['request']['slots']['payload']['$encode']['source']['quantity']='100'
                else:aa['delay-reassessment']['control']['parameters']['point']='command.beforeCommit'
            with self.assertRaises(ValueError):layer.restore(v)
            with self.assertRaises(ValueError):parent.restore(v)

    def test_feature_inverse_preserves_original_and_refuses_mutation(self):
        text=layer.lifecycle_layer().original('scenario.feature').decode();self.assertEqual(layer.feature(),text)
        self.assertEqual(layer.original_bytes('scenario.feature').decode(),layer.restore_feature(text))
        self.assertEqual(parent.restore_feature(text),parent.restore_feature(layer.restore_feature(text)))
        with self.assertRaises(ValueError):layer.restore_feature(text.replace('close-current-work','forged',1))

if __name__=='__main__':unittest.main()
