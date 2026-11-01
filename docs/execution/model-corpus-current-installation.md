# 현재 packaged MODEL 입력 설치 증거다

## 판정이다

2026-10-24 현재 source baseline `dda42dd63e550741bd370a136760f39b062d1f20`에서
M01–M60 입력을 조사했다. 60개 중 44개는 원본 fixture를 바꾸지 않는
별도 typed installation input으로 정적 mapping을 완료했다. 44개를 새
disposable PostgreSQL에 설치했으며, 이 중 42개는 database-only probe,
M05와 M50은 packaged backend를 통한 실제 HTTP helper 설치다. 현재 설치
행은 모두 `COMMITTED` 또는 `COMMITTED_HTTP_HELPER_VERIFIED`다.

설치 원행의 source receipt는 64,527개이고 이에 대응하는 inverse 수도
64,527개다. database-only receipt/readback 검사는 각 `SERIALIZABLE`
설치 transaction 안에서 commit 전에 수행됐다. 이를 독립적인 post-commit
acceptance로 세지 않는다. 44개 모두 `fixtureComplete=false`이며 실제
model call·scenario run은 0개다. 제품 acceptance와 application model
runtime은 `NOT_RUN`이다. 따라서 이 결과는 현재 packaged MODEL 입력의
제한된 설치 가능성과 M05/M50 HTTP helper만 확인하며, model acceptance,
S5/S6 또는 전체 Task 완료를 뜻하지 않는다.

## current install 범위다

설치된 case는 다음 44개다. M05와 M50 이외의 42개는 database-only
경로다. 원본 fixture·binding·oracle의 보호 artifact 218개는 hash drift
없이 보존했다.

```text
M01–M12, M14, M17–M26, M28–M29, M31–M35, M37, M39–M46,
M48–M51, M59
```

44개 모두 PostgreSQL 18.6과 `btree_gist` 1.8을 사용했다. 42개
database-only attempt는 current migration SQL 45개를 수동 적용했고,
M05/M50 HTTP attempt는 실제 Flyway로 migration 45개를 적용했다.

예전 M04/M14/M45의 SUPPLIER schema 오류는 현재 fresh PostgreSQL 설치에
적용되지 않는다. 세 case는 current migration 45개로 `COMMITTED`다.
과거 미관찰 catalog kind 9개 중 8개에 SQL receipt가 있다.
`SupplierReply`는 계속 미관찰이다. M04의 `SUPPLIER_ACCEPT` event는 해당
SQL receipt가 아니다.

## M05와 M50의 실제 HTTP 결과다

- M05는 45개 Flyway migration과 receipt/inverse 각 1,505개를 설치했다.
  원본 issuer/audience 환경에서 별도 TARGET-bound grant 적용 전 read는
  `403 FORBIDDEN`, 적용 뒤는 `200`이며 `readApplied=true`다. 비즈니스
  effect 추가는 0이다.
- M50은 실제 validation `VALIDATED`와 첫 command `APPLIED`를 보였다.
  45개 Flyway migration과 receipt/inverse 각 1,381개를 설치했다. 동일
  요청 replay는 원 응답과 counts를 재사용하며, 변경 payload는
  `IDEMPOTENCY_CONFLICT`와 counts 불변을 보인다. warehouse quantity는
  `100`이다.

M50의 설치는 승인된 corrected physical Given만 사용한다. 두 helper는
스케줄러 요청과 responder request가 비어 있고, backend·PostgreSQL 및 HTTP
listener 정지와 당시 port 폐쇄, attempt runtime directory 제거를 기록했다.
port 폐쇄는 cleanup 시점의 관찰이며 이후 port가 재사용되지 않는다는
보증이 아니다.

