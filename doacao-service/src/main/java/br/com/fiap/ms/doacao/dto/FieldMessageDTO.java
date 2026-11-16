package br.com.fiap.ms.doacao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Um campo reprovado na validacao e a respectiva mensagem.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FieldMessageDTO {

	private String fieldName;
	private String message;
}
