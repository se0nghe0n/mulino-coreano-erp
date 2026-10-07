# inventory 계약 검증 증거

제품 adapter가 없는 상태를 제품 PASS나0효과로 오해하지 않도록
실제 Gherkin RED와 assertion 자체 검사를 구분한다. 실행 시점의
공통 HEAD는 `77c2df3`이고 소유 계약 파일은 아직 commit 전이었다.
case/fixture hash는 각 subcase evidence에 기록됐다.

- `harness-commands.json`, `harness.log.gz`: `./verify harness` exit0,
  142 tests, failure/error/skip0이다. 공통130과 inventory12를 포함한다.
- `inventory-selftests.xml`: 실제 AssertionEngine으로 틀린 수량·단위,
  중복 실물, 누락 owner, 상태·버전 오염, bound 위반을 거부했다.
  표본은 고정 관찰이며 제품 driver가 아니다.
- `validate-commands.json`, `validate.log.gz`: 소유7 case schema 모두
  PREPARATION_SCHEMA_VALID, exit0이다. 앞선 source 중복 own bug는
  observation.sources 중복 제거로 수정됐고 전체 검사에서 통과했다.
- `contract-red-commands.json`, `contract-red.log.gz`,
  `feature-red-summary.json`, `feature-red-cucumber.json`,
  `feature-red-junit.xml`: 실제 file-selector 실행에서 expected,
  discovered, started, NOT_IMPLEMENTED failure는 각각32, skip0,
  exit1이다. 각 case/evidence 아래32개 subcase 원본 evidence가 있다.
- `scenarios-notrun-commands.json`, `scenarios-notrun.log.gz`,
  `scenarios-notrun.json`: 대표 T03의8 subcase는 NOT_RUN, exit2다.
- `summary.json`: 7 case·32 subcase·526 assertion·20 oracle·91 named
  observation과 명령/증거 경로·hash를 기록한다. fixture source JSON
  192개의 실제 SHA256과 Gherkin 행동/assertion 순서를 정적 대조했다.

RED는 각 시나리오의 첫 unavailable assertion에서 실패하므로526개
assertion의 제품 결과를 검증한 증거가 아니다. 제품 API/독립DB/실제
host·barrier/실모델/규제·BTP 실행은 NOT_RUN이다. 경합의 실제 lock,
현재 조건 recheck, terminal ACK와 원 행 mapping은 이후 구현에서
이 계약을 실행해야 확정된다. 제품 fake나 옛 구현을 사용하지 않았다.

raw log는 공백과 stacktrace 들여쓰기를 바꾸지 않고 lossless gzip으로
보존한다. summary.json의 uncompressedSha256으로 원 bytes를 대조한다.
