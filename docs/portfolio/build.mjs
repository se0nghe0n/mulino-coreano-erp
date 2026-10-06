import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

// Relative source/output paths. Runtime dependencies and private logs come from env.
const dir=path.dirname(fileURLToPath(import.meta.url));
const evidence=JSON.parse(await fs.readFile(path.join(dir,'evidence.json'),'utf8'));
const workspaceDir=path.resolve(process.env.PORTFOLIO_WORKSPACE ?? path.join(dir,'../..'));
const buildDir=path.resolve(process.env.PORTFOLIO_BUILD_DIR ?? path.join(workspaceDir,'.portfolio-build',`run-${Date.now()}`));
const outputDir=path.resolve(process.env.PORTFOLIO_OUTPUT_DIR ?? dir);
const skill=process.env.PRESENTATIONS_SKILL_DIR;
const python=process.env.RUNTIME_PYTHON;
if(!skill || !python) throw new Error('Set PRESENTATIONS_SKILL_DIR and RUNTIME_PYTHON to bundled runtime paths.');
const {finalizePresentation,applyPresentationChartFont}=await import(pathToFileURL(path.join(skill,'container_tools/artifact_tool_utils.mjs')).href);
await fs.mkdir(buildDir,{recursive:true});await fs.mkdir(outputDir,{recursive:true});
const font=process.env.PORTFOLIO_FONT ?? 'Apple SD Gothic Neo';
const C={bg:'#FAFAF6',ink:'#142B2C',muted:'#526667',accent:'#176957',line:'#CCD7D2',header:'#176957',light:'#EAF0EA',white:'#FFFFFF'};
const p=Presentation.create({slideSize:{width:1280,height:720}});
function txt(s,text,x,y,w,h,size=28,bold=false,color=C.ink){
 const sh=s.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{fill:'none',width:0}});
 sh.text=text;sh.text.style={typeface:font,fontSize:size,bold,color,autoFit:'none'};return sh;
}
function slide(title,notes){const s=p.slides.add();s.background.fill=C.bg;txt(s,title,72,42,1136,72,44,true);txt(s,String(p.slides.items.length).padStart(2,'0'),1160,659,48,28,19,false,C.muted);s.speakerNotes.textFrame.setText(notes);return s;}
function foot(s,text){txt(s,text,72,623,1070,48,21,false,C.muted);}
function table(s,values,y=153,widths=[330,380,426],height=418){
 const t=s.tables.add({rows:values.length,columns:values[0].length,left:72,top:y,width:1136,height,columnWidths:widths,values});
 t.borders.assign({style:'solid',fill:C.line,width:1});
 for(let r=0;r<values.length;r++)for(let c=0;c<values[0].length;c++){
  const cell=t.getCell(r,c);cell.fill=r===0?C.header:(r%2===0?C.light:C.bg);
  cell.text.style={typeface:font,fontSize:25,bold:r===0,color:r===0?C.white:C.ink};
 }
 return t;
}
function prose(s,rows,start=162){let y=start;for(const [head,body] of rows){txt(s,head,72,y,1136,42,30,true);txt(s,body,72,y+49,1136,67,27);y+=143;}}
function node(s,text,x,y,w=244,h=90){let n=s.shapes.add({geometry:'rect',position:{left:x,top:y,width:w,height:h},fill:C.light,line:{fill:C.accent,width:2}});n.text=text;n.text.style={typeface:font,fontSize:27,bold:true,color:C.ink};return n;}
function connect(s,x,y,w,h=0){s.shapes.add({geometry:'line',position:{left:x,top:y,width:w,height:h},fill:'none',line:{fill:C.accent,width:2}});}
const repo='https://github.com/se0nghe0n/mulino-coreano-erp';
const src=(f,sha=evidence.sourceBaseline)=>`${repo}/blob/${sha}/${f}`;
let s=slide('MULINO COREANO','가상 식품 제조 ERP 포트폴리오. 실제 Mulino Bianco 프로젝트나 SAP 제품 구현이 아니다. 기본 독자는 SAP/ERP 면접 평가자, 발표 시간은 10–15분이다. 2026-10-06 역사적 Supplier baseline 0132e0a, 현재 Claude 인수 source 4e6935d. 출처: README.md, docs/portfolio/evidence.json.');
txt(s,'한국 진출을 가정한 식품 ERP와\n인간 승인 기반 AI 업무 실행',72,182,1136,167,54,true);
txt(s,'분석 · 설계 · 구현 · 검증',72,403,1136,58,32,false,C.accent);
txt(s,'로컬 PoC  /  2026-10-06',72,519,1136,45,27,false,C.muted);

