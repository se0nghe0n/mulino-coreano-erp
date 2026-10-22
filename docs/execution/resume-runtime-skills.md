# 재개 Step 2: 운영 skill package와 native client 설정

2026-10-22에 여섯 운영 package를 새로 작성해
`RUNTIME_SKILL_MISSING`의 원인을 해소했다. 기준은 Task parent
`d6288006df3b17b9294c844fcea2270cf69a69c5`다. 원본 checkout의
untracked `agents/`는 읽거나 재사용하지 않았다.

## 구성과 공개 계약

실제 디렉터리는 `agents/skills/` 아래에 있다.

- `ontology-work-coordinator`: 두 진입점, 목표·의무·인계·남은 책임이다.
- `ontology-procurement-transport`: 구매 제안·승인·전달·약속·운송이다.
- `ontology-import-qc`: 제출 근거·부분 수령·QC와 독립된 제한이다.
- `ontology-sales-returns-recall`: 적격성·인도·반품·정정·회수다.
- `ontology-settlement`: 송장 대조·차이·지급 참조다.
- `ontology-definition-authoring`: typed 정의·검증·승인·발행·전환이다.

`.agents/skills/<name>`과 `.claude/skills/<name>`의 12개 symlink는
각각 실제 디렉터리에 직접 연결한다. 개발용 skill은 이 목록에 넣지
않았다. metadata→본문→필요 reference 순서로 읽는다. 기존 native
client의 `load_operational_skill`이 읽을 수 있는 package 파일 13개에
각 역할의 capability/slot reference와 공통 tool 계약을 포함했다.

`agents/runtime-package-manifest.json`은 package별 version·파일 hash·
전체 skill hash, 지원 capability·schema, 서버 정의 호환 조건과
client별 검증 범위를 기록한다. 역할별 reference는 실제 backend
handler와 `contracts/capability-slots.json`에서 대조했다.
`acceptance-capabilities.json`의 과거 준비 상태를 실행 지원 목록으로
사용하지 않았다. 실행 시에는 배포 서버의 `tools/list`와
`/api/ontology/capabilities`, Work의 고정 정의를 다시 확인해야 한다.

서버 인가·승인·목표 판정·영속 멱등성은 skill로 대체하지 않는다.
수량/기간/근거와 남은 인간 책임, 두 진입점의 같은 ID/시점,
문서 지시의 데이터 경계를 각 역할에 명시했다. 기관 자동 제출,
은행 이체, 임의 코드 발행과 제품이 실행하지 않는 definition 회귀는
지원한다고 안내하지 않는다.

## Sol Ultrafast와 역사 보존

최신 사용자 지정에 따라 `NativeClientPolicy.MODEL`,
`NativeCodexClient.RuntimeHome.CONFIG`, 승인 pin과 계약 fixture를
`gpt-6.1-sol` / `ultrafast`로 맞췄다. 기존 high effort는 유지했다.
R8은 기존 구독 인증만 허용하며 추가 API 비용 상한0, credit 구매
금지, fallback 금지, 구현 중 모델 호출 금지를 유지한다. 업무 수용치
미확정도 변경하지 않았다.

`verification/native-client/local-preflight.json`의 Astra 증거는
수정하지 않았다. 당시 checks와 승인 파일은
`checks-astra-20261022.json`, `subscription-approval-astra-20261022.json`에
바이트 그대로 보존했다. 새 결과는
`local-preflight-sol-ultrafast.json`과 현재 `checks.json`을 읽는다.

실제 `codex-cli 0.163.0` non-model preflight는 기존 ChatGPT 구독,
명시한 Sol model, Ultrafast와 high effort의 `thread/start` 수락,
dynamic tool schema 수락을 확인했다. `turn/start`는 호출하지 않았고
실제 모델 호출은 0회다. 테스트 double의 가상 turn 이벤트는 실제 모델
실행이 아니다. 개인 config·인증 파일 내용을 수정하거나 복사하지 않았다.

## 검증

상세 command·version·hash·exit code는
`verification/native-client/checks.json`과 연결 artifact에 있다.

| 검사 | 관찰 |
|---|---|
| `validate-runtime-skills.py` | 6 package, 12 직접 link, 13 파일·hash·slot/source 대조 PASS |
| `skill-creator/scripts/quick_validate.py` | 6 package 모두 exit0 |
| native/model-binding 관련 네 suite | 76 tests, 실패0·오류0·skip0 |
| 최종 package 변경 후 native suite | 15 tests, 실패0·오류0·skip0 |
| `./verify prepare` | 41 case PREPARED, exit0; runtime PASS는 아님 |
| `verification/model-binding/run native-preflight` | Sol Ultrafast 설정 수락, exit0; 모델 호출0 |

quick_validate의 PyYAML은 `/tmp`의 별도 venv에 설치했다. 개인 Python
설정은 바꾸지 않았다. package나 공개 계약을 수정하면 먼저 내용을
검토하고 `python3 verification/native-client/validate-runtime-skills.py
--refresh`로 manifest hash를 갱신한다. 일반 검증은 `--refresh` 없이
실행해 drift를 실패로 처리한다.

실제 모델의 본문/reference 선택·로딩, 제품 tool 왕복과 의미 인수는
`NOT_RUN`이다. `NativeClientIntegration`과 model-binding adapter는
다른 소유 범위이며 이 작업에서 수정하지 않았다. worker-goals와
worker-claims의 backend/fixture/harness 파일도 변경하지 않았다.
필요한 별도 통합과 허용된 실제 실행 전까지 전체 Step/S5/Task 완료로
올리지 않는다. push·PR·외부 메시지·BTP 작업은 수행하지 않았다.
