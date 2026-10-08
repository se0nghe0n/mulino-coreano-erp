#!/usr/bin/env python3
"""Build fixed acceptance declarations, never product behavior or execution evidence."""
import json, hashlib, copy
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
H='/data/hostObservation/extractor/rawRows/facts/'
D='/data/rawRows/'
T='2026-10-07T09:00:00Z'; K='2026-10-07T09:00:01Z'
B2='feaca0af9673620eff9a5ac0f08a657ce14e9ccd'
ORACLES={
 'archive':('T23.archive-and-data-branch',['archive-restored','data-branch','manufacture-as-import-relabel']),
 'schema':('T23.schema-ownership-upgrade-drift',['schema-owner','schema-drift']),
 'cutover':('T23.cutover-and-safe-recovery',['cutover-order','recovery-boundaries','SQLRollbackPretendedPhysicalContractCancel']),
 'fresh':('V8.fresh-install',['fresh-integration']),
 'upgrade':('V8.ontology-upgrade-preservation',['upgrade-preserves','legacy-migration-substitutes-ontology-upgrade']),
 'restore':('V8.db-blob-definition-restore',['restore-artifacts','missing-blob-restore-complete','missing-evaluator-restore-complete']),
 'environment':('V8.btp-client-separate-gates',['btp-acceptance','result-scope'])}
def ref(action,pointer):return {'$result':{'actionId':action,'pointer':pointer}}
def alias(name):return {'$alias':name}
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def descriptor(name):
 p=ROOT/'verification/platform-tests/inputs'/name;b=p.read_bytes()
 return {'path':str(p.relative_to(ROOT)),'sha256':hashlib.sha256(b).hexdigest(),'sizeBytes':len(b),'scope':{'caseFamily':'platform'},'completeness':'COMPLETE'}
