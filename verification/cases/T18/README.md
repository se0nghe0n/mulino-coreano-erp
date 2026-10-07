# 반품·양방향 trace·ADMIN 회수와 종료 대조의 인수 계약

B2 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`에서 작성한
Step 2 테스트 계약이다. 제품 API/DB/host 실행은 NOT_RUN이다.
fixture 설치는 입력이며 업무 효과나 승인 성공을 미리 저장하지 않는다.
모든 action·assertion은 같은 한국어 scenario에 대응한다.

| subcase | 손계산과 책임 |
|---|---|
| return-new-receipt | 인도60 뒤 반품20을 새 수령·보류로 연결하고 같은 실물 재접수 효과를 막는다 |
| unidentified-return-provisional | LOT 미식별 반품20의 임시 접수는 가용0과 대조 책임을 유지한다 |
| trace-and-recall-decisions | 양방향 LOT trace와 후보60·승인50·기관 통지 및 scope 변경 재승인을 확인한다 |
| recall-closure-accounting | ADMIN scope50의 회수25·동일 실물 폐기25와 미확인25는 처리25다 |

수량은 BOX이며 송장 차이는 EUR다. API와 독립 DB의 동일 snapshot을
대조한다. 원 행은 각 requested source의 완전한 query·parameters·mapping·
snapshot artifact와 함께 반환해야 한다. 미확인/상충은 0으로 바꾸지 않는다.
거부 감사와 접수 책임은 허용하되 금지된 원장·배분·정상 이행·외부
효과는 명시한 전후 원 행으로 비교한다. 과거 인도와 현재 반품을 빼서
한 값으로 만들지 않는다. 회수25를 다시 폐기해도 distinct 실물은25다.

| oracle | named observation과 assertion |
|---|---|
| T18.return-new-receipt | historical-delivery → return-new-receipt:historical-delivery-1, return-new-receipt:historical-delivery-2 |
| T18.return-new-receipt | return-received → return-new-receipt:return-received-3, return-new-receipt:return-received-4 |
| T18.return-new-receipt | return-eligible → return-new-receipt:return-eligible-5, return-new-receipt:return-eligible-6, unidentified-return-provisional:return-eligible-3, unidentified-return-provisional:return-eligible-4 |
| T18.return-new-receipt | duplicate-return-effects → return-new-receipt:duplicate-return-effects-7, return-new-receipt:duplicate-return-effects-8, return-new-receipt:duplicate-return-effects-9, return-new-receipt:duplicate-return-effects-10, return-new-receipt:duplicate-return-effects-11, return-new-receipt:duplicate-return-effects-12 |
| T18.return-new-receipt | return-added-purchase-contribution → return-new-receipt:return-added-purchase-contribution-13, return-new-receipt:return-added-purchase-contribution-14 |
| T18.return-new-receipt | return-followup → return-new-receipt:return-followup-15, return-new-receipt:return-followup-16, unidentified-return-provisional:return-followup-1, unidentified-return-provisional:return-followup-2, unidentified-return-provisional:return-followup-5, unidentified-return-provisional:return-followup-6, unidentified-return-provisional:return-followup-7, unidentified-return-provisional:return-followup-8, unidentified-return-provisional:return-followup-9, unidentified-return-provisional:return-followup-10, unidentified-return-provisional:return-followup-11 |
| T18.trace-and-recall-decisions | bidirectional-trace → trace-and-recall-decisions:bidirectional-trace-6, trace-and-recall-decisions:bidirectional-trace-7, trace-and-recall-decisions:bidirectional-trace-8, trace-and-recall-decisions:bidirectional-trace-9, trace-and-recall-decisions:bidirectional-trace-10 |
| T18.trace-and-recall-decisions | unauthorized-recall-decision → trace-and-recall-decisions:unauthorized-recall-decision-1, trace-and-recall-decisions:unauthorized-recall-decision-2, trace-and-recall-decisions:unauthorized-recall-decision-3, trace-and-recall-decisions:unauthorized-recall-decision-4, trace-and-recall-decisions:unauthorized-recall-decision-5 |
| T18.trace-and-recall-decisions | approval-notice → trace-and-recall-decisions:approval-notice-11, trace-and-recall-decisions:approval-notice-12, trace-and-recall-decisions:approval-notice-13, trace-and-recall-decisions:approval-notice-14, trace-and-recall-decisions:approval-notice-15 |
| T18.recall-closure-accounting | recovery-distinct → recall-closure-accounting:recovery-distinct-1, recall-closure-accounting:recovery-distinct-2 |
| T18.recall-closure-accounting | processed-distinct → recall-closure-accounting:processed-distinct-3, recall-closure-accounting:processed-distinct-4 |
| T18.recall-closure-accounting | unknown → recall-closure-accounting:unknown-5, recall-closure-accounting:unknown-6 |
| T18.recall-closure-accounting | false-closure → recall-closure-accounting:false-closure-7, recall-closure-accounting:false-closure-8, recall-closure-accounting:false-closure-9, recall-closure-accounting:false-closure-10, recall-closure-accounting:false-closure-11 |
| T18.recall-closure-accounting | exclusive-endstates → recall-closure-accounting:exclusive-endstates-12, recall-closure-accounting:exclusive-endstates-13, recall-closure-accounting:exclusive-endstates-14, recall-closure-accounting:exclusive-endstates-15, recall-closure-accounting:exclusive-endstates-16, recall-closure-accounting:exclusive-endstates-17, recall-closure-accounting:exclusive-endstates-18, recall-closure-accounting:exclusive-endstates-19, recall-closure-accounting:exclusive-endstates-20, recall-closure-accounting:exclusive-endstates-21, recall-closure-accounting:exclusive-endstates-22, recall-closure-accounting:exclusive-endstates-23, recall-closure-accounting:exclusive-endstates-24, recall-closure-accounting:exclusive-endstates-25, recall-closure-accounting:exclusive-endstates-26, recall-closure-accounting:exclusive-endstates-27 |

검증 증거는 evidence/에 보존했다. 최종 전체 harness143개
(판매 표본·mutant13개 포함)는 PASS이며 JSON schema는 유효하다. 이 case의 실제 Gherkin
selector는 4개를 발견·시작했고 모두 NOT_IMPLEMENTED assertion으로
RED(exit1), scenario skip0이다. 실제 제품 profile은 NOT_RUN(exit2)이다.
전체 명령·version·집계는 E1/evidence/sales-preparation-summary.json에 있다.
