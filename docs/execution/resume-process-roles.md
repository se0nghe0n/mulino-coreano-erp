# 단일 application process role 제어 복구

2026-10-22에 실행 중인 application의 `start`를 실패로 처리하던
host 계약을 수정했다. 2026-10-12 결정에 따라 첫 실제 start는
application을 기동하고, 후속 role start는 같은 instance를 확인한다.
fixture 설치가 application을 먼저 기동했다면 case의 첫 start도
기존 instance 연결이며 새로운 기동으로 보고하지 않는다.

## 범위와 기준선

- 기준선: `79a0cb6e9fe0e21a5296d4868b2b49d9c722a6e8`이다.
- branch: `fix/process-role-host-controls`다.
- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-cd6df958`다.
- 실행자는 GPT-6 Astra이며 위임·모델 전환을 하지 않았다.
  Ultrafast 요청은 받았으나 이 worker의 runtime 정보에는 tier가
  노출되지 않아 적용 여부를 별도로 확인하지 못했다.
- 수정 범위는 host process script, Java adapter·validator, host guide,
  집중 테스트와 이 기록이다. case와 production backend는 수정하지 않았다.
- 이 작업은 사용자 전체 구현 Task의 process 제어 Subtask다.
  전체 사용자 Step이나 S5/S6 gate를 닫는 증거가 아니다.

## 동작과 증거

`start`는 미기동 상태에서 `STARTED`, 기존 RUNNING instance에서는
`ATTACHED_EXISTING`을 반환한다. 후자는 전후 PID가 같고
`data.launched=false`다. 따라서 기존 instance 연결 때 verification
clock을 재적용하지 않는다. `restart`는 실행 중인 이전 PID를 종료하고
다른 PID로 시작하며 `RESTARTED`를 반환한다. `stop`은 application 전체를
종료하고 HTTP 응답도 종료됐는지 확인한다. 멈춘 application에 대한
stop/restart, PID 관찰 불일치, HTTP 미응답, 시작 실패는 FAILED다.

Java adapter의 동시 요청과 Python host 명령은 application별로
직렬화한다. Python은 PID file 옆 lock을 사용한다. 잘못된 이전 PID
관찰을 새 관찰로 바꾸어 숨기지 않고, 실제 after와 실패 이유를 남긴다.
명령 실패에도 terminal snapshot을 만들며 기존 scope/hash/provenance,
독립 extractor와 before/after 시간 경계 검증을 유지한다.

host의 role 확인은 scheduler 활성화 증거가 아니다. 자연 scheduler의
watcher 이전 비활성 상태, 관찰 창, 자연 제출·완료 주기 증거는 기존
별도 gate로 유지한다. scheduler 활성화 및 재시작 후 runtime profile
복원은 backend/driver 담당 변경과 통합한 뒤 확인해야 한다.

## 실행한 검증

아래 검증은 로컬 host 동작·계약 테스트다. HTTP process는 Python
표준 라이브러리 서버이며 ontology 업무 응답을 흉내 내지 않는다.
제품 업무 fixture PASS나 coverage receipt로 사용하지 않는다.

```sh
npm ci --prefix backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f verification/harness/pom.xml \
  -Dtest=BackendProcessControlsTest,HostObservationValidatorTest test
python3 -m unittest discover -s verification/actual/scenarios \
  -p test_process_host.py -v
git diff --check
```

- npm ci는 exit 0이다. 기존 dependency audit는 high 4개, critical
  4개를 보고했다. 의존성 수정은 범위 밖이라 실행하지 않았다.
- Java 21은 설치 경로 기준 `21.0.12.1`이다. Maven 집중 테스트는
  17개, failures/errors/skips 모두 0, exit 0이다.
- Python 집중 테스트는 7개, exit 0이다. 두 동시 start의 단일 PID,
  fixture 선기동 후 연결, 다섯 role 연결, 새 PID restart, 실제 stop,
  중복 stop/restart 실패, PID 불일치, 미응답과 시작 실패를 확인했다.
- Java smoke는 adapter → Python 명령 → 독립 extractor →
  HostObservationValidator 전체 경로를 확인한다. 같은 PID 연결을
  새 launch로 바꾸거나 다른 PID로 변조하면 validator가 거부한다.
- diff whitespace 검사는 exit 0이다.

최종 실행 로그는 이 worktree의 아래 경로에 있다.

- `verification/harness/target/evidence/process-roles/harness-tests.log`
- `verification/harness/target/evidence/process-roles/python-tests.log`
- `verification/harness/target/surefire-reports/`
- `verification/harness/target/evidence/actual/process-smoke-*/`
  아래 각 명령의 request, terminal snapshot, command transcript,
  descriptor, extractor 원행과 transcript가 있다.

## 남은 통합 검증

실제 ontology JVM/DB의 T26·V5·V7와 자연 scheduler gate는 이 Subtask에서
실행하지 않았다. 전체 41 case, 실제 모델 UAT, BTP도 NOT_RUN이다.
부모 coordinator가 다른 worker의 runtime role 변경과 통합하고
해당 제품 시나리오를 실행해야 한다. push·PR·외부 알림은 하지 않는다.
