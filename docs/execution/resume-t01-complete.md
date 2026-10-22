# T01 실제 실행 결과와 남은 인수 범위

T01의 12개 subcase와 125개 assertion이 모두 PASS다. 모든 subcase의
runtimeComplete=true이며 실제 action60개를 실행했다. partial fixture 실행이므로
전체 ontology Task나 S1–S6 선행 profile 인수 완료를 뜻하지 않는다.

## 범위와 결과

- 작업 경로는 `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-9d6cb28b`다.
- 시작 commit은 `5861058e`이고 최종 실행 source commit은
  `7754cd87978d15d2bb693efdbbc8ebcd005d1f3d`다.
- 실행 source는 clean이며 workingTreeDirty=false다. 이 문서의 결과 기록은
  실행 후 별도 문서 commit으로 남긴다.
- same-world, inventory, supply-basis, waiting, responsibility,
  customer-impact와 제외 업무6개가 모두 PASS다.
- 현재 보유100, 판매 적격60, 누적 도착100, 보류40을 구별한다.
  동일 snapshot, 의무 tuple, 고객 후보C의 실제 연결을 확인했다.

## 구현과 원천

T01SourceFacts는 명시적 원문·event kind·subject·범위·수량·단위·정책을
받는다. 실제 서버의 recordActivity, matchSourceIdentity,
linkCanonicalOccurrence를 호출해 원천8개를 대조·발행한다. VERIFIED 행이나
적격60을 직접 seed하지 않는다. QC·고객·처분 허용 및 기관 제출·허용도
실제 도메인 명령으로 기록한다. SELL 정책은 fixture가 명시한
T01-eligibility-v1이고 기관 정책은 가상임을 명시한다.

수령60+40과 보류40은 원천에 연결된 역사 fixture로 설치한다. 기존
INITIAL_BALANCE를 같은 물량의 RECEIPT 근거로 연결하며 물량을 추가로
생성하지 않는다. 보유량을 누적 도착량에 복사하지 않는다. 역사 보류의
의무는 기존 O1 담당·다음 행동 tuple이며 setup을 command 인수로 세지 않는다.
setup artifact의 sourceFactEvidence는 실제 HTTP command30개의 결과와
역사 fixture 구분을 보존한다.

WorkReadHandler는 직접 LOT와 SubjectLinks의 실제 관계로 scope를 제한한다.
WorkSupplyRead는 QualityEligibility의 현재 결과에서 공급 가능 물량과
근거를 읽는다. LotCustomerRead는 발행된 SUBJECT_OF/CUSTOMER 관계와
현재 조회 권한, 같은 item을 확인하며 오염 확정이 아닌 고객 후보를 반환한다.

기존 ObjectRelations의 끝점 whitelist에는 Work·Customer가 없었다.
V37__work_customer_read_relations.sql로 같은 조직의 실제 Work·Customer만
추가했고 정의·cardinality·cycle 검사는 유지했다. 새 관계가 있는
QuantitySegment 명사 조회도 권한이 있는 Work 끝점을 읽을 수 있다.

## Oracle 유지

12개 subcase와125개 assertion은 유지했다. 실패를 주석이나 삭제로 숨기지
않았다. 근거 집합은 실제 canonical·원문·정책8개를 exact 대조한다.
관계는 존재하지 않는 type 열 대신 실제 relationDefinitionId로 검증한다.
전체 segmentIds와 공급 가능한 eligibleSegmentIds를 구별한다.

질문의 불필요한 selector를 제거해 동일한 scope의 snapshot을 비교한다.
고객별 공급 조회에서는 custody와 적격성에 같은 customer selector를 넘긴다.
공통 installer의 wait 변환은 T01에 한정해 다른 fixture의 표현을 보존한다.
AssessmentService/GoalInput, recovery driver/process/clock, V5/T26,
native-client/model-binding 소유 파일은 수정하지 않았다.

## 실행 증거

