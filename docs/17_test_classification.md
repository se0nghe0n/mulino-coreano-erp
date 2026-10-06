# #57 테스트 분류와 삭제 범위

문구와 빈 context 검증을 줄이되 승인·업무·보안의 기존 증거를
임의로 없애지 않는다. 2026-10-03 소유자가 승인한 삭제는 아래 3건이다.
그 밖의 기존 테스트 삭제는 이번 작업에서 승인되지 않았다.

| 기존 테스트 | 분류 | 적용 이유와 범위 |
|---|---|---|
| `BackendApplicationTests.contextLoads` | 삭제 | 빈 context 검증. 실제 업무 SIT가 Spring 구동과 업무 상태를 검증한다. |
| `GlobalExceptionHandlerTest.responseStatusExceptionKeepsItsHttpStatus` | 삭제 | 응답 문구·코드 중심 probe. 기존 실제 HTTP/API 통합 검증을 유지한다. |
| `GlobalExceptionHandlerTest.annotatedConflictExceptionsStayConflicts` | 삭제 | 합성 conflict probe. 실제 업무 충돌·idempotency 검증을 유지한다. |
| `GlobalExceptionHandlerTest.undeclaredFailuresStayInternal` | 유지 | 예상하지 않은 실패를 성공이나 다른 업무 오류로 바꾸지 않는 HTTP 경계를 검증한다. 소유자가 명시적으로 유지했다. |
| 나머지 기존 469건 | 유지 | 이번 삭제 승인 범위 밖이다. 기존 업무·권한·가격 정밀도·lease 검증을 그대로 유지했다. 전수 품질 재분류를 수행했다는 뜻은 아니다. |

초기 부모 `c5f1958`의 473건에서 승인한 3건을 삭제하고 scenario helper
12건을 더해 482건을 검증했다. 이때 실행 시간은 92초였다.

통합 검증에서 발견한 활성 창고 중복 접수 문제는 #65에서 수정했다.
`RuntimeIntakeIntegrationTest`의 순차 충돌·replay와 동시 접수 2건은
추가 유지한다. 수정 후 부모의 `test bootJar`는 475건, 79초로 통과했다.
2026-10-03 #65 통합 기준은 삭제 후 472건에 helper 12건을 더해
Unit 484건이었다.
Cucumber가 발견한 업무 시나리오 13건은 Unit에서 제외되어 skipped로
보고하므로 JUnit XML 합계는 497건이다. 최종 통합 `test bootJar`는
93초, 실패·오류 0건으로 통과했다. 오래된 stack의 546건이나 402건을
현재 branch의 전후 수치로 쓰지 않는다.

MRP→P2P 전체 흐름은 새 SIT로 검증한다. 부모에는 `DemoE2eTest`가
없으므로 이 파일을 이식하거나 삭제한 것으로 보고하지 않는다.
새 helper는 timeout·프로세스 종료·폐기용 DB guard·Run 사용량 합산·
증거 파일의 업무 결과 보존을 검증한다. source helper의 오류 문구
단언은 실패·시간 제한·실제 프로세스 종료 검증으로 고쳐 썼다.

## 2026-10-05 #34 추가 검증

CANCEL 권한·version/hash·replay·최종 상태·history 실패 rollback을
검증하고 기존 APPROVE/BLOCK 경합 테스트에 CANCEL을 추가했다. Goal 2와
4의 업무 상태를 확인하는 실행 사례가 7건 늘었다. 테스트를 삭제하지
않았다. 현재 검증은 Unit 491건 통과, scenario 14건 skipped다. MCP 15건,
SIT 업무 6개와 실제 재시작 2건도 통과했다. 상세 실행 기록은
[시나리오 문서](16_scenario_tests.md)의 2026-10-05 절에 있다.

## 2026-10-05 #26/#33 입고 품질 검증

기존 Unit을 삭제하지 않고 QC 업무 상태 검증을 15건 추가했다. Goal
1·2·4·5·6에 대해 안전 RELEASED와 생산 잔량, 역할·활성 DB 신원,
canonical 알레르겐과 명시적 분류, 인증 만료·30일 Attention, source
freshness·결정 race·rollback·불변 이력을 확인한다. 생산 감사 실패와
창고 충돌, 입고 전 생산 및 raw LOT 자체 만료도 입력·잔량·감사 미변경을
확인한다. 구현 세부 문구를 확인하는 테스트를 추가하지 않았다.

생산 감사와 창고 검증까지 전체 `test bootJar` 505건이 통과했다. 이후
raw LOT 자체 만료 guard의 마지막 변경은 QC 15건과 전체 SIT 14건으로
다시 검증했다. Cucumber는 Unit에서 15개 scenario를 skipped로 보고하며
SIT에서는 P2P 6개·QM 6개·실제 backend 재시작 2개가 통과한다. RC 3개는
다음 리콜 구현 단계의 pending이다. MCP 18건, runner 56건, Zig smoke
32건 및 Zig unit/build도 통과했다. 실제 모델 UAT는 실행하지 않았다.

