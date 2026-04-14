CREATE TABLE IF NOT EXISTS blacklist (
    user_id BIGINT PRIMARY KEY,
    reason TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NULL
);

CREATE TABLE IF NOT EXISTS message_forward (
    id BIGSERIAL PRIMARY KEY,
    source_chat_id BIGINT NOT NULL,
    source_message_id INTEGER NOT NULL,
    from_user_id BIGINT NOT NULL,
    from_username TEXT NULL,
    message_date TIMESTAMP NOT NULL,
    text TEXT NULL,
    status TEXT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP NULL,
    last_error TEXT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_message_forward_source
ON message_forward (source_chat_id, source_message_id);

CREATE INDEX IF NOT EXISTS ix_message_forward_status_attempt
ON message_forward (status, last_attempt_at);
