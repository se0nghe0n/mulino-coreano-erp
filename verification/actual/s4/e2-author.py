#!/usr/bin/env python3
"""Author E2 source inputs only. Does not read runtime effects or seed facts."""
import copy,json,pathlib,uuid
OUT=pathlib.Path(__file__).resolve().parent
T='2026-10-07T09:00:02Z'; NEXT='2026-10-08T09:00:00Z'; UNTIL='2026-10-31T00:00:00Z'
import sys;sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent));import subjects
def load(name):return json.loads((OUT/name).read_text())
def write(name,obj):(OUT/name).write_text(json.dumps(subjects.declare(obj),ensure_ascii=False,indent=2)+'\n')
def declare_capabilities(definition):
 # A verb without its pinned capability makes the whole definition non-VALID
 # (plan §8); the product then HOLDs every goal pinned to it.
 known={c['capabilityId'] for c in definition['capabilities']}
 for verb in definition['verbs']:
  if verb['capabilityId'] not in known:
   definition['capabilities'].append({'capabilityId':verb['capabilityId'],'semanticVersion':'1.0.0','evaluatorVersion':'core-v1','inputSchemaVersion':'1.0.0','outputSchemaVersion':'1.0.0','supportedWorkMigration':[]});known.add(verb['capabilityId'])
def cmd(name,cap,slots,bind=None,rev=0,actor=None,outcome='APPLIED',assertions=None,intent=None):
 a={'id':name,'type':'command','capability':cap,'outcome':outcome,'request':{'intentKind':intent or ('RECORD' if cap in ['recordRecovery','recordRecallNotice','recordDispositionBasis'] else 'COMMAND'),'definitionVersion':'definition-v1','capabilityId':cap,'expectedRevision':rev,'commandIdempotencyKey':name,'slots':slots,'provenance':{},'subjectRefs':[{'type':'Work','id':'$WORK'}]}}
 if bind:a['bind']=bind
 if actor:a['actor']=actor
 if assertions:a['assertions']=assertions
 return a
def eq(pointer,expected):return {'pointer':pointer,'operator':'equals','expected':expected}
def rows(table,where,count):return {'pointer':'/rawRows/'+table,'operator':'matchingRows','where':where,'expected':count}
def total(table,column,quantity,where=None):
 a={'pointer':'/rawRows/'+table,'operator':'sum','column':column,'expected':quantity}
 if where:a['where']=where
 return a
def obs(name,assertions=None,bindRows=None):
 a={'id':name,'type':'observe','assertions':assertions or []}
 if bindRows:a['bindRows']=bindRows
 return a
def revision(name,table,alias,key):return obs(name,bindRows={alias:{'pointer':'/rawRows/'+table,'where':{'id':key},'column':'revision'}})
def orig(name,kind,content,scope,q,subject='RECALL_SCOPE'):
 return {'id':name,'type':'original','fixture':{'synthetic':True,'clock':{'asOf':T,'knownAt':T},'sourceProfile':{'namespace':'native-s4-'+name,'policyVersion':'fixture-v1','nextAction':'회수 범위와 잔여 책임 대조','nextCheckAt':NEXT},'occurrence':{'kind':kind,'content':content,'externalEventId':name,'quantity':q,'unit':'BOX','assertion':'authored synthetic external original'}},'binding':{'organizationId':'$ORG','workId':'$WORK','itemId':'$P','actorId':'$reader','supervisorId':'$supervisor','physicalScopeId':scope,'subjectId':scope,'subjectKind':subject,'placeId':'$W'}}
def link(name,scope,q):return [cmd(name+'-match','matchSourceIdentity',{'claimId':'$'+name+'.claim','basisDocumentId':'$'+name+'.document','physicalScopeId':scope,'policyVersion':'fixture-v1','sourceIdentity':'native-s4-'+name+':'+name+':1','quantity':q,'unit':'BOX','effectiveFrom':T,'reason':'원본과 동일 물리 범위 대조'},bind={name+'.reconciliation':'/id'},rev=1),cmd(name+'-link','linkCanonicalOccurrence',{'reconciliationId':'$'+name+'.reconciliation'},bind={name+'.canonical':'/id'},rev=1)]
fixture=load('fixture.json');fixture['fixtureId']='s4-e2-independent-original-inputs';fixture['baseline']=[];fixture['evidence']=[];fixture['responsibilities']=[]
classes={c:'RECALL' for c in ['openInvestigation','proposeRecall','approveRecall','recordRecallNotice','recordRecovery','closeRecall']}
for actor in fixture['actors'].values():
 if actor['subject'].endswith('outsider'):continue
 for cap in classes:
  if cap not in actor['roleCapabilities']:actor['roleCapabilities'].append(cap)
  if cap not in actor['grant']['actions']:actor['grant']['actions'].append(cap)
