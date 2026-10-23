# T17 원본 분할 실행과 독립 대조

고정27ec8cc3의 native Workflow `wf_62923727-846`이 T17 원본15개
subcase와257개 assertion을 모두 기록했다. 결과는79 PASS·178 FAIL이며
PASS한 subcase는 없다. 누락 assertion은0이다. 전체 인수 gate는 false이고
Task와 현재 Step은 OPEN이다.

| Shard | Assertions | PASS | FAIL | 첫 실패 |
|---|---:|---:|---:|---|
| S01 | 58 | 24 | 34 | eligible-after-reserve-4 |
| S02 | 60 | 19 | 41 | actual-delivered-1 |
| S03 | 66 | 16 | 50 | claim-inventory-effects-3 |
| S04 | 37 | 12 | 25 | old-allocation-1 |
| S05 | 36 | 8 | 28 | eligibility-response-1 |

각 wrapper는 한 번 실행돼 exit1과 원본 report를 남겼다. 모델 node는
setup·shard5개·fanin의7개다. primary는 journal, 모든 node metadata와
assistant record113개에서 실제 `claude-haiku-5-5`를 확인했다.
advisor와 Monitor 호출은 없었다. 최대 동시 shard 수는3이다.

primary는 각 tree의 소스3,163개 hash와 원래 assertion ID의 정확한 합집합을
독립 대조했다. 모든 tree는27ec8cc3이고 clean이다. 실제 실행 JAR의 hash가
각 빌드 산출물과 일치하며 product class322개의 전체 hash map도 같다.
JAR archive hash 차이는 코드 차이로 처리하지 않았다. 자체 container5개는
모두 부재를 확인했다. 원본 case와 fixture는 변경하지 않았다.

각 report의 gateComplete·prerequisiteRuntimeComplete는 false이고
complete는 ABSENT다. completed=3은 행 개수다. partialFixtures=true와
relaxWire=false를 유지했다. 이 shard 합집합을 전체 profile coverage
receipt나 전체 제품 PASS로 올리지 않는다.

원본은 `scenario-t17-shards-20261023/S01..S05`의
`.local/native-scenario-shard/`에 있다. controller 산출물은t3-65c7a301의
`.local/native-t17-five-shards-27ec-20261023/`이다. primary 독립 증거는
`.local/resume-20261023/native-t17-five-shard-audit/proof.json`이다.
178개 실패의 실제 효과·공개 조회·selector·관찰 계약 분류는 진행 중이다.
