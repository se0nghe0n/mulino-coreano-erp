# S3 공통 통합 검증

신규 수령은 원천 대조만으로 목표 완료가 되면 안 된다. 공통 포트는
실제 수령 적용과 인정된 목표 기여를 별도로 확인한다. 기관 결정은
동일 물리 범위의 같은 시각에 여러 개 존재할 수 있으므로 원문에
근거한 사건 정체성과 의미 hash를 사용한다. 같은 정체성의 상충은
원천을 새로 붙여도 기존 확정 사건을 덮어쓰지 않는다.

## 실행 증거

- Java 21, Node 24에서 `S3TypedEvidencePostgresTest`의 선택한 2건이
  실제 PostgreSQL, persisted source/original/CQN 대조와 link를 통과했다.
  같은 시각의 별개 결정, 두 원천의 같은 결정, 같은 정체성의 상충을
  검증했다. 이 fixture 제공자는 공통 계약 검증이며 실제 기관 도메인
  전체 acceptance로 세지 않는다.
- `S3CanonicalGatePostgresTest` 첫 검사가 통과했다. 정확한 범위·수량과
  `asOf`·`knownAt`을 요구하고 늦게 발견한 원천 상충이 과거 결과를
  덮어쓰지 않음을 확인했다.
- 같은 실행의 나머지 4건은 불변 S1 Work와 SourceProfiles를 수정하는
  fixture가 거부되어 오류로 끝났다. 실패 로그를
  `attempts/shared-immutable-fixture-error.log.gz`에 보존했다.
  `bedb9321`은 검증된 receipt fixture처럼 새 COMMAND Work를 만들며
  source policy 수정이 거부됨을 검사한다. 이후 source guard 2건은 실제 PostgreSQL에서 통과했다.

## 결합 검증

V22까지의 CDS/Flyway parity는 1,432개 column, primary key와 구조가
일치했다. Timestamp widening 276개와 NOT NULL 강화 584개를 명시적
목록과 대조했다. 실제 명사·동사 world snapshot과 customer 맥락
검사도 통과했다. 이 실행은
`attempts/parity-source-pass-authority-fixture-error.log.gz`에 보존했다.

공통 gateway 3건은 fixture의 AuthorityFences 누락으로 REJECTED가
발생했다. 실제 gateway의 현재 권한 fence 요구를 fixture에 추가했다.
`3d9d8e12` 이후 재실행에서 공통 gateway 3건이 모두 통과했다.
적용·현재 권한 재시도, 책임·invalidation·transition 동시 rollback,
서로 다른 만료 사건의 안정적 책임 정체성을 확인했다.
`attempts/shared-gateway-pass.log.gz`와 JUnit XML에 증거를 보존했다.
도메인 전체 gateway 및 HTTP 흐름 검증은 Task 통합 branch에서 별도로
확인한다. worker 완료를 S3 완료로 취급하지 않는다.
