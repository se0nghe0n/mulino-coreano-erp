# S4c 반품 worker 실행 기록

baseline은 `step3-s4c-baseline`(50646c99)이고 branch는
`step3/s4c-returns`다. 소유 범위는 반품 application/domain,
`ReturnStockPrimitives`, `ReturnGatewayPostgresTest`,
`ReturnRangeContractTest`다.

## 원인

baseline에서 `ReturnGatewayPostgresTest` 7건이 모두
`@BeforeEach returnFixture`에서 DB 제약에 걸려 제품 경로에 닿지
못했다. 차례로 드러난 원인은 아래와 같다.

| 순서 | 관찰 | 분류 | 조치 |
|---|---|---|---|
| 1 | CanonicalOccurrences.sourceProfileId NOT NULL 위반 | fixture | 선행 인도 event의 profile(SOURCE)을 넣는다 |
| 2 | CanonicalOccurrences revision CHECK(>=1) 위반 | fixture | revision 1을 명시한다 |
| 3 | `lineage timeline overlaps active parent and child` | fixture | retire된 transit 부모를 고객 재고 자식보다 먼저 insert한다 |
| 4 | `AssessmentCorrectionImpact.evidenceLinked` "Unavailable scope" | 공유 제품 결함 | scope worker 소유, 미수정 |
| 5 | Restrictions.decisionId NOT NULL 위반 | 반품 제품 결함 | 기본 QC 보류에 불변 quality Decision을 연결한다 |

4번은 `AssessmentRepository.rows`가 `recordedAt <= c.knownAt()`로
거르는데, `EvidenceReconciliation.link`가 같은 context로 방금 기록한
canonical을 다시 찾기 때문에 생긴다. 5번은 이를 임시로 우회해야
드러났다.

## 실행

모든 Maven 실행은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`
뒤 `$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml
-Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=... test`로 했다.

| 상태 | 대상 | exit | run/fail/error/skip |
|---|---|---|---|
| fixture 1·2·3 수정 뒤 | ReturnGatewayPostgresTest | 1 | 7/0/6/0 (6건 모두 Unavailable scope) |
| 임시 scope patch(commit 안 함) + 5번 수정 | ReturnGatewayPostgresTest | 0 | 7/0/0/0 |
| 최종 commit 상태(임시 patch 제거) | ReturnGatewayPostgresTest | 1 | 7/0/6/0 |
| 최종 commit 상태 | ReturnRangeContractTest | 0 | 3/0/0/0 |

임시 patch는 `evidenceLinked` 첫 줄에서 context의 knownAt을
`Instant.now()`로 바꾼 한 줄이며 commit하지 않았다. 그 상태에서
인도30 보존·반품10 별도·독립 의무·replay 단일 효과·권한 철회·
read-only grant 무효과·과거 명사 view 7개 oracle이 모두 통과했다.

## 미실행과 잔여

- 최종 commit 상태의 6 error는 blocked-by-scope다. scope worker의
  `evidenceLinked` 수정이 통합된 뒤 재실행해야 한다. NOT_RUN 아님,
  FAIL로 기록한다.
- 전체 backend suite는 coordinator 몫이므로 실행하지 않았다(NOT_RUN).
- `RecallStockPrimitives.investigate`도 Restrictions를 decisionId 없이
  기록한다. 회수 소유자의 확인이 필요하다.
