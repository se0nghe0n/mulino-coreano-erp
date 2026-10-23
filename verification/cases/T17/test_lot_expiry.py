"""LOT expiry conventions preserve the original semantic fixture and oracle bytes."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BASELINE = 'eebb2998511d57826c83303aed2f446ebde8f3a0'
SPEC = importlib.util.spec_from_file_location('author_lot_expiry', HERE/'author_lot_expiry.py')
LOT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(LOT)
PUBLIC_SPEC = importlib.util.spec_from_file_location('author_public_contracts', HERE/'author_public_contracts.py')
PUBLIC = importlib.util.module_from_spec(PUBLIC_SPEC)
PUBLIC_SPEC.loader.exec_module(PUBLIC)


def original(path):
    return json.loads(subprocess.check_output(['git','show',f'{BASELINE}:{path.relative_to(ROOT)}'],cwd=ROOT))


class LotExpiryContract(unittest.TestCase):
    def test_exact_authoring_and_reverse_preserve_all_other_facts(self):
        for path in sorted((HERE/'fixtures').glob('*.json')):
            with self.subTest(fixture=path.stem):
                before = original(path); after = PUBLIC.restore_fixture(json.loads(path.read_text()))
                generated = copy.deepcopy(before)
                self.assertTrue(LOT.apply(generated))
                self.assertEqual(after, generated)
                self.assertFalse(LOT.apply(generated))
                self.assertEqual(before, LOT.revert(after))
                self.assertEqual(before['actors'], after['actors'])
                self.assertEqual(before['baseline']['segments'], after['baseline']['segments'])
                self.assertEqual(before['baseline']['conditions'], after['baseline']['conditions'])
                self.assertEqual(before['evidence'], after['evidence'])
                for alias, value in before['aliases'].items():
                    if value['type'] == 'QuantitySegment':
                        restored = copy.deepcopy(after['aliases'][alias]); restored['lotAlias'] = value['lotAlias']
                        self.assertEqual(value, restored)
                    elif value['type'] != 'ManufacturerLot':
                        self.assertEqual(value, after['aliases'][alias])
                # Only exact B/EARLY lot references and explicit typed metadata change.
                for delta in after['baseline']['policy'][LOT.META]['changes']:
                    path = delta['path']
                    self.assertTrue(path.startswith(('/aliases/L/', '/aliases/L_B', '/aliases/L_EARLY', '/aliases/t17LotManufacturer'))
                                    or path in ('/aliases/B/lotAlias', '/aliases/EARLY/lotAlias')
                                    or (path.startswith(('/baseline/sourceProvenance/', '/baseline/works/')) and path.endswith('/lotAlias')), path)

    def test_all_explicit_expiries_agree_with_their_typed_lot(self):
        for path in (HERE/'fixtures').glob('eligibility-*.json'):
            f = json.loads(path.read_text()); LOT.validate(f)
            self.assertEqual('L', f['aliases']['A']['lotAlias'])
            self.assertEqual('2026-10-20T00:00:00Z', f['aliases']['L']['expiresAt'])
            for name, expiry in [('B','2026-10-25T00:00:00Z'), ('EARLY','2026-10-10T00:00:00Z')]:
                self.assertEqual(expiry, f['aliases'][f['aliases'][name]['lotAlias']]['expiresAt'])
            wrong = copy.deepcopy(f);wrong['aliases']['B']['lotAlias']='L'
            with self.assertRaises(ValueError): LOT.validate(wrong)
            self.assertEqual(original(path)['aliases']['U'], f['aliases']['U'])

    def test_explicit_unknown_expiry_is_not_filled(self):
        path = HERE/'fixtures/eligibility-unknown.json';f=original(path)
        f['aliases']['U']['expiresAt']='UNKNOWN'
        LOT.apply(f)
        self.assertNotIn('expiresAt',f['aliases'][f['aliases']['U']['lotAlias']])
        self.assertEqual('UNKNOWN',f['aliases']['U']['expiresAt'])

    def test_normal_and_post_dispatch_conventions_are_new_and_bounded(self):
        for path in (HERE/'fixtures').glob('*.json'):
            f=json.loads(path.read_text());meta=f['baseline']['policy'][LOT.META]
            if path.stem=='post-dispatch-expiry':
                self.assertEqual('2026-10-07T09:05:00Z',f['aliases']['L']['expiresAt'])
            elif path.stem in LOT.NORMAL:
                self.assertEqual(max(c['validUntil'] for c in f['baseline']['conditions']),f['aliases']['L']['expiresAt'])
                self.assertTrue(meta['selection'].startswith('NEW_SYNTHETIC'))
            self.assertFalse(meta['operatingExpiryClaimed'])
        f=original(HERE/'fixtures/normal-consumed-delivery.json');f['baseline']['conditions'][0]['status']='UNKNOWN'
        with self.assertRaises(ValueError):LOT.apply(f)

    def test_oracle_bytes_are_identical(self):
        for name in ['case.json','scenario.feature']:
            path=HERE/name
            before = subprocess.check_output(['git','show',f'{BASELINE}:{path.relative_to(ROOT)}'],cwd=ROOT)
            if name == 'case.json': self.assertEqual(json.loads(before), PUBLIC.restore_case(json.loads(path.read_bytes())))
            else: self.assertEqual(before.decode(),PUBLIC.restore_feature(path.read_text()))
        c=json.loads((HERE/'case.json').read_text())
        self.assertEqual(257,sum(len(s['assertions']) for s in c['subcases']))


if __name__=='__main__':unittest.main()
