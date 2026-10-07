# Step 2 공통 계약의 부분 검토

기록일은 2026-10-07이다. 사용자 Step 2는 ACTIVE이며 B2는 아직
동결하지 않았다. 이 기록의 정적·harness 검사는 제품 인수가 아니다.
전체 시나리오 통합 뒤 필수 두 모델 review를 별도로 수행한다.

## 통합과 실행

| 산출물 | Task commit | 관찰 |
|---|---|---|
| 독립 규범 catalog | `7f972dd` | 122 oracle·499 observation·41 case·D26 |
| 공통 harness | `32c446f` | Java21/Maven·Cucumber8.0.4·JUnit6.1.2 |
| 실모델 corpus 수정 | `b8ee1ae` | 60 case·21 외국어/혼합·180 planned attempts |

coordinator가 catalog validator와 자체 tests24개를 실행해 통과했다.
`b8ee1ae` 통합 뒤 `./verify harness`는 tests18·failure0·error0·skip0,
`validate.py`는 VALID, corpus 자체 tests47개는 PASS였다. 최초 corpus
명령에서 존재하지 않는 `validate_corpus.py`를 지정해 exit2가 났고,
README의 실제 명령 `verification/model-corpus/validate.py`로 바로잡았다.
이 경로 오기는 의미 있는 contract RED로 세지 않는다.

harness writer의 실행 증거는
[검사 기록](../../verification/harness/checks.md)에 있다. 한국어 Gherkin의
명시적 RED와 file-selector RED는 각각 scenario1·NOT_IMPLEMENTED 실패1·
exit1이었다. 실제 제품 profile8개는 NOT_RUN·exit2였다. 이 증거는
runner의 발견/실패 구분만 확인하며 실제 제품 효과를 검증하지 않았다.

## Catalog의 해소된 P2

실제 GPT-6.1 Sol xhigh가 `7f972dd`에서 이전 세 P2의 scoped closure
PASS를 반환했다. UNKNOWN/CONFLICT 부정과 상충 보존, EXISTS_IN과
끝 시점·THROUGHOUT의 구분 및 정상 구간 충족, 처분/정산 MANAGER
확인 전 효과0과 확인 후 효과를 추가했다. 기존 oracle 삭제나 약화는
없다. 구체 업무 assertion의 충족 여부는 사례 코드 통합 뒤 확인한다.

## 공통 runner에서 확인한 P2

coordinator가 발견한 두 항목과 실제 GPT-6 Astra low가 `5aec49e`
writer source에서 발견한 여섯 항목을 수정한다. 대상은 통합된
`32c446f`와 같다. 아래 항목이 해결되기 전 공통 계약 gate를 닫지 않는다.

- Gherkin 전체 문자열 검색을 subcase별 실제 단계·순서 검사로 바꾼다.
- registry의 unique case ID·path·feature·subcase 집합을 정확히 대조한다.
- DB 관찰은 요청 scope·snapshot·sources와 응답의 실제 범위를 대조한다.
- barrier ACK는 요청 barrier·participant·transaction·point·state와 대조한다.
- absent는 null/scalar 부모를 관찰한 부재로 인정하지 않는다.
- 모든 async start는 연결된 terminal await가 있어야 완료할 수 있다.
- decimalDelta는 전후의 실제 단위를 모두 확인한다.
- 일부 미실행이 있어도 실행된 관찰에서 확인한 위반은 FAIL로 남긴다.

## Corpus의 부분 closure

실제 GPT-6 Astra low가 `b8ee1ae`에서 M57 권한, M56 목표100·기여,
전60건 slot 출처297개, effective fixture와 감사 whitelist의 이전
P2 다섯 항목이 해소됐음을 확인했다. SIT/UAT 경로 분리에는 다음 두
충돌이 남아 같은 작성자가 수정한다.

- 근거 있는 사전 중단에 필요한 조회의 READ_AUDIT를 명시적으로 허용한다.
- M47의 사전 중단 효과0과 SUSPENDED·새 재인가 의무 요구를 분리한다.
  실제 서버 거부/보호 전이의 oracle는 보존하고 조회만으로 쓰기 전이가
  일어난다고 가정하지 않는다.

corpus의 실제 harness binding은 후속 산출물이다. 실제 모델 호출은0회,
usage/비용은 null이며 비용 승인 R8은 대기다. 이 부분 검토와 검사는
전체 Step 2 또는 DB/API/MCP/client/model/BTP의 PASS가 아니다.
