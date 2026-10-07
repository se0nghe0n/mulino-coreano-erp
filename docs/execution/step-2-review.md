# Step2 전체 검토와 수정 기록

41개 제품 사례와 별도60개 모델 사례가 모인 뒤 의미·계약·집계를
함께 검토했다. selftest PASS와 ID 연결만으로 준비 완료를 선언하면
정상 구현을 거부하거나 결함 있는 구현을 통과시키는 테스트가 남는다.
이 기록은 전체 Step2 gate를 추적하며 실제 제품 인수와 구별한다.

## 기준선과 실행 증거

- 전체 review baseline: `9b5e5d7675033666944b1caacc69234b6c4864a5`다.
- reviewer는 실제 GPT-6.1 Sol xhigh와 GPT-6 Astra low다.
- Astra는 공통 host·업무·모델 집계의 독립 검토를 병렬로 통합했다.
  Sol도 인가·경합·host/coverage·모델의 독립 검토를 병렬로 통합한다.
- 결합 harness320건은 failure/error/skip0으로 PASS다.
- 최초 preparation의 수량 primary3개 누락은 `f426712`에서 수정했다.
  수정 후41 case·785 subcase·19996 assertion 준비 검사는 PASS다.
- [최초 실패와 수정 후 결과](evidence/step2-integrated/summary.json)를
  함께 보존한다. 제품 runtime과 S0–S6 인수는 NOT_RUN이다.

## 지적과 처리

아래는 확정된 지적이다. worker 완료만으로 CLOSED로 바꾸지 않는다.
수정 commit 통합·반례 검사·reviewer closure가 모두 필요하다.

| ID | 출처 | 문제와 필요한 교정 | 상태 |
|---|---|---|---|
| A1 | Astra P1 | T26·V5·T14의 host rawRows85개 참조를 실제 schema 경로와 연결한다 | 수정 중 |
| A2 | Astra P2 | V3 물량 baseline에서 잠금용 kind filter를 제거한다 | 수정 중 |
| A3 | Astra P2 | 모델 common assertion과 선택 negative path assertion을 함께 요구한다 | 수정 중 |
| A4 | Astra P2 | 호출별 usage/cost 내용과 전체 합계를 검증한다 | 수정 중 |
| A5 | Astra P2 | parallel timeout 뒤 future 취소와 bounded cleanup을 보장한다 | 수정 중 |
| A6 | Astra P2 | T20 승인 우회는 유효 proposal 입력과 실제 승인 countercall로 검증한다 | 수정 중 |
| A7 | Astra P2 | E2 QC20 해제 성공과 recall60 유지를 각각 검증한다 | 수정 중 |
| A8 | Astra P2 | T26 만료 복구를 사용자 command 전 독립 sweep으로 검증한다 | 수정 중 |
| A9 | Astra P2 | repair dry-run·무권한 APPLY 직후 projection 불변을 각각 검증한다 | 수정 중 |
| S1 | Sol P2 | tick/sweep 제출 identity와 awaitRuntimeTask 계약을 일치시킨다 | 수정 중 |
| S2 | Sol P2 | T10·T11 인계는 대상 담당자의 실제 수락을 요구하고 대리 수락을 거부한다 | 수정 중 |
| S3 | Sol P1 | C3 발주 positive 전에 유효한 MANAGER 승인을 확보한다 | 수정 중 |
| S4 | Sol P1 | C3 정의 전환 positive에 발행된 정의·영향·mapping·전환 승인을 연결한다 | 수정 중 |
| S5 | Sol P2 | T20 MRTR 주체 검사는 동일 권한 두 주체의 자기/타인 state로 분리한다 | 수정 중 |
| S6 | Sol P2 | V8은 첫 lock 보유 중 두 번째 transaction의 실제 DB WAIT를 관찰한다 | 수정 중 |
| S7 | Sol P2 | C4 재정정 적용·새 evidence revision을 먼저 입증한다 | 수정 중 |
| S8 | Sol P2 | 모델 사전 중단의 허위 완료 응답을 독립 상태와 대조한다 | 수정 중 |
| S9 | Sol P2 | 모델 API oracle를 실제 인증 request/response와 같은 key/payload replay에 연결한다 | 수정 중 |

