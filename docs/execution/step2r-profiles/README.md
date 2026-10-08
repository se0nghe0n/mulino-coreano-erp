# Step2 재검토 2라운드 profiles worker 실행 기록

- 사용자 Step 2(tests) 재검토 2라운드, Claude Opus high. Task HEAD
  `d21aee7c`(tag `step2r2-baseline`)에서 branch `step2r/profiles`,
  worktree `/Volumes/VideoStore/Developer/mulino-ontology-step2r-profiles`.
- 소유: `verification/cases/{E1,E2,T25,C3,T09,T12,T15,T23,V8}/**`와
  생성기(`verification/mcp-tests/author_cases.py`의 T25 부분,
  `verification/cases/C3/author_prerequisites.py`,
  `verification/platform-tests/build_cases.py`),
  `verification/requirements/**`, 해당 registry 항목.
- 제품 runtime·실제 모델·client·BTP·규제 검토는 실행하지 않았다.
  모두 NOT_RUN이다. registry는 바꾸지 않았다(subcase 수 797 그대로).

## 왜 profile 선언만으로 부족했나

`assemble.py`의 필수 profile 도달성은 profile 단위다. case가 `mcp`를
선언하면 그 case의 모든 assertion이 mcp profile에 연결된다. 1라운드에서
E1·E2·T25·C3에 profile을 추가하자 `Unreachable required profile`은
0이 됐지만, 관찰 단위로 보면 MCP 응답이나 skill loading을 하나도 읽지
않는 관찰이 남아 있었다. 그런 관찰은 MCP adapter가 다른 계산·캐시를
쓰거나 host가 skill을 읽지 않아도 통과한다. 이번 작업은 관찰마다 그
계층의 증거를 읽는 assertion을 두고, 이를 정적으로 검사하는
`verification/requirements/check_layer_routes.py`를 추가했다.

시작 시점(`d21aee7c`) 관찰 단위 MCP·SKILLS 누락은 내 case에서
E1 4, E2 2, C3 3, T25 7, V8 5개였다. 지금은 검토한 예외 1개
(E1 model-reference)만 남는다.

## 항목별 판정

| 항목 | 판정 | 처리 | commit |
|---|---|---|---|
| 1 E1 mcp | DONE | receipt60-doc-duplicate-effects·return-added-purchase-arrival·logistics-closes-unrelated-duties를 같은 snapshot의 MCP 조회로 대조. model-reference는 EXEMPT(아래) | `9c885ec7` |
| 1 E2 mcp | DONE | status-axes(API·MCP 양쪽 축), unauthorized-exception-close(after-operator-mcp) | `9c885ec7` |
| 1 C3 skills | DONE | model-query 30 subcase에 skillLoadingTrace DISCOVERED·BODY_READ stage와 MCP COMMAND·RECORD 의도0을 세 관찰에 연결 | `4daeb2c4` |
| 1 T25 mcp·skills | DONE | runtime-links-required가 관찰마다 필수 profile PASS link·artifactKind별 PASS artifact·profile별 결과 행을 요구. evidence-wrapper-fields에 mcp·skills·scenarios 기록/wrapper와 artifact 종류. actual-model-usage-required에 실행별 protocol transcript·skill loading | `dc6314ff` |
| 1 V8 | DONE | restore-artifacts·missing-blob/evaluator·legacy-migration·result-scope를 MCP wire 결과로 대조, MCP route subcase의 requiredAdapters에 mcp | `164c7f96` |
| 1 T09·T15 | DONE | API로만 보던 T09 final-held·T15 confirmed-eligible에 DB 원행, T09 initial-held에 API 값 | `0e41fb1f` |
| 1 T12·T23 | NOT_A_DEFECT | MCP·SKILLS 계층이 없고 profile 도달성은 coverage worker가 닫았다. 남은 것은 효과 관찰의 API 미연결뿐이며 명령 응답은 같은 oracle의 다른 관찰이 API로 읽는다 | - |
| 1 T04 | CROSS_OWNER(cases-c) | profile 도달성은 닫혔다. 관찰 단위로는 MCP·SKILLS 요구가 없고 DB 참고 1개(decimal-boundary/wire-decimals)와 API 참고 5개다 | - |
| 1 검사 도구 | DONE | `check_layer_routes.py`와 반례 test 3개 | `4f6ae884` |
| 2 T25 artifactKinds | DONE(catalog 정정) | 아래 판단 | `d84f2942`, `dc6314ff` |
| 3 의무 kind 어휘 | DONE(목록) | 아래 표. 이름은 바꾸지 않았다 | 이 문서 |
| 4 E1 catalog pin | DONE | `model-reference-host` descriptor를 새 catalog sha256 `c1187224…`·363267 bytes로 갱신 | `d84f2942` |

