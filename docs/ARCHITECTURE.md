# Arquitetura — Corporate AI Assistant

## Visão geral

O sistema é composto por dois microsserviços com responsabilidades bem definidas, suportados por dois serviços de infraestrutura.

```
┌──────────────────────────────────────────────────────────────────┐
│                        Rede Docker interna                       │
│                                                                  │
│   ┌─────────────┐    HTTP     ┌──────────────┐                  │
│   │  api-core   │ ──────────► │  ai-service  │                  │
│   │  :8080      │             │  :8001       │                  │
│   └──────┬──────┘             └──────┬───────┘                  │
│          │                           │                           │
│          │ JPA/JDBC                  │ SQLAlchemy               │
│          ▼                           ▼                           │
│   ┌─────────────────────────────────────┐                       │
│   │         PostgreSQL :5432            │                       │
│   │         + pgvector extension        │                       │
│   └─────────────────────────────────────┘                       │
│          │                                                       │
│          │ Spring Data Redis                                     │
│          ▼                                                       │
│   ┌─────────────┐                                               │
│   │  Redis :6379│                                               │
│   └─────────────┘                                               │
└──────────────────────────────────────────────────────────────────┘
```

---

## api-core (Java / Spring Boot)

Responsável por toda a lógica de negócio, segurança, persistência e orquestração do fluxo de chat.

### Estrutura de pacotes

```
br.com.api_core/
├── config/
│   ├── SecurityConfig.java       # Configuração do Spring Security e filtros
│   ├── RedisConfig.java          # Configuração do RedisTemplate
│   └── WebClientConfig.java      # Configuração do WebClient (timeout, base URL)
│
├── modules/
│   ├── auth/
│   │   ├── AuthController.java   # POST /api/auth/register, POST /api/auth/login
│   │   ├── AuthService.java      # Lógica de registro e autenticação
│   │   └── dto/                  # LoginRequest, RegisterRequest, TokenResponse
│   │
│   ├── chat/
│   │   ├── ChatController.java   # POST /api/chat
│   │   ├── ChatService.java      # Orquestra histórico + chamada ao ai-service
│   │   ├── ChatHistoryService.java  # Leitura e escrita no Redis
│   │   └── dto/                  # ChatRequest, ChatResponse, MessageDTO
│   │
│   ├── product/
│   │   ├── ProductController.java  # CRUD /api/products
│   │   ├── ProductService.java
│   │   └── dto/
│   │
│   ├── order/
│   │   ├── OrderController.java  # CRUD /api/orders
│   │   ├── OrderService.java
│   │   └── dto/
│   │
│   └── audit/
│       ├── AuditService.java     # Persiste log após cada interação
│       └── dto/
│
├── domain/
│   ├── User.java                 # Entidade mapeada para tb_users
│   ├── AuditLog.java             # Entidade mapeada para tb_audit_logs
│   ├── Product.java              # Entidade mapeada para tb_products
│   ├── Order.java                # Entidade mapeada para tb_orders
│   ├── OrderItem.java            # Entidade mapeada para tb_order_items
│   ├── Document.java             # Entidade mapeada para tb_documents
│   └── enums/
│       ├── Role.java             # ROLE_ADMIN, ROLE_USER, ROLE_VIEWER
│       └── OrderStatus.java      # PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
│
├── infra/
│   ├── security/
│   │   ├── JwtService.java       # Geração e validação de tokens JWT
│   │   └── JwtFilter.java        # OncePerRequestFilter — extrai e valida token
│   └── exception/
│       └── GlobalExceptionHandler.java  # @ControllerAdvice — trata exceções globais
│
└── client/
    └── AiServiceClient.java      # WebClient — chama /chat, /embed, /search no ai-service
```

### Fluxo de uma requisição de chat

```
POST /api/chat
      │
      ▼
JwtFilter (valida token, injeta autenticação no contexto)
      │
      ▼
ChatController (recebe ChatRequest, extrai userId do token)
      │
      ▼
ChatService
  ├── ChatHistoryService.getHistory(userId, sessionId)  → Redis
  ├── AiServiceClient.search(userMessage)               → ai-service /search
  │       └── retorna chunks relevantes do pgvector
  ├── AiServiceClient.chat(messages + context)          → ai-service /chat
  │       └── retorna resposta do LLM
  ├── ChatHistoryService.saveMessage(...)               → Redis
  └── AuditService.save(userId, question, answer, ...)  → PostgreSQL
      │
      ▼
ChatResponse → cliente
```

### Endpoints principais

| Método | Endpoint | Role | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | público | Cadastro de usuário |
| POST | `/api/auth/login` | público | Login e geração de JWT |
| POST | `/api/chat` | USER, ADMIN | Enviar mensagem ao assistente |
| GET | `/api/products` | USER, ADMIN, VIEWER | Listar produtos |
| POST | `/api/products` | ADMIN | Criar produto |
| PUT | `/api/products/{id}` | ADMIN | Atualizar produto |
| DELETE | `/api/products/{id}` | ADMIN | Remover produto |
| GET | `/api/orders` | USER, ADMIN | Listar pedidos |
| POST | `/api/orders` | USER, ADMIN | Criar pedido |
| POST | `/api/admin/ingest` | ADMIN | Indexar documento no RAG |

---

## ai-service (Python / FastAPI)

Microsserviço de inteligência. Encapsula toda a comunicação com a OpenAI e a lógica de busca semântica. Não possui autenticação própria — é um serviço interno, acessado apenas pela api-core dentro da rede Docker.

### Estrutura de pacotes

