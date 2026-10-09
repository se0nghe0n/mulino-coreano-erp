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
- C3 fixtures (item 4 of the round): transaction aliases the installer cannot install (no installer type) and that
  no C3 or V4 action, assertion or other fixture fact names are removed with their prior facts (PO2, PO-LINE,
  PO2-LINE, INBOX). The used ones that stated no fact receive the relational facts their neighbours already
  imply (DISPATCH of ALLOCATION/A20, CARGO of SHIPMENT, HOLD on A20, EXTERNAL-OP of PROPOSAL, OCCURRENCE of the
  receipt event, LEASE of WORK) so a Step 3 installer type has its inputs; installing them remains a Step 3
  finding.
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


# Installer alias types (ScenarioFixtureInstaller.SUPPORTED_TYPES); other C3 aliases are transaction state the
# installer cannot write yet.
INSTALLABLE = {'Organization', 'Human', 'Agent', 'TradeItem', 'Product', 'Manufacturer', 'ManufacturerLot', 'ManufacturingLot',
               'Place', 'Customer', 'Supplier', 'QuantitySegment', 'Work', 'DocumentVersion', 'DefinitionVersion',
               'PolicyVersion', 'SalesOrder', 'SalesOrderLine', 'Allocation'}
C3_FACTS = {
    'DISPATCH': {'revision': 1, 'state': 'DISPATCHED', 'allocationAlias': 'ALLOCATION', 'segmentAlias': 'A20', 'quantity': '20', 'unit': 'BOX'},
    'CARGO': {'revision': 1, 'shipmentAlias': 'SHIPMENT', 'itemAlias': 'P', 'quantity': '20', 'unit': 'BOX'},
    'HOLD': {'revision': 1, 'status': 'ACTIVE', 'scopeSegmentAlias': 'A20', 'action': 'SELL', 'category': 'QC', 'decisionOwnerAlias': 'qc'},
    'EXTERNAL-OP': {'revision': 1, 'state': 'PENDING', 'operation': 'dispatchPurchaseOrder', 'proposalAlias': 'PROPOSAL'},
    'OCCURRENCE': {'revision': 1, 'kind': 'PHYSICAL_RECEIPT', 'itemAlias': 'P', 'lotAlias': 'L', 'placeAlias': 'W', 'quantity': '20', 'unit': 'BOX'},
    'LEASE': {'revision': 1, 'state': 'ACTIVE', 'workAlias': 'WORK', 'holderAlias': 'reader'},
}


def strings(node):
    if isinstance(node, dict):
        for value in node.values():
            yield from strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from strings(value)
    elif isinstance(node, str):
        yield node


def case_aliases():
    names = set()
    for case in ('C3', 'V4'):
        names |= R12.RC.aliases_in(json.loads((ROOT / f'verification/cases/{case}/case.json').read_text()))
    return names


def edit_c3_aliases(changed, check):
    used_by_cases = case_aliases()
    for path in sorted((ROOT / 'verification/cases/C3').glob('fixture*.json')):
        data, fmt, text = R12.load(path)
        aliases, prior = data.get('aliases') or {}, (data.get('baseline') or {}).get('priorEntities') or {}
        inside = set()
        for key, value in data.items():
            if key != 'aliases':
                for s in strings(value if key != 'baseline' else {k: v for k, v in value.items() if k != 'priorEntities'}):
                    inside.add(s)
        for name, facts in prior.items():
            inside |= {s for s in strings(facts)}
        for name in [n for n, a in aliases.items() if a.get('type') not in INSTALLABLE and n not in used_by_cases and n not in inside]:
            aliases.pop(name)
            prior.pop(name, None)
        for name, facts in C3_FACTS.items():
            if name in aliases and name not in prior:
                prior[name] = dict(facts)
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
    edit_c3_aliases(changed, check)
    if check and changed:
        sys.exit('round 13 decisions not applied: ' + ', '.join(changed))
    print(('CURRENT' if check else 'WRITTEN') + f' round 13: {len(changed)} file(s) changed')


if __name__ == '__main__':
    main()
