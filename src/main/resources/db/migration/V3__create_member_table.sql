CREATE SEQUENCE IF NOT EXISTS member_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('member_seq'),
    login_id VARCHAR(30) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15),
    name VARCHAR(30),
    nickname VARCHAR(30) NOT NULL,
    intro VARCHAR(500),
    profile_image VARCHAR(500),
    location VARCHAR(100),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

ALTER SEQUENCE member_seq OWNED BY member.member_id;

ALTER TABLE member
    ADD CONSTRAINT uk_member_login_id UNIQUE (login_id);

ALTER TABLE member
    ADD CONSTRAINT uk_member_email UNIQUE (email);
