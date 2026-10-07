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
    그러면 "prepared-only" assertion으로 "prepared-only: /data/hostObservation/extractor/rawRows/gate/preparationStatus의 실제 equals 기대값은 'PREPARED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "runtime-not-run" assertion으로 "runtime-not-run: /data/hostObservation/extractor/rawRows/gate/runtimeStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "whole-gate-open" assertion으로 "whole-gate-open: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "semantic-review-required" assertion으로 "semantic-review-required: /data/hostObservation/extractor/rawRows/gate/semanticOracleEquivalence의 실제 equals 기대값은 'REQUIRES_CASE_REVIEW'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-runtime-artifacts-fabricated" assertion으로 "no-runtime-artifacts-fabricated: /data/hostObservation/extractor/rawRows/runtimeArtifacts의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 dropOracle 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-dropOracle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
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
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'removeRuntimeArtifact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 replaceWithStubPass 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-replaceWithStubPass"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'replaceWithStubPass'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 skipCase 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-skipCase"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
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
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'partialPass'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 독립41 case/D26 catalog 연결과 confirmedViolation 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "coverage-confirmedViolation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "all-41-cases" assertion으로 "all-41-cases: /data/hostObservation/extractor/rawRows/requiredCases의 실제 값는 고정한 41개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-D26" assertion으로 "all-D26: /data/hostObservation/extractor/rawRows/requirementIds의 실제 값는 고정한 26개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "catalog-oracle-ids" assertion으로 "catalog-oracle-ids: /data/hostObservation/extractor/rawRows/catalogOracles의 실제 oracleId는 고정한 122개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "all-named-observation-tuples" assertion으로 "all-named-observation-tuples: /data/hostObservation/extractor/rawRows/catalogObservations의 실제 ['oracleId', 'name', 'type', 'scope', 'operator']는 고정한 499개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-rejected" assertion으로 "mutation-rejected: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-mutation-code" assertion으로 "specific-mutation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "mutation-input-case" assertion으로 "mutation-input-case: /data/hostObservation/extractor/rawRows/mutatedInput/caseId의 실제 equals 기대값은 'T20'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "mutation-kind" assertion으로 "mutation-kind: /data/hostObservation/extractor/rawRows/mutatedInput/mutation의 실제 equals 기대값은 'confirmedViolation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-gate-pass" assertion으로 "no-gate-pass: /data/hostObservation/extractor/rawRows/gate/gateComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 전체 catalog named observation의 실제 assertion·artifact 연결
    먼저 사례 파일 "verification/cases/T25/case.json"의 "runtime-links-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "coverage" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-runtime-sources" 행동을 수행한다
    그러면 "all-case-profile-artifacts" assertion으로 "all-case-profile-artifacts: 비어 있지 않은 실제 원행마다 caseId, subcaseId, profile, assertionId, path, sha256, sizeBytes, scope, fixtureHash, codeCommit, command, expected, observed, exitCode를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "no-link-violations" assertion으로 "no-link-violations: /data/hostObservation/extractor/rawRows/validation/issues의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "content-review-evidence" assertion으로 "content-review-evidence: /data/hostObservation/extractor/rawRows/semanticReview/status의 실제 equals 기대값은 'PASS'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-000" assertion으로 "concrete-links-000: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-000" assertion으로 "link-name-000: /data/hostObservation/extractor/rawRows/namedObservations/0/observationName의 실제 equals 기대값은 'same-world'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-000" assertion으로 "link-oracle-000: /data/hostObservation/extractor/rawRows/namedObservations/0/oracleId의 실제 equals 기대값은 'T01.two-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-001" assertion으로 "concrete-links-001: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-001" assertion으로 "link-name-001: /data/hostObservation/extractor/rawRows/namedObservations/1/observationName의 실제 equals 기대값은 'answers-grounded'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-001" assertion으로 "link-oracle-001: /data/hostObservation/extractor/rawRows/namedObservations/1/oracleId의 실제 equals 기대값은 'T01.five-competency-questions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-002" assertion으로 "concrete-links-002: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-002" assertion으로 "link-name-002: /data/hostObservation/extractor/rawRows/namedObservations/2/observationName의 실제 equals 기대값은 'excluded-effects-created'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-002" assertion으로 "link-oracle-002: /data/hostObservation/extractor/rawRows/namedObservations/2/oracleId의 실제 equals 기대값은 'T01.excluded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-003" assertion으로 "concrete-links-003: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-003" assertion으로 "link-name-003: /data/hostObservation/extractor/rawRows/namedObservations/3/observationName의 실제 equals 기대값은 'unsupported-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-003" assertion으로 "link-oracle-003: /data/hostObservation/extractor/rawRows/namedObservations/3/oracleId의 실제 equals 기대값은 'T01.excluded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-004" assertion으로 "concrete-links-004: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-004" assertion으로 "link-name-004: /data/hostObservation/extractor/rawRows/namedObservations/4/observationName의 실제 equals 기대값은 'distinct-item-ids'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-004" assertion으로 "link-oracle-004: /data/hostObservation/extractor/rawRows/namedObservations/4/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-005" assertion으로 "concrete-links-005: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-005" assertion으로 "link-name-005: /data/hostObservation/extractor/rawRows/namedObservations/5/observationName의 실제 equals 기대값은 'distinct-lot-ids'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-005" assertion으로 "link-oracle-005: /data/hostObservation/extractor/rawRows/namedObservations/5/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-006" assertion으로 "concrete-links-006: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-006" assertion으로 "link-name-006: /data/hostObservation/extractor/rawRows/namedObservations/6/observationName의 실제 equals 기대값은 'implicit-merges'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-006" assertion으로 "link-oracle-006: /data/hostObservation/extractor/rawRows/namedObservations/6/oracleId의 실제 equals 기대값은 'T02.issuer-identity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-007" assertion으로 "concrete-links-007: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-007" assertion으로 "link-name-007: /data/hostObservation/extractor/rawRows/namedObservations/7/observationName의 실제 equals 기대값은 'code-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-007" assertion으로 "link-oracle-007: /data/hostObservation/extractor/rawRows/namedObservations/7/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-008" assertion으로 "concrete-links-008: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-008" assertion으로 "link-name-008: /data/hostObservation/extractor/rawRows/namedObservations/8/observationName의 실제 equals 기대값은 'silent-remap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-008" assertion으로 "link-oracle-008: /data/hostObservation/extractor/rawRows/namedObservations/8/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-009" assertion으로 "concrete-links-009: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-009" assertion으로 "link-name-009: /data/hostObservation/extractor/rawRows/namedObservations/9/observationName의 실제 equals 기대값은 'code-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-009" assertion으로 "link-oracle-009: /data/hostObservation/extractor/rawRows/namedObservations/9/oracleId의 실제 equals 기대값은 'T02.identifier-period-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-010" assertion으로 "concrete-links-010: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-010" assertion으로 "link-name-010: /data/hostObservation/extractor/rawRows/namedObservations/10/observationName의 실제 equals 기대값은 'converted-content'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-010" assertion으로 "link-oracle-010: /data/hostObservation/extractor/rawRows/namedObservations/10/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-011" assertion으로 "concrete-links-011: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-011" assertion으로 "link-name-011: /data/hostObservation/extractor/rawRows/namedObservations/11/observationName의 실제 equals 기대값은 'conversion-created-inventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-011" assertion으로 "link-oracle-011: /data/hostObservation/extractor/rawRows/namedObservations/11/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-012" assertion으로 "concrete-links-012: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-012" assertion으로 "link-name-012: /data/hostObservation/extractor/rawRows/namedObservations/12/observationName의 실제 equals 기대값은 'conversion-created-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-012" assertion으로 "link-oracle-012: /data/hostObservation/extractor/rawRows/namedObservations/12/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-013" assertion으로 "concrete-links-013: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-013" assertion으로 "link-name-013: /data/hostObservation/extractor/rawRows/namedObservations/13/observationName의 실제 equals 기대값은 'repackaging-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-013" assertion으로 "link-oracle-013: /data/hostObservation/extractor/rawRows/namedObservations/13/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-014" assertion으로 "concrete-links-014: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-014" assertion으로 "link-name-014: /data/hostObservation/extractor/rawRows/namedObservations/14/observationName의 실제 equals 기대값은 'substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-014" assertion으로 "link-oracle-014: /data/hostObservation/extractor/rawRows/namedObservations/14/oracleId의 실제 equals 기대값은 'T02.conversion-not-substitution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-015" assertion으로 "concrete-links-015: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-015" assertion으로 "link-name-015: /data/hostObservation/extractor/rawRows/namedObservations/15/observationName의 실제 equals 기대값은 'active-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-015" assertion으로 "link-oracle-015: /data/hostObservation/extractor/rawRows/namedObservations/15/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-016" assertion으로 "concrete-links-016: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-016" assertion으로 "link-name-016: /data/hostObservation/extractor/rawRows/namedObservations/16/observationName의 실제 equals 기대값은 'parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-016" assertion으로 "link-oracle-016: /data/hostObservation/extractor/rawRows/namedObservations/16/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-017" assertion으로 "concrete-links-017: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-017" assertion으로 "link-name-017: /data/hostObservation/extractor/rawRows/namedObservations/17/observationName의 실제 equals 기대값은 'allocation-transferred'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-017" assertion으로 "link-oracle-017: /data/hostObservation/extractor/rawRows/namedObservations/17/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-018" assertion으로 "concrete-links-018: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-018" assertion으로 "link-name-018: /data/hostObservation/extractor/rawRows/namedObservations/18/observationName의 실제 equals 기대값은 'allocation-copies'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-018" assertion으로 "link-oracle-018: /data/hostObservation/extractor/rawRows/namedObservations/18/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-019" assertion으로 "concrete-links-019: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-019" assertion으로 "link-name-019: /data/hostObservation/extractor/rawRows/namedObservations/19/observationName의 실제 equals 기대값은 'atomic-genealogy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-019" assertion으로 "link-oracle-019: /data/hostObservation/extractor/rawRows/namedObservations/19/oracleId의 실제 equals 기대값은 'T03.split-merge-conservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-020" assertion으로 "concrete-links-020: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-020" assertion으로 "link-name-020: /data/hostObservation/extractor/rawRows/namedObservations/20/observationName의 실제 equals 기대값은 'pallet-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-020" assertion으로 "link-oracle-020: /data/hostObservation/extractor/rawRows/namedObservations/20/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-021" assertion으로 "concrete-links-021: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-021" assertion으로 "link-name-021: /data/hostObservation/extractor/rawRows/namedObservations/21/observationName의 실제 equals 기대값은 'lot-membership'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-021" assertion으로 "link-oracle-021: /data/hostObservation/extractor/rawRows/namedObservations/21/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-022" assertion으로 "concrete-links-022: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-022" assertion으로 "link-name-022: /data/hostObservation/extractor/rawRows/namedObservations/22/observationName의 실제 equals 기대값은 'cross-lot-segment-merge'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-022" assertion으로 "link-oracle-022: /data/hostObservation/extractor/rawRows/namedObservations/22/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-023" assertion으로 "concrete-links-023: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-023" assertion으로 "link-name-023: /data/hostObservation/extractor/rawRows/namedObservations/23/observationName의 실제 equals 기대값은 'membership-time'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-023" assertion으로 "link-oracle-023: /data/hostObservation/extractor/rawRows/namedObservations/23/oracleId의 실제 equals 기대값은 'T03.logistics-multiple-lots'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-024" assertion으로 "concrete-links-024: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-024" assertion으로 "link-name-024: /data/hostObservation/extractor/rawRows/namedObservations/24/observationName의 실제 equals 기대값은 'invalid-quantity-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-024" assertion으로 "link-oracle-024: /data/hostObservation/extractor/rawRows/namedObservations/24/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-025" assertion으로 "concrete-links-025: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-025" assertion으로 "link-name-025: /data/hostObservation/extractor/rawRows/namedObservations/25/observationName의 실제 equals 기대값은 'invalid-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-025" assertion으로 "link-oracle-025: /data/hostObservation/extractor/rawRows/namedObservations/25/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-026" assertion으로 "concrete-links-026: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-026" assertion으로 "link-name-026: /data/hostObservation/extractor/rawRows/namedObservations/26/observationName의 실제 equals 기대값은 'invalid-results'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-026" assertion으로 "link-oracle-026: /data/hostObservation/extractor/rawRows/namedObservations/26/oracleId의 실제 equals 기대값은 'T03.cycle-and-retired-parent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-027" assertion으로 "concrete-links-027: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-027" assertion으로 "link-name-027: /data/hostObservation/extractor/rawRows/namedObservations/27/observationName의 실제 equals 기대값은 'source-affected'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-027" assertion으로 "link-oracle-027: /data/hostObservation/extractor/rawRows/namedObservations/27/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-028" assertion으로 "concrete-links-028: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-028" assertion으로 "link-name-028: /data/hostObservation/extractor/rawRows/namedObservations/28/observationName의 실제 equals 기대값은 'current-candidate-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-028" assertion으로 "link-oracle-028: /data/hostObservation/extractor/rawRows/namedObservations/28/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-029" assertion으로 "concrete-links-029: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-029" assertion으로 "link-name-029: /data/hostObservation/extractor/rawRows/namedObservations/29/observationName의 실제 equals 기대값은 'arbitrary-clean-selection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-029" assertion으로 "link-oracle-029: /data/hostObservation/extractor/rawRows/namedObservations/29/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-030" assertion으로 "concrete-links-030: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-030" assertion으로 "link-name-030: /data/hostObservation/extractor/rawRows/namedObservations/30/observationName의 실제 equals 기대값은 'trace-certainty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-030" assertion으로 "link-oracle-030: /data/hostObservation/extractor/rawRows/namedObservations/30/oracleId의 실제 equals 기대값은 'T03.indistinguishable-mixture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-031" assertion으로 "concrete-links-031: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-031" assertion으로 "link-name-031: /data/hostObservation/extractor/rawRows/namedObservations/31/observationName의 실제 equals 기대값은 'invalid-decimals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-031" assertion으로 "link-oracle-031: /data/hostObservation/extractor/rawRows/namedObservations/31/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-032" assertion으로 "concrete-links-032: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-032" assertion으로 "link-name-032: /data/hostObservation/extractor/rawRows/namedObservations/32/observationName의 실제 equals 기대값은 'rounded-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-032" assertion으로 "link-oracle-032: /data/hostObservation/extractor/rawRows/namedObservations/32/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-033" assertion으로 "concrete-links-033: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-033" assertion으로 "link-name-033: /data/hostObservation/extractor/rawRows/namedObservations/33/observationName의 실제 equals 기대값은 'wire-decimals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-033" assertion으로 "link-oracle-033: /data/hostObservation/extractor/rawRows/namedObservations/33/oracleId의 실제 equals 기대값은 'T04.decimal-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-034" assertion으로 "concrete-links-034: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-034" assertion으로 "link-name-034: /data/hostObservation/extractor/rawRows/namedObservations/34/observationName의 실제 equals 기대값은 'held-after-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-034" assertion으로 "link-oracle-034: /data/hostObservation/extractor/rawRows/namedObservations/34/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-035" assertion으로 "concrete-links-035: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-035" assertion으로 "link-name-035: /data/hostObservation/extractor/rawRows/namedObservations/35/observationName의 실제 equals 기대값은 'eligible-after-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-035" assertion으로 "link-oracle-035: /data/hostObservation/extractor/rawRows/namedObservations/35/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-036" assertion으로 "concrete-links-036: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-036" assertion으로 "link-name-036: /data/hostObservation/extractor/rawRows/namedObservations/36/observationName의 실제 equals 기대값은 'held-after-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-036" assertion으로 "link-oracle-036: /data/hostObservation/extractor/rawRows/namedObservations/36/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-037" assertion으로 "concrete-links-037: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-037" assertion으로 "link-name-037: /data/hostObservation/extractor/rawRows/namedObservations/37/observationName의 실제 equals 기대값은 'physical-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-037" assertion으로 "link-oracle-037: /data/hostObservation/extractor/rawRows/namedObservations/37/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-038" assertion으로 "concrete-links-038: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-038" assertion으로 "link-name-038: /data/hostObservation/extractor/rawRows/namedObservations/38/observationName의 실제 equals 기대값은 'decrease-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-038" assertion으로 "link-oracle-038: /data/hostObservation/extractor/rawRows/namedObservations/38/oracleId의 실제 equals 기대값은 'T04.hold-versus-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-039" assertion으로 "concrete-links-039: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-039" assertion으로 "link-name-039: /data/hostObservation/extractor/rawRows/namedObservations/39/observationName의 실제 equals 기대값은 'rollback-ledger-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-039" assertion으로 "link-oracle-039: /data/hostObservation/extractor/rawRows/namedObservations/39/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-040" assertion으로 "concrete-links-040: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-040" assertion으로 "link-name-040: /data/hostObservation/extractor/rawRows/namedObservations/40/observationName의 실제 equals 기대값은 'rollback-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-040" assertion으로 "link-oracle-040: /data/hostObservation/extractor/rawRows/namedObservations/40/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-041" assertion으로 "concrete-links-041: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-041" assertion으로 "link-name-041: /data/hostObservation/extractor/rawRows/namedObservations/41/observationName의 실제 equals 기대값은 'rollback-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-041" assertion으로 "link-oracle-041: /data/hostObservation/extractor/rawRows/namedObservations/41/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-042" assertion으로 "concrete-links-042: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-042" assertion으로 "link-name-042: /data/hostObservation/extractor/rawRows/namedObservations/42/observationName의 실제 equals 기대값은 'rollback-committed-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-042" assertion으로 "link-oracle-042: /data/hostObservation/extractor/rawRows/namedObservations/42/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-043" assertion으로 "concrete-links-043: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-043" assertion으로 "link-name-043: /data/hostObservation/extractor/rawRows/namedObservations/43/observationName의 실제 equals 기대값은 'guarded-primitives'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-043" assertion으로 "link-oracle-043: /data/hostObservation/extractor/rawRows/namedObservations/43/oracleId의 실제 equals 기대값은 'T04.atomic-ledger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-044" assertion으로 "concrete-links-044: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-044" assertion으로 "link-name-044: /data/hostObservation/extractor/rawRows/namedObservations/44/observationName의 실제 equals 기대값은 'held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-044" assertion으로 "link-oracle-044: /data/hostObservation/extractor/rawRows/namedObservations/44/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-045" assertion으로 "concrete-links-045: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-045" assertion으로 "link-name-045: /data/hostObservation/extractor/rawRows/namedObservations/45/observationName의 실제 equals 기대값은 'sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-045" assertion으로 "link-oracle-045: /data/hostObservation/extractor/rawRows/namedObservations/45/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-046" assertion으로 "concrete-links-046: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-046" assertion으로 "link-name-046: /data/hostObservation/extractor/rawRows/namedObservations/46/observationName의 실제 equals 기대값은 'unreserved-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-046" assertion으로 "link-oracle-046: /data/hostObservation/extractor/rawRows/namedObservations/46/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-047" assertion으로 "concrete-links-047: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-047" assertion으로 "link-name-047: /data/hostObservation/extractor/rawRows/namedObservations/47/observationName의 실제 equals 기대값은 'relations-independent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-047" assertion으로 "link-oracle-047: /data/hostObservation/extractor/rawRows/namedObservations/47/oracleId의 실제 equals 기대값은 'T05.ownership-custody-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-048" assertion으로 "concrete-links-048: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-048" assertion으로 "link-name-048: /data/hostObservation/extractor/rawRows/namedObservations/48/observationName의 실제 equals 기대값은 'current-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-048" assertion으로 "link-oracle-048: /data/hostObservation/extractor/rawRows/namedObservations/48/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-049" assertion으로 "concrete-links-049: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-049" assertion으로 "link-name-049: /data/hostObservation/extractor/rawRows/namedObservations/49/observationName의 실제 equals 기대값은 'planned-destination'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-049" assertion으로 "link-oracle-049: /data/hostObservation/extractor/rawRows/namedObservations/49/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-050" assertion으로 "concrete-links-050: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-050" assertion으로 "link-name-050: /data/hostObservation/extractor/rawRows/namedObservations/50/observationName의 실제 equals 기대값은 'planned-receipt-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-050" assertion으로 "link-oracle-050: /data/hostObservation/extractor/rawRows/namedObservations/50/oracleId의 실제 equals 기대값은 'T05.planned-location'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-051" assertion으로 "concrete-links-051: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-051" assertion으로 "link-name-051: /data/hostObservation/extractor/rawRows/namedObservations/51/observationName의 실제 equals 기대값은 'ordinary-write-confirmed-basis-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-051" assertion으로 "link-oracle-051: /data/hostObservation/extractor/rawRows/namedObservations/51/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-052" assertion으로 "concrete-links-052: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-052" assertion으로 "link-name-052: /data/hostObservation/extractor/rawRows/namedObservations/52/observationName의 실제 equals 기대값은 'eligible-before-manager-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-052" assertion으로 "link-oracle-052: /data/hostObservation/extractor/rawRows/namedObservations/52/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-053" assertion으로 "concrete-links-053: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-053" assertion으로 "link-name-053: /data/hostObservation/extractor/rawRows/namedObservations/53/observationName의 실제 equals 기대값은 'proposal-evidence-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-053" assertion으로 "link-oracle-053: /data/hostObservation/extractor/rawRows/namedObservations/53/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-054" assertion으로 "concrete-links-054: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-054" assertion으로 "link-name-054: /data/hostObservation/extractor/rawRows/namedObservations/54/observationName의 실제 equals 기대값은 'authorized-confirmation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-054" assertion으로 "link-oracle-054: /data/hostObservation/extractor/rawRows/namedObservations/54/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-055" assertion으로 "concrete-links-055: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-055" assertion으로 "link-name-055: /data/hostObservation/extractor/rawRows/namedObservations/55/observationName의 실제 equals 기대값은 'eligible-after-manager-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-055" assertion으로 "link-oracle-055: /data/hostObservation/extractor/rawRows/namedObservations/55/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-056" assertion으로 "concrete-links-056: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-056" assertion으로 "link-name-056: /data/hostObservation/extractor/rawRows/namedObservations/56/observationName의 실제 equals 기대값은 'ordinary-authorized-reservation-after-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-056" assertion으로 "link-oracle-056: /data/hostObservation/extractor/rawRows/namedObservations/56/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-057" assertion으로 "concrete-links-057: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-057" assertion으로 "link-name-057: /data/hostObservation/extractor/rawRows/namedObservations/57/observationName의 실제 equals 기대값은 'ordinary-authorized-dispatch-after-confirmation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-057" assertion으로 "link-oracle-057: /data/hostObservation/extractor/rawRows/namedObservations/57/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-058" assertion으로 "concrete-links-058: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-058" assertion으로 "link-name-058: /data/hostObservation/extractor/rawRows/namedObservations/58/observationName의 실제 equals 기대값은 'new-human-approval-added-to-reservation-or-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-058" assertion으로 "link-oracle-058: /data/hostObservation/extractor/rawRows/namedObservations/58/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-059" assertion으로 "concrete-links-059: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-059" assertion으로 "link-name-059: /data/hostObservation/extractor/rawRows/namedObservations/59/observationName의 실제 equals 기대값은 'manager-decision-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-059" assertion으로 "link-oracle-059: /data/hostObservation/extractor/rawRows/namedObservations/59/oracleId의 실제 equals 기대값은 'T05.disposition-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-060" assertion으로 "concrete-links-060: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-060" assertion으로 "link-name-060: /data/hostObservation/extractor/rawRows/namedObservations/60/observationName의 실제 equals 기대값은 'then-known'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-060" assertion으로 "link-oracle-060: /data/hostObservation/extractor/rawRows/namedObservations/60/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-061" assertion으로 "concrete-links-061: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-061" assertion으로 "link-name-061: /data/hostObservation/extractor/rawRows/namedObservations/61/observationName의 실제 equals 기대값은 'currently-known'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-061" assertion으로 "link-oracle-061: /data/hostObservation/extractor/rawRows/namedObservations/61/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-062" assertion으로 "concrete-links-062: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-062" assertion으로 "link-name-062: /data/hostObservation/extractor/rawRows/namedObservations/62/observationName의 실제 equals 기대값은 'time-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-062" assertion으로 "link-oracle-062: /data/hostObservation/extractor/rawRows/namedObservations/62/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-063" assertion으로 "concrete-links-063: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-063" assertion으로 "link-name-063: /data/hostObservation/extractor/rawRows/namedObservations/63/observationName의 실제 equals 기대값은 'correction-not-fake-movement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-063" assertion으로 "link-oracle-063: /data/hostObservation/extractor/rawRows/namedObservations/63/oracleId의 실제 equals 기대값은 'T06.bitemporal-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-064" assertion으로 "concrete-links-064: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-064" assertion으로 "link-name-064: /data/hostObservation/extractor/rawRows/namedObservations/64/observationName의 실제 equals 기대값은 'state-values'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-064" assertion으로 "link-oracle-064: /data/hostObservation/extractor/rawRows/namedObservations/64/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-065" assertion으로 "concrete-links-065: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-065" assertion으로 "link-name-065: /data/hostObservation/extractor/rawRows/namedObservations/65/observationName의 실제 equals 기대값은 'zero-inference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-065" assertion으로 "link-oracle-065: /data/hostObservation/extractor/rawRows/namedObservations/65/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-066" assertion으로 "concrete-links-066: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-066" assertion으로 "link-name-066: /data/hostObservation/extractor/rawRows/namedObservations/66/observationName의 실제 equals 기대값은 'conflict-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-066" assertion으로 "link-oracle-066: /data/hostObservation/extractor/rawRows/namedObservations/66/oracleId의 실제 equals 기대값은 'T06.unknown-state-distinction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-067" assertion으로 "concrete-links-067: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-067" assertion으로 "link-name-067: /data/hostObservation/extractor/rawRows/namedObservations/67/observationName의 실제 equals 기대값은 'invalid-types'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-067" assertion으로 "link-oracle-067: /data/hostObservation/extractor/rawRows/namedObservations/67/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-068" assertion으로 "concrete-links-068: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-068" assertion으로 "link-name-068: /data/hostObservation/extractor/rawRows/namedObservations/68/observationName의 실제 equals 기대값은 'invalid-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-068" assertion으로 "link-oracle-068: /data/hostObservation/extractor/rawRows/namedObservations/68/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-069" assertion으로 "concrete-links-069: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-069" assertion으로 "link-name-069: /data/hostObservation/extractor/rawRows/namedObservations/69/observationName의 실제 equals 기대값은 'predicate-three-valued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-069" assertion으로 "link-oracle-069: /data/hostObservation/extractor/rawRows/namedObservations/69/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-070" assertion으로 "concrete-links-070: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-070" assertion으로 "link-name-070: /data/hostObservation/extractor/rawRows/namedObservations/70/observationName의 실제 equals 기대값은 'relation-created-work'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-070" assertion으로 "link-oracle-070: /data/hostObservation/extractor/rawRows/namedObservations/70/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-071" assertion으로 "concrete-links-071: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-071" assertion으로 "link-name-071: /data/hostObservation/extractor/rawRows/namedObservations/71/observationName의 실제 equals 기대값은 'relation-created-run'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-071" assertion으로 "link-oracle-071: /data/hostObservation/extractor/rawRows/namedObservations/71/oracleId의 실제 equals 기대값은 'T07.typed-relations-and-predicates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-072" assertion으로 "concrete-links-072: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-072" assertion으로 "link-name-072: /data/hostObservation/extractor/rawRows/namedObservations/72/observationName의 실제 equals 기대값은 'receipt-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-072" assertion으로 "link-oracle-072: /data/hostObservation/extractor/rawRows/namedObservations/72/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-073" assertion으로 "concrete-links-073: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-073" assertion으로 "link-name-073: /data/hostObservation/extractor/rawRows/namedObservations/73/observationName의 실제 equals 기대값은 'document-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-073" assertion으로 "link-oracle-073: /data/hostObservation/extractor/rawRows/namedObservations/73/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-074" assertion으로 "concrete-links-074: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-074" assertion으로 "link-name-074: /data/hostObservation/extractor/rawRows/namedObservations/74/observationName의 실제 equals 기대값은 'canonical-occurrence-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-074" assertion으로 "link-oracle-074: /data/hostObservation/extractor/rawRows/namedObservations/74/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-075" assertion으로 "concrete-links-075: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-075" assertion으로 "link-name-075: /data/hostObservation/extractor/rawRows/namedObservations/75/observationName의 실제 equals 기대값은 'duplicate-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-075" assertion으로 "link-oracle-075: /data/hostObservation/extractor/rawRows/namedObservations/75/oracleId의 실제 equals 기대값은 'T07.two-documents-one-occurrence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-076" assertion으로 "concrete-links-076: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-076" assertion으로 "link-name-076: /data/hostObservation/extractor/rawRows/namedObservations/76/observationName의 실제 equals 기대값은 'unavailable-evidence-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-076" assertion으로 "link-oracle-076: /data/hostObservation/extractor/rawRows/namedObservations/76/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-077" assertion으로 "concrete-links-077: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-077" assertion으로 "link-name-077: /data/hostObservation/extractor/rawRows/namedObservations/77/observationName의 실제 equals 기대값은 'unreadable-source'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-077" assertion으로 "link-oracle-077: /data/hostObservation/extractor/rawRows/namedObservations/77/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-078" assertion으로 "concrete-links-078: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-078" assertion으로 "link-name-078: /data/hostObservation/extractor/rawRows/namedObservations/78/observationName의 실제 equals 기대값은 'blob-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-078" assertion으로 "link-oracle-078: /data/hostObservation/extractor/rawRows/namedObservations/78/oracleId의 실제 equals 기대값은 'T07.document-availability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-079" assertion으로 "concrete-links-079: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-079" assertion으로 "link-name-079: /data/hostObservation/extractor/rawRows/namedObservations/79/observationName의 실제 equals 기대값은 'cross-org-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-079" assertion으로 "link-oracle-079: /data/hostObservation/extractor/rawRows/namedObservations/79/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-080" assertion으로 "concrete-links-080: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-080" assertion으로 "link-name-080: /data/hostObservation/extractor/rawRows/namedObservations/80/observationName의 실제 equals 기대값은 'cross-org-identity-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-080" assertion으로 "link-oracle-080: /data/hostObservation/extractor/rawRows/namedObservations/80/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-081" assertion으로 "concrete-links-081: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-081" assertion으로 "link-name-081: /data/hostObservation/extractor/rawRows/namedObservations/81/observationName의 실제 equals 기대값은 'invalid-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-081" assertion으로 "link-oracle-081: /data/hostObservation/extractor/rawRows/namedObservations/81/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-082" assertion으로 "concrete-links-082: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-082" assertion으로 "link-name-082: /data/hostObservation/extractor/rawRows/namedObservations/82/observationName의 실제 equals 기대값은 'fk-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-082" assertion으로 "link-oracle-082: /data/hostObservation/extractor/rawRows/namedObservations/82/oracleId의 실제 equals 기대값은 'T08.organization-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-083" assertion으로 "concrete-links-083: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-083" assertion으로 "link-name-083: /data/hostObservation/extractor/rawRows/namedObservations/83/observationName의 실제 equals 기대값은 'role-or-payload-escalation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-083" assertion으로 "link-oracle-083: /data/hostObservation/extractor/rawRows/namedObservations/83/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-084" assertion으로 "concrete-links-084: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-084" assertion으로 "link-name-084: /data/hostObservation/extractor/rawRows/namedObservations/84/observationName의 실제 equals 기대값은 'grant-exceeding-write'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-084" assertion으로 "link-oracle-084: /data/hostObservation/extractor/rawRows/namedObservations/84/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-085" assertion으로 "concrete-links-085: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-085" assertion으로 "link-name-085: /data/hostObservation/extractor/rawRows/namedObservations/85/observationName의 실제 equals 기대값은 'server-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-085" assertion으로 "link-oracle-085: /data/hostObservation/extractor/rawRows/namedObservations/85/oracleId의 실제 equals 기대값은 'T08.control-envelope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-086" assertion으로 "concrete-links-086: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-086" assertion으로 "link-name-086: /data/hostObservation/extractor/rawRows/namedObservations/86/observationName의 실제 equals 기대값은 'self-amplification'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-086" assertion으로 "link-oracle-086: /data/hostObservation/extractor/rawRows/namedObservations/86/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-087" assertion으로 "concrete-links-087: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-087" assertion으로 "link-name-087: /data/hostObservation/extractor/rawRows/namedObservations/87/observationName의 실제 equals 기대값은 'out-of-scope-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-087" assertion으로 "link-oracle-087: /data/hostObservation/extractor/rawRows/namedObservations/87/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-088" assertion으로 "concrete-links-088: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-088" assertion으로 "link-name-088: /data/hostObservation/extractor/rawRows/namedObservations/88/observationName의 실제 equals 기대값은 'grant-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-088" assertion으로 "link-oracle-088: /data/hostObservation/extractor/rawRows/namedObservations/88/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-089" assertion으로 "concrete-links-089: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-089" assertion으로 "link-name-089: /data/hostObservation/extractor/rawRows/namedObservations/89/observationName의 실제 equals 기대값은 'identity-admin-only'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-089" assertion으로 "link-oracle-089: /data/hostObservation/extractor/rawRows/namedObservations/89/oracleId의 실제 equals 기대값은 'T08.grant-control-plane'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-090" assertion으로 "concrete-links-090: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-090" assertion으로 "link-name-090: /data/hostObservation/extractor/rawRows/namedObservations/90/observationName의 실제 equals 기대값은 'cumulative-arrived'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-090" assertion으로 "link-oracle-090: /data/hostObservation/extractor/rawRows/namedObservations/90/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-091" assertion으로 "concrete-links-091: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-091" assertion으로 "link-name-091: /data/hostObservation/extractor/rawRows/namedObservations/91/observationName의 실제 equals 기대값은 'state-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-091" assertion으로 "link-oracle-091: /data/hostObservation/extractor/rawRows/namedObservations/91/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-092" assertion으로 "concrete-links-092: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-092" assertion으로 "link-name-092: /data/hostObservation/extractor/rawRows/namedObservations/92/observationName의 실제 equals 기대값은 'cumulative-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-092" assertion으로 "link-oracle-092: /data/hostObservation/extractor/rawRows/namedObservations/92/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-093" assertion으로 "concrete-links-093: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-093" assertion으로 "link-name-093: /data/hostObservation/extractor/rawRows/namedObservations/93/observationName의 실제 equals 기대값은 'state-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-093" assertion으로 "link-oracle-093: /data/hostObservation/extractor/rawRows/namedObservations/93/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-094" assertion으로 "concrete-links-094: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-094" assertion으로 "link-name-094: /data/hostObservation/extractor/rawRows/namedObservations/94/observationName의 실제 equals 기대값은 'goal-mode-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-094" assertion으로 "link-oracle-094: /data/hostObservation/extractor/rawRows/namedObservations/94/oracleId의 실제 equals 기대값은 'T09.quantity-modes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-095" assertion으로 "concrete-links-095: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-095" assertion으로 "link-name-095: /data/hostObservation/extractor/rawRows/namedObservations/95/observationName의 실제 equals 기대값은 'stage-requirements'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-095" assertion으로 "link-oracle-095: /data/hostObservation/extractor/rawRows/namedObservations/95/oracleId의 실제 equals 기대값은 'T09.stage-required-lot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-096" assertion으로 "concrete-links-096: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-096" assertion으로 "link-name-096: /data/hostObservation/extractor/rawRows/namedObservations/96/observationName의 실제 equals 기대값은 'draft-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-096" assertion으로 "link-oracle-096: /data/hostObservation/extractor/rawRows/namedObservations/96/oracleId의 실제 equals 기대값은 'T09.stage-required-lot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-097" assertion으로 "concrete-links-097: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-097" assertion으로 "link-name-097: /data/hostObservation/extractor/rawRows/namedObservations/97/observationName의 실제 equals 기대값은 'gap-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-097" assertion으로 "link-oracle-097: /data/hostObservation/extractor/rawRows/namedObservations/97/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-098" assertion으로 "concrete-links-098: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-098" assertion으로 "link-name-098: /data/hostObservation/extractor/rawRows/namedObservations/98/observationName의 실제 equals 기대값은 'inferred-continuity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-098" assertion으로 "link-oracle-098: /data/hostObservation/extractor/rawRows/namedObservations/98/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-099" assertion으로 "concrete-links-099: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-099" assertion으로 "link-name-099: /data/hostObservation/extractor/rawRows/namedObservations/99/observationName의 실제 equals 기대값은 'interval-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-099" assertion으로 "link-oracle-099: /data/hostObservation/extractor/rawRows/namedObservations/99/oracleId의 실제 equals 기대값은 'T09.throughout-observation-gap'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-100" assertion으로 "concrete-links-100: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-100" assertion으로 "link-name-100: /data/hostObservation/extractor/rawRows/namedObservations/100/observationName의 실제 equals 기대값은 'initial-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-100" assertion으로 "link-oracle-100: /data/hostObservation/extractor/rawRows/namedObservations/100/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-101" assertion으로 "concrete-links-101: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-101" assertion으로 "link-name-101: /data/hostObservation/extractor/rawRows/namedObservations/101/observationName의 실제 equals 기대값은 'final-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-101" assertion으로 "link-oracle-101: /data/hostObservation/extractor/rawRows/namedObservations/101/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-102" assertion으로 "concrete-links-102: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-102" assertion으로 "link-name-102: /data/hostObservation/extractor/rawRows/namedObservations/102/observationName의 실제 equals 기대값은 'exists-in-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-102" assertion으로 "link-oracle-102: /data/hostObservation/extractor/rawRows/namedObservations/102/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-103" assertion으로 "concrete-links-103: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-103" assertion으로 "link-name-103: /data/hostObservation/extractor/rawRows/namedObservations/103/observationName의 실제 equals 기대값은 'end-state-at-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-103" assertion으로 "link-oracle-103: /data/hostObservation/extractor/rawRows/namedObservations/103/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-104" assertion으로 "concrete-links-104: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-104" assertion으로 "link-name-104: /data/hostObservation/extractor/rawRows/namedObservations/104/observationName의 실제 equals 기대값은 'throughout-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-104" assertion으로 "link-oracle-104: /data/hostObservation/extractor/rawRows/namedObservations/104/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-105" assertion으로 "concrete-links-105: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-105" assertion으로 "link-name-105: /data/hostObservation/extractor/rawRows/namedObservations/105/observationName의 실제 equals 기대값은 'same-trajectory-three-goals'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-105" assertion으로 "link-oracle-105: /data/hostObservation/extractor/rawRows/namedObservations/105/oracleId의 실제 equals 기대값은 'T09.exists-in-versus-end-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-106" assertion으로 "concrete-links-106: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-106" assertion으로 "link-name-106: /data/hostObservation/extractor/rawRows/namedObservations/106/observationName의 실제 equals 기대값은 'initial-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-106" assertion으로 "link-oracle-106: /data/hostObservation/extractor/rawRows/namedObservations/106/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-107" assertion으로 "concrete-links-107: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-107" assertion으로 "link-name-107: /data/hostObservation/extractor/rawRows/namedObservations/107/observationName의 실제 equals 기대값은 'final-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-107" assertion으로 "link-oracle-107: /data/hostObservation/extractor/rawRows/namedObservations/107/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-108" assertion으로 "concrete-links-108: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-108" assertion으로 "link-name-108: /data/hostObservation/extractor/rawRows/namedObservations/108/observationName의 실제 equals 기대값은 'throughout-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-108" assertion으로 "link-oracle-108: /data/hostObservation/extractor/rawRows/namedObservations/108/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-109" assertion으로 "concrete-links-109: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-109" assertion으로 "link-name-109: /data/hostObservation/extractor/rawRows/namedObservations/109/observationName의 실제 equals 기대값은 'sufficient-continuity-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-109" assertion으로 "link-oracle-109: /data/hostObservation/extractor/rawRows/namedObservations/109/oracleId의 실제 equals 기대값은 'T09.throughout-fully-observed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-110" assertion으로 "concrete-links-110: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-110" assertion으로 "link-name-110: /data/hostObservation/extractor/rawRows/namedObservations/110/observationName의 실제 equals 기대값은 'owner-by-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-110" assertion으로 "link-oracle-110: /data/hostObservation/extractor/rawRows/namedObservations/110/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-111" assertion으로 "concrete-links-111: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-111" assertion으로 "link-name-111: /data/hostObservation/extractor/rawRows/namedObservations/111/observationName의 실제 equals 기대값은 'notification-as-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-111" assertion으로 "link-oracle-111: /data/hostObservation/extractor/rawRows/namedObservations/111/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-112" assertion으로 "concrete-links-112: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-112" assertion으로 "link-name-112: /data/hostObservation/extractor/rawRows/namedObservations/112/observationName의 실제 equals 기대값은 'active-human-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-112" assertion으로 "link-oracle-112: /data/hostObservation/extractor/rawRows/namedObservations/112/oracleId의 실제 equals 기대값은 'T10.handover-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-113" assertion으로 "concrete-links-113: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-113" assertion으로 "link-name-113: /data/hostObservation/extractor/rawRows/namedObservations/113/observationName의 실제 equals 기대값은 'source-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-113" assertion으로 "link-oracle-113: /data/hostObservation/extractor/rawRows/namedObservations/113/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-114" assertion으로 "concrete-links-114: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-114" assertion으로 "link-name-114: /data/hostObservation/extractor/rawRows/namedObservations/114/observationName의 실제 equals 기대값은 'source-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-114" assertion으로 "link-oracle-114: /data/hostObservation/extractor/rawRows/namedObservations/114/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-115" assertion으로 "concrete-links-115: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-115" assertion으로 "link-name-115: /data/hostObservation/extractor/rawRows/namedObservations/115/observationName의 실제 equals 기대값은 'orphan-target-assignment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-115" assertion으로 "link-oracle-115: /data/hostObservation/extractor/rawRows/namedObservations/115/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-116" assertion으로 "concrete-links-116: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-116" assertion으로 "link-name-116: /data/hostObservation/extractor/rawRows/namedObservations/116/observationName의 실제 equals 기대값은 'transfer-guard'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-116" assertion으로 "link-oracle-116: /data/hostObservation/extractor/rawRows/namedObservations/116/oracleId의 실제 equals 기대값은 'T10.obligation-transfer-failure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-117" assertion으로 "concrete-links-117: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-117" assertion으로 "link-name-117: /data/hostObservation/extractor/rawRows/namedObservations/117/observationName의 실제 equals 기대값은 'transferred-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-117" assertion으로 "link-oracle-117: /data/hostObservation/extractor/rawRows/namedObservations/117/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-118" assertion으로 "concrete-links-118: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-118" assertion으로 "link-name-118: /data/hostObservation/extractor/rawRows/namedObservations/118/observationName의 실제 equals 기대값은 'remaining-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-118" assertion으로 "link-oracle-118: /data/hostObservation/extractor/rawRows/namedObservations/118/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-119" assertion으로 "concrete-links-119: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-119" assertion으로 "link-name-119: /data/hostObservation/extractor/rawRows/namedObservations/119/observationName의 실제 equals 기대값은 'unresolved-root-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-119" assertion으로 "link-oracle-119: /data/hostObservation/extractor/rawRows/namedObservations/119/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-120" assertion으로 "concrete-links-120: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-120" assertion으로 "link-name-120: /data/hostObservation/extractor/rawRows/namedObservations/120/observationName의 실제 equals 기대값은 'current-assignments'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-120" assertion으로 "link-oracle-120: /data/hostObservation/extractor/rawRows/namedObservations/120/oracleId의 실제 equals 기대값은 'T10.partial-transfer-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-121" assertion으로 "concrete-links-121: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-121" assertion으로 "link-name-121: /data/hostObservation/extractor/rawRows/namedObservations/121/observationName의 실제 equals 기대값은 'emergency-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-121" assertion으로 "link-oracle-121: /data/hostObservation/extractor/rawRows/namedObservations/121/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-122" assertion으로 "concrete-links-122: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-122" assertion으로 "link-name-122: /data/hostObservation/extractor/rawRows/namedObservations/122/observationName의 실제 equals 기대값은 'unauthorized-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-122" assertion으로 "link-oracle-122: /data/hostObservation/extractor/rawRows/namedObservations/122/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-123" assertion으로 "concrete-links-123: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-123" assertion으로 "link-name-123: /data/hostObservation/extractor/rawRows/namedObservations/123/observationName의 실제 equals 기대값은 'unauthorized-waiver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-123" assertion으로 "link-oracle-123: /data/hostObservation/extractor/rawRows/namedObservations/123/oracleId의 실제 equals 기대값은 'T10.emergency-and-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-124" assertion으로 "concrete-links-124: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-124" assertion으로 "link-name-124: /data/hostObservation/extractor/rawRows/namedObservations/124/observationName의 실제 equals 기대값은 'parent-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-124" assertion으로 "link-oracle-124: /data/hostObservation/extractor/rawRows/namedObservations/124/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-125" assertion으로 "concrete-links-125: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-125" assertion으로 "link-name-125: /data/hostObservation/extractor/rawRows/namedObservations/125/observationName의 실제 equals 기대값은 'parent-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-125" assertion으로 "link-oracle-125: /data/hostObservation/extractor/rawRows/namedObservations/125/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-126" assertion으로 "concrete-links-126: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-126" assertion으로 "link-name-126: /data/hostObservation/extractor/rawRows/namedObservations/126/observationName의 실제 equals 기대값은 'false-fulfilled-closure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-126" assertion으로 "link-oracle-126: /data/hostObservation/extractor/rawRows/namedObservations/126/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-127" assertion으로 "concrete-links-127: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-127" assertion으로 "link-name-127: /data/hostObservation/extractor/rawRows/namedObservations/127/observationName의 실제 equals 기대값은 'alternative-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-127" assertion으로 "link-oracle-127: /data/hostObservation/extractor/rawRows/namedObservations/127/oracleId의 실제 equals 기대값은 'T11.parent-not-child-completion'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-128" assertion으로 "concrete-links-128: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-128" assertion으로 "link-name-128: /data/hostObservation/extractor/rawRows/namedObservations/128/observationName의 실제 equals 기대값은 'cancel-draft-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-128" assertion으로 "link-oracle-128: /data/hostObservation/extractor/rawRows/namedObservations/128/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-129" assertion으로 "concrete-links-129: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-129" assertion으로 "link-name-129: /data/hostObservation/extractor/rawRows/namedObservations/129/observationName의 실제 equals 기대값은 'timeout-generated-arrival-or-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-129" assertion으로 "link-oracle-129: /data/hostObservation/extractor/rawRows/namedObservations/129/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-130" assertion으로 "concrete-links-130: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-130" assertion으로 "link-name-130: /data/hostObservation/extractor/rawRows/namedObservations/130/observationName의 실제 equals 기대값은 'unsupported-activation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-130" assertion으로 "link-oracle-130: /data/hostObservation/extractor/rawRows/namedObservations/130/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-131" assertion으로 "concrete-links-131: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-131" assertion으로 "link-name-131: /data/hostObservation/extractor/rawRows/namedObservations/131/observationName의 실제 equals 기대값은 'stale-revision-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-131" assertion으로 "link-oracle-131: /data/hostObservation/extractor/rawRows/namedObservations/131/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-132" assertion으로 "concrete-links-132: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-132" assertion으로 "link-name-132: /data/hostObservation/extractor/rawRows/namedObservations/132/observationName의 실제 equals 기대값은 'pending-assessment-fulfilled-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-132" assertion으로 "link-oracle-132: /data/hostObservation/extractor/rawRows/namedObservations/132/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-133" assertion으로 "concrete-links-133: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-133" assertion으로 "link-name-133: /data/hostObservation/extractor/rawRows/namedObservations/133/observationName의 실제 equals 기대값은 'cancel-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-133" assertion으로 "link-oracle-133: /data/hostObservation/extractor/rawRows/namedObservations/133/oracleId의 실제 equals 기대값은 'T11.work-transitions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-134" assertion으로 "concrete-links-134: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-134" assertion으로 "link-name-134: /data/hostObservation/extractor/rawRows/namedObservations/134/observationName의 실제 equals 기대값은 'dependency-cycle-created'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-134" assertion으로 "link-oracle-134: /data/hostObservation/extractor/rawRows/namedObservations/134/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-135" assertion으로 "concrete-links-135: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-135" assertion으로 "link-name-135: /data/hostObservation/extractor/rawRows/namedObservations/135/observationName의 실제 equals 기대값은 'shared-distinct-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-135" assertion으로 "link-oracle-135: /data/hostObservation/extractor/rawRows/namedObservations/135/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-136" assertion으로 "concrete-links-136: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-136" assertion으로 "link-name-136: /data/hostObservation/extractor/rawRows/namedObservations/136/observationName의 실제 equals 기대값은 'closed-work-reopen-or-rewrite'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-136" assertion으로 "link-oracle-136: /data/hostObservation/extractor/rawRows/namedObservations/136/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-137" assertion으로 "concrete-links-137: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-137" assertion으로 "link-name-137: /data/hostObservation/extractor/rawRows/namedObservations/137/observationName의 실제 equals 기대값은 'followup-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-137" assertion으로 "link-oracle-137: /data/hostObservation/extractor/rawRows/namedObservations/137/oracleId의 실제 equals 기대값은 'T11.dependency-and-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-138" assertion으로 "concrete-links-138: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-138" assertion으로 "link-name-138: /data/hostObservation/extractor/rawRows/namedObservations/138/observationName의 실제 equals 기대값은 'insufficient-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-138" assertion으로 "link-oracle-138: /data/hostObservation/extractor/rawRows/namedObservations/138/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-139" assertion으로 "concrete-links-139: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-139" assertion으로 "link-name-139: /data/hostObservation/extractor/rawRows/namedObservations/139/observationName의 실제 equals 기대값은 'wrong-place-condition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-139" assertion으로 "link-oracle-139: /data/hostObservation/extractor/rawRows/namedObservations/139/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-140" assertion으로 "concrete-links-140: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-140" assertion으로 "link-name-140: /data/hostObservation/extractor/rawRows/namedObservations/140/observationName의 실제 equals 기대값은 'assessment-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-140" assertion으로 "link-oracle-140: /data/hostObservation/extractor/rawRows/namedObservations/140/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-141" assertion으로 "concrete-links-141: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-141" assertion으로 "link-name-141: /data/hostObservation/extractor/rawRows/namedObservations/141/observationName의 실제 equals 기대값은 'missing-as-zero'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-141" assertion으로 "link-oracle-141: /data/hostObservation/extractor/rawRows/namedObservations/141/oracleId의 실제 equals 기대값은 'T12.assessment-evidence-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-142" assertion으로 "concrete-links-142: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-142" assertion으로 "link-name-142: /data/hostObservation/extractor/rawRows/namedObservations/142/observationName의 실제 equals 기대값은 'historical-violation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-142" assertion으로 "link-oracle-142: /data/hostObservation/extractor/rawRows/namedObservations/142/oracleId의 실제 equals 기대값은 'T12.deadline-change-and-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-143" assertion으로 "concrete-links-143: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-143" assertion으로 "link-name-143: /data/hostObservation/extractor/rawRows/namedObservations/143/observationName의 실제 equals 기대값은 'old-approval-revised-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-143" assertion으로 "link-oracle-143: /data/hostObservation/extractor/rawRows/namedObservations/143/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-144" assertion으로 "concrete-links-144: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-144" assertion으로 "link-name-144: /data/hostObservation/extractor/rawRows/namedObservations/144/observationName의 실제 equals 기대값은 'unauthorized-approved-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-144" assertion으로 "link-oracle-144: /data/hostObservation/extractor/rawRows/namedObservations/144/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-145" assertion으로 "concrete-links-145: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-145" assertion으로 "link-name-145: /data/hostObservation/extractor/rawRows/namedObservations/145/observationName의 실제 equals 기대값은 'approval-bound'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-145" assertion으로 "link-oracle-145: /data/hostObservation/extractor/rawRows/namedObservations/145/oracleId의 실제 equals 기대값은 'T13.approval-version-binding'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-146" assertion으로 "concrete-links-146: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-146" assertion으로 "link-name-146: /data/hostObservation/extractor/rawRows/namedObservations/146/observationName의 실제 equals 기대값은 'purchase-created-held-inventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-146" assertion으로 "link-oracle-146: /data/hostObservation/extractor/rawRows/namedObservations/146/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-147" assertion으로 "concrete-links-147: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-147" assertion으로 "link-name-147: /data/hostObservation/extractor/rawRows/namedObservations/147/observationName의 실제 equals 기대값은 'transmission-created-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-147" assertion으로 "link-oracle-147: /data/hostObservation/extractor/rawRows/namedObservations/147/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-148" assertion으로 "concrete-links-148: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-148" assertion으로 "link-name-148: /data/hostObservation/extractor/rawRows/namedObservations/148/observationName의 실제 equals 기대값은 'supplier-acceptance-created-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-148" assertion으로 "link-oracle-148: /data/hostObservation/extractor/rawRows/namedObservations/148/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-149" assertion으로 "concrete-links-149: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-149" assertion으로 "link-name-149: /data/hostObservation/extractor/rawRows/namedObservations/149/observationName의 실제 equals 기대값은 'cancel-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-149" assertion으로 "link-oracle-149: /data/hostObservation/extractor/rawRows/namedObservations/149/oracleId의 실제 equals 기대값은 'T13.delivery-commitment-arrival-separate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-150" assertion으로 "concrete-links-150: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-150" assertion으로 "link-name-150: /data/hostObservation/extractor/rawRows/namedObservations/150/observationName의 실제 equals 기대값은 'authorized-arrival-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-150" assertion으로 "link-oracle-150: /data/hostObservation/extractor/rawRows/namedObservations/150/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-151" assertion으로 "concrete-links-151: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-151" assertion으로 "link-name-151: /data/hostObservation/extractor/rawRows/namedObservations/151/observationName의 실제 equals 기대값은 'excess-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-151" assertion으로 "link-oracle-151: /data/hostObservation/extractor/rawRows/namedObservations/151/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-152" assertion으로 "concrete-links-152: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-152" assertion으로 "link-name-152: /data/hostObservation/extractor/rawRows/namedObservations/152/observationName의 실제 equals 기대값은 'return-added-to-purchase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-152" assertion으로 "link-oracle-152: /data/hostObservation/extractor/rawRows/namedObservations/152/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-153" assertion으로 "concrete-links-153: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-153" assertion으로 "link-name-153: /data/hostObservation/extractor/rawRows/namedObservations/153/observationName의 실제 equals 기대값은 'relocation-added-to-purchase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-153" assertion으로 "link-oracle-153: /data/hostObservation/extractor/rawRows/namedObservations/153/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-154" assertion으로 "concrete-links-154: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-154" assertion으로 "link-name-154: /data/hostObservation/extractor/rawRows/namedObservations/154/observationName의 실제 equals 기대값은 'contribution-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-154" assertion으로 "link-oracle-154: /data/hostObservation/extractor/rawRows/namedObservations/154/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-155" assertion으로 "concrete-links-155: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-155" assertion으로 "link-name-155: /data/hostObservation/extractor/rawRows/namedObservations/155/observationName의 실제 equals 기대값은 'excess-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-155" assertion으로 "link-oracle-155: /data/hostObservation/extractor/rawRows/namedObservations/155/oracleId의 실제 equals 기대값은 'T13.partial-and-excess-contributions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-156" assertion으로 "concrete-links-156: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-156" assertion으로 "link-name-156: /data/hostObservation/extractor/rawRows/namedObservations/156/observationName의 실제 equals 기대값은 'distinct-cargo-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-156" assertion으로 "link-oracle-156: /data/hostObservation/extractor/rawRows/namedObservations/156/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-157" assertion으로 "concrete-links-157: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-157" assertion으로 "link-name-157: /data/hostObservation/extractor/rawRows/namedObservations/157/observationName의 실제 equals 기대값은 'double-order-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-157" assertion으로 "link-oracle-157: /data/hostObservation/extractor/rawRows/namedObservations/157/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-158" assertion으로 "concrete-links-158: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-158" assertion으로 "link-name-158: /data/hostObservation/extractor/rawRows/namedObservations/158/observationName의 실제 equals 기대값은 'leg-facts'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-158" assertion으로 "link-oracle-158: /data/hostObservation/extractor/rawRows/namedObservations/158/oracleId의 실제 equals 기대값은 'T14.many-to-many-shipment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-159" assertion으로 "concrete-links-159: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-159" assertion으로 "link-name-159: /data/hostObservation/extractor/rawRows/namedObservations/159/observationName의 실제 equals 기대값은 'received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-159" assertion으로 "link-oracle-159: /data/hostObservation/extractor/rawRows/namedObservations/159/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-160" assertion으로 "concrete-links-160: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-160" assertion으로 "link-name-160: /data/hostObservation/extractor/rawRows/namedObservations/160/observationName의 실제 equals 기대값은 'transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-160" assertion으로 "link-oracle-160: /data/hostObservation/extractor/rawRows/namedObservations/160/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-161" assertion으로 "concrete-links-161: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-161" assertion으로 "link-name-161: /data/hostObservation/extractor/rawRows/namedObservations/161/observationName의 실제 equals 기대값은 'automatic-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-161" assertion으로 "link-oracle-161: /data/hostObservation/extractor/rawRows/namedObservations/161/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-162" assertion으로 "concrete-links-162: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-162" assertion으로 "link-name-162: /data/hostObservation/extractor/rawRows/namedObservations/162/observationName의 실제 equals 기대값은 'separate-observations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-162" assertion으로 "link-oracle-162: /data/hostObservation/extractor/rawRows/namedObservations/162/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-163" assertion으로 "concrete-links-163: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-163" assertion으로 "link-name-163: /data/hostObservation/extractor/rawRows/namedObservations/163/observationName의 실제 equals 기대값은 'discrepancy-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-163" assertion으로 "link-oracle-163: /data/hostObservation/extractor/rawRows/namedObservations/163/oracleId의 실제 equals 기대값은 'T14.arrival-discrepancy-not-loss'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-164" assertion으로 "concrete-links-164: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-164" assertion으로 "link-name-164: /data/hostObservation/extractor/rawRows/namedObservations/164/observationName의 실제 equals 기대값은 'orphan-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-164" assertion으로 "link-oracle-164: /data/hostObservation/extractor/rawRows/namedObservations/164/oracleId의 실제 equals 기대값은 'T14.orphan-temperature-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-165" assertion으로 "concrete-links-165: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-165" assertion으로 "link-name-165: /data/hostObservation/extractor/rawRows/namedObservations/165/observationName의 실제 equals 기대값은 'notification-closes-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-165" assertion으로 "link-oracle-165: /data/hostObservation/extractor/rawRows/namedObservations/165/oracleId의 실제 equals 기대값은 'T14.orphan-temperature-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-166" assertion으로 "concrete-links-166: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-166" assertion으로 "link-name-166: /data/hostObservation/extractor/rawRows/namedObservations/166/observationName의 실제 equals 기대값은 'confirmed-sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-166" assertion으로 "link-oracle-166: /data/hostObservation/extractor/rawRows/namedObservations/166/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-167" assertion으로 "concrete-links-167: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-167" assertion으로 "link-name-167: /data/hostObservation/extractor/rawRows/namedObservations/167/observationName의 실제 equals 기대값은 'remaining-agency'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-167" assertion으로 "link-oracle-167: /data/hostObservation/extractor/rawRows/namedObservations/167/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-168" assertion으로 "concrete-links-168: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-168" assertion으로 "link-name-168: /data/hostObservation/extractor/rawRows/namedObservations/168/observationName의 실제 equals 기대값은 'qc-expanded-agency-allowance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-168" assertion으로 "link-oracle-168: /data/hostObservation/extractor/rawRows/namedObservations/168/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-169" assertion으로 "concrete-links-169: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-169" assertion으로 "link-name-169: /data/hostObservation/extractor/rawRows/namedObservations/169/observationName의 실제 equals 기대값은 'decision-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-169" assertion으로 "link-oracle-169: /data/hostObservation/extractor/rawRows/namedObservations/169/oracleId의 실제 equals 기대값은 'T15.partial-regulatory-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-170" assertion으로 "concrete-links-170: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-170" assertion으로 "link-name-170: /data/hostObservation/extractor/rawRows/namedObservations/170/observationName의 실제 equals 기대값은 'local-document-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-170" assertion으로 "link-oracle-170: /data/hostObservation/extractor/rawRows/namedObservations/170/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-171" assertion으로 "concrete-links-171: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-171" assertion으로 "link-name-171: /data/hostObservation/extractor/rawRows/namedObservations/171/observationName의 실제 equals 기대값은 'unknown-submit-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-171" assertion으로 "link-oracle-171: /data/hostObservation/extractor/rawRows/namedObservations/171/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-172" assertion으로 "concrete-links-172: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-172" assertion으로 "link-name-172: /data/hostObservation/extractor/rawRows/namedObservations/172/observationName의 실제 equals 기대값은 'local-document-external-submission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-172" assertion으로 "link-oracle-172: /data/hostObservation/extractor/rawRows/namedObservations/172/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-173" assertion으로 "concrete-links-173: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-173" assertion으로 "link-name-173: /data/hostObservation/extractor/rawRows/namedObservations/173/observationName의 실제 equals 기대값은 'regulatory-history'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-173" assertion으로 "link-oracle-173: /data/hostObservation/extractor/rawRows/namedObservations/173/oracleId의 실제 equals 기대값은 'T15.procedure-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-174" assertion으로 "concrete-links-174: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-174" assertion으로 "link-name-174: /data/hostObservation/extractor/rawRows/namedObservations/174/observationName의 실제 equals 기대값은 'confirmed-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-174" assertion으로 "link-oracle-174: /data/hostObservation/extractor/rawRows/namedObservations/174/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-175" assertion으로 "concrete-links-175: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-175" assertion으로 "link-name-175: /data/hostObservation/extractor/rawRows/namedObservations/175/observationName의 실제 equals 기대값은 'legal-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-175" assertion으로 "link-oracle-175: /data/hostObservation/extractor/rawRows/namedObservations/175/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-176" assertion으로 "concrete-links-176: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-176" assertion으로 "link-name-176: /data/hostObservation/extractor/rawRows/namedObservations/176/observationName의 실제 equals 기대값은 'regulatory-gate'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-176" assertion으로 "link-oracle-176: /data/hostObservation/extractor/rawRows/namedObservations/176/oracleId의 실제 equals 기대값은 'T15.unresolved-real-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-177" assertion으로 "concrete-links-177: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-177" assertion으로 "link-name-177: /data/hostObservation/extractor/rawRows/namedObservations/177/observationName의 실제 equals 기대값은 'provisional-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-177" assertion으로 "link-oracle-177: /data/hostObservation/extractor/rawRows/namedObservations/177/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-178" assertion으로 "concrete-links-178: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-178" assertion으로 "link-name-178: /data/hostObservation/extractor/rawRows/namedObservations/178/observationName의 실제 equals 기대값은 'receipt-transit-double-creation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-178" assertion으로 "link-oracle-178: /data/hostObservation/extractor/rawRows/namedObservations/178/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-179" assertion으로 "concrete-links-179: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-179" assertion으로 "link-name-179: /data/hostObservation/extractor/rawRows/namedObservations/179/observationName의 실제 equals 기대값은 'eligible-after-QC-only-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-179" assertion으로 "link-oracle-179: /data/hostObservation/extractor/rawRows/namedObservations/179/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-180" assertion으로 "concrete-links-180: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-180" assertion으로 "link-name-180: /data/hostObservation/extractor/rawRows/namedObservations/180/observationName의 실제 equals 기대값은 'remaining-recall'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-180" assertion으로 "link-oracle-180: /data/hostObservation/extractor/rawRows/namedObservations/180/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-181" assertion으로 "concrete-links-181: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-181" assertion으로 "link-name-181: /data/hostObservation/extractor/rawRows/namedObservations/181/observationName의 실제 equals 기대값은 'hold-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-181" assertion으로 "link-oracle-181: /data/hostObservation/extractor/rawRows/namedObservations/181/oracleId의 실제 equals 기대값은 'T16.provisional-and-independent-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-182" assertion으로 "concrete-links-182: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-182" assertion으로 "link-name-182: /data/hostObservation/extractor/rawRows/namedObservations/182/observationName의 실제 equals 기대값은 'held-before-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-182" assertion으로 "link-oracle-182: /data/hostObservation/extractor/rawRows/namedObservations/182/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-183" assertion으로 "concrete-links-183: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-183" assertion으로 "link-name-183: /data/hostObservation/extractor/rawRows/namedObservations/183/observationName의 실제 equals 기대값은 'held-after-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-183" assertion으로 "link-oracle-183: /data/hostObservation/extractor/rawRows/namedObservations/183/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-184" assertion으로 "concrete-links-184: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-184" assertion으로 "link-name-184: /data/hostObservation/extractor/rawRows/namedObservations/184/observationName의 실제 equals 기대값은 'distinct-total-after-internal-move'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-184" assertion으로 "link-oracle-184: /data/hostObservation/extractor/rawRows/namedObservations/184/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-185" assertion으로 "concrete-links-185: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-185" assertion으로 "link-name-185: /data/hostObservation/extractor/rawRows/namedObservations/185/observationName의 실제 equals 기대값은 'adjustment-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-185" assertion으로 "link-oracle-185: /data/hostObservation/extractor/rawRows/namedObservations/185/oracleId의 실제 equals 기대값은 'T16.movement-stocktake-adjustment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-186" assertion으로 "concrete-links-186: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-186" assertion으로 "link-name-186: /data/hostObservation/extractor/rawRows/namedObservations/186/observationName의 실제 equals 기대값은 'allocation-after-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-186" assertion으로 "link-oracle-186: /data/hostObservation/extractor/rawRows/namedObservations/186/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-187" assertion으로 "concrete-links-187: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-187" assertion으로 "link-name-187: /data/hostObservation/extractor/rawRows/namedObservations/187/observationName의 실제 equals 기대값은 'post-expiry-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-187" assertion으로 "link-oracle-187: /data/hostObservation/extractor/rawRows/namedObservations/187/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-188" assertion으로 "concrete-links-188: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-188" assertion으로 "link-name-188: /data/hostObservation/extractor/rawRows/namedObservations/188/observationName의 실제 equals 기대값은 'expiry-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-188" assertion으로 "link-oracle-188: /data/hostObservation/extractor/rawRows/namedObservations/188/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-189" assertion으로 "concrete-links-189: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-189" assertion으로 "link-name-189: /data/hostObservation/extractor/rawRows/namedObservations/189/observationName의 실제 equals 기대값은 'boundary-recheck'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-189" assertion으로 "link-oracle-189: /data/hostObservation/extractor/rawRows/namedObservations/189/oracleId의 실제 equals 기대값은 'T16.no-event-expiry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-190" assertion으로 "concrete-links-190: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-190" assertion으로 "link-name-190: /data/hostObservation/extractor/rawRows/namedObservations/190/observationName의 실제 equals 기대값은 'held-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-190" assertion으로 "link-oracle-190: /data/hostObservation/extractor/rawRows/namedObservations/190/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-191" assertion으로 "concrete-links-191: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-191" assertion으로 "link-name-191: /data/hostObservation/extractor/rawRows/namedObservations/191/observationName의 실제 equals 기대값은 'eligible-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-191" assertion으로 "link-oracle-191: /data/hostObservation/extractor/rawRows/namedObservations/191/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-192" assertion으로 "concrete-links-192: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-192" assertion으로 "link-name-192: /data/hostObservation/extractor/rawRows/namedObservations/192/observationName의 실제 equals 기대값은 'unreserved-after-reserve'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-192" assertion으로 "link-oracle-192: /data/hostObservation/extractor/rawRows/namedObservations/192/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-193" assertion으로 "concrete-links-193: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-193" assertion으로 "link-name-193: /data/hostObservation/extractor/rawRows/namedObservations/193/observationName의 실제 equals 기대값은 'warehouse-after-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-193" assertion으로 "link-oracle-193: /data/hostObservation/extractor/rawRows/namedObservations/193/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-194" assertion으로 "concrete-links-194: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-194" assertion으로 "link-name-194: /data/hostObservation/extractor/rawRows/namedObservations/194/observationName의 실제 equals 기대값은 'dispatch-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-194" assertion으로 "link-oracle-194: /data/hostObservation/extractor/rawRows/namedObservations/194/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-195" assertion으로 "concrete-links-195: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-195" assertion으로 "link-name-195: /data/hostObservation/extractor/rawRows/namedObservations/195/observationName의 실제 equals 기대값은 'in-transit-after-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-195" assertion으로 "link-oracle-195: /data/hostObservation/extractor/rawRows/namedObservations/195/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-196" assertion으로 "concrete-links-196: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-196" assertion으로 "link-name-196: /data/hostObservation/extractor/rawRows/namedObservations/196/observationName의 실제 equals 기대값은 'delivered-before-observation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-196" assertion으로 "link-oracle-196: /data/hostObservation/extractor/rawRows/namedObservations/196/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-197" assertion으로 "concrete-links-197: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-197" assertion으로 "link-name-197: /data/hostObservation/extractor/rawRows/namedObservations/197/observationName의 실제 equals 기대값은 'second-reservation-same60'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-197" assertion으로 "link-oracle-197: /data/hostObservation/extractor/rawRows/namedObservations/197/oracleId의 실제 equals 기대값은 'T17.reserve-pick-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-198" assertion으로 "concrete-links-198: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-198" assertion으로 "link-name-198: /data/hostObservation/extractor/rawRows/namedObservations/198/observationName의 실제 equals 기대값은 'delivered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-198" assertion으로 "link-oracle-198: /data/hostObservation/extractor/rawRows/namedObservations/198/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-199" assertion으로 "concrete-links-199: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-199" assertion으로 "link-name-199: /data/hostObservation/extractor/rawRows/namedObservations/199/observationName의 실제 equals 기대값은 'cargo-in-transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-199" assertion으로 "link-oracle-199: /data/hostObservation/extractor/rawRows/namedObservations/199/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-200" assertion으로 "concrete-links-200: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-200" assertion으로 "link-name-200: /data/hostObservation/extractor/rawRows/namedObservations/200/observationName의 실제 equals 기대값은 'delivery-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-200" assertion으로 "link-oracle-200: /data/hostObservation/extractor/rawRows/namedObservations/200/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-201" assertion으로 "concrete-links-201: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-201" assertion으로 "link-name-201: /data/hostObservation/extractor/rawRows/namedObservations/201/observationName의 실제 equals 기대값은 'allocation-still'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-201" assertion으로 "link-oracle-201: /data/hostObservation/extractor/rawRows/namedObservations/201/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-202" assertion으로 "concrete-links-202: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-202" assertion으로 "link-name-202: /data/hostObservation/extractor/rawRows/namedObservations/202/observationName의 실제 equals 기대값은 'delivery-created-new-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-202" assertion으로 "link-oracle-202: /data/hostObservation/extractor/rawRows/namedObservations/202/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-203" assertion으로 "concrete-links-203: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-203" assertion으로 "link-name-203: /data/hostObservation/extractor/rawRows/namedObservations/203/observationName의 실제 equals 기대값은 'delivery-created-new-warehouse-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-203" assertion으로 "link-oracle-203: /data/hostObservation/extractor/rawRows/namedObservations/203/oracleId의 실제 equals 기대값은 'T17.normal-consumed-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-204" assertion으로 "concrete-links-204: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-204" assertion으로 "link-name-204: /data/hostObservation/extractor/rawRows/namedObservations/204/observationName의 실제 equals 기대값은 'actual-delivered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-204" assertion으로 "link-oracle-204: /data/hostObservation/extractor/rawRows/namedObservations/204/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-205" assertion으로 "concrete-links-205: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-205" assertion으로 "link-name-205: /data/hostObservation/extractor/rawRows/namedObservations/205/observationName의 실제 equals 기대값은 'remaining-transit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-205" assertion으로 "link-oracle-205: /data/hostObservation/extractor/rawRows/namedObservations/205/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-206" assertion으로 "concrete-links-206: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-206" assertion으로 "link-name-206: /data/hostObservation/extractor/rawRows/namedObservations/206/observationName의 실제 equals 기대값은 'new-warehouse-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-206" assertion으로 "link-oracle-206: /data/hostObservation/extractor/rawRows/namedObservations/206/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-207" assertion으로 "concrete-links-207: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-207" assertion으로 "link-name-207: /data/hostObservation/extractor/rawRows/namedObservations/207/observationName의 실제 equals 기대값은 'new-executable-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-207" assertion으로 "link-oracle-207: /data/hostObservation/extractor/rawRows/namedObservations/207/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-208" assertion으로 "concrete-links-208: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-208" assertion으로 "link-name-208: /data/hostObservation/extractor/rawRows/namedObservations/208/observationName의 실제 equals 기대값은 'fact-not-permission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-208" assertion으로 "link-oracle-208: /data/hostObservation/extractor/rawRows/namedObservations/208/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-209" assertion으로 "concrete-links-209: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-209" assertion으로 "link-name-209: /data/hostObservation/extractor/rawRows/namedObservations/209/observationName의 실제 equals 기대값은 'late-restriction-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-209" assertion으로 "link-oracle-209: /data/hostObservation/extractor/rawRows/namedObservations/209/oracleId의 실제 equals 기대값은 'T17.late-restriction-actual-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-210" assertion으로 "concrete-links-210: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-210" assertion으로 "link-name-210: /data/hostObservation/extractor/rawRows/namedObservations/210/observationName의 실제 equals 기대값은 'claim-inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-210" assertion으로 "link-oracle-210: /data/hostObservation/extractor/rawRows/namedObservations/210/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-211" assertion으로 "concrete-links-211: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-211" assertion으로 "link-name-211: /data/hostObservation/extractor/rawRows/namedObservations/211/observationName의 실제 equals 기대값은 'claim-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-211" assertion으로 "link-oracle-211: /data/hostObservation/extractor/rawRows/namedObservations/211/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-212" assertion으로 "concrete-links-212: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-212" assertion으로 "link-name-212: /data/hostObservation/extractor/rawRows/namedObservations/212/observationName의 실제 equals 기대값은 'claim-new-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-212" assertion으로 "link-oracle-212: /data/hostObservation/extractor/rawRows/namedObservations/212/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-213" assertion으로 "concrete-links-213: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-213" assertion으로 "link-name-213: /data/hostObservation/extractor/rawRows/namedObservations/213/observationName의 실제 equals 기대값은 'claim-not-canonical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-213" assertion으로 "link-oracle-213: /data/hostObservation/extractor/rawRows/namedObservations/213/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-214" assertion으로 "concrete-links-214: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-214" assertion으로 "link-name-214: /data/hostObservation/extractor/rawRows/namedObservations/214/observationName의 실제 equals 기대값은 'claim-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-214" assertion으로 "link-oracle-214: /data/hostObservation/extractor/rawRows/namedObservations/214/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-215" assertion으로 "concrete-links-215: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-215" assertion으로 "link-name-215: /data/hostObservation/extractor/rawRows/namedObservations/215/observationName의 실제 equals 기대값은 'no-prior-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-215" assertion으로 "link-oracle-215: /data/hostObservation/extractor/rawRows/namedObservations/215/oracleId의 실제 equals 기대값은 'T17.false-or-unmatched-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-216" assertion으로 "concrete-links-216: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-216" assertion으로 "link-name-216: /data/hostObservation/extractor/rawRows/namedObservations/216/observationName의 실제 equals 기대값은 'old-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-216" assertion으로 "link-oracle-216: /data/hostObservation/extractor/rawRows/namedObservations/216/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-217" assertion으로 "concrete-links-217: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-217" assertion으로 "link-name-217: /data/hostObservation/extractor/rawRows/namedObservations/217/observationName의 실제 equals 기대값은 'new-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-217" assertion으로 "link-oracle-217: /data/hostObservation/extractor/rawRows/namedObservations/217/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-218" assertion으로 "concrete-links-218: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-218" assertion으로 "link-name-218: /data/hostObservation/extractor/rawRows/namedObservations/218/observationName의 실제 equals 기대값은 'executable-reserved-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-218" assertion으로 "link-oracle-218: /data/hostObservation/extractor/rawRows/namedObservations/218/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-219" assertion으로 "concrete-links-219: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-219" assertion으로 "link-name-219: /data/hostObservation/extractor/rawRows/namedObservations/219/observationName의 실제 equals 기대값은 'old-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-219" assertion으로 "link-oracle-219: /data/hostObservation/extractor/rawRows/namedObservations/219/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-220" assertion으로 "concrete-links-220: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-220" assertion으로 "link-name-220: /data/hostObservation/extractor/rawRows/namedObservations/220/observationName의 실제 equals 기대값은 'replacement-atomic'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-220" assertion으로 "link-oracle-220: /data/hostObservation/extractor/rawRows/namedObservations/220/oracleId의 실제 equals 기대값은 'T17.allocation-replacement-no-revival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-221" assertion으로 "concrete-links-221: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-221" assertion으로 "link-name-221: /data/hostObservation/extractor/rawRows/namedObservations/221/observationName의 실제 equals 기대값은 'eligibility-response'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-221" assertion으로 "link-oracle-221: /data/hostObservation/extractor/rawRows/namedObservations/221/oracleId의 실제 equals 기대값은 'T17.eligibility-fefo-contract'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-222" assertion으로 "concrete-links-222: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-222" assertion으로 "link-name-222: /data/hostObservation/extractor/rawRows/namedObservations/222/observationName의 실제 equals 기대값은 'unauthorized-packaging-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-222" assertion으로 "link-oracle-222: /data/hostObservation/extractor/rawRows/namedObservations/222/oracleId의 실제 equals 기대값은 'T17.eligibility-fefo-contract'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-223" assertion으로 "concrete-links-223: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-223" assertion으로 "link-name-223: /data/hostObservation/extractor/rawRows/namedObservations/223/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-223" assertion으로 "link-oracle-223: /data/hostObservation/extractor/rawRows/namedObservations/223/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-224" assertion으로 "concrete-links-224: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-224" assertion으로 "link-name-224: /data/hostObservation/extractor/rawRows/namedObservations/224/observationName의 실제 equals 기대값은 'return-received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-224" assertion으로 "link-oracle-224: /data/hostObservation/extractor/rawRows/namedObservations/224/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-225" assertion으로 "concrete-links-225: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-225" assertion으로 "link-name-225: /data/hostObservation/extractor/rawRows/namedObservations/225/observationName의 실제 equals 기대값은 'return-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-225" assertion으로 "link-oracle-225: /data/hostObservation/extractor/rawRows/namedObservations/225/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-226" assertion으로 "concrete-links-226: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-226" assertion으로 "link-name-226: /data/hostObservation/extractor/rawRows/namedObservations/226/observationName의 실제 equals 기대값은 'duplicate-return-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-226" assertion으로 "link-oracle-226: /data/hostObservation/extractor/rawRows/namedObservations/226/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-227" assertion으로 "concrete-links-227: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-227" assertion으로 "link-name-227: /data/hostObservation/extractor/rawRows/namedObservations/227/observationName의 실제 equals 기대값은 'return-added-purchase-contribution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-227" assertion으로 "link-oracle-227: /data/hostObservation/extractor/rawRows/namedObservations/227/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-228" assertion으로 "concrete-links-228: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-228" assertion으로 "link-name-228: /data/hostObservation/extractor/rawRows/namedObservations/228/observationName의 실제 equals 기대값은 'return-followup'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-228" assertion으로 "link-oracle-228: /data/hostObservation/extractor/rawRows/namedObservations/228/oracleId의 실제 equals 기대값은 'T18.return-new-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-229" assertion으로 "concrete-links-229: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-229" assertion으로 "link-name-229: /data/hostObservation/extractor/rawRows/namedObservations/229/observationName의 실제 equals 기대값은 'bidirectional-trace'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-229" assertion으로 "link-oracle-229: /data/hostObservation/extractor/rawRows/namedObservations/229/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-230" assertion으로 "concrete-links-230: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-230" assertion으로 "link-name-230: /data/hostObservation/extractor/rawRows/namedObservations/230/observationName의 실제 equals 기대값은 'unauthorized-recall-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-230" assertion으로 "link-oracle-230: /data/hostObservation/extractor/rawRows/namedObservations/230/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-231" assertion으로 "concrete-links-231: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-231" assertion으로 "link-name-231: /data/hostObservation/extractor/rawRows/namedObservations/231/observationName의 실제 equals 기대값은 'approval-notice'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-231" assertion으로 "link-oracle-231: /data/hostObservation/extractor/rawRows/namedObservations/231/oracleId의 실제 equals 기대값은 'T18.trace-and-recall-decisions'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-232" assertion으로 "concrete-links-232: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-232" assertion으로 "link-name-232: /data/hostObservation/extractor/rawRows/namedObservations/232/observationName의 실제 equals 기대값은 'recovery-distinct'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-232" assertion으로 "link-oracle-232: /data/hostObservation/extractor/rawRows/namedObservations/232/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-233" assertion으로 "concrete-links-233: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-233" assertion으로 "link-name-233: /data/hostObservation/extractor/rawRows/namedObservations/233/observationName의 실제 equals 기대값은 'processed-distinct'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-233" assertion으로 "link-oracle-233: /data/hostObservation/extractor/rawRows/namedObservations/233/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-234" assertion으로 "concrete-links-234: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-234" assertion으로 "link-name-234: /data/hostObservation/extractor/rawRows/namedObservations/234/observationName의 실제 equals 기대값은 'unknown'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-234" assertion으로 "link-oracle-234: /data/hostObservation/extractor/rawRows/namedObservations/234/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-235" assertion으로 "concrete-links-235: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-235" assertion으로 "link-name-235: /data/hostObservation/extractor/rawRows/namedObservations/235/observationName의 실제 equals 기대값은 'false-closure'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-235" assertion으로 "link-oracle-235: /data/hostObservation/extractor/rawRows/namedObservations/235/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-236" assertion으로 "concrete-links-236: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-236" assertion으로 "link-name-236: /data/hostObservation/extractor/rawRows/namedObservations/236/observationName의 실제 equals 기대값은 'exclusive-endstates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-236" assertion으로 "link-oracle-236: /data/hostObservation/extractor/rawRows/namedObservations/236/oracleId의 실제 equals 기대값은 'T18.recall-closure-accounting'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-237" assertion으로 "concrete-links-237: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-237" assertion으로 "link-name-237: /data/hostObservation/extractor/rawRows/namedObservations/237/observationName의 실제 equals 기대값은 'logistics-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-237" assertion으로 "link-oracle-237: /data/hostObservation/extractor/rawRows/namedObservations/237/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-238" assertion으로 "concrete-links-238: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-238" assertion으로 "link-name-238: /data/hostObservation/extractor/rawRows/namedObservations/238/observationName의 실제 equals 기대값은 'original-invoice-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-238" assertion으로 "link-oracle-238: /data/hostObservation/extractor/rawRows/namedObservations/238/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-239" assertion으로 "concrete-links-239: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-239" assertion으로 "link-name-239: /data/hostObservation/extractor/rawRows/namedObservations/239/observationName의 실제 equals 기대값은 'settlement-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-239" assertion으로 "link-oracle-239: /data/hostObservation/extractor/rawRows/namedObservations/239/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-240" assertion으로 "concrete-links-240: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-240" assertion으로 "link-name-240: /data/hostObservation/extractor/rawRows/namedObservations/240/observationName의 실제 equals 기대값은 'settlement-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-240" assertion으로 "link-oracle-240: /data/hostObservation/extractor/rawRows/namedObservations/240/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-241" assertion으로 "concrete-links-241: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-241" assertion으로 "link-name-241: /data/hostObservation/extractor/rawRows/namedObservations/241/observationName의 실제 equals 기대값은 'matching-links'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-241" assertion으로 "link-oracle-241: /data/hostObservation/extractor/rawRows/namedObservations/241/oracleId의 실제 equals 기대값은 'T19.invoice-quantity-price-match'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-242" assertion으로 "concrete-links-242: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-242" assertion으로 "link-name-242: /data/hostObservation/extractor/rawRows/namedObservations/242/observationName의 실제 equals 기대값은 'original-amount'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-242" assertion으로 "link-oracle-242: /data/hostObservation/extractor/rawRows/namedObservations/242/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-243" assertion으로 "concrete-links-243: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-243" assertion으로 "link-name-243: /data/hostObservation/extractor/rawRows/namedObservations/243/observationName의 실제 equals 기대값은 'converted-amount'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-243" assertion으로 "link-oracle-243: /data/hostObservation/extractor/rawRows/namedObservations/243/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-244" assertion으로 "concrete-links-244: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-244" assertion으로 "link-name-244: /data/hostObservation/extractor/rawRows/namedObservations/244/observationName의 실제 equals 기대값은 'fx-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-244" assertion으로 "link-oracle-244: /data/hostObservation/extractor/rawRows/namedObservations/244/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-245" assertion으로 "concrete-links-245: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-245" assertion으로 "link-name-245: /data/hostObservation/extractor/rawRows/namedObservations/245/observationName의 실제 equals 기대값은 'commercial-as-domestic-tax-document'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-245" assertion으로 "link-oracle-245: /data/hostObservation/extractor/rawRows/namedObservations/245/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-246" assertion으로 "concrete-links-246: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-246" assertion으로 "link-name-246: /data/hostObservation/extractor/rawRows/namedObservations/246/observationName의 실제 equals 기대값은 'payment-reference-bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-246" assertion으로 "link-oracle-246: /data/hostObservation/extractor/rawRows/namedObservations/246/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-247" assertion으로 "concrete-links-247: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-247" assertion으로 "link-name-247: /data/hostObservation/extractor/rawRows/namedObservations/247/observationName의 실제 equals 기대값은 'automatic-tax-issue-or-submit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-247" assertion으로 "link-oracle-247: /data/hostObservation/extractor/rawRows/namedObservations/247/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-248" assertion으로 "concrete-links-248: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-248" assertion으로 "link-name-248: /data/hostObservation/extractor/rawRows/namedObservations/248/observationName의 실제 equals 기대값은 'qc-pass-payment-authorization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-248" assertion으로 "link-oracle-248: /data/hostObservation/extractor/rawRows/namedObservations/248/oracleId의 실제 equals 기대값은 'T19.fx-document-payment-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-249" assertion으로 "concrete-links-249: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-249" assertion으로 "link-name-249: /data/hostObservation/extractor/rawRows/namedObservations/249/observationName의 실제 equals 기대값은 'ordinary-write-confirmed-settlement-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-249" assertion으로 "link-oracle-249: /data/hostObservation/extractor/rawRows/namedObservations/249/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-250" assertion으로 "concrete-links-250: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-250" assertion으로 "link-name-250: /data/hostObservation/extractor/rawRows/namedObservations/250/observationName의 실제 equals 기대값은 'observations-proposal-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-250" assertion으로 "link-oracle-250: /data/hostObservation/extractor/rawRows/namedObservations/250/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-251" assertion으로 "concrete-links-251: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-251" assertion으로 "link-name-251: /data/hostObservation/extractor/rawRows/namedObservations/251/observationName의 실제 equals 기대값은 'authorized-confirmation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-251" assertion으로 "link-oracle-251: /data/hostObservation/extractor/rawRows/namedObservations/251/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-252" assertion으로 "concrete-links-252: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-252" assertion으로 "link-name-252: /data/hostObservation/extractor/rawRows/namedObservations/252/observationName의 실제 equals 기대값은 'confirmed-settlement-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-252" assertion으로 "link-oracle-252: /data/hostObservation/extractor/rawRows/namedObservations/252/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-253" assertion으로 "concrete-links-253: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-253" assertion으로 "link-name-253: /data/hostObservation/extractor/rawRows/namedObservations/253/observationName의 실제 equals 기대값은 'preserved-original-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-253" assertion으로 "link-oracle-253: /data/hostObservation/extractor/rawRows/namedObservations/253/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-254" assertion으로 "concrete-links-254: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-254" assertion으로 "link-name-254: /data/hostObservation/extractor/rawRows/namedObservations/254/observationName의 실제 equals 기대값은 'confirmed-difference-bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-254" assertion으로 "link-oracle-254: /data/hostObservation/extractor/rawRows/namedObservations/254/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-255" assertion으로 "concrete-links-255: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-255" assertion으로 "link-name-255: /data/hostObservation/extractor/rawRows/namedObservations/255/observationName의 실제 equals 기대값은 'management-decision-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-255" assertion으로 "link-oracle-255: /data/hostObservation/extractor/rawRows/namedObservations/255/oracleId의 실제 equals 기대값은 'T19.settlement-manager-decision'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-256" assertion으로 "concrete-links-256: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-256" assertion으로 "link-name-256: /data/hostObservation/extractor/rawRows/namedObservations/256/observationName의 실제 equals 기대값은 'intent-validation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-256" assertion으로 "link-oracle-256: /data/hostObservation/extractor/rawRows/namedObservations/256/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-257" assertion으로 "concrete-links-257: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-257" assertion으로 "link-name-257: /data/hostObservation/extractor/rawRows/namedObservations/257/observationName의 실제 equals 기대값은 'input-collection-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-257" assertion으로 "link-oracle-257: /data/hostObservation/extractor/rawRows/namedObservations/257/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-258" assertion으로 "concrete-links-258: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-258" assertion으로 "link-name-258: /data/hostObservation/extractor/rawRows/namedObservations/258/observationName의 실제 equals 기대값은 'canonicalization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-258" assertion으로 "link-oracle-258: /data/hostObservation/extractor/rawRows/namedObservations/258/oracleId의 실제 equals 기대값은 'T20.structured-intent-stages'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-259" assertion으로 "concrete-links-259: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-259" assertion으로 "link-name-259: /data/hostObservation/extractor/rawRows/namedObservations/259/observationName의 실제 equals 기대값은 'wire-protocol'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-259" assertion으로 "link-oracle-259: /data/hostObservation/extractor/rawRows/namedObservations/259/oracleId의 실제 equals 기대값은 'T20.mcp-stateless-wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-260" assertion으로 "concrete-links-260: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-260" assertion으로 "link-name-260: /data/hostObservation/extractor/rawRows/namedObservations/260/observationName의 실제 equals 기대값은 'domain-parity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-260" assertion으로 "link-oracle-260: /data/hostObservation/extractor/rawRows/namedObservations/260/oracleId의 실제 equals 기대값은 'T20.mcp-stateless-wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-261" assertion으로 "concrete-links-261: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-261" assertion으로 "link-name-261: /data/hostObservation/extractor/rawRows/namedObservations/261/observationName의 실제 equals 기대값은 'mrtr-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-261" assertion으로 "link-oracle-261: /data/hostObservation/extractor/rawRows/namedObservations/261/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-262" assertion으로 "concrete-links-262: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-262" assertion으로 "link-name-262: /data/hostObservation/extractor/rawRows/namedObservations/262/observationName의 실제 equals 기대값은 'requeststate-as-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-262" assertion으로 "link-oracle-262: /data/hostObservation/extractor/rawRows/namedObservations/262/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-263" assertion으로 "concrete-links-263: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-263" assertion으로 "link-name-263: /data/hostObservation/extractor/rawRows/namedObservations/263/observationName의 실제 equals 기대값은 'accept-string-as-approval'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-263" assertion으로 "link-oracle-263: /data/hostObservation/extractor/rawRows/namedObservations/263/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-264" assertion으로 "concrete-links-264: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-264" assertion으로 "link-name-264: /data/hostObservation/extractor/rawRows/namedObservations/264/observationName의 실제 equals 기대값은 'invalid-continuation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-264" assertion으로 "link-oracle-264: /data/hostObservation/extractor/rawRows/namedObservations/264/oracleId의 실제 equals 기대값은 'T20.mrtr-bound-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-265" assertion으로 "concrete-links-265: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-265" assertion으로 "link-name-265: /data/hostObservation/extractor/rawRows/namedObservations/265/observationName의 실제 equals 기대값은 'skill-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-265" assertion으로 "link-oracle-265: /data/hostObservation/extractor/rawRows/namedObservations/265/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-266" assertion으로 "concrete-links-266: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-266" assertion으로 "link-name-266: /data/hostObservation/extractor/rawRows/namedObservations/266/observationName의 실제 equals 기대값은 'skill-hash-as-loading-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-266" assertion으로 "link-oracle-266: /data/hostObservation/extractor/rawRows/namedObservations/266/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-267" assertion으로 "concrete-links-267: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-267" assertion으로 "link-name-267: /data/hostObservation/extractor/rawRows/namedObservations/267/observationName의 실제 equals 기대값은 'allowed-tools-as-server-authorization'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-267" assertion으로 "link-oracle-267: /data/hostObservation/extractor/rawRows/namedObservations/267/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-268" assertion으로 "concrete-links-268: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-268" assertion으로 "link-name-268: /data/hostObservation/extractor/rawRows/namedObservations/268/observationName의 실제 equals 기대값은 'document-instruction-authority'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-268" assertion으로 "link-oracle-268: /data/hostObservation/extractor/rawRows/namedObservations/268/oracleId의 실제 equals 기대값은 'T20.skills-real-loading-and-meaning'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-269" assertion으로 "concrete-links-269: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-269" assertion으로 "link-name-269: /data/hostObservation/extractor/rawRows/namedObservations/269/observationName의 실제 equals 기대값은 'definition-lifecycle'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-269" assertion으로 "link-oracle-269: /data/hostObservation/extractor/rawRows/namedObservations/269/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-270" assertion으로 "concrete-links-270: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-270" assertion으로 "link-name-270: /data/hostObservation/extractor/rawRows/namedObservations/270/observationName의 실제 equals 기대값은 'impact-validation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-270" assertion으로 "link-oracle-270: /data/hostObservation/extractor/rawRows/namedObservations/270/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-271" assertion으로 "concrete-links-271: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-271" assertion으로 "link-name-271: /data/hostObservation/extractor/rawRows/namedObservations/271/observationName의 실제 equals 기대값은 'invalid-payload-published'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-271" assertion으로 "link-oracle-271: /data/hostObservation/extractor/rawRows/namedObservations/271/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-272" assertion으로 "concrete-links-272: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-272" assertion으로 "link-name-272: /data/hostObservation/extractor/rawRows/namedObservations/272/observationName의 실제 equals 기대값은 'definition-generated-new-capability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-272" assertion으로 "link-oracle-272: /data/hostObservation/extractor/rawRows/namedObservations/272/oracleId의 실제 equals 기대값은 'T21.definition-publish-and-impact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-273" assertion으로 "concrete-links-273: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-273" assertion으로 "link-name-273: /data/hostObservation/extractor/rawRows/namedObservations/273/observationName의 실제 equals 기대값은 'old-work-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-273" assertion으로 "link-oracle-273: /data/hostObservation/extractor/rawRows/namedObservations/273/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-274" assertion으로 "concrete-links-274: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-274" assertion으로 "link-name-274: /data/hostObservation/extractor/rawRows/namedObservations/274/observationName의 실제 equals 기대값은 'new-work-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-274" assertion으로 "link-oracle-274: /data/hostObservation/extractor/rawRows/namedObservations/274/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-275" assertion으로 "concrete-links-275: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-275" assertion으로 "link-name-275: /data/hostObservation/extractor/rawRows/namedObservations/275/observationName의 실제 equals 기대값은 'current-sell-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-275" assertion으로 "link-oracle-275: /data/hostObservation/extractor/rawRows/namedObservations/275/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-276" assertion으로 "concrete-links-276: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-276" assertion으로 "link-name-276: /data/hostObservation/extractor/rawRows/namedObservations/276/observationName의 실제 equals 기대값은 'v1-used-to-bypass-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-276" assertion으로 "link-oracle-276: /data/hostObservation/extractor/rawRows/namedObservations/276/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-277" assertion으로 "concrete-links-277: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-277" assertion으로 "link-name-277: /data/hostObservation/extractor/rawRows/namedObservations/277/observationName의 실제 equals 기대값은 'v1-correction-reassessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-277" assertion으로 "link-oracle-277: /data/hostObservation/extractor/rawRows/namedObservations/277/oracleId의 실제 equals 기대값은 'T21.pinned-old-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-278" assertion으로 "concrete-links-278: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-278" assertion으로 "link-name-278: /data/hostObservation/extractor/rawRows/namedObservations/278/observationName의 실제 equals 기대값은 'unsupported-version-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-278" assertion으로 "link-oracle-278: /data/hostObservation/extractor/rawRows/namedObservations/278/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-279" assertion으로 "concrete-links-279: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-279" assertion으로 "link-name-279: /data/hostObservation/extractor/rawRows/namedObservations/279/observationName의 실제 equals 기대값은 'unsupported-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-279" assertion으로 "link-oracle-279: /data/hostObservation/extractor/rawRows/namedObservations/279/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-280" assertion으로 "concrete-links-280: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-280" assertion으로 "link-name-280: /data/hostObservation/extractor/rawRows/namedObservations/280/observationName의 실제 equals 기대값은 'unsupported-version-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-280" assertion으로 "link-oracle-280: /data/hostObservation/extractor/rawRows/namedObservations/280/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-281" assertion으로 "concrete-links-281: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-281" assertion으로 "link-name-281: /data/hostObservation/extractor/rawRows/namedObservations/281/observationName의 실제 equals 기대값은 'explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-281" assertion으로 "link-oracle-281: /data/hostObservation/extractor/rawRows/namedObservations/281/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-282" assertion으로 "concrete-links-282: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-282" assertion으로 "link-name-282: /data/hostObservation/extractor/rawRows/namedObservations/282/observationName의 실제 equals 기대값은 'active-pointer-rollback-physical-compensation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-282" assertion으로 "link-oracle-282: /data/hostObservation/extractor/rawRows/namedObservations/282/oracleId의 실제 equals 기대값은 'T21.unsupported-and-explicit-migration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-283" assertion으로 "concrete-links-283: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-283" assertion으로 "link-name-283: /data/hostObservation/extractor/rawRows/namedObservations/283/observationName의 실제 equals 기대값은 'accepted-inbox-per-key'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-283" assertion으로 "link-oracle-283: /data/hostObservation/extractor/rawRows/namedObservations/283/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-284" assertion으로 "concrete-links-284: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-284" assertion으로 "link-name-284: /data/hostObservation/extractor/rawRows/namedObservations/284/observationName의 실제 equals 기대값은 'same-key-other-hash'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-284" assertion으로 "link-oracle-284: /data/hostObservation/extractor/rawRows/namedObservations/284/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-285" assertion으로 "concrete-links-285: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-285" assertion으로 "link-name-285: /data/hostObservation/extractor/rawRows/namedObservations/285/observationName의 실제 equals 기대값은 'canonical-receipt'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-285" assertion으로 "link-oracle-285: /data/hostObservation/extractor/rawRows/namedObservations/285/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-286" assertion으로 "concrete-links-286: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-286" assertion으로 "link-name-286: /data/hostObservation/extractor/rawRows/namedObservations/286/observationName의 실제 equals 기대값은 'last-write-wins'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-286" assertion으로 "link-oracle-286: /data/hostObservation/extractor/rawRows/namedObservations/286/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-287" assertion으로 "concrete-links-287: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-287" assertion으로 "link-name-287: /data/hostObservation/extractor/rawRows/namedObservations/287/observationName의 실제 equals 기대값은 'identity-unverified-auto-sum'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-287" assertion으로 "link-oracle-287: /data/hostObservation/extractor/rawRows/namedObservations/287/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-288" assertion으로 "concrete-links-288: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-288" assertion으로 "link-name-288: /data/hostObservation/extractor/rawRows/namedObservations/288/observationName의 실제 equals 기대값은 'source-conflict-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-288" assertion으로 "link-oracle-288: /data/hostObservation/extractor/rawRows/namedObservations/288/oracleId의 실제 equals 기대값은 'T22.source-duplicate-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-289" assertion으로 "concrete-links-289: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-289" assertion으로 "link-name-289: /data/hostObservation/extractor/rawRows/namedObservations/289/observationName의 실제 equals 기대값은 'invalid-match-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-289" assertion으로 "link-oracle-289: /data/hostObservation/extractor/rawRows/namedObservations/289/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-290" assertion으로 "concrete-links-290: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-290" assertion으로 "link-name-290: /data/hostObservation/extractor/rawRows/namedObservations/290/observationName의 실제 equals 기대값은 'old-source-overwrites-new-restriction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-290" assertion으로 "link-oracle-290: /data/hostObservation/extractor/rawRows/namedObservations/290/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-291" assertion으로 "concrete-links-291: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-291" assertion으로 "link-name-291: /data/hostObservation/extractor/rawRows/namedObservations/291/observationName의 실제 equals 기대값은 'unprofiled-connector-activation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-291" assertion으로 "link-oracle-291: /data/hostObservation/extractor/rawRows/namedObservations/291/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-292" assertion으로 "concrete-links-292: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-292" assertion으로 "link-name-292: /data/hostObservation/extractor/rawRows/namedObservations/292/observationName의 실제 equals 기대값은 'source-profile'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-292" assertion으로 "link-oracle-292: /data/hostObservation/extractor/rawRows/namedObservations/292/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-293" assertion으로 "concrete-links-293: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-293" assertion으로 "link-name-293: /data/hostObservation/extractor/rawRows/namedObservations/293/observationName의 실제 equals 기대값은 'quarantine-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-293" assertion으로 "link-oracle-293: /data/hostObservation/extractor/rawRows/namedObservations/293/oracleId의 실제 equals 기대값은 'T22.reconciliation-control-and-source-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-294" assertion으로 "concrete-links-294: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-294" assertion으로 "link-name-294: /data/hostObservation/extractor/rawRows/namedObservations/294/observationName의 실제 equals 기대값은 'lost-response-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-294" assertion으로 "link-oracle-294: /data/hostObservation/extractor/rawRows/namedObservations/294/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-295" assertion으로 "concrete-links-295: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-295" assertion으로 "link-name-295: /data/hostObservation/extractor/rawRows/namedObservations/295/observationName의 실제 equals 기대값은 'unreconciled-external-reissue'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-295" assertion으로 "link-oracle-295: /data/hostObservation/extractor/rawRows/namedObservations/295/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-296" assertion으로 "concrete-links-296: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-296" assertion으로 "link-name-296: /data/hostObservation/extractor/rawRows/namedObservations/296/observationName의 실제 equals 기대값은 'confirmed-success-reissue'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-296" assertion으로 "link-oracle-296: /data/hostObservation/extractor/rawRows/namedObservations/296/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-297" assertion으로 "concrete-links-297: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-297" assertion으로 "link-name-297: /data/hostObservation/extractor/rawRows/namedObservations/297/observationName의 실제 equals 기대값은 'external-transition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-297" assertion으로 "link-oracle-297: /data/hostObservation/extractor/rawRows/namedObservations/297/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-298" assertion으로 "concrete-links-298: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-298" assertion으로 "link-name-298: /data/hostObservation/extractor/rawRows/namedObservations/298/observationName의 실제 equals 기대값은 'external-reconciliation-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-298" assertion으로 "link-oracle-298: /data/hostObservation/extractor/rawRows/namedObservations/298/oracleId의 실제 equals 기대값은 'T22.unknown-external-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-299" assertion으로 "concrete-links-299: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-299" assertion으로 "link-name-299: /data/hostObservation/extractor/rawRows/namedObservations/299/observationName의 실제 equals 기대값은 'archive-restored'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-299" assertion으로 "link-oracle-299: /data/hostObservation/extractor/rawRows/namedObservations/299/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-300" assertion으로 "concrete-links-300: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-300" assertion으로 "link-name-300: /data/hostObservation/extractor/rawRows/namedObservations/300/observationName의 실제 equals 기대값은 'data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-300" assertion으로 "link-oracle-300: /data/hostObservation/extractor/rawRows/namedObservations/300/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-301" assertion으로 "concrete-links-301: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-301" assertion으로 "link-name-301: /data/hostObservation/extractor/rawRows/namedObservations/301/observationName의 실제 equals 기대값은 'manufacture-as-import-relabel'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-301" assertion으로 "link-oracle-301: /data/hostObservation/extractor/rawRows/namedObservations/301/oracleId의 실제 equals 기대값은 'T23.archive-and-data-branch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-302" assertion으로 "concrete-links-302: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-302" assertion으로 "link-name-302: /data/hostObservation/extractor/rawRows/namedObservations/302/observationName의 실제 equals 기대값은 'schema-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-302" assertion으로 "link-oracle-302: /data/hostObservation/extractor/rawRows/namedObservations/302/oracleId의 실제 equals 기대값은 'T23.schema-ownership-upgrade-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-303" assertion으로 "concrete-links-303: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-303" assertion으로 "link-name-303: /data/hostObservation/extractor/rawRows/namedObservations/303/observationName의 실제 equals 기대값은 'schema-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-303" assertion으로 "link-oracle-303: /data/hostObservation/extractor/rawRows/namedObservations/303/oracleId의 실제 equals 기대값은 'T23.schema-ownership-upgrade-drift'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-304" assertion으로 "concrete-links-304: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-304" assertion으로 "link-name-304: /data/hostObservation/extractor/rawRows/namedObservations/304/observationName의 실제 equals 기대값은 'cutover-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-304" assertion으로 "link-oracle-304: /data/hostObservation/extractor/rawRows/namedObservations/304/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-305" assertion으로 "concrete-links-305: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-305" assertion으로 "link-name-305: /data/hostObservation/extractor/rawRows/namedObservations/305/observationName의 실제 equals 기대값은 'recovery-boundaries'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-305" assertion으로 "link-oracle-305: /data/hostObservation/extractor/rawRows/namedObservations/305/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-306" assertion으로 "concrete-links-306: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-306" assertion으로 "link-name-306: /data/hostObservation/extractor/rawRows/namedObservations/306/observationName의 실제 equals 기대값은 'SQLRollbackPretendedPhysicalContractCancel'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-306" assertion으로 "link-oracle-306: /data/hostObservation/extractor/rawRows/namedObservations/306/oracleId의 실제 equals 기대값은 'T23.cutover-and-safe-recovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-307" assertion으로 "concrete-links-307: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-307" assertion으로 "link-name-307: /data/hostObservation/extractor/rawRows/namedObservations/307/observationName의 실제 equals 기대값은 'cross-org-data-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-307" assertion으로 "link-oracle-307: /data/hostObservation/extractor/rawRows/namedObservations/307/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-308" assertion으로 "concrete-links-308: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-308" assertion으로 "link-name-308: /data/hostObservation/extractor/rawRows/namedObservations/308/observationName의 실제 equals 기대값은 'forbidden-write-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-308" assertion으로 "link-oracle-308: /data/hostObservation/extractor/rawRows/namedObservations/308/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-309" assertion으로 "concrete-links-309: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-309" assertion으로 "link-name-309: /data/hostObservation/extractor/rawRows/namedObservations/309/observationName의 실제 equals 기대값은 'surface-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-309" assertion으로 "link-oracle-309: /data/hostObservation/extractor/rawRows/namedObservations/309/oracleId의 실제 equals 기대값은 'T24.all-auth-surfaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-310" assertion으로 "concrete-links-310: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-310" assertion으로 "link-name-310: /data/hostObservation/extractor/rawRows/namedObservations/310/observationName의 실제 equals 기대값은 'committed-domain-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-310" assertion으로 "link-oracle-310: /data/hostObservation/extractor/rawRows/namedObservations/310/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-311" assertion으로 "concrete-links-311: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-311" assertion으로 "link-name-311: /data/hostObservation/extractor/rawRows/namedObservations/311/observationName의 실제 equals 기대값은 'committed-ledger-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-311" assertion으로 "link-oracle-311: /data/hostObservation/extractor/rawRows/namedObservations/311/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-312" assertion으로 "concrete-links-312: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-312" assertion으로 "link-name-312: /data/hostObservation/extractor/rawRows/namedObservations/312/observationName의 실제 equals 기대값은 'committed-allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-312" assertion으로 "link-oracle-312: /data/hostObservation/extractor/rawRows/namedObservations/312/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-313" assertion으로 "concrete-links-313: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-313" assertion으로 "link-name-313: /data/hostObservation/extractor/rawRows/namedObservations/313/observationName의 실제 equals 기대값은 'committed-duty-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-313" assertion으로 "link-oracle-313: /data/hostObservation/extractor/rawRows/namedObservations/313/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-314" assertion으로 "concrete-links-314: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-314" assertion으로 "link-name-314: /data/hostObservation/extractor/rawRows/namedObservations/314/observationName의 실제 equals 기대값은 'committed-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-314" assertion으로 "link-oracle-314: /data/hostObservation/extractor/rawRows/namedObservations/314/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-315" assertion으로 "concrete-links-315: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-315" assertion으로 "link-name-315: /data/hostObservation/extractor/rawRows/namedObservations/315/observationName의 실제 equals 기대값은 'committed-idempotency-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-315" assertion으로 "link-oracle-315: /data/hostObservation/extractor/rawRows/namedObservations/315/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-316" assertion으로 "concrete-links-316: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-316" assertion으로 "link-name-316: /data/hostObservation/extractor/rawRows/namedObservations/316/observationName의 실제 equals 기대값은 'audit-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-316" assertion으로 "link-oracle-316: /data/hostObservation/extractor/rawRows/namedObservations/316/oracleId의 실제 equals 기대값은 'T24.audit-failure-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-317" assertion으로 "concrete-links-317: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-317" assertion으로 "link-name-317: /data/hostObservation/extractor/rawRows/namedObservations/317/observationName의 실제 equals 기대값은 'legal-hold-delete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-317" assertion으로 "link-oracle-317: /data/hostObservation/extractor/rawRows/namedObservations/317/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-318" assertion으로 "concrete-links-318: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-318" assertion으로 "link-name-318: /data/hostObservation/extractor/rawRows/namedObservations/318/observationName의 실제 equals 기대값은 'unresolved-reference-delete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-318" assertion으로 "link-oracle-318: /data/hostObservation/extractor/rawRows/namedObservations/318/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-319" assertion으로 "concrete-links-319: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-319" assertion으로 "link-name-319: /data/hostObservation/extractor/rawRows/namedObservations/319/observationName의 실제 equals 기대값은 'retention-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-319" assertion으로 "link-oracle-319: /data/hostObservation/extractor/rawRows/namedObservations/319/oracleId의 실제 equals 기대값은 'T24.retention-legalhold-blob-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-320" assertion으로 "concrete-links-320: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-320" assertion으로 "link-name-320: /data/hostObservation/extractor/rawRows/namedObservations/320/observationName의 실제 equals 기대값은 'secret-sentinel-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-320" assertion으로 "link-oracle-320: /data/hostObservation/extractor/rawRows/namedObservations/320/oracleId의 실제 equals 기대값은 'T24.secret-redaction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-321" assertion으로 "concrete-links-321: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-321" assertion으로 "link-name-321: /data/hostObservation/extractor/rawRows/namedObservations/321/observationName의 실제 equals 기대값은 'redaction-purpose'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-321" assertion으로 "link-oracle-321: /data/hostObservation/extractor/rawRows/namedObservations/321/oracleId의 실제 equals 기대값은 'T24.secret-redaction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-322" assertion으로 "concrete-links-322: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-322" assertion으로 "link-name-322: /data/hostObservation/extractor/rawRows/namedObservations/322/observationName의 실제 equals 기대값은 'coverage-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-322" assertion으로 "link-oracle-322: /data/hostObservation/extractor/rawRows/namedObservations/322/oracleId의 실제 equals 기대값은 'T25.independent-traceability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-323" assertion으로 "concrete-links-323: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-323" assertion으로 "link-name-323: /data/hostObservation/extractor/rawRows/namedObservations/323/observationName의 실제 equals 기대값은 'result-separation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-323" assertion으로 "link-oracle-323: /data/hostObservation/extractor/rawRows/namedObservations/323/oracleId의 실제 equals 기대값은 'T25.independent-traceability'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-324" assertion으로 "concrete-links-324: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-324" assertion으로 "link-name-324: /data/hostObservation/extractor/rawRows/namedObservations/324/observationName의 실제 equals 기대값은 'evidence-fields'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-324" assertion으로 "link-oracle-324: /data/hostObservation/extractor/rawRows/namedObservations/324/oracleId의 실제 equals 기대값은 'T25.evidence-manifest-and-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-325" assertion으로 "concrete-links-325: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-325" assertion으로 "link-name-325: /data/hostObservation/extractor/rawRows/namedObservations/325/observationName의 실제 equals 기대값은 'wrapper-truth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-325" assertion으로 "link-oracle-325: /data/hostObservation/extractor/rawRows/namedObservations/325/oracleId의 실제 equals 기대값은 'T25.evidence-manifest-and-entrypoints'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-326" assertion으로 "concrete-links-326: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-326" assertion으로 "link-name-326: /data/hostObservation/extractor/rawRows/namedObservations/326/observationName의 실제 equals 기대값은 'corpus-size'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-326" assertion으로 "link-oracle-326: /data/hostObservation/extractor/rawRows/namedObservations/326/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-327" assertion으로 "concrete-links-327: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-327" assertion으로 "link-name-327: /data/hostObservation/extractor/rawRows/namedObservations/327/observationName의 실제 equals 기대값은 'proposed-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-327" assertion으로 "link-oracle-327: /data/hostObservation/extractor/rawRows/namedObservations/327/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-328" assertion으로 "concrete-links-328: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-328" assertion으로 "link-name-328: /data/hostObservation/extractor/rawRows/namedObservations/328/observationName의 실제 equals 기대값은 'usage-and-version-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-328" assertion으로 "link-oracle-328: /data/hostObservation/extractor/rawRows/namedObservations/328/oracleId의 실제 equals 기대값은 'T25.model-corpus-and-budget'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-329" assertion으로 "concrete-links-329: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-329" assertion으로 "link-name-329: /data/hostObservation/extractor/rawRows/namedObservations/329/observationName의 실제 equals 기대값은 'recovery-rediscovery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-329" assertion으로 "link-oracle-329: /data/hostObservation/extractor/rawRows/namedObservations/329/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-330" assertion으로 "concrete-links-330: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-330" assertion으로 "link-name-330: /data/hostObservation/extractor/rawRows/namedObservations/330/observationName의 실제 equals 기대값은 'operational-results'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-330" assertion으로 "link-oracle-330: /data/hostObservation/extractor/rawRows/namedObservations/330/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-331" assertion으로 "concrete-links-331: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-331" assertion으로 "link-name-331: /data/hostObservation/extractor/rawRows/namedObservations/331/observationName의 실제 equals 기대값은 'notification-resolves-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-331" assertion으로 "link-oracle-331: /data/hostObservation/extractor/rawRows/namedObservations/331/oracleId의 실제 equals 기대값은 'T26.scheduler-recovery-and-operations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-332" assertion으로 "concrete-links-332: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-332" assertion으로 "link-name-332: /data/hostObservation/extractor/rawRows/namedObservations/332/observationName의 실제 equals 기대값은 'boundary-index'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-332" assertion으로 "link-oracle-332: /data/hostObservation/extractor/rawRows/namedObservations/332/oracleId의 실제 equals 기대값은 'T26.boundary-index-and-emergency-repair'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-333" assertion으로 "concrete-links-333: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-333" assertion으로 "link-name-333: /data/hostObservation/extractor/rawRows/namedObservations/333/observationName의 실제 equals 기대값은 'safe-runbook'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-333" assertion으로 "link-oracle-333: /data/hostObservation/extractor/rawRows/namedObservations/333/oracleId의 실제 equals 기대값은 'T26.boundary-index-and-emergency-repair'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-334" assertion으로 "concrete-links-334: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-334" assertion으로 "link-name-334: /data/hostObservation/extractor/rawRows/namedObservations/334/observationName의 실제 equals 기대값은 'restore-verification'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-334" assertion으로 "link-oracle-334: /data/hostObservation/extractor/rawRows/namedObservations/334/oracleId의 실제 equals 기대값은 'T26.backup-complete-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-335" assertion으로 "concrete-links-335: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-335" assertion으로 "link-name-335: /data/hostObservation/extractor/rawRows/namedObservations/335/observationName의 실제 equals 기대값은 'held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-335" assertion으로 "link-oracle-335: /data/hostObservation/extractor/rawRows/namedObservations/335/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-336" assertion으로 "concrete-links-336: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-336" assertion으로 "link-name-336: /data/hostObservation/extractor/rawRows/namedObservations/336/observationName의 실제 equals 기대값은 'sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-336" assertion으로 "link-oracle-336: /data/hostObservation/extractor/rawRows/namedObservations/336/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-337" assertion으로 "concrete-links-337: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-337" assertion으로 "link-name-337: /data/hostObservation/extractor/rawRows/namedObservations/337/observationName의 실제 equals 기대값은 'unreserved-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-337" assertion으로 "link-oracle-337: /data/hostObservation/extractor/rawRows/namedObservations/337/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-338" assertion으로 "concrete-links-338: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-338" assertion으로 "link-name-338: /data/hostObservation/extractor/rawRows/namedObservations/338/observationName의 실제 equals 기대값은 'customer60-added-by-qc-or-app-write'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-338" assertion으로 "link-oracle-338: /data/hostObservation/extractor/rawRows/namedObservations/338/oracleId의 실제 equals 기대값은 'C1.initial-eligibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-339" assertion으로 "concrete-links-339: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-339" assertion으로 "link-name-339: /data/hostObservation/extractor/rawRows/namedObservations/339/observationName의 실제 equals 기대값은 'new-reservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-339" assertion으로 "link-oracle-339: /data/hostObservation/extractor/rawRows/namedObservations/339/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-340" assertion으로 "concrete-links-340: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-340" assertion으로 "link-name-340: /data/hostObservation/extractor/rawRows/namedObservations/340/observationName의 실제 equals 기대값은 'new-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-340" assertion으로 "link-oracle-340: /data/hostObservation/extractor/rawRows/namedObservations/340/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-341" assertion으로 "concrete-links-341: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-341" assertion으로 "link-name-341: /data/hostObservation/extractor/rawRows/namedObservations/341/observationName의 실제 equals 기대값은 'existing-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-341" assertion으로 "link-oracle-341: /data/hostObservation/extractor/rawRows/namedObservations/341/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-342" assertion으로 "concrete-links-342: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-342" assertion으로 "link-name-342: /data/hostObservation/extractor/rawRows/namedObservations/342/observationName의 실제 equals 기대값은 'existing-obligation-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-342" assertion으로 "link-oracle-342: /data/hostObservation/extractor/rawRows/namedObservations/342/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-343" assertion으로 "concrete-links-343: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-343" assertion으로 "link-name-343: /data/hostObservation/extractor/rawRows/namedObservations/343/observationName의 실제 equals 기대값은 'revocation-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-343" assertion으로 "link-oracle-343: /data/hostObservation/extractor/rawRows/namedObservations/343/oracleId의 실제 equals 기대값은 'C1.revoked-disposition'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-344" assertion으로 "concrete-links-344: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-344" assertion으로 "link-name-344: /data/hostObservation/extractor/rawRows/namedObservations/344/observationName의 실제 equals 기대값은 'cumulative-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-344" assertion으로 "link-oracle-344: /data/hostObservation/extractor/rawRows/namedObservations/344/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-345" assertion으로 "concrete-links-345: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-345" assertion으로 "link-name-345: /data/hostObservation/extractor/rawRows/namedObservations/345/observationName의 실제 equals 기대값은 'state-at-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-345" assertion으로 "link-oracle-345: /data/hostObservation/extractor/rawRows/namedObservations/345/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-346" assertion으로 "concrete-links-346: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-346" assertion으로 "link-name-346: /data/hostObservation/extractor/rawRows/namedObservations/346/observationName의 실제 equals 기대값은 'cumulative'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-346" assertion으로 "link-oracle-346: /data/hostObservation/extractor/rawRows/namedObservations/346/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-347" assertion으로 "concrete-links-347: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-347" assertion으로 "link-name-347: /data/hostObservation/extractor/rawRows/namedObservations/347/observationName의 실제 equals 기대값은 'state-at'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-347" assertion으로 "link-oracle-347: /data/hostObservation/extractor/rawRows/namedObservations/347/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-348" assertion으로 "concrete-links-348: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-348" assertion으로 "link-name-348: /data/hostObservation/extractor/rawRows/namedObservations/348/observationName의 실제 equals 기대값은 'mode-evidence-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-348" assertion으로 "link-oracle-348: /data/hostObservation/extractor/rawRows/namedObservations/348/oracleId의 실제 equals 기대값은 'C2.cumulative-vs-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-349" assertion으로 "concrete-links-349: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-349" assertion으로 "link-name-349: /data/hostObservation/extractor/rawRows/namedObservations/349/observationName의 실제 equals 기대값은 'work-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-349" assertion으로 "link-oracle-349: /data/hostObservation/extractor/rawRows/namedObservations/349/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-350" assertion으로 "concrete-links-350: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-350" assertion으로 "link-name-350: /data/hostObservation/extractor/rawRows/namedObservations/350/observationName의 실제 equals 기대값은 'inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-350" assertion으로 "link-oracle-350: /data/hostObservation/extractor/rawRows/namedObservations/350/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-351" assertion으로 "concrete-links-351: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-351" assertion으로 "link-name-351: /data/hostObservation/extractor/rawRows/namedObservations/351/observationName의 실제 equals 기대값은 'followup-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-351" assertion으로 "link-oracle-351: /data/hostObservation/extractor/rawRows/namedObservations/351/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-352" assertion으로 "concrete-links-352: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-352" assertion으로 "link-name-352: /data/hostObservation/extractor/rawRows/namedObservations/352/observationName의 실제 equals 기대값은 'allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-352" assertion으로 "link-oracle-352: /data/hostObservation/extractor/rawRows/namedObservations/352/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-353" assertion으로 "concrete-links-353: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-353" assertion으로 "link-name-353: /data/hostObservation/extractor/rawRows/namedObservations/353/observationName의 실제 equals 기대값은 'approval-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-353" assertion으로 "link-oracle-353: /data/hostObservation/extractor/rawRows/namedObservations/353/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-354" assertion으로 "concrete-links-354: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-354" assertion으로 "link-name-354: /data/hostObservation/extractor/rawRows/namedObservations/354/observationName의 실제 equals 기대값은 'external-outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-354" assertion으로 "link-oracle-354: /data/hostObservation/extractor/rawRows/namedObservations/354/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-355" assertion으로 "concrete-links-355: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-355" assertion으로 "link-name-355: /data/hostObservation/extractor/rawRows/namedObservations/355/observationName의 실제 equals 기대값은 'read-audit-permitted'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-355" assertion으로 "link-oracle-355: /data/hostObservation/extractor/rawRows/namedObservations/355/oracleId의 실제 equals 기대값은 'C3.read-grant-all-writes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-356" assertion으로 "concrete-links-356: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-356" assertion으로 "link-name-356: /data/hostObservation/extractor/rawRows/namedObservations/356/observationName의 실제 equals 기대값은 'query-intent-write-tool-execution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-356" assertion으로 "link-oracle-356: /data/hostObservation/extractor/rawRows/namedObservations/356/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-357" assertion으로 "concrete-links-357: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-357" assertion으로 "link-name-357: /data/hostObservation/extractor/rawRows/namedObservations/357/observationName의 실제 equals 기대값은 'query-intent-business-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-357" assertion으로 "link-oracle-357: /data/hostObservation/extractor/rawRows/namedObservations/357/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-358" assertion으로 "concrete-links-358: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-358" assertion으로 "link-name-358: /data/hostObservation/extractor/rawRows/namedObservations/358/observationName의 실제 equals 기대값은 'evaluation-path'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-358" assertion으로 "link-oracle-358: /data/hostObservation/extractor/rawRows/namedObservations/358/oracleId의 실제 equals 기대값은 'C3.model-query-write-boundary'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-359" assertion으로 "concrete-links-359: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-359" assertion으로 "link-name-359: /data/hostObservation/extractor/rawRows/namedObservations/359/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-359" assertion으로 "link-oracle-359: /data/hostObservation/extractor/rawRows/namedObservations/359/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-360" assertion으로 "concrete-links-360: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-360" assertion으로 "link-name-360: /data/hostObservation/extractor/rawRows/namedObservations/360/observationName의 실제 equals 기대값은 'new-return'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-360" assertion으로 "link-oracle-360: /data/hostObservation/extractor/rawRows/namedObservations/360/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-361" assertion으로 "concrete-links-361: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-361" assertion으로 "link-name-361: /data/hostObservation/extractor/rawRows/namedObservations/361/observationName의 실제 equals 기대값은 'duplicate-return-material-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-361" assertion으로 "link-oracle-361: /data/hostObservation/extractor/rawRows/namedObservations/361/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-362" assertion으로 "concrete-links-362: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-362" assertion으로 "link-name-362: /data/hostObservation/extractor/rawRows/namedObservations/362/observationName의 실제 equals 기대값은 'return-overwrites-delivery-to80'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-362" assertion으로 "link-oracle-362: /data/hostObservation/extractor/rawRows/namedObservations/362/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-363" assertion으로 "concrete-links-363: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-363" assertion으로 "link-name-363: /data/hostObservation/extractor/rawRows/namedObservations/363/observationName의 실제 equals 기대값은 'physical-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-363" assertion으로 "link-oracle-363: /data/hostObservation/extractor/rawRows/namedObservations/363/oracleId의 실제 equals 기대값은 'C4.return-not-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-364" assertion으로 "concrete-links-364: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-364" assertion으로 "link-name-364: /data/hostObservation/extractor/rawRows/namedObservations/364/observationName의 실제 equals 기대값은 'currently-supported-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-364" assertion으로 "link-oracle-364: /data/hostObservation/extractor/rawRows/namedObservations/364/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-365" assertion으로 "concrete-links-365: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-365" assertion으로 "link-name-365: /data/hostObservation/extractor/rawRows/namedObservations/365/observationName의 실제 equals 기대값은 'current-unresolved-deficit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-365" assertion으로 "link-oracle-365: /data/hostObservation/extractor/rawRows/namedObservations/365/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-366" assertion으로 "concrete-links-366: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-366" assertion으로 "link-name-366: /data/hostObservation/extractor/rawRows/namedObservations/366/observationName의 실제 equals 기대값은 'historical-assessment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-366" assertion으로 "link-oracle-366: /data/hostObservation/extractor/rawRows/namedObservations/366/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-367" assertion으로 "concrete-links-367: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-367" assertion으로 "link-name-367: /data/hostObservation/extractor/rawRows/namedObservations/367/observationName의 실제 equals 기대값은 'deficit-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-367" assertion으로 "link-oracle-367: /data/hostObservation/extractor/rawRows/namedObservations/367/oracleId의 실제 equals 기대값은 'C4.corrected-delivery-98'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-368" assertion으로 "concrete-links-368: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-368" assertion으로 "link-name-368: /data/hostObservation/extractor/rawRows/namedObservations/368/observationName의 실제 equals 기대값은 'new-unresolved-deficit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-368" assertion으로 "link-oracle-368: /data/hostObservation/extractor/rawRows/namedObservations/368/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-369" assertion으로 "concrete-links-369: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-369" assertion으로 "link-name-369: /data/hostObservation/extractor/rawRows/namedObservations/369/observationName의 실제 equals 기대값은 'resolved-debt-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-369" assertion으로 "link-oracle-369: /data/hostObservation/extractor/rawRows/namedObservations/369/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-370" assertion으로 "concrete-links-370: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-370" assertion으로 "link-name-370: /data/hostObservation/extractor/rawRows/namedObservations/370/observationName의 실제 equals 기대값은 'valid-resolution'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-370" assertion으로 "link-oracle-370: /data/hostObservation/extractor/rawRows/namedObservations/370/oracleId의 실제 equals 기대값은 'C4.resolved-debt-no-resurrection'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-371" assertion으로 "concrete-links-371: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-371" assertion으로 "link-name-371: /data/hostObservation/extractor/rawRows/namedObservations/371/observationName의 실제 equals 기대값은 'intake-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-371" assertion으로 "link-oracle-371: /data/hostObservation/extractor/rawRows/namedObservations/371/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-372" assertion으로 "concrete-links-372: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-372" assertion으로 "link-name-372: /data/hostObservation/extractor/rawRows/namedObservations/372/observationName의 실제 equals 기대값은 'intake-tracking'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-372" assertion으로 "link-oracle-372: /data/hostObservation/extractor/rawRows/namedObservations/372/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-373" assertion으로 "concrete-links-373: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-373" assertion으로 "link-name-373: /data/hostObservation/extractor/rawRows/namedObservations/373/observationName의 실제 equals 기대값은 'alert-delivery-closes-intake'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-373" assertion으로 "link-oracle-373: /data/hostObservation/extractor/rawRows/namedObservations/373/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-374" assertion으로 "concrete-links-374: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-374" assertion으로 "link-name-374: /data/hostObservation/extractor/rawRows/namedObservations/374/observationName의 실제 equals 기대값은 'closed-parent-rewrite'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-374" assertion으로 "link-oracle-374: /data/hostObservation/extractor/rawRows/namedObservations/374/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-375" assertion으로 "concrete-links-375: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-375" assertion으로 "link-name-375: /data/hostObservation/extractor/rawRows/namedObservations/375/observationName의 실제 equals 기대값은 'intake-activation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-375" assertion으로 "link-oracle-375: /data/hostObservation/extractor/rawRows/namedObservations/375/oracleId의 실제 equals 기대값은 'C5.failed-link-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-376" assertion으로 "concrete-links-376: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-376" assertion으로 "link-name-376: /data/hostObservation/extractor/rawRows/namedObservations/376/observationName의 실제 equals 기대값은 'anomaly-work-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-376" assertion으로 "link-oracle-376: /data/hostObservation/extractor/rawRows/namedObservations/376/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-377" assertion으로 "concrete-links-377: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-377" assertion으로 "link-name-377: /data/hostObservation/extractor/rawRows/namedObservations/377/observationName의 실제 equals 기대값은 'anomaly-obligation-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-377" assertion으로 "link-oracle-377: /data/hostObservation/extractor/rawRows/namedObservations/377/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-378" assertion으로 "concrete-links-378: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-378" assertion으로 "link-name-378: /data/hostObservation/extractor/rawRows/namedObservations/378/observationName의 실제 equals 기대값은 'connected-work-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-378" assertion으로 "link-oracle-378: /data/hostObservation/extractor/rawRows/namedObservations/378/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-379" assertion으로 "concrete-links-379: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-379" assertion으로 "link-name-379: /data/hostObservation/extractor/rawRows/namedObservations/379/observationName의 실제 equals 기대값은 'normal-observation-work'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-379" assertion으로 "link-oracle-379: /data/hostObservation/extractor/rawRows/namedObservations/379/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-380" assertion으로 "concrete-links-380: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-380" assertion으로 "link-name-380: /data/hostObservation/extractor/rawRows/namedObservations/380/observationName의 실제 equals 기대값은 'normal-observation-run'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-380" assertion으로 "link-oracle-380: /data/hostObservation/extractor/rawRows/namedObservations/380/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-381" assertion으로 "concrete-links-381: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-381" assertion으로 "link-name-381: /data/hostObservation/extractor/rawRows/namedObservations/381/observationName의 실제 equals 기대값은 'intake-close-after-link'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-381" assertion으로 "link-oracle-381: /data/hostObservation/extractor/rawRows/namedObservations/381/oracleId의 실제 equals 기대값은 'C5.retry-once'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-382" assertion으로 "concrete-links-382: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-382" assertion으로 "link-name-382: /data/hostObservation/extractor/rawRows/namedObservations/382/observationName의 실제 equals 기대값은 'v1-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-382" assertion으로 "link-oracle-382: /data/hostObservation/extractor/rawRows/namedObservations/382/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-383" assertion으로 "concrete-links-383: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-383" assertion으로 "link-name-383: /data/hostObservation/extractor/rawRows/namedObservations/383/observationName의 실제 equals 기대값은 'new-v2-endpoint'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-383" assertion으로 "link-oracle-383: /data/hostObservation/extractor/rawRows/namedObservations/383/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-384" assertion으로 "concrete-links-384: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-384" assertion으로 "link-name-384: /data/hostObservation/extractor/rawRows/namedObservations/384/observationName의 실제 equals 기대값은 'current-sell-permission'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-384" assertion으로 "link-oracle-384: /data/hostObservation/extractor/rawRows/namedObservations/384/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-385" assertion으로 "concrete-links-385: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-385" assertion으로 "link-name-385: /data/hostObservation/extractor/rawRows/namedObservations/385/observationName의 실제 equals 기대값은 'old-assessment-semantics'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-385" assertion으로 "link-oracle-385: /data/hostObservation/extractor/rawRows/namedObservations/385/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-386" assertion으로 "concrete-links-386: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-386" assertion으로 "link-name-386: /data/hostObservation/extractor/rawRows/namedObservations/386/observationName의 실제 equals 기대값은 'skill-hash-substitutes-runtime-proof'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-386" assertion으로 "link-oracle-386: /data/hostObservation/extractor/rawRows/namedObservations/386/oracleId의 실제 equals 기대값은 'V1.pinned-meaning-current-policy'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-387" assertion으로 "concrete-links-387: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-387" assertion으로 "link-name-387: /data/hostObservation/extractor/rawRows/namedObservations/387/observationName의 실제 equals 기대값은 'unsupported-command-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-387" assertion으로 "link-oracle-387: /data/hostObservation/extractor/rawRows/namedObservations/387/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-388" assertion으로 "concrete-links-388: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-388" assertion으로 "link-name-388: /data/hostObservation/extractor/rawRows/namedObservations/388/observationName의 실제 equals 기대값은 'unsupported'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-388" assertion으로 "link-oracle-388: /data/hostObservation/extractor/rawRows/namedObservations/388/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-389" assertion으로 "concrete-links-389: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-389" assertion으로 "link-name-389: /data/hostObservation/extractor/rawRows/namedObservations/389/observationName의 실제 equals 기대값은 'unsupported-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-389" assertion으로 "link-oracle-389: /data/hostObservation/extractor/rawRows/namedObservations/389/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-390" assertion으로 "concrete-links-390: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-390" assertion으로 "link-name-390: /data/hostObservation/extractor/rawRows/namedObservations/390/observationName의 실제 equals 기대값은 'no-silent-fallback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-390" assertion으로 "link-oracle-390: /data/hostObservation/extractor/rawRows/namedObservations/390/oracleId의 실제 equals 기대값은 'V1.unsupported-v1-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-391" assertion으로 "concrete-links-391: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-391" assertion으로 "link-name-391: /data/hostObservation/extractor/rawRows/namedObservations/391/observationName의 실제 equals 기대값은 'active-physical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-391" assertion으로 "link-oracle-391: /data/hostObservation/extractor/rawRows/namedObservations/391/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-392" assertion으로 "concrete-links-392: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-392" assertion으로 "link-name-392: /data/hostObservation/extractor/rawRows/namedObservations/392/observationName의 실제 equals 기대값은 'existing-obligation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-392" assertion으로 "link-oracle-392: /data/hostObservation/extractor/rawRows/namedObservations/392/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-393" assertion으로 "concrete-links-393: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-393" assertion으로 "link-name-393: /data/hostObservation/extractor/rawRows/namedObservations/393/observationName의 실제 equals 기대값은 'new-executable-reservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-393" assertion으로 "link-oracle-393: /data/hostObservation/extractor/rawRows/namedObservations/393/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-394" assertion으로 "concrete-links-394: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-394" assertion으로 "link-name-394: /data/hostObservation/extractor/rawRows/namedObservations/394/observationName의 실제 equals 기대값은 'retired-parent-reconsumption'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-394" assertion으로 "link-oracle-394: /data/hostObservation/extractor/rawRows/namedObservations/394/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-395" assertion으로 "concrete-links-395: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-395" assertion으로 "link-name-395: /data/hostObservation/extractor/rawRows/namedObservations/395/observationName의 실제 equals 기대값은 'all-executable-reservations'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-395" assertion으로 "link-oracle-395: /data/hostObservation/extractor/rawRows/namedObservations/395/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-396" assertion으로 "concrete-links-396: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-396" assertion으로 "link-name-396: /data/hostObservation/extractor/rawRows/namedObservations/396/observationName의 실제 equals 기대값은 'allocation-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-396" assertion으로 "link-oracle-396: /data/hostObservation/extractor/rawRows/namedObservations/396/oracleId의 실제 equals 기대값은 'V2.split-reserve-race'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-397" assertion으로 "concrete-links-397: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-397" assertion으로 "link-name-397: /data/hostObservation/extractor/rawRows/namedObservations/397/observationName의 실제 equals 기대값은 'active-physical'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-397" assertion으로 "link-oracle-397: /data/hostObservation/extractor/rawRows/namedObservations/397/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-398" assertion으로 "concrete-links-398: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-398" assertion으로 "link-name-398: /data/hostObservation/extractor/rawRows/namedObservations/398/observationName의 실제 equals 기대값은 'executable-allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-398" assertion으로 "link-oracle-398: /data/hostObservation/extractor/rawRows/namedObservations/398/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-399" assertion으로 "concrete-links-399: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-399" assertion으로 "link-name-399: /data/hostObservation/extractor/rawRows/namedObservations/399/observationName의 실제 equals 기대값은 'promised-obligation-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-399" assertion으로 "link-oracle-399: /data/hostObservation/extractor/rawRows/namedObservations/399/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-400" assertion으로 "concrete-links-400: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-400" assertion으로 "link-name-400: /data/hostObservation/extractor/rawRows/namedObservations/400/observationName의 실제 equals 기대값은 'minimum-shortage-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-400" assertion으로 "link-oracle-400: /data/hostObservation/extractor/rawRows/namedObservations/400/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-401" assertion으로 "concrete-links-401: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-401" assertion으로 "link-name-401: /data/hostObservation/extractor/rawRows/namedObservations/401/observationName의 실제 equals 기대값은 'shortage-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-401" assertion으로 "link-oracle-401: /data/hostObservation/extractor/rawRows/namedObservations/401/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-402" assertion으로 "concrete-links-402: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-402" assertion으로 "link-name-402: /data/hostObservation/extractor/rawRows/namedObservations/402/observationName의 실제 equals 기대값은 'past-reservation-deletion-to-hide-shortage'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-402" assertion으로 "link-oracle-402: /data/hostObservation/extractor/rawRows/namedObservations/402/oracleId의 실제 equals 기대값은 'V2.actual50-correction'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-403" assertion으로 "concrete-links-403: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-403" assertion으로 "link-name-403: /data/hostObservation/extractor/rawRows/namedObservations/403/observationName의 실제 equals 기대값은 'new-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-403" assertion으로 "link-oracle-403: /data/hostObservation/extractor/rawRows/namedObservations/403/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-404" assertion으로 "concrete-links-404: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-404" assertion으로 "link-name-404: /data/hostObservation/extractor/rawRows/namedObservations/404/observationName의 실제 equals 기대값은 'allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-404" assertion으로 "link-oracle-404: /data/hostObservation/extractor/rawRows/namedObservations/404/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-405" assertion으로 "concrete-links-405: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-405" assertion으로 "link-name-405: /data/hostObservation/extractor/rawRows/namedObservations/405/observationName의 실제 equals 기대값은 'blocked-dispatch-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-405" assertion으로 "link-oracle-405: /data/hostObservation/extractor/rawRows/namedObservations/405/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-406" assertion으로 "concrete-links-406: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-406" assertion으로 "link-name-406: /data/hostObservation/extractor/rawRows/namedObservations/406/observationName의 실제 equals 기대값은 'scope-lock'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-406" assertion으로 "link-oracle-406: /data/hostObservation/extractor/rawRows/namedObservations/406/oracleId의 실제 equals 기대값은 'V3.hold-before-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-407" assertion으로 "concrete-links-407: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-407" assertion으로 "link-name-407: /data/hostObservation/extractor/rawRows/namedObservations/407/observationName의 실제 equals 기대값은 'committed-dispatch-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-407" assertion으로 "link-oracle-407: /data/hostObservation/extractor/rawRows/namedObservations/407/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-408" assertion으로 "concrete-links-408: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-408" assertion으로 "link-name-408: /data/hostObservation/extractor/rawRows/namedObservations/408/observationName의 실제 equals 기대값은 'allocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-408" assertion으로 "link-oracle-408: /data/hostObservation/extractor/rawRows/namedObservations/408/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-409" assertion으로 "concrete-links-409: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-409" assertion으로 "link-name-409: /data/hostObservation/extractor/rawRows/namedObservations/409/observationName의 실제 equals 기대값은 'post-dispatch-hold-response'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-409" assertion으로 "link-oracle-409: /data/hostObservation/extractor/rawRows/namedObservations/409/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-410" assertion으로 "concrete-links-410: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-410" assertion으로 "link-name-410: /data/hostObservation/extractor/rawRows/namedObservations/410/observationName의 실제 equals 기대값은 'history-deletion-or-fake-rollback'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-410" assertion으로 "link-oracle-410: /data/hostObservation/extractor/rawRows/namedObservations/410/oracleId의 실제 equals 기대값은 'V3.dispatch-before-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-411" assertion으로 "concrete-links-411: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-411" assertion으로 "link-name-411: /data/hostObservation/extractor/rawRows/namedObservations/411/observationName의 실제 equals 기대값은 'new-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-411" assertion으로 "link-oracle-411: /data/hostObservation/extractor/rawRows/namedObservations/411/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-412" assertion으로 "concrete-links-412: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-412" assertion으로 "link-name-412: /data/hostObservation/extractor/rawRows/namedObservations/412/observationName의 실제 equals 기대값은 'newly-dispatched'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-412" assertion으로 "link-oracle-412: /data/hostObservation/extractor/rawRows/namedObservations/412/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-413" assertion으로 "concrete-links-413: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-413" assertion으로 "link-name-413: /data/hostObservation/extractor/rawRows/namedObservations/413/observationName의 실제 equals 기대값은 'late-release-overwrites-new-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-413" assertion으로 "link-oracle-413: /data/hostObservation/extractor/rawRows/namedObservations/413/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-414" assertion으로 "concrete-links-414: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-414" assertion으로 "link-name-414: /data/hostObservation/extractor/rawRows/namedObservations/414/observationName의 실제 equals 기대값은 'source-order-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-414" assertion으로 "link-oracle-414: /data/hostObservation/extractor/rawRows/namedObservations/414/oracleId의 실제 equals 기대값은 'V3.late-old-qc-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-415" assertion으로 "concrete-links-415: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-415" assertion으로 "link-name-415: /data/hostObservation/extractor/rawRows/namedObservations/415/observationName의 실제 equals 기대값은 'inventory-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-415" assertion으로 "link-oracle-415: /data/hostObservation/extractor/rawRows/namedObservations/415/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-416" assertion으로 "concrete-links-416: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-416" assertion으로 "link-name-416: /data/hostObservation/extractor/rawRows/namedObservations/416/observationName의 실제 equals 기대값은 'allocation-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-416" assertion으로 "link-oracle-416: /data/hostObservation/extractor/rawRows/namedObservations/416/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-417" assertion으로 "concrete-links-417: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-417" assertion으로 "link-name-417: /data/hostObservation/extractor/rawRows/namedObservations/417/observationName의 실제 equals 기대값은 'approval-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-417" assertion으로 "link-oracle-417: /data/hostObservation/extractor/rawRows/namedObservations/417/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-418" assertion으로 "concrete-links-418: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-418" assertion으로 "link-name-418: /data/hostObservation/extractor/rawRows/namedObservations/418/observationName의 실제 equals 기대값은 'outbox-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-418" assertion으로 "link-oracle-418: /data/hostObservation/extractor/rawRows/namedObservations/418/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-419" assertion으로 "concrete-links-419: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-419" assertion으로 "link-name-419: /data/hostObservation/extractor/rawRows/namedObservations/419/observationName의 실제 equals 기대값은 'mixed-batch-allowed-partial-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-419" assertion으로 "link-oracle-419: /data/hostObservation/extractor/rawRows/namedObservations/419/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-420" assertion으로 "concrete-links-420: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-420" assertion으로 "link-name-420: /data/hostObservation/extractor/rawRows/namedObservations/420/observationName의 실제 equals 기대값은 'same-auth-path'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-420" assertion으로 "link-oracle-420: /data/hostObservation/extractor/rawRows/namedObservations/420/oracleId의 실제 equals 기대값은 'V4.all-alternate-write-paths'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-421" assertion으로 "concrete-links-421: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-421" assertion으로 "link-name-421: /data/hostObservation/extractor/rawRows/namedObservations/421/observationName의 실제 equals 기대값은 'internal-move-created-customer-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-421" assertion으로 "link-oracle-421: /data/hostObservation/extractor/rawRows/namedObservations/421/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-422" assertion으로 "concrete-links-422: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-422" assertion으로 "link-name-422: /data/hostObservation/extractor/rawRows/namedObservations/422/observationName의 실제 equals 기대값은 'internal-move-created-order-fulfilment'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-422" assertion으로 "link-oracle-422: /data/hostObservation/extractor/rawRows/namedObservations/422/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-423" assertion으로 "concrete-links-423: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-423" assertion으로 "link-name-423: /data/hostObservation/extractor/rawRows/namedObservations/423/observationName의 실제 equals 기대값은 'internal-move-created-return-or-disposal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-423" assertion으로 "link-oracle-423: /data/hostObservation/extractor/rawRows/namedObservations/423/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-424" assertion으로 "concrete-links-424: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-424" assertion으로 "link-name-424: /data/hostObservation/extractor/rawRows/namedObservations/424/observationName의 실제 equals 기대값은 'observed-unauthorized-fact'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-424" assertion으로 "link-oracle-424: /data/hostObservation/extractor/rawRows/namedObservations/424/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-425" assertion으로 "concrete-links-425: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-425" assertion으로 "link-name-425: /data/hostObservation/extractor/rawRows/namedObservations/425/observationName의 실제 equals 기대값은 'unauthorized-move-reconciliation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-425" assertion으로 "link-oracle-425: /data/hostObservation/extractor/rawRows/namedObservations/425/oracleId의 실제 equals 기대값은 'V4.internal-move-cannot-deliver'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-426" assertion으로 "concrete-links-426: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-426" assertion으로 "link-name-426: /data/hostObservation/extractor/rawRows/namedObservations/426/observationName의 실제 equals 기대값은 'resumed-or-escalated-duty-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-426" assertion으로 "link-oracle-426: /data/hostObservation/extractor/rawRows/namedObservations/426/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-427" assertion으로 "concrete-links-427: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-427" assertion으로 "link-name-427: /data/hostObservation/extractor/rawRows/namedObservations/427/observationName의 실제 equals 기대값은 'within-test-profile'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-427" assertion으로 "link-oracle-427: /data/hostObservation/extractor/rawRows/namedObservations/427/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-428" assertion으로 "concrete-links-428: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-428" assertion으로 "link-name-428: /data/hostObservation/extractor/rawRows/namedObservations/428/observationName의 실제 equals 기대값은 'duty-after-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-428" assertion으로 "link-oracle-428: /data/hostObservation/extractor/rawRows/namedObservations/428/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-429" assertion으로 "concrete-links-429: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-429" assertion으로 "link-name-429: /data/hostObservation/extractor/rawRows/namedObservations/429/observationName의 실제 equals 기대값은 'queue-success-as-goal-satisfied'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-429" assertion으로 "link-oracle-429: /data/hostObservation/extractor/rawRows/namedObservations/429/oracleId의 실제 equals 기대값은 'V5.wait-after-full-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-430" assertion으로 "concrete-links-430: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-430" assertion으로 "link-name-430: /data/hostObservation/extractor/rawRows/namedObservations/430/observationName의 실제 equals 기대값은 'duplicate-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-430" assertion으로 "link-oracle-430: /data/hostObservation/extractor/rawRows/namedObservations/430/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-431" assertion으로 "concrete-links-431: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-431" assertion으로 "link-name-431: /data/hostObservation/extractor/rawRows/namedObservations/431/observationName의 실제 equals 기대값은 'stale-worker-commit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-431" assertion으로 "link-oracle-431: /data/hostObservation/extractor/rawRows/namedObservations/431/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-432" assertion으로 "concrete-links-432: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-432" assertion으로 "link-name-432: /data/hostObservation/extractor/rawRows/namedObservations/432/observationName의 실제 equals 기대값은 'fence-state'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-432" assertion으로 "link-oracle-432: /data/hostObservation/extractor/rawRows/namedObservations/432/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-433" assertion으로 "concrete-links-433: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-433" assertion으로 "link-name-433: /data/hostObservation/extractor/rawRows/namedObservations/433/observationName의 실제 equals 기대값은 'exhausted-retry-owner'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-433" assertion으로 "link-oracle-433: /data/hostObservation/extractor/rawRows/namedObservations/433/oracleId의 실제 equals 기대값은 'V5.two-workers-fencing'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-434" assertion으로 "concrete-links-434: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-434" assertion으로 "link-name-434: /data/hostObservation/extractor/rawRows/namedObservations/434/observationName의 실제 equals 기대값은 'physical-receipt-total'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-434" assertion으로 "link-oracle-434: /data/hostObservation/extractor/rawRows/namedObservations/434/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-435" assertion으로 "concrete-links-435: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-435" assertion으로 "link-name-435: /data/hostObservation/extractor/rawRows/namedObservations/435/observationName의 실제 equals 기대값은 'committed-effect-count'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-435" assertion으로 "link-oracle-435: /data/hostObservation/extractor/rawRows/namedObservations/435/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-436" assertion으로 "concrete-links-436: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-436" assertion으로 "link-name-436: /data/hostObservation/extractor/rawRows/namedObservations/436/observationName의 실제 equals 기대값은 'retry-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-436" assertion으로 "link-oracle-436: /data/hostObservation/extractor/rawRows/namedObservations/436/oracleId의 실제 equals 기대값은 'V6.receipt60-lost-response-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-437" assertion으로 "concrete-links-437: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-437" assertion으로 "link-name-437: /data/hostObservation/extractor/rawRows/namedObservations/437/observationName의 실제 equals 기대값은 'different-payload'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-437" assertion으로 "link-oracle-437: /data/hostObservation/extractor/rawRows/namedObservations/437/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-438" assertion으로 "concrete-links-438: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-438" assertion으로 "link-name-438: /data/hostObservation/extractor/rawRows/namedObservations/438/observationName의 실제 equals 기대값은 'conflict-new-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-438" assertion으로 "link-oracle-438: /data/hostObservation/extractor/rawRows/namedObservations/438/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-439" assertion으로 "concrete-links-439: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-439" assertion으로 "link-name-439: /data/hostObservation/extractor/rawRows/namedObservations/439/observationName의 실제 equals 기대값은 'other-owner-result-leak'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-439" assertion으로 "link-oracle-439: /data/hostObservation/extractor/rawRows/namedObservations/439/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-440" assertion으로 "concrete-links-440: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-440" assertion으로 "link-name-440: /data/hostObservation/extractor/rawRows/namedObservations/440/observationName의 실제 equals 기대값은 'namespaces'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-440" assertion으로 "link-oracle-440: /data/hostObservation/extractor/rawRows/namedObservations/440/oracleId의 실제 equals 기대값은 'V6.key-conflicts-owner-and-distinct-order'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-441" assertion으로 "concrete-links-441: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-441" assertion으로 "link-name-441: /data/hostObservation/extractor/rawRows/namedObservations/441/observationName의 실제 equals 기대값은 'draft-supplement-physical-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-441" assertion으로 "link-oracle-441: /data/hostObservation/extractor/rawRows/namedObservations/441/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-442" assertion으로 "concrete-links-442: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-442" assertion으로 "link-name-442: /data/hostObservation/extractor/rawRows/namedObservations/442/observationName의 실제 equals 기대값은 'draft-supplement-idempotency-conflict'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-442" assertion으로 "link-oracle-442: /data/hostObservation/extractor/rawRows/namedObservations/442/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-443" assertion으로 "concrete-links-443: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-443" assertion으로 "link-name-443: /data/hostObservation/extractor/rawRows/namedObservations/443/observationName의 실제 equals 기대값은 'completed-intent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-443" assertion으로 "link-oracle-443: /data/hostObservation/extractor/rawRows/namedObservations/443/oracleId의 실제 equals 기대값은 'V6.input-destination-supplement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-444" assertion으로 "concrete-links-444: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-444" assertion으로 "link-name-444: /data/hostObservation/extractor/rawRows/namedObservations/444/observationName의 실제 equals 기대값은 'new-effects-quantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-444" assertion으로 "link-oracle-444: /data/hostObservation/extractor/rawRows/namedObservations/444/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-445" assertion으로 "concrete-links-445: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-445" assertion으로 "link-name-445: /data/hostObservation/extractor/rawRows/namedObservations/445/observationName의 실제 equals 기대값은 'blocked-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-445" assertion으로 "link-oracle-445: /data/hostObservation/extractor/rawRows/namedObservations/445/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-446" assertion으로 "concrete-links-446: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-446" assertion으로 "link-name-446: /data/hostObservation/extractor/rawRows/namedObservations/446/observationName의 실제 equals 기대값은 'worker-auth'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-446" assertion으로 "link-oracle-446: /data/hostObservation/extractor/rawRows/namedObservations/446/oracleId의 실제 equals 기대값은 'V7.enqueue-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-447" assertion으로 "concrete-links-447: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-447" assertion으로 "link-name-447: /data/hostObservation/extractor/rawRows/namedObservations/447/observationName의 실제 equals 기대값은 'effect-if-revoke-commits-first'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-447" assertion으로 "link-oracle-447: /data/hostObservation/extractor/rawRows/namedObservations/447/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-448" assertion으로 "concrete-links-448: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-448" assertion으로 "link-name-448: /data/hostObservation/extractor/rawRows/namedObservations/448/observationName의 실제 equals 기대값은 'preserved-if-effect-commits-first'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-448" assertion으로 "link-oracle-448: /data/hostObservation/extractor/rawRows/namedObservations/448/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-449" assertion으로 "concrete-links-449: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-449" assertion으로 "link-name-449: /data/hostObservation/extractor/rawRows/namedObservations/449/observationName의 실제 equals 기대값은 'effect-after-serialized-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-449" assertion으로 "link-oracle-449: /data/hostObservation/extractor/rawRows/namedObservations/449/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-450" assertion으로 "concrete-links-450: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-450" assertion으로 "link-name-450: /data/hostObservation/extractor/rawRows/namedObservations/450/observationName의 실제 equals 기대값은 'linearization-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-450" assertion으로 "link-oracle-450: /data/hostObservation/extractor/rawRows/namedObservations/450/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-451" assertion으로 "concrete-links-451: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-451" assertion으로 "link-name-451: /data/hostObservation/extractor/rawRows/namedObservations/451/observationName의 실제 equals 기대값은 'blocked-or-post-revoke-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-451" assertion으로 "link-oracle-451: /data/hostObservation/extractor/rawRows/namedObservations/451/oracleId의 실제 equals 기대값은 'V7.authorization-then-revoke'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-452" assertion으로 "concrete-links-452: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-452" assertion으로 "link-name-452: /data/hostObservation/extractor/rawRows/namedObservations/452/observationName의 실제 equals 기대값은 'unauthorized-recovered-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-452" assertion으로 "link-oracle-452: /data/hostObservation/extractor/rawRows/namedObservations/452/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-453" assertion으로 "concrete-links-453: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-453" assertion으로 "link-name-453: /data/hostObservation/extractor/rawRows/namedObservations/453/observationName의 실제 equals 기대값은 'prior-committed-preserved'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-453" assertion으로 "link-oracle-453: /data/hostObservation/extractor/rawRows/namedObservations/453/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-454" assertion으로 "concrete-links-454: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-454" assertion으로 "link-name-454: /data/hostObservation/extractor/rawRows/namedObservations/454/observationName의 실제 equals 기대값은 'blocked-duty-after-restart'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-454" assertion으로 "link-oracle-454: /data/hostObservation/extractor/rawRows/namedObservations/454/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-455" assertion으로 "concrete-links-455: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-455" assertion으로 "link-name-455: /data/hostObservation/extractor/rawRows/namedObservations/455/observationName의 실제 equals 기대값은 'safe-retry'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-455" assertion으로 "link-oracle-455: /data/hostObservation/extractor/rawRows/namedObservations/455/oracleId의 실제 equals 기대값은 'V7.restart-after-revocation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-456" assertion으로 "concrete-links-456: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-456" assertion으로 "link-name-456: /data/hostObservation/extractor/rawRows/namedObservations/456/observationName의 실제 equals 기대값은 'fresh-integration'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-456" assertion으로 "link-oracle-456: /data/hostObservation/extractor/rawRows/namedObservations/456/oracleId의 실제 equals 기대값은 'V8.fresh-install'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-457" assertion으로 "concrete-links-457: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-457" assertion으로 "link-name-457: /data/hostObservation/extractor/rawRows/namedObservations/457/observationName의 실제 equals 기대값은 'upgrade-preserves'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-457" assertion으로 "link-oracle-457: /data/hostObservation/extractor/rawRows/namedObservations/457/oracleId의 실제 equals 기대값은 'V8.ontology-upgrade-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-458" assertion으로 "concrete-links-458: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-458" assertion으로 "link-name-458: /data/hostObservation/extractor/rawRows/namedObservations/458/observationName의 실제 equals 기대값은 'legacy-migration-substitutes-ontology-upgrade'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-458" assertion으로 "link-oracle-458: /data/hostObservation/extractor/rawRows/namedObservations/458/oracleId의 실제 equals 기대값은 'V8.ontology-upgrade-preservation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-459" assertion으로 "concrete-links-459: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-459" assertion으로 "link-name-459: /data/hostObservation/extractor/rawRows/namedObservations/459/observationName의 실제 equals 기대값은 'restore-artifacts'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-459" assertion으로 "link-oracle-459: /data/hostObservation/extractor/rawRows/namedObservations/459/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-460" assertion으로 "concrete-links-460: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-460" assertion으로 "link-name-460: /data/hostObservation/extractor/rawRows/namedObservations/460/observationName의 실제 equals 기대값은 'missing-blob-restore-complete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-460" assertion으로 "link-oracle-460: /data/hostObservation/extractor/rawRows/namedObservations/460/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-461" assertion으로 "concrete-links-461: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-461" assertion으로 "link-name-461: /data/hostObservation/extractor/rawRows/namedObservations/461/observationName의 실제 equals 기대값은 'missing-evaluator-restore-complete'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-461" assertion으로 "link-oracle-461: /data/hostObservation/extractor/rawRows/namedObservations/461/oracleId의 실제 equals 기대값은 'V8.db-blob-definition-restore'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-462" assertion으로 "concrete-links-462: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-462" assertion으로 "link-name-462: /data/hostObservation/extractor/rawRows/namedObservations/462/observationName의 실제 equals 기대값은 'btp-acceptance'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-462" assertion으로 "link-oracle-462: /data/hostObservation/extractor/rawRows/namedObservations/462/oracleId의 실제 equals 기대값은 'V8.btp-client-separate-gates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-463" assertion으로 "concrete-links-463: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-463" assertion으로 "link-name-463: /data/hostObservation/extractor/rawRows/namedObservations/463/observationName의 실제 equals 기대값은 'result-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-463" assertion으로 "link-oracle-463: /data/hostObservation/extractor/rawRows/namedObservations/463/oracleId의 실제 equals 기대값은 'V8.btp-client-separate-gates'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-464" assertion으로 "concrete-links-464: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-464" assertion으로 "link-name-464: /data/hostObservation/extractor/rawRows/namedObservations/464/observationName의 실제 equals 기대값은 'purchase-cumulative-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-464" assertion으로 "link-oracle-464: /data/hostObservation/extractor/rawRows/namedObservations/464/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-465" assertion으로 "concrete-links-465: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-465" assertion으로 "link-name-465: /data/hostObservation/extractor/rawRows/namedObservations/465/observationName의 실제 equals 기대값은 'warehouse-current-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-465" assertion으로 "link-oracle-465: /data/hostObservation/extractor/rawRows/namedObservations/465/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-466" assertion으로 "concrete-links-466: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-466" assertion으로 "link-name-466: /data/hostObservation/extractor/rawRows/namedObservations/466/observationName의 실제 equals 기대값은 'historical-delivery'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-466" assertion으로 "link-oracle-466: /data/hostObservation/extractor/rawRows/namedObservations/466/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-467" assertion으로 "concrete-links-467: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-467" assertion으로 "link-name-467: /data/hostObservation/extractor/rawRows/namedObservations/467/observationName의 실제 equals 기대값은 'return-received'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-467" assertion으로 "link-oracle-467: /data/hostObservation/extractor/rawRows/namedObservations/467/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-468" assertion으로 "concrete-links-468: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-468" assertion으로 "link-name-468: /data/hostObservation/extractor/rawRows/namedObservations/468/observationName의 실제 equals 기대값은 'current-sell-eligible'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-468" assertion으로 "link-oracle-468: /data/hostObservation/extractor/rawRows/namedObservations/468/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-469" assertion으로 "concrete-links-469: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-469" assertion으로 "link-name-469: /data/hostObservation/extractor/rawRows/namedObservations/469/observationName의 실제 equals 기대값은 'agency-unverified-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-469" assertion으로 "link-oracle-469: /data/hostObservation/extractor/rawRows/namedObservations/469/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-470" assertion으로 "concrete-links-470: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-470" assertion으로 "link-name-470: /data/hostObservation/extractor/rawRows/namedObservations/470/observationName의 실제 equals 기대값은 'QC-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-470" assertion으로 "link-oracle-470: /data/hostObservation/extractor/rawRows/namedObservations/470/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-471" assertion으로 "concrete-links-471: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-471" assertion으로 "link-name-471: /data/hostObservation/extractor/rawRows/namedObservations/471/observationName의 실제 equals 기대값은 'return-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-471" assertion으로 "link-oracle-471: /data/hostObservation/extractor/rawRows/namedObservations/471/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-472" assertion으로 "concrete-links-472: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-472" assertion으로 "link-name-472: /data/hostObservation/extractor/rawRows/namedObservations/472/observationName의 실제 equals 기대값은 'bank-transfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-472" assertion으로 "link-oracle-472: /data/hostObservation/extractor/rawRows/namedObservations/472/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-473" assertion으로 "concrete-links-473: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-473" assertion으로 "link-name-473: /data/hostObservation/extractor/rawRows/namedObservations/473/observationName의 실제 equals 기대값은 'receipt60-doc-duplicate-effects'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-473" assertion으로 "link-oracle-473: /data/hostObservation/extractor/rawRows/namedObservations/473/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-474" assertion으로 "concrete-links-474: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-474" assertion으로 "link-name-474: /data/hostObservation/extractor/rawRows/namedObservations/474/observationName의 실제 equals 기대값은 'return-added-purchase-arrival'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-474" assertion으로 "link-oracle-474: /data/hostObservation/extractor/rawRows/namedObservations/474/oracleId의 실제 equals 기대값은 'E1.full-flow-quantities'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-475" assertion으로 "concrete-links-475: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-475" assertion으로 "link-name-475: /data/hostObservation/extractor/rawRows/namedObservations/475/observationName의 실제 equals 기대값은 'logistics-goal'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-475" assertion으로 "link-oracle-475: /data/hostObservation/extractor/rawRows/namedObservations/475/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-476" assertion으로 "concrete-links-476: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-476" assertion으로 "link-name-476: /data/hostObservation/extractor/rawRows/namedObservations/476/observationName의 실제 equals 기대값은 'invoice-difference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-476" assertion으로 "link-oracle-476: /data/hostObservation/extractor/rawRows/namedObservations/476/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-477" assertion으로 "concrete-links-477: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-477" assertion으로 "link-name-477: /data/hostObservation/extractor/rawRows/namedObservations/477/observationName의 실제 equals 기대값은 'QC-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-477" assertion으로 "link-oracle-477: /data/hostObservation/extractor/rawRows/namedObservations/477/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-478" assertion으로 "concrete-links-478: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-478" assertion으로 "link-name-478: /data/hostObservation/extractor/rawRows/namedObservations/478/observationName의 실제 equals 기대값은 'return-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-478" assertion으로 "link-oracle-478: /data/hostObservation/extractor/rawRows/namedObservations/478/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-479" assertion으로 "concrete-links-479: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-479" assertion으로 "link-name-479: /data/hostObservation/extractor/rawRows/namedObservations/479/observationName의 실제 equals 기대값은 'settlement-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-479" assertion으로 "link-oracle-479: /data/hostObservation/extractor/rawRows/namedObservations/479/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-480" assertion으로 "concrete-links-480: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-480" assertion으로 "link-name-480: /data/hostObservation/extractor/rawRows/namedObservations/480/observationName의 실제 equals 기대값은 'two-entry-snapshot'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-480" assertion으로 "link-oracle-480: /data/hostObservation/extractor/rawRows/namedObservations/480/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-481" assertion으로 "concrete-links-481: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-481" assertion으로 "link-name-481: /data/hostObservation/extractor/rawRows/namedObservations/481/observationName의 실제 equals 기대값은 'logistics-closes-unrelated-duties'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-481" assertion으로 "link-oracle-481: /data/hostObservation/extractor/rawRows/namedObservations/481/oracleId의 실제 equals 기대값은 'E1.independent-goals-and-owners'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-482" assertion으로 "concrete-links-482: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-482" assertion으로 "link-name-482: /data/hostObservation/extractor/rawRows/namedObservations/482/observationName의 실제 equals 기대값은 'actual-runtime-observation'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-482" assertion으로 "link-oracle-482: /data/hostObservation/extractor/rawRows/namedObservations/482/oracleId의 실제 equals 기대값은 'E1.whole-runtime-and-model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-483" assertion으로 "concrete-links-483: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-483" assertion으로 "link-name-483: /data/hostObservation/extractor/rawRows/namedObservations/483/observationName의 실제 equals 기대값은 'model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-483" assertion으로 "link-oracle-483: /data/hostObservation/extractor/rawRows/namedObservations/483/oracleId의 실제 equals 기대값은 'E1.whole-runtime-and-model-reference'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-484" assertion으로 "concrete-links-484: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-484" assertion으로 "link-name-484: /data/hostObservation/extractor/rawRows/namedObservations/484/observationName의 실제 equals 기대값은 'physical-held'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-484" assertion으로 "link-oracle-484: /data/hostObservation/extractor/rawRows/namedObservations/484/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-485" assertion으로 "concrete-links-485: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-485" assertion으로 "link-name-485: /data/hostObservation/extractor/rawRows/namedObservations/485/observationName의 실제 equals 기대값은 'dispatched-after-QC-only-release'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-485" assertion으로 "link-oracle-485: /data/hostObservation/extractor/rawRows/namedObservations/485/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-486" assertion으로 "concrete-links-486: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-486" assertion으로 "link-name-486: /data/hostObservation/extractor/rawRows/namedObservations/486/observationName의 실제 equals 기대값은 'recall-hold'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-486" assertion으로 "link-oracle-486: /data/hostObservation/extractor/rawRows/namedObservations/486/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-487" assertion으로 "concrete-links-487: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-487" assertion으로 "link-name-487: /data/hostObservation/extractor/rawRows/namedObservations/487/observationName의 실제 equals 기대값은 'candidate-as-confirmed-contamination'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-487" assertion으로 "link-oracle-487: /data/hostObservation/extractor/rawRows/namedObservations/487/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-488" assertion으로 "concrete-links-488: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-488" assertion으로 "link-name-488: /data/hostObservation/extractor/rawRows/namedObservations/488/observationName의 실제 equals 기대값은 'investigation-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-488" assertion으로 "link-oracle-488: /data/hostObservation/extractor/rawRows/namedObservations/488/oracleId의 실제 equals 기대값은 'E2.overlapping-holds'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-489" assertion으로 "concrete-links-489: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-489" assertion으로 "link-name-489: /data/hostObservation/extractor/rawRows/namedObservations/489/observationName의 실제 equals 기대값은 'unique-recovered'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-489" assertion으로 "link-oracle-489: /data/hostObservation/extractor/rawRows/namedObservations/489/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-490" assertion으로 "concrete-links-490: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-490" assertion으로 "link-name-490: /data/hostObservation/extractor/rawRows/namedObservations/490/observationName의 실제 equals 기대값은 'unique-finally-processed'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-490" assertion으로 "link-oracle-490: /data/hostObservation/extractor/rawRows/namedObservations/490/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-491" assertion으로 "concrete-links-491: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-491" assertion으로 "link-name-491: /data/hostObservation/extractor/rawRows/namedObservations/491/observationName의 실제 equals 기대값은 'unknown'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-491" assertion으로 "link-oracle-491: /data/hostObservation/extractor/rawRows/namedObservations/491/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-492" assertion으로 "concrete-links-492: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-492" assertion으로 "link-name-492: /data/hostObservation/extractor/rawRows/namedObservations/492/observationName의 실제 equals 기대값은 '25-plus-25-equals50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-492" assertion으로 "link-oracle-492: /data/hostObservation/extractor/rawRows/namedObservations/492/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-493" assertion으로 "concrete-links-493: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-493" assertion으로 "link-name-493: /data/hostObservation/extractor/rawRows/namedObservations/493/observationName의 실제 equals 기대값은 'unapproved-unknown-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-493" assertion으로 "link-oracle-493: /data/hostObservation/extractor/rawRows/namedObservations/493/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-494" assertion으로 "concrete-links-494: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-494" assertion으로 "link-name-494: /data/hostObservation/extractor/rawRows/namedObservations/494/observationName의 실제 equals 기대값은 'status-axes'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-494" assertion으로 "link-oracle-494: /data/hostObservation/extractor/rawRows/namedObservations/494/oracleId의 실제 equals 기대값은 'E2.same-25-not-50'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-495" assertion으로 "concrete-links-495: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-495" assertion으로 "link-name-495: /data/hostObservation/extractor/rawRows/namedObservations/495/observationName의 실제 equals 기대값은 'unauthorized-exception-close'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-495" assertion으로 "link-oracle-495: /data/hostObservation/extractor/rawRows/namedObservations/495/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-496" assertion으로 "concrete-links-496: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-496" assertion으로 "link-name-496: /data/hostObservation/extractor/rawRows/namedObservations/496/observationName의 실제 equals 기대값은 'exception-unresolved-scope'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-496" assertion으로 "link-oracle-496: /data/hostObservation/extractor/rawRows/namedObservations/496/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-497" assertion으로 "concrete-links-497: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-497" assertion으로 "link-name-497: /data/hostObservation/extractor/rawRows/namedObservations/497/observationName의 실제 equals 기대값은 'exception-residual-duty'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-497" assertion으로 "link-oracle-497: /data/hostObservation/extractor/rawRows/namedObservations/497/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "concrete-links-498" assertion으로 "concrete-links-498: 비어 있지 않은 실제 원행마다 caseId, subcaseId, assertionId, profile, status, evidenceRefs를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "link-name-498" assertion으로 "link-name-498: /data/hostObservation/extractor/rawRows/namedObservations/498/observationName의 실제 equals 기대값은 'exception-evidence'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "link-oracle-498" assertion으로 "link-oracle-498: /data/hostObservation/extractor/rawRows/namedObservations/498/oracleId의 실제 equals 기대값은 'E2.exception-responsibility'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 증거 version·command·exit·계층 분리·선행 gate
    먼저 사례 파일 "verification/cases/T25/case.json"의 "evidence-wrapper-fields"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "manifest-required-fields" assertion으로 "manifest-required-fields: 비어 있지 않은 실제 원행마다 codeCommit, schemaVersion, definitionVersion, evaluatorVersion, skillVersion, toolVersion, clientVersion, modelVersion, dbVersion, buildpackVersion, fixtureHash, command, timestamp, expected, observed, artifactRefs, status를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "required-wrapper-profiles" assertion으로 "required-wrapper-profiles: /data/hostObservation/extractor/rawRows/wrappers의 실제 profile는 고정한 9개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wrapper-actual-fields" assertion으로 "wrapper-actual-fields: 비어 있지 않은 실제 원행마다 internalCommand, toolVersions, exitCode, status, dependencyProfiles를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "separate-evidence-classes" assertion으로 "separate-evidence-classes: /data/hostObservation/extractor/rawRows/evidenceClasses의 실제 값는 고정한 9개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "no-regulatory-claim" assertion으로 "no-regulatory-claim: /data/hostObservation/extractor/rawRows/regulatoryReviews의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "regulatory-not-run" assertion으로 "regulatory-not-run: /data/hostObservation/extractor/rawRows/gate/regulatoryStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-no-authorization" assertion으로 "model-no-authorization: /data/hostObservation/extractor/rawRows/gate/modelStatus의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "dependencies-mcp" assertion으로 "dependencies-mcp: /data/hostObservation/extractor/rawRows/dependencies/mcp의 실제 equals 기대값은 ['schema', 'contracts', 'scenarios', 'recovery']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "dependencies-model" assertion으로 "dependencies-model: /data/hostObservation/extractor/rawRows/dependencies/model의 실제 equals 기대값은 ['schema', 'contracts', 'scenarios', 'recovery', 'mcp', 'skills']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실행 증거 missing-law-source mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-law-source"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 missing-cost-approval mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-cost-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 missing-command-version mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "missing-command-version"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 실행 증거 fake-wrapper-success mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "fake-wrapper-success"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "evidence" 행동을 수행한다
    그러면 "invalid-evidence" assertion으로 "invalid-evidence: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-evidence-violation" assertion으로 "specific-evidence-violation: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: M60×3 binding 준비와 실제 model/usage NOT_RUN
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-preparation-not-runtime"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
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
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage missing-attempt-usage mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-missing-attempt-usage"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage null-usage-as-zero mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-null-usage-as-zero"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage partial-cost-complete mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-partial-cost-complete"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
    그러면 "model-mutation-fail" assertion으로 "model-mutation-fail: /data/hostObservation/extractor/rawRows/validation/status의 실제 equals 기대값은 'FAIL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "model-violation-code" assertion으로 "model-violation-code: /data/hostObservation/extractor/rawRows/validation/issues의 실제 code는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "whole-UAT-open" assertion으로 "whole-UAT-open: /data/hostObservation/extractor/rawRows/gate/uatComplete의 실제 equals 기대값은 false다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: model binding/usage unapproved-business-SLA mutant 거부
    먼저 사례 파일 "verification/cases/T25/case.json"의 "model-unapproved-business-SLA"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "model-records" 행동을 수행한다
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
    그러면 "actual-zero-ambiguousImproperExecution" assertion으로 "actual-zero-ambiguousImproperExecution: /data/hostObservation/extractor/rawRows/metrics/ambiguousImproperExecution의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-unauthorized" assertion으로 "actual-zero-unauthorized: /data/hostObservation/extractor/rawRows/metrics/unauthorized의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-duplicate" assertion으로 "actual-zero-duplicate: /data/hostObservation/extractor/rawRows/metrics/duplicate의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-zero-falseCompletion" assertion으로 "actual-zero-falseCompletion: /data/hostObservation/extractor/rawRows/metrics/falseCompletion의 실제 equals 기대값은 0다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-p50LatencyMillis" assertion으로 "metric-p50LatencyMillis: /data/hostObservation/extractor/rawRows/metrics/p50LatencyMillis의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-p95LatencyMillis" assertion으로 "metric-p95LatencyMillis: /data/hostObservation/extractor/rawRows/metrics/p95LatencyMillis의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-clarificationRate" assertion으로 "metric-clarificationRate: /data/hostObservation/extractor/rawRows/metrics/clarificationRate의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "metric-totalCost" assertion으로 "metric-totalCost: /data/hostObservation/extractor/rawRows/metrics/totalCost의 실제 decimalAtLeast 기대값은 '0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
