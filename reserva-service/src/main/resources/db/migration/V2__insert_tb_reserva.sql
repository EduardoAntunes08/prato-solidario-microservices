-- Carga inicial de reservas.
-- Os doacao_id e status conversam com a carga inicial do doacao-service:
--   doacao 1 -> RESERVADA  (esta reserva esta ATIVA)
--   doacao 2 -> RETIRADA   (esta reserva foi CONCLUIDA)
--   doacao 3 -> DISPONIVEL (esta reserva foi CANCELADA e devolveu a doacao)
INSERT INTO tb_reserva (doacao_id, instituicao, responsavel, data_reserva, data_retirada, status) VALUES
    (1, 'ONG Mesa Cheia', 'Maria Oliveira', '2026-11-10', '2027-01-15', 'ATIVA'),
    (2, 'Cozinha Comunitaria Esperanca', 'Joao Pereira', '2026-11-05', '2026-11-08', 'CONCLUIDA'),
    (3, 'Casa de Apoio Sao Jose', 'Ana Souza', '2026-11-01', '2026-11-12', 'CANCELADA');
