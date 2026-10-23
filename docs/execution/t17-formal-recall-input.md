# T17의 명시적 formal recall 입력을 연결한다

2026-10-24, baseline
`d0885a8031eb754e42f35f998a6d7f970b059cee`에서 수행했다.
`fix/t17-formal-recall` branch와 T3 전용 worktree
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-db3fa28c`를
사용했다. T3 child의 provider/model은 `codex_2/gpt-6.1-sol`이며,
지정된 `xhigh/priority`를 변경하지 않았다.

기존 `placeHold`는 실제 SELL 제한을 기록하지만 formal Investigation을
열지 않는다. fixture의 ITEM P/RECALL 책임 선언에도 대응하는 ACTIVE
Work가 없었다. 명시적 Work 준비와 실제 `openInvestigation`으로 이 입력
공백을 닫았다. 조사와 ADMIN 책임의 실재는 확인했으나, 원본 late oracle
전체는 아래 다섯 실패 때문에 FAIL로 남는다. Task/S5/S6 완료가 아니다.

## 승인된 입력 delta와 보존을 구별한다

late fixture의 정확한 추가는 다음 두 개뿐이다.

- `/aliases/t17FormalRecallWork`에 `{"type":"Work"}`를 추가한다.
- `/baseline/works/1`에 ITEM P, LOT L, kind RECALL, status ACTIVE,
  owner admin, supervisor supervisor, revision1의 Work를 추가한다.

기존 ITEM P/RECALL responsibility의 owner·supervisor·nextAction·
nextCheckAt을 그대로 사용한다. 기존 responsibility 자체는 수정하지
않는다. actors, roleCapabilities, all-of grants, clock, evidence,
source profiles와 그 밖의 fixture 내용도 보존한다. admin의 기존
`openInvestigation/getEvidence` 권한으로 실행하며 broad grant나
`assessGoal` 권한을 추가하지 않는다.

late fixture의 SHA-256은 다음과 같다.

```text
before 20513a43eabf5c1ec8cb95f9a3ae2b33b98f3977140b7f13f8cfe856abb1675c
after  81d0ca37af98d56bb6f7df4d067b8121eb5dad621398e726f63cdbf9897d693e
```

나머지 14개 fixture는 baseline bytes와 현재 bytes가 일치한다.
역변환한 late fixture도 baseline bytes와 일치한다. 각 파일의 전후
hash와 두 pointer의 before/after는
[recall-input-delta.json](../../verification/cases/T17/recall-input-delta.json)에
고정했다. condition 통합 시점까지 15개 fixture bytes를 보존했다는
과거 기록은 그대로 둔다. 이번 변경 뒤의 15개 전체 byte 불변은
주장하지 않는다.

late subcase에 `late-hold` 직후 다음 네 action만 추가했다.

1. admin의 `open-recall-investigation`이 `openInvestigation`을 실행한다.
2. `formal-recall-work`가 실제 RECALL Work를 공개 조회한다.
3. `formal-recall-basis`가 실제 Events를 공개 조회한다.
4. `formal-recall-investigation`이 실제 Investigation을 공개 조회한다.

basisEvidenceId는 `late-hold-source-event`의 `/response/id`다.
Document ID를 넣지 않는다. 실제 transit segment를
`late-hold-source-scope`에서 받아 `[start=0, quantity=30] BOX`로 묶는다.
occurredAt은 `2026-10-07T09:00:00Z`이며 knownAt보다 늦지 않다.
nextCheckAt은 원래 책임의 미래 시각 `2026-10-07T10:00:00Z`다.
승인·통지·회수·오염 확정은 생성하지 않는다.

기존 257 assertion의 ID·순서·op·expected·oracleRef뿐 아니라 전체
assertion objects와 15개 assertion array의 bytes를 보존했다. case의
추가 assertion은0이다. 필요한 조사·수량·출처·owner·public/DB 검사는
별도 focused Java 검사에 추가했다. 나머지 14 subcase는 전체 object가
baseline과 일치한다. 원래 feature lines도 역변환 뒤 bytes가 일치한다.

## generator와 focused 실행을 기록한다

[author_recall_inputs.py](../../verification/cases/T17/author_recall_inputs.py)는
현재 case/fixtures를 받아 additive delta를 적용하고 정확히 역변환한다.
독립 실행도 현재 입력을 사용하므로 root가 먼저 합성한 다른 layer를
baseline으로 덮어쓰지 않는다. ledger의 기준은 d088로 고정한다.
공개 generator의 최종 순서는 condition layer 뒤
`recall_inputs.apply(case, fixtures)`이며 역변환은 recall 다음 condition이다.
이 tree는 condition worker commit을 가져오지 않았다.

기존 author inverse와 historical test 세 개에는 recall inverse만
합성했다. 원래 layer의 독립 기대와 fixture 보존 검사를 유지하면서
새 layer를 먼저 되돌려 비교한다. 새 검사도 실제 최신 bytes의 정확한
재현과 원본 bytes의 보존을 독립적으로 확인한다.

실행 command와 결과는 다음과 같다.

```text
python3 -m unittest discover -s verification/cases/T17 -p 'test_*.py' -v
exit0, 38 tests

