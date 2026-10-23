# 2026-10-23 canonical 재검증의 단일 실물 사실

같은 canonical의 정상 검증 행이 둘이라는 이유로 인도 사실을 숨기면
안 된다. 현재 제품은 같은 reconciliation을 다른 command key로 다시
link해 같은 canonical에 검증 근거를 추가할 수 있다.

coordinator가 실제 backend gateway와 PostgreSQL에서 다음을 확인했다.
`repeatedCanonicalVerificationStillConfirmsOnePhysicalDelivery` 한 건이
실패·오류·SKIP 없이 PASS했고 Maven exit는0이다.

- link 두 번의 canonical ID가 같고 VERIFIED 행은2개다.
- canonical은1개이며 실제20/기여20 인도와 DELIVERY 이동은 각각1개다.
- 출고30 중 운송 잔량10, 고객 위치20을 유지한다.

backend 제품 코드는 변경하지 않고 회귀 test를 추가했다.
`.local/resume-20261023/canonical-reverification-product/`에 명령·JUnit·
source/class hash와 자체 container 부재 증거를 남겼다. 공유 test class의
임시 blob 정리를 확인한 것으로 표시하지 않는다.

검토 중인 observer의 검증 행 수1개 제약은 아직 통합하지 않았다.
근거를 모두 보존하면서 실물량은 중복 계산하지 않도록 보완한다.
합성 source-row 검사와 이 제품 검사를 구별하고, 원래 T17 다섯
assertion의 actual 인수는 NOT_RUN으로 유지한다. Task와 Step은 OPEN이다.
