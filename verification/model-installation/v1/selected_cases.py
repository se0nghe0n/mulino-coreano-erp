"""Explicit coordinator selections. This module never reads the corpus, oracle or expected intent."""
import copy, hashlib, json
from decimal import Decimal

def canonical_hash(value):return hashlib.sha256(json.dumps(value,ensure_ascii=False,separators=(',',':'),sort_keys=True).encode()).hexdigest()

def purchase(b,price='1.00',order=False,pointer=''):
 proposal='synthetic:purchase:proposal';revision=proposal+':revision';approval=proposal+':approval';supplier='synthetic:supplierIT';at=b.at(-2)
 b.row('Supplier',supplier,dict(name='Coordinator-selected synthetic supplier-IT issuer representative'))
 fields=dict(proposalId=proposal,workId='O1',goalVersionId='O1:goal',itemId='P',supplierId=supplier,destinationId='W',quantity='100',unit='BOX',price=price,currency='EUR',dueAt=b.until,endpoint='ARRIVED',quantityMode='CUMULATIVE_EVENT',revision=1)
 keys=['proposalId','revision','workId','itemId','supplierId','destinationId','quantity','unit','price','currency','dueAt','endpoint','quantityMode']
 actual={k:str(fields[k]) if k not in ['proposalId','workId','itemId','supplierId','destinationId'] else b.id(fields[k]) for k in keys};hash=canonical_hash(actual)
 fields.update(proposalHash=hash,reason='Coordinator-selected completed synthetic purchase proposal')
 b.row('PurchaseProposal',proposal,dict(workId='O1',currentRevision=1,status='APPROVED' if not order else 'ORDERED',currentApprovalId=approval),pointer)
 b.row('PurchaseRevision',revision,fields)
 policy=next(r for r in b.out['records'] if r['alias']=='synthetic:policy:COMMAND');policyhash=policy['fields']['contentHash']
 grant='synthetic:purchase:approvalAuthority';valid=b.f['baseline']['grantFacts']['grant'];cap='approvePurchase'
 b.row('CapabilityAssignment',grant+':capability',dict(actorId='manager',capabilityId=cap,scopeKind='ORGANIZATION',scopeId='org',validFrom=valid['validFrom'],validUntil=valid['validUntil']))
 b.row('ManagementAuthority',grant+':management',dict(actorId='manager',capabilityId=cap,scopeKind='WORK',scopeId='O1',validFrom=valid['validFrom'],validUntil=valid['validUntil']))
 b.row('Grant',grant,dict(actorId='manager',delegatorId='manager',validFrom=valid['validFrom'],validUntil=valid['validUntil']))
 b.out['grantContents'][grant]=dict(actions=[cap],scope={'ORGANIZATION':['org'],'ITEM':['P'],'PLACE':['W'],'WORK':['O1'],'SUPPLIER':[supplier]},purpose='SYNTHETIC_PURCHASE_SETUP')
 b.row('PurchaseApproval',approval,dict(proposalId=proposal,proposalRevision=1,proposalHash=hash,approverId='manager',policyHash=policyhash,decision='APPROVE',conditionsJson=[],decidedAt=at,validUntil=b.until,consumptionPolicy='SINGLE_ORDER_REVISION'))
 b.selected('/baseline/domainFacts/proposal' if b.case=='M45' else '/synthetic/purchase',copy.deepcopy(b.d.get('proposal')),dict(proposalId=b.id(proposal),revisionId=b.id(revision),actualHashInput=actual,actualProposalHash=hash,approvalId=b.id(approval),originalSemanticHashStatus='ORIGINAL_REFERENCE_ONLY'))
 if not order:return proposal,revision,approval,None,None
 order='synthetic:purchase:order';line=order+':line'
 b.row('PurchaseOrder',order,dict(proposalId=proposal,proposalRevision=1,proposalHash=hash,approvalId=approval,channel='SYNTHETIC_HISTORICAL_REFERENCE_ONLY',externalOperationId=b.case+'-synthetic-prior-order',outboxId=b.id('synthetic:purchase:externalReference'),conditionAssessmentJson={'decision':'UNCONDITIONAL'}))
 b.row('PurchaseLine',line,dict(orderId=order,proposalId=proposal,proposalRevision=1,workId='O1',itemId='P',destinationId='W',quantity='100',unit='BOX',status='DISPATCHED'))
 return proposal,revision,approval,order,line

