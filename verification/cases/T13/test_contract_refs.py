"""T13 요청 참조의 정적 회귀 검사다. 실제 시나리오 인수는 수행하지 않는다.

미해결 frontier (assertion/fixture를 바꿔 숨기지 않는다):
- cancel-unexecuted: 원본 raw report의 installer가 PO-A를 설치하지 않았다.
- supplier-*의 transmission은 RuntimeCommands의 outboxId/decision/evidenceId
  계약과 다르고, supplier-reply의 canonical source chain도 없다. 현재 어떤
  fixture actor도 matchSourceIdentity/linkCanonicalOccurrence 권한이 없다.
- policy-rechecked는 정책 payload 계약 정렬이 필요하다.
- partial-excess-return-relocation은 shipment/receipt/quality/sales/return
  source chain과 request 계약 정렬이 필요하다. approval80의 consumptionPolicy,
  reduce-goal의 승인 결합 slot과 /response/workRevision도 제품 계약에 없다.
  승인 조건을 지워 단순 reviseGoal로 바꾸지 않는다.
- changed-*의 /response/changes/<field> assertion과 REJECTED 결과 등은
  현 PurchaseCommands 응답과 차이가 있지만 기존 oracle을 그대로 둔다.
"""
import copy
import importlib.util
import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

from author_contract_refs import BASELINE, DIRECTORY, SUPPLIER_CASES, repair, result

ROOT = DIRECTORY.parents[2]
CASE_PATH = 'verification/cases/T13/case.json'
spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
request_contract = importlib.util.module_from_spec(spec)
spec.loader.exec_module(request_contract)


def baseline_bytes():
    return subprocess.check_output(['git', 'show', f'{BASELINE}:{CASE_PATH}'], cwd=ROOT)


def assertion_bytes(raw):
    # Decode only to locate the exact JSON array endpoint; compare original bytes,
    # including whitespace, ordering, expected values and negative probes.
    text = raw.decode()
    decoder = json.JSONDecoder()
    found = []
    for match in re.finditer(r'"assertions":\s*', text):
        _, end = decoder.raw_decode(text, match.end())
        found.append(text[match.end():end].encode())
    return found


