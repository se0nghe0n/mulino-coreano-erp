#!/usr/bin/env python3
"""Step 2 round 14: hand-maintained case and fixture decisions from the s5d adapter run (run-6b9aed36).

Run-driven (AGENTS.md 2026-10-09, 2026-10-11): docs/execution/s5d-adapter/README.md "Step 2 case defects" lists
the case defects the fourth adapter run exposed. Rules every generator applies live in
verification/cases/request_contract.py and contracts/request-contracts.json; this script holds the per-file
decisions no shared rule can make, for the files no generator owns (generator-owned cases are changed in their
generators: T01/T20 verification/mcp-tests/author_cases.py, C3 verification/cases/C3/author_prerequisites.py).

- T19 (finding 7): every fixture names OLD-WORK as the logistics Work of the historical purchase line PO-A
  (baseline.historicalFacts.purchase.logisticsWorkAlias) but states no baseline works entry, so the installer
  omits the Work and with it PO-A, and every recordInvoice on PO-A is refused FORBIDDEN. The entry takes only what
  the historical purchase states: item P (the purchase subject of every T19 invoice), the ARRIVED endpoint, 100 BOX
  at W, kind PURCHASE.
- C3 fixture.json (finding 9): the base world of the model-query subcases kept aliases that state no fact and that
  no subcase installing fixture.json names (WORK2, SHIPMENT, RETURN, DELIVERY, RECALL, INVOICE, BASIS, HANDOVER,
  REGULATORY, RECONCILIATION, CASE-COMMAND). A factless alias installs nothing, so they are removed. A factless
  alias another fact names (SHIPMENT, the shipment of CARGO) takes the facts all C3 capability fixtures agree on.
- C3 policy fixtures (finding 9): the installer installs an ACTIVE PolicyVersion only with an authored kind,
  content and effectiveFrom; the retirePolicy POLICY stated only state/hash, so its read was refused. It gets the
  fixture's own business policy as content, effective from that policy's effectiveAt. The PUBLISHED activatePolicy
  POLICY gets its approver (plan section 6/7.1 policy rows: draft, CONFIG approval, immutable publish, activation),
  the delegator who holds approvePolicy organization-wide in that fixture.
Idempotent: a second run changes nothing (verification/requirements/test_case_generators_reproduce.py).
Run: python3 -I docs/execution/step2r-round14/author_round14.py [--check]
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


def write(path, data, fmt, text, changed, check):
    new = R12.render(data, fmt)
    if new != text:
        changed.append(str(path.relative_to(ROOT)))
        if not check:
            path.write_text(new)


# ---- T19 historical purchase Work (finding 7) ---------------------------------------------------------------------------
def edit_t19_works(changed, check):
    for path in sorted((ROOT / 'verification/cases/T19/fixtures').glob('*.json')):
        data, fmt, text = R12.load(path)
        baseline = data.get('baseline') or {}
        purchase = (baseline.get('historicalFacts') or {}).get('purchase') or {}
        work = purchase.get('logisticsWorkAlias')
        if not work or (data.get('aliases') or {}).get(work, {}).get('type') != 'Work':
            continue
        works = baseline.setdefault('works', [])
        if not any(w.get('alias') == work for w in works):
            works.append({'alias': work, 'itemAlias': 'P', 'kind': 'PURCHASE',
                          'goal': {'quantity': purchase['quantity'], 'unit': purchase['unit'], 'endpoint': purchase['goalEndpoint'],
                                   'destinationAlias': purchase['placeAlias']}})
        write(path, data, fmt, text, changed, check)


# ---- C3 placeholder aliases (finding 9) ---------------------------------------------------------------------------------
ACTOR_TYPES = {'Organization', 'Human', 'Agent'}


def strings(node):
    if isinstance(node, dict):
        for value in node.values():
            yield from strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from strings(value)
    elif isinstance(node, str):
        yield node


def base_world_uses():
    """Aliases named by the C3 and V4 subcases that install the C3 base fixture.json."""
    ref = 'verification/cases/C3/fixture.json'
    names = set()
    for case in ('C3', 'V4'):
        for sub in json.loads((ROOT / f'verification/cases/{case}/case.json').read_text())['subcases']:
            refs = {sub.get('fixtureRef')} | {a['fixtureRef'] for a in R12.RC.iter_actions(sub.get('actions', [])) if a.get('fixtureRef')}
            if ref in refs:
                names |= R12.RC.aliases_in({'actions': sub.get('actions', []), 'assertions': sub.get('assertions', [])})
    return names


def edit_c3_base_world(changed, check):
    path = ROOT / 'verification/cases/C3/fixture.json'
    data, fmt, text = R12.load(path)
    aliases, baseline = data['aliases'], data.setdefault('baseline', {})
    prior = baseline.get('priorEntities') or {}
    used = base_world_uses()
    stated = {w.get('alias') for key in ('works', 'work') for w in (baseline.get(key) if isinstance(baseline.get(key), list)
              else [baseline.get(key)] if isinstance(baseline.get(key), dict) else [])}
    inside = set()
    for key, value in data.items():
        if key != 'aliases':
            inside |= set(strings(value if key != 'baseline' else {k: v for k, v in value.items() if k != 'priorEntities'}))
    for facts in prior.values():
        inside |= set(strings(facts))
    factless = [n for n, a in aliases.items() if a.get('type') not in ACTOR_TYPES and not prior.get(n) and n not in stated
                and a.get('type') not in R12_INSTALLED_MASTER]
    for name in [n for n in factless if n not in used and n not in inside]:
        aliases.pop(name)
        prior.pop(name, None)
    # A factless alias that another fact names (SHIPMENT of CARGO) takes the facts every C3 capability fixture states for
    # it, when they all agree; otherwise it stays factless and the installer reports it.
    for name in [n for n in factless if n in aliases]:
        stated_facts = {json.dumps(f, sort_keys=True) for f in (json.loads(p.read_text())['baseline'].get('priorEntities', {}).get(name)
                        for p in sorted((ROOT / 'verification/cases/C3').glob('fixture-*.json'))) if f}
        if len(stated_facts) == 1:
            baseline.setdefault('priorEntities', prior)[name] = json.loads(stated_facts.pop())
    write(path, data, fmt, text, changed, check)


# Master-data aliases the installer writes from the alias entry itself (no prior facts needed).
R12_INSTALLED_MASTER = {'TradeItem', 'Product', 'Manufacturer', 'ManufacturerLot', 'ManufacturingLot', 'Place', 'Customer', 'Supplier',
                        'QuantitySegment', 'DocumentVersion', 'DefinitionVersion', 'Grant', 'GoalVersion', 'SalesOrderLine'}


def edit_c3_policies(changed, check):
    for name in ('retirePolicy', 'activatePolicy'):
        path = ROOT / f'verification/cases/C3/fixture-{name}.json'
        data, fmt, text = R12.load(path)
        facts = data['baseline']['priorEntities']['POLICY']
        if facts.get('state') == 'ACTIVE':
            business = copy.deepcopy(data['baseline']['policies'])
            facts.setdefault('kind', 'SCENARIO_BUSINESS_POLICY')
            facts.setdefault('version', data['versions']['policy'])
            facts.setdefault('content', business)
            facts.setdefault('effectiveFrom', business['effectiveAt'])
        elif facts.get('state') == 'PUBLISHED':
            facts.setdefault('approverAlias', 'delegator')
        write(path, data, fmt, text, changed, check)


def main():
    check = sys.argv[1:] == ['--check']
    changed = []
    edit_t19_works(changed, check)
    edit_c3_base_world(changed, check)
    edit_c3_policies(changed, check)
    if check and changed:
        sys.exit('round 14 decisions not applied: ' + ', '.join(changed))
    print(('CURRENT' if check else 'WRITTEN') + f' round 14: {len(changed)} file(s) changed')


if __name__ == '__main__':
    main()
