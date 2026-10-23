# T17 조건 oracle와 실제 원천 설치 계약

고정된 업무 status를 실제 evaluator의 조건 결과에 대조하고, 근거 ID는
독립 원천 관계와 실제 setup receipt에 각각 대조한다. 서로 다른 저장
grain과 fixture alias를 같은 evidence ID로 취급하던 열 개 assertion을
교정한다. 제품·observer·ReferenceResolver는 이 worker가 수정하지 않는다.

기준선은 `d0885a8031eb754e42f35f998a6d7f970b059cee`이며 branch는
`fix/t17-condition-oracle-contract-20261024`다. worker의 실제 provider와
model은 `codex_2`와 `gpt-6.1-sol`이다. 위임 요청의 runtime option은
`reasoningEffort=xhigh`, `serviceTier=priority`이며 catalog가 두 값을
지원함을 확인했다. 추가 위임·API 구매·push·PR·Grok 알림은 하지 않았다.

## 원본 실패와 대응

root의 `.local/resume-20261024/semantic-native-preparation/manifest.json`이
가리키는 frozen `f8bb11e9` T17-S01–S05 보고서를 읽었다. 원본은15개
subcase·257개 assertion이며165 PASS와92 FAIL이다. 이는 과거 실행
결과다. 파일별 절대 경로와 SHA-256은 이 tree의
`.local/t17-condition-proof/original-report-audit.json`에 보존했다.
원본 보고서와 artifact는 수정하지 않았다.

`fix-wave-20261024-sales.md`의 actual producer 계약에 따르면 기존
`rawRows.conditions`는 permission·restriction 등 heterogeneous 원행이다.
observer의 `conditionResults`는 Work의 immutable AssessmentReferences다.
둘 다 현재 SELL eligibility evaluator의 조건 결과가 아니다. 원래
`qc-proof`·`agency-proof`는 새 설치된 document/canonical ID가 아니고,
실제 evaluator는 `quality-eligibility-v1`이다. 실제 evidenceRefs는
없거나 하나 또는 여러 개일 수 있다.

다음 다섯 subcase의 `eligibility-response-1`과 `-2`를 교정했다.

- `eligibility-unknown`
- `eligibility-conflict`
- `eligibility-fefo-no-reason`
- `eligibility-fefo-with-reason`
- `eligibility-packaging-unapproved`

`-1`의 ID와 `relationSet`을 유지하고 API의
`segmentId/conditionId/status/evaluatorVersion`을 고정 기대와 비교한다.
원래105개 `segment/condition/status` tuple을 모두 보존했다. QC,
REGULATORY, LABEL, DISPOSITION, CUSTOMER_CONTRACT와 모든 원래
ALLOWED/UNKNOWN/CONFLICT/DENIED를 유지했다. evaluator 상수는 등록된
actual producer 계약에서 고정했으며 관측 결과에서 복사하지 않았다.

`-2`의 ID와 `sameAs`를 유지하고 API의
`segmentId/conditionId/evidenceRefs`를 독립
`rawRows.sellConditionSourceRefs`와 비교한다. 이 source는 실제 segment
ID와 QC,REGULATORY,LABEL,DISPOSITION,CUSTOMER_CONTRACT 순서를 지키며
각 evidenceRefs를 정렬한다. 상태·evaluator·적격량은 생산하지 않는다.

추가 `relationSet`은 `rawRows.sellConditionSourceLinks`의 segment,
condition, evidenceId, sourceTable을 실제 설치 receipt identity와
대조한다. 품질 permission 문서, 규제/label canonical 원본, 실제
`early-hold-source-document` 응답 ID를 각각 사용한다. exact set이므로
누락·추가·다른 proof도 실패한다. sourceId/sourceRow/verificationChains
존재도 별도로 요구한다. 허용 permission과 현재 QC hold는 둘 다
대조하며 permissionStatus=ALLOWED, restrictionStatus=DENIED와 원래
semantic QC DENIED를 독립적으로 고정한다.

receipt의 원래 `/pointer`와 `/given` 전체를 고정 fixture와 비교한다.
receipt index를 original condition index라고 가정하지 않는다. UNKNOWN의
pointer·given·명시적 미설치 사유도 exact set으로 검증한다.

## ITEM P와 ALT의 분리

원래 packaging assertion에는 item ALT의 OTHERPACK 다섯 tuple이 있으나
기존 query/observer는 ITEM P다. `eligibility-alt` 공개 query와 별도
`eligibility-alt-db` observer를 추가하고 같은 ALT scope와 API snapshot을
연결했다. 원래 P의 `-1`은 P tuple20개, 추가 ALT의 고정 assertion은
OTHERPACK tuple5개를 검사한다. 원래105개 tuple은 둘을 합쳐 정확히
보존된다. P observer에 ALT 원행을 섞지 않는다.

P 주문에 OTHERPACK을 예약하려는 원래 요청·거부 기대는 유지한다.
fixture·grant·clock·원본 조건 status는 한 byte도 바꾸지 않았다.
ALT 읽기 권한은 기존 observer의 all-of grant에 이미 있다.

## 실제 REGULATORY CONFLICT 설치

`T17SourceFacts`는 원래 B/REGULATORY CONFLICT를 건너뛰던 경로 대신
두 개의 synthetic immutable 원본을 공개 API로 설치한다. 기존
`agency-proof`와 `claim-proof` 각각의 원본 alias·provenance metadata,
원래 condition pointer와 condition 전체를 각 문서에 보존한다. 실제
scope·action SELL·[0,20) BOX·validFrom/validUntil도 일치시킨다.
이 문서는 실제 법규나 운영 승인을 주장하지 않는다.

