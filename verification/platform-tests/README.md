# 플랫폼 인수 계약

사용자 Step 2에서 제품 부재와 oracle 오류를 구별하려고 T23/V8의
실행 선언과 assertion을 작성했다. baseline은 `step2-b2`의
`feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다. 이 산출물은 새
schema, migration, 실행 adapter나 제품 PASS를 제공하지 않는다.

## 작성한 범위

- `T23`은 파일 archive roundtrip, authoritative 자료의 fresh/live 분기,
  Flyway-only DDL과 compiler drift, cutover 순서 및 개방 전후 복구다.
- `V8`은 fresh 설치, 실제 제약과 현재 인가, rollback, 두 transaction의
  잠금, 새 ontology v1→v2 보존과 재인수, 완전/불완전 복원, 실제
  BTP/client 및 환경별 미실행 gate다.
- `build_cases.py`는 독립 기대값으로 JSON/Gherkin을 생성한다. 서비스나
  상태 전이를 구현하지 않으며 관측값을 기대값으로 계산하지 않는다.
- `PlatformAssertionTest`는 실제 `AssertionEngine`에 고정 관찰 표본과
  mutant를 넣는다. 가짜 API/DB/worker/process를 실행하지 않는다.

## 수량과 책임의 독립 계산

진행 중 v1 fixture의 원 물량은 Q100이다. Q100은 retired이며 active
leaf는 W의 A60과 운송 중인 B40이다. 현재 실물은 60+40=100 BOX이고
W 보유는 60 BOX다. 누적 인정 수령은 canonical R60 한 사건의 60 BOX다.
목표100의 잔여40과 인간 procurement의 다음 행동·확인시점이 남는다.

기존 예약은 A60의20 BOX다. 두 거래가 같은 A60에 신규40/30을 경합하면
먼저 확정된40과 기존20은 총60이다. 후행30은 stale revision으로 거부돼
실물 초과를 만들지 않는다. split20의 감사/outbox/멱등 결과 실패에서는
원장·계보·배분·의무·감사·outbox·COMMITTED 결과 전체를 전후 대조한다.

live 자료 분기 연습의 source input은 import60, unmapped 제조7,
quarantine3으로 총70 BOX다. 새 수입 실적으로 옮길 수 있는 것은 명시적
mapping의60뿐이며 원본7과 미확인3을 없애지 않는다. source 의무의
owner·다음 행동·확인시점도 보존한다. 이 fixture는 실제 운영 자료가
있다는 증거가 아니다. R3 실제 inventory는 여전히 별도 인수다.

## host와 API/DB의 증거 연결

공통 host guide의 23개 operation 중 좁힌 operation만 호출한다.
`process`의 명령 ACK는 결과가 아니다. 다음을 함께 요구한다.

1. input descriptor에는 실제 path/hash/bytes/scope를 둔다. 생성 output의
   미래 hash를 fixture에서 seed하지 않는다.
2. schema/backup/restore/cutover/client 등 생성 작업의 actual
   `generatedOutputs`를 strict result ref로 `inspectArtifacts`에 넘긴다.
3. 모든 host assertion의 source는 실제 extractor가 읽은 JSON artifact의
   `hostObservation.extractor.rawRows.facts`다. 그 JSON은 guide의
   evidenceClass/operation/command/environment/driverProvenance/scope와
   actual artifact descriptor에 exact 연결돼야 한다.
4. `facts`는 아래 테스트 JSON이 요구하는 원 행과 계산 입력을 담는다.
   단일 Boolean, 서버 summary 또는 파일 존재로 conjunction을 대신하지
   않는다. 예를 들어 제약은 실제 삽입의 SQLSTATE, schema는 실제 DDL
   writer/차이 목록, 외부효과는 operation ID와 발행 count를 반환한다.
5. DB는 read-only 별도 connection의 원 행과 scope/snapshot/query를
   반환한다. active leaf를 명시적으로 선택하고 retired parent를 제외한다.
   API 수량과 DB 수량·관계·owner·version·의무·audit·outbox를 대조한다.

`schema-delta-contract.json`은 compiler/허용 delta의 테스트 입력이다.
제품 CDS/DDL이 아니다. 새 ontology schema source와 compiler 생성물은
Step 3에서 만들고 actual artifact로 인수한다. 옛 ERP schema나 코드는
migration source로 읽거나 재사용하지 않는다.

archive의 보존 후보는 계획에 기록된 옛 main commit
`ae02ff63751714510db4d93a39498b8d51b3697c`다. 이번 작성에서는
`git cat-file -t`로 commit object 존재만 확인했다. 보존 inventory와
isolated restore는 tree metadata와 파일 hash만 읽어 old source 보존을
검증해야 한다. 옛 구현 파일의 내용·명령·schema는 읽지 않는다.

## 복원과 cutover 경계

복원 bundle은 DB/blob/definition/capability/evaluator/skill/non-secret
config/deployment manifest를 포함한다. blob/evaluator 누락 fault는 원
backup을 보존한 별도 restore sandbox에서 해당 artifact를 접근 불가로
만든다. fault가 반환한 actual restoreInputArtifacts를 restore에 넘긴다.
불완전한 복원에는 COMPLETE record0, 쓰기 차단, 인간 복구 책임을 요구한다.

완전 복원은 원본 artifact bytes/hash를 먼저 검증하고 삭제 journal과
legal hold를 다시 적용한다. 보존된 DB 상태와 v1 판정·원문 hash·현재
인가를 검사한다. 삭제된 blob의 재활성화는0이고 legal-hold blob은 남는다.

cutover는 freeze→최종 snapshot→자료/의무 대조→version 적용→smoke와
현재 인가/재개→쓰기 개방을 각각 실행한다. 개방 전 실패는 검증된
snapshot과 호환 artifact로 복구한다. 개방 후에는 새 발주 proposal과
MANAGER 승인, 외부 발주 응답 유실을 실제 행동으로 수행하고 접수 중지와
outbox freeze, 외부 성공 대조 뒤 repair한다. external issueCount1과
새 주문 보존, replay/보상 결정, 남은 owner를 검증한다. SQL rollback이
실물/계약 취소라는 주장은0이다.

## manifest와 미실행 gate

`installFixture`의 실제 adapter는 `data.runtimeBindings`에 검증한
non-secret manifest/binding을 반환할 수 있다. 여기에 없는 R1 selected
stack manifest, R7 costApprovalRef, exact clientId/clientVersion와
syntheticMappingApproval를 임의 값으로 생성하지 않는다. 필수 binding
부재는 UNAVAILABLE/NOT_RUN이다. case의 strict result refs는 이를
현재 실행 입력과 연결한다.

exact CAP 후보 버전은 조사 문서에서 고정했다. actual runtime version은
별도 검증한 선택 manifest와 대조한다. SDK와 지원 client를 확인하지 않은
후보를 최종 stack으로 보고하지 않는다. 후보 변경은 실제 S0 판정과
동등한 oracle 증거에 따라 입력을 갱신해야 한다.

BTP probe에는 실제 region/entitlement/cost 승인·buildpack·datasource
binding·TLS peer/host 검증·issuer/audience/조직/현재 grant 및 wire가
필요하다. client probe는 실제 설치 version과 host의 discovery/loading/tool
artifact를 요구한다. local/BTP/client의 결과는 분리한다. 미확보 BTP,
client 또는 실자료를 local 성공으로 대체하지 않는다. client probe는
유료 모델 실행을 허용하지 않으며 비모델 loading 관찰이 없으면 NOT_RUN이다.

## 실행 기록

`checks.md`에 실제 command/exit/count/한계를 기록한다. `subcase-index.json`은
coordinator의 registry 통합용 준비 입력이다. 최종 coverage/evidence manifest
및 registry는 이 writer가 변경하지 않는다. 제품 profile과 실제 모델·유료
배포의 결과는 NOT_RUN이다.
