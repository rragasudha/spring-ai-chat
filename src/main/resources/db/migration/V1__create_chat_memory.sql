-- Spring AI JdbcChatMemoryRepository expects this exact schema
-- Each row is one message in a conversation

CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    id              BIGSERIAL       PRIMARY KEY,
    conversation_id VARCHAR(256)    NOT NULL,
    content         TEXT            NOT NULL,
    type            VARCHAR(50)     NOT NULL,   -- USER, ASSISTANT, SYSTEM, TOOL
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Fast lookup by conversation — used on every chat request to fetch history
CREATE INDEX idx_chat_memory_conversation
    ON spring_ai_chat_memory(conversation_id, created_at DESC);
