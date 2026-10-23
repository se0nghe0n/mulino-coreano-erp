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
    def setUp(self):self.old=layer.baseline();self.new=json.loads((HERE/'case.json').read_text())

    def test_fixed_point_strict_inverse_and_complete_ledger(self):
        self.assertEqual(self.new,layer.apply(self.old));self.assertEqual(self.new,layer.apply(self.new))
        self.assertEqual(self.old,layer.restore(self.new))
        ledger=json.loads((HERE/'runtime-input-ledger.json').read_text())
        self.assertEqual(layer.digest(self.new),ledger['afterSha256'])
        self.assertEqual(layer.authored(self.old)[1],ledger['changes'])
        self.assertEqual(parent.baseline(),parent.restore(self.new))

    def test_110_assertion_bytes_all_fixtures_and_older_ledgers_unchanged(self):
        self.assertEqual(blocks(layer.original_bytes('case.json').decode()),blocks((HERE/'case.json').read_text()))
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

    def test_shared_ranges_keep_original_kind_quantity_source_and_fresh_fence(self):
        sub=next(s for s in self.new['subcases'] if s['id']=='shared-distinct60');actions={a['id']:a for a in sub['actions']}
        for name,start in [('parent-one-link','0'),('parent-two-link','30')]:
            r=actions[name]['request'];self.assertEqual('SHARES_ACTIVITY',r['slots']['linkKind'])
            self.assertEqual(start,r['slots']['startQuantity']);self.assertEqual({'value':'30','unit':'BOX'},r['slots']['quantity'])
            self.assertEqual(layer.ref('parent-one','/response/workId'),actions[name+'-current-work']['request']['workId'])
            self.assertEqual(layer.ref(name+'-current-work','/response/data/revision'),r['expectedRevision'])

    def test_negative_mutations_are_not_normalized_away(self):
        for kind in ('oracle','actor','quantity','source','command','inventProof','range','unselected'):
            v=copy.deepcopy(self.new);sub=next(s for s in v['subcases'] if s['id']=='shared-distinct60')
            actions={a['id']:a for a in sub['actions']};link=actions['parent-two-link']
            if kind=='oracle':sub['assertions'][0]['expected']='APPLIED'
            elif kind=='actor':link['actorRef']='supervisor'
            elif kind=='quantity':link['request']['slots']['quantity']['value']='90'
            elif kind=='source':link['request']['slots']['occurrenceId']=layer.ref('receive60','/response/effects/receiptId')
            elif kind=='command':link['capabilityId']='openObligation'
            elif kind=='inventProof':next(a for s in v['subcases'] if s['id']=='wait-timeout' for a in s['actions'] if a['id']=='resume-without-proof')['request']['slots']['evidence']={'receipt':{'$alias':'DOC'}}
            elif kind=='range':link['request']['slots']['startQuantity']='31'
            else:v['subcases'][0]['actions'][0]['evidenceRefs']=[]
            with self.assertRaises(ValueError):layer.restore(v)
            with self.assertRaises(ValueError):parent.restore(v)

    def test_feature_inverse_preserves_original_and_refuses_mutation(self):
        text=(HERE/'scenario.feature').read_text();self.assertEqual(layer.feature(),text)
        self.assertEqual(layer.original_bytes('scenario.feature').decode(),layer.restore_feature(text))
        self.assertEqual(parent.restore_feature(text),parent.restore_feature(layer.restore_feature(text)))
        with self.assertRaises(ValueError):layer.restore_feature(text.replace('close-current-work','forged',1))

if __name__=='__main__':unittest.main()
