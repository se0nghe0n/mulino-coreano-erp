"""새로 승인한 T17 synthetic 날짜·category 권한·후속 원문을 작성한다.

2348dfd3의 원래 값과 사업 oracle은 별도 가역 ledger로 보존한다.
"""
import copy
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID, uuid5

HERE=Path(__file__).resolve().parent
BASE='2348dfd312579412874398937773f6f4c0f2b96f'
META='completionInputAuthoring'
PROVENANCE={'synthetic':True,'authoredBy':'coordinator-selected-fixture',
            'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}


def instant(value):return datetime.fromisoformat(value.replace('Z','+00:00'))


def chain(contract,sub,prefix,original,kind,fixture):
    """Original -> actual upload/event/claim/review/canonical; no state seed."""
    event_id=str(uuid5(UUID('316f409b-fc7a-42b4-a572-4fc130dafbad'),sub+':'+prefix))
    original=dict(PROVENANCE,**original,sourceDecisionId=event_id,sourceVersion='1')
    subject={'kind':'SEGMENT','id':contract.alias('A')}
    at=fixture['clock']['asOf'];quantity=original['quantity'];unit=original['unit']
    result=contract.source_actions(sub,prefix,subject,original,kind,at,quantity,unit,external_id=event_id)
    result[0]['request']['slots']['provenance']='NEW_COORDINATOR_SYNTHETIC_ACTION_SOURCE; original evidence descriptor retained; operatingApprovalClaimed=false'
    result[1]['request']['slots']['assertion']='New synthetic action proposal source; neither the original evidence nor a committed business effect is asserted'
    result += [contract.command(sub,prefix+'-match','matchSourceIdentity',{
        'claimId':contract.ref(prefix+'-event','claimId'),
        'basisDocumentId':contract.ref(prefix+'-document','id'),
        'physicalScopeId':contract.alias('A'),'policyVersion':contract.POLICY,
        'sourceIdentity':contract.NAMESPACE+':'+event_id+':1',
        'quantity':quantity,'unit':unit,'effectiveFrom':at,
        'reason':'새 synthetic 후속 요청 원문과 원래 실물 범위·수량을 대조한다'},revision=1),
        contract.command(sub,prefix+'-canonical','linkCanonicalOccurrence',{
        'reconciliationId':contract.ref(prefix+'-match','id')},revision=1)]
    profile=next(p for p in fixture['baseline']['sourceProfiles'] if p['namespace']==contract.NAMESPACE)
    if kind not in profile['eventKinds']:profile['eventKinds'].append(kind)
    return result


def apply(case,fixtures,contract):
    for sub in case['subcases']:
        f=fixtures[sub['id']]
        if META in f['baseline']['policy']:raise ValueError('Restore completion layer before authoring')
        decisions=[]
        for action in sub['actions']:
            if action.get('capabilityId')=='createSalesOrder' and 'dueAt' not in action['request']['slots']:
                at=instant(f['clock']['asOf']);due=at+timedelta(hours=1)
                limits=[instant(c['validUntil']) for c in f['baseline']['conditions']]
                limits += [instant(a['expiresAt']) for a in f['aliases'].values() if a.get('expiresAt') and a['expiresAt']!='UNKNOWN']
                if not at<due<min(limits):raise ValueError('Chosen synthetic dueAt must precede every original condition/LOT expiry')
                value=due.isoformat().replace('+00:00','Z')
                action['request']['slots']['dueAt']=value
                action['request']['provenance']['dueAt']='CONTEXT'
                decisions.append(dict(PROVENANCE,kind='NEW_DUE_AT',actionId=action['id'],originalValuePresent=False,value=value,
                    selection='original fixture asOf + 1 hour; strictly before original condition and LOT expiry',
                    originalAsOf=f['clock']['asOf'],earliestOriginalBoundary=min(limits).isoformat().replace('+00:00','Z')))
            if action.get('capabilityId')=='createSalesOrder' and 'packageTerms' not in action['request']['slots']:
                item=action['request']['slots']['itemId']['$alias']
                value=f['aliases'][item]['packagingVersion']
                action['request']['slots']['packageTerms']=value
                action['request']['provenance']['packageTerms']='CONTEXT'
                decisions.append(dict(PROVENANCE,kind='NEW_PACKAGE_TERMS',actionId=action['id'],originalValuePresent=False,
                    value=value,selection='new synthetic order term referencing the original TradeItem packagingVersion',itemAlias=item))
            if action.get('capabilityId')=='placeHold':
                actor=action['actorRef'];cap='decideRecallRestriction' if action['request']['slots']['category']=='RECALL' else 'decideQuality'
                a=f['actors'][actor]
                # The sole addition is this category prerequisite. Every original
                # identity, scope dimension, validity and business capability stays.
                if cap in a['roleCapabilities'] or cap in a['grant']['actions']:raise ValueError('Category permission already present in completion baseline')
                before=copy.deepcopy(a)
                a['roleCapabilities'].append(cap);a['grant']['actions'].append(cap)
                decisions.append(dict(PROVENANCE,kind='NEW_CATEGORY_CAPABILITY',actorAlias=actor,capability=cap,
                    originalActor=before,scope=copy.deepcopy(a['grant']['scope']),validFrom=a['grant']['validFrom'],validUntil=a['grant']['validUntil'],
                    selection='only category prerequisite of original positive hold; original all-of scope and validity unchanged'))
        if sub['id']=='replacement-no-revival':
            actions={a['id']:a for a in sub['actions']}
            hold_query={'id':'hold-current-segment','kind':'query','actorRef':'observer','route':'api',
                'capabilityId':'getObject','request':{'id':contract.alias('A'),
                'scope':{'organizationId':contract.alias('ORG'),'itemId':contract.alias('P'),'objectType':'QuantitySegment'},
                'asOf':f['clock']['asOf'],'knownAt':f['clock']['knownAt']},
                'evidenceRefs':['hold-current-segment:actual-query-artifact']}
            actions['hold']['request']['expectedRevision']=contract.ref('hold-current-segment','data/revision')
            original_replace=copy.deepcopy(actions['replace']['request'])
            original_release=copy.deepcopy(actions['release-hold']['request'])
            alias=original_replace['slots']['evidenceId']['$alias']
            descriptor=copy.deepcopy(next(e for e in f['evidence'] if e['alias']==alias))
            common={'segmentId':contract.alias('A'),'oldSegmentId':contract.alias('A'),'newSegmentId':contract.alias('B'),
                'quantity':actions['replace']['request']['slots']['quantity'],'unit':actions['replace']['request']['slots']['unit'],
                'workId':contract.ref('order','workId'),'salesLineId':contract.ref('order','line/id'),
                'oldAllocationId':contract.ref('reserve','effects/allocationId')}
            replacement=dict(common,witnessKind='replacement-proof',operation='replaceAllocation',
                originalRequest=original_replace,originalEvidenceDescriptor=descriptor,originalEvidenceId=contract.alias(alias),
                proposalOnly=True,committedEffectClaimed=False)
            allocation_query={'id':'replace-source-allocation','kind':'query','actorRef':'observer','route':'api',
                'capabilityId':'getObject','request':{'id':contract.ref('reserve','effects/allocationId'),
                'scope':{'organizationId':contract.alias('ORG'),'itemId':contract.alias('P'),'objectType':'Allocation'},
                'asOf':f['clock']['asOf'],'knownAt':f['clock']['knownAt']},
                'evidenceRefs':['replace-source-allocation:actual-query-artifact']}
            replace_source=[allocation_query]+chain(contract,sub['id'],'replace-source',replacement,'T17_REPLACEMENT_PROPOSAL',f)
            actions['replace']['request']['expectedRevision']=contract.ref('replace-source-allocation','data/revision')
            actions['allocation-release']['request']['expectedRevision']=contract.ref('replace','revision')
            actions['replace']['request']['slots']['evidenceId']=contract.ref('replace-source-document','id')
            actions['replace']['request']['provenance']['evidenceId']='CONTEXT'
            reason='원래 QC 검토 보류를 해제하고 대체 예약은 유지한다'
            alias=original_release['slots']['evidenceId']['$alias']
            release=dict(common,operation='releaseHold',restrictionId=contract.ref('hold','effects/restrictionId'),reason=reason,
                originalRequest=original_release,originalEvidenceDescriptor=copy.deepcopy(next(e for e in f['evidence'] if e['alias']==alias)),
                originalEvidenceId=contract.alias(alias),proposalOnly=True,committedEffectClaimed=False)
            release_source=chain(contract,sub['id'],'release-hold-source',release,'T17_RELEASE_HOLD_SOURCE',f)
            actions['release-hold']['request']['slots'].update(reason=reason,evidenceId=contract.ref('release-hold-source-document','id'))
            actions['release-hold']['request']['provenance'].update(reason='CONTEXT',evidenceId='CONTEXT')
            output=[]
            for action in sub['actions']:
                if action['id']=='hold':output.append(hold_query)
                if action['id']=='replace':output.extend(replace_source)
                if action['id']=='release-hold':output.extend(release_source)
                output.append(action)
            sub['actions']=output
            decisions.append(dict(PROVENANCE,kind='NEW_REPLACEMENT_AND_RELEASE_SOURCES',
                originalReplacementRequest=original_replace,originalReleaseRequest=original_release,
                reason=reason,selection='original A20 -> B20 replacement; release original hold with exact actual restriction identity'))
        if decisions:f['baseline']['policy'][META]=dict(PROVENANCE,decisions=decisions)
    return case,fixtures


def ledger():return json.loads((HERE/'completion-input-delta.json').read_text())


def restore_case(case):
    case=copy.deepcopy(case)
    for sub in case['subcases']:
        changes={x['actionId']:x for x in ledger()['actionChanges'] if x['subcase']==sub['id']}
        result=[]
        for a in sub['actions']:
            change=changes.get(a['id'])
            if change:
                if a!=change['after']:raise ValueError('Completion action drift: '+a['id'])
                if change['before'] is not None:result.append(copy.deepcopy(change['before']))
            else:result.append(a)
        sub['actions']=result
    return case


def restore_fixture(f):
    f=copy.deepcopy(f)
    for change in reversed(ledger()['fixtureChanges'].get(f['fixtureId'],[])):
        obj=f;keys=change['path'].split('/')
        for k in keys[:-1]:obj=obj[int(k)] if isinstance(obj,list) else obj[k]
        key=int(keys[-1]) if isinstance(obj,list) else keys[-1]
        if obj[key]!=change['after']:raise ValueError('Completion fixture drift: '+change['path'])
        if change['hadBefore']:obj[key]=copy.deepcopy(change['before'])
        else:del obj[key]
    return f


def restore_feature(text):
    added={x['actionId'] for x in ledger()['actionChanges'] if x['before'] is None}
    return ''.join(line for line in text.splitlines(keepends=True) if not any(f'역할이 "{a}" 행동을 수행한다' in line for a in added))


if __name__=='__main__':
    import subprocess
    import author_public_contracts as public
    root=HERE.parents[2]
    def load(path):return json.loads(subprocess.check_output(['git','show',BASE+':'+str(path.relative_to(root))],cwd=root))
    before=load(HERE/'case.json');originals={p.stem:load(p) for p in (HERE/'fixtures').glob('*.json')}
    case,fixtures=apply(copy.deepcopy(before),copy.deepcopy(originals),public)
    changes=[];deltas={}
    for sub,old in zip(case['subcases'],before['subcases']):
        prior={a['id']:a for a in old['actions']}
        for a in sub['actions']:
            if a!=prior.get(a['id']):changes.append({'subcase':sub['id'],'actionId':a['id'],'before':prior.get(a['id']),'after':a})
    def diff(old,new,path=''):
        result=[]
        for key,value in new.items():
            here=path+str(key)
            if key not in old:result.append({'path':here,'hadBefore':False,'before':None,'after':value})
            elif isinstance(value,dict) and isinstance(old[key],dict):result.extend(diff(old[key],value,here+'/'))
            elif value!=old[key]:result.append({'path':here,'hadBefore':True,'before':old[key],'after':value})
        return result
    for sid,f in fixtures.items():deltas[f['fixtureId']]=diff(originals[sid],f)
    record=dict(PROVENANCE,baseline=BASE,businessAssertionsChanged=False,actionChanges=changes,fixtureChanges=deltas)
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    (HERE/'scenario.feature').write_text(public.feature_lines(feature,before,case))
    for path,value in [(HERE/'case.json',case),(HERE/'completion-input-delta.json',record)]+[(HERE/'fixtures'/f'{sid}.json',f) for sid,f in fixtures.items()]:path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    print('T17 completion input authoring:',len(changes),'action deltas')
