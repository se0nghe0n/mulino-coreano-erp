import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { ProcessExecutor, DockerExecutor } from '../src/executor.js';
import { claudeDiagnostics, codexDiagnostics } from '../src/native-diagnostics.js';

const fixture = fileURLToPath(new URL('./fixtures/child.js', import.meta.url));
const claim = { runRef: 'RUN-1', caseRef: 'CASE-1', workItemRef: 'WI-1', agentKey: 'SUPPLY_CHAIN',
  capabilityToken: 'cap-secret', leaseToken: 'lease-secret', context: { objective: 'Exact objective' } };
const executor = (mode, extra = {}) => new ProcessExecutor({
  invocation: () => ({ command: process.execPath, args: [fixture, mode], env: { MULINO_TOKEN: 'cap-secret' } }), ...extra,
});

test('real child receives bounded context stdin and structured final result is parsed', async () => {
  const handle = executor('context').start(claim);
  assert.ok(handle);
  assert.deepEqual(await handle.result, { outcome: 'DONE', summary: 'Exact objective' });
});

test('real child gets explicit env only and known tokens are redacted from output', async () => {
  const handle = executor('env').start(claim, { secrets: ['cap-secret', 'lease-secret', 'secret-from-stderr'] });
  assert.ok(handle);
  const result = await handle.result;
  assert.doesNotMatch(result.summary, /cap-secret|lease-secret|MULINO_LOCAL_SERVICE_SECRET|HOME|PATH/);
  assert.match(result.summary, /REDACTED/);
});

for (const [mode, code] of [['bad', 'INVALID_MODEL_JSON'], ['oversize', 'MODEL_OUTPUT_TOO_LARGE'], ['fail', 'MODEL_PROCESS_FAILED']]) {
  test(`real child ${mode} fails with bounded safe diagnostic`, async () => {
    const handle = executor(mode, { maxOutputBytes: 4096 }).start(claim);
    assert.ok(handle);
    await assert.rejects(handle.result, error => error.code === code && !error.message.includes('secret-from-stderr'));
  });
}

test('cancel terminates actual process and rejects once', async () => {
  const handle = executor('hang').start(claim);
  assert.ok(handle);
  const rejected = assert.rejects(handle.result, error => error.code === 'MODEL_CANCELLED');
  await handle.cancel('SHUTDOWN');
  await rejected;
  assert.throws(() => process.kill(handle.pid, 0), error => error.code === 'ESRCH');
});

test('Docker invocation isolates mounts, environment and immutable runtime configuration', () => {
  const docker = new DockerExecutor({ image: 'mulino-runtime:local', authVolume: 'mulino-codex-auth',
    agentApiUrl: 'http://host.docker.internal:8080/api/v1' });
  const spec = docker.buildInvocation(claim);
  assert.ok(spec);
  assert.equal(spec.command, 'docker');
  assert.equal(spec.args.filter(v => v === '--mount').length, 1);
  assert.ok(spec.args.includes('--read-only'));
  assert.ok(spec.args.includes('--cap-drop=ALL'));
  assert.ok(spec.args.includes('--security-opt=no-new-privileges'));
  assert.ok(spec.args.includes('--user=10001:10001'));
  assert.ok(spec.args.includes('--output-schema'));
  assert.ok(spec.args.includes('--json'));
  assert.ok(spec.args.includes('-'));
  const containerEnv = spec.args.flatMap((arg, index) => arg === '--env' ? [spec.args[index + 1]] : []);
  assert.deepEqual(containerEnv, [
    'MULINO_TOKEN', 'MULINO_API_URL=http://host.docker.internal:8080/api/v1',
    'CODEX_HOME=/home/mulino/.codex', 'HOME=/home/mulino',
  ]);
  assert.doesNotMatch(JSON.stringify(spec.args), /cap-secret|lease-secret|docker\.sock|\/Users\/|--privileged|--network=host/);
  assert.deepEqual(Object.keys(spec.env).sort(), ['MULINO_TOKEN', 'PATH']);
  assert.equal(spec.env.MULINO_TOKEN, 'cap-secret');
});

test('Claude Code result event is the final structured result and its usage is kept', async () => {
  const handle = executor('claude').start(claim);
  assert.deepEqual(await handle.result, { outcome: 'DONE', summary: 'Exact objective', resultRef: 'PLAN-7' });
  assert.deepEqual(handle.usage, { resolvedModel: 'claude-sonnet-5', costUsd: 0.12, inputTokens: 10, outputTokens: 20, turns: 3 });
});

test('Claude Code error result fails the Run', async () => {
  await assert.rejects(executor('claude-error').start(claim).result, error => error.code === 'MODEL_PROCESS_FAILED');
});

 test('Codex turn usage is recorded exactly without inventing cost', async () => {
  const handle = executor('codex-usage').start(claim);
  assert.equal((await handle.result).outcome,'DONE');
  assert.deepEqual(handle.usage,{inputTokens:41,outputTokens:13,cacheReadTokens:7});
});

