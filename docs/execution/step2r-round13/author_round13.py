#!/usr/bin/env python3
"""Step 2 round 13: hand-maintained case and fixture decisions the shared request-contract rules cannot make.

Run-driven (AGENTS.md 2026-10-09): docs/execution/s5b-adapter/run-85ce2365.json stopped 629 subcases on a
baseline read refused FORBIDDEN, because one fixture grant named items, works, places and segments at once and
the product authorizes a grant only when every named dimension matches (all-of, the user's decision). The
general rules live in verification/cases/request_contract.py and contracts/request-contracts.json
(grantAuthorization); every generator and docs/execution/step2r-round12/author_round12.py apply them. This
script holds the per-case decisions the static grant check (check_request_contracts.py) exposed once the
grants were all-of:

- T02 period-boundary-nonoverlap: the steward links the issuer code to P2 in the second period, yet its grant
  listed P, P_EA and P_BUNDLE only. The second link is expected to apply, so P2 joins the steward's items.
- T07 immutable-authorized-download: the outsider's grant named an empty evidenceIds list, which installs as an
  organization-wide grant (no scope row), so the download it must be refused was authorized. The outsider keeps
  getEvidence as a role capability but its current grant no longer delegates it (plan section 7.1: the current
  grant, not the role, authorizes).
- T08 payload-org and payload-actor: the attack body carries organizationId/actorId (and delegatorId/role) that
  contracts/intent.schema.json does not allow. Plan section 3.3 defines the request by that schema, takes the
  requester and delegation from the authenticated context, and orders validation as required slots, type,
  unit, cardinality, supported effect, then applicable policy; section 3.4 lists TYPE_INVALID for such an
  envelope. The product's TYPE_INVALID before authorization follows that order, so the code oracle becomes
  TYPE_INVALID; the zero-effect, no-foreign-data and REJECTED oracles stay.
Idempotent: a second run changes nothing (verification/requirements/test_case_generators_reproduce.py).
Run: python3 -I docs/execution/step2r-round13/author_round13.py [--check]
"""
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
_r12 = importlib.util.spec_from_file_location('author_round12', ROOT / 'docs/execution/step2r-round12/author_round12.py')
R12 = importlib.util.module_from_spec(_r12)
_r12.loader.exec_module(R12)

FIXTURE_EDITS = {
    'verification/cases/T02/fixtures/period-boundary-nonoverlap.json': ('steward', 'items', 'P2'),
}
GRANT_ACTION_DROPS = {
    'verification/cases/T07/fixtures/immutable-authorized-download.json': ('outsider', 'getEvidence'),
}
# (subcase, action, assertion) -> expected value
T08_CODES = {
    ('payload-org', 'attack', 'attack-code'): 'TYPE_INVALID',
    ('payload-org', 'attack', 'no-identity-error-details'): {'code': 'TYPE_INVALID'},
    ('payload-actor', 'attack', 'attack-code'): 'TYPE_INVALID',
}


def edit_fixtures(changed, check):
    for ref, (actor, key, value) in FIXTURE_EDITS.items():
        path = ROOT / ref
        data, fmt, text = R12.load(path)
        scope = data['actors'][actor]['grant']['scope']
        if value not in scope.setdefault(key, []):
            scope[key].append(value)
        new = R12.render(data, fmt)
        if new != text:
            changed.append(ref)
            if not check:
                path.write_text(new)
    for ref, (actor, capability) in GRANT_ACTION_DROPS.items():
        path = ROOT / ref
        data, fmt, text = R12.load(path)
        grant = data['actors'][actor]['grant']
        grant['actions'] = [a for a in grant['actions'] if a != capability]
        new = R12.render(data, fmt)
        if new != text:
            changed.append(ref)
            if not check:
                path.write_text(new)


def edit_t08(changed, check):
    path = ROOT / 'verification/cases/T08/case.json'
    data, fmt, text = R12.load(path)
    for sub in data['subcases']:
        for assertion in sub['assertions']:
            key = (sub['id'], assertion['source'].get('actionId'), assertion['id'])
            if key in T08_CODES:
                assertion['expected'] = T08_CODES[key]
    new = R12.render(data, fmt)
    if new != text:
        changed.append(str(path.relative_to(ROOT)))
        if not check:
            path.write_text(new)


def main():
    check = sys.argv[1:] == ['--check']
    changed = []
    edit_fixtures(changed, check)
    edit_t08(changed, check)
    if check and changed:
        sys.exit('round 13 decisions not applied: ' + ', '.join(changed))
    print(('CURRENT' if check else 'WRITTEN') + f' round 13: {len(changed)} file(s) changed')


if __name__ == '__main__':
    main()
