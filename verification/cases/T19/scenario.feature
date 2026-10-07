# language: ko
@T19 @D19 @sit @uat
기능: 송장 차이 환율 근거와 관리 결정을 보존한다

  시나리오: 물류100 도착 뒤 송장105 EUR의 차이5 EUR는 별도 책임이다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "invoice-match-difference"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "charge" 행동을 수행한다
    만일 "ordinary" 역할이 "match" 행동을 수행한다
    만일 "ordinary" 역할이 "sale-invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "sale-match" 행동을 수행한다
    만일 "ordinary" 역할이 "adjustment-proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "logistics-satisfied" assertion으로 "고정 oracle logistics-assessment의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "logistics-db" assertion으로 "고정 oracle logistics-assessment의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original5" assertion으로 "고정 oracle original-invoice-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original-difference5" assertion으로 "고정 oracle original-invoice-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "settlement-unsatisfied" assertion으로 "고정 oracle settlement-assessment의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "settlement-owner-one" assertion으로 "고정 oracle settlement-duty의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "settlement-owner-owner" assertion으로 "고정 oracle settlement-duty의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchase-three-way" assertion으로 "고정 oracle matching-links의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "sale-delivery-invoice" assertion으로 "고정 oracle matching-links의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "adjustment-original-kept" assertion으로 "고정 oracle matching-links의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 발주 수령100과 송장105 상자의 차이5를 금액5 EUR와 구분한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "invoice-quantity-difference"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "match" 행동을 수행한다
    만일 "ordinary" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "quantity-match-scope" assertion으로 "고정 oracle matching-links의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "quantity-difference5BOX" assertion으로 "고정 oracle matching-links의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "amount-difference5EUR" assertion으로 "고정 oracle original-invoice-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original-money5" assertion으로 "고정 oracle original-invoice-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "logistics-still-satisfied" assertion으로 "고정 oracle logistics-assessment의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "settlement-still-open" assertion으로 "고정 oracle settlement-assessment의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "quantity-mismatch-owner-one" assertion으로 "고정 oracle settlement-duty의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "quantity-mismatch-owner-owner" assertion으로 "고정 oracle settlement-duty의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: COMMERCIAL 문서는 원100 EUR와 환산150000 KRW를 구분한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "fx-commercial"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "before-payment" 행동을 수행한다
    만일 "시스템" 역할이 "before-payment-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-result" 행동을 수행한다
    만일 "ordinary" 역할이 "payment-ref" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-draft" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-approved" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-active" 행동을 수행한다
    만일 "ordinary" 역할이 "after-payment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "original100EUR" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "converted150000KRW" assertion으로 "고정 oracle converted-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-exact-snapshot" assertion으로 "고정 oracle fx-snapshot의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "invoice-kind-exact" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-original-immutable" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "commercial-no-domestic-tax" assertion으로 "고정 oracle commercial-as-domestic-tax-document의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "bankTransfers-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-count0" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "taxOperations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "taxOperations-count0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "paymentAuthorizations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "paymentAuthorizations-count0" assertion으로 "고정 oracle qc-pass-payment-authorization의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "payment-is-reference" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "tax-outbox0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: DOMESTIC_TAX 문서는 원100 EUR와 환산150000 KRW를 구분한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "fx-domestic_tax"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "before-payment" 행동을 수행한다
    만일 "시스템" 역할이 "before-payment-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-result" 행동을 수행한다
    만일 "ordinary" 역할이 "payment-ref" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-draft" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-approved" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-active" 행동을 수행한다
    만일 "ordinary" 역할이 "after-payment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "original100EUR" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "converted150000KRW" assertion으로 "고정 oracle converted-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-exact-snapshot" assertion으로 "고정 oracle fx-snapshot의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "invoice-kind-exact" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-original-immutable" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-count0" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "taxOperations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "taxOperations-count0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "paymentAuthorizations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "paymentAuthorizations-count0" assertion으로 "고정 oracle qc-pass-payment-authorization의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "payment-is-reference" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "tax-outbox0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: CORRECTION 문서는 원100 EUR와 환산150000 KRW를 구분한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "fx-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "before-payment" 행동을 수행한다
    만일 "시스템" 역할이 "before-payment-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-result" 행동을 수행한다
    만일 "ordinary" 역할이 "payment-ref" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-draft" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-approved" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-active" 행동을 수행한다
    만일 "ordinary" 역할이 "after-payment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "original100EUR" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "converted150000KRW" assertion으로 "고정 oracle converted-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-exact-snapshot" assertion으로 "고정 oracle fx-snapshot의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "invoice-kind-exact" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-original-immutable" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-count0" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "taxOperations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "taxOperations-count0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "paymentAuthorizations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "paymentAuthorizations-count0" assertion으로 "고정 oracle qc-pass-payment-authorization의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "payment-is-reference" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "tax-outbox0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: CREDIT_NOTE 문서는 원100 EUR와 환산150000 KRW를 구분한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "fx-credit_note"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "before-payment" 행동을 수행한다
    만일 "시스템" 역할이 "before-payment-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-result" 행동을 수행한다
    만일 "ordinary" 역할이 "payment-ref" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-draft" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-approved" 행동을 수행한다
    만일 "config" 역할이 "fx-policy-active" 행동을 수행한다
    만일 "ordinary" 역할이 "after-payment" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "original100EUR" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "converted150000KRW" assertion으로 "고정 oracle converted-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-exact-snapshot" assertion으로 "고정 oracle fx-snapshot의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "invoice-kind-exact" assertion으로 "고정 oracle original-amount의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "fx-original-immutable" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "bankTransfers-count0" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "taxOperations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "taxOperations-count0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "paymentAuthorizations-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "paymentAuthorizations-count0" assertion으로 "고정 oracle qc-pass-payment-authorization의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "payment-is-reference" assertion으로 "고정 oracle payment-reference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "tax-outbox0" assertion으로 "고정 oracle automatic-tax-issue-or-submit의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 정산 차이5 EUR의 valid MANAGER 결정 경계를 확인한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "settlement-valid"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "match" 행동을 수행한다
    만일 "ordinary" 역할이 "proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "before-confirm" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-ordinary" 행동을 수행한다
    만일 "시스템" 역할이 "ordinary-db" 행동을 수행한다
    만일 "manager" 역할이 "manager-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-manager" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "ordinary-proposal-APPLIED" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager-error" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-confirmation0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "original-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "proposal-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "ordinary-record-proposal" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-denial-audit" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "one-manager-confirmation" assertion으로 "고정 oracle authorized-confirmation-count의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed5EUR" assertion으로 "고정 oracle confirmed-settlement-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "current-manager-boundary" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmed-api5" assertion으로 "고정 oracle confirmed-settlement-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "preserved-original-api5" assertion으로 "고정 oracle preserved-original-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "manager-applied" assertion으로 "고정 oracle authorized-confirmation-count의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmation-not-resolution-one" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "confirmation-not-resolution-owner" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original5-preserved" assertion으로 "고정 oracle preserved-original-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-bank-delta" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "no-bank0" assertion으로 "고정 oracle confirmed-difference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 정산 차이5 EUR의 unauthorized MANAGER 결정 경계를 확인한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "settlement-unauthorized"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "match" 행동을 수행한다
    만일 "ordinary" 역할이 "proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "before-confirm" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-ordinary" 행동을 수행한다
    만일 "시스템" 역할이 "ordinary-db" 행동을 수행한다
    만일 "unauthorized" 역할이 "manager-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-manager" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "ordinary-proposal-APPLIED" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager-error" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-confirmation0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "original-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "proposal-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "ordinary-record-proposal" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-denial-audit" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "manager-denial" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "manager-denial-error" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denied-manager0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "denied-manager-responsibility-one" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denied-manager-responsibility-owner" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original5-preserved" assertion으로 "고정 oracle preserved-original-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-bank-delta" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "no-bank0" assertion으로 "고정 oracle confirmed-difference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 정산 차이5 EUR의 stale MANAGER 결정 경계를 확인한다
    먼저 사례 파일 "verification/cases/T19/case.json"의 "settlement-stale"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "ordinary" 역할이 "invoice" 행동을 수행한다
    만일 "ordinary" 역할이 "match" 행동을 수행한다
    만일 "ordinary" 역할이 "proposal" 행동을 수행한다
    만일 "ordinary" 역할이 "before-confirm" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "ordinary" 역할이 "ordinary-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-ordinary" 행동을 수행한다
    만일 "시스템" 역할이 "ordinary-db" 행동을 수행한다
    만일 "ordinary" 역할이 "proposal-revision" 행동을 수행한다
    만일 "manager" 역할이 "manager-confirm" 행동을 수행한다
    만일 "ordinary" 역할이 "after-manager" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "ordinary-proposal-APPLIED" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-not-manager-error" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-confirmation0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "original-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "proposal-preserved-ordinary" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "ordinary-record-proposal" assertion으로 "고정 oracle observations-proposal-preserved의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "ordinary-denial-audit" assertion으로 "고정 oracle ordinary-write-confirmed-settlement-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "manager-denial" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "manager-denial-error" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denied-manager0" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "denied-manager-responsibility-one" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denied-manager-responsibility-owner" assertion으로 "고정 oracle management-decision-boundary의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "original5-preserved" assertion으로 "고정 oracle preserved-original-difference의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "no-bank-delta" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "no-bank0" assertion으로 "고정 oracle confirmed-difference-bank-transfer의 실제 값과 범위를 확인한다"를 확인한다
