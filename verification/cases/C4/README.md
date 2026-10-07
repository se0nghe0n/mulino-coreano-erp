# 새 반품과 과거 정정 및 유효 의무 해소의 인수 계약

B2 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`에서 작성한
Step 2 테스트 계약이다. 제품 API/DB/host 실행은 NOT_RUN이다.
fixture 설치는 입력이며 업무 효과나 승인 성공을 미리 저장하지 않는다.
모든 action·assertion은 같은 한국어 scenario에 대응한다.

| subcase | 손계산과 책임 |
|---|---|
| return-not-correction | 과거 인도100과 새 반품20을 분리하며 중복 반품·과거80 덮어쓰기를 막는다 |
| corrected-delivery-98 | 인도100의 실제98 정정은 과거 판정과 현재 부족2의 인간 책임을 함께 남긴다 |
| resolved-no-resurrection-resolved | 정정으로 생긴 부족2를 RESOLVED 근거로 해소한 뒤 재대조해도 같은 의무는 부활하지 않는다 |
| resolved-no-resurrection-waived | 정정으로 생긴 부족2를 WAIVED 근거로 해소한 뒤 재대조해도 같은 의무는 부활하지 않는다 |

수량은 BOX이며 송장 차이는 EUR다. API와 독립 DB의 동일 snapshot을
대조한다. 원 행은 각 requested source의 완전한 query·parameters·mapping·
snapshot artifact와 함께 반환해야 한다. 미확인/상충은 0으로 바꾸지 않는다.
거부 감사와 접수 책임은 허용하되 금지된 원장·배분·정상 이행·외부
효과는 명시한 전후 원 행으로 비교한다. 과거 인도와 현재 반품을 빼서
한 값으로 만들지 않는다. 회수25를 다시 폐기해도 distinct 실물은25다.

| oracle | named observation과 assertion |
|---|---|
| C4.return-not-correction | historical-delivery → return-not-correction:historical-delivery-1, return-not-correction:historical-delivery-2 |
| C4.return-not-correction | new-return → return-not-correction:new-return-3, return-not-correction:new-return-4 |
| C4.return-not-correction | duplicate-return-material-effects → return-not-correction:duplicate-return-material-effects-5, return-not-correction:duplicate-return-material-effects-6, return-not-correction:duplicate-return-material-effects-7, return-not-correction:duplicate-return-material-effects-8 |
| C4.return-not-correction | return-overwrites-delivery-to80 → return-not-correction:return-overwrites-delivery-to80-9, return-not-correction:return-overwrites-delivery-to80-12 |
| C4.return-not-correction | physical-reference → return-not-correction:physical-reference-10, return-not-correction:physical-reference-11 |
| C4.corrected-delivery-98 | currently-supported-delivery → corrected-delivery-98:currently-supported-delivery-3, corrected-delivery-98:currently-supported-delivery-4 |
| C4.corrected-delivery-98 | current-unresolved-deficit → corrected-delivery-98:current-unresolved-deficit-5, corrected-delivery-98:current-unresolved-deficit-6 |
| C4.corrected-delivery-98 | historical-assessment → corrected-delivery-98:historical-assessment-1, corrected-delivery-98:historical-assessment-2, corrected-delivery-98:historical-assessment-7, corrected-delivery-98:historical-assessment-8, corrected-delivery-98:historical-assessment-9, corrected-delivery-98:historical-assessment-10 |
| C4.corrected-delivery-98 | deficit-owner → corrected-delivery-98:deficit-owner-11, corrected-delivery-98:deficit-owner-12, corrected-delivery-98:deficit-owner-13, corrected-delivery-98:deficit-owner-14, corrected-delivery-98:deficit-owner-15, corrected-delivery-98:deficit-owner-16, corrected-delivery-98:deficit-owner-17 |
| C4.resolved-debt-no-resurrection | new-unresolved-deficit → resolved-no-resurrection-resolved:new-unresolved-deficit-1, resolved-no-resurrection-resolved:new-unresolved-deficit-2, resolved-no-resurrection-waived:new-unresolved-deficit-1, resolved-no-resurrection-waived:new-unresolved-deficit-2 |
| C4.resolved-debt-no-resurrection | resolved-debt-resurrection → resolved-no-resurrection-resolved:resolved-debt-resurrection-3, resolved-no-resurrection-waived:resolved-debt-resurrection-3 |
| C4.resolved-debt-no-resurrection | valid-resolution → resolved-no-resurrection-resolved:valid-resolution-4, resolved-no-resurrection-resolved:valid-resolution-5, resolved-no-resurrection-resolved:valid-resolution-6, resolved-no-resurrection-waived:valid-resolution-4, resolved-no-resurrection-waived:valid-resolution-5, resolved-no-resurrection-waived:valid-resolution-6 |

검증 증거는 evidence/에 보존했다. 최종 전체 harness143개
(판매 표본·mutant13개 포함)는 PASS이며 JSON schema는 유효하다. 이 case의 실제 Gherkin
selector는 4개를 발견·시작했고 모두 NOT_IMPLEMENTED assertion으로
RED(exit1), scenario skip0이다. 실제 제품 profile은 NOT_RUN(exit2)이다.
전체 명령·version·집계는 E1/evidence/sales-preparation-summary.json에 있다.

## Step2 adversarial review 보완

기존 최종 상태만으로 중간 명령의 실행을 인정하지 않도록 보완했다.
검증은 고정 관찰 표본을 사용하는 AssertionEngine SELFTEST와 실제
제품 API/DB 실행을 구별한다. 제품 인수는 계속 NOT_RUN이다.

RESOLVED/WAIVED 각각 기존6개 assertion을 유지하고8개를 추가했다.
재확인 명령의 APPLIED, 새 occurrence ID와 revision, 원 사건·직전
revision의 supersedes 연결, 현재 canonical98BOX, source version2의
별도 증거 ID, 적용 command·transaction ID를 대조한다. 기존98
revision과 과거 판정은 보존하고 동일 의무 ID·root·owner·해소 상태를
유지한다. 실제98을100으로 되돌리는 가짜 정정은 만들지 않는다.
새 증거는 synthetic source revision 입력이며 재확인 성공을 seed하지
않는다. 기존 catalog oracle와 named observation은 그대로다.

추가 assertion ID는 각 resolved-no-resurrection subcase의
reconfirm-applied-7, reconfirm-new-occurrence-8,
reconfirm-new-revision-9, reconfirm-baseline-98-10,
reconfirm-canonical-revision-11, reconfirm-effective-98-12,
reconfirm-old-revision-13, reconfirm-same-responsibility-id-14다.
모두 valid-resolution 관찰을 보강한다.

보완 검증은 JDK21 javac와 JUnit Platform/Jupiter6.1.2 직접 launcher로
실행했다. OutcomeEffectAssertionSelfTest의7개 method가 발견·시작·
성공했고 skip/FAIL0이다. 전체 정상 표본과42개 거부/no-op·출처·
수량·transaction·책임 mutant를 검증했다. Maven queue를 쓰지 않은
독립 SELFTEST이며 통합 harness check는 coordinator가 수행한다.
정확한 argv·버전·source hash·log는
C4/evidence/review-outcomes/selftest-commands.json에 보존했다.
