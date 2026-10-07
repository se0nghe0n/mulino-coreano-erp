#!/usr/bin/env python3
"""Validate only corpus integrity. This program cannot run or certify a model."""
import argparse
import collections
import json
import re
import sys
import uuid
from pathlib import Path

CATEGORY_COUNTS = {
    'clear_synonyms': 20,
    'ambiguity_target_conflict': 10,
    'query_write_boundary': 10,
    'version_authority_document': 10,
    'business_exception_responsibility': 10,
}
CATEGORY_ORDER = list(CATEGORY_COUNTS)
PROTECTED = {'WORK', 'QUANTITY', 'ALLOCATION', 'APPROVAL', 'GRANT', 'OUTBOX',
             'EXTERNAL_SEND', 'BANK_TRANSFER'}
PROVENANCE = {'USER', 'CONTEXT', 'APPROVED_DEFAULT'}
INPUT_KEYS = {'utterance', 'fixtureRef', 'priorUserTurnRefs', 'contextPolicy'}
EXPECTED_KEYS = {'expectedIntent', 'oracle', 'capabilityId', 'slots',
                 'acceptanceProposal', 'semanticFocus', 'requirementRefs'}
REQUIREMENT = re.compile(r'^(T(0[1-9]|1[0-9]|2[0-6])|C[1-5]|V[1-8]|E[12])$')
# These independent semantic expectations are more than category/requirement tags.
MANDATORY_ASSERTIONS = {
    'M01': {'response.onHand.value': '100', 'response.eligible.value': '40'},
    'M21': {'effects.businessEffectCount': 0},
    'M31': {'effects.businessEffectCount': 0, 'state.segment.B.qc': 'HOLD'},
    'M41': {'effects.authorizationEscalationCount': 0,
            'state.grant.readGrant.actions': ['READ']},
    'M43': {'response.goalDefinitionVersion': 'fixture-def-v1',
            'response.goalEndpoint': 'ARRIVED', 'response.currentSell': 'DENIED'},
    'M50': {'state.receipt.totalEffectCount': 1, 'effects.newReceiptCount': 0,
            'effects.newOutboxCount': 0},
    'M51': {'state.delivery.actual.value': '20',
            'state.transport.remaining.value': '10',
            'effects.newWarehouseDispatchCount': 0,
            'state.assessment.normalFulfillment': False},
    'M52': {'state.work.O1.fulfilled': False,
            'state.assessment.arrived.value': '90'},
    'M53': {'state.receipt.cumulative.value': '60',
            'state.evidence.documentCount': 2},
    'M54': {'state.intake.ownerRef': 'intakeOwner',
            'state.currentObligationCount': 1, 'state.followupWorkCount': 1},
    'M55': {'state.delivery.historical.value': '100',
            'state.return.received.value': '20'},
    'M56': {'state.delivery.currentActual.value': '98',
            'effects.returnCount': 0, 'state.currentShortfall.value': '2'},
    'M57': {'state.recall.distinctProcessed.value': '25',
            'state.recall.unknown.value': '25', 'state.recall.closed': False},
    'M59': {'response.errorCode': 'TYPE_INVALID', 'effects.allocationCount': 0},
}


