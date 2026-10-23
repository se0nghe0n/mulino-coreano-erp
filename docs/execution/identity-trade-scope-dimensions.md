# 거래 상대와 LOT scope 차원을 저장한다

구매 handler가 요구하는 좁은 SUPPLIER grant를 저장할 수 없던 DB
제약을 수정한다. 권한의 새 발급이나 ORGANIZATION 확대 없이 기존
capability assignment와 grant가 같은 typed 차원을 표현하게 한다.

기준선은 `e995595a3e39ffb2170a6e02c9991b6ea38c1989`다.
작업 branch는 `fix/typed-trade-grant-scopes`이며 작업 트리는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-e08ae0e5`다.
D08/T08, D13/T13, D19/T19와 C3/V7의 인가 경계를 다룬다.
전체 시나리오 인수와 실모델 인수는 이 작업의 실행 범위가 아니다.

## 원인과 저장 계약

사용자가 전달한 M04 실패는 ITEM=P, PLACE=W, WORK=O1,
SUPPLIER=syntheticSupplier grant 설치의 SQLSTATE `23514`다.
원 실패 로그는 root의 `.local/model-fixture-probe/*M04*r4*`에
보존하며 여기서 수정하거나 재실행하지 않는다.

기준선의 `PurchaseCommands.prepare`는 SUPPLIER를 내보낸다.
`SettlementCommands.scopes`는 거래 line의 supplierId/customerId에
따라 SUPPLIER/CUSTOMER를 내보내며 `SettlementQueries`도 같은 함수를
호출한다. `SalesQueries`, `RecallQueries`, `RecordObjectQueries`도
CUSTOMER scope를 사용한다. 기존 V3의 GrantScopes와
CapabilityAssignments CHECK에는 두 차원이 없다.

`ScopeDimension`을 identity 경로의 어휘와 typed resolver로 사용한다.

| 차원 | 식별 계약과 실제 CQN 대상 |
|---|---|
| ORGANIZATION | 현재 organizationId와 같은 UUID, identity.Organizations |
| TARGET | UUID인 기존 다형 참조, 특정 entity를 역추정하지 않는다 |
| WORK | UUID, work.read.Works |
| ITEM | UUID, inventory.TradeItems |
| PLACE | UUID, inventory.Places |
| SOURCE | 공백이 아닌 최대 36 codepoint 원문, UUID 강제 없음 |
| LOT | UUID, inventory.ManufacturingLots |
| CUSTOMER | UUID, trade.sales.Customers |
| SUPPLIER | UUID, trade.purchase.Suppliers |

표의 entity 앞에는 모두 `mulino.` namespace가 붙는다.
typed 차원은 정확한 entity와 현재 조직을 함께 조회한다. 존재하지 않는
UUID, 다른 조직의 대상과 다른 유형의 UUID는 같은 FORBIDDEN이다.
미등록 차원과 UUID 형식 오류는 TYPE_INVALID다.

TARGET에는 문서·활동·업무·거래 등 여러 유형이 들어간다. 범용 scope의
DB FK가 없는 것을 특정 entity의 FK로 위장하지 않는다. SOURCE도
namespace를 profile UUID로 변환하거나 거꾸로 추정하지 않는다. 현재
evidence/transport handler가 제시하는 sourceProfileId는 그대로
허용되는 SOURCE 값이다. 원문 namespace grant가 다른 profile ID까지
허용하지는 않으며 정확히 같은 대상 값일 때만 일치한다.

LOT은 기존 ManufacturingLot 및 evidence subject의 유형을 따른다.
기준선의 업무 handler는 LOT을 주로 TARGET/ITEM과 typed subject로
표현한다. LOT 차원 등록만으로 기존 handler에 LOT을 추가하거나
TARGET 기반 grant를 LOT으로 바꾸지 않는다. handler가 LOT을 제시하지
않으면 LOT 제한 grant는 계속 거부된다.

V45만 추가하고 V3를 수정하지 않는다. 두 CHECK에 세 차원을 추가하며
기존 scope 행은 보존한다. identity CDS의 세 scopeId는 UUID에서
String(72)로 바꾼다. CAP 5.1.1의 길이 검사는 UTF-16 code unit을 세므로
String(36)은 36 codepoint 원문을 모두 저장하지 못한다. DB의 동일
VARCHAR(72)와 `char_length(scopeId) <= 36` CHECK, helper의 codepoint
검사가 기존 36자 한계를 유지한다. 실제 36개 보조 평면 문자 저장과
37개 거부를 검사한다. schema 소유권은 계속 Flyway 하나다.

## 인가와 typed subject

IdentityCommands는 prepare와 실행에서 scope를 검증한다. 구체 noun이
있는 scope는 handler 소유의 선택적 SubjectBinding으로 제공한다.
Supplier, Customer, ManufacturingLot 선언은 검증한 scope UUID만
가리킨다. 선언 생략이 새 필수 입력을 만들지 않는다. TARGET의 noun을
추정하거나 SOURCE 문자열을 UUID SubjectBinding으로 만들지 않는다.
최종 선언의 정의·대상·개수 검증은 기존 CommandDefinitionResolver가
계속 맡는다.

IdentityAuthorization은 미등록 차원, 잘못된 ID, 다른 조직의
ORGANIZATION scope를 효과 없이 거부한다. 차원 간 all-of, 같은 차원의
ID 간 OR, 현재 membership·assignment·grant와 부모 grant 재검증은
유지한다. 새 차원을 알고 있다는 사실은 새 authority가 아니다.

## 검증과 증거

2026-10-24 focused 실행은 27 tests, failures 0, errors 0, skipped 0,
exit 0이다. 실제 PostgreSQL 18.6과 CQN에서 다음을 관찰했다.

- 제품 PurchaseCommands.prepare의 ITEM·PLACE·WORK·SUPPLIER 네 차원으로
  좁은 grant를 발급·저장했다. 네 차원 중 하나가 없거나 다르면 거부했다.
- SUPPLIER·CUSTOMER·LOT 각각의 assignment와 grant를 저장하고 실제
  typed noun을 바인딩했다. 같은 UUID라도 다른 차원은 허용하지 않았다.
- 미등록 차원·형식 오류·비존재·다른 유형·다른 조직은 prepare와 실행,
  grant와 capability assignment 양쪽에서 거부했다. 전후 identity 행,
  구매 제안, 물량 원장, runtime outbox의 증가가 없었다.
- 부모 grant의 all-of 조건을 생략한 새 위임은 효과 없이 거부했다.
  부모 grant 철회 뒤 자식은 같은 대상에도 권한이 없었다. 현재 철회된
  assignment도 과거 asOf/knownAt으로 복구되지 않았다. 같은 차원의
  ID 간 OR는 별도 unit assertion으로 유지했다.
- V44의 두 테이블에서 세 차원 insert가 여섯 번 SQLSTATE `23514`로
  실패했다. V45 적용 뒤 같은 여섯 insert가 성공했고 미등록 차원은
  두 테이블 모두 `23514`로 거부됐다. 기존 ITEM과 SOURCE 원문 행이
  그대로 남았다. 새 DB 전체 migration 설치도 함께 성공했다.
- SOURCE 원문과 36개 보조 평면 문자가 CQN에서 원문 그대로 저장됐다.
  37개 문자는 DB CHECK에서 `23514`로 거부됐다.

실행 command는 다음과 같다. 경로는 위 작업 트리 기준이다.

```sh
./mvnw -B -ntp -f backend/pom.xml \
  -Dtest=ScopeDimensionTest,IdentityAuthorizationTest,IdentityPersistenceTest,IdentityTradeScopesPostgresTest,AuthorityEvidenceTest,CommandSubjectTest \
  -Didentity.scope.inventory="$PWD/.local/identity-trade-scopes/evidence/live-query-registration.json" \
  test
./mvnw -B -ntp -f backend/pom.xml -DskipTests package
```

JDK는 Homebrew OpenJDK 21.0.12.1, Maven 3.9.16, Node 24.19.0,
npm/npx 11.19.1이다. package-lock의 CDS 10.1.1, CDS DK 10.1.0,
compiler 7.1.1, postgres adapter 3.1.1을 npm ci로 설치했다.
DOCKER_HOST는 `unix:///Users/se0nghe0n/.orbstack/run/docker.sock`,
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE는 `/var/run/docker.sock`,
TESTCONTAINERS_HOST_OVERRIDE는 `127.0.0.1`로 명시했다. 서명 검증용
임시 public key 경로를 JWT_PUBLIC_KEY로 제공했으며 private key와
token은 증거에 포함하지 않는다.

최종 로그는 `.local/identity-trade-scopes/evidence/focused-final.log`다.
SHA256은
`28c95a46aa46b7f1ac5d340c861e2a0e5dcb93b263cba1d25bad1ff339dbae92`다.
package는 exit 0이며 추가 테스트는 실행하지 않았다. 생성 JAR의 SHA256은
`6202aee370c1ba1deb99a2959ee25516a0ba0f193f8a1ec68945b9d7eb979827`다.
테스트와 package의 CSN SHA256이 모두
`97e3ffdaac7fcf7b5f1e0fb09b46bd900646cddd14bfc72f795ac150732fa434`로
같았다. 원 V3와 package-lock hash는 versions-exact.txt에 기록했다.

위 evidence 디렉터리의 로그·실제 등록·JUnit 원본 압축과 checksum을
`.local/identity-trade-scopes/identity-trade-scopes-evidence.tar.gz`로
묶었다. SHA256은
`37e8bd0fc3cbdca1f5ff4998c15c6ef785c6ce9df91fcd8621085ea42e3769c3`다.
이 작업의 로그로 확인한 컨테이너 17개와 JVM 6개는 종료됐고 임시
public/private key를 정리했다. 다른 작업자의 실행 자원은 중지하지 않았다.

실패 attempt 로그도 같은 evidence 디렉터리에 보존한다. 첫 test
compile의 overload 모호성과 초기 테스트 setup의 specification 누락은
검증 코드 오류였다. String(36)의 UTF-16 거부는 실제 CQN에서 재현한
추가 제품 제약이었다. 기존 AuthorityEvidenceTest의 문자열 TARGET을
UUID로 고친 것은 unit fixture를 원 계약에 맞춘 변경이며, actor·grant
사슬·시계·oracle는 유지했다. 실모델 corpus/fixture는 수정하지 않았다.

이 결과는 focused JUnit/실제 CQN·PostgreSQL 증거이며 전체 coverage
PASS와 구별한다. scenario/model/Workflow, HTTP/MCP, BTP와 규제 인수는
NOT_RUN이다. root의 V46과 결합한 migration 실행도 여기서는 NOT_RUN이다.

## Root 통합 소유

공개 `contracts/intent.schema.json`에는 scopeKind enum이 없다.
`contracts/request-contracts.json`의 grantAuthorization 설명은 여전히
여섯 차원과 installer의 미표현 supplierAliases/customerAliases를
기술한다. 제품의 새 차원 지원과 installer의 실제 지원 여부를 구별해
root가 필요한 최소 설명을 결정한다. 원 fixture·binding·oracle·생성기는
수정하지 않는다.

NativeMcpTransport는 root/context worker 소유다. 이 작업에서 읽거나
수정·import하지 않는다. root의 기존 여섯 차원 enum에
`LOT`, `CUSTOMER`, `SUPPLIER` 세 항목만 추가하면 위 어휘와 일치한다.
SOURCE의 36 codepoint 원문 검사는 유지한다.
