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

### Configuração do Banco de Dados

As credenciais configuradas para acesso ao PostgreSQL são:

| Parâmetro | Chave | Valor |
| :--- | :--- | :--- |
| **URL de Conexão** | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/energif` |
| **Usuário** | `spring.datasource.username` | `postgres` |
| **Senha** | `spring.datasource.password` | `ifpb` |

> ⚠️ **Atenção:** Se a sua senha de usuário `postgres` for diferente de `ifpb`, você deve alterar o valor no arquivo de configuração antes de iniciar a aplicação.

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

## 🌐 Acesso ao Sistema

Após a inicialização bem-sucedida (o servidor deve estar escutando na porta `8081`), o sistema estará acessível nos seguintes endereços:

### 1. Acesso Local (Na Máquina que Executa)
