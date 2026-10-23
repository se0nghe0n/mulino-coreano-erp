#!/usr/bin/env python3
"""T13의 기존 synthetic MANAGER에게 품목 한정 구매 결정 권한을 설치한다.

PurchaseCommands.manager는 역할/grant 외에 ManagementAuthorities를 요구한다.
ScenarioFixtureInstaller는 baseline.managementAuthorities만 해당 행으로 설치한다.
기존 supervisor delegator는 installer가 이미 생성하므로 변경하지 않는다.
case/action/oracle, actor/grant, 물리 사실은 그대로 둔다. 실제 인수는 수행하지 않는다.

Run: python3 verification/cases/T13/author_approval_scope.py
"""
import json
from pathlib import Path

DIRECTORY = Path(__file__).resolve().parent
BASELINE = '7d22ce26d60520ec787ab7404752a4867a4b8bbc'


def repair(fixture):
    manager = fixture['actors']['manager']
    grant = manager['grant']
    assert 'approvePurchase' in manager['roleCapabilities']
    assert 'approvePurchase' in grant['actions']
    # This fixture declares ITEM as its sole restrictive dimension. Do not
    # silently drop a newly authored dimension or widen to ORGANIZATION.
    scope = grant['scope']
    assert set(scope) == {'organizationAlias', 'caseId', 'subcaseId', 'items'}
    assert scope['items'] == ['P', 'P-alt']
    assert fixture['baseline']['policies']['purchaseApproval'] == 'MANAGER'
    authorities = [
        {'actorAlias': 'manager', 'capabilityId': 'approvePurchase',
         'scopeKind': 'ITEM', 'scopeAlias': item,
         'validFrom': grant['validFrom'], 'validUntil': grant['validUntil'],
         'revokedAt': None}
        for item in scope['items']
    ]
    existing = fixture['baseline'].get('managementAuthorities')
    assert existing is None or existing == authorities
    fixture['baseline']['managementAuthorities'] = authorities
    return fixture


def selected_fixtures(case):
    return [sub['fixtureRef'] for sub in case['subcases']
            if any(a.get('capabilityId') == 'approvePurchase'
                   and a.get('actorRef') == 'manager' for a in sub['actions'])]


def main():
    root = DIRECTORY.parents[2]
    case = json.loads((DIRECTORY / 'case.json').read_text())
    for reference in selected_fixtures(case):
        path = root / reference
        path.write_text(json.dumps(repair(json.loads(path.read_text())),
                                   ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
