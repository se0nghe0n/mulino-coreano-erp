# language: ko
@harness-selftest
기능: 한국어 Gherkin과 독립 assertion이 discovery된다
  시나리오: 고정 표본은 제품 실행 증거가 아니다
    먼저 고정 관찰 표본과 독립 기대값을 읽는다
    만일 표본 수량과 책임을 assertion으로 검사한다
    그러면 제품 실행 coverage 없이 11개 assertion 자체가 통과한다
