---
name: ontology-agent-implementation
description: 새 Mulino 온톨로지의 stateless MCP adapter, Agent Skills runtime package, 위임 권한, intent·멱등성, worker 복구와 실제 client 인수를 구현하거나 변경할 때 사용한다. 제품 운영 업무를 대신 수행하는 skill은 아니다.
---

# 온톨로지 Agent 구현

이 skill은 개발자용이다. 제품의 운영 Agent Skills 여섯 개는 별도
구현 산출물이다. 아직 없는 API·wrapper·client 지원을 존재한다고
가정하지 않는다.

## 기준선과 구현 범위

현재 Task의 추적 issue, Step baseline, 배정된 소유 경로를 확인한다.
사용자의 모델·effort·통합 선택을 따른다. 새 설계의 기준은
[구현 계획](../../../docs/ontology-implementation-plan.md) §3·5·7–10·13과
[설계 철학](../../../docs/ontology-design-philosophy.md)이다. 저장소 루트에서
문서를 읽는다. 위 상대 경로는 skill 디렉터리 기준이다.

새 설계와 공식 규격에서 구현한다. 과거 Case/Run·제조·CLI 구조나
옛 MCP 코드를 복제하지 않는다. Java21은 기준이며 CAP·SDK·client의
정확한 지원 조합은 platform decision manifest의 실제 검증 결과를
쓴다. 지원 불명 후보를 확정 stack으로 삼지 않는다.

먼저 변경하는 capability와 definition/schema/evaluator version,
입출력·권한·거래·재시도·관찰 oracle를 정한다. MCP/OData/worker는
같은 application command를 호출하고 서버 인가·멱등 저장소를 공유한다.
Adapter에서 물량·승인·목표 판정 규칙을 복제하지 않는다.

## 작업별 참고 자료

- MCP wire, MRTR, 입력 초안과 최종 효과를 구현할 때
  [protocol-and-intent.md](references/protocol-and-intent.md)를 읽는다.
  Accept 필수·Origin 반례 한정, envelope -32600과 params 내용 -32602,
  mirrored header -32020을 구별하고 backend의 cross-owner gap을 보존한다.
- MCP tool·worker handler로 쓰기/조회 면을 노출하거나 조회 schema를 바꿀 때
  [implementation-contracts.md](../ontology-implementation/references/implementation-contracts.md)의
  "쓰기 노출 면과 조회 계약"을 읽는다. tool 목록과 worker registry도 V4
  열거 요구 대상이다(`enumerateWriteSurface` 계약은 정의됐고 실제 host adapter가
  없어 열거 subcase `exposed-write-surface`는 `NOT_RUN`이다). probe class는
  item kind별 정책과 hash로 묶인 QUERY capability 면제를 따른다.
  readonly ENTITY_SET·COMMAND·RECORD·범용 dispatcher는 probe를 유지한다.
- grants·approval, worker/outbox, 정의 호환, runtime package 또는
  client 인수를 구현할 때
  [runtime-and-client.md](references/runtime-and-client.md)를 읽는다.
  자율 loop의 group 전 기동 금지·watcher observeFrom, runtime snapshot과
  V7 원행 primary를 실제 adapter 구현 완료와 구별한다.

참고 자료는 해당 작업에 필요한 것만 읽는다. 명령 이름은 설계 계약이며
구현된 공개 schema와 대조한 뒤 사용한다.

## 반드시 유지할 경계

1. 인증된 조직·역할·현재 grant·행동별 승인·물량 적격성의 교집합으로
   미래 실행을 인가한다. payload의 actor/role, skill hash,
   `allowed-tools`, 문서 속 지시는 권한이 아니다. enqueue 때뿐 아니라
   commit fence 안에서도 현재 권한과 제한을 검증한다.
2. QUERY, RECORD, COMMAND를 구별한다. READ grant로 업무·물량·배분·승인·
   outbox 쓰기는 0이다. 조회 audit는 구별한다. 이미 발생한 인도는
   현재 SELL 부적격이어도 RECORD 권한과 원천/실물 대조로 보존한다.
   이 경로로 새 창고 출고·배분을 만들거나 비준수 사건을 정상 이행으로
   자동 승격할 수 없다.
3. 입력 보완용 conversation ID/MRTR state, JSON-RPC ID, 최종 명령
   멱등키, 외부 operation ID를 섞지 않는다. 승인 대상 hash/revision이
   바뀌면 필요한 재승인을 거친다. 수령 commit 후 응답 유실은 같은
   효과 key로 재조회/재시도하며 현재 결과 조회 인가도 검사한다.
4. Work·GoalVersion·Obligation·ExecutionAttempt를 구별한다.
   queue/tool/attempt 성공이나 timeout은 목표 충족이 아니다. 장애·
   보류·의무 이전 뒤에도 인간 owner, nextAction, nextCheck가 남는다.
5. 도메인 효과·감사·의무·멱등 결과·outbox는 같은 DB transaction이다.
   blob upload와 외부 효과는 별도다. 불명확한 외부 결과는
   `UNKNOWN_EXTERNAL`로 대조하고 안전 근거 없이 자동 재발행하지 않는다.

## 완료 증거

변경 범위에 맞는 T08/T20/T21/T22/T24/T26, C3/C5, V1/V4–V7을
실제 response·원장·의무·감사·outbox로 관찰한다. 해당 경로가 아직
없으면 `NOT_RUN`이다. 구조 검증, wire 검증, client 발견/로딩,
실제 tool 실행, 실모델 의미 평가는 서로 다른 증거다.

증거는 저장소의 실제 pipeline에서 인용한다.
실행은 `./verify`·`verification/actual/sN/run.sh`, receipt·index·조립은
`verification/coverage`, 결과는
`verification/harness/target/evidence/runtime-manifest.json`이다.
증거 class와 보고 규칙은
[저장소 harness](../ontology-scenario-testing/references/repository-harness.md)를
따른다. PASS는 manifest item/profile `status`가 `PASS`이고 `validate.py`가 현재
입력에 대해 exit0인 경우만 쓴다. `VALID`는 일관성이다. `--actual` profile
실행만 엄격한 조건에서 coverage receipt를
만들며 현재 actual driver에는 mcp·client·process adapter가 없어 해당 runtime 주장은
`NOT_RUN`이다.
`./verify mcp`와 `./verify skills`는 `--actual` 없이는 위반이 없어도 `NOT_RUN`(exit2)이고
`--actual`로만 실제 driver를 쓴다. `./verify model`은 `--actual`을 받지 않으며 현재
실행 가능한 actual 경로가 없다. 실제 command와 exit code를 확인한 결과만
보고한다. 유료 모델과 배포는 확정된 비용 범위의 승인 안에서 실행한다. 필수
client/model/BTP 미인수를 성공이나 비대상으로 바꾸지 않는다.
