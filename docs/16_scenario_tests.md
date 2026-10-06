# 업무 시나리오 테스트

모델 호출을 대신한 스크립트의 성공만으로 실제 하네스가 업무를
수행한다고 판단할 수 없다. 같은 한국어 업무 시나리오를 scripted
SIT와 실제 모델 UAT에서 실행하고 DB 업무 상태와 Run 증거를 남긴다.
설계의 원본 이력과 통합 방향은
[시나리오 설계](superpowers/specs/2026-09-25-scenario-tests-design.md)를
따른다. 이 문서는 #25/#26/#27 통합 source의 실행 계약을 설명한다.

## 실행 계층

| 명령 | 실행 범위 | 모델 호출 |
|---|---|---|
| `./gradlew test bootJar` | 기존 규칙·보안 테스트와 scenario helper | 없음 |
| `./gradlew sitTest` | P2P 6개·QM 6개·RC 6개 및 backend 재시작 2건 | 없음 |
| `./gradlew pendingScenarios` | 현재 pending 없음; 미구현 목록이 없으면 task 실패 | 없음 |
| `./gradlew uatTest` | P2P-001/002/004·QM-001·RC-001 5개 | 비용 발생 |

Cucumber 8.0.1을 쓴다. Unit task의 sentinel tag는 업무 시나리오를
실행하지 않으며 Cucumber가 보고하는 제외 시나리오는 skipped다.
SIT/UAT는 `@pending`을 제외한다. pending task는 미정의 단계만
허용하며 다른 discovery·설정 실패를 성공으로 처리하지 않는다.
`-PscenarioTags=@TC-P2P-001`로 범위를 줄여도 task 기본 tag와 교집합을
취하므로 pending 업무가 일반 SIT/UAT에 들어오지 않는다.

## 접속과 고정 시계

Java 21, PostgreSQL 18, Node 24.16.0, Zig 0.16.0이 필요하다.
먼저 `agents/cli`에서 `zig build`하고 `mcp-server` 의존성을 설치한다.
backend에서 각 계층에 다른 폐기용 DB를 지정한다.

```bash
DB_URL=jdbc:postgresql://127.0.0.1:55447/mulino_sit_scenario \
DB_USERNAME=postgres DB_PASSWORD=TEST_DB_PASSWORD ./gradlew sitTest
```

시나리오는 Flyway schema를 초기화하므로 loopback의 `*_scenario` DB만
허용한다. 검사기는 Spring 시작 전 DB URL을 확인한다.
SIT/UAT 서버는 local profile이며 planningClock을
`2026-09-05T00:00:00Z`, Asia/Seoul로 고정한다. 실제 벽시계에 따른
lease는 고정하지 않는다. fixture 적재는 테스트 코드에서만 수행한다.

인간 행동은 실제 역할별 stdio MCP로 전달한다. 보충 접수는
`create_case`의 `replenishment`와 `requestKey`를 사용한다.
승인 ID와 일반 질문은 `list_attention`에서 현재 Case의 기록을
찾고 `get_approval` 또는 `answer_attention`으로 처리한다.
일반 `get_case` 응답에 없는 승인 collection을 가정하지 않는다.

시나리오 JVM마다 별도의 예측 불가능한 인간·서비스 key를 생성한다.
DynamicPropertySource로 서버에 주입하고 같은 JVM의 backend 재시작에도
유지한다. 인간 key는 host stdio MCP에만, 서비스 key는 host runner에만
전달한다. 공개된 과거 scenario key와 경계를 바꾼 key는 조회·claim을
허용하지 않는다. native 환경·argv·context에는 두 key를 넣지 않는다.

host runner에는 테스트 서버와 같은 서비스 secret을 넣고 내부 요청은
`X-Mulino-Local-Service`를 쓴다. 컨테이너 API 주소는 실제 Spring
동적 port의 `host.docker.internal`이다. agent CLI의 capability Bearer와
호스트 서비스 secret은 서로 전달 경계를 넘지 않는다.

