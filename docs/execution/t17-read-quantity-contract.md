# T17 공개 수량과 prior Dispatch 전제의 가역 계약

2026-10-24, baseline `fb58d86663f28ec90dd38322399c384a42de4672`에서
`fix/t17-read-quantity-contract` branch와 별도 T3 worktree로 작성한다.
실행 모델은 `codex_2/gpt-6.1-sol/xhigh/priority`다. Task와 S5/S6는
열린 상태다. root의 다른 변경은 이 worktree에 가져오지 않는다.

SQL 물리 segment에는 현재 SELL 적격성 필드가 없다. 따라서 원행에
`eligibilityStatus`를 붙이거나 missing을 0으로 처리하지 않고, 공개
제품의 동적 수량과 별도의 물리·조건 출처 증거를 연결한다.

`verification/cases/T17/author_read_quantity_contract.py`와
`read-quantity-contract-delta.json`이 정확한 before/after 행을 보존한다.
원래 301 assertion의 상대 ID, op, fixed expected, oracleRef,
oracleExplanation, requirement와 예상 unit은 모두 유지한다. 변경은
아래 13 assertion의 source/selector/unitSource/evidence 참조다.
125개의 엄격 proof는 원래 301개에 추가되어 이 worktree에서는 426개다.
fixture, actor, grant, clock, 원래 action과 condition status의 byte는
바꾸지 않는다. root의 별도 RESPONSE_SCOPE 8개는 root가 합성한다.

## 물리 원행과 동적 적격성

`reserve-pick-dispatch/eligible-after-reserve-4`는 공개 `getObject`의
`data.segmentEligibility`에서 ALLOWED segment의 `eligibleQuantity`를
합산하고 원래 `60 BOX`와 대조한다. 같은 구간의
`unreservedEligibleQuantity` 합계는
`unreserved-after-reserve-6`의 원래 `0 BOX`와 대조한다. unit은 같은
공개 segment의 실제 `unit`에서 읽는다. 기존 공개 aggregate의
60·0과 SQL 보유 100도 함께 검증한다. 예약은 물리 감소가 아니다.

다섯 `eligibility-*` subcase의 `eligibility-response-3`은 원래
`ALLOWED AND regulatory UNKNOWN` 물리 후보 0이라는 의미다.
가역층은 항상 존재하던 U의 물리 ID를 공개 `segmentEligibility`의
`segmentId`에 연결한다. 모든 규제 UNKNOWN 후보 집합을 공개
`conditionResults`의 정확한 ID 집합과 독립 대조하며, 집합의 각 후보에
ALLOWED 0개와 빈 `allowedRanges`를 요구한다. `eligibility-unknown`의
집합은 B·U이며 나머지 넷은 U다. 따라서 U 하나의 0으로 B의 잘못된
허용을 감추지 않는다. `eligibility-positive-control`은 같은 공개
응답에서 원래 A의 ALLOWED 1개를 요구한다. 기대 status는 바꾸지 않는다.

동시에 다음 독립 증거를 유지한다.

- 공개 물리 ID 집합과 SQL의 ID·item·LOT·W·수량·단위·active tuple이다.
- A의 정확한 `[0,60)` 또는 `[0,20)` 허용 구간과 확정 적격 수량이다.
- 물리 보유 100 또는 80 BOX, 실제 SQL 배분 ID·segment·시작·양·단위·
  EXECUTABLE 상태와 예약 전후 물리 열·movement 비교다.
- observer의 `sourceEvidence.complete`, 실제 SQL `sourceQuery`와
  REPEATABLE READ snapshot의 query 전후 digest 결합이다.
- 기존 다섯 조건 QC/REGULATORY/LABEL/DISPOSITION/CUSTOMER_CONTRACT의
  fixed status, `quality-eligibility-v1`, `evidenceRefs`와 독립
  `sellConditionSourceRefs/Links`의 typed 원천·검증 chain이다.
- ALT OTHERPACK는 별도 API/SQL scope를 사용한다. 같은 양의 ALT가 원래
  P 주문을 이행할 권한은 없으며, FEFO의 사유 없는 거부와 사유 있는
  배분은 실제 command 결과·행으로 대조한다.

observer의 추정 command attribution은 reserve 뒤 물리 원행에 다른
metadata를 붙일 수 있다. 물리 불변 검사는 이 metadata를 제외한 실제
물리 열을 비교한다. 원래 SQL ownerId의 명시 null은 null로 보존하며,
독립 JDBC의 예약/claim 전후 전체 tuple에서 nullable ownership까지
비교한다. 관측기의 evaluator 호출이나 파생 permission은 추가하지 않는다.

## 관찰별 prior Dispatch 전제 조회

`getObject(id=<실제 observation ID>, scope.objectType=DeliveryObservation)`의
`data.priorDispatchCheck`는 아래 다섯 필드다.

| 필드 | 의미 |
|---|---|
| `required` | 이 관찰의 정상 인도 확정에는 prior Dispatch가 필요하다 |
| `linkedDispatchId` | 저장한 관찰이 주장한 Dispatch ID 또는 명시 null이다 |
| `salesLineId` | 저장한 관찰이 주장한 line ID 또는 명시 null이다 |
| `status` | 아래 저장 사실 대조의 ALLOWED/DENIED/UNKNOWN이다 |
| `reasonCode` | 누락·확인 불가·상충을 구별하는 이유다 |