class PurchaseContractRefs(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = baseline_bytes()
        cls.before = json.loads(cls.original)
        cls.current_bytes = (ROOT / CASE_PATH).read_bytes()
        cls.current = json.loads(cls.current_bytes)

    def test_all_123_assertions_are_byte_for_byte_unchanged(self):
        self.assertEqual(123, sum(len(s['assertions']) for s in self.current['subcases']))
        self.assertEqual(assertion_bytes(self.original), assertion_bytes(self.current_bytes))
        self.assertEqual(17, len(assertion_bytes(self.current_bytes)))

    def test_reproducible_scoped_patch_and_fixed_point(self):
        self.assertEqual(self.current, repair(copy.deepcopy(self.before)))
        self.assertEqual(self.current, repair(copy.deepcopy(self.current)))
        old = {s['id']: s for s in self.before['subcases']}
        for sub in self.current['subcases']:
            previous = old[sub['id']]
            self.assertEqual(len(previous['actions']), len(sub['actions']))
            for a, b in zip(previous['actions'], sub['actions']):
                if a.get('capabilityId') == 'proposePurchase':
                    expected = copy.deepcopy(a)
                    expected['request']['slots']['goalVersionId'] = result('work', '/response/goalVersionId')
                    expected['request']['expectedRevision'] = result('work', '/response/revision')
                    self.assertEqual(expected, b)
                elif sub['id'] in SUPPLIER_CASES and a.get('capabilityId') == 'recordSupplierReply':
                    expected = copy.deepcopy(a)
                    del expected['request']['slots']['originalQuantity']
                    del expected['request']['provenance']['originalQuantity']
                    expected['request']['slots']['purchaseId'] = result('dispatch', '/response/orderId')
                    expected['request']['expectedRevision'] = result('dispatch', '/response/revision')
                    self.assertEqual(expected, b)
                else:
                    self.assertEqual(a, b, (sub['id'], a['id']))
            # Non-action content includes all assertions, fixtures, actors and explanations.
            self.assertEqual({k:v for k,v in previous.items() if k != 'actions'},
                             {k:v for k,v in sub.items() if k != 'actions'})

    def test_selected_requests_follow_published_slot_allowlist(self):
        catalog = json.loads((ROOT / 'contracts/capability-slots.json').read_text())['capabilities']
        contracts = request_contract.Contracts(ROOT)
        selected = 0
        for sub in self.current['subcases']:
            for action in sub['actions']:
                if action.get('capabilityId') not in {'proposePurchase', 'recordSupplierReply'}:
                    continue
                selected += 1
                request = action['request']
                self.assertLessEqual(set(request['slots']), set(catalog[action['capabilityId']]['slots']))
                self.assertEqual(set(request['slots']), set(request['provenance']))
                problems, _ = contracts.command_problems(action, sub)
                self.assertEqual([], problems)
        self.assertEqual(19, selected)
        self.assertNotIn('/response/goal/id', json.dumps(self.current))

    def test_product_response_methods_resolve_references(self):
        # Compile the exact current product return expressions, not a handwritten
        # mock response. Sentinel revision 7 detects hardcoded setup revision 1.
        work = (ROOT / 'backend/src/main/java/com/mulino/application/work/WorkLifecycle.java').read_text()
        purchase = (ROOT / 'backend/src/main/java/com/mulino/application/trade/purchase/PurchaseCommands.java').read_text()
        wm = re.search(r' private Map<String,Object> result\(String id,String goal,int revision,String outcome\)\{[^\n]+', work).group().replace('private ', 'static ', 1)
        pm = re.search(r' private Map<String,Object> proposalResult\(Map<String,Object> p\)\{[^\n]+', purchase).group().replace('private ', 'static ', 1)
        dispatch = purchase.split('result.putAll(Map.of("objectId",order,', 1)[1].split('));', 1)[0]
        expression = 'Map.of("objectId",order,' + dispatch + ')'
        body = '''import java.util.*;
public class PurchaseResponseProbe {
WORK_METHOD
PROPOSAL_METHOD
static Map<String,Object> dispatch(String order,String line,String out){return DISPATCH;}
static void emit(String kind,Map<String,Object> m){for(var e:m.entrySet())if(!(e.getValue() instanceof Map)&&!(e.getValue() instanceof List))System.out.println(kind+"|"+e.getKey()+"|"+e.getValue());}
public static void main(String[] args){
 emit("work",result("work-id","goal-id",7,"APPLIED"));
 emit("proposal",proposalResult(Map.of("proposalId","proposal-id","revision",3,"proposalHash","hash","workId","work-id")));
 emit("dispatch",dispatch("order-id","line-id","outbox-id"));
}}
'''.replace('WORK_METHOD', wm).replace('PROPOSAL_METHOD', pm).replace('DISPATCH', expression)
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / 'PurchaseResponseProbe.java'
            source.write_text(body)
            compiled = subprocess.run(['javac', '--release', '21', str(source)], capture_output=True, text=True)
            self.assertEqual(0, compiled.returncode, compiled.stderr)
            run = subprocess.run(['java', '-cp', temporary, 'PurchaseResponseProbe'], capture_output=True, text=True)
            self.assertEqual(0, run.returncode, run.stderr)
        responses = {}
        for line in run.stdout.splitlines():
            kind, key, value = line.split('|', 2)
            responses.setdefault(kind, {})[key] = value
        count = 0
        for sub in self.current['subcases']:
            for action in sub['actions']:
                if action.get('capabilityId') not in {'proposePurchase', 'approvePurchase', 'revisePurchase', 'dispatchPurchaseOrder', 'recordSupplierReply'}:
                    continue
                request = action['request']
                values = list(request['slots'].values()) + [request['expectedRevision']]
                for value in values:
                    ref = value.get('$result') if isinstance(value, dict) else None
                    if ref and ref['actionId'] in responses:
                        key = ref['pointer'].removeprefix('/response/')
                        self.assertIn(key, responses[ref['actionId']], (sub['id'], action['id'], ref))
                        count += 1
        self.assertGreater(count, 90)
        self.assertEqual('goal-id', responses['work']['goalVersionId'])
        self.assertEqual('7', responses['work']['revision'])
        self.assertEqual('order-id', responses['dispatch']['orderId'])


if __name__ == '__main__':
    unittest.main()
