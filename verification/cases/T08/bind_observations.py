#!/usr/bin/env python3
"""Regenerate or check observation-bindings.json from case.json.

The bindings file is derived data: every catalog observation of T08 maps to the
concrete subcase/assertion that references it. Run without arguments to rewrite
the file, or with --check to fail on drift (no hand edits)."""
import hashlib, json, pathlib, sys
HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[2]
CASE = HERE / 'case.json'
OUT = HERE / 'observation-bindings.json'
CATALOG = ROOT / 'verification/requirements/mandatory-oracles.json'

def build():
    case = json.loads(CASE.read_text())
    catalog = json.loads(CATALOG.read_text())
    previous = json.loads(OUT.read_text()) if OUT.exists() else {}
    bindings = {}
    for i, sub in enumerate(case['subcases']):
        for j, a in enumerate(sub['assertions']):
            for name in a['oracleRef']['observationNames']:
                bindings.setdefault((a['oracleRef']['oracleId'], name), []).append({
                    'subcaseId': sub['id'], 'assertionId': a['id'], 'pointer': f'/subcases/{i}/assertions/{j}',
                    'actionId': a['source']['actionId'], 'op': a['op']})
    primaries = {(o['oracleId'], o['observationName']): o.get('primaryFixedQuantityAssertions', [])
                 for o in previous.get('observations', [])}
    observations = []
    for oracle in catalog['oracles']:
        if oracle['caseId'] != 'T08':
            continue
        for obs in oracle['expectedObservations']:
            key = (oracle['oracleId'], obs['name'])
            if key not in bindings:
                sys.exit(f'unlinked observation {key}')
            observations.append({'oracleId': key[0], 'observationName': key[1], 'runtimeStatus': 'NOT_RUN',
                                 'primaryFixedQuantityAssertions': primaries.get(key, []), 'bindings': bindings[key]})
    return {'recordType': 'PREPARED_NORMATIVE_OBSERVATION_BINDINGS', 'caseId': 'T08',
            'caseHash': hashlib.sha256(CASE.read_bytes()).hexdigest(),
            'catalogSha256': hashlib.sha256(CATALOG.read_bytes()).hexdigest(),
            'productRuntimeClaimed': False, 'runtimeStatus': 'NOT_RUN', 'observations': observations}

def _compact(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))

def _render(value, indent, key=None):
    pad, inner = '  ' * indent, '  ' * (indent + 1)
    if isinstance(value, dict):
        items = [f'{inner}{json.dumps(k, ensure_ascii=False)}: {_render(v, indent + 1, k)}' for k, v in value.items()]
        return '{\n' + ',\n'.join(items) + '\n' + pad + '}'
    if isinstance(value, list):
        if key == 'observations':
            return '[\n' + ',\n'.join(inner + _render(v, indent + 1) for v in value) + '\n' + pad + ']'
        return '[\n' + ''.join(inner + _compact(v) + (',' if i < len(value) - 1 else '') + '\n' for i, v in enumerate(value)) + pad + ']'
    return json.dumps(value, ensure_ascii=False)

def render(data):
    """Same layout as the authored file: one compact binding per line."""
    return _render(data, 0) + '\n'

if __name__ == '__main__':
    text = render(build())
    if sys.argv[1:] == ['--check']:
        if OUT.read_text() != text:
            sys.exit('observation-bindings.json drifted from case.json; rerun bind_observations.py')
        print('T08 observation bindings: CURRENT')
    else:
        OUT.write_text(text)
        print('T08 observation bindings: WRITTEN')