초기 시도는 최종 결과와 분리한다. M05의 첫 backend start는 private blob
directory가 mode `0755`여서 거부됐고 새 attempt의 directory만 `0700`으로
만들어 다시 실행했다. 이어진 M05/M50의 첫 HTTP 설정은 launcher가 원본
MODEL identity와 다른 issuer를 사용해 `403`이 났다. 그 결과는 원본 grant의
negative 증거가 아니다. 재실행에서 바꾼 것은 임시 environment의
issuer/audience뿐이며 product source, fixture, grant 또는 oracle은 바꾸지
않았다.

attempt에는 해당 DB/app 연결을 위해 생성한 disposable credentials만
사용했고 기존 secret file은 읽지 않았다. Orchestration은 일반 process
environment 전체를 감사하지 않았으므로 environment가 credential-free였다고
주장하지 않는다.

## 재현 가능한 명령 형태다

정적 inventory 명령은 Java 21의 `ModelFixtureProbe`로 실행했고 exit 0을
기록했다.

```sh
java -Drepo.root=<ignored source snapshot> \
  -cp <executed harness classes and dependencies> \
  org.mulino.verification.nativeclient.ModelFixtureProbe \
  inventory-all ALL inventory-current.json
```

database-only 설치는 attempt마다 새 loopback PostgreSQL cluster, database,
owner와 attempt-owned temporary blob root를 만든 뒤 아래 installer probe를 실행했다.
이 probe는 current migration SQL과 typed source installer를 호출했으며
backend/Flyway 경로가 아니다.

```sh
java -Drepo.root=<ignored source snapshot> \
  -cp <executed harness classes and dependencies> \
  org.mulino.verification.nativeclient.ModelCorpusInstallProbe \
  Mxx <attempt-private-blob-root> <installation.json>
```

M05/M50은 같은 attempt의 실제 backend와 database를 두고 별도 HTTP
installer probe를 실행했다.

```sh
java -Drepo.root=<ignored source snapshot> \
  -cp <executed harness classes and dependencies> \
  org.mulino.verification.nativeclient.ModelCorpusHttpInstallProbe \
  M05-or-M50 <attempt-private-blob-root> <installation.json>
```

실행 증거는 ignored local directory `.local/model-corpus-current/`에 있다.
위 installer probe는 이 경로에서 쓰는 temporary evidence driver이며
repository에 포함된 일반 entrypoint가 아니다. M05/M50의 HTTP listener는
loopback only이고 external fixture endpoint와 scheduler start는 꺼져
있었다. `inventory-summary.json`의 command는 전체 dependency classpath를
보존한다. Evidence log에는 credential·key·token 원문을 넣지 않는다.

## 16개 입력 차단이다

현재 packaged layer에는 다음 explicit typed mapping/source graph가 없다.
어떤 것은 원본이 확보되면 engineering으로 mapping할 수 있고, 실제 source
fact·author 결정이 빠진 것은 출처가 있는 입력이 먼저 필요하다.
`baseline.domainFacts`에 기대 Boolean이나 summary 값을 넣어 빈 graph를
대체하지 않는다.

- M13, M27: `/baseline/domainFacts/delivery` typed graph다.
- M15: `/baseline/domainFacts/trace` typed graph다.
- M16: `/baseline/domainFacts/objects/Q/return` (`ReturnReceipt`)와
  `/baseline/domainFacts/e1` graph다.
- M30: `/baseline/domainFacts/conflictingClaims`와
  `/baseline/domainFacts/reconciliation` graph다.
- M36: `/baseline/domainFacts/dispatch` support ID,
  `/baseline/domainFacts/dispatch/delivered`,
  `/baseline/domainFacts/dispatch/remaining` 물리 사실이다.
- M38: `/baseline/domainFacts/settlementDifference` graph다.
- M47: `/baseline/domainFacts/queuedCommand`,
  `/baseline/domainFacts/allocation/{startQuantity,action,workId,authorizationActorId,segmentId,orderLineId,unit,customerId,rootId}`,
  `/baseline/domainFacts/allocation/consumedQuantity`,
  `/baseline/domainFacts/currentOwnerRef`다.
