# T01 수량·책임·두 진입점

품목 P·LOT L의 구별된 A60과 B40이 W에 있다. 현재 보유는
60+40=100 BOX다. A60의 QC·기관·처분·고객 조건 근거를 가상 정책으로
고정하고 B40에는 별도 QC 제한을 둔다. 판매 적격은60 BOX다.
유효한 두 canonical 수령은 O1의 누적 도착100에 기여한다.
물량·상태·누적 목표를 같은 값 하나로 저장하지 않는다.

명사와 동사 조회는 서버 발급 ID·실제 snapshot token·scope·asOf·knownAt을
공유한다. 응답 간 일치뿐 아니라100/60/100의 고정 기대값과 독립 DB
leaf 원행·물리 identity·의무 tuple을 대조한다. 다섯 질문 각각에
API 응답, 근거, 인간 owner, nextAction, nextCheckAt을 연결한다.

LOT→A60→S1→C는 영향 후보 관계다. 이 baseline은 이미 발생한 출고나
인도를 만들지 않는다. 추적 후보를 오염 확정으로 표시하지 않는다.

제외 기능은 structureIntent에서 명시적으로 거부하며 승인·업무·원장·
배분·외부 outbox의 전후 원행은 같다. REJECTED command metadata와
허용된 조회/거부 감사는 업무 효과와 구별한다. 제조/BOM/B2C/GL/세금
신고/은행 이체를 유사한 capability로 대체하지 않는다.

정책·품목·근거는 synthetic fixture다. 실제 규제 적합성이나 제품
실행 PASS의 증거가 아니다. 미구현 adapter 경로는 NOT_RUN이다.