E1 model-reference 예외: 이 관찰은 host가 읽은 독립 입력(catalog oracle,
R8 상태, model plan)으로 실모델 gate가 업무 runtime과 분리됐는지 본다.
같은 oracle의 MCP 요구는 같은 subcase의 actual-runtime-observation이
runtime-mcp로 충족한다. MCP 업무 조회로는 모델 gate 분리를 증명할 수
없어 패딩 assertion을 만들지 않았다.

## 항목 2: T25 artifactKinds 판단

계획 §13.4는 coverage verifier가 D26·C5·V8·E2 각각을 실제 assertion과
artifact에 연결하는지 확인하라고 한다. T25는 그 verifier의 출력만
관찰하며 업무 API를 부르거나 업무 DB를 읽지 않는다. catalog의 T25 네
관찰은 499개 중 484개가 공유하는 기본값 `api_response`·`db_snapshot`을
갖고 있었다. 정상 제품도 T25에서 이 artifact를 만들 수 없으니 이
관찰은 올바른 제품에게도 닫히지 않는다. T25에 DB observe를 덧붙이면
coverage와 무관한 형식적 probe가 된다.

그래서 catalog가 틀렸다고 판단했다. 규범 lock 절차대로 별도 commit에서
artifactKinds를 `coverage_report`·`runtime_manifest`(evidence 두 관찰은
`wrapper_record` 추가)로 바꾸고 두 oracle의 `contractSha256`을 다시
계산했다. `reviewUpdates[2]`에 이유·이전 값·바뀐 관찰·약화가 아닌
근거를 적었다. 약화가 아닌 이유는 requiredLayers·expected·관찰 이름이
그대로이고, DB·API·MCP·SKILLS 증거 요구를 검사 가능한 곳으로 옮겨
강화했기 때문이다. T25 runtime-links-required는 이제 다른 492개
관찰마다 db_snapshot을 포함한 catalog artifactKind별 PASS artifact와
필수 profile별 PASS link를 요구한다. 결과로 `./verify prepare`의
`artifactKindAttributionGaps`는 4→0이다.

## 항목 3: 의무 kind 어휘(이름 변경 없음)

계획은 의무 kind 값을 하나도 이름 짓지 않는다. §5.3은 `Obligation`이
`kind` 필드를 가진다고만 하고, §6·§13은 의무의 의미만 서술한다. 아래
표는 coordinator가 공개할 어휘 계약의 입력이다. "S4 구현"은
`backend/src/main`(읽기만 함)에서 찾은 값이며 대응은 추정이다.

