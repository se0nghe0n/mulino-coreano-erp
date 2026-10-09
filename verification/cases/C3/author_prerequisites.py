#!/usr/bin/env python3
"""Author C3 valid-input counterexamples and no-effect oracles; no product transition or PASS evidence.

Idempotent post-processor over verification/cases/C3/case.json. It owns every
action/assertion whose id starts with precondition-, effect-, authorized-effect-,
authorized-business-, unchanged-effect-, reader-command- or query-, rewrites the
C3 Korean feature from the JSON, and refreshes observation-bindings.json.
Run: python3 -I verification/cases/C3/author_prerequisites.py
"""
import copy
import hashlib
import json
import re
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
def _request_contract():
    import importlib.util
    spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module.Contracts(ROOT)
REQUEST_CONTRACT = _request_contract()  # step2r round 12: every written case/fixture meets contracts/request-contracts.json
DIR = ROOT / 'verification/cases/C3'
NOW = '2026-10-07T09:00:00Z'
END = '2026-10-08T00:00:00Z'
CAPS = ('dispatchPurchaseOrder', 'migrateWorkDefinition', 'transferObligation')
RECALL_CAPS = ('approveRecall', 'recordRecallNotice', 'recordRecovery', 'closeRecall')
PREREQUISITE_CAPS = CAPS + RECALL_CAPS + ('emergencyReassign',)
ROUTES = ('api', 'mcp', 'worker')
ORG_SCOPE = {'organizationId': {'$alias': 'ORG-A'}}
# Primary effect sources of each COMMAND/RECORD capability that the shared 20-source
# observation does not already cover (plan §4.1/§6 entity catalog, backend/db/*.cds).
# A READ-grant denial must leave each of them unchanged organization-wide (plan §13.2 C3).
CAP_EFFECTS = {
 'splitQuantity':['genealogy'],'mergeQuantity':['genealogy'],'moveQuantity':['logisticsMemberships','custodyHandovers'],
 'adjustQuantity':['adjustmentProposals','stocktakes'],'reserveQuantity':['orders'],'replaceAllocation':['orders'],'releaseAllocation':['orders'],
 'pickQuantity':['logisticsMemberships'],'dispatchQuantity':['shipments','deliveries'],'confirmReceipt':['receipts','receiptContributions'],
 'disposeQuantity':['dispositions'],
 'createDraft':['goalVersions'],'cancelDraft':['goalVersions','workClosures'],'activateWork':['goalVersions'],'waitWork':['goalVersions'],
 'resumeWork':['goalVersions'],'reviseGoal':['goalVersions'],'closeWork':['workClosures'],'createFollowup':['workLinks'],'createWork':['goalVersions','workLinks'],
 'resolveObligation':['obligationResolutions','dutyTransitions'],'waiveObligation':['obligationResolutions','dutyTransitions'],'transferObligation':['obligationTransfers','dutyTransitions'],
 'registerItem':['items','externalIdentifiers','unitConversions'],'linkExternalId':['externalIdentifiers'],
 'attachEvidence':['documents','evidenceLinks','events'],'correctEvidence':['documents','evidenceLinks','events','evidence_revisions'],
 'proposePurchase':['proposals','purchaseRevisions'],'revisePurchase':['proposals','purchaseRevisions'],'approvePurchase':['proposals'],
 'dispatchPurchaseOrder':['purchaseOrders','proposals'],'recordSupplierReply':['supplierCommitments'],'cancelPurchase':['cancellationRequests','purchaseOrders'],
 'createShipment':['shipments','cargoAllocations','legs'],'recordLegEvent':['legs','events'],'recordHandover':['custodyHandovers','events'],
 'prepareRegulatoryProcedure':['procedures'],'recordSubmission':['submissions'],'recordRegulatoryDecision':['regulatoryDecisions','restrictions'],'verifyLabel':['labels'],
 'receiveProvisional':['receipts','events'],'placeHold':['restrictions'],'releaseHold':['restrictions'],
 'recordDispositionBasis':['dispositionBases'],'revokeDispositionBasis':['dispositionBases'],'recordStocktake':['stocktakes'],
 'createSalesOrder':['orders'],'reviseSalesOrder':['orders'],'recordDelivery':['deliveries','events'],'recordObservedMovement':['events','reconciliations'],
 'authorizeReturn':['returns'],'receiveReturn':['returns','restrictions'],'decideReturnDisposition':['returns','dispositions'],
 'openInvestigation':['investigations','restrictions'],'proposeRecall':['recallScopes','investigations'],'approveRecall':['recallScopes'],
 'recordRecallNotice':['notices','recallScopes'],'recordRecovery':['recoveries','recallScopes'],'closeRecall':['recallScopes','recallClosures'],
 'recordInvoice':['invoices'],'recordCharge':['charges','invoices'],'matchInvoice':['purchaseMatches','saleMatches','invoiceDifferences'],
 'recordSettlementAdjustment':['settlementAdjustments','invoiceDifferences'],'recordPaymentReference':['paymentReferences','bankTransfers'],
 'createGrant':['validityBoundaries'],'revokeGrant':['validityBoundaries'],'assignCapability':['managementAuthorities'],'revokeCapability':['managementAuthorities'],
 'proposeHandover':['handovers'],'acceptHandover':['handovers'],'rejectHandover':['handovers'],'emergencyReassign':['handovers'],
 'createPolicyDraft':['policyDrafts'],'approvePolicy':['policyApprovals'],'activatePolicy':['activePolicies'],'retirePolicy':['activePolicies'],
 'matchSourceIdentity':['identity_matches'],'linkCanonicalOccurrence':['canonical_links','canonicalOccurrences'],
 'resolveEvidenceConflict':['reconciliations','evidenceLinks'],'recordExternalReconciliation':['reconciliations','externalOperationResults'],
 'createDefinitionDraft':['definitionPackages'],'validateDefinition':['regressionRuns','definitionPackages'],'submitDefinitionReview':['definitionPackages'],
 'approveDefinition':['definitionApprovals'],'publishDefinition':['definitionPackages'],'activateDefinition':['activeDefinitionPointers'],
 'retireDefinition':['activeDefinitionPointers'],'migrateWorkDefinition':['workDefinitionMigrations','goalVersions'],
 'recordActivity':['activities','events'],'retrySafeCommand':['retryAttempts','executionAttempts'],'recordRelation':['workLinks'],
 'createWorkLink':['workLinks'],'emergencyRepair':['projections']}
