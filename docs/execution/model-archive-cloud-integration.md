# MODEL 입력과 공개 조회 목록의 cloud 결합 검증이다

2026-10-24에 `3de8cc99`에서 재개했다. 이 수정은 최신 공개 QUERY
catalog와 그 파생 입력을 정합하는 범위다. 제품 handler, DB schema,
기존 업무 fixture와 업무 oracle은 변경하지 않았다. Task, 현재 Step,
S5와 S6는 OPEN이다.

사용자가 이 대화의 cloud 작업에 허용한 `GPT-6-Luna` worker 두 개를
`xhigh`로 실행했다. 실제 T3 `codex_2` 또는 Haiku dynamic Workflow로
표시하지 않는다. 별도 API 결제, 업무 model 호출, push와 배포는 없었다.

## 확인한 실패와 통합한 수정이다

- 원 archive의 M01–M60 입력 60개는 이전 capability catalog SHA를
  authority와 template 두 곳에 고정했다. 현재 catalog에서 이 입력은
  DB 접근 전에 거부된다. 정적 hash 대조와 archive 회귀의 RED로
  확인했으며 실제 DB 설치 실패를 실행한 것으로 표시하지 않는다.
- `36ebefd7`에서 새 catalog에 맞춰 두 pin만 갱신했다. 원본 218개
  artifact, 입력 record 수와 fixture hash, generated source 1,476개의
  경로와 byte를 보존했다. archive-first 생성·포장은 두 번 동일한
  archive와 index를 만들었다. 과거 PG proof는 그대로 보존했다.
- 별도로 untouched `3de8cc99`의 기존 generator 검사를 실행했다.
  MCP suite는 1 PASS/2 FAIL, case suite는 5 PASS/1 FAIL이었다.
  새 QUERY 17개가 T20/C3/V4의 공개 도구 기대 목록에 반영되지 않았다.
- `8759fd2a`에서 36개 discovery 목록과 해당 파생 hash만 갱신했다.
  원본 업무 assertion, actor/grant, fixture, 양과 시계는 그대로다.
  clone에 없는 worker commit에 의존하던 보존 test는 실제 통합 history의
  114→120→137 단계를 확인하도록 고쳤다. 정확한 역복원과 negative
  mutation 검사로 보호 범위를 유지했다.

새 archive SHA256은 다음과 같다.

`32d5c2df808ad89892dd7d7fe2e3c681e212c0f9176402be44b215602dda1519`

정합 후 입력은 새 정적 준비물이다. 기존 `proof-index.json`과
`focused-evidence.tar.gz`의 과거 실제 PG 설치 결과가 새 입력에 대한
실행 증거가 되지는 않는다. 16개 미완료 mapping과 전체 model 인수의
미실행 상태도 이 수정으로 닫지 않았다.

## 통합 checkout의 실제 검사다

`8759fd2a10529991e3ceb3811b0d86e03ea156d2`에서 preparation의 Python
asset 13개를 직접 실행했고 모두 exit 0이었다. 같은 checkout의 별도
public inventory preservation suite도 6개 모두 PASS였다.
실제 toolchain은 Linux amd64, OpenJDK 21.0.12.1, Maven 3.9.16,
Python 3.12.14다. 과거 macOS 실행 환경과 구별한다.

asset에는 archive 회귀 6개, MCP generator 3개, case generator 6개와
catalog, layer, vocabulary, request, observation binding 검사가 포함된다.
실행 전후 tracked 파일 4,867개의 SHA를 대조해 drift 0을 확인했다.
이 수를 고유 업무 scenario 또는 assertion coverage로 합산하지 않는다.

`PreparationAssetChecks.java`는 JDK compiler의 parse 단계도 통과했다.
이는 문법 검사만이며 dependency resolution, type check 또는 전체
Java compile 성공이 아니다. `git diff --check`는 exit 0이다.

정확한 argv, exit, 소요 시간, source commit과 원로그 SHA는
`.local/model-archive-integration/combined-checks.json`에 보존했다.
같은 디렉터리의 `archive-worker/`, `query-worker/`에 각 worker의
RED·재현·역복원 로그를 복사했고 기존 원본도 보존했다.

## 미실행 경계와 다음 검증이다

focused `ModelFixtureInstallerTest,NativeProductIntegrationTest`의 첫
Maven 시도는 기본 cache 경로 권한 때문에 exit 1이었다. workspace
소유 cache로 바로잡은 다음 시도도 Maven Central DNS 때문에
`cucumber-bom:8.0.4`, `junit-bom:6.1.2`를 받지 못해 test 시작 전에
exit 1이었다. 제품 실패가 아니라 환경 차단이며 두 원로그를 보존했다.
dependency 버전을 낮추거나 다른 네트워크 경로로 우회하지 않았다.

Java focused tests, Java `./verify prepare`, backend 전체, 실제 PG 설치,
업무 scenario, 실제 client/model과 S6/BTP는 이번 cloud 실행에서
NOT_RUN이다. Python asset의 직접 성공을 전체 prepare PASS로 쓰지
않는다. 다음에는 승인된 환경에서 고정 dependency를 확보하고 이 두
focused class와 전체 prepare를 실행한 뒤, disposable DB의 실제 설치와
남은 mapping을 검증한다. 새 실제 증거 전에는 과거 gate를 승격하지 않는다.
