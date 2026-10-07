# S1 정의 계약

업무의 원 의미를 현재 정책으로 덮어쓰지 않도록 DefinitionVersions와
PolicyVersions를 분리한다. 모든 정의 FK는 organizationId+ID를 사용한다.
V4는 Flyway만 적용하고 CAP는 CDS/CQN으로 읽는다.

DefinitionVersion의 content는 Definition DTO의 JSON이다. contentHash는
실제 UTF-8 content SHA-256이다. JSON 내부 contentHash는 빈 문자열로
저장하고 읽을 때 DB hash를 반환한다. 자기 참조 hash를 계산하지 않는다.
PUBLISHED 상태와 그 하위 정의는 DB trigger로 불변이다. ACTIVE 포인터,
CONFIG_APPROVER 승인과 발행 workflow는 S5의 계약으로 남는다.

DefinitionValidator는 noun/property/slot/reference 타입, unit 정밀도,
relation endpoint/cardinality, stage, goal 완결성, evaluator manifest와
bounded predicate를 검증한다. 숫자는 decimal 문자열+unit이며 26 정수
자리와 12 소수 자리, 정의의 더 좁은 unit 정밀도를 초과하면 거부한다.
Decimal 단위 환산이나 포장 대체 허가를 추론하지 않는다.

Predicate의 초기 operator는 equals/in/compare/range/exists/cardinality/
timeIn/all/any/not이다. distinctEventQuantity/stateQuantity는 S2에서
원천 대조와 물량 evaluator를 구현할 때까지 UNSUPPORTED다. SQL/script와
알 수 없는 필드는 거부한다. 구조 검증은 사실 확인이나 실행 승인이 아니다.
순수 logical combiner는 UNKNOWN과 conflict를 보존한다.

getDefinition은 공통 dispatcher의 trusted context와 ReadAuthorizer를 쓴다.
기존 Work는 definitionVersionId를 유지한다. 지원되지 않는 pinned
capability/evaluator/schema 조합은 HELD_UNSUPPORTED이며 최신으로 바꾸지
않는다. PolicyRepository.current는 별도의 유효시점을 읽으며 중복 정책을
단일 승인으로 합치지 않는다.

검증 범위는 DefinitionValidatorTest와 실제 PostgreSQL의
DefinitionsPersistenceTest다. T02 전체 외부 ID·단위 환산, T07 문서/사건
대조, T21 승인·migration과 전체 V1은 해당 후속 Step에서 인수한다.

Predicate 크기는 depth32/node1000/operand100으로 제한한다. 타입별
value envelope는 지정 필드만 받고 역전된 수량/시간 bounds를 거부한다.
최종 보강은 short runner로 실제 JUnit assertion 8개를 실행했다.
초기 Maven 결과와 최종 short 결과를 checks.json에서 구별한다.
