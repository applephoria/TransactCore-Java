-- 02_criar_transferencias.sql
-- Autor: Marcel Florentino (@applephoria)
-- Schema Corporativo: financeiro

CREATE TABLE IF NOT EXISTS financeiro.transferencias (
    id BIGSERIAL PRIMARY KEY,
    conta_origem_id BIGINT NOT NULL,
    conta_destino_id BIGINT NOT NULL,
    valor NUMERIC(15,2) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONCLUIDA',
    CONSTRAINT fk_transferencia_origem FOREIGN KEY (conta_origem_id) 
        REFERENCES financeiro.contas(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transferencia_destino FOREIGN KEY (conta_destino_id) 
        REFERENCES financeiro.contas(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_valor_positivo CHECK (valor > 0.00),
    CONSTRAINT chk_contas_distintas CHECK (conta_origem_id <> conta_destino_id)
);

CREATE INDEX IF NOT EXISTS idx_transferencias_origem ON financeiro.transferencias(conta_origem_id);
CREATE INDEX IF NOT EXISTS idx_transferencias_destino ON financeiro.transferencias(conta_destino_id);
CREATE INDEX IF NOT EXISTS idx_transferencias_data ON financeiro.transferencias(data_hora);
