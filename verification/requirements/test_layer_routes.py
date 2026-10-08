"""Observation-level MCP/SKILLS route check: current repository and counterexamples.
Run: python3 -m unittest discover -s verification/requirements -p 'test_layer_routes.py' -v
"""
import json, pathlib, shutil, subprocess, sys, tempfile, unittest
ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'verification/requirements/check_layer_routes.py'

class LayerRoutes(unittest.TestCase):
    def copy(self, tmp):
        catalog = json.loads((ROOT / 'verification/requirements/mandatory-oracles.json').read_text())
        (tmp / 'verification/requirements').mkdir(parents=True)
        shutil.copy2(ROOT / 'verification/requirements/mandatory-oracles.json', tmp / 'verification/requirements/mandatory-oracles.json')
        shutil.copy2(ROOT / 'verification/requirements/layer-route-review.json', tmp / 'verification/requirements/layer-route-review.json')
        for case_id in catalog['requiredCaseIds']:
            (tmp / f'verification/cases/{case_id}').mkdir(parents=True)
            shutil.copy2(ROOT / f'verification/cases/{case_id}/case.json', tmp / f'verification/cases/{case_id}/case.json')

    def run_check(self, root):
        return subprocess.run([sys.executable, '-I', str(SCRIPT), '--root', str(root)], capture_output=True, text=True)

    def test_repository_has_no_unexplained_gap(self):
        done = self.run_check(ROOT)
        self.assertEqual(0, done.returncode, done.stdout + done.stderr)

    def mutate(self, case_id, change, review=None):
        with tempfile.TemporaryDirectory() as name:
            tmp = pathlib.Path(name); self.copy(tmp)
            path = tmp / f'verification/cases/{case_id}/case.json'
            case = json.loads(path.read_text()); change(case); path.write_text(json.dumps(case))
            if review:
                rpath = tmp / 'verification/requirements/layer-route-review.json'
                record = json.loads(rpath.read_text()); review(record); rpath.write_text(json.dumps(record))
            return self.run_check(tmp)

    # No real KNOWN_OPEN gap remains (step2r round 4 closed T20 x3 and V4 x1). The counterexamples recreate the V4 one
    # by dropping the MCP leg of mixed-atomic-batch.
    KNOWN_V4 = {'status': 'KNOWN_OPEN', 'oracleId': 'V4.all-alternate-write-paths', 'observationName': 'mixed-batch-allowed-partial-effects',
                'layer': 'MCP', 'owner': 'V4 case owner (counterexample)', 'reason': 'MCP leg removed in this test copy',
                'closeWhen': 'the MCP leg is restored'}

    @staticmethod
    def drop_mcp_batch(case):
        for sub in case['subcases']:
            if sub['id'] == 'mixed-atomic-batch':
                sub['actions'] = [a for a in sub['actions'] if not a['id'].startswith('mcp-batch')]
                sub['assertions'] = [a for a in sub['assertions'] if not a['id'].startswith('mcp-batch-')]

    def test_repository_has_no_known_open_gap(self):
        done = self.run_check(ROOT)
        self.assertNotIn('KNOWN_OPEN', done.stdout)
        self.assertIn('"knownOpen": 0', done.stdout)

    def test_removed_mcp_batch_leg_is_an_unexplained_gap(self):
        done = self.mutate('V4', self.drop_mcp_batch)
        self.assertEqual(1, done.returncode, done.stdout)
        self.assertIn('unexplained layer route gap V4 V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects MCP', done.stdout)

    def test_known_open_is_listed_with_owner(self):
        done = self.mutate('V4', self.drop_mcp_batch, lambda record: record['entries'].append(dict(self.KNOWN_V4)))
        self.assertEqual(0, done.returncode, done.stdout)
        self.assertIn('KNOWN_OPEN V4 case owner (counterexample)', done.stdout)
        self.assertIn('"knownOpen": 1', done.stdout)

    def test_stale_known_open_entry_fails(self):
        # A closed gap whose entry stays in the list would silently re-open the allowlist: the list must be exact.
        done = self.mutate('V4', lambda case: None, lambda record: record['entries'].append(dict(self.KNOWN_V4)))
        self.assertEqual(1, done.returncode, done.stdout)
        self.assertIn('stale layer-route review entry', done.stdout)

    def test_known_open_without_owner_or_closing_condition_fails(self):
        for field in ('owner', 'closeWhen', 'reason'):
            with self.subTest(field=field):
                done = self.mutate('V4', self.drop_mcp_batch, lambda record: record['entries'].append(dict(self.KNOWN_V4, **{field: ' '})))
                self.assertEqual(1, done.returncode, done.stdout)
                self.assertIn('unexplained layer route gap', done.stdout)

    def test_api_only_duty_check_is_a_gap(self):
        def drop(case):
            for sub in case['subcases']:
                sub['assertions'] = [a for a in sub['assertions'] if not (a['id'].startswith('logistics-closes-unrelated-duties') and a['id'].endswith('-mcp'))]
        done = self.mutate('E1', drop)
        self.assertEqual(1, done.returncode)
        self.assertIn('E1.independent-goals-and-owners/logistics-closes-unrelated-duties\tMCP', done.stdout)

    def test_skill_hash_list_alone_is_a_gap(self):
        def drop(case):
            for sub in case['subcases']:
                sub['assertions'] = [a for a in sub['assertions'] if not a['id'].startswith('skill-stage-')]
        done = self.mutate('C3', drop)
        self.assertEqual(1, done.returncode)
        self.assertIn('C3.model-query-write-boundary/query-intent-write-tool-execution\tSKILLS', done.stdout)

    def test_mcp_route_renamed_to_api_is_a_gap(self):
        def reroute(case):
            for sub in case['subcases']:
                for action in sub['actions']:
                    if action.get('route') == 'mcp': action['route'] = 'api'
        done = self.mutate('V8', reroute)
        self.assertEqual(1, done.returncode)
        self.assertIn('V8.db-blob-definition-restore/restore-artifacts\tMCP', done.stdout)

if __name__ == '__main__':
    unittest.main()
