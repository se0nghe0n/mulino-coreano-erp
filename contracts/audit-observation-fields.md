# 감사 관찰 행 field 계약

[`audit-observation-fields.json`](audit-observation-fields.json)은 독립 DB
observer가 `/data/rawRows/audit`와 `/data/rawRows/queryAudit`로 돌려주는
논리 감사 행의 이름 목록이다. 이전에는 case마다 같은 감사를 `commandKey`·
`commandIdempotencyKey`, `action`·`capabilityId`, `outcome`·`result`,
`kind`로 다르게 읽었다. 감사 테이블 하나는 이 이름들을 동시에 가질 수
없어서 올바른 제품도 어느 한 계열의 case에서 실패했다. 이 계약은 이름을
backend의 실제 감사 열에서 가져와 하나로 고정한다.

## 원천

| source | 의미 | backend |
|---|---|---|
| `audit` | 명령 감사. 거부 뒤 따로 commit한 거부 감사와 기존 command record의 replay 감사를 포함한다 | `mulino.commands.CommandAudits`를 `commandId`로 `CommandRecords`에 join |
| `queryAudit` | 조회 감사. 거부된 조회도 포함한다. 계획 §7.4 "조회 감사는 업무 상태 변경과 구별한다" | 아직 없음(PENDING_PRODUCT) |

조회 감사를 명령 감사와 다른 source로 둔 이유는 두 가지다. 계획이 둘을
구별하라고 한다. 또 `AssertionEngine`은 `where` key가 source의 모든 행에서
null이 아니어야 하므로, 멱등키가 없는 조회 행이 같은 source에 섞이면
`commandIdempotencyKey`로 고르는 모든 명령 감사 단언이 올바른 제품에서도
"filter field missing"으로 실패한다.

## presence와 where 규칙

- `ALWAYS`: 모든 행에 null 아닌 값이 있다. `where`에 쓸 수 있는 것은
  이 field뿐이다. `audit`는 auditId, organizationId, actorId, commandId,
  commandIdempotencyKey, capabilityId, canonicalHash, outcome, createdAt,
  transactionId, replayAttempt, expectedDenial, effectRefs다.
- `CONDITIONAL`: 조건이 맞는 행에만 있다. 예: `errorCode`는 outcome이
  APPLIED·ACCEPTED_PENDING_EXTERNAL이 아닐 때만 있다. ALWAYS field로
  고른 행에서만 `field`로 투영한다.
- `PENDING`: 계획 §7.4가 감사에 요구하지만 backend가 아직 기록하지 않는
  내용이다(delegatorId, reason, before/after, requestId, approvalRefs,
  evidenceIds, movementId, grantRevision, proposalHash, sweepId). case는 이
  이름으로 읽고, 구현은 Step 3 추가 요청이다. backend 사실로 표시하지 않는다.

## outcome과 오류 code

`outcome`은 [domain vocabulary](domain-vocabulary.md)의 명령 outcome이다.
오류 code를 outcome에 쓰지 않는다. FORBIDDEN 거부는 `outcome=REJECTED`,
`errorCode=FORBIDDEN`이다. `queryAudit.outcome`은 READ 또는 REJECTED다.

`transactionId`는 CommandAudits 행을 넣은 거래의 PostgreSQL 64-bit
transaction id(xid8)를 `xmin`에서 계산한 text다. 같은 계산을 쓰는 다른
원행과 비교해 감사와 업무 효과가 한 거래였음을 보인다.

## 바뀐 이름

| 이전 | 지금 |
|---|---|
| `commandKey` | `commandIdempotencyKey` |
| `action` | `capabilityId` |
| `result=FORBIDDEN` | `outcome=REJECTED` + `errorCode=FORBIDDEN` |
| `kind=DENIAL`/`MUTATION`/`QUERY` | outcome이 APPLIED 아님 / `outcome=APPLIED` / source `queryAudit` |
| `authorizationResult`+`businessOutcome` | `expectedDenial`+`outcome` |
| `id` | `auditId` |
| `basisEvidenceId` | `evidenceIds` |
| `denialAudit` source | `audit`/`queryAudit`를 key·outcome으로 고른다 |

`verification/cases/check_vocabulary.py --check`가 case의 감사 source 이름,
where key의 presence, field 이름과 outcome 값을 이 계약으로 검사한다.