## 업무 확인

- TC-P2P-001: 승인 전 발주 0건, 구매안 16,500원, MANAGER 승인 후
  발주 1건·16,500원 및 입고 확인 WAITING.
- TC-P2P-002: 반려 후 발주·후속 업무 0건.
- TC-P2P-003: OPERATOR 승인 시도 후 구매안 PENDING·발주 0건.
- TC-P2P-004: 공급 단가 변경으로 옛 구매안 EXPIRED·발주 0건.
  명시적 인간 재계산 답변 이후 새 구매안·발주 17,000원.
- TC-P2P-005: 실행기를 실제로 종료하고 새 worker가 저장된 업무를
  이어서 처리한다. 계획은 1개이고 Supply Chain DONE도 1건이다.

추가 JUnit SIT는 승인 대기 중 Spring application context를 실제로
종료·재생성한 뒤 같은 계획을 승인해 발주가 1건만 생김을 확인한다.
구매 승인·적용 성공은 서버가 구매 Work Item을 즉시 DONE으로 만들고
후속 책임을 저장한다. Procurement 모델의 승인 후 확인 Run을
예약하거나 기다리지 않는다.

## UAT 증거와 제한

`MULINO_AGENT_RUNTIME`, `MULINO_AGENT_MODEL`, `MULINO_RUNTIME_IMAGE`,
`MULINO_AUTH_VOLUME`을 명시한다. image와 volume은 metadata만 검사하고
로그인 내용은 읽지 않는다. 설정·image·volume이 없으면 해당 UAT는
명시적으로 SKIPPED되며 누락 항목을 증거 파일에 기록한다.
존재하는 volume의 실제 로그인·모델 접근은 모델 실행 전까지 모른다.

증거는 `backend/build/uat/<날짜>/<TC-ID>.json`이다.
실행 runtime·모델, Claude가 보고한 resolvedModel, Run별 업무 결과·
실패 코드·비용·토큰과 최종 발주·Case 상태를 저장한다.
Claude inputTokens는 CLI의 native uncached input이며 cacheReadTokens와
cacheWriteTokens를 별도로 보존한다. 누락된 지표는 null이다.
사용량이 누락되면 usageComplete=false이고 전체 비용을 만들지 않으며
보고된 비용 합계만 partialCostUsd에 남긴다.
`beforeHumanDecision`에는 실제 승인 전 PENDING·발주 0건과 그 시점의
Run 사용량을 별도로 저장한다. 사용량 로그가 없는 Run은
`usageReported=false`로 구분한다. model_finished 이벤트만 있고
사용량이 없는 경우도 보고된 사용량으로 취급하지 않는다. 모델 비용은 가격표로 추정하지 않는다.

각 역할은 source Work Item/Run으로 전환한다. 이 증거를 native
subagent 실행 성공으로 해석하지 않는다. QC·리콜 pending script는
해당 기능 구현이나 한국 규제 전체의 인수 증거가 아니다.

이미 종료된 backend COMPLETED 결과를 heartbeat로 확인하면 runner는
revoked capability를 갱신하지 않고 최대 60초의 종료 유예만 둔다.
이 유예는 전체 실행 상한 안에서 final JSON과 CLI 사용량을 수집한다.
유예 만료·shutdown은 자식을 종료하고 확정된 backend 결과를 유지하며
사용량 실패를 기록한다. 실제 UAT는 이 실패를 성공 상태 뒤에 숨기지 않는다.
승인 후에는 원본 조정 업무가 실제 후속 책임을 확인해 DONE이 되고,
그 Run의 사용량까지 수집한 뒤 시나리오를 끝낸다.

## 2026-10-03 검증 기록

초기 `cb04403` 기준 `test bootJar`는 482건 통과, 시나리오 13건은
Unit에서 제외되어 skipped로 보고했고 92초가 걸렸다. SIT는 업무 5개와
실제 backend 재시작 검증 2건이 통과했다. pending 목록은 QM 5개·리콜
3개다. 아래 실제 모델 UAT 기록은 이 초기 기준선의 결과다.