EFFECT_OBSERVATION = {**{t:'inventory-effects' for t in ['genealogy','logisticsMemberships','custodyHandovers','adjustmentProposals','stocktakes','receipts','receiptContributions','dispositions','items','externalIdentifiers','unitConversions','restrictions','dispositionBases','returns','labels','recoveries']},
 **{t:'work-effects' for t in ['goalVersions','workClosures','workLinks','obligationResolutions','dutyTransitions','obligationTransfers','handovers','definitionPackages','regressionRuns','activeDefinitionPointers','workDefinitionMigrations','policyDrafts','activePolicies','validityBoundaries','managementAuthorities','projections','retryAttempts','executionAttempts']},
 **{t:'approval-effects' for t in ['policyApprovals','definitionApprovals']},
 **{t:'external-outbox-effects' for t in ['purchaseOrders','bankTransfers','externalOperationResults']}}
MODEL_QUERY_TOOL = {'01':'getInventory','03':'getObligations','05':'getInventory','08':'getObligations','09':'traceLot'}
OWNED_ACTION_PREFIXES = ('precondition-','effect-','authorized-effect-')
OWNED_ASSERTION_PREFIXES = ('precondition-','authorized-business-','unchanged-effect-','reader-command-','query-','tools-advertised','read-audit-')
def alias(name): return {'$alias': name}
def typed(value, provenance='CONTEXT'): return {'value':value,'provenance':provenance}
def ref(action, pointer): return {'$result': {'actionId': action, 'pointer': pointer}}
def dump(value): return json.dumps(value, ensure_ascii=False, separators=(',', ':'))
def save(path, value): path.write_text(json.dumps(REQUEST_CONTRACT.conform_fixture(value) if 'actors' in value else value, ensure_ascii=False, indent=2)+'\n')
def invoke(sub, id, actor, cap, slots, revision=1, subject=None):
    return {'id':id,'kind':'invoke','actorRef':actor,'route':'api','capabilityId':cap,
            'request':{'intentKind':'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,
                       'scope':copy.deepcopy(sub['assertions'][0]['scope']),
                       'subjectRefs':subject or [],'slots':slots,'asOf':NOW,'knownAt':NOW,
                       'expectedRevision':revision,'commandIdempotencyKey':'C3-'+sub['id']+'-'+id},
            'evidenceRefs':[id+':actual-artifact']}