`SalesQueries.project(Observations)`는 실제 `DispatchCargoPort`를 호출한다.
관찰 자체의 조직·현재 READ·all-of 인가를 통과한 뒤 연결 Dispatch도
TARGET/WORK/ITEM/PLACE/CUSTOMER scope로 검사한다. 연결 ID는 관찰에 이미
저장된 값이며 hidden Dispatch에서 새 ID를 가져오지 않는다.

| 저장 사실/가시성 | status | reasonCode |
|---|---|---|
| Dispatch 참조가 없다 | DENIED | PRIOR_DISPATCH_MISSING |
| 조직/knownAt에 보이지 않는 참조다 | UNKNOWN | DISPATCH_UNAVAILABLE |
| 현재 Dispatch READ scope가 없다 | UNKNOWN | DISPATCH_READ_SCOPE_UNAVAILABLE |
| 연결 필수 열이 빠졌다 | UNKNOWN | DISPATCH_LINK_INCOMPLETE |
| allocation history·수치 형식이 손상됐다 | UNKNOWN | DISPATCH_HISTORY_INCONSISTENT |
| Dispatch가 asOf 뒤에 발생했다 | UNKNOWN | DISPATCH_NOT_YET_OBSERVED |
| 읽을 수 있지만 item·Work·customer·line·cargo·LOT·root·단위·목적지·구간·시각이 모순이다 | DENIED | DISPATCH_SCOPE_MISMATCH |
| 실제 Dispatch와 위 범위가 모두 맞는다 | ALLOWED | PRIOR_DISPATCH_MATCHED |

ALLOWED는 이 관찰의 prior Dispatch 전제가 확인됐다는 뜻이다.
증거의 canonical 확정, 현재 SELL/WAREHOUSE 실행 권한, 목표 이행을
보장하지 않는다. 기존 `saleExecutionAuthorized=false` 상수와 전체
item의 `normalSaleAuthorization`을 oracle 근거로 쓰지 않는다.

`claim-nopriordispatch/no-prior-dispatch-24`의 원래 문자열 DENIED는
위 관찰별 `status`와 대조한다. typed UUID를 넣은 실제 `recordDelivery`
confirmation은 `HELD/DELIVERY_UNMATCHED`를 반환해야 하며, segment·
movement·배분·인도·기여·assessment는 전후 같아야 한다. claim 접수의
물리 기여 0과 OPEN 대조 책임은 그대로다. READ만 남겨도 prerequisite를
읽을 수 있고 RECORD를 잃은 명령은 FORBIDDEN이어야 한다.

DispatchCargoPort의 실제 구현은 quality/evidence를 의존한다.
SalesQueries의 eager constructor 주입은 EvidenceRepository와 cycle을
만들어 최초 전체 context/HTTP 기동에 실패했다. 포트 인자에 `@Lazy`를
붙여 초기 bean graph의 cycle을 끊고, 실제 조회 시 같은 저장 포트를
사용한다. 초기 실패와 수정 후 전체 context 검사를 모두 보존한다.

## 검증과 실행 증거

이 문서의 집중 JUnit/HTTP/SQL 결과는 시나리오 profile의 coverage PASS를
대신하지 않는다. `./verify scenarios --actual`은 실행하지 않는다.
최종 source가 clean인 상태의 집중 실행과 증거 경로를 후속 기록한다.

실행 command는 아래와 같다. 자체 JDK21과 자체 checkout에서 설치한
CDS dependency·컴파일 산출물을 사용한다.

```sh
python3 -m unittest discover -s verification/cases/T17 -p 'test_*.py'
./verify validate verification/cases/T17/case.json
./mvnw -B -ntp -f verification/harness/pom.xml \
  -Dtest=T17ReadQuantityAssertionTest,T17ConditionOracleAssertionTest test
./mvnw -B -ntp -f backend/pom.xml \
  -Dtest=PriorDispatchReadTest,PriorDispatchReadPostgresTest test
python3 verification/cases/T17/run_read_quantity_pg_test.py
```

`JAVA_HOME`과 `PATH`는
`/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home`을,
`DOCKER_HOST`는 로컬 OrbStack socket을 명시한다. runner는 disposable PG,
loopback Spring HTTP, ephemeral 인증 key와 blob을 만들고 자신의 자원만
정리한다. 인증 원문은 증거에 남기지 않는다.

초기 실패는 `.local/t17-read-contract/backend-focused-initial.log`와
`.local/t17-source-pg-a03a28d3-8a60-4e3c-9b34-475447360e80/`에 남긴다.
후자의 `cleanup.json`은 server/container/key/blob 정리를 확인한다.
`run_read_quantity_pg_test.py`는 실제 runner의 JUnit/XML·요청·응답·
SQL artifact에 더해 commit, 전후 clean/source hash, fixture hash,
JDK/Maven/Node/CDS version과 cleanup을 결합한다.

root는 통합된 현재 source에서 위 신규 집중 검사와 전체 backend를
새로 실행하고, Haiku native Workflow로 원래 시나리오 전체를 검증한다.
root의 RESPONSE_SCOPE·policy·observer 변경은 이 실행의 인수 범위가
아니다. Task/S5/S6, 전체 시나리오, 실모델·규제·BTP 인수는 닫지 않는다.
