# 구현 계획 완결성 반복 검토 — GPT-6.1 Sol high

검토일: 2026-10-07. 최종 판정: **구현 계획 완결성 PASS**. [구현 계획](../ontology-implementation-plan.md)의 채택된 온톨로지 범위를 구현할 데이터·명령·상태·권한·거래·검증·진행 절차에 남은 blocking finding이 없다. 이는 모든 가능한 구현 오류가 없다는 증명이나 실제 프로그램의 인수 결과가 아니다.

## 요청·범위·방법

사용자는 GPT-6.1 Sol high adversarial review를 반복하고 구현 계획이 온톨로지 설계를 구현하기에 완결될 때까지 수정하라고 요청했다. 실제 `gpt-6.1-sol`과 `reasoning_effort=high`로 별도 문맥의 세 검토자를 사용했다. 모델/effort를 프롬프트 문구만으로 흉내 낸 검토가 아니다.

- domain 검토자는 D01–D19의 품목·물량·구매·운송·수입·QC·판매·반품·회수·정산을 소유했다.
- execution 검토자는 D08–D12·D20–D26의 공통 요청·목표/의무·권한·멱등성·FDE·MCP·Skills·복구를 소유했다.
- delivery 검토자는 전체 요구 추적·Step 의존성·증거·기술 판정·fork 전환·운영 gate를 소유했다.

세 검토자는 읽기만 수행했다. coordinator가 계획을 수정하고 교차 반론을 전달한 뒤 수정된 실제 파일을 다시 읽게 했다. 기준은 논의 기록의 채택된 D01–D26·C1–C5와 사용자 기술 선택이며, 미채택 상세 제안을 몰래 필수화하거나 전체 범위를 축소하지 않았다. 이전 스택 검토의 V1–V8도 유지했다.

## 검토 라운드와 근거

| 라운드 | 검토한 계획 SHA-256 | 관찰과 조치 |
|---|---|---|
| 1 최초 완결성 검토 | `6a65c94835dc69844f0dceb3b6cc2c22be6cd08e5e50d43aea5c2774f1466e5d` | 기존 계획은 스택+V1–V8 중심. D13–D19 구현 catalog, API/state/control plane, D 전체 인수/Step, fork 자료 분기 누락을 확인해 전면 구체화 |
| 2 전체 수정본 공격·교차 반박 | `6c6210eb3d0e3ddfac14dea8066798065ca7585f17104dfce868c418869afc00` | 선언한 계약 사이의 실제 반례를 찾음. 실행/관측 혼동, 일반 이동 우회, 허위 FULFILLED, 의무 이전 단절, 무이벤트 만료, 관리 명령 누락, 너무 이른 V gate를 수정 |
| 3 보완본 재검토 | `ee7f289e8e0bed4d7286a3159d09d8ecb0de2b7b5c6d4e9b289bd8a105a9e92b` | 본문상 blockers는 해소됐지만 인도 관측에도 현재 적격성을 요구하는 과거 권한표 한 행이 남음. 세 검토자가 같은 충돌을 확인 |
| 최종 교정 확인 | `d58be4740a28e1eba2851d7f50fe96f17e216fcdb2ffac9d49227fa91a52503b` | 해당 표를 실행과 관측으로 분리한 현재 파일을 세 검토자가 다시 읽고 담당 범위 PLAN PASS. 이후 최종 편집은 문서 버전 표시·이 보고서 연결·표의 Markdown 구분자 escape만 변경 |

## 반례별 처분과 최종 계약

| ID | 반례·검토 쟁점 | 최종 근거와 인수 연결 | 처분 |
|---|---|---|---|
| P01 | V1–V8만 통과해 수입·회수·정산을 만들지 않아도 완료처럼 보임 | §6 도메인 전체 catalog·§13 D01–D26→모듈/Step/T, E1/E2 | CLOSED |
| P02 | MRTR 목적지 추가가 같은 key의 다른 payload conflict가 됨 | §3.3 입력수집 ID→최종 proposal/hash→효과key, §9 MRTR, V6 | CLOSED |
| P03 | 발주100을 현재 재고100으로 만들거나 증빙 두 장을120으로 집계 | §4 canonical 실물/사건·§6 계획/실물 구별, T07/T13/E1 | CLOSED |
| P04 | dispatch가 배분을 CONSUMED로 바꾸면 이후 delivery의 active allocation 조건이 막음 | §6 RECORD는 Dispatch/CargoScope와 소비된 배분을 참조, T17 | CLOSED |
| P05 | 출고 뒤 recall/처분 만료가 실제 인도 사실 기록까지 막음 | §6/7 실행과 관측 인가 분리, 확정 전 효과0·확정 사실/비준수 의무 분리. §7.1 표 최종 교정 | CLOSED |
| P06 | generic move의 목적지를 고객으로 바꿔 QC/출고 통제 우회 | §4.2 내부 primitive 비노출·내부 이동 scope·서버 effect class, V4 | CLOSED |
| P07 | 목표90/100의 잔여10을 넘기면 FULFILLED로 종료 | §5.2 현재 필수 조건 SATISFIED·pending 부재·종료사유 분리, T11 | CLOSED |
| P08 | TRANSFERRED만 남기고 target 생성/인수 실패로 책임 단절 | §5.3 stable root/assignment·수락/효력 한 거래·대상 유효성·부분 scope 보존·순환 금지, T10 | CLOSED |
| P09 | 큐/입력이 없으면 허용기한이 지난 예약을 발견하지 못함 | §10 nextValidityBoundary·due sweep·동일 fence·의무 upsert·실행 시 재검증, T16/T26/V5 | CLOSED |
| P10 | grant·정책·대조·안전 재시도를 설명하지만 실행 command 없음 | §7.2 관리/책임/정책/원천/복구 명령과 guard·reader, T08/T22/T24/T26 | CLOSED |
| P11 | S2 전체 V6 수령/V4 MCP PASS가 S3/S5 구현을 선행 요구 | §11 기반 계약/최종 도메인/채널 gate 구별·최초 전체 인수 시점, §13 evidence | CLOSED |
| P12 | 어려운 필수 D/V/BTP를 N/A로 바꿔 S6 종료 | §11 필수 D/C/V/E PASS, 선택·조건부 분기만 N/A. 계정/비용 미확정은 NOT_RUN | CLOSED |
| P13 | 기존 제조 DB live 변환이 없는데도 강제되거나 old commit으로 새 외부효과를 되돌렸다고 주장 | §12 fresh/live 자료 분기·archive·새 schema v1→v2·post-write 대조/복구, T23/V8 | CLOSED |