test('parent host credentials never enter native Docker invocation or agent context', async () => {
  const keys = ['MULINO_LOCAL_HUMAN_SECRET', 'MULINO_LOCAL_SERVICE_SECRET'];
  const previous = keys.map(key => process.env[key]);
  process.env.MULINO_LOCAL_HUMAN_SECRET = 'host-only-human-sentinel';
  process.env.MULINO_LOCAL_SERVICE_SECRET = 'host-only-service-sentinel';
  const excluded = /host-only-(human|service)-sentinel|MULINO_LOCAL_(HUMAN|SERVICE)_SECRET/;
  try {
    for (const runtime of ['CODEX', 'CLAUDE']) {
      const docker = new DockerExecutor({image:'mulino-runtime:local', authVolume:'mulino-test-auth', runtime});
      const spec = docker.buildInvocation(claim);
      assert.doesNotMatch(JSON.stringify({args:spec.args, env:spec.env, context:claim.context}), excluded);
    }
    const result = await executor('env').start(claim).result;
    assert.doesNotMatch(JSON.stringify(result), excluded);
  } finally {
    keys.forEach((key, index) => {
      if (previous[index] === undefined) delete process.env[key];
      else process.env[key] = previous[index];
    });
  }
});


test('native quota failure preserves reported partial usage and only fixed safe diagnostics', async () => {
  const handle = executor('claude-quota').start(claim);
  await assert.rejects(handle.result, error => error.code === 'MODEL_PROCESS_FAILED');
  assert.equal(handle.diagnostics.nativeResultSubtype, 'error_during_execution');
  assert.equal(handle.diagnostics.nativeFailureCategory, 'USAGE_LIMIT');
  assert.ok(handle.diagnostics.nativeExitCode === null || Number.isInteger(handle.diagnostics.nativeExitCode));
  assert.equal(handle.usage.costUsd, 0.05);
  assert.doesNotMatch(JSON.stringify(handle.diagnostics), /cap-secret|lease-secret|secret-from-stderr|errors/);
});

for (const [event, category] of [
  [{subtype:'error_max_turns',is_error:true}, 'TURN_LIMIT'],
  [{subtype:'error_max_budget_usd',is_error:true}, 'BUDGET_LIMIT'],
  [{subtype:'error_max_structured_output_retries',is_error:true}, 'SCHEMA_RETRIES'],
  [{subtype:'error_during_execution',is_error:true,error:{status:401,type:'authentication_error'}}, 'AUTHENTICATION'],
  [{is_error:true,error:{status:403}}, 'FORBIDDEN'],
  [{is_error:true,error:{status:429}}, 'RATE_LIMIT'],
  [{is_error:true,errors:['model does not exist or you do not have access']}, 'MODEL_ACCESS'],
  [{is_error:true,errors:['cap-secret unknown issue'],subtype:'cap-secret'}, 'UNKNOWN_NATIVE_FAILURE'],
]) test(`native ${category} classification does not invent quota or expose messages`, () => {
  assert.equal(claudeDiagnostics(event).nativeFailureCategory, category);
  assert.doesNotMatch(JSON.stringify(claudeDiagnostics(event)), /cap-secret|unknown issue|errors/);
});


test('Codex routing error is captured before cancellation without raw message or fabricated usage', async () => {
  const handle = executor('codex-routing-error').start(claim);
  await assert.rejects(handle.result, error => error.code === 'MODEL_PROCESS_FAILED');
  assert.equal(handle.diagnostics.nativeFrameKind, 'turn.failed');
  assert.equal(handle.diagnostics.nativeFailureCategory, 'WORKSPACE_ROUTING');
  assert.equal(handle.diagnostics.nativeHttpStatus, 401);
  assert.deepEqual(handle.usage, {});
  assert.doesNotMatch(JSON.stringify(handle.diagnostics), /cap-secret|lease-secret|secret-from-stderr|discovery unauthorized/);
});

for (const [error,category] of [
  [{code:'invalid_api_key',status:401},'AUTHENTICATION'],
  [{code:'model_not_found'},'MODEL_ACCESS'],
  [{code:'insufficient_quota'},'USAGE_LIMIT'],
  [{status:429},'RATE_LIMIT'],
  [{status:503},'PROVIDER_SERVER'],
  [{message:'unknown field in config cap-secret'},'CONFIGURATION'],
  [{message:'read-only file system cap-secret'},'FILESYSTEM'],
  [{message:'cap-secret unexplained error',code:'cap-secret'},'UNKNOWN_NATIVE_FAILURE'],
]) test(`Codex ${category} reports only allowed native diagnostics`, () => {
  const d=codexDiagnostics({type:'error',error});
  assert.equal(d.nativeFailureCategory,category);
  assert.doesNotMatch(JSON.stringify(d),/cap-secret|message|unexplained/);
});
