# S2 목표·책임·거래·복구 통합 기록

사용자 Step3와 시스템 S2는 ACTIVE다. S1은 실제 backend64·harness417
검사 및 새 DB의 HTTP/JDBC21 assertions로 완료했다. 첫 native fixture
실패도 보존했으며 최종 수정은 backend 입력을 변경하지 않았다.

## 기준선과 소유권

S2의 공통 baseline은 `40a240c3886d3316ba1fd686e9b44bfa1ba6ec2d`이고
로컬 tag는 `step3-s2-baseline`이다. S1 완료 tag 뒤의 원본 관찰 artifact
추적 보완까지 포함한다. 모든 writer는 실제 GPT-6.1 Sol medium으로
독립 branch/worktree에서 작업한다. coordinator만 Task branch에 통합한다.

아래 절대 경로의 공통 prefix는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s2-`다.

| Subtask | branch / worktree suffix | 소유 범위 |
|---|---|---|
| 공통 거래·승인·감사 | `step3/s2-commands` / `commands` | 공통 command ports·gateway·V8·일반 adapter·공유 build/import |
| 업무·목표 lifecycle | `step3/s2-work` / `work` | Work·Goal canonical 상태·V9·Work read 모델·평가/책임 ports |
| 의무·인계 | `step3/s2-responsibility` / `responsibility` | stable duty root/scope·현재 assignment·handover·V10 |
| 목표 판정 | `step3/s2-evaluation` / `evaluation` | 실제 입력 snapshot·typed predicate·불변 Assessment·V11·pure validator 보강 |
| 현재 신원·정책 | `step3/s2-control` / `control` | identity/capability/grant 관리·policy lifecycle·현재 command guard·V12 |
| outbox·복구 | `step3/s2-runtime` / `runtime` | outbox·execution lease·claim fencing·reconciler·V13 |
| 물량 primitive | `step3/s2-inventory` / `inventory` | split/merge/move/adjust/dispose·원장·stock fence·V14 |
| 사건·증거 대조 | `step3/s2-evidence` / `evidence` | typed RECORD·canonical 대조·정정 영향·blob adapter·V15 |
| 실제 인수 adapter | `step3/s2-adapter` / `adapter` | 실제 HTTP/비동기/독립 SQL·fixture·host 관찰·격리 runner |

`work-read.cds`는 work 단일 writer, `ontology.cds`와 backend POM은
commands 단일 writer다. `adapters/blob`는 evidence가 소유한다.
evaluation이 기존 DefinitionValidator와 그 unit test의 quantitySum/
stateQuantity 검증도 소유한다. 나머지 정의 발행 lifecycle은 후속 S5다.
긴 Maven/verify 실행은 전체 동시2개이며 slot 반환 후 다음 writer에게
배정한다. 개별 worker의 부분 PASS는 통합 인수로 집계하지 않는다.

## 먼저 고정한 결합 계약

공개 capability ID는 기존 계획/fixture의 `createWork`, `createDraft`,
`transferObligation` 등으로 유지하고 semanticVersion을 별도 표현한다.
정의는 실제 조직별 immutable version/hash로 해석하며 임의 문자열
allowlist로 구현된 요청을 거부하거나 최신 의미로 바꾸지 않는다.

S1의 `Works`, `GoalReferences`, `AssessmentReferences`,
`ObligationReferences`를 권위 상태·불변 이력·현재 assignment로 확장한다.
별도로 변경하는 shadow state를 만들지 않는다. Work와 책임/평가 ports로
같은 DB transaction의 인가·상태 전이·판정·의무·감사·outbox·멱등 결과를
결합한다. query가 새 판정을 쓰거나 단순 시간 경과가 승인/도착을
만들지 않는다.

command의 `prepare`는 효과가 없으며 권한·대상·정책·claim fence를
획득한 뒤 revision과 현재 실행 조건을 다시 확인한다. 현재 권한의
시계와 과거 사실의 asOf/knownAt을 분리한다. callback·retry·worker도
같은 명령 경로를 사용하고 payload의 역할/위임을 신뢰하지 않는다.

실제 adapter는 입력 fixture와 독립 raw DB 관찰을 사용한다. expected
assertion을 런타임 답으로 읽거나 미지원 source/control을0으로 채우지
않는다. 공개 projection hash와 PostgreSQL MVCC snapshot을 같은 값으로
복사하지 않으며 비교 근거를 독립적으로 재구성한다. test 전용 caseId는
업무 query 조건으로 오인하지 않고 전송 변환과 관찰 범위에 기록한다.

## 현재 통합과 남은 gate

공통 port9de6c55와 승인 저장7bc8f25, Work ports/lifecycle
f248df9·8775c9c·2f334ae, 책임8fd55de, 판정da25f48,
증거34677c8·608abad, runtime0b4060d를 초기 산출물로 통합했다.
일부는 아직 실제 검사 전 코드다. 최종 commit·검사·결합 실패와 수정은
후속 기록에 연결한다. 이 목록은 S2 완료 선언이 아니다.

S2 종료에는 실제 command gateway를 통한 lifecycle·책임·판정·현재
권한·approval hash·idem·audit rollback·outbox/lease 복구의 결합 증거가
필요하다. S3/S4/S5 영역의 미지원으로 남는 전체 case는 NOT_RUN으로
표시하되 S2 core 기능을 단순 fail-closed stub으로 남기지 않는다.
원본 ERP와 운영 자료는 건드리지 않고 native 개발/검토 외의 유료
모델·BTP 실행은 승인된 범위가 없어 계속 미실행이다.
