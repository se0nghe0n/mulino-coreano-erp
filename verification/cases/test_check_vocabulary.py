"""Mutants for check_vocabulary.py: each wrong name or pair must be reported.
Run: python3 -m unittest discover -s verification/cases -p 'test_check_vocabulary.py' -v
"""
import importlib.util, json, tempfile, unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location('check_vocabulary', Path(__file__).with_name('check_vocabulary.py'))

def load():
    module = importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(module); return module

def case(assertions, actions=None):
    return {'caseId': 'X01', 'subcases': [{'id': 's', 'actions': actions or [{'id': 'cmd', 'kind': 'invoke', 'capabilityId': 'placeHold'}], 'assertions': assertions}]}

def eq(id, action, pointer, expected, **source):
    return {'id': id, 'op': 'equals', 'source': {'actionId': action, 'pointer': pointer, **source}, 'expected': expected}

class VocabularyMutants(unittest.TestCase):
    def problems(self, data):
        m = load()
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'case.json'; path.write_text(json.dumps(data)); m.check_case(path)
        return m.problems

    def test_vocabulary_names_pass(self):
        self.assertEqual([], self.problems(case([eq('o', 'cmd', '/response/outcome', 'CONFLICT'), eq('c', 'cmd', '/response/error/code', 'STALE_REVISION'),
            {'id': 'a', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/audit', 'where': {'commandIdempotencyKey': 'k', 'outcome': 'CONFLICT'}}, 'expected': 1},
            {'id': 'k', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/obligations', 'where': {'kind': 'QUALITY_REVIEW'}}, 'expected': 1}])))

    def test_code_with_wrong_outcome_fails(self):
        self.assertTrue(any('STALE_REVISION is asserted with outcome REJECTED' in p for p in self.problems(case(
            [eq('o', 'cmd', '/response/outcome', 'REJECTED'), eq('c', 'cmd', '/response/error/code', 'STALE_REVISION')]))))

    def test_unknown_code_and_outcome_fail(self):
        found = self.problems(case([eq('o', 'cmd', '/response/outcome', 'PENDING_EXTERNAL'), eq('c', 'cmd', '/response/error/code', 'MADE_UP_CODE')]))
        self.assertTrue(any('PENDING_EXTERNAL' in p for p in found) and any('MADE_UP_CODE' in p for p in found))

    def test_retired_audit_names_fail(self):
        found = self.problems(case([{'id': 'a', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/audit', 'where': {'commandKey': 'k', 'result': 'FORBIDDEN'}}, 'expected': 1},
            {'id': 'b', 'op': 'equals', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/denialAudit'}, 'expected': 'FORBIDDEN'},
            {'id': 'c', 'op': 'relationSet', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/audit', 'field': ['commandIdempotencyKey', 'outcome']}, 'expected': [['k', 'TYPE_INVALID']]},
            {'id': 'd', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/audit', 'where': {'errorCode': 'FORBIDDEN'}}, 'expected': 1}]))
        for needle in ['commandKey', 'result', 'denialAudit', 'TYPE_INVALID is an error code', 'errorCode is CONDITIONAL']:
            self.assertTrue(any(needle in p for p in found), needle)

    def test_unknown_obligation_kind_fails(self):
        self.assertTrue(any('obligation kind QC' in p for p in self.problems(case(
            [{'id': 'k', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/obligations', 'where': {'kind': 'QC'}}, 'expected': 1}]))))

    def test_negative_assertion_is_not_use(self):
        self.assertEqual([], self.problems(case([{'id': 'n', 'op': 'notEquals', 'source': {'actionId': 'cmd', 'pointer': '/response/outcome'}, 'expected': 'PENDING_EXTERNAL'}])))

if __name__ == '__main__':
    unittest.main()
