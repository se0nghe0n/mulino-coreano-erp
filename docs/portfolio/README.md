# Mulino Coreano 발표 자료

한국 진출을 가정한 식품 ERP의 현지화 설계, SAP 업무 개념 대응,
인간 승인과 지속 Case, 구매·품질·리콜 데모를 설명한다. 실제 기업의
프로젝트나 SAP 시스템 연동을 주장하지 않는다.

## 산출물과 발표 조건

- [편집 가능한 한국어 발표 원본](mulino-coreano-portfolio-ko.pptx)
- [배포용 PDF](mulino-coreano-portfolio-ko.pdf)
- [재생성 source](build.mjs)
- [해시와 검증 계층을 기록한 evidence](evidence.json)

14개 슬라이드와 각 슬라이드의 발표자 노트를 제공한다. 질문에 응답이
없어 적용한 작업 기본값은 SAP/ERP 면접 평가자, 10–15분 발표,
절제한 표·업무 흐름도다. native text·table·chart·diagram을 사용하며
사진이나 생성한 공장 이미지를 증거로 사용하지 않았다.

## 발표 흐름과 데모 근거

| 슬라이드 | 내용 | 근거 |
|---|---|---|
| 1–3 | 목적, As-Is/To-Be, SAP 모듈 대응 | README, 초기 기획안, ERD |
| 4–5 | 3계층·MONITOR, Case·Run·인간 승인 | 인터페이스·실행·구매 계약 |
| 6 | 구매 계산·제안·승인·재개 | #34 source d7ce2d8, 구매 인수 기록 |
| 7 | HOLD·QC·품질 판정 | 품질 API, QM SIT |
| 8 | 사고 원료와 LOT·고객·리콜 범위 | #27 현재 JAR proof |
| 9 | 증거·충돌·명시적 successor | #44 현재 JAR·CLI·stdio proof |
| 10 | Supplier 변경·replay·감사 | #41 현재 JAR·Swagger proof |
| 11–12 | 검증 계층, 과거·현재 native UAT | 아래 evidence 표 |
| 13–14 | 제외 범위·규제 갭, 데모 순서·인수 경계 | 범위 결정·runbook |

[데모 runbook](../15_demo_runbook.md),
[시나리오](../16_scenario_tests.md),
[테스트 분류](../17_test_classification.md)를 실행 절차의 기준으로 삼는다.
이 발표 작업은 해당 문서의 현재 native UAT 기록을 변경하지 않았다.

## source와 검증 계층

기준 source는 `0132e0a62ad9f086e5c70c5c857d4546f5921ef9`다.
이는 fork의 통합 baseline이며 조직 `main` 병합을 뜻하지 않는다.
선행 변경의 현재 로컬 proof를 해당 source와 함께 기록한다.
모든 수치는 이미 실행한 담당자의 보고서에서 옮겼다. 문서 작업을 위해
백엔드 테스트나 유료 모델을 다시 실행하지 않았다.

| 계층 | 실제 관측 | source·한계 |
|---|---|---|
| Supplier 실행 테스트 | 567 발견, 18 skip, 549 통과, 실패 0 | 0132e0a |
| 최신 scripted SIT | P2P 6, QM 6, RC 6, 재시작 2, 합계 20 통과 | Supplier 통합 baseline |
| 도구 단위·smoke | MCP 26, runner 57, Zig 7, CLI 32 통과 | #44 0ee144c의 별도 기록 |
| 리콜 현재 JAR | 생산 LOT 10, 사고 원료 root 2, 증거 raw LOT 3 | #27 5911721 |
| 리콜 범위 | 고객 2, 출고 115, 무관 LOT ACTIVE, replay 안정 | OFFLINE/PENDING, submittedAt null |
| 증거 현재 JAR | source 3, judgment 7, ERP 쓰기 0 | #44 0ee144c, 충돌·successor 보존 |
| Supplier 현재 JAR | CRUD 5, Swagger 5, 감사 3, canonical replay | soft delete·version 충돌 확인 |
| 구매 데모 | 5/5 | #34 d7ce2d8, 실제 인간 stdio, 모델 호출 없음 |
| 과거 native UAT | 10월 3일 Claude 3건 통과 | cb04403. 현재 source 인수로 이월하지 않음 |
| 현재 native UAT | P2P-001 통과, 나머지 4개 진행 중 | 아래 provisional checkpoint |
| 실제 클라이언트 #35 | 인수 대기 | 로컬 stdio 구현과 별도 |
| 운영 배포 | 미실시 | 로컬 PoC 범위 |

`evidence.json`은 담당자 원본 보고서의 SHA-256과 공개 가능한 결과를
보관한다. 원본 전체 로그와 diagnostic/실패 시도는 coordinator의 비공개
인수 bundle에 보존한다. capability·human/service secret, 인증 volume,
개인 파일 경로는 발표 원본·노트·source·evidence에 복사하지 않았다.

