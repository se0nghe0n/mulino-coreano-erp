#!/usr/bin/env python3
"""Deterministic synthetic C4/T17 inputs, never reads or manufactures results."""
import copy,json,pathlib,re,uuid
D=pathlib.Path(__file__).resolve().parent
T='2026-10-07T09:00:02Z'; N='2026-10-08T09:00:00Z'
import sys;sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent));import subjects
def write(name,obj): (D/name).write_text(json.dumps(subjects.declare(obj),ensure_ascii=False,indent=2)+'\n')
def declare_capabilities(definition):
 # A verb without its pinned capability makes the whole definition non-VALID
 # (plan §8); the product then HOLDs every goal pinned to it.
 known={c['capabilityId'] for c in definition['capabilities']}
 for verb in definition['verbs']:
  if verb['capabilityId'] not in known:
   definition['capabilities'].append({'capabilityId':verb['capabilityId'],'semanticVersion':'1.0.0','evaluatorVersion':'core-v1','inputSchemaVersion':'1.0.0','outputSchemaVersion':'1.0.0','supportedWorkMigration':[]});known.add(verb['capabilityId'])
def cmd(i,cap,slots,rev=0,bind=None,intent='COMMAND',outcome='APPLIED',actor=None,assertions=None):
 a=dict(id=i,type='command',capability=cap,request=dict(intentKind=intent,definitionVersion='definition-v1',capabilityId=cap,expectedRevision=rev,commandIdempotencyKey=i,slots=slots,provenance={},subjectRefs=[dict(type='Work',id='$SALES_WORK')]),outcome=outcome)
 if bind:a['bind']=bind
 if actor:a['actor']=actor
 if assertions:a['assertions']=assertions
 return a
def orig(i,kind,content,physical,q,subject='WORK',subjectid='$SALES_WORK',place='$CUSTOMER_PLACE',known=T):
 return dict(id=i,type='original',fixture=dict(synthetic=True,clock=dict(asOf=content.get('occurredAt',T),knownAt=known),sourceProfile=dict(namespace='native-c4-'+i,policyVersion='fixture-v1',nextAction='가상 원본과 실제 범위 대조',nextCheckAt=N),occurrence=dict(kind=kind,content=content,externalEventId=i,quantity=q,unit='BOX' if q is not None else None,assertion='authored synthetic external original')),binding=dict(organizationId='$ORG',workId='$SALES_WORK',itemId='$P',actorId='$reader',supervisorId='$supervisor',physicalScopeId=physical,subjectId=subjectid,subjectKind=subject,placeId=place))
def link(i,physical,q,revision=1,version='1'):
 return [cmd(i+'-match','matchSourceIdentity',dict(claimId='$'+i+'.claim',basisDocumentId='$'+i+'.document',physicalScopeId=physical,policyVersion='fixture-v1',sourceIdentity='native-c4-'+i+':'+i+':'+version,quantity=q,unit='BOX',effectiveFrom=T,reason='원본 정체성과 물량 대조'),rev=revision,bind={i+'.reconciliation':'/id'}),cmd(i+'-link','linkCanonicalOccurrence',dict(reconciliationId='$'+i+'.reconciliation'),rev=revision,bind={i+'.canonical':'/id'})]
def obs(i,assertions,bindRows=None):
 a=dict(id=i,type='observe',assertions=assertions)
 if bindRows:a['bindRows']=bindRows
 return a
def rows(table,where,n):return dict(pointer='/rawRows/'+table,operator='matchingRows',where=where,expected=n)
def total(table,col,q,where=None):
 a=dict(pointer='/rawRows/'+table,operator='sum',column=col,expected=q)
 if where is not None:a['where']=where
 return a
