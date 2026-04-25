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
│   │   └── dto/
│   │       ├── AuthRegisterDTO.java
│   │       ├── AuthLoginDTO.java
│   │       └── AuthResponseDTO.java
│   │
│   ├── chat/
│   │   ├── ChatController.java      # POST /api/chat — extrai User + IP, delega ao service
│   │   ├── ChatService.java         # Orquestra histórico + RAG + LLM + auditoria
│   │   ├── ChatHistoryService.java  # Leitura e escrita no Redis (TTL 2h)
│   │   └── dto/
│   │       ├── ChatRequestDTO.java  # message (@NotBlank) + sessionId (opcional)
│   │       ├── ChatResponseDTO.java # sessionId + answer + tokensUsed
│   │       ├── MessageDTO.java      # role (MessageRole) + content
│   │       └── MessageRole.java     # enum USER / ASSISTANT com @JsonProperty
│   │
│   ├── product/
│   │   ├── ProductController.java   # GET, POST /api/products | GET, PUT, DELETE /api/products/{id}
│   │   ├── ProductService.java
│   │   └── dto/
│   │       ├── ProductCreateDTO.java
│   │       ├── ProductUpdateDTO.java
│   │       └── ProductResponseDTO.java
│   │
│   ├── order/
│   │   ├── OrderController.java        # Endpoints do usuário — /api/orders
│   │   ├── AdminOrderController.java   # Endpoints admin — /api/admin/orders
│   │   ├── OrderService.java
│   │   └── dto/
│   │       ├── OrderCreateDTO.java
│   │       ├── OrderItemCreateDTO.java
│   │       ├── OrderStatusUpdateDTO.java
│   │       ├── OrderResponseDTO.java
│   │       └── OrderItemResponseDTO.java
│   │
│   ├── audit/
│   │   ├── AuditController.java      # GET /api/audit — consultas de auditoria
│   │   ├── AdminAuditController.java  # GET /api/admin/audit — exclusivo ADMIN
│   │   ├── AuditService.java         # Persiste log após cada interação do chat
│   │   └── dto/
│   │       └── AuditLogResponseDTO.java
│   │
│   └── admin/
│       ├── AdminController.java      # POST /api/admin/ingest
│       ├── AdminService.java         # Gera embedding via AiServiceClient e persiste em tb_documents
│       └── dto/
│           └── IngestRequestDTO.java
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
├── domain/repository/
│   ├── UserRepository.java
│   ├── AuditLogRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── DocumentRepository.java
│
├── infra/
│   ├── security/
│   │   ├── JwtService.java              # Geração e validação de tokens JWT
│   │   ├── JwtFilter.java               # OncePerRequestFilter — extrai e valida token
│   │   ├── UserDetailsServiceImpl.java  # Carrega usuário do banco para o Spring Security
│   │   └── SecurityUtils.java           # Extrai userId do Authentication para os controllers
│   ├── converter/
│   │   ├── FloatArrayToVectorConverter.java  # float[] <-> vector(1536) do pgvector
│   │   └── MapToJsonbConverter.java          # Map<String,Object> <-> jsonb
│   └── exception/
│       ├── GlobalExceptionHandler.java       # @ControllerAdvice — trata exceções globais
│       ├── ErrorResponse.java                # Record padrão de resposta de erro
│       ├── BusinessException.java            # Base para todas as exceptions de negócio
│       ├── UserAlreadyExistsException.java   # HTTP 409
│       ├── ResourceNotFoundException.java    # HTTP 404
│       ├── UnauthorizedException.java        # HTTP 401
│       ├── ProductNotFoundException.java     # HTTP 404
│       ├── OrderNotFoundException.java       # HTTP 404
│       ├── OrderCancellationNotAllowedException.java  # HTTP 422
│       └── AiServiceUnavailableException.java       # HTTP 503
│
└── client/
    └── AiServiceClient.java      # WebClient — métodos: search(), chat(), embed()
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
  ├── ChatHistoryService.saveHistory(userId, sessionId, messages) → Redis
  └── AuditService.save(user, sessionId, question, answer, tokensUsed, latencyMs, ip)  → PostgreSQL
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
| GET | `/api/products` | autenticado | Listar produtos ativos |
| GET | `/api/products/{id}` | autenticado | Buscar produto por ID |
| POST | `/api/products` | ADMIN | Criar produto |
| PUT | `/api/products/{id}` | ADMIN | Atualizar produto |
| DELETE | `/api/products/{id}` | ADMIN | Soft delete de produto |
| GET | `/api/orders` | USER, ADMIN | Listar pedidos do usuário autenticado |
| GET | `/api/orders/{id}` | USER, ADMIN | Buscar pedido por ID |
| POST | `/api/orders` | USER, ADMIN | Criar pedido |
| DELETE | `/api/orders/{id}` | USER, ADMIN | Cancelar pedido (só PENDING) |
| GET | `/api/admin/orders` | ADMIN | Listar todos os pedidos |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN | Atualizar status do pedido |
| GET | `/api/audit` | USER, ADMIN | Listar logs do usuário autenticado |
| GET | `/api/audit/session/{sessionId}` | USER, ADMIN | Listar logs de uma sessão |
| GET | `/api/admin/audit` | ADMIN | Listar todos os logs do sistema |
| POST | `/api/admin/ingest` | ADMIN | Indexar documento no RAG |

