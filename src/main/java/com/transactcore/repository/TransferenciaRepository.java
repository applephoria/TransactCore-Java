package com.transactcore.repository;

import com.transactcore.model.Transferencia;
import java.sql.Connection;

/**
 * Contrato de Persistencia: TransferenciaRepository
 * 
 * Define as operacoes de persistencia e consulta para registros contabeis,
 * suportando execucoes isoladas e sob transacao ACID compartilhada.
 * 
 * Autor: Marcel Florentino
 */
public interface TransferenciaRepository {

    void salvar(Transferencia transferencia);

    void salvar(Transferencia transferencia, Connection conn);

    Transferencia buscarPorId(Long id);
}
