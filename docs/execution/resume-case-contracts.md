# 재개 시점 case 계약 수정

2026-10-22에 확정된 사용자 결정을 case 입력에 반영했다. 제품 인수나
전체 구현 완료를 뜻하지 않는다. actual scenarios는 실행하지 않았다.

- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-f6aefc7d`
- branch: `test/fix-case-contracts`
- baseline: `79a0cb6e9fe0e21a5296d4868b2b49d9c722a6e8`
- Java: `21.0.12.1`, `JAVA_HOME=$(/usr/libexec/java_home -v 21)`와
  `$JAVA_HOME/bin`을 PATH 첫 항목으로 사용했다.
- 사용자 최신 GPT-6 Astra 지정으로 작업했다. runtime에 모델은
  GPT-6 Astra로 표시되지만 별도 service tier 증명 필드는 제공되지 않았다.
- 위임, 외부 알림, push, PR, production backend와 harness 변경은 없다.

## 수정한 계약

1. T01 제외 업무 6개는 `HELD VERSION_UNSUPPORTED`를 기대한다.
   효과0, 요청 효과 식별, 대체 명령 미생성 assertion은 유지했다.
   `verification/mcp-tests/author_cases.py`와 파생 Gherkin도 수정했다.
2. 판매 요청 34개의 `CUSTOMER_ACCEPTED`를 `DELIVERED`로 바꾸고
   `qualityTerms=STANDARD`, provenance `USER`를 명시했다. 이는 기존
   C3 판매 대조와 같은 합성 fixture 조건이며 실제 고객 정책이 아니다.
   수량·가격·품목·목적지와 모든 업무 assertion은 유지했다.
3. V5 4개와 T26 13개 goal 형 `createWork`의 `workType`을 `kind`로
   바꾸고 provenance도 같이 옮겼다.
4. C3 12개와 V4 16개 권한 대조 요청에 명시적 누적 기간을 추가했다.
   시작은 해당 fixture의 `2026-10-07T09:00:00Z`, 끝은 기존 dueAt이며
   양 끝을 포함한다. T20 누락/오류 입력은 보충하지 않았다.
5. T21 `explicit-authorized-migration`, `pinned-meaning-current-policy`의
   FDE grant에서 정의 명령이 제시하지 않는 ITEM 차원을 제거했다.
   조직, capability 목록, 유효기간, 위임자는 유지했다. 다른 actor의
   item 범위는 유지하며 PER_DIMENSION을 사용하지 않는다.
6. `verification/cases/author_resume_contracts.py`는 위 입력 수정을
   재현하는 멱등 post-processor다. 기존 생성기 fixed point도 통과했다.

## 실행 증거

| 검사 | exit | 결과 |
|---|---:|---|
| `python3 -m unittest discover -s verification/cases -p test_resume_contracts.py -v` | 0 | 6 tests 통과 |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py' -v` | 0 | 3 tests 통과 |
| `python3 verification/cases/author_resume_contracts.py` 재실행 | 0 | `changes {}` |
| `./verify prepare` | 1 | 41 cases, 802 subcases, 24,034 assertions; 아래 실패 |
| `git diff --check` | 0 | whitespace 오류 없음 |
| `npm ci --prefix backend` | 0 | 설치 성공; npm audit 8건(4 high, 4 critical) 보고 |

prepare의 마지막 실행은 `2026-10-22T08:06:34.709505Z`이다.
`verification/harness/target/evidence/prepare.json`과
`/tmp/mulino-test-prepare-final.log`에 원본이 있다. 이들은 로컬 실행
산출물이며 commit한 runtime receipt가 아니다. Maven wrapper의
harness package는 성공했다. prepare 내부 12개 자산 검사 중 10개가
통과했다. request-contracts, binding 5종, 두 generator 재현 검사를
포함한다. capability slot 검사에는 1,864 명령 중 463 위반이 남았다.

prepare를 막는 소유 밖 파일은 다음과 같다.

- 기존 `docs/ontology-implementation-plan.md`,
  `docs/ontology-design-philosophy.md`, `docs/ontology-discussion.md`와
  normative catalog의 hash가 불일치한다. 기준 문서 변경 의도를 검토한
  뒤 catalog 소유자가 갱신해야 한다. test worker는 hash를 덮지 않았다.
- `contracts/domain-vocabulary.json`의 PENDING pair
  `CAPABILITY_UNSUPPORTED/REJECTED`가 T01 수정으로 미사용이 됐다.
  checker의 정확한 오류는 `pending entry pair
  CAPABILITY_UNSUPPORTED/REJECTED is used by no case; remove it`이다.
  coordinator가 이 낡은 선언을 제거해야 한다. checker를 완화하지 않았다.

## 남은 계약과 실행 제한

- C3/V4 정의 lifecycle의 범용 slot 묶음은 아직 남았다. 단순 삭제만
  하면 올바른 hash, lifecycle 상태, validation/review/approval 증거가
  없는 positive control이 된다. fixture 생성과 연결된 precondition을
  함께 고쳐야 하며 이 결과를 해당 문제 해결로 계산하지 않는다.
- V5/T26의 goal 내부에는 기존 `quantity`, `destinationId`,
  `deadlineInclusive` 같은 저장 goal 계약과 다른 필드가 남았다.
  `kind` 수정만으로 activateWork의 완전한 goal을 입증하지 않는다.
  C3/V4도 기간 외 eventKind·기여 범위 등의 activation 전제가 남을 수 있다.
- T21 검증 문제별 oracle, definition pin, cross-organization draft 거부
  시점, 미지원 v2 evaluator/package 계약은 그대로 남았다. 특히 실제
  새 v2 evaluator 구현과 지원 계약으로의 fixture 변경은 의미가 다르므로
  근거 없이 대체하지 않았다. regression 실행 runner와 nextAction 없는
  활성화의 필수화도 기존 결정/구현 공백으로 남는다.
- processId는 단일 application의 role이라는 10월 12일 결정을 따른다.
  process adapter는 다른 worker 소유이며 이 commit에는 변경하지 않았다.
- 모든 변경 JSON의 semantic diff를 확인했다. T01의 확정된 오류 기대값
  외 기존 assertion, subcase 수, 수량 oracle를 바꾸지 않았다.
- actual 시나리오, full backend suite, 실모델 UAT, BTP, 규제 인수는
  `NOT_RUN`이다. coordinator 통합 뒤 실제 실행이 필요하다.
