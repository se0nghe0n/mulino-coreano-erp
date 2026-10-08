# S4c 판매 worker 실행 기록

- 사용자 Step3, 시스템 S4. baseline tag `step3-s4c-baseline`
  (`50646c99`), branch `step3/s4c-sales`, worktree
  `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-sales`.
- 소유: `application/trade/sales/**`, `domain/trade/sales/**`,
  `backend/db/sales*.cds`, SalesCommandPostgresTest,
  DeliveryCorrectionTest.

## 원인

| 증상(baseline) | 원인 | 구분 |
|---|---|---|
| SalesCommandPostgresTest 5 error, P0001 `Pinned Work identity cannot change` | salesSetup이 기존 Work의 `definitionVersionId`를 UPDATE. V9 guard와 계획 §274(기존 업무는 원 정의 유지)가 거부 | fixture |
| guard 제거 뒤 revision HELD 대신 APPLIED | 실행 효과를 knownAt=now+60 context로 기록해 recordedAt이 미래. 현재 시각 명령이 볼 수 없음 | fixture |
| 과거 asOf의 getSalesOrder가 `currentRevision=1`, `revision=2` | Orders root의 revision 열은 제자리 갱신되는데 투영이 덮어쓰지 않음 | product |

## 실행

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`
(Java 21, Node 24), PostgreSQL Testcontainers(OrbStack).

```
$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml \
  -Dmulino.evidence.blob-root=$(mktemp -d) \
  -Dtest=SalesCommandPostgresTest,DeliveryCorrectionTest test
```

| 실행 | 상태 | exit | 결과 |
|---|---|---|---|
| run1 | Work UPDATE 삭제만 | 1 | Sales 5 run, 1 fail(dispatch revision APPLIED); Delivery 2/2 |
| red | 투영 수정 stash, 새 test만 | 1 | 1 run, 1 fail(156행 revision) |
| run2 | 최종(7050f601 내용) | 0 | Sales 6 run 0 fail/error/skip; Delivery 2 run 0/0/0 |

## NOT_RUN

- 전체 backend suite(coordinator 담당).
- DISPATCHED 효과를 실제 배분·피킹·출고 명령으로 만드는 경로.
  이 test는 SalesExecutionPort를 직접 호출한다.
- getSalesOrders 목록과 Delivery 투영의 과거 시점 검증.
- AssessmentCorrectionImpact의 `Unavailable scope` 차단은 이번
  focused test에서 관찰되지 않았다.