INPUTS=[descriptor(n) for n in ['archive-sentinel.txt','source-inventory.json','schema-delta-contract.json','environment-gates.json','receipt-evidence.json']]
class Case:
 def __init__(self,id,title,profiles):self.id=id;self.title=title;self.profiles=profiles;self.subs=[]
 def sub(self,id,title,oracle,variant='baseline'):
  self.s=Sub(self,id,title,oracle,variant);self.subs.append(self.s);return self.s
 def save(self):
  path=f'verification/cases/{self.id}/case.json';requirement=['D23'] if self.id=='T23' else ['D23','D24','D26']
  for s in self.subs:
   if any(a.get('route')=='mcp' for a in s.data['actions']) and 'mcp' not in s.data['requiredAdapters']:s.data['requiredAdapters'].append('mcp')
  write(path,{'schemaVersion':'1.0.0','caseId':self.id,'title':self.title,'requirementRefs':requirement,'profiles':self.profiles,'subcases':[s.data for s in self.subs]})
  lines=['# language: ko',f'@{self.id} @D23 '+('@D24 @D26 ' if self.id=='V8' else '')+'@contract-red @sit',f'기능: {self.title}']
  for s in self.subs:
   lines.extend(['',f'  시나리오: {s.data["title"]}',f'    먼저 사례 파일 "{path}"의 "{s.id}"를 준비한다'])
   for a in s.data['actions']:lines.append(f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다')
   for a in s.data['assertions']:lines.append(f'    그러면 "{a["id"]}" assertion으로 "{a["oracleExplanation"]}"를 확인한다')
  (ROOT/Path(path).parent/'scenario.feature').write_text('\n'.join(lines)+'\n')
class Sub:
 def __init__(self,c,id,title,oracle,variant):
  self.c=c;self.id=id;self.oracle=oracle;self.env=f'{c.id}-{id}';self.scope={'organizationId':alias('ORG'),'environmentId':self.env}
  fixture=f'verification/cases/{c.id}/fixtures/{id}.json';write(fixture,make_fixture(self.env,variant))
  self.data={'id':id,'title':title,'fixtureRef':fixture,'requiredAdapters':['fixture','host','api','db']+(['btp'] if id=='btp-auth-binding-tls-wire' else ['client'] if id=='supported-client-separate' else []),'actions':[{'id':'setup','kind':'installFixture','evidenceRefs':['setup:actual-installation-aliases-hash']}], 'assertions':[],'oracleExplanation':title+'.가상 입력은 운영 자료나 실제 실행 증거가 아니다. 실행하지 않은 제품 경로는 NOT_RUN이다.'}
 def action(self,id,kind,**kw):
  a={'id':id,'kind':kind,**kw,'evidenceRefs':[id+':actual-artifact']};self.data['actions'].append(a);return id
 def host(self,id,op,**parameters):
  p={'scope':self.scope,'environmentId':self.env,'profile':'LOCAL',**parameters}
  self.action(id,'control',control={'type':'process','operation':op,'parameters':p})
  return id
 def inspect(self,source):return self.host(source+'-inspect','inspectArtifacts',inspectionId=source+'-inspection',artifacts=ref(source,'/data/hostObservation/generatedOutputs'))
 def query(self,id,cap='getInventory',actor='reader',route='api',**request):
  scope={**self.scope,'environmentId':request.pop('environmentId',self.env)}
  if cap=='getInventory':scope.update({'itemId':alias('P'),'placeId':alias('W')})
  return self.action(id,'query',actorRef=actor,route=route,capabilityId=cap,request={'scope':scope,'asOf':T,'knownAt':K,**request})
 def invoke(self,id,cap,slots=None,actor='warehouse',route='api',**request):
  return self.action(id,'invoke',actorRef=actor,route=route,capabilityId=cap,request={'intentKind':'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,'subjectRefs':[{'type':'QuantitySegment','id':alias('A60')}],'slots':slots or {},'expectedRevision':1,'commandIdempotencyKey':self.env+'-'+id,**request})
 def observe(self,id,snapshot='CURRENT_COMMITTED',**scope):
  return self.action(id,'observe',observation={'scope':{**self.scope,'itemId':alias('P'),'placeIds':[alias('W'),alias('TRANSIT')],'inventoryMeasurePlaceId':alias('W'),**scope},'snapshotRef':snapshot,'asOf':T,'knownAt':K,'sources':['segments','genealogy','movements','allocations','works','goalVersions','assessments','obligations','definitions','evidence','idempotency','audit','outbox','approvals','externalEffects','schemaCatalog','restoreSessions','recoveryObligations']})
 def check(self,id,action,path,expected,op='equals',obs=None,**kw):
  oid,names=ORACLES[self.oracle];names=obs or names
  if op=='count' and isinstance(expected,list):expected=len(expected)
  a={'id':id,'op':op,'source':{'actionId':action,'pointer':path},'expected':expected,'requirementRefs':['D23'] if self.c.id=='T23' else ['D23','D24','D26'],'evidenceRefs':[action+':actual-artifact'],'scope':self.scope,'oracleExplanation':id.replace('-',' '),'oracleRef':{'oracleId':oid,'observationNames':names}}
  for field in ['where','field']:
   if field in kw:a['source'][field]=kw.pop(field)
  a.update(kw)
  sourceAction=next((x for x in self.data['actions'] if x['id']==action),None)
  if sourceAction:
   a['scope']=sourceAction.get('observation',{}).get('scope',sourceAction.get('control',{}).get('parameters',{}).get('scope',sourceAction.get('request',{}).get('scope',self.scope)))
  self.data['assertions'].append(a);return a
 def fact(self,id,action,path,expected,op='equals',**kw):return self.check(id,action,H+path,expected,op,**kw)
 def unit(self,id,action,path,value,op='decimalEquals',field=None,baseline=None):
  source={'actionId':action,'pointer':path.rsplit('/',1)[0]+'/unit'}
  kw={'unit':'BOX','unitSource':source}
  if field:kw={'field':field,'unit':'BOX','unitSource':{'actionId':action,'pointer':path,'field':'unit'}}
  if baseline:kw.update({'baseline':{'actionId':baseline,'pointer':path},'baselineUnitSource':{'actionId':baseline,'pointer':path.rsplit('/',1)[0]+'/unit'}})
  return self.check(id,action,path,value,op,**kw)
 def preserve(self,before,after,observation=None):
  for name in ['segments','genealogy','works','goalVersions','assessments','obligations','definitions','evidence','idempotency','allocations']:
   self.check('보존-'+name,after,D+name,True,'sameAs',obs=observation,baseline={'actionId':before,'pointer':D+name})
  self.unit('현재-W-60',after,'/data/data/inventory/heldQuantity','60')
  self.check('식별된-실물-100',after,D+'segments','100','sumEquals',where={'active':True},field='quantity',unit='BOX',unitSource={'actionId':after,'pointer':D+'segments','where':{'active':True},'field':'unit'})
  self.check('실물-identity-중복0',after,D+'segments',True,'unique',where={'active':True},field='physicalScopeId')
  self.check('원-LOT-관계',after,D+'segments',[[alias('A60'),alias('LOT'),alias('W'),'60','BOX'],[alias('B40'),alias('LOT'),alias('TRANSIT'),'40','BOX']],'relationSet',where={'active':True},field=['id','lotId','placeId','quantity','unit'])
  self.check('진행-업무-WAITING',after,D+'works/0/status','WAITING')
  self.check('v1-정의',after,D+'works/0/definitionVersion','definition-v1')
  self.check('v1-evaluator',after,D+'assessments/0/evaluatorVersion','evaluator-v1')
  self.check('v1-도착-미충족',after,D+'assessments/0/result','UNSATISFIED')
  self.unit('v1-인정-도착60',after,D+'assessments/0/quantity','60')
  self.check('남은40-인간-owner',after,D+'obligations',[[alias('DUTY'),alias('procurement'),'OPEN','40','BOX','나머지 수령 확인','2026-10-08T09:00:00Z']],'relationSet',field=['id','ownerId','status','quantity','unit','nextAction','nextCheckAt'])
  self.check('원문-증거-hash',after,D+'evidence/0/sha256',INPUTS[4]['sha256'])
  self.check('원-수령-멱등결과',after,D+'idempotency/0/resultId',alias('R60'))

def make_fixture(env,variant):
 caps=['getInventory','getWork','getAssessment','getObligations','getDefinition','getEvidence','traceLot','searchOperationalIssues','getAccessContext','getGrant','reserveQuantity','splitQuantity','dispatchQuantity','retrySafeCommand','confirmReceipt','proposePurchase','approvePurchase','dispatchPurchaseOrder','recordExternalReconciliation']
 actorCaps={'reader':caps[:11],'warehouse':['reserveQuantity','splitQuantity','dispatchQuantity','confirmReceipt'],'procurement':['proposePurchase','dispatchPurchaseOrder','recordExternalReconciliation','getWork','getObligations'],'manager':['approvePurchase'],'outsider':['getInventory'],'operations':['retrySafeCommand','searchOperationalIssues']}
 actors={}
 for a,cs in actorCaps.items():actors[a]={'issuer':'synthetic-platform-issuer','subject':a,'audience':'isolated-ontology','organizationAlias':'OTHER_ORG' if a=='outsider' else 'ORG','roleCapabilities':cs,'grant':{'delegatorAlias':'supervisor','actions':cs,'scope':{'organizationAlias':'OTHER_ORG' if a=='outsider' else 'ORG','environmentId':env,'segmentAliases':['A60','B40'],'workAliases':['O1','S1']},'validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T23:59:59Z','revision':1}}
 aliases={a:{'type':'Human'} for a in [*actorCaps,'supervisor','qc','dataOwner','intake']}
 aliases.update({'ORG':{'type':'Organization'},'OTHER_ORG':{'type':'Organization'},'P':{'type':'TradeItem','unit':'BOX'},'LOT':{'type':'ManufacturingLot','manufacturer':'synthetic-import-supplier'},'W':{'type':'Place'},'TRANSIT':{'type':'Place'},'Q100':{'type':'QuantitySegment','active':False},'A60':{'type':'QuantitySegment','itemAlias':'P','lotAlias':'LOT','placeAlias':'W','quantity':'60','unit':'BOX','physicalScopeId':'physical-A'},'B40':{'type':'QuantitySegment','itemAlias':'P','lotAlias':'LOT','placeAlias':'TRANSIT','quantity':'40','unit':'BOX','physicalScopeId':'physical-B'},'O1':{'type':'Work'},'S1':{'type':'Work'},'SO100':{'type':'SalesOrder'},'SO-LINE':{'type':'SalesOrderLine'},'C':{'type':'Customer'},'CUSTOMER-PLACE':{'type':'Place'},'DUTY':{'type':'Obligation'},'R60':{'type':'Activity'},'HOLD-DOC':{'type':'DocumentVersion','sourceArtifact':INPUTS[4],'legalHold':True,'evidenceAlias':'receipt-evidence'},'DELETED-DOC':{'type':'DocumentVersion','sourceArtifact':INPUTS[4],'relationToInventory':'NONE','currentStatus':'DELETED'}})
 baseline={'variant':variant,'environmentId':env,'environmentProfile':'BTP' if 'btp-auth-binding-tls-wire' in env else 'CLIENT' if 'supported-client-separate' in env else 'LOCAL','schemaFamily':'NEW_ONTOLOGY_ONLY','schemaVersion':'ontology-v1','setupIsExecutionCoverage':False,'intakeConfiguration':{'ownerAlias':'intake','supervisorAlias':'supervisor','nextAction':'원천 사건과 책임 대조','nextCheckAt':'2026-10-08T09:00:00Z'},'segments':[{'alias':'Q100','active':False,'quantity':'100','unit':'BOX'},{'alias':'A60','active':True,'parentAlias':'Q100','quantity':'60','unit':'BOX','locationAlias':'W'},{'alias':'B40','active':True,'parentAlias':'Q100','quantity':'40','unit':'BOX','locationAlias':'TRANSIT'}],'genealogy':[{'parentAlias':'Q100','childAlias':'A60','quantity':'60','unit':'BOX'},{'parentAlias':'Q100','childAlias':'B40','quantity':'40','unit':'BOX'}],'work':{'alias':'O1','status':'WAITING','definitionVersion':'definition-v1','ownerAlias':'procurement','goal':{'quantity':'100','unit':'BOX','quantityMode':'CUMULATIVE_EVENT','endpoint':'ARRIVED','destinationAlias':'W','dueAt':'2026-10-09T09:00:00Z'},'assessment':{'quantity':'60','unit':'BOX','result':'UNSATISFIED','evaluatorVersion':'evaluator-v1','canonicalReceiptAlias':'R60'}},'idempotency':[{'ownerAlias':'warehouse','capabilityId':'confirmReceipt','key':'v1-receive60','payloadHash':'b'*64,'resultAlias':'R60','status':'COMMITTED'}],'retention':{'syntheticPolicy':'retention-v1','restoreBaseline':{'createActualPreDeletionSnapshot':True,'snapshotScope':'v1-data-before-deletion','preDeletionBlobAliases':['DELETED-DOC','HOLD-DOC'],'currentDeletionJournalIsSeparate':True},'legalHoldAliases':['HOLD-DOC'],'deletionJournal':[{'documentAlias':'DELETED-DOC','status':'DELETED','effectiveAt':'2026-10-07T08:00:00Z'}]},'allocation':{'scopeAlias':'A60','quantity':'20','unit':'BOX','orderLineAlias':'SO-LINE','responsibleWorkAlias':'S1','status':'EXECUTABLE'},'salesOrder':{'alias':'SO100','lineAlias':'SO-LINE','workAlias':'S1','customerAlias':'C','quantity':'100','unit':'BOX','destinationAlias':'CUSTOMER-PLACE','endpoint':'DELIVERED','ownerAlias':'warehouse'},'eligibilityBases':[{'id':'synthetic-'+condition+'-A60','scopeAlias':'A60','quantity':'60','unit':'BOX','condition':condition,'status':'ALLOWED','actions':['SELL','RESERVE'],'validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T23:59:59Z','evidenceSource':'SYNTHETIC-'+condition+'-basis-v1','decisionActorAlias':actor,'customerAlias':'C' if condition=='CUSTOMER' else None} for condition,actor in [('QC','qc'),('REGULATORY','dataOwner'),('DISPOSITION','manager'),('CUSTOMER','warehouse')]],'syntheticEligibilityInputs':{'B40':'in transit, not available at W'},'archivePolicy':'Only inventory/hash and isolated roundtrip; no old implementation content inspection','runtimeRequirements':{'bindingSource':'ACTUAL_VERIFIED_NON_SECRET_MANIFEST','requiredBindings':(['schemaSourceRevision','schemaSourceArtifacts'] if env.startswith('T23-schema-') else ['selectedStackManifest'] if 'fresh-install-manifest-cqn' in env else ['costApprovalRef'] if 'btp-auth-binding-tls-wire' in env else ['clientId','clientVersion'] if 'supported-client-separate' in env else ['syntheticMappingApproval'] if 'data-live' in env else []),'missingBindingResult':'NOT_RUN'},'operatingActivation':'blocked pending actual R3/R5/R7 and exact R1 manifest'}
 if variant=='fresh':
  baseline.update({'schemaVersion':'EMPTY','segments':[],'genealogy':[],'work':None,'idempotency':[],'allocation':None,'salesOrder':None,'eligibilityBases':[]})
  for name in ['Q100','A60','B40','O1','S1','DUTY','R60','SO100','SO-LINE','C','CUSTOMER-PLACE']:aliases.pop(name,None)
  for actor in actors.values():
   actor['grant']['scope'].pop('segmentAliases',None);actor['grant']['scope'].pop('workAliases',None)
   actor['grant']['scope'].update({'itemAliases':['P'],'placeAliases':['W','TRANSIT']})
 return {'schemaVersion':'1.0.0','fixtureId':env,'synthetic':True,'baseRefs':[],'clock':{'asOf':T,'knownAt':K,'timezone':'Asia/Seoul','precision':'SECOND','deadlineInclusive':True},'versions':{'definition':'definition-v1','evaluator':'evaluator-v1','policy':'SYNTHETIC-platform-v1','candidateCompiler':'7.1.1','schema':'ontology-v1'},'actors':actors,'aliases':aliases,'baseline':baseline,'evidence':[{'alias':'receipt-evidence','sha256':INPUTS[4]['sha256'],'sourceNamespace':'synthetic-platform-warehouse','externalEventId':'R60','sourceVersion':'1','occurredAt':'2026-10-06T09:00:00Z','recordedAt':'2026-10-06T09:00:01Z'}],'responsibilities':([] if variant=='fresh' else [{'scope':{'workAlias':'O1','remainingQuantity':'40','unit':'BOX'},'ownerAlias':'procurement','supervisorAlias':'supervisor','nextAction':'나머지 수령 확인','nextCheckAt':'2026-10-08T09:00:00Z'}])+[{'scope':{'environmentId':env},'ownerAlias':'dataOwner','supervisorAlias':'supervisor','nextAction':'복구 자료와 미해결 책임 대조','nextCheckAt':'2026-10-08T09:00:00Z'}]}

# T23: source preservation and cutover boundaries.
t=Case('T23','자료를 보존하고 새 schema와 cutover의 경계를 검증한다',['schema','scenarios','recovery','deployment'])
s=t.sub('archive-inventory-roundtrip','파일 inventory와 보존 ref를 isolated 환경에서 hash로 복원한다','archive')
s.host('inventory','archiveInventory',repositoryId='new-ontology-fork',baselineCommit=B2,inventoryId='archive-inventory',inputArtifacts=[INPUTS[0]],inventoryMode='TREE_METADATA_HASH_ONLY',preservationCommit='ae02ff63751714510db4d93a39498b8d51b3697c',requiredFields=['currentHash','classification','replacementPath','archiveRef','owner','validation'])
s.inspect('inventory')
s.host('restore-archive','archiveRestore',archiveId='preserved-baseline',restoreEnvironmentId=s.env+'-archive-restore',restoredCommit='ae02ff63751714510db4d93a39498b8d51b3697c',artifacts=ref('inventory','/data/hostObservation/generatedOutputs'),readMode='TREE_METADATA_HASH_ONLY')
s.inspect('restore-archive')
s.fact('전수-inventory-fields','inventory','files',['path','currentHash','classification','replacementPath','archiveRef','owner','validation'],'fieldsPresent',obs=['archive-restored'])
s.fact('현재-track경로-전수','inventory','preservedPaths',True,'sameAs',obs=['archive-restored'],baseline={'actionId':'restore-archive','pointer':H+'restoredPaths'})
s.fact('원-tree-hash-복원','restore-archive','restoredTreeHash',True,'sameAs',obs=['archive-restored'],baseline={'actionId':'inventory','pointer':H+'preservedTreeHash'})
s.fact('원-파일-hash-대조','restore-archive','fileHashes',True,'sameAs',obs=['archive-restored'],baseline={'actionId':'inventory','pointer':H+'preservedFileHashes'})
s.fact('역사-명령-active-참조0','inventory','activeAssertionHistoricalCommandRefs',[],'exactSet',obs=['archive-restored'])
s.fact('archive-내용-재사용0','restore-archive','implementationContentReads',0,obs=['archive-restored'])
s.fact('보존-commit','restore-archive','restoredCommit','ae02ff63751714510db4d93a39498b8d51b3697c',obs=['archive-restored'])

for variant in ['fresh','live']:
 s=t.sub('data-'+variant,'authoritative 자료 '+('부재를 확인해 독립 DB에서 시작한다' if variant=='fresh' else 'snapshot과 원본을 보존하고 mapping과 격리 수량·책임을 대조한다'),'archive',variant)
 s.host('data-inventory','dataInventory',inventoryId=variant+'-inventory',authoritativeSourceId='synthetic-'+variant+'-source',branch='NO_AUTHORITATIVE_LIVE_DATA' if variant=='fresh' else 'AUTHORITATIVE_LIVE_DATA',inputArtifacts=[INPUTS[1]],sourceClasses=['DB','BLOB','EXTERNAL_EFFECT','OPEN_DUTY'],synthetic=True)
 s.inspect('data-inventory')
 s.fact('네-source-분류-검증','data-inventory','sourceClasses',['DB','BLOB','EXTERNAL_EFFECT','OPEN_DUTY'],'exactSet',obs=['data-branch'])
 s.fact('운영-자료-가상-분리','data-inventory','inventoryEvidenceScope','SYNTHETIC_REHEARSAL',obs=['data-branch'])
 s.fact('원-제조-import-relabel0','data-inventory','manufactureAsImportRows',[],'count',obs=['manufacture-as-import-relabel'])
 if variant=='fresh':
  s.fact('authoritative-원천0','data-inventory','authoritativeSources',[],'exactSet',obs=['data-branch'])
  s.fact('fresh-분기','data-inventory','selectedBranch','NO_AUTHORITATIVE_LIVE_DATA',obs=['data-branch'])
  s.fact('임의-legacy-migration0','data-inventory','legacyMigrationJobs',[],'count',obs=['data-branch'])
 else:
  s.fact('live-분기','data-inventory','selectedBranch','AUTHORITATIVE_LIVE_DATA',obs=['data-branch'])
  s.fact('읽기전용-archive','data-inventory','originalAccessMode','READ_ONLY',obs=['data-branch'])
  s.fact('미대응-제조원본-읽기유지','data-inventory','sourceRecords',[['manufacturing-unmapped','UNMAPPED_ORIGINAL','7','BOX','dataOwner'],['import-mapped','IMPORT_RECEIPT','60','BOX','procurement'],['ambiguous-record','QUARANTINE','3','BOX','intake']],'relationSet',field=['id','classification','quantity','unit','owner'],obs=['data-branch'])
  s.fact('원본70-대조','data-inventory','sourceQuantity','70','decimalEquals',unit='BOX',unitSource={'actionId':'data-inventory','pointer':H+'unit'},obs=['data-branch'])
  s.fact('누락물량0','data-inventory','unaccountedQuantity','0','decimalEquals',unit='BOX',unitSource={'actionId':'data-inventory','pointer':H+'unit'},obs=['data-branch'])
  s.fact('의무-미손실','data-inventory','dutyReconciliation',[['legacy-duty-reference','dataOwner','원본 책임 대조','2026-10-08T09:00:00Z']],'relationSet',field=['sourceId','owner','nextAction','nextCheckAt'],obs=['data-branch'])
  s.host('confirm-mapping','cutoverStage',cutoverId=s.env+'-mapping',stage='RECONCILE',artifacts=ref('data-inventory','/data/hostObservation/generatedOutputs'),humanScopeConfirmationRef=ref('setup','/data/runtimeBindings/syntheticMappingApproval'),requiredActor='dataOwner');s.inspect('confirm-mapping')
  s.fact('권한자-전환검토','confirm-mapping','cutoverDecision',[['dataOwner','SCOPE_CONFIRMED','synthetic-live-mapping-v1']],'relationSet',field=['actor','decision','mappingVersion'],obs=['data-branch'])

for version in ['fresh','upgrade','unexpected-drift']:
 s=t.sub('schema-'+version,'Flyway 단독 소유와 고정 compiler의 '+version+' schema 차이를 검증한다','schema','fresh' if version=='fresh' else 'baseline')
 if version=='fresh':s.host('install','schemaInstall',schemaVersion='ontology-v1',migrationOwner='Flyway',databaseInitialState='EMPTY',inputArtifacts=[INPUTS[2]]);s.inspect('install')
 else:s.observe('before');s.host('upgrade','schemaUpgrade',fromVersion='ontology-v1',toVersion='ontology-v2',migrationOwner='Flyway',inputArtifacts=[INPUTS[2]]);s.inspect('upgrade')
 if version=='unexpected-drift':s.action('inject-drift','control',control={'type':'fault','operation':'alterSchemaMetadataForProbe','parameters':{'scope':s.scope,'constraint':'NONNEGATIVE_QUANTITY','operation':'REMOVE_IN_DISPOSABLE_DB'}})
 s.host('compile','compileSchema',compilerId='@sap/cds-compiler',compilerVersion='7.1.1',sourceRevision=ref('setup','/data/runtimeBindings/schemaSourceRevision'),sourceArtifacts=ref('setup','/data/runtimeBindings/schemaSourceArtifacts'),inputArtifacts=[INPUTS[2]])
 s.inspect('compile')
 s.host('compare','compilerSchemaProbe',compilerId='@sap/cds-compiler',compilerVersion='7.1.1',schemaVersion='ontology-v1' if version=='fresh' else 'ontology-v2',artifacts=ref('compile','/data/hostObservation/generatedOutputs'),allowlistRef=INPUTS[2])
 s.inspect('compare');s.observe('after')
 s.fact('유일-DDL-writer','compare','ddlWriters',['Flyway'],'exactSet',obs=['schema-owner'])
 s.fact('CAP-auto-deployer0','compare','autoDeployExecutions',[],'count',obs=['schema-owner'])
 s.fact('Boot-SQL-init-never','compare','sqlInitMode','never',obs=['schema-owner'])
 s.fact('단일-CQN-model','compare','persistenceModels',['CDS_CQN'],'exactSet',obs=['schema-owner'])
 s.fact('고정-compiler-version','compare','compilerVersion','7.1.1',obs=['schema-drift'])
 s.fact('custom-제약-allowlist-version','compare','allowlistVersion','synthetic-constraint-v1',obs=['schema-drift'])
 s.fact('허용-schema-delta','compare','allowedDeltaKinds',['ORGANIZATION_FK','NONNEGATIVE_QUANTITY','PHYSICAL_SCOPE_UNIQUENESS','ALLOCATION_CONSERVATION','OUTBOX_FK'],'exactSet',obs=['schema-drift'])
 s.fact('schema-drift-결과','compare','status','DRIFT_DETECTED' if version=='unexpected-drift' else 'MATCH',obs=['schema-drift'])
 s.fact('비허용-delta','compare','unapprovedDeltaKinds',['REMOVED_NONNEGATIVE_QUANTITY'] if version=='unexpected-drift' else [],'exactSet',obs=['schema-drift'])
 s.fact('FK-quantity-outbox-CQN-원검사','compare','probes',[['ORG_FK','REJECTED_CROSS_ORG'],['QUANTITY','ACCEPTED_NEGATIVE' if version=='unexpected-drift' else 'REJECTED_NEGATIVE'],['OUTBOX_FK','ENFORCED'],['CQN_READ_ACTION','ROUNDTRIP']],'relationSet',field=['id','observedOutcome'],obs=['schema-drift'])
 if version!='fresh':s.preserve('before','after',['schema-drift'])

# Explicit cutover stages and recovery before/after writes.
def stages(s,ending='OPEN_WRITES'):
 previous=None
 for stage in ['WRITE_FREEZE','FINAL_SNAPSHOT','RECONCILE','APPLY_VERSION','SMOKE_AUTH_RESUME','OPEN_WRITES']:
  params={'cutoverId':s.env+'-cutover','stage':stage,'inputArtifacts':[INPUTS[3]]}
  if previous:params['previousStageArtifacts']=ref(previous,'/data/hostObservation/generatedOutputs')
  s.host(stage.lower(),'cutoverStage',**params);s.inspect(stage.lower());previous=stage.lower()
  s.fact('stage-'+stage,previous,'stage',stage,obs=['cutover-order'])
  if stage==ending:break
 return previous
s=t.sub('cutover-open-order','freeze부터 smoke와 인가 재개까지 확인한 뒤 쓰기를 연다','cutover')
s.observe('before');stages(s);s.observe('after');s.preserve('before','after',['cutover-order'])
s.fact('쓰기개방-선행-stage','open_writes','completedStageOrder',['WRITE_FREEZE','FINAL_SNAPSHOT','RECONCILE','APPLY_VERSION','SMOKE_AUTH_RESUME','OPEN_WRITES'],obs=['cutover-order'])
s.fact('개방-전-수량차이0','reconcile','quantityDifference','0','decimalEquals',unit='BOX',unitSource={'actionId':'reconcile','pointer':H+'unit'},obs=['cutover-order'])
s.fact('개방-전-의무차이0','reconcile','missingDutyRoots',[],'count',obs=['cutover-order'])
s.fact('snapshot-호환-artifact-종류','final_snapshot','bundleKinds',['DB','BLOB','DEFINITION','CAPABILITY','EVALUATOR','SKILL','NON_SECRET_CONFIG','DEPLOYMENT_MANIFEST'],'exactSet',obs=['cutover-order'])
s.fact('인가-재개-smoke','smoke_auth_resume','authCases',[['SCOPED','ALLOWED'],['NO_AUTH','DENIED'],['OTHER_ORG','DENIED'],['REVOKED_GRANT','DENIED']],'relationSet',field=['case','outcome'],obs=['cutover-order'])

s=t.sub('failure-before-open','쓰기 개방 전 실패는 검증된 snapshot과 호환 artifact로 복구한다','cutover')
s.observe('before');stages(s,'APPLY_VERSION')
s.action('fail-smoke','control',control={'type':'fault','operation':'failCutoverSmoke','parameters':{'scope':s.scope,'cutoverId':s.env+'-cutover'}})
s.host('rollback','cutoverStage',cutoverId=s.env+'-cutover',stage='ROLLBACK_BEFORE_OPEN',artifacts=ref('final_snapshot','/data/hostObservation/generatedOutputs'));s.inspect('rollback');s.observe('after');s.preserve('before','after',['recovery-boundaries'])
s.fact('개방전-복구-mode','rollback','mode','VERIFIED_SNAPSHOT_COMPATIBLE_ARTIFACTS',obs=['recovery-boundaries'])
s.fact('개방전-write상태','rollback','writeIntake','FROZEN',obs=['recovery-boundaries'])
s.fact('실물계약-SQL취소0','rollback','physicalOrContractCancellationClaims',[],'count',obs=['SQLRollbackPretendedPhysicalContractCancel'])

for recovery in ['forward','approved-snapshot']:
 s=t.sub('failure-after-open-'+recovery,'새 쓰기와 외부효과 뒤 '+recovery+' 복구는 명령과 외부결과를 대조한다','cutover')
 s.observe('before');stages(s)
 s.invoke('proposal','proposePurchase',{'itemId':alias('P'),'quantity':{'value':'5','unit':'BOX'},'destinationId':alias('W'),'dueAt':'2026-10-09T09:00:00Z','price':{'value':'10','currency':'EUR'}},actor='procurement')
 s.invoke('approve','approvePurchase',actor='manager',proposalId=ref('proposal','/response/proposalId'),proposalHash=ref('proposal','/response/proposalHash'),expectedRevision=ref('proposal','/response/revision'))
 s.action('external-loss','control',control={'type':'externalResponder','operation':'commitThenLoseResponse','parameters':{'scope':s.scope,'externalOperationId':s.env+'-external-po','supportsIdempotency':False,'statusQuerySupported':True}})
 s.invoke('send-order','dispatchPurchaseOrder',actor='procurement',proposalId=ref('proposal','/response/proposalId'),proposalHash=ref('proposal','/response/proposalHash'),approvalId=ref('approve','/response/approvalId'),externalOperationId=s.env+'-external-po')
 s.observe('after-write');s.host('stop-reconcile','cutoverStage',cutoverId=s.env+'-cutover',stage='STOP_AND_RECONCILE_AFTER_OPEN',newCommandId=ref('send-order','/response/commandId'),externalOperationId=s.env+'-external-po');s.inspect('stop-reconcile')
 s.invoke('external-reconcile','recordExternalReconciliation',actor='procurement',externalOperationId=s.env+'-external-po',evidenceRefs=['synthetic-confirmed-external-po'],confirmedOutcome='SUCCESS')
 s.host('repair','cutoverStage',cutoverId=s.env+'-cutover',stage='FORWARD_REPAIR',recoveryMode='FORWARD_REPAIR' if recovery=='forward' else 'APPROVED_SNAPSHOT_WITH_COMMAND_EXTERNAL_RECONCILIATION',approvalActor='manager',artifacts=ref('final_snapshot','/data/hostObservation/generatedOutputs'),newCommandId=ref('send-order','/response/commandId'),externalOperationId=s.env+'-external-po');s.inspect('repair');s.observe('after')
 s.fact('접수중지','stop-reconcile','writeIntake','STOPPED',obs=['recovery-boundaries'])
 s.fact('outbox-freeze','stop-reconcile','outboxDispatch','FROZEN',obs=['recovery-boundaries'])
 s.fact('외부결과-대조','repair','externalEffects',[[s.env+'-external-po','CONFIRMED_SUCCESS',1]],'relationSet',field=['externalOperationId','status','issueCount'],obs=['recovery-boundaries'])
 s.fact('명시-replay-compensation','repair','commandDecisions',[[ref('send-order','/response/commandId'),'LOCAL_RESULT_LINK_ONLY','NO_EXTERNAL_REPLAY','NO_PHYSICAL_CANCEL']],'relationSet',field=['commandId','localAction','externalAction','physicalAction'],obs=['recovery-boundaries'])
 s.fact('실물계약-취소주장0','repair','physicalOrContractCancellationClaims',[],'count',obs=['SQLRollbackPretendedPhysicalContractCancel'])
 s.unit('복구후-물량불변','after','/data/data/inventory/heldQuantity','0','decimalDelta',baseline='before')
 s.check('외부-재발행0','after',D+'externalEffects',True,'sameAs',obs=['recovery-boundaries'],baseline={'actionId':'after-write','pointer':D+'externalEffects'})
 s.check('새-구매-기록보존','after',D+'purchaseOrders',True,'sameAs',obs=['recovery-boundaries'],baseline={'actionId':'after-write','pointer':D+'purchaseOrders'})
 s.check('대조-책임-유지','after',D+'recoveryObligations',['ownerId','nextAction','nextCheckAt'],'fieldsPresent',obs=['recovery-boundaries'])

# V8 platform execution gates.
v=Case('V8','빈 설치와 upgrade 복원 및 환경별 실제 인수를 대조한다',['schema','contracts','scenarios','recovery','mcp','deployment'])
s=v.sub('fresh-install-manifest-cqn','exact manifest로 빈 DB를 설치하고 CQN read/action과 outbox를 검증한다','fresh','fresh')
s.host('install','schemaInstall',schemaVersion='ontology-v1',migrationOwner='Flyway',databaseInitialState='EMPTY',inputArtifacts=[INPUTS[2],INPUTS[3]],selectedStackManifest=ref('setup','/data/runtimeBindings/selectedStackManifest'));s.inspect('install')
s.invoke('receipt','confirmReceipt',{'quantity':{'value':'60','unit':'BOX'},'itemId':alias('P'),'lotId':alias('LOT'),'placeId':alias('W'),'sourceEventId':'R60','evidenceRef':'receipt-evidence'},subjectRefs=[{'type':'TradeItem','id':alias('P')}])
s.query('read-cqn',snapshotRef=ref('receipt','/response/snapshotRevision'));s.query('read-mcp',route='mcp',snapshotRef=ref('receipt','/response/snapshotRevision'));s.observe('after',snapshot=ref('receipt','/response/snapshotRevision'))
s.fact('Flyway-only-DDL','install','ddlWriters',['Flyway'],'exactSet')
s.fact('처음-빈-DB','install','initialUserTables',[],'count')
s.fact('정확-manifest-component-set','install','versionManifest/components',['JAVA','MAVEN','DB','JDBC','FLYWAY','CDS_COMPILER','CDS_RUNTIME','CAP','SPRING_BOOT','MCP_SDK','MCP_PROTOCOL','BUILD_ARTIFACT'],'exactSet',field='component')
s.fact('버전-manifest-근거','install','versionManifest/components',['version','sha256','sourceArtifact','verificationCommand'],'fieldsPresent')
s.fact('stack-확정-runtime-증거','install','versionManifest/status','VERIFIED_RUNTIME')
s.fact('lock-manifest-실제-runtime-동일','install','runtimeVersions',True,'sameAs',field=['component','version'],baseline={'actionId':'install','pointer':H+'versionManifest/components','field':['component','version']})
s.fact('manifest-exact-후보버전','install','candidateVersionChecks',[['JAVA','21.0.5+11'],['MAVEN','3.9.16'],['DB','18.6'],['JDBC','42.7.13'],['FLYWAY','12.4.0'],['CDS_COMPILER','7.1.1'],['CDS_RUNTIME','10.1.1'],['CAP','5.1.1'],['SPRING_BOOT','4.1.1']],'relationSet',field=['component','version'])
s.check('CQN-수령-원장','after',D+'movements',[[ref('receipt','/response/activityId'),'RECEIPT','60','BOX']],'relationSet',field=['activityId','kind','quantity','unit'])
s.unit('신규-수령60','read-cqn','/response/data/heldQuantity','60')
s.check('MCP-CQN-동일-response','read-mcp','/response/data',True,'sameAs',baseline={'actionId':'read-cqn','pointer':'/response/data'})
s.check('원자적-감사-연결','after',D+'audit',[[ref('receipt','/response/commandId'),'confirmReceipt','APPLIED']],'relationSet',field=['commandId','capabilityId','outcome'])
s.check('원자적-outbox-연결','after',D+'outbox',[[ref('receipt','/response/commandId'),'RECEIPT_REEVALUATE','PENDING']],'relationSet',field=['commandId','type','status'])
s.check('멱등-COMMITTED','after',D+'idempotency/0/status','COMMITTED')

s=v.sub('fresh-constraints-auth','빈 설치의 실제 조직 FK 수량제약과 custom endpoint 인가를 검증한다','fresh','fresh')
s.host('install','schemaInstall',schemaVersion='ontology-v1',migrationOwner='Flyway',databaseInitialState='EMPTY',inputArtifacts=[INPUTS[2]]);s.inspect('install')
s.observe('before')
s.host('constraints','compilerSchemaProbe',compilerId='@sap/cds-compiler',compilerVersion='7.1.1',schemaVersion='ontology-v1',probeMode='DISPOSABLE_DB_CONSTRAINT_INSERTS',inputArtifacts=[INPUTS[2]])
s.inspect('constraints')
s.query('unauthorized-endpoint',route='mcp',actor='outsider',targetOrganizationId=alias('ORG'))
s.observe('after')
# A denied read is a query audit row, not a command audit (contracts/audit-observation-fields.json, plan §7.4).
s.data['actions'][-1]['observation']['sources'].append('queryAudit')
s.fact('조직-FK-23503','constraints','constraintAttempts',[['ORG_FK','23503'],['NEGATIVE_QUANTITY','23514'],['PRECISION_OVERFLOW','22003'],['OUTBOX_FK','23503']],'relationSet',field=['case','sqlState'])
s.check('custom-MCP-현재인가','unauthorized-endpoint','/response/error/code','FORBIDDEN')
for table in ['segments','movements','allocations','works','approvals','outbox']:s.check('무권한효과0-'+table,'after',D+table,True,'sameAs',baseline={'actionId':'before','pointer':D+table})
s.check('허용-denial-audit','after',D+'queryAudit',[['REJECTED','FORBIDDEN']],'relationSet',field=['outcome','errorCode'],where={'actorId':alias('outsider'),'capabilityId':'getInventory'})

s=v.sub('transaction-rollback-all-effects','감사 outbox 멱등 결과 실패는 하나의 업무 transaction 전체를 rollback한다','fresh')
s.observe('before')
s.action('fail-persistence','control',control={'type':'fault','operation':'failTransactionWrite','parameters':{'scope':s.scope,'afterDomainMutation':True,'failurePoints':['AUDIT_INSERT','OUTBOX_INSERT','IDEMPOTENCY_COMMIT'],'applyToSeparateInvocations':True}})
for point in ['audit','outbox','idempotency']:s.invoke('split-'+point,'splitQuantity',{'quantity':{'value':'20','unit':'BOX'},'faultPoint':point.upper()})
s.observe('after')
for table in ['segments','genealogy','movements','allocations','obligations','audit','outbox','idempotency']:s.check('rollback-금지효과0-'+table,'after',D+table,True,'sameAs',baseline={'actionId':'before','pointer':D+table})
for point in ['audit','outbox','idempotency']:s.check('rollback-실패응답-'+point,'split-'+point,'/response/outcome','REJECTED')
s.unit('rollback-수량변화0','after','/data/data/inventory/heldQuantity','0','decimalDelta',baseline='before')

s=v.sub('real-lock-two-transactions','실제 두 transaction의 scope 잠금은 예약40과 신규30의 초과소비를 막는다','fresh')
s.observe('before')
for id,qty,tx in [('reserve40','40','tx-reserve40'),('reserve30','30','tx-reserve30')]:
 call={'id':id+'-call','kind':'invoke','actorRef':'warehouse','route':'api','capabilityId':'reserveQuantity','request':{'intentKind':'COMMAND','definitionVersion':'definition-v1','capabilityId':'reserveQuantity','subjectRefs':[{'type':'QuantitySegment','id':alias('A60')}],'slots':{'quantity':{'value':qty,'unit':'BOX'},'orderLineId':alias('SO-LINE')},'expectedRevision':1,'commandIdempotencyKey':s.env+'-'+id,'testTransactionId':tx,'testParticipantId':id,'testBarrierId':'scope-lock','testBarrierPoint':'AFTER_SCOPE_LOCK' if id=='reserve40' else 'BEFORE_SCOPE_LOCK'},'evidenceRefs':[id+':actual-transaction']}
 s.action(id,'start',call=call)
 s.action(id+'-reached','control',control={'type':'barrier','operation':'waitReached','parameters':{'barrierId':'scope-lock','participantId':id,'transactionId':tx,'point':'AFTER_SCOPE_LOCK' if id=='reserve40' else 'BEFORE_SCOPE_LOCK','state':'REACHED','scope':s.scope}})
# The waiter must actually attempt the lock while the first DB transaction holds it.
s.action('resume30','control',control={'type':'barrier','operation':'resume','parameters':{'barrierId':'scope-lock','participantId':'reserve30','transactionId':'tx-reserve30','point':'BEFORE_SCOPE_LOCK','state':'RESUMED','scope':s.scope}})
holder=ref('reserve40-reached','/data/database/transactionId')
waiter=ref('reserve30-reached','/data/database/transactionId')
s.action('waiter-before-release','observe',observation={'scope':{**s.scope,'segmentId':alias('A60'),'lockProbe':{'holderTransactionId':holder,'waiterTransactionId':waiter,'databaseCatalogs':['pg_catalog.pg_locks','pg_catalog.pg_stat_activity','pg_catalog.pg_blocking_pids'],'readOnly':True,'waitUntil':'DATABASE_LOCK_WAIT','timeoutSeconds':30}},'snapshotRef':'CURRENT_LOCK_WAIT','asOf':T,'knownAt':K,'sources':['lockWaits','databaseTransactions']})
s.action('resume40','control',control={'type':'barrier','operation':'resume','parameters':{'barrierId':'scope-lock','participantId':'reserve40','transactionId':'tx-reserve40','point':'AFTER_SCOPE_LOCK','state':'RESUMED','scope':s.scope}})
s.action('terminal40','await',awaitActionId='reserve40',timeoutSeconds=30)
s.action('terminal30','await',awaitActionId='reserve30',timeoutSeconds=30)
s.observe('after',transactionIds=[holder,waiter])
next(a for a in s.data['actions'] if a['id']=='after')['observation']['sources']+=['lockEvidence','lockRevalidations','newAllocations']
s.check('동시-실제-DB-lock-WAIT','waiter-before-release',D+'lockWaits',[[alias('A60'),waiter,holder,holder,'transactionid','ShareLock',False,'ExclusiveLock',True,'Lock','pg_catalog']],'relationSet',field=['scopeId','transactionId','blockingTransactionId','lockResourceId','lockType','lockMode','granted','holderLockMode','holderGranted','waitEventType','source'])
s.check('WAIT-시점-holder와-waiter-거래-open','waiter-before-release',D+'databaseTransactions',[[holder,'OPEN','HOLDING_SCOPE_LOCK'],[waiter,'OPEN','WAITING_SCOPE_LOCK']],'relationSet',field=['transactionId','status','lockState'])
s.check('서로다른-실제-DB-transaction','reserve30-reached','/data/database/transactionId',holder,'notEquals')
s.check('예약40-commit','terminal40','/response/outcome','APPLIED')
s.check('예약30-현재revision-거부','terminal30','/response/error/code','STALE_REVISION')
s.check('waiter-잠금후-현재revision-재검증','after',D+'lockRevalidations',[[alias('A60'),waiter,2,1,'AFTER_SCOPE_LOCK','STALE_REVISION']],'relationSet',field=['scopeId','transactionId','lockedRevision','requestedRevision','point','result'])
s.unit('실물-잠금전후-불변','after','/data/data/inventory/heldQuantity','0','decimalDelta',baseline='before')
s.check('실제-독립-transaction','after',D+'lockEvidence',[holder,waiter],'exactSet',field='transactionId')
s.check('잠금-fence-원범위','after',D+'lockEvidence',[[alias('A60'),holder,'EXCLUSIVE'],[alias('A60'),waiter,'EXCLUSIVE']],'relationSet',field=['scopeId','transactionId','lockMode'])
s.check('첫-reserve-40-효과','after',D+'newAllocations',[['40','BOX','EXECUTABLE']],'relationSet',field=['quantity','unit','status'])
s.unit('기존20+새40-실행배분60','after','/data/data/inventory/reservedQuantity','60')
s.check('기존-부족40-책임보존','after',D+'obligations',[[alias('DUTY'),alias('procurement'),'OPEN','40','BOX','나머지 수령 확인','2026-10-08T09:00:00Z']],'relationSet',field=['id','ownerId','status','quantity','unit','nextAction','nextCheckAt'])

s=v.sub('ontology-v1-v2-preserves','새 ontology v1에서 v2로 upgrade해 진행 업무 물량 의무와 판정 의미를 보존한다','upgrade')
s.observe('before');s.query('assessment-before','getAssessment',workId=alias('O1'))
s.host('upgrade','schemaUpgrade',fromVersion='ontology-v1',toVersion='ontology-v2',migrationOwner='Flyway',inputArtifacts=[INPUTS[2]]);s.inspect('upgrade')
s.query('assessment-after','getAssessment',workId=alias('O1'));s.query('inventory-after');s.query('duties-after','getObligations',workId=alias('O1'));s.query('mcp-after','getAssessment',route='mcp',workId=alias('O1'));s.observe('after');s.preserve('before','after',['upgrade-preserves'])
s.check('API-v1-판정-동일','assessment-after','/response/data',True,'sameAs',obs=['upgrade-preserves'],baseline={'actionId':'assessment-before','pointer':'/response/data'})
s.check('MCP-v1-판정-동일','mcp-after','/response/data',True,'sameAs',obs=['upgrade-preserves'],baseline={'actionId':'assessment-after','pointer':'/response/data'})
s.fact('새-schema-family','upgrade','schemaFamily','NEW_ONTOLOGY',obs=['legacy-migration-substitutes-ontology-upgrade'])
s.check('MCP-upgrade-전-판정-동일','mcp-after','/response/data',True,'sameAs',obs=['legacy-migration-substitutes-ontology-upgrade'],baseline={'actionId':'assessment-before','pointer':'/response/data'},oracleExplanation='새 ontology v1→v2 upgrade 뒤 MCP로 읽은 O1의 v1 도착 판정(인정60 BOX·UNSATISFIED)은 upgrade 전 API 값과 같다. 옛 구현 migration이 자료를 다시 쓴 결과로 대체되지 않는다')
s.fact('옛-migration-source0','upgrade','legacySchemaSources',[],'count',obs=['legacy-migration-substitutes-ontology-upgrade'])
s.fact('upgrade-재인수-probes','upgrade','revalidated',[['CONSTRAINT','PASS'],['LOCK','PASS'],['OUTBOX','PASS'],['AUTH','PASS'],['MCP_WIRE','PASS']],'relationSet',field=['gate','result'],obs=['upgrade-preserves'])

for missing in ['none','blob','evaluator']:
 s=v.sub('restore-'+missing,'DB blob 정의 evaluator와 인가를 복원한다' if missing=='none' else '필수 '+missing+' 누락 복원을 완료로 표시하지 않는다','restore')
 s.observe('before');s.query('assessment-before','getAssessment',workId=alias('O1'))
 s.host('backup','backup',backupId=s.env+'-backup',snapshotId=ref('setup','/data/retentionBaseline/snapshotId'),inputArtifacts=[INPUTS[4],INPUTS[3]],includeKinds=['DB','BLOB','DEFINITION','CAPABILITY','EVALUATOR','SKILL','NON_SECRET_CONFIG','DEPLOYMENT_MANIFEST']);s.inspect('backup')
 if missing!='none':s.action('remove-'+missing,'control',control={'type':'fault','operation':'withholdBackupArtifact','parameters':{'scope':s.scope,'artifactKind':missing.upper(),'artifacts':ref('backup','/data/hostObservation/generatedOutputs'),'retainOriginalBackup':True,'copyToRestoreSandbox':True}})
 s.host('restore','restore',backupId=s.env+'-backup',restoreEnvironmentId=s.env+'-restored',snapshotId=ref('setup','/data/retentionBaseline/snapshotId'),artifacts=ref('backup','/data/hostObservation/generatedOutputs') if missing=='none' else ref('remove-'+missing,'/data/restoreInputArtifacts'),withheldArtifactKind=missing.upper(),reapplyDeletionJournal=True);s.inspect('restore')
 s.fact('bundle-전수-종류','backup','bundleKinds',['DB','BLOB','DEFINITION','CAPABILITY','EVALUATOR','SKILL','NON_SECRET_CONFIG','DEPLOYMENT_MANIFEST'],'exactSet',obs=['restore-artifacts'])
 s.fact('삭제전-백업-원본-positive','backup','preDeletionBlobs',[['DELETED-DOC',INPUTS[4]['sha256'],'ACTIVE'],['HOLD-DOC',INPUTS[4]['sha256'],'LEGAL_HOLD']],'relationSet',field=['sourceAlias','sha256','state'],obs=['restore-artifacts'])
 s.fact('백업-원문-hash','backup','evidenceHashes',[INPUTS[4]['sha256']],'exactSet',obs=['restore-artifacts'])
 if missing=='none':
  s.observe('after',environmentId=s.env+'-restored');s.query('assessment-after','getAssessment',workId=alias('O1'),environmentId=s.env+'-restored');s.query('evidence-after','getEvidence',evidenceId=alias('HOLD-DOC'),environmentId=s.env+'-restored');s.preserve('before','after',['restore-artifacts'])
  s.check('복원-API-evidence-hash','evidence-after','/response/data/sha256',INPUTS[4]['sha256'],obs=['restore-artifacts'])
  s.fact('완전복원-결과','restore','status','COMPLETE',obs=['restore-artifacts'])
  s.fact('artifact-hash-대조','restore','restoredArtifactHashes',True,'sameAs',obs=['restore-artifacts'],baseline={'actionId':'backup','pointer':H+'artifactHashes'})
  s.fact('복원-현재인가','restore','authCases',[['SCOPED','ALLOWED'],['NO_AUTH','DENIED'],['OTHER_ORG','DENIED'],['REVOKED_GRANT','DENIED']],'relationSet',field=['case','outcome'],obs=['restore-artifacts'])
  s.fact('삭제-이력재적용','restore','deletionJournalApplied',[['DELETED-DOC','DELETED','2026-10-07T08:00:00Z']],'relationSet',field=['sourceAlias','status','effectiveAt'],obs=['restore-artifacts'])
  s.fact('deleted-원문-복활0','restore','readableDeletedBlobs',[],'count',obs=['restore-artifacts'])
  s.fact('legal-hold-보존','restore','legalHoldBlobs',['HOLD-DOC'],'exactSet',obs=['restore-artifacts'])
  s.check('복원-API-v1-의미','assessment-after','/response/data',True,'sameAs',obs=['restore-artifacts'],baseline={'actionId':'assessment-before','pointer':'/response/data'})
  s.query('mcp-after-restore','getAssessment',route='mcp',workId=alias('O1'),environmentId=s.env+'-restored')
  s.check('복원-MCP-v1-의미','mcp-after-restore','/response/data',True,'sameAs',obs=['restore-artifacts'],baseline={'actionId':'assessment-before','pointer':'/response/data'},oracleExplanation='완전 복원 환경의 MCP wire로 읽은 O1의 v1 판정은 backup 전 API 값과 같다. DB만 돌아오고 MCP 진입점의 정의·인가·evaluator가 다르면 실패한다')
 else:
  observation='missing-'+missing+'-restore-complete'
  s.observe('partial-db',environmentId=s.env+'-restored')
  s.query('partial-access','getAccessContext',environmentId=s.env+'-restored')
  s.check('partial-API-write차단','partial-access','/response/data/writeActivation','BLOCKED',obs=[observation])
  s.query('partial-mcp-access','getAccessContext',route='mcp',environmentId=s.env+'-restored')
  s.check('partial-MCP-write차단','partial-mcp-access','/response/data/writeActivation','BLOCKED',obs=[observation],oracleExplanation=f'필수 {missing} artifact가 빠진 불완전 복원에서는 MCP 진입점도 운영 쓰기 활성화를 BLOCKED로 보인다. API만 막고 MCP로 쓰기를 열면 실패한다')
  s.check('partial-DB-incomplete','partial-db',D+'restoreSessions/0/status','INCOMPLETE',obs=[observation])
  s.check('partial-DB-completion0','partial-db',D+'restoreCompletionRecords',0,'count',obs=[observation])
  s.check('partial-DB-recovery-owner','partial-db',D+'recoveryObligations',['ownerId','nextAction','nextCheckAt'],'fieldsPresent',obs=[observation])
  s.fact('불완전복원-결과','restore','status','INCOMPLETE',obs=[observation])
  s.fact('누락-kind','restore','missingArtifactKinds',[missing.upper()],'exactSet',obs=[observation])
  s.fact('허위-완료-record0','restore','completionRecords',[],'count',obs=[observation])
  s.fact('운영쓰기-차단','restore','writeActivation','BLOCKED',obs=[observation])
  s.fact('복구-인간책임','restore','recoveryDuty',[['dataOwner','필수 복구 artifact 확보','2026-10-08T09:00:00Z']],'relationSet',field=['owner','nextAction','nextCheckAt'],obs=[observation])

s=v.sub('btp-auth-binding-tls-wire','실제 BTP의 entitlement runtime binding TLS 현재 인가와 MCP wire를 인수한다','environment','fresh')
s.host('btp','deploymentProbe',deploymentId=s.env+'-deployment',profile='BTP',inputArtifacts=[INPUTS[3]],requireDecision='R7_CONFIRMED',costAuthorizationRef=ref('setup','/data/runtimeBindings/costApprovalRef'),requestedAuthCases=['NO_AUTH','WRONG_ISSUER','WRONG_AUDIENCE','OTHER_ORG','REVOKED_GRANT','SCOPED'],protocolVersion='2026-07-28');s.inspect('btp')
s.fact('실제-BTP-profile','btp','profile','BTP',obs=['btp-acceptance'])
s.fact('R7-region-entitlement-cost','btp','decisionR7',['region','entitlement','owner','approvedCostCap','approvalRef','deploymentLogHash'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('실제-runtime-binding','btp','runtimeBindings',['jdkVersion','buildpackVersion','datasourceServiceId','bindingId','bindingType','tlsProtocol','peerCertificateFingerprint','host'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('현재-token-인가','btp','authCases',[['NO_AUTH','DENIED'],['WRONG_ISSUER','DENIED'],['WRONG_AUDIENCE','DENIED'],['OTHER_ORG','DENIED'],['REVOKED_GRANT','DENIED'],['SCOPED','ALLOWED']],'relationSet',field=['case','outcome'],obs=['btp-acceptance'])
s.fact('TLS-검증결과','btp','tlsVerification','VERIFIED_PEER_AND_HOST',obs=['btp-acceptance'])
s.fact('실제-최신-MCP-wire','btp','wireMessages',[['server/discover','2026-07-28','BTP'],['tools/call','2026-07-28','BTP']],'relationSet',field=['method','protocolVersion','profile'],obs=['btp-acceptance'])
s.fact('BTP-wire-header-version','btp','wireRequestHeaders/MCP-Protocol-Version','2026-07-28',obs=['btp-acceptance'])
s.fact('BTP-wire-body-version','btp','wireRequestBody/params/_meta/io.modelcontextprotocol~1protocolVersion','2026-07-28',obs=['btp-acceptance'])
s.fact('BTP-raw-transcript-link','btp','wireTranscripts',['path','sha256','sizeBytes','scope','jsonRpcId','requestBody','responseBody'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('BTP-wire-효과refs','btp','toolResults',['rpcId','actorId','organizationId','grantRevision','capabilityId','outcome','dbSnapshotId','redactedTranscriptHash'],'fieldsPresent',obs=['btp-acceptance'])

s=v.sub('supported-client-separate','대상 실제 client의 발견 loading과 tool 왕복을 local BTP와 분리한다','environment','fresh')
s.host('client','clientProbe',clientId=ref('setup','/data/runtimeBindings/clientId'),clientVersion=ref('setup','/data/runtimeBindings/clientVersion'),profile='CLIENT',inputArtifacts=[INPUTS[3]],protocolVersion='2026-07-28',requireActualInstalledClient=True,permitModelExecution=False,skillLoadMode='HOST_TELEMETRY_OR_NON_MODEL_TEST');s.inspect('client')
s.fact('실제-client-profile','client','profile','CLIENT',obs=['btp-acceptance'])
s.fact('실제-client-version-source','client','clientInstall',['clientId','clientVersion','installedBinaryHash','supportedProtocolVersion','observedCommand'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('실제-client-wire','client','wireMessages',[['server/discover','2026-07-28','CLIENT'],['tools/call','2026-07-28','CLIENT']],'relationSet',field=['method','protocolVersion','profile'],obs=['btp-acceptance'])
s.fact('client-wire-header-version','client','wireRequestHeaders/MCP-Protocol-Version','2026-07-28',obs=['btp-acceptance'])
s.fact('client-wire-body-version','client','wireRequestBody/params/_meta/io.modelcontextprotocol~1protocolVersion','2026-07-28',obs=['btp-acceptance'])
s.fact('client-raw-transcript-link','client','wireTranscripts',['path','sha256','sizeBytes','scope','jsonRpcId','requestBody','responseBody'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('client-skill-loading-artifact','client','loadingObservations',['packageHash','discoveryArtifactHash','bodyLoadArtifactHash','referenceLoadArtifactHash','toolTranscriptHash'],'fieldsPresent',obs=['btp-acceptance'])
s.fact('profile-결과-각각','client','acceptanceGates',[['LOCAL','NOT_RUN'],['BTP','NOT_RUN'],['CLIENT','PASS']],'relationSet',field=['profile','result'],obs=['result-scope'])
s.fact('client-성공의-BTP-참조0','client','gatePromotionRefs',[],'count',obs=['result-scope'])

s=v.sub('unavailable-environment-gates','필수 BTP client 부재와 미검증 후보는 NOT_RUN으로 남긴다','environment')
s.query('local-witness')
s.unit('로컬-관찰-보유60','local-witness','/response/data/heldQuantity','60')
s.query('local-mcp-witness',route='mcp',snapshotRef=ref('local-witness','/response/snapshotRevision'))
s.check('로컬-MCP-같은-snapshot','local-mcp-witness','/response/data',True,'sameAs',obs=['result-scope'],baseline={'actionId':'local-witness','pointer':'/response/data'},oracleExplanation='BTP·CLIENT 연결이 없어도 LOCAL MCP wire는 같은 snapshot에서 API와 같은 W 보유60 BOX를 읽는다. 이 LOCAL 결과는 BTP·CLIENT 인수로 승격되지 않고 그 둘은 NOT_RUN으로 남는다')
s.action('disable-external-bindings','control',control={'type':'fault','operation':'disableEnvironmentBindings','parameters':{'scope':s.scope,'profiles':['BTP','CLIENT'],'restoreAfterSubcase':True}})
s.host('gate-inventory','dataInventory',inventoryId='environment-gates',authoritativeSourceId='R3-operating-inventory',inputArtifacts=[INPUTS[3]],inventoryMode='ENVIRONMENT_AND_DECISION_GATE_INVENTORY',actualInventoryOnly=True,localWitnessSnapshot=ref('local-witness','/response/snapshotRevision'),resultScope='SUBCASE_ONLY');s.inspect('gate-inventory')
s.fact('환경별-NOT_RUN','gate-inventory','acceptanceGates',[['LOCAL','PASS'],['BTP','NOT_RUN'],['CLIENT','NOT_RUN']],'relationSet',field=['profile','result'],obs=['result-scope'])
s.fact('필수-gate-생략0','gate-inventory','waivers',[],'count',obs=['result-scope'])
s.fact('candidate-최종확정0','gate-inventory','unverifiedFinalStackClaims',[],'count',obs=['result-scope'])
s.fact('대안-oracle-동일','gate-inventory','candidateOracleBindings',[[candidate,oracle] for candidate in ['JAVA_CAP_POSTGRESQL','JAVA_BOOT_JOOQ_POSTGRESQL'] for oracle in ['V8.fresh-install','V8.ontology-upgrade-preservation','V8.db-blob-definition-restore','V8.btp-client-separate-gates']],'relationSet',field=['candidate','oracleId'],obs=['result-scope'])
s.fact('미정-계정-비용-실행0','gate-inventory','unauthorizedPaidRuns',[],'count',obs=['result-scope'])
# Revalidate real transaction/lock/auth paths after the NEW ontology upgrade.
s=v.sub('upgrade-revalidate-transactions','upgrade 뒤 제약 인가 rollback 잠금과 transactional outbox를 다시 인수한다','upgrade')
s.host('upgrade','schemaUpgrade',fromVersion='ontology-v1',toVersion='ontology-v2',migrationOwner='Flyway',inputArtifacts=[INPUTS[2]]);s.inspect('upgrade')
s.host('constraints','compilerSchemaProbe',compilerId='@sap/cds-compiler',compilerVersion='7.1.1',schemaVersion='ontology-v2',probeMode='DISPOSABLE_DB_CONSTRAINT_INSERTS',inputArtifacts=[INPUTS[2]]);s.inspect('constraints')
s.fact('upgrade-실제-constraint-SQLSTATE','constraints','constraintAttempts',[['ORG_FK','23503'],['NEGATIVE_QUANTITY','23514'],['PRECISION_OVERFLOW','22003'],['OUTBOX_FK','23503']],'relationSet',field=['case','sqlState'],obs=['upgrade-preserves'])
s.query('unauthorized','getInventory',actor='outsider',route='mcp',targetOrganizationId=alias('ORG'))
s.check('upgrade-실제-MCP-인가','unauthorized','/response/error/code','FORBIDDEN',obs=['upgrade-preserves'])
for sourceId,prefix in [('transaction-rollback-all-effects','rollback-'),('real-lock-two-transactions','lock-')]:
 source=next(x for x in v.subs if x.id==sourceId)
 ids={a['id']:prefix+a['id'] for a in source.data['actions'] if a['id']!='setup'}
 def rebind(n):
  if isinstance(n,list):return [rebind(x) for x in n]
  if isinstance(n,dict):
   out={k:rebind(val) for k,val in n.items()}
   for key in ['id','actionId','awaitActionId']:
    if isinstance(n.get(key),str) and n[key] in ids:out[key]=ids[n[key]]
   if 'oracleRef' in out:out['oracleRef']={'oracleId':ORACLES['upgrade'][0],'observationNames':['upgrade-preserves']}
   return out
  if isinstance(n,str):
   value=n.replace(source.env,s.env)
   for old,new in ids.items():
    if value.startswith(old+':'):return new+value[len(old):]
   return value
  return n
 for a in source.data['actions'][1:]:s.data['actions'].append(rebind(a))
 for a in source.data['assertions']:
  bound=rebind(a);bound['id']=prefix+a['id'];s.data['assertions'].append(bound)
s.observe('upgrade-final')
s.check('upgrade-outbox-command-일치','upgrade-final',D+'newOutbox',[[ref('lock-terminal40','/response/commandId'),alias('ORG'),'ALLOCATION_CHANGED','PENDING']],'relationSet',field=['commandId','organizationId','type','status'],obs=['upgrade-preserves'])
for case in [t,v]:case.save()
write('verification/platform-tests/subcase-index.json',{'baseline':B2,'kind':'PREPARATION_ONLY','cases':[{'caseId':c.id,'path':f'verification/cases/{c.id}/case.json','feature':f'verification/cases/{c.id}/scenario.feature','subcaseIds':[s.id for s in c.subs]} for c in [t,v]],'assertions':sum(len(s.data['assertions']) for c in [t,v] for s in c.subs),'namedObservations':16,'productResult':'NOT_RUN'})
print('Generated',sum(len(c.subs) for c in [t,v]),'subcases;',sum(len(s.data['assertions']) for c in [t,v] for s in c.subs),'assertions')
