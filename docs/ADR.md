# ADR — Architecture Decision Records

Registro das principais decisões arquiteturais tomadas no projeto Corporate AI Assistant. Cada entrada documenta o contexto, as alternativas consideradas e a justificativa da escolha.

---

## ADR-001 — Separação em dois microsserviços (Java + Python)

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O sistema precisa de uma API robusta com autenticação, segurança e lógica de negócio, além de integração com modelos de linguagem e busca semântica. Essas duas responsabilidades têm ecossistemas tecnológicos distintos.

### Alternativas consideradas

- **Monolito Java puro** — integrar a OpenAI diretamente via HTTP no Spring Boot, sem serviço Python separado.
- **Monolito Python puro** — construir tudo em FastAPI, incluindo a lógica de negócio e segurança.
- **Dois microsserviços** — api-core em Java para negócio/segurança, ai-service em Python para IA.

### Decisão

Dois microsserviços separados.

### Justificativa

O ecossistema Python (LangChain, pgvector, OpenAI SDK) é significativamente mais maduro para IA do que as alternativas Java. Ao mesmo tempo, o ecossistema Java (Spring Security, Spring Data JPA, Flyway) é superior para lógica de negócio, segurança corporativa e persistência. A separação permite que cada serviço evolua, escale e seja testado de forma independente.

---

## ADR-002 — PostgreSQL com pgvector para busca semântica

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O sistema precisa armazenar embeddings vetoriais e realizar buscas por similaridade semântica para o RAG.

### Alternativas consideradas

- **Pinecone** — banco vetorial gerenciado, sem necessidade de infraestrutura própria.
- **Weaviate** — banco vetorial open-source, focado em busca semântica.
- **ChromaDB** — banco vetorial leve, popular em projetos Python.
- **PostgreSQL + pgvector** — extensão que adiciona tipos e índices vetoriais ao PostgreSQL já existente.

### Decisão

PostgreSQL com a extensão pgvector.

### Justificativa

O sistema já utiliza PostgreSQL como banco principal. Adicionar pgvector elimina a necessidade de outro serviço de infraestrutura, reduz complexidade operacional e mantém os dados relacionais e vetoriais no mesmo banco. Para o volume esperado do projeto, a performance do pgvector com índice `ivfflat` é suficiente. Soluções como Pinecone introduziriam custo e dependência de serviço externo desde o início.

---

## ADR-003 — Redis para gerenciamento de contexto de chat

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O chat precisa manter histórico de mensagens por usuário e sessão para que a IA responda com contexto. Esse histórico é temporário e deve expirar automaticamente.

### Alternativas consideradas

- **PostgreSQL** — persistir o histórico em uma tabela `tb_chat_history`.
- **Memória da aplicação** — guardar em Map em memória no processo Java.
- **Redis** — cache com TTL automático e estrutura de dados adequada.

### Decisão

Redis com TTL de 2 horas por sessão.

### Justificativa

Histórico de chat é dado temporário, volátil e com alta frequência de leitura e escrita. Armazenar no PostgreSQL criaria carga desnecessária no banco principal e exigiria limpeza periódica manual. Memória da aplicação não persiste entre restarts e não funciona em múltiplas instâncias. O Redis é projetado exatamente para esse caso de uso, com TTL nativo, latência de microsegundos e estrutura de lista que mapeia naturalmente ao array de mensagens.

---

## ADR-004 — Flyway para controle de schema

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O banco de dados precisa ser versionado e as migrations precisam rodar de forma automática e controlada.

### Alternativas consideradas

- **Hibernate ddl-auto: update** — o Hibernate detecta diferenças e aplica alterações automaticamente.
- **Hibernate ddl-auto: create-drop** — recria o schema a cada restart.
- **Flyway** — ferramenta de migration com versionamento explícito de cada alteração.
- **Liquibase** — alternativa ao Flyway com suporte a XML, YAML e SQL.

### Decisão

Flyway com migrations em SQL puro.

### Justificativa

O `ddl-auto: update` do Hibernate não é seguro para ambientes que não sejam de desenvolvimento — ele pode gerar alterações inesperadas, não remove colunas e não garante idempotência. O Flyway fornece controle explícito, rastreável e auditável de cada mudança no schema. Foi preferido ao Liquibase pela simplicidade de configuração com Spring Boot e pela familiaridade com SQL puro, que torna as migrations mais legíveis e portáveis.

