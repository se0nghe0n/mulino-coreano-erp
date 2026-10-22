# 2026-10-22 worker scenario adapter 연결

기준선은 `f3209558`이다. 작업 범위는 `ActualAcceptanceDriver`,
`ActualWorkerTransportTest`, 이 기록이다. backend·case·fixture·host
process는 변경하지 않는다.

`route=worker`는 인증된 `POST /api/runtime/worker/commands`를 호출한다.
기존 bare intent는 `{ "intent": <request> }`로 감싼다. claim이 없으면
그대로 생략하며 backend가 FORBIDDEN으로 거부해야 한다. adapter는
권한·claim 유효성·효과를 판정하거나 성공을 합성하지 않는다.

명시적인 `{ "intent": ..., "claim": { "claimId": ...,
"fencingToken": ... } }` 입력은 envelope와 token의 JSON 타입을
보존한다. malformed claim과 추가 필드도 수선하지 않고 서버로 보낸다.
기존 credential binding·authenticationTest·barrier header와 HTTP
응답 기록을 재사용한다. receipt는 원래 request, 실제 wireRequest,
endpoint, HTTP status, 원래 response, credential hash를 보존한다.
claim 발급이나 enqueue를 암묵적으로 수행하지 않는다.

## 결합 의존성과 미실행 범위

동시 backend 작업의 `WorkerCommandsRest`를 읽어 endpoint와 envelope를
대조했다. backend의 claim resolver와 현재 actor 검증, lease fence,
효과 0은 coordinator가 통합 backend에서 검증해야 한다. 이 변경만
적용한 backend에 endpoint가 없으면 기존처럼 NOT_IMPLEMENTED다.

현재 worker case들은 bare intent를 사용한다. 권한 있는 actor라도
claim이 없으면 성공할 수 없다. 정상 fixture는 실제 claim 발급 결과의
claimId와 fencingToken을 명시적으로 연결해야 한다. 직접 driver
호출은 위 envelope를 받지만 case preparation의 intent schema와
fixture binding 확장은 이번 소유 범위 밖이다. 기존 정상 case를
PASS로 주장하지 않는다. V7의 enqueue/barrier 실행 맥락도 lease 발급
경로를 대신하지 않으며 미지원 barrier는 기존 사전 gate가 거부한다.

## 검증

- Java 21에서 `./mvnw -q -f verification/harness/pom.xml
  -Dtest=ActualWorkerTransportTest,ActualS2TransportTest test`: exit 0,
  5 tests, failures/errors 0이다.
- Java 21에서 `./mvnw -q -f verification/harness/pom.xml test`:
  exit 0, 539 tests, failures/errors/skipped 0이다.
- `git diff --check`: exit 0이다.
- 새 4개 test는 loopback HTTP server에서 실제 경로·POST·body·인증
  변형·HTTP 403/200 응답 보존·404 미지원 처리를 확인한다.
- test server의 APPLIED는 응답 전달 검증용이다. 유효 claim 발급,
  실제 인가나 DB 효과 검증이 아니다. 실제 제품 인수는 NOT_RUN이다.
- shell의 `mvn`은 설치 경로에 없어 exit 127이었다. 저장소 Maven
  wrapper로 실행해 해결했다.

모델 catalog에서 codex의 GPT-6 Astra와 Ultrafast option을 확인했다.
현재 child의 실제 tier는 catalog가 노출하지 않으므로 상위 dispatch의
명시적 `serviceTier=ultrafast` 기록을 실행 근거로 확인해야 한다.