후속 #65의 활성 창고 접수 충돌 수정은 409·atomic rollback·동시성·
성공한 원래 요청의 replay를 검증하는 회귀 테스트 2건을 추가했다.
수정 후 통합 `test bootJar`는 484건 통과, 13건 skipped, 93초였다.
MCP 14건, SIT 업무 5개와 실제 재시작 2건도 통과했다.
이 변경 뒤 실제 모델 UAT는 재실행하지 않았다. 중복 접수는 초기 Run
예약 전에 거부하고, 현재 모델 없는 SIT로 승인·반려·재승인을 확인했다.

실제 TC-P2P-001은 CLAUDE/claude-sonnet-5에서 통과했다. 승인 전
PENDING·발주 0건, 승인 후 발주 1건·16,500원, 원본 조정 업무 DONE과
Case WAITING을 확인했다. 5개 Run 모두 사용량이 완전하며 CLI 보고
비용 합계는 약 USD 3.0618774이다. native uncached input 120,
output 23,313, cache read 4,943,597, cache creation 459,947이다.
증거는 `backend/build/uat/2026-10-03/TC-P2P-001.json`이다.

첫 실패는 scoped proxy의 직접 port 필드 접근으로 API port 0을
전달한 문제다. CLI 보고 비용은 USD 0.43898200000000004다.
두 번째 시도는 PENDING에 도달했지만 terminal heartbeat가 모델을
즉시 취소해 Procurement 사용량을 잃었다. 알려진 비용만
USD 1.2156242이며 Procurement 비용은 unknown이다. 이 둘은
`backend/build/uat/2026-10-03/attempts/`에 별도 보존했다.
Procurement의 누락된 비용이 있어 전체 시도 비용은 알 수 없다.

실제 tagged UAT 3개를 모두 통과했다. TC-P2P-002는 반려 후 발주·후속
0건이며 5개 Run의 보고 비용은 약 USD 2.890354다. TC-P2P-004는
옛 제안 EXPIRED·발주 0건, 인간 재계산 지시 후 새 승인안 17,000원,
승인 후 발주 1건·Case WAITING과 조정 업무 DONE을 확인했다.
9개 Run의 보고 비용은 약 USD 6.216729다. 001은 351초, 002·004의
합동 task는 851초였다. 각 시나리오의 모든 Run은 사용량이 완전하고
resolvedModel은 claude-sonnet-5이며 machine failure는 없다.

성공한 세 시나리오의 보고 비용 합계는 약 USD 12.1689604다.
앞선 실패·불완전 시도의 알려진 비용까지 더하면 약
USD 13.8235666이지만 이전 Procurement 비용이 unknown이므로
전체 시도 비용은 확정하지 않는다. 전체 ledger와 Run 증거는
`backend/build/uat/2026-10-03/`에 보존했다. 실제 인수는 source
역할 Run의 검증이며 native subagent 수행 증거로 바꾸어 말하지 않는다.

## 2026-10-05 #34 CANCEL 검증 기록

PENDING 구매안의 MANAGER CANCEL을 위한 TC-P2P-006을 @sit로 추가했다.
취소 후 승인 시도가 기존 CANCELLED 상태를 유지하며 발주·후속 업무를
만들지 않는 업무 결과를 검증한다. @uat로 추가하거나 모델을 호출하지
않았다. 기존 5개 업무 시나리오와 QM 5개·리콜 3개의 pending 범위는 유지한다.

Java 21.0.12.1, PostgreSQL 18.6, Node 24.16.0에서 새 폐기용 DB로
`./gradlew clean test bootJar --no-daemon`이 1분 51초에 통과했다.
Unit 491건, scenario skipped 14건이며 XML 합계 505건의 실패·오류는 0이다.
`npm test`는 15건 통과했다. `./gradlew sitTest --no-daemon`은 1분 35초에
업무 6개·실제 backend 재시작 2건을 통과했다. pending 8건은 skipped이며
SIT XML 합계는 16건이다. 로그와 XML은
`/tmp/mulino-cancel-34-20261005/`에 보관했다.

