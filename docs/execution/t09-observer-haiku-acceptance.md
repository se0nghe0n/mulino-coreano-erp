# T09 관찰 원천 수정의 실제 회귀

1692f74f에 고정한 원본 T09의 9 subcase·94 assertion이 모두 통과했다.
이 결과는 해당 scripted 시나리오의 부분 인수이며 전체 Task와
Step·S5/S6의 완료를 뜻하지 않는다.

2026-10-24 Haiku 5.5 medium native dynamic Workflow
`wf_681f5144-c24`가 setup, runner3개, fanin의 5개 node를 실행했다.
native task는 `wf9yzc5fd`다. 기존 구독 인증을 사용했고 각 runner는
격리 tree의 wrapper를 한 번만 실행했다. 최대 동시는3이다.

| shard | subcase | assertions | PASS | FAIL | 미관찰 |
|---|---:|---:|---:|---:|---:|
| T09-S01 | 3 | 39 | 39 | 0 | 0 |
| T09-S02 | 3 | 23 | 23 | 0 | 0 |
| T09-S03 | 3 | 32 | 32 | 0 | 0 |
| 합계 | 9 | 94 | 94 | 0 | 0 |

primary가 원본의 subcase별 assertion ID·case bytes, source3198개,
wrapper hash와 raw 실행 artifact를 대조했다. 전체 ID는 subcase ID와
함께 비교했다. 반복되는 assertion의 짧은 이름만 세지 않았다.
5개 실제 Haiku metadata와 assistant transcript85개, Workflow journal의
launched1·started5·result5를 확인했다. 실행된 JAR별 hash를 연결하고
그 안의 product class330개가 모두 같음을 확인했다. JAR zip 전체의
bytes는 서로 달랐으며 이를 같은 JAR hash라고 표시하지 않는다.
세 tree는 clean이고 source drift가 없다. 소유한 DB3개가 없음을
확인했다.

통합 전제는 1692f74f harness920 PASS·80 opt-in SKIP, prepare exit0과
08e2a7a5 backend827 PASS다. 1692f74f의 backend/database는 그 backend
실행과 bytes가 같음을 별도 binding으로 확인했다. 이후 T11·Sales의
제품 통합은 이 결과의 실행 기준선에 포함하지 않는다.

raw `gateComplete=false`, `prerequisiteRuntimeComplete=false`는 그대로
보존했다. Main의 gate는 명시적 subcase 선택이 없어야 true가 되므로
이번 선택 실행은 전체 profile receipt가 아니다. fanin이 wrapper의
성공을 gateComplete=true로 잘못 집계한 것은 controller가 raw false로
정정했다. primary audit도 raw artifact를 직접 읽어 false를 확인했다.
모델 업무 client·model corpus·운영 및 BTP 인수는 이 결과로 대체하지
않는다.

실행 전후 원문과 독립 audit의 기준 경로는 root의
`.local/resume-20261024/fix-until-done/`다.

- `native-preparation-t09/manifest.json`: 정확한 격리 tree와 원래 ID다.
- `native-audit-t09/audit.py`, `proof.json`, `failures.json`: primary 대조다.
- `native-audit-t09/workflow-config.json`: 실제 journal·transcript와
  controller 산출물의 절대 경로다.
- `native-audit-t09/gate-explanation.json`: gate의 코드 조건을 연결한다.

Task coordinator에 한정된 결과 알림 규칙과 달리 Haiku child가 subtask의
Grok 알림을 한 번 보냈다고 보고했다. Task 완료 알림으로 세지 않는다.
후속 worker brief에는 subtask 알림 금지를 명시했다. 전체 Task는 OPEN이다.
