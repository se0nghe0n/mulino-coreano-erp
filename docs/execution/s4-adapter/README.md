# S4 실제 인수 adapter

S4의 최종 수량을 seed하면 판매·인도·반품의 실행을 검증할 수 없다.
따라서 identity·정의·정책과 원본만 설치하고 실제 HTTP 실행과 독립
PostgreSQL REPEATABLE_READ 관찰을 대조한다. 원본은 source identity
대조와 canonical link를 실제 command로 통과해야 한다.

현재 baseline은 `1387024a6c5176d3f663f41e30dd22c1b268faef`다.
`verification/actual/s4/flow.json`의 제품 계약 미통합 표시는 NOT_RUN을
반환한다. 이 infrastructure 자체는 E1/E2 통과나 S4 완료가 아니다.

`./verify actual-s4 <새 증거 경로>`는 이미 빌드한 backend JAR를 복사해
새 Docker PostgreSQL, RSA key, blob root, loopback backend를 실행한다.
각 실패와 cleanup 결과를 새 receipt에 보존한다. 제품 table은 adapter가
소유한 prefix와 실제 column metadata로 읽으며 요청 SQL을 실행하지
않는다. SQL·bound organization·MVCC snapshot·원본 artifact를 보존한다.

긴 Maven/native 실행은 coordinator가 부여한 slot에서만 수행한다.
`python3 verification/actual/s4/build.py`는 clean commit 전체를 빌드하고
custody를 기록한다. `--reuse-backend`는 기존 custody의 backend inputs와
JAR hash가 정확히 같을 때 harness만 빌드한다. source commit/hash와
실행 JAR를 기록하며 paid model·규제·BTP 결과는 대신하지 않는다.

검증은 Java Zulu21에서
`./mvnw -B -ntp -f verification/harness/pom.xml -Dtest=ActualS4FixtureSafetyTest,ActualS3FixtureSafetyTest test`를 실행했다.
첫 compile의 잘못된 proxy class 이름은 수정했다. 두 번째 실행은
4 tests, failures0, errors0, skipped0, exit0이다. `sh -n`과 Python
compile, `git diff --check`도 통과했다. S4 native 실행은 아직 NOT_RUN이다.
