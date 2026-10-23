# 2026-10-23 폐쇄·예약·출고 내부 원천 권한 통합

원래 업무 담당자의 폐쇄와 예약·출고가 별도 공개 증거 조회 권한에
결합되던 문제를 수정했다. 실제 명령의 권한과 현재 원천을 검증하며
공개 조회 권한을 추가하지 않는다. 전체 Task와 현재 Step은 OPEN이다.

## 변경과 통합

폐쇄 재판정은 서버가 선택한 CLOSE_WORK 목적을 사용한다. 공개
assessGoal은 기존 권한을 유지한다. 수령·인도 원천의 요청 Work와
source Work 각각에 같은 목적의 현재 권한을 요구하며 TARGET·ITEM·PLACE
조건도 유지한다. source pin과 입력 hash의 의미는 바꾸지 않는다.

예약·대체·pick·dispatch는 transaction 안에서 생성한 내부 목적을
사용한다. 실제 판매 행·고객·품목·물량·Work·장소와 QC·규제의 원문,
canonical, 불변 버전, 유효기간과 현재 정책을 대조한다. 효과를 반환하기
전에 원천 권한을 다시 확인하며 도중에 만료되면 rollback한다.
SELL 근거만으로 DISPATCH를 허용하지 않는다. 공개 getEvidence와
download, 일반 eligibility 조회의 기존 권한은 유지한다.

| 변경 | worker commit | Task branch commit |
|---|---|---|
| T11 폐쇄의 내부 재판정 목적 | 513e73cd | 1f0e9dd6 |
| 인도 원천의 양쪽 Work 권한 | a28c3d24 | 486c8643 |
| 예약·출고 내부 원천 검증 | 88679d66 | e2fc92af |

coordinator가 20개 변경 파일을 검토하고 worker 원본과 통합 파일 hash를
대조했다. schema·migration·기존 fixture grant와 업무 oracle은
변경하지 않았다. 별도 T09 합성 입력은 아직 이 source에 통합하지 않았다.

## 결합 검증

고정 source는 `e2fc92af784eae95ecf51fe78edc7ff9ee21e251`이다.
Java21에서 전체 backend를 한 번 실행해 JUnit 727개가 모두 PASS였다.
실패·오류·SKIP은 0개이고 Maven exit code는 0이다. 실행 시간은
14분 19초다. backend·contracts·database·verification의 tracked
3,144개 파일 hash를 실행 전후 대조했으며 source drift는 0개다.

원본 JUnit 74개와 로그를 별도 보존했다. Hikari connection-refused
경고도 로그에 남기며 이를 테스트 실패나 성공의 대체 근거로 쓰지 않는다.
실제 검사 결과는 runner와 각 JUnit outcome으로 확인했다. CAP telemetry
logger 설정 외에 제품 인가나 검사 조건을 끄지 않았다.

증거는 `.local/resume-20261023/admission-eligibility-integrated/`의
`backend-proof.json`, `frozen-source.json`, `backend.log`,
`backend-junit/`에 있다. 기존 source e9327108의 harness 658 PASS·59
opt-in SKIP과 schema 1,958열 검증은 이전 고정 source의 증거로 유지한다.
이번 backend 실행을 새 source의 전체 harness·시나리오 인수로 표시하지
않는다.

## 실제 실행과 남은 범위

worker의 T11 HTTP/PG는 19 PASS·3 FAIL이다. 원래 담당자가 자식30과
60을 폐쇄하고 부모 기여90과 UNSATISFIED를 유지하는 경로는 확인했다.
부족10의 typed 의무가 없어 후속 인계를 진행하지 못한다. 무PO 수령의
미배분90은 별도 책임이므로 부족10에 합치거나 같은 후속 의무로
해소하지 않는다. 해당 제품 구현과 실제 인수는 계속 열려 있다.

별도 T09 입력 worktree에서는 실제 합성 원천에 근거한 예약60/100과
각 replay 후 allocation 한 개를 확인했다. 원천 정정 후 SELL 적격량
60→0과 예약 무효과도 확인했다. 10개 HTTP/PG 중 7 PASS·3 FAIL이며
원래 pick의 응답 ID selector가 다음 실패다. 원래 94개 assertion을
보존하고 정확한 응답 경로와 revision 연결만 별도 ledger로 보완한다.
아직 pick·dispatch와 후속 수령 기여는 NOT_RUN이다.

Haiku 5.5 native dynamic Workflow의 T01·T13·T17 실행은 별도 source
21850e42에서 진행한다. 예정44개 subcase·517개 assertion은 결과가
아니며 원본 report를 감사하기 전에는 집계하지 않는다. 이번 source와
혼합해 전체 인수나 완료율을 주장하지 않는다.

제품 실모델 client 호출은 0회다. 기존 구독 인증만 사용하며 별도 API
과금과 BTP 배포를 실행하지 않았다. 실제 운영 identity·법규·계정·region·
배포와 전체 인수, 후속 매뉴얼·최종 검토는 미완료다.
