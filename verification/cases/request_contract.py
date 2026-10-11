#!/usr/bin/env python3
"""Request contracts of case invoke/query actions (step2r round 12); never a product or test driver.

The first actual run (docs/execution/s5a-adapter/run-61bd0f8a.json) showed every api command and most queries
rejected for envelope shape before any business rule ran. This module owns the request-side rules of
contracts/request-contracts.json:

- commands: every intent body validates against contracts/intent.schema.json (the product-published envelope)
  with a provenance entry for every slot;
- queries: only the fields, scope keys and filters of the product query envelope (QueryRequests and the
  per-handler guards transcribed in the contract file);
- grants (step2r round 13): one all-of grant per actor (user decision 2026-10-09). A grant keeps only the dimension
  kinds every capability the cases call with that actor presents; grant_problems proves per subcase that every
  read or command expected to be authorized is covered and every action pinned FORBIDDEN is not.

conform_case/conform_fixture rewrite authored requests into those contracts (used by every case generator and
docs/execution/step2r-round12/author_round12.py); case_problems/fixture_problems check them (used by
check_request_contracts.py, which ./verify prepare runs). A request a case sends on purpose to prove rejection
keeps its fields and declares them in action.harness.intentionalViolation. A needed selector the product
contract lacks stays in the request and is reported against a knownOpen product gap instead of a new key.
"""
import base64
import copy
import hashlib
import json
import re
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
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
        self.segments, self.works, self.lots, self.allocations, self.clock, self.actor_source = {}, {}, {}, {}, None, {}
        self.documents, self.prior, self.clock_settings = {}, {}, {}
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
        for name in data.get('actors') or {}:
            self.actor_source[name] = data.get('fixtureId')
        baseline = data.get('baseline') or {}
        for row in baseline.get('segments') or []:
            if isinstance(row, dict) and isinstance(row.get('alias'), str):
                self.segments[row['alias']] = row
        for key in ('works', 'work'):
            rows = baseline.get(key)
            for row in rows if isinstance(rows, list) else [rows] if isinstance(rows, dict) else []:
                if isinstance(row, dict) and isinstance(row.get('alias'), str):
                    self.works[row['alias']] = row
        for row in baseline.get('lots') or []:
            if isinstance(row, dict) and isinstance(row.get('alias'), str):
                self.lots[row['alias']] = row
        for key in ('allocations', 'allocation'):
            rows = baseline.get(key)
            for row in rows if isinstance(rows, list) else [rows] if isinstance(rows, dict) else []:
                if isinstance(row, dict) and isinstance(row.get('alias'), str):
                    self.allocations[row['alias']] = row
        for name, row in (baseline.get('priorEntities') or {}).items():
            if isinstance(row, dict) and (self.aliases.get(name) or {}).get('type') == 'Allocation':
                self.allocations.setdefault(name, {}).update(row)
        clock = (data.get('clock') or {}).get('asOf')
        if isinstance(clock, str):
            self.clock = clock
        self.clock_settings.update({k: v for k, v in (data.get('clock') or {}).items() if isinstance(v, str)})
        for row in baseline.get('documents') or []:
            if isinstance(row, dict) and isinstance(row.get('alias'), str):
                self.documents[row['alias']] = row
        for name, row in (baseline.get('priorEntities') or {}).items():
            if isinstance(row, dict):
                self.prior.setdefault(name, {}).update(row)
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

    # -- facts the product authorizes against (step2r round 13) --
    def org_of(self, name):
        return (self.aliases.get(name) or {}).get('organizationAlias')

    def segment_item(self, name):
        for row in (self.aliases.get(name) or {}, self.segments.get(name) or {}):
            if isinstance(row.get('itemAlias'), str):
                return row['itemAlias']
        for row in (self.aliases.get(name) or {}, self.segments.get(name) or {}):
            if isinstance(row.get('lotAlias'), str):
                return self.lot_item(row['lotAlias'])
        return None

    def segment_place(self, name):
        for row in (self.aliases.get(name) or {}, self.segments.get(name) or {}):
            for key in ('locationAlias', 'placeAlias'):
                if isinstance(row.get(key), str):
                    return row[key]
        return None

    def lot_item(self, name):
        for row in (self.aliases.get(name) or {}, self.lots.get(name) or {}):
            if isinstance(row.get('itemAlias'), str):
                return row['itemAlias']
        return None

    def work_item(self, name):
        """The installer's Work item: the work entry's itemAlias, else the one item of the grants naming the work, else
        the organization's only TradeItem (ScenarioFixtureInstaller.workItem)."""
        row = self.works.get(name) or {}
        if isinstance(row.get('itemAlias'), str):
            return row['itemAlias']
        org = self.org_of(name)
        items = []
        for actor in self.actors.values():
            scope = (actor.get('grant') or {}).get('scope') or {}
            if name in (scope.get('workAliases') or []):
                items += [i for i in scope.get('itemAliases') or [] if self.org_of(i) == org and i not in items]
        if len(items) == 1:
            return items[0]
        only = self.org_items(org)
        return only[0] if len(only) == 1 else None

    def item_segments(self, item):
        names = set(self.segments) | {a for a, v in self.aliases.items() if v.get('type') == 'QuantitySegment'}
        return sorted(n for n in names if self.segment_item(n) == item and (self.segments.get(n) or {}).get('active', True) is not False)