---

## ai-service (Python / FastAPI)

Microsserviço de inteligência. Encapsula toda a comunicação com a OpenAI e a lógica de busca semântica. Não possui autenticação própria — é um serviço interno, acessado apenas pela api-core dentro da rede Docker.

### Estrutura de pacotes

```
app/
├── main.py                    # Entrypoint FastAPI, registro de routers
├── config.py                  # Leitura de variáveis de ambiente (pydantic-settings)
├── database.py                # Engine SQLAlchemy + SessionLocal + get_db()
│
├── routers/
│   ├── chat.py                # POST /chat
│   ├── embed.py               # POST /embed
│   ├── search.py              # POST /search
│   └── health.py              # GET /health
│
├── services/
│   ├── llm_service.py         # Integração com OpenAI (chat completions)
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
llm_service.chat(messages)             → OpenAI GPT (gpt-4o-mini)
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

## Entidades — observações e detalhes importantes

### Geração de UUID via `@PrePersist`

Todas as entidades geram o próprio `id` via `UUID.randomUUID()` no `@PrePersist`, ao invés de usar `@GeneratedValue`. Isso é intencional — permite que o `id` seja conhecido antes do `flush` para o banco, o que facilita logs, rastreabilidade e testes. O banco também define `DEFAULT gen_random_uuid()` na migration como fallback, mas na prática quem gera é sempre o Java.

### Timestamps via `@PrePersist` e `@PreUpdate`

`created_at` e `updated_at` são gerenciados pelos callbacks JPA, não pelo banco. O banco define `DEFAULT now()` nas migrations como fallback para inserções diretas via SQL (scripts, seeds), mas em operações via JPA o Java sempre controla os valores.

### `AuditLog` — tabela append-only

Não possui `updated_at` e nenhum setter deve ser usado para alterar registros existentes. Todo registro é imutável após a inserção. O `AuditService` apenas chama `save()`, nunca `update()`.

### `OrderItem` — snapshot de preço

O campo `unit_price` armazena o preço do produto **no momento da criação do pedido**, independentemente de alterações futuras no `tb_products`. Isso garante integridade histórica dos pedidos. O valor deve ser copiado de `Product.price` no momento da criação do `OrderItem`, nunca referenciado dinamicamente.

### `Order` — cascade e orphanRemoval

O relacionamento `@OneToMany` com `OrderItem` usa `cascade = CascadeType.ALL` e `orphanRemoval = true`. Isso significa que ao salvar um `Order` com itens, os itens são salvos automaticamente. Ao remover um item da lista e salvar o `Order`, o item é deletado do banco. Nunca delete um `OrderItem` diretamente pelo repositório — remova da lista do `Order`.

### `Document` — converters para tipos nativos do PostgreSQL

O Hibernate não possui mapeamento nativo para `vector` (pgvector) nem `jsonb`. Dois `AttributeConverter` foram criados em `infra/converter/` para resolver isso:

**`FloatArrayToVectorConverter`** — converte `float[]` para o formato string `[0.1,0.2,...]` esperado pelo pgvector e vice-versa. O campo `embedding` é preenchido exclusivamente pelo ai-service após chamada à OpenAI Embeddings API (`text-embedding-3-small`, 1536 dimensões). O campo é preenchido pelo ai-service via VectorService.store() durante o ingest — a api-core não manipula embeddings diretamente.

**`MapToJsonbConverter`** — converte `Map<String, Object>` para JSON string usando Jackson, que o PostgreSQL armazena como `jsonb`. O campo `metadata` é livre e pode conter qualquer estrutura, por exemplo:
```json
{ "productId": "uuid-aqui", "category": "electronics", "language": "pt-BR" }
```
Isso permite filtros futuros por metadados no pgvector sem alterar o schema do banco.

O `ObjectMapper` nos converters é `static` — `ObjectMapper` é thread-safe e instanciá-lo a cada conversão seria um desperdício de recursos.

### `Document.source_id` — referência sem FK

O campo `source_id` é um `UUID` simples sem `@ManyToOne` nem FK no banco. Isso é intencional — um documento pode ser originado de um produto, pedido, FAQ ou qualquer fonte futura. Manter uma FK forçaria a escolha de uma única tabela de origem. O campo `source_type` (`PRODUCT`, `ORDER`, `MANUAL`, `FAQ`) indica qual tabela consultar se precisar recuperar o registro de origem.

### `Role` — prefixo `ROLE_`

Os valores do enum são armazenados com o prefixo `ROLE_` diretamente no banco (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_VIEWER`). Isso elimina transformações no código ao carregar o `UserDetails` — o valor do banco é usado diretamente como `GrantedAuthority`. O `@Enumerated(EnumType.STRING)` garante que o Hibernate persiste o nome do enum, não o ordinal.