- M52: `/baseline/domainFacts/receipt`와
  `/baseline/domainFacts/remainingObligation` graph다.
- M53: `/baseline/domainFacts/receipt`와
  `/baseline/domainFacts/secondDocument` graph다.
- M54: `/baseline/domainFacts/incomingObservation`,
  `/baseline/domainFacts/faultInjection`, `/baseline/domainFacts/intake`
  graph다.
- M55: `/baseline/domainFacts/delivery`와
  `/baseline/domainFacts/return` graph다.
- M56: `/baseline/domainFacts/objects/S1/contributions`,
  `/baseline/domainFacts/delivery`, `/baseline/domainFacts/correctionEvidence`,
  `/baseline/domainFacts/assessment`,
  `/baseline/domainFacts/shortfallObligationPolicy`다.
- M57: `/baseline/domainFacts/recall` graph다.
- M58: `/baseline/domainFacts/holds`,
  `/baseline/domainFacts/allocation/{startQuantity,action,workId,authorizationActorId,segmentId,orderLineId,unit,customerId,rootId}`,
  `/baseline/domainFacts/allocation/currentEligibilityNeedsRecheck`다.
- M60: `/baseline/domainFacts/shipment`와
  `/baseline/domainFacts/stocktake` graph다.

원본 `DOC1`/`DOC2` bytes는 고정 hash대로 archive에 없다. 모든 case는
`DOC1` SHA-256 `5525afd2e97fb8f88f063b2bc5869cb70687397157d985d5b9bb9de697269aa8`
를 요구한다. namespace는 M07만 `authority-KR`, 나머지는 `warehouse-W`다.
`DOC2` SHA-256은
`a430f1c489e2e11d2adc657e6a1042abb64b2afd110e92863d4b779270214f1d`이며,
namespace는 M04만 `supplier-IT`, 나머지는 `carrier-IT`다. 원본 bytes는
fixture/source owner에게서 받아야 하며, 대체가 필요하면 별도의
corrected-Given 결정을 남겨야 한다. hash를 추측해 fixture를 만들거나
권한으로 해석하지 않는다.

이 결과는 사용자에게 임의의 DB row 작성을 요구하지 않는다. Fixture/source
owner가 원본 bytes와 각 source fact를 제공하면 그 provenance를 사용해
missing mapping을 구현한다. bytes를 사용할 수 없으면 별도의 author decision으로
corrected Given을 확정한 뒤 변경 provenance를 보존한다. 이후
`SupplierReply` SQL receipt focused proof, full regression과 지원되는
application-client runtime 연결을 engineering work로 수행한다.

## provenance와 hash다

- Code baseline은 `dda42dd63e550741bd370a136760f39b062d1f20`이다.
- Original input lock의 baseline은
  `a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`이다.
- 60개 generated input index의 SHA-256은
  `866001fdc34d09f7ea6bc67aef7b49f28415eb4dc2aa44393a974d64886b0fe6`
  이다.
- `installation-inputs.tar.gz` SHA-256은
  `32d5c2df808ad89892dd7d7fe2e3c681e212c0f9176402be44b215602dda1519`
  이다.
- M05 fixture/input SHA-256은
  `2a6367c411cc00e2e00aff2efeaee2ecd4e8247605df79bc42a6d8f66d4a4cf4` /
  `d610d738632d43da95f34430faa696561f2dbcda95f8c457e47f3c3b6afb19c3`
  이다.
- M50 fixture/input SHA-256은
  `d48b3fc060b0a8f4835fdf8ea46dc4aeea2592c71621000574844d72db72abcb` /
  `91a56c6c44bfcdcaeb657a6d70c63f05d8125447d17be7e3905900cd9cd629ee`
  이다.
- Final aggregate summary SHA-256은
  `546421288fc7228413f4defdb8c48b223bea03ac442e835286d1483a4b1fd0cd`
  이다.
