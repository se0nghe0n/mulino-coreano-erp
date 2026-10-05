import assert from "node:assert/strict";
import http from "node:http";
import path from "node:path";
import { afterEach, test } from "node:test";
import { fileURLToPath } from "node:url";

import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const PROJECT_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const resources = [];

afterEach(async () => {
  while (resources.length) {
    await resources.pop()();
  }
});

test("ask_inventory sends only the explicit product or SKU search term", async () => {
  let requestedUrl;
  const apiServer = http.createServer((req, res) => {
    requestedUrl = req.url;
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({
      answer: "1 inventory location",
      intent: "ASK",
      query: "AMR-200",
      inventory: [],
      provenance: "test",
      totalLocationCount: 1,
      returnedLocationCount: 1,
      truncated: false,
    }));
  });
  const apiBase = await listen(apiServer);
  resources.push(() => closeServer(apiServer));
  const client = await connectClient({ MULINO_API_BASE: `${apiBase}/api/v1` });

  const result = await client.callTool({
    name: "ask_inventory",
    arguments: { productQuery: "AMR-200" },
  });

  assert.equal(result.isError, undefined);
  assert.equal(requestedUrl, "/api/v1/ask?q=AMR-200");
});

test("list_cases accepts the valid argument-free MCP call", async () => {
  let requestedUrl;
  const apiServer = http.createServer((req, res) => {
    requestedUrl = req.url;
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end("[]");
  });
  const apiBase = await listen(apiServer);
  resources.push(() => closeServer(apiServer));
  const client = await connectClient({ MULINO_API_BASE: `${apiBase}/api/v1` });

  const result = await client.callTool({ name: "list_cases" });

  assert.equal(result.isError, undefined);
  assert.equal(requestedUrl, "/api/v1/cases");
});

test("list_cases rejects a status outside its declared contract", async () => {
  let requests = 0;
  const apiServer = http.createServer((req, res) => {
    requests++;
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end("[]");
  });
  const apiBase = await listen(apiServer);
  resources.push(() => closeServer(apiServer));
  const client = await connectClient({ MULINO_API_BASE: `${apiBase}/api/v1` });

  const result = await client.callTool({
    name: "list_cases",
    arguments: { status: "OPEN&unexpected=true" },
  });

  assert.equal(result.isError, true);
  assert.match(result.content[0].text, /status/);
  assert.equal(requests, 0);
});

test("case creation body timeout reports an uncertain mutation outcome without retrying", async () => {
  let requests = 0;
  const apiServer = http.createServer((req, res) => {
    requests++;
    req.resume();
    res.writeHead(200, { "Content-Type": "application/json" });
    res.flushHeaders();
    // Deliberately leave the body open: JSON parsing must share the API deadline.
  });
  const apiBase = await listen(apiServer);
  resources.push(() => closeServer(apiServer));
  const client = await connectClient({
    MULINO_API_BASE: `${apiBase}/api/v1`,
    MULINO_API_TIMEOUT_MS: "50",
  });

  const result = await within(
    client.callTool({
      name: "create_case",
      arguments: { objective: "Keep Amaretti in stock" },
    }),
    500,
    "connector did not enforce the configured API timeout",
  );

  assert.equal(result.isError, true);
  assert.match(result.content[0].text, /시간 초과/);
  assert.match(result.content[0].text, /반영되었을 수/);
  assert.match(result.content[0].text, /자동 재시도하지/);
  assert.equal(requests, 1);
});

async function connectClient(extraEnv) {
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: ["src/index.js"],
    cwd: PROJECT_DIR,
    env: extraEnv,
    stderr: "pipe",
  });
  const client = new Client({ name: "mulino-mcp-test", version: "1.0.0" });
  await client.connect(transport);
  resources.push(() => client.close());
  return client;
}

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      const address = server.address();
      resolve(`http://127.0.0.1:${address.port}`);
    });
  });
}

function closeServer(server) {
  return new Promise((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
    server.closeAllConnections?.();
  });
}

function within(promise, timeoutMs, message) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(message)), timeoutMs)),
  ]);
}

