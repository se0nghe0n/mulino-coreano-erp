# 결합 causal source witness 회귀 수정

관측 한정 grant를 남긴 테스트 callback이 원 명령의 commit 인가를
스스로 깨뜨렸다. 테스트의 진단 scope만 원래 행으로 복원하고 서버의
현재 all-of 인가를 보존한다. Task·사용자 Step·S5/S6는 OPEN이다.

## 기준선과 소유권

baseline은 `d0885a8031eb754e42f35f998a6d7f970b059cee`다.
branch는 `fix/causal-witness-scope`이고 별도 worktree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-37156c4f`다.
현재 T3 child의 실제 `t3_thread_configuration` 조회 결과는
`codex_2 / gpt-6.1-sol / reasoningEffort=xhigh / serviceTier=priority`다.
그 원행은 `.local/causal-witness-combined/runtime-selection.json`에
보존한다. 로컬 Codex turn context에서도 `gpt-6.1-sol / xhigh`를
확인했다. catalog는 priority를 Fast 값으로 공개한다. catalog의
기본값은 실행 선택의 증거로 사용하지 않는다.

수정 파일은 `PublicWorldReadPostgresTest.java`와 이 기록뿐이다.
`DeliveryRecordCause.java`·`DeliveryAdmission.java`는 baseline 그대로다.
T17 case·T17SourceFacts·observer·공유 추적 문서를 변경하지 않는다.
다른 worktree의 source/commit/JAR를 가져오지 않았다. main·push·Grok·
추가 위임은 수행하지 않는다. coordinator가 통합 후 결합 checks를 맡는다.

## 실패와 scope 체인

root의 최초 전체 backend log는 다음 읽기 전용 경로다.

`/Users/se0nghe0n/Developer/mulino-coreano-erp/.local/`
`resume-20261024/fix-until-done/backend-full/backend.log`

그 원본을 변경하지 않고 `.local/causal-witness-combined/`의
`root-backend-initial.log`로 snapshot했다. 당시
PublicWorldReadPostgresTest는14개 중13개 성공·1개 실패였다.
normal/restricted의 실제 RECORD→인도→불변 전체 evaluator 두 경로는
성공했다. witness 테스트는 과거 quantity validator 차단을 지나
callback과 최종 commit 인가에 도달한 뒤 FORBIDDEN으로 실패했다.

실패 원인은 다음의 구체 scope 차이다.

1. `DeliveryCommands.prepare`는 원 observation 외에 실제 Dispatch·
   sales line·transit segment를 TARGET으로 저장하고 목적지와
   transit의 PLACE, Work·item·event 원천 SOURCE도 저장한다.
2. 원 fixture grant는 ORGANIZATION 범위다. callback은 그 grant에
   observation만 허용하는 TARGET과 WORK·ITEM·PLACE·SOURCE 진단
   차원을 추가한다. grant의 차원은 all-of이므로 ORGANIZATION 행이
   있어도 새 TARGET 제한이 적용된다.
3. `DeliveryRecordCause`의 한정 observation witness는 이 진단 scope로
   정상 통과한다. 각 차원의 잘못된 ID와 capability 철회는 거부된다.
4. `ApplicationCommands.apply`의 최종 `PolicyCommandGuard.verifyCommit`
   →`verify`→`everyScope`는 저장된 각 TARGET의 조합을 다시 검사한다.
   observation만 남으면 Dispatch TARGET 등은 거부된다. 기존 서버의
   fail-closed 동작이 맞으며 제품 변경은 필요하지 않다.

새 assertion은 같은 IN_PROGRESS 명령의 authorizationScopeJson을 읽어
Dispatch TARGET이 실제 저장됐음을 확인한다. 그 Dispatch scope가
원래 grant에서는 허용되고 진단 중에는 거부됨을 단언한다. callback의
`finally`는 organization/grant로 한정해 진단 행을 지우고 snapshot한
모든 원래 열·값을 그대로 복원한다. 복원 직후와 명령 commit 뒤 전체
행 equality를 검사하며 Dispatch 권한과 COMMITTED 상태를 확인한다.
최종 기대 APPLIED·실제 고객30 EA는 유지한다.

getEvidence 철회 뒤의 양성 witness, SOURCE/TARGET/WORK/ITEM/PLACE의
모든 음성, recordDelivery capability 철회, transaction 밖 witness
거부, 원 actor·Work·수량·source 검사는 그대로다. 별도 assessGoal
권한을 부여하지 않고 실제 evaluator 두 경로의 assertion도 유지한다.
witness 테스트의 spy는 원래와 같이 assessment port만 분리하며 실제
command·canonical graph·실물 효과·책임 remedy는 제품 경로를 쓴다.

## 실행과 증거

Docker context inspect가 공개한 endpoint는 다음과 같다.

```text
DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
npm_config_offline=true
```

Java21.0.12.1, Maven wrapper3.9.16, Node24.16.0, npm11.13.0,
CAP5.1.1, Spring Boot4.1.1, Testcontainers2.0.5와 PostgreSQL18.6을
사용한다. PG image는 fixture가 고정한 다음 digest다.

`postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`

`npm ci --offline --ignore-scripts --no-audit --no-fund --prefix backend`
는 exit0이다. 현재 package-lock만으로 dependency를 설치했다.
Maven은 `-o`로 기존 cache만 사용한다. 이 tree의253개 main source와
전체 test source를 직접 compile하고 이 tree의 CDS에서 CSN/EDMX를
생성한다. 다른 tree의 JAR·schema·code를 재사용하지 않는다.

첫 `clean test`는 cache에 없는 maven-clean-plugin3.5.0을 offline으로
가져올 수 없어 test0개에서 exit1이었다. target이 없는 새 tree였으므로
clean을 생략하고 같은 source로 실행했다. 최초 환경 실패 로그
`baseline-clean-plugin-environment.log`도 보존한다.

각 Maven 명령 앞에는 위 세 환경변수를 설정한다.

```sh
./mvnw -o -B -ntp -f backend/pom.xml \
  '-Dtest=PublicWorldReadPostgresTest#causalRecordWitnessRequiresRealCommandEffectAndCurrentAllOfSourcesWithoutPublicRead' \
  test

