# 서버 계약을 구현하는 판단표

각 변경에서 관련 항목만 읽고 계획의 정확한 필드·전이를 확인한다.

## 정의와 typed predicate — D02/D06/D07/D09/D12/D21

DefinitionVersion·PolicyVersion·GoalVersion·CapabilityContract를 분리한다.
발행 정의는 불변이며 업무는 원 의미/evaluator를 유지한다. 현재 행동은
효력이 있는 policy/grant/제한을 다시 적용한다. manifest는
definition→capability semanticVersion/evaluator/schema/migration 대응을
고정한다. 미지원 handler/evaluator를 최신 버전으로 몰래 바꾸지 않는다.

predicate 발행 검사는 property/slot 참조, 타입, 단위, 시간 의미,
evidence selector를 확인한다. 초기 허용 operator는 `equals/in`, 호환 단위의
`compare/range`, `exists`, 승인 relation의 cardinality, 시간 구간 포함,
`all/any/not`, verified distinct 사건량 합계, 현재 상태량 평가다. 임의 SQL,
script, 새 실행 권한, 범용 DSL은 payload에서 허용하지 않는다.

`all`은 하나라도 확정 false면 UNSATISFIED, 모두 true면 SATISFIED,
나머지는 UNVERIFIED다. `any`는 인정 가능한 true가 하나라도 있으면
SATISFIED, 모두 확정 false면 UNSATISFIED, 나머지는 UNVERIFIED다.
UNKNOWN/CONFLICT를 false·0으로 치환하지 않으며 상충 flag를 보존한다.
`not`에서도 미확인을 참으로 만들지 않는다. Assessment는 조건별 결과,
상충, input snapshot, 근거, evaluator/definition/policy/goal version,
평가시점과 이전 판정을 불변으로 연결한다.

외부 issuer/namespace/유효기간을 품목 ID와 구별한다. 관계는 endpoint
type/cardinality/기간/순환을 검사한다. core 잔량·적격·권한·판정은 확장
속성으로 덮어쓸 수 없다. 금액·수량은 decimal 문자열+unit, DB 기본은
numeric(38,12)다. 허용 정밀도를 넘으면 거부한다. 기준 단위 소수 제약,
시간대/끝점, MISSING/UNKNOWN/NOT_APPLICABLE/CONFLICT를 보존한다.

## application command checklist — D08/D13–D20/D24

명령 하나의 수용 여부를 아래 항목으로 결정한다.

1. `intentKind`, supported capability/definition, stage별 필수 slot과 단위,
   관계 타입을 확인한다. 구매 초안에서 수령 단계의 LOT를 미리 강제하지
   않는다. 해석 초안의 conversationRequestId와 최종 effect key를 분리한다.
2. 인증 문맥에서 organization/actor/delegator를 만든다. 조직 접근∩역할∩
   현재 grant∩행동별 승인∩물량별 허용을 적용한다. payload·문서·role 이름·
   queue system user는 권한을 만들지 않는다. 승인 필요 행동만 지정
   MANAGER/QC/ADMIN/CONFIG_APPROVER 결정으로 연결한다.
3. proposalRevision/canonical intent hash·승인 대상 revision을 검증한다.
   승인100을120으로 바꾸면 재승인이다. policy unresolved는 효과0이다.
4. 같은 효과 scope의 segment/배분/제한/grant/policy fence를 고정 ID 순서로
   잠그고 현재 revision·적격성을 재읽는다. 새 제한 phantom과 commit 직전
   grant 철회를 같은 직렬화 경계로 처리한다. 충돌은 bounded retry 후
   구조화 conflict이며 최신 의도를 몰래 실행하지 않는다.
5. effect class/scope를 application에서 검사한다. 내부 move를 고객 출고,
   반품·폐기·주문 이행의 우회 action으로 열지 않는다. raw ledger/core CRUD,
   direct/nested/batch/projection/MCP/worker 경로에도 같은 guard를 적용한다.
   guard 대상은 아래 "쓰기 노출 면"의 열거 결과이며 고정 목록이 아니다.
