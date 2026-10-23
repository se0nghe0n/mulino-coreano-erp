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

from source_contracts import ROOT, CASES, document_and_event, receipt_chain, repair_inputs

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
        original = base64.b64decode(graph[0]['request']['slots']['contentBase64'])
        self.assertEqual(hashlib.sha256(original).hexdigest(), graph[0]['request']['slots']['expectedHash'])
        self.assertEqual(json.loads(original), json.loads(graph[1]['request']['slots']['payload']))
        self.assertEqual(self.binding['workId'], json.loads(original)['workId'])
        self.assertEqual('60', graph[2]['request']['slots']['quantity'])
        self.assertEqual('60', graph[3]['request']['slots']['quantity'])
        contract = rc.Contracts(ROOT)
        for action in graph:
            problems, known = contract.command_problems(action, {'id': 'bound-receipt', 'assertions': []})
            self.assertEqual(([], []), (problems, known), (action['id'], problems, known))
        self.assertFalse(any('VERIFIED' in a['request']['slots'].get('valueState', '') for a in graph))

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
        self.assertEqual({'fixtureAlias': 'PORT'}, json.loads(scope['request']['slots']['payload'])['reportedScope']['placeId'])
        claims = [a for a in subs['conflicting-claims']['actions'] if a.get('capabilityId') == 'recordActivity']
        self.assertEqual(['100', '98'], [a['request']['slots']['quantity'] for a in claims])
        self.assertEqual(['synthetic-warehouse', 'synthetic-carrier'], [a['request']['slots']['sourceNamespace'] for a in claims])
        self.assertEqual('claim', json.loads(claims[1]['request']['slots']['payload'])['samePhysicalReceiptCandidate']['priorSourceAction'])
        for sub in (subs[x] for x in ('no-receipt-evidence', 'insufficient-source-scope', 'conflicting-claims')):
            self.assertFalse(any(a.get('capabilityId') in ('confirmReceipt', 'linkCanonicalOccurrence') for a in sub['actions']))

    def test_actual_receipt_matcher_rejects_source_mutations(self):
        """Execute the product's matcher source, not a Python facsimile of it."""
        source = (ROOT / 'backend/src/main/java/com/mulino/application/evidence/EvidenceReconciliation.java').read_text()
        method = source.split('  private static boolean receiptPayloadMatches', 1)[1].split('  private static boolean sameQuantity', 1)[0]
        method = '  private static boolean receiptPayloadMatches' + method
        graph = receipt_chain('r', 'A', self.binding, '60', '2026-10-07T01:00:00Z')
        payload = graph[1]['request']['slots']['payload']
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
