# 2026-10-24 ordinary regression 실행 증거

## 판정

현재 통합 source `8bd6779702f00d4f6c7e42f16cb219055aa8660d`에서
`./verify harness`를 실행해 956 tests, 실패 0, 오류 0, skip 258,
exit 0을 확인했다. 실제 실행한 검사는 698개이며 `actualModelCalls=0`,
tracked source drift 0이다. 이 결과는 기본 cloud regression gate의
통과이며 전체 MODEL scenario, 애플리케이션 model, 사용자 Step, S0–S6
acceptance를 닫지 않는다. 현재 허용된 cloud ordinary engineering checks와
native GPT-6-Luna worker 범위는 구현·검증에 한정하며, worker 작업은
application-model scenario run이나 runtime acceptance receipt가 아니다.

Coordinator가 만든 canonical code commit은
`50b2d0131392088719f262913abd5dd62bdf7f0b`이며 parent는
`037a75485046c111436f35fcae01e81db4091815`다. Coordinator는 commit tree
`66ea9e61d2251302300e46afb3f75098ecfc7e5f`와 local tested source
`8bd6779702f00d4f6c7e42f16cb219055aa8660d`의 tree가 일치한다고 확인했다.
이 문서와 증거 묶음은 해당 canonical code commit의 후속 문서 변경이다.
branch 게시 결과는 실제 ref 확인과 구별해 기록한다.

이 문서는 `fix/cloud-harness-regression`의 위 source commit과 같은
baseline을 기준으로 별도 `docs/ordinary-regression-20261024`
worktree에서 작성했다. 증거 원문은 실행 worktree의 무시 파일 경로인
`.local/continue-20261024/`에 보존하며 대형 원문을 이 문서에 복제하지
않는다. 실행별 summary, log, report의 SHA-256을 아래에 기록한다.

## 게시된 증거 묶음

[검증 원문 묶음](evidence/ordinary-regression-20261024.tar.gz)에 18개
member를 보존한다. 기계 생성 summary, 실제 SupplierReply proof와
permitted-context report, observer JUnit XML, prepare 출력, inventory
요약 및 protected hash 검사를 포함한다. `index.json`은 각 원문 SHA-256과
크기만 묶으며 acceptance receipt나 새로운 PASS 판정을 만들지 않는다.
archive 크기는 115,282 bytes이며 SHA-256은
`a6e84b50432be1e90a36f128e2d9cd17533bc915d9df8c0c77e11515861f2dca`다.
큰 실행 log 전체는 묶음에 넣지 않고 원래 log hash를 summary에 남긴다.

## Harness 회귀와 수정 이력

초기 전체 Maven harness run은 source `037a75485046c111436f35fcae01e81db4091815`
에서 972 tests, 실패 2, 오류 71, skip 219, exit 1로 끝났다. tracked
source drift는 0이었고 application model call은 0이었다. 원인은
UTF-8 subprocess 출력 실패 2개, `CAPTURED_SELFTEST` allowlist pin의
오류 4개와 opt-in 조건을 상속하지 못한 PostgreSQL 하위 suite 오류
67개였다. 이는 실패 결과로 보존하며 최종 통과 수에 합산하지 않는다.

회귀를 다음과 같이 고쳤다.

- `44afc367904cb2ec9afe6ef88478c8d45bc7d635`에서 subprocess JSON 출력을
  UTF-8로 고정했다. 기존 빈 environment도 보존했다.
- allowlist pin은 worker commit
  `988e53204f0570add3a3524a05fd0fd2ce55003a`를 바탕으로
  `100efbf2857669e3a79e30024cf2811b3d93efcc`에서 통합했다. `CAPTURED_SELFTEST`만 pin을 갱신했으며,
  검증 결과는 120에서 137로 17개 QUERY entry가 추가되고 기존 QUERY는
  바뀌지 않았으며 runtime validator도 변하지 않았다.
- `bffd51fe6ef3c8cd692a4c6996116f03d4a16f00`의 observer PostgreSQL
  opt-in 및 disposable database guard를 `8bd6779702f00d4f6c7e42f16cb219055aa8660d`에 통합했다.
  기본 harness에서는 실제 DB opt-in 없이 해당 PostgreSQL suites를
  skip한다.
- SupplierReply 단독 검사는 worker commit `4781e2ea3aef36a651fc8ad33717c4c93b84e1e7`에서 시작했다.
  통합 source에는 test `56791fb1fa99df85a0cb333609a0b4b9376275a0`, typed JSON 보존 `7fe6f64f620684dfa46ec5ca86e06874796ac74d`,
  PostgreSQL 원래 열 이름 readback `4dc67b1ea26a5e5e13a1294b179f58a13924f831`, 동일 instant 시각 검증
  `5315db525f48346a16f30b456f77b268fbd3face`가 포함된다. actual PG 검사는 수정 완료 source
  `5315db525f48346a16f30b456f77b268fbd3face`로 실행했다.