for (const role of [undefined, "MANAGER"]) {
  test(`create_case forwards the configured human role (${role ?? "default"})`, async () => {
    let headers; let body;
    const apiServer = http.createServer((req, res) => {
      headers = req.headers;
      const chunks=[]; req.on("data",chunk=>chunks.push(chunk)); req.on("end",()=>{ body=JSON.parse(Buffer.concat(chunks)); });
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ caseRef: "CASE-test", title: "work", status: "OPEN" }));
    });
    const apiBase = await listen(apiServer);
    resources.push(() => closeServer(apiServer));
    const env = { MULINO_API_BASE: `${apiBase}/api/v1`, MULINO_LOCAL_HUMAN_SECRET: "test-human-gateway" };
    if (role) env.MULINO_LOCAL_ROLE = role;
    const client = await connectClient(env);
    const result = await client.callTool({ name: "create_case", arguments: { objective: "work", requestKey: "scope-key", replenishment: { warehouseId: 3, productSkus: ["SKU-A"], targetDate: "2026-09-05" } } });
    assert.equal(result.isError, undefined);
    assert.equal(headers["x-mulino-local-role"], role ?? "OPERATOR");
    assert.equal(headers["x-mulino-local-human"], "test-human-gateway");
    assert.equal(headers["idempotency-key"], "scope-key");
    assert.deepEqual(body.replenishment,{ warehouseId: 3, productSkus: ["SKU-A"], targetDate: "2026-09-05" });
    assert.equal(headers.authorization, undefined);
    assert.equal(headers["x-mulino-local-service"], undefined);
  });
}

for (const [name,args,url] of [["whoami",{},"/me"],["get_case",{caseRef:"CASE-a"},"/cases/CASE-a"],["get_plan",{planRef:"PLAN-a"},"/plans/PLAN-a"]]) {
  test(`${name} calls only the human read endpoint`, async () => {
    const apiServer = http.createServer((req,res) => {
      assert.equal(req.url,"/api/v1"+url);
      assert.equal(req.method,"GET");
      assert.equal(req.headers["x-mulino-local-role"],"VIEWER");
      assert.equal(req.headers.authorization,undefined);
      assert.equal(req.headers["x-mulino-local-service"],undefined);
      res.writeHead(200,{"Content-Type":"application/json"});res.end('{"status":"OPEN"}');
    });
    const apiBase=await listen(apiServer);resources.push(()=>closeServer(apiServer));
    const client=await connectClient({MULINO_API_BASE:apiBase+"/api/v1",MULINO_LOCAL_ROLE:"VIEWER"});
    const result=await client.callTool({name,arguments:args});assert.equal(result.isError,undefined);
  });
}

for (const [name,args,url,method] of [
  ['get_quality_approval',{approvalId:'9007199254740993'},'/quality/approvals/9007199254740993','GET'],
  ['get_inbound_quality',{inboundId:'9007199254740993'},'/quality/inbound/9007199254740993','GET'],
  ['decide_quality',{approvalId:'9007199254740993',decision:'APPROVE',expectedVersion:2,proposalHash:'c'.repeat(64),reason:'QC reviewed',requestKey:'qc-once'},'/quality/approvals/9007199254740993/decision','POST'],
  ['get_approval',{approvalId:'9007199254740993'},'/approvals/9007199254740993','GET'],
  ['get_purchase_order',{purchaseOrderId:'9007199254740993'},'/purchase-orders/9007199254740993','GET'],
  ['decide_purchase',{approvalId:'9007199254740993',decision:'APPROVE',expectedVersion:2,proposalHash:'a'.repeat(64),reason:'Reviewed',requestKey:'manager-once'},'/approvals/9007199254740993/decision','POST'],
  ['decide_purchase',{approvalId:'9007199254740993',decision:'CANCEL',expectedVersion:2,proposalHash:'b'.repeat(64),reason:'Cancel pending proposal',requestKey:'cancel-once'},'/approvals/9007199254740993/decision','POST'],
  ['answer_attention',{attentionRequestId:1,answer:'Friday',expectedVersion:2,scope:'THIS_ACTION',requestKey:'answer-once'},'/attention/1/answer','POST'],
]) {
  test(`${name} preserves exact business evidence and uses only human role authority`, async () => {
    let body='';
    const apiServer=http.createServer((req,res)=>{
      assert.equal(req.url,'/api/v1'+url);assert.equal(req.method,method);
      assert.equal(req.headers['x-mulino-local-role'],name==='decide_quality'?'QC':'MANAGER');
      assert.equal(req.headers.authorization,undefined);
      assert.equal(req.headers['x-mulino-local-service'],undefined);
      assert.equal(req.headers['x-mulino-run-capability'],undefined);
      req.on('data',chunk=>body+=chunk);req.on('end',()=>{
        if(method==='POST') {const actual=JSON.parse(body);assert.equal(actual.expectedVersion,2);assert.equal(actual.decision,args.decision);assert.equal(actual.proposalHash,args.proposalHash);assert.equal(req.headers['idempotency-key'],args.requestKey);}
        res.writeHead(200,{'Content-Type':'application/json'});res.end('{"id":9007199254740993,"totalKrw":9999999999999.99,"status":"APPROVED"}');
      });
    });
    const apiBase=await listen(apiServer);resources.push(()=>closeServer(apiServer));
    const client=await connectClient({MULINO_API_BASE:apiBase+'/api/v1',MULINO_LOCAL_ROLE:name==='decide_quality'?'QC':'MANAGER'});
    const result=await client.callTool({name,arguments:args});
    assert.equal(result.isError,undefined);assert.equal(result.structuredContent.id,'9007199254740993');
    assert.equal(result.structuredContent.totalKrw,'9999999999999.99');
    assert.equal(result.structuredContent.capabilityToken,undefined);assert.equal(result.structuredContent.leaseToken,undefined);
  });
}

