# Haiku 두 번째 결합 scenario 실행의 실제 결과

`fa316f01b2972720627bd337db147d761fa7bb80`의 실제 실행은
505개 기대 assertion 중456 PASS·35 FAIL·14 미관찰이다.
36개 subcase는20 PASS·16 FAIL이다. 전체 Task/S5/S6는 OPEN이다.
이 기록은 scripted SIT이며 실모델 업무 client 인수가 아니다.

## 실행과 원문 대조

Native dynamic Workflow는 `wf_17cd7443-f63`, native Task는
`wpcf2vmkb`다. Setup·runner12개·fanin의 실제 agent14개가
`claude-haiku-5-5`/medium으로 실행됐다. 동시에 실행한 runner는 최대3개다.
Primary는 assistant transcript120개와 journal의 started/result14개를
대조했다. Monitor·advisor·외부 shell fanout·추가 retry는 없다.

원본461개 ID와 순서를 보존하고 독립 조건 근거 검사44개를 추가했다.
12개 격리 tree의 실행 source·JAR·동일 product class335개,
입력 hash·전후 clean 및 소유 DB12개 부재도 primary가 확인했다.
Controller의 missing14개는 실제 assertion 결과가 없는 미관찰14개다.
이를 FAIL 실행이나 PASS로 바꾸지 않는다.

| Case | 기대 | PASS | FAIL | 미관찰 |
|---|---:|---:|---:|---:|
| T09 | 94 | 94 | 0 | 0 |
| T11 | 110 | 94 | 2 | 14 |
| T17 | 301 | 268 | 33 | 0 |
| 합계 | 505 | 456 | 35 | 14 |

Raw gateComplete와 prerequisiteRuntimeComplete는 false다.
Complete/runtimeComplete 필드는 없다. Wrapper·fanin의 성공을
전체 profile receipt나 운영 gate의 PASS로 해석하지 않는다.
41 case·802 subcase의 전체 실행과 모델 corpus/client·배포 인수는 별도다.

## 실패 분류와 후속 수정

T11 cancel의 reserve는 실제 INSUFFICIENT_ELIGIBLE_QUANTITY로 거부됐다.
Allocation 응답 pointer만 바꾸면 해결되지 않는다. 원 fixture의 과거
Assessment, 의무·assignment와 실제 현재 적격성 Given도 설치되지 않았다.
Closed Work의 history count0과 후속 parentWorkId 관찰 공백도 확인했다.

T17은 raw segment의 파생 eligibilityStatus 누락12개, evidence policy
불일치로 인한 UNVERIFIED6개, typed causal sourceOccurrenceId 누락7개,
명시 formal recall 입력 부재6개, ITEM 전체 책임 selector 범위1개와
prior dispatch prerequisite 조회 부재1개다. 원래 기대값과 미관찰을 보존한다.

실행 후 다음 수정은 별도 focused 증거와 함께 통합했다. 이 표의 통합은
새 native scenario PASS를 주장하지 않는다.

| 수정 | 통합 commit | 한정 근거 |
|---|---|---|
| 명시 formal recall Work·실제 조사 명령 | fb58d866 | HTTP/PG146 checks PASS, 기존26 oracle21 PASS·5 FAIL 보존 |
| 실제 typed source·FOLLOWUP 관계 projection | 140c9b5a | PG 회귀165 PASS·기존 replay1 NOT_RUN, 원145개 case 파일 byte 보존 |
| 독립 EVIDENCE policy 입력7개 | 03aa82e5 | HTTP/PG9 PASS, 원301 assertion 전체 및 fixture inverse15개 보존 |
| Work 후속 책임과 ITEM 전체 assignment 범위 | de794494 | Python 결합47 PASS, 원301 inverse와 추가8개 검사 |

T11 명시 Given·현재 revision 입력, T17 공개 적격량/priorDispatchCheck,
승계 assignment 생성 transaction의 독립 관찰은 별도 writer가 수정한다.
기존 actor/grant/clock과 원 fixed oracle을 줄여 통과시키지 않는다.
현재 권한 all-of와 current=valid/status 독립도 유지한다.

## 증거 위치

Primary 원문 대조는 다음 root 상대 경로에 보존한다.

- `.local/resume-20261024/fix-until-done/native-audit-r2/proof.json`
- 같은 디렉터리의 `failures.json`, `preliminary-scenarios.json`
- `.local/resume-20261024/fix-until-done/native-preparation-r2/manifest.json`

Controller 원문은
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-fdfcbe77/.local/native-fix-r2-fa316f01/`에 있다.
Workflow script SHA-256은
`42757f5e60cb4a1b57faee9c4948b60badc7914b69f8ddcf29067ac979381698`다.
Primary proof가 script·journal·transcript·source의 실제 경로와 hash를
연결한다. 이전 실행과 실패 artifact는 덮어쓰지 않았다.
