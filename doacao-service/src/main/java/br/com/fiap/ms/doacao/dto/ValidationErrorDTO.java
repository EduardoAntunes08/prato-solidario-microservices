package br.com.fiap.ms.doacao.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Erro de validacao (HTTP 422): alem do corpo padrao, lista campo a campo
 * o que foi reprovado pelo Bean Validation.
 */
@Getter
public class ValidationErrorDTO extends CustomErrorDTO {

	private final List<FieldMessageDTO> errors = new ArrayList<>();

	public ValidationErrorDTO(Instant timestamp, Integer status, String error, String path) {
		super(timestamp, status, error, path);
	}

	public void addError(String fieldName, String message) {
		errors.add(new FieldMessageDTO(fieldName, message));
	}
}
