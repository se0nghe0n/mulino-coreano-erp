"""SELFTEST only. Temporary protocol examples are not actual product runtime evidence."""
import copy
import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('coverage_assembly', HERE / 'assemble.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
COMMIT = 'a' * 40


class CoverageSelftest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.a = m.Assembly(self.root, COMMIT)

    def write(self, ref, value):
        path = self.root / ref
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value))
        return self.a.descriptor(ref)

    def protocol_fixture(self, profile='schema'):
        # Deliberately counterfactual protocol data: shape acceptance is not product execution.
        identity = dict(runId='test-run', hostId='test-host', workspaceId='test-workspace', actorId='test-actor')
        command = dict(argv=['java', 'acceptance'], display='java acceptance', exitCode=0,
                       startedAt='2026-10-07T00:00:00Z', completedAt='2026-10-07T00:01:00Z')
        versions = dict(schema='ontology-v1', definition='definition-v1', evaluator='evaluator-v1', policy='synthetic-policy-v1',
                        tool='java21', db='postgres18', protocol='mcp-v1', client='client-v1', model='model-v1', prompt='prompt-v1', skill='skill-v1')
        report = dict(status='PASS', profile=profile, codeCommit=COMMIT, command=command['display'], exitCode=0,
                      executionIdentity=identity, gateComplete=True, discovered=1, started=1, completed=1, skipped=0, cases=[])
        receipt = dict(schemaVersion='1.0.0', evidenceClass='ACTUAL', codeCommit=COMMIT, executionIdentity=identity, command=command, versions=versions,
                       inputs=[self.write('input.json', {'input': 'fixed'})], artifacts=[], reportArtifact=self.write('report.json', report))
        raw = dict(scope={'environmentId': 'test-workspace'}, evidenceClass='ACTUAL_RUNTIME', executionIdentity=identity, command=command, versions=versions,
                   provenance={'source': 'ACTUAL_RUNTIME', 'independent': True}, profileResult=report, observations={})
        receipt['artifacts'] = [dict(self.write('raw.json', raw), scope=raw['scope'], completeness='COMPLETE')]
        self.write('receipt.json', receipt)
        return copy.deepcopy(report), copy.deepcopy(receipt), copy.deepcopy(raw)

    def receipt_check(self, report, receipt, raw=None, profile='schema'):
        receipt = copy.deepcopy(receipt)
        receipt['reportArtifact'] = self.write('report.json', report)
        if raw is not None and receipt.get('artifacts'):
            receipt['artifacts'] = [dict(self.write('raw.json', raw), scope=raw['scope'], completeness='COMPLETE')]
        self.write('receipt.json', receipt)
        return self.a.receipt(report, 'receipt.json', profile, 'report.json')

    def test_status_failure_dominates_missing(self):
        self.assertEqual('FAIL', m.status_of(['FAIL', 'NOT_RUN', 'PASS']))
        self.assertEqual('NOT_RUN', m.status_of([]))
        self.assertEqual('NOT_RUN', m.status_of(['PASS', 'NOT_RUN']))

    def test_existing_byte_descriptor_is_checked(self):
        d = self.write('bytes.json', {'fact': 80})
        self.assertEqual(d, self.a.checked_descriptor(d))
        (self.root / 'bytes.json').write_text('{}')
        with self.assertRaises(ValueError):
            self.a.checked_descriptor(d)

    def test_size_zero_does_not_replace_missing_or_nonempty_bytes(self):
        d = self.write('bytes.json', {'fact': 80})
        d['sizeBytes'] = 0
        with self.assertRaises(ValueError):
            self.a.checked_descriptor(d)
        self.assertIsNone(self.a.read('missing.json'))
        self.assertEqual('NOT_RUN', self.a.problems[-1]['status'])

    def test_outside_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as outside:
            target = pathlib.Path(outside) / 'outside.json'
            target.write_text('{}')
            (self.root / 'escape.json').symlink_to(target)
            self.assertIsNone(self.a.read('escape.json'))
            self.assertEqual('FAIL', self.a.problems[-1]['status'])

    def test_protocol_shape_with_exact_capture_matches(self):
        report, receipt, raw = self.protocol_fixture()
        self.assertIsNotNone(self.receipt_check(report, receipt, raw))

    def test_actual_label_cannot_lift_canned_capture(self):
        report, receipt, raw = self.protocol_fixture()
        raw['evidenceClass'] = 'CAPTURED_SELFTEST'
        self.assertIsNone(self.receipt_check(report, receipt, raw))
        self.assertEqual('FAIL', self.a.problems[-1]['status'])

    def test_captured_provenance_rejected_even_with_actual_labels(self):
        report, receipt, raw = self.protocol_fixture()
        raw['provenance']['source'] = 'CANNED_CONTRACT_SELFTEST'
        self.assertIsNone(self.receipt_check(report, receipt, raw))

    def test_mismatching_captured_report_rejected(self):
        report, receipt, raw = self.protocol_fixture()
        raw['profileResult'] = dict(report, discovered=99)
        self.assertIsNone(self.receipt_check(report, receipt, raw))

    def test_receipt_mutation_rejected(self):
        mutations = [lambda r: r.update(codeCommit='b' * 40),
                     lambda r: r['executionIdentity'].update(runId='other'),
                     lambda r: r['command'].update(exitCode=1),
                     lambda r: r['command'].update(startedAt='2026-10-08T00:00:00Z'),
                     lambda r: r['versions'].update(schema=None),
                     lambda r: r['versions'].update(tool='stub-runtime'),
                     lambda r: r.update(inputs=[]), lambda r: r.update(artifacts=[]),
                     lambda r: r.update(evidenceClass='SELFTEST')]
        for mutation in mutations:
            with self.subTest(mutation=mutation):
                report, receipt, raw = self.protocol_fixture()
                mutation(receipt)
                self.assertIsNone(self.receipt_check(report, receipt, raw))

    def test_actual_missing_artifact_is_not_run(self):
        report, receipt, raw = self.protocol_fixture()
        (self.root / 'raw.json').unlink()
        self.assertIsNone(self.receipt_check(report, receipt))
        self.assertEqual('NOT_RUN', self.a.problems[-1]['status'])

    def test_local_cannot_replace_btp(self):
        report, receipt, raw = self.protocol_fixture('btp-deployment')
        receipt['environment'] = 'LOCAL'
        self.assertIsNone(self.receipt_check(report, receipt, raw, 'btp-deployment'))

    def test_actual_failure_preserved_without_receipt(self):
        self.write('failure.json', {'status': 'FAIL', 'reason': 'Observed duplicate effect'})
        profiles = self.a.profiles({'profiles': [{'profile': 'schema', 'reportRef': 'failure.json', 'receiptRef': 'absent.json', 'evidenceClass': 'ACTUAL'}]})
        self.assertEqual('FAIL', profiles['schema']['status'])

    def test_red_and_selftest_are_not_actual_profile_pass(self):
        self.write('pass.json', {'status': 'PASS', 'gateComplete': True})
        for evidence_class in ('SELFTEST', 'CONTRACT_RED'):
            profiles = self.a.profiles({'profiles': [{'profile': 'schema', 'reportRef': 'pass.json', 'evidenceClass': evidence_class}]})
            self.assertEqual('NOT_RUN', profiles['schema']['status'])

    def test_skipped_or_partly_started_profile_not_pass(self):
        for change in ({'skipped': 1}, {'completed': 0}, {'started': 0}, {'discovered': 0}):
            with self.subTest(change=change):
                report, receipt, raw = self.protocol_fixture()
                report.update(change)
                raw['profileResult'] = report
                self.receipt_check(report, receipt, raw)
                p = self.a.profiles({'profiles': [{'profile': 'schema', 'reportRef': 'report.json', 'receiptRef': 'receipt.json', 'evidenceClass': 'ACTUAL'}]})
                self.assertEqual('NOT_RUN', p['schema']['status'])

    def test_missing_prerequisite_keeps_later_profile_incomplete(self):
        report, receipt, raw = self.protocol_fixture('contracts')
        self.receipt_check(report, receipt, raw, 'contracts')
        p = self.a.profiles({'profiles': [{'profile': 'contracts', 'reportRef': 'report.json', 'receiptRef': 'receipt.json', 'evidenceClass': 'ACTUAL'}]})
        self.assertEqual('NOT_RUN', p['contracts']['status'])

    def test_waiver_cannot_replace_model_or_btp(self):
        for profile in ('model', 'btp-deployment'):
            self.write('waiver.json', {'status': 'WAIVED', 'waiver': 'no account'})
            p = self.a.profiles({'profiles': [{'profile': profile, 'reportRef': 'waiver.json', 'evidenceClass': 'ACTUAL', 'receiptRef': 'missing.json'}]})
            self.assertEqual('NOT_RUN', p[profile]['status'])
            self.assertTrue(any(i['status'] == 'FAIL' and 'Waiver' in i['reason'] for i in self.a.problems))

    def test_unknown_and_duplicate_profiles_rejected(self):
        self.write('pass.json', {'status': 'NOT_RUN'})
        entry = {'profile': 'schema', 'reportRef': 'pass.json', 'evidenceClass': 'SELFTEST'}
        self.a.profiles({'profiles': [entry, entry, dict(entry, profile='unsupported')]})
        self.assertEqual(2, sum(p['status'] == 'FAIL' for p in self.a.problems))

    def declaration(self):
        sub = {'id': 'only', 'actions': [{'id': 'read', 'kind': 'query'}], 'assertions': [{'id': 'quantity', 'op': 'decimalEquals',
                 'source': {'actionId': 'read', 'pointer': '/response/quantity'}, 'expected': '80', 'evidenceRefs': ['read:actual'],
                 'oracleRef': {'oracleId': 'T01.quantity', 'observationNames': ['quantity']}}]}
        case = self.write('verification/cases/T01/case.json', {'caseId': 'T01', 'subcases': [sub]})
        fixture = self.write('fixture.json', {'versions': {'definition': 'definition-v1'}})
        return {'caseId': 'T01', 'subcaseId': 'only', 'profile': 'schema', 'status': 'NOT_RUN',
                'caseHash': case['sha256'], 'fixtureArtifacts': [fixture], 'assertions': [], '_sub': sub}, case, fixture

    def test_partial_case_retains_observed_failure(self):
        declaration, _, _ = self.declaration()
        run = {'caseId': 'T01', 'subcaseId': 'only', 'status': 'NOT_RUN', 'assertions': [{'assertionId': 'quantity', 'status': 'FAIL'}]}
        self.a.run_case(declaration, {'schema': {'_report': {'cases': [run]}}})
        self.assertEqual('FAIL', declaration['status'])

    def test_assertion_source_cannot_change_without_capture_bytes(self):
        declaration, case, fixture = self.declaration()
        report, receipt, raw = self.protocol_fixture()
        action = {'actionId': 'read', 'driverStatus': 'EXECUTED', 'response': {'quantity': '80'},
                  'provenance': {'scopeComplete': True, 'source': 'ACTUAL_API'}, 'artifactRefs': ['raw.json']}
        assertion = dict(assertionId='quantity', expected='80', source=declaration['_sub']['assertions'][0]['source'], status='PASS')
        run = dict(caseId='T01', subcaseId='only', status='PASS', runtimeComplete=True,
                   caseHash=case['sha256'], fixtureHash=fixture['sha256'], versions=receipt['versions'],
                   startedAt='2026-10-07T00:00:01Z', finishedAt='2026-10-07T00:00:03Z',
                   actions={'read': action}, assertions=[assertion])
        report['cases'] = [run]
        raw['observations'] = {'read': copy.deepcopy(action)}
        raw['profileResult'] = copy.deepcopy(report)
        receipt['inputs'].extend([case, fixture])
        checked = self.receipt_check(report, receipt, raw)
        self.assertIsNotNone(checked)
        run['actions']['read']['response']['quantity'] = '0'
        self.a.run_case(declaration, {'schema': {'_report': report, '_receipt': checked, 'status': 'PASS'}})
        self.assertEqual('FAIL', declaration['status'])

    def test_missing_model_usage_remains_null_and_not_run(self):
        model = self.a.model({}, {})
        self.assertEqual('NOT_RUN', model['status'])
        self.assertIsNone(model['usage'])
        self.assertIsNone(model['cost'])
        self.assertIsNone(model['actualModelCalls'])

    def test_empty_model_repeats_dont_complete_even_if_pass_claimed(self):
        model = self.a.model({}, {'model': {'status': 'PASS', '_report': {'status': 'PASS', 'actualModelCalls': 219, 'usage': {'inputTokens': 0, 'outputTokens': 0}, 'cost': {'amount': '0', 'currency': 'USD', 'pricingRef': 'receipt'}}}})
        self.assertEqual('NOT_RUN', model['status'])

    def binding_fixture(self):
        corpus = json.loads((HERE.parents[1] / 'verification/model-corpus/corpus.json').read_text())
        ch = self.write('verification/model-corpus/corpus.json', corpus)
        entries, descriptors = [], []
        for ci, case in enumerate(corpus['cases']):
            turns = []
            for ti, turn in enumerate(case['turns']):
                tid = f'turn-{ti + 1}'
                turns.append({'id': tid, 'corpusTurnPointer': f'/cases/{ci}/turns/{ti}',
                              'commonAssertions': [{'assertionId': f'{case["id"]}/{tid}/common-{ai + 1}',
                                                    'corpusAssertionPointer': f'/cases/{ci}/turns/{ti}/oracle/assertions/{ai}',
                                                    'semanticPath': assertion['path']} for ai, assertion in enumerate(turn['oracle']['assertions'])]})
            ref = f'verification/model-binding/cases/{case["id"]}/binding.json'
            entries.append({'caseId': case['id'], 'bindingRef': ref, 'turnIds': [t['id'] for t in turns]})
            descriptors += [self.write(ref, {'caseId': case['id'], 'turns': turns}),
                            self.write(ref.replace('binding.json', 'scenario.feature'), {'evidenceClass': 'SELFTEST'}),
                            self.write(ref.replace('binding.json', 'fixture.json'), {'synthetic': True})]
        registry = {'plannedRepeats': 3, 'cases': entries}
        paths = sorted({a['path'] for c in corpus['cases'] for t in c['turns'] for a in t['oracle']['assertions']})
        descriptors += [ch, self.write('verification/model-binding/registry.json', registry),
                        self.write('verification/model-binding/semantic-paths.json', {'corpusSha256': ch['sha256'], 'paths': [{'semanticPath': p, 'common': True} for p in paths]})]
        return corpus, registry, {'inputArtifacts': descriptors}

    def test_all_221_assertions_and_154_paths_are_read_from_real_corpus(self):
        corpus, registry, report = self.binding_fixture()
        bindings = self.a.model_bindings(corpus, registry, report)
        self.assertEqual(221, len(bindings))
        self.assertEqual([], self.a.preparation_problems)

    def test_count_preserving_pointer_or_path_and_file_mutations_are_rejected(self):
        mutations = [('wrong-case-pointer', lambda b: b['turns'][0].update(corpusTurnPointer='/cases/1/turns/0')),
                     ('wrong-path', lambda b: b['turns'][0]['commonAssertions'][0].update(semanticPath='state.wrong.value')),
                     ('duplicate-pointer', lambda b: b['turns'][0]['commonAssertions'][1].update(corpusAssertionPointer=b['turns'][0]['commonAssertions'][0]['corpusAssertionPointer']))]
        for name, mutate in mutations:
            with self.subTest(name=name):
                corpus, registry, report = self.binding_fixture()
                ref = registry['cases'][0]['bindingRef']
                binding = json.loads((self.root / ref).read_text())
                mutate(binding)
                self.write(ref, binding)
                before = len(self.a.preparation_problems)
                self.a.model_bindings(corpus, registry, report)
                self.assertTrue(any(p['status'] == 'FAIL' for p in self.a.preparation_problems[before:]))

    def test_semantic_mapping_count_cannot_hide_one_missing_and_one_extra_path(self):
        corpus, registry, report = self.binding_fixture()
        ref = 'verification/model-binding/semantic-paths.json'
        paths = json.loads((self.root / ref).read_text())
        paths['paths'][0]['semanticPath'] = 'unexpected.path'
        changed = self.write(ref, paths)
        report['inputArtifacts'] = [changed if d['path'] == ref else d for d in report['inputArtifacts']]
        self.a.model_bindings(corpus, registry, report)
        self.assertTrue(any('154 immutable' in p['reason'] and p['status'] == 'FAIL' for p in self.a.preparation_problems))

    def test_count_preserving_normative_contract_weakening_is_rejected(self):
        repository = HERE.parents[1]
        catalog = json.loads((repository / 'verification/requirements/mandatory-oracles.json').read_text())
        refs = ['verification/requirements/mandatory-oracles.json', 'verification/requirements/normative-contract-lock.json',
                'verification/requirements/validate_catalog.py', 'verification/requirements/mandatory-oracles.schema.json']
        refs += [d['path'] for d in catalog['sourceFiles']]
        for ref in refs:
            target = self.root / ref
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes((repository / ref).read_bytes())
        catalog['oracles'][0]['expectedObservations'][0]['expected']['fields'] = []
        self.write('verification/requirements/mandatory-oracles.json', catalog)
        oracles, observations = self.a.catalog()
        self.assertEqual(122, len(oracles))
        self.assertEqual(499, len(observations))
        self.assertTrue(any(p['status'] == 'FAIL' and 'normative catalog' in p['reason'] for p in self.a.preparation_problems))

    def test_binding_preparation_cannot_close_model_gate(self):
        corpus = {'cases': [{'id': f'M{i:02}', 'turns': [{}]} for i in range(1, 61)]}
        corpus['cases'][0]['turns'] += [{}] * 13
        ch = self.write('verification/model-corpus/corpus.json', corpus)
        rh = self.write('verification/model-binding/registry.json', {'plannedRepeats': 3})
        self.write('verification/harness/target/evidence/model-binding-preparation.json', {
            'corpusSha256': ch['sha256'], 'bindingRegistrySha256': rh['sha256'], 'caseCount': 60, 'turnCount': 73,
            'semanticPathCount': 154, 'preparationStatus': 'PREPARED', 'productGateComplete': True, 'actualModelCalls': 0})
        model = self.a.model({}, {})
        self.assertEqual('NOT_RUN', model['status'])
        self.assertTrue(any(i['status'] == 'FAIL' for i in self.a.problems))

    def test_real_baseline_absence_is_complete_499_not_run_inventory(self):
        root = HERE.parents[1]
        result = m.Assembly(root).assemble({'profiles': []})
        m.validate_manifest(result)
        self.assertEqual(122, result['oracleCount'])
        self.assertEqual(499, result['observationCount'])
        self.assertEqual('NOT_RUN', result['status'])
        self.assertFalse(result['gateComplete'])
        self.assertFalse(result['productRuntimeClaimed'])
        self.assertEqual('NOT_RUN', result['model']['status'])
        # The no-case baseline is expected only at B2; completed future case files need not stay absent.
        self.assertTrue(all(o['status'] == 'NOT_RUN' for o in result['namedObservations']))

    def test_forged_summary_completion_and_downgrade_rejected(self):
        result = m.Assembly(HERE.parents[1]).assemble({'profiles': []})
        for flag in ['gateComplete', 'runtimeComplete', 'productRuntimeClaimed']:
            with self.subTest(flag=flag):
                mutant = copy.deepcopy(result)
                mutant[flag] = True
                with self.assertRaises(ValueError):
                    m.validate_manifest(mutant)
        mutant = copy.deepcopy(result)
        mutant['coverageProblems'].append({'status': 'FAIL', 'reason': 'Observed violation'})
        with self.assertRaises(ValueError):
            m.validate_manifest(mutant)

    def test_saved_manifest_reassembly_rejects_forged_observation_status(self):
        root = HERE.parents[1]
        result = m.Assembly(root).assemble({'profiles': []})
        m.validate_saved(root, result)
        mutant = copy.deepcopy(result)
        mutant['namedObservations'][0]['status'] = 'PASS'
        with self.assertRaises(ValueError):
            m.validate_saved(root, mutant)

    def test_observed_case_failure_cannot_be_downgraded_to_not_run(self):
        result = m.Assembly(HERE.parents[1]).assemble({'profiles': []})
        result['cases'] = [{'caseId': 'T01', 'subcaseId': 'one', 'profile': 'scenarios', 'status': 'FAIL'}]
        with self.assertRaises(ValueError):
            m.validate_manifest(result)

    def test_saved_manifest_detects_replaced_source_bytes(self):
        result = m.Assembly(HERE.parents[1]).assemble({'profiles': []})
        descriptor = self.write('input.json', {'fixed': True})
        result['codeCommit'] = COMMIT
        result['inputArtifacts'] = [descriptor]
        (self.root / 'input.json').write_text('{}')
        with self.assertRaises(ValueError):
            self.a.checked_descriptor(result['inputArtifacts'][0])


if __name__ == '__main__':
    unittest.main(verbosity=2)