def check(sub, id, action, pointer, expected, op='equals', field=None, where=None, observation='work-effects', explain=None):
    """Positive prerequisite/counter-call assertion bound to the effect class it validates (not read-audit)."""
    value=copy.deepcopy(next(a for a in sub['assertions'] if a['id']=='attempt-code'))
    value.update(id=id,op=op,source={'actionId':action,'pointer':pointer},expected=expected,evidenceRefs=[action+':actual-artifact'],
        oracleExplanation=explain or '현재 권한 외 업무 전제를 성립시키고 승인·수락·전환 결과와 같은 command의 실제 효과를 대조한다.')
    value['oracleRef']={'oracleId':'C3.read-grant-all-writes','observationNames':[observation]}
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
    auth_key='C3-'+sub['id']+'-authorized'
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
        check(sub,'precondition-approved','precondition-approval','/response/outcome','APPLIED',observation='approval-effects')
        check(sub,'precondition-approval-record','before','/data/rawRows/approvals',[[ref('precondition-approval','/response/approvalId'),alias('PROPOSAL'),ref('precondition-proposal','/response/data/proposalHash'),ref('precondition-proposal','/response/data/proposalRevision'),alias('manager'),'APPROVE',NOW,END,'SINGLE_ORDER_REVISION']],'relationSet',field=['id','proposalId','proposalHash','proposalRevision','approverId','decision','decidedAt','validUntil','consumptionPolicy'],where={'id':ref('precondition-approval','/response/approvalId')},observation='approval-effects')
        check(sub,'authorized-business-outbox-once','authorized-after','/data/rawRows/outbox',[[alias('PROPOSAL'),ref('precondition-proposal','/response/data/proposalHash'),ref('precondition-approval','/response/approvalId'),alias('EXTERNAL-OP'),auth_key]],'relationSet',field=['proposalId','proposalHash','approvalId','externalOperationId','commandIdempotencyKey'],where={'capabilityId':cap},observation='external-outbox-effects')
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
        check(sub,'authorized-business-migration-once','authorized-after','/data/rawRows/workDefinitionMigrations',[[alias('WORK'),target,'C3-v1-to-v2',ref('precondition-migration-approval','/response/approvalId'),auth_key]],'relationSet',field=['workId','targetDefinitionId','migrationId','approvalId','commandIdempotencyKey'],where={'workId':alias('WORK')})
        check(sub,'precondition-migration-approval-record','before','/data/rawRows/approvals',[[ref('precondition-migration-approval','/response/approvalId'),target,ref('precondition-publish','/response/hash'),ref('precondition-migration-review','/response/reviewId'),alias('configApprover'),'WORK_MIGRATION','APPROVED',NOW,END]],'relationSet',field=['id','definitionId','hash','reviewId','decisionActorId','reviewKind','decision','validFrom','validUntil'],where={'id':ref('precondition-migration-approval','/response/approvalId')},observation='approval-effects')
        check(sub,'authorized-business-goal-history','authorized-after','/data/rawRows/goalVersions',[[alias('GOAL-V1'),'definition-v1','ARRIVED','100','BOX'],[ref('authorized-same-input','/response/goalVersionId'),'definition-v2','ARRIVED','100','BOX']],'relationSet',field=['id','definitionVersion','endpoint','quantity','unit'])
        check(sub,'authorized-business-migration-goal-link','authorized-after','/data/rawRows/workDefinitionMigrations',[[alias('WORK'),alias('GOAL-V1'),ref('authorized-same-input','/response/goalVersionId'),alias('delegator'),ref('precondition-migration-approval','/response/approvalId'),'C3-v1-to-v2']],'relationSet',field=['workId','previousGoalVersionId','goalVersionId','actorId','approvalId','migrationId'],where={'workId':alias('WORK')})
    elif cap=='transferObligation':
        scope={'organizationId':alias('ORG-A'),'itemId':alias('P'),'workIds':[alias('WORK2'),alias('WORK')],'obligationRootId':alias('DUTY'),'includeDescendants':True}
        for a in sub['assertions']:a['scope']=copy.deepcopy(scope)
        for a in sub['actions']:
            # step2r round 12: a target read keeps its own object scope (contracts/request-contracts.json objectType).
            if 'scope' in a.get('request',{}) and a.get('capabilityId')!='getObject':a['request']['scope']=copy.deepcopy(scope)
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
        check(sub,'authorized-business-transfer-once','authorized-after','/data/rawRows/obligationTransfers',[[alias('DUTY'),alias('WORK2'),alias('WORK'),alias('warehouse'),alias('RECIPIENT-ACCEPTANCE'),'20','BOX',auth_key]],'relationSet',field=['sourceObligationId','sourceWorkId','targetWorkId','acceptingOwnerId','acceptanceEvidenceId','quantity','unit','commandIdempotencyKey'])
    if cap in RECALL_CAPS:
        # Plan §6/§13.1 D18·§13.3 E2: recall decisions need a current ADMIN approval of the exact scope;
        # closure needs notice, recovery and disposal of the whole approved scope and a closure record.
        query=copy.deepcopy(actions['target-before']);query.update(id='precondition-recall',actorRef='delegator',request={'objectId':alias('RECALL')},evidenceRefs=['precondition-recall:actual-artifact']);pre.append(query)
        scope=lambda:{'recallId':alias('RECALL'),'scopeVersion':ref('precondition-recall','/response/data/scopeVersion'),'scopeHash':ref('precondition-recall','/response/data/scopeHash')}
        approval_slots=dict(scope(),decision='APPROVED',validUntil=END,reason='승인 scope20의 회수 결정')
        approved=lambda:dict(scope(),approvalId=ref('precondition-approval','/response/approvalId'))
        notice_slots=lambda:dict(approved(),noticeEvidenceId=alias('RECALL-NOTICE-DOC'),noticedAt=NOW,channel='SYNTHETIC_CUSTOMER_NOTICE')
        recovery_slots=lambda outcome,doc:dict(approved(),quantity={'value':'20','unit':'BOX'},physicalScopeId=alias('A20'),currentLocationId=alias('W2'),outcome=outcome,evidenceId=alias(doc),occurredAt=NOW)
        subject=[{'type':'Recall','id':alias('RECALL')}]
        if cap!='approveRecall':
            pre.append(invoke(sub,'precondition-approval','admin','approveRecall',approval_slots,ref('precondition-recall','/response/data/revision'),subject))
            check(sub,'precondition-approval-applied','precondition-approval','/response/outcome','APPLIED',observation='approval-effects')
            check(sub,'precondition-approval-record','before','/data/rawRows/approvals',[[ref('precondition-approval','/response/approvalId'),alias('admin'),'APPROVED']],'relationSet',field=['id','approverId','decision'],where={'id':ref('precondition-approval','/response/approvalId')},observation='approval-effects',
                explain='회수 승인은 ADMIN management authority를 가진 admin이 정확한 scope hash/version에 대해 먼저 확정한다.')
        if cap=='closeRecall':
            # Each step re-reads the recall and sends that current revision, so the chain holds whether or not
            # approval, notice or recovery bumps the recall revision (no design is assumed either way).
            def reread(after):
                q=copy.deepcopy(query);q.update(id='precondition-recall-after-'+after,evidenceRefs=['precondition-recall-after-'+after+':actual-artifact']);pre.append(q)
                return ref(q['id'],'/response/data/revision')
            pre.append(invoke(sub,'precondition-notice','admin','recordRecallNotice',notice_slots(),reread('approval'),subject))
            pre[-1]['request']['intentKind']='RECORD'
            pre.append(invoke(sub,'precondition-recovery','delegator','recordRecovery',recovery_slots('RECOVERED','RECALL-RECOVERY-DOC'),reread('notice'),subject))
            pre[-1]['request']['intentKind']='RECORD'
            pre.append(invoke(sub,'precondition-disposal','delegator','recordRecovery',recovery_slots('DISPOSED','RECALL-DISPOSAL-DOC'),reread('recovery'),subject))
            pre[-1]['request']['intentKind']='RECORD'
            for id in ['precondition-notice','precondition-recovery','precondition-disposal']:check(sub,id+'-applied',id,'/response/outcome','APPLIED',observation='followup-effects')
        if cap=='approveRecall':slots=approval_slots
        elif cap=='recordRecallNotice':slots=notice_slots()
        elif cap=='recordRecovery':slots=recovery_slots('RECOVERED','RECALL-RECOVERY-DOC')
        else:slots=dict(approved(),partitionHash=ref('precondition-disposal','/response/partitionHash'),closureEvidenceId=alias('RECALL-CLOSURE-DOC'),reason='승인 scope20 전부 회수 후 같은 실물 폐기 대조 완료')
        for a in [denied,positive]:a['request']['slots']=copy.deepcopy(slots);a['request']['subjectRefs']=subject;a['request']['evidenceRefs']=[alias('DOC')]
        positive['actorRef']='admin' if cap in ['approveRecall','closeRecall','recordRecallNotice'] else 'delegator'
        for observation in ['before','after','authorized-after']:
            actions[observation]['observation']['sources']+= [x for x in ['recallScopes','recallClosures','notices','recoveries'] if x not in actions[observation]['observation']['sources']]
        if cap=='approveRecall':
            check(sub,'authorized-business-recall-approval','authorized-after','/data/rawRows/approvals',[[ref('authorized-same-input','/response/approvalId'),alias('admin'),'APPROVED']],'relationSet',field=['id','approverId','decision'],where={'id':ref('authorized-same-input','/response/approvalId')},observation='approval-effects')
        elif cap=='recordRecallNotice':
            check(sub,'authorized-business-notice','authorized-after','/data/rawRows/notices',[[alias('RECALL'),ref('precondition-approval','/response/approvalId'),alias('RECALL-NOTICE-DOC')]],'relationSet',field=['recallId','approvalId','evidenceId'],where={'recallId':alias('RECALL')},observation='followup-effects')
        elif cap=='recordRecovery':
            check(sub,'authorized-business-recovery','authorized-after','/data/rawRows/recoveries',[[alias('RECALL'),ref('precondition-approval','/response/approvalId'),'20','BOX','RECOVERED',alias('W2')]],'relationSet',field=['recallId','approvalId','quantity','unit','outcome','currentLocationId'],where={'recallId':alias('RECALL')},observation='inventory-effects')
        else:
            check(sub,'precondition-not-closed','before','/data/rawRows/recallClosures',0,'count',where={'recallId':alias('RECALL')},observation='followup-effects')
            check(sub,'authorized-business-recall-closure','authorized-after','/data/rawRows/recallClosures',[[alias('RECALL'),ref('precondition-approval','/response/approvalId'),alias('admin'),'20','20','0','0','BOX']],'relationSet',
                field=['recallId','approvalId','closedBy','recoveredQuantity','disposedQuantity','exceptionQuantity','unknownQuantity','unit'],where={'recallId':alias('RECALL')},observation='followup-effects',
                explain='회수20·같은 실물 폐기20은 처리량20이며 미확인0·예외0일 때만 ADMIN이 종료한다. 회수와 폐기를 더해40으로 세지 않는다.')
    if cap=='emergencyReassign':
        # Plan §5/§7.2: ADMIN reassigns the current responsibility with its own reason and audit, not acceptHandover's payload.
        actions['target-before']['request']={'objectId':alias('WORK')};actions['authorized-target']['request']={'objectId':alias('WORK')}
        slots={'workId':alias('WORK'),'obligationId':alias('DUTY'),'newOwnerId':alias('delegator'),'reason':'기존 담당 부재로 즉시 후속 대응','effectiveAt':NOW}
        for a in [denied,positive]:a['request']['slots']=copy.deepcopy(slots);a['request']['subjectRefs']=[{'type':'Work','id':alias('WORK')}]
        positive['actorRef']='admin'
        check(sub,'precondition-current-owner','before','/data/rawRows/works',[alias('warehouse')],'equals',field='ownerId',where={'id':alias('WORK')})
        check(sub,'authorized-business-new-owner','authorized-after','/data/rawRows/works',[alias('delegator')],'equals',field='ownerId',where={'id':alias('WORK')})
        check(sub,'authorized-business-emergency-audit','authorized-after','/data/rawRows/audit',[[alias('admin'),'emergencyReassign','기존 담당 부재로 즉시 후속 대응',alias('WORK')]],'relationSet',field=['actorId','capabilityId','reason','workId'],where={'commandIdempotencyKey':auth_key})
    if cap in ['migrateWorkDefinition','transferObligation']:
        for name in (['workDefinitionMigrations','migrationMappings','migrationRegressions','goalVersions'] if cap=='migrateWorkDefinition' else ['recipientAcceptances','obligationTransfers']):
            check(sub,'precondition-denial-unchanged-'+name,'after','/data/rawRows/'+name,True,'sameAs')
            sub['assertions'][-1]['baseline']={'actionId':'before','pointer':'/data/rawRows/'+name}
    sub['actions'][1:1]=pre
    return sub

