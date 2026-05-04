CREATE SEQUENCE IF NOT EXISTS wallet_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS wallet_ledger_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS wallet (
    wallet_id BIGINT PRIMARY KEY DEFAULT nextval('wallet_seq'),
    member_id BIGINT NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_wallet_member_id UNIQUE (member_id),
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS wallet_ledger (
    wallet_ledger_id BIGINT PRIMARY KEY DEFAULT nextval('wallet_ledger_seq'),
    wallet_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    description VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(wallet_id),
    CONSTRAINT chk_wallet_ledger_amount_non_zero CHECK (amount <> 0),
    CONSTRAINT chk_wallet_ledger_balance_after_non_negative CHECK (balance_after >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wallet_ledger_member_created_at ON wallet_ledger(member_id, created_at DESC);
