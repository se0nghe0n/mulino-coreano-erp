"""T17 공개 입력 교정. 사업 oracle에서 원천 사실을 만들지 않는다.

기존 요청의 수량/시각/원천 주장을 immutable 원문으로 보존한다.
정상 인도만 실제 dispatch 원문 대조와 canonical 확인을 요청한다.
미검증 claim은 관찰 접수에 머무르며 검증 결과를 미리 작성하지 않는다.
"""
import copy
import importlib.util
import sys
import json
from decimal import Decimal
from pathlib import Path
from uuid import UUID, uuid5

HERE = Path(__file__).resolve().parent
_spec=importlib.util.spec_from_file_location('t17_first_failure',HERE/'author_first_failure_inputs.py')
first_failure=importlib.util.module_from_spec(_spec);_spec.loader.exec_module(first_failure)
_completion_spec=importlib.util.spec_from_file_location('t17_completion',HERE/'author_completion_inputs.py')
completion=importlib.util.module_from_spec(_completion_spec);_completion_spec.loader.exec_module(completion)
_delivery_spec=importlib.util.spec_from_file_location('t17_delivery',HERE/'author_delivery_inputs.py')
delivery=importlib.util.module_from_spec(_delivery_spec);_delivery_spec.loader.exec_module(delivery)
_goal_spec=importlib.util.spec_from_file_location('t17_goal',HERE/'author_delivery_goal_inputs.py')
goal=importlib.util.module_from_spec(_goal_spec);_goal_spec.loader.exec_module(goal)
_sales_spec=importlib.util.spec_from_file_location('t17_sales_inputs',HERE/'author_sales_contract_inputs.py')
sales_inputs=importlib.util.module_from_spec(_sales_spec);_sales_spec.loader.exec_module(sales_inputs)
_recall_spec=importlib.util.spec_from_file_location('t17_recall_inputs',HERE/'author_recall_inputs.py')
recall_inputs=importlib.util.module_from_spec(_recall_spec);_recall_spec.loader.exec_module(recall_inputs)
ROOT = HERE.parents[2]
META = 'publicContractAuthoring'
NAMESPACE = 'synthetic-t17-eligibility'
POLICY = 't17-synthetic-v1'


def ref(action, field):
    return {'$result': {'actionId': action, 'pointer': '/response/' + field}}


def alias(name): return {'$alias': name}
def encoded(value, encoding): return {'$encode': {'source': copy.deepcopy(value), 'encoding': encoding}}


def command(sid, name, cap, slots, actor='t17Source', kind='COMMAND', revision=None):
    request = {'intentKind': kind, 'definitionVersion': 'definition-v1', 'capabilityId': cap,
               'subjectRefs': [], 'slots': slots,
               'provenance': {key: 'CONTEXT' for key in slots},
               'commandIdempotencyKey': 'T17-' + sid + '-' + name}
    if revision is not None: request['expectedRevision'] = revision
    return {'id': name, 'kind': 'invoke', 'actorRef': actor, 'route': 'api',
            'capabilityId': cap, 'request': request, 'evidenceRefs': [name + ':actual-command-artifact']}


def source_actions(sid, prefix, subject, original, kind, at, quantity, unit, external_id=None, namespace=NAMESPACE, actor='t17Source'):
    external_id=external_id or 'T17-' + sid + '-' + prefix
    doc = command(sid, prefix + '-document', 'attachEvidence', {
        'subject': subject, 'sourceNamespace': namespace, 'sourceReference': external_id,
        'mediaType': 'application/json', 'expectedHash': encoded(original, 'sha256'),
        'contentBase64': encoded(original, 'base64'),
        'provenance': 'SYNTHETIC T17 original given; not a verified fact or regulatory approval'}, actor=actor,kind='RECORD')
    event = command(sid, prefix + '-event', 'recordActivity', {
        'subject': subject, 'kind': kind, 'sourceNamespace': namespace,
        'externalEventId': external_id, 'sourceVersion': '1',
        'effectiveFrom': at, 'timeZone': 'UTC', 'timePrecision': 'SECOND', 'valueState': 'KNOWN',
        'payload': encoded(original, 'json'), 'documentId': ref(doc['id'], 'id'),
        'assertion': 'Original reported claim; current reconciliation must establish identity',
        'quantity': quantity, 'unit': unit},actor=actor,kind='RECORD')
    return [doc, event]


