"""Committed T01/T20/T25/C3/T26 contract files must equal their authoring scripts' output.

Each generator runs in a disposable copy of only the inputs it reads, so the
repository is never rewritten. For T01/T20/T25 (author_cases.py derives every
output from the catalog, capability registry and s0-protocol.md) a hand edit of
an output, or a generator change that was not re-run, fails here.
C3 and T26 are post-processors that read their own committed case.json, so these
two tests prove only the fixed point (idempotence): a hand edit of a part the
post-processor regenerates fails, but a hand edit of a part it does not own
survives. Pinning those non-owned parts is open (step2r round 5 README).
Run: python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py' -v
"""
import shutil, subprocess, sys, tempfile, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
# step2r round 12: every generator writes its requests through the shared request contract module.
REQUEST_CONTRACT = ['verification/cases/request_contract.py', 'contracts/intent.schema.json', 'contracts/request-contracts.json']

def copy(rel, dest):
    target = dest / rel
    target.parent.mkdir(parents=True, exist_ok=True)
    (shutil.copytree if (ROOT / rel).is_dir() else shutil.copy2)(ROOT / rel, target)

class GeneratorsReproduceCommittedFiles(unittest.TestCase):
    def run_generator(self, script, inputs, outputs):
        with tempfile.TemporaryDirectory() as tmp:
            dest = Path(tmp)
            for rel in inputs + [script]: copy(rel, dest)
            done = subprocess.run([sys.executable, '-I', str(dest / script)], cwd=dest, capture_output=True, text=True)
            self.assertEqual(0, done.returncode, done.stderr)
            for rel in outputs:
                self.assertEqual((ROOT / rel).read_bytes(), (dest / rel).read_bytes(), rel + ' differs from generator output')

    def test_channel_cases(self):
        outputs = [f'verification/cases/{c}/{f}' for c in ['T01', 'T20', 'T25'] for f in ['case.json', 'fixture.json', 'scenario.feature']]
        self.run_generator('verification/mcp-tests/author_cases.py', ['verification/requirements/mandatory-oracles.json', 'contracts/acceptance-capabilities.json',
                                                                    'contracts/mcp/s0-protocol.md'] + REQUEST_CONTRACT + outputs, outputs)

    def test_c3_post_processor_is_a_fixed_point(self):
        self.run_generator('verification/cases/C3/author_prerequisites.py', ['verification/cases/C3', 'contracts/acceptance-capabilities.json',
                                                                             'verification/requirements/mandatory-oracles.json'] + REQUEST_CONTRACT,
                           [f'verification/cases/C3/{f}' for f in ['case.json', 'scenario.feature', 'observation-bindings.json', 'fixture-closeRecall.json', 'fixture-emergencyReassign.json', 'fixture-dispatchPurchaseOrder.json', 'recipient-acceptance.json']])

    def test_t26_post_processor_is_a_fixed_point(self):
        self.run_generator('verification/cases/T26/author_review_fixes.py', ['verification/cases/T26', 'contracts/acceptance-capabilities.json'] + REQUEST_CONTRACT,
                           [f'verification/cases/T26/{f}' for f in ['case.json', 'scenario.feature', 'oracle-bindings.json', 'fixtures/safe-retry-forged-original-actor.json',
                            'fixtures/due-wait-autonomous-loop.json', 'fixtures/lot-expiry-autonomous-loop.json', 'fixtures/orphan-intake-autonomous-loop.json']])

if __name__ == '__main__':
    unittest.main()
