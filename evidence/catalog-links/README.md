# 고정 수량 관찰과 보조 assertion의 연결 수정

기존 `Main.catalogLinks`는 수량 관찰에 연결된 모든 assertion에 같은
고정 수량·단위를 요구했다. T11의 독립 합계60 BOX를 검증하는
`sumEquals`와 부모 둘의 비중첩30/30 실물 범위를 검증하는 `relationSet`을
같이 연결하면 후자의 tuple이 수량60이 아니라는 이유로 실패했다.

`CatalogLinkValidator`는 관찰별로 고정 수량·단위를 직접 검증하는 주
assertion이 적어도 하나 있는지 검사한다. 동일 관찰의 identity/relation/
count 및 다른 수량 측면은 보조 assertion으로 연결할 수 있다. 이들을
삭제하거나 다른 constraint로 옮겨서 준비 검사를 통과시킬 필요가 없다.

exact 수량은 `decimalEquals`, `sumEquals`, `decimalDelta`를 주 assertion
operator로 인정한다. 상한·하한은 각각 `decimalAtMost`,
`decimalAtLeast`로 검증하며 bound를 exact 조건으로 바꾸지 않는다.
기대값은 독립 decimal 상수, 단위는 고정 단위와 실제 `unitSource`를
요구한다. result self-copy, true/presence/count/identity-only 연결은
수량 주 assertion을 대신할 수 없다. delta는 실제 baseline과 단위
source를 요구하며 동일 source를 자기 baseline으로 쓰지 못한다.

unknown oracle/관찰, 다른 case의 연결, source hash drift는 계속 거부한다.
같은 관찰의 여러 수량 측면 중 어떤 assertion이 업무 의미를 입증하는지는
최종 case review의 대상이다. 이 구조 검사는 원장 partition의 의미
완전성이나 제품 실행 coverage를 자동 증명하지 않는다.

## 실행 증거

baseline은 `step2-b2`의 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다.
Java는 Zulu21.0.5, Maven은 wrapper3.9.16이다.

- `./verify harness`: exit0, 전체121개 tests, 실패/오류/skip0이다.
  새 `CatalogLinkValidatorTest`30개가 포함된다.
- 실제 T11 author worktree의 파일을 읽기만 하는 재현에서 원래의
  sum60+relationSet30/30 연결은 통과했다. 주 assertion 제거,
  기대값59, 단위EA 변경은 각각 실패했다.
- `git diff --check`: 통과했다.

`harness-verification.json`, `wrapper-commands.json`과
`t11-reproduction.json`에 실행 결과와 입력/helper hash를 남겼다.
초기 추가 test의 method return-type 오타와 identity 응답 fixture의
EXECUTED/scopeComplete metadata 누락으로 생긴 실패를 수정한 뒤
최종 전체 검사를 통과했다. 이 실패들은 업무 RED로 세지 않았다.
처음 shell 생성의 OS file-descriptor 오류는 code를 읽기 전에 발생했고
process가 회복된 뒤 진행했다. OS의 영구 설정을 바꾸지 않았다.

## 읽기 전용 재현

root에서 harness를 compile하고 `dependency:build-classpath`로 생성한
`verification/harness/target/classpath.txt`를 classpath에 사용한다.
`LinkageProbe.java`를 target의 별도 output에 compile한 뒤 다음 인자를
전달한다.

```text
org.mulino.verification.LinkageProbe
  <repository root>
  <actual T11 case.json absolute path>
  <output evidence JSON absolute path>
```

probe는 대상 oracle의 assertion을 그대로 복사해 구조 검사를 한다.
counterexample은 메모리에서만 변경하며 원 case/catalog 파일은 쓰지
않는다. 기록된 재현 당시 실제 case hash는 `t11-reproduction.json`에
있다. 제품 DB/API/MCP/실제 모델은 실행하지 않았고 runtime 상태는
`NOT_RUN`이다. 다른 공통 수정과 전체 case의 통합 검증은 coordinator가
수행한다.