### 현재 native P2P checkpoint

CLAUDE / `claude-sonnet-5`의 TC-P2P-001은 통과했다. PO 1개와
16,500원을 적용했고 Case는 WAITING이며 원래 coordinator는 DONE이다.
5개 Run 모두 usageComplete다. CLI가 보고한 해당 시나리오 비용은
USD 3.0874898, input 94, output 18,050, cache read 4,165,609,
cache write 518,420 tokens다. 전체 5건의 합계 비용으로 해석하지 않는다.

실행 image는 `evidence.json`의 image SHA로 식별한다. production baseline은
0132e0a이며 private-key test harness의 최종 commit은 인수 통합 때 기록한다.
나머지 4건과 최종 source 인수는 진행 중이므로 전체 완료를 주장하지 않는다.

## 규제와 권한의 한계

22개 allergen master를 19개 법정군으로 매핑한다. 필수 인증서 유형
전체 검사와 자동 30일 사전 알림은 이슈 등록을 기다리는 갭이다.
리콜 기록의 소프트웨어 2년 보관과 즉시 보고용 초안을 구현했지만 실제
MFDS 전송·법정 양식 검증을 수행하지 않았다. Goal 6 전체 완료나 법적
적합성 인증을 주장하지 않는다. As-Is/To-Be 표는 초기 설계 가정이다.

VERIFIED는 인간의 명시적 attestation이며 독립적인 외부 사실 검증이나
ERP 변경 권한을 제공하지 않는다. MONITOR는 승인 없이 조회하지만 채널
인증·Case scope를 적용한다. 공유 local role/key는 개인별 운영 IAM의
증거가 아니다. OAuth·전용 대시보드·운영 배포를 제외했다. 능동 알림과
실제 다중 채널 UX 인수는 별도 경계다.

## 재생성과 검증

Node 24와 `@oai/artifact-tool`이 필요하다. 설치된 Presentations skill 및
bundled Python 경로를 환경변수로 전달한다. source는 상대 경로를 사용한다.
기본 font는 macOS의 Apple SD Gothic Neo이며 다른 환경에서는 설치된
한국어 font를 `PORTFOLIO_FONT`으로 명시한다. 기본값을 바꿀 때 모든 페이지를
다시 렌더링해 줄바꿈·fallback을 확인한다.

```bash
export PRESENTATIONS_SKILL_DIR='<설치된 Presentations skill 디렉터리>'
export RUNTIME_PYTHON='<bundled python3>'
export RUNTIME_NODE_MODULES='<bundled node_modules>'
export PORTFOLIO_OUTPUT_DIR='<worktree 안의 새 output 디렉터리>'
# 실행 전 artifact operation marker는 skill 지침에 따라 한 번 기록한다.
node docs/portfolio/build.mjs
# bundled soffice와 기존 한국어 font로 PPTX에서 PDF를 만든다.
export SOFFICE='<bundled soffice>'
node docs/portfolio/export-pdf.mjs \
  "$PORTFOLIO_OUTPUT_DIR/mulino-coreano-portfolio-ko.pptx" \
  "$PORTFOLIO_OUTPUT_DIR/pdf"
```

finalizer는 package integrity, 14개 슬라이드, native table/chart, font
선언, geometry와 Artifact Tool 재import를 검사한다. chart workbook은
표시한 scripted SIT literal data의 snapshot이며 별도 계산식을 만들지
않았다. 상세 로그·render는 산출물과 별도로 보관한다. PDF는 PPTX를
변환한 배포본이며 편집 원본은 PPTX다. `export-pdf.mjs`는 private
Fontconfig로 기존 한국어 font 경로를 전달한다. 최초 headless export의
한국어 glyph 손실을 수정한 경로다. 다른 OS는 `PORTFOLIO_FONT_DIRS`에
설치된 font 디렉터리를 전달하고 PDF 페이지를 다시 확인한다.

최종 PPTX 14개 슬라이드와 PDF 14개 페이지를 각각 렌더링해 확인했다.
한국어 glyph·표·chart·업무 흐름도·footnote에 clipping이나 의도하지 않은
겹침을 발견하지 않았다. PDF 14개 페이지 모두 한국어 text를 추출할 수
있고 Apple SD Gothic Neo subset font를 포함한다. PPTX의 14개 노트,
5개 native table, 1개 chart와 workbook, editable diagram 객체를 확인했다.

PowerPoint 앱에서 직접 열어 편집하거나 저장한 검증은 수행하지 않았다.
최종 package와 렌더링 검사는 해당 앱의 실행 확인을 대신하지 않는다.
최종 source·native UAT·문서 통합과 combined checks의 인수는 coordinator가
담당한다. 이 독립 발표 작업의 종료만으로 전체 Step을 완료하지 않는다.
