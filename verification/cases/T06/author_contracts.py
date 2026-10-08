"""Deterministic contract authoring only; never a product or test driver."""
import copy
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
NOW = '2026-10-07T00:00:00Z'
LATER = '2026-10-07T01:00:00Z'
EVENT = '2026-10-05T00:00:00Z'
NEXT = '2026-10-07T02:00:00Z'

def alias(s): return {'$alias': s}
def result(a, p): return {'$result': {'actionId': a, 'pointer': p}}
def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')

READS = ['getObject','getEvidence','getAssessment','getInventory','getObligations','getInbox','getReconciliation','getWork','searchObjects','searchOperationalIssues','getPolicy']
ROLES = {
    'recorder': READS + ['recordActivity','attachEvidence','correctEvidence','confirmReceipt','createWork'],
    'reconciler': READS + ['matchSourceIdentity','linkCanonicalOccurrence','resolveEvidenceConflict','recordExternalReconciliation'],
    'operations': READS + ['retrySafeCommand','dispatchPurchaseOrder','cancelPurchase'],
    'warehouse': READS + ['reserveQuantity','dispatchQuantity','placeHold','releaseHold'],
    'readAgent': READS + ['dispatchQuantity','placeHold','assignCapability'],
    'admin': READS + ['assignCapability'],
    'worker': READS + ['dispatchQuantity','retrySafeCommand'],
    'config': READS + ['createPolicyDraft','approvePolicy','activatePolicy'],
    'intake': READS,
    'supervisor': READS,
}
TABLES = ['events','evidence_revisions','claims','inbox','identity_matches','canonical_links','movements','segments','allocations','assessments','obligations','assignments','audit','outbox','command_records','restrictions','source_profiles','policies','documents','tombstones','deletion_log','domain_records']