./verify prepare
exit0, PREPARED; runtime acceptance가 아니다.

DOCKER_HOST=unix:///Users/se0nghe0n/.orbstack/run/docker.sock \
  python3 verification/cases/T17/run_formal_recall_pg_test.py
exit1, focused checks146 통과, 원본 late assertions21/26 통과
```

Python 최종 log는 `.local/t17-recall-python-final.log`다.
정적 byte 증거는 `.local/t17-recall-preservation.json`이며 runtime
receipt가 아니다. prepare 최종 log는
`.local/t17-recall-prepare-final.log`다.

최종 HTTP/PostgreSQL 산출물 디렉터리는 다음과 같다.

```text
.local/t17-formal-recall-f747b281-b4be-464c-bfd2-6c24af9a7682/
```

JDK는 Homebrew OpenJDK21.0.12.1, Node24.16.0, npm11.13.0이다.
PostgreSQL은 `verification/platform/versions.json`의 pinned image digest로
별도 container를 만들었다. 이 checkout의 backend와 harness를 직접
compile했다. 다른 tree의 JAR/schema를 복사하지 않았다.

```text
product source commit
d0885a8031eb754e42f35f998a6d7f970b059cee

product JAR
backend/target/ontology-0.1.0-SNAPSHOT.jar

JAR SHA-256
2679c9103b361adb4bf1647b9d7595f222d4611b33c7c45b2d173d048aa50d3b

focused Java source SHA-256
3fbde25cc1beac800109482b07df803f0e87ac0e3154761e3d8c10d52ed0cbe6
```

`build-identity.json`에 source/worktree/branch/JAR/fixture hash를 남겼다.
`commands.json`에 command·exit·log를 남겼다. 최종 Java source는
[T17FormalRecallInputCheck.java](../../verification/cases/T17/T17FormalRecallInputCheck.java)다.

실제 Investigation은 `dace4ce6-363e-48f9-840e-3f9930d5a619`이며,
basis Event는 `fdc42b14-cd1c-4607-92a3-90ddc7245104`다.
Event/Claim/verification/Document/CanonicalOccurrence의 실제 연결과
수량30 BOX를 DB에서 확인했다. 공개 Investigation의 identity·scope·
quantity·owner·supervisor·basis·state를 DB와 대조했다. OPEN recall root는
실제 `openInvestigation` CommandRecords를 출처로 하며 scopeJson의
domainSourceId가 이 Investigation이다. ADMIN owner와 지정된 Work,
다음 행동, 미래 nextCheck를 확인했다.

공개 조회와 독립 REPEATABLE READ observation은 query 전후 business row
digest로 같은 revision에 결합했다. 실제 confirmed delivery20,
contributed0, transit10과 제한 위반 책임을 확인했다. Work/PLACE의
추가 all-of 차원 불일치와 현재 getEvidence 권한 누락을 거부했고,
Document를 Event로 제출하는 경우, 범위 초과, 단위 오류, 미래 발생,
만료 nextCheck도 거부했다. 각 거부 전후 domain·재고·책임·outbox 원행이
일치했다. 거부 감사/CommandRecords는 이 불변 집합에 포함하지 않는다.
`formal-recall-domain-proof.json`과 각 `*-rollback.json`에 실제 원행을 남겼다.

## 실패 원문과 잔여 gate를 보존한다

최종 `focused-result.json`은 원본26개의 실제 관찰을 모두 기록한다.
다음 원문은 d088의 제품/observer 조합에서 나온5개 실패다.

```text
fact-not-permission-10
set or relation mismatch: ["UNVERIFIED"] != ["UNSATISFIED"]

