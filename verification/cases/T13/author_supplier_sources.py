"""T13 세 supplier 원문 입력과 공개 source chain을 재현한다. Oracle를 읽지 않는다."""
import copy
import hashlib
import json
import subprocess
from pathlib import Path
from uuid import uuid5, NAMESPACE_URL

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE='bcac964ced0d5cf47a990afabba5eddefb66b2c8'
SELECTED={'supplier-accept','supplier-reject','supplier-change'}
ACTOR='t13Source'
NS='synthetic-t13-supplier-source'
POLICY='t13-synthetic-v1'
META='supplierSourceAuthoring'
PROVENANCE={'newSynthetic':True,'originalAuthorityClaimed':False,'operatingApprovalClaimed':False,'authoredBy':'coordinator-selected-fixture'}

def ref(action,field):return {'$result':{'actionId':action,'pointer':'/response/'+field}}
def alias(name):return {'$alias':name}
def encode(source,encoding):return {'$encode':{'source':copy.deepcopy(source),'encoding':encoding}}
def original(path):return json.loads(subprocess.check_output(['git','show',f'{BASE}:{path.relative_to(ROOT)}'],cwd=ROOT))

def command(sid,name,cap,slots,kind='COMMAND',revision=None):
 r={'intentKind':kind,'definitionVersion':'definition-v1','capabilityId':cap,'subjectRefs':[], 'slots':slots,'provenance':{k:'CONTEXT' for k in slots},'commandIdempotencyKey':f'T13-{sid}-{name}'}
 if revision is not None:r['expectedRevision']=revision
 return {'id':name,'kind':'invoke','actorRef':ACTOR,'route':'api','capabilityId':cap,'request':r,'evidenceRefs':[name+':actual-adapter-artifact']}

def chain(sid,prefix,subject,payload,kind,physical,quantity=None,unit=None):
 event=str(uuid5(NAMESPACE_URL,f'urn:mulino:T13:{sid}:{prefix}'))
 at='2026-10-07T09:00:00Z'
 doc=command(sid,prefix+'-document','attachEvidence',{'subject':subject,'sourceNamespace':NS,'sourceReference':event,'mediaType':'application/json','expectedHash':encode(payload,'sha256'),'contentBase64':encode(payload,'base64'),'provenance':'NEW_COORDINATOR_SYNTHETIC_SOURCE_NOT_ORIGINAL_AUTHORITY_OR_OPERATING_APPROVAL'},'RECORD')
 slots={'subject':subject,'kind':kind,'sourceNamespace':NS,'externalEventId':event,'sourceVersion':'1','effectiveFrom':at,'timeZone':'UTC','timePrecision':'SECOND','valueState':'KNOWN','payload':encode(payload,'json'),'documentId':ref(doc['id'],'id'),'assertion':'새 synthetic 원문 주장이다. 현재 source/identity/quantity/time 대조로 검증한다.'}
 if quantity is not None:slots.update(quantity=quantity,unit=unit)
 ev=command(sid,prefix+'-event','recordActivity',slots,'RECORD')
 review={'claimId':ref(ev['id'],'claimId'),'basisDocumentId':ref(doc['id'],'id'),'physicalScopeId':physical,'policyVersion':POLICY,'sourceIdentity':NS+':'+event+':1','effectiveFrom':at,'reason':'명시한 synthetic 원문과 현재 typed 대상/수량/시간을 대조한다.'}
 if quantity is not None:review.update(quantity=quantity,unit=unit)
 match=command(sid,prefix+'-match','matchSourceIdentity',review,revision=1)
 link=command(sid,prefix+'-canonical','linkCanonicalOccurrence',{'reconciliationId':ref(match['id'],'id')},revision=1)
 return [doc,ev,match,link]

def repair_fixture(f):
 if META in f['baseline'].get('policy',{}):return f
 old=copy.deepcopy(f)
 source=f['actors']['source']
 # Source-only actor receives the evidence-read prerequisite; no business actor changes.
 for key in ['roleCapabilities']:
  source[key].append('getEvidence')
 source['grant']['actions'].append('getEvidence')
 actor=copy.deepcopy(old['actors']['source']);actor['subject']='t13-synthetic-source-human'
 caps=['attachEvidence','recordActivity','matchSourceIdentity','linkCanonicalOccurrence','getEvidence']
 actor['roleCapabilities']=caps;actor['grant']['actions']=caps.copy();actor['grant']['scope']['items']=['P']
 f['actors'][ACTOR]=actor;f['aliases'][ACTOR]={'type':'Human'}
 f['baseline']['sourceProfiles']=[{'namespace':NS,'reconciliationOwnerAlias':ACTOR,'recordActorAlias':ACTOR,'policyVersion':POLICY}]
 f['baseline'].setdefault('policy',{})[META]=dict(PROVENANCE,version=1,originalSourceActor=old['actors']['source'],originalSourceDescriptor=next(e for e in old['evidence'] if e['alias']=='supplier-ship'),addedActor=ACTOR,sourceReadReason='공개 reconciliation 및 canonical guard가 현재 getEvidence를 요구한다. 기존 source 전용 synthetic actor에 읽기만 추가한다.')
 return f

def restore_fixture(f):
 f=copy.deepcopy(f);m=f['baseline'].get('policy',{}).pop(META,None)
 if not m:return f

 if not f['baseline']['policy']:del f['baseline']['policy']
 f['actors']['source']=m['originalSourceActor'];del f['actors'][ACTOR];del f['aliases'][ACTOR];del f['baseline']['sourceProfiles']
 return f

