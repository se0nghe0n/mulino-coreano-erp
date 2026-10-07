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
문서·사건·주장만 설치한다. S3 QuantitySegment seed는 금지한다.
검증·canonical publication·업무 효과는 공개 API에서 수행한다.
독립 SQL은 organization 전체의 실제 table을 하나의 REPEATABLE_READ
snapshot으로 읽고 table별 SQL·boundValues·rowPointer를 기록한다.
미지원 table·누락 수량을0으로 치환하지 않는다.

2026-10-08 focused harness compile와 `ActualS3FixtureSafetyTest`는2개
검사를 모두 통과했다. product 종단 실행은 아직 NOT_RUN이다.
유료 모델·BTP·실제 supplier/regulator 전송은 실행 범위에 포함하지 않는다.