s=slide('As-Is / To-Be: 한국 현지화',`원래 비교표는 설계 가정이다. 법적 적합성 인증을 주장하지 않는다. 22개 allergen master를 19개 법정군으로 매핑한다. 필수 인증서 유형 전체와 자동 30일 알림 갭은 미등록 이슈 대기 상태다. 실제 MFDS 전송·법정 양식 검증은 수행하지 않았다. 출처: ${src('README.md')}, ${src('database/seed/allergens.sql')}, docs/16_decisions.md.`);
table(s,[['설계 항목','As-Is: EU 기준 가정','To-Be: 한국 PoC'],['알레르겐','EU 14종 기준','22개 master / 19개 법정군'],['인증서','HACCP / BRC / IFS','HACCP / GMP / 이력추적등록'],['리콜 보고','EU 기반 보고 절차','즉시 보고용 OFFLINE 초안'],['보관 정책','EU 기반 보관 기간','소비기한 + 2년 보관 모델'],['세금계산서','기존 EU 설계','번호·날짜 컬럼'] ],145,[230,390,516],422);
foot(s,'규제 설계와 실제 신고·법적 적합성 검증은 별도다. 인증서 자동화 갭이 남아 있다.');

s=slide('SAP 모듈 대응',`SAP 업무 개념과 가상 ERP 테이블의 대응이다. 실제 SAP 연동, SAP 표준 전체 기능 구현, FI 전표 처리를 주장하지 않는다. 출처: ${src('README.md')}, ${src('docs/01_project_overview.txt')}, ${src('docs/03_erd.md')}.`);
table(s,[['업무','SAP 개념','구현 데이터·경계'],['구매·입고','MM: ME21N / MIGO','suppliers, purchase_orders, inbound'],['창고·LOT','EWM / Batch Management','warehouses, stock, raw_material_lots'],['생산·BOM','PP','production_lots, bom_versions'],['수주·출고','SD: VA01','orders, outbound, outbound_lots'],['품질·리콜','QM','inbound_inspections, recalls'],['승인·감사','GRC','governance_actions, audit logs']],145,[220,370,546],432);
foot(s,'SAP 모듈 개념을 설계에 대응했다. 실제 SAP 연동 및 전체 모듈 구현 범위는 아니다.');

s=slide('구현 아키텍처와 MONITOR 표면',`역사적 Supplier baseline ${evidence.sourceBaseline}, 현재 accepted production source ${evidence.nativeUat.currentAccepted.acceptedProductionSource}. backend/read/role 변경을 포함한다. DDL 61개 CREATE TABLE: 원래 ERP 30 + interface 13 + 후속 18. 별도 governance/는 scaffold이며 승인 게이트는 backend에 구현한다. MONITOR reads는 ERP 승인 없이 진행하되 채널 인증과 Case scope를 적용한다. 공유 local role/key는 개인별 운영 IAM이 아니다. #24/#25 현재 Claude five-case 인수는 PASS다. #35 actual client 인수는 남아 있다. 출처: docs/16_decisions.md, ${src('mcp-server/src/tools.js')}, ${src('backend/src/main/java/com/mulinocoreano/backend/security/LocalSecurityConfiguration.java')}, ${src('agents/cli/README.md')}.`);
node(s,'대화 MCP\nASK / ACT / MONITOR',72,156,350,96);node(s,'native runner\nClaude Code / Codex',462,156,350,96);node(s,'Zig CLI\nmulino',852,156,350,96);
connect(s,247,252,0,68);connect(s,637,252,0,68);connect(s,1027,252,0,68);
node(s,'L1  백엔드 도메인 승인·불변 감사',72,320,1130,87);connect(s,637,407,0,47);node(s,'L0  Spring Boot REST API · PostgreSQL 18 · 61개 테이블',72,454,1130,87);
foot(s,'L2는 동일 API를 호출한다. 전용 화면·OAuth 제외. 능동 알림·실제 클라이언트 인수는 남아 있다.');

