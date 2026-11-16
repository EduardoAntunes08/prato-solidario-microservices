package br.com.fiap.ms.reserva.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Representacao da doacao como ela chega do doacao-service.
 * Fica no pacote do client porque e um contrato externo, nao um DTO desta API.
 */
@Getter
@Setter
@NoArgsConstructor
public class DoacaoClientResponse {

	private Long id;
	private String doador;
	private String alimento;
	private String categoria;
	private Double quantidadeKg;
	private LocalDate dataValidade;
	private String status;
}
