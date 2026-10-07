# 실제 모델 corpus의 독립 binding

60개 발화 사례의 기대 의미를 prompt에 넣으면 모델 평가가 답안 재현
검사로 바뀐다. 이 binding은 닫힌 corpus를 evaluator 전용 원문으로
유지하고 실제 입력·도구·DB·host 증거와 대조한다. 기준선은
`step2-b2`의 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다.

`registry.json`은 M01–M60, 73 turns, 공통 assertions221개와 semantic
paths154개를 등록한다. `semantic-paths.json`에는 negative 경로의 추가
4개 path까지 총158개가 있다. T/C/V/E41 registry에 M ID를 넣지 않는다.
각 `cases/Mxx/binding.json`의 `commonAssertions`와 `oracleAssertions`는
기대값 복사본 대신 corpus JSON pointer와 hash를 참조한다. 개별
assertionId·semanticPath·원문 pointer를 전수 검사한다.

SIT/UAT는 같은 `scenario.feature`, fixture, oracle를 쓴다. 60개 실제
한국어 Gherkin scenario는 설치, 각 turn의 context 조회·전 snapshot·
Agent·후 snapshot·판정과 공통 assertion221개를 명시한다. 13개
multi-turn case는 하나의 isolated installation에서 순차 실행한다.
SIT는 `AgentRunner.Scripted`, UAT는 `AgentRunner.Actual`로 교체한다.
QUERY/RECORD/COMMAND는 사용자 의미이며 public capability의 실행
분류와 구별한다. M19의 `linkRelation`만 `recordRelation`으로 연결한다.
이 mapping으로 배분·출고·권한 변경을 structural relation에 숨길 수 없다.

## 입력과 fixture

`generate.py`는 object를 deep merge하고 list를 교체한다. 각 driver
fixture는 독립 조직·주체·구체 grant·시계·버전·원천·기존 인간 책임과
case의 기존 업무 사실을 설치한다. corpus의 oracle/expectedIntent는
설치하지 않는다. 계산된 인가 cache인 currentWriteAuthorization도
설치하지 않는다. 실제 설치가 반환한 aliasMap/installationId/snapshot
revision만 실행에 사용한다. fixture UUID는 실제 서버 ID의 기대값이
아니며 설치 결과의 ID로 대체한다.

M47의 `command-actor`와 `read-probe-actor`는 issuer/subject/audience/
조직이 같은 principal이다. 첫 profile은 철회된 WRITE grant를,
둘째는 별도 유효 READ-only grant를 사용한다. grant revision/revokedAt/
source/target scope는 fixture에 명시된다. READ profile은 command 권한을
복원하지 않으며 실제 tool call의 profile/principal/grant가 대조된다.

context 조회는 실제 인증된 `getObject`의
`view=ACTOR_PERMITTED_BUSINESS_CONTEXT` 요청으로 수행한다. 구현 시
adapter는 허용된 business context를 실제 API에서 반환해야 한다.
fixture 전체, expectedIntent/oracle, title/requirementRefs를 prompt에
직렬화하지 않는다. actual action에는 raw utterance, 실제 허용 context,
이전 raw user turns와 실제 이전 client result만 전달한다. typed intent는
scripted runner에만 준다. 추가 slot은 범위·권한·효과를 바꿀 수 없다.

## 관찰 adapter 계약

이 문서는 구현된 제품 API/DDL을 주장하지 않는다. Step3의 실제 adapter가
아래 mapping을 실제 API·물리 column·원장으로 연결하기 전까지 실행은
NOT_RUN이다. AcceptanceDriver는 업무 규칙이나 목표 판정을 계산하지
않는다. 임의의 기대값 응답이나 fake product를 adapter로 제공하지 않는다.

