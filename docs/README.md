# Corporate AI Assistant

## Propósito

O **Corporate AI Assistant** é um sistema de inteligência artificial aplicada a ambientes corporativos. O objetivo é fornecer uma camada de IA conversacional integrada a dados reais de negócio — respondendo perguntas sobre estoque, pedidos e catálogo de produtos com base em informações reais do sistema, não apenas em conhecimento genérico do modelo.

O projeto foi concebido com arquitetura de microsserviços, separando claramente a responsabilidade da lógica de negócio (API Java) da inteligência artificial (serviço Python), permitindo que cada parte evolua de forma independente.

---

## Problema que resolve

Sistemas corporativos do tipo ERP acumulam grande volume de dados operacionais, mas o acesso a essa informação geralmente exige navegar por telas, relatórios e consultas manuais. O Corporate AI Assistant permite que usuários façam perguntas em linguagem natural como:

- _"Qual o estoque atual do produto SKU-001?"_
- _"Quais pedidos estão com status PENDING há mais de 3 dias?"_
- _"Me dê um resumo dos produtos com estoque abaixo de 10 unidades."_

A IA responde com base nos dados reais do sistema, utilizando a técnica de **RAG (Retrieval-Augmented Generation)** para buscar as informações mais relevantes antes de gerar a resposta.

---

## Stack tecnológica

### api-core — Java
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem base |
| Spring Boot | 3.5.x | Framework principal |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Acesso ao banco de dados |
| Spring WebFlux | 3.x | WebClient para chamadas ao ai-service |
| Spring Data Redis | 3.x | Cache de contexto de conversas |
| Flyway | 11.x | Controle de migrations |
| PostgreSQL Driver | 42.x | Conexão com banco |
| JJWT | 0.12.x | Geração e validação de tokens JWT |

### ai-service — Python
| Tecnologia | Versão | Uso |
|---|---|---|
| Python | 3.11+ | Linguagem base |
| FastAPI | 0.110+ | Framework de API |
| LangChain | 0.2+ | Orquestração de LLM |
| OpenAI SDK | 1.x | Integração com GPT e embeddings |
| pgvector | 0.2+ | Busca semântica no PostgreSQL |
| SQLAlchemy | 2.x | Acesso ao banco via ORM |
| Pydantic | 2.x | Validação de schemas |

### Infraestrutura
| Tecnologia | Uso |
|---|---|
| PostgreSQL 16 + pgvector | Banco principal e busca vetorial |
| Redis 7.2 | Cache de sessões e histórico de chat |
| Docker + Docker Compose | Orquestração local |

---

## Módulos do sistema

### api-core

- **auth** — cadastro, login, geração de JWT e controle de sessão
- **chat** — recebe mensagens, orquestra contexto e delega ao ai-service
- **product** — CRUD de produtos com controle de estoque
- **order** — CRUD de pedidos e itens de pedido
- **audit** — registro imutável de todas as interações com a IA
- **infra/security** — filtro JWT, configuração do Spring Security
- **infra/exception** — tratamento global de erros
- **client** — WebClient configurado para comunicação com o ai-service

### ai-service

- **chat** — endpoint principal, recebe histórico e retorna resposta do LLM
- **embed** — gera embedding vetorial de um texto
- **search** — busca semântica nos documentos indexados via pgvector

---

## Segurança

O sistema implementa controle de acesso baseado em roles:

| Role | Permissões |
|---|---|
| `ROLE_ADMIN` | Acesso total, incluindo ingestão de documentos e auditoria |
| `ROLE_USER` | Acesso ao chat e consulta de produtos e pedidos próprios |
| `ROLE_VIEWER` | Somente leitura, sem acesso ao chat |

Toda requisição autenticada é rastreada na tabela `tb_audit_logs`.

---

## Estrutura do repositório

```
corporate-ai-assistant/
├── ai-service/          # Python FastAPI — microsserviço de IA
├── api-core/            # Java Spring Boot — API principal
├── docs/                # Documentação técnica
│   ├── architecture.md  # Arquitetura detalhada
│   └── adr.md           # Registro de decisões arquiteturais
├── docker-compose.yml   # Orquestração local
├── .env.example         # Variáveis de ambiente necessárias
└── README.md            # Este arquivo
```

---

## Como rodar localmente

### Pré-requisitos

- Docker e Docker Compose instalados
- Java 21
- Python 3.11+
- Maven 3.9+

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/corporate-ai-assistant.git
cd corporate-ai-assistant

# 2. Configure as variáveis de ambiente
cp .env.example .env
# Edite o .env com sua OPENAI_API_KEY e JWT_SECRET

# 3. Suba a infraestrutura
docker-compose up postgres redis -d

# 4. Rode a API Java (em outro terminal)
cd api-core
./mvnw spring-boot:run

# 5. Rode o AI Service (em outro terminal)
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

A API estará disponível em `http://localhost:8080`
A documentação Swagger em `http://localhost:8080/swagger-ui.html`
O AI Service em `http://localhost:8001/docs`

---

## Variáveis de ambiente

| Variável | Descrição | Exemplo |
|---|---|---|
| `OPENAI_API_KEY` | Chave da API OpenAI | `sk-...` |
| `JWT_SECRET` | Segredo para assinatura do JWT (mín. 32 chars) | `minha-chave-secreta` |
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5433/corporate_ai` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `postgres` |
| `SPRING_DATA_REDIS_HOST` | Host do Redis | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Porta do Redis | `6379` |
| `AI_SERVICE_URL` | URL base do ai-service | `http://localhost:8001` |