# 🏛️ Arquitetura em Camadas (Separation of Concerns)

> **Autor:** Marcel Florentino ([@applephoria](https://github.com/applephoria))  
> **Tema:** O Fluxo Unidirecional de Dependências e Desacoplamento

---

## 🏗️ Visão Arquitetural

O **TransactCore-Java** foi construído sob uma arquitetura desacoplada em 5 camadas lógicas estritas, onde o domínio e os contratos são o centro de gravidade do sistema:

```mermaid
flowchart TD
    subgraph APP["Camada de Apresentação / Execução"]
        MAIN["Main.java (Ponto de Entrada / Orquestrador de Testes)"]
    end

    subgraph SERVICE["Camada de Serviço (Service Layer)"]
        TS["TransferenciaService.java (Orquestrador de Transações ACID)"]
    end

    subgraph REPO["Camada de Persistência (Data Access Layer)"]
        CR_INT["ContaRepository (Interface)"]
        CR_JDBC["ContaRepositoryJdbc (Implementação JDBC)"]
        TR_INT["TransferenciaRepository (Interface)"]
        TR_JDBC["TransferenciaRepositoryJdbc (Implementação JDBC)"]
    end

    subgraph MODEL["Camada de Domínio (Business Model)"]
        CONTA["Conta.java (Regras de Saldo, Saque, Depósito)"]
        TRANSF["Transferencia.java (Comprovante Contábil Imutável)"]
    end

    subgraph INFRA["Camada de Infraestrutura (Drivers e Conexão)"]
        DBCONN["DatabaseConnection.java (Fábrica JDBC Socket)"]
        PG["PostgreSQL 18 (SGBD Relacional / Schema financeiro)"]
    end

    MAIN --> TS
    MAIN --> CR_INT

    TS --> CR_INT
    TS --> TR_INT
    TS -.-> DBCONN

    CR_JDBC -.->|Implementa| CR_INT
    TR_JDBC -.->|Implementa| TR_INT

    CR_JDBC --> CONTA
    TR_JDBC --> TRANSF

    CR_JDBC -.-> DBCONN
    TR_JDBC -.-> DBCONN

    DBCONN --> PG
```

---

## 📋 Responsabilidade de Cada Camada

| Camada | Pacote Java | Responsabilidade Primária | O que NÃO faz |
| :--- | :--- | :--- | :--- |
| **Apresentação** | `(default / raiz)` | Inicializar cenários de teste, exibir telemetria no console e chamar serviços. | Não escreve SQL e não muta saldos diretamente. |
| **Serviço** | `com.transactcore.service` | Garantir a atomicidade das transferências, orquestrar `commit` e `rollback`, e controlar o ciclo transacional. | Não conhece detalhes de sintaxe SQL ou queries JDBC. |
| **Repositório** | `com.transactcore.repository` | Executar instruções SQL (`INSERT`, `SELECT`, `UPDATE`), mapear `ResultSet` para entidades e gerenciar `PreparedStatements`. | Não decide regras de negócio sobre quando fazer commit ou rollback. |
| **Domínio** | `com.transactcore.model` | Guardar o estado das entidades com encapsulamento estrito (`private`), executar cálculos contábeis e validar invariantes com Fail-Fast. | Não tem nenhuma dependência de bibliotecas de banco ou SQL (`java.sql`). |
| **Infraestrutura** | `com.transactcore.infra` | Configurar o driver JDBC, credenciais seguras e fornecer sockets de rede ativos para o banco. | Não manipula dados de contas ou regras financeiras. |