fact-not-permission-11
duplicate set member/physical identity: "REGULATORY_REVIEW"

fact-not-permission-12
expected "UNSATISFIED", observed "UNVERIFIED"

late-restriction-duty-18
missing field "sourceOccurrenceId"

late-restriction-duty-25
missing field "sourceOccurrenceId"
```

`current=true`로 읽힌 kind에는 원래 두 책임 외에 RESOLVED
REGULATORY_REVIEW2·DELIVERY_RECONCILIATION1과 OPEN QUALITY_REVIEW1이
존재했다. 이 원행을 삭제하거나 source filter/expected를 바꾸지 않았다.
관찰 원행의 `sourceKind=DECISION/sourceId=command`를
sourceOccurrenceId로 alias하지 않았다. root는 실제 Event/canonical chain,
assessment criteria와 현재 의무 계약을 별도 공통 수정으로 조사한다.

원본 recall duty20..24 및26은 통과했다. duty25의 필드 누락과 전체
exact current kinds/목표 판정은 아직 해결된 것으로 표시하지 않는다.
root의 fa316 이후 공통 수정은 이 tree에서 읽거나 가져오지 않았다.
통합 후 같은 원본26개와 root의301/505 assertion 집합을 실제 실행하는
gate가 남는다. `./verify scenarios --actual`은 실행하지 않았다.
Haiku native Workflow/실모델/규제/BTP 인수와 전체 Task gate도 닫지 않는다.

앞선 준비/검사 실패도 삭제하지 않는다.

- `.local/t17-formal-recall-632fc052-83fc-4826-97b5-842fe609a08d/`:
  `sh: cds: command not found`다. 이 tree의 npm 의존성을 설치했다.
- `.local/t17-formal-recall-d2695238-b340-410d-b59a-048472390b38/`:
  `observer readMode <query revision> not implemented`다. focused 검사에서
  기존 RESULT_REVISION binding을 사용하도록 교정했다.
- `.local/t17-formal-recall-5b4775f8-e830-427e-bab3-64b68fedb537/`:
  provisional delivery의 실제 `ACCEPTED_PENDING_EXTERNAL`을 검사의
  `APPLIED`와 비교한 오류다. provisional과 최종 확정을 구별했다.
- `.local/t17-recall-python-full.log`의 초기6FAIL/1ERROR는 새 layer의
  historical inverse 합성 누락이다. 최종38개 검사에서 해소했다.

이 수정은 제품/FixtureInstaller/공통 observer를 변경하지 않는다.
main·root Task checkout·remote·PR·Grok도 변경하지 않는다.

## 실행 자원을 정리한다

각 실행은 자신이 만든 Spring process와 PostgreSQL container/volume만
정리했다. 최종 container
`t17-formal-recall-3538c88d-9f00-4930-98b8-d5bd5030b3d2`는 제거했다.
Spring process는 종료했고 private/public PEM을 삭제했다.
`cleanup.json`과 `cleanup-container.log`에 결과를 남겼다.
`.local/t17-recall-cleanup-audit.json`은 이 worker가 선언한 네 실행의
container 부재, process 종료, 인증 파일 제거를 다시 대조한 결과다.
다른 worker/container에는 접근하지 않았다. 로그·원행·blob·빌드 산출물은
검토를 위해 `.local`과 이 worktree의 ignored target에 보존한다.
