# S2 복구 처리

scheduler는 기술 시도 만료를 DB에서 EXPIRED로 바꾸고 같은 Work를
재확인 대상으로 색인한다. grant/policy 변경 색인을 소비할 때에도
원본 Work와 인간 owner/supervisor를 유지한다. policy 변경의 영향은
현재 조직의 활성 업무를 보수적으로 재판정한다. 시각 경과는 새 승인이나
실물 사건을 만들지 않는다.

복구 실행자는 실제 Actors.stableRequestOwner를 읽는다. 각 assessGoal과
resumeWork에 안정된 복구 key로 ExecutionAttempt를 claim한다. 공통
executeClaimed는 DB에 보존한 actor를 복원하고 현재 grant/policy와 정의,
revision, claim을 검사한다. runtime은 별도 업무 전이 구현을 갖지 않는다.
검증된 재개 predicate를 만족하지 못하면 WAITING을 그대로 보존한다.

현재 인가·evaluator·정책·predicate를 통과하지 못한 복구는 HELD_MANUAL과
구체 nextAction/nextCheck를 저장한다. 같은 +1초 pending을 무한 생성하지
않는다. 원본 확정 CommandRecord가 있으면 RUNTIME_RECOVERY 의무를 실제
책임 module에 연결한다. owner 비활성화 또는 원본 부족으로 연결할 수
없으면 supervisor의 owner 복구·승인된 인계 행동을 명시한다. 의무 연결의
실패 거래는 rollback한 뒤 관측 index에 수동 복구를 남긴다.

외부 전달은 IN_FLIGHT를 먼저 commit한다. 실제 IO 시점에는 current
policy/actor fence를 잡고 scoped grant를 재검사한다. 전송은 bounded
adapter 호출이며 외부 효과가 DB rollback으로 취소된다고 가정하지
않는다. 현재 typed 외부 domain preparation이 없으면 전달은 fail-closed다.
verification fixture adapter의 효과는 별도의 HTTP/SQLite process에 있다.

원본 key retry는 common command의 retryOriginal 경로에 의존한다. 제품
구매·수령 domain adapter, 실모델, 운영 BTP 인수는 이 문서의 범위가 아니다.
