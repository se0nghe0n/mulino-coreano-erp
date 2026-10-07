# S0 보존과 자료 분기

빈 fork는 운영 자료 부재의 증거가 아니다. Git 파일 보존은 tree와
object hash로 확인하고 DB·원문·외부 효과·의무 존재는 별도로 판정한다.
사용자가 2026-10-08 Asia/Seoul에 다음 범위를 확정했다.

> 실제 운영 자료 없음 — 새 DB와 개발 fixture로 진행

R3의 실제 운영 DB·원문 blob·pending external effects·open work는
`NONE / USER_DECLARATION`이다. 자원 내용 조회로 검증한 부재가 아니다.
fresh isolated DB와 가상 ontology fixture 개발을 진행한다. 과거 운영
자료 migration/cutover는 이 선언 범위에서 비대상이다. 새 ontology
schema v1→v2, DB+blob+definition backup/restore 인수는 여전히 필수다.

## 기록 범위

Task는 새 온톨로지 전체 구현이며 사용자 Step 3의 S0 Subtask다.
입력은 `step2-complete`=`4280a750dca0aef35abaf10b6797881181dd88d7`다.
소유 경로는 `docs/execution/s0-inventory/**`와
`verification/platform/inventory/**`다. 다른 worker 변경과 원본 ERP를
수정하지 않는다. coordinator가 Task branch에 통합한다.

`files.json`은 fork default/main/integration과 원본 checkout HEAD의
각 파일·link를 `currentHash, classification, replacementPath,
archiveRef, owner, validation`으로 기록한다. 옛 파일의 `currentHash`는
archive tree의 Git object hash이고 현재 새 구현의 hash가 아니다.
`newBaselineHashAtSamePath`가 새 기준선의 같은 경로를 구별한다.
분류 `REPLACE`는 새 구현 대상이며 옛 내용 재사용을 뜻하지 않는다.
`HISTORY_ARCHIVE`는 역사 보존이다. replacement 경로는 계획상 모듈
경계로 보낸다. 경로 존재·기능 완성·runtime PASS를 주장하지 않는다.

새 baseline의 모든 tracked 파일은 `newBaseline` 배열에 기록한다.
이 worker가 작성한 source와 문서의 SHA-256은 `owned-files.json`에
기록한다. generated JSON의 자기 hash는 순환하므로 포함하지 않는다.
JSON의 필드·관계·자료 분기 근거는 `check.py`가 검사한다.

## Git 보존 증거

`archive-recovery.json`은 archive ref의 현재 commit/tree hash와 모든
tree entry의 object 존재를 `git ls-tree`와 `git cat-file --batch-check`로
확인한다. legacy blob 내용은 읽거나 checkout하지 않았다. 원본 ERP의
ref/tree/object metadata만 읽었다. Git bundle 생성과 DB/blob 복원은
수행하지 않았다. `PASS`는 현재 reachable archive object의 회수 가능성
확인이며 전체 V8 restore rehearsal의 PASS가 아니다.

remote tracking ref는 움직일 수 있으므로 각 행의 `archiveCommit`을
복구 식별자로 쓴다. 현재 object는 기록한 ref에 reachable하지만 ref 삭제나
강제 변경 이후 보존까지 보장하지 않는다. S6 정리 전에 coordinator가
보존 ref 또는 검증한 bundle을 고정하고 필요시 재검증한다.

온톨로지 문서 7개는 초기 baseline
`847758afa6cb58e2a4c661e578773f1531098ee8`과 Step 2 및 현재 파일의
Git blob/SHA-256을 대조했다. 구현 계획 SHA-256은
`7f010093b1fba3673f674c78dddc8c407512e037831f093babc87d7421a22a00`다.
문서 내용을 변경하지 않았다.

## 자원 관찰과 권한 경계

Docker에서 이미 알려진 `mulino-scenario-pg`의 이름·image·state·mount만
확인했다. 정지된 `postgres:18` container와 persistent volume이 존재한다.
컨테이너 내용의 authoritative 여부는 `UNKNOWN`이다. 이것은 사용자
선언의 운영 자료 부재와 모순되지 않으며 DB 부재의 증거도 아니다.
volume을 보존한다. 삭제·컨테이너 시작/중지·DB 쓰기·외부 전송 권한은
이 작업에 없다. private file·env·credential·legacy source·schema·test·
skill·운영 문서를 읽지 않았다. unrelated container metadata는 저장하지
않았다.

자료 분기는 `resources.json`, 현재 fork/original/default/local main은
`git-metadata.json`, 정확한 명령·exit code는 `commands.json`에 남긴다.
기록된 timestamp는 수집 시점이다. Git 원격 조회는 read-only
`ls-remote`이며 fetch/push나 원격 설정 변경은 없다.

## 재검증

이 worker worktree 또는 통합된 checkout에서 다음을 실행한다.
원본 metadata 경로는 `collect.py` 상수에 명시했다. 경로를 임의 탐색하지
않는다. R3 사용자 선언을 보존하며 새로운 운영 자료 발견 시 coordinator가
결정 register와 인수 범위를 갱신한다.

```sh
python3 verification/platform/inventory/collect.py
python3 verification/platform/inventory/check.py
git diff --check
```

수집 명령의 실패를 PASS로 축소하지 않는다. `check.py`는 metadata
증거와 hash의 일치를 검증한다. 앱·거래·MCP·실모델·법규·BTP·DB/blob
restore와 새 ontology v1→v2 실행은 이 Subtask에서 `NOT_RUN`이다.
S0 전체 종료는 stack/security/protocol 산출물 통합과 결합 checks 및
지정 review 후 coordinator가 판정한다.
