# T25 전체 추적과 실제 증거의 분리

ID나 파일의 수만 맞추면 누락된 업무 oracle를 숨길 수 있다.
독립 catalog의41 case·D26·122 oracle·499 named observation과
실제 assertion link를 대조한다. 원문 tuple의 type/scope/operator도
같아야 한다. 각 named observation의 assertionLinks는 비어 있으면 실패다.

verifyCoverage는 명시한 선행 입력 snapshot을 독립적으로 검사한다.
currentExecution은 현재 T25 결과를 그 실행의 선행 입력으로 요구하지
않도록 표시한다. 전체 gate는 이 결과를 포함한 모든 case/profile 이후
coordinator의 별도 assembler가 판정한다. preparation과 실제 runtime
artifact 검사는 별도 subcase다. 실제 artifact descriptor는 후속
inspectArtifacts에서 bytes/hash/scope를 직접 검증한다.

oracle/관찰/실행 artifact 삭제, stub PASS, skip, 필수 경로 waiver,
부분 실행 PASS, 확인된 위반을 숨긴 결과를 각각 거부한다. command/version/
fixture hash/expected/observed/exit의 누락도 실패다. QA content review는
별도 실제 artifact로 연결하며 구조검사 성공이 semantic completeness는 아니다.

M60은60건·각3회·최소 외국어/혼합10건이다. 원문 category의
20/10/10/10/10 대응과 binding60/turn73을 읽는다. 실제 model transcript는
별도다. 승인·attempt/retry usage·가격·통화·총비용·version·지연·확인질문이
없으면 MODEL/UAT는 NOT_RUN이며 null/missingReason을0으로 바꾸지 않는다.
R8 전 제안 수용값을 업무 SLA로 확정하지 않는다.

새 제품·host·model·규제·BTP 결과는 이 디렉터리에서 만들지 않는다.
현재 고정 검사와 Gherkin RED는 계약 준비의 증거다.
