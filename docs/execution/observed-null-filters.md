# 관찰된 SQL null과 누락 필드의 필터 경계

T11 실제 terminal 관찰에는 ACTIVE Work의 closeReason 컬럼이 null로
들어 있다. 원래 `state=CLOSED AND closeReason=FULFILLED`의 개수0 검사는
이 행을 제외해야 한다. 기존 AssertionEngine은 명시 null도 누락 컬럼으로
간주해 값 비교 전에 실패했다. 필터에서 실제 존재하는 Json null은 값으로
비교하고, 필드 자체가 없으면 계속 실패하도록 고쳤다.

이 변경은 실제 상태·이유·oracle를 수정하지 않는다. ACTIVE와 null을
CLOSED/FULFILLED로 채우거나 누락 행을 삭제하지 않는다. 실제 CLOSED +
FULFILLED 행은 여전히 개수0 위반이다. 명시 null을 선택하는 조건도
실제 존재하는 null에만 일치한다. 다른 조건이 행을 제외하더라도 빠진
필드를 자동으로 인정하지 않으므로 missing-column 검사는 유지한다.

수량의 null 투영, unit 없는 빈 합, scope 불완전, 실행되지 않은 관찰은
그대로 실패한다. expected null의 허용은 where의 값 비교에 한정하며
business quantity·fieldsPresent·exactSet 등의 필수 값 검사를 바꾸지 않는다.

원래 T11 실패와 raw는 native capture audit에 보존한다. 새 반례6개와
기존 AssertionEngine13개·ReferenceResolver19개, 합38개가 실패·오류·
skip0으로 통과했다. 로그·원본 case hash·source/class hash·JUnit은
`.local/resume-20261023/observed-null-filter/`에 남긴다. 실제 native
시나리오 재실행은 아직 NOT_RUN이며 정정 HTTP500과 runtime control,
별도 contribution·책임 공백은 이 수정의 완료 범위가 아니다.
