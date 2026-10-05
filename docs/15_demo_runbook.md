# 실제 구매 인수 데모 실행 절차

#34는 사람이 승인한 구매안만 ERP 발주로 반영되고, 승인 뒤에도 입고를
기다리는 책임과 Case가 남는지 검증한다. 재계산 전후의 구매안을 하나의
Case에서 구분하여 확인한다. BLOCK과 CANCEL은 각각 독립적인 최종 인간 결정으로 보존한다.

## 실행 범위와 전제

- 저장소 root에서 Java 21, Python 3, Node, Docker CLI를 사용한다.
- PostgreSQL 18 컨테이너 `mulino-merged-validation-pg`의 포트는 55460이다.
  예시 DB 계정 `postgres/test`는 이 폐기용 로컬 컨테이너에만 해당한다.
- JAR를 새로 빌드하고 `local` profile로 실행한다. Flyway가 새 DB에
  현재 migration을 적용한다. 기존 DB를 재사용하거나 삭제하지 않는다.
- `MULINO_ACCEPTANCE_DB`는 `mulino_purchase_acceptance_`로 시작하는
  **새 빈 DB**여야 한다. API와 SQL이 반드시 같은 DB를 가리켜야 한다.
- 테스트는 localhost HTTP와 실제 stdio MCP를 사용한다. agent Run은
  service header로 수동 claim한다. LLM, 로그인 volume, 유료 모델은
  사용하지 않으므로 실제 모델 UAT의 증거가 아니다.
- Case 생성은 HTTP, 계획·구매안 생성은 capability HTTP, 인간 최종
  승인과 취소는 stdio MCP다. 공급망·구매 업무 배정과 과거 ERP 데이터는
  폐기용 DB에 fixture SQL로 준비한다. 자율 dispatch의 증거가 아니다.

## 1. 새 DB와 현재 JAR 준비

아래 DB 이름은 재실행마다 새 이름으로 바꾼다. 존재하는 DB를 drop하지
않는다. OrbStack 이외의 환경에서는 `DOCKER_HOST`와 포트를 맞춘다.

```bash
export DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock
export MULINO_ACCEPTANCE_DB=mulino_purchase_acceptance_20261005_repeat1
export MULINO_ACCEPTANCE_CONTAINER=mulino-merged-validation-pg
export MULINO_ACCEPTANCE_EVIDENCE=/tmp/mulino-purchase-acceptance-repeat1
export MULINO_API_BASE=http://127.0.0.1:55463/api/v1
# 이 값은 폐기용 로컬 데모 전용이다. 운영 credential을 쓰지 않는다.
export MULINO_LOCAL_SERVICE_SECRET=purchase-acceptance-local-demo
# 인간 host terminal에만 보관한다. service/agent subprocess에 전달하지 않는다.
export MULINO_LOCAL_HUMAN_SECRET="$(openssl rand -hex 32)"
mkdir -p "$MULINO_ACCEPTANCE_EVIDENCE"
docker start "$MULINO_ACCEPTANCE_CONTAINER"
docker exec "$MULINO_ACCEPTANCE_CONTAINER" createdb -U postgres \
  "$MULINO_ACCEPTANCE_DB"
(cd backend && ./gradlew bootJar --no-daemon) \
  > "$MULINO_ACCEPTANCE_EVIDENCE/build.log" 2>&1
(cd mcp-server && npm ci)
```

## 2. 전용 서버 실행

같은 terminal에서 foreground로 실행하고, 다음 단계는 동일한 환경
변수를 설정한 두 번째 terminal에서 수행한다. 종료 시 이 서버만
Ctrl-C로 멈춘다. DB는 증거와 함께 보존한다.

```bash
DB_URL="jdbc:postgresql://127.0.0.1:55460/$MULINO_ACCEPTANCE_DB" \
DB_USERNAME=postgres DB_PASSWORD=test \
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --server.port=55463 \
  > "$MULINO_ACCEPTANCE_EVIDENCE/backend.log" 2>&1
```

