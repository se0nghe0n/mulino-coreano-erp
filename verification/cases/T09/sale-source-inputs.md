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
정확한 `/response/effects/procedureId`와
`/response/effects/procedureVersionId`의 엄격한 UUID 허용과 회귀 검사다.
일반 `versionId`나 접미사 전체 허용은 포함하지 않는다.

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

## 같은 입력 구현의 후속 연결

Coordinator가 공유 resolver와 T09 assessment inverse 연결을 명시적으로
허용했다. Task와 현재 Step은 OPEN이며 전체 작업을 BLOCKED로 판정하지
않는다. 위의 UUID 연결과 과거 보존 검사 문제는 이 후속 변경으로 해소했다.

`ReferenceResolver`는 실제 생성기가 사용하는 두 경로
`/response/effects/procedureId`와
`/response/effects/procedureVersionId`만 별도로 허용한다. ordinary
reference와 `$encode` 모두 canonical UUID를 요구한다. 일반 `versionId`,
임의 접미사, 수량·상태·업무 관찰값은 새 identity가 되지 않는다.
기존 delivery/assessment 검사와 위조 경로·비UUID·빈 값·타입·null·미실행
결과 검사를 포함한 `ReferenceResolverTest` 19개가 통과했다.

T09의 strict inverse는 `assessment_contracts.reverse/original_fixture`에서
한 번만 호출한다. 기존 source 검사에 있던 중복 wrapper를 제거했다.
변조된 기존/신규 actor, grant, action, fixture identity와 feature는
거부한다. 269개 semantic oracle과 126개 binding ledger를 유지한다.
후속 Python 전체 검사 26개와 T11 parent 보존 검사 6개가 통과했다.

첫 후속 전체 HTTP/PG 실행은
`.local/t09-sale-source-pg-bfa6f204-6b15-46c2-b848-f4ab10382290/`다.
10개 중 7개가 통과했고 3개가 실패했다. 두 fixture 모두 새 QC와 규제
공개 사슬 전체가 통과했다. 다음 남은 차이를 발견했다.

- cumulative의 실제 confirmReceipt 반환 leaf는 revision 0이다.
  원래 `reserve60.expectedRevision=1`은 보존했으므로 완전한 SELL 원천을
  만든 뒤에는 `STALE_REVISION`이 드러난다. 입력이 부족했던 과거 실행은
  이 검사 전에 `INSUFFICIENT_ELIGIBLE_QUANTITY`로 끝났다.
- exists의 원래 warehouse 공개 `getEvidence`는 여전히 FORBIDDEN이고,
  완전한 원천 이후에도 예약은 INSUFFICIENT_ELIGIBLE_QUANTITY다.
  이 내부 권한 경계는 별도 제품 writer의 범위다.
- correction 반례는 최초 문서와 같은 recordedAt으로 supersession을
  요청해 DB의 `Invalid immutable supersession`에 걸렸다. 별도 반례에서만
  fixture 시계를 1초 전진시키도록 고쳤다. 원래 시나리오 시계는 유지한다.

원래 reserve 요청의 revision을 임의로 바꾸거나 실제 leaf revision을
SQL로 덮지 않는다. fresh getObject로 원래 요청의 expectedRevision만
연결하는 별도 범위 결정이 필요하다. 각 HTTP snapshot에 94개 원래
assertion의 관측 결과와 미도달 action을 담은 `*-oracles.json`을 추가한다.
이 파일은 한정 검사의 관측 기록이며 전체 scenario acceptance receipt가
아니다. 다른 독립 fixture의 assertion은 해당 instance에서 NOT_RUN이다.

### 후속 재실행의 정확한 frontier

`.local/t09-sale-source-pg-54af05c6-d17c-4f1b-b4b6-4dc967c691c5/`에서
전체 10개 test를 다시 실행했다. 7개 PASS, 3개 FAIL, ERROR/SKIP 0이다.
원천 actor가 조회한 실제 SELL 적격량은 cumulative 60 BOX, exists 100 BOX다.
두 fixture 모두 6개 typed basis와 2개 규제 결정의 SQL 연결을 통과했다.
SELL 원천만 있는 별도 반례에서 DISPATCH 적격량은 0이다.

실패 셋은 원래 `reserve60`의 STALE_REVISION 두 경로와 correction 한
경로다. cumulative 실제 leaf revision 0에 대해 원래 요청의 1을 그대로
보냈다. pick/dispatch를 실행한 결과로 표시하지 않는다. exists는 기대한
내부 인가 seam을 확인하는 test가 통과했으나, 원래 예약 이후 행동은 여전히
실행되지 않았다. 94개 assertion별 미도달 목록은 각 `*-oracles.json`에
있으며, target 두 subcase의 47개 중 2개 PASS와 45개 NOT_RUN이다. 나머지
7개 subcase의 47개 oracle은 이 bounded 실행의 대상 밖으로 NOT_RUN이다.

Correction 후속 probe는
`.local/t09-sale-source-pg-9aae1da4-8552-4a9e-a313-f11bf1af1289/`에 있다.
시계 전진 후에는 `Duty needs active work`가 확인되어, 이 별도 반례에서만
기존 A의 공개 권한으로 판매 Work의 최신 revision을 읽고 `activateWork`를
시도했다. 실제 응답은 NEEDS_INPUT / GOAL_SLOT_MISSING,
`Missing goal slot: timezone`이다. `createSalesOrder`가 생성한 실제 판매
Work는 DRAFT이고 그 goal에는 timezone이 없다. 이 계약은 backend의
소유 범위이므로 fixture의 성공값이나 goal patch로 대신하지 않는다.
원래 case actions, 상태, 시계와 94개 assertion은 변경하지 않았다.

수정 후에도 실제 correction 적용, 양성 reserve/replay/pick/dispatch와
그 뒤의 원래 oracle는 미완료다. source correction test는 이를 FAIL로
남긴다. 원래 reserve revision의 명시적 연결 결정, 판매 Work의 활성화
가능한 goal 계약, 별도 내부 증거 인가 patch가 다음 통합 seam이다.

최종 정적 준비는 `.local/t09-expanded-prepare-stable.log`의 exit 0이다.
앞선 `.local/t09-expanded-prepare.log`의 exit 1은 문서를 기록하던 시각과
생성기 임시 복사 검사가 겹쳐 `sale-source-inputs.md`만 달랐던 실행이다.
문서를 고정한 재실행은 통과했으며 두 실행을 보존한다.

독립 원문 대조 결과는 `.local/t09-regulatory-original-audit.json`이다.
두 fixture의 SELL/DISPATCH 총 네 결정에 대해 문서 원문 hash, event payload,
claim/verification/canonical의 조직 join, 실제 procedure/version ID,
수량·단위를 확인했다. 제출은 PREPARED version, 결정은 SUBMITTED version을
가리키며 versionId 일반 허용을 사용하지 않는다. 정리는
`.local/t09-cleanup-audit.json`에서 여덟 실행 모두 key 부재, container 부재,
Spring 종료를 확인했다. 모든 결과는 한정 검증이며 Task/Step은 OPEN이다.
