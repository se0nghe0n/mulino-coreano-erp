# T17 source-facts HTTP/PG 검사의 provisional 계약을 맞춘다

2026-10-24 기록이다. baseline은
`a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`이다.
Task·사용자 Step·S5·S6는 OPEN으로 유지한다.

## 원인과 수정 범위

root r2 full harness에서 `T17SourceFactsPostgresTest`는
34개 중 9개가 실패했다. 모두 provisional observation의
`ACCEPTED_PENDING_EXTERNAL`을 helper가 `APPLIED`와 비교한
실패다. 첫 실패에서 중단됐으므로 이후 인도·원장 검증은 그 실행으로
확인되지 않았다.

계획 §6은 관측 claim 접수와 확정 인도를 구별한다.
`64054871`에서 도입한 제품 계약과
`DeliveryCommands`의 현재 구현도 같다. `recordDelivery`의
observation form과 `recordObservedMovement`는 원 보고를 보존하고
대조 책임을 열며, 응답의 물리량·목표 기여량은0이다.
검증된 canonical occurrence를 사용한 confirmation form은
`APPLIED`로 실제 인도를 반영한다.

허용된 테스트 파일 안의 helper만 이 구별을 적용했다. observation
action은 `claim` 또는 `delivery-provisional`이어야 하고 정확히
`ACCEPTED_PENDING_EXTERNAL`이어야 한다. 다른 성공 명령의
`APPLIED` 기대와 dispatch·hold 호출자의 권한별 기대는 유지했다.
여러 outcome을 허용하는 집합으로 실패를 숨기지 않았다.

접수 단계에서 다음을 실제 HTTP 응답과 PostgreSQL로 확인한다.

- 응답 revision1과 정확한 observation ID·physical0·contributed0이다.
- 물량 segment·movement·배분·Dispatch·CargoScope·DeliveryTransfer·
  Delivery·판매 실행 효과·Work 기여·판정·판정 입력 원행이 전후 같다.
- 저장된 observation은 revision1·PROVISIONAL이며 검증된
  rangeRootId는 null이다. 주장한 root와 원 요청 slot·event·Work를
  그대로 보존한다.
- 정확한 observation에 연결된 현재 열린 대조 의무가 하나다.
  source profile의 인간 intake owner·supervisor, 원 nextAction·
  nextCheckAt·보고 수량·단위와 생성 명령의 인과 관계를 확인한다.
- 해당 인증 주체의 RECORD 명령은 COMMITTED이며 저장 결과의
  provisional outcome·effects와 canonical intent의 slot이 같다.

기존 검사의 확정 인도30/기여30, 제한·만료 후 실제 인도20/기여0,
운송 잔여10, CONSUMED 유지, 허위·상충·미식별·초과 보고 효과0,
출고 없는 보고의 정상 인도 거부, 원천·조직·권한·정의 guard를
유지했다. 제품 코드나 원래 case의 기대값은 변경하지 않았다.

## 소유와 격리

- worktree는
  `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-0e15b8f9`다.
- branch는 `fix/t17-sourcefacts-harness`다.
- 소유 파일은 `T17SourceFactsPostgresTest.java`와 이 기록뿐이다.
- runtime metadata의 provider/model은 `codex_2`·
  `gpt-6.1-sol`이다. 요청 설정은 xhigh·priority다.
  catalog가 해당 effort와 Fast(priority)를 지원함을 확인했다.
- root checkout과 다른 worker의 코드·import를 변경하지 않았다.
  push·PR·Grok·외부 알림을 실행하지 않았다.
- scenario runner·실모델·Workflow는 실행하지 않았다.
  실제 scenario 실행은 Haiku 5.5 native Workflow 소유다.

## 실행과 증거

로컬 artifact root는 다음 절대 경로다.

```text
/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-0e15b8f9/.local/t17-provisional-sourcefacts-support
```

`run.py`는 이 checkout의 backend를 빌드하고 전용 PostgreSQL,
loopback Spring HTTP, 임시 RSA key와 blob 저장소를 소유한다.
비밀값은 환경변수로만 전달하고 기록하지 않는다.

```sh
npm ci --offline --no-audit --no-fund  # cwd: backend
./mvnw -B -ntp -o -f backend/pom.xml \
  -Dmaven.test.skip=true package
T17_PG_TEST=true ./mvnw -B -ntp -o \
  -f verification/harness/pom.xml \
  -Dtest=T17SourceFactsPostgresTest test
```

마지막 명령은 runner가 제공하는 disposable DB·JWT·base URL·
identity binding·suite isolation 설정과 함께 실행한다.
원문 명령과 exit는 `commands.json`에 남긴다.

실제 도구는 Homebrew Java21.0.12.1, Maven3.9.16,
Node24.16.0, npm11.13.0, Docker29.4.0이다.
PostgreSQL image는 versions manifest의 다음 digest로 고정한다.

