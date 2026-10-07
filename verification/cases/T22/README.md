# D22의 원천·외부 결과 대조 계약

source key와 실물 동일성은 다르다.15개 subcase는 원문 두 hash,
canonical60 BOX, 명시 대조 권한과 외부 결과를 별도로 검사한다.

- source-key-hash-conflict는 carrier/EV60/v1의 같은 hash 재전송을
  accepted1로 보존한다. replay는 다른 command key로 source 중복을
  직접 검사한다. 같은 source key의 다른 hash는 별도 상충 claim이다.
  두 원문은 실제 다운로드 artifact의 hash로 다시 확인한다.
- two-sources-one-occurrence는 `recordActivity`·identity match·canonical
  연결·`confirmReceipt`를 명시 실행한다. warehouse 보고가 미확인인
  동안 원장 변화는0이고 연결 뒤에도 receipt는60 BOX 하나다.
- cross-org-match·overlapping-match·arbitrary-priority·unauthorized-match는
  금지된 identity/canonical/원장/판정 변경0과 허용 대조 책임을 구별한다.
  조직 경계는 올바른 TradeItem 타입의 조직B 대상을 사용한다. 중첩은
  A60 식별1–60과 B40 식별41–80의20 BOX 교집합을 원문으로 대조하며
  이미 인정된 receipt60 BOX를 추가 수령으로 확대하지 않는다.
- late-old-release는 순서가 미확정인 옛 release가 revision2의 새 hold를
  지우지 못하는지 원 restriction row 전체와 수량·단위로 검사한다.
- unprofiled-automatic-source는 DB profile과 host의 실제 활성 namespace를
  읽는다. 원문 접수와 connector 활성화를 같은 의미로 처리하지 않는다.
- reviewed-source-profile은 basis·namespace·mapping·revision/time 신뢰·
  schema·dedup·owner·retry·externalWrites를 실제 row tuple로 검사한다.
  미확정 처리는 QUARANTINED와 다음 확인 책임으로 남긴다.
- external-unknown/success/failure-retry/failure-no-retry/no-lookup/
  local-cancel은 실제 outbox를 만든 뒤 scheduler tick와 autonomous task의
  terminal ACK를 기다린다. responder는 실제 요청 기록과 상태 조회
  artifact를 제공한다. 알려진 실패 branch는 REJECT 후 응답 유실이다.
  성공 branch는 ACCEPT 후 응답 유실이다. 원문 proof는 `attachEvidence`
  후 `recordExternalReconciliation`에 연결한다.

미확정과 확인된 성공은 요청 수1이다. 실패 확인과 policy 허용이 있는
branch만 같은 externalOperationId로 요청 수2다. 새 시도 응답도 유실되면
다시 UNKNOWN_EXTERNAL이고 retryEligible=false다. 로컬 취소는 상대에
CANCEL 요청을 만들지 않는다. 미래 operation ID·artifact hash를 fixture로
예측하지 않고 실제 결과를 후속 request로 연결한다.

raw source 원문은 편집하지 않는다. 원 namespace별 신뢰·중복키·
대조 담당을 fixture에 명시하며 가상 profile을 공식 기관 연동으로
표시하지 않는다. 미확인·상충 scope의 책임은 인간 intake, 고정 다음
행동/nextCheck, 현재 assignment1로 API와 DB 양쪽에서 검사한다.

API/DB/host adapter와 실제 외부 시스템은 NOT_RUN이다. 가상 responder
control의 계약은 실제 adapter가 연결할 지점이며 구현된 fake가 아니다.
