# T13 공급자 회신의 공개 입력 연결

Task와 current Step은 OPEN이다. 이 문서는 baseline
`bcac964ced0d5cf47a990afabba5eddefb66b2c8`에서 수행한 한정 Subtask의
통합 인계다. root coordinator가 통합과 최종 검증을 소유한다.

## 변경과 보존

`supplier-accept`, `supplier-reject`, `supplier-change`에만 별도 synthetic
전송확인과 공급자 원문을 추가한다. `author_supplier_sources.py`는 원래
요청의 수량100/변경80 BOX와 reply를 읽고, assertion expected는 읽지 않는다.
원문에는 `newSynthetic=true`, `originalAuthorityClaimed=false`,
`operatingApprovalClaimed=false`를 명시한다. 원래 supplier-ship 파일과
17개 source descriptor, 가격·통화·기한·조건·기존 사업 actor는 보존한다.

원래 135 assertion과 세 시나리오 문장은 그대로다. 새 행동은 별도
ledger의 `addedActionIds`에 기록한다. `supplier-source-ledger.json`은
원래 세 subcase 전체와 생성 결과 hash를 보존한다. strict inverse는
미등록 변경을 거부하고 baseline을 재현한다. 세 fixture의 가역 metadata는
`baseline.policy.supplierSourceAuthoring`에만 둔다.

전송 대조의 입력은 공개 `outboxId/decision/evidenceId`다. 불필요한
expectedRevision과 정의에 없는 Work subject action을 보내지 않는다.
outbox와 command ID는 실제 dispatch 응답에서 연결한다. 외부 작업 ID는
원래 요청의 UUIDv5를 그대로 사용하며 다른 ID를 만들지 않는다.
공급자 문서는 `PURCHASE_ORDER` subject, 정확한 `SUPPLIER_*` kind,
order/item/quantity/unit/proposalRevision으로 현재 검증기를 통과해야 한다.
문서 upload → event/claim → matchSourceIdentity → linkCanonicalOccurrence를
모두 공개 HTTP로 요청한다.

새 `t13Source` actor는 기존 조직·기한·delegator와 품목 P의 all-of scope로
제한한다. 기존 source 전용 synthetic actor에는 현재 evidence guard가
필요로 하는 `getEvidence` 읽기만 추가한다. 원래 source actor 전체를
metadata에 남기며 다른 사업 actor/grant는 변경하지 않는다.
ScenarioFixtureInstaller hook은 필요하지 않았다. 공통 resolver, observer,
runtime, 제품 backend, DB migration, slot catalog는 수정하지 않았다.

`PurchaseInputProbe.java`의 저장소 샘플에는 현재 공개 필수 계약인
`SINGLE_ORDER_REVISION`을 추가했다. 이 샘플에 값이 없어 승인 최신성 guard
이전에 실패하던 기존 테스트만 수정하며 사업 fixture의 승인은 바꾸지 않는다.

## 실제 실행과 남은 제품 경계

`T13SupplierSourceFactsPostgresTest`는 이 tree에서 새로 빌드한 JAR,
Java21, 임시 RSA key, 독립 PostgreSQL을 사용한다. 실제 HTTP 요청·응답과
독립 SQL 원행을 `.local/t13-supplier-pg-*/`에 보존한다. 테스트 helper는
공개 요청을 보내고 SQL은 읽기만 한다. 원래 scenario runner나 model client는
실행하지 않는다. 임시 key와 생성한 DB/container만 정리한다.

공급자 source 자체의 독립 검사는 전송 대조를 건너뛰었음을 명시한다.
ACCEPT100/REJECT100/CHANGE80의 서로 다른 commitment, 주문100 보존,
도착량/재고0, outbox1, 거절·변경 뒤 OPEN 담당/다음 행동/기한,
동일 key replay와 중복 canonical 거부를 검증한다. 이 검사는 전송 성공이나
전체 case PASS를 뜻하지 않는다.

다음은 소유 밖 제품 문제다. 기대값을 바꾸거나 상태를 seed하지 않았다.

