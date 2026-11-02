package com.transactcore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de Dominio: Transferencia (Pix / TED)
 * 
 * Representa o registro contabil imutavel de movimentacao financeira entre contas.
 * Possui apenas getters para assegurar a inviolabilidade do historico transacional.
 * 
 * Autor: Marcel Florentino
 */
public class Transferencia {

    private Long id;
    private Long contaOrigem;
    private Long contaDestino;
    private BigDecimal valor;
    private LocalDateTime dataHora;
    private String status;

    /**
     * Construtor para Inicio de Transferencia.
     * Utilizado para criar um novo registro com timestamp e status CONCLUIDA.
     */
    public Transferencia(Long contaOrigem, Long contaDestino, BigDecimal valor) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.dataHora = LocalDateTime.now();
        this.status = "CONCLUIDA";
    }

    /**
     * Construtor de Reconstrucao da Entidade.
     * Utilizado pelo RowMapper ao ler comprovantes persistidos no banco de dados.
     */
    public Transferencia(Long id, Long contaOrigem, Long contaDestino, BigDecimal valor, LocalDateTime dataHora, String status) {
        this.id = id;
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.dataHora = dataHora;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getContaOrigem() {
        return contaOrigem;
    }

    public Long getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getStatus() {
        return status;
    }
}
