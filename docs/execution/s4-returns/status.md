# S4 반품 구현 기록

사용자 Step3과 S4는 계속 ACTIVE다. 기준선은
`1387024a6c5176d3f663f41e30dd22c1b268faef`다.
소유 branch는 `step3/s4-returns`이며 shared ports `7a021d36`와
inventory range helper `600191cc`는 별도 dependency다.

반품을 과거 인도의 감소나 새 구매 도착으로 처리하면 C4와 E1의
수량이 달라진다. `ReturnCommands`는 관리 권한이 현재 유효한 원 인도
범위의 반품 승인, 재고 효과가 없는 임시 관측, 원문 대조가 끝난
실물 반품 이동을 분리한다. `ReturnStockPrimitives`는 실제 고객 leaf의
정확한 계보 구간을 창고로 이동하고 QC ALL 보류를 남긴다. 새 재고를
만들지 않는다. 서로 다른 사건·출처·원 인도라도 같은 root 구간을
반복 접수할 수 없도록 application fence와 PostgreSQL exclusion이
함께 막는다.

교환·환불·정산과 QC 후속 의무는 독립적으로 남는다. 처분 결정은
관리 권한과 정책에 지정한 QC/ADMIN 권한을 다시 확인하며, 결정
기록으로 QC 보류를 자동 해제하거나 은행 효과를 만들지 않는다.
후속 실제 처분은 별도 현재 인가된 품질/재고 명령으로 실행한다.

## 실행 증거

Java는 `/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home`이며
Node는 고정 Node24 runtime을 사용했다. fresh backend에서 `npm ci
--ignore-scripts`를 실행했다.

`./mvnw -q -f backend/pom.xml -Dtest=ReturnRangeContractTest test`는
exit 0이다. Surefire는 3 tests, failures 0, errors 0, skipped 0을
보고했다. assertions는 반개방 구간 중복, 인도30의 정확한 반품10
계보 투영, 불확실 계보의 구간 생성 거부를 확인한다.

이 실행은 unit 계약 검증이다. 실제 PostgreSQL gateway와 공개
원본→관측→대조→canonical 경로, E1/C4 결합 실행은 아직 NOT_RUN이다.
실모델·BTP·실제 외부 제출·은행 실행도 NOT_RUN이다. 조용한 Maven
raw log는 출력이 없으며 Surefire 결과를 별도 보존한다.
