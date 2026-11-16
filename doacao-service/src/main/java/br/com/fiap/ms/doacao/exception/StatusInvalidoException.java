package br.com.fiap.ms.doacao.exception;

/**
 * Lancada quando se tenta uma transicao de status que o ciclo de vida da
 * doacao nao permite (ex.: reservar uma doacao que ja esta RESERVADA).
 * Resulta em HTTP 409 Conflict.
 */
public class StatusInvalidoException extends RuntimeException {

	public StatusInvalidoException(String message) {
		super(message);
	}
}
