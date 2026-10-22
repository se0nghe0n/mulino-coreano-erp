# T01 실제 실행 복구

## 범위와 기준

- 사용자 Step 2 T01 복구이며 전체 Task 완료가 아니다.
- branch: `fix/t01-actual-failures`, 시작 commit: `eb1678bc`.
- 원래 실행: `64d5ccbd`, actual run
  `01d3c3d1-d857-430d-b3c7-50a4cc7c955f`, T01 12 FAIL이다.
- C3/V4/V7, worker claim, ScenarioFixtureInstaller 및 다른 worker 소유
  파일은 변경하지 않는다. push/PR/외부 메시지/실모델/BTP는 실행하지 않는다.

## 변경 계약

`structureIntent`의 파싱 완료 입력에서 요청된 여섯 제외 업무명만
`error.requestedEffect`에 반환한다. 임의 문자열·객체·다른 capability는
반사하지 않는다. 실제 수행 효과를 추론하지 않으며 현재 권한을 주지 않는다.
`HELD`, `VERSION_UNSUPPORTED`, 빈 effects를 유지한다.

근거는 계획 §1의 제외 업무, §3.3의 요청/효과 구분과 §3.4의 구조화 오류,
기존 T01 excluded assertion이다. command-response schema의 error는 확장
필드를 허용한다. 전체 입력이나 식별자를 오류 응답에 싣지 않는다.

기존 no-substitute-command assertion은 존재하지 않는 raw `requestId`를
필터링했다. 실제 CommandRecords는 `commandIdempotencyKey`와
`canonicalIntentJson`을 저장한다. 같은 요청의 정확한 멱등키로 COMMITTED
0개를 확인하도록 필터를 고쳤다. 전후 원행 불변 assertion 여섯 개와 모든
기대값, 총 125 assertion을 유지한다. 관찰 누락은 계속 실패다.

책임 선언만으로 obligation row가 생기지 않아 두 WORK_NEXT_ACTION 의무를
기존 installer가 지원하는 명시적 Obligation alias로 작성했다.
이는 fixture에 이미 명시된 source 문서·Work·owner·다음 행동을 설치하는
입력이며 기대값으로 판정 결과를 설치하는 우회가 아니다.

## 검증

- Java 21, npm ci --prefix backend 후 focused backend tests 7개 PASS다.
  `UnsupportedEffectTest`, `CommandEnvelopeTest`를 실행했다.
- generator 재현성 3개 PASS와 T01 authoring invariant 2개 PASS다.
- actual은 아래 후속 실행 기록으로 판정한다. 정적 tests는 actual PASS가 아니다.

## 남은 통합 경계

원래 실행의 정상 여섯 subcase는 다음 원인이 겹친다.

1. Work에는 직접 lotId가 없고 fixture의 Work/LOT 관계는 installer가
   설치하지 않는다. WorkReadHandler.matches는 직접 lotId만 비교하므로
   getWork가 FORBIDDEN이다. 현재 ITEM grant 자체는 이미 정상이며 all-of를
   바꿀 이유가 없다. 정확한 subject/lot 관계를 설치하고 관계 기반 scope
   매칭을 구현해야 한다.
2. baseline eligibilityBases/restrictions/relations는 현재 installer의
   CONSUMED_BASELINE이 아니다. 단순 document hash/time은 canonical
   occurrence, disposition/규제 허용, 수령 사실을 만들지 않는다.
   원천 evidence chain과 receipt/quality/regulatory row를 설치하는 독립
   helper 및 installer 소유자의 연결이 필요하다. 보유100을 누적100으로
   복사하거나 적격60을 직접 넣어서는 안 된다.
3. getAssessment의 현재 Work 조회는 supply-basis가 요구하는 실제 행동별
   판정/조건/근거 계약이 아니다. QualityEligibility의 실제 결과에 연결한
   조회 계약이 필요하며 assessment owner와 통합해야 한다.
4. 정상 질문의 customerId/action/filter 및 scope는 명사 snapshot과 동일한
   의미로 정규화해야 한다. traceLot는 현재 action/customer filter를 거부한다.
5. snapshot은 현재 응답의 evidence 집합에도 영향받는다. 행동별 질문을
   같은 world snapshot으로 묶되 새로운 권한이나 무관한 사실을 노출하지
   않도록 실제 원행 기반 회귀 검증이 필요하다.

ScenarioFixtureInstaller를 직접 변경하지 않았다. 원천 정책·수량·의무를
oracle값으로 설치하거나 누락 관찰을0으로 만드는 변경을 하지 않는다.

## 2026-10-22 clean source 실행 결과

