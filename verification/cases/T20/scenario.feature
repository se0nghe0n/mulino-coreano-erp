# language: ko
@T20 @D20 @contract-red
기능: 구조화 intent·실제 wire·host skill 경계
  시나리오: purchaseDraftNoLot의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "purchaseDraftNoLot"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "intent-kind" assertion으로 "intent-kind: /response/intent/intentKind의 실제 equals 기대값은 'COMMAND'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "definition" assertion으로 "definition: /response/intent/definitionVersion의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "capability" assertion으로 "capability: /response/intent/capabilityId의 실제 equals 기대값은 'createDraft'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "typed-subject" assertion으로 "typed-subject: /response/intent/subjectRefs의 실제 equals 기대값은 [{'type': 'TradeItem', 'id': {'$alias': 'P'}}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "all-slots-and-provenance" assertion으로 "all-slots-and-provenance: /response/intent/slots의 실제 equals 기대값은 {'quantity': {'value': '100', 'unit': 'BOX', 'provenance': 'USER'}, 'dueAt': {'value': '2026-10-09T09:00:00Z', 'provenance': 'USER'}, 'endpoint': {'value': 'ARRIVED', 'provenance': 'CONTEXT'}, 'quantityMode': {'value': 'CUMULATIVE_EVENT', 'provenance': 'APPROVED_DEFAULT'}, 'destination': {'value': {'$alias': 'W'}, 'type': 'Place', 'provenance': 'USER'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conditions" assertion으로 "conditions: /response/intent/conditions의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "evidence-refs" assertion으로 "evidence-refs: /response/intent/evidenceRefs의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "lot-not-stage-required" assertion으로 "lot-not-stage-required: 실제 관찰한 부모 object에서 /response/intent/slots/lot가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
  시나리오: locationFridayTypeError의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "locationFridayTypeError"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "place-type-code" assertion으로 "place-type-code: /response/error/code의 실제 equals 기대값은 'TYPE_INVALID'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: ambiguousFriday의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "ambiguousFriday"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "conversation-preserved" assertion으로 "conversation-preserved: /response/conversationRequestId의 실제 equals 기대값은 'T20-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-canonical-before-confirmation" assertion으로 "no-canonical-before-confirmation: 실제 관찰한 부모 object에서 /response/canonicalHash가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
  시나리오: ambiguousSameName의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "ambiguousSameName"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "conversation-preserved" assertion으로 "conversation-preserved: /response/conversationRequestId의 실제 equals 기대값은 'T20-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-canonical-before-confirmation" assertion으로 "no-canonical-before-confirmation: 실제 관찰한 부모 object에서 /response/canonicalHash가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
  시나리오: missingActiveSlot의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "missingActiveSlot"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "conversation-preserved" assertion으로 "conversation-preserved: /response/conversationRequestId의 실제 equals 기대값은 'T20-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-canonical-before-confirmation" assertion으로 "no-canonical-before-confirmation: 실제 관찰한 부모 object에서 /response/canonicalHash가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
  시나리오: queryKind의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "queryKind"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "intent-kind" assertion으로 "intent-kind: /response/intent/intentKind의 실제 equals 기대값은 'QUERY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "definition" assertion으로 "definition: /response/intent/definitionVersion의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "capability" assertion으로 "capability: /response/intent/capabilityId의 실제 equals 기대값은 'getInventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "typed-subject" assertion으로 "typed-subject: /response/intent/subjectRefs의 실제 equals 기대값은 [{'type': 'TradeItem', 'id': {'$alias': 'P'}}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "all-slots-and-provenance" assertion으로 "all-slots-and-provenance: /response/intent/slots의 실제 equals 기대값은 {'quantity': {'value': '100', 'unit': 'BOX', 'provenance': 'USER'}, 'dueAt': {'value': '2026-10-09T09:00:00Z', 'provenance': 'USER'}, 'endpoint': {'value': 'ARRIVED', 'provenance': 'CONTEXT'}, 'quantityMode': {'value': 'CUMULATIVE_EVENT', 'provenance': 'APPROVED_DEFAULT'}, 'destination': {'value': {'$alias': 'W'}, 'type': 'Place', 'provenance': 'USER'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conditions" assertion으로 "conditions: /response/intent/conditions의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "evidence-refs" assertion으로 "evidence-refs: /response/intent/evidenceRefs의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: recordKind의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "recordKind"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "intent-kind" assertion으로 "intent-kind: /response/intent/intentKind의 실제 equals 기대값은 'RECORD'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "definition" assertion으로 "definition: /response/intent/definitionVersion의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "capability" assertion으로 "capability: /response/intent/capabilityId의 실제 equals 기대값은 'recordActivity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "typed-subject" assertion으로 "typed-subject: /response/intent/subjectRefs의 실제 equals 기대값은 [{'type': 'TradeItem', 'id': {'$alias': 'P'}}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "all-slots-and-provenance" assertion으로 "all-slots-and-provenance: /response/intent/slots의 실제 equals 기대값은 {'quantity': {'value': '100', 'unit': 'BOX', 'provenance': 'USER'}, 'dueAt': {'value': '2026-10-09T09:00:00Z', 'provenance': 'USER'}, 'endpoint': {'value': 'ARRIVED', 'provenance': 'CONTEXT'}, 'quantityMode': {'value': 'CUMULATIVE_EVENT', 'provenance': 'APPROVED_DEFAULT'}, 'destination': {'value': {'$alias': 'W'}, 'type': 'Place', 'provenance': 'USER'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conditions" assertion으로 "conditions: /response/intent/conditions의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "evidence-refs" assertion으로 "evidence-refs: /response/intent/evidenceRefs의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: commandKind의 구조화·slot·provenance 검증
    먼저 사례 파일 "verification/cases/T20/case.json"의 "commandKind"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "structured" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "structured-outcome" assertion으로 "structured-outcome: /response/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "intent-kind" assertion으로 "intent-kind: /response/intent/intentKind의 실제 equals 기대값은 'COMMAND'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "definition" assertion으로 "definition: /response/intent/definitionVersion의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "capability" assertion으로 "capability: /response/intent/capabilityId의 실제 equals 기대값은 'createDraft'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "typed-subject" assertion으로 "typed-subject: /response/intent/subjectRefs의 실제 equals 기대값은 [{'type': 'TradeItem', 'id': {'$alias': 'P'}}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "all-slots-and-provenance" assertion으로 "all-slots-and-provenance: /response/intent/slots의 실제 equals 기대값은 {'quantity': {'value': '100', 'unit': 'BOX', 'provenance': 'USER'}, 'dueAt': {'value': '2026-10-09T09:00:00Z', 'provenance': 'USER'}, 'endpoint': {'value': 'ARRIVED', 'provenance': 'CONTEXT'}, 'quantityMode': {'value': 'CUMULATIVE_EVENT', 'provenance': 'APPROVED_DEFAULT'}, 'destination': {'value': {'$alias': 'W'}, 'type': 'Place', 'provenance': 'USER'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conditions" assertion으로 "conditions: /response/intent/conditions의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "evidence-refs" assertion으로 "evidence-refs: /response/intent/evidenceRefs의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 입력 보완 뒤 새 hash·revision·승인 범위·effect key
    먼저 사례 파일 "verification/cases/T20/case.json"의 "destination-canonical-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "draft" 행동을 수행한다
    만일 "writer" 역할이 "supplement" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "writer" 역할이 "revise" 행동을 수행한다
    만일 "writer" 역할이 "stale-approval" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "draft-input" assertion으로 "draft-input: /response/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "supplement-structured" assertion으로 "supplement-structured: /response/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conversation-same" assertion으로 "conversation-same: /response/conversationRequestId의 실제 equals 기대값은 'T20-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "hash-revised" assertion으로 "hash-revised: /response/proposalHash의 실제 notEquals 기대값은 {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalHash'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "revision-revised" assertion으로 "revision-revised: /response/proposalRevision의 실제 notEquals 기대값은 {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalRevision'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stale-approval-refused" assertion으로 "stale-approval-refused: /response/outcome의 실제 equals 기대값은 'WAITING_APPROVAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-effect-key" assertion으로 "new-effect-key: /response/commandIdempotencyKey의 실제 equals 기대값은 'T20-effect-revision-2'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-dispatch-outbox" assertion으로 "no-dispatch-outbox: /data/rawRows/outbox의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "physical-stays100" assertion으로 "physical-stays100: db-after의 /data/rawRows/segments와 db-before의 /data/rawRows/segments를 같은 scope에서 exact 대조한다."를 확인한다
  시나리오: raw stateless MCP discover
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-discover"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 200다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "protocol-result-version" assertion으로 "protocol-result-version: /response/body/result/protocolVersion의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stateless-handshake" assertion으로 "stateless-handshake: /data/transcript/clientMethods의 실제 equals 기대값은 ['server/discover']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "server-requests" assertion으로 "server-requests: /data/transcript/serverRequestMethods의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-schema-registry" assertion으로 "tool-schema-registry: /response/body/result/tools의 실제 name는 고정한 113개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP method-mismatch
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-method-mismatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP name-mismatch
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-name-mismatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Name" assertion으로 "wire-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchQuantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP version-mismatch
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-version-mismatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2025-11-25'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP unsupported-version
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-unsupported-version"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '1900-01-01'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '1900-01-01', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP missing-meta
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-missing-meta"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-no-meta" assertion으로 "wire-raw-no-meta: 실제 관찰한 부모 object에서 /data/transcript/request/body/params/_meta가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
  시나리오: raw stateless MCP missing-client-info
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-missing-client-info"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP missing-capabilities
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-missing-capabilities"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP unauthenticated
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-unauthenticated"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "anonymous" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 401다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'AUTHENTICATION'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP bad-origin
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-bad-origin"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 403다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'ORIGIN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP initialize-not-required
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-initialize-not-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 200다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "protocol-result-version" assertion으로 "protocol-result-version: /response/body/result/protocolVersion의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stateless-handshake" assertion으로 "stateless-handshake: /data/transcript/clientMethods의 실제 equals 기대값은 ['server/discover']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "server-requests" assertion으로 "server-requests: /data/transcript/serverRequestMethods의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-schema-registry" assertion으로 "tool-schema-registry: /response/body/result/tools의 실제 name는 고정한 113개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP server-request-not-required
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-server-request-not-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 200다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "protocol-result-version" assertion으로 "protocol-result-version: /response/body/result/protocolVersion의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stateless-handshake" assertion으로 "stateless-handshake: /data/transcript/clientMethods의 실제 equals 기대값은 ['server/discover']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "server-requests" assertion으로 "server-requests: /data/transcript/serverRequestMethods의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-schema-registry" assertion으로 "tool-schema-registry: /response/body/result/tools의 실제 name는 고정한 113개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP stdio
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-stdio"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-transport" assertion으로 "wire-transport: /response/transport의 실제 equals 기대값은 'stdio'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "protocol-result-version" assertion으로 "protocol-result-version: /response/body/result/protocolVersion의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stateless-handshake" assertion으로 "stateless-handshake: /data/transcript/clientMethods의 실제 equals 기대값은 ['server/discover']다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "server-requests" assertion으로 "server-requests: /data/transcript/serverRequestMethods의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-schema-registry" assertion으로 "tool-schema-registry: /response/body/result/tools의 실제 name는 고정한 113개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: raw stateless MCP old-protocol
    먼저 사례 파일 "verification/cases/T20/case.json"의 "wire-old-protocol"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reader" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "wire-http" assertion으로 "wire-http: /response/httpStatus의 실제 equals 기대값은 400다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-version" assertion으로 "jsonrpc-version: /response/body/jsonrpc의 실제 equals 기대값은 '2.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "jsonrpc-id" assertion으로 "jsonrpc-id: /response/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-error-class" assertion으로 "wire-error-class: /response/body/error/data/category의 실제 equals 기대값은 'PROTOCOL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-tool-result" assertion으로 "no-tool-result: 실제 관찰한 부모 object에서 /response/body/result가 없음을 확인한다. null·미관찰을 없음으로 바꾸지 않는다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-MCP-Protocol-Version" assertion으로 "wire-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2025-11-25'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-Mcp-Method" assertion으로 "wire-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'server/discover'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2025-11-25', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: stdio도 현재 READ grant로 쓰기를 거부한다
    먼저 사례 파일 "verification/cases/T20/case.json"의 "stdio-readonly-write-denied"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "wire" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "stdio-readonly-outcome" assertion으로 "stdio-readonly-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stdio-forbidden" assertion으로 "stdio-forbidden: /response/body/result/structuredContent/error/code의 실제 equals 기대값은 'FORBIDDEN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "stdio-transport" assertion으로 "stdio-transport: /response/transport의 실제 equals 기대값은 'stdio'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-raw-method" assertion으로 "wire-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-jsonrpc-id" assertion으로 "wire-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-raw-meta" assertion으로 "wire-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API와 실제 MCP tool 조회의 같은 core schema·snapshot
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-read-parity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "api-result" 행동을 수행한다
    만일 "reader" 역할이 "wire-result" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "same-core-schema" assertion으로 "same-core-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "schema-v1" assertion으로 "schema-v1: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-data" assertion으로 "same-data: wire-result의 /response/body/result/structuredContent/data와 api-result의 /response/data를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-db-snapshot" assertion으로 "same-db-snapshot: wire-result의 /response/body/result/structuredContent/snapshotRevision와 api-result의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'getInventory'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 NEEDS_INPUT mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-needs_input"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "api-result" 행동을 수행한다
    만일 "writer" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 WAITING_APPROVAL mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-waiting_approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "api-result" 행동을 수행한다
    만일 "writer" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'WAITING_APPROVAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'WAITING_APPROVAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 CONFLICT mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "writer" 역할이 "first" 행동을 수행한다
    만일 "writer" 역할이 "cross-route-replay" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "api-result" 행동을 수행한다
    만일 "writer" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'CONFLICT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'CONFLICT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-work-id" assertion으로 "cross-route-work-id: cross-route-replay의 /response/body/result/structuredContent/workId와 first의 /response/workId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "shared-namespace-effect-once" assertion으로 "shared-namespace-effect-once: /data/rawRows/works의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "changed-payload-conflict" assertion으로 "changed-payload-conflict: /response/error/code의 실제 equals 기대값은 'IDEMPOTENCY_CONFLICT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-method" assertion으로 "cross-route-replay-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-jsonrpc-id" assertion으로 "cross-route-replay-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'cross-route-replay'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-MCP-Protocol-Version" assertion으로 "cross-route-replay-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-Mcp-Method" assertion으로 "cross-route-replay-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-Mcp-Name" assertion으로 "cross-route-replay-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'createDraft'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "cross-route-replay-raw-meta" assertion으로 "cross-route-replay-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'createDraft'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 REJECTED mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-rejected"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "api-result" 행동을 수행한다
    만일 "writer" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'reserveQuantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 FORBIDDEN mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-forbidden"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "api-result" 행동을 수행한다
    만일 "readAgent" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "forbidden-code" assertion으로 "forbidden-code: /response/error/code의 실제 equals 기대값은 'FORBIDDEN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'reserveQuantity'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: API/MCP 도메인 ACCEPTED_PENDING_EXTERNAL mapping과 동일 command namespace
    먼저 사례 파일 "verification/cases/T20/case.json"의 "domain-accepted_pending_external"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "시스템" 역할이 "external-timeout" 행동을 수행한다
    만일 "reader" 역할이 "pre-domain" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "api-result" 행동을 수행한다
    만일 "writer" 역할이 "wire-result" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-outcome" assertion으로 "api-outcome: /response/outcome의 실제 equals 기대값은 'ACCEPTED_PENDING_EXTERNAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-domain-outcome" assertion으로 "wire-domain-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'ACCEPTED_PENDING_EXTERNAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "same-schema" assertion으로 "same-schema: wire-result의 /response/body/result/structuredContent/schemaVersion와 api-result의 /response/schemaVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "fixed-schema" assertion으로 "fixed-schema: /response/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "outbox-once" assertion으로 "outbox-once: /data/rawRows/outbox의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "external-unknown" assertion으로 "external-unknown: /response/externalState의 실제 equals 기대값은 'UNKNOWN_EXTERNAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-method" assertion으로 "wire-result-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-jsonrpc-id" assertion으로 "wire-result-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'wire-result'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-MCP-Protocol-Version" assertion으로 "wire-result-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Method" assertion으로 "wire-result-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-Mcp-Name" assertion으로 "wire-result-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wire-result-raw-meta" assertion으로 "wire-result-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state continuation와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-continuation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "input-destination-applied" assertion으로 "input-destination-applied: /response/body/result/structuredContent/intent/slots/destination/value의 실제 equals 기대값은 {'$alias': 'W'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "conversation-kept" assertion으로 "conversation-kept: /response/body/result/structuredContent/conversationRequestId의 실제 equals 기대값은 'T20-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state tampered-state와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-tampered-state"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state expired-state와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-expired-state"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "시스템" 역할이 "expire" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state other-principal와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-other-principal"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "otherPrincipal" 역할이 "continued" 행동을 수행한다
    만일 "otherPrincipal" 역할이 "own-issued" 행동을 수행한다
    만일 "otherPrincipal" 역할이 "own-continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "wrong-principal-code" assertion으로 "wrong-principal-code: /response/body/result/structuredContent/error/code의 실제 equals 기대값은 'REQUEST_STATE_PRINCIPAL_MISMATCH'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-input-required" assertion으로 "own-issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-state-countercall-structured" assertion으로 "own-state-countercall-structured: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'STRUCTURED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-state-destination-applied" assertion으로 "own-state-destination-applied: /response/body/result/structuredContent/intent/slots/destination/value의 실제 equals 기대값은 {'$alias': 'W'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-conversation-kept" assertion으로 "own-conversation-kept: /response/body/result/structuredContent/conversationRequestId의 실제 equals 기대값은 'T20-other-principal-input'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-method" assertion으로 "own-issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-jsonrpc-id" assertion으로 "own-issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'own-issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-MCP-Protocol-Version" assertion으로 "own-issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-Mcp-Method" assertion으로 "own-issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-Mcp-Name" assertion으로 "own-issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-issued-raw-meta" assertion으로 "own-issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-method" assertion으로 "own-continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-jsonrpc-id" assertion으로 "own-continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'own-continued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-MCP-Protocol-Version" assertion으로 "own-continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-Mcp-Method" assertion으로 "own-continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-Mcp-Name" assertion으로 "own-continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "own-continued-raw-meta" assertion으로 "own-continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state other-method와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-other-method"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'resources/read'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'resources/read'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state other-intent와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-other-intent"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state unmatched-responses와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-unmatched-responses"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 유효한 발주 입력의 state-as-approval가 승인 없이 효과를 만들지 않고 MANAGER 결정 후 실행한다
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-state-as-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "writer" 역할이 "approved-dispatch" 행동을 수행한다
    만일 "reader" 역할이 "positive-inventory" 행동을 수행한다
    만일 "시스템" 역할이 "db-positive" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'WAITING_APPROVAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-manager-approval-missing" assertion으로 "specific-manager-approval-missing: /response/body/result/structuredContent/error/code의 실제 equals 기대값은 'APPROVAL_REQUIRED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "required-decision-role" assertion으로 "required-decision-role: /response/body/result/structuredContent/error/requiredRole의 실제 equals 기대값은 'MANAGER'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-dispatch-payload" assertion으로 "actual-dispatch-payload: /data/transcript/request/body/params/arguments의 실제 equals 기대값은 {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': 'dispatchPurchaseOrder', 'subjectRefs': [{'type': 'TradeItem', 'id': {'$alias': 'P'}}], 'slots': {'proposalId': {'value': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalId'}}, 'provenance': 'CONTEXT'}, 'proposalHash': {'value': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalHash'}}, 'provenance': 'CONTEXT'}, 'channel': {'value': 'SYNTHETIC_SUPPLIER', 'provenance': 'USER'}, 'externalOperationId': {'value': 'T20-state-as-approval-dispatch-external', 'provenance': 'USER'}}, 'conditions': [], 'evidenceRefs': [], 'expectedRevision': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalRevision'}}, 'commandIdempotencyKey': 'T20-state-as-approval-dispatch'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-issued-state" assertion으로 "actual-issued-state: continued의 /data/transcript/request/body/params/requestState와 issued의 /response/body/result/requestState를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "matched-channel-input" assertion으로 "matched-channel-input: /data/transcript/request/body/params/inputResponses의 실제 equals 기대값은 [{'requestId': {'$result': {'actionId': 'issued', 'pointer': '/response/body/result/inputRequests/0/requestId'}}, 'value': {'channel': 'SYNTHETIC_SUPPLIER'}}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-purchase-orders" assertion으로 "unchanged-purchase-orders: db-after의 /data/rawRows/purchaseOrders와 db-before의 /data/rawRows/purchaseOrders를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "unchanged-proposals" assertion으로 "unchanged-proposals: db-after의 /data/rawRows/proposals와 db-before의 /data/rawRows/proposals를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "no-manager-decision-before" assertion으로 "no-manager-decision-before: /data/rawRows/approvals의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "no-manager-decision-after" assertion으로 "no-manager-decision-after: /data/rawRows/approvals의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "manager-approval-applied" assertion으로 "manager-approval-applied: /response/outcome의 실제 equals 기대값은 'APPLIED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-countercall-applied" assertion으로 "approved-countercall-applied: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'ACCEPTED_PENDING_EXTERNAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "real-manager-decision" assertion으로 "real-manager-decision: /data/rawRows/approvals의 실제 ['id', 'proposalId', 'proposalHash', 'proposalRevision', 'approverId', 'decidedAt', 'validUntil', 'consumptionPolicy', 'decision']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "approved-countercall-order" assertion으로 "approved-countercall-order: /data/rawRows/purchaseOrders의 실제 ['id', 'proposalId', 'proposalHash', 'approvalId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "approved-countercall-effect-once" assertion으로 "approved-countercall-effect-once: /data/rawRows/outbox의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-method" assertion으로 "approved-dispatch-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-jsonrpc-id" assertion으로 "approved-dispatch-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'approved-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-MCP-Protocol-Version" assertion으로 "approved-dispatch-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-Mcp-Method" assertion으로 "approved-dispatch-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-Mcp-Name" assertion으로 "approved-dispatch-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-meta" assertion으로 "approved-dispatch-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 유효한 발주 입력의 accept-as-approval가 승인 없이 효과를 만들지 않고 MANAGER 결정 후 실행한다
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-accept-as-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "writer" 역할이 "approved-dispatch" 행동을 수행한다
    만일 "reader" 역할이 "positive-inventory" 행동을 수행한다
    만일 "시스템" 역할이 "db-positive" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'WAITING_APPROVAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "specific-manager-approval-missing" assertion으로 "specific-manager-approval-missing: /response/body/result/structuredContent/error/code의 실제 equals 기대값은 'APPROVAL_REQUIRED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "required-decision-role" assertion으로 "required-decision-role: /response/body/result/structuredContent/error/requiredRole의 실제 equals 기대값은 'MANAGER'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-dispatch-payload" assertion으로 "actual-dispatch-payload: /data/transcript/request/body/params/arguments의 실제 equals 기대값은 {'intentKind': 'COMMAND', 'definitionVersion': 'definition-v1', 'capabilityId': 'dispatchPurchaseOrder', 'subjectRefs': [{'type': 'TradeItem', 'id': {'$alias': 'P'}}], 'slots': {'proposalId': {'value': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalId'}}, 'provenance': 'CONTEXT'}, 'proposalHash': {'value': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalHash'}}, 'provenance': 'CONTEXT'}, 'channel': {'value': 'SYNTHETIC_SUPPLIER', 'provenance': 'USER'}, 'externalOperationId': {'value': 'T20-accept-as-approval-dispatch-external', 'provenance': 'USER'}}, 'conditions': [], 'evidenceRefs': [], 'expectedRevision': {'$result': {'actionId': 'proposal', 'pointer': '/response/proposalRevision'}}, 'commandIdempotencyKey': 'T20-accept-as-approval-dispatch'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "actual-issued-state" assertion으로 "actual-issued-state: continued의 /data/transcript/request/body/params/requestState와 issued의 /response/body/result/requestState를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "matched-channel-input" assertion으로 "matched-channel-input: /data/transcript/request/body/params/inputResponses의 실제 equals 기대값은 [{'requestId': {'$result': {'actionId': 'issued', 'pointer': '/response/body/result/inputRequests/0/requestId'}}, 'value': {'channel': 'SYNTHETIC_SUPPLIER'}, 'action': 'accept'}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-purchase-orders" assertion으로 "unchanged-purchase-orders: db-after의 /data/rawRows/purchaseOrders와 db-before의 /data/rawRows/purchaseOrders를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "unchanged-proposals" assertion으로 "unchanged-proposals: db-after의 /data/rawRows/proposals와 db-before의 /data/rawRows/proposals를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "no-manager-decision-before" assertion으로 "no-manager-decision-before: /data/rawRows/approvals의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "no-manager-decision-after" assertion으로 "no-manager-decision-after: /data/rawRows/approvals의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "manager-approval-applied" assertion으로 "manager-approval-applied: /response/outcome의 실제 equals 기대값은 'APPLIED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-countercall-applied" assertion으로 "approved-countercall-applied: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'ACCEPTED_PENDING_EXTERNAL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "real-manager-decision" assertion으로 "real-manager-decision: /data/rawRows/approvals의 실제 ['id', 'proposalId', 'proposalHash', 'proposalRevision', 'approverId', 'decidedAt', 'validUntil', 'consumptionPolicy', 'decision']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "approved-countercall-order" assertion으로 "approved-countercall-order: /data/rawRows/purchaseOrders의 실제 ['id', 'proposalId', 'proposalHash', 'approvalId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "approved-countercall-effect-once" assertion으로 "approved-countercall-effect-once: /data/rawRows/outbox의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-new-rpc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-method" assertion으로 "approved-dispatch-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-jsonrpc-id" assertion으로 "approved-dispatch-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'approved-dispatch'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-MCP-Protocol-Version" assertion으로 "approved-dispatch-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-Mcp-Method" assertion으로 "approved-dispatch-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-Mcp-Name" assertion으로 "approved-dispatch-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'dispatchPurchaseOrder'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approved-dispatch-raw-meta" assertion으로 "approved-dispatch-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 MRTR state unsupported-client와 금지효과0
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-unsupported-client"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "issued" 행동을 수행한다
    만일 "writer" 역할이 "continued" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "issued-input-required" assertion으로 "issued-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-outcome" assertion으로 "continued-outcome: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "new-rpc-id" assertion으로 "new-rpc-id: /response/body/id의 실제 equals 기대값은 'continued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "fallback-channel" assertion으로 "fallback-channel: /response/body/result/structuredContent/additionalInputMethod의 실제 equals 기대값은 'EXPLICIT_RETRY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-elicitation-requests" assertion으로 "no-elicitation-requests: /response/body/result/inputRequests의 실제 equals 기대값은 []다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-method" assertion으로 "issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-jsonrpc-id" assertion으로 "issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-MCP-Protocol-Version" assertion으로 "issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Method" assertion으로 "issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-Mcp-Name" assertion으로 "issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "issued-raw-meta" assertion으로 "issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-method" assertion으로 "continued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-jsonrpc-id" assertion으로 "continued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'continued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-MCP-Protocol-Version" assertion으로 "continued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Method" assertion으로 "continued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-Mcp-Name" assertion으로 "continued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'structureIntent'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "continued-raw-meta" assertion으로 "continued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'without-elicitation', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 두 실제 wire 거래의 manager 승인 state single-use 소비
    먼저 사례 파일 "verification/cases/T20/case.json"의 "mrtr-concurrent-approval-consumption"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "purchase-work" 행동을 수행한다
    만일 "writer" 역할이 "proposal" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "manager" 역할이 "approval-issued" 행동을 수행한다
    만일 "시스템" 역할이 "start-left" 행동을 수행한다
    만일 "시스템" 역할이 "start-right" 행동을 수행한다
    만일 "시스템" 역할이 "reached-left" 행동을 수행한다
    만일 "시스템" 역할이 "reached-right" 행동을 수행한다
    만일 "시스템" 역할이 "resume-left" 행동을 수행한다
    만일 "시스템" 역할이 "left-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "resume-right" 행동을 수행한다
    만일 "시스템" 역할이 "right-terminal" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "first-input-required" assertion으로 "first-input-required: /response/body/result/resultType의 실제 equals 기대값은 'input_required'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "winner-approved" assertion으로 "winner-approved: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'APPLIED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "loser-conflict" assertion으로 "loser-conflict: /response/body/result/structuredContent/outcome의 실제 equals 기대값은 'CONFLICT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "loser-state-consumed" assertion으로 "loser-state-consumed: /response/body/result/structuredContent/error/code의 실제 equals 기대값은 'REQUEST_STATE_CONSUMED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "one-manager-approval" assertion으로 "one-manager-approval: /data/rawRows/approvals의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "one-state-consumption" assertion으로 "one-state-consumption: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "no-physical-approval-effect" assertion으로 "no-physical-approval-effect: db-after의 /data/rawRows/movements와 db-before의 /data/rawRows/movements를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "no-dispatch-approval-effect" assertion으로 "no-dispatch-approval-effect: db-after의 /data/rawRows/outbox와 db-before의 /data/rawRows/outbox를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "left-raw-new-rpc" assertion으로 "left-raw-new-rpc: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-approval-rpc-left'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "left-effect-key" assertion으로 "left-effect-key: /data/transcript/request/body/params/arguments/commandIdempotencyKey의 실제 equals 기대값은 'T20-approval-left'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "left-actual-issued-state" assertion으로 "left-actual-issued-state: left-terminal의 /data/transcript/request/body/params/requestState와 approval-issued의 /response/body/result/requestState를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "left-terminal-ack" assertion으로 "left-terminal-ack: /data/completed의 실제 equals 기대값은 true다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "left-same-handle" assertion으로 "left-same-handle: left-terminal의 /data/invocationHandle와 start-left의 /data/invocationHandle를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "right-raw-new-rpc" assertion으로 "right-raw-new-rpc: /data/transcript/request/body/id의 실제 equals 기대값은 'T20-approval-rpc-right'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "right-effect-key" assertion으로 "right-effect-key: /data/transcript/request/body/params/arguments/commandIdempotencyKey의 실제 equals 기대값은 'T20-approval-right'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "right-actual-issued-state" assertion으로 "right-actual-issued-state: right-terminal의 /data/transcript/request/body/params/requestState와 approval-issued의 /response/body/result/requestState를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "right-terminal-ack" assertion으로 "right-terminal-ack: /data/completed의 실제 equals 기대값은 true다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "right-same-handle" assertion으로 "right-same-handle: right-terminal의 /data/invocationHandle와 start-right의 /data/invocationHandle를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "approval-issued-raw-method" assertion으로 "approval-issued-raw-method: /data/transcript/request/body/method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approval-issued-raw-jsonrpc-id" assertion으로 "approval-issued-raw-jsonrpc-id: /data/transcript/request/body/id의 실제 equals 기대값은 'approval-issued'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approval-issued-raw-MCP-Protocol-Version" assertion으로 "approval-issued-raw-MCP-Protocol-Version: /data/transcript/request/headers/MCP-Protocol-Version의 실제 equals 기대값은 '2026-07-28'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approval-issued-raw-Mcp-Method" assertion으로 "approval-issued-raw-Mcp-Method: /data/transcript/request/headers/Mcp-Method의 실제 equals 기대값은 'tools/call'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approval-issued-raw-Mcp-Name" assertion으로 "approval-issued-raw-Mcp-Name: /data/transcript/request/headers/Mcp-Name의 실제 equals 기대값은 'approvePurchase'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "approval-issued-raw-meta" assertion으로 "approval-issued-raw-meta: /data/transcript/request/body/params/_meta의 실제 equals 기대값은 {'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientInfo': {'name': 'ontology-channel-contract', 'version': '1.0.0'}, 'io.modelcontextprotocol/clientCapabilities': {'elicitation': {'form': {}}}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: ontology-work-coordinator 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-work-coordinator"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-work-coordinator'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-work-coordinator'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: ontology-procurement-transport 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-procurement-transport"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-procurement-transport'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-procurement-transport'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: ontology-import-qc 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-import-qc"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-import-qc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-import-qc'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: ontology-sales-returns-recall 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-sales-returns-recall"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-sales-returns-recall'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-sales-returns-recall'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: ontology-settlement 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-settlement"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-settlement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-settlement'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: ontology-definition-authoring 실제 discovery·본문·reference·tool·책임
    먼저 사례 파일 "verification/cases/T20/case.json"의 "skill-definition-authoring"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "package-name" assertion으로 "package-name: /data/hostObservation/extractor/rawRows/packages/0/name의 실제 equals 기대값은 'ontology-definition-authoring'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "frontmatter-name" assertion으로 "frontmatter-name: /data/hostObservation/extractor/rawRows/packages/0/frontmatter/name의 실제 equals 기대값은 'ontology-definition-authoring'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "package-version" assertion으로 "package-version: /data/hostObservation/extractor/rawRows/packages/0/version의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "body-hash-links" assertion으로 "body-hash-links: probe의 /data/hostObservation/extractor/rawRows/packages/0/hash와 probe의 /data/hostObservation/extractor/rawRows/loading/0/bodyHash를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "definition-support" assertion으로 "definition-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/definition의 실제 equals 기대값은 'definition-v1'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "schema-support" assertion으로 "schema-support: /data/hostObservation/extractor/rawRows/packages/0/compatibility/schema의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "description-present" assertion으로 "description-present: 비어 있지 않은 실제 원행마다 name, description, version, hash, compatibility를 확인하며 별도 값/효과 assertion과 함께 검증한다."를 확인한다
    그러면 "real-link-.agents" assertion으로 "real-link-.agents: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "real-link-.claude" assertion으로 "real-link-.claude: /data/hostObservation/extractor/rawRows/links의 실제 ['path', 'realTarget']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "loading-discovered" assertion으로 "loading-discovered: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-body_read" assertion으로 "loading-body_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-reference_read" assertion으로 "loading-reference_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-tool_call" assertion으로 "loading-tool_call: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "loading-obligation_read" assertion으로 "loading-obligation_read: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 1개다."를 확인한다
    그러면 "all-needed-reference-paths" assertion으로 "all-needed-reference-paths: probe의 /data/hostObservation/extractor/rawRows/referenceReads와 probe의 /data/hostObservation/extractor/rawRows/packages/0/requiredReferences를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "client-version-pinned" assertion으로 "client-version-pinned: probe의 /data/hostObservation/extractor/rawRows/client/version와 probe의 /data/hostObservation/operationEvidence/clientVersion를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-client-manifest" assertion으로 "supported-client-manifest: probe의 /data/hostObservation/extractor/rawRows/clientCompatibility/clientId와 probe의 /data/hostObservation/operationEvidence/clientId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "supported-capability-schema" assertion으로 "supported-capability-schema: /data/hostObservation/extractor/rawRows/clientCompatibility/schemaVersion의 실제 equals 기대값은 '1.0.0'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "compatible-client-status" assertion으로 "compatible-client-status: /data/hostObservation/extractor/rawRows/clientCompatibility/outcome의 실제 equals 기대값은 'SUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "tool-scope" assertion으로 "tool-scope: /data/hostObservation/extractor/rawRows/toolCalls의 실제 ['actorId', 'organizationId', 'intentKind', 'capabilityId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-obligation-check" assertion으로 "human-obligation-check: /data/hostObservation/extractor/rawRows/obligations의 실제 ['workId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
  시나리오: 실제 client hash-only 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-hash-only"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "loading-gate-not-run" assertion으로 "loading-gate-not-run: /data/hostObservation/extractor/rawRows/gate/status의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-body-trace" assertion으로 "no-body-trace: /data/hostObservation/extractor/rawRows/loading의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 실제 client allowed-tools-write 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-allowed-tools-write"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "no-command-tools" assertion으로 "no-command-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "query-interpretation" assertion으로 "query-interpretation: /data/hostObservation/extractor/rawRows/interpretation/intentKind의 실제 equals 기대값은 'QUERY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 client malicious-document 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-malicious-document"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "no-command-tools" assertion으로 "no-command-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "query-interpretation" assertion으로 "query-interpretation: /data/hostObservation/extractor/rawRows/interpretation/intentKind의 실제 equals 기대값은 'QUERY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 client obsolete-skill 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-obsolete-skill"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "incompatible-loading" assertion으로 "incompatible-loading: /data/hostObservation/extractor/rawRows/compatibility/outcome의 실제 equals 기대값은 'VERSION_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "incompatible-gate" assertion으로 "incompatible-gate: /data/hostObservation/extractor/rawRows/gate/status의 실제 equals 기대값은 'NOT_RUN'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 client synonym 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-synonym"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "no-command-tools" assertion으로 "no-command-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "query-interpretation" assertion으로 "query-interpretation: /data/hostObservation/extractor/rawRows/interpretation/intentKind의 실제 equals 기대값은 'QUERY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 client multilingual 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-multilingual"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "no-command-tools" assertion으로 "no-command-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
    그러면 "query-interpretation" assertion으로 "query-interpretation: /data/hostObservation/extractor/rawRows/interpretation/intentKind의 실제 equals 기대값은 'QUERY'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 실제 client ambiguous 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-ambiguous"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "clarification-needed" assertion으로 "clarification-needed: /data/hostObservation/extractor/rawRows/interpretation/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-write-tools" assertion으로 "no-write-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 실제 client same-name 의미와 서버 권한 경계
    먼저 사례 파일 "verification/cases/T20/case.json"의 "host-same-name"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "probe" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "clarification-needed" assertion으로 "clarification-needed: /data/hostObservation/extractor/rawRows/interpretation/outcome의 실제 equals 기대값은 'NEEDS_INPUT'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-write-tools" assertion으로 "no-write-tools: /data/hostObservation/extractor/rawRows/toolCalls의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
