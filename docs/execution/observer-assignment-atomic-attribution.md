# 인수 감사와 신규 assignment 생성 transaction의 관찰

이 변경은 T11 `acceptance-and-assignment-one-transaction`의 감사
transactionId와 새 TARGET/OPEN assignment.createdTransactionId를 실제
원행으로 연결한다. oracle의 ID/op/expected=true/selector는 바꾸지
않는다. Task/S5/S6는 OPEN이며 새 runtime scenario 인수는 NOT_RUN이다.

작업 baseline은 `1cf45afaee7efe6f00ed705b2790fc2279056fda`이고
branch는 `fix/t11-assignment-atomic-projection`이다. coordinator는
앞 변경의 동일 bytes를 root `140c9b5a`에 통합했다고 전달했다.
요청 당시 root는 `de794494`이며 full harness source freeze 중이다.
root 파일을 편집하거나 다른 tree의 code/compiled 산출물을 import하지
않았다. 자체 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-b370b5a3`다.
수정 runtime 지정은 `codex_2/gpt-6.1-sol/xhigh/priority`다.

## 실제 쓰기 계약과 관찰의 의미

자체 baseline의 ApplicationCommands.apply는 현재 쓰기 transaction의
`pg_current_xact_id()::xid::text`를 result.transactionId에 넣고 원 결과를
CommandRecords.resultJson에 저장한다. 중복 재시도는 원 결과를 돌려준다.
새 retry transaction ID를 원 결과에 넣지 않는다. CommandRepository는
같은 transaction에서 실제 CommandAudits 행을 저장한다. V8의 trigger는
terminal command와 audit를 불변으로 유지한다.

ResponsibilityCommands의 acceptHandover 결과는 accepted Handover다.
그 result의 assignmentId는 **원 assignment ID**다. 새 successor ID가
응답에 있다고 가정하지 않는다. ResponsibilityService.transfer는
원 assignment를 TRANSFERRED/valid=false로 바꾸고 predecessorId가
원 assignment인 새 TARGET assignment를 OPEN/valid=true/revision=0으로
만든다. 부분 이전은 root 아래 scope를 나누어 target과 원 Work의 잔여
assignment를 따로 만든다.

xmin은 현재 tuple을 마지막으로 쓴 transaction이다. 따라서 모든 행의
xmin을 createdTransactionId로 이름만 바꾸지 않는다. 아래의 정확한
명령 결과·typed predecessor 관계·최초 revision·물리적 tuple을 모두
검증한 새 target assignment에 한해서 생성 transaction으로 노출한다.

## 하나의 read-only REPEATABLE_READ 증명

ScenarioObservationProjection은 기존 observer connection에서 아래를
모두 읽고 SQL과 boundValues를 같은 sourceQuery에 남긴다.

1. 같은 조직의 실제 CommandRecord는 COMMITTED acceptHandover이고
   effectClass=RESPONSIBILITY여야 한다. 같은 조직/xmin의 CommandRecords는
   상태·시간·capability로 줄이기 전에 정확히 하나여야 한다.
2. canonicalIntentJson의 COMMAND/capability/definition/capability version,
   idempotency key와 정수 expectedRevision을 대조한다. 공개 hash 계약처럼
   비의미 envelope key를 빼고 재귀 정렬한 원문 SHA-256을 다시 계산한다.
   실제 canonicalHash, result.canonicalIntentHash와 일치해야 한다.
3. APPLIED result의 commandId는 실제 CommandRecord ID이고
   transactionId는 그 실제 xmin이어야 한다. canonical slots.handoverId로
   읽은 실제 Handover는 TRANSFER/ACCEPTED이며 expectedRevision+1이어야
   한다. result.effects.responsibility와 원행의 모든 저장 필드를
   비교한다. 시각과 decimal은 각각 instant와 정확한 decimal로 대조한다.
4. Handover.assignmentId의 실제 predecessor는 같은 조직에서
   TRANSFERRED/valid=false이고 source Work/owner가 맞아야 한다.
   그 predecessorId와 result의 targetWorkId를 가진 실제 successor가
   정확히 하나여야 한다. root/status/시간으로 후보를 줄여 유일성을
   만들지 않는다.
5. successor는 OPEN/valid=true/revision=0, createdAt=recordedAt이고
   원 root/kind, 실제 target Work/recipient/owner/supervisor,
   nextAction/nextCheck가 맞아야 한다. 실제 root와 이전/이후 Scopes를
   읽어 scopeJson, unit, 범위·수량, full/partial 이전을 검증한다.
   두 Work와 scope는 같은 조직의 실제 저장 행이어야 한다.
6. 원 APPLIED audit는 같은 command ID와 실제 xmin으로 정확히 하나다.
   actor/capability/canonicalHash, effectRefs.commandId,
   auditFacts.targetId/beforeRevision/scope와 CommandRecord의 실제
   authorizationScopeJson을 비교한다. scope에는 실제 target Work와 두
   Work의 ITEM이 포함되어야 한다. 감사 createdAt는 completedAt와 같아야
   한다.
7. CommandRecord, 원 audit, accepted Handover, 수정된 predecessor와
   새 successor는 같은 실제 xmin과 snapshot xmax를 가져야 한다.
   command/audit/domain의 관련 생성·기록·효력 시각은 knownAt/asOf를
   넘지 않아야 한다. 다음 확인일 같은 미래 약속을 기록 시각으로
   오해하지 않는다.

성립할 때 audit.transactionId와 successor.createdTransactionId는 이
실제 PostgreSQL xmin 문자열이다. atomicAttribution에 실제 command,
audit, Handover, predecessor/successor, root/Scopes, Work 원행과
물리적 tuple metadata를 남긴다. `historyComplete=false`다.

불명·불일치·중복은 명시적 gap과 필드 생략으로 남긴다. 기존 원행,
root/source 필드와 `current=valid`를 보존한다. RESOLVED/WAIVED의
current=true는 생성 증명이 부족하다는 이유로 false가 되지 않는다.
OWNER 인계, 다른 command나 다른 audit의 xmin은 승계 생성 증거가 아니다.

기존 allocation의 unwrappedTupleXid 계약을 그대로 사용한다.
`3 <= xmin < snapshot xmax < 1,000,000,000`만 지원한다. frozen/reserved,
wrapped/old cluster나 malformed XID의 epoch를 추정하지 않는다.
같은 top-level transaction 안의 서로 다른 subtransaction xmin도
임의로 합치지 않는다. 장기 사업 ID나 변경 이력으로 사용하지 않는다.
나중에 수정된 successor/command/audit/Handover의 새 xmin은 원 생성
ID로 노출하지 않는다. 과거 snapshot을 보간하지 않는다.

## 읽기 전용 독립 원자료의 custody

attempt5 index는 다음 경로에서 읽기만 했다.

`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-d049db44/.local/t11-native-r2-attempt5/http-pg/native-r2-cancel-after-shipment/transfer-artifact-index.json`

SHA-256은
`3a467dc5e1a551fd59d11b5e9724e7349cd81cb9797cbad2f9f6400c88c5224a`다.
당시 actual HTTP/after-db와 원 14 oracle 결과는 있지만 CommandRecord
raw/xmin은 미캡처이고 DB는 이미 정리됐다. 이 자료로 소급 증명하지
않는다. attempt6 캡처 진행 소식은 받았으나 이 변경의 근거로 사용한
새 SQL 자료는 다음 **attempt7**이다.

`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-d049db44/.local/t11-native-r2-attempt7/http-pg/native-r2-cancel-after-shipment/successor-transaction-sql.json`

SHA-256은
`109dbd3105e4cf4e90892e427232829fda048563ef3ae0cf6df036415c5c7958`다.
인계된 read-only REPEATABLE_READ SQL 증거의 snapshot은 `1022:1022:`다.
실제 acceptResponse.transactionId와 CommandRecords/CommandAudits 및
TARGET/OPEN successor의 xmin은 모두 `1020`이다. 각 SELECT의
row_to_json 원문과 조직/key/command/Work bindings가 들어 있다.
원 CommandRecord의 canonical 원문 hash와 저장된 result가 실제
acceptResponse와 같은지도 새 회귀에서 독립 대조한다.

이 artifact의 `1020`을 새 테스트 DB에 넣거나 관찰 필드의 답으로
캐시하지 않는다. 자체 PG fixture는 자신의 실제
pg_current_xact_id/xmin을 만들며 원자료와 별개다. attempt7의 기존 원
결과는 13 PASS/1 FAIL이고 atomic oracle는 missing transactionId로
실패한 상태다. 이 원 결과나 after-db를 덮어쓰지 않는다.

## 집중 PG 검증과 잔여 gate

새 AssignmentAtomicAttributionPostgresTest는 독립 PostgreSQL의
CSN 형태 schema에서 실제 transfer transaction과 동시 reader를
실행한다. 원 T11 sameAs assertion을 case.json에서 그대로 읽어 full과
partial 이전의 실제 JDBC 관찰에 적용한다. 실제 HTTP 제품 command나
Haiku scenario 전체 실행으로 세지 않는다.

다른 조직/command/root/Work/proposal/target, 잘못된 hash와 원문,
result/effect/audit scope 불일치, 미래 시각, 중복 command/audit/successor,
revision 변경 및 revision을 보존한 나중 no-op UPDATE, RESOLVED의
current=true, uncommitted/rollback, read-only 쓰기 거부, 기존 snapshot의
동시 commit 격리, XID wrap 경계를 검사한다. 실제 V8 terminal/audit
trigger도 자체 schema에 설치해 불변 원행을 검증한다.

OpenJDK `21.0.12.1`, Maven wrapper `3.9.16`, PostgreSQL `18.4`를
사용했다. 자체 backend build와 최종 harness JAR build는 각각 exit0이다.
전용 container는 `t11-atomic-b370-pg`이고 각 검사는 고유 obs schema를
사용했다. 다른 tree의 JAR/class/CSN을 복사하지 않았다.

결합 검사는 아래 command로 exit0, 150개 통과/실패0/skip0이다.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
OBSERVER_PG_TEST=true \
OBSERVER_PG_URL=jdbc:postgresql://127.0.0.1:32877/observer \
DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock \
T11_ATOMIC_SOURCE=/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-d049db44/.local/t11-native-r2-attempt7/http-pg/native-r2-cancel-after-shipment/successor-transaction-sql.json \
./mvnw -B -ntp -o -f verification/harness/pom.xml \
  -Dtest=AssignmentAtomicAttributionPostgresTest,AllocationAtomicAttributionPostgresTest,ObligationSourceOccurrencePostgresTest,FollowupParentProjectionPostgresTest,ScenarioObservationProjectionTest \
  test
```

