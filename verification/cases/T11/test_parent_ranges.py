import copy
import importlib.util
import json
import re
import sys
import unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import author_parent_ranges as layer


def blocks(text):
    out=[]
    for match in re.finditer(r'"assertions":\s*', text):
        start=match.end();_,length=json.JSONDecoder().raw_decode(text[start:]);out.append(text[start:start+length])
    return out


class ParentRanges(unittest.TestCase):
    def setUp(self):
        self.old=layer.baseline();self.current=layer.runtime_layer().restore(json.loads((HERE/'case.json').read_text()))

    def test_strict_inverse_and_authoring_fixed_point(self):
        self.assertEqual(self.current,layer.apply(self.old));self.assertEqual(self.current,layer.apply(self.current))
        self.assertEqual(self.old,layer.restore(self.current))
        ledger=json.loads((HERE/'parent-range-input-ledger.json').read_text())
        self.assertEqual(layer.digest(self.current),ledger['afterSha256'])
        self.assertEqual(layer.authored(self.old)[1],ledger['changes'])

    def test_all_110_oracle_bytes_and_other_nine_subcases_unchanged(self):
        import subprocess
        before=subprocess.check_output(['git','show',layer.BASE+':verification/cases/T11/case.json'],cwd=layer.ROOT).decode()
        self.assertEqual(blocks(before),blocks(layer.runtime_layer().lifecycle_layer().original('case.json').decode()))
        self.assertEqual(110,sum(len(s['assertions']) for s in self.current['subcases']))
        others=[s for s in self.old['subcases'] if s['id'] not in layer.SELECTED]
        self.assertEqual(9,len(others));self.assertEqual(others,[s for s in self.current['subcases'] if s['id'] not in layer.SELECTED])
        # All original fixtures, grants, times and source documents remain byte-identical.
        for path in (HERE/'fixtures').glob('*.json'):
            old=subprocess.check_output(['git','show',layer.BASE+':'+str(path.relative_to(layer.ROOT))],cwd=layer.ROOT)
            self.assertEqual(old,path.read_bytes())

    def test_feature_additions_preserve_every_original_line(self):
        import subprocess
        current=layer.runtime_layer().restore_feature((HERE/"scenario.feature").read_text())
        self.assertEqual(layer.authored_feature(),current)
        old=subprocess.check_output(["git","show",layer.BASE+":verification/cases/T11/scenario.feature"],cwd=layer.ROOT).decode()
        self.assertEqual(old,layer.restore_feature(current))
        with self.assertRaises(ValueError):layer.restore_feature(current.replace("child30-link", "forged-child30-link",1))

    def test_ranges_and_current_revision_are_explicit_public_inputs(self):
        slots=json.loads((layer.ROOT/'contracts/capability-slots.json').read_text())['capabilities']
        for sub in self.current['subcases']:
            if sub['id'] not in layer.SELECTED:continue
            actions={a['id']:a for a in sub['actions']}
            for name,start,amount in [('child30-link','0','30'),('child60-link','30','60')]:
                a=actions[name];r=a['request'];self.assertEqual(start,r['slots']['startQuantity']);self.assertEqual({'value':amount,'unit':'BOX'},r['slots']['quantity'])
                self.assertNotIn('physicalScope',r['slots']);self.assertEqual(layer.ref(name+'-current-work','/response/data/revision'),r['expectedRevision'])
            for a in sub['actions']:
                if a.get('capabilityId')=='closeWork':
                    self.assertEqual({'workId','reason'},set(a['request']['slots']))
                    self.assertLess(sub['actions'].index(actions[a['id']+'-current-work']),sub['actions'].index(a))
                if a.get('capabilityId') in ('closeWork','createWorkLink'):
                    self.assertLessEqual(set(a['request']['slots']),set(slots[a['capabilityId']]['slots']))

    def test_strict_inverse_rejects_input_oracle_scope_and_unselected_drift(self):
        for mutation in ('quantity','range','oracle','grantlike','other'):
            value=copy.deepcopy(self.current);sub=value['subcases'][0]
            link=next(a for a in sub['actions'] if a['id']=='child30-link')
            if mutation=='quantity':link['request']['slots']['quantity']['value']='31'
            elif mutation=='range':link['request']['slots']['startQuantity']='1'
            elif mutation=='oracle':sub['assertions'][0]['expected']='APPLIED'
            elif mutation=='grantlike':link['actorRef']='supervisor'
            else:value['subcases'][3]['actions'][0]['evidenceRefs']=[]
            with self.assertRaises(ValueError):layer.restore(value)

    def test_composes_with_existing_source_cumulative_and_assessment_layers(self):
        sys.path.insert(0,str(layer.ROOT/'verification/cases/T09'))
        from source_contracts import repair_inputs
        sys.path.insert(0,str(layer.ROOT/'verification/cases'))
        from author_cumulative_goal_setup import repair
        fixtures={}
        def fixture(ref):return fixtures.setdefault(ref,json.loads((layer.ROOT/ref).read_text()))
        self.assertEqual(self.current,repair_inputs(repair(copy.deepcopy(self.current)),fixture))


if __name__=='__main__':unittest.main()
