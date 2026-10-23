#!/usr/bin/env python3
"""T17의 기존 source 선언을 실제 installer 입력에 연결한다.

원천 namespace, 대조 책임자, 정책 version만 명시한다. 업무 권한이나
ALLOWED/VERIFIED 사실을 만들지 않으며 조건/실물 선언은 그대로 둔다.
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIELDS = ('namespace', 'reconciliationOwnerAlias', 'policyVersion')


def apply(fixture):
    if not fixture['fixtureId'].startswith('T17-') or fixture.get('synthetic') is not True:
        raise ValueError('Synthetic T17 fixture required')
    owner = fixture['actors']['intake']
    if not {'matchSourceIdentity', 'linkCanonicalOccurrence'} <= set(owner['roleCapabilities']):
        raise ValueError('Existing source reconciliation owner required')
    changed = 0
    seen = set()
    for profile in fixture['baseline']['sourceProfiles']:
        # The separately authored typed-source actor owns this explicit new profile.
        if profile['sourceNamespace'] == 'synthetic-t17-eligibility':
            continue
        namespace = profile['sourceNamespace']
        if namespace in seen or profile.get('synthetic') is not True:
            raise ValueError('Distinct synthetic source namespace required')
        seen.add(namespace)
        if 'intake' not in profile['acceptedActorAliases']:
            raise ValueError('Authored source must accept the existing intake owner')
        values = (namespace, 'intake', profile['version'])
        for key, value in zip(FIELDS, values):
            if key in profile and profile[key] != value:
                raise ValueError(f'Conflicting source profile {namespace}: {key}')
            if key not in profile:
                profile[key] = value
                changed += 1
    return changed


def main():
    changed = profiles = 0
    for path in sorted((HERE / 'fixtures').glob('*.json')):
        fixture = json.loads(path.read_text())
        count = apply(fixture)
        if count:
            path.write_text(json.dumps(fixture, ensure_ascii=False, indent=2) + '\n')
        changed += count
        profiles += len(fixture['baseline']['sourceProfiles'])
    print(f'T17 source profiles: {profiles} profiles, {changed} fields authored')


if __name__ == '__main__':
    main()