def validate_schema(instance, schema):
    """Check the JSON Schema vocabulary used by the bundled schema, with stdlib.

    This is deliberately not a general-purpose Draft 2020-12 implementation.
    Unsupported validation keywords fail rather than silently passing.
    """
    errors = []
    supported = {'$schema', '$id', '$defs', '$ref', 'title', 'type', 'properties',
                 'required', 'additionalProperties', 'items', 'minItems',
                 'maxItems', 'minLength', 'pattern', 'enum', 'const', 'minimum'}

    def equal_json(left, right):
        return json.dumps(left, sort_keys=True) == json.dumps(right, sort_keys=True)

    def walk(value, rule, path):
        unknown = set(rule) - supported
        if unknown:
            errors.append(f'{path}: unsupported schema keywords {sorted(unknown)}')
            return
        if '$ref' in rule:
            target = rule['$ref']
            if not target.startswith('#/$defs/') or target[8:] not in schema.get('$defs', {}):
                errors.append(f'{path}: unresolved local schema reference {target}')
                return
            walk(value, schema['$defs'][target[8:]], path)
            return
        types = {'object': isinstance(value, dict), 'array': isinstance(value, list),
                 'string': isinstance(value, str), 'integer': type(value) is int,
                 'boolean': type(value) is bool, 'null': value is None}
        if 'type' in rule and not types.get(rule['type'], False):
            errors.append(f'{path}: schema expects {rule["type"]}')
            return
        if 'const' in rule and not equal_json(value, rule['const']):
            errors.append(f'{path}: schema const mismatch')
        if 'enum' in rule and not any(equal_json(value, v) for v in rule['enum']):
            errors.append(f'{path}: schema enum mismatch')
        if isinstance(value, dict):
            for key in rule.get('required', []):
                if key not in value:
                    errors.append(f'{path}: required property {key} absent')
            props = rule.get('properties', {})
            for key, child in value.items():
                if key in props:
                    walk(child, props[key], f'{path}.{key}')
                elif rule.get('additionalProperties') is False:
                    errors.append(f'{path}.{key}: additional property forbidden')
                elif isinstance(rule.get('additionalProperties'), dict):
                    walk(child, rule['additionalProperties'], f'{path}.{key}')
        if isinstance(value, list):
            if len(value) < rule.get('minItems', 0):
                errors.append(f'{path}: schema minItems violated')
            if 'maxItems' in rule and len(value) > rule['maxItems']:
                errors.append(f'{path}: schema maxItems violated')
            if 'items' in rule:
                for i, child in enumerate(value):
                    walk(child, rule['items'], f'{path}[{i}]')
        if type(value) in (int, float) and 'minimum' in rule and value < rule['minimum']:
            errors.append(f'{path}: schema minimum violated')
        if isinstance(value, str):
            if len(value) < rule.get('minLength', 0):
                errors.append(f'{path}: schema minLength violated')
            if 'pattern' in rule and not re.search(rule['pattern'], value):
                errors.append(f'{path}: schema pattern mismatch')
    walk(instance, schema, '$')
    return errors