새 DDL 00–20와 seeds, 새 Flyway V1–V28 DB의 columns·constraints·
indexes·enums·functions·triggers는 semantic parity를 통과했다. 물리적
column 순서는 비교에서 제외했다. 실제 JAR의 HTTP·stdio MCP 인수는
[새 5/5 기록](reviews/2026-10-05-purchase-acceptance.md)을 따른다.
기존 실제 모델 UAT 기록은 2026-10-03 source의 결과이며 이번 변경 뒤
재실행하지 않았다.

마지막 review에서 경합 HTTP 승자와 final decision·상태·application·
followup 수의 일치를 추가로 확인했다. history 쓰기 실패의 원래 구매 업무
WAITING·승인 대기 ACTIVE 보존도 보강했다. 영향을 받는 parameterized
4건을 새 DB에서 다시 실행해 실패 없이 통과했다 (`assertions.log`, 12초).
production source 변경 없이 assertion만 보강해 전체 검증을 반복하지 않았다.

## 입고 QM 구현 (#26/#33)

QM 6개는 pending에서 실제 SIT로 이동했다. 실제 QC runner와 CLI가
검사를 저장하고 인간 stdio MCP가 QC 승인·반려 및 MANAGER 권한 거절을
검증한다. 생산 투입 시도는 API를 통하며 SQL은 fixture와 상태 조회에만
쓴다. RC 3개는 다음 리콜 단계의 pending이다. 계약은
`docs/18_inbound_quality_api.md`에 기록한다.

최종 QC 검증은 15개 행동 Unit과 P2P 6개·QM 6개·backend 재시작
2개 SIT다. 생산 입력의 immutable audit·replay·rollback, 창고 충돌,
입고 전 생산, raw LOT 자체 만료를 추가로 검증했다. standalone DDL과
Flyway의 schema는 기존 events.external_ref 열 순서를 제외하고 같았다.
알레르겐 seed를 반복해도 추가 행이 생기지 않았으며 fresh DDL/Flyway
각각 22개 고유 코드와 19개 법정 군을 유지했다. 로컬 Jar의 인간 stdio
검사는 pending·QC 승인·MANAGER 거절·반려·취소·생산 잔량·불변 감사와
현재 만료 LOT 거절을 확인했다. receiving REST와 실제 모델 UAT를
검증한 것으로 보고하지 않는다.

### 배치 리콜 구현 증거 (#27)

RC-001~006은 실제 CLI/runner QC 조사·제안과 인간 ADMIN stdio MCP를
사용한다. 2단계 중간제품, 10개 영향 LOT, 원재료 3개, 고객 2명,
115개 출하 fixture로 전수 추적·승인 전 불변·ADMIN 승인·권한 거절·
반려·취소를 검증한다. 보고는 OFFLINE/PENDING 초안이며 제출 증거가 아니다.
SIT는 scripted agent 증거다. 실제 모델 UAT는 별도 실행·비용 게이트다.

## 2026-10-06 현재 source 실제 모델 시도 (#25/#26/#27)

baseline은 `0132e0a62ad9f086e5c70c5c857d4546f5921ef9`다. CLI·role·JAR
source hash와 새 runtime image ID는
`/tmp/mulino-project-completion-20261005/native/source-manifest.json`에
남겼다. private per-JVM key 변경과 종료 gate의 최종 harness hash는
같은 디렉터리의 `final-harness-hashes.json`이다. 실제 모델이 실행한
CLI·role·JAR는 그 사이 바꾸지 않았다.

Java 21.0.12.1, Node 24.16.0, PostgreSQL 18.6에서 첫 private-key SIT
20건이 3분 22초에 통과했다. 종료·권한 근거·snapshot·진단 보강 뒤 새
`native_diagnostics_sit_scenario`의 20건도 3분 19초에 통과했다. 실패·오류·
skipped는 없다. runner 76건, UatEvidence helper 7건, 모델 없는 image
smoke와 `bootJar`도 통과했다. test harness와 host runner의 진단 변경에 대해 전체 Unit 549건은
반복하지 않았다.

