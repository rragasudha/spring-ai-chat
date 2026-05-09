# spring-ai-chat

Production-grade AI chat API demonstrating Spring AI 1.0 advisor chains with semantic caching and persistent memory.

## Stack
- Spring Boot 3.5 + Spring AI 1.0
- Amazon Bedrock Nova Micro (LLM) + Titan Embeddings V2 (semantic cache)
- Redis Stack (vector store for semantic cache)
- PostgreSQL (persistent conversation memory via Flyway + JDBC)

## Architecture

```
User Request
    │
    ▼
[Advisor 1] SemanticCacheAdvisor — checks Redis for similar past questions
    │ HIT → return cached answer instantly (no LLM call)
    │ MISS ↓
[Advisor 2] MessageChatMemoryAdvisor — injects conversation history from PostgreSQL
    │
    ▼
Amazon Bedrock Nova Micro — generates response
    │
    ▼
Response stored in Redis (cache) + PostgreSQL (memory)
```

## Local Setup

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Copy and configure environment
cp .env.example .env
# Edit .env with your AWS credentials

# 3. Run the app
./mvnw spring-boot:run
```

## Endpoints

```bash
# Chat
POST /chat
{"userId": "captain", "message": "What is Spring AI?"}

# View history
GET /history/{userId}

# Clear history
DELETE /history/{userId}

# Run benchmark (shows cache hit rate + cost savings)
POST /benchmark

# Cache stats
GET /cache/stats
DELETE /cache/stats  # reset counters
```

## AWS Deployment

```bash
# On EC2 — IAM role provides Bedrock credentials automatically
git clone https://github.com/rragasudha/spring-ai-chat
cd spring-ai-chat
cp .env.example .env
# Edit .env — remove AWS keys (IAM role handles it), update DB/Redis if needed
docker compose up -d          # starts Redis + Postgres
./mvnw spring-boot:run        # or build jar and run
```
