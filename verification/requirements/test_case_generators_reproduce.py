"""Committed V4, T06/T22/T24 and T23/V8 files must equal their generators' output (temp copy; never a runtime PASS).

verification/mcp-tests/test_generators_reproduce.py covers T01/T20/T25, C3 and T26. The generators
below were outside every gate, so a hand edit of their outputs or a generator change that was not
re-run passed ./verify prepare. Each generator runs in a disposable copy of the directories it reads
and writes; afterwards every file of those directories must be byte-identical to the repository, so
an unexpected rewrite anywhere also fails. The repository is never written.
C3/T26/V4 are post-processors over their own committed case.json, so for them this proves the fixed
point (idempotence), not derivation from a separate base.
Run: python3 -m unittest discover -s verification/requirements -p 'test_case_generators_reproduce.py' -v
"""
import shutil, subprocess, sys, tempfile, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
IGNORE = shutil.ignore_patterns('__pycache__', '*.pyc', 'target')


def files(base, rel):
    root = base / rel
    return {p.relative_to(base).as_posix() for p in root.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix != '.pyc'}


class CaseGeneratorsReproduce(unittest.TestCase):
    def reproduce(self, script, args, trees):
        with tempfile.TemporaryDirectory() as tmp:
            dest = Path(tmp)
            for rel in trees:
                (dest / rel).parent.mkdir(parents=True, exist_ok=True)
                if (ROOT / rel).is_dir():
                    shutil.copytree(ROOT / rel, dest / rel, ignore=IGNORE)
                else:
                    shutil.copy2(ROOT / rel, dest / rel)
            done = subprocess.run([sys.executable, '-I', str(dest / script)] + args, cwd=dest, capture_output=True, text=True)
            self.assertEqual(0, done.returncode, done.stdout + done.stderr)
            for rel in trees:
                if not (ROOT / rel).is_dir():
                    continue
                before, after = files(ROOT, rel), files(dest, rel)
                self.assertEqual(before, after, f'{script} added or removed files under {rel}')
                drift = sorted(f for f in before if (ROOT / f).read_bytes() != (dest / f).read_bytes())
                self.assertEqual([], drift, f'{script} output differs from the committed files')

    def test_v4_review_fixes(self):
        self.reproduce('verification/cases/V4/author_review_fixes.py', [],
                       ['verification/cases', 'verification/requirements/mandatory-oracles.json', 'contracts'])

    def test_t06_contracts(self):
        self.reproduce('verification/cases/T06/author_contracts.py', [],
                       ['verification/cases', 'verification/requirements/mandatory-oracles.json', 'contracts'])

    def test_platform_cases(self):
        self.reproduce('verification/platform-tests/build_cases.py', [],
                       ['verification/cases', 'verification/platform-tests', 'verification/requirements/mandatory-oracles.json', 'contracts'])


if __name__ == '__main__':
    unittest.main()
