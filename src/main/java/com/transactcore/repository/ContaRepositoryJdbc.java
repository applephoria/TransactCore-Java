package com.transactcore.repository;

import com.transactcore.infra.DatabaseConnection;
import com.transactcore.model.Conta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Implementacao JDBC do Repositorio de Contas
 * Camada: Acesso a Dados / Persistencia (Repository)
 * 
 * Executa operacoes CRUD diretamente no PostgreSQL 18 utilizando PreparedStatement
 * e mapeamento manual de ResultSet (RowMapper).
 * 
 * Autor: Marcel Florentino
 */
public class ContaRepositoryJdbc implements ContaRepository {

    @Override
    public void salvar(Conta novaConta) {
        String sql = "INSERT INTO financeiro.contas (titular, cpf, saldo, criado_em) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, novaConta.getTitular());
            ps.setString(2, novaConta.getCpf());
            ps.setBigDecimal(3, novaConta.getSaldo());
            ps.setTimestamp(4, Timestamp.valueOf(novaConta.getCriadoEm()));

            ps.executeUpdate();
            System.out.println("[INFO] Conta de " + novaConta.getTitular() + " gravada com sucesso no PostgreSQL.");

        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao salvar conta no banco: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Conta buscarPorCpf(String cpf) {
        String sql = "SELECT id, titular, cpf, saldo, criado_em FROM financeiro.contas WHERE cpf = ?";

        try (Connection conn = DatabaseConnection.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cpf);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearConta(rs);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao buscar conta por CPF: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public Conta buscarPorId(Long id) {
        String sql = "SELECT id, titular, cpf, saldo, criado_em FROM financeiro.contas WHERE id = ?";

        try (Connection conn = DatabaseConnection.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearConta(rs);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao buscar conta por ID: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void atualizarSaldo(Conta conta) {
        try (Connection conn = DatabaseConnection.conectar()) {
            atualizarSaldo(conta, conn);
        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao atualizar saldo no banco: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void atualizarSaldo(Conta conta, Connection conn) {
        String sql = "UPDATE financeiro.contas SET saldo = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, conta.getSaldo());
            ps.setLong(2, conta.getId());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                System.out.println("[INFO] Saldo da conta ID " + conta.getId() + " atualizado para R$ " + conta.getSaldo() + " com sucesso.");
            } else {
                System.err.println("[WARN] Alerta de Integridade: Nenhuma conta encontrada com o ID " + conta.getId() + ".");
                throw new RuntimeException("Conta nao encontrada no banco de dados para atualizar saldo. ID: " + conta.getId());
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao atualizar saldo na transacao: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Metodo Auxiliar: Mapeador de Entidade (RowMapper)
     * Extrai os registros brutos do ResultSet e instancia o objeto de Dominio na Heap.
     */
    private Conta mapearConta(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String titular = rs.getString("titular");
        String cpf = rs.getString("cpf");
        BigDecimal saldo = rs.getBigDecimal("saldo");
        LocalDateTime criadoEm = rs.getTimestamp("criado_em").toLocalDateTime();

        return new Conta(id, titular, cpf, saldo, criadoEm);
    }
}
