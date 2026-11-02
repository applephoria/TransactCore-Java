package com.transactcore.repository;

import com.transactcore.model.Conta;
import java.sql.Connection;

/**
 * Contrato de Persistencia: ContaRepository
 * 
 * Define as operacoes de acesso a dados para a entidade Conta,
 * suportando execucoes isoladas e sob transacao ACID compartilhada.
 * 
 * Autor: Marcel Florentino
 */
public interface ContaRepository {

    void salvar(Conta conta);

    Conta buscarPorId(Long id);

    Conta buscarPorCpf(String cpf);

    void atualizarSaldo(Conta conta);

    void atualizarSaldo(Conta conta, Connection conn);
}