### `OrderStatus` — sem prefixo

Ao contrário de `Role`, `OrderStatus` não usa prefixo. O Spring Security não interfere nesse enum — ele é usado apenas para lógica de negócio, sem envolvimento com `hasRole()` ou `GrantedAuthority`.

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

// Produtos — leitura para qualquer autenticado, escrita só ADMIN
.requestMatchers(GET, "/api/products", "/api/products/**").authenticated()
.requestMatchers(POST, "/api/products").hasRole("ADMIN")
.requestMatchers(PUT, "/api/products/**").hasRole("ADMIN")
.requestMatchers(DELETE, "/api/products/**").hasRole("ADMIN")

// Chat
.requestMatchers(POST, "/api/chat").hasAnyRole("USER", "ADMIN")

// Admin — todos os endpoints sob /api/admin
.requestMatchers("/api/admin/**").hasRole("ADMIN")

.anyRequest().authenticated()
```

---

## Tratamento de exceções

### Hierarquia de exceptions

Todas as exceptions de negócio estendem `BusinessException`, localizada em `infra/exception/`. Isso permite que o `GlobalExceptionHandler` capture qualquer exception de negócio em um único método `@ExceptionHandler(BusinessException.class)`, sem precisar adicionar handlers novos a cada exception criada.

```
BusinessException (base — RuntimeException)
├── UserAlreadyExistsException              → HTTP 409 CONFLICT
├── ResourceNotFoundException               → HTTP 404 NOT FOUND
├── UnauthorizedException                   → HTTP 401 UNAUTHORIZED
├── ProductNotFoundException                → HTTP 404 NOT FOUND
├── OrderNotFoundException                  → HTTP 404 NOT FOUND
├── OrderCancellationNotAllowedException    → HTTP 422 UNPROCESSABLE ENTITY
└── AiServiceUnavailableException           → HTTP 503 SERVICE UNAVAILABLE
```

Novas exceptions de negócio nos módulos futuros (product, order) seguem o mesmo padrão — estendem `BusinessException` com o status HTTP adequado no construtor.

### Response padrão de erro

Toda resposta de erro retorna o record `ErrorResponse` com o seguinte formato:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with email: user@email.com",
  "path": "/api/auth/register",
  "timestamp": "2026-04-17T10:30:00"
}
```