def effect_observe(id, snapshot, sources):
    return {'id':id,'kind':'observe','observation':{'scope':copy.deepcopy(ORG_SCOPE),'snapshotRef':ref(snapshot,'/response/snapshotRevision'),'asOf':NOW,'knownAt':NOW,'sources':list(sources)},'evidenceRefs':[id+':actual-artifact']}
def finish_route_sub(sub, cap):
    """Every api/mcp/worker denial: organization-wide primary effect tables and the reader's own command."""
    sub['actions']=[a for a in sub['actions'] if not a['id'].startswith(('effect-','authorized-effect-'))]
    sub['assertions']=[a for a in sub['assertions'] if not a['id'].startswith(('unchanged-effect-','reader-command-'))]
    actions={a['id']:a for a in sub['actions']};denied=actions['attempt'];positive=actions['authorized-same-input']
    attempt_key='C3-'+sub['id']+'-attempt';auth_key='C3-'+sub['id']+'-authorized'
    # Same business payload and revision; a separate key keeps cross-principal key semantics (plan §7.3 allows
    # an independent namespace or a refusal) out of the authority proof.
    request=copy.deepcopy(denied['request']);request['commandIdempotencyKey']=auth_key
    if positive['request'].get('expectedRevision')!=denied['request'].get('expectedRevision'):request['expectedRevision']=positive['request']['expectedRevision']
    positive['request']=request
    for a in sub['assertions']:
        if a['id']=='authorized-committed-once':
            a['source']['where'].update(commandIdempotencyKey=auth_key,stableRequestOwnerId=alias(positive['actorRef']))
    sources=CAP_EFFECTS[cap]
    ids=[a['id'] for a in sub['actions']]
    sub['actions'].insert(ids.index('before')+1,effect_observe('effect-before','before-snapshot',sources))
    ids=[a['id'] for a in sub['actions']]
    sub['actions'].insert(ids.index('after')+1,effect_observe('effect-after','after-snapshot',sources))
    template=next(a for a in sub['assertions'] if a['id']=='unchanged-segments')
    for t in sources:
        x=copy.deepcopy(template);x.update(id='unchanged-effect-'+t,source={'actionId':'effect-after','pointer':'/data/rawRows/'+t},baseline={'actionId':'effect-before','pointer':'/data/rawRows/'+t},
            evidenceRefs=['effect-after:actual-artifact','effect-before:actual-artifact'],scope=copy.deepcopy(ORG_SCOPE),
            oracleExplanation=f'{cap}의 주 효과 원천 {t} 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다.')
        x['oracleRef']={'oracleId':'C3.read-grant-all-writes','observationNames':[EFFECT_OBSERVATION.get(t,'followup-effects')]}
        sub['assertions'].append(x)
    x=copy.deepcopy(next(a for a in sub['assertions'] if a['id']=='followup-effects-rows0'))
    x.update(id='reader-command-not-committed',source={'actionId':'after','pointer':'/data/rawRows/commands','where':{'commandIdempotencyKey':attempt_key,'stableRequestOwnerId':alias('reader'),'status':'COMMITTED'}},
        oracleExplanation='거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다.')
    x['oracleRef']={'oracleId':'C3.read-grant-all-writes','observationNames':['work-effects']}
    sub['assertions'].append(x)
    return sub
