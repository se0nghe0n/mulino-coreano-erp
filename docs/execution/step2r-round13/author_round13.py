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
- C3 fixtures (item 4 of the round): per fixture, an alias that no C3 or V4 subcase installing that fixture names in
  its actions or assertions, and that no other fixture fact names, is removed with its prior facts when the installer
  cannot install its type (PurchaseOrder, Invoice, Recall, Shipment...) or when it states no fact at all (Q60, B20,
  WORK-B, ALLOCATION2, and WORK2 outside the transferObligation fixture); removal repeats until nothing changes. The
  used ones that stated no fact receive the relational facts their neighbours already imply (DISPATCH of
  ALLOCATION/A20, CARGO of SHIPMENT, HOLD on A20, EXTERNAL-OP of PROPOSAL, OCCURRENCE of the receipt event, LEASE of
  WORK) so a Step 3 installer type has its inputs; installing them remains a Step 3 finding. The base fixture.json of
  the model-query subcases keeps every alias any C3 or V4 subcase names (a model reads that world through questions,
  not alias references). A Work stated only as
  prior facts (WORK2, the transferObligation source work) gets the baseline works entry the installer reads
  (ScenarioFixtureInstaller.works reads baseline works/work only), with the item of its responsibility scope.
Idempotent: a second run changes nothing (verification/requirements/test_case_generators_reproduce.py).
Run: python3 -I docs/execution/step2r-round13/author_round13.py [--check]
"""
import copy
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


def fixture_uses():
    """fixtureRef -> aliases the C3 and V4 subcases installing that fixture name in their actions and assertions."""
    uses = {}
    for case in ('C3', 'V4'):
        for sub in json.loads((ROOT / f'verification/cases/{case}/case.json').read_text())['subcases']:
            refs = {sub.get('fixtureRef')} | {a['fixtureRef'] for a in R12.RC.iter_actions(sub.get('actions', [])) if a.get('fixtureRef')}
            names = R12.RC.aliases_in({'actions': sub.get('actions', []), 'assertions': sub.get('assertions', [])})
            for ref in refs - {None}:
                uses.setdefault(ref, set()).update(names)
    return uses


ACTOR_TYPES = {'Organization', 'Human', 'Agent'}


def referenced(data, prior):
    """Strings the fixture states outside its alias table (baseline, actors, evidence, responsibilities, prior facts)."""
    inside = set()
    for key, value in data.items():
        if key != 'aliases':
            inside |= set(strings(value if key != 'baseline' else {k: v for k, v in value.items() if k != 'priorEntities'}))
    for facts in prior.values():
        inside |= set(strings(facts))
    return inside


def work_entry(name, facts, data):
    """A baseline works entry for a Work stated only as prior facts (the installer reads baseline works/work only)."""
    items = [r['scope']['itemAlias'] for r in data.get('responsibilities') or []
             if (r.get('scope') or {}).get('workAlias') == name and isinstance(r['scope'].get('itemAlias'), str)]
    org = data['aliases'][name].get('organizationAlias')
    if not items:
        items = [a for a, v in data['aliases'].items() if v.get('type') == 'TradeItem' and v.get('organizationAlias') == org]
    if len(set(items)) != 1:
        return None
    entry = {'alias': name, 'itemAlias': items[0]}
    for key in ('revision', 'state', 'ownerAlias', 'supervisorAlias', 'definitionVersion', 'goal'):
        if key in facts:
            entry[key] = copy.deepcopy(facts[key])
    return entry


def edit_c3_aliases(changed, check):
    uses = fixture_uses()
    for path in sorted((ROOT / 'verification/cases/C3').glob('fixture*.json')):
        ref = str(path.relative_to(ROOT))
        data, fmt, text = R12.load(path)
        aliases = data.get('aliases') or {}
        baseline = data.setdefault('baseline', {})
        prior = baseline.get('priorEntities') or {}
        # The base world of the model-query subcases is read by a model through natural-language questions, so it keeps
        # every alias any C3 or V4 subcase names (the case-wide rule), not only the ones its own subcases reference.
        used = set().union(*uses.values()) if path.name == 'fixture.json' else uses.get(ref, set())
        while True:
            inside = referenced(data, prior)
            drop = [n for n, a in aliases.items() if a.get('type') not in ACTOR_TYPES and n not in used and n not in inside
                    and (a.get('type') not in INSTALLABLE or n not in prior)]
            if not drop:
                break
            for name in drop:
                aliases.pop(name)
                prior.pop(name, None)
        for name, facts in C3_FACTS.items():
            if name in aliases and name not in prior:
                prior[name] = dict(facts)
        stated = {r.get('alias') for key in ('works', 'work') for r in (baseline.get(key) if isinstance(baseline.get(key), list)
                  else [baseline.get(key)] if isinstance(baseline.get(key), dict) else [])}
        for name, a in aliases.items():
            if a.get('type') == 'Work' and name in prior and name not in stated:
                entry = work_entry(name, prior[name], data)
                if entry:
                    baseline.setdefault('works', []).append(entry)
        new = R12.render(data, fmt)
        if new != text:
            changed.append(ref)
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
