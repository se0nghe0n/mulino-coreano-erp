# T09 불변 입력의 원천 관계와 snapshot 계약

Task와 현재 Step은 OPEN이다. 이 기록은 기존 HTTP 자료와
고정1b41e17f 코드의 진단이며 새 시나리오 PASS가 아니다.

## 원천 관계

`exists-same-input-scope`, `state-same-input-scope`,
`throughout-same-input-scope`의 실제 segment Q와 DISPATCH movement는
각 최종 InputSnapshots.contentJson 안에 이미 있다. 같은 조직의
facts.sources/inventory.QuantityMovements 원행과
facts.properties/QuantitySegment.stateQuantity의 sourceId,
physicalScopeId, evidenceRefs가 정확히 연결된다. provider나 schema의
원천 저장 누락으로 분류하지 않는다. 초기 snapshot에 나중 출고가
없는 것은 당시 knownAt 경계를 보존한 결과다.

원천은 AssessmentFactProvider.load가 고르고 AssessmentService.calculate가
Goal·정책·조건과 함께 hash로 고정한다. observer의 @assessmentInputs는
현재 조건당 한 행을 내지만 이 원천 관계를 별도 필드로 투영하지 않는다.
원래 case의94 assertions·fixture·grant·수량·시점은 변경하지 않는다.

coordinator가 AssertionEngine도 추가 확인했다. relationSet은 동일 tuple의
반복을 거부하며 sameAs는 배열의 행 수까지 비교한다. 그러므로 원천 ID를
현재의 모든 조건 행에 복사하면 세 관계 assertion도 해소되지 않는다.
실제 입력 관계와 조건별 계산 근거를 보존하는 관찰 행 단위가 필요하다.
조건 입력이 빈 논리 부모에 실제 movement를 사용했다고 표시하지 않는다.

## 공통 원천과 평가별 저장의 차이

세 최종 평가의 asOf/knownAt/recordedAt은 모두2026-10-07T02:00:00Z이고
저장된 facts는 구조적으로 같다. 조회 knownAt04:00Z가 과거 snapshot을
04:00 기준으로 다시 계산한 것은 아니다. Goal과 조건 계산 결과가
달라 전체 contentJson/hash와 실제 InputSnapshots.ID는 각각 다르다.
현재 observer의 snapshotId는 그 실제 row ID다.

`state-same-assessment-input-snapshot`과
`throughout-same-assessment-input-snapshot`은 같은 snapshotId 배열을
요구한다. 현재 조건 행 수는 EXISTS_IN/STATE_AT/THROUGHOUT 각각3/3/6이다.
공통 값을 덮어써도 같은 배열이 되지 않는다. query revision이나 digest를
실제 persisted identity인 것처럼 노출하지 않는다.

사용자에게 같은 snapshot의 의미를 물은 뒤, 더 묻지 말고 계속하라는
지시를 받았다. coordinator는 기존 인수 기준을 유지하는 공통 원천
캡처와 목표별 평가의 분리로 결정했다. 사용자가 세부 저장 설계를
직접 선택한 것은 아니다. 사용자 답변 대기는 해소했으며 구현·인수는 OPEN이다.
공유 범위·시점·권한의 경계, 실제 영속 identity와 Assessment binding,
관찰 행 단위와 기존 T11/T12 소비자의 보존은 결합 설계에서 검증한다.

진단의 정확한 원문 pointer·ID·hash·시각·코드 위치는 별도 worker tree
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b/`
`.local/t09-immutable-source-gap/diagnosis.md`와 diagnosis.json에 있다.
이 진단에서 코드 변경·빌드·테스트·actual 재실행은 수행하지 않았다.
후속 구현은 별도 Sol high/Fast 작업에서 진행하며 결합 검증 뒤 반영한다.