CLAUDE/claude-sonnet-5와 `completion-0132e0a`, 기존
`mulino-claude-auth`를 명시했다. probe는 새 `native_claude_probe_scenario`,
나머지 tag는 새 `native_claude_remaining_scenario`를 사용했다.

| Case | 현재 실제 결과 | CLI 보고 비용 USD |
|---|---|---:|
| TC-P2P-001 | PASS. 승인 후 발주 1·16,500원, 원본 조정 DONE·Case WAITING, 5 Run 사용량 완전 | 3.0874898 |
| TC-P2P-002 | PASS. MANAGER BLOCK 뒤 발주·후속 0, 조정 ABORTED·Case OPEN, 5 Run 사용량 완전 | 3.2866446 |
| TC-P2P-004 | FAIL. 새 발주 1·17,000원이나 마지막 조정 Run MODEL_PROCESS_FAILED | 6.2873046 |
| TC-QM-001 | FAIL. QC MODEL_PROCESS_FAILED, 검사 제안 없음, resolvedModel 없음 | 0 (CLI 보고값) |
| TC-RC-001 | INCOMPLETE. 제안 저장과 추적 단언은 통과했으나 native 종료·사용량 누락 | unknown |

002의 인간 요청 reason은 `scenario BLOCK`이고 테스트 경로가 BLOCKED를
검증했다. 별도 decision/work/Attention DB export는 당시 hook에 없었다.
이를 직접 export 증거로 바꾸어 쓰지 않는다. Case OPEN과 조정 Run
ABORTED는 JSON에 남았다. 이후 시도에는 allowlisted decision reason·role,
work·Attention·audit 및 QM/리콜 상태를 남긴다. 원본 002는 재실행하지
않았다.

004는 옛 제안 EXPIRED 뒤 명시적 인간 재계산·새 MANAGER APPROVE·발주
17,000원까지 도달했다. 그러나 마지막 원본 조정 Run의 모델 실패를
구매 성공으로 숨기지 않는다. 9 Run의 보고 지표와 알려진 비용은
보존했으나 usageComplete=false다. QM의 0은 CLI가 실제 보고한 값이며
unknown 값을 0으로 바꾼 것이 아니다. 실패 원인은 현재 실행기의 일반
MODEL_PROCESS_FAILED 코드만으로 인증·한도·모델 접근 중 하나라고
확정할 수 없다.

RC의 원래 JSON status=PASSED는 변경하지 않았다. 별도
`remaining-first-attempt/TC-RC-001-classification.json`에서 INCOMPLETE로
분류했다. 종료 hook을 수정했지만 이 시점의 유효한 RC 재인수 증거는
아직 없다. 알려진 보고 비용 합계는 USD 12.661439이고 RC 비용이
unknown이므로 전체 시도 비용은 확정하지 않는다. raw JSON·XML·실패
로그·business snapshot·ledger는 같은 `native/` 디렉터리에 보관한다.
현 단계에서 현재 실제 모델 5개 인수 통과나 Codex parity를 주장하지
않는다. 이전 2026-10-03 기록은 이전 source의 결과로 유지한다.

### 실패 진단과 runtime metadata 제한

host runner는 Claude 오류를 정해진 subtype·category·HTTP status·native
exit code로 줄여 기록한다. raw 오류·prompt·credential은 전달하지
않는다. turn/budget/schema 재시도, 인증, model 접근, 사용량·rate limit,
provider 오류와 unknown을 구분하고 알려진 partial 비용을 유지한다.
parser·실제 child 회귀를 포함한 runner 66건이 통과했다.

