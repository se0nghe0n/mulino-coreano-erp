# 2026-10-23 원천·관찰·구매 승인 source의 실제 병렬 실행

고정 source `bcac964ced0d5cf47a990afabba5eddefb66b2c8`의
T01/T09/T11/T12/T13/T17을 실제 제품에 실행했다. 고유 70개 중
26 PASS·33 FAIL·11 NOT_RUN이다. 전체 Task와 현재 Step은 OPEN이다.
이 선택 실행의 통과율을 전체 구현 완성도로 사용하지 않는다.

## 실행과 독립 확인

Claude Haiku 5.5의 native dynamic Workflow
`wf_f1ed9414-4a9`, `wf_52ea3bf0-5fa`를 사용했다.
실제 node metadata 9개가 모두 `claude-haiku-5-5`임을 확인했다.
T12/T17의 첫 실행은 agent가 background verify 종료 전에 반환하면서
중단됐다. 그 원본을 `attempt-0-interrupted`에 보존하고 각각 한 번의
foreground native Workflow 완료 결과만 집계했다. 제품 실패를 같은
source에서 반복해 PASS를 고른 결과가 아니다.

coordinator는 고유 case/subcase와 예상 assertion ID, source HEAD와
clean 상태, fixture hash, 원본·복사 report hash, 실제 JAR 기동과
정리 기록을 대조했다. 6개 fresh JAR의 전체 entry 이름·내용 hash는
같다. ZIP metadata 차이로 다른 JAR hash를 같은 hash라고 표시하지 않는다.
shell build prelude가 있는 wrapper stdout과 원본 harness stdout도
구별해 확인했다.

| case | PASS | FAIL | NOT_RUN | assertion PASS | FAIL | NOT_RUN | 미관찰 |
|---|---:|---:|---:|---:|---:|---:|---:|
| T01 | 12 | 0 | 0 | 125 | 0 | 0 | 0 |
| T09 | 3 | 2 | 4 | 25 | 0 | 22 | 47 |
| T11 | 1 | 7 | 4 | 15 | 3 | 21 | 71 |
| T12 | 0 | 2 | 3 | 11 | 1 | 37 | 16 |
| T13 | 10 | 7 | 0 | 99 | 9 | 0 | 27 |
| T17 | 0 | 15 | 0 | 58 | 144 | 0 | 55 |
| 합계 | 26 | 33 | 11 | 333 | 157 | 80 | 216 |

예상 assertion은 786개다. 미관찰 216개를 명시 NOT_RUN 80개나
PASS에 합치지 않는다. PASS 26개는 각 행의 `runtimeComplete=true`,
빈 `missingAdapters`와 모든 예상 assertion을 확인했다.
top-level `runtimeComplete`와 `reportComplete` key는 없다.
`partialFixtures=true`, `relaxWire=false`, `gateComplete=false`다.

## 남은 최초 실패

T13의 승인 개정 7건은 실제 소비 정책까지 통과했다. 나머지 공급자
전달·회신, 조건부 승인과 정책 입력, 취소·부분 운송은 실패다.
공급자 입력에는 공개되지 않은 `externalOperationId`가 전송 대조에
남아 있다. 성공 결과를 기대값에 복사해 해소하지 않는다.

T09/T11/T12의 일부 DB capture는 nonempty recovery source의 과거
`recordedAt` 이력을 증명하지 못해 NOT_RUN이다. 이를 빈 값으로 숨기면
다른 source까지 불완전한 snapshot으로 PASS가 되므로, 실제 제품의
불변 recovery 이력과 시간·scope를 보완한다.

T09 예약은 실제 `INSUFFICIENT_ELIGIBLE_QUANTITY`다. 뒤의 allocation ID
누락만 adapter 문제로 분류하지 않는다. T11의 자유 문자열 물량 범위는
`VERIFIED_RANGE_REQUIRED`이며, resume/correction slot과 worker control도
남아 있다. 미지원 evaluator의 실제 `HELD/VERSION_UNSUPPORTED`와
기존 `REJECTED` 기대의 protocol 차이를 별도로 다룬다.

T12의 잘못된 창고 수령은 실제 `TYPE_INVALID`이며, 판정은 기대
UNSATISFIED와 다른 UNVERIFIED다. 미발행 condition/version 이름으로
판정을 채우지 않는다. T17은 active Work가 없는 의무, 인도 원천의
현재 scope, item 범위 적격성 조회와 실제 원행 projection 공백이 남았다.
예약 중복·FEFO 사유·품목/단위 불일치의 올바른 거부도 다른 실패와
구별한다.

## 후속 작업과 경계

Sol Ultrafast의 분리 worktree에서 recovery 이력, T17 active Work와
인도 원천, T13 공급자 source 입력을 수정한다. 결과는 coordinator가
검토·통합하고 결합 검사를 통과한 뒤 새 고정 source에서 재실행한다.
worker 완료와 단위 검사 통과는 실제 전체 인수 완료가 아니다.

recovery 후속 실행 하나는 `Selected model is at capacity`로 시작하지
못했다. 실패를 보존하고 같은 `codex/gpt-6.1-sol/high/ultrafast`로
한 번 재시도했다. provider나 tier를 조용히 바꾸지 않았다.
coordinator의 live tier 제어 제한은 앞선 통합 기록과 같다.

T13 worker가 새로 만든 전달확인 필수 probe는 채택하지 않았다.
계획 §6과 설계 철학은 전달·공급자 약속·도착을 별개 기록으로 정한다.
검증된 공급자 회신을 저장하기 위해 전달확인까지 필수로 하는 제품
조건은 추가하지 않는다. 이 새 probe만 사실 분리 검사로 바로잡고,
기존 135개 업무 assertion과 현재 전송 권한 재검증은 유지한다.

독립 증거는
`.local/resume-20261023/source-observer-policy-wave-coordinator-proof.json`,
검사기는 같은 경로의 `verify-source-observer-policy-wave.py`다.
원본과 Workflow artifact는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-20d8c824/`
아래 `.local/source-observer-policy-wave-20261023/`에 보존했다.
실모델 client 호출은 0개이며 규제·BTP·배포 인수는 완료하지 않았다.
