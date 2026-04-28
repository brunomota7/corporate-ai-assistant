# Corporate AI Assistant

An AI-powered corporate assistant built with a microservices architecture. The system combines a Java Spring Boot API with a Python FastAPI service to deliver intelligent chat with RAG (Retrieval-Augmented Generation), product and order management, and full audit logging.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        Docker Network                            │
│                                                                  │
│   ┌─────────────┐    HTTP     ┌──────────────┐                  │
│   │  api-core   │ ──────────► │  ai-service  │                  │
│   │  :8080      │             │  :8001       │                  │
│   └──────┬──────┘             └──────┬───────┘                  │
│          │ JPA/JDBC                  │ SQLAlchemy               │
│          ▼                           ▼                           │
│   ┌─────────────────────────────────────┐                       │
│   │     PostgreSQL + pgvector           │                       │
│   └─────────────────────────────────────┘                       │
│          │ Spring Data Redis                                     │
│          ▼                                                       │
│   ┌─────────────┐                                               │
│   │    Redis    │                                               │
│   └─────────────┘                                               │
└──────────────────────────────────────────────────────────────────┘
```

**api-core** handles all business logic, security, persistence, and chat orchestration.

**ai-service** encapsulates all OpenAI communication and semantic search logic — it is an internal service, only accessible by api-core within the Docker network.

---

## Tech Stack

| Layer | Technology |
|---|---|
| API | Java 21, Spring Boot 3.5, Spring Security |
| AI Service | Python 3.11, FastAPI, Uvicorn |
| ORM | Hibernate 6 / JPA (Java), SQLAlchemy (Python) |
| Database | PostgreSQL 16 + pgvector extension |
| Cache | Redis 7.2 |
| Auth | JWT (JJWT 0.12) |
| LLM | OpenAI GPT-4o-mini |
| Embeddings | OpenAI text-embedding-3-small (1536 dimensions) |
| Migrations | Flyway |
| Tests | JUnit 5, Mockito 5 (Java) · pytest, pytest-mock (Python) |
| Containers | Docker, Docker Compose |

---

## Prerequisites

- Docker and Docker Compose
- OpenAI API Key ([platform.openai.com/api-keys](https://platform.openai.com/api-keys))
- `openssl` (to generate JWT secret)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/corporate-ai-assistant.git
cd corporate-ai-assistant
```

### 2. Configure environment variables

Copy the example file and fill in the required values:

```bash
cp .env.example .env
```

Edit `.env`:

```env
# Database
POSTGRES_DB=corporate_ai
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password_here

# Spring datasource (used when running api-core locally without Docker)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/corporate_ai
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password_here
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT — generate with: openssl rand -base64 32
JWT_SECRET=your_base64_secret_here

# AI service
AI_SERVICE_URL=http://localhost:8001
OPENAI_API_KEY=sk-proj-your-openai-key-here
```

### 3. Start all services

```bash
docker-compose up --build
```

Expected startup order:

```
1. postgres    → healthy
2. redis       → healthy
3. ai-service  → healthy (Uvicorn running on :8001)
4. api-core    → started (Spring Boot on :8080, Flyway migrations applied)
```

### 4. Verify services are running

```bash
# ai-service health check
curl http://localhost:8001/health

# api-core — attempt a registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"12345678"}'
```

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login and receive JWT |

### Chat

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/chat` | USER, ADMIN | Send a message to the AI assistant |

### Products

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | Authenticated | List active products |
| GET | `/api/products/{id}` | Authenticated | Get product by ID |
| POST | `/api/products` | ADMIN | Create product |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Soft delete product |

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/orders` | USER, ADMIN | List authenticated user's orders |
| GET | `/api/orders/{id}` | USER, ADMIN | Get order by ID |
| POST | `/api/orders` | USER, ADMIN | Create order |
| DELETE | `/api/orders/{id}` | USER, ADMIN | Cancel order (PENDING only) |

### Admin

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/orders` | ADMIN | List all orders |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN | Update order status |
| GET | `/api/admin/audit` | ADMIN | List all audit logs |
| POST | `/api/admin/ingest` | ADMIN | Index a document into the RAG |

### Audit

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/audit` | USER, ADMIN | List authenticated user's audit logs |
| GET | `/api/audit/session/{sessionId}` | USER, ADMIN | List logs for a specific session |

---

## Usage Examples

### Register and login

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Bruno","email":"bruno@example.com","password":"12345678"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"bruno@example.com","password":"12345678"}'
```

### Ingest a document into the RAG (ADMIN required)

```bash
curl -X POST http://localhost:8080/api/admin/ingest \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Product Catalog",
    "content": "The Dell Notebook is a corporate device priced at $4500 with 10 units in stock.",
    "sourceType": "PRODUCT"
  }'
