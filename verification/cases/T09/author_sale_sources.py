#!/usr/bin/env python3
"""Explicit NEW SYNTHETIC T09 inputs. No expected oracle is a source of facts."""
import copy
import hashlib
import json
import subprocess
from pathlib import Path
from uuid import uuid5, NAMESPACE_URL

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
BASE = 'e932710856553570094a3350268417f19b03a021'
SELECTED = ('cumulative-versus-state', 'exists-versus-end-throughout')
ACTOR = 't09SaleSource'
DELEGATOR = 't09SaleSourceDelegator'
NAMESPACE = 't09-sale-eligibility'
VERSION = 'SYNTHETIC-policy-v1'
UNTIL = '2026-10-31T23:59:59Z'
CAPABILITIES = ['getObject', 'getWork', 'getInventory', 'getEvidence', 'evaluateEligibility',
    'attachEvidence', 'recordActivity', 'matchSourceIdentity', 'linkCanonicalOccurrence',
    'recordDispositionBasis', 'decideQuality', 'confirmCustomerConditions',
    'confirmCommercialDisposition', 'prepareRegulatoryProcedure', 'recordSubmission',
    'recordRegulatoryDecision']
FLAGS = dict(newSynthetic=True, originalAuthorityClaimed=False, operatingApprovalClaimed=False)


def baseline(path):
    return subprocess.check_output(['git', 'show', BASE+':'+str(path)], cwd=ROOT).decode()


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def ref(action, field='id'):
    return {'$result': {'actionId': action, 'pointer': '/response/'+field}}


def alias(name):
    return {'$alias': name}


def encode(source, kind):
    return {'$encode': {'source': copy.deepcopy(source), 'encoding': kind}}


def invoke(aid, cap, slots, kind='COMMAND', revision=None):
    req = dict(intentKind=kind, definitionVersion='definition-v1', capabilityId=cap,
               subjectRefs=[], slots=slots, provenance={k:'CONTEXT' for k in slots},
               commandIdempotencyKey=aid)
    if revision is not None:
        req['expectedRevision'] = revision
    return dict(id=aid, kind='invoke', actorRef=ACTOR, route='api', capabilityId=cap,
                evidenceRefs=[aid+':actual-artifact'], request=req)


def query(aid, cap, identity):
    return dict(id=aid, kind='query', actorRef=ACTOR, route='api', capabilityId=cap,
                evidenceRefs=[aid+':actual-artifact'],
                request={**identity, 'scope': {'organizationId':alias('ORG'), **({'objectType':'QuantitySegment'} if cap=='getObject' else {})}})


def graph(prefix, subject, kind, original, at, quantity=None):
    event = str(uuid5(NAMESPACE_URL, 'mulino:T09:new-synthetic:'+prefix))
    original = {**FLAGS, **original}
    original['sourceVersion'] = '1'
    original['sourceDecisionId' if kind.startswith('QUALITY_') else 'regulatoryEventId'] = event
    doc = invoke(prefix+'-document', 'attachEvidence', dict(subject=subject,
        sourceNamespace=NAMESPACE, sourceReference=event, mediaType='application/json',
        expectedHash=encode(original,'sha256'), contentBase64=encode(original,'base64'),
        provenance='NEW SYNTHETIC T09 input; no original authority or operating approval'), 'RECORD')
    slots = dict(subject=subject, kind=kind, sourceNamespace=NAMESPACE, externalEventId=event,
        sourceVersion='1', effectiveFrom=at, timeZone='UTC', timePrecision='SECOND',
        valueState='KNOWN', payload=encode(original,'json'), documentId=ref(prefix+'-document'),
        assertion='NEW SYNTHETIC source; requires actual canonical reconciliation')
    if quantity is not None:
        slots.update(quantity=quantity,unit='BOX')
    evt = invoke(prefix+'-event','recordActivity',slots,'RECORD')
    match = dict(claimId=ref(prefix+'-event','claimId'),basisDocumentId=ref(prefix+'-document'),
        physicalScopeId=subject['id'],policyVersion=VERSION,
        sourceIdentity=NAMESPACE+':'+event+':1',effectiveFrom=at,
        reason='명시된 가상 원문과 실제 물량 및 정책 버전을 대조한다')
    if quantity is not None:
        match.update(quantity=quantity,unit='BOX')
    return [doc,evt,invoke(prefix+'-match','matchSourceIdentity',match,
        revision=ref(prefix+'-event','revision')),
        invoke(prefix+'-canonical','linkCanonicalOccurrence',
               dict(reconciliationId=ref(prefix+'-match')),revision=ref(prefix+'-match','revision'))]


