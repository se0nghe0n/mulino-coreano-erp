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
                    turns.append(dict(turnId=tid, selectedPathId=path, status='PASS', actualModelCalls=1,
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
