#!/usr/bin/env python3
"""Author C3 valid-input counterexamples only; no product transition or PASS evidence."""
import copy
import hashlib
import json
import re
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
DIR = ROOT / 'verification/cases/C3'
NOW = '2026-10-07T09:00:00Z'
END = '2026-10-08T00:00:00Z'
CAPS = ('dispatchPurchaseOrder', 'migrateWorkDefinition', 'transferObligation')
def alias(name): return {'$alias': name}
def typed(value, provenance='CONTEXT'): return {'value':value,'provenance':provenance}
def ref(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}
def dump(value): return json.dumps(value, ensure_ascii=False, separators=(',', ':'))
def save(path, value): path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')
def invoke(sub, id, actor, cap, slots, revision=1, subject=None):
    return {'id':id,'kind':'invoke','actorRef':actor,'route':'api','capabilityId':cap,
            'request':{'intentKind':'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,
                       'scope':copy.deepcopy(sub['assertions'][0]['scope']),
                       'subjectRefs':subject or [],'slots':slots,'asOf':NOW,'knownAt':NOW,
                       'expectedRevision':revision,'commandIdempotencyKey':'C3-'+sub['id']+'-'+id},
            'evidenceRefs':[id+':actual-artifact']}
def check(sub, id, action, pointer, expected, op='equals', field=None, where=None):
    value=copy.deepcopy(sub['assertions'][-1]);value.update(id=id,op=op,source={'actionId':action,'pointer':pointer},expected=expected,
        evidenceRefs=[action+':actual-artifact'],oracleExplanation='현재 권한 외 업무 전제를 성립시키고 승인·수락·전환 결과와 같은 command의 실제 효과를 대조한다.')
    value.pop('baseline',None)
    if field:value['source']['field']=field
    if where:value['source']['where']=where
    sub['assertions'].append(value)
def allow(actor, actions):
    for key in ['roleCapabilities']:
        actor[key]=list(dict.fromkeys(actor[key]+actions))
    actor['grant']['actions']=list(dict.fromkeys(actor['grant']['actions']+actions))