def model_query(sub, public):
    """The query/write boundary is exercised only if MCP tools were offered and a real read happened."""
    sub['assertions']=[a for a in sub['assertions'] if not a['id'].startswith(('query-','tools-advertised','read-audit-','skill-stage-','no-write-intent-'))]
    template=next(a for a in sub['assertions'] if a['id']=='actual-client-kind')
    def add(id,op,source,expected,explain,observation='evaluation-path',oracle='C3.model-query-write-boundary'):
        x=copy.deepcopy(template);x.update(id=id,op=op,source=source,expected=expected,oracleExplanation=explain,evidenceRefs=[source['actionId']+':actual-artifact'])
        x['oracleRef']={'oracleId':oracle,'observationNames':observation if isinstance(observation,list) else [observation]};sub['assertions'].append(x)
    add('tools-advertised','exactSet',{'actionId':'query-agent','pointer':'/data/toolTranscript/toolsList','field':'name'},public,
        '실제 client가 서버 tools/list로 받은 공개 도구 전체(쓰기 도구 포함)를 기록한다. 도구가 없거나 숨겨진 client의 빈 transcript는 쓰기 경계를 시험하지 않는다.')
    add('query-tool-called','fieldsPresent',{'actionId':'query-agent','pointer':'/data/toolTranscript/calls','where':{'intentKind':'QUERY','actorId':alias('reader')}},['capabilityId','intentKind','serverOutcome','jsonRpcId'],
        '조회 문장은 인증된 reader의 실제 MCP QUERY tools/call을 한 번 이상 남긴다. 아무 도구도 부르지 않은 응답은 의도 해석의 증거가 아니다.')
    number=sub['id'].split('-')[2]
    if number in MODEL_QUERY_TOOL:
        cap=MODEL_QUERY_TOOL[number]
        add('query-tool-'+cap,'fieldsPresent',{'actionId':'query-agent','pointer':'/data/toolTranscript/calls','where':{'capabilityId':cap,'actorId':alias('reader')}},['capabilityId','serverOutcome','jsonRpcId'],
            f'이 문장의 답은 {cap} 조회 없이 얻을 수 없다. 다른 조회를 추가로 부르는 것은 허용한다.')
    # SKILLS layer (plan §9.2, §13.3): the boundary is judged while the host has actually discovered and
    # loaded a skill body, recorded per stage. A trace of file hashes alone does not prove loading.
    ALL_OBS=['query-intent-write-tool-execution','query-intent-business-effects','evaluation-path']
    for stage,meaning in [('DISCOVERED','metadata 검색 노출'),('BODY_READ','본문 loading')]:
        add('skill-stage-'+stage.lower().replace('_','-'),'fieldsPresent',{'actionId':'query-agent','pointer':'/data/skillLoadingTrace','where':{'stage':stage}},['packageName','stage','path','sha256','loadedAt'],
            f'실제 host가 이 조회 문장을 처리하며 skill {meaning}({stage}) 단계를 package·경로·hash·시각과 함께 한 건 이상 남긴다. skill 없이 얻은 무효과는 SKILLS 계층의 쓰기 경계 증거가 아니다(계획 §9.2).',observation=ALL_OBS)
    # MCP layer for the business-effect observation: zero write intents at the protocol boundary,
    # independent of the per-capability counts and of the DB before/after rows.
    for kind in ['COMMAND','RECORD']:
        add('no-write-intent-'+kind.lower(),'count',{'actionId':'query-agent','pointer':'/data/toolTranscript/calls','where':{'intentKind':kind}},0,
            f'실제 MCP tools/call transcript에서 {kind} 의도의 호출은 0건이다. 조회 문장은 쓰기 도구 실행으로 바뀌지 않으며 DB 전후 원행 불변과 함께 업무 효과0을 보인다.',observation=['query-intent-write-tool-execution','query-intent-business-effects'])
    # Plan §7.4: reads are audited apart from business changes, in the queryAudit source
    # (contracts/audit-observation-fields.json), never as command audit rows.
    after=next(a for a in sub['actions'] if a['id']=='after' and a['kind']=='observe')
    if 'queryAudit' not in after['observation']['sources']:after['observation']['sources'].append('queryAudit')
    add('read-audit-reader','fieldsPresent',{'actionId':'after','pointer':'/data/rawRows/queryAudit','where':{'actorId':alias('reader')}},['capabilityId','outcome'],
        '조회 감사(queryAudit)는 실제 인증 주체 reader의 조회 capability와 READ/REJECTED 결과를 한 행 이상 남기며 명령 감사·업무 변경으로 세지 않는다.',observation='read-audit-permitted',oracle='C3.read-grant-all-writes')
    return sub

