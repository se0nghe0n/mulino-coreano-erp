# 원본 case를 보존하는 분할 실행

T17 전체 실행이 foreground600초 제한에 종료돼257 assertions의 결과를
회수하지 못했다. 원본 case와 fixture를 편집하지 않고 선언된 subcase를
선택하는 `--subcase CASE/subcase`를 scenarios profile에 추가했다.
Task와 현재 Step은 OPEN이다.

```sh
./verify scenarios --actual \
  --subcase T17/normal-consumed-delivery \
  verification/cases/T17/case.json
```

`--subcase=CASE/subcase`도 지원하며 여러 번 지정할 수 있다. 실행 순서는
원래 case의 선언 순서다. 존재하지 않거나 중복·잘못된 selector는 실행
전에 거부한다. prepare나 다른 profile에는 이 option을 적용하지 않는다.

CaseRunner는 원본 case 전체를 읽고 원래 caseHash/fixtureHash와 assertion을
그대로 기록한다. 보고서의 discovered는 전체 scenarios의 선언 수이고
selected/started/completed는 실제 선택한 수다. 명시적으로 모든 subcase를
지정해도 partial run이며 gateComplete=false와 coverage receipt 거부를
유지한다. 여러 shard 결과를 합쳐 전체 profile PASS를 만들지 않는다.
원래 ID·hash·기대와 관찰의 대조로 case 단위 실행 범위를 별도 평가한다.

집중 검사는 새 selection8개와 기존 receipt/coverage 회귀8개, 총16개가
실패·오류·skip0으로 통과했다. 여기서 driver는 unimplemented이므로
제품 runtime 인수는 NOT_RUN이다. 선택한 한 건·복수·전체·무선택,
원본 discovery·hash 보존과 잘못된 selector 거부를 확인했다.
로그와 JUnit은 `.local/resume-20261023/subcase-selection/`에 보존한다.
새 실제 T17 실행은 Haiku native Workflow가 수행한다.

고정 `27ec8cc3`의 전체 harness는732 PASS·80 opt-in SKIP이며 실패와
오류는0이다. 실제 observer PG101개를 포함하고 소스3,163개의 hash
drift0과 자체 PG 제거를 확인했다. prepare는41 case·802 subcase·
24,260 assertion·문제0·asset12 PASS다. 결합 증거는
`.local/resume-20261023/subcase-selection-integrated/proof.json`이다.

같은 commit의 전용 tree5개에서 locked npm ci와 사전 검사를 통과했다.
Workflow `wf_62923727-846`은 Haiku5.5 medium의 최대 동시3개 node로
원래 T17의15 subcase·257 assertions를 실행한다. 시작 turn의 종료는
Workflow 완료가 아니며 실제 결과·모델 metadata·정리 증거는 회수 전이다.
이전600초 종료의 부분 로그와 NOT_RUN 기록도 보존한다.
