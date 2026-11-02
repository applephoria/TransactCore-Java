# 🧪 Catálogo de Exemplos de Teste — TransactCore-Java

> Este arquivo reúne todos os cenários práticos desenvolvidos no projeto, escritos no **Padrão Verboso e Didático de Elite por Marcel Florentino** (cada variável explicitada, sem atalhos em linha e com o rastro dos dados comentado passo a passo).  
> 
> Se você quiser testar qualquer um destes fluxos, **basta copiar o bloco de código e colar dentro do método `main()` no seu `Main.java`**!

---

## 📌 Cenário 1: Regras de Negócio da Conta (Memória Heap & Fail-Fast)
*Testa a criação da entidade em memória, depósitos, saques válidos e a barreira de segurança impedindo saques sem saldo.*

```java
// 1. Dados de entrada explícitos:
String titular = "Marcel Florentino";
String cpf = "12345678901";

// 2. Instancia a conta na memória Heap usando o Construtor 1:
Conta contaMarcel = new Conta(titular, cpf);

// 3. Leitura dos dados iniciais através dos Getters:
System.out.println("[INFO] Titular: " + contaMarcel.getTitular());
System.out.println("[INFO] CPF: " + contaMarcel.getCpf());
System.out.println("[INFO] Saldo Inicial: R$ " + contaMarcel.getSaldo());
System.out.println("[INFO] Data de Criacao: " + contaMarcel.getCriadoEm());

// 4. Executando um Depósito válido:
System.out.println("\n--- REALIZANDO DEPOSITO ---");
BigDecimal valorDeposito = new BigDecimal("500.00");
contaMarcel.depositar(valorDeposito);
System.out.println("[INFO] Saldo apos deposito de R$ 500.00: R$ " + contaMarcel.getSaldo());

// 5. Executando um Saque válido:
System.out.println("\n--- REALIZANDO SAQUE ---");
BigDecimal valorSaqueValido = new BigDecimal("200.00");
contaMarcel.sacar(valorSaqueValido);
System.out.println("[INFO] Saldo apos saque de R$ 200.00: R$ " + contaMarcel.getSaldo());

// 6. Testando a barreira de Fail-Fast (Tentativa de sacar R$ 400.00 tendo apenas R$ 300.00):
System.out.println("\n--- TENTANDO SACAR R$ 400.00 (SEM SALDO) ---");
BigDecimal valorSaqueSemSaldo = new BigDecimal("400.00");

try {
    contaMarcel.sacar(valorSaqueSemSaldo);
} catch (IllegalStateException e) {
    System.out.println("[SUCCESS] Bloqueado com sucesso pelo Fail-Fast: " + e.getMessage());
}
```

---

## 📌 Cenário 2: Teste de Conexão Física com o PostgreSQL 18
*Testa se o canal de rede JDBC consegue conectar na porta 5432 e autenticar no banco `transactcore_db`.*

```java
System.out.println("\n--- TESTANDO CONEXAO COM O POSTGRESQL ---");

try (Connection conn = DatabaseConnection.conectar()) {
    System.out.println("[SUCCESS] Conexao estabelecida com sucesso.");
    System.out.println("[INFO] Conectado ao banco transactcore_db no PostgreSQL 18.");
} catch (SQLException e) {
    System.err.println("[ERROR] Falha ao conectar no banco: " + e.getMessage());
}
```

---

## 📌 Cenário 3: Salvar e Buscar Conta por CPF (INSERT e SELECT)
*Testa a gravação de uma nova conta no disco rígido do PostgreSQL e sua posterior recuperação pelo CPF.*

