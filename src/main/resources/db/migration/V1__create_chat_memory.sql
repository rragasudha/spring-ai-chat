-- Spring AI JdbcChatMemoryRepository expects this exact schema
-- Each row is one message in a conversation
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    id              BIGSERIAL       PRIMARY KEY,
    conversation_id VARCHAR(256)    NOT NULL,
    content         TEXT            NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    timestamp       TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_memory_conversation
    ON spring_ai_chat_memory(conversation_id, timestamp DESC);
