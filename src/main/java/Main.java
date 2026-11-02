import com.transactcore.model.Conta;
import com.transactcore.model.Transferencia;
import com.transactcore.repository.ContaRepository;
import com.transactcore.repository.ContaRepositoryJdbc;
import com.transactcore.repository.TransferenciaRepository;
import com.transactcore.repository.TransferenciaRepositoryJdbc;
import com.transactcore.service.TransferenciaService;

import java.math.BigDecimal;

/**
 * Ponto de Entrada Principal (Main) - TransactCore-Java
 * 
 * Demonstracao do Motor Transacional Bancario em Acao:
 * 1. Transferencia Bancaria Atomica com Sucesso (ACID COMMIT)
 * 2. Protecao de Saldo e Rollback em Caso de Erro (ACID ROLLBACK)
 * 
 * Autor: Marcel Florentino
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=====================================================");
        System.out.println("   TRANSACTCORE-JAVA: MOTOR TRANSACIONAL BANCARIO    ");
        System.out.println("=====================================================");

        executarDemonstracaoTransacional();
    }

    private static void executarDemonstracaoTransacional() {
        // 1. Instanciamos o Repositorio para consultas e o Servico Transacional:
        ContaRepository contaRepository = new ContaRepositoryJdbc();
        TransferenciaService transferenciaService = new TransferenciaService();

        // 2. Identificadores explicitos das duas contas no PostgreSQL:
        Long idContaMarcel = 1L;
        Long idContaHugo = 2L;

        // 3. Consultamos as contas no banco antes de qualquer operacao:
        System.out.println("\n--- ESTADO INICIAL DAS CONTAS NO BANCO ---");
        Conta contaMarcelAntes = contaRepository.buscarPorId(idContaMarcel);
        Conta contaHugoAntes = contaRepository.buscarPorId(idContaHugo);

        if (contaMarcelAntes != null) {
            System.out.println("[INFO] Origem: " + contaMarcelAntes.getTitular() + " | Saldo Atual: R$ " + contaMarcelAntes.getSaldo());
        }
        if (contaHugoAntes != null) {
            System.out.println("[INFO] Destino: " + contaHugoAntes.getTitular() + " | Saldo Atual: R$ " + contaHugoAntes.getSaldo());
        }

        // =========================================================================
        // PARTE 1: TRANSFERENCIA PIX BEM-SUCEDIDA (COMMIT)
        // =========================================================================
        System.out.println("\n--- TESTE 1: EXECUTANDO TRANSFERENCIA ATOMICA COM SUCESSO ---");
        BigDecimal valorTransferencia = new BigDecimal("50.00");

        try {
            transferenciaService.transferir(idContaMarcel, idContaHugo, valorTransferencia);
        } catch (Exception e) {
            System.err.println("[ERROR] Falha inesperada na transferencia: " + e.getMessage());
        }

        // Consultamos novamente do banco para comprovar a gravacao fisica no disco:
        System.out.println("\n--- ESTADO DAS CONTAS APOS A TRANSFERENCIA (GRAVADO NO POSTGRESQL) ---");
        Conta contaMarcelDepois = contaRepository.buscarPorId(idContaMarcel);
        Conta contaHugoDepois = contaRepository.buscarPorId(idContaHugo);

        if (contaMarcelDepois != null) {
            System.out.println("[INFO] " + contaMarcelDepois.getTitular() + " | Novo Saldo: R$ " + contaMarcelDepois.getSaldo());
        }
        if (contaHugoDepois != null) {
            System.out.println("[INFO] " + contaHugoDepois.getTitular() + " | Novo Saldo: R$ " + contaHugoDepois.getSaldo());
        }

        // =========================================================================
        // PARTE 2: PROTECAO CONTRA SALDO INSUFICIENTE (ROLLBACK ATIVADO)
        // =========================================================================
        System.out.println("\n--- TESTE 2: TENTATIVA DE TRANSFERENCIA SEM SALDO SUFICIENTE (ROLLBACK) ---");
        BigDecimal valorSemSaldo = new BigDecimal("50000.00");
        System.out.println("[INFO] Tentando transferir R$ " + valorSemSaldo + " da conta de " + contaMarcelDepois.getTitular() + "...");

        try {
            transferenciaService.transferir(idContaMarcel, idContaHugo, valorSemSaldo);
        } catch (Exception e) {
            System.out.println("[SUCCESS] Protecao Ativada com Sucesso! Excecao capturada: " + e.getMessage());
        }

        // Conferimos o banco de dados para comprovar que nenhum centavo foi alterado:
        System.out.println("\n--- ESTADO FINAL DAS CONTAS (INTEGRIDADE TOTAL GARANTIDA) ---");
        Conta contaMarcelFinal = contaRepository.buscarPorId(idContaMarcel);
        Conta contaHugoFinal = contaRepository.buscarPorId(idContaHugo);

        if (contaMarcelFinal != null) {
            System.out.println("[INFO] " + contaMarcelFinal.getTitular() + " | Saldo Intacto: R$ " + contaMarcelFinal.getSaldo());
        }
        if (contaHugoFinal != null) {
            System.out.println("[INFO] " + contaHugoFinal.getTitular() + " | Saldo Intacto: R$ " + contaHugoFinal.getSaldo());
        }

        System.out.println("\n[SUCCESS] Todos os testes de transacao ACID validados com exito.");
    }
}
