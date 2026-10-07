# language: ko
@harness-example @contract-red
기능: 독립적인 수량과 책임 인수 계약
  시나리오: 보류는 실물 감소가 아니다
    먼저 사례 파일 "verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"의 "hold-preserves-physical"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "before" 행동을 수행한다
    만일 "qc" 역할이 "hold" 행동을 수행한다
    만일 "qc" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "held-100" assertion으로 "Setup60+40=100. A hold changes availability without physical reduction."를 확인한다
    그러면 "eligible-80" assertion으로 "100−20=80, using fixed synthetic distinct scope."를 확인한다
    그러면 "eligible-delta" assertion으로 "After80−before100=−20."를 확인한다
    그러면 "physical-unique" assertion으로 "No physical scope counted twice."를 확인한다
    그러면 "segment-set" assertion으로 "No parent or unexplained segment counted."를 확인한다
    그러면 "relation-set" assertion으로 "Hold relation applies to exactly20 withinA60."를 확인한다
    그러면 "owner-qc" assertion으로 "Open restriction responsibility stays with configured human qc."를 확인한다
    그러면 "next-check" assertion으로 "Deadline uses a fixed UTC instant."를 확인한다
    그러면 "version-v1" assertion으로 "Current response preserves the active scenario definition."를 확인한다
    그러면 "obligation-fields" assertion으로 "Responsibility has no owner/action/deadline gap."를 확인한다
    그러면 "raw-segment-sum" assertion으로 "Independent rows60+40=100."를 확인한다
