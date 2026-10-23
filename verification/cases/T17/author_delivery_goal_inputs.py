"""기존 주문 Work의 goal을 보존하면서 승인된 synthetic 완성 입력을 추가한다."""
import copy
import hashlib
import json
import subprocess
from datetime import datetime
from pathlib import Path

HERE=Path(__file__).resolve().parent
BASE='f2f978b51c0eb9b50d75764ebbb0a0329d6a48ea'
META='deliveryGoalInputAuthoring'
PROVENANCE={'synthetic':True,'authoredBy':'coordinator-selected-fixture',
            'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}
DEFINITION_ALIAS='t17DeliveryGoalDefinition'
EXISTING=('quantityMode','targetQuantity','unit','endpoint','dueAt','placeId','scope',
          'eventKind','contributionScope','deduplication','provenance')


def canonical(value):return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'))


def binding(f):
    path=HERE.parents[2]/'verification/actual/scenarios/installer-conventions.json'
    conventions=json.loads(path.read_text())
    definition=copy.deepcopy(conventions['definition']);definition['version']=f['versions']['definition']
    candidates=[g for g in definition['goals'] if g['endpoint']=='DELIVERED' and g['quantityMode']=='CUMULATIVE_EVENT']
    if len(candidates)!=1 or candidates[0]['evaluatorVersion']!=definition['evaluatorVersion']:
        raise ValueError('A unique pinned supported delivery template is required')
    policies=[p for p in conventions['policies'] if p['kind']=='EVIDENCE']
    authored=[a for a in f['aliases'].values() if a['type']=='PolicyVersion' and a.get('kind')=='EVIDENCE']
    if authored or f['baseline'].get('definitionContract') or any(a['type']=='DefinitionVersion' for a in f['aliases'].values()):
        raise ValueError('Existing definition/EVIDENCE override requires an explicit binding review')
    if len(policies)!=1:raise ValueError('One installed EVIDENCE convention required')
    # Predeclare the runtime identity keys so publisher replacement preserves the
    # sorted insertion order. The normal installer binds real org/id and hashes
    # these exact published bytes; the source encoder uses the same sorted bytes.
    definition.update(id='',organizationId='',state='PUBLISHED')
    definition=json.loads(canonical(definition))
    if len(canonical(definition).encode())>65536:raise ValueError('Definition exceeds public encoder bound')
    policy=policies[0]
    return definition,dict(PROVENANCE,conventionsPath=str(path.relative_to(HERE.parents[2])),
        conventionsSha256=hashlib.sha256(path.read_bytes()).hexdigest(),definitionVersion=definition['version'],
        definitionTemplateHash=hashlib.sha256(canonical(definition).encode()).hexdigest(),
        template=copy.deepcopy(candidates[0]),evaluatorVersion=definition['evaluatorVersion'],
        evidencePolicyVersion=policy['version'],evidencePolicyContent=copy.deepcopy(policy['content']),
        evidencePolicyHash=hashlib.sha256(json.dumps(policy['content'],ensure_ascii=False,separators=(',',':')).encode()).hexdigest())


def apply(case,fixtures,contract):
    for sub in case['subcases']:
        actions={a['id']:a for a in sub['actions']}
        if 'activate-order-delivery-work' not in actions:continue
        f=fixtures[sub['id']]
        if META in f['baseline']['policy']:raise ValueError('Restore goal completion layer before authoring')
        definition,proof=binding(f)
        actor=f['actors']['t17Source'];before=copy.deepcopy(actor)
        if 'reviseGoal' in actor['roleCapabilities']:raise ValueError('Unexpected pre-existing reviseGoal grant')
        actor['roleCapabilities'].append('reviseGoal');actor['grant']['actions'].append('reviseGoal')
        f['aliases'][DEFINITION_ALIAS]={'type':'DefinitionVersion','version':definition['version'],
            'state':'PUBLISHED','content':definition,**PROVENANCE}
        order=actions['order']['request']['slots'];start=f['clock']['asOf'];end=order['dueAt']
        if not datetime.fromisoformat(start.replace('Z','+00:00'))<datetime.fromisoformat(end.replace('Z','+00:00')):
            raise ValueError('Synthetic delivery window must be nonempty')
        proof['window']={'periodStart':start,'periodEnd':end,'periodStartInclusive':True,'periodEndInclusive':True,
            'selection':'NEW coordinator-selected synthetic delivery window; not an original given'}
        proof['originalActor']=before
        f['baseline']['policy'][META]=proof
        materialized=copy.deepcopy(definition);materialized['id']=contract.alias(DEFINITION_ALIAS);materialized['organizationId']=contract.alias('ORG')
        context=dict(PROVENANCE,definitionVersionId=contract.alias(DEFINITION_ALIAS),
            definitionContentHash=contract.encoded(materialized,'sha256'),definitionVersion=definition['version'],
            evidencePolicyVersion=proof['evidencePolicyVersion'],evidencePolicyHash=proof['evidencePolicyHash'],
            conventionsSha256=proof['conventionsSha256'],selection=proof['window']['selection'])
        new={'timezone':f['clock']['timezone'],**{k:v for k,v in proof['window'].items() if k!='selection'},
             'evidencePolicyVersion':proof['evidencePolicyVersion'],'evaluatorVersion':proof['evaluatorVersion'],
             'conditions':[{'id':proof['template']['name']}]}
        context['fieldProvenance']={k:dict(PROVENANCE,source='fixture.clock.timezone' if k=='timezone' else
            'installed immutable EVIDENCE policy' if k=='evidencePolicyVersion' else
            'pinned PUBLISHED definition' if k in ('evaluatorVersion','conditions') else proof['window']['selection']) for k in new}
        goal={k:contract.ref('order-delivery-work','data/currentGoal/slots/'+k) for k in EXISTING}
        goal.update(new,defaultContext=context)
        revise=contract.command(sub['id'],'complete-order-delivery-goal','reviseGoal',
            {'workId':contract.ref('order-delivery-work','data/ID'),'goal':goal,
             'reason':'원래 주문 인도 goal을 보존하고 승인된 synthetic 필수 입력을 완성한다'},
            revision=contract.ref('order-delivery-work','data/revision'))
        fresh=copy.deepcopy(actions['order-delivery-work']);fresh['id']='completed-order-delivery-work'
        fresh['evidenceRefs']=['completed-order-delivery-work:actual-query-artifact']
        activation=actions['activate-order-delivery-work']['request']
        activation['slots']['workId']=contract.ref(fresh['id'],'data/ID')
        activation['expectedRevision']=contract.ref(fresh['id'],'data/revision')
        out=[]
        for a in sub['actions']:
            if a['id']=='activate-order-delivery-work':out.extend([revise,fresh])
            out.append(a)
        sub['actions']=out
    return case,fixtures


def ledger():return json.loads((HERE/'delivery-goal-input-delta.json').read_text())


def restore_case(case):
    case=copy.deepcopy(case)
    for sub in case['subcases']:
        changes={x['actionId']:x for x in ledger()['actionChanges'] if x['subcase']==sub['id']}
        if not set(changes).issubset({a['id'] for a in sub['actions']}):raise ValueError('Missing goal completion action')
        out=[]
        for a in sub['actions']:
            d=changes.get(a['id'])
            if d:
                if a!=d['after']:raise ValueError('Goal completion action drift: '+a['id'])
                if d['before'] is not None:out.append(copy.deepcopy(d['before']))
            else:out.append(a)
        sub['actions']=out
    return case


def restore_fixture(f):
    f=copy.deepcopy(f);d=ledger()['fixtureChanges'].get(f['fixtureId'])
    if d:
        if f['actors']['t17Source']!=d['afterActor'] or f['aliases'].get(DEFINITION_ALIAS)!=d['definitionBinding'] or f['baseline']['policy'].get(META)!=d['metadata']:
            raise ValueError('Goal completion binding/grant drift')
        f['actors']['t17Source']=copy.deepcopy(d['beforeActor']);del f['aliases'][DEFINITION_ALIAS];del f['baseline']['policy'][META]
    return f


def restore_feature(text):
    if hashlib.sha256(text.encode()).hexdigest()!=ledger()['featureAfterSha256']:
        raise ValueError('Goal completion feature drift')
    added={x['actionId'] for x in ledger()['actionChanges'] if x['before'] is None}
    return ''.join(line for line in text.splitlines(keepends=True) if not any(f'역할이 "{a}" 행동을 수행한다' in line for a in added))


if __name__=='__main__':
    import author_public_contracts as public
    root=HERE.parents[2]
    def load(path):return json.loads(subprocess.check_output(['git','show',BASE+':'+str(path.relative_to(root))],cwd=root))
    before=load(HERE/'case.json');old={p.stem:load(p) for p in (HERE/'fixtures').glob('*.json')}
    case,fixtures=apply(copy.deepcopy(before),copy.deepcopy(old),public);changes=[];deltas={}
    for sub,prior in zip(case['subcases'],before['subcases']):
        originals={a['id']:a for a in prior['actions']}
        for a in sub['actions']:
            if a!=originals.get(a['id']):changes.append({'subcase':sub['id'],'actionId':a['id'],'before':originals.get(a['id']),'after':a})
        f=fixtures[sub['id']]
        if META in f['baseline']['policy']:deltas[f['fixtureId']]={'beforeActor':old[sub['id']]['actors']['t17Source'],'afterActor':f['actors']['t17Source'],'definitionBinding':f['aliases'][DEFINITION_ALIAS],'metadata':f['baseline']['policy'][META]}
    feature=subprocess.check_output(['git','show',BASE+':verification/cases/T17/scenario.feature'],cwd=root).decode()
    authored_feature=public.feature_lines(feature,before,case)
    (HERE/'scenario.feature').write_text(authored_feature)
    record=dict(PROVENANCE,baseline=BASE,businessAssertionsChanged=False,actionChanges=changes,fixtureChanges=deltas,
        featureBeforeSha256=hashlib.sha256(feature.encode()).hexdigest(),
        featureAfterSha256=hashlib.sha256(authored_feature.encode()).hexdigest())
    for path,value in [(HERE/'case.json',case),(HERE/'delivery-goal-input-delta.json',record)]+[(HERE/'fixtures'/f'{sid}.json',f) for sid,f in fixtures.items()]:path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    print('T17 goal completion:',len(changes),'action deltas')