6. 효과·의무·판정 pending·audit·outbox·idem 결과를 한 DB transaction에
   기록한다. audit 실패는 rollback이다. 대형/외부 판정은 pending으로
   표시하고 즉시 SATISFIED를 반환하지 않는다.
7. response의 outcome/revision/effect refs/nextAction을 보존한다. 외부 결과
   미확인은 ACCEPTED_PENDING_EXTERNAL 또는 해당 미확인 상태다. 오류에
   다른 조직의 객체 존재·token·비밀을 노출하지 않는다.

## 쓰기 노출 면과 조회 계약 — D01/D08/V4/계획 §3.4/§4.2

**쓰기 노출 면.** CAP은 노출한 CDS 항목에 generic CREATE/UPDATE/DELETE,
`$batch` changeset, deep insert, upsert, draft activation handler를 자동으로
붙인다. 이 경로는 command fence·grant 검사 밖에서 core를 바꿀 수 있다.
따라서 서비스에 projection, action, MCP tool, worker handler, 관리
endpoint를 추가·변경할 때마다 실제 노출 면을 열거한다. 쓰기 가능한 항목은
capability allowlist에 있어야 하고 core entity는 노출하지 않거나
`@readonly`·`@restrict`로 막는다. 규칙 없는 `404`나 "generic projection을
만들지 않는다"는 관례만으로 충족했다고 하지 않는다.

probe 적용 여부는 `writeCapable`이 아니라 item kind로도 결정한다.
readonly ENTITY_SET의 쓰기 거부, BOUND/UNBOUND_ACTION의 자기 call과
BATCH_CHANGESET, TOOL·WORKER_HANDLER·MANAGEMENT_ENDPOINT의 대응 probe를
빠뜨리지 않는다. 다만 TOOL·BOUND_ACTION·UNBOUND_ACTION이 하나의
QUERY capability를 부르면 적용 probe class가 없다. 면제는 hash로 묶인
allowlist의 kind=QUERY, writeCapable=false, itemId가 capability id
자체이거나 `.`·`/` 뒤 그 id인 이름을 harness가 확인할 때만 적용한다.
COMMAND·RECORD·범용 dispatcher·id 없음/목록 밖·다른 이름의 항목은
면제되지 않는다. readonly ENTITY_SET도 계속 probe한다.
`HostObservationValidator`가 면제를 반영해 적용 수와 probe 행을
재계산한다. 전체 표와 실제 adapter의 NOT_RUN은 아래 저장소 harness의
V4 절을 따른다.

이 열거와 우회 시도는 요구다. V4의 고정 route inventory 92개는 열거가
아니며 `exposed-write-surface`가 실제 열거를 맡는다. host 조작
`enumerateWriteSurface`의 schema·guide 계약은 정의됐고 실제 adapter는 없다.
열거 subcase가 실제 PASS하기 전에는 V4 노출 면 인수를 `NOT_RUN`으로
보고하고 수동 열거는 handoff·checks에 남긴다. 정확한 계약과 cross-owner
요청은 [저장소 harness](../../ontology-scenario-testing/references/repository-harness.md)를 따른다.

query/복구 관찰을 연결할 때 API revision의 `RESULT_REVISION`과 task
terminal의 `RUNTIME_TASK_SNAPSHOT`을 구별한다. 전자는 원행 재계산,
후자는 task identity·await 결과·snapshot artifact bytes로 검증한다.
실제 observer의 두 mode와 verifyCoverage host 출력은 Step 3 actual
소유이며 아직 `NOT_RUN`이다. PREPARATION 제품 validator의 checkout
대조를 실제 host 구현 완료로 보고하지 않는다.

