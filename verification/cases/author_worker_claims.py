#!/usr/bin/env python3
"""Add explicit production lease setup to C3/V4/V7 worker calls; never synthesize a claim.

Run after the per-case generators. Owns worker-claim-* setup/assertions and
harness.workerClaim only; keeps every existing business/read oracle intact.
"""
import copy
import importlib.util
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
def load(name, path):
    spec=importlib.util.spec_from_file_location(name,ROOT/path); module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
C3=load('worker_c3','verification/cases/C3/author_prerequisites.py')
BIND=load('worker_bind','verification/cases/V7/bind_observations.py')
def patch(case):
    for sub in case['subcases']:
        sub['actions']=[a for a in sub['actions'] if not a['id'].startswith(('worker-claim-','worker-negative-'))]
        sub['assertions']=[a for a in sub['assertions'] if not a['id'].startswith(('worker-claim-','worker-negative-'))]
        if case['caseId']=='V4' and sub['id']=='worker-work':
            # This route-family probe requires an existing Work-scoped command; createDraft has no WORK scope.
            sub['fixtureRef']='verification/cases/C3/fixture-waitWork.json'
            for action in sub['actions']:
                if action['id'] in ('attempt','authorized-same-route'):
                    action['capabilityId']='waitWork'
                    action['request'].update(capabilityId='waitWork',subjectRefs=[{'type':'Work','id':{'$alias':'WORK'}}],
                        slots={'workId':{'$alias':'WORK'},'wait':{'reason':'현재 근거를 확인할 때까지 기다린다','target':{'$alias':'WORK'},
                            'resumePredicate':{'kind':'evidenceAvailable','occurrenceId':{'$alias':'OCCURRENCE'}},
                            'verifier':{'$alias':'warehouse'},'nextCheckAt':'2026-10-07T10:00:00Z','overdueAction':'담당자가 확인한다'}},
                        provenance={'workId':'CONTEXT','wait':'USER'})
        claims=[]
        for action in sub['actions']:
            call=action.get('call',action)
            # Exercise the existing current-authorization races through the public worker endpoint.
            if case['caseId']=='V7' and sub['id'] in ('authorization-revoke-first','authorization-effect-first') and call.get('capabilityId')=='dispatchQuantity':
                call['route']='worker'
                call.setdefault('harness',{}).setdefault('executionContext',{})['requestedExecution']='DURABLE_WORKER'
            if call.get('route')!='worker':continue
            name='worker-claim-'+action['id']
            intent=call['request']
            if case['caseId']=='C3' and sub['id']=='worker-waitWork':
                intent['slots']['wait']['target']={'$alias':'WORK'}
            claims.append({'id':name,'kind':'control','control':{'type':'workerClaim','operation':'issue','parameters':{
                'actorRef':call['actorRef'],'workId':{'$alias':'WORK'},'capabilityId':call['capabilityId'],
                'commandId':intent['commandIdempotencyKey'],'workerId':'case-worker','ttlSeconds':300}},'evidenceRefs':[name+':actual-artifact']})
            call.setdefault('harness',{})['workerClaim']={'$result':{'actionId':name,'pointer':'/response/claim'}}
            assertion=copy.deepcopy(sub['assertions'][0]);assertion.update(id=name+'-issued',op='equals',source={'actionId':name,'pointer':'/response/issued'},expected=True,evidenceRefs=[name+':actual-artifact'],oracleExplanation='인증된 actor로 production RuntimeRepository가 실제 lease를 저장하고 반환했는지 확인한다.')
            for k in ('baseline','unit','unitSource','baselineUnitSource'):assertion.pop(k,None)
            sub['assertions'].append(assertion)
        if claims:
            sub['actions'][1:1]=claims
            if 'worker' not in sub['requiredAdapters']:sub['requiredAdapters'].append('worker')
            if case['caseId']=='V4' and sub['id']=='worker-work':
                add_negative_claims(sub)
    return case

