# 권한 영역 작성 검증 증거

조직·위임·경로·멱등·현재 철회의 거부가 observer 미실행을 효과0으로
오인하지 않도록 실제 schema, AssertionEngine 표본 및 Gherkin 발견
결과를 보존한다. T08/C3/V4/V6/V7의 합계는437 subcase,
13,937 assertion, 13 oracle, 54 named observation이다.

- `checks.json`: 명령·exit·case/fixture/feature hash와 증거 hash다.
- `validate.log.gz`:5개 schema 검증이 exit0이다.
- `harness.log.gz`, `authority-assertions-junit.xml`: 전체139 검사와
  소유 표본9 검사 모두 실패/오류/skip0이다. 수량·단위·중복·owner·
  version·ID·observer 누락과 조회 모델의 실제 쓰기 호출 mutant다.
- `feature-red-summary.json`: expected/discovered/started/failed와
  NOT_IMPLEMENTED failure가 각각437이며 scenario skip0이다.
- `feature-red-cucumber.json.gz`, `contract-red.log.gz`: 실제
  file selector가 수행한 Korean Gherkin 전체 결과 원본이다.
  첫 필수 assertion 실패 뒤의 skipped step은 scenario skip과 다르다.
- `contract-red-case-evidence.json.gz`: 실제437개 CaseFeatureTest
  case evidence 원본을 JSON array로 묶는다. case/fixture hash는
  검증된 작업파일과 정확히 일치한다.
- `scenarios-not-run.json.gz`, `scenarios-not-run.log.gz`,
  `scenarios-wrapper-commands.json`:437개 제품 subcase 전부 NOT_RUN,
  exit2와 gateComplete=false를 보존한다.

검증 시 common dependency HEAD는
`77c2df3440b6d8b362bfb52fed677e441c372b4c`다. 소유 산출물은 아직
commit 전이었으므로 workingTreeDirty=true이며 개별 case/fixture/
checkedFeatureHash가 정확한 검증 입력을 고정한다. 검증 뒤 Git
whitespace 검사에서 feature의 빈 EOF 한 줄만 제거했다. 현재
featureHash와 checkedFeatureHash를 구별하며 모든 Gherkin 문장과
scenario/action/assertion identity는 같다. 증거를 작성한 뒤 commit
하므로 그 후 commit hash를 검증 당시 codeCommit으로 꾸미지 않는다.

`.gz`는 표준 gzip이며 압축을 풀면 JSON 또는 실행 log 원본이다.
실제 API/독립 DB/MCP/worker/barrier/host/model adapter가 없으므로
제품 인수와 실제 model 실행은 NOT_RUN이다. scripted model이 실제
모델 성공을 대신하지 않고 unavailable observer가 factual0을 만들지
않는다. 통합 coverage와 의미 동등성은 Step2 review에서 확인한다.
