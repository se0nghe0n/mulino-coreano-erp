# 재개 prepare 계약 정합성

2026-10-22 현재 Step의 준비 계약만 정합화했다. `./verify prepare`는
PREPARED이며 제품 실행 인수는 NOT_RUN이다.

## 범위와 대조 근거

- 작업 baseline은 `6f88fd0616f62dec07b9c268cc1ec33d7d9421a2`다.
- 작업 경로는
  `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-4c20bf67`다.
- 기존 source hash 세 개는 모두 문서 baseline
  `847758afa6cb58e2a4c661e578773f1531098ee8`의 실제 bytes와 일치했다.
- 해당 baseline부터 현재까지의 세 문서 diff를 대조했다.
  `06833c23`·`a6ef9c3e`는 설계자 인터뷰, 통사구조 중심 판단,
  SAP 사례 대조 설명을 추가했다. 기존 D/T/C/V/E 인수, S0–S6 순서,
  효과·권한·책임 계약은 삭제하거나 완화하지 않았다. 새 KPI 산식과
  capability를 확정하지 않으며 실제 실행과 문서 대조를 구별한다.
- 계획·철학·논의 문서 자체와 제품/adapter는 수정하지 않았다.

## 변경

`normative-contract-lock.json`과 `mandatory-oracles.json`의 source
hash를 현재 세 문서에 맞췄다. sourceRefs를 포함해 hash하는 122개
oracle의 contractSha256도 갱신했다. hash field를 제외한 catalog
전체가 baseline과 같은지 구조 비교했다. 499개 기대 관찰, 요구 ID,
고정 fixture hash와 oracle 이름은 유지했다.

C3·T08·V4·V6·V7의 기존 생성기를 실행해 observation binding을
갱신했다. 다섯 파일 모두 catalogSha256만 바뀌었고 caseHash와
관찰 연결은 동일하다. prepare의 생성기 재현 검사도 통과했다.

미사용 `CAPABILITY_UNSUPPORTED/REJECTED` PENDING의 실제 위치는
`verification/cases/check_vocabulary.py`였다. 그 예외 한 줄을
삭제했다. `contracts/domain-vocabulary.json`의
`CAPABILITY_UNSUPPORTED/HELD`는 다른 의미의 유효한 계약이므로
유지했다. 검사 로직은 바꾸지 않았고, 폐기 pair는 실패하며 현재
`VERSION_UNSUPPORTED/HELD`는 통과하는 regression 두 개를 추가했다.

T01의 제외 업무 효과0과 HELD/VERSION_UNSUPPORTED, 판매의
DELIVERED·qualityTerms 계약은 baseline의 명시 결정을 유지했다.
T01 case hash는
`a755277e3a1fff89697f6c4f14921f89229e97fb61a3b5db5beb2516f300d7b7`다.
case·fixture·assertion 자체는 변경하지 않았다.

## 실행 검증

Java 21.0.12.1, Maven 3.9.16, Python 3.14.7로 실행했다.

| 검사 | 결과 |
|---|---|
| 수정 전 `./verify prepare` | exit1, FAIL, preparationProblems 5개 |
| 수정 후 `./verify prepare` | exit0, PREPARED, preparationProblems 0개 |
| `python3 -m unittest discover -s verification/requirements -p test_catalog.py` | exit0, 25개 통과 |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | exit0, 8개 통과 |
| `python3 -m unittest discover -s verification/cases -p test_resume_contracts.py` | exit0, 6개 통과 |
| `git diff --check` | exit0 |

수정 전 실패는 문서 source drift 3개, 같은 원인의 catalog check
1개, 미사용 PENDING vocabulary check 1개다. 수정 후 41 case,
802 subcase, 24,034 assertion과 caseAssetChecks 12개를 확인했다.
모든 변경 파일을 검토했으며 catalog/binding의 의미 보존을 별도로
구조 비교했다. checker 약화나 인수 oracle 삭제는 없다.

로컬 실행 artifact는 다음과 같다. 준비 시점은 baseline HEAD에
위 변경이 있는 dirty tree이며 제품 receipt로 사용하지 않는다.

- `verification/harness/target/evidence/prepare.json`
- `verification/harness/target/wrapper-commands.json`
- `/tmp/mulino-prepare-before.json`
- `/tmp/mulino-prepare-before.log`
- `/tmp/mulino-prepare-after.log`
- `/tmp/mulino-catalog-tests.log`

## 남는 범위

prepare failure는 없다. 보고서의 KNOWN_OPEN 206개와 regulatory,
SCHEDULER_CYCLE_RECORD의 NOT_RUN_GATED 두 항목은 유지된다.
artifact attribution gap은 0개지만 runtimeStatus는 NOT_RUN이다.
실제 시나리오·실모델·배포는 이번 Subtask에서 실행하지 않았다.

실행 모델은 T3가 보고한 `codex/gpt-6-astra`다. live catalog에서
Ultrafast 지원은 확인했다. 다만 조회 결과가 실행 중 run의 tier를
노출하지 않아 이 Subtask에서 활성 tier를 독립 확인하거나 바꾸지는
못했다. 상위 dispatch의 명시 옵션 확인은 coordinator에게 남긴다.
추가 worker·push·PR·외부 메시지는 실행하지 않았다.
