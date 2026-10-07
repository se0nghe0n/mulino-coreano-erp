import { generateKeyPairSync, sign, createHash } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

// Tokens/private material are never included in stdout or evidence.
const [mode, inputPath, outputPath] = process.argv.slice(2);
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
if (mode === 'prepare') {
  const dir = resolve(inputPath); mkdirSync(dir, {recursive:true, mode:0o700});
  const {privateKey, publicKey} = generateKeyPairSync('rsa', {modulusLength:2048});
  writeFileSync(`${dir}/public.pem`, publicKey.export({type:'spki', format:'pem'}), {mode:0o600});
  const now = Math.floor(Date.now()/1000);
  const base = {iss:'https://mulino.local.invalid', aud:'mulino-platform', sub:'writer-a',
    organizationId:'org-a', stableRequestOwner:'owner-a', iat:now, nbf:now-5, exp:now+7200};
  const jwt = claims => {const body = `${encode({alg:'RS256',typ:'JWT'})}.${encode(claims)}`;
    return `${body}.${sign('RSA-SHA256',Buffer.from(body),privateKey).toString('base64url')}`;};
  const tokens = {};
  for (const [name, patch] of Object.entries({writer:{},reader:{sub:'reader-a',role:'WRITE'},
      wrongIssuer:{iss:'https://untrusted.invalid'},wrongAudience:{aud:'another-api'},
      expired:{exp:now-300,nbf:now-600},notYetValid:{nbf:now+300},
      otherOrganization:{organizationId:'org-b',stableRequestOwner:'owner-b'},
      revoked:{sub:'revoked-a'},missingOrganization:{organizationId:null},
      missingOwner:{stableRequestOwner:null}})) tokens[name] = jwt({...base,...patch});
  const outsider = generateKeyPairSync('rsa', {modulusLength:2048});
  const body = `${encode({alg:'RS256',typ:'JWT'})}.${encode(base)}`;
  tokens.wrongSignature = `${body}.${sign('RSA-SHA256',Buffer.from(body),outsider.privateKey).toString('base64url')}`;
  writeFileSync(`${dir}/tokens.json`,JSON.stringify(tokens),{mode:0o600});
  console.log(JSON.stringify({publicKeyPath:`${dir}/public.pem`,tokenPath:`${dir}/tokens.json`}));
} else if (mode === 'run') {
  const config = JSON.parse(readFileSync(inputPath));
  if (!/^http:\/\/(localhost|127\.0\.0\.1):\d+$/.test(config.baseUrl)) throw Error('Local fixture URL required');
  const tokens = JSON.parse(readFileSync(config.tokenPath));
  const observations = [];
  async function request(route, tokenName, payload, extraHeaders={}) {
    const headers = {...extraHeaders};
    if (tokenName) headers.authorization = `Bearer ${tokens[tokenName]}`;
    if (payload !== undefined) headers['content-type'] = 'application/json';
    const response = await fetch(config.baseUrl+route.path,{method:route.method,headers,
      body:payload===undefined?undefined:JSON.stringify(payload),signal:AbortSignal.timeout(15000)});
    const raw = await response.text(); let body;
    try { body=JSON.parse(raw); } catch { body=null; }
    return {status:response.status,body,bodyHash:createHash('sha256').update(raw).digest('hex')};
  }
  for (const test of config.cases) {
    const before = test.snapshot ? await request(config.snapshot,'writer') : null;
    const actual = await request(test.route,test.token,test.payload,test.headers);
    const after = test.snapshot ? await request(config.snapshot,'writer') : null;
    const statusOk = test.status.includes(actual.status);
    const unchanged = before===null || (before.status===200 && after.status===200 &&
      JSON.stringify(before.body)===JSON.stringify(after.body));
    const bodyOk = (!test.bodyContains || JSON.stringify(actual.body).includes(test.bodyContains)) &&
      Object.entries(test.bodyEquals??{}).every(([pointer,value])=>{
        const observed=pointer.split('/').slice(1).reduce((obj,key)=>obj?.[key],actual.body);
        return JSON.stringify(observed)===JSON.stringify(value);
      });
    observations.push({id:test.id,method:test.route.method,path:test.route.path,status:actual.status,
      expectedStatus:test.status,responseHash:actual.bodyHash,expectedCode:test.bodyContains??null,expectedBody:test.bodyEquals??null,
      bodyCodeMatches:bodyOk,snapshotBefore:before?.body??null,snapshotAfter:after?.body??null,
      publicSnapshotUnchanged:unchanged,result:statusOk&&unchanged&&bodyOk?'PASS':'FAIL'});
  }
  const evidence = {contractVersion:1,at:new Date().toISOString(),node:process.version,
    command:'node verification/platform/security/security-smoke.mjs run <local-config> <evidence>',
    scope:'S0 public HTTP authentication and sequential grant/policy boundary only',
    observations,result:observations.every(o=>o.result==='PASS')?'PASS':'FAIL',
    notRun:['full T08/C3/V4','V6 receipt response loss/retry','V7 concurrency/restart','production IAS/IdP mapping']};
  writeFileSync(outputPath,JSON.stringify(evidence,null,2)+'\n');
  console.log(JSON.stringify({result:evidence.result,cases:observations.length,failed:observations.filter(o=>o.result==='FAIL').map(o=>o.id)}));
  if (evidence.result==='FAIL') process.exitCode=1;
} else throw Error('Usage: security-smoke.mjs prepare <temp-dir> | run <local-config> <evidence-path>');
