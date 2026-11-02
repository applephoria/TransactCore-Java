# 📖 Dossiê de Engenharia: Aprendizados & Arquitetura do TransactCore-Java

> **Autor:** Marcel Florentino ([@applephoria](https://github.com/applephoria))  
> **Linguagem & Plataforma:** Java 25 LTS (OpenJDK)  
> **Banco de Dados:** PostgreSQL 18  
> **Protocolo de Persistência:** Raw JDBC (`java.sql`)  

---

## 🏛️ 1. O Manifesto: Por que Construir um Core Bancário em Java Puro?

A grande maioria dos desenvolvedores modernos inicia seus estudos de persistência em Java através de abstrações de altíssimo nível, como Spring Boot, Hibernate e Spring Data JPA. Embora esses frameworks sejam poderosos para o mercado de aplicações rápidas, eles criam uma **cortina de fumaça cognitiva**: o desenvolvedor passa a acreditar que o banco de dados é uma "coleção mágica em memória" e perde completamente a sensibilidade sobre o que acontece na placa de rede, no socket TCP e nos setores do disco rígido.

Como engenheiro de software com paixão pela física da computação, este projeto nasceu com um propósito intransigente: **descer às fundações da engenharia**. 

O **TransactCore-Java** foi construído sem nenhum framework mágico. Apenas o compilador Java 25, o driver oficial do PostgreSQL e a biblioteca nativa `java.sql`. Ao remover os intermediários, cada byte trafegado, cada transação ACID e cada alocação de memória torna-se visível, auditável e determinística.

---

## 🧠 2. A Física da Memória RAM: Stack vs Heap

Compreender o tráfego de dados exige entender a separação física de memória gerenciada pela JVM (Java Virtual Machine):

```
       MEMÓRIA STACK (Rápida / Por Thread)              MEMÓRIA HEAP (Dinâmica / Global)
    ┌────────────────────────────────────────┐       ┌─────────────────────────────────────┐
    │ main() Frame                           │       │                                     │
    │  ├─ idContaOrigem = 1L (Primitivo)     │       │  Objeto Conta (Marcel)              │
    │  ├─ contaMarcelAntes ──────────[Ref: 0x4A1F]───┼──> [id=1, titular="Marcel", ...]    │
    │  ├─ valorPix = [Ref: 0x8C3B]───────────┼───────┼──> Objeto BigDecimal ("100.00")     │
    │                                        │       │                                     │
    │ transferenciaService.transferir()      │       │  Objeto Transferencia               │
    │  ├─ conn ──────────────────────[Ref: 0x1102]───┼──> [origem=1, destino=2, status...] │
    └────────────────────────────────────────┘       └─────────────────────────────────────┘
```

1. **Memória Stack (Pilha de Execução):**
   * Armazena as variáveis locais do método em execução, os ponteiros de instrução e as referências de memória.
   * A alocação e desalocação são instantâneas: quando a execução de um método atinge a chave de fechamento `}`, o seu frame na pilha é destruído imediatamente.
2. **Memória Heap (Monte Dinâmico):**
   * Onde todos os objetos reais instanciados via palavra-chave `new` residem fisicamente (ex: instâncias de `Conta`, `Transferencia`, `BigDecimal` e `PreparedStatement`).
   * A limpeza da Heap não é manual como em C (`free()`), mas orquestrada pelo **Garbage Collector (GC)** da JVM, que varre periodicamente a memória coletando objetos que não possuem mais nenhuma referência viva na Stack.

---

## ⚖️ 3. A Batalha dos Tipos: `Long` (Wrapper) vs `long` (Primitivo)

Uma das decisões arquiteturais mais críticas no design da entidade `Conta` foi a escolha do tipo do atributo identificador `id`:

| Característica | Primitivo `long` | Wrapper `Long` |
| :--- | :--- | :--- |
| **Tamanho em Memória** | 64 bits crus de valor na Stack. | Referência na Stack apontando para um objeto na Heap. |
| **Valor Inicial Padrão** | `0` (Zero). | `null` (Nenhuma referência alocada). |
| **Aceita Valor Nulo?** | ❌ Não aceita `null`. | ✅ Aceita `null` perfeitamente. |
| **Semântica de Banco de Dados** | Ambíguo: Não diferencia ID zero de conta nova. | **Perfeito:** `null` indica explicitamente registro ainda não persistido. |

> **A Regra de Ouro da Persistência:** Quando uma conta acabou de nascer na memória da aplicação, o banco de dados ainda não gerou sua chave primária sequencial (`BIGSERIAL`). Portanto, o seu ID **deve ser nulo**. Se usássemos `long` primitivo, o ID nasceria valendo `0`, criando o risco gravíssimo de uma query confundir uma conta nova com uma hipotética conta de ID 0 existente no disco.

---

## 💰 4. Precisão Monetária Arbitrária: A Morte do IEEE 754

Nunca, sob hipótese alguma, utilize `float` ou `double` para manipular dinheiro em software contábil ou bancário.

### O Problema do IEEE 754
Processadores executam cálculos em ponto flutuante binário segundo o padrão internacional IEEE 754. Certas frações decimais perfeitas em base 10 (como `0.1` ou `0.05`) não possuem representação finita exata em base 2 (tornam-se dízimas periódicas binárias).

Se você somar `0.1 + 0.2` utilizando tipos primitivos em qualquer linguagem, a CPU devolverá:
$$\text{Resultado} = 0.30000000000000004$$

Em uma instituição financeira que processa dezenas de milhões de transferências diárias, essa imprecisão fracionária geraria rombos milionários de conciliação contábil.

### A Solução: `java.math.BigDecimal`
O `BigDecimal` armazena números com **precisão arbitrária em base 10**, decompondo o valor em dois inteiros de tamanho ilimitado:
1. Um `BigInteger` não escalado contendo os dígitos (ex: `10050`);
2. Uma escala inteira de 32 bits que define a posição da vírgula (ex: escala `2`, resultando em `100.50`).

### A Física do `compareTo()` e a Subtração Direta na CPU
O `BigDecimal` é um objeto imutável. Logo, operadores aritméticos (`+`, `-`) e relacionais (`<`, `>`, `<=`) não funcionam diretamente. Para comparar valores, a JVM implementa o método da interface `Comparable`:

$$\text{Retorno} = \text{Valor\_Digitado} - \text{Alvo}$$

A CPU executa uma subtração interna entre os valores e avalia o sinal aritmético do resultado:
* Se $A - B < 0 \rightarrow$ Retorna **`-1`** ($A$ é estritamente menor que $B$);
* Se $A - B = 0 \rightarrow$ Retorna **`0`** ($A$ é exatamente igual a $B$);
* Se $A - B > 0 \rightarrow$ Retorna **`+1`** ($A$ é estritamente maior que $B$).

> **A Armadilha do Bug de Comparação:** A validação lógica no código deve SEMPRE ser feita comparando o retorno do método contra o número `0` (ex: `valor.compareTo(BigDecimal.ZERO) <= 0`). Testar contra outros valores corrompe a semântica de decisão lógica.

---

## 🛡️ 5. Filosofia Fail-Fast & A Semântica das Exceções

O princípio **Fail-Fast** estabelece que qualquer inconsistência deve interromper a execução na fronteira imediata de entrada, impedindo que o dado corrompido viaje profundamente pelo sistema e gaste recursos de rede ou disco:

```java
// Fronteira de Segurança do Saque:
public void sacar(BigDecimal valor) {
    // 1. Trava de Parâmetro: O dado externo enviado é ilícito por si só
    if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("O valor do saque deve ser maior que zero!");
    }

    // 2. Trava de Estado: O dado é lícito (ex: R$ 50), mas o estado da entidade impede
    if (this.saldo.compareTo(valor) < 0) {
        throw new IllegalStateException("Saldo insuficiente para realizar o saque");
    }

    this.saldo = this.saldo.subtract(valor);
}
```

### A Distinção Semântica Crucial:
* **`IllegalArgumentException`:** O erro reside no argumento recebido. O chamador passou um valor absurdo (negativo, zero ou nulo). O objeto não tem culpa do argumento inválido.
* **`IllegalStateException`:** O parâmetro é matematicamente válido, mas o estado interno atual do objeto receptor não permite aquela operação naquele momento (ex: tentar sacar R$ 100 de uma conta que só tem R$ 20).

---

## 🔌 6. A Conexão Física JDBC (`DatabaseConnection`)

Para conectar o Java ao PostgreSQL sem intermediários, utilizamos o driver JDBC oficial da comunidade (`org.postgresql.postgresql`):

```
+------------------+         TCP Socket (Port 5432)        +------------------+
|  Aplicação Java  | ===================================== |  PostgreSQL 18   |
| (DatabaseConn)   |   URL: jdbc:postgresql://localhost... |  Schema: finance |
+------------------+                                       +------------------+
```

### Os Componentes da URL de Conexão:
`jdbc:postgresql://localhost:5432/transactcore_db?currentSchema=financeiro`
1. `jdbc:` Protocolo raiz do subsistema de banco de dados do ecossistema Java.
2. `postgresql:` Sub-protocolo que instrui o `DriverManager` a carregar a classe do driver PostgreSQL registrada no classpath.
3. `//localhost:` Endereço do host da máquina servidora (`127.0.0.1`).
4. `:5432` Porta de rede TCP oficial padronizada mundialmente pelo projeto PostgreSQL.
5. `/transactcore_db` Nome do catálogo / banco físico criado na instância.
6. `?currentSchema=financeiro` Parâmetro de sessão que ancora automaticamente todas as queries SQL dentro do schema corporativo, isolando-o do schema `public`.

---

## 🗃️ 7. O Padrão Repository & O RowMapper Manual

Para desacoplar a manipulação em memória da infraestrutura de SQL, implementamos o **Repository Pattern**:

### A Missão do RowMapper (`mapearConta`):
O banco de dados relacional devolve os dados em tabelas bidimensionais de linhas e colunas através do objeto `ResultSet`. O método privado `mapearConta` atua como um tradutor cirúrgico:
1. Extrai tipos primitivos e SQL do cursor (`rs.getLong("id")`, `rs.getString("titular")`, `rs.getBigDecimal("saldo")`);
2. Converte timestamps de banco para a API moderna de tempo do Java 8+ (`Timestamp.toLocalDateTime()`);
3. Instancia a entidade de domínio `Conta` na memória Heap através do Construtor de Reconstrução.

---

## ⚛️ 8. O Coração Transacional ACID & A Conexão Compartilhada

O maior desafio técnico do projeto foi garantir que uma transferência bancária (Pix) cumpra rigorosamente os 4 pilares do **ACID**:

* **Atomicidade (Atomicity):** Ou o débito na origem, o crédito no destino e o comprovante no extrato são gravados juntos, ou **nada** é gravado.
* **Consistência (Consistency):** Nenhuma regra de integridade do banco (como `saldo >= 0.00` ou FKs) pode ser violada.
* **Isolamento (Isolation):** Nenhuma outra transação simultânea pode enxergar saldos parciais no meio do caminho.
* **Durabilidade (Durability):** Uma vez feito o `commit`, a gravação é persistida no disco físico e resiste até mesmo a quedas repentinas de energia.

### O Segredo da Conexão Compartilhada:
No JDBC puro, uma transação de banco de dados está estritamente vinculada a **uma única instância de `java.sql.Connection`**. 

Se o repositório de contas abrisse sua própria conexão para atualizar o saldo e o repositório de transferências abrisse outra conexão diferente, teríamos duas sessões TCP isoladas no PostgreSQL. Um eventual `rollback()` executado na sessão 2 seria incapaz de desfazer a gravação já cometida pela sessão 1!

Por isso, concebemos a sobrecarga de métodos nos repositórios:
```java
void atualizarSaldo(Conta conta, Connection conn);
void salvar(Transferencia transferencia, Connection conn);
```

### O Algoritmo de Execução Atômica (`TransferenciaService`):
```java
try (Connection conn = DatabaseConnection.conectar()) {

    // 1. DESATIVA O AUTO-COMMIT: Ativa o controle transacional manual
    conn.setAutoCommit(false);

    try {
        // 2. Busca e valida saldos na memória Heap
        contaOrigem.sacar(valor);
        contaDestino.depositar(valor);

        // 3. Atualiza saldos usando o MESMO socket físico (conn)
        contaRepository.atualizarSaldo(contaOrigem, conn);
        contaRepository.atualizarSaldo(contaDestino, conn);

        // 4. Registra o comprovante usando o MESMO socket físico (conn)
        transferenciaRepository.salvar(registroContabil, conn);

        // 5. CARIMBA O DISCO DEFINITIVO: Tudo persistido com sucesso!
        conn.commit();

    } catch (Exception e) {
        // 6. ESCUDO DE PROTEÇÃO: Se qualquer coisa falhar, desfaz tudo na hora!
        conn.rollback();
        throw new RuntimeException("Transação abortada: " + e.getMessage(), e);
    }
}
```

---

## 📦 9. Anatomia do Maven & Build Hermético (`pom.xml`)

O gerenciador de dependências e build adotado é o **Apache Maven**:
* **Coordenadas GAV (`groupId`, `artifactId`, `version`):** Identificam universalmente a biblioteca do projeto no ecossistema global.
* **Driver PostgreSQL (`42.7.5`):** Adicionado ao escopo de compilação e runtime sem acoplamento manual de arquivos `.jar` na raiz do repositório.
* **Plugins de Compilação:** Fixação do Java 25 (`<release>25</release>`) e codificação estrita em UTF-8 (`<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`), garantindo compilação uniforme tanto em Windows quanto em servidores Linux em nuvem.

---

## 🏆 10. Conclusão

Construir o **TransactCore-Java** linha por linha, sem atalhos e sem atenuantes, consolida os fundamentos que diferenciam um programador comum de um arquiteto de software de elite. Compreender a física dos dados na memória RAM e os limites dos protocolos de persistência em disco é a base que sustenta os sistemas de missão crítica do sistema financeiro global.