s=slide('지속하는 Case와 인간 승인',`요청한 편집 가능한 업무 흐름도. Case·Work Item·Run을 별도로 영속화하며 대기 중 모델을 유지하지 않는다. 승인 이벤트 payload 자체를 권위로 믿지 않고 DB 인간 결정과 scope를 재검증한다. 조회에 ERP approval은 요구하지 않지만 local 인증과 scope를 적용한다. 출처: ${src('docs/08_interface_overview.md')}, ${src('docs/14_human_purchase_api.md')}, ${src('docs/14_cli_and_runtime.md')}.`);
node(s,'ACT 목표\nCase 생성',72,173);connect(s,316,218,49);node(s,'담당 Work Item\nRun 실행',365,173);connect(s,609,218,49);node(s,'인간 판단 필요\nAttention 대기',658,173);connect(s,902,218,49);node(s,'답변·승인\nEvent 기록',951,173);
txt(s,'모델 실행은 종료해도 업무 책임과 재개 근거는 DB에 남는다',72,343,1136,74,36,true);
txt(s,'구매 MANAGER   /   입고 QC   /   리콜 ADMIN',72,459,1136,53,30,false,C.accent);
foot(s,'조회는 Case를 만들지 않는다. 승인·반려·멱등 replay의 결과를 ERP 상태로 검증한다.');

s=slide('구매 데모: 제안과 발주 사이의 승인',`source d7ce2d8의 #34 demo 5/5 기록. 최신 Supplier 통합 SIT의 P2P 6개와 별도 증거다. 실제 human stdio MCP, scripted agent와 현재 native UAT를 혼동하지 않는다. 출처: docs/reviews/2026-10-05-purchase-acceptance.md, docs/15_demo_runbook.md, docs/16_scenario_tests.md, ${src('docs/14_human_purchase_api.md')}.`);
prose(s,[['1. 순소요와 공급처를 계산한다','수요·BOM·가용 LOT·예정 입고·MOQ를 반영한 후보를 저장한다.'],['2. MANAGER가 구매 제안을 판단한다','승인 전 PO는 없다. 반려는 ERP를 변경하지 않는다.'],['3. 결정 뒤에도 동일 Case를 이어간다','발주 적용과 후속 업무를 연결하고 중복 요청은 같은 결과를 반환한다.']]);
foot(s,'#34 데모 5/5는 source d7ce2d8 기록이다. 현재 모델 UAT 결과는 별도 인수 자료다.');

s=slide('입고 품질: QC 판단을 적용한다',`scripted SIT QM 6건: 승인·반려·잘못된 역할·인증서/알레르겐·온도/Case 경계를 실제 business state로 확인한다. 현재 Claude native QM PASS는 QC 승인안 PENDING, 입고 HOLD·잔량 2·적용 0과 native WAITING/receipt WAITING, 완전 사용량을 확인한다. canonical 22 master / 19 legal categories. Certificate all-required-type and automatic 30-day notices remain gap; don't claim Goal6 complete. 출처: ${src('docs/18_inbound_quality_api.md')}, ${src('backend/src/test/resources/scenarios/qm-inbound-inspection.feature')}, ${src('database/seed/allergens.sql')}.`);
prose(s,[['입고는 HOLD 상태에서 시작한다','온도·알레르겐·인증서 근거를 같은 Case에 보관한다.'],['QC만 승인·보류·차단을 판단한다','역할 밖 요청과 재사용한 결정으로 ERP를 변경하지 못한다.'],['판단과 적용 상태를 감사한다','scripted QM SIT 6건 통과. native QM은 QC PENDING·HOLD·잔량 2·적용 0으로 PASS다.']]);
foot(s,'인증서 필수 유형 전체 검사와 자동 30일 알림은 미완료다. 규제 Goal 6 전체 인수를 주장하지 않는다.');

