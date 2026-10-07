# language: ko
@E1 @D05 @D09 @D13 @D15 @D16 @D17 @D18 @D19 @sit @uat
기능: 구매부터 반품·정산 후속까지 전체 수량·책임

  시나리오: 구매100→수령60+40→허용30 판매→반품10으로 W80·과거인도30·적격0을 계산한다
    먼저 사례 파일 "verification/cases/E1/case.json"의 "full-flow-quantities"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "purchase" 행동을 수행한다
    만일 "manager" 역할이 "approve-purchase" 행동을 수행한다
    만일 "procurement" 역할이 "transmit-po" 행동을 수행한다
    만일 "procurement" 역할이 "supplier-accept" 행동을 수행한다
    만일 "procurement" 역할이 "shipment" 행동을 수행한다
    만일 "procurement" 역할이 "leg-departure" 행동을 수행한다
    만일 "procurement" 역할이 "receipt60" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-before-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-before-doc-db" 행동을 수행한다
    만일 "receiver" 역할이 "carrier60-document" 행동을 수행한다
    만일 "procurement" 역할이 "canonical60-link" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-after-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-after-doc-db" 행동을 수행한다
    만일 "procurement" 역할이 "receipt40" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold60" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold40" 행동을 수행한다
    만일 "qc" 역할이 "qc-pass60" 행동을 수행한다
    만일 "procurement" 역할이 "split60" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-prepare" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-submit" 행동을 수행한다
    만일 "procurement" 역할이 "agency-allow30" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "before-return" 행동을 수행한다
    만일 "시스템" 역할이 "before-return-db" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "e1" 행동을 수행한다
    만일 "시스템" 역할이 "e1-db" 행동을 수행한다
    그러면 "purchase-cumulative-arrival-1" assertion으로 "purchase-cumulative-arrival"를 확인한다
    그러면 "purchase-cumulative-arrival-2" assertion으로 "purchase-cumulative-arrival"를 확인한다
    그러면 "warehouse-current-held-3" assertion으로 "warehouse-current-held"를 확인한다
    그러면 "warehouse-current-held-4" assertion으로 "warehouse-current-held"를 확인한다
    그러면 "historical-delivery-5" assertion으로 "historical-delivery"를 확인한다
    그러면 "historical-delivery-6" assertion으로 "historical-delivery"를 확인한다
    그러면 "return-received-7" assertion으로 "return-received"를 확인한다
    그러면 "return-received-8" assertion으로 "return-received"를 확인한다
    그러면 "current-sell-eligible-9" assertion으로 "current-sell-eligible"를 확인한다
    그러면 "current-sell-eligible-10" assertion으로 "current-sell-eligible"를 확인한다
    그러면 "agency-unverified-held-11" assertion으로 "agency-unverified-held"를 확인한다
    그러면 "agency-unverified-held-12" assertion으로 "agency-unverified-held"를 확인한다
    그러면 "QC-held-13" assertion으로 "QC-held"를 확인한다
    그러면 "QC-held-14" assertion으로 "QC-held"를 확인한다
    그러면 "return-held-15" assertion으로 "return-held"를 확인한다
    그러면 "return-held-16" assertion으로 "return-held"를 확인한다
    그러면 "bank-transfer-17" assertion으로 "bank-transfer"를 확인한다
    그러면 "bank-transfer-18" assertion으로 "bank-transfer"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-19" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-20" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-21" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-22" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-23" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-24" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다
    그러면 "return-added-purchase-arrival-25" assertion으로 "return-added-purchase-arrival"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-26" assertion으로 "receipt60-doc-duplicate-effects"를 확인한다

  시나리오: 전체 물류100 충족 뒤 송장5EUR 차이와 QC40·반품10·정산 owner는 별도로 남는다
    먼저 사례 파일 "verification/cases/E1/case.json"의 "independent-goals-and-owners"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "purchase" 행동을 수행한다
    만일 "manager" 역할이 "approve-purchase" 행동을 수행한다
    만일 "procurement" 역할이 "transmit-po" 행동을 수행한다
    만일 "procurement" 역할이 "supplier-accept" 행동을 수행한다
    만일 "procurement" 역할이 "shipment" 행동을 수행한다
    만일 "procurement" 역할이 "leg-departure" 행동을 수행한다
    만일 "procurement" 역할이 "receipt60" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-before-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-before-doc-db" 행동을 수행한다
    만일 "receiver" 역할이 "carrier60-document" 행동을 수행한다
    만일 "procurement" 역할이 "canonical60-link" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-after-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-after-doc-db" 행동을 수행한다
    만일 "procurement" 역할이 "receipt40" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold60" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold40" 행동을 수행한다
    만일 "qc" 역할이 "qc-pass60" 행동을 수행한다
    만일 "procurement" 역할이 "split60" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-prepare" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-submit" 행동을 수행한다
    만일 "procurement" 역할이 "agency-allow30" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "before-return" 행동을 수행한다
    만일 "시스템" 역할이 "before-return-db" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "e1" 행동을 수행한다
    만일 "시스템" 역할이 "e1-db" 행동을 수행한다
    만일 "settlement" 역할이 "invoice" 행동을 수행한다
    만일 "settlement" 역할이 "match-invoice" 행동을 수행한다
    만일 "observer" 역할이 "goals" 행동을 수행한다
    만일 "시스템" 역할이 "goals-db" 행동을 수행한다
    만일 "observer" 역할이 "noun" 행동을 수행한다
    만일 "observer" 역할이 "verb" 행동을 수행한다
    그러면 "logistics-goal-1" assertion으로 "logistics-goal"를 확인한다
    그러면 "logistics-goal-2" assertion으로 "logistics-goal"를 확인한다
    그러면 "invoice-difference-3" assertion으로 "invoice-difference"를 확인한다
    그러면 "invoice-difference-4" assertion으로 "invoice-difference"를 확인한다
    그러면 "QC-duty-5" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-6" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-7" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-8" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-9" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-10" assertion으로 "QC-duty"를 확인한다
    그러면 "QC-duty-11" assertion으로 "QC-duty"를 확인한다
    그러면 "return-duty-12" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-13" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-14" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-15" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-16" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-17" assertion으로 "return-duty"를 확인한다
    그러면 "return-duty-18" assertion으로 "return-duty"를 확인한다
    그러면 "settlement-duty-19" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-20" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-21" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-22" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-23" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-24" assertion으로 "settlement-duty"를 확인한다
    그러면 "settlement-duty-25" assertion으로 "settlement-duty"를 확인한다
    그러면 "two-entry-snapshot-26" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-27" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-28" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-29" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-30" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-31" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-32" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-33" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "two-entry-snapshot-34" assertion으로 "two-entry-snapshot"를 확인한다
    그러면 "logistics-closes-unrelated-duties-35" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-36" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-37" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-38" assertion으로 "logistics-closes-unrelated-duties"를 확인한다

  시나리오: 실제 원장·배분·판정·책임·감사·outbox를 묶고 실모델 gate는 독립 host 입력으로 구분한다
    먼저 사례 파일 "verification/cases/E1/case.json"의 "whole-runtime-and-model-reference"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "purchase" 행동을 수행한다
    만일 "manager" 역할이 "approve-purchase" 행동을 수행한다
    만일 "procurement" 역할이 "transmit-po" 행동을 수행한다
    만일 "procurement" 역할이 "supplier-accept" 행동을 수행한다
    만일 "procurement" 역할이 "shipment" 행동을 수행한다
    만일 "procurement" 역할이 "leg-departure" 행동을 수행한다
    만일 "procurement" 역할이 "receipt60" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-before-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-before-doc-db" 행동을 수행한다
    만일 "receiver" 역할이 "carrier60-document" 행동을 수행한다
    만일 "procurement" 역할이 "canonical60-link" 행동을 수행한다
    만일 "observer" 역할이 "receipt60-after-doc" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-after-doc-db" 행동을 수행한다
    만일 "procurement" 역할이 "receipt40" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold60" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold40" 행동을 수행한다
    만일 "qc" 역할이 "qc-pass60" 행동을 수행한다
    만일 "procurement" 역할이 "split60" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-prepare" 행동을 수행한다
    만일 "procurement" 역할이 "regulatory-submit" 행동을 수행한다
    만일 "procurement" 역할이 "agency-allow30" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "before-return" 행동을 수행한다
    만일 "시스템" 역할이 "before-return-db" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "e1" 행동을 수행한다
    만일 "시스템" 역할이 "e1-db" 행동을 수행한다
    만일 "operator" 역할이 "unauthorized-approval" 행동을 수행한다
    만일 "sales" 역할이 "consume-again" 행동을 수행한다
    만일 "receiver" 역할이 "delivery-again" 행동을 수행한다
    만일 "observer" 역할이 "runtime" 행동을 수행한다
    만일 "시스템" 역할이 "runtime-db" 행동을 수행한다
    만일 "시스템" 역할이 "model-reference-host" 행동을 수행한다
    그러면 "actual-runtime-observation-1" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-2" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-3" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-4" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-5" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-6" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-7" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-8" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-9" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-10" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-11" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-12" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-13" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-14" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-15" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-16" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-17" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-18" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-19" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-20" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-21" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "model-reference-22" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-23" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-24" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-25" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-26" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-27" assertion으로 "model-reference"를 확인한다
    그러면 "actual-runtime-observation-28" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-29" assertion으로 "actual-runtime-observation"를 확인한다
