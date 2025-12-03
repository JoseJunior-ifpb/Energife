# 🚀 ENERGIF - Sistema de Gerenciamento de Candidatos

Este repositório contém o código-fonte do sistema ENERGIF, desenvolvido em Spring Boot e utilizando PostgreSQL como banco de dados.

## 📝 Visão Geral das Configurações

O projeto utiliza o arquivo `application.properties` (ou similar) para definir as configurações essenciais de ambiente e banco de dados.

| Configuração | Chave | Valor Padrão |
| :--- | :--- | :--- |
| **Nome da Aplicação** | `spring.application.name` | `energif` |
| **Porta do Servidor** | `server.port` | **`8081`** |
| **Endereço do Servidor** | `server.address` | `0.0.0.0` (Acessível na rede local) |
| **Upload Máximo (Arquivos)** | `spring.servlet.multipart.max-file-size` | `50MB` |
| **Upload Máximo (Requisição)** | `spring.servlet.multipart.max-request-size` | `50MB` |
| **Dialeto JPA** | `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.PostgreSQLDialect` |
| **DDL Hibernate** | `spring.jpa.hibernate.ddl-auto` | `update` (Cria/Atualiza o esquema automaticamente) |

---

## 🛠️ Pré-requisitos

Para rodar a aplicação localmente, você precisa ter o seguinte instalado e configurado:

1.  **Java Development Kit (JDK):** Versão 17 ou superior.
2.  **Gerenciador de Build:** Maven ou Gradle.
3.  **Banco de Dados PostgreSQL:**
    * O servidor deve estar rodando (porta padrão: `5432`).
    * Um banco de dados chamado **`energif`** deve ser criado.

### Configuração do Banco de Dados (PostgreSQL ONLY)

Todos os dados desta aplicação são persistidos exclusivamente em PostgreSQL — não existe fallback para bancos embutidos (H2 foi removido).

Credenciais/configurações padrão utilizadas pelo projeto (são placeholders — preferível usar variáveis de ambiente em produção):

| Parâmetro | Chave | Valor padrão |
| :--- | :--- | :--- |
| **URL de Conexão** | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/energif` |
| **Usuário** | `spring.datasource.username` | `postgres` |
| **Senha** | `spring.datasource.password` | `postgres` |

Use variáveis de ambiente para configurar as credenciais reais durante o deploy/execução (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD).

> ⚠️ Observação: o projeto não inclui mais as SQL migrations no repositório — as migrações e controle de esquema devem ser gerenciados pela equipe/infra (ou reintroduzidas via Flyway/CI se desejado).

---

## ▶️ Como Executar o Projeto

Você pode iniciar o projeto de duas maneiras principais: via IDE ou via JAR executável.

### Opção 1: Via Linha de Comando (Recomendado para Produção)

1.  **Geração do Pacote (JAR):**
    Navegue até o diretório raiz do projeto e use o Maven Wrapper para compilar e empacotar:
    ```bash
    ./mvnw clean package
    ```
2.  **Execução:**
    Execute o arquivo JAR gerado (encontrado no diretório `target/`):
    ```bash
    java -jar target/nome-do-seu-arquivo.jar
    ```

### Opção 2: Via IDE (Para Desenvolvimento)

1.  Abra o projeto na sua IDE (IntelliJ, VS Code, Eclipse, etc.).
2.  Localize a classe principal da aplicação (aquela com a anotação `@SpringBootApplication`, ex: `EnergifApplication.java`).
3.  Execute a classe principal usando a função "Run" da sua IDE.

---

## 🧪 Testes locais

Os testes de integração foram configurados para rodar contra um PostgreSQL real usando **Testcontainers** — isto exige Docker em ambiente local/CI.

Para rodar os testes localmente (requer Docker):

```bash
./mvnw test
```

Se preferir compilar sem executar testes (útil quando Docker não estiver disponível):

```bash
./mvnw -DskipTests package
```

## 🐳 Rodar um PostgreSQL local via Docker Compose

Se você não tem um PostgreSQL local configurado, pode subir um container com as credenciais padrão do projeto (usadas em `application.properties`) executando:

```powershell
# iniciar o banco em background (requer Docker)
docker compose up -d

# parar/remover
docker compose down
```

O `docker-compose.yml` já está incluído no repositório e publica a instância Postgres na porta **5433** do host (evita conflito com um Postgres já instalado na porta 5432). As credenciais padrão configuradas são `postgres`/`postgres` e banco `energif`.

Se você preferir usar a porta padrão 5432 e não tiver um Postgres local em execução, pare o serviço local antes de subir o compose:

```powershell
# pare o serviço PostgreSQL do Windows (exemplo, nome do serviço pode variar)
Stop-Service -Name postgresql-x64-12
docker compose up -d --build
```

## 🌐 Acesso ao Sistema

Após a inicialização bem-sucedida (o servidor deve estar escutando na porta `8081`), o sistema estará acessível nos seguintes endereços:

### 1. Acesso Local (Na Máquina que Executa)
