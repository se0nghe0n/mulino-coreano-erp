#!/usr/bin/env python3
"""자체 checkout의 HTTP/PG 집중 실행과 clean/source/fixture custody를 보존한다."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT/'.local/t17-read-contract'
OUT.mkdir(parents=True, exist_ok=True)


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True)


def snapshot():
    paths = []
    for folder in ('backend/src', 'backend/db', 'backend/srv', 'verification/harness/src', 'verification/cases/T17'):
        paths.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
    hashes = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(paths)}
    status = git('status', '--porcelain')
    return dict(commit=git('rev-parse', 'HEAD').strip(), clean=not status, status=status,
                sourceSha256=hashes, combinedSourceSha256=hashlib.sha256(json.dumps(hashes, sort_keys=True).encode()).hexdigest(),
                fixtureSha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'verification/cases/T17/fixtures').glob('*.json'))})


def version(argv):
    result = subprocess.run(argv, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return dict(command=argv, exitCode=result.returncode, output=result.stdout)


before = snapshot()
existing = set((ROOT/'.local').glob('t17-source-pg-*'))
env = os.environ.copy()
env['T17_PG_TEST_CLASS'] = 'T17ReadQuantityPostgresTest'
command = [sys.executable, str(ROOT/'verification/cases/T17/run_source_pg_test.py')]
result = subprocess.run(command, cwd=ROOT, env=env)
after = snapshot()
created = set((ROOT/'.local').glob('t17-source-pg-*')) - existing
if len(created) != 1:
    raise RuntimeError('Expected exactly one own disposable execution directory')
actual = created.pop()
junit = actual/'junit'
junit.mkdir()
for report in (ROOT/'verification/harness/target/surefire-reports').glob('*T17ReadQuantityPostgresTest*'):
    shutil.copy2(report, junit/report.name)
custody = dict(command=command, focusedExitCode=result.returncode, before=before, after=after,
               sourceBytesUnchanged=before['sourceSha256'] == after['sourceSha256'],
               fixturesUnchanged=before['fixtureSha256'] == after['fixtureSha256'],
               cleanBeforeAndAfter=before['clean'] and after['clean'] and before['commit'] == after['commit'],
               actualEvidence=str(actual), cleanup=json.loads((actual/'cleanup.json').read_text()),
               versions=[version(['java', '-version']), version([str(ROOT/'mvnw'), '-version']),
                         version(['node', '--version']), version([str(ROOT/'backend/node_modules/.bin/cds'), 'version'])])
(actual/'read-contract-custody.json').write_text(json.dumps(custody, indent=2) + '\n')
(OUT/'latest-http-pg-evidence.txt').write_text(str(actual) + '\n')
print('Focused HTTP/PG custody:', actual/'read-contract-custody.json')
sys.exit(result.returncode)