test('purchase decision requires version and hash before any API call', async ()=>{
  const client=await connectClient({MULINO_API_BASE:'http://127.0.0.1:1/api/v1'});
  const listed=await client.listTools();
  assert.equal(listed.tools.find(t=>t.name==='decide_purchase').annotations.readOnlyHint,false);
  for(const omitted of ['expectedVersion','proposalHash']) {
    const args={approvalId:1,decision:'APPROVE',expectedVersion:1,proposalHash:'a'.repeat(64),reason:'Reviewed'};delete args[omitted];
    assert.equal((await client.callTool({name:'decide_purchase',arguments:args})).isError,true);
  }
});

test('legacy work reads carry the host gateway and returned errors never expose it', async () => {
  const sentinel='host-human-transport-sentinel';
  let requests=0;
  const apiServer=http.createServer((req,res)=>{
    requests++;
    assert.equal(req.headers['x-mulino-local-human'],sentinel);
    assert.equal(req.headers['x-mulino-local-role'],'VIEWER');
    assert.equal(req.headers.authorization,undefined);
    assert.equal(req.headers['x-mulino-local-service'],undefined);
    res.writeHead(401);res.end('rejected '+sentinel);
  });
  const apiBase=await listen(apiServer);resources.push(()=>closeServer(apiServer));
  const client=await connectClient({MULINO_API_BASE:apiBase+'/api/v1',MULINO_LOCAL_ROLE:'VIEWER',MULINO_LOCAL_HUMAN_SECRET:sentinel});
  assert.ok(!JSON.stringify(await client.listTools()).includes(sentinel));
  for(const name of ['list_cases','list_attention','monitor_status']) {
    const result=await client.callTool({name,arguments:{MULINO_LOCAL_HUMAN_SECRET:'forged-tool-value'}});
    assert.equal(result.isError,true);
    assert.ok(!JSON.stringify(result).includes(sentinel));
  }
  assert.equal(requests,3);
});