정수 revision 범위 guard를 마지막으로 보강한 뒤 같은 환경에서
`-Dtest=AssignmentAtomicAttributionPostgresTest test`를 다시 실행해
최종 bytes의 64개 통과/실패0/skip0, exit0을 확인했다. 기존 allocation
21개, typed source 42개, followup 15개와 순수 projection 8개를 함께
보존했다. 최초 집중 RED의 4개 실패는 새 회귀 fixture의 기본
createdAt=before/recordedAt=now 때문이었다. 새 successor를 제품 writer
계약대로 createdAt=recordedAt=now로 만들도록 바로잡았다. case/fixture나
oracle를 수정한 것은 아니다. 모든 초기 log도 보존했다.

증거 위치는 자체 tree의 `.local/t11-assignment-atomic/`다.
`combined-pg.log`, `final-pg.log`, `reports/`의 원 Surefire XML,
`captures/`, 두 build log와 `focused-check-summary.json`을 보존한다.
summary는 실제 XML/hash/byte 대조를 수집한 한정 기록이며 coverage
receipt/runtime-manifest가 아니다.

수정한 projection/mapping/새 회귀 3개를 경로 순으로 정렬한
`{path: sha256}` compact JSON의 SHA-256은
`0b8893e70d338d85089bde1e95e8ffdffef7b6102c78c1bc61f08a66383e0c99`다.
개별 hash는 summary에 있다. 자체 backend JAR의 SHA-256은
`6895578d89c5743551bb4e3b82bc0fc8e285fbb268ae9344462d162cac93a456`,
최종 harness JAR는
`2bef3135c4b1d772a2245ae5dd734331d8110b863a531edc38d95086fb607d65`다.

T09/T11/T17의 추적 파일 145개가 baseline Git blob과 byte 동일하다.
원본 inventory의 SHA-256은
`c85441aecd9f2723f601b69520f99c38af429f905ca59f2f74db68f08a1991a8`다.
`git diff --check`는 exit0이다. 남은 자체 obs schema 0개를 확인한 뒤
자체 container/volume을 제거했다. root 파일, 다른 process/container,
읽기 전용 원자료는 수정하거나 정리하지 않았다.

원 T11 14개 전체의 통합 native Haiku 재실행은 coordinator의 잔여
gate다. attempt7의 13 PASS/atomic1 FAIL 원기록은 유지한다. 전체
scenario와 Task/S5/S6 종료는 이 worker의 완료 범위가 아니다.
