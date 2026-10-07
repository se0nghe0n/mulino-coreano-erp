# S2 부분 adversarial review

기준선 `b3c25c555f1a732d84dd3cc9a21f30f398b2f88d`를 실제
GPT-6.1 Sol xhigh와 GPT-6 Astra low가 읽기 전용으로 검토했다.
이는 사용자 Step3 전체 검토가 아니다. 두 reviewer는 아래 6건을
지적했으며 수정 후 결합 실행과 재검토 전까지 지적을 닫지 않는다.

| reviewer / 우선순위 | 반례 | 수정과 남은 검증 |
|---|---|---|
| Astra P1 | 같은 RESPONSE_COMPLETED 50으로 서로 다른 50 의무를 두 번 해소한다 | stable root·범위·원본 증거를 결합하고 불변 completion credit을 원자적으로 소비한다. 책임 영역 실제 PG 포함 21검사 통과. Evidence typed provider와 공개 HTTP 결합 확인이 남았다. |
| Astra P2 | 기간 끝 제외 입력이 사라져 정확히 종료 시각의 수량을 포함한다 | Goal의 start/endInclusive를 보존하고 평가에 적용한다. 평가 22검사와 Work gateway 5검사 통과. 최종 결합 재검사가 남았다. |
| Sol P1 | 다중 WORK의 일부 권한만 남아도 전체 저장 결과를 replay한다 | 저장 scope의 모든 대상과 현재 위임 chain을 재인가한다. 실제 W2 권한 철회 후 replay 거부를 포함한 control 13검사 통과. 이후 denial audit assertion 변경은 최종 재검사 대상이다. |
| Sol P1 | Work/Evidence의 정상 결과를 공통 gateway가 거부해 rollback한다 | 공통 성공 outcome은 APPLIED로 맞추고 업무·증거 상태를 별도 필드로 보존한다. Work gateway 통과. Evidence gateway의 결합 검사는 진행 중이다. |
| Sol P2 | 기존 멱등키의 거부와 내용 충돌에 감사가 남지 않는다 | 기존 COMMITTED 결과를 보존하고 같은 command에 별도 안전한 denial audit를 연결한다. 최신 공통 검사 18개가 통과했다. |
| Sol P2 | 기본 서버에 S0의 독립 쓰기 경로가 남는다 | REST/MCP/command를 명시적인 platform-spike profile로 격리하고 CAP generic READ까지 서버 전용 역할로 제한한다. 기본 profile 실제 HTTP 거부 재검사가 남았다. |

공통 검사 18개는 CommandSubject5·CommandEnvelope3·
CommandTransaction10이며 실패·오류·skip은 없다. 해당 worker source에는
subject 결합 `3d44a08`·`b89cd54`와 기존 감사/terminal record 수정이
포함됐다. 이 숫자를 전체 S2 인수로 합산하지 않는다.

추가로 command의 선언 subject와 실제 효과 대상의 결합을 명시적인
handler 계약으로 보완한다. 임의로 인가 scope에 포함됐다는 이유만으로
관련 없는 ID를 subject로 인정하지 않는다. 실제 Work 대상 불일치와
잘못된 명사형을 공개 gateway에서 거부하는 반례를 확인한다.

## 현재 통합 실패 기록

최초 전체 backend 검사 `2479333`은 140개 중 106 PASS·2 FAIL·
32 ERROR였고 원본 증거를 보존했다. 이 결과는 최종 상태가 아니다.
이후 S1 fixture 기대값·JDBC Instant·물량 SQL·mock binding·명시적
schema compatibility inventory를 수정하고 영역별 재검사를 수행했다.

runtime pass2는 `7248738`에서 6 PASS·3 ERROR다. intake fixture의
stableRequestOwner가 UUID 저장 폭을 초과해 seed에서 실패했다.
`3c75e92`의 pass3는 7 PASS·1 FAIL·1 ERROR다. intake 후속 책임 연결과
외부 명령 claim의 현재 권한 경로를 조사 중이다. 두 실행 모두 source가
실행 중 바뀌지 않았으며 실패를 성공으로 바꾸거나 제외하지 않았다.

원본 로그·XML과 hash는
[pass2 manifest](../evidence/step3-s2/runtime-pass2/manifest.json)와
[pass3 manifest](../evidence/step3-s2/runtime-pass3/manifest.json)에 있다.
S2와 사용자 Step3는 ACTIVE이며 S3로 진행하기 전 모든 필수 결합
검사와 지적의 closure를 완료한다.
