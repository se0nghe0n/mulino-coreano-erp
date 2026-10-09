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
        self.assertEqual({'quantity': 'USER', 'dueAt': 'CONTEXT', 'orderLineId': 'CONTEXT', 'digits': 'USER', 'segmentId': 'CONTEXT', 'reason': 'USER'}, body['provenance'])
        self.assertEqual({'value': '20', 'unit': 'BOX'}, body['slots']['quantity'])
        self.assertEqual('0', body['slots']['digits'])
        self.assertNotIn('missing', body['slots'])
        self.assertEqual([], body['subjectRefs'])
        self.assertEqual({'transactionId': 't', 'participantId': 'p', 'barrierId': 'b', 'point': 'AFTER'}, action['harness']['testBarrier'])
        self.assertEqual([], C.case_problems(case([action]))[0])
        again = copy.deepcopy(action)
        C.conform_action(again, 'X01', 's', RC.World(ROOT, None))
        self.assertEqual(action, again, 'conformance is idempotent')

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
        self.assertEqual({'id': A('A20'), 'scope': {'organizationId': A('ORG')}, 'type': 'QuantitySegment'}, read['request'])
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

    def test_instants_must_be_utc(self):
        problems, _ = self.check(query({'scope': {'itemId': A('P')}, 'asOf': '2026-10-07T09:00:00+09:00'}))
        self.assertTrue(any('/asOf: UTC instant required' in p for p in problems))


class GrantComposition(unittest.TestCase):
    def fixture(self, scope, **grant):
        return {'actors': {'reader': {'grant': {'delegatorAlias': 'd', 'actions': [], 'scope': scope, 'validFrom': 'a', 'validUntil': 'b', 'revision': 1, **grant}}}}

    def test_multi_dimension_grant_declares_composition(self):
        two = self.fixture({'itemAliases': ['P'], 'workAliases': ['W']})
        self.assertTrue(C.fixture_problems('f', two))
        C.conform_fixture(two)
        self.assertEqual('PER_DIMENSION', two['actors']['reader']['grant']['scopeComposition'])
        self.assertEqual([], C.fixture_problems('f', two))

    def test_single_dimension_and_empty_lists_need_none(self):
        self.assertEqual([], C.fixture_problems('f', self.fixture({'itemAliases': ['P'], 'segmentAliases': [], 'caseId': 'X'})))
        self.assertTrue(C.fixture_problems('f', self.fixture({'itemAliases': ['P']}, scopeComposition='ANY')))


class RepositoryIsCurrent(unittest.TestCase):
    def test_committed_cases_meet_the_contracts(self):
        spec = importlib.util.spec_from_file_location('check_request_contracts', Path(__file__).with_name('check_request_contracts.py'))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        problems, lines, cases, _ = module.review(ROOT)
        self.assertEqual([], problems)
        self.assertEqual(41, cases)
        self.assertTrue(all(line.startswith('KNOWN_OPEN PG-') for line in lines))


if __name__ == '__main__':
    unittest.main()
