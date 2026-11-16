package br.com.fiap.ms.reserva.entity;

/**
 * Ciclo de vida de uma reserva feita por uma instituicao.
 *
 * ATIVA -> CONCLUIDA   (a instituicao retirou a doacao)
 * ATIVA -> CANCELADA   (a reserva foi desfeita e a doacao volta a DISPONIVEL)
 *
 * PENDENTE_SINCRONIZACAO e o estado gravado pelo fallback quando o
 * doacao-service esta indisponivel: a reserva mudou de lado de ca, mas a
 * doacao ainda nao pode ser atualizada do lado de la.
 */
public enum StatusReserva {

	ATIVA,
	CONCLUIDA,
	CANCELADA,
	PENDENTE_SINCRONIZACAO
}
