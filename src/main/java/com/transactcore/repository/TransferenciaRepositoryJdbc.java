package com.transactcore.repository;

import com.transactcore.infra.DatabaseConnection;
import com.transactcore.model.Transferencia;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Implementacao JDBC do Repositorio de Transferencias
 * Camada: Acesso a Dados / Persistencia (Repository)
 * 
 * Gerencia o registro historico de transferencias no PostgreSQL 18.
 * 
 * Autor: Marcel Florentino
 */
public class TransferenciaRepositoryJdbc implements TransferenciaRepository {

    @Override
    public void salvar(Transferencia transferencia) {
        try (Connection conn = DatabaseConnection.conectar()) {
            salvar(transferencia, conn);
        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao registrar transferencia no banco: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void salvar(Transferencia transferencia, Connection conn) {
        String sql = "INSERT INTO financeiro.transferencias (conta_origem_id, conta_destino_id, valor, data_hora, status) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transferencia.getContaOrigem());
            ps.setLong(2, transferencia.getContaDestino());
            ps.setBigDecimal(3, transferencia.getValor());
            ps.setTimestamp(4, Timestamp.valueOf(transferencia.getDataHora()));
            ps.setString(5, transferencia.getStatus());

            ps.executeUpdate();
            System.out.println("[INFO] Transferencia registrada com sucesso no banco.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao registrar transferencia na transacao: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Transferencia buscarPorId(Long id) {
        String sql = "SELECT id, conta_origem_id, conta_destino_id, valor, data_hora, status FROM financeiro.transferencias WHERE id = ?";

        try (Connection conn = DatabaseConnection.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapearTransferencia(rs);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Erro ao buscar transferencia por ID: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * Metodo Auxiliar: Mapeador de Entidade (RowMapper)
     * Converte a linha do ResultSet no objeto imutavel Transferencia.
     */
    private Transferencia mapearTransferencia(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long contaOrigem = rs.getLong("conta_origem_id");
        Long contaDestino = rs.getLong("conta_destino_id");
        BigDecimal valor = rs.getBigDecimal("valor");
        LocalDateTime dataHora = rs.getTimestamp("data_hora").toLocalDateTime();
        String status = rs.getString("status");

        return new Transferencia(id, contaOrigem, contaDestino, valor, dataHora, status);
    }
}
