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
- grants·approval, worker/outbox, 정의 호환, runtime package 또는
  client 인수를 구현할 때
  [runtime-and-client.md](references/runtime-and-client.md)를 읽는다.

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

`verification/manifest.json`에는 commit, 관련 version, fixture hash,
실행 command·시각, expected/observed, artifact와 PASS/FAIL/NOT_RUN을
연결한다. `./verify mcp`, `./verify skills`, `./verify model`은 계획의
납품 entrypoint다. 파일이 존재하고 실제 command/exit code가 확인된
경우에만 실행 가능하다고 보고한다. 유료 모델과 배포는 확정된 비용
범위의 승인 안에서 실행한다. 필수 client/model/BTP 미인수를 성공이나
비대상으로 바꾸지 않는다.
