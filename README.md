# Mulino Coreano — 온톨로지 설계 기준선

기존 구현을 대체할 새 시스템을 온톨로지 문서부터 시작하는 checkout이다. 현재 작업 트리는 아래 문서만 포함하며 애플리케이션 구현은 아직 없다.

## 읽는 순서

1. [설계 철학과 워크플로우](docs/ontology-design-philosophy.md): 명사·동사 두 관리 진입점과 통사구조, 업무 사례.
2. [구현 계획](docs/ontology-implementation-plan.md): D01–D26의 데이터·명령·상태·권한·구현 Step·인수 기준.
3. [논의 기록](docs/ontology-discussion.md): 제안·사용자 코멘트·채택 범위·변경 이력.
4. [구현 계획 완결성 검토](docs/reviews/2026-10-07-ontology-implementation-completeness-sol-high.md): 반복 반례 검토와 최종 판정.

## 검토 이력

- [Astra 설계 검토](docs/reviews/2026-10-07-ontology-adversarial-debate.md)
- [Sol xhigh 설계 검토](docs/reviews/2026-10-07-ontology-sol-xhigh-debate.md)
- [Sol high 기술 스택 검토](docs/reviews/2026-10-07-ontology-stack-sol-high-debate.md)

## 기준선과 보존

2026-10-07에 fork `se0nghe0n/mulino-coreano-erp`의 `main` commit `ae02ff63751714510db4d93a39498b8d51b3697c`를 별도로 checkout했다. 기존 tracked 파일 제거와 이 문서 추가는 로컬 변경이며 기존 commit 이력은 보존한다. 아직 commit·push하지 않았다.

온톨로지 문서 7개는 기존 작업 디렉터리에서 복사했다. 계획과 보고서에 적힌 로컬 branch·commit·파일 hash는 각 검토 당시의 기록이다. 구현 계획 본문은 복사 전과 동일하다. 새 checkout에 없는 기존 프로젝트 문서의 링크를 원본 저장소의 고정 commit으로 바꾸고 설명서의 끝 공백을 정리했으며 원래 작업 디렉터리는 수정하지 않았다.

계획의 완결성 검토는 완료됐지만 코드·DB·MCP·Agent Skills·실모델·BTP 실행 인수는 미실행이다. 구현 착수는 계획의 추적 이슈·기술 기준선·환경 gate를 따른다.
