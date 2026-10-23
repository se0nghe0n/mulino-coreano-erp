#!/usr/bin/env python3
"""실제 Work·판정·raw Event와 공개 lifecycle 명령을 잇는 가역 계층이다."""
import copy, hashlib, json, subprocess
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE='b4772eb6'
PARENTS=('parent90-cancelled','parent90-impossible','parent90-superseded')
def original(name):return subprocess.check_output(['git','show',BASE+':verification/cases/T11/'+name],cwd=ROOT)
def baseline():return json.loads(original('case.json'))
def ref(a,p):return {'$result':{'actionId':a,'pointer':p}}
def alias(a):return {'$alias':a}
def query(a,work,actor='A'):
 return {'id':a,'kind':'query','actorRef':actor,'route':'api','capabilityId':'getWork','request':{'workId':work,'scope':{'organizationId':alias('ORG')}},'evidenceRefs':[a+':actual-artifact']}
def command(a,cap,slots,revision,actor='A',key=None):
 return {'id':a,'kind':'invoke','actorRef':actor,'route':'api','capabilityId':cap,'evidenceRefs':[a+':actual-artifact'],'request':{'intentKind':'COMMAND','definitionVersion':'definition-v1','capabilityId':cap,'subjectRefs':[{'type':'TradeItem','id':alias('P')}],'slots':slots,'provenance':{k:'USER' if k in ('reason','nextAction','nextCheckAt','expiresAt','goal') else 'CONTEXT' for k in slots},'expectedRevision':revision,'commandIdempotencyKey':key or a}}
def goal(mode,quantity):
 g={'quantityMode':mode,'targetQuantity':quantity,'unit':'BOX','endpoint':'ARRIVED','dueAt':'2026-10-07T02:00:00Z','timezone':'UTC','evidencePolicyVersion':'SYNTHETIC-policy-v1','evaluatorVersion':'core-v1','scope':{'itemId':alias('P')},'placeId':alias('W')}
 if mode=='STATE_AT':g.update(evaluationAt='2026-10-07T02:00:00Z',action='PHYSICAL_HELD',includeReserved=True,conditions=[{'id':'held-at'}])
 else:g.update(periodStart='2026-10-07T00:00:00Z',periodEnd='2026-10-07T02:00:00Z',periodStartInclusive=True,periodEndInclusive=True,eventKind='CONFIRMED_RECEIPT',contributionScope='CANONICAL_PHYSICAL_FULFILMENT',deduplication='CANONICAL_OCCURRENCE',conditions=[{'id':'arrived'}])
 return g

def authored(old):
 new=copy.deepcopy(old);changes=[]
 for sub in new['subcases']:
  sid=sub['id'];before=copy.deepcopy(sub);out=[]
  if sid in ('cancel-after-shipment','closed-followup'):sub['fixtureRef']='verification/cases/T11/fixtures/native-r2/'+sid+'.json'
  for a in sub['actions']:
   aid=a['id'];r=a.get('request',{});s=r.get('slots',{});pre=[];post=[]
   if sid in PARENTS:
    if aid=='parent':
     s.update(nextAction='남은10상자 수령 확인',nextCheckAt='2026-10-08T09:00:00Z');r['provenance'].update(nextAction='USER',nextCheckAt='USER')
    if aid=='deficit-transfer':
     s.pop('acceptanceEvidenceId');r['provenance'].pop('acceptanceEvidenceId');s['quantity']='10'
     s['assignmentId']=ref('parent-assessment-evaluate','/response/effects/responsibility/assignments/0/ID')
     s['expiresAt']=s['nextCheckAt'];r['provenance']['expiresAt']='USER'
     q=aid+'-current-work';pre.append(query(q,s['workId']));r['expectedRevision']=ref(q,'/response/data/revision')
     post.append(command('deficit-accept','acceptHandover',{'handoverId':ref(aid,'/response/effects/responsibility/ID')},0,key=sid+'-deficit-accept'))
   elif sid=='wait-timeout' and aid=='wait':
    pre.append(query('wait-original-work',alias('SOURCE')))
    pre.append(command('wait-pin-goal','reviseGoal',{'workId':alias('SOURCE'),'goal':goal('CUMULATIVE_EVENT','10'),'reason':'기존10상자 도착 목표의 정의·시각·근거 계약을 명시한다'},ref('wait-original-work','/response/data/revision')))
    r['expectedRevision']=ref('wait-pin-goal','/response/revision')
    s['wait']['resumePredicate']={'operator':'exists','property':'Receipt.quantity','evidenceSelector':'VERIFIED_DISTINCT'}
   elif sid=='closed-followup' and aid=='followup':
    pre.append(query('followup-original-work',alias('PARENT')))
    draft={'itemId':alias('P'),'kind':'TEMPERATURE_REVIEW','ownerId':s['ownerId'],'supervisorId':s['supervisorId'],'goal':goal('STATE_AT','100'),'nextAction':s['nextAction'],'nextCheckAt':s['nextCheckAt']}
    r['slots']={'workId':s['workId'],'sourceEventId':ref('temperature','/response/id'),'reason':'종료 뒤 원천 온도 관측의 대응 책임을 별도 업무에 연결한다','draft':draft}
    r['provenance']={k:'USER' if k in ('reason','draft') else 'CONTEXT' for k in r['slots']};r['expectedRevision']=ref('followup-original-work','/response/data/revision')
   elif sid=='cancel-after-shipment':
    if aid in ('pick','dispatch'):
     assert s['allocationId']==ref('reserve','/response/allocationId')
     s['allocationId']=ref('reserve','/response/effects/allocationId')
    if aid=='transfer':
     pre.append(query('transfer-source-work',alias('SOURCE')))
     pre.append(command('transfer-proposal','transferObligation',{'workId':alias('SOURCE'),'targetWorkId':alias('TARGET'),'assignmentId':alias('ASSIGN'),'recipientId':alias('B'),'nextAction':s['nextAction'],'nextCheckAt':s['nextCheckAt'],'expiresAt':s['nextCheckAt']},ref('transfer-source-work','/response/data/revision')))
     a=command('transfer','acceptHandover',{'handoverId':ref('transfer-proposal','/response/effects/responsibility/ID')},0,actor='B',key=r['commandIdempotencyKey'])
    elif aid in ('close','close-with-duty'):
     if aid=='close':
      for k in ('decisionEvidenceId','reasonText'):s.pop(k);r['provenance'].pop(k)
     q='cancel-current-work' if aid=='close' else 'close-with-duty-current-work'
     pre.append(query(q,alias('SOURCE')));r['expectedRevision']=ref(q,'/response/data/revision')
   out.extend(pre+[a]+post)
  sub['actions']=out
  for assertion in sub['assertions']:
   if sid=='unsupported-evaluator':
    if assertion['id']=='activate-outcome':assertion['expected']='HELD'
    elif assertion['id']=='activate-denial-audit':assertion['source']['where']['outcome']='HELD';assertion.get('where',{})['outcome']='HELD'
   elif sid=='wait-timeout' and assertion['id']=='resume-denied':assertion['expected']='HELD'
   elif sid=='cancel-after-shipment' and assertion['id']=='recipient-authenticated-acceptance':assertion['expected'][0][1]='acceptHandover'
   elif sid=='shared-distinct60' and assertion['id']=='shared-scope-partition':
    assertion['source']['field']=['workId','quantity','physicalRange'];assertion['expected']=[
     [ref('parent-one','/response/workId'),'30',{'physicalScopeId':alias('Q'),'occurrenceId':ref('receive60-canonical','/response/id'),'startQuantity':'0','endQuantity':'30','unit':'BOX'}],
     [ref('parent-two','/response/workId'),'30',{'physicalScopeId':alias('Q'),'occurrenceId':ref('receive60-canonical','/response/id'),'startQuantity':'30','endQuantity':'60','unit':'BOX'}]]
  if sub!=before:changes.append({'subcaseId':sid,'before':before,'after':copy.deepcopy(sub)})
 return new,changes

