# 대체 쓰기 경로와 내부 이동의 테스트 계약

직접·nested·atomic batch·projection·MCP·worker·blob·management
각 경로에서 물량·배분·업무·승인·거래·증거·identity·정의·정책·
운영 family의 인가를 검사한다. 이 route inventory는 새 구현이
공개할 모든 효과 class를 인수하기 위한 계약이며 존재하는 endpoint
주장이 아니다. 현재 없는 경로는 NOT_IMPLEMENTED/NOT_RUN이다.

각 거부의 전후 DB scope를 보존한 뒤 권한 있는 same-route 입력과
COMMITTED 원행으로 진입점을 대조한다. raw core CRUD는 공개하지
않으므로 실제404와 업무 원행 불변을 확인한다. 한 atomic changeset의
허용 RECORD와 금지 출고는 허용 부분까지 rollback해야 한다.

내부 W→W2 이동의 실제20은 허용 대조다. 고객 장소 ID 교체, action
이름 변경, raw primitive, 반품/폐기 effect class 변조는 출고·정상
주문 이행·반품·폐기를 생성하지 못한다. 이미 발생한 무허가 이동은
별도 RECORD 주체/원천/실물 scope의 claim과 대조 책임으로 남긴다.
해당 claim은 새로운 출고 허가나 정상 Goal 충족을 만들지 않는다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.

## Step 2 재검토 2차 보완

`author_review_fixes.py`가 아래를 소유하고 `case.json`·`scenario.feature`·
`observation-bindings.json`을 다시 만든다(`--check`로 drift를 검사한다).

- route×family 80개 subcase: 대표 capability의 주 효과 원천을 C3
  `CAP_EFFECTS`와 같은 표로 `effect-before`/`effect-after`에서 조직
  범위로 관찰하고 `unchanged-effect-<원천>`으로 exact 대조한다. 거부된
  reader command가 COMMITTED가 아님을 `reader-command-not-committed`로
  확인한다. 공용 20개 원천에 없는 효과(예: moveQuantity의
  logisticsMemberships·custodyHandovers)를 놓치지 않는다.
- 위임 있는 대조 호출은 같은 payload·revision에 자기 멱등키
  `V4-<subcase>-authorized`를 쓴다. batch operations와 blob businessAction
  안의 키도 함께 바꾼다. 다른 주체가 같은 키를 쓰는 계획 §7.3 경계가
  권한 증명에 섞이지 않는다.
- `exposed-write-surface`: 고정 route inventory는 "존재하는 endpoint
  주장이 아니다". 이 subcase가 실행 중 시스템의 노출 면을 열거한다.
  READ grant 주체 reader로 MCP `server/discover`·`tools/list`를 raw wire로
  읽고, host 조작 `enumerateWriteSurface`가 OData `$metadata`(entity
  set·action·function), MCP 두 면, worker handler registry, 관리/actuator
  endpoint를 읽은 뒤 열거된 모든 쓰기 가능 항목에 direct 생성·수정·삭제,
  deep insert, upsert, `$batch` changeset, draft activation, nested
  navigation 생성·수정·삭제, bound/unbound action, MCP tools/call, worker
  제출, 관리 endpoint 쓰기를 시도한다.

### enumerateWriteSurface 계약(구현 전)

요청 parameters는 scope, environmentId, enumerationId, actorRef(reader),
surfaces 5개, probeClasses 15개, allowlistRef
(`contracts/acceptance-capabilities.json`)와 그 sha256, targetPolicy다.
identity는 environmentId·enumerationId다. 독립 extractor rawRows는 다음을
낸다.

| rawRows | 행 | 필드 |
|---|---|---|
| surfaces | 읽은 노출 면마다 하나 | surface, sourceRef, itemCount, sha256 |
| surfaceItems | 열거한 항목마다 하나 | surface, itemKind, name, writeCapable, capabilityId, allowlisted |
| probeCoverage | probe class마다 하나 | probeClass, enumeratedTargets, probedTargets, complete |
| probes | 시도마다 하나 | surface, target, probeClass, outcome, committed, transcriptRef |

`allowlisted`는 allowlist 파일의 capability id와 항목의 capabilityId를
대조한 결과이고, `complete`는 enumeratedTargets와 probedTargets의 일치다.
probe의 outcome은 REJECTED·NOT_EXPOSED·APPLIED·UNKNOWN 중 하나다.
판정 assertion은 목록 밖 쓰기 가능 항목0, MCP tools/list=allowlist,
probe class 15개 완전 시도, probe commit0·APPLIED0·UNKNOWN0, 전후 DB
원행 불변(공용 16개와 모든 capability 주 효과 원천), reader COMMITTED0이다.
위임 있는 delegator의 MCP moveQuantity 대조 호출은 APPLIED·COMMITTED 1회다.

`enumerateWriteSurface`는 아직 `contracts/acceptance-host-observation.schema.json`
operation 목록과 `verification/host-observation-guide.md` 표에 없다. harness
소유자가 추가하고 Step 3 adapter가 구현하기 전에는 이 subcase가
NOT_IMPLEMENTED이며 V4 노출 면 부분은 NOT_RUN이다. catalog의
`V4.all-alternate-write-paths` same-auth-path 연결 갱신은 catalog 소유자의
일이다.
