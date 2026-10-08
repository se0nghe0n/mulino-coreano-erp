#!/usr/bin/env python3
"""Read-only evidence assembly. No product adapter, model call, deployment or waiver."""
import argparse
import datetime as dt
import decimal
import hashlib
import importlib.util
import json
import pathlib
import platform
import re
import subprocess
import sys

_model_spec = importlib.util.spec_from_file_location('coverage_model_validation', pathlib.Path(__file__).with_name('model_validation.py'))
model_validation = importlib.util.module_from_spec(_model_spec)
_model_spec.loader.exec_module(model_validation)

CASE_IDS = [f'T{i:02}' for i in range(1, 27)] + [f'C{i}' for i in range(1, 6)] + [f'V{i}' for i in range(1, 9)] + ['E1', 'E2']
PROFILES = ['schema', 'contracts', 'scenarios', 'recovery', 'mcp', 'skills', 'model', 'local-deployment', 'btp-deployment', 'regulatory']
PREREQUISITES = {p: [] for p in PROFILES}
PREREQUISITES.update(contracts=['schema'], scenarios=['schema', 'contracts'], recovery=['schema', 'contracts'],
                    mcp=['schema', 'contracts', 'scenarios', 'recovery'], skills=['schema', 'contracts', 'scenarios', 'recovery'])
for _profile in ['model', 'local-deployment', 'btp-deployment']:
    PREREQUISITES[_profile] = ['schema', 'contracts', 'scenarios', 'recovery', 'mcp', 'skills']
LAYER_PROFILE = {'UNIT': 'contracts', 'API': 'scenarios', 'DB': 'scenarios', 'MCP': 'mcp', 'SKILLS': 'skills',
                 'MODEL': 'model', 'LOCAL_DEPLOYMENT': 'local-deployment', 'BTP_DEPLOYMENT': 'btp-deployment', 'REGULATORY_REVIEW': 'regulatory'}
ARTIFACT_CLASSES = {'ACTUAL', 'ACTUAL_HOST', 'ACTUAL_RUNTIME'}
BAD_MARKERS = ('selftest', 'canned', 'stub', 'fake', 'captured', 'unimplemented')
REGULATORY_REVIEW_FIELDS = ('officialSourceRef', 'jurisdiction', 'applicableDate', 'reviewerId', 'reviewedAt')


def status_of(values):
    values = list(values)
    return 'FAIL' if 'FAIL' in values else 'PASS' if values and all(v == 'PASS' for v in values) else 'NOT_RUN'


def nonempty(value):
    return isinstance(value, str) and bool(value.strip())


def instant(value):
    if not nonempty(value):
        raise ValueError('Missing execution time')
    parsed = dt.datetime.fromisoformat(value.replace('Z', '+00:00'))
    if parsed.tzinfo is None:
        raise ValueError('Execution time needs an offset')
    return parsed


def pointer(node, path):
    if path == '':
        return node
    if not isinstance(path, str) or not path.startswith('/'):
        raise ValueError('Invalid RFC6901 pointer')
    for part in path[1:].split('/'):
        part = part.replace('~1', '/').replace('~0', '~')
        node = node[int(part)] if isinstance(node, list) else node[part]
    return node


def effective_profiles(profiles):
    return [effective for profile in profiles for effective in (['local-deployment', 'btp-deployment'] if profile == 'deployment' else [profile])]


def flatten(actions):
    result = []
    for action in actions:
        result.append(action)
        for branch in action.get('branches', []):
            result.extend(flatten(branch.get('actions', [])))
    return result


RECHECK_OPS = {'equals', 'notEquals', 'present', 'absent', 'decimalEquals', 'decimalAtMost', 'decimalAtLeast', 'sumEquals',
               'decimalDelta', 'count', 'exactSet', 'relationSet', 'sameAs', 'fieldsPresent'}
DECIMAL = re.compile(r'-?(0|[1-9][0-9]*)(\.[0-9]+)?')


class NotRecheckable(Exception):
    """The independent re-evaluation cannot decide; it never turns into PASS or FAIL by itself."""


def has_reference(node):
    if isinstance(node, dict):
        return any(k.startswith('$') for k in node) or any(has_reference(v) for v in node.values())
    if isinstance(node, list):
        return any(has_reference(v) for v in node)
    return False


def same_json(left, right):
    return json.dumps(left, sort_keys=True) == json.dumps(right, sort_keys=True)


def recheck_select(results, source):
    """Mirror of the runner's documented pointer/where/field projection over captured StepResults."""
    if not isinstance(source, dict) or has_reference(source.get('where')):
        raise NotRecheckable()
    try:
        value = pointer(results[source['actionId']], source['pointer'])
    except (KeyError, IndexError, TypeError, ValueError) as error:
        raise NotRecheckable() from error
    if value is None:
        raise NotRecheckable()
    if 'where' in source:
        if not isinstance(value, list) or any(not isinstance(r, dict) or any(r.get(k) is None for k in source['where']) for r in value):
            raise NotRecheckable()
        value = [r for r in value if all(same_json(r[k], v) for k, v in source['where'].items())]
    if 'field' in source:
        names = source['field'] if isinstance(source['field'], list) else [source['field']]
        if not isinstance(value, list) or any(not isinstance(r, dict) or r.get(n) is None for r in value for n in names):
            raise NotRecheckable()
        value = [[r[n] for n in names] if isinstance(source['field'], list) else r[names[0]] for r in value]
    return value


def recheck_decimal(value):
    if not isinstance(value, str) or not DECIMAL.fullmatch(value):
        return None
    return decimal.Decimal(value)


def independent_verdict(declared, results):
    """Re-evaluate a declared assertion from captured action bytes. True/False, or None when undecidable."""
    op, expected = declared.get('op'), declared.get('expected')
    if op not in RECHECK_OPS or has_reference(expected):
        return None
    try:
        if op == 'absent':
            try:
                pointer(results[declared['source']['actionId']], declared['source']['pointer'])
                return False
            except (KeyError, IndexError):
                return True
        value = recheck_select(results, declared.get('source'))
        if 'unit' in declared:
            unit = recheck_select(results, declared.get('unitSource'))
            units = unit if isinstance(unit, list) else [unit]
            if not units or any(not same_json(u, declared['unit']) for u in units):
                return False
        if op == 'equals':
            return same_json(value, expected)
        if op == 'notEquals':
            return not same_json(value, expected)
        if op == 'present':
            return True
        if op in ('decimalEquals', 'decimalAtMost', 'decimalAtLeast', 'decimalDelta'):
            left, right = recheck_decimal(value), recheck_decimal(expected)
            if left is None or right is None:
                return False
            if op == 'decimalDelta':
                if 'unit' in declared:
                    base_unit = recheck_select(results, declared.get('baselineUnitSource'))
                    if any(not same_json(u, declared['unit']) for u in (base_unit if isinstance(base_unit, list) else [base_unit])):
                        return False
                base = recheck_decimal(recheck_select(results, declared.get('baseline')))
                return base is not None and left - base == right
            return left == right if op == 'decimalEquals' else left <= right if op == 'decimalAtMost' else left >= right
        if op == 'sumEquals':
            parts = [recheck_decimal(v) for v in value] if isinstance(value, list) else [None]
            right = recheck_decimal(expected)
            return None not in parts and right is not None and sum(parts, decimal.Decimal(0)) == right
        if op == 'count':
            return isinstance(value, list) and type(expected) is int and len(value) == expected
        if op in ('exactSet', 'relationSet'):
            if not isinstance(value, list) or not isinstance(expected, list):
                return False
            observed = [json.dumps(v, sort_keys=True) for v in value]
            wanted = [json.dumps(v, sort_keys=True) for v in expected]
            return len(observed) == len(set(observed)) and set(observed) == set(wanted)
        if op == 'sameAs':
            return same_json(value, recheck_select(results, declared.get('baseline')))
        if op == 'fieldsPresent':
            return (isinstance(value, list) and value and isinstance(expected, list)
                    and all(isinstance(r, dict) and r.get(f) is not None and not (isinstance(r.get(f), str) and not r[f].strip()) for r in value for f in expected))
    except NotRecheckable:
        return None
    return None