### 리콜 전수 추적과 ADMIN 결정 (#27, #33)

LOT graph·원재료 잔량·출하 배분·고객 전수성, 승인 역할·source freshness,
경쟁 결정·replay·rollback, 생산·출고·계획 barrier, 불변 초안·기록의 최소
보관은 Goal 1·2·4·6의 Unit/integration 규칙으로 검증한다. RC-001~006은
실제 CLI/runner와 인간 stdio MCP를 통과하는 SIT다. OFFLINE/PENDING
보고 초안이 있으므로 실제 식약처 전송·법정 양식 검증은 완료로 분류하지
않는다. 고정 planningClock은 업무일이며 보관 최소 기한은 DB 생성 시각을
따른다. 실제 모델 UAT 증거는 scripted SIT와 별도로 수집한다.

## 2026-10-06 실제 모델 증거 경계 (#25/#26/#27)

공개 고정 scenario key는 실제 UAT의 인간·서비스 인증 근거가 될 수
없다. per-JVM 난수 key와 DynamicPropertySource로 바꾸고, 같은 JVM의
backend 재시작에는 동일 key를 유지한다. 기존 재시작 SIT에서 과거 공개
key와 반대 경계 key의 조회·claim 거절, private host client의 성공을
검증한다. runner는 CLAUDE·CODEX 모두 parent 인간·서비스 sentinel을
native argv·환경·context에 전달하지 않음을 확인한다.

QC·리콜의 PENDING 제안은 모델 종료 성공과 다르다. 실제 UAT의 마지막
hook은 Case의 모든 Run이 COMPLETED 또는 정상 반려 ABORTED이고
ABORTED는 해당 반려 시나리오의 최종 MANAGER 결정·승인 상태가
일치하는 원본 Orchestrator에만 허용한다. FAILED가 아니며 정상 native
종료와 runtime에서 실제 보고하는 model_finished 사용량을 확인한
뒤에만 업무 인수로 통과한다. Claude의 완전 accounting 기준은 유지한다.
Codex의 비용·cacheWrite·resolvedModel 미보고는 usageComplete=false와
accounting PARTIAL로 남기고, business execution readiness와 구분한다. 실패 때에도 원래
Run·partial 비용·unknown 지표를 JSON에 남긴 뒤 test를 실패시킨다.
`UatEvidenceTest`는 PENDING 뒤 usage 누락·RUNNING·FAILED·종료 유예
실패의 거절과 완전한 WAITING·정상 반려 ABORTED의 수용을 검증한다. 이는 Goal 4·5의 실제
하네스 인수 증거이며 모델 결과 문구나 호출 순서를 고정하지 않는다.


native 오류 회귀는 free-form 오류에 host sentinel이 있어도 category만
남기고 CLI가 보고한 partial 비용을 보존함을 증명한다. quota와 단순
429 rate limit, 인증과 권한/모델 접근, turn/budget/schema 제한을 서로
바꾸어 보고하지 않는다. 현재 unknown 실패 원인을 테스트 fixture의
분류 사례로 소급 확정하지 않는다.


Codex readiness 회귀는 terminal 업무와 exit 0·실제 보고 token이 있으면
business readiness를 수용하면서 비용·미보고 token·resolved model을
null로 유지함을 검증한다. model_finished 누락·native cancellation·
실패·nonzero exit는 수용하지 않는다. UatEvidence helper는 7건이다.


같은 Run의 이른 정상 model_finished 뒤에 실패·취소·미보고 종료가
있으면 business readiness를 수용하지 않는다. 이번 UAT는 실패 후
같은 Run의 의도적 재시도가 아니므로 보수적으로 모든 attempt의 정상
보고를 요구한다. 오류 이력과 partial/unknown accounting은 지우지
않는다. Codex native error frame→executor kill→runner failure 로그의
행동 회귀는 credential과 free-form 오류가 출력되지 않으면서 정해진
category/status가 유지되는지 확인한다. 현재 runner 76건이 통과했다.


### 3차 audit의 진단 증거 보강

whole-role 실패가 최소 no-tool 성공과 다른 조건에서 발생하므로 이를
인증 실패라고 재분류하지 않는다. parser는 schema/tool/filesystem/config/
output validation/context 문제를 정해진 category로 줄이고, 허용된 key
이름·primitive count·error fingerprint만 남긴다. fixture의 raw sentinel과
오류 문장이 출력되지 않는 회귀까지 runner 82건을 검증한다. business와
readiness 조건·native schema·반려·ERP approval guard는 바꾸지 않았다.

production/basic schema probe의 동일 fingerprint는 실제 오류 문장이나
특정 keyword 원인을 제공하지 않는다. raw 원문은 버린 상태다. 최소
진단을 실제 업무 PASS로 세지 않고 Recovery 3건의 zero-write 실패와
기존 유효 PASS·실패·INCOMPLETE 이력을 별도로 유지한다.
