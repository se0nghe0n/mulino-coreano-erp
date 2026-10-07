# 실제 host skill 인수 계약

skill package의 hash만으로 실제 발견·loading·해석을 증명할 수 없다.
T20의 여섯 `skill-*` 시나리오는 실제 client에서 discovery, 본문 읽기,
reference 읽기, 실제 MCP tool 호출, 남은 의무 확인을 각각 관찰한다.
현재 제품 package와 host/model adapter는 없으므로 실제 경로는 NOT_RUN이다.

`clientProbe`의 packageName은 host 통합 task의 입력이다. 실제 모델에는
userUtterance와 허용된 문맥만 전달하며 assertion, 예상 intent·tool·결과를
prompt에 넣지 않는다. host가 loading trace를 제공하지 않으면
해당 범위는 NOT_RUN이다. 본문/reference 읽기를 합성하지 않는다.

독립 extractor의 실제 artifact에는 packages, links, discovery,
loading, toolCalls, obligations, interpretation, compatibility, gate를
분리해 기록한다. loading row는 packageName, stage, artifact path/hash,
client identity, 발생시각과 실제 read/tool transcript를 연결한다.
stage는 DISCOVERED, BODY_READ, REFERENCE_READ, TOOL_CALL,
OBLIGATION_READ다. 마지막 두 stage는 wire와 실제 DB/API 결과를 연결한다.
같은 사건을 stage 이름만 바꿔 중복 증거로 만들지 않는다.

packages에는 parsed frontmatter와 name/version/hash, 지원
definition/capability/schema, 검증된 client manifest를 남긴다.
두 discovery link의 realTarget은 실제 `agents/skills/<name>` directory다.
developer skill을 여섯 제품 package에 포함하지 않는다.

별도 host-* 시나리오는 hash만 있음, allowed-tools를 권한으로 오해함,
악성 문서 지시, 오래된 package, 동의표현, 다국어, 모호성, 동명이인
입력을 실행한다. QUERY가 COMMAND로 바뀌면 client tool transcript에서
실패이며, 서버의 업무·재고·승인·outbox 효과도 독립 전후 원행으로
검증한다. 조회/거부 감사와 인간 책임은 별도로 보존한다.

M60 원문과 binding은 다른 담당자의 소유다. T25는
`verification/model-binding/registry.json`,
`verification/harness/target/evidence/model-binding-preparation.json`,
`verification/harness/target/evidence/runtime-manifest.json`을 읽는다.
실제 usage/비용의 누락은 null과 missingReason이며 0이 아니다.
R8 승인 없는 실모델 호출이나 실제 host 결과를 만들지 않는다.
