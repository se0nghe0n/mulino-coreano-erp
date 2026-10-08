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
    만일 "observer" 역할이 "receipt60-after-doc-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "receipt60-after-doc-db" 행동을 수행한다
    만일 "procurement" 역할이 "receipt40" 행동을 수행한다
    만일 "observer" 역할이 "received-custody" 행동을 수행한다
    만일 "시스템" 역할이 "received-custody-db" 행동을 수행한다
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
    만일 "observer" 역할이 "e1-mcp" 행동을 수행한다
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
    그러면 "received-custody-control" assertion으로 "보류 전 W 활성 실물 = 수령60 BOX·수령40 BOX, 둘 다 내부 보관자 receiver"를 확인한다
    그러면 "agency-unverified-held-11" assertion으로 "agency-unverified-held"를 확인한다
    그러면 "agency-unverified-held-12" assertion으로 "agency-unverified-held"를 확인한다
    그러면 "agency-unverified-held-axis70" assertion으로 "규제 축 UNKNOWN 구매 수령분은 QC 상태와 독립으로 70 BOX다"를 확인한다
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
    그러면 "e1-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "purchase-cumulative-arrival-1-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 purchase-cumulative-arrival-1와 같은 값을 읽는다"를 확인한다
    그러면 "warehouse-current-held-3-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 warehouse-current-held-3와 같은 값을 읽는다"를 확인한다
    그러면 "historical-delivery-5-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 historical-delivery-5와 같은 값을 읽는다"를 확인한다
    그러면 "return-received-7-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 return-received-7와 같은 값을 읽는다"를 확인한다
    그러면 "current-sell-eligible-9-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 current-sell-eligible-9와 같은 값을 읽는다"를 확인한다
    그러면 "agency-unverified-held-11-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 agency-unverified-held-11와 같은 값을 읽는다"를 확인한다
    그러면 "QC-held-13-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 QC-held-13와 같은 값을 읽는다"를 확인한다
    그러면 "return-held-15-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 return-held-15와 같은 값을 읽는다"를 확인한다
    그러면 "bank-transfer-18-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 bank-transfer-18와 같은 값을 읽는다"를 확인한다
    그러면 "receipt60-after-doc-mcp-same-snapshot" assertion으로 "MCP 조회는 문서 연결 뒤 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-22-mcp" assertion으로 "MCP 진입점: 수령60 문서 2건 연결 뒤 segments가 연결 전과 같아 60 BOX가 120 BOX로 늘지 않는다"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-23-mcp" assertion으로 "MCP 진입점: 수령60 문서 2건 연결 뒤 movements가 연결 전과 같아 60 BOX가 120 BOX로 늘지 않는다"를 확인한다
    그러면 "receipt60-doc-duplicate-effects-24-mcp" assertion으로 "MCP 진입점: 수령60 문서 2건 연결 뒤 receiptContributions가 연결 전과 같아 60 BOX가 120 BOX로 늘지 않는다"를 확인한다
    그러면 "return-added-purchase-arrival-mcp" assertion으로 "MCP 진입점: 반품10 BOX 뒤에도 구매 누적 도착은 반품 전 100 BOX와 같다"를 확인한다

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
    만일 "observer" 역할이 "received-custody" 행동을 수행한다
    만일 "시스템" 역할이 "received-custody-db" 행동을 수행한다
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
    만일 "observer" 역할이 "goals-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "goals-db" 행동을 수행한다
    만일 "observer" 역할이 "noun" 행동을 수행한다
    만일 "observer" 역할이 "noun-mcp" 행동을 수행한다
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
    그러면 "QC-duty-hold60-resolved" assertion으로 "QC_PASS로 해제한 hold60 의무는 QC·RESOLVED·current로 남는다"를 확인한다
    그러면 "QC-duty-hold60-resolution-evidence" assertion으로 "해소된 hold60 의무는 해소 결정 근거를 가진다"를 확인한다
    그러면 "QC-duty-hold40-identity" assertion으로 "남은 OPEN QC 의무는 QC 보류40의 의무이고 owner는 qc다"를 확인한다
    그러면 "return-duty-kind-set" assertion으로 "반품10의 현재 의무 kind는 RETURN_QC_REVIEW·RETURN_COMMERCIAL_REVIEW·RETURN_SETTLEMENT_REVIEW 셋이다"를 확인한다
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
    그러면 "two-entry-anchor-snapshot" assertion으로 "명사 linkedSummary의 snapshot은 goals 원장 관찰과 같다"를 확인한다
    그러면 "two-entry-anchor-asOf" assertion으로 "명사 linkedSummary의 asOf는 고정 업무 시계와 같다"를 확인한다
    그러면 "two-entry-anchor-knownAt" assertion으로 "명사 linkedSummary의 knownAt은 고정 업무 시계와 같다"를 확인한다
    그러면 "two-entry-anchor-duty-fields" assertion으로 "명사 linkedSummary duties는 비어 있지 않고 필수 필드를 가진다"를 확인한다
    그러면 "two-entry-anchor-qc-duty-ledger" assertion으로 "명사 linkedSummary의 OPEN QC 의무는 독립 원장 행과 같다"를 확인한다
    그러면 "two-entry-anchor-return-duty-ledger" assertion으로 "명사 linkedSummary의 OPEN 반품 의무는 독립 원장 행과 같다"를 확인한다
    그러면 "two-entry-anchor-settlement-duty-ledger" assertion으로 "명사 linkedSummary의 OPEN 정산 의무는 독립 원장 행과 같다"를 확인한다
    그러면 "two-entry-anchor-qc-duty-identity" assertion으로 "명사 linkedSummary의 OPEN QC 의무는 QC 보류40의 의무다"를 확인한다
    그러면 "two-entry-anchor-return-owner" assertion으로 "명사 linkedSummary의 반품 의무 owner는 receiver다"를 확인한다
    그러면 "two-entry-anchor-settlement-owner" assertion으로 "명사 linkedSummary의 정산 의무 owner는 settlement다"를 확인한다
    그러면 "two-entry-duties-noun-verb" assertion으로 "동사 linkedSummary duties는 명사와 같다"를 확인한다
    그러면 "two-entry-anchor-purchaseArrivalQuantity" assertion으로 "linkedSummary 구매 누적 도착은 100 BOX다"를 확인한다
    그러면 "two-entry-anchor-heldQuantity" assertion으로 "linkedSummary W 보유는 80 BOX다"를 확인한다
    그러면 "two-entry-anchor-deliveredQuantity" assertion으로 "linkedSummary 과거 인도는 30 BOX다"를 확인한다
    그러면 "two-entry-anchor-returnedQuantity" assertion으로 "linkedSummary 반품은 10 BOX다"를 확인한다
    그러면 "two-entry-anchor-eligibleQuantity" assertion으로 "linkedSummary 현재 판매 적격은 0 BOX다"를 확인한다
    그러면 "received-custody-control" assertion으로 "보류 전 W 활성 실물 = 수령60 BOX·수령40 BOX, 둘 다 내부 보관자 receiver"를 확인한다
    그러면 "two-entry-anchor-invoiceDifference" assertion으로 "linkedSummary 송장 차이는 5 EUR다"를 확인한다
    그러면 "logistics-closes-unrelated-duties-35" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-36" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-37" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "logistics-closes-unrelated-duties-38" assertion으로 "logistics-closes-unrelated-duties"를 확인한다
    그러면 "goals-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "logistics-goal-1-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 logistics-goal-1와 같은 값을 읽는다"를 확인한다
    그러면 "invoice-difference-3-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 invoice-difference-3와 같은 값을 읽는다"를 확인한다
    그러면 "QC-duty-11-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 QC-duty-11와 같은 값을 읽는다"를 확인한다
    그러면 "return-duty-18-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 return-duty-18와 같은 값을 읽는다"를 확인한다
    그러면 "settlement-duty-25-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 settlement-duty-25와 같은 값을 읽는다"를 확인한다
    그러면 "noun-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-26-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-26와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-27-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-27와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-28-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-28와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-29-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-29와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-30-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-30와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-31-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-31와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-32-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-32와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-33-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-33와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-snapshot-34-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-snapshot-34와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-snapshot-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-snapshot와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-asOf-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-asOf와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-knownAt-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-knownAt와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-duty-fields-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-duty-fields와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-qc-duty-ledger-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-qc-duty-ledger와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-return-duty-ledger-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-return-duty-ledger와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-settlement-duty-ledger-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-settlement-duty-ledger와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-qc-duty-identity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-qc-duty-identity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-return-owner-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-return-owner와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-settlement-owner-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-settlement-owner와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-duties-noun-verb-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-duties-noun-verb와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-purchaseArrivalQuantity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-purchaseArrivalQuantity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-heldQuantity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-heldQuantity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-deliveredQuantity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-deliveredQuantity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-returnedQuantity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-returnedQuantity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-eligibleQuantity-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-eligibleQuantity와 같은 값을 읽는다"를 확인한다
    그러면 "two-entry-anchor-invoiceDifference-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 two-entry-anchor-invoiceDifference와 같은 값을 읽는다"를 확인한다
    그러면 "logistics-closes-unrelated-duties-35-mcp" assertion으로 "MCP 진입점: 물류100 충족 뒤에도 QC 보류40 BOX의 의무는 OPEN·current다"를 확인한다
    그러면 "logistics-closes-unrelated-duties-36-mcp" assertion으로 "MCP 진입점: 물류 충족 뒤 OPEN QC 의무는 정확히 1건이다"를 확인한다
    그러면 "logistics-closes-unrelated-duties-37-mcp" assertion으로 "MCP 진입점: 물류100 충족 뒤에도 반품10 BOX의 RETURN 의무는 OPEN뿐이다"를 확인한다
    그러면 "logistics-closes-unrelated-duties-38-mcp" assertion으로 "MCP 진입점: 물류100 충족 뒤에도 송장 차이 5 EUR의 SETTLEMENT 의무는 OPEN뿐이다"를 확인한다

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
    만일 "observer" 역할이 "received-custody" 행동을 수행한다
    만일 "시스템" 역할이 "received-custody-db" 행동을 수행한다
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
    만일 "sales" 역할이 "consume-again-target" 행동을 수행한다
    만일 "sales" 역할이 "consume-again" 행동을 수행한다
    만일 "receiver" 역할이 "delivery-again" 행동을 수행한다
    만일 "observer" 역할이 "runtime" 행동을 수행한다
    만일 "observer" 역할이 "runtime-mcp" 행동을 수행한다
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
    그러면 "actual-runtime-observation-10-reason" assertion으로 "이미 소비된 배분의 같은 형식 재출고는 INSUFFICIENT_ELIGIBLE_QUANTITY로 거부된다"를 확인한다
    그러면 "actual-runtime-observation-10-audit" assertion으로 "재출고 거부 감사가 정확히 1건 남는다"를 확인한다
    그러면 "actual-runtime-observation-11" assertion으로 "e1 시점 모든 명령 감사 원행에 auditId·actorId·capabilityId·commandId·outcome·evidenceIds·policyVersion이 있다"를 확인한다
    그러면 "actual-runtime-observation-12" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-13" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-14" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-15" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-16" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-17" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-18" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-19" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-20" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-21" assertion으로 "거부 감사로 기록된 행 중 APPLIED는 0건이다"를 확인한다
    그러면 "model-reference-22" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-23" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-24" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-25" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-26" assertion으로 "model-reference"를 확인한다
    그러면 "model-reference-27" assertion으로 "model-reference"를 확인한다
    그러면 "actual-runtime-observation-28" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "actual-runtime-observation-29" assertion으로 "actual-runtime-observation"를 확인한다
    그러면 "received-custody-control" assertion으로 "보류 전 W 활성 실물 = 수령60 BOX·수령40 BOX, 둘 다 내부 보관자 receiver"를 확인한다
    그러면 "runtime-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "actual-runtime-observation-5-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 actual-runtime-observation-5와 같은 값을 읽는다"를 확인한다
    그러면 "actual-runtime-observation-6-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 actual-runtime-observation-6와 같은 값을 읽는다"를 확인한다
    그러면 "actual-runtime-observation-7-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 actual-runtime-observation-7와 같은 값을 읽는다"를 확인한다
    그러면 "actual-runtime-observation-8-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 actual-runtime-observation-8와 같은 값을 읽는다"를 확인한다