서버의 `Started BackendApplication`과 Flyway 성공을 확인한다.

```bash
curl --fail --silent http://127.0.0.1:55463/api-docs > /dev/null
python3 mcp-server/scripts/purchase-acceptance.py \
  > "$MULINO_ACCEPTANCE_EVIDENCE/run.log" 2>&1
cat "$MULINO_ACCEPTANCE_EVIDENCE/summary.json"
```

script는 실패하면 nonzero로 종료한다. 기존 fixture를 다시 적용하지
않는다. 실패 로그와 DB를 남기고 새 DB로 다시 실행한다.

## 3. 판단해야 할 업무 결과

| 경로 | 관찰 결과 |
|---|---|
| 구매안 대기 | 구매 application·연결 PO·후속 업무 0 |
| VIEWER 승인 시도 | stdio 쓰기 거부, MANAGER 승인 전 PO 없음 |
| BLOCK 및 같은 request key 재전송 | BLOCKED, 구매 업무 CANCELLED, PO·후속 업무 0, 결정·감사 중복 없음 |
| 다른 version 또는 hash | HTTP 409, application·PO·후속·결정·감사 수 불변 |
| 실제 공급조건 단가 변경 | 기존 구매안 EXPIRED, PO·후속 업무 0 |
| 같은 Case 재계산 | 새 version/hash를 갖는 독립 구매안 |
| MANAGER 승인 및 재전송 | application 1, 연결 PO 1, 후속 업무 1 |
| 후속 책임 | 구매 업무 DONE, 후속 업무 WAITING, Case WAITING |
| 발주 후 입고·생산 | 새 발주 연결 inbound 0, 생산 LOT 추가 없음 |
| 감사 로그 변조 | governance_audit_logs UPDATE·DELETE·TRUNCATE 거부, 원행 불변 |
| 인간 CANCEL 및 재전송 | CANCELLED, 최종 CANCEL 이력, 구매 업무·대기 CANCELLED, ERP 쓰기·후속 업무 0 |
| 취소 뒤 승인 및 새 취소 | HTTP 409, 최종 이력·ERP 상태 불변 |

fixture 원본 기준일은 2026-09-05다. script는 모든 날짜 literal에
`오늘(Asia/Seoul) - 기준일`을 동일하게 더해 `fixture.sql`로 저장한다.
서버 clock과 요청의 asOf를 고정하지 않는다. 실제 실행일, fixture hash,
JAR hash, source commit은 `summary.json`에 남는다. 오늘 날짜를 넘기는
자정 전후에는 새 서버·DB로 다시 실행한다.

기대 발주는 밀가루 5 KG × 1,200원, 설탕 5 KG × 1,500원,
포장재 60 EA × 50원으로 합계 16,500원이다. 새 발주에는 실제 세금계산서
발급이 없으므로 `taxInvoiceNumber`와 `taxInvoiceDate`는 null이다.
가격 변경 경로는 밀가루 단가를 100원 올려 만료를 확인한 뒤 fixture
가격을 복구하고 새 version을 계산한다. 만료된 구매안을 되살리지 않는다.

## 4. 증거와 남은 인수 조건

`summary.json`, `history.json`, `orders.json`, `business-state.json`,
`audit-mutation-denied.json`, `stdio.log`, `stdio-cancel.log`,
`cancel-final-history-mutation-denied.json`를 보관한다. 각 구매안의 pending
snapshot과 409 응답도 별도 JSON으로 남는다. 토큰과 service secret은
증거 파일에 저장하지 않는다. 전체 발주 수에는 fixture 과거 발주가
포함되므로 `purchase_application_id IS NOT NULL`인 새 발주를 세어야 한다.

