# Case 증거·Claim 계약 (#44)

원자료의 존재와 지지 관계는 사실 검증이나 ERP 승인 권한이 아니다.
이 API는 관측과 주장, 인간의 명시적 판단을 분리한다. local Human
공유 key는 역할 신원이며 개인 동의나 독립적인 외부 검증의 증거가 아니다.

## 권한과 등록

local에서 활성 OPERATOR·MANAGER는 자신이 참여하거나 개설한 Case,
또는 사용자 배정 작업이 있는 Case에 쓸 수 있다. 에이전트는 활성 Case
참여자이면서 같은 Case의 live Run과 현재 작업 배정을 가져야 한다.
매 요청은 transaction 안에서 DB 역할·활성 상태·Case·lease를 다시
확인한다. 동일 key 재전송도 권한 검사를 생략하지 않는다. VIEWER,
worker service, anonymous는 새 쓰기 API에 접근하지 못한다.
!local의 기존 anonymous intake는 유지하며 새 API는 닫힌다.

인간 prefix는 `/api/v1/epistemic/cases/{caseRef}`, 에이전트 prefix는
`/api/v1/agent/epistemic/cases/{caseRef}`다. 쓰기는 `Idempotency-Key`가
필수다. 같은 actor·Case·operation·key와 같은 입력은 원래 응답을 반환하고,
다른 입력은 409다. 서로 다른 key로 동일 관계를 연결해도 중복하지 않는다.

| 경로 | 입력 | 결과 |
|---|---|---|
| POST `/evidence` | sourceType, externalRef, observedAt, title, content 또는 contentUri·contentHash | immutable Evidence |
| POST `/claims` | subjectType, subjectRef, claimText; 선택 supersedesClaimId·supersessionReason | ASSERTED Claim |
| POST `/claims/{id}/links` | evidenceRef, relation: SUPPORTS/REFUTES | 같은 Case 연결과 최신 Claim |
| GET `/claims/{id}` | 없음 | 원본·정정·판단 이력, revision·evidenceFingerprint |
| POST `/claims/{id}/judgment` | status, expectedRevision, evidenceFingerprint, reason | 명시적 인간 attestation |

sourceType은 EMAIL/EXCEL_3PL/API/SLACK/MANUAL이다. externalRef는 외부
원본의 식별자이며 title과 내용은 비어 있을 수 없다. observedAt은 offset이
있는 ISO 시각을 받아 UTC timestamp로 저장한다. inline content는 서버가
UTF-8 SHA-256을 계산하며 제공한 hash가 다르면 거절한다. 외부 contentUri는
credentials 없는 HTTPS URI와 SHA-256이 필요하다. 서버는 URI를 fetch하지
않으므로 hash는 내용 수집이나 진실성을 검증한 결과가 아니다.
작성자 principal, user/agent, Run ID는 서버에서 기록한다.
응답의 DB 원본 필드는 snake_case이며 조회 metadata는 evidenceFingerprint,
supports, refutes, sources, judgments다. 원본의 숫자·문자열은 변경하지 않는다.

## 판단과 정정

Claim은 ASSERTED로 시작한다. SUPPORTS만으로 VERIFIED가 되지 않는다.
VERIFIED는 support가 있고 refute가 없을 때 인간이 지정한다. REFUTED는
refute가 있고 support가 없을 때 지정한다. 반박이 있으면 CONFLICTED를
선택할 수 있고 지지와 반박이 함께 있으면 VERIFIED/REFUTED를 거절한다.
어떤 상태도 ERP write·governance approval을 만들지 않는다.

현재 revision과 모든 연결 원본·정정 chain의 canonical SHA-256을 보고
인간이 reason과 함께 판단한다. 오래된 revision/hash는 409다. 판단은
별도 append-only `claim_judgments`에 이전 상태·새 상태·reason·fingerprint,
실제 principal/user와 시각을 보관한다.

