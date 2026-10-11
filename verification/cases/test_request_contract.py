"""Mutants for request_contract.py (step2r round 12): every non-contract request shape must be reported or conformed.
Run: python3 -m unittest discover -s verification/cases -p 'test_request_contract.py' -v
"""
import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('request_contract', Path(__file__).with_name('request_contract.py'))
RC = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RC)
C = RC.Contracts(ROOT)
A = lambda name: {'$alias': name}
R = lambda action, pointer: {'$result': {'actionId': action, 'pointer': pointer}}


def command(**extra):
    body = {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': 'moveQuantity',
            'subjectRefs': [{'type': 'QuantitySegment', 'id': A('A20')}],
            'slots': {'quantity': {'value': '20', 'unit': 'BOX'}, 'destinationId': A('W2')},
            'provenance': {'quantity': 'USER', 'destinationId': 'CONTEXT'},
            'expectedRevision': R('read', '/response/data/revision'), 'commandIdempotencyKey': 'k-1', 'evidenceRefs': [A('DOC')]}
    body.update(extra)
    return body


def invoke(request, route='api', cap='moveQuantity', aid='attempt', **kw):
    return {'id': aid, 'kind': 'invoke', 'actorRef': 'reader', 'route': route, 'capabilityId': cap, 'request': request,
            'evidenceRefs': [aid + ':x'], **kw}


def query(request, cap='getInventory', route='api', aid='read'):
    return {'id': aid, 'kind': 'query', 'actorRef': 'reader', 'route': route, 'capabilityId': cap, 'request': request,
            'evidenceRefs': [aid + ':x']}


def case(actions, assertions=None, fixture=None):
    return {'caseId': 'X01', 'subcases': [{'id': 's', 'fixtureRef': fixture, 'actions': actions, 'assertions': assertions or []}]}


class CommandContract(unittest.TestCase):
    def problems(self, action, assertions=None):
        return C.case_problems(case([action], assertions))[0]

    def test_conforming_command_passes(self):
        self.assertEqual([], self.problems(invoke(command())))

    def test_missing_provenance_and_extra_fields_fail(self):
        body = command(asOf='2026-10-07T09:00:00Z', requesterContext='verified-auth-only')
        del body['provenance']
        found = self.problems(invoke(body))
        self.assertTrue(any('/provenance: required' in p for p in found), found)
        self.assertTrue(any('/asOf: not allowed' in p for p in found) and any('/requesterContext: not allowed' in p for p in found), found)

    def test_embedded_slot_provenance_fails(self):
        body = command()
        body['slots']['quantity'] = {'value': '20', 'unit': 'BOX', 'provenance': 'USER'}
        self.assertTrue(any('/slots/quantity' in p for p in self.problems(invoke(body))))

    def test_provenance_must_name_every_slot(self):
        body = command(provenance={'quantity': 'USER'})
        self.assertTrue(any('differ from slots' in p for p in self.problems(invoke(body))))

    def test_untyped_subject_and_non_uuid_evidence_fail(self):
        found = self.problems(invoke(command(subjectRefs=[{'type': 'TradeItem', 'name': 'x'}], evidenceRefs=['need'])))
        self.assertTrue(any('/subjectRefs/0/id: required' in p for p in found), found)
        self.assertTrue(any("'need' is not a UUID" in p for p in found), found)

    def test_execution_needs_idempotency_key_and_matching_capability(self):
        body = command(capabilityId='splitQuantity')
        del body['commandIdempotencyKey']
        found = self.problems(invoke(body))
        self.assertTrue(any('commandIdempotencyKey: required' in p for p in found) and any('differs from the action capability' in p for p in found), found)

    def test_list_slot_is_a_known_product_gap(self):
        body = command()
        body['slots']['segmentIds'] = [A('A20'), A('A60')]
        body['provenance']['segmentIds'] = 'CONTEXT'
        problems, known = C.case_problems(case([invoke(body)]))
        self.assertEqual([], problems)
        self.assertTrue(any('PG-INTENT-ARRAY-SLOT' in k for k in known))

    def test_intentional_violation_must_match_and_be_pinned(self):
        action = invoke(command(organizationId=A('ORG-B')), harness={'intentionalViolation': {'fields': ['organizationId'], 'reason': 'forged org'}})
        self.assertTrue(any('pinned rejection' in p for p in self.problems(action)))
        pinned = [{'id': 'code', 'source': {'actionId': 'attempt', 'pointer': '/response/error/code'}, 'expected': 'FORBIDDEN'}]
        self.assertEqual([], self.problems(action, pinned))
        wrong = copy.deepcopy(action)
        wrong['harness']['intentionalViolation']['fields'] = ['actorId']
        self.assertTrue(any('declares' in p for p in self.problems(wrong, pinned)))

    def test_batch_and_wire_tool_arguments_are_intents(self):
        bad = command()
        del bad['provenance']
        found = self.problems(invoke({'atomic': True, 'changesetId': 'c', 'operations': [bad]}, route='batch'))
        self.assertTrue(any('/operations/0/provenance: required' in p for p in found), found)
        wire = {'id': 'w', 'kind': 'invoke', 'actorRef': 'reader', 'route': 'wire', 'protocolOperation': 'tools/call', 'evidenceRefs': ['w:x'],
                'request': {'transport': 'streamable-http', 'body': {'jsonrpc': '2.0', 'id': 'w', 'method': 'tools/call',
                                                                    'params': {'name': 'moveQuantity', 'arguments': bad}}}}
        self.assertTrue(any('tools/call moveQuantity/provenance: required' in p for p in self.problems(wire)))


