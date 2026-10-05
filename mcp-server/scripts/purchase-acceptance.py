#!/usr/bin/env python3
"""Opt-in destructive fixture setup, only for a fresh disposable local demo database.
HTTP agent calls are manually scripted; human approval uses live stdio MCP.
No model is invoked. The backend must already run with Flyway and local auth.
"""
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.request
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[2]
BASE = os.environ['MULINO_API_BASE']
DB = os.environ['MULINO_ACCEPTANCE_DB']
CONTAINER = os.environ.get('MULINO_ACCEPTANCE_CONTAINER', 'mulino-merged-validation-pg')
SECRET = os.environ['MULINO_LOCAL_SERVICE_SECRET']
OUT = Path(os.environ['MULINO_ACCEPTANCE_EVIDENCE'])
assert re.fullmatch(r'mulino_purchase_acceptance_[a-z0-9_]+', DB), 'A separate acceptance DB is required'
url = urlsplit(BASE)
assert url.scheme == 'http' and url.hostname == '127.0.0.1' and url.port is not None, 'Local demo backend only'
assert 1 <= url.port <= 65535 and url.username is None and url.password is None
assert url.path == '/api/v1' and not url.query and not url.fragment
OUT.mkdir(parents=True, exist_ok=True)
checks = []

def sql(query, expected_failure=False):
    result = subprocess.run(['docker', 'exec', '-i', CONTAINER, 'psql', '-U', 'postgres', '-d', DB,
                             '-At', '-v', 'ON_ERROR_STOP=1'], input=query, text=True, capture_output=True)
    if expected_failure:
        assert result.returncode != 0, 'Audit mutation unexpectedly succeeded'
        return result.stderr.strip()
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()

