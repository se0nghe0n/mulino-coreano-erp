#!/usr/bin/env python3
"""Scoped V5/T26 runtime input repairs; preserve every business assertion.

A rolled-back move has no successful response revision. The generic command
schema makes expectedRevision optional; retrySafeCommand has no implemented
handler/revision contract. Remove that invalid outer reference without inventing
a revision or a stored command ID. Its unresolved command ID remains a visible
product-contract blocker. Split children are explicitly ordered by their index.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def apply(case):
    changes = 0
    if case['caseId'] not in ('V5', 'T26'):
        return changes
    for sub in case.get('subcases', []):
        for action in sub.get('actions', []):
            request = action.get('request', {})
            if action.get('capabilityId') == 'retrySafeCommand':
                ref = request.get('expectedRevision', {}).get('$result', {}) if isinstance(request.get('expectedRevision'), dict) else {}
                if ref == {'actionId': 'original', 'pointer': '/response/revision'}:
                    del request['expectedRevision']
                    changes += 1
            def visit(node):
                nonlocal changes
                if isinstance(node, dict):
                    if node.get('$result') == {'actionId': 'split', 'pointer': '/response/segmentId'}:
                        node['$result']['pointer'] = '/response/children/0/segmentId'
                        changes += 1
                    for child in node.values():
                        visit(child)
                elif isinstance(node, list):
                    for child in node:
                        visit(child)
            visit(request)
    return changes


def main():
    for cid in ('V5', 'T26'):
        path = ROOT / f'verification/cases/{cid}/case.json'
        case = json.loads(path.read_text())
        changes = apply(case)
        if changes:
            path.write_text(json.dumps(case, ensure_ascii=False, indent=2) + '\n')
        print(cid, changes)


if __name__ == '__main__':
    main()
