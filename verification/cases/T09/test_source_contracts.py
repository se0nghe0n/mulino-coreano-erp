"""Deterministic source mapping checks; does not execute actual scenarios."""
import base64
import copy
import hashlib
import importlib.util
import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

from source_contracts import ROOT, CASES, document_and_event, receipt_chain, repair_inputs, SOURCE_ACTOR, SOURCE_CAPABILITIES, OBSERVATION_ACTOR, OBSERVATION_CAPABILITIES

spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
rc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rc)

ASSERTION_HASHES = {
    'T09': '54ac825d05828af51dfdf6680180e890e5d07149f109e2e61da0e8587cef9c7f',
    'T11': '104f61abad63eabbda98396e6c7940121c290be5d0c5b9b4c289e508f3934959',
    'T12': '63889cb521ae5a00c830afdd8ab5f4057886c4f6e7215424407e4961cc77f6a3'}


class SourceContracts(unittest.TestCase):
    def setUp(self):
        self.binding = {k: f'00000000-0000-4000-8000-{i:012d}' for i, k in enumerate(
            ('itemId', 'placeId', 'workId', 'rangeRootId', 'ownerId', 'supervisorId', 'lotId'), 1)}
        self.binding['nextCheckAt'] = '2026-10-08T09:00:00Z'
        self.binding['policyVersion'] = 'SYNTHETIC-policy-v1'

    def test_269_assertions_and_all_original_actions_preserved(self):
        counts = []
        for case_id in CASES:
            path = f'verification/cases/{case_id}/case.json'
            current_text = (ROOT / path).read_text()
            original_text = subprocess.check_output(['git', 'show', '66e4d068:' + path], cwd=ROOT).decode()
            current = json.loads(current_text)
            original = json.loads(original_text)
            def raw_assertions(text):
                blocks = []
                for match in re.finditer(r'"assertions":\s*', text):
                    start = match.end()
                    _, length = json.JSONDecoder().raw_decode(text[start:])
                    blocks.append(text[start:start + length])
                return blocks
            self.assertEqual(raw_assertions(original_text), raw_assertions(current_text), case_id)
            assertions = [s['assertions'] for s in current['subcases']]
            self.assertEqual(ASSERTION_HASHES[case_id], hashlib.sha256(
                json.dumps(assertions, ensure_ascii=False, sort_keys=True).encode()).hexdigest())
            counts.append(sum(map(len, assertions)))
            self.assertEqual([s['id'] for s in original['subcases']], [s['id'] for s in current['subcases']])
            for before, after in zip(original['subcases'], current['subcases']):
                self.assertEqual(before['assertions'], after['assertions'])
                self.assertTrue({a['id'] for a in before['actions']} <= {a['id'] for a in after['actions']})
        self.assertEqual([94, 110, 65], counts)

    def test_repair_fixed_point_and_public_record_inputs(self):
        contract = rc.Contracts(ROOT)
        allowed = json.loads((ROOT / 'contracts/capability-slots.json').read_text())['capabilities']
        for case_id in CASES:
            case = json.loads((ROOT / f'verification/cases/{case_id}/case.json').read_text())
            fixtures = {}
            def fixture(ref):
                return fixtures.setdefault(ref, json.loads((ROOT / ref).read_text()))
            repaired = repair_inputs(copy.deepcopy(case), fixture)
            self.assertEqual(case, repaired)
            for sub in case['subcases']:
                for action in sub['actions']:
                    if action.get('capabilityId') in ('recordActivity', 'attachEvidence', 'createSalesOrder'):
                        self.assertLessEqual(set(action['request']['slots']), set(allowed[action['capabilityId']]['slots']))
                        problems, known = contract.command_problems(action, sub)
                        self.assertEqual(([], []), (problems, known), (case_id, sub['id'], action['id']))

    def test_receipt_graph_has_real_commands_and_original_identity(self):
        graph = receipt_chain('receipt', 'A', self.binding, '60', '2026-10-07T01:00:00Z')
        self.assertEqual(['attachEvidence', 'recordActivity', 'receiveProvisional',
                          'matchSourceIdentity', 'linkCanonicalOccurrence', 'confirmReceipt'],
                         [a['capabilityId'] for a in graph])
        resolved = rc.Placeholders()(graph)
        original = base64.b64decode(resolved[0]['request']['slots']['contentBase64'])
        self.assertEqual(hashlib.sha256(original).hexdigest(), resolved[0]['request']['slots']['expectedHash'])
        self.assertEqual(json.loads(original), json.loads(resolved[1]['request']['slots']['payload']))
        self.assertEqual(self.binding['workId'], json.loads(original)['workId'])
        self.assertEqual('60', graph[2]['request']['slots']['quantity'])
        self.assertEqual('60', graph[3]['request']['slots']['quantity'])
        contract = rc.Contracts(ROOT)
        for action in graph:
            problems, known = contract.command_problems(action, {'id': 'bound-receipt', 'assertions': []})
            self.assertEqual(([], []), (problems, known), (action['id'], problems, known))
        self.assertFalse(any('VERIFIED' in a['request']['slots'].get('valueState', '') for a in graph))

    def test_stock_goals_select_the_actual_published_template_without_changing_target(self):
        definition = json.loads((ROOT / 'verification/actual/scenarios/installer-conventions.json').read_text())['definition']
        original = json.loads(subprocess.check_output([
            'git', 'show', '2998782e:verification/cases/T09/case.json'], cwd=ROOT))
        before = {(s['id'], a['id']): a for s in original['subcases'] for a in s['actions']}
        current = json.loads((ROOT / 'verification/cases/T09/case.json').read_text())
        selected = []
        for sub in current['subcases']:
            for action in sub['actions']:
                slots = action.get('request', {}).get('slots', {})
                mode = slots.get('quantityMode')
                if action.get('capabilityId') != 'createWork' or mode not in ('STATE_AT', 'EXISTS_IN', 'THROUGHOUT'):
                    continue
                templates = [g for g in definition['goals'] if g['endpoint'] == slots['endpoint'] and g['quantityMode'] == mode]
                self.assertEqual(1, len(templates))
                self.assertEqual([templates[0]['name']], slots['conditions'])
                self.assertEqual(before[(sub['id'], action['id'])]['request']['slots']['quantity'], slots['quantity'])
                if mode != 'STATE_AT':
                    self.assertEqual('complete-initial-and-final-ledger', slots['observationPolicy'])
                selected.append((sub['id'], action['id']))
        self.assertEqual(6, len(selected))

    def test_no_document_no_canonical_and_invalid_bindings(self):
        graph = document_and_event('claim', 'A', {'kind': 'WORK', 'id': self.binding['workId']},
            'RECEIPT_CLAIM', {'quantity': '100'}, 'synthetic-warehouse', 'no-evidence', '1',
            '2026-10-07T01:00:00Z', '100', 'BOX', document=False)
        self.assertEqual(1, len(graph))
        self.assertNotIn('documentId', graph[0]['request']['slots'])
        self.assertEqual('KNOWN', graph[0]['request']['slots']['valueState'])
        for quantity in ('0', '-1'):
            with self.assertRaises(ValueError):
                receipt_chain('r', 'A', self.binding, quantity, '2026-10-07T01:00:00Z')
        with self.assertRaises(ValueError):
            receipt_chain('r', 'A', {**self.binding, 'workId': 'fake'}, '1', '2026-10-07T01:00:00Z')
        with self.assertRaises(ValueError):
            receipt_chain('r', 'A', self.binding, '1', '2026-10-07T01:00:00Z',
                          transit_segment_id=self.binding['rangeRootId'],
                          receiving_custodian_id=self.binding['ownerId'])

    def test_negative_claims_keep_lack_scope_conflict_and_time(self):
        case = json.loads((ROOT / 'verification/cases/T12/case.json').read_text())
        subs = {s['id']: s for s in case['subcases']}
        no = next(a for a in subs['no-receipt-evidence']['actions'] if a['id'] == 'claim')
        self.assertNotIn('documentId', no['request']['slots'])
        self.assertEqual([], no['request']['evidenceRefs'])
        scope = next(a for a in subs['insufficient-source-scope']['actions'] if a['id'] == 'claim')
        self.assertEqual({'$alias': 'PORT'}, scope['request']['slots']['payload']['$encode']['source']['reportedScope']['placeId'])
        claims = [a for a in subs['conflicting-claims']['actions'] if a.get('capabilityId') == 'recordActivity']
        self.assertEqual(['100', '98'], [a['request']['slots']['quantity'] for a in claims])
        self.assertEqual(['synthetic-warehouse', 'synthetic-carrier'], [a['request']['slots']['sourceNamespace'] for a in claims])
        self.assertEqual({'$result': {'actionId': 'claim', 'pointer': '/response/id'}}, claims[1]['request']['slots']['payload']['$encode']['source']['samePhysicalReceiptCandidate'])
        for sub in (subs[x] for x in ('no-receipt-evidence', 'insufficient-source-scope', 'conflicting-claims')):
            self.assertFalse(any(a.get('capabilityId') in ('confirmReceipt', 'linkCanonicalOccurrence') for a in sub['actions']))

    def test_all_ten_receipts_are_connected_and_keep_original_physical_inputs(self):
        total = 0
        contract = rc.Contracts(ROOT)
        for case_id in CASES:
            case = json.loads((ROOT / f'verification/cases/{case_id}/case.json').read_text())
            frontier = json.loads((ROOT / f'verification/cases/{case_id}/source-contract-frontier.json').read_text())
            for entry in frontier['unresolvedReceiptInputs']:
                total += 1
                sub = next(s for s in case['subcases'] if s['id'] == entry['subcase'])
                aid = entry['action']
                graph = [a for a in sub['actions'] if a['id'] == aid or a['id'].startswith(aid + '-')]
                self.assertEqual({'$result': {'actionId': aid + '-provisional',
                                             'pointer': '/response/revision'}},
                                 graph[-1]['request']['expectedRevision'])
                # No current business assertion supplies a source fact or input.
                expected = ['attachEvidence', 'recordActivity', 'receiveProvisional']
                if sub['id'] != 'receipt-missing-lot':
                    expected += ['matchSourceIdentity', 'linkCanonicalOccurrence']
                expected += ['confirmReceipt']
                self.assertEqual(expected, [a['capabilityId'] for a in graph])
                original = entry['originalPhysicalInput']
                doc, event, observation = [a['request']['slots'] for a in graph[:3]]
                source = event['payload']['$encode']['source']
                self.assertEqual(source, doc['contentBase64']['$encode']['source'])
                self.assertEqual(source, doc['expectedHash']['$encode']['source'])
                self.assertEqual('PHYSICAL_RECEIPT', event['kind'])
                self.assertEqual(original['quantity']['value'], source['quantity'])
                self.assertEqual(original['quantity']['unit'], source['unit'])
                for key in ('occurredAt', 'placeId', 'lotId'):
                    if key in original:
                        self.assertEqual(original[key], source[key])
                        self.assertEqual(original[key], observation[key])
                for action in graph:
                    problems, known = contract.command_problems(action, sub)
                    self.assertEqual([], problems, (case_id, sub['id'], action['id']))
                    if sub['id'] == 'receipt-missing-lot' and action['id'] == aid:
                        self.assertEqual({'canonicalOccurrenceId', 'lotId'}, {k.split(':')[0].removeprefix('/slots/') for k in known})
                    else:
                        self.assertEqual([], known, (case_id, sub['id'], action['id']))
                self.assertEqual({'$result': {'actionId': aid + '-provisional', 'pointer': '/response/effects/receiptId'}}, graph[-1]['request']['slots']['receiptId'])
                if sub['id'] == 'receipt-missing-lot':
                    self.assertNotIn('lotId', observation)
                    self.assertNotIn('lotId', graph[-1]['request']['slots'])
                    self.assertNotIn('canonicalOccurrenceId', graph[-1]['request']['slots'])
                if 'segmentId' in original:
                    self.assertEqual(original['segmentId'], observation['transitSegmentId'])
                self.assertTrue(all(a['actorRef'] == SOURCE_ACTOR for a in graph[:-1]))
        self.assertEqual(10, total)

    def test_new_source_identity_does_not_broaden_business_grants_or_erase_facts(self):
        for case_id in CASES:
            case = json.loads((ROOT / f'verification/cases/{case_id}/case.json').read_text())
            for sub in case['subcases']:
                ref = sub['fixtureRef']
                current = json.loads((ROOT / ref).read_text())
                before = json.loads(subprocess.check_output(['git', 'show', 'ad80e1bc:' + ref], cwd=ROOT))
                for name, actor in before['actors'].items():
                    self.assertEqual(actor, current['actors'][name], (ref, name))
                for key in ('segments', 'works', 'documents', 'lots', 'purchaseOrders'):
                    self.assertEqual(before['baseline'].get(key), current['baseline'].get(key), (ref, key))
                if SOURCE_ACTOR in current['actors']:
                    actor = current['actors'][SOURCE_ACTOR]
                    self.assertEqual(SOURCE_CAPABILITIES, actor['roleCapabilities'])
                    self.assertEqual(SOURCE_CAPABILITIES, actor['grant']['actions'])
                    self.assertEqual({'organizationAlias': 'ORG', 'itemAliases': ['P']}, actor['grant']['scope'])
                    self.assertNotIn('confirmReceipt', actor['grant']['actions'])
                    profile = next(p for p in current['baseline']['sourceProfiles'] if p['namespace'] == 'synthetic-receipt')
                    self.assertEqual(SOURCE_ACTOR, profile['reconciliationOwnerAlias'])
                    self.assertEqual('SYNTHETIC-policy-v1', profile['policyVersion'])

    def test_only_eight_authorized_source_roles_and_eight_auxiliary_actions_change(self):
        before = json.loads(subprocess.check_output(['git', 'show', '76fe11e2:verification/cases/T09/case.json'], cwd=ROOT))
        current = json.loads((ROOT / 'verification/cases/T09/case.json').read_text())
        roles = additions = 0
        normalized = copy.deepcopy(current)
        for old, new, restored in zip(before['subcases'], current['subcases'], normalized['subcases']):
            # The subsequent receipt revision repair has its own strict check.
            original_actions = {a['id']: a for a in old['actions']}
            for action in restored['actions']:
                if action.get('capabilityId') == 'confirmReceipt':
                    self.assertEqual({'$result': {'actionId': action['id'] + '-provisional',
                                                 'pointer': '/response/revision'}},
                                     action['request']['expectedRevision'])
                    action['request']['expectedRevision'] = original_actions[action['id']]['request']['expectedRevision']
            if old['id'] not in ('throughout-gap', 'throughout-complete'):
                self.assertEqual(old, restored)
                continue
            originals = {a['id']: a for a in old['actions']}
            new_actions = {a['id']: a for a in new['actions']}
            expected_extra = {prefix + suffix for prefix in ('initial-observation', 'final-observation')
                              for suffix in ('-match', '-canonical')}
            self.assertEqual(expected_extra, set(new_actions) - set(originals))
            additions += len(expected_extra)
            self.assertEqual(list(originals), [a['id'] for a in new['actions'] if a['id'] in originals])
            restored['actions'] = [a for a in restored['actions'] if a['id'] in originals]
            for action in restored['actions']:
                aid = action['id']
                if aid in ('initial-observation-document', 'initial-observation',
                           'final-observation-document', 'final-observation'):
                    self.assertEqual('A', originals[aid]['actorRef'])
                    self.assertEqual(OBSERVATION_ACTOR, action['actorRef'])
                    action['actorRef'] = 'A'
                    roles += 1
                if aid in ('initial-observation', 'final-observation'):
                    # Only the canonical validity coordinates differ from the report time.
                    action['request']['slots']['effectiveFrom'] = originals[aid]['request']['slots']['effectiveFrom']
                    action['request']['slots'].pop('effectiveUntil', None)
                    action['request']['provenance'].pop('effectiveUntil', None)
            self.assertEqual(old, restored)
        self.assertEqual((8, 8), (roles, additions))
        self.assertEqual(before, normalized)
        feature = (ROOT / 'verification/cases/T09/scenario.feature').read_text()
        original_feature = subprocess.check_output(['git', 'show', '76fe11e2:verification/cases/T09/scenario.feature'], cwd=ROOT).decode()
        lines = []
        replaced = removed = 0
        for line in feature.splitlines(keepends=True):
            if '"observationSource" 역할이' in line:
                if '-match" 행동' in line or '-canonical" 행동' in line:
                    removed += 1
                    continue
                line = line.replace('"observationSource" 역할이', '"A" 역할이')
                replaced += 1
            lines.append(line)
        self.assertEqual((8, 8), (replaced, removed))
        self.assertEqual(original_feature, ''.join(lines))
        print('source roles: exactly 8 substitutions; exactly 8 auxiliary actions; every other feature line unchanged')

    def test_observation_reviewer_is_additive_and_item_scoped(self):
        for suffix in ('gap', 'complete'):
            ref = f'verification/cases/T09/fixtures/throughout-{suffix}.json'
            before = json.loads(subprocess.check_output(['git', 'show', '76fe11e2:' + ref], cwd=ROOT))
            current = json.loads((ROOT / ref).read_text())
            actor = current['actors'].pop(OBSERVATION_ACTOR)
            self.assertEqual(OBSERVATION_CAPABILITIES, actor['roleCapabilities'])
            self.assertEqual(OBSERVATION_CAPABILITIES, actor['grant']['actions'])
            self.assertEqual(OBSERVATION_ACTOR, actor['grant']['delegatorAlias'])
            self.assertEqual({'organizationAlias': 'ORG', 'itemAliases': ['P']}, actor['grant']['scope'])
            self.assertEqual({'type': 'Human'}, current['aliases'].pop(OBSERVATION_ACTOR))
            profile = next(p for p in current['baseline']['sourceProfiles'] if p['namespace'] == 'synthetic-warehouse')
            self.assertEqual(OBSERVATION_ACTOR, profile['reconciliationOwnerAlias'])
            self.assertEqual('SYNTHETIC-policy-v1', profile['policyVersion'])
            previous_profile = next(p for p in before['baseline']['sourceProfiles'] if p['namespace'] == 'synthetic-warehouse')
            profile.clear()
            profile.update(previous_profile)
            self.assertEqual(before, current)

    def test_four_observation_graphs_keep_original_bytes_and_explicit_time_bounds(self):
        from datetime import datetime, timedelta
        before = json.loads(subprocess.check_output(['git', 'show', '76fe11e2:verification/cases/T09/case.json'], cwd=ROOT))
        current = json.loads((ROOT / 'verification/cases/T09/case.json').read_text())
        contract = rc.Contracts(ROOT)
        graphs = 0
        for sub in current['subcases']:
            if sub['id'] not in ('throughout-gap', 'throughout-complete'):
                continue
            old = next(s for s in before['subcases'] if s['id'] == sub['id'])
            for prefix in ('initial-observation', 'final-observation'):
                ids = [prefix + '-document', prefix, prefix + '-match', prefix + '-canonical']
                graph = [next(a for a in sub['actions'] if a['id'] == aid) for aid in ids]
                self.assertEqual(['attachEvidence', 'recordActivity', 'matchSourceIdentity', 'linkCanonicalOccurrence'],
                                 [a['capabilityId'] for a in graph])
                self.assertEqual([sub['actions'].index(graph[0]) + i for i in range(4)],
                                 [sub['actions'].index(a) for a in graph])
                document, event, match, canonical = [a['request']['slots'] for a in graph]
                original = next(a for a in old['actions'] if a['id'] == prefix)['request']['slots']['payload']['$encode']['source']
                self.assertEqual(original, event['payload']['$encode']['source'])
                self.assertEqual(original, document['contentBase64']['$encode']['source'])
                self.assertEqual(original, document['expectedHash']['$encode']['source'])
                self.assertEqual(original['coverageStart'], event['effectiveFrom'])
                self.assertEqual(event['effectiveFrom'], match['effectiveFrom'])
                self.assertEqual(original['segmentId'], match['physicalScopeId'])
                self.assertEqual(original['quantity'], match['quantity'])
                self.assertEqual(event['quantity'], match['quantity'])
                self.assertEqual(event['unit'], match['unit'])
                self.assertEqual(':'.join(event[k] for k in ('sourceNamespace', 'externalEventId', 'sourceVersion')), match['sourceIdentity'])
                self.assertEqual({'$result': {'actionId': prefix, 'pointer': '/response/claimId'}}, match['claimId'])
                self.assertEqual({'$result': {'actionId': prefix + '-document', 'pointer': '/response/id'}}, match['basisDocumentId'])
                self.assertEqual({'$result': {'actionId': prefix + '-match', 'pointer': '/response/id'}}, canonical['reconciliationId'])
                if original['coverageStart'] == original['coverageEnd']:
                    self.assertNotIn('effectiveUntil', event)
                    self.assertEqual('throughout-gap', sub['id'])
                else:
                    until = datetime.fromisoformat(event['effectiveUntil'].replace('Z', '+00:00'))
                    end = datetime.fromisoformat(original['coverageEnd'].replace('Z', '+00:00'))
                    self.assertEqual(timedelta(microseconds=1), until - end)
                resolved = rc.Placeholders()(graph)
                original_bytes = base64.b64decode(resolved[0]['request']['slots']['contentBase64'])
                self.assertEqual(hashlib.sha256(original_bytes).hexdigest(), resolved[0]['request']['slots']['expectedHash'])
                self.assertEqual(json.loads(original_bytes), json.loads(resolved[1]['request']['slots']['payload']))
                for action in graph:
                    problems, known = contract.command_problems(action, sub)
                    self.assertEqual(([], []), (problems, known), (sub['id'], action['id']))
                self.assertEqual('KNOWN', event['valueState'])
                graphs += 1
        self.assertEqual(4, graphs)
        print('4 real observation graphs: original byte/hash/identity bindings and PostgreSQL microsecond bounds preserved')

    def test_actual_receipt_matcher_rejects_source_mutations(self):
        """Execute the product's matcher source, not a Python facsimile of it."""
        source = (ROOT / 'backend/src/main/java/com/mulino/application/evidence/EvidenceReconciliation.java').read_text()
        method = source.split('  private static boolean receiptPayloadMatches', 1)[1].split('  private static boolean sameQuantity', 1)[0]
        method = '  private static boolean receiptPayloadMatches' + method
        graph = receipt_chain('r', 'A', self.binding, '60', '2026-10-07T01:00:00Z')
        payload = rc.Placeholders()(graph[1]['request']['slots']['payload'])
        jars = []
        for name in ('jackson-databind', 'jackson-core', 'jackson-annotations'):
            choices = sorted((Path.home() / '.m2/repository/com/fasterxml/jackson/core' / name).glob('*/*.jar'))
            self.assertTrue(choices, name)
            jars.append(str(choices[-1]))
        classpath = ':'.join(jars)
        body = '''import java.time.*; import java.math.*; import java.util.*;
import com.fasterxml.jackson.databind.*;
public class SourceMatcherCheck {
 METHOD
 public static void main(String[] args) throws Exception {
  var p=new ObjectMapper().readTree(PAYLOAD);
  var scope=new com.mulino.application.trade.ReceiptEvidenceScopePort.Scope(
   p.path("rangeRootId").asText(),p.path("itemId").asText(),p.path("lotId").asText(),
   p.path("placeId").asText(),p.path("workId").asText(),new BigDecimal("60"),"BOX",
   Instant.parse("2026-10-07T01:00:00Z"),BigDecimal.ZERO);
  if(!receiptPayloadMatches(p,scope))throw new AssertionError("Original source rejected");
  for(String key:List.of("rangeRootId","itemId","lotId","placeId","workId","quantity","unit","occurredAt","startQuantity")) {
   var wrong=p.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)wrong).put(key,"different");
   if(receiptPayloadMatches(wrong,scope))throw new AssertionError("Accepted mismatch "+key);
   var missing=p.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)missing).remove(key);
   if(receiptPayloadMatches(missing,scope))throw new AssertionError("Accepted missing "+key);
  }
  System.out.println("actual receipt matcher: original accepted; 18 negative source mutations rejected");
 }
}'''.replace('METHOD', method).replace('PAYLOAD', json.dumps(payload))
        # Compile the actual scope record; DomainContext is irrelevant to this pure predicate.
        scope_source = (ROOT / 'backend/src/main/java/com/mulino/application/trade/ReceiptEvidenceScopePort.java').read_text()
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            paths = [root / 'SourceMatcherCheck.java', root / 'ReceiptEvidenceScopePort.java', root / 'DomainContext.java']
            paths[0].write_text(body)
            paths[1].write_text(scope_source)
            paths[2].write_text('package com.mulino.application.core; public class DomainContext {}')
            for cmd in (['javac', '--release', '21', '-cp', classpath, '-d', tmp, *map(str, paths)],
                        ['java', '-cp', classpath + ':' + tmp, 'SourceMatcherCheck']):
                run = subprocess.run(cmd, text=True, capture_output=True)
                self.assertEqual(0, run.returncode, run.stdout + run.stderr)
                if run.stdout:
                    print(run.stdout.strip())


if __name__ == '__main__':
    unittest.main()
