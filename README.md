# Mulino Coreano — 새 온톨로지 시스템

이탈리아 완제품의 구매·운송·수입·수령·QC·국내 B2B 인도부터
반품·회수·정산 참조와 후속 책임까지를 지원하는 시스템을 새로
구현한다. 명사와 동사 두 진입점이 같은 대상·업무·물량·증거·책임을
읽는 것이 설계의 기준이다.

현재는 사용자 Step 1의 skills와 실행 지침을 구성하는 개발 branch다.
시작 baseline에는 문서만 있고 애플리케이션·DB·실행 검증은 없다.
현재 진행과 남은 gate는 [작업 기록](docs/execution/task-status.md)을
따른다. 계획 완결성 검토는 코드나 실환경 인수의 증거가 아니다.

## 읽는 순서

1. [설계 철학](docs/ontology-design-philosophy.md): 두 진입점과 업무 사례.
2. [구현 계획](docs/ontology-implementation-plan.md): D01–D26의 계약,
   S0–S6 통합 gate, T/C/V/E 인수와 운영 결정 register.
3. [논의 기록](docs/ontology-discussion.md): 제안·채택·변경의 경계.
4. [작업 지침](AGENTS.md): 사용자 작업 순서·모델·review·통합 규칙.
5. [작업 기록](docs/execution/task-status.md): 현재 Step와 실행 증거.

## 구현과 검증

Java21·CAP·Maven·CDS/CQN·PostgreSQL은 S0에서 검증할 기술 후보다.
exact 버전과 지원 조합은 아직 고정하지 않았다. 구현 계획의
`./verify` 명령은 향후 제공할 계약이며 현재 실행 가능한 startup
명령이 아니다. 새 구현의 실제 실행 명령은 검증 후 기록한다.

사용자 작업은 skills → tests → 구현 → 실제 E2E → 패턴/refactor →
운영 매뉴얼 순서다. 매 단계 Sol xhigh와 Astra low의 adversarial
review, 지적 수정, 통합 checks를 통과하고 미완료 작업을 반복한다.
이 순서는 시스템 S0–S6의 필수 인수를 줄이거나 대체하지 않는다.

## 기준선과 보존

fork는 `se0nghe0n/mulino-coreano-erp`다. 새 문서 baseline commit은
`847758afa6cb58e2a4c661e578773f1531098ee8`이며 옛 구현의 commit 이력은
Git에 보존한다. 사용자 승인으로 로컬 main에 이 baseline을
fast-forward했다. 원격 main 반영은 이 기록 시점에 미실행이다.
이후 개발은 별도 Task branch와 PR을 따른다.

옛 구현은 새 작업 트리에서 제거했다. 옛 코드·schema·tests·skills·
운영 문서를 읽거나 재사용하지 않는다. 원본
[#57](https://github.com/mulino-coreano/mulino-coreano-erp/issues/57)의
테스트 방법론만 새 업무에 맞게 참고한다. 온톨로지 문서의 과거
branch·hash·기술 조사는 각 검토 당시 기록이다.

새 구현 Task의 추적 위치는 아직 확정하지 않았다.
[구현 이슈 초안](docs/execution/implementation-issue.md)을 준비했으며
원본 #57을 새 fork 전체 구현의 추적 이슈로 표시하지 않는다.

## 설계 검토 기록

- [Astra 설계 검토](docs/reviews/2026-10-07-ontology-adversarial-debate.md)
- [Sol xhigh 설계 검토](docs/reviews/2026-10-07-ontology-sol-xhigh-debate.md)
- [Sol high 기술 스택 검토](docs/reviews/2026-10-07-ontology-stack-sol-high-debate.md)
- [계획 완결성 검토](docs/reviews/2026-10-07-ontology-implementation-completeness-sol-high.md)