def source_actions(sub, fixture):
    acts = {a['id']:a for a in sub['actions']}
    reserve_id = 'reserve60' if sub['id'] == SELECTED[0] else 'reserve'
    reserve = acts[reserve_id]['request']['slots']
    sale_id = 'sale60' if sub['id'] == SELECTED[0] else 'sale'
    sale = acts[sale_id]['request']['slots']
    # Read original authored physical request, never assertions or expected values.
    segment, quantity, start = reserve['segmentId'], reserve['quantity'], reserve['startQuantity']
    assert start == '0' and reserve['unit'] == 'BOX'
    assert sale['quantity'] == quantity and sale['unit'] == 'BOX'
    work = ref(sale_id,'workId')
    at = fixture['clock']['asOf']
    subject = dict(kind='SEGMENT',id=segment)
    prefix = 't09-sale-source'
    out = []
    for action in ('SELL','DISPATCH'):
        for category in ('QC','CUSTOMER','COMMERCIAL'):
            p = prefix+'-'+action.lower()+'-'+category.lower()
            slots = dict(operation='recordDispositionBasis',action=action,category=category,
                segmentId=segment,workId=work,startQuantity=start,quantity=quantity,unit='BOX',
                customerId=sale['customerId'],validFrom=at,validUntil=UNTIL)
            out.extend(graph(p,subject,'QUALITY_DECISION_recordDispositionBasis_'+category,slots,at,quantity))
            out.append(query(p+'-segment','getObject',dict(id=segment)))
            out.append(query(p+'-work','getWork',dict(workId=work)))
            typed = {k:v for k,v in slots.items() if k!='operation'}
            typed.update(evidenceId=ref(p+'-document'),nextCheckAt=UNTIL,
                         reason='명시적으로 새로 만든 가상 판매 적격 원천을 기록한다')
            out.append(invoke(p+'-basis','recordDispositionBasis',typed,'RECORD',
                              ref(p+'-segment','data/revision')))
        p = prefix+'-'+action.lower()+'-regulatory'
        prep_source = {**FLAGS,'purpose':'NEW SYNTHETIC procedure preparation',
                       'itemId':sale['itemId'],'lotId':alias('LOT'),'physicalScopeId':segment,'workId':work}
        out.append(invoke(p+'-prepare-document','attachEvidence',dict(subject=subject,
            sourceNamespace=NAMESPACE,sourceReference=p+'-prepare',mediaType='application/json',
            expectedHash=encode(prep_source,'sha256'),contentBase64=encode(prep_source,'base64'),
            provenance='NEW SYNTHETIC preparation, not submission or permission'),'RECORD'))
        out.append(query(p+'-work','getWork',dict(workId=work)))
        out.append(invoke(p+'-prepare','prepareRegulatoryProcedure',dict(itemId=sale['itemId'],
            lotId=alias('LOT'),physicalScopeId=segment,workId=work,
            policyId=alias('t09SalePolicy'+action),documentVersionId=ref(p+'-prepare-document'))))
        common=dict(procedureId=ref(p+'-prepare','effects/procedureId'),
            procedureVersionId=ref(p+'-prepare','effects/procedureVersionId'),itemId=sale['itemId'],
            lotId=alias('LOT'),physicalScopeId=segment,authority='T09_SYNTHETIC_AUTHORITY',policyVersion=VERSION)
        out.extend(graph(p+'-submission',subject,'REGULATORY_SUBMISSION',
                         dict(**common,occurredAt=at),at))
        out.append(invoke(p+'-submit','recordSubmission',dict(procedureId=common['procedureId'],
            occurredAt=at,occurrenceId=ref(p+'-submission-canonical')),'RECORD',ref(p+'-prepare','revision')))
        common['procedureVersionId']=ref(p+'-submit','effects/procedureVersionId')
        typed=dict(procedureId=common['procedureId'],decision='ALLOWED',action=action,
                   startQuantity=start,quantity=quantity,unit='BOX',validFrom=at,validUntil=UNTIL)
        out.extend(graph(p+'-decision',subject,'REGULATORY_DECISION',{**common,**typed},at,quantity))
        out.append(invoke(p+'-allow','recordRegulatoryDecision',
            dict(**typed,occurrenceId=ref(p+'-decision-canonical')),'RECORD',ref(p+'-submit','revision')))
    return sale_id,out


