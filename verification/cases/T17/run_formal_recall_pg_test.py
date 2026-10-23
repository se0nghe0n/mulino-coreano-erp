#!/usr/bin/env python3
"""이 checkout을 빌드해 실제 HTTP/PostgreSQL의 한정된 조사 계약을 검사한다.

scenario runner와 coverage receipt는 실행/작성하지 않는다.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import time
import uuid

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / '.local' / ('t17-formal-recall-' + str(uuid.uuid4()))
OUT.mkdir(parents=True, mode=0o700)
ENV = os.environ.copy()
if not ENV.get('DOCKER_HOST'):
    raise RuntimeError('Explicit DOCKER_HOST required')
if not ENV.get('JAVA_HOME'):
    raise RuntimeError('Explicit JDK21 JAVA_HOME required')
ENV['PATH'] = ENV['JAVA_HOME'] + '/bin:' + ENV['PATH']
CONTAINER = 't17-formal-recall-' + str(uuid.uuid4())
records = []
server = None
created = False
exit_code = 1


def run(args, log, env=ENV):
    with (OUT / log).open('w') as stream:
        done = subprocess.run(args, cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT)
    records.append({'command': args, 'exitCode': done.returncode, 'log': log})
    if done.returncode:
        raise RuntimeError(log + ': exit ' + str(done.returncode))


try:
    run(['java', '-version'], 'jdk.log')
    if not re.search(r'version "21[.]', (OUT / 'jdk.log').read_text()):
        raise RuntimeError('JDK21 required')
    run(['node', '--version'], 'node.log')
    run(['npm', '--version'], 'npm.log')
    if not (ROOT / 'backend/node_modules/.bin/cds').exists():
        run(['npm', 'ci', '--ignore-scripts', '--no-audit', '--no-fund', '--prefix', 'backend'], 'npm-ci.log')
    run(['openssl', 'genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:2048', '-out', str(OUT / 'private.pem')], 'keygen.log')
    run(['openssl', 'pkey', '-in', str(OUT / 'private.pem'), '-pubout', '-out', str(OUT / 'public.pem')], 'public-key.log')
    ENV.update(JWT_PRIVATE_KEY=str(OUT / 'private.pem'), JWT_PUBLIC_KEY=str(OUT / 'public.pem'),
               JWT_ISSUER='https://mulino-native.invalid', JWT_AUDIENCE='isolated-ontology',
               DB_USERNAME='postgres', DB_PASSWORD=secrets.token_urlsafe(24))
    ENV['POSTGRES_PASSWORD'] = ENV['DB_PASSWORD']
    run([str(ROOT / 'mvnw'), '-B', '-ntp', '-f', 'backend/pom.xml', '-Dmaven.test.skip=true', 'package'], 'backend-build.log')
    jar = ROOT / 'backend/target/ontology-0.1.0-SNAPSHOT.jar'
    source = ROOT / 'verification/cases/T17/T17FormalRecallInputCheck.java'
    fixtures = sorted((ROOT / 'verification/cases/T17/fixtures').glob('*.json'))
    (OUT / 'build-identity.json').write_text(json.dumps({
        'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'sourceWorktree': str(ROOT), 'branch': subprocess.check_output(['git', 'branch', '--show-current'], cwd=ROOT, text=True).strip(),
        'productJar': str(jar), 'productJarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
        'focusedCheckSourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
        'dockerHost': ENV['DOCKER_HOST'], 'fixtureHashes': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in fixtures}}, indent=2) + '\n')
    run([str(ROOT / 'mvnw'), '-B', '-ntp', '-f', 'verification/harness/pom.xml', '-DskipTests', 'test-compile',
         'dependency:build-classpath', '-Dmdep.outputFile=' + str(OUT / 'classpath.txt')], 'harness-build.log')
    cp = str(ROOT / 'verification/harness/target/classes') + ':' + (OUT / 'classpath.txt').read_text().strip()
    (OUT / 'classes').mkdir()
    run(['javac', '--release', '21', '-cp', cp, '-d', str(OUT / 'classes'), str(source)], 'focused-compile.log')
    image = json.loads((ROOT / 'verification/platform/versions.json').read_text())['postgresImage']['reference']
    run(['docker', 'run', '-d', '--name', CONTAINER, '-e', 'POSTGRES_PASSWORD', '-e', 'POSTGRES_DB=ontology',
         '-p', '127.0.0.1::5432', image], 'container.log')
    created = True
    port = subprocess.check_output(['docker', 'port', CONTAINER, '5432/tcp'], env=ENV, text=True).strip().rsplit(':', 1)[1]
    ENV['DB_URL'] = 'jdbc:postgresql://127.0.0.1:' + port + '/ontology'
    for _ in range(120):
        if subprocess.run(['docker', 'exec', CONTAINER, 'pg_isready', '-U', 'postgres', '-d', 'ontology'], env=ENV,
                          stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0:
            break
        time.sleep(.25)
    else:
        raise RuntimeError('PostgreSQL startup timeout')
    with (OUT / 'spring.log').open('w') as log:
        args = ['java', '-jar', str(jar), '--spring.profiles.active=local,verification',
                '--mulino.verification.instant=2026-10-07T09:00:00Z', '--server.address=127.0.0.1', '--server.port=0',
                '--mulino.evidence.blob-root=' + str(OUT / 'blobs')]
        server = subprocess.Popen(args, cwd=ROOT, env=ENV, stdout=log, stderr=subprocess.STDOUT)
    records.append({'command': args, 'log': 'spring.log'})
    for _ in range(240):
        if server.poll() is not None:
            raise RuntimeError('Spring startup failed')
        ports = re.findall(r'Tomcat started on port (\d+)', (OUT / 'spring.log').read_text())
        if ports:
            break
        time.sleep(.25)
    else:
        raise RuntimeError('Spring startup timeout')
    ENV.update(ACTUAL_BASE_URL='http://127.0.0.1:' + ports[-1], ACTUAL_DISPOSABLE_DATABASE='true',
               ACTUAL_BUILD_COMMIT=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip())
    run(['java', '-ea', '-Dverification.actual.identityBinding=verification/actual/scenarios/identity-binding.json',
         '-Dverification.actual.blobRoot=' + str(OUT / 'blobs'), '-cp', str(OUT / 'classes') + ':' + cp,
         'org.mulino.verification.actual.T17FormalRecallInputCheck', str(ROOT), str(OUT)], 'focused-test.log')
    exit_code = 0
finally:
    if server:
        server.terminate()
        try:
            server.wait(timeout=15)
        except subprocess.TimeoutExpired:
            server.kill()
            server.wait(timeout=5)
    if created:
        with (OUT / 'postgres.log').open('w') as log:
            subprocess.run(['docker', 'logs', CONTAINER], env=ENV, stdout=log, stderr=subprocess.STDOUT)
        run(['docker', 'rm', '-f', '-v', CONTAINER], 'cleanup-container.log')
    for name in ('private.pem', 'public.pem'):
        (OUT / name).unlink(missing_ok=True)
    (OUT / 'commands.json').write_text(json.dumps(records, indent=2) + '\n')
    (OUT / 'cleanup.json').write_text(json.dumps({'container': CONTAINER, 'containerRemoved': created,
        'serverStopped': server is None or server.poll() is not None,
        'authenticationFilesRemoved': not (OUT / 'private.pem').exists() and not (OUT / 'public.pem').exists(),
        'focusedExitCode': exit_code}, indent=2) + '\n')
    print('Evidence:', OUT)
