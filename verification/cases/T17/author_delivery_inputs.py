"""승인된 T17 주문 Work 준비와 공개 원천 ID 연결을 별도 가역 layer로 기록한다."""
import copy
import json
import subprocess
from pathlib import Path

HERE=Path(__file__).resolve().parent
BASE='bcac964ced0d5cf47a990afabba5eddefb66b2c8'
META='deliveryInputAuthoring'
PROVENANCE={'synthetic':True,'authoredBy':'coordinator-selected-fixture',
            'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}


def query(contract,name,identity,kind,f):
    return {'id':name,'kind':'query','actorRef':'observer','route':'api','capabilityId':'getObject',
        'request':{'id':identity,'scope':{'organizationId':contract.alias('ORG'),
        'itemId':contract.alias('P'),'objectType':kind},'asOf':f['clock']['asOf'],
        'knownAt':f['clock']['knownAt']},'evidenceRefs':[name+':actual-query-artifact']}


def apply(case,fixtures,contract):
    for sub in case['subcases']:
        sid=sub['id'];f=fixtures[sid];actions={a['id']:a for a in sub['actions']}
        if sid not in contract.DELIVERY_CASES or 'dispatch' not in actions:continue
        if META in f['baseline']['policy']:raise ValueError('Restore delivery layer before authoring')
        actor=f['actors']['t17Source'];before=copy.deepcopy(actor)
        if 'activateWork' in actor['roleCapabilities']:raise ValueError('Unexpected existing preparation grant')
        actor['roleCapabilities'].append('activateWork');actor['grant']['actions'].append('activateWork')
        f['baseline']['policy'][META]=dict(PROVENANCE,originalActor=before,
            selection='Public activation of the original order delivery Work; original ITEM/org all-of scope and validity retained')
        name='claim' if sid.startswith('claim-') else 'delivery';prefix=name+'-source'
        work_query=query(contract,'order-delivery-work',contract.ref('order','workId'),'Work',f)
        work_query['capabilityId']='getWork';del work_query['request']['scope']['objectType']
        preparation=[work_query,
            contract.command(sid,'activate-order-delivery-work','activateWork',
                {'workId':contract.ref('order-delivery-work','data/ID')},
                revision=contract.ref('order-delivery-work','data/revision'))]
        dispatch_query=query(contract,prefix+'-dispatch',contract.ref('dispatch','effects/dispatchId'),'Dispatch',f)
        line_query=query(contract,prefix+'-sales-line',contract.ref('order','line/id'),'SalesOrderLine',f)
        # Only public typed identities are read into the source encoder. Quantity,
        # time and scope claims remain the original report, including false claims.
        for field,action in [('dispatchId',prefix+'-dispatch'),('salesLineId',prefix+'-sales-line')]:
            for target in (prefix+'-document',prefix+'-event'):
                slots=actions[target]['request']['slots']
                encoders=('expectedHash','contentBase64') if target.endswith('-document') else ('payload',)
                for encoder in encoders:slots[encoder]['$encode']['source'][field]=contract.ref(action,'data/ID')
            actions[name if name=='claim' else name+'-provisional']['request']['slots'][field]=contract.ref(action,'data/ID')
        for target in (prefix+'-document',prefix+'-event'):
            slots=actions[target]['request']['slots']
            for encoder in (('expectedHash','contentBase64') if target.endswith('-document') else ('payload',)):
                source=slots[encoder]['$encode']['source'];source.update(PROVENANCE)
                original=source['originalRequest'];evidence=original.get('sourceEvidenceId') or original.get('evidenceId')
                if evidence and '$alias' in evidence:
                    source['originalEvidenceDescriptor']=copy.deepcopy(next(e for e in f['evidence'] if e['alias']==evidence['$alias']))
            slots['subject']['id']=contract.ref(prefix+'-dispatch','data/ID')
        intake=actions[name if name=='claim' else name+'-provisional']['request']['slots']
        intake['workId']=contract.ref('order-delivery-work','data/ID')
        output=[]
        for a in sub['actions']:
            if a['id']==prefix+'-document':output.extend([dispatch_query,line_query])
            if a['id']==prefix+'-match':
                observation_query=query(contract,prefix+'-observation',contract.ref(name+'-provisional','effects/observationId'),'DeliveryObservation',f)
                at=actions[name+'-provisional']['request']['slots']['occurredAt']
                observation_query['request']['asOf']=max(f['clock']['asOf'],at)
                observation_query['request']['knownAt']=max(f['clock']['knownAt'],at)
                output.append(observation_query)
                a['request']['slots']['physicalScopeId']=contract.ref(prefix+'-observation','data/ID')
            output.append(a)
            if a['id']=='dispatch':output.extend(preparation)
        sub['actions']=output
    return case,fixtures


def ledger():return json.loads((HERE/'delivery-input-delta.json').read_text())


def restore_case(case):
    case=copy.deepcopy(case)
    for sub in case['subcases']:
        changes={x['actionId']:x for x in ledger()['actionChanges'] if x['subcase']==sub['id']}
        if not set(changes).issubset({a['id'] for a in sub['actions']}):
            raise ValueError('Missing authored delivery action: '+sub['id'])
        result=[]
        for a in sub['actions']:
            d=changes.get(a['id'])
            if d:
                if a!=d['after']:raise ValueError('Delivery action drift: '+a['id'])
                if d['before'] is not None:result.append(copy.deepcopy(d['before']))
            else:result.append(a)
        sub['actions']=result
    return case


def restore_fixture(f):
    f=copy.deepcopy(f)
    d=ledger()['fixtureChanges'].get(f['fixtureId'])
    if d:
        if f['actors']['t17Source']!=d['afterActor'] or f['baseline']['policy'].get(META)!=d['metadata']:
            raise ValueError('Delivery preparation grant drift: '+f['fixtureId'])
        f['actors']['t17Source']=copy.deepcopy(d['beforeActor']);del f['baseline']['policy'][META]
    return f


def restore_feature(text):
    added={x['actionId'] for x in ledger()['actionChanges'] if x['before'] is None}
    return ''.join(line for line in text.splitlines(keepends=True) if not any(f'역할이 "{a}" 행동을 수행한다' in line for a in added))


if __name__=='__main__':
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
        f=fixtures[sub['id']];of=originals[sub['id']]
        if META in f['baseline']['policy']:deltas[f['fixtureId']]={'beforeActor':of['actors']['t17Source'],
            'afterActor':f['actors']['t17Source'],'metadata':f['baseline']['policy'][META]}
    record=dict(PROVENANCE,baseline=BASE,businessAssertionsChanged=False,actionChanges=changes,fixtureChanges=deltas)
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    (HERE/'scenario.feature').write_text(public.feature_lines(feature,before,case))
    for path,value in [(HERE/'case.json',case),(HERE/'delivery-input-delta.json',record)]+[(HERE/'fixtures'/f'{sid}.json',f) for sid,f in fixtures.items()]:
        path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    print('T17 delivery input authoring:',len(changes),'action deltas')