### Handlers registrados no `GlobalExceptionHandler`

| Handler | Captura | Status retornado |
|---|---|---|
| `handleBusinessException` | `BusinessException` e todas as filhas | Definido na exception |
| `handleValidationException` | `MethodArgumentNotValidException` (falha no `@Valid`) | 400 BAD REQUEST |
| `handleBadCredentials` | `BadCredentialsException` (login inválido) | 401 UNAUTHORIZED |
| `handleGenericException` | `Exception` (fallback geral) | 500 INTERNAL SERVER ERROR |

O fallback genérico retorna sempre `"An unexpected error occurred"` — nunca expõe `ex.getMessage()` para não vazar detalhes de infraestrutura.

---

## Estratégia de testes

### Testes unitários de service (`@ExtendWith(MockitoExtension.class)`)

Usados para testar a lógica de negócio isoladamente. Todas as dependências são mockadas com `@Mock` e injetadas com `@InjectMocks`. Não sobe contexto Spring — execução rápida.

Coberturas obrigatórias por service:
- Caminho feliz (operação bem-sucedida)
- Exception de negócio esperada (ex: `UserAlreadyExistsException`)
- Casos de borda relevantes (ex: senha inválida, recurso não encontrado)

### Testes de controller (`@WebMvcTest`)

Usados para testar a camada HTTP: mapeamento de rotas, serialização/desserialização JSON, validação de DTOs e status HTTP. O service é mockado com `@MockitoBean`.

**Configuração padrão para todos os `@WebMvcTest` do projeto:**

```java
@WebMvcTest(XyzController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class XyzControllerTest { ... }
```

O `@AutoConfigureMockMvc(addFilters = false)` desativa todos os filtros de servlet (incluindo o `JwtFilter`) no contexto de teste. O `@Import(TestSecurityConfig.class)` importa uma config de segurança permissiva que libera todas as rotas e desabilita CSRF.

O `TestSecurityConfig` fica em `src/test/java/br/com/api_core/support/` e é reutilizado por todos os controller tests via `@Import`.

Coberturas obrigatórias por controller:
- Requisição válida retorna status e body esperados
- Requisição com body inválido retorna 400 (cobre o `@Valid`)
- Para rotas protegidas: requisição sem token retorna 401 (testado em integração)

### Localização dos testes

```
src/test/java/br/com/api_core/
├── support/
│   └── TestSecurityConfig.java          # Config de segurança permissiva — compartilhada
└── modules/
    ├── auth/
    │   ├── AuthServiceTest.java
    │   └── AuthControllerTest.java
    ├── product/
    │   ├── ProductServiceTest.java
    │   └── ProductControllerTest.java
    ├── order/
    │   ├── OrderServiceTest.java
    │   └── OrderControllerTest.java
    ├── audit/
    │   ├── AuditServiceTest.java
    │   └── AuditControllerTest.java
    ├── chat/
    │   ├── ChatServiceTest.java         # 7 testes — sessionId, histórico, auditoria, falhas
    │   ├── ChatControllerTest.java
    │   └── ChatHistoryServiceTest.java  # 3 testes — chave inexistente, retorno com dados, TTL
    └── admin/
        └── AdminControllerTest.java
```

---

## Decisões de infraestrutura

### Por que microsserviços separados?

A separação entre api-core e ai-service permite que cada serviço escale de forma independente. Em cenários de alta demanda de IA, o ai-service pode ter múltiplas instâncias sem afetar a api-core. Além disso, isola as dependências Python (LangChain, OpenAI SDK) do ecossistema Java.

### Por que Redis para histórico de chat?

Histórico de conversa é dado temporário, de alta frequência de leitura/escrita e com TTL natural. O PostgreSQL seria um overhead para esse caso de uso. O Redis resolve com latência de microsegundos e TTL automático.

### Por que Flyway ao invés de ddl-auto do Hibernate?

O Hibernate com `ddl-auto: update` ou `create` não é seguro para produção — ele pode alterar ou perder dados. O Flyway garante controle total e rastreável do schema, com histórico de versões e possibilidade de rollback planejado.