s=slide('LOT 추적과 리콜 범위',`현재 JAR proof, source ${evidence.recall.commit}, JAR SHA ${evidence.recall.jarSha256}. 실제 trace: production LOT 10, incident roots 2, evidence raw LOT 3, customer 2, outbound shipments 115. Third raw lot is traced evidence, not third incident root. Unrelated production LOT stays active. Approval replay stable. Report PENDING/OFFLINE with submittedAt null, no MFDS transmission. 출처: ${src('docs/02_flow.md',evidence.recall.commit)}, ${src('database/ddl/22_recall_trace.sql',evidence.recall.commit)}, evidence.json의 recall 및 ownerReport provenance.`);
table(s,[['추적 단계','현재 JAR 관측','업무 결과'],['사고 원료 LOT','2개 사고 root / 3개 증거 raw','관련 관계를 함께 보존'],['생산 LOT','10개','ADMIN 승인으로 10개 리콜'],['고객·출고','2개 고객 / 115개 출고','고객과 출고 범위를 추적'],['무관 LOT','범위 밖','ACTIVE 유지'],['보고 초안','OFFLINE / PENDING','submittedAt = null']],145,[310,390,436],425);
foot(s,'리콜 replay는 안정적이다. 식약처 실제 전송과 법정 양식 검증은 수행하지 않았다.');

s=slide('증거와 Claim 판단의 이력',`source ${evidence.evidence.commit}, JAR SHA ${evidence.evidence.jarSha256}. current JAR + Zig CLI + SDK human stdio proof: source 3, judgment 7, ERP writes 0. Support relation does not verify. Human explicit attestation sets VERIFIED; new link/correction stales judgment, history append-only. Explicit successor keeps original conflicted claim. 출처: ${src('docs/19_evidence_claim_api.md',evidence.evidence.commit)}, evidence.json.`);
prose(s,[['출처를 등록하고 주장을 연결한다','현재 JAR에서 source 3개와 judgment 7개를 관측했다.'],['인간이 판단하고 반증을 보존한다','충돌 Claim은 남기고 별도 successor를 명시적으로 판단한다.'],['정정은 기존 판단을 stale로 만든다','VERIFIED는 인간 attestation이다. 독립적 진실이나 ERP 권한을 뜻하지 않는다.']]);
foot(s,'증거·판단 proof의 ERP 쓰기는 0건이다. 반증 연결만으로 자동 검증하지 않는다.');

s=slide('Supplier master의 변경 통제',`source ${evidence.supplier.commit}, JAR SHA ${evidence.supplier.jarSha256}. Current built JAR CRUD5/Swagger5, auditCount 3, canonicalReplay, version SUP002, missing SUP001, human-role checks. Soft delete preserves planning references/history; backend immutable audit. Fresh DDL/Flyway structural match 756 constraints /124 triggers. 출처: ${src('docs/21_supplier_master_api.md')}, ${src('database/ddl/24_supplier_master.sql')}, evidence.json의 supplier.`);
table(s,[['업무 위험','구현 통제','현재 로컬 증거'],['잘못된 인간 역할','역할·capability 검증','혼합·위조 credentials 거부'],['동시 변경','version과 resource lock','충돌 SUP002'],['재시도','canonical receipt','동일 요청 replay 안정'],['삭제 후 이력','soft delete','계획 참조와 감사 보존'],['API 탐색','Swagger 계약','CRUD 5 / Swagger 5 확인']],145,[280,390,466],425);
foot(s,'인증서 자동화 확장·OAuth·실모델 Supplier UAT는 이 구현에 포함하지 않는다.');

