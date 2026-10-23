#!/usr/bin/env python3
"""T11 전용 결정적 HTTP/PG 검사. 실제 scenario/model runner를 호출하지 않는다."""
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import uuid

ROOT=Path(__file__).resolve().parents[3]
OUT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT/'.local'/('t11-http-'+str(uuid.uuid4()))
OUT.mkdir(parents=True,exist_ok=False)
env=dict(os.environ,JAVA_HOME='/opt/homebrew/opt/openjdk@21')
env['PATH']=env['JAVA_HOME']+'/bin:'+env['PATH']
container='mulino-t11-'+str(uuid.uuid4());tmp=Path(tempfile.mkdtemp(prefix='mulino-t11-'))
server=None;created=False;result=3;record={'taskStatus':'OPEN','scenarioRunnerUsed':False,'modelCalls':0,'commands':[]}

def run(name,args,check=True):
    with (OUT/(name+'.log')).open('w') as log:
        completed=subprocess.run(args,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    record['commands'].append({'name':name,'command':args,'exit':completed.returncode})
    if check and completed.returncode:raise RuntimeError(name+' exited '+str(completed.returncode))
    return completed.returncode

try:
    run('npm-ci',['npm','--prefix','backend','ci'])
    run('backend-build',['./mvnw','-B','-ntp','-f','backend/pom.xml','-DskipTests','package'])
    jar=ROOT/'backend/target/ontology-0.1.0-SNAPSHOT.jar'
    record['sourceCommit']=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    record['sourceDirty']=bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True))
    record['jarSha256']=hashlib.sha256(jar.read_bytes()).hexdigest()
    record['csnSha256']=hashlib.sha256((ROOT/'backend/src/main/resources/edmx/csn.json').read_bytes()).hexdigest()
    run('keygen',['openssl','genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:2048','-out',str(tmp/'private.pem')])
    run('public-key',['openssl','pkey','-in',str(tmp/'private.pem'),'-pubout','-out',str(tmp/'public.pem')])
    env.update(JWT_PRIVATE_KEY=str(tmp/'private.pem'),JWT_PUBLIC_KEY=str(tmp/'public.pem'),JWT_ISSUER='https://mulino-native.invalid',JWT_AUDIENCE='isolated-ontology',DB_USERNAME='postgres',DB_PASSWORD=secrets.token_urlsafe(24))
    image=json.loads((ROOT/'verification/platform/versions.json').read_text())['postgresImage']['reference']
    # Password goes through the process environment, never a logged argument.
    env['POSTGRES_PASSWORD']=env['DB_PASSWORD']
    run('docker-start',['docker','run','-d','--name',container,'-e','POSTGRES_PASSWORD','-e','POSTGRES_DB=ontology','-p','127.0.0.1::5432',image]);created=True
    port=subprocess.check_output(['docker','port',container,'5432/tcp'],text=True).strip().rsplit(':',1)[1]
    env['DB_URL']='jdbc:postgresql://127.0.0.1:'+port+'/ontology'
    for attempt in range(120):
        if subprocess.run(['docker','exec',container,'pg_isready','-h','127.0.0.1','-U','postgres','-d','ontology'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:break
        time.sleep(.25)
    else:raise RuntimeError('PostgreSQL readiness')
    args=['java','-jar',str(jar),'--spring.profiles.active=local,verification','--mulino.verification.instant=2026-10-07T02:00:00Z','--server.address=127.0.0.1','--server.port=0','--mulino.evidence.blob-root='+str(tmp/'blobs')]
    with (OUT/'backend-runtime.log').open('w') as log:server=subprocess.Popen(args,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    for attempt in range(480):
        if server.poll() is not None:raise RuntimeError('Backend startup exited')
        matches=re.findall(r'Tomcat started on port (\d+)',(OUT/'backend-runtime.log').read_text())
        if matches:break
        time.sleep(.25)
    else:raise RuntimeError('Backend readiness')
    env.update(ACTUAL_BASE_URL='http://127.0.0.1:'+matches[-1],ACTUAL_DISPOSABLE_DATABASE='true',ACTUAL_BUILD_COMMIT=record['sourceCommit'],T11_PG_TEST='true',T11_TEST_EVIDENCE=str(OUT/'http-pg'))
    result=run('t11-http-pg',['./mvnw','-B','-ntp','-f','verification/harness/pom.xml','-Dverification.actual.identityBinding=verification/actual/scenarios/identity-binding.json','-Dverification.actual.blobRoot='+str(tmp/'blobs'),'-Dtest='+env.get('T11_TEST_SELECT','T11ParentContributionPostgresTest'),'test'],check=False)
except Exception as failure:
    record['environmentFailure']=str(failure)
finally:
    if server is not None:
        server.terminate()
        try:server.wait(timeout=20)
        except subprocess.TimeoutExpired:server.kill();server.wait()
    record['backendStopped']=server is None or server.poll() is not None
    if created:
        run('postgres-runtime',['docker','logs',container],False)
        record['containerRemoved']=run('docker-cleanup',['docker','rm','-f','-v',container],False)==0
    shutil.rmtree(tmp);record['ephemeralKeysRemoved']=not tmp.exists();record['exit']=result
    (OUT/'run.json').write_text(json.dumps(record,indent=2)+'\n')
print(str(OUT/'run.json'));sys.exit(result)
