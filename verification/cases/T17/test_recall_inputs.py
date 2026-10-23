"""명시적 조사 준비만 추가하며 원본 257 assertion과 다른 fixture bytes를 보존한다."""
import copy
import hashlib
import importlib.util
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
spec = importlib.util.spec_from_file_location('recall', HERE / 'author_recall_inputs.py')
RECALL = importlib.util.module_from_spec(spec)
spec.loader.exec_module(RECALL)
condition_spec = importlib.util.spec_from_file_location('condition_oracle', HERE/'author_condition_oracle.py')
CONDITION = importlib.util.module_from_spec(condition_spec)
condition_spec.loader.exec_module(CONDITION)


def load_original(path):
    return json.loads(RECALL.original(path))


def assertion_blocks(raw):
    text = raw.decode()
    decoder, blocks, start = json.JSONDecoder(), [], 0
    while True:
        at = text.find('"assertions": [', start)
        if at < 0:
            return blocks
        at = text.index('[', at)
        _, size = decoder.raw_decode(text[at:])
        blocks.append(text[at:at + size].encode())
        start = at + size


class RecallInputsTest(unittest.TestCase):
    def test_exact_reversible_delta_preserves_257_oracles_and_14_fixture_bytes(self):
        before = load_original(HERE / 'case.json')
        old = {p.stem: load_original(p) for p in (HERE / 'fixtures').glob('*.json')}
        after, fixtures = RECALL.apply(before, old)
        current = CONDITION.restore_case(json.loads((HERE / 'case.json').read_text()))
        self.assertEqual(after, current)
        self.assertEqual(before, RECALL.restore_case(after))
        self.assertEqual((after, fixtures), RECALL.apply(after, fixtures))
        self.assertEqual(257, sum(len(s['assertions']) for s in after['subcases']))
        old_blocks = assertion_blocks(RECALL.original(HERE / 'case.json'))
        self.assertEqual(15, len(old_blocks))
        self.assertEqual(old_blocks, assertion_blocks(RECALL.bytes_of(current)))
        for a, b in zip(before['subcases'], after['subcases']):
            self.assertEqual(a['assertions'], b['assertions'])
            if a['id'] != RECALL.SID:
                self.assertEqual(a, b)
        ledger = json.loads(RECALL.LEDGER.read_text())
        self.assertEqual(2, len(ledger['fixtureChanges']))
        for path in sorted((HERE / 'fixtures').glob('*.json')):
            current = json.loads(path.read_text())
            self.assertEqual(fixtures[path.stem], current)
            self.assertEqual(RECALL.original(path), RECALL.bytes_of(RECALL.restore_fixture(current)))
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), ledger['fixtureHashes'][path.stem]['after'])
            if path.stem != RECALL.SID:
                self.assertEqual(RECALL.original(path), path.read_bytes())
        f = fixtures[RECALL.SID]
        for key in ('actors', 'clock', 'responsibilities', 'evidence'):
            self.assertEqual(old[RECALL.SID][key], f[key])
        self.assertEqual(RECALL.original(HERE / 'scenario.feature').decode(),
                         RECALL.restore_feature(CONDITION.restore_feature((HERE / 'scenario.feature').read_text())))
        proof = {'kind': 'STATIC_EXACT_INPUT_PRESERVATION_NOT_RUNTIME_ACCEPTANCE',
                 'baseline': RECALL.BASE, 'originalAssertions': 257,
                 'assertionBlockSha256': [hashlib.sha256(b).hexdigest() for b in old_blocks],
                 'unchangedFixtureBytes': 14, 'fixtureHashes': ledger['fixtureHashes'],
                 'reversedLateFixtureSha256': hashlib.sha256(RECALL.bytes_of(RECALL.restore_fixture(f))).hexdigest(),
                 'actorsGrantsClockResponsibilitiesUnchanged': True}
        path = ROOT / '.local/t17-recall-preservation.json'
        path.parent.mkdir(exist_ok=True)
        path.write_text(json.dumps(proof, indent=2) + '\n')

    def test_event_range_time_owner_and_current_authority_are_explicit(self):
        case = json.loads((HERE / 'case.json').read_text())
        fixture = json.loads((HERE / 'fixtures' / (RECALL.SID + '.json')).read_text())
        sub = next(s for s in case['subcases'] if s['id'] == RECALL.SID)
        actions = {a['id']: a for a in sub['actions']}
        request = actions['open-recall-investigation']['request']
        slots = request['slots']
        self.assertEqual(RECALL.ref('late-hold-source-event', 'id'), slots['basisEvidenceId'])
        self.assertNotEqual(RECALL.ref('late-hold-source-document', 'id'), slots['basisEvidenceId'])
        self.assertEqual(('0', '30', 'BOX'), tuple(slots[k] for k in ('startQuantity', 'quantity', 'unit')))
        self.assertEqual(RECALL.ref('late-hold-source-scope', 'data/ID'), slots['segmentId'])
        self.assertLessEqual(slots['occurredAt'], fixture['clock']['knownAt'])
        self.assertGreater(slots['nextCheckAt'], fixture['clock']['knownAt'])
        work = next(w for w in fixture['baseline']['works'] if w['alias'] == RECALL.WORK)
        self.assertEqual(('P', 'L', 'RECALL', 'ACTIVE', 'admin', 'supervisor'),
                         tuple(work[k] for k in ('itemAlias', 'lotAlias', 'kind', 'status', 'ownerAlias', 'supervisorAlias')))
        admin = fixture['actors']['admin']
        self.assertEqual({'organizationAlias': 'ORG', 'itemAliases': ['P', 'ALT']}, admin['grant']['scope'])
        self.assertNotIn('assessGoal', admin['grant']['actions'])
        for cap in ('getEvidence', 'openInvestigation'):
            self.assertIn(cap, admin['roleCapabilities'])
            self.assertIn(cap, admin['grant']['actions'])
        ids = [a['id'] for a in sub['actions']]
        self.assertLess(ids.index('late-hold'), ids.index('open-recall-investigation'))
        self.assertLess(ids.index('open-recall-investigation'), ids.index('before-delivery'))
        self.assertFalse(any(a.get('capabilityId') in ('approveRecall', 'recordRecallNotice', 'assessGoal') for a in sub['actions']))

    def test_mutations_are_rejected_by_exact_inverse(self):
        fixture = json.loads((HERE / 'fixtures' / (RECALL.SID + '.json')).read_text())
        fixture['baseline']['works'][-1]['ownerAlias'] = 'sales'
        with self.assertRaises(ValueError):
            RECALL.restore_fixture(fixture)
        case = json.loads((HERE / 'case.json').read_text())
        sub = next(s for s in case['subcases'] if s['id'] == RECALL.SID)
        next(a for a in sub['actions'] if a['id'] == 'open-recall-investigation')['request']['slots']['quantity'] = '29'
        with self.assertRaises(ValueError):
            RECALL.restore_case(case)

    def test_standalone_and_composed_generator_reproduce_current_bytes(self):
        with tempfile.TemporaryDirectory() as tmp:
            dest = Path(tmp)
            shutil.copytree(HERE, dest / 'verification/cases/T17', ignore=shutil.ignore_patterns('__pycache__'))
            shared = ROOT / 'verification/actual/scenarios/installer-conventions.json'
            (dest / shared.relative_to(ROOT)).parent.mkdir(parents=True)
            shutil.copy2(shared, dest / shared.relative_to(ROOT))
            # git show must still read this task's baseline, not another tree.
            (dest / '.git').write_text('gitdir: ' + subprocess.check_output(['git', 'rev-parse', '--absolute-git-dir'], cwd=ROOT, text=True).strip() + '\n')
            for generator in ('author_recall_inputs.py', 'author_public_contracts.py'):
                done = subprocess.run([sys.executable, str(dest / 'verification/cases/T17' / generator)],
                                      cwd=dest, capture_output=True, text=True)
                self.assertEqual(0, done.returncode, done.stdout + done.stderr)
                for path in (HERE / 'fixtures').glob('*.json'):
                    self.assertEqual(path.read_bytes(), (dest / path.relative_to(ROOT)).read_bytes(), generator + ':' + path.name)
                for name in ('case.json', 'scenario.feature', 'recall-input-delta.json'):
                    self.assertEqual((HERE / name).read_bytes(), (dest / 'verification/cases/T17' / name).read_bytes(), generator + ':' + name)
            # Standalone execution also composes with an unrelated root change.
            path = dest / 'verification/cases/T17/case.json'
            case = json.loads(path.read_text())
            other = next(s for s in case['subcases'] if s['id'] != RECALL.SID)
            other['title'] += ' (독립 layer 보존 probe)'
            path.write_bytes(RECALL.bytes_of(case))
            done = subprocess.run([sys.executable, str(dest / 'verification/cases/T17/author_recall_inputs.py')],
                                  cwd=dest, capture_output=True, text=True)
            self.assertEqual(0, done.returncode, done.stdout + done.stderr)
            self.assertEqual(case, json.loads(path.read_text()))


if __name__ == '__main__':
    unittest.main()
