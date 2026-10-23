#!/usr/bin/env python3
"""Compare the authored pin follow-up with its exact prior input archive; never changes original inputs."""
import argparse, copy, hashlib, json, pathlib, tarfile
from inventory import ROOT, HERE, sha, verify

p=argparse.ArgumentParser();p.add_argument('baseline_archive');p.add_argument('output');a=p.parse_args()
def members(path):
 with tarfile.open(path,mode='r:gz') as archive:
  return {entry.name:archive.extractfile(entry).read() for entry in archive.getmembers() if entry.isfile()}
baseline=members(a.baseline_archive);current=members(HERE/'installation-inputs.tar.gz')
snapshot='verification/model-installation/v1/capability-contract-a7f18cc8.json'
assert set(current)-set(baseline)=={snapshot} and not set(baseline)-set(current)
assert current[snapshot]==(ROOT/snapshot).read_bytes()
cases=[];source_unchanged=0
for path,before_bytes in baseline.items():
 after_bytes=current[path]
 if '/inputs/' not in path:
  assert before_bytes==after_bytes,path
  source_unchanged+=1;continue
 before=json.loads(before_bytes);after=json.loads(after_bytes);normalized=copy.deepcopy(after)
 assert normalized['authorityCorrection'].pop('capabilityContractPath')==snapshot
 assert normalized['templateArtifacts'][1]['path']==snapshot
 normalized['templateArtifacts'][1]['path']='contracts/acceptance-capabilities.json'
 assert before==normalized,path
 cases.append(dict(caseId=path.split('/')[-1][:3],fixtureSha256=after['fixtureSha256'],
                   beforeInputSha256=hashlib.sha256(before_bytes).hexdigest(),
                   afterInputSha256=hashlib.sha256(after_bytes).hexdigest(),
                   changedPointers=['/authorityCorrection/capabilityContractPath','/templateArtifacts/1/path'],
                   recordsUnchanged=True,grantContentsUnchanged=True,originalFixtureHashUnchanged=True))
assert len(cases)==60
result=dict(evidenceClass='EXACT_INSTALLATION_INPUT_PIN_DELTA',originalArtifactsVerified=verify(),
            baselineArchiveSha256=sha(pathlib.Path(a.baseline_archive)),
            currentArchiveSha256=sha(HERE/'installation-inputs.tar.gz'),capabilityContractPath=snapshot,
            capabilityContractSha256=sha(ROOT/snapshot),unchangedSourceFiles=source_unchanged,cases=cases)
pathlib.Path(a.output).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(dict(caseCount=len(cases),unchangedSourceFiles=source_unchanged,originalArtifactsVerified=result['originalArtifactsVerified'],currentArchiveSha256=result['currentArchiveSha256'])))