# ---- conformance -----------------------------------------------------------------------------------------------------
class Contracts:
    def __init__(self, root=ROOT):
        self.root = Path(root)
        self._clock = self._case = self._world = None
        self.intent = load_json(self.root, 'contracts/intent.schema.json')
        self.data = load_json(self.root, 'contracts/request-contracts.json')
        self.intent_fields = list(self.intent['properties'])
        routes = self.data['commandRoutes']
        self.intent_routes = set(routes['intentBody'])
        self.addressing = routes['intentWithAddressing']
        self.query = self.data['queryEnvelope']
        self.grants = self.data['grantAuthorization']
        self.dimensions = self.grants['dimensionKinds']
        self.known = self.data['knownOpen']
        self.kinds = {c['id']: c['kind'] for c in load_json(self.root, 'contracts/acceptance-capabilities.json')['capabilities']}

    # -- commands --
    def conform_case(self, case, cache=None):
        cache = {} if cache is None else cache
        for sub in case.get('subcases', []):
            world = World(self.root, sub.get('fixtureRef'), cache)
            prior = []
            self._clock, self._case, self._world = world.clock, case.get('caseId'), world
            for action in iter_actions(sub.get('actions', [])):
                if action.get('kind') == 'control' and (action.get('control') or {}).get('type') == 'clock':
                    params = action['control'].get('parameters') or {}
                    self._clock = next((params[k] for k in ('instant', 'asOf', 'knownAt') if isinstance(params.get(k), str)), self._clock)
                self.conform_action(action, case.get('caseId'), sub.get('id'), world, prior)
                prior.append(action)
        self._clock = self._case = self._world = None
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
        for name, value in list(slots.items()):
            if isinstance(value, str) and value in world.evidence and value not in world.aliases and f'slots/{name}' not in intentional:
                slots[name] = {'$alias': value}  # commandSlots evidenceAliasSlots: a bare evidence alias names that evidence
        self.conform_slots(body, slots, provenance, action, intentional)
        body['provenance'] = {name: provenance[name] for name in slots}
        if intentional:
            harness['intentionalViolation'] = {k: v for k, v in (('fields', sorted(intentional)), ('reason', override['reason']),
                                                                ('unpinnedBecause', override.get('unpinnedBecause'))) if v}
        merge_harness(action, harness)

    # -- command slots (step2r round 13) --
    SLOT_SOURCES = {
        'segmentId': ('segmentId', 'sourceSegmentId', '@QuantitySegment'), 'segmentIds': ('segmentIds', 'sourceSegmentIds'),
        'salesLineId': ('salesLineId', 'orderLineId'), 'transitPlaceId': ('transitPlaceId', 'cargoPlaceId'),
        'workId': ('workId', '@Work'), 'itemId': ('itemId', '@TradeItem'), 'allocationId': ('allocationId', '@Allocation'),
        'handoverId': ('handoverId', '@Handover'), 'restrictionId': ('restrictionId', '@Restriction'),
        'dispositionBasisId': ('dispositionBasisId', '@DispositionBasis'), 'assignmentId': ('assignmentId', '@Obligation'),
        'proposalId': ('proposalId', '@PurchaseProposal'), 'purchaseId': ('purchaseId', '@PurchaseOrder'),
        'shipmentId': ('shipmentId', '@Shipment'), 'procedureId': ('procedureId', '@RegulatoryProcedure'),
        'invoiceId': ('invoiceId', '@Invoice'), 'id': ('id', '@PolicyVersion', '@Grant', '@CapabilityAssignment'),
        'deliveryEndpoint': ('deliveryEndpoint', 'endpoint'), 'packageTerms': ('packageTerms', 'packagingVersion'),
        'workType': ('workType', 'workKind'), 'evidenceRef': ('evidenceRef', '@evidenceRefs'), 'occurredAt': ('occurredAt', '@clock'),
    }

    def slot_source(self, source, body, slots):
        """(value, provenance or None) of one slot source: a slot, a typed subject, the request evidenceRefs or the clock."""
        if source == '@evidenceRefs':
            refs = body.get('evidenceRefs') or []
            return (copy.deepcopy(refs[0]), 'CONTEXT') if refs else (None, None)
        if source == '@clock':
            return (self._clock, 'CONTEXT') if getattr(self, '_clock', None) else (None, None)
        if source.startswith('@'):
            found = [r.get('id') for r in body.get('subjectRefs') or [] if isinstance(r, dict) and r.get('type') == source[1:]]
            return (copy.deepcopy(found[0]), 'CONTEXT') if len(found) == 1 else (None, None)
        return (slots[source], None) if source in slots else (None, None)

    def conform_slots(self, body, slots, provenance, action, intentional):
        """Write command slots in the declaring contract's vocabulary (contracts/request-contracts.json commandSlots)."""
        spec = self.data['commandSlots']
        capability = body.get('capabilityId') or action.get('capabilityId')
        declared = spec['capabilities'].get(capability)
        if declared is None or any(f.startswith('slots/') for f in intentional):
            return
        for rule in spec['renames']:
            if capability in rule['capabilities'] and rule['from'] in slots and rule['to'] not in slots:
                slots[rule['to']] = slots.pop(rule['from'])
                provenance[rule['to']] = provenance.pop(rule['from'], None) or default_provenance(slots[rule['to']])
        self.contract_shapes(body, slots, provenance, capability)
        wait = spec['waitContract']
        if capability in wait['capabilities'] and wait['slot'] not in slots and any(v in slots for v in wait['fields'].values()):
            value, said = {}, set()
            for field, source in wait['fields'].items():
                if source in slots:
                    value[field] = slots.pop(source)
                    said.add(provenance.pop(source, None) or default_provenance(value[field]))
            slots[wait['slot']] = value
            provenance[wait['slot']] = 'USER' if 'USER' in said else 'CONTEXT'
        def fill(name, sources):
            for source in sources:
                value, said = self.slot_source(source, body, slots)
                if value is not None:
                    return value, said or provenance.get(source) or default_provenance(value)
            return None, None
        for rule in (spec['basisSlot'], spec['occurrenceSlot']):
            if capability in rule['capabilities'] and rule['slot'] not in slots:
                value, said = fill(rule['slot'], self.SLOT_SOURCES[rule['slot']])
                if value is not None:
                    slots[rule['slot']], provenance[rule['slot']] = value, said
        if self._case in spec['declaredOnlyCases']['cases']:
            projected, kept = {}, {}
            overrides = spec['sourceOverrides'].get(capability, {})
            for name in declared['allowed']:
                value, said = fill(name, overrides.get(name) or self.SLOT_SOURCES.get(name, (name,)))
                start = spec['startCoordinate']
                if value is None and name == start['slot'] and capability in start['capabilities']:
                    value, said = start['value'], 'CONTEXT'
                if value is not None:
                    projected[name], kept[name] = value, said
            for name in (spec['planSlots'].get(capability) or {}).get('slots', []):
                if name in slots:
                    projected[name], kept[name] = slots[name], provenance.get(name) or default_provenance(slots[name])
            slots.clear()
            slots.update(projected)
            for name in list(provenance):
                provenance.pop(name)
            provenance.update(kept)
            self.fill_required(body, slots, provenance, action, capability, declared)
        else:
            for name in declared['allowed']:
                sources = [s for s in self.SLOT_SOURCES.get(name, ()) if s.startswith('@') and s not in ('@clock', '@evidenceRefs')]
                if name not in slots and sources:
                    value, said = fill(name, sources)
                    if value is not None:
                        slots[name], provenance[name] = value, said
        for qname in spec['quantityWithUnit']['slots']:
            if capability in spec['quantityWithUnit']['capabilities'] and isinstance(slots.get(qname), dict) \
                    and set(slots[qname]) == {'value', 'unit'} and slots.get('unit') in (None, slots[qname]['unit']):
                quantity, said = slots[qname], provenance.get(qname) or 'USER'
                items = []
                for name, value in slots.items():
                    if name == qname:
                        items += [(qname, quantity['value']), ('unit', quantity['unit'])]
                    elif name != 'unit':
                        items.append((name, value))
                slots.clear()
                slots.update(items)  # the unit follows its quantity, so a second pass writes the same order
                provenance[qname], provenance['unit'] = said, provenance.get('unit') or said
        if self._case in spec['declaredOnlyCases']['cases']:
            order = {name: i for i, name in enumerate(declared['allowed'])}
            ordered = sorted(slots.items(), key=lambda kv: order.get(kv[0], len(order)))
            slots.clear()
            slots.update(ordered)

    def contract_shapes(self, body, slots, provenance, capability):
        """Step2r round 13 item 3: slot shapes the capability contract states another way (commandSlots listQuantities,
        contractDrops, intervalPeriod, subjectSlots). Each rewrite keeps the slot order and is a no-op on its own output."""
        spec = self.data['commandSlots']
        rule = spec['listQuantities']
        parts = slots.get(rule['from'])
        if capability in rule['capabilities'] and isinstance(parts, list) and parts and rule['to'] not in slots:
            values, units = [], set()
            for part in parts:
                if isinstance(part, dict) and set(part) == {'value', 'unit'}:
                    values.append(part['value']); units.add(part['unit'])
                else:
                    values = None
                    break
            unit = slots.get(rule['unitSlot'])
            if values and all(isinstance(v, str) for v in values) and len(units) == 1 and unit in (None, *units):
                said = provenance.pop(rule['from'], None) or 'USER'
                items = []
                for name, value in slots.items():
                    if name == rule['from']:
                        items += [(rule['to'], values), (rule['unitSlot'], units.pop())]
                    elif name != rule['unitSlot']:
                        items.append((name, value))
                slots.clear()
                slots.update(items)
                provenance[rule['to']] = said
                provenance[rule['unitSlot']] = provenance.get(rule['unitSlot']) or said
        for rule in spec['contractDrops']['rules']:
            name = rule['slot']
            if capability not in rule['capabilities'] or name not in slots:
                continue
            value, when = slots[name], rule['when']
            if when == 'always':
                drop = True
            elif when == 'zero':
                drop = isinstance(value, dict) and set(value) == {'value', 'unit'} and re.fullmatch(r'0(\.0+)?', str(value['value'])) is not None
            elif when == 'wholeLeaf':
                drop = self.whole_leaf(slots)
            elif when == 'sameSourceAsWork':
                source = lambda v: (v.get('$result') or {}).get('actionId') if isinstance(v, dict) else None
                drop = source(value) is not None and source(value) == source(slots.get('workId'))
            else:
                drop = value == when.get('equals')
            if drop:
                slots.pop(name)
                provenance.pop(name, None)
                if when == 'wholeLeaf' and 'unit' in slots:
                    slots.pop('unit')  # the unit of the restated leaf quantity
                    provenance.pop('unit', None)
        for rule in spec['typedRefRenames']:
            value = slots.get(rule['from'])
            if capability in rule['capabilities'] and rule['to'] not in slots and isinstance(value, dict) \
                    and set(value) == {'type', 'id'} and value['type'] == rule['type']:
                items = [(rule['to'], value['id']) if name == rule['from'] else (name, v) for name, v in slots.items()]
                slots.clear()
                slots.update(items)
                provenance[rule['to']] = provenance.pop(rule['from'], None) or 'CONTEXT'
        rule = spec['intervalPeriod']
        if capability in rule['capabilities'] and slots.get('quantityMode') in rule['modes'] and isinstance(slots.get(rule['from']), dict):
            interval, said = slots.pop(rule['from']), provenance.pop(rule['from'], None)
            slots[rule['to']] = interval  # replaces the cumulative period in place, else appended
            provenance[rule['to']] = said or provenance.get(rule['to']) or 'USER'
        created = spec['createdSubjects']['capabilities'].get(capability)
        if created and isinstance(body.get('subjectRefs'), list):
            body['subjectRefs'] = [r for r in body['subjectRefs'] if not (isinstance(r, dict) and r.get('type') in created)]
        rule = spec['subjectSlots']
        if capability in rule['capabilities'] and rule['slot'] in slots:
            refs = body.setdefault('subjectRefs', [])
            typed = [r for r in refs if isinstance(r, dict) and r.get('type') == rule['type']]
            value = slots[rule['slot']]
            if not typed:
                refs.append({'type': rule['type'], 'id': value})
                typed = refs[-1:]
            if len(typed) == 1 and typed[0].get('id') == value:
                slots.pop(rule['slot'])
                provenance.pop(rule['slot'], None)

    def whole_leaf(self, slots):
        """True when a moveQuantity quantity restates the whole fixture leaf it names (s2-inventory moveQuantity)."""
        world, segment = getattr(self, '_world', None), slots.get('segmentId')
        if world is None or not isinstance(segment, dict) or '$alias' not in segment:
            return False
        row = world.segments.get(segment['$alias']) or {}
        quantity = slots.get('quantity')
        if isinstance(quantity, dict) and set(quantity) == {'value', 'unit'}:
            value, unit = quantity['value'], quantity['unit']
        else:
            value, unit = quantity, slots.get('unit')
        try:
            return row.get('quantity') is not None and Decimal(str(row['quantity'])) == Decimal(str(value)) and unit == row.get('unit')
        except InvalidOperation:
            return False

    def fill_required(self, body, slots, provenance, action, capability, declared):
        """Required slots a shared-slot case did not state, from commandSlots.requiredFill (step2r round 13)."""
        spec = self.data['commandSlots']
        retype = spec['subjectFromWorkItem']
        world = getattr(self, '_world', None)
        if capability in retype['capabilities'] and world is not None:
            refs = body.get('subjectRefs') or []
            works = [r for r in refs if isinstance(r, dict) and r.get('type') == 'Work' and isinstance(r.get('id'), dict) and '$alias' in r['id']]
            if len(works) == 1 and not any(isinstance(r, dict) and r.get('type') == 'TradeItem' for r in refs):
                item = world.work_item(works[0]['id']['$alias'])
                if item:
                    body['subjectRefs'] = [{'type': 'TradeItem', 'id': {'$alias': item}} if r is works[0] else r for r in refs]
        for name, sources in (spec['requiredFill']['capabilities'].get(capability) or {}).items():
            if name in slots or name not in declared['required']:
                continue
            for source in sources:
                value, said = self.fill_source(source, body, slots, action, world)
                if value is not None:
                    slots[name], provenance[name] = value, said
                    break

    def fill_source(self, source, body, slots, action, world):
        if not source.startswith('@'):
            return (copy.deepcopy(slots[source]), provenance_of(slots[source])) if source in slots else (None, None)
        kind, _, arg = source[1:].partition(':')
        refs = [r for r in body.get('subjectRefs') or [] if isinstance(r, dict)]
        evidence = body.get('evidenceRefs') or []
        if kind == 'literal':
            return json.loads(arg), 'USER'
        if kind == 'sha256':
            return hashlib.sha256(arg.encode()).hexdigest(), 'USER'
        if kind == 'evidenceRefs':
            return (copy.deepcopy(evidence[0]), 'CONTEXT') if evidence else (None, None)
        if kind.startswith('clock'):
            if not self._clock:
                return None, None
            at = datetime.fromisoformat(self._clock.replace('Z', '+00:00'))
            if kind.startswith('clock+'):
                at += parse_duration(kind[len('clock+'):])
            return at.astimezone(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'), 'CONTEXT'
        if world is None:
            return None, None
        if kind == 'alias':
            return ({'$alias': arg}, 'CONTEXT') if world.known_alias(arg) else (None, None)
        if kind == 'fact':
            name, _, field = arg.partition('.')
            value = (world.prior.get(name) or {}).get(field)
            return (value, 'CONTEXT') if value is not None else (None, None)
        if kind == 'fixture':
            value = world.clock_settings.get(arg)
            return (value, 'CONTEXT') if value is not None else (None, None)
        if kind == 'document':
            first = evidence[0] if evidence else None
            row = world.documents.get(first.get('$alias')) if isinstance(first, dict) else None
            if not row or not isinstance(row.get('path'), str) or not (self.root / row['path']).is_file():
                return None, None
            content = (self.root / row['path']).read_bytes()
            value = {'path': row['path'], 'mediaType': row.get('mediaType'), 'sha256': hashlib.sha256(content).hexdigest(),
                     'base64': base64.b64encode(content).decode()}.get(arg)
            return (value, 'CONTEXT') if value is not None else (None, None)
        if kind == 'evidenceSubject':
            typed = [r for r in refs if r.get('type') in SUBJECT_KINDS]
            return ({'kind': SUBJECT_KINDS[typed[0]['type']], 'id': copy.deepcopy(typed[0]['id'])}, 'CONTEXT') if len(typed) == 1 else (None, None)
        if kind == 'subjectItem':
            segs = [r['id']['$alias'] for r in refs if r.get('type') == 'QuantitySegment' and isinstance(r.get('id'), dict) and '$alias' in r['id']]
            item = world.segment_item(segs[0]) if len(segs) == 1 else None
            return ({'$alias': item}, 'CONTEXT') if item else (None, None)
        if kind in ('actorItem', 'actorItemScopes'):
            actor = world.actors.get(action.get('actorRef')) or {}
            items = ((actor.get('grant') or {}).get('scope') or {}).get('itemAliases') or world.org_items(actor.get('organizationAlias'))
            if not items:
                return None, None
            if kind == 'actorItem':
                return {'$alias': items[0]}, 'CONTEXT'
            return [{'scopeKind': 'ITEM', 'scopeId': {'$alias': i}} for i in items], 'CONTEXT'
        if kind == 'quantityUnit':
            quantity = slots.get('quantity')
            return (quantity['unit'], 'USER') if isinstance(quantity, dict) and isinstance(quantity.get('unit'), str) else (None, None)
        if kind[:1].isupper():
            found = [r.get('id') for r in refs if r.get('type') == kind]
            return (copy.deepcopy(found[0]), 'CONTEXT') if len(found) == 1 else (None, None)
        raise ValueError(f'unknown requiredFill source {source}')

    def slot_problems(self, body, action, case_id):
        """KNOWN_OPEN lines for slots a capability does not declare or requires (Step 2 vocabulary backlog), FAIL for the
        declared-only cases."""
        spec = self.data['commandSlots']
        capability = body.get('capabilityId') or action.get('capabilityId')
        declared = spec['capabilities'].get(capability)
        slots = body.get('slots') if isinstance(body.get('slots'), dict) else {}
        intentional = set(((action.get('harness') or {}).get('intentionalViolation') or {}).get('fields') or [])
        if declared is None or {f for f in intentional if f.startswith('slots/')}:
            return [], []
        problems, known = [], []
        strict = case_id in spec['declaredOnlyCases']['cases']
        plan = spec['planSlots'].get(capability) or {}
        for name in sorted(set(slots) & set(plan.get('slots', []))):
            known.append(f'/slots/{name}: the plan requires {name} for {capability} but the handler declares no slot ({plan["contractRef"]}) KNOWN_OPEN {plan["gap"]}')
        for name in sorted(set(slots) - set(declared['allowed']) - set(plan.get('slots', []))):
            text = f'/slots/{name}: {capability} declares no slot {name} ({declared["ref"]})'
            (problems.append(text + ' (contracts/request-contracts.json commandSlots)') if strict else known.append(text + ' KNOWN_OPEN S2-SLOT-VOCABULARY'))
        for name in sorted(set(declared['required']) - set(slots)):
            known.append(f'/slots/{name}: {capability} requires {name} ({declared["ref"]}) KNOWN_OPEN S2-SLOT-REQUIRED')
        wait = spec['waitContract']
        if capability in wait['capabilities'] and isinstance(slots.get(wait['slot']), dict):
            for field in sorted(set(wait['fields']) - set(slots[wait['slot']])):
                known.append(f'/slots/{wait["slot"]}/{field}: {capability} requires the complete wait contract ({wait["contractRef"]}) KNOWN_OPEN S2-SLOT-REQUIRED')
        return problems, known

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
                scope['objectType'] = kind
        if op in ('getObject', 'searchObjects') and 'type' in request:
            if 'objectType' in scope and scope['objectType'] != request['type']:
                raise ValueError(f"{action.get('id')}: type differs from scope.objectType")
            scope['objectType'] = request.pop('type')  # a top-level type lifts into filters, which provider handlers refuse
        dedicated = self.query['dedicatedReads'].get(scope.get('objectType')) if op == 'getObject' else None
        if dedicated:
            scope.pop('objectType')
            action['capabilityId'] = op = dedicated['operation']  # a non-noun target has its own read (plan section 3.4)
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

    def family(self, capability):
        return self.grants['capabilityFamilies'].get(capability, self.grants['defaultFamily'])

    def grant_override(self, fixture_id, actor):
        for row in self.grants['overrides']:
            if row['fixtureId'] == fixture_id and row['actor'] == actor:
                return row
        return None

    def grant_usage(self):
        """(fixtureId, actor) -> capabilities the committed cases call with that fixture actor, or through it as a fixture
        delegator, outside actions pinned FORBIDDEN (computed from verification/cases/*/case.json, cached)."""
        if getattr(self, '_usage', None) is None:
            usage, cache = {}, {}
            for path in sorted((self.root / 'verification/cases').glob('*/case.json')):
                case = json.loads(path.read_text())
                for sub in case.get('subcases', []):
                    world = World(self.root, sub.get('fixtureRef'), cache)
                    actions = list(iter_actions(sub.get('actions', [])))
                    pins = pinned_codes(sub, actions)
                    for action in actions:
                        if action.get('kind') not in ('invoke', 'query') or not action.get('actorRef') or pins.get(action.get('id')) == 'FORBIDDEN':
                            continue
                        for capability, request in self.authorized_calls(action):
                            names = {capability}
                            if self.item_less_object(world, capability, request, action):
                                names.add(capability + self.NO_ITEM)
                            if self.cites_evidence(world, capability, request):
                                names.add(self.CITED)
                            who, seen = action['actorRef'], set()
                            while who in world.actors and who not in seen:
                                seen.add(who)
                                usage.setdefault((world.actor_source.get(who), who), set()).update(names)
                                who = (world.actors[who].get('grant') or {}).get('delegatorAlias')
            self._usage = usage
        return self._usage

    NO_ITEM = '#noItem'
    CITED = '#citedEvidence'

    def cites_evidence(self, world, capability, request):
        """True when a command whose handler reads the evidence it cites (grantAuthorization citedEvidenceRead) names
        evidence: request evidenceRefs or a slot naming a fixture evidence alias."""
        if capability not in self.grants['citedEvidenceRead']['capabilities'] or not isinstance(request, dict):
            return False
        if request.get('evidenceRefs'):
            return True
        slots = request.get('slots') if isinstance(request.get('slots'), dict) else {}
        return any(isinstance(v, dict) and v.get('$alias') in world.evidence for v in slots.values())

    def item_less_object(self, world, capability, request, action):
        """True when an OBJECT-family read names a noun whose product row presents no ITEM (a SalesOrder row carries
        TARGET and WORK only), so an ITEM grant would refuse it under the all-of rule."""
        if self.family(capability) != 'OBJECT' or not isinstance(request, dict):
            return False
        presented = self.presentations(world, capability, request, action)
        return isinstance(presented, list) and bool(presented) and all('ITEM' not in p for p in presented)

    def kept_scope_keys(self, fixture_id, actor, grant):
        """Scope keys conform_fixture keeps: metadata, the ITEM keys unless a capability the cases call with this actor (or
        through it as delegator) presents no item, and the keys an override names (contracts/request-contracts.json
        grantAuthorization)."""
        without = set(self.grants['familiesWithoutItem'])
        usage = self.grant_usage().get((fixture_id, actor), set())
        used = usage & set(grant.get('actions') or [])
        item = not any(self.family(cap) in without or cap + self.NO_ITEM in usage for cap in used)
        override = self.grant_override(fixture_id, actor)
        keep = set(self.grants['metadataScopeKeys']) | set((override or {}).get('keep') or [])
        if item and not (override and override.get('dropItem')):
            keep |= {k for k, kind in self.dimensions.items() if kind == 'ITEM'}
        return keep

    def conform_fixture(self, fixture):
        """Step 2 round 13: one all-of grant per actor (user decision 2026-10-09). Before a Work loses the grant that named
        it, its baseline entry gets the item the installer inferred from that grant (ScenarioFixtureInstaller.workItem)."""
        actors = fixture.get('actors') or {}
        baseline = fixture.get('baseline') or {}
        world = World(self.root, None)
        world.aliases, world.actors = fixture.get('aliases') or {}, actors
        for key in ('works', 'work'):
            rows = baseline.get(key)
            for row in rows if isinstance(rows, list) else [rows] if isinstance(rows, dict) else []:
                if isinstance(row, dict) and isinstance(row.get('alias'), str) and 'itemAlias' not in row:
                    item = world.work_item(row['alias'])
                    if item:
                        rebuilt = {}
                        for k, v in row.items():
                            rebuilt[k] = v
                            if k == 'alias':
                                rebuilt['itemAlias'] = item
                        row.clear()
                        row.update(rebuilt)
        for name, actor in actors.items():
            grant = actor.get('grant')
            if not isinstance(grant, dict):
                continue
            grant.pop('scopeComposition', None)
            scope = grant.get('scope')
            if isinstance(scope, dict):
                keep = self.kept_scope_keys(fixture.get('fixtureId'), name, grant)
                for key in [k for k in scope if k not in keep]:
                    scope.pop(key)
            if self.CITED in self.grant_usage().get((fixture.get('fixtureId'), name), set()):
                needed = self.grants['citedEvidenceRead']['requires']
                lists = [actor.get('roleCapabilities'), grant.get('actions')]
                if isinstance(scope, dict) and isinstance(scope.get('capabilityIds'), list):
                    lists.append(scope['capabilityIds'])
                for values in lists:
                    if isinstance(values, list) and needed not in values:
                        values.append(needed)
        return fixture

    # -- checks --
    def case_problems(self, case):
        """(problems, knownOpen) of every invoke/query request of a case."""
        problems, known = [], []
        self._case = case.get('caseId')
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
        self._case = None
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
            p, k = self.slot_problems(body, action, getattr(self, '_case', None))
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
        for kind_value in (scope.get('objectType'), filters.get('type') if op in ('getObject', 'searchObjects') else None):
            if isinstance(kind_value, str) and kind_value not in self.query['objectTypes']:
                violations.append(('objectType', kind_value))
        required = (spec or {}).get('requires')
        if required and not any(present(request, path) for path in required):
            violations.append(('requires', '|'.join(required)))
        problems, known = [], []
        for kind, key in violations:
            gap = self.query_gap(op, kind, key)
            text = {'field': f'/{key}: not a query envelope field', 'scope': f'/scope/{key}: not a {op} scope key',
                    'filter': f'/{key}: not a {op} filter', 'requires': f': {op} needs one of {key}',
                    'objectType': f'/scope/objectType: {key} is not a noun type the product serves'}[kind]
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
            if kind == 'objectType':
                if key in match.get('objectTypes', []):
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
            if 'scopeComposition' in grant:
                problems.append(f'{rel} actor {name}: scopeComposition is retired; a grant is one all-of grant (contracts/request-contracts.json grantAuthorization)')
            keep = self.kept_scope_keys(fixture.get('fixtureId'), name, grant)
            for key in grant.get('scope') or {}:
                if key not in keep:
                    kind = self.dimensions.get(key)
                    why = 'has no product grant scope kind' if key in self.grants['unrepresentedScopeKeys'] else \
                        f'is a {kind} restriction that a capability of this grant does not present' if kind else 'is not a grant scope key'
                    problems.append(f'{rel} actor {name}: grant scope {key} {why}; the all-of grant would refuse its own reads (contracts/request-contracts.json grantAuthorization)')
        return problems

    # -- grant coverage (step2r round 13) --
    def grant_problems(self, case, cache=None):
        """Static all-of check of every authenticated read and command (contracts/request-contracts.json grantAuthorization):
        an action the subcase expects to be authorized must be covered by its actor's grant, and an action pinned to
        FORBIDDEN must stay uncovered. Unresolvable targets (runtime results, unmodelled families), decisions that another
        authority makes and declared nonGrantForbidden actions are not judged."""
        cache = {} if cache is None else cache
        declared = {(r['caseId'], r['subcaseId'], r['actionId']) for r in self.grants['nonGrantForbidden']}
        problems = []
        for sub in case.get('subcases', []):
            world = World(self.root, sub.get('fixtureRef'), cache)
            actions = list(iter_actions(sub.get('actions', [])))
            pins = pinned_codes(sub, actions)
            started = {a['call'].get('id') for a in actions if isinstance(a.get('call'), dict)}
            controls = [i for i, a in enumerate(actions) if a.get('kind') == 'invoke' and a.get('capabilityId') in IDENTITY_CONTROL
                        and pins.get(a.get('id')) is None]
            clock = world.clock
            for i, action in enumerate(actions):
                if action.get('kind') == 'control' and (action.get('control') or {}).get('type') == 'clock':
                    params = action['control'].get('parameters') or {}
                    clock = next((params[k] for k in ('instant', 'asOf', 'knownAt') if isinstance(params.get(k), str)), clock)
                if action.get('kind') not in ('invoke', 'query') or not action.get('actorRef'):
                    continue
                key = (case.get('caseId'), sub.get('id'), action.get('id'))
                # a grant changed at run time before this call (or before the await of a started call) is not judged
                if any(c < i for c in controls) or action.get('id') in started and any(c > i for c in controls):
                    continue
                code = pins.get(action.get('id'))
                wire = action.get('route') == 'wire'
                if code != 'FORBIDDEN' and (wire or code is not None or not expects_effect(sub, actions, i, action)):
                    continue
                if code == 'FORBIDDEN' and key in declared:
                    continue
                calls = self.authorized_calls(action)
                verdicts = [self.covered(world, action['actorRef'], cap, request, action, clock) + (cap,) for cap, request in calls]
                where = f"{case.get('caseId')}/{sub.get('id')}/{action.get('id')}"
                if code == 'FORBIDDEN':
                    if any(v is False for v, _, _ in verdicts) or not verdicts:
                        continue
                    decided = [cap for v, _, cap in verdicts if v is True and cap in self.grants['decisionAuthority']['capabilities']]
                    if all(v is True for v, _, _ in verdicts) and not decided:
                        why = '; '.join(f'{cap}: {w}' for _, w, cap in verdicts)
                        problems.append(f'{where}: pinned FORBIDDEN but the grant of {action["actorRef"]} covers it ({why}); the negative authorization oracle would not hold (contracts/request-contracts.json grantAuthorization)')
                else:
                    for v, w, cap in verdicts:
                        if v is False and not w.startswith('the request names'):
                            problems.append(f'{where}: {cap} is expected to be authorized but the grant of {action["actorRef"]} does not cover it ({w}) under the all-of rule (contracts/request-contracts.json grantAuthorization)')
        return problems

    def authorized_calls(self, action):
        request = action.get('request') if isinstance(action.get('request'), dict) else {}
        if action.get('route') == 'wire':
            return [(proxy['capabilityId'], args) for args, proxy in self.tool_calls(action)]
        if action.get('route') == 'batch':
            return [(op.get('capabilityId'), op) for op in request.get('operations') or [] if isinstance(op, dict)]
        if action.get('route') == 'blob' and isinstance(request.get('businessAction'), dict):
            return [(action.get('capabilityId'), request['businessAction'])]
        return [(action.get('capabilityId'), request)]

    def covered(self, world, actor_name, capability, request, action, clock, seen=()):
        """(True|False|None, reason) for the actor and its fixture delegators."""
        actor = world.actors.get(actor_name)
        if actor is None or actor_name in seen:
            return None, f'{actor_name} is no fixture actor'
        if self.family(capability) == 'NO_HANDLER':
            return None, f'{capability} has no product handler; VERSION_UNSUPPORTED precedes authorization'
        if (action.get('harness') or {}).get('authenticationTest') and not seen:
            return False, 'the credential is a declared authentication variant'
        grant = actor.get('grant') or {}
        if capability not in (actor.get('roleCapabilities') or []) or capability not in (grant.get('actions') or []):
            return False, f'{capability} is not in its roleCapabilities and grant actions'
        ids = (grant.get('scope') or {}).get('capabilityIds')
        if isinstance(ids, list) and capability not in ids:
            return False, f'{capability} is not in its grant scope capabilityIds'
        needed = self.grants['citedEvidenceRead']['requires']
        if self.cites_evidence(world, capability, request) and not all(
                needed in (values or []) for values in (actor.get('roleCapabilities'), grant.get('actions'))
                + ((ids,) if isinstance(ids, list) else ())):
            return False, f'{capability} reads the evidence it cites and {needed} is not in its roleCapabilities and grant actions'
        if clock and not (str(grant.get('validFrom', '')) <= clock < str(grant.get('validUntil', '~'))):
            return False, f'the grant is not valid at the product clock {clock}'
        revoked = (grant.get('scope') or {}).get('revokedAt')
        if clock and isinstance(revoked, str) and revoked <= clock:
            return False, f'the grant was revoked at {revoked}'
        org = actor.get('organizationAlias')
        scope_org = (request.get('scope') or {}).get('organizationId') if isinstance(request.get('scope'), dict) else None
        if isinstance(scope_org, dict) and scope_org.get('$alias') and scope_org['$alias'] != org:
            return False, f'the request names organization {scope_org["$alias"]}'
        foreign = sorted(a for a in aliases_in(request) if world.org_of(a) not in (None, org) and (world.aliases.get(a) or {}).get('type') != 'Organization')
        if foreign:
            return False, f'the request names {foreign} of another organization'
        presented = self.presentations(world, capability, request, action)
        result, why = self.match_grant(world, org, grant, presented, self.family(capability))
        if result is False:
            return False, why
        delegator = grant.get('delegatorAlias')
        if delegator and delegator != actor_name and delegator in world.actors and world.actors[delegator].get('organizationAlias') == org:
            parent, pwhy = self.covered(world, delegator, capability, request, action, clock, seen + (actor_name,))
            if parent is False:
                return False, f'its delegator {delegator}: {pwhy}'
            if parent is None:
                result = None if result else result
        return result, why

    def match_grant(self, world, org, grant, presented, family):
        dims = {}
        for key, value in (grant.get('scope') or {}).items():
            kind = self.dimensions.get(key)
            if kind and value not in (None, [], ''):
                dims.setdefault(kind, set()).update(value if isinstance(value, list) else [value])
        if presented is None:
            return True, 'any grant with the capability passes'
        if presented == 'unknown':
            return None, f'{family} targets are not modelled'
        verdicts = []
        for p in presented:
            if any(world.org_of(v) not in (None, org) for values, _ in p.values() for v in values):
                verdicts.append((False, 'the target belongs to another organization'))
                continue
            if 'ORGANIZATION' in p:
                verdicts.append((not dims, 'the product presents only the organization' + ('' if not dims else f'; the grant names {sorted(dims)}')))
                continue
            verdict, why = True, f'grant kinds {sorted(dims) or "none"} match {sorted(p)}'
            for kind, allowed in dims.items():
                if kind not in p:
                    verdict, why = False, f'the grant names {kind} but {family} presents only {sorted(p)}'
                    break
                values, unknown = p[kind]
                if unknown or not values:
                    verdict, why = (None if verdict else verdict), f'{kind} value unresolved'
                    continue
                if not values <= allowed:
                    verdict, why = False, f'{kind} {sorted(values - allowed)} outside the grant {sorted(allowed)}'
                    break
            verdicts.append((verdict, why))
        for wanted in (True, None, False):
            for verdict, why in verdicts:
                if verdict is wanted:
                    return verdict, why
        return None, 'no presentation'

    def presentations(self, world, capability, request, action):
        """Alternative target maps {kind: (alias values, unresolved)} the product presents for this call."""
        family = self.family(capability)
        spec = self.grants['families'][family]['presents']
        if spec is None or spec == 'unknown':
            return spec
        refs = TargetRefs(world, request, capability)
        if spec == 'inventory':
            item = refs.first('item')
            if item is None:
                return 'unknown'
            rows = [{'TARGET': ({s}, False), 'ITEM': ({item}, False), 'PLACE': refs.place_of(s)} for s in world.item_segments(item)
                    if refs.segment_matches(s)]
            return rows + [{'TARGET': ({item}, False), 'ITEM': ({item}, False)}]
        if spec == 'object':
            return refs.object_presentation()
        out = []
        for template in spec:
            out.append({kind: refs.values(source) for kind, source in template.items()})
        return out

IDENTITY_CONTROL = ('createGrant', 'revokeGrant', 'assignCapability', 'revokeCapability')


def pinned_codes(sub, actions):
    """/response/error/code pinned per action id, an await's pin applying to its start call."""
    codes = {}
    for assertion in sub.get('assertions', []):
        source = assertion.get('source') or {}
        pointer = str(source.get('pointer', ''))
        if assertion.get('op') == 'equals' and isinstance(assertion.get('expected'), str) and (
                pointer == '/response/error/code' or pointer.startswith('/response/body') and pointer.endswith('/error/code')):
            codes[source.get('actionId')] = assertion['expected']
        if source.get('pointer') == '/response/outcome' and assertion.get('op') == 'equals' and assertion.get('expected') not in ('APPLIED', None):
            codes.setdefault(source.get('actionId'), 'OUTCOME:' + str(assertion.get('expected')))
    starts = {a['call'].get('id'): a.get('id') for a in actions if isinstance(a.get('call'), dict)}
    for action in actions:
        if action.get('kind') == 'await':
            for call, holder in starts.items():
                if holder == action.get('awaitActionId') and action.get('id') in codes:
                    codes.setdefault(call, codes[action['id']])
    return codes


def expects_effect(sub, actions, at, action):
    """A query without a pinned rejection is read by the case; a command applies when its result is read or pinned APPLIED
    (ContractValidator.expectedToApply)."""
    if action.get('kind') == 'query':
        return True
    aid = action.get('id')
    for assertion in sub.get('assertions', []):
        source = assertion.get('source') or {}
        pointer = str(source.get('pointer', ''))
        if source.get('actionId') == aid:
            if pointer == '/response/outcome':
                if assertion.get('op') == 'equals' and assertion.get('expected') == 'APPLIED':
                    return True
            elif pointer.startswith('/response/') and not pointer.startswith('/response/error') and assertion.get('op') not in ('absent', 'notEquals'):
                return True
        if aid in result_ids(assertion):
            return True
    return any(aid in result_ids(later.get('request')) for later in actions[at + 1:])


def aliases_in(node):
    out = set()
    if isinstance(node, dict):
        if isinstance(node.get('$alias'), str) and len(node) == 1:
            out.add(node['$alias'])
        for value in node.values():
            out |= aliases_in(value)
    elif isinstance(node, list):
        for value in node:
            out |= aliases_in(value)
    return out


def result_ids(node):
    out = set()
    if isinstance(node, dict):
        ref = node.get('$result')
        if isinstance(ref, dict) and isinstance(ref.get('actionId'), str):
            out.add(ref['actionId'])
        for value in node.values():
            out |= result_ids(value)
    elif isinstance(node, list):
        for value in node:
            out |= result_ids(value)
    return out


class TargetRefs:
    """Alias values a request names, classified by the fixture type of each alias (runtime results stay unresolved)."""
    SLOT_KINDS = {'itemId': 'item', 'workId': 'work', 'targetWorkId': 'work', 'segmentId': 'segment', 'sourceSegmentId': 'segment',
                  'segmentIds': 'segment', 'destinationId': 'destination', 'placeId': 'place', 'transitPlaceId': 'transit',
                  'cargoPlaceId': 'transit', 'lotId': 'lot', 'allocationId': 'allocation'}
    TYPE_KINDS = {'TradeItem': 'item', 'Work': 'work', 'QuantitySegment': 'segment', 'Place': 'place', 'ManufacturingLot': 'lot',
                  'ManufacturerLot': 'lot', 'Allocation': 'allocation'}

    def __init__(self, world, request, capability):
        self.world, self.capability = world, capability
        self.named = {k: set() for k in ('item', 'work', 'segment', 'destination', 'place', 'transit', 'lot', 'allocation', 'id')}
        self.unresolved = set()
        scope = request.get('scope') if isinstance(request.get('scope'), dict) else {}
        slots = request.get('slots') if isinstance(request.get('slots'), dict) else {}
        for key, value in list(scope.items()) + [(k, request[k]) for k in ('id', 'workId', 'itemId', 'lotId', 'placeId') + QUERY_ID_KEYS if k in request]:
            kind = {'itemId': 'item', 'workId': 'work', 'lotId': 'lot', 'placeId': 'place', 'id': 'id', **{k: 'id' for k in QUERY_ID_KEYS}}.get(key)
            if kind:
                self.add(kind, value)
        for key, value in slots.items():
            if key in self.SLOT_KINDS:
                self.add(self.SLOT_KINDS[key], value)
        for ref in request.get('subjectRefs') or []:
            if isinstance(ref, dict) and ref.get('type') in self.TYPE_KINDS:
                self.add(self.TYPE_KINDS[ref['type']], ref.get('id'))
        for name in list(self.named['id']):
            kind = self.TYPE_KINDS.get((world.aliases.get(name) or {}).get('type'))
            if kind:
                self.named[kind].add(name)
        for name in list(self.named['allocation']):
            row = world.allocations.get(name) or world.aliases.get(name) or {}
            if isinstance(row.get('segmentAlias'), str):
                self.named['segment'].add(row['segmentAlias'])
            else:
                self.unresolved.add('segment')

    def add(self, kind, value):
        if isinstance(value, list):
            for v in value:
                self.add(kind, v)
            return
        if isinstance(value, dict) and set(value) == {'type', 'id'}:
            value = value['id']
        if isinstance(value, dict) and isinstance(value.get('value'), (dict, str)) and '$alias' not in value:
            value = value['value']
        if isinstance(value, dict) and isinstance(value.get('$alias'), str):
            self.named[kind].add(value['$alias'])
        elif value is not None:
            self.unresolved.add(kind)

    def first(self, kind):
        values = self.values(kind)[0]
        return sorted(values)[0] if len(values) == 1 else None

    def place_of(self, segment):
        place = self.world.segment_place(segment)
        return ({place}, False) if place else (set(), True)

    def segment_matches(self, segment):
        places, lots = self.named['place'], self.named['lot']
        if places and self.world.segment_place(segment) not in places:
            return False
        if lots:
            lot = (self.world.aliases.get(segment) or {}).get('lotAlias') or (self.world.segments.get(segment) or {}).get('lotAlias')
            return lot in lots
        return True

    def values(self, source):
        """(alias set, unresolved) for a family source: item, workItem, work, segment, segmentPlace(+destination|+transit), id, ?."""
        w = self.world
        if source == '?':
            return set(), True
        if source == 'organization':
            return set(), False
        parts = source.split('+')
        out, unknown = set(), False
        for part in parts:
            if part == 'item':
                items = set(self.named['item'])
                items |= {w.segment_item(s) for s in self.named['segment']}
                items |= {w.lot_item(l) for l in self.named['lot']}
                items |= {w.work_item(x) for x in self.named['work']}
                unknown |= None in items or not (items - {None}) and bool(self.unresolved & {'item', 'segment', 'lot', 'work', 'id'})
                out |= items - {None}
                if not out:
                    unknown = True
            elif part == 'workItem':
                items = {w.work_item(x) for x in self.named['work']}
                unknown |= None in items or 'work' in self.unresolved or not items
                out |= items - {None}
            elif part in ('work', 'segment', 'destination', 'transit', 'place', 'lot', 'id'):
                out |= self.named[part]
                unknown |= part in self.unresolved or not self.named[part]
            elif part == 'segmentPlace':
                places = {w.segment_place(s) for s in self.named['segment']}
                unknown |= None in places or 'segment' in self.unresolved or not places
                out |= places - {None}
        return out, unknown

    def object_presentation(self):
        w = self.world
        ids = self.named['id'] or self.named['item']
        if len(ids) != 1 or self.unresolved & {'id'}:
            return 'unknown'
        name = next(iter(ids))
        kind = (w.aliases.get(name) or {}).get('type')
        one = ({name}, False)
        def known(value):
            return ({value}, False) if value else (set(), True)
        if kind == 'TradeItem':
            return [{'TARGET': one, 'ITEM': one}]
        if kind in ('ManufacturingLot', 'ManufacturerLot'):
            return [{'TARGET': one, 'ITEM': known(w.lot_item(name))}]
        if kind == 'QuantitySegment':
            return [{'TARGET': one, 'ITEM': known(w.segment_item(name)), 'PLACE': known(w.segment_place(name))}]
        if kind == 'Place':
            return [{'TARGET': one, 'PLACE': one}]
        if kind == 'Work':
            return [{'TARGET': one, 'WORK': one, 'ITEM': known(w.work_item(name))}]
        if kind in ('Customer', 'Supplier'):
            return [{'TARGET': one}]
        if kind == 'SalesOrder':
            return [{'TARGET': one, 'WORK': (set(), True)}]  # Orders carry workId, no itemId (SalesQueries.scopes)
        return 'unknown'


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


def provenance_of(value):
    return default_provenance(value)


def parse_duration(text):
    match = re.fullmatch(r'P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?)?', text)
    if not match or not any(match.groups()):
        raise ValueError(f'unsupported duration {text}')
    days, hours, minutes = (int(g or 0) for g in match.groups())
    return timedelta(days=days, hours=hours, minutes=minutes)


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
