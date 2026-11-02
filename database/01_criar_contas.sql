-- 01_criar_contas.sql
-- Autor: Marcel Florentino (@applephoria)
-- Schema Corporativo: financeiro

CREATE SCHEMA IF NOT EXISTS financeiro;

CREATE TABLE IF NOT EXISTS financeiro.contas (
    id BIGSERIAL PRIMARY KEY,
    titular VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    saldo NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_saldo_positivo CHECK (saldo >= 0.00)
);

CREATE INDEX IF NOT EXISTS idx_contas_cpf ON financeiro.contas(cpf);
