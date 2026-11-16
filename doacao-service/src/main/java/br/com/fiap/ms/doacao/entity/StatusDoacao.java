package br.com.fiap.ms.doacao.entity;

/**
 * Ciclo de vida de uma doacao de excedente de alimento.
 *
 * DISPONIVEL -> RESERVADA -> RETIRADA
 *
 * A transicao e sempre disparada pelo reserva-service por meio da
 * comunicacao sincrona (OpenFeign).
 */
public enum StatusDoacao {

	DISPONIVEL,
	RESERVADA,
	RETIRADA
}
