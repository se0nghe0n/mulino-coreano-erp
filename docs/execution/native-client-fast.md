# 구독 client의 Sol Fast 설정

2026-10-23 사용자의 Ultrafast 중단과 Sol Fast 지시를 실제 client
설정에도 적용한다. 현재 승인 pin은
`verification/native-client/subscription-approval-fast-20261023.json`이다.
이전 승인·Ultrafast preflight artifact는 당시 기록으로 보존한다.

NativeClientPolicy와 격리 RuntimeHome은 gpt-6.1-sol/high 및
serviceTier=priority를 요청한다. Ultrafast feature override를 제거한다.
요청 tier가 catalog에 없거나 실제 thread가 다른 tier로 해석되면
모델 turn을 시작하지 않는다. 기존 구독 인증, API key·구매 credit
거부와 결정적 선행 인수 gate는 그대로다.

모델 호출 없는 계약 검사35건이 실패·오류·skip0으로 통과했다.
Ultrafast로 해석된 thread를 거부하는 반례도 실행했다. 설치된
codex-cli 0.163.0의 실제 preflight는 ChatGPT 구독, credit fallback 없음,
Sol/high/priority와 dynamic tool schema 수락을 확인했다. turnStarted는
false이고 actualModelCalls는0이다. 이 결과는 모델 corpus 또는 업무
인수가 아니다.

원로그·JUnit·현재 설정과 이전 승인 hash·격리 임시 경로 정리는
`.local/resume-20261023/native-client-fast/`에 보존한다. Task와 현재
Step은 OPEN이며 실제 model/client 인수의 결정적 선행 조건을 계속
해소한다.