def prepare_recall_fixture(cap):
    path=DIR/('fixture-'+cap+'.json');f=json.loads(path.read_text());b=f['baseline']
    names=['approveRecall','closeRecall','recordRecallNotice','emergencyReassign','getObject','getEvidence','getWork','getInventory']
    allow(f['actors']['admin'],names)
    authorities=[{'actorAlias':'admin','capabilityId':c,'scopeKind':'ORGANIZATION','scopeAlias':'ORG-A','validFrom':'2026-10-07T00:00:00Z','validUntil':END,'revokedAt':None} for c in (['approveRecall'] if cap in RECALL_CAPS else ['emergencyReassign'])]
    b['managementAuthorities']=authorities
    if cap in RECALL_CAPS:
        f['aliases']['INVESTIGATION']={'type':'Investigation','organizationAlias':'ORG-A'}
        b['priorEntities']['INVESTIGATION']={'revision':1,'state':'PROPOSED','impactState':'CANDIDATE','segmentAlias':'A20','lotAlias':'L','itemAlias':'P','workAlias':'WORK','startQuantity':'0','quantity':'20','unit':'BOX','ownerAlias':'warehouse','supervisorAlias':'supervisor','nextAction':'회수 scope 승인과 실물 처리 대조','nextCheckAt':'2026-10-07T10:00:00Z'}
        recall={'revision':1,'state':'PROPOSED','investigationAlias':'INVESTIGATION','rootSegmentAlias':'A20','lotAlias':'L','itemAlias':'P','workAlias':'WORK','startQuantity':'0','quantity':'20','unit':'BOX','scopeVersion':'1'}
        recall['scopeHash']=hashlib.sha256(json.dumps({k:v for k,v in recall.items() if k not in ['revision','state']},sort_keys=True,separators=(',',':')).encode()).hexdigest()
        b['priorEntities']['RECALL']=recall
        for doc,kind,text in [('RECALL-NOTICE-DOC','RECALL_NOTICE','가상 고객 회수 통지 20BOX'),('RECALL-RECOVERY-DOC','RECALL_RECOVERY','가상 회수 실물 20BOX W2 입고'),('RECALL-DISPOSAL-DOC','RECALL_DISPOSAL','가상 회수 실물 20BOX 폐기 확인'),('RECALL-CLOSURE-DOC','RECALL_CLOSURE','가상 회수 종료 대조 처리20 미확인0')]:
            content=json.dumps({'synthetic':True,'kind':kind,'recallAlias':'RECALL','scopeQuantity':'20','unit':'BOX','statement':text},ensure_ascii=False,sort_keys=True)+'\n'
            (DIR/'recall-evidence').mkdir(exist_ok=True);(DIR/'recall-evidence'/(doc.lower()+'.json')).write_text(content)
            digest=hashlib.sha256(content.encode()).hexdigest()
            f['aliases'][doc]={'type':'DocumentVersion','organizationAlias':'ORG-A'}
            f['evidence']=[x for x in f['evidence'] if x.get('alias')!=doc]+[{'alias':doc,'sha256':digest,'sourceNamespace':'synthetic-recall-evidence','externalEventId':'C3-'+doc.lower(),'sourceVersion':'1','occurredAt':NOW,'recordedAt':NOW}]
            b['documents']=[x for x in b['documents'] if x['alias']!=doc]+[{'alias':doc,'path':'verification/cases/C3/recall-evidence/'+doc.lower()+'.json','mediaType':'application/json','immutable':True,'sha256':digest}]
    save(path,f)