**조회 두 진입점.** `getObject`·`searchObjects`·`getWork`·`searchWorks`·
`getInventory`·`getObligations`·`traceLot`·`getEvidence`·`getAssessment`·
`getDefinition`은 같은 application query와 query schema를 공유한다. 명사에서
연결 업무를, 업무에서 대상·물량·목표·근거를 반환하며 같은 ID와
`snapshotRevision`/평가시점이면 결과가 같다. 응답은 `data`, `asOf`,
`knownAt`, `scope`, `unknowns`, `conflicts`, `evidenceRefs`, `nextCursor`를
포함한다(계획 §3.4는 모든 조회 응답에 `nextCursor`를 둔다. 다음 page가
없을 때의 값은 공개 schema가 정한다). 목록은 안정된 ID tie-break와 cursor를 쓰고
page 사이 시점 변화를 응답에 밝힌다. offset paging과 자유 SQL·허용 밖 filter/sort/relation을 만들지
않는다. 검색 결과로 다른 조직 객체의 존재를 드러내지 않는다. 오류는
`TYPE_INVALID`, `VERSION_UNSUPPORTED`, `POLICY_UNRESOLVED`, `FORBIDDEN`,
`STALE_REVISION`, `IDEMPOTENCY_CONFLICT`, `INSUFFICIENT_ELIGIBLE_QUANTITY`,
`EVIDENCE_CONFLICT`처럼 구조화한다. 명령 오류 코드는 응답의 `error.code`
(`contracts/command-response.schema.json`) 하나이며 최상위 `errorCode`·`code`를
만들지 않는다. 의무·판정 원행의 `current`는 revision 유효성이고 `status`와
독립이다. 해소된 의무도 대체되지 않았으면 `current=true`다.

## 수량 원장과 실행 배분 — D03/D04/D05/D16/D17/D18

inventory primitive만 QuantityMovement/GenealogyEdge/Allocation을 쓴다.
분할은 부모 retired+자식/잔여/근거 있는 감소+기존 배분 이관이 원자적이다.
원량=자식량+잔여량+감소량이며 부모 재소비는0이다. 합침은 품목·제조 LOT·
위치·단위·통제 조건이 호환되는 source를 소모한다. 서로 다른 LOT는
LogisticsUnit 안에 함께 둘 수 있지만 단일 LOT segment로 합치지 않는다.
식별 불가능 혼합은 계보 숫자로 깨끗한 subset을 선택하지 않는다.

보유는 active physical leaf를 한 번씩 합산한다. 행동별 적격은 현재
QC/규제/고객 조건/처분 근거의 허용 scope 교집합이다. UNKNOWN은 confirmed
eligible이 아니다. 장소·보관자 판정도 이 교집합에 포함한다.
[fixture 장소 종류](../../../../contracts/fixture-place-kinds.md)의
기계 원본은 같은 이름의 JSON이다. INTERNAL_STORAGE는 같은 조직의
HUMAN/AGENT custodian이 확인돼야 내부 보관으로 판정한다.
TRANSIT·CUSTOMER·SUPPLIER·EXTERNAL_PORT는 외부로 적격 확정0이다.
제품의 그 밖 어휘 밖 kind(`EXTERNAL_*` 제외)는 UNKNOWN·적격0이며
confirmed eligible에 넣지 않는다. 보유 사실은 보존하고 UNKNOWN을
DENIED/확정0으로 바꾸지 않는다. fixture는 다섯 kind만 쓰며 명시
`kindControl=UNRECOGNIZED_PLACE_KIND` 반례 외 미지 kind·누락 kind와
내부 보관자 누락을 prepare가 거부한다. FixtureInstaller·native fixture와
첫 수령 custodian slot의 남은 요청은 저장소 harness의 fixture 절을 따른다.

신규 실행 배분은 적격 실물 이하이며 suspended 배분은
실행 불가여도 기존 의무로 조회된다. QC/정정으로 줄어든 가능량은 과거
예약 삭제가 아닌 부족 의무/대체 배분이다. replace는 원배분 비활성과
대체배분 생성이 원자적이며 과거 예약 자동 부활이 없다.

피킹/출고는 현재 적격성을 commit 경계에서 재검증한다. 출고는 배분을
CONSUMED로 만들고 창고 물량을 운송 위치로 옮긴다. 현재 SELL 부적격은
이미 발생한 인도 사실을 지우는 조건이 아니다. 해당 사실은 아래 RECORD
대조를 거치며 정상 목표 이행 인정 여부는 별도로 평가한다.