```

### Chat with the assistant

```bash
# First message — server generates sessionId
curl -X POST http://localhost:8080/api/chat \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "What products are available?"}'

# Follow-up — reuse sessionId to maintain context
curl -X POST http://localhost:8080/api/chat \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the price?", "sessionId": "<sessionId>"}'
```

### Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": "<product-uuid>", "quantity": 2}],
    "notes": "Urgent delivery"
  }'
```

---

## Chat Flow

```
POST /api/chat + JWT
       │
       ▼
JwtFilter — validates token
       │
       ▼
ChatController — extracts User + IP from token
       │
       ▼
ChatService
  ├── ChatHistoryService.getHistory()     → Redis (session context)
  ├── AiServiceClient.search()            → ai-service /search
  │       └── pgvector semantic search
  ├── AiServiceClient.chat()              → ai-service /chat
  │       └── OpenAI GPT-4o-mini + RAG context injected
  ├── ChatHistoryService.saveHistory()    → Redis (TTL 2h)
  └── AuditService.save()                → PostgreSQL
       │
       ▼
ChatResponse { sessionId, answer, tokensUsed }
```

---

## Running Tests

### api-core (Java)

```bash
cd api-core
./mvnw test
```

Test coverage:

| Class | Tests |
|---|---|
| `AuthServiceTest` | 5 — register, duplicate email, login, user not found, wrong password |
| `AuthControllerTest` | 3 — register 201, login 200, invalid body 400 |
| `ChatServiceTest` | 7 — sessionId generation, history, audit, ai-service failure |
| `ChatHistoryServiceTest` | 3 — empty key, existing key, TTL persistence |
| `OrderServiceTest` | 7 — create, price snapshot, cancel, ownership, status update |
| `ProductServiceTest` | 7 — create, SKU collision retry, soft delete, partial update |
| `AuditServiceTest` | 6 — save, nullable fields, findByUser, findBySession, findAll |

### ai-service (Python)

```bash
cd ai-service
pip install -r requirements-dev.txt
pytest tests/ -v
```

Test coverage:

| File | Tests |
|---|---|
| `test_llm_service.py` | 3 — response mapping, parameter validation, OpenAI failure |
| `test_vector_service.py` | 4 — embed, search with results, empty search, store |

---

## Project Structure

```
corporate-ai-assistant/
├── api-core/                  # Java Spring Boot service
│   ├── src/
│   │   ├── main/java/br/com/api_core/
│   │   │   ├── config/        # Security, Redis, WebClient
│   │   │   ├── modules/       # auth, chat, product, order, audit, admin
│   │   │   ├── domain/        # JPA entities, repositories, enums
│   │   │   ├── infra/         # security, converters, exceptions
│   │   │   └── client/        # AiServiceClient (WebClient)
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/  # Flyway migrations V0–V9
│   └── Dockerfile
│
├── ai-service/                # Python FastAPI service
│   ├── app/
│   │   ├── routers/           # chat, embed, search, health
│   │   ├── services/          # llm_service, vector_service
│   │   └── schemas/           # Pydantic request/response models
│   ├── tests/
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   └── Dockerfile
│
├── docs/                      # Architecture and ADR documentation
├── docker-compose.yml
├── .env.example
└── .gitignore
```

---

## Database Migrations

| File | Description |
|---|---|
| `V0__enable_pgvector.sql` | Enables the `vector` extension |
| `V1__create_users.sql` | Users table with role check constraint |
| `V2__create_audit_logs.sql` | Immutable audit log table |
| `V3__create_products.sql` | Product catalog with soft delete |
| `V4__create_orders.sql` | Orders with status and user FK |
| `V5__create_order_items.sql` | Order items with price snapshot |
| `V6__create_documents.sql` | RAG documents with vector embeddings |
| `V7__create_documents_index.sql` | ivfflat index for semantic search |
| `V8__fix_users_role_check.sql` | Updates role constraint to ROLE_ prefix |
| `V9__insert_admin_user.sql` | Seeds the initial admin user |

---

## Environment Variables Reference

| Variable | Used by | Description |
|---|---|---|
| `POSTGRES_DB` | docker-compose | Database name |
| `POSTGRES_USER` | docker-compose | Database user |
| `POSTGRES_PASSWORD` | docker-compose | Database password |
| `SPRING_DATASOURCE_URL` | api-core | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | api-core | Database username |
| `SPRING_DATASOURCE_PASSWORD` | api-core | Database password |
| `SPRING_DATA_REDIS_HOST` | api-core | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | api-core | Redis port |
| `JWT_SECRET` | api-core | Base64 secret for JWT signing |
| `AI_SERVICE_URL` | api-core | Internal URL of the ai-service |
| `OPENAI_API_KEY` | ai-service | OpenAI API key |
| `DATABASE_URL` | ai-service | SQLAlchemy connection URL |

---

## License

MIT