모델 없는 auth status는 Claude first-party Pro 로그인과 Codex ChatGPT
로그인 metadata를 보고했다. read-only volume의 Claude 최소 진단은
exit 1·UNKNOWN_NATIVE_FAILURE·보고 비용/토큰 0·resolvedModel 없음이다.
이 진단은 실제 runner의 writable named volume과 다르므로 외부 quota나
인증 gate의 증거로 쓰지 않는다. 004의 과거 오류 원인도 재분류하지
않았다. 새 전체 Case 재시도는 하지 않았다.

Codex의 read-only model-list 시도는 startup에 실패했다. 실제 실행기의
writable named volume·UID·tmpfs·workdir 조건으로만 metadata를 다시
확인하자 model/list가 성공했다. gpt-6-astra(default), gpt-5.6-sol,
gpt-5.6-terra, gpt-5.6-luna, gpt-5.5, gpt-5.2가 현재 CLI catalog에 있다.
service tier metadata는 null이며 rateLimits 조회는 -32603으로
실패했다. 이를 모델 호출 성공이나 잔여 quota 증거로 쓰지 않는다.
metadata 호출의 modelRequests는 0이다. 로그인 변경·credential 내용
조회·복사·export는 하지 않았다.

현재 Codex executor의 native usage는 input/output/cacheRead만 남는다.
비용·cacheWrite·resolvedModel을 추정하거나 0으로 채우지 않는다.
조정자가 runtime별 인수 증거 기준을 승인했다. Claude는 완전 보고
사용량을 요구한다. Codex는 정상 종료 nativeExitCode=0, model_finished,
input/output/cacheRead 보고값을 요구한다. 실패·signal·사용량 이벤트
누락은 어느 runtime에서도 수용하지 않는다. 모든 DB Run의 기대 업무
결과도 유지한다. strict usageComplete는 변경하지 않아 Codex의 미보고
비용·cacheWrite·resolvedModel이 있으면 false다. business PASSED와
accounting PARTIAL을 별도로 기록한다.

현재 실제 catalog의 gpt-5.6-sol default effort는 low다. 조정자는 이
기본값으로 단일 TC-P2P-001 parity probe를 승인했다. 별도 effort/tier
설정을 만들지 않았다. 이전 Claude 결과는 `attempts/claude-initial/`에
보존하고 새 probe는 별도 DB를 사용한다.


### 단일 Codex parity 시도와 현재 접근 gate

CODEX/gpt-5.6-sol(default low)의 TC-P2P-001을 새
`native_codex_probe_scenario`에서 한 번 실행했다. 첫 Orchestrator
RUN-2129e629c88a가 MODEL_PROCESS_FAILED로 실패했다. Case OPEN,
원본 work BLOCKED, OPEN JUDGMENT_REQUIRED Attention이며 발주·decision·
audit는 0이다. nativeSignal=SIGKILL은 error/turn.failed를 받은 실행기가
자식을 중단한 증거다. nativeExitCode와 비용·token·resolvedModel은
null이다. 비용을 0으로 추정하지 않았다. executionReady=false,
accountingStatus=PARTIAL, usageComplete=false이며 business pass가 아니다.
raw JSON·XML은 `attempts/codex-probe/`와 `native/codex-probe-attempt/`에
보존했다. 이전 Claude PASS를 덮어쓰지 않았다. Recovery 3개는 실행하지
않았다.

현재 모델 없는 account endpoint 검사는 AUTHENTICATION을 보고했다.
같은 Orchestrator role config와 요청 model을 넣은 strict app-server
metadata도 model/list는 성공하지만 account/rateLimits/read는 인증
오류다. native exec help는 실제 사용한 flag를 지원한다. 이 증거를 과거
probe 오류의 정확한 원인으로 소급 확정하지 않는다. human login 갱신
명령과 metadata 재검사를 준비했으나 로그인·재인수를 수행하지 않았다.