for(const [name,args,path] of [
  ['register_evidence',{caseRef:'CASE-1',sourceType:'EMAIL',externalRef:'message-1',observedAt:'2026-10-05T12:30:00+09:00',title:'Observation',content:'exact 0.1234567890123456789',requestKey:'source'},'/epistemic/cases/CASE-1/evidence'],
  ['create_claim',{caseRef:'CASE-1',subjectType:'ETA',subjectRef:'PO-1',claimText:'Tomorrow',requestKey:'claim'},'/epistemic/cases/CASE-1/claims'],
  ['link_claim_evidence',{caseRef:'CASE-1',claimId:'9007199254740993',evidenceRef:'EV-1',relation:'REFUTES',requestKey:'refute'},'/epistemic/cases/CASE-1/claims/9007199254740993/links'],
  ['judge_claim',{caseRef:'CASE-1',claimId:'9007199254740993',status:'CONFLICTED',expectedRevision:0,evidenceFingerprint:'a'.repeat(64),reason:'Human attestation',requestKey:'judge'},'/epistemic/cases/CASE-1/claims/9007199254740993/judgment'],
]) {
  test(`${name} forwards explicit source or judgment input without invented actor and preserves historical numbers`,async()=>{
    let requests=0;
    const server=http.createServer((req,res)=>{
      requests++;assert.equal(req.url,'/api/v1'+path);assert.equal(req.method,'POST');
      assert.equal(req.headers['x-mulino-local-role'],'OPERATOR');assert.equal(req.headers.authorization,undefined);
      assert.equal(req.headers['idempotency-key'],args.requestKey);
      let body='';req.on('data',d=>body+=d);req.on('end',()=>{
        const sent=JSON.parse(body);for(const key of Object.keys(sent))assert.deepEqual(sent[key],args[key]);
        assert.equal(sent.userId,undefined);assert.equal(sent.runId,undefined);assert.equal(sent.caseRef,undefined);
        res.writeHead(200,{'Content-Type':'application/json'});res.end('{"claim_id":9007199254740993,"revision":1,"status":"CONFLICTED","content":"exact 0.1234567890123456789"}');
      });
    });
    const base=await listen(server);resources.push(()=>closeServer(server));
    const client=await connectClient({MULINO_API_BASE:base+'/api/v1',MULINO_LOCAL_ROLE:'OPERATOR'});
    const result=await client.callTool({name,arguments:args});assert.equal(result.isError,undefined);
    assert.equal(result.structuredContent.claim_id,'9007199254740993');assert.equal(result.structuredContent.status,'CONFLICTED');
    assert.equal(result.structuredContent.content,'exact 0.1234567890123456789');assert.equal(requests,1);
  });
}

test('human judgment needs revision fingerprint and rationale before touching the backend',async()=>{
  const client=await connectClient({MULINO_API_BASE:'http://127.0.0.1:1/api/v1',MULINO_LOCAL_ROLE:'OPERATOR'});
  for(const field of ['expectedRevision','evidenceFingerprint','reason']) {
    const args={caseRef:'CASE-1',claimId:1,status:'VERIFIED',expectedRevision:1,evidenceFingerprint:'a'.repeat(64),reason:'Explicit review'};delete args[field];
    assert.equal((await client.callTool({name:'judge_claim',arguments:args})).isError,true);
  }
});

test('new interpretation references its original claim with an explicit rationale and never judges automatically',async()=>{
  let requests=0;
  const server=http.createServer((req,res)=>{
    requests++;assert.equal(req.url,'/api/v1/epistemic/cases/CASE-1/claims');
    let body='';req.on('data',d=>body+=d);req.on('end',()=>{
      const sent=JSON.parse(body);assert.equal(sent.supersedesClaimId,'9007199254740993');
      assert.equal(sent.supersessionReason,'Reassess current corrected source');assert.equal(sent.status,undefined);
      res.writeHead(200,{'Content-Type':'application/json'});res.end('{"claim_id":2,"supersedes_claim_id":9007199254740993,"status":"ASSERTED"}');
    });
  });
  const base=await listen(server);resources.push(()=>closeServer(server));
  const client=await connectClient({MULINO_API_BASE:base+'/api/v1',MULINO_LOCAL_ROLE:'OPERATOR'});
  const args={caseRef:'CASE-1',subjectType:'ETA',subjectRef:'PO-1',claimText:'New interpretation',supersedesClaimId:'9007199254740993',supersessionReason:'Reassess current corrected source'};
  const result=await client.callTool({name:'create_claim',arguments:args});assert.equal(result.isError,undefined);
  assert.equal(result.structuredContent.status,'ASSERTED');assert.equal(result.structuredContent.supersedes_claim_id,'9007199254740993');assert.equal(requests,1);
  delete args.supersessionReason;assert.equal((await client.callTool({name:'create_claim',arguments:args})).isError,true);assert.equal(requests,1);
});
