# Step3 S0 구현과 통합 기록

도메인 구현 전에 CAP·PostgreSQL의 저장·거래·인가 경로가 하나로
동작하는지 확인한다. 테스트 준비만으로 후보 stack을 채택하지 않는다.
사용자 Step3는 실제 GPT-6.1 Sol medium으로 진행하며 S0의 모든
산출물과 결합 checks가 끝나기 전 S1로 넘어가지 않는다.

## 기준선과 소유권

모든 S0 writer는 `4280a750dca0aef35abaf10b6797881181dd88d7`, 로컬
`step2-complete`에서 시작한 독립 branch/worktree를 사용했다.
절대 경로와 소유 파일은 [작업 기록](task-status.md)의 S0 표를 따른다.
backend는 platform writer 한 명이 소유했다. security와 protocol은
독립 HTTP probe를 작성하고 같은 서버에서 순차 실행했다.

| Subtask | 통합한 worker commit | 현재 확인한 범위 |
|---|---|---|
| platform | `9ba927c`, `8c6248f` | CAP build·PG18.6·Flyway·거래/경합/복원10 tests |
| security | `b996279`, `1e999c4`, `07d17c5`, `0d65aa2`, `bdad7d0`, `17bb768` | 실제 HTTP23건 PASS, 세 경로 정상/거부 counter-call |
| protocol | `581013d`, `346383a`, `33806da`, `1da5e77`, `ca84fb8`, `63266c3` | 실제29 requests·118 assertions PASS |
| inventory | `552b992` | 보존 metadata·7문서 hash PASS, R3 사용자 선언 기록 |

## 구현 중 교정한 계약

최신 공식 MCP `2026-07-28`을 확인하면서 T20의 discovery oracle가
비표준 필드를 요구하는 점을 발견했다. 다섯 subcase만 수정해
supportedVersions와 별도 tools/list를 검증한다. clientInfo는 선택
metadata로 처리한다. 공개 tool 목록의 exactSet은 유지했다.
[정정 근거](../../verification/platform/protocol/compatibility-repair.md)를
남겼고 focused22 tests가 통과했다. root의 `bd7d6a3` fresh preparation은
41 case·789 subcase·20473 assertion PREPARED, 문제0이었다.
이 변경은 기존 Step2 보고서의 과거 실행 결과를 덮어쓰지 않는다.

실제 서버 probe는 OData 권한 거부500과 MCP namespace metadata 누락을
발견했다. platform에서 수정한 뒤 보안23건과 wire118건이 통과했다.
대기 중 nbf 시간이 지나 유효해진 token fixture의 실패도 별도 보존했다.
[보안 결과](../../verification/platform/security/http-evidence.json)와
[wire 결과](../../verification/platform/protocol/evidence/wire-summary.json)는
전체 제품 인수와 구별한다.

## 자료와 운영 경계

R3는 사용자의 “실제 운영 자료 없음 — 새 DB와 개발 fixture로 진행”
선택으로 CONFIRMED다. 기존 volume은 보존하고 새 격리 DB를 쓴다.
[보존 inventory](s0-inventory/README.md)는 Git tree의 파일명·hash와
object 존재만 확인했으며 legacy 내용을 읽거나 재사용하지 않았다.
새 온톨로지 v1→v2와 DB/blob/definition 복원 인수는 계속 필수다.

개발 signed JWT는 local profile 전용이다. 실제 IdP 구현 전에는
모든 nonlocal startup을 거부한다. 운영 신원·정책·BTP·실모델 인수는
미실행이다. S0 기술 fixture의 public decimal 표현과 text JSON,
전체 도메인/MRTR/client loading은 해당 후속 gate에서 구현한다.

## 현재 통합 gate

S0는 ACTIVE다. platform의 실제 DB WAIT 관찰과 fresh compiler 출력
대조를 보강하고, 실행한 JAR/source hash에 HTTP 결과를 연결한다.
coordinator가 통합 branch에서 관련 checks를 실행하고 exact platform
판정을 확정한 뒤 S0를 닫는다. S1–S6와 사용자 Step3 전체는 미완료다.
