# 입고 품질 검사와 QC 인간 결정

입고 기본 HOLD는 수령 당시의 안전 상태다. 에이전트의 검사 결과는
ERP status 변경이 아니라 고정된 QC 승인안이다. 이 계약은 #26의 즉시
생산 차단과 #33의 인간 승인 매트릭스를 함께 지킨다.

## 행위와 권한

| 경로 | 행위 | 신원 |
|---|---|---|
| GET /quality/inbound/{id} | 현재 안전 증빙·적격성 조회 | 인간 erp:read |
| POST /quality/inbound/{id}/assign | QC 업무·Run 배정 | OPERATOR/MANAGER |
| GET /agent/quality/inbound/{id} | 배정된 입고 조회 | live QC Run |
| POST /agent/quality/inbound/{id}/inspect | 검사를 고정하고 인간 승인 대기 | live QC Run |
| POST /quality/inbound/{id}/inspect | 인간의 직접 검사 요청 | OPERATOR/MANAGER |
| GET /quality/approvals/{id} | 승인안·버전·hash·최종 결정 조회 | 인간 erp:read |
| POST /quality/approvals/{id}/decision | APPROVE/BLOCK/CANCEL | 활성 DB QC 인간 |
| POST /quality/production-inputs | 승인된 LOT 생산 투입 기록 | OPERATOR/MANAGER |

쓰기에는 Idempotency-Key가 필요하다. 검사·배정 body는 caseRef다.
결정 body는 decision, expectedVersion, proposalHash, reason이다.
생산 body는 productionRecordId, rawMaterialLotId, quantity다.
로컬 PoC만 위 신원을 허용한다. 기본 profile의 기존 익명 foundation
경로는 유지되며 새 품질 경로는 거절한다. ADMIN에게 구매 결정이나
일반 업무 쓰기를 주지 않는다.

## 검사와 적용

검사는 한국 canonical 22종 master, 원재료의 명시적 알레르겐 분류와
is_trace 매핑, 입고일과 업무 기준일의 HACCP, 공급사 활성 상태,
발주 원재료·공급사 일치, 소비기한, 온도 범위와 실제 기록을 검사한다.
ALLERGEN_FREE 선언은 실제 알레르겐 link가 없어야 한다. 선언이나
증빙이 빠지면 BLOCKED를 제안한다. HACCP 만료 30일 전 Attention을
남긴다. 기존 이력을 migration에서 임의로 안전하다고 보정하지 않는다.

검사 snapshot과 proposer, 최종 결정, 감사 이력은 수정·삭제할 수 없다.
legacy requested_by FK는 비활성 서비스 user에 연결한다. 실제 제안자는
inbound_inspections의 agent/user FK와 audit snapshot에 별도 보존한다.
서비스 user는 인증 가능한 인간 QC가 아니다.

APPROVE는 snapshot/hash가 현재 자료와 일치할 때만 제안 status를
적용한다. BLOCK/CANCEL은 제안을 반려·취소하고 ERP status를 바꾸지
않는다. 부적격 입고는 planning과 production에서 계속 제외된다.
결정 이후 반품·폐기 판단은 QC 인간에게 별도 Work/Attention으로 남긴다.
검사 결정은 반품·폐기 ERP 적용이나 MFDS 제출을 뜻하지 않는다.

planning revision guard는 입고, LOT, 공급사, 인증, 알레르겐, 온도,
검사 자료를 직렬화한다. snapshot transaction은 경합 시 새 snapshot으로
재시도한다. 생산 guard는 HOLD/BLOCKED, pending/rejected hazard,
소비기한, 입고·생산 기록·위치가 알려진 생산 LOT의 창고와 원재료
일치, 입고일을 검사하며 잔량 delta를 반영한다. 생산 투입도 인간 actor와
input, 잔량 before/after를 같은 transaction의 불변 감사 로그에 남긴다.
replay는 입력·감사·잔량을 중복 적용하지 않는다. 감사 실패는 입력과
잔량, idempotency receipt를 함께 rollback한다.

## 실행과 증거

인간 stdio 도구는 request_quality_inspection, get_inbound_quality,
get_quality_approval, decide_quality, record_production_input이다.
에이전트 CLI는 qc show/inspect이며 인간 decision 명령은 없다.
QM 6개 SIT는 실제 backend, runner, CLI와 인간 stdio MCP를 연결한다.
새 unit은 안전 release·잔량, stale source, 역할·활성 DB 신원, rollback,
최종 증거 불변성, race와 30일 notice를 검증한다. 실제 모델 UAT는
별도 검증 층이며 이 구현에서 유료 실행하지 않았다.
