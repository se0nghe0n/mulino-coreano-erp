#!/usr/bin/env python3
"""Author V4 review-closure oracles; never a product or test driver and never PASS evidence.

Idempotent post-processor over verification/cases/V4/case.json. It owns:
- in every route-family subcase (<route>-<family>): the effect-before/effect-after
  observations, the unchanged-effect-<source> and reader-command-not-committed
  assertions, and the authorized counter-call's own idempotency key;
- the whole exposed-write-surface subcase (enumeration of the running system's write
  surface, plan §4.2 "실제 노출된 direct/nested/batch/projection 경로 모두 검사");
- the MCP leg of mixed-atomic-batch (actions mcp-batch*, assertions mcp-batch-*): the same mixed
  operations as one JSON-RPC batch array, rejected as a whole with zero partial effect.
It rewrites the V4 Korean feature from the JSON and refreshes observation-bindings.json
through verification/cases/V7/bind_observations.py.

The per-capability primary-effect sources are imported from the C3 authoring script
(CAP_EFFECTS), so C3 and V4 observe the same tables for the same capability.
Run: python3 -I verification/cases/V4/author_review_fixes.py [--check]
"""
import copy
import hashlib
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DIR = ROOT / 'verification/cases/V4'
def _request_contract():
    import importlib.util
    spec = importlib.util.spec_from_file_location('request_contract', ROOT / 'verification/cases/request_contract.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module.Contracts(ROOT)
REQUEST_CONTRACT = _request_contract()  # step2r round 12: every written case/fixture meets contracts/request-contracts.json


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


C3 = _load('c3_author', ROOT / 'verification/cases/C3/author_prerequisites.py')
BIND = _load('v_bind', ROOT / 'verification/cases/V7/bind_observations.py')
CAP_EFFECTS = C3.CAP_EFFECTS
NOW = '2026-10-07T09:00:00Z'
ORACLE = 'V4.all-alternate-write-paths'
ROUTES = ('direct', 'nested', 'batch', 'projection', 'mcp', 'worker', 'blob', 'management')
FAMILIES = ('inventory', 'allocation', 'work', 'approval', 'trade', 'evidence', 'identity', 'definition',
            'policy', 'operations')
ORG_SCOPE = {'organizationId': {'$alias': 'ORG-A'}}
CASE_SCOPE = {'organizationId': {'$alias': 'ORG-A'}, 'itemId': {'$alias': 'P'}, 'workId': {'$alias': 'WORK'},
              'includeDescendants': True}
BASE_SOURCES = ['segments', 'movements', 'allocations', 'approvals', 'works', 'goals', 'obligations', 'assignments',
                'outbox', 'commands', 'claims', 'grants', 'capabilityAssignments', 'boundaries', 'definitions',
                'policies', 'relations', 'fenceCommits', 'constraints', 'audit']
# C3 observation family -> V4 catalog observation (verification/requirements/mandatory-oracles.json).
OBSERVATION = {'inventory-effects': 'inventory-effects', 'approval-effects': 'approval-effects',
               'external-outbox-effects': 'outbox-effects'}
SURFACE = 'exposed-write-surface'
SURFACES = ['ODATA_METADATA', 'MCP_SERVER_DISCOVER', 'MCP_TOOLS_LIST', 'WORKER_HANDLER_REGISTRY', 'MANAGEMENT_ENDPOINTS']
PROBE_CLASSES = ['DIRECT_CREATE', 'DIRECT_UPDATE', 'DIRECT_DELETE', 'DEEP_INSERT', 'UPSERT', 'BATCH_CHANGESET',
                 'DRAFT_ACTIVATE', 'NESTED_NAVIGATION_CREATE', 'NESTED_NAVIGATION_UPDATE', 'NESTED_NAVIGATION_DELETE',
                 'BOUND_ACTION', 'UNBOUND_ACTION', 'MCP_TOOL_CALL', 'WORKER_HANDLER_SUBMIT',
                 'MANAGEMENT_ENDPOINT_WRITE']
ALLOWLIST = ROOT / 'contracts/acceptance-capabilities.json'


def ref(action, pointer):
    return {'$result': {'actionId': action, 'pointer': pointer}}


def alias(name):
    return {'$alias': name}


def observation_for(table):
    return OBSERVATION.get(C3.EFFECT_OBSERVATION.get(table, ''), 'same-auth-path')


def observe(aid, snapshot, sources, scope):
    return {'id': aid, 'kind': 'observe', 'observation': {'scope': copy.deepcopy(scope),
            'snapshotRef': ref(snapshot, '/response/snapshotRevision'), 'asOf': NOW, 'knownAt': NOW,
            'sources': list(sources)}, 'evidenceRefs': [aid + ':actual-artifact']}


def assertion(aid, op, source, expected, explain, observation='same-auth-path', baseline=None, scope=None):
    x = {'id': aid, 'op': op, 'source': source, 'expected': expected, 'requirementRefs': ['D08', 'D17', 'D24'],
         'evidenceRefs': [source['actionId'] + ':actual-artifact'], 'scope': copy.deepcopy(scope or CASE_SCOPE),
         'oracleExplanation': explain, 'oracleRef': {'oracleId': ORACLE, 'observationNames': [observation]}}
    if baseline:
        x['baseline'] = baseline
        x['evidenceRefs'].append(baseline['actionId'] + ':actual-artifact')
    return x


def unchanged_effect(table, cap):
    return assertion('unchanged-effect-' + table, 'sameAs', {'actionId': 'effect-after', 'pointer': '/data/rawRows/' + table},
                     True, f'{cap}의 주 효과 원천 {table} 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. '
                     '공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표).',
                     observation_for(table), {'actionId': 'effect-before', 'pointer': '/data/rawRows/' + table}, ORG_SCOPE)


def keys(node):
    if isinstance(node, dict):
        return ([node['commandIdempotencyKey']] if 'commandIdempotencyKey' in node else []) + \
            [k for k, v in node.items() if k != 'commandIdempotencyKey' for k in keys(v)]
    if isinstance(node, list):
        return [k for v in node for k in keys(v)]
    return []


def rekey(node, old, new):
    if isinstance(node, dict):
        return {k: (new if k == 'commandIdempotencyKey' and v == old else rekey(v, old, new)) for k, v in node.items()}
    if isinstance(node, list):
        return [rekey(v, old, new) for v in node]
    return node


def route_subcase(sub):
    route, family = sub['id'].split('-', 1)
    acts = [a for a in sub['actions'] if a['id'] not in ('effect-before', 'effect-after')]
    sub['assertions'] = [a for a in sub['assertions']
                         if not a['id'].startswith(('unchanged-effect-', 'reader-command-not-committed'))]
    by = {a['id']: a for a in acts}
    denied, positive = by['attempt'], by['authorized-same-route']
    cap = denied['capabilityId']
    attempt_key = f'V4-{sub["id"]}'
    auth_key = attempt_key + '-authorized'
    # Same business payload and revision; a separate key keeps cross-principal key semantics (plan §7.3 allows an
    # independent namespace or a refusal) out of the authority proof (same fix as C3). Batch operations and blob
    # business actions carry the key one level down, so every occurrence of the attempt key is replaced.
    assert keys(denied['request']) == [attempt_key], sub['id']
    positive['request'] = rekey(copy.deepcopy(denied['request']), attempt_key, auth_key)
    for a in sub['assertions']:
        if a['id'] == 'authorized-route-committed':
            a['source']['where']['commandIdempotencyKey'] = auth_key
    ids = [a['id'] for a in acts]
    acts.insert(ids.index('before') + 1, observe('effect-before', 'before-snapshot', CAP_EFFECTS[cap], ORG_SCOPE))
    ids = [a['id'] for a in acts]
    acts.insert(ids.index('after') + 1, observe('effect-after', 'after-snapshot', CAP_EFFECTS[cap], ORG_SCOPE))
    sub['actions'] = acts
    for table in CAP_EFFECTS[cap]:
        sub['assertions'].append(unchanged_effect(table, cap))
    sub['assertions'].append(assertion(
        'reader-command-not-committed', 'count',
        {'actionId': 'after', 'pointer': '/data/rawRows/commands',
         'where': {'commandIdempotencyKey': attempt_key, 'stableRequestOwnerId': alias('reader'), 'status': 'COMMITTED'}},
        0, '거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다.'))
    return sub


def wire(aid, method, actor='reader', args=None):
    body = {'jsonrpc': '2.0', 'id': aid, 'method': method, 'params': dict(args or {})}
    body['params']['_meta'] = {'io.modelcontextprotocol/protocolVersion': '2026-07-28',
                               'io.modelcontextprotocol/clientInfo': {'name': 'ontology-v4-surface', 'version': '1.0.0'},
                               'io.modelcontextprotocol/clientCapabilities': {}}
    # contracts/mcp/s0-protocol.md "독립 요청": Accept is required (406 otherwise); no Origin is sent because no contract
    # declares an allowed one and an Origin-less request is accepted.
    headers = {'MCP-Protocol-Version': '2026-07-28', 'Mcp-Method': method, 'Accept': 'application/json, text/event-stream',
               'Content-Type': 'application/json'}
    return {'id': aid, 'kind': 'invoke', 'actorRef': actor, 'route': 'wire', 'protocolOperation': method,
            'request': {'transport': 'streamable-http', 'httpMethod': 'POST', 'headers': headers, 'body': body,
                        'credentialProfileRef': actor},
            'evidenceRefs': [aid + ':redacted-raw-wire', aid + ':authenticated-context', aid + ':server-response']}


MIXED = 'mixed-atomic-batch'
MCP_BATCH_SOURCES = ['segments', 'movements', 'allocations', 'approvals', 'works', 'obligations', 'outbox', 'commands', 'claims',
                     'stocktakes']
MCP_BATCH_ACTIONS = ('mcp-batch-before', 'mcp-batch', 'mcp-batch-after-snapshot', 'mcp-batch-after')


def mixed_batch_mcp(sub):
    """MCP leg of the mixed batch (plan §13.2 V4 MCP path, contracts/mcp/s0-protocol.md "batch ... 거부").
    MCP has no changeset: the same allowed RECORD and forbidden dispatch sent as one JSON-RPC batch array must be
    rejected as an invalid request as a whole, and the allowed RECORD must leave no partial effect."""
    acts = [a for a in sub['actions'] if a['id'] not in MCP_BATCH_ACTIONS]
    sub['assertions'] = [a for a in sub['assertions'] if not a['id'].startswith('mcp-batch-')]
    batch = next(a for a in acts if a['id'] == 'batch')
    calls = []
    for op in batch['request']['operations']:
        args = rekey(copy.deepcopy(op), op['commandIdempotencyKey'], op['commandIdempotencyKey'] + '-mcp')
        calls.append({'jsonrpc': '2.0', 'id': 'mcp-batch-' + op['capabilityId'], 'method': 'tools/call',
                      'params': {'name': op['capabilityId'], 'arguments': args}})
    call = wire('mcp-batch', 'tools/call', actor=batch['actorRef'])
    meta = call['request']['body']['params']['_meta']
    for c in calls:
        c['params']['_meta'] = copy.deepcopy(meta)
    call['request']['body'] = calls
    allowed_key = batch['request']['operations'][0]['commandIdempotencyKey'] + '-mcp'
    acts += [observe('mcp-batch-before', 'after-snapshot', MCP_BATCH_SOURCES, ORG_SCOPE), call,
             snapshot_query('mcp-batch-after-snapshot'), observe('mcp-batch-after', 'mcp-batch-after-snapshot', MCP_BATCH_SOURCES, ORG_SCOPE)]
    sub['actions'] = acts
    if 'wire' not in sub['requiredAdapters']:
        sub['requiredAdapters'].append('wire')
    o = 'mixed-batch-allowed-partial-effects'
    x = [assertion('mcp-batch-http', 'equals', {'actionId': 'mcp-batch', 'pointer': '/response/httpStatus'}, 400,
                   'MCP에는 changeset이 없다. 허용된 RECORD와 금지된 출고를 한 JSON-RPC batch 배열로 보내면 envelope 자체가 '
                   '유효하지 않은 요청이라 HTTP 400이다(contracts/mcp/s0-protocol.md: batch와 malformed 입력은 거부한다).', o),
         assertion('mcp-batch-invalid-request', 'equals', {'actionId': 'mcp-batch', 'pointer': '/response/body/error/code'}, -32600,
                   'batch 배열은 JSON-RPC Invalid Request(-32600)로 한 번에 거부한다. 요소별 tool result로 나눠 일부를 실행하지 않는다. '
                   'Mcp-Name header도 없지만 envelope 검사가 mirrored header 검사보다 먼저이므로 -32020이 아니다(contracts/mcp/s0-protocol.md 오류 우선순위).', o),
         assertion('mcp-batch-no-tool-result', 'absent', {'actionId': 'mcp-batch', 'pointer': '/response/body/result'}, None,
                   '거부된 batch 응답에는 tool result가 없다. 허용 요소만 실행한 결과를 돌려주지 않는다.', o),
         assertion('mcp-batch-allowed-record-not-committed', 'count',
                   {'actionId': 'mcp-batch-after', 'pointer': '/data/rawRows/commands',
                    'where': {'commandIdempotencyKey': allowed_key, 'status': 'COMMITTED'}}, 0,
                   'batch 안의 허용된 recordStocktake는 MCP 경로에서도 COMMITTED command를 남기지 않는다(부분 효과0).', o, scope=ORG_SCOPE),
         assertion('mcp-batch-allowed-claims0', 'count',
                   {'actionId': 'mcp-batch-after', 'pointer': '/data/rawRows/claims', 'where': {'commandIdempotencyKey': allowed_key}}, 0,
                   '허용된 RECORD의 실행 claim도 0이다. 거부 전에 일부 요소가 실행 단계에 들어가지 않았다.', o, scope=ORG_SCOPE)]
    for table in MCP_BATCH_SOURCES:
        if table == 'commands':
            continue  # a REJECTED protocol attempt may leave no record or a rejected one; COMMITTED is counted above
        x.append(assertion('mcp-batch-unchanged-' + table, 'sameAs', {'actionId': 'mcp-batch-after', 'pointer': '/data/rawRows/' + table}, True,
                           f'MCP batch 전후 조직 범위의 {table} 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다.',
                           o, {'actionId': 'mcp-batch-before', 'pointer': '/data/rawRows/' + table}, ORG_SCOPE))
    sub['assertions'] += x
    return sub


def snapshot_query(aid):
    return {'id': aid, 'kind': 'query', 'actorRef': 'delegator', 'route': 'api', 'capabilityId': 'getInventory',
            'request': {'scope': copy.deepcopy(CASE_SCOPE), 'asOf': NOW, 'knownAt': NOW}, 'evidenceRefs': [aid + ':actual-artifact']}


def surface_subcase(template):
    """Enumerate what the running system exposes, then probe every write-capable item with a READ-grant subject."""
    capabilities = json.loads(ALLOWLIST.read_text())['capabilities']
    public = [c['id'] for c in capabilities]
    effect_tables = sorted({t for tables in CAP_EFFECTS.values() for t in tables} - set(BASE_SOURCES))
    move = copy.deepcopy(next(a for a in template['actions'] if a['id'] == 'attempt'))
    positive_key = 'V4-exposed-write-surface-authorized'
    move.update(id='authorized-control', actorRef='delegator', route='mcp',
                evidenceRefs=['authorized-control:actual-artifact'])
    move['request']['commandIdempotencyKey'] = positive_key
    rows = '/data/hostObservation/extractor/rawRows/'
    actions = [
        {'id': 'setup', 'kind': 'installFixture', 'evidenceRefs': ['setup:actual-artifact']},
        snapshot_query('before-snapshot'),
        observe('before', 'before-snapshot', BASE_SOURCES, CASE_SCOPE),
        observe('effect-before', 'before-snapshot', effect_tables, ORG_SCOPE),
        wire('surface-discover', 'server/discover'),
        wire('surface-tools', 'tools/list'),
        {'id': 'enumerate', 'kind': 'control', 'control': {'type': 'process', 'operation': 'enumerateWriteSurface', 'parameters': {
            'scope': copy.deepcopy(ORG_SCOPE), 'environmentId': 'V4-exposed-surface-isolated',
            'enumerationId': 'V4-exposed-write-surface', 'actorRef': 'reader', 'surfaces': SURFACES,
            'probeClasses': PROBE_CLASSES, 'allowlistRef': 'contracts/acceptance-capabilities.json',
            'allowlistSha256': hashlib.sha256(ALLOWLIST.read_bytes()).hexdigest(),
            'targetPolicy': 'SYNTHETIC_FIXTURE_ENTITIES_ONLY'}},
         'evidenceRefs': ['enumerate:actual-host-command', 'enumerate:independent-extractor', 'enumerate:actual-artifact']},
        snapshot_query('after-snapshot'),
        observe('after', 'after-snapshot', BASE_SOURCES, CASE_SCOPE),
        observe('effect-after', 'after-snapshot', effect_tables, ORG_SCOPE),
        move,
        snapshot_query('authorized-after-snapshot'),
        observe('authorized-after', 'authorized-after-snapshot', BASE_SOURCES, CASE_SCOPE),
    ]
    x = []
    x.append(assertion('surfaces-enumerated', 'exactSet', {'actionId': 'enumerate', 'pointer': rows + 'surfaces', 'field': 'surface'},
                       SURFACES, '실행 중 시스템의 다섯 노출 면(OData $metadata entity set·action·function, MCP server/discover, '
                       'MCP tools/list, worker handler registry, 관리/actuator endpoint)을 실제로 읽었다. 고정 목록이 아닌 이 열거가 '
                       '아래 probe의 대상이다(계획 §4.2 실제 노출 경로).', scope=ORG_SCOPE))
    x.append(assertion('surfaces-read-artifacts', 'fieldsPresent', {'actionId': 'enumerate', 'pointer': rows + 'surfaces'},
                       ['surface', 'sourceRef', 'itemCount', 'sha256'],
                       '각 노출 면의 원문 artifact 위치·hash와 열거 항목 수가 독립 extractor 원행에 있다.', scope=ORG_SCOPE))
    x.append(assertion('no-unlisted-write-surface', 'count', {'actionId': 'enumerate', 'pointer': rows + 'surfaceItems',
                       'where': {'writeCapable': True, 'allowlisted': False}}, 0,
                       '쓰기 가능한 노출 항목(entity set의 insert/update/delete/upsert, action, tool, handler, endpoint)은 모두 공개 '
                       'capability allowlist에 있다. raw core CRUD나 목록 밖 쓰기 면이 하나라도 있으면 실패한다.', scope=ORG_SCOPE))
    x.append(assertion('mcp-tools-equal-allowlist', 'exactSet', {'actionId': 'surface-tools', 'pointer': '/response/body/result/tools',
                       'field': 'name'}, public,
                       'READ 주체가 받은 MCP tools/list는 공개 capability allowlist와 정확히 같다. 목록 밖 tool은 없다.', scope=ORG_SCOPE))
    x.append(assertion('mcp-discover-complete', 'equals', {'actionId': 'surface-discover', 'pointer': '/response/body/result/resultType'},
                       'complete', 'server/discover를 실제로 왕복해 노출 capability를 읽었다.', scope=ORG_SCOPE))
    x.append(assertion('probe-classes-covered', 'exactSet', {'actionId': 'enumerate', 'pointer': rows + 'probeCoverage', 'field': 'probeClass'},
                       PROBE_CLASSES, '열거한 대상마다 direct 생성·수정·삭제, deep insert, upsert, $batch changeset, draft activation, '
                       'nested navigation 생성·수정·삭제, bound/unbound action, MCP tools/call, worker 제출, 관리 endpoint 쓰기를 시도했다.',
                       scope=ORG_SCOPE))
    x.append(assertion('probe-coverage-complete', 'count', {'actionId': 'enumerate', 'pointer': rows + 'probeCoverage',
                       'where': {'complete': False}}, 0,
                       'probe class마다 열거된 적용 대상 전부를 시도했다. 일부 대상만 probe하고 통과하지 않는다.', scope=ORG_SCOPE))
    x.append(assertion('probes-observed', 'fieldsPresent', {'actionId': 'enumerate', 'pointer': rows + 'probes'},
                       ['surface', 'target', 'probeClass', 'outcome', 'committed', 'transcriptRef'],
                       '모든 probe의 대상·방식·결과·commit 여부와 redacted transcript가 원행으로 남는다. probe 0개는 통과가 아니다.',
                       scope=ORG_SCOPE))
    x.append(assertion('probes-none-committed', 'count', {'actionId': 'enumerate', 'pointer': rows + 'probes', 'where': {'committed': True}},
                       0, 'READ grant 주체의 probe는 하나도 commit되지 않았다.', scope=ORG_SCOPE))
    x.append(assertion('probes-none-applied', 'count', {'actionId': 'enumerate', 'pointer': rows + 'probes', 'where': {'outcome': 'APPLIED'}},
                       0, 'READ grant 주체의 probe 응답 중 APPLIED는0이다.', scope=ORG_SCOPE))
    x.append(assertion('probes-none-unknown', 'count', {'actionId': 'enumerate', 'pointer': rows + 'probes', 'where': {'outcome': 'UNKNOWN'}},
                       0, '시간초과·응답 유실 등 결과 미확인 probe는0이다. 미확인을 거부로 세지 않는다.', scope=ORG_SCOPE))
    for table in BASE_SOURCES:
        if table in ('commands', 'fenceCommits', 'constraints', 'audit'):
            continue  # REJECTED command records and denial audits are permitted; route subcases skip the same four
        x.append(assertion('unchanged-' + table, 'sameAs', {'actionId': 'after', 'pointer': '/data/rawRows/' + table}, True,
                           f'열거 probe 전후 동일 조직·업무·실물 scope의 {table} 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다.',
                           baseline={'actionId': 'before', 'pointer': '/data/rawRows/' + table}))
    for table in effect_tables:
        x.append(assertion('unchanged-effect-' + table, 'sameAs', {'actionId': 'effect-after', 'pointer': '/data/rawRows/' + table},
                           True, f'모든 capability의 주 효과 원천 {table} 원행 전체가 조직 범위에서 열거 probe 전후 같다.',
                           observation_for(table), {'actionId': 'effect-before', 'pointer': '/data/rawRows/' + table}, ORG_SCOPE))
    x.append(assertion('reader-command-not-committed', 'count', {'actionId': 'after', 'pointer': '/data/rawRows/commands',
                       'where': {'stableRequestOwnerId': alias('reader'), 'status': 'COMMITTED'}}, 0,
                       'probe 동안 reader 명의로 COMMITTED된 command record는0이다.'))
    x.append(assertion('authorized-control-applied', 'equals', {'actionId': 'authorized-control', 'pointer': '/response/outcome'}, 'APPLIED',
                       '같은 설치에서 위임 있는 delegator의 공개 MCP moveQuantity 20 BOX는 APPLIED다. 서버가 모든 쓰기를 무조건 막아 통과하는 것이 아니다.'))
    x.append(assertion('authorized-control-committed-once', 'count', {'actionId': 'authorized-after', 'pointer': '/data/rawRows/commands',
                       'where': {'commandIdempotencyKey': positive_key, 'stableRequestOwnerId': alias('delegator'), 'status': 'COMMITTED'}}, 1,
                       '대조 호출은 자기 멱등키로 정확히 한 번 COMMITTED다.'))
    return {'id': SURFACE, 'title': '실행 중 시스템이 노출한 모든 쓰기 면을 열거하고 READ 주체의 우회 효과0을 확인한다',
            'fixtureRef': 'verification/cases/V4/fixture.json',
            'requiredAdapters': ['fixture', 'api', 'db', 'wire', 'mcp', 'host'],
            'actions': actions, 'assertions': x,
            'oracleExplanation': '고정 route inventory는 존재하는 endpoint 주장이 아니므로 실행 중 노출 면을 열거해 probe 집합을 만든다. '
                                 '열거·probe는 독립 host extractor 원행으로, 효과0은 전후 DB 원행으로, 서버가 쓰기를 처리할 수 있음은 '
                                 '위임 있는 대조 호출로 확인한다(계획 §4.2, §13.2 V4).'}


def explain_map(case):
    out = {}
    for s in case['subcases']:
        for a in s['assertions']:
            if s['id'] == SURFACE or a['id'].startswith(('unchanged-effect-', 'reader-command-not-committed')) \
                    or (s['id'] == MIXED and a['id'].startswith('mcp-batch-')):
                out[(s['id'], a['id'])] = a['oracleExplanation']
    return out


def render_feature(case, header, previous):
    """Same layout as the committed V4 feature; owned assertions carry their Korean explanation."""
    explain = explain_map(case)
    lines = header[:]
    for s in case['subcases']:
        lines += ['', '  @sit', '  시나리오: ' + s['title'],
                  f'    먼저 사례 파일 "verification/cases/V4/case.json"의 "{s["id"]}"를 준비한다']
        lines += [f'    만일 "{a.get("actorRef", "시스템")}" 역할이 "{a["id"]}" 행동을 수행한다' for a in s['actions']]
        for a in s['assertions']:
            text = explain.get((s['id'], a['id'])) or previous.get((s['id'], a['id'])) \
                or f'{a["oracleRef"]["observationNames"][0]} {a["id"]}'
            lines.append(f'    그러면 "{a["id"]}" assertion으로 "{text}"를 확인한다')
    return '\n'.join(lines) + '\n'


def previous_texts(text):
    import re
    out, sid = {}, None
    for line in text.split('\n'):
        m = re.search(r'의 "([^"]+)"를 준비한다$', line)
        if m:
            sid = m.group(1)
        m = re.match(r'    그러면 "([^"]+)" assertion으로 "(.*)"를 확인한다$', line)
        if m and sid:
            out[(sid, m.group(1))] = m.group(2)
    return out


def self_check(case):
    for s in case['subcases']:
        parts = s['id'].split('-', 1)
        if s['id'] == MIXED:
            acts = {a['id']: a for a in s['actions']}
            assert isinstance(acts['mcp-batch']['request']['body'], list) and len(acts['mcp-batch']['request']['body']) == 2, s['id']
            assert {'mcp-batch-http', 'mcp-batch-invalid-request', 'mcp-batch-allowed-record-not-committed'} <= {a['id'] for a in s['assertions']}
            continue
        if s['id'] == SURFACE:
            ids = {a['id'] for a in s['assertions']}
            assert {'surfaces-enumerated', 'no-unlisted-write-surface', 'probes-none-committed',
                    'probe-coverage-complete', 'authorized-control-committed-once'} <= ids
            continue
        if len(parts) != 2 or parts[0] not in ROUTES or parts[1] not in FAMILIES:
            continue
        acts = {a['id']: a for a in s['actions']}
        cap = acts['attempt']['capabilityId']
        assert acts['effect-before']['observation']['sources'] == CAP_EFFECTS[cap] == acts['effect-after']['observation']['sources'], s['id']
        assert {'unchanged-effect-' + t for t in CAP_EFFECTS[cap]} <= {a['id'] for a in s['assertions']}, s['id']
        assert keys(acts['attempt']['request']) == ['V4-' + s['id']], s['id']
        assert keys(acts['authorized-same-route']['request']) == ['V4-' + s['id'] + '-authorized'], s['id']
        assert rekey(acts['authorized-same-route']['request'], 'V4-' + s['id'] + '-authorized', 'V4-' + s['id']) == acts['attempt']['request'], s['id']


def build():
    case = REQUEST_CONTRACT.conform_case(json.loads((DIR / 'case.json').read_text()))  # derived requests copy conformed ones
    case['subcases'] = [s for s in case['subcases'] if s['id'] != SURFACE]
    template = next(s for s in case['subcases'] if s['id'] == 'mcp-inventory')
    for s in case['subcases']:
        parts = s['id'].split('-', 1)
        if len(parts) == 2 and parts[0] in ROUTES and parts[1] in FAMILIES:
            route_subcase(s)
        if s['id'] == MIXED:
            mixed_batch_mcp(s)
    case['subcases'].append(surface_subcase(template))
    REQUEST_CONTRACT.conform_case(case)
    self_check(case)
    return case


def main():
    check = sys.argv[1:] == ['--check']
    case_path, feature_path = DIR / 'case.json', DIR / 'scenario.feature'
    old_case, old_feature = case_path.read_text(), feature_path.read_text()
    old_bindings = (DIR / 'observation-bindings.json').read_text()
    case = build()
    case_text = C3.render_case(case)
    feature_text = render_feature(case, old_feature.split('\n')[:3], previous_texts(old_feature))
    case_path.write_text(case_text)
    bindings_text = BIND.render(BIND.build('V4'))
    if check:
        case_path.write_text(old_case)
        drift = [n for n, a, b in [('case.json', old_case, case_text), ('scenario.feature', old_feature, feature_text),
                                    ('observation-bindings.json', old_bindings, bindings_text)] if a != b]
        if drift:
            sys.exit('V4 drifted from author_review_fixes.py: ' + ', '.join(drift))
        print('V4 review fixes: CURRENT')
        return
    feature_path.write_text(feature_text)
    (DIR / 'observation-bindings.json').write_text(bindings_text)
    print('V4 review fixes: WRITTEN', len(case['subcases']), 'subcases')


if __name__ == '__main__':
    main()
