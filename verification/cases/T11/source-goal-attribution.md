# T11 고정 source Goal 구간 기여

`e61e952b` 뒤의 한정 구현이다. Task/Step은 OPEN이며 통합·최종 검증은
coordinator가 소유한다. 부족10·미배분90·인계 의미는 바꾸지 않는다.

## 저장과 판정

V41은 WorkContributions에 아래 세 nullable 열만 추가한다. 기존 행의
source pin은 NULL로 남기며 현재 Work로 backfill하지 않는다.

| 열 | PostgreSQL | CDS | nullable | default |
|---|---|---|---|---|
| sourceWorkId | varchar(36) | UUID | YES | 없음 |
| sourceGoalVersionId | varchar(36) | UUID | YES | 없음 |
| sourceConditionId | varchar(100) | String(100) | YES | 없음 |

기존 PK `(organizationId, ID)`와 11열은 유지한다. 새 composite FK는
`(organizationId, sourceGoalVersionId, sourceWorkId)`에서
`GoalReferences(organizationId, ID, workId)`를 참조한다. 세 값은 전부
NULL이거나 전부 있어야 한다. 새 insert는 source pin을 요구하고 실제
link의 양끝과 양쪽 Work의 현재 GoalVersion을 대조한다. 기존 allocation의
UPDATE/DELETE 금지는 유지하고, 참조한 link의 UPDATE/DELETE도 막는다.
V40은 이 tree에 없으므로 upgrade 증거는 **V39→V41**이다.

application은 기존 source/target/canonical fence와 transaction 안에서
각 Goal의 발행 정의·evaluator·유일한 quantitySum property·단위·품목·
장소·LOT·기간·knownAt·현재 evidence policy를 확인한다. 저장한
`conditionId`와 `sourceConditionId`는 실제 property `Receipt.quantity`다.
caller의 과거 `quantity` 표기는 입력 호환용이며 저장 조건을 결정하지 않는다.

한 allocation ID를 SOURCE/TARGET 역할로 읽는다. raw `goalId`와
`targetWorkId`는 원래 target 의미를 유지하고 `boundGoalId`가 읽는 쪽의
고정 Goal을 밝힌다. source 새 Goal에는 과거 allocation을 적용하지 않는다.
legacy source는 UNKNOWN이다. 직접 canonical 수량은 그 사건의 allocation을
포함하므로 직접90과 부분30을 동시에 더하거나 직접90을30으로 줄이지 않는다.
각 Goal의 실제 evidence/policy/period 판정은 기존 evaluator가 수행한다.

정정 영향은 저장된 source와 target을 모두 찾는다. 폐쇄 Work를 다시 열지
않고 기존 C5 followup을 남긴다. 현재 getWork 권한이 없는 다른 endpoint의
Goal ID가 contribution 또는 assessment snapshot에서 노출되지 않도록 한다.

## 검증과 남은 경계

`verification/actual/t11/run.py`는 own npm ci/build, disposable PostgreSQL,
실제 HTTP와 독립 SQL만 사용한다. model/scenario runner는 호출하지 않는다.
원래 T11 입력·110 oracle·다른 9 subcase와 T09/T12는 수정하지 않는다.

`863c61ec` 후속 변경은 서버 내부 `CanonicalFactAdmission.Purpose`를
도입한다. 공개 assessGoal과 기존 호출은 ASSESS_GOAL을 사용하고,
AssessmentService.requireFulfilled만 CLOSE_WORK를 선택한다. 요청 slot으로
purpose를 받지 않는다. receipt admission은 같은 TARGET/ITEM/PLACE 조건을
유지하며 요청 Work와 실제 원천 Work 각각에 해당 operation 권한을 요구한다.
DeliveryAdmission도 같은 TARGET/ITEM/PLACE와 현재 purpose를 유지하며
요청 Work와 원천 Work를 각각 검사한다. WORK 배열 한 번의 any-of 판정으로
한쪽 권한만 확인하는 경로를 제거한다.

목적은 snapshot에 저장하지 않는다. 성공한 두 operation은 같은 사실·증거를
반환하므로 purpose만 바뀌어 input hash가 달라지지 않는다. 폐쇄는 여전히
현재 원천·고정 Goal·최신 assessment·입력 hash·현재 정책을 다시 검증한다.
actor를 바꾸거나 과거 snapshot을 현재 사실로 재사용하지 않는다.

원래 A의 child30/60 폐쇄는 APPLIED로 진행한다. 원래 세 전체 흐름은 이후
부족10의 obligationId를 얻지 못하며 alternative-close의 UNRESOLVED_DUTY가
남는다. 원래 부족10 인계와 미배분90은 이 후속 수정의 범위 밖이다.

명령·원실패·최종 검사 수·source/JAR hash·DB 정리 증거는
`.local/t11-source-attribution/` 및 후속 `.local/t11-closure-purpose/`에 둔다. schema manifest는 coordinator가
V40 및 다른 변경과 통합한 뒤 동기화한다.

후속 검증에서 backend 집중 89개와 operation authority 재검사 6개가
통과했다. 전체 T11 HTTP suite는 19 PASS / 3 FAIL이며 세 실패는 원래
전체 흐름의 alternative-close에 남은 UNRESOLVED_DUTY다. 자식30/60의
폐쇄, 부모90, 원래 단일 canonical/receipt/movement와 두 allocation은
통과했다. 추가 정정 음성 검사는 기존 supersession 규칙에 맞게 검증
시계만 1μs 전진시키며 원래 시나리오 시각은 바꾸지 않는다.


Delivery 후속 권한 반례는 any-of WORK 의미를 재현하는 독립 unit으로
검사한다. 요청 Work만 또는 원천 Work만 허용한 경우 두 purpose에서 모두
거부하고, 양쪽과 같은 TARGET/ITEM/PLACE가 허용될 때만 실제 인도 수량을
반환한다. 목적만 다른 성공 결과는 동일한 사실/hash를 유지하고 기본 호출은
assessGoal 전용이다. RED 17개 중 4개 실패를 보존한 뒤 같은 검사로 GREEN을
확인한다. 증거는 `.local/delivery-admission-authority/`에 둔다.
GREEN은 새 권한 unit 17개, 기존 DeliveryCorrection 6개, 실제 PG
Fulfillment 2개로 총 25 PASS다. Fulfillment test 파일은 수정하지 않았으며
이 작은 수정 때문에 전체 backend/harness/HTTP 검사를 반복하지 않는다.
