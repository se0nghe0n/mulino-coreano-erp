# 공통 캡처 통합 뒤 실제 부분 인수

동결 source `5bcb46b56036d538fcf40ad10f8c1d7d5070148e`에서 Haiku
native dynamic Workflow `wf_8af75b88-62f`를 실행했다. T09 원본 전체
9개 subcase·94개 assertion과 T11 영향 범위 3개 subcase·13개 assertion의
합계는 12개·107개다. 결과는 assertion 100 PASS / 7 FAIL / 미관찰0,
subcase 7 PASS / 5 FAIL이다. 전체 profile 인수와 Task·Step은 OPEN이다.

setup 뒤 T09 3개 shard를 최대3개 병렬 실행하고 T11 1개 shard와 fanin을
수행했다. 실행마다 원래 assertion ID와 개수를 그대로 보존했다.

| shard | subcase 수 | assertion PASS / FAIL | verify exit |
|---|---:|---:|---:|
| T09-S01 | 3 | 37 / 2 | 1 |
| T09-S02 | 3 | 23 / 0 | 0 |
| T09-S03 | 3 | 31 / 1 | 1 |
| T11-S01 | 3 | 9 / 4 | 1 |

각 raw report의 `complete`는 ABSENT, `gateComplete`와
`prerequisiteRuntimeComplete`는 false다. T09-S02 exit0은 해당 부분
선택의 통과이며 전체 인수 영수증이 아니다. product harness의
`SCRIPTED_SIT`와 실제 Workflow 실행 모델을 구별한다.

coordinator는 6개 node metadata와 transcript의 assistant records77개를
직접 읽어 모두 `claude-haiku-5-5`임을 확인했다. API message ID 중복을
제거한 개수와 transcript record 개수는 별도로 취급한다. journal에는
launched1·started6·result6이 있으며 advisor·Monitor 호출은 없다.
네 tree는 각각 source3175개 hash가 그대로이고 실행 전후 clean이다.
JAR zip hash는 네 빌드마다 다르지만 제품 class324개의 이름·내용 hash
map은 전부 동일하다. 실제 실행 JAR와 기록된 hash도 일치한다.
생성된 네 DB container는 coordinator가 부재를 직접 확인했다.

남은 실패는 다음과 같다. 기대값과 원행을 수정하거나 숨기지 않았다.

- T09 cumulative의 직접 수령100은 평가되지만 observer의 contributions는
  별도 Work 간 귀속 행을 읽어 비어 있다. 합100과 두 occurrence ID의
  assertion 두 개가 실패했다. 구매 기여나 WorkLink를 임의 생성하지 않는다.
- T09 throughout-complete의 raw movements에는 fixture가 설치한 실제
  INITIAL_BALANCE 1개가 있다. 원래0 기대와 차이를 그대로 보존한다.
- T11 wait-timeout의 resume-denied는 REJECTED 기대와 HELD 응답이 다르다.
- T11 pending-correction은 실제 Work의 pendingInvalidation=true이지만
  assessment·input snapshot이 없어 현재 observer marker가0이다.
  correction은 HTTP500과 `Invalid immutable supersession`을 남겼다.
  delay/release runtime control도 NOT_IMPLEMENTED다. close는 별도
  UNRESOLVED_DUTY로 거부됐으므로 이 거부를 정정 성공의 증거로 쓰지 않는다.
  terminal의 closeReason null 처리도 독립 assertion 실패로 기록했다.
- T11 shared-distinct60의 실제 contribution 행에는 원래 oracle가 요구한
  workId·physicalScope 표시 계약이 연결되지 않았다.

원본 raw, wrapper, ID 대조, 모델·JAR·source·cleanup hash는
`.local/resume-20261023/native-capture-5bcb-audit/`의 `proof.json`,
`failures.json`, 재현 가능한 `audit.py`에 남긴다. controller 산출물은
`t3-65c7a301/.local/native-capture-cases-5bcb-20261023/`에 있다.
