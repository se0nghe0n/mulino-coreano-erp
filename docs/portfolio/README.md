# Mulino Coreano 발표 자료

한국 진출을 가정한 식품 ERP의 현지화 설계, SAP 업무 개념 대응,
인간 승인과 지속 Case, 구매·품질·리콜 데모를 설명한다. 실제 기업의
프로젝트나 SAP 시스템 연동을 주장하지 않는다.

## 산출물과 발표 조건

- [편집 가능한 한국어 발표 원본](mulino-coreano-portfolio-ko.pptx)
- [배포용 PDF](mulino-coreano-portfolio-ko.pdf)
- [재생성 source](build.mjs)
- [해시와 검증 계층을 기록한 evidence](evidence.json)

14개 슬라이드와 각 슬라이드의 발표자 노트를 제공한다. 질문에 응답이
없어 적용한 작업 기본값은 SAP/ERP 면접 평가자, 10–15분 발표,
절제한 표·업무 흐름도다. native text·table·chart·diagram을 사용하며
사진이나 생성한 공장 이미지를 증거로 사용하지 않았다.

## 발표 흐름과 데모 근거

| 슬라이드 | 내용 | 근거 |
|---|---|---|
| 1–3 | 목적, As-Is/To-Be, SAP 모듈 대응 | README, 초기 기획안, ERD |
| 4–5 | 3계층·MONITOR, Case·Run·인간 승인 | 인터페이스·실행·구매 계약 |
| 6 | 구매 계산·제안·승인·재개 | #34 source d7ce2d8, 구매 인수 기록 |
| 7 | HOLD·QC·품질 판정 | 품질 API, QM SIT |
| 8 | 사고 원료와 LOT·고객·리콜 범위 | #27 현재 JAR proof |
| 9 | 증거·충돌·명시적 successor | #44 현재 JAR·CLI·stdio proof |
| 10 | Supplier 변경·replay·감사 | #41 현재 JAR·Swagger proof |
| 11–12 | 검증 계층, 과거·현재 native UAT | 아래 evidence 표 |
| 13–14 | 제외 범위·규제 갭, 데모 순서·인수 경계 | 범위 결정·runbook |

[데모 runbook](../15_demo_runbook.md),
[시나리오](../16_scenario_tests.md),
[테스트 분류](../17_test_classification.md)를 실행 절차의 기준으로 삼는다.
현재 five-case checkpoint를 해당 문서의 최신 결과에 함께 기록했다.

## source와 검증 계층

역사적 Supplier 통합 baseline은 `0132e0a`이며 `sourceBaseline`으로
보존한다. 현재 실제 모델이 실행하고 인수한 production source는
`4e6935de693b433755e12b4ffcf0197297a6ae31`이다. backend JAR와 agent read
view·role 안내를 보강했으므로 0132의 runtime이 그대로라고 주장하지
않는다. 현재 JAR SHA-256은
`11d73935589fee336f6011d5a034e497814bbc917f759e829024e02e59604f2d`다.
image는 `mulino-agent-runtime:manager-proof-1006`, digest는
`sha256:143e19e38f669ecd6bc0f71c4396008140701e7e57a69c52f9e5f88e520f1567`다.
CLI·native executable·lock bytes는 직전 compact image와 같다.

`evidence.json`의 `nativeUat.current`는 원래 2d2a900 인수와 recovery
이력이다. 새 `nativeUat.currentAccepted`는 현재 five-case checkpoint다.
기존 기록과 hash를 덮어쓰지 않는다. fe6a51f의 통합 551건·SIT 20건과
Supplier 0132의 549건은 과거 checkpoint로 유지한다.

4e6935d 보강 과정에서 backend 587건 발견·18건 제외·569건 실행과
bootJar가 통과했다. 이후 공용 stop helper가 MANAGER CANCEL을 거부한
SIT 19/20 실패를 보존하고 helper·prose만 교정했다. 최종 focused 22건
실행·18건 제외, 전체 scripted SIT 20건, bootJar와 runner 84건이
통과했다. 최종 helper·prose 교정 뒤 full backend를 다시 실행하지 않았다.
모델 없는 image smoke·cancellation은 요청 0건으로 통과했다.

모든 수치는 이미 실행한 담당자의 보고서에서 옮겼다. 문서 작업을 위해
백엔드 테스트나 유료 모델을 다시 실행하지 않았다.

