"""Unit checks for the T25 verifyCoverage host command: baseline PASS on the real inputs, one specific issue per mutation.

Run: python3 -m unittest discover -s verification/actual/scenarios -p 'test_*.py'
Writes only a synthetic preparation report under verification/harness/target (ignored) and removes it.
"""
import importlib.util, json, pathlib, subprocess, unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location('coverage_verify', ROOT / 'verification/actual/scenarios/coverage_verify.py')
cv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cv)
REPORT = 'verification/harness/target/test-coverage-verify/prepare.json'
SCOPE = {'caseId': 'T20', 'oracleId': 'T20.mrtr-bound-state', 'observationName': 'invalid-continuation-effects'}


class PreparationCoverageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = ROOT / REPORT
        path.parent.mkdir(parents=True, exist_ok=True)
        commit = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()
        path.write_text(json.dumps({'status': 'PREPARED', 'runtimeStatus': 'NOT_RUN', 'gateComplete': False, 'codeCommit': commit,
                                    'workingTreeDirty': False, 'semanticOracleEquivalence': 'REQUIRES_CASE_REVIEW',
                                    'runtimeGates': [{'profile': 'regulatory', 'status': 'NOT_RUN_GATED'}]}))

    @classmethod
    def tearDownClass(cls):
        (ROOT / REPORT).unlink()

    def run_with(self, mutation):
        request = {'registryPath': 'verification/cases/registry.json', 'catalogPath': 'verification/requirements/mandatory-oracles.json',
                   'inputSnapshotKind': 'PREPARATION', 'preparationReportPath': REPORT, 'mutation': mutation, 'mutationScope': SCOPE}
        return cv.preparation(ROOT, request, REPORT)

    def test_baseline_links_every_catalog_oracle(self):
        rows = self.run_with('none')
        self.assertEqual('PASS', rows['validation']['status'], rows['validation']['issues'][:3])
        self.assertEqual(41, len(rows['requiredCases']))
        self.assertEqual('PREPARED', rows['gate']['preparationStatus'])
        self.assertFalse(rows['gate']['gateComplete'])
        self.assertNotIn('baseline', rows)

    def test_each_mutation_reports_exactly_its_issue(self):
        for mutation, code in [('dropOracle', 'MISSING_ORACLE'), ('dropObservation', 'MISSING_OBSERVATION'),
                               ('skipCase', 'SKIPPED_REQUIRED_CASE'), ('mandatoryWaiver', 'MANDATORY_PATH_WAIVED')]:
            with self.subTest(mutation=mutation):
                rows = self.run_with(mutation)
                self.assertEqual('PASS', rows['baseline']['validation']['status'])
                self.assertEqual('FAIL', rows['validation']['status'])
                self.assertEqual([code], [i['code'] for i in rows['validation']['issues']])
                self.assertEqual({'caseId': 'T20', 'mutation': mutation}, {k: rows['mutatedInput'][k] for k in ('caseId', 'mutation')})
                # The read registry/catalog are reported unmutated; the mutation lives only in the in-memory copy.
                self.assertEqual(41, len(rows['requiredCases']))


class ModelCoverageTest(unittest.TestCase):
    def test_each_model_mutation_reports_exactly_its_issue(self):
        corpus = json.loads((ROOT / 'verification/model-corpus/corpus.json').read_text())
        bindings = json.loads((ROOT / 'verification/model-binding/registry.json').read_text())
        manifest = {'status': 'PREPARED', 'runtimeStatus': 'NOT_RUN', 'attempts': [], 'usage': None, 'actualModelCalls': 0}
        self.assertEqual('PASS', cv.model_validation(corpus, bindings, manifest)['status'])
        for mutation, code in [('drop-binding', 'MISSING_MODEL_BINDING'), ('missing-attempt-usage', 'ATTEMPT_USAGE_MISSING'),
                               ('null-usage-as-zero', 'MISSING_USAGE_COERCED_ZERO'), ('partial-cost-complete', 'INCOMPLETE_USAGE_PASS'),
                               ('unapproved-business-SLA', 'R8_ACCEPTANCE_UNCONFIRMED')]:
            with self.subTest(mutation=mutation):
                result = cv.model_validation(*cv.mutate_model(mutation, corpus, bindings, manifest))
                self.assertEqual([code], [i['code'] for i in result['issues']])


if __name__ == '__main__':
    unittest.main()
