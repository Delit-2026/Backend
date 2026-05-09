CREATE SEQUENCE IF NOT EXISTS notification_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT PRIMARY KEY DEFAULT nextval('notification_seq'),
    member_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_type VARCHAR(30),
    target_id BIGINT,
    target_url VARCHAR(500),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_member_created_at ON notification(member_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_member_unread ON notification(member_id, read_at) WHERE read_at IS NULL AND deleted_at IS NULL;