```java
System.out.println("\n--- GRAVANDO E BUSCANDO CONTA POR CPF ---");

// 1. Instanciamos o repositório de contas:
ContaRepository contaRepository = new ContaRepositoryJdbc();

// 2. Dados da nova conta:
String titularNovo = "Marcel Florentino";
String cpfNovo = "12345678901";

// 3. Instancia o objeto na memória Heap:
Conta novaConta = new Conta(titularNovo, cpfNovo);

// 4. Grava no banco (Execute apenas na primeira vez para não duplicar CPF):
// contaRepository.salvar(novaConta);

// 5. Consulta a conta recém-gravada através do CPF:
String cpfConsulta = "12345678901";
Conta contaDoBanco = contaRepository.buscarPorCpf(cpfConsulta);

// 6. Validação defensiva antes de ler os dados (Zero NullPointerException):
if (contaDoBanco != null) {
    System.out.println("[SUCCESS] Conta encontrada no banco.");
    System.out.println("[INFO] ID Oficial: " + contaDoBanco.getId());
    System.out.println("[INFO] Titular: " + contaDoBanco.getTitular());
    System.out.println("[INFO] CPF: " + contaDoBanco.getCpf());
    System.out.println("[INFO] Saldo Persistido: R$ " + contaDoBanco.getSaldo());
    System.out.println("[INFO] Data de Criacao: " + contaDoBanco.getCriadoEm());
} else {
    System.out.println("[ERROR] Conta nao encontrada para o CPF: " + cpfConsulta);
}
```

---

## 📌 Cenário 4: Buscar por ID e Atualizar Saldo no Disco (UPDATE)
*Busca a conta no disco, altera o saldo na memória e regrava o novo saldo no PostgreSQL usando `UPDATE` com validação de linhas afetadas.*

```java
System.out.println("\n--- ATUALIZANDO O SALDO NO BANCO DE DADOS ---");

// 1. Instanciamos o repositório de contas:
ContaRepository contaRepository = new ContaRepositoryJdbc();

// 2. Buscamos a conta existente pelo ID oficial (1L):
Long idConta = 1L;
Conta contaPeloId = contaRepository.buscarPorId(idConta);

// 3. Se a conta existir, realizamos o fluxo de atualização:
if (contaPeloId != null) {
    System.out.println("[INFO] Saldo antes da operacao: R$ " + contaPeloId.getSaldo());

    // Passo A: A conta recebe um depósito na memória RAM:
    BigDecimal valorPixRecebido = new BigDecimal("200.00");
    contaPeloId.depositar(valorPixRecebido);

    // Passo B: Mandamos o repositório disparar o UPDATE no PostgreSQL:
    contaRepository.atualizarSaldo(contaPeloId);

    // Passo C: Buscamos novamente do disco para certificar a persistência física:
    Conta contaAtualizada = contaRepository.buscarPorId(idConta);
    System.out.println("[SUCCESS] Saldo final conferido no PostgreSQL: R$ " + contaAtualizada.getSaldo());
} else {
    System.out.println("[ERROR] Nenhuma conta encontrada com o ID " + idConta);
}
```

---

## 📌 Cenário 5: Registro Contábil de Transferência
*Gera uma transferência da Conta 1 para a Conta 2, salva no banco e consulta o comprovante imutável.*

```java
System.out.println("\n--- REGISTRO CONTABIL DE TRANSFERENCIA ---");

// 1. Instanciamos os dois repositórios especialistas:
ContaRepository contaRepository = new ContaRepositoryJdbc();
TransferenciaRepository transferenciaRepository = new TransferenciaRepositoryJdbc();

// 2. Dados da transferência declarados explicitamente:
Long contaOrigemId = 1L;
Long contaDestinoId = 2L;
BigDecimal valorTransferencia = new BigDecimal("150.00");

// 3. Instancia a Transferência na memória Heap (com data e status automáticos):
Transferencia pix = new Transferencia(contaOrigemId, contaDestinoId, valorTransferencia);

// 4. Manda o repositório gravar no banco (INSERT em financeiro.transferencias):
transferenciaRepository.salvar(pix);

// 5. Consulta o comprovante gravado no disco pelo ID da transferência (1L):
Long idTransferencia = 1L;
Transferencia comprovante = transferenciaRepository.buscarPorId(idTransferencia);

// 6. Exibe os dados oficiais do comprovante contábil:
if (comprovante != null) {
    System.out.println("[SUCCESS] Comprovante oficial encontrado.");
    System.out.println("[INFO] ID da Transferencia: " + comprovante.getId());
    System.out.println("[INFO] Origem: Conta ID " + comprovante.getContaOrigem());
    System.out.println("[INFO] Destino: Conta ID " + comprovante.getContaDestino());
    System.out.println("[INFO] Valor: R$ " + comprovante.getValor());
    System.out.println("[INFO] Data/Hora: " + comprovante.getDataHora());
    System.out.println("[INFO] Status: " + comprovante.getStatus());
} else {
    System.out.println("[ERROR] Comprovante nao encontrado para o ID " + idTransferencia);
}
```

