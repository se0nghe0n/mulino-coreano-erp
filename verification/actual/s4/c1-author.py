#!/usr/bin/env python3
"""Deterministic synthetic C1 inputs (plan §13.2); never reads or manufactures results.

Custody100 of one identified segment carries QC/CUSTOMER/regulatory SELL and
DISPATCH authority for all100 but a COMMERCIAL consignment basis only for
[0,40). The remaining60 is customer custody only. After a reservation20 the
commercial SELL basis is revoked through the public command; a new
reservation and the dispatch of the existing allocation must both have zero
effect while the suspended allocation and its human review stay open.
"""
import copy,json,pathlib,re,uuid
D=pathlib.Path(__file__).resolve().parent
T='2026-10-07T09:00:02Z';N='2026-10-08T09:00:00Z';UNTIL='2026-10-31T00:00:00Z'
SEG='$c1-receipt60.segment'
import sys;sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent));import subjects
def write(name,obj):(D/name).write_text(json.dumps(subjects.declare(obj),ensure_ascii=False,indent=2)+'\n')
def cmd(i,cap,slots,rev=0,bind=None,intent='COMMAND',outcome='APPLIED',actor=None,assertions=None,refs=None):
 a=dict(id=i,type='command',capability=cap,request=dict(intentKind=intent,definitionVersion='definition-v1',capabilityId=cap,expectedRevision=rev,commandIdempotencyKey=i,slots=slots,provenance={},subjectRefs=refs or [dict(type='Work',id='$SALES_WORK')]),outcome=outcome)
 if bind:a['bind']=bind
 if actor:a['actor']=actor
 if assertions:a['assertions']=assertions
 return a
def eq(pointer,expected):return dict(pointer=pointer,operator='equals',expected=expected)
def rows(table,where,n):return dict(pointer='/rawRows/'+table,operator='matchingRows',where=where,expected=n)
def total(table,col,q,where=None):
 a=dict(pointer='/rawRows/'+table,operator='sum',column=col,expected=q)
 if where is not None:a['where']=where
 return a
def obs(i,assertions=None,bindRows=None):
 a=dict(id=i,type='observe',assertions=assertions or [])
 if bindRows:a['bindRows']=bindRows
 return a
def inventory(i,assertions):return dict(id=i,type='query',capability='getInventory',request=dict(scope=dict(organizationId='$ORG',itemId='$P',placeId='$W'),asOf=T,knownAt=T),assertions=assertions)
# Upstream: E1's public purchase/shipment/receipt choreography reduced to one
# receipt of100 plus its own SELL and DISPATCH authority. No E1 result is copied.
up=json.loads((D/'e1-upstream.json').read_text())['actions'];dispatch=json.loads((D/'e1-dispatch-authority.json').read_text())['actions']
selected=[]
for a in up:
 i=a['id']
 if a['type'] in ['observe','query','parallel'] or i.startswith(('unidentified','sourceB','receipt40','s40','e1-qc-hold40','RANGE40','RANGE5','other-actor','assess-')):continue
 if a['type']=='uuid' and i not in ['RANGE60','shipment-physical-range']:continue
 if i=='receipt60-confirm':a=copy.deepcopy(a);a['type']='command'
 selected.append(a)