| case 값 | 쓰는 case·assertion | 계획 근거(의미만) | S4 구현 값(추정 대응) |
|---|---|---|---|
| `QC` | E1 independent-goals-and-owners QC-duty-5~11·hold60/hold40 identity·logistics-closes-36(+mcp)·two-entry-anchor-qc-*. 다른 case fixture에도 다수 | §6 수입·수령(QC 별도 계산), §13.3 E1 "QC/반품/정산 각각의 미해결 의무" | `QUALITY_REVIEW`(QualityResponsibilities.kind) |
| `RETURN` | E1 return-duty-12~18·logistics-closes-37(+mcp)·two-entry-anchor-return-* | §6 반품 "반품 기본 QC 보류, 교환/환불/정산 의무 별도", §13.3 E1 | `RETURN_QC_REVIEW`·`RETURN_COMMERCIAL_REVIEW`·`RETURN_SETTLEMENT_REVIEW`(ReturnCommands `"RETURN_"+…`), `RETURN_RECONCILIATION`. case의 한 값이 구현의 여러 값에 대응하므로 E1 `exactSet ['OPEN']`의 범위를 정해야 한다 |
| `SETTLEMENT` | E1 settlement-duty-19~25·logistics-closes-38(+mcp)·two-entry-anchor-settlement-* | §6 정산 "물류 목표가 충족돼도 정산 차이는 별도 미해결 목표", §13.3 E1 송장 차이5 | `SETTLEMENT_DIFFERENCE`(SettlementCommands) |
| `DELIVERY_DEFICIT` | C4 corrected-delivery-98 current-unresolved-deficit-6·deficit-owner-*, return-not-correction의 0건 확인(cases-b 소유) | §13.2 C4 "실제98 정정은 … 현재 유효한 의무2", §6 판매·사실 기록 | `DELIVERY_CORRECTED_DEFICIT`(DeliveryCorrection) |
| `RECALL_RESPONSE` | E2 overlapping-holds investigation-duty-13~19(+mcp), exception-responsibility exception-residual-duty-10~16(+mcp) | §6 반품·회수 "미확인은 … ADMIN 예외 결정과 잔여 책임 없이 종료할 수 없다", §13.3 E2 "잔여 책임/근거" | 조사 의무 `RECALL_INVESTIGATION`, 예외 잔여 `RECALL_EXCEPTION_RESIDUAL`, 제외 범위 `RECALL_EXCLUDED_SCOPE`. case는 두 의미에 한 값을 쓴다 |
| `SALES_PROMISE` | V2 actual50-correction promised-obligation-total-5(cases-b 소유), `promiseCoverage`의 `SHORTAGE_OBLIGATION` | §13.2 V2 "기존 의무40 … 부족 의무 보존", §4.2 "부족 의무와 대체 배분 대상" | `ALLOCATION_SHORTAGE`(StockPrimitives impact) |
| `QUANTITY_SHORTFALL` | T12 deadline-revision-and-correction corrected-debt2 | §6 구매·운송 부분 수령·납기 변경의 과거 위반 보존 | `RECEIPT_SHORTFALL`(수령 impact) |

같은 `kind` 이름을 쓰지만 의무가 아닌 값: E2 decisions의
`RECALL_EXCEPTION`(결정 종류), V3 slots의 `QC_RELEASE_CLAIM`(claim
종류), restriction의 `QC`·`REGULATORY`·`DISPOSITION`, outbox의
`BANK_TRANSFER`. 어휘 계약에서 의무 kind와 섞지 않아야 한다.

## 실행 증거