---

## 📌 Cenário 6: Transferência Completa Atômica (ACID Transaction & Rollback)
*Executa o fluxo bancário real através da camada de serviço (`TransferenciaService`), garantindo Atomicidade (Tudo ou Nada). Se houver saldo, debita, credita, registra o comprovante e grava no disco (`conn.commit()`). Se faltar saldo ou der erro, cancela tudo sem corromper os dados (`conn.rollback()`).*

```java
System.out.println("\n--- MOTOR TRANSACIONAL BANCARIO (ACID COMMIT & ROLLBACK) ---");

// 1. Instanciamos o repositório de contas e a camada de serviço:
ContaRepository contaRepository = new ContaRepositoryJdbc();
TransferenciaService transferenciaService = new TransferenciaService();

// 2. Identificadores explícitos das contas de origem e destino:
Long idContaOrigem = 1L;
Long idContaDestino = 2L;

// 3. Consultamos as contas no banco antes da transação:
Conta contaOrigemAntes = contaRepository.buscarPorId(idContaOrigem);
Conta contaDestinoAntes = contaRepository.buscarPorId(idContaDestino);

System.out.println("[INFO] Saldo Origem Antes: R$ " + contaOrigemAntes.getSaldo());
System.out.println("[INFO] Saldo Destino Antes: R$ " + contaDestinoAntes.getSaldo());

// 4. Executando uma Transferência Válida (Atomicidade com COMMIT):
System.out.println("\n--- DISPARANDO TRANSFERENCIA DE R$ 100.00 ---");
BigDecimal valorPixValido = new BigDecimal("100.00");

try {
    transferenciaService.transferir(idContaOrigem, idContaDestino, valorPixValido);
} catch (Exception e) {
    System.err.println("[ERROR] Falha na transferencia: " + e.getMessage());
}

// 5. Consultamos as contas após a transferência para conferir o COMMIT gravado no disco:
Conta contaOrigemDepois = contaRepository.buscarPorId(idContaOrigem);
Conta contaDestinoDepois = contaRepository.buscarPorId(idContaDestino);

System.out.println("[INFO] Saldo Origem Apos Commit: R$ " + contaOrigemDepois.getSaldo());
System.out.println("[INFO] Saldo Destino Apos Commit: R$ " + contaDestinoDepois.getSaldo());

// 6. Testando a Proteção Atômica contra Saldo Insuficiente (ACID ROLLBACK):
System.out.println("\n--- TENTANDO TRANSFERENCIA DE R$ 50.000,00 (SEM SALDO SUFICIENTE) ---");
BigDecimal valorSemSaldo = new BigDecimal("50000.00");

try {
    transferenciaService.transferir(idContaOrigem, idContaDestino, valorSemSaldo);
} catch (Exception e) {
    System.out.println("[SUCCESS] Rollback disparado com sucesso! Nenhuma alteracao foi gravada: " + e.getMessage());
}

// 7. Conferência final do banco de dados (Prova de Integridade):
Conta contaOrigemFinal = contaRepository.buscarPorId(idContaOrigem);
Conta contaDestinoFinal = contaRepository.buscarPorId(idContaDestino);

System.out.println("[INFO] Saldo Origem Final (Intacto): R$ " + contaOrigemFinal.getSaldo());
System.out.println("[INFO] Saldo Destino Final (Intacto): R$ " + contaDestinoFinal.getSaldo());
```
