# S3 초기 계약 검토

실제 GPT-6 Astra low가 통합 source `d4707e8`을 읽기 전용으로
검토했다. 구현 중의 좁은 검토이며 S3 또는 사용자 Step3 전체 review
완료 판정이 아니다. 테스트는 reviewer가 재실행하지 않았다.

| 우선순위 | 반례 | 조치와 남은 확인 |
|---|---|---|
| P1 | 표시 VERIFIED 뒤 같은 절차/포장/규격의 REJECTED가 와도 과거 VERIFIED가 있으면 적격량을 허용한다 | 현재 효력 있는 표시 결정 revision을 선택하고 미래 결정이 현재를 덮거나 만료 후 과거 승인이 부활하지 않도록 수정했다. 실제 gateway 회귀 실행이 남았다. |
| P2 | nextValidityBoundary가 필수 표시 검증의 시작/만료를 누락한다 | 표시 결정의 시작/만료를 포함했다. QC sweeper와 결합한 시간 경계 실행이 남았다. |

수정 OWN `3dc4e86`, `ccab441`, `6a21254`와 후속 source 결합을
Task branch에 통합했다. fixture 초기화/읽기 전용 Work의 사용 오류가
발견돼 실제 lifecycle로 만든 Work를 쓰도록 고쳤다. 실패는 보존하며
최종 성공 근거가 생기기 전 지적을 완료로 표시하지 않는다.
