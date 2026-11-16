package br.com.fiap.ms.doacao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Corpo padrao de erro da API.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CustomErrorDTO {

	private Instant timestamp;
	private Integer status;
	private String error;
	private String path;
}
