#!/usr/bin/env python3
"""Independent S0 HTTP observer. No SDK, model calls, DB mutations outside tools."""
import argparse, base64, datetime, hashlib, json, os, pathlib, platform, sys
import urllib.error, urllib.request, uuid
from decimal import Decimal

VERSION = '2026-07-28'
META = {'io.modelcontextprotocol/protocolVersion': VERSION,
        'io.modelcontextprotocol/clientInfo': {'name': 'mulino-s0-wire-probe', 'version': '1.0.0'},
        'io.modelcontextprotocol/clientCapabilities': {}}

class Probe:
    def __init__(self, url, token):
        self.url, self.token, self.rows, self.checks = url, token, [], []

    def request(self, label, method='server/discover', params=None, headers=None,
                http_method='POST', authenticated=True, raw=None, with_meta=True):
        request_id = str(uuid.uuid4())
        p = dict(params or {})
        if with_meta:
            p.setdefault('_meta', dict(META))
        body = {'jsonrpc': '2.0', 'id': request_id, 'method': method, 'params': p}
        h = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream',
             'MCP-Protocol-Version': p.get('_meta', {}).get('io.modelcontextprotocol/protocolVersion', VERSION),
             'Mcp-Method': method}
        if method == 'tools/call':
            h['Mcp-Name'] = p.get('name', '')
        h.update(headers or {})
        h = {k: v for k, v in h.items() if v is not None}
        persisted_headers = dict(h)
        if authenticated:
            h['Authorization'] = 'Bearer ' + self.token
            persisted_headers['Authorization'] = '[REDACTED_SECRET]'
        wire = raw if raw is not None else json.dumps(body, ensure_ascii=False, separators=(',', ':')).encode()
        req = urllib.request.Request(self.url, data=wire if http_method == 'POST' else None,
                                     headers=h, method=http_method)
        try:
            response = urllib.request.urlopen(req, timeout=15)
        except urllib.error.HTTPError as e:
            response = e
        data = response.read().decode('utf-8')
        response_headers = dict(response.headers.items())
        # Authentication material never enters an artifact, even if a server echoes it.
        if self.token in data or any(self.token in v for v in response_headers.values()):
            raise RuntimeError('Server echoed a credential; refused artifact persistence')
        try:
            parsed = json.loads(data)
        except json.JSONDecodeError:
            parsed = None
        row = {'case': label, 'request': {'httpMethod': http_method, 'url': self.url,
               'headers': persisted_headers, 'body': body if raw is None else None,
               'rawBody': wire.decode() if http_method == 'POST' else ''},
               'response': {'status': response.status, 'headers': response_headers,
                            'rawBody': data, 'body': parsed}}
        self.rows.append(row)
        self.check(label + ':no-session', not any(k.lower() == 'mcp-session-id' for k in response_headers))
        return row

    def check(self, label, success):
        self.checks.append({'assertion': label, 'status': 'PASS' if success else 'FAIL'})

    def result(self, row):
        b = row['response']['body'] or {}
        self.check(row['case'] + ':json-response', row['response']['status'] == 200 and
                   row['response']['headers'].get('Content-Type', '').startswith('application/json'))
        self.check(row['case'] + ':rpc-id', b.get('id') == row['request']['body']['id'] and b.get('jsonrpc') == '2.0')
        self.check(row['case'] + ':complete', b.get('result', {}).get('resultType') == 'complete')
        return b.get('result', {})

    def error(self, row, status, code=None):
        self.check(row['case'] + ':http', row['response']['status'] == status)
        if code is not None:
            b = row['response']['body'] or {}
            self.check(row['case'] + ':error-code', b.get('error', {}).get('code') == code)
            self.check(row['case'] + ':rpc-id', b.get('id') == row['request']['body']['id'])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--url', default='http://127.0.0.1:8080/mcp')
    ap.add_argument('--scope-id', required=True)
    ap.add_argument('--revision', type=int, default=0)
    ap.add_argument('--quantity', default='1')
    ap.add_argument('--output', type=pathlib.Path, required=True)
    args = ap.parse_args()
    token = os.environ.get('MCP_TOKEN')
    if not token:
        ap.error('MCP_TOKEN environment variable required; do not pass secrets on command line')
    probe = Probe(args.url, token)
    discovery = probe.result(probe.request('discover'))
    probe.check('discover:supportedVersions', discovery.get('supportedVersions') == [VERSION])
    probe.check('discover:tools-capability', 'tools' in discovery.get('capabilities', {}))
    server_info = discovery.get('_meta', {}).get('io.modelcontextprotocol/serverInfo', {})
    probe.check('discover:server-info', bool(server_info.get('name')) and bool(server_info.get('version')))
    tools = probe.result(probe.request('list', 'tools/list')).get('tools', [])
    probe.check('list:minimum-tools', {'platform.readScope', 'platform.reserve'}.issubset({t.get('name') for t in tools}))
    for tool in tools:
        probe.check('schema:' + tool.get('name', '?'), isinstance(tool.get('inputSchema'), dict))
        expected = {'scopeId': 'string'}
        if tool.get('name') == 'platform.reserve':
            expected.update(quantity='string', expectedRevision='integer', idempotencyKey='string')
        schema = tool.get('inputSchema', {})
        probe.check('schema-required:' + tool.get('name', '?'), set(schema.get('required', [])) == set(expected))
        probe.check('schema-types:' + tool.get('name', '?'), all(schema.get('properties', {}).get(k, {}).get('type') == v for k,v in expected.items()))
    read_args = {'name': 'platform.readScope', 'arguments': {'scopeId': args.scope_id}}
    before = probe.result(probe.request('read-before', 'tools/call', read_args))
    key = 's0-wire-' + str(uuid.uuid4())
    command = {'name': 'platform.reserve', 'arguments': {'scopeId': args.scope_id,
               'quantity': args.quantity, 'expectedRevision': args.revision, 'idempotencyKey': key}}
    first = probe.result(probe.request('reserve', 'tools/call', command))
    replay = probe.result(probe.request('reserve-retry-fresh-rpc-id', 'tools/call', command))
    probe.check('reserve:success', first.get('isError') is False and isinstance(first.get('structuredContent'), dict))
    probe.check('retry:same-domain-result', first.get('structuredContent') == replay.get('structuredContent'))
    probe.check('reserve:accepted-outcome', first.get('structuredContent', {}).get('outcome') == 'ACCEPTED')
    probe.check('retry:no-error', replay.get('isError') is False)
    after = probe.result(probe.request('read-after', 'tools/call', read_args))
    probe.check('read:structured', isinstance(before.get('structuredContent'), dict) and isinstance(after.get('structuredContent'), dict))
    b, a = before.get('structuredContent', {}), after.get('structuredContent', {})
    probe.check('reserve:single-revision', b.get('revision') == args.revision and a.get('revision') == args.revision + 1)
    probe.check('reserve:held-unchanged', b.get('quantity') == a.get('quantity'))
    try:
        delta_ok = Decimal(str(a.get('reserved'))) - Decimal(str(b.get('reserved'))) == Decimal(args.quantity)
    except Exception:
        delta_ok = False
    probe.check('reserve:single-quantity-effect', delta_ok)
    changed = json.loads(json.dumps(command)); changed['arguments']['quantity'] = '2' if args.quantity != '2' else '3'
    conflict = probe.result(probe.request('same-key-different-intent', 'tools/call', changed))
    probe.check('conflict:tool-error', conflict.get('isError') is True and bool(conflict.get('structuredContent')))
    probe.check('conflict:domain-outcome', conflict.get('structuredContent', {}).get('outcome') == 'IDEMPOTENCY_CONFLICT')
    probe.error(probe.request('missing-auth', authenticated=False), 401)
    probe.error(probe.request('foreign-origin', headers={'Origin': 'https://attacker.invalid'}), 403)
    probe.error(probe.request('get-not-supported', http_method='GET'), 405)
    probe.error(probe.request('delete-not-supported', http_method='DELETE'), 405)
    probe.error(probe.request('unsupported-rpc', 'mulino/unknown'), 404, -32601)
    probe.error(probe.request('legacy-initialize-rejected', 'initialize'), 404, -32601)
    unsupported_meta = dict(META); unsupported_meta['io.modelcontextprotocol/protocolVersion'] = '1900-01-01'
    unsupported = probe.request('unsupported-version', params={'_meta': unsupported_meta})
    probe.error(unsupported, 400, -32022)
    probe.check('unsupported:versions', (unsupported['response']['body'] or {}).get('error', {}).get('data', {}).get('supported') == [VERSION])
    for label, override in [('missing-version-header', {'MCP-Protocol-Version': None}),
                            ('missing-method-header', {'Mcp-Method': None}),
                            ('mismatched-method', {'Mcp-Method': 'tools/list'}),
                            ('mismatched-version', {'MCP-Protocol-Version': '2025-11-25'})]:
        probe.error(probe.request(label, headers=override), 400, -32020)
    for label, override in [('missing-name-header', {'Mcp-Name': None}),
                            ('mismatched-name', {'Mcp-Name': 'platform.reserve'})]:
        probe.error(probe.request(label, 'tools/call', read_args, headers=override), 400, -32020)
    encoded = '=?base64?' + base64.b64encode(b'platform.readScope').decode() + '?='
    late_read = probe.result(probe.request('encoded-name', 'tools/call', read_args, headers={'Mcp-Name': encoded}))
    probe.check('rejections:no-later-domain-effect', late_read.get('structuredContent') == after.get('structuredContent'))
    probe.result(probe.request('obsolete-session-ignored', headers={'Mcp-Session-Id': 'obsolete', 'Last-Event-ID': 'obsolete'}))
    probe.result(probe.request('client-info-optional', params={'_meta': {k:v for k,v in META.items() if not k.endswith('/clientInfo')}}))
    probe.error(probe.request('missing-body-meta', with_meta=False), 400)
    no_caps = dict(META); no_caps.pop('io.modelcontextprotocol/clientCapabilities')
    probe.error(probe.request('missing-client-capabilities', params={'_meta': no_caps}), 400)
    probe.error(probe.request('wrong-content-type', headers={'Content-Type': 'text/plain'}), 415)
    probe.error(probe.request('missing-accept', headers={'Accept': None}), 406)
    probe.error(probe.request('malformed-json', raw=b'{'), 400)
    probe.error(probe.request('batch-rejected', raw=b'[]'), 400)
    fail = any(c['status'] == 'FAIL' for c in probe.checks)
    artifact = {'schemaVersion': '1.0.0', 'scope': 'S0 minimum custom MCP wire; not full T20/S5',
                'protocolVersion': VERSION, 'client': {'name': 'mulino-s0-wire-probe', 'version': '1.0.0',
                'runtime': platform.python_version(), 'sdk': None},
                'timestamp': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'status': 'FAIL' if fail else 'PASS', 'assertions': probe.checks, 'transcript': probe.rows}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    payload = (json.dumps(artifact, ensure_ascii=False, indent=2) + '\n').encode()
    args.output.write_bytes(payload)
    args.output.with_suffix(args.output.suffix + '.sha256').write_text(hashlib.sha256(payload).hexdigest() + '  ' + args.output.name + '\n')
    print(json.dumps({'status': artifact['status'], 'assertions': len(probe.checks),
                      'failures': [c for c in probe.checks if c['status'] == 'FAIL'], 'artifact': str(args.output)}))
    return 1 if fail else 0

if __name__ == '__main__':
    sys.exit(main())
