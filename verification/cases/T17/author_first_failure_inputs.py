"""T17 한정 새 synthetic DISPATCH 원문과 보류 입력을 가역적으로 작성한다.

기존 SELL 사실/actor/grant/oracle은 변경하지 않는다. 없는 dueAt은 추론하지 않는다.
"""
import copy
import json
from pathlib import Path
from uuid import UUID, uuid5

HERE = Path(__file__).resolve().parent
BASE = '20c2666848b89b3ab7b4a1b1598e58cb408d13c4'
META = 'firstFailureInputAuthoring'
PROVENANCE = {'synthetic': True, 'authoredBy': 'coordinator-selected-fixture',
              'operatingApprovalClaimed': False, 'originalAuthorityClaimed': False}


def apply(case, fixtures, contract):
    for sub in case['subcases']:
        f = fixtures[sub['id']]
        spec = f['baseline']['t17SourceFacts']
        if META in f['baseline']['policy']:
            raise ValueError('Restore the first-failure layer before authoring')
        if any(a.get('capabilityId') == 'dispatchQuantity' for a in sub['actions']):
            policy = copy.deepcopy(f['aliases'][spec['policyAlias']])
            policy['action'] = 'DISPATCH'
            f['aliases']['t17DispatchRegulatoryPolicy'] = policy
            spec['dispatchPolicyAlias'] = 't17DispatchRegulatoryPolicy'
            # Only the physical segment explicitly reserved for the declared
            # dispatch gets new authority; B40 is not broadened by A60's flow.
            by_id={a['id']:a for a in sub['actions']}
            dispatch_segments=[]
            for a in sub['actions']:
                if a.get('capabilityId')=='dispatchQuantity':
                    allocation=a['request']['slots']['allocationId']['$result']['actionId']
                    segment=by_id[allocation]['request']['slots']['segmentId']['$alias']
                    if segment not in dispatch_segments:dispatch_segments.append(segment)
            spec['dispatchSegmentAliases']=dispatch_segments
            spec['dispatchInputs'] = [dict(PROVENANCE, action='DISPATCH',
                originalConditionPointer=f'/baseline/conditions/{i}',
                originalCondition=copy.deepcopy(row))
                for i, row in enumerate(f['baseline']['conditions']) if row['segmentAlias'] in dispatch_segments]
            f['baseline']['policy'][META] = dict(PROVENANCE,
                selection='NEW_ACTION_SPECIFIC_DISPATCH_SOURCE_INPUTS',
                preserved='original condition scope, quantity, unit, status, evidence and interval')
        out = []
        for action in sub['actions']:
            if action.get('capabilityId') != 'placeHold':
                out.append(action)
                continue
            slots = action['request']['slots']; name = action['id']
            before = copy.deepcopy(slots)
            # Existing QC reasons describe the requested restriction. The late
            # recall request has no authorized source decision capability.
            category = 'RECALL' if slots['reason'] == 'RECALL_INVESTIGATION' else 'QC'
            segment = slots['segmentId']
            alias = segment.get('$alias', 'A')
            row = next(r for r in f['baseline']['conditions']
                       if r['segmentAlias'] == alias and r['conditionId'] == 'QC')
            # These are NEW finite fixture boundaries, selected from its declared
            # condition window. No original hold validity or approval is claimed.
            validity = dict(PROVENANCE, selection='NEW_HOLD_WINDOW_FROM_ORIGINAL_QC_CONDITION',
                            originalCondition=copy.deepcopy(row))
            original = dict(PROVENANCE, originalRequest=before, validityAuthoring=validity,
                operation='placeHold', segmentId=copy.deepcopy(segment), action=slots['action'],
                category=category, startQuantity='0', quantity=slots['quantity'], unit=slots['unit'],
                validFrom=max(row['validFrom'],f['clock']['asOf']), validUntil=row['validUntil'],
                workId=contract.alias(spec['workAliases'][alias]))
            evidence_alias=before.get('evidenceId',{}).get('$alias',row['evidenceAlias'])
            original['originalEvidenceDescriptor']=copy.deepcopy(next(e for e in f['evidence'] if e['alias']==evidence_alias))
            original['originalEvidenceId']=contract.alias(evidence_alias)
            original['sourceDecisionId']=str(uuid5(UUID('4de6f5c3-dc98-43b8-aef4-402505d9b679'),sub['id']+':'+name))
            original['sourceVersion']='1'
            prefix = name + '-source'
            if '$result' in segment:
                # Read the actual typed segment ID through the public noun query.
                # The input encoder permits /response/data/ID, while the dispatch
                # transitSegmentId pointer is only accepted outside encoded JSON.
                scope_action={'id':prefix+'-scope','kind':'query','actorRef':'observer',
                    'route':'api','capabilityId':'getObject','request':{
                        'id':copy.deepcopy(segment),'scope':{'organizationId':contract.alias('ORG'),
                        'itemId':contract.alias('P'),'objectType':'QuantitySegment'},
                        'asOf':f['clock']['asOf'],'knownAt':f['clock']['knownAt']},
                    'evidenceRefs':[prefix+'-scope:actual-query-artifact']}
                out.append(scope_action)
                action['request']['expectedRevision']=contract.ref(prefix+'-scope','data/revision')
                segment=contract.ref(prefix+'-scope','data/ID')
                original['originalRequestJson']=json.dumps(before,ensure_ascii=False,separators=(',',':'))
                original['originalRequest']['segmentId']=copy.deepcopy(segment)
                original['segmentId']=copy.deepcopy(segment)
            source = contract.source_actions(sub['id'], prefix, {'kind':'SEGMENT','id':segment},
                original, 'QUALITY_DECISION_placeHold_' + category,
                original['validFrom'], slots['quantity'], slots['unit'],external_id=original['sourceDecisionId'])
            source[0]['request']['slots']['provenance']='NEW_COORDINATOR_SYNTHETIC_HOLD_WINDOW; originalRequest preserved; operatingApprovalClaimed=false'
            source[1]['request']['slots']['assertion']='Original hold request with explicitly new synthetic validity; no operating approval or category grant is claimed'
            source += [contract.command(sub['id'], prefix+'-match', 'matchSourceIdentity', {
                'claimId':contract.ref(prefix+'-event','claimId'),
                'basisDocumentId':contract.ref(prefix+'-document','id'),
                'physicalScopeId':segment, 'policyVersion':contract.POLICY,
                'sourceIdentity':contract.NAMESPACE+':'+original['sourceDecisionId']+':1',
                'quantity':slots['quantity'], 'unit':slots['unit'], 'effectiveFrom':original['validFrom'],
                'reason':'원래 보류 요청의 수량·범위·증거와 명시한 synthetic 기간을 대조한다'},revision=1),
                contract.command(sub['id'],prefix+'-canonical','linkCanonicalOccurrence',{
                'reconciliationId':contract.ref(prefix+'-match','id')},revision=1)]
            for key in ['category','startQuantity','validFrom','validUntil','workId']:
                slots[key] = copy.deepcopy(original[key])
                action['request']['provenance'][key] = 'CONTEXT'
            slots['evidenceId'] = contract.ref(prefix+'-document','id')
            slots['nextCheckAt'] = f['responsibilities'][0]['nextCheckAt']
            action['request']['provenance']['evidenceId'] = 'CONTEXT'
            action['request']['provenance']['nextCheckAt'] = 'CONTEXT'
            profile = next(p for p in f['baseline']['sourceProfiles'] if p['namespace']==contract.NAMESPACE)
            kind = 'QUALITY_DECISION_placeHold_' + category
            if kind not in profile['eventKinds']: profile['eventKinds'].append(kind)
            out.extend(source); out.append(action)
        sub['actions'] = out
    return case, fixtures


