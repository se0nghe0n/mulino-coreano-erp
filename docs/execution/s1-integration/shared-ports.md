# S1 공통 read port

서로 다른 adapter가 인가·시점·오류를 다시 계산하지 않도록 공통
application port를 둔다. S0 platform 증명 경로와 제품 read 경로는
분리한다. S1에서는 제품 write dispatcher를 열지 않는다.

- `DomainContext`: 검증된 외부 신원에서 서버가 찾은 조직·actor·owner,
  요청의 `asOf`와 `knownAt`이다. client identity override를 허용하지 않는다.
- `ReadAuthorizer`: 현재 mapping과 capability/target grant를 검사한다.
  search의 target은 null이고 결과별 target도 다시 검사한다.
- `QueryRequest`: operation, id, scope, whitelist filter, page limit,
  cursor, definitionVersion, asOf, knownAt, snapshotRef다.
- `QueryHandler`: 지원 operation을 명시하고 scope를 제한한 CQN을 사용한다.
- `QueryResult`: data, scope, unknowns, conflicts, evidenceRefs, nextCursor다.
  공통 application은 snapshotRevision을 포함한 공개 envelope를 만든다.
- `DomainError`: domain outcome과 code를 모든 adapter에 동일하게 전달한다.

내부 조직은 UUID다. FK는 `mulino_identity_Organizations(ID)`를 참조한다.
actor는 `(organizationId, ID)`로 참조한다. CDS namespace와 Flyway table은
CAP compiler의 underscore 이름을 사용한다.

## 시점과 snapshot 의미

`Timestamp`의 기본 compiler SQL은 `TIMESTAMP`다. 조직 간 조회와 offset
입력을 절대 시점으로 비교하려는 R1 결정에 따라 제품 시점은
`TIMESTAMPTZ`로 넓힌다. S0 platform schema는 별도 증명 자료로 보존한다.
모든 허용 column을 compatibility manifest에 열거하고 그 밖의 차이는
거부한다. 실제 CQN의 offset/UTC/Asia-Seoul 대조는 별도 검증한다.

`snapshotRevision`은 인가된 read projection의 content hash다.
PostgreSQL MVCC snapshot ID를 뜻하지 않는다. 같은 조직·actor·scope·
시점에서 명사·업무 조회는 같은 Work/Goal/Assessment/Obligation 참조와
inventory 조회를 조합하므로 같은 hash를 반환한다. 재조회 projection이
바뀌면 기존 snapshotRef로 결과를 꾸미지 않고 `SNAPSHOT_CHANGED`를
반환한다. 독립 DB observer는 hash를 그대로 복사해 MVCC 증거라고
기록할 수 없다. 재현한 projection digest나 별도 native assertion이
없으면 해당 oracle는 `NOT_RUN`이다.
