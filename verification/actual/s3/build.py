#!/usr/bin/env python3
"""Build exact committed source and record custody; invoke only within a granted Maven slot."""
import hashlib,json,pathlib,subprocess,sys,datetime
root=pathlib.Path(__file__).resolve().parents[3]
sys.dont_write_bytecode=True
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from receipt import inputs,sha
if subprocess.check_output(['git','status','--porcelain'],cwd=root).strip():
    raise SystemExit('S3 actual build requires committed clean source')
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root).decode().strip()
before=inputs(root)
commands=[['./mvnw','-B','-ntp','-f','backend/pom.xml','-DskipTests','package'],
          ['./mvnw','-B','-ntp','-f','verification/harness/pom.xml','-Dmaven.test.skip=true','package','dependency:build-classpath','-Dmdep.outputFile=target/classpath.txt']]
for command in commands:
    code=subprocess.run(command,cwd=root).returncode
    if code:raise SystemExit(code)
if inputs(root)!=before or subprocess.check_output(['git','status','--porcelain'],cwd=root).strip():
    raise SystemExit('Source drift during S3 actual build')
receipt={'sourceCommit':commit,'sourceInputHashes':before,'buildCommands':commands,
    'jarSha256':sha(root/'backend/target/ontology-0.1.0-SNAPSHOT.jar'),
    'builtAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'harnessClasses':{str(p.relative_to(root)):sha(p) for p in sorted((root/'verification/harness/target/classes/org/mulino/verification/actual').rglob('*.class'))}}
p=root/'verification/harness/target/s3-build-custody.json';p.write_text(json.dumps(receipt,indent=2)+'\n')