정정은 `/evidence`에 correctsEvidenceRef와 correctionReason을 추가해
같은 Case의 successor를 만든다. 한 predecessor에는 successor 하나만
허용하며 추가 정정은 마지막 successor를 참조한다. 기존 source는
UPDATE/DELETE/TRUNCATE가 불가능하다. Claim assertion과 관계도 지우지
못한다. 새로운 관계 또는 정정 chain 변경은 revision을 높이고 과거
판단을 이력으로 보존하며 현재 judgment_stale=true로 바꾼다. 반박 근거가
있으면 CONFLICTED, 없으면 ASSERTED로 돌아가 새 인간 판단을 기다린다.

V31과 독립 DDL23의 trigger는 같은 Case 관계·Run 출처를 검사하고,
모든 link와 여러 차례 정정에서 판단을 무효화한다. legacy nullable Case
원본과 작성자 NULL은 유지하며 작성자를 소급 생성하지 않는다.
Dispatcher는 기존 legacy 관계·Event 중복 계약을 유지한다. 새 managed
Claim/Evidence는 신뢰할 수 있는 actor가 없는 worker Event 경로에서
연결할 수 없다. 인증된 module API와 Dispatcher는 `ClaimEvidenceLinks`
INSERT를 공유한다. Case 없는 Event의 기존 글로벌 scope도 유지한다.

## 컨텍스트와 transport

Case `epistemic.evidence[].provenance`에는 전체 immutable 원본과 correction
참조가 남고 Claim에는 revision·stale·fingerprint 및 판단 이력이 남는다.
새 Run도 이 Case 상태를 조회한다. historical writer NULL은 그대로 보인다.

[CLI](../agents/cli/README.md)의 evidence register, claim create/link는
Run token만 사용한다. 인간 stdio MCP는 register_evidence, create_claim,
link_claim_evidence, get_claim, judge_claim을 제공한다. judge_claim은 최신
조회 후 인간이 명시적으로 선택한 상태·근거만 전달한다. 자동 재시도나
과거 동의로 판단하지 않는다. Human/service key는 Run context·env·argv에
전달하지 않는다. OAuth와 실제 ChatGPT/Codex UX는 이 구현의 검증 범위 밖이다.

## 검증 목적

Goal 2는 지지와 인간 판단 및 ERP 승인이 분리되는 결과, Goal 4는 새 Run의
동일 Case 원본·정정·판단 이력 유지, Goal 5는 다른 Case·권한 없는 actor·
만료 Run이 write와 replay를 만들지 못하는 결과로 검증한다.

`observed_instant`는 새 관측의 offset 시각을 TIMESTAMPTZ로 보존한다.
기존 `observed_at`의 UTC projection과 legacy 값은 변경하지 않는다.
연결 row의 writer_principal/user/Run도 실제 writer를 기록하며 legacy
Event 경로는 없는 actor를 만들어 채우지 않는다.

## 반박 정정 후의 새 주장

정정 successor는 원본 관계를 감사 context에서 물려받는다. REFUTES 원본을
정정해도 기존 반박을 없애거나 Claim을 자동으로 해결하지 않는다. 따라서
기존 Claim에 SUPPORTS와 REFUTES가 모두 있으면 그 Claim은 VERIFIED/
REFUTED로 바꿀 수 없다. 원본과 successor를 함께 연결해도 source/relation
쌍은 조회와 fingerprint에서 한 번만 계산한다. 반대 relation은 각각 남긴다.

새로운 해석이 필요하면 `/claims`에 supersedesClaimId와 supersessionReason을
넣어 새 ASSERTED 주장을 만든다. predecessor는 같은 Case·subjectType·
subjectRef여야 하며 rationale은 필수다. 이 immutable pointer는 기존
Claim의 상태·판단·counterevidence를 변경하지 않는다. 새 Claim에 검토한
현재 source를 명시적으로 연결한 뒤 별도 revision/fingerprint와 인간
판단을 기록한다. 새 VERIFIED가 기존 CONFLICTED를 지우거나 ERP 승인
권한을 만들지 않는다. 모든 Claim과 supersession 이유는 새 Run에 남는다.

각 transition의 previous_resolved_at도 저장하므로 기존 VERIFIED의 해결
시각이 ASSERTED projection의 NULL로 바뀌어도 원래 시각은 이력에 남는다.
legacy 판단의 reason·fingerprint·작성자가 없으면 만들지 않고 NULL을 유지한다.
