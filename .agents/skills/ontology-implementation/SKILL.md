---
name: ontology-implementation
description: Mulino의 새 완제품 수입·B2B 유통 온톨로지를 구현하거나 변경할 때 D01–D26를 서버 계약, 모듈 경계, C/V/E 인수 증거로 연결한다. 개발용 skill이며 운영 업무를 대신 실행하는 role skill이 아니다.
---

# 온톨로지 구현

## 기준선과 읽기 경로

이 skill은 새 구현을 위한 개발 절차다. 현재 Task의 사용자 지시와
[`구현 계획`](../../../docs/ontology-implementation-plan.md)을 먼저 읽는다.
설계 의미는 [`설계 철학`](../../../docs/ontology-design-philosophy.md),
채택·제안 경계는 [`논의 기록`](../../../docs/ontology-discussion.md)의
§10·§12를 따른다. 계획의 경로·명령은 납품 계약이며 존재나 성공의 증거가 아니다.

기존 구현을 복사하거나 조사해 새 설계의 근거로 삼지 않는다. 현재 문서와
공식 기술 자료에서 계약을 만든다. 보존을 위한 Git ref/hash inventory와 실제
authoritative 자료 유무 확인은 코드 재사용과 구별한다.

- 착수·stack 선택·Step 종료: [stage-gates.md](references/stage-gates.md)를 읽는다.
- schema·evaluator·command·복구 변경:
  [implementation-contracts.md](references/implementation-contracts.md)를 읽는다.
- 노출 면(projection/action/tool)이나 조회 query를 바꿀 때:
  implementation-contracts.md의 "쓰기 노출 면과 조회 계약"을 읽는다.
- 변경 범위·oracle·인수 증거:
  [case-routing.md](references/case-routing.md)에서 관련 D/T와 C/V/E를 찾고,
  계획 §13의 원문 fixture를 읽는다. 요약표만으로 fixture를 축소하지 않는다.

## 변경 하나를 구현하는 순서

1. 현재 fork/Task branch의 baseline, 추적 issue, 현재 사용자 단계와 계획 S gate,
   맡은 모듈/파일을 기록한다. 서로 다른 두 단계 번호를 혼용하지 않는다.
2. 요구 D ID와 도메인 명령을 고르고 입력·현재 인가·revision·승인 hash·상태
   전이·수량 효과·증거·잔여 의무·외부 효과·오류 결과를 한 계약으로 적는다.
   새 업무 의미가 필요하면 채택된 규칙과의 차이를 명시한다.
3. 관련 oracle를 seed/입력/실행/관찰 가능한 assertion으로 연결한다. 계획상
   경로와 아직 없는 adapter는 `NOT_RUN`으로 남긴다. 임의 mock의 성공을
   DB 잠금·MCP·모델·배포 인수로 전환하지 않는다.
4. S0의 검증된 platform decision이 있으면 그 persistence 경로로 구현한다.
   없으면 허용된 현재 범위에서 계약/fixture를 준비하고 S0 spike를 수행한다.
   CAP 후보를 이미 채택된 stack으로 가정해 도메인 구현을 진행하지 않는다.
5. repository는 저장·잠금, application service는 거래·인가·멱등성 조정,
   adapter는 presentation을 맡는다. inventory primitive 밖의 원장 쓰기,
   adapter별 권한/evaluator 복제, 범용 core CRUD를 만들지 않는다.
6. 같은 DB transaction에 도메인 효과·배분·의무·감사·outbox·멱등 결과를
   통합한다. blob와 외부 효과는 별도 대조한다. 관련 성공·실패·경합·재시작
   oracle를 실행하고 아래 증거를 남긴다.
7. 모든 맡은 산출물을 Task branch에 통합한 뒤 합친 checks와 지정 review를
   수행한다. worker commit이나 한 fixture의 성공만으로 Step를 닫지 않는다.

## 항상 보존할 판단 경계

- 명사/동사 조회는 같은 ID·snapshot·범위의 목표·증거·책임을 읽는다.
- 구조적으로 유효한 의도, 증거로 확인한 사실, 현재 실행 권한은 별도다.
  `QUERY`, `RECORD`, `COMMAND`를 서로 자동 승격하지 않는다.
- 보유·누적 도착·행동별 적격·예약·미예약 적격은 서로 다른 양이다.
  문서 수, 환산, 반품, 회수 후 폐기를 새 이행량으로 더하지 않는다.
- 업무 종료·시도 성공·목표 충족·의무 해소를 독립으로 저장한다. 유효한 잔여
  의무에는 인간 owner·nextAction·nextCheck가 남는다.
- 발행된 정의와 과거 판정 의미는 불변이다. 현재 정책·grant·제한은 실행
  commit 경계에서 다시 검증한다. 미지원 버전은 효과0의 보류로 남긴다.
- 개발 fixture의 정책값은 공식 규제값이 아니다. 실제 정책/계정/승인자나
  비용 승인이 미정이면 해당 운영 범위를 막고 결정적 검증은 계속한다.

## 완료 보고와 증거

증거 기록은 `verification/manifest.json`(없는 파일)이 아니라 저장소의 실제
pipeline이다. 실행은 `./verify`·`verification/actual/sN/run.sh`, 연결·조립은
`verification/coverage`의 receipt·index·`assemble.py`·`validate.py`,
결과는
`verification/harness/target/evidence/runtime-manifest.json`이다.
증거 class(ACTUAL/SELFTEST/CONTRACT_RED/STUB/LOGIC_REVIEW)와 보고 규칙은
[저장소 harness](../ontology-scenario-testing/references/repository-harness.md)를
따른다. ACTUAL receipt가 검증되지 않은 PASS 주장은 `NOT_RUN`이다.
requirement/case→assertion→artifact, commit, version, fixture hash, 정확한
command, expected/observed와 `PASS|FAIL|NOT_RUN`을 그 산출물에서 인용한다.
논리 review·문서/skill 검증·로컬 결정적 테스트·실모델·규제 검토·BTP/client
결과를 분리한다.

handoff에는 Task/사용자 단계/S gate, baseline/worktree/ownership, commit과
변경 파일, 통합 상태, checks 결과, 미해결 R 결정, 다음 행동을 남긴다.
