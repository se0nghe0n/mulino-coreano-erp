# S4 회수 command 계약

회수 후보를 오염 확정으로 오인하거나 실제 회수25와 같은 물량의 폐기25를
처리50으로 합산하지 않도록 versioned scope와 물리 범위를 분리한다.
현재 사용자 Step3과 S4는 진행 중이다. OWN baseline은
`1387024a6c5176d3f663f41e30dd22c1b268faef`다.

- `openInvestigation`은 원천 Events 근거와 LOT/segment의 정확한 범위를
  받는다. CANDIDATE 상태와 독립 RECALL_INVESTIGATION SELL/PICK/DISPATCH
  제한을 기록한다. `validUntil=9999-12-31T23:59:59Z`는 조사 보류가 검토
  기한에 자동 해소되지 않도록 한 저장 표현이다. 승인 유효기간이 아니다.
  QC 제한은 해제하지 않는다.
- `proposeRecall`은 investigationId와 startQuantity/quantity/unit/reason을
  받아 immutable scopeId/version/hash를 만든다. scope 변경은 현재 HUMAN
  approveRecall 관리 권한을 요구하고 이전 승인을 무효화한다. 조사 범위 중
  제외한 물량은 RECALL_EXCLUDED_SCOPE 인간 책임으로 보존한다.
- `approveRecall`은 scopeId/hash/decision/validUntil을 받는다. ManagementAuthorities
  approveRecall과 현재 grant가 있어야 하며 HUMAN 승인만 허용한다. 순서는
  같은 기록 시각에서도 단조 revision으로 구별한다. 승인 자체는 실제 통지나
  기관 제출 증거가 아니다.
- `recordRecovery`의 RECORD는 kind RECOVERED/DISPOSED/SAFE/CONSUMED_LOST/
  EXCEPTION을 구별한다. canonicalOccurrenceId/actualEventId/scopeId/hash/
  approvalId/rootSegmentId/startQuantity/quantity/unit/currentPlaceId가 필수다.
  물리 이동/폐기는 sourceSegmentId를 참조한다. 독립 QC 범위는 현재 별도
  처분 권한 없이 폐기할 수 없다. RECOVERED는 INTERNAL_STORAGE로만 이동한다.
  returnId가 있으면 exact immutable 반환 receipt를 대조해 이동을 반복하지 않는다.
- `recordRecallNotice`는 같은 exact 범위의 RECALL_NOTICE 증거만 기록한다.
- `closeRecall`은 exact ADMIN 승인과 RECALL_CLOSURE canonical 증거,
  현재 partitionHash를 받는다. SAFE/DISPOSED/CONSUMED_LOST/EXCEPTION은
  상호 배타적이다. UNKNOWN이 남으면 종료하지 않는다. EXCEPTION에는
  원문 근거와 사유, 현재 OPEN인 HUMAN owner/supervisor/nextAction/nextCheck
  잔여 책임이 필수다. 회수 case 종료는 Work 종료와 독립이다.

원문 JSON은 actualEventId, scopeId/version/hash, rootSegmentId, itemId,
lotId, startQuantity, quantity, unit, eventKind, occurredAt, currentPlaceId와
동작별 sourceSegmentId/returnId/reason/partitionHash를 담는다. 원문 bytes와
Events payload를 전체 대조한 뒤 원본→claim→match→canonical 경로를 따른다.
DB에 검증 완료 seed를 넣는 것은 public 증거 경로 인수가 아니다.

현재 원문/인가/부분 범위/중복/미확인 종료/예외 책임/rollback의 실제 PG
검증은 아직 NOT_RUN이다. tests 완료 후 별도 실행 증거를 기록한다.