---

## ADR-005 — JWT stateless para autenticação

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O sistema precisa de autenticação que funcione bem em ambiente de microsserviços, sem necessidade de sessão centralizada.

### Alternativas consideradas

- **Sessões HTTP com Spring Session** — sessão armazenada no Redis, stateful.
- **OAuth2 / Keycloak** — servidor de autorização externo com suporte a múltiplos provedores.
- **JWT stateless** — token auto-contido, validado localmente sem consulta ao banco.

### Decisão

JWT stateless com JJWT.

### Justificativa

Para o escopo do projeto, JWT stateless é a solução com menor complexidade operacional. Não requer infraestrutura adicional (como um servidor OAuth2), funciona naturalmente em microsserviços (o token carrega as informações necessárias) e é amplamente compreendido. OAuth2/Keycloak seria mais adequado em um contexto com múltiplos sistemas e SSO, o que está fora do escopo atual.

---

## ADR-006 — WebClient (reativo) ao invés de RestTemplate

**Data:** Abril 2026
**Status:** Aceito

### Contexto

A api-core precisa fazer chamadas HTTP ao ai-service. O Spring oferece múltiplas formas de realizar isso.

### Alternativas consideradas

- **RestTemplate** — cliente HTTP síncrono, legado desde o Spring 5.
- **OpenFeign** — cliente declarativo via interfaces, popular com Spring Cloud.
- **WebClient** — cliente HTTP reativo do Spring WebFlux.

### Decisão

WebClient do Spring WebFlux.

### Justificativa

O `RestTemplate` está em modo de manutenção e não é recomendado para novos projetos. O `WebClient` é a alternativa oficial do Spring, suporta chamadas síncronas e assíncronas, tem suporte nativo a timeout configurável e integra bem com o restante do ecossistema Spring Boot 3.x. O OpenFeign adiciona uma dependência de Spring Cloud que não se justifica para um único cliente HTTP neste projeto.

---

## ADR-007 — OpenAI text-embedding-3-small para embeddings

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O sistema precisa gerar embeddings vetoriais para indexar documentos e realizar buscas semânticas.

### Alternativas consideradas

- **text-embedding-3-large** — modelo OpenAI mais preciso, vetores de 3072 dimensões.
- **text-embedding-3-small** — modelo OpenAI mais eficiente, vetores de 1536 dimensões.
- **text-embedding-ada-002** — modelo legado OpenAI, vetores de 1536 dimensões.
- **Modelos locais (sentence-transformers)** — rodam localmente, sem custo por token.

### Decisão

`text-embedding-3-small` com vetores de 1536 dimensões.

### Justificativa

O `text-embedding-3-small` oferece excelente custo-benefício — performance significativamente superior ao `ada-002` legado a um custo menor, e performance próxima ao `large` com metade das dimensões. 1536 dimensões é suficiente para o volume de documentos esperado e é compatível com o índice `ivfflat` do pgvector. Modelos locais foram descartados para simplificar a infraestrutura inicial.

---

## ADR-008 — Roles prefixadas com ROLE_ no banco

**Data:** Abril 2026
**Status:** Aceito

### Contexto

O Spring Security espera que roles usadas com `hasRole()` sejam prefixadas internamente com `ROLE_`. A questão é onde aplicar esse prefixo — no código ou no banco.

### Alternativas consideradas

- **Armazenar sem prefixo no banco** (`ADMIN`, `USER`) e adicionar o prefixo no código.
- **Armazenar com prefixo no banco** (`ROLE_ADMIN`, `ROLE_USER`) e usar diretamente.

### Decisão

Armazenar com prefixo `ROLE_` diretamente no banco.

### Justificativa

Armazenar com o prefixo elimina a necessidade de transformação no código ao carregar o `UserDetails`. O valor do banco é usado diretamente como `GrantedAuthority`, tornando o fluxo mais simples e menos propenso a erros. A `CHECK constraint` no banco garante que apenas valores válidos (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_VIEWER`) são persistidos.