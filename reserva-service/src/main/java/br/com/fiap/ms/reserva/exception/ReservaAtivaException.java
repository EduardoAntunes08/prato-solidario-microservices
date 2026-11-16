package br.com.fiap.ms.reserva.exception;

/**
 * A reserva ainda esta ATIVA e segura uma doacao do outro lado, entao a
 * operacao pedida nao pode ser feita sem antes cancelar a reserva.
 * Resulta em HTTP 409 Conflict.
 */
public class ReservaAtivaException extends RuntimeException {

	public ReservaAtivaException(String message) {
		super(message);
	}
}
