# 수령 잔여 책임의 typed 해소

기존 구현은 각 수령마다 새 부족 root를 만들어 60+20+20 수령 뒤에도
40과 20의 의무가 남았다. 실제 발주 기여가 부족을 줄인다는 원인을
현재 Receipt와 immutable ReceiptCredits에서 읽어 한 root를 해소한다.

TradeResidualRemedy는 receiptId만 받는다. 조직·발주 line·업무·품목·장소·
단위·canonical PHYSICAL_RECEIPT를 대조하고 현재 기여 합계를 읽는다.
최초 부족량과 당시 누적 기여를 ReceiptResidualRoots에 불변으로 묶는다.
그 뒤 추가 기여는 해당 root의 앞쪽 구간을 덮는다. root 수량은 바꾸지
않고 leaf를 split해 해소된 leaf와 OPEN 잔여를 보존한다.
40 중 10이 다른 업무로 인계됐다면 그 10의 owner·supervisor·기한을
보존한다. 새 기여가 그 구간에 도달할 때만 해당 recipient의 의무를
해소한다. 이미 승인으로 RESOLVED/WAIVED 된 leaf는 다시 열지 않는다.
현재 주문량 변경은 자동 면제가 아니므로 별도 재판정을 요구한다.

이행 증거가 PHYSICAL_RECEIPT이므로 RESPONSE_COMPLETED를 만들지
않는다. V22의 ReceiptResidualCredits가 receipt와 정확한 leaf를 연결하고
DB에서 발주 기여 범위를 확인한다. 이 원장은 기존 응답 이행 credit과
별개다. 원천 정정은 과거 credit이나 root를 고치지 않는다.

provisional 대조도 observationId·confirmedReceiptId로 정확히 해소한다.
원천B의 동일 물량은 기존 receipt를 참조한다. ObservationReceiptCredits는
그 observation의 domainSourceId·range·업무와 일치하는 root만 해소한다.
무관한 원천 대조 책임과 이미 해소된 의무는 그대로 보존된다.

모든 write는 기존 command transaction과 fence에 참여한다. 범용 공개
command나 client supplied 잔여 scalar는 없다. PostgreSQL constraint
검증과 gateway 인수 결과는 실제 실행 뒤 별도로 기록한다.