def apply(value):
 old=baseline();new,_=authored(old)
 if value==new:return copy.deepcopy(value)
 if value!=old:raise ValueError('T11 lifecycle input baseline drift')
 return new

def restore(value):
 old=baseline();new,_=authored(old)
 if value!=new:raise ValueError('T11 lifecycle inverse refuses unrecorded changes')
 return old

def feature():
 text=original('scenario.feature').decode();old={s['id']:s for s in baseline()['subcases']};new,_=authored(baseline())
 for sub in new['subcases']:
  added={a['id']:a for a in sub['actions'] if a['id'] not in {x['id'] for x in old[sub['id']]['actions']}}
  if not added:continue
  start=text.index('"'+sub['id']+'"를 준비한다');end=text.find('\n  시나리오:',start)
  if end<0:end=len(text)
  block=text[start:end];pending=[]
  for a in sub['actions']:
   line=f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다\n'
   if a['id'] in added:pending.append(line)
   elif pending:
    if line not in block:raise ValueError('Feature action missing '+a['id'])
    block=block.replace(line,''.join(pending)+line,1);pending=[]
  if pending:raise ValueError('Trailing feature actions')
  text=text[:start]+block+text[end:]
 return text

def audit_changes(changes):
 output=[]
 for change in changes:
  before=change['before'];after=change['after'];old={a['id']:a for a in before['actions']};new={a['id']:a for a in after['actions']}
  actions=[]
  for aid in dict.fromkeys([*old,*new]):
   if old.get(aid)!=new.get(aid):actions.append({'actionId':aid,'before':old.get(aid),'after':new.get(aid)})
  assertions=[{'assertionId':a['id'],'before':a,'after':b} for a,b in zip(before['assertions'],after['assertions']) if a!=b]
  entry={'subcaseId':change['subcaseId'],'beforeActionIds':list(old),'afterActionIds':list(new),'actions':actions,'assertions':assertions}
  if before['fixtureRef']!=after['fixtureRef']:entry['fixtureRef']={'before':before['fixtureRef'],'after':after['fixtureRef'],'completionLedger':'verification/cases/T11/native-r2-fixture-ledger.json'}
  output.append(entry)
 return output

def main():
 old=baseline();new,changes=authored(old);current=json.loads((HERE/'case.json').read_text());apply(current)
 (HERE/'case.json').write_text(json.dumps(new,ensure_ascii=False,indent=2)+'\n');(HERE/'scenario.feature').write_text(feature())
 ledger={'recordType':'T11_LIFECYCLE_INPUT_AUTHORING','baseline':BASE,'originalAssertionIDsPreserved':110,'originalFixturesUnchanged':True,'changes':audit_changes(changes)}
 (HERE/'lifecycle-input-ledger.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