A1은 정상 schema를 따르는 출력도 downstream 참조에서 실패하는
문제다. A3은 공통 assertion만 있는180회는 통과하면서 실제 선택
경로 assertion이 포함된 결과를 누락으로 판정하는 반례가 있었다.
A4는 잘못된 metric object와 숫자가 아닌 비용 문자열을 통과시키는
집계 반례가 있었다. A6·A7·A8·A9·S2는 의도한 업무 효과가 없거나
잘못된 주체가 처리해도 통과할 수 있는 의미 검증 누락이다.

Astra의9건과 Sol의12건 중 V3·E2·usage/cost3건이 겹쳐 독립 지적은
18건이다. 각 reviewer의 최초 전체 판정은 FAIL이며 수정 후 closure가
남았다. 같은 원인의 회귀 범위로 T17 해제·배분 상태와 C3 인계 수락도
확인한다. 확인된 제한을 Step3 구현에 넘겨 해결한 것으로 세지 않는다.

root entrypoint와 별도 모델·coverage 도구의 미연결은 알려진 통합
제한이다. 새 skills가 Step2에서 별도 명령과 실제 adapter 부재를
허용하므로 추가 blocker로 세지 않았다. Step3에서 실제 adapter와
root 명령을 연결해야 한다. default UAT gate는 미승인 유료 호출을
실행하지 않으며 실제 모델 품질을 주장하지 않는다.

## 1차 수정 통합과 부분 closure

아래 산출물을 Task branch에 통합했다. 각 worker의 checks는 수정한
영역의 증거이며 전체 Step2 완료를 뜻하지 않는다.

| 범위 | worker commit | Task commit | 확인한 checks |
|---|---|---|---|
| T10·T11 수락 | `f205e53` | `ca57ea0` | focused19 PASS, schema/feature PASS, RED31/31·skip0 |
| 공통 host·timeout | `3a9de9f` | `d35967d` | targeted27, 당시 결합 harness329 PASS |
| V8 DB WAIT | `6621904` | `fe4e13f` | focused25 PASS, RED12/12·skip0 |
| C4·T17 실제 효과 | `3d459a1` | `d11177b` | 직접 JUnit7 PASS, mutant42 검출, schema PASS |
| host 참조·만료·repair | `8fcb810` | `6140f53` | focused26 PASS, schema3 PASS, RED28/28·skip0 |
| 모델 coverage | `66551fe` | `7c49687` | Python43, schema3 PASS, 실제 assembly NOT_RUN |

`bbff6dd`에서 T26 자동 만료와 지연 commit guard를 독립적으로 검사할
4개 필수 subcase를 registry에 추가했다. `7c49687`의 fresh preparation은
41 case·789 subcase·20255 assertion이며 문제0으로 PREPARED다.
[실행 증거](evidence/step2-integrated/prepare-after-runtime-fixes.json)를
보존했다. 실제 runtime은 NOT_RUN이고 gateComplete=false다.

Astra low는 같은 `7c49687`에서 A1/A3/A4/A5/A9/S1/S2/S6/S7과 T17
회귀 범위의 부분 closure를 확인했다. 다만 A8의 sweep 시점에는
OPEN 의무 수만 검사하고 current assignment·owner·supervisor·
nextAction·nextCheck는 사용자 dispatch 이후 관찰에 남아 있었다.
담당 없는 의무를 sweep가 만든 뒤 dispatch가 책임을 보완하는
반례가 가능해 A8은 아직 열린 상태다. 담당 worker가 dispatch 전
책임과 반복 sweep의 동일성 검사를 추가한다. Sol의 부분 closure와
남은 승인·모델 binding 수정, 전체 결합 검사는 별도로 필요하다.

## 남은 gate

실제 Sol high 수정 산출물을 Task branch에 통합하고 필수 subcase
registry를 갱신한다. 전체 preparation, 결합 harness, 실제 Gherkin
RED의 discovery/start/NOT_IMPLEMENTED/skip 수, 모델 binding 준비와
coverage assembly를 확인한다. 두 reviewer의 전체 지적 closure를
확인한 뒤 Step2 COMPLETE를 판정한다. 현재 판정은 ACTIVE다.
