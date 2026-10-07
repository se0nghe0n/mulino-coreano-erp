# [FEAT] 완제품 수입 온톨로지 시스템 전체 구현

상태: 게시 전 초안이다. 새 fork Task의 이슈 번호는 미확정이다.
fork의 issue 기능은 비활성 상태로 확인됐다. 추적 위치 결정과 기능
활성화 또는 별도 추적 연결은 coordinator가 사용자와 확정한다.
이 초안 작성은 원격 게시나 원본 프로젝트 scope 변경이 아니다.

## 작업 개요

명사와 동사 두 관리 진입점에서 같은 대상·업무·물량·증거·책임을
읽고, 이탈리아 완제품 구매부터 국내 B2B 인도와 후속 책임까지를
새로 구현한다. 채택된 D01–D26와 C1–C5 전체를 구현 계약으로 삼는다.

사용자는 skills·tests·구현·실제 E2E·패턴/refactor·운영 매뉴얼과
미완료 해소의 반복을 요청했다. 각 사용자 Step의 두 모델 검토와
지적 수정·통합 checks를 완료 조건에 포함한다.

## 레이어

- [x] backend·database·contracts
- [x] agents·skills·MCP
- [x] verification·실행 인수
- [x] deploy·운영·복구

## 상세 작업 내용

- S0: 새 추적 경계·실자료 inventory와 Java21/CAP 후보 spike를
  수행하고 exact 기술 manifest·schema 소유권·신원·MCP를 고정한다.
- S1: 정의 발행·품목·LOT·segment·계보·단위·증거·조직/grant·
  두 진입점 조회를 구축한다.
- S2: 목표 버전·판정·대기·의무·인계·승인·멱등성·감사와
  outbox·claim·reconciler를 원자적 application command로 구현한다.
- S3: 구매·공급 약속·운송·수입 근거·임시/확정 수령·QC·
  재고 대조를 구현한다.
- S4: 판매 적격·예약·출고·인도·반품·정정·회수·정산 참조를
  구현한다. 실제 은행 이체·세금 신고 효과는 생성하지 않는다.
- S5: 최신 stateless MCP의 전체 조회/command, MRTR와 여섯 업무
  skills·manifest·discovery links, 정의 전환과 원천 adapter를
  통합한다. 결정적 검증과 실제 client/model 인수를 구별한다.
- S6: 새 ontology schema upgrade·신원/binding/TLS·보존·운영 관측·
  backup restore·cutover를 인수한다.
- D01–D26 ↔ T01–T26, C1–C5, V1–V8, E1/E2의 실제 assertion과
  artifact를 연결한다. source/model/local/client/BTP 증거를 나눈다.
- 원본 #57의 테스트 방법론만 참고하고 옛 코드·tests·schema·
  skills·운영 문서는 읽거나 재사용하지 않는다.
- R1–R9를 결정자·근거·적용일·차단 gate와 기록한다. 운영 계정과
  비용이 미확정이면 해당 실행은 대기로 두고 독립 검증을 계속한다.
- 사용자 Step 순서의 모델/effort로 수행하고 매 Step의 Sol xhigh·
  Astra low review 지적을 해결한다. 실제 동작으로 운영 매뉴얼을 쓴다.

## 완료 조건 (Definition of Done)

- [ ] 채택된 D01–D26와 C1–C5의 구현을 통합했다.
- [ ] T01–T26·C1–C5·V1–V8·E1/E2의 필수 실행을 통과했다.
- [ ] 무권한 쓰기·중복 효과·물량 이중소비·허위 완료가 0건이다.
- [ ] 동일 시점의 두 진입점에서 ID·물량·근거·책임이 일치한다.
- [ ] 전체 command의 현재 인가·거래·멱등·감사·복구를 확인했다.
- [ ] 실제 client의 MCP wire·skill loading·tool 실행과 승인된
  비용 범위의 실모델 의미 평가를 인수했다.
- [ ] 새 schema v1→v2와 DB·blob·정의/evaluator 복원을 인수했다.
- [ ] BTP와 실제 운영 정책·identity의 필수 인수 증거를 남겼다.
- [ ] 매 사용자 Step의 두 검토 지적을 해결하고 통합 checks를
  통과했다. 미실행을 PASS·비대상·waiver로 숨기지 않았다.
- [ ] 검증된 동작·명령으로 운영 매뉴얼을 작성했다.
- [ ] 새 fork Task의 추적 위치와 이슈 연결을 확정했다.

## 참고 자료

- [설계 철학](../ontology-design-philosophy.md)
- [구현 계획](../ontology-implementation-plan.md), 특히 §11–§13
- [논의 기록](../ontology-discussion.md)
- [현재 실행 기록](task-status.md)
- [원본 #57 테스트 방법론](https://github.com/mulino-coreano/mulino-coreano-erp/issues/57)

GitHub 게시 시 상대 링크를 게시 대상의 확정 commit 또는 Task branch
URL로 바꾼다. 원본 #57을 이 Task의 구현 이슈로 닫거나 scope를 바꾸지
않는다. 기존 원본 board/Phase가 새 fork 개발 범위를 대신하지 않는다.
