#!/usr/bin/env python3
"""공개 명령의 검증된 입력과 current revision을 연결하는 T11 가역 입력 계층이다."""
import copy
import hashlib
import json
import subprocess
from pathlib import Path

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE='477bba574e9ac6c4be32b236d93164f763b12401'
PARENTS=('parent90-cancelled','parent90-impossible','parent90-superseded')
SELECTED=('shared-distinct60','wait-timeout','pending-correction')

def original_bytes(name):
    return subprocess.check_output(['git','show',BASE+':verification/cases/T11/'+name],cwd=ROOT)

def baseline():return json.loads(original_bytes('case.json'))
def digest(v):return hashlib.sha256(json.dumps(v,ensure_ascii=False,sort_keys=True).encode()).hexdigest()
def ref(a,p):return {'$result':{'actionId':a,'pointer':p}}
def query(a,work,actor='A'):
    return {'id':a,'kind':'query','actorRef':actor,'route':'api','capabilityId':'getWork',
            'request':{'workId':copy.deepcopy(work),'scope':{'organizationId':{'$alias':'ORG'}}},'evidenceRefs':[a+':actual-artifact']}

def authored(old):
    value=copy.deepcopy(old);changes=[]
    for sub in value['subcases']:
        if sub['id'] not in SELECTED:continue
        output=[]
        for a in sub['actions']:
            before=copy.deepcopy(a);inserted_before=[];inserted_after=[]
            if sub['id']=='wait-timeout' and a['id']=='resume-without-proof':
                r=a['request'];slots=r['slots']
                assert slots.pop('resumeEvidenceIds')==[]
                # Lifecycle requires an evidence object; preserve the original absence of proof.
                slots['evidence']={};r['provenance']['evidence']=r['provenance'].pop('resumeEvidenceIds')
                q=a['id']+'-current-work';inserted_before.append(query(q,slots['workId']))
                r['expectedRevision']=ref(q,'/response/data/revision')
            elif sub['id']=='pending-correction' and a['id']=='close':
                q='close-current-work';inserted_before.append(query(q,a['request']['slots']['workId']))
                a['request']['expectedRevision']=ref(q,'/response/data/revision')
            elif sub['id']=='shared-distinct60' and a['id'] in ('parent-one-link','parent-two-link'):
                source=next(x for x in sub['actions'] if x['id']=='receive60-event')['request']['slots']['payload']['$encode']['source']
                assert source['startQuantity']=='0' and source['quantity']=='60' and source['unit']=='BOX'
                r=a['request'];s=r['slots'];label=s.pop('physicalScope')
                start='0' if a['id']=='parent-one-link' else '30'
                assert label==('serial-boxes-001-030' if start=='0' else 'serial-boxes-031-060')
                assert s['quantity']=={'value':'30','unit':'BOX'}
                s['startQuantity']=start;r['provenance']['startQuantity']=r['provenance'].pop('physicalScope')
                # prepare fences the actual activity's source Work for both links.
                q=a['id']+'-current-work';inserted_before.append(query(q,source['workId']))
                r['expectedRevision']=ref(q,'/response/data/revision')
            output.extend(inserted_before+[a]+inserted_after)
            if a!=before or inserted_before or inserted_after:
                changes.append({'subcaseId':sub['id'],'actionId':a['id'],'before':before,'after':copy.deepcopy(a),
                                'insertedBefore':inserted_before,'insertedAfter':inserted_after})
        sub['actions']=output
    return value,changes

def apply(v):
    old=baseline();new,_=authored(old)
    if v==new:return copy.deepcopy(v)
    if v!=old:raise ValueError('T11 runtime input baseline drift')
    return new

def restore(v):
    old=baseline();new,_=authored(old)
    if v!=new:raise ValueError('T11 runtime strict inverse refuses unrecorded input/oracle drift')
    return old

def feature():
    text=original_bytes('scenario.feature').decode();_,changes=authored(baseline())
    for sid in SELECTED:
        start=text.index('"'+sid+'"를 준비한다');end=text.find('\n  시나리오:',start)
        if end<0:end=len(text)
        block=text[start:end]
        for c in changes:
            if c['subcaseId']!=sid:continue
            old=f'    만일 "{c["before"]["actorRef"]}" 역할이 "{c["actionId"]}" 행동을 수행한다\n'
            assert block.count(old)==1
            def lines(actions):return ''.join(f'    만일 "{a["actorRef"]}" 역할이 "{a["id"]}" 행동을 수행한다\n' for a in actions)
            block=block.replace(old,lines(c['insertedBefore'])+old+lines(c['insertedAfter']))
        text=text[:start]+block+text[end:]
    return text

def restore_feature(text):
    if text!=feature():raise ValueError('T11 runtime feature drift')
    return original_bytes('scenario.feature').decode()

def main():
    v=apply(json.loads((HERE/'case.json').read_text()));_,changes=authored(baseline())
    (HERE/'case.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
    (HERE/'scenario.feature').write_text(feature())
    ledger={'recordType':'T11_RUNTIME_INPUT_AUTHORING','baseline':BASE,'assertionsUnchanged':110,
            'beforeSha256':digest(baseline()),'afterSha256':digest(v),'changes':changes,
            'originalGrantsUnchanged':True,'newSourceFacts':False,
            'authorityFrontier':'No already-public, originally-authorized command creates proven goal shortfall10. Parent subcases remain byte/equality identical to baseline; no alternate duty is fabricated.',
            'supersedesInputCommit':'850496aa',
            'parentSubcasesRestoredToBaseline':list(PARENTS)}
    (HERE/'runtime-input-ledger.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