1. `RuntimeService.claimDelivery`는 원래 procurement의 ITEM 제한 grant를
   WORK scope만으로 검사한다. 유효한 품목 grant도 fail-closed되어 실제
   outbox가 `PENDING`, attempts0에 머문다. 최소 수정 범위는 저장된 원래
   command의 검증된 scope와 현재 대상의 ITEM/WORK를 일관되게 재검증하는
   delivery authorization이다. 원래 actor의 권한을 넓히면 안 된다.
2. `RuntimeFixtureDeliveryAuthorization.require`는
   `fixtureExternalEffect`만 지원한다. purchase 전송의 실제 adapter/현재
   승인·정책·원명령 인수는 이 한정 변경으로 구현할 수 없다. 인수 범위는
   purchase dispatch의 원래 명령과 승인 hash/current authority를 유지하는
   외부 전송 경로다. synthetic responder 설정은 실제 외부 공급자 전송이 아니다.
3. `PurchaseCommands.recordSupplierReply`는 전송 확인이 없는 주문에도
   검증된 supplier canonical만 있으면 commitment를 기록한다.
   `missingTransmissionConfirmationCannotCreateCommitment`는 요청된
   무효과/HELD 기대를 유지하므로 실패한다. root가 전달/회신 분리 규칙과
   사용자의 추가 인수 조건을 함께 해결해야 한다.

4. 원래 `reply-distinct`를 실제 `ScenarioJdbcObservation`에 실행하면
   `projected field missing: purchaseId`다. 현재 SupplierReplies 원행은
   `orderId/proposedQuantity`를 제공하며 원 assertion의
   `purchaseId/itemId/quantity` 관찰 계약이 연결되지 않았다. 최소 범위는
   observer가 같은 조직의 immutable order/line과 실제 reply를 연결해
   세 필드를 제공하고 그 SQL/alias 근거를 기록하는 것이다. observer는
   이 writer의 금지 경로이므로 수정하지 않았다.

잘못된 source identity·수량·단위·시각·대상, 다른 조직, 만료 grant,
불일치 ITEM/WORK scope, 문서만 있는 회신은 별도 실제 HTTP 음성 검사다.
전체 전송+회신 positive 세 건은 위 제품 경계가 해소되기 전 FAIL이다.
`UNKNOWN_EXTERNAL` 상태 자체의 성공 대조 인수는 NOT_RUN이다.
제품 delivery를 우회하는 직접 SQL, 판정/권한 seed, 성공 결과 합성은 없다.

최종 명령/exit/검사 수와 artifact는 같은 디렉터리의
`SUPPLIER-SOURCE-RESULTS.md`에 기록한다. 원본 실패 JSON은
`.local/t13-supplier-work/raw-source-bcac-scenarios.json`에 보존한다.
실모델·Haiku dynamic Workflow·운영 공급자 전송·BTP·배포·merge는 NOT_RUN이다.

## 2026-10-23 coordinator 정정과 delivery 후속 구현

앞선 `missingTransmissionConfirmationCannotCreateCommitment`는 새 probe에
잘못 추가한 기대였다. 계획 §6과 설계 철학은 공급자 회신·발주 전달·도착을
각각 독립 사실로 규정한다. 전달 확인이 없다는 이유로 검증된 공급자 회신을
막는 제품 guard는 추가하지 않는다. 그 새 probe만
`verifiedReplyRemainsDistinctFromUnconfirmedTransmission`으로 교체했다.
원래 135 assertion과 source ledger는 그대로 보존했다. 위 과거 제품 경계
목록의 3번은 철회하며 이전 FAIL은 제품 결함으로 취급하지 않는다.

후속 구현은 제품의 purchase delivery authorization이다. 외부 공급자
연동을 구현하거나 verification responder를 운영 adapter로 바꾸지 않는다.
원래 COMMITTED 명령과 immutable order/line/outbox의 identity를 확인하고,
원 handler의 scope 및 현재 policy/grant/승인을 claim과 IO 직전에 재검증한다.
검증된 supplier 회신은 여전히 전송·도착과 독립이며, responder 응답만으로
SUCCEEDED가 되지 않는다. 상세 결과는 후속 delivery 인계 문서에 기록한다.
