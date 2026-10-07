#!/usr/bin/env python3
"""Deterministic contract generation only. Never invokes a driver or a model."""
import copy, hashlib, json, pathlib
ROOT=pathlib.Path(__file__).resolve().parents[2]
OUT=ROOT/'verification/model-binding'
def write(p,d):
 p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def merge(a,b):
 r=copy.deepcopy(a)
 for k,v in b.items(): r[k]=merge(r[k],v) if isinstance(v,dict) and isinstance(r.get(k),dict) else copy.deepcopy(v)
 return r
corpus=ROOT/'verification/model-corpus/corpus.json'; D=json.loads(corpus.read_text()); H=hashlib.sha256(corpus.read_bytes()).hexdigest()
paths={a['path'] for c in D['cases'] for t in c['turns'] for a in t['oracle']['assertions']}
extra=set()
for c in D['cases']:
 for t in c['turns']:
  o=t['oracle']; extra.update(a['path'] for a in o.get('sitDirectCommand',{}).get('assertions',[]))
  for p in o.get('uatCompletion',{}).get('pathOracles',{}).values(): extra.update(a['path'] for a in p.get('assertions',[]))
# Observer rows are read-only physical column mappings, not oracle-shaped projections.
mappings=[]
for p in sorted(paths|extra):
 parts=p.split('.'); domain=parts[1] if len(parts)>1 else ''
 m={'semanticPath':p,'common':p in paths,'evidenceClass':{'response':'AUTHENTICATED_API','state':'INDEPENDENT_DB_ROWS','effects':'SCOPED_EFFECT_DELTA'}[parts[0]]}
 if parts[0]=='response': m.update(pointer='/response/'+ '/'.join(parts[1:]))
 elif parts[0]=='state':
  # Dataset/column identifiers are the adapter's versioned read-only mapping contract.
  m.update(dataset=domain,attribute='.'.join(parts[2:]) or 'value',selector='ISOLATED_INSTALLATION_AND_ACTUAL_ENTITY_SCOPE',cardinality='EXACTLY_ONE',sourceColumnsRequired=True)
 else: m.update(metric=parts[1],source='DISTINCT_NEW_EFFECT_IDS',scope='ORGANIZATION_INSTALLATION_TURN',unknownClasses='FORBIDDEN')
 if p in {'state.receipt.cumulative.value','state.receipt.distinctOccurrenceCount','state.recall.distinctProcessed.value','state.recall.unknown.value','state.activeChildren.quantities','state.activeChildren.total.value','state.evidence.documentCount','state.currentObligationCount','state.currentReturnObligationCount','state.followupWorkCount'}:
  dataset,attribute,reduce={
   'state.receipt.cumulative.value':('receiptOccurrence','quantity','uniquePhysicalSum'),
   'state.receipt.distinctOccurrenceCount':('receiptOccurrence','quantity','distinctPhysicalCount'),
   'state.recall.distinctProcessed.value':('recallProcessing','quantity','uniquePhysicalSum'),
   'state.recall.unknown.value':('recallUnknownScope','quantity','uniquePhysicalSum'),
   'state.activeChildren.quantities':('activeChildQuantity','quantity','collect'),
   'state.activeChildren.total.value':('activeChildQuantity','quantity','sum'),
   'state.evidence.documentCount':('evidenceDocument','id','count'),
   'state.currentObligationCount':('currentObligation','id','count'),
   'state.currentReturnObligationCount':('currentReturnObligation','id','count'),
   'state.followupWorkCount':('followupWork','id','count')
  }[p]
  m.update(dataset=dataset,attribute=attribute,reducer=reduce,cardinality='COMPLETE_SCOPED_ROWS')
 mappings.append(m)
