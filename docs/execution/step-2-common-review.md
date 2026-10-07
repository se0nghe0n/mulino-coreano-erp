# Step 2 공통 계약의 부분 검토

기록일은 2026-10-07이다. 공통 계약 gate를 닫고 로컬 tag `step2-b2`를
영역별 작성 기준점으로 사용한다. 사용자 Step 2는 ACTIVE다. 이 기록의
정적·harness 검사는 제품 인수가 아니며, 전체 시나리오 통합 뒤 필수
두 모델 review와 결합 검사를 별도로 수행한다.

## 통합과 실행

| 산출물 | Task commit | 관찰 |
|---|---|---|
| 독립 규범 catalog | `7f972dd` | 122 oracle·499 observation·41 case·D26 |
| 공통 runner와 관찰 보강 | `591025f`, `d6ddd4a`, `485ae34` | 원 행·비동기·단위·strict identity·wire 계약 |
| corpus 경로별 oracle | `c7aad7e` | 60 case·73 turn·21 외국어/혼합·180 planned attempts |
| 실제 Gherkin·registry 준비 검사 | `dadff73` | scenario별 순서·역할·assertion·exact registry |
| host 관찰 계약 | `95ccf9f` | 23 operation·독립 artifact·실제 분류 대조 |
| typed runner 연결 | `611330d` | EXECUTED process control에 host validator 직접 적용 |

coordinator는 `611330d076d5d2173ad838d2d3c8e2288bc938b7`에서
`./verify harness`를 실행했다. 91 tests, failure0·error0·skip0, exit0과
한국어 smoke scenario1을 확인했다. preparation39·host10·typed wiring을
포함한다. [결합 검사 기록](evidence/step2-b2/summary.json)과
[실행 log](evidence/step2-b2/combined-harness.txt)를 보존한다.

기존 독립 catalog 자체 tests24개와 corpus 자체 tests61개는 통과했다.
이후 해당 코드 변경이 없어 반복하지 않았다. writer의
[typed wiring 검사](../../verification/harness/evidence/host-wiring/summary.json)는
harness52 PASS, classpath/file-selector의 scenario1·NOT_IMPLEMENTED
assertion 실패1·skip0·exit1, scenarios/recovery NOT_RUN·exit2를 기록한다.
RED는 runner가 미구현을 발견한 증거이며 제품 효과의 성공이 아니다.

## 해소한 review 지적

실제 GPT-6.1 Sol xhigh와 GPT-6 Astra low의 부분 검토를 수행했다.
전체 Step 2의 최종 review와 구별한다.

- Sol xhigh는 `7f972dd`에서 catalog의 UNKNOWN/CONFLICT 보존,
  EXISTS_IN/끝 시점/THROUGHOUT 구분, 처분·정산 확인 전후 효과의
  세 P2가 해소됐음을 확인했다.
- 공통 runner는 요청 scope·snapshot·각 source의 원 행/완료/query,
  barrier 요청과 ACK identity, 관찰하지 않은 absent 거부, 모든 start의
  terminal await, decimalDelta 양쪽 단위, 실행된 위반의 FAIL 보존을
  구현했다. Astra low는 수정 범위의 closure를 확인했다.
- coordinator가 발견한 Gherkin 전체 문자열 검색과 느슨한 registry를
  실제 Pickle scenario별 검사와 canonical exact registry로 교체했다.
  Sol xhigh는 `dadff73`의 두 P2에 scoped PASS를 반환했다.
- corpus의 권한·목표 수량·slot 출처·effective fixture·감사 whitelist와
  SIT/UAT 사전 중단/서버 거부 경로를 수정했다. Astra low는 `c7aad7e`의
  기존 P2가 모두 해소됐음을 확인했다. M47의 현재 READ 조회를 철회된
  WRITE 복구나 묵시적 SUSPENDED 전이로 취급하지 않는다.
- identity suffix 추측은 업무 Boolean/수량 self-copy를 허용했다.
  case-sensitive exact allowlist와 실제 ID/hash/revision 타입 검사로
  바꿨고 Astra low는 `485ae34`에 scoped PASS를 반환했다.
- host/provenance label만 바꿔 selftest를 실제 증거로 표시할 수 있었다.
  원 artifact·transcript·snapshot의 분류와 metadata를 대조하도록
  수정했다. Sol xhigh는 `95ccf9f`에서 기존 변조와 driver/argv까지 바꾼
  반례를 재실행해 거부를 확인했고 scoped PASS를 반환했다.

typed wiring은 reflection 없이 `HostObservationValidator.validate`를
직접 호출한다. hostless ACK와 빈 host metadata를 거부하며 미구현 결과의
NOT_IMPLEMENTED/null 및 제품 NOT_RUN을 유지하는 회귀 검사를 포함한다.

## B2 이후의 범위

같은 `step2-b2`에서 10개 case writer와 별도 model-binding writer를
격리한다. [작성 인계](step-2-case-handoff.md)의 소유권과 인수 조건을
따르며 공통 registry·전체 coverage manifest는 coordinator가 통합한다.
case별 구체 assertion이 규범 의미를 충족하는지는 이후 review 대상이다.

실제 모델 호출은0회이고 usage/비용은 null이며 R8은 대기다. DB/API/MCP,
client/model/BTP·규제·운영 인수는 모두 NOT_RUN이다. B2 동결이나 이
부분 검토는 사용자 Step 2 완료 또는 시스템 S0–S6 PASS가 아니다.
