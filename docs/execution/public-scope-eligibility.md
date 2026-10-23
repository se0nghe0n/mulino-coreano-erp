# 품목 범위 공개 적격성 조회

Task와 actual scenario failure repair Step은 OPEN이다. 이 변경은 D17의
`evaluateEligibility` ITEM 조회에만 해당한다. 원래 T17의 257개 oracle,
fixture, actor, grant, observer는 수정하지 않는다. 전체 T17 인수나
실모델·BTP 검증을 대신하지 않는다.

## 기준선과 소유 범위

- baseline: `e8c6649999a42219cbd07b4f4477e3d63b9b0360`이다.
- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-74b099a6`이다.
- branch: `fix/item-scope-eligibility`다.
- T3 thread configuration에서 provider `codex`, model `gpt-6.1-sol`,
  `reasoningEffort=high`, `serviceTier=ultrafast`를 확인했다.
- 제품 변경은 `QualityEligibility`, `InventoryReadCandidates`,
  `InventoryQueries`의 기존 후보 predicate 위임이다. 새 테스트와 이 문서만
  추가한다. private fulfillment/evidence, schema, 원래 case는 바꾸지 않는다.

## 추가 조회 계약

`id`가 있으면 기존 segment 조회 응답을 유지한다. `id`가 없을 때는
`scope.itemId` 또는 공개 decoder의 `itemId` filter를 요구한다.
`scope.placeId`, `scope.lotId`, `scope.workId`, customer와 action을 받는다.
customer의 scope/filter가 서로 다르면 `TYPE_INVALID`다. action은 기존
`SELL`, `DISPATCH`, `DISPOSE`, `INTERNAL_MOVE`만 지원한다.

ITEM 응답의 `data`는 `itemId`, `action`, `segmentIds`, `segments`다.
`segments`는 ID 순이며 각 행은 기존 segment 결과 필드와 `placeId`,
`lotId`, 선택 customer, 해당 행의 `unknowns`, `evidenceRefs`를 담는다.
`conditions`, `allowedRanges`, `eligibleQuantity`, `nextValidityBoundary`는
동일 actor/time/policy로 기존 evaluator를 실행한 값이다. 서로 다른
segment의 좌표·고객 조건을 합친 scalar를 만들지 않는다. 원천이 없는
condition ID, conditionResults alias, 포장 허용, evaluator version을
합성하지 않는다. 기존 evaluator가 반환하는 policy reference와 source
reference만 전달하며, 별도의 평가 version snapshot 계약은 미지원이다.

후보의 조직·knownAt은 InventoryRepository의 기존 CQN predicate를
따른다. active 판단과 명시한 place/LOT predicate는 inventory 조회와
공유한다. 장소를 지정하지 않으면 TRANSIT/customer/supplier도 후보이며,
SELL/DISPATCH는 기존 custody 판정으로 적격0이다. heldQuantity의 전역
의미는 바꾸지 않는다. Work는 공개 WorkReadRepository에서 보이는
동일 item의 Work이며, Work의 LOT 또는 SubjectLinks에 연결된 LOT로
한정한다. Work의 LOT이 없으면 item 범위다. 배분·고객별 연결을 추측하지
않으며, Work의 item/LOT 범위를 확장하지 않는다.

각 반환 후보는 현재 `evaluateEligibility` capability, membership,
assignment, grant, delegator의 TARGET/ITEM/PLACE 및 선택 WORK all-of
인가를 통과한다. 거부된 후보를 평가하거나 ID·근거를 반환하지 않는다.
후보가 없으면 item 자체와 선언한 place/Work 범위를 인가한다. 증거
검증은 기존 공개 `getEvidence` 권한 그대로다. private fulfillment
purpose로 전환하지 않는다. 기존 `assess`가 관련 없는 segment의 근거와
유효 경계를 수집하던 부분은 실제 물리 좌표 projection이 있을 때만
수집하도록 제한했다. restriction 경계도 물리적으로 관련된 segment에만
적용한다. 수량 산식과 내부 증거 검증은 유지한다.

## 시간과 남은 의미상 한계

조회는 ApplicationQueries의 기존 read-only REPEATABLE READ 안에서
실행한다. retirement 기록 시각이 없는 이미 retired인 row는 과거 active로
추측하지 않는다. 현재 mutable row의 수량·장소·policy를 근거 없이 과거
사실로 복원하지 않는다. 기존 evaluator가 current policy를 사용하는
계약도 바꾸지 않는다. 이 변경은 과거 모든 quality/regulatory/정책 이력의
완전한 bitemporal 복원을 주장하지 않는다.

기존 evaluator가 일부 상충/미확인 원천을 UNKNOWN으로 반환하는 표현을
그대로 유지한다. 이 변경은 새 CONFLICT 상태나 원래 T17의 condition
label을 만들지 않는다. 저장된 source가 없는 평가·회수 책임, 공개 인도
projection, observer alias와 요청 selector 문제는 root의 다른 소유 범위다.

## 검증

독립 disposable PostgreSQL에서 기존 실제 command/evidence fixture만
재사용했다.
다른 worktree의 target/JAR/DB는 사용하지 않는다. 실모델, 실제 scenario,
full backend/harness는 실행하지 않는다.

집중 검증 command는 다음과 같다. exit0이며 46개 모두 실패·오류·skip0이다.

```sh
./mvnw -f backend/pom.xml \
  '-Dtest=PublicScopeEligibilityPostgresTest,PublicScopeEligibilityTest,QualityRangesTest,InventoryPostgresTest,ReadContractsTest,QualityPostgresTest,FulfillmentPostgresTest#unrecognizedPlaceKindIsUnknownCustodyAndSegmentEligibilityAppliesCustody' \
  -Dlogging.level.com.sap.cds=ERROR test