write(OUT/'semantic-paths.json',{'schemaVersion':'1.0.0','mappingVersion':'model-binding-v1','corpusSha256':H,'commonPathCount':154,'paths':mappings})
entries=[]
for index,c in enumerate(D['cases']):
 cid=c['id']; F=merge(D['commonFixture'],c['fixture']); active=F['authentication']['actorRef']; profiles={}
 def profile(name,actorAlias,grantAlias):
  a=F['actors'][actorAlias]; g=F['grants'][grantAlias]
  return {'issuer':a['issuer'],'subject':a['subject'],'audience':a['audience'],'organizationAlias':a.get('organizationRef','org'),'roleCapabilities':a.get('roles',[]),'grant':{'delegatorAlias':a.get('delegatorRef',g.get('delegatorRef','manager')),'actions':g['actions'],'scope':{'organizationAlias':g.get('organizationRef','org'),'targetAliases':g['targetScope'],'grantAlias':grantAlias,'revokedAt':g.get('revokedAt'),'sourceNamespaces':g.get('sourceNamespaces',[g['sourceNamespace']] if 'sourceNamespace' in g else [])},'validFrom':g['validFrom'],'validUntil':g['validUntil'],'revision':g['revision']}}
 activeGrant=F['actors'][active]['grantRef']; profiles['command-actor']=profile('command-actor',active,activeGrant)
 readGrant=F['actors'][active].get('readGrantRef',activeGrant); profiles['read-probe-actor']=profile('read-probe-actor',active,readGrant)
 if readGrant!=activeGrant: profiles['read-probe-actor']['roleCapabilities']=['READ']
 responsibilities=[]
 for o in F.get('obligations',[]):
  if o.get('status')=='OPEN': responsibilities.append({'scope':{'kind':o['kind'],'targetAlias':o.get('scopeRef',o.get('rootRef','O1'))},'ownerAlias':o['ownerRef'],'supervisorAlias':F['policy']['supervisorRef'],'nextAction':o['nextAction'],'nextCheckAt':o['nextCheckAt']})
 facts={k:v for k,v in F.items() if k not in {'ids','authentication','actors','grants','businessClock','mergeRule','currentWriteAuthorization'}}
 fixture={'schemaVersion':'1.0.0','fixtureId':cid+'-isolated','synthetic':True,'baseRefs':[], 'clock':{'asOf':F['businessClock']['asOf'],'knownAt':F['businessClock']['knownAt'],'timezone':F['businessClock']['timezone'],'precision':'MILLISECONDS','deadlineInclusive':F['businessClock']['dueInclusive']},'versions':{'definition':F['definition']['activeVersion'],'evaluator':F['definition']['evaluatorVersion'],'policy':F['policy']['version']},'actors':profiles,'aliases':F['ids'],'baseline':{'domainFacts':facts,'principalFacts':F['actors'],'grantFacts':F['grants'],'sourceFixtureSha256':hashlib.sha256(json.dumps(F,sort_keys=True).encode()).hexdigest()},'evidence':[],'responsibilities':responsibilities}
 write(OUT/f'cases/{cid}/fixture.json',fixture)
 turns=[]
 feature=['# language: ko',f'기능: {cid} {c["title"]}',f'  시나리오: {cid}의 같은 fixture와 oracle를 SIT와 UAT로 확인한다',f'    먼저 model binding "{cid}"를 준비한다',f'    만일 model binding "{cid}"의 "install" 단계를 실행한다']
 for n,t in enumerate(c['turns'],1):
  tid=f'turn-{n}'; cap=t['expectedIntent']['capabilityId']; mapped='recordRelation' if cap=='linkRelation' else cap
  common=[{'assertionId':f'{cid}/{tid}/common-{j+1}', 'corpusAssertionPointer':f'/cases/{index}/turns/{n-1}/oracle/assertions/{j}', 'semanticPath':a['path']} for j,a in enumerate(t['oracle']['assertions'])]
  branches={}
  for branch, oracle in [('SIT_DIRECT_COMMAND',t['oracle'].get('sitDirectCommand',{})), *t['oracle'].get('uatCompletion',{}).get('pathOracles',{}).items()]:
   ptr=f'/cases/{index}/turns/{n-1}/oracle/'+('sitDirectCommand' if branch=='SIT_DIRECT_COMMAND' else 'uatCompletion/pathOracles/'+branch)
   branches[branch]=[{'assertionId':f'{cid}/{tid}/{branch}-{j+1}','corpusAssertionPointer':ptr+f'/assertions/{j}','semanticPath':a['path']} for j,a in enumerate(oracle.get('assertions',[]))]
  turns.append({'id':tid,'corpusTurnPointer':f'/cases/{index}/turns/{n-1}','capabilityMapping':{'semantic':cap,'public':mapped},'commandActorRef':'command-actor','readActorRef':'read-probe-actor','steps':['context','before','agent','after','assert'],'commonAssertions':common,'oracleAssertions':branches,'oracleRef':f'/cases/{index}/turns/{n-1}/oracle'})
  for step in turns[-1]['steps']: feature.append(f'    그리고 model binding "{cid}"의 "{tid}/{step}" 단계를 실행한다')
  for a in common: feature.append(f'    그리고 model binding "{cid}"의 "{a["assertionId"]}" oracle로 "{a["semanticPath"]}"를 확인한다')
 feature.append(f'    그러면 model binding "{cid}"의 모든 turn을 독립 관찰로 판정한다')
 (OUT/f'cases/{cid}/scenario.feature').write_text('\n'.join(feature)+'\n')
 binding={'schemaVersion':'1.0.0','caseId':cid,'corpusSha256':H,'corpusCasePointer':f'/cases/{index}','fixtureRef':f'verification/model-binding/cases/{cid}/fixture.json','featureRef':f'verification/model-binding/cases/{cid}/scenario.feature','profiles':['SIT','UAT'],'plannedRepeats':3,'turns':turns}
 write(OUT/f'cases/{cid}/binding.json',binding)
 entries.append({'caseId':cid,'bindingRef':f'verification/model-binding/cases/{cid}/binding.json','turnIds':[t['id'] for t in turns]})
write(OUT/'registry.json',{'schemaVersion':'1.0.0','baselineCommit':'feaca0af9673620eff9a5ac0f08a657ce14e9ccd','corpusRef':'verification/model-corpus/corpus.json','corpusSha256':H,'caseCount':60,'turnCount':73,'commonAssertionCount':221,'commonSemanticPathCount':154,'plannedRepeats':3,'cases':entries})
