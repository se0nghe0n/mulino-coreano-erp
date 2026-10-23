# Typed 인도 의무 이후 rollback 회귀를 복구한다

2026-10-24에 실제 PostgreSQL에서 두 실패를 재현하고 테스트 hook을
수정했다. focused 2개와 관련 cohort 19개가 모두 성공했다. 제품
변경은 없으며 기존 rollback·중복 transaction oracle을 보존했다.

## 범위와 원인

- baseline은 `d0885a8031eb754e42f35f998a6d7f970b059cee`다.
- 전용 branch는 `fix/delivery-rollback-regression`이다.
- worktree는
  `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8ca6a617`이다.
- 소유 파일은 `FulfillmentPostgresTest.java`와 이 기록이다.
- T3 위임의 `codex_2 / gpt-6.1-sol / xhigh / priority` 설정을
  유지했다. live catalog와 현재 thread에서 provider/model을,
  Codex turn context에서 `xhigh`를 확인했다. 별도 위임은 하지 않았다.
- 관련 계약은 계획 §6·§7.3·§7.4의 D17/T17·D24/T24와 V6다.

`DeliveryCommands`의 임시 인도 접수는 residual scope·quantity·unit이
추가된 10개 인자의 `TradeImpact.recorded`를 호출한다. baseline의
두 테스트는 예전 7개 인자 overload에만 예외를 주입했다. 따라서
실제 접수와 의무가 commit되고 `assertThrows`가 실패했다.

`failAfterRealDeliveryDuty`는 실제 10개 인자 overload를 spy로
가로채 `callRealMethod()`를 실행한다. 같은 transaction에서 해당
관측의 실제 `20 EA` root와 `OPEN`·`valid` assignment, 인간 owner·
nextAction·nextCheckAt을 SQL로 확인한 뒤 지정 예외를 던진다.
두 테스트는 이 예외의 메시지도 확인한다.

첫 테스트의 기존 관측·root 부재, duty 원행 동일성, 감사·명령·물리
상태 복원을 모두 유지한다. 둘째 테스트도 실제 의무 생성 이후에
실패하도록 바꾸고 root·물리 상태 복원을 추가한다. 기존 outbox·
감사·명령 rollback과 latch·`pg_blocking_pids`로 관찰하는 두 실제
transaction의 중복 대기, 동일 응답·효과 1회 oracle을 유지한다.
중복 처리 후 추가 root 1개와 물리 상태 동일성도 확인한다.

## 실행 환경과 명령

JDK는 OpenJDK `21.0.12.1`, Maven은 `3.9.16`, Node는 `24.16.0`이다.
고정 backend lock으로 `npm ci --ignore-scripts --no-audit --no-fund`를
실행했다. CDS Java·plugin은 `5.1.1`, Spring Boot는 `4.1.1`,
CDS compiler는 lock의 `7.1.1`이다. Docker server는 `29.4.0`이다.
Testcontainers는 다음 PostgreSQL image의 fresh DB에 Flyway 44개
migration을 적용했다.

```text
postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280
```

모든 PG 실행에 실제 context에서 얻은 아래 환경을 명시했다.

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:/Users/se0nghe0n/.nvm/versions/node/v24.16.0/bin:$PATH"
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
```

`DOCKER_HOST`의 관찰값은
`unix:///Users/se0nghe0n/.orbstack/run/docker.sock`이다.
run별 private key는 process memory에서만 생성해 public PEM을 만든다.
`JWT_PUBLIC_KEY`에는 임시 public PEM 경로만 제공하고 종료 후 삭제했다.
`-DargLine=-Djava.io.tmpdir=<run>/temp`로 임시 blob도 격리했다.

원본 root의 실패 artifact는 다음 경로에서 읽었다.

```text
/Users/se0nghe0n/Developer/mulino-coreano-erp/.local/resume-20261024/fix-until-done/backend-full
```

이 worker의 실행 artifact root는 다음 경로다. 원본 compiled artifact나
schema를 복사하지 않았으며 이 tree에서 CDS·resources·JAR를 생성했다.

```text
/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8ca6a617/.local/delivery-rollback-20261024
```

`run.py`가 실제 실행한 argv·환경·버전·exit code·JUnit hash·cleanup을
각 run의 `execution.json`에 기록한다. PG 명령은 다음 공통 옵션과
각 JSON의 정확한 `-Dtest` selector 및 `test` goal로 실행했다.

```text
./mvnw -B -ntp -f backend/pom.xml
-Dlogging.level.com.sap.cds=WARN
-DargLine=-Djava.io.tmpdir=<run>/temp
-Ddelivery.provisional.evidence=<run>/sql-proof
```

