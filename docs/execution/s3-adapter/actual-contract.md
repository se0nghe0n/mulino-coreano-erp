# S3 실제 adapter 계약과 진행 기록

HTTP 200만으로 업무 성공을 판단하면 보류 응답을 성공으로 오인한다.
S3 adapter는 공개 POST 응답의 정확한 outcome과 독립 PostgreSQL 상태를
함께 검증한다. 사용자 Step3와 시스템 S3의 부분 인수이며 전체
T13–T16·C1·C5·V6 완료나 실제 기관 승인으로 표시하지 않는다.

baseline은 `c6c910b81c8a61fbdf309da9144022482408d5dc`다.
소유 branch는 `step3/s3-adapter`다. domain 구현은 다른 worker가 소유한다.
공통 계약 dependency `a806bbd`, `f9bbd67`과 구매 schema `8294508`을
명시적으로 cherry-pick했다. adapter 고유 commit과 dependency를 구별한다.

`verification/actual/s3`의 clean build custody와 disposable runner는
committed source hash, 실행 JAR/class hash와 PostgreSQL image digest를
기록한다. 각 실행은 새 DB container·서명 키·private blob root를 만들고
생성한 자원만 정리한다. server verification clock ACK를 확인한다.

fixture는 typed 정의·현재 COMMAND 정책·권한·기초 명사와 외부 ORIGINAL
문서·사건·주장만 설치한다. main S3 QuantitySegment seed는 금지한다.
검증·canonical publication·업무 효과는 공개 API에서 수행한다.
독립 SQL은 organization 전체의 실제 table을 하나의 REPEATABLE_READ
snapshot으로 읽고 table별 SQL·boundValues·rowPointer를 기록한다.
미지원 table·누락 수량을0으로 치환하지 않는다.

2026-10-08 focused harness compile와 `ActualS3FixtureSafetyTest`는2개
검사를 모두 통과했다. 첫 실제 실행은79개 bounded assertion 이후
recordHandover HTTP500으로 FAIL했다. ShipmentCommands의 null kind
guard 문제이며 수정은 shipment 담당자가 소유한다. 실행 증거는
`/tmp/mulino-s3-actual-20261008-run1`에 보존했다. 전체 gate 통과를
주장하지 않는다.
유료 모델·BTP·실제 supplier/regulator 전송은 실행 범위에 포함하지 않는다.

V6 native 경로는 receipt60을 실제 loopback proxy를 통해 backend에
보내고 upstream APPLIED 응답을 관찰한 뒤 client socket을 응답 없이
닫는다. SQL로 commit된60을 확인하고 새 jti의 JWT와 새 RPC 두 개로
동시에 재시도한다. token 원문은 저장하지 않고 SHA-256 fingerprint만
기록한다. 변경 payload와 같은 effect key는 conflict이며 READ grant만
있는 다른 actor에게 기존 effect 결과를 주지 않는다.

별도 organization의 `transit-fixture.json`은 이미 존재하는 운송중60과
INITIAL_BALANCE 원장을 명시적인 입력으로 설치한다. 이는 운송 재고의
취득/생성을 검증하지 않는다. 실제 confirmReceipt가 leaf를 이동한 뒤
보유60·운송중0·창고60·RECEIPT_MOVE1을 검증한다. main 구매→신규 수령
fixture는 실물 seed를 포함하지 않는다.

미식별 provisional7은 LOT 없이 실제 receiveProvisional로 기록한다.
독립 SQL의 UNKNOWN/PROVISIONAL1·확정 수령0·재고0과 실제 inventory
eligible0을 검사한다. 이 원본은 canonical publication이나 확정
수령에 사용하지 않는다.

최종 native4는 `./verify actual-s3`에서154개 실제 action과594개
bounded assertion을 통과했다. source798541c6과 JAR3509481f50655589
전체 hash, table별 SQL·bind·snapshot, 원본 hash, 요청·응답, clock ACK,
cleanup은 `checks.json`과 `attempts/native-4.tar.gz`에 보존했다.
앞선3회 실패도 같은 archive 구조로 보존했다. 새 임시 backend·DB·
키·blob root의 cleanup은 모두 true다. focused harness3개도 PASS다.

V6의 두 RPC는 실제로 새 인증과 동시에 시작하지만 transaction 단계의
barrier는 없다. 이미 commit된 결과를 병렬 재시도하는 검증이며
진행 중인 transaction 내부의 controlled overlap을 입증하지 않는다.
전체 normative case corpus와 coordinator aggregate replay는 별도다.
