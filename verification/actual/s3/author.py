#!/usr/bin/env python3
"""Author bounded flow input and independent quantity oracles; never queries product output."""
import json,pathlib
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
def original(name,kind,quantity,physical,payload,subject='$WORK',subject_kind='WORK',place='$W',existing=None):
    A.append({'id':name,'type':'original','fixture':{'synthetic':True,'clock':{'asOf':T,'knownAt':T},'sourceProfile':{'namespace':'native-s3-'+name,'policyVersion':'fixture-v1','nextAction':'원본 범위와 실제 효과 대조','nextCheckAt':'2026-10-08T09:00:00Z'},'occurrence':{'kind':kind,'content':payload,'externalEventId':name,'quantity':quantity,'unit':'BOX','assertion':'authored synthetic external original'}},'binding':{'organizationId':'$ORG','workId':'$WORK','itemId':'$P','actorId':'$reader','supervisorId':'$supervisor','physicalScopeId':physical,'subjectId':subject,'subjectKind':subject_kind,'placeId':place}})
    slots={'claimId':'$'+name+'.claim','basisDocumentId':'$'+name+'.document','physicalScopeId':physical,'policyVersion':'fixture-v1','sourceIdentity':'native-s3-'+name+':'+name+':1','quantity':quantity,'unit':'BOX','effectiveFrom':T,'reason':'외부 ORIGINAL과 동일한 범위 대조'}
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
def receipt(name,start,quantity,existing=None,transit=None):
    physical='$RANGE60' if quantity=='60' else '$RANGE40' if quantity=='40' else '$RANGE5'
    payload={'rangeRootId':physical,'startQuantity':start,'itemId':'$P','lotId':'$L','placeId':'$W','workId':'$WORK','quantity':quantity,'unit':'BOX','occurredAt':T}
    # Original seed precedes provisional; public reconciliation follows the observation.
    idx=len(A);original(name,'PHYSICAL_RECEIPT',quantity,physical,payload,subject='$P',subject_kind='ITEM',existing=existing)
    review=A[idx+1:];del A[idx+1:]
    slots={'eventId':'$'+name+'.event','rangeRootId':physical,'startQuantity':start,'quantity':quantity,'unit':'BOX','itemId':'$P','lotId':'$L','placeId':'$W','workId':'$WORK','ownerId':'$reader','supervisorId':'$supervisor','nextAction':'실물과 원본 사건 확인','nextCheckAt':'2026-10-08T09:00:00Z','occurredAt':T,'purchaseLineId':'$LINE'}
    if transit:slots['transitSegmentId']=transit
    cmd(name+'-provisional','receiveProvisional',slots,revision=0,bind={name+'.observation':'/effects/receiptId'})
    if name=='receipt60':raw('provisional-physical0',[count('mulino_inventory_quantitysegments',0),count('mulino_trade_receipt_observations',1)])
    A.extend(review)
    cmd(name+'-confirm','confirmReceipt',{'receiptId':'$'+name+'.observation','canonicalOccurrenceId':'$'+name+'.canonical','lotId':'$L'},revision=0,subject='$'+name+'.observation',noun='Receipt',bind={name+'.segment':'/effects/segmentId'},key=name+'-confirm-stable')
receipt('receipt60','0','60')
raw('after60',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','60')])
# Same key retry is a new authenticated HTTP call; observation must remain one effect.
cmd('retry60','confirmReceipt',{'receiptId':'$receipt60.observation','canonicalOccurrenceId':'$receipt60.canonical','lotId':'$L'},revision=0,subject='$receipt60.observation',noun='Receipt',key='receipt60-confirm-stable')
receipt('sourceB60','0','60',existing='$receipt60.canonical')
raw('duplicate60',[sums('mulino_inventory_quantitysegments','quantity','60',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','60')])
receipt('receipt40','0','40')
raw('after100',[sums('mulino_inventory_quantitysegments','quantity','100',{'retiredat':None}),sums('mulino_trade_receipt_receipts','contributedquantity','100')])
receipt('extra5','0','5')
raw('extra5-discrepancy',[sums('mulino_trade_receipt_receipts','contributedquantity','100'),sums('mulino_trade_receipt_receipts','excessquantity','5')])
flow['actions']=A
(ROOT/'flow.json').write_text(json.dumps(flow,indent=2,ensure_ascii=False)+'\n')
