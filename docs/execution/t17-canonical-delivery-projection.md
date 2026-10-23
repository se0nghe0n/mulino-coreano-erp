# T17 canonical 인도 원천 projection

Task와 현재 Step는 OPEN이다. baseline은
`648ca63810470425cce8598a338a2ec3fa45ed91`이다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-59421ce9`이며
branch는 `fix/t17-canonical-delivery`다. 실행은
`codex / gpt-6.1-sol / medium / serviceTier=default`다.
primary가 실제 thread 설정을 확인했고, local runtime과 T3 thread는
provider/model/tree/branch를 확인했다. Ultrafast를 사용하지 않았다.

## 이유와 범위

저장된 Sales Deliveries에는 canonicalOccurrenceId가 있지만, observer의
raw deliveries에는 canonical marker가 없었다. 이 변경은 실제 저장
원천과 물리 범위의 연결을 독립 확인한 경우에만 marker를 노출한다.
canonical은 SELL 허용, legitimateQuantity 또는 목표 기여 판정이 아니다.
원행의 수량·단위·시각·상태와 legitimateQuantity를 바꾸지 않는다.

대상은 B-canonical-delivery의 다음 다섯 assertion이다.

- normal-consumed-delivery/delivered-2: 실제30이다.
- late-restriction-actual-delivery/actual-delivered-2: 실제20이다.
- late-restriction-actual-delivery/fact-not-permission-9이다.
- post-dispatch-expiry/actual-delivered-2: 실제20이다.
- post-dispatch-expiry/fact-not-permission-9이다.

T17 원래 257 assertions, fixture, actor/grant/source/actions/clock와
기대값을 보존한다. backend/schema/인가를 바꾸지 않는다. 기존 T09
Goal/Input과 T11 wait projection도 보존한다.

## 저장 원천과 실패 경계

`ScenarioObservationProjection.canonicalDelivery`는 동일 connection의
bound SQL로 다음 관계를 읽는다. 연결 대상은 같은 organization이며,
recordedAt이 요청 knownAt 이하여야 한다.

1. Deliveries의 canonicalOccurrenceId가 가리키는 CanonicalOccurrences는
   PHYSICAL_DELIVERY / KNOWN / COMPLETE여야 한다. Work/item/place/unit,
   실제 수량과 occurredAt/effectiveFrom이 같고, physicalScopeId는 정확한
   observationId여야 한다. 같은 canonical의 delivery 소유 행도 유일하다.
2. Observations와 Dispatches의 실제 ID, Work, 품목/LOT, 고객, 주문 line,
   cargo, root, 단위와 목적지를 대조한다. observation의 원 주장 root도
   대조한다. 인도 구간은 dispatch 구간 안에 있고 인도는 출고보다 이르지
   않아야 한다. 이미 발생한 실제 사실만 읽는다.
3. 해당 canonical의 VERIFIED Verifications가 정확한 claim과 원문
   document를 연결하고 다섯 match flag가 모두 true여야 한다.
   Claim/Event의 typed DISPATCH subject, Work/item/place/profile, 수량과
   시각을 대조한다. 원문 payload의 인도 사건·observation·dispatch·cargo·
   line·고객·품목·LOT·root·구간·장소·단위·시각도 같은 사실이어야 한다.
4. SourceProfiles의 namespace/policy와 유일한 InboxRecords의 원천 key,
   eventId/payloadHash를 대조한다. CONFLICT와 알려진 supersedes/invalidates
   원천은 marker를 만들지 않는다. 원천의 유효 종료가 있으면 인도 시점이
   그 이전이어야 한다. 유효 연결이 여러 개면 선택하지 않는다.
5. 명시한 `verification.actual.blobRoot/objects/<blob UUID>`의 원문을
   읽어 DocumentVersions.sha256/byteLength 및 Event.payload와 대조한다.
   blob이 없거나 변경됐으면 marker를 만들지 않는다. blob은 immutable
   object를 읽으며 DB의 MVCC snapshot 자체라고 주장하지 않는다.

이 관계가 모두 확인되면 `canonical=true`와 정확한 row ID를 담은
`canonicalSource`를 추가한다. 증명할 수 없으면 canonical을 생략한다.
원행은 삭제하지 않으며 unknown을 false 사실이나 수량0으로 바꾸지 않는다.
새 판정·인가·업무 정책을 만들거나 제품 명령을 호출하지 않는다.

SQL은 observer의 read-only REPEATABLE_READ transaction을 그대로 쓴다.
원행과 모든 연결 SQL/boundValues는 deliveries의 sourceEvidence에 남는다.

## 검증과 증거 범위

집중 unit 8건과 실제 PostgreSQL source-row tests 32건을 실행했다.
정상 연결, 실제30/기여30과 실제20/기여0의 독립 보존, 다른 canonical/Work/
조직/품목/단위/시각/dispatch, 물리 구간 초과, 원문 누락·변조, 미래 원천,
미검증, 다중 연결과 조회 무효과를 확인했다. concurrent verification
변경 뒤에도 기존 reader snapshot은 같은 연결을 읽고 새 snapshot은
marker를 생략한다. 이 tests의 source graph는 합성 CSN 원행이다.
late restriction/expiry 정책을 제품에서 다시 실행했다는 뜻은 아니다.

`21850e42`와 `db594723`의 기존 자료를 진단으로 읽었다. 두 자료 모두
normal의 실제30/기여30과 late restriction/expiry의 실제20/기여0을
보존한다. 후자의 세 delivery 원행을 새 PG에 넣어 읽은 검사는
**source-row replay**다. 캡처에는 canonical/verification/document 전체
원행과 blob이 없어 canonical 생략을 확인했다. 부족한 원천을 fixture나
기대값으로 복원하지 않았다. 이 replay는 제품 재실행이나 다섯 actual
assertion의 PASS 증거가 아니다.

초기 PG 시도는 이 tree에 생성 CSN이 없어 환경 오류로 실패했다.
이후 baseline의 backend/srv를 기존 local CDS compiler로 읽어 `.local`에
CSN을 생성하고, 테스트 root에서 현재 verification을 참조해 재실행했다.
backend 파일과 schema는 수정하지 않았다. `./verify prepare`는 exit0다.
정확한 명령·exit/count·fixture hash·source pointer·cleanup은
`.local/t17-canonical-delivery/handoff.json`과 실행 log에 기록한다.

`./verify scenarios --actual`, API/실모델, 추가 위임, BTP, push, merge,
Grok 알림은 실행하지 않았다. 별도 Haiku의 고정 tree도 변경하지 않았다.
다섯 assertion의 새 actual 인수는 NOT_RUN이다. T17 eligibility17,
fulfillment/effect/provenance/claim/responsibility/assessment/recall은
별도 OPEN이다. Task와 Step를 닫는 근거로 사용하지 않는다.
