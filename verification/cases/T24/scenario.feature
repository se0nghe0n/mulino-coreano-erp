# language: ko
@T24 @D24 @sit @uat @contract-red
기능: 모든 인가 경로·감사 rollback·보존·복원·원문 redaction을 검증한다

  시나리오: search 경로의 조직밖 또는 READ위임 우회를 거부한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "deny-search"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "allowed-own" 행동을 수행한다
    만일 "readAgent" 역할이 "attack" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "search-data-empty" assertion으로 "search data empty의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "denial-audit" assertion으로 "denial audit의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: blob 경로의 조직밖 또는 READ위임 우회를 거부한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "deny-blob"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "allowed-own" 행동을 수행한다
    만일 "readAgent" 역할이 "attack" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "forbidden-code" assertion으로 "forbidden code의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-secret-reference" assertion으로 "no secret reference의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "denial-audit" assertion으로 "denial audit의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-download-ref" assertion으로 "no download ref의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: batch 경로의 조직밖 또는 READ위임 우회를 거부한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "deny-batch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "warehouse" 역할이 "permitted-first" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "allowed-own" 행동을 수행한다
    만일 "warehouse" 역할이 "attack" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "forbidden-code" assertion으로 "forbidden code의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-secret-reference" assertion으로 "no secret reference의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "denial-audit" assertion으로 "denial audit의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "restrictions-unchanged" assertion으로 "restrictions unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "permitted-first-applied" assertion으로 "permitted first applied의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "first-hold-survives-batch" assertion으로 "first hold survives batch의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: worker 경로의 조직밖 또는 READ위임 우회를 거부한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "deny-worker"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "allowed-own" 행동을 수행한다
    만일 "worker" 역할이 "attack" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "forbidden-code" assertion으로 "forbidden code의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-secret-reference" assertion으로 "no secret reference의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "denial-audit" assertion으로 "denial audit의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "worker-delegated-actor" assertion으로 "worker delegated actor의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: admin 경로의 조직밖 또는 READ위임 우회를 거부한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "deny-admin"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "allowed-own" 행동을 수행한다
    만일 "admin" 역할이 "attack" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "forbidden-code" assertion으로 "forbidden code의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-secret-reference" assertion으로 "no secret reference의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "denial-audit" assertion으로 "denial audit의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 서버 인가 후 단기참조로 자기 근거 bytes를 읽는다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "authorized-blob"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "readAgent" 역할이 "download" 행동을 수행한다
    만일 "시스템" 역할이 "inspect" 행동을 수행한다
    그러면 "own-download-digest" assertion으로 "own download digest의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "short-reference-expiry" assertion으로 "short reference expiry의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "short-reference-object" assertion으로 "short reference object의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 감사저장 실패는 모든 거래효과를 rollback하고 같은key로 재시도한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "audit-rollback-and-retry"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "warehouse" 역할이 "fail" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "시스템" 역할이 "remove-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "retry" 행동을 수행한다
    만일 "recorder" 역할이 "retry-view" 행동을 수행한다
    만일 "시스템" 역할이 "retry-db" 행동을 수행한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "obligations-unchanged" assertion으로 "obligations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assignments-unchanged" assertion으로 "assignments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "command_records-unchanged" assertion으로 "command_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-error" assertion으로 "audit error의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-applied" assertion으로 "retry applied의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-complete" assertion으로 "audit complete의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-before-after" assertion으로 "audit before after의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-after" assertion으로 "audit after의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-effect-reference" assertion으로 "audit effect reference의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-request-ref" assertion으로 "audit request ref의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-no-extra-approval" assertion으로 "audit no extra approval의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "audit-evidence" assertion으로 "audit evidence의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-one-movement" assertion으로 "retry one movement의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 보존 legal-hold의 근거·참조·실제blob효과를 검증한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "legal-hold"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-sweep" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "sweep-authenticated-reviewer" assertion으로 "sweep authenticated reviewer의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "different-artifact-policies" assertion으로 "different artifact policies의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "documents-unchanged" assertion으로 "documents unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "tombstones-unchanged" assertion으로 "tombstones unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "deletion_log-unchanged" assertion으로 "deletion_log unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "obligations-unchanged" assertion으로 "obligations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assignments-unchanged" assertion으로 "assignments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "blob-preserved" assertion으로 "blob preserved의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 보존 active-reference의 근거·참조·실제blob효과를 검증한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "active-reference"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-sweep" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "sweep-authenticated-reviewer" assertion으로 "sweep authenticated reviewer의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "different-artifact-policies" assertion으로 "different artifact policies의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "documents-unchanged" assertion으로 "documents unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "tombstones-unchanged" assertion으로 "tombstones unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "deletion_log-unchanged" assertion으로 "deletion_log unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "obligations-unchanged" assertion으로 "obligations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assignments-unchanged" assertion으로 "assignments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "blob-preserved" assertion으로 "blob preserved의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 보존 R6-unconfirmed의 근거·참조·실제blob효과를 검증한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "R6-unconfirmed"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-sweep" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "sweep-authenticated-reviewer" assertion으로 "sweep authenticated reviewer의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "different-artifact-policies" assertion으로 "different artifact policies의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "documents-unchanged" assertion으로 "documents unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "tombstones-unchanged" assertion으로 "tombstones unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "deletion_log-unchanged" assertion으로 "deletion_log unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "obligations-unchanged" assertion으로 "obligations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assignments-unchanged" assertion으로 "assignments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "blob-preserved" assertion으로 "blob preserved의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 보존 authorized-delete의 근거·참조·실제blob효과를 검증한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "authorized-delete"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-sweep" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "sweep-authenticated-reviewer" assertion으로 "sweep authenticated reviewer의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "different-artifact-policies" assertion으로 "different artifact policies의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "tombstone-one" assertion으로 "tombstone one의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "delete-reconciliation" assertion으로 "delete reconciliation의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "actual-blob-deleted" assertion으로 "actual blob deleted의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 보존 restore-deleted의 근거·참조·실제blob효과를 검증한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "restore-deleted"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-sweep" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-restore" 행동을 수행한다
    그러면 "sweep-authenticated-reviewer" assertion으로 "sweep authenticated reviewer의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "different-artifact-policies" assertion으로 "different artifact policies의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "tombstone-one" assertion으로 "tombstone one의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "delete-reconciliation" assertion으로 "delete reconciliation의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "actual-blob-deleted" assertion으로 "actual blob deleted의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "restored-blob-not-resurrected" assertion으로 "restored blob not resurrected의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "backup-components" assertion으로 "backup components의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "restored-tombstone" assertion으로 "restored tombstone의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "restore-deletion-policy" assertion으로 "restore deletion policy의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 가상비밀을queue·오류·log·감사·근거·backup에서 전체byte로 검사한다
    먼저 사례 파일 "verification/cases/T24/case.json"의 "sentinel-artifact-scan"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "request-with-sentinel" 행동을 수행한다
    만일 "readAgent" 역할이 "query-audit" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "collect-artifacts" 행동을 수행한다
    만일 "시스템" 역할이 "inventory" 행동을 수행한다
    만일 "시스템" 역할이 "scan" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "scan-six-surfaces" assertion으로 "scan six surfaces의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "all-files-scanned" assertion으로 "all files scanned의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-0-no-sentinel" assertion으로 "surface 0 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-1-no-sentinel" assertion으로 "surface 1 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-2-no-sentinel" assertion으로 "surface 2 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-3-no-sentinel" assertion으로 "surface 3 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-4-no-sentinel" assertion으로 "surface 4 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "surface-5-no-sentinel" assertion으로 "surface 5 no sentinel의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "domain_records-unchanged" assertion으로 "domain_records unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "outbox-unchanged" assertion으로 "outbox unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "query-audit-exists" assertion으로 "query audit exists의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "necessary-refs-preserved" assertion으로 "necessary refs preserved의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "separate-secret-restore" assertion으로 "separate secret restore의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-secret-config-content" assertion으로 "no secret config content의 실제 값과 범위를 대조한다"를 확인한다
