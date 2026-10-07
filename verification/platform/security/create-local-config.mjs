import {writeFileSync} from 'node:fs';
const [tokenPath,outputPath,baseUrl='http://localhost:8080'] = process.argv.slice(2);
if (!tokenPath || !outputPath) throw Error('tokenPath and outputPath required');
const scopeId='00000000-0000-0000-0000-000000000001';
const get={method:'GET',path:`/api/platform/scopes/${scopeId}`};
const list={method:'GET',path:'/odata/v4/platform/Scopes'};
const rest={method:'POST',path:'/api/platform/actions/reserve'};
const odata={method:'POST',path:'/odata/v4/platform/reserve'};
const mcp={method:'POST',path:'/mcp'};
const cases=[];
for(const token of [null,'wrongSignature','wrongIssuer','wrongAudience','expired','notYetValid','missingOrganization','missingOwner'])
 cases.push({id:`rest-auth-${token??'anonymous'}`,route:get,token,status:[401]});
cases.push({id:'rest-no-grant-read',route:get,token:'noGrant',status:[403]});
cases.push({id:'odata-no-grant-list',route:list,token:'noGrant',status:[200],bodyEquals:{'/value':[]}});
cases.push({id:'odata-other-org-list',route:list,token:'otherOrganization',status:[200],bodyEquals:{'/value':[]}});
cases.push({id:'rest-valid-read',route:get,token:'writer',status:[200]});
cases.push({id:'rest-other-org',route:get,token:'otherOrganization',status:[403]});
const payload=(key)=>({scopeId,quantity:'1',expectedRevision:0,idempotencyKey:key});
for(const token of ['reader','revoked']) {
 const key=`security-${token}-rest`;
 cases.push({id:`rest-${token}-denial`,route:rest,token,payload:payload(key),status:[403],snapshot:true});
}
cases.push({id:'rest-payload-role-forgery',route:rest,token:'reader',
 payload:{...payload('security-forged-role'),role:'ADMIN',actorId:'writer-a',organizationId:'org-a'},status:[403],snapshot:true});
for(const route of [odata,mcp]) cases.push({id:`anonymous-${route.path}`,route,status:[401],payload:{}});
cases.push({id:'odata-reader-denial',route:odata,token:'reader',payload:payload('security-reader-odata'),status:[403],snapshot:true});
const headers={'accept':'application/json, text/event-stream','MCP-Protocol-Version':'2026-07-28',
 'Mcp-Method':'tools/call','Mcp-Name':'platform.reserve'};
cases.push({id:'mcp-reader-denial',route:mcp,token:'reader',headers,status:[200],bodyEquals:{'/result/isError':true,'/result/structuredContent/outcome':'DENIED'},snapshot:true,
 payload:{jsonrpc:'2.0',id:'security-reader-mcp',method:'tools/call',params:{name:'platform.reserve',
 arguments:payload('security-reader-mcp'),_meta:{'io.modelcontextprotocol/protocolVersion':'2026-07-28',
 'io.modelcontextprotocol/clientInfo':{name:'security-probe',version:'1.0.0'},'io.modelcontextprotocol/clientCapabilities':{}}}}});
// The valid same-input write is deliberately last so rejected writes all see revision0.
cases.push({id:'rest-authorized-counter-call',route:rest,token:'writer',payload:payload('security-authorized-rest'),status:[200]});
cases.push({id:'odata-authorized-counter-call',route:odata,token:'writer',payload:{...payload('security-authorized-odata'),expectedRevision:1},status:[200]});
const mcpCounter=structuredClone(cases.find(test=>test.id==='mcp-reader-denial'));
mcpCounter.id='mcp-authorized-counter-call';mcpCounter.token='writer';mcpCounter.snapshot=false;
mcpCounter.bodyEquals={'/result/isError':false};mcpCounter.payload.id='security-authorized-mcp';
mcpCounter.payload.params.arguments={...payload('security-authorized-mcp'),expectedRevision:2};
cases.push(mcpCounter);
writeFileSync(outputPath,JSON.stringify({baseUrl,tokenPath,snapshot:get,cases},null,2)+'\n',{mode:0o600});
console.log(JSON.stringify({cases:cases.length,configPath:outputPath}));
