# ITEM 단위 조회의 독립 관찰 계약

Task/Step은 OPEN이다. 64054871의 native Haiku Workflow
`wf_e6778389-9a7`은 36 subcase 모두 첫 필수 관찰에서 중단됐다.
461 assertions는 한 건도 실행되지 않았다. 이를 업무 assertion의
0 FAIL이나 제품 PASS로 해석하지 않는다.

ScenarioJdbcObservation이 동일 REPEATABLE_READ 안에서 실제 TradeItem의
단위를 조회하고 snapshot.measurementMetadataQueries에 SQL·bound values를
기록했으나, acceptance-observation.schema.json은 이 optional 필드를
등록하지 않은 채 additionalProperties=false를 유지했다. 실제 adapter가
artifactRef까지 연결한 native 원문도 그 필드 때문에 거부됐다.

schema에 이 optional 조회 trace만 명시한다. TradeItems 한 query의 table,
비어 있지 않은 SQL, 조직·ITEM·기록/생성 시점의 네 문자열 bound values를
요구한다. query와 snapshot의 알 수 없는 속성은 계속 거부한다. 기존
schema의 다른 속성은 모두 동일하다. 근거 trace는 provenance이며 업무
판정이나 수량 oracle로 읽지 않는다. observer·제품 동작·원래 업무
assertion/fixture/grant/시계는 바꾸지 않는다.

동일한 native-bound JSON을 옛 schema에 넣으면 exit1, 새 schema에는
exit0이다. 원문 hash·두 로그·옛 계약은
`.local/resume-20261023/measurement-schema/`에 보존한다. JDBC reader의
raw 단계에는 adapter가 나중에 붙이는 artifactRef가 없으므로, 그 단계의
probe 오류도 별도로 보존하고 native wire 결과와 혼동하지 않는다.

ObservationSourceRelationsPostgresTest의 공통 capture helper가 반환하는
실제 PG 관찰은 이제 전체 wire schema를 검사한다. fixture는 실제 JDBC 사실을 그대로 두고
실제로 저장한 test artifact의 참조만 연결한다. 새 반례는 필수 query
필드 누락, 잘못된 boundValues 타입, query/snapshot의 미등록 속성을
거부하는지 검사한다. 실제 PG 관계37개가 전체 wire schema 검사를
통과했다. 전체 harness는926 executions 중846 PASS·80 opt-in SKIP이며
실패·오류0, source drift0, 자체 DB 정리 확인이다. 실행 기준은64054871과
동결한 schema/test patch이며 commit 후 동일한3189개 source hash로
연결한다. patch·원로그·JUnit·정리 근거는
`.local/resume-20261023/measurement-schema-full/`에 보존한다.

이전 Workflow는 14개 실제 Haiku5.5 node·245 assistant records와 journal,
330 product class bytes의 일치, 12개 자체 DB 정리를 primary가 확인했다.
실행 원본·미관찰461·각 subcase의 harnessError는
`.local/resume-20261023/native-world-deferred-64054871-audit/proof.json`에
남겼다. 이전 T09/T11 100 PASS/7 FAIL 및 T17 79 PASS/178 FAIL은
당시 사업 assertion의 관찰이며 변경하지 않는다. 새 깨끗한 기준선의
native 재실행이 필요하다.
