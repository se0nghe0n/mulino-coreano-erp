# 2026-10-23 목표 부족 의무·판매 원천 통합

현재 평가에 근거한 부족10 의무와 T09의 실제 판매 원천 입력을 통합했다.
고정 source e8c66499의 전체 backend는 740 PASS, harness는 660 PASS와
80개 별도 opt-in SKIP이다. 전체 Task와 현재 Step은 OPEN이다.

## 목표 부족 의무

명시적 openObligation의 GOAL_QUANTITY_SHORTFALL은 COMMITTED assessGoal의
불변 결과·입력 hash·현재 GoalVersion과 정책·현재 사실을 다시 대조한다.
서버가 고른 내부 권한 목적과 양쪽 Work의 현재 권한을 사용한다. 공개
assessGoal 권한을 추가하거나 caller가 평가 목적을 고르도록 하지 않는다.

모호하지 않은 quantitySum lower bound 한 개만 부족량을 증명한다.
부모100에서 실제 인식90을 뺀10을 stable root로 만들며 새 command key와
같은 입력의 새 평가도 현재 root·assignment를 재사용한다. transfer/accept
후에는 실제 successor를 반환한다. 추가 실제 수령5로 인식95가 되더라도
최초 root10을 바꾸거나 이전 assignment를 다시 열지 않고 HELD로 남긴다.
resolve/waiver/자동 해소 의미를 새로 만들지 않았다.

V42는 OLD 또는 NEW kind가 해당 kind인 root UPDATE를 거부한다. 기존
DELETE guard와 다른 kind, scope/assignment 인계는 유지한다. table·column·
PK·FK와 과거 migration checksum은 변경하지 않았다. 첫 sourceVersion
직접 변경과 중복 target leaf가 통과하던 RED를 보존하고 수정했다.

worker의 독립 fresh HTTP/PG 36개와 관련 backend 115개, V41→42/fresh
migration 정밀 검사2개, HELD 응답과 현재 assignment의 SQL 대조4개를
원본 JUnit·hash와 함께 확인했다. 원래 T11 110개 assertion과 grant는
변경하지 않았다. 부족10과 무PO 수령의 미배분90은 별도 책임이며 후자는
계속 OPEN이다. 원래 alternative-close의 전체 통과를 주장하지 않는다.

## T09 판매 원천과 입력 연결

명시적으로 새로 만든 fictional QC·CUSTOMER·COMMERCIAL와 SELL/DISPATCH
규제 원천을 공개 원문·claim·검증·canonical·typed 기록으로 연결했다.
원래 actor/grant와 94개 assertion bytes, 다른 일곱 subcase는 보존했다.
실제 leaf revision 한 개와 allocation ID의 응답 envelope 네 개만
엄격한 역변환 ledger로 교정했다. 후속 조회3개는 기존 Work의 placeId를
그대로 요청 scope에 연결했다. worker의 backend SYNC는 중복 통합하지
않았다. source actor의 category capability 세 개는 실제 정책이 추가로
검사하며 각각 제거한 HTTP 반례의 FORBIDDEN도 확인했다.

실제 reserve·pick·dispatch는 각각60/100 BOX가 APPLIED이고 replay 뒤에도
allocation 한 개다. CONSUMED allocation→dispatch→cargo를 실제 SQL로
대조했다. 최종 warehouse/transit은40/60 또는0/100이다. 원천 정정 후
적격량60→0과 예약 무효과도 확인했다. 공개 getEvidence는 기존 권한대로
cumulative200, exists403을 유지한다.

후속 worker의 HTTP/PG는8 PASS·2 FAIL이다. 선택한 원래47개 oracle은
27 PASS·20 FAIL이며 나머지47개는 별도 subcase로 NOT_RUN이다.
무PO 수령의 contributedQuantity0, 목표/입력·이동 ID 조회 연결이 남았다.
held100은 장소를 지정하지 않은 품목 전체 조회의 정상 결과다. 기존
Work 입력을 근거로 정확히3개 조회에 창고를 명시했다. 같은 시점의 창고/
품목 전체/운송 조회는 처음100/100/0, 출고 후40/100/60 또는0/100/100이며
별도 read-only SQL과 같고 업무 효과는 없다. 전역 집계·기대값·PO는
변경하지 않았다.

