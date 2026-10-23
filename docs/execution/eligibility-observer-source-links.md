# 현재 SELL 조건의 독립 원천 관계

T17의 고정 조건 기대와 실제 근거를 서로 독립적으로 대조하도록
원천 관계 projection을 추가한다. 기존 `conditions`는 heterogeneous
원행이고 `conditionResults`는 Work의 immutable assessment다. 두 원천을
현재 eligibility evaluator의 결과처럼 사용하지 않는다.

coordinator의 작업 branch는 `fix/t17-eligibility-observer-20261024`다.
처음 기준선은 b4e77b7c이며 Sales와 T11 통합 뒤 d0885a80으로
fast-forward했다. runtime은 codex_2/Sol/xhigh/priority다. 이 변경은
제품 evaluator와 T17 case·fixture를 수정하지 않는다.

## 두 원천의 계약

- `sellConditionSourceRefs`는 실제 segment별 QC, REGULATORY, LABEL,
  DISPOSITION, CUSTOMER_CONTRACT와 정렬한 `evidenceRefs[]`를 반환한다.
  segment ID 순서와 위 조건 순서를 유지한다. `contextSources`에는
  같은 snapshot의 실제 ITEM, LOT, regulatory policy, active policy
  pointer와 그 immutable version을 남긴다. 상태·적격량·evaluatorVersion을
  계산하거나 반환하지 않는다.
- `sellConditionSourceLinks`는 각 근거의 실제 typed source table/ID와
  원행을 보존한다. canonical·verification·claim·event·document·profile·
  inbox ID와 실제 immutable blob bytes를 대조한 chain도 남긴다.
  LABEL이 실제 현행 정책상 불필요할 때만 그 policy ID를 연결한다.

QC·DISPOSITION·CUSTOMER_CONTRACT는 실제 disposition permission과
같은 범위의 현재 제한을 각각 보존한다. 두 근거를 합쳐 ALLOWED나
DENIED를 추론하지 않는다. REGULATORY는 실제 current procedure head와
SUBMITTED source, supersession·시각·scope가 맞는 결정의 canonical ID를
연결한다. LABEL은 해당 head의 최신 label과 실제 원문을 연결한다.
ALLOWED와 REVOKED 결정은 두 원천으로 남으며 CONFLICT 판정은 제품과
고정 oracle이 담당한다.

모든 SQL은 기존 observer의 read-only REPEATABLE READ transaction과
bound parameter를 사용한다. 조직·ITEM·Work·LOT 및 recordedAt/knownAt
관계는 기존 projection을 재사용한다. 잘못된 원문·다른 조직·미래 문서·
변조·무효화·불명확한 head는 완전한 근거로 반환하지 않는다.

현재 지원 범위는 독립된 leaf의 SELL 원천이다. genealogy나 별도
customer-specific 입력이 필요하면 명시적으로 unsupported다. ActivePolicies
pointer에는 과거 시각 이력이 없으므로 `activePointerHistoryComplete=false`를
남긴다. 현재 transaction에서 읽은 pointer 관계이며 과거 운영 권한의
증거가 아니다. 이 제한을 full Task의 waiver로 사용하지 않는다.

## 실행 증거와 경계

coordinator tree의 `.local/eligibility-source-proof/`에 정확한 명령,
source hash, SQL 캡처, JUnit, exit code와 소유 DB 정리를 보존했다.
새 PostgreSQL 검사11개는 실패·오류·skip0, exit0이다. permission과 hold의
별도 문서, 상반된 두 결정과 label의 별도 canonical ID, 정책상 label
불필요, 다른 조직·미래·변조·누락 bytes·잘못된 범위/원문·무효화와
genealogy의 fail-closed를 검사했다. product evaluator를 호출해 기대를
생산하지 않는다.

첫 검사11개는 새 tree의 CSN 미생성 때문에 모두 setup 오류였다.
`first-missing-csn/`에 실패와 정리 증거를 보존하고, 같은 lock의
`npm ci --offline --ignore-scripts`와 backend generate-resources로
자기 tree의 CSN을 생성한 뒤 다시 실행했다. 다른 tree의 JAR나 schema
산출물을 복사하지 않았다. 재실행 전후 source drift는 없다.

현재 focused source projection의 PASS이며 T17 실제 condition oracle의
인수는 아니다. 별도 T17 writer가 원래 10개 assertion의 가역 변환,
실제 상충 근거 설치와 ITEM grain을 맞춘다. 두 변경을 통합한 뒤
원본 의미를 보존한 HTTP/PG와 Haiku dynamic Workflow로 인수한다.
Task·Step·S5/S6는 OPEN이다.