PENDING 구매안은 MANAGER가 CANCEL할 수 있다. version/hash별 최종
CANCEL 결정은 BLOCK·APPROVE와 별도다. 새 ERP 쓰기나 후속 업무 없이
구매 책임만 닫고 상위 방침 확인을 남긴다. 기존 승인 후속 책임은 보존한다.
날짜별 결과는 [2026-10-05 인수 기록](reviews/2026-10-05-purchase-acceptance.md)에
기록한다. 실제 harness/model UAT와 운영 발주·세금계산서 발급은 별도다.

## 실제 모델 시나리오 인수와 구분 (#25/#26/#27)

위 구매 demo는 모델 없는 HTTP·stdio 인수다. 현재 source의 실제 모델
인수는 `backend`에서 `uatTest`로 따로 실행한다. 상세 tag·고정 업무
시계·폐기 DB guard·비용과 종료 증거 계약은
[시나리오 문서](16_scenario_tests.md)를 따른다.

2026-10-06 runtime image는 현재 CLI와 role 파일에서 allowlisted
build context로 새로 만든 `mulino-agent-runtime:completion-0132e0a`다.
image ID는 `sha256:aa055d76d0c8d44b9707c84726101e551ec123dc0dba714bea4746b02d4d4656`이다.
모델 없는 smoke를 통과한 뒤 기존 named login volume의 metadata만
확인하고 CLAUDE/claude-sonnet-5를 명시했다. 로그인 파일을 읽거나
초기화하지 않았다. 실제 호출은 비용이 발생한다.

scenario 인간·서비스 key는 JVM마다 별도 난수로 생성하고 host client에
한정한다. 이 값을 명령 인자·모델 context·로그인 volume·증거에 쓰지
않는다. 테스트용 고정 업무일과 실제 wall-clock lease는 구분한다.
QC/리콜 PENDING만으로 UAT 성공을 판단하지 않고 모델 종료 및 모든
Run의 보고 사용량까지 확인한다. OFFLINE/PENDING 리콜 초안은 실제
식약처 전송 증거가 아니다.

### 현재 Codex 접근 확인의 인간 경계

같은 native runtime의 account/rateLimits/read가 AUTHENTICATION 오류를
보고한다. 모델 catalog와 cached login 성공만으로 실제 모델 실행 접근을
판단하지 않는다. Codex P2P parity probe는 첫 Run에서 실패했고 비용과
사용량은 unknown이다. Claude의 과거 실패 원인과 현재 상태는 별개다.

인간이 검토할 정확한 device login 명령과 갱신 후 모델 없는 확인 절차는
`/tmp/mulino-project-completion-20261005/native/human-codex-auth-refresh.md`에
준비했다. native `login --help`에서 `--device-auth`를 확인했다. 기존
image·volume·UID·격리를 유지하고 credential을 읽거나 export하지
않는다. 로그인 갱신을 실행하지 않았으며 인간의 선택을 기다린다.
production OAuth/IAM이나 현재 5개 실제 모델 인수의 완료 증거가 아니다.

### 3차 audit 뒤 Claude 재인수 시도

정상 writable mount의 no-tool Claude 진단은 같은 Sonnet 5에서 exit 0,
OK, resolvedModel 및 USD 0.001066 보고 사용량으로 성공했다. 이는 최소
모델 호출의 현재 접근 증거이며 전체 role 실행이나 업무 인수의 증거는
아니다. 이 확인 뒤 004·QM-001·RC-001만 새 DB에서 재시도했다.

세 업무는 첫 Run에서 실패했고 ERP 쓰기는 없었다. 생산 schema와 기본
boolean schema의 tools-disabled 최소 진단도 같은 error fingerprint로
실패했다. 현재 Claude를 인증 불가라고 단정하거나 특정 schema keyword가
원인이라고 판단하지 않는다. tools-disabled와 structured-output의 상호
작용 및 budget·일반 처리 경로도 미해결이다. 원래 validation은 유지했고
추가 호출이나 로그인 변경은 하지 않았다. 상세 증거는 16번 문서를
따른다.