class Assembly:
    def __init__(self, root, commit=None, working_tree_dirty=None):
        self.root = pathlib.Path(root).resolve()
        self.commit = commit or subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=self.root, text=True).strip()
        if working_tree_dirty is None:
            working_tree_dirty = True if commit else bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=self.root, text=True).strip())
        self.working_tree_dirty = working_tree_dirty
        self.problems, self.sources, self.preparation_problems = [], {}, []

    def issue(self, status, message, preparation=False):
        item = {'status': status, 'reason': message}
        (self.preparation_problems if preparation else self.problems).append(item)
        return status

    def file(self, ref):
        if not nonempty(ref):
            raise ValueError('Missing file reference')
        lexical = self.root / ref
        actual = lexical.resolve(strict=True)
        actual.relative_to(self.root)
        if not actual.is_file():
            raise ValueError('Evidence is not a regular file')
        return actual

    def descriptor(self, ref):
        path = self.file(ref)
        content = path.read_bytes()
        value = {'path': path.relative_to(self.root).as_posix(), 'sha256': hashlib.sha256(content).hexdigest(), 'sizeBytes': len(content)}
        self.sources[value['path']] = value
        return value

    def read(self, ref, preparation=False):
        try:
            self.descriptor(ref)
            return json.loads(self.file(ref).read_text())
        except FileNotFoundError:
            self.issue('NOT_RUN', f'Missing file: {ref}', preparation)
        except (ValueError, OSError, json.JSONDecodeError) as error:
            self.issue('FAIL', f'Invalid file {ref}: {error}', preparation)
        return None

    def checked_descriptor(self, expected):
        if not isinstance(expected, dict):
            raise ValueError('Artifact descriptor is missing')
        actual = self.descriptor(expected.get('path'))
        if expected.get('sha256') != actual['sha256'] or type(expected.get('sizeBytes')) is not int or expected['sizeBytes'] != actual['sizeBytes']:
            raise ValueError(f'Artifact bytes/hash mismatch: {actual["path"]}')
        return actual

    def catalog(self):
        catalog = self.read('verification/requirements/mandatory-oracles.json', True)
        if catalog is None:
            return [], {}
        problems_before = len(self.preparation_problems)
        lock = self.read('verification/requirements/normative-contract-lock.json', True)
        # Any lock bytes that parsed (including {}, null, [] or 0) must pass the independent
        # validator. A falsy lock is a FAIL, never a reason to skip the drift fence.
        if len(self.preparation_problems) == problems_before:
            try:
                module_ref = 'verification/requirements/validate_catalog.py'
                self.descriptor(module_ref)
                self.descriptor('verification/requirements/mandatory-oracles.schema.json')
                spec = importlib.util.spec_from_file_location('coverage_normative_validator', self.file(module_ref))
                validator = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(validator)
                validator.check_lock(lock)
                validator.validate(catalog, lock, self.root)
            except FileNotFoundError:
                self.issue('NOT_RUN', 'Independent catalog validator/schema source missing', True)
            except (ValueError, KeyError, TypeError, OSError, AssertionError) as error:
                self.issue('FAIL', 'Independent normative catalog validation failed: ' + str(error), True)
        oracles, observations = {}, {}
        if set(catalog.get('requiredCaseIds', [])) != set(CASE_IDS) or len(catalog.get('requiredCaseIds', [])) != 41:
            self.issue('FAIL', 'Independent catalog case identity drift', True)
        for oracle in catalog.get('oracles', []):
            oid = oracle.get('oracleId')
            if oid in oracles:
                self.issue('FAIL', f'Duplicate oracle {oid}', True)
            oracles[oid] = oracle
            for observation in oracle.get('expectedObservations', []):
                key = (oid, observation.get('name'))
                if key in observations:
                    self.issue('FAIL', f'Duplicate named observation {key}', True)
                observations[key] = {'oracleId': oid, 'observationName': key[1], 'caseId': oracle.get('caseId'),
                                     'requiredProfiles': sorted(set(LAYER_PROFILE[l] for l in oracle.get('requiredLayers', []) if l in LAYER_PROFILE)),
                                     'assertionLinks': [], 'status': 'NOT_RUN', 'expected': observation.get('expected')}
        if len(oracles) != 122 or len(observations) != 499:
            self.issue('FAIL', 'Independent catalog requires 122 oracles and 499 observations', True)
        for source in catalog.get('sourceFiles', []):
            try:
                if self.descriptor(source.get('path'))['sha256'] != source.get('sha256'):
                    self.issue('FAIL', 'Normative source hash drift: ' + str(source.get('path')), True)
            except FileNotFoundError:
                self.issue('NOT_RUN', 'Missing normative source: ' + str(source.get('path')), True)
            except (ValueError, OSError) as error:
                self.issue('FAIL', str(error), True)
        return oracles, observations

    def fixture_bundle(self, ref, visited=None):
        visited = set() if visited is None else visited
        if ref in visited:
            self.issue('FAIL', f'Fixture cycle: {ref}', True)
            return []
        visited.add(ref)
        fixture = self.read(ref, True)
        if fixture is None:
            return []
        descriptors = [self.descriptor(ref)]
        for base in fixture.get('baseRefs', []):
            descriptors.extend(self.fixture_bundle(base, visited))
        visited.remove(ref)
        return descriptors

    def declarations(self, observations):
        registry = self.read('verification/cases/registry.json', True)
        entries = registry.get('cases', []) if registry else []
        found = [e.get('caseId') for e in entries]
        if registry and (len(found) != len(set(found)) or set(found) != set(CASE_IDS)):
            self.issue('FAIL', 'Registry needs exact unique case identities', True)
        declarations = []
        for case_id in CASE_IDS:
            case_ref = f'verification/cases/{case_id}/case.json'
            case = self.read(case_ref, True)
            if case is None:
                continue
            if case.get('caseId') != case_id:
                self.issue('FAIL', f'Case ID differs from actual path: {case_id}', True)
            entry = next((e for e in entries if e.get('caseId') == case_id), None)
            if entry:
                try:
                    if self.file(entry.get('path')) != self.file(case_ref) or self.file(entry.get('feature')) != self.file(case_ref.replace('case.json', 'scenario.feature')):
                        self.issue('FAIL', f'Registry path mismatch: {case_id}', True)
                except FileNotFoundError:
                    self.issue('NOT_RUN', f'Missing case/feature: {case_id}', True)
                except (ValueError, OSError) as error:
                    self.issue('FAIL', str(error), True)
                ids = [s.get('id') for s in case.get('subcases', [])]
                registered = entry.get('subcaseIds', [])
                if len(ids) != len(set(ids)) or len(registered) != len(set(registered)) or set(ids) != set(registered):
                    self.issue('FAIL', f'Registry subcase identity mismatch: {case_id}', True)
            unknown_profiles = sorted(set(effective_profiles(case.get('profiles', []))) - set(PROFILES))
            if unknown_profiles:
                self.issue('FAIL', f'Case declares unknown verification profile(s): {case_id} {unknown_profiles}', True)
            for sub in case.get('subcases', []):
                bundle = self.fixture_bundle(sub.get('fixtureRef'))
                sub_profiles = effective_profiles(case.get('profiles', []))
                # REGULATORY_REVIEW is not a case-declarable execution profile (plan §13.4 records the
                # official source/applicable date/reviewer separately). A subcase that asserts such an
                # observation is also declared on the separate regulatory evidence profile, so the
                # observation is reachable only through an actual reviewed regulatory receipt.
                if 'regulatory' not in sub_profiles and any(
                        'regulatory' in observations.get((a.get('oracleRef', {}).get('oracleId'), name), {}).get('requiredProfiles', [])
                        for a in sub.get('assertions', []) for name in a.get('oracleRef', {}).get('observationNames', [])):
                    sub_profiles = sub_profiles + ['regulatory']
                for profile in sub_profiles:
                    declarations.append({'caseId': case_id, 'subcaseId': sub.get('id'), 'profile': profile, 'status': 'NOT_RUN',
                                         'caseHash': self.descriptor(case_ref)['sha256'], 'fixtureArtifacts': bundle,
                                         'assertions': [], '_sub': sub})
                for assertion in sub.get('assertions', []):
                    ref = assertion.get('oracleRef', {})
                    for name in ref.get('observationNames', []):
                        key = (ref.get('oracleId'), name)
                        observation = observations.get(key)
                        if observation is None or observation['caseId'] != case_id:
                            self.issue('FAIL', f'Unknown/wrong-case oracle observation: {case_id}/{key}', True)
                        else:
                            for profile in sub_profiles:
                                observation['assertionLinks'].append({'caseId': case_id, 'subcaseId': sub.get('id'), 'assertionId': assertion.get('id'),
                                                                       'profile': profile, 'status': 'NOT_RUN', 'evidenceRefs': assertion.get('evidenceRefs', [])})
        for observation in observations.values():
            if not observation['assertionLinks']:
                self.issue('NOT_RUN', f'Unlinked observation: {observation["oracleId"]}/{observation["observationName"]}', True)
                continue
            # requiredLayers map to required profiles. A profile that no case assertion executes on can
            # never yield a clause state, so the observation would stay NOT_RUN forever while the
            # declaration looked complete. That is a preparation defect, not a runtime gap.
            linked = {link['profile'] for link in observation['assertionLinks']}
            for profile in observation['requiredProfiles']:
                if profile not in linked:
                    self.issue('FAIL', f'Unreachable required profile: {observation["oracleId"]}/{observation["observationName"]} '
                                       f'requires {profile} but case {observation["caseId"]} links no assertion on that profile', True)
        if registry and (registry.get('expectedCases') != 41 or registry.get('expectedSubcases') != len({(d['caseId'], d['subcaseId']) for d in declarations})):
            self.issue('FAIL', 'Registry discovery counts differ from declaration inputs', True)
        return declarations

    def receipt(self, report, ref, profile, report_ref):
        receipt = self.read(ref)
        if receipt is None:
            return None
        try:
            if receipt.get('schemaVersion') != '1.0.0':
                raise ValueError('Unknown execution receipt schema version')
            if receipt.get('evidenceClass') != 'ACTUAL':
                raise ValueError('SELFTEST/CONTRACT_RED is not actual runtime receipt')
            if receipt.get('codeCommit') != self.commit:
                raise ValueError('Execution commit differs from assembly commit')
            if self.checked_descriptor(receipt.get('reportArtifact')) != self.descriptor(report_ref):
                raise ValueError('Receipt is bound to a different report file')
            identity = receipt.get('executionIdentity', {})
            if report.get('executionIdentity') != identity:
                raise ValueError('Report execution identity differs from receipt')
            if any(not nonempty(identity.get(k)) for k in ['runId', 'hostId', 'workspaceId', 'actorId']):
                raise ValueError('Missing actual execution identity')
            command = receipt.get('command', {})
            if report.get('command') != command.get('display'):
                raise ValueError('Report command differs from actual receipt display')
            if not isinstance(command.get('argv'), list) or not command['argv'] or any(not nonempty(v) for v in command['argv']):
                raise ValueError('Missing actual command argv')
            if type(command.get('exitCode')) is not int or command['exitCode'] != report.get('exitCode'):
                raise ValueError('Receipt/report exit code differs')
            if type(report.get('exitCode')) is not int or report['exitCode'] < 0:
                raise ValueError('Actual report exit code missing/invalid')
            if instant(command.get('startedAt')) > instant(command.get('completedAt')):
                raise ValueError('Execution interval is reversed')
            versions = receipt.get('versions', {})
            if any(not nonempty(versions.get(k)) for k in ['schema', 'definition', 'evaluator', 'policy', 'tool']):
                raise ValueError('Missing actual versions')
            if profile in ('scenarios', 'recovery', 'mcp', 'skills') and not nonempty(versions.get('db')):
                raise ValueError('Missing actual DB version')
            if profile in ('mcp', 'skills') and not nonempty(versions.get('protocol')):
                raise ValueError('Missing actual MCP protocol version')
            if profile == 'model' and any(not nonempty(versions.get(k)) for k in ['client', 'model', 'prompt', 'skill']):
                raise ValueError('Missing actual model/client/prompt/skill version')
            if any(marker in json.dumps({'versions': versions, 'command': command, 'identity': identity}).lower() for marker in BAD_MARKERS):
                raise ValueError('Captured/stub/selftest identity cannot become ACTUAL')
            if not receipt.get('inputs') or not receipt.get('artifacts'):
                raise ValueError('Missing actual input/artifact bytes')
            for artifact in receipt['inputs']:
                self.checked_descriptor(artifact)
            output_paths = set()
            documents = []
            for artifact in receipt['artifacts']:
                actual = self.checked_descriptor(artifact)
                if '/src/test/' in '/' + actual['path'] or actual['path'].startswith(('verification/coverage/checks/', 'verification/model-corpus/checks/')):
                    raise ValueError('Known selftest artifact path cannot become ACTUAL')
                if actual['path'] in output_paths:
                    raise ValueError('Duplicate canonical actual artifact path')
                output_paths.add(actual['path'])
                content = json.loads(self.file(actual['path']).read_text())
                documents.append(content)
                if not isinstance(artifact.get('scope'), dict) or not artifact['scope'] or artifact.get('completeness') != 'COMPLETE' or content.get('scope') != artifact['scope']:
                    raise ValueError('Actual artifact scope/completeness differs from captured bytes')
                if content.get('evidenceClass') not in ARTIFACT_CLASSES or content.get('executionIdentity') != identity or content.get('command') != command or content.get('versions') != versions:
                    raise ValueError('Actual artifact execution identity/command/versions differ from receipt')
                provenance = content.get('provenance', {})
                if not isinstance(provenance, dict) or provenance.get('independent') is not True or not nonempty(provenance.get('source')) or not provenance['source'].startswith('ACTUAL_'):
                    raise ValueError('Actual artifact lacks independent captured provenance')
                if any(marker in str(content.get('provenance', '')).lower() for marker in BAD_MARKERS):
                    raise ValueError('Canned/stub artifact cannot become ACTUAL')
            if report.get('codeCommit') != receipt.get('codeCommit'):
                raise ValueError('Report commit differs from actual receipt')
            if receipt.get('workingTreeClean') is not True:
                raise ValueError('Actual run from a dirty or unrecorded working tree cannot identify the tested code commit')
            if profile.endswith('-deployment') and receipt.get('environment') != ('BTP' if profile == 'btp-deployment' else 'LOCAL'):
                raise ValueError('Deployment environment cannot substitute LOCAL for BTP')
            if profile == 'regulatory':
                review = receipt.get('regulatoryReview')
                if not isinstance(review, dict) or any(not nonempty(review.get(k)) for k in REGULATORY_REVIEW_FIELDS):
                    raise ValueError('Regulatory evidence needs official source, jurisdiction, applicable date, reviewer and review time')
                instant(review['reviewedAt'])
                if review.get('fictionalFixture') is not False or any(m in json.dumps(review).lower() for m in BAD_MARKERS + ('synthetic', 'fictional-policy')):
                    raise ValueError('Fictional fixture/selftest policy cannot become regulatory acceptance')
            if not any(document.get('profileResult') == report for document in documents):
                raise ValueError('Actual profile report lacks matching independently captured artifact bytes')
            receipt['_artifactPaths'] = output_paths
            receipt['_artifactDocuments'] = documents
            receipt['_descriptors'] = {self.descriptor(a['path'])['path']: dict(self.descriptor(a['path']), scope=a['scope'], completeness=a['completeness']) for a in receipt['artifacts']}
            return receipt
        except FileNotFoundError as error:
            self.issue('NOT_RUN', f'Missing receipt artifact: {error.filename}')
        except (KeyError, ValueError, OSError, TypeError) as error:
            self.issue('FAIL', f'Invalid actual receipt {ref}: {error}')
        return None

    def profiles(self, index):
        by_profile = {}
        for item in index.get('profiles', []):
            profile = item.get('profile')
            if profile not in PROFILES or profile in by_profile:
                self.issue('FAIL', f'Unknown/duplicate profile evidence: {profile}')
                continue
            report = self.read(item.get('reportRef'))
            record = {'profile': profile, 'status': 'NOT_RUN', 'prerequisiteProfiles': PREREQUISITES[profile], 'missingReason': 'Actual profile evidence unavailable'}
            if report:
                record['reportArtifact'] = self.descriptor(item['reportRef'])
                observed_fail = report.get('status') == 'FAIL' or any(a.get('status') == 'FAIL' for a in report.get('assertionResults', []))
                if item.get('evidenceClass') == 'ACTUAL':
                    if report.get('waiver'):
                        self.issue('FAIL', f'Mandatory runtime profile cannot be waived: {profile}')
                    receipt = self.receipt(report, item.get('receiptRef'), profile, item.get('reportRef'))
                    record['_report'], record['_receipt'] = report, receipt
                    if receipt:
                        record.update(receiptArtifact=self.descriptor(item['receiptRef']), executionIdentity=receipt['executionIdentity'], command=receipt['command'], versions=receipt['versions'], inputArtifacts=receipt['inputs'], artifactRefs=sorted(receipt['_artifactPaths']))
                    if observed_fail:
                        record['status'] = self.issue('FAIL', f'Observed runtime failure: {profile}')
                    elif report.get('status') == 'PASS' and receipt and report.get('gateComplete') is True and report.get('skipped', 0) == 0 and report.get('discovered', 0) > 0 and report.get('discovered') == report.get('started') == report.get('completed'):
                        record['status'], record['missingReason'] = 'PASS', None
                    elif report.get('waiver') or report.get('status') not in ('PASS', 'FAIL', 'NOT_RUN'):
                        self.issue('FAIL', f'Waiver/invalid actual runtime status: {profile}')
                else:
                    record['missingReason'] = 'Preparation/SELFTEST/CONTRACT_RED is not product runtime'
            by_profile[profile] = record
        for profile in PROFILES:
            by_profile.setdefault(profile, {'profile': profile, 'status': 'NOT_RUN', 'prerequisiteProfiles': PREREQUISITES[profile], 'missingReason': 'Required profile report missing; no waiver'})
        for profile in PROFILES:
            record = by_profile[profile]
            if record['status'] == 'PASS' and any(by_profile[p]['status'] != 'PASS' for p in PREREQUISITES[profile]):
                record['status'], record['missingReason'] = 'NOT_RUN', 'Required prerequisite profile is incomplete'
        return by_profile

    def run_case(self, declaration, profiles):
        sub, profile = declaration['_sub'], declaration['profile']
        record = profiles.get(profile, {})
        report = record.get('_report', {})
        runs = [r for r in report.get('cases', []) if r.get('caseId') == declaration['caseId'] and r.get('subcaseId') == declaration['subcaseId']]
        if len(runs) != 1:
            if len(runs) > 1:
                self.issue('FAIL', 'Duplicate case runtime identity')
            declaration['missingReason'] = 'Exact case/subcase/profile execution report missing'
            return
        run = runs[0]
        declaration['status'] = 'FAIL' if run.get('status') == 'FAIL' or any(a.get('status') == 'FAIL' for a in run.get('assertions', [])) else 'NOT_RUN'
        receipt = record.get('_receipt')
        if not receipt:
            actual_assertions = {a.get('assertionId'): a for a in run.get('assertions', [])}
            for declared in sub.get('assertions', []):
                actual = actual_assertions.get(declared['id'], {})
                declaration['assertions'].append({'assertionId': declared['id'], 'oracleId': declared.get('oracleRef', {}).get('oracleId'),
                                                  'observationNames': declared.get('oracleRef', {}).get('observationNames', []), 'expected': declared.get('expected'),
                                                  'observed': None, 'evidenceRefs': [], 'status': 'FAIL' if actual.get('status') == 'FAIL' else 'NOT_RUN'})
            declaration['missingReason'] = 'Actual command receipt missing/invalid'
            return
        try:
            if run.get('caseHash') != declaration['caseHash'] or not declaration['fixtureArtifacts'] or run.get('fixtureHash') != declaration['fixtureArtifacts'][0]['sha256']:
                raise ValueError('Runtime case/fixture hash differs from actual files')
            if any(run.get('versions', {}).get(k) != receipt['versions'].get(k) for k in ['definition', 'evaluator', 'policy']):
                raise ValueError('Case versions differ from command receipt')
            if not instant(receipt['command']['startedAt']) <= instant(run.get('startedAt')) <= instant(run.get('finishedAt')) <= instant(receipt['command']['completedAt']):
                raise ValueError('Case interval is outside actual command interval')
            required_inputs = {self.descriptor(f'verification/cases/{declaration["caseId"]}/case.json')['path']} | {d['path'] for d in declaration['fixtureArtifacts']}
            if not required_inputs <= {d.get('path') for d in receipt['inputs']}:
                raise ValueError('Case/fixture/base input hash descriptors missing in receipt')
            action_results = run.get('actions', {})
            expected_actions = {a['id']: a for a in flatten(sub.get('actions', []))}
            expected_assertions = {a['id']: a for a in sub.get('assertions', [])}
            observed_assertions = {a.get('assertionId'): a for a in run.get('assertions', [])}
            complete = set(expected_actions) == set(action_results) and set(expected_assertions) == set(observed_assertions) and len(observed_assertions) == len(run.get('assertions', []))
            for aid, action in expected_actions.items():
                actual = action_results.get(aid, {})
                if actual.get('driverStatus') != 'EXECUTED':
                    complete = False
                    continue
                provenance = actual.get('provenance', {})
                if provenance.get('scopeComplete') is not True or not actual.get('artifactRefs'):
                    complete = False
                if any(marker in json.dumps(provenance).lower() for marker in BAD_MARKERS):
                    raise ValueError('Canned/stub action cannot become actual execution')
                if not any(document.get('observations', {}).get(aid) == actual for document in receipt['_artifactDocuments']):
                    raise ValueError('Action result differs from captured artifact bytes')
                if action.get('kind') == 'observe' and (provenance.get('independent') is not True or not provenance.get('sourceQuery') or not provenance.get('snapshot')):
                    complete = False
                for ref in actual.get('artifactRefs', []):
                    if self.descriptor(ref)['path'] not in receipt['_artifactPaths']:
                        raise ValueError('Action artifact is not bound to actual receipt')
            for aid, declared in expected_assertions.items():
                actual = observed_assertions.get(aid, {})
                state = actual.get('status', 'NOT_RUN')
                link = {'assertionId': aid, 'oracleId': declared.get('oracleRef', {}).get('oracleId'), 'observationNames': declared.get('oracleRef', {}).get('observationNames', []),
                        'expected': declared.get('expected'), 'observed': None, 'evidenceRefs': [], 'status': state}
                if state not in ('PASS', 'FAIL', 'NOT_RUN'):
                    raise ValueError('Invalid assertion execution status')
                if state in ('PASS', 'FAIL'):
                    if actual.get('expected') != declared.get('expected') or actual.get('source') != declared.get('source'):
                        raise ValueError('Runtime assertion differs from declared oracle/source')
                    # Do not take the runner's PASS on trust: re-evaluate what can be decided from the captured
                    # action bytes with the declared op/unit/where/field/baseline. Undecidable cases are left to review.
                    if state == 'PASS' and independent_verdict(declared, action_results) is False:
                        raise ValueError(f'Runner PASS contradicts independent re-evaluation of {aid} over captured bytes')
                    sources = [declared[key] for key in ['source', 'baseline', 'unitSource', 'baselineUnitSource'] if key in declared]
                    observed = {}
                    for source in sources:
                        result = action_results.get(source['actionId'], {})
                        if result.get('driverStatus') != 'EXECUTED':
                            complete = False
                            link['status'] = 'NOT_RUN' if state == 'PASS' else 'FAIL'
                        else:
                            try:
                                observed[source['actionId'] + ':' + source['pointer']] = pointer(result, source['pointer'])
                            except (KeyError, IndexError):
                                if declared.get('op') == 'absent' and source['pointer'].rpartition('/')[0]:
                                    pointer(result, source['pointer'].rpartition('/')[0])
                                    observed[source['actionId'] + ':' + source['pointer']] = {'observation': 'ABSENT'}
                                else:
                                    raise ValueError('PASS assertion has no actual source observation')
                            link['evidenceRefs'].extend(result.get('artifactRefs', []))
                    link['observed'] = observed or None
                    if not declared.get('evidenceRefs') or not link['evidenceRefs']:
                        complete = False
                        link['status'] = 'NOT_RUN' if state == 'PASS' else 'FAIL'
                declaration['assertions'].append(link)
            declaration['status'] = status_of([declaration['status']] if declaration['status'] == 'FAIL' else
                                               [a['status'] for a in declaration['assertions']] + ['PASS' if complete and run.get('status') == 'PASS' and run.get('runtimeComplete') is True and record.get('status') == 'PASS' else 'NOT_RUN'])
            declaration['missingReason'] = None if declaration['status'] == 'PASS' else 'Failed or incomplete action/assertion/artifact/profile execution'
        except FileNotFoundError:
            declaration['status'] = status_of([declaration['status'], 'NOT_RUN'])
            declaration['missingReason'] = 'Actual action artifact missing'
        except (KeyError, IndexError, ValueError, TypeError, OSError) as error:
            declaration['status'] = self.issue('FAIL', f'Invalid runtime case evidence: {declaration["caseId"]}/{declaration["subcaseId"]}: {error}')

    def model_bindings(self, corpus, registry, report):
        """Read each immutable corpus assertion pointer, not only report totals."""
        bindings = {}
        self.model_turn_bindings = {}
        all_ids = set()
        required_inputs = {'verification/model-corpus/corpus.json', 'verification/model-binding/registry.json',
                           'verification/model-binding/semantic-paths.json'}
        entries = registry.get('cases', [])
        ids = [entry.get('caseId') for entry in entries]
        if len(ids) != 60 or set(ids) != {f'M{i:02}' for i in range(1, 61)} or registry.get('plannedRepeats') != 3:
            self.issue('FAIL', 'Model registry identity/repeat drift', True)
            return bindings
        for entry in entries:
            cid = entry['caseId']
            ref = entry.get('bindingRef')
            binding = self.read(ref, True)
            if not binding:
                continue
            required_inputs.update({ref, ref.replace('binding.json', 'scenario.feature'), ref.replace('binding.json', 'fixture.json')})
            corpus_case = next((c for c in corpus['cases'] if c.get('id') == cid), None)
            turn_ids = [turn.get('id') for turn in binding.get('turns', [])]
            if not corpus_case or len(turn_ids) != len(set(turn_ids)) or set(turn_ids) != set(entry.get('turnIds', [])) or len(turn_ids) != len(corpus_case['turns']):
                self.issue('FAIL', 'Model binding turn membership drift: ' + cid, True)
                continue
            for turn in binding['turns']:
                try:
                    expected_case_index = next(i for i, c in enumerate(corpus['cases']) if c.get('id') == cid)
                    expected_turn_pointer = f'/cases/{expected_case_index}/turns/{entry["turnIds"].index(turn["id"])}'
                    if turn['corpusTurnPointer'] != expected_turn_pointer:
                        raise ValueError('Binding source turn identity differs from registry order')
                    actual_turn = pointer(corpus, turn['corpusTurnPointer'])
                    if actual_turn not in corpus_case['turns']:
                        raise ValueError('Binding points to another corpus case')
                    common = turn.get('commonAssertions', [])
                    if len(common) != len(actual_turn['oracle']['assertions']):
                        raise ValueError('Common corpus assertion omitted')
                    pointers = set()
                    turn_binding = {'common': {}, 'paths': {}}
                    expected_pointer_prefix = turn['corpusTurnPointer'] + '/oracle/assertions/'
                    for assertion in common:
                        aid, ap = assertion['assertionId'], assertion['corpusAssertionPointer']
                        if aid in bindings or ap in pointers or not ap.startswith(expected_pointer_prefix):
                            raise ValueError('Duplicate/wrong-turn common assertion')
                        source_assertion = pointer(corpus, ap)
                        if source_assertion.get('path') != assertion.get('semanticPath'):
                            raise ValueError('Common assertion semantic path differs from source')
                        pointers.add(ap)
                        bindings[aid] = {'caseId': cid, 'turnId': turn['id'], 'semanticPath': assertion['semanticPath']}
                        turn_binding['common'][aid] = bindings[aid]
                        if aid in all_ids:
                            raise ValueError('Duplicate model assertion identity')
                        all_ids.add(aid)
                    if pointers != {expected_pointer_prefix + str(i) for i in range(len(common))}:
                        raise ValueError('Common corpus assertion pointer set incomplete')
                    oracle = actual_turn['oracle']
                    source_paths = {'SIT_DIRECT_COMMAND': (oracle.get('sitDirectCommand', {}), '/oracle/sitDirectCommand')}
                    source_paths.update({key: (value, '/oracle/uatCompletion/pathOracles/' + key)
                                         for key, value in oracle.get('uatCompletion', {}).get('pathOracles', {}).items()})
                    declared_paths = turn.get('oracleAssertions', {})
                    if set(declared_paths) != set(source_paths):
                        raise ValueError('Binding oracle branch membership differs from source')
                    path_assertions = {}
                    for branch, (source, suffix) in source_paths.items():
                        refs = declared_paths[branch]
                        prefix = turn['corpusTurnPointer'] + suffix + '/assertions/'
                        expected_pointers = {prefix + str(i) for i in range(len(source.get('assertions', [])))}
                        if len(refs) != len(expected_pointers) or {a['corpusAssertionPointer'] for a in refs} != expected_pointers:
                            raise ValueError('Selected-path corpus assertion pointers incomplete')
                        path_assertions[branch] = {}
                        for assertion in refs:
                            aid = assertion['assertionId']
                            if aid in all_ids or pointer(corpus, assertion['corpusAssertionPointer']).get('path') != assertion.get('semanticPath'):
                                raise ValueError('Duplicate or wrong selected-path assertion')
                            all_ids.add(aid)
                            path_assertions[branch][aid] = {'caseId': cid, 'turnId': turn['id'], 'semanticPath': assertion['semanticPath']}
                    allowed = oracle.get('uatCompletion', {}).get('allowedPaths', ['DIRECT'])
                    for selected in allowed:
                        # Runner falls back to sitDirectCommand only for SERVER_REJECTION.
                        turn_binding['paths'][selected] = path_assertions.get(selected, path_assertions['SIT_DIRECT_COMMAND'] if selected == 'SERVER_REJECTION' else {})
                    self.model_turn_bindings[(cid, turn['id'])] = turn_binding
                except (KeyError, IndexError, ValueError, TypeError) as error:
                    self.issue('FAIL', 'Model assertion source mismatch: ' + str(error), True)
        declared = {d.get('path'): d for d in report.get('inputArtifacts', [])}
        if not required_inputs <= set(declared):
            self.issue('NOT_RUN', 'Model preparation report lacks exact binding/scenario/fixture/source input hashes', True)
        for ref in required_inputs | set(declared):
            if ref not in declared:
                self.read(ref, True)
                continue
            try:
                self.checked_descriptor(declared[ref])
            except FileNotFoundError:
                self.issue('NOT_RUN', 'Model preparation input missing: ' + ref, True)
            except (ValueError, OSError) as error:
                self.issue('FAIL', 'Model preparation input drift: ' + str(error), True)
        paths = self.read('verification/model-binding/semantic-paths.json', True)
        if paths:
            common_paths = [p.get('semanticPath') for p in paths.get('paths', []) if p.get('common') is True]
            corpus_paths = {a['path'] for c in corpus['cases'] for t in c['turns'] for a in t['oracle']['assertions']}
            if len(common_paths) != 154 or len(common_paths) != len(set(common_paths)) or set(common_paths) != corpus_paths or paths.get('corpusSha256') != self.descriptor('verification/model-corpus/corpus.json')['sha256']:
                self.issue('FAIL', 'Semantic common path mapping differs from all 154 immutable corpus paths', True)
        if len(bindings) != 221:
            self.issue('FAIL', 'Model binding common assertion membership must cover all 221 source assertions', True)
        return bindings

    def model(self, index, profiles):
        corpus = self.read('verification/model-corpus/corpus.json', True)
        registry = self.read('verification/model-binding/registry.json', True)
        report = self.read(index.get('modelBindingReportRef', 'verification/harness/target/evidence/model-binding-preparation.json'), True)
        prepared = 'NOT_RUN'
        bindings = {}
        case_count = len(corpus.get('cases', [])) if corpus else 0
        turn_count = sum(len(c.get('turns', [])) for c in corpus.get('cases', [])) if corpus else 0
        if corpus is not None:
            # Binding hashes only repeat whatever corpus.json holds; the independent corpus
            # validator and the reviewed lock pin are what fix the model oracle.
            try:
                module_ref = 'verification/model-corpus/validate.py'
                self.descriptor(module_ref)
                self.descriptor('verification/model-corpus/corpus.schema.json')
                spec = importlib.util.spec_from_file_location('coverage_corpus_validator', self.file(module_ref))
                corpus_validator = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(corpus_validator)
                corpus_errors = corpus_validator.validate(corpus) + corpus_validator.reviewed_pin_errors(
                    self.file('verification/model-corpus/corpus.json'), self.root)
                if corpus_errors:
                    self.issue('FAIL', 'Model corpus validation/reviewed pin failed: ' + '; '.join(corpus_errors[:5]), True)
            except FileNotFoundError:
                self.issue('NOT_RUN', 'Model corpus validator source missing', True)
            except (ValueError, KeyError, TypeError, AttributeError, OSError) as error:
                self.issue('FAIL', 'Model corpus validator failed: ' + str(error), True)
        if registry and corpus and report:
            before = len(self.preparation_problems)
            bindings = self.model_bindings(corpus, registry, report)
            if report.get('corpusSha256') != self.descriptor('verification/model-corpus/corpus.json')['sha256'] or report.get('bindingRegistrySha256') != self.descriptor('verification/model-binding/registry.json')['sha256']:
                self.issue('FAIL', 'Model binding report input hash drift', True)
            elif len(self.preparation_problems) == before and report.get('caseCount') == case_count == 60 and report.get('turnCount') == turn_count == 73 and report.get('semanticPathCount') == 154 and report.get('preparationStatus') == 'PREPARED':
                prepared = 'PREPARED'
            if report.get('productGateComplete') is True and report.get('actualModelCalls', 0) == 0:
                self.issue('FAIL', 'Binding preparation/RED cannot close model gate')
        runtime = profiles.get('model', {}).get('_report', {})
        receipt = profiles.get('model', {}).get('_receipt')
        usage, cost = runtime.get('usage'), runtime.get('cost') or runtime.get('totalCost')
        calls = runtime.get('actualModelCalls') if runtime else None
        state = profiles.get('model', {}).get('status', 'NOT_RUN')
        attempts = runtime.get('attempts', [])
        if not isinstance(attempts, list) or any(not isinstance(a, dict) for a in attempts):
            self.issue('FAIL', 'Model attempts must be an array of objects')
            state, attempts = 'FAIL', []
        wanted = {(f'M{i:02}', n) for i in range(1, 61) for n in range(1, 4)}
        actual = set()
        # §13.3 proposal: correct structuring of clear requests >=95%. A clear request is every turn the
        # corpus expects as STRUCTURED (not only the clear_synonyms category); NEEDS_INPUT turns stay exact.
        expected_status = {(c.get('id'), f'turn-{i}'): t.get('expectedIntent', {}).get('status')
                           for c in (corpus or {}).get('cases', []) for i, t in enumerate(c.get('turns', []), 1)}
        intent_matched, intent_total, intent_complete = 0, 0, True
        for attempt in attempts:
            pair = (attempt.get('caseId'), attempt.get('repeat'))
            if type(pair[1]) is not int or not isinstance(pair[0], str) or pair not in wanted or pair in actual:
                self.issue('FAIL', 'Duplicate or invalid model attempt identity')
                state = 'FAIL'
            else:
                actual.add(pair)
        complete = case_count == 60 and prepared == 'PREPARED' and receipt and actual == wanted and len(attempts) == 180 and type(calls) is int and calls >= 219
        for attempt in attempts:
            if attempt.get('status') == 'FAIL' or any(a.get('status') == 'FAIL' for a in attempt.get('assertionResults', [])):
                state = 'FAIL'
            if attempt.get('status') != 'PASS' or attempt.get('skipped', 0) != 0 or not attempt.get('artifactRefs') or not attempt.get('usage') or not attempt.get('cost'):
                complete = False
            expected_turns = {b['turnId'] for b in bindings.values() if b['caseId'] == attempt.get('caseId')}
            turns = attempt.get('turnResults', [])
            if not isinstance(turns, list) or any(not isinstance(t, dict) for t in turns):
                self.issue('FAIL', 'Model turnResults must be an array of objects')
                state = 'FAIL'
                continue
            turn_ids = [t.get('turnId') for t in turns]
            if any(not isinstance(tid, str) for tid in turn_ids) or len(turn_ids) != len(set(turn_ids)) or not set(turn_ids) <= expected_turns:
                self.issue('FAIL', 'Duplicate or wrong model turn identity')
                state = 'FAIL'
                continue
            if not expected_turns or set(turn_ids) != expected_turns:
                complete = False
            if receipt and not any(document.get('modelAttempts') == attempts for document in receipt['_artifactDocuments']):
                complete = False
            for turn in turns:
                if turn.get('status') == 'FAIL' or any(a.get('status') == 'FAIL' for a in turn.get('assertionResults', [])):
                    state = 'FAIL'
                match, wanted_status = turn.get('intentMatch'), expected_status.get((attempt.get('caseId'), turn.get('turnId')))
                if type(match) is not bool:
                    intent_complete = False
                elif wanted_status == 'STRUCTURED':
                    intent_total += 1
                    intent_matched += match
                elif not match:
                    self.issue('FAIL', 'Ambiguous/incomplete input was structured differently: ' + str((attempt.get('caseId'), turn.get('turnId'))))
                    state = 'FAIL'
                binding = getattr(self, 'model_turn_bindings', {}).get((attempt.get('caseId'), turn.get('turnId')))
                try:
                    if turn.get('status') != 'PASS' or not binding or not model_validation.selected_assertions(turn, binding):
                        complete = False
                except (KeyError, TypeError, ValueError) as error:
                    self.issue('FAIL', 'Invalid selected model path/assertions: ' + str(error))
                    state = 'FAIL'
            if receipt and any(ref not in receipt['_artifactPaths'] for ref in attempt.get('artifactRefs', [])):
                self.issue('FAIL', 'Model attempt artifact not tied to actual receipt')
                state = 'FAIL'
        if runtime:
            try:
                complete = model_validation.accounting(runtime, receipt) and complete
            except (KeyError, TypeError, ValueError, AttributeError) as error:
                self.issue('FAIL', 'Invalid actual model accounting: ' + str(error))
                state = 'FAIL'
        else:
            complete = False
        clear_intent = None
        if complete and intent_complete and intent_total:
            minimum = model_validation.decimal_ratio((corpus or {}).get('acceptanceProposal', {}).get('clearStructuredMinimumRatio'))
            rate = model_validation.Decimal(intent_matched) / model_validation.Decimal(intent_total)
            clear_intent = {'matched': intent_matched, 'total': intent_total, 'rate': str(rate.quantize(model_validation.Decimal('0.0001'))), 'minimumRatio': str(minimum)}
            if rate < minimum:
                self.issue('FAIL', f'Clear structured intent rate {intent_matched}/{intent_total} is below the proposed minimum {minimum}')
                state = 'FAIL'
        elif complete:
            complete = False
        if not complete and state != 'FAIL':
            state = 'NOT_RUN'
        if not runtime:
            usage, cost, calls = None, None, None
        return {'caseCount': case_count, 'turnCount': turn_count, 'plannedRepeats': 3, 'preparationStatus': prepared, 'status': state, 'clearStructuredIntent': clear_intent,
                'actualModelCalls': calls, 'usage': usage, 'cost': cost, 'missingReason': None if state == 'PASS' else 'Actual per-case repeats, per-turn assertions, usage/cost and versioned artifacts incomplete'}

    def assemble(self, index=None, check_preparation=False):
        index = {} if index is None else index
        for ref in ['verification/coverage/model_validation.py', 'verification/coverage/assemble.py', 'verification/coverage/validate.py', 'verification/coverage/runtime-manifest.schema.json', 'verification/coverage/runtime-evidence-index.schema.json', 'verification/coverage/execution-receipt.schema.json', 'verification/manifest.schema.json']:
            if (self.root / ref).is_file():
                self.descriptor(ref)
        _, observations = self.catalog()
        declarations = self.declarations(observations)
        preparation = 'FAIL' if any(p['status'] == 'FAIL' for p in self.preparation_problems) else 'NOT_RUN'
        if check_preparation and not self.preparation_problems:
            command = [str(self.root / 'verify'), 'prepare']
            completed = subprocess.run(command, cwd=self.root, capture_output=True, text=True)
            transcript = self.root / 'verification/harness/target/evidence/coverage-prepare.txt'
            transcript.parent.mkdir(parents=True, exist_ok=True)
            transcript.write_text(completed.stdout + completed.stderr)
            self.descriptor(str(transcript))
            report = self.read('verification/harness/target/evidence/prepare.json', True)
            if report and report.get('workingTreeDirty') is not False:
                preparation = self.issue('NOT_RUN', 'Preparation ran on a dirty/unrecorded working tree; codeCommit does not identify it', True)
            elif completed.returncode == 0 and report and report.get('status') == 'PREPARED' and report.get('codeCommit') == self.commit and report.get('preparedCases') == 41 and not report.get('preparationProblems'):
                if report.get('harnessMainClassSha256') == self.descriptor('verification/harness/target/classes/org/mulino/verification/Main.class')['sha256']:
                    preparation = 'PREPARED'
                else:
                    preparation = self.issue('FAIL', 'Preparation compiled parser hash differs from current class', True)
            else:
                preparation = self.issue('FAIL', 'Fresh preparation parser/schema/registry check failed', True)
        profiles = self.profiles(index)
        for declaration in declarations:
            self.run_case(declaration, profiles)
        runtime_artifacts = []
        runtime_descriptors = {}
        for declaration in declarations:
            profile_record = profiles.get(declaration['profile'], {})
            receipt = profile_record.get('_receipt')
            if not receipt:
                continue
            for assertion in declaration['assertions']:
                for ref in assertion['evidenceRefs']:
                    descriptor = self.descriptor(ref)
                    captured_descriptor = receipt['_descriptors'][descriptor['path']]
                    if descriptor['path'] in runtime_descriptors and runtime_descriptors[descriptor['path']] != captured_descriptor:
                        self.issue('FAIL', 'Same runtime artifact has conflicting captured identity')
                    runtime_descriptors[descriptor['path']] = captured_descriptor
                    runtime_artifacts.append(dict(descriptor, caseId=declaration['caseId'], subcaseId=declaration['subcaseId'],
                                                  profile=declaration['profile'], assertionId=assertion['assertionId'], status=assertion['status'],
                                                  fixtureHash=declaration['fixtureArtifacts'][0]['sha256'], codeCommit=receipt['codeCommit'],
                                                  command=receipt['command'], versions=receipt['versions'], expected=assertion['expected'], observed=assertion['observed'],
                                                  exitCode=receipt['command']['exitCode']))
        for observation in observations.values():
            for link in observation['assertionLinks']:
                matches = [a for d in declarations if d['caseId'] == link['caseId'] and d['subcaseId'] == link['subcaseId'] and d['profile'] == link['profile']
                           for a in d['assertions'] if a['assertionId'] == link['assertionId']]
                link['status'] = status_of(a['status'] for a in matches)
                if matches:
                    link['evidenceRefs'] = sorted({ref for a in matches for ref in a['evidenceRefs']})
            states = []
            for profile in observation['requiredProfiles']:
                clause_states = [link['status'] for link in observation['assertionLinks'] if link['profile'] == profile]
                states.append(status_of(clause_states + [profiles[profile]['status']]) if clause_states else 'NOT_RUN')
            observation['status'] = status_of(states)
        model = self.model(index, profiles)
        if self.working_tree_dirty:
            # §13.4: evidence must name the exact code commit. Uncommitted changes break that link.
            self.issue('NOT_RUN', 'Working tree is dirty; codeCommit does not identify the assembled code and evidence inputs')
        preparation = 'FAIL' if any(p['status'] == 'FAIL' for p in self.preparation_problems) else 'PREPARED' if preparation == 'PREPARED' and model['preparationStatus'] == 'PREPARED' else 'NOT_RUN'
        states = [d['status'] for d in declarations] + [o['status'] for o in observations.values()] + [p['status'] for p in profiles.values()] + [model['status']] + [p['status'] for p in self.problems]
        runtime = status_of(states)
        if any(p['status'] == 'FAIL' for p in self.preparation_problems):
            runtime = 'FAIL'
        complete = runtime == 'PASS' and preparation == 'PREPARED' and not self.preparation_problems and self.working_tree_dirty is False
        status = 'PASS' if complete else 'FAIL' if runtime == 'FAIL' else 'NOT_RUN'
        for declaration in declarations:
            declaration.pop('_sub', None)
        clean_profiles = [{k: v for k, v in p.items() if not k.startswith('_')} for p in profiles.values()]
        return {'schemaVersion': '1.0.0', 'recordType': 'ACCEPTANCE_HARNESS_REPORT', 'profile': 'coverage', 'status': status,
                'exitCode': {'PASS': 0, 'FAIL': 1, 'NOT_RUN': 2}[status], 'timestamp': dt.datetime.now(dt.timezone.utc).isoformat(),
                'command': ' '.join(sys.argv), 'codeCommit': self.commit, 'workingTreeDirty': self.working_tree_dirty, 'assemblyVersion': '1.0.0', 'assemblerPythonVersion': platform.python_version(),
                'gateComplete': complete, 'runtimeComplete': complete, 'productRuntimeClaimed': complete,
                'artifactCoverageStatus': runtime, 'preparationStatus': preparation, 'runtimeStatus': runtime,
                'expectedCaseIds': CASE_IDS, 'oracleCount': len({k[0] for k in observations}), 'observationCount': len(observations),
                'cases': declarations, 'runtimeArtifacts': runtime_artifacts, 'runtimeArtifactDescriptors': list(runtime_descriptors.values()), 'namedObservations': list(observations.values()), 'profiles': clean_profiles, 'model': model,
                'inputArtifacts': sorted(self.sources.values(), key=lambda d: d['path']), 'coverageProblems': self.problems,
                'preparationProblems': self.preparation_problems, 'semanticOracleEquivalence': 'REQUIRES_CASE_AND_RUNTIME_REVIEW',
                'assemblyEvidenceClass': 'FILESYSTEM_READ_ONLY', 'selftestIsProductEvidence': False}


