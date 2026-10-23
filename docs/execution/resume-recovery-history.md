# 2026-10-23 복구 이력과 공급자 회신의 독립 관찰

nonempty recovery source의 과거 상태를 증명하지 못해 T09/T11/T12의
DB capture가 NOT_RUN이던 원인을 보완했다. 이를 빈 source로 숨기지
않고 실제 변경 이력을 같은 transaction에 보존한다. 전체 Task와
현재 Step은 OPEN이며 새 source의 실제 시나리오 인수는 아직 아니다.

## 제품 이력과 시점

V39의 `RecoveryObservationHistory`는 원본 source ID·revision,
ExecutionClock 기록 시각, transaction·sequence, 실제 item/Work/LOT
scope와 변경 snapshot을 저장한다. 원본 변경과 같은 거래에서 trigger가
기록하며 수정·삭제·truncate를 막는다. 삭제 원본은 tombstone으로 남긴다.
같은 시각의 변경도 sequence로 구별하고 rollback된 이력은 노출하지 않는다.
과거 원본에 이력을 소급 생성하지 않는다. 불완전한 legacy 상태는
계속 관찰 불가로 남는다.

모든 현재 recovery writer가 transaction 안에서 ExecutionClock을
명시한다. host clock으로 대체하지 않는다. 관찰기는 read-only
REPEATABLE READ에서 실제 원본과 최신 이력의 일치, 요청 knownAt 이전의
불변 scope와 상태를 확인한다. 현재 값을 과거 값으로 재사용하지 않는다.

쓰기 session이 Asia/Seoul이고 조회가 UTC 또는 America/New_York이면
PostgreSQL JSON timestamp 문자열이 달라 같은 원본도 거부하던 문제를
재현했다. 알려진 timestamptz 필드만 절대 시각으로 비교하고 나머지
JSON 값·key 집합은 그대로 검사한다. 누락 key, 문자열로 위장한 숫자,
실제 원본 변경을 탐지하는 반례도 통과했다.

V38 이후 새 승인 fixture에는 `SINGLE_ORDER_REVISION`을 명시했다.
과거 NULL과 미지원 소비 정책의 음성 검사는 유지한다. schema manifest는
기존 1939열을 보존하고 V39의 14열·PK와 실제 관찰 hash를 더했다.

## 공급자 회신 관찰

SupplierReplies의 `orderId`와 `proposedQuantity`를 각각 `purchaseId`,
`quantity`로도 제공한다. 원행 열과 ACCEPT/REJECT/CHANGE는 보존한다.
품목과 Work는 같은 조직의 불변 Orders와 정확한 proposal revision/hash로
연결한다. 최신 proposal이나 현재 Work의 다른 LOT를 대신 읽지 않는다.
LOT는 그 revision이 고정한 불변 GoalReferences에 명시된 경우만 제공한다.

링크의 누락·중복·hash 불일치·knownAt 이후 기록은 관찰 불가다.
요청 scope에서 결과가 비어도 깨진 조직 내 원행을 정상 0개로 숨기지
않는다. source query와 bound parameter를 같은 snapshot 증거에 남긴다.
전달·공급자 약속·물품 도착의 상태를 서로 유추하지 않는다.

## 통합과 실행 증거

worker `af7cd03c`, `8feb1d44`, `0fee1517`, `4f01e111`을 각각
`9b3342ef`, `6fb7fdbd`, `5cd13c68`, `415a7bb7`로 통합했다.
coordinator가 변경 파일, 정확한 14열 추가와 schema 관찰 hash,
초기 실패와 수정 후 로그를 검토했다.

첫 worker 전체 backend 결과는 646개 중 644 PASS·2 FAIL이다.
새로 방출하는 오류 코드 선언 누락과 V37→V38 검사에서 unbounded latest를
적용하던 문제를 보존·수정했다. closed vocabulary scanner를 유지하고
migration 검사는 target V38로 고정했다. 기대 개수를 2로 바꾸지 않았다.
수정 후 focused backend 8개와 observer 48개는 PASS다.

coordinator의 고정 source `415a7bb7` 전체 harness는 680개 중
658 PASS·22 SKIP·실패 0개다. 실제 PostgreSQL observer 42개를 포함한다.
22개는 별도 T17 HTTP/PG opt-in이며 제품 인수 PASS로 세지 않는다.
같은 source의 전체 backend 646개도 한 번의 실행에서 모두 PASS다.
실패·오류·SKIP은 0개다. JUnit 원본과 로그·제품 source hash를 보존한다.

증거는 `.local/resume-20261023/recovery-history-integration-proof.json`과
`.local/resume-20261023/recovery-supplier-integrated/proof.json`에 보존한다.
worker의 첫 실패와 시간대 재현은 `t3-fbbae58e` worktree의
`.local/recovery-followup/`, 공급자 alias RED와 수정 후 검사는
`.local/supplier-observer-followup/`에 있다. 생성 schema와 observer
컨테이너의 정리도 확인했다.

후속 T17은 원래 257개 assertion과 기존 ledger를 보존한 입력 수정이다.
초기 실제 HTTP/PG는 24 PASS·8 FAIL이며, 생성된 DRAFT goal의 필수 입력이
없어 활성화에 멈췄다. 같은 원래 Work를 공개 reviseGoal로 준비하는 새
합성 입력을 별도로 작성한다. 자동 활성화나 실제 운영 승인으로 바꾸지
않는다. T11의 물량 범위 입력과 구매 전송 인가도 병렬 보완 중이다.

구현 worker는 `codex/gpt-6.1-sol/high/ultrafast`를 유지한다.
coordinator live tier 제어 제한은 기존 기록과 같다. 실모델 client 호출,
실제 supplier 전송·규제·BTP·배포 인수는 완료하지 않았다.
