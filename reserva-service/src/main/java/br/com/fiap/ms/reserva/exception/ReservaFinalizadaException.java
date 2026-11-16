package br.com.fiap.ms.reserva.exception;

/**
 * A reserva ja foi concluida ou cancelada e nao aceita nova transicao.
 * Resulta em HTTP 409 Conflict.
 */
public class ReservaFinalizadaException extends RuntimeException {

	public ReservaFinalizadaException(String message) {
		super(message);
	}
}
