# 📐 Diagrama Estrutural de Classes (UML)

> **Autor:** Marcel Florentino ([@applephoria](https://github.com/applephoria))  
> **Padrão:** Especificação OMG UML 2.5 / Mermaid

---

## 🏛️ Visão Estrutural de Classes e Interfaces

O diagrama abaixo ilustra o desacoplamento de responsabilidades entre o Domínio (`model`), as Interfaces e Implementações de Acesso a Dados (`repository`), a Camada Transacional (`service`) e a Conexão Física (`infra`):

```mermaid
classDiagram
    direction TB

    %% Camada de Infraestrutura
    class DatabaseConnection {
        -String URL$
        -String USER$
        -String PASSWORD$
        +conectar()$ Connection
    }

    %% Camada de Modelo (Domínio)
    class Conta {
        -Long id
        -String titular
        -String cpf
        -BigDecimal saldo
        -LocalDateTime criadoEm
        +Conta(String titular, String cpf)
        +Conta(Long id, String titular, String cpf, BigDecimal saldo, LocalDateTime criadoEm)
        +getId() Long
        +getTitular() String
        +getCpf() String
        +getSaldo() BigDecimal
        +getCriadoEm() LocalDateTime
        +depositar(BigDecimal valor) void
        +sacar(BigDecimal valor) void
    }

    class Transferencia {
        -Long id
        -Long contaOrigem
        -Long contaDestino
        -BigDecimal valor
        -LocalDateTime dataHora
        -String status
        +Transferencia(Long contaOrigem, Long contaDestino, BigDecimal valor)
        +Transferencia(Long id, Long contaOrigem, Long contaDestino, BigDecimal valor, LocalDateTime dataHora, String status)
        +getId() Long
        +getContaOrigem() Long
        +getContaDestino() Long
        +getValor() BigDecimal
        +getDataHora() LocalDateTime
        +getStatus() String
    }

    %% Camada de Repositório (Contratos e Implementações)
    class ContaRepository {
        <<interface>>
        +salvar(Conta conta) void
        +buscarPorId(Long id) Conta
        +buscarPorCpf(String cpf) Conta
        +atualizarSaldo(Conta conta) void
        +atualizarSaldo(Conta conta, Connection conn) void
    }

    class TransferenciaRepository {
        <<interface>>
        +salvar(Transferencia transferencia) void
        +salvar(Transferencia transferencia, Connection conn) void
        +buscarPorId(Long id) Transferencia
    }

    class ContaRepositoryJdbc {
        +salvar(Conta novaConta) void
        +buscarPorCpf(String cpf) Conta
        +buscarPorId(Long id) Conta
        +atualizarSaldo(Conta conta) void
        +atualizarSaldo(Conta conta, Connection conn) void
        -mapearConta(ResultSet rs) Conta
    }

    class TransferenciaRepositoryJdbc {
        +salvar(Transferencia transferencia) void
        +salvar(Transferencia transferencia, Connection conn) void
        +buscarPorId(Long id) Transferencia
        -mapearTransferencia(ResultSet rs) Transferencia
    }

    %% Camada de Serviço
    class TransferenciaService {
        -ContaRepository contaRepository
        -TransferenciaRepository transferenciaRepository
        +TransferenciaService()
        +TransferenciaService(ContaRepository contaRepo, TransferenciaRepository transfRepo)
        +transferir(Long contaOrigemId, Long contaDestinoId, BigDecimal valor) void
    }

    %% Relacionamentos
    ContaRepository <|.. ContaRepositoryJdbc : Implementa
    TransferenciaRepository <|.. TransferenciaRepositoryJdbc : Implementa

    ContaRepositoryJdbc ..> DatabaseConnection : Utiliza para abrir sessao
    TransferenciaRepositoryJdbc ..> DatabaseConnection : Utiliza para abrir sessao

    ContaRepositoryJdbc ..> Conta : Cria e Manipula
    TransferenciaRepositoryJdbc ..> Transferencia : Cria e Manipula

    TransferenciaService --> ContaRepository : Depende de Abstracao
    TransferenciaService --> TransferenciaRepository : Depende de Abstracao
    TransferenciaService ..> DatabaseConnection : Abre transacao ACID
```
