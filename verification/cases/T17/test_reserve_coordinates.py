"""좌표 authoring의 범위 보존, 재현성 및 수량 경계를 검사한다."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location('author_reserve_coordinates', HERE / 'author_reserve_coordinates.py')
AUTHOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUTHOR)


class ReserveCoordinateContract(unittest.TestCase):
    def setUp(self):
        self.case = json.loads((HERE / 'case.json').read_text())

    def test_reproduce_exact_case_and_preserve_every_other_field(self):
        before = copy.deepcopy(self.case)
        for sub in self.case['subcases']:
            if sub['id'] in AUTHOR.STARTS:
                action = next(a for a in sub['actions'] if a['id'] == 'reserve')
                del action['request']['slots']['startQuantity']
                del action['request']['provenance']['startQuantity']
        self.assertEqual(14, AUTHOR.apply(self.case))
        self.assertEqual(before, self.case)
        self.assertEqual(0, AUTHOR.apply(self.case))

    def test_reject_stock_overflow_and_fractional_box(self):
        for quantity in ('31', '0.5'):
            with self.subTest(quantity=quantity):
                case = copy.deepcopy(self.case)
                sub = next(s for s in case['subcases'] if s['id'] == 'normal-consumed-delivery')
                next(a for a in sub['actions'] if a['id'] == 'reserve')['request']['slots']['quantity'] = quantity
                with self.assertRaises(ValueError):
                    AUTHOR.apply(case)

    def test_partial_reservation_keeps_its_quantity(self):
        sub = next(s for s in self.case['subcases'] if s['id'] == 'normal-consumed-delivery')
        slots = next(a for a in sub['actions'] if a['id'] == 'reserve')['request']['slots']
        slots['quantity'] = '20'
        AUTHOR.apply(self.case)
        self.assertEqual('20', slots['quantity'])
        self.assertEqual('0', slots['startQuantity'])


if __name__ == '__main__':
    unittest.main()
