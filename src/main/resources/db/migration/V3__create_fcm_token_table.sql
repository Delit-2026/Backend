CREATE SEQUENCE IF NOT EXISTS fcm_token_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS fcm_token (
    fcm_token_id BIGINT PRIMARY KEY DEFAULT nextval('fcm_token_seq'),
    member_id BIGINT NOT NULL,
    token VARCHAR(4096) NOT NULL,
    device_id VARCHAR(100),
    platform VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_fcm_token_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_fcm_token_member_id ON fcm_token(member_id);