```

- 새 PG 15개와 결정적 unit 2개를 실행했다. 서로 다른 customer/category,
  물리 좌표 `[0,40)`와 `[20,50)`, 장소·LOT·Work·미래 recordedAt 후보,
  모든 all-of 차원의 거부, membership/assignment/grant/delegator의 현재
  만료·철회, 공개 증거 미인가와 내부 reserve 성공을 대조했다.
- 저장된 증거 supersession, 충돌 inbox, 실제 blob 불가용, 조건 만료가
  적격 양수가 되지 않는지 같은 segment 조회와 대조했다. 이 경우 기존
  evaluator가 UNKNOWN으로 표현하면 그대로 유지한다.
- 관련 없는 segment의 basis/hold가 근거·nextValidityBoundary를 바꾸지
  않는지 확인했다. 조회 전후 전체 `mulino_*` 테이블 fingerprint와 같은
  조회의 snapshotRevision이 동일하다. 원천을 정정하는 쓰기는 없다.
- 기존 QualityPostgresTest 10개, InventoryPostgresTest 10개,
  ReadContractsTest 4개, QualityRangesTest 4개, 기존 segment custody 1개가
  함께 통과했다. 전체 backend/harness 또는 실제 scenario 실행은 아니다.
- `python3 -I verification/cases/check_request_contracts.py`는 exit0,
  41 cases / 374 fixtures / 신규 문제0이다. 기존 knownOpen 3715개는
  그대로 보고되며 해결됐다고 주장하지 않는다.
- 초기 fixture 준비 실패는 `.local/public-scope-eligibility/new-pg.log`와
  `focused.log`에 보존했다. 실제 불변 제약에 맞는 최초 입력과 단조
  supersession으로 고쳤으며, 제품 불변 제약을 우회하지 않았다.

실행 로그·JUnit 결과 복사본·JAR 및 코드 SHA-256은 이 worktree의
`.local/public-scope-eligibility/`에 보존한다. `SHA256SUMS`는 검증용
hash 목록이며 전체 온톨로지 인수 receipt가 아니다. 원본 case/contract/
schema는 baseline 대비 변경0이다. 이 commit을 root가 통합한 뒤 결합
검증과 실제 scenario를 실행해야 한다.

정리 증거를 남기는 test-only hook 추가 후 새 PG 15개를 다시 실행해
실패·오류·skip0을 확인했다. `cleanup-verification.log`와 `cleanup.txt`,
`cleanup-verified.txt`에 실행 결과, 생성한 blob 경로의 삭제, 각 실행에서
생성한 container ID의 부재를 남겼다. Testcontainers가 container를
정리하며 다른 실행의 container/디렉터리를 삭제하지 않는다.
`./mvnw -f backend/pom.xml -DskipTests package`도 exit0이다.