def build(b):
 if b.case in ['M03','M50']:
  grant=b.f['baseline']['grantFacts']['grant']
  for alias in [b.d['objects']['O1']['ownerRef'],b.supervisor]:
   mid='synthetic:'+b.case+':responsibilityMembership:'+alias
   b.row('Membership',mid,dict(actorId=alias,validFrom=grant['validFrom'],validUntil=grant['validUntil']))
   b.selected('/baseline/principalFacts/'+alias+'/membership',None,dict(membershipId=b.id(mid),originalMembershipProvided=False,coordinatorSelectedSynthetic=True,capabilitiesAdded=[],grantCountAdded=0,validFrom=grant['validFrom'],validUntil=grant['validUntil']))
 if b.case=='M03':
  at='2026-10-04T03:00:00Z';retired='2026-10-04T04:00:00Z'
  for alias in ['A','B']:
   obj=b.d['objects'][alias]
   for row in b.out['records']:
    if row['alias']==alias:row['fields'].update(validFrom=at,retiredAt=retired,retirementRecordedAt=b.now)
   name='synthetic:M03:disposal:'+alias;canonical,doc=b.source(name,alias,'PHYSICAL_DISPOSAL',dict(operation='DISPOSE',historicalSyntheticGenesis=True,notCurrentO1ReceiptContribution=True),obj['quantity'],obj['unit'],at=retired)
   b.row('Movement',name+':movement',dict(sourceId=alias,quantity=obj['quantity'],unit=obj['unit'],kind='DISPOSE',occurredAt=retired,evidenceRef=b.id(doc),commandId=b.id(name+':command')))
   b.quality(alias,obj,'COMMERCIAL','DISPOSE',True,validFrom=at)
   b.selected('/baseline/domainFacts/objects/'+alias+'/inactiveHistory',None,dict(validFrom=at,retiredAt=retired,disposalQuantity=obj['quantity'],originalHistoryProvided=False,coordinatorSelectedSynthetic=True,canonicalOccurrenceId=b.id(canonical)))
  name='synthetic:M03:incomingReceipt';observation=name+':observation';quantity=b.d['receipt']['observed'];at=b.now
  canonical,doc=b.source(name,'A','PHYSICAL_RECEIPT',dict(lotId=b.id('L'),rangeRootId=b.id('A'),startQuantity='0',independentIncomingPhysicalReceipt=True),quantity,'BOX',at=at)
  b.row('ReceiptObservation',observation,dict(revision=0,occurredAt=at,eventId=name+':event',rangeRootId='A',startQuantity='0',quantity=quantity,unit='BOX',itemId='P',lotId='L',placeId='W',identificationStatus='CLAIMED',state='PROVISIONAL',workId='O1',ownerId=b.d['objects']['O1']['ownerRef'],supervisorId=b.supervisor,nextAction='Confirm source-backed synthetic incoming receipt',nextCheckAt=b.until),'/baseline/domainFacts/receipt')
  b.out['receiptAuthority']=dict(version='model-M03-confirm-authority-v1',observationId=b.id(observation),grantId=b.id('synthetic:M03:confirmGrant'),delegatorGrantId=b.id('synthetic:M03:confirmRoot'),originalReceiptObservationProvided=False,coordinatorSelectedSynthetic=True,originalAuthorityClaimed=False)
  b.out['typedReferences']['EV1']={'ReceiptObservation':b.id(observation),'CanonicalOccurrence':b.id(canonical),'Document':b.id(doc),'sourcePointer':'/baseline/domainFacts/receipt','originalInputProvided':False,'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}

 if b.case=='M45':purchase(b,pointer='/baseline/domainFacts/proposal')
 if b.case=='M04':
  proposal,revision,approval,order,line=purchase(b,order=True)
  # The reply is a verified incoming source. Recording SupplierReply is the later user command.
  canonical,doc=b.source('synthetic:M04:supplierReply',order,'SUPPLIER_ACCEPT',dict(reply='ACCEPT',acceptedQuantity=b.d['supplierReply']['acceptedQuantity'],originalLogicalIssuer='supplier-IT'),'100','BOX',subject='PURCHASE_ORDER')
  for row in b.out['records']:
   if row['alias']==canonical:row.update(sourcePointer='/baseline/domainFacts/supplierReply')
  b.selected('/baseline/domainFacts/supplierReply/sourceNamespace','supplier-IT',dict(newIndependentNamespace=b.namespace,originalSourceClaimStatus='UNVERIFIED',canonicalOccurrenceId=b.id(canonical),documentId=b.id(doc),purchaseOrderId=b.id(order),purchaseLineId=b.id(line)))
 if b.case=='M14':
  _,_,_,order,line=purchase(b,price='5.00',order=True)
  payload=dict(invoiceId=b.id('INV1'),invoiceKind='COMMERCIAL',scopeKind='PURCHASE',externalInvoiceReference='M14-synthetic-invoice-1',lineId=b.id(line),referenceId=b.id(order),itemId=b.id('P'),quantity='100',unit='BOX',unitPrice='5.00',originalAmount='500.00',currency='EUR')
  canonical,doc=b.source('synthetic:M14:invoice','O1','INVOICE',payload,'100','BOX',subject='WORK')
  hash=next(r['fields']['sha256'] for r in b.out['records'] if r['alias']==doc)
  b.row('Invoice','INV1',dict(workId='O1',itemId='P',lineId=line,referenceId=order,scopeKind='PURCHASE',invoiceKind='COMMERCIAL',sourceNamespace=b.namespace,sourceKey='M14-synthetic-invoice-1',sourceVersion='1',sourceHash=hash,evidenceId=doc,quantity='100',unit='BOX',unitPrice='5.00',originalAmount='500.00',currency='EUR',taxReferenceOnly=False),'/baseline/domainFacts/invoice')
 if b.case=='M05':
  b.out['readCompletion']=dict(version='model-M05-shipment-read-v1',grantId=b.id('synthetic:M05:readGrant'),rootGrantId=b.id('synthetic:M05:readRoot'),coordinatorSelectedSynthetic=True,originalAuthorityClaimed=False)
  origin='synthetic:ItalianSupplierPlace';port='synthetic:BusanPort';carrier='synthetic:carrierIT';cargo='synthetic:SH1:cargo';leg='synthetic:SH1:leg'
  b.row('Place',origin,dict(name='Synthetic Italian supplier place',kind='SUPPLIER'))
  b.row('Place',port,dict(name='BusanPort',kind='PORT'))
  b.row('Principal',carrier,dict(kind='EXTERNAL',stableRequestOwner=b.id(carrier)))
  b.row('Shipment','SH1',dict(recordedBy='synthetic:sourceActor',originId=origin,destinationId='W',carrierRef=b.id(carrier),workId='O1',state='PLANNED'),'/baseline/domainFacts/shipment')
  b.row('ShipmentCargo',cargo,dict(recordedBy='synthetic:sourceActor',shipmentId='SH1',itemId='P',plannedQuantity='100',unit='BOX'))
  b.row('ShipmentLeg',leg,dict(recordedBy='synthetic:sourceActor',shipmentId='SH1',sequence=1,originId=origin,destinationId=port,carrierRef=b.id(carrier),plannedDepartureAt=b.at(-7200),plannedArrivalAt=b.at(-3600)))
  canonical,doc=b.source('synthetic:M05:arrival','O1','SHIPMENT_ARRIVAL',dict(shipmentId=b.id('SH1'),cargoId=b.id(cargo),legId=b.id(leg),placeId=b.id(port),originalConfirmedPlace='BusanPort'),'100','BOX',subject='WORK',at=b.at(-3600),place=port)
  b.row('ShipmentLegEvent','synthetic:SH1:arrival',dict(recordedBy='synthetic:sourceActor',shipmentId='SH1',cargoId=cargo,legId=leg,kind='ARRIVAL',placeId=port,physicalScopeId='O1',occurrenceId=canonical,quantity='100',unit='BOX',occurredAt=b.at(-3600),detailStatus='CONFIRMED'))
  b.selected('/baseline/domainFacts/shipment/occurredAt',None,dict(arrival=b.at(-3600),originalTimesProvided=False,coordinatorSelectedSynthetic=True))
 if b.case=='M07':
  policy='synthetic:M07:policy';procedure='synthetic:M07:procedure';prepared=procedure+':prepared';p='/baseline/domainFacts/regulatory';at=b.d['regulatory']['submittedAt']
  b.row('RegulatoryPolicy',policy,dict(recordedBy='synthetic:sourceActor',version=b.sourcepolicy,authority='EXPLICIT_SYNTHETIC_MODEL_AUTHORITY',sourceNamespace=b.namespace,action='IMPORT',validFrom=b.at(-7200),validUntil=b.expiry,fictional=True,status='ACTIVE',requiresLabel=False))
  common=dict(recordedBy='synthetic:sourceActor',itemId='P',lotId='L',physicalScopeId='A',workId='O1',policyId=policy)
  b.row('RegulatoryProcedure',procedure,dict(**common,currentVersionId=prepared),p)
  b.row('RegulatoryVersion',prepared,dict(**common,procedureId=procedure,status='PREPARED',occurredAt=b.at(-7200)))
  canonical,doc=b.source('synthetic:M07:authorityReceipt','O1','REGULATORY_SUBMISSION',dict(authorityReceipt='fixture-receipt-7',submittedAt=at,procedure='O1',procedureId=b.id(procedure),procedureVersionId=b.id(prepared),synthetic=True,operationalAuthority=False),'60','BOX',subject='WORK',at=at)
  b.selected(p,None,dict(procedureId=b.id(procedure),preparedVersionId=b.id(prepared),receiptDocumentId=b.id(doc),canonicalOccurrenceId=b.id(canonical),originalSourceClaimStatus='UNVERIFIED'))
