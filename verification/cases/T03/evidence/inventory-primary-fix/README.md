# 수량 primary3건 수정 검증

통합 prepare가 수량40/0/0에 대한 정식 primary3건 누락을 발견해
해당 assertion만 보강했다. 카탈로그나 공통 validator는 바꾸지 않았다.

- T03 `source-affected-1`은 실제 trace value40과 unitBOX를 읽는
  decimalEquals primary다. 기존 unit·계보 assertion도 유지했다.
- C1 `new-reservation-quantity-primary`,
  `new-dispatch-quantity-primary`는 각각 독립DB의 해당 commandKey
  allocation/movement 원행 quantity 합계0을 검사한다. 실제 단위는
  같은 snapshot의 실물CON40 원행에서 읽으며 BOX여야 한다.
  완료된 빈 효과 scope만0으로 합산하고 누락/null/미구현은 거부한다.
  기존 count0·전후DB·감사 assertion은 유지했다.

실행 시점은 `25b634b` 다음 소유 수정이 미commit인 상태였다.
각 명령의 exact argv와 exit는 `*-commands.json`에 있다.

- `./verify harness`: exit0, 145 tests, failure/error/skip0이다.
  inventory15개에는 신규 primary3건의 실제 Engine mutant가 포함된다.
  잘못된 수량, 실물 unit, 누락/null scope, incomplete 및 미구현을
  거부했다.0수량의 잘못된 effect row도 기존 count0이 거부했다.
- `./verify validate` T03/C1: exit0, 두 case schema valid다.
- `./verify contract-red` T03/C1: exit1, 실제 Gherkin file-selector의
  expected/discovered/started/NOT_IMPLEMENTED는10/10/10/10, skip0이다.
- Gherkin과 assertion 순서는 전수 일치한다. 전체 inventory는7 case,
  32 subcase,528 assertion,91 named observation으로 유지한다.

raw log는 lossless gzip으로 보존하고 summary에 원 bytes SHA256을
기록했다. 최초526 assertion 검증 증거는 `../inventory-final/`에
보존하며 이 수정의 최신 계약 hash/RED는 이 폴더에서 확인한다.
전체41 registry prepare는 coordinator가 통합 뒤 실행한다.
실제 제품/API/DB/host/model은 NOT_RUN이며 RED는 제품 PASS가 아니다.