```
app/
├── main.py                    # Entrypoint FastAPI, registro de routers
├── config.py                  # Leitura de variáveis de ambiente (pydantic-settings)
│
├── routers/
│   ├── chat.py                # POST /chat
│   ├── embed.py               # POST /embed
│   ├── search.py              # POST /search
│   └── health.py              # GET /health
│
├── services/
│   ├── llm_service.py         # Integração com ChatOpenAI via LangChain
│   └── vector_service.py      # Geração de embeddings e busca no pgvector
│
└── schemas/
    ├── chat_schema.py          # ChatRequest, ChatResponse, MessageSchema
    └── embed_schema.py         # EmbedRequest, EmbedResponse, SearchRequest
```

### Endpoints

| Método | Endpoint | Payload | Descrição |
|---|---|---|---|
| POST | `/chat` | `{ messages[], context[] }` | Gera resposta do LLM com histórico |
| POST | `/embed` | `{ text, source_type, source_id }` | Gera e persiste embedding |
| POST | `/search` | `{ query, top_k }` | Busca semântica no pgvector |
| GET | `/health` | — | Health check para o Docker |

### Fluxo do RAG

```
POST /search  (query do usuário)
      │
      ▼
vector_service.embed(query)        → OpenAI text-embedding-3-small
      │
      ▼
pgvector SELECT ... ORDER BY       → top K chunks por distância cosseno
embedding <=> query_vector LIMIT K
      │
      ▼
retorna chunks para api-core

api-core injeta chunks como contexto no prompt
      │
      ▼
POST /chat  (messages + contexto injetado)
      │
      ▼
LangChain ChatOpenAI.invoke(messages)  → OpenAI GPT
      │
      ▼
retorna resposta para api-core
```

---

## Banco de dados

### Diagrama de tabelas

```
tb_users
├── id (PK)
├── name, email, password
├── role, active
└── created_at, updated_at
     │
     ├──────────────────────────┐
     ▼                          ▼
tb_audit_logs              tb_orders
├── id (PK)                ├── id (PK)
├── user_id (FK)           ├── user_id (FK)
├── session_id             ├── status, total_amount
├── question, answer       ├── notes
├── tokens_used            └── created_at, updated_at
├── latency_ms                  │
├── ip_address                  ▼
└── created_at             tb_order_items
                           ├── id (PK)
                           ├── order_id (FK)
                           ├── product_id (FK)
                           ├── quantity
                           └── unit_price

tb_products
├── id (PK)
├── sku (UNIQUE), name
├── description, price
├── stock_quantity, category
├── active
└── created_at, updated_at

tb_documents  (RAG)
├── id (PK)
├── title, content
├── source_type, source_id
├── embedding (VECTOR 1536)
├── metadata (JSONB)
└── created_at
```

### Migrations (Flyway)

| Arquivo | Descrição |
|---|---|
| `V0__enable_pgvector.sql` | Habilita a extensão `vector` no PostgreSQL |
| `V1__create_users.sql` | Tabela de usuários com roles |
| `V2__create_audit_logs.sql` | Log imutável de interações |
| `V3__create_products.sql` | Catálogo de produtos |
| `V4__create_orders.sql` | Pedidos com status e FK para usuários |
| `V5__create_order_items.sql` | Itens de pedido com snapshot de preço |
| `V6__create_documents.sql` | Documentos para RAG com embeddings |
| `V7__create_documents_index.sql` | Índice ivfflat para busca semântica |

---

## Redis — gerenciamento de contexto

O histórico de cada conversa é armazenado no Redis com TTL de 2 horas.

```
Chave:  chat:{userId}:{sessionId}
Valor:  JSON array de mensagens
        [
          { "role": "user",      "content": "Qual o estoque do SKU-001?" },
          { "role": "assistant", "content": "O produto possui 42 unidades." },
          ...
        ]
TTL:    7200 segundos (2 horas)
```

A cada nova mensagem a api-core recupera o histórico, adiciona a nova mensagem, envia ao ai-service e persiste o histórico atualizado de volta no Redis.

---

## Segurança

### Autenticação JWT

```
POST /api/auth/login
      │
      ▼
AuthService valida email + senha (BCrypt)
      │
      ▼
JwtService.generateToken(userId, role)
      │
      ▼
Token retornado ao cliente

--- próximas requisições ---

Authorization: Bearer <token>
      │
      ▼
JwtFilter extrai e valida token
      │
      ▼
SecurityContextHolder recebe autenticação
      │
      ▼
Controller acessa userId via Principal
```

### Configuração de segurança por endpoint

```java
// Rotas públicas
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(GET, "/api/products").hasAnyRole("USER", "ADMIN", "VIEWER")

// Rotas protegidas
.requestMatchers(POST, "/api/chat").hasAnyRole("USER", "ADMIN")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

---

## Decisões de infraestrutura

### Por que microsserviços separados?

A separação entre api-core e ai-service permite que cada serviço escale de forma independente. Em cenários de alta demanda de IA, o ai-service pode ter múltiplas instâncias sem afetar a api-core. Além disso, isola as dependências Python (LangChain, OpenAI SDK) do ecossistema Java.

### Por que Redis para histórico de chat?

Histórico de conversa é dado temporário, de alta frequência de leitura/escrita e com TTL natural. O PostgreSQL seria um overhead para esse caso de uso. O Redis resolve com latência de microsegundos e TTL automático.

### Por que Flyway ao invés de ddl-auto do Hibernate?

O Hibernate com `ddl-auto: update` ou `create` não é seguro para produção — ele pode alterar ou perder dados. O Flyway garante controle total e rastreável do schema, com histórico de versões e possibilidade de rollback planejado.