- Inventory / database-only summary SHA-256은
  `02ce1cac16e034af32c5dfbf4cb86a0848a818c66ac0db160eb936906ed5e820` /
  `525f875c9eba051b0323c11b72b5179d51559b78da5369820f27f2fa1cf4dd58`
  이다.
- 293 executed harness class hash의 SHA-256 manifest는
  `70858b7cd27e153f4f15b5353f8e2e38966365d777316f2ff9169fe2d0960675`
  이다.
- M05/M50 installation JSON SHA-256은
  `e557891d3cb2cb6520402649c6c7487acec15e3641fde0972f61a95b5f78feeb` /
  `9d6cc93fc229193fabd7e4958287a5d8d83259d293f1fc3767084271a48797cd`
  이다.
- M05/M50 summary JSON SHA-256은
  `4da0b18899620fd0529edb28878d4c3af0cece7b5cb49a331c642b7a44834b06` /
  `8f1892c3e23c77b58840df85b06390e4f21c1be4325fade458ae9bed8e75307b`
  이다.
- Backend package `final-result.json` SHA-256은
  `9184c2627a18bc83c867dae19becc5f49e4954fe534f731ab0882519dc1f3978`
  이다. Packaged JAR SHA-256은
  `699bb4b7c6b7ef4a856b13585446820cdcacffa4fbd1aa31300c268301270543`
  이다.
- Legacy `proof-index.json` SHA-256은
  `2e08c0617ab3751c26cceb2911e45e794b3f2eea08bef57fdf98017026a93e3c`
  이다.
- Original input lock JSON SHA-256은
  `b1f39ecea11bf62a25d1fc4bde104a4ea6df33b4d78b9a8173896994888b0f9f`
  이다.

`proof-index.json`은 `a7f18cc8` baseline과 더 오래된 archive에 속한
역사 증거다. 이 current installation 결과로 다시 만들거나 재귀속하지
않았으며 원본 bytes를 그대로 보존했다.

M05/M50 backend에는 source baseline `dda42dd6`에서 Maven
`package -DskipTests`를 실행했다. 첫 시도는 `backend/node_modules`의 CDS
CLI가 없어 실패했으며 Java compile은 시작되지 않았다. `npm ci`가 90개
package를 설치했고 lockfile은 바뀌지 않았다. 재시도는 exit 0으로 끝나
259 main·106 test Java source를 compile했으나 `-DskipTests`였으므로 test는
실행하지 않았다. 산출물은 69,450,168 bytes, Spring Boot `4.1.1` JAR다.
Maven package의 원 결과 hash는
`.local/model-corpus-current/backend-build/20261024T053115Z-package/final-result.json`
에 있다.
Java 21, Node `v24.22.1`, npm `11.9.0`을 사용했다. toolchain 세부사항은
[cloud 실행 기록](model-archive-cloud-integration.md)에 연결한다.

재실행 package command는 Maven wrapper를 사용했다. 비밀·임시 경로는
placeholder로 표시했다.

```sh
./mvnw -B -ntp -s <temporary-settings.xml> \
  -Dmaven.repo.local=<temporary-repository> \
  -Dmaven.compiler.fork=true \
  -Dmaven.compiler.executable=<verified-JDK21-javac> \
  -f backend/pom.xml -DskipTests package
```

Probe는 기존 compiled harness `target` class를 사용했다. 293개 class
hash는 기록 전후 일치했다. compiled/tested `e2f71ce0`와 `dda42dd6`의 차이는
`model-archive-cloud-integration.md`, `task-status.md`, 이 installer
README의 문서 3개뿐이다. 이는 새 clean harness compile이나
reproducible-build attestation이 아니다. backend JAR build와 harness probe
class provenance는 서로 다른 증거다.

최종 aggregate는 44개 attempt의 cleanup을 기록한다. `fixtureComplete=false`,
고정 문서 bytes 부재와 남은 16개 typed graph를 유지하므로 이 증거만으로
runtime 수용을 주장하지 않는다.