각 proof는 attachEvidence → recordActivity → matchSourceIdentity →
linkCanonicalOccurrence를 통과한다. 실제 prepareRegulatoryProcedure와
검증된 recordSubmission 뒤 같은 현행 SUBMITTED procedure version에
ALLOWED와 REVOKED를 각각 recordRegulatoryDecision으로 기록한다.
predecessor를 연결하지 않으므로 둘 다 current decision head다. 이어지는
원래 LABEL 입력은 같은 procedure version을 실제 verifyLabel로 검증한다.
REJECTED는 procedure version을 전진시키므로 대체로 쓰지 않았다.
verification·decision·permission 행을 직접 seed하지 않았다.

충돌 receipt는 원래 `/pointer=/baseline/conditions/6`와 `/given`을
보존하고 `decisionReceipts[0]`, `[1]`에 각각 document/event/review/
canonical/typedDecision ID, original alias, 실제 입력 decision을 남긴다.
충돌 receipt 자체에는 singleton documentId/canonicalOccurrenceId를
만들지 않는다. 두 proof의 ALLOWED/REVOKED와 원래 두 alias도 고정
assertion으로 확인한다. UNKNOWN은 positive source를 설치하지 않는다.

setup action에서 필요한 identity pointer는 다음과 같다.

- `/data/sourceFactEvidence/conditions/N/documentId`
- `/data/sourceFactEvidence/conditions/N/canonicalOccurrenceId`
- `/data/sourceFactEvidence/conditions/N/decisionReceipts/0/canonicalOccurrenceId`
- `/data/sourceFactEvidence/conditions/N/decisionReceipts/1/canonicalOccurrenceId`

coordinator의 별도 `2342545e`가 이 경로의 canonical UUID만 허용한다.
status·수량·evaluator를 결과에서 expected로 복사하는 권한은 없다.
이 tree로 그 commit이나 observer `8e8e2195`를 가져오지 않았다.

## 가역성·검증·인계

`condition-oracle-delta.json`은 d088 기준61개의 정확한 before/after
record를 가진다. 원래257개 ID의 상대 순서·op·oracleRef와 나머지247개
assertion 전체를 유지한다. 독립 원천 검사44개를 추가해 현재 총301개다.
추가 feature step은44개 assertion과 ALT 행동2개다. inverse는 추가분을
제거하고 원래 잘못된 mapping까지 정확히 복원한다. 부분 삭제나 status,
source, receipt delta 변조는 실패한다.

`author_public_contracts.py`의 기존 계층을 모두 재생성해 sourceDrift=[]를
확인했다. historical generator tests는 새 계층을 먼저 역변환한 뒤
기존257개 원문을 계속 대조한다. 단순히 기대를 새 출력으로 바꾸지 않았다.
fixture15개의 원본 byte와 SHA-256을 ledger/test에서 비교한다.

실행 명령·exit code·JUnit XML·source/JAR/fixture hash·소유 자원 cleanup은
`.local/t17-condition-proof/handoff.json`과 연결 artifact에 기록한다.
Java21.0.12.1, Maven3.9.16, locked PostgreSQL image를 사용했다. Node는
설치된26.9.0이며 package의24.x 권장 범위와 다른 engine warning을
보존했다. 같은 lock의 offline dependency로 이 tree의 CSN/backend JAR를
생성했으며 다른 tree의 산출물을 복사하지 않았다.

- Python38개는 실제 generator 고정점·정확한 inverse·원래 tuple/ID/fixture
  보존과 기존 의미 있는 회귀를 검사한다.
- focused JUnit9개는 기존 source validation6개와 새 condition assertion3개다.
  새 검사는 조건 삭제, UNKNOWN/CONFLICT/DENIED를 ALLOWED로 변경,
  evaluator 바꿔치기, duplicate, hold 누락, alias/foreign proof, 원본
  conflict alias 중복·provenance 누락을 거부한다. controlled witness이며
  제품 runtime 인수로 세지 않는다.
- 실제 HTTP/PostgreSQL2개는 원본 conflict fixture의 두 verified chain,
  원본 blob bytes/hash·source provenance·scope/수량/시간·current head를
  대조하고 고정 업무 status를 실제 API에 검사한다. UNKNOWN 근거0,
  QC permission/hold 근거2, P20/ALT5 조건 분리, 원래 P 주문의 대체 거부와
  배분0도 확인한다. scenario runner를 호출하지 않는다.
- 첫 compile의 Java 문법/API 오류와 수정 후 성공 로그를 모두 보존했다.
  `./verify prepare`는 기존 ReferenceResolver가 새 identity pointer를
  거부해 exit3이다. `2342545e`를 통합한 뒤 exact resolver와 독립 observer를
  붙인 전체 assertion 검사는 root가 수행해야 한다.

정식 recall 입력의 ACTIVE RECALL Work/openInvestigation 공백은 이 범위가
아니다. 원래 recall 기대를 그대로 남겼다. T11의 intake owner 기대도
변경하지 않았다. full scenarios·실모델·BTP는 NOT_RUN이며 실제 시나리오
인수는 통합 뒤 Haiku5.5 dynamic Workflow가 맡는다. 이 focused 인계로
Task·Step·S5·S6를 닫지 않는다.
