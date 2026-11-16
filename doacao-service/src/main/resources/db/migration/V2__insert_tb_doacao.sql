-- Carga inicial de doacoes.
-- Os status aqui conversam com a carga inicial do reserva-service:
--   id 1 -> RESERVADA  (reserva 1, ATIVA)
--   id 2 -> RETIRADA   (reserva 2, CONCLUIDA)
--   id 3 -> DISPONIVEL (reserva 3 foi CANCELADA e devolveu a doacao)
INSERT INTO tb_doacao (doador, alimento, categoria, quantidade_kg, data_validade, status) VALUES
    ('Padaria Pao Quente', 'Paes do dia anterior', 'PAES', 12.5, '2027-03-15', 'RESERVADA'),
    ('Hortifruti Vale Verde', 'Bananas maduras', 'FRUTAS_E_VERDURAS', 30.0, '2027-02-28', 'RETIRADA'),
    ('Mercado Bom Preco', 'Iogurtes proximos do vencimento', 'LATICINIOS', 8.75, '2027-01-20', 'DISPONIVEL'),
    ('Restaurante Sabor Caseiro', 'Marmitas do almoco', 'REFEICOES_PRONTAS', 22.0, '2027-04-10', 'DISPONIVEL'),
    ('Feira do Produtor', 'Verduras folhosas', 'FRUTAS_E_VERDURAS', 15.3, '2027-05-05', 'DISPONIVEL'),
    ('Supermercado Economia', 'Alimentos diversos nao pereciveis', 'OUTROS', 45.0, '2027-12-31', 'DISPONIVEL');