| 계층 | 실제 관측 | source·한계 |
|---|---|---|
| 과거 통합 실행 테스트 checkpoint | 569 발견, 18 skip, 551 통과, 실패·오류 0 | fe6a51f, clean test bootJar sitTest |
| 과거 통합 scripted SIT checkpoint | 20 통과, 실패·오류·skip 0 | fe6a51f, 모델 호출 0 |
| 과거 Supplier 실행 테스트 | 567 발견, 18 skip, 549 통과, 실패 0 | 0132e0a |
| Supplier scripted SIT checkpoint | P2P 6, QM 6, RC 6, 재시작 2, 합계 20 통과 | Supplier 통합 baseline |
| 도구 단위·smoke | MCP 26, runner 57, Zig 7, CLI 32 통과 | #44 0ee144c의 별도 기록 |
| 과거 source 리콜 JAR proof | 생산 LOT 10, 사고 원료 root 2, 증거 raw LOT 3 | #27 5911721 |
| 리콜 범위 | 고객 2, 출고 115, 무관 LOT ACTIVE, replay 안정 | OFFLINE/PENDING, submittedAt null |
| 과거 source 증거 JAR proof | source 3, judgment 7, ERP 쓰기 0 | #44 0ee144c, 충돌·successor 보존 |
| Supplier baseline JAR proof | CRUD 5, Swagger 5, 감사 3, canonical replay | soft delete·version 충돌 확인 |
| 구매 데모 | 5/5 | #34 d7ce2d8, 실제 인간 stdio, 모델 호출 없음 |
| 과거 native UAT | 10월 3일 Claude 3건 통과 | cb04403. 현재 source 인수로 이월하지 않음 |
| 원래 Claude native UAT | PASS 2, FAIL 2, INCOMPLETE 1 | source 2d2a900, 이력 보존 |
| 후속 Recovery 3 | P2P-004·QM-001·RC-001 모두 FAIL, ERP 쓰기 0 | source e61ded2, 인수 회복 없음 |
| Codex parity | P2P-001 FAIL, 제안 전 실행 실패 | 비용·tokens·resolvedModel unknown |
| 현재 source backend checkpoint | 587 발견, 18 제외, 569 실행 통과 | 최종 helper·prose CANCEL 교정 전 |
| 최종 source focused·SIT·도구 | focused 22, 제외 18, SIT 20, runner 84 통과 | bootJar·요청 0 image smoke 통과 |
| 현재 Claude native UAT | 5 PASS, 21 complete Run, receipt 일치, denial 0 | 4e6935d, QC·리콜은 승인 대기 |
| 실제 클라이언트 #35 | 인수 대기 | 로컬 stdio 구현과 별도 |
| 운영 배포 | 미실시 | 로컬 PoC 범위 |

`evidence.json`은 담당자 원본 보고서의 SHA-256과 공개 가능한 결과를
보관한다. 원본 전체 로그와 diagnostic/실패 시도는 coordinator의 비공개
인수 bundle에 보존한다. capability·human/service secret, 인증 volume,
개인 파일 경로는 발표 원본·노트·source·evidence에 복사하지 않았다.

### 현재 Claude five-case 인수와 접근 gate

CLAUDE / `claude-sonnet-5`는 현재 source에서 다섯 업무를 모두 통과했다.

| Case | 현재 판정과 업무 결과 | 보고 비용 USD |
|---|---|---:|
| P2P-001 | PASS. 승인 PO 1·16,500원, 원본 조정 책임 DONE | 0.6780952 |
| P2P-002 | PASS. final MANAGER BLOCK, PO 0·재발행 없음, native·receipt ABORTED | 0.7219284 |
| P2P-004 | PASS. 옛 제안 EXPIRED·PO 0, 명시적 인간 재계산·새 승인 PO 1·17,000원 | 1.3771636 |
| QM-001 | PASS. QC 승인안 PENDING, 입고 HOLD·잔량 2, 적용 0 | 0.1040890 |
| RC-001 | PASS. ADMIN 승인안 PENDING, raw 3·생산 10·고객 2·출고 115, 적용 0 | 0.1040666 |

21개 고유 Run과 model_finished가 일치한다. native exit 0, COMPLETE
사용량, native·stored receipt outcome 일치와 permission denial 0을
확인했다. P2P-002의 원본 조정 Work Item은 BLOCKED, Case는 OPEN이다.
서버의 JUDGMENT_REQUIRED Attention도 유지한다. immutable actor snapshot은
현재 parent·plan·action·final MANAGER BLOCK을 연결한다. legacy UNKNOWN은
소급 보정하지 않는다. backend 승인·ABORTED Attention 정책은 바꾸지 않았다.
QC·리콜은 인간 승인 대기에 도달한 인수이며 실제 적용·MFDS 전송은 없다.

이번 batch 보고 비용 합계는 USD 2.9853428, unknown 비용은 0건이다.
원본 JSON Decimal 합계 `2.98534280000000011`은 float 표현을 포함한다.
과거 미보고 시도의 전체 비용·tokens는 여전히 unknown이다.