selected+=dispatch
text=json.dumps(selected).replace('native-s4','native-c1').replace('"60"','"100"').replace('"30"','"100"')
# C1 reuses fixture.json, whose regulator policies name the native-s4 namespaces;
# the regulatory gateway rejects an original from any other namespace.
text=text.replace('native-c1-regulator','native-s4-regulator').replace('native-c1-dispatch-regulator','native-s4-dispatch-regulator')
for value in set(re.findall(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',text)):
 text=text.replace(value,str(uuid.uuid5(uuid.NAMESPACE_URL,'c1:'+value)))
selected=json.loads(text)
aliases={a['id']:a['alias'] for a in selected if a['type']=='uuid'}
for i in sorted([a['id'] for a in selected],key=len,reverse=True):
 text=text.replace('"'+i+'"','"c1-'+i+'"').replace('$'+i+'.','$c1-'+i+'.').replace('"'+i+'.','"c1-'+i+'.')
selected=json.loads(text)
for a in selected:
 a.pop('assertions',None)
 # Only action IDs are prefixed; uuid domain aliases keep the name references read.
 if a['type']=='uuid':a['alias']=aliases[a['id'][3:]]
 if a['type']=='command':a['request']['commandIdempotencyKey']=a['id']
 if a['type']=='original':
  o=a['fixture']['occurrence'];o['externalEventId']=o['externalEventId'] if o['externalEventId'].startswith('c1-') or re.fullmatch(r'[0-9a-f-]{36}',o['externalEventId']) else 'c1-'+o['externalEventId']
originals={a['id']:a for a in selected if a['type']=='original'}
for a in selected:
 if a.get('capability')=='matchSourceIdentity':
  o=originals[a['request']['slots']['claimId'][1:].rsplit('.',1)[0]]
  a['request']['slots']['sourceIdentity']=o['fixture']['sourceProfile']['namespace']+':'+o['fixture']['occurrence']['externalEventId']+':1'
# Consignment sale permission is the exact interval [0,40) for SELL and DISPATCH;
# custody60 has no commercial basis. Every copy of the range changes together.
commercial=[p for p in ['c1-s60-COMMERCIAL','c1-dispatch-s60-COMMERCIAL']]
for a in selected:
 base=a['id'].rsplit('-',1)[0] if a['id'].endswith(('-match','-link','-decision')) else a['id']
 if base not in commercial:continue
 if a['type']=='original':a['fixture']['occurrence']['content']['quantity']='40';a['fixture']['occurrence']['quantity']='40'
 elif a.get('capability') in ['matchSourceIdentity','recordDispositionBasis']:a['request']['slots']['quantity']='40'
assert sum(1 for a in selected if a['type']=='original' and a['fixture']['occurrence']['quantity']=='40')==2
write('c1-upstream.json',dict(schemaVersion='1.0.0',status='NOT_RUN',actions=selected))
# Sales order40 against the consignment basis; reservation20 then revocation.
sales=json.loads((D/'e1-sales-return.json').read_text())['actions']
work=json.loads(json.dumps(next(x for x in sales if x['id']=='e1-create-sales-work')).replace('e1-','c1-'))
work['request']['slots']['quantity']={'value':'40','unit':'BOX'};work['request']['commandIdempotencyKey']=work['id']
order=json.loads(json.dumps(next(x for x in sales if x['id']=='e1-create-sales')).replace('e1-','c1-'))
order['request']['slots']['quantity']='40';order['request']['commandIdempotencyKey']=order['id']
segment_refs=[dict(type='QuantitySegment',id=SEG),dict(type='SalesOrderLine',id='$SALES_LINE')]
def segment_revision(i):return obs(i,bindRows={'C1_SEG_REV':dict(pointer='/rawRows/mulino_inventory_quantitysegments',where=dict(id=SEG),column='revision')})
s=[inventory('c1-custody100-sale40',[eq('/data/heldQuantity','100'),eq('/data/eligibleQuantity','40'),eq('/data/unreservedEligibleQuantity','40'),eq('/data/reservedQuantity','0')]),
 work,order,segment_revision('c1-segment-revision'),
 # QC/regulatory/app authority over all100 cannot make custody60 saleable.
 cmd('c1-reserve-custody60-denied','reserveQuantity',dict(segmentId=SEG,salesLineId='$SALES_LINE',startQuantity='40',quantity='10',unit='BOX'),rev='$C1_SEG_REV',outcome='REJECTED',refs=segment_refs,assertions=[eq('/error/code','SCOPE_INELIGIBLE'),eq('/effects',{})]),
 cmd('c1-reserve20','reserveQuantity',dict(segmentId=SEG,salesLineId='$SALES_LINE',startQuantity='0',quantity='20',unit='BOX'),rev='$C1_SEG_REV',refs=segment_refs,bind={'ALLOCATION':'/effects/allocationId','ALLOCATION_REV':'/revision'}),
 cmd('c1-pick20','pickQuantity',dict(allocationId='$ALLOCATION'),rev='$ALLOCATION_REV',bind={'ALLOCATION_REV':'/revision'}),
 inventory('c1-reserved20-before-revoke',[eq('/data/eligibleQuantity','40'),eq('/data/reservedQuantity','20'),eq('/data/unreservedEligibleQuantity','20')]),
 obs('c1-commercial-basis',[rows('mulino_inventory_dispositionbases',dict(category='COMMERCIAL',action='SELL',state='ACTIVE',startquantity='0',quantity='40'),1)],{'C1_COMMERCIAL':dict(pointer='/rawRows/mulino_inventory_dispositionbases',where=dict(category='COMMERCIAL',action='SELL',state='ACTIVE'),column='id'),'C1_COMMERCIAL_REV':dict(pointer='/rawRows/mulino_inventory_dispositionbases',where=dict(category='COMMERCIAL',action='SELL',state='ACTIVE'),column='revision')})]
# Withdrawal original is an exact segment document; the generic segment identity
# reconciles the whole physical quantity100 before QualityEvidence accepts it.
name='c1-revoke-commercial';content=dict(segmentId=SEG,operation='revokeDispositionBasis',dispositionBasisId='$C1_COMMERCIAL')
s+=[dict(id=name,type='original',fixture=dict(synthetic=True,clock=dict(asOf=T,knownAt=T),sourceProfile=dict(namespace='native-c1-'+name,policyVersion='fixture-v1',nextAction='위탁 판매 허용 철회 범위 대조',nextCheckAt=N),occurrence=dict(kind='COMMERCIAL_REVOKED',content=content,externalEventId=name,quantity='100',unit='BOX',assertion='authored synthetic consignment withdrawal original')),binding=dict(organizationId='$ORG',workId='$WORK',itemId='$P',actorId='$reader',supervisorId='$supervisor',physicalScopeId=SEG,subjectId=SEG,subjectKind='SEGMENT',placeId='$W')),
 cmd(name+'-match','matchSourceIdentity',dict(claimId='$'+name+'.claim',basisDocumentId='$'+name+'.document',physicalScopeId=SEG,policyVersion='fixture-v1',sourceIdentity='native-c1-'+name+':'+name+':1',quantity='100',unit='BOX',effectiveFrom=T,reason='철회 원본과 동일 물리 범위 대조'),rev=1,refs=[dict(type='QuantitySegment',id=SEG)],bind={name+'.reconciliation':'/id'}),
 cmd(name+'-link','linkCanonicalOccurrence',dict(reconciliationId='$'+name+'.reconciliation'),rev=1,refs=[dict(type='QuantitySegment',id=SEG)],bind={name+'.canonical':'/id'}),
 cmd('c1-revoke-sell40','revokeDispositionBasis',dict(dispositionBasisId='$C1_COMMERCIAL',evidenceId='$'+name+'.document',reason='위탁 판매 허용 철회'),rev='$C1_COMMERCIAL_REV',refs=[dict(type='QuantitySegment',id=SEG)]),
 obs('c1-after-revoke',[rows('mulino_inventory_dispositionbases',dict(category='COMMERCIAL',action='SELL',state='ACTIVE'),0),rows('mulino_inventory_segmentallocations',dict(id='$ALLOCATION',state='SUSPENDED',quantity='20'),1)],{'ALLOCATION_REV':dict(pointer='/rawRows/mulino_inventory_segmentallocations',where=dict(id='$ALLOCATION'),column='revision'),'C1_SEG_REV':dict(pointer='/rawRows/mulino_inventory_quantitysegments',where=dict(id=SEG),column='revision')}),
 cmd('c1-new-reserve-after-revoke-denied','reserveQuantity',dict(segmentId=SEG,salesLineId='$SALES_LINE',startQuantity='20',quantity='20',unit='BOX'),rev='$C1_SEG_REV',outcome='REJECTED',refs=segment_refs,assertions=[eq('/error/code','SCOPE_INELIGIBLE'),eq('/effects',{})]),
 cmd('c1-dispatch-after-revoke-denied','dispatchQuantity',dict(allocationId='$ALLOCATION',transitPlaceId='$TRANSIT',occurredAt=T,evidenceRef='synthetic-c1-withdrawn-dispatch'),rev='$ALLOCATION_REV',outcome='REJECTED',assertions=[eq('/error/code','SCOPE_INELIGIBLE'),eq('/effects',{})]),
 obs('c1-final-independent',[total('mulino_inventory_quantitysegments','quantity','100',dict(retiredat=None,placeid='$W')),
  total('mulino_inventory_quantitysegments','quantity','0',dict(retiredat=None,placeid='$TRANSIT')),
  rows('mulino_inventory_segmentallocations',{},1),rows('mulino_inventory_segmentallocations',dict(id='$ALLOCATION',state='SUSPENDED',quantity='20'),1),
  dict(pointer='/rawRows/mulino_inventory_dispatches',operator='size',expected=0),dict(pointer='/rawRows/mulino_trade_sales_executioneffects',operator='size',expected=0),
  dict(pointer='/rawRows/mulino_work_read_obligationreferences',operator='humanDuties',kinds=['QUALITY_REVIEW'])]),
 inventory('c1-after-revoke-responsibility',[eq('/data/heldQuantity','100'),eq('/data/eligibleQuantity','0'),eq('/data/unreservedEligibleQuantity','0'),eq('/data/reservationResponsibilityQuantity','20')])]
write('c1-sale-revocation.json',dict(schemaVersion='1.0.0',status='NOT_RUN',actions=s))
write('c1-flow.json',dict(schemaVersion='1.0.0',status='NOT_RUN',requiredCases=['C1'],fullCaseCoverageClaimed=False,actions=[dict(id='c1-setup',type='setup',fixtureRef='verification/actual/s4/fixture.json',organizationAlias='ORG')]+[dict(id='c1-include-'+x,type='include',scriptRef='verification/actual/s4/'+x+'.json') for x in ['c1-upstream','c1-sale-revocation']]))