Codex error/turn.failed는 kill 전에 fixed frame kind·알려진 error code·
category·HTTP status를 보존한다. workspace routing과 일반 인증,
model 접근·quota·rate·provider·config·filesystem 문제를 구분하되
미확인 원인은 unknown이다. raw message·prompt·credential은 저장하거나
전달하지 않는다. 실제 child→runner의 오류 보존 회귀까지 76건이
통과했다. helper 7건과 마지막 전체 SIT 20건도 통과했다. 더 이른 성공
event 뒤의 실패·취소·사용량 누락을 같은 Run의 성공으로 숨기지 않는다.

현재 유효한 실제 업무 인수는 Claude 001·002의 두 PASS다. Claude 004와
QM-001은 FAIL, RC-001은 INCOMPLETE이고 Codex parity도 FAIL이다.
현재 account 접근 gate와 Claude unknown 상태가 해소되기 전에는 전체
5개 인수 또는 모델 간 parity 완료로 보고하지 않는다.


### 3차 audit 및 남은 Claude 3개 재시도

정상 writable auth mount의 no-tool 진단은 exit 0·OK이며
resolvedModel=claude-sonnet-5, input 513·output 4, CLI 보고 USD
0.001066으로 성공했다. 증거는 `native/third-audit-claude-diagnostic.json`이다.
이 결과는 이전 read-only 진단과 달라 최소 모델 호출의 현재 접근을
확인한다. 이전 실패 원인을 인증 문제로 소급 확정하지 않는다.

clean `2d2a900b59a8704e27a4462740196b2c4902829b`에서 기존 image/model/
auth volume의 정상 mount를 유지하고 새 `native_claude_recovery_third_scenario`
DB로 TC-P2P-004·TC-QM-001·TC-RC-001만 실행했다. 유효한 001·002는
재실행하지 않았다. 과거 default build 파일은 clean 뒤 없었으므로
보존한 raw 증거를 `attempts/pre-third-audit-recovery/`에 복구했다.

20초의 세 업무 task는 모두 첫 Run에서 MODEL_PROCESS_FAILED로 실패했다.
category는 UNKNOWN_NATIVE_FAILURE이며 resolvedModel은 null이다.
CLI가 비용·input/output/cache를 각각 0, turns를 1로 보고했다.
이는 관찰한 보고값이며 실제 사용량이 미보고된 다른 시도를 0으로 만든
것이 아니다. 모든 Case는 OPEN이고 실행한 work는 BLOCKED,
JUDGMENT_REQUIRED Attention은 OPEN이다. 발주·decision·audit·QC 검사·
리콜 제안은 0이다. sanitized work/Attention/audit snapshot을 fixture
초기화 전에 저장했다. executionReady=false, accountingStatus=PARTIAL,
usageComplete=false이며 세 건 모두 FAIL이다.

원본 JSON·XML·로그·source/image/CLI/JAR/host harness hash는
`native/recovery-third-audit/`와 `attempts/third-audit-recovery/`에 보존했다.
전체 CLI·role·JAR 및 production validation을 변경하지 않았다. 새로운
로그인·provider/model/tier 대체나 Codex 호출을 하지 않았다.

전체 role과 최소 호출의 차이를 조사한 뒤 조정자가 두 개의 개별 최소
진단을 승인했다. 둘 다 Sonnet 5·같은 writable mount·no ERP key·no role
append·tools disabled·USD 0.045 budget·35초 제한을 유지했다. 첫 진단은
정확한 production schema에서 draft URI만 제거했고, 두 번째는
`{ok: boolean}` 기본 schema다. 둘 다 exit 1·success subtype/error flag,
resolvedModel 없음·CLI 보고 비용/토큰 0·turns 1로 실패했다.
두 error fingerprint는 동일하다.

`8378c30e44e0ccecc1544d814a0fd4ad9f168c645c2716d0c0c49d52c7f111f0`

이는 두 probe 조건에서 특정 production keyword만을 원인으로 삼을
근거를 약화한다. 그러나 tools-disabled × structured-output 상호작용,
최소 성공 호출의 USD 0.05와 probe의 USD 0.045 차이, 일반 처리 경로는
분리하지 못했다. 전체 runtime의 structured-output 불가나 인증 실패를
증명한 것이 아니다. 도움말은 flag를 지원하고 schema는 유효한 JSON이다.
배포된 CLI binary에는 Ajv와 해당 keyword marker가 있으나 marker 존재는
deployed validator/API 지원의 증거가 아니다. 근거 있는 production 수정은
없어 validation을 약화하지 않았다.

