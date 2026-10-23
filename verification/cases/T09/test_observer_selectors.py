"""Strict observation corrections preserve all original business fixture inputs and expected values."""
import copy
import json
import unittest
from pathlib import Path
import observer_selectors as layer
from author_sale_sources import authored, HERE, expected_ledger, restore


class ObserverSelectors(unittest.TestCase):
    def test_only_reviewed_source_bindings_and_baseline_change(self):
        old, fixtures = authored(observers=False)
        current, new_fixtures = authored()
        self.assertEqual(fixtures, new_fixtures)
        self.assertEqual(old, layer.restore(current))
        self.assertEqual(current, layer.apply(old))
        self.assertEqual(current, json.loads((HERE/'case.json').read_text()))
        self.assertEqual(94, sum(len(s['assertions']) for s in current['subcases']))
        for before, after in zip(old['subcases'], current['subcases']):
            self.assertEqual([a['id'] for a in before['assertions']], [a['id'] for a in after['assertions']])
            for a, b in zip(before['assertions'], after['assertions']):
                for field in ('expected', 'unit', 'oracleRef', 'oracleExplanation', 'requirementRefs', 'scope'):
                    self.assertEqual(a.get(field), b.get(field), (before['id'], a['id'], field))
            # Every invoke/query/control input, business clock, grant and actor is byte-equivalent.
            for action in before['actions']:
                actual = next(a for a in after['actions'] if a['id'] == action['id'])
                if action['id'] == 'after-db' and before['id'] == 'cumulative-versus-state':
                    actual = copy.deepcopy(actual)
                    self.assertEqual('receipts', actual['observation']['sources'].pop())
                self.assertEqual(action, actual)

    def test_receipt_selector_uses_actual_confirmed_receipt_work_and_canonical_id(self):
        case, _ = authored()
        sub = next(s for s in case['subcases'] if s['id'] == 'cumulative-versus-state')
        for name in ('receive60-provisional', 'receive40-provisional'):
            source = next(a for a in sub['actions'] if a['id'] == name)['request']['slots']
            self.assertEqual({'$result': {'actionId': 'cumulative', 'pointer': '/response/workId'}}, source['workId'])
        for name in ('raw-receipt-sum100', 'arrival-distinct-identities'):
            assertion = next(a for a in sub['assertions'] if a['id'] == name)
            self.assertEqual('/data/rawRows/receipts', assertion['source']['pointer'])
            self.assertEqual({'workId': {'$result': {'actionId': 'cumulative', 'pointer': '/response/workId'}}}, assertion['source']['where'])
        self.assertEqual('canonicalOccurrenceId', next(a for a in sub['assertions'] if a['id'] == 'arrival-distinct-identities')['source']['field'])

    def test_selector_ledger_matches_every_exact_assertion_change(self):
        before, _ = authored(observers=False)
        after, _ = authored()
        ledger = json.loads((HERE/'observer-selector-corrections.json').read_text())
        changes = []
        for old_sub, new_sub in zip(before['subcases'], after['subcases']):
            for old, new in zip(old_sub['assertions'], new_sub['assertions']):
                if old != new:
                    changes.append(dict(subcase=old_sub['id'], assertionId=old['id'], before=old, after=new))
        self.assertEqual(3, len(changes))
        self.assertEqual(changes, ledger['changes'])
        self.assertEqual(94, ledger['originalAssertions'])
        self.assertEqual('b4772eb6b33929f6ce524523c40df06c98641b2e', ledger['baseline'])

    def test_strict_inverse_rejects_baseline_or_business_mutations(self):
        case, _ = authored()
        for variant in ('baseline-scope', 'baseline-time', 'baseline-source', 'baseline-op', 'expected', 'receipt-pointer'):
            bad = copy.deepcopy(case)
            sub = next(s for s in bad['subcases'] if s['id'] == 'throughout-complete')
            before = sub['actions'][1]
            if variant == 'baseline-scope': before['observation']['scope']['itemId'] = {'$alias': 'OTHER'}
            elif variant == 'baseline-time': before['observation']['knownAt'] = '2099-01-01T00:00:00Z'
            elif variant == 'baseline-source': before['observation']['sources'] = []
            elif variant == 'baseline-op': next(a for a in sub['assertions'] if a['id'] == 'no-quantity-changing-events')['op'] = 'count'
            elif variant == 'expected': sub['assertions'][0]['expected'] = 'UNSATISFIED'
            else: bad['subcases'][0]['assertions'][0]['source']['pointer'] = '/forged'
            with self.assertRaises((ValueError, AssertionError)):
                restore(bad)


if __name__ == '__main__': unittest.main()
