-- 공급업체 master 확장. 기존 KRW 계산·FK·이력은 보존한다.
CREATE TYPE payment_terms AS ENUM ('NET_30', 'NET_60', 'COD', 'PREPAID');
ALTER TABLE suppliers
    ADD COLUMN address_line VARCHAR(200),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN payment_terms payment_terms NOT NULL DEFAULT 'NET_30',
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'KRW' CHECK (currency ~ '^[A-Z]{3}$'),
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
-- 기존 행의 최종 수정 시각은 알 수 없으므로 NULL로 유지한다.
ALTER TABLE suppliers ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
