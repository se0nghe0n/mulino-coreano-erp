# 실제 시나리오 원행 분류와 후속 실행

이 기록은 전체 인수 완료가 아니다. 기준 실행의 원인 분류와 새 source의
선택 실행을 구별한다. 분류 label은 수정 순서를 위한 진단이며 제품 판정이
아니다.

## 680개 기준 실행 분류

기준 source는 `1e6a6a521142aacb74778972005e80f209d5f658`이다.
원본802개 중 PASS122개는 partial fixture이고, FAIL592개와 NOT_RUN88개를
분류했다. inventory의 NOT_IMPLEMENTED88개는 원본 NOT_RUN의 표시다.

| 분류 | 행 수 |
|---|---:|
| TEST_INPUT | 434 |
| ADAPTER | 220 |
| PRODUCT | 22 |
| EXTERNAL_GATE | 4 |
| 합계 | 680 |

native Workflow `wf_55351ea3-0fd`의 agent metadata24개가 모두
`claude-haiku-5-5`다. 최초 fan-in이 정당한 null action ID를 거부해
재작업했으며, 최종 checker는 null과 pointer 부재를 구별한다.
coordinator가 최종680개 고유 행, 원행 상태, 419개 source file SHA256과
JSON pointer1,976개를 독립 재검사했고 오류0이다. 실패 action이 기록되지
않은36개는 null을 유지한다. V7의 없는 dispatch-ready action 대신 실제
harnessError를 가리키도록 두 pointer를 수정했다.

fingerprint label은273개이고 신뢰도는 HIGH327·MEDIUM292·LOW61이다.
label이 달라도 같은 원인일 수 있다. LOW를 면제하거나 분류 결과를
현재 source의 실패 수로 전환하지 않는다. 기존 종합 markdown의 산술과
V7 문구는 최신 JSON과 다를 수 있어 인수 근거로 사용하지 않았다.

최종 원본은 `t3-c57d50ca/.local/triage-20261023/aggregate-triage.json`이며
SHA256은
`a103711e02137747f10e0dbf62e0382b3368c02ad857530d966ac4b1a52ad50d`다.
root의 독립 검사 기록은
`.local/resume-20261023/triage-coordinator-validation-final.json`이다.
이 artifact는 진단이며 coverage receipt가 아니다.

## 누적 목표 입력 통합 후 실행

`d3d4c2df7230cd3bbd096fb83732271a4c802858`에서 native Workflow
`wf_88329bcd-6b2`가 T09·T11·T12·T13을 병렬 실행했다. agent metadata6개는
모두 Haiku5.5다. T09·T12·T13의 source는 clean이고 원본 report hash를
coordinator가 대조했다.

| case | PASS | FAIL | NOT_RUN |
|---|---:|---:|---:|
| T09 | 3 | 6 | 0 |
| T11 | 0 | 0 | 12 |
| T12 | 0 | 5 | 0 |
| T13 | 0 | 17 | 0 |
| 합계 | 3 | 28 | 12 |

고유31개에서 actual action104개를 실행했다. 예상392개 assertion 중
41개가 기록됐고, 나머지는 앞선 action 실패 또는 T11 미실행 때문에
관찰되지 않았다. PASS3개의 assertion18개는 모두 원본 기대와 일치하고
각 runtimeComplete=true다. 미관찰351개를 성공이나 효과0으로 해석하지
않는다. 전체43개 실행 gate는 미완료다.

T11 agent는 backend package command 뒤에 완료 결과를 남기지 않았다.
마지막 exit0은 harness package의 값이며 backend 완료 값이 아니다.
동일 source의 T11만 새 native Workflow `wf_e7355455-033`으로 재실행한다.
기존 부분 실행 결과를 덮어쓰지 않는다.

T09의 activate2개와 T12·T13의 createWork21개는 공통 정의 검증에서
`HELD VERSION_UNSUPPORTED: Pinned predicate unsupported`를 반환했다.
이는 이번에 보완한 기간 입력 다음에 드러난 실패다. 실제 등록 handler와
기본 fixture 정의의 capability manifest 일치를 별도 수정한다.
기타 scope·conditions·destination 입력 실패도 남았다. oracle나 미지원
evaluator 반례를 바꾸어 PASS로 만들지 않는다.

원본은 `t3-0754e9b5/.local/cumulative-rerun-20261023/manifest.json`이
가리키는 세 실행 report다. root 대조 기록은
`.local/resume-20261023/cumulative-rerun-coordinator-proof.json`이다.
모든 실행은 partialFixtures=true·relaxWire=false다.

이전 중단 recovery 실행의 잔류 PID57150은 cwd·로그·run ID로 소유를
확인한 뒤 종료했고 해당 container를 정지했다. DB 자료·jar·worktree와
실패 증거는 보존했다. 정리 기록은
`.local/resume-20261023/owned-orphan-cleanup.json`이다.

전체 Task와 현재 통합 Step는 열려 있다. 실모델 인수·실제 규제·BTP
배포 인수를 주장하지 않는다.

## T11 재실행과 같은 source의 43개 결과

T11 재실행은 같은 clean source d3d4c2df에서 exit1로 완료했다.
12개 고유 subcase는 3PASS·9FAIL이며 actual action74개를 실행했다.
원본 report SHA256은
`d12ddf2e6a39d0f26ea91edb8053ed06f1b0ce34a30e6e8d8b04dbd07d881c0f`다.
root의 `t11-retry-coordinator-proof.json`에 경로와 독립 집계를 보존했다.

앞선 세 case 실행과 합하면 같은 source의 43개는 6PASS·37FAIL이다.
actual action178개이며 assertion392개 중73개가 기록됐다. 나머지319개는
앞선 action 실패로 미관찰이며 성공으로 집계하지 않는다. 앞선 부분 실행의
12NOT_RUN 기록은 보존한다. T11에서도 공통 정의 검증 거부와 별도 slot·
조건 오류가 드러났다. 전체 gate는 여전히 미완료다.