- response path는 실제 인증 API 결과의 RFC6901 pointer로 읽는다.
- state path는 독립 observer의 rawRows를 읽는다. 각 row에는 dataset,
  attribute, 실제 table/column/rowId, value와 필요한 unit이 있다.
  이는 read-only 물리 column mapping이다. 서버 API projection이나
  oracle 이름의 Boolean을 row로 복사하지 않는다. scalar는 정확히
  한 row를 요구한다. 전체 원 행과 actual SQL/parameters/mappingVersion,
  snapshot token/isolation/artifact를 공유 observation schema로 검증한다.
- 수령 canonical occurrence와 회수·폐기 scope는 physicalScopeId로
  중복 제거한 수량 합계로 검사한다. 동일 scope 회수25와 폐기25는25다.
  서로 다른 실물 ID, 같은 ID의 상충 수량·단위는 별도로 실패한다.
  문서 수·현재 의무·follow-up은 원 행의 정확한 count로 검사한다.
- effect는 설치 scope의 immutable effect IDs 전후 차이다. classes는
  복수 의미 분류이고 operation은 실제 발생 행동이다. 각 새 occurrence를
  한 번만 센다. businessEffectCount는 audit 두 종류를 제외한 occurrence
  수이며 미리 계산된 서버 Boolean을 읽지 않는다. unknown class도
  금지한다. numeric measurement는 실제 단위와 같이 합산한다.
- obligations는 실제 원 행의 kind/scope/owner/status/nextAction/nextCheck/
  quantity를 대조한다. 기존 duty/owner 변경에는 해당 duty ID의 허용된
  obligation effect가 필요하다. 알림·모델 종료는 책임 종료가 아니다.

`physical-columns.schema.json`, `observer-rows.schema.json`은 이 별도
mapping의 row 계약이다. `mappingVersion=model-binding-v1`을 요구한다.
actual adapter는 아직 없고 actual sourceQuery/table mapping도 실행되지
않았다. adapter 미지원·관찰 누락은 정상 수량0이나 PASS가 아니다.

## negative 경로와 host

11 negative turns의 SIT는 `sitDirectCommand`의 실제 command 거부 outcome과
정확한 server error를 검사한다. UAT는 모델의 self label을 무시하고 실제
host transcript와 실제 context 조회의 authenticated READ/COMMAND wire,
constraint facts,
독립 전후 snapshot/effect/의무로 completion path를 고른다. 읽은 blocking
fact는 요청 capability를 막는 실제 근거이고 독립 persisted row와 같아야
한다. 이미 허용 context 조회에서 충분한 근거를 받은 모델에는 추가 조회를
강제하지 않는다. 모델 설명만으로 preflight PASS를 만들지 않는다.
tool 순서는 답안으로
고정하지 않는다. host evidence는 `HostObservationValidator`의 실제 process,
extractor hash·bytes·scope 계약을 재사용한다. 실제 agent transcript와 각
wire artifact도 연결하고 captured selftest를 UAT로 인정하지 않는다.

공통 oracle와 선택한 path만 합성한다. M47 preflight는 기존 EXECUTABLE
배분·OPEN DELIVERY_REMAINING/owner를 보존하고 business effect0을 요구한다.
SERVER_REJECTION/SIT는 SUSPENDED 전이1개와 새 REAUTHORIZE 의무1개 및
기존 인간 책임 보존을 요구한다. READ_AUDIT는 명시된 현재 READ grant의
actor/조직/target scope 안에서만 허용한다. 조회가 전이를 만든다고
가정하지 않는다. QUERY/NEEDS_INPUT의 unlisted write도 전부 금지한다.

## 실행과 증거

저장소 루트에서 실행한다. root verify/pom/공통 runner는 변경하지 않았다.

```bash
verification/model-binding/run selftest
verification/model-binding/run prepare
verification/model-binding/run red
verification/model-binding/run gherkin-red
verification/model-binding/run red UAT
verification/model-binding/run gherkin-red UAT
```

