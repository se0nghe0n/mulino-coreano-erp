# T13 delivery 최종 권한 검사 뒤 lease 경계

Task/current Step은 OPEN이다. 이 후속 변경은 `84dd8025`의 delivery
경계만 고친다. 원래 135 assertion, 다른 14 subcase, source ledger,
fixture와 grant는 변경하지 않았다.

## 재현과 수정

`RuntimePostgresTest.leaseExpiredDuringFinalAuthorizationCannotSendExternalIO`
검사는 실제 PostgreSQL의 IN_FLIGHT lease를 T+5로 저장한다. 명시적
fixture authorization double의 세 번째 require 호출에서 시계를 T+6으로
이동한다. 세 호출은 claim, 첫 delivery 검사, 마지막 delivery 검사다.
이 검사는 lease/transaction과 제어된 시계를 검증하며 실제 권한 인수를
대신하지 않는다.

수정 전에는 마지막 권한 검사 뒤 adapter를 한 번 호출한 다음
`deliveryResult`에서 만료를 발견했다. transaction rollback은 이미 수행한
IO를 되돌리지 못했다. RED의 실제 실패는 `expected: <0> but was: <1>`이다.
수정 후에는 마지막 권한 검사 직후 lease를 확인한다. 권한 검사 횟수와
현재 authority fence, 승인 정책, 안정적 명령 identity는 바꾸지 않았다.

단독 GREEN은 adapter 호출 0, IN_FLIGHT 유지, 만료 lease 복구 후
UNKNOWN_EXTERNAL, 다시 claim 불가를 함께 확인한다. 코드 수정 commit은
`cb27f8cb`다. 새 제품 변경은 RuntimeService의 delivery 부분과 이 회귀
검사뿐이다.

## 승인된 소스 동기화

다음은 root가 이미 검토한 commit의 SYNC이며 새 deliverable이 아니다.
root에 다시 중복 적용하지 않는다. 다른 branch 전체는 merge하지 않았다.

| 검토 원본 | 이 tree의 cherry-pick |
| --- | --- |
| 9b3342ef | 00b28e7e |
| 6fb7fdbd | c3b1d38b |
| 5cd13c68 | 99619829 |
| 415a7bb7 | dc8da208 |

충돌 없이 적용됐다. RuntimeService의 recovery 부분은 검토된
9b3342ef 원문과 동일함을 별도로 확인했다. migration/schema/observer
변경은 위 승인된 동기화에만 포함되며 worker가 새로 수정하지 않았다.
검증 소스 HEAD는 `dc8da208191e42982f806443f29f85e27f295649`다.

## 실행 증거

`.local/t13-lease-boundary/lease-red.log`, `red-reports/`, `red-source.json`에
수정 전 실패와 정확한 소스 hash를 보존했다. `lease-green.log`는 같은
회귀 검사의 수정 후 단독 실행이다. 이전 19 PASS/1 FAIL 결과와 원문은
`.local/t13-delivery-authorization/` 및
`.local/t13-supplier-pg-773a8503-55d0-4790-b824-0d0192d25d7e/`에 그대로 있다.

후속 focused/HTTP 결과와 새 JAR hash는 아래에 기록한다.

## 최종 검증 결과

| 검사 | PASS | FAIL | NOT_RUN/skip | exit |
| --- | ---: | ---: | ---: | ---: |
| 수정 전 lease 회귀 | 0 | 1 | 0 | 1 |
| 수정 후 lease 회귀 단독 | 1 | 0 | 0 | 0 |
| 동기화 후 Runtime/Purchase focused | 48 | 0 | 0 | 0 |
| 새 JAR의 T13 HTTP/Spring/PG | 20 | 0 | 0 | 0 |

focused는 RuntimePostgresTest 7개와 PurchaseCommandPostgresTest 41개다.
새 lease 회귀는 focused 7개에도 포함되므로 합산하여 중복 계상하지 않는다.
HTTP는 supplier source 10개와 delivery 10개다. 원래 reply-distinct
assertion을 변경하지 않고 observer 실패가 해소됐음을 확인했다.
full backend/full harness는 이번 요청대로 반복하지 않아 NOT_RUN이다.
실제 모델/scenario 실행도 NOT_RUN이며 Task/current Step은 OPEN이다.

Java21과 이 tree의 기존 npm ci 설치를 사용해 자체 JAR를 새로 만들었다.
검증 소스는 위 dc8da208이며 새 JAR SHA-256은 다음과 같다.

`7ddf3fa63dba05557878a6b33fbf0d44496896bbc228ff20fda1f873ef6c045a`

HTTP 원문은
`.local/t13-supplier-pg-4e257748-a017-4d68-b677-26afe5ce5303/`의
`build-identity.json`, `commands.json`, `test.log`, HTTP/SQL 원행 및
surefire XML에 있다. JAR bytes가 이 hash와 일치함을 확인했다.
이 실행의 DB container 및 임시 private/public key는 정리하고 부재를
확인했다. 다른 tree의 target/generated/JAR는 복사하지 않았다.

명령·exit·현재 source hash·SYNC mapping·정리 결과는
`.local/t13-lease-boundary/summary.json`, `acceptance-commands.json`,
`focused.log`, `focused-reports/`, `http.log`, `sync.json`,
`preservation.json`에 남겼다. RED와 이전 19 PASS/1 FAIL을 삭제하거나
현재 PASS로 덮어쓰지 않았다. 원래 135 oracle/14개 다른 case/source
ledger/grant의 보존을 확인했다.

새 deliverable 파일은 다음 네 개다. 승인된 4개 SYNC의 변경 파일은
새 deliverable에 포함하지 않는다.

- `backend/src/main/java/com/mulino/application/runtime/RuntimeService.java`
- `backend/src/test/java/com/mulino/runtime/RuntimePostgresTest.java`
- `verification/cases/T13/LEASE-BOUNDARY.md`
- `verification/cases/T13/DELIVERY-AUTHORIZATION.md`