class Conformance(unittest.TestCase):
    def test_command_conforms_with_stated_and_rule_provenance(self):
        old = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': 'reserveQuantity', 'scope': {'organizationId': A('ORG')},
               'asOf': 'x', 'knownAt': 'y', 'requesterContext': 'verified-auth-only', 'valueProvenance': 'USER',
               'slots': {'quantity': {'value': '20', 'unit': 'BOX', 'provenance': 'USER'}, 'dueAt': {'value': '2026-10-09T09:00:00Z', 'provenance': 'CONTEXT'},
                         'orderLineId': A('LINE'), 'missing': None, 'digits': 0},
               'expectedRevision': 1, 'commandIdempotencyKey': 'k', 'segmentId': A('A20'), 'reason': 'typed',
               'testTransactionId': 't', 'testParticipantId': 'p', 'testBarrierId': 'b', 'testBarrierPoint': 'AFTER'}
        action = invoke(old, cap='reserveQuantity')
        C.conform_action(action, 'X01', 's', RC.World(ROOT, None))
        body = action['request']
        self.assertNotIn('scope', body)
        # step2r round 13: the reserve line is the declared salesLineId, the quantity a decimal string beside its unit slot.
        self.assertEqual({'quantity': 'USER', 'unit': 'USER', 'dueAt': 'CONTEXT', 'salesLineId': 'CONTEXT', 'digits': 'USER', 'segmentId': 'CONTEXT', 'reason': 'USER'}, body['provenance'])
        self.assertEqual(('20', 'BOX'), (body['slots']['quantity'], body['slots']['unit']))
        self.assertEqual('0', body['slots']['digits'])
        self.assertNotIn('missing', body['slots'])
        self.assertEqual([], body['subjectRefs'])
        self.assertEqual({'transactionId': 't', 'participantId': 'p', 'barrierId': 'b', 'point': 'AFTER'}, action['harness']['testBarrier'])
        self.assertEqual([], C.case_problems(case([action]))[0])
        again = copy.deepcopy(action)
        C.conform_action(again, 'X01', 's', RC.World(ROOT, None))
        self.assertEqual(action, again, 'conformance is idempotent')

    def test_declared_only_cases_send_the_capability_slots(self):
        shared = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': 'moveQuantity',
                  'subjectRefs': [{'type': 'QuantitySegment', 'id': A('A20')}], 'evidenceRefs': [A('DOC')],
                  'slots': {'quantity': {'value': '20', 'unit': 'BOX'}, 'destinationId': A('W2'), 'orderLineId': A('LINE'),
                            'sourceSegmentId': A('A20'), 'reason': 'r'}, 'expectedRevision': 1, 'commandIdempotencyKey': 'k'}
        sub = {'id': 's', 'actions': [invoke(shared)], 'assertions': []}
        C.conform_case({'caseId': 'C3', 'subcases': [sub]})
        body = sub['actions'][0]['request']
        self.assertEqual({'segmentId': A('A20'), 'destinationId': A('W2'), 'evidenceRef': A('DOC')}, body['slots'])
        self.assertEqual({'segmentId': 'CONTEXT', 'destinationId': 'CONTEXT', 'evidenceRef': 'CONTEXT'}, body['provenance'])
        stray = copy.deepcopy(sub)
        stray['actions'][0]['request']['slots']['reason'] = 'r'
        stray['actions'][0]['request']['provenance']['reason'] = 'USER'
        problems, known = C.case_problems({'caseId': 'C3', 'subcases': [stray]})
        self.assertTrue(any('moveQuantity declares no slot reason' in p for p in problems), problems)
        self.assertTrue(any('requires occurredAt' in k and 'S2-SLOT-REQUIRED' in k for k in known), known)
        problems, known = C.case_problems({'caseId': 'T99', 'subcases': [stray]})
        self.assertEqual([], [p for p in problems if 'declares no slot' in p])
        self.assertTrue(any('S2-SLOT-VOCABULARY' in k for k in known), known)

    def test_declared_only_cases_complete_required_slots_for_the_authorization_probe(self):
        # round 13 run-7b6c69b4: a probe missing required slots is TYPE_INVALID before authorization (plan section 3.3).
        hold = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': 'placeHold',
                'subjectRefs': [{'type': 'QuantitySegment', 'id': A('A20')}], 'evidenceRefs': [A('DOC')],
                'slots': {'segmentId': A('A20'), 'action': 'SELL', 'reason': 'r', 'validFrom': '2026-10-07T09:00:00Z',
                          'validUntil': '2026-10-08T00:00:00Z', 'quantity': '20', 'unit': 'BOX'},
                'expectedRevision': 1, 'commandIdempotencyKey': 'k'}
        draft = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': 'createDraft',
                 'subjectRefs': [{'type': 'Work', 'id': A('WORK')}], 'slots': {'ownerId': A('warehouse'), 'supervisorId': A('supervisor')},
                 'expectedRevision': 1, 'commandIdempotencyKey': 'k2'}
        sub = {'id': 'api-placeHold', 'fixtureRef': 'verification/cases/C3/fixture-placeHold.json',
               'actions': [invoke(hold, cap='placeHold'), invoke(draft, cap='createDraft', aid='draft')], 'assertions': []}
        C.conform_case({'caseId': 'C3', 'subcases': [sub]})
        slots = sub['actions'][0]['request']['slots']
        self.assertEqual(('QC', A('DOC'), '0', A('WORK'), '2026-10-07T10:00:00Z'),
                         (slots['category'], slots['evidenceId'], slots['startQuantity'], slots['workId'], slots['nextCheckAt']))
        self.assertEqual('USER', sub['actions'][0]['request']['provenance']['category'])
        self.assertEqual('CONTEXT', sub['actions'][0]['request']['provenance']['nextCheckAt'])
        self.assertEqual([{'type': 'TradeItem', 'id': A('P')}], sub['actions'][1]['request']['subjectRefs'])
        again = copy.deepcopy(sub)
        C.conform_case({'caseId': 'C3', 'subcases': [again]})
        self.assertEqual(sub, again, 'filling is idempotent')
        problems, known = C.case_problems({'caseId': 'C3', 'subcases': [sub]})
        self.assertFalse(any('placeHold requires' in k for k in known), known)

    def test_contract_shapes_rewrite_slots_the_capability_contract_states_another_way(self):
        # step2r round 13 item 3: s2-inventory splitQuantity/moveQuantity, plan sections 3.3 and 5.1, placeHold category.
        world = RC.World(ROOT, None)
        world.segments = {'Q20': {'alias': 'Q20', 'quantity': '20', 'unit': 'BOX'}}
        def run(cap, slots, **extra):
            body = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': cap, 'subjectRefs': [], 'slots': slots,
                    'expectedRevision': 1, 'commandIdempotencyKey': 'k', **extra}
            sub = {'id': 's', 'actions': [invoke(body, cap=cap)], 'assertions': []}
            C._world = world
            try:
                C.conform_action(sub['actions'][0], 'T99', 's', world)
            finally:
                C._world = None
            again = copy.deepcopy(sub['actions'][0])
            C._world = world
            try:
                C.conform_action(again, 'T99', 's', world)
            finally:
                C._world = None
            self.assertEqual(sub['actions'][0], again, 'contract shapes are idempotent')
            return sub['actions'][0]['request']
        split = run('splitQuantity', {'segmentId': A('Q20'), 'children': [{'value': '12', 'unit': 'BOX'}, {'value': '8', 'unit': 'BOX'}],
                                      'loss': {'value': '0', 'unit': 'BOX'}, 'allocationTransfer': [{'childIndex': 0}], 'reason': 'r'},
                    evidenceRefs=[A('DOC')])
        self.assertEqual(['12', '8'], split['slots']['quantities'])
        self.assertEqual('BOX', split['slots']['unit'])
        self.assertFalse({'children', 'loss', 'allocationTransfer', 'reason'} & set(split['slots']))
        named = run('splitQuantity', {'segmentId': A('Q20'), 'children': [{'alias': 'A', 'quantity': '12', 'unit': 'BOX'}, {'alias': 'B', 'quantity': '8', 'unit': 'BOX'}]})
        self.assertIn('children', named['slots'], 'children read back by alias stay until their response pointer is rewritten')
        lossy = run('splitQuantity', {'segmentId': A('Q20'), 'children': [{'value': '12', 'unit': 'BOX'}], 'loss': {'value': '2', 'unit': 'BOX'}})
        self.assertIn('loss', lossy['slots'])
        whole = run('moveQuantity', {'segmentId': A('Q20'), 'destinationId': A('W2'), 'quantity': '20', 'unit': 'BOX'})
        self.assertFalse({'quantity', 'unit'} & set(whole['slots']))
        part = run('moveQuantity', {'segmentId': A('Q20'), 'destinationId': A('W2'), 'quantity': '5', 'unit': 'BOX'})
        self.assertEqual(('5', 'BOX'), (part['slots']['quantity'], part['slots']['unit']))
        draft = run('createDraft', {'itemId': A('P'), 'quantityMode': 'THROUGHOUT', 'period': {'start': 'a'}, 'interval': {'start': 'b'},
                                    'lotIdentification': 'MISSING', 'ownerId': A('o'), 'supervisorId': A('s')})
        self.assertEqual({'start': 'b'}, draft['slots']['period'])
        self.assertFalse({'interval', 'lotIdentification', 'itemId'} & set(draft['slots']))
        self.assertEqual([{'type': 'TradeItem', 'id': A('P')}], draft['subjectRefs'])
        hold = run('placeHold', {'segmentId': A('Q20'), 'kind': 'QC'})
        self.assertEqual('QC', hold['slots']['category'])
        handover = run('proposeHandover', {'workId': A('WORK'), 'toOwnerId': A('B')})
        self.assertEqual(A('B'), handover['slots']['recipientId'])
        grant = run('createGrant', {'actorId': A('reader')}, subjectRefs=[{'type': 'Grant', 'id': A('GRANT')}, {'type': 'Agent', 'id': A('reader')}])
        self.assertEqual([{'type': 'Agent', 'id': A('reader')}], grant['subjectRefs'], 'only the created noun is dropped')

    def test_wait_slots_form_the_wait_contract(self):
        flat = {'intentKind': 'COMMAND', 'definitionVersion': 'd', 'capabilityId': 'waitWork', 'subjectRefs': [],
                'slots': {'workId': A('W'), 'reason': 'r', 'targetId': A('PO'), 'resumePredicate': {'kind': 'x'}, 'verifierId': A('V'),
                          'nextCheckAt': '2026-10-08T00:00:00Z', 'overdueAction': 'o'}, 'expectedRevision': 1, 'commandIdempotencyKey': 'k'}
        sub = {'id': 's', 'actions': [invoke(flat, cap='waitWork')], 'assertions': []}
        C.conform_case({'caseId': 'T99', 'subcases': [sub]})
        slots = sub['actions'][0]['request']['slots']
        self.assertEqual({'workId', 'wait'}, set(slots))
        self.assertEqual({'reason', 'target', 'resumePredicate', 'verifier', 'nextCheckAt', 'overdueAction'}, set(slots['wait']))

    def test_foreign_organization_needs_a_decision(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, 'f.json').write_text(json.dumps({'actors': {'reader': {'organizationAlias': 'ORG-A'}}}))
            world = RC.World(tmp, 'f.json')
            with self.assertRaises(ValueError):
                C.conform_action(invoke(command(organizationId=A('ORG-B'))), 'X01', 's', world)

    def test_query_conforms_to_product_envelope(self):
        read = query({'objectId': A('A20'), 'scope': {'organizationId': A('ORG'), 'caseId': 'X01', 'includeDescendants': True}}, cap='getObject')
        inventory = query({'scope': {'organizationId': A('ORG'), 'itemId': A('P'), 'workId': A('W'), 'includeDescendants': True},
                           'intentKind': 'QUERY', 'capabilityId': 'getInventory', 'include': ['x'], 'environmentId': 'env'})
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, 'f.json').write_text(json.dumps({'aliases': {'A20': {'type': 'QuantitySegment'}}}))
            world = RC.World(tmp, 'f.json')
            for action in (read, inventory):
                C.conform_action(action, 'X01', 's', world)
        self.assertEqual({'id': A('A20'), 'scope': {'organizationId': A('ORG'), 'objectType': 'QuantitySegment'}}, read['request'])
        self.assertEqual({'scope': {'organizationId': A('ORG'), 'itemId': A('P')}}, inventory['request'])
        self.assertEqual({'environmentId': 'env'}, inventory['harness'])
        self.assertEqual([], C.case_problems(case([read, inventory]))[0])