이후 `8bd6779702f00d4f6c7e42f16cb219055aa8660d`에서 `./verify harness`를 다시 실행해 956 tests,
실패 0, 오류 0, skip 258, exit 0을 확인했다. 초기 실행보다 집계
테스트 수가 16개 적은 것은 기본 parameterized DB discovery 결과가
달라진 데 따른 차이다. 하위 DB suite 67개가 기본 실행에서는 38개
skip으로 집계되고 새 검사 13개가 추가되어 `972 - 29 + 13 = 956`이
된다. source snapshot은 정확히 고정했고
tracked source drift는 0이다. 이를 테스트 삭제나 실패 은폐의 증거로
해석하지 않는다. skip 258은 PASS로 세지 않는다. 이 full harness에서
`ModelFixtureOwnerClaimTest` 4개도 모두 통과했다. 이 보존 회귀는 M47의
중복 owner snapshot을 명시된 source claim에만 연결하고 새 product row를
만들지 않으며 원 fixture와 installation input을 바꾸지 않음을 확인한다.
이는 fail-closed inventory 검사이며 M47의 설치 성공을 뜻하지 않는다.

## 집중 검증 결과

### Backend unit 검사

기존 Mockito 5.23.0 test dependency를 사용해 49개 class의 274 tests를
명시적 Java 21 startup `-javaagent` 방식으로 실행했다. 274개 모두 통과,
실패·오류·skip 0, exit 0이다. 첫 실행은 Mockito self-attachment로
실패했으며 그 원문을 보존했다. 재실행에서는 test JVM에만 명시적 agent를
지정했고 host security 설정을 바꾸거나 source를 수정하지 않았다.
대상 product snapshot은 `dda42dd63e550741bd370a136760f39b062d1f20`이며
검사한 product bytes는 `037a75485046c111436f35fcae01e81db4091815`와
동일하다. DB 접근 및 model call은 없었다.

### Prepare

`./verify prepare`는 `8bd6779702f00d4f6c7e42f16cb219055aa8660d`에서 exit 0, `PREPARED`를 반환했다.
41 cases, 802 subcases, 24,437 assertions를 읽었다. 그러나
`gateComplete=false`, `runtimeStatus=NOT_RUN`, artifact coverage
`NOT_RUN`, semantic oracle equivalence `REQUIRES_CASE_REVIEW`다.
Capability-slot 검사는 2,165 commands를 확인해 3개 의도적 예외와
407개 violation finding을 보고했다. 따라서 prepare 성공을 product
runtime 또는 scenario acceptance로 해석하지 않는다.

### Standalone SupplierReply PostgreSQL 검사

`ModelFixtureSupplierReplyInstallerTest#actualPostgresReadsBackSupplierReplyEvidenceAndExactInverseAndRejectsMissingOccurrenceAtomically`
하나를 fresh, owner-owned PostgreSQL 18.6에서 실행했다. 실제 결과는
1 test 통과, 실패·오류·skip 0, Maven exit 0이다. source commit은
`5315db525f48346a16f30b456f77b268fbd3face`, 합성 fixture SHA는
`fb65fcea5491db177770c3ed199a111c0c3267dd95bfcf90acd63ac8a4ea821e`다.
45 migrations 적용과 51 source receipt 및 정확한 inverse readback을
확인했다. 누락 occurrence는 SQLSTATE `23503`으로 거부됐고 rollback 뒤
organization 0행, SupplierReply 0행, blob file 2개 전후 동일함을 확인했다.
DB는 SCRAM-authenticated loopback TCP에 한정했으며 실행 뒤 server stop,
port closed, runtime directory 제거 및 source drift 0을 확인했다.

이 결과는 단독 합성 설치·readback·rollback 검사다. proof의
`fixtureComplete=true`는 이 합성 fixture installation의 상태만 뜻하며
M01–M60 어느 case의 완료나 업무 실행을 뜻하지 않는다. `acceptanceStatus`
는 `NOT_RUN`이고 original input, business execution, authority 및 operating
approval을 주장하지 않는다.

### Permitted-context HTTP/PostgreSQL 검사