def add_negative_claims(sub):
    # One cumulative before/after oracle covers every rejected call, using a command-authorized actor.
    call=next(a for a in sub['actions'] if a['id']=='authorized-same-route')
    expired='worker-negative-expired-issue'
    setup={'id':expired,'kind':'control','control':{'type':'workerClaim','operation':'issue','parameters':{
        'actorRef':call['actorRef'],'workId':{'$alias':'WORK'},'capabilityId':call['capabilityId'],
        'commandId':'V4-worker-expired','workerId':'case-worker','ttlSeconds':1}},'evidenceRefs':[expired+':actual-artifact']}
    clock={'id':'worker-negative-expire-clock','kind':'control','control':{'type':'clock','operation':'advanceTo','parameters':{'instant':'2026-10-07T09:00:01Z'}},'evidenceRefs':['worker-negative-expire-clock:actual-artifact']}
    sub['actions'][1:1]=[setup,clock]
    # Business observations are made at the new authority clock; the original fixture remains at 09:00:00.
    def retime(value):
        if isinstance(value,dict):
            for k,v in value.items():
                if k in ('asOf','knownAt') and v=='2026-10-07T09:00:00Z':value[k]='2026-10-07T09:00:01Z'
                else:retime(v)
        elif isinstance(value,list):
            for v in value:retime(v)
    retime(sub)
    negatives=[]
    for kind in ('missing','forged','expired'):
        negative=copy.deepcopy(call);negative['id']='worker-negative-'+kind
        negative['request']['commandIdempotencyKey']='V4-worker-'+kind
        negative.pop('harness',None);negative['evidenceRefs']=[negative['id']+':actual-artifact']
        if kind=='forged':negative['harness']={'workerClaim':{'claimId':'00000000-0000-0000-0000-000000000001','fencingToken':1}}
        if kind=='expired':negative['harness']={'workerClaim':{'$result':{'actionId':expired,'pointer':'/response/claim'}}}
        negatives.append(negative)
        check=copy.deepcopy(sub['assertions'][0]);check.update(id=negative['id']+'-denied',op='equals',source={'actionId':negative['id'],'pointer':'/response/error/code'},expected='FORBIDDEN',evidenceRefs=negative['evidenceRefs'],oracleExplanation='COMMAND 권한이 있는 actor라도 누락·위조·만료 claim은 실제 worker endpoint에서 효과 없이 거부한다.')
        sub['assertions'].append(check)
    index=next(i for i,a in enumerate(sub['actions']) if a['id']=='attempt')
    sub['actions'][index:index]=negatives

def write_case(cid):
    directory=ROOT/'verification/cases'/cid;path=directory/'case.json';case=patch(json.loads(path.read_text()))
    path.write_text(C3.render_case(case))
    feature=directory/'scenario.feature';old=feature.read_text();previous={};sid=None
    import re
    for line in old.splitlines():
        m=re.search(r'의 "([^"]+)"를 준비한다$',line)
        if m:sid=m[1]
        m=re.match(r'    그러면 "([^"]+)" assertion으로 "(.*)"를 확인한다$',line)
        if m:previous[(sid,m[1])]=m[2]
    lines=old.splitlines()[:3]
    for sub in case['subcases']:
        lines+=['','  @uat @model' if sub['id'].startswith('model-query') else '  @sit','  시나리오: '+sub['title'],f'    먼저 사례 파일 "verification/cases/{cid}/case.json"의 "{sub["id"]}"를 준비한다']
        lines += [f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in sub['actions']]
        lines += [f'    그러면 "{a["id"]}" assertion으로 "{previous.get((sub["id"],a["id"]),a["oracleExplanation"])}"를 확인한다' for a in sub['assertions']]
    feature.write_text(C3.render_feature(case,old.splitlines()[:3]) if cid=='C3' else '\n'.join(lines)+'\n')
    (directory/'observation-bindings.json').write_text(BIND.render(BIND.build(cid)))
if __name__=='__main__':
    for cid in ('C3','V4','V7'):write_case(cid)
