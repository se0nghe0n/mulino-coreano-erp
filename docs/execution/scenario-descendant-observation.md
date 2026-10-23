# 실제 FOLLOWUP 관계에 따른 descendant 관찰

e995595a의 Haiku 실제 시나리오 초기 실행에서 C3의 Work scope가
`includeDescendants=true` 때문에 NOT_IMPLEMENTED로 차단됐다.
관찰자가 요청 조직의 모든 Work를 반환하는 대신, 실제 저장된
방향 있는 FOLLOWUP 관계를 읽도록 구현했다.

새 ScenarioWorkDescendants는 관찰자의 같은 read-only
REPEATABLE_READ transaction 안에서 recursive SQL을 실행한다.
원 Work와 자식은 같은 조직·ITEM이어야 하며 Work의 createdAt과
recordedAt, WorkLinks의 createdAt이 knownAt 이하여야 한다.
CONTRIBUTES_TO·반대 방향·무관한 Work는 descendant로 넣지 않는다.
중복 관계와 cycle은 UNION으로 중복을 제거하고, 완전하게 관찰할 수
있는 10,000개를 넘으면 차단한다.

여러 조직의 ID를 평평하게 합쳐 다른 조직의 범위를 넓히지 않도록
현재 descendant 요청은 조직 하나와 명시한 Work root를 요구한다.
알 수 없거나 미래의 root는 완전한 빈 결과로 바꾸지 않는다.
실제 SQL·bound values·원 root ID·선택된 ID를 각 요청 source의
sourceQuery에 보존한다. includeDescendants가 없는 조회는 기존의
정확한 Work 범위를 유지한다.

별도 worktree `coordinator-scenario-descendants-e995595a`에서
JDK 21.0.12.1, Maven wrapper, PostgreSQL 18.6으로 실행했다.
현재 package lock의 의존성만 복제하고 자기 source에서 CDS를
컴파일했다. Node는 worker가 검증한 official 24.19.0 executable을
사용했으며 경로·binary hash·lock hash·CSN hash를 별도 기록했다.
과거 S0 인수 증거로 소급하지 않는다.

```text
./mvnw -B -ntp -o -f verification/harness/pom.xml \
  -Dtest=ScenarioWorkDescendantsPostgresTest,ScenarioObservationProjectionTest,ScenarioObservationPostgresTest test
```

최종 결과는 77 tests 중 76 통과, failures 0, errors 0, skipped 1,
exit 0이다. 새 descendant 실제 PG 검사 5개는 모두 통과했다.
기존 optional `goalInputProjectionReplaysCapturedImmutableRowsInFreshPostgres`
한 개는 T09_GOAL_INPUT_SOURCE가 없어 skip됐다. 이번 변경의 새
검사가 아니며 전체 인수 PASS로 포함하지 않는다.

처음 실행의 자기 CSN 미생성 오류와 cleanup 검증 문자열의 대소문자
오류는 `.local/descendant-scope/focused.log`와 root 실행 기록에
보존했다. 자기 CSN 생성 후 재실행은 `focused-r2.log`다.
두 자기 PG container의 제거와 absence를 확인했다.

이 검사는 CSN 형태의 실제 PG table로 observer SQL을 검증한다.
제품의 쓰기 제약을 검증하는 Flyway 통합 검사나 Haiku 실제 scenario
재실행을 대신하지 않는다. 원 case·fixture·oracle·actor·grant·clock은
변경하지 않았다. root의 e995595a 실행은 계속 보존하며 이 commit의
통합과 새 source의 Haiku 재검증은 아직 수행하지 않았다.
