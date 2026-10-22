# Native model integration 재개 기록

실제 제품 MCP 전송과 독립 PostgreSQL capture를 연결했다. 전체
model-binding adapter는 아직 인수할 수 없다. 아래 공백을 기대값이나
빈 관찰로 채우지 않고 `NOT_IMPLEMENTED`로 반환한다. 실제 model 호출은
0회이고 incremental API cost cap은 0이다.

## 기준선과 범위

- parent는 `bbdf3f9be8330622465025d0d03f9eb420d31e39`다.
- branch는 `feat/native-client-integration`이다.
- `ActualAcceptanceDriver`, `ScenarioFixtureInstaller`, backend query,
  T01/V5/T26 및 native model config는 변경하지 않았다.
- 재위임, push, PR, 외부 메시지, BTP 실행은 하지 않았다.
- 이번 산출물은 native Codex를 실행하지 않는다. coordinator가
  prerequisite를 해결한 뒤 실제 model 인수를 별도로 수행해야 한다.

## 연결한 동작

`NativeProductIntegration`을 `NativeClientIntegration` service로 등록했다.
기존 strict fixture 형식은 public `ActualAcceptanceDriver`로 설치한다.
새 설치 시 이전 scope를 먼저 폐기한다. M01–M60의 UUID alias 및
`baseline.domainFacts` 형식은 전체 변환이 없으므로 쓰기 전에 거부한다.
실패 설치를 부분 성공으로 보고하지 않는다.

`NativeMcpTransport`는 실제 `/mcp/ontology`에 RS256 인증으로 전송한다.
`tools/list`의 public input schema를 그대로 native dynamic tools로
변환한다. `tools/call` arguments와 raw RPC 응답을 보존하고 text content와
`isError`를 native tool 응답으로 연결한다. 요청/응답 RPC ID, 시간,
scope, 제품/adapter source hash 및 artifact hash/bytes를 기록한다.
credential과 private key는 artifact에 쓰지 않는다. JWT를 보냈다는 사실을
서버가 선택한 actor/grant 관찰로 승격하지 않는다.

`NativePhysicalObserver`는 별도의 read-only `REPEATABLE_READ` transaction과
`pg_current_snapshot()`을 사용한다. 현재 segments, allocations, movements,
grants, outbox의 5개 물리 테이블을 읽는다. 각 query의 실제 SQL/parameters와
전체 조회 행, table/column/rowId/value/unit cell을 남긴다. 이 capture는
`mappingComplete=false`인 진단이다. 전체 effect/obligation mapper가 없으므로
미지원 관찰을 빈 배열이나 수량 0으로 생성하지 않는다.

`NativeFinalResponseObserver`는 native capture와 받은 evidence의 동일성을
확인하고 마지막 string 원문과 source hash를 보존한다. 독립 의미 extractor가
없으므로 completion, quantity 또는 책임 해소를 추정하지 않는다.
contract double에서 만든 capture는 `CAPTURED_CONTRACT_SELFTEST`로 구별한다.

`ModelBindingRunner`의 UAT clarification은 string에 `.clarification`을
조회하지 않는다. 이미 검증된 독립 final-response observation의
`claims.execution`으로 확인질문 필요 여부를 검사한다. 현재 schema에는
독립 관찰된 missing-slot 집합이 없으므로 해당 UAT turn은 `NOT_RUN`이다.
SIT typed response 검사는 유지한다. 원래 답변 string은 변경하지 않는다.

`NativeDeterministicGate`는 schema/contracts/scenarios/recovery/mcp/skills의
정확히 한 개씩 존재하는 PASS를 요구한다. 현재 commit, clean evidence 및
기존 coverage validator의 source 재조립 검증도 요구한다. `PREPARED`,
중복 profile, 부분 PASS, stale evidence는 model 실행 허가가 아니다.
독립 interface가 없는 동안에는 prerequisite가 통과해도 session을 열지
않는다. 직접 `ActualClientPort` 호출도 같은 gate를 거친다.

## 실제 검사

Java `21.0.12.1`, Maven `3.9.16`으로 실행했다.

```sh
./mvnw -B -ntp -f verification/harness/pom.xml \
  -Dtest=NativeProductIntegrationTest,NativeClientContractTest,ModelBindingResponseEvidenceTest,ModelBindingEvaluatorTest,ModelBindingContractTest \
  test
verification/model-binding/actual-integration transport-sit
verification/model-binding/actual-integration readiness
```

- focused JUnit 81개가 실패/오류/skip 없이 통과했다. 이 중 신규 5개는
  명시적 contract doubles다. 테스트 결과는 실제 model 인수가 아니다.
- `transport-sit`는 `npm ci --prefix backend`, backend Maven package,
  digest 고정 PostgreSQL Docker, 복사한 backend JAR를 사용했다. 14개
  assertion을 통과했고 exit 0이다. 생성한 container와 임시 key를 정리했다.
