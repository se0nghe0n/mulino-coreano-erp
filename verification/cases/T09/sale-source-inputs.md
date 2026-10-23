# T09 판매 원천 입력의 한정 보완

두 예약의 원래 물량과 94개 assertion을 유지하고, 부족했던 판매 원천을
새로운 가상 입력으로 명시한다. Task와 현재 Step은 OPEN이다.

기준은 `e932710856553570094a3350268417f19b03a021`이다.
`author_sale_sources.py`는 `cumulative-versus-state`와
`exists-versus-end-throughout`만 다룬다. 각각 원래 `sale60`과 `sale`
직후에 공개 명령을 넣는다. 기존 예약·수령·출고·시계·PO·초기량·원문,
다른 7개 subcase와 기존 actor/grant는 바꾸지 않는다.

## 새 가상 결정

`newSynthetic=true`, `originalAuthorityClaimed=false`,
`operatingApprovalClaimed=false`로 표시한다. 원래 또는 실제 규제 승인을
복원했다는 뜻이 아니다.

- LOT 만료와 모든 새 원천의 유효 종료는
  `2026-10-31T23:59:59Z`다. 시작은 해당 fixture의 `clock.asOf`다.
- QC, CUSTOMER, COMMERCIAL을 SELL과 DISPATCH별 독립 원문으로 만든다.
  각각 원래 고객, 품목, LOT와 [0,60) 또는 [0,100) BOX를 가리킨다.
- authority는 `T09_SYNTHETIC_AUTHORITY`, namespace는
  `t09-sale-eligibility`, version은 `SYNTHETIC-policy-v1`이다.
- `requiresLabel=false`는 명시적인 최소 가상 정책 선택이다. 운영 면제나
  원래 요구사항이 없었다는 주장이 아니다. 기존 ELIGIBILITY의
  `requiredCategories=[QC,CUSTOMER,COMMERCIAL]`은 그대로다.
- 새 Human `t09SaleSource`와 `t09SaleSourceDelegator`만 추가한다.
  동일 조직·정확한 P 품목·기존 기간·all-of를 유지한다. category 결정,
  규제 기록, 원문 대조와 해당 조회에 필요한 capability만 부여한다.
  `getObject`/`getWork`의 공통 화면 조회가 사용하는 `getInventory`도
  명시한다. admin·승인·실물 수령/예약/출고 권한은 추가하지 않는다.
- 새 quality와 규제 원천의 Work는 실제 판매 주문의 반환 Work다.
  원래 구매/수량 판정 Work에 판매 원천을 혼합하지 않는다.

## 공개 원천 사슬

QC는 SEGMENT 문서 업로드 → 같은 JSON의 category event → 실제 claim
대조 → canonical → 최신 segment/Work 조회 → 문서 ID를 사용한
`recordDispositionBasis`를 실행한다. `$encode`가 동일한 원문에서 payload,
base64, SHA-256을 만들고 `$result`가 실제 반환 ID를 전달한다.

규제는 실제 procedure와 version을 먼저 만든다. 그 version의 제출
원문을 canonical로 대조한 뒤 `recordSubmission`을 실행하고, 반환된
SUBMITTED version으로 독립 SELL/DISPATCH 결정을 기록한다. 제출 event와
결정 event는 다르다. 성공 행이나 canonical ID를 SQL로 만들지 않는다.

## 보존과 재현

`python3 verification/cases/T09/author_sale_sources.py`로 재현한다.
`python3 -m unittest discover -s verification/cases/T09 -p test_sale_sources.py`
는 원래 94개 assertion의 raw bytes, 원래 action 순서/요청, 다른 7개
subcase, 기존 grant와 원문을 검사한다. strict inverse는 생성된 전체
입력과 일치해야만 이전 authoring 계층을 복원하며 변조를 거부한다.
`source_contracts.py`의 T09 전용 wrapper만 새 계층을 벗긴다.
T11/T12는 기존 코드 경로와 복원 결과를 유지한다.

## 실제 검증 경계

`python3 verification/cases/T09/run_sale_source_pg_test.py`는 이 작업
트리에서만 npm 의존성과 JAR를 준비한다. 자체 Spring, 임시 RSA key,
무작위 포트의 disposable PostgreSQL, 전용 blob 폴더를 사용한다.
전체 scenario/model runner와 외부 client를 호출하지 않는다.

각 실행의 `.local/t09-sale-source-pg-<uuid>/`에 command/exit, JAR/source
hash, HTTP 원문과 SQL 행을 남긴다. key는 종료 시 삭제하고 Spring과 DB를
정리한다. 이전 RED 실행도 보존한다. 이 한정 실행은 전체 시나리오나
실모델/운영 규제/BTP 인수 PASS가 아니다.

