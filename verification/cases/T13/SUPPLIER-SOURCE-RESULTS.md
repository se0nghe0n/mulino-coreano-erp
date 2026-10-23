# T13 supplier source 검증 결과

2026-10-23, branch `fix/t13-supplier-replies`, 공통 baseline
`bcac964ced0d5cf47a990afabba5eddefb66b2c8`에서 검증했다.
Task/current Step은 OPEN이다. 전체 T13 또는 전체 ontology 인수 PASS가 아니다.

| 검사 | PASS | FAIL | NOT_RUN/skip | exit |
|---|---:|---:|---:|---:|
| T13 Python 전체 | 20 | 0 | 0 | 0 |
| 전체 harness 660 tests | 616 | 0 | 44 | 0 |
| 한정 실제 HTTP/Spring/PG 10 tests | 5 | 5 | 0 | 1 |
| `./verify prepare` | PREPARED | 0 | runtime 인수 별도 | 0 |

전체 harness에는 마지막 observer probe 추가 직전의 T13 HTTP test 9개가
skip으로 포함된다. 마지막 probe까지의 10개는 이후 실제 HTTP/Spring/PG
실행에서 모두 실행했다. JUnit의 FAIL 5개는 assertion failure 2개와
제품 거부에 따른 error 3개다. 이를 환경 실패나 PASS로 바꾸지 않았다.

## 실제 HTTP/PG test별 결과

| test | 결과 | 관찰 |
|---|---|---|
| accepts100WithoutArrival | FAIL | 전송 대조가 STALE_EXECUTION이다 |
| rejects100WithResponsibility | FAIL | 전송 대조가 STALE_EXECUTION이다 |
| changes80PreservesOrdered100 | FAIL | 전송 대조가 STALE_EXECUTION이다 |
| missingTransmissionConfirmationCannotCreateCommitment | FAIL | 전달확인 없이 supplier ACCEPT가 APPLIED다 |
| originalReplyAssertionUsesActualObserverFields | FAIL | 원래 reply-distinct의 purchaseId 필드가 없다 |
| supplierCanonicalRepliesAreDistinctIndependentOfTransmission | PASS | 전송 대조를 생략한 별도 검사에서 ACCEPT100/REJECT100/CHANGE80, 주문100·도착0·재고0·outbox1, 후속 책임과 중복 거부를 확인했다 |
| unverifiedDocumentAndWrongQuantityCannotCommit | PASS | 문서만 있는 회신과 잘못된 단위를 거부했다 |
| mismatchedSourceIdentityQuantityTimeAndOrderDoNotVerify | PASS | source identity/수량/단위/시각/대상 불일치는 MATCHED가 되지 않는다 |
| wrongScopeAndExpiredSourceCannotPublish | PASS | ITEM/WORK all-of 불일치와 만료 source grant는 무효과다 |
| anotherOrganizationCannotUseOriginalOrderOrCanonical | PASS | 다른 조직의 원 주문/canonical 사용은 FORBIDDEN이다 |

위 독립 supplier 검사에서 전송 성공을 주장하지 않는다. 실제 outbox는
PENDING/attempts0이고 생성한 물리 수량은0이다. root가 해결해야 하는
제품·observer 파일과 최소 범위는 [원천 인계](SUPPLIER-SOURCES.md)에 있다.

## 명령과 artifact

공통 환경은 `JAVA_HOME=$(/usr/libexec/java_home -v 21)`이고 PATH 앞에
해당 JDK와 이 tree의 `backend/node_modules/.bin`을 둔다.

- `npm --prefix backend ci`: exit0이다.
- `./mvnw -B -ntp -f backend/pom.xml -Dmaven.test.skip=true package
  dependency:build-classpath -Dmdep.includeScope=test
  -Dmdep.outputFile=$PWD/.local/t13-supplier-work/backend-classpath.txt`:
  exit0이다. 이후 각 PG runner도 이 tree의 JAR를 새로 빌드했다.
- `T13_BACKEND_CLASSPATH=$PWD/.local/t13-supplier-work/backend-classpath.txt
  python3 -m unittest discover -s verification/cases/T13 -p 'test_*.py' -v`:
  전체 T13 Python 검사다.
- `python3 verification/cases/T13/run_supplier_pg_test.py`:
  마지막 실행은
  `.local/t13-supplier-pg-6888fc7d-138d-47bd-aff1-dad8d2ae0010/`이다.
  `commands.json`, `build-identity.json`, `test.log`, JUnit XML,
  각 `*-http.json`의 실제 HTTP 원문과 SQL rawRows를 보존했다.
- `./verify harness`: `.local/t13-supplier-work/harness-final.log`다.
- `./verify prepare`: `.local/t13-supplier-work/prepare-final.log`와
  같은 폴더에 복사한 `prepare.json`이다.
- 전체 요약은 `.local/t13-supplier-work/verification-summary.json`이다.
  `raw-source-bcac-scenarios.json`에 받은 실패 원본을 보존했다.

새 JAR는 `backend/target/ontology-0.1.0-SNAPSHOT.jar`이며 SHA-256은
`a3a73fb3151eeda15387155a666b14eff346f72f8530ec9611267de4209ecde3`이다.
backend source는 baseline과 동일하다. 타 tree의 JAR/generated를 복사하지 않았다.
임시 key와 생성 DB/container는 정리했다. 로그·blob·실패 원문은 보존했다.

실모델/Haiku Workflow 및 실제 scenario runner는 실행하지 않았다.
운영 supplier 전송·BTP·배포·API 과금·push·main/PR merge·Grok 알림도
수행하지 않았다. 원래 135 assertion과 비대상 14 subcase는 보존했다.