- 실제 public tool discovery, 재고 `heldQuantity=100`, native text tool
  응답 변환, 미지원 context의 `TYPE_INVALID`, 미등록 주체의 `FORBIDDEN`,
  독립 전후 snapshot을 관찰했다. 두 snapshot에서 조회한 5개 테이블의
  행이 같았다. 이는 관찰하지 않은 전체 업무 effect 0을 뜻하지 않는다.
- 실제 integration driver를 통한 strict fixture 설치, 원본 MCP 진단
  artifact, 불완전 observer의 `NOT_IMPLEMENTED`, M01 fixture 사전 거부를
  검사했다. fixture 값은 assertion에서만 대조했다.
- `readiness`는 `DETERMINISTIC_EVIDENCE_MISSING`, `NOT_RUN`, exit 2다.
  이 isolated worktree에는 parent의 target manifest가 복사되지 않았다.
  FAIL profile 차단은 contract test에서도 별도로 확인했다.
- `git diff --check`와 실행 스크립트 Python 구문 검사가 통과했다.

최종 SIT run은
`verification/harness/target/native-integration-runs/d0e1fae9-41b1-43d2-baed-30787a2b5a73/`
다. `execution.json`의 SHA-256은
`fdebf0be5402e2480e5d158bf0ba1632ea8a6934dfbc366344965fbf48de0605`,
`transport-sit.json`은
`6a099d5d43461d1ae3c67a7805e219857d24766a77fa33a3d0f5074e8a49bb38`다.
실행 전후 source hash가 동일한지 검사했고 다른 source로 바뀌면 PASS를
거부한다. report의 `inputs`에 backend/harness/contracts의 실제 hash와
bytes가 있다. 각 MCP/DB artifact는 별도 scope와 source에 연결된다.

최종 readiness run은
`verification/harness/target/native-integration-runs/7ad10835-c046-47ab-8be7-c17783516995/`
다. `readiness.json` SHA-256은
`299a50781677226c16e3b3206e31d106e58854e221fc5cbce36f39f519aea6e8`이다.
이 경로들은 로컬 실행 증거이며 Git에 올린 runtime PASS가 아니다.

## 남은 interface와 필요한 후속 patch

1. **Model fixture 전체 설치.** `baseline.domainFacts`, `principalFacts`,
   `grantFacts`와 UUID alias를 strict 신규 helper에서 설치해야 한다.
   현재 `ScenarioFixtureInstaller`의 typed alias/baseline 계약과 다르다.
   definition, 물리 사실, 기존 인간 책임, current grant를 빠짐없이
   설치해야 하며 기존 fixture를 축소하거나 `partialFixtures`로 바꾸면
   안 된다. 이번 작업은 변환을 구현했다고 주장하지 않는다.
2. **실제 허용 context endpoint.** `QueryRequests.FIELDS`와
   `ApplicationQueries`가 현재 요청된 `view`와 installation/turn scope를
   지원하지 않는다. 실제 실행에서도 `TYPE_INVALID`다. backend owner가
   actor 허용 context 계약을 제공한 뒤 해당 응답만 prompt에 연결해야 한다.
3. **서버 인증 capture.** `OntologyMcp` 응답에는 서버가 해석한
   issuer/subject/audience/organization/선택 grant의 독립 capture가 없다.
   서명한 JWT나 요청 actor를 복사해 `captured-api.schema.json`의
   `authenticatedActor`로 채우면 안 된다. 서버 관찰 hook이 필요하다.
4. **Snapshot 계약 충돌.** 공통 `acceptance-observation.schema.json`은
   `rawRows` object를, `physical-columns.schema.json`은 array를 요구한다.
   현재 runner는 같은 값에 두 검증을 모두 적용한다. owner가 공통 원행
   envelope와 physical cell list의 명시적 위치를 분리하고 두 검증을
   유지해야 한다. schema를 느슨하게 바꾸는 수정은 하지 않았다.
5. **Revision과 MVCC 분리.** `ApplicationQueries.snapshot`은 actor/시점/
   scope/projection의 SHA-256이다. runner는 이것과 DB snapshot ID가 같아야
   한다고 검사한다. 실제 PostgreSQL token `828:828:`은 API hash와 다르다.
   독립 projection revision 재계산 및 별도 MVCC token 계약이 필요하다.
6. **전체 effect/obligation 관찰.** immutable effect IDs와 class/operation,
   installation/turn 귀속, canonical physical scope, obligation owner와
   nextAction/nextCheck를 연결할 완전한 mapper가 필요하다. 현재 물리
   column 진단을 acceptance observer로 사용하면 안 된다.
7. **독립 host/의미 extractor.** 실제 process/native event/wire와 전체
   final response에 연결된 `HostObservationValidator` 증거가 필요하다.
   clarification missing-slot 집합도 독립 관찰 계약이 필요하다. 모델의
   self-label, 문자열 유사도 또는 oracle로 이를 대신하지 않는다.

이 공백을 해결하고 현재 deterministic gate가 통과한 뒤 coordinator가
승인된 기존 구독 인증으로 actual model 인수를 진행한다. 이번 구현의
`transport-sit`/`readiness` 실행 경로는 model 호출을 제공하지 않는다.
