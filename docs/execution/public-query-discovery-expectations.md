# 공개 QUERY discovery 기대값을 생성기와 정합한다

기준선 `3de8cc999d7c7d44e09ab7a742f5304c0f73076a`의 120개 공개 tool
기대값은 새 계획 QUERY 17개가 catalog에 들어온 뒤 오래된 목록이 됐다.
이 수정은 `contracts/acceptance-capabilities.json`을 공개 discovery의
원천으로 사용해 생성된 기대값만 갱신한다. QUERY metadata는 공개 목록이며
정의·grant·fixture 권한이나 업무 실행 구현을 만들지 않는다. 실제 실행과
native client 인수는 이 기록의 범위가 아니다.

## 단계별 catalog 보존

reachable Git snapshot을 대조해 114→120→137 단계의 원본을 고정했다.
`d49079678a8ceda00178911acaa28cd9f405e710`에는114개가 있고,
`020be0cff3e784c7a37bc1a9cbef011debaaff78`에는 그 항목과 순서를 유지한
채 기존 6개 command가 더 있다. `1038719b`는 그 120개를 그대로 보존하고
아래 17개 `kind=QUERY, implemented=false` 항목을 더한다.

`getTrace`, `getSettlement`, `getSalesOrder`, `getSalesOrders`,
`getDelivery`, `getDeliveries`, `getRegulatoryProcedure`, `getRecall`,
`getRecalls`, `traceRecall`, `getReceipt`, `getReceipts`, `getPurchase`,
`getReturn`, `getReturns`, `getShipment`, `searchShipments`다.

기존 보존 test가 사용하던 `ce0eecc1`은 이 clone에서 찾을 수 없는
객체였다. 임의 ref를 가져오거나 해당 검사를 없애지 않았다. 통합 history의
114개 기준 `d4907967`과 query 직전 120개 기준 `020be0cf`를 각각 고정해
114+6+17개의 단계별 delta와 전체 entry content/order를 검사한다.

## 생성된 기대값과 권한 경계

- `verification/mcp-tests/author_cases.py`가 T20의
  `tool-schema-registry` 5개 exact list를 source catalog와 일치시킨다.
- `verification/cases/C3/author_prerequisites.py`가 기존
  `model-query` fixture grant 경로와 별도로 C3 `tools-advertised` 30개
  exact list를 source catalog와 일치시킨다.
- `verification/cases/V4/author_review_fixes.py`가 공개
  `tools/list`와 allowlist exact 비교를 137개로 갱신하고 source catalog
  SHA256을 새 값으로 고정한다.

기존 120개 discovery 이름은 그대로 있고 총36개 기대값만 137개가 됐다.
V4 allowlist SHA256은 다음 값으로 갱신됐다.

- 이전: `dd4987f1ce7050dd510da42e4ab4cf5706d19413e6181bbc29924a251505c3a4`
- 현재: `329289bc6d7ec4b20af96366e4d73cfdb366963fc6c8f62ace64b6d9bcb94faf`

C3와 V4의 `observation-bindings.json`은 각 case의 파생
`caseHash`만 갱신했다. actor, grant, scope, quantity, clock, 업무 oracle,
assertion ID/order/count, fixture와 feature는 변경하지 않았다.

`test_public_inventory_preservation.py`는 query 직전 case로 메타데이터를
역복원했을 때 T20/C3/V4의 전체 JSON 객체가 각각 정확히 같음을 확인한다.
각 generator renderer를 사용한 역복원 byte도 세 case 모두 원래 byte와
같다. C3/V4 binding은 `caseHash`만 역복원하면 원 byte와 같고, 나머지
T20/C3/V4 파일은 원래 byte와 같다. 검사는 fake tool, 기존/신규 이름의
축소, actor·scope·business outcome·quantity 변경과 잘못된 V4 hash를
거부한다.

## 실행 증거와 범위

먼저 untouched baseline에서 MCP generator suite는 1 pass/2 fail(T20·C3),
case-generator suite는 5 pass/1 fail(V4)이었다. 생성기를 실행한 뒤 아래
세 deterministic suite를 통과했다.

- MCP generator suite: 3 pass, 0 fail.

  ```sh
  python3 -I -m unittest discover \
    -s verification/mcp-tests -p test_generators_reproduce.py -v
  ```

- Preservation suite: 6 pass, 0 fail.

  ```sh
  python3 -I -m unittest discover \
    -s verification/mcp-tests -p test_public_inventory_preservation.py -v
  ```

- Case generator suite: 6 pass, 0 fail.

  ```sh
  python3 -I -m unittest discover \
    -s verification/requirements -p test_case_generators_reproduce.py -v
  ```
- `git diff --check`: exit 0.

원본 RED, 생성기 실행, 후속 test 로그와 역복원 proof는 ignored
`.local/model-archive-query-discovery/`에 보존했다. Java/Maven, `./verify
prepare`, backend/PG/native MCP·client, 실제 model, full scenarios는 이
좁은 변경에서 실행하지 않았다. coordinator의 기준선에는 Maven Central
BOM DNS로 focused harness test가 시작되지 못한 기록이 있으며 runtime PASS로
대체하지 않는다.

변경 파일은 T20/C3/V4의 `case.json`, C3/V4의 파생
`observation-bindings.json`, 그리고 stage-aware 보존 test다. 업무 계약,
product handler, catalog source, synthetic actor/grant, model-installation
archive에는 변경이 없다. 전체 Task/Step은 계속 OPEN이다.
