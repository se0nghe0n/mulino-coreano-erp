#!/usr/bin/env python3
"""Independently check T01 source chains in an actual report; never emit a receipt."""
import argparse
from collections import Counter
from decimal import Decimal
import hashlib
import json
from pathlib import Path

SOURCES = ('receipt60', 'receipt40', 'qc60', 'hold40', 'disposition60',
           'customerConditions60', 'regulatorySubmission', 'regulatory60')
COUNTS = {'recordActivity': 8, 'matchSourceIdentity': 8,
          'linkCanonicalOccurrence': 8, 'attachEvidence': 1,
          'recordSubmission': 1, 'recordRegulatoryDecision': 1,
          'recordDispositionBasis': 3}


def verify(report):
    assert report['status'] == 'PASS' and report['workingTreeDirty'] is False
    cases = [case for case in report['cases'] if case['caseId'] == 'T01']
    assert len(cases) == 12
    assert sum(len(case['assertions']) for case in cases) == 125
    for case in cases:
        assert case['runtimeComplete'] and case['status'] == 'PASS', case['subcaseId']
        assert all(assertion['status'] == 'PASS' for assertion in case['assertions'])
        setup = case['actions']['setup']['data']
        aliases = setup['aliasMap']
        commands = setup['sourceFactEvidence']['commands']
        assert Counter(command['capabilityId'] for command in commands) == COUNTS
        assert all(command['httpStatus'] == 200 and command['outcome'] == 'APPLIED'
                   for command in commands)
        assert all(command['evidenceStatus'] == 'MATCHED' for command in commands
                   if command['capabilityId'] == 'matchSourceIdentity')
        assert all(command['evidenceStatus'] == 'VERIFIED_RECORD_ONLY' for command in commands
                   if command['capabilityId'] == 'linkCanonicalOccurrence')
        observation = case['actions']['db-after']['data']['rawRows']
        claims = observation['claims']
        links = observation['evidenceLinks']
        for name in SOURCES:
            source_claims = [claim for claim in claims
                             if claim['documentVersionId'] == aliases[name]]
            assert len(source_claims) == 1, (case['subcaseId'], name)
            claim = source_claims[0]
            assert claim['valueState'] == 'KNOWN'
            assert any(link.get('canonicalOccurrenceId') == aliases[name + 'Fact']
                       and link.get('documentVersionId') == aliases[name]
                       for link in links), (case['subcaseId'], name, 'canonical source link')
            assert any(link.get('claimId') == claim['id']
                       and link.get('documentVersionId') == aliases[name]
                       for link in links), (case['subcaseId'], name, 'claim source link')
        segments = observation['segments']
        assert {row['id'] for row in segments} == {aliases['A60'], aliases['B40']}
        assert sum(Decimal(row['quantity']) for row in segments) == 100
        assert all(row['lotId'] == aliases['L'] for row in segments)
        duties = observation['obligations']
        assert {(row['responsibleWorkId'], row['ownerId'], row['nextAction']) for row in duties} == {
            (aliases['O1'], aliases['reader'], '검사 근거 확인'),
            (aliases['S1'], aliases['writer'], '인도 증거 확인')}
    return len(cases)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('report', type=Path)
    args = parser.parse_args()
    raw = args.report.read_bytes()
    report = json.loads(raw)
    count = verify(report)
    print(f'T01_SOURCE_CHAINS PASS: {count} cases, 96 source chains, '
          f'commit={report["codeCommit"]}, reportSha256={hashlib.sha256(raw).hexdigest()}')


if __name__ == '__main__':
    main()
