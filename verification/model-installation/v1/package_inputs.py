#!/usr/bin/env python3
"""Package only the separately authored inputs; original artifacts are read/hash checked, never rewritten."""
import gzip,io,json,pathlib,tarfile
from inventory import ROOT,HERE,verify,sha
verify();index=[];files=[]
for p in sorted((HERE/'inputs').glob('M*-isolated.json')):
 j=json.loads(p.read_text());index.append(dict(fixtureId=p.stem,fixtureSha256=j['fixtureSha256'],inputSha256=sha(p),recordCount=len(j['records'])));files.append(p)
files+=sorted((HERE/'generated').glob('M*/*.json'))
raw=io.BytesIO()
with tarfile.open(fileobj=raw,mode='w') as archive:
 for p in files:
  data=p.read_bytes();info=tarfile.TarInfo(str(p.relative_to(ROOT)));info.size=len(data);info.mode=0o444;info.mtime=0;archive.addfile(info,io.BytesIO(data))
(HERE/'installation-inputs.tar.gz').write_bytes(gzip.compress(raw.getvalue(),mtime=0))
(HERE/'generated-input-index.json').write_text(json.dumps(dict(schemaVersion='model-installation-input-index-v1',fixtureCount=len(index),archiveSha256=sha(HERE/'installation-inputs.tar.gz'),generatedSourceCount=len(files)-len(index),inputs=index),ensure_ascii=False,indent=2)+'\n');print(len(index),len(files),sha(HERE/'installation-inputs.tar.gz'));verify()
