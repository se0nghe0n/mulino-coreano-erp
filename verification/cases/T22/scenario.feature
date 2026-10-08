# language: ko
@T22 @D22 @sit @uat @contract-red
기능: 원천 중복·상충·canonical 동일성과 불명확한 외부 효과를 대조한다

  시나리오: 같은source키 replay와 다른hash 상충을 원문 보존으로 구별한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "source-key-hash-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "first" 행동을 수행한다
    만일 "recorder" 역할이 "accepted" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "replay" 행동을 수행한다
    만일 "recorder" 역할이 "conflict" 행동을 수행한다
    만일 "recorder" 역할이 "inbox" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "recorder" 역할이 "download-carrier-A" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-carrier-A" 행동을 수행한다
    만일 "recorder" 역할이 "download-carrier-B" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-carrier-B" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-EVIDENCE_CONFLICT" 행동을 수행한다
    그러면 "accepted-once" assertion으로 "accepted once의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "hash-conflict" assertion으로 "hash conflict의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "accepted-original-hash" assertion으로 "accepted original hash의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "conflicting-raw-hash" assertion으로 "conflicting raw hash의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "canonical_links-unchanged" assertion으로 "canonical_links unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "preserved-carrier-A-bytes" assertion으로 "preserved carrier A bytes의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "preserved-carrier-B-bytes" assertion으로 "preserved carrier B bytes의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EVIDENCE_CONFLICT-one-assignment" assertion으로 "EVIDENCE_CONFLICT one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EVIDENCE_CONFLICT-responsibility" assertion으로 "EVIDENCE_CONFLICT responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EVIDENCE_CONFLICT-human-owner" assertion으로 "EVIDENCE_CONFLICT human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EVIDENCE_CONFLICT-api-responsibility" assertion으로 "EVIDENCE_CONFLICT api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 운송과창고의 같은60은 두문서 한실물이다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "two-sources-one-occurrence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "carrier" 행동을 수행한다
    만일 "reconciler" 역할이 "match-carrier" 행동을 수행한다
    만일 "reconciler" 역할이 "canonical" 행동을 수행한다
    만일 "recorder" 역할이 "receipt" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "warehouse" 행동을 수행한다
    만일 "recorder" 역할이 "unverified" 행동을 수행한다
    만일 "시스템" 역할이 "db-unverified" 행동을 수행한다
    만일 "reconciler" 역할이 "match-warehouse" 행동을 수행한다
    만일 "reconciler" 역할이 "link-warehouse" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "recorder" 역할이 "arrival-api" 행동을 수행한다
    그러면 "unverified-movements" assertion으로 "unverified movements의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "unverified-segments" assertion으로 "unverified segments의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "receipt-sum" assertion으로 "receipt sum의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "arrival-api-60" assertion으로 "arrival api 60의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "one-physical-receipt" assertion으로 "one physical receipt의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "one-canonical-two-reports" assertion으로 "one canonical two reports의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 권한 또는 실물범위 근거 없는 cross-org-match 대조를 거부한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "cross-org-match"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reconciler" 역할이 "invalid" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "invalid-outcome" assertion으로 "invalid outcome의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "identity_matches-unchanged" assertion으로 "identity_matches unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "canonical_links-unchanged" assertion으로 "canonical_links unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assessments-unchanged" assertion으로 "assessments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 권한 또는 실물범위 근거 없는 overlapping-match 대조를 거부한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "overlapping-match"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reconciler" 역할이 "invalid" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "invalid-outcome" assertion으로 "invalid outcome의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "identity_matches-unchanged" assertion으로 "identity_matches unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "canonical_links-unchanged" assertion으로 "canonical_links unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assessments-unchanged" assertion으로 "assessments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "existing-receipt-stays-60" assertion으로 "existing receipt stays 60의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 권한 또는 실물범위 근거 없는 arbitrary-priority 대조를 거부한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "arbitrary-priority"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "reconciler" 역할이 "invalid" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "invalid-outcome" assertion으로 "invalid outcome의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "identity_matches-unchanged" assertion으로 "identity_matches unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "canonical_links-unchanged" assertion으로 "canonical_links unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assessments-unchanged" assertion으로 "assessments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 권한 또는 실물범위 근거 없는 unauthorized-match 대조를 거부한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "unauthorized-match"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "readAgent" 역할이 "invalid" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "invalid-outcome" assertion으로 "invalid outcome의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "identity_matches-unchanged" assertion으로 "identity_matches unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "canonical_links-unchanged" assertion으로 "canonical_links unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "assessments-unchanged" assertion으로 "assessments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 늦은옛해제 보고는 현재 새보류를 덮지 않는다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "late-old-release"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "late" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "restrictions-unchanged" assertion으로 "restrictions unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "new-hold-active" assertion으로 "new hold active의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 미설정 자동 source를 활성화하지 않고 원문과 담당을 남긴다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "unprofiled-automatic-source"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "automatic" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "시스템" 역할이 "connector-config" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "source_profiles-unchanged" assertion으로 "source_profiles unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "automatic-connector-disabled" assertion으로 "automatic connector disabled의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 검토된 source profile 필드와 원문·pending 처리를 검증한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "reviewed-source-profile"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "intake" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "recorder" 역할이 "pending-inbox" 행동을 수행한다
    만일 "recorder" 역할이 "raw-profile-document" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-original" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-SOURCE_QUARANTINE" 행동을 수행한다
    그러면 "profile-fields" assertion으로 "profile fields의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "original-hash" assertion으로 "original hash의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "explicit-pending" assertion으로 "explicit pending의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "original-bytes-hash" assertion으로 "original bytes hash의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-one-assignment" assertion으로 "SOURCE_QUARANTINE one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-responsibility" assertion으로 "SOURCE_QUARANTINE responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-human-owner" assertion으로 "SOURCE_QUARANTINE human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "SOURCE_QUARANTINE-api-responsibility" assertion으로 "SOURCE_QUARANTINE api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 unknown의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-unknown"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-EXTERNAL_RECONCILIATION" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "unknown-kept" assertion으로 "unknown kept의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-one-assignment" assertion으로 "EXTERNAL_RECONCILIATION one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-responsibility" assertion으로 "EXTERNAL_RECONCILIATION responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-human-owner" assertion으로 "EXTERNAL_RECONCILIATION human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-api-responsibility" assertion으로 "EXTERNAL_RECONCILIATION api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 success의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-success"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-proof" 행동을 수행한다
    만일 "recorder" 역할이 "attach-proof" 행동을 수행한다
    만일 "reconciler" 역할이 "reconcile" 행동을 수행한다
    만일 "recorder" 역할이 "reconciled" 행동을 수행한다
    만일 "시스템" 역할이 "db-reconciled" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "success-existing-result" assertion으로 "success existing result의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 failure-retry의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-failure-retry"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-proof" 행동을 수행한다
    만일 "recorder" 역할이 "attach-proof" 행동을 수행한다
    만일 "reconciler" 역할이 "reconcile" 행동을 수행한다
    만일 "recorder" 역할이 "reconciled" 행동을 수행한다
    만일 "시스템" 역할이 "db-reconciled" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "retry-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "confirmed-failure-before-retry" assertion으로 "confirmed failure before retry의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-1" assertion으로 "same external operation 1의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "failure-retry-policy" assertion으로 "failure retry policy의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "new-attempt-unknown-blocks-next-retry" assertion으로 "new attempt unknown blocks next retry의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 failure-no-retry의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-failure-no-retry"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-proof" 행동을 수행한다
    만일 "recorder" 역할이 "attach-proof" 행동을 수행한다
    만일 "reconciler" 역할이 "reconcile" 행동을 수행한다
    만일 "recorder" 역할이 "reconciled" 행동을 수행한다
    만일 "시스템" 역할이 "db-reconciled" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "confirmed-failure-before-retry" assertion으로 "confirmed failure before retry의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "failure-retry-policy" assertion으로 "failure retry policy의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 no-lookup의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-no-lookup"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-EXTERNAL_RECONCILIATION" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "unknown-kept" assertion으로 "unknown kept의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-one-assignment" assertion으로 "EXTERNAL_RECONCILIATION one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-responsibility" assertion으로 "EXTERNAL_RECONCILIATION responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-human-owner" assertion으로 "EXTERNAL_RECONCILIATION human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-api-responsibility" assertion으로 "EXTERNAL_RECONCILIATION api responsibility의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 외부 응답유실 뒤 local-cancel의 결과와 재발행을 대조한다
    먼저 사례 파일 "verification/cases/T22/case.json"의 "external-local-cancel"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "remote-mode" 행동을 수행한다
    만일 "operations" 역할이 "send" 행동을 수행한다
    만일 "시스템" 역할이 "send-tick" 행동을 수행한다
    만일 "시스템" 역할이 "send-terminal" 행동을 수행한다
    만일 "recorder" 역할이 "unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "remote-before" 행동을 수행한다
    만일 "operations" 역할이 "cancel" 행동을 수행한다
    만일 "operations" 역할이 "retry-target" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "시스템" 역할이 "retry-tick" 행동을 수행한다
    만일 "시스템" 역할이 "remote-after" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-EXTERNAL_RECONCILIATION" 행동을 수행한다
    그러면 "unknown-state" assertion으로 "unknown state의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-unknown" assertion으로 "raw unknown의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "retry-server-derived-target" assertion으로 "서버 감사 원행에서 retrySafeCommand의 실행 주체는 인증된 operations이고 대상은 원 발주 전달 command 하나다. 요청은 commandId·사유만 보내며 원 actor·hash·멱등키·외부 operation ID를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "remote-request-count" assertion으로 "remote request count의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "same-external-operation-0" assertion으로 "same external operation 0의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "remote-cancel-not-sent" assertion으로 "remote cancel not sent의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "local-cancel-pending" assertion으로 "local cancel pending의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-one-assignment" assertion으로 "EXTERNAL_RECONCILIATION one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-responsibility" assertion으로 "EXTERNAL_RECONCILIATION responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-human-owner" assertion으로 "EXTERNAL_RECONCILIATION human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "EXTERNAL_RECONCILIATION-api-responsibility" assertion으로 "EXTERNAL_RECONCILIATION api responsibility의 실제 값과 범위를 대조한다"를 확인한다