def validate_manifest(value):
    """Enforce aggregate invariants independently of the schema's shape checks."""
    if value.get('recordType') != 'ACCEPTANCE_HARNESS_REPORT' or value.get('profile') != 'coverage':
        raise ValueError('Not a runtime coverage manifest')
    if value.get('status') not in ('PASS', 'FAIL', 'NOT_RUN'):
        raise ValueError('Invalid runtime aggregate status')
    complete = value.get('status') == 'PASS'
    if complete and value.get('workingTreeDirty') is not False:
        raise ValueError('PASS from a dirty working tree does not identify the tested code commit')
    if any(value.get(k) is not complete for k in ['gateComplete', 'runtimeComplete', 'productRuntimeClaimed']):
        raise ValueError('Completion flags contradict runtime status')
    if value.get('exitCode') != {'PASS': 0, 'FAIL': 1, 'NOT_RUN': 2}[value['status']]:
        raise ValueError('Exit code contradicts status')
    rows = value.get('cases', [])
    if len({(d.get('caseId'), d.get('subcaseId'), d.get('profile')) for d in rows}) != len(rows):
        raise ValueError('Duplicate case/subcase/profile result')
    if complete and (value.get('preparationStatus') != 'PREPARED' or value.get('runtimeStatus') != 'PASS' or value.get('oracleCount') != 122 or value.get('observationCount') != 499
                     or set(d.get('caseId') for d in rows) != set(CASE_IDS) or set(p.get('profile') for p in value.get('profiles', [])) != set(PROFILES)
                     or any(p.get('status') != 'PASS' for p in value['profiles']) or any(d.get('status') != 'PASS' for d in rows)
                     or any(o.get('status') != 'PASS' for o in value.get('namedObservations', [])) or value.get('model', {}).get('status') != 'PASS'
                     or value.get('coverageProblems') or value.get('preparationProblems')):
        raise ValueError('PASS has incomplete required evidence')
    observed_states = [value.get('runtimeStatus')] + [p.get('status') for p in value.get('coverageProblems', []) + value.get('preparationProblems', []) + value.get('cases', []) + value.get('profiles', []) + value.get('namedObservations', [])] + [value.get('model', {}).get('status')]
    if value.get('status') != 'FAIL' and 'FAIL' in observed_states:
        raise ValueError('Observed failure was downgraded')
    return value



