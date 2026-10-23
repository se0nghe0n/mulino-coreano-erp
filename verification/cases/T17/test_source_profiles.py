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
                    fixture = json.loads(path.read_bytes())
                    for profile in fixture['baseline']['sourceProfiles']:
                        for field in AUTHOR.FIELDS:
                            del profile[field]
                        profiles += 1
                    self.assertEqual(before, (json.dumps(fixture, ensure_ascii=False, indent=2) + '\n').encode())
                else:
                    self.assertEqual(before, path.read_bytes())
        self.assertEqual(370, profiles)

    def test_regeneration_is_stable_and_exactly_additive(self):
        for path in (HERE / 'fixtures').glob('*.json'):
            before = json.loads(original(path))
            expected = json.loads(path.read_bytes())
            self.assertEqual(3 * len(before['baseline']['sourceProfiles']), AUTHOR.apply(before))
            self.assertEqual(expected, before)
            self.assertEqual(0, AUTHOR.apply(before))

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
            self.assertEqual(before['actors'], after['actors'])
            # No actor has the decision permissions required by QualityCommands.
            capabilities = {c for a in after['actors'].values() for c in a['roleCapabilities']}
            self.assertTrue(capabilities.isdisjoint({'decideQuality', 'confirmCustomerConditions',
                                                    'confirmCommercialDisposition'}))

    def test_case_actions_feature_and_257_assertions_unchanged(self):
        path = HERE / 'case.json'
        before = original(path)
        self.assertEqual(before, path.read_bytes())
        case = json.loads(before)
        count = sum(len(s['assertions']) for s in case['subcases'])
        self.assertEqual(257, count)
        self.assertEqual(original(HERE / 'scenario.feature'), (HERE / 'scenario.feature').read_bytes())
        print('T17 original case sha256=' + hashlib.sha256(before).hexdigest())
        print('T17 original feature sha256=' + hashlib.sha256((HERE / 'scenario.feature').read_bytes()).hexdigest())


if __name__ == '__main__':
    unittest.main()
