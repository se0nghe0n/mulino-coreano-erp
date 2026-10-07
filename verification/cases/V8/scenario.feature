# language: ko
@V8 @D23 @D24 @D26 @contract-red @sit
기능: 빈 설치와 upgrade 복원 및 환경별 실제 인수를 대조한다

  시나리오: exact manifest로 빈 DB를 설치하고 CQN read/action과 outbox를 검증한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "fresh-install-manifest-cqn"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "install" 행동을 수행한다
    만일 "시스템" 역할이 "install-inspect" 행동을 수행한다
    만일 "warehouse" 역할이 "receipt" 행동을 수행한다
    만일 "reader" 역할이 "read-cqn" 행동을 수행한다
    만일 "reader" 역할이 "read-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "Flyway-only-DDL" assertion으로 "Flyway only DDL"를 확인한다
    그러면 "처음-빈-DB" assertion으로 "처음 빈 DB"를 확인한다
    그러면 "정확-manifest-component-set" assertion으로 "정확 manifest component set"를 확인한다
    그러면 "버전-manifest-근거" assertion으로 "버전 manifest 근거"를 확인한다
    그러면 "stack-확정-runtime-증거" assertion으로 "stack 확정 runtime 증거"를 확인한다
    그러면 "lock-manifest-실제-runtime-동일" assertion으로 "lock manifest 실제 runtime 동일"를 확인한다
    그러면 "manifest-exact-후보버전" assertion으로 "manifest exact 후보버전"를 확인한다
    그러면 "CQN-수령-원장" assertion으로 "CQN 수령 원장"를 확인한다
    그러면 "신규-수령60" assertion으로 "신규 수령60"를 확인한다
    그러면 "MCP-CQN-동일-response" assertion으로 "MCP CQN 동일 response"를 확인한다
    그러면 "원자적-감사-연결" assertion으로 "원자적 감사 연결"를 확인한다
    그러면 "원자적-outbox-연결" assertion으로 "원자적 outbox 연결"를 확인한다
    그러면 "멱등-COMMITTED" assertion으로 "멱등 COMMITTED"를 확인한다

  시나리오: 빈 설치의 실제 조직 FK 수량제약과 custom endpoint 인가를 검증한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "fresh-constraints-auth"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "install" 행동을 수행한다
    만일 "시스템" 역할이 "install-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "constraints" 행동을 수행한다
    만일 "시스템" 역할이 "constraints-inspect" 행동을 수행한다
    만일 "outsider" 역할이 "unauthorized-endpoint" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "조직-FK-23503" assertion으로 "조직 FK 23503"를 확인한다
    그러면 "custom-MCP-현재인가" assertion으로 "custom MCP 현재인가"를 확인한다
    그러면 "무권한효과0-segments" assertion으로 "무권한효과0 segments"를 확인한다
    그러면 "무권한효과0-movements" assertion으로 "무권한효과0 movements"를 확인한다
    그러면 "무권한효과0-allocations" assertion으로 "무권한효과0 allocations"를 확인한다
    그러면 "무권한효과0-works" assertion으로 "무권한효과0 works"를 확인한다
    그러면 "무권한효과0-approvals" assertion으로 "무권한효과0 approvals"를 확인한다
    그러면 "무권한효과0-outbox" assertion으로 "무권한효과0 outbox"를 확인한다
    그러면 "허용-denial-audit" assertion으로 "허용 denial audit"를 확인한다

  시나리오: 감사 outbox 멱등 결과 실패는 하나의 업무 transaction 전체를 rollback한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "transaction-rollback-all-effects"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "fail-persistence" 행동을 수행한다
    만일 "warehouse" 역할이 "split-audit" 행동을 수행한다
    만일 "warehouse" 역할이 "split-outbox" 행동을 수행한다
    만일 "warehouse" 역할이 "split-idempotency" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "rollback-금지효과0-segments" assertion으로 "rollback 금지효과0 segments"를 확인한다
    그러면 "rollback-금지효과0-genealogy" assertion으로 "rollback 금지효과0 genealogy"를 확인한다
    그러면 "rollback-금지효과0-movements" assertion으로 "rollback 금지효과0 movements"를 확인한다
    그러면 "rollback-금지효과0-allocations" assertion으로 "rollback 금지효과0 allocations"를 확인한다
    그러면 "rollback-금지효과0-obligations" assertion으로 "rollback 금지효과0 obligations"를 확인한다
    그러면 "rollback-금지효과0-audit" assertion으로 "rollback 금지효과0 audit"를 확인한다
    그러면 "rollback-금지효과0-outbox" assertion으로 "rollback 금지효과0 outbox"를 확인한다
    그러면 "rollback-금지효과0-idempotency" assertion으로 "rollback 금지효과0 idempotency"를 확인한다
    그러면 "rollback-실패응답-audit" assertion으로 "rollback 실패응답 audit"를 확인한다
    그러면 "rollback-실패응답-outbox" assertion으로 "rollback 실패응답 outbox"를 확인한다
    그러면 "rollback-실패응답-idempotency" assertion으로 "rollback 실패응답 idempotency"를 확인한다
    그러면 "rollback-수량변화0" assertion으로 "rollback 수량변화0"를 확인한다

  시나리오: 실제 두 transaction의 scope 잠금은 예약40과 신규30의 초과소비를 막는다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "real-lock-two-transactions"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "reserve40" 행동을 수행한다
    만일 "시스템" 역할이 "reserve40-reached" 행동을 수행한다
    만일 "시스템" 역할이 "reserve30" 행동을 수행한다
    만일 "시스템" 역할이 "reserve30-reached" 행동을 수행한다
    만일 "시스템" 역할이 "resume30" 행동을 수행한다
    만일 "시스템" 역할이 "waiter-before-release" 행동을 수행한다
    만일 "시스템" 역할이 "resume40" 행동을 수행한다
    만일 "시스템" 역할이 "terminal40" 행동을 수행한다
    만일 "시스템" 역할이 "terminal30" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "동시-실제-DB-lock-WAIT" assertion으로 "동시 실제 DB lock WAIT"를 확인한다
    그러면 "WAIT-시점-holder와-waiter-거래-open" assertion으로 "WAIT 시점 holder와 waiter 거래 open"를 확인한다
    그러면 "서로다른-실제-DB-transaction" assertion으로 "서로다른 실제 DB transaction"를 확인한다
    그러면 "예약40-commit" assertion으로 "예약40 commit"를 확인한다
    그러면 "예약30-현재revision-거부" assertion으로 "예약30 현재revision 거부"를 확인한다
    그러면 "waiter-잠금후-현재revision-재검증" assertion으로 "waiter 잠금후 현재revision 재검증"를 확인한다
    그러면 "실물-잠금전후-불변" assertion으로 "실물 잠금전후 불변"를 확인한다
    그러면 "실제-독립-transaction" assertion으로 "실제 독립 transaction"를 확인한다
    그러면 "잠금-fence-원범위" assertion으로 "잠금 fence 원범위"를 확인한다
    그러면 "첫-reserve-40-효과" assertion으로 "첫 reserve 40 효과"를 확인한다
    그러면 "기존20+새40-실행배분60" assertion으로 "기존20+새40 실행배분60"를 확인한다
    그러면 "기존-부족40-책임보존" assertion으로 "기존 부족40 책임보존"를 확인한다

  시나리오: 새 ontology v1에서 v2로 upgrade해 진행 업무 물량 의무와 판정 의미를 보존한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "ontology-v1-v2-preserves"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "assessment-before" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade-inspect" 행동을 수행한다
    만일 "reader" 역할이 "assessment-after" 행동을 수행한다
    만일 "reader" 역할이 "inventory-after" 행동을 수행한다
    만일 "reader" 역할이 "duties-after" 행동을 수행한다
    만일 "reader" 역할이 "mcp-after" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "보존-segments" assertion으로 "보존 segments"를 확인한다
    그러면 "보존-genealogy" assertion으로 "보존 genealogy"를 확인한다
    그러면 "보존-works" assertion으로 "보존 works"를 확인한다
    그러면 "보존-goalVersions" assertion으로 "보존 goalVersions"를 확인한다
    그러면 "보존-assessments" assertion으로 "보존 assessments"를 확인한다
    그러면 "보존-obligations" assertion으로 "보존 obligations"를 확인한다
    그러면 "보존-definitions" assertion으로 "보존 definitions"를 확인한다
    그러면 "보존-evidence" assertion으로 "보존 evidence"를 확인한다
    그러면 "보존-idempotency" assertion으로 "보존 idempotency"를 확인한다
    그러면 "보존-allocations" assertion으로 "보존 allocations"를 확인한다
    그러면 "현재-W-60" assertion으로 "현재 W 60"를 확인한다
    그러면 "식별된-실물-100" assertion으로 "식별된 실물 100"를 확인한다
    그러면 "실물-identity-중복0" assertion으로 "실물 identity 중복0"를 확인한다
    그러면 "원-LOT-관계" assertion으로 "원 LOT 관계"를 확인한다
    그러면 "진행-업무-WAITING" assertion으로 "진행 업무 WAITING"를 확인한다
    그러면 "v1-정의" assertion으로 "v1 정의"를 확인한다
    그러면 "v1-evaluator" assertion으로 "v1 evaluator"를 확인한다
    그러면 "v1-도착-미충족" assertion으로 "v1 도착 미충족"를 확인한다
    그러면 "v1-인정-도착60" assertion으로 "v1 인정 도착60"를 확인한다
    그러면 "남은40-인간-owner" assertion으로 "남은40 인간 owner"를 확인한다
    그러면 "원문-증거-hash" assertion으로 "원문 증거 hash"를 확인한다
    그러면 "원-수령-멱등결과" assertion으로 "원 수령 멱등결과"를 확인한다
    그러면 "API-v1-판정-동일" assertion으로 "API v1 판정 동일"를 확인한다
    그러면 "MCP-v1-판정-동일" assertion으로 "MCP v1 판정 동일"를 확인한다
    그러면 "새-schema-family" assertion으로 "새 schema family"를 확인한다
    그러면 "옛-migration-source0" assertion으로 "옛 migration source0"를 확인한다
    그러면 "upgrade-재인수-probes" assertion으로 "upgrade 재인수 probes"를 확인한다

  시나리오: DB blob 정의 evaluator와 인가를 복원한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "restore-none"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "assessment-before" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "backup-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "restore-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "reader" 역할이 "assessment-after" 행동을 수행한다
    만일 "reader" 역할이 "evidence-after" 행동을 수행한다
    그러면 "bundle-전수-종류" assertion으로 "bundle 전수 종류"를 확인한다
    그러면 "삭제전-백업-원본-positive" assertion으로 "삭제전 백업 원본 positive"를 확인한다
    그러면 "백업-원문-hash" assertion으로 "백업 원문 hash"를 확인한다
    그러면 "보존-segments" assertion으로 "보존 segments"를 확인한다
    그러면 "보존-genealogy" assertion으로 "보존 genealogy"를 확인한다
    그러면 "보존-works" assertion으로 "보존 works"를 확인한다
    그러면 "보존-goalVersions" assertion으로 "보존 goalVersions"를 확인한다
    그러면 "보존-assessments" assertion으로 "보존 assessments"를 확인한다
    그러면 "보존-obligations" assertion으로 "보존 obligations"를 확인한다
    그러면 "보존-definitions" assertion으로 "보존 definitions"를 확인한다
    그러면 "보존-evidence" assertion으로 "보존 evidence"를 확인한다
    그러면 "보존-idempotency" assertion으로 "보존 idempotency"를 확인한다
    그러면 "보존-allocations" assertion으로 "보존 allocations"를 확인한다
    그러면 "현재-W-60" assertion으로 "현재 W 60"를 확인한다
    그러면 "식별된-실물-100" assertion으로 "식별된 실물 100"를 확인한다
    그러면 "실물-identity-중복0" assertion으로 "실물 identity 중복0"를 확인한다
    그러면 "원-LOT-관계" assertion으로 "원 LOT 관계"를 확인한다
    그러면 "진행-업무-WAITING" assertion으로 "진행 업무 WAITING"를 확인한다
    그러면 "v1-정의" assertion으로 "v1 정의"를 확인한다
    그러면 "v1-evaluator" assertion으로 "v1 evaluator"를 확인한다
    그러면 "v1-도착-미충족" assertion으로 "v1 도착 미충족"를 확인한다
    그러면 "v1-인정-도착60" assertion으로 "v1 인정 도착60"를 확인한다
    그러면 "남은40-인간-owner" assertion으로 "남은40 인간 owner"를 확인한다
    그러면 "원문-증거-hash" assertion으로 "원문 증거 hash"를 확인한다
    그러면 "원-수령-멱등결과" assertion으로 "원 수령 멱등결과"를 확인한다
    그러면 "복원-API-evidence-hash" assertion으로 "복원 API evidence hash"를 확인한다
    그러면 "완전복원-결과" assertion으로 "완전복원 결과"를 확인한다
    그러면 "artifact-hash-대조" assertion으로 "artifact hash 대조"를 확인한다
    그러면 "복원-현재인가" assertion으로 "복원 현재인가"를 확인한다
    그러면 "삭제-이력재적용" assertion으로 "삭제 이력재적용"를 확인한다
    그러면 "deleted-원문-복활0" assertion으로 "deleted 원문 복활0"를 확인한다
    그러면 "legal-hold-보존" assertion으로 "legal hold 보존"를 확인한다
    그러면 "복원-API-v1-의미" assertion으로 "복원 API v1 의미"를 확인한다

  시나리오: 필수 blob 누락 복원을 완료로 표시하지 않는다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "restore-blob"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "assessment-before" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "backup-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "remove-blob" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "restore-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "partial-db" 행동을 수행한다
    만일 "reader" 역할이 "partial-access" 행동을 수행한다
    그러면 "bundle-전수-종류" assertion으로 "bundle 전수 종류"를 확인한다
    그러면 "삭제전-백업-원본-positive" assertion으로 "삭제전 백업 원본 positive"를 확인한다
    그러면 "백업-원문-hash" assertion으로 "백업 원문 hash"를 확인한다
    그러면 "partial-API-write차단" assertion으로 "partial API write차단"를 확인한다
    그러면 "partial-DB-incomplete" assertion으로 "partial DB incomplete"를 확인한다
    그러면 "partial-DB-completion0" assertion으로 "partial DB completion0"를 확인한다
    그러면 "partial-DB-recovery-owner" assertion으로 "partial DB recovery owner"를 확인한다
    그러면 "불완전복원-결과" assertion으로 "불완전복원 결과"를 확인한다
    그러면 "누락-kind" assertion으로 "누락 kind"를 확인한다
    그러면 "허위-완료-record0" assertion으로 "허위 완료 record0"를 확인한다
    그러면 "운영쓰기-차단" assertion으로 "운영쓰기 차단"를 확인한다
    그러면 "복구-인간책임" assertion으로 "복구 인간책임"를 확인한다

  시나리오: 필수 evaluator 누락 복원을 완료로 표시하지 않는다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "restore-evaluator"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "assessment-before" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "backup-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "remove-evaluator" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "restore-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "partial-db" 행동을 수행한다
    만일 "reader" 역할이 "partial-access" 행동을 수행한다
    그러면 "bundle-전수-종류" assertion으로 "bundle 전수 종류"를 확인한다
    그러면 "삭제전-백업-원본-positive" assertion으로 "삭제전 백업 원본 positive"를 확인한다
    그러면 "백업-원문-hash" assertion으로 "백업 원문 hash"를 확인한다
    그러면 "partial-API-write차단" assertion으로 "partial API write차단"를 확인한다
    그러면 "partial-DB-incomplete" assertion으로 "partial DB incomplete"를 확인한다
    그러면 "partial-DB-completion0" assertion으로 "partial DB completion0"를 확인한다
    그러면 "partial-DB-recovery-owner" assertion으로 "partial DB recovery owner"를 확인한다
    그러면 "불완전복원-결과" assertion으로 "불완전복원 결과"를 확인한다
    그러면 "누락-kind" assertion으로 "누락 kind"를 확인한다
    그러면 "허위-완료-record0" assertion으로 "허위 완료 record0"를 확인한다
    그러면 "운영쓰기-차단" assertion으로 "운영쓰기 차단"를 확인한다
    그러면 "복구-인간책임" assertion으로 "복구 인간책임"를 확인한다

  시나리오: 실제 BTP의 entitlement runtime binding TLS 현재 인가와 MCP wire를 인수한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "btp-auth-binding-tls-wire"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "btp" 행동을 수행한다
    만일 "시스템" 역할이 "btp-inspect" 행동을 수행한다
    그러면 "실제-BTP-profile" assertion으로 "실제 BTP profile"를 확인한다
    그러면 "R7-region-entitlement-cost" assertion으로 "R7 region entitlement cost"를 확인한다
    그러면 "실제-runtime-binding" assertion으로 "실제 runtime binding"를 확인한다
    그러면 "현재-token-인가" assertion으로 "현재 token 인가"를 확인한다
    그러면 "TLS-검증결과" assertion으로 "TLS 검증결과"를 확인한다
    그러면 "실제-최신-MCP-wire" assertion으로 "실제 최신 MCP wire"를 확인한다
    그러면 "BTP-wire-header-version" assertion으로 "BTP wire header version"를 확인한다
    그러면 "BTP-wire-body-version" assertion으로 "BTP wire body version"를 확인한다
    그러면 "BTP-raw-transcript-link" assertion으로 "BTP raw transcript link"를 확인한다
    그러면 "BTP-wire-효과refs" assertion으로 "BTP wire 효과refs"를 확인한다

  시나리오: 대상 실제 client의 발견 loading과 tool 왕복을 local BTP와 분리한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "supported-client-separate"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "client" 행동을 수행한다
    만일 "시스템" 역할이 "client-inspect" 행동을 수행한다
    그러면 "실제-client-profile" assertion으로 "실제 client profile"를 확인한다
    그러면 "실제-client-version-source" assertion으로 "실제 client version source"를 확인한다
    그러면 "실제-client-wire" assertion으로 "실제 client wire"를 확인한다
    그러면 "client-wire-header-version" assertion으로 "client wire header version"를 확인한다
    그러면 "client-wire-body-version" assertion으로 "client wire body version"를 확인한다
    그러면 "client-raw-transcript-link" assertion으로 "client raw transcript link"를 확인한다
    그러면 "client-skill-loading-artifact" assertion으로 "client skill loading artifact"를 확인한다
    그러면 "profile-결과-각각" assertion으로 "profile 결과 각각"를 확인한다
    그러면 "client-성공의-BTP-참조0" assertion으로 "client 성공의 BTP 참조0"를 확인한다

  시나리오: 필수 BTP client 부재와 미검증 후보는 NOT_RUN으로 남긴다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "unavailable-environment-gates"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "reader" 역할이 "local-witness" 행동을 수행한다
    만일 "시스템" 역할이 "disable-external-bindings" 행동을 수행한다
    만일 "시스템" 역할이 "gate-inventory" 행동을 수행한다
    만일 "시스템" 역할이 "gate-inventory-inspect" 행동을 수행한다
    그러면 "로컬-관찰-보유60" assertion으로 "로컬 관찰 보유60"를 확인한다
    그러면 "환경별-NOT_RUN" assertion으로 "환경별 NOT_RUN"를 확인한다
    그러면 "필수-gate-생략0" assertion으로 "필수 gate 생략0"를 확인한다
    그러면 "candidate-최종확정0" assertion으로 "candidate 최종확정0"를 확인한다
    그러면 "대안-oracle-동일" assertion으로 "대안 oracle 동일"를 확인한다
    그러면 "미정-계정-비용-실행0" assertion으로 "미정 계정 비용 실행0"를 확인한다

  시나리오: upgrade 뒤 제약 인가 rollback 잠금과 transactional outbox를 다시 인수한다
    먼저 사례 파일 "verification/cases/V8/case.json"의 "upgrade-revalidate-transactions"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "constraints" 행동을 수행한다
    만일 "시스템" 역할이 "constraints-inspect" 행동을 수행한다
    만일 "outsider" 역할이 "unauthorized" 행동을 수행한다
    만일 "시스템" 역할이 "rollback-before" 행동을 수행한다
    만일 "시스템" 역할이 "rollback-fail-persistence" 행동을 수행한다
    만일 "warehouse" 역할이 "rollback-split-audit" 행동을 수행한다
    만일 "warehouse" 역할이 "rollback-split-outbox" 행동을 수행한다
    만일 "warehouse" 역할이 "rollback-split-idempotency" 행동을 수행한다
    만일 "시스템" 역할이 "rollback-after" 행동을 수행한다
    만일 "시스템" 역할이 "lock-before" 행동을 수행한다
    만일 "시스템" 역할이 "lock-reserve40" 행동을 수행한다
    만일 "시스템" 역할이 "lock-reserve40-reached" 행동을 수행한다
    만일 "시스템" 역할이 "lock-reserve30" 행동을 수행한다
    만일 "시스템" 역할이 "lock-reserve30-reached" 행동을 수행한다
    만일 "시스템" 역할이 "lock-resume30" 행동을 수행한다
    만일 "시스템" 역할이 "lock-waiter-before-release" 행동을 수행한다
    만일 "시스템" 역할이 "lock-resume40" 행동을 수행한다
    만일 "시스템" 역할이 "lock-terminal40" 행동을 수행한다
    만일 "시스템" 역할이 "lock-terminal30" 행동을 수행한다
    만일 "시스템" 역할이 "lock-after" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade-final" 행동을 수행한다
    그러면 "upgrade-실제-constraint-SQLSTATE" assertion으로 "upgrade 실제 constraint SQLSTATE"를 확인한다
    그러면 "upgrade-실제-MCP-인가" assertion으로 "upgrade 실제 MCP 인가"를 확인한다
    그러면 "rollback-rollback-금지효과0-segments" assertion으로 "rollback 금지효과0 segments"를 확인한다
    그러면 "rollback-rollback-금지효과0-genealogy" assertion으로 "rollback 금지효과0 genealogy"를 확인한다
    그러면 "rollback-rollback-금지효과0-movements" assertion으로 "rollback 금지효과0 movements"를 확인한다
    그러면 "rollback-rollback-금지효과0-allocations" assertion으로 "rollback 금지효과0 allocations"를 확인한다
    그러면 "rollback-rollback-금지효과0-obligations" assertion으로 "rollback 금지효과0 obligations"를 확인한다
    그러면 "rollback-rollback-금지효과0-audit" assertion으로 "rollback 금지효과0 audit"를 확인한다
    그러면 "rollback-rollback-금지효과0-outbox" assertion으로 "rollback 금지효과0 outbox"를 확인한다
    그러면 "rollback-rollback-금지효과0-idempotency" assertion으로 "rollback 금지효과0 idempotency"를 확인한다
    그러면 "rollback-rollback-실패응답-audit" assertion으로 "rollback 실패응답 audit"를 확인한다
    그러면 "rollback-rollback-실패응답-outbox" assertion으로 "rollback 실패응답 outbox"를 확인한다
    그러면 "rollback-rollback-실패응답-idempotency" assertion으로 "rollback 실패응답 idempotency"를 확인한다
    그러면 "rollback-rollback-수량변화0" assertion으로 "rollback 수량변화0"를 확인한다
    그러면 "lock-동시-실제-DB-lock-WAIT" assertion으로 "동시 실제 DB lock WAIT"를 확인한다
    그러면 "lock-WAIT-시점-holder와-waiter-거래-open" assertion으로 "WAIT 시점 holder와 waiter 거래 open"를 확인한다
    그러면 "lock-서로다른-실제-DB-transaction" assertion으로 "서로다른 실제 DB transaction"를 확인한다
    그러면 "lock-예약40-commit" assertion으로 "예약40 commit"를 확인한다
    그러면 "lock-예약30-현재revision-거부" assertion으로 "예약30 현재revision 거부"를 확인한다
    그러면 "lock-waiter-잠금후-현재revision-재검증" assertion으로 "waiter 잠금후 현재revision 재검증"를 확인한다
    그러면 "lock-실물-잠금전후-불변" assertion으로 "실물 잠금전후 불변"를 확인한다
    그러면 "lock-실제-독립-transaction" assertion으로 "실제 독립 transaction"를 확인한다
    그러면 "lock-잠금-fence-원범위" assertion으로 "잠금 fence 원범위"를 확인한다
    그러면 "lock-첫-reserve-40-효과" assertion으로 "첫 reserve 40 효과"를 확인한다
    그러면 "lock-기존20+새40-실행배분60" assertion으로 "기존20+새40 실행배분60"를 확인한다
    그러면 "lock-기존-부족40-책임보존" assertion으로 "기존 부족40 책임보존"를 확인한다
    그러면 "upgrade-outbox-command-일치" assertion으로 "upgrade outbox command 일치"를 확인한다