for cap,cls in classes.items():
 fixture['aliases']['CPOL']['content']['rules'][cap]={'effectClass':cls}
 if cap not in {v['capabilityId'] for v in fixture['aliases']['DEF']['content']['verbs']}:fixture['aliases']['DEF']['content']['verbs'].append({'name':cap,'intentKind':'RECORD' if cap.startswith('record') else 'COMMAND','capabilityId':cap,'stage':'DRAFT','slots':{}})
fixture['aliases']['RECALL_ADMIN']={'type':'ManagementAuthority','actorAlias':'supervisor','capabilityId':'approveRecall','validFrom':'2026-10-01T00:00:00Z','validUntil':UNTIL}
# Fresh organization, same coherent LOT/item/place aliases: no stock/result seed.
# The regulatory gateway binds each original to its policy's sourceNamespace;
# E2's own SELL/DISPATCH regulator originals use these namespaces below.
fixture['aliases']['REGPOL']['sourceNamespace']='native-s4-e2-sell-regulator'
fixture['aliases']['REGDISPATCH']['sourceNamespace']='native-s4-e2-dispatch-regulator'
# Recall evidence originals are claimed against the RecallScope noun.
for noun in ['Recall','RecallScope']:
 if noun not in {n['name'] for n in fixture['aliases']['DEF']['content']['nouns']}:fixture['aliases']['DEF']['content']['nouns'].append({'name':noun,'core':True})
declare_capabilities(fixture['aliases']['DEF']['content'])
write('e2-fixture.json',fixture)
a=[{'id':'e2-setup','type':'setup','fixtureRef':'verification/actual/s4/e2-fixture.json','organizationAlias':'ORG'}]
up=load('e1-upstream.json')['actions']; selected=[]
ids=['create-work','proposal100','approve100','dispatch100','supplier-accept','supplier-accept-match','supplier-accept-link','supplier-accepted','RANGE60','receipt60','receipt60-provisional','receipt60-match','receipt60-link','receipt60-confirm']
# Explicitly reuse only authored public upstream fragments, rewritten to purchase60.
for old in ids:
 x=copy.deepcopy(next(v for v in up if v['id']==old));x['type']='command' if x['type']=='lost-response' else x['type'];selected.append(x)
# Make every original/action key independent, preserving dynamic alias linkage.
rename={x['id']:'e2-'+x['id'] for x in selected}
def transform(node):
 if isinstance(node,str):
  if node=='100':return '60'
  for old,new in sorted(rename.items(),key=lambda p:-len(p[0])):node=node.replace(old,new)
  return node
 if isinstance(node,list):return [transform(x) for x in node]
 if isinstance(node,dict):return {transform(k):transform(v) for k,v in node.items()}
 return node
selected=transform(selected);a+=selected
SEG='$e2-receipt60.segment'
# Conditions needed to reserve/pick60 before either independent hold is active.
for action in ['SELL','DISPATCH']:
 for cat in ['QC','CUSTOMER','COMMERCIAL']:
  old='s60-'+cat;fragment=[copy.deepcopy(x) for x in up if x['id'] in [old,old+'-match',old+'-link',old+'-decision']]
  text=json.dumps(fragment).replace('s60-'+cat,'e2-'+action.lower()+'-'+cat).replace('$receipt60.segment',SEG).replace('"SELL"','"'+action+'"')
  # SELL and DISPATCH bases are distinct source decisions; one decision id
  # with two contents is an evidence CONFLICT at the quality gateway.
  decision=[x for x in fragment if x['type']=='original'][0]['fixture']['occurrence']['content']['sourceDecisionId']
  text=text.replace(decision,str(uuid.uuid5(uuid.NAMESPACE_URL,'e2-'+action+':'+decision)))
  fragment=json.loads(text)
  for x in fragment:
   if x.get('capability')=='recordDispositionBasis':x['request']['intentKind']='RECORD'
  a+=fragment
 # Typed regulatory sources and document submission/label decisions are public.
 reg=[copy.deepcopy(x) for x in up if x['id'].startswith('reg-') or x['id'] in ['submit-reg','allow30','label-verified']]
 reg=[x for x in reg if x['type']!='observe']
 text=json.dumps(reg).replace('reg-','e2-'+action.lower()+'-reg-').replace('native-s4-regulator','native-s4-e2-'+action.lower()+'-regulator').replace('$receipt60.segment',SEG).replace('"SELL"','"'+action+'"').replace('"30"','"60"').replace('$REGPOL','$REGDISPATCH' if action=='DISPATCH' else '$REGPOL').replace('$PROC','$E2_'+action+'_PROC').replace('"PROC','"E2_'+action+'_PROC').replace('"submit-reg"','"e2-'+action.lower()+'-submit-reg"').replace('"allow30"','"e2-'+action.lower()+'-allow60"').replace('"label-verified"','"e2-'+action.lower()+'-label-verified"')
 a+=json.loads(text)
