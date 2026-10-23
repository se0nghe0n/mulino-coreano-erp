#!/usr/bin/env python3
"""Runs one bounded installer/command-service test, never the scenario runner."""
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import time
import uuid
import secrets
import hashlib

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / '.local' / ('t17-source-pg-' + str(uuid.uuid4()))
OUT.mkdir(parents=True)
os.chmod(OUT, 0o700)
container = 't17-source-test-' + str(uuid.uuid4())
server = None
created = False
records = []


def run(args, log, env=None):
    with (OUT / log).open('w') as stream:
        result = subprocess.run(args, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, env=env)
    records.append({'command': args, 'exitCode': result.returncode, 'log': log})
    if result.returncode:
        raise RuntimeError(f'{log}: exit {result.returncode}')


try:
    run(['openssl', 'genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:2048', '-out', str(OUT/'private.pem')], 'keygen.log')
    run(['openssl', 'pkey', '-in', str(OUT/'private.pem'), '-pubout', '-out', str(OUT/'public.pem')], 'public-key.log')
    env = os.environ.copy()
    env.update(JWT_PRIVATE_KEY=str(OUT/'private.pem'), JWT_PUBLIC_KEY=str(OUT/'public.pem'),
               JWT_ISSUER='https://mulino-native.invalid', JWT_AUDIENCE='isolated-ontology',
               DB_USERNAME='postgres', DB_PASSWORD=secrets.token_urlsafe(24))
    env['POSTGRES_PASSWORD'] = env['DB_PASSWORD']
    # Build this checkout before attributing its commit to a running service.
    # A pre-existing target jar may belong to another source revision.
    run([str(ROOT/'mvnw'), '-B', '-ntp', '-f', 'backend/pom.xml',
         '-Dmaven.test.skip=true', 'package'], 'backend-build.log', env)
    product_jar = ROOT/'backend/target/ontology-0.1.0-SNAPSHOT.jar'
    (OUT/'build-identity.json').write_text(json.dumps({
        'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'productJar': str(product_jar),
        'productJarSha256': hashlib.sha256(product_jar.read_bytes()).hexdigest(),
    }, indent=2) + '\n')
    image = json.loads((ROOT/'verification/platform/versions.json').read_text())['postgresImage']['reference']
    run(['docker', 'run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD', '-e', 'POSTGRES_DB=ontology', '-p', '127.0.0.1::5432', image], 'container.log', env)
    created = True
    port = subprocess.check_output(['docker', 'port', container, '5432/tcp'], text=True).strip().rsplit(':', 1)[1]
    env['DB_URL'] = f'jdbc:postgresql://127.0.0.1:{port}/ontology'
    for _ in range(120):
        if subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'postgres', '-d', 'ontology'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0: break
        time.sleep(.25)
    else: raise RuntimeError('PostgreSQL startup timeout')
    server_log = (OUT/'spring.log').open('w')
    command = ['java', '-jar', str(product_jar), '--spring.profiles.active=local,verification',
               '--mulino.verification.instant=2026-10-07T09:00:00Z', '--server.address=127.0.0.1', '--server.port=0', '--mulino.evidence.blob-root='+str(OUT/'blobs')]
    server = subprocess.Popen(command, cwd=ROOT, env=env, stdout=server_log, stderr=subprocess.STDOUT)
    records.append({'command': command, 'log': 'spring.log'})
    for _ in range(240):
        if server.poll() is not None: raise RuntimeError('Spring startup failed')
        match = re.findall(r'Tomcat started on port (\d+)', (OUT/'spring.log').read_text())
        if match: break
        time.sleep(.25)
    else: raise RuntimeError('Spring startup timeout')
    env.update(ACTUAL_BASE_URL='http://127.0.0.1:'+match[-1], ACTUAL_DISPOSABLE_DATABASE='true',
               ACTUAL_BUILD_COMMIT=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
               T17_PG_TEST='true', T17_TEST_EVIDENCE=str(OUT))
    run([str(ROOT/'mvnw'), '-B', '-ntp', '-f', 'verification/harness/pom.xml', '-Dtest=T17SourceFactsPostgresTest',
         '-Dverification.actual.identityBinding=verification/actual/scenarios/identity-binding.json',
         '-Dverification.actual.blobRoot='+str(OUT/'blobs'), 'test'], 'test.log', env)
    print('PASS T17 source PostgreSQL integration:', OUT)
finally:
    if server:
        server.terminate()
        try: server.wait(timeout=15)
        except subprocess.TimeoutExpired: server.kill(); server.wait(timeout=5)
    if created:
        with (OUT/'postgres.log').open('w') as stream: subprocess.run(['docker','logs',container],stdout=stream,stderr=subprocess.STDOUT)
        subprocess.run(['docker','rm','-f','-v',container],stdout=subprocess.DEVNULL,check=True)
    # Authentication materials are ephemeral and never part of evidence.
    for name in ['private.pem', 'public.pem']:
        (OUT/name).unlink(missing_ok=True)
    (OUT/'commands.json').write_text(json.dumps(records,indent=2)+'\n')
    print('Evidence:', OUT)