def fixture_inputs(f):
    f = copy.deepcopy(f)
    original = f['actors']['A']
    for name in (DELEGATOR,ACTOR):
        assert name not in f['actors'] and name not in f['aliases']
        f['aliases'][name]={'type':'Human'}
        f['actors'][name]=dict(issuer=original['issuer'],subject=f['fixtureId']+'-'+name,
            audience=original['audience'],organizationAlias='ORG',roleCapabilities=list(CAPABILITIES),
            grant=dict(delegatorAlias=DELEGATOR,actions=list(CAPABILITIES),
                scope=dict(organizationAlias='ORG',itemAliases=['P']),
                validFrom=original['grant']['validFrom'],validUntil=original['grant']['validUntil'],revision=1))
    assert 'expiresAt' not in f['aliases']['LOT']
    f['aliases']['LOT']['expiresAt']=UNTIL
    for action in ('SELL','DISPATCH'):
        f['aliases']['t09SalePolicy'+action]=dict(type='RegulatoryPolicy',fictional=True,
            **FLAGS,actorAlias=ACTOR,version=VERSION,authority='T09_SYNTHETIC_AUTHORITY',
            sourceNamespace=NAMESPACE,action=action,validFrom=f['clock']['asOf'],validUntil=UNTIL,
            status='ACTIVE',requiresLabel=False)
    f['baseline'].setdefault('sourceProfiles',[]).append(dict(namespace=NAMESPACE,
        policyVersion=VERSION,reconciliationOwnerAlias=ACTOR,**FLAGS))
    return f


def authored():
    case=json.loads(baseline('verification/cases/T09/case.json'))
    fixtures={}
    for sub in case['subcases']:
        if sub['id'] not in SELECTED: continue
        f=json.loads(baseline(sub['fixtureRef']))
        fixtures[sub['fixtureRef']]=fixture_inputs(f)
        sale,extra=source_actions(sub,f)
        index=next(i for i,a in enumerate(sub['actions']) if a['id']==sale)
        sub['actions'][index+1:index+1]=extra
    return case,fixtures


def restore(value):
    verify_ledger()
    new,_=authored()
    if value != new: raise ValueError('T09 strict sale-source inverse rejects unrecorded input/oracle/grant drift')
    return json.loads(baseline('verification/cases/T09/case.json'))


def restore_fixture(value):
    verify_ledger()
    _,fixtures=authored()
    for path,new in fixtures.items():
        if value.get('fixtureId') == new['fixtureId']:
            if value != new: raise ValueError('T09 strict fixture inverse rejects unrecorded source/grant drift')
            return json.loads(baseline(path))
    if any(name in value.get('actors', {}) for name in (ACTOR, DELEGATOR)) or any(
            profile.get('namespace') == NAMESPACE for profile in value.get('baseline', {}).get('sourceProfiles', [])):
        raise ValueError('T09 sale-source fixture identity differs from the declared fixtures')
    return copy.deepcopy(value)


def apply(value):
    new,_=authored()
    old=json.loads(baseline('verification/cases/T09/case.json'))
    if value != old and value != new: raise ValueError('T09 sale-source authoring baseline drift')
    return new