`NativePermittedContextHttpPgTest#directCurrentProductContextAndAuthorityCounterexamples`
를 harness source `8e7750c183f57b2c20249c258d1102ccd1a178ba`로 실행했다.
실제 Boot JAR은 앞선 `dda42dd63e550741bd370a136760f39b062d1f20`
snapshot에서 build한 동일 product source이며 SHA-256은
`699bb4b7c6b7ef4a856b13585446820cdcacffa4fbd1aa31300c268301270543`다.
fresh PostgreSQL 18.6과 결합했다. 실제 authenticated server capture를
사용한 direct local verification loopback 검사 1개와 18 checks가 모두
통과했고 Maven exit 0이다. `--mulino.verification.mcp-capture-enabled=true`
는 이 local verification run에만 명시했다. model call 0, scenario run 0,
`fullModelRunStatus=NOT_RUN`이다.

첫 attempt `de1b44550a9fe672`는 capture flag 누락으로 환경 실패했다.
이 원문을 보존한 뒤 명시 flag를 주어 `f43803703e64b87a`로 재실행했다.
성공 run 뒤 backend와 PostgreSQL을 멈추고 HTTP/DB port를 닫았으며
runtime directory를 제거했다. 이 검사는 실제 업무 model turn이나 전체
client acceptance가 아니다.

### Observer 실제 PostgreSQL 검사

`observer/attempt-f2ec5434216d3851/`의 PostgreSQL 18.6 JUnit 결과는
ScenarioObservation 64 tests 중 63 pass·1 optional skip,
SharedCaptureObservation 67 tests 중 66 pass·1 optional skip이다.
총 131 tests 중 129 pass, 실패·오류 0, optional skip 2, Maven exit 0이다.
두 skip은 모두 환경상 `T09_GOAL_INPUT_SOURCE`가 필요한 source-row
replay assumption이다. runner는 strict no-skip 기준 때문에 `FAIL`을
기록했다. 이 raw runner 결과를 숨기거나 skip을 PASS로 재분류하지 않는다.

첫 시도는 generated CSN 부재로 환경 실패했다. 뒤이어 공식 cached CDS
compiler로 current CSN을 생성했다. 이전 build와 비교하면 service 주석
차이만 있고 schema 사실은 바뀌지 않았다. 현재 JUnit 분석은
`acceptanceStatus=NOT_RUN`이다. 실행 뒤 PostgreSQL server stop, port closed,
runtime directory 제거를 확인했다.

## MODEL 입력 경계와 열린 항목

현재 60개 MODEL 입력 inventory는 44개를 explicit source mapping이 있어
설치 가능한 것으로 분류하고 16개는 typed source graph 부족으로 unresolved로
남긴다. inventory의 원본 archive SHA는
`32d5c2df808ad89892dd7d7fe2e3c681e212c0f9176402be44b215602dda1519`,
inventory SHA는
`0f5991e4297f34c88675c3c97845829f03100c110d7fa789c1f0c57daf09e87d`다.
현재 재계산은 M47의 `/baseline/domainFacts/currentOwnerRef` 입력 누락
문제 하나를 제거했으며 새 issue를 추가하지 않았다. M58의 issue 배열
순서 차이는 set iteration ordering뿐이다. 설치 가능 수와 unresolved 수는
44와 16으로 동일하며 이를 product execution으로 주장하지 않는다.

M47의 equality-only owner claim은 source commit
`584f664fe9af1d2572809755df0c613deec15784`에서 실제 product row나 grant가
없음을 확인했다. claim equality만으로 설치 가능하게 만들지 않으며 해당
입력은 계속 설치 불가다. original DOC1/DOC2 bytes는 제한된 이력·archive
검색 뒤에도 확보되지 않았다. 44/16 inventory 경계는 unchanged다.
protected original artifact 218개 hash도 변경되지 않았다. full harness
`ModelFixtureOwnerClaimTest`의 JUnit XML SHA-256은
`78c7f659e4e427cbe2d623c868900de4c4ced154a96231cf63fe7c2389bc399f`다.

실제 전체 corpus scenario, application-model workflow, M01–M60
acceptance 및 사용자 Step acceptance는 이번 기록에서 실행하지 않았다.
어떤 focused PASS도 이 열린 범위를 대신하지 않는다.

## 명령·환경·증거 무결성

통합 regression 명령은 `./verify harness`, prepare 명령은
`./verify prepare`다. 실행 environment는 allowlisted PATH, LANG, LC_ALL,
TZ와 disposable HOME, 명시적 Java/Maven 설정을 사용했고 model 및 DB
opt-in은 제공하지 않았다. 실제 harness/prepare Java는 Debian
`21.0.12.1+1`, Maven Wrapper는 `3.9.16`이다. 별도 PG probe compile은
JDK `21.0.13+9`를 명시했다. 실제 disposable database는 PostgreSQL
`18.6`이다.

증거 파일은 test worktree의 `.local/continue-20261024/`에 있다.
기록한 summary와 주요 원문 SHA-256은 다음과 같다.