Codex의 지원되는 account/read(refreshToken=true)는 account=null,
requiresOpenaiAuth=true이며 rateLimits는 AUTHENTICATION이다. login이나
새 모델 호출은 하지 않았다. 현재 gate를 과거 Codex FAIL의 정확한 원인으로
소급하지 않는다. Codex parity·실제 클라이언트 #35·규제·운영 인수는 남아 있다.

### 원래 native 인수와 비용 이력

원래 source 2d2a900의 CLAUDE / `claude-sonnet-5` 업무 인수는 두 PASS다.

| Case | 원래 판정과 업무 결과 | CLI 보고 비용 USD |
|---|---|---:|
| P2P-001 | PASS. PO 1·16,500원, coordinator DONE·Case WAITING, 5 Run 사용량 완전 | 3.0874898 |
| P2P-002 | PASS. MANAGER BLOCK, PO·followup 0, coordinator ABORTED·Case OPEN, 5 Run 사용량 완전 | 3.2866446 |
| P2P-004 | FAIL. PO 1·17,000원이지만 마지막 coordinator MODEL_PROCESS_FAILED, usageComplete=false | 6.2873046 |
| QM-001 | FAIL. 검사 제안 전 QC 실행 실패, resolvedModel 없음 | 0 (실제 CLI 보고값) |
| RC-001 | INCOMPLETE. 제안·추적 단언 뒤 native 종료·사용량 누락 | unknown |

P2P-002의 인간 reason은 `scenario BLOCK`이며 테스트 경로가 BLOCKED를
검증했다. 원래 hook에는 별도 decision/work/Attention DB export가 없었다.
ABORTED는 인간의 authoritative BLOCK 결과이며 machine failure로 해석하지
않는다. P2P-004는 발주 결과가 있어도 최종 조정 실패이므로 PASS가 아니다.
QM의 0은 실제 CLI 보고값이며 unknown을 0으로 바꾼 값이 아니다. RC 원본
JSON의 PASSED는 보존하고 별도 INCOMPLETE 분류를 적용했다. 원래
시점에는 유효한 RC 재인수 증거가 없었다. 초기 모델 실패의 정확한 원인은 unknown이다.

원래 시도의 알려진 CLI 보고 비용 소계는 USD 12.661439다. 후속 최소
성공 진단 USD 0.001066을 더한 당시 알려진 소계는 USD 12.662505다.
RC와 Codex 등 미보고 항목이 있으므로 전체 시도 비용과 불완전한 사용량은 unknown으로 유지한다.
CODEX / `gpt-5.6-sol` (catalog default low)의 P2P-001은 첫 Run에서
MODEL_PROCESS_FAILED로 실패했다. proposal 이전이며 비용·tokens·resolvedModel은
unknown이고 executionReady=false, accountingStatus=PARTIAL이다.

현재 모델 없는 Codex account/rateLimits/read는 AUTHENTICATION을 보고한다.
이 현재 접근 gate를 과거 probe의 정확한 실패 원인으로 소급하지 않는다.
인간 login 갱신·metadata 재검사 절차를 준비했지만 실행하지 않았다.
이 원래 시점에는 5개 native 인수가 미완료였다. 현재 Claude five-case
checkpoint와 구분하며 Codex parity·실제 클라이언트 #35는 남아 있다.
운영 배포·OAuth·개인 IAM·규제 자동화의 증거로 확대하지 않는다.

### 과거 Recovery와 제한된 진단

새 Recovery 3건의 P2P-004·QM-001·RC-001은 모두 첫 Run에서 FAIL이다.
ERP 쓰기는 0건이며 Case OPEN, executionReady=false,
accountingStatus=PARTIAL, usageComplete=false다. CLI 보고 비용은 각 0이지만
전체 비용 unknown을 0으로 바꾸지 않는다. 원래 004·QM FAIL과 RC
INCOMPLETE, 당시 유효한 001·002 PASS를 별도 이력으로 보존한다.

normal RW mount의 최소 Claude 진단은 USD 0.001066으로 성공했다.
업무 인수 성공의 증거로 세지 않는다. 당시 production schema와 기본 boolean
schema의 no-tool 진단은 각 USD 0.045 budget에서 모두 FAIL이며 같은
error fingerprint를 남겼다. USD 0.045는 cap이며 실제 보고 비용이 아니다.
raw 오류 원문을 폐기한 상태이므로 정확한 원인은 unknown이다.