sales=load('e1-sales-return.json')['actions']
for old in ['e1-create-sales-work','e1-create-sales','e1-segment-revision','e1-reserve30','e1-pick30']:
 x=copy.deepcopy(next(v for v in sales if v['id']==old));x=json.loads(json.dumps(x).replace('e1-','e2-').replace('$receipt60.segment',SEG).replace('"30"','"60"'));a.append(x)
# QC hold20 is ALL: disposal must separately release it before recall disposal.
name='e2-qc20';decision=str(uuid.uuid5(uuid.NAMESPACE_URL,name))
content={'segmentId':SEG,'operation':'placeHold','action':'ALL','category':'QC','startQuantity':'0','quantity':'20','unit':'BOX','validFrom':T,'validUntil':UNTIL,'workId':'$WORK','sourceDecisionId':decision,'sourceVersion':'1'}
x=orig(name,'QUALITY_DECISION_placeHold_QC',content,SEG,'20','SEGMENT');x['fixture']['occurrence']['externalEventId']=decision;a.append(x)
ls=link(name,SEG,'20');ls[0]['request']['slots']['sourceIdentity']='native-s4-'+name+':'+decision+':1';a+=ls
a+=[revision('e2-pre-hold-segment','mulino_inventory_quantitysegments','E2_SEG_REV',SEG),cmd('e2-place-qc20','placeHold',{k:v for k,v in content.items() if k not in ['operation','sourceDecisionId','sourceVersion']}|{'evidenceId':'$'+name+'.document','reason':'독립 QC20 보류','nextCheckAt':NEXT},bind={'E2_QC':'/effects/restrictionId','E2_QC_REV':'/revision'},rev='$E2_SEG_REV')]
basis=orig('e2-investigation-basis','TEMPERATURE_ANOMALY',{'itemId':'$P','lotId':'$L','quantity':'60','unit':'BOX','candidateOnly':True},SEG,'60','SEGMENT');a.append(basis)
a+=[revision('e2-pre-investigate','mulino_inventory_quantitysegments','E2_SEG_REV',SEG),cmd('e2-open-investigation','openInvestigation',{'segmentId':SEG,'itemId':'$P','lotId':'$L','workId':'$WORK','startQuantity':'0','quantity':'60','unit':'BOX','basisEvidenceId':'$e2-investigation-basis.event','occurredAt':T,'nextAction':'미확인 물량과 조사 후보 확인','nextCheckAt':NEXT},bind={'E2_INV':'/effects/investigationId'},rev='$E2_SEG_REV',assertions=[eq('/effects/impactState','CANDIDATE')])]
# QualityEvidence.require accepts a release document only after a VERIFIED
# canonical of the exact segment; the generic segment identity compares the
# whole physical quantity60, not the released hold20.
rel=orig('e2-qc-release','QUALITY_RELEASE',{'segmentId':SEG,'operation':'releaseHold','restrictionId':'$E2_QC'},SEG,'60','SEGMENT');a.append(rel);a+=link('e2-qc-release',SEG,'60')
a+=[cmd('e2-release-qc20','releaseHold',{'restrictionId':'$E2_QC','evidenceId':'$e2-qc-release.document','reason':'현재 QC20 해제 근거'},rev='$E2_QC_REV'),revision('e2-allocation-after-holds','mulino_inventory_segmentallocations','E2_ALLOC_REV','$ALLOCATION'),cmd('e2-dispatch-denied-after-qc-release','dispatchQuantity',{'allocationId':'$ALLOCATION','transitPlaceId':'$TRANSIT','occurredAt':T,'evidenceRef':'synthetic-e2-denied'},rev='$E2_ALLOC_REV',outcome='REJECTED',assertions=[{'pointer':'/error/code','operator':'equals','expected':'INSUFFICIENT_ELIGIBLE_QUANTITY'},{'pointer':'/effects','operator':'equals','expected':{}}]),obs('e2-independent-holds',[rows('mulino_inventory_restrictions',{'category':'QC','state':'ACTIVE'},0),rows('mulino_inventory_restrictions',{'category':'RECALL_INVESTIGATION','state':'ACTIVE','quantity':'60'},3),rows('mulino_inventory_dispatches',{},0),rows('mulino_trade_recall_investigations',{'id':'$E2_INV','impactstate':'CANDIDATE'},1)])]
a+=[cmd('e2-propose50','proposeRecall',{'investigationId':'$E2_INV','startQuantity':'0','quantity':'50','unit':'BOX','reason':'조사60 중 ADMIN 회수범위50 제안'},bind={'E2_SCOPE':'/effects/scopeId','E2_HASH':'/effects/scopeHash','E2_VERSION':'/effects/scopeVersion'}),cmd('e2-admin-approve50','approveRecall',{'scopeId':'$E2_SCOPE','scopeHash':'$E2_HASH','decision':'APPROVE','validUntil':UNTIL},actor='supervisor',bind={'E2_APPROVAL':'/effects/approvalId'})]
def physical(name,kind,start,q,source=None,reason=None,partition=None):
 event='RECALL_'+{'RECOVERED':'RECOVERY','DISPOSED':'DISPOSAL'}.get(kind,kind);alias=name+'-actual-event'
 a.append({'id':alias,'type':'uuid','alias':alias})
 payload={'actualEventId':'$'+alias,'scopeId':'$E2_SCOPE','scopeVersion':'$E2_VERSION','scopeHash':'$E2_HASH','rootSegmentId':SEG,'itemId':'$P','lotId':'$L','startQuantity':start,'quantity':q,'unit':'BOX','eventKind':event,'occurredAt':T,'currentPlaceId':'$W'}
 if source:payload['sourceSegmentId']=source
 if reason:payload['reason']=reason
 if partition:payload['partitionHash']=partition
 a.append(orig(name,event,payload,'$E2_SCOPE',q));a.extend(link(name,'$E2_SCOPE',q))
 slots={k:v for k,v in payload.items() if k not in ['scopeVersion','itemId','lotId','eventKind','occurredAt','partitionHash']};slots.update(approvalId='$E2_APPROVAL',canonicalOccurrenceId='$'+name+'.canonical',kind=kind)
 return slots
