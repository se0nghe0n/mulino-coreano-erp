#!/usr/bin/env python3
"""원 fixture bytes를 보존하며 coordinator가 채택한 synthetic Given을 분리한다."""
import copy,hashlib,json
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
SELECTED=('cancel-after-shipment','closed-followup')
def canonical(value):return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'))
def sha(value):return hashlib.sha256(canonical(value).encode()).hexdigest()
def completion(sid):
 original=json.loads((HERE/'fixtures'/f'{sid}.json').read_text());f=copy.deepcopy(original)
 provenance={'synthetic':True,'coordinatorSelectedSynthetic':True,'originalAuthorityClaimed':False,'operatingApprovalClaimed':False}
 if sid=='closed-followup':
  template=next(g for g in json.loads((ROOT/'verification/actual/scenarios/installer-conventions.json').read_text())['definition']['goals'] if g['name']=='held-at')
  predicate=copy.deepcopy(template['predicate']);predicate['minimum']['value']='100'
  f['baseline']['historicalGoalCompletion']={**provenance,'originalGoalModeProvided':False,'historicalEvaluationExecuted':False,'originalGoalLabel':'goal-closed-v1','workAlias':'PARENT','assessmentAlias':'OLDASSESS','definitionVersion':'definition-v1','template':template,'templateSha256':sha(template),'instantiatedPredicate':predicate,'predicateSha256':sha(predicate),'goal':{'quantityMode':'STATE_AT','targetQuantity':'100','unit':'BOX','endpoint':template['endpoint'],'evaluationAt':'2026-10-07T00:00:00Z','action':'PHYSICAL_HELD','includeReserved':True,'itemAlias':'P','lotAlias':'LOT','placeAlias':'W'},'originalFixtureSha256':hashlib.sha256((HERE/'fixtures'/f'{sid}.json').read_bytes()).hexdigest()}
 else:
  capabilities=['attachEvidence','recordActivity','matchSourceIdentity','linkCanonicalOccurrence','getEvidence','recordDispositionBasis','decideQuality','confirmCustomerConditions','confirmCommercialDisposition','prepareRegulatoryProcedure','recordSubmission','recordRegulatoryDecision','verifyLabel']
  actor='nativeR2Source';f['aliases'][actor]={'type':'Human'}
  f['actors'][actor]={'issuer':'synthetic-test-issuer','subject':'T11-native-r2-source','audience':'isolated-ontology','organizationAlias':'ORG','roleCapabilities':capabilities,'grant':{'delegatorAlias':actor,'actions':capabilities,'scope':{'organizationAlias':'ORG','itemAliases':['P']},'validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T23:59:59Z','revision':1}}
  namespace='synthetic-t11-native-r2';version='t11-native-r2-positive-v1'
  f['aliases']['LOT']['expiresAt']='2026-10-31T23:59:59Z'
  for action in ('SELL','DISPATCH'):
   f['aliases']['nativeR2Policy'+action]={'type':'RegulatoryPolicy','actorAlias':actor,'version':version,'authority':'FICTIONAL_T11_NATIVE_R2_AUTHORITY','sourceNamespace':namespace,'action':action,'validFrom':'2026-10-01T00:00:00Z','validUntil':'2026-10-31T23:59:59Z','fictional':True,'status':'ACTIVE','requiresLabel':True}
  f['baseline'].setdefault('sourceProfiles',[]).append({'namespace':namespace,'policyVersion':version,'reconciliationOwnerAlias':actor})
  original_root=f['baseline']['obligations'][0]
  document={**provenance,'recordType':'SYNTHETIC_HISTORICAL_OBLIGATION_GIVEN','sourceNamespace':namespace,'externalEventId':original_root['sourceOccurrence'],'sourceOccurrence':original_root['sourceOccurrence'],'sourceVersion':original_root['occurrenceVersion'],'originalObligation':copy.deepcopy(original_root),'originalAssignment':copy.deepcopy(f['baseline']['assignments'][0])}
  f['aliases']['nativeR2DutyDocument']={'type':'DocumentVersion','fixtureContent':document}
  f['baseline']['nativeR2LifecycleCompletion']={**provenance,'version':1,'actorAlias':actor,'namespace':namespace,'sourceVersion':'1','policyVersion':version,'sourceDocumentAlias':'nativeR2DutyDocument','sourceKind':'DOCUMENT','originalSourceOccurrence':original_root['sourceOccurrence'],'originalCurrentSaleConditions':copy.deepcopy(f['baseline']['policies'][0]['currentSaleConditions']),'actions':['SELL','DISPATCH'],'positiveConditions':['QC','CUSTOMER','COMMERCIAL','REGULATORY','LABEL'],'segmentAlias':'Q','workAlias':'SOURCE','quantity':'100','unit':'BOX','startQuantity':'0','validFrom':'2026-10-07T00:00:00Z','validUntil':'2026-10-31T23:59:59Z','originalFixtureSha256':hashlib.sha256((HERE/'fixtures'/f'{sid}.json').read_bytes()).hexdigest(),'lotExpiryCompletion':{**provenance,'applied':True,'originalExpiryProvided':False,'lotAlias':'LOT','expiresAt':'2026-10-31T23:59:59Z'},'additionalAuthority':'New isolated synthetic source identity; original actors/grants unchanged; no assessment authority added'}
 return original,f
def restore(sid,value):
 old,new=completion(sid)
 if value!=new:raise ValueError('T11 native-r2 fixture inverse refuses unrecorded delta')
 return old
def main():
 entries=[]
 for sid in SELECTED:
  old,new=completion(sid);p=HERE/'fixtures/native-r2'/f'{sid}.json';p.parent.mkdir(exist_ok=True)
  p.write_text(json.dumps(new,ensure_ascii=False,indent=2)+'\n')
  entries.append({'subcaseId':sid,'originalPath':f'verification/cases/T11/fixtures/{sid}.json','completedPath':str(p.relative_to(ROOT)),'originalBytesSha256':hashlib.sha256((HERE/'fixtures'/f'{sid}.json').read_bytes()).hexdigest(),'completedBytesSha256':hashlib.sha256(p.read_bytes()).hexdigest(),'original':old,'completed':new})
 (HERE/'native-r2-fixture-ledger.json').write_text(json.dumps({'recordType':'T11_NATIVE_R2_EXPLICIT_SYNTHETIC_COMPLETION','baseline':'fb58d86663f28ec90dd38322399c384a42de4672','historicalRuntimePassClaimed':False,'entries':entries},ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
