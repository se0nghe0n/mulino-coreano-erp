# T02 인수 계약

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

issuer/namespace가 다른 같은 SKU는 별도 품목이다. 제조자와 품목
문맥이 다른 LOT 표기도 별도 LOT다. 신규 생성 두 ID로 DB scope를
좁히므로 fixture의 기존 품목을 신규 품목으로 세지 않는다.
유효기간은 [from,until)로 고정한다. 경계의 비겹친 mapping을
허용하되 이전 mapping을 보존한다. 겹치면 CONFLICT와 인간 대조
owner·nextAction·nextCheck를 확인한다.
1 BUNDLE×12 EA/BUNDLE=12 EA는 내용량 환산이다. 실제1 BUNDLE
재고는 그대로이고 개봉·재포장·새 이행은0이다. 대체 근거 MISSING은
UNVERIFIED이며 허용으로 바꾸지 않는다. baseline/후속 단위를 모두 검사한다.

## Observation 연결

| Oracle / observation | 실제 assertion |
|---|---|
| T02.conversion-not-substitution / conversion-created-inventory | `conversion-no-substitution/unchanged-segments`, `conversion-no-substitution/unchanged-movements`, `conversion-no-substitution/held-bundle-delta` |
| T02.conversion-not-substitution / conversion-created-order-fulfilment | `conversion-no-substitution/unchanged-fulfilments` |
| T02.conversion-not-substitution / converted-content | `conversion-no-substitution/conversion-source-row`, `conversion-no-substitution/converted-content-12`, `conversion-no-substitution/reverse-original-unit` |
| T02.conversion-not-substitution / repackaging-effects | `conversion-no-substitution/unchanged-activities`, `conversion-no-substitution/unchanged-businessOutbox` |
| T02.conversion-not-substitution / substitution | `conversion-no-substitution/substitution-unverified`, `conversion-no-substitution/substitution-missing` |
| T02.identifier-period-conflict / code-conflict | `period-overlap/overlap-outcome`, `period-overlap/conflict-candidate-items` |
| T02.identifier-period-conflict / code-reconciliation | `period-overlap/current-duty-count`, `period-overlap/current-duty-owner`, `period-overlap/current-duty-action`, `period-overlap/current-duty-time`, `period-overlap/current-duty-state`, `period-overlap/duty-supervisor` |
| T02.identifier-period-conflict / silent-remap | `period-overlap/overlap-api-effects`, `period-overlap/unchanged-externalIdentifiers`, `period-overlap/unchanged-mergeRecords`, `period-overlap/unchanged-segments`, `period-overlap/unchanged-businessOutbox`, `period-boundary-nonoverlap/nonoverlap-applied`, `period-boundary-nonoverlap/historical-mappings-preserved`, `period-boundary-nonoverlap/boundary-current-item`, `period-boundary-nonoverlap/unchanged-mergeRecords`, `period-boundary-nonoverlap/unchanged-segments`, `period-boundary-nonoverlap/unchanged-businessOutbox` |
| T02.issuer-identity / distinct-item-ids | `issuer-and-lot-context/api-distinct-item-ids`, `issuer-and-lot-context/db-item-count`, `issuer-and-lot-context/db-item-set`, `issuer-and-lot-context/issuer-item-relations` |
| T02.issuer-identity / distinct-lot-ids | `issuer-and-lot-context/db-lot-count`, `issuer-and-lot-context/db-lot-set`, `issuer-and-lot-context/manufacturer-lot-relations` |
| T02.issuer-identity / implicit-merges | `issuer-and-lot-context/unchanged-mergeRecords` |

## 실제 검사 결과

B2와 공통 수정77c2df3을 기준으로 schema 검증 exit0, harness145개
검사 PASS·skip0, 본 영역 표본15개 PASS를 확인했다. 네 Gherkin
file selector는32개를 발견·시작했고 NOT_IMPLEMENTED assertion failure
32개·scenario skip0·exit1이다. 제품 scenarios32개는 NOT_RUN·exit2다.
`evidence/checks.json`은 case별 count와 원본 report·로그·hash를 연결한다.
선행 goalVersionId schema 오류는 공통 수정 후 해소됐으며 최종 RED가 아니다.