class Case:
    def __init__(self, cid, title):
        self.cid,self.title,self.subs=cid,title,[]
    def sub(self, sid, title, oracle, baseline=None):
        return Sub(self, sid,title,oracle,baseline or {})
    def finish(self):
        folder=ROOT/'verification/cases'/self.cid
        data={'schemaVersion':'1.0.0','caseId':self.cid,'title':self.title,'requirementRefs':['D'+self.cid[1:]],'profiles':['contracts','scenarios','recovery'],'subcases':[s.data for s in self.subs]}
        write(folder/'case.json',data)
        lines=['# language: ko',f'@{self.cid} @D{self.cid[1:]} @sit @uat @contract-red',f'기능: {self.title}']
        for sub in self.subs:
            lines += ['',f'  시나리오: {sub.title}',f'    먼저 사례 파일 "verification/cases/{self.cid}/case.json"의 "{sub.sid}"를 준비한다']
            for a in sub.data['actions']:
                lines.append(f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다')
            for a in sub.data['assertions']:
                lines.append(f'    그러면 "{a["id"]}" assertion으로 "{a["oracleExplanation"]}"를 확인한다')
        (folder/'scenario.feature').write_text('\n'.join(lines)+'\n')

class Sub:
    def __init__(self, case,sid,title,oracle,baseline):
        self.case,self.sid,self.title,self.oracle=case,sid,title,oracle
        self.scope={'organizationId':alias('ORG-A'),'itemId':alias('P'),'caseId':case.cid,'subcaseId':sid}
        self.observer_scope=copy.deepcopy(self.scope)
        self.data={'id':sid,'title':title,'fixtureRef':f'verification/cases/{case.cid}/fixtures/{sid}.json','requiredAdapters':['fixture','api','db'],'actions':[],'assertions':[],'oracleExplanation':'원천·시간·인가 범위와 전후 실제 효과를 대조한다. fixture의 과거 사실은 이번 행동의 실행 증거가 아니다.'}
        self.fixture=self.make_fixture(baseline)
        case.subs.append(self)
        self.action('setup','installFixture')
    def make_fixture(self, baseline):
        actors={}
        for who,caps in ROLES.items():
            grant=READS if who in ['readAgent','worker'] else caps
            actors[who]={'issuer':'synthetic-test-issuer','subject':who,'audience':'isolated-ontology','organizationAlias':'ORG-A','roleCapabilities':caps,'grant':{'delegatorAlias':'supervisor','actions':grant,'scope':{'organizationAlias':'ORG-A','itemAliases':['P'],'segmentAliases':['Q100','A60','B40'],'workAliases':['O1','S1'],'sourceNamespaces':['carrier','warehouse','purchase-peer'],'managementOrganizationAliases':['ORG-A']},'validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T00:00:00Z','revision':1}}
        aliases={'ORG-A':{'type':'Organization'},'ORG-B':{'type':'Organization'},'P':{'type':'TradeItem','unit':'BOX'},'W':{'type':'Place','organizationAlias':'ORG-A'},'C':{'type':'Customer','organizationAlias':'ORG-A'},'L':{'type':'ManufacturingLot','itemAlias':'P','manufacturer':'synthetic-manufacturer'},'O1':{'type':'Work','ownerAlias':'intake','revision':1},'S1':{'type':'Work','ownerAlias':'warehouse','revision':1},'Q100':{'type':'QuantitySegment','unit':'BOX','itemAlias':'P','lotAlias':'L','locationAlias':'W','physicalScope':'Q100-physical','revision':1},'A60':{'type':'QuantitySegment','unit':'BOX','itemAlias':'P','lotAlias':'L','locationAlias':'W','physicalScope':'A60-physical','revision':1},'B40':{'type':'QuantitySegment','unit':'BOX','itemAlias':'P','lotAlias':'L','locationAlias':'W','physicalScope':'B40-physical','revision':1},'B-human':{'type':'Human','organizationAlias':'ORG-B'},'B60':{'type':'QuantitySegment','organizationAlias':'ORG-B','itemAlias':'P-B','unit':'BOX','physicalScope':'B60-physical','revision':1},'P-B':{'type':'TradeItem','organizationAlias':'ORG-B','unit':'BOX'},'B-secret':{'type':'DocumentVersion','organizationAlias':'ORG-B','content':'other-organization-only'},'old-event':{'type':'Event','revision':1},'old-assessment':{'type':'Assessment'},'old-allocation':{'type':'Allocation','revision':1},'receipt-occurrence':{'type':'CanonicalOccurrence'},'doc':{'type':'DocumentVersion'},'policy-v1':{'type':'PolicyVersion'}}
        aliases.update({x:{'type':'Human'} for x in ROLES if x!='worker'})
        aliases['worker']={'type':'ServicePrincipal'}
        f={'schemaVersion':'1.0.0','fixtureId':f'{self.case.cid}-{self.sid}','synthetic':True,'baseRefs':[],'clock':{'asOf':NOW,'knownAt':NOW,'timezone':'Asia/Seoul','precision':'SECOND','deadlineInclusive':True},'versions':{'definition':'definition-v1','evaluator':'evaluator-v1','policy':'SYNTHETIC-policy-v1'},'actors':actors,'aliases':aliases,'baseline':{'setupIsExecutionCoverage':False,'independentDatabase':True,'hostOperatorAlias':'config','hostAuthorizations':[{'actorAlias':'config','actions':['backup','restore','retentionSweep','dataInventory','inspectArtifacts','scanArtifacts','tickScheduler','awaitRuntimeTask'],'environmentIds':['retention-isolated','restored-isolated','redaction-isolated','source-isolated'],'organizationAliases':['ORG-A'],'basis':'reviewed synthetic maintenance installation manifest','validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T00:00:00Z','revision':1}],'quantities':[],'goals':[{'workAlias':'O1','quantity':'100','unit':'BOX','quantityMode':'CUMULATIVE_EVENT','endpoint':'ARRIVED','placeAlias':'W','dueAt':'2026-10-06T00:00:00+09:00','deadlineInclusive':True,'ownerAlias':'intake'}], 'syntheticPolicyBasis':{'basis':'internal deterministic test policy only','reviewerAlias':'config','effectiveDate':'2026-10-01','officialRegulatoryAcceptance':False}, **baseline},'evidence':[],'responsibilities':[{'scope':{'workAlias':'O1','itemAlias':'P'},'ownerAlias':'intake','supervisorAlias':'supervisor','nextAction':'원천·실물 범위 대조','nextCheckAt':NEXT}]}
        return f
    def payload(self, name, obj, ns='warehouse', ev=None, version='v1'):
        p=ROOT/'verification/cases'/self.case.cid/'payloads'/self.sid/(name+'.json')
        write(p,obj)
        digest=hashlib.sha256(p.read_bytes()).hexdigest()
        self.fixture['evidence'].append({'alias':name,'sha256':digest,'sourceNamespace':ns,'externalEventId':ev or name,'sourceVersion':version,'occurredAt':EVENT,'recordedAt':NOW})
        self.fixture['aliases'][name]={'type':'DocumentVersion','organizationAlias':'ORG-A'}
        self.fixture['baseline'].setdefault('documentInputs',[]).append({'alias':name,'path':str(p.relative_to(ROOT)),'sha256':digest,'mediaType':'application/json','itemAlias':'P','workAlias':'O1','organizationAlias':'ORG-A','immutable':True})
        for actor in self.fixture['actors'].values(): actor['grant']['scope'].setdefault('documentAliases',[]).append(name)
        return {'documentId':alias(name),'sha256':digest,'sourceNamespace':ns,'externalEventId':ev or name,'sourceVersion':version,'occurredAt':EVENT,'originalOccurredAt':'2026-10-05T09:00:00+09:00','originalOffset':'+09:00','timezone':'Asia/Seoul','precision':'SECOND','recordedAt':NOW}
    def action(self, aid, kind, **kw):
        a={'id':aid,'kind':kind,'evidenceRefs':[aid+':actual-artifact'],**kw};self.data['actions'].append(a);return a
    def query(self, aid, cap='getObject', actor='recorder', request=None,route='api'):
        return self.action(aid,'query',actorRef=actor,route=route,capabilityId=cap,request={'scope':self.scope,'subjectRefs':[{'type':'TradeItem','id':alias('P')}],'asOf':NOW,'knownAt':NOW,**(request or {})})
    def invoke(self, aid, cap, actor='recorder', request=None,route='api'):
        return self.action(aid,'invoke',actorRef=actor,route=route,capabilityId=cap,request={'intentKind':'RECORD' if cap in ['correctEvidence','recordActivity','recordExternalReconciliation','attachEvidence'] else 'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,'scope':self.scope,'expectedRevision':1,'commandIdempotencyKey':f'{self.case.cid}-{self.sid}-{aid}',**(request or {})})
    def control(self, aid, typ, op, params):
        if typ=='process' and 'host' not in self.data['requiredAdapters']: self.data['requiredAdapters'].append('host')
        return self.action(aid,'control',control={'type':typ,'operation':op,'parameters':{'scope':self.scope,**params}})
    def db(self, aid, view, tables=None, known=NOW):
        self.action(aid,'observe',observation={'scope':self.observer_scope,'snapshotRef':result(view,'/response/snapshotRevision'),'asOf':NOW,'knownAt':known,'sources':tables or TABLES})
    def before(self,tables=None): self.query('view-before');self.db('db-before','view-before',tables)
    def after(self,tables=None): self.query('view-after');self.db('db-after','view-after',tables)
    def ass(self, aid, action,pointer,expected,obs,op='equals',field=None,where=None,baseline=None,unit=None,unitpointer=None,explain=None):
        source={'actionId':action,'pointer':pointer}
        if field:source['field']=field
        if where:source['where']=where
        a={'id':aid,'op':op,'source':source,'expected':expected,'requirementRefs':['D'+self.case.cid[1:]],'evidenceRefs':[action+':actual-artifact',action+':raw-source'],'scope':copy.deepcopy(next((x['observation']['scope'] for x in self.data['actions'] if x['id']==action and x['kind']=='observe'),self.scope)),'oracleExplanation':explain or aid.replace('-',' ')+'의 실제 값과 범위를 대조한다','oracleRef':{'oracleId':self.oracle,'observationNames':[obs]}}
        if baseline:a['baseline']={'actionId':baseline[0],'pointer':baseline[1]}
        if unit:
            a['unit']=unit;a['unitSource']={'actionId':action,'pointer':unitpointer or pointer.rsplit('/',1)[0]+'/unit'}
            if field:a['unitSource']['pointer']=pointer;a['unitSource']['field']='unit'
            if where:a['unitSource']['where']=where
        self.data['assertions'].append(a);return a
    def raw(self, aid,table,expected,obs,op='equals',field=None,where=None,db='db-after',explain=None):
        return self.ass(aid,db,'/data/rawRows/'+table,expected,obs,op,field,where,explain=explain)
    def unchanged(self,table,obs,aid=None):
        return self.ass(aid or table+'-unchanged','db-after','/data/rawRows/'+table,True,obs,'sameAs',baseline=('db-before','/data/rawRows/'+table))
    def duty(self,obs,kind,action='원천·실물 범위 대조',owner='intake'):
        where={'kind':kind,'status':'OPEN'}
        self.raw(kind+'-one-assignment','assignments',1,obs,'count',where=where)
        self.raw(kind+'-responsibility','assignments',[[alias(owner),action,NEXT]],obs,'relationSet',field=['ownerId','nextAction','nextCheckAt'],where=where)
        self.raw(kind+'-human-owner','assignments',['HUMAN'],obs,'exactSet',field='ownerType',where=where)
        self.query('api-duty-'+kind,'getObligations',actor='intake',request={'kind':kind})
        self.ass(kind+'-api-responsibility','api-duty-'+kind,'/response/data/items',[[alias(owner),action,NEXT]],obs,'relationSet',field=['ownerId','nextAction','nextCheckAt'],where=where)
    def finish(self): write(ROOT/self.data['fixtureRef'],self.fixture)

def build_t06():
    c=Case('T06','과거에 알던 사실과 현재 정정·시간·미확인 상태를 구별한다')
    o='T06.bitemporal-correction'
    s=c.sub('known-at-correction','100 기록을98로 정정해 당시100과 현재98을 읽는다',o)
    p=s.payload('receipt100',{'quantity':'100','unit':'BOX','location':'W'})
    q=s.payload('receipt98',{'quantity':'98','unit':'BOX','location':'W','reason':'검수 수량 정정'},ev='receipt100',version='v2');q['recordedAt']=LATER
    s.invoke('record-original','recordActivity',request={'kind':'RECEIPT_OBSERVATION','targetId':alias('Q100'),'quantity':'100','unit':'BOX','source':p})
    s.query('then','getEvidence',request={'activityId':result('record-original','/response/activityId'),'knownAt':NOW})
    s.db('db-before','then',['events','evidence_revisions','movements','assessments'])
    s.control('later','clock','set',{'asOf':LATER,'knownAt':LATER})
    s.invoke('correct','correctEvidence',request={'activityId':result('record-original','/response/activityId'),'expectedRevision':result('record-original','/response/revision'),'quantity':'98','unit':'BOX','source':q,'supersedes':result('record-original','/response/activityId'),'reason':'확인 수량 정정'})
    s.query('then-again','getEvidence',request={'activityId':result('record-original','/response/activityId'),'asOf':NOW,'knownAt':NOW})
    s.query('current','getEvidence',request={'activityId':result('record-original','/response/activityId'),'asOf':NOW,'knownAt':LATER})
    s.db('db-after','current',['events','evidence_revisions','movements','assessments'],LATER)
    for a,e,n in [('then-again','100','then-known'),('current','98','currently-known')]:
        s.ass(a+'-quantity',a,'/response/data/quantity',e,n,'decimalEquals',unit='BOX')
    s.ass('original-immutable','then-again','/response/data',True,'time-preserved','sameAs',baseline=('then','/response/data'))
    s.ass('utc-instant','current','/response/data/occurredAt',EVENT,'time-preserved','timeEquals')
    for field,value in [('originalOffset','+09:00'),('timezone','Asia/Seoul'),('precision','SECOND')]: s.ass('preserve-'+field,'current','/response/data/'+field,value,'time-preserved')
    s.raw('revision-pair','evidence_revisions',[['100','BOX',1],['98','BOX',2]],'currently-known','relationSet',field=['quantity','unit','revision'])
    s.raw('occurrence-recording-time-pair','evidence_revisions',[[1,EVENT,NOW],[2,EVENT,LATER]],'time-preserved','relationSet',field=['revision','occurredAt','recordedAt'])
    s.unchanged('movements','correction-not-fake-movement')
    s.finish()
    s=c.sub('inconsistent-after-dispatch','기출고100의98 정정은 가짜 과거 이동 없이 대조로 남는다',o,{'priorHistory':[{'kind':'CONFIRMED_RECEIPT','quantity':'100','unit':'BOX','occurredAt':EVENT,'eventAlias':'old-event','segmentAlias':'Q100'},{'kind':'DISPATCH','quantity':'100','unit':'BOX','segmentAlias':'Q100','destinationAlias':'C','allocationAlias':'old-allocation'}],'originalClaim':{'eventAlias':'old-event','quantity':'100','unit':'BOX','revision':1},'goals':[]})
    q=s.payload('correction98',{'quantity':'98','unit':'BOX','reason':'역산 모순'})
    s.before();s.invoke('correct','correctEvidence',request={'activityId':alias('old-event'),'quantity':'98','unit':'BOX','source':q,'reason':'이미 출고된 원 수량의 정정'})
    s.after()
    for t in ['movements','segments','allocations']:s.unchanged(t,'correction-not-fake-movement')
    s.ass('projection-pending','view-after','/response/data/projectionStatus','PENDING','correction-not-fake-movement')
    s.duty('correction-not-fake-movement','STOCK_RECONCILIATION')
    s.invoke('dispatch-pending','dispatchQuantity','warehouse',{'segmentId':alias('Q100'),'quantity':'2','unit':'BOX'})
    s.query('pending-check');s.db('pending-db','pending-check',['movements','allocations','outbox'])
    s.ass('pending-rejection','dispatch-pending','/response/outcome','REJECTED','correction-not-fake-movement')
    for t in ['movements','allocations','outbox']:s.ass('pending-'+t,'pending-db','/data/rawRows/'+t,True,'correction-not-fake-movement','sameAs',baseline=('db-after','/data/rawRows/'+t))
    s.finish()
    for incl,status,sid in [(True,'SATISFIED','inclusive-deadline'),(False,'UNSATISFIED','exclusive-deadline')]:
        s=c.sub(sid,'기한과 같은 사건 시각의 '+('포함' if incl else '제외')+' 끝점을 검증한다',o)
        s.fixture['baseline']['goals'][0]['deadlineInclusive']=incl
        p=s.payload('at-deadline',{'quantity':'100','unit':'BOX','occurredAt':'2026-10-05T15:00:00Z'})
        p['occurredAt']='2026-10-05T15:00:00Z';p['originalOccurredAt']='2026-10-06T00:00:00+09:00'
        s.invoke('record','recordActivity',request={'kind':'RECEIPT_OBSERVATION','quantity':'100','unit':'BOX','targetId':alias('Q100'),'source':p})
        s.invoke('match','matchSourceIdentity','reconciler',{'activityId':result('record','/response/activityId'),'targetId':alias('Q100'),'basis':alias('at-deadline')})
        s.invoke('link','linkCanonicalOccurrence','reconciler',{'activityId':result('record','/response/activityId'),'scope':s.scope,'targetId':alias('Q100'),'identityMatchId':result('match','/response/id'),'basis':alias('at-deadline')})
        s.invoke('confirm','confirmReceipt',request={'occurrenceId':result('link','/response/occurrenceId'),'segmentId':alias('Q100'),'quantity':'100','unit':'BOX','placeId':alias('W')})
        s.query('assessment','getAssessment',request={'workId':alias('O1')});s.db('db-after','assessment',['events','assessments'])
        s.ass('deadline-status','assessment','/response/data/result',status,'time-preserved')
        s.raw('deadline-meaning','assessments',[[status,incl,'2026-10-05T15:00:00Z','Asia/Seoul']],'time-preserved','relationSet',field=['result','deadlineInclusive','dueAt','timezone'])
        s.finish()
    s=c.sub('date-only-range','날짜만 알려진 사건이 기한을 걸치면 미확인이다',o)
    s.fixture['baseline']['goals'][0]['dueAt']='2026-10-05T12:00:00+09:00'
    p=s.payload('date-only',{'quantity':'100','unit':'BOX','occurredDate':'2026-10-05'})
    p.pop('occurredAt');p.pop('originalOccurredAt');p['occurredDate']='2026-10-05'
    p.update({'precision':'DATE','occurredRange':{'start':'2026-10-04T15:00:00Z','end':'2026-10-05T15:00:00Z','endInclusive':False}})
    s.invoke('record','recordActivity',request={'kind':'RECEIPT_OBSERVATION','quantity':'100','unit':'BOX','targetId':alias('Q100'),'source':p})
    s.invoke('match','matchSourceIdentity','reconciler',{'activityId':result('record','/response/activityId'),'targetId':alias('Q100'),'basis':alias('date-only')})
    s.invoke('link','linkCanonicalOccurrence','reconciler',{'activityId':result('record','/response/activityId'),'targetId':alias('Q100'),'identityMatchId':result('match','/response/id'),'basis':alias('date-only')})
    s.invoke('confirm','confirmReceipt',request={'occurrenceId':result('link','/response/occurrenceId'),'segmentId':alias('Q100'),'quantity':'100','unit':'BOX','placeId':alias('W'),'occurredRange':p['occurredRange'],'precision':'DATE'})
    s.query('evidence','getEvidence',request={'activityId':result('record','/response/activityId')});s.query('assessment','getAssessment',request={'workId':alias('O1')});s.db('db-after','assessment',['events','assessments','movements'])
    s.ass('date-range','evidence','/response/data/occurredRange',p['occurredRange'],'time-preserved')
    s.ass('date-precision','evidence','/response/data/precision','DATE','time-preserved')
    s.ass('date-unverified','assessment','/response/data/result','UNVERIFIED','time-preserved')
    a=s.raw('date-known-receipt-100','movements','100','time-preserved','sumEquals',field='quantity',where={'kind':'RECEIPT'});a.update(unit='BOX',unitSource={'actionId':'db-after','pointer':'/data/rawRows/movements','where':{'kind':'RECEIPT'},'field':'unit'})
    s.finish()
    o='T06.unknown-state-distinction'
    s=c.sub('four-states','미입력·조사전·적용안됨·상충을0으로 합치지 않는다',o,{'observedAttributes':[{'name':'missing','state':'MISSING'},{'name':'unknown','state':'UNKNOWN','reason':'조사 전'},{'name':'irrelevant','state':'NOT_APPLICABLE','reason':'적용 범위 밖'},{'name':'conflicting','state':'CONFLICT','claimValues':['98','100'],'unit':'BOX'}]})
    s.before(['claims','movements','segments','allocations']);s.query('states');s.after(['claims','movements','segments','allocations'])
    states=['MISSING','UNKNOWN','NOT_APPLICABLE','CONFLICT']
    s.ass('response-states','states','/response/data/observedAttributes',states,'state-values','exactSet',field='state')
    s.raw('raw-states','claims',states,'state-values','exactSet',field='state')
    for t in ['claims','movements','segments','allocations']:s.unchanged(t,'zero-inference')
    for index in range(4): s.ass('state-'+str(index)+'-not-known-zero','states',f'/response/data/observedAttributes/{index}/knownQuantity',True,'zero-inference','absent')
    s.ass('conflict-values','states','/response/data/conflicts/0/claimValues',['98','100'],'conflict-preserved','exactSet')
    s.finish()
    for sid,op,operands,status,conflict in [('all-false-conflict','all',['FALSE','CONFLICT'],'UNSATISFIED',True),('all-true-conflict','all',['TRUE','CONFLICT'],'UNVERIFIED',True),('any-true-conflict','any',['TRUE','CONFLICT'],'SATISFIED',True),('not-unknown','not',['UNKNOWN'],'UNVERIFIED',False)]:
        claims=[]; predicates=[]
        for index,state in enumerate(operands):
            name='quantity-'+str(index)
            claim={'property':name,'unit':'BOX','state':'KNOWN' if state in ['TRUE','FALSE'] else state}
            if state in ['TRUE','FALSE']:claim['value']='100' if state=='TRUE' else '90'
            if state=='CONFLICT':claim['sourceValues']=['98','100']
            claims.append(claim)
            predicates.append({'operator':'compare','propertyRef':name,'comparison':'GTE','value':'100','unit':'BOX','evidenceSelector':'verified-scoped-events'})
        s=c.sub(sid,'typed '+op+' 판정과 상충 flag를 독립 검증한다',o,{'sourceClaims':claims,'typedPredicate':{'operator':op,'predicates':predicates}})
        s.invoke('create-goal','createWork',request={'verb':'inspect','goalPredicate':s.fixture['baseline']['typedPredicate'],'ownerId':alias('intake'),'targetId':alias('Q100')})
        s.query('assessment','getAssessment',request={'workId':result('create-goal','/response/workId')});s.db('db-after','assessment',['assessments','movements','allocations'])
        s.ass('predicate-result','assessment','/response/data/result',status,'conflict-preserved');s.ass('predicate-conflict','assessment','/response/data/conflict',conflict,'conflict-preserved')
        s.raw('raw-predicate','assessments',[[status,conflict,'evaluator-v1']],'conflict-preserved','relationSet',field=['result','conflict','evaluatorVersion'])
        s.raw('no-physical-inference','movements',0,'zero-inference','count');s.finish()
    c.finish();return c

def build_t22():
    c=Case('T22','원천 중복·상충·canonical 동일성과 불명확한 외부 효과를 대조한다')
    o='T22.source-duplicate-conflict'
    s=c.sub('source-key-hash-conflict','같은source키 replay와 다른hash 상충을 원문 보존으로 구별한다',o)
    p=s.payload('carrier-A',{'quantity':'60','unit':'BOX','physicalScope':'A60'},'carrier','EV60')
    q=s.payload('carrier-B',{'quantity':'40','unit':'BOX','physicalScope':'A60'},'carrier','EV60')
    s.invoke('first','recordActivity',request={'source':p,'quantity':'60','unit':'BOX','targetId':alias('A60')});s.query('accepted','getInbox',request={'sourceKey':['carrier','EV60','v1']});s.db('db-before','accepted',['inbox','movements','canonical_links'])
    s.invoke('replay','recordActivity',request={'source':p,'quantity':'60','unit':'BOX','targetId':alias('A60')})
    s.invoke('conflict','recordActivity',request={'source':q,'quantity':'40','unit':'BOX','targetId':alias('A60')});s.query('inbox','getInbox',request={'sourceKey':['carrier','EV60','v1']});s.db('db-after','inbox',['inbox','movements','canonical_links','assignments','claims'])
    s.raw('accepted-once','inbox',1,'accepted-inbox-per-key','count',where={'sourceNamespace':'carrier','externalEventId':'EV60','sourceVersion':'v1','status':'ACCEPTED'})
    s.ass('hash-conflict','conflict','/response/error/code','EVIDENCE_CONFLICT','same-key-other-hash')
    s.raw('accepted-original-hash','inbox',[p['sha256']],'last-write-wins','exactSet',field='payloadHash',where={'status':'ACCEPTED'})
    s.raw('conflicting-raw-hash','claims',[q['sha256']],'same-key-other-hash','exactSet',field='payloadHash',where={'state':'CONFLICT'})
    s.unchanged('movements','last-write-wins');s.unchanged('canonical_links','identity-unverified-auto-sum')
    for name,meta in [('carrier-A',p),('carrier-B',q)]:
        s.query('download-'+name,'getEvidence',request={'evidenceId':alias(name),'download':True},route='blob')
        s.control('inspect-'+name,'process','inspectArtifacts',{'inspectionId':'source-'+name,'artifacts':result('download-'+name,'/response/downloadedArtifacts')})
        s.ass('preserved-'+name+'-bytes','inspect-'+name,'/data/hostObservation/observedArtifacts/0/sha256',meta['sha256'],'last-write-wins')
    s.duty('source-conflict-owner','EVIDENCE_CONFLICT');s.finish()
    s=c.sub('two-sources-one-occurrence','운송과창고의 같은60은 두문서 한실물이다',o)
    p=s.payload('carrier60',{'quantity':'60','unit':'BOX','physicalScope':'A60'},'carrier','EV60')
    q=s.payload('warehouse60',{'quantity':'60','unit':'BOX','physicalScope':'A60'},'warehouse','W60')
    s.invoke('carrier','recordActivity',request={'source':p,'quantity':'60','unit':'BOX','targetId':alias('A60')})
    s.invoke('match-carrier','matchSourceIdentity','reconciler',{'activityId':result('carrier','/response/activityId'),'targetId':alias('A60'),'basis':alias('carrier60')})
    s.invoke('canonical','linkCanonicalOccurrence','reconciler',{'activityId':result('carrier','/response/activityId'),'targetId':alias('A60'),'identityMatchId':result('match-carrier','/response/id'),'basis':alias('carrier60')})
    s.invoke('receipt','confirmReceipt',request={'occurrenceId':result('canonical','/response/occurrenceId'),'segmentId':alias('A60'),'quantity':'60','unit':'BOX','placeId':alias('W')})
    s.before(['movements','canonical_links','segments']);s.invoke('warehouse','recordActivity',request={'source':q,'quantity':'60','unit':'BOX','targetId':alias('A60')})
    s.query('unverified');s.db('db-unverified','unverified',['movements','segments'])
    s.invoke('match-warehouse','matchSourceIdentity','reconciler',{'activityId':result('warehouse','/response/activityId'),'targetId':alias('A60'),'basis':alias('warehouse60')})
    s.invoke('link-warehouse','linkCanonicalOccurrence','reconciler',{'activityId':result('warehouse','/response/activityId'),'occurrenceId':result('canonical','/response/occurrenceId'),'identityMatchId':result('match-warehouse','/response/id'),'basis':alias('warehouse60')});s.after(['movements','segments','canonical_links'])
    for t in ['movements','segments']:s.ass('unverified-'+t,'db-unverified','/data/rawRows/'+t,True,'identity-unverified-auto-sum','sameAs',baseline=('db-before','/data/rawRows/'+t))
    a=s.raw('receipt-sum','movements','60','canonical-receipt','sumEquals',field='quantity',where={'kind':'RECEIPT'});a.update(unit='BOX',unitSource={'actionId':'db-after','pointer':'/data/rawRows/movements','where':{'kind':'RECEIPT'},'field':'unit'})
    s.query('arrival-api','getInventory',request={'workId':alias('O1'),'quantityMode':'CUMULATIVE_EVENT','endpoint':'ARRIVED'})
    s.ass('arrival-api-60','arrival-api','/response/data/arrivalQuantity','60','canonical-receipt','decimalEquals',unit='BOX')
    s.raw('one-physical-receipt','movements',1,'canonical-receipt','count',where={'kind':'RECEIPT'})
    s.raw('one-canonical-two-reports','canonical_links',[[result('carrier','/response/activityId'),result('canonical','/response/occurrenceId')],[result('warehouse','/response/activityId'),result('canonical','/response/occurrenceId')]],'canonical-receipt','relationSet',field=['activityId','occurrenceId']);s.finish()
    o='T22.reconciliation-control-and-source-order'
    for sid,cap,actor,request in [('cross-org-match','matchSourceIdentity','reconciler',{'targetId':alias('P-B')}),('overlapping-match','linkCanonicalOccurrence','reconciler',{'physicalScope':['B40-physical'],'occurrenceId':alias('candidate-occurrence'),'targetId':alias('B40')}),('arbitrary-priority','resolveEvidenceConflict','reconciler',{'strategy':'LAST_RECEIVED_WINS','sourcePriority':'carrier'}),('unauthorized-match','matchSourceIdentity','readAgent',{'targetId':alias('A60')})]:
        s=c.sub(sid,'권한 또는 실물범위 근거 없는 '+sid+' 대조를 거부한다',o,{'pendingSource':{'sourceNamespace':'carrier','eventAlias':'old-event','quantity':'60','unit':'BOX','status':'QUARANTINED'},'sourceProfiles':[{'namespace':'carrier','mapping':'scoped-item-lot-physical-v1','basis':'synthetic-reviewed-profile','revisionOrdering':'VERIFIED_MONOTONIC','timeTrust':'ORIGINAL_OFFSET','schema':'source-v1','dedupKey':['namespace','eventId','version'],'conflictOwnerAlias':'intake','retryPolicy':'bounded-no-external-write','externalWrites':False}]})
        basis={'reason':'공식 대조 근거 없음'}
        if sid=='overlapping-match':
            members_a=[f'physical-box-{i:03}' for i in range(1,61)]
            members_b=[f'physical-box-{i:03}' for i in range(41,81)]
            s.fixture['aliases']['candidate-occurrence']={'type':'CanonicalOccurrence','candidateOnly':True}
            s.fixture['baseline'].update({'physicalMembership':[{'segmentAlias':'A60','members':members_a},{'segmentAlias':'B40','members':members_b}],'canonicalOccurrences':[{'occurrenceAlias':'receipt-occurrence','segmentAlias':'A60','physicalMembers':members_a,'quantity':'60','unit':'BOX','verified':True}],'pastMovements':[{'kind':'RECEIPT','occurrenceAlias':'receipt-occurrence','segmentAlias':'A60','quantity':'60','unit':'BOX','occurredAt':EVENT}],'pendingSource':{'sourceNamespace':'carrier','eventAlias':'old-event','quantity':'40','unit':'BOX','physicalMembers':members_b,'status':'QUARANTINED'}})
            p=s.payload('physical-membership',{'existingMembers':members_a,'proposedMembers':members_b,'overlapMembers':members_a[40:]},'carrier','PHYSICAL40')
            basis=alias('physical-membership')
            request={**request,'quantity':'40','unit':'BOX','evidenceHash':p['sha256']}
        if sid=='arbitrary-priority':
            s.fixture['baseline']['conflictingClaims']=[{'eventAlias':'old-event','sourceNamespace':'carrier','quantity':'60','unit':'BOX','state':'CONFLICT'},{'eventAlias':'old-event','sourceNamespace':'warehouse','quantity':'40','unit':'BOX','state':'CONFLICT'}]
        s.before();s.invoke('invalid',cap,actor,{'activityId':alias('old-event'),'basis':basis,**request});s.after()
        s.ass('invalid-outcome','invalid','/response/outcome','REJECTED','invalid-match-effects')
        for t in ['identity_matches','canonical_links','movements','segments','assessments']:s.unchanged(t,'invalid-match-effects')
        if sid=='overlapping-match':
            a=s.raw('existing-receipt-stays-60','movements','60','invalid-match-effects','sumEquals',field='quantity',where={'kind':'RECEIPT'});a.update(unit='BOX',unitSource={'actionId':'db-after','pointer':'/data/rawRows/movements','where':{'kind':'RECEIPT'},'field':'unit'})
        s.duty('quarantine-owner','SOURCE_QUARANTINE');s.finish()
    s=c.sub('late-old-release','늦은옛해제 보고는 현재 새보류를 덮지 않는다',o,{'restrictionHistory':[{'idAlias':'new-hold','status':'ACTIVE','revision':2,'segmentAlias':'A60','quantity':'60','unit':'BOX','action':'SELL','sourceVersion':'v2'}],'sourceProfiles':[{'namespace':'carrier','revisionOrdering':'UNVERIFIED','timeTrust':'UNVERIFIED','conflictOwnerAlias':'intake'}]})
    s.fixture['aliases']['new-hold']={'type':'Restriction','revision':2}
    p=s.payload('old-release',{'kind':'RELEASE','sourceVersion':'v1','restriction':'new-hold'},'carrier','RELEASE',version='v1')
    s.before();s.invoke('late','recordActivity',request={'source':p,'kind':'RESTRICTION_RELEASE','targetId':alias('A60')});s.after()
    s.unchanged('restrictions','old-source-overwrites-new-restriction');s.raw('new-hold-active','restrictions',[[alias('new-hold'),'ACTIVE',2,'60','BOX']],'old-source-overwrites-new-restriction','relationSet',field=['id','status','revision','quantity','unit']);s.duty('quarantine-owner','SOURCE_QUARANTINE');s.finish()
    s=c.sub('unprofiled-automatic-source','미설정 자동 source를 활성화하지 않고 원문과 담당을 남긴다',o,{'sourceProfiles':[]})
    p=s.payload('unprofiled',{'quantity':'60','unit':'BOX'},'carrier','AUTO60')
    s.before();s.invoke('automatic','recordActivity',request={'source':p,'ingestionMode':'AUTOMATED','connectorProfileRevision':'UNCONFIGURED','quantity':'60','unit':'BOX'});s.after()
    s.unchanged('source_profiles','unprofiled-connector-activation');s.unchanged('movements','unprofiled-connector-activation')
    s.control('connector-config','process','dataInventory',{'environmentId':'source-isolated','inventoryId':'connector-activation-state','authoritativeSourceId':'actual-connector-config'})
    s.ass('automatic-connector-disabled','connector-config','/data/hostObservation/extractor/rawRows/enabledSourceNamespaces',[],'unprofiled-connector-activation','exactSet')
    s.duty('quarantine-owner','SOURCE_QUARANTINE');s.finish()
    s=c.sub('reviewed-source-profile','검토된 source profile 필드와 원문·pending 처리를 검증한다',o,{'sourceProfiles':[{'namespace':'carrier','basis':'internal synthetic source review','mappingVersion':'map-v1','revisionOrdering':'VERIFIED_MONOTONIC','timeTrust':'ORIGINAL_OFFSET','schemaVersion':'source-v1','dedupPolicy':'NAMESPACE_EVENT_VERSION','conflictOwnerAlias':'intake','retryPolicy':'BOUNDED','externalWrites':False}]})
    p=s.payload('profile-report',{'quantity':'60','unit':'BOX'},'carrier','PROFILE60')
    s.invoke('intake','recordActivity',request={'source':p,'targetId':alias('A60'),'quantity':'60','unit':'BOX'});s.after()
    s.raw('profile-fields','source_profiles',[['carrier','internal synthetic source review','map-v1','VERIFIED_MONOTONIC','ORIGINAL_OFFSET','source-v1','NAMESPACE_EVENT_VERSION',alias('intake'),'BOUNDED',False]],'source-profile','relationSet',field=['namespace','basis','mappingVersion','revisionOrdering','timeTrust','schemaVersion','dedupPolicy','conflictOwnerId','retryPolicy','externalWrites'])
    s.raw('original-hash','inbox',[p['sha256']],'source-profile','exactSet',field='payloadHash')
    s.query('pending-inbox','getInbox',request={'activityId':result('intake','/response/activityId')})
    s.ass('explicit-pending','pending-inbox','/response/data/processingState','QUARANTINED','source-profile')
    s.query('raw-profile-document','getEvidence',request={'evidenceId':alias('profile-report'),'download':True},route='blob')
    s.control('inspect-original','process','inspectArtifacts',{'inspectionId':'source-original','artifacts':result('raw-profile-document','/response/downloadedArtifacts')})
    s.ass('original-bytes-hash','inspect-original','/data/hostObservation/observedArtifacts/0/sha256',p['sha256'],'source-profile')
    s.duty('source-profile','SOURCE_QUARANTINE');s.finish()
    o='T22.unknown-external-reconciliation'
    for variant in ['unknown','success','failure-retry','failure-no-retry','no-lookup','local-cancel']:
        s=c.sub('external-'+variant,'외부 응답유실 뒤 '+variant+'의 결과와 재발행을 대조한다',o,{'approvedPurchaseRevision':{'workAlias':'O1','quantity':'100','unit':'BOX','revision':1,'proposalHash':hashlib.sha256(b'approved100').hexdigest(),'approverAlias':'admin','approvedAt':'2026-10-06T00:00:00Z','validUntil':'2026-10-31T00:00:00Z','consumptionPolicy':'ONE_EFFECT'},'externalPolicy':{'idempotencySupported':variant!='no-lookup','lookupSupported':variant!='no-lookup','retryAfterConfirmedFailure':variant=='failure-retry','automaticReissueWhileUnknown':False}})
        s.control('remote-mode','externalResponder','configure',{'receiverId':'purchase-peer','behavior':'REJECT_THEN_DROP_RESPONSE' if variant.startswith('failure') else 'ACCEPT_THEN_DROP_RESPONSE','remoteResultId':'REMOTE-RESULT-1','idempotencySupported':variant!='no-lookup','lookupSupported':variant!='no-lookup'})
        s.invoke('send','dispatchPurchaseOrder','operations',{'workId':alias('O1'),'approvedRevision':1,'approvedHash':hashlib.sha256(b'approved100').hexdigest(),'channel':'purchase-peer'})
        s.control('send-tick','process','tickScheduler',{'schedulerId':'outbox-scheduler','tickId':'initial-outbox-tick','asOf':NOW,'workId':alias('O1')})
        s.control('send-terminal','process','awaitRuntimeTask',{'schedulerId':'outbox-scheduler','taskId':result('send-tick','/data/hostObservation/extractor/rawRows/tasks/0/taskId')})
        s.query('unknown','searchOperationalIssues',request={'workId':alias('O1')});s.db('db-before','unknown',['outbox','command_records','assignments','domain_records'])
        s.ass('unknown-state','unknown','/response/data/issues/0/state','UNKNOWN_EXTERNAL','lost-response-state')
        s.raw('raw-unknown','outbox',['UNKNOWN_EXTERNAL'],'lost-response-state','exactSet',field='state')
        s.control('remote-before','externalResponder','inspectRequests',{'receiverId':'purchase-peer','externalOperationId':result('send','/response/externalOperationId')})
        if variant in ['success','failure-retry','failure-no-retry']:
            s.control('remote-proof','externalResponder','queryOperation',{'receiverId':'purchase-peer','externalOperationId':result('send','/response/externalOperationId')})
            s.invoke('attach-proof','attachEvidence',request={'artifact':result('remote-proof','/data/receiptArtifact'),'sourceNamespace':'purchase-peer','externalOperationId':result('send','/response/externalOperationId'),'evidenceHash':result('remote-proof','/data/receiptHash')})
            s.invoke('reconcile','recordExternalReconciliation','reconciler',{'externalOperationId':result('send','/response/externalOperationId'),'externalResult':'SUCCESS' if variant=='success' else 'FAILURE','remoteResultId':'REMOTE-RESULT-1','evidenceId':result('attach-proof','/response/evidenceId')})
            s.query('reconciled','searchOperationalIssues',request={'workId':alias('O1')});s.db('db-reconciled','reconciled',['outbox'])
            if variant.startswith('failure'):
                s.raw('confirmed-failure-before-retry','outbox',['CONFIRMED_FAILURE'],'external-transition','exactSet',field='state',db='db-reconciled')
        if variant=='local-cancel':s.invoke('cancel','cancelPurchase','operations',{'workId':alias('O1'),'reason':'로컬 취소 요청'})
        # Plan §7.2: retry names only the original command record and a reason. The server derives the
        # original actor, canonical hash, idempotency key and externalOperationId from that record.
        original=[{'type':'CommandRecord','id':result('send','/response/commandId')}]
        s.query('retry-target','getObject','operations',{'subjectRefs':original})
        s.invoke('retry','retrySafeCommand','operations',{'subjectRefs':original,'expectedRevision':result('retry-target','/response/data/revision'),'slots':{'commandId':result('send','/response/commandId'),'reason':'외부 대조 상태와 원 canonical 명령·현재 위임을 다시 확인한다'}})
        s.control('retry-tick','process','tickScheduler',{'schedulerId':'outbox-scheduler','tickId':'retry-outbox-tick','asOf':NOW,'workId':alias('O1')})
        if variant=='failure-retry': s.control('retry-terminal','process','awaitRuntimeTask',{'schedulerId':'outbox-scheduler','taskId':result('retry-tick','/data/hostObservation/extractor/rawRows/tasks/0/taskId')})
        s.control('remote-after','externalResponder','inspectRequests',{'receiverId':'purchase-peer','externalOperationId':result('send','/response/externalOperationId')});s.after(['outbox','command_records','assignments','domain_records','audit'])
        s.raw('retry-server-derived-target','audit',[[alias('operations'),result('send','/response/commandId'),f'T22-{s.sid}-retry']],'external-transition','relationSet',field=['actorId','targetId','commandIdempotencyKey'],where={'action':'retrySafeCommand'},
              explain='서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2).')
        count=2 if variant=='failure-retry' else 1
        obs='confirmed-success-reissue' if variant=='success' else 'unreconciled-external-reissue' if variant in ['unknown','no-lookup'] else 'external-transition'
        s.ass('remote-request-count','remote-after','/data/requests',count,obs,'count')

        for index in range(count): s.ass('same-external-operation-'+str(index),'remote-after',f'/data/requests/{index}/externalOperationId',True,'external-transition','sameAs',baseline=('remote-before','/data/requests/0/externalOperationId'))
        if variant=='success':
            s.raw('success-existing-result','outbox',[['CONFIRMED_SUCCESS','REMOTE-RESULT-1']],'external-transition','relationSet',field=['state','remoteResultId'])
        if variant in ['unknown','no-lookup']:
            s.raw('unknown-kept','outbox',['UNKNOWN_EXTERNAL'],'unreconciled-external-reissue','exactSet',field='state');s.duty('external-reconciliation-owner','EXTERNAL_RECONCILIATION')
        if variant=='local-cancel':
            s.ass('remote-cancel-not-sent','remote-after','/data/requests',0,'external-transition','count',where={'kind':'CANCEL'})
            s.raw('local-cancel-pending','domain_records',['CANCEL_PENDING_EXTERNAL'],'external-transition','exactSet',field='state')
            s.duty('external-reconciliation-owner','EXTERNAL_RECONCILIATION')
        if variant.startswith('failure'):
            s.raw('failure-retry-policy','outbox',[variant=='failure-retry'],'external-transition','exactSet',field='retryEligible',db='db-reconciled')
            if variant=='failure-retry':s.raw('new-attempt-unknown-blocks-next-retry','outbox',[[False,'UNKNOWN_EXTERNAL']],'external-transition','relationSet',field=['retryEligible','state'])
        s.finish()
    c.finish();return c

def build_t24():
    c=Case('T24','모든 인가 경로·감사 rollback·보존·복원·원문 redaction을 검증한다')
    o='T24.all-auth-surfaces'
    for surface,cap,route,actor,req in [('search','searchObjects','api','readAgent',{'organizationId':alias('ORG-B'),'filter':{'id':alias('B-secret')}}),('blob','getEvidence','blob','readAgent',{'evidenceId':alias('B-secret'),'download':True}),('batch','dispatchQuantity','batch','warehouse',{'atomic':True,'commands':[{'capabilityId':'placeHold','subjectId':alias('A60'),'quantity':'20','unit':'BOX'},{'capabilityId':'dispatchQuantity','subjectId':alias('B60'),'quantity':'20','unit':'BOX'}]}),('worker','dispatchQuantity','worker','worker',{'originalActorId':alias('readAgent'),'originalDelegatorId':alias('supervisor'),'segmentId':alias('A60'),'quantity':'20','unit':'BOX'}),('admin','assignCapability','management','admin',{'organizationId':alias('ORG-B'),'subjectId':alias('B-human'),'capability':'dispatchQuantity'})]:
        s=c.sub('deny-'+surface,surface+' 경로의 조직밖 또는 READ위임 우회를 거부한다',o,{'quantities':[{'alias':'A60','quantity':'60','unit':'BOX','physicalScope':'A60-physical'}],'serviceContext':{'workerAlias':'worker','contextType':'SYSTEM_USER','delegatedHumanAlias':'readAgent','delegatedActions':READS},'otherOrganizationStock':[{'alias':'B60','organizationAlias':'ORG-B','itemAlias':'P-B','quantity':'60','unit':'BOX'}],'managementScope':{'adminAlias':'admin','organizationAliases':['ORG-A'],'businessActions':[]}})
        s.observer_scope={'organizationIds':[alias('ORG-A'),alias('ORG-B')],'caseId':c.cid,'subcaseId':s.sid}
        if surface=='batch': s.invoke('permitted-first','placeHold','warehouse',{'segmentId':alias('A60'),'quantity':'10','unit':'BOX','reason':'가상 QC 사유','action':'SELL'})
        s.before();s.query('allowed-own','getObject','readAgent',{'objectId':alias('P')})
        if surface in ['search','blob']:s.query('attack',cap,actor,req,route)
        else:s.invoke('attack',cap,actor,req,route)
        s.after()
        if surface=='search':s.ass('search-data-empty','attack','/response/data/items',0,'cross-org-data-leak','count')
        else:
            s.ass('forbidden-code','attack','/response/error/code','FORBIDDEN','surface-auth')
            s.ass('no-secret-reference','attack','/response/error/targetId',True,'cross-org-data-leak','absent')
        for t in ['domain_records','movements','segments','allocations','outbox','command_records']:s.unchanged(t,'forbidden-write-effects')
        s.raw('denial-audit','audit',1,'surface-auth','count',where={'kind':'DENIAL','action':cap})
        if surface=='blob':s.ass('no-download-ref','attack','/response/downloadReference',True,'surface-auth','absent')
        if surface=='batch':
            s.unchanged('restrictions','surface-auth')
            s.ass('permitted-first-applied','permitted-first','/response/outcome','APPLIED','surface-auth')
            s.raw('first-hold-survives-batch','restrictions',1,'surface-auth','count')
        if surface=='worker':s.raw('worker-delegated-actor','audit',[alias('readAgent')],'surface-auth','exactSet',field='actorId',where={'kind':'DENIAL'})
        s.finish()
    s=c.sub('authorized-blob','서버 인가 후 단기참조로 자기 근거 bytes를 읽는다',o)
    p=s.payload('own-document',{'quantity':'60','unit':'BOX','scope':'ORG-A'})
    s.query('download','getEvidence','readAgent',{'evidenceId':alias('own-document'),'download':True},'blob')
    s.control('inspect','process','inspectArtifacts',{'artifacts':result('download','/response/downloadedArtifacts'),'inspectionId':'own-blob-inspection'})
    s.ass('own-download-digest','inspect','/data/hostObservation/observedArtifacts/0/sha256',p['sha256'],'surface-auth')
    s.ass('short-reference-expiry','download','/response/downloadReference/expiresAt','2026-10-07T00:05:00Z','surface-auth','timeEquals')
    s.ass('short-reference-object','download','/response/downloadReference/evidenceId',alias('own-document'),'surface-auth')
    s.finish()
    o='T24.audit-failure-rollback'
    s=c.sub('audit-rollback-and-retry','감사저장 실패는 모든 거래효과를 rollback하고 같은key로 재시도한다',o,{'quantities':[{'alias':'A60','quantity':'60','unit':'BOX','physicalScope':'A60-physical'}],'allocation':[{'alias':'old-allocation','segmentAlias':'A60','workAlias':'S1','quantity':'20','unit':'BOX','status':'EXECUTABLE','revision':1}],'eligibilityBasis':{'segmentAlias':'A60','action':'DISPATCH','syntheticQCEvidence':'PASS','syntheticRegulatoryEvidence':'ALLOWED','dispositionScope':'20 BOX','effectiveUntil':'2026-10-31T00:00:00Z'}})
    basis=s.payload('dispatch-basis',{'QC':'PASS','regulatory':'ALLOWED','disposition':'20 BOX','scope':'A60'})
    s.before();s.control('fault','fault','install',{'faultId':'audit-persist-failure','point':'AUDIT_INSERT','commandKey':'T24-audit-effect','failure':'PERSISTENCE_ERROR'})
    request={'segmentId':alias('A60'),'allocationId':alias('old-allocation'),'workId':alias('S1'),'quantity':'20','unit':'BOX','destinationId':alias('C'),'commandIdempotencyKey':'T24-audit-effect','evidenceRefs':[alias('dispatch-basis')]}
    s.invoke('fail','dispatchQuantity','warehouse',request);s.after()
    for table,obs in [('domain_records','committed-domain-effects'),('movements','committed-ledger-effects'),('allocations','committed-allocation-effects'),('obligations','committed-duty-effects'),('assignments','committed-duty-effects'),('outbox','committed-outbox-effects'),('command_records','committed-idempotency-result')]:s.unchanged(table,obs)
    s.ass('audit-error','fail','/response/error/code','AUDIT_PERSISTENCE_FAILED','committed-domain-effects')
    s.control('remove-fault','fault','remove',{'faultId':'audit-persist-failure'});s.invoke('retry','dispatchQuantity','warehouse',request);s.query('retry-view');s.db('retry-db','retry-view',['audit','movements','allocations','outbox','command_records','domain_records'])
    s.ass('retry-applied','retry','/response/outcome','APPLIED','audit-fields')
    s.ass('audit-complete','retry-db','/data/rawRows/audit',[[alias('warehouse'),alias('supervisor'),alias('S1'),alias('A60'),'dispatchQuantity','definition-v1','SYNTHETIC-policy-v1','APPLIED','T24-audit-effect']],'audit-fields','relationSet',field=['actorId','delegatorId','workId','targetId','action','definitionVersion','policyVersion','result','commandIdempotencyKey'],where={'kind':'MUTATION'})
    s.ass('audit-before-after','retry-db','/data/rawRows/audit',[{'allocationStatus':'EXECUTABLE','warehouseHeld':{'value':'60','unit':'BOX'}}],'audit-fields',field='before',where={'kind':'MUTATION'})
    s.ass('audit-after','retry-db','/data/rawRows/audit',[{'allocationStatus':'CONSUMED','warehouseHeld':{'value':'40','unit':'BOX'}}],'audit-fields',field='after',where={'kind':'MUTATION'})
    s.ass('audit-effect-reference','retry-db','/data/rawRows/audit',[result('retry','/response/movementId')],'audit-fields',field='movementId',where={'kind':'MUTATION'})
    s.ass('audit-request-ref','retry-db','/data/rawRows/audit',[result('retry','/response/requestId')],'audit-fields',field='requestId',where={'kind':'MUTATION'})
    s.raw('audit-no-extra-approval','audit',[[]],'audit-fields',field='approvalRefs',where={'kind':'MUTATION'})
    s.raw('audit-evidence','audit',[[alias('dispatch-basis')]],'audit-fields',field='evidenceIds',where={'kind':'MUTATION'})
    s.raw('retry-one-movement','movements',1,'audit-fields','count',where={'commandIdempotencyKey':'T24-audit-effect','evidenceRefs':[alias('dispatch-basis')]})
    s.finish()
    o='T24.retention-legalhold-blob-restore'
    for variant in ['legal-hold','active-reference','R6-unconfirmed','authorized-delete','restore-deleted']:
        policy=[{'artifactType':typ,'retentionDays':days,'basis':'synthetic internal retention review','effectiveDate':'2026-01-01','reviewerAlias':'config','deletionMethod':'TOMBSTONE_AND_BLOB_DELETE' if typ=='document' else 'REDACT','policyVersion':'retention-v1','approved':variant!='R6-unconfirmed'} for typ,days in [('document',30),('ledger',365),('definition',730),('audit',180),('personalData',7)]]
        s=c.sub(variant,'보존 '+variant+'의 근거·참조·실제blob효과를 검증한다',o,{'retentionPolicies':policy,'R6':{'status':'UNCONFIRMED' if variant=='R6-unconfirmed' else 'CONFIRMED_SYNTHETIC_ONLY','operationalDeletionEnabled':False},'legalHolds':[{'documentAlias':'doc','holdId':'HOLD-1','status':'ACTIVE','basis':'synthetic case preservation'}] if variant=='legal-hold' else [],'activeDutyReferences':[{'documentAlias':'doc','workAlias':'O1','status':'OPEN','ownerAlias':'intake'}] if variant=='active-reference' else [],'documentCreatedAt':'2026-01-01T00:00:00Z'})
        p=s.payload('retained-document',{'quantity':'60','unit':'BOX','identity':'immutable-original'})
        s.fixture['aliases']['doc']['documentInputAlias']='retained-document'
        s.before(['policies','documents','tombstones','deletion_log','obligations','assignments'])
        if variant=='restore-deleted':s.control('backup','process','backup',{'environmentId':'retention-isolated','backupId':'old-backup','snapshotId':result('view-before','/response/snapshotRevision')})
        s.control('sweep','process','retentionSweep',{'actorRef':'config','environmentId':'retention-isolated','policyVersion':'retention-v1','sweepId':'sweep-'+variant,'profile':'SYNTHETIC_ONLY','asOf':NOW,'artifactIds':[alias('doc')]})
        s.control('inspect-sweep','process','inspectArtifacts',{'inspectionId':'inspect-'+variant,'artifacts':result('sweep','/data/hostObservation/generatedOutputs')})
        s.after(['policies','documents','tombstones','deletion_log','obligations','assignments','audit'])
        # The sweep's authenticated reviewer is read from the server-written audit row, not from the
        # harness provenance that only echoes the identity the driver was asked to sign as.
        s.raw('sweep-authenticated-reviewer','audit',[[alias('config'),alias('ORG-A'),'retention-v1','sweep-'+variant]],'retention-policy','relationSet',field=['actorId','organizationId','policyVersion','sweepId'],where={'action':'retentionSweep'},
              explain='서버 감사 원행에서 보존 sweep 1건의 실행 주체는 인증된 config, 조직은 ORG-A, 정책은 retention-v1, sweep ID는 sweep-'+variant+'다. harness가 서명을 요청한 provenance를 읽지 않는다(계획 §7.4).')
        obs='legal-hold-delete' if variant=='legal-hold' else 'unresolved-reference-delete' if variant=='active-reference' else 'retention-policy'
        s.raw('different-artifact-policies','policies',[[x['artifactType'],x['retentionDays'],x['basis'],x['effectiveDate'],'REDACT' if x['artifactType']!='document' else 'TOMBSTONE_AND_BLOB_DELETE',alias('config')] for x in policy],'retention-policy','relationSet',field=['artifactType','retentionDays','basis','effectiveDate','deletionMethod','reviewerId'])
        if variant in ['legal-hold','active-reference','R6-unconfirmed']:
            for table in ['documents','tombstones','deletion_log','obligations','assignments']:s.unchanged(table,obs)
            s.ass('blob-preserved','inspect-sweep','/data/hostObservation/extractor/rawRows/blobs',[[alias('doc'),p['sha256'],'PRESENT']],obs,'relationSet',field=['documentId','sha256','state'])
        else:
            s.raw('tombstone-one','tombstones',1,'retention-policy','count')
            s.raw('delete-reconciliation','deletion_log',[[alias('doc'),p['sha256'],'DELETED','retention-v1',alias('config')]],'retention-policy','relationSet',field=['documentId','originalHash','blobState','policyVersion','reviewerId'])
            s.ass('actual-blob-deleted','inspect-sweep','/data/hostObservation/extractor/rawRows/blobs',[[alias('doc'),'NOT_PRESENT']],'retention-policy','relationSet',field=['documentId','state'])
        if variant=='restore-deleted':
            s.control('restore','process','restore',{'backupId':'old-backup','restoreEnvironmentId':'restored-isolated','snapshotId':result('view-before','/response/snapshotRevision'),'artifacts':result('backup','/data/hostObservation/generatedOutputs'),'deletionHistoryArtifacts':result('sweep','/data/hostObservation/generatedOutputs'),'applyDeletionHistoryBeforeWriteOpen':True})
            s.control('inspect-restore','process','inspectArtifacts',{'inspectionId':'inspect-restored','artifacts':result('restore','/data/hostObservation/generatedOutputs')})
            s.ass('restored-blob-not-resurrected','inspect-restore','/data/hostObservation/extractor/rawRows/blobs',[[alias('doc'),'NOT_PRESENT']],'retention-policy','relationSet',field=['documentId','state'])
            s.ass('backup-components','inspect-restore','/data/hostObservation/extractor/rawRows/restoredComponents',['DB','EVIDENCE_BLOB','DEFINITION','CAPABILITY','EVALUATOR','SKILL','CONFIG','DEPLOYMENT_MANIFEST'],'retention-policy','exactSet',field='kind')
            s.ass('restored-tombstone','inspect-restore','/data/hostObservation/extractor/rawRows/tombstones',[alias('doc')],'retention-policy','exactSet',field='documentId')
            s.ass('restore-deletion-policy','inspect-restore','/data/hostObservation/extractor/rawRows/deletion_log',[['retention-v1','DELETED']],'retention-policy','relationSet',field=['policyVersion','blobState'])
        s.finish()
    o='T24.secret-redaction'
    s=c.sub('sentinel-artifact-scan','가상비밀을queue·오류·log·감사·근거·backup에서 전체byte로 검사한다',o,{'sentinelIsSyntheticNotCredential':True,'artifactLayout':[{'artifactType':t,'scopeMode':'ENTIRE_ISOLATED_CASE','completeSourceFiles':1} for t in ['queue','error','log','audit','evidence','backupManifest']],'secretManagerRestore':{'procedure':'external secret-manager restore','storeSecretsInBundle':False}})
    s.before();s.invoke('request-with-sentinel','recordActivity',request={'syntheticAuthenticationMetadata':{'token':'SYNTHETIC-DO-NOT-LOG'},'source':{'namespace':'untrusted','rawPrivateValue':'SYNTHETIC-DO-NOT-LOG'},'targetId':alias('A60'),'forceValidationError':'TYPE_INVALID'})
    s.query('query-audit','getObject','readAgent');s.control('backup','process','backup',{'environmentId':'redaction-isolated','backupId':'redaction-backup','snapshotId':result('query-audit','/response/snapshotRevision')})
    s.control('collect-artifacts','process','dataInventory',{'environmentId':'redaction-isolated','inventoryId':'redaction-artifacts','authoritativeSourceId':'actual-runtime-and-backup','artifactTypes':['queue','error','log','audit','evidence','backupManifest'],'scopeMode':'ENTIRE_ISOLATED_CASE','completeSourceFilesPerType':1,'backupArtifacts':result('backup','/data/hostObservation/generatedOutputs')})
    s.control('inventory','process','inspectArtifacts',{'inspectionId':'redaction-inventory','artifacts':result('collect-artifacts','/data/hostObservation/extractor/rawRows/artifacts')})
    s.control('scan','process','scanArtifacts',{'scanId':'full-redaction-scan','artifacts':result('inventory','/data/hostObservation/observedArtifacts'),'patterns':[{'id':'synthetic-secret','literal':'SYNTHETIC-DO-NOT-LOG'}]});s.after()
    s.ass('scan-six-surfaces','inventory','/data/hostObservation/extractor/rawRows/artifactInventory',['queue','error','log','audit','evidence','backupManifest'],'secret-sentinel-leak','exactSet',field='artifactType')
    s.ass('all-files-scanned','scan','/data/hostObservation/reads',6,'secret-sentinel-leak','count')
    for i in range(6):s.ass('surface-'+str(i)+'-no-sentinel','scan',f'/data/hostObservation/reads/{i}/findings',0,'secret-sentinel-leak','count')
    for t in ['movements','allocations','segments','domain_records','outbox']:s.unchanged(t,'redaction-purpose')
    s.raw('query-audit-exists','audit',1,'redaction-purpose','count',where={'kind':'QUERY','actorId':alias('readAgent')})
    s.raw('necessary-refs-preserved','audit',[[alias('readAgent'),alias('P'),'getObject','READ']],'redaction-purpose','relationSet',field=['actorId','targetId','action','result'],where={'kind':'QUERY'})
    s.ass('separate-secret-restore','inventory','/data/hostObservation/extractor/rawRows/backupConfiguration/secretRestoreProcedure','external secret-manager restore','redaction-purpose')
    s.ass('no-secret-config-content','inventory','/data/hostObservation/extractor/rawRows/backupConfiguration/credentials',True,'redaction-purpose','absent')
    s.finish();c.finish();return c

if __name__ == '__main__':
    cases=[build_t06(),build_t22(),build_t24()]
    for c in cases:
        observed={(a['oracleRef']['oracleId'],n) for s in c.subs for a in s.data['assertions'] for n in a['oracleRef']['observationNames']}
        print(c.cid,'subcases',len(c.subs),'assertions',sum(len(s.data['assertions']) for s in c.subs),'observations',len(observed))
