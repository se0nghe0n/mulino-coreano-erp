# 2026-10-23 구간 기여·인도 claim·전송 권한 통합

미확인 인도 root가 FK 오류로 접수되지 않던 문제와 전송 직전 lease
만료를 수정하고, 같은 실제 구간을 source/target 목표에 고정했다.
전체 Task와 현재 Step은 OPEN이다. 아래의 결정적 검사와 선택한 실제
HTTP/PG 증거는 전체 시나리오·실모델·운영 인수와 구별한다.

## 제품과 입력

T17의 원래 257개 assertion과 기존 ledger를 보존했다. 생성된 같은
Work에 공개 reviseGoal로 필수 목표 입력을 명시한 뒤 활성화한다.
정상·지연·만료의 목표는 모두 30이며 지연·만료 보고량은 원래 20이다.
새 입력은 합성 fixture 선택이며 실제 운영 승인으로 표시하지 않는다.

V40은 원래 주장 root와 공개 입력 JSON을 provisional 관측에 보존한다.
검증된 `rangeRootId`는 원천·실제 Dispatch·canonical을 대조한 뒤에만
채운다. 다른 조직·품목·미확인 root의 존재를 조회하지 않고 동일하게
접수하며, 관측만으로 재고 이동·주문 이행을 만들지 않는다. 기존 V28
FK와 확정 인도의 무결성은 유지한다. legacy 원문은 backfill하지 않는다.

T13은 전달 확인·공급자 회신·물품 도착을 별도 사실로 유지한다.
공급자 원문은 공개 업로드·대조·검증 경로로 연결한다. 전송은 불변 원래
명령·발주 revision/hash·outbox identity와 현재 실행자·위임자·승인·정책·
scope를 다시 확인한다. 최종 권한 검사 중 lease가 만료되면 adapter IO
전에 차단한다. 수정 전 adapter 1회 호출의 RED와 수정 후 0회 호출을
보존한다. 응답만으로 외부 성공을 확정하거나 미확인 전송을 재발송하지
않는다. 원래 135개 oracle와 다른 14개 subcase는 보존한다.

T11의 공개 물량 범위 입력은 실제 canonical90을 `[0,30)`, `[30,90)`으로
연결한다. V41은 같은 allocation에 source Work/GoalVersion/condition을
고정하며 source용 row를 복제하지 않는다. 부모 직접90과 부분 기여30을
120으로 더하지 않는다. 새 목표로 과거 기여를 소급하지 않고, 정정은
양쪽 판정을 무효화하며 폐쇄 Work의 과거를 다시 쓰지 않는다.
legacy source pin은 NULL/UNKNOWN으로 남긴다.

## 통합 commit

| 변경 | worker commit | Task branch commit |
|---|---|---|
| T17 실제 Work·인도 ID 입력 | f2f978b5 | 14bec3af |
| T17 명시적 목표 준비 | bffbde51 | f04bde35 |
| T11 공개 구간 입력 | 7fd055f8 | e9c6085c |
| T11 한정 역변환 bridge | 58f86819 | 5a178d98 |
| T11 후속 계약 제안 | e61e952b | 0667826f |
| T13 공개 공급자 원천 | 64d74f90 | d0b14d20 |
| T13 현재 전달 권한 | 84dd8025 | 00cedd66 |
| 최종 권한 검사 뒤 lease | cb27f8cb | 5a04c815 |
| lease 실행 기록 | c98fc8d7 | eb553739 |
| T17 미확인 claim | 9dd03f69 | e33e69d5 |
| T11 불변 source 목표 | 863c61ec | 4be9480d |

동기화용 worker commit을 중복 적용하지 않았다. coordinator가 변경
파일을 검토하고 각 원본과 통합 파일 bytes를 독립 대조했다. 결합하면서
V39/V40 migration 검사의 target을 각각 고정하고 V41은 실제 V40에서
업그레이드하도록 했다. 통합 검사의 기대 migration 수를 늘리지 않았다.

## 실행 증거