def duties(*k):return dict(pointer='/rawRows/mulino_work_read_obligationreferences',operator='humanDuties',kinds=list(k))
def fixture():
 f=json.loads((D/'fixture.json').read_text());f['id']='c4-synthetic-setup'
 for who,a in f['actors'].items():
  if who=='outsider':continue
  # correctEvidence with documentId also records a claim, which EvidenceRecords
  # authorizes as attachEvidence on the cited original document.
  for cap in ['correctEvidence','recordActivity','assessGoal','attachEvidence']:
   if cap not in a['roleCapabilities']:a['roleCapabilities'].append(cap)
   if cap not in a['grant']['actions']:a['grant']['actions'].append(cap)
 f['aliases']['CPOL']['content']['rules']['correctEvidence']={'effectClass':'RECORD'}
 for cap in ['correctEvidence','recordActivity']:
  if not any(x['name']==cap for x in f['aliases']['DEF']['content']['verbs']):f['aliases']['DEF']['content']['verbs'].append(dict(name=cap,intentKind='RECORD',capabilityId=cap,stage='DRAFT',slots={}))
 # Regulator originals below are renamed native-c4-*; the policies must name them.
 f['aliases']['REGPOL']['sourceNamespace']='native-c4-regulator'
 f['aliases']['REGDISPATCH']['sourceNamespace']='native-c4-dispatch-regulator'
 declare_capabilities(f['aliases']['DEF']['content'])
 return f
# Preserve E1's public input choreography, retain only receipt100 and its own
# authoritative SELL/DISPATCH evidence. Do not copy any E1 expected results.
up=json.loads((D/'e1-upstream.json').read_text())['actions']; dispatch=json.loads((D/'e1-dispatch-authority.json').read_text())['actions']
selected=[]
for a in up:
 i=a['id']
 # receipt40 and its QC hold40 belong to E1 only; C4 keeps the receipt100 path.
 if a['type'] in ['observe','query','parallel'] or i.startswith(('unidentified','sourceB','receipt40','s40','e1-qc-hold40','RANGE40','RANGE5','other-actor','assess-')):continue
 if a['type']=='uuid' and i!='RANGE60' and i!='shipment-physical-range':continue
 if i=='receipt60-confirm':a=copy.deepcopy(a);a['type']='command'
 selected.append(a)
