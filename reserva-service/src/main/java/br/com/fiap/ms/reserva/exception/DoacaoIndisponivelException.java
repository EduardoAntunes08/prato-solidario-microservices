package br.com.fiap.ms.reserva.exception;

/**
 * A doacao existe, mas nao esta DISPONIVEL para ser reservada.
 * Resulta em HTTP 409 Conflict.
 */
public class DoacaoIndisponivelException extends RuntimeException {

	public DoacaoIndisponivelException(String message) {
		super(message);
	}
}
