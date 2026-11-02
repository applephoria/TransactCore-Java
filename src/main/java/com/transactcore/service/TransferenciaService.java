package com.transactcore.service;

import com.transactcore.infra.DatabaseConnection;
import com.transactcore.model.Conta;
import com.transactcore.model.Transferencia;
import com.transactcore.repository.ContaRepository;
import com.transactcore.repository.ContaRepositoryJdbc;
import com.transactcore.repository.TransferenciaRepository;
import com.transactcore.repository.TransferenciaRepositoryJdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Camada de Servico (Service Layer)
 * 
 * Orquestra as regras de negocio de movimentacao financeira entre contas
 * e garante a Atomicidade (Tudo ou Nada) por meio de Transacoes ACID no PostgreSQL.
 * 
 * Autor: Marcel Florentino
 */
public class TransferenciaService {

    private final ContaRepository contaRepository;
    private final TransferenciaRepository transferenciaRepository;

    public TransferenciaService() {
        this.contaRepository = new ContaRepositoryJdbc();
        this.transferenciaRepository = new TransferenciaRepositoryJdbc();
    }

    public TransferenciaService(ContaRepository contaRepository, TransferenciaRepository transferenciaRepository) {
        this.contaRepository = contaRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    /**
     * Executa uma transferencia bancaria atomica completa entre duas contas.
     * 
     * Rastreamento da Transacao ACID:
     * 1. conn.setAutoCommit(false): Ativa o modo de seguranca transacional;
     * 2. Debita da conta de origem na memoria;
     * 3. Credita na conta de destino na memoria;
     * 4. Atualiza os saldos no banco compartilhando a mesma conexao fisica;
     * 5. Grava o registro contabil na tabela transferencias usando a mesma conexao;
     * 6. conn.commit(): Persiste definitivamente as alteracoes no disco;
     * 7. conn.rollback(): Desfaz todas as operacoes caso ocorra qualquer inconsistencia.
     */
    public void transferir(Long contaOrigemId, Long contaDestinoId, BigDecimal valor) {
        // Validacoes Fail-Fast preliminares
        if (contaOrigemId == null || contaDestinoId == null) {
            throw new IllegalArgumentException("As contas de origem e destino sao obrigatorias.");
        }
        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("A conta de origem nao pode ser igual a conta de destino.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferencia deve ser maior que zero.");
        }

        // Abre a conexao transacional unica:
        try (Connection conn = DatabaseConnection.conectar()) {

            // Desativa o auto-commit para controlar manualmente a transacao:
            conn.setAutoCommit(false);

            try {
                // Passo 1: Busca as duas contas no banco de dados
                Conta contaOrigem = contaRepository.buscarPorId(contaOrigemId);
                Conta contaDestino = contaRepository.buscarPorId(contaDestinoId);

                if (contaOrigem == null) {
                    throw new IllegalArgumentException("Conta de origem nao encontrada no banco. ID: " + contaOrigemId);
                }
                if (contaDestino == null) {
                    throw new IllegalArgumentException("Conta de destino nao encontrada no banco. ID: " + contaDestinoId);
                }

                // Passo 2: Aplica as regras de negocio de debito e credito na memoria Heap
                contaOrigem.sacar(valor);
                contaDestino.depositar(valor);

                // Passo 3: Atualiza o saldo das duas contas compartilhando a mesma conexao
                contaRepository.atualizarSaldo(contaOrigem, conn);
                contaRepository.atualizarSaldo(contaDestino, conn);

                // Passo 4: Cria e grava o comprovante contabil utilizando a mesma conexao
                Transferencia registroContabil = new Transferencia(contaOrigemId, contaDestinoId, valor);
                transferenciaRepository.salvar(registroContabil, conn);

                // Passo 5: Carimba todas as gravacoes no disco de forma atomica
                conn.commit();
                System.out.println("[SUCCESS] Transferencia de R$ " + valor + " concluida com sucesso e persistida no banco (COMMIT).");

            } catch (Exception e) {
                // Em caso de falha, desfaz todas as alteracoes realizadas na transacao
                conn.rollback();
                System.err.println("[ROLLBACK] Falha na operacao bancaria. Transacao cancelada e revertida: " + e.getMessage());
                throw new RuntimeException("Transacao abortada: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Erro de infraestrutura no banco de dados: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
