# S4 초기 통합 검토

S4의 각 구현은 Task branch에 순차 통합 중이다. 이 기록은 coordinator의
연결 검토이며 사용자 Step3 전체의 지정된 두 모델 review를 대신하지 않는다.
개별 compile과 단위 검사는 실제 PostgreSQL 및 E1/E2 결합 인수와 구별한다.

## 공통 계약

`SalesOrderLinePort.remainingQuantity`는 주문 수량에서 취소되지 않는 실제
출고 수량을 뺀 값이다. 예약 명령은 여기에 현재 EXECUTABLE/SUSPENDED
배분을 추가로 차감한다. 인도 목표의 인정 수량과 반품 수량은 별도다.
과거 주문 revision은 조회할 수 있지만 후속 revision의 실행을 무시한
새 예약의 근거가 되어서는 안 된다.

## 초기 발견과 인수 조건

| 발견 | 수정 또는 남은 검증 |
|---|---|
| 중간 구간 제거 뒤 부족 배분의 좌표가 틀림 | source interval 차집합으로 변경했다. 실제 PG에서 제거 구간과 부족 책임을 대조해야 한다. |
| 부분 출고의 잔존 구간도 출고 이동으로 기록됨 | 잔존 edge/movement를 RETAINED로 구분했다. 출고20의 이동 합계20을 확인해야 한다. |
| 계보 offset 한쪽 NULL이 SQL UNKNOWN으로 통과함 | 양쪽 NOT NULL 조건을 추가했다. DB 거부를 확인해야 한다. |
| 좌표 없는 기존 배분에 임의 좌표를 부여함 | PHYSICAL_RANGE_UNCONFIRMED 정지로 변경했다. 실행 효과0을 확인해야 한다. |
| 원 제한과 겹치지 않는 자식까지 전체 차단됨 | 정확한 계보의 불일치와 미확정 계보를 구분하도록 수정했다. 두 경우를 각각 검증해야 한다. |
| 실제 감량에 잔존량이 합산됨 | ADJUST_DECREASE/DISPOSE 잔존 이동을 RETAINED로 변경했다. 60→50의 실제 감량10을 검증해야 한다. |
| 출고가 판매 실행 계약에 전달되지 않음 | 실제 출고에서 DISPATCHED 기록을 추가했다. 후속 예약과 revision 경계를 검증해야 한다. |
| 관측 인도 장소와 실제 원장 이동 장소가 달라질 수 있음 | 판매·재고 담당자가 명시 관측 장소 계약과 위반 후속 책임을 연결한다. |
| 송장 관련 canonical과 별도 문서의 값이 혼합될 수 있음 | canonical의 실제 verification 문서·의미와 입력 문서의 결합을 강제해야 한다. |
| 일부 새 조회가 미지원 filter·시점·연결 대상 인가를 생략함 | 명사/동사 양 경로에서 같은 scope·현재 권한·asOf/knownAt을 검증해야 한다. |
| 회수 보류의 controlScope와 필수 policy/기간 필드가 불일치함 | 실제 segment의 controlScope와 유효한 정책 근거로 생성하고 출고 거부를 검증해야 한다. |
| 동일 recordedAt에서 주문·승인 최신값 선택이 모호함 | 명시 revision/현재 포인터로 결정하고 옛 허용의 재사용을 거부해야 한다. |

각 수정의 OWN commit과 원본 실행 증거는 담당자의
`docs/execution/s4-*` 기록에서 통합한다. 위 표의 수정 표기는 source
수정을 뜻하며 runtime PASS를 뜻하지 않는다. 전체 backend, schema parity,
deterministic V2/V3 경합 및 실제 E1/E2 종단 인수가 끝날 때까지 S4는 ACTIVE다.
