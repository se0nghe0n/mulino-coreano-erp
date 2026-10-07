"""Pure checks over captured model evidence; never invokes a provider."""
from decimal import Decimal, InvalidOperation, localcontext
import re


def metrics(record):
    """Missing observations stay incomplete; observed invalid values are failures."""
    if not isinstance(record, dict):
        raise ValueError('Model accounting record must be an object')
    usage, cost = record.get('usage'), record.get('cost')
    tokens, amount, currency, pricing = None, None, None, None
    if usage is not None:
        if not isinstance(usage, dict):
            raise ValueError('Model usage must be an object')
        for key in ('inputTokens', 'outputTokens'):
            if key in usage and (type(usage[key]) is not int or usage[key] < 0):
                raise ValueError('Provider token counts must be nonnegative integers')
        if all(key in usage for key in ('inputTokens', 'outputTokens')):
            tokens = usage['inputTokens'], usage['outputTokens']
    if cost is not None:
        if not isinstance(cost, dict):
            raise ValueError('Model cost must be an object')
        if 'amount' in cost:
            value = cost['amount']
            if type(value) not in (str, int, float) or (type(value) is str and not re.fullmatch(r'\d+(?:\.\d+)?', value)):
                raise ValueError('Provider cost amount must be a nonnegative decimal')
            try:
                amount = Decimal(str(value))
            except InvalidOperation as error:
                raise ValueError('Invalid provider cost amount') from error
            if not amount.is_finite() or amount < 0:
                raise ValueError('Provider cost must be finite and nonnegative')
        if 'currency' in cost:
            currency = cost['currency']
            if not isinstance(currency, str) or not re.fullmatch(r'[A-Z]{3}', currency):
                raise ValueError('Provider cost currency must be an uppercase three-letter code')
        if 'pricingRef' in cost:
            pricing = cost['pricingRef']
            if not isinstance(pricing, str) or not pricing.strip():
                raise ValueError('Provider pricingRef is missing')
    if tokens is None or any(v is None for v in (amount, currency, pricing)):
        return None
    return tokens[0], tokens[1], amount, currency, pricing


def aggregate(record, children):
    observed = metrics(record)
    values = [metrics(child) for child in children]
    if observed is None or not values or any(v is None for v in values):
        return False
    if any(v[3:] != observed[3:] for v in values):
        raise ValueError('Model aggregate currency/pricingRef differs from provider calls')
    with localcontext() as context:
        context.prec = max(28, sum(len(str(v[2])) for v in values) + 10)
        if observed[:3] != tuple(sum(v[i] for v in values) for i in range(3)):
            raise ValueError('Model usage/cost aggregate differs from observed provider calls')
    return True


def selected_assertions(turn, binding):
    selected = turn.get('selectedPathId', turn.get('selectedPath'))
    if 'selectedPathId' in turn and 'selectedPath' in turn and turn['selectedPathId'] != turn['selectedPath']:
        raise ValueError('Conflicting selectedPathId/selectedPath')
    if selected is None:
        return None
    if selected not in binding['paths']:
        raise ValueError('Selected model path is not allowed by immutable corpus')
    expected = dict(binding['common'])
    expected.update(binding['paths'][selected])
    results = turn.get('assertionResults', [])
    if not isinstance(results, list):
        raise ValueError('Model assertionResults must be an array')
    ids = [a.get('assertionId') for a in results]
    if len(ids) != len(set(ids)) or set(ids) != set(expected):
        raise ValueError('Model common plus selected-path assertion membership differs')
    for assertion in results:
        if assertion.get('semanticPath') != expected[assertion['assertionId']]['semanticPath']:
            raise ValueError('Model assertion semantic path differs from selected binding')
    return all(a.get('status') == 'PASS' for a in results)


def accounting(runtime, receipt):
    """Exact globally unique provider calls, with turn/attempt/report rollups."""
    metrics(dict(runtime, cost=runtime.get('cost', runtime.get('totalCost'))))
    attempts = runtime.get('attempts', [])
    seen = set()
    complete = bool(attempts)
    all_calls_observed = bool(attempts)
    for attempt in attempts:
        metrics(attempt)
        turns = attempt.get('turnResults', [])
        attempt_count = 0
        calls_observed = True
        for turn in turns:
            metrics(turn)
            calls = turn.get('modelCalls')
            if calls is None or calls == []:
                complete = False
                calls_observed = False
                continue
            if not isinstance(calls, list):
                raise ValueError('Provider modelCalls must be an array')
            for call in calls:
                identity = call.get('callId')
                if not isinstance(identity, str) or not identity.strip() or identity in seen:
                    raise ValueError('Provider call identity missing or duplicated')
                seen.add(identity)
                if type(call.get('repeat')) is not int or (call.get('caseId'), call.get('repeat'), call.get('turnId')) != (attempt.get('caseId'), attempt.get('repeat'), turn.get('turnId')):
                    raise ValueError('Provider call case/repeat/turn identity differs')
                if any(call.get(k) is None for k in ('provider', 'model')):
                    complete = False
                elif any(not isinstance(call[k], str) or not call[k].strip() for k in ('provider', 'model')):
                    raise ValueError('Invalid actual provider/model identity')
                if receipt and call.get('model') is not None and call['model'] != receipt['versions']['model']:
                    raise ValueError('Provider model differs from receipt version')
                refs = call.get('artifactRefs')
                if not isinstance(refs, list) or not refs:
                    complete = False
                elif receipt and any(ref not in receipt['_artifactPaths'] for ref in refs):
                    raise ValueError('Provider call artifact is outside actual receipt')
                value = metrics(call)
                if value is None:
                    complete = False
                elif value[0] == 0:
                    raise ValueError('Actual provider call claims zero input tokens')
            attempt_count += len(calls)
            if turn.get('actualModelCalls') is None:
                complete = False
            elif type(turn['actualModelCalls']) is not int or turn['actualModelCalls'] < 0 or turn['actualModelCalls'] != len(calls):
                raise ValueError('Turn actualModelCalls differs from provider call identities')
            complete = aggregate(turn, calls) and complete
        if attempt.get('actualModelCalls') is None:
            complete = False
        elif type(attempt['actualModelCalls']) is not int or attempt['actualModelCalls'] < 0 or (calls_observed and attempt['actualModelCalls'] != attempt_count):
            raise ValueError('Attempt actualModelCalls differs from turn provider calls')
        all_calls_observed = calls_observed and all_calls_observed
        complete = aggregate(attempt, turns) and complete
    if runtime.get('actualModelCalls') is None:
        complete = False
    elif type(runtime['actualModelCalls']) is not int or runtime['actualModelCalls'] < 0 or (all_calls_observed and runtime['actualModelCalls'] != len(seen)):
        raise ValueError('Report actualModelCalls differs from unique provider calls')
    complete = aggregate(dict(runtime, cost=runtime.get('cost', runtime.get('totalCost'))), attempts) and complete
    return complete
