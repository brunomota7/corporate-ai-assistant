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
│   ├── converter/
│   │   ├── FloatArrayToVectorConverter.java  # float[] <-> vector(1536) do pgvector
│   │   └── MapToJsonbConverter.java          # Map<String,Object> <-> jsonb
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

**`FloatArrayToVectorConverter`** — converte `float[]` para o formato string `[0.1,0.2,...]` esperado pelo pgvector e vice-versa. O campo `embedding` é preenchido exclusivamente pelo ai-service após chamada à OpenAI Embeddings API (`text-embedding-3-small`, 1536 dimensões). A api-core apenas recebe e persiste o array.

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
.requestMatchers(GET, "/api/products").hasAnyRole("USER", "ADMIN", "VIEWER")

// Rotas protegidas
.requestMatchers(POST, "/api/chat").hasAnyRole("USER", "ADMIN")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

---

## Tratamento de exceções

### Hierarquia de exceptions

Todas as exceptions de negócio estendem `BusinessException`, localizada em `infra/exception/`. Isso permite que o `GlobalExceptionHandler` capture qualquer exception de negócio em um único método `@ExceptionHandler(BusinessException.class)`, sem precisar adicionar handlers novos a cada exception criada.

```
BusinessException (base — RuntimeException)
├── UserAlreadyExistsException     → HTTP 409 CONFLICT
├── ResourceNotFoundException      → HTTP 404 NOT FOUND
└── UnauthorizedException          → HTTP 401 UNAUTHORIZED
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

---

## Decisões de infraestrutura

### Por que microsserviços separados?

A separação entre api-core e ai-service permite que cada serviço escale de forma independente. Em cenários de alta demanda de IA, o ai-service pode ter múltiplas instâncias sem afetar a api-core. Além disso, isola as dependências Python (LangChain, OpenAI SDK) do ecossistema Java.

### Por que Redis para histórico de chat?

Histórico de conversa é dado temporário, de alta frequência de leitura/escrita e com TTL natural. O PostgreSQL seria um overhead para esse caso de uso. O Redis resolve com latência de microsegundos e TTL automático.

### Por que Flyway ao invés de ddl-auto do Hibernate?

O Hibernate com `ddl-auto: update` ou `create` não é seguro para produção — ele pode alterar ou perder dados. O Flyway garante controle total e rastreável do schema, com histórico de versões e possibilidade de rollback planejado.