def repair_fixture(f):
    """Explicit OTHERPACK-only physical correction and additive source vocabulary."""
    before = copy.deepcopy(f)
    changes = []
    def put(path, value):
        parent = f
        parts = path.strip('/').split('/')
        for key in parts[:-1]: parent = parent[int(key)] if isinstance(parent, list) else parent[key]
        key = int(parts[-1]) if isinstance(parent, list) else parts[-1]
        changes.append({'path': path, 'before': copy.deepcopy(parent.get(key)),
                        'hadBefore': key in parent, 'after': copy.deepcopy(value)})
        parent[key] = copy.deepcopy(value)
    if f['fixtureId'] == 'T17-eligibility-packaging-unapproved':
        segment, lot = f['aliases']['OTHERPACK'], f['aliases']['L']
        if segment['itemAlias'] != 'ALT' or segment['lotAlias'] != 'L' or lot['itemAlias'] != 'P':
            raise ValueError('Unexpected original OTHERPACK identity')
        new = copy.deepcopy(lot); new['itemAlias'] = 'ALT'
        put('/aliases/L_ALT', new)
        put('/aliases/OTHERPACK/lotAlias', 'L_ALT')
        for i, row in enumerate(f['baseline']['sourceProvenance']):
            if row['segmentAlias'] == 'OTHERPACK': put(f'/baseline/sourceProvenance/{i}/lotAlias', 'L_ALT')
        work = f['baseline']['t17SourceFacts']['workAliases']['OTHERPACK']
        for i, row in enumerate(f['baseline']['works']):
            if row['alias'] == work: put(f'/baseline/works/{i}/lotAlias', 'L_ALT')
    # Only source actors already authorized for this namespace may submit these
    # event kinds; no capability assignment or grant is changed.
    sid=f['fixtureId'].removeprefix('T17-')
    if sid in DELIVERY_CASES:
        target='SYNTHETIC-UNVERIFIED-SOURCE' if sid.startswith('claim-') else NAMESPACE
        for i, profile in enumerate(f['baseline']['sourceProfiles']):
            if profile['namespace'] == target:
                kinds=profile['eventKinds'] + [k for k in ['PHYSICAL_DELIVERY','OBSERVED_MOVEMENT'] if k not in profile['eventKinds']]
                put(f'/baseline/sourceProfiles/{i}/eventKinds',kinds)
    if changes:
        f['baseline']['policy'][META] = {'version': 1, 'synthetic': True,
            'operatingApprovalClaimed': False, 'changes': changes,
            'otherpackMapping': {'segmentAlias': 'OTHERPACK', 'oldLotAlias': 'L', 'newLotAlias': 'L_ALT',
                                'itemAlias': 'ALT', 'expirySelection': 'same existing L future fixture boundary'}
                               if f['fixtureId'].endswith('packaging-unapproved') else None}
    validate_fixture(f)
    return before


def validate_fixture(f):
    if f['fixtureId'] != 'T17-eligibility-packaging-unapproved':return
    for name,segment in f['aliases'].items():
        if segment['type'] != 'QuantitySegment':continue
        lot=f['aliases'][segment['lotAlias']]
        if lot['itemAlias'] != segment['itemAlias']:raise ValueError(name+': typed LOT/item mismatch')
    segment=f['aliases']['OTHERPACK'];lot=f['aliases'][segment['lotAlias']]
    if lot['manufacturerAlias'] != f['aliases']['L']['manufacturerAlias'] or lot['expiresAt'] != f['aliases']['L']['expiresAt']:
        raise ValueError('OTHERPACK must retain the same manufacturer and approved fixture expiry boundary')


DELIVERY_CASES = {'normal-consumed-delivery', 'late-restriction-actual-delivery', 'post-dispatch-expiry',
                  'claim-falseclaim', 'claim-unidentifiedscope', 'claim-conflictingquantity',
                  'claim-nopriordispatch', 'claim-scopeoverflow'}


def restore_fixture(f):
    f = recall_inputs.restore_fixture(f)
    f = first_failure.restore_fixture(completion.restore_fixture(delivery.restore_fixture(goal.restore_fixture(f))))
    meta = f['baseline']['policy'].pop(META, None)
    if meta:
        for change in reversed(meta['changes']):
            parent = f; parts = change['path'].strip('/').split('/')
            for key in parts[:-1]: parent = parent[int(key)] if isinstance(parent, list) else parent[key]
            key = int(parts[-1]) if isinstance(parent, list) else parts[-1]
            if parent[key] != change['after']: raise ValueError('Changed authored delta ' + change['path'])
            if change['hadBefore']: parent[key] = change['before']
            else: del parent[key]
    return f


