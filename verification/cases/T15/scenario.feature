# language: ko
@T15 @D15 @sit @uat
기능: 기관 제출 부분 허용과 내부 QC를 구별한다

  시나리오: 기관30 내부QC100의 교집합은 판매30이다
    먼저 사례 파일 "verification/cases/T15/case.json"의 "agency30-qc100"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "regulator" 역할이 "procedure" 행동을 수행한다
    만일 "regulator" 역할이 "submitted" 행동을 수행한다
    만일 "regulator" 역할이 "allow30" 행동을 수행한다
    만일 "regulator" 역할이 "before-qc" 행동을 수행한다
    만일 "시스템" 역할이 "before-qc-db" 행동을 수행한다
    만일 "qc" 역할이 "qc100" 행동을 수행한다
    만일 "regulator" 역할이 "eligibility" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "eligible-api30" assertion으로 "고정 oracle confirmed-sell-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed-leaf30" assertion으로 "고정 oracle confirmed-sell-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "eligible-db30" assertion으로 "고정 oracle confirmed-sell-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unknown70-agency" assertion으로 "고정 oracle remaining-agency의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "QC100-scope" assertion으로 "고정 oracle qc-expanded-agency-allowance의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "qc-no-agency-expansion" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "decision-scope-raw" assertion으로 "고정 oracle decision-scope의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 로컬 작성 미확인 제출 접수 보완 반려 재제출의 version을 보존한다
    먼저 사례 파일 "verification/cases/T15/case.json"의 "procedure-lifecycle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "split-life" 행동을 수행한다
    만일 "regulator" 역할이 "before-local" 행동을 수행한다
    만일 "시스템" 역할이 "before-local-db" 행동을 수행한다
    만일 "regulator" 역할이 "prepare" 행동을 수행한다
    만일 "regulator" 역할이 "local" 행동을 수행한다
    만일 "시스템" 역할이 "local-db" 행동을 수행한다
    만일 "regulator" 역할이 "unknown-submit" 행동을 수행한다
    만일 "regulator" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "unknown-db" 행동을 수행한다
    만일 "regulator" 역할이 "received-submit" 행동을 수행한다
    만일 "regulator" 역할이 "submitted" 행동을 수행한다
    만일 "시스템" 역할이 "submitted-db" 행동을 수행한다
    만일 "regulator" 역할이 "partial" 행동을 수행한다
    만일 "regulator" 역할이 "supplement" 행동을 수행한다
    만일 "regulator" 역할이 "reject" 행동을 수행한다
    만일 "regulator" 역할이 "resubmit-prepare" 행동을 수행한다
    만일 "regulator" 역할이 "resubmit" 행동을 수행한다
    만일 "regulator" 역할이 "label" 행동을 수행한다
    만일 "regulator" 역할이 "history" 행동을 수행한다
    만일 "시스템" 역할이 "history-db" 행동을 수행한다
    그러면 "local-PREPARED" assertion으로 "고정 oracle local-document-state의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "local-db-PREPARED" assertion으로 "고정 oracle local-document-state의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "local-external-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "local-outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "no-local-submit" assertion으로 "고정 oracle local-document-external-submission의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-local-submit-outbox" assertion으로 "고정 oracle local-document-external-submission의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "submit-unconfirmed" assertion으로 "고정 oracle unknown-submit-state의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "submit-unconfirmed-raw" assertion으로 "고정 oracle unknown-submit-state의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "submitted-requires-receipt" assertion으로 "고정 oracle regulatory-history의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "immutable-v1-v2" assertion으로 "고정 oracle regulatory-history의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "decision-history" assertion으로 "고정 oracle regulatory-history의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "labels-not-agency" assertion으로 "고정 oracle regulatory-history의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 실제 규제 officialSource 누락은 운영 허용을 만들지 않는다
    먼저 사례 파일 "verification/cases/T15/case.json"의 "missing-officialSource"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "config" 역할이 "policy-draft" 행동을 수행한다
    만일 "config" 역할이 "before-activate" 행동을 수행한다
    만일 "시스템" 역할이 "before-activate-db" 행동을 수행한다
    만일 "config" 역할이 "activate" 행동을 수행한다
    만일 "regulator" 역할이 "eligible" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "unverified-eligible0" assertion으로 "고정 oracle confirmed-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "legal-UNVERIFIED" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-effect0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "official-active0" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked-error" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fixture-not-official" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "agency-unknown" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed-eligible-agency-allowed0" assertion으로 "검토 근거 없는 정책에서 DB의 AGENCY ALLOWED 원행은 0개로 확정 판매 적격 0 BOX다"를 확인한다

  시나리오: 실제 규제 applicableDate 누락은 운영 허용을 만들지 않는다
    먼저 사례 파일 "verification/cases/T15/case.json"의 "missing-applicableDate"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "config" 역할이 "policy-draft" 행동을 수행한다
    만일 "config" 역할이 "before-activate" 행동을 수행한다
    만일 "시스템" 역할이 "before-activate-db" 행동을 수행한다
    만일 "config" 역할이 "activate" 행동을 수행한다
    만일 "regulator" 역할이 "eligible" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "unverified-eligible0" assertion으로 "고정 oracle confirmed-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "legal-UNVERIFIED" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-effect0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "official-active0" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked-error" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fixture-not-official" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "agency-unknown" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed-eligible-agency-allowed0" assertion으로 "검토 근거 없는 정책에서 DB의 AGENCY ALLOWED 원행은 0개로 확정 판매 적격 0 BOX다"를 확인한다

  시나리오: 실제 규제 reviewer 누락은 운영 허용을 만들지 않는다
    먼저 사례 파일 "verification/cases/T15/case.json"의 "missing-reviewer"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "config" 역할이 "policy-draft" 행동을 수행한다
    만일 "config" 역할이 "before-activate" 행동을 수행한다
    만일 "시스템" 역할이 "before-activate-db" 행동을 수행한다
    만일 "config" 역할이 "activate" 행동을 수행한다
    만일 "regulator" 역할이 "eligible" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "unverified-eligible0" assertion으로 "고정 oracle confirmed-eligible의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "legal-UNVERIFIED" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-effect0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "official-active0" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "activation-blocked-error" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fixture-not-official" assertion으로 "고정 oracle regulatory-gate의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "agency-unknown" assertion으로 "고정 oracle legal-eligibility의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed-eligible-agency-allowed0" assertion으로 "검토 근거 없는 정책에서 DB의 AGENCY ALLOWED 원행은 0개로 확정 판매 적격 0 BOX다"를 확인한다
