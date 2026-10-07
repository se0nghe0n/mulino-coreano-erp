# language: ko
@T23 @D23 @contract-red @sit
기능: 자료를 보존하고 새 schema와 cutover의 경계를 검증한다

  시나리오: 파일 inventory와 보존 ref를 isolated 환경에서 hash로 복원한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "archive-inventory-roundtrip"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "inventory" 행동을 수행한다
    만일 "시스템" 역할이 "inventory-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "restore-archive" 행동을 수행한다
    만일 "시스템" 역할이 "restore-archive-inspect" 행동을 수행한다
    그러면 "전수-inventory-fields" assertion으로 "전수 inventory fields"를 확인한다
    그러면 "현재-track경로-전수" assertion으로 "현재 track경로 전수"를 확인한다
    그러면 "원-tree-hash-복원" assertion으로 "원 tree hash 복원"를 확인한다
    그러면 "원-파일-hash-대조" assertion으로 "원 파일 hash 대조"를 확인한다
    그러면 "역사-명령-active-참조0" assertion으로 "역사 명령 active 참조0"를 확인한다
    그러면 "archive-내용-재사용0" assertion으로 "archive 내용 재사용0"를 확인한다
    그러면 "보존-commit" assertion으로 "보존 commit"를 확인한다

  시나리오: authoritative 자료 부재를 확인해 독립 DB에서 시작한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "data-fresh"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "data-inventory" 행동을 수행한다
    만일 "시스템" 역할이 "data-inventory-inspect" 행동을 수행한다
    그러면 "네-source-분류-검증" assertion으로 "네 source 분류 검증"를 확인한다
    그러면 "운영-자료-가상-분리" assertion으로 "운영 자료 가상 분리"를 확인한다
    그러면 "원-제조-import-relabel0" assertion으로 "원 제조 import relabel0"를 확인한다
    그러면 "authoritative-원천0" assertion으로 "authoritative 원천0"를 확인한다
    그러면 "fresh-분기" assertion으로 "fresh 분기"를 확인한다
    그러면 "임의-legacy-migration0" assertion으로 "임의 legacy migration0"를 확인한다

  시나리오: authoritative 자료 snapshot과 원본을 보존하고 mapping과 격리 수량·책임을 대조한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "data-live"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "data-inventory" 행동을 수행한다
    만일 "시스템" 역할이 "data-inventory-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "confirm-mapping" 행동을 수행한다
    만일 "시스템" 역할이 "confirm-mapping-inspect" 행동을 수행한다
    그러면 "네-source-분류-검증" assertion으로 "네 source 분류 검증"를 확인한다
    그러면 "운영-자료-가상-분리" assertion으로 "운영 자료 가상 분리"를 확인한다
    그러면 "원-제조-import-relabel0" assertion으로 "원 제조 import relabel0"를 확인한다
    그러면 "live-분기" assertion으로 "live 분기"를 확인한다
    그러면 "읽기전용-archive" assertion으로 "읽기전용 archive"를 확인한다
    그러면 "미대응-제조원본-읽기유지" assertion으로 "미대응 제조원본 읽기유지"를 확인한다
    그러면 "원본70-대조" assertion으로 "원본70 대조"를 확인한다
    그러면 "누락물량0" assertion으로 "누락물량0"를 확인한다
    그러면 "의무-미손실" assertion으로 "의무 미손실"를 확인한다
    그러면 "권한자-전환검토" assertion으로 "권한자 전환검토"를 확인한다

  시나리오: Flyway 단독 소유와 고정 compiler의 fresh schema 차이를 검증한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "schema-fresh"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "install" 행동을 수행한다
    만일 "시스템" 역할이 "install-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "compile" 행동을 수행한다
    만일 "시스템" 역할이 "compile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "compare" 행동을 수행한다
    만일 "시스템" 역할이 "compare-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "유일-DDL-writer" assertion으로 "유일 DDL writer"를 확인한다
    그러면 "CAP-auto-deployer0" assertion으로 "CAP auto deployer0"를 확인한다
    그러면 "Boot-SQL-init-never" assertion으로 "Boot SQL init never"를 확인한다
    그러면 "단일-CQN-model" assertion으로 "단일 CQN model"를 확인한다
    그러면 "고정-compiler-version" assertion으로 "고정 compiler version"를 확인한다
    그러면 "custom-제약-allowlist-version" assertion으로 "custom 제약 allowlist version"를 확인한다
    그러면 "허용-schema-delta" assertion으로 "허용 schema delta"를 확인한다
    그러면 "schema-drift-결과" assertion으로 "schema drift 결과"를 확인한다
    그러면 "비허용-delta" assertion으로 "비허용 delta"를 확인한다
    그러면 "FK-quantity-outbox-CQN-원검사" assertion으로 "FK quantity outbox CQN 원검사"를 확인한다

  시나리오: Flyway 단독 소유와 고정 compiler의 upgrade schema 차이를 검증한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "schema-upgrade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "compile" 행동을 수행한다
    만일 "시스템" 역할이 "compile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "compare" 행동을 수행한다
    만일 "시스템" 역할이 "compare-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "유일-DDL-writer" assertion으로 "유일 DDL writer"를 확인한다
    그러면 "CAP-auto-deployer0" assertion으로 "CAP auto deployer0"를 확인한다
    그러면 "Boot-SQL-init-never" assertion으로 "Boot SQL init never"를 확인한다
    그러면 "단일-CQN-model" assertion으로 "단일 CQN model"를 확인한다
    그러면 "고정-compiler-version" assertion으로 "고정 compiler version"를 확인한다
    그러면 "custom-제약-allowlist-version" assertion으로 "custom 제약 allowlist version"를 확인한다
    그러면 "허용-schema-delta" assertion으로 "허용 schema delta"를 확인한다
    그러면 "schema-drift-결과" assertion으로 "schema drift 결과"를 확인한다
    그러면 "비허용-delta" assertion으로 "비허용 delta"를 확인한다
    그러면 "FK-quantity-outbox-CQN-원검사" assertion으로 "FK quantity outbox CQN 원검사"를 확인한다
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

  시나리오: Flyway 단독 소유와 고정 compiler의 unexpected-drift schema 차이를 검증한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "schema-unexpected-drift"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade" 행동을 수행한다
    만일 "시스템" 역할이 "upgrade-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "inject-drift" 행동을 수행한다
    만일 "시스템" 역할이 "compile" 행동을 수행한다
    만일 "시스템" 역할이 "compile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "compare" 행동을 수행한다
    만일 "시스템" 역할이 "compare-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "유일-DDL-writer" assertion으로 "유일 DDL writer"를 확인한다
    그러면 "CAP-auto-deployer0" assertion으로 "CAP auto deployer0"를 확인한다
    그러면 "Boot-SQL-init-never" assertion으로 "Boot SQL init never"를 확인한다
    그러면 "단일-CQN-model" assertion으로 "단일 CQN model"를 확인한다
    그러면 "고정-compiler-version" assertion으로 "고정 compiler version"를 확인한다
    그러면 "custom-제약-allowlist-version" assertion으로 "custom 제약 allowlist version"를 확인한다
    그러면 "허용-schema-delta" assertion으로 "허용 schema delta"를 확인한다
    그러면 "schema-drift-결과" assertion으로 "schema drift 결과"를 확인한다
    그러면 "비허용-delta" assertion으로 "비허용 delta"를 확인한다
    그러면 "FK-quantity-outbox-CQN-원검사" assertion으로 "FK quantity outbox CQN 원검사"를 확인한다
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

  시나리오: freeze부터 smoke와 인가 재개까지 확인한 뒤 쓰기를 연다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "cutover-open-order"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "stage-WRITE_FREEZE" assertion으로 "stage WRITE_FREEZE"를 확인한다
    그러면 "stage-FINAL_SNAPSHOT" assertion으로 "stage FINAL_SNAPSHOT"를 확인한다
    그러면 "stage-RECONCILE" assertion으로 "stage RECONCILE"를 확인한다
    그러면 "stage-APPLY_VERSION" assertion으로 "stage APPLY_VERSION"를 확인한다
    그러면 "stage-SMOKE_AUTH_RESUME" assertion으로 "stage SMOKE_AUTH_RESUME"를 확인한다
    그러면 "stage-OPEN_WRITES" assertion으로 "stage OPEN_WRITES"를 확인한다
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
    그러면 "쓰기개방-선행-stage" assertion으로 "쓰기개방 선행 stage"를 확인한다
    그러면 "개방-전-수량차이0" assertion으로 "개방 전 수량차이0"를 확인한다
    그러면 "개방-전-의무차이0" assertion으로 "개방 전 의무차이0"를 확인한다
    그러면 "snapshot-호환-artifact-종류" assertion으로 "snapshot 호환 artifact 종류"를 확인한다
    그러면 "인가-재개-smoke" assertion으로 "인가 재개 smoke"를 확인한다

  시나리오: 쓰기 개방 전 실패는 검증된 snapshot과 호환 artifact로 복구한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "failure-before-open"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "fail-smoke" 행동을 수행한다
    만일 "시스템" 역할이 "rollback" 행동을 수행한다
    만일 "시스템" 역할이 "rollback-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "stage-WRITE_FREEZE" assertion으로 "stage WRITE_FREEZE"를 확인한다
    그러면 "stage-FINAL_SNAPSHOT" assertion으로 "stage FINAL_SNAPSHOT"를 확인한다
    그러면 "stage-RECONCILE" assertion으로 "stage RECONCILE"를 확인한다
    그러면 "stage-APPLY_VERSION" assertion으로 "stage APPLY_VERSION"를 확인한다
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
    그러면 "개방전-복구-mode" assertion으로 "개방전 복구 mode"를 확인한다
    그러면 "개방전-write상태" assertion으로 "개방전 write상태"를 확인한다
    그러면 "실물계약-SQL취소0" assertion으로 "실물계약 SQL취소0"를 확인한다

  시나리오: 새 쓰기와 외부효과 뒤 forward 복구는 명령과 외부결과를 대조한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "failure-after-open-forward"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes-inspect" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approve" 행동을 수행한다
    만일 "시스템" 역할이 "external-loss" 행동을 수행한다
    만일 "procurement" 역할이 "send-order" 행동을 수행한다
    만일 "시스템" 역할이 "after-write" 행동을 수행한다
    만일 "시스템" 역할이 "stop-reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "stop-reconcile-inspect" 행동을 수행한다
    만일 "procurement" 역할이 "external-reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "repair" 행동을 수행한다
    만일 "시스템" 역할이 "repair-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "stage-WRITE_FREEZE" assertion으로 "stage WRITE_FREEZE"를 확인한다
    그러면 "stage-FINAL_SNAPSHOT" assertion으로 "stage FINAL_SNAPSHOT"를 확인한다
    그러면 "stage-RECONCILE" assertion으로 "stage RECONCILE"를 확인한다
    그러면 "stage-APPLY_VERSION" assertion으로 "stage APPLY_VERSION"를 확인한다
    그러면 "stage-SMOKE_AUTH_RESUME" assertion으로 "stage SMOKE_AUTH_RESUME"를 확인한다
    그러면 "stage-OPEN_WRITES" assertion으로 "stage OPEN_WRITES"를 확인한다
    그러면 "접수중지" assertion으로 "접수중지"를 확인한다
    그러면 "outbox-freeze" assertion으로 "outbox freeze"를 확인한다
    그러면 "외부결과-대조" assertion으로 "외부결과 대조"를 확인한다
    그러면 "명시-replay-compensation" assertion으로 "명시 replay compensation"를 확인한다
    그러면 "실물계약-취소주장0" assertion으로 "실물계약 취소주장0"를 확인한다
    그러면 "복구후-물량불변" assertion으로 "복구후 물량불변"를 확인한다
    그러면 "외부-재발행0" assertion으로 "외부 재발행0"를 확인한다
    그러면 "새-구매-기록보존" assertion으로 "새 구매 기록보존"를 확인한다
    그러면 "대조-책임-유지" assertion으로 "대조 책임 유지"를 확인한다

  시나리오: 새 쓰기와 외부효과 뒤 approved-snapshot 복구는 명령과 외부결과를 대조한다
    먼저 사례 파일 "verification/cases/T23/case.json"의 "failure-after-open-approved-snapshot"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze" 행동을 수행한다
    만일 "시스템" 역할이 "write_freeze-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "final_snapshot-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "reconcile-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version" 행동을 수행한다
    만일 "시스템" 역할이 "apply_version-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume" 행동을 수행한다
    만일 "시스템" 역할이 "smoke_auth_resume-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes" 행동을 수행한다
    만일 "시스템" 역할이 "open_writes-inspect" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approve" 행동을 수행한다
    만일 "시스템" 역할이 "external-loss" 행동을 수행한다
    만일 "procurement" 역할이 "send-order" 행동을 수행한다
    만일 "시스템" 역할이 "after-write" 행동을 수행한다
    만일 "시스템" 역할이 "stop-reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "stop-reconcile-inspect" 행동을 수행한다
    만일 "procurement" 역할이 "external-reconcile" 행동을 수행한다
    만일 "시스템" 역할이 "repair" 행동을 수행한다
    만일 "시스템" 역할이 "repair-inspect" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "stage-WRITE_FREEZE" assertion으로 "stage WRITE_FREEZE"를 확인한다
    그러면 "stage-FINAL_SNAPSHOT" assertion으로 "stage FINAL_SNAPSHOT"를 확인한다
    그러면 "stage-RECONCILE" assertion으로 "stage RECONCILE"를 확인한다
    그러면 "stage-APPLY_VERSION" assertion으로 "stage APPLY_VERSION"를 확인한다
    그러면 "stage-SMOKE_AUTH_RESUME" assertion으로 "stage SMOKE_AUTH_RESUME"를 확인한다
    그러면 "stage-OPEN_WRITES" assertion으로 "stage OPEN_WRITES"를 확인한다
    그러면 "접수중지" assertion으로 "접수중지"를 확인한다
    그러면 "outbox-freeze" assertion으로 "outbox freeze"를 확인한다
    그러면 "외부결과-대조" assertion으로 "외부결과 대조"를 확인한다
    그러면 "명시-replay-compensation" assertion으로 "명시 replay compensation"를 확인한다
    그러면 "실물계약-취소주장0" assertion으로 "실물계약 취소주장0"를 확인한다
    그러면 "복구후-물량불변" assertion으로 "복구후 물량불변"를 확인한다
    그러면 "외부-재발행0" assertion으로 "외부 재발행0"를 확인한다
    그러면 "새-구매-기록보존" assertion으로 "새 구매 기록보존"를 확인한다
    그러면 "대조-책임-유지" assertion으로 "대조 책임 유지"를 확인한다