free-form raw 오류는 즉시 버렸으므로 이후 의미 검토에 사용할 수 없다.
허용된 key 이름·primitive count·fingerprint와 schema/tool/filesystem/config/
output/context의 fixed category만 증거에 남긴다. 현재 두 probe의 category는
unknown으로 유지한다. SDK 오류 텍스트나 credential·prompt를 공개하지
않았으며 더 이상의 paid call을 하지 않았다.

이 logging-only 진단 보강의 runner 82건과 helper 7건을 검증했다.
기존 business/readiness gate의 전체 SIT 20건은 앞선 기록으로 유지하고
진단 metadata 변경 때문에 전체 Unit/SIT를 다시 실행하지 않았다.

### 2026-10-06 QC 단독 재개

새 `native_resumed_1006_qm_scenario` DB에서 기존 image·Sonnet 5·정상
writable auth mount로 TC-QM-001만 실행했다. 첫 launch는 host PATH에
Docker 경로가 없어 모델 호출 전에 SKIPPED됐다. 이 기록을 보존하고
`/usr/local/bin`을 추가해 같은 disposable fixture를 초기화했다.

실제 QC Run은 FAILED다. native 결과는 success·exit 0이며
resolvedModel은 `claude-sonnet-5`다. CLI 보고 비용은 USD 0.1097564,
uncached input 6·output 1,400·cache read 31,762·cache creation 22,348,
turns 6이다. usageComplete=true·accountingStatus=COMPLETE지만
executionReady=false다. native 오류 category·fingerprint는 없다.
이 결과를 이전 UNKNOWN_NATIVE_FAILURE의 원인 확정으로 해석하지 않는다.

검사·승인안·decision·audit은 0건이고 Case는 OPEN, QC work는 BLOCKED,
JUDGMENT_REQUIRED Attention은 OPEN이다. 입고는 HOLD이며 원재료 LOT
잔량은 2다. 모델이 끝나고 Run이 실패한 뒤에도 observer가 제안을
기다려 owned runner만 정상 종료했다. observer의 early-exit 실패와
이미 저장된 업무 실패를 구분한다. 증거는
`/tmp/mulino-project-completion-20261005/native/resumed-qm-1006/`이다.

저장된 모델 업무 summary는 Read로 role 문서를 읽었지만 dontAsk의
Bash 권한 거부로 mulino 명령을 실행하지 못했다고 보고한다. 배정된
qualityWork의 inboundId는 4이며 설치된 QC skill은 qc show·inspect를
안내한다. tool permission 설정·명령 matching을 다음 진단 대상으로
삼는다. 모델 보고만으로 정확한 권한 거부 원인을 확정하지 않는다.

QC·리콜 proposal wait에 기존 persisted terminal failure 판정을 연결했다.
성공 조건과 native 사용량 gate는 유지한다. 이 helper 수정 후 paid UAT는
재실행하지 않았다. 유효한 P2P-001·002와 이전 실패 기록도 유지한다.
AgentDriver·UatEvidence helper 13건과 QM-001·RC-001 scripted SIT,
해당 task에 포함되는 backend restart 2건을 검증했다.
유효한 실제 인수는 여전히 Claude 001·002의 두 PASS다. 원래 004/QM FAIL,
RC INCOMPLETE와 새 Recovery 3건 FAIL, Codex parity FAIL을 모두 보존한다.
Known 보고 비용은 기존 USD 12.661439에 최소 성공 진단 USD 0.001066을
더한 USD 12.662505다. 이전 RC·Codex 등 미보고 비용이 있으므로 전체
시도 비용은 unknown이다. 현재 전체 5개 실제 인수는 완료되지 않았다.
