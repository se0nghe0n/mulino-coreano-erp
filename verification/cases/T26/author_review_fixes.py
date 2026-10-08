#!/usr/bin/env python3
"""Idempotent re-review fixes for T26; no product, scheduler or host implementation.

Owns: the retrySafeCommand request shape of the safe-retry subcases, the forged
actor/hash negative subcases, the autonomous-loop subcases (no harness tick or
sweep; loop processes stay stopped until the watcher group), due-wait
post-restart queue evidence, the derived Korean feature and oracle-bindings.json. Run: python3 -I verification/cases/T26/author_review_fixes.py
"""
import copy, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
DIR=ROOT/'verification/cases/T26'
CASE=DIR/'case.json'
AUTONOMOUS=[('due-wait-db-rediscovery','due-wait-autonomous-loop','tick','db'),('lot-expiry-no-event','lot-expiry-autonomous-loop','sweep','sweep-db'),('orphan-intake-recovered','orphan-intake-autonomous-loop','tick','db')]
NEW_SUBCASES=['safe-retry-forged-original-actor','safe-retry-forged-request-hash','due-wait-autonomous-loop','lot-expiry-autonomous-loop','orphan-intake-autonomous-loop']
OWNED_ASSERTIONS=('retry-request-','stored-','forged-','autonomous-','queue-empty-after-restart')
FORGED_HASH='f0'*32
def dump(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
def alias(x):return {'$alias':x}
def ref(a,p):return {'$result':{'actionId':a,'pointer':p}}
def by_id(items,id):return next(x for x in items if x['id']==id)
def rescope(node,old,new):
    """Replace the subcase id inside scopes, environment ids, scheduler ids and command keys."""
    text=json.dumps(node,ensure_ascii=False).replace(old,new)
    return json.loads(text)
def assertion_like(sub,template_id,**kw):
    x=copy.deepcopy(by_id(sub['assertions'],template_id));x.pop('baseline',None);x.pop('unit',None);x.pop('unitSource',None);x.pop('baselineUnitSource',None);x.update(kw);return x
IDENTITY_FIELDS=('commandId','canonicalRequestHash','originalCommandIdempotencyKey','originalActorId')
def shape_retry(action,extra=None):
    """One management shape for retrySafeCommand (as in C3): the original command identity and a reason.
    Actor, delegation, canonical hash and key are reloaded from the stored command (plan §3.3, §7.2)."""
    r=action['request']
    command=r.get('commandId',r.get('slots',{}).get('commandId'))
    out={k:v for k,v in r.items() if k not in IDENTITY_FIELDS+('slots','subjectRefs')}
    out['subjectRefs']=[{'type':'CommandRecord','id':copy.deepcopy(command)}]
    out['slots']={'commandId':copy.deepcopy(command),'reason':'원 canonical 명령과 현재 위임을 다시 확인한다'}
    if extra:out.update(extra)
    action['request']=out
def fix_safe_retry(sub):
    for a in sub['actions']:
        if a.get('capabilityId')=='retrySafeCommand':shape_retry(a)
    sub['assertions']=[a for a in sub['assertions'] if not a['id'].startswith(OWNED_ASSERTIONS)]
    for a in sub['assertions']:
        if a['id']=='safe-retry-original-actor':
            a['oracleExplanation']='저장된 원 command의 actor는 원 요청을 인증한 warehouse다. retry payload는 actor를 전달하지 않으므로 서버가 저장 원행에서 다시 읽은 값이다.'
        if a['id']=='retry-canonical-hash':
            a['oracleExplanation']='저장 command의 canonical hash는 원 요청 응답의 hash와 같다. retry payload는 hash를 전달하지 않는다.'
    return sub
def forged_actor(base):
    s=rescope(copy.deepcopy(base),'safe-retry-revoked-blocked','safe-retry-forged-original-actor')
    s['id']='safe-retry-forged-original-actor';s['title']='철회된 원 actor 대신 payload가 지목한 actor로 재시도할 수 없다'
    s['fixtureRef']='verification/cases/T26/fixtures/safe-retry-forged-original-actor.json'
    s['oracleExplanation']='원 actor warehouse의 grant를 철회한 뒤 OPERATIONS가 아직 이동 권한이 있는 warehouseLead를 payload의 originalActorId로 넣어 재시도한다. 서버는 저장 command의 actor로 다시 인가해 거부하고 실물·계보·배분 효과0을 남긴다(plan §3.3, §7.2).'
    shape_retry(by_id(s['actions'],'retry'),{'originalActorId':alias('warehouseLead')})
    # FORBIDDEN (stored actor revoked) or TYPE_INVALID (strict envelope) are both rejections; the audit code is not pinned.
    s['assertions']=[a for a in s['assertions'] if a['id'] not in ['retry-forbidden','retry-denial-audit']]
    s['assertions'].insert(0,assertion_like(base,'retry-forbidden',id='forged-actor-rejected',source={'actionId':'retry','pointer':'/response/outcome'},expected='REJECTED',
        oracleExplanation='payload의 originalActorId는 권한이 아니다. 저장된 원 actor의 철회된 grant 때문에 또는 허용되지 않은 필드 때문에 거부되며 어느 경우도 효과를 만들지 않는다.'))
    s['assertions'].append(assertion_like(base,'db-current-assignment-one',id='forged-actor-no-committed-retry',source={'actionId':'db','pointer':'/data/rawRows/commands','where':{'commandIdempotencyKey':'T26-safe-retry-forged-original-actor-retry','status':'COMMITTED'}},expected=0,
        oracleExplanation='위조된 actor로 시도한 retry는 COMMITTED command를 남기지 않는다.'))
    return s
def forged_hash(base):
    s=rescope(copy.deepcopy(base),'safe-retry-canonical-current-grant','safe-retry-forged-request-hash')
    s['id']='safe-retry-forged-request-hash';s['title']='호출자가 제시한 다른 canonical hash로 안전 재시도할 수 없다'
    s['fixtureRef']='verification/cases/T26/fixtures/safe-retry-canonical-current-grant.json'
    s['oracleExplanation']='grant가 유효한 상태에서도 OPERATIONS가 다른 payload의 canonicalRequestHash를 함께 보내면 retry는 거부되고 이동0이다. hash는 저장 command에서만 온다(plan §7.2·§7.3).'
    shape_retry(by_id(s['actions'],'retry'),{'canonicalRequestHash':FORGED_HASH})
    keep=[a for a in s['assertions'] if a['id']=='before-movement-zero']
    s['assertions']=keep+[
        assertion_like(base,'before-movement-zero',id='forged-hash-rejected',op='equals',source={'actionId':'retry','pointer':'/response/outcome'},expected='REJECTED',oracleExplanation='저장 hash와 다른 호출자 hash로는 재시도하지 않는다.'),
        assertion_like(base,'before-movement-zero',id='forged-hash-no-movement',source={'actionId':'db','pointer':'/data/rawRows/movements'},expected=0,oracleExplanation='거부된 위조 hash 재시도는 실제 이동을 만들지 않는다.'),
        assertion_like(base,'before-movement-zero',id='forged-hash-no-committed-retry',source={'actionId':'db','pointer':'/data/rawRows/commands','where':{'commandIdempotencyKey':'T26-safe-retry-forged-request-hash-retry','status':'COMMITTED'}},expected=0,oracleExplanation='위조 hash retry는 COMMITTED command를 남기지 않는다.'),
        assertion_like(base,'retry-canonical-hash',id='stored-hash-unchanged',source={'actionId':'db','pointer':'/data/rawRows/commands','field':'requestHash','where':{'commandIdempotencyKey':'CANONICAL-MOVE-safe-retry-forged-request-hash'}},oracleExplanation='원 canonical key의 저장 command hash는 원 응답의 hash 그대로이며 호출자 hash로 바뀌지 않는다. 거부된 retry record는 별도 허용 기록이다.')]
    return s
# step2r round 9 (Step 2 closure review 6 follow-up): the expiry guard negatives pick the reservation before the boundary,
# so the only remaining reason to reject the dispatch is the expiry, and pin the code the product's guard returns.
# FulfillmentCommands.prepare (read only): grant expiry fails the scope authorization (FORBIDDEN, line 28) and a lot,
# disposition-basis or eligibility-policy expiry fails the suspended/current-sale check (INSUFFICIENT_ELIGIBLE_QUANTITY,
# line 38: 'Suspended allocation cannot execute' after the sweep, 'Current exact sale permission denied' before it), both
# before the pick check ('Pick before dispatch required', TYPE_INVALID). Without the pick a guard-less product also rejected.
EXPIRY_GUARDS={'lot-expiry-no-event':'lot','lot-expiry-delayed-guard':'lot','disposition-expiry-no-event':'disposition','disposition-expiry-delayed-guard':'disposition',
    'grant-expiry-no-event':'grant','grant-expiry-delayed-guard':'grant','policy-expiry-no-event':'policy','policy-expiry-delayed-guard':'policy'}
GUARD_CODES={'lot':'INSUFFICIENT_ELIGIBLE_QUANTITY','disposition':'INSUFFICIENT_ELIGIBLE_QUANTITY','policy':'INSUFFICIENT_ELIGIBLE_QUANTITY','grant':'FORBIDDEN'}
GUARD_REASONS={'lot':'LOT 만료 뒤 현재 판매 적격이 없다','disposition':'처분 근거 만료 뒤 현재 판매 적격이 없다','policy':'적격 정책 만료 뒤 현재 판매 적격을 판정할 수 없다',
    'grant':'출고자 warehouse의 grant가 만료돼 scope 인가가 거부된다'}
def grant_pick(fixture_ref):
    path=ROOT/fixture_ref;f=json.loads(path.read_text());w=f['actors']['warehouse']
    for key in (w['roleCapabilities'],w['grant']['actions']):
        if 'pickQuantity' not in key:key.insert(key.index('dispatchQuantity'),'pickQuantity')
    dump(path,f)
def pick_before_guard(sub):
    kind=EXPIRY_GUARDS[sub['id']];acts=sub['actions']
    acts[:]=[a for a in acts if a['id']!='pick']
    reserve=by_id(acts,'reserve');r=reserve['request'];dispatch=by_id(acts,'dispatch')
    assert reserve['actorRef']==dispatch['actorRef']=='warehouse',sub['id']
    pick={'id':'pick','kind':'invoke','actorRef':'warehouse','route':'api','capabilityId':'pickQuantity',
        'request':{'intentKind':'COMMAND','definitionVersion':r['definitionVersion'],'capabilityId':'pickQuantity','scope':copy.deepcopy(r['scope']),'asOf':r['asOf'],'knownAt':r['knownAt'],
            'allocationId':ref('reserve','/response/allocationId'),'expectedRevision':ref('reserve','/response/revision'),'commandIdempotencyKey':'T26-'+sub['id']+'-pick'},
        'evidenceRefs':['pick:actual-artifact']}
    acts.insert(acts.index(reserve)+1,pick)
    dispatch['request']['expectedRevision']=ref('pick','/response/revision')
    sub['assertions']=[a for a in sub['assertions'] if a['id'] not in ('guard-pick-applied','guard-code')]
    at=[a['id'] for a in sub['assertions']].index('guard-outcome')
    picked=assertion_like(sub,'guard-outcome',id='guard-pick-applied',source={'actionId':'pick','pointer':'/response/outcome'},expected='APPLIED',evidenceRefs=['pick:actual-artifact'],
        oracleExplanation='만료 경계 전에 예약20을 pick한다. 그래서 뒤 출고를 거부할 이유는 만료뿐이다. pick이 없으면 만료 guard가 없는 제품도 pick 누락(TYPE_INVALID)으로 거부해 통과한다.')
    code=assertion_like(sub,'guard-outcome',id='guard-code',source={'actionId':'dispatch','pointer':'/response/error/code'},expected=GUARD_CODES[kind],
        oracleExplanation=GUARD_REASONS[kind]+'. 그래서 pick된 예약의 출고 거부 이유는 '+GUARD_CODES[kind]+'이며 pick 누락의 TYPE_INVALID가 아니다(FulfillmentCommands, step2r round 9).')
    sub['assertions'][at:at]=[picked];sub['assertions'].insert(at+2,code)
AUTONOMOUS_PROFILE={'controlledTicks':False,'pausedUntilTickControl':False}
def autonomous_fixture(base_ref,new_id):
    """Same seeded state as the base fixture, but the scheduler/sweeper loop runs on its own 1s tick.
    controlledTicks=false and pausedUntilTickControl=false mean no harness tick gates the loop (README §runtimeProfile)."""
    f=json.loads((ROOT/base_ref).read_text())
    f['fixtureId']='T26-'+new_id
    f['baseline']['runtimeProfile'].update(AUTONOMOUS_PROFILE)
    return f
def process_action(template,new_action_id,operation):
    a=copy.deepcopy(template);a['id']=new_action_id;a['control']['operation']=operation;a['evidenceRefs']=[new_action_id+':actual-artifact'];return a
LOOP_PROCESSES=('scheduler','due-sweeper')
LOOP_START_IDS={'scheduler':'start-scheduler','due-sweeper':'start-sweeper'}
GROUP_ID='start-loop-while-observing'
def lifecycle(a):return a.get('kind')=='control' and a['control']['type']=='process' and a['control']['operation'] in ('start','stop','restart')
def process_id(a):return a['control']['parameters']['processId']
def autonomous(base,new_id,trigger_id,db_id):
    """Same fault and DB oracles, but the harness only stops/starts processes and moves the clock.
    The fixture lets the loop run freely, so a loop process (scheduler, due sweeper) is never started before the watcher
    group: it cannot consume the fault/seed state before before-db or before the watcher exists (step2r round 5).
    api and workers start sequentially before the group. The group holds the passive watcher in branch 0 and one loop
    start per further branch, so loop boot is the only start inside the watcher window, which CaseRunner anchors at the
    harness boundary captured before the group (HostObservationValidator naturalTick)."""
    s=rescope(copy.deepcopy(base),base['id'],new_id);s['id']=new_id
    s['fixtureRef']=f'verification/cases/T26/fixtures/{new_id}.json'
    s['title']=base['title']+' — harness tick/sweep 없이 scheduler loop가 스스로 찾는다'
    s['oracleExplanation']=('plan §10은 별도 이벤트나 사용자 요청 없이 scheduler/due sweeper가 DB에서 다시 찾아 test 관찰 제한30초 안에 처리하기를 요구한다. '
        'fixture runtimeProfile은 loop가 harness tick 없이 1초마다 스스로 돈다(controlledTicks=false, pausedUntilTickControl=false). '
        '그래서 loop process는 장애·seed 단계 동안 한 번도 띄우지 않는다. harness는 api·worker 중지/시작과 가상 clock 전진만 하고, '
        '수동 관찰('+trigger_id+'의 OBSERVE_NEXT_NATURAL_TICK)과 loop process 시작을 한 parallel action으로 함께 시작해 loop의 첫 제출을 '
        'harness 관찰 경계부터 30초 안에서 본다. tick hook만 있고 loop가 없는 구현은 30초 안에 제출을 만들지 못한다.')
    acts=s['actions']
    # Orphan intake restarts processes before the advance; split each restart into stop (before) and start (after).
    restarts=[a for a in acts if lifecycle(a) and a['control']['operation']=='restart']
    if restarts:
        advance=by_id(acts,'advance');acts.remove(advance)
        first=acts.index(restarts[0])
        stops=[process_action(r,r['id'].replace('restart-','stop-again-'),'stop') for r in restarts]
        starts=[process_action(r,r['id'].replace('restart-','start-again-'),'start') for r in restarts]
        for r in restarts:acts.remove(r)
        acts[first:first]=stops+[advance]+starts
    t=by_id(acts,trigger_id);trigger_at=acts.index(t)
    templates={}
    for a in acts[:trigger_at]:
        if lifecycle(a):templates.setdefault(process_id(a),a)
    loops=[p for p in LOOP_PROCESSES if p in templates]
    assert loops,new_id
    # Every lifecycle action of a loop process before the watcher is dropped: the loop is NOT_PRESENT from setup on.
    head=[a for a in acts[:trigger_at] if not (lifecycle(a) and process_id(a) in loops)]
    advance_at=head.index(by_id(head,'advance'))
    assert all(lifecycle(a) and a['control']['operation']=='start' for a in head[advance_at+1:]),new_id
    loop_starts=[process_action(templates[p],LOOP_START_IDS[p],'start') for p in loops]
    start_id=LOOP_START_IDS['due-sweeper' if 'due-sweeper' in loops else 'scheduler']
    t['control']['parameters'].pop('clockInstant',None)
    t['control']['parameters'].update(trigger='OBSERVE_NEXT_NATURAL_TICK',observationWindowSeconds=30,triggeredBy='SCHEDULER_LOOP')
    group={'id':GROUP_ID,'kind':'parallel','timeoutSeconds':90,
        'branches':[{'id':'natural-tick-watch','actions':[t]}]+[{'id':'loop-start-'+process_id(a),'actions':[a]} for a in loop_starts],
        'evidenceRefs':[GROUP_ID+':actual-acks']}
    s['actions']=head+[group]+acts[trigger_at+1:]
    for a in s['actions']:
        if a.get('kind')=='control' and a['control']['operation'] in ('tickScheduler','sweepDue') and a['id']!=trigger_id:
            a['control']['parameters'].pop('clockInstant',None)
            a['control']['parameters'].update(trigger='OBSERVE_NEXT_NATURAL_TICK',observationWindowSeconds=30,triggeredBy='SCHEDULER_LOOP')
    origin=next(x for x in s['assertions'] if x['id'].endswith('-origin'))
    s['assertions'] += [
        # submittedBy comes from the scheduler's own submission rows (extractor rawRows), never from operationEvidence:
        # trigger/triggeredBy are watcher request parameters, so an observer echoing them would prove nothing
        # (HostObservationValidator.naturalTick rejects them in operationEvidence).
        assertion_like(s,origin['id'],id='autonomous-trigger-loop',op='exactSet',
            source={'actionId':trigger_id,'pointer':'/data/hostObservation/extractor/rawRows/schedulerSubmissions',
                    'where':{'taskId':ref(trigger_id,'/data/hostObservation/operationEvidence/taskId')},'field':'submittedBy'},expected=['SCHEDULER_LOOP'],
            oracleExplanation='관찰 창의 첫 제출(operationEvidence의 taskId) 행을 scheduler가 직접 기록한 제출 원행에서 읽으면 제출 주체는 scheduler loop다. 요청 parameter의 되풀이가 아니라 scheduler 기록이며 harness tick이 만든 제출이면 실패한다.'),
        assertion_like(s,origin['id'],id='autonomous-within-30s',op='timeAtMostSeconds',source={'actionId':trigger_id,'pointer':'/data/hostObservation/operationEvidence/submittedAt'},
            baseline={'actionId':GROUP_ID,'pointer':'/data/observationBoundaryAt'},expected='30',
            oracleExplanation='harness가 관찰 group을 시작하기 직전에 잡은 관찰 경계부터 30초(개발/CI 관찰 제한, plan §10) 안에 자율 제출이 관찰된다. validator의 관찰 창과 같은 기준이다.'),
        assertion_like(s,origin['id'],id='autonomous-after-loop-start',op='timeAtMostSeconds',source={'actionId':trigger_id,'pointer':'/data/hostObservation/operationEvidence/submittedAt'},
            baseline={'actionId':start_id,'pointer':'/data/hostObservation/command/startedAt'},expected='30',
            oracleExplanation='제출은 멈춰 있던 loop process의 시작 command보다 앞설 수 없고 그 시작부터도 30초 안이다. 시작 전 제출은 다른 주체의 것이다.'),
        assertion_like(s,origin['id'],id='autonomous-attempt-source',op='exactSet',source={'actionId':db_id,'pointer':'/data/rawRows/attempts','field':'triggeredBy'},expected=['SCHEDULER_LOOP'],
            oracleExplanation='독립 DB attempt 원행도 scheduler loop가 시작한 시도만 있다. harness tick이나 API 호출로 시작한 시도는 없다.')]
    return s
def fixture_forged_actor():
    f=json.loads((DIR/'fixtures/safe-retry-revoked-blocked.json').read_text())
    f['fixtureId']='T26-safe-retry-forged-original-actor'
    lead=copy.deepcopy(f['actors']['warehouse']);lead['subject']=lead['subject'].replace('warehouse','warehouse-lead') if 'warehouse' in lead['subject'] else lead['subject']+'-lead'
    if 'grant' in lead and lead['grant']:lead['grant']['revision']=1
    f['actors']['warehouseLead']=lead
    f['aliases']['warehouseLead']=copy.deepcopy(f['aliases']['warehouse'])
    return f
def render_feature(c,header):
    lines=header[:]
    for s in c['subcases']:
        lines += ['','  시나리오: '+s['title'],f'    먼저 사례 파일 "verification/cases/T26/case.json"의 "{s["id"]}"를 준비한다']
        lines += [f'    만일 "{a.get("actorRef","시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in s['actions']]
        lines += [f'    그러면 "{a["id"]}" assertion으로 "{a["oracleExplanation"].replace(chr(34),chr(39))}"를 확인한다' for a in s['assertions']]
    return '\n'.join(lines)+'\n'
def bindings(c,old):
    out={'caseId':'T26','evidenceClass':old['evidenceClass'],'productStatus':old['productStatus'],'bindings':{}}
    for oracle,names in old['bindings'].items():
        out['bindings'][oracle]={n:[{'subcaseId':s['id'],'assertionId':a['id'],'actionId':a['source']['actionId'],'pointer':a['source']['pointer']}
            for s in c['subcases'] for a in s['assertions'] if a['oracleRef']['oracleId']==oracle and n in a['oracleRef']['observationNames']] for n in names}
    return out
def self_check(c):
    ids=[s['id'] for s in c['subcases']];assert len(ids)==len(set(ids))
    for s in c['subcases']:
        for a in s['actions']:
            if a.get('capabilityId')=='retrySafeCommand' and s['id']!='safe-retry-forged-original-actor':assert 'originalActorId' not in a['request'],s['id']
            if a.get('capabilityId')=='retrySafeCommand' and s['id']!='safe-retry-forged-request-hash':assert 'canonicalRequestHash' not in a['request'],s['id']
        if 'autonomous' in s['id']:
            flat=[c for a in s['actions'] for c in ([x for b in a['branches'] for x in b['actions']] if a['kind']=='parallel' else [a])]
            ops=[a['control'] for a in flat if a.get('kind')=='control' and a['control']['operation'] in ('tickScheduler','sweepDue')]
            group=next(a for a in s['actions'] if a['kind']=='parallel')
            assert group['branches'][0]['actions'][0]['control']['operation'] in ('tickScheduler','sweepDue'),s['id']
            assert all(len(b['actions'])==1 and b['actions'][0]['control']['operation']=='start' and process_id(b['actions'][0]) in LOOP_PROCESSES for b in group['branches'][1:]),s['id']
            pre=s['actions'][:s['actions'].index(group)]
            assert not any(lifecycle(a) and process_id(a) in LOOP_PROCESSES for a in pre),s['id']
            assert not any(a.get('kind')=='control' and a['control']['operation']=='restart' for a in flat),s['id']
            assert ops and all(o['parameters'].get('trigger')=='OBSERVE_NEXT_NATURAL_TICK' and 'clockInstant' not in o['parameters'] for o in ops),s['id']
def main():
    c=json.loads(CASE.read_text())
    c['subcases']=[s for s in c['subcases'] if s['id'] not in NEW_SUBCASES]
    subs={s['id']:s for s in c['subcases']}
    for sub in c['subcases']:
        fix_safe_retry(sub)
        if sub['id'] in EXPIRY_GUARDS:grant_pick(sub['fixtureRef']);pick_before_guard(sub)
    due=subs['due-wait-db-rediscovery'];due['assertions']=[a for a in due['assertions'] if not a['id'].startswith(OWNED_ASSERTIONS)]
    due['assertions'].append(assertion_like(due,'empty-message-queue',id='queue-empty-after-restart',source={'actionId':'db','pointer':'/data/rawRows/queueMessages'},expected=0,
        oracleExplanation='재시작 뒤 terminal 관찰 시점에도 queue message0이다. 재발견은 DB due index에서만 온다.'))
    dump(DIR/'fixtures/safe-retry-forged-original-actor.json',fixture_forged_actor())
    order=[]
    for s in c['subcases']:
        order.append(s)
        if s['id']=='safe-retry-revoked-blocked':order += [forged_actor(subs['safe-retry-revoked-blocked']),forged_hash(subs['safe-retry-canonical-current-grant'])]
    for base_id,new_id,trigger_id,db_id in AUTONOMOUS:
        dump(DIR/f'fixtures/{new_id}.json',autonomous_fixture(subs[base_id]['fixtureRef'],new_id))
        order.append(autonomous(subs[base_id],new_id,trigger_id,db_id))
    c['subcases']=order
    self_check(c)
    dump(CASE,c)
    feature=DIR/'scenario.feature';header=feature.read_text().split('\n')[:3]
    feature.write_text(render_feature(c,header))
    old=json.loads((DIR/'oracle-bindings.json').read_text())
    dump(DIR/'oracle-bindings.json',bindings(c,old))
if __name__=='__main__':main()
