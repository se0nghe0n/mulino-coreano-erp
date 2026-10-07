# S2 실제 adapter의 연결 계약

S1 adapter는 조회만 전달했다. S2 adapter는 서버의 공통 command
pipeline에 command와 record를 전달하고 실제 HTTP 응답을 보존한다.
이는 업무 구현이나 전체 T/C/V/E 인수를 대신하지 않는다.

- `ActualAcceptanceDriver.java`는 COMMAND를
  `/api/ontology/commands/{capabilityId}`, RECORD를
  `/api/ontology/records/{capabilityId}`에 POST한다. JWT는 ephemeral
  RS256 key로 서명하고 서버가 현재 신원·권한을 다시 검사한다.
- 요청 JSON은 그대로 전달한다. 조회의 `scope.caseId`만 harness
  metadata로 분리한다. artifact는 원래 `request`, 실제 `wireRequest`,
  HTTP status와 실제 응답을 함께 보존한다. 다른 field를 제거하지 않는다.
- `start`는 실제 HTTP 요청을 비동기로 제출한다. `await`는 해당 요청의
  최종 HTTP 응답을 기다리고 같은 opaque handle을 반환한다. 제출 ACK나
  timeout을 완료로 바꾸지 않는다. business 거부 응답도 그대로 남긴다.
- `ActualFixtureBindings.java`는 명시적인 isolated manifest가 선택된
  경우에만 fixture의 abstract issuer/audience를 deployed URL에 연결한다.
  배포 설정과 manifest가 다르면 거부한다. grant·role·scope·시간·원래
  fixture hash는 보존하고 binding file hash를 별도 기록한다.
- `FixtureInstaller.java`는 완전한 authored content를 가진
  `DefinitionVersion`과 `PolicyVersion`을 실제 PostgreSQL에 설치한다.
  UUID/organization 참조만 설치한 대상에 맞춘다. hash는 실제 content에서
  계산한다. 미지원 alias·baseline·evidence·책임은 생략하지 않고 거부한다.

## Snapshot 경계

현재 API snapshot은 PostgreSQL MVCC snapshot ID가 아니라
`org|actorUUID|asOf|knownAt|normalized(scope)|normalized(sharedWorld)`의
SHA-256이다. 정규화는 map key 정렬, list 순서 유지, decimal 문자열화다.
별도 JDBC transaction은 `REPEATABLE_READ`와 `pg_current_snapshot()`을
기록한다. 이 두 ID를 복사해 같은 snapshot으로 주장하지 않는다.

normative snapshotRef를 처리하려면 같은 authoritative row에서 현재
인가로 읽을 수 있는 shared world를 별도 SQL로 구성하고 동일 projection
hash를 독립 계산해야 한다. API hash를 DB 결과에 그대로 붙이는 방식은
허용하지 않는다. 그 mapping이 없는 요청은 `NOT_IMPLEMENTED`다.

## 미완료 범위

고정 server clock, host/process/barrier, worker lease 제어는 실제 runtime
adapter 계약이 필요하다. 미지원 control은 실행 성공으로 표시하지 않는다.
S3/S4 table source가 포함된 normative observation과 underspecified
fixture의 전체 설치도 미지원이다. 41 case·789 subcase·20,473 assertion은
줄이지 않고 default RED 경로를 유지한다. 새 bounded test가 통과해도
전체 case gate는 false다.

## 고정 시계와 bounded native runner

`NativeS2WorkMain`은 authored physical/definition fixture를 설치하고
verification profile의 서버 시계 ACK를 확인한다. 같은 key의 draft 요청
두 개를 실제 HTTP로 함께 제출한다. 응답의 work identity와 별도 JDBC
Works/GoalReferences의 한 행을 대조한 뒤 payload 변경 conflict와 draft
취소의 CLOSED/CANCELLED 상태를 확인한다. 전체 계획의 사례를 축소한
대체본이 아니라 별도 native probe다. 실행 결과와 assertion 수는 실제
receipt에서만 확인한다.

`verification/actual/s2/build.py`는 깨끗한 commit에서 backend JAR와
harness를 build한다. source commit·모든 tracked file hash·실제 Maven
command·JAR·actual adapter class hash를 custody file에 기록한다. 이
script는 coordinator가 부여한 Maven slot에서 실행한다. 이후
`./verify actual-s2 <새 evidence directory>`는 이 custody와 현재 source,
복사한 JAR, harness class를 검증한다. 고정 서버 시각은
`2026-10-07T09:00:00Z`이고 JWT 유효기간은 실제 transport 시계로 짧게
서명한다. 현재 authority clock과 query asOf는 별개다.

runner는 자신이 만든 container·backend PID·임시 key/blob directory만
정리한다. 실행 receipt에는 원래 fixture/binding·code/build·실제 artifact
hash와 cleanup 결과를 남긴다. JWT·credential·환경 전체 dump는 남기지
않는다. 서버와 clock·공통 pipeline이 통합되지 않았으면 성공을 추정하지
않고 NOT_RUN 또는 실제 실패를 기록한다.
