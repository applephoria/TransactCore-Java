package com.transactcore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de Dominio: Conta Bancaria
 * 
 * Modela a conta bancaria contendo regras de negocio rigorosas,
 * encapsulamento puro e imutabilidade de campos criticos.
 * 
 * Autor: Marcel Florentino
 */
public class Conta {

    private Long id;
    private String titular;
    private String cpf;
    private BigDecimal saldo;
    private LocalDateTime criadoEm;

    /**
     * Construtor para Abertura de Nova Conta Bancaria.
     * Utilizado quando a conta nasce na memoria da aplicacao (ID ainda nao gerado pelo banco).
     */
    public Conta(String titular, String cpf) {
        this.titular = titular;
        this.cpf = cpf;
        this.saldo = BigDecimal.ZERO;
        this.criadoEm = LocalDateTime.now();
    }

    /**
     * Construtor de Reconstrucao da Entidade.
     * Utilizado pelo RowMapper ao carregar dados existentes persistidos no PostgreSQL.
     */
    public Conta(Long id, String titular, String cpf, BigDecimal saldo, LocalDateTime criadoEm) {
        this.id = id;
        this.titular = titular;
        this.cpf = cpf;
        this.saldo = saldo;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getTitular() {
        return titular;
    }

    public String getCpf() {
        return cpf;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    /**
     * Regra de Negocio: Deposito Bancario
     * Aplica o padrao Fail-Fast para impedir valores nulos ou menores/iguais a zero.
     */
    public void depositar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do deposito deve ser maior que zero!");
        }

        this.saldo = this.saldo.add(valor);
    }

    /**
     * Regra de Negocio: Saque Bancario
     * Aplica barreiras de seguranca defensiva:
     * 1. Trava de valor invalido (IllegalArgumentException)
     * 2. Trava de saldo insuficiente (IllegalStateException)
     */
    public void sacar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser maior que zero!");
        }

        if (this.saldo.compareTo(valor) < 0) {
            throw new IllegalStateException("Saldo insuficiente para realizar o saque");
        }

        this.saldo = this.saldo.subtract(valor);
    }
}