nextValidityBoundary에는 LOT 기한·처분 허용·grant·정책의 다음 경계를
등록한다. 무이벤트 만료도 sweeper가 같은 scope lock 안에서 배분 정지와
의무/owner/nextCheck를 upsert한다. sweeper가 늦어도 실행 guard가 출고를
막아야 한다. 보류와 폐기를 같은 수량 감소로 취급하지 않는다.

## 관측→canonical 사실 — D06/D07/D12/D14/D18/D22

inbox unique sourceNamespace+externalEventId+sourceVersion은 원천 중복키다.
같은 key의 다른 hash는 conflict다. 서로 다른 출처의 같은 수령60은
canonical occurrence/실물 범위에 연결해60으로 센다. source key나 문서
hash만으로 실물 동일성을 확정하지 않는다. scope 겹침/동일성 미확인은
claim/quarantine과 대조 owner/nextCheck로 남긴다.

RECORD는 권한 있는 주체·원천·scope와 claim 원문을 접수한다. canonical
반영은 원천/사건/식별/수량/중복/evidence policy 대조를 별도로 통과한다.
`recordDelivery`는 prior Dispatch/CargoScope와 이미 CONSUMED인 배분을
참조한다. 확정된 실제 인도는 운송 실물을 고객으로 대조 이동할 수 있지만
새 창고 출고·예약을 만들지 않는다. prior Dispatch가 없는 관측은 실물
대조/권한 있는 조정으로 처리하고 정상 주문 이행을 자동 생성하지 않는다.
허위/상충 보고의 정상 재고·이행 효과는0이다.

관측은 supersedes/invalidates로 정정하고 발생/기록 시각을 분리한다.
실제 반품은 새 receipt이며 과거 인도량을 깎는 정정이 아니다. 실제98의
인도100 정정은 과거 판정 보존+현재 유효 의무2 평가다. 이미 해소/면제된
의무는 부활시키지 않는다. 현재 projection/예약/판정/의무 재평가는 원자적
또는 명시 pending이며 미확정 영향 범위의 후속 실행을 막는다.

## S4 정산 복원·면제와 정정 영향 — D12/D19

[S4 closure 3](../../../../docs/execution/s4k-closure/README.md)의
`3addaefe` 구현 규칙을 보존한다. 수령/인도 기여를 복원해 Match가
CURRENT이지만 SATISFIED가 아니고 열린 차이 root가 있으면 열린
assignment를 다시 발행한다(`ResponsibilityService.reissueOpen`).
owner·work·root는 유지하고 revision을 올리며 복원 nextAction·nextCheck와
`CONTRIBUTION_RESTORED:<canonical>` basis를 기록한다. 같은 basis 재처리는
revision을 다시 올리지 않는다. 정정 전 assignment revision에 묶인
면제 결정은 실행 시 STALE_REVISION으로 거부하고, 현재 revision으로
새 결정한 면제만 실행한다. 복원과 면제 실행 순서로 책임이 사라지면 안 된다.

열린 root가 없을 때 CURRENT 잔여를 이미 유효하게 면제한 root가 덮으면
새 root/follow-up을 열지 않는다. Match 자신의 root는 불변
`originalDifference`, CURRENT 복원 root는 scope residual의
`settlementDifference`를 비교한다. 금액이 다르거나 확인할 수 없으면
새 책임을 여는 기존 fail-closed 규칙을 따른다. 새 CHANGED 정정의 의무와
이미 면제한 같은 잔여의 자동 부활을 구별한다.

`AssessmentCorrectionImpact`의 `evidenceLinked`·`apply` 영향 집합은
IMPORTED Work를 제외한다. 가져온 행은 revision·pendingInvalidation을
바꾸지 않고 knownAt 조회에 보존한다. 직접 대상으로 정정해도 IMPORTED
Work에 무효화 표시나 FOLLOWUP_REVIEW를 만들지 않는다. 해당 참조 업무의
후속 책임 위치는 work/responsibility owner의 S5 backlog다.

