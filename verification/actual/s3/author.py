#!/usr/bin/env python3
"""Author bounded flow input and independent quantity oracles; never queries product output."""
import json,pathlib,uuid
ROOT=pathlib.Path(__file__).resolve().parent
flow=json.loads((ROOT/'flow.json').read_text())
# Keep the reviewed purchase prefix; additions are rebuilt deterministically.
A=flow['actions'][:4]
T='2026-10-07T09:00:02Z'
RECORD={'recordSupplierReply','recordLegEvent','recordHandover','receiveProvisional','recordSubmission','recordRegulatoryDecision','verifyLabel','recordDispositionBasis'}
def cmd(name,cap,slots,revision=1,subject='$WORK',noun='Work',bind=None,outcome='APPLIED',assertions=None,key=None):
    req={'intentKind':'RECORD' if cap in RECORD else 'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,'expectedRevision':revision,'commandIdempotencyKey':key or name,'slots':slots,'provenance':{},'subjectRefs':[{'type':noun,'id':subject}]}
    a={'id':name,'type':'command','capability':cap,'request':req,'outcome':outcome}
    if bind:a['bind']=bind
    if assertions:a['assertions']=assertions
    A.append(a)
def original(name,kind,quantity,physical,payload,subject='$WORK',subject_kind='WORK',place='$W',existing=None,namespace=None,external=None,publish=True):
    namespace=namespace or 'native-s3-'+name
    external=external or name
    A.append({'id':name,'type':'original','fixture':{'synthetic':True,'clock':{'asOf':T,'knownAt':T},'sourceProfile':{'namespace':namespace,'policyVersion':'fixture-v1','nextAction':'원본 범위와 실제 효과 대조','nextCheckAt':'2026-10-08T09:00:00Z'},'occurrence':{'kind':kind,'content':payload,'externalEventId':external,'quantity':quantity,'unit':'BOX' if quantity is not None else None,'assertion':'authored synthetic external original'}},'binding':{'organizationId':'$ORG','workId':'$WORK','itemId':'$P','actorId':'$reader','supervisorId':'$supervisor','physicalScopeId':physical,'subjectId':subject,'subjectKind':subject_kind,'placeId':place}})
    if not publish:return
    slots={'claimId':'$'+name+'.claim','basisDocumentId':'$'+name+'.document','physicalScopeId':physical,'policyVersion':'fixture-v1','sourceIdentity':namespace+':'+external+':1','quantity':quantity,'unit':'BOX','effectiveFrom':T,'reason':'외부 ORIGINAL과 동일한 범위 대조'}
    if quantity is None:slots.pop('quantity');slots.pop('unit')
    if existing:slots['existingCanonicalId']=existing
    cmd(name+'-match','matchSourceIdentity',slots,subject=subject,noun={'SEGMENT':'QuantitySegment','ITEM':'TradeItem','PURCHASE_ORDER':'PurchaseOrder'}.get(subject_kind,'Work'),bind={name+'.reconciliation':'/id'},assertions=[{'pointer':'/evidenceStatus','operator':'equals','expected':'MATCHED'}])
    cmd(name+'-link','linkCanonicalOccurrence',{'reconciliationId':'$'+name+'.reconciliation'},subject=subject,noun={'SEGMENT':'QuantitySegment','ITEM':'TradeItem','PURCHASE_ORDER':'PurchaseOrder'}.get(subject_kind,'Work'),bind={name+'.canonical':'/id'},assertions=[{'pointer':'/evidenceStatus','operator':'equals','expected':'VERIFIED_RECORD_ONLY'}])
def raw(name,assertions,bind=None):
    a={'id':name,'type':'observe','assertions':assertions}
    if bind:a['bind']=bind
    A.append(a)
def sums(table,column,expected,where=None):
    a={'pointer':'/rawRows/'+table,'operator':'sum','column':column,'expected':expected}
    if where:a['where']=where
    return a
def count(table,expected,where=None):
    a={'pointer':'/rawRows/'+table,'operator':'matchingRows' if where else 'size','expected':expected}
    if where:a['where']=where
    return a
for alias in ['RANGE60','RANGE40','RANGE5']:A.append({'id':alias,'type':'uuid','alias':alias})
# Receipt raw original is installed before provisional; no canonical or physical stock seed.
def receipt(name,start,quantity,existing=None,transit=None,physical=None,purchase_line='$LINE'):
    physical=physical or ('$RANGE60' if quantity=='60' else '$RANGE40' if quantity=='40' else '$RANGE5')
    payload={'rangeRootId':physical,'startQuantity':start,'itemId':'$P','lotId':'$L','placeId':'$W','workId':'$WORK','quantity':quantity,'unit':'BOX','occurredAt':T}
    # Original seed precedes provisional; public reconciliation follows the observation.
    idx=len(A);original(name,'PHYSICAL_RECEIPT',quantity,physical,payload,subject='$P',subject_kind='ITEM',existing=existing)
    review=A[idx+1:];del A[idx+1:]
    slots={'eventId':'$'+name+'.event','rangeRootId':physical,'startQuantity':start,'quantity':quantity,'unit':'BOX','itemId':'$P','lotId':'$L','placeId':'$W','workId':'$WORK','ownerId':'$reader','supervisorId':'$supervisor','nextAction':'실물과 원본 사건 확인','nextCheckAt':'2026-10-08T09:00:00Z','occurredAt':T,'purchaseLineId':purchase_line}
    if purchase_line is None:slots.pop('purchaseLineId')
    if transit:slots['transitSegmentId']=transit
    cmd(name+'-provisional','receiveProvisional',slots,revision=0,bind={name+'.observation':'/effects/receiptId'})
    if name=='receipt60':raw('provisional-physical0',[count('mulino_inventory_quantitysegments',0),count('mulino_trade_receipt_observations',2)])
    A.extend(review)
    cmd(name+'-confirm','confirmReceipt',{'receiptId':'$'+name+'.observation','canonicalOccurrenceId':'$'+name+'.canonical','lotId':'$L'},revision=0,subject='$'+name+'.observation',noun='Receipt',bind={name+'.segment':'/effects/segmentId'},key=name+'-confirm-stable')
# Unidentified provisional input remains responsibility only, with no stock/eligibility.
A.append({'id':'unidentified-range','type':'uuid','alias':'UNKNOWN_RANGE'})
unknown={'rangeRootId':'$UNKNOWN_RANGE','startQuantity':'0','itemId':'$P','placeId':'$W','workId':'$WORK','quantity':'7','unit':'BOX','occurredAt':T}
original('unidentified-original','PHYSICAL_RECEIPT','7','$UNKNOWN_RANGE',unknown,subject='$P',subject_kind='ITEM',publish=False)
cmd('unidentified-provisional','receiveProvisional',{'eventId':'$unidentified-original.event',**unknown,'ownerId':'$reader','supervisorId':'$supervisor','nextAction':'LOT 미식별 실물 대조','nextCheckAt':'2026-10-08T09:00:00Z'},revision=0)
raw('unidentified-no-stock',[count('mulino_trade_receipt_observations',1,{'identificationstatus':'UNKNOWN','state':'PROVISIONAL'}),count('mulino_trade_receipt_receipts',0),count('mulino_inventory_quantitysegments',0)])
A.append({'id':'unidentified-eligible0','type':'query','capability':'getInventory','request':{'id':'$P','scope':{'organizationId':'$ORG','itemId':'$P'},'asOf':T,'knownAt':T},'assertions':[{'pointer':'/data/eligibleQuantity','operator':'equals','expected':'0'}]})
receipt('receipt60','0','60')
A[-1]['type']='lost-response'
raw('after60',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','60'),sums('mulino_work_read_obligationreferences','quantity','40',{'kind':'RECEIPT_SHORTFALL','status':'OPEN'}),count('mulino_responsibility_receiptresidualroots',1),sums('mulino_responsibility_receiptresidualroots','initialcontribution','60'),sums('mulino_responsibility_receiptresidualroots','orderedquantity','100')])
# Same key retry is a new authenticated HTTP call; observation must remain one effect.
cmd('retry60','confirmReceipt',{'receiptId':'$receipt60.observation','canonicalOccurrenceId':'$receipt60.canonical','lotId':'$L'},revision=0,subject='$receipt60.observation',noun='Receipt',key='receipt60-confirm-stable')
A[-1]['type']='parallel'
raw('concurrent-retry-one-effect',[count('mulino_commands_commandrecords',1,{'commandidempotencykey':'receipt60-confirm-stable','actorid':'$reader','state':'COMMITTED'}),count('mulino_commands_commandaudits',1,{'capabilityid':'confirmReceipt','outcome':'APPLIED'}),count('mulino_inventory_quantitymovements',1,{'kind':'RECEIPT'}),count('mulino_trade_receipt_receipts',1)])
receipt('sourceB60','0','60',existing='$receipt60.canonical')
raw('duplicate60',[count('mulino_evidence_canonicaloccurrences',1,{'kind':'PHYSICAL_RECEIPT'}),count('mulino_inventory_quantitymovements',1,{'kind':'RECEIPT'}),sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','60')])
cmd('other-actor-no-replay','confirmReceipt',{'receiptId':'$receipt60.observation','canonicalOccurrenceId':'$receipt60.canonical','lotId':'$L'},revision=0,subject='$receipt60.observation',noun='Receipt',key='receipt60-confirm-stable',outcome='REJECTED',assertions=[{'pointer':'/effects','operator':'equals','expected':{}},{'pointer':'/error/code','operator':'equals','expected':'FORBIDDEN'}]);A[-1]['actor']='outsider'
receipt('receipt40','0','40')
raw('after100',[sums('mulino_inventory_quantitysegments','quantity','100',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','100'),count('mulino_work_read_obligationreferences',0,{'kind':'RECEIPT_SHORTFALL','status':'OPEN'}),sums('mulino_responsibility_receiptresidualcredits','quantity','40'),sums('mulino_work_read_obligationreferences','quantity','40',{'kind':'RECEIPT_SHORTFALL','status':'RESOLVED'})])
receipt('extra5','0','5')
raw('extra5-discrepancy',[sums('mulino_trade_receipt_receipts','contributedquantity','100'),sums('mulino_trade_receipt_receipts','excessquantity','5')])
# Source reply and shipment plan belong before physical receipt.
start_index=len(A)
original('supplier-accept','SUPPLIER_ACCEPT','100','$ORDER',{'orderId':'$ORDER','itemId':'$P','reply':'ACCEPT','quantity':'100','unit':'BOX','proposalRevision':1},subject='$ORDER',subject_kind='PURCHASE_ORDER')
cmd('supplier-accepted','recordSupplierReply',{'purchaseId':'$ORDER','reply':'ACCEPT','proposedQuantity':{'value':'100','unit':'BOX'},'evidence':'$supplier-accept.document','canonicalOccurrenceId':'$supplier-accept.canonical'},subject='$ORDER',noun='PurchaseOrder',bind={'LINE_REV':'/revision'})
cmd('shipment-plan','createShipment',{'originId':'$TRANSIT','destinationId':'$W','carrierRef':'synthetic-no-external-send','workId':'$WORK','cargo':[{'itemId':'$P','quantity':'100','unit':'BOX','allocations':[{'poLineId':'$LINE','quantity':'100'}]}],'legs':[{'originId':'$TRANSIT','destinationId':'$W','carrierRef':'synthetic-no-external-send'}]},revision=0,bind={'SHIPMENT':'/effects/shipmentId','CARGO':'/effects/cargoIds/0','LEG':'/effects/legIds/0'})
A[-1]['request']['subjectRefs']=[]
# Planned shipment makes no physical inventory. Independent snapshot verifies this.
raw('shipment-planned-physical0',[count('mulino_inventory_quantitysegments',0),sums('mulino_trade_shipment_shipmentcargo','plannedquantity','100')])
A.append({'id':'shipment-physical-range','type':'uuid','alias':'SHIP_RANGE'})
for name,kind,place in [('ship-departure','DEPARTURE','$TRANSIT'),('ship-arrival','ARRIVAL','$W')]:
    payload={'shipmentEventId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino-shipment:'+name)),'shipmentId':'$SHIPMENT','cargoId':'$CARGO','legId':'$LEG','placeId':place,'physicalScopeId':'$SHIP_RANGE','quantity':'100','unit':'BOX','occurredAt':T,'kind':kind}
    original(name,'SHIPMENT_'+kind,'100','$SHIP_RANGE',payload,subject='$P',subject_kind='ITEM',place=place)
    cmd(name+'-record','recordLegEvent',{'shipmentId':'$SHIPMENT','cargoId':'$CARGO','legId':'$LEG','kind':kind,'placeId':place,'physicalScopeId':'$SHIP_RANGE','occurrenceId':'$'+name+'.canonical','quantity':'100','unit':'BOX','occurredAt':T},revision=1 if kind=='DEPARTURE' else '$SHIP_REV',subject='$SHIPMENT',noun='Shipment',bind={'SHIP_REV':'/revision'})
name='custody-handover'
payload={'shipmentEventId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino-shipment:'+name)),'shipmentId':'$SHIPMENT','cargoId':'$CARGO','legId':'$LEG','placeId':'$W','physicalScopeId':'$SHIP_RANGE','quantity':'100','unit':'BOX','occurredAt':T,'fromCustodianId':'$reader','toCustodianId':'$supervisor'}
original(name,'CUSTODY_HANDOVER','100','$SHIP_RANGE',payload,subject='$P',subject_kind='ITEM')
cmd('handover-record','recordHandover',{'shipmentId':'$SHIPMENT','cargoId':'$CARGO','legId':'$LEG','placeId':'$W','physicalScopeId':'$SHIP_RANGE','occurrenceId':'$custody-handover.canonical','quantity':'100','unit':'BOX','occurredAt':T,'fromCustodianId':'$reader','toCustodianId':'$supervisor'},revision='$SHIP_REV',subject='$SHIPMENT',noun='Shipment')
raw('shipment-actual-no-stock',[count('mulino_inventory_quantitysegments',0),count('mulino_trade_shipment_legevents',2),count('mulino_trade_shipment_custodyhandovers',1)])

planned=A[start_index:];del A[start_index:];A[4:4]=planned
# Current revision is observed after each assessment/invalidation.
raw('before-assessment100',[sums('mulino_trade_receipt_receipts','contributedquantity','100')],bind={'WORK_REV':'/rawRows/mulino_work_read_works/0/revision'})
cmd('assess-arrival','assessGoal',{'workId':'$WORK'},revision='$WORK_REV',assertions=[{'pointer':'/assessment/outcome','operator':'equals','expected':'SATISFIED'}])
# Fictional regulator accepts30 from the60 physical scope, after actual submission.
original('reg-draft','REGULATORY_DRAFT','60','$receipt60.segment',{'draft':'fictional dossier','itemId':'$P'},subject='$receipt60.segment',subject_kind='SEGMENT',namespace='native-s3-regulator',publish=False)
cmd('reg-prepare','prepareRegulatoryProcedure',{'itemId':'$P','lotId':'$L','physicalScopeId':'$receipt60.segment','workId':'$WORK','policyId':'$REGPOL','documentVersionId':'$reg-draft.document'},revision=0,subject='$receipt60.segment',noun='QuantitySegment',bind={'PROC':'/effects/procedureId','PROC_VERSION':'/effects/procedureVersionId','PROC_REV':'/revision'},assertions=[{'pointer':'/status','operator':'equals','expected':'PREPARED'}])
raw('draft-not-submitted',[count('mulino_trade_regulatory_procedureversions',1,{'status':'PREPARED'}),count('mulino_trade_regulatory_decisionversions',0)])
def reg_original(name,kind,fields,quantity=None):
    payload={'regulatoryEventId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino-regulatory:'+name)),'procedureId':'$PROC','procedureVersionId':'$PROC_VERSION','itemId':'$P','lotId':'$L','physicalScopeId':'$receipt60.segment','authority':'FICTIONAL-AUTHORITY','policyVersion':'fixture-v1',**fields}
    original(name,kind,quantity,'$receipt60.segment',payload,subject='$receipt60.segment',subject_kind='SEGMENT',namespace='native-s3-regulator')
reg_original('reg-submitted','REGULATORY_SUBMISSION',{'occurredAt':T})
cmd('submit-reg','recordSubmission',{'procedureId':'$PROC','occurredAt':T,'occurrenceId':'$reg-submitted.canonical'},revision='$PROC_REV',subject='$receipt60.segment',noun='QuantitySegment',bind={'PROC_VERSION':'/effects/procedureVersionId','PROC_REV':'/revision'},assertions=[{'pointer':'/status','operator':'equals','expected':'SUBMITTED'}])
decision={'decision':'ALLOWED','action':'SELL','startQuantity':'0','quantity':'30','unit':'BOX','validFrom':T,'validUntil':'2026-10-31T00:00:00Z'}
reg_original('reg-allow30','REGULATORY_DECISION',decision,'30')
cmd('allow30','recordRegulatoryDecision',{'procedureId':'$PROC','occurrenceId':'$reg-allow30.canonical',**decision},revision='$PROC_REV',subject='$receipt60.segment',noun='QuantitySegment',bind={'PROC_REV':'/revision'})
label={'decision':'VERIFIED','packagingVersionId':'$PACK','specificationVersionId':'$SPEC','validFrom':T,'validUntil':'2026-10-31T00:00:00Z'}
reg_original('reg-label','LABEL_VERIFICATION',label)
cmd('label-verified','verifyLabel',{'procedureId':'$PROC','occurrenceId':'$reg-label.canonical',**label},revision='$PROC_REV',subject='$receipt60.segment',noun='QuantitySegment')
def quality(name,segment,quantity,category,operation='recordDispositionBasis'):
    external=str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino-s3-native:'+name))
    fields={'segmentId':segment,'operation':operation,'action':'SELL','category':category,'startQuantity':'0','quantity':quantity,'unit':'BOX','validFrom':T,'validUntil':'2026-10-31T00:00:00Z','workId':'$WORK'}
    original(name,'QUALITY_DECISION_'+operation+'_'+category,quantity,segment,{**fields,'sourceDecisionId':external,'sourceVersion':'1'},subject=segment,subject_kind='SEGMENT',external=external)
    slots={k:v for k,v in fields.items() if k!='operation'}
    slots.update(evidenceId='$'+name+'.document',reason='원본의 정확한 행동 범위를 판정',nextCheckAt='2026-10-08T09:00:00Z')
    cmd(name+'-decision',operation,slots,revision=0,subject=segment,noun='QuantitySegment',bind={name+'.restriction':'/effects/restrictionId'} if operation=='placeHold' else None)
for segment,quantity,prefix in [('$receipt60.segment','60','s60'),('$receipt40.segment','40','s40')]:
    for category in ['QC','CUSTOMER','COMMERCIAL']:quality(prefix+'-'+category,segment,quantity,category)
def inventory(name,expected):
    A.append({'id':name,'type':'query','capability':'getInventory','request':{'id':'$P','scope':{'organizationId':'$ORG','itemId':'$P'},'asOf':T,'knownAt':T},'assertions':[{'pointer':'/data/eligibleQuantity','operator':'equals','expected':expected}]})
raw('independent-permission100-reg30',[sums('mulino_inventory_dispositionbases','quantity','100',{'category':'QC','state':'ACTIVE'}),sums('mulino_trade_regulatory_decisionversions','quantity','30',{'decision':'ALLOWED'}),sums('mulino_inventory_quantitysegments','quantity','105',{'retiredat':None})])
inventory('eligible-max30','30')
quality('qc-hold20','$receipt60.segment','20','QC','placeHold')
quality('recall-hold60','$receipt60.segment','60','RECALL','placeHold')
inventory('overlap-holds0','0')
original('qc-release','QUALITY_RELEASE','60','$receipt60.segment',{'segmentId':'$receipt60.segment','operation':'releaseHold','restrictionId':'$qc-hold20.restriction'},subject='$receipt60.segment',subject_kind='SEGMENT')
cmd('release-qc-only','releaseHold',{'restrictionId':'$qc-hold20.restriction','evidenceId':'$qc-release.document','reason':'QC 범위 해제만 기록'},revision=0,subject='$receipt60.segment',noun='QuantitySegment')
inventory('recall-retained0','0')
raw('independent-overlap-release',[count('mulino_inventory_restrictions',1,{'state':'ACTIVE','category':'RECALL'}),count('mulino_inventory_restrictions',1,{'state':'RELEASED','category':'QC'}),sums('mulino_inventory_quantitysegments','quantity','105',{'retiredat':None})])

# A changed effect payload must conflict on the original stable key.
cmd('changed-key40','confirmReceipt',{'receiptId':'$receipt40.observation','canonicalOccurrenceId':'$receipt40.canonical','lotId':'$L'},revision=0,subject='$receipt40.observation',noun='Receipt',key='receipt60-confirm-stable',outcome='CONFLICT',assertions=[{'pointer':'/error/code','operator':'equals','expected':'IDEMPOTENCY_CONFLICT'}])
raw('before-cancel',[sums('mulino_trade_receipt_receipts','contributedquantity','100')],bind={'WORK_REV':'/rawRows/mulino_work_read_works/0/revision'})
def purchase(name,quantity):
    cmd(name+'-proposal','proposePurchase',{'workId':'$WORK','quantity':{'value':quantity,'unit':'BOX'},'price':{'value':'20','currency':'EUR'},'supplierId':'$SUP','destinationId':'$W','dueAt':'2026-10-31T00:00:00Z','endpoint':'ARRIVED','quantityMode':'CUMULATIVE_EVENT'},revision='$WORK_REV',bind={name+'.proposal':'/proposalId',name+'.hash':'/proposalHash'})
    cmd(name+'-approval','approvePurchase',{'proposalId':'$'+name+'.proposal','proposalHash':'$'+name+'.hash','decision':'APPROVE','validUntil':'2026-10-31T00:00:00Z','conditions':[]},bind={name+'.approval':'/approvalId'})
    A[-1]['actor']='supervisor'
purchase('cancel10','10')
cmd('cancel10-dispatch','dispatchPurchaseOrder',{'proposalId':'$cancel10.proposal','proposalHash':'$cancel10.hash','approvalId':'$cancel10.approval','channel':'SYNTHETIC','externalOperationId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino:s3-cancel10'))},bind={'CANCEL_ORDER':'/objectId'})
cmd('cancel-unconfirmed','cancelPurchase',{'purchaseId':'$CANCEL_ORDER','quantity':{'value':'10','unit':'BOX'},'reason':'외부 수락 확인 전 취소 요청','externalAcceptance':'UNKNOWN'},subject='$CANCEL_ORDER',noun='PurchaseOrder',assertions=[{'pointer':'/businessStatus','operator':'equals','expected':'CANCELLATION_PENDING'}])
raw('cancel-duty-retained',[count('mulino_trade_purchase_cancellations',1,{'externalacceptance':'UNKNOWN'}),count('mulino_work_read_obligationreferences',1,{'kind':'PURCHASE_CANCELLATION_ACCEPTANCE','status':'OPEN','ownerid':'$reader'})],bind={'WORK_REV':'/rawRows/mulino_work_read_works/0/revision'})
purchase('stale100','100')
cmd('revise120','revisePurchase',{'proposalId':'$stale100.proposal','changes':{'quantity':{'value':'120','unit':'BOX'}},'reason':'원승인을 새 수량에 재사용할 수 없다'},bind={'REVISED_HASH':'/proposalHash'})
cmd('stale100-dispatch','dispatchPurchaseOrder',{'proposalId':'$stale100.proposal','proposalHash':'$stale100.hash','approvalId':'$stale100.approval','channel':'SYNTHETIC','externalOperationId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino:s3-stale-must-not-send'))},revision=2,outcome='HELD',assertions=[{'pointer':'/error/code','operator':'equals','expected':'APPROVAL_HASH_MISMATCH'}])
raw('stale-dispatch-effect0',[count('mulino_trade_purchase_orders',0,{'externaloperationid':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino:s3-stale-must-not-send'))}),sums('mulino_inventory_quantitysegments','quantity','105',{'retiredat':None})])

raw('before-independent-order',[],bind={'WORK_REV':'/rawRows/mulino_work_read_works/0/revision'})
purchase('independent5','5')
cmd('independent5-dispatch','dispatchPurchaseOrder',{'proposalId':'$independent5.proposal','proposalHash':'$independent5.hash','approvalId':'$independent5.approval','channel':'SYNTHETIC','externalOperationId':str(uuid.uuid5(uuid.NAMESPACE_URL,'mulino:s3-independent5'))},bind={'SEPARATE_LINE':'/poLineId'})
A.append({'id':'independent-range','type':'uuid','alias':'RANGESEP'})
receipt('independent5','0','5',physical='$RANGESEP',purchase_line='$SEPARATE_LINE')
raw('separate-order-separate-key',[sums('mulino_trade_receipt_receipts','contributedquantity','100',{'purchaselineid':'$LINE'}),sums('mulino_trade_receipt_receipts','contributedquantity','5',{'purchaselineid':'$SEPARATE_LINE'}),sums('mulino_inventory_quantitysegments','quantity','110',{'retiredat':None})])

# Separate organization proves movement of a pre-existing transit input only.
A.append({'id':'transit-input-setup','type':'setup','fixtureRef':'verification/actual/s3/transit-fixture.json','organizationAlias':'TRANSIT_ORG'})
raw('transit-input60',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None,'placeid':'$TRANSIT'}),count('mulino_inventory_quantitymovements',1,{'kind':'INITIAL_BALANCE'})])
create=json.loads((ROOT/'flow.json').read_text())['actions'][0]
create['id']='transit-work';create['request']['commandIdempotencyKey']='transit-work';create['request']['slots']['quantity']['value']='60';A.append(create)
A.append({'id':'transit-range','type':'uuid','alias':'RANGETRANSIT'})
receipt('transit60','0','60',transit='$INPUT60',physical='$RANGETRANSIT',purchase_line=None)
raw('transit-moved-once',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),sums('mulino_inventory_quantitysegments','quantity','0',{'retiredat':None,'placeid':'$TRANSIT'}),sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None,'placeid':'$W'}),count('mulino_inventory_quantitymovements',1,{'kind':'RECEIPT_MOVE'})])
cmd('transit-retry','confirmReceipt',{'receiptId':'$transit60.observation','canonicalOccurrenceId':'$transit60.canonical','lotId':'$L'},revision=0,subject='$transit60.observation',noun='Receipt',key='transit60-confirm-stable');A[-1]['type']='parallel'
raw('transit-retry-no-new-effect',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),count('mulino_inventory_quantitymovements',1,{'kind':'RECEIPT_MOVE'}),count('mulino_trade_receipt_receipts',1,{'physicaleffect':'TRANSIT_MOVE'})])

# Warehouse custody is known only from verified receipt evidence (plan §4.1/§4.2):
# the direct warehouse receipt originals name the receiving custodian and every
# confirm of them carries the same explicit slot, so SELL eligibility at W is
# assessed, not left CUSTODY_UNCONFIRMED. The custodian is the supervisor, not
# the confirming reader, so nothing is inferred from the acting identity.
CUSTODY_RECEIPTS={'receipt60','receipt40'}
for x in A:
    if x['type']=='original' and x['id'] in CUSTODY_RECEIPTS:x['fixture']['occurrence']['content']['receivingCustodianId']='$supervisor'
    if x.get('capability')=='confirmReceipt' and x['request']['slots'].get('receiptId') in {'$'+r+'.observation' for r in CUSTODY_RECEIPTS}:x['request']['slots']['receivingCustodianId']='$supervisor'
flow['actions']=A
(ROOT/'flow.json').write_text(json.dumps(flow,indent=2,ensure_ascii=False)+'\n')