독립 원본 보존·SQL·HTTP·JAR proof는
`.local/resume-20261023/t09-input-review/final-preservation-http-proof.json`이다.
HTTP build 뒤 Python 보존 test 한 파일이 추가 수정된 차이도 기록했다.
실행 Java와 입력 hash는 같고 최종 Python28개는 root에서 다시 PASS했다.
후속3be6a117의 원래94개 assertion bytes·fixture·feature와 요청3개 외
변경 없음, raw artifact68개·JAR·실행 Java hash를 독립 대조했다.
`.local/resume-20261023/t09-warehouse-integrated/worker-proof.json`을 따른다.
해당 실행도 HTTP build 뒤 Python test 한 파일이 보강됐으며 실행 Java와
입력은 동일하다. 자체 container·키·HTTP port 정리도 독립 확인했다.
통합46b1a8eb에서 T09 Python30개, T11 parent 보존6개, resolver와 입력
직렬화25개가 모두 PASS했다. 생성기를 다시 실행해 tracked 변경 없음을
확인했고 prepare는41 case·802 subcase·24,260 assertion·문제0·asset12
PASS다. 이는 준비 검사이며 전체 runtime gate는 false로 유지한다.

## 통합과 결정적 검증

| 변경 | worker commit | Task branch commit |
|---|---|---|
| T09 새 fictional 입력 | 3d323121 | 8e1c86eb |
| 규제 UUID와 역변환 연결 | 6fb96efa | 14d75879 |
| 실제 leaf revision | 1c2f40b2 | 3d16b8d8 |
| allocation 응답 경로 | 625de825 | 3d8a3111 |
| 목표 부족 의무 | f2139fe0 | 37cb940e |
| V42 root 불변성 | a4e54342 | 9fa4ea0b |
| 유일 조건·실제 successor 보존 | b7f55549 | e8c66499 |
| 품목 범위 공개 eligibility | 999bbccd | 905e04b8 |
| 기존 Work의 창고 조회 범위 | 3be6a117 | 46b1a8eb |

결합 harness의 첫 실행은659 PASS·1 FAIL·70 opt-in SKIP이었다. 새 원천이
참조하는 실제 effect ID를 기존 직렬화 unit mock이 발급하지 않았다.
coordinator가 해당 응답 shape와 source event 수만 보완한276df5b3에서
관련25개가 PASS했다. 실제 제품 응답이나 업무 oracle은 바꾸지 않았다.

고정 source는 `e8c6649999a42219cbd07b4f4477e3d63b9b0360`이다.
tracked source3,158개를 실행 전후 대조해 drift0을 확인했다.
전체 backend740개는 한 번의 실행에서 실패·오류·SKIP0, Maven exit0이다.
전체 harness는740개 중660 PASS·80 opt-in SKIP·실패/오류0이며 실제 PG
observer42개가 포함된다. prepare는41 case·802 subcase·24,260 assertion·
문제0·asset12 PASS이고 runtime/artifact 인수는 NOT_RUN, gate는 false다.

backend 종료 때 System.exit(0) 후30초를 넘겨 Surefire가 fork JVM을
종료한 경고를 보존했다. 이 경고를 숨기거나 JUnit 실패로 바꾸지 않았다.
해당 실행이 만든 container36개의 부재를 독립 확인했고 무관한 실행의
container는 건드리지 않았다. 원본 JUnit76개와 dump·로그·cleanup proof는
`.local/resume-20261023/shortfall-t09-integrated/`에 있다.

이후 품목 범위 공개 eligibility 수정999bbccd를905e04b8에 통합했다.
기존 per-segment evaluator와 공개 권한으로 각 구간을 반환하고 관련 없는
원천의 근거·만료 경계 유출을 막는다. worker46개와 cleanup15개가 PASS며
root의 영향 범위 결합148개도 실패·오류·SKIP0, Maven exit0이다.
source3,161개 drift0, 생성 container4개의 부재와 종료 경고 없음을
확인했다. `.local/resume-20261023/public-scope-integrated/proof.json`과
[공개 적격성 계약](public-scope-eligibility.md)을 따른다. 이전 전체 backend
740개를 이 후속 source의 전체 실행 결과로 표시하지 않는다.

사용자의 최신 지시로 이후 Ultrafast를 사용하지 않는다. coordinator의
현재 serviceTier는 default이며 새 Codex 위임도 default로 지정한다.
이미 종료된 worker의 Ultrafast 실행 근거는 당시 기록대로 보존한다.

제품 실모델 client 호출은0회다. 실제 규제·운영 identity·BTP 인수와
최종 매뉴얼·검토는 미완료다. main/PR merge와 유료 배포를 실행하지 않았다.
