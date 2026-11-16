package br.com.fiap.ms.reserva.exception;

/**
 * Falha de comunicacao com o doacao-service (timeout, 5xx, instancia fora
 * do ar). E esta excecao que o Circuit Breaker contabiliza como falha.
 * Resulta em HTTP 503 Service Unavailable.
 */
public class ComunicacaoException extends RuntimeException {

	public ComunicacaoException(String message) {
		super(message);
	}

	public ComunicacaoException(String message, Throwable cause) {
		super(message, cause);
	}
}
