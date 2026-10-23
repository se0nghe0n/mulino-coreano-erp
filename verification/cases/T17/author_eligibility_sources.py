#!/usr/bin/env python3
"""원래 T17 given에 전용 source actor/Work/fictional policy만 보충한다."""
import json
from pathlib import Path
HERE = Path(__file__).resolve().parent
ACTOR = 't17Source'
NAMESPACE = 'synthetic-t17-eligibility'
CAPABILITIES = ['attachEvidence', 'recordActivity', 'matchSourceIdentity',
                'linkCanonicalOccurrence', 'getEvidence', 'recordDispositionBasis',
                'decideQuality', 'confirmCustomerConditions', 'confirmCommercialDisposition',
                'prepareRegulatoryProcedure', 'recordSubmission', 'recordRegulatoryDecision', 'verifyLabel']


def add_equal(mapping, key, value):
    if key in mapping and mapping[key] != value:
        raise ValueError('Conflicting source authoring: ' + key)
    mapping[key] = value


def apply(f):
    if not f['fixtureId'].startswith('T17-') or not f['synthetic']:
        raise ValueError('Only synthetic T17 given facts')
    segments = sorted({x['segmentAlias'] for x in f['baseline']['conditions']})
    items = sorted({f['aliases'][s]['itemAlias'] for s in segments})
    template = f['actors']['intake']
    actor = {k: template[k] for k in ['issuer', 'audience', 'organizationAlias']}
    actor.update(subject=f['fixtureId'] + '-source', roleCapabilities=CAPABILITIES,
                 grant={'delegatorAlias': 'supervisor', 'actions': CAPABILITIES,
                        'scope': {'organizationAlias': template['organizationAlias'], 'itemAliases': items},
                        'validFrom': template['grant']['validFrom'], 'validUntil': template['grant']['validUntil'], 'revision': 1})
    add_equal(f['actors'], ACTOR, actor)
    add_equal(f['aliases'], ACTOR, {'type': 'Human'})
    works = f['baseline'].setdefault('works', [])
    for segment in segments:
        alias = 't17SourceWork-' + segment
        add_equal(f['aliases'], alias, {'type': 'Work'})
        spec = {'alias': alias, 'itemAlias': f['aliases'][segment]['itemAlias'],
                'lotAlias': f['aliases'][segment]['lotAlias'], 'ownerAlias': ACTOR,
                'supervisorAlias': 'supervisor', 'kind': 'QUALITY', 'status': 'ACTIVE', 'revision': 1}
        old = next((w for w in works if w['alias'] == alias), None)
        if old is not None and old != spec: raise ValueError('Conflicting source Work')
        if old is None: works.append(spec)
    conditions = f['baseline']['conditions']
    policy = {'type': 'RegulatoryPolicy', 'actorAlias': ACTOR, 'version': 't17-synthetic-v1',
              'authority': 'FICTIONAL_T17_GIVEN_AUTHORITY', 'sourceNamespace': NAMESPACE,
              'action': 'SELL', 'validFrom': min(x['validFrom'] for x in conditions),
              'validUntil': max(x['validUntil'] for x in conditions),
              'fictional': True, 'status': 'ACTIVE', 'requiresLabel': True}
    add_equal(f['aliases'], 't17RegulatoryPolicy', policy)
    profile = {'synthetic': True, 'sourceNamespace': NAMESPACE, 'namespace': NAMESPACE,
               'version': 't17-synthetic-v1', 'policyVersion': 't17-synthetic-v1',
               'reconciliationOwnerAlias': ACTOR, 'acceptedActorAliases': [ACTOR],
               'eventKinds': ['QUALITY_DECISION_recordDispositionBasis_QC',
                              'QUALITY_DECISION_recordDispositionBasis_CUSTOMER',
                              'QUALITY_DECISION_recordDispositionBasis_COMMERCIAL',
                              'REGULATORY_SUBMISSION', 'REGULATORY_DECISION', 'LABEL_VERIFICATION']}
    profiles = f['baseline']['sourceProfiles']
    old = next((p for p in profiles if p['sourceNamespace'] == NAMESPACE), None)
    if old is not None and old != profile: raise ValueError('Conflicting source profile')
    if old is None: profiles.append(profile)
    add_equal(f['baseline'], 't17SourceFacts', {'version': 1, 'actorAlias': ACTOR,
        'namespace': NAMESPACE, 'policyAlias': 't17RegulatoryPolicy', 'legalComplianceClaimed': False,
        'workAliases': {s: 't17SourceWork-' + s for s in segments}})


if __name__ == '__main__':
    changed = 0
    for path in sorted((HERE / 'fixtures').glob('*.json')):
        f = json.loads(path.read_text()); before = json.dumps(f, ensure_ascii=False, indent=2) + '\n'
        apply(f); after = json.dumps(f, ensure_ascii=False, indent=2) + '\n'
        if after != before: path.write_text(after); changed += 1
    print(f'T17 source input authoring: {changed} fixtures changed')
