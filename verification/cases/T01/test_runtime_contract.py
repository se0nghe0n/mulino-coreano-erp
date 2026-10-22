"""T01 authoring invariants, separate from actual product acceptance."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent

class RuntimeContract(unittest.TestCase):
    def test_all_baseline_assertions_and_excluded_effects_are_preserved(self):
        case = json.loads((ROOT / 'case.json').read_text())
        self.assertEqual(12, len(case['subcases']))
        self.assertEqual(125, sum(len(s['assertions']) for s in case['subcases']))
        excluded = [s for s in case['subcases'] if s['id'].startswith('excluded-')]
        self.assertEqual({'manufacture','BOM','B2C','generalLedger','taxSubmit','bankTransfer'},
                         {s['id'].removeprefix('excluded-') for s in excluded})
        for sub in excluded:
            assertions = {a['id']: a for a in sub['assertions']}
            self.assertEqual('HELD', assertions['unsupported-outcome']['expected'])
            self.assertEqual('VERSION_UNSUPPORTED', assertions['unsupported-code']['expected'])
            self.assertEqual(0, assertions['no-substitute-command']['expected'])
            self.assertEqual({'commandIdempotencyKey': 'T01-'+sub['id'], 'status': 'COMMITTED'},
                             assertions['no-substitute-command']['source']['where'])
            self.assertEqual(6, sum(a['op']=='sameAs' for a in sub['assertions']))

    def test_read_fixture_scope_and_real_duties_are_explicit(self):
        fixture = json.loads((ROOT / 'fixture.json').read_text())
        for name, actor in fixture['actors'].items():
            scope = actor['grant']['scope']
            if name == 'sourceInstaller':
                self.assertEqual({'organizationAlias': 'ORG'}, scope)
                self.assertEqual('synthetic-source-installer', actor['subject'])
                continue
            self.assertEqual(['P'], scope['itemAliases'])
            self.assertNotIn('workAliases', scope)
            self.assertNotIn('segmentAliases', scope)
        for duty, work, owner in [('purchaseDuty','O1','reader'), ('salesDuty','S1','writer')]:
            row = fixture['aliases'][duty]
            self.assertEqual('Obligation',row['type'])
            self.assertEqual('WORK_NEXT_ACTION',row['kind'])
            self.assertEqual(work,row['workAlias'])
            self.assertEqual(owner,row['ownerAlias'])
            self.assertEqual('OPEN',row['status'])

if __name__ == '__main__':
    unittest.main()