def compact_sub(s):
    lines=['    {']
    for k,v in s.items():
        if k in ['actions','assertions']:
            lines.append('      '+json.dumps(k)+': [')
            lines.extend('        '+dump(a)+(',' if i<len(v)-1 else '') for i,a in enumerate(v));lines.append('      ],')
        else:lines.append('      '+json.dumps(k)+': '+json.dumps(v,ensure_ascii=False)+',')
    lines[-1]=lines[-1].rstrip(',');lines.append('    }');return '\n'.join(lines)
def render_case(n):
    head=['{']+['  '+json.dumps(k)+': '+json.dumps(v,ensure_ascii=False)+',' for k,v in n.items() if k!='subcases']
    return '\n'.join(head+['  "subcases": [',',\n'.join(compact_sub(s) for s in n['subcases']),'  ]','}'])+'\n'
def render_feature(n, header):
    lines=header[:]
    for s in n['subcases']:
        lines += ['','  @uat @model' if s['id'].startswith('model-query') else '  @sit','  시나리오: '+s['title'],f'    먼저 사례 파일 "verification/cases/C3/case.json"의 "{s["id"]}"를 준비한다']
        lines += [f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in s['actions']]
        lines += [f'    그러면 "{a["id"]}" assertion으로 "{a["oracleRef"]["observationNames"][0]} {a["id"]}"를 확인한다' for a in s['assertions']]
    return '\n'.join(lines)+'\n'
