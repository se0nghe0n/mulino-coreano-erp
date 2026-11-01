"""Discovery-list publication cannot change synthetic authority or business oracles."""
import copy
import hashlib
import importlib.util
import json
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# Reachable integrated stages: 114 entries before the six-command publication,
# then 120 before the 17 QUERY discovery additions.
PRE_SIX_BASE = 'd49079678a8ceda00178911acaa28cd9f405e710'
PRE_QUERY_BASE = '020be0cff3e784c7a37bc1a9cbef011debaaff78'
ADDED_COMMANDS = [
    'createObligation', 'decideQualityDutyWaiver', 'decideQuantityDutyWaiver',
    'decideRecallDutyWaiver', 'expireHandover', 'openObligation',
]
ADDED_QUERIES = [
    'getTrace', 'getSettlement', 'getSalesOrder', 'getSalesOrders', 'getDelivery',
    'getDeliveries', 'getRegulatoryProcedure', 'getRecall', 'getRecalls', 'traceRecall',
    'getReceipt', 'getReceipts', 'getPurchase', 'getReturn', 'getReturns',
    'getShipment', 'searchShipments',
]
DISCOVERY_ORACLES = {
    'T20': 'tool-schema-registry',
    'C3': 'tools-advertised',
    'V4': 'mcp-tools-equal-allowlist',
}


def git_bytes(ref, path):
    return subprocess.check_output(['git', 'show', ref + ':' + path], cwd=ROOT)


def old_bytes(path):
    return git_bytes(PRE_QUERY_BASE, path)


def old_json(path, ref=PRE_QUERY_BASE):
    return json.loads(git_bytes(ref, path))


def current(path):
    return json.loads((ROOT / path).read_text())


def catalog_ids(catalog):
    return [capability['id'] for capability in catalog['capabilities']]


def discovery_assertions(case, assertion_id):
    return [assertion for subcase in case['subcases'] for assertion in subcase['assertions']
            if assertion['id'] == assertion_id]


def render_reverted_case(case_id, value):
    """Use each owning generator's renderer to prove exact old case bytes after reversal."""
    if case_id == 'T20':
        spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
        request_contract = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(request_contract)
        value = request_contract.Contracts(ROOT).conform_case(value)
        return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()
    spec = importlib.util.spec_from_file_location('c3_author', ROOT / 'verification/cases/C3/author_prerequisites.py')
    author = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(author)
    return author.render_case(value).encode()


def check_query_publication():
    """Return the pre-query and current public ID lists after checking the exact delta."""
    before = old_json('contracts/acceptance-capabilities.json')['capabilities']
    after = current('contracts/acceptance-capabilities.json')['capabilities']
    before_ids = [capability['id'] for capability in before]
    after_ids = [capability['id'] for capability in after]
    added = [capability for capability in after if capability['id'] not in set(before_ids)]
    if [capability['id'] for capability in added] != ADDED_QUERIES:
        raise ValueError('The discovery delta must be exactly the 17 source catalog queries')
    if any(capability.get('kind') != 'QUERY' or capability.get('implemented') is not False
           for capability in added):
        raise ValueError('New public discovery entries must remain non-implemented QUERY metadata')
    if [capability for capability in after if capability['id'] in set(before_ids)] != before:
        raise ValueError('The prior 120 catalog entries or their order changed')
    if len(after_ids) != len(set(after_ids)):
        raise ValueError('Public capability IDs must remain unique')
    return before_ids, after_ids