def prepare_fixture(cap):
    path=DIR/('fixture-'+cap+'.json');f=json.loads(path.read_text());b=f['baseline']
    if cap=='dispatchPurchaseOrder':
        p=b['priorEntities']['PROPOSAL'];p.update(proposalRevision=1,supplierAlias='SUPPLIER',workAlias='WORK',dueAt='2026-10-09T09:00:00Z')
        canonical={k:v for k,v in p.items() if k not in ['canonicalHash','canonicalPayload','state','revision','proposalRevision']}
        p['canonicalPayload']=canonical;p['canonicalHash']=hashlib.sha256(json.dumps(canonical,sort_keys=True,separators=(',',':')).encode()).hexdigest()
        b['sourceProfiles'].append({'namespace':'fixture-local-outbox','operations':['dispatchPurchaseOrder'],'externalWriteMode':'LOCAL_OUTBOX_ONLY','synthetic':True}) if not any(x.get('namespace')=='fixture-local-outbox' for x in b['sourceProfiles']) else None
    elif cap=='migrateWorkDefinition':
        allow(f['actors']['fde'],['validateDefinition','submitDefinitionReview','publishDefinition'])
        f['actors']['configApprover']=copy.deepcopy(f['actors']['manager']);a=f['actors']['configApprover'];a['subject']='fixture-config-approver';a['roleCapabilities']=['approveDefinition','getDefinition'];a['grant']['actions']=a['roleCapabilities'].copy()
        f['aliases']['configApprover']={'type':'Human','organizationAlias':'ORG-A'}
        f['aliases']['GOAL-V1']={'type':'GoalVersion','organizationAlias':'ORG-A'}
        b['work'].update(revision=1,goalVersionAlias='GOAL-V1',evaluatorVersion='arrival-v1',ownerAlias='delegator')
        for responsibility in f['responsibilities']:
            if responsibility.get('scope',{}).get('workAlias')=='WORK':responsibility['ownerAlias']='delegator'
        b['goalVersions']=[{'alias':'GOAL-V1','workAlias':'WORK','definitionVersion':'definition-v1',**copy.deepcopy(b['work']['goal'])}]
        b['definitionPackages']=[{'alias':'DEFINITION','version':'definition-v1','state':'ACTIVE','hash':hashlib.sha256(b'C3-definition-v1').hexdigest(),
            'goalTemplate':{'endpoint':'ARRIVED','quantityMode':'CUMULATIVE_EVENT','evaluatorVersion':'arrival-v1','requiredSlots':['item','quantity','unit','destination','dueAt']}}]
        b['supportManifest']=[{'definitionVersion':v,'capabilityId':c,'semanticVersion':'core-v1','evaluatorVersion':'arrival-v1','inputSchemaVersion':'1.0.0','outputSchemaVersion':'1.0.0','supportedWorkMigration':['C3-v1-to-v2'] if v=='definition-v2' else []}
            for v in ['definition-v1','definition-v2'] for c in ['createDefinitionDraft','validateDefinition','submitDefinitionReview','approveDefinition','publishDefinition','migrateWorkDefinition','getWork','getInventory','getObject','getAccessContext','getDefinition']]
    elif cap=='transferObligation':
        # A versioned synthetic prior recipient decision is a prerequisite, not evidence of this execution.
        f['aliases']['RECIPIENT-ACCEPTANCE']={'type':'DocumentVersion','organizationAlias':'ORG-A'}
        for actor_name in ['reader','delegator','warehouse']:
            actor=f['actors'][actor_name]
            scope=actor['grant']['scope'];scope['workAliases']=list(dict.fromkeys(scope.get('workAliases',[])+['WORK2']))
        b['work']['revision']=1
        duty=b['priorEntities']['DUTY'];duty.update(revision=1,rootAlias='DUTY',workAlias='WORK2',ownerAlias='delegator',quantity='20',unit='BOX')
        b['priorEntities']['WORK2']={'revision':1,'state':'ACTIVE','ownerAlias':'delegator','definitionVersion':'definition-v1','supervisorAlias':'supervisor','nextAction':'의무 이전 전 수신자 수락을 검증한다','nextCheckAt':'2026-10-07T10:00:00Z','goal':{'quantity':'20','unit':'BOX','endpoint':'ARRIVED','destinationAlias':'W'}}
        b['work'].update(nextAction='인수할 의무의 실물 범위를 확인한다',nextCheckAt='2026-10-07T10:00:00Z')
        f['responsibilities']=[r for r in f['responsibilities'] if r.get('scope',{}).get('workAlias')!='WORK2']+[{'scope':{'workAlias':'WORK2','itemAlias':'P'},'ownerAlias':'delegator','supervisorAlias':'supervisor','nextAction':'의무 이전 전 수신자 수락을 검증한다','nextCheckAt':'2026-10-07T10:00:00Z'}]
        acceptance={'decision':'ACCEPTED','issuedByAlias':'warehouse','verifiedIdentity':{'issuer':f['actors']['warehouse']['issuer'],'subject':f['actors']['warehouse']['subject'],'audience':f['actors']['warehouse']['audience'],'organizationAlias':'ORG-A'},
            'rootObligationAlias':'DUTY','sourceObligationAlias':'DUTY','sourceWorkAlias':'WORK2','targetWorkAlias':'WORK','acceptingOwnerAlias':'warehouse','expectedSourceRevision':1,'expectedTargetRevision':1,
            'quantity':'20','unit':'BOX','scope':{'organizationAlias':'ORG-A','segmentAlias':'A20','quantity':'20','unit':'BOX'},'acceptedAt':'2026-10-07T08:00:00Z','validFrom':'2026-10-07T08:00:00Z','validUntil':END,'consumptionPolicy':'ONE_TRANSFER','consumed':False,'revokedAt':None}
        content=json.dumps(acceptance,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n';(DIR/'recipient-acceptance.json').write_text(content)
        b['recipientAcceptances']=[{'alias':'RECIPIENT-ACCEPTANCE',**acceptance,'sha256':hashlib.sha256(content.encode()).hexdigest(),'verificationStatus':'VERIFIED'}]
        f['evidence']=[x for x in f['evidence'] if x.get('alias')!='RECIPIENT-ACCEPTANCE']+[{'alias':'RECIPIENT-ACCEPTANCE','sha256':hashlib.sha256(content.encode()).hexdigest(),'sourceNamespace':'synthetic-recipient-decisions','externalEventId':'C3-warehouse-accept-transfer20','sourceVersion':'1','occurredAt':'2026-10-07T08:00:00Z','recordedAt':'2026-10-07T08:00:01Z'}]
        b['documents']=[x for x in b['documents'] if x['alias']!='RECIPIENT-ACCEPTANCE']+[{'alias':'RECIPIENT-ACCEPTANCE','path':'verification/cases/C3/recipient-acceptance.json','mediaType':'application/json','immutable':True,'sha256':hashlib.sha256(content.encode()).hexdigest()}]
    save(path,f)
def patch_sub(sub, cap):
    sub['actions']=[a for a in sub['actions'] if not a['id'].startswith('precondition-')]
    sub['assertions']=[a for a in sub['assertions'] if not a['id'].startswith('precondition-') and not a['id'].startswith('authorized-business-')]
    actions={a['id']:a for a in sub['actions']};denied=actions['attempt'];positive=actions['authorized-same-input']
    pre=[]
    if cap=='dispatchPurchaseOrder':
        query=copy.deepcopy(actions['target-before']);query['id']='precondition-proposal';query['request']={'objectId':alias('PROPOSAL')};query['evidenceRefs']=['precondition-proposal:actual-artifact'];pre.append(query)
        pre.append(invoke(sub,'precondition-approval','manager','approvePurchase',{
            'proposalId':typed(alias('PROPOSAL')),'proposalHash':typed(ref('precondition-proposal','/response/data/proposalHash')),
            'decision':typed('APPROVE','USER'),'decidedAt':typed(NOW,'USER'),'validUntil':typed(END,'USER'),
            'consumptionPolicy':typed('SINGLE_ORDER_REVISION','USER'),'conditions':typed([],'USER')},
            ref('precondition-proposal','/response/data/proposalRevision'),[{'type':'TradeItem','id':alias('P')}]))
        for target in ['target-before','authorized-target']:actions[target]['request']={'objectId':alias('PROPOSAL')}
        for a in [denied,positive]:
            a['request']['subjectRefs']=[{'type':'TradeItem','id':alias('P')}]
            a['request']['slots']={'proposalId':typed(alias('PROPOSAL')),'proposalHash':typed(ref('target-before','/response/data/proposalHash')),
                'approvalId':typed(ref('precondition-approval','/response/approvalId')),'channel':typed('fixture-local-outbox','USER'),'externalOperationId':typed(alias('EXTERNAL-OP'),'USER')}
            a['request']['expectedRevision']=ref('target-before','/response/data/proposalRevision')
        check(sub,'precondition-approved','precondition-approval','/response/outcome','APPLIED')
        check(sub,'precondition-approval-record','before','/data/rawRows/approvals',[[ref('precondition-approval','/response/approvalId'),alias('PROPOSAL'),ref('precondition-proposal','/response/data/proposalHash'),ref('precondition-proposal','/response/data/proposalRevision'),alias('manager'),'APPROVE',NOW,END,'SINGLE_ORDER_REVISION']],'relationSet',field=['id','proposalId','proposalHash','proposalRevision','approverId','decision','decidedAt','validUntil','consumptionPolicy'],where={'id':ref('precondition-approval','/response/approvalId')})
        check(sub,'authorized-business-outbox-once','authorized-after','/data/rawRows/outbox',[[alias('PROPOSAL'),ref('precondition-proposal','/response/data/proposalHash'),ref('precondition-approval','/response/approvalId'),alias('EXTERNAL-OP'),positive['request']['commandIdempotencyKey']]],'relationSet',field=['proposalId','proposalHash','approvalId','externalOperationId','commandIdempotencyKey'],where={'capabilityId':cap})
    elif cap=='migrateWorkDefinition':
        package={'nounTypes':[{'name':'QuantitySegment','coreRef':'QuantitySegment'}],'verbDefinitions':[{'name':'arrive','kind':'WORK','slotTypes':{'item':'TradeItem','destination':'Place','quantity':'DecimalWithUnit'},'endpoint':'ARRIVED'}],
            'goalTemplates':[{'name':'default','quantity':'100','unit':'BOX','quantityMode':'CUMULATIVE_EVENT','endpoint':'ARRIVED','evaluatorId':'arrival-v1','evaluatorVersion':'arrival-v1','evidencePolicyVersion':'synthetic-authority-v1','dueAt':'2026-10-09T18:00:00+09:00'}],
            'compatibilityManifest':[{'capabilityId':'migrateWorkDefinition','semanticVersion':'core-v1','evaluatorVersion':'arrival-v1','inputSchemaVersion':'1.0.0','outputSchemaVersion':'1.0.0','supportedWorkMigration':['C3-v1-to-v2']}],
            'regressionFixtureRefs':['T07','T21'],'organizationId':alias('ORG-A')}
        pre.append(invoke(sub,'precondition-draft','fde','createDefinitionDraft',{'parentDefinitionId':alias('DEFINITION'),'version':'definition-v2','package':package}))
        target=ref('precondition-draft','/response/definitionId');hash=ref('precondition-draft','/response/hash')
        pre.append(invoke(sub,'precondition-validate','fde','validateDefinition',{'definitionId':target,'hash':hash},ref('precondition-draft','/response/revision')))
        pre.append(invoke(sub,'precondition-review','fde','submitDefinitionReview',{'definitionId':target,'hash':hash,'validationId':ref('precondition-validate','/response/validationId')},ref('precondition-validate','/response/revision')))
        pre.append(invoke(sub,'precondition-publish-approval','configApprover','approveDefinition',{'definitionId':target,'hash':hash,'decision':'APPROVED','validFrom':NOW,'validUntil':END,'consumptionPolicy':'ONE_PACKAGE_PUBLISH'},ref('precondition-review','/response/revision')))
        pre.append(invoke(sub,'precondition-publish','fde','publishDefinition',{'definitionId':target,'hash':hash,'approvalId':ref('precondition-publish-approval','/response/approvalId')},ref('precondition-publish-approval','/response/revision')))
        mapping={'quantity':{'from':'100','to':'100','unit':'BOX'},'endpoint':{'from':'ARRIVED','to':'ARRIVED'},'destination':{'from':alias('W'),'to':alias('W')}}
        pre.append(invoke(sub,'precondition-migration-review','fde','submitDefinitionReview',{'definitionId':target,'hash':ref('precondition-publish','/response/hash'),'reviewKind':'WORK_MIGRATION','migrationId':'C3-v1-to-v2','affectedWorkIds':[alias('WORK')],'affectedDataIds':[alias('GOAL-V1')],'mapping':mapping,'unsupportedValues':[],'regressionRunIds':ref('precondition-validate','/response/regressionRunIds')},ref('precondition-publish','/response/revision')))
        pre.append(invoke(sub,'precondition-migration-approval','configApprover','approveDefinition',{'definitionId':target,'hash':ref('precondition-publish','/response/hash'),'reviewId':ref('precondition-migration-review','/response/reviewId'),'reviewKind':'WORK_MIGRATION','decision':'APPROVED','affectedWorkIds':[alias('WORK')],'validFrom':NOW,'validUntil':END,'consumptionPolicy':'ONE_MIGRATION'},ref('precondition-migration-review','/response/revision')))
        for query in ['target-before','authorized-target']:actions[query]['capabilityId']='getWork';actions[query]['request']={'workId':alias('WORK')}
        for a in [denied,positive]:
            a['request']['subjectRefs']=[{'type':'Work','id':alias('WORK')}]
            a['request']['slots']={'workId':alias('WORK'),'targetDefinitionId':target,'migrationId':'C3-v1-to-v2','approvalId':ref('precondition-migration-approval','/response/approvalId'),'reason':'승인된 동일 목표의 명시적 정의 전환'}
        for id in ['precondition-draft','precondition-validate','precondition-review','precondition-publish-approval','precondition-publish','precondition-migration-review','precondition-migration-approval']:
            check(sub,id+'-applied',id,'/response/outcome','APPLIED')
        check(sub,'precondition-published','before','/data/rawRows/definitions',[[target,'definition-v2','PUBLISHED',ref('precondition-publish','/response/hash')]],'relationSet',field=['id','version','state','hash'],where={'id':target})
        check(sub,'authorized-business-work-version','authorized-after','/data/rawRows/works',[[alias('WORK'),'definition-v2',target]],'relationSet',field=['id','definitionVersion','definitionId'],where={'id':alias('WORK')})
        for observation in ['before','after','authorized-after']:
            actions[observation]['observation']['sources']+= [x for x in ['workDefinitionMigrations','migrationMappings','migrationRegressions','goalVersions'] if x not in actions[observation]['observation']['sources']]
        check(sub,'authorized-business-migration-once','authorized-after','/data/rawRows/workDefinitionMigrations',[[alias('WORK'),target,'C3-v1-to-v2',ref('precondition-migration-approval','/response/approvalId'),positive['request']['commandIdempotencyKey']]],'relationSet',field=['workId','targetDefinitionId','migrationId','approvalId','commandIdempotencyKey'],where={'workId':alias('WORK')})
        check(sub,'precondition-migration-approval-record','before','/data/rawRows/approvals',[[ref('precondition-migration-approval','/response/approvalId'),target,ref('precondition-publish','/response/hash'),ref('precondition-migration-review','/response/reviewId'),alias('configApprover'),'WORK_MIGRATION','APPROVED',NOW,END]],'relationSet',field=['id','definitionId','hash','reviewId','decisionActorId','reviewKind','decision','validFrom','validUntil'],where={'id':ref('precondition-migration-approval','/response/approvalId')})
        check(sub,'authorized-business-goal-history','authorized-after','/data/rawRows/goalVersions',[[alias('GOAL-V1'),'definition-v1','ARRIVED','100','BOX'],[ref('authorized-same-input','/response/goalVersionId'),'definition-v2','ARRIVED','100','BOX']],'relationSet',field=['id','definitionVersion','endpoint','quantity','unit'])
        check(sub,'authorized-business-migration-goal-link','authorized-after','/data/rawRows/workDefinitionMigrations',[[alias('WORK'),alias('GOAL-V1'),ref('authorized-same-input','/response/goalVersionId'),alias('delegator'),ref('precondition-migration-approval','/response/approvalId'),'C3-v1-to-v2']],'relationSet',field=['workId','previousGoalVersionId','goalVersionId','actorId','approvalId','migrationId'],where={'workId':alias('WORK')})
    elif cap=='transferObligation':
        scope={'organizationId':alias('ORG-A'),'itemId':alias('P'),'workIds':[alias('WORK2'),alias('WORK')],'obligationRootId':alias('DUTY'),'includeDescendants':True}
        for a in sub['assertions']:a['scope']=copy.deepcopy(scope)
        for a in sub['actions']:
            if 'scope' in a.get('request',{}):a['request']['scope']=copy.deepcopy(scope)
            if 'observation' in a:a['observation']['scope']=copy.deepcopy(scope)
        for a in [denied,positive]:
            a['request']['scope']=copy.deepcopy(scope)
            a['request']['slots'].pop('resolutionEvidenceId',None)
            a['request']['slots'].update(acceptedById=alias('warehouse'),acceptanceEvidenceId=alias('RECIPIENT-ACCEPTANCE'),quantity={'value':'20','unit':'BOX'})
            a['request']['evidenceRefs']=[alias('RECIPIENT-ACCEPTANCE')]
        check(sub,'precondition-recipient-acceptance','before','/data/rawRows/recipientAcceptances',[[alias('RECIPIENT-ACCEPTANCE'),alias('warehouse'),alias('DUTY'),alias('WORK2'),alias('WORK'),'20','BOX','VERIFIED','ACCEPTED',END]],'relationSet',field=['id','issuedById','sourceObligationId','sourceWorkId','targetWorkId','quantity','unit','verificationStatus','decision','validUntil'])
        check(sub,'precondition-recipient-binding','before','/data/rawRows/recipientAcceptances',[[alias('RECIPIENT-ACCEPTANCE'),1,1,False,'2026-10-07T08:00:00Z','2026-10-07T08:00:00Z','ONE_TRANSFER',hashlib.sha256((DIR/'recipient-acceptance.json').read_bytes()).hexdigest(),{'organizationId':alias('ORG-A'),'segmentId':alias('A20'),'quantity':'20','unit':'BOX'}]],'relationSet',field=['id','expectedSourceRevision','expectedTargetRevision','consumed','acceptedAt','validFrom','consumptionPolicy','sha256','scope'])
        for observation in ['before','after','authorized-after']:
            actions[observation]['observation']['sources']+= [x for x in ['recipientAcceptances','obligationTransfers'] if x not in actions[observation]['observation']['sources']]
        check(sub,'authorized-business-transfer-once','authorized-after','/data/rawRows/obligationTransfers',[[alias('DUTY'),alias('WORK2'),alias('WORK'),alias('warehouse'),alias('RECIPIENT-ACCEPTANCE'),'20','BOX',positive['request']['commandIdempotencyKey']]],'relationSet',field=['sourceObligationId','sourceWorkId','targetWorkId','acceptingOwnerId','acceptanceEvidenceId','quantity','unit','commandIdempotencyKey'])
    if cap in ['migrateWorkDefinition','transferObligation']:
        for name in (['workDefinitionMigrations','migrationMappings','migrationRegressions','goalVersions'] if cap=='migrateWorkDefinition' else ['recipientAcceptances','obligationTransfers']):
            check(sub,'precondition-denial-unchanged-'+name,'after','/data/rawRows/'+name,True,'sameAs')
            sub['assertions'][-1]['baseline']={'actionId':'before','pointer':'/data/rawRows/'+name}
    # Both calls use literally the same business payload/revision after prerequisite state is captured.
    positive['request']=copy.deepcopy(denied['request'])
    sub['actions'][1:1]=pre
    return sub

def compact_sub(s):
    lines=['    {']
    for k,v in s.items():
        if k in ['actions','assertions']:
            lines.append('      '+json.dumps(k)+': [')
            lines.extend('        '+dump(a)+(',' if i<len(v)-1 else '') for i,a in enumerate(v));lines.append('      ],')
        else:lines.append('      '+json.dumps(k)+': '+json.dumps(v,ensure_ascii=False)+',')
    lines[-1]=lines[-1].rstrip(',');lines.append('    }');return '\n'.join(lines)
def main():
    path=DIR/'case.json';original=path.read_text();n=json.loads(original)
    for cap in CAPS:prepare_fixture(cap)
    for s in n['subcases']:
        if s['id'] not in {r+'-'+c for c in CAPS for r in ['api','mcp','worker']}:continue
        marker='    {\n      "id": '+json.dumps(s['id'])+',';start=original.index(marker);end=original.find('\n    }',start)+len('\n    }')
        original=original[:start]+compact_sub(patch_sub(s,s['id'].split('-',1)[1]))+original[end:]
    path.write_text(original)
    feature=DIR/'scenario.feature';text=feature.read_text()
    for s in n['subcases']:
        if s['id'] not in {r+'-'+c for c in CAPS for r in ['api','mcp','worker']}:continue
        marker='  시나리오: '+s['title'];start=text.index(marker);end=text.find('\n  시나리오:',start+1)
        if end<0:end=len(text)
        lines=[marker,f'    먼저 사례 파일 "verification/cases/C3/case.json"의 "{s["id"]}"를 준비한다']
        lines += [f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in s['actions']]
        lines += [f'    그러면 "{a["id"]}" assertion으로 "{a["oracleRef"]["observationNames"][0]} {a["id"]}"를 확인한다' for a in s['assertions']]
        text=text[:start]+'\n'.join(lines)+'\n'+text[end:]
    feature.write_text(text)
    bindings=DIR/'observation-bindings.json';b=json.loads(bindings.read_text());b['caseHash']=hashlib.sha256(path.read_bytes()).hexdigest()
    for observation in b['observations']:
        observation['bindings']=[{'subcaseId':s['id'],'assertionId':a['id'],'pointer':f'/subcases/{i}/assertions/{j}','actionId':a['source']['actionId'],'op':a['op']}
            for i,s in enumerate(n['subcases']) for j,a in enumerate(s['assertions']) if a.get('oracleRef',{}).get('oracleId')==observation['oracleId'] and observation['observationName'] in a.get('oracleRef',{}).get('observationNames',[])]
    old=bindings.read_text()
    old=re.sub(r'"caseHash": "[^"]+"', '"caseHash": '+json.dumps(b['caseHash']), old)
    blocks=iter(b['observations'])
    def render_bindings(match):
        rows=next(blocks)['bindings']
        return '      "bindings": [\n'+'\n'.join('        '+dump(row)+(',' if i<len(rows)-1 else '') for i,row in enumerate(rows))+'\n      ]'
    old=re.sub(r'      "bindings": \[\n.*?\n      \]',render_bindings,old,flags=re.S)
    bindings.write_text(old)
if __name__=='__main__':main()
