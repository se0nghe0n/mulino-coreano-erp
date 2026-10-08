#!/usr/bin/env python3
"""Preparation-independent vocabulary check for the 41 acceptance cases.

Reads only committed files: contracts/domain-vocabulary.json (command outcomes, structured
error codes and their allowed outcomes, obligation kinds), contracts/audit-observation-fields.json
(logical audit row fields) and verification/cases/*/case.json plus every case fixture JSON.
It never runs a product or the harness and claims no product behaviour.

It rejects, in case oracles and fixtures:
  * a command outcome or structured error code outside the vocabulary,
  * an error code asserted together with an outcome the vocabulary does not allow for it,
  * an obligation kind outside the vocabulary (obligations/assignments rows, obligation
    responses, getObligations kind filters, seeded fixture duties),
  * an audit-source name that is not a field of contracts/audit-observation-fields.json, a
    where filter on a field that is not always present, an outcome written as an error code,
    or an audit source other than audit/queryAudit,
except the explicit PENDING list below. Each pending entry names the plan section that requires
the behaviour and is a vocabulary addition request for Step 3, not an accepted name. A pending
entry that no case uses any more is itself a problem, so the list cannot go stale.

Negative assertions (notEquals, absent) are not checked: forbidding a value is not using it.

Every pending entry is printed as a KNOWN_OPEN line with its owner, so ./verify prepare (caseAssetChecks
"vocabulary") copies it into knownOpenGaps instead of hiding it in the output tail; the coverage assembler
loads review() and records the same entries. review() is the single implementation both use.

Usage: python3 -I verification/cases/check_vocabulary.py [--check] [--json]
  default  print problems and KNOWN_OPEN pending entries, exit 0
  --check  exit 1 when any problem exists
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VOCAB = json.loads((ROOT / 'contracts/domain-vocabulary.json').read_text())
AUDIT = json.loads((ROOT / 'contracts/audit-observation-fields.json').read_text())

OUTCOMES = {o['outcome'] for o in VOCAB['outcomes']} | {o['outcome'] for o in VOCAB['validationOutcomes']}
CODES = {e['code']: set(e['outcomes']) for e in VOCAB['errorCodes']}
KINDS = {k['kind'] for k in VOCAB['obligationKinds']}
AUDIT_SOURCES = {s['source'] for s in AUDIT['sources']}
AUDIT_FIELDS = {}
for f in AUDIT['fields']:
    AUDIT_FIELDS.setdefault(f['source'], {})[f['name']] = f['presence']
QUERY_AUDIT_OUTCOMES = {'READ', 'REJECTED'}
PENDING_OWNER = 'Step 3 vocabulary owner (contracts/domain-vocabulary.json)'

# Vocabulary addition requests (docs/execution/step2r-cases3/README.md). key -> (cases, plan, reason)
PENDING = {
    ('code', 'APPROVAL_REQUIRED'): ('T20', '§7.1·§9.1', 'MRTR input collection is not a manager decision; WAITING_APPROVAL carries the missing approval role'),
    ('code', 'REQUEST_STATE_INTEGRITY_FAILED'): ('T20', '§9.1 MRTR', 'S5 MRTR requestState integrity binding is not implemented'),
    ('code', 'REQUEST_STATE_EXPIRED'): ('T20', '§9.1 MRTR', 'S5 MRTR requestState TTL (600s) is not implemented'),
    ('code', 'REQUEST_STATE_PRINCIPAL_MISMATCH'): ('T20', '§9.1 MRTR', 'S5 MRTR principal binding is not implemented'),
    ('code', 'REQUEST_STATE_INTENT_MISMATCH'): ('T20', '§9.1 MRTR', 'S5 MRTR intent binding is not implemented'),
    ('code', 'REQUEST_STATE_CONSUMED'): ('T20', '§9.1 MRTR·§7.1', 'S5 single-use approval state consumption is not implemented'),
    ('code', 'INPUT_RESPONSE_UNMATCHED'): ('T20', '§9.1 MRTR', 'S5 inputResponses/requestId binding is not implemented'),
    ('code', 'TRANSACTION_ROLLED_BACK'): ('T04', '§4.2·§7.4', 'a fault after a partial domain write rolls the whole transaction back; the backend has no structured code for it'),
    ('code', 'RAW_CORE_WRITE_FORBIDDEN'): ('T04', '§4.1·§4.2', 'core entities are not written directly; the backend has no structured code for a guarded primitive bypass'),
    ('code', 'DB_PRIVILEGE_DENIED'): ('T04', '§4.2', 'the application DB role cannot UPDATE core rows; the probe result needs a structured code'),
    ('code', 'GENEALOGY_CYCLE'): ('T03', '§4.2 계보 비순환', 'cycle-creating split/merge rejection has no structured code'),
    ('code', 'CARDINALITY_INVALID'): ('T21', '§8 발행 검사', 'definition validation finding codes are not in the vocabulary'),
    ('code', 'REQUIRED_STAGE_INVALID'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'UNIT_INCOMPATIBLE'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'RELATION_CYCLE'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'GOAL_INCOMPLETE'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'ORGANIZATION_SCOPE_INVALID'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'REGRESSION_FAILED'): ('T21', '§8 발행 검사', 'definition validation finding code'),
    ('code', 'AUDIT_PERSISTENCE_FAILED'): ('T24', '§7.4', 'audit write failure rolls back the business effect; the backend has no structured code for it'),
    ('pair', 'CAPABILITY_UNSUPPORTED/REJECTED'): ('T01', '§1 제외 범위·D01', 'an excluded capability (manufacture, BOM, B2C, GL, tax, bank transfer) is a permanent rejection, not a HELD responsibility; the vocabulary allows only HELD'),
    ('outcome', 'STRUCTURED'): ('T20', '§3.3', 'structureIntent effect-free structuring result; the vocabulary lists only VALIDATED for validateCommand'),
    ('kind', 'SALES_PROMISE'): ('C1,V2', '§4.2·§13.2 V2', 'customer sales promise obligation; ALLOCATION_SHORTAGE covers only the shortage part'),
    ('kind', 'RECONCILIATION'): ('T17', '§6 판매·§13.1 D17', 'source claim intake reconciliation; candidates DELIVERY_RECONCILIATION or UNVERIFIED_SOURCE_REVIEW'),
    ('kind', 'REAUTHORIZE'): ('T08,V7', '§7.1·§7.2', 'responsibility left after a grant revocation blocks a command'),
    ('kind', 'QUANTITY_SHORTFALL'): ('T12,T10,T11', '§6 구매·운송', 'arrival shortfall duty; candidate RECEIPT_SHORTFALL'),
    ('kind', 'TRANSPORT_DISCREPANCY'): ('T14', '§6 운송', 'transport loss/discrepancy duty'),
    ('kind', 'MOVEMENT_RECONCILIATION'): ('V4', '§4.2·§4.3', 'reconciliation duty for an observed unauthorized movement'),
    ('kind', 'VERSION_UNSUPPORTED'): ('T21,V1', '§8 미지원 과거 evaluator', 'responsibility for a held unsupported definition version'),
    ('kind', 'STOCK_RECONCILIATION'): ('T06', '§4.3', 'stock correction reconciliation duty'),
    ('kind', 'SOURCE_QUARANTINE'): ('T22', '§9.3 외부 원천', 'quarantined external source duty'),
    ('kind', 'EVIDENCE_CONFLICT'): ('T22', '§4.3', 'duty for conflicting valid sources; the name is also an error code'),
    ('kind', 'RECEIPT_REMAINDER'): ('V6', '§6 수령', 'remaining receipt quantity duty'),
    ('kind', 'DELIVER'): ('C3,T08,V4,V6,V7', '§5.3', 'seeded generic delivery duty in fixtures'),
    ('kind', 'ARRIVAL_REMAINDER'): ('T21,V1', '§5.3', 'seeded arrival remainder duty in fixtures'),
}

OUTCOME_POINTER = re.compile(r'^/response(/body/result/structuredContent)?/outcome$')
CODE_POINTER = re.compile(r'^/response(/body/result/structuredContent)?/error/code$')
OBLIGATION_POINTER = re.compile(r'/(obligations|assignments)$|/linkedSummary/duties$')
NEGATIVE_OPS = {'notEquals', 'absent'}

problems, used = [], {}


def is_ref(v):
    return isinstance(v, dict) and ('$alias' in v or '$result' in v)


def strings(v):
    if isinstance(v, str):
        yield v
    elif isinstance(v, list):
        for x in v:
            yield from strings(x)


def use(kind, value, where):
    key = (kind, value)
    if key in PENDING:
        used.setdefault(key, set()).add(where.split('/')[0])
        return True
    return False


def check_outcome(v, where):
    if v not in OUTCOMES and not use('outcome', v, where):
        problems.append(f'{where}: outcome {v} is not in contracts/domain-vocabulary.json')


def check_code(v, where):
    if v not in CODES and not use('code', v, where):
        problems.append(f'{where}: error code {v} is not in contracts/domain-vocabulary.json')


def check_kind(v, where):
    if v not in KINDS and not use('kind', v, where):
        problems.append(f'{where}: obligation kind {v} is not in contracts/domain-vocabulary.json')


def field_values(assertion, src, name):
    """Expected values projected for field `name` (single field or a tuple position)."""
    field, exp = src.get('field'), assertion.get('expected')
    if field == name:
        return [v for v in (exp if isinstance(exp, list) else [exp]) if isinstance(v, str)]
    if isinstance(field, list) and name in field and isinstance(exp, list):
        i = field.index(name)
        return [row[i] for row in exp if isinstance(row, list) and len(row) > i and isinstance(row[i], str)]
    return []


def check_audit(assertion, role, src, where):
    pointer = src['pointer']
    name = pointer.rsplit('/', 1)[-1]
    if name not in AUDIT_SOURCES:
        problems.append(f'{where}: audit source {pointer} is not one of {sorted(AUDIT_SOURCES)} (contracts/audit-observation-fields.json)')
        return
    fields = AUDIT_FIELDS[name]
    for key, value in src.get('where', {}).items():
        if key not in fields:
            problems.append(f'{where}: audit where key {key} is not a {name} field')
        elif fields[key] != 'ALWAYS':
            problems.append(f'{where}: audit where key {key} is {fields[key]}; only ALWAYS fields may filter')
    projected = src.get('field')
    for f in ([projected] if isinstance(projected, str) else projected or []):
        if f not in fields:
            problems.append(f'{where}: audit field {f} is not a {name} field')
    if role == 'source' and assertion['op'] == 'fieldsPresent':
        for f in assertion['expected']:
            if f not in fields:
                problems.append(f'{where}: audit field {f} is not a {name} field')
    outcomes = [src.get('where', {}).get('outcome')] + (field_values(assertion, src, 'outcome') if role == 'source' else [])
    for v in [x for x in outcomes if isinstance(x, str)]:
        if name == 'queryAudit':
            if v not in QUERY_AUDIT_OUTCOMES:
                problems.append(f'{where}: queryAudit outcome {v} is not READ/REJECTED')
        elif v in CODES and v not in OUTCOMES:
            problems.append(f'{where}: audit outcome {v} is an error code; use outcome plus errorCode')
        else:
            check_outcome(v, where)
    if role == 'source':
        for v in field_values(assertion, src, 'errorCode'):
            check_code(v, where)


def check_case(path):
    case = json.loads(path.read_text())
    cid = case['caseId']
    for sub in case['subcases']:
        actions = {}
        stack = list(sub['actions'])
        while stack:
            a = stack.pop()
            actions[a['id']] = a
            for b in a.get('branches', []):
                stack.extend(b.get('actions', []))
        for a in actions.values():
            if a.get('capabilityId') == 'getObligations' and isinstance(a.get('request', {}).get('kind'), str):
                check_kind(a['request']['kind'], f'{cid}/{sub["id"]}/{a["id"]}')
        pairs = {}
        for assertion in sub['assertions']:
            where = f'{cid}/{sub["id"]}/{assertion["id"]}'
            if assertion['op'] in NEGATIVE_OPS:
                continue
            for role in ('source', 'baseline', 'unitSource', 'baselineUnitSource'):
                src = assertion.get(role)
                if not src:
                    continue
                pointer = src.get('pointer', '')
                last = pointer.rsplit('/', 1)[-1]
                if pointer.startswith('/data/rawRows/') and 'audit' in last.lower():
                    check_audit(assertion, role, src, where)
                if OBLIGATION_POINTER.search(pointer) or (pointer.startswith('/response/data/items')
                                                          and actions.get(src['actionId'], {}).get('capabilityId') == 'getObligations'):
                    kind = src.get('where', {}).get('kind')
                    if isinstance(kind, str):
                        check_kind(kind, where)
                    if role == 'source':
                        for v in field_values(assertion, src, 'kind'):
                            check_kind(v, where)
                if pointer.endswith('/toolCalls'):
                    w = src.get('where', {})
                    if isinstance(w.get('serverOutcome'), str):
                        check_outcome(w['serverOutcome'], where)
                    if isinstance(w.get('serverErrorCode'), str):
                        check_code(w['serverErrorCode'], where)
            src = assertion['source']
            value = assertion.get('expected')
            if assertion['op'] == 'equals' and isinstance(value, str):
                if OUTCOME_POINTER.match(src['pointer']):
                    check_outcome(value, where)
                    pairs.setdefault((src['actionId'], src['pointer'][:-len('/outcome')]), {})['outcome'] = value
                elif CODE_POINTER.match(src['pointer']):
                    check_code(value, where)
                    pairs.setdefault((src['actionId'], src['pointer'][:-len('/error/code')]), {})['code'] = value
        for (action, _), pair in pairs.items():
            outcome, code = pair.get('outcome'), pair.get('code')
            if outcome and code in CODES and outcome not in CODES[code]:
                if not use('pair', f'{code}/{outcome}', f'{cid}/{sub["id"]}/{action}'):
                    problems.append(f'{cid}/{sub["id"]}/{action}: {code} is asserted with outcome {outcome}; vocabulary allows {sorted(CODES[code])}')


def walk_fixture(node, path, where):
    if isinstance(node, dict):
        for key, value in node.items():
            here = f'{path}/{key}'
            if key == 'kind' and isinstance(value, str) and re.search(r'obligation|duties|DUTY', path, re.I):
                check_kind(value, f'{where}:{path}')
            walk_fixture(value, here, where)
    elif isinstance(node, list):
        for value in node:
            walk_fixture(value, path, where)


def review():
    """Check every committed case and fixture. Returns (problems, pending): pending maps 'kind name' to the cases
    that use it, its plan reference, reason and owner. Safe to call more than once."""
    problems.clear()
    used.clear()
    for path in sorted((ROOT / 'verification/cases').glob('*/case.json')):
        check_case(path)
    for path in sorted((ROOT / 'verification/cases').glob('*/**/*.json')):
        rel = path.relative_to(ROOT / 'verification/cases')
        if rel.name in ('case.json', 'observation-bindings.json', 'oracle-bindings.json', 'registry.json') or rel.parts[1] in ('evidence', 'selftest'):
            continue
        try:
            data = json.loads(path.read_text())
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if isinstance(data, dict) and 'fixtureId' in data:
            walk_fixture(data, '', str(rel))
    stale = sorted(f'{k[0]} {k[1]}' for k in PENDING if k not in used)
    problems.extend(f'pending entry {s} is used by no case; remove it' for s in stale)
    pending = {f'{k[0]} {k[1]}': {'cases': sorted(used.get(k, [])), 'planRef': PENDING[k][1], 'reason': PENDING[k][2], 'owner': PENDING_OWNER}
               for k in PENDING}
    return list(problems), pending


def main(argv):
    found, pending = review()
    report = {'status': 'VALID' if not found else 'INVALID', 'problems': found, 'pending': pending}
    if '--json' in argv:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        for p in found:
            print('PROBLEM', p)
        for key, info in pending.items():
            print(f'KNOWN_OPEN {info["owner"]}\t{key}\t{",".join(info["cases"])}\t{info["planRef"]}')
        print(json.dumps({'status': report['status'], 'problems': len(found), 'knownOpen': len(pending)}))
    return 1 if found and '--check' in argv else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