def save(name, value):
    (OUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def api(route, body=None, headers=None, status=200):
    headers = dict(headers or {})
    if body is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(BASE + route, data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            code, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        code, raw = error.code, error.read()
    assert code == status, f'{route}: HTTP {code}, expected {status}: {raw.decode()}'
    return json.loads(raw) if raw else None

service = {'X-Mulino-Local-Service': SECRET}
manager = {'X-Mulino-Local-Role': 'MANAGER'}

def agent_work(case, tag, role):
    ref = 'WI-A-' + tag.replace('-SUPPLY', '-S').replace('-PURCHASE', '-P')
    sql(f"INSERT INTO work_items(work_item_ref,case_id,title,assigned_agent_id) SELECT '{ref}',{case['caseId']},'{ref}',agent_id FROM agents WHERE agent_key='{role}';")
    api('/runs', {'agentKey': role, 'caseRef': case['caseRef'], 'workItemRef': ref, 'runtime': 'CODEX'}, service)
    run = api('/internal/runs/claim', {'workerId': 'acceptance-manual', 'runtime': 'CODEX'}, service)
    assert run is not None
    return ref, {'Authorization': 'Bearer ' + run['capabilityToken']}

def plan_and_propose(case, tag):
    work, auth = agent_work(case, tag + '-SUPPLY', 'SUPPLY_CHAIN')
    plan = api('/cases/' + case['caseRef'] + '/plans', {'warehouseId': warehouse, 'productIds': products, 'horizonDays': 30},
               {**auth, 'Idempotency-Key': tag + '-plan'})
    assert plan['result']['status'] == 'READY'
    assert str(plan['result']['totalAmount']) == '16500', plan['result']
    api('/agent/work-items/' + work + '/transition', {'outcome': 'DONE', 'summary': 'Persisted plan checked', 'waitingConditions': []},
        {**auth, 'Idempotency-Key': tag + '-supply-done'})
    work, auth = agent_work(case, tag + '-PURCHASE', 'PROCUREMENT')
    proposal = api('/plans/' + plan['ref'] + '/purchase-proposal', {}, {**auth, 'Idempotency-Key': tag + '-proposal'})
    approval = api('/approvals/' + str(proposal['approvalId']), headers=manager)
    save(tag + '-pending.json', approval)
    assert approval['status'] == 'PENDING'
    return plan, proposal, work

def decide(proposal, decision, key, status=200, **changes):
    body = {'decision': decision, 'expectedVersion': proposal['version'], 'proposalHash': proposal['proposalHash'],
            'reason': 'Local acceptance: quantities, supplier conditions and delivery reviewed', **changes}
    result = api('/approvals/' + str(proposal['approvalId']) + '/decision', body, {**manager, 'Idempotency-Key': key}, status)
    save(key + '.json', result)
    return result

def state():
    return json.loads(sql("SELECT json_build_object('applications',(SELECT count(*) FROM purchase_applications),'linkedOrders',(SELECT count(*) FROM purchase_orders WHERE purchase_application_id IS NOT NULL),'followups',(SELECT count(*) FROM replenishment_followups),'decisions',(SELECT count(*) FROM governance_decisions),'audits',(SELECT count(*) FROM governance_audit_logs));"))

# Move every literal business date by the same offset. The backend uses its real
# Asia/Seoul date; no injected fixed clock or forged asOf request is used.
today = dt.datetime.now(dt.timezone(dt.timedelta(hours=9))).date()
delta = today - dt.date(2026, 9, 5)
seed = (ROOT / 'database/seed/replenishment_demo.sql').read_text()
seed = re.sub(r'\b20\d{2}-\d{2}-\d{2}\b', lambda m: str(dt.date.fromisoformat(m[0]) + delta), seed)
assert sql('SELECT count(*) FROM cases;') == '0'
assert sql('SELECT count(*) FROM products;') == '0'
(OUT / 'fixture.sql').write_text(seed)
sql(seed)
production_count_before = sql('SELECT count(*) FROM production_lots;')
warehouse = int(sql("SELECT warehouse_id FROM warehouses WHERE plant_id='DEMO-KR-01';"))
products = json.loads(sql("SELECT json_agg(product_id ORDER BY product_id) FROM products WHERE sku IN ('DEMO-AMR','DEMO-BSC');"))

def create(tag):
    return api('/cases', {'objective': 'Purchase acceptance ' + tag},
               {'X-Mulino-Local-Role': 'OPERATOR', 'Idempotency-Key': tag + '-case'})

blocked_case = create('block')
_, blocked, blocked_work = plan_and_propose(blocked_case, 'block')
before = state()
assert before['applications'] == before['linkedOrders'] == before['followups'] == 0
blocked_result = decide(blocked, 'BLOCK', 'block-once')
assert blocked_result['status'] == 'BLOCKED'
blocked_state = state()
assert decide(blocked, 'BLOCK', 'block-once') == blocked_result
assert state() == blocked_state
assert state()['linkedOrders'] == state()['followups'] == 0
assert sql(f"SELECT status FROM work_items WHERE work_item_ref='{blocked_work}';") == 'CANCELLED'
checks += ['BLOCK keeps zero orders/followups; replay has no extra history']

case = blocked_case  # Continue the same warehouse responsibility and version history.
cancel_plan, cancelled, cancelled_work = plan_and_propose(case, 'cancel')
before = state()
for role in ['OPERATOR', 'VIEWER']:
    body = {'decision':'CANCEL','expectedVersion':cancelled['version'],'proposalHash':cancelled['proposalHash'],'reason':'Cancel this pending proposal'}
    api('/approvals/' + str(cancelled['approvalId']) + '/decision', body,
        {'X-Mulino-Local-Role':role,'Idempotency-Key':'cancel-denied-'+role},403)
decide(cancelled,'CANCEL','cancel-wrong-version',409,expectedVersion=cancelled['version']+1)
decide(cancelled,'CANCEL','cancel-wrong-hash',409,proposalHash='0'*64)
assert state() == before
cancel_attention = sql(f"INSERT INTO attention_requests(case_id,reason_type,title,question,suggested_scope) VALUES({case['caseId']},'MISSING_HUMAN_CONTEXT','Cancel scope','Scope?','THIS_ACTION') RETURNING attention_request_id;").splitlines()[0]
cancel_env = {**os.environ,'MULINO_TEST_APPROVAL_ID':str(cancelled['approvalId']),'MULINO_TEST_PLAN_REF':cancel_plan['ref'],
              'MULINO_TEST_CASE_REF':case['caseRef'],'MULINO_TEST_ATTENTION_ID':cancel_attention,'MULINO_TEST_DECISION':'CANCEL'}
cancel_env.pop('MULINO_LOCAL_SERVICE_SECRET',None)
r = subprocess.run(['node','scripts/local-human-flow.mjs'],cwd=ROOT/'mcp-server',env=cancel_env,text=True,capture_output=True)
(OUT/'stdio-cancel.log').write_text(r.stdout+r.stderr)
assert r.returncode == 0,r.stdout+r.stderr
cancel_state = state()
decide(cancelled,'CANCEL','cancel-new-key',409)
decide(cancelled,'APPROVE','live-cancel-once',409)
assert state() == cancel_state
assert cancel_state['applications'] == cancel_state['linkedOrders'] == cancel_state['followups'] == 0
assert sql(f"SELECT status::text || ':' || procurement_outcome FROM work_items WHERE work_item_ref='{cancelled_work}';") == 'CANCELLED:CANCELLED'
assert sql(f"SELECT count(*) FROM waiting_conditions w JOIN work_items i USING(work_item_id) WHERE i.work_item_ref='{cancelled_work}' AND w.status='ACTIVE';") == '0'
decision_id = sql(f"SELECT governance_decision_id FROM governance_decisions WHERE governance_action_id={cancelled['approvalId']} AND decision='CANCEL' AND is_final;")
assert decision_id.isdecimal()
save('cancel-final-history-mutation-denied.json',{'update':sql(f"UPDATE governance_decisions SET reason='tampered' WHERE governance_decision_id={decision_id};",True),
     'delete':sql(f"DELETE FROM governance_decisions WHERE governance_decision_id={decision_id};",True)})
checks += ['Live stdio MANAGER CANCEL is final; VIEWER/OPERATOR denied; version/hash guards and replay preserve counts; no orders/applications/followups']

_, stale, _ = plan_and_propose(case, 'stale')
before = state()
decide(stale, 'APPROVE', 'wrong-version', 409, expectedVersion=stale['version'] + 1)
decide(stale, 'APPROVE', 'wrong-hash', 409, proposalHash='0' * 64)
assert state() == before
# A real source price change invalidates the pending proposal. Restore only the
# fixture price afterwards; the old action remains EXPIRED forever.
sql("UPDATE supplier_material_terms SET unit_price=unit_price+100 WHERE raw_material_id=(SELECT raw_material_id FROM raw_materials WHERE name='DEMO 밀가루') AND supplier_id=(SELECT supplier_id FROM suppliers WHERE name='DEMO 신속 공급');")
decide(stale, 'APPROVE', 'changed-source', 409)
assert api('/approvals/' + str(stale['approvalId']), headers=manager)['status'] == 'EXPIRED'
assert state()['linkedOrders'] == state()['followups'] == 0
sql("UPDATE supplier_material_terms SET unit_price=unit_price-100 WHERE raw_material_id=(SELECT raw_material_id FROM raw_materials WHERE name='DEMO 밀가루') AND supplier_id=(SELECT supplier_id FROM suppliers WHERE name='DEMO 신속 공급');")
plan, proposal, work = plan_and_propose(case, 'approved')
assert proposal['version'] > stale['version']
assert proposal['proposalHash'] != stale['proposalHash']
checks += ['Version/hash mismatch keeps state unchanged', 'Changed supplier price expires old action without orders', 'Recalculation has a new version/hash and separate approval']
attention = sql(f"INSERT INTO attention_requests(case_id,reason_type,title,question,suggested_scope) VALUES({case['caseId']},'MISSING_HUMAN_CONTEXT','Local scope','Scope?','THIS_ACTION') RETURNING attention_request_id;").splitlines()[0]
# Existing real stdio check proves VIEWER denial, MANAGER approval, replay and PO
# total. Do not pass agent/service credentials into its MCP child processes.
env = {**os.environ, 'MULINO_TEST_APPROVAL_ID': str(proposal['approvalId']), 'MULINO_TEST_PLAN_REF': plan['ref'],
       'MULINO_TEST_CASE_REF': case['caseRef'], 'MULINO_TEST_ATTENTION_ID': attention}
env.pop('MULINO_LOCAL_SERVICE_SECRET', None)
r = subprocess.run(['node', 'scripts/local-human-flow.mjs'], cwd=ROOT / 'mcp-server', env=env, text=True, capture_output=True)
(OUT / 'stdio.log').write_text(r.stdout + r.stderr)
assert r.returncode == 0, r.stdout + r.stderr
approved = api('/approvals/' + str(proposal['approvalId']), headers=manager)
save('approved-final.json', approved)
orders = [api('/purchase-orders/' + str(order_id), headers=manager) for order_id in approved['purchaseOrderIds']]
save('orders.json', orders)
assert sum(int(line['lineAmountKrw']) for order in orders for line in order['items']) == 16500
assert all(order['taxInvoiceNumber'] is None and order['taxInvoiceDate'] is None for order in orders)
snapshot = json.loads(sql(f"SELECT json_build_object('caseStatus',(SELECT status FROM cases WHERE case_id={case['caseId']}),'purchaseWorkStatus',(SELECT status FROM work_items WHERE work_item_ref='{work}'),'followupStatus',(SELECT w.status FROM replenishment_followups f JOIN work_items w USING(work_item_id) WHERE f.case_id={case['caseId']}),'audits',(SELECT json_agg(row_to_json(a)) FROM governance_audit_logs a));"))
save('business-state.json', snapshot)
final = state()
assert final['applications'] == final['linkedOrders'] == final['followups'] == 1
assert sql(f"SELECT status FROM work_items WHERE work_item_ref='{work}';") == 'DONE'
assert sql(f"SELECT w.status FROM replenishment_followups f JOIN work_items w USING(work_item_id) WHERE f.case_id={case['caseId']};") == 'WAITING'
assert api('/cases/' + case['caseRef'], headers=manager)['status'] == 'WAITING'
assert sql('SELECT count(*) FROM inbound WHERE purchase_order_item_id IN (SELECT purchase_order_item_id FROM purchase_order_items i JOIN purchase_orders p USING(purchase_order_id) WHERE p.purchase_application_id IS NOT NULL);') == '0'
assert sql('SELECT count(*) FROM production_lots;') == production_count_before
checks += ['Live stdio VIEWER denied, MANAGER approved once, replay yields same PO', 'Purchase work DONE; followup WAITING; Case WAITING; no new inbound/production']
# Observe immutable action audit, not merely application events.
audit_id = sql(f"SELECT governance_audit_log_id FROM governance_audit_logs WHERE governance_action_id={proposal['approvalId']} AND event_type='PURCHASE_APPLIED';")
assert audit_id.isdecimal()
audit_before = sql(f'SELECT row_to_json(g) FROM governance_audit_logs g WHERE governance_audit_log_id={audit_id};')
errors = {}
for name, query in {'update': f"UPDATE governance_audit_logs SET event_type='TAMPERED' WHERE governance_audit_log_id={audit_id};",
                    'delete': f'DELETE FROM governance_audit_logs WHERE governance_audit_log_id={audit_id};',
                    'truncate': 'TRUNCATE governance_audit_logs;'}.items():
    errors[name] = sql(query, True)
    assert sql(f'SELECT row_to_json(g) FROM governance_audit_logs g WHERE governance_audit_log_id={audit_id};') == audit_before
save('audit-mutation-denied.json', errors)
checks += ['Action audit UPDATE/DELETE/TRUNCATE denied and row unchanged']
history = json.loads(sql("SELECT json_agg(row_to_json(t)) FROM (SELECT a.governance_action_id,a.case_id,a.proposal_version,a.proposal_hash,a.status,d.decision,d.decided_by,d.decided_at FROM governance_actions a LEFT JOIN governance_decisions d USING(governance_action_id) WHERE a.replenishment_plan_id IS NOT NULL ORDER BY a.governance_action_id) t;"))
save('history.json', history)
assert [h['status'] for h in history] == ['BLOCKED', 'CANCELLED', 'EXPIRED', 'APPROVED']
assert [h['decision'] for h in history] == ['BLOCK', 'CANCEL', None, 'APPROVE']
summary = {'sourceCommit': subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT,text=True).strip(),
           'sourceDiffSha256': hashlib.sha256(subprocess.check_output(['git','diff','HEAD'],cwd=ROOT)).hexdigest(),
           'migrationV28Sha256': hashlib.sha256((ROOT/'backend/src/main/resources/db/migration/V28__purchase_cancellation.sql').read_bytes()).hexdigest(),
           'jarSha256': hashlib.sha256((ROOT/'backend/build/libs/backend-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
           'executedAt': dt.datetime.now(dt.timezone.utc).isoformat(), 'businessDate': str(today), 'database': DB,
           'fixtureSha256': hashlib.sha256(seed.encode()).hexdigest(), 'manualAgentRuns': True, 'modelInvoked': False,
           'checks': checks, 'finalState': final, 'history': history,
           'issue34': {'passedCriteria': 5, 'totalCriteria': 5, 'complete': True},
           'productionLotsBefore': int(production_count_before),
           'productionLotsAfter': int(sql('SELECT count(*) FROM production_lots;')),
           'unresolved': None}
save('summary.json', summary)
print(json.dumps(summary, ensure_ascii=False, indent=2))