def restore_metadata(case_id, value):
    """Restore only the documented public discovery fields to the pre-query stage."""
    result = copy.deepcopy(value)
    before = old_json(f'verification/cases/{case_id}/case.json')
    old_public, new_public = check_query_publication()
    changes = 0
    for subcase, prior_subcase in zip(result['subcases'], before['subcases']):
        if case_id == 'V4' and subcase['id'] == 'exposed-write-surface':
            action = next(action for action in subcase['actions'] if action['id'] == 'enumerate')
            prior_action = next(action for action in prior_subcase['actions'] if action['id'] == 'enumerate')
            actual = hashlib.sha256((ROOT / 'contracts/acceptance-capabilities.json').read_bytes()).hexdigest()
            if action['control']['parameters']['allowlistSha256'] != actual:
                raise ValueError('V4 allowlist hash must pin the current source catalog')
            action['control']['parameters']['allowlistSha256'] = prior_action['control']['parameters']['allowlistSha256']
        for assertion, prior in zip(subcase['assertions'], prior_subcase['assertions']):
            if assertion == prior:
                continue
            if assertion['id'] != DISCOVERY_ORACLES[case_id]:
                raise ValueError('Unrecorded business oracle mutation')
            if prior['expected'] != old_public:
                raise ValueError('The pre-query discovery oracle did not match the 120-entry source catalog')
            if assertion['expected'] != new_public:
                raise ValueError('Discovery additions must be exact; no shrink, fake tool, or unrelated name')
            assertion['expected'] = prior['expected']
            changes += 1
    if result != before:
        raise ValueError('Unrecorded action, oracle, actor, scope, outcome, or authority mutation')
    return result, changes