def authored_feature():
    text=baseline('verification/cases/T09/scenario.feature')
    case,_=authored()
    for sub in case['subcases']:
        if sub['id'] not in SELECTED: continue
        old=next(s for s in json.loads(baseline('verification/cases/T09/case.json'))['subcases'] if s['id']==sub['id'])
        old_ids={a['id'] for a in old['actions']}
        sale='sale60' if sub['id']==SELECTED[0] else 'sale'
        anchor=f'    만일 "{next(a for a in old["actions"] if a["id"]==sale)["actorRef"]}" 역할이 "{sale}" 행동을 수행한다\n'
        start=text.index('"'+sub['id']+'"를 준비한다')
        end=text.find('\n  시나리오:',start)
        if end<0:end=len(text)
        block=text[start:end]
        assert block.count(anchor)==1
        extra=''.join(f'    만일 "{ACTOR}" 역할이 "{a["id"]}" 행동을 수행한다\n' for a in sub['actions'] if a['id'] not in old_ids)
        text=text[:start]+block.replace(anchor,anchor+extra)+text[end:]
    return text


def restore_feature(value):
    verify_ledger()
    if value != authored_feature(): raise ValueError('T09 strict feature inverse rejects unrecorded drift')
    return baseline('verification/cases/T09/scenario.feature')


def expected_ledger():
    case,fixtures=authored()
    records=[]
    for path,value in {'verification/cases/T09/case.json':case,**fixtures}.items():
        before=baseline(path);after=json.dumps(value,ensure_ascii=False,indent=2)+'\n'
        records.append(dict(path=path,beforeSha256=hashlib.sha256(before.encode()).hexdigest(),afterSha256=hashlib.sha256(after.encode()).hexdigest()))
    path='verification/cases/T09/scenario.feature'
    records.append(dict(path=path,beforeSha256=hashlib.sha256(baseline(path).encode()).hexdigest(),afterSha256=hashlib.sha256(authored_feature().encode()).hexdigest()))
    return dict(recordType='T09_NEW_SYNTHETIC_SALE_INPUT',baseline=BASE,**FLAGS,
        selectedSubcases=list(SELECTED),originalAssertions=94,files=records,
        actor=ACTOR,delegator=DELEGATOR,capabilities=CAPABILITIES,policyNamespace=NAMESPACE,
        policyVersion=VERSION,authority='T09_SYNTHETIC_AUTHORITY',lotExpiresAt=UNTIL,
        requiresLabel=False,requiresLabelMeaning='Explicit minimal fictional policy; no operating waiver or original requirement claim',
        work='Actual sale order Work, never purchase/quantity-assessment Work',
        ranges={'cumulative-versus-state':'original reserve60 [0,60) BOX on actual received leaf',
                'exists-versus-end-throughout':'original reserve [0,100) BOX on fixture Q'},
        generatedActionIds={s['id']:[a['id'] for a in s['actions'] if a['id'].startswith('t09-sale-source-')] for s in case['subcases'] if s['id'] in SELECTED})


def verify_ledger(value=None):
    if value is None:
        value=json.loads((HERE/'sale-source-input-ledger.json').read_text())
    if value != expected_ledger():
        raise ValueError('T09 sale-source ledger differs from the declared generation contract')


def main():
    case,fixtures=authored()
    target=HERE/'case.json'
    apply(json.loads(target.read_text()))
    files={'verification/cases/T09/case.json':case,**fixtures}
    records=[]
    for path,value in files.items():
        before=baseline(path)
        after=json.dumps(value,ensure_ascii=False,indent=2)+'\n'
        current=(ROOT/path).read_text()
        if current not in (before,after): raise ValueError('Undeclared file drift: '+path)
        records.append(dict(path=path,beforeSha256=hashlib.sha256(before.encode()).hexdigest(),
                            afterSha256=hashlib.sha256(after.encode()).hexdigest()))
        (ROOT/path).write_text(after)
    feature=HERE/'scenario.feature';before=baseline(str(feature.relative_to(ROOT)));after=authored_feature()
    if feature.read_text() not in (before,after):raise ValueError('Undeclared feature drift')
    feature.write_text(after)
    records.append(dict(path=str(feature.relative_to(ROOT)),beforeSha256=hashlib.sha256(before.encode()).hexdigest(),afterSha256=hashlib.sha256(after.encode()).hexdigest()))
    ledger=expected_ledger()
    (HERE/'sale-source-input-ledger.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()