def validate(data):
    schema = json.loads(Path(__file__).with_name("corpus.schema.json").read_text())
    errors = validate_schema(data, schema)

    def require(condition, message):
        if not condition:
            errors.append(message)

    def nonempty(value):
        return isinstance(value, str) and bool(value.strip())

    def no_input_leaks(value):
        if isinstance(value, dict):
            return not (EXPECTED_KEYS & value.keys()) and all(
                no_input_leaks(v) for v in value.values())
        if isinstance(value, list):
            return all(no_input_leaks(v) for v in value)
        return True

    if not isinstance(data, dict):
        return ['root must be an object']
    for key in ('schemaVersion', 'baselineCommit', 'status', 'commonFixture',
                'executionPlan', 'acceptanceProposal', 'modelInputContract',
                'observationContract', 'cases'):
        require(key in data, f'missing root field {key}')
    require(data.get('schemaVersion') == 'model-corpus-v1', 'unknown corpus version')
    require(data.get('status') == 'NOT_RUN', 'corpus must not claim runtime PASS')
    require(bool(re.fullmatch(r'[a-f0-9]{40}', data.get('baselineCommit', ''))),
            'baselineCommit must be exact commit')
    plan = data.get('executionPlan', {})
    require(plan.get('repeatCount') == 3, 'planned repeatCount must be 3')
    require(plan.get('plannedAttempts') == 180, 'plannedAttempts must be 180')
    for name in ('client', 'model', 'prompt', 'skillPackage', 'server'):
        require(isinstance(plan.get(name), dict) and
                plan[name].get('status') == 'NOT_RUN', f'{name} must remain NOT_RUN')
        if isinstance(plan.get(name), dict):
            require(all(v is None for k, v in plan[name].items() if k != 'status'),
                    f'{name} config must not invent a validated exact version')
    approval = plan.get('approval', {})
    require(approval.get('decisionRef') == 'R8' and
            approval.get('status') == 'NOT_AUTHORIZED', 'R8 must remain unapproved')
    for name in ('approvedCostCap', 'currency', 'approvedClientModel'):
        require(approval.get(name) is None, f'R8 {name} must be null without approval')
    for name in ('usage', 'totalCost'):
        require(name in plan and plan[name] is None, f'unexecuted {name} must be null')
    require(nonempty(plan.get('metricMissingReason')), 'missing metric reason')
    proposal = data.get('acceptanceProposal', {})
    require(proposal.get('approvedInR8') is False and
            proposal.get('status') == 'PROPOSAL_NOT_BUSINESS_SLA',
            'proposal must not become an approved business SLA')
    require(proposal.get('clearStructuredMinimumRatio') == '0.95',
            'clear structure proposal changed')
    for key in ('ambiguousImproperExecutionMaximum', 'unauthorizedEffectMaximum',
                'duplicateEffectMaximum', 'falseFulfillmentMaximum'):
        require(proposal.get(key) == 0, f'{key} must stay zero')
    fixture = data.get('commonFixture', {})
    for key in ('organizationId', 'ids', 'businessClock', 'authentication',
                'actors', 'grants', 'definition', 'policy', 'objects',
                'sources', 'evidence', 'approvalFacts'):
        require(key in fixture, f'commonFixture missing {key}')
    ids = fixture.get('ids', {})
    require(len(ids) == len(set(ids.values())), 'fixture UUID collision')
    for alias, value in ids.items():
        try:
            uuid.UUID(value)
        except (ValueError, TypeError, AttributeError):
            errors.append(f'{alias} invalid UUID')
    clock = fixture.get('businessClock', {})
    for key in ('asOf', 'knownAt', 'timezone', 'dueAt', 'dueInclusive'):
        require(key in clock, f'fixed clock missing {key}')
    require(fixture.get('approvalFacts') == [], 'commonFixture cannot preapprove all cases')
    for name, grant in fixture.get('grants', {}).items():
        require(grant.get('actions') and '*' not in grant['actions'],
                f'grant {name} cannot use wildcard actions')
        require(grant.get('targetScope') and '*' not in grant['targetScope'],
                f'grant {name} must have explicit target scope')
    require(fixture.get('grants', {}).get('readGrant', {}).get('actions') == ['READ'],
            'READ grant boundary weakened')
    require(fixture.get('actors', {}).get('readAgent', {}).get('roles') == ['WRITE', 'READ'],
            'C3 needs role WRITE plus only READ delegation')
    cases = data.get('cases', [])
    if not isinstance(cases, list):
        return errors + ['cases must be an array']
    require(len(cases) == 60, f'expected exactly 60 cases; got {len(cases)}')
    seen_ids, seen_focus, seen_first, seen_assertions = set(), set(), set(), set()
    counts, foreign = collections.Counter(), 0
    by_id = {}
    for case in cases:
        if not isinstance(case, dict):
            errors.append('case must be an object')
            continue
        cid = case.get('id', '?')
        require(cid not in seen_ids, f'duplicate case ID {cid}')
        seen_ids.add(cid)
        by_id[cid] = case
        require(bool(re.fullmatch(r'M(0[1-9]|[1-5][0-9]|60)', cid)), f'invalid ID {cid}')
        category = case.get('category')
        counts[category] += 1
        if re.fullmatch(r'M\d{2}', cid) and 1 <= int(cid[1:]) <= 60:
            n = int(cid[1:])
            expected_category = CATEGORY_ORDER[0 if n <= 20 else (n - 21) // 10 + 1]
            require(category == expected_category, f'{cid}: category does not match fixed distribution')
        for key in ('title', 'semanticFocus'):
            require(nonempty(case.get(key)), f'{cid}: missing {key}')
        focus = case.get('semanticFocus')
        require(focus not in seen_focus, f'{cid}: duplicate semantic focus')
        seen_focus.add(focus)
        langs = case.get('languages', [])
        require(isinstance(langs, list) and langs and set(langs) <= {'ko', 'it', 'en'},
                f'{cid}: invalid languages')
        foreign += bool(set(langs) & {'it', 'en'})
        refs = case.get('requirementRefs', [])
        require(bool(refs) and all(isinstance(r, str) and REQUIREMENT.fullmatch(r)
                                   for r in refs), f'{cid}: invalid requirement refs')
        require(isinstance(case.get('fixture'), dict), f'{cid}: missing isolated fixture override')
        execution = case.get('execution', {})
        require(execution.get('status') == 'NOT_RUN', f'{cid}: no runtime result allowed')
        require(execution.get('plannedRepeats') == 3, f'{cid}: plannedRepeats must be 3')
        require(execution.get('attempts') == [], f'{cid}: cannot invent executed attempts')
        for key in ('observed', 'usage', 'totalCost', 'latencyMs'):
            require(key in execution and execution[key] is None,
                    f'{cid}: {key} must be explicitly null')
        require(nonempty(execution.get('reason')), f'{cid}: NOT_RUN needs reason')
        turns = case.get('turns', [])
        require(bool(turns), f'{cid}: no utterance turns')
        require(category != 'ambiguity_target_conflict' or len(turns) >= 2,
                f'{cid}: ambiguity needs explicit followup input')
        case_assertions = []
        for index, t in enumerate(turns, 1):
            loc = f'{cid} turn {index}'
            inp = t.get('input', {})
            require(set(inp) == INPUT_KEYS, f'{loc}: input fields must contain raw input only')
            require(no_input_leaks(inp), f'{loc}: expected data leaked into model input')
            require(nonempty(inp.get('utterance')), f'{loc}: missing raw utterance')
            require(inp.get('fixtureRef') == cid, f'{loc}: wrong isolated fixture ref')
            require(inp.get('contextPolicy') == 'PERMITTED_CONTEXT_ONLY', f'{loc}: context exposure')
            require(inp.get('priorUserTurnRefs') == [f'turn:{i}' for i in range(1, index)],
                    f'{loc}: followup continuity missing')
            if index == 1:
                text = inp.get('utterance')
                require(text not in seen_first, f'{cid}: duplicated utterance')
                seen_first.add(text)
            intent = t.get('expectedIntent', {})
            require(intent.get('intentKind') in {'QUERY', 'RECORD', 'COMMAND'},
                    f'{loc}: intentKind must be one disjoint kind')
            require(intent.get('status') in {'STRUCTURED', 'NEEDS_INPUT'}, f'{loc}: invalid intent status')
            require(nonempty(intent.get('capabilityId')) and nonempty(intent.get('definitionVersion')),
                    f'{loc}: missing semantic capability/version')
            slots = intent.get('slots', {})
            require(isinstance(slots, dict), f'{loc}: missing slots')
            for key, value in slots.items():
                require(isinstance(value, dict) and 'value' in value and
                        value.get('provenance') in PROVENANCE and nonempty(value.get('sourceRef')),
                        f'{loc}: slot {key} lacks value/provenance/source')
                if isinstance(value, dict) and value.get('provenance') == 'USER':
                    require(value.get('sourceRef') in [f'turn:{i}' for i in range(1, index + 1)],
                            f'{loc}: slot {key} has future user provenance')
            oracle = t.get('oracle', {})
            require(nonempty(oracle.get('outcome')), f'{loc}: missing expected outcome')
            assertions = oracle.get('assertions', [])
            require(bool(assertions), f'{loc}: no independent assertion')
            require(any(a.get('path', '').startswith(('state.', 'response.', 'effects.'))
                        for a in assertions), f'{loc}: missing business observation')
            for a in assertions:
                require(nonempty(a.get('path')) and a.get('operator') in {'eq', 'lte', 'gte', 'contains'}
                        and 'expected' in a, f'{loc}: malformed assertion')
            case_assertions.extend(assertions)
            allowed = oracle.get('allowedEffects', [])
            require(bool(allowed), f'{loc}: missing explicit allowed audit/effects')
            allowed_classes = {a.get('class') for a in allowed}
            forbidden = oracle.get('forbiddenEffects', [])
            require(isinstance(forbidden, list) and len(forbidden) == len(set(forbidden)),
                    f'{loc}: malformed forbidden effects')
            require(not (allowed_classes & set(forbidden)), f'{loc}: allowed/forbidden effects overlap')
            require(PROTECTED <= allowed_classes | set(forbidden),
                    f'{loc}: protected effect class left unspecified')
            for a in allowed:
                require(nonempty(a.get('class')) and type(a.get('maxNew')) is int
                        and a['maxNew'] >= 0, f'{loc}: allowed effect needs finite bound')
            clarification = oracle.get('clarification', {})
            needs = intent.get('missingSlots', [])
            require(clarification.get('required') == (intent.get('status') == 'NEEDS_INPUT'),
                    f'{loc}: clarification and intent disagree')
            require(clarification.get('slots') == needs, f'{loc}: missing slots disagree')
            require(clarification.get('approvalByAnswer') is False,
                    f'{loc}: answer cannot supply hidden business approval')
            if needs:
                require(oracle.get('outcome') == 'NEEDS_INPUT' and
                        clarification.get('newBusinessEffectBeforeAnswer') == 0,
                        f'{loc}: incomplete intent must have zero business effects')
                require(PROTECTED <= set(forbidden), f'{loc}: ambiguity authorizes protected effects')
            if intent.get('intentKind') == 'QUERY':
                require(PROTECTED <= set(forbidden), f'{loc}: QUERY allows protected write')
            for ob in oracle.get('obligations', []):
                require(all(nonempty(ob.get(k)) for k in
                            ('kind', 'scopeRef', 'ownerRef', 'status', 'nextAction', 'nextCheckAt')),
                        f'{loc}: obligation lacks owner/action/check')
                require(ob.get('ownerRef') in fixture.get('actors', {}) and
                        fixture['actors'][ob['ownerRef']].get('human') is True,
                        f'{loc}: obligation must retain explicit human owner')
                if 'quantity' in ob:
                    require(isinstance(ob['quantity'].get('value'), str) and
                            nonempty(ob['quantity'].get('unit')), f'{loc}: obligation decimal/unit')
        signature = json.dumps(case_assertions, sort_keys=True)
        require(signature not in seen_assertions, f'{cid}: duplicate entire semantic oracle')
        seen_assertions.add(signature)
    require(dict(counts) == CATEGORY_COUNTS, f'category counts differ: {dict(counts)}')
    require(foreign >= 10, f'need at least 10 foreign/mixed cases; got {foreign}')
    for cid, expected in MANDATORY_ASSERTIONS.items():
        actual = {a.get('path'): a.get('expected') for t in by_id.get(cid, {}).get('turns', [])
                  for a in t.get('oracle', {}).get('assertions', []) if a.get('operator') == 'eq'}
        for path, value in expected.items():
            require(path in actual and type(actual[path]) is type(value) and actual[path] == value,
                    f'{cid}: required semantic assertion {path} changed or absent')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('path', nargs='?', type=Path,
                        default=Path(__file__).with_name('corpus.json'))
    args = parser.parse_args()
    try:
        data = json.loads(args.path.read_text())
        errors = validate(data)
    except (ValueError, OSError, TypeError, KeyError, AttributeError, IndexError) as exc:
        errors = [f'invalid corpus: {exc}']
    if errors:
        print(json.dumps({'corpusIntegrity': 'INVALID', 'errors': errors}, ensure_ascii=False, indent=2))
        return 1
    print(json.dumps({'corpusIntegrity': 'VALID', 'caseCount': 60,
                      'categoryCounts': CATEGORY_COUNTS, 'plannedAttempts': 180,
                      'foreignOrMixedCases': sum(bool(set(c['languages']) & {'en', 'it'})
                                                 for c in data['cases']),
                      'modelAcceptance': 'NOT_RUN', 'usage': None, 'totalCost': None},
                     ensure_ascii=False, indent=2))
    return 0


if __name__ == '__main__':
    sys.exit(main())