고정 source는 `e932710856553570094a3350268417f19b03a021`이다.
검사 전후 backend·contracts·database·verification의 tracked 3,141개
파일 hash는 변하지 않았다. JUnit 원본과 명령·로그·hash를 보존한다.

| 검사 | PASS | FAIL/ERROR | SKIP |
|---|---:|---:|---:|
| 전체 backend, 한 번의 실행 | 666 | 0 | 0 |
| 전체 harness | 658 | 0 | 59 |
| schema와 V38→39→40→41 경계 | 4 | 0 | 0 |

harness의 실제 PostgreSQL observer 42개는 658 PASS에 포함된다.
59개는 별도 HTTP/PG opt-in이며 PASS로 세지 않는다. exact CDS/Flyway
1,958열과 PK가 일치하고 구조 차이는 0개다. prepare는 41 case·802
subcase·24,260 assertion, preparationProblems 0개와 asset check 12개
PASS다. `gateComplete=false`, runtime/artifact coverage는 NOT_RUN이다.
workingTreeDirty는 보존한 무관한 untracked 세 경로 때문에 true다.

coordinator가 별도로 감사한 worker의 fresh HTTP/Spring/PG 결과는
T13 20 PASS, T17 34 PASS다. 각 worker의 고정 source와 실제 실행 JAR,
raw JUnit을 연결한 한정 증거이며 위 source의 전체 실제 시나리오 결과로
합산하지 않는다. 이전 RED와 중단 로그는 그대로 보존한다.

결합 proof는 `.local/resume-20261023/three-fixes-integrated/proof.json`,
입력·원본 통합 대조는 같은 디렉터리의 `source-integration-proof.json`,
schema 경계는 `schema-migration-proof.json`에 있다. 전체 로그는
`backend.log`, `harness.log`, `prepare.log`이며 JUnit 사본을 함께 둔다.
observer container와 worker HTTP process·DB·임시 키의 정리를 확인했다.

## 남은 인수와 병렬 소유권

T11 자식30/60은 SATISFIED지만 원래 A의 closeWork 재판정이 assessGoal
권한에 결합돼 ASSESSMENT_NOT_CURRENT로 남았다. 권한 확대 없이 내부
평가 목적을 구분하는 후속 제품 수정을 진행한다. 부족10과 무PO 수령의
미배분90은 다른 책임이며 한쪽 인계로 다른 쪽을 해소하지 않는다.

T09의 두 예약은 LOT 만료일·실제 QC/고객/처분·규제 원천이 빠져 있다.
내부 적격성 검증이 창고 담당자의 공개 getEvidence에 결합된 경계도
확인했다. 같은 e9327108에서 입력 writer와 제품 writer를 분리했다.
기존 담당자의 grant와 원래 94개 assertion은 보존한다.

새 T09 fixture 선택은 명시적으로 합성이다. LOT 만료와 새 원천의
유효기간 끝은 2026-10-31T23:59:59Z이며 시작은 fixture 시계다.
SELL/DISPATCH는 각각 QC·CUSTOMER·COMMERCIAL와 규제 근거를 갖는다.
규제 authority `T09_SYNTHETIC_AUTHORITY`, namespace
`t09-sale-eligibility`, version `SYNTHETIC-policy-v1`,
`requiresLabel=false`는 이 한정 테스트의 새 fictional 선택이다.
실제 법규·운영 면제·원래 제공된 사실로 주장하지 않는다. 필요한
최소 source actor와 위임 권한도 새 입력으로 추적하며 기존 grant를
넓히지 않는다.

후속 구현의 Sol Ultrafast 설정은 실제 runtime에서 확인했다.
다음 실제 시나리오는 Haiku 5.5 native dynamic Workflow로 고정 source를
병렬 실행한다. 기존 구독 인증만 사용하며 제품 실모델 client 호출은
0회다. 실제 supplier 전달·법규·운영 identity·BTP 계정/region/배포 인수는
미완료다. main/PR merge와 유료 배포를 수행하지 않았다.
