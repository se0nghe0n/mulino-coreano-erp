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

외부 결과의 대조는 EXTERNAL_RESULT 사건에 한정한다. runtime이 소유한
ExternalOperationScopePort가 실제 조직별 operation UUID와 Work를 확인한다.
인간 대조 담당이 제출한 원본 JSON과 Event payload의 operation·확정 결과가
같아야 한다. 이 사실의 canonical은 재고 segment가 아니라 operation UUID를
범위로 쓰며 물량은 만들지 않는다. 후속 recordExternalReconciliation은
ExternalResultEvidenceGuard로 같은 원본·검증·정확한 결과를 다시 확인한다.
미확인 외부 결과를 확정 성공으로 꾸미거나 새 외부 발행을 만들지 않는다.


일반 gateway는 증거 접수와 대조의 성공을 APPLIED로 commit한다. 원래
RECORDED·EVIDENCE_CONFLICT·VERIFIED_RECORD_ONLY 상태는 evidenceStatus에
별도로 둔다. 상충 접수도 담당자·감독자·다음 조치를 보존하며 실물 효과는
0이다. gateway 성공 허용 목록에 증거 상태를 추가하지 않는다.

RESPONSE_COMPLETED는 실제 의무 root가 만들어진 뒤 작성한 원본 JSON과
Event payload에서 dutyRootId·startQuantity·quantity·unit을 대조한다.
검증된 원본 hash·source policy·현재 verification을 다시 확인하고 불변
CompletionCoverages에 저장한다. 공개 요청의 범위 힌트로 완료 구간을
만들지 않는다. 실제 root·Work assignment·physicalScopeId·단위를 확인한다.
CompletionBindings는 이 검증 행의 정확한 범위와 verification을 참조한다.
50의 검증 구간은 같은 50 leaf만 완료하고, 100의 구간은 겹치지 않는 두
50 leaf에 총100까지 credit을 준다. 같은 구간의 중복 credit은 거부한다.

추가 gateway 검증은 PostgreSQL 18.6과 실제 파일 저장소에서 25건을
통과했다. 접수·첨부·정정의 command commit·audit·불변 원본과 partial/full
완료 credit을 확인했다. PolicyCommandGuard만 로컬 승인 fixture로 바꿨다.
HTTP 서명·실제 policy coupling의 통합 결과는 root의 결합 실행에서 확인한다.
