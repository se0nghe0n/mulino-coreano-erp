#!/usr/bin/env python3
"""Author the coordinator-selected synthetic input layer, independently pinned to each unchanged original fixture."""
import argparse, copy, hashlib, json, pathlib, uuid, datetime
from decimal import Decimal
from inventory import ROOT, HERE, verify, sha
FLAGS={'originalInputProvided':False,'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}
def encoded(value):return json.dumps(value,ensure_ascii=False,separators=(',',':')).encode()
def digest(value):return hashlib.sha256(encoded(value)).hexdigest()
def write_generated(path,data):
 path.parent.mkdir(parents=True,exist_ok=True)
 if path.exists():path.chmod(path.stat().st_mode|0o200)
 path.write_bytes(data)
class Build:
 def __init__(self,path):
  self.file=path;self.f=json.loads(path.read_text());self.hash=sha(path);self.d=self.f['baseline']['domainFacts'];self.ids=dict(self.f['aliases']);self.case=path.parent.name
  self.out=dict(schemaVersion='model-installation-inputs-v1',fixtureSha256=self.hash,records=[],documentBytes={},typedReferences={},logicalCustody=[],completionLedger=[],versionTranslations=[],templateArtifacts=[],grantContents={},**FLAGS)
  self.out['authorityCorrection']={'version':'model-authority-correction-v1','capabilityContractSha256':sha(ROOT/'contracts/acceptance-capabilities.json'),'supplementalRead':True}
  self.out['staticClaims']={k:dict(sourcePointer='/baseline/domainFacts/'+k,originalClaim=copy.deepcopy(v),sourceClass='ORIGINAL_STATIC_CLAIM_UNVERIFIED',productFactClaimed=False,promptInputPermitted=False,**FLAGS) for k,v in self.d.items() if k in ['fx','selectionContext','selectedRefs','documentContent','actualLocationStatus','recallCandidates','contaminationConfirmed']}
  if self.case=='M59':
   self.out['quantityCorrection']=dict(version='model-unit-normalization-v1',fromUnit='BOX',toUnit='EA',factor='12')
   for a in ['A','B','O1','S1']:
    o=self.d['objects'][a]['goal'] if a=='O1' else self.d['objects'][a];o['quantity']=str(Decimal(o['quantity'])*12);o['unit']='EA'
    if 'allocated' in o:o['allocated']=str(Decimal(o['allocated'])*12)
  self.now=self.f['clock']['asOf'];self.until=self.d['objects']['O1']['goal']['dueAt'];self.expiry='2026-10-31T23:59:59Z';self.owner=self.d['policy']['intakeOwnerRef'];self.supervisor=self.d['policy']['supervisorRef']
  self.namespace='model-source-'+self.case;self.sourcepolicy='model-synthetic-source-v1';self.outdir=HERE/'generated'/self.case
 def id(self,a):return self.ids.setdefault(a,str(uuid.uuid5(uuid.NAMESPACE_URL,self.hash+':'+a)))
 def row(self,type,alias,fields,pointer='',part='',activate=False):
  r=dict(type=type,alias=alias,id=self.id(alias),sourcePointer=pointer,part=part,fields=fields,**FLAGS)
  if activate:r['activate']=True
  self.out['records'].append(r);return r
 def selected(self,pointer,before,after):self.out['completionLedger'].append(dict(pointer=pointer,before=before,after=after,inverse='REMOVE_EXACT_SYNTHETIC_ADDITION' if before is None else 'RESTORE_EXACT_BEFORE',**FLAGS))
 def at(self,seconds):return (datetime.datetime.fromisoformat(self.now.replace('Z','+00:00'))+datetime.timedelta(seconds=seconds)).isoformat().replace('+00:00','Z')
 def source(self,name,segment,kind,payload,quantity,unit,subject='SEGMENT',policy=None,at=None,canonicalAlias=None,place='W'):
  at=at or self.now;policy=policy or self.sourcepolicy;p=copy.deepcopy(payload);p.update(subjectKind=subject,subjectId=self.id(segment),itemId=self.id('P'),placeId=self.id(place),workId=self.id('O1'),effectiveFrom=at,quantity=quantity,unit=unit)
  self.outdir.mkdir(parents=True,exist_ok=True);file=self.outdir/(name+'.json');write_generated(file,encoded(p));hash=sha(file)
  scope=dict(recordedBy='synthetic:sourceActor',subjectKind=subject,subjectId=segment,itemId='P',placeId=place,workId='O1')
  doc=name+':doc';event=name+':event';claim=name+':claim';canonical=canonicalAlias or name+':canonical';verification=name+':verification'
  self.row('Document',doc,dict(**scope,sha256=hash,byteLength=file.stat().st_size,mediaType='application/json',sourceNamespace=self.namespace,sourceProfileId='synthetic:sourceProfile',sourceReference=name,availability='AVAILABLE',provenance=dict(source='coordinator-selected-synthetic-input',**FLAGS)))
  self.out['documentBytes'][doc]=str(file.relative_to(ROOT))
  temporal=dict(effectiveFrom=at,timeZone=self.f['clock']['timezone'],timePrecision='INSTANT',valueState='KNOWN')
  self.row('Event',event,dict(**scope,**temporal,kind=kind,sourceNamespace=self.namespace,sourceProfileId='synthetic:sourceProfile',externalEventId=name,sourceVersion='1',payloadHash=hash,payload=p))
  self.row('Inbox',name+':inbox',dict(**scope,eventId=event,sourceNamespace=self.namespace,sourceProfileId='synthetic:sourceProfile',externalEventId=name,sourceVersion='1',payloadHash=hash,state='RECEIVED',intakeOwnerId=self.owner,supervisorId=self.supervisor,nextAction='Retain synthetic source evidence',nextCheckAt=self.until))
  self.row('Claim',claim,dict(**scope,**temporal,sourceProfileId='synthetic:sourceProfile',eventId=event,documentVersionId=doc,assertion=p,quantity=quantity,unit=unit))
  self.row('CanonicalOccurrence',canonical,dict(**scope,**temporal,sourceProfileId='synthetic:sourceProfile',kind=kind,physicalScopeId=segment,occurrenceIdentity=self.id(name+':identity'),occurrenceSemanticHash=digest(p),quantity=quantity,unit=unit,reassessmentState='COMPLETE'))
  self.row('Verification',verification,dict(recordedBy='synthetic:sourceActor',claimId=claim,canonicalOccurrenceId=canonical,basisDocumentId=doc,policyVersion=policy,reason='Coordinator-selected independent synthetic original and exact source scope match'))
  self.row('EvidenceLink',name+':link',dict(recordedBy='synthetic:sourceActor',documentVersionId=doc,canonicalOccurrenceId=canonical,role='BASIS'))
  return canonical,doc
 def quality(self,segment,obj,category,action,allowed,part=None,validFrom=None,nameSuffix=''):
  validFrom=validFrom or self.now
  name=f'synthetic:{segment}:{category}:{action}'+nameSuffix;originalHold=next((h for h in self.d.get('holds',[]) if h.get('scopeRef')==segment and h.get('kind')==category and h.get('status')=='ACTIVE' and action=='SELL'),None);basisAlias=originalHold['ref'] if originalHold else name+':basis';type='DispositionBasis' if allowed else 'Restriction';operation='recordDispositionBasis' if allowed else 'placeHold';basis=self.id(name+':basis');decision=self.id(name+':decision')
  payload=dict(operation=operation,segmentId=self.id(segment),workId=self.id('O1'),action=action,category=category,startQuantity='0',quantity=obj['quantity'],unit=obj['unit'],validFrom=validFrom,validUntil=self.expiry)
  canonical,doc=self.source(name,segment,'QUALITY_DECISION_'+operation+'_'+category,payload,obj['quantity'],obj['unit'],at=validFrom)
  common=dict(controlScope='model:'+self.id(segment),action=action,state='ACTIVE',validFrom=validFrom,validUntil=self.expiry,decisionId=name+':decision',evidenceRef=self.id(doc),segmentId=segment,startQuantity='0',quantity=obj['quantity'],unit=obj['unit'],category=category,workId='O1',nextCheckAt=self.until,commandId=self.id(name+':command'),policyHash=digest(payload))
  self.row(type,basisAlias,common,'/baseline/domainFacts/objects/'+segment if part else '',part or '')
  self.row('QualityDecision',name+':decision',dict(actorId='synthetic:sourceActor',operation=operation,segmentId=segment,evidenceId=doc,policyHash=digest(payload),commandId=self.id(name+':command'),reason='Explicit synthetic '+category+' '+action+' decision',**({'dispositionBasisId':name+':basis'} if allowed else {'restrictionId':basisAlias})))
 def regulatory(self,segment,obj):
  validFrom=self.now
  for action in ['SELL','DISPATCH']:
   name=f'synthetic:{segment}:regulatory:{action}';policy='synthetic:regulatoryPolicy:'+action;proc=name+':procedure';prepared=name+':prepared';submitted=name+':submitted'
   if not any(r['alias']==policy for r in self.out['records']):self.row('RegulatoryPolicy',policy,dict(recordedBy='synthetic:sourceActor',version=self.sourcepolicy,authority='EXPLICIT_SYNTHETIC_MODEL_AUTHORITY',sourceNamespace=self.namespace,action=action,validFrom=self.now,validUntil=self.expiry,fictional=True,status='ACTIVE',requiresLabel=True))
   common=dict(recordedBy='synthetic:sourceActor',itemId='P',lotId=obj['lotRef'],physicalScopeId=segment,workId='O1',policyId=policy)
   self.row('RegulatoryProcedure',proc,dict(**common,currentVersionId=submitted))
   self.row('RegulatoryVersion',prepared,dict(**common,procedureId=proc,status='PREPARED',occurredAt=self.now))
   payload=dict(itemId=self.id('P'),lotId=self.id(obj['lotRef']),physicalScopeId=self.id(segment),authority='EXPLICIT_SYNTHETIC_MODEL_AUTHORITY',policyVersion=self.sourcepolicy,procedureId=self.id(proc),procedureVersionId=self.id(prepared),occurredAt=self.now)
   canonical,doc=self.source(name+':submission',segment,'REGULATORY_SUBMISSION',payload,obj['quantity'],obj['unit'],at=validFrom)
   self.row('RegulatoryVersion',submitted,dict(**common,procedureId=proc,previousVersionId=prepared,status='SUBMITTED',documentVersionId=doc,occurrenceId=canonical,occurredAt=self.now))
   payload.update(procedureVersionId=self.id(submitted),decision='ALLOWED',action=action,startQuantity='0',quantity=obj['quantity'],unit=obj['unit'],validFrom=validFrom,validUntil=self.expiry)
   canonical,doc=self.source(name+':allowance',segment,'REGULATORY_DECISION',payload,obj['quantity'],obj['unit'],at=validFrom)
   self.row('RegulatoryDecision',name+':decision',dict(recordedBy='synthetic:sourceActor',procedureId=proc,procedureVersionId=submitted,itemId='P',lotId=obj['lotRef'],physicalScopeId=segment,policyId=policy,action=action,decision='ALLOWED',authority='EXPLICIT_SYNTHETIC_MODEL_AUTHORITY',startQuantity='0',quantity=obj['quantity'],unit=obj['unit'],validFrom=self.now,validUntil=self.expiry,occurrenceId=canonical),'/baseline/domainFacts/objects/'+segment if action=='SELL' else '', 'regulatory' if action=='SELL' else '')
   label=dict(itemId=self.id('P'),lotId=self.id(obj['lotRef']),physicalScopeId=self.id(segment),authority='EXPLICIT_SYNTHETIC_MODEL_AUTHORITY',policyVersion=self.sourcepolicy,procedureId=self.id(proc),procedureVersionId=self.id(submitted),decision='VERIFIED',packagingVersionId=self.id('P:pack'),specificationVersionId=self.id('P:spec'),validFrom=self.now,validUntil=self.expiry)
   canonical,doc=self.source(name+':label',segment,'LABEL_VERIFICATION',label,obj['quantity'],obj['unit'])
   self.row('LabelVerification',name+':labelRecord',dict(recordedBy='synthetic:sourceActor',procedureId=proc,procedureVersionId=submitted,itemId='P',lotId=obj['lotRef'],physicalScopeId=segment,policyId=policy,packagingVersionId='P:pack',specificationVersionId='P:spec',decision='VERIFIED',validFrom=self.now,validUntil=self.expiry,occurrenceId=canonical))
 def build(self):
  convpath=ROOT/'verification/actual/scenarios/installer-conventions.json';conv=json.loads(convpath.read_text());self.out['templateArtifacts']=[dict(path=str(convpath.relative_to(ROOT)),sha256=sha(convpath)),dict(path='contracts/acceptance-capabilities.json',sha256=sha(ROOT/'contracts/acceptance-capabilities.json'))]
  for v in sorted(set(self.d['definition']['supportedVersions']+[self.d['objects']['O1']['definitionVersion']])):
   content=copy.deepcopy(conv['definition'])
   if self.case=='M59':
    before=copy.deepcopy(content['goals'])
    for g in content['goals']:
     predicate=g.get('predicate',{})
     if predicate.get('unit')=='BOX':predicate['unit']='EA'
     if predicate.get('minimum',{}).get('unit')=='BOX':predicate['minimum'].update(value=str(Decimal(predicate['minimum']['value'])*12),unit='EA')
    self.selected('/synthetic/definition/'+v+'/goals',before,copy.deepcopy(content['goals']))
   alias='synthetic:definition:'+v;evaluator='core-v1' if v in self.d['definition']['supportedVersions'] else self.d['definition']['evaluatorVersion'];content.update(version=v,evaluatorVersion=evaluator,schemaVersion='1.0.0',organizationId=self.id('org'),id=self.id(alias));pointer='/baseline/domainFacts/definition' if v==self.d['objects']['O1']['definitionVersion'] else ''
   self.row('DefinitionVersion',alias,dict(version=v,state='PUBLISHED',contentHash=digest(content),content=content,evaluatorVersion=evaluator,schemaVersion='1.0.0'),pointer)
  self.out['versionTranslations']=[dict(sourcePointer='/baseline/domainFacts/definition/evaluatorVersion',before=self.d['definition']['evaluatorVersion'],after='core-v1',inverse='RESTORE_EXACT_BEFORE',**FLAGS),dict(sourcePointer='/baseline/domainFacts/definition/schemaVersion',before=self.d['definition']['schemaVersion'],after='1.0.0',inverse='RESTORE_EXACT_BEFORE',**FLAGS)]
  for policy in conv['policies']:
   alias='synthetic:policy:'+policy['kind'];pointer='/baseline/domainFacts/policy' if policy['kind']=='COMMAND' else ''
   self.row('PolicyVersion',alias,dict(version=policy['version'],kind=policy['kind'],content=policy['content'],contentHash=digest(policy['content']),effectiveFrom=self.now,effectiveUntil=self.expiry),pointer,activate=True)
  for alias in ['synthetic:sourceActor','synthetic:supplierOwner','synthetic:orgCustodian','synthetic:customerCustodian']:
   self.row('Principal',alias,dict(kind='HUMAN' if alias in ['synthetic:sourceActor','synthetic:orgCustodian'] else 'EXTERNAL',stableRequestOwner=self.id(alias)))
  self.out['logicalCustody']=[dict(representativeActorId=self.id('synthetic:orgCustodian'),representsType='Organization',representsId=self.id('org'),capabilityCount=0,grantCount=0,**FLAGS),dict(representativeActorId=self.id('synthetic:customerCustodian'),representsType='Customer',representsId=self.id('C'),capabilityCount=0,grantCount=0,**FLAGS),dict(representativeActorId=self.id('synthetic:supplierOwner'),representsType='SyntheticSupplier',capabilityCount=0,grantCount=0,**FLAGS)]
  setup=['attachEvidence','matchSourceIdentity','recordDispositionBasis','placeHold','prepareRegulatoryProcedure','recordSubmission','recordRegulatoryDecision','verifyLabel']
  self.row('Membership','synthetic:sourceMembership',dict(actorId='synthetic:sourceActor',validFrom=self.now,validUntil=self.expiry))
  for cap in setup:self.row('CapabilityAssignment','synthetic:sourceCapability:'+cap,dict(actorId='synthetic:sourceActor',capabilityId=cap,scopeKind='ORGANIZATION',scopeId='org',validFrom=self.now,validUntil=self.expiry))
  self.row('Grant','synthetic:sourceGrant',dict(actorId='synthetic:sourceActor',delegatorId='synthetic:sourceActor',validFrom=self.now,validUntil=self.expiry))
  self.out['grantContents']['synthetic:sourceGrant']=dict(actions=setup,scope={'ORGANIZATION':['org'],'TARGET':[a for a,o in self.d['objects'].items() if o['type'] in ['TradeItem','QuantitySegment','Work','Place']],'SOURCE':[self.namespace]},purpose='SYNTHETIC_SOURCE_SETUP')
  self.row('SourceProfile','synthetic:sourceProfile',dict(recordedBy='synthetic:sourceActor',namespace=self.namespace,policyVersion=self.sourcepolicy,intakeOwnerId=self.owner,supervisorId=self.supervisor,nextAction='Retain independently authored synthetic source originals',nextCheckAt=self.until))
  for namespace,s in self.d['sources'].items():self.row('SourceProfile','source:'+namespace,dict(recordedBy='synthetic:sourceActor',namespace=namespace,policyVersion=self.sourcepolicy,intakeOwnerId=self.owner,supervisorId=self.supervisor,nextAction='Original source metadata remains unverified until exact bytes arrive',nextCheckAt=self.until),'/baseline/domainFacts/sources/'+namespace)
  for a,doc in self.d['evidence'].items():self.row('Document',a,dict(recordedBy='synthetic:sourceActor',subjectKind='ITEM',subjectId='P',itemId='P',sourceProfileId='source:'+doc['sourceNamespace'],byteLength=0,mediaType='application/octet-stream',provenance=dict(source='original-fixed-hash-only',originalClaimAvailable=doc['available'],originalBytesProvided=False,**FLAGS)),'/baseline/domainFacts/evidence/'+a)
  for alias,obj in self.d['objects'].items():
   p='/baseline/domainFacts/objects/'+alias;typ=obj['type']
   if self.case=='M50' and alias=='A':
    self.out['physicalCorrection']=dict(version='model-M50-original-reference-only-v1',sourcePointer=p,originalSnapshot=copy.deepcopy(obj),originalId=self.id('A'),originalIdentityPreservedInProduct=False,correctedGiven=True,coordinatorSelectedSynthetic=True,**FLAGS);continue
   if typ=='TradeItem':
    product=alias+':product';spec=alias+':spec';pack=alias+':pack';manufacturer=obj['issuer'];self.row('Manufacturer',manufacturer,dict(name='Explicit synthetic manufacturer '+manufacturer),p,'manufacturer')
    self.row('Product',product,dict(name=obj['name']))
    for t,a in [('SpecificationVersion',spec),('PackagingVersion',pack)]:
     content=dict(sourceItemRef=alias,baseUnit=obj['baseUnit'],scale=obj['scale'],**FLAGS);self.row(t,a,dict(productId=product,version='model-synthetic-v1',contentHash=digest(content),description=encoded(content).decode()))
    self.row('TradeItem',alias,dict(productId=product,specificationVersionId=spec,packagingVersionId=pack),p)
    self.row('ExternalIdentifier',alias+':identifier',dict(namespace='SKU',validFrom=self.now),p,'identifier')
    for conversion in obj.get('conversion',{}):self.row('UnitConversion',alias+':conversion:'+conversion,dict(validFrom=self.now),p,'conversion:'+conversion)
   elif typ=='Place':self.row(typ,alias,dict(kind='INTERNAL_STORAGE'),p)
   elif typ=='ManufacturerLot':self.row(typ,alias,dict(expiresAt=self.expiry),p);self.selected(p+'/expiresAt',None,self.expiry)
   elif typ=='QuantitySegment':
    temporal={} if obj['active'] else dict(retiredAt=self.at(-1),retirementRecordedAt=self.now)
    self.row(typ,alias,dict(**temporal,custodianId='synthetic:customerCustodian' if alias=='B' else 'synthetic:orgCustodian',ownerId='synthetic:customerCustodian' if alias=='B' else 'synthetic:supplierOwner',controlScope='model:'+self.id(alias),validFrom=self.now,mixtureStatus='IDENTIFIED'),p)
    if obj.get('regulatory')=='ALLOWED':self.regulatory(alias,obj)
    for action in ['SELL','DISPATCH']:
     self.quality(alias,obj,'QC',action,obj.get('qc')=='PASS','qc' if action=='SELL' else None)
     self.quality(alias,obj,'COMMERCIAL',action,obj.get('disposition') not in ['CUSTOMER_CUSTODY_SELL_DENIED','UNKNOWN'],'disposition' if action=='SELL' and obj.get('disposition')!='UNKNOWN' else None)
     self.quality(alias,obj,'CUSTOMER',action,True)
   elif typ=='Work':
    definition='synthetic:definition:'+obj['definitionVersion'];goal=alias+':goal';self.row('Work',alias,dict(itemId='P',kind='PURCHASE_ARRIVAL',supervisorId=self.supervisor,lifecycleMode='COMMAND',nextAction='Verify source-backed receipt and remaining quantity',nextCheckAt=self.until,currentGoalVersionId=goal),p)
    self.row('Goal',goal,self.goal(alias,definition,obj['goal']['quantity'],obj['goal']['unit'],'ARRIVED'),p,'goal')
   elif typ=='SalesOrder':
    work=alias+':deliveryWork';revision=alias+':revision';definition='synthetic:definition:'+self.d['objects']['O1']['definitionVersion'];goal=alias+':deliveryGoal'
    self.row('Work',work,dict(itemId=obj['itemRef'],definitionVersionId=definition,kind='DELIVERY',status='DRAFT',ownerId=obj['ownerRef'],supervisorId=self.supervisor,lifecycleMode='COMMAND',nextAction='Verify source-backed delivery and remaining quantity',nextCheckAt=self.until,currentGoalVersionId=goal),p,'work')
    self.row('Goal',goal,self.goal(work,definition,obj['quantity'],obj['unit'],'DELIVERED'),p if 'goal' in obj else '', 'goal' if 'goal' in obj else '')
    self.row('SalesOrder',alias,dict(workId=work),p)
    self.row('SalesRevision',revision,dict(orderId=alias,workId=work,customerId=obj['customerRef'],revisionNumber=obj['revision'],reason='Coordinator-selected synthetic initial sales contract'),p,'revision')
    self.row('SalesLine',alias+':line',dict(revisionId=revision,workId=work,dueAt=self.until,destinationId='W',qualityTerms='Synthetic contract: current QC, regulatory, label, disposition and customer eligibility required',packageTerms='Original trade-item packaging; no substitution'),p,'line')
    self.out['typedReferences'][alias]={'SalesOrder':self.id(alias),'Work':self.id(work),'SalesOrderLine':self.id(alias+':line'),'sourcePointer':p,**FLAGS}
  allocation=self.d.get('allocation',{})
  if allocation.get('ref') and allocation.get('scopeRef') and allocation.get('orderRef'):
   extra=dict(pickedAt=self.at(-1)) if self.case=='M11' else {}
   self.row('Allocation',allocation['ref'],dict(**extra,rootId=allocation['ref'],segmentId=allocation['scopeRef'],orderLineId=allocation['orderRef']+':line',startQuantity='0',action='SELL',customerId=self.d['objects'][allocation['orderRef']]['customerRef'],workId=allocation['orderRef']+':deliveryWork',authorizationActorId='actor',nextValidityBoundary=self.until,commandId=self.id('synthetic:allocationCommand')),'/baseline/domainFacts/allocation')
  if self.case in ['M12','M51']:self.dispatch_genesis()
  if self.case in ['M17','M43']:self.c2_history()
  if self.case=='M50':self.prior_receipt()
  for i,obligation in enumerate(self.d['obligations']):
   p=f'/baseline/domainFacts/obligations/{i}';a='obligation:'+str(i);scope=dict(targetId=self.id(obligation['scopeRef']));q=obligation.get('quantity');fields=dict(workId='O1',supervisorId=self.supervisor,scopeJson=scope,valid=True)
   self.row('Obligation',a,fields,p)
  from selected_cases import build as build_selected_cases
  build_selected_cases(self)
  self.current_policy_restrictions()
  self.out['completionLedger'] += [dict(pointer=r['sourcePointer'],part=r['part'],selectedRecordId=r['id'],inverse='DELETE_EXACT_INSERTED_ROW',**FLAGS) for r in self.out['records']]
  return self.out
 def current_policy_restrictions(self):
  policy=self.d['policy'];restrictions=[]
  if policy.get('currentSell')=='DENIED':restrictions.append(('currentSell','COMMERCIAL','SELL'))
  if policy.get('recallHold')=='ACTIVE':
   restrictions.extend([('recallHold','RECALL_INVESTIGATION','SELL'),('recallHold','RECALL_INVESTIGATION','DISPATCH')])
  segments=[r for r in self.out['records'] if r['type']=='QuantitySegment' and not r['fields'].get('retiredAt')]
  for part,category,action in restrictions:
   for segment in segments:
    a=segment['alias'];o=self.d['objects'].get(a,segment['fields'])
    self.quality(a,o,category,action,False,nameSuffix=':policy:'+part)
    r=self.out['records'][-2];r.update(sourcePointer='/baseline/domainFacts/policy',part=part+':'+a+':'+action)
 def prior_receipt(self):
  name='synthetic:M50:receipt';observation=name+':observation';quantity='60';unit='BOX';at=self.at(-1)
  canonical,doc=self.source(name,'A','PHYSICAL_RECEIPT',dict(lotId=self.id('L'),rangeRootId=self.id('A'),startQuantity='0',originalReferenceOnlyPhysicalRoot=True),quantity,unit,at=at)
  self.row('ReceiptObservation',observation,dict(revision=0,occurredAt=at,eventId=name+':event',rangeRootId='A',startQuantity='0',quantity=quantity,unit=unit,itemId='P',lotId='L',placeId='W',identificationStatus='CLAIMED',state='PROVISIONAL',workId='O1',ownerId=self.d['objects']['O1']['ownerRef'],supervisorId=self.supervisor,nextAction='Confirm the independently sourced synthetic receipt',nextCheckAt=self.until))
  self.out['priorReceipt']=dict(version='model-M50-prior-receipt-v1',observationId=self.id(observation),observationRevision=0,canonicalOccurrenceId=self.id(canonical),documentId=self.id(doc),grantId=self.id('synthetic:M50:confirmGrant'),delegatorGrantId=self.id('synthetic:M50:confirmRoot'),commandIdempotencyKey=self.d['receipt']['commandIdempotencyKey'],originalCanonicalHash=self.d['receipt']['canonicalHash'],originalCanonicalPayload=copy.deepcopy(self.d['receipt']['canonicalPayload']),originalReceiptObservationProvided=False,coordinatorSelectedSynthetic=True,originalAuthorityClaimed=False)
 def c2_history(self):
  monday='2026-10-05T03:00:00Z';tuesday='2026-10-06T03:00:00Z';wednesday='2026-10-07T02:00:00Z';p='/baseline/domainFacts/c2';place='synthetic:c2:transitPlace';transit='synthetic:c2:transit60'
  for row in self.out['records']:
   if row['alias']=='A':row['fields'].update(validFrom=monday,retiredAt=tuesday,retirementRecordedAt=self.now)
   if row['alias']=='B':row['fields'].update(validFrom=wednesday)
   if row['alias']=='O1:goal':row['fields']['slotsJson'].update(periodStart='2026-10-05T00:00:00Z')
  self.row('Place',place,dict(name='Coordinator-selected synthetic c2 transit',kind='TRANSIT'))
  self.row('QuantitySegment',transit,dict(itemId='P',lotId='L',identificationStatus='CONFIRMED',quantity='60',unit='BOX',placeId=place,custodianId='synthetic:orgCustodian',ownerId='synthetic:supplierOwner',controlScope='model:'+self.id('A'),validFrom=tuesday,mixtureStatus='IDENTIFIED'))
  self.row('Genealogy','synthetic:c2:dispatchEdge',dict(sourceId='A',targetId=transit,sourceStartQuantity='0',targetStartQuantity='0',quantity='60',unit='BOX',kind='DISPATCH',uncertain=False,occurredAt=tuesday))
  for alias,quantity,at in [('A','60',monday),('B','40',wednesday)]:
   name='synthetic:c2:receipt:'+alias;observation=name+':observation';canonical,doc=self.source(name,alias,'PHYSICAL_RECEIPT',dict(segmentId=self.id(alias),rangeRootId=self.id(alias),startQuantity='0',identificationStatus='CONFIRMED',coordinatorSelectedSynthetic=True,originalTimesProvided=False),quantity,'BOX',at=at)
   self.row('ReceiptObservation',observation,dict(occurredAt=at,eventId=name+':event',rangeRootId=alias,startQuantity='0',quantity=quantity,unit='BOX',itemId='P',lotId='L',placeId='W',identificationStatus='CLAIMED',state='CONFIRMED',workId='O1',ownerId=self.owner,supervisorId=self.supervisor,nextAction='Retain synthetic historical receipt source',nextCheckAt=self.until))
   self.row('Receipt',name+':receipt',dict(occurredAt=at,observationId=observation,canonicalOccurrenceId=canonical,rangeRootId=alias,startQuantity='0',quantity=quantity,unit='BOX',itemId='P',lotId='L',placeId='W',segmentId=alias,workId='O1',contributedQuantity=quantity,excessQuantity='0',physicalEffect='NEW_STOCK'),p,'mondayArrived' if alias=='A' else 'wednesdayArrived')
   self.row('Movement',name+':movement',dict(targetId=alias,quantity=quantity,unit='BOX',kind='RECEIPT',occurredAt=at,evidenceRef=self.id(canonical)))
  canonical,doc=self.source('synthetic:c2:dispatch','A','PHYSICAL_DISPATCH',dict(transitSegmentId=self.id(transit),coordinatorSelectedSynthetic=True,originalTimesProvided=False),'60','BOX',at=tuesday)
  self.row('Movement','synthetic:c2:dispatchMovement',dict(sourceId='A',targetId=transit,quantity='60',unit='BOX',kind='DISPATCH',occurredAt=tuesday,evidenceRef=self.id(doc)),p,'tuesdayDispatched')
  self.selected(p+'/occurredAt',None,dict(monday=monday,tuesday=tuesday,wednesday=wednesday,originalTimesProvided=False,coordinatorSelectedSynthetic=True))
  self.selected('/baseline/domainFacts/objects/O1/goal/periodStart',None,'2026-10-05T00:00:00Z')
 def dispatch_genesis(self):
  for row in self.out['records']:
   if row['alias']=='A':row['fields']['validFrom']=self.at(-1)
  d=self.d['dispatch'];p='/baseline/domainFacts/dispatch';unit=self.d['objects']['A']['unit'];parent='synthetic:dispatchParent';transit='synthetic:dispatchTransit';place='synthetic:transitPlace';at=self.at(-1);work='S1:deliveryWork';cargo='synthetic:D1:cargo';source='synthetic:D1:source'
  self.row('Place',place,dict(name='Coordinator-selected synthetic domestic transit',kind='TRANSIT'))
  common=dict(itemId='P',lotId='L',identificationStatus='CONFIRMED',unit=unit,custodianId='synthetic:orgCustodian',ownerId='synthetic:supplierOwner',controlScope='model:'+self.id(parent),mixtureStatus='IDENTIFIED')
  self.row('QuantitySegment',parent,dict(**common,quantity='60',placeId='W',validFrom=self.at(-2),retiredAt=at,retirementRecordedAt=self.now))
  self.row('QuantitySegment',transit,dict(**common,quantity='30',placeId=place,validFrom=at))
  canonical,doc=self.source(source,parent,'PHYSICAL_DISPATCH',dict(dispatchId=self.id('D1'),allocationId=self.id('AL1'),cargoScopeId=self.id(cargo),rangeRootId=self.id(parent),startQuantity='30',transitSegmentId=self.id(transit),destinationId=self.id('W'),coordinatorSelectedTemporalGenesis=True),'30',unit,at=at)
  for target,start in [('A','0'),(transit,'30')]:
   self.row('Genealogy','synthetic:dispatchEdge:'+target,dict(sourceId=parent,targetId=target,sourceStartQuantity=start,targetStartQuantity='0',quantity='30',unit=unit,kind='DISPATCH' if target==transit else 'SPLIT',uncertain=False,occurredAt=at,evidenceRef=self.id(doc)))
  self.row('Movement','synthetic:dispatchMovement',dict(sourceId=parent,targetId=transit,quantity='30',unit=unit,kind='DISPATCH',occurredAt=at,evidenceRef=self.id(doc),commandId=self.id('synthetic:D1:command')))
  self.row('Allocation','AL1',dict(rootId=parent,segmentId=parent,orderLineId='S1:line',quantity='30',unit=unit,state='CONSUMED',startQuantity='30',action='SELL',customerId='C',workId=work,authorizationActorId='synthetic:sourceActor',pickedAt=self.at(-2),commandId=self.id('synthetic:D1:command')),p,'allocationStatus')
  self.row('Dispatch','D1',dict(cargoScopeId=cargo,allocationId='AL1',salesLineId='S1:line',customerId='C',workId=work,itemId='P',lotId='L',rangeRootId=parent,startQuantity='30',quantity='30',unit=unit,transitSegmentId=transit,destinationId='W',occurredAt=at,commandId=self.id('synthetic:D1:command'),evidenceRef=self.id(doc)),p)
  self.row('CargoScope',cargo,dict(dispatchId='D1',rangeRootId=parent,startQuantity='30',quantity='30',unit=unit,segmentId=transit,customerId='C',workId=work,commandId=self.id('synthetic:D1:command')))
  self.out['typedReferences']['D1']={'Dispatch':self.id('D1'),'CargoScope':self.id(cargo),'TransitQuantitySegment':self.id(transit),'sourcePointer':p,**FLAGS}
  if d.get('cargoScope'):self.out['typedReferences'][d['cargoScope']]={'CargoScope':self.id(cargo),'QuantitySegment':self.id(transit),'sourcePointer':p+'/cargoScope',**FLAGS}
  if self.case=='M51':
   report=self.d['deliveryReport'];canonical,doc=self.source('synthetic:M51:deliveryReport',transit,'PHYSICAL_DELIVERY',dict(dispatchId=self.id('D1'),cargoScopeId=self.id(cargo),rangeRootId=self.id(parent),startQuantity='30',stillTransport=report['stillTransport'],originalSourceNamespace=report['sourceNamespace']),report['quantity'],unit)
   for row in self.out['records']:
    if row['alias']==canonical:row.update(sourcePointer='/baseline/domainFacts/deliveryReport')
  self.selected(p+'/temporalGenesis',None,dict(parentId=self.id(parent),originalRemainingChildId=self.id('A'),newTransitChildId=self.id(transit),ranges=[['0','30'],['30','60']],sourceId=self.id(canonical)))
 def goal(self,work,definition,quantity,unit,endpoint):
  slots=dict(quantityMode='CUMULATIVE_EVENT',targetQuantity=quantity,unit=unit,endpoint=endpoint,scope={'itemId':self.id('P')},placeId=self.id('W'),dueAt=self.until,timezone=self.f['clock']['timezone'],evidencePolicyVersion=self.sourcepolicy,periodStart=self.now,periodEnd=self.until,periodStartInclusive=True,periodEndInclusive=True,deadlineInclusive=True,eventKind='PHYSICAL_RECEIPT' if endpoint=='ARRIVED' else 'PHYSICAL_DELIVERY',contributionScope={'itemId':self.id('P')},deduplication='CANONICAL_OCCURRENCE',conditions=[{'id':'arrived' if endpoint=='ARRIVED' else 'delivered'}],evaluatorVersion='core-v1')
  return dict(workId=work,definitionVersionId=definition,quantityMode='CUMULATIVE_EVENT',targetQuantity=quantity,unit=unit,endpoint=endpoint,scopeJson=slots['scope'],slotsJson=slots,provenanceJson=dict(source='coordinator-selected synthetic completion',**FLAGS),evidencePolicyVersion=self.sourcepolicy,timezone=self.f['clock']['timezone'],dueAt=self.until)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('cases',nargs='*');args=parser.parse_args();verify()
 ids=args.cases or [f'M{i:02}' for i in range(1,61)]
 for cid in ids:
  if not(cid.startswith('M') and len(cid)==3 and cid[1:].isdigit() and 1<=int(cid[1:])<=60):raise SystemExit('Invalid case ID')
  builder=Build(ROOT/f'verification/model-binding/cases/{cid}/fixture.json');out=builder.build();path=HERE/'inputs'/f'{cid}-isolated.json';write_generated(path,(json.dumps(out,ensure_ascii=False,indent=2)+'\n').encode());print(cid,len(out['records']),sha(path))
 verify()
if __name__=='__main__':main()