s=slide('검증 결과와 증거 계층',`현재 accepted source ${evidence.nativeUat.currentAccepted.acceptedProductionSource}. Full backend checkpoint는 최종 helper·prose CANCEL 교정 전 587 발견/18 제외/569 active PASS 및 bootJar PASS다. 처음 scripted SIT 19/20의 CANCEL 실패는 generic helper가 BLOCK만 허용한 오류이며 보존했다. 최종 helper·prose 교정 뒤 focused22 active/18excluded, full scripted SIT20,bootJar,runner84 PASS다. Full backend는 이 교정만을 위해 재실행하지 않았다. 모델 없는 image smoke/cancellation PASS,modelRequests0. CLI/native/lock bytes는 compact image와 같다. compact17560f2 checkpoint backend562active/SIT20/MCP26/CLI34/Zig7. 과거 fe6a51f551active/SIT20와 Supplier0132 549active는 별도다. 현재 실제 Claude5PASS/21complete native runs와 scripted 검증을 구분한다. 출처: evidence.json nativeUat.currentAccepted.verification, native/manager-stop-1006의 해시로 식별한 담당자 보고서. 원본 로그는 private acceptance bundle에 보존한다.`);
const ch=s.charts.add('bar',{position:{left:72,top:164,width:650,height:389},categories:['P2P','QM','RC','재시작'],series:[{name:'scripted SIT 통과',values:[6,6,6,2],fill:C.accent}],barOptions:{direction:'column',grouping:'clustered',gapWidth:85},hasLegend:false,dataLabels:{showValue:true,position:'outEnd',textStyle:{fontSize:24}},xAxis:{textStyle:{fontSize:24}},yAxis:{minimumScale:0,maximumScale:8,majorUnit:2,textStyle:{fontSize:22},numberFormatCode:'0'},chartFill:C.bg,plotAreaFill:C.bg});
applyPresentationChartFont(ch,{fontFamily:font});
txt(s,'569건 실행 통과',775,165,430,70,38,true);txt(s,'587건 발견 / 18건 제외\n최종 helper·prose 교정 전',775,245,430,87,26);
txt(s,'최신 native harness 검증',775,355,430,52,30,true);txt(s,'focused 22 / 제외 18\nSIT 20 / runner 84 / bootJar\nfull backend 재실행 없음',775,417,430,127,26);
foot(s,'scripted SIT 20건과 JAR·CLI·stdio proof를 확보했다. 실제 모델·클라이언트·운영 인수는 구분한다.');

s=slide('현재 Claude native UAT: 다섯 업무 PASS',`현재 clean accepted production source ${evidence.nativeUat.currentAccepted.acceptedProductionSource}, CLAUDE/claude-sonnet-5. image ${evidence.nativeUat.currentAccepted.imageSha256}, JAR ${evidence.nativeUat.currentAccepted.backendJarSha256}. 역사적 Supplier0132와 backend/read/role이 달라 runtime unchanged를 주장하지 않는다. P2P001 approved PO1/16500/originalCoordDONE. P2P002 final immutable MANAGER BLOCK/PO0/no reissue/nativeABORTED=receiptABORTED/originalWorkBLOCKED/CaseOPEN/backendJUDGMENTAttention retained. P2P004 oldEXPIRED/PO0 then explicit human recalc/newapprove PO1/17000/originalCoordDONE. QM PENDINGQC/HOLD/quantity2/zeroapplied. RC PENDINGADMIN/raw3/production10/customer2/shipments115/zeroapplied/OFFLINE. 21 unique model_finished/native runs exit0, accountingCOMPLETE/usagecomplete/receiptmatch/denial0. Thisbatch displayed costUSD2.9853428, exactJSONDecimal2.98534280000000011 contains float representation; unknowncost0 onlythisbatch. Historical totals remainunknown. Codex supported account/read refreshTokentrue returned accountnull/requiresOpenaiAuthtrue; rateAUTHENTICATION, no login/start/model calls. Earlier parityFAIL causeunknown. Source: evidence.json nativeUat.currentAccepted and SHA-256 provenance for five-case-summary/per-case JSONs and immutable-provenance-work-attention.jsonl.`);
table(s,[['runtime·Case','현재 판정·업무 결과','보고 비용 USD'],['Claude P2P-001','PASS. 승인 PO 1 · 16,500원, 조정 DONE','0.6780952'],['Claude P2P-002','PASS. MANAGER BLOCK, PO 0 · ABORTED','0.7219284'],['Claude P2P-004','PASS. 만료 후 인간 재계산 · 새 PO 17,000원','1.3771636'],['Claude QM-001','PASS. QC PENDING · HOLD 2 · 적용 0','0.1040890'],['Claude RC-001','PASS. ADMIN PENDING · 적용 0 · OFFLINE','0.1040666'],['Codex 접근 gate','account null · 인증 필요, parity 미완료','unknown']],145,[275,650,211],432);
foot(s,'현재 batch USD 2.9853428. 21 Run 완전 사용량·receipt 일치·denial 0. 과거 전체 비용 unknown.');

