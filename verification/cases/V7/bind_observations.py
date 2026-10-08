#!/usr/bin/env python3
"""Regenerate or check observation-bindings.json of V4, V6 or V7 from case.json.

The bindings file is derived data: every catalog observation of the case maps to the
concrete subcase/assertion that references it, and each fixed-quantity primary keeps
pointing at the same subcase/assertion ID after assertions move. Hand edits are drift.

Usage: python3 -I verification/cases/V7/bind_observations.py V4|V6|V7 [--check]
Before this script existed the output reproduced the committed V4/V6/V7 files byte-for-byte."""
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
CATALOG = ROOT / 'verification/requirements/mandatory-oracles.json'
CASES = ('V4', 'V6', 'V7')


def build(cid):
    here = ROOT / 'verification/cases' / cid
    case_path = here / 'case.json'
    case = json.loads(case_path.read_text())
    catalog = json.loads(CATALOG.read_text())
    previous = json.loads((here / 'observation-bindings.json').read_text())
    bindings, positions = {}, {}
    for i, sub in enumerate(case['subcases']):
        for j, a in enumerate(sub['assertions']):
            pointer = f'/subcases/{i}/assertions/{j}'
            positions[(sub['id'], a['id'])] = (pointer, a)
            for name in a['oracleRef']['observationNames']:
                bindings.setdefault((a['oracleRef']['oracleId'], name), []).append({
                    'subcaseId': sub['id'], 'assertionId': a['id'], 'pointer': pointer,
                    'actionId': a['source']['actionId'], 'op': a['op']})
    primaries = {(o['oracleId'], o['observationName']): o.get('primaryFixedQuantityAssertions', [])
                 for o in previous['observations']}
    observations = []
    for oracle in catalog['oracles']:
        if oracle['caseId'] != cid:
            continue
        for obs in oracle['expectedObservations']:
            key = (oracle['oracleId'], obs['name'])
            if key not in bindings:
                sys.exit(f'unlinked observation {key}')
            kept = []
            for p in primaries.get(key, []):
                if (p['subcaseId'], p['assertionId']) not in positions:
                    sys.exit(f'primary assertion vanished: {p}')
                pointer, a = positions[(p['subcaseId'], p['assertionId'])]
                kept.append({**p, 'pointer': pointer, 'actionId': a['source']['actionId'], 'op': a['op']})
            observations.append({'oracleId': key[0], 'observationName': key[1], 'runtimeStatus': 'NOT_RUN',
                                 'primaryFixedQuantityAssertions': kept, 'bindings': bindings[key]})
    return {'recordType': 'PREPARED_NORMATIVE_OBSERVATION_BINDINGS', 'caseId': cid,
            'caseHash': hashlib.sha256(case_path.read_bytes()).hexdigest(),
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
        return '[\n' + ''.join(inner + _compact(v) + (',' if i < len(value) - 1 else '') + '\n'
                               for i, v in enumerate(value)) + pad + ']'
    return json.dumps(value, ensure_ascii=False)


def render(data):
    return _render(data, 0) + '\n'


if __name__ == '__main__':
    args = sys.argv[1:]
    if not args or args[0] not in CASES or args[1:] not in ([], ['--check']):
        sys.exit(__doc__)
    cid = args[0]
    out = ROOT / 'verification/cases' / cid / 'observation-bindings.json'
    text = render(build(cid))
    if args[1:] == ['--check']:
        if out.read_text() != text:
            sys.exit(f'{cid} observation-bindings.json drifted from case.json; rerun bind_observations.py {cid}')
        print(f'{cid} observation bindings: CURRENT')
    else:
        out.write_text(text)
        print(f'{cid} observation bindings: WRITTEN')
