# language: ko
@T25 @D25 @contract-red
기능: 독립 traceability와 증거 gate
  시나리오: 독립41 case/D26 catalog 연결과 none 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-none"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "prepared-only" assertion으로 "prepared-only: /data/hostObservation/extractor/rawRows/gate/preparationStatus의 실제 equals 기대값은 'PREPARED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "runtime-not-run" assertion으로 "준비 보고 입력은 실행 증거가 없으므로 runtime은 NOT_RUN이다. 실제 runtime manifest의 상태를 고정하는 단언이 아니다."를 확인한다
    그러면 "whole-gate-open" assertion으로 "준비 보고만으로 전체 gate를 닫지 않는다."를 확인한다
    그러면 "semantic-review-required" assertion으로 "semantic-review-required: /data/hostObservation/extractor/rawRows/gate/semanticOracleEquivalence의 실제 equals 기대값은 'REQUIRES_CASE_REVIEW'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-runtime-artifacts-fabricated" assertion으로 "준비 보고에는 runtime receipt가 없다. 검증기가 준비 입력에서 runtime artifact를 만들어 내지 않는다."를 확인한다
    그러면 "preparation-model-not-run" assertion으로 "preparation-model-not-run: /data/hostObservation/extractor/rawRows/gate/modelStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "preparation-regulatory-not-run" assertion으로 "preparation-regulatory-not-run: /data/hostObservation/extractor/rawRows/gate/regulatoryStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "preparation-no-regulatory-claim" assertion으로 "preparation-no-regulatory-claim: /data/hostObservation/extractor/rawRows/regulatoryReviews의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 dropOracle 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-dropOracle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'dropOracle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 dropObservation 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-dropObservation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'dropObservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 removeRuntimeArtifact 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-removeRuntimeArtifact"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'removeRuntimeArtifact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "baseline-required-paths-pass" assertion으로 "변조 전 실제 runtime 입력은 현재 T25 실행을 뺀 모든 필수 경로가 PASS여야 변조의 효과를 관찰할 수 있다."를 확인한다
    그러면 "mutated-required-paths-not-pass" assertion으로 "변조된 사본은 필수 경로 PASS를 유지하지 못한다. NOT_RUN·부분 실행·위반을 PASS로 숨기지 않는다."를 확인한다
    그러면 "baseline-has-t20-artifact" assertion으로 "삭제 대상 T20 runtime artifact가 변조 전 입력에 실제로 있어야 MISSING_RUNTIME_ARTIFACT가 의미 있다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 replaceWithStubPass 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-replaceWithStubPass"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'replaceWithStubPass'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "baseline-required-paths-pass" assertion으로 "변조 전 실제 runtime 입력은 현재 T25 실행을 뺀 모든 필수 경로가 PASS여야 변조의 효과를 관찰할 수 있다."를 확인한다
    그러면 "mutated-required-paths-not-pass" assertion으로 "변조된 사본은 필수 경로 PASS를 유지하지 못한다. NOT_RUN·부분 실행·위반을 PASS로 숨기지 않는다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 skipCase 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-skipCase"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'skipCase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 mandatoryWaiver 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-mandatoryWaiver"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'mandatoryWaiver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 partialPass 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-partialPass"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'partialPass'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "baseline-required-paths-pass" assertion으로 "변조 전 실제 runtime 입력은 현재 T25 실행을 뺀 모든 필수 경로가 PASS여야 변조의 효과를 관찰할 수 있다."를 확인한다
    그러면 "mutated-required-paths-not-pass" assertion으로 "변조된 사본은 필수 경로 PASS를 유지하지 못한다. NOT_RUN·부분 실행·위반을 PASS로 숨기지 않는다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 confirmedViolation 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-confirmedViolation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "input-snapshot-kind" assertion으로 "검증기가 요청한 입력 snapshot 종류를 그대로 읽었는지 확인한다. 준비 보고와 실제 runtime manifest를 섞지 않는다."를 확인한다
    그러면 "baseline-input-valid" assertion으로 "같은 입력의 변조 전 사본은 유효해야 한다. 변조가 PASS→FAIL 전이를 만든 것만 거부 증거로 센다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'confirmedViolation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "baseline-required-paths-pass" assertion으로 "변조 전 실제 runtime 입력은 현재 T25 실행을 뺀 모든 필수 경로가 PASS여야 변조의 효과를 관찰할 수 있다."를 확인한다
    그러면 "mutated-required-paths-not-pass" assertion으로 "변조된 사본은 필수 경로 PASS를 유지하지 못한다. NOT_RUN·부분 실행·위반을 PASS로 숨기지 않는다."를 확인한다
  시나리오: 전체 catalog named observation의 실제 PASS assertion·artifact 연결
    먼저 사례 파일 "verification/cases/T25/case.json"의 "runtime-links-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-runtime-sources" 행동을 수행한다
    그러면 "input-snapshot-kind" assertion으로 "input-snapshot-kind: /data/hostObservation/extractor/rawRows/input/snapshotKind의 실제 equals 기대값은 'REQUIRED_PATH_RUNTIME_EVIDENCE'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "all-case-profile-artifacts" assertion으로 "all-case-profile-artifacts: 비어 있지 않은 실제 원행마다 caseId, subcaseId, profile, assertionId, path, sha256, sizeBytes, scope, fixtureHash, codeCommit, command, expected, observed, exitCode, status를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "no-link-violations" assertion으로 "no-link-violations: /data/hostObservation/extractor/rawRows/validation/issues의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "content-review-evidence" assertion으로 "content-review-evidence: /data/hostObservation/extractor/rawRows/semanticReview/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "required-paths-pass" assertion으로 "현재 T25 실행을 제외한 모든 필수 case/profile 경로가 실제 PASS여야 한다. NOT_RUN·FAIL link를 연결만으로 통과시키지 않는다."를 확인한다
    그러면 "profile-result-schema" assertion으로 "schema profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-contracts" assertion으로 "contracts profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-scenarios" assertion으로 "scenarios profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-recovery" assertion으로 "recovery profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-mcp" assertion으로 "mcp profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-skills" assertion으로 "skills profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-model" assertion으로 "model profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-local-deployment" assertion으로 "local-deployment profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-btp-deployment" assertion으로 "btp-deployment profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "profile-result-regulatory" assertion으로 "regulatory profile의 결과는 별도 행 하나로 남고 실제 PASS여야 한다. 다른 profile의 PASS나 부분 실행으로 이 profile을 채우지 않는다."를 확인한다
    그러면 "runtime-artifacts-no-fail" assertion으로 "runtime-artifacts-no-fail: /data/hostObservation/extractor/rawRows/runtimeArtifacts의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "assertion-links-no-fail" assertion으로 "평면화한 모든 assertion link에 FAIL·NOT_RUN이 없어야 한다. 현재 T25 link만 CURRENT_EXECUTION으로 구별한다."를 확인한다
    그러면 "runtime-artifacts-no-not-run" assertion으로 "runtime-artifacts-no-not-run: /data/hostObservation/extractor/rawRows/runtimeArtifacts의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "assertion-links-no-not-run" assertion으로 "평면화한 모든 assertion link에 FAIL·NOT_RUN이 없어야 한다. 현재 T25 link만 CURRENT_EXECUTION으로 구별한다."를 확인한다
    그러면 "runtime-artifacts-no-exit1" assertion으로 "필수 경로 artifact의 실제 exit code는 0이다. 의도된 RED(1)·NOT_RUN(2)·환경 오류(3)를 PASS artifact로 연결하지 않는다."를 확인한다
    그러면 "runtime-artifacts-no-exit2" assertion으로 "필수 경로 artifact의 실제 exit code는 0이다. 의도된 RED(1)·NOT_RUN(2)·환경 오류(3)를 PASS artifact로 연결하지 않는다."를 확인한다
    그러면 "runtime-artifacts-no-exit3" assertion으로 "필수 경로 artifact의 실제 exit code는 0이다. 의도된 RED(1)·NOT_RUN(2)·환경 오류(3)를 PASS artifact로 연결하지 않는다."를 확인한다
    그러면 "profile-links-000-scenarios" assertion으로 "T01.two-entrypoints/same-world의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-000-api_response" assertion으로 "T01.two-entrypoints/same-world에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-000-db_snapshot" assertion으로 "T01.two-entrypoints/same-world에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-000" assertion으로 "T01.two-entrypoints/same-world에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-000" assertion으로 "link-name-000: /data/hostObservation/extractor/rawRows/namedObservations/0/observationName의 실제 equals 기대값은 'same-world'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-000" assertion으로 "link-oracle-000: /data/hostObservation/extractor/rawRows/namedObservations/0/oracleId의 실제 equals 기대값은 'T01.two-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-001-scenarios" assertion으로 "T01.five-competency-questions/answers-grounded의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-001-api_response" assertion으로 "T01.five-competency-questions/answers-grounded에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-001-db_snapshot" assertion으로 "T01.five-competency-questions/answers-grounded에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-001" assertion으로 "T01.five-competency-questions/answers-grounded에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-001" assertion으로 "link-name-001: /data/hostObservation/extractor/rawRows/namedObservations/1/observationName의 실제 equals 기대값은 'answers-grounded'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-001" assertion으로 "link-oracle-001: /data/hostObservation/extractor/rawRows/namedObservations/1/oracleId의 실제 equals 기대값은 'T01.five-competency-questions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-002-scenarios" assertion으로 "T01.excluded-effects/excluded-effects-created의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-002-api_response" assertion으로 "T01.excluded-effects/excluded-effects-created에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-002-db_snapshot" assertion으로 "T01.excluded-effects/excluded-effects-created에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-002" assertion으로 "T01.excluded-effects/excluded-effects-created에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-002" assertion으로 "link-name-002: /data/hostObservation/extractor/rawRows/namedObservations/2/observationName의 실제 equals 기대값은 'excluded-effects-created'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-002" assertion으로 "link-oracle-002: /data/hostObservation/extractor/rawRows/namedObservations/2/oracleId의 실제 equals 기대값은 'T01.excluded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-003-scenarios" assertion으로 "T01.excluded-effects/unsupported-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-003-api_response" assertion으로 "T01.excluded-effects/unsupported-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-003-db_snapshot" assertion으로 "T01.excluded-effects/unsupported-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-003" assertion으로 "T01.excluded-effects/unsupported-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-003" assertion으로 "link-name-003: /data/hostObservation/extractor/rawRows/namedObservations/3/observationName의 실제 equals 기대값은 'unsupported-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-003" assertion으로 "link-oracle-003: /data/hostObservation/extractor/rawRows/namedObservations/3/oracleId의 실제 equals 기대값은 'T01.excluded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-004-scenarios" assertion으로 "T02.issuer-identity/distinct-item-ids의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-004-api_response" assertion으로 "T02.issuer-identity/distinct-item-ids에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-004-db_snapshot" assertion으로 "T02.issuer-identity/distinct-item-ids에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-004" assertion으로 "T02.issuer-identity/distinct-item-ids에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-004" assertion으로 "link-name-004: /data/hostObservation/extractor/rawRows/namedObservations/4/observationName의 실제 equals 기대값은 'distinct-item-ids'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-004" assertion으로 "link-oracle-004: /data/hostObservation/extractor/rawRows/namedObservations/4/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-005-scenarios" assertion으로 "T02.issuer-identity/distinct-lot-ids의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-005-api_response" assertion으로 "T02.issuer-identity/distinct-lot-ids에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-005-db_snapshot" assertion으로 "T02.issuer-identity/distinct-lot-ids에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-005" assertion으로 "T02.issuer-identity/distinct-lot-ids에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-005" assertion으로 "link-name-005: /data/hostObservation/extractor/rawRows/namedObservations/5/observationName의 실제 equals 기대값은 'distinct-lot-ids'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-005" assertion으로 "link-oracle-005: /data/hostObservation/extractor/rawRows/namedObservations/5/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-006-scenarios" assertion으로 "T02.issuer-identity/implicit-merges의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-006-api_response" assertion으로 "T02.issuer-identity/implicit-merges에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-006-db_snapshot" assertion으로 "T02.issuer-identity/implicit-merges에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-006" assertion으로 "T02.issuer-identity/implicit-merges에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-006" assertion으로 "link-name-006: /data/hostObservation/extractor/rawRows/namedObservations/6/observationName의 실제 equals 기대값은 'implicit-merges'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-006" assertion으로 "link-oracle-006: /data/hostObservation/extractor/rawRows/namedObservations/6/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-007-scenarios" assertion으로 "T02.identifier-period-conflict/code-conflict의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-007-api_response" assertion으로 "T02.identifier-period-conflict/code-conflict에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-007-db_snapshot" assertion으로 "T02.identifier-period-conflict/code-conflict에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-007" assertion으로 "T02.identifier-period-conflict/code-conflict에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-007" assertion으로 "link-name-007: /data/hostObservation/extractor/rawRows/namedObservations/7/observationName의 실제 equals 기대값은 'code-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-007" assertion으로 "link-oracle-007: /data/hostObservation/extractor/rawRows/namedObservations/7/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-008-scenarios" assertion으로 "T02.identifier-period-conflict/silent-remap의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-008-api_response" assertion으로 "T02.identifier-period-conflict/silent-remap에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-008-db_snapshot" assertion으로 "T02.identifier-period-conflict/silent-remap에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-008" assertion으로 "T02.identifier-period-conflict/silent-remap에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-008" assertion으로 "link-name-008: /data/hostObservation/extractor/rawRows/namedObservations/8/observationName의 실제 equals 기대값은 'silent-remap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-008" assertion으로 "link-oracle-008: /data/hostObservation/extractor/rawRows/namedObservations/8/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-009-scenarios" assertion으로 "T02.identifier-period-conflict/code-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-009-api_response" assertion으로 "T02.identifier-period-conflict/code-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-009-db_snapshot" assertion으로 "T02.identifier-period-conflict/code-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-009" assertion으로 "T02.identifier-period-conflict/code-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-009" assertion으로 "link-name-009: /data/hostObservation/extractor/rawRows/namedObservations/9/observationName의 실제 equals 기대값은 'code-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-009" assertion으로 "link-oracle-009: /data/hostObservation/extractor/rawRows/namedObservations/9/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-010-contracts" assertion으로 "T02.conversion-not-substitution/converted-content의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-010-scenarios" assertion으로 "T02.conversion-not-substitution/converted-content의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-010-api_response" assertion으로 "T02.conversion-not-substitution/converted-content에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-010-db_snapshot" assertion으로 "T02.conversion-not-substitution/converted-content에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-010" assertion으로 "T02.conversion-not-substitution/converted-content에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-010" assertion으로 "link-name-010: /data/hostObservation/extractor/rawRows/namedObservations/10/observationName의 실제 equals 기대값은 'converted-content'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-010" assertion으로 "link-oracle-010: /data/hostObservation/extractor/rawRows/namedObservations/10/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-011-contracts" assertion으로 "T02.conversion-not-substitution/conversion-created-inventory의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-011-scenarios" assertion으로 "T02.conversion-not-substitution/conversion-created-inventory의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-011-api_response" assertion으로 "T02.conversion-not-substitution/conversion-created-inventory에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-011-db_snapshot" assertion으로 "T02.conversion-not-substitution/conversion-created-inventory에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-011" assertion으로 "T02.conversion-not-substitution/conversion-created-inventory에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-011" assertion으로 "link-name-011: /data/hostObservation/extractor/rawRows/namedObservations/11/observationName의 실제 equals 기대값은 'conversion-created-inventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-011" assertion으로 "link-oracle-011: /data/hostObservation/extractor/rawRows/namedObservations/11/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-012-contracts" assertion으로 "T02.conversion-not-substitution/conversion-created-order-fulfilment의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-012-scenarios" assertion으로 "T02.conversion-not-substitution/conversion-created-order-fulfilment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-012-api_response" assertion으로 "T02.conversion-not-substitution/conversion-created-order-fulfilment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-012-db_snapshot" assertion으로 "T02.conversion-not-substitution/conversion-created-order-fulfilment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-012" assertion으로 "T02.conversion-not-substitution/conversion-created-order-fulfilment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-012" assertion으로 "link-name-012: /data/hostObservation/extractor/rawRows/namedObservations/12/observationName의 실제 equals 기대값은 'conversion-created-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-012" assertion으로 "link-oracle-012: /data/hostObservation/extractor/rawRows/namedObservations/12/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-013-contracts" assertion으로 "T02.conversion-not-substitution/repackaging-effects의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-013-scenarios" assertion으로 "T02.conversion-not-substitution/repackaging-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-013-api_response" assertion으로 "T02.conversion-not-substitution/repackaging-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-013-db_snapshot" assertion으로 "T02.conversion-not-substitution/repackaging-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-013" assertion으로 "T02.conversion-not-substitution/repackaging-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-013" assertion으로 "link-name-013: /data/hostObservation/extractor/rawRows/namedObservations/13/observationName의 실제 equals 기대값은 'repackaging-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-013" assertion으로 "link-oracle-013: /data/hostObservation/extractor/rawRows/namedObservations/13/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-014-contracts" assertion으로 "T02.conversion-not-substitution/substitution의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-014-scenarios" assertion으로 "T02.conversion-not-substitution/substitution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-014-api_response" assertion으로 "T02.conversion-not-substitution/substitution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-014-db_snapshot" assertion으로 "T02.conversion-not-substitution/substitution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-014" assertion으로 "T02.conversion-not-substitution/substitution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-014" assertion으로 "link-name-014: /data/hostObservation/extractor/rawRows/namedObservations/14/observationName의 실제 equals 기대값은 'substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-014" assertion으로 "link-oracle-014: /data/hostObservation/extractor/rawRows/namedObservations/14/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-015-scenarios" assertion으로 "T03.split-merge-conservation/active-quantity의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-015-api_response" assertion으로 "T03.split-merge-conservation/active-quantity에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-015-db_snapshot" assertion으로 "T03.split-merge-conservation/active-quantity에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-015" assertion으로 "T03.split-merge-conservation/active-quantity에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-015" assertion으로 "link-name-015: /data/hostObservation/extractor/rawRows/namedObservations/15/observationName의 실제 equals 기대값은 'active-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-015" assertion으로 "link-oracle-015: /data/hostObservation/extractor/rawRows/namedObservations/15/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-016-scenarios" assertion으로 "T03.split-merge-conservation/parent의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-016-api_response" assertion으로 "T03.split-merge-conservation/parent에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-016-db_snapshot" assertion으로 "T03.split-merge-conservation/parent에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-016" assertion으로 "T03.split-merge-conservation/parent에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-016" assertion으로 "link-name-016: /data/hostObservation/extractor/rawRows/namedObservations/16/observationName의 실제 equals 기대값은 'parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-016" assertion으로 "link-oracle-016: /data/hostObservation/extractor/rawRows/namedObservations/16/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-017-scenarios" assertion으로 "T03.split-merge-conservation/allocation-transferred의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-017-api_response" assertion으로 "T03.split-merge-conservation/allocation-transferred에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-017-db_snapshot" assertion으로 "T03.split-merge-conservation/allocation-transferred에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-017" assertion으로 "T03.split-merge-conservation/allocation-transferred에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-017" assertion으로 "link-name-017: /data/hostObservation/extractor/rawRows/namedObservations/17/observationName의 실제 equals 기대값은 'allocation-transferred'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-017" assertion으로 "link-oracle-017: /data/hostObservation/extractor/rawRows/namedObservations/17/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-018-scenarios" assertion으로 "T03.split-merge-conservation/allocation-copies의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-018-api_response" assertion으로 "T03.split-merge-conservation/allocation-copies에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-018-db_snapshot" assertion으로 "T03.split-merge-conservation/allocation-copies에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-018" assertion으로 "T03.split-merge-conservation/allocation-copies에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-018" assertion으로 "link-name-018: /data/hostObservation/extractor/rawRows/namedObservations/18/observationName의 실제 equals 기대값은 'allocation-copies'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-018" assertion으로 "link-oracle-018: /data/hostObservation/extractor/rawRows/namedObservations/18/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-019-scenarios" assertion으로 "T03.split-merge-conservation/atomic-genealogy의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-019-api_response" assertion으로 "T03.split-merge-conservation/atomic-genealogy에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-019-db_snapshot" assertion으로 "T03.split-merge-conservation/atomic-genealogy에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-019" assertion으로 "T03.split-merge-conservation/atomic-genealogy에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-019" assertion으로 "link-name-019: /data/hostObservation/extractor/rawRows/namedObservations/19/observationName의 실제 equals 기대값은 'atomic-genealogy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-019" assertion으로 "link-oracle-019: /data/hostObservation/extractor/rawRows/namedObservations/19/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-020-scenarios" assertion으로 "T03.logistics-multiple-lots/pallet-quantity의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-020-api_response" assertion으로 "T03.logistics-multiple-lots/pallet-quantity에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-020-db_snapshot" assertion으로 "T03.logistics-multiple-lots/pallet-quantity에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-020" assertion으로 "T03.logistics-multiple-lots/pallet-quantity에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-020" assertion으로 "link-name-020: /data/hostObservation/extractor/rawRows/namedObservations/20/observationName의 실제 equals 기대값은 'pallet-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-020" assertion으로 "link-oracle-020: /data/hostObservation/extractor/rawRows/namedObservations/20/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-021-scenarios" assertion으로 "T03.logistics-multiple-lots/lot-membership의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-021-api_response" assertion으로 "T03.logistics-multiple-lots/lot-membership에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-021-db_snapshot" assertion으로 "T03.logistics-multiple-lots/lot-membership에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-021" assertion으로 "T03.logistics-multiple-lots/lot-membership에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-021" assertion으로 "link-name-021: /data/hostObservation/extractor/rawRows/namedObservations/21/observationName의 실제 equals 기대값은 'lot-membership'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-021" assertion으로 "link-oracle-021: /data/hostObservation/extractor/rawRows/namedObservations/21/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-022-scenarios" assertion으로 "T03.logistics-multiple-lots/cross-lot-segment-merge의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-022-api_response" assertion으로 "T03.logistics-multiple-lots/cross-lot-segment-merge에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-022-db_snapshot" assertion으로 "T03.logistics-multiple-lots/cross-lot-segment-merge에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-022" assertion으로 "T03.logistics-multiple-lots/cross-lot-segment-merge에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-022" assertion으로 "link-name-022: /data/hostObservation/extractor/rawRows/namedObservations/22/observationName의 실제 equals 기대값은 'cross-lot-segment-merge'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-022" assertion으로 "link-oracle-022: /data/hostObservation/extractor/rawRows/namedObservations/22/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-023-scenarios" assertion으로 "T03.logistics-multiple-lots/membership-time의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-023-api_response" assertion으로 "T03.logistics-multiple-lots/membership-time에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-023-db_snapshot" assertion으로 "T03.logistics-multiple-lots/membership-time에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-023" assertion으로 "T03.logistics-multiple-lots/membership-time에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-023" assertion으로 "link-name-023: /data/hostObservation/extractor/rawRows/namedObservations/23/observationName의 실제 equals 기대값은 'membership-time'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-023" assertion으로 "link-oracle-023: /data/hostObservation/extractor/rawRows/namedObservations/23/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-024-scenarios" assertion으로 "T03.cycle-and-retired-parent/invalid-quantity-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-024-api_response" assertion으로 "T03.cycle-and-retired-parent/invalid-quantity-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-024-db_snapshot" assertion으로 "T03.cycle-and-retired-parent/invalid-quantity-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-024" assertion으로 "T03.cycle-and-retired-parent/invalid-quantity-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-024" assertion으로 "link-name-024: /data/hostObservation/extractor/rawRows/namedObservations/24/observationName의 실제 equals 기대값은 'invalid-quantity-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-024" assertion으로 "link-oracle-024: /data/hostObservation/extractor/rawRows/namedObservations/24/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-025-scenarios" assertion으로 "T03.cycle-and-retired-parent/invalid-allocation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-025-api_response" assertion으로 "T03.cycle-and-retired-parent/invalid-allocation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-025-db_snapshot" assertion으로 "T03.cycle-and-retired-parent/invalid-allocation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-025" assertion으로 "T03.cycle-and-retired-parent/invalid-allocation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-025" assertion으로 "link-name-025: /data/hostObservation/extractor/rawRows/namedObservations/25/observationName의 실제 equals 기대값은 'invalid-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-025" assertion으로 "link-oracle-025: /data/hostObservation/extractor/rawRows/namedObservations/25/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-026-scenarios" assertion으로 "T03.cycle-and-retired-parent/invalid-results의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-026-api_response" assertion으로 "T03.cycle-and-retired-parent/invalid-results에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-026-db_snapshot" assertion으로 "T03.cycle-and-retired-parent/invalid-results에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-026" assertion으로 "T03.cycle-and-retired-parent/invalid-results에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-026" assertion으로 "link-name-026: /data/hostObservation/extractor/rawRows/namedObservations/26/observationName의 실제 equals 기대값은 'invalid-results'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-026" assertion으로 "link-oracle-026: /data/hostObservation/extractor/rawRows/namedObservations/26/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-027-scenarios" assertion으로 "T03.indistinguishable-mixture/source-affected의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-027-api_response" assertion으로 "T03.indistinguishable-mixture/source-affected에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-027-db_snapshot" assertion으로 "T03.indistinguishable-mixture/source-affected에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-027" assertion으로 "T03.indistinguishable-mixture/source-affected에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-027" assertion으로 "link-name-027: /data/hostObservation/extractor/rawRows/namedObservations/27/observationName의 실제 equals 기대값은 'source-affected'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-027" assertion으로 "link-oracle-027: /data/hostObservation/extractor/rawRows/namedObservations/27/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-028-scenarios" assertion으로 "T03.indistinguishable-mixture/current-candidate-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-028-api_response" assertion으로 "T03.indistinguishable-mixture/current-candidate-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-028-db_snapshot" assertion으로 "T03.indistinguishable-mixture/current-candidate-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-028" assertion으로 "T03.indistinguishable-mixture/current-candidate-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-028" assertion으로 "link-name-028: /data/hostObservation/extractor/rawRows/namedObservations/28/observationName의 실제 equals 기대값은 'current-candidate-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-028" assertion으로 "link-oracle-028: /data/hostObservation/extractor/rawRows/namedObservations/28/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-029-scenarios" assertion으로 "T03.indistinguishable-mixture/arbitrary-clean-selection의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-029-api_response" assertion으로 "T03.indistinguishable-mixture/arbitrary-clean-selection에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-029-db_snapshot" assertion으로 "T03.indistinguishable-mixture/arbitrary-clean-selection에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-029" assertion으로 "T03.indistinguishable-mixture/arbitrary-clean-selection에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-029" assertion으로 "link-name-029: /data/hostObservation/extractor/rawRows/namedObservations/29/observationName의 실제 equals 기대값은 'arbitrary-clean-selection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-029" assertion으로 "link-oracle-029: /data/hostObservation/extractor/rawRows/namedObservations/29/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-030-scenarios" assertion으로 "T03.indistinguishable-mixture/trace-certainty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-030-api_response" assertion으로 "T03.indistinguishable-mixture/trace-certainty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-030-db_snapshot" assertion으로 "T03.indistinguishable-mixture/trace-certainty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-030" assertion으로 "T03.indistinguishable-mixture/trace-certainty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-030" assertion으로 "link-name-030: /data/hostObservation/extractor/rawRows/namedObservations/30/observationName의 실제 equals 기대값은 'trace-certainty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-030" assertion으로 "link-oracle-030: /data/hostObservation/extractor/rawRows/namedObservations/30/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-031-contracts" assertion으로 "T04.decimal-boundary/invalid-decimals의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-031-scenarios" assertion으로 "T04.decimal-boundary/invalid-decimals의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-031-api_response" assertion으로 "T04.decimal-boundary/invalid-decimals에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-031-db_snapshot" assertion으로 "T04.decimal-boundary/invalid-decimals에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-031" assertion으로 "T04.decimal-boundary/invalid-decimals에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-031" assertion으로 "link-name-031: /data/hostObservation/extractor/rawRows/namedObservations/31/observationName의 실제 equals 기대값은 'invalid-decimals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-031" assertion으로 "link-oracle-031: /data/hostObservation/extractor/rawRows/namedObservations/31/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-032-contracts" assertion으로 "T04.decimal-boundary/rounded-effects의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-032-scenarios" assertion으로 "T04.decimal-boundary/rounded-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-032-api_response" assertion으로 "T04.decimal-boundary/rounded-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-032-db_snapshot" assertion으로 "T04.decimal-boundary/rounded-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-032" assertion으로 "T04.decimal-boundary/rounded-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-032" assertion으로 "link-name-032: /data/hostObservation/extractor/rawRows/namedObservations/32/observationName의 실제 equals 기대값은 'rounded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-032" assertion으로 "link-oracle-032: /data/hostObservation/extractor/rawRows/namedObservations/32/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-033-contracts" assertion으로 "T04.decimal-boundary/wire-decimals의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-033-scenarios" assertion으로 "T04.decimal-boundary/wire-decimals의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-033-api_response" assertion으로 "T04.decimal-boundary/wire-decimals에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-033-db_snapshot" assertion으로 "T04.decimal-boundary/wire-decimals에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-033" assertion으로 "T04.decimal-boundary/wire-decimals에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-033" assertion으로 "link-name-033: /data/hostObservation/extractor/rawRows/namedObservations/33/observationName의 실제 equals 기대값은 'wire-decimals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-033" assertion으로 "link-oracle-033: /data/hostObservation/extractor/rawRows/namedObservations/33/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-034-scenarios" assertion으로 "T04.hold-versus-disposal/held-after-hold의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-034-api_response" assertion으로 "T04.hold-versus-disposal/held-after-hold에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-034-db_snapshot" assertion으로 "T04.hold-versus-disposal/held-after-hold에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-034" assertion으로 "T04.hold-versus-disposal/held-after-hold에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-034" assertion으로 "link-name-034: /data/hostObservation/extractor/rawRows/namedObservations/34/observationName의 실제 equals 기대값은 'held-after-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-034" assertion으로 "link-oracle-034: /data/hostObservation/extractor/rawRows/namedObservations/34/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-035-scenarios" assertion으로 "T04.hold-versus-disposal/eligible-after-hold의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-035-api_response" assertion으로 "T04.hold-versus-disposal/eligible-after-hold에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-035-db_snapshot" assertion으로 "T04.hold-versus-disposal/eligible-after-hold에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-035" assertion으로 "T04.hold-versus-disposal/eligible-after-hold에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-035" assertion으로 "link-name-035: /data/hostObservation/extractor/rawRows/namedObservations/35/observationName의 실제 equals 기대값은 'eligible-after-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-035" assertion으로 "link-oracle-035: /data/hostObservation/extractor/rawRows/namedObservations/35/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-036-scenarios" assertion으로 "T04.hold-versus-disposal/held-after-disposal의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-036-api_response" assertion으로 "T04.hold-versus-disposal/held-after-disposal에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-036-db_snapshot" assertion으로 "T04.hold-versus-disposal/held-after-disposal에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-036" assertion으로 "T04.hold-versus-disposal/held-after-disposal에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-036" assertion으로 "link-name-036: /data/hostObservation/extractor/rawRows/namedObservations/36/observationName의 실제 equals 기대값은 'held-after-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-036" assertion으로 "link-oracle-036: /data/hostObservation/extractor/rawRows/namedObservations/36/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-037-scenarios" assertion으로 "T04.hold-versus-disposal/physical-disposal의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-037-api_response" assertion으로 "T04.hold-versus-disposal/physical-disposal에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-037-db_snapshot" assertion으로 "T04.hold-versus-disposal/physical-disposal에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-037" assertion으로 "T04.hold-versus-disposal/physical-disposal에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-037" assertion으로 "link-name-037: /data/hostObservation/extractor/rawRows/namedObservations/37/observationName의 실제 equals 기대값은 'physical-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-037" assertion으로 "link-oracle-037: /data/hostObservation/extractor/rawRows/namedObservations/37/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-038-scenarios" assertion으로 "T04.hold-versus-disposal/decrease-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-038-api_response" assertion으로 "T04.hold-versus-disposal/decrease-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-038-db_snapshot" assertion으로 "T04.hold-versus-disposal/decrease-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-038" assertion으로 "T04.hold-versus-disposal/decrease-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-038" assertion으로 "link-name-038: /data/hostObservation/extractor/rawRows/namedObservations/38/observationName의 실제 equals 기대값은 'decrease-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-038" assertion으로 "link-oracle-038: /data/hostObservation/extractor/rawRows/namedObservations/38/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-039-scenarios" assertion으로 "T04.atomic-ledger/rollback-ledger-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-039-api_response" assertion으로 "T04.atomic-ledger/rollback-ledger-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-039-db_snapshot" assertion으로 "T04.atomic-ledger/rollback-ledger-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-039" assertion으로 "T04.atomic-ledger/rollback-ledger-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-039" assertion으로 "link-name-039: /data/hostObservation/extractor/rawRows/namedObservations/39/observationName의 실제 equals 기대값은 'rollback-ledger-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-039" assertion으로 "link-oracle-039: /data/hostObservation/extractor/rawRows/namedObservations/39/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-040-scenarios" assertion으로 "T04.atomic-ledger/rollback-allocation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-040-api_response" assertion으로 "T04.atomic-ledger/rollback-allocation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-040-db_snapshot" assertion으로 "T04.atomic-ledger/rollback-allocation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-040" assertion으로 "T04.atomic-ledger/rollback-allocation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-040" assertion으로 "link-name-040: /data/hostObservation/extractor/rawRows/namedObservations/40/observationName의 실제 equals 기대값은 'rollback-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-040" assertion으로 "link-oracle-040: /data/hostObservation/extractor/rawRows/namedObservations/40/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-041-scenarios" assertion으로 "T04.atomic-ledger/rollback-outbox-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-041-api_response" assertion으로 "T04.atomic-ledger/rollback-outbox-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-041-db_snapshot" assertion으로 "T04.atomic-ledger/rollback-outbox-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-041" assertion으로 "T04.atomic-ledger/rollback-outbox-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-041" assertion으로 "link-name-041: /data/hostObservation/extractor/rawRows/namedObservations/41/observationName의 실제 equals 기대값은 'rollback-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-041" assertion으로 "link-oracle-041: /data/hostObservation/extractor/rawRows/namedObservations/41/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-042-scenarios" assertion으로 "T04.atomic-ledger/rollback-committed-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-042-api_response" assertion으로 "T04.atomic-ledger/rollback-committed-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-042-db_snapshot" assertion으로 "T04.atomic-ledger/rollback-committed-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-042" assertion으로 "T04.atomic-ledger/rollback-committed-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-042" assertion으로 "link-name-042: /data/hostObservation/extractor/rawRows/namedObservations/42/observationName의 실제 equals 기대값은 'rollback-committed-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-042" assertion으로 "link-oracle-042: /data/hostObservation/extractor/rawRows/namedObservations/42/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-043-scenarios" assertion으로 "T04.atomic-ledger/guarded-primitives의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-043-api_response" assertion으로 "T04.atomic-ledger/guarded-primitives에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-043-db_snapshot" assertion으로 "T04.atomic-ledger/guarded-primitives에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-043" assertion으로 "T04.atomic-ledger/guarded-primitives에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-043" assertion으로 "link-name-043: /data/hostObservation/extractor/rawRows/namedObservations/43/observationName의 실제 equals 기대값은 'guarded-primitives'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-043" assertion으로 "link-oracle-043: /data/hostObservation/extractor/rawRows/namedObservations/43/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-044-scenarios" assertion으로 "T05.ownership-custody-disposition/held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-044-api_response" assertion으로 "T05.ownership-custody-disposition/held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-044-db_snapshot" assertion으로 "T05.ownership-custody-disposition/held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-044" assertion으로 "T05.ownership-custody-disposition/held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-044" assertion으로 "link-name-044: /data/hostObservation/extractor/rawRows/namedObservations/44/observationName의 실제 equals 기대값은 'held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-044" assertion으로 "link-oracle-044: /data/hostObservation/extractor/rawRows/namedObservations/44/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-045-scenarios" assertion으로 "T05.ownership-custody-disposition/sell-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-045-api_response" assertion으로 "T05.ownership-custody-disposition/sell-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-045-db_snapshot" assertion으로 "T05.ownership-custody-disposition/sell-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-045" assertion으로 "T05.ownership-custody-disposition/sell-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-045" assertion으로 "link-name-045: /data/hostObservation/extractor/rawRows/namedObservations/45/observationName의 실제 equals 기대값은 'sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-045" assertion으로 "link-oracle-045: /data/hostObservation/extractor/rawRows/namedObservations/45/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-046-scenarios" assertion으로 "T05.ownership-custody-disposition/unreserved-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-046-api_response" assertion으로 "T05.ownership-custody-disposition/unreserved-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-046-db_snapshot" assertion으로 "T05.ownership-custody-disposition/unreserved-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-046" assertion으로 "T05.ownership-custody-disposition/unreserved-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-046" assertion으로 "link-name-046: /data/hostObservation/extractor/rawRows/namedObservations/46/observationName의 실제 equals 기대값은 'unreserved-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-046" assertion으로 "link-oracle-046: /data/hostObservation/extractor/rawRows/namedObservations/46/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-047-scenarios" assertion으로 "T05.ownership-custody-disposition/relations-independent의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-047-api_response" assertion으로 "T05.ownership-custody-disposition/relations-independent에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-047-db_snapshot" assertion으로 "T05.ownership-custody-disposition/relations-independent에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-047" assertion으로 "T05.ownership-custody-disposition/relations-independent에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-047" assertion으로 "link-name-047: /data/hostObservation/extractor/rawRows/namedObservations/47/observationName의 실제 equals 기대값은 'relations-independent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-047" assertion으로 "link-oracle-047: /data/hostObservation/extractor/rawRows/namedObservations/47/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-048-scenarios" assertion으로 "T05.planned-location/current-location의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-048-api_response" assertion으로 "T05.planned-location/current-location에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-048-db_snapshot" assertion으로 "T05.planned-location/current-location에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-048" assertion으로 "T05.planned-location/current-location에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-048" assertion으로 "link-name-048: /data/hostObservation/extractor/rawRows/namedObservations/48/observationName의 실제 equals 기대값은 'current-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-048" assertion으로 "link-oracle-048: /data/hostObservation/extractor/rawRows/namedObservations/48/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-049-scenarios" assertion으로 "T05.planned-location/planned-destination의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-049-api_response" assertion으로 "T05.planned-location/planned-destination에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-049-db_snapshot" assertion으로 "T05.planned-location/planned-destination에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-049" assertion으로 "T05.planned-location/planned-destination에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-049" assertion으로 "link-name-049: /data/hostObservation/extractor/rawRows/namedObservations/49/observationName의 실제 equals 기대값은 'planned-destination'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-049" assertion으로 "link-oracle-049: /data/hostObservation/extractor/rawRows/namedObservations/49/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-050-scenarios" assertion으로 "T05.planned-location/planned-receipt-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-050-api_response" assertion으로 "T05.planned-location/planned-receipt-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-050-db_snapshot" assertion으로 "T05.planned-location/planned-receipt-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-050" assertion으로 "T05.planned-location/planned-receipt-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-050" assertion으로 "link-name-050: /data/hostObservation/extractor/rawRows/namedObservations/50/observationName의 실제 equals 기대값은 'planned-receipt-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-050" assertion으로 "link-oracle-050: /data/hostObservation/extractor/rawRows/namedObservations/50/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-051-scenarios" assertion으로 "T05.disposition-manager-decision/ordinary-write-confirmed-basis-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-051-api_response" assertion으로 "T05.disposition-manager-decision/ordinary-write-confirmed-basis-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-051-db_snapshot" assertion으로 "T05.disposition-manager-decision/ordinary-write-confirmed-basis-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-051" assertion으로 "T05.disposition-manager-decision/ordinary-write-confirmed-basis-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-051" assertion으로 "link-name-051: /data/hostObservation/extractor/rawRows/namedObservations/51/observationName의 실제 equals 기대값은 'ordinary-write-confirmed-basis-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-051" assertion으로 "link-oracle-051: /data/hostObservation/extractor/rawRows/namedObservations/51/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-052-scenarios" assertion으로 "T05.disposition-manager-decision/eligible-before-manager-confirmation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-052-api_response" assertion으로 "T05.disposition-manager-decision/eligible-before-manager-confirmation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-052-db_snapshot" assertion으로 "T05.disposition-manager-decision/eligible-before-manager-confirmation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-052" assertion으로 "T05.disposition-manager-decision/eligible-before-manager-confirmation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-052" assertion으로 "link-name-052: /data/hostObservation/extractor/rawRows/namedObservations/52/observationName의 실제 equals 기대값은 'eligible-before-manager-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-052" assertion으로 "link-oracle-052: /data/hostObservation/extractor/rawRows/namedObservations/52/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-053-scenarios" assertion으로 "T05.disposition-manager-decision/proposal-evidence-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-053-api_response" assertion으로 "T05.disposition-manager-decision/proposal-evidence-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-053-db_snapshot" assertion으로 "T05.disposition-manager-decision/proposal-evidence-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-053" assertion으로 "T05.disposition-manager-decision/proposal-evidence-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-053" assertion으로 "link-name-053: /data/hostObservation/extractor/rawRows/namedObservations/53/observationName의 실제 equals 기대값은 'proposal-evidence-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-053" assertion으로 "link-oracle-053: /data/hostObservation/extractor/rawRows/namedObservations/53/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-054-scenarios" assertion으로 "T05.disposition-manager-decision/authorized-confirmation-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-054-api_response" assertion으로 "T05.disposition-manager-decision/authorized-confirmation-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-054-db_snapshot" assertion으로 "T05.disposition-manager-decision/authorized-confirmation-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-054" assertion으로 "T05.disposition-manager-decision/authorized-confirmation-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-054" assertion으로 "link-name-054: /data/hostObservation/extractor/rawRows/namedObservations/54/observationName의 실제 equals 기대값은 'authorized-confirmation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-054" assertion으로 "link-oracle-054: /data/hostObservation/extractor/rawRows/namedObservations/54/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-055-scenarios" assertion으로 "T05.disposition-manager-decision/eligible-after-manager-confirmation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-055-api_response" assertion으로 "T05.disposition-manager-decision/eligible-after-manager-confirmation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-055-db_snapshot" assertion으로 "T05.disposition-manager-decision/eligible-after-manager-confirmation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-055" assertion으로 "T05.disposition-manager-decision/eligible-after-manager-confirmation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-055" assertion으로 "link-name-055: /data/hostObservation/extractor/rawRows/namedObservations/55/observationName의 실제 equals 기대값은 'eligible-after-manager-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-055" assertion으로 "link-oracle-055: /data/hostObservation/extractor/rawRows/namedObservations/55/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-056-scenarios" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-reservation-after-confirmation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-056-api_response" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-reservation-after-confirmation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-056-db_snapshot" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-reservation-after-confirmation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-056" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-reservation-after-confirmation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-056" assertion으로 "link-name-056: /data/hostObservation/extractor/rawRows/namedObservations/56/observationName의 실제 equals 기대값은 'ordinary-authorized-reservation-after-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-056" assertion으로 "link-oracle-056: /data/hostObservation/extractor/rawRows/namedObservations/56/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-057-scenarios" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-dispatch-after-confirmation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-057-api_response" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-dispatch-after-confirmation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-057-db_snapshot" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-dispatch-after-confirmation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-057" assertion으로 "T05.disposition-manager-decision/ordinary-authorized-dispatch-after-confirmation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-057" assertion으로 "link-name-057: /data/hostObservation/extractor/rawRows/namedObservations/57/observationName의 실제 equals 기대값은 'ordinary-authorized-dispatch-after-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-057" assertion으로 "link-oracle-057: /data/hostObservation/extractor/rawRows/namedObservations/57/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-058-scenarios" assertion으로 "T05.disposition-manager-decision/new-human-approval-added-to-reservation-or-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-058-api_response" assertion으로 "T05.disposition-manager-decision/new-human-approval-added-to-reservation-or-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-058-db_snapshot" assertion으로 "T05.disposition-manager-decision/new-human-approval-added-to-reservation-or-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-058" assertion으로 "T05.disposition-manager-decision/new-human-approval-added-to-reservation-or-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-058" assertion으로 "link-name-058: /data/hostObservation/extractor/rawRows/namedObservations/58/observationName의 실제 equals 기대값은 'new-human-approval-added-to-reservation-or-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-058" assertion으로 "link-oracle-058: /data/hostObservation/extractor/rawRows/namedObservations/58/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-059-scenarios" assertion으로 "T05.disposition-manager-decision/manager-decision-binding의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-059-api_response" assertion으로 "T05.disposition-manager-decision/manager-decision-binding에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-059-db_snapshot" assertion으로 "T05.disposition-manager-decision/manager-decision-binding에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-059" assertion으로 "T05.disposition-manager-decision/manager-decision-binding에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-059" assertion으로 "link-name-059: /data/hostObservation/extractor/rawRows/namedObservations/59/observationName의 실제 equals 기대값은 'manager-decision-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-059" assertion으로 "link-oracle-059: /data/hostObservation/extractor/rawRows/namedObservations/59/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-060-scenarios" assertion으로 "T06.bitemporal-correction/then-known의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-060-api_response" assertion으로 "T06.bitemporal-correction/then-known에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-060-db_snapshot" assertion으로 "T06.bitemporal-correction/then-known에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-060" assertion으로 "T06.bitemporal-correction/then-known에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-060" assertion으로 "link-name-060: /data/hostObservation/extractor/rawRows/namedObservations/60/observationName의 실제 equals 기대값은 'then-known'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-060" assertion으로 "link-oracle-060: /data/hostObservation/extractor/rawRows/namedObservations/60/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-061-scenarios" assertion으로 "T06.bitemporal-correction/currently-known의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-061-api_response" assertion으로 "T06.bitemporal-correction/currently-known에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-061-db_snapshot" assertion으로 "T06.bitemporal-correction/currently-known에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-061" assertion으로 "T06.bitemporal-correction/currently-known에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-061" assertion으로 "link-name-061: /data/hostObservation/extractor/rawRows/namedObservations/61/observationName의 실제 equals 기대값은 'currently-known'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-061" assertion으로 "link-oracle-061: /data/hostObservation/extractor/rawRows/namedObservations/61/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-062-scenarios" assertion으로 "T06.bitemporal-correction/time-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-062-api_response" assertion으로 "T06.bitemporal-correction/time-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-062-db_snapshot" assertion으로 "T06.bitemporal-correction/time-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-062" assertion으로 "T06.bitemporal-correction/time-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-062" assertion으로 "link-name-062: /data/hostObservation/extractor/rawRows/namedObservations/62/observationName의 실제 equals 기대값은 'time-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-062" assertion으로 "link-oracle-062: /data/hostObservation/extractor/rawRows/namedObservations/62/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-063-scenarios" assertion으로 "T06.bitemporal-correction/correction-not-fake-movement의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-063-api_response" assertion으로 "T06.bitemporal-correction/correction-not-fake-movement에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-063-db_snapshot" assertion으로 "T06.bitemporal-correction/correction-not-fake-movement에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-063" assertion으로 "T06.bitemporal-correction/correction-not-fake-movement에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-063" assertion으로 "link-name-063: /data/hostObservation/extractor/rawRows/namedObservations/63/observationName의 실제 equals 기대값은 'correction-not-fake-movement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-063" assertion으로 "link-oracle-063: /data/hostObservation/extractor/rawRows/namedObservations/63/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-064-contracts" assertion으로 "T06.unknown-state-distinction/state-values의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-064-scenarios" assertion으로 "T06.unknown-state-distinction/state-values의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-064-api_response" assertion으로 "T06.unknown-state-distinction/state-values에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-064-db_snapshot" assertion으로 "T06.unknown-state-distinction/state-values에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-064" assertion으로 "T06.unknown-state-distinction/state-values에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-064" assertion으로 "link-name-064: /data/hostObservation/extractor/rawRows/namedObservations/64/observationName의 실제 equals 기대값은 'state-values'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-064" assertion으로 "link-oracle-064: /data/hostObservation/extractor/rawRows/namedObservations/64/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-065-contracts" assertion으로 "T06.unknown-state-distinction/zero-inference의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-065-scenarios" assertion으로 "T06.unknown-state-distinction/zero-inference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-065-api_response" assertion으로 "T06.unknown-state-distinction/zero-inference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-065-db_snapshot" assertion으로 "T06.unknown-state-distinction/zero-inference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-065" assertion으로 "T06.unknown-state-distinction/zero-inference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-065" assertion으로 "link-name-065: /data/hostObservation/extractor/rawRows/namedObservations/65/observationName의 실제 equals 기대값은 'zero-inference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-065" assertion으로 "link-oracle-065: /data/hostObservation/extractor/rawRows/namedObservations/65/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-066-contracts" assertion으로 "T06.unknown-state-distinction/conflict-preserved의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-066-scenarios" assertion으로 "T06.unknown-state-distinction/conflict-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-066-api_response" assertion으로 "T06.unknown-state-distinction/conflict-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-066-db_snapshot" assertion으로 "T06.unknown-state-distinction/conflict-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-066" assertion으로 "T06.unknown-state-distinction/conflict-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-066" assertion으로 "link-name-066: /data/hostObservation/extractor/rawRows/namedObservations/66/observationName의 실제 equals 기대값은 'conflict-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-066" assertion으로 "link-oracle-066: /data/hostObservation/extractor/rawRows/namedObservations/66/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-067-contracts" assertion으로 "T07.typed-relations-and-predicates/invalid-types의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-067-scenarios" assertion으로 "T07.typed-relations-and-predicates/invalid-types의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-067-api_response" assertion으로 "T07.typed-relations-and-predicates/invalid-types에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-067-db_snapshot" assertion으로 "T07.typed-relations-and-predicates/invalid-types에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-067" assertion으로 "T07.typed-relations-and-predicates/invalid-types에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-067" assertion으로 "link-name-067: /data/hostObservation/extractor/rawRows/namedObservations/67/observationName의 실제 equals 기대값은 'invalid-types'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-067" assertion으로 "link-oracle-067: /data/hostObservation/extractor/rawRows/namedObservations/67/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-068-contracts" assertion으로 "T07.typed-relations-and-predicates/invalid-effects의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-068-scenarios" assertion으로 "T07.typed-relations-and-predicates/invalid-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-068-api_response" assertion으로 "T07.typed-relations-and-predicates/invalid-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-068-db_snapshot" assertion으로 "T07.typed-relations-and-predicates/invalid-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-068" assertion으로 "T07.typed-relations-and-predicates/invalid-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-068" assertion으로 "link-name-068: /data/hostObservation/extractor/rawRows/namedObservations/68/observationName의 실제 equals 기대값은 'invalid-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-068" assertion으로 "link-oracle-068: /data/hostObservation/extractor/rawRows/namedObservations/68/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-069-contracts" assertion으로 "T07.typed-relations-and-predicates/predicate-three-valued의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-069-scenarios" assertion으로 "T07.typed-relations-and-predicates/predicate-three-valued의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-069-api_response" assertion으로 "T07.typed-relations-and-predicates/predicate-three-valued에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-069-db_snapshot" assertion으로 "T07.typed-relations-and-predicates/predicate-three-valued에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-069" assertion으로 "T07.typed-relations-and-predicates/predicate-three-valued에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-069" assertion으로 "link-name-069: /data/hostObservation/extractor/rawRows/namedObservations/69/observationName의 실제 equals 기대값은 'predicate-three-valued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-069" assertion으로 "link-oracle-069: /data/hostObservation/extractor/rawRows/namedObservations/69/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-070-contracts" assertion으로 "T07.typed-relations-and-predicates/relation-created-work의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-070-scenarios" assertion으로 "T07.typed-relations-and-predicates/relation-created-work의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-070-api_response" assertion으로 "T07.typed-relations-and-predicates/relation-created-work에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-070-db_snapshot" assertion으로 "T07.typed-relations-and-predicates/relation-created-work에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-070" assertion으로 "T07.typed-relations-and-predicates/relation-created-work에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-070" assertion으로 "link-name-070: /data/hostObservation/extractor/rawRows/namedObservations/70/observationName의 실제 equals 기대값은 'relation-created-work'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-070" assertion으로 "link-oracle-070: /data/hostObservation/extractor/rawRows/namedObservations/70/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-071-contracts" assertion으로 "T07.typed-relations-and-predicates/relation-created-run의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-071-scenarios" assertion으로 "T07.typed-relations-and-predicates/relation-created-run의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-071-api_response" assertion으로 "T07.typed-relations-and-predicates/relation-created-run에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-071-db_snapshot" assertion으로 "T07.typed-relations-and-predicates/relation-created-run에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-071" assertion으로 "T07.typed-relations-and-predicates/relation-created-run에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-071" assertion으로 "link-name-071: /data/hostObservation/extractor/rawRows/namedObservations/71/observationName의 실제 equals 기대값은 'relation-created-run'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-071" assertion으로 "link-oracle-071: /data/hostObservation/extractor/rawRows/namedObservations/71/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-072-scenarios" assertion으로 "T07.two-documents-one-occurrence/receipt-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-072-api_response" assertion으로 "T07.two-documents-one-occurrence/receipt-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-072-db_snapshot" assertion으로 "T07.two-documents-one-occurrence/receipt-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-072" assertion으로 "T07.two-documents-one-occurrence/receipt-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-072" assertion으로 "link-name-072: /data/hostObservation/extractor/rawRows/namedObservations/72/observationName의 실제 equals 기대값은 'receipt-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-072" assertion으로 "link-oracle-072: /data/hostObservation/extractor/rawRows/namedObservations/72/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-073-scenarios" assertion으로 "T07.two-documents-one-occurrence/document-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-073-api_response" assertion으로 "T07.two-documents-one-occurrence/document-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-073-db_snapshot" assertion으로 "T07.two-documents-one-occurrence/document-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-073" assertion으로 "T07.two-documents-one-occurrence/document-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-073" assertion으로 "link-name-073: /data/hostObservation/extractor/rawRows/namedObservations/73/observationName의 실제 equals 기대값은 'document-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-073" assertion으로 "link-oracle-073: /data/hostObservation/extractor/rawRows/namedObservations/73/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-074-scenarios" assertion으로 "T07.two-documents-one-occurrence/canonical-occurrence-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-074-api_response" assertion으로 "T07.two-documents-one-occurrence/canonical-occurrence-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-074-db_snapshot" assertion으로 "T07.two-documents-one-occurrence/canonical-occurrence-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-074" assertion으로 "T07.two-documents-one-occurrence/canonical-occurrence-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-074" assertion으로 "link-name-074: /data/hostObservation/extractor/rawRows/namedObservations/74/observationName의 실제 equals 기대값은 'canonical-occurrence-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-074" assertion으로 "link-oracle-074: /data/hostObservation/extractor/rawRows/namedObservations/74/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-075-scenarios" assertion으로 "T07.two-documents-one-occurrence/duplicate-physical-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-075-api_response" assertion으로 "T07.two-documents-one-occurrence/duplicate-physical-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-075-db_snapshot" assertion으로 "T07.two-documents-one-occurrence/duplicate-physical-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-075" assertion으로 "T07.two-documents-one-occurrence/duplicate-physical-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-075" assertion으로 "link-name-075: /data/hostObservation/extractor/rawRows/namedObservations/75/observationName의 실제 equals 기대값은 'duplicate-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-075" assertion으로 "link-oracle-075: /data/hostObservation/extractor/rawRows/namedObservations/75/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-076-scenarios" assertion으로 "T07.document-availability/unavailable-evidence-contribution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-076-api_response" assertion으로 "T07.document-availability/unavailable-evidence-contribution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-076-db_snapshot" assertion으로 "T07.document-availability/unavailable-evidence-contribution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-076" assertion으로 "T07.document-availability/unavailable-evidence-contribution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-076" assertion으로 "link-name-076: /data/hostObservation/extractor/rawRows/namedObservations/76/observationName의 실제 equals 기대값은 'unavailable-evidence-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-076" assertion으로 "link-oracle-076: /data/hostObservation/extractor/rawRows/namedObservations/76/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-077-scenarios" assertion으로 "T07.document-availability/unreadable-source의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-077-api_response" assertion으로 "T07.document-availability/unreadable-source에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-077-db_snapshot" assertion으로 "T07.document-availability/unreadable-source에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-077" assertion으로 "T07.document-availability/unreadable-source에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-077" assertion으로 "link-name-077: /data/hostObservation/extractor/rawRows/namedObservations/77/observationName의 실제 equals 기대값은 'unreadable-source'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-077" assertion으로 "link-oracle-077: /data/hostObservation/extractor/rawRows/namedObservations/77/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-078-scenarios" assertion으로 "T07.document-availability/blob-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-078-api_response" assertion으로 "T07.document-availability/blob-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-078-db_snapshot" assertion으로 "T07.document-availability/blob-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-078" assertion으로 "T07.document-availability/blob-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-078" assertion으로 "link-name-078: /data/hostObservation/extractor/rawRows/namedObservations/78/observationName의 실제 equals 기대값은 'blob-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-078" assertion으로 "link-oracle-078: /data/hostObservation/extractor/rawRows/namedObservations/78/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-079-scenarios" assertion으로 "T08.organization-boundaries/cross-org-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-079-api_response" assertion으로 "T08.organization-boundaries/cross-org-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-079-db_snapshot" assertion으로 "T08.organization-boundaries/cross-org-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-079" assertion으로 "T08.organization-boundaries/cross-org-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-079" assertion으로 "link-name-079: /data/hostObservation/extractor/rawRows/namedObservations/79/observationName의 실제 equals 기대값은 'cross-org-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-079" assertion으로 "link-oracle-079: /data/hostObservation/extractor/rawRows/namedObservations/79/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-080-scenarios" assertion으로 "T08.organization-boundaries/cross-org-identity-leak의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-080-api_response" assertion으로 "T08.organization-boundaries/cross-org-identity-leak에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-080-db_snapshot" assertion으로 "T08.organization-boundaries/cross-org-identity-leak에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-080" assertion으로 "T08.organization-boundaries/cross-org-identity-leak에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-080" assertion으로 "link-name-080: /data/hostObservation/extractor/rawRows/namedObservations/80/observationName의 실제 equals 기대값은 'cross-org-identity-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-080" assertion으로 "link-oracle-080: /data/hostObservation/extractor/rawRows/namedObservations/80/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-081-scenarios" assertion으로 "T08.organization-boundaries/invalid-auth의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-081-api_response" assertion으로 "T08.organization-boundaries/invalid-auth에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-081-db_snapshot" assertion으로 "T08.organization-boundaries/invalid-auth에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-081" assertion으로 "T08.organization-boundaries/invalid-auth에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-081" assertion으로 "link-name-081: /data/hostObservation/extractor/rawRows/namedObservations/81/observationName의 실제 equals 기대값은 'invalid-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-081" assertion으로 "link-oracle-081: /data/hostObservation/extractor/rawRows/namedObservations/81/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-082-scenarios" assertion으로 "T08.organization-boundaries/fk-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-082-api_response" assertion으로 "T08.organization-boundaries/fk-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-082-db_snapshot" assertion으로 "T08.organization-boundaries/fk-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-082" assertion으로 "T08.organization-boundaries/fk-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-082" assertion으로 "link-name-082: /data/hostObservation/extractor/rawRows/namedObservations/82/observationName의 실제 equals 기대값은 'fk-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-082" assertion으로 "link-oracle-082: /data/hostObservation/extractor/rawRows/namedObservations/82/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-083-scenarios" assertion으로 "T08.control-envelope/role-or-payload-escalation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-083-api_response" assertion으로 "T08.control-envelope/role-or-payload-escalation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-083-db_snapshot" assertion으로 "T08.control-envelope/role-or-payload-escalation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-083" assertion으로 "T08.control-envelope/role-or-payload-escalation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-083" assertion으로 "link-name-083: /data/hostObservation/extractor/rawRows/namedObservations/83/observationName의 실제 equals 기대값은 'role-or-payload-escalation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-083" assertion으로 "link-oracle-083: /data/hostObservation/extractor/rawRows/namedObservations/83/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-084-scenarios" assertion으로 "T08.control-envelope/grant-exceeding-write의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-084-api_response" assertion으로 "T08.control-envelope/grant-exceeding-write에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-084-db_snapshot" assertion으로 "T08.control-envelope/grant-exceeding-write에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-084" assertion으로 "T08.control-envelope/grant-exceeding-write에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-084" assertion으로 "link-name-084: /data/hostObservation/extractor/rawRows/namedObservations/84/observationName의 실제 equals 기대값은 'grant-exceeding-write'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-084" assertion으로 "link-oracle-084: /data/hostObservation/extractor/rawRows/namedObservations/84/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-085-scenarios" assertion으로 "T08.control-envelope/server-authority의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-085-api_response" assertion으로 "T08.control-envelope/server-authority에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-085-db_snapshot" assertion으로 "T08.control-envelope/server-authority에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-085" assertion으로 "T08.control-envelope/server-authority에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-085" assertion으로 "link-name-085: /data/hostObservation/extractor/rawRows/namedObservations/85/observationName의 실제 equals 기대값은 'server-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-085" assertion으로 "link-oracle-085: /data/hostObservation/extractor/rawRows/namedObservations/85/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-086-scenarios" assertion으로 "T08.grant-control-plane/self-amplification의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-086-api_response" assertion으로 "T08.grant-control-plane/self-amplification에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-086-db_snapshot" assertion으로 "T08.grant-control-plane/self-amplification에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-086" assertion으로 "T08.grant-control-plane/self-amplification에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-086" assertion으로 "link-name-086: /data/hostObservation/extractor/rawRows/namedObservations/86/observationName의 실제 equals 기대값은 'self-amplification'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-086" assertion으로 "link-oracle-086: /data/hostObservation/extractor/rawRows/namedObservations/86/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-087-scenarios" assertion으로 "T08.grant-control-plane/out-of-scope-assignment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-087-api_response" assertion으로 "T08.grant-control-plane/out-of-scope-assignment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-087-db_snapshot" assertion으로 "T08.grant-control-plane/out-of-scope-assignment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-087" assertion으로 "T08.grant-control-plane/out-of-scope-assignment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-087" assertion으로 "link-name-087: /data/hostObservation/extractor/rawRows/namedObservations/87/observationName의 실제 equals 기대값은 'out-of-scope-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-087" assertion으로 "link-oracle-087: /data/hostObservation/extractor/rawRows/namedObservations/87/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-088-scenarios" assertion으로 "T08.grant-control-plane/grant-revocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-088-api_response" assertion으로 "T08.grant-control-plane/grant-revocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-088-db_snapshot" assertion으로 "T08.grant-control-plane/grant-revocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-088" assertion으로 "T08.grant-control-plane/grant-revocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-088" assertion으로 "link-name-088: /data/hostObservation/extractor/rawRows/namedObservations/88/observationName의 실제 equals 기대값은 'grant-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-088" assertion으로 "link-oracle-088: /data/hostObservation/extractor/rawRows/namedObservations/88/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-089-scenarios" assertion으로 "T08.grant-control-plane/identity-admin-only의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-089-api_response" assertion으로 "T08.grant-control-plane/identity-admin-only에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-089-db_snapshot" assertion으로 "T08.grant-control-plane/identity-admin-only에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-089" assertion으로 "T08.grant-control-plane/identity-admin-only에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-089" assertion으로 "link-name-089: /data/hostObservation/extractor/rawRows/namedObservations/89/observationName의 실제 equals 기대값은 'identity-admin-only'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-089" assertion으로 "link-oracle-089: /data/hostObservation/extractor/rawRows/namedObservations/89/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-090-scenarios" assertion으로 "T09.quantity-modes/cumulative-arrived의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-090-api_response" assertion으로 "T09.quantity-modes/cumulative-arrived에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-090-db_snapshot" assertion으로 "T09.quantity-modes/cumulative-arrived에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-090" assertion으로 "T09.quantity-modes/cumulative-arrived에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-090" assertion으로 "link-name-090: /data/hostObservation/extractor/rawRows/namedObservations/90/observationName의 실제 equals 기대값은 'cumulative-arrived'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-090" assertion으로 "link-oracle-090: /data/hostObservation/extractor/rawRows/namedObservations/90/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-091-scenarios" assertion으로 "T09.quantity-modes/state-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-091-api_response" assertion으로 "T09.quantity-modes/state-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-091-db_snapshot" assertion으로 "T09.quantity-modes/state-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-091" assertion으로 "T09.quantity-modes/state-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-091" assertion으로 "link-name-091: /data/hostObservation/extractor/rawRows/namedObservations/91/observationName의 실제 equals 기대값은 'state-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-091" assertion으로 "link-oracle-091: /data/hostObservation/extractor/rawRows/namedObservations/91/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-092-scenarios" assertion으로 "T09.quantity-modes/cumulative-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-092-api_response" assertion으로 "T09.quantity-modes/cumulative-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-092-db_snapshot" assertion으로 "T09.quantity-modes/cumulative-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-092" assertion으로 "T09.quantity-modes/cumulative-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-092" assertion으로 "link-name-092: /data/hostObservation/extractor/rawRows/namedObservations/92/observationName의 실제 equals 기대값은 'cumulative-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-092" assertion으로 "link-oracle-092: /data/hostObservation/extractor/rawRows/namedObservations/92/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-093-scenarios" assertion으로 "T09.quantity-modes/state-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-093-api_response" assertion으로 "T09.quantity-modes/state-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-093-db_snapshot" assertion으로 "T09.quantity-modes/state-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-093" assertion으로 "T09.quantity-modes/state-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-093" assertion으로 "link-name-093: /data/hostObservation/extractor/rawRows/namedObservations/93/observationName의 실제 equals 기대값은 'state-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-093" assertion으로 "link-oracle-093: /data/hostObservation/extractor/rawRows/namedObservations/93/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-094-scenarios" assertion으로 "T09.quantity-modes/goal-mode-fields의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-094-api_response" assertion으로 "T09.quantity-modes/goal-mode-fields에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-094-db_snapshot" assertion으로 "T09.quantity-modes/goal-mode-fields에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-094" assertion으로 "T09.quantity-modes/goal-mode-fields에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-094" assertion으로 "link-name-094: /data/hostObservation/extractor/rawRows/namedObservations/94/observationName의 실제 equals 기대값은 'goal-mode-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-094" assertion으로 "link-oracle-094: /data/hostObservation/extractor/rawRows/namedObservations/94/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-095-scenarios" assertion으로 "T09.stage-required-lot/stage-requirements의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-095-api_response" assertion으로 "T09.stage-required-lot/stage-requirements에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-095-db_snapshot" assertion으로 "T09.stage-required-lot/stage-requirements에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-095" assertion으로 "T09.stage-required-lot/stage-requirements에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-095" assertion으로 "link-name-095: /data/hostObservation/extractor/rawRows/namedObservations/95/observationName의 실제 equals 기대값은 'stage-requirements'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-095" assertion으로 "link-oracle-095: /data/hostObservation/extractor/rawRows/namedObservations/95/oracleId의 실제 equals 기대값은 'T09.stage-required-lot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-096-scenarios" assertion으로 "T09.stage-required-lot/draft-physical-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-096-api_response" assertion으로 "T09.stage-required-lot/draft-physical-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-096-db_snapshot" assertion으로 "T09.stage-required-lot/draft-physical-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-096" assertion으로 "T09.stage-required-lot/draft-physical-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-096" assertion으로 "link-name-096: /data/hostObservation/extractor/rawRows/namedObservations/96/observationName의 실제 equals 기대값은 'draft-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-096" assertion으로 "link-oracle-096: /data/hostObservation/extractor/rawRows/namedObservations/96/oracleId의 실제 equals 기대값은 'T09.stage-required-lot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-097-contracts" assertion으로 "T09.throughout-observation-gap/gap-result의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-097-scenarios" assertion으로 "T09.throughout-observation-gap/gap-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-097-api_response" assertion으로 "T09.throughout-observation-gap/gap-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-097-db_snapshot" assertion으로 "T09.throughout-observation-gap/gap-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-097" assertion으로 "T09.throughout-observation-gap/gap-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-097" assertion으로 "link-name-097: /data/hostObservation/extractor/rawRows/namedObservations/97/observationName의 실제 equals 기대값은 'gap-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-097" assertion으로 "link-oracle-097: /data/hostObservation/extractor/rawRows/namedObservations/97/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-098-contracts" assertion으로 "T09.throughout-observation-gap/inferred-continuity의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-098-scenarios" assertion으로 "T09.throughout-observation-gap/inferred-continuity의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-098-api_response" assertion으로 "T09.throughout-observation-gap/inferred-continuity에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-098-db_snapshot" assertion으로 "T09.throughout-observation-gap/inferred-continuity에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-098" assertion으로 "T09.throughout-observation-gap/inferred-continuity에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-098" assertion으로 "link-name-098: /data/hostObservation/extractor/rawRows/namedObservations/98/observationName의 실제 equals 기대값은 'inferred-continuity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-098" assertion으로 "link-oracle-098: /data/hostObservation/extractor/rawRows/namedObservations/98/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-099-contracts" assertion으로 "T09.throughout-observation-gap/interval-policy의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-099-scenarios" assertion으로 "T09.throughout-observation-gap/interval-policy의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-099-api_response" assertion으로 "T09.throughout-observation-gap/interval-policy에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-099-db_snapshot" assertion으로 "T09.throughout-observation-gap/interval-policy에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-099" assertion으로 "T09.throughout-observation-gap/interval-policy에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-099" assertion으로 "link-name-099: /data/hostObservation/extractor/rawRows/namedObservations/99/observationName의 실제 equals 기대값은 'interval-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-099" assertion으로 "link-oracle-099: /data/hostObservation/extractor/rawRows/namedObservations/99/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-100-contracts" assertion으로 "T09.exists-in-versus-end-state/initial-held의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-100-scenarios" assertion으로 "T09.exists-in-versus-end-state/initial-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-100-api_response" assertion으로 "T09.exists-in-versus-end-state/initial-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-100-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/initial-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-100" assertion으로 "T09.exists-in-versus-end-state/initial-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-100" assertion으로 "link-name-100: /data/hostObservation/extractor/rawRows/namedObservations/100/observationName의 실제 equals 기대값은 'initial-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-100" assertion으로 "link-oracle-100: /data/hostObservation/extractor/rawRows/namedObservations/100/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-101-contracts" assertion으로 "T09.exists-in-versus-end-state/final-held의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-101-scenarios" assertion으로 "T09.exists-in-versus-end-state/final-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-101-api_response" assertion으로 "T09.exists-in-versus-end-state/final-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-101-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/final-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-101" assertion으로 "T09.exists-in-versus-end-state/final-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-101" assertion으로 "link-name-101: /data/hostObservation/extractor/rawRows/namedObservations/101/observationName의 실제 equals 기대값은 'final-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-101" assertion으로 "link-oracle-101: /data/hostObservation/extractor/rawRows/namedObservations/101/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-102-contracts" assertion으로 "T09.exists-in-versus-end-state/exists-in-result의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-102-scenarios" assertion으로 "T09.exists-in-versus-end-state/exists-in-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-102-api_response" assertion으로 "T09.exists-in-versus-end-state/exists-in-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-102-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/exists-in-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-102" assertion으로 "T09.exists-in-versus-end-state/exists-in-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-102" assertion으로 "link-name-102: /data/hostObservation/extractor/rawRows/namedObservations/102/observationName의 실제 equals 기대값은 'exists-in-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-102" assertion으로 "link-oracle-102: /data/hostObservation/extractor/rawRows/namedObservations/102/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-103-contracts" assertion으로 "T09.exists-in-versus-end-state/end-state-at-result의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-103-scenarios" assertion으로 "T09.exists-in-versus-end-state/end-state-at-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-103-api_response" assertion으로 "T09.exists-in-versus-end-state/end-state-at-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-103-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/end-state-at-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-103" assertion으로 "T09.exists-in-versus-end-state/end-state-at-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-103" assertion으로 "link-name-103: /data/hostObservation/extractor/rawRows/namedObservations/103/observationName의 실제 equals 기대값은 'end-state-at-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-103" assertion으로 "link-oracle-103: /data/hostObservation/extractor/rawRows/namedObservations/103/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-104-contracts" assertion으로 "T09.exists-in-versus-end-state/throughout-result의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-104-scenarios" assertion으로 "T09.exists-in-versus-end-state/throughout-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-104-api_response" assertion으로 "T09.exists-in-versus-end-state/throughout-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-104-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/throughout-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-104" assertion으로 "T09.exists-in-versus-end-state/throughout-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-104" assertion으로 "link-name-104: /data/hostObservation/extractor/rawRows/namedObservations/104/observationName의 실제 equals 기대값은 'throughout-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-104" assertion으로 "link-oracle-104: /data/hostObservation/extractor/rawRows/namedObservations/104/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-105-contracts" assertion으로 "T09.exists-in-versus-end-state/same-trajectory-three-goals의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-105-scenarios" assertion으로 "T09.exists-in-versus-end-state/same-trajectory-three-goals의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-105-api_response" assertion으로 "T09.exists-in-versus-end-state/same-trajectory-three-goals에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-105-db_snapshot" assertion으로 "T09.exists-in-versus-end-state/same-trajectory-three-goals에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-105" assertion으로 "T09.exists-in-versus-end-state/same-trajectory-three-goals에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-105" assertion으로 "link-name-105: /data/hostObservation/extractor/rawRows/namedObservations/105/observationName의 실제 equals 기대값은 'same-trajectory-three-goals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-105" assertion으로 "link-oracle-105: /data/hostObservation/extractor/rawRows/namedObservations/105/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-106-contracts" assertion으로 "T09.throughout-fully-observed/initial-held의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-106-scenarios" assertion으로 "T09.throughout-fully-observed/initial-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-106-api_response" assertion으로 "T09.throughout-fully-observed/initial-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-106-db_snapshot" assertion으로 "T09.throughout-fully-observed/initial-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-106" assertion으로 "T09.throughout-fully-observed/initial-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-106" assertion으로 "link-name-106: /data/hostObservation/extractor/rawRows/namedObservations/106/observationName의 실제 equals 기대값은 'initial-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-106" assertion으로 "link-oracle-106: /data/hostObservation/extractor/rawRows/namedObservations/106/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-107-contracts" assertion으로 "T09.throughout-fully-observed/final-held의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-107-scenarios" assertion으로 "T09.throughout-fully-observed/final-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-107-api_response" assertion으로 "T09.throughout-fully-observed/final-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-107-db_snapshot" assertion으로 "T09.throughout-fully-observed/final-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-107" assertion으로 "T09.throughout-fully-observed/final-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-107" assertion으로 "link-name-107: /data/hostObservation/extractor/rawRows/namedObservations/107/observationName의 실제 equals 기대값은 'final-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-107" assertion으로 "link-oracle-107: /data/hostObservation/extractor/rawRows/namedObservations/107/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-108-contracts" assertion으로 "T09.throughout-fully-observed/throughout-result의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-108-scenarios" assertion으로 "T09.throughout-fully-observed/throughout-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-108-api_response" assertion으로 "T09.throughout-fully-observed/throughout-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-108-db_snapshot" assertion으로 "T09.throughout-fully-observed/throughout-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-108" assertion으로 "T09.throughout-fully-observed/throughout-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-108" assertion으로 "link-name-108: /data/hostObservation/extractor/rawRows/namedObservations/108/observationName의 실제 equals 기대값은 'throughout-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-108" assertion으로 "link-oracle-108: /data/hostObservation/extractor/rawRows/namedObservations/108/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-109-contracts" assertion으로 "T09.throughout-fully-observed/sufficient-continuity-evidence의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-109-scenarios" assertion으로 "T09.throughout-fully-observed/sufficient-continuity-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-109-api_response" assertion으로 "T09.throughout-fully-observed/sufficient-continuity-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-109-db_snapshot" assertion으로 "T09.throughout-fully-observed/sufficient-continuity-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-109" assertion으로 "T09.throughout-fully-observed/sufficient-continuity-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-109" assertion으로 "link-name-109: /data/hostObservation/extractor/rawRows/namedObservations/109/observationName의 실제 equals 기대값은 'sufficient-continuity-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-109" assertion으로 "link-oracle-109: /data/hostObservation/extractor/rawRows/namedObservations/109/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-110-scenarios" assertion으로 "T10.handover-acceptance/owner-by-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-110-api_response" assertion으로 "T10.handover-acceptance/owner-by-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-110-db_snapshot" assertion으로 "T10.handover-acceptance/owner-by-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-110" assertion으로 "T10.handover-acceptance/owner-by-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-110" assertion으로 "link-name-110: /data/hostObservation/extractor/rawRows/namedObservations/110/observationName의 실제 equals 기대값은 'owner-by-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-110" assertion으로 "link-oracle-110: /data/hostObservation/extractor/rawRows/namedObservations/110/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-111-scenarios" assertion으로 "T10.handover-acceptance/notification-as-acceptance의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-111-api_response" assertion으로 "T10.handover-acceptance/notification-as-acceptance에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-111-db_snapshot" assertion으로 "T10.handover-acceptance/notification-as-acceptance에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-111" assertion으로 "T10.handover-acceptance/notification-as-acceptance에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-111" assertion으로 "link-name-111: /data/hostObservation/extractor/rawRows/namedObservations/111/observationName의 실제 equals 기대값은 'notification-as-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-111" assertion으로 "link-oracle-111: /data/hostObservation/extractor/rawRows/namedObservations/111/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-112-scenarios" assertion으로 "T10.handover-acceptance/active-human-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-112-api_response" assertion으로 "T10.handover-acceptance/active-human-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-112-db_snapshot" assertion으로 "T10.handover-acceptance/active-human-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-112" assertion으로 "T10.handover-acceptance/active-human-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-112" assertion으로 "link-name-112: /data/hostObservation/extractor/rawRows/namedObservations/112/observationName의 실제 equals 기대값은 'active-human-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-112" assertion으로 "link-oracle-112: /data/hostObservation/extractor/rawRows/namedObservations/112/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-113-scenarios" assertion으로 "T10.obligation-transfer-failure/source-assignment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-113-api_response" assertion으로 "T10.obligation-transfer-failure/source-assignment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-113-db_snapshot" assertion으로 "T10.obligation-transfer-failure/source-assignment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-113" assertion으로 "T10.obligation-transfer-failure/source-assignment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-113" assertion으로 "link-name-113: /data/hostObservation/extractor/rawRows/namedObservations/113/observationName의 실제 equals 기대값은 'source-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-113" assertion으로 "link-oracle-113: /data/hostObservation/extractor/rawRows/namedObservations/113/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-114-scenarios" assertion으로 "T10.obligation-transfer-failure/source-responsibility의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-114-api_response" assertion으로 "T10.obligation-transfer-failure/source-responsibility에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-114-db_snapshot" assertion으로 "T10.obligation-transfer-failure/source-responsibility에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-114" assertion으로 "T10.obligation-transfer-failure/source-responsibility에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-114" assertion으로 "link-name-114: /data/hostObservation/extractor/rawRows/namedObservations/114/observationName의 실제 equals 기대값은 'source-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-114" assertion으로 "link-oracle-114: /data/hostObservation/extractor/rawRows/namedObservations/114/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-115-scenarios" assertion으로 "T10.obligation-transfer-failure/orphan-target-assignment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-115-api_response" assertion으로 "T10.obligation-transfer-failure/orphan-target-assignment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-115-db_snapshot" assertion으로 "T10.obligation-transfer-failure/orphan-target-assignment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-115" assertion으로 "T10.obligation-transfer-failure/orphan-target-assignment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-115" assertion으로 "link-name-115: /data/hostObservation/extractor/rawRows/namedObservations/115/observationName의 실제 equals 기대값은 'orphan-target-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-115" assertion으로 "link-oracle-115: /data/hostObservation/extractor/rawRows/namedObservations/115/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-116-scenarios" assertion으로 "T10.obligation-transfer-failure/transfer-guard의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-116-api_response" assertion으로 "T10.obligation-transfer-failure/transfer-guard에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-116-db_snapshot" assertion으로 "T10.obligation-transfer-failure/transfer-guard에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-116" assertion으로 "T10.obligation-transfer-failure/transfer-guard에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-116" assertion으로 "link-name-116: /data/hostObservation/extractor/rawRows/namedObservations/116/observationName의 실제 equals 기대값은 'transfer-guard'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-116" assertion으로 "link-oracle-116: /data/hostObservation/extractor/rawRows/namedObservations/116/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-117-scenarios" assertion으로 "T10.partial-transfer-atomic/transferred-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-117-api_response" assertion으로 "T10.partial-transfer-atomic/transferred-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-117-db_snapshot" assertion으로 "T10.partial-transfer-atomic/transferred-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-117" assertion으로 "T10.partial-transfer-atomic/transferred-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-117" assertion으로 "link-name-117: /data/hostObservation/extractor/rawRows/namedObservations/117/observationName의 실제 equals 기대값은 'transferred-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-117" assertion으로 "link-oracle-117: /data/hostObservation/extractor/rawRows/namedObservations/117/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-118-scenarios" assertion으로 "T10.partial-transfer-atomic/remaining-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-118-api_response" assertion으로 "T10.partial-transfer-atomic/remaining-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-118-db_snapshot" assertion으로 "T10.partial-transfer-atomic/remaining-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-118" assertion으로 "T10.partial-transfer-atomic/remaining-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-118" assertion으로 "link-name-118: /data/hostObservation/extractor/rawRows/namedObservations/118/observationName의 실제 equals 기대값은 'remaining-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-118" assertion으로 "link-oracle-118: /data/hostObservation/extractor/rawRows/namedObservations/118/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-119-scenarios" assertion으로 "T10.partial-transfer-atomic/unresolved-root-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-119-api_response" assertion으로 "T10.partial-transfer-atomic/unresolved-root-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-119-db_snapshot" assertion으로 "T10.partial-transfer-atomic/unresolved-root-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-119" assertion으로 "T10.partial-transfer-atomic/unresolved-root-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-119" assertion으로 "link-name-119: /data/hostObservation/extractor/rawRows/namedObservations/119/observationName의 실제 equals 기대값은 'unresolved-root-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-119" assertion으로 "link-oracle-119: /data/hostObservation/extractor/rawRows/namedObservations/119/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-120-scenarios" assertion으로 "T10.partial-transfer-atomic/current-assignments의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-120-api_response" assertion으로 "T10.partial-transfer-atomic/current-assignments에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-120-db_snapshot" assertion으로 "T10.partial-transfer-atomic/current-assignments에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-120" assertion으로 "T10.partial-transfer-atomic/current-assignments에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-120" assertion으로 "link-name-120: /data/hostObservation/extractor/rawRows/namedObservations/120/observationName의 실제 equals 기대값은 'current-assignments'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-120" assertion으로 "link-oracle-120: /data/hostObservation/extractor/rawRows/namedObservations/120/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-121-scenarios" assertion으로 "T10.emergency-and-resolution/emergency-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-121-api_response" assertion으로 "T10.emergency-and-resolution/emergency-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-121-db_snapshot" assertion으로 "T10.emergency-and-resolution/emergency-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-121" assertion으로 "T10.emergency-and-resolution/emergency-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-121" assertion으로 "link-name-121: /data/hostObservation/extractor/rawRows/namedObservations/121/observationName의 실제 equals 기대값은 'emergency-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-121" assertion으로 "link-oracle-121: /data/hostObservation/extractor/rawRows/namedObservations/121/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-122-scenarios" assertion으로 "T10.emergency-and-resolution/unauthorized-resolution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-122-api_response" assertion으로 "T10.emergency-and-resolution/unauthorized-resolution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-122-db_snapshot" assertion으로 "T10.emergency-and-resolution/unauthorized-resolution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-122" assertion으로 "T10.emergency-and-resolution/unauthorized-resolution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-122" assertion으로 "link-name-122: /data/hostObservation/extractor/rawRows/namedObservations/122/observationName의 실제 equals 기대값은 'unauthorized-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-122" assertion으로 "link-oracle-122: /data/hostObservation/extractor/rawRows/namedObservations/122/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-123-scenarios" assertion으로 "T10.emergency-and-resolution/unauthorized-waiver의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-123-api_response" assertion으로 "T10.emergency-and-resolution/unauthorized-waiver에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-123-db_snapshot" assertion으로 "T10.emergency-and-resolution/unauthorized-waiver에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-123" assertion으로 "T10.emergency-and-resolution/unauthorized-waiver에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-123" assertion으로 "link-name-123: /data/hostObservation/extractor/rawRows/namedObservations/123/observationName의 실제 equals 기대값은 'unauthorized-waiver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-123" assertion으로 "link-oracle-123: /data/hostObservation/extractor/rawRows/namedObservations/123/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-124-scenarios" assertion으로 "T11.parent-not-child-completion/parent-contribution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-124-api_response" assertion으로 "T11.parent-not-child-completion/parent-contribution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-124-db_snapshot" assertion으로 "T11.parent-not-child-completion/parent-contribution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-124" assertion으로 "T11.parent-not-child-completion/parent-contribution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-124" assertion으로 "link-name-124: /data/hostObservation/extractor/rawRows/namedObservations/124/observationName의 실제 equals 기대값은 'parent-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-124" assertion으로 "link-oracle-124: /data/hostObservation/extractor/rawRows/namedObservations/124/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-125-scenarios" assertion으로 "T11.parent-not-child-completion/parent-assessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-125-api_response" assertion으로 "T11.parent-not-child-completion/parent-assessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-125-db_snapshot" assertion으로 "T11.parent-not-child-completion/parent-assessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-125" assertion으로 "T11.parent-not-child-completion/parent-assessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-125" assertion으로 "link-name-125: /data/hostObservation/extractor/rawRows/namedObservations/125/observationName의 실제 equals 기대값은 'parent-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-125" assertion으로 "link-oracle-125: /data/hostObservation/extractor/rawRows/namedObservations/125/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-126-scenarios" assertion으로 "T11.parent-not-child-completion/false-fulfilled-closure의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-126-api_response" assertion으로 "T11.parent-not-child-completion/false-fulfilled-closure에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-126-db_snapshot" assertion으로 "T11.parent-not-child-completion/false-fulfilled-closure에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-126" assertion으로 "T11.parent-not-child-completion/false-fulfilled-closure에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-126" assertion으로 "link-name-126: /data/hostObservation/extractor/rawRows/namedObservations/126/observationName의 실제 equals 기대값은 'false-fulfilled-closure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-126" assertion으로 "link-oracle-126: /data/hostObservation/extractor/rawRows/namedObservations/126/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-127-scenarios" assertion으로 "T11.parent-not-child-completion/alternative-close의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-127-api_response" assertion으로 "T11.parent-not-child-completion/alternative-close에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-127-db_snapshot" assertion으로 "T11.parent-not-child-completion/alternative-close에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-127" assertion으로 "T11.parent-not-child-completion/alternative-close에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-127" assertion으로 "link-name-127: /data/hostObservation/extractor/rawRows/namedObservations/127/observationName의 실제 equals 기대값은 'alternative-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-127" assertion으로 "link-oracle-127: /data/hostObservation/extractor/rawRows/namedObservations/127/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-128-scenarios" assertion으로 "T11.work-transitions/cancel-draft-physical-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-128-api_response" assertion으로 "T11.work-transitions/cancel-draft-physical-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-128-db_snapshot" assertion으로 "T11.work-transitions/cancel-draft-physical-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-128" assertion으로 "T11.work-transitions/cancel-draft-physical-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-128" assertion으로 "link-name-128: /data/hostObservation/extractor/rawRows/namedObservations/128/observationName의 실제 equals 기대값은 'cancel-draft-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-128" assertion으로 "link-oracle-128: /data/hostObservation/extractor/rawRows/namedObservations/128/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-129-scenarios" assertion으로 "T11.work-transitions/timeout-generated-arrival-or-approval의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-129-api_response" assertion으로 "T11.work-transitions/timeout-generated-arrival-or-approval에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-129-db_snapshot" assertion으로 "T11.work-transitions/timeout-generated-arrival-or-approval에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-129" assertion으로 "T11.work-transitions/timeout-generated-arrival-or-approval에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-129" assertion으로 "link-name-129: /data/hostObservation/extractor/rawRows/namedObservations/129/observationName의 실제 equals 기대값은 'timeout-generated-arrival-or-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-129" assertion으로 "link-oracle-129: /data/hostObservation/extractor/rawRows/namedObservations/129/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-130-scenarios" assertion으로 "T11.work-transitions/unsupported-activation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-130-api_response" assertion으로 "T11.work-transitions/unsupported-activation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-130-db_snapshot" assertion으로 "T11.work-transitions/unsupported-activation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-130" assertion으로 "T11.work-transitions/unsupported-activation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-130" assertion으로 "link-name-130: /data/hostObservation/extractor/rawRows/namedObservations/130/observationName의 실제 equals 기대값은 'unsupported-activation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-130" assertion으로 "link-oracle-130: /data/hostObservation/extractor/rawRows/namedObservations/130/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-131-scenarios" assertion으로 "T11.work-transitions/stale-revision-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-131-api_response" assertion으로 "T11.work-transitions/stale-revision-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-131-db_snapshot" assertion으로 "T11.work-transitions/stale-revision-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-131" assertion으로 "T11.work-transitions/stale-revision-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-131" assertion으로 "link-name-131: /data/hostObservation/extractor/rawRows/namedObservations/131/observationName의 실제 equals 기대값은 'stale-revision-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-131" assertion으로 "link-oracle-131: /data/hostObservation/extractor/rawRows/namedObservations/131/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-132-scenarios" assertion으로 "T11.work-transitions/pending-assessment-fulfilled-close의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-132-api_response" assertion으로 "T11.work-transitions/pending-assessment-fulfilled-close에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-132-db_snapshot" assertion으로 "T11.work-transitions/pending-assessment-fulfilled-close에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-132" assertion으로 "T11.work-transitions/pending-assessment-fulfilled-close에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-132" assertion으로 "link-name-132: /data/hostObservation/extractor/rawRows/namedObservations/132/observationName의 실제 equals 기대값은 'pending-assessment-fulfilled-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-132" assertion으로 "link-oracle-132: /data/hostObservation/extractor/rawRows/namedObservations/132/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-133-scenarios" assertion으로 "T11.work-transitions/cancel-preservation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-133-api_response" assertion으로 "T11.work-transitions/cancel-preservation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-133-db_snapshot" assertion으로 "T11.work-transitions/cancel-preservation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-133" assertion으로 "T11.work-transitions/cancel-preservation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-133" assertion으로 "link-name-133: /data/hostObservation/extractor/rawRows/namedObservations/133/observationName의 실제 equals 기대값은 'cancel-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-133" assertion으로 "link-oracle-133: /data/hostObservation/extractor/rawRows/namedObservations/133/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-134-scenarios" assertion으로 "T11.dependency-and-followup/dependency-cycle-created의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-134-api_response" assertion으로 "T11.dependency-and-followup/dependency-cycle-created에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-134-db_snapshot" assertion으로 "T11.dependency-and-followup/dependency-cycle-created에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-134" assertion으로 "T11.dependency-and-followup/dependency-cycle-created에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-134" assertion으로 "link-name-134: /data/hostObservation/extractor/rawRows/namedObservations/134/observationName의 실제 equals 기대값은 'dependency-cycle-created'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-134" assertion으로 "link-oracle-134: /data/hostObservation/extractor/rawRows/namedObservations/134/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-135-scenarios" assertion으로 "T11.dependency-and-followup/shared-distinct-contribution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-135-api_response" assertion으로 "T11.dependency-and-followup/shared-distinct-contribution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-135-db_snapshot" assertion으로 "T11.dependency-and-followup/shared-distinct-contribution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-135" assertion으로 "T11.dependency-and-followup/shared-distinct-contribution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-135" assertion으로 "link-name-135: /data/hostObservation/extractor/rawRows/namedObservations/135/observationName의 실제 equals 기대값은 'shared-distinct-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-135" assertion으로 "link-oracle-135: /data/hostObservation/extractor/rawRows/namedObservations/135/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-136-scenarios" assertion으로 "T11.dependency-and-followup/closed-work-reopen-or-rewrite의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-136-api_response" assertion으로 "T11.dependency-and-followup/closed-work-reopen-or-rewrite에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-136-db_snapshot" assertion으로 "T11.dependency-and-followup/closed-work-reopen-or-rewrite에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-136" assertion으로 "T11.dependency-and-followup/closed-work-reopen-or-rewrite에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-136" assertion으로 "link-name-136: /data/hostObservation/extractor/rawRows/namedObservations/136/observationName의 실제 equals 기대값은 'closed-work-reopen-or-rewrite'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-136" assertion으로 "link-oracle-136: /data/hostObservation/extractor/rawRows/namedObservations/136/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-137-scenarios" assertion으로 "T11.dependency-and-followup/followup-link의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-137-api_response" assertion으로 "T11.dependency-and-followup/followup-link에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-137-db_snapshot" assertion으로 "T11.dependency-and-followup/followup-link에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-137" assertion으로 "T11.dependency-and-followup/followup-link에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-137" assertion으로 "link-name-137: /data/hostObservation/extractor/rawRows/namedObservations/137/observationName의 실제 equals 기대값은 'followup-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-137" assertion으로 "link-oracle-137: /data/hostObservation/extractor/rawRows/namedObservations/137/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-138-contracts" assertion으로 "T12.assessment-evidence-policy/insufficient-evidence의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-138-scenarios" assertion으로 "T12.assessment-evidence-policy/insufficient-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-138-api_response" assertion으로 "T12.assessment-evidence-policy/insufficient-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-138-db_snapshot" assertion으로 "T12.assessment-evidence-policy/insufficient-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-138" assertion으로 "T12.assessment-evidence-policy/insufficient-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-138" assertion으로 "link-name-138: /data/hostObservation/extractor/rawRows/namedObservations/138/observationName의 실제 equals 기대값은 'insufficient-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-138" assertion으로 "link-oracle-138: /data/hostObservation/extractor/rawRows/namedObservations/138/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-139-contracts" assertion으로 "T12.assessment-evidence-policy/wrong-place-condition의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-139-scenarios" assertion으로 "T12.assessment-evidence-policy/wrong-place-condition의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-139-api_response" assertion으로 "T12.assessment-evidence-policy/wrong-place-condition에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-139-db_snapshot" assertion으로 "T12.assessment-evidence-policy/wrong-place-condition에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-139" assertion으로 "T12.assessment-evidence-policy/wrong-place-condition에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-139" assertion으로 "link-name-139: /data/hostObservation/extractor/rawRows/namedObservations/139/observationName의 실제 equals 기대값은 'wrong-place-condition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-139" assertion으로 "link-oracle-139: /data/hostObservation/extractor/rawRows/namedObservations/139/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-140-contracts" assertion으로 "T12.assessment-evidence-policy/assessment-snapshot의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-140-scenarios" assertion으로 "T12.assessment-evidence-policy/assessment-snapshot의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-140-api_response" assertion으로 "T12.assessment-evidence-policy/assessment-snapshot에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-140-db_snapshot" assertion으로 "T12.assessment-evidence-policy/assessment-snapshot에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-140" assertion으로 "T12.assessment-evidence-policy/assessment-snapshot에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-140" assertion으로 "link-name-140: /data/hostObservation/extractor/rawRows/namedObservations/140/observationName의 실제 equals 기대값은 'assessment-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-140" assertion으로 "link-oracle-140: /data/hostObservation/extractor/rawRows/namedObservations/140/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-141-contracts" assertion으로 "T12.assessment-evidence-policy/missing-as-zero의 필수 계층 contracts profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-141-scenarios" assertion으로 "T12.assessment-evidence-policy/missing-as-zero의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-141-api_response" assertion으로 "T12.assessment-evidence-policy/missing-as-zero에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-141-db_snapshot" assertion으로 "T12.assessment-evidence-policy/missing-as-zero에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-141" assertion으로 "T12.assessment-evidence-policy/missing-as-zero에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-141" assertion으로 "link-name-141: /data/hostObservation/extractor/rawRows/namedObservations/141/observationName의 실제 equals 기대값은 'missing-as-zero'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-141" assertion으로 "link-oracle-141: /data/hostObservation/extractor/rawRows/namedObservations/141/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-142-scenarios" assertion으로 "T12.deadline-change-and-correction/historical-violation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-142-api_response" assertion으로 "T12.deadline-change-and-correction/historical-violation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-142-db_snapshot" assertion으로 "T12.deadline-change-and-correction/historical-violation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-142" assertion으로 "T12.deadline-change-and-correction/historical-violation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-142" assertion으로 "link-name-142: /data/hostObservation/extractor/rawRows/namedObservations/142/observationName의 실제 equals 기대값은 'historical-violation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-142" assertion으로 "link-oracle-142: /data/hostObservation/extractor/rawRows/namedObservations/142/oracleId의 실제 equals 기대값은 'T12.deadline-change-and-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-143-scenarios" assertion으로 "T13.approval-version-binding/old-approval-revised-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-143-api_response" assertion으로 "T13.approval-version-binding/old-approval-revised-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-143-db_snapshot" assertion으로 "T13.approval-version-binding/old-approval-revised-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-143" assertion으로 "T13.approval-version-binding/old-approval-revised-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-143" assertion으로 "link-name-143: /data/hostObservation/extractor/rawRows/namedObservations/143/observationName의 실제 equals 기대값은 'old-approval-revised-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-143" assertion으로 "link-oracle-143: /data/hostObservation/extractor/rawRows/namedObservations/143/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-144-scenarios" assertion으로 "T13.approval-version-binding/unauthorized-approved-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-144-api_response" assertion으로 "T13.approval-version-binding/unauthorized-approved-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-144-db_snapshot" assertion으로 "T13.approval-version-binding/unauthorized-approved-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-144" assertion으로 "T13.approval-version-binding/unauthorized-approved-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-144" assertion으로 "link-name-144: /data/hostObservation/extractor/rawRows/namedObservations/144/observationName의 실제 equals 기대값은 'unauthorized-approved-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-144" assertion으로 "link-oracle-144: /data/hostObservation/extractor/rawRows/namedObservations/144/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-145-scenarios" assertion으로 "T13.approval-version-binding/approval-bound의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-145-api_response" assertion으로 "T13.approval-version-binding/approval-bound에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-145-db_snapshot" assertion으로 "T13.approval-version-binding/approval-bound에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-145" assertion으로 "T13.approval-version-binding/approval-bound에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-145" assertion으로 "link-name-145: /data/hostObservation/extractor/rawRows/namedObservations/145/observationName의 실제 equals 기대값은 'approval-bound'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-145" assertion으로 "link-oracle-145: /data/hostObservation/extractor/rawRows/namedObservations/145/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-146-scenarios" assertion으로 "T13.delivery-commitment-arrival-separate/purchase-created-held-inventory의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-146-api_response" assertion으로 "T13.delivery-commitment-arrival-separate/purchase-created-held-inventory에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-146-db_snapshot" assertion으로 "T13.delivery-commitment-arrival-separate/purchase-created-held-inventory에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-146" assertion으로 "T13.delivery-commitment-arrival-separate/purchase-created-held-inventory에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-146" assertion으로 "link-name-146: /data/hostObservation/extractor/rawRows/namedObservations/146/observationName의 실제 equals 기대값은 'purchase-created-held-inventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-146" assertion으로 "link-oracle-146: /data/hostObservation/extractor/rawRows/namedObservations/146/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-147-scenarios" assertion으로 "T13.delivery-commitment-arrival-separate/transmission-created-arrival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-147-api_response" assertion으로 "T13.delivery-commitment-arrival-separate/transmission-created-arrival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-147-db_snapshot" assertion으로 "T13.delivery-commitment-arrival-separate/transmission-created-arrival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-147" assertion으로 "T13.delivery-commitment-arrival-separate/transmission-created-arrival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-147" assertion으로 "link-name-147: /data/hostObservation/extractor/rawRows/namedObservations/147/observationName의 실제 equals 기대값은 'transmission-created-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-147" assertion으로 "link-oracle-147: /data/hostObservation/extractor/rawRows/namedObservations/147/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-148-scenarios" assertion으로 "T13.delivery-commitment-arrival-separate/supplier-acceptance-created-arrival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-148-api_response" assertion으로 "T13.delivery-commitment-arrival-separate/supplier-acceptance-created-arrival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-148-db_snapshot" assertion으로 "T13.delivery-commitment-arrival-separate/supplier-acceptance-created-arrival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-148" assertion으로 "T13.delivery-commitment-arrival-separate/supplier-acceptance-created-arrival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-148" assertion으로 "link-name-148: /data/hostObservation/extractor/rawRows/namedObservations/148/observationName의 실제 equals 기대값은 'supplier-acceptance-created-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-148" assertion으로 "link-oracle-148: /data/hostObservation/extractor/rawRows/namedObservations/148/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-149-scenarios" assertion으로 "T13.delivery-commitment-arrival-separate/cancel-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-149-api_response" assertion으로 "T13.delivery-commitment-arrival-separate/cancel-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-149-db_snapshot" assertion으로 "T13.delivery-commitment-arrival-separate/cancel-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-149" assertion으로 "T13.delivery-commitment-arrival-separate/cancel-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-149" assertion으로 "link-name-149: /data/hostObservation/extractor/rawRows/namedObservations/149/observationName의 실제 equals 기대값은 'cancel-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-149" assertion으로 "link-oracle-149: /data/hostObservation/extractor/rawRows/namedObservations/149/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-150-scenarios" assertion으로 "T13.partial-and-excess-contributions/authorized-arrival-contribution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-150-api_response" assertion으로 "T13.partial-and-excess-contributions/authorized-arrival-contribution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-150-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/authorized-arrival-contribution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-150" assertion으로 "T13.partial-and-excess-contributions/authorized-arrival-contribution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-150" assertion으로 "link-name-150: /data/hostObservation/extractor/rawRows/namedObservations/150/observationName의 실제 equals 기대값은 'authorized-arrival-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-150" assertion으로 "link-oracle-150: /data/hostObservation/extractor/rawRows/namedObservations/150/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-151-scenarios" assertion으로 "T13.partial-and-excess-contributions/excess-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-151-api_response" assertion으로 "T13.partial-and-excess-contributions/excess-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-151-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/excess-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-151" assertion으로 "T13.partial-and-excess-contributions/excess-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-151" assertion으로 "link-name-151: /data/hostObservation/extractor/rawRows/namedObservations/151/observationName의 실제 equals 기대값은 'excess-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-151" assertion으로 "link-oracle-151: /data/hostObservation/extractor/rawRows/namedObservations/151/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-152-scenarios" assertion으로 "T13.partial-and-excess-contributions/return-added-to-purchase의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-152-api_response" assertion으로 "T13.partial-and-excess-contributions/return-added-to-purchase에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-152-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/return-added-to-purchase에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-152" assertion으로 "T13.partial-and-excess-contributions/return-added-to-purchase에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-152" assertion으로 "link-name-152: /data/hostObservation/extractor/rawRows/namedObservations/152/observationName의 실제 equals 기대값은 'return-added-to-purchase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-152" assertion으로 "link-oracle-152: /data/hostObservation/extractor/rawRows/namedObservations/152/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-153-scenarios" assertion으로 "T13.partial-and-excess-contributions/relocation-added-to-purchase의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-153-api_response" assertion으로 "T13.partial-and-excess-contributions/relocation-added-to-purchase에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-153-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/relocation-added-to-purchase에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-153" assertion으로 "T13.partial-and-excess-contributions/relocation-added-to-purchase에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-153" assertion으로 "link-name-153: /data/hostObservation/extractor/rawRows/namedObservations/153/observationName의 실제 equals 기대값은 'relocation-added-to-purchase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-153" assertion으로 "link-oracle-153: /data/hostObservation/extractor/rawRows/namedObservations/153/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-154-scenarios" assertion으로 "T13.partial-and-excess-contributions/contribution-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-154-api_response" assertion으로 "T13.partial-and-excess-contributions/contribution-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-154-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/contribution-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-154" assertion으로 "T13.partial-and-excess-contributions/contribution-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-154" assertion으로 "link-name-154: /data/hostObservation/extractor/rawRows/namedObservations/154/observationName의 실제 equals 기대값은 'contribution-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-154" assertion으로 "link-oracle-154: /data/hostObservation/extractor/rawRows/namedObservations/154/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-155-scenarios" assertion으로 "T13.partial-and-excess-contributions/excess-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-155-api_response" assertion으로 "T13.partial-and-excess-contributions/excess-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-155-db_snapshot" assertion으로 "T13.partial-and-excess-contributions/excess-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-155" assertion으로 "T13.partial-and-excess-contributions/excess-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-155" assertion으로 "link-name-155: /data/hostObservation/extractor/rawRows/namedObservations/155/observationName의 실제 equals 기대값은 'excess-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-155" assertion으로 "link-oracle-155: /data/hostObservation/extractor/rawRows/namedObservations/155/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-156-scenarios" assertion으로 "T14.many-to-many-shipment/distinct-cargo-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-156-api_response" assertion으로 "T14.many-to-many-shipment/distinct-cargo-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-156-db_snapshot" assertion으로 "T14.many-to-many-shipment/distinct-cargo-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-156" assertion으로 "T14.many-to-many-shipment/distinct-cargo-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-156" assertion으로 "link-name-156: /data/hostObservation/extractor/rawRows/namedObservations/156/observationName의 실제 equals 기대값은 'distinct-cargo-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-156" assertion으로 "link-oracle-156: /data/hostObservation/extractor/rawRows/namedObservations/156/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-157-scenarios" assertion으로 "T14.many-to-many-shipment/double-order-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-157-api_response" assertion으로 "T14.many-to-many-shipment/double-order-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-157-db_snapshot" assertion으로 "T14.many-to-many-shipment/double-order-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-157" assertion으로 "T14.many-to-many-shipment/double-order-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-157" assertion으로 "link-name-157: /data/hostObservation/extractor/rawRows/namedObservations/157/observationName의 실제 equals 기대값은 'double-order-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-157" assertion으로 "link-oracle-157: /data/hostObservation/extractor/rawRows/namedObservations/157/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-158-scenarios" assertion으로 "T14.many-to-many-shipment/leg-facts의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-158-api_response" assertion으로 "T14.many-to-many-shipment/leg-facts에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-158-db_snapshot" assertion으로 "T14.many-to-many-shipment/leg-facts에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-158" assertion으로 "T14.many-to-many-shipment/leg-facts에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-158" assertion으로 "link-name-158: /data/hostObservation/extractor/rawRows/namedObservations/158/observationName의 실제 equals 기대값은 'leg-facts'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-158" assertion으로 "link-oracle-158: /data/hostObservation/extractor/rawRows/namedObservations/158/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-159-scenarios" assertion으로 "T14.arrival-discrepancy-not-loss/received의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-159-api_response" assertion으로 "T14.arrival-discrepancy-not-loss/received에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-159-db_snapshot" assertion으로 "T14.arrival-discrepancy-not-loss/received에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-159" assertion으로 "T14.arrival-discrepancy-not-loss/received에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-159" assertion으로 "link-name-159: /data/hostObservation/extractor/rawRows/namedObservations/159/observationName의 실제 equals 기대값은 'received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-159" assertion으로 "link-oracle-159: /data/hostObservation/extractor/rawRows/namedObservations/159/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-160-scenarios" assertion으로 "T14.arrival-discrepancy-not-loss/transit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-160-api_response" assertion으로 "T14.arrival-discrepancy-not-loss/transit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-160-db_snapshot" assertion으로 "T14.arrival-discrepancy-not-loss/transit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-160" assertion으로 "T14.arrival-discrepancy-not-loss/transit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-160" assertion으로 "link-name-160: /data/hostObservation/extractor/rawRows/namedObservations/160/observationName의 실제 equals 기대값은 'transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-160" assertion으로 "link-oracle-160: /data/hostObservation/extractor/rawRows/namedObservations/160/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-161-scenarios" assertion으로 "T14.arrival-discrepancy-not-loss/automatic-loss의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-161-api_response" assertion으로 "T14.arrival-discrepancy-not-loss/automatic-loss에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-161-db_snapshot" assertion으로 "T14.arrival-discrepancy-not-loss/automatic-loss에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-161" assertion으로 "T14.arrival-discrepancy-not-loss/automatic-loss에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-161" assertion으로 "link-name-161: /data/hostObservation/extractor/rawRows/namedObservations/161/observationName의 실제 equals 기대값은 'automatic-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-161" assertion으로 "link-oracle-161: /data/hostObservation/extractor/rawRows/namedObservations/161/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-162-scenarios" assertion으로 "T14.arrival-discrepancy-not-loss/separate-observations의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-162-api_response" assertion으로 "T14.arrival-discrepancy-not-loss/separate-observations에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-162-db_snapshot" assertion으로 "T14.arrival-discrepancy-not-loss/separate-observations에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-162" assertion으로 "T14.arrival-discrepancy-not-loss/separate-observations에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-162" assertion으로 "link-name-162: /data/hostObservation/extractor/rawRows/namedObservations/162/observationName의 실제 equals 기대값은 'separate-observations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-162" assertion으로 "link-oracle-162: /data/hostObservation/extractor/rawRows/namedObservations/162/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-163-scenarios" assertion으로 "T14.arrival-discrepancy-not-loss/discrepancy-responsibility의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-163-api_response" assertion으로 "T14.arrival-discrepancy-not-loss/discrepancy-responsibility에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-163-db_snapshot" assertion으로 "T14.arrival-discrepancy-not-loss/discrepancy-responsibility에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-163" assertion으로 "T14.arrival-discrepancy-not-loss/discrepancy-responsibility에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-163" assertion으로 "link-name-163: /data/hostObservation/extractor/rawRows/namedObservations/163/observationName의 실제 equals 기대값은 'discrepancy-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-163" assertion으로 "link-oracle-163: /data/hostObservation/extractor/rawRows/namedObservations/163/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-164-scenarios" assertion으로 "T14.orphan-temperature-intake/orphan-intake의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-164-api_response" assertion으로 "T14.orphan-temperature-intake/orphan-intake에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-164-db_snapshot" assertion으로 "T14.orphan-temperature-intake/orphan-intake에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-164" assertion으로 "T14.orphan-temperature-intake/orphan-intake에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-164" assertion으로 "link-name-164: /data/hostObservation/extractor/rawRows/namedObservations/164/observationName의 실제 equals 기대값은 'orphan-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-164" assertion으로 "link-oracle-164: /data/hostObservation/extractor/rawRows/namedObservations/164/oracleId의 실제 equals 기대값은 'T14.orphan-temperature-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-165-scenarios" assertion으로 "T14.orphan-temperature-intake/notification-closes-intake의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-165-api_response" assertion으로 "T14.orphan-temperature-intake/notification-closes-intake에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-165-db_snapshot" assertion으로 "T14.orphan-temperature-intake/notification-closes-intake에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-165" assertion으로 "T14.orphan-temperature-intake/notification-closes-intake에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-165" assertion으로 "link-name-165: /data/hostObservation/extractor/rawRows/namedObservations/165/observationName의 실제 equals 기대값은 'notification-closes-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-165" assertion으로 "link-oracle-165: /data/hostObservation/extractor/rawRows/namedObservations/165/oracleId의 실제 equals 기대값은 'T14.orphan-temperature-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-166-scenarios" assertion으로 "T15.partial-regulatory-eligibility/confirmed-sell-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-166-api_response" assertion으로 "T15.partial-regulatory-eligibility/confirmed-sell-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-166-db_snapshot" assertion으로 "T15.partial-regulatory-eligibility/confirmed-sell-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-166" assertion으로 "T15.partial-regulatory-eligibility/confirmed-sell-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-166" assertion으로 "link-name-166: /data/hostObservation/extractor/rawRows/namedObservations/166/observationName의 실제 equals 기대값은 'confirmed-sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-166" assertion으로 "link-oracle-166: /data/hostObservation/extractor/rawRows/namedObservations/166/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-167-scenarios" assertion으로 "T15.partial-regulatory-eligibility/remaining-agency의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-167-api_response" assertion으로 "T15.partial-regulatory-eligibility/remaining-agency에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-167-db_snapshot" assertion으로 "T15.partial-regulatory-eligibility/remaining-agency에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-167" assertion으로 "T15.partial-regulatory-eligibility/remaining-agency에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-167" assertion으로 "link-name-167: /data/hostObservation/extractor/rawRows/namedObservations/167/observationName의 실제 equals 기대값은 'remaining-agency'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-167" assertion으로 "link-oracle-167: /data/hostObservation/extractor/rawRows/namedObservations/167/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-168-scenarios" assertion으로 "T15.partial-regulatory-eligibility/qc-expanded-agency-allowance의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-168-api_response" assertion으로 "T15.partial-regulatory-eligibility/qc-expanded-agency-allowance에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-168-db_snapshot" assertion으로 "T15.partial-regulatory-eligibility/qc-expanded-agency-allowance에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-168" assertion으로 "T15.partial-regulatory-eligibility/qc-expanded-agency-allowance에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-168" assertion으로 "link-name-168: /data/hostObservation/extractor/rawRows/namedObservations/168/observationName의 실제 equals 기대값은 'qc-expanded-agency-allowance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-168" assertion으로 "link-oracle-168: /data/hostObservation/extractor/rawRows/namedObservations/168/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-169-scenarios" assertion으로 "T15.partial-regulatory-eligibility/decision-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-169-api_response" assertion으로 "T15.partial-regulatory-eligibility/decision-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-169-db_snapshot" assertion으로 "T15.partial-regulatory-eligibility/decision-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-169" assertion으로 "T15.partial-regulatory-eligibility/decision-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-169" assertion으로 "link-name-169: /data/hostObservation/extractor/rawRows/namedObservations/169/observationName의 실제 equals 기대값은 'decision-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-169" assertion으로 "link-oracle-169: /data/hostObservation/extractor/rawRows/namedObservations/169/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-170-scenarios" assertion으로 "T15.procedure-lifecycle/local-document-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-170-api_response" assertion으로 "T15.procedure-lifecycle/local-document-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-170-db_snapshot" assertion으로 "T15.procedure-lifecycle/local-document-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-170" assertion으로 "T15.procedure-lifecycle/local-document-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-170" assertion으로 "link-name-170: /data/hostObservation/extractor/rawRows/namedObservations/170/observationName의 실제 equals 기대값은 'local-document-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-170" assertion으로 "link-oracle-170: /data/hostObservation/extractor/rawRows/namedObservations/170/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-171-scenarios" assertion으로 "T15.procedure-lifecycle/unknown-submit-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-171-api_response" assertion으로 "T15.procedure-lifecycle/unknown-submit-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-171-db_snapshot" assertion으로 "T15.procedure-lifecycle/unknown-submit-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-171" assertion으로 "T15.procedure-lifecycle/unknown-submit-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-171" assertion으로 "link-name-171: /data/hostObservation/extractor/rawRows/namedObservations/171/observationName의 실제 equals 기대값은 'unknown-submit-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-171" assertion으로 "link-oracle-171: /data/hostObservation/extractor/rawRows/namedObservations/171/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-172-scenarios" assertion으로 "T15.procedure-lifecycle/local-document-external-submission의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-172-api_response" assertion으로 "T15.procedure-lifecycle/local-document-external-submission에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-172-db_snapshot" assertion으로 "T15.procedure-lifecycle/local-document-external-submission에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-172" assertion으로 "T15.procedure-lifecycle/local-document-external-submission에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-172" assertion으로 "link-name-172: /data/hostObservation/extractor/rawRows/namedObservations/172/observationName의 실제 equals 기대값은 'local-document-external-submission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-172" assertion으로 "link-oracle-172: /data/hostObservation/extractor/rawRows/namedObservations/172/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-173-scenarios" assertion으로 "T15.procedure-lifecycle/regulatory-history의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-173-api_response" assertion으로 "T15.procedure-lifecycle/regulatory-history에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-173-db_snapshot" assertion으로 "T15.procedure-lifecycle/regulatory-history에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-173" assertion으로 "T15.procedure-lifecycle/regulatory-history에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-173" assertion으로 "link-name-173: /data/hostObservation/extractor/rawRows/namedObservations/173/observationName의 실제 equals 기대값은 'regulatory-history'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-173" assertion으로 "link-oracle-173: /data/hostObservation/extractor/rawRows/namedObservations/173/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-174-regulatory" assertion으로 "T15.unresolved-real-policy/confirmed-eligible의 필수 계층 regulatory profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-174-scenarios" assertion으로 "T15.unresolved-real-policy/confirmed-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-174-api_response" assertion으로 "T15.unresolved-real-policy/confirmed-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-174-db_snapshot" assertion으로 "T15.unresolved-real-policy/confirmed-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-174" assertion으로 "T15.unresolved-real-policy/confirmed-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-174" assertion으로 "link-name-174: /data/hostObservation/extractor/rawRows/namedObservations/174/observationName의 실제 equals 기대값은 'confirmed-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-174" assertion으로 "link-oracle-174: /data/hostObservation/extractor/rawRows/namedObservations/174/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-175-regulatory" assertion으로 "T15.unresolved-real-policy/legal-eligibility의 필수 계층 regulatory profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-175-scenarios" assertion으로 "T15.unresolved-real-policy/legal-eligibility의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-175-api_response" assertion으로 "T15.unresolved-real-policy/legal-eligibility에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-175-db_snapshot" assertion으로 "T15.unresolved-real-policy/legal-eligibility에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-175" assertion으로 "T15.unresolved-real-policy/legal-eligibility에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-175" assertion으로 "link-name-175: /data/hostObservation/extractor/rawRows/namedObservations/175/observationName의 실제 equals 기대값은 'legal-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-175" assertion으로 "link-oracle-175: /data/hostObservation/extractor/rawRows/namedObservations/175/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-176-regulatory" assertion으로 "T15.unresolved-real-policy/regulatory-gate의 필수 계층 regulatory profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-176-scenarios" assertion으로 "T15.unresolved-real-policy/regulatory-gate의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-176-api_response" assertion으로 "T15.unresolved-real-policy/regulatory-gate에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-176-db_snapshot" assertion으로 "T15.unresolved-real-policy/regulatory-gate에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-176" assertion으로 "T15.unresolved-real-policy/regulatory-gate에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-176" assertion으로 "link-name-176: /data/hostObservation/extractor/rawRows/namedObservations/176/observationName의 실제 equals 기대값은 'regulatory-gate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-176" assertion으로 "link-oracle-176: /data/hostObservation/extractor/rawRows/namedObservations/176/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-177-scenarios" assertion으로 "T16.provisional-and-independent-holds/provisional-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-177-api_response" assertion으로 "T16.provisional-and-independent-holds/provisional-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-177-db_snapshot" assertion으로 "T16.provisional-and-independent-holds/provisional-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-177" assertion으로 "T16.provisional-and-independent-holds/provisional-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-177" assertion으로 "link-name-177: /data/hostObservation/extractor/rawRows/namedObservations/177/observationName의 실제 equals 기대값은 'provisional-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-177" assertion으로 "link-oracle-177: /data/hostObservation/extractor/rawRows/namedObservations/177/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-178-scenarios" assertion으로 "T16.provisional-and-independent-holds/receipt-transit-double-creation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-178-api_response" assertion으로 "T16.provisional-and-independent-holds/receipt-transit-double-creation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-178-db_snapshot" assertion으로 "T16.provisional-and-independent-holds/receipt-transit-double-creation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-178" assertion으로 "T16.provisional-and-independent-holds/receipt-transit-double-creation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-178" assertion으로 "link-name-178: /data/hostObservation/extractor/rawRows/namedObservations/178/observationName의 실제 equals 기대값은 'receipt-transit-double-creation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-178" assertion으로 "link-oracle-178: /data/hostObservation/extractor/rawRows/namedObservations/178/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-179-scenarios" assertion으로 "T16.provisional-and-independent-holds/eligible-after-QC-only-release의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-179-api_response" assertion으로 "T16.provisional-and-independent-holds/eligible-after-QC-only-release에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-179-db_snapshot" assertion으로 "T16.provisional-and-independent-holds/eligible-after-QC-only-release에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-179" assertion으로 "T16.provisional-and-independent-holds/eligible-after-QC-only-release에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-179" assertion으로 "link-name-179: /data/hostObservation/extractor/rawRows/namedObservations/179/observationName의 실제 equals 기대값은 'eligible-after-QC-only-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-179" assertion으로 "link-oracle-179: /data/hostObservation/extractor/rawRows/namedObservations/179/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-180-scenarios" assertion으로 "T16.provisional-and-independent-holds/remaining-recall의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-180-api_response" assertion으로 "T16.provisional-and-independent-holds/remaining-recall에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-180-db_snapshot" assertion으로 "T16.provisional-and-independent-holds/remaining-recall에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-180" assertion으로 "T16.provisional-and-independent-holds/remaining-recall에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-180" assertion으로 "link-name-180: /data/hostObservation/extractor/rawRows/namedObservations/180/observationName의 실제 equals 기대값은 'remaining-recall'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-180" assertion으로 "link-oracle-180: /data/hostObservation/extractor/rawRows/namedObservations/180/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-181-scenarios" assertion으로 "T16.provisional-and-independent-holds/hold-authority의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-181-api_response" assertion으로 "T16.provisional-and-independent-holds/hold-authority에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-181-db_snapshot" assertion으로 "T16.provisional-and-independent-holds/hold-authority에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-181" assertion으로 "T16.provisional-and-independent-holds/hold-authority에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-181" assertion으로 "link-name-181: /data/hostObservation/extractor/rawRows/namedObservations/181/observationName의 실제 equals 기대값은 'hold-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-181" assertion으로 "link-oracle-181: /data/hostObservation/extractor/rawRows/namedObservations/181/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-182-scenarios" assertion으로 "T16.movement-stocktake-adjustment/held-before-adjustment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-182-api_response" assertion으로 "T16.movement-stocktake-adjustment/held-before-adjustment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-182-db_snapshot" assertion으로 "T16.movement-stocktake-adjustment/held-before-adjustment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-182" assertion으로 "T16.movement-stocktake-adjustment/held-before-adjustment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-182" assertion으로 "link-name-182: /data/hostObservation/extractor/rawRows/namedObservations/182/observationName의 실제 equals 기대값은 'held-before-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-182" assertion으로 "link-oracle-182: /data/hostObservation/extractor/rawRows/namedObservations/182/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-183-scenarios" assertion으로 "T16.movement-stocktake-adjustment/held-after-adjustment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-183-api_response" assertion으로 "T16.movement-stocktake-adjustment/held-after-adjustment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-183-db_snapshot" assertion으로 "T16.movement-stocktake-adjustment/held-after-adjustment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-183" assertion으로 "T16.movement-stocktake-adjustment/held-after-adjustment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-183" assertion으로 "link-name-183: /data/hostObservation/extractor/rawRows/namedObservations/183/observationName의 실제 equals 기대값은 'held-after-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-183" assertion으로 "link-oracle-183: /data/hostObservation/extractor/rawRows/namedObservations/183/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-184-scenarios" assertion으로 "T16.movement-stocktake-adjustment/distinct-total-after-internal-move의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-184-api_response" assertion으로 "T16.movement-stocktake-adjustment/distinct-total-after-internal-move에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-184-db_snapshot" assertion으로 "T16.movement-stocktake-adjustment/distinct-total-after-internal-move에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-184" assertion으로 "T16.movement-stocktake-adjustment/distinct-total-after-internal-move에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-184" assertion으로 "link-name-184: /data/hostObservation/extractor/rawRows/namedObservations/184/observationName의 실제 equals 기대값은 'distinct-total-after-internal-move'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-184" assertion으로 "link-oracle-184: /data/hostObservation/extractor/rawRows/namedObservations/184/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-185-scenarios" assertion으로 "T16.movement-stocktake-adjustment/adjustment-proof의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-185-api_response" assertion으로 "T16.movement-stocktake-adjustment/adjustment-proof에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-185-db_snapshot" assertion으로 "T16.movement-stocktake-adjustment/adjustment-proof에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-185" assertion으로 "T16.movement-stocktake-adjustment/adjustment-proof에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-185" assertion으로 "link-name-185: /data/hostObservation/extractor/rawRows/namedObservations/185/observationName의 실제 equals 기대값은 'adjustment-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-185" assertion으로 "link-oracle-185: /data/hostObservation/extractor/rawRows/namedObservations/185/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-186-scenarios" assertion으로 "T16.no-event-expiry/allocation-after-boundary의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-186-api_response" assertion으로 "T16.no-event-expiry/allocation-after-boundary에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-186-db_snapshot" assertion으로 "T16.no-event-expiry/allocation-after-boundary에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-186" assertion으로 "T16.no-event-expiry/allocation-after-boundary에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-186" assertion으로 "link-name-186: /data/hostObservation/extractor/rawRows/namedObservations/186/observationName의 실제 equals 기대값은 'allocation-after-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-186" assertion으로 "link-oracle-186: /data/hostObservation/extractor/rawRows/namedObservations/186/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-187-scenarios" assertion으로 "T16.no-event-expiry/post-expiry-dispatched의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-187-api_response" assertion으로 "T16.no-event-expiry/post-expiry-dispatched에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-187-db_snapshot" assertion으로 "T16.no-event-expiry/post-expiry-dispatched에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-187" assertion으로 "T16.no-event-expiry/post-expiry-dispatched에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-187" assertion으로 "link-name-187: /data/hostObservation/extractor/rawRows/namedObservations/187/observationName의 실제 equals 기대값은 'post-expiry-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-187" assertion으로 "link-oracle-187: /data/hostObservation/extractor/rawRows/namedObservations/187/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-188-scenarios" assertion으로 "T16.no-event-expiry/expiry-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-188-api_response" assertion으로 "T16.no-event-expiry/expiry-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-188-db_snapshot" assertion으로 "T16.no-event-expiry/expiry-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-188" assertion으로 "T16.no-event-expiry/expiry-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-188" assertion으로 "link-name-188: /data/hostObservation/extractor/rawRows/namedObservations/188/observationName의 실제 equals 기대값은 'expiry-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-188" assertion으로 "link-oracle-188: /data/hostObservation/extractor/rawRows/namedObservations/188/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-189-scenarios" assertion으로 "T16.no-event-expiry/boundary-recheck의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-189-api_response" assertion으로 "T16.no-event-expiry/boundary-recheck에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-189-db_snapshot" assertion으로 "T16.no-event-expiry/boundary-recheck에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-189" assertion으로 "T16.no-event-expiry/boundary-recheck에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-189" assertion으로 "link-name-189: /data/hostObservation/extractor/rawRows/namedObservations/189/observationName의 실제 equals 기대값은 'boundary-recheck'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-189" assertion으로 "link-oracle-189: /data/hostObservation/extractor/rawRows/namedObservations/189/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-190-scenarios" assertion으로 "T17.reserve-pick-dispatch/held-after-reserve의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-190-api_response" assertion으로 "T17.reserve-pick-dispatch/held-after-reserve에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-190-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/held-after-reserve에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-190" assertion으로 "T17.reserve-pick-dispatch/held-after-reserve에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-190" assertion으로 "link-name-190: /data/hostObservation/extractor/rawRows/namedObservations/190/observationName의 실제 equals 기대값은 'held-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-190" assertion으로 "link-oracle-190: /data/hostObservation/extractor/rawRows/namedObservations/190/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-191-scenarios" assertion으로 "T17.reserve-pick-dispatch/eligible-after-reserve의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-191-api_response" assertion으로 "T17.reserve-pick-dispatch/eligible-after-reserve에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-191-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/eligible-after-reserve에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-191" assertion으로 "T17.reserve-pick-dispatch/eligible-after-reserve에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-191" assertion으로 "link-name-191: /data/hostObservation/extractor/rawRows/namedObservations/191/observationName의 실제 equals 기대값은 'eligible-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-191" assertion으로 "link-oracle-191: /data/hostObservation/extractor/rawRows/namedObservations/191/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-192-scenarios" assertion으로 "T17.reserve-pick-dispatch/unreserved-after-reserve의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-192-api_response" assertion으로 "T17.reserve-pick-dispatch/unreserved-after-reserve에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-192-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/unreserved-after-reserve에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-192" assertion으로 "T17.reserve-pick-dispatch/unreserved-after-reserve에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-192" assertion으로 "link-name-192: /data/hostObservation/extractor/rawRows/namedObservations/192/observationName의 실제 equals 기대값은 'unreserved-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-192" assertion으로 "link-oracle-192: /data/hostObservation/extractor/rawRows/namedObservations/192/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-193-scenarios" assertion으로 "T17.reserve-pick-dispatch/warehouse-after-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-193-api_response" assertion으로 "T17.reserve-pick-dispatch/warehouse-after-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-193-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/warehouse-after-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-193" assertion으로 "T17.reserve-pick-dispatch/warehouse-after-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-193" assertion으로 "link-name-193: /data/hostObservation/extractor/rawRows/namedObservations/193/observationName의 실제 equals 기대값은 'warehouse-after-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-193" assertion으로 "link-oracle-193: /data/hostObservation/extractor/rawRows/namedObservations/193/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-194-scenarios" assertion으로 "T17.reserve-pick-dispatch/dispatch-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-194-api_response" assertion으로 "T17.reserve-pick-dispatch/dispatch-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-194-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/dispatch-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-194" assertion으로 "T17.reserve-pick-dispatch/dispatch-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-194" assertion으로 "link-name-194: /data/hostObservation/extractor/rawRows/namedObservations/194/observationName의 실제 equals 기대값은 'dispatch-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-194" assertion으로 "link-oracle-194: /data/hostObservation/extractor/rawRows/namedObservations/194/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-195-scenarios" assertion으로 "T17.reserve-pick-dispatch/in-transit-after-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-195-api_response" assertion으로 "T17.reserve-pick-dispatch/in-transit-after-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-195-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/in-transit-after-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-195" assertion으로 "T17.reserve-pick-dispatch/in-transit-after-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-195" assertion으로 "link-name-195: /data/hostObservation/extractor/rawRows/namedObservations/195/observationName의 실제 equals 기대값은 'in-transit-after-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-195" assertion으로 "link-oracle-195: /data/hostObservation/extractor/rawRows/namedObservations/195/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-196-scenarios" assertion으로 "T17.reserve-pick-dispatch/delivered-before-observation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-196-api_response" assertion으로 "T17.reserve-pick-dispatch/delivered-before-observation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-196-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/delivered-before-observation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-196" assertion으로 "T17.reserve-pick-dispatch/delivered-before-observation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-196" assertion으로 "link-name-196: /data/hostObservation/extractor/rawRows/namedObservations/196/observationName의 실제 equals 기대값은 'delivered-before-observation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-196" assertion으로 "link-oracle-196: /data/hostObservation/extractor/rawRows/namedObservations/196/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-197-scenarios" assertion으로 "T17.reserve-pick-dispatch/second-reservation-same60의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-197-api_response" assertion으로 "T17.reserve-pick-dispatch/second-reservation-same60에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-197-db_snapshot" assertion으로 "T17.reserve-pick-dispatch/second-reservation-same60에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-197" assertion으로 "T17.reserve-pick-dispatch/second-reservation-same60에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-197" assertion으로 "link-name-197: /data/hostObservation/extractor/rawRows/namedObservations/197/observationName의 실제 equals 기대값은 'second-reservation-same60'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-197" assertion으로 "link-oracle-197: /data/hostObservation/extractor/rawRows/namedObservations/197/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-198-scenarios" assertion으로 "T17.normal-consumed-delivery/delivered의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-198-api_response" assertion으로 "T17.normal-consumed-delivery/delivered에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-198-db_snapshot" assertion으로 "T17.normal-consumed-delivery/delivered에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-198" assertion으로 "T17.normal-consumed-delivery/delivered에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-198" assertion으로 "link-name-198: /data/hostObservation/extractor/rawRows/namedObservations/198/observationName의 실제 equals 기대값은 'delivered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-198" assertion으로 "link-oracle-198: /data/hostObservation/extractor/rawRows/namedObservations/198/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-199-scenarios" assertion으로 "T17.normal-consumed-delivery/cargo-in-transit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-199-api_response" assertion으로 "T17.normal-consumed-delivery/cargo-in-transit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-199-db_snapshot" assertion으로 "T17.normal-consumed-delivery/cargo-in-transit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-199" assertion으로 "T17.normal-consumed-delivery/cargo-in-transit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-199" assertion으로 "link-name-199: /data/hostObservation/extractor/rawRows/namedObservations/199/observationName의 실제 equals 기대값은 'cargo-in-transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-199" assertion으로 "link-oracle-199: /data/hostObservation/extractor/rawRows/namedObservations/199/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-200-scenarios" assertion으로 "T17.normal-consumed-delivery/delivery-assessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-200-api_response" assertion으로 "T17.normal-consumed-delivery/delivery-assessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-200-db_snapshot" assertion으로 "T17.normal-consumed-delivery/delivery-assessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-200" assertion으로 "T17.normal-consumed-delivery/delivery-assessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-200" assertion으로 "link-name-200: /data/hostObservation/extractor/rawRows/namedObservations/200/observationName의 실제 equals 기대값은 'delivery-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-200" assertion으로 "link-oracle-200: /data/hostObservation/extractor/rawRows/namedObservations/200/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-201-scenarios" assertion으로 "T17.normal-consumed-delivery/allocation-still의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-201-api_response" assertion으로 "T17.normal-consumed-delivery/allocation-still에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-201-db_snapshot" assertion으로 "T17.normal-consumed-delivery/allocation-still에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-201" assertion으로 "T17.normal-consumed-delivery/allocation-still에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-201" assertion으로 "link-name-201: /data/hostObservation/extractor/rawRows/namedObservations/201/observationName의 실제 equals 기대값은 'allocation-still'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-201" assertion으로 "link-oracle-201: /data/hostObservation/extractor/rawRows/namedObservations/201/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-202-scenarios" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-202-api_response" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-202-db_snapshot" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-202" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-202" assertion으로 "link-name-202: /data/hostObservation/extractor/rawRows/namedObservations/202/observationName의 실제 equals 기대값은 'delivery-created-new-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-202" assertion으로 "link-oracle-202: /data/hostObservation/extractor/rawRows/namedObservations/202/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-203-scenarios" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-warehouse-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-203-api_response" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-warehouse-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-203-db_snapshot" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-warehouse-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-203" assertion으로 "T17.normal-consumed-delivery/delivery-created-new-warehouse-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-203" assertion으로 "link-name-203: /data/hostObservation/extractor/rawRows/namedObservations/203/observationName의 실제 equals 기대값은 'delivery-created-new-warehouse-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-203" assertion으로 "link-oracle-203: /data/hostObservation/extractor/rawRows/namedObservations/203/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-204-scenarios" assertion으로 "T17.late-restriction-actual-delivery/actual-delivered의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-204-api_response" assertion으로 "T17.late-restriction-actual-delivery/actual-delivered에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-204-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/actual-delivered에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-204" assertion으로 "T17.late-restriction-actual-delivery/actual-delivered에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-204" assertion으로 "link-name-204: /data/hostObservation/extractor/rawRows/namedObservations/204/observationName의 실제 equals 기대값은 'actual-delivered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-204" assertion으로 "link-oracle-204: /data/hostObservation/extractor/rawRows/namedObservations/204/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-205-scenarios" assertion으로 "T17.late-restriction-actual-delivery/remaining-transit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-205-api_response" assertion으로 "T17.late-restriction-actual-delivery/remaining-transit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-205-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/remaining-transit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-205" assertion으로 "T17.late-restriction-actual-delivery/remaining-transit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-205" assertion으로 "link-name-205: /data/hostObservation/extractor/rawRows/namedObservations/205/observationName의 실제 equals 기대값은 'remaining-transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-205" assertion으로 "link-oracle-205: /data/hostObservation/extractor/rawRows/namedObservations/205/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-206-scenarios" assertion으로 "T17.late-restriction-actual-delivery/new-warehouse-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-206-api_response" assertion으로 "T17.late-restriction-actual-delivery/new-warehouse-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-206-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/new-warehouse-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-206" assertion으로 "T17.late-restriction-actual-delivery/new-warehouse-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-206" assertion으로 "link-name-206: /data/hostObservation/extractor/rawRows/namedObservations/206/observationName의 실제 equals 기대값은 'new-warehouse-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-206" assertion으로 "link-oracle-206: /data/hostObservation/extractor/rawRows/namedObservations/206/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-207-scenarios" assertion으로 "T17.late-restriction-actual-delivery/new-executable-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-207-api_response" assertion으로 "T17.late-restriction-actual-delivery/new-executable-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-207-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/new-executable-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-207" assertion으로 "T17.late-restriction-actual-delivery/new-executable-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-207" assertion으로 "link-name-207: /data/hostObservation/extractor/rawRows/namedObservations/207/observationName의 실제 equals 기대값은 'new-executable-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-207" assertion으로 "link-oracle-207: /data/hostObservation/extractor/rawRows/namedObservations/207/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-208-scenarios" assertion으로 "T17.late-restriction-actual-delivery/fact-not-permission의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-208-api_response" assertion으로 "T17.late-restriction-actual-delivery/fact-not-permission에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-208-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/fact-not-permission에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-208" assertion으로 "T17.late-restriction-actual-delivery/fact-not-permission에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-208" assertion으로 "link-name-208: /data/hostObservation/extractor/rawRows/namedObservations/208/observationName의 실제 equals 기대값은 'fact-not-permission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-208" assertion으로 "link-oracle-208: /data/hostObservation/extractor/rawRows/namedObservations/208/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-209-scenarios" assertion으로 "T17.late-restriction-actual-delivery/late-restriction-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-209-api_response" assertion으로 "T17.late-restriction-actual-delivery/late-restriction-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-209-db_snapshot" assertion으로 "T17.late-restriction-actual-delivery/late-restriction-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-209" assertion으로 "T17.late-restriction-actual-delivery/late-restriction-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-209" assertion으로 "link-name-209: /data/hostObservation/extractor/rawRows/namedObservations/209/observationName의 실제 equals 기대값은 'late-restriction-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-209" assertion으로 "link-oracle-209: /data/hostObservation/extractor/rawRows/namedObservations/209/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-210-scenarios" assertion으로 "T17.false-or-unmatched-delivery/claim-inventory-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-210-api_response" assertion으로 "T17.false-or-unmatched-delivery/claim-inventory-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-210-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/claim-inventory-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-210" assertion으로 "T17.false-or-unmatched-delivery/claim-inventory-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-210" assertion으로 "link-name-210: /data/hostObservation/extractor/rawRows/namedObservations/210/observationName의 실제 equals 기대값은 'claim-inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-210" assertion으로 "link-oracle-210: /data/hostObservation/extractor/rawRows/namedObservations/210/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-211-scenarios" assertion으로 "T17.false-or-unmatched-delivery/claim-order-fulfilment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-211-api_response" assertion으로 "T17.false-or-unmatched-delivery/claim-order-fulfilment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-211-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/claim-order-fulfilment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-211" assertion으로 "T17.false-or-unmatched-delivery/claim-order-fulfilment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-211" assertion으로 "link-name-211: /data/hostObservation/extractor/rawRows/namedObservations/211/observationName의 실제 equals 기대값은 'claim-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-211" assertion으로 "link-oracle-211: /data/hostObservation/extractor/rawRows/namedObservations/211/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-212-scenarios" assertion으로 "T17.false-or-unmatched-delivery/claim-new-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-212-api_response" assertion으로 "T17.false-or-unmatched-delivery/claim-new-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-212-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/claim-new-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-212" assertion으로 "T17.false-or-unmatched-delivery/claim-new-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-212" assertion으로 "link-name-212: /data/hostObservation/extractor/rawRows/namedObservations/212/observationName의 실제 equals 기대값은 'claim-new-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-212" assertion으로 "link-oracle-212: /data/hostObservation/extractor/rawRows/namedObservations/212/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-213-scenarios" assertion으로 "T17.false-or-unmatched-delivery/claim-not-canonical의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-213-api_response" assertion으로 "T17.false-or-unmatched-delivery/claim-not-canonical에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-213-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/claim-not-canonical에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-213" assertion으로 "T17.false-or-unmatched-delivery/claim-not-canonical에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-213" assertion으로 "link-name-213: /data/hostObservation/extractor/rawRows/namedObservations/213/observationName의 실제 equals 기대값은 'claim-not-canonical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-213" assertion으로 "link-oracle-213: /data/hostObservation/extractor/rawRows/namedObservations/213/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-214-scenarios" assertion으로 "T17.false-or-unmatched-delivery/claim-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-214-api_response" assertion으로 "T17.false-or-unmatched-delivery/claim-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-214-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/claim-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-214" assertion으로 "T17.false-or-unmatched-delivery/claim-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-214" assertion으로 "link-name-214: /data/hostObservation/extractor/rawRows/namedObservations/214/observationName의 실제 equals 기대값은 'claim-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-214" assertion으로 "link-oracle-214: /data/hostObservation/extractor/rawRows/namedObservations/214/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-215-scenarios" assertion으로 "T17.false-or-unmatched-delivery/no-prior-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-215-api_response" assertion으로 "T17.false-or-unmatched-delivery/no-prior-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-215-db_snapshot" assertion으로 "T17.false-or-unmatched-delivery/no-prior-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-215" assertion으로 "T17.false-or-unmatched-delivery/no-prior-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-215" assertion으로 "link-name-215: /data/hostObservation/extractor/rawRows/namedObservations/215/observationName의 실제 equals 기대값은 'no-prior-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-215" assertion으로 "link-oracle-215: /data/hostObservation/extractor/rawRows/namedObservations/215/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-216-scenarios" assertion으로 "T17.allocation-replacement-no-revival/old-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-216-api_response" assertion으로 "T17.allocation-replacement-no-revival/old-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-216-db_snapshot" assertion으로 "T17.allocation-replacement-no-revival/old-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-216" assertion으로 "T17.allocation-replacement-no-revival/old-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-216" assertion으로 "link-name-216: /data/hostObservation/extractor/rawRows/namedObservations/216/observationName의 실제 equals 기대값은 'old-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-216" assertion으로 "link-oracle-216: /data/hostObservation/extractor/rawRows/namedObservations/216/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-217-scenarios" assertion으로 "T17.allocation-replacement-no-revival/new-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-217-api_response" assertion으로 "T17.allocation-replacement-no-revival/new-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-217-db_snapshot" assertion으로 "T17.allocation-replacement-no-revival/new-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-217" assertion으로 "T17.allocation-replacement-no-revival/new-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-217" assertion으로 "link-name-217: /data/hostObservation/extractor/rawRows/namedObservations/217/observationName의 실제 equals 기대값은 'new-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-217" assertion으로 "link-oracle-217: /data/hostObservation/extractor/rawRows/namedObservations/217/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-218-scenarios" assertion으로 "T17.allocation-replacement-no-revival/executable-reserved-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-218-api_response" assertion으로 "T17.allocation-replacement-no-revival/executable-reserved-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-218-db_snapshot" assertion으로 "T17.allocation-replacement-no-revival/executable-reserved-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-218" assertion으로 "T17.allocation-replacement-no-revival/executable-reserved-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-218" assertion으로 "link-name-218: /data/hostObservation/extractor/rawRows/namedObservations/218/observationName의 실제 equals 기대값은 'executable-reserved-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-218" assertion으로 "link-oracle-218: /data/hostObservation/extractor/rawRows/namedObservations/218/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-219-scenarios" assertion으로 "T17.allocation-replacement-no-revival/old-revival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-219-api_response" assertion으로 "T17.allocation-replacement-no-revival/old-revival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-219-db_snapshot" assertion으로 "T17.allocation-replacement-no-revival/old-revival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-219" assertion으로 "T17.allocation-replacement-no-revival/old-revival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-219" assertion으로 "link-name-219: /data/hostObservation/extractor/rawRows/namedObservations/219/observationName의 실제 equals 기대값은 'old-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-219" assertion으로 "link-oracle-219: /data/hostObservation/extractor/rawRows/namedObservations/219/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-220-scenarios" assertion으로 "T17.allocation-replacement-no-revival/replacement-atomic의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-220-api_response" assertion으로 "T17.allocation-replacement-no-revival/replacement-atomic에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-220-db_snapshot" assertion으로 "T17.allocation-replacement-no-revival/replacement-atomic에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-220" assertion으로 "T17.allocation-replacement-no-revival/replacement-atomic에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-220" assertion으로 "link-name-220: /data/hostObservation/extractor/rawRows/namedObservations/220/observationName의 실제 equals 기대값은 'replacement-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-220" assertion으로 "link-oracle-220: /data/hostObservation/extractor/rawRows/namedObservations/220/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-221-scenarios" assertion으로 "T17.eligibility-fefo-contract/eligibility-response의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-221-api_response" assertion으로 "T17.eligibility-fefo-contract/eligibility-response에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-221-db_snapshot" assertion으로 "T17.eligibility-fefo-contract/eligibility-response에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-221" assertion으로 "T17.eligibility-fefo-contract/eligibility-response에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-221" assertion으로 "link-name-221: /data/hostObservation/extractor/rawRows/namedObservations/221/observationName의 실제 equals 기대값은 'eligibility-response'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-221" assertion으로 "link-oracle-221: /data/hostObservation/extractor/rawRows/namedObservations/221/oracleId의 실제 equals 기대값은 'T17.eligibility-fefo-contract'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-222-scenarios" assertion으로 "T17.eligibility-fefo-contract/unauthorized-packaging-fulfilment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-222-api_response" assertion으로 "T17.eligibility-fefo-contract/unauthorized-packaging-fulfilment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-222-db_snapshot" assertion으로 "T17.eligibility-fefo-contract/unauthorized-packaging-fulfilment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-222" assertion으로 "T17.eligibility-fefo-contract/unauthorized-packaging-fulfilment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-222" assertion으로 "link-name-222: /data/hostObservation/extractor/rawRows/namedObservations/222/observationName의 실제 equals 기대값은 'unauthorized-packaging-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-222" assertion으로 "link-oracle-222: /data/hostObservation/extractor/rawRows/namedObservations/222/oracleId의 실제 equals 기대값은 'T17.eligibility-fefo-contract'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-223-scenarios" assertion으로 "T18.return-new-receipt/historical-delivery의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-223-api_response" assertion으로 "T18.return-new-receipt/historical-delivery에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-223-db_snapshot" assertion으로 "T18.return-new-receipt/historical-delivery에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-223" assertion으로 "T18.return-new-receipt/historical-delivery에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-223" assertion으로 "link-name-223: /data/hostObservation/extractor/rawRows/namedObservations/223/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-223" assertion으로 "link-oracle-223: /data/hostObservation/extractor/rawRows/namedObservations/223/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-224-scenarios" assertion으로 "T18.return-new-receipt/return-received의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-224-api_response" assertion으로 "T18.return-new-receipt/return-received에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-224-db_snapshot" assertion으로 "T18.return-new-receipt/return-received에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-224" assertion으로 "T18.return-new-receipt/return-received에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-224" assertion으로 "link-name-224: /data/hostObservation/extractor/rawRows/namedObservations/224/observationName의 실제 equals 기대값은 'return-received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-224" assertion으로 "link-oracle-224: /data/hostObservation/extractor/rawRows/namedObservations/224/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-225-scenarios" assertion으로 "T18.return-new-receipt/return-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-225-api_response" assertion으로 "T18.return-new-receipt/return-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-225-db_snapshot" assertion으로 "T18.return-new-receipt/return-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-225" assertion으로 "T18.return-new-receipt/return-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-225" assertion으로 "link-name-225: /data/hostObservation/extractor/rawRows/namedObservations/225/observationName의 실제 equals 기대값은 'return-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-225" assertion으로 "link-oracle-225: /data/hostObservation/extractor/rawRows/namedObservations/225/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-226-scenarios" assertion으로 "T18.return-new-receipt/duplicate-return-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-226-api_response" assertion으로 "T18.return-new-receipt/duplicate-return-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-226-db_snapshot" assertion으로 "T18.return-new-receipt/duplicate-return-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-226" assertion으로 "T18.return-new-receipt/duplicate-return-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-226" assertion으로 "link-name-226: /data/hostObservation/extractor/rawRows/namedObservations/226/observationName의 실제 equals 기대값은 'duplicate-return-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-226" assertion으로 "link-oracle-226: /data/hostObservation/extractor/rawRows/namedObservations/226/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-227-scenarios" assertion으로 "T18.return-new-receipt/return-added-purchase-contribution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-227-api_response" assertion으로 "T18.return-new-receipt/return-added-purchase-contribution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-227-db_snapshot" assertion으로 "T18.return-new-receipt/return-added-purchase-contribution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-227" assertion으로 "T18.return-new-receipt/return-added-purchase-contribution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-227" assertion으로 "link-name-227: /data/hostObservation/extractor/rawRows/namedObservations/227/observationName의 실제 equals 기대값은 'return-added-purchase-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-227" assertion으로 "link-oracle-227: /data/hostObservation/extractor/rawRows/namedObservations/227/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-228-scenarios" assertion으로 "T18.return-new-receipt/return-followup의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-228-api_response" assertion으로 "T18.return-new-receipt/return-followup에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-228-db_snapshot" assertion으로 "T18.return-new-receipt/return-followup에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-228" assertion으로 "T18.return-new-receipt/return-followup에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-228" assertion으로 "link-name-228: /data/hostObservation/extractor/rawRows/namedObservations/228/observationName의 실제 equals 기대값은 'return-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-228" assertion으로 "link-oracle-228: /data/hostObservation/extractor/rawRows/namedObservations/228/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-229-scenarios" assertion으로 "T18.trace-and-recall-decisions/bidirectional-trace의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-229-api_response" assertion으로 "T18.trace-and-recall-decisions/bidirectional-trace에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-229-db_snapshot" assertion으로 "T18.trace-and-recall-decisions/bidirectional-trace에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-229" assertion으로 "T18.trace-and-recall-decisions/bidirectional-trace에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-229" assertion으로 "link-name-229: /data/hostObservation/extractor/rawRows/namedObservations/229/observationName의 실제 equals 기대값은 'bidirectional-trace'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-229" assertion으로 "link-oracle-229: /data/hostObservation/extractor/rawRows/namedObservations/229/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-230-scenarios" assertion으로 "T18.trace-and-recall-decisions/unauthorized-recall-decision의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-230-api_response" assertion으로 "T18.trace-and-recall-decisions/unauthorized-recall-decision에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-230-db_snapshot" assertion으로 "T18.trace-and-recall-decisions/unauthorized-recall-decision에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-230" assertion으로 "T18.trace-and-recall-decisions/unauthorized-recall-decision에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-230" assertion으로 "link-name-230: /data/hostObservation/extractor/rawRows/namedObservations/230/observationName의 실제 equals 기대값은 'unauthorized-recall-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-230" assertion으로 "link-oracle-230: /data/hostObservation/extractor/rawRows/namedObservations/230/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-231-scenarios" assertion으로 "T18.trace-and-recall-decisions/approval-notice의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-231-api_response" assertion으로 "T18.trace-and-recall-decisions/approval-notice에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-231-db_snapshot" assertion으로 "T18.trace-and-recall-decisions/approval-notice에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-231" assertion으로 "T18.trace-and-recall-decisions/approval-notice에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-231" assertion으로 "link-name-231: /data/hostObservation/extractor/rawRows/namedObservations/231/observationName의 실제 equals 기대값은 'approval-notice'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-231" assertion으로 "link-oracle-231: /data/hostObservation/extractor/rawRows/namedObservations/231/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-232-scenarios" assertion으로 "T18.recall-closure-accounting/recovery-distinct의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-232-api_response" assertion으로 "T18.recall-closure-accounting/recovery-distinct에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-232-db_snapshot" assertion으로 "T18.recall-closure-accounting/recovery-distinct에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-232" assertion으로 "T18.recall-closure-accounting/recovery-distinct에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-232" assertion으로 "link-name-232: /data/hostObservation/extractor/rawRows/namedObservations/232/observationName의 실제 equals 기대값은 'recovery-distinct'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-232" assertion으로 "link-oracle-232: /data/hostObservation/extractor/rawRows/namedObservations/232/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-233-scenarios" assertion으로 "T18.recall-closure-accounting/processed-distinct의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-233-api_response" assertion으로 "T18.recall-closure-accounting/processed-distinct에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-233-db_snapshot" assertion으로 "T18.recall-closure-accounting/processed-distinct에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-233" assertion으로 "T18.recall-closure-accounting/processed-distinct에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-233" assertion으로 "link-name-233: /data/hostObservation/extractor/rawRows/namedObservations/233/observationName의 실제 equals 기대값은 'processed-distinct'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-233" assertion으로 "link-oracle-233: /data/hostObservation/extractor/rawRows/namedObservations/233/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-234-scenarios" assertion으로 "T18.recall-closure-accounting/unknown의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-234-api_response" assertion으로 "T18.recall-closure-accounting/unknown에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-234-db_snapshot" assertion으로 "T18.recall-closure-accounting/unknown에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-234" assertion으로 "T18.recall-closure-accounting/unknown에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-234" assertion으로 "link-name-234: /data/hostObservation/extractor/rawRows/namedObservations/234/observationName의 실제 equals 기대값은 'unknown'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-234" assertion으로 "link-oracle-234: /data/hostObservation/extractor/rawRows/namedObservations/234/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-235-scenarios" assertion으로 "T18.recall-closure-accounting/false-closure의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-235-api_response" assertion으로 "T18.recall-closure-accounting/false-closure에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-235-db_snapshot" assertion으로 "T18.recall-closure-accounting/false-closure에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-235" assertion으로 "T18.recall-closure-accounting/false-closure에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-235" assertion으로 "link-name-235: /data/hostObservation/extractor/rawRows/namedObservations/235/observationName의 실제 equals 기대값은 'false-closure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-235" assertion으로 "link-oracle-235: /data/hostObservation/extractor/rawRows/namedObservations/235/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-236-scenarios" assertion으로 "T18.recall-closure-accounting/exclusive-endstates의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-236-api_response" assertion으로 "T18.recall-closure-accounting/exclusive-endstates에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-236-db_snapshot" assertion으로 "T18.recall-closure-accounting/exclusive-endstates에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-236" assertion으로 "T18.recall-closure-accounting/exclusive-endstates에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-236" assertion으로 "link-name-236: /data/hostObservation/extractor/rawRows/namedObservations/236/observationName의 실제 equals 기대값은 'exclusive-endstates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-236" assertion으로 "link-oracle-236: /data/hostObservation/extractor/rawRows/namedObservations/236/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-237-scenarios" assertion으로 "T19.invoice-quantity-price-match/logistics-assessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-237-api_response" assertion으로 "T19.invoice-quantity-price-match/logistics-assessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-237-db_snapshot" assertion으로 "T19.invoice-quantity-price-match/logistics-assessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-237" assertion으로 "T19.invoice-quantity-price-match/logistics-assessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-237" assertion으로 "link-name-237: /data/hostObservation/extractor/rawRows/namedObservations/237/observationName의 실제 equals 기대값은 'logistics-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-237" assertion으로 "link-oracle-237: /data/hostObservation/extractor/rawRows/namedObservations/237/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-238-scenarios" assertion으로 "T19.invoice-quantity-price-match/original-invoice-difference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-238-api_response" assertion으로 "T19.invoice-quantity-price-match/original-invoice-difference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-238-db_snapshot" assertion으로 "T19.invoice-quantity-price-match/original-invoice-difference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-238" assertion으로 "T19.invoice-quantity-price-match/original-invoice-difference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-238" assertion으로 "link-name-238: /data/hostObservation/extractor/rawRows/namedObservations/238/observationName의 실제 equals 기대값은 'original-invoice-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-238" assertion으로 "link-oracle-238: /data/hostObservation/extractor/rawRows/namedObservations/238/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-239-scenarios" assertion으로 "T19.invoice-quantity-price-match/settlement-assessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-239-api_response" assertion으로 "T19.invoice-quantity-price-match/settlement-assessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-239-db_snapshot" assertion으로 "T19.invoice-quantity-price-match/settlement-assessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-239" assertion으로 "T19.invoice-quantity-price-match/settlement-assessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-239" assertion으로 "link-name-239: /data/hostObservation/extractor/rawRows/namedObservations/239/observationName의 실제 equals 기대값은 'settlement-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-239" assertion으로 "link-oracle-239: /data/hostObservation/extractor/rawRows/namedObservations/239/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-240-scenarios" assertion으로 "T19.invoice-quantity-price-match/settlement-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-240-api_response" assertion으로 "T19.invoice-quantity-price-match/settlement-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-240-db_snapshot" assertion으로 "T19.invoice-quantity-price-match/settlement-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-240" assertion으로 "T19.invoice-quantity-price-match/settlement-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-240" assertion으로 "link-name-240: /data/hostObservation/extractor/rawRows/namedObservations/240/observationName의 실제 equals 기대값은 'settlement-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-240" assertion으로 "link-oracle-240: /data/hostObservation/extractor/rawRows/namedObservations/240/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-241-scenarios" assertion으로 "T19.invoice-quantity-price-match/matching-links의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-241-api_response" assertion으로 "T19.invoice-quantity-price-match/matching-links에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-241-db_snapshot" assertion으로 "T19.invoice-quantity-price-match/matching-links에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-241" assertion으로 "T19.invoice-quantity-price-match/matching-links에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-241" assertion으로 "link-name-241: /data/hostObservation/extractor/rawRows/namedObservations/241/observationName의 실제 equals 기대값은 'matching-links'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-241" assertion으로 "link-oracle-241: /data/hostObservation/extractor/rawRows/namedObservations/241/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-242-scenarios" assertion으로 "T19.fx-document-payment-reference/original-amount의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-242-api_response" assertion으로 "T19.fx-document-payment-reference/original-amount에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-242-db_snapshot" assertion으로 "T19.fx-document-payment-reference/original-amount에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-242" assertion으로 "T19.fx-document-payment-reference/original-amount에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-242" assertion으로 "link-name-242: /data/hostObservation/extractor/rawRows/namedObservations/242/observationName의 실제 equals 기대값은 'original-amount'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-242" assertion으로 "link-oracle-242: /data/hostObservation/extractor/rawRows/namedObservations/242/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-243-scenarios" assertion으로 "T19.fx-document-payment-reference/converted-amount의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-243-api_response" assertion으로 "T19.fx-document-payment-reference/converted-amount에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-243-db_snapshot" assertion으로 "T19.fx-document-payment-reference/converted-amount에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-243" assertion으로 "T19.fx-document-payment-reference/converted-amount에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-243" assertion으로 "link-name-243: /data/hostObservation/extractor/rawRows/namedObservations/243/observationName의 실제 equals 기대값은 'converted-amount'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-243" assertion으로 "link-oracle-243: /data/hostObservation/extractor/rawRows/namedObservations/243/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-244-scenarios" assertion으로 "T19.fx-document-payment-reference/fx-snapshot의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-244-api_response" assertion으로 "T19.fx-document-payment-reference/fx-snapshot에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-244-db_snapshot" assertion으로 "T19.fx-document-payment-reference/fx-snapshot에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-244" assertion으로 "T19.fx-document-payment-reference/fx-snapshot에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-244" assertion으로 "link-name-244: /data/hostObservation/extractor/rawRows/namedObservations/244/observationName의 실제 equals 기대값은 'fx-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-244" assertion으로 "link-oracle-244: /data/hostObservation/extractor/rawRows/namedObservations/244/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-245-scenarios" assertion으로 "T19.fx-document-payment-reference/commercial-as-domestic-tax-document의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-245-api_response" assertion으로 "T19.fx-document-payment-reference/commercial-as-domestic-tax-document에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-245-db_snapshot" assertion으로 "T19.fx-document-payment-reference/commercial-as-domestic-tax-document에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-245" assertion으로 "T19.fx-document-payment-reference/commercial-as-domestic-tax-document에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-245" assertion으로 "link-name-245: /data/hostObservation/extractor/rawRows/namedObservations/245/observationName의 실제 equals 기대값은 'commercial-as-domestic-tax-document'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-245" assertion으로 "link-oracle-245: /data/hostObservation/extractor/rawRows/namedObservations/245/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-246-scenarios" assertion으로 "T19.fx-document-payment-reference/payment-reference-bank-transfer의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-246-api_response" assertion으로 "T19.fx-document-payment-reference/payment-reference-bank-transfer에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-246-db_snapshot" assertion으로 "T19.fx-document-payment-reference/payment-reference-bank-transfer에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-246" assertion으로 "T19.fx-document-payment-reference/payment-reference-bank-transfer에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-246" assertion으로 "link-name-246: /data/hostObservation/extractor/rawRows/namedObservations/246/observationName의 실제 equals 기대값은 'payment-reference-bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-246" assertion으로 "link-oracle-246: /data/hostObservation/extractor/rawRows/namedObservations/246/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-247-scenarios" assertion으로 "T19.fx-document-payment-reference/automatic-tax-issue-or-submit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-247-api_response" assertion으로 "T19.fx-document-payment-reference/automatic-tax-issue-or-submit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-247-db_snapshot" assertion으로 "T19.fx-document-payment-reference/automatic-tax-issue-or-submit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-247" assertion으로 "T19.fx-document-payment-reference/automatic-tax-issue-or-submit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-247" assertion으로 "link-name-247: /data/hostObservation/extractor/rawRows/namedObservations/247/observationName의 실제 equals 기대값은 'automatic-tax-issue-or-submit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-247" assertion으로 "link-oracle-247: /data/hostObservation/extractor/rawRows/namedObservations/247/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-248-scenarios" assertion으로 "T19.fx-document-payment-reference/qc-pass-payment-authorization의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-248-api_response" assertion으로 "T19.fx-document-payment-reference/qc-pass-payment-authorization에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-248-db_snapshot" assertion으로 "T19.fx-document-payment-reference/qc-pass-payment-authorization에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-248" assertion으로 "T19.fx-document-payment-reference/qc-pass-payment-authorization에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-248" assertion으로 "link-name-248: /data/hostObservation/extractor/rawRows/namedObservations/248/observationName의 실제 equals 기대값은 'qc-pass-payment-authorization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-248" assertion으로 "link-oracle-248: /data/hostObservation/extractor/rawRows/namedObservations/248/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-249-scenarios" assertion으로 "T19.settlement-manager-decision/ordinary-write-confirmed-settlement-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-249-api_response" assertion으로 "T19.settlement-manager-decision/ordinary-write-confirmed-settlement-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-249-db_snapshot" assertion으로 "T19.settlement-manager-decision/ordinary-write-confirmed-settlement-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-249" assertion으로 "T19.settlement-manager-decision/ordinary-write-confirmed-settlement-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-249" assertion으로 "link-name-249: /data/hostObservation/extractor/rawRows/namedObservations/249/observationName의 실제 equals 기대값은 'ordinary-write-confirmed-settlement-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-249" assertion으로 "link-oracle-249: /data/hostObservation/extractor/rawRows/namedObservations/249/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-250-scenarios" assertion으로 "T19.settlement-manager-decision/observations-proposal-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-250-api_response" assertion으로 "T19.settlement-manager-decision/observations-proposal-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-250-db_snapshot" assertion으로 "T19.settlement-manager-decision/observations-proposal-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-250" assertion으로 "T19.settlement-manager-decision/observations-proposal-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-250" assertion으로 "link-name-250: /data/hostObservation/extractor/rawRows/namedObservations/250/observationName의 실제 equals 기대값은 'observations-proposal-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-250" assertion으로 "link-oracle-250: /data/hostObservation/extractor/rawRows/namedObservations/250/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-251-scenarios" assertion으로 "T19.settlement-manager-decision/authorized-confirmation-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-251-api_response" assertion으로 "T19.settlement-manager-decision/authorized-confirmation-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-251-db_snapshot" assertion으로 "T19.settlement-manager-decision/authorized-confirmation-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-251" assertion으로 "T19.settlement-manager-decision/authorized-confirmation-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-251" assertion으로 "link-name-251: /data/hostObservation/extractor/rawRows/namedObservations/251/observationName의 실제 equals 기대값은 'authorized-confirmation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-251" assertion으로 "link-oracle-251: /data/hostObservation/extractor/rawRows/namedObservations/251/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-252-scenarios" assertion으로 "T19.settlement-manager-decision/confirmed-settlement-difference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-252-api_response" assertion으로 "T19.settlement-manager-decision/confirmed-settlement-difference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-252-db_snapshot" assertion으로 "T19.settlement-manager-decision/confirmed-settlement-difference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-252" assertion으로 "T19.settlement-manager-decision/confirmed-settlement-difference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-252" assertion으로 "link-name-252: /data/hostObservation/extractor/rawRows/namedObservations/252/observationName의 실제 equals 기대값은 'confirmed-settlement-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-252" assertion으로 "link-oracle-252: /data/hostObservation/extractor/rawRows/namedObservations/252/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-253-scenarios" assertion으로 "T19.settlement-manager-decision/preserved-original-difference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-253-api_response" assertion으로 "T19.settlement-manager-decision/preserved-original-difference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-253-db_snapshot" assertion으로 "T19.settlement-manager-decision/preserved-original-difference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-253" assertion으로 "T19.settlement-manager-decision/preserved-original-difference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-253" assertion으로 "link-name-253: /data/hostObservation/extractor/rawRows/namedObservations/253/observationName의 실제 equals 기대값은 'preserved-original-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-253" assertion으로 "link-oracle-253: /data/hostObservation/extractor/rawRows/namedObservations/253/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-254-scenarios" assertion으로 "T19.settlement-manager-decision/confirmed-difference-bank-transfer의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-254-api_response" assertion으로 "T19.settlement-manager-decision/confirmed-difference-bank-transfer에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-254-db_snapshot" assertion으로 "T19.settlement-manager-decision/confirmed-difference-bank-transfer에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-254" assertion으로 "T19.settlement-manager-decision/confirmed-difference-bank-transfer에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-254" assertion으로 "link-name-254: /data/hostObservation/extractor/rawRows/namedObservations/254/observationName의 실제 equals 기대값은 'confirmed-difference-bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-254" assertion으로 "link-oracle-254: /data/hostObservation/extractor/rawRows/namedObservations/254/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-255-scenarios" assertion으로 "T19.settlement-manager-decision/management-decision-boundary의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-255-api_response" assertion으로 "T19.settlement-manager-decision/management-decision-boundary에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-255-db_snapshot" assertion으로 "T19.settlement-manager-decision/management-decision-boundary에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-255" assertion으로 "T19.settlement-manager-decision/management-decision-boundary에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-255" assertion으로 "link-name-255: /data/hostObservation/extractor/rawRows/namedObservations/255/observationName의 실제 equals 기대값은 'management-decision-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-255" assertion으로 "link-oracle-255: /data/hostObservation/extractor/rawRows/namedObservations/255/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-256-scenarios" assertion으로 "T20.structured-intent-stages/intent-validation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-256-api_response" assertion으로 "T20.structured-intent-stages/intent-validation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-256-db_snapshot" assertion으로 "T20.structured-intent-stages/intent-validation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-256" assertion으로 "T20.structured-intent-stages/intent-validation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-256" assertion으로 "link-name-256: /data/hostObservation/extractor/rawRows/namedObservations/256/observationName의 실제 equals 기대값은 'intent-validation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-256" assertion으로 "link-oracle-256: /data/hostObservation/extractor/rawRows/namedObservations/256/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-257-scenarios" assertion으로 "T20.structured-intent-stages/input-collection-physical-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-257-api_response" assertion으로 "T20.structured-intent-stages/input-collection-physical-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-257-db_snapshot" assertion으로 "T20.structured-intent-stages/input-collection-physical-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-257" assertion으로 "T20.structured-intent-stages/input-collection-physical-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-257" assertion으로 "link-name-257: /data/hostObservation/extractor/rawRows/namedObservations/257/observationName의 실제 equals 기대값은 'input-collection-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-257" assertion으로 "link-oracle-257: /data/hostObservation/extractor/rawRows/namedObservations/257/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-258-scenarios" assertion으로 "T20.structured-intent-stages/canonicalization의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-258-api_response" assertion으로 "T20.structured-intent-stages/canonicalization에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-258-db_snapshot" assertion으로 "T20.structured-intent-stages/canonicalization에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-258" assertion으로 "T20.structured-intent-stages/canonicalization에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-258" assertion으로 "link-name-258: /data/hostObservation/extractor/rawRows/namedObservations/258/observationName의 실제 equals 기대값은 'canonicalization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-258" assertion으로 "link-oracle-258: /data/hostObservation/extractor/rawRows/namedObservations/258/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-259-mcp" assertion으로 "T20.mcp-stateless-wire/wire-protocol의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-259-scenarios" assertion으로 "T20.mcp-stateless-wire/wire-protocol의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-259-api_response" assertion으로 "T20.mcp-stateless-wire/wire-protocol에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-259-db_snapshot" assertion으로 "T20.mcp-stateless-wire/wire-protocol에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-259" assertion으로 "T20.mcp-stateless-wire/wire-protocol에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-259" assertion으로 "link-name-259: /data/hostObservation/extractor/rawRows/namedObservations/259/observationName의 실제 equals 기대값은 'wire-protocol'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-259" assertion으로 "link-oracle-259: /data/hostObservation/extractor/rawRows/namedObservations/259/oracleId의 실제 equals 기대값은 'T20.mcp-stateless-wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-260-mcp" assertion으로 "T20.mcp-stateless-wire/domain-parity의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-260-scenarios" assertion으로 "T20.mcp-stateless-wire/domain-parity의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-260-api_response" assertion으로 "T20.mcp-stateless-wire/domain-parity에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-260-db_snapshot" assertion으로 "T20.mcp-stateless-wire/domain-parity에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-260" assertion으로 "T20.mcp-stateless-wire/domain-parity에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-260" assertion으로 "link-name-260: /data/hostObservation/extractor/rawRows/namedObservations/260/observationName의 실제 equals 기대값은 'domain-parity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-260" assertion으로 "link-oracle-260: /data/hostObservation/extractor/rawRows/namedObservations/260/oracleId의 실제 equals 기대값은 'T20.mcp-stateless-wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-261-mcp" assertion으로 "T20.mrtr-bound-state/mrtr-state의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-261-scenarios" assertion으로 "T20.mrtr-bound-state/mrtr-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-261-api_response" assertion으로 "T20.mrtr-bound-state/mrtr-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-261-db_snapshot" assertion으로 "T20.mrtr-bound-state/mrtr-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-261" assertion으로 "T20.mrtr-bound-state/mrtr-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-261" assertion으로 "link-name-261: /data/hostObservation/extractor/rawRows/namedObservations/261/observationName의 실제 equals 기대값은 'mrtr-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-261" assertion으로 "link-oracle-261: /data/hostObservation/extractor/rawRows/namedObservations/261/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-262-mcp" assertion으로 "T20.mrtr-bound-state/requeststate-as-approval의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-262-scenarios" assertion으로 "T20.mrtr-bound-state/requeststate-as-approval의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-262-api_response" assertion으로 "T20.mrtr-bound-state/requeststate-as-approval에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-262-db_snapshot" assertion으로 "T20.mrtr-bound-state/requeststate-as-approval에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-262" assertion으로 "T20.mrtr-bound-state/requeststate-as-approval에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-262" assertion으로 "link-name-262: /data/hostObservation/extractor/rawRows/namedObservations/262/observationName의 실제 equals 기대값은 'requeststate-as-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-262" assertion으로 "link-oracle-262: /data/hostObservation/extractor/rawRows/namedObservations/262/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-263-mcp" assertion으로 "T20.mrtr-bound-state/accept-string-as-approval의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-263-scenarios" assertion으로 "T20.mrtr-bound-state/accept-string-as-approval의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-263-api_response" assertion으로 "T20.mrtr-bound-state/accept-string-as-approval에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-263-db_snapshot" assertion으로 "T20.mrtr-bound-state/accept-string-as-approval에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-263" assertion으로 "T20.mrtr-bound-state/accept-string-as-approval에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-263" assertion으로 "link-name-263: /data/hostObservation/extractor/rawRows/namedObservations/263/observationName의 실제 equals 기대값은 'accept-string-as-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-263" assertion으로 "link-oracle-263: /data/hostObservation/extractor/rawRows/namedObservations/263/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-264-mcp" assertion으로 "T20.mrtr-bound-state/invalid-continuation-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-264-scenarios" assertion으로 "T20.mrtr-bound-state/invalid-continuation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-264-api_response" assertion으로 "T20.mrtr-bound-state/invalid-continuation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-264-db_snapshot" assertion으로 "T20.mrtr-bound-state/invalid-continuation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-264" assertion으로 "T20.mrtr-bound-state/invalid-continuation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-264" assertion으로 "link-name-264: /data/hostObservation/extractor/rawRows/namedObservations/264/observationName의 실제 equals 기대값은 'invalid-continuation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-264" assertion으로 "link-oracle-264: /data/hostObservation/extractor/rawRows/namedObservations/264/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-265-mcp" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-265-model" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-265-skills" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-265-skill_loading_trace" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-265-protocol_transcript" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-265-model_transcript" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-265-model_usage_manifest" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-265" assertion으로 "T20.skills-real-loading-and-meaning/skill-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-265" assertion으로 "link-name-265: /data/hostObservation/extractor/rawRows/namedObservations/265/observationName의 실제 equals 기대값은 'skill-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-265" assertion으로 "link-oracle-265: /data/hostObservation/extractor/rawRows/namedObservations/265/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-266-mcp" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-266-model" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-266-skills" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-266-skill_loading_trace" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-266-protocol_transcript" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-266-model_transcript" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-266-model_usage_manifest" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-266" assertion으로 "T20.skills-real-loading-and-meaning/skill-hash-as-loading-proof에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-266" assertion으로 "link-name-266: /data/hostObservation/extractor/rawRows/namedObservations/266/observationName의 실제 equals 기대값은 'skill-hash-as-loading-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-266" assertion으로 "link-oracle-266: /data/hostObservation/extractor/rawRows/namedObservations/266/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-267-mcp" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-267-model" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-267-skills" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-267-skill_loading_trace" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-267-protocol_transcript" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-267-model_transcript" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-267-model_usage_manifest" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-267" assertion으로 "T20.skills-real-loading-and-meaning/allowed-tools-as-server-authorization에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-267" assertion으로 "link-name-267: /data/hostObservation/extractor/rawRows/namedObservations/267/observationName의 실제 equals 기대값은 'allowed-tools-as-server-authorization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-267" assertion으로 "link-oracle-267: /data/hostObservation/extractor/rawRows/namedObservations/267/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-268-mcp" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-268-model" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-268-skills" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-268-skill_loading_trace" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-268-protocol_transcript" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-268-model_transcript" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-268-model_usage_manifest" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-268" assertion으로 "T20.skills-real-loading-and-meaning/document-instruction-authority에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-268" assertion으로 "link-name-268: /data/hostObservation/extractor/rawRows/namedObservations/268/observationName의 실제 equals 기대값은 'document-instruction-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-268" assertion으로 "link-oracle-268: /data/hostObservation/extractor/rawRows/namedObservations/268/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-269-scenarios" assertion으로 "T21.definition-publish-and-impact/definition-lifecycle의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-269-api_response" assertion으로 "T21.definition-publish-and-impact/definition-lifecycle에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-269-db_snapshot" assertion으로 "T21.definition-publish-and-impact/definition-lifecycle에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-269" assertion으로 "T21.definition-publish-and-impact/definition-lifecycle에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-269" assertion으로 "link-name-269: /data/hostObservation/extractor/rawRows/namedObservations/269/observationName의 실제 equals 기대값은 'definition-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-269" assertion으로 "link-oracle-269: /data/hostObservation/extractor/rawRows/namedObservations/269/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-270-scenarios" assertion으로 "T21.definition-publish-and-impact/impact-validation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-270-api_response" assertion으로 "T21.definition-publish-and-impact/impact-validation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-270-db_snapshot" assertion으로 "T21.definition-publish-and-impact/impact-validation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-270" assertion으로 "T21.definition-publish-and-impact/impact-validation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-270" assertion으로 "link-name-270: /data/hostObservation/extractor/rawRows/namedObservations/270/observationName의 실제 equals 기대값은 'impact-validation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-270" assertion으로 "link-oracle-270: /data/hostObservation/extractor/rawRows/namedObservations/270/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-271-scenarios" assertion으로 "T21.definition-publish-and-impact/invalid-payload-published의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-271-api_response" assertion으로 "T21.definition-publish-and-impact/invalid-payload-published에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-271-db_snapshot" assertion으로 "T21.definition-publish-and-impact/invalid-payload-published에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-271" assertion으로 "T21.definition-publish-and-impact/invalid-payload-published에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-271" assertion으로 "link-name-271: /data/hostObservation/extractor/rawRows/namedObservations/271/observationName의 실제 equals 기대값은 'invalid-payload-published'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-271" assertion으로 "link-oracle-271: /data/hostObservation/extractor/rawRows/namedObservations/271/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-272-scenarios" assertion으로 "T21.definition-publish-and-impact/definition-generated-new-capability의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-272-api_response" assertion으로 "T21.definition-publish-and-impact/definition-generated-new-capability에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-272-db_snapshot" assertion으로 "T21.definition-publish-and-impact/definition-generated-new-capability에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-272" assertion으로 "T21.definition-publish-and-impact/definition-generated-new-capability에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-272" assertion으로 "link-name-272: /data/hostObservation/extractor/rawRows/namedObservations/272/observationName의 실제 equals 기대값은 'definition-generated-new-capability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-272" assertion으로 "link-oracle-272: /data/hostObservation/extractor/rawRows/namedObservations/272/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-273-scenarios" assertion으로 "T21.pinned-old-current-policy/old-work-endpoint의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-273-api_response" assertion으로 "T21.pinned-old-current-policy/old-work-endpoint에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-273-db_snapshot" assertion으로 "T21.pinned-old-current-policy/old-work-endpoint에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-273" assertion으로 "T21.pinned-old-current-policy/old-work-endpoint에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-273" assertion으로 "link-name-273: /data/hostObservation/extractor/rawRows/namedObservations/273/observationName의 실제 equals 기대값은 'old-work-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-273" assertion으로 "link-oracle-273: /data/hostObservation/extractor/rawRows/namedObservations/273/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-274-scenarios" assertion으로 "T21.pinned-old-current-policy/new-work-endpoint의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-274-api_response" assertion으로 "T21.pinned-old-current-policy/new-work-endpoint에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-274-db_snapshot" assertion으로 "T21.pinned-old-current-policy/new-work-endpoint에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-274" assertion으로 "T21.pinned-old-current-policy/new-work-endpoint에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-274" assertion으로 "link-name-274: /data/hostObservation/extractor/rawRows/namedObservations/274/observationName의 실제 equals 기대값은 'new-work-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-274" assertion으로 "link-oracle-274: /data/hostObservation/extractor/rawRows/namedObservations/274/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-275-scenarios" assertion으로 "T21.pinned-old-current-policy/current-sell-policy의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-275-api_response" assertion으로 "T21.pinned-old-current-policy/current-sell-policy에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-275-db_snapshot" assertion으로 "T21.pinned-old-current-policy/current-sell-policy에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-275" assertion으로 "T21.pinned-old-current-policy/current-sell-policy에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-275" assertion으로 "link-name-275: /data/hostObservation/extractor/rawRows/namedObservations/275/observationName의 실제 equals 기대값은 'current-sell-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-275" assertion으로 "link-oracle-275: /data/hostObservation/extractor/rawRows/namedObservations/275/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-276-scenarios" assertion으로 "T21.pinned-old-current-policy/v1-used-to-bypass-current-policy의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-276-api_response" assertion으로 "T21.pinned-old-current-policy/v1-used-to-bypass-current-policy에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-276-db_snapshot" assertion으로 "T21.pinned-old-current-policy/v1-used-to-bypass-current-policy에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-276" assertion으로 "T21.pinned-old-current-policy/v1-used-to-bypass-current-policy에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-276" assertion으로 "link-name-276: /data/hostObservation/extractor/rawRows/namedObservations/276/observationName의 실제 equals 기대값은 'v1-used-to-bypass-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-276" assertion으로 "link-oracle-276: /data/hostObservation/extractor/rawRows/namedObservations/276/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-277-scenarios" assertion으로 "T21.pinned-old-current-policy/v1-correction-reassessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-277-api_response" assertion으로 "T21.pinned-old-current-policy/v1-correction-reassessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-277-db_snapshot" assertion으로 "T21.pinned-old-current-policy/v1-correction-reassessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-277" assertion으로 "T21.pinned-old-current-policy/v1-correction-reassessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-277" assertion으로 "link-name-277: /data/hostObservation/extractor/rawRows/namedObservations/277/observationName의 실제 equals 기대값은 'v1-correction-reassessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-277" assertion으로 "link-oracle-277: /data/hostObservation/extractor/rawRows/namedObservations/277/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-278-scenarios" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-278-api_response" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-278-db_snapshot" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-278" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-278" assertion으로 "link-name-278: /data/hostObservation/extractor/rawRows/namedObservations/278/observationName의 실제 equals 기대값은 'unsupported-version-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-278" assertion으로 "link-oracle-278: /data/hostObservation/extractor/rawRows/namedObservations/278/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-279-scenarios" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-279-api_response" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-279-db_snapshot" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-279" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-279" assertion으로 "link-name-279: /data/hostObservation/extractor/rawRows/namedObservations/279/observationName의 실제 equals 기대값은 'unsupported-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-279" assertion으로 "link-oracle-279: /data/hostObservation/extractor/rawRows/namedObservations/279/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-280-scenarios" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-280-api_response" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-280-db_snapshot" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-280" assertion으로 "T21.unsupported-and-explicit-migration/unsupported-version-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-280" assertion으로 "link-name-280: /data/hostObservation/extractor/rawRows/namedObservations/280/observationName의 실제 equals 기대값은 'unsupported-version-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-280" assertion으로 "link-oracle-280: /data/hostObservation/extractor/rawRows/namedObservations/280/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-281-scenarios" assertion으로 "T21.unsupported-and-explicit-migration/explicit-migration의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-281-api_response" assertion으로 "T21.unsupported-and-explicit-migration/explicit-migration에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-281-db_snapshot" assertion으로 "T21.unsupported-and-explicit-migration/explicit-migration에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-281" assertion으로 "T21.unsupported-and-explicit-migration/explicit-migration에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-281" assertion으로 "link-name-281: /data/hostObservation/extractor/rawRows/namedObservations/281/observationName의 실제 equals 기대값은 'explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-281" assertion으로 "link-oracle-281: /data/hostObservation/extractor/rawRows/namedObservations/281/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-282-scenarios" assertion으로 "T21.unsupported-and-explicit-migration/active-pointer-rollback-physical-compensation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-282-api_response" assertion으로 "T21.unsupported-and-explicit-migration/active-pointer-rollback-physical-compensation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-282-db_snapshot" assertion으로 "T21.unsupported-and-explicit-migration/active-pointer-rollback-physical-compensation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-282" assertion으로 "T21.unsupported-and-explicit-migration/active-pointer-rollback-physical-compensation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-282" assertion으로 "link-name-282: /data/hostObservation/extractor/rawRows/namedObservations/282/observationName의 실제 equals 기대값은 'active-pointer-rollback-physical-compensation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-282" assertion으로 "link-oracle-282: /data/hostObservation/extractor/rawRows/namedObservations/282/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-283-scenarios" assertion으로 "T22.source-duplicate-conflict/accepted-inbox-per-key의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-283-api_response" assertion으로 "T22.source-duplicate-conflict/accepted-inbox-per-key에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-283-db_snapshot" assertion으로 "T22.source-duplicate-conflict/accepted-inbox-per-key에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-283" assertion으로 "T22.source-duplicate-conflict/accepted-inbox-per-key에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-283" assertion으로 "link-name-283: /data/hostObservation/extractor/rawRows/namedObservations/283/observationName의 실제 equals 기대값은 'accepted-inbox-per-key'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-283" assertion으로 "link-oracle-283: /data/hostObservation/extractor/rawRows/namedObservations/283/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-284-scenarios" assertion으로 "T22.source-duplicate-conflict/same-key-other-hash의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-284-api_response" assertion으로 "T22.source-duplicate-conflict/same-key-other-hash에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-284-db_snapshot" assertion으로 "T22.source-duplicate-conflict/same-key-other-hash에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-284" assertion으로 "T22.source-duplicate-conflict/same-key-other-hash에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-284" assertion으로 "link-name-284: /data/hostObservation/extractor/rawRows/namedObservations/284/observationName의 실제 equals 기대값은 'same-key-other-hash'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-284" assertion으로 "link-oracle-284: /data/hostObservation/extractor/rawRows/namedObservations/284/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-285-scenarios" assertion으로 "T22.source-duplicate-conflict/canonical-receipt의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-285-api_response" assertion으로 "T22.source-duplicate-conflict/canonical-receipt에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-285-db_snapshot" assertion으로 "T22.source-duplicate-conflict/canonical-receipt에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-285" assertion으로 "T22.source-duplicate-conflict/canonical-receipt에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-285" assertion으로 "link-name-285: /data/hostObservation/extractor/rawRows/namedObservations/285/observationName의 실제 equals 기대값은 'canonical-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-285" assertion으로 "link-oracle-285: /data/hostObservation/extractor/rawRows/namedObservations/285/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-286-scenarios" assertion으로 "T22.source-duplicate-conflict/last-write-wins의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-286-api_response" assertion으로 "T22.source-duplicate-conflict/last-write-wins에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-286-db_snapshot" assertion으로 "T22.source-duplicate-conflict/last-write-wins에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-286" assertion으로 "T22.source-duplicate-conflict/last-write-wins에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-286" assertion으로 "link-name-286: /data/hostObservation/extractor/rawRows/namedObservations/286/observationName의 실제 equals 기대값은 'last-write-wins'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-286" assertion으로 "link-oracle-286: /data/hostObservation/extractor/rawRows/namedObservations/286/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-287-scenarios" assertion으로 "T22.source-duplicate-conflict/identity-unverified-auto-sum의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-287-api_response" assertion으로 "T22.source-duplicate-conflict/identity-unverified-auto-sum에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-287-db_snapshot" assertion으로 "T22.source-duplicate-conflict/identity-unverified-auto-sum에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-287" assertion으로 "T22.source-duplicate-conflict/identity-unverified-auto-sum에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-287" assertion으로 "link-name-287: /data/hostObservation/extractor/rawRows/namedObservations/287/observationName의 실제 equals 기대값은 'identity-unverified-auto-sum'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-287" assertion으로 "link-oracle-287: /data/hostObservation/extractor/rawRows/namedObservations/287/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-288-scenarios" assertion으로 "T22.source-duplicate-conflict/source-conflict-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-288-api_response" assertion으로 "T22.source-duplicate-conflict/source-conflict-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-288-db_snapshot" assertion으로 "T22.source-duplicate-conflict/source-conflict-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-288" assertion으로 "T22.source-duplicate-conflict/source-conflict-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-288" assertion으로 "link-name-288: /data/hostObservation/extractor/rawRows/namedObservations/288/observationName의 실제 equals 기대값은 'source-conflict-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-288" assertion으로 "link-oracle-288: /data/hostObservation/extractor/rawRows/namedObservations/288/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-289-scenarios" assertion으로 "T22.reconciliation-control-and-source-order/invalid-match-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-289-api_response" assertion으로 "T22.reconciliation-control-and-source-order/invalid-match-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-289-db_snapshot" assertion으로 "T22.reconciliation-control-and-source-order/invalid-match-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-289" assertion으로 "T22.reconciliation-control-and-source-order/invalid-match-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-289" assertion으로 "link-name-289: /data/hostObservation/extractor/rawRows/namedObservations/289/observationName의 실제 equals 기대값은 'invalid-match-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-289" assertion으로 "link-oracle-289: /data/hostObservation/extractor/rawRows/namedObservations/289/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-290-scenarios" assertion으로 "T22.reconciliation-control-and-source-order/old-source-overwrites-new-restriction의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-290-api_response" assertion으로 "T22.reconciliation-control-and-source-order/old-source-overwrites-new-restriction에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-290-db_snapshot" assertion으로 "T22.reconciliation-control-and-source-order/old-source-overwrites-new-restriction에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-290" assertion으로 "T22.reconciliation-control-and-source-order/old-source-overwrites-new-restriction에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-290" assertion으로 "link-name-290: /data/hostObservation/extractor/rawRows/namedObservations/290/observationName의 실제 equals 기대값은 'old-source-overwrites-new-restriction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-290" assertion으로 "link-oracle-290: /data/hostObservation/extractor/rawRows/namedObservations/290/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-291-scenarios" assertion으로 "T22.reconciliation-control-and-source-order/unprofiled-connector-activation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-291-api_response" assertion으로 "T22.reconciliation-control-and-source-order/unprofiled-connector-activation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-291-db_snapshot" assertion으로 "T22.reconciliation-control-and-source-order/unprofiled-connector-activation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-291" assertion으로 "T22.reconciliation-control-and-source-order/unprofiled-connector-activation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-291" assertion으로 "link-name-291: /data/hostObservation/extractor/rawRows/namedObservations/291/observationName의 실제 equals 기대값은 'unprofiled-connector-activation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-291" assertion으로 "link-oracle-291: /data/hostObservation/extractor/rawRows/namedObservations/291/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-292-scenarios" assertion으로 "T22.reconciliation-control-and-source-order/source-profile의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-292-api_response" assertion으로 "T22.reconciliation-control-and-source-order/source-profile에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-292-db_snapshot" assertion으로 "T22.reconciliation-control-and-source-order/source-profile에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-292" assertion으로 "T22.reconciliation-control-and-source-order/source-profile에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-292" assertion으로 "link-name-292: /data/hostObservation/extractor/rawRows/namedObservations/292/observationName의 실제 equals 기대값은 'source-profile'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-292" assertion으로 "link-oracle-292: /data/hostObservation/extractor/rawRows/namedObservations/292/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-293-scenarios" assertion으로 "T22.reconciliation-control-and-source-order/quarantine-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-293-api_response" assertion으로 "T22.reconciliation-control-and-source-order/quarantine-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-293-db_snapshot" assertion으로 "T22.reconciliation-control-and-source-order/quarantine-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-293" assertion으로 "T22.reconciliation-control-and-source-order/quarantine-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-293" assertion으로 "link-name-293: /data/hostObservation/extractor/rawRows/namedObservations/293/observationName의 실제 equals 기대값은 'quarantine-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-293" assertion으로 "link-oracle-293: /data/hostObservation/extractor/rawRows/namedObservations/293/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-294-scenarios" assertion으로 "T22.unknown-external-reconciliation/lost-response-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-294-api_response" assertion으로 "T22.unknown-external-reconciliation/lost-response-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-294-db_snapshot" assertion으로 "T22.unknown-external-reconciliation/lost-response-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-294" assertion으로 "T22.unknown-external-reconciliation/lost-response-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-294" assertion으로 "link-name-294: /data/hostObservation/extractor/rawRows/namedObservations/294/observationName의 실제 equals 기대값은 'lost-response-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-294" assertion으로 "link-oracle-294: /data/hostObservation/extractor/rawRows/namedObservations/294/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-295-scenarios" assertion으로 "T22.unknown-external-reconciliation/unreconciled-external-reissue의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-295-api_response" assertion으로 "T22.unknown-external-reconciliation/unreconciled-external-reissue에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-295-db_snapshot" assertion으로 "T22.unknown-external-reconciliation/unreconciled-external-reissue에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-295" assertion으로 "T22.unknown-external-reconciliation/unreconciled-external-reissue에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-295" assertion으로 "link-name-295: /data/hostObservation/extractor/rawRows/namedObservations/295/observationName의 실제 equals 기대값은 'unreconciled-external-reissue'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-295" assertion으로 "link-oracle-295: /data/hostObservation/extractor/rawRows/namedObservations/295/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-296-scenarios" assertion으로 "T22.unknown-external-reconciliation/confirmed-success-reissue의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-296-api_response" assertion으로 "T22.unknown-external-reconciliation/confirmed-success-reissue에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-296-db_snapshot" assertion으로 "T22.unknown-external-reconciliation/confirmed-success-reissue에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-296" assertion으로 "T22.unknown-external-reconciliation/confirmed-success-reissue에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-296" assertion으로 "link-name-296: /data/hostObservation/extractor/rawRows/namedObservations/296/observationName의 실제 equals 기대값은 'confirmed-success-reissue'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-296" assertion으로 "link-oracle-296: /data/hostObservation/extractor/rawRows/namedObservations/296/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-297-scenarios" assertion으로 "T22.unknown-external-reconciliation/external-transition의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-297-api_response" assertion으로 "T22.unknown-external-reconciliation/external-transition에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-297-db_snapshot" assertion으로 "T22.unknown-external-reconciliation/external-transition에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-297" assertion으로 "T22.unknown-external-reconciliation/external-transition에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-297" assertion으로 "link-name-297: /data/hostObservation/extractor/rawRows/namedObservations/297/observationName의 실제 equals 기대값은 'external-transition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-297" assertion으로 "link-oracle-297: /data/hostObservation/extractor/rawRows/namedObservations/297/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-298-scenarios" assertion으로 "T22.unknown-external-reconciliation/external-reconciliation-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-298-api_response" assertion으로 "T22.unknown-external-reconciliation/external-reconciliation-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-298-db_snapshot" assertion으로 "T22.unknown-external-reconciliation/external-reconciliation-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-298" assertion으로 "T22.unknown-external-reconciliation/external-reconciliation-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-298" assertion으로 "link-name-298: /data/hostObservation/extractor/rawRows/namedObservations/298/observationName의 실제 equals 기대값은 'external-reconciliation-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-298" assertion으로 "link-oracle-298: /data/hostObservation/extractor/rawRows/namedObservations/298/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-299-local-deployment" assertion으로 "T23.archive-and-data-branch/archive-restored의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-299-deployment_report" assertion으로 "T23.archive-and-data-branch/archive-restored에는 catalog artifactKind deployment_report의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-299" assertion으로 "T23.archive-and-data-branch/archive-restored에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-299" assertion으로 "link-name-299: /data/hostObservation/extractor/rawRows/namedObservations/299/observationName의 실제 equals 기대값은 'archive-restored'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-299" assertion으로 "link-oracle-299: /data/hostObservation/extractor/rawRows/namedObservations/299/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-300-local-deployment" assertion으로 "T23.archive-and-data-branch/data-branch의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-300-deployment_report" assertion으로 "T23.archive-and-data-branch/data-branch에는 catalog artifactKind deployment_report의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-300" assertion으로 "T23.archive-and-data-branch/data-branch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-300" assertion으로 "link-name-300: /data/hostObservation/extractor/rawRows/namedObservations/300/observationName의 실제 equals 기대값은 'data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-300" assertion으로 "link-oracle-300: /data/hostObservation/extractor/rawRows/namedObservations/300/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-301-local-deployment" assertion으로 "T23.archive-and-data-branch/manufacture-as-import-relabel의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-301-deployment_report" assertion으로 "T23.archive-and-data-branch/manufacture-as-import-relabel에는 catalog artifactKind deployment_report의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-301" assertion으로 "T23.archive-and-data-branch/manufacture-as-import-relabel에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-301" assertion으로 "link-name-301: /data/hostObservation/extractor/rawRows/namedObservations/301/observationName의 실제 equals 기대값은 'manufacture-as-import-relabel'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-301" assertion으로 "link-oracle-301: /data/hostObservation/extractor/rawRows/namedObservations/301/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-302-local-deployment" assertion으로 "T23.schema-ownership-upgrade-drift/schema-owner의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-302-scenarios" assertion으로 "T23.schema-ownership-upgrade-drift/schema-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-302-api_response" assertion으로 "T23.schema-ownership-upgrade-drift/schema-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-302-db_snapshot" assertion으로 "T23.schema-ownership-upgrade-drift/schema-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-302" assertion으로 "T23.schema-ownership-upgrade-drift/schema-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-302" assertion으로 "link-name-302: /data/hostObservation/extractor/rawRows/namedObservations/302/observationName의 실제 equals 기대값은 'schema-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-302" assertion으로 "link-oracle-302: /data/hostObservation/extractor/rawRows/namedObservations/302/oracleId의 실제 equals 기대값은 'T23.schema-ownership-upgrade-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-303-local-deployment" assertion으로 "T23.schema-ownership-upgrade-drift/schema-drift의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-303-scenarios" assertion으로 "T23.schema-ownership-upgrade-drift/schema-drift의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-303-api_response" assertion으로 "T23.schema-ownership-upgrade-drift/schema-drift에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-303-db_snapshot" assertion으로 "T23.schema-ownership-upgrade-drift/schema-drift에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-303" assertion으로 "T23.schema-ownership-upgrade-drift/schema-drift에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-303" assertion으로 "link-name-303: /data/hostObservation/extractor/rawRows/namedObservations/303/observationName의 실제 equals 기대값은 'schema-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-303" assertion으로 "link-oracle-303: /data/hostObservation/extractor/rawRows/namedObservations/303/oracleId의 실제 equals 기대값은 'T23.schema-ownership-upgrade-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-304-local-deployment" assertion으로 "T23.cutover-and-safe-recovery/cutover-order의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-304-scenarios" assertion으로 "T23.cutover-and-safe-recovery/cutover-order의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-304-api_response" assertion으로 "T23.cutover-and-safe-recovery/cutover-order에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-304-db_snapshot" assertion으로 "T23.cutover-and-safe-recovery/cutover-order에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-304" assertion으로 "T23.cutover-and-safe-recovery/cutover-order에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-304" assertion으로 "link-name-304: /data/hostObservation/extractor/rawRows/namedObservations/304/observationName의 실제 equals 기대값은 'cutover-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-304" assertion으로 "link-oracle-304: /data/hostObservation/extractor/rawRows/namedObservations/304/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-305-local-deployment" assertion으로 "T23.cutover-and-safe-recovery/recovery-boundaries의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-305-scenarios" assertion으로 "T23.cutover-and-safe-recovery/recovery-boundaries의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-305-api_response" assertion으로 "T23.cutover-and-safe-recovery/recovery-boundaries에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-305-db_snapshot" assertion으로 "T23.cutover-and-safe-recovery/recovery-boundaries에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-305" assertion으로 "T23.cutover-and-safe-recovery/recovery-boundaries에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-305" assertion으로 "link-name-305: /data/hostObservation/extractor/rawRows/namedObservations/305/observationName의 실제 equals 기대값은 'recovery-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-305" assertion으로 "link-oracle-305: /data/hostObservation/extractor/rawRows/namedObservations/305/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-306-local-deployment" assertion으로 "T23.cutover-and-safe-recovery/SQLRollbackPretendedPhysicalContractCancel의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-306-scenarios" assertion으로 "T23.cutover-and-safe-recovery/SQLRollbackPretendedPhysicalContractCancel의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-306-api_response" assertion으로 "T23.cutover-and-safe-recovery/SQLRollbackPretendedPhysicalContractCancel에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-306-db_snapshot" assertion으로 "T23.cutover-and-safe-recovery/SQLRollbackPretendedPhysicalContractCancel에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-306" assertion으로 "T23.cutover-and-safe-recovery/SQLRollbackPretendedPhysicalContractCancel에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-306" assertion으로 "link-name-306: /data/hostObservation/extractor/rawRows/namedObservations/306/observationName의 실제 equals 기대값은 'SQLRollbackPretendedPhysicalContractCancel'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-306" assertion으로 "link-oracle-306: /data/hostObservation/extractor/rawRows/namedObservations/306/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-307-scenarios" assertion으로 "T24.all-auth-surfaces/cross-org-data-leak의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-307-api_response" assertion으로 "T24.all-auth-surfaces/cross-org-data-leak에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-307-db_snapshot" assertion으로 "T24.all-auth-surfaces/cross-org-data-leak에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-307" assertion으로 "T24.all-auth-surfaces/cross-org-data-leak에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-307" assertion으로 "link-name-307: /data/hostObservation/extractor/rawRows/namedObservations/307/observationName의 실제 equals 기대값은 'cross-org-data-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-307" assertion으로 "link-oracle-307: /data/hostObservation/extractor/rawRows/namedObservations/307/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-308-scenarios" assertion으로 "T24.all-auth-surfaces/forbidden-write-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-308-api_response" assertion으로 "T24.all-auth-surfaces/forbidden-write-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-308-db_snapshot" assertion으로 "T24.all-auth-surfaces/forbidden-write-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-308" assertion으로 "T24.all-auth-surfaces/forbidden-write-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-308" assertion으로 "link-name-308: /data/hostObservation/extractor/rawRows/namedObservations/308/observationName의 실제 equals 기대값은 'forbidden-write-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-308" assertion으로 "link-oracle-308: /data/hostObservation/extractor/rawRows/namedObservations/308/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-309-scenarios" assertion으로 "T24.all-auth-surfaces/surface-auth의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-309-api_response" assertion으로 "T24.all-auth-surfaces/surface-auth에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-309-db_snapshot" assertion으로 "T24.all-auth-surfaces/surface-auth에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-309" assertion으로 "T24.all-auth-surfaces/surface-auth에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-309" assertion으로 "link-name-309: /data/hostObservation/extractor/rawRows/namedObservations/309/observationName의 실제 equals 기대값은 'surface-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-309" assertion으로 "link-oracle-309: /data/hostObservation/extractor/rawRows/namedObservations/309/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-310-scenarios" assertion으로 "T24.audit-failure-rollback/committed-domain-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-310-api_response" assertion으로 "T24.audit-failure-rollback/committed-domain-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-310-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-domain-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-310" assertion으로 "T24.audit-failure-rollback/committed-domain-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-310" assertion으로 "link-name-310: /data/hostObservation/extractor/rawRows/namedObservations/310/observationName의 실제 equals 기대값은 'committed-domain-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-310" assertion으로 "link-oracle-310: /data/hostObservation/extractor/rawRows/namedObservations/310/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-311-scenarios" assertion으로 "T24.audit-failure-rollback/committed-ledger-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-311-api_response" assertion으로 "T24.audit-failure-rollback/committed-ledger-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-311-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-ledger-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-311" assertion으로 "T24.audit-failure-rollback/committed-ledger-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-311" assertion으로 "link-name-311: /data/hostObservation/extractor/rawRows/namedObservations/311/observationName의 실제 equals 기대값은 'committed-ledger-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-311" assertion으로 "link-oracle-311: /data/hostObservation/extractor/rawRows/namedObservations/311/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-312-scenarios" assertion으로 "T24.audit-failure-rollback/committed-allocation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-312-api_response" assertion으로 "T24.audit-failure-rollback/committed-allocation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-312-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-allocation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-312" assertion으로 "T24.audit-failure-rollback/committed-allocation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-312" assertion으로 "link-name-312: /data/hostObservation/extractor/rawRows/namedObservations/312/observationName의 실제 equals 기대값은 'committed-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-312" assertion으로 "link-oracle-312: /data/hostObservation/extractor/rawRows/namedObservations/312/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-313-scenarios" assertion으로 "T24.audit-failure-rollback/committed-duty-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-313-api_response" assertion으로 "T24.audit-failure-rollback/committed-duty-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-313-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-duty-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-313" assertion으로 "T24.audit-failure-rollback/committed-duty-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-313" assertion으로 "link-name-313: /data/hostObservation/extractor/rawRows/namedObservations/313/observationName의 실제 equals 기대값은 'committed-duty-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-313" assertion으로 "link-oracle-313: /data/hostObservation/extractor/rawRows/namedObservations/313/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-314-scenarios" assertion으로 "T24.audit-failure-rollback/committed-outbox-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-314-api_response" assertion으로 "T24.audit-failure-rollback/committed-outbox-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-314-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-outbox-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-314" assertion으로 "T24.audit-failure-rollback/committed-outbox-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-314" assertion으로 "link-name-314: /data/hostObservation/extractor/rawRows/namedObservations/314/observationName의 실제 equals 기대값은 'committed-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-314" assertion으로 "link-oracle-314: /data/hostObservation/extractor/rawRows/namedObservations/314/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-315-scenarios" assertion으로 "T24.audit-failure-rollback/committed-idempotency-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-315-api_response" assertion으로 "T24.audit-failure-rollback/committed-idempotency-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-315-db_snapshot" assertion으로 "T24.audit-failure-rollback/committed-idempotency-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-315" assertion으로 "T24.audit-failure-rollback/committed-idempotency-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-315" assertion으로 "link-name-315: /data/hostObservation/extractor/rawRows/namedObservations/315/observationName의 실제 equals 기대값은 'committed-idempotency-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-315" assertion으로 "link-oracle-315: /data/hostObservation/extractor/rawRows/namedObservations/315/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-316-scenarios" assertion으로 "T24.audit-failure-rollback/audit-fields의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-316-api_response" assertion으로 "T24.audit-failure-rollback/audit-fields에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-316-db_snapshot" assertion으로 "T24.audit-failure-rollback/audit-fields에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-316" assertion으로 "T24.audit-failure-rollback/audit-fields에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-316" assertion으로 "link-name-316: /data/hostObservation/extractor/rawRows/namedObservations/316/observationName의 실제 equals 기대값은 'audit-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-316" assertion으로 "link-oracle-316: /data/hostObservation/extractor/rawRows/namedObservations/316/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-317-scenarios" assertion으로 "T24.retention-legalhold-blob-restore/legal-hold-delete의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-317-api_response" assertion으로 "T24.retention-legalhold-blob-restore/legal-hold-delete에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-317-db_snapshot" assertion으로 "T24.retention-legalhold-blob-restore/legal-hold-delete에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-317" assertion으로 "T24.retention-legalhold-blob-restore/legal-hold-delete에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-317" assertion으로 "link-name-317: /data/hostObservation/extractor/rawRows/namedObservations/317/observationName의 실제 equals 기대값은 'legal-hold-delete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-317" assertion으로 "link-oracle-317: /data/hostObservation/extractor/rawRows/namedObservations/317/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-318-scenarios" assertion으로 "T24.retention-legalhold-blob-restore/unresolved-reference-delete의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-318-api_response" assertion으로 "T24.retention-legalhold-blob-restore/unresolved-reference-delete에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-318-db_snapshot" assertion으로 "T24.retention-legalhold-blob-restore/unresolved-reference-delete에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-318" assertion으로 "T24.retention-legalhold-blob-restore/unresolved-reference-delete에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-318" assertion으로 "link-name-318: /data/hostObservation/extractor/rawRows/namedObservations/318/observationName의 실제 equals 기대값은 'unresolved-reference-delete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-318" assertion으로 "link-oracle-318: /data/hostObservation/extractor/rawRows/namedObservations/318/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-319-scenarios" assertion으로 "T24.retention-legalhold-blob-restore/retention-policy의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-319-api_response" assertion으로 "T24.retention-legalhold-blob-restore/retention-policy에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-319-db_snapshot" assertion으로 "T24.retention-legalhold-blob-restore/retention-policy에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-319" assertion으로 "T24.retention-legalhold-blob-restore/retention-policy에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-319" assertion으로 "link-name-319: /data/hostObservation/extractor/rawRows/namedObservations/319/observationName의 실제 equals 기대값은 'retention-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-319" assertion으로 "link-oracle-319: /data/hostObservation/extractor/rawRows/namedObservations/319/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-320-scenarios" assertion으로 "T24.secret-redaction/secret-sentinel-leak의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-320-api_response" assertion으로 "T24.secret-redaction/secret-sentinel-leak에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-320-db_snapshot" assertion으로 "T24.secret-redaction/secret-sentinel-leak에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-320" assertion으로 "T24.secret-redaction/secret-sentinel-leak에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-320" assertion으로 "link-name-320: /data/hostObservation/extractor/rawRows/namedObservations/320/observationName의 실제 equals 기대값은 'secret-sentinel-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-320" assertion으로 "link-oracle-320: /data/hostObservation/extractor/rawRows/namedObservations/320/oracleId의 실제 equals 기대값은 'T24.secret-redaction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-321-scenarios" assertion으로 "T24.secret-redaction/redaction-purpose의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-321-api_response" assertion으로 "T24.secret-redaction/redaction-purpose에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-321-db_snapshot" assertion으로 "T24.secret-redaction/redaction-purpose에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-321" assertion으로 "T24.secret-redaction/redaction-purpose에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-321" assertion으로 "link-name-321: /data/hostObservation/extractor/rawRows/namedObservations/321/observationName의 실제 equals 기대값은 'redaction-purpose'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-321" assertion으로 "link-oracle-321: /data/hostObservation/extractor/rawRows/namedObservations/321/oracleId의 실제 equals 기대값은 'T24.secret-redaction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-322" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-322" assertion으로 "link-name-322: /data/hostObservation/extractor/rawRows/namedObservations/322/observationName의 실제 equals 기대값은 'coverage-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-322" assertion으로 "link-oracle-322: /data/hostObservation/extractor/rawRows/namedObservations/322/oracleId의 실제 equals 기대값은 'T25.independent-traceability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-323" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-323" assertion으로 "link-name-323: /data/hostObservation/extractor/rawRows/namedObservations/323/observationName의 실제 equals 기대값은 'result-separation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-323" assertion으로 "link-oracle-323: /data/hostObservation/extractor/rawRows/namedObservations/323/oracleId의 실제 equals 기대값은 'T25.independent-traceability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-324" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-324" assertion으로 "link-name-324: /data/hostObservation/extractor/rawRows/namedObservations/324/observationName의 실제 equals 기대값은 'evidence-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-324" assertion으로 "link-oracle-324: /data/hostObservation/extractor/rawRows/namedObservations/324/oracleId의 실제 equals 기대값은 'T25.evidence-manifest-and-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-325" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-325" assertion으로 "link-name-325: /data/hostObservation/extractor/rawRows/namedObservations/325/observationName의 실제 equals 기대값은 'wrapper-truth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-325" assertion으로 "link-oracle-325: /data/hostObservation/extractor/rawRows/namedObservations/325/oracleId의 실제 equals 기대값은 'T25.evidence-manifest-and-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-326" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-326" assertion으로 "link-name-326: /data/hostObservation/extractor/rawRows/namedObservations/326/observationName의 실제 equals 기대값은 'corpus-size'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-326" assertion으로 "link-oracle-326: /data/hostObservation/extractor/rawRows/namedObservations/326/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-327" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-327" assertion으로 "link-name-327: /data/hostObservation/extractor/rawRows/namedObservations/327/observationName의 실제 equals 기대값은 'proposed-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-327" assertion으로 "link-oracle-327: /data/hostObservation/extractor/rawRows/namedObservations/327/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-328" assertion으로 "현재 실행 중인 T25 자신의 observation은 CURRENT_EXECUTION link로만 연결하고 PASS를 선행 조건으로 요구하지 않는다."를 확인한다
    그러면 "link-name-328" assertion으로 "link-name-328: /data/hostObservation/extractor/rawRows/namedObservations/328/observationName의 실제 equals 기대값은 'usage-and-version-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-328" assertion으로 "link-oracle-328: /data/hostObservation/extractor/rawRows/namedObservations/328/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-329-scenarios" assertion으로 "T26.scheduler-recovery-and-operations/recovery-rediscovery의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-329-api_response" assertion으로 "T26.scheduler-recovery-and-operations/recovery-rediscovery에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-329-db_snapshot" assertion으로 "T26.scheduler-recovery-and-operations/recovery-rediscovery에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-329" assertion으로 "T26.scheduler-recovery-and-operations/recovery-rediscovery에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-329" assertion으로 "link-name-329: /data/hostObservation/extractor/rawRows/namedObservations/329/observationName의 실제 equals 기대값은 'recovery-rediscovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-329" assertion으로 "link-oracle-329: /data/hostObservation/extractor/rawRows/namedObservations/329/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-330-scenarios" assertion으로 "T26.scheduler-recovery-and-operations/operational-results의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-330-api_response" assertion으로 "T26.scheduler-recovery-and-operations/operational-results에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-330-db_snapshot" assertion으로 "T26.scheduler-recovery-and-operations/operational-results에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-330" assertion으로 "T26.scheduler-recovery-and-operations/operational-results에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-330" assertion으로 "link-name-330: /data/hostObservation/extractor/rawRows/namedObservations/330/observationName의 실제 equals 기대값은 'operational-results'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-330" assertion으로 "link-oracle-330: /data/hostObservation/extractor/rawRows/namedObservations/330/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-331-scenarios" assertion으로 "T26.scheduler-recovery-and-operations/notification-resolves-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-331-api_response" assertion으로 "T26.scheduler-recovery-and-operations/notification-resolves-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-331-db_snapshot" assertion으로 "T26.scheduler-recovery-and-operations/notification-resolves-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-331" assertion으로 "T26.scheduler-recovery-and-operations/notification-resolves-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-331" assertion으로 "link-name-331: /data/hostObservation/extractor/rawRows/namedObservations/331/observationName의 실제 equals 기대값은 'notification-resolves-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-331" assertion으로 "link-oracle-331: /data/hostObservation/extractor/rawRows/namedObservations/331/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-332-scenarios" assertion으로 "T26.boundary-index-and-emergency-repair/boundary-index의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-332-api_response" assertion으로 "T26.boundary-index-and-emergency-repair/boundary-index에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-332-db_snapshot" assertion으로 "T26.boundary-index-and-emergency-repair/boundary-index에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-332" assertion으로 "T26.boundary-index-and-emergency-repair/boundary-index에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-332" assertion으로 "link-name-332: /data/hostObservation/extractor/rawRows/namedObservations/332/observationName의 실제 equals 기대값은 'boundary-index'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-332" assertion으로 "link-oracle-332: /data/hostObservation/extractor/rawRows/namedObservations/332/oracleId의 실제 equals 기대값은 'T26.boundary-index-and-emergency-repair'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-333-scenarios" assertion으로 "T26.boundary-index-and-emergency-repair/safe-runbook의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-333-api_response" assertion으로 "T26.boundary-index-and-emergency-repair/safe-runbook에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-333-db_snapshot" assertion으로 "T26.boundary-index-and-emergency-repair/safe-runbook에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-333" assertion으로 "T26.boundary-index-and-emergency-repair/safe-runbook에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-333" assertion으로 "link-name-333: /data/hostObservation/extractor/rawRows/namedObservations/333/observationName의 실제 equals 기대값은 'safe-runbook'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-333" assertion으로 "link-oracle-333: /data/hostObservation/extractor/rawRows/namedObservations/333/oracleId의 실제 equals 기대값은 'T26.boundary-index-and-emergency-repair'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-334-local-deployment" assertion으로 "T26.backup-complete-restore/restore-verification의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-334-scenarios" assertion으로 "T26.backup-complete-restore/restore-verification의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-334-api_response" assertion으로 "T26.backup-complete-restore/restore-verification에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-334-db_snapshot" assertion으로 "T26.backup-complete-restore/restore-verification에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-334" assertion으로 "T26.backup-complete-restore/restore-verification에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-334" assertion으로 "link-name-334: /data/hostObservation/extractor/rawRows/namedObservations/334/observationName의 실제 equals 기대값은 'restore-verification'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-334" assertion으로 "link-oracle-334: /data/hostObservation/extractor/rawRows/namedObservations/334/oracleId의 실제 equals 기대값은 'T26.backup-complete-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-335-scenarios" assertion으로 "C1.initial-eligibility/held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-335-api_response" assertion으로 "C1.initial-eligibility/held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-335-db_snapshot" assertion으로 "C1.initial-eligibility/held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-335" assertion으로 "C1.initial-eligibility/held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-335" assertion으로 "link-name-335: /data/hostObservation/extractor/rawRows/namedObservations/335/observationName의 실제 equals 기대값은 'held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-335" assertion으로 "link-oracle-335: /data/hostObservation/extractor/rawRows/namedObservations/335/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-336-scenarios" assertion으로 "C1.initial-eligibility/sell-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-336-api_response" assertion으로 "C1.initial-eligibility/sell-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-336-db_snapshot" assertion으로 "C1.initial-eligibility/sell-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-336" assertion으로 "C1.initial-eligibility/sell-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-336" assertion으로 "link-name-336: /data/hostObservation/extractor/rawRows/namedObservations/336/observationName의 실제 equals 기대값은 'sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-336" assertion으로 "link-oracle-336: /data/hostObservation/extractor/rawRows/namedObservations/336/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-337-scenarios" assertion으로 "C1.initial-eligibility/unreserved-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-337-api_response" assertion으로 "C1.initial-eligibility/unreserved-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-337-db_snapshot" assertion으로 "C1.initial-eligibility/unreserved-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-337" assertion으로 "C1.initial-eligibility/unreserved-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-337" assertion으로 "link-name-337: /data/hostObservation/extractor/rawRows/namedObservations/337/observationName의 실제 equals 기대값은 'unreserved-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-337" assertion으로 "link-oracle-337: /data/hostObservation/extractor/rawRows/namedObservations/337/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-338-scenarios" assertion으로 "C1.initial-eligibility/customer60-added-by-qc-or-app-write의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-338-api_response" assertion으로 "C1.initial-eligibility/customer60-added-by-qc-or-app-write에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-338-db_snapshot" assertion으로 "C1.initial-eligibility/customer60-added-by-qc-or-app-write에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-338" assertion으로 "C1.initial-eligibility/customer60-added-by-qc-or-app-write에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-338" assertion으로 "link-name-338: /data/hostObservation/extractor/rawRows/namedObservations/338/observationName의 실제 equals 기대값은 'customer60-added-by-qc-or-app-write'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-338" assertion으로 "link-oracle-338: /data/hostObservation/extractor/rawRows/namedObservations/338/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-339-scenarios" assertion으로 "C1.revoked-disposition/new-reservation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-339-api_response" assertion으로 "C1.revoked-disposition/new-reservation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-339-db_snapshot" assertion으로 "C1.revoked-disposition/new-reservation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-339" assertion으로 "C1.revoked-disposition/new-reservation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-339" assertion으로 "link-name-339: /data/hostObservation/extractor/rawRows/namedObservations/339/observationName의 실제 equals 기대값은 'new-reservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-339" assertion으로 "link-oracle-339: /data/hostObservation/extractor/rawRows/namedObservations/339/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-340-scenarios" assertion으로 "C1.revoked-disposition/new-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-340-api_response" assertion으로 "C1.revoked-disposition/new-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-340-db_snapshot" assertion으로 "C1.revoked-disposition/new-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-340" assertion으로 "C1.revoked-disposition/new-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-340" assertion으로 "link-name-340: /data/hostObservation/extractor/rawRows/namedObservations/340/observationName의 실제 equals 기대값은 'new-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-340" assertion으로 "link-oracle-340: /data/hostObservation/extractor/rawRows/namedObservations/340/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-341-scenarios" assertion으로 "C1.revoked-disposition/existing-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-341-api_response" assertion으로 "C1.revoked-disposition/existing-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-341-db_snapshot" assertion으로 "C1.revoked-disposition/existing-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-341" assertion으로 "C1.revoked-disposition/existing-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-341" assertion으로 "link-name-341: /data/hostObservation/extractor/rawRows/namedObservations/341/observationName의 실제 equals 기대값은 'existing-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-341" assertion으로 "link-oracle-341: /data/hostObservation/extractor/rawRows/namedObservations/341/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-342-scenarios" assertion으로 "C1.revoked-disposition/existing-obligation-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-342-api_response" assertion으로 "C1.revoked-disposition/existing-obligation-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-342-db_snapshot" assertion으로 "C1.revoked-disposition/existing-obligation-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-342" assertion으로 "C1.revoked-disposition/existing-obligation-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-342" assertion으로 "link-name-342: /data/hostObservation/extractor/rawRows/namedObservations/342/observationName의 실제 equals 기대값은 'existing-obligation-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-342" assertion으로 "link-oracle-342: /data/hostObservation/extractor/rawRows/namedObservations/342/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-343-scenarios" assertion으로 "C1.revoked-disposition/revocation-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-343-api_response" assertion으로 "C1.revoked-disposition/revocation-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-343-db_snapshot" assertion으로 "C1.revoked-disposition/revocation-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-343" assertion으로 "C1.revoked-disposition/revocation-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-343" assertion으로 "link-name-343: /data/hostObservation/extractor/rawRows/namedObservations/343/observationName의 실제 equals 기대값은 'revocation-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-343" assertion으로 "link-oracle-343: /data/hostObservation/extractor/rawRows/namedObservations/343/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-344-scenarios" assertion으로 "C2.cumulative-vs-state/cumulative-arrival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-344-api_response" assertion으로 "C2.cumulative-vs-state/cumulative-arrival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-344-db_snapshot" assertion으로 "C2.cumulative-vs-state/cumulative-arrival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-344" assertion으로 "C2.cumulative-vs-state/cumulative-arrival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-344" assertion으로 "link-name-344: /data/hostObservation/extractor/rawRows/namedObservations/344/observationName의 실제 equals 기대값은 'cumulative-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-344" assertion으로 "link-oracle-344: /data/hostObservation/extractor/rawRows/namedObservations/344/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-345-scenarios" assertion으로 "C2.cumulative-vs-state/state-at-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-345-api_response" assertion으로 "C2.cumulative-vs-state/state-at-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-345-db_snapshot" assertion으로 "C2.cumulative-vs-state/state-at-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-345" assertion으로 "C2.cumulative-vs-state/state-at-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-345" assertion으로 "link-name-345: /data/hostObservation/extractor/rawRows/namedObservations/345/observationName의 실제 equals 기대값은 'state-at-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-345" assertion으로 "link-oracle-345: /data/hostObservation/extractor/rawRows/namedObservations/345/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-346-scenarios" assertion으로 "C2.cumulative-vs-state/cumulative의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-346-api_response" assertion으로 "C2.cumulative-vs-state/cumulative에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-346-db_snapshot" assertion으로 "C2.cumulative-vs-state/cumulative에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-346" assertion으로 "C2.cumulative-vs-state/cumulative에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-346" assertion으로 "link-name-346: /data/hostObservation/extractor/rawRows/namedObservations/346/observationName의 실제 equals 기대값은 'cumulative'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-346" assertion으로 "link-oracle-346: /data/hostObservation/extractor/rawRows/namedObservations/346/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-347-scenarios" assertion으로 "C2.cumulative-vs-state/state-at의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-347-api_response" assertion으로 "C2.cumulative-vs-state/state-at에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-347-db_snapshot" assertion으로 "C2.cumulative-vs-state/state-at에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-347" assertion으로 "C2.cumulative-vs-state/state-at에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-347" assertion으로 "link-name-347: /data/hostObservation/extractor/rawRows/namedObservations/347/observationName의 실제 equals 기대값은 'state-at'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-347" assertion으로 "link-oracle-347: /data/hostObservation/extractor/rawRows/namedObservations/347/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-348-scenarios" assertion으로 "C2.cumulative-vs-state/mode-evidence-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-348-api_response" assertion으로 "C2.cumulative-vs-state/mode-evidence-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-348-db_snapshot" assertion으로 "C2.cumulative-vs-state/mode-evidence-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-348" assertion으로 "C2.cumulative-vs-state/mode-evidence-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-348" assertion으로 "link-name-348: /data/hostObservation/extractor/rawRows/namedObservations/348/observationName의 실제 equals 기대값은 'mode-evidence-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-348" assertion으로 "link-oracle-348: /data/hostObservation/extractor/rawRows/namedObservations/348/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-349-mcp" assertion으로 "C3.read-grant-all-writes/work-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-349-scenarios" assertion으로 "C3.read-grant-all-writes/work-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-349-api_response" assertion으로 "C3.read-grant-all-writes/work-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-349-db_snapshot" assertion으로 "C3.read-grant-all-writes/work-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-349" assertion으로 "C3.read-grant-all-writes/work-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-349" assertion으로 "link-name-349: /data/hostObservation/extractor/rawRows/namedObservations/349/observationName의 실제 equals 기대값은 'work-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-349" assertion으로 "link-oracle-349: /data/hostObservation/extractor/rawRows/namedObservations/349/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-350-mcp" assertion으로 "C3.read-grant-all-writes/inventory-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-350-scenarios" assertion으로 "C3.read-grant-all-writes/inventory-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-350-api_response" assertion으로 "C3.read-grant-all-writes/inventory-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-350-db_snapshot" assertion으로 "C3.read-grant-all-writes/inventory-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-350" assertion으로 "C3.read-grant-all-writes/inventory-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-350" assertion으로 "link-name-350: /data/hostObservation/extractor/rawRows/namedObservations/350/observationName의 실제 equals 기대값은 'inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-350" assertion으로 "link-oracle-350: /data/hostObservation/extractor/rawRows/namedObservations/350/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-351-mcp" assertion으로 "C3.read-grant-all-writes/followup-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-351-scenarios" assertion으로 "C3.read-grant-all-writes/followup-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-351-api_response" assertion으로 "C3.read-grant-all-writes/followup-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-351-db_snapshot" assertion으로 "C3.read-grant-all-writes/followup-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-351" assertion으로 "C3.read-grant-all-writes/followup-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-351" assertion으로 "link-name-351: /data/hostObservation/extractor/rawRows/namedObservations/351/observationName의 실제 equals 기대값은 'followup-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-351" assertion으로 "link-oracle-351: /data/hostObservation/extractor/rawRows/namedObservations/351/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-352-mcp" assertion으로 "C3.read-grant-all-writes/allocation-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-352-scenarios" assertion으로 "C3.read-grant-all-writes/allocation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-352-api_response" assertion으로 "C3.read-grant-all-writes/allocation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-352-db_snapshot" assertion으로 "C3.read-grant-all-writes/allocation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-352" assertion으로 "C3.read-grant-all-writes/allocation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-352" assertion으로 "link-name-352: /data/hostObservation/extractor/rawRows/namedObservations/352/observationName의 실제 equals 기대값은 'allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-352" assertion으로 "link-oracle-352: /data/hostObservation/extractor/rawRows/namedObservations/352/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-353-mcp" assertion으로 "C3.read-grant-all-writes/approval-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-353-scenarios" assertion으로 "C3.read-grant-all-writes/approval-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-353-api_response" assertion으로 "C3.read-grant-all-writes/approval-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-353-db_snapshot" assertion으로 "C3.read-grant-all-writes/approval-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-353" assertion으로 "C3.read-grant-all-writes/approval-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-353" assertion으로 "link-name-353: /data/hostObservation/extractor/rawRows/namedObservations/353/observationName의 실제 equals 기대값은 'approval-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-353" assertion으로 "link-oracle-353: /data/hostObservation/extractor/rawRows/namedObservations/353/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-354-mcp" assertion으로 "C3.read-grant-all-writes/external-outbox-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-354-scenarios" assertion으로 "C3.read-grant-all-writes/external-outbox-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-354-api_response" assertion으로 "C3.read-grant-all-writes/external-outbox-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-354-db_snapshot" assertion으로 "C3.read-grant-all-writes/external-outbox-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-354" assertion으로 "C3.read-grant-all-writes/external-outbox-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-354" assertion으로 "link-name-354: /data/hostObservation/extractor/rawRows/namedObservations/354/observationName의 실제 equals 기대값은 'external-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-354" assertion으로 "link-oracle-354: /data/hostObservation/extractor/rawRows/namedObservations/354/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-355-mcp" assertion으로 "C3.read-grant-all-writes/read-audit-permitted의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-355-scenarios" assertion으로 "C3.read-grant-all-writes/read-audit-permitted의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-355-api_response" assertion으로 "C3.read-grant-all-writes/read-audit-permitted에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-355-db_snapshot" assertion으로 "C3.read-grant-all-writes/read-audit-permitted에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-355" assertion으로 "C3.read-grant-all-writes/read-audit-permitted에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-355" assertion으로 "link-name-355: /data/hostObservation/extractor/rawRows/namedObservations/355/observationName의 실제 equals 기대값은 'read-audit-permitted'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-355" assertion으로 "link-oracle-355: /data/hostObservation/extractor/rawRows/namedObservations/355/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-356-mcp" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-356-model" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-356-skills" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-356-model_transcript" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-356-model_usage_manifest" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-356-protocol_transcript" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-356-skill_loading_trace" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-356" assertion으로 "C3.model-query-write-boundary/query-intent-write-tool-execution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-356" assertion으로 "link-name-356: /data/hostObservation/extractor/rawRows/namedObservations/356/observationName의 실제 equals 기대값은 'query-intent-write-tool-execution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-356" assertion으로 "link-oracle-356: /data/hostObservation/extractor/rawRows/namedObservations/356/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-357-mcp" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-357-model" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-357-skills" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-357-model_transcript" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-357-model_usage_manifest" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-357-protocol_transcript" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-357-skill_loading_trace" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-357" assertion으로 "C3.model-query-write-boundary/query-intent-business-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-357" assertion으로 "link-name-357: /data/hostObservation/extractor/rawRows/namedObservations/357/observationName의 실제 equals 기대값은 'query-intent-business-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-357" assertion으로 "link-oracle-357: /data/hostObservation/extractor/rawRows/namedObservations/357/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-358-mcp" assertion으로 "C3.model-query-write-boundary/evaluation-path의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-358-model" assertion으로 "C3.model-query-write-boundary/evaluation-path의 필수 계층 model profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-358-skills" assertion으로 "C3.model-query-write-boundary/evaluation-path의 필수 계층 skills profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-358-model_transcript" assertion으로 "C3.model-query-write-boundary/evaluation-path에는 catalog artifactKind model_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-358-model_usage_manifest" assertion으로 "C3.model-query-write-boundary/evaluation-path에는 catalog artifactKind model_usage_manifest의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-358-protocol_transcript" assertion으로 "C3.model-query-write-boundary/evaluation-path에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-358-skill_loading_trace" assertion으로 "C3.model-query-write-boundary/evaluation-path에는 catalog artifactKind skill_loading_trace의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-358" assertion으로 "C3.model-query-write-boundary/evaluation-path에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-358" assertion으로 "link-name-358: /data/hostObservation/extractor/rawRows/namedObservations/358/observationName의 실제 equals 기대값은 'evaluation-path'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-358" assertion으로 "link-oracle-358: /data/hostObservation/extractor/rawRows/namedObservations/358/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-359-scenarios" assertion으로 "C4.return-not-correction/historical-delivery의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-359-api_response" assertion으로 "C4.return-not-correction/historical-delivery에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-359-db_snapshot" assertion으로 "C4.return-not-correction/historical-delivery에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-359" assertion으로 "C4.return-not-correction/historical-delivery에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-359" assertion으로 "link-name-359: /data/hostObservation/extractor/rawRows/namedObservations/359/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-359" assertion으로 "link-oracle-359: /data/hostObservation/extractor/rawRows/namedObservations/359/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-360-scenarios" assertion으로 "C4.return-not-correction/new-return의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-360-api_response" assertion으로 "C4.return-not-correction/new-return에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-360-db_snapshot" assertion으로 "C4.return-not-correction/new-return에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-360" assertion으로 "C4.return-not-correction/new-return에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-360" assertion으로 "link-name-360: /data/hostObservation/extractor/rawRows/namedObservations/360/observationName의 실제 equals 기대값은 'new-return'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-360" assertion으로 "link-oracle-360: /data/hostObservation/extractor/rawRows/namedObservations/360/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-361-scenarios" assertion으로 "C4.return-not-correction/duplicate-return-material-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-361-api_response" assertion으로 "C4.return-not-correction/duplicate-return-material-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-361-db_snapshot" assertion으로 "C4.return-not-correction/duplicate-return-material-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-361" assertion으로 "C4.return-not-correction/duplicate-return-material-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-361" assertion으로 "link-name-361: /data/hostObservation/extractor/rawRows/namedObservations/361/observationName의 실제 equals 기대값은 'duplicate-return-material-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-361" assertion으로 "link-oracle-361: /data/hostObservation/extractor/rawRows/namedObservations/361/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-362-scenarios" assertion으로 "C4.return-not-correction/return-overwrites-delivery-to80의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-362-api_response" assertion으로 "C4.return-not-correction/return-overwrites-delivery-to80에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-362-db_snapshot" assertion으로 "C4.return-not-correction/return-overwrites-delivery-to80에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-362" assertion으로 "C4.return-not-correction/return-overwrites-delivery-to80에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-362" assertion으로 "link-name-362: /data/hostObservation/extractor/rawRows/namedObservations/362/observationName의 실제 equals 기대값은 'return-overwrites-delivery-to80'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-362" assertion으로 "link-oracle-362: /data/hostObservation/extractor/rawRows/namedObservations/362/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-363-scenarios" assertion으로 "C4.return-not-correction/physical-reference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-363-api_response" assertion으로 "C4.return-not-correction/physical-reference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-363-db_snapshot" assertion으로 "C4.return-not-correction/physical-reference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-363" assertion으로 "C4.return-not-correction/physical-reference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-363" assertion으로 "link-name-363: /data/hostObservation/extractor/rawRows/namedObservations/363/observationName의 실제 equals 기대값은 'physical-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-363" assertion으로 "link-oracle-363: /data/hostObservation/extractor/rawRows/namedObservations/363/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-364-scenarios" assertion으로 "C4.corrected-delivery-98/currently-supported-delivery의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-364-api_response" assertion으로 "C4.corrected-delivery-98/currently-supported-delivery에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-364-db_snapshot" assertion으로 "C4.corrected-delivery-98/currently-supported-delivery에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-364" assertion으로 "C4.corrected-delivery-98/currently-supported-delivery에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-364" assertion으로 "link-name-364: /data/hostObservation/extractor/rawRows/namedObservations/364/observationName의 실제 equals 기대값은 'currently-supported-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-364" assertion으로 "link-oracle-364: /data/hostObservation/extractor/rawRows/namedObservations/364/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-365-scenarios" assertion으로 "C4.corrected-delivery-98/current-unresolved-deficit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-365-api_response" assertion으로 "C4.corrected-delivery-98/current-unresolved-deficit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-365-db_snapshot" assertion으로 "C4.corrected-delivery-98/current-unresolved-deficit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-365" assertion으로 "C4.corrected-delivery-98/current-unresolved-deficit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-365" assertion으로 "link-name-365: /data/hostObservation/extractor/rawRows/namedObservations/365/observationName의 실제 equals 기대값은 'current-unresolved-deficit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-365" assertion으로 "link-oracle-365: /data/hostObservation/extractor/rawRows/namedObservations/365/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-366-scenarios" assertion으로 "C4.corrected-delivery-98/historical-assessment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-366-api_response" assertion으로 "C4.corrected-delivery-98/historical-assessment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-366-db_snapshot" assertion으로 "C4.corrected-delivery-98/historical-assessment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-366" assertion으로 "C4.corrected-delivery-98/historical-assessment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-366" assertion으로 "link-name-366: /data/hostObservation/extractor/rawRows/namedObservations/366/observationName의 실제 equals 기대값은 'historical-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-366" assertion으로 "link-oracle-366: /data/hostObservation/extractor/rawRows/namedObservations/366/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-367-scenarios" assertion으로 "C4.corrected-delivery-98/deficit-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-367-api_response" assertion으로 "C4.corrected-delivery-98/deficit-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-367-db_snapshot" assertion으로 "C4.corrected-delivery-98/deficit-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-367" assertion으로 "C4.corrected-delivery-98/deficit-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-367" assertion으로 "link-name-367: /data/hostObservation/extractor/rawRows/namedObservations/367/observationName의 실제 equals 기대값은 'deficit-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-367" assertion으로 "link-oracle-367: /data/hostObservation/extractor/rawRows/namedObservations/367/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-368-scenarios" assertion으로 "C4.resolved-debt-no-resurrection/new-unresolved-deficit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-368-api_response" assertion으로 "C4.resolved-debt-no-resurrection/new-unresolved-deficit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-368-db_snapshot" assertion으로 "C4.resolved-debt-no-resurrection/new-unresolved-deficit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-368" assertion으로 "C4.resolved-debt-no-resurrection/new-unresolved-deficit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-368" assertion으로 "link-name-368: /data/hostObservation/extractor/rawRows/namedObservations/368/observationName의 실제 equals 기대값은 'new-unresolved-deficit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-368" assertion으로 "link-oracle-368: /data/hostObservation/extractor/rawRows/namedObservations/368/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-369-scenarios" assertion으로 "C4.resolved-debt-no-resurrection/resolved-debt-resurrection의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-369-api_response" assertion으로 "C4.resolved-debt-no-resurrection/resolved-debt-resurrection에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-369-db_snapshot" assertion으로 "C4.resolved-debt-no-resurrection/resolved-debt-resurrection에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-369" assertion으로 "C4.resolved-debt-no-resurrection/resolved-debt-resurrection에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-369" assertion으로 "link-name-369: /data/hostObservation/extractor/rawRows/namedObservations/369/observationName의 실제 equals 기대값은 'resolved-debt-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-369" assertion으로 "link-oracle-369: /data/hostObservation/extractor/rawRows/namedObservations/369/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-370-scenarios" assertion으로 "C4.resolved-debt-no-resurrection/valid-resolution의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-370-api_response" assertion으로 "C4.resolved-debt-no-resurrection/valid-resolution에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-370-db_snapshot" assertion으로 "C4.resolved-debt-no-resurrection/valid-resolution에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-370" assertion으로 "C4.resolved-debt-no-resurrection/valid-resolution에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-370" assertion으로 "link-name-370: /data/hostObservation/extractor/rawRows/namedObservations/370/observationName의 실제 equals 기대값은 'valid-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-370" assertion으로 "link-oracle-370: /data/hostObservation/extractor/rawRows/namedObservations/370/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-371-scenarios" assertion으로 "C5.failed-link-responsibility/intake-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-371-api_response" assertion으로 "C5.failed-link-responsibility/intake-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-371-db_snapshot" assertion으로 "C5.failed-link-responsibility/intake-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-371" assertion으로 "C5.failed-link-responsibility/intake-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-371" assertion으로 "link-name-371: /data/hostObservation/extractor/rawRows/namedObservations/371/observationName의 실제 equals 기대값은 'intake-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-371" assertion으로 "link-oracle-371: /data/hostObservation/extractor/rawRows/namedObservations/371/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-372-scenarios" assertion으로 "C5.failed-link-responsibility/intake-tracking의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-372-api_response" assertion으로 "C5.failed-link-responsibility/intake-tracking에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-372-db_snapshot" assertion으로 "C5.failed-link-responsibility/intake-tracking에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-372" assertion으로 "C5.failed-link-responsibility/intake-tracking에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-372" assertion으로 "link-name-372: /data/hostObservation/extractor/rawRows/namedObservations/372/observationName의 실제 equals 기대값은 'intake-tracking'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-372" assertion으로 "link-oracle-372: /data/hostObservation/extractor/rawRows/namedObservations/372/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-373-scenarios" assertion으로 "C5.failed-link-responsibility/alert-delivery-closes-intake의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-373-api_response" assertion으로 "C5.failed-link-responsibility/alert-delivery-closes-intake에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-373-db_snapshot" assertion으로 "C5.failed-link-responsibility/alert-delivery-closes-intake에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-373" assertion으로 "C5.failed-link-responsibility/alert-delivery-closes-intake에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-373" assertion으로 "link-name-373: /data/hostObservation/extractor/rawRows/namedObservations/373/observationName의 실제 equals 기대값은 'alert-delivery-closes-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-373" assertion으로 "link-oracle-373: /data/hostObservation/extractor/rawRows/namedObservations/373/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-374-scenarios" assertion으로 "C5.failed-link-responsibility/closed-parent-rewrite의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-374-api_response" assertion으로 "C5.failed-link-responsibility/closed-parent-rewrite에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-374-db_snapshot" assertion으로 "C5.failed-link-responsibility/closed-parent-rewrite에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-374" assertion으로 "C5.failed-link-responsibility/closed-parent-rewrite에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-374" assertion으로 "link-name-374: /data/hostObservation/extractor/rawRows/namedObservations/374/observationName의 실제 equals 기대값은 'closed-parent-rewrite'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-374" assertion으로 "link-oracle-374: /data/hostObservation/extractor/rawRows/namedObservations/374/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-375-scenarios" assertion으로 "C5.failed-link-responsibility/intake-activation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-375-api_response" assertion으로 "C5.failed-link-responsibility/intake-activation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-375-db_snapshot" assertion으로 "C5.failed-link-responsibility/intake-activation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-375" assertion으로 "C5.failed-link-responsibility/intake-activation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-375" assertion으로 "link-name-375: /data/hostObservation/extractor/rawRows/namedObservations/375/observationName의 실제 equals 기대값은 'intake-activation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-375" assertion으로 "link-oracle-375: /data/hostObservation/extractor/rawRows/namedObservations/375/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-376-scenarios" assertion으로 "C5.retry-once/anomaly-work-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-376-api_response" assertion으로 "C5.retry-once/anomaly-work-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-376-db_snapshot" assertion으로 "C5.retry-once/anomaly-work-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-376" assertion으로 "C5.retry-once/anomaly-work-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-376" assertion으로 "link-name-376: /data/hostObservation/extractor/rawRows/namedObservations/376/observationName의 실제 equals 기대값은 'anomaly-work-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-376" assertion으로 "link-oracle-376: /data/hostObservation/extractor/rawRows/namedObservations/376/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-377-scenarios" assertion으로 "C5.retry-once/anomaly-obligation-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-377-api_response" assertion으로 "C5.retry-once/anomaly-obligation-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-377-db_snapshot" assertion으로 "C5.retry-once/anomaly-obligation-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-377" assertion으로 "C5.retry-once/anomaly-obligation-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-377" assertion으로 "link-name-377: /data/hostObservation/extractor/rawRows/namedObservations/377/observationName의 실제 equals 기대값은 'anomaly-obligation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-377" assertion으로 "link-oracle-377: /data/hostObservation/extractor/rawRows/namedObservations/377/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-378-scenarios" assertion으로 "C5.retry-once/connected-work-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-378-api_response" assertion으로 "C5.retry-once/connected-work-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-378-db_snapshot" assertion으로 "C5.retry-once/connected-work-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-378" assertion으로 "C5.retry-once/connected-work-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-378" assertion으로 "link-name-378: /data/hostObservation/extractor/rawRows/namedObservations/378/observationName의 실제 equals 기대값은 'connected-work-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-378" assertion으로 "link-oracle-378: /data/hostObservation/extractor/rawRows/namedObservations/378/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-379-scenarios" assertion으로 "C5.retry-once/normal-observation-work의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-379-api_response" assertion으로 "C5.retry-once/normal-observation-work에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-379-db_snapshot" assertion으로 "C5.retry-once/normal-observation-work에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-379" assertion으로 "C5.retry-once/normal-observation-work에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-379" assertion으로 "link-name-379: /data/hostObservation/extractor/rawRows/namedObservations/379/observationName의 실제 equals 기대값은 'normal-observation-work'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-379" assertion으로 "link-oracle-379: /data/hostObservation/extractor/rawRows/namedObservations/379/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-380-scenarios" assertion으로 "C5.retry-once/normal-observation-run의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-380-api_response" assertion으로 "C5.retry-once/normal-observation-run에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-380-db_snapshot" assertion으로 "C5.retry-once/normal-observation-run에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-380" assertion으로 "C5.retry-once/normal-observation-run에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-380" assertion으로 "link-name-380: /data/hostObservation/extractor/rawRows/namedObservations/380/observationName의 실제 equals 기대값은 'normal-observation-run'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-380" assertion으로 "link-oracle-380: /data/hostObservation/extractor/rawRows/namedObservations/380/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-381-scenarios" assertion으로 "C5.retry-once/intake-close-after-link의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-381-api_response" assertion으로 "C5.retry-once/intake-close-after-link에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-381-db_snapshot" assertion으로 "C5.retry-once/intake-close-after-link에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-381" assertion으로 "C5.retry-once/intake-close-after-link에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-381" assertion으로 "link-name-381: /data/hostObservation/extractor/rawRows/namedObservations/381/observationName의 실제 equals 기대값은 'intake-close-after-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-381" assertion으로 "link-oracle-381: /data/hostObservation/extractor/rawRows/namedObservations/381/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-382-scenarios" assertion으로 "V1.pinned-meaning-current-policy/v1-endpoint의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-382-api_response" assertion으로 "V1.pinned-meaning-current-policy/v1-endpoint에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-382-db_snapshot" assertion으로 "V1.pinned-meaning-current-policy/v1-endpoint에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-382" assertion으로 "V1.pinned-meaning-current-policy/v1-endpoint에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-382" assertion으로 "link-name-382: /data/hostObservation/extractor/rawRows/namedObservations/382/observationName의 실제 equals 기대값은 'v1-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-382" assertion으로 "link-oracle-382: /data/hostObservation/extractor/rawRows/namedObservations/382/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-383-scenarios" assertion으로 "V1.pinned-meaning-current-policy/new-v2-endpoint의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-383-api_response" assertion으로 "V1.pinned-meaning-current-policy/new-v2-endpoint에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-383-db_snapshot" assertion으로 "V1.pinned-meaning-current-policy/new-v2-endpoint에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-383" assertion으로 "V1.pinned-meaning-current-policy/new-v2-endpoint에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-383" assertion으로 "link-name-383: /data/hostObservation/extractor/rawRows/namedObservations/383/observationName의 실제 equals 기대값은 'new-v2-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-383" assertion으로 "link-oracle-383: /data/hostObservation/extractor/rawRows/namedObservations/383/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-384-scenarios" assertion으로 "V1.pinned-meaning-current-policy/current-sell-permission의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-384-api_response" assertion으로 "V1.pinned-meaning-current-policy/current-sell-permission에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-384-db_snapshot" assertion으로 "V1.pinned-meaning-current-policy/current-sell-permission에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-384" assertion으로 "V1.pinned-meaning-current-policy/current-sell-permission에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-384" assertion으로 "link-name-384: /data/hostObservation/extractor/rawRows/namedObservations/384/observationName의 실제 equals 기대값은 'current-sell-permission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-384" assertion으로 "link-oracle-384: /data/hostObservation/extractor/rawRows/namedObservations/384/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-385-scenarios" assertion으로 "V1.pinned-meaning-current-policy/old-assessment-semantics의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-385-api_response" assertion으로 "V1.pinned-meaning-current-policy/old-assessment-semantics에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-385-db_snapshot" assertion으로 "V1.pinned-meaning-current-policy/old-assessment-semantics에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-385" assertion으로 "V1.pinned-meaning-current-policy/old-assessment-semantics에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-385" assertion으로 "link-name-385: /data/hostObservation/extractor/rawRows/namedObservations/385/observationName의 실제 equals 기대값은 'old-assessment-semantics'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-385" assertion으로 "link-oracle-385: /data/hostObservation/extractor/rawRows/namedObservations/385/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-386-scenarios" assertion으로 "V1.pinned-meaning-current-policy/skill-hash-substitutes-runtime-proof의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-386-api_response" assertion으로 "V1.pinned-meaning-current-policy/skill-hash-substitutes-runtime-proof에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-386-db_snapshot" assertion으로 "V1.pinned-meaning-current-policy/skill-hash-substitutes-runtime-proof에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-386" assertion으로 "V1.pinned-meaning-current-policy/skill-hash-substitutes-runtime-proof에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-386" assertion으로 "link-name-386: /data/hostObservation/extractor/rawRows/namedObservations/386/observationName의 실제 equals 기대값은 'skill-hash-substitutes-runtime-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-386" assertion으로 "link-oracle-386: /data/hostObservation/extractor/rawRows/namedObservations/386/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-387-scenarios" assertion으로 "V1.unsupported-v1-held/unsupported-command-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-387-api_response" assertion으로 "V1.unsupported-v1-held/unsupported-command-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-387-db_snapshot" assertion으로 "V1.unsupported-v1-held/unsupported-command-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-387" assertion으로 "V1.unsupported-v1-held/unsupported-command-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-387" assertion으로 "link-name-387: /data/hostObservation/extractor/rawRows/namedObservations/387/observationName의 실제 equals 기대값은 'unsupported-command-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-387" assertion으로 "link-oracle-387: /data/hostObservation/extractor/rawRows/namedObservations/387/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-388-scenarios" assertion으로 "V1.unsupported-v1-held/unsupported의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-388-api_response" assertion으로 "V1.unsupported-v1-held/unsupported에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-388-db_snapshot" assertion으로 "V1.unsupported-v1-held/unsupported에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-388" assertion으로 "V1.unsupported-v1-held/unsupported에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-388" assertion으로 "link-name-388: /data/hostObservation/extractor/rawRows/namedObservations/388/observationName의 실제 equals 기대값은 'unsupported'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-388" assertion으로 "link-oracle-388: /data/hostObservation/extractor/rawRows/namedObservations/388/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-389-scenarios" assertion으로 "V1.unsupported-v1-held/unsupported-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-389-api_response" assertion으로 "V1.unsupported-v1-held/unsupported-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-389-db_snapshot" assertion으로 "V1.unsupported-v1-held/unsupported-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-389" assertion으로 "V1.unsupported-v1-held/unsupported-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-389" assertion으로 "link-name-389: /data/hostObservation/extractor/rawRows/namedObservations/389/observationName의 실제 equals 기대값은 'unsupported-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-389" assertion으로 "link-oracle-389: /data/hostObservation/extractor/rawRows/namedObservations/389/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-390-scenarios" assertion으로 "V1.unsupported-v1-held/no-silent-fallback의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-390-api_response" assertion으로 "V1.unsupported-v1-held/no-silent-fallback에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-390-db_snapshot" assertion으로 "V1.unsupported-v1-held/no-silent-fallback에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-390" assertion으로 "V1.unsupported-v1-held/no-silent-fallback에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-390" assertion으로 "link-name-390: /data/hostObservation/extractor/rawRows/namedObservations/390/observationName의 실제 equals 기대값은 'no-silent-fallback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-390" assertion으로 "link-oracle-390: /data/hostObservation/extractor/rawRows/namedObservations/390/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-391-scenarios" assertion으로 "V2.split-reserve-race/active-physical의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-391-api_response" assertion으로 "V2.split-reserve-race/active-physical에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-391-db_snapshot" assertion으로 "V2.split-reserve-race/active-physical에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-391" assertion으로 "V2.split-reserve-race/active-physical에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-391" assertion으로 "link-name-391: /data/hostObservation/extractor/rawRows/namedObservations/391/observationName의 실제 equals 기대값은 'active-physical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-391" assertion으로 "link-oracle-391: /data/hostObservation/extractor/rawRows/namedObservations/391/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-392-scenarios" assertion으로 "V2.split-reserve-race/existing-obligation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-392-api_response" assertion으로 "V2.split-reserve-race/existing-obligation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-392-db_snapshot" assertion으로 "V2.split-reserve-race/existing-obligation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-392" assertion으로 "V2.split-reserve-race/existing-obligation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-392" assertion으로 "link-name-392: /data/hostObservation/extractor/rawRows/namedObservations/392/observationName의 실제 equals 기대값은 'existing-obligation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-392" assertion으로 "link-oracle-392: /data/hostObservation/extractor/rawRows/namedObservations/392/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-393-scenarios" assertion으로 "V2.split-reserve-race/new-executable-reservation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-393-api_response" assertion으로 "V2.split-reserve-race/new-executable-reservation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-393-db_snapshot" assertion으로 "V2.split-reserve-race/new-executable-reservation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-393" assertion으로 "V2.split-reserve-race/new-executable-reservation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-393" assertion으로 "link-name-393: /data/hostObservation/extractor/rawRows/namedObservations/393/observationName의 실제 equals 기대값은 'new-executable-reservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-393" assertion으로 "link-oracle-393: /data/hostObservation/extractor/rawRows/namedObservations/393/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-394-scenarios" assertion으로 "V2.split-reserve-race/retired-parent-reconsumption의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-394-api_response" assertion으로 "V2.split-reserve-race/retired-parent-reconsumption에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-394-db_snapshot" assertion으로 "V2.split-reserve-race/retired-parent-reconsumption에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-394" assertion으로 "V2.split-reserve-race/retired-parent-reconsumption에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-394" assertion으로 "link-name-394: /data/hostObservation/extractor/rawRows/namedObservations/394/observationName의 실제 equals 기대값은 'retired-parent-reconsumption'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-394" assertion으로 "link-oracle-394: /data/hostObservation/extractor/rawRows/namedObservations/394/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-395-scenarios" assertion으로 "V2.split-reserve-race/all-executable-reservations의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-395-api_response" assertion으로 "V2.split-reserve-race/all-executable-reservations에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-395-db_snapshot" assertion으로 "V2.split-reserve-race/all-executable-reservations에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-395" assertion으로 "V2.split-reserve-race/all-executable-reservations에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-395" assertion으로 "link-name-395: /data/hostObservation/extractor/rawRows/namedObservations/395/observationName의 실제 equals 기대값은 'all-executable-reservations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-395" assertion으로 "link-oracle-395: /data/hostObservation/extractor/rawRows/namedObservations/395/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-396-scenarios" assertion으로 "V2.split-reserve-race/allocation-transfer의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-396-api_response" assertion으로 "V2.split-reserve-race/allocation-transfer에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-396-db_snapshot" assertion으로 "V2.split-reserve-race/allocation-transfer에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-396" assertion으로 "V2.split-reserve-race/allocation-transfer에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-396" assertion으로 "link-name-396: /data/hostObservation/extractor/rawRows/namedObservations/396/observationName의 실제 equals 기대값은 'allocation-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-396" assertion으로 "link-oracle-396: /data/hostObservation/extractor/rawRows/namedObservations/396/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-397-scenarios" assertion으로 "V2.actual50-correction/active-physical의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-397-api_response" assertion으로 "V2.actual50-correction/active-physical에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-397-db_snapshot" assertion으로 "V2.actual50-correction/active-physical에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-397" assertion으로 "V2.actual50-correction/active-physical에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-397" assertion으로 "link-name-397: /data/hostObservation/extractor/rawRows/namedObservations/397/observationName의 실제 equals 기대값은 'active-physical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-397" assertion으로 "link-oracle-397: /data/hostObservation/extractor/rawRows/namedObservations/397/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-398-scenarios" assertion으로 "V2.actual50-correction/executable-allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-398-api_response" assertion으로 "V2.actual50-correction/executable-allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-398-db_snapshot" assertion으로 "V2.actual50-correction/executable-allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-398" assertion으로 "V2.actual50-correction/executable-allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-398" assertion으로 "link-name-398: /data/hostObservation/extractor/rawRows/namedObservations/398/observationName의 실제 equals 기대값은 'executable-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-398" assertion으로 "link-oracle-398: /data/hostObservation/extractor/rawRows/namedObservations/398/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-399-scenarios" assertion으로 "V2.actual50-correction/promised-obligation-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-399-api_response" assertion으로 "V2.actual50-correction/promised-obligation-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-399-db_snapshot" assertion으로 "V2.actual50-correction/promised-obligation-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-399" assertion으로 "V2.actual50-correction/promised-obligation-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-399" assertion으로 "link-name-399: /data/hostObservation/extractor/rawRows/namedObservations/399/observationName의 실제 equals 기대값은 'promised-obligation-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-399" assertion으로 "link-oracle-399: /data/hostObservation/extractor/rawRows/namedObservations/399/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-400-scenarios" assertion으로 "V2.actual50-correction/minimum-shortage-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-400-api_response" assertion으로 "V2.actual50-correction/minimum-shortage-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-400-db_snapshot" assertion으로 "V2.actual50-correction/minimum-shortage-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-400" assertion으로 "V2.actual50-correction/minimum-shortage-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-400" assertion으로 "link-name-400: /data/hostObservation/extractor/rawRows/namedObservations/400/observationName의 실제 equals 기대값은 'minimum-shortage-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-400" assertion으로 "link-oracle-400: /data/hostObservation/extractor/rawRows/namedObservations/400/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-401-scenarios" assertion으로 "V2.actual50-correction/shortage-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-401-api_response" assertion으로 "V2.actual50-correction/shortage-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-401-db_snapshot" assertion으로 "V2.actual50-correction/shortage-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-401" assertion으로 "V2.actual50-correction/shortage-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-401" assertion으로 "link-name-401: /data/hostObservation/extractor/rawRows/namedObservations/401/observationName의 실제 equals 기대값은 'shortage-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-401" assertion으로 "link-oracle-401: /data/hostObservation/extractor/rawRows/namedObservations/401/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-402-scenarios" assertion으로 "V2.actual50-correction/past-reservation-deletion-to-hide-shortage의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-402-api_response" assertion으로 "V2.actual50-correction/past-reservation-deletion-to-hide-shortage에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-402-db_snapshot" assertion으로 "V2.actual50-correction/past-reservation-deletion-to-hide-shortage에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-402" assertion으로 "V2.actual50-correction/past-reservation-deletion-to-hide-shortage에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-402" assertion으로 "link-name-402: /data/hostObservation/extractor/rawRows/namedObservations/402/observationName의 실제 equals 기대값은 'past-reservation-deletion-to-hide-shortage'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-402" assertion으로 "link-oracle-402: /data/hostObservation/extractor/rawRows/namedObservations/402/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-403-scenarios" assertion으로 "V3.hold-before-dispatch/new-dispatched의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-403-api_response" assertion으로 "V3.hold-before-dispatch/new-dispatched에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-403-db_snapshot" assertion으로 "V3.hold-before-dispatch/new-dispatched에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-403" assertion으로 "V3.hold-before-dispatch/new-dispatched에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-403" assertion으로 "link-name-403: /data/hostObservation/extractor/rawRows/namedObservations/403/observationName의 실제 equals 기대값은 'new-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-403" assertion으로 "link-oracle-403: /data/hostObservation/extractor/rawRows/namedObservations/403/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-404-scenarios" assertion으로 "V3.hold-before-dispatch/allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-404-api_response" assertion으로 "V3.hold-before-dispatch/allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-404-db_snapshot" assertion으로 "V3.hold-before-dispatch/allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-404" assertion으로 "V3.hold-before-dispatch/allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-404" assertion으로 "link-name-404: /data/hostObservation/extractor/rawRows/namedObservations/404/observationName의 실제 equals 기대값은 'allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-404" assertion으로 "link-oracle-404: /data/hostObservation/extractor/rawRows/namedObservations/404/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-405-scenarios" assertion으로 "V3.hold-before-dispatch/blocked-dispatch-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-405-api_response" assertion으로 "V3.hold-before-dispatch/blocked-dispatch-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-405-db_snapshot" assertion으로 "V3.hold-before-dispatch/blocked-dispatch-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-405" assertion으로 "V3.hold-before-dispatch/blocked-dispatch-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-405" assertion으로 "link-name-405: /data/hostObservation/extractor/rawRows/namedObservations/405/observationName의 실제 equals 기대값은 'blocked-dispatch-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-405" assertion으로 "link-oracle-405: /data/hostObservation/extractor/rawRows/namedObservations/405/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-406-scenarios" assertion으로 "V3.hold-before-dispatch/scope-lock의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-406-api_response" assertion으로 "V3.hold-before-dispatch/scope-lock에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-406-db_snapshot" assertion으로 "V3.hold-before-dispatch/scope-lock에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-406" assertion으로 "V3.hold-before-dispatch/scope-lock에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-406" assertion으로 "link-name-406: /data/hostObservation/extractor/rawRows/namedObservations/406/observationName의 실제 equals 기대값은 'scope-lock'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-406" assertion으로 "link-oracle-406: /data/hostObservation/extractor/rawRows/namedObservations/406/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-407-scenarios" assertion으로 "V3.dispatch-before-hold/committed-dispatch-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-407-api_response" assertion으로 "V3.dispatch-before-hold/committed-dispatch-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-407-db_snapshot" assertion으로 "V3.dispatch-before-hold/committed-dispatch-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-407" assertion으로 "V3.dispatch-before-hold/committed-dispatch-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-407" assertion으로 "link-name-407: /data/hostObservation/extractor/rawRows/namedObservations/407/observationName의 실제 equals 기대값은 'committed-dispatch-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-407" assertion으로 "link-oracle-407: /data/hostObservation/extractor/rawRows/namedObservations/407/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-408-scenarios" assertion으로 "V3.dispatch-before-hold/allocation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-408-api_response" assertion으로 "V3.dispatch-before-hold/allocation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-408-db_snapshot" assertion으로 "V3.dispatch-before-hold/allocation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-408" assertion으로 "V3.dispatch-before-hold/allocation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-408" assertion으로 "link-name-408: /data/hostObservation/extractor/rawRows/namedObservations/408/observationName의 실제 equals 기대값은 'allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-408" assertion으로 "link-oracle-408: /data/hostObservation/extractor/rawRows/namedObservations/408/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-409-scenarios" assertion으로 "V3.dispatch-before-hold/post-dispatch-hold-response의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-409-api_response" assertion으로 "V3.dispatch-before-hold/post-dispatch-hold-response에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-409-db_snapshot" assertion으로 "V3.dispatch-before-hold/post-dispatch-hold-response에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-409" assertion으로 "V3.dispatch-before-hold/post-dispatch-hold-response에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-409" assertion으로 "link-name-409: /data/hostObservation/extractor/rawRows/namedObservations/409/observationName의 실제 equals 기대값은 'post-dispatch-hold-response'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-409" assertion으로 "link-oracle-409: /data/hostObservation/extractor/rawRows/namedObservations/409/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-410-scenarios" assertion으로 "V3.dispatch-before-hold/history-deletion-or-fake-rollback의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-410-api_response" assertion으로 "V3.dispatch-before-hold/history-deletion-or-fake-rollback에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-410-db_snapshot" assertion으로 "V3.dispatch-before-hold/history-deletion-or-fake-rollback에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-410" assertion으로 "V3.dispatch-before-hold/history-deletion-or-fake-rollback에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-410" assertion으로 "link-name-410: /data/hostObservation/extractor/rawRows/namedObservations/410/observationName의 실제 equals 기대값은 'history-deletion-or-fake-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-410" assertion으로 "link-oracle-410: /data/hostObservation/extractor/rawRows/namedObservations/410/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-411-scenarios" assertion으로 "V3.late-old-qc-release/new-hold의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-411-api_response" assertion으로 "V3.late-old-qc-release/new-hold에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-411-db_snapshot" assertion으로 "V3.late-old-qc-release/new-hold에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-411" assertion으로 "V3.late-old-qc-release/new-hold에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-411" assertion으로 "link-name-411: /data/hostObservation/extractor/rawRows/namedObservations/411/observationName의 실제 equals 기대값은 'new-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-411" assertion으로 "link-oracle-411: /data/hostObservation/extractor/rawRows/namedObservations/411/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-412-scenarios" assertion으로 "V3.late-old-qc-release/newly-dispatched의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-412-api_response" assertion으로 "V3.late-old-qc-release/newly-dispatched에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-412-db_snapshot" assertion으로 "V3.late-old-qc-release/newly-dispatched에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-412" assertion으로 "V3.late-old-qc-release/newly-dispatched에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-412" assertion으로 "link-name-412: /data/hostObservation/extractor/rawRows/namedObservations/412/observationName의 실제 equals 기대값은 'newly-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-412" assertion으로 "link-oracle-412: /data/hostObservation/extractor/rawRows/namedObservations/412/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-413-scenarios" assertion으로 "V3.late-old-qc-release/late-release-overwrites-new-hold의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-413-api_response" assertion으로 "V3.late-old-qc-release/late-release-overwrites-new-hold에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-413-db_snapshot" assertion으로 "V3.late-old-qc-release/late-release-overwrites-new-hold에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-413" assertion으로 "V3.late-old-qc-release/late-release-overwrites-new-hold에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-413" assertion으로 "link-name-413: /data/hostObservation/extractor/rawRows/namedObservations/413/observationName의 실제 equals 기대값은 'late-release-overwrites-new-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-413" assertion으로 "link-oracle-413: /data/hostObservation/extractor/rawRows/namedObservations/413/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-414-scenarios" assertion으로 "V3.late-old-qc-release/source-order-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-414-api_response" assertion으로 "V3.late-old-qc-release/source-order-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-414-db_snapshot" assertion으로 "V3.late-old-qc-release/source-order-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-414" assertion으로 "V3.late-old-qc-release/source-order-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-414" assertion으로 "link-name-414: /data/hostObservation/extractor/rawRows/namedObservations/414/observationName의 실제 equals 기대값은 'source-order-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-414" assertion으로 "link-oracle-414: /data/hostObservation/extractor/rawRows/namedObservations/414/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-415-mcp" assertion으로 "V4.all-alternate-write-paths/inventory-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-415-scenarios" assertion으로 "V4.all-alternate-write-paths/inventory-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-415-api_response" assertion으로 "V4.all-alternate-write-paths/inventory-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-415-db_snapshot" assertion으로 "V4.all-alternate-write-paths/inventory-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-415" assertion으로 "V4.all-alternate-write-paths/inventory-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-415" assertion으로 "link-name-415: /data/hostObservation/extractor/rawRows/namedObservations/415/observationName의 실제 equals 기대값은 'inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-415" assertion으로 "link-oracle-415: /data/hostObservation/extractor/rawRows/namedObservations/415/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-416-mcp" assertion으로 "V4.all-alternate-write-paths/allocation-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-416-scenarios" assertion으로 "V4.all-alternate-write-paths/allocation-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-416-api_response" assertion으로 "V4.all-alternate-write-paths/allocation-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-416-db_snapshot" assertion으로 "V4.all-alternate-write-paths/allocation-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-416" assertion으로 "V4.all-alternate-write-paths/allocation-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-416" assertion으로 "link-name-416: /data/hostObservation/extractor/rawRows/namedObservations/416/observationName의 실제 equals 기대값은 'allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-416" assertion으로 "link-oracle-416: /data/hostObservation/extractor/rawRows/namedObservations/416/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-417-mcp" assertion으로 "V4.all-alternate-write-paths/approval-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-417-scenarios" assertion으로 "V4.all-alternate-write-paths/approval-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-417-api_response" assertion으로 "V4.all-alternate-write-paths/approval-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-417-db_snapshot" assertion으로 "V4.all-alternate-write-paths/approval-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-417" assertion으로 "V4.all-alternate-write-paths/approval-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-417" assertion으로 "link-name-417: /data/hostObservation/extractor/rawRows/namedObservations/417/observationName의 실제 equals 기대값은 'approval-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-417" assertion으로 "link-oracle-417: /data/hostObservation/extractor/rawRows/namedObservations/417/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-418-mcp" assertion으로 "V4.all-alternate-write-paths/outbox-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-418-scenarios" assertion으로 "V4.all-alternate-write-paths/outbox-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-418-api_response" assertion으로 "V4.all-alternate-write-paths/outbox-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-418-db_snapshot" assertion으로 "V4.all-alternate-write-paths/outbox-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-418" assertion으로 "V4.all-alternate-write-paths/outbox-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-418" assertion으로 "link-name-418: /data/hostObservation/extractor/rawRows/namedObservations/418/observationName의 실제 equals 기대값은 'outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-418" assertion으로 "link-oracle-418: /data/hostObservation/extractor/rawRows/namedObservations/418/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-419-mcp" assertion으로 "V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-419-scenarios" assertion으로 "V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-419-api_response" assertion으로 "V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-419-db_snapshot" assertion으로 "V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-419" assertion으로 "V4.all-alternate-write-paths/mixed-batch-allowed-partial-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-419" assertion으로 "link-name-419: /data/hostObservation/extractor/rawRows/namedObservations/419/observationName의 실제 equals 기대값은 'mixed-batch-allowed-partial-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-419" assertion으로 "link-oracle-419: /data/hostObservation/extractor/rawRows/namedObservations/419/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-420-mcp" assertion으로 "V4.all-alternate-write-paths/same-auth-path의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-420-scenarios" assertion으로 "V4.all-alternate-write-paths/same-auth-path의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-420-api_response" assertion으로 "V4.all-alternate-write-paths/same-auth-path에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-420-db_snapshot" assertion으로 "V4.all-alternate-write-paths/same-auth-path에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-420" assertion으로 "V4.all-alternate-write-paths/same-auth-path에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-420" assertion으로 "link-name-420: /data/hostObservation/extractor/rawRows/namedObservations/420/observationName의 실제 equals 기대값은 'same-auth-path'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-420" assertion으로 "link-oracle-420: /data/hostObservation/extractor/rawRows/namedObservations/420/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-421-scenarios" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-customer-dispatch의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-421-api_response" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-customer-dispatch에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-421-db_snapshot" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-customer-dispatch에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-421" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-customer-dispatch에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-421" assertion으로 "link-name-421: /data/hostObservation/extractor/rawRows/namedObservations/421/observationName의 실제 equals 기대값은 'internal-move-created-customer-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-421" assertion으로 "link-oracle-421: /data/hostObservation/extractor/rawRows/namedObservations/421/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-422-scenarios" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-order-fulfilment의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-422-api_response" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-order-fulfilment에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-422-db_snapshot" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-order-fulfilment에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-422" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-order-fulfilment에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-422" assertion으로 "link-name-422: /data/hostObservation/extractor/rawRows/namedObservations/422/observationName의 실제 equals 기대값은 'internal-move-created-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-422" assertion으로 "link-oracle-422: /data/hostObservation/extractor/rawRows/namedObservations/422/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-423-scenarios" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-return-or-disposal의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-423-api_response" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-return-or-disposal에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-423-db_snapshot" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-return-or-disposal에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-423" assertion으로 "V4.internal-move-cannot-deliver/internal-move-created-return-or-disposal에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-423" assertion으로 "link-name-423: /data/hostObservation/extractor/rawRows/namedObservations/423/observationName의 실제 equals 기대값은 'internal-move-created-return-or-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-423" assertion으로 "link-oracle-423: /data/hostObservation/extractor/rawRows/namedObservations/423/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-424-scenarios" assertion으로 "V4.internal-move-cannot-deliver/observed-unauthorized-fact의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-424-api_response" assertion으로 "V4.internal-move-cannot-deliver/observed-unauthorized-fact에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-424-db_snapshot" assertion으로 "V4.internal-move-cannot-deliver/observed-unauthorized-fact에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-424" assertion으로 "V4.internal-move-cannot-deliver/observed-unauthorized-fact에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-424" assertion으로 "link-name-424: /data/hostObservation/extractor/rawRows/namedObservations/424/observationName의 실제 equals 기대값은 'observed-unauthorized-fact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-424" assertion으로 "link-oracle-424: /data/hostObservation/extractor/rawRows/namedObservations/424/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-425-scenarios" assertion으로 "V4.internal-move-cannot-deliver/unauthorized-move-reconciliation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-425-api_response" assertion으로 "V4.internal-move-cannot-deliver/unauthorized-move-reconciliation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-425-db_snapshot" assertion으로 "V4.internal-move-cannot-deliver/unauthorized-move-reconciliation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-425" assertion으로 "V4.internal-move-cannot-deliver/unauthorized-move-reconciliation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-425" assertion으로 "link-name-425: /data/hostObservation/extractor/rawRows/namedObservations/425/observationName의 실제 equals 기대값은 'unauthorized-move-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-425" assertion으로 "link-oracle-425: /data/hostObservation/extractor/rawRows/namedObservations/425/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-426-scenarios" assertion으로 "V5.wait-after-full-restart/resumed-or-escalated-duty-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-426-api_response" assertion으로 "V5.wait-after-full-restart/resumed-or-escalated-duty-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-426-db_snapshot" assertion으로 "V5.wait-after-full-restart/resumed-or-escalated-duty-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-426" assertion으로 "V5.wait-after-full-restart/resumed-or-escalated-duty-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-426" assertion으로 "link-name-426: /data/hostObservation/extractor/rawRows/namedObservations/426/observationName의 실제 equals 기대값은 'resumed-or-escalated-duty-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-426" assertion으로 "link-oracle-426: /data/hostObservation/extractor/rawRows/namedObservations/426/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-427-scenarios" assertion으로 "V5.wait-after-full-restart/within-test-profile의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-427-api_response" assertion으로 "V5.wait-after-full-restart/within-test-profile에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-427-db_snapshot" assertion으로 "V5.wait-after-full-restart/within-test-profile에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-427" assertion으로 "V5.wait-after-full-restart/within-test-profile에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-427" assertion으로 "link-name-427: /data/hostObservation/extractor/rawRows/namedObservations/427/observationName의 실제 equals 기대값은 'within-test-profile'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-427" assertion으로 "link-oracle-427: /data/hostObservation/extractor/rawRows/namedObservations/427/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-428-scenarios" assertion으로 "V5.wait-after-full-restart/duty-after-restart의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-428-api_response" assertion으로 "V5.wait-after-full-restart/duty-after-restart에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-428-db_snapshot" assertion으로 "V5.wait-after-full-restart/duty-after-restart에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-428" assertion으로 "V5.wait-after-full-restart/duty-after-restart에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-428" assertion으로 "link-name-428: /data/hostObservation/extractor/rawRows/namedObservations/428/observationName의 실제 equals 기대값은 'duty-after-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-428" assertion으로 "link-oracle-428: /data/hostObservation/extractor/rawRows/namedObservations/428/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-429-scenarios" assertion으로 "V5.wait-after-full-restart/queue-success-as-goal-satisfied의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-429-api_response" assertion으로 "V5.wait-after-full-restart/queue-success-as-goal-satisfied에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-429-db_snapshot" assertion으로 "V5.wait-after-full-restart/queue-success-as-goal-satisfied에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-429" assertion으로 "V5.wait-after-full-restart/queue-success-as-goal-satisfied에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-429" assertion으로 "link-name-429: /data/hostObservation/extractor/rawRows/namedObservations/429/observationName의 실제 equals 기대값은 'queue-success-as-goal-satisfied'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-429" assertion으로 "link-oracle-429: /data/hostObservation/extractor/rawRows/namedObservations/429/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-430-scenarios" assertion으로 "V5.two-workers-fencing/duplicate-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-430-api_response" assertion으로 "V5.two-workers-fencing/duplicate-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-430-db_snapshot" assertion으로 "V5.two-workers-fencing/duplicate-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-430" assertion으로 "V5.two-workers-fencing/duplicate-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-430" assertion으로 "link-name-430: /data/hostObservation/extractor/rawRows/namedObservations/430/observationName의 실제 equals 기대값은 'duplicate-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-430" assertion으로 "link-oracle-430: /data/hostObservation/extractor/rawRows/namedObservations/430/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-431-scenarios" assertion으로 "V5.two-workers-fencing/stale-worker-commit의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-431-api_response" assertion으로 "V5.two-workers-fencing/stale-worker-commit에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-431-db_snapshot" assertion으로 "V5.two-workers-fencing/stale-worker-commit에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-431" assertion으로 "V5.two-workers-fencing/stale-worker-commit에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-431" assertion으로 "link-name-431: /data/hostObservation/extractor/rawRows/namedObservations/431/observationName의 실제 equals 기대값은 'stale-worker-commit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-431" assertion으로 "link-oracle-431: /data/hostObservation/extractor/rawRows/namedObservations/431/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-432-scenarios" assertion으로 "V5.two-workers-fencing/fence-state의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-432-api_response" assertion으로 "V5.two-workers-fencing/fence-state에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-432-db_snapshot" assertion으로 "V5.two-workers-fencing/fence-state에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-432" assertion으로 "V5.two-workers-fencing/fence-state에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-432" assertion으로 "link-name-432: /data/hostObservation/extractor/rawRows/namedObservations/432/observationName의 실제 equals 기대값은 'fence-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-432" assertion으로 "link-oracle-432: /data/hostObservation/extractor/rawRows/namedObservations/432/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-433-scenarios" assertion으로 "V5.two-workers-fencing/exhausted-retry-owner의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-433-api_response" assertion으로 "V5.two-workers-fencing/exhausted-retry-owner에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-433-db_snapshot" assertion으로 "V5.two-workers-fencing/exhausted-retry-owner에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-433" assertion으로 "V5.two-workers-fencing/exhausted-retry-owner에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-433" assertion으로 "link-name-433: /data/hostObservation/extractor/rawRows/namedObservations/433/observationName의 실제 equals 기대값은 'exhausted-retry-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-433" assertion으로 "link-oracle-433: /data/hostObservation/extractor/rawRows/namedObservations/433/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-434-mcp" assertion으로 "V6.receipt60-lost-response-retry/physical-receipt-total의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-434-scenarios" assertion으로 "V6.receipt60-lost-response-retry/physical-receipt-total의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-434-api_response" assertion으로 "V6.receipt60-lost-response-retry/physical-receipt-total에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-434-db_snapshot" assertion으로 "V6.receipt60-lost-response-retry/physical-receipt-total에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-434" assertion으로 "V6.receipt60-lost-response-retry/physical-receipt-total에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-434" assertion으로 "link-name-434: /data/hostObservation/extractor/rawRows/namedObservations/434/observationName의 실제 equals 기대값은 'physical-receipt-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-434" assertion으로 "link-oracle-434: /data/hostObservation/extractor/rawRows/namedObservations/434/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-435-mcp" assertion으로 "V6.receipt60-lost-response-retry/committed-effect-count의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-435-scenarios" assertion으로 "V6.receipt60-lost-response-retry/committed-effect-count의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-435-api_response" assertion으로 "V6.receipt60-lost-response-retry/committed-effect-count에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-435-db_snapshot" assertion으로 "V6.receipt60-lost-response-retry/committed-effect-count에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-435" assertion으로 "V6.receipt60-lost-response-retry/committed-effect-count에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-435" assertion으로 "link-name-435: /data/hostObservation/extractor/rawRows/namedObservations/435/observationName의 실제 equals 기대값은 'committed-effect-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-435" assertion으로 "link-oracle-435: /data/hostObservation/extractor/rawRows/namedObservations/435/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-436-mcp" assertion으로 "V6.receipt60-lost-response-retry/retry-result의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-436-scenarios" assertion으로 "V6.receipt60-lost-response-retry/retry-result의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-436-api_response" assertion으로 "V6.receipt60-lost-response-retry/retry-result에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-436-db_snapshot" assertion으로 "V6.receipt60-lost-response-retry/retry-result에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-436" assertion으로 "V6.receipt60-lost-response-retry/retry-result에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-436" assertion으로 "link-name-436: /data/hostObservation/extractor/rawRows/namedObservations/436/observationName의 실제 equals 기대값은 'retry-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-436" assertion으로 "link-oracle-436: /data/hostObservation/extractor/rawRows/namedObservations/436/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-437-scenarios" assertion으로 "V6.key-conflicts-owner-and-distinct-order/different-payload의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-437-api_response" assertion으로 "V6.key-conflicts-owner-and-distinct-order/different-payload에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-437-db_snapshot" assertion으로 "V6.key-conflicts-owner-and-distinct-order/different-payload에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-437" assertion으로 "V6.key-conflicts-owner-and-distinct-order/different-payload에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-437" assertion으로 "link-name-437: /data/hostObservation/extractor/rawRows/namedObservations/437/observationName의 실제 equals 기대값은 'different-payload'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-437" assertion으로 "link-oracle-437: /data/hostObservation/extractor/rawRows/namedObservations/437/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-438-scenarios" assertion으로 "V6.key-conflicts-owner-and-distinct-order/conflict-new-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-438-api_response" assertion으로 "V6.key-conflicts-owner-and-distinct-order/conflict-new-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-438-db_snapshot" assertion으로 "V6.key-conflicts-owner-and-distinct-order/conflict-new-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-438" assertion으로 "V6.key-conflicts-owner-and-distinct-order/conflict-new-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-438" assertion으로 "link-name-438: /data/hostObservation/extractor/rawRows/namedObservations/438/observationName의 실제 equals 기대값은 'conflict-new-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-438" assertion으로 "link-oracle-438: /data/hostObservation/extractor/rawRows/namedObservations/438/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-439-scenarios" assertion으로 "V6.key-conflicts-owner-and-distinct-order/other-owner-result-leak의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-439-api_response" assertion으로 "V6.key-conflicts-owner-and-distinct-order/other-owner-result-leak에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-439-db_snapshot" assertion으로 "V6.key-conflicts-owner-and-distinct-order/other-owner-result-leak에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-439" assertion으로 "V6.key-conflicts-owner-and-distinct-order/other-owner-result-leak에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-439" assertion으로 "link-name-439: /data/hostObservation/extractor/rawRows/namedObservations/439/observationName의 실제 equals 기대값은 'other-owner-result-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-439" assertion으로 "link-oracle-439: /data/hostObservation/extractor/rawRows/namedObservations/439/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-440-scenarios" assertion으로 "V6.key-conflicts-owner-and-distinct-order/namespaces의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-440-api_response" assertion으로 "V6.key-conflicts-owner-and-distinct-order/namespaces에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-440-db_snapshot" assertion으로 "V6.key-conflicts-owner-and-distinct-order/namespaces에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-440" assertion으로 "V6.key-conflicts-owner-and-distinct-order/namespaces에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-440" assertion으로 "link-name-440: /data/hostObservation/extractor/rawRows/namedObservations/440/observationName의 실제 equals 기대값은 'namespaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-440" assertion으로 "link-oracle-440: /data/hostObservation/extractor/rawRows/namedObservations/440/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-441-mcp" assertion으로 "V6.input-destination-supplement/draft-supplement-physical-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-441-scenarios" assertion으로 "V6.input-destination-supplement/draft-supplement-physical-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-441-api_response" assertion으로 "V6.input-destination-supplement/draft-supplement-physical-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-441-db_snapshot" assertion으로 "V6.input-destination-supplement/draft-supplement-physical-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-441" assertion으로 "V6.input-destination-supplement/draft-supplement-physical-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-441" assertion으로 "link-name-441: /data/hostObservation/extractor/rawRows/namedObservations/441/observationName의 실제 equals 기대값은 'draft-supplement-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-441" assertion으로 "link-oracle-441: /data/hostObservation/extractor/rawRows/namedObservations/441/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-442-mcp" assertion으로 "V6.input-destination-supplement/draft-supplement-idempotency-conflict의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-442-scenarios" assertion으로 "V6.input-destination-supplement/draft-supplement-idempotency-conflict의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-442-api_response" assertion으로 "V6.input-destination-supplement/draft-supplement-idempotency-conflict에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-442-db_snapshot" assertion으로 "V6.input-destination-supplement/draft-supplement-idempotency-conflict에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-442" assertion으로 "V6.input-destination-supplement/draft-supplement-idempotency-conflict에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-442" assertion으로 "link-name-442: /data/hostObservation/extractor/rawRows/namedObservations/442/observationName의 실제 equals 기대값은 'draft-supplement-idempotency-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-442" assertion으로 "link-oracle-442: /data/hostObservation/extractor/rawRows/namedObservations/442/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-443-mcp" assertion으로 "V6.input-destination-supplement/completed-intent의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-443-scenarios" assertion으로 "V6.input-destination-supplement/completed-intent의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-443-api_response" assertion으로 "V6.input-destination-supplement/completed-intent에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-443-db_snapshot" assertion으로 "V6.input-destination-supplement/completed-intent에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-443" assertion으로 "V6.input-destination-supplement/completed-intent에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-443" assertion으로 "link-name-443: /data/hostObservation/extractor/rawRows/namedObservations/443/observationName의 실제 equals 기대값은 'completed-intent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-443" assertion으로 "link-oracle-443: /data/hostObservation/extractor/rawRows/namedObservations/443/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-444-scenarios" assertion으로 "V7.enqueue-then-revoke/new-effects-quantity의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-444-api_response" assertion으로 "V7.enqueue-then-revoke/new-effects-quantity에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-444-db_snapshot" assertion으로 "V7.enqueue-then-revoke/new-effects-quantity에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-444" assertion으로 "V7.enqueue-then-revoke/new-effects-quantity에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-444" assertion으로 "link-name-444: /data/hostObservation/extractor/rawRows/namedObservations/444/observationName의 실제 equals 기대값은 'new-effects-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-444" assertion으로 "link-oracle-444: /data/hostObservation/extractor/rawRows/namedObservations/444/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-445-scenarios" assertion으로 "V7.enqueue-then-revoke/blocked-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-445-api_response" assertion으로 "V7.enqueue-then-revoke/blocked-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-445-db_snapshot" assertion으로 "V7.enqueue-then-revoke/blocked-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-445" assertion으로 "V7.enqueue-then-revoke/blocked-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-445" assertion으로 "link-name-445: /data/hostObservation/extractor/rawRows/namedObservations/445/observationName의 실제 equals 기대값은 'blocked-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-445" assertion으로 "link-oracle-445: /data/hostObservation/extractor/rawRows/namedObservations/445/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-446-scenarios" assertion으로 "V7.enqueue-then-revoke/worker-auth의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-446-api_response" assertion으로 "V7.enqueue-then-revoke/worker-auth에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-446-db_snapshot" assertion으로 "V7.enqueue-then-revoke/worker-auth에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-446" assertion으로 "V7.enqueue-then-revoke/worker-auth에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-446" assertion으로 "link-name-446: /data/hostObservation/extractor/rawRows/namedObservations/446/observationName의 실제 equals 기대값은 'worker-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-446" assertion으로 "link-oracle-446: /data/hostObservation/extractor/rawRows/namedObservations/446/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-447-scenarios" assertion으로 "V7.authorization-then-revoke/effect-if-revoke-commits-first의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-447-api_response" assertion으로 "V7.authorization-then-revoke/effect-if-revoke-commits-first에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-447-db_snapshot" assertion으로 "V7.authorization-then-revoke/effect-if-revoke-commits-first에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-447" assertion으로 "V7.authorization-then-revoke/effect-if-revoke-commits-first에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-447" assertion으로 "link-name-447: /data/hostObservation/extractor/rawRows/namedObservations/447/observationName의 실제 equals 기대값은 'effect-if-revoke-commits-first'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-447" assertion으로 "link-oracle-447: /data/hostObservation/extractor/rawRows/namedObservations/447/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-448-scenarios" assertion으로 "V7.authorization-then-revoke/preserved-if-effect-commits-first의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-448-api_response" assertion으로 "V7.authorization-then-revoke/preserved-if-effect-commits-first에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-448-db_snapshot" assertion으로 "V7.authorization-then-revoke/preserved-if-effect-commits-first에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-448" assertion으로 "V7.authorization-then-revoke/preserved-if-effect-commits-first에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-448" assertion으로 "link-name-448: /data/hostObservation/extractor/rawRows/namedObservations/448/observationName의 실제 equals 기대값은 'preserved-if-effect-commits-first'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-448" assertion으로 "link-oracle-448: /data/hostObservation/extractor/rawRows/namedObservations/448/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-449-scenarios" assertion으로 "V7.authorization-then-revoke/effect-after-serialized-revoke의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-449-api_response" assertion으로 "V7.authorization-then-revoke/effect-after-serialized-revoke에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-449-db_snapshot" assertion으로 "V7.authorization-then-revoke/effect-after-serialized-revoke에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-449" assertion으로 "V7.authorization-then-revoke/effect-after-serialized-revoke에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-449" assertion으로 "link-name-449: /data/hostObservation/extractor/rawRows/namedObservations/449/observationName의 실제 equals 기대값은 'effect-after-serialized-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-449" assertion으로 "link-oracle-449: /data/hostObservation/extractor/rawRows/namedObservations/449/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-450-scenarios" assertion으로 "V7.authorization-then-revoke/linearization-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-450-api_response" assertion으로 "V7.authorization-then-revoke/linearization-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-450-db_snapshot" assertion으로 "V7.authorization-then-revoke/linearization-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-450" assertion으로 "V7.authorization-then-revoke/linearization-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-450" assertion으로 "link-name-450: /data/hostObservation/extractor/rawRows/namedObservations/450/observationName의 실제 equals 기대값은 'linearization-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-450" assertion으로 "link-oracle-450: /data/hostObservation/extractor/rawRows/namedObservations/450/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-451-scenarios" assertion으로 "V7.authorization-then-revoke/blocked-or-post-revoke-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-451-api_response" assertion으로 "V7.authorization-then-revoke/blocked-or-post-revoke-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-451-db_snapshot" assertion으로 "V7.authorization-then-revoke/blocked-or-post-revoke-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-451" assertion으로 "V7.authorization-then-revoke/blocked-or-post-revoke-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-451" assertion으로 "link-name-451: /data/hostObservation/extractor/rawRows/namedObservations/451/observationName의 실제 equals 기대값은 'blocked-or-post-revoke-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-451" assertion으로 "link-oracle-451: /data/hostObservation/extractor/rawRows/namedObservations/451/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-452-scenarios" assertion으로 "V7.restart-after-revocation/unauthorized-recovered-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-452-api_response" assertion으로 "V7.restart-after-revocation/unauthorized-recovered-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-452-db_snapshot" assertion으로 "V7.restart-after-revocation/unauthorized-recovered-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-452" assertion으로 "V7.restart-after-revocation/unauthorized-recovered-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-452" assertion으로 "link-name-452: /data/hostObservation/extractor/rawRows/namedObservations/452/observationName의 실제 equals 기대값은 'unauthorized-recovered-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-452" assertion으로 "link-oracle-452: /data/hostObservation/extractor/rawRows/namedObservations/452/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-453-scenarios" assertion으로 "V7.restart-after-revocation/prior-committed-preserved의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-453-api_response" assertion으로 "V7.restart-after-revocation/prior-committed-preserved에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-453-db_snapshot" assertion으로 "V7.restart-after-revocation/prior-committed-preserved에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-453" assertion으로 "V7.restart-after-revocation/prior-committed-preserved에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-453" assertion으로 "link-name-453: /data/hostObservation/extractor/rawRows/namedObservations/453/observationName의 실제 equals 기대값은 'prior-committed-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-453" assertion으로 "link-oracle-453: /data/hostObservation/extractor/rawRows/namedObservations/453/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-454-scenarios" assertion으로 "V7.restart-after-revocation/blocked-duty-after-restart의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-454-api_response" assertion으로 "V7.restart-after-revocation/blocked-duty-after-restart에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-454-db_snapshot" assertion으로 "V7.restart-after-revocation/blocked-duty-after-restart에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-454" assertion으로 "V7.restart-after-revocation/blocked-duty-after-restart에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-454" assertion으로 "link-name-454: /data/hostObservation/extractor/rawRows/namedObservations/454/observationName의 실제 equals 기대값은 'blocked-duty-after-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-454" assertion으로 "link-oracle-454: /data/hostObservation/extractor/rawRows/namedObservations/454/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-455-scenarios" assertion으로 "V7.restart-after-revocation/safe-retry의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-455-api_response" assertion으로 "V7.restart-after-revocation/safe-retry에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-455-db_snapshot" assertion으로 "V7.restart-after-revocation/safe-retry에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-455" assertion으로 "V7.restart-after-revocation/safe-retry에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-455" assertion으로 "link-name-455: /data/hostObservation/extractor/rawRows/namedObservations/455/observationName의 실제 equals 기대값은 'safe-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-455" assertion으로 "link-oracle-455: /data/hostObservation/extractor/rawRows/namedObservations/455/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-456-local-deployment" assertion으로 "V8.fresh-install/fresh-integration의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-456-mcp" assertion으로 "V8.fresh-install/fresh-integration의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-456-scenarios" assertion으로 "V8.fresh-install/fresh-integration의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-456-api_response" assertion으로 "V8.fresh-install/fresh-integration에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-456-db_snapshot" assertion으로 "V8.fresh-install/fresh-integration에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-456" assertion으로 "V8.fresh-install/fresh-integration에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-456" assertion으로 "link-name-456: /data/hostObservation/extractor/rawRows/namedObservations/456/observationName의 실제 equals 기대값은 'fresh-integration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-456" assertion으로 "link-oracle-456: /data/hostObservation/extractor/rawRows/namedObservations/456/oracleId의 실제 equals 기대값은 'V8.fresh-install'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-457-local-deployment" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-457-mcp" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-457-scenarios" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-457-api_response" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-457-db_snapshot" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-457" assertion으로 "V8.ontology-upgrade-preservation/upgrade-preserves에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-457" assertion으로 "link-name-457: /data/hostObservation/extractor/rawRows/namedObservations/457/observationName의 실제 equals 기대값은 'upgrade-preserves'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-457" assertion으로 "link-oracle-457: /data/hostObservation/extractor/rawRows/namedObservations/457/oracleId의 실제 equals 기대값은 'V8.ontology-upgrade-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-458-local-deployment" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-458-mcp" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-458-scenarios" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-458-api_response" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-458-db_snapshot" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-458" assertion으로 "V8.ontology-upgrade-preservation/legacy-migration-substitutes-ontology-upgrade에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-458" assertion으로 "link-name-458: /data/hostObservation/extractor/rawRows/namedObservations/458/observationName의 실제 equals 기대값은 'legacy-migration-substitutes-ontology-upgrade'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-458" assertion으로 "link-oracle-458: /data/hostObservation/extractor/rawRows/namedObservations/458/oracleId의 실제 equals 기대값은 'V8.ontology-upgrade-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-459-local-deployment" assertion으로 "V8.db-blob-definition-restore/restore-artifacts의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-459-mcp" assertion으로 "V8.db-blob-definition-restore/restore-artifacts의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-459-scenarios" assertion으로 "V8.db-blob-definition-restore/restore-artifacts의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-459-api_response" assertion으로 "V8.db-blob-definition-restore/restore-artifacts에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-459-db_snapshot" assertion으로 "V8.db-blob-definition-restore/restore-artifacts에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-459" assertion으로 "V8.db-blob-definition-restore/restore-artifacts에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-459" assertion으로 "link-name-459: /data/hostObservation/extractor/rawRows/namedObservations/459/observationName의 실제 equals 기대값은 'restore-artifacts'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-459" assertion으로 "link-oracle-459: /data/hostObservation/extractor/rawRows/namedObservations/459/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-460-local-deployment" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-460-mcp" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-460-scenarios" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-460-api_response" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-460-db_snapshot" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-460" assertion으로 "V8.db-blob-definition-restore/missing-blob-restore-complete에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-460" assertion으로 "link-name-460: /data/hostObservation/extractor/rawRows/namedObservations/460/observationName의 실제 equals 기대값은 'missing-blob-restore-complete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-460" assertion으로 "link-oracle-460: /data/hostObservation/extractor/rawRows/namedObservations/460/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-461-local-deployment" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete의 필수 계층 local-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-461-mcp" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-461-scenarios" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-461-api_response" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-461-db_snapshot" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-461" assertion으로 "V8.db-blob-definition-restore/missing-evaluator-restore-complete에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-461" assertion으로 "link-name-461: /data/hostObservation/extractor/rawRows/namedObservations/461/observationName의 실제 equals 기대값은 'missing-evaluator-restore-complete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-461" assertion으로 "link-oracle-461: /data/hostObservation/extractor/rawRows/namedObservations/461/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-462-btp-deployment" assertion으로 "V8.btp-client-separate-gates/btp-acceptance의 필수 계층 btp-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-462-mcp" assertion으로 "V8.btp-client-separate-gates/btp-acceptance의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-462-deployment_report" assertion으로 "V8.btp-client-separate-gates/btp-acceptance에는 catalog artifactKind deployment_report의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-462-protocol_transcript" assertion으로 "V8.btp-client-separate-gates/btp-acceptance에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-462" assertion으로 "V8.btp-client-separate-gates/btp-acceptance에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-462" assertion으로 "link-name-462: /data/hostObservation/extractor/rawRows/namedObservations/462/observationName의 실제 equals 기대값은 'btp-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-462" assertion으로 "link-oracle-462: /data/hostObservation/extractor/rawRows/namedObservations/462/oracleId의 실제 equals 기대값은 'V8.btp-client-separate-gates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-463-btp-deployment" assertion으로 "V8.btp-client-separate-gates/result-scope의 필수 계층 btp-deployment profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-463-mcp" assertion으로 "V8.btp-client-separate-gates/result-scope의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-463-deployment_report" assertion으로 "V8.btp-client-separate-gates/result-scope에는 catalog artifactKind deployment_report의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-463-protocol_transcript" assertion으로 "V8.btp-client-separate-gates/result-scope에는 catalog artifactKind protocol_transcript의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-463" assertion으로 "V8.btp-client-separate-gates/result-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-463" assertion으로 "link-name-463: /data/hostObservation/extractor/rawRows/namedObservations/463/observationName의 실제 equals 기대값은 'result-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-463" assertion으로 "link-oracle-463: /data/hostObservation/extractor/rawRows/namedObservations/463/oracleId의 실제 equals 기대값은 'V8.btp-client-separate-gates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-464-mcp" assertion으로 "E1.full-flow-quantities/purchase-cumulative-arrival의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-464-scenarios" assertion으로 "E1.full-flow-quantities/purchase-cumulative-arrival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-464-api_response" assertion으로 "E1.full-flow-quantities/purchase-cumulative-arrival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-464-db_snapshot" assertion으로 "E1.full-flow-quantities/purchase-cumulative-arrival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-464" assertion으로 "E1.full-flow-quantities/purchase-cumulative-arrival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-464" assertion으로 "link-name-464: /data/hostObservation/extractor/rawRows/namedObservations/464/observationName의 실제 equals 기대값은 'purchase-cumulative-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-464" assertion으로 "link-oracle-464: /data/hostObservation/extractor/rawRows/namedObservations/464/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-465-mcp" assertion으로 "E1.full-flow-quantities/warehouse-current-held의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-465-scenarios" assertion으로 "E1.full-flow-quantities/warehouse-current-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-465-api_response" assertion으로 "E1.full-flow-quantities/warehouse-current-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-465-db_snapshot" assertion으로 "E1.full-flow-quantities/warehouse-current-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-465" assertion으로 "E1.full-flow-quantities/warehouse-current-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-465" assertion으로 "link-name-465: /data/hostObservation/extractor/rawRows/namedObservations/465/observationName의 실제 equals 기대값은 'warehouse-current-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-465" assertion으로 "link-oracle-465: /data/hostObservation/extractor/rawRows/namedObservations/465/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-466-mcp" assertion으로 "E1.full-flow-quantities/historical-delivery의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-466-scenarios" assertion으로 "E1.full-flow-quantities/historical-delivery의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-466-api_response" assertion으로 "E1.full-flow-quantities/historical-delivery에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-466-db_snapshot" assertion으로 "E1.full-flow-quantities/historical-delivery에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-466" assertion으로 "E1.full-flow-quantities/historical-delivery에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-466" assertion으로 "link-name-466: /data/hostObservation/extractor/rawRows/namedObservations/466/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-466" assertion으로 "link-oracle-466: /data/hostObservation/extractor/rawRows/namedObservations/466/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-467-mcp" assertion으로 "E1.full-flow-quantities/return-received의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-467-scenarios" assertion으로 "E1.full-flow-quantities/return-received의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-467-api_response" assertion으로 "E1.full-flow-quantities/return-received에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-467-db_snapshot" assertion으로 "E1.full-flow-quantities/return-received에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-467" assertion으로 "E1.full-flow-quantities/return-received에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-467" assertion으로 "link-name-467: /data/hostObservation/extractor/rawRows/namedObservations/467/observationName의 실제 equals 기대값은 'return-received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-467" assertion으로 "link-oracle-467: /data/hostObservation/extractor/rawRows/namedObservations/467/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-468-mcp" assertion으로 "E1.full-flow-quantities/current-sell-eligible의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-468-scenarios" assertion으로 "E1.full-flow-quantities/current-sell-eligible의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-468-api_response" assertion으로 "E1.full-flow-quantities/current-sell-eligible에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-468-db_snapshot" assertion으로 "E1.full-flow-quantities/current-sell-eligible에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-468" assertion으로 "E1.full-flow-quantities/current-sell-eligible에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-468" assertion으로 "link-name-468: /data/hostObservation/extractor/rawRows/namedObservations/468/observationName의 실제 equals 기대값은 'current-sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-468" assertion으로 "link-oracle-468: /data/hostObservation/extractor/rawRows/namedObservations/468/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-469-mcp" assertion으로 "E1.full-flow-quantities/agency-unverified-held의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-469-scenarios" assertion으로 "E1.full-flow-quantities/agency-unverified-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-469-api_response" assertion으로 "E1.full-flow-quantities/agency-unverified-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-469-db_snapshot" assertion으로 "E1.full-flow-quantities/agency-unverified-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-469" assertion으로 "E1.full-flow-quantities/agency-unverified-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-469" assertion으로 "link-name-469: /data/hostObservation/extractor/rawRows/namedObservations/469/observationName의 실제 equals 기대값은 'agency-unverified-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-469" assertion으로 "link-oracle-469: /data/hostObservation/extractor/rawRows/namedObservations/469/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-470-mcp" assertion으로 "E1.full-flow-quantities/QC-held의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-470-scenarios" assertion으로 "E1.full-flow-quantities/QC-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-470-api_response" assertion으로 "E1.full-flow-quantities/QC-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-470-db_snapshot" assertion으로 "E1.full-flow-quantities/QC-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-470" assertion으로 "E1.full-flow-quantities/QC-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-470" assertion으로 "link-name-470: /data/hostObservation/extractor/rawRows/namedObservations/470/observationName의 실제 equals 기대값은 'QC-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-470" assertion으로 "link-oracle-470: /data/hostObservation/extractor/rawRows/namedObservations/470/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-471-mcp" assertion으로 "E1.full-flow-quantities/return-held의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-471-scenarios" assertion으로 "E1.full-flow-quantities/return-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-471-api_response" assertion으로 "E1.full-flow-quantities/return-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-471-db_snapshot" assertion으로 "E1.full-flow-quantities/return-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-471" assertion으로 "E1.full-flow-quantities/return-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-471" assertion으로 "link-name-471: /data/hostObservation/extractor/rawRows/namedObservations/471/observationName의 실제 equals 기대값은 'return-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-471" assertion으로 "link-oracle-471: /data/hostObservation/extractor/rawRows/namedObservations/471/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-472-mcp" assertion으로 "E1.full-flow-quantities/bank-transfer의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-472-scenarios" assertion으로 "E1.full-flow-quantities/bank-transfer의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-472-api_response" assertion으로 "E1.full-flow-quantities/bank-transfer에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-472-db_snapshot" assertion으로 "E1.full-flow-quantities/bank-transfer에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-472" assertion으로 "E1.full-flow-quantities/bank-transfer에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-472" assertion으로 "link-name-472: /data/hostObservation/extractor/rawRows/namedObservations/472/observationName의 실제 equals 기대값은 'bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-472" assertion으로 "link-oracle-472: /data/hostObservation/extractor/rawRows/namedObservations/472/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-473-mcp" assertion으로 "E1.full-flow-quantities/receipt60-doc-duplicate-effects의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-473-scenarios" assertion으로 "E1.full-flow-quantities/receipt60-doc-duplicate-effects의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-473-api_response" assertion으로 "E1.full-flow-quantities/receipt60-doc-duplicate-effects에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-473-db_snapshot" assertion으로 "E1.full-flow-quantities/receipt60-doc-duplicate-effects에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-473" assertion으로 "E1.full-flow-quantities/receipt60-doc-duplicate-effects에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-473" assertion으로 "link-name-473: /data/hostObservation/extractor/rawRows/namedObservations/473/observationName의 실제 equals 기대값은 'receipt60-doc-duplicate-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-473" assertion으로 "link-oracle-473: /data/hostObservation/extractor/rawRows/namedObservations/473/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-474-mcp" assertion으로 "E1.full-flow-quantities/return-added-purchase-arrival의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-474-scenarios" assertion으로 "E1.full-flow-quantities/return-added-purchase-arrival의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-474-api_response" assertion으로 "E1.full-flow-quantities/return-added-purchase-arrival에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-474-db_snapshot" assertion으로 "E1.full-flow-quantities/return-added-purchase-arrival에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-474" assertion으로 "E1.full-flow-quantities/return-added-purchase-arrival에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-474" assertion으로 "link-name-474: /data/hostObservation/extractor/rawRows/namedObservations/474/observationName의 실제 equals 기대값은 'return-added-purchase-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-474" assertion으로 "link-oracle-474: /data/hostObservation/extractor/rawRows/namedObservations/474/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-475-mcp" assertion으로 "E1.independent-goals-and-owners/logistics-goal의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-475-scenarios" assertion으로 "E1.independent-goals-and-owners/logistics-goal의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-475-api_response" assertion으로 "E1.independent-goals-and-owners/logistics-goal에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-475-db_snapshot" assertion으로 "E1.independent-goals-and-owners/logistics-goal에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-475" assertion으로 "E1.independent-goals-and-owners/logistics-goal에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-475" assertion으로 "link-name-475: /data/hostObservation/extractor/rawRows/namedObservations/475/observationName의 실제 equals 기대값은 'logistics-goal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-475" assertion으로 "link-oracle-475: /data/hostObservation/extractor/rawRows/namedObservations/475/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-476-mcp" assertion으로 "E1.independent-goals-and-owners/invoice-difference의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-476-scenarios" assertion으로 "E1.independent-goals-and-owners/invoice-difference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-476-api_response" assertion으로 "E1.independent-goals-and-owners/invoice-difference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-476-db_snapshot" assertion으로 "E1.independent-goals-and-owners/invoice-difference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-476" assertion으로 "E1.independent-goals-and-owners/invoice-difference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-476" assertion으로 "link-name-476: /data/hostObservation/extractor/rawRows/namedObservations/476/observationName의 실제 equals 기대값은 'invoice-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-476" assertion으로 "link-oracle-476: /data/hostObservation/extractor/rawRows/namedObservations/476/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-477-mcp" assertion으로 "E1.independent-goals-and-owners/QC-duty의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-477-scenarios" assertion으로 "E1.independent-goals-and-owners/QC-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-477-api_response" assertion으로 "E1.independent-goals-and-owners/QC-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-477-db_snapshot" assertion으로 "E1.independent-goals-and-owners/QC-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-477" assertion으로 "E1.independent-goals-and-owners/QC-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-477" assertion으로 "link-name-477: /data/hostObservation/extractor/rawRows/namedObservations/477/observationName의 실제 equals 기대값은 'QC-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-477" assertion으로 "link-oracle-477: /data/hostObservation/extractor/rawRows/namedObservations/477/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-478-mcp" assertion으로 "E1.independent-goals-and-owners/return-duty의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-478-scenarios" assertion으로 "E1.independent-goals-and-owners/return-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-478-api_response" assertion으로 "E1.independent-goals-and-owners/return-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-478-db_snapshot" assertion으로 "E1.independent-goals-and-owners/return-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-478" assertion으로 "E1.independent-goals-and-owners/return-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-478" assertion으로 "link-name-478: /data/hostObservation/extractor/rawRows/namedObservations/478/observationName의 실제 equals 기대값은 'return-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-478" assertion으로 "link-oracle-478: /data/hostObservation/extractor/rawRows/namedObservations/478/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-479-mcp" assertion으로 "E1.independent-goals-and-owners/settlement-duty의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-479-scenarios" assertion으로 "E1.independent-goals-and-owners/settlement-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-479-api_response" assertion으로 "E1.independent-goals-and-owners/settlement-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-479-db_snapshot" assertion으로 "E1.independent-goals-and-owners/settlement-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-479" assertion으로 "E1.independent-goals-and-owners/settlement-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-479" assertion으로 "link-name-479: /data/hostObservation/extractor/rawRows/namedObservations/479/observationName의 실제 equals 기대값은 'settlement-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-479" assertion으로 "link-oracle-479: /data/hostObservation/extractor/rawRows/namedObservations/479/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-480-mcp" assertion으로 "E1.independent-goals-and-owners/two-entry-snapshot의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-480-scenarios" assertion으로 "E1.independent-goals-and-owners/two-entry-snapshot의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-480-api_response" assertion으로 "E1.independent-goals-and-owners/two-entry-snapshot에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-480-db_snapshot" assertion으로 "E1.independent-goals-and-owners/two-entry-snapshot에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-480" assertion으로 "E1.independent-goals-and-owners/two-entry-snapshot에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-480" assertion으로 "link-name-480: /data/hostObservation/extractor/rawRows/namedObservations/480/observationName의 실제 equals 기대값은 'two-entry-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-480" assertion으로 "link-oracle-480: /data/hostObservation/extractor/rawRows/namedObservations/480/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-481-mcp" assertion으로 "E1.independent-goals-and-owners/logistics-closes-unrelated-duties의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-481-scenarios" assertion으로 "E1.independent-goals-and-owners/logistics-closes-unrelated-duties의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-481-api_response" assertion으로 "E1.independent-goals-and-owners/logistics-closes-unrelated-duties에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-481-db_snapshot" assertion으로 "E1.independent-goals-and-owners/logistics-closes-unrelated-duties에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-481" assertion으로 "E1.independent-goals-and-owners/logistics-closes-unrelated-duties에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-481" assertion으로 "link-name-481: /data/hostObservation/extractor/rawRows/namedObservations/481/observationName의 실제 equals 기대값은 'logistics-closes-unrelated-duties'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-481" assertion으로 "link-oracle-481: /data/hostObservation/extractor/rawRows/namedObservations/481/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-482-mcp" assertion으로 "E1.whole-runtime-and-model-reference/actual-runtime-observation의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-482-scenarios" assertion으로 "E1.whole-runtime-and-model-reference/actual-runtime-observation의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-482-api_response" assertion으로 "E1.whole-runtime-and-model-reference/actual-runtime-observation에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-482-db_snapshot" assertion으로 "E1.whole-runtime-and-model-reference/actual-runtime-observation에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-482" assertion으로 "E1.whole-runtime-and-model-reference/actual-runtime-observation에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-482" assertion으로 "link-name-482: /data/hostObservation/extractor/rawRows/namedObservations/482/observationName의 실제 equals 기대값은 'actual-runtime-observation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-482" assertion으로 "link-oracle-482: /data/hostObservation/extractor/rawRows/namedObservations/482/oracleId의 실제 equals 기대값은 'E1.whole-runtime-and-model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-483-mcp" assertion으로 "E1.whole-runtime-and-model-reference/model-reference의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-483-scenarios" assertion으로 "E1.whole-runtime-and-model-reference/model-reference의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-483-api_response" assertion으로 "E1.whole-runtime-and-model-reference/model-reference에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-483-db_snapshot" assertion으로 "E1.whole-runtime-and-model-reference/model-reference에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-483" assertion으로 "E1.whole-runtime-and-model-reference/model-reference에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-483" assertion으로 "link-name-483: /data/hostObservation/extractor/rawRows/namedObservations/483/observationName의 실제 equals 기대값은 'model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-483" assertion으로 "link-oracle-483: /data/hostObservation/extractor/rawRows/namedObservations/483/oracleId의 실제 equals 기대값은 'E1.whole-runtime-and-model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-484-mcp" assertion으로 "E2.overlapping-holds/physical-held의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-484-scenarios" assertion으로 "E2.overlapping-holds/physical-held의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-484-api_response" assertion으로 "E2.overlapping-holds/physical-held에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-484-db_snapshot" assertion으로 "E2.overlapping-holds/physical-held에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-484" assertion으로 "E2.overlapping-holds/physical-held에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-484" assertion으로 "link-name-484: /data/hostObservation/extractor/rawRows/namedObservations/484/observationName의 실제 equals 기대값은 'physical-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-484" assertion으로 "link-oracle-484: /data/hostObservation/extractor/rawRows/namedObservations/484/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-485-mcp" assertion으로 "E2.overlapping-holds/dispatched-after-QC-only-release의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-485-scenarios" assertion으로 "E2.overlapping-holds/dispatched-after-QC-only-release의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-485-api_response" assertion으로 "E2.overlapping-holds/dispatched-after-QC-only-release에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-485-db_snapshot" assertion으로 "E2.overlapping-holds/dispatched-after-QC-only-release에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-485" assertion으로 "E2.overlapping-holds/dispatched-after-QC-only-release에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-485" assertion으로 "link-name-485: /data/hostObservation/extractor/rawRows/namedObservations/485/observationName의 실제 equals 기대값은 'dispatched-after-QC-only-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-485" assertion으로 "link-oracle-485: /data/hostObservation/extractor/rawRows/namedObservations/485/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-486-mcp" assertion으로 "E2.overlapping-holds/recall-hold의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-486-scenarios" assertion으로 "E2.overlapping-holds/recall-hold의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-486-api_response" assertion으로 "E2.overlapping-holds/recall-hold에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-486-db_snapshot" assertion으로 "E2.overlapping-holds/recall-hold에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-486" assertion으로 "E2.overlapping-holds/recall-hold에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-486" assertion으로 "link-name-486: /data/hostObservation/extractor/rawRows/namedObservations/486/observationName의 실제 equals 기대값은 'recall-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-486" assertion으로 "link-oracle-486: /data/hostObservation/extractor/rawRows/namedObservations/486/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-487-mcp" assertion으로 "E2.overlapping-holds/candidate-as-confirmed-contamination의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-487-scenarios" assertion으로 "E2.overlapping-holds/candidate-as-confirmed-contamination의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-487-api_response" assertion으로 "E2.overlapping-holds/candidate-as-confirmed-contamination에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-487-db_snapshot" assertion으로 "E2.overlapping-holds/candidate-as-confirmed-contamination에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-487" assertion으로 "E2.overlapping-holds/candidate-as-confirmed-contamination에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-487" assertion으로 "link-name-487: /data/hostObservation/extractor/rawRows/namedObservations/487/observationName의 실제 equals 기대값은 'candidate-as-confirmed-contamination'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-487" assertion으로 "link-oracle-487: /data/hostObservation/extractor/rawRows/namedObservations/487/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-488-mcp" assertion으로 "E2.overlapping-holds/investigation-duty의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-488-scenarios" assertion으로 "E2.overlapping-holds/investigation-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-488-api_response" assertion으로 "E2.overlapping-holds/investigation-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-488-db_snapshot" assertion으로 "E2.overlapping-holds/investigation-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-488" assertion으로 "E2.overlapping-holds/investigation-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-488" assertion으로 "link-name-488: /data/hostObservation/extractor/rawRows/namedObservations/488/observationName의 실제 equals 기대값은 'investigation-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-488" assertion으로 "link-oracle-488: /data/hostObservation/extractor/rawRows/namedObservations/488/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-489-mcp" assertion으로 "E2.same-25-not-50/unique-recovered의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-489-scenarios" assertion으로 "E2.same-25-not-50/unique-recovered의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-489-api_response" assertion으로 "E2.same-25-not-50/unique-recovered에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-489-db_snapshot" assertion으로 "E2.same-25-not-50/unique-recovered에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-489" assertion으로 "E2.same-25-not-50/unique-recovered에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-489" assertion으로 "link-name-489: /data/hostObservation/extractor/rawRows/namedObservations/489/observationName의 실제 equals 기대값은 'unique-recovered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-489" assertion으로 "link-oracle-489: /data/hostObservation/extractor/rawRows/namedObservations/489/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-490-mcp" assertion으로 "E2.same-25-not-50/unique-finally-processed의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-490-scenarios" assertion으로 "E2.same-25-not-50/unique-finally-processed의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-490-api_response" assertion으로 "E2.same-25-not-50/unique-finally-processed에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-490-db_snapshot" assertion으로 "E2.same-25-not-50/unique-finally-processed에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-490" assertion으로 "E2.same-25-not-50/unique-finally-processed에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-490" assertion으로 "link-name-490: /data/hostObservation/extractor/rawRows/namedObservations/490/observationName의 실제 equals 기대값은 'unique-finally-processed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-490" assertion으로 "link-oracle-490: /data/hostObservation/extractor/rawRows/namedObservations/490/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-491-mcp" assertion으로 "E2.same-25-not-50/unknown의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-491-scenarios" assertion으로 "E2.same-25-not-50/unknown의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-491-api_response" assertion으로 "E2.same-25-not-50/unknown에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-491-db_snapshot" assertion으로 "E2.same-25-not-50/unknown에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-491" assertion으로 "E2.same-25-not-50/unknown에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-491" assertion으로 "link-name-491: /data/hostObservation/extractor/rawRows/namedObservations/491/observationName의 실제 equals 기대값은 'unknown'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-491" assertion으로 "link-oracle-491: /data/hostObservation/extractor/rawRows/namedObservations/491/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-492-mcp" assertion으로 "E2.same-25-not-50/25-plus-25-equals50의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-492-scenarios" assertion으로 "E2.same-25-not-50/25-plus-25-equals50의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-492-api_response" assertion으로 "E2.same-25-not-50/25-plus-25-equals50에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-492-db_snapshot" assertion으로 "E2.same-25-not-50/25-plus-25-equals50에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-492" assertion으로 "E2.same-25-not-50/25-plus-25-equals50에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-492" assertion으로 "link-name-492: /data/hostObservation/extractor/rawRows/namedObservations/492/observationName의 실제 equals 기대값은 '25-plus-25-equals50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-492" assertion으로 "link-oracle-492: /data/hostObservation/extractor/rawRows/namedObservations/492/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-493-mcp" assertion으로 "E2.same-25-not-50/unapproved-unknown-close의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-493-scenarios" assertion으로 "E2.same-25-not-50/unapproved-unknown-close의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-493-api_response" assertion으로 "E2.same-25-not-50/unapproved-unknown-close에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-493-db_snapshot" assertion으로 "E2.same-25-not-50/unapproved-unknown-close에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-493" assertion으로 "E2.same-25-not-50/unapproved-unknown-close에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-493" assertion으로 "link-name-493: /data/hostObservation/extractor/rawRows/namedObservations/493/observationName의 실제 equals 기대값은 'unapproved-unknown-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-493" assertion으로 "link-oracle-493: /data/hostObservation/extractor/rawRows/namedObservations/493/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-494-mcp" assertion으로 "E2.same-25-not-50/status-axes의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-494-scenarios" assertion으로 "E2.same-25-not-50/status-axes의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-494-api_response" assertion으로 "E2.same-25-not-50/status-axes에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-494-db_snapshot" assertion으로 "E2.same-25-not-50/status-axes에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-494" assertion으로 "E2.same-25-not-50/status-axes에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-494" assertion으로 "link-name-494: /data/hostObservation/extractor/rawRows/namedObservations/494/observationName의 실제 equals 기대값은 'status-axes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-494" assertion으로 "link-oracle-494: /data/hostObservation/extractor/rawRows/namedObservations/494/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-495-mcp" assertion으로 "E2.exception-responsibility/unauthorized-exception-close의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-495-scenarios" assertion으로 "E2.exception-responsibility/unauthorized-exception-close의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-495-api_response" assertion으로 "E2.exception-responsibility/unauthorized-exception-close에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-495-db_snapshot" assertion으로 "E2.exception-responsibility/unauthorized-exception-close에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-495" assertion으로 "E2.exception-responsibility/unauthorized-exception-close에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-495" assertion으로 "link-name-495: /data/hostObservation/extractor/rawRows/namedObservations/495/observationName의 실제 equals 기대값은 'unauthorized-exception-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-495" assertion으로 "link-oracle-495: /data/hostObservation/extractor/rawRows/namedObservations/495/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-496-mcp" assertion으로 "E2.exception-responsibility/exception-unresolved-scope의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-496-scenarios" assertion으로 "E2.exception-responsibility/exception-unresolved-scope의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-496-api_response" assertion으로 "E2.exception-responsibility/exception-unresolved-scope에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-496-db_snapshot" assertion으로 "E2.exception-responsibility/exception-unresolved-scope에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-496" assertion으로 "E2.exception-responsibility/exception-unresolved-scope에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-496" assertion으로 "link-name-496: /data/hostObservation/extractor/rawRows/namedObservations/496/observationName의 실제 equals 기대값은 'exception-unresolved-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-496" assertion으로 "link-oracle-496: /data/hostObservation/extractor/rawRows/namedObservations/496/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-497-mcp" assertion으로 "E2.exception-responsibility/exception-residual-duty의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-497-scenarios" assertion으로 "E2.exception-responsibility/exception-residual-duty의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-497-api_response" assertion으로 "E2.exception-responsibility/exception-residual-duty에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-497-db_snapshot" assertion으로 "E2.exception-responsibility/exception-residual-duty에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-497" assertion으로 "E2.exception-responsibility/exception-residual-duty에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-497" assertion으로 "link-name-497: /data/hostObservation/extractor/rawRows/namedObservations/497/observationName의 실제 equals 기대값은 'exception-residual-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-497" assertion으로 "link-oracle-497: /data/hostObservation/extractor/rawRows/namedObservations/497/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "profile-links-498-mcp" assertion으로 "E2.exception-responsibility/exception-evidence의 필수 계층 mcp profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "profile-links-498-scenarios" assertion으로 "E2.exception-responsibility/exception-evidence의 필수 계층 scenarios profile에서 실제 PASS assertion link가 한 건 이상 있다. 다른 profile의 PASS로 대신하지 않는다."를 확인한다
    그러면 "artifact-kind-498-api_response" assertion으로 "E2.exception-responsibility/exception-evidence에는 catalog artifactKind api_response의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "artifact-kind-498-db_snapshot" assertion으로 "E2.exception-responsibility/exception-evidence에는 catalog artifactKind db_snapshot의 실제 PASS artifact가 path·hash·크기·case·profile과 함께 연결된다."를 확인한다
    그러면 "concrete-links-498" assertion으로 "E2.exception-responsibility/exception-evidence에는 실제 PASS assertion link가 하나 이상 있어야 한다."를 확인한다
    그러면 "link-name-498" assertion으로 "link-name-498: /data/hostObservation/extractor/rawRows/namedObservations/498/observationName의 실제 equals 기대값은 'exception-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-498" assertion으로 "link-oracle-498: /data/hostObservation/extractor/rawRows/namedObservations/498/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 증거 version·command·exit·계층 분리·선행 gate
    먼저 사례 파일 "verification/cases/T25/case.json"의 "evidence-wrapper-fields"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "input-snapshot-kind" assertion으로 "input-snapshot-kind: /data/hostObservation/extractor/rawRows/input/snapshotKind의 실제 equals 기대값은 'REQUIRED_PATH_RUNTIME_EVIDENCE'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "manifest-required-fields" assertion으로 "manifest-required-fields: 비어 있지 않은 실제 원행마다 profile, codeCommit, schemaVersion, definitionVersion, evaluatorVersion, skillVersion, toolVersion, clientVersion, modelVersion, dbVersion, buildpackVersion, fixtureHash, command, timestamp, expected, observed, artifactRefs, status를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "required-wrapper-profiles" assertion으로 "required-wrapper-profiles: /data/hostObservation/extractor/rawRows/wrappers의 실제 profile는 고정한 9개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wrapper-actual-fields" assertion으로 "wrapper-actual-fields: 비어 있지 않은 실제 원행마다 internalCommand, toolVersions, exitCode, status, dependencyProfiles를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "separate-evidence-classes" assertion으로 "separate-evidence-classes: /data/hostObservation/extractor/rawRows/evidenceClasses의 실제 값는 고정한 9개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "regulatory-source-date-reviewer" assertion으로 "법규 검토는 출처·관할·적용일·검토자·검토시각·범위·artifact·상태를 기록한다(plan §13.4). 미검토를 기록 없음으로 숨기지 않는다."를 확인한다
    그러면 "regulatory-no-waiver" assertion으로 "regulatory-no-waiver: /data/hostObservation/extractor/rawRows/regulatoryReviews의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "model-cost-approval" assertion으로 "실모델 평가는 실행 전 비용 승인(client/model/반복/비용 상한/통화)을 별도로 기록한다(plan §13.4, R8)."를 확인한다
    그러면 "model-gate-pass" assertion으로 "S6 필수 경로의 승인된 실모델 평가는 PASS 증거를 가져야 한다. 미실행을 비대상이나 waiver로 바꾸지 않는다."를 확인한다
    그러면 "mcp-records-protocol" assertion으로 "mcp profile 기록은 실제 MCP protocol version·tool·DB version과 artifact를 가진다(plan §13.4, §9.1). 다른 profile 기록으로 대신하지 않는다."를 확인한다
    그러면 "skills-records-client" assertion으로 "skills profile 기록은 실제 skill·client·protocol version과 artifact를 가진다(plan §9.2, §13.4)."를 확인한다
    그러면 "scenarios-wrapper-db" assertion으로 "./verify scenarios wrapper는 실제 API·DB 실행의 내부 command·tool version·DB version·exit code를 드러낸다."를 확인한다
    그러면 "mcp-wrapper-protocol" assertion으로 "./verify mcp wrapper는 실제 내부 command·tool version·MCP protocol version·exit code를 드러낸다."를 확인한다
    그러면 "db-snapshot-artifacts" assertion으로 "증거 manifest는 DB 계층 관찰의 db_snapshot artifact를 hash·크기·case·profile·DB version과 함께 기록한다. API 응답만으로 DB 계층을 대신하지 않는다."를 확인한다
    그러면 "api-response-artifacts" assertion으로 "증거 manifest는 API 계층의 api_response artifact를 hash·크기·case·profile과 함께 기록한다."를 확인한다
    그러면 "protocol-transcript-artifacts" assertion으로 "증거 manifest는 MCP 계층의 실제 wire protocol_transcript를 hash·크기·protocol version과 함께 기록한다. wrapper 성공 표시만으로 MCP 실행을 주장하지 않는다."를 확인한다
    그러면 "dependencies-mcp" assertion으로 "dependencies-mcp: /data/hostObservation/extractor/rawRows/dependencies/mcp의 실제 equals 기대값은 ['schema', 'contracts', 'scenarios', 'recovery']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "dependencies-model" assertion으로 "dependencies-model: /data/hostObservation/extractor/rawRows/dependencies/model의 실제 equals 기대값은 ['schema', 'contracts', 'scenarios', 'recovery', 'mcp', 'skills']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실행 증거 missing-law-source mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-law-source"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "baseline-evidence-valid" assertion으로 "변조 전 실제 증거 입력은 유효해야 한다. 변조가 만든 PASS→FAIL 전이만 거부 증거다."를 확인한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 missing-cost-approval mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-cost-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "baseline-evidence-valid" assertion으로 "변조 전 실제 증거 입력은 유효해야 한다. 변조가 만든 PASS→FAIL 전이만 거부 증거다."를 확인한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 missing-command-version mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-command-version"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "baseline-evidence-valid" assertion으로 "변조 전 실제 증거 입력은 유효해야 한다. 변조가 만든 PASS→FAIL 전이만 거부 증거다."를 확인한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 fake-wrapper-success mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "fake-wrapper-success"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "baseline-evidence-valid" assertion으로 "변조 전 실제 증거 입력은 유효해야 한다. 변조가 만든 PASS→FAIL 전이만 거부 증거다."를 확인한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: M60×3 binding 준비와 실제 model/usage NOT_RUN
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-preparation-not-runtime"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "input-snapshot-kind" assertion으로 "모델 binding 준비 산출물만 읽는다. 실제 실행 manifest의 NOT_RUN을 고정하지 않는다."를 확인한다
    그러면 "60-source-ids" assertion으로 "60-source-ids: /data/hostObservation/extractor/rawRows/corpus/ids의 실제 값는 고정한 60개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "repeat-plan" assertion으로 "repeat-plan: /data/hostObservation/extractor/rawRows/corpus/repetitions의 실제 equals 기대값은 3다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "foreign10" assertion으로 "foreign10: /data/hostObservation/extractor/rawRows/corpus/foreignOrMixedCount의 실제 decimalAtLeast 기대값은 '10'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "categories-exact" assertion으로 "categories-exact: /data/hostObservation/extractor/rawRows/corpus/categories의 실제 ['category', 'count']는 고정한 5개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "proposed95" assertion으로 "proposed95: /data/hostObservation/extractor/rawRows/acceptance/proposed/clearStructuredIntentRate의 실제 equals 기대값은 '>=0.95'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "not-business-SLA" assertion으로 "not-business-SLA: /data/hostObservation/extractor/rawRows/acceptance/status의 실제 equals 기대값은 'PROPOSED_PENDING_R8'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-gate-not-run" assertion으로 "model-gate-not-run: /data/hostObservation/extractor/rawRows/gate/modelStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cost-gate-not-run" assertion으로 "cost-gate-not-run: /data/hostObservation/extractor/rawRows/gate/usageStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-attempts-zero" assertion으로 "actual-attempts-zero: /data/hostObservation/extractor/rawRows/attempts의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "no-total-cost-zero-fill" assertion으로 "no-total-cost-zero-fill: /data/hostObservation/extractor/rawRows/usage/totalCostState의 실제 equals 기대값은 'MISSING'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-whole-UAT-PASS" assertion으로 "no-whole-UAT-PASS: /data/hostObservation/extractor/rawRows/uatPasses의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "proposed-zero-ambiguousImproperExecution" assertion으로 "proposed-zero-ambiguousImproperExecution: /data/hostObservation/extractor/rawRows/acceptance/proposed/ambiguousImproperExecution의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "proposed-zero-unauthorized" assertion으로 "proposed-zero-unauthorized: /data/hostObservation/extractor/rawRows/acceptance/proposed/unauthorized의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "proposed-zero-duplicate" assertion으로 "proposed-zero-duplicate: /data/hostObservation/extractor/rawRows/acceptance/proposed/duplicate의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "proposed-zero-falseCompletion" assertion으로 "proposed-zero-falseCompletion: /data/hostObservation/extractor/rawRows/acceptance/proposed/falseCompletion의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage drop-binding mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-drop-binding"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "baseline-model-input-valid" assertion으로 "baseline-model-input-valid: /data/hostObservation/extractor/rawRows/baseline/validation/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage missing-attempt-usage mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-missing-attempt-usage"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "baseline-model-input-valid" assertion으로 "baseline-model-input-valid: /data/hostObservation/extractor/rawRows/baseline/validation/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage null-usage-as-zero mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-null-usage-as-zero"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "baseline-model-input-valid" assertion으로 "baseline-model-input-valid: /data/hostObservation/extractor/rawRows/baseline/validation/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage partial-cost-complete mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-partial-cost-complete"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "baseline-model-input-valid" assertion으로 "baseline-model-input-valid: /data/hostObservation/extractor/rawRows/baseline/validation/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage unapproved-business-SLA mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-unapproved-business-SLA"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "baseline-model-input-valid" assertion으로 "baseline-model-input-valid: /data/hostObservation/extractor/rawRows/baseline/validation/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 승인된 실제 M60×3·attempt/retry usage·비용·version 인수
    먼저 사례 파일 "verification/cases/T25/case.json"의 "actual-model-usage-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "case-repeat-identities" assertion으로 "case-repeat-identities: /data/hostObservation/extractor/rawRows/modelCaseRuns의 실제 ['caseId', 'repeat']는 고정한 180개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "every-attempt-retry-usage" assertion으로 "every-attempt-retry-usage: 비어 있지 않은 실제 원행마다 attemptId, caseId, turnId, repeat, retryIndex, inputTokens, outputTokens, clientVersion, modelVersion, promptHash, skillHash, definitionVersion, priceBasis, currency, cost, startedAt, completedAt, protocolTranscriptRef, skillLoadingTraceRef를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "no-incomplete-usage" assertion으로 "no-incomplete-usage: /data/hostObservation/extractor/rawRows/validation/issues의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "clear-structure95" assertion으로 "clear-structure95: /data/hostObservation/extractor/rawRows/metrics/clearStructuredIntentRate의 실제 decimalAtLeast 기대값은 '0.95'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "R8-confirmed" assertion으로 "R8-confirmed: /data/hostObservation/extractor/rawRows/acceptance/status의 실제 equals 기대값은 'CONFIRMED_R8'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "usage-gaps-optional" assertion으로 "usage-gaps-optional: /data/hostObservation/extractor/rawRows/invalidUnprovidedMetrics의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "pinned-authorized-versions" assertion으로 "pinned-authorized-versions: model-records의 /data/hostObservation/extractor/rawRows/versions와 model-records의 /data/hostObservation/extractor/rawRows/authorization/versions를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "approved-repeat-count" assertion으로 "approved-repeat-count: /data/hostObservation/extractor/rawRows/authorization/repetitions의 실제 equals 기대값은 3다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "case-run-protocol-transcripts" assertion으로 "M60×3의 모든 case/반복은 실제 MCP wire transcript가 검증된 실행이다. transcript 없는 모델 응답은 수용 지표에 들어가지 않는다(plan §9.1, §13.3)."를 확인한다
    그러면 "case-run-skill-loading" assertion으로 "M60×3의 모든 case/반복에서 host의 skill 검색 노출과 본문 loading이 실제 관찰됐다. skill hash만으로 loading을 대신하지 않는다(plan §9.2)."를 확인한다
    그러면 "attempt-trace-artifacts" assertion으로 "attempt/retry마다 protocolTranscriptRef·skillLoadingTraceRef가 가리키는 artifact의 hash를 검증하지 못한 행은 0개다."를 확인한다
    그러면 "actual-zero-ambiguousImproperExecution" assertion으로 "actual-zero-ambiguousImproperExecution: /data/hostObservation/extractor/rawRows/metrics/ambiguousImproperExecution의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-unauthorized" assertion으로 "actual-zero-unauthorized: /data/hostObservation/extractor/rawRows/metrics/unauthorized의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-duplicate" assertion으로 "actual-zero-duplicate: /data/hostObservation/extractor/rawRows/metrics/duplicate의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-falseCompletion" assertion으로 "actual-zero-falseCompletion: /data/hostObservation/extractor/rawRows/metrics/falseCompletion의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-p50LatencyMillis" assertion으로 "metric-p50LatencyMillis: /data/hostObservation/extractor/rawRows/metrics/p50LatencyMillis의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-p95LatencyMillis" assertion으로 "metric-p95LatencyMillis: /data/hostObservation/extractor/rawRows/metrics/p95LatencyMillis의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-clarificationRate" assertion으로 "metric-clarificationRate: /data/hostObservation/extractor/rawRows/metrics/clarificationRate의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-totalCost" assertion으로 "metric-totalCost: /data/hostObservation/extractor/rawRows/metrics/totalCost의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
