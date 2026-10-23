"""Exact reversible T17 deltas and real source input boundaries, never oracle reduction."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE='ac0549e65aabc2137ac44827f544bc3cb0ca782c'
spec=importlib.util.spec_from_file_location('public',HERE/'author_public_contracts.py')
PUBLIC=importlib.util.module_from_spec(spec);spec.loader.exec_module(PUBLIC)


def original(path):
    return subprocess.check_output(['git','show',BASE+':'+str(path.relative_to(ROOT))],cwd=ROOT)


def assertion_blocks(data):
    text=data.decode();blocks=[];decoder=json.JSONDecoder();start=0
    while True:
        pos=text.find('"assertions": [',start)
        if pos<0:return blocks
        pos=text.index('[',pos);_,length=decoder.raw_decode(text[pos:])
        blocks.append(text[pos:pos+length].encode());start=pos+length


class PublicContractTests(unittest.TestCase):
    def test_exact_fixedpoint_inverse_and_257_raw_oracles(self):
        before=json.loads(original(HERE/'case.json'))
        fixtures={p.stem:json.loads(original(p)) for p in (HERE/'fixtures').glob('*.json')}
        generated,changed=PUBLIC.generate(before,fixtures)
        generated,changed=PUBLIC.first_failure.apply(generated,changed,PUBLIC)
        generated,changed=PUBLIC.completion.apply(generated,changed,PUBLIC)
        actual=json.loads((HERE/'case.json').read_text())
        self.assertEqual(generated,actual)
        self.assertEqual(before,PUBLIC.restore_case(actual))
        for sid,f in changed.items():
            self.assertEqual(f,json.loads((HERE/'fixtures'/f'{sid}.json').read_text()))
            self.assertEqual(fixtures[sid],PUBLIC.restore_fixture(f))
            self.assertEqual(fixtures[sid]['actors'],PUBLIC.completion.restore_fixture(f)['actors'])
            self.assertEqual(fixtures[sid]['baseline']['segments'],f['baseline']['segments'])
            self.assertEqual(fixtures[sid]['baseline']['conditions'],f['baseline']['conditions'])
        old=assertion_blocks(original(HERE/'case.json'));new=assertion_blocks((HERE/'case.json').read_bytes())
        restored=PUBLIC.restore_case(actual)
        restored_blocks=[json.dumps(sub['assertions'],ensure_ascii=False,indent=2).replace('\n','\n      ').encode() for sub in restored['subcases']]
        self.assertEqual(old,restored_blocks);self.assertEqual(15,len(new))
        self.assertEqual(257,sum(len(s['assertions']) for s in actual['subcases']))
        self.assertEqual(original(HERE/'scenario.feature').decode(),PUBLIC.restore_feature((HERE/'scenario.feature').read_text()))
        ledger=json.loads((HERE/'public-contract-delta.json').read_text())
        self.assertEqual(BASE,ledger['baseline'])
        def without_pointers(value):
            if isinstance(value,dict):return {k:without_pointers(v) for k,v in value.items() if k!='pointer'}
            if isinstance(value,list):return [without_pointers(v) for v in value]
            return value
        for delta in ledger['assertionChanges']:
            self.assertEqual(without_pointers(delta['before']),without_pointers(delta['after']))
            def compare(a,b):
                if isinstance(a,dict):
                    self.assertEqual(set(a),set(b))
                    for key in a:
                        if key=='pointer' and a[key]!=b[key]:
                            self.assertIn((a[key],b[key]),{
                                ('/response/allocationId','/response/effects/allocationId'),
                                ('/response/restrictionId','/response/effects/restrictionId'),
                                ('/response/id','/response/effects/deliveryId')})
                        else:compare(a[key],b[key])
                elif isinstance(a,list):
                    self.assertEqual(len(a),len(b))
                    for x,y in zip(a,b):compare(x,y)
                else:self.assertEqual(a,b)
            compare(delta['before'],delta['after'])
        for s,b in zip(actual['subcases'],before['subcases']):
            ids={a['id'] for a in b['actions']}
            self.assertEqual([a['id'] for a in b['actions']],[a['id'] for a in s['actions'] if a['id'] in ids])
            self.assertEqual({a['id']:a.get('actorRef') for a in b['actions']},
                             {a['id']:a.get('actorRef') for a in s['actions'] if a['id'] in ids})
        proof={'baseline':BASE,'assertionCount':257,'originalRawAssertionBlockSha256':[hashlib.sha256(b).hexdigest() for b in old],
               'currentRawAssertionBlockSha256':[hashlib.sha256(b).hexdigest() for b in new],
               'assertionReferenceCorrections':len(ledger['assertionChanges']),
               'featureSha256':hashlib.sha256((HERE/'scenario.feature').read_bytes()).hexdigest(),
               'caseSha256':hashlib.sha256((HERE/'case.json').read_bytes()).hexdigest(),
               'exactReversibleActionDeltas':len(ledger['changes'])}
        target=ROOT/'.local/t17-public-contracts';target.mkdir(parents=True,exist_ok=True)
        (target/'preservation-proof.json').write_text(json.dumps(proof,indent=2)+'\n')

    def test_otherpack_only_new_lot_matches_item_and_keeps_original_quantity(self):
        path=HERE/'fixtures/eligibility-packaging-unapproved.json';f=json.loads(path.read_text())
        self.assertEqual('L_ALT',f['aliases']['OTHERPACK']['lotAlias'])
        lot=f['aliases']['L_ALT'];self.assertEqual('ALT',lot['itemAlias'])
        self.assertEqual(f['aliases']['L']['manufacturerAlias'],lot['manufacturerAlias'])
        self.assertEqual(f['aliases']['L']['expiresAt'],lot['expiresAt'])
        self.assertEqual(json.loads(original(path)),PUBLIC.restore_fixture(f))
        # All confirmed LOT/item references in this fixture agree, including A/B/EARLY.
        for a in f['aliases'].values():
            if a['type']=='QuantitySegment':self.assertEqual(a['itemAlias'],f['aliases'][a['lotAlias']]['itemAlias'])
        wrong=copy.deepcopy(f);wrong['aliases']['OTHERPACK']['lotAlias']='L'
        with self.assertRaises(ValueError):PUBLIC.validate_fixture(wrong)

    def test_sources_keep_original_requests_and_claims_never_link_canonical(self):
        before=json.loads(original(HERE/'case.json'));actual=json.loads((HERE/'case.json').read_text())
        for s,b in zip(actual['subcases'],before['subcases']):
            if s['id'] not in PUBLIC.DELIVERY_CASES:continue
            name='claim' if s['id'].startswith('claim-') else 'delivery'
            actions={a['id']:a for a in s['actions']};old=next(a for a in b['actions'] if a['id']==name)
            doc=actions[name+'-source-document']['request']['slots'];event=actions[name+'-source-event']['request']['slots']
            content=doc['contentBase64']['$encode']['source']
            # Only exact public response pointers may differ inside the preserved request.
            restored=PUBLIC.restore_case({'subcases':[copy.deepcopy(s)]})['subcases'][0]
            self.assertEqual(old,next(a for a in restored['actions'] if a['id']==name))
            self.assertEqual(content,event['payload']['$encode']['source'])
            self.assertEqual(content,doc['expectedHash']['$encode']['source'])
            for key in ['quantity','unit','occurredAt']:
                self.assertEqual(old['request']['slots'][key],content[key])
            self.assertTrue(content['synthetic']);self.assertFalse(content['operatingApprovalClaimed'])
            if name=='claim':self.assertNotIn(name+'-source-canonical',actions)
            else:self.assertEqual({'observationId','canonicalOccurrenceId'},set(actions[name]['request']['slots']))
            if name=='claim':
                self.assertEqual(old['actorRef'],actions[name+'-source-event']['actorRef'])
                for key in ['sourceNamespace','externalEventId','sourceVersion']:
                    self.assertEqual(old['request']['slots'][key],event[key])
                if old['request']['slots']['verificationInput']['observedPhysicalScope'].endswith('-unmatched'):
                    self.assertNotEqual({'$alias':'A'},content['rangeRootId'])
                    self.assertNotIn('lotId',content)

    def test_delta_tampering_and_nonleaf_coordinates_rejected(self):
        case=json.loads((HERE/'case.json').read_text());case['subcases'][0]['actions'][1]['request']['slots']['quantity']='999'
        # Exact preservation catches even an unchanged action outside the authored ledger.
        with self.assertRaises((AssertionError,ValueError)):self.assertEqual(json.loads(original(HERE/'case.json')),PUBLIC.restore_case(case))
        fixture=json.loads(original(HERE/'fixtures/normal-consumed-delivery.json'))
        action={'request':{'slots':{'segmentId':{'$alias':'A'},'quantity':'30','unit':'BOX'},'provenance':{}}}
        fixture['aliases']['A']['startQuantity']='1'
        with self.assertRaises(ValueError):PUBLIC.coordinates(action,fixture)


if __name__=='__main__':unittest.main()