PHYSICAL_DELIVERY 증거의 명시 invalidatesId는 UNVERIFIED Match에 owner
있는 SETTLEMENT_DIFFERENCE를 연다. 일반 supersession 뒤 relink하지 않은
SALE 인도는 Match UNVERIFIED와 owner 있는 FOLLOWUP_REVIEW로 남으며,
단일 정산 root를 잇는 계약은 settlement owner의 S5 backlog다.
이 두 DEFERRED 항목을 구현 완료로 안내하지 않는다. closure 기록의
backend 513 tests와 native S1–S4 PASS는 이전 한정 증거이며 coverage
manifest PASS가 아니다. 결합 `./verify scenarios --actual` 인수는
그 기록에서 NOT_RUN이고 이번 skill 변경으로 해소되지 않는다.

## 목표·의무·인계 — D09/D10/D11/D12/D26

수량 모드는 CUMULATIVE_EVENT/STATE_AT/EXISTS_IN/THROUGHOUT 중 하나다.
사건 종류·기간·기여/중복배제 또는 평가시점·장소·행동 적격·예약 포함,
관측 정책을 필수로 받는다. 부모 목표100의 기여90은 자식 종료나 의무10
이전으로 FULFILLED가 되지 않는다. 기한 변경은 과거 위반을 지우지 않는다.

WAITING에는 reason/대상/resume predicate/verifier/nextCheckAt/overdueAction이
필수다. timeout·queue delivery·ExecutionAttempt 성공은 업무 성공이 아니다.
close는 현재 GoalVersion 판정·pending 영향·잔여 의무를 확인한다.
FULFILLED 이외 종료도 실제 외부 계약/출하를 자동 취소하지 않는다.
폐쇄 업무 정정은 과거를 reopen해 덮기보다 followup과 현재 의무를 연결한다.

의무키는 source+kind+scope+발생 version에 안정적이다. stable root당 현재
유효 assignment는 하나다. transfer는 수락 후 원 assignment TRANSFERRED와
대상 OPEN을 같은 거래로 연결하며 수락 전 원 책임이 남는다. 부분 이전은
겹치지 않는 scope와 원량=이전량+잔여량을 보존한다. 순환을 거부한다.
owner 인계도 수락 전 기존 인간 owner가 유지되며 긴급 재배정은 ADMIN
사유/감사가 필요하다. 의무 resolve/waive는 종류별 증거·권한을 검사한다.

부모 없는 이상도 intake owner/supervisor·nextAction·nextCheck를 가진다.
연결 실패는 접수 추적을 유지하고 retry에서 같은 의무/업무 하나만 만든다.
정상 관측은 업무0이며 알림 성공은 추적 종료가 아니다.

## 멱등·외부 효과·복구 — D22/D23/D24/D26

멱등 unique는 organizationId+stableRequestOwner+capabilityId+command key다.
stableRequestOwner는 token/RPC/Run ID가 아니다. canonical hash와 version을
저장하고 같은 key/다른 hash는 conflict, 타주체 결과는 비공개다. commit과
결과는 원자적이며 stale claim fencing token의 commit을 막는다. 효과
tombstone은 업무 효과보다 먼저 삭제하지 않는다.

outbox externalOperationId는 안정적이다. 응답 유실은 UNKNOWN_EXTERNAL로
대조하며 멱등 지원/조회 수단 없는 외부 쓰기는 자동 재발행하지 않는다.
confirmed success는 로컬 연결만 하고 다시 보내지 않는다. scheduler는
DB의 due wait/의무, 미연결 intake, 미확인 외부 결과, 만료 claim을 찾는다.
safe retry도 원 canonical 요청/key·현재 grant·claim fence를 검사한다.

복원에는 DB+evidence blob+정의/capability/evaluator/skill artifact가 필요하다.
새 schema v1→v2 upgrade에서 진행 업무·v1 정의·증거·idem을 보존한다.
retention/legal hold/삭제 이력은 복원 환경에도 적용한다. 쓰기 개방 후
새 효과가 있으면 old commit만 되돌리지 않고 요청/outbox 중지→외부 대조→
forward repair 또는 승인된 snapshot 복원+효과 재대조를 수행한다.
