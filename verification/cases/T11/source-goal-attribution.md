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

실제 실행에서 source pin 후 child30/60은 SATISFIED가 된다. 그러나 원래 A의
closeWork 재판정은 ReceiptCanonicalFactAdmission의 `assessGoal` 권한 검사로
HELD가 된다. A는 원래 closeWork 권한을, 평가 전용 actor는 assessGoal 권한을
갖는다. 이 구현은 fixture grant를 추가하거나 현재 closure 검사를 우회하지
않는다. 해당 별도 권한 계약 수정은 coordinator의 소유권 결정이 필요하다.
원래 부족10 인계와 미배분90도 계속 별도 경계다.

명령·원실패·최종 검사 수·source/JAR hash·DB 정리 증거는
`.local/t11-source-attribution/`에 둔다. schema manifest는 coordinator가
V40 및 다른 변경과 통합한 뒤 동기화한다.
