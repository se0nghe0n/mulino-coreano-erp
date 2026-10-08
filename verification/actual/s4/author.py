#!/usr/bin/env python3
"""Author deterministic S4 inputs; no database or product response is read here."""
import copy,json,pathlib,uuid
root=pathlib.Path(__file__).resolve().parents[3]
out=root/'verification/actual/s4'
T='2026-10-07T09:00:02Z';NEXT='2026-10-08T09:00:00Z'
def write(name,value): (out/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
def declare_capabilities(definition):
 # A verb without its pinned capability makes the whole definition non-VALID
 # (plan §8); the product then HOLDs every goal pinned to it.
 known={c['capabilityId'] for c in definition['capabilities']}
 for verb in definition['verbs']:
  if verb['capabilityId'] not in known:
   definition['capabilities'].append({'capabilityId':verb['capabilityId'],'semanticVersion':'1.0.0','evaluatorVersion':'core-v1','inputSchemaVersion':'1.0.0','outputSchemaVersion':'1.0.0','supportedWorkMigration':[]});known.add(verb['capabilityId'])
def command(id,capability,slots,bind=None,intent='COMMAND',revision=0,refs=None,actor=None,outcome='APPLIED',assertions=None):
 a={'id':id,'type':'command','capability':capability,'request':{'intentKind':intent,'definitionVersion':'definition-v1','capabilityId':capability,'expectedRevision':revision,'commandIdempotencyKey':id,'slots':slots,'provenance':{},'subjectRefs':refs or [{'type':'Work','id':'$WORK'}]},'outcome':outcome}
 if bind:a['bind']=bind
 if actor:a['actor']=actor
 if assertions:a['assertions']=assertions
 return a

def original(id,kind,content,physical,quantity,subject='WORK',subjectid='$WORK',work='$WORK',place='$W'):
 return {'id':id,'type':'original','fixture':{'synthetic':True,'clock':{'asOf':T,'knownAt':T},'sourceProfile':{'namespace':'native-s4-'+id,'policyVersion':'fixture-v1','nextAction':'원본 범위와 실제 효과 대조','nextCheckAt':NEXT},'occurrence':{'kind':kind,'content':content,'externalEventId':id,'quantity':quantity,'unit':'BOX' if quantity is not None else None,'assertion':'authored synthetic external original'}},'binding':{'organizationId':'$ORG','workId':work,'itemId':'$P','actorId':'$reader','supervisorId':'$supervisor','physicalScopeId':physical,'subjectId':subjectid,'subjectKind':subject,'placeId':place}}

def link(id,physical,quantity,refs=None):
 m=command(id+'-match','matchSourceIdentity',{'claimId':'$'+id+'.claim','basisDocumentId':'$'+id+'.document','physicalScopeId':physical,'policyVersion':'fixture-v1','sourceIdentity':'native-s4-'+id+':'+id+':1','quantity':quantity,'unit':'BOX','effectiveFrom':T,'reason':'동일 원본과 대상 범위 대조'},bind={id+'.reconciliation':'/id'},revision=1,refs=refs)
 l=command(id+'-link','linkCanonicalOccurrence',{'reconciliationId':'$'+id+'.reconciliation'},bind={id+'.canonical':'/id'},revision=1,refs=refs)
 return [m,l]

fixture=json.loads((out/'fixture.json').read_text())
fixture['aliases']['C']={'type':'Customer','name':'가상 B2B 고객'}
fixture['aliases']['CUSTOMER_PLACE']={'type':'Place','name':'가상 고객 장소','kind':'CUSTOMER'}
fixture['aliases']['REGPOL']['sourceNamespace']='native-s4-regulator'
fixture['aliases']['REGDISPATCH']=copy.deepcopy(fixture['aliases']['REGPOL']);fixture['aliases']['REGDISPATCH'].update(action='DISPATCH',sourceNamespace='native-s4-dispatch-regulator')
fixture['aliases']['ELIG']['content']['actions']['DISPATCH']=copy.deepcopy(fixture['aliases']['ELIG']['content']['actions']['SELL'])
classes={'createSalesOrder':'SALES_ORDER','reviseSalesOrder':'SALES_ORDER','recordDelivery':'RECORD','recordObservedMovement':'RECORD','reserveQuantity':'RESERVE','replaceAllocation':'RESERVE','pickQuantity':'ALLOCATION','releaseAllocation':'ALLOCATION','dispatchQuantity':'DISPATCH','authorizeReturn':'RETURN','receiveReturn':'RETURN','recordInvoice':'SETTLEMENT','matchInvoice':'SETTLEMENT'}
for actor in fixture['actors'].values():
 if actor['subject'].endswith('outsider'):continue
 for cap in classes:
  if cap not in actor['roleCapabilities']:actor['roleCapabilities'].append(cap)
  if cap not in actor['grant']['actions']:actor['grant']['actions'].append(cap)
for cap,cls in classes.items():
 fixture['aliases']['CPOL']['content']['rules'][cap]={'effectClass':cls}
 if cap not in {v['name'] for v in fixture['aliases']['DEF']['content']['verbs']}:fixture['aliases']['DEF']['content']['verbs'].append({'name':cap,'intentKind':'RECORD' if cap in ['recordDelivery','recordObservedMovement','receiveReturn','recordInvoice'] else 'COMMAND','capabilityId':cap,'stage':'DRAFT','slots':{}})
for noun in ['SalesOrder','SalesOrderLine','DeliveryObservation','Delivery','Return']:
 if noun not in {n['name'] for n in fixture['aliases']['DEF']['content']['nouns']}:fixture['aliases']['DEF']['content']['nouns'].append({'name':noun,'core':True})
fixture['aliases']['RETURN_AUTH']={'type':'ManagementAuthority','actorAlias':'supervisor','capabilityId':'authorizeReturn','validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T23:59:59Z'}
fixture['aliases']['DEF']['content']['attributes'].append({'nounType':'Delivery','name':'quantity','type':'DECIMAL','referenceType':None,'unit':'BOX','decimalPlaces':0,'minimumCount':1,'maximumCount':1,'requiredStage':'READ','core':True}) if not any(x['nounType']=='Delivery' and x['name']=='quantity' for x in fixture['aliases']['DEF']['content']['attributes']) else None
if not any(x['name']=='delivered' for x in fixture['aliases']['DEF']['content']['goals']): fixture['aliases']['DEF']['content']['goals'].append({'name':'delivered','quantityMode':'CUMULATIVE_EVENT','endpoint':'DELIVERED','evaluatorVersion':'core-v1','predicate':{'operator':'quantitySum','property':'Delivery.quantity','minimum':{'value':'1','unit':'BOX'},'unit':'BOX','evidenceSelector':'VERIFIED_DISTINCT'}})
declare_capabilities(fixture['aliases']['DEF']['content'])
write('fixture.json',fixture)
# All DISPATCH conditions arise from their own approved originals and gateway decisions.
base=json.loads((out/'e1-upstream.json').read_text())['actions'];extras=[]
sell_decisions=[a['fixture']['occurrence']['content']['sourceDecisionId'] for a in base if a['id'] in ['s60-QC','s60-CUSTOMER','s60-COMMERCIAL']]
for a in base:
 if a['id'] in ['s60-QC','s60-QC-match','s60-QC-link','s60-QC-decision','s60-CUSTOMER','s60-CUSTOMER-match','s60-CUSTOMER-link','s60-CUSTOMER-decision','s60-COMMERCIAL','s60-COMMERCIAL-match','s60-COMMERCIAL-link','s60-COMMERCIAL-decision']:
  text=json.dumps(a).replace('s60-','dispatch-s60-').replace('"SELL"','"DISPATCH"')
  # A DISPATCH basis is a separate source decision. Reusing the SELL decision
  # id gives one occurrence identity two contents, which the gateway must
  # reconcile as CONFLICT rather than a second verified basis.
  for decision in sell_decisions:text=text.replace(decision,str(uuid.uuid5(uuid.NAMESPACE_URL,'s4-dispatch:'+decision)))
  extras.append(json.loads(text))
for a in base:
 if a['id'].startswith('reg-') or a['id'] in ['submit-reg','allow30','label-verified','draft-not-submitted']:
  x=json.loads(json.dumps(a).replace('reg-','dispatch-reg-').replace('native-s4-regulator','native-s4-dispatch-regulator').replace('$REGPOL','$REGDISPATCH').replace('$PROC','$DPROC').replace('"PROC','"DPROC').replace('"SELL"','"DISPATCH"').replace('"submit-reg"','"dispatch-submit-reg"').replace('"allow30"','"dispatch-allow30"').replace('"label-verified"','"dispatch-label-verified"').replace('"draft-not-submitted"','"dispatch-draft-not-submitted"'))
  # Global table sum assertions are upstream-only; individual response assertions remain.
  if x['type']=='observe':continue
  extras.append(x)
write('e1-dispatch-authority.json',{'schemaVersion':'1.0.0','actions':extras})
# Planned sales input and observed physical delivery are separate commands.
work=copy.deepcopy(json.loads((root/'verification/actual/s3/flow.json').read_text())['actions'][0])
work['id']='e1-create-sales-work';work['request']['commandIdempotencyKey']=work['id']
work['request']['slots'].update(workType='SALES',endpoint='DELIVERED',eventKinds=['PHYSICAL_DELIVERY'],quantity={'value':'30','unit':'BOX'})
work['bind']={'SALES_WORK':'/effects/workId','SALES_WORK_REV':'/revision'}
a=[work,command('e1-create-sales','createSalesOrder',{'workId':'$SALES_WORK','customerId':'$C','itemId':'$P','quantity':'30','unit':'BOX','price':'1','currency':'EUR','dueAt':'2026-10-31T00:00:00Z','destinationId':'$CUSTOMER_PLACE','deliveryEndpoint':'DELIVERED','qualityTerms':'가상 조건 충족','packageTerms':'가상 포장 조건 충족'},bind={'SALES_ORDER':'/effects/orderId','SALES_LINE':'/effects/lineId'},refs=[{'type':'Work','id':'$SALES_WORK'},{'type':'TradeItem','id':'$P'}]),
 {'id':'e1-segment-revision','type':'query','capability':'getObject','request':{'id':'$receipt60.segment','scope':{'organizationId':'$ORG','objectType':'QuantitySegment'},'asOf':T,'knownAt':T},'bind':{'RESERVE_SEGMENT_REV':'/data/revision'}},
 command('e1-reserve30','reserveQuantity',{'segmentId':'$receipt60.segment','salesLineId':'$SALES_LINE','startQuantity':'0','quantity':'30','unit':'BOX'},bind={'ALLOCATION':'/effects/allocationId','ALLOCATION_REV':'/revision'},revision='$RESERVE_SEGMENT_REV',refs=[{'type':'QuantitySegment','id':'$receipt60.segment'},{'type':'SalesOrderLine','id':'$SALES_LINE'}]),
 command('e1-pick30','pickQuantity',{'allocationId':'$ALLOCATION'},bind={'ALLOCATION_REV':'/revision'},revision='$ALLOCATION_REV'),
 command('e1-dispatch30','dispatchQuantity',{'allocationId':'$ALLOCATION','transitPlaceId':'$TRANSIT','occurredAt':T,'evidenceRef':'synthetic-e1-dispatch30'},bind={'DISPATCH':'/effects/dispatchId','CARGO':'/effects/cargoScopeId','TRANSIT_SEGMENT':'/effects/transitSegmentId','DISPATCH_RANGE':'/effects/rangeRootId'},revision='$ALLOCATION_REV'),
 {'id':'DELIVERY_EVENT','type':'uuid','alias':'DELIVERY_EVENT'}, {'id':'DELIVERY_OBSERVATION','type':'uuid','alias':'DELIVERY_OBSERVATION'}]
content={'deliveryEventId':'$DELIVERY_EVENT','dispatchId':'$DISPATCH','cargoScopeId':'$CARGO','salesLineId':'$SALES_LINE','customerId':'$C','itemId':'$P','lotId':'$L','rangeRootId':'$DISPATCH_RANGE','physicalScopeId':'$DELIVERY_OBSERVATION','startQuantity':'0','quantity':'30','unit':'BOX','placeId':'$CUSTOMER_PLACE','occurredAt':T}
a += [original('e1-delivery-original','PHYSICAL_DELIVERY',content,'$DELIVERY_OBSERVATION','30',work='$SALES_WORK',subjectid='$SALES_WORK',place='$CUSTOMER_PLACE'),
 command('e1-delivery-intake','recordDelivery',{'observationId':'$DELIVERY_OBSERVATION','eventId':'$e1-delivery-original.event','dispatchId':'$DISPATCH','cargoScopeId':'$CARGO','salesLineId':'$SALES_LINE','workId':'$SALES_WORK','customerId':'$C','itemId':'$P','lotId':'$L','rangeRootId':'$DISPATCH_RANGE','startQuantity':'0','quantity':'30','unit':'BOX','placeId':'$CUSTOMER_PLACE','occurredAt':T,'nextAction':'인도 원본 대조','nextCheckAt':NEXT},intent='RECORD',refs=[{'type':'Work','id':'$SALES_WORK'}])]
a += link('e1-delivery-original','$DELIVERY_OBSERVATION','30',refs=[{'type':'Work','id':'$SALES_WORK'}])
a += [command('e1-delivery-confirm','recordDelivery',{'observationId':'$DELIVERY_OBSERVATION','canonicalOccurrenceId':'$e1-delivery-original.canonical'},intent='RECORD',revision=1,bind={'DELIVERY':'/effects/deliveryId'},refs=[{'type':'Work','id':'$SALES_WORK'}]),
 {'id':'e1-delivery-revision','type':'query','capability':'getObject','request':{'id':'$DELIVERY','scope':{'organizationId':'$ORG','objectType':'Delivery'},'asOf':T,'knownAt':T},'bind':{'DELIVERY_REV':'/data/revision'}},
 command('e1-return-authorize10','authorizeReturn',{'deliveryId':'$DELIVERY','startQuantity':'0','quantity':'10','unit':'BOX','destinationId':'$W','validUntil':'2026-10-31T00:00:00Z','reason':'실제 고객 반품10 접수'},actor='supervisor',revision='$DELIVERY_REV',refs=[{'type':'Work','id':'$SALES_WORK'}],bind={'RETURN_AUTHORIZATION':'/effects/authorizationId'}),
 {'id':'RETURN_EVENT','type':'uuid','alias':'RETURN_EVENT'},
 command('e1-return-intake10','receiveReturn',{'authorizationId':'$RETURN_AUTHORIZATION','eventId':'$RETURN_EVENT','occurredAt':T,'nextCheckAt':NEXT},intent='RECORD',bind={'RETURN':'/effects/returnId'})]
ret={'returnId':'$RETURN','kind':'RETURN_RECEIPT','eventId':'$RETURN_EVENT','deliveryId':'$DELIVERY','customerId':'$C','itemId':'$P','lotId':'$L','rangeRootId':'$DISPATCH_RANGE','startQuantity':'0','quantity':'10','unit':'BOX','placeId':'$W','workId':'$SALES_WORK','occurredAt':T}
a += [original('e1-return-original','RETURN_RECEIPT',ret,'$RETURN','10',subject='RETURN',subjectid='$RETURN',work='$SALES_WORK')]
a += link('e1-return-original','$RETURN','10',refs=[{'type':'Work','id':'$SALES_WORK'}])
a += [command('e1-return-confirm10','receiveReturn',{'returnId':'$RETURN','canonicalOccurrenceId':'$e1-return-original.canonical','nextCheckAt':NEXT},intent='RECORD',refs=[{'type':'Work','id':'$SALES_WORK'}],bind={'RETURN_RECEIPT':'/effects/receiptId'})]
write('e1-sales-return.json',{'schemaVersion':'1.0.0','status':'NOT_RUN','actions':a})
# Invoice40 at EUR200.125 against actual receipt40 at EUR200 produces money gap5.
# The second original preserves byte-for-byte content while binding the new invoice noun.
inv={'sourceNamespace':'synthetic-invoice-e1','sourceKey':'invoice40-receipt40','sourceVersion':'1','lineId':'$LINE','referenceId':'$receipt40.canonical','scopeKind':'PURCHASE','invoiceKind':'COMMERCIAL','itemId':'$P','quantity':'40','unit':'BOX','unitPrice':'200.125','originalAmount':'8005','currency':'EUR','occurredAt':T}
b=[original('e1-invoice-input','INVOICE',inv,'$LINE','40',subject='PURCHASE_ORDER_LINE',subjectid='$LINE'),
 {'id':'e1-purchase-line-revision','type':'observe','bind':{'PURCHASE_LINE_REV':'/rawRows/mulino_trade_purchase_orderlines/0/revision'}},
 command('e1-record-invoice40','recordInvoice',{'invoiceKind':'COMMERCIAL','originalAmount':{'value':'8005','currency':'EUR','provenance':'USER'},'line':{'value':{'purchaseLineId':'$LINE','receiptOccurrenceId':'$receipt40.canonical','quantity':{'value':'40','unit':'BOX'},'unitPrice':{'value':'200.125','currency':'EUR'}},'provenance':'USER'},'evidence':'$e1-invoice-input.document'},intent='RECORD',revision='$PURCHASE_LINE_REV',bind={'INVOICE':'/invoiceId'}),
 original('e1-invoice-original','INVOICE',inv,'$INVOICE','40',subject='INVOICE',subjectid='$INVOICE')]
b += link('e1-invoice-original','$INVOICE','40')
b += [command('e1-match-invoice-gap5','matchInvoice',{'invoiceId':'$INVOICE','canonicalOccurrenceId':'$e1-invoice-original.canonical','evidence':'$e1-invoice-original.document'},revision=1,assertions=[{'pointer':'/quantityDifference','operator':'equals','expected':'0'},{'pointer':'/originalDifference','operator':'equals','expected':'5'},{'pointer':'/businessStatus','operator':'equals','expected':'DIFFERENCE'},{'pointer':'/settlementResult','operator':'equals','expected':'UNSATISFIED'},{'pointer':'/bankEffect','operator':'equals','expected':'0'}])]
write('e1-settlement.json',{'schemaVersion':'1.0.0','status':'NOT_RUN','actions':b})
c=[{'id':'e1-current-sales-revision','type':'observe','bindRows':{'SALES_WORK_REV':{'pointer':'/rawRows/mulino_work_read_works','where':{'id':'$SALES_WORK'},'column':'revision'}}},command('e1-assess-sales30','assessGoal',{'workId':'$SALES_WORK'},revision='$SALES_WORK_REV',refs=[{'type':'Work','id':'$SALES_WORK'}],assertions=[{'pointer':'/assessment/outcome','operator':'equals','expected':'SATISFIED'}]),{'id':'e1-final-independent','type':'observe','assertions':[
 {'pointer':'/rawRows/mulino_inventory_quantitysegments','operator':'sum','column':'quantity','where':{'retiredat':None,'placeid':'$W'},'expected':'80'},
 {'pointer':'/rawRows/mulino_trade_receipt_receipts','operator':'sum','column':'contributedquantity','expected':'100'},
 {'pointer':'/rawRows/mulino_trade_sales_deliveries','operator':'sum','column':'quantity','expected':'30'},
 {'pointer':'/rawRows/mulino_trade_returns_receipts','operator':'sum','column':'quantity','expected':'10'},
 {'pointer':'/rawRows/mulino_trade_settlement_matches','operator':'sum','column':'originaldifference','expected':'5'},
 {'pointer':'/rawRows/mulino_trade_settlement_paymentreferences','operator':'size','expected':0},
 {'pointer':'/rawRows/mulino_work_read_obligationreferences','operator':'humanDuties','kinds':['QUALITY_REVIEW','RETURN_QC_REVIEW','RETURN_COMMERCIAL_REVIEW','RETURN_SETTLEMENT_REVIEW','SETTLEMENT_DIFFERENCE']} ]},
 {'id':'e1-current-warehouse','type':'query','capability':'getInventory','request':{'scope':{'organizationId':'$ORG','itemId':'$P','placeId':'$W'},'asOf':T,'knownAt':T},'assertions':[{'pointer':'/data/heldQuantity','operator':'equals','expected':'80'},{'pointer':'/data/eligibleQuantity','operator':'equals','expected':'0'}]},
 {'id':'e1-noun','type':'query','capability':'getObject','request':{'id':'$P','scope':{'organizationId':'$ORG','itemId':'$P'},'asOf':T,'knownAt':T},'bind':{'E1_SNAPSHOT':'/snapshotRevision','E1_WORK_IDS':'/data/workIds','E1_DUTIES':'/data/obligations','E1_CONTRIBUTIONS':'/data/contributions','E1_OWNERS':'/data/ownerIds','E1_EVIDENCE':'/data/evidenceRefs'}},
 {'id':'e1-verb','type':'query','capability':'getWork','request':{'id':'$WORK','scope':{'organizationId':'$ORG','itemId':'$P'},'asOf':T,'knownAt':T,'snapshotRef':'$E1_SNAPSHOT'},'assertions':[{'pointer':'/snapshotRevision','operator':'equals','expected':'$E1_SNAPSHOT'},{'pointer':'/data/workIds','operator':'equals','expected':'$E1_WORK_IDS'},{'pointer':'/data/obligations','operator':'equals','expected':'$E1_DUTIES'},{'pointer':'/data/contributions','operator':'equals','expected':'$E1_CONTRIBUTIONS'},{'pointer':'/data/ownerIds','operator':'equals','expected':'$E1_OWNERS'},{'pointer':'/data/evidenceRefs','operator':'equals','expected':'$E1_EVIDENCE'}]}]
write('e1-final.json',{'schemaVersion':'1.0.0','status':'NOT_RUN','actions':c})