| 실행 | tests | failures/errors/skips | exit | 초 |
|---|---:|---|---:|---:|
| `red-pg` baseline | 2 | 2/0/0 | 1 | 22.784 |
| `green-focused` 수정 후 | 2 | 0/0/0 | 0 | 25.041 |
| `green-cohort` 인접 인도 | 12 | 0/0/0 | 0 | 99.640 |
| 같은 cohort typed reconciliation PG port | 7 | 0/0/0 | 0 | 위와 같다 |

RED의 두 원인은 원본과 동일한 `Expected IllegalStateException to be
thrown, but nothing was thrown`이다. 최초 `red` 시도는 compiler 설치
전 `cds: command not found`로 exit 1이었으며 테스트는 `NOT_RUN`이다.

cohort의 12개는 임시 접수·재전송·현재 인가, 미확인 root·입력 거부,
정상·회수 후 인도, 허위 보고, 반복 canonical 연결, 출고 identity
rollback·gateway 원자성을 검사한다. 나머지 7개는 기존
`TradeReconciliationPostgresTest`의 typed 수량·단위, legacy 호환,
실제 update rollback port 검사다. 해당 port fixture 범위를 기록한다.
각 testcase의 정확한 이름은 `diagnostic-summary.json`에 보존한다.

focused SQL artifact에서 예외 직전 reconciliation root·duty는 각각
2개이며 rollback 뒤 기존 1개만 남는다. 동시 중복 재시도 뒤 각각
2개로 하나씩만 증가한다. `green-focused/sql-proof/in-flight-*.json`,
`rollback.json`, `claim-rollback.json`, `claim-concurrent-duplicate.json`에
실제 SQL 행을 저장했다. 전체 artifact hash 목록도 summary에 둔다.

## Source·JAR·fixture hash와 정리

`./mvnw -B -ntp -f backend/pom.xml -DskipTests clean package`를 이 tree에서
실행해 exit 0을 관찰했다. `package/execution.json`에 build argv·log hash를
보존했다. 아래 source digest는 경로순 `path + NUL + fileSHA256 + LF`의
SHA-256이다. 대상 357개 파일과 개별 hash는 `source-hashes.json`에 둔다.

| 대상 | SHA-256 |
|---|---|
| 제품 source·계약·migration·build 입력 | `aba515eb3b14f6791acf13988df540153972519b646ba6c09f0988f41f5a0b0f` |
| baseline 실행 fixture/test source | `bb85383b0e817d7ad57480da0149a4faf36139c7800b4a9e40fc3fb498b8b5cf` |
| 수정 실행 fixture/test source | `835b06d5aefe7c9ca2831cd0fb16e963ea0c94e9a6a9c1bf7b448bb94aaf538a` |
| 자체 생성 `ontology-0.1.0-SNAPSHOT.jar` | `86567d5c9844990dfd489bc04f2d537fd3abfa7914548a611393294b112ab31f` |
| 자체 생성 `edmx/csn.json` | `c21d4f9f0eaae148385954deafb1329e98df7a7b4899d22cbaec84fbc838994c` |
| RED JUnit | `56f6bd6e2599f196ef2e72e756b1c913c2bb22fa98dc0b5aeaa6efdb2aee1d5a` |
| focused JUnit | `ab0ee89ea97cbf7207e27e64690f54f27bcf44542a3c433bbea348fc789dd7e7` |
| 인도 cohort JUnit | `908b3088704f2824e81a873c2caf013d2fde34579113d474588703cefda27a70` |
| typed port cohort JUnit | `eb330919412e51ca10f2f6e60724a70aca1b789403534bad9c22181b723f8c52` |

`build-byte-proof.json`에서 제품 source의 baseline byte drift 0과
JAR 내부 class/resource의 자체 `target/classes` byte 불일치 0을 확인했다.
각 run의 로그에서 식별한 소유 container 총 7개는 전부 부재를
재확인했다. PG 4개·Ryuk 3개이며 임시 root 3개와 public PEM·blob도
삭제했다. 타인의 container·worktree·branch에는 쓰지 않았다.

최종 `final-byte-proof.json`은 commit 후 owned file의 Git blob·working
byte·실행 fixture byte 일치, clean worktree, 소유 container·temp 부재를
다시 대조한다. Git에는 두 소유 파일만 commit하고 이 tree와 ignored
실행 artifact는 coordinator의 통합을 위해 남긴다.

이 기록의 결과는 한정된 JUnit/PG 회귀 검증이다. 전체 899개·112개 재실행,
`./verify scenarios --actual`, 실모델·규제·BTP 검증은 이 worker에서
`NOT_RUN`이다. 전체 Task·S gate의 판정과 병합은 coordinator가 맡는다.
