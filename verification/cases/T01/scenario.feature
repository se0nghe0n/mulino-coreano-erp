# language: ko
@T01 @D01 @contract-red
기능: 같은 업무 세계의 역량 질문과 범위
  시나리오: 명사와 동사의 같은 snapshot·수량·증거·책임
    먼저 사례 파일 "verification/cases/T01/case.json"의 "same-world"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "verb" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "same-snapshotRevision" assertion으로 "same-snapshotRevision: noun의 /response/snapshotRevision와 verb의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-asOf" assertion으로 "same-asOf: noun의 /response/asOf와 verb의 /response/asOf를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-knownAt" assertion으로 "same-knownAt: noun의 /response/knownAt와 verb의 /response/knownAt를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-scope" assertion으로 "same-scope: noun의 /response/scope와 verb의 /response/scope를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-itemId" assertion으로 "same-itemId: noun의 /response/data/itemId와 verb의 /response/data/itemId를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-workIds" assertion으로 "same-workIds: noun의 /response/data/workIds와 verb의 /response/data/workIds를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-evidenceRefs" assertion으로 "same-evidenceRefs: noun의 /response/data/evidenceRefs와 verb의 /response/data/evidenceRefs를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-ownerIds" assertion으로 "same-ownerIds: noun의 /response/data/ownerIds와 verb의 /response/data/ownerIds를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "same-nextActions" assertion으로 "same-nextActions: noun의 /response/data/nextActions와 verb의 /response/data/nextActions를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "noun-heldQuantity" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "verb-heldQuantity" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "noun-eligibleQuantity" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "verb-eligibleQuantity" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "noun-cumulativeArrival" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "verb-cumulativeArrival" assertion으로 "두 leaf60+40=보유100, QC보류40 제외=판매60, 같은 canonical 수령60+40=누적100이다."를 확인한다
    그러면 "db-held-sum" assertion으로 "db-held-sum: /data/rawRows/segments의 실제 sumEquals 기대값은 '100'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "responsibility" assertion으로 "responsibility: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "unique-physical-leaf" assertion으로 "unique-physical-leaf: /data/rawRows/segments의 실제 unique 기대값은 true다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 무엇이 어디에 얼마나 있는가
    먼저 사례 파일 "verification/cases/T01/case.json"의 "inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "answer" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "answer-snapshot" assertion으로 "answer-snapshot: answer의 /response/snapshotRevision와 noun의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "answer-asOf" assertion으로 "answer-asOf: /response/asOf의 실제 equals 기대값은 '2026-10-07T09:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-knownAt" assertion으로 "answer-knownAt: /response/knownAt의 실제 equals 기대값은 '2026-10-07T09:00:01Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-scope" assertion으로 "answer-scope: /response/scope의 실제 equals 기대값은 {'organizationId': {'$alias': 'ORG'}, 'itemId': {'$alias': 'P'}, 'lotId': {'$alias': 'L'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-evidence" assertion으로 "answer-evidence: /response/evidenceRefs의 실제 값는 고정한 3개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-duty" assertion으로 "human-duty: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "held100" assertion으로 "held100: /response/data/heldQuantity의 실제 decimalEquals 기대값은 '100'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "leaf-location" assertion으로 "leaf-location: /data/rawRows/segments의 실제 ['id', 'locationId', 'quantity', 'unit']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 왜 공급할 수 있는가
    먼저 사례 파일 "verification/cases/T01/case.json"의 "supply-basis"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "answer" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "answer-snapshot" assertion으로 "answer-snapshot: answer의 /response/snapshotRevision와 noun의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "answer-asOf" assertion으로 "answer-asOf: /response/asOf의 실제 equals 기대값은 '2026-10-07T09:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-knownAt" assertion으로 "answer-knownAt: /response/knownAt의 실제 equals 기대값은 '2026-10-07T09:00:01Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-scope" assertion으로 "answer-scope: /response/scope의 실제 equals 기대값은 {'organizationId': {'$alias': 'ORG'}, 'itemId': {'$alias': 'P'}, 'lotId': {'$alias': 'L'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-evidence" assertion으로 "answer-evidence: /response/evidenceRefs의 실제 값는 고정한 3개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-duty" assertion으로 "human-duty: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "supply-action" assertion으로 "supply-action: /response/data/action의 실제 equals 기대값은 'SELL'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "supply-quantity-scope" assertion으로 "supply-quantity-scope: /response/data/segmentIds의 실제 equals 기대값은 [{'$alias': 'A60'}]다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "basis-QC" assertion으로 "basis-QC: /response/data/conditions의 실제 ['kind', 'result', 'evidenceId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "basis-REGULATORY" assertion으로 "basis-REGULATORY: /response/data/conditions의 실제 ['kind', 'result', 'evidenceId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "basis-DISPOSITION" assertion으로 "basis-DISPOSITION: /response/data/conditions의 실제 ['kind', 'result', 'evidenceId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "basis-CUSTOMER" assertion으로 "basis-CUSTOMER: /response/data/conditions의 실제 ['kind', 'result', 'evidenceId']는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 어떤 업무가 무엇을 기다리는가
    먼저 사례 파일 "verification/cases/T01/case.json"의 "waiting"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "answer" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "answer-snapshot" assertion으로 "answer-snapshot: answer의 /response/snapshotRevision와 noun의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "answer-asOf" assertion으로 "answer-asOf: /response/asOf의 실제 equals 기대값은 '2026-10-07T09:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-knownAt" assertion으로 "answer-knownAt: /response/knownAt의 실제 equals 기대값은 '2026-10-07T09:00:01Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-scope" assertion으로 "answer-scope: /response/scope의 실제 equals 기대값은 {'organizationId': {'$alias': 'ORG'}, 'itemId': {'$alias': 'P'}, 'lotId': {'$alias': 'L'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-evidence" assertion으로 "answer-evidence: /response/evidenceRefs의 실제 값는 고정한 3개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-duty" assertion으로 "human-duty: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "wait-status" assertion으로 "wait-status: /response/data/status의 실제 equals 기대값은 'WAITING'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "wait-reason" assertion으로 "wait-reason: /response/data/wait/reason의 실제 equals 기대값은 'B40 품질 후속 근거 대기'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "resume-evidence" assertion으로 "resume-evidence: /response/data/wait/resumePredicate/evidenceId의 실제 equals 기대값은 {'$alias': 'hold40'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "verifier" assertion으로 "verifier: /response/data/wait/verifierId의 실제 equals 기대값은 {'$alias': 'reader'}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "check" assertion으로 "check: /response/data/wait/nextCheckAt의 실제 equals 기대값은 '2026-10-07T10:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
  시나리오: 누가 언제까지 무엇을 해야 하는가
    먼저 사례 파일 "verification/cases/T01/case.json"의 "responsibility"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "answer" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "answer-snapshot" assertion으로 "answer-snapshot: answer의 /response/snapshotRevision와 noun의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "answer-asOf" assertion으로 "answer-asOf: /response/asOf의 실제 equals 기대값은 '2026-10-07T09:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-knownAt" assertion으로 "answer-knownAt: /response/knownAt의 실제 equals 기대값은 '2026-10-07T09:00:01Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-scope" assertion으로 "answer-scope: /response/scope의 실제 equals 기대값은 {'organizationId': {'$alias': 'ORG'}, 'itemId': {'$alias': 'P'}, 'lotId': {'$alias': 'L'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-evidence" assertion으로 "answer-evidence: /response/evidenceRefs의 실제 값는 고정한 3개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-duty" assertion으로 "human-duty: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "answer-duty" assertion으로 "answer-duty: /response/data/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 어느 고객까지 영향이 있는가
    먼저 사례 파일 "verification/cases/T01/case.json"의 "customer-impact"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "reader" 역할이 "answer" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "answer-snapshot" assertion으로 "answer-snapshot: answer의 /response/snapshotRevision와 noun의 /response/snapshotRevision를 같은 scope에서 exact 대조한다."를 확인한다
    그러면 "answer-asOf" assertion으로 "answer-asOf: /response/asOf의 실제 equals 기대값은 '2026-10-07T09:00:00Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-knownAt" assertion으로 "answer-knownAt: /response/knownAt의 실제 equals 기대값은 '2026-10-07T09:00:01Z'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-scope" assertion으로 "answer-scope: /response/scope의 실제 equals 기대값은 {'organizationId': {'$alias': 'ORG'}, 'itemId': {'$alias': 'P'}, 'lotId': {'$alias': 'L'}}다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "answer-evidence" assertion으로 "answer-evidence: /response/evidenceRefs의 실제 값는 고정한 3개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "human-duty" assertion으로 "human-duty: /data/rawRows/obligations의 실제 ['responsibleWorkId', 'ownerId', 'nextAction', 'nextCheckAt']는 고정한 2개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "customers" assertion으로 "customers: /response/data/customerIds의 실제 값는 고정한 1개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
    그러면 "candidate-not-contamination" assertion으로 "candidate-not-contamination: /response/data/impactKind의 실제 equals 기대값은 'CANDIDATE'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "lot-delivery-customer" assertion으로 "lot-delivery-customer: /data/rawRows/relations의 실제 ['sourceId', 'targetId', 'type']는 고정한 4개 identity/관계와 exact 일치하며 중복·누락을 거부한다."를 확인한다
  시나리오: 미지원 manufacture 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-manufacture"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'manufacture'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 미지원 BOM 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-BOM"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'BOM'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 미지원 B2C 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-B2C"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'B2C'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 미지원 generalLedger 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-generalLedger"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'generalLedger'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 미지원 taxSubmit 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-taxSubmit"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'taxSubmit'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
  시나리오: 미지원 bankTransfer 요청의 효과0
    먼저 사례 파일 "verification/cases/T01/case.json"의 "excluded-bankTransfer"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "noun" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "writer" 역할이 "unsupported" 행동을 수행한다
    만일 "reader" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-works" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "승인·실물·배분·업무·외부효과의 전후 원행을 exact 대조한다. 허용 감사는 이 불변 대상에서 분리한다."를 확인한다
    그러면 "unsupported-outcome" assertion으로 "unsupported-outcome: /response/outcome의 실제 equals 기대값은 'REJECTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-code" assertion으로 "unsupported-code: /response/error/code의 실제 equals 기대값은 'CAPABILITY_UNSUPPORTED'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "unsupported-effect" assertion으로 "unsupported-effect: /response/error/requestedEffect의 실제 equals 기대값은 'bankTransfer'다. 미관찰·UNKNOWN은 정상값이 아니다."를 확인한다
    그러면 "no-substitute-command" assertion으로 "no-substitute-command: /data/rawRows/commandResults의 scope·filter를 만족하는 실제 원행은 정확히 0개다."를 확인한다