Java21 PATH, npm ci, Docker PostgreSQL을 사용했다. 실제 실행 command는
다음과 같고 exit0이다.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home/bin:$PATH \
./verify scenarios --actual verification/cases/T01/case.json
```

증거 디렉터리는 다음과 같다.

`verification/harness/target/evidence/actual-scenarios-run-8ba26197-34c0-441e-94c8-d14acab10200/`

- scenarios.json: 12 PASS,125 assertion PASS, actual action60개다.
- t01-source-verification.txt: 원문→claim 및 원문→canonical 연결96개,
  실제 대조·발행 결과와 LOT 물량·의무 tuple을 독립 재검사했다.
- 원천 연결을 제거한 report 반례는 verifier가 거부했다.
- backend-read-tests.log: S1ReadIntegrationTest 13 PASS다. 다른 LOT 제외,
  SubjectLinks 기반 동일 snapshot, 실제 의무, 고객 후보, 없는 끝점 거부를
  포함한다.
- harness-tests.log: T01SourceFactsTest4, ChannelsContractTest27,
  RecoveryGoalDefinitionTest2 PASS다.
- generator-tests.log: 재현성3개 PASS다. validate.log도 exit0이다.
- 초기 독립 SQL 대조는 완료된7개 fixture 조직에서 수령100·보류40·허용
  근거3개와 실제 canonical 연결을 확인했다. 별도 artifact에 보존했으며
  이를12개 SQL 대조로 확대하지 않는다.

최종 fixture SHA256은
`9e79557bf1571f8c1209d331c098d02c2bddedd2e9e9264da064698ede6af691`이다.
case SHA256은
`679ccab6ff33095f74adcc70bdefea0943cabddb5ae0e1f8b8e2a9744596a037`이다.
실행 jar SHA256은
`58870b1f50dfa3d34701f7ea17b068222fc885450e0427bf46bdee13f9638d3e`이다.

## 남은 범위

T01의 요청된 oracle 실패는0이다. 기존 partialFixtures=true,
relaxWire=false 실행을 사용했다. 사용하지 않는 불완전 SL1 선언은
omittedFacts에, 해시만 있는 delivery·temperature는 notRepresentedFacts에
남겼다. 이를 소비·검증 완료로 바꾸지 않았다. 사용한 원천8개는 모두 실제
대조·발행됐고 원천 verifier96개를 통과했다.

profile gateComplete=false는 별도 선행 profile 인수가 없다는 뜻이다.
전체 Task/Step2 종료, 실모델, 규제, BTP 인수를 주장하지 않는다.
재위임·모델 호출·push·PR·외부 메시지는 실행하지 않았다. live catalog에서
codex/gpt-6.1-sol의 Ultrafast 지원을 확인했으나 이 worker의 effective tier를
별도 증명하는 telemetry는 노출되지 않았다.

## 2026-10-23 통합 source의 실제 재검증

clean source `9a2e3df763ad5817f03bb9d0a197ec6482a7275a`를 별도
worktree `t3-690c7777`에서 빌드·실행했다. native dynamic Workflow
`wf_c50cb2c6-a3e`의 prepare·execute·contract-review·validate agent
4개는 각각의 runtime metadata에서 `claude-haiku-5-5`임을 확인했다.
execute와 contract-review를 병렬 수행하고 execute 완료 뒤 validate를
시작했다. 실제 command는 위와 같은 `./verify scenarios --actual`의
T01 선택이며 foreground에서 한 번 실행하고 exit0으로 끝났다.

12개 고유 subcase와 125개 assertion이 모두 PASS다. 실제 action60개,
각 runtimeComplete=true와 missingAdapters=[]를 coordinator가 전부
확인했다. 원천 verifier도 12개 fixture의 96개 연결을 통과했다.
worker의 정적 review가 주장한 attachEvidence·Fact alias 누락은
실제 command와 runtime aliasMap·원천 연결 증거에 대조해 기각했다.

원본 report는 해당 worktree의
`verification/harness/target/evidence/actual-scenarios-run-9e8db30a-ab5b-4e5c-83ff-c498ca75a385/scenarios.json`이다.
SHA256은
`7c7dc2248e1cc4a68f827e1be4fe86827ef3ba0c0800c0590564b058ac390ef8`이다.
실행 jar SHA256은
`92b01495e3a16b1f9be77fed1ad1c32c4e7f42746e86e3f50af6a54ff5247612`다.
coordinator 대조 결과와 agent metadata hash는 root의
`.local/resume-20261023/t01-integration-proof.json`에 보존했다.
실행 전후 source는 clean이고 변경이 없다. runner가 만든 application과
container만 정리했으며 기존 container는 보존했다.

gateComplete=false는 명시적 case 선택으로 802개 중 12개를 실행한
현재 profile 조건에 따른다. report의 공통 reason 문구만으로 adapter
누락이 있다고 해석하지 않는다. partialFixtures=true·relaxWire=false이고
모든 fixture에 SL1 미설치와 delivery·temperature 원천 미발행이 남았다.
T01 선택의 실행 성공을 전체 profile·규제·실모델·BTP 인수로 확대하지
않는다. 실제 모델 인수 호출은 0회다.