def _ledger(): return json.loads((HERE/'first-failure-input-delta.json').read_text())


def restore_case(case):
    case = copy.deepcopy(case)
    for sub in case['subcases']:
        changes = {c['actionId']:c for c in _ledger()['actionChanges'] if c['subcase']==sub['id']}
        restored = []
        for action in sub['actions']:
            change = changes.get(action['id'])
            if change:
                if action != change['after']: raise ValueError('First-failure action drift: '+action['id'])
                if change['before'] is not None: restored.append(change['before'])
            else: restored.append(action)
        sub['actions'] = restored
    return case


def restore_fixture(f):
    f = copy.deepcopy(f)
    for change in reversed(_ledger()['fixtureChanges'].get(f['fixtureId'], [])):
        parent = f
        keys = change['path'].split('/')
        for key in keys[:-1]: parent = parent[int(key)] if isinstance(parent,list) else parent[key]
        key = int(keys[-1]) if isinstance(parent,list) else keys[-1]
        if parent[key] != change['after']: raise ValueError('First-failure fixture drift: '+change['path'])
        if change['hadBefore']: parent[key] = change['before']
        else: del parent[key]
    return f


def restore_feature(feature):
    added = {c['actionId'] for c in _ledger()['actionChanges'] if c['before'] is None}
    return ''.join(line for line in feature.splitlines(keepends=True)
                   if not any(f'역할이 "{a}" 행동을 수행한다' in line for a in added))


if __name__ == '__main__':
    import subprocess
    import author_public_contracts as public
    root=HERE.parents[2]
    def load(path):return json.loads(subprocess.check_output(['git','show',BASE+':'+str(path.relative_to(root))],cwd=root))
    before=load(HERE/'case.json'); originals={p.stem:load(p) for p in (HERE/'fixtures').glob('*.json')}
    case, fixtures=apply(copy.deepcopy(before),copy.deepcopy(originals),public)
    changes=[]; deltas={}
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
    unresolved=[{'subcase':s['id'],'actionId':a['id'],'reason':'ORIGINAL_DUE_AT_ABSENT_NO_VALUE_AUTHORED'} for s in before['subcases'] for a in s['actions'] if a.get('capabilityId')=='createSalesOrder' and 'dueAt' not in a['request']['slots']]
    ledger=dict(PROVENANCE,baseline=BASE,businessAssertionsChanged=False,actionChanges=changes,fixtureChanges=deltas,unresolvedDueAt=unresolved)
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    (HERE/'scenario.feature').write_text(public.feature_lines(feature,before,case))
    for path,value in [(HERE/'case.json',case),(HERE/'first-failure-input-delta.json',ledger)]+[(HERE/'fixtures'/f'{sid}.json',f) for sid,f in fixtures.items()]:
        path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    print(f'T17 first-failure input layer: {len(changes)} action deltas; {len(unresolved)} dueAt values remain absent')