def coordinates(action, fixture):
    slots = action['request']['slots']
    if 'startQuantity' in slots: return
    segment = fixture['aliases'][slots['segmentId']['$alias']]
    physical, = (x for x in fixture['baseline']['segments'] if x['alias'] == slots['segmentId']['$alias'])
    if any(fixture['baseline'].get(k) for k in ('genealogy','genealogyEdges','allocations')) or any(k in segment for k in ('parentAlias','sourceAlias','retiredAt')):
        raise ValueError('Coordinate authoring requires the original independent root leaves')
    for key in ('quantity', 'unit', 'physicalScope', 'active'):
        if segment[key] != physical[key]: raise ValueError('Conflicting physical leaf ' + key)
    if not segment['active'] or segment.get('startQuantity', '0') != '0' or physical.get('startQuantity', '0') != '0':
        raise ValueError('Only confirmed original [0,q) leaves supported')
    if not 0 < Decimal(slots['quantity']) <= Decimal(physical['quantity']): raise ValueError('Range overflow')
    slots['startQuantity'] = '0'; action['request']['provenance']['startQuantity'] = 'CONTEXT'


def repair_case(case, fixtures):
    before = copy.deepcopy(case)
    for sub in case['subcases']:
        sid = sub['id']; fixture = fixtures[sid]; output = []
        for action in sub['actions']:
            if sid == 'late-restriction-actual-delivery' and action['id'] == 'late-hold':
                # The dispatch's transit leaf is the exact physical realization
                # of this cargo. Category/date/evidence authorization gaps stay
                # visible; no finite hold duration or approval is invented.
                slots=action['request']['slots']
                if slots.pop('cargoScopeId') != ref('dispatch','cargoScope/id'):
                    raise ValueError('Unexpected original recall cargo scope')
                slots['segmentId']=ref('dispatch','effects/transitSegmentId')
                action['request']['provenance'].pop('cargoScopeId')
                action['request']['provenance']['segmentId']='CONTEXT'
            if action.get('capabilityId') in ('reserveQuantity', 'replaceAllocation'):
                coordinates(action, fixture)
            if action.get('capabilityId') not in ('recordDelivery', 'recordObservedMovement'):
                output.append(action); continue
            name = action['id']; old = copy.deepcopy(action['request']['slots'])
            has_dispatch = 'dispatchId' in old
            # The observation UUID is a new authored input, accepted by the public
            # handler, not a fabricated response or a fixture database row.
            observation = str(uuid5(UUID('ff2352df-deca-4014-b468-7f351fab8913'), sid + ':' + name))
            fields = {'customerId': alias('C'), 'itemId': old['itemId'], 'lotId': alias('L'),
                      'rangeRootId': alias('A'), 'placeId': alias('CUSTOMER'),
                      'startQuantity': '0', 'quantity': old['quantity'], 'unit': old['unit'],
                      'occurredAt': old['occurredAt']}
            if name=='claim' and old['verificationInput']['observedPhysicalScope'].endswith('-unmatched'):
                # Required observation coordinates identify only this reported
                # unknown range, never the known warehouse A or its LOT.
                fields['rangeRootId']=str(uuid5(UUID(observation),'unmatched-reported-range'))
                del fields['lotId']
            if has_dispatch:
                fields.update(dispatchId=ref('dispatch','effects/dispatchId'),
                    cargoScopeId=ref('dispatch','effects/cargoScopeId'), salesLineId=ref('order','line/id'))
                subject = {'kind': 'DISPATCH', 'id': fields['dispatchId']}
                work = ref('order', 'workId')
            else:
                subject = {'kind': 'WORK', 'id': alias('t17SourceWork-A')}
                work = alias('t17SourceWork-A')
            original = copy.deepcopy(fields)
            original.update(physicalScopeId=observation,
                deliveryEventId=str(uuid5(UUID(observation), 'original-delivery-event')),
                originalRequest=old, synthetic=True, operatingApprovalClaimed=False)
            prefix = name + '-source'
            output.extend(source_actions(sid,prefix,subject,original,
                'PHYSICAL_DELIVERY' if has_dispatch else 'OBSERVED_MOVEMENT',
                old['occurredAt'],old['quantity'],old['unit'],old.get('externalEventId'),old.get('sourceNamespace',NAMESPACE),
                action['actorRef'] if name=='claim' else 't17Source'))
            intake = copy.deepcopy(fields)
            intake.update(observationId=observation, eventId=ref(prefix+'-event','id'), workId=work,
                nextAction='원래 보고의 실물 범위와 원문을 대조한다', nextCheckAt='2026-10-07T10:00:00Z')
            if 'allocationId' in old: intake['allocationId'] = old['allocationId']
            if name == 'claim':
                # Keep false/conflicting/unidentified claims unverified. Their
                # original assertions remain a runtime frontier, never waived.
                action['request']['slots'] = intake
                action['request']['provenance'] = {k:'CONTEXT' for k in intake}
                output.append(action)
            else:
                output.append(command(sid,name+'-provisional','recordDelivery',intake,actor=action['actorRef'],kind='RECORD'))
                output.append(command(sid,prefix+'-match','matchSourceIdentity',{
                    'claimId':ref(prefix+'-event','claimId'), 'basisDocumentId':ref(prefix+'-document','id'),
                    'physicalScopeId':observation, 'policyVersion':POLICY,
                    'sourceIdentity':NAMESPACE+':T17-'+sid+'-'+prefix+':1',
                    'quantity':old['quantity'],'unit':old['unit'],'effectiveFrom':old['occurredAt'],
                    'reason':'기존 원문과 실제 dispatch의 범위·시각·물량을 대조한다'},revision=1))
                output.append(command(sid,prefix+'-canonical','linkCanonicalOccurrence',{
                    'reconciliationId':ref(prefix+'-match','id')},revision=1))
                action['request']['slots'] = {'observationId':ref(name+'-provisional','effects/observationId'),
                    'canonicalOccurrenceId':ref(prefix+'-canonical','id')}
                action['request']['subjectRefs'] = [{'type':'DeliveryObservation',
                    'id':ref(name+'-provisional','effects/observationId')}]
                action['request']['provenance'] = {k:'CONTEXT' for k in action['request']['slots']}
                action['request']['expectedRevision'] = 1
                output.append(action)
        sub['actions'] = output
        # Exact public response structure only; no adapter aliases or oracle edits.
        caps = {a['id']:a.get('capabilityId') for a in output}
        def fix(node):
            if isinstance(node, dict):
                if 'actionId' in node and 'pointer' in node:
                    r = node; cap = caps.get(r['actionId']); p = r['pointer']
                    if cap in ('reserveQuantity','replaceAllocation','releaseAllocation','pickQuantity') and p == '/response/allocationId': r['pointer']='/response/effects/allocationId'
                    elif cap in ('placeHold','releaseHold') and p == '/response/restrictionId': r['pointer']='/response/effects/restrictionId'
                    elif cap == 'dispatchQuantity' and p == '/response/id': r['pointer']='/response/effects/dispatchId'
                    elif cap == 'dispatchQuantity' and p == '/response/cargoScope/id': r['pointer']='/response/effects/cargoScopeId'
                    elif cap == 'recordDelivery' and p == '/response/id': r['pointer']='/response/effects/deliveryId'
                for v in node.values(): fix(v)
            elif isinstance(node,list):
                for v in node: fix(v)
        for action in output: fix(action)
        for assertion in sub['assertions']: fix(assertion)
    return before