s=slide('범위와 남은 인수 경계',`명시한 limits는 source의 의미를 바꾸는 material caveats다. 범위 제외: OAuth and dedicated dashboard. No planned future OAuth promise. Local role/key not individual production IAM. Goal6 gap not issue implemented. MFDS no transmission/form validation. #35 current pending. 출처: docs/16_decisions.md, docs/portfolio/README.md, evidence.json.`);
prose(s,[['전용 화면과 OAuth를 제외한다','MONITOR는 대화 MCP로 조회한다. 능동 알림은 별도 채널 과제다.'],['규제 모델과 실제 신고를 구분한다','22개 알레르겐·보관 모델을 구현했다. 인증서 자동화와 실제 MFDS 신고는 남아 있다.'],['로컬 승인과 운영 신원을 구분한다','공유 local role/key를 사용한다. 개인별 운영 IAM·운영 배포·실제 클라이언트 인수는 미완료다.']]);
foot(s,'VERIFIED는 인간 attestation이다. 법적 적합성 인증·외부 사실 검증을 주장하지 않는다.');

s=slide('보존한 실패 이력과 남은 인수 gate',`현재 Claude5PASS와 과거 인수·진단을 구분한다. Original source2d2a900 2PASS/2FAIL/1INCOMPLETE, recovery3FAIL/zeroERP, canonicalQM native finalization failure and compact17560f2 4PASS/1FAIL retained. Compact002 MANAGER BLOCK kept PO0 but native FAILED due to ambiguous guidance; never convert historical FAILED to ABORTED. Current source provides parent/plan/action/finaldecision and forward-only immutable actor snapshot; legacyUNKNOWN/no backfill. Backend approval and Attention policies unchanged. Minimal Claude/schema newer diagnostics succeeded but are not business acceptance. Earlier failures exact causeunknown. Historical knownCLI subtotal12.662505 preserved, overallcost/tokensunknown. Currentbatch2.9853428/unknowncost0 does not make historical overall complete. Codex prior native parityFAIL causeunknown; supported accountread refreshTokentrue currentnull/requiresAuthtrue/rateAUTH, no login/modelcalls. Actualclient35 pending, OAuth/dashboard excluded, certificate fulltype/auto30day untracked awaiting registration; no MFDS actualtransmission/formvalidation, no productiondeploy/individualIAM/PowerPoint app validation. 출처: evidence.json의 original current/recoveryAttempt와 currentAccepted, docs/15_demo_runbook.md, docs/16_scenario_tests.md.`);
prose(s,[['원래 실패와 이후 재시도를 보존한다','원래 2 PASS·2 FAIL·1 INCOMPLETE, Recovery 3 FAIL, compact 4 PASS·1 FAIL을 남긴다.'],['현재 Claude 업무 인수와 진단을 구분한다','현재 다섯 PASS는 업무·native 종료를 검증했다. 최소 Claude/schema 진단은 별도다.'],['Codex와 실제 클라이언트 인수가 남아 있다','Codex account 인증 gate와 #35를 해소하지 않았다. 규제·운영 인수는 별도다.']]);
foot(s,'이전 실패 원인·역사적 전체 비용은 unknown이다. 현재 five-case PASS로 과거 기록을 덮어쓰지 않는다.');

const candidate=path.join(buildDir,'candidate.pptx');const final=path.join(outputDir,'mulino-coreano-portfolio-ko.pptx');
await (await PresentationFile.exportPptx(p)).save(candidate);
const result=await finalizePresentation({workspaceDir,candidatePath:candidate,finalPath:final,pythonExecutable:python,integrityValidatorPath:path.join(skill,'container_tools/inspect_presentation_package_integrity.py'),layoutValidatorPath:path.join(skill,'container_tools/inspect_presentation_layout_geometry.py'),layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-heading-fit',...[2,3,8,10,12].flatMap(n=>['--require-native-table-slide',String(n)])],explicitTotalSlideCount:14,requiredNativeTableOwnerSlides:[2,3,8,10,12],requiredNativeChartOwnerSlides:[11],materializeLiteralChartWorkbooks:true,fontPolicy:{basis:'design',families:[font]},verifyArtifactToolImport:true,receiptPath:path.join(buildDir,'validation.json')});
await fs.writeFile(path.join(buildDir,'result.json'),JSON.stringify(result,null,2));
console.log(JSON.stringify({pptx:final,slides:p.slides.items.length,font,receipt:path.join(buildDir,'validation.json')}));
