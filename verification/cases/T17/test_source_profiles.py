"""Source profile authoring must preserve every original T17 byte/physical fact."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BASELINE = '04af8e3c8c76820b855d6b644dc36d958b446d44'
SPEC = importlib.util.spec_from_file_location('author_source_profiles', HERE / 'author_source_profiles.py')
AUTHOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUTHOR)
SOURCE_SPEC = importlib.util.spec_from_file_location('author_eligibility_sources', HERE / 'author_eligibility_sources.py')
SOURCE = importlib.util.module_from_spec(SOURCE_SPEC)
SOURCE_SPEC.loader.exec_module(SOURCE)
LOT_SPEC = importlib.util.spec_from_file_location('author_lot_expiry', HERE / 'author_lot_expiry.py')
LOT = importlib.util.module_from_spec(LOT_SPEC)
LOT_SPEC.loader.exec_module(LOT)
PUBLIC_SPEC = importlib.util.spec_from_file_location('author_public_contracts', HERE / 'author_public_contracts.py')
PUBLIC = importlib.util.module_from_spec(PUBLIC_SPEC)
PUBLIC_SPEC.loader.exec_module(PUBLIC)


def original(path):
    return subprocess.check_output(['git', 'show', f'{BASELINE}:{path.relative_to(ROOT)}'], cwd=ROOT)


class SourceProfileContract(unittest.TestCase):
    def test_every_original_file_and_physical_fact_preserved(self):
        tracked = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', BASELINE,
                                          'verification/cases/T17'], cwd=ROOT, text=True).splitlines()
        profiles = 0
        for name in tracked:
            path = ROOT / name
            with self.subTest(file=name):
                before = original(path)
                if path.parent == HERE / 'fixtures':
                    fixture = LOT.revert(PUBLIC.restore_fixture(json.loads(path.read_bytes())))
                    fixture['baseline']['sourceProfiles'] = [p for p in fixture['baseline']['sourceProfiles'] if p['sourceNamespace'] != SOURCE.NAMESPACE]
                    del fixture['baseline']['t17SourceFacts']
                    fixture['baseline']['works'] = [w for w in fixture['baseline']['works'] if not w['alias'].startswith('t17SourceWork-')]
                    if not fixture['baseline']['works']: del fixture['baseline']['works']
                    for key in [k for k in fixture['aliases'] if k == SOURCE.ACTOR or k == 't17RegulatoryPolicy' or k.startswith('t17SourceWork-')]: del fixture['aliases'][key]
                    del fixture['actors'][SOURCE.ACTOR]
                    for profile in fixture['baseline']['sourceProfiles']:
                        for field in AUTHOR.FIELDS:
                            del profile[field]
                        profiles += 1
                    self.assertEqual(before, (json.dumps(fixture, ensure_ascii=False, indent=2) + '\n').encode())
                elif path.name == 'scenario.feature':
                    self.assertEqual(before.decode(), PUBLIC.restore_feature(path.read_text()))
                elif path.name == 'case.json':
                    self.assertEqual(json.loads(before), PUBLIC.restore_case(json.loads(path.read_bytes())))
                elif path.name not in {'author_source_profiles.py', 'test_source_profiles.py', 'T17SourceProfileMappingCheck.java'}:
                    self.assertEqual(before, path.read_bytes())
        self.assertEqual(370, profiles)

    def test_regeneration_is_stable_and_exactly_additive(self):
        for path in (HERE / 'fixtures').glob('*.json'):
            before = json.loads(original(path))
            expected = PUBLIC.restore_fixture(json.loads(path.read_bytes()))
            self.assertEqual(3 * len(before['baseline']['sourceProfiles']), AUTHOR.apply(before))
            SOURCE.apply(before)
            LOT.apply(before)
            self.assertEqual(expected, before)
            self.assertEqual(0, AUTHOR.apply(before))
            SOURCE.apply(before)
            LOT.apply(before)
            self.assertEqual(expected, before)

    def test_reject_conflicting_namespace_owner_policy(self):
        fixture = json.loads((HERE / 'fixtures/normal-consumed-delivery.json').read_bytes())
        for key in AUTHOR.FIELDS:
            changed = copy.deepcopy(fixture)
            changed['baseline']['sourceProfiles'][0][key] = 'different'
            with self.subTest(field=key), self.assertRaises(ValueError):
                AUTHOR.apply(changed)

    def test_no_business_authority_added(self):
        for path in (HERE / 'fixtures').glob('*.json'):
            before = json.loads(original(path))
            after = json.loads(path.read_bytes())
            self.assertEqual(before['actors'], {k:v for k,v in after['actors'].items() if k != SOURCE.ACTOR})
            self.assertEqual({'organizationAlias': 'ORG', 'itemAliases': sorted({v['itemAlias'] for v in before['aliases'].values() if v['type'] == 'QuantitySegment'})}, after['actors'][SOURCE.ACTOR]['grant']['scope'])
            # No actor has the decision permissions required by QualityCommands.
            capabilities = {c for a in before['actors'].values() for c in a['roleCapabilities']}
            self.assertTrue(capabilities.isdisjoint({'decideQuality', 'confirmCustomerConditions',
                                                    'confirmCommercialDisposition'}))

    def test_case_actions_feature_and_257_assertions_unchanged(self):
        path = HERE / 'case.json'
        before = original(path)
        self.assertEqual(json.loads(before), PUBLIC.restore_case(json.loads(path.read_bytes())))
        case = json.loads(before)
        count = sum(len(s['assertions']) for s in case['subcases'])
        self.assertEqual(257, count)
        self.assertEqual(original(HERE / 'scenario.feature').decode(), PUBLIC.restore_feature((HERE / 'scenario.feature').read_text()))
        print('T17 original case sha256=' + hashlib.sha256(before).hexdigest())
        print('T17 original feature sha256=' + hashlib.sha256(original(HERE / 'scenario.feature')).hexdigest())


if __name__ == '__main__':
    unittest.main()