def generate(case, fixtures):
    case=copy.deepcopy(case); fixtures=copy.deepcopy(fixtures)
    for fixture in fixtures.values(): repair_fixture(fixture)
    repair_case(case,fixtures)
    return case,fixtures


def restore_case(case):
    case=recall_inputs.restore_case(case)
    case=first_failure.restore_case(completion.restore_case(delivery.restore_case(goal.restore_case(case))))
    ledger=json.loads((HERE/'public-contract-delta.json').read_text())
    for sub in case['subcases']:
        changes={d['actionId']:d for d in ledger['changes'] if d['subcase']==sub['id']}
        original=[]
        for action in sub['actions']:
            delta=changes.get(action['id'])
            if delta:
                if action != delta['after']: raise ValueError('Authored action drift: '+sub['id']+'/'+action['id'])
                if delta['before'] is not None: original.append(delta['before'])
            else: original.append(action)
        sub['actions']=original
        changes={d['assertionId']:d for d in ledger.get('assertionChanges',[]) if d['subcase']==sub['id']}
        for i,assertion in enumerate(sub['assertions']):
            delta=changes.get(assertion['id'])
            if delta:
                if assertion != delta['after']:raise ValueError('Authored assertion reference drift')
                sub['assertions'][i]=delta['before']
    return case


