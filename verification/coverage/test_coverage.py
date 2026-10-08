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
        command = dict(argv=['./verify', 'deployment' if profile.endswith('-deployment') else profile, '--actual'], display='java acceptance', exitCode=0,
                       startedAt='2026-10-07T00:00:00Z', completedAt='2026-10-07T00:01:00Z')
        versions = dict(schema='ontology-v1', definition='definition-v1', evaluator='evaluator-v1', policy='synthetic-policy-v1',
                        tool='java21', db='postgres18', protocol='mcp-v1', client='client-v1', model='model-v1', prompt='prompt-v1', skill='skill-v1')
        report = dict(status='PASS', profile=profile, codeCommit=COMMIT, command=command['display'], exitCode=0,
                      executionIdentity=identity, gateComplete=True, discovered=1, started=1, completed=1, skipped=0, cases=[],
                      workingTreeDirty=False, explicitCaseSelection=False, timestamp='2026-10-07T00:01:00Z',
                      preRun=dict(codeCommit=COMMIT, workingTreeDirty=False, observedAt='2026-10-07T00:00:00Z'))
        tree = dict(before=dict(codeCommit=COMMIT, clean=True, observedAt='2026-10-07T00:00:00Z'),
                    after=dict(codeCommit=COMMIT, clean=True, observedAt='2026-10-07T00:01:00Z'))
        receipt = dict(schemaVersion='1.0.0', evidenceClass='ACTUAL', codeCommit=COMMIT, workingTreeClean=True, executionIdentity=identity, command=command, versions=versions,
                       inputs=[self.write('input.json', {'input': 'fixed'})], artifacts=[], reportArtifact=self.write('report.json', report),
                       workingTreeObservations=tree, buildIdentity=dict(commit=COMMIT, source='DECLARED_ACTUAL_BUILD_COMMIT'))
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

    def raw_capture_check(self, referenced, versions=None, case_versions=None):
        report, receipt, raw = self.protocol_fixture()
        capture = dict(self.write('evidence/read-1.json', {'httpStatus': 200}), scope={'actionId': 'read'}, completeness='COMPLETE', role='RAW_CAPTURE')
        if referenced:
            raw['observations'] = {'read': {'actionId': 'read', 'artifactRefs': ['evidence/read-1.json']}}
        receipt = copy.deepcopy(receipt)
        receipt['versions'].update(versions or {})
        raw['versions'] = receipt['versions']
        if case_versions is not None:
            receipt['caseVersions'] = case_versions
        receipt['reportArtifact'] = self.write('report.json', report)
        receipt['artifacts'] = [dict(self.write('raw.json', raw), scope=raw['scope'], completeness='COMPLETE'), capture]
        self.write('receipt.json', receipt)
        return self.a.receipt(report, 'receipt.json', 'schema', 'report.json')

    def test_raw_capture_is_bound_by_hash_and_must_be_referenced_by_an_envelope(self):
        # The Java producer binds adapter bytes as RAW_CAPTURE: they cannot carry the final command interval.
        self.assertIsNotNone(self.raw_capture_check(True))
        self.assertIsNone(self.raw_capture_check(False))
        self.assertTrue(any('Raw capture is not referenced' in p['reason'] for p in self.a.problems))

    def test_per_case_versions_need_exact_case_entries(self):
        self.assertIsNone(self.raw_capture_check(True, versions={'policy': 'PER_CASE'}))
        self.assertTrue(any('PER_CASE versions need' in p['reason'] for p in self.a.problems))
        entry = {'caseId': 'T01', 'subcaseId': 'only', 'versions': {'definition': 'd', 'evaluator': 'e', 'policy': 'p'}}
        self.assertIsNone(self.raw_capture_check(True, versions={'policy': 'PER_CASE'}, case_versions=[entry, entry]))
        self.assertIsNotNone(self.raw_capture_check(True, versions={'policy': 'PER_CASE'}, case_versions=[entry]))

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
        self.write('failure.json', {'status': 'FAIL', 'profile': 'schema', 'reason': 'Observed duplicate effect'})
        profiles = self.a.profiles({'profiles': [{'profile': 'schema', 'reportRef': 'failure.json', 'receiptRef': 'absent.json', 'evidenceClass': 'ACTUAL'}]})
        self.assertEqual('FAIL', profiles['schema']['status'])

    def reachability_problems(self, profiles):
        # One linked assertion; the oracle requires UNIT(contracts) and API/DB(scenarios).
        self.write('verification/cases/registry.json', {'expectedCases': 41, 'expectedSubcases': 1, 'cases': []})
        self.write('verification/cases/T04/case.json', {'caseId': 'T04', 'profiles': profiles, 'subcases': [{
            'id': 'one', 'fixtureRef': 'fixture.json', 'assertions': [{
                'id': 'a1', 'oracleRef': {'oracleId': 'T04.decimal-boundary', 'observationNames': ['invalid-decimals']}}]}]})
        self.write('fixture.json', {'baseRefs': []})
        observations = {('T04.decimal-boundary', 'invalid-decimals'): {
            'oracleId': 'T04.decimal-boundary', 'observationName': 'invalid-decimals', 'caseId': 'T04',
            'requiredProfiles': ['contracts', 'scenarios'], 'assertionLinks': [], 'status': 'NOT_RUN', 'expected': 'REJECTED'}}
        self.a.declarations(observations)
        return [p for p in self.a.preparation_problems if 'Unreachable required profile' in p['reason'] or 'unknown verification profile' in p['reason']]

    def test_required_profile_without_case_assertion_fails_preparation(self):
        problems = self.reachability_problems(['scenarios'])
        self.assertEqual(1, len(problems))
        self.assertEqual('FAIL', problems[0]['status'])
        self.assertIn('requires contracts', problems[0]['reason'])

    def test_every_required_profile_linked_has_no_reachability_problem(self):
        self.assertEqual([], self.reachability_problems(['contracts', 'scenarios']))

    def test_unknown_case_profile_cannot_satisfy_a_required_profile(self):
        problems = self.reachability_problems(['scenarios', 'contract'])
        self.assertTrue(any(p['status'] == 'FAIL' and 'unknown verification profile' in p['reason'] for p in problems))
        self.assertTrue(any('requires contracts' in p['reason'] for p in problems))

    def test_real_repository_reports_every_unreachable_required_profile(self):
        # Mutation over the real repository: dropping one declared case profile must surface.
        a = m.Assembly(HERE.parents[1], COMMIT)
        _, observations = a.catalog()
        a.declarations(observations)
        baseline = {p['reason'] for p in a.preparation_problems if 'Unreachable required profile' in p['reason']}
        self.assertFalse(any('case T04 ' in r or 'case T09 ' in r or 'case T12 ' in r or 'case T23 ' in r or 'case V8 ' in r for r in baseline))
        b = m.Assembly(HERE.parents[1], COMMIT)
        original = b.read
        def without_contracts(ref, preparation=False):
            value = original(ref, preparation)
            if ref == 'verification/cases/T09/case.json':
                value = dict(value, profiles=[p for p in value['profiles'] if p != 'contracts'])
            return value
        b.read = without_contracts
        _, observations = b.catalog()
        b.declarations(observations)
        mutated = {p['reason'] for p in b.preparation_problems if 'Unreachable required profile' in p['reason']}
        self.assertEqual(13, len([r for r in mutated - baseline if 'case T09 ' in r and 'requires contracts' in r]))

    def test_regulatory_review_observation_reachable_only_through_reviewed_receipt(self):
        a = m.Assembly(HERE.parents[1], COMMIT)
        _, observations = a.catalog()
        declarations = a.declarations(observations)
        self.assertFalse([p for p in a.preparation_problems if 'case T15 ' in p['reason']])
        regulatory = {d['subcaseId'] for d in declarations if d['caseId'] == 'T15' and d['profile'] == 'regulatory'}
        self.assertEqual({'missing-officialSource', 'missing-applicableDate', 'missing-reviewer'}, regulatory)
        review = dict(officialSourceRef='MFDS-notice-2026-01', jurisdiction='KR', applicableDate='2026-10-01',
                      reviewerId='qa-regulatory-reviewer', reviewedAt='2026-10-07T00:00:00Z', fictionalFixture=False)
        for mutant, accepted in [(None, False), (dict(review, reviewerId=''), False), (dict(review, fictionalFixture=True), False),
                                 (dict(review, officialSourceRef='synthetic-policy-v1'), False), (review, True)]:
            with self.subTest(review=mutant):
                report, receipt, raw = self.protocol_fixture('regulatory')
                if mutant is not None:
                    receipt['regulatoryReview'] = mutant
                self.assertEqual(accepted, self.receipt_check(report, receipt, raw, 'regulatory') is not None)

    def test_red_and_selftest_are_not_actual_profile_pass(self):
        self.write('pass.json', {'status': 'PASS', 'profile': 'schema', 'gateComplete': True})
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
            self.write('waiver.json', {'status': 'WAIVED', 'profile': profile, 'waiver': 'no account'})
            p = self.a.profiles({'profiles': [{'profile': profile, 'reportRef': 'waiver.json', 'evidenceClass': 'ACTUAL', 'receiptRef': 'missing.json'}]})
            self.assertEqual('NOT_RUN', p[profile]['status'])
            self.assertTrue(any(i['status'] == 'FAIL' and 'Waiver' in i['reason'] for i in self.a.problems))

    def test_unknown_and_duplicate_profiles_rejected(self):
        self.write('pass.json', {'status': 'NOT_RUN', 'profile': 'schema'})
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
        assertion = dict(assertionId='quantity', op='decimalEquals', expected='80', source=declaration['_sub']['assertions'][0]['source'], status='PASS', observed='80')
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

    def captured_run(self, observed_quantity, **record):
        declaration, case, fixture = self.declaration()
        report, receipt, raw = self.protocol_fixture()
        action = {'actionId': 'read', 'driverStatus': 'EXECUTED', 'response': {'quantity': observed_quantity},
                  'provenance': {'scopeComplete': True, 'source': 'ACTUAL_API'}, 'artifactRefs': ['raw.json']}
        assertion = dict(assertionId='quantity', op='decimalEquals', expected='80', source=declaration['_sub']['assertions'][0]['source'], status='PASS',
                         observed=observed_quantity)
        assertion.update(record)
        for key in [k for k, v in record.items() if v is None]:
            del assertion[key]
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
        self.a.run_case(declaration, {'schema': {'_report': report, '_receipt': checked, 'status': 'PASS'}})
        return declaration

    def provenance_run(self, provenance, kind='observe'):
        declaration, case, fixture = self.declaration()
        declaration['_sub']['actions'][0]['kind'] = kind
        report, receipt, raw = self.protocol_fixture()
        action = {'actionId': 'read', 'driverStatus': 'EXECUTED', 'response': {'quantity': '80'}, 'provenance': provenance, 'artifactRefs': ['raw.json']}
        assertion = dict(assertionId='quantity', op='decimalEquals', expected='80', source=declaration['_sub']['assertions'][0]['source'], status='PASS', observed='80')
        run = dict(caseId='T01', subcaseId='only', status='PASS', runtimeComplete=True, caseHash=case['sha256'], fixtureHash=fixture['sha256'],
                   versions=receipt['versions'], startedAt='2026-10-07T00:00:01Z', finishedAt='2026-10-07T00:00:03Z', actions={'read': action}, assertions=[assertion])
        report['cases'] = [run]
        raw['observations'] = {'read': copy.deepcopy(action)}
        raw['profileResult'] = copy.deepcopy(report)
        receipt['inputs'].extend([case, fixture])
        checked = self.receipt_check(report, receipt, raw)
        self.assertIsNotNone(checked)
        self.a.run_case(declaration, {'schema': {'_report': report, '_receipt': checked, 'status': 'PASS'}})
        return declaration

    def test_actual_observe_snapshot_is_not_a_selftest_marker(self):
        # step2-closure P2: every real observe provenance carries snapshot.capturedAt; the marker check reads labels only.
        snapshot = {'id': '811:811:', 'isolation': 'REPEATABLE_READ', 'capturedAt': '2026-10-07T00:00:02Z', 'artifactRef': 'raw.json',
                    'readMode': 'CURRENT_COMMITTED'}
        query = {'statementId': 's1-physical-segments-v1', 'sql': 'SELECT * FROM segments', 'parameters': {}, 'mappingVersion': '1.0.0'}
        actual = {'adapter': 'actual-http-jdbc', 'adapterVersion': '2.0.0', 'buildVersion': COMMIT, 'source': 'POSTGRESQL_JDBC', 'independent': True,
                  'scopeComplete': True, 'sourceQuery': query, 'snapshot': snapshot}
        self.assertIsNone(m.provenance_marker(actual))
        self.assertEqual('PASS', self.provenance_run(actual)['status'])
        for label, value in [('source', 'CANNED_CONTRACT_SELFTEST'), ('adapter', 'V8-lock-SELFTEST'), ('buildVersion', 'stub-build')]:
            with self.subTest(label=label):
                self.a = m.Assembly(self.root, COMMIT)
                self.assertEqual('FAIL', self.provenance_run(dict(actual, **{label: value}))['status'])
                self.assertTrue(any('Canned/stub action' in p['reason'] for p in self.a.problems), self.a.problems)
        self.a = m.Assembly(self.root, COMMIT)
        self.assertEqual('FAIL', self.provenance_run(dict(actual, snapshot=dict(snapshot, isolation='SELFTEST_CAPTURED')))['status'])

    def test_selection_contract_violation_contradicts_a_runner_pass(self):
        # A regressed evaluator that treats a missing filter field as non-matching reports count 0 (vacuous no-effect PASS).
        results = {'x': {'data': {'rows': [{'quantity': '5'}], 'v': None}}}
        count = dict(op='count', source=dict(actionId='x', pointer='/data/rows', where={'status': 'APPLIED'}), expected=0)
        self.assertIs(False, m.independent_verdict(count, results))
        self.assertIn('filter field missing', m.record_problem(count, dict(op='count', where={'status': 'APPLIED'}, observed=[]), results))
        field = dict(op='sumEquals', source=dict(actionId='x', pointer='/data/rows', field='committed'), expected='0')
        self.assertIs(False, m.independent_verdict(field, results))
        for pointer in ('/data/v', '/data/missing'):
            with self.subTest(pointer=pointer):
                self.assertIs(False, m.independent_verdict(dict(op='present', source=dict(actionId='x', pointer=pointer), expected=True), results))
        # Undecidable inputs stay undecidable: no captured result, or an alias the bytes cannot bind.
        self.assertIsNone(m.independent_verdict(dict(count, source=dict(count['source'], actionId='absent')), results))
        self.assertIsNone(m.independent_verdict(dict(count, source=dict(count['source'], where={'status': {'$alias': 'A'}})), results))

    def test_profile_pass_needs_matching_profile_and_every_declared_subcase(self):
        report, receipt, raw = self.protocol_fixture('schema')
        report['cases'] = [{'caseId': 'T01', 'subcaseId': 'only'}]
        raw['profileResult'] = report
        self.receipt_check(report, receipt, raw)
        entry = [{'profile': 'schema', 'reportRef': 'report.json', 'receiptRef': 'receipt.json', 'evidenceClass': 'ACTUAL'}]
        one = [{'caseId': 'T01', 'subcaseId': 'only', 'profile': 'schema'}]
        self.assertEqual('PASS', self.a.profiles({'profiles': entry}, one)['schema']['status'])
        two = one + [{'caseId': 'T02', 'subcaseId': 'other', 'profile': 'schema'}]
        partial = self.a.profiles({'profiles': entry}, two)['schema']
        self.assertEqual('NOT_RUN', partial['status'])
        self.assertIn('1 declared schema subcases did not run', partial['missingReason'])
        for change in ({'explicitCaseSelection': True}, {'explicitCaseSelection': None}):
            with self.subTest(change=change):
                mutated = dict(report, **change)
                raw['profileResult'] = mutated
                self.receipt_check(mutated, receipt, raw)
                self.assertEqual('NOT_RUN', self.a.profiles({'profiles': entry}, one)['schema']['status'])
        other = dict(report, profile='contracts')
        raw['profileResult'] = other
        self.receipt_check(other, receipt, raw)
        self.a = m.Assembly(self.root, COMMIT)
        self.assertEqual('NOT_RUN', self.a.profiles({'profiles': entry}, one)['schema']['status'])
        self.assertTrue(any('produced for profile contracts' in p['reason'] for p in self.a.problems))

    def test_receipt_needs_named_profile_and_clean_tree_before_and_after(self):
        for mutate, reason in [
                (lambda r, c: c['command'].update(argv=['./verify', 'contracts', '--actual']), 'does not name profile'),
                (lambda r, c: r['preRun'].update(workingTreeDirty=True), 'before and after'),
                (lambda r, c: r.pop('preRun'), 'before and after'),
                (lambda r, c: r['preRun'].update(codeCommit='b' * 40), 'before and after'),
                (lambda r, c: c['workingTreeObservations']['before'].update(clean=False), 'before and after'),
                (lambda r, c: c.pop('workingTreeObservations'), 'before and after'),
                (lambda r, c: r.update(workingTreeDirty=True), 'before and after')]:
            with self.subTest(reason=reason):
                self.a = m.Assembly(self.root, COMMIT)
                report, receipt, raw = self.protocol_fixture()
                mutate(report, receipt)
                raw['profileResult'] = report
                raw['command'] = receipt['command']
                self.assertIsNone(self.receipt_check(report, receipt, raw))
                self.assertTrue(any(reason in p['reason'] for p in self.a.problems), self.a.problems)

    def test_regulatory_profile_is_a_named_not_run_gate(self):
        record = self.a.profiles({})['regulatory']
        self.assertEqual('NOT_RUN', record['status'])
        self.assertTrue(record['missingReason'].startswith('NOT_RUN_GATED: no regulatory runner'))

    def test_layer_route_known_open_marks_observation_and_stale_entry_fails_preparation(self):
        real = HERE.parents[1]
        a = m.Assembly(real, COMMIT)
        _, observations = a.catalog()
        self.assertEqual([], a.layer_routes(observations), 'no KNOWN_OPEN layer gap remains in the repository')
        self.assertFalse([p for p in a.preparation_problems if 'Layer route' in p['reason']])
        self.assertFalse({k for k, o in observations.items() if o.get('layerRouteGaps')})
        # Counterexample copy: drop the MCP leg of V4 mixed-atomic-batch so the gap exists again.
        catalog = json.loads((real / 'verification/requirements/mandatory-oracles.json').read_text())
        for case_id in catalog['requiredCaseIds']:
            (self.root / f'verification/cases/{case_id}').mkdir(parents=True, exist_ok=True)
            case = json.loads((real / f'verification/cases/{case_id}/case.json').read_text())
            for sub in case['subcases']:
                if sub['id'] == 'mixed-atomic-batch':
                    sub['actions'] = [x for x in sub['actions'] if not x['id'].startswith('mcp-batch')]
                    sub['assertions'] = [x for x in sub['assertions'] if not x['id'].startswith('mcp-batch-')]
            (self.root / f'verification/cases/{case_id}/case.json').write_text(json.dumps(case))
        (self.root / 'verification/requirements').mkdir(parents=True, exist_ok=True)
        for name in ('mandatory-oracles.json', 'check_layer_routes.py'):
            (self.root / 'verification/requirements' / name).write_bytes((real / 'verification/requirements' / name).read_bytes())
        review = json.loads((real / 'verification/requirements/layer-route-review.json').read_text())
        known = dict(status='KNOWN_OPEN', oracleId='V4.all-alternate-write-paths', observationName='mixed-batch-allowed-partial-effects',
                     layer='MCP', owner='V4 case owner (counterexample)', reason='MCP leg removed in this copy', closeWhen='MCP leg restored')
        target = ('V4.all-alternate-write-paths', 'mixed-batch-allowed-partial-effects')
        for entries, expect in [(review['entries'] + [known], 'known'), (review['entries'], 'unexplained'),
                                (review['entries'] + [known, dict(known, observationName='closed-gap')], 'stale')]:
            with self.subTest(expect=expect):
                (self.root / 'verification/requirements/layer-route-review.json').write_text(json.dumps(dict(review, entries=entries)))
                b = m.Assembly(self.root, COMMIT)
                copied = copy.deepcopy(observations)
                gaps = b.layer_routes(copied)
                reasons = [p['reason'] for p in b.preparation_problems if p['status'] == 'FAIL']
                if expect == 'known':
                    self.assertEqual([], reasons)
                    self.assertEqual([target], [k for k, o in copied.items() if o.get('layerRouteGaps')])
                    self.assertEqual(['layer-routes'], [g['check'] for g in gaps])
                elif expect == 'unexplained':
                    self.assertTrue(any('unexplained layer route gap V4' in r for r in reasons), reasons)
                else:
                    self.assertTrue(any('stale layer-route review entry' in r for r in reasons), reasons)

    def test_vocabulary_problems_fail_preparation_and_pending_entries_are_listed(self):
        real = HERE.parents[1]
        a = m.Assembly(real, COMMIT)
        gaps = a.vocabulary()
        self.assertFalse([p for p in a.preparation_problems if 'vocabulary' in p['reason']])
        self.assertTrue(gaps and all(g['check'] == 'vocabulary' and g['owner'] for g in gaps))
        # Counterexample copy: a retired audit field name in one case is a preparation failure.
        (self.root / 'verification/cases').mkdir(parents=True, exist_ok=True)
        for ref in ('verification/cases/check_vocabulary.py', 'contracts/domain-vocabulary.json', 'contracts/audit-observation-fields.json'):
            (self.root / ref).parent.mkdir(parents=True, exist_ok=True)
            (self.root / ref).write_bytes((real / ref).read_bytes())
        case = {'caseId': 'X01', 'subcases': [{'id': 's', 'actions': [{'id': 'db', 'kind': 'observe'}], 'assertions': [
            {'id': 'a', 'op': 'count', 'source': {'actionId': 'db', 'pointer': '/data/rawRows/audit', 'where': {'commandKey': 'k'}}, 'expected': 1}]}]}
        (self.root / 'verification/cases/X01').mkdir(parents=True, exist_ok=True)
        (self.root / 'verification/cases/X01/case.json').write_text(json.dumps(case))
        b = m.Assembly(self.root, COMMIT)
        b.vocabulary()
        reasons = [p['reason'] for p in b.preparation_problems if p['status'] == 'FAIL']
        self.assertTrue(any('commandKey' in r for r in reasons), reasons)

    def test_runner_pass_is_rechecked_against_captured_bytes(self):
        self.assertEqual('PASS', self.captured_run('80')['status'])
        self.a = m.Assembly(self.root, COMMIT)
        declaration = self.captured_run('100')  # consistent capture, but the runner claimed PASS for 80
        self.assertEqual('FAIL', declaration['status'])
        self.assertTrue(any('contradicts independent re-evaluation' in p['reason'] for p in self.a.problems))

    def test_runtime_record_must_restate_operator_and_projected_value(self):
        # coverage-08: the runner record carries op/unit/where/field and the post-projection compared value.
        for record, reason in [(dict(op=None), 'operator differs'), (dict(op='decimalAtLeast'), 'operator differs'),
                               (dict(observed=None), 'lacks the compared observed value'), (dict(observed='79'), 'recorded observed differs'),
                               (dict(unit='BOX'), 'unit differs'), (dict(where={'k': 'A'}), 'where/field differs')]:
            with self.subTest(record=record):
                self.a = m.Assembly(self.root, COMMIT)
                declaration = self.captured_run('80', **record)
                self.assertEqual('FAIL', declaration['status'])
                self.assertTrue(any(reason in p['reason'] for p in self.a.problems), self.a.problems)

    def test_recorded_values_decide_when_captured_bytes_cannot(self):
        declared = dict(op='decimalEquals', source=dict(actionId='x', pointer='/data/rows', where={'k': {'$alias': 'A'}}, field='q'), expected='80')
        results = {'x': {'data': {'rows': [{'k': 'id-a', 'q': '80'}]}}}
        self.assertIsNone(m.independent_verdict(declared, results))
        self.assertIs(False, m.recorded_verdict(declared, dict(observed=['80'])))  # list is not one decimal
        self.assertIs(True, m.independent_verdict(dict(declared, op='sumEquals'), results, {'A': 'id-a'}))
        self.assertIs(False, m.independent_verdict(dict(declared, op='sumEquals'), results, {'A': 'other'}))
        self.assertIs(False, m.recorded_verdict(dict(declared, op='sumEquals'), dict(observed=['70'])))
        self.assertIn('differs', m.record_problem(dict(declared, op='sumEquals'), dict(op='sumEquals', where=declared['source']['where'], field='q', observed=[]),
                                                   results, {'A': 'id-a'}))

    def test_independent_verdict_projection_ops_and_undecidable_references(self):
        results = {'x': {'data': {'rows': [{'k': 'A', 'q': '80', 'u': 'BOX'}, {'k': 'B', 'q': '20', 'u': 'BOX'}], 'v': '80', 'n': None}}}
        src = lambda pointer, **extra: dict(actionId='x', pointer=pointer, **extra)
        cases = [
            (dict(op='count', source=src('/data/rows', where={'k': 'A'}), expected=1), True),
            (dict(op='count', source=src('/data/rows', where={'k': 'A'}), expected=2), False),
            (dict(op='sumEquals', source=src('/data/rows', field='q'), expected='100'), True),
            (dict(op='sumEquals', source=src('/data/rows', field='q'), expected='90'), False),
            (dict(op='exactSet', source=src('/data/rows', field=['k', 'q']), expected=[['B', '20'], ['A', '80']]), True),
            (dict(op='exactSet', source=src('/data/rows', field=['k', 'q']), expected=[['A', '80']]), False),
            (dict(op='decimalEquals', source=src('/data/v'), expected='80.0', unit='BOX', unitSource=src('/data/rows', field='u')), True),
            (dict(op='decimalEquals', source=src('/data/v'), expected='80', unit='EA', unitSource=src('/data/rows', field='u')), False),
            (dict(op='decimalAtMost', source=src('/data/v'), expected='79'), False),
            (dict(op='absent', source=src('/data/n'), expected=True), False),
            (dict(op='absent', source=src('/data/missing'), expected=True), True),
            (dict(op='equals', source=src('/data/v'), expected={'$result': {'actionId': 'x', 'pointer': '/data/v'}}), True),
            (dict(op='equals', source=src('/data/v'), expected={'$result': {'actionId': 'missing', 'pointer': '/data/v'}}), None),
            (dict(op='count', source=src('/data/rows', where={'k': {'$alias': 'A'}}), expected=1), None),
            (dict(op='timeEquals', source=src('/data/v'), expected='80'), None),
        ]
        for declared, verdict in cases:
            with self.subTest(declared=declared):
                self.assertEqual(verdict, m.independent_verdict(declared, results))

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
            binding = json.loads((HERE.parents[1] / f'verification/model-binding/cases/{case["id"]}/binding.json').read_text())
            turns = binding['turns']
            ref = f'verification/model-binding/cases/{case["id"]}/binding.json'
            entries.append({'caseId': case['id'], 'bindingRef': ref, 'turnIds': [t['id'] for t in turns]})
            descriptors += [self.write(ref, binding),
                            self.write(ref.replace('binding.json', 'scenario.feature'), {'evidenceClass': 'SELFTEST'}),
                            self.write(ref.replace('binding.json', 'fixture.json'), {'synthetic': True})]
        registry = {'plannedRepeats': 3, 'cases': entries}
        paths = sorted({a['path'] for c in corpus['cases'] for t in c['turns'] for a in t['oracle']['assertions']})
        descriptors += [ch, self.write('verification/model-binding/registry.json', registry),
                        self.write('verification/model-binding/semantic-paths.json', {'corpusSha256': ch['sha256'], 'paths': [{'semanticPath': p, 'common': True} for p in paths]})]
        return corpus, registry, {'inputArtifacts': descriptors}

    def model_runtime_fixture(self, selected='SERVER_REJECTION'):
        # SELFTEST protocol only, no real provider/model execution or product gate.
        corpus, registry, prep = self.binding_fixture()
        prep.update(corpusSha256=self.a.descriptor('verification/model-corpus/corpus.json')['sha256'],
                    bindingRegistrySha256=self.a.descriptor('verification/model-binding/registry.json')['sha256'],
                    caseCount=60, turnCount=73, semanticPathCount=154, preparationStatus='PREPARED')
        self.write('verification/harness/target/evidence/model-binding-preparation.json', prep)
        self.a.model_bindings(corpus, registry, prep)
        attempts = []
        for entry in registry['cases']:
            for repeat in range(1, 4):
                turns = []
                for tid in entry['turnIds']:
                    binding = self.a.model_turn_bindings[(entry['caseId'], tid)]
                    path = selected if selected in binding['paths'] else 'DIRECT'
                    assertions = dict(binding['common'], **binding['paths'][path])
                    metric = dict(usage=dict(inputTokens=10, outputTokens=2), cost=dict(amount='0.01', currency='USD', pricingRef='pricing-v1'))
                    call = dict(callId=f'{entry["caseId"]}/{repeat}/{tid}', caseId=entry['caseId'], repeat=repeat,
                                turnId=tid, provider='test-provider', model='model-v1', artifactRefs=['raw.json'], **copy.deepcopy(metric))
                    turns.append(dict(turnId=tid, selectedPathId=path, status='PASS', actualModelCalls=1, intentMatch=True,
                                      assertionResults=[dict(assertionId=aid, semanticPath=b['semanticPath'], status='PASS') for aid, b in assertions.items()],
                                      modelCalls=[call], **metric))
                n = len(turns)
                attempts.append(dict(caseId=entry['caseId'], repeat=repeat, status='PASS', skipped=0, artifactRefs=['raw.json'],
                                     actualModelCalls=n, turnResults=turns, usage=dict(inputTokens=n*10, outputTokens=n*2),
                                     cost=dict(amount=str(n/100), currency='USD', pricingRef='pricing-v1')))
        runtime = dict(status='PASS', actualModelCalls=219, attempts=attempts,
                       usage=dict(inputTokens=2190, outputTokens=438), cost=dict(amount='2.19', currency='USD', pricingRef='pricing-v1'))
        receipt = dict(_artifactPaths={'raw.json'}, _artifactDocuments=[{'modelAttempts': copy.deepcopy(attempts)}], versions={'model': 'model-v1'})
        return runtime, receipt

    def model_check(self, runtime, receipt, recapture=True):
        if recapture:
            receipt['_artifactDocuments'] = [{'modelAttempts': copy.deepcopy(runtime['attempts'])}]
        return self.a.model({}, {'model': {'status': 'PASS', '_report': runtime, '_receipt': receipt}})

    def structured_turns(self, runtime, corpus_status):
        corpus = json.loads((HERE.parents[1] / 'verification/model-corpus/corpus.json').read_text())
        status = {(c['id'], f'turn-{i}'): t['expectedIntent']['status'] for c in corpus['cases'] for i, t in enumerate(c['turns'], 1)}
        return [t for a in runtime['attempts'] for t in a['turnResults'] if status[(a['caseId'], t['turnId'])] == corpus_status]

    def test_clear_structured_intent_rate_uses_every_structured_turn_and_95_percent(self):
        runtime, receipt = self.model_runtime_fixture()
        model = self.model_check(runtime, receipt)
        self.assertEqual('PASS', model['status'])
        self.assertEqual(dict(matched=186, total=186, rate='1.0000', minimumRatio='0.95'), model['clearStructuredIntent'])
        structured = self.structured_turns(runtime, 'STRUCTURED')
        self.assertEqual(186, len(structured))  # 62 STRUCTURED turns x3, not only the 20 clear_synonyms cases
        for turn in structured[:9]:
            turn['intentMatch'] = False
        self.a = m.Assembly(self.root, COMMIT)
        model = self.model_check(runtime, receipt)
        self.assertEqual('PASS', model['status'], 'a 177/186 structuring rate meets the proposal')
        structured[9]['intentMatch'] = False
        self.a = m.Assembly(self.root, COMMIT)
        model = self.model_check(runtime, receipt)
        self.assertEqual('FAIL', model['status'], '176/186 is below 0.95')

    def test_ambiguous_turn_mismatch_and_missing_intent_metric(self):
        runtime, receipt = self.model_runtime_fixture()
        self.structured_turns(runtime, 'NEEDS_INPUT')[0]['intentMatch'] = False
        self.assertEqual('FAIL', self.model_check(runtime, receipt)['status'])
        runtime, receipt = self.model_runtime_fixture()
        del self.structured_turns(runtime, 'STRUCTURED')[0]['intentMatch']
        self.a = m.Assembly(self.root, COMMIT)
        model = self.model_check(runtime, receipt)
        self.assertEqual('NOT_RUN', model['status'])
        self.assertIsNone(model['clearStructuredIntent'])

    def test_full_common_plus_selected_negative_path_protocol_is_accepted(self):
        for selected in ['SERVER_REJECTION', 'EVIDENCED_PREFLIGHT_STOP']:
            with self.subTest(selected=selected):
                runtime, receipt = self.model_runtime_fixture(selected)
                self.assertEqual('PASS', self.model_check(runtime, receipt)['status'])
                turn = next(a for a in runtime['attempts'] if a['caseId'] == 'M47')['turnResults'][0]
                self.assertEqual(5 if selected == 'SERVER_REJECTION' else 3,
                                 sum('/common-' not in a['assertionId'] for a in turn['assertionResults']))

    def test_selected_path_missing_extra_wrong_duplicate_and_alias_conflicts_fail(self):
        mutations = [lambda t: t['assertionResults'].pop(),
                     lambda t: t['assertionResults'].append(dict(assertionId='extra', semanticPath='response.errorCode', status='PASS')),
                     lambda t: t.update(selectedPathId='DIRECT'),
                     lambda t: t.update(selectedPath='EVIDENCED_PREFLIGHT_STOP'),
                     lambda t: t['assertionResults'].append(copy.deepcopy(t['assertionResults'][0])),
                     lambda t: t['assertionResults'][0].update(semanticPath='wrong.path')]
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                runtime, receipt = self.model_runtime_fixture()
                turn = next(a for a in runtime['attempts'] if a['caseId'] == 'M47')['turnResults'][0]
                mutate(turn)
                self.assertEqual('FAIL', self.model_check(runtime, receipt)['status'])

    def test_runner_selected_path_alias_and_server_fallback_are_supported(self):
        runtime, receipt = self.model_runtime_fixture()
        for attempt in runtime['attempts']:
            for turn in attempt['turnResults']:
                turn['selectedPath'] = turn.pop('selectedPathId')
        self.assertEqual('PASS', self.model_check(runtime, receipt)['status'])
        fallback = next(a for a in runtime['attempts'] if a['caseId'] == 'M44')['turnResults'][0]
        self.assertTrue(any('/SIT_DIRECT_COMMAND-' in a['assertionId'] for a in fallback['assertionResults']))

    def test_model_usage_cost_and_identity_mutations_fail(self):
        mutations = [lambda r,a,t,c: c['usage'].update(inputTokens=-1),
                     lambda r,a,t,c: c['usage'].update(outputTokens='2'),
                     lambda r,a,t,c: c['usage'].update(inputTokens=True),
                     lambda r,a,t,c: c['usage'].update(inputTokens=0),
                     lambda r,a,t,c: c['cost'].update(amount='-0.01'),
                     lambda r,a,t,c: c['cost'].update(amount='NaN'),
                     lambda r,a,t,c: c['cost'].update(amount='unknown'),
                     lambda r,a,t,c: c['cost'].update(currency='usd'),
                     lambda r,a,t,c: c['cost'].update(pricingRef=''),
                     lambda r,a,t,c: c.update(turnId='another-turn'),
                     lambda r,a,t,c: c.update(repeat=2),
                     lambda r,a,t,c: c.update(repeat=True),
                     lambda r,a,t,c: c.update(model='other-model'),
                     lambda r,a,t,c: c.update(callId=r['attempts'][1]['turnResults'][0]['modelCalls'][0]['callId']),
                     lambda r,a,t,c: t['usage'].update(inputTokens=11),
                     lambda r,a,t,c: a['cost'].update(amount='1'),
                     lambda r,a,t,c: r['usage'].update(outputTokens=0),
                     lambda r,a,t,c: r['cost'].update(amount='0'),
                     lambda r,a,t,c: r['cost'].update(pricingRef='other-price'),
                     lambda r,a,t,c: t.update(actualModelCalls=2),
                     lambda r,a,t,c: a.update(actualModelCalls=0),
                     lambda r,a,t,c: r.update(actualModelCalls=220)]
        runtime, receipt = self.model_runtime_fixture()
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                changed = copy.deepcopy(runtime)
                attempt = changed['attempts'][0]; turn = attempt['turnResults'][0]; call = turn['modelCalls'][0]
                mutate(changed, attempt, turn, call)
                self.assertEqual('FAIL', self.model_check(changed, receipt)['status'])

    def test_missing_provider_usage_stays_not_run_and_failed_assertion_stays_fail(self):
        runtime, receipt = self.model_runtime_fixture()
        runtime['attempts'][0]['turnResults'][0]['modelCalls'][0]['usage'] = None
        self.assertEqual('NOT_RUN', self.model_check(runtime, receipt)['status'])
        runtime['attempts'][0]['turnResults'][0]['assertionResults'][0]['status'] = 'FAIL'
        self.assertEqual('FAIL', self.model_check(runtime, receipt)['status'])

    def test_model_capture_bytes_cannot_be_replaced_by_report_metadata(self):
        runtime, receipt = self.model_runtime_fixture()
        receipt['_artifactDocuments'][0]['modelAttempts'][0]['usage']['inputTokens'] += 1
        self.assertEqual('NOT_RUN', self.model_check(runtime, receipt, recapture=False)['status'])

    def test_missing_provider_calls_and_identity_are_not_filled_with_zero(self):
        runtime, receipt = self.model_runtime_fixture()
        runtime['attempts'][0]['turnResults'][0].pop('modelCalls')
        self.assertEqual('NOT_RUN', self.model_check(runtime, receipt)['status'])
        runtime, receipt = self.model_runtime_fixture()
        runtime['attempts'][0]['turnResults'][0]['modelCalls'][0].pop('provider')
        self.assertEqual('NOT_RUN', self.model_check(runtime, receipt)['status'])

    def test_negative_branch_binding_pointers_cannot_be_changed_with_fresh_hash(self):
        corpus, registry, report = self.binding_fixture()
        ref = 'verification/model-binding/cases/M47/binding.json'
        binding = json.loads((self.root / ref).read_text())
        branch = binding['turns'][0]['oracleAssertions']['SERVER_REJECTION']
        branch[0]['corpusAssertionPointer'] = binding['turns'][0]['commonAssertions'][0]['corpusAssertionPointer']
        descriptor = self.write(ref, binding)
        report['inputArtifacts'] = [descriptor if d['path'] == ref else d for d in report['inputArtifacts']]
        self.a.model_bindings(corpus, registry, report)
        self.assertTrue(any('Selected-path corpus assertion pointers' in p['reason'] for p in self.a.preparation_problems))

    def test_missing_metric_cannot_hide_an_observed_invalid_metric(self):
        for record in [dict(usage=None, cost=dict(amount='-1', currency='USD', pricingRef='pricing-v1')),
                       dict(usage=dict(inputTokens=-1), cost=None),
                       dict(usage=dict(inputTokens='unknown', outputTokens=0), cost=None)]:
            with self.subTest(record=record), self.assertRaises(ValueError):
                m.model_validation.metrics(record)

    def test_decimal_cost_sum_is_exact_beyond_default_precision(self):
        amount = '123456789012345678901234567890.123456789'
        leaf = dict(usage=dict(inputTokens=1, outputTokens=0), cost=dict(amount=amount, currency='USD', pricingRef='price'))
        total = dict(usage=dict(inputTokens=2, outputTokens=0), cost=dict(amount='246913578024691357802469135780.246913578', currency='USD', pricingRef='price'))
        self.assertTrue(m.model_validation.aggregate(total, [leaf, leaf]))

    def test_small_numeric_provider_cost_is_not_rejected_as_nonnumeric(self):
        metric = dict(usage=dict(inputTokens=1, outputTokens=0), cost=dict(amount=0.00000001, currency='USD', pricingRef='price'))
        self.assertEqual('1E-8', str(m.model_validation.metrics(metric)[2]))

    def test_wrong_or_duplicate_model_turn_identity_fails(self):
        for duplicate in [False, True]:
            runtime, receipt = self.model_runtime_fixture()
            turns = runtime['attempts'][0]['turnResults']
            if duplicate:
                turns.append(copy.deepcopy(turns[0]))
            else:
                turns[0]['turnId'] = 'unknown-turn'
            self.assertEqual('FAIL', self.model_check(runtime, receipt)['status'])

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

    def copy_catalog_inputs(self):
        repository = HERE.parents[1]
        catalog = json.loads((repository / 'verification/requirements/mandatory-oracles.json').read_text())
        refs = ['verification/requirements/mandatory-oracles.json', 'verification/requirements/normative-contract-lock.json',
                'verification/requirements/validate_catalog.py', 'verification/requirements/mandatory-oracles.schema.json']
        refs += [d['path'] for d in catalog['sourceFiles']]
        for ref in refs:
            target = self.root / ref
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes((repository / ref).read_bytes())
        return catalog

    def test_empty_null_or_non_object_lock_cannot_disable_contract_lock(self):
        for lock in [{}, None, [], 0, {'oracleContracts': {}}]:
            with self.subTest(lock=lock):
                self.a = m.Assembly(self.root, COMMIT)
                catalog = self.copy_catalog_inputs()
                e1 = next(o for o in catalog['oracles'] if o['oracleId'] == 'E1.full-flow-quantities')
                held = next(o for o in e1['expectedObservations'] if o['type'] == 'quantity' and o['expected']['value'] == '80')
                held['expected']['value'] = '100'
                self.write('verification/requirements/mandatory-oracles.json', catalog)
                (self.root / 'verification/requirements/normative-contract-lock.json').write_text(json.dumps(lock))
                self.a.catalog()
                self.assertTrue(any(p['status'] == 'FAIL' and 'normative catalog' in p['reason'] for p in self.a.preparation_problems),
                                self.a.preparation_problems)

    def test_missing_lock_is_not_run_not_pass(self):
        self.copy_catalog_inputs()
        (self.root / 'verification/requirements/normative-contract-lock.json').unlink()
        self.a.catalog()
        self.assertTrue(any(p['status'] == 'NOT_RUN' and 'normative-contract-lock' in p['reason'] for p in self.a.preparation_problems))

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

    def test_regenerated_model_corpus_weakening_fails_model_preparation(self):
        repository = HERE.parents[1]
        for ref in ['verification/model-corpus/corpus.json', 'verification/model-corpus/corpus.schema.json',
                    'verification/model-corpus/validate.py', 'verification/requirements/normative-contract-lock.json']:
            target = self.root / ref
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes((repository / ref).read_bytes())
        self.a.model({}, {})
        self.assertFalse([p for p in self.a.preparation_problems if 'Model corpus' in p['reason']])
        corpus = json.loads((self.root / 'verification/model-corpus/corpus.json').read_text())
        case = next(c for c in corpus['cases'] if c['id'] == 'M16')
        next(a for t in case['turns'] for a in t['oracle']['assertions'] if a['path'] == 'response.onHand.value')['expected'] = '100'
        self.write('verification/model-corpus/corpus.json', corpus)
        self.a = m.Assembly(self.root, COMMIT)
        self.a.model({}, {})
        self.assertTrue(any(p['status'] == 'FAIL' and 'reviewed pin' in p['reason'] for p in self.a.preparation_problems))

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

    def consistent_not_run_manifest(self):
        # Mutation tests below need a NOT_RUN manifest. Repository-state preparation FAILs, such as
        # cross-owner unreachable required profiles, are asserted by their own tests.
        result = copy.deepcopy(m.Assembly(HERE.parents[1]).assemble({'profiles': []}))
        result['preparationProblems'] = [p for p in result['preparationProblems'] if p['status'] != 'FAIL']
        result['coverageProblems'] = [p for p in result['coverageProblems'] if p['status'] != 'FAIL']
        result.update(status='NOT_RUN', exitCode=2, runtimeStatus='NOT_RUN', artifactCoverageStatus='NOT_RUN',
                      preparationStatus='NOT_RUN' if result['preparationStatus'] == 'FAIL' else result['preparationStatus'])
        return m.validate_manifest(result)

    def test_real_baseline_absence_is_complete_499_not_run_inventory(self):
        root = HERE.parents[1]
        result = m.Assembly(root).assemble({'profiles': []})
        m.validate_manifest(result)
        self.assertEqual(122, result['oracleCount'])
        self.assertEqual(499, result['observationCount'])
        # No runtime evidence: NOT_RUN, or FAIL only from observed preparation defects.
        self.assertIn(result['status'], ('NOT_RUN', 'FAIL'))
        self.assertEqual(result['status'] == 'FAIL', any(p['status'] == 'FAIL' for p in result['preparationProblems'] + result['coverageProblems']))
        self.assertFalse(result['gateComplete'])
        self.assertFalse(result['productRuntimeClaimed'])
        self.assertEqual('NOT_RUN', result['model']['status'])
        # The no-case baseline is expected only at B2; completed future case files need not stay absent.
        self.assertTrue(all(o['status'] == 'NOT_RUN' for o in result['namedObservations']))

    def test_forged_summary_completion_and_downgrade_rejected(self):
        result = self.consistent_not_run_manifest()
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

    def test_dirty_working_tree_cannot_identify_a_pass(self):
        report, receipt, raw = self.protocol_fixture()
        for value in (False, None):
            with self.subTest(workingTreeClean=value):
                mutant = copy.deepcopy(receipt)
                if value is None:
                    del mutant['workingTreeClean']
                else:
                    mutant['workingTreeClean'] = value
                self.assertIsNone(self.receipt_check(report, mutant, raw))
        result = self.consistent_not_run_manifest()
        forged = dict(copy.deepcopy(result), status='PASS', exitCode=0, gateComplete=True, runtimeComplete=True,
                      productRuntimeClaimed=True, workingTreeDirty=True)
        with self.assertRaisesRegex(ValueError, 'dirty'):
            m.validate_manifest(forged)
        dirty = m.Assembly(HERE.parents[1], COMMIT, working_tree_dirty=True).assemble({'profiles': []})
        self.assertTrue(any(p['status'] == 'NOT_RUN' and 'dirty' in p['reason'] for p in dirty['coverageProblems']))
        self.assertFalse(dirty['gateComplete'])

    def test_saved_manifest_cannot_hide_working_tree_state(self):
        root = HERE.parents[1]
        result = m.Assembly(root).assemble({'profiles': []})
        mutant = copy.deepcopy(result)
        mutant['workingTreeDirty'] = not result['workingTreeDirty']
        with self.assertRaisesRegex(ValueError, 'working tree'):
            m.validate_saved(root, mutant)

    def test_saved_manifest_reassembly_rejects_forged_observation_status(self):
        root = HERE.parents[1]
        result = m.Assembly(root).assemble({'profiles': []})
        m.validate_saved(root, result)
        mutant = copy.deepcopy(result)
        mutant['namedObservations'][0]['status'] = 'PASS'
        with self.assertRaises(ValueError):
            m.validate_saved(root, mutant)

    def test_observed_case_failure_cannot_be_downgraded_to_not_run(self):
        result = self.consistent_not_run_manifest()
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