교차 반박에서 QC 사건의 인과 순서와 출고의 동시성은 서로 대체하지 못함을 유지했다. 회수25→폐기25도 다른 단계의 같은 실물을 더해50으로 만들 수 없다. 예약 이력/부족 의무와 실행 가능한 배분의 상한을 구별했고, 내용량 환산·관측·문서만으로 새로운 실물이나 권한을 만들지 않게 했다.

## 요구별 완료 감사

| 요구 묶음 | 확인한 실제 계획 근거 | 최종 판정 |
|---|---|---|
| D01–D07 범위·품목·계보·수량·시간·문서 | §1/3/4/6, T01–T07, C1/E1. domain이 전체 해당 항목과 충돌 사례를 검토 | PLAN PASS |
| D08–D12 신원·목표·책임·전이·판정 | §3/5/7, T08–T12, C2–C5, V1/V5/V7. domain/execution 중첩 검토 | PLAN PASS |
| D13–D19 구매부터 정산 | §6, T13–T19, C1/C4, V2/V3, E1/E2. 부분 기관처리·인도/반품·회수 대조 포함 | PLAN PASS |
| D20–D22 자연어·FDE·외부 원천 | §3/7.2/8/9, T20–T22, MCP·Skills·MRTR/멱등키·정의전환 | PLAN PASS |
| D23–D26 저장·감사·인수·운영 | §7/10–13, T23–T26, V4–V8. 자료 전환·법적 보류·복원·실행 evidence 포함 | PLAN PASS |
| 명시한 데이터/명령/state/guard | §3–9 catalog와 control plane, 같은 거래의 효과/감사/outbox·오류/재시도 | PLAN PASS |
| 전체 구현 순서·병렬 소유·통합 gate | §11 S0–S6, 기반검증과 최종 C/V 범위의 구별 | PLAN PASS |
| 실제 환경/업무값의 미확정 처리 | §11 R1–R9, owner·입력·기본경로·증거·차단Step·실패경로 | PLAN PASS |
| 제안/합의 및 구현/인수 구별 | §1/13/14와 논의 기록 §10/12. 법규·실모델·BTP는 미래 gate | PLAN PASS |

D별 구체 데이터/행동/Step/fixture는 계획 §13.1에 각각 한 행으로 연결돼 있다. 검토자는 ID가 있다는 사실만 확인하지 않고 각 계약의 동작·예외를 읽었다. coordinator는 최종 파일에서 D26개·C5개·V8개·E2개 참조와 로컬 문서 연결·fence·whitespace를 검사했다. 이 구조 검사는 위 내용 검토를 대신하지 않는다.

## 미실행 사항과 완료의 경계

이번 Task는 계획 보완이므로 backend·schema·MCP adapter·skill package를 구현하지 않았다. DB/API/경합/복구 test, 최신 SDK/client wire 실행, 실모델 평가, 실제 법규 검증, BTP 배포는 모두 NOT_RUN이다. R1–R9의 실제 값·증거는 해당 구현 Step에서 확정한다. 이들이 미래 gate로 명시됐다는 것이 이미 통과했다는 뜻은 아니다.

원격 issue/board/PR·main·DB·배포 상태를 변경하지 않았다. 직접 완료한 것은 현재 계획의 구체화와 반복 논리 검토, 연결/구조 검사, 논의 기록 반영이다. 계획 완결성은 현재 채택 범위와 확인한 반례에 대한 판정이며 향후 실제 구현에서 새 반례가 확인되면 해당 계약·test를 함께 수정한다.

## 최종 파일 검사

최종 계획 SHA-256은 `7f010093b1fba3673f674c78dddc8c407512e037831f093babc87d7421a22a00`이다. 검토 완료 hash `d58be474…`의 파일과 비교해 문서 버전 표시·보고서 링크·표 구분자 escape를 제외한 본문이 정확히 동일함을 확인했다.

구조 검사 결과는 D26/T26/C5/V8/E2의 개별 ID 대응, 관련 네 문서의 로컬 링크·명시 anchor·fence 균형·표 열 수·trailing whitespace 모두 PASS다. Markdown renderer 실행이나 backend test를 했다는 뜻은 아니다. 계획 검토와 구조 검사의 근거 범위를 구별한다.