selftest와 preparation의 exit0은 바인딩·assertion 자체의 검증이다.
준비 성공은 coverage schema와 같은 `preparationStatus=PREPARED`로
기록한다. `PASS`는 실제 case/runtime 판정에 쓰며 준비 enum을 대체하지
않는다. 준비가 끝난 RED 보고서도 `preparationStatus=PREPARED`와
`runtimeStatus=NOT_RUN`을 함께 유지한다.
`red`의 exit2는 실제 adapter 부재로 인한 제품 NOT_RUN이다. gherkin-red는
60개 scenario를 discovery/실행하며 NOT_IMPLEMENTED assertion으로
의도적으로 exit1이다. 구문·환경 오류를 제품 RED로 세지 않는다.

보고서는 `verification/harness/target/evidence/`의
`model-binding-preparation.json`, `model-binding-red.json`,
`model-binding-gherkin-red.json`이다. 같은 UAT baseline의 별도 보고서는
`model-binding-red-uat.json`, `model-binding-gherkin-red-uat.json`이다. inputArtifacts에 corpus/registry,
60 bindings/fixtures/features, mapping/schema/runner source의 실제
path/SHA-256/sizeBytes를 기록한다. assertionResults는 binding의 정확한
assertionId와 semanticPath를 사용한다. raw Maven log의 trailing space는
실제 출력 byte/hash를 보존하기 위해 evidence/*.log에만 whitespace
예외를 적용한다. 코드·schema·Gherkin에는 이 예외를 적용하지 않는다. runtime-manifest.json은 coordinator가
실제 report path/hash로 연결하며 이 Subtask가 만들지 않는다.

실제 client/model/prompt/skill/server 설정과 승인된 비용 상한은 아직
없다. 기본 UatExecutionGate는 호출 전에 거부한다. UAT Gherkin은 같은 파일을
`model.binding.profile=UAT`로 선택한다. 별도 실제
ModelBindingRuntimeProvider가 설치되면 같은 public port에 연결한다. 이후 별도 승인된
runtime만 R8/config/approval artifact를 제공할 수 있다. 이번 납품의
actual model calls는0, attempts는 빈 배열, usage/cost는 null과 누락
이유이며 status는 NOT_RUN이다. planned repeats3/180 attempts는 실행
증거가 아니다. 95% clear-structure/부당 실행0은 R8 수용치 제안이며
사업 SLA나 실제 모델 품질 주장으로 바꾸지 않는다.

## 마지막 답변과 실제 API 결과의 분리

client의 마지막 답변은 API 응답 원장이 아니다. `AUTHENTICATED_API`
assertion은 `apiAssertionSources[{semanticPath,capturedCallId}]`로 지정한
실제 call의 raw MCP request/response에서만 읽는다. UAT의 call 목록과
source 지정은 hash/bytes가 확인된 `agentTranscriptRef`에서 읽고, SIT는
실제 transport가 반환한 `data.capturedApiCalls`와
`data.apiAssertionSources`를 쓴다. 각 call에는 `callId`, capability,
인증 profile/principal/grant, `wireArtifactRef`,
`businessResponsePointer`가 있다. pointer는 raw wire의 `/response/` 아래
실제 business response object를 가리킨다. adapter가 답안으로 만든
projection이나 client final answer를 이 object로 복사할 수 없다.

`captured-api.schema.json`의 wire에는 raw JSON-RPC request/response,
서버에서 관찰한 authenticatedActor, actionId, installation/turn scope,
requestSentAt/responseReceivedAt가 있다. request/response RPC ID,
capability, 인증 주체와 grant, 현재 action/scope를 대조한다. UAT tool wire는
실제 host invocation 시간 안에 있어야 하며 host extractor가 읽은
artifact의 path/hash/bytes와 연결된다. SIT 및 사전 context read는
기존 driver schema의 `provenance.independent=true`,
`provenance.source=ACTUAL_AUTHENTICATED_TRANSPORT`와
`data.capturedArtifacts[{path,sha256,sizeBytes}]`를 요구한다.
CAPTURED_CONTRACT_SELFTEST는 계약 반례에만 사용하고 제품 PASS로 세지 않는다.

QUERY는 이미 인증된 context read가 실제 API assertion의 원천이 될 수
있으며 같은 읽기를 다시 호출하도록 강제하지 않는다. executed command는
실제 matching command RPC가 필수다. M50 turn1은
동일 commandIdempotencyKey, stableRequestOwner와 전체 canonical payload의
실제 replay request, 실제 APPLIED/reusedCommittedResult 응답을 요구한다.
RPC ID는 업무 idempotency key와 구별한다. 실제 응답의
committedReceiptOccurrenceRef와 committedPhysicalScopeRef는 각각 독립
receiptOccurrence 원 행의 rowId와 physicalScopeId다. 동일 실물 scope와
원장 occurrence ID를 섞지 않는다. 원장에는 기존 key/owner column도
관찰되어야 한다. 실제 응답의 committedEffectRefs는 기존 key/owner에
속한 receipt/movement/outbox 업무 effect ID 집합과 정확히 같아야 하고,
전후 원장에 기존 occurrence/effect가 보존되어야 한다. 기존 수량60과
새 effect0만으로 replay가 실행됐다고 판정하지 않는다.

UAT의 모든 마지막 답변은 actual transcript의 `finalResponse`로 보존하며
StepResult.response와 같아야 한다. 별도
`data.finalResponseObservationRef`는 전체 마지막 답변의 의미를 독립
extractor가 관찰한 artifact다. schema는
`final-response-observation.schema.json`이다. sourceTranscriptRef,
sourceResponse 전체 값, extractor의 name/version/실제 command와 scope를
확인하고 shared host extractor의 실제 output에 있는
finalResponseObservation과 동일해야 한다. artifact와 원 transcript의
hash/bytes도 같은 host 계약으로 확인한다. 모델이 작성한 completion
self label을 semantic observation으로 인정하지 않는다.

독립 extractor는 claimed effects, business completion, residual human
responsibility 세 영역의 의미를 빠짐없이 관찰한다. 정확한 답변 문구를
요구하지 않는다. semantic assertions와 언급한 잔여 의무/owner/수량은
실제 API·독립 DB·effect delta와 대조한다. 모든 negative 경로에서
거짓 FULFILLED/CLOSED와 남은 인간 책임의 RELEASED를 금지한다. 합법적인
EVIDENCED_PREFLIGHT_STOP은 실제 차단 근거와 불변 snapshot을 유지하며
NOT_EXECUTED 의미를, SERVER_REJECTION은 실제 거부 RPC와 REJECTED 의미를
요구한다. preflight에도 APPLIED라는 최종 structured outcome을 허용하지
않는다. 정확한 server error는 SIT/서버 거부 경로에만 요구한다.

이는 의미 extractor나 제품 구현을 제공하는 변경이 아니다. 실제
read-only extractor가 전체 final response를 관찰해 위 증거를 생성하기
전에는 UAT PASS가 불가능하다. 제품 fake, 답안 seed, 실제 모델 호출은 없다.
perTurn에는 canonical selectedPathId와 같은 값의 selectedPath,
capturedApiCallIds, apiAssertionSources의 실제 wire/pointer,
finalResponseObservationRef를 기록한다. 이 API call IDs는 유료 provider
modelCalls의 callId와 다른 namespace이며 모델 usage/cost를 나타내지 않는다.


capture hash 목록은 별도 `capture-artifacts.schema.json`의 typed array로
검증한다. 공통 driver provenance의 additionalProperties=false는 유지한다.
사전 중단이나 business effect0에서 책임을 TRANSFERRED라고 주장할 수 없다.
실행된 이전을 주장하려면 실제 OBLIGATION_ASSIGNMENT effect의
obligationRef/newOwnerRef, 독립 현재 duty의 id/ownerRef, 답변에서 관찰된
인수자와 잔여 의무가 함께 같아야 한다. 기존 인간 책임이 남았는데 이전을
완료했다고 답하는 것도 거짓 완료다.
