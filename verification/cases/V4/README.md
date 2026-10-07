# 대체 쓰기 경로와 내부 이동의 테스트 계약

직접·nested·atomic batch·projection·MCP·worker·blob·management
각 경로에서 물량·배분·업무·승인·거래·증거·identity·정의·정책·
운영 family의 인가를 검사한다. 이 route inventory는 새 구현이
공개할 모든 효과 class를 인수하기 위한 계약이며 존재하는 endpoint
주장이 아니다. 현재 없는 경로는 NOT_IMPLEMENTED/NOT_RUN이다.

각 거부의 전후 DB scope를 보존한 뒤 권한 있는 same-route 입력과
COMMITTED 원행으로 진입점을 대조한다. raw core CRUD는 공개하지
않으므로 실제404와 업무 원행 불변을 확인한다. 한 atomic changeset의
허용 RECORD와 금지 출고는 허용 부분까지 rollback해야 한다.

내부 W→W2 이동의 실제20은 허용 대조다. 고객 장소 ID 교체, action
이름 변경, raw primitive, 반품/폐기 effect class 변조는 출고·정상
주문 이행·반품·폐기를 생성하지 못한다. 이미 발생한 무허가 이동은
별도 RECORD 주체/원천/실물 scope의 claim과 대조 책임으로 남긴다.
해당 claim은 새로운 출고 허가나 정상 Goal 충족을 만들지 않는다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.