def repair_case(c,ledger=None):
 if any(a['id']=='source-reply-document' for s in c['subcases'] for a in s['actions']):return c
 before=copy.deepcopy(c);ledgers=[]
 for sub in c['subcases']:
  sid=sub['id']
  if sid not in SELECTED:continue
  old=copy.deepcopy(sub);by={a['id']:a for a in sub['actions']}
  dispatch=by['dispatch'];trans=by['transmission'];reply=by['supplier-reply'];r=reply['request']['slots'];quantity=r['proposedQuantity']['value'];unit=r['proposedQuantity']['unit']
  ext=dispatch['request']['slots']['externalOperationId']
  body=dict(PROVENANCE,externalOperationId=ext,outcome='CONFIRMED_SUCCESS',orderId=ref('dispatch','objectId'),workId=ref('work','workId'),commandId=ref('dispatch','commandId'),sourceMeaning='synthetic transport acknowledgement; not supplier acceptance or arrival')
  extchain=chain(sid,'source-transmission',{'kind':'WORK','id':ref('work','workId')},body,'EXTERNAL_RESULT',ext)
  replybody=dict(PROVENANCE,orderId=ref('dispatch','objectId'),itemId=alias('P'),supplierId=alias('SUP'),proposalRevision=ref('proposal','proposalRevision'),reply=r['reply'],quantity=quantity,unit=unit,occurredAt='2026-10-07T09:00:00Z',sourceMeaning='synthetic supplier reply; not arrival')
  replychain=chain(sid,'source-reply',{'kind':'PURCHASE_ORDER','id':ref('dispatch','objectId')},replybody,'SUPPLIER_'+r['reply'],ref('dispatch','objectId'),quantity,unit)
  by['delivery-ack']['control']['parameters']['externalOperationId']=ext
  tick={'id':'source-delivery-tick','kind':'control','control':{'type':'process','operation':'tickScheduler','parameters':{'schedulerId':'T13-'+sid,'scope':{'organizationId':alias('ORG')}}},'evidenceRefs':['source-delivery-tick:actual-adapter-artifact']}
  trans['request']['slots']={'outboxId':ref('dispatch','outboxId'),'decision':'CONFIRMED_SUCCESS','evidenceId':ref('source-transmission-document','id')};trans['request'].pop('expectedRevision');trans['request']['subjectRefs']=[];trans['request']['provenance']={k:'CONTEXT' for k in trans['request']['slots']}
  reply['request']['slots']['evidence']=ref('source-reply-document','id');reply['request']['slots']['canonicalOccurrenceId']=ref('source-reply-canonical','id');reply['request']['provenance']['canonicalOccurrenceId']='CONTEXT'
  actions=[]
  for a in sub['actions']:
   if a['id']=='transmission':actions += [tick]+extchain
   if a['id']=='supplier-reply':actions += replychain
   actions.append(a)
  sub['actions']=actions
  ledgers.append({'subcaseId':sid,'originalSubcase':old,'generatedSha256':hashlib.sha256(json.dumps(sub,sort_keys=True,ensure_ascii=False).encode()).hexdigest(),'syntheticTransmissionInput':body,'syntheticReplyInput':replybody,'addedActionIds':[a['id'] for a in actions if a['id'].startswith('source-')]})
 if ledger is not None:ledger.update(dict(PROVENANCE,baseline=BASE,changes=ledgers))
 return c

def restore_case(c):
 c=copy.deepcopy(c)
 entries={s['subcaseId']:s for s in json.loads((HERE/'supplier-source-ledger.json').read_text())['changes']}
 originals={k:v['originalSubcase'] for k,v in entries.items()}
 for sub in c['subcases']:
  if sub['id'] not in originals:continue
  assert hashlib.sha256(json.dumps(sub,sort_keys=True,ensure_ascii=False).encode()).hexdigest()==entries[sub['id']]['generatedSha256'], 'Unexpected supplier delta'
  old=originals[sub['id']];old_actions={a['id']:a for a in old['actions']}
  sub['actions']=[copy.deepcopy(old_actions.get(a['id'],a)) for a in sub['actions'] if not a['id'].startswith('source-')]
 return c

def restore_feature(text):
 return ''.join(line for line in text.splitlines(keepends=True) if '"source-' not in line)

def main():
 ledger={};c=repair_case(original(HERE/'case.json'),ledger);(HERE/'supplier-source-ledger.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n');(HERE/'case.json').write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n')
 for s in c['subcases']:
  if s['id'] in SELECTED:
   p=ROOT/s['fixtureRef'];p.write_text(json.dumps(repair_fixture(original(p)),ensure_ascii=False,indent=2)+'\n')
 feature=HERE/'scenario.feature'
 import subprocess
 text=subprocess.check_output(['git','show',f'{BASE}:verification/cases/T13/scenario.feature'],cwd=ROOT,text=True)
 chunks=text.split('  시나리오:')
 for index,sub in enumerate(c['subcases'],1):
  if sub['id'] not in SELECTED:continue
  for target,prefix in [('transmission','source-transmission'),('supplier-reply','source-reply')]:
   names=([('시스템','source-delivery-tick')] if target=='transmission' else [])+[(ACTOR,prefix+'-'+suffix) for suffix in ['document','event','match','canonical']]
   lines=''.join(f'    만일 "{actor}" 역할이 "{name}" 행동을 수행한다\n' for actor,name in names)
   needle=f'    만일 "source" 역할이 "{target}" 행동을 수행한다'
   chunks[index]=chunks[index].replace(needle,lines+needle)
 feature.write_text('  시나리오:'.join(chunks))
if __name__=='__main__':main()