def validate_saved(root, value, index_ref='verification/coverage/runtime-evidence-index.json'):
    """Re-read exact source bytes and independently reassemble runtime links/statuses."""
    validate_manifest(value)
    assembly = Assembly(root)
    if value.get('codeCommit') != assembly.commit:
        raise ValueError('Manifest baseline differs from current commit')
    if value.get('workingTreeDirty') is not assembly.working_tree_dirty:
        raise ValueError('Manifest working tree state differs from current checkout')
    for descriptor in value.get('inputArtifacts', []):
        assembly.checked_descriptor(descriptor)
    index = assembly.read(index_ref)
    if index is None:
        raise ValueError('Runtime evidence index missing/invalid')
    current = assembly.assemble(index)
    for key in ['cases', 'runtimeArtifacts', 'runtimeArtifactDescriptors', 'namedObservations', 'profiles', 'model', 'runtimeStatus', 'artifactCoverageStatus', 'coverageProblems', 'preparationProblems']:
        if value.get(key) != current.get(key):
            raise ValueError('Manifest differs from re-read actual evidence: ' + key)
    if value.get('preparationStatus') == 'PREPARED':
        prepared = assembly.read('verification/harness/target/evidence/prepare.json')
        if not prepared or prepared.get('status') != 'PREPARED' or prepared.get('codeCommit') != assembly.commit or prepared.get('preparedCases') != 41 or current['model']['preparationStatus'] != 'PREPARED':
            raise ValueError('Missing matching deterministic/model preparation evidence')
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[2])
    parser.add_argument('--index', default='verification/coverage/runtime-evidence-index.json')
    parser.add_argument('--check-preparation', action='store_true', help='Run fresh existing ./verify prepare only when all declaration inputs exist')
    args = parser.parse_args()
    assembly = Assembly(args.root)
    index = assembly.read(args.index) if (assembly.root / args.index).exists() else {}
    value = validate_manifest(assembly.assemble(index, args.check_preparation))
    target = assembly.root / 'verification/harness/target/evidence/runtime-manifest.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'status': value['status'], 'preparationStatus': value['preparationStatus'], 'runtimeStatus': value['runtimeStatus'],
                      'gateComplete': value['gateComplete'], 'caseProfiles': len(value['cases']), 'observations': value['observationCount'], 'output': str(target)}, ensure_ascii=False))
    return value['exitCode']


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError, TypeError, KeyError) as error:
        print('ENVIRONMENT_OR_EVIDENCE_FORMAT_FAILURE: ' + str(error), file=sys.stderr)
        sys.exit(3)