class QueryContract(unittest.TestCase):
    def check(self, action):
        return C.case_problems(case([action]))

    def test_unknown_scope_key_and_field_fail(self):
        problems, _ = self.check(query({'scope': {'itemId': A('P'), 'includeDescendants': True}, 'objectId': A('P')}))
        self.assertTrue(any('/scope/includeDescendants' in p for p in problems) and any('/objectId' in p for p in problems), problems)

    def test_operation_filters_and_scope(self):
        problems, _ = self.check(query({'scope': {'itemId': A('P'), 'workId': A('W')}}))
        self.assertTrue(any('/scope/workId: not a getInventory scope key' in p for p in problems), problems)
        problems, _ = self.check(query({'scope': {'workId': A('W')}, 'itemId': A('P')}, cap='searchOperationalIssues'))
        self.assertTrue(any('/itemId: not a searchOperationalIssues filter' in p for p in problems), problems)

    def test_needed_selector_is_a_known_product_gap(self):
        problems, known = self.check(query({'scope': {'itemId': A('P')}, 'action': 'SELL', 'customerId': A('C')}))
        self.assertEqual([], problems)
        self.assertTrue(all('PG-INVENTORY-ACTION-ELIGIBILITY' in k for k in known) and len(known) == 2, known)

    def test_reads_name_their_identifier(self):
        problems, known = self.check(query({'scope': {'organizationId': A('ORG')}}, cap='getWork'))
        self.assertTrue(any('getWork needs one of id|workId' in p for p in problems), problems)
        problems, known = self.check(query({'scope': {'organizationId': A('ORG')}}))
        self.assertEqual([], problems)
        self.assertTrue(any('PG-ORGANIZATION-SNAPSHOT' in k for k in known), known)

    def test_non_noun_targets_use_their_own_read_or_a_recorded_gap(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, 'f.json').write_text(json.dumps({'aliases': {'WORK': {'type': 'Work'}, 'ALLOC': {'type': 'Allocation'}}}))
            world = RC.World(tmp, 'f.json')
            work, alloc = query({'objectId': A('WORK')}, cap='getObject'), query({'objectId': A('ALLOC')}, cap='getObject', aid='alloc')
            for action in (work, alloc):
                C.conform_action(action, 'X01', 's', world)
        self.assertEqual(('getWork', {'id': A('WORK')}), (work['capabilityId'], work['request']))
        problems, known = C.case_problems(case([work, alloc]))
        self.assertEqual([], problems)
        self.assertTrue(any('PG-TARGET-REVISION-READ' in k for k in known), known)
        problems, _ = C.case_problems(case([query({'id': A('X'), 'scope': {'objectType': 'MadeUp'}}, cap='getObject')]))
        self.assertTrue(any('MadeUp is not a noun type' in p for p in problems), problems)

    def test_conformance_derives_identifiers_from_the_case(self):
        work = invoke(command(capabilityId='createWork'), cap='createWork', aid='work')
        read = query({'scope': {'organizationId': A('ORG'), 'itemId': A('P')}}, cap='getAssessment', aid='assess')
        noun = query({'scope': {'organizationId': A('ORG'), 'itemId': A('P')}}, cap='getObject', aid='noun')
        C.conform_case(case([work, read, noun]))
        self.assertEqual({'$result': {'actionId': 'work', 'pointer': '/response/workId'}}, read['request']['workId'])
        self.assertEqual(A('P'), noun['request']['id'])

    def test_instants_must_be_utc(self):
        problems, _ = self.check(query({'scope': {'itemId': A('P')}, 'asOf': '2026-10-07T09:00:00+09:00'}))
        self.assertTrue(any('/asOf: UTC instant required' in p for p in problems))