HEAD `4f6ae884`(문서 commit 전), working tree clean. 환경
`. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java 21.0.5,
Maven 명령은 `$MULINO_SLOT`로 하나씩 실행했다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 461 tests, failure/error/skip 0 |
| `./verify prepare` | 1 | FAIL, 41 case·797 subcase·23570 assertion. 문제 5개는 모두 T24 sweep-authenticated-reviewer `/provenance`(시작 시점과 같음, 내 소유 아님). `artifactKindAttributionGaps` 0(시작 4) |
| `./verify contract-red verification/cases/<ID>/case.json` E1·E2·C3·V8·T09·T15·T25 | 1 각각 | expected=discovered=NOT_IMPLEMENTED 실패 3·4·309·12·9·5·22, skip 0 |
| `./verify validate` E1·E2·V8·T09·T15 | 0 | schema·semantic 유효 |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | VALID, 122 oracle·499 관찰 |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 29 tests OK(catalog 25 + layer routes 4) |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, EXEMPT 1, KNOWN_OPEN 4(T20 3, V4 1) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(T01·T20 byte 동일, T25·C3 생성기 재현) |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 57 OK(276s) |
| `python3 verification/coverage/assemble.py` | 2 | NOT_RUN, preparationProblems 0, Unreachable 0 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN |
| `python3 verification/coverage/assemble.py --check-preparation` | 1 | FAIL. 유일한 문제는 fresh prepare 실패(T24 5개). Unreachable 0 |
| `python3 verification/model-corpus/validate.py` | 0 | VALID |
| `verification/model-binding/run selftest` | 0 | 61 tests, failure 0 |
| `verification/model-binding/run prepare` | 0 | corpusIntegrity VALID, PREPARED, runtime NOT_RUN |
| 생성기 재실행(`build_cases.py`·`author_cases.py`·`author_prerequisites.py`) | 0 | 두 번 실행해 같은 bytes |
| `python3 verification/cases/V2/cases_b_invariants.py` | 0 | 0 problem(E1·E2 수정 뒤) |
| `python3 verification/cases/T08/bind_observations.py --check` | 1 | catalog hash가 바뀌어 drift 보고(T08 소유, 아래) |

assertion 수 변화: E1 164→173, E2 113→119, C3 +120, V8 182→187(platform-tests 합계 371→376,
T23 불변), T09 +2, T15 +3, T25 1694→3331. subcase 수는
바뀌지 않았다.

## 다른 소유자에게 넘길 일

1. T24 소유자: `sweep-authenticated-reviewer` 5개가 `/provenance`를
   읽는다. 이것이 `./verify prepare`와 `--check-preparation`의 유일한
   FAIL 원인이다.
2. T08 소유자 또는 coordinator: catalog bytes가 바뀌어
   `verification/cases/T08/observation-bindings.json`의 `catalogSha256`이
   낡았다. `python3 verification/cases/T08/bind_observations.py`를 다시
   실행하면 된다(case 내용 변화 없음, `caseHash` 검사는 통과).
3. T20 소유자(cases-a): `T20.skills-real-loading-and-meaning`의
   allowed-tools-as-server-authorization·document-instruction-authority는
   SKILLS loading을, skill-hash-as-loading-proof는 MCP transcript를 읽는
   연결 assertion이 없다.
4. V4 소유자: `V4.all-alternate-write-paths/mixed-batch-allowed-partial-
   effects`에 MCP 경로 증거가 없다.
5. Step 3/coverage(verifyCoverage·assembler 구현): T25가 요구하는 출력
   `namedObservations[].artifacts`(artifactKind 포함),
   `namedObservations[].assertionLinks[].profile`, profile별 `profiles`
   행, records·wrappers의 `profile`·protocol/skill/client/DB version, 평면
   `artifacts`, `modelCaseRuns[].protocolTranscriptStatus`·
   `skillDiscoveryStatus`·`skillBodyStatus`, `attempts[].traceArtifactsVerified`.
   assembler의 observation 기록에 artifactKinds도 실어야 한다.
6. Step 3/S5(ActualClientPort): C3 `skillLoadingTrace` 행에 `packageName`·
   `stage`(DISCOVERED·BODY_READ·REFERENCE_READ·TOOL_CALL)를 기록한다.
7. coordinator: 항목 3 표로 의무 kind 어휘 계약을 공개한다. `RETURN`과
   `RECALL_RESPONSE`는 구현에서 여러 kind로 갈라지므로 case를 나눌지
   구현을 묶을지 결정이 필요하다. `check_layer_routes.py`를 root
   `./verify` 또는 assembler 준비 검사에 연결할지도 결정한다.

## 하지 않은 일

- 의무 kind 이름을 바꾸지 않았다(지시대로 목록만).
- API layer는 관찰 단위 검사에서 뺐다. 효과 관찰은 DB 원행이 artifact이고
  명령 응답은 같은 oracle의 다른 관찰이 API로 읽는다. `--include-db`는
  DB 참고 목록만 낸다(다른 소유자 case의 DB 참고 항목은 harness가 sibling
  관찰로 인정한다).
- C3 feature의 설명 인자는 생성기의 기존 형식(관찰명+assertion ID)을
  유지했다. 업무 문장으로 바꾸려면 309 scenario 전체를 다시 써야 해서
  이번 범위에서 뺐다.
- platform-tests/checks.md의 361, T09·T15 등 준비 기록의 옛 catalog
  hash는 당시 기록이라 고치지 않았다.
- 실제 MCP·client·모델 실행이 없어 새 assertion은 모두 NOT_RUN이다.
