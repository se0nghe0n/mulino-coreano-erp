# T09·T11·T12 원천 입력과 실제 참조 graph

최초 입력 목록은 `66e4d068017753147017f5d08062ba460e4e385c` 기준이다.
실제 참조 graph 수정 baseline은 `ad80e1bc`이며 격리 작업 경로는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-f152b63f`다.
전체 인수는 미완료이며 이 문서는 실행 receipt가 아니다.

## 반영한 입력과 보존한 oracle

- `recordActivity` 9개를 공개 EVENT 형식으로 옮긴다. 원문의 수량,
  시각, 장소, LOT, coverage 구간과 상충 후보 관계를 JSON 원문에
  보존한다. 원문이 있는 8개는 `attachEvidence`로 저장한 뒤 그 반환
  document ID로 claim을 만든다. 문서 없는 T12 반례는 계속 문서가 없다.
- 누적/상태 목표의 기존 period에 `startInclusive=true`를 명시한다.
  T09의 낡은 `interval`은 같은 값의 공개 `period`로 옮긴다.
- 판매 목적지는 Customer ID 대신 별도 `Place(kind=CUSTOMER)`로
  지정하고 필수 `packageTerms=STANDARD`를 보완한다.
- 26개 subcase와 269개 assertion(T09 94, T11 110, T12 65)을
  byte 단위로 보존한다. 기존 action도 삭제하지 않는다. 미지원 evaluator의
  HELD 대 REJECTED 기대는 그대로 둔다. grant의 all-of 규칙도 유지한다.
- 기존 period helper는 인자 없이 T09/T11/T12만 쓴다. T13은 명시적
  `--cases T13` 선택이 있을 때만 쓴다. 이전 T13 산출물은 바꾸지 않는다.

## 현재 case에 남긴 수령 입력 10개

`source-contract-frontier.json`은 각 case의 미해결 action과 원래 물리
입력을 그대로 담는다. `confirmReceipt`의 수량·장소·발생시각 등은
공개 slot이 아니므로 삭제만 해서 유효한 수령으로 만들 수 없다.

제품의 실제 계약은 아래 순서다.

1. 실제 ID가 담긴 원문을 `attachEvidence`로 저장한다.
2. 같은 원문과 주체로 `recordActivity(kind=PHYSICAL_RECEIPT)`를
   보내 event와 claim을 만든다.
3. 그 event로 `receiveProvisional`을 보내 수량·LOT·장소·Work·책임자와
   실제 범위를 등록한다. 운송 leaf가 있다면 그 leaf를 참조한다.
4. `matchSourceIdentity`가 claim·원문·provisional 범위를 대조한다.
5. MATCHED 결과를 `linkCanonicalOccurrence`로 연결한다.
6. 반환 canonical ID, observation ID, LOT로 `confirmReceipt`를 보낸다.

`source_contracts.receipt_chain`을 10개 원래 수령 action에 연결했다.
마지막 `confirmReceipt`의 action ID는 그대로다. 보조 action ID는
`-event-document`, `-event`, `-provisional`, `-match`, `-canonical`이다.
LOT 없는 반례는 문서·event·provisional 뒤 원래 confirm probe만 보낸다.
LOT나 canonical을 만들어 넣지 않는다.

`$encode`는 INPUT 전용이다. 실제 alias/result identity를 먼저 해석하고
object key를 정렬한 compact JSON의 UTF-8 bytes에서 JSON 문자열,
base64 또는 SHA-256을 만든다. payload와 첨부 원문은 같은 source object를
쓴다. 65,536 bytes·32 depth·4,096 nodes 상한이 있으며 임의 실행·산술·
중첩 encoder/transform·관측 business 값 복사는 허용하지 않는다.
assertion expected/identity에는 encoder를 쓸 수 없다.

기존 관측의 `fixtureAlias`·`priorSourceAction` 표기는 실제 `$alias`·
`$result` binding으로 바꿨다. 문서 부재, PORT로 제한된 주장 범위,
100/98 상충 주장, 관측 공백과 발생시각은 유지한다.

수령 원천에는 별도 synthetic `receiptSource` 인간과
`synthetic-receipt` profile을 둔다. 원래 business actor/grant는 불변이다.
새 actor의 ITEM=P all-of grant는 attach/event/provisional/match/link/read
6개 capability만 가지며 수령 확정 권한은 원래 actor에게 남는다.
profile과 관련 GoalVersion의 policy는 `SYNTHETIC-policy-v1`로 같다.
이는 가상 테스트 설정이며 규제 승인이나 VERIFIED fixture가 아니다.

`receiveProvisional`은 응답 revision=0이지만 저장 observation revision은
1이다. confirm은 그 observation에 expectedRevision=1을 보낸다.
observation ID는 `/response/effects/receiptId`, 확정 segment는
`/response/effects/segmentId`, event/claim/canonical은 각각 공개 반환
`id`/`claimId`/`id`로 참조한다. 원래 assertion pointer는 바꾸지 않는다.

## 다음 actual 이전에 필요한 통합

- prepare의 custody 검사는 현재 confirmReceipt의 옛 inline 물리 slot만
  읽는다. 연결된 receiveProvisional의 transitSegmentId를 추적하지 않아
  T09 receive60을 직접 수령으로 오판한다. 해당 validator 소유자의
  한정 수정과 기존 custody 반례의 회귀 검증이 필요하다.
- T09 STATE_AT/EXISTS_IN/THROUGHOUT은 현행 fixture 기본 definition에
  대응 goal template이 없다. 기본 definition에는 누적 `arrived`와
  `delivered`만 있다. 존재하지 않는 condition ID를 넣어 통과시키지
  않았다. 원래 상태/관측 공백 oracle에 맞는 typed template과 명시적
  conditions·observationPolicy가 필요하다.
- 원래 assertion은 누적 eventKinds에 `CONFIRMED_RECEIPT`를 요구하지만
  실제 ReceiptFactAdmission은 `PHYSICAL_RECEIPT` canonical을 받는다.
  문자열만 바꾸면 assertion을 위반한다. 제품의 public goal event 의미와
  canonical kind의 연결을 해결해야 한다.
- T12 미근거·불충분 범위·상충 주장은 여전히 확인 수령이 아니다.
  관측 저장 성공이 해당 주장의 판정/상충 flag 반영을 증명하지 않는다.
- 수령 후 T11 Work 기여 범위/참조, T11·T12 수령 정정, T12 기한 변경의
  공개 입력도 남는다. JSON 목록은 관련 unpublished input을 기록한다.
  미구현 의미를 기존 primitive의 비슷한 입력으로 바꾸지 않았다.

첫 actual frontier는 T09 `cumulative-versus-state/state`의 definition과
연결된 `receive60` source graph의 서버 실행이다. T11은 `parent90-*/receive90`, T12는
음성 주장 저장 후 판정 및 `wrong-warehouse/receipt` graph를 확인한다.
이후 실패도 기존 assertion으로 판정한다.

## 결정적 검증

`test_source_contracts.py`는 연결된 10개 graph, 공개 slot 검사, 269 assertion의 raw JSON
block 불변, 음성 주장 보존, 원문 hash/동일성, graph의 명령 순서를
검사한다. 제품의 실제 `receiptPayloadMatches` 메서드를 Java21로
컴파일하여 정상 원문과 18개 필수 필드 누락/불일치 반례를 검사한다.
이 순수 matcher 검사는 DB·인가·MCP·actual scenario 인수가 아니다.

기존 `test_cumulative_goal_setup.py`의 기간/음성 검사는 유지한다.
ReferenceResolver unit은 새 UUID binding과 실행 순서, 원문 bytes/hash,
잘못된 입력과 상한, assertion 금지를 검증한다.
전체 로그는 ignored `.local/source-graph/`에 둔다.
actual scenario는 실행하지 않는다. 통합 후 coordinator가 Haiku 5.5
native dynamic Workflow로 실행해야 한다.