특정 production schema keyword나 인증 문제를 원인으로 확정하지 않는다.
tools-disabled와 structured-output/schema·budget 경로의 상호작용,
최소 성공 진단의 USD 0.05와 probe USD 0.045 조건 차이는 남은 한계다.
진단 수정의 runner 82건과 helper 7건만 검증했다. fe6a51f의 통합 551건·
SIT 20건 checkpoint는 유지하며 이 logging-only 변경 뒤 재실행한 결과로
바꾸지 않는다. 실제 클라이언트 #35, Codex 현재 AUTHENTICATION 접근 gate,
인간 login 갱신 미실행은 유지한다. 이후 최소 Claude/schema 진단은
성공했으나 업무 인수로 세지 않는다. 현재 업무 인수는 위 five-case 증거다.

## 규제와 권한의 한계

22개 allergen master를 19개 법정군으로 매핑한다. 필수 인증서 유형
전체 검사와 자동 30일 사전 알림은 이슈 등록을 기다리는 갭이다.
리콜 기록의 소프트웨어 2년 보관과 즉시 보고용 초안을 구현했지만 실제
MFDS 전송·법정 양식 검증을 수행하지 않았다. Goal 6 전체 완료나 법적
적합성 인증을 주장하지 않는다. As-Is/To-Be 표는 초기 설계 가정이다.

VERIFIED는 인간의 명시적 attestation이며 독립적인 외부 사실 검증이나
ERP 변경 권한을 제공하지 않는다. MONITOR는 승인 없이 조회하지만 채널
인증·Case scope를 적용한다. 공유 local role/key는 개인별 운영 IAM의
증거가 아니다. OAuth·전용 대시보드·운영 배포를 제외했다. 능동 알림과
실제 다중 채널 UX 인수는 별도 경계다.

## 재생성과 검증

Node 24와 `@oai/artifact-tool`이 필요하다. 설치된 Presentations skill 및
bundled Python 경로를 환경변수로 전달한다. source는 상대 경로를 사용한다.
기본 font는 macOS의 Apple SD Gothic Neo이며 다른 환경에서는 설치된
한국어 font를 `PORTFOLIO_FONT`으로 명시한다. 기본값을 바꿀 때 모든 페이지를
다시 렌더링해 줄바꿈·fallback을 확인한다.

```bash
export PRESENTATIONS_SKILL_DIR='<설치된 Presentations skill 디렉터리>'
export RUNTIME_PYTHON='<bundled python3>'
export RUNTIME_NODE_MODULES='<bundled node_modules>'
export PORTFOLIO_OUTPUT_DIR='<worktree 안의 새 output 디렉터리>'
# 실행 전 artifact operation marker는 skill 지침에 따라 한 번 기록한다.
node docs/portfolio/build.mjs
# bundled soffice와 기존 한국어 font로 PPTX에서 PDF를 만든다.
export SOFFICE='<bundled soffice>'
node docs/portfolio/export-pdf.mjs \
  "$PORTFOLIO_OUTPUT_DIR/mulino-coreano-portfolio-ko.pptx" \
  "$PORTFOLIO_OUTPUT_DIR/pdf"
```

finalizer는 package integrity, 14개 슬라이드, native table/chart, font
선언, geometry와 Artifact Tool 재import를 검사한다. chart workbook은
표시한 scripted SIT literal data의 snapshot이며 별도 계산식을 만들지
않았다. 상세 로그·render는 산출물과 별도로 보관한다. PDF는 PPTX를
변환한 배포본이며 편집 원본은 PPTX다. `export-pdf.mjs`는 private
Fontconfig로 기존 한국어 font 경로를 전달한다. 최초 headless export의
한국어 glyph 손실을 수정한 경로다. 다른 OS는 `PORTFOLIO_FONT_DIRS`에
설치된 font 디렉터리를 전달하고 PDF 페이지를 다시 확인한다.

최종 PPTX 14개 슬라이드와 PDF 14개 페이지를 각각 렌더링해 확인했다.
한국어 glyph·표·chart·업무 흐름도·footnote에 clipping이나 의도하지 않은
겹침을 발견하지 않았다. PDF 14개 페이지 모두 한국어 text를 추출할 수
있고 Apple SD Gothic Neo subset font를 포함한다. PPTX의 14개 노트,
5개 native table, 1개 chart와 workbook, editable diagram 객체를 확인했다.

PowerPoint 앱에서 직접 열어 편집하거나 저장한 검증은 수행하지 않았다.
최종 package와 렌더링 검사는 해당 앱의 실행 확인을 대신하지 않는다.
최종 source·native UAT·문서 통합과 combined checks의 인수는 coordinator가
담당한다. 이 독립 발표 작업의 종료만으로 전체 Step을 완료하지 않는다.