class GrantAuthorization(unittest.TestCase):
    """Step 2 round 13: one all-of grant per actor, proven against the subcase's reads and commands."""
    def world(self, scope, actions, roles=None, **fixture):
        actor = {'organizationAlias': 'ORG', 'roleCapabilities': roles or actions,
                 'grant': {'delegatorAlias': 'root', 'actions': actions, 'scope': scope, 'validFrom': '2026-10-01T00:00:00Z',
                           'validUntil': '2026-10-31T00:00:00Z', 'revision': 1}}
        f = {'fixtureId': 'unit', 'clock': {'asOf': '2026-10-07T09:00:00Z'}, 'actors': {'reader': actor},
             'aliases': {'ORG': {'type': 'Organization'}, 'P': {'type': 'TradeItem', 'organizationAlias': 'ORG'},
                         'Q': {'type': 'TradeItem', 'organizationAlias': 'ORG'},
                         'S': {'type': 'QuantitySegment', 'organizationAlias': 'ORG', 'itemAlias': 'P', 'locationAlias': 'W'},
                         'W': {'type': 'Place', 'organizationAlias': 'ORG', 'kind': 'INTERNAL_STORAGE'},
                         'WORK': {'type': 'Work', 'organizationAlias': 'ORG'},
                         'X': {'type': 'TradeItem', 'organizationAlias': 'ORG-B'}},
             'baseline': {'work': {'alias': 'WORK', 'ownerAlias': 'reader'}}, **fixture}
        return f

    def problems(self, fixture, actions, assertions=()):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'fixture.json'
            path.write_text(json.dumps(fixture))
            sub = {'id': 's', 'fixtureRef': str(path), 'actions': actions, 'assertions': list(assertions)}
            return C.grant_problems({'caseId': 'X', 'subcases': [sub]})

    def read(self, scope=None, aid='read', cap='getInventory', **request):
        return query({'scope': scope or {'organizationId': A('ORG'), 'itemId': A('P')}, **request}, cap=cap, aid=aid)

    def test_a_dimension_the_read_does_not_present_refuses_it(self):
        f = self.world({'itemAliases': ['P'], 'workAliases': ['WORK']}, ['getInventory'])
        self.assertTrue(any('names WORK' in p for p in self.problems(f, [self.read()])))
        C.conform_fixture(f)
        self.assertEqual({'itemAliases': ['P']}, f['actors']['reader']['grant']['scope'])
        self.assertEqual([], self.problems(f, [self.read()]))

    def test_item_restriction_is_kept_and_checked_by_value(self):
        f = self.world({'itemAliases': ['P']}, ['getInventory'])
        problems = self.problems(f, [self.read({'organizationId': A('ORG'), 'itemId': A('Q')})])
        self.assertTrue(any("ITEM ['Q'] outside the grant" in p for p in problems), problems)

    def test_a_pinned_forbidden_that_the_grant_covers_is_reported(self):
        f = self.world({'itemAliases': ['P']}, ['getInventory'])
        pin = {'id': 'code', 'op': 'equals', 'source': {'actionId': 'read', 'pointer': '/response/error/code'}, 'expected': 'FORBIDDEN'}
        self.assertTrue(any('pinned FORBIDDEN' in p for p in self.problems(f, [self.read()], [pin])))
        other = self.read({'organizationId': A('ORG'), 'itemId': A('Q')})
        self.assertEqual([], self.problems(f, [other], [pin]))
        foreign = self.read({'organizationId': A('ORG'), 'itemId': A('X')})
        self.assertEqual([], self.problems(self.world({}, ['getInventory']), [foreign], [pin]))

    def test_capability_outside_the_grant_or_revoked_grant_is_uncovered(self):
        f = self.world({}, ['getInventory'], roles=['getInventory', 'getWork'])
        problems = self.problems(f, [self.read(cap='getWork', workId=A('WORK'))])
        self.assertTrue(any('not in its roleCapabilities and grant actions' in p for p in problems), problems)
        f = self.world({'revokedAt': '2026-10-07T08:00:00Z'}, ['getInventory'])
        self.assertTrue(any('revoked' in p for p in self.problems(f, [self.read()])))

    def test_item_restriction_drops_when_a_called_capability_presents_no_item(self):
        f = self.world({'itemAliases': ['P']}, ['getInventory', 'getDefinition'])
        self.assertEqual({'itemAliases'}, C.kept_scope_keys('unit', 'reader', f['actors']['reader']['grant']) & {'itemAliases'})
        self.assertTrue(any('names ITEM but TARGET_ID' in p for p in self.problems(f, [self.read(cap='getDefinition', id=A('P'))])))

    def test_item_restriction_drops_when_a_read_names_a_noun_without_item(self):
        # step2r round 13 run-9d213f5a: getObject of a SalesOrder (Orders row: TARGET, WORK) under an ITEM grant was FORBIDDEN.
        f = self.world({'itemAliases': ['P']}, ['getObject'])
        f['aliases']['SALE'] = {'type': 'SalesOrder', 'organizationAlias': 'ORG'}
        read = query({'id': A('SALE'), 'scope': {'objectType': 'SalesOrder'}}, cap='getObject')
        self.assertTrue(any('names ITEM' in p for p in self.problems(f, [read])), 'the ITEM grant refuses the order read')
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'f.json'
            path.write_text(json.dumps(f))
            world = RC.World(ROOT, str(path))
            self.assertTrue(C.item_less_object(world, 'getObject', read['request'], read))
            item = query({'id': A('P')}, cap='getObject')
            self.assertFalse(C.item_less_object(world, 'getObject', item['request'], item))

    def test_retired_composition_and_unrepresented_keys_fail_the_fixture(self):
        f = self.world({'itemAliases': ['P'], 'customerAliases': ['C']}, ['getInventory'])
        f['actors']['reader']['grant']['scopeComposition'] = 'PER_DIMENSION'
        problems = C.fixture_problems('f', f)
        self.assertTrue(any('scopeComposition is retired' in p for p in problems))
        self.assertTrue(any('customerAliases has no product grant scope kind' in p for p in problems))
        C.conform_fixture(f)
        self.assertEqual([], C.fixture_problems('f', f))

    def test_work_item_survives_the_dropped_work_dimension(self):
        f = self.world({'itemAliases': ['P'], 'workAliases': ['WORK']}, ['getInventory'])
        f['aliases']['Q2'] = {'type': 'TradeItem', 'organizationAlias': 'ORG'}
        C.conform_fixture(f)
        self.assertEqual('P', f['baseline']['work']['itemAlias'])


class RepositoryIsCurrent(unittest.TestCase):
    def test_committed_cases_meet_the_contracts(self):
        spec = importlib.util.spec_from_file_location('check_request_contracts', Path(__file__).with_name('check_request_contracts.py'))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        problems, lines, cases, _ = module.review(ROOT)
        self.assertEqual([], problems)
        self.assertEqual(41, cases)
        self.assertTrue(all(line.startswith(('KNOWN_OPEN PG-', 'KNOWN_OPEN S2-')) for line in lines))


if __name__ == '__main__':
    unittest.main()