class PublicationPreservation(unittest.TestCase):
    def test_staged_catalog_publications_preserve_114_plus_six_plus_17(self):
        path = 'contracts/acceptance-capabilities.json'
        original = old_json(path, PRE_SIX_BASE)
        prior = old_json(path, PRE_QUERY_BASE)
        new = current(path)
        original_caps = original['capabilities']
        prior_caps = prior['capabilities']
        new_caps = new['capabilities']
        original_ids = {capability['id'] for capability in original_caps}
        prior_ids = {capability['id'] for capability in prior_caps}

        self.assertEqual(114, len(original_caps))
        self.assertEqual(120, len(prior_caps))
        self.assertEqual(ADDED_COMMANDS,
                         [capability['id'] for capability in prior_caps if capability['id'] not in original_ids])
        self.assertEqual(original_caps,
                         [capability for capability in prior_caps if capability['id'] in original_ids])
        self.assertEqual(prior_caps, [capability for capability in new_caps if capability['id'] in prior_ids])
        self.assertEqual(ADDED_QUERIES,
                         [capability['id'] for capability in new_caps if capability['id'] not in prior_ids])
        self.assertTrue(all(capability['kind'] == 'QUERY' and capability['implemented'] is False
                            for capability in new_caps if capability['id'] not in prior_ids))
        self.assertEqual({key: value for key, value in prior.items() if key != 'capabilities'},
                         {key: value for key, value in new.items() if key != 'capabilities'})

    def test_every_discovery_delta_restores_exact_pre_query_case(self):
        for case_id, expected_count in [('T20', 5), ('C3', 30), ('V4', 1)]:
            path = f'verification/cases/{case_id}/case.json'
            value = current(path)
            restored, changed = restore_metadata(case_id, value)
            self.assertEqual(expected_count, changed)
            self.assertEqual(old_json(path), restored)
            expected_id = DISCOVERY_ORACLES[case_id]
            self.assertEqual(expected_count, len(discovery_assertions(value, expected_id)))
            self.assertEqual([(subcase['id'], [assertion['id'] for assertion in subcase['assertions']])
                              for subcase in restored['subcases']],
                             [(subcase['id'], [assertion['id'] for assertion in subcase['assertions']])
                              for subcase in value['subcases']])
            for assertion in discovery_assertions(value, expected_id):
                self.assertEqual(check_query_publication()[1], assertion['expected'])

    def test_reverse_metadata_restores_exact_original_case_bytes(self):
        for case_id in ('T20', 'C3', 'V4'):
            path = f'verification/cases/{case_id}/case.json'
            value, _ = restore_metadata(case_id, current(path))
            self.assertEqual(old_bytes(path), render_reverted_case(case_id, value), case_id)

    def test_all_non_metadata_case_files_remain_byte_identical(self):
        for case_id in ('T20', 'C3', 'V4'):
            base = f'verification/cases/{case_id}'
            old_paths = set(subprocess.check_output(
                ['git', 'ls-tree', '-r', '--name-only', PRE_QUERY_BASE + ':' + base], cwd=ROOT, text=True).splitlines())
            current_paths = {path.relative_to(ROOT / base).as_posix() for path in (ROOT / base).rglob('*')
                             if path.is_file() and '__pycache__' not in path.parts and path.suffix != '.pyc'}
            self.assertEqual(old_paths, current_paths, base + ' file inventory changed')
            metadata_paths = {'case.json'}
            if 'observation-bindings.json' in old_paths:
                metadata_paths.add('observation-bindings.json')
            for path in old_paths - metadata_paths:
                self.assertEqual(old_bytes(base + '/' + path), (ROOT / base / path).read_bytes(), path)

    def test_binding_delta_is_only_derived_case_hash(self):
        for case_id in ('C3', 'V4'):
            path = f'verification/cases/{case_id}/observation-bindings.json'
            old = old_json(path)
            new = current(path)
            self.assertEqual(hashlib.sha256((ROOT / f'verification/cases/{case_id}/case.json').read_bytes()).hexdigest(),
                             new['caseHash'])
            old['caseHash'] = new['caseHash']
            self.assertEqual(old, new)
            old_case_bytes = old_bytes(f'verification/cases/{case_id}/case.json')
            old_case_hash = hashlib.sha256(old_case_bytes).hexdigest()
            old_binding_bytes = old_bytes(path)
            new_binding_bytes = (ROOT / path).read_bytes()
            self.assertEqual(old_binding_bytes.replace(old_case_hash.encode(), new['caseHash'].encode()),
                             new_binding_bytes)

    def test_negative_mutations_are_not_hidden_by_metadata_restoration(self):
        for mutation in ('removeOld', 'removeQuery', 'addFake', 'actor', 'scope', 'outcome', 'quantity'):
            value = current('verification/cases/T20/case.json')
            target_subcase = next(subcase for subcase in value['subcases']
                                  if any(assertion['id'] == DISCOVERY_ORACLES['T20']
                                         for assertion in subcase['assertions']))
            oracle = next(assertion for assertion in target_subcase['assertions']
                          if assertion['id'] == DISCOVERY_ORACLES['T20'])
            if mutation == 'removeOld':
                oracle['expected'].pop(0)
            elif mutation == 'removeQuery':
                oracle['expected'].remove(ADDED_QUERIES[0])
            elif mutation == 'addFake':
                oracle['expected'].append('fakeRegisteredEffect')
            elif mutation == 'actor':
                action = next(action for action in target_subcase['actions'] if 'actorRef' in action)
                action['actorRef'] = 'supervisor'
            elif mutation == 'scope':
                action = next(action for action in target_subcase['actions']
                              if isinstance(action.get('request'), dict) and 'scope' in action['request'])
                action['request']['scope']['organizationId']['$alias'] = 'ORG-OTHER'
            elif mutation == 'quantity':
                action = next(action for subcase in value['subcases'] for action in subcase['actions']
                              if isinstance(action.get('request'), dict)
                              and isinstance(action['request'].get('slots'), dict)
                              and 'quantity' in action['request']['slots'])
                quantity = action['request']['slots']['quantity']
                if isinstance(quantity, dict) and 'value' in quantity:
                    quantity['value'] = '999'
                else:
                    action['request']['slots']['quantity'] = '999'
            else:
                business = next(assertion for assertion in target_subcase['assertions']
                                if assertion['id'] != DISCOVERY_ORACLES['T20'])
                business['expected'] = 'MADE_UP'
            with self.assertRaises(ValueError, msg=mutation):
                restore_metadata('T20', value)

        value = current('verification/cases/V4/case.json')
        target = next(action for subcase in value['subcases'] if subcase['id'] == 'exposed-write-surface'
                      for action in subcase['actions'] if action['id'] == 'enumerate')
        target['control']['parameters']['allowlistSha256'] = '0' * 64
        with self.assertRaises(ValueError):
            restore_metadata('V4', value)


if __name__ == '__main__':
    unittest.main()
