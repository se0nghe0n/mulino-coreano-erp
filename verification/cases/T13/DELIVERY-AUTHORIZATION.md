# T13 purchase delivery의 현재 권한 재검증

Task와 current Step은 OPEN이다. 이 변경은 `fix/t13-supplier-replies`의
`64d74f9025ce301571bae8de1b6cffa89f6a9bb7` 이후 한정 구현이다.
root coordinator가 recovery 변경 통합과 supplier observer 수정,
결합 검증 및 최종 인수를 소유한다.

## 계약 정정과 구현

coordinator가 계획 §6 및 설계 철학에 따라 새 probe의 잘못된 기대를
정정했다. 검증된 공급자 회신은 전달 확인 없이도 독립 사실로 기록한다.
새 `verifiedReplyRemainsDistinctFromUnconfirmedTransmission` 검사는
outbox PENDING, supplier commitment 1, 도착/재고 0, 외부 대조 0을
함께 확인한다. 미검증 문서는 계속 EVIDENCE_UNVERIFIED다.
원래 135 assertion, 14개 비대상 subcase, source ledger 및 fixture는
이 후속 변경에서 수정하지 않았다.

`PurchaseDeliveryAuthorization`은 기존 COMMITTED dispatch의 원문,
canonical hash, 결과와 order/line/outbox payload identity를 대조한다.
원 command key를 fence하고 원래 handler의 ITEM/WORK/PLACE/SUPPLIER
scope, 현재 membership/grant/policy와 승인 최신성·hash·기한·승인자 및
조건을 재검사한다. 기존 `approveForDispatch`와 조건 평가기를 재사용한다.
승인을 다시 소비하거나 dispatch business effect를 다시 실행하지 않는다.

`RuntimeService`는 claim과 실제 IO 직전 이 port를 사용한다.
command/domain → policy/authority → outbox 순서로 잠그고, 잠근 뒤에도
identity 및 lease를 대조한다. verification barrier 뒤 시간 경계도 다시
검사한다. 원 actor와 stable request owner를 바꾸거나 WORK 권한을
추가하지 않는다. 지원 guard가 없거나 둘 이상이면 효과 없이 거부한다.
기존 fixture guard는 verification profile 및 fixtureExternalEffect에만
적용하며 purchase guard는 synthetic responder의 존재와 독립적이다.

IO는 명시적으로 설정한 loopback verification responder에만 수행했다.
이것은 운영 공급자 adapter나 실제 외부 공급자 전달의 증거가 아니다.
responder의 CONFIRMED_SUCCESS 응답만으로는 UNKNOWN_EXTERNAL이고,
원래 synthetic transmission 원문의 현재 typed canonical 검증을 거친
공개 recordExternalReconciliation만 SUCCEEDED로 대조한다.

## 검증 범위

새 HTTP/PG suite는 원 권한으로 실제 dispatch/claim/IO를 실행하고 다음을
검사한다. grant 철회·만료, membership/승인자 만료, 잘못된 ITEM/WORK/
TARGET scope, 상업 조건 7개 개정, 현재 policy 변경, command/actor/owner/
external ID/payload 불일치 및 다른 조직은 IO를 만들지 않는다.
승인 만료, lease 만료, 불변 command/approval DB 제약도 검사한다.
전달 전 철회는 IO 0이고, authority fence를 먼저 잡은 전달 뒤의 철회는
실제 DB lock에서 기다린다. 완료 뒤 재전송은 없고 UNKNOWN을 유지한다.

경합 검사의 철회는 테스트 전용 JDBC transaction이 제품과 같은
organization/authority fence를 잡고 실제 grant의 revokedAt/revision을
변경한다. 공개 revokeGrant API 인수라고 주장하지 않는다. 정책 변경과
손상 반례도 별도 disposable DB의 명시적 음성 입력이며 decision/verdict/
permission/fulfillment를 seed하여 성공을 만들지 않는다. 기존 사업 actor의
권한을 넓히지 않는다. 전역 scheduler가 이전 검사 fixture를 선택하지
않도록 새 fixture 설치 때 이전 조직의 PENDING 예약 시각만 2100년으로
미룬다. 상태/판정/수량은 바꾸지 않고 그 수를 raw 결과에 남긴다. 원본 command/approval 변조는 DB 불변 제약의 실제
거부를 확인하며 제약을 우회하지 않는다.

기존 purchase PG 검사에는 현재 조건이 충족된 전달과 claim 후 조건이
사라진 경우를 추가했다. 후자는 APPROVAL_CONDITION_UNVERIFIED로
adapter 호출 0, IN_FLIGHT 보존, 주문/outbox 추가 생성 0이다.
상업 조건 7개를 각각 바꾼 실제 service/PG 검사에서는 delivery claim의
APPROVAL_NOT_CURRENT와 attempts 0을 직접 확인한다. 개정된 품목의
optional subject 오류가 이 승인 원인을 덮지 않도록 순서를 유지한다.
RuntimePostgresTest의 두 lease 검사는 명시적 port double을 쓰는 저장
계층 검사이며 위 실제 HTTP/PG 인수와 구별한다.