- 초기 full harness (`037a75485046c111436f35fcae01e81db4091815`)은
  972 tests, 실패 2, 오류 71, skip 219, exit 1이다.
  Summary: `.local/continue-20261024/harness-full/summary.json`
  SHA-256: `c36a4e442f146ce7f5985c980897d19188a5837563907a5b6a7db9f0360dd87a`
  Log SHA-256: `8392549cb99b26bea39b45d326c44597b0322a4d044df601e07e811d4fa8d995`

- 통합 harness (`8bd6779702f00d4f6c7e42f16cb219055aa8660d`)은
  956 tests, 실패 0, 오류 0, skip 258, exit 0이다.
  Summary SHA-256: `5aa5df81b61b9e53c787ad3e4befc61ce94a3a9d2e964a99e2b674dcded83e91`
  Log SHA-256: `5debbbd5eabc04f5ee390b8f01f90a6733d16de8f9d4c0b9967331838f4f9e22`
  Source snapshot tar SHA-256:
  `a835be4091954e5b46f2f14baf5a4dcaccf387226426e5085b0ebd262c32fca4`

- Backend unit 검사 274 tests, 실패·오류·skip 0, exit 0이다.
  Summary SHA-256: `34d44080e13faba49a414fed28d97c6a8c484a53c091d116e62c75e0dc43b4a2`
  Log SHA-256: `bdc1e98f8a9c626d94607fdefe7b3d643c366142751ffef1931b2b622ebcfd98`
  Startup agent SHA-256:
  `ae295bebd5d11fab97ab297815dc7617188b86003cbce3dfd5c0d5c3a6cc4a0c`
  Exact selectors와 arguments는 묶음의 `backend-explicit-agent-summary.json`에도 있다.

- Prepare는 `PREPARED`, 41 / 802 / 24,437, exit 0이다.
  Summary SHA-256: `6b7ca2a2191200e51abd7bc0c5f6c7f88e4b1cda8ffa37b981df869a62e5ec47`
  Log SHA-256: `0bc24cea3431b7f9d865e7ebc3cdfceba1d77104cf2eaeacfad977a79693faa5`
  Raw report `.local/continue-20261024/prepare/prepare.json` SHA-256:
  `b86e74f43c3e0ef9efb02450decfa61380a6098268940ee067062c6b4ccaebdc`

- SupplierReply PG는 1 test 통과, 실패·오류·skip 0, exit 0이다.
  Summary SHA-256: `c61a57cb109d763cc4cdceebc549b96f413b74c972398e2a09a1552f523e0402`
  Proof SHA-256:
  `6a3e63d345d38a3165c1b5d1a00e23c5549e3f031f3081dfb055ca1c522f280e`
  JUnit XML SHA-256:
  `26b3bc1571cb854c1aa8b5ede4a822b2ba230ee288e4e356d6efe14cc6f07539`

- Permitted-context PG는 1 test 및 18 checks 통과, exit 0이다.
  Direct-context report SHA-256:
  `a471c15f4b7ef429876f6a18c2250b1729f9864fde6f89f4d0d7107c0895bb79`
  Summary SHA-256: `47fcd9c588b2f0656f7f8a4a9d1aef1dcbace32e3588cb917b96e76247a2ed75`

- Observer PG JUnit은 131 tests, 실패·오류 0, optional skip 2다.
  Maven exit 0이나 strict runner 결과는 FAIL이다.
  JUnit analysis SHA-256:
  `0cd02814da60fd13cea8af178c6d8fdd615fcdb36b7b3b2f09146613bd1f11f8`
  Runner summary SHA-256:
  `3edf94d77e1ad502aef9d1db7191f2092188ccf6de96a5a587021d284c1b02c1`

- Current inventory는 60 / 44 installable / 16 unresolved다.
  Summary SHA-256: `80d0ab99e3ef3c64c4fe00ca99ef674cd3e522b19f345569dd565958b5ca3479`
  Inventory SHA-256: `0f5991e4297f34c88675c3c97845829f03100c110d7fa789c1f0c57daf09e87d`

- Protected-original check는 218 artifacts, drift 0이다.
  `.local/continue-20261024/protected-originals.json` SHA-256:
  `254d686e5884e8186c7761c45387626c4a9fe163140b88b8b4cfa9a311513427`

각 disposable PostgreSQL/backend run은 자체 실행 경계와 cleanup 기록을
보존한다. 모든 소유 DB/backend를 중지하고 사용 port를 닫았으며 임시
credential 및 runtime 경로를 제거했다. 비밀 값은 이 문서나 commit에
기록하지 않는다.