실행 command:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home/bin:$PATH \
./verify scenarios --actual verification/cases/T01/case.json
```

- 실행 source commit: `deb0e15ea989dcd65643420d98d40edb7f1c51b2`.
- `workingTreeDirty=false`, selected/started/completed 모두 12다.
- 실제 실행 action 60개, assertion 70 PASS / 55 FAIL이다.
- 여섯 excluded subcase는 runtimeComplete=true, PASS다.
- 정상 여섯 subcase는 FAIL, 전체 profile exit1이다. T01 완료가 아니다.
- evidence 경로:
  `verification/harness/target/evidence/actual-scenarios-run-9121be50-8178-444e-9374-07ae52623738/scenarios.json`.
- fixture SHA256:
  `b9bf0239af65ae40f972cc4a4a6ba9de9ec7960e4efd8a9886b539f5ace5961c`.
- case SHA256:
  `6847cc327cf745cc2bb7a5b58c6655b1add831ac88365b543bd49f5b1ddb7499`.
- 실행 jar SHA256:
  `b9ba191ccf808dca93796dda9612f2c696b63bb9b94b6fc90855f2c3dc79b56f`.
- focused backend tests 7 PASS, generator 재현성 3 PASS,
  T01 authoring invariant 2 PASS다. `./verify validate`는 exit0이다. 로그는 같은 evidence 디렉터리에 있다.
- 격리 PostgreSQL container와 임시 서버는 runner가 정리했다.

| subcase | 실제 남은 첫 원인 | 소유 경계 |
|---|---|---|
| same-world | getWork FORBIDDEN, 직접 lotId 매칭 | PRODUCT + fixture relation |
| inventory | customerId scope가 달라 SNAPSHOT_CHANGED | TEST query scope |
| supply-basis | Work lot 필터 FORBIDDEN, 이후 조건 projection 부재 | PRODUCT + ADAPTER |
| waiting | Work lot 필터 FORBIDDEN, wait 필드 명칭도 원천과 다름 | PRODUCT + ADAPTER |
| responsibility | Work lot 필터 FORBIDDEN | PRODUCT |
| customer-impact | traceLot action/customer filter TYPE_INVALID, 관계 원행 없음 | TEST + ADAPTER |

의무 alias 수정으로 모든 정상 subcase의 DB human-duty tuple 검증은
통과했다. same-world의 responsibility assertion도 통과했다.
반면 적격60과 누적100은 아직 원천 사실이 없어 실제0이다. 이0을 정상
oracle로 바꾸지 않았다.

coordinator가 연결해야 할 구체 경계는 다음과 같다.

- `ScenarioFixtureInstaller.run()`의 동일 JDBC transaction에서 master/Work/
  evidence 설치 뒤 독립 원천 사실 helper를 호출한다. 현재 `works()`는
  COMMAND snapshot까지 저장하므로 Work의 LOT scope를 추가할 때에는
  WorkTransitions snapshot도 같은 source로 만들어야 한다.
- helper는 baseline의 원천 event kind, claim, canonical scope, receipt
  identity, policy와 허용 범위를 명시적으로 받아야 한다. 현재 hash/time만
  있는 evidence로 VERIFIED를 추론하지 않는다. 필요한 원천 입력을 T01
  generator에 추가하고 그 fixture hash로 검증한다.
- `WorkReadHandler.matches/workScopes/world`는 실제 SubjectLinks 등으로
  확인한 LOT 관계를 사용한다. 단순히 lot 필터를 무시하면 다른 LOT 정보가
  새어 나가므로 금지한다. ITEM grant와 현재 auth는 유지한다.
- getAssessment 공급 가능 근거 조회는 QualityEligibility가 계산한 현재
  SELL 결과와 근거에 연결한다. 기존 Assessment owner와 contract를 정한
  뒤 합친다. `conditions`와 `conditionResults`를 이름만 바꾸지 않는다.
- T01 query generator는 필요 없는 selector를 지우되 assertion은 유지한다.
  같은 snapshot의 scope를 전달하고, getWork wait의 원천 JSON과 공개
  verifierId/evidenceId 응답을 일치시킨다. 원천 원행이 설치된 다음에
  snapshot·권한 회귀와 함께 수정해야 한다.

위 연결은 이번 commit에 구현하지 않았다. ScenarioFixtureInstaller와
Assessment 담당자의 병렬 소유 범위를 침범하지 않기 위해 남긴 통합점이다.

현재 live catalog에서 codex/gpt-6.1-sol의 ultrafast 지원을 확인했다.
현재 turn의 실제 tier를 증명하는 telemetry는 제공되지 않아 실행 tier를
별도 검증했다고 주장하지 않는다. 재위임은 하지 않았다.
