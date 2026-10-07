# V1 인수 계약

이 계약은 제품 구현에 앞서 기대 동작을 고정한다. API·독립 DB·blob·
control adapter는 아직 없으며 실제 제품 판정은 NOT_RUN이다. 표본
JUnit은 실제 AssertionEngine만 검사하고 제품 workflow를 만들지 않는다.

각 fixture의 시각은 2026-10-07T09:00:00Z이고 Asia/Seoul, SECOND
정밀도, inclusive deadline을 명시한다. 정책은 synthetic 값이며
실제 법규·승인·운영 자료의 인수가 아니다. actor의 역할과 grant는
명시한 조직·품목·장소·업무·case namespace로 제한한다.

installFixture 이후 서버 ID를 strict alias/result ref로 연결한다.
observe는 실제 API가 발급한 snapshotRevision, 완전한 scope와
원 행/source query artifact를 요구한다. effect0은 허용된 감사·대조
의무와 구분하고 금지된 원장·배분·이행·outbox의 전후 원 행을 비교한다.

## 독립 계산과 책임

v1 WAITING의 목표/evaluator는 ARRIVED/arrival-v1로 남고 신규
v2 업무는 SELL_ELIGIBLE이다. QC 제한은 현재 SELL 실행을 거부한다.
원40+수령20=60과 정정 후40+18=58의 과거/현재 판정을 확인한다.
v1 evaluator 미지원과 client의 skill hash 주장을 별도 실행한다.
최신 의미로 몰래 바꾸거나 skill hash로 runtime 지원을 대신하지 않는다.
새 VERSION_UNSUPPORTED 의무의 현재 assignment는 정확히1이고
인간 owner·supervisor·nextAction·nextCheck가 모두 남는다.

## Observation 연결

| Oracle / observation | 실제 assertion |
|---|---|
| V1.pinned-meaning-current-policy / current-sell-permission | `pinned-meaning-current-policy/sell-current-denied`, `pinned-meaning-current-policy/v1-reserve-forbidden`, `pinned-meaning-current-policy/db-current-qc-restriction` |
| V1.pinned-meaning-current-policy / new-v2-endpoint | `pinned-meaning-current-policy/new-endpoint`, `pinned-meaning-current-policy/db-new-goal-endpoint` |
| V1.pinned-meaning-current-policy / old-assessment-semantics | `pinned-meaning-current-policy/old-definition`, `pinned-meaning-current-policy/old-evaluator`, `pinned-meaning-current-policy/v1-reserve-no-effects`, `pinned-meaning-current-policy/unchanged-segments`, `pinned-meaning-current-policy/unchanged-movements`, `pinned-meaning-current-policy/unchanged-allocations`, `pinned-meaning-current-policy/unchanged-businessOutbox`, `pinned-meaning-current-policy/db-old-work-pinned`, `pinned-meaning-current-policy/arrival60-result`, `pinned-meaning-current-policy/corrected58-result`, `pinned-meaning-current-policy/correction-definition-v1`, `pinned-meaning-current-policy/correction-evaluator-v1`, `pinned-meaning-current-policy/correction-still-arrived`, `pinned-meaning-current-policy/current-qc-still-denied`, `pinned-meaning-current-policy/corrected-receipt-58`, `pinned-meaning-current-policy/immutable-assessment-history`, `pinned-meaning-current-policy/db-current-arrival58`, `pinned-meaning-current-policy/previous-assessment-linked` |
| V1.pinned-meaning-current-policy / skill-hash-substitutes-runtime-proof | `skill-hash-no-runtime-proof/hash-cannot-prove-support`, `skill-hash-no-runtime-proof/hash-effects0`, `skill-hash-no-runtime-proof/unchanged-segments`, `skill-hash-no-runtime-proof/unchanged-movements`, `skill-hash-no-runtime-proof/unchanged-allocations`, `skill-hash-no-runtime-proof/unchanged-businessOutbox`, `skill-hash-no-runtime-proof/unchanged-goalVersions`, `skill-hash-no-runtime-proof/meaning-stays-v1` |
| V1.pinned-meaning-current-policy / v1-endpoint | `pinned-meaning-current-policy/old-endpoint`, `pinned-meaning-current-policy/db-old-goal-endpoint` |
| V1.unsupported-v1-held / no-silent-fallback | `unsupported-v1-held/unsupported-assessment-no-fallback`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |
| V1.unsupported-v1-held / unsupported | `unsupported-v1-held/unsupported-error`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |
| V1.unsupported-v1-held / unsupported-command-effects | `unsupported-v1-held/unsupported-effects0`, `unsupported-v1-held/unchanged-segments`, `unsupported-v1-held/unchanged-movements`, `unsupported-v1-held/unchanged-allocations`, `unsupported-v1-held/unchanged-goalVersions`, `unsupported-v1-held/unchanged-businessOutbox`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |
| V1.unsupported-v1-held / unsupported-duty | `unsupported-v1-held/work-held`, `unsupported-v1-held/current-duty-count`, `unsupported-v1-held/current-duty-owner`, `unsupported-v1-held/current-duty-action`, `unsupported-v1-held/current-duty-time`, `unsupported-v1-held/current-duty-state`, `unsupported-v1-held/duty-supervisor`, `unsupported-v1-held/api-duty-count`, `unsupported-v1-held/api-duty-details`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |

## 실제 검사 결과

B2와 공통 수정77c2df3을 기준으로 schema 검증 exit0, harness145개
검사 PASS·skip0, 본 영역 표본15개 PASS를 확인했다. 네 Gherkin
file selector는32개를 발견·시작했고 NOT_IMPLEMENTED assertion failure
32개·scenario skip0·exit1이다. 제품 scenarios32개는 NOT_RUN·exit2다.
`evidence/checks.json`은 case별 count와 원본 report·로그·hash를 연결한다.
선행 goalVersionId schema 오류는 공통 수정 후 해소됐으며 최종 RED가 아니다.
