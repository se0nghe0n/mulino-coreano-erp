# 2026-10-23 인도·공급자 Haiku 병렬 실행

고정 source 21850e42의 세 case를 native dynamic Workflow로 실행했다.
44개 subcase는 22 PASS·19 FAIL·3 NOT_RUN이다. 전체 Task와 현재 Step은
OPEN이며 이 선택 실행을 전체 완료율이나 실모델 인수로 바꾸지 않는다.

## 실행과 독립 대조

source는 `21850e42ba6a2d8e6efe7785e2605d7787f5b480`이다.
Workflow는 `wf_27f1aeee-4a4`, native task는 `wxz8lfg5k`다.
setup 한 개, 병렬 case 세 개, fan-in 한 개를 실행했다. 실제 metadata
다섯 개 모두 `claude-haiku-5-5`와 foreground를 기록한다. medium effort는
script의 요청값이며 metadata에서 별도로 확인한 값은 아니다.

첫 owner launch의 ECONNREFUSED는 작업 시작 전 실패다. 원본 T3 오류를
보존하고 같은 provider/model로 한 번 재시도했다. 실제 제품 실패의
재실행은 없었다. case마다 별도 checkout·npm·JAR·DB·키·포트를 사용했다.
shell 병렬 실행을 native Workflow라고 표시하지 않았다.

coordinator가 세 tree 각각의 tracked source 3,141개 hash, clean 상태,
fixture와 case hash, 실행 원본·복사 report, 실제 harness stdout과
wrapper prelude, 명령·exit·cleanup을 대조했다. JAR의 raw hash는 서로
다르지만 모든 ZIP entry 이름과 내용 bytes는 같다. 다섯 agent metadata와
journal도 독립 대조했다. 정확한 proof와 원본 경로는
`.local/resume-20261023/delivery-contribution-wave-coordinator-proof.json`,
검사기는 같은 디렉터리의 `verify-delivery-contribution-wave.py`에 있다.

## 결과

| case | subcase PASS / FAIL / NOT_RUN | assertion PASS / FAIL / 미관찰 |
|---|---|---|
| T01 | 12 / 0 / 0 | 125 / 0 / 0 |
| T13 | 10 / 4 / 3 | 102 / 6 / 27 |
| T17 | 0 / 15 / 0 | 74 / 183 / 0 |
| 합계 | 22 / 19 / 3 | 301 / 189 / 27 |

예상 assertion은 517개다. 27개는 원본 결과에 ID별 관측이 없는 미관찰이며
명시적 assertion NOT_RUN으로 바꾸지 않는다. 공급자 세 subcase는 각각
assertion 여섯 개가 PASS여도 runtime task 한 개가 남아 NOT_RUN이다.
22개 PASS subcase는 각각 runtimeComplete=true, missingAdapters=[],
전체 기대 assertion PASS를 확인했다. case·subcase·assertion 중복은 없다.

세 report 모두 최상위 runtimeComplete·reportComplete·missingAdapters는
ABSENT다. `gateComplete=false`, prerequisiteRuntimeComplete=false이며
partialFixtures=true, relaxWire=false다. 실제 stdout의 coverage receipt도
schema/DB 실제 version 입력 누락으로 NOT_EMITTED다. SCRIPTED_SIT 실행은
제품 실모델 client 인수가 아니며 해당 호출은 0회다.

## 실패와 다음 소유권

T13은 conditional approval의 HELD/APPROVAL_CONDITION_UNSUPPORTED,
createPolicyDraft의 미지원 requires slot, 취소 scope와 부분 선적·수령의
입력/observer 연결이 남았다. 외부 worker task가 남은 공급자 세 건을
정상 완료로 세지 않는다.

T17은 item 범위 eligibility의 segment ID 요구, 공개 인도·배분 관계
projection과 실제 원행을 연결하지 못한 selector가 주요 경계다.
evidence Claims 전체와 인도 Observations, 현재 assignment와 OPEN 상태,
원래 생성 command와 후속 변경 transaction을 같은 것으로 취급하지 않는다.
실제 assessment·회수 조사 원천이 없는 항목도 alias로 채우지 않는다.

품목 전체 held100과 창고 held40의 차이는 장소를 지정하지 않은 요청의
범위 차이다. 전역 집계 의미를 창고 전용으로 바꾸지 않는다. 원래 업무
oracle을 보존하고 요청의 명시적 Work/장소와 공개 조회·관찰 계약을
연결한다. 이후의 제품 수정은 이 source의 결과에 소급 적용하지 않는다.