## 보존과 인계

RuntimeService의 recordExternalReconciliation부터 delivery section 전까지
recovery 부분은 원본과 byte-identical이다. SHA-256은 다음과 같다.

`d76abe9b01604098f39471398f75d348bad508507d887f699e1f5cd75aac37c8`

RuntimeRepository, V39/schema, observer, T17, ScenarioFixtureInstaller는
수정하지 않았다. root가 recovery writer의 변경을 통합할 때 delivery
부분만 결합해야 한다. supplier observer의 `purchaseId` alias 실패는
수정하거나 기대를 낮추지 않았다. 전체 Task/Step 인수 PASS가 아니다.

실행 결과와 명령·exit·JAR identity는 아래 후속 실행 결과에 기록한다.

## 변경 파일

- `backend/src/main/java/com/mulino/application/trade/purchase/PurchaseDeliveryAuthorization.java`
- `verification/cases/T13/DELIVERY-AUTHORIZATION.md`
- `verification/harness/src/test/java/org/mulino/verification/actual/T13PurchaseDeliveryPostgresTest.java`
- `backend/src/main/java/com/mulino/application/runtime/ExternalDeliveryAuthorization.java`
- `backend/src/main/java/com/mulino/application/runtime/RuntimeFixtureDeliveryAuthorization.java`
- `backend/src/main/java/com/mulino/application/runtime/RuntimeService.java`
- `backend/src/main/java/com/mulino/application/trade/purchase/PurchaseCommands.java`
- `backend/src/test/java/com/mulino/core/PurchaseCommandPostgresTest.java`
- `backend/src/test/java/com/mulino/runtime/RuntimePostgresTest.java`
- `verification/cases/T13/SUPPLIER-SOURCE-RESULTS.md`
- `verification/cases/T13/SUPPLIER-SOURCES.md`
- `verification/cases/T13/run_supplier_pg_test.py`
- `verification/harness/src/main/java/org/mulino/verification/actual/T13SupplierSourceFacts.java`
- `verification/harness/src/test/java/org/mulino/verification/actual/T13SupplierSourceFactsPostgresTest.java`

## 최종 실행 결과

| 검사 | PASS | FAIL | NOT_RUN/skip | exit |
| --- | ---: | ---: | ---: | ---: |
| T13 Python 전체 | 20 | 0 | 0 | 0 |
| 기존 runtime/purchase/worker PG | 61 | 0 | 0 | 0 |
| 새 JAR의 실제 HTTP/Spring/PG | 19 | 1 | 0 | 1 |
| full harness | 616 | 0 | 55 | 0 |

`./verify prepare`는 exit0이다. 준비 검사의 성공은 실제 인수 성공이
아니며, full harness의 환경 조건부 skip 55개를 PASS로 세지 않았다.
실제 HTTP 20개 중 supplier source 9 PASS/1 FAIL, delivery 10 PASS다.
남은 실패는 원래 reply-distinct를 실행한 observer의
`projected field missing: purchaseId`이고 root 소유로 남겼다.
원래 135 assertion 전체의 실모델 인수와 다른 14 subcase 실제 실행은
이 후속 작업에서 NOT_RUN이다. Haiku Workflow/model/API 과금/운영
공급자 전송/push/merge/deploy/Grok/새 위임을 실행하지 않았다.

실행은 Java 21.0.12.1, Node 24.16.0, npm 11.13.0에서 수행했다.
`backend`의 npm ci 이후 이 tree에서 새 JAR를 만들었다. 다른 tree의
JAR/generated/target은 복사하지 않았다. 최종 JAR SHA-256은

`e392055bfc4269ab72019615eaf512e6d48c684a7805f836a47483431b9fd909`

이다. build identity는 baseline `64d74f90`와 미커밋 source diff hash 및
변경 Java/Python/JSON의 개별 hash를 포함한다. 최종 검사 종료 후에도
그 hash와 현재 source/JAR가 일치함을 확인했다.

원문 evidence 디렉터리는
`.local/t13-supplier-pg-773a8503-55d0-4790-b824-0d0192d25d7e/`다.
`build-identity.json`, `commands.json`, `test.log`, HTTP 요청·응답,
SQL 원행, Spring/PG 로그와 surefire XML을 보존했다. 임시 private/public
key와 이 실행이 생성한 DB container는 정리했으며 부재를 확인했다.

추가 로그는 `.local/t13-delivery-authorization/`의
`final-commands.json`, `final-summary.json`, `backend-tests.log`,
`python-tests.log`, `harness.log`, `prepare.log`, `preservation.json`과
각 surefire report다. 첫 compile/env/fixture-isolation 실패와 중간 HTTP
실패도 별도 로그 및 원문 run 디렉터리에 남겼다. 원래 bcac failure는
`.local/t13-supplier-work/raw-source-bcac-scenarios.json`에 보존한다.

원래 assertion·fixture·source ledger와 recovery section의 보존을
재확인했다. root가 supplier observer alias 및 recovery 변경을 통합하고
결합 인수를 수행해야 하므로 Task/current Step은 OPEN이다.
