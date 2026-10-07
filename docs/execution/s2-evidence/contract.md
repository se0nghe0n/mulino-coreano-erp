# S2 증거 명령의 경계

원천 접수 성공만으로 실물 사실이나 재고 효과를 만들지 않는다.
`recordActivity`는 불변 Event와 Inbox를 기록하고, 같은 원천 key의 같은
hash를 replay하며 다른 hash를 충돌로 보존한다. source profile의 인간
intake owner·supervisor와 다음 행동·확인 시각이 없으면 활성화하지 않는다.

`attachEvidence`는 공통 RECORD envelope의 typed metadata와 base64 원본을
받는다. caller path나 URI를 가져오지 않는다. 원본 hash와 16MiB 한도를
검사하고, 기존 private 파일 권한·rollback cleanup을 유지한다.

`matchSourceIdentity`와 `resolveEvidenceConflict`는 지정된 접수 담당이나
감독자의 권한 있는 대조 결정을 불변 Reconciliation으로 남긴다.
동일성·수량·원본·policy가 부족하면 UNVERIFIED, 같은 source key의 상충
hash는 CONFLICT다. 임의 source 우선순위로 해소하지 않는다. 각각의
미확정 결정은 intake owner·nextAction·nextCheck를 보존한다.

`linkCanonicalOccurrence`는 MATCHED 결정의 원본·현재 원천 충돌·policy와
동일한 식별 segment의 scope·수량·단위·시점을 commit 경계에서 재검사한다.
서로 다른 문서가 같은 실물 사건을 뒷받침하면 canonical 하나에 연결한다.
공개 slots는 검증 boolean을 허용하지 않는다. S2의 일반 증거 명령은
QuantityMovement·예약·배분을 쓰지 않는다. 실제 수령·인도 효과는 이후
도메인 명령의 원장·배분 guard를 통과해야 한다.

`correctEvidence`는 EVENT·DOCUMENT·CLAIM typed selector의 과거 원본을
바꾸지 않고 supersedes revision을 추가한다.
필수 EvidenceCorrectionImpact port가 같은 transaction에서 영향받는 Work와
Goal의 현재 판정·책임을 처리한다. 이 port가 없으면 효과0 보류다.
과거 assessment와 stock movement를 수정하지 않는다.

C4의 인도100→실제98 정정·유효 의무2와 반품20은 S4 거래 경로에서 별도로
검증해야 한다. S2 일반 기록을 실제 인도 원장 효과로 표시하지 않는다.
