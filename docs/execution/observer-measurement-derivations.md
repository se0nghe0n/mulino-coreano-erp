# Observer 측정 metadata의 실제 원행 derivation

Task/Step은 OPEN이다. baseline은
`04bd203af8632e0117ed6b6ee4a343b69064d897`이며 branch는
`fix/observer-measurement-derivations-04bd`다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b`다.
실제 runtime은 codex/gpt-6.1-sol/high/serviceTier=priority(Fast)다.
root의 후속 08e2a7a5나 SYNC history를 가져오지 않는다.

## 실패 원천과 재현

기존 ScenarioObservationProjection.measurementMetadata는 같은 read-only
REPEATABLE_READ snapshot의 실제 TradeItems에서 baseUnit/id를 읽었지만,
ScenarioJdbcObservation은 data.unit/itemId만 반환했다. SQL trace는
snapshot.measurementMetadataQueries에 있었고 metadata 원행과 derivations는
없었다. 따라서 JSON schema가 통과해도 CaseRunner가
ObserverDerivations.verify에서 `/itemId`의 원행 derivation 부재를 거부했다.

권위 native 증거는 wf_5b5e1885-698의 frozen T17-S01 원본 observation이다.
원본을 읽기 전용으로 복사하고 변경 없이 baseline verifier로 replay했다.
`Observer data field '/itemId' has no raw-row derivation`과 exit1을 재현했다.
원본 path/hash는 `.local/observer-measurement-derivations/native-origin.json`에
남긴다. controller의 12 shard/36 FAIL/0 assertions/461 UNOBSERVED 결과를
새 PASS로 재분류하지 않는다. 모델·workflow·원래 scenario를 재실행하지
않았다.

별도 genuine PG capture도 기존 wire/schema를 통과한 다음 같은 semantic
오류로 실패했다. 이 RED는 pg-red.log/xml이다. 중간 factor compile 오류와
신구 schema를 섞은 준비 오류는 별도 로그에 남겼으며 semantic RED로
계산하지 않는다.

## 실제 원행과 제한된 grammar

새 `measurementMetadataRows`는 별도 기술 companion이다. 요청한 업무
rawRows/sourceEvidence에 TradeItems라는 요청 source를 끼워 넣지 않는다.
원래 요청 source와 원행·수량·단위·조건은 그대로다.

companion에는 실제 TradeItems의 다섯 column 값만 둔다.

| wire field | 실제 column |
|---|---|
| id | mulino_inventory_TradeItems.ID |
| organizationId | organizationId |
| baseUnit | baseUnit |
| recordedAt | recordedAt |
| createdAt | createdAt |

조회는 정확히 한 organization과 ITEM에 한정한다. SQL은 기존과 같은
`SELECT r.* ... WHERE r.organizationId=? AND r.ID=? AND r.recordedAt<=?
AND r.createdAt<=? ORDER BY r.ID`이고 네 bound 값은 조직, 품목, knownAt,
knownAt이다. 같은 snapshot에서 직접 관찰한 값이다. request의 itemId를
사실 값으로 복사하거나 빈 업무 원행에서 unit을 추정하지 않는다.

metadata가 유일하고 실제 baseUnit이 알려져 있을 때 다음 두 derivation만
허용한다. harness가 원행에서 다시 읽어 exact identity를 대조한다.
unit과 item ID에 decimal 수량의 동등성 규칙을 적용하지 않는다.

```json
{
  "/itemId": {"rowPointer":"/measurementMetadataRows","aggregate":"single","field":"id"},
  "/unit": {"rowPointer":"/measurementMetadataRows","aggregate":"single","field":"baseUnit"}
}
```

이 pointer는 /itemId와 /unit에만 허용한다. 다른 field, filter, aggregate,
업무 수량 derivation은 거부한다. 나머지 derivation은 기존처럼 반드시
요청한 rawRows source를 읽어야 한다. metadata는 proof 면제가 아니다.

ObserverDerivations는 정확한 SQL/table/네 bound 값, observation scope와
knownAt, row의 organization/ID/두 timestamp, cardinality와 저장 타입을
검증한다. strict JSON schema는 query와 companion의 동반 존재, 원행의
다섯 field와 최대 한 행을 요구하고 임의 추가 field를 거부한다.

명령되지 않은 scope, 여러 organization/ITEM에는 metadata가 없다. 실제
row가 없으면 빈 companion과 실제 SQL trace는 남되 data 값은 없다.
foreign/future/다른 ITEM은 조회에 포함하지 않는다. baseUnit null/blank도
알려진 단위로 보완하지 않는다. 다중 실제 원행은 unsupported다.

## 전체 native 검증 경로와 PG helper

CaseRunner의 기존 scope/time/requested source/artifact/provenance/derivation
검사를 ContractValidator.observation으로 모았다. snapshot directive·revision
binding은 여전히 CaseRunner가 검사한다. requested source의 완전한 원행과
sourceEvidence, 실제 artifact, 독립 provenance 요구를 없애지 않았다.

새 metadata는 snapshot artifact에 저장된 실제 companion/query/context와도
일치해야 한다. 원행과 data.unit을 함께 바꿔 산술적으로 일치시키더라도
저장된 snapshot artifact와 다르면 거부한다. 이는 기존 independent adapter
custody 위에서 관찰 결과의 일관성을 검사하는 것이며 제3자가 전체 원본
artifact 자체를 위조하지 못한다는 새로운 암호학적 보장은 아니다.

실제 adapter와 PG helper는 ScenarioJdbcObservation.bindArtifact를 공유한다.
이것은 ActualAcceptanceDriver.observe의 기존 wire 조립을 추출한 것이다.
내부 approximateMapping 진단은 원래 native wire가 반환하지 않았으므로
helper도 같은 wire를 검사한다. schema를 느슨하게 바꿔 내부 field를
통과시키지 않는다. SQL 원행이나 요청 source를 바꾸지 않는다.

기존 ScenarioObservationPostgresTest.capture는 실제 capture/wire/artifact를
만든 뒤 ContractValidator.result(schema)와 observation(semantic)을 모두
실행한다. 새 focused PG 검사는 실제 capture를 CaseRunner.execute에 보내
동일한 전체 observe 경로도 통과한다. 임시 검사 case는 target에 생성하고
삭제하며, 원래 case/fixture/grant를 수정하거나 실행하지 않는다.

## 검증 범위와 통합 주의

집중 검사는 empty 업무 원행, metadata 누락·중복·foreign·future·다른 ITEM,
null/blank unit, 단일/복수 scope, data/원행/SQL/table/bound 값/시간/derivation
변조, 미요청 업무 source 대체, 함께 변조한 원행·값의 artifact 불일치,
unit identity와 실제 CaseRunner 경로를 포함한다. 기존 원행 relation,
atomic attribution, capture 및 계약 회귀도 함께 확인한다.

최종 집중 실행은 총172개 중171 PASS, 기존 optional replay1 NOT_RUN이며
FAIL/ERROR는0이고 exit0이다. 새 metadata 검사39개는 모두 PASS다.
`./verify prepare`는 exit0/PREPARED이며 runtime 인수 PASS를 뜻하지 않는다.

정확한 command/exit/count/원문·source·class·JAR·log hash와 소유 resource
cleanup은 `.local/observer-measurement-derivations/handoff.json`에 남긴다.
기존 optional T09 원문 replay는 T09_GOAL_INPUT_SOURCE가 없어 NOT_RUN이며,
새 skip이나 waiver를 추가하지 않는다. 전체 backend/harness와 native는
NOT_RUN이고 primary가 통합·최종 검증·다음 native freeze를 소유한다.

이번 schema 변경은 acceptance-observation.schema.json이다. 현재 baseline의
agents/runtime-package-manifest.json schemaRefs/hashes는 intent,
capability-slots, command-response만 추적하므로 이 schema의 직접 hash
갱신 항목은 없다. manifest는 수정하지 않는다. primary는 08e2a7a5의
command-response schema/manifest 갱신을 그대로 보존하고 통합 시 manifest
의존성을 최종 확인해야 한다.