./mvnw -o -B -ntp -f backend/pom.xml \
  -Dtest=PublicWorldReadPostgresTest,DeliveryAdmissionAuthorityTest \
  -Dworld.evidence=/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-37156c4f/.local/causal-witness-combined/public-world \
  package
```

JUnit XML을 직접 집계한 결과는 다음과 같다. 두 실행 모두 새 JVM과
실제 disposable PG를 사용한다.

| 실행 | tests | failures | errors | skipped | exit |
|---|---:|---:|---:|---:|---:|
| baseline witness 재현 | 1 | 1 | 0 | 0 | 1 |
| 최종 PublicWorldReadPostgresTest 전체 | 14 | 0 | 0 | 0 | 0 |
| 최종 DeliveryAdmissionAuthorityTest | 26 | 0 | 0 | 0 | 0 |
| 최종 package 합계 | 40 | 0 | 0 | 0 | 0 |

실제 evaluator의 normal/restricted 두 invocation과 수정한 witness
1개가 모두 실행됐다. normal은 SATISFIED, restricted는 UNSATISFIED인
실제 불변 assessment를 저장했고 두 경우 모두 commandId·COMMITTED·
실제 인도30·배분 CONSUMED와 assessGoal 권한 부재를 검사했다.
전체 suite189.384초, 별도 admission unit26개0.111초다.
`git diff --check`도 exit0이다.

산출물은 이 worktree의 `.local/causal-witness-combined/` 아래에 있다.
`baseline-reports/`는 수정 전 XML/TXT, `final-reports/`는 최종 XML/TXT다.
`public-world/`는 테스트가 생성한 공개 world와 SQL 대조 artifact다.
`handoff.json`은 원 JUnit에서 읽은 test별 결과·모든 artifact hash·
생성 CSN/EDMX hash를 기록한다. coverage receipt는 생성하지 않는다.

현재 tree의 tracked build input458개 hash를 package 진행 중 채취해
완료 뒤 대조했고 source drift는0이다. aggregate는 정렬한
path→SHA-256 JSON을 compact encoding한 bytes의 SHA-256이다.

| 대상 | SHA-256 |
|---|---|
| tracked build input aggregate | `62747ff29b8e6bf6c9a7bfd7dd9963b2c9be29ddf5231dcd36585d2b24f9cd51` |
| PublicWorldReadPostgresTest.java | `3316c9cf7a879c3ed3af56a90016b183a5fcc876da07635fdf2b8691e72b3590` |
| DeliveryRecordCause.java (baseline 그대로) | `b66a25173167a507fe8fa45e11af1e8ced1be742ad4efd544db9d6fe736620cd` |
| DeliveryAdmission.java (baseline 그대로) | `ea74b1a1cd515e963017c91090318d84f3f84fea6298ece666ec86a1079695de` |
| 자체 package JAR | `8250f6a8fa580303ccca654ad303b159781ee5ad13e565dbed0bd1bdc0bf0b98` |
| 자체 csn.json | `c21d4f9f0eaae148385954deafb1329e98df7a7b4899d22cbaec84fbc838994c` |
| root-backend-initial.log | `c72f3510173aca29e09a30b779c91a87c19bfc5e9ef3dd4ba25ac10f18e4a3e9` |
| baseline-red.log | `b4288710851c2fef060637c83ec5d49edd4312260e1d1570c6e97338d430832a` |
| baseline-clean-plugin-environment.log | `58a5d9fb489f8bfcdf20a0886ac09d7dd14701739213da4e798aefe0fcf836a8` |
| focused-green.log | `9be0924f76a49f50f20aded574602233fbf5ddb7d45b1f8ea57cf5d443422051` |
| final-reports/TEST-com.mulino.quality.PublicWorldReadPostgresTest.xml | `80f6ac44c56738600b79f185d23e1f2c9b2395d07f72c528d3cd1b3b56d439b7` |
| final-reports/TEST-com.mulino.trade.sales.DeliveryAdmissionAuthorityTest.xml | `0587996345b265700c839a347a9383c1eb14866edb766dfa21f3c9bae2f43859` |

cleanup.json은 이 두 실행의 PG2개·Ryuk2개가 모두 없어졌고 임시 blob
root2개도 제거됐음을 확인한다. 별도 key를 만들지 않았다. 첫 정리
collector는 Docker의 소문자 `no such object` 응답을 대소문자 때문에
오판해 exit1이었다. 원 오류는 cleanup-initial.log에 남기고 비교만
교정했으며 테스트를 재실행하거나 container를 강제 삭제하지 않았다.
다른 worker·root의 자원은 조작하지 않았다.

## 인수 범위

이 수정은 실제 PostgreSQL/application command·query의 backend focused
회귀다. 해당 suite는 JwtDecoder를 mock한다. root의 별도
PlatformIntegrationTest11개 환경 오류는 실제 decoder에 필요한
JWT_PUBLIC_KEY 누락이며 이 witness FORBIDDEN과 원인이 다르다.
이번 환경도 JWT_PUBLIC_KEY 없이 새 JVM에서 focused suite를 실행한다.
별도 HTTP 인증 suite의 성공으로 확대하지 않는다.

`./verify scenarios --actual`은 실행하지 않는다. 실제 시나리오 인수는
Haiku native dynamic Workflow 소유다. 실모델·BTP·규제·전체 인수는
NOT_RUN이며 이 회귀 수정으로 Task나 S gate를 닫지 않는다.
