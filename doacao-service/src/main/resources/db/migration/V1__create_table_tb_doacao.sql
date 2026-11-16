CREATE TABLE IF NOT EXISTS tb_doacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doador VARCHAR(100) NOT NULL,
    alimento VARCHAR(100) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    quantidade_kg DOUBLE NOT NULL,
    data_validade DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tb_doacao PRIMARY KEY (id)
);