baseline의 `cumulative-versus-state` warehouse에는 이미 `getEvidence`가
있다. `exists-versus-end-throughout` warehouse에는 없다. 어느 쪽도
수정하지 않는다. 후자의 공개 증거 조회 거부를 확인하며, 내부 eligibility
권한 분리는 별도 제품 writer가 소유한다. 수령 contribution=0 문제는
별도 assessment/receipt admission writer의 범위다. 두 seam을 입력이나
oracle 변경으로 감추지 않는다.

## 2026-10-23 한정 실행 결과와 인계

작업 트리는 `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-748a2371`,
branch는 `fix/t09-synthetic-source-fixtures`다. 지정 runtime은
`codex/gpt-6.1-sol/medium/serviceTier=ultrafast`다.

- 보존/생성/변조 검사 6개와 기존 source 검사 12개가 통과했다.
- `./verify prepare`는 exit 0이다. 전체 인수 PASS가 아니다.
- 실제 HTTP/PG의 원래 입력 거부, 원문/미검증 거부, 잘못된 grant,
  typed 입력 변조, 규제 없는 6개 원천 검사 5개가 통과했다.
- 후속 단일 HTTP/PG 검사는 두 fixture의 6개 typed basis를 각각
  document/event/claim/verification/canonical/decision과 SQL로 대조했다.
  source actor의 공개 `getEvidence`는 200이다. 원래 warehouse는
  cumulative에서 200, exists에서 403/FORBIDDEN으로 기존 권한을 유지한다.
- 규제 없이 보낸 원래 `reserve60` [0,60) BOX 및 `reserve` [0,100) BOX는
  모두 REJECTED/INSUFFICIENT_ELIGIBLE_QUANTITY이며 allocation 0이다.
  실패 전후 물량/이동/배분 행도 비교했다.

완전한 신규 입력 실행의 마지막 실제 성공은 두 fixture 모두
`t09-sale-source-sell-regulatory-prepare`다. 다음
`t09-sale-source-sell-regulatory-submission-document`의 HTTP 전송 전에
공유 `ReferenceResolver`가 `Assertion references must bind identity, not
observed business values`로 중단한다. 실제 응답의 `procedureId` 및
`procedureVersionId`가 `$encode` identity allowlist에 없기 때문이다.
가짜 ID나 별도 encoding으로 우회하지 않았다. 이 공유 helper는 현재
소유 범위 밖이다. 필요한 최소 연결은 실제 UUID 필드
`procedureId`/`procedureVersionId`/`versionId`의 엄격한 허용과 회귀 검사다.

기존 `test_assessment_contracts.py`는 6개 통과, 과거 fixture/feature 보존
검사 1개 실패다. 이 검사는 새 가상 입력 계층의 strict inverse를 아직
호출하지 않는다. 기대값을 바꾸지 않고 T09 inverse만 연결하는 별도
소유권 결정이 필요하다. 현재 범위의 기존 source 검사는 이미 그 방식으로
연결했고 T11/T12 복원 결과는 동일하다.

이 때문에 완전 규제 graph, 그 후의 긍정 reserve/replay/pick/dispatch,
독립 category/규제/expiry/correction 반례와 원래 94개 runtime oracle의
전체 실행은 미완료다. 테스트는 추가했으나 PASS로 보고하지 않는다.
공유 UUID 연결 후에도 원래 exists warehouse의 내부 증거 인가와
cumulative receipt contribution=0은 다른 제품 writer가 소유한다.
제품 패치 통합 없이 이를 fixture/grant/기대값으로 숨기지 않는다.

증거는 다음 디렉터리에 남겼다. 각 디렉터리에 실행 command/exit,
source/JAR hash, HTTP 원문, 독립 SQL, 실패 또는 JUnit 결과가 있다.

- 첫 진단 실행: `.local/t09-sale-source-pg-7b844fb1-bcf2-4b5e-a212-1fe573d0dac5/`
- 실제 UUID encoding 차단: `.local/t09-sale-source-pg-74dddca8-686d-47b6-8da6-0854b20fbd8f/`
- 중간 반례 실행: `.local/t09-sale-source-pg-8f0de500-c588-4e6b-83c5-5e21da7d9fc4/`
- 현재 반례/SQL 실행: `.local/t09-sale-source-pg-3daa6434-0251-4601-82a4-a0d5b2cdca65/`
- 최종 source 공개 권한/SQL 실행: `.local/t09-sale-source-pg-dc04d31c-3756-489b-b101-0b85694862f4/`
- 정리 확인: `.local/t09-cleanup-audit.json`
- 정적 검사: `.local/t09-python-tests.log`, `.local/t09-prepare.log`,
  `.local/t09-prior-assessment-tests.log`

모든 실행의 임시 RSA key를 삭제했고, 자체 Spring 종료와 disposable
PostgreSQL container 삭제를 확인했다. DB 원문 행은 증거로 남아 있다.
원격 push, Grok 알림, 추가 위임, 외부 모델/과금/BTP 실행은 하지 않았다.