selected+=dispatch
# Every original/action source identity is fresh. Replacing literals is input-only.
text=json.dumps(selected).replace('native-s4','native-c4').replace('"60"','"100"').replace('"30"','"100"')
for value in set(re.findall(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',text)):
 text=text.replace(value,str(uuid.uuid5(uuid.NAMESPACE_URL,'c4:'+value)))
selected=json.loads(text)
ids=[a['id'] for a in selected]
# Prefix captured action IDs and their bindings/references while preserving domain aliases.
for i in sorted(ids,key=len,reverse=True):
 text=text.replace('"'+i+'"','"c4-'+i+'"').replace('$'+i+'.','$c4-'+i+'.').replace('"'+i+'.','"c4-'+i+'.')
selected=json.loads(text)
# Only action IDs are prefixed; uuid domain aliases such as RANGE60 stay bound
# under the name every receipt reference reads.
for a in selected:
 if a['type']=='uuid':a['alias']=a['alias'][3:] if a['alias'].startswith('c4-') else a['alias']
for a in selected:
 a.pop('assertions',None)
 if a['type']=='command':a['request']['commandIdempotencyKey']=a['id']
# regex replacement must also update original externalEventId/sourceIdentity.
for a in selected:
 if a['type']=='original':
  # A source decision UUID is already fresh (uuid5 'c4:'); quality gateways
  # require externalEventId to equal that decision id, so only names get prefixed.
  old=a['fixture']['occurrence']['externalEventId'];a['fixture']['occurrence']['externalEventId']=old if old.startswith('c4-') or re.fullmatch(r'[0-9a-f-]{36}',old) else 'c4-'+old
# sourceIdentity external part got prefixed by exact strings only if whole;
# normalize from each original rather than infer a verified result.
originals={a['id']:a for a in selected if a['type']=='original'}
for a in selected:
 if a.get('capability')=='matchSourceIdentity':
  ref=a['request']['slots']['claimId'][1:].rsplit('.',1)[0];o=originals[ref]
  a['request']['slots']['sourceIdentity']=o['fixture']['sourceProfile']['namespace']+':'+o['fixture']['occurrence']['externalEventId']+':1'
write('c4-fixture.json',fixture());write('c4-upstream.json',dict(schemaVersion='1.0.0',status='NOT_RUN',actions=selected))
base=copy.deepcopy(json.loads((D/'e1-sales-return.json').read_text())['actions'])
# Sales/dispatch/delivery100; explicit consumed allocation assertion before delivery.
cut=next(i for i,a in enumerate(base) if a['id']=='e1-delivery-revision')
base=base[:cut]
s=json.dumps(base).replace('e1-','c4-').replace('native-s4','native-c4').replace('"30"','"100"').replace('$receipt60.','$c4-receipt60.')
base=json.loads(s)
for a in base:
 if a['type']=='command':a['request']['commandIdempotencyKey']=a['id']
idx=next(i for i,a in enumerate(base) if a['id']=='c4-dispatch30')+1
base.insert(idx,obs('c4-consumed-before-real-delivery',[rows('mulino_inventory_segmentallocations',dict(id='$ALLOCATION',state='CONSUMED'),1)]))
# Shipment cargo alias is replaced by public dispatch cargo in base.
write('c4-delivery100.json',dict(schemaVersion='1.0.0',status='NOT_RUN',actions=base))
a=[obs('c4-history100-before-return',[total('mulino_trade_sales_deliveries','quantity','100'),total('mulino_inventory_quantitysegments','quantity','100',dict(retiredat=None,placeid='$CUSTOMER_PLACE'))],{'DELIVERY_REV':dict(pointer='/rawRows/mulino_trade_sales_deliveries',where=dict(id='$DELIVERY'),column='revision'),'SALES_REV':dict(pointer='/rawRows/mulino_work_read_works',where=dict(id='$SALES_WORK'),column='revision')})]
a+=[cmd('c4-assess-delivery100','assessGoal',dict(workId='$SALES_WORK'),rev='$SALES_REV',bind={'PAST_ASSESSMENT':'/effects/assessmentId'},assertions=[dict(pointer='/assessment/outcome',operator='equals',expected='SATISFIED')]),cmd('c4-return-authorize20','authorizeReturn',dict(deliveryId='$DELIVERY',startQuantity='0',quantity='20',unit='BOX',destinationId='$W',validUntil='2026-10-31T00:00:00Z',reason='실제 반품20은 과거 인도와 별도 사건이다'),rev='$DELIVERY_REV',actor='supervisor',bind={'RETURN_AUTHORIZATION':'/effects/authorizationId'}),dict(id='c4-return-event',type='uuid',alias='RETURN_EVENT'),cmd('c4-return-intake20','receiveReturn',dict(authorizationId='$RETURN_AUTHORIZATION',eventId='$RETURN_EVENT',occurredAt=T,nextCheckAt=N),intent='RECORD',bind={'RETURN':'/effects/returnId'})]
r=dict(returnId='$RETURN',kind='RETURN_RECEIPT',eventId='$RETURN_EVENT',deliveryId='$DELIVERY',customerId='$C',itemId='$P',lotId='$L',rangeRootId='$DISPATCH_RANGE',startQuantity='0',quantity='20',unit='BOX',placeId='$W',workId='$SALES_WORK',occurredAt=T)
a+=[orig('c4-return-original','RETURN_RECEIPT',r,'$RETURN','20',subject='RETURN',subjectid='$RETURN',place='$W')]+link('c4-return-original','$RETURN','20')+[cmd('c4-return-confirm20','receiveReturn',dict(returnId='$RETURN',canonicalOccurrenceId='$c4-return-original.canonical',nextCheckAt=N),intent='RECORD'),obs('c4-return-distinct-history',[total('mulino_trade_sales_deliveries','quantity','100'),total('mulino_trade_returns_receipts','quantity','20'),total('mulino_inventory_quantitysegments','quantity','20',dict(retiredat=None,placeid='$W')),total('mulino_inventory_quantitysegments','quantity','80',dict(retiredat=None,placeid='$CUSTOMER_PLACE'))])]
# Correct immutable physical delivery event through the public command; original
# importer supplies bytes/document only, never a corrected canonical/effect.
content=next(x['fixture']['occurrence']['content'] for x in base if x['type']=='original')
# EvidenceReconciliation requires a delivery correction to keep the original
# event's source profile and subject: it is version 2 of that source event.
delivered=next(x for x in base if x['type']=='original' and x['id']=='c4-delivery-original')
for q,known,prev,rev in [('98','2026-10-07T09:00:03Z','$c4-delivery-original.event',1)]:
 i='c4-correction'+q;c=copy.deepcopy(content);c['quantity']=q;c['correctionOfDeliveryId']='$DELIVERY'
 a += [dict(id=i+'-clock',type='clock',instant=known),orig(i,'PHYSICAL_DELIVERY',c,'$DELIVERY_OBSERVATION',q,subject='DISPATCH',subjectid='$DISPATCH',known=known)]
 payload=json.dumps(c,separators=(',',':'))
 payload=re.sub(r'"\$([^" ]+)"',lambda m:'"${'+m.group(1)+'}"',payload)
 a += [cmd(i+'-event','correctEvidence',dict(subject=dict(kind='DISPATCH',id='$DISPATCH'),kind='PHYSICAL_DELIVERY',sourceNamespace=delivered['fixture']['sourceProfile']['namespace'],externalEventId=delivered['fixture']['occurrence']['externalEventId'],sourceVersion='2',effectiveFrom=T,timeZone='UTC',timePrecision='SECOND',valueState='KNOWN',payload=payload,supersedesId=prev,documentId='$'+i+'.document',assertion='명시적 실제 인도 정정',quantity=q,unit='BOX',evidenceType='EVENT'),rev=rev,intent='RECORD',bind={i+'.event':'/id',i+'.claim':'/claimId'})]+link(i,'$DELIVERY_OBSERVATION',q,version='2')
 # A correction keeps the original event's subject (the dispatch).
 a[-3]['request']['subjectRefs']=[{'type':'Dispatch','id':'$DISPATCH'}]
 # The corrected event is version 2 of the same source event; match that key.
 a[-2]['request']['slots']['sourceIdentity']=delivered['fixture']['sourceProfile']['namespace']+':'+delivered['fixture']['occurrence']['externalEventId']+':2'
a += [obs('c4-correct98-independent',[rows('mulino_evidence_events',dict(id='$c4-delivery-original.event'),1),rows('mulino_evidence_events',dict(id='$c4-correction98.event',supersedesid='$c4-delivery-original.event'),1),rows('mulino_work_read_assessmentreferences',dict(id='$PAST_ASSESSMENT',outcome='SATISFIED'),1),total('mulino_trade_sales_deliveries','quantity','100'),total('mulino_trade_sales_deliverycorrections','quantity','98'),total('mulino_trade_returns_receipts','quantity','20'),total('mulino_inventory_quantitysegments','quantity','20',dict(retiredat=None,placeid='$W')),total('mulino_inventory_quantitysegments','quantity','80',dict(retiredat=None,placeid='$CUSTOMER_PLACE')),total('mulino_inventory_quantitysegments','quantity','0',dict(retiredat=None,placeid='$TRANSIT')),total('mulino_work_read_obligationreferences','quantity','2',dict(kind='DELIVERY_CORRECTED_DEFICIT',status='OPEN',valid=True)),duties('DELIVERY_CORRECTED_DEFICIT')])]
write('c4-history-return-correction.json',dict(schemaVersion='1.0.0',status='NOT_RUN',actions=a))
write('c4-flow.json',dict(schemaVersion='1.0.0',status='NOT_RUN',requiredCases=['C4','T17_CONSUMED'],fullCaseCoverageClaimed=False,actions=[dict(id='c4-setup',type='setup',fixtureRef='verification/actual/s4/c4-fixture.json',organizationAlias='ORG')]+[dict(id='c4-include-'+x,type='include',scriptRef='verification/actual/s4/'+x+'.json') for x in ['c4-upstream','c4-delivery100','c4-history-return-correction']]))