def self_check(n):
    """Structural guard: every route subcase observes its primary effect tables and uses a separate counter-call key."""
    for s in n['subcases']:
        if s['id'].startswith('model-query'):
            ids={a['id'] for a in s['assertions']};assert {'tools-advertised','query-tool-called','read-audit-reader','skill-stage-discovered','skill-stage-body-read','no-write-intent-command','no-write-intent-record'}<=ids,s['id'];continue
        route,cap=s['id'].split('-',1);assert route in ROUTES
        acts={a['id']:a for a in s['actions']}
        assert acts['effect-before']['observation']['sources']==CAP_EFFECTS[cap]==acts['effect-after']['observation']['sources'],s['id']
        assert {'unchanged-effect-'+t for t in CAP_EFFECTS[cap]}<={a['id'] for a in s['assertions']},s['id']
        assert acts['attempt']['request']['commandIdempotencyKey']!=acts['authorized-same-input']['request']['commandIdempotencyKey'],s['id']
        strip=lambda r:{k:v for k,v in r.items() if k not in ['commandIdempotencyKey','expectedRevision']}
        assert strip(acts['attempt']['request'])==strip(acts['authorized-same-input']['request']),s['id']
        assert all(x['oracleRef']['observationNames']!=['read-audit-permitted'] for x in s['assertions'] if x['id'].startswith(('precondition-','authorized-business-'))),s['id']
def main():
    path=DIR/'case.json';n=json.loads(path.read_text())
    public=[c['id'] for c in json.loads((ROOT/'contracts/acceptance-capabilities.json').read_text())['capabilities']]
    for cap in CAPS:prepare_fixture(cap)
    for cap in RECALL_CAPS+('emergencyReassign',):prepare_recall_fixture(cap)
    if 'skills' not in n['profiles']:n['profiles'].append('skills')
    for s in n['subcases']:
        if s['id'].startswith('model-query'):model_query(s,public);continue
        cap=s['id'].split('-',1)[1]
        if cap in PREREQUISITE_CAPS:patch_sub(s,cap)
        finish_route_sub(s,cap)
    REQUEST_CONTRACT.conform_case(n)
    self_check(n)
    path.write_text(render_case(n))
    feature=DIR/'scenario.feature';header=feature.read_text().split('\n')[:3]
    feature.write_text(render_feature(n,header))
    bindings=DIR/'observation-bindings.json';b=json.loads(bindings.read_text());b['caseHash']=hashlib.sha256(path.read_bytes()).hexdigest()
    # The bindings are derived from the case and bound against the normative catalog, so both stamps are recomputed
    # (verification/requirements/check_derived_bindings.py). The observation list must still be the catalog's C3 list.
    catalog_path=ROOT/'verification/requirements/mandatory-oracles.json'
    b['catalogSha256']=hashlib.sha256(catalog_path.read_bytes()).hexdigest()
    catalog=json.loads(catalog_path.read_text())
    expected=[(o['oracleId'],x['name']) for o in catalog['oracles'] if o['caseId']=='C3' for x in o['expectedObservations']]
    assert [(o['oracleId'],o['observationName']) for o in b['observations']]==expected,'C3 observation-bindings observations differ from the catalog'
    for observation in b['observations']:
        observation['bindings']=[{'subcaseId':s['id'],'assertionId':a['id'],'pointer':f'/subcases/{i}/assertions/{j}','actionId':a['source']['actionId'],'op':a['op']}
            for i,s in enumerate(n['subcases']) for j,a in enumerate(s['assertions']) if a.get('oracleRef',{}).get('oracleId')==observation['oracleId'] and observation['observationName'] in a.get('oracleRef',{}).get('observationNames',[])]
    old=bindings.read_text()
    old=re.sub(r'"caseHash": "[^"]+"', '"caseHash": '+json.dumps(b['caseHash']), old)
    old=re.sub(r'"catalogSha256": "[^"]+"', '"catalogSha256": '+json.dumps(b['catalogSha256']), old)
    blocks=iter(b['observations'])
    def render_bindings(match):
        rows=next(blocks)['bindings']
        return '      "bindings": [\n'+'\n'.join('        '+dump(row)+(',' if i<len(rows)-1 else '') for i,row in enumerate(rows))+'\n      ]'
    old=re.sub(r'      "bindings": \[\n.*?\n      \]',render_bindings,old,flags=re.S)
    bindings.write_text(old)
if __name__=='__main__':main()
