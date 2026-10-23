# T11 정정의 동일 기록 시각과 재평가 보류

Task와 Step은 OPEN이다. 이 문서는 격리 branch의 집중 구현 증거이며
원래 T11 110 assertions의 실제 인수 완료를 뜻하지 않는다.
SYNC e58817d5는 Task로 역통합하지 않는다.

## 동일 기록 시각의 정정

원래 고정 verification clock에서는 원본 EVENT와 정정 EVENT의
`recordedAt`이 같다. V6의 `mulino_evidence_validate_supersession()`은
`prior_recorded >= NEW.recordedAt`에서 SQLSTATE 23514를 발생시킨다.
이 경우 HTTP500은 성공한 correction의 증거가 아니다. 기존 pending은
앞선 canonical 연결에서 생겼을 수 있으므로 correction commit과
구별한다. 실제 PostgreSQL V43에서도 동일 조건을 재현했다.

V44는 이 함수만 교체한다. 기존 prior가 실제 존재하고 같은 조직·
subjectId·subjectKind이며 revision이 정확히 1 증가할 때 같은 기록
instant를 허용한다. 더 이른 instant·누락 prior·자기 참조·다른 scope·
잘못된 revision은 계속 23514다. 불변 prior와 엄격한 revision 증가가
동일 시각 내 순서를 정한다. 기존 single-successor unique indexes와
UPDATE/DELETE 금지 trigger는 그대로다. 재귀적으로 과거를 수정하거나
backfill하지 않는다. 원래 effective/knownAt·fixture clock을 바꾸지 않는다.

`EqualInstantEvidenceMigrationPostgresTest`는 실제 PostgreSQL 18.6에서
fresh V44와 V43→V44를 각각 검사한다. V43 동일 시각 거부 RED,
revision 1→2→3→4의 같은 시각 append, 과거 원행 보존,
이전 시각·missing prior·org/subject/kind/revision/self 참조 거부,
UPDATE/DELETE 거부, 두 실제 transaction의 unique fence lock과
중복 branch 23505를 확인한다. Upgrade 전후 columns·constraints·
indexes·triggers inventory가 같고 migration은 정확히 하나다.

schema delta는 함수 body 교체 하나다. 추가/수정 tables·columns·PK·FK는
0개이며 schema manifest와 과거 migration checksum은 수정하지 않는다.

집중 원로그와 JUnit은 `.local/t11-deferred-reassessment/`에 보존한다.
`attempt6-v44-green-runtime-envelope-red.log`는 V44 검사 2 PASS와
고정 clock의 실제 HTTP EVENT98 APPLIED를 포함한다. 동일 로그의
runtime UUID 입력 오류는 migration과 별도 경계로 남겼다.
