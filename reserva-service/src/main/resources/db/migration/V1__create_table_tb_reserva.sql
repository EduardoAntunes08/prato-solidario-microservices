CREATE TABLE IF NOT EXISTS tb_reserva (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doacao_id BIGINT NOT NULL,
    instituicao VARCHAR(100) NOT NULL,
    responsavel VARCHAR(100) NOT NULL,
    data_reserva DATE NOT NULL,
    data_retirada DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT pk_tb_reserva PRIMARY KEY (id)
);

-- doacao_id e uma referencia logica ao doacao-service. Nao existe chave
-- estrangeira: cada microsservico tem banco proprio e independente.
