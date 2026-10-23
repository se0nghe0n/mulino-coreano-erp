# S5 native host extractor process의 실행 경계

native client의 raw wire와 최종 답변을 별도 Java process가 읽도록
연결했다. 같은 JVM의 helper 호출만으로 독립 process 실행을
주장하던 공백을 줄인다. 최종 답변의 전체 업무 의미를 검증하는
host acceptance mapping은 아직 완료하지 않았다.

입력은 repository 안의 capture 한 파일과 실제 SHA256뿐이다.
extractor는 shell, product API, DB, model, corpus 또는 oracle을
호출하거나 읽지 않는다. 환경 변수를 비우고 15초 제한을 둔다.
입력 크기는 16 MiB로 제한하며 경로 이탈과 입력 교체를 거부한다.

부모와 자식이 실제 읽은 implementation class 다섯 개의 hash를
대조한다. 명령 argv, PID, JDK binary hash, 시작·완료 시각, exit,
stdout·stderr의 경로·크기·hash와 원 capture hash를 보존한다.
raw host event와 finalResponse가 다르거나 문장 grammar가 해석하지
못한 부분은 UNKNOWN으로 남는다. 계약 fixture를 process에서
읽었다는 이유로 ACTUAL_HOST 인수 자료로 승격하지 않는다.

검증 baseline은 e995595a다. 별도 worktree
`coordinator-native-final-response-e995595a`에서 JDK 21.0.12.1과
Maven wrapper로 다음 focused 검사를 실행했다.

```text
./mvnw -B -ntp -o -f verification/harness/pom.xml \
  -Dtest=NativeHostExtractionProcessTest,NativeObservationTest,NativeProductIntegrationTest test
```

최종 실행은 25 tests, failures 0, errors 0, skipped 0, exit 0이다.
새 process 검사 5개는 실제 extractor JVM을 실행한다. raw event의
모순, arbitrary prose와 model self-label, 잘못된 hash와 추가 인자,
repository 밖의 capture를 포함한다. 초기 Java 변수 이름이 package
이름을 가린 compile 실패는 `.local/native-host-extractor/focused.log`에
보존했다. 수정 후 실행은 `focused-r2.log`, class 대조 추가 후 최종
실행은 `focused-r3.log`다.

NativeFinalResponseObserver는 여전히 NOT_IMPLEMENTED를 반환한다.
별도 process의 custody가 실제 clientProbe 전체 schema와 semantic
oracle 연결을 대신하지 않는다. 한국어의 제한된 명시 문장 이외의
답변을 의미 검증 PASS로 세지 않는다. 원 60 fixture·corpus·binding과
고정 oracle, model prompt, 구독 승인 pin은 변경하지 않았다.
실모델·시나리오·Workflow 실행은 이 작업에서 0회다.

통합 branch는 backend 전체 검사와 Haiku Workflow 때문에 e995595a로
고정되어 있다. 이 변경은 별도 commit으로 보존하며 root 통합과
새 source에서의 결합 검증 전에는 S5 또는 Task 완료로 취급하지 않는다.