def feature_lines(feature, before, after):
    """Insert source-only steps; every original feature line remains byte-identical."""
    by_id={s['id']:s for s in before['subcases']};lines=feature.splitlines(keepends=True)
    additions={};current=None
    for s in after['subcases']:
        original_ids={a['id'] for a in by_id[s['id']]['actions']}
        pending=[];insert={}
        for a in s['actions']:
            if a['id'] not in original_ids:pending.append(a)
            elif pending:insert[a['id']]=pending;pending=[]
        if pending:raise ValueError('Trailing source steps unsupported')
        additions[s['id']]=insert
    output=[]
    for line in lines:
        if '먼저 사례 파일 ' in line:current=line.split('의 "',1)[1].split('"',1)[0]
        if ' 행동을 수행한다' in line:
            action=line.split('역할이 "',1)[1].split('"',1)[0]
            for a in additions.get(current,{}).get(action,[]):
                output.append(f'    만일 "{a.get("actorRef", "시스템")}" 역할이 "{a["id"]}" 행동을 수행한다\n')
        output.append(line)
    return ''.join(output)


def restore_feature(feature):
    feature=recall_inputs.restore_feature(feature)
    feature=first_failure.restore_feature(completion.restore_feature(delivery.restore_feature(goal.restore_feature(feature))))
    ledger=json.loads((HERE/'public-contract-delta.json').read_text())
    additions={d['actionId'] for d in ledger['changes'] if d['before'] is None}
    return ''.join(line for line in feature.splitlines(keepends=True)
                   if not any(f'역할이 "{name}" 행동을 수행한다' in line for name in additions))


if __name__ == '__main__':
    import subprocess
    baseline='ac0549e65aabc2137ac44827f544bc3cb0ca782c'
    def load(path): return json.loads(subprocess.check_output(['git','show',baseline+':'+str(path.relative_to(ROOT))],cwd=ROOT))
    case_path=HERE/'case.json'; before=load(case_path)
    fixtures={p.stem:load(p) for p in sorted((HERE/'fixtures').glob('*.json'))}
    case,authored=generate(before,fixtures)
    changes=[]
    for sub,old in zip(case['subcases'],before['subcases']):
        originals={a['id']:a for a in old['actions']}
        for i,a in enumerate(sub['actions']):
            if a['id'] not in originals or a != originals[a['id']]:
                changes.append({'subcase':sub['id'],'actionId':a['id'],'index':i,
                    'before':originals.get(a['id']),'after':a})
    assertions=[]
    for sub,old in zip(case['subcases'],before['subcases']):
        for a,b in zip(sub['assertions'],old['assertions']):
            if a!=b:assertions.append({'subcase':sub['id'],'assertionId':a['id'],'before':b,'after':a})
    ledger={'baseline':baseline,'businessAssertionsChanged':False,'assertionChanges':assertions,
            'originalFeatureLinesChanged':False,'changes':changes}
    # Compose the newer bounded input layer after computing this layer's ledger.
    # Its separate reversible ledger preserves all fifteen root pointer fixes.
    case,authored=first_failure.apply(copy.deepcopy(case),copy.deepcopy(authored),sys.modules[__name__])
    case,authored=completion.apply(copy.deepcopy(case),copy.deepcopy(authored),sys.modules[__name__])
    case,authored=delivery.apply(case,authored,sys.modules[__name__])
    case,authored=goal.apply(case,authored,sys.modules[__name__])
    case=sales_inputs.apply(case)
    case,authored=recall_inputs.apply(case,authored)
    feature_path=HERE/'scenario.feature'
    feature=subprocess.check_output(['git','show',baseline+':'+str(feature_path.relative_to(ROOT))],cwd=ROOT).decode()
    feature_path.write_text(sales_inputs.condition_oracle.apply_feature(feature_lines(feature,before,case)))
    for path,value in [(case_path,case),(HERE/'public-contract-delta.json',ledger)]+[(HERE/'fixtures'/f'{sid}.json',f) for sid,f in authored.items()]:
        data=json.dumps(value,ensure_ascii=False,indent=2)+'\n'
        if not path.exists() or path.read_text()!=data: path.write_text(data)
    print('T17 public input authoring: '+str(len(changes))+' exact action deltas; business assertions unchanged')