```text
postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280
```

실제 실행 JAR의 SHA-256은 다음과 같다.

```text
96133f1cb503d1860a26d3eaf39513bba346a531d4ca72d5fb758d3716def2e2
```

- `initial-r2/`는 원래 root r2 로그·JUnit·HTTP/원장 artifact와
  source/cleanup 기록의 보존 사본이다. 원본을 수정하지 않았다.
- `initial-focused.log`와 `initial-focused/`는 수정 전
  `explicitDispatchNormalDelivery`의 실제 재현이다.
  opt-in method selection으로 1개 실행·1개 실패·33개 skip이며
  Maven exit1이다. 실패는 동일한 APPLIED/PENDING 비교다.
- `final-whole.log`와 `final-whole/`는 수정 후 전체 34개 실행이다.
  34개 실행·실패0·오류0·skip0이며 Maven exit0이다.
  JUnit 실행 시간은591.4초다.
- provisional 전후 원행은 각
  `*-provisional-state.json`에 남긴다. 실제 응답과 종료 원장은
  기존 HTTP artifact 및 `*-ledger.json`에 남긴다.
- `source-initial.json`·`source-final.json`은 모든 tracked
  backend·contracts·database·verification 파일의 SHA-256을
  기록한다. 변경 허용 파일은 이 테스트 하나다.
- `baseline-parity.json`은 root r2 source와 baseline 불일치0을
  확인한다. `source-final-drift.json`의 실행 중 drift도0이다.
- `fixture-hashes.json`은 T17 원래 파일별 hash를 보존한다.
  `provisional-boundaries.json`은 실제 접수9건의 전후 물리·배분·
  기여·판정 원행이 동일함을 확인한다.

검사한 테스트 source의 SHA-256은 다음과 같다. commit 전 실행의
baseline과 변경 bytes를 위 manifest로 구별하며, 이후 commit에도
같은 테스트 bytes를 보존한다.

```text
ed4e5a77c6b80550f677aa1186f05e9f03297d287d7282234b46b6c00e3589fb
```

T17의 기존 assertion434개(case301+read125+scope8), oracle,
fixture·grant·clock을 보존한다. 대표 불변 hash는 다음과 같다.

```text
verification/cases/T17/case.json
33d560aed386dd37555e74e9616be2fd3eba8f1d0f7619d2def5d6f008229969
verification/requirements/mandatory-oracles.json
1f056056e67c1b720e30336e7a0af05493e4ded61914009ae681f7244bac5338
ScenarioFixtureInstaller.java
eb60c0ecd505647672b0ae052243b73535716463855e4a19eed5827e0ff855d4
T17SourceFacts.java
26bfe959297272dbfe54b98b96fc5c034ec36ac97eb5bf36625f20cf7f1231dd
```

## 최종 결과와 남은 gate

직접 HTTP/PG JUnit 검사34개가 모두 통과했다. 원래 root r2의
9개 실패를 해결했고 이 suite에서 확인된 추가 제품 결함은 없다.
shared helper는 변경하지 않아 인접 suite는 재실행하지 않았다.
제품 코드·원래 case·fixture·oracle는 그대로다.

`cleanup.json`은 전용 container의 remove exit0·부재,
Spring 종료, private/public key와 blob root 부재를 확인한다.
다른 worker의 container·키·blob은 건드리지 않았다.

대표 artifact SHA-256은 다음과 같다. 모두 위 artifact root 아래다.

```text
final-whole/junit/TEST-org.mulino.verification.actual.T17SourceFactsPostgresTest.xml
254a82917a262edbce9d4ef80430e3d7b2d5ec50df904fdc84e685c92c045eb3
final-whole/proof.json
8cb36a63773cb67edeef8909ef7480aa402bf5037173ef263ee5a0888a7b592c
fixture-hashes.json
c5a753ad4acc1e03c653048ba43ddb2c3fdf0c9d425a415703ff57ccf4e94bdf
provisional-boundaries.json
f0689c67c0f5de55faa8dc0a129a5ac9609a655d88a92e27e325e8cd5074203f
cleanup.json
ff84c4219c451d5b47ebb469028d0e4cdb74a1c5229c5b852f99d19526c4c50e
```

이 결과는 한정된 직접 검사 증거다. coverage pipeline의 ACTUAL
receipt나 runtime-manifest PASS를 생성·주장하지 않는다.

전체 harness 재결합·Haiku 실제 scenario·필수 D/C/V/E coverage,
실모델·규제·운영 identity·BTP 인수는 이 작업의 실행 범위 밖이다.
이를 면제하거나 완료로 바꾸지 않는다. Task·사용자 Step·S5·S6는
OPEN이다. coordinator가 이 commit을 통합한 source에서 full harness와
소유 Workflow의 실제 scenario 인수를 이어간다.