recover=physical('e2-recover25','RECOVERED','0','25',SEG)
a.append(cmd('e2-record-recover25','recordRecovery',recover,bind={'E2_RECOVERED_LEAF':'/effects/currentSegmentId'},assertions=[eq('/effects/partition/ACCOUNTED','0'),eq('/effects/partition/UNKNOWN','50')]))
a.append(cmd('e2-repeat-recovery-denied','recordRecovery',recover,outcome='CONFLICT',assertions=[eq('/error/code','RECALL_EVENT_ALREADY_APPLIED'),eq('/effects',{})]))
dispose=physical('e2-dispose-same25','DISPOSED','0','25','$E2_RECOVERED_LEAF')
a.append(cmd('e2-record-dispose25','recordRecovery',dispose,bind={'E2_PARTIAL_HASH':'/effects/partitionHash'},assertions=[eq('/effects/partition/RECOVERED','25'),eq('/effects/partition/DISPOSED','25'),eq('/effects/partition/ACCOUNTED','25'),eq('/effects/partition/UNKNOWN','25')]))
a.append(cmd('e2-repeat-disposal-denied','recordRecovery',dispose,outcome='CONFLICT',assertions=[eq('/error/code','RECALL_EVENT_ALREADY_APPLIED'),eq('/effects',{})]))
a.append(cmd('e2-close-unknown25-denied','closeRecall',{'scopeId':'$E2_SCOPE','approvalId':'$E2_APPROVAL','scopeHash':'$E2_HASH','canonicalOccurrenceId':'$e2-dispose-same25.canonical','partitionHash':'$E2_PARTIAL_HASH'},actor='supervisor',outcome='HELD',assertions=[eq('/error/code','RECALL_RESIDUAL_UNKNOWN')]))
a.append(obs('e2-accounted25-not50',[total('mulino_trade_recall_actions','quantity','25',{'scopeid':'$E2_SCOPE','kind':'RECOVERED'}),total('mulino_trade_recall_actions','quantity','25',{'scopeid':'$E2_SCOPE','kind':'DISPOSED'}),rows('mulino_trade_recall_closures',{},0),total('mulino_inventory_quantitysegments','quantity','35',{'retiredat':None,'placeid':'$W'})]))
# Replace same-scope approval to prove old approval cannot cause another effect.
a.append(cmd('e2-admin-reapprove50','approveRecall',{'scopeId':'$E2_SCOPE','scopeHash':'$E2_HASH','decision':'APPROVE','validUntil':UNTIL},actor='supervisor',bind={'E2_CURRENT_APPROVAL':'/effects/approvalId'}))
exception=physical('e2-exception25','EXCEPTION','25','25',reason='가상 ADMIN 예외25: 미확인 물량 조사 책임 유지')
# A copy: the later current-approval mutation must not rewrite this stale request.
a.append(cmd('e2-stale-approval-exception-denied','recordRecovery',copy.deepcopy(exception),actor='supervisor',outcome='HELD',assertions=[eq('/error/code','RECALL_APPROVAL_STALE')]))
exception['approvalId']='$E2_CURRENT_APPROVAL'
a.append(cmd('e2-record-approved-exception25','recordRecovery',exception,actor='supervisor',bind={'E2_FINAL_HASH':'/effects/partitionHash','E2_RESIDUAL_DUTY':'/effects/residualDutyId'},assertions=[eq('/effects/partition/ACCOUNTED','50'),eq('/effects/partition/EXCEPTION','25'),eq('/effects/partition/UNKNOWN','0')]))
close=physical('e2-closure-original','CLOSURE','0','50',partition='$E2_FINAL_HASH')
a.append(cmd('e2-admin-close-with-residual','closeRecall',{'scopeId':'$E2_SCOPE','approvalId':'$E2_CURRENT_APPROVAL','scopeHash':'$E2_HASH','canonicalOccurrenceId':'$e2-closure-original.canonical','partitionHash':'$E2_FINAL_HASH'},actor='supervisor',assertions=[eq('/effects/residualResponsibilityRetained',True)]))
a.append(obs('e2-final-independent',[rows('mulino_trade_recall_investigations',{'id':'$E2_INV','status':'CLOSED','impactstate':'CANDIDATE'},1),rows('mulino_trade_recall_closures',{'scopeid':'$E2_SCOPE'},1),rows('mulino_trade_recall_actions',{'scopeid':'$E2_SCOPE'},3),total('mulino_trade_recall_actions','quantity','25',{'scopeid':'$E2_SCOPE','kind':'EXCEPTION'}),total('mulino_inventory_quantitysegments','quantity','35',{'retiredat':None,'placeid':'$W'}),{'pointer':'/rawRows/mulino_work_read_obligationreferences','operator':'humanDuties','duties':[{'kind':'RECALL_EXCEPTION_RESIDUAL','where':{'id':'$E2_RESIDUAL_DUTY','workid':'$WORK','scope.residual.scopeId':'$E2_SCOPE','quantity':'25','unit':'BOX'},'count':1},{'kind':'RECALL_EXCLUDED_SCOPE','where':{'workid':'$WORK','scope.residual.scopeId':'$E2_SCOPE','quantity':'10','unit':'BOX'},'count':1}]},rows('mulino_work_read_obligationreferences',{'id':'$E2_RESIDUAL_DUTY','status':'OPEN','valid':True},1)]))
# Allocation commands bind the sales Work the allocation belongs to; a purchase
# Work subject is a TYPE_INVALID mismatch, not the intended HOLD.
for x in a:
 if x.get('capability') in ['pickQuantity','dispatchQuantity','releaseAllocation']:x['request']['subjectRefs']=[{'type':'Work','id':'$SALES_WORK'}]
write('e2-flow.json',{'schemaVersion':'1.0.0','status':'NOT_RUN','requiredCases':['E2'],'fullCaseCoverageClaimed':False,'actions':a})
