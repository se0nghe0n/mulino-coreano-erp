#!/usr/bin/env python3
"""Request contracts of case invoke/query actions (step2r round 12); never a product or test driver.

The first actual run (docs/execution/s5a-adapter/run-61bd0f8a.json) showed every api command and most queries
rejected for envelope shape before any business rule ran. This module owns the request-side rules of
contracts/request-contracts.json:

- commands: every intent body validates against contracts/intent.schema.json (the product-published envelope)
  with a provenance entry for every slot;
- queries: only the fields, scope keys and filters of the product query envelope (QueryRequests and the
  per-handler guards transcribed in the contract file);
- grants: a fixture grant naming two or more product dimension kinds declares scopeComposition.

conform_case/conform_fixture rewrite authored requests into those contracts (used by every case generator and
docs/execution/step2r-round12/author_round12.py); case_problems/fixture_problems check them (used by
check_request_contracts.py, which ./verify prepare runs). A request a case sends on purpose to prove rejection
keeps its fields and declares them in action.harness.intentionalViolation. A needed selector the product
contract lacks stays in the request and is reported against a knownOpen product gap instead of a new key.
"""
import copy
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UUID_RE = re.compile(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')
REFERENCE_KEYS = ('$alias', '$result', '$transform')
CASE_NAMESPACE = ('caseId', 'subcaseId', 'scenarioId', 'caseNamespace', 'caseKey')
BARRIER_KEYS = {'testTransactionId': 'transactionId', 'testParticipantId': 'participantId', 'testBarrierId': 'barrierId', 'testBarrierPoint': 'point'}
# Query parameters that only narrowed a case's own reading and that the product answers without (the response
# already holds the full projection, the evaluation time equals asOf, the case namespace is the subcase's own
# organization). Removing them keeps the oracle meaning.
QUERY_DROP = ('intentKind', 'capabilityId', 'include', 'evaluationTime', 'rawRowsOrder', 'issueKinds', 'quantityMode', 'endpoint',
              'predicateInputId', 'evaluatorVersion', 'current', 'includeAvailability', 'includeValidation', 'includeDrafts', 'accessMode', 'expand', 'download')
QUERY_SCOPE_DROP = CASE_NAMESPACE + ('includeDescendants', 'rawRowsOrder', 'workIds', 'obligationRootId')
QUERY_ID_KEYS = ('objectId', 'policyId', 'grantId', 'definitionId', 'commandId', 'evidenceId')
OBJECT_OPS = ('getObject', 'searchObjects', 'getInventory', 'traceLot', 'getTrace')
WORK_OPS = ('getWork', 'searchWorks', 'getObligations', 'getAssessment')
EVIDENCE_OPS = ('getEvidence', 'getInbox', 'getReconciliation')
SUBJECT_KINDS = {'TradeItem': 'ITEM', 'ManufacturingLot': 'LOT', 'ManufacturerLot': 'LOT', 'QuantitySegment': 'SEGMENT', 'Work': 'WORK',
                 'Place': 'PLACE', 'PurchaseOrder': 'PURCHASE_ORDER'}

# Explicit per-action decisions (caseId, subcaseId, actionId). Everything else follows the general rules.
OVERRIDES = {
    ('T08', 'payload-org', 'attack'): {'intentional': ['organizationId', 'actorId'],
        'reason': 'the payload claims another organization and actor; plan section 3.3 lets only the authenticated context set them'},
    ('T08', 'payload-actor', 'attack'): {'intentional': ['actorId', 'delegatorId', 'role'],
        'reason': 'the payload claims another actor, delegator and role; plan section 3.3 lets only the authenticated context set them'},
    ('T24', 'deny-worker', 'attack'): {'intentional': ['originalActorId', 'originalDelegatorId'],
        'reason': 'the worker payload forges the original requester; the runtime task context, never the payload, carries it (plan section 7.1)'},
    ('T24', 'sentinel-artifact-scan', 'request-with-sentinel'): {'intentional': ['forceValidationError', 'syntheticAuthenticationMetadata'],
        'reason': 'a rejected request must carry the synthetic secret so the artifact scan can prove it is never logged',
        'unpinnedBecause': 'the subcase asserts the six artifact surfaces, not the response of this request'},
    ('T26', 'safe-retry-forged-original-actor', 'retry'): {'intentional': ['originalActorId'],
        'reason': 'the retry payload forges the original actor; the stored command, never the payload, names it (plan section 7.2)'},
    ('T04', 'scale-overflow', 'invalid'): {'intentional': ['slots/quantity'],
        'reason': 'a quantity beyond 12 fraction digits must be rejected, never rounded (plan section 3.1)'},
    # The user answered the destination question (MRTR inputResponses), so the supplied destination is the user's statement.
    ('V6', 'destination-input-supplement', 'input-complete'): {'provenance': {'destinationId': 'USER'}},
    # The user named an item that matches two items: the subject is unresolved text, so it is a USER slot, not a typed subject.
    ('T20', 'ambiguousSameName', 'structured'): {'transform': lambda body: unresolved_subject(body)},
    # The story is a cross-organization subject, not a payload claim: the ORG-A administrator names the ORG-B human.
    ('T24', 'deny-admin', 'attack'): {'dropForeignOrganization': True},
}


def unresolved_subject(body):
    kept = []
    for ref in body.get('subjectRefs') or []:
        if isinstance(ref, dict) and 'id' not in ref and isinstance(ref.get('name'), str):
            body.setdefault('slots', {})['itemName'] = ref['name']
        else:
            kept.append(ref)
    body['subjectRefs'] = kept


def load_json(root, rel):
    return json.loads((Path(root) / rel).read_text())


def is_ref(value):
    return isinstance(value, dict) and len(value) == 1 and next(iter(value)) in REFERENCE_KEYS


def is_typed_ref(value):
    return isinstance(value, dict) and set(value) == {'type', 'id'}


# ---- minimal JSON Schema (draft 2020-12 subset of contracts/intent.schema.json) --------------------------------------
def _type_ok(value, name):
    return {'object': isinstance(value, dict), 'array': isinstance(value, list), 'string': isinstance(value, str),
            'boolean': isinstance(value, bool), 'null': value is None,
            'integer': isinstance(value, int) and not isinstance(value, bool),
            'number': isinstance(value, (int, float)) and not isinstance(value, bool)}[name]


def schema_errors(value, schema, root, path=''):
    """Errors of value against schema. Unknown keywords fail closed so a schema change cannot pass silently."""
    out = []
    if 'type' in schema:
        types = schema['type'] if isinstance(schema['type'], list) else [schema['type']]
        if not any(_type_ok(value, t) for t in types):
            return [f'{path or "/"}: expected {"|".join(types)}']
    for key, rule in schema.items():
        if key in ('type', '$schema', '$id', 'title', 'description', '$defs'):
            continue
        if key == '$ref':
            if not rule.startswith('#/'):
                raise ValueError('external $ref ' + rule)
            node = root
            for part in rule[2:].split('/'):
                node = node[part]
            out += schema_errors(value, node, root, path)
        elif key == 'required':
            out += [f'{path}/{name}: required' for name in rule if isinstance(value, dict) and name not in value]
        elif key == 'properties':
            if isinstance(value, dict):
                for name, sub in rule.items():
                    if name in value:
                        out += schema_errors(value[name], sub, root, f'{path}/{name}')
        elif key == 'additionalProperties':
            if isinstance(value, dict):
                for name in value:
                    if name in schema.get('properties', {}):
                        continue
                    if rule is False:
                        out.append(f'{path}/{name}: not allowed')
                    elif isinstance(rule, dict):
                        out += schema_errors(value[name], rule, root, f'{path}/{name}')
        elif key == 'enum':
            if value not in rule:
                out.append(f'{path or "/"}: {json.dumps(value, ensure_ascii=False)[:60]} not in {rule}')
        elif key == 'const':
            if value != rule:
                out.append(f'{path or "/"}: must be {rule}')
        elif key == 'minLength':
            if isinstance(value, str) and len(value) < rule:
                out.append(f'{path}: shorter than {rule}')
        elif key == 'pattern':
            if isinstance(value, str) and not re.search(rule, value):
                out.append(f'{path}: {value[:40]!r} does not match {rule}')
        elif key == 'format':
            if rule == 'uuid' and isinstance(value, str) and not UUID_RE.match(value):
                out.append(f'{path}: {value[:40]!r} is not a UUID')
        elif key == 'items':
            if isinstance(value, list):
                for i, item in enumerate(value):
                    out += schema_errors(item, rule, root, f'{path}/{i}')
        elif key == 'uniqueItems':
            if rule and isinstance(value, list) and len({json.dumps(v, sort_keys=True) for v in value}) != len(value):
                out.append(f'{path}: duplicate items')
        elif key == 'minimum':
            if _type_ok(value, 'number') and value < rule:
                out.append(f'{path}: below {rule}')
        elif key in ('oneOf', 'anyOf'):
            matched = sum(1 for sub in rule if not schema_errors(value, sub, root, path))
            if (key == 'oneOf' and matched != 1) or (key == 'anyOf' and matched < 1):
                out.append(f'{path or "/"}: {key} matched {matched}')
        elif key == 'not':
            if not schema_errors(value, rule, root, path):
                out.append(f'{path or "/"}: matches a forbidden shape')
        else:
            raise ValueError('unsupported schema keyword ' + key)
    return out


class Placeholders:
    """References are resolved at run time; validation sees a stable typed stand-in (same reference, same value)."""
    def __init__(self):
        self.seen = {}

    def __call__(self, node, key=None):
        if is_ref(node):
            if key in ('expectedRevision', 'proposalRevision'):
                return 0
            if key == 'canonicalIntentHash':
                return 'a' * 64
            token = json.dumps(node, sort_keys=True)
            if token not in self.seen:
                self.seen[token] = f'00000000-0000-4000-8000-{len(self.seen) + 1:012d}'
            return self.seen[token]
        if isinstance(node, dict):
            return {k: self(v, k) for k, v in node.items()}
        if isinstance(node, list):
            return [self(v, key) for v in node]
        return node


# ---- fixture world ---------------------------------------------------------------------------------------------------
class World:
    """Merged aliases and actors of a subcase fixture (baseRefs first, the fixture itself last)."""
    def __init__(self, root, ref, cache=None):
        self.aliases, self.actors, self.evidence = {}, {}, set()
        if ref:
            self._merge(Path(root), ref, set(), cache if cache is not None else {})

    def _merge(self, root, ref, visiting, cache):
        if ref in visiting or not (root / ref).is_file():
            return
        visiting.add(ref)
        if ref not in cache:
            cache[ref] = json.loads((root / ref).read_text())
        data = cache[ref]
        for base in data.get('baseRefs') or []:
            self._merge(root, base, visiting, cache)
        self.aliases.update(data.get('aliases') or {})
        self.actors.update(data.get('actors') or {})
        evidence = data.get('evidence')
        for row in evidence if isinstance(evidence, list) else []:
            if isinstance(row, dict) and isinstance(row.get('alias'), str):
                self.evidence.add(row['alias'])

    def known_alias(self, name):
        return name in self.aliases or name in self.evidence

    def alias_type(self, value):
        return (self.aliases.get(value['$alias']) or {}).get('type') if isinstance(value, dict) and '$alias' in value else None

    def actor_org(self, actor):
        return (self.actors.get(actor) or {}).get('organizationAlias')

    def org_items(self, org):
        return sorted(a for a, v in self.aliases.items() if v.get('type') == 'TradeItem' and v.get('organizationAlias') == org)


# ---- conformance -----------------------------------------------------------------------------------------------------
class Contracts:
    def __init__(self, root=ROOT):
        self.root = Path(root)
        self.intent = load_json(self.root, 'contracts/intent.schema.json')
        self.data = load_json(self.root, 'contracts/request-contracts.json')
        self.intent_fields = list(self.intent['properties'])
        routes = self.data['commandRoutes']
        self.intent_routes = set(routes['intentBody'])
        self.addressing = routes['intentWithAddressing']
        self.query = self.data['queryEnvelope']
        self.dimensions = self.data['grantComposition']['dimensionKinds']
        self.known = self.data['knownOpen']
        self.kinds = {c['id']: c['kind'] for c in load_json(self.root, 'contracts/acceptance-capabilities.json')['capabilities']}

    # -- commands --
    def conform_case(self, case, cache=None):
        cache = {} if cache is None else cache
        for sub in case.get('subcases', []):
            world = World(self.root, sub.get('fixtureRef'), cache)
            prior = []
            for action in iter_actions(sub.get('actions', [])):
                self.conform_action(action, case.get('caseId'), sub.get('id'), world, prior)
                prior.append(action)
        return case

    def conform_action(self, action, case_id, sub_id, world, prior=()):
        override = OVERRIDES.get((case_id, sub_id, action.get('id')), {})
        if not isinstance(action.get('request'), dict):
            return
        # Generators share dicts between actions (one scope object for many reads); conform a private copy.
        request = action['request'] = copy.deepcopy(action['request'])
        route = action.get('route')
        if action.get('kind') == 'invoke':
            if route in self.intent_routes or route in self.addressing:
                self.conform_intent(request, action, world, override, skip=self.addressing.get(route))
            elif route == 'batch':
                if 'commands' in request:
                    self.batch_from_commands(request, world)
                for op in request.get('operations', []):
                    self.conform_intent(op, action, world, override)
            elif route == 'blob':
                self.conform_intent(request['businessAction'] if 'businessAction' in request else request, action, world, override)
        elif action.get('kind') == 'query' and route in self.query['routes']:
            self.conform_query(request, action, world, prior)
        if route == 'wire' and action.get('kind') in ('invoke', 'query'):
            for args, proxy in self.tool_calls(action):
                if self.kinds.get(proxy['capabilityId']) == 'QUERY' and proxy['capabilityId'] != 'structureIntent':
                    self.conform_query(args, proxy, world)
                else:
                    self.conform_intent(args, proxy, world, override)
                merge_harness(action, proxy.get('harness') or {})

    def tool_calls(self, action):
        """(arguments, proxy action) of every MCP tools/call in a wire request: the arguments are the tool input
        (backend OntologyMcp: CommandSchemas.input for commands, the query fields for reads)."""
        body = (action.get('request') or {}).get('body')
        for element in body if isinstance(body, list) else [body]:
            params = element.get('params') if isinstance(element, dict) and element.get('method') == 'tools/call' else None
            if isinstance(params, dict) and isinstance(params.get('arguments'), dict) and isinstance(params.get('name'), str):
                yield params['arguments'], {'id': action.get('id'), 'route': 'mcp', 'actorRef': action.get('actorRef'), 'capabilityId': params['name']}

    def batch_from_commands(self, request, world):
        """T24 deny-batch authored a private batch shape; the batch route carries intent bodies in operations."""
        key, commands = request['commandIdempotencyKey'], request.pop('commands')
        operations = []
        for i, command in enumerate(commands):
            subject = command['subjectId']
            operations.append({'intentKind': 'COMMAND', 'definitionVersion': request['definitionVersion'], 'capabilityId': command['capabilityId'],
                               'subjectRefs': [{'type': world.alias_type(subject) or 'QuantitySegment', 'id': subject}],
                               'slots': {'quantity': {'value': command['quantity'], 'unit': command['unit']}},
                               'expectedRevision': request.get('expectedRevision', 1), 'commandIdempotencyKey': f'{key}-{i + 1}'})
        for name in list(request):
            request.pop(name)
        request.update({'atomic': True, 'changesetId': key, 'operations': operations})

    def conform_intent(self, body, action, world, override, skip=None):
        harness = {}
        intentional = list(override.get('intentional', []))
        stated = dict(override.get('provenance') or {})
        if isinstance(body.get('valueProvenance'), dict):
            stated.update(body['valueProvenance'])
        body.pop('valueProvenance', None)
        barrier = {BARRIER_KEYS[k]: body.pop(k) for k in list(BARRIER_KEYS) if k in body}
        if barrier:
            harness['testBarrier'] = barrier
        for key in ('executionContext', 'environmentId'):
            if key in body:
                harness[key] = body.pop(key)
        if action.get('route') == 'mcp':
            mcp = {k: body.pop(k) for k in ('transport', 'requestState', 'inputResponses') if k in body}
            if mcp:
                harness['mcp'] = mcp
        else:
            body.pop('requestState', None)  # api continuation is conversationRequestId (plan section 3.3)
        for key in ('scope', 'asOf', 'knownAt', 'requesterContext'):
            body.pop(key, None)  # commands run at the product clock in the authenticated context (plan sections 3.3, 7.1)
        if 'organizationId' in body and 'organizationId' not in intentional:
            org = body.pop('organizationId')
            own = world.actor_org(action.get('actorRef'))
            if isinstance(org, dict) and own and org.get('$alias') != own and not override.get('dropForeignOrganization'):
                raise ValueError(f"{action.get('id')}: foreign organizationId {org} without an override")
        for old, new in (('originalTextRef', 'sourceRefs'), ('documentText', 'sourceRefs'), ('contextRef', 'contextRefs')):
            if old in body:
                body.setdefault(new, []).append(body.pop(old))
        if 'canonicalRequestHash' in body:
            rename(body, 'canonicalRequestHash', 'canonicalIntentHash')
        if isinstance(body.get('evidenceRefs'), list):
            refs = []
            for ref in body['evidenceRefs']:
                if isinstance(ref, str) and not UUID_RE.match(ref):
                    if world.known_alias(ref):
                        refs.append({'$alias': ref})  # a fixture evidence alias written without $alias
                    else:
                        body.setdefault('sourceRefs', []).append(ref)  # an external source, not an installed evidence object
                else:
                    refs.append(ref)
            body['evidenceRefs'] = refs
        if callable(override.get('transform')):
            override['transform'](body)
        slots = body.setdefault('slots', {})
        if 'proposalRevision' in slots and 'proposalRevision' not in body:
            value, _ = normalize_slot(slots.pop('proposalRevision'))
            body['proposalRevision'] = value  # the intent field that binds an approval to the proposal (plan section 3.3)
        for key in list(body):
            if key in self.intent_fields or key in intentional or key == skip:
                continue
            value = body.pop(key)
            if key in slots and slots[key] != value:
                raise ValueError(f"{action.get('id')}: {key} differs between request and slots")
            slots[key] = value
        body.setdefault('subjectRefs', [])
        if 'capabilityId' not in body and action.get('capabilityId'):
            body['capabilityId'] = action['capabilityId']
        provenance = dict(body.get('provenance') or {})
        for name in list(slots):
            value, said = normalize_slot(slots[name])
            if value is _ABSENT:
                del slots[name]
                provenance.pop(name, None)
                continue
            slots[name] = value
            provenance[name] = provenance.get(name) or stated.get(name) or said or default_provenance(value)
        body['provenance'] = {name: provenance[name] for name in slots}
        if intentional:
            harness['intentionalViolation'] = {k: v for k, v in (('fields', sorted(intentional)), ('reason', override['reason']),
                                                                ('unpinnedBecause', override.get('unpinnedBecause'))) if v}
        merge_harness(action, harness)

    # -- queries --
    def conform_query(self, request, action, world, prior=()):
        op, harness = action.get('capabilityId'), {}
        scope = request.get('scope') if isinstance(request.get('scope'), dict) else None
        env = request.pop('environmentId', None)
        if scope is not None and 'environmentId' in scope:
            env2 = scope.pop('environmentId')
            if env is not None and env2 != env:
                raise ValueError(f"{action.get('id')}: two environment ids")
            env = env2
        if env is not None:
            harness['environmentId'] = env
        if 'authenticationTest' in request:
            harness['authenticationTest'] = request.pop('authenticationTest')
        if request.get('evaluationTime') not in (None, request.get('asOf')):
            raise ValueError(f"{action.get('id')}: evaluationTime differs from asOf")
        if 'targetOrganizationId' in request:
            target = request.pop('targetOrganizationId')
            if scope is None or scope.get('organizationId') != target:
                raise ValueError(f"{action.get('id')}: targetOrganizationId differs from the scope organization")
        if op == 'getAccessContext' and request.get('actorId') == {'$alias': action.get('actorRef')}:
            request.pop('actorId')  # the product answers the authenticated caller
        slots = request.pop('slots', None) or {}
        subjects = request.pop('subjectRefs', None) or []
        if slots.get('effectiveAt') not in (None, request.get('asOf')):
            raise ValueError(f"{action.get('id')}: effectiveAt differs from asOf")
        slots.pop('effectiveAt', None)
        for key in QUERY_DROP:
            request.pop(key, None)
        if op == 'getCommandResult' and 'commandId' in request:
            request.pop('commandIdempotencyKey', None)
        if scope is None:
            scope = {}
        if 'organizationId' in request:
            scope['organizationId'] = request.pop('organizationId')  # the organization the reader asks for (T24 cross-organization search)
        for key in QUERY_SCOPE_DROP:
            scope.pop(key, None)
        if 'organizationIds' in scope:
            scope.pop('organizationIds')
            org = world.actor_org(action.get('actorRef'))
            scope['organizationId'] = {'$alias': org}
            items = world.org_items(org)
            if 'itemId' not in scope and op in OBJECT_OPS and len(items) == 1:
                scope['itemId'] = {'$alias': items[0]}
        if isinstance(request.get('filter'), dict):
            for key, value in request.pop('filter').items():
                if key == 'organizationId':
                    scope[key] = value
                elif key == 'name' and op in OBJECT_OPS:
                    continue  # the object handlers have no name filter; the attack is the foreign organization
                else:
                    request.setdefault(key, value)
        for key, value in slots.items():
            if key in ('definitionId', 'evidenceId', 'objectId'):
                request.setdefault('id', value)
            elif key in QUERY_DROP:
                continue
            else:
                request.setdefault(key, value)
        for key in QUERY_ID_KEYS:
            if key in request:
                if 'id' in request:
                    if request.pop(key) != request['id']:
                        raise ValueError(f"{action.get('id')}: {key} differs from id")
                else:
                    rename(request, key, 'id')
        for ref in subjects:
            self.place_subject(request, scope, op, ref)
        if op == 'getObject' and 'type' not in request and 'objectType' not in scope:
            kind = world.alias_type(request.get('id'))
            if kind and kind != 'TradeItem':
                request['type'] = kind
        self.place_by_operation(request, scope, op)
        if op == 'getObject' and 'id' not in request and 'itemId' in scope:
            request['id'] = scope['itemId']  # the object a getObject without id reads is the item its scope names
        if op in ('getAssessment', 'getWork') and 'id' not in request and 'workId' not in request:
            works = [a for a in prior if a.get('kind') == 'invoke' and a.get('capabilityId') in ('createWork', 'createDraft')]
            if len(works) == 1:
                request['workId'] = {'$result': {'actionId': works[0]['id'], 'pointer': '/response/workId'}}  # the subcase's only Work
        if scope:
            if 'scope' not in request:
                request['scope'] = scope
        else:
            request.pop('scope', None)
        merge_harness(action, harness)

    def place_subject(self, request, scope, op, ref):
        kind, value = ref.get('type'), ref.get('id')
        if op in ('getObject',) and 'id' not in request:
            request['id'] = value
            if kind != 'TradeItem':
                request['type'] = kind
        elif op in EVIDENCE_OPS:
            if 'subjectId' not in scope and kind in SUBJECT_KINDS:
                scope['subjectKind'], scope['subjectId'] = SUBJECT_KINDS[kind], value
        elif kind == 'TradeItem':
            if op in WORK_OPS and 'itemId' not in scope:
                request.setdefault('itemId', value)
            elif op not in WORK_OPS:
                scope.setdefault('itemId', value)
        elif kind == 'Work':
            request.setdefault('workId', value)
        elif kind in ('ManufacturingLot', 'ManufacturerLot'):
            scope.setdefault('lotId', value)

    def place_by_operation(self, request, scope, op):
        if op in OBJECT_OPS or op in ('evaluateEligibility', 'convertUnit'):
            for key in ('itemId', 'lotId', 'placeId'):
                if key in request:
                    scope.setdefault(key, request.pop(key))
        if op in ('getInventory', 'traceLot', 'getTrace'):
            request.pop('workId', None)  # no work-scoped inventory in the plan or product; the item scope holds the material
            scope.pop('workId', None)
        if op == 'getAssessment':
            for key in [k for k in scope if k not in ('organizationId', 'workId')]:
                scope.pop(key)
        if op == 'searchOperationalIssues':
            if 'workId' in request:
                scope.setdefault('workId', request.pop('workId'))
            for key in [k for k in scope if k not in ('organizationId', 'workId')]:
                scope.pop(key)
        if op in EVIDENCE_OPS:
            for key, kind in (('itemId', 'ITEM'), ('lotId', 'LOT'), ('placeId', 'PLACE'), ('workId', 'WORK')):
                if key in scope:
                    value = scope.pop(key)
                    if 'subjectId' not in scope:
                        scope['subjectKind'], scope['subjectId'] = kind, value
            if 'placeId' in request:
                request.pop('placeId')
            if 'itemId' in request and 'subjectId' not in scope:
                scope['subjectKind'], scope['subjectId'] = 'ITEM', request.pop('itemId')
            request.pop('itemId', None)
        if op not in OBJECT_OPS and 'placeId' in request:
            scope.setdefault('placeId', request.pop('placeId'))

    # -- fixtures --
    def grant_kinds(self, grant):
        kinds = set()
        for key, value in (grant.get('scope') or {}).items():
            if key in self.dimensions and value not in (None, [], ''):
                kinds.add(self.dimensions[key])
        return kinds

    def conform_fixture(self, fixture):
        for actor in (fixture.get('actors') or {}).values():
            grant = actor.get('grant')
            if isinstance(grant, dict) and len(self.grant_kinds(grant)) >= 2 and 'scopeComposition' not in grant:
                rebuilt = {}
                for key, value in grant.items():
                    rebuilt[key] = value
                    if key == 'scope':
                        rebuilt['scopeComposition'] = 'PER_DIMENSION'
                grant.clear()
                grant.update(rebuilt)
        return fixture

    # -- checks --
    def case_problems(self, case):
        """(problems, knownOpen) of every invoke/query request of a case."""
        problems, known = [], []
        for sub in case.get('subcases', []):
            for action in iter_actions(sub.get('actions', [])):
                where = f"{case.get('caseId')}/{sub.get('id')}/{action.get('id')}"
                if action.get('route') == 'wire' and action.get('kind') in ('invoke', 'query'):
                    p, k = self.wire_problems(action, sub)
                elif action.get('kind') == 'invoke':
                    p, k = self.command_problems(action, sub)
                elif action.get('kind') == 'query':
                    p, k = self.query_problems(action)
                else:
                    continue
                problems += [f'{where}{x}' for x in p]
                known += [f'{where}{x}' for x in k]
                if action.get('harness', {}).get('intentionalViolation') and action.get('kind') != 'invoke':
                    problems.append(f'{where}: intentionalViolation is declared on a non-command action')
        return problems, known

    def command_problems(self, action, sub):
        route, request = action.get('route'), action.get('request')
        if not isinstance(request, dict):
            return [': request must be an object'], []
        bodies, problems = [], []
        if route in self.intent_routes:
            bodies = [('', request, None)]
        elif route in self.addressing:
            bodies = [('', request, self.addressing[route])]
        elif route == 'batch':
            envelope = self.data['commandRoutes']['batch']['envelope']
            problems += [f'/{k}: not a batch envelope field' for k in request if k not in envelope]
            if not isinstance(request.get('operations'), list) or not request['operations']:
                problems.append('/operations: batch needs intent operations')
            bodies = [(f'/operations/{i}', op, None) for i, op in enumerate(request.get('operations') or [])]
        elif route == 'blob':
            if 'businessAction' in request:
                envelope = self.data['commandRoutes']['blob']['envelope']
                problems += [f'/{k}: not a blob envelope field' for k in request if k not in envelope]
                bodies = [('/businessAction', request['businessAction'], None)]
            else:
                bodies = [('', request, None)]
        else:
            return [], []
        known = []
        declared = (action.get('harness') or {}).get('intentionalViolation')
        violations = {}
        for prefix, body, skip in bodies:
            p, k, v = self.intent_problems(body, action, skip)
            problems += [prefix + x for x in p]
            known += [prefix + x for x in k]
            for key, messages in v.items():
                violations.setdefault(key, []).extend(prefix + m for m in messages)
        if declared:
            fields = set(declared.get('fields') or [])
            if fields != set(violations):
                problems.append(f': intentionalViolation declares {sorted(fields)} but the request violates {sorted(violations)}')
            if not declared.get('reason'):
                problems.append(': intentionalViolation needs a reason')
            if not declared.get('unpinnedBecause') and not pinned_rejection(sub, action.get('id')):
                problems.append(': intentionalViolation needs a pinned rejection (/response/outcome REJECTED|CONFLICT or /response/error/code) or unpinnedBecause')
        else:
            problems += [m for key in sorted(violations) for m in violations[key]]
        return problems, known

    def intent_problems(self, body, action, skip):
        """(problems, knownOpen, violations by field or slots/<name>) of one intent body against contracts/intent.schema.json."""
        if not isinstance(body, dict):
            return [': intent body must be an object'], [], {}
        body = {k: v for k, v in body.items() if k != skip}
        known, problems, violations = [], [], {}
        slots = body.get('slots')
        candidate = copy.deepcopy(body)
        if isinstance(slots, dict):
            for name, value in slots.items():
                if isinstance(value, list):
                    known.append(f'/slots/{name}: KNOWN_OPEN PG-INTENT-ARRAY-SLOT (list slot, intent.schema.json has no array value)')
                    candidate['slots'][name] = 'array-slot'
        candidate = Placeholders()(candidate)
        for error in schema_errors(candidate, self.intent, self.intent):
            path = error.split(':', 1)[0]
            parts = path.strip('/').split('/')
            key = '/'.join(parts[:2]) if parts[0] == 'slots' and len(parts) > 1 else parts[0]
            suffix = ' (contracts/intent.schema.json)' if error.endswith('not allowed') else ''
            violations.setdefault(key or '/', []).append(error + suffix)
        if isinstance(slots, dict) and isinstance(body.get('provenance'), dict) and set(slots) != set(body['provenance']):
            problems.append(f'/provenance: keys {sorted(body["provenance"])} differ from slots {sorted(slots)}')
        capability = action.get('capabilityId')
        if capability != 'structureIntent' and action.get('route') != 'batch' and body.get('capabilityId') not in (None, capability):
            problems.append(f'/capabilityId: {body.get("capabilityId")} differs from the action capability {capability}')
        if capability != 'structureIntent' and 'commandIdempotencyKey' not in body:
            problems.append('/commandIdempotencyKey: required to execute (backend CommandSchemas.input)')
        return problems, known, violations

    def wire_problems(self, action, sub):
        problems, known = [], []
        for i, (args, proxy) in enumerate(self.tool_calls(action)):
            proxy['harness'] = action.get('harness') or {}
            if self.kinds.get(proxy['capabilityId']) == 'QUERY' and proxy['capabilityId'] != 'structureIntent':
                proxy['request'], proxy['kind'] = args, 'query'
                p, k = self.query_problems({**proxy, 'harness': {}})
            else:
                proxy['request'], proxy['kind'] = args, 'invoke'
                p, k = self.command_problems(proxy, sub)
            problems += [f' tools/call {proxy["capabilityId"]}{x}' for x in p]
            known += [f' tools/call {proxy["capabilityId"]}{x}' for x in k]
        return problems, known

    def query_problems(self, action):
        route, request, op = action.get('route'), action.get('request'), action.get('capabilityId')
        if route not in self.query['routes']:
            return [], []
        if not isinstance(request, dict):
            return [': request must be an object'], []
        if (action.get('harness') or {}).get('intentionalViolation'):
            return [': queries cannot declare intentionalViolation'], []
        fields = self.query['mcpFields'] if route == 'mcp' else self.query['fields']
        spec = self.query['operations'].get(op)
        violations = [('field', k) for k in request if k not in fields]
        scope = request.get('scope', {})
        if not isinstance(scope, dict):
            return ['/scope: must be an object'], []
        allowed = (spec or {}).get('scope', self.query['scopeKeys'])
        violations += [('scope', k) for k in scope if k not in allowed]
        filters = dict(request.get('filters') or {})
        for key in self.query['liftedToFilters']:
            if key in request:
                filters[key] = request[key]
        if spec is not None:
            violations += [('filter', k) for k in filters if k not in spec['filters']]
        required = (spec or {}).get('requires')
        if required and not any(present(request, path) for path in required):
            violations.append(('requires', '|'.join(required)))
        problems, known = [], []
        for kind, key in violations:
            gap = self.query_gap(op, kind, key)
            text = {'field': f'/{key}: not a query envelope field', 'scope': f'/scope/{key}: not a {op} scope key',
                    'filter': f'/{key}: not a {op} filter', 'requires': f': {op} needs one of {key}'}[kind]
            (known if gap else problems).append(f'{text}' + (f' KNOWN_OPEN {gap}' if gap else ' (contracts/request-contracts.json queryEnvelope)'))
        for key in ('asOf', 'knownAt'):
            if key in request and not (isinstance(request[key], str) and re.match(r'^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?Z$', request[key])):
                problems.append(f'/{key}: UTC instant required')
        if 'limit' in request and not (_type_ok(request['limit'], 'integer') and 1 <= request['limit'] <= 200):
            problems.append('/limit: integer 1..200 required')
        return problems, known

    def query_gap(self, op, kind, key):
        for gap in self.known:
            match = gap['match']
            if gap['kind'] != 'query' or op not in match.get('operations', []):
                continue
            if kind == 'requires':
                if match.get('missingIdentifier'):
                    return gap['id']
                continue
            if match.get('anyParameter') or key in match.get({'field': 'fields', 'scope': 'scopeKeys', 'filter': 'filters'}[kind], []):
                return gap['id']
            if kind == 'filter' and key in match.get('fields', []):
                return gap['id']
        return None

    def fixture_problems(self, rel, fixture):
        problems = []
        for name, actor in (fixture.get('actors') or {}).items():
            grant = actor.get('grant') or {}
            kinds = self.grant_kinds(grant)
            composition = grant.get('scopeComposition')
            if composition is not None and composition not in self.data['grantComposition']['values']:
                problems.append(f'{rel} actor {name}: unknown scopeComposition {composition}')
            if len(kinds) >= 2 and composition is None:
                problems.append(f'{rel} actor {name}: grant names {sorted(kinds)} without scopeComposition (contracts/request-contracts.json grantComposition)')
        return problems


_ABSENT = object()


def normalize_slot(value):
    """(value, stated provenance). Provenance embedded in the value moves out; null means the requester gave no value."""
    if value is None:
        return _ABSENT, None
    if isinstance(value, bool):
        return value, None
    if isinstance(value, (int, float)):
        return str(value), None  # decimal strings (plan section 3.1)
    if isinstance(value, list):
        said = set()
        items = []
        for item in value:
            item, s = normalize_slot(item)
            if item is not _ABSENT:
                items.append(item)
            if s:
                said.add(s)
        return items, (said.pop() if len(said) == 1 else None)
    if isinstance(value, dict) and 'provenance' in value and isinstance(value['provenance'], str):
        said = value['provenance']
        rest = {k: v for k, v in value.items() if k != 'provenance'}
        if set(rest) == {'value'}:
            inner, _ = normalize_slot(rest['value'])
            return inner, said
        if set(rest) == {'value', 'type'} and is_ref(rest['value']):
            return {'type': rest['type'], 'id': rest['value']}, said
        return rest, said
    return value, None


def default_provenance(value):
    """CONTEXT for a value picked from the fixture world or an earlier response, USER for a literal the requester typed."""
    if is_ref(value) or is_typed_ref(value):
        return 'CONTEXT'
    if isinstance(value, list) and value and all(is_ref(v) or is_typed_ref(v) for v in value):
        return 'CONTEXT'
    return 'USER'


def present(request, path):
    node = request
    for part in path.split('/'):
        if not isinstance(node, dict) or part not in node:
            return False
        node = node[part]
    return True


def pinned_rejection(sub, action_id):
    for assertion in sub.get('assertions', []):
        source = assertion.get('source') or {}
        if source.get('actionId') != action_id:
            continue
        if source.get('pointer') == '/response/error/code':
            return True
        if source.get('pointer') == '/response/outcome' and assertion.get('expected') in ('REJECTED', 'CONFLICT'):
            return True
    return False


def merge_harness(action, harness):
    """Harness declarations sit right after the request so a reader sees what is never sent beside what is."""
    if not harness:
        return
    if 'harness' not in action:
        items = []
        for key, value in action.items():
            items.append((key, value))
            if key == 'request':
                items.append(('harness', {}))
        action.clear()
        action.update(items)
        action.setdefault('harness', {})
    current = action['harness']
    for key, value in harness.items():
        if key in current and current[key] != value:
            raise ValueError(f"{action.get('id')}: harness.{key} differs")
        current[key] = value


def rename(body, old, new):
    """Rename a key in place, keeping the authored key order."""
    items = [((new if k == old else k), v) for k, v in body.items()]
    body.clear()
    body.update(items)


def iter_actions(actions):
    for action in actions:
        if not isinstance(action, dict):
            continue
        yield action
        for branch in action.get('branches') or []:
            yield from iter_actions(branch.get('actions', []))
        if isinstance(action.get('call'), dict):
            yield from iter_actions([action['call']])


def conform_case(case, root=ROOT, cache=None):
    return Contracts(root).conform_case(case, cache)


def conform_fixture(fixture, root=ROOT):
    return Contracts(root).conform_fixture(fixture)
