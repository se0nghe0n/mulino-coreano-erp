#!/usr/bin/env python3
"""Source/artifact custody receipt; no credentials or environment dump."""
import hashlib, json, pathlib, subprocess, sys, datetime

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def inputs(root):
    names=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
    return {name:sha(root/name) for name in sorted(names) if name and (root/name).is_file()}

def main():
    mode,root_arg,out_arg=sys.argv[1:4]
    root=pathlib.Path(root_arg);out=pathlib.Path(out_arg)
    if mode=='start':
        status=subprocess.check_output(['git','status','--porcelain'],cwd=root).decode()
        receipt={'recordType':'S3_DISPOSABLE_RUN_RECEIPT','status':'NOT_RUN','gateComplete':False,
            'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'codeCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root).decode().strip(),
            'sourceCleanBefore':not bool(status),'sourceStatusBefore':status,'sourceInputHashesBefore':inputs(root),
            'executedJarSha256':sha(root/'backend/target/ontology-0.1.0-SNAPSHOT.jar'),
            'fixtureTemplateSha256':sha(root/'verification/actual/s3/fixture.json'),
            'harnessMainClassSha256':sha(root/'verification/harness/target/classes/org/mulino/verification/actual/NativeS3TradeMain.class')}
        custody=json.loads((root/'verification/harness/target/s3-build-custody.json').read_text())
        if custody['sourceCommit']!=receipt['codeCommit'] or custody['sourceInputHashes']!=receipt['sourceInputHashesBefore'] or custody['jarSha256']!=receipt['executedJarSha256']:
            raise SystemExit('S3 build custody does not match source/JAR; run the recorded clean build first')
        actual_classes={str(p.relative_to(root)):sha(p) for p in sorted((root/'verification/harness/target/classes/org/mulino/verification/actual').rglob('*.class'))}
        if custody['harnessClasses']!=actual_classes: raise SystemExit('S3 harness class custody mismatch')
        receipt['buildCustody']=custody
        (out/'run-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
        if status: raise SystemExit('Actual runner requires committed clean source')
    else:
        receipt=json.loads((out/'run-receipt.json').read_text());after=inputs(root)
        status_after=subprocess.check_output(['git','status','--porcelain'],cwd=root).decode()
        receipt.update({'sourceStatusAfter':status_after,'sourceCleanAfter':not bool(status_after),'finishedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'sourceInputHashesAfter':after,'sourceDrift':receipt['sourceInputHashesBefore']!=after or bool(status_after),
            'jarSha256After':sha(root/'backend/target/ontology-0.1.0-SNAPSHOT.jar'),
            'exitCode':int(sys.argv[4]),'phase':sys.argv[5],
            'cleanup':{'backendStopped':sys.argv[6]=='true','createdContainerRemoved':sys.argv[7]=='true','ephemeralKeyDirectoryRemoved':sys.argv[8]=='true','ephemeralBlobDirectoryRemoved':sys.argv[8]=='true'}})
        native=out/'actual-s3-native.json'
        if native.exists():
            result=json.loads(native.read_text());receipt['nativeStatus']=result.get('status');receipt['fixedAuthorityInstant']='2026-10-07T09:00:00Z';receipt['effectiveFixtureSha256']=result.get('fixtureHash')
        if receipt.get('nativeStatus')=='FAIL':
            receipt['status']='FAIL';receipt['exitCode']=1
        elif receipt['exitCode']==0 and receipt.get('nativeStatus')=='PASS':
            receipt['status']='PASS'
        else:
            receipt['status']='NOT_RUN';receipt['exitCode']=2 if receipt.get('nativeStatus')=='NOT_RUN' else 3
        if receipt['sourceDrift'] or receipt['jarSha256After']!=receipt['executedJarSha256'] or not all(receipt['cleanup'].values()):
            receipt['status']='FAIL';receipt['exitCode']=1
        receipt['artifactHashes']={str(p.relative_to(out)):sha(p) for p in sorted(out.rglob('*')) if p.is_file() and p.name!='run-receipt.json'}
        (out/'run-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
        raise SystemExit(receipt['exitCode'])
if __name__=='__main__':main()
