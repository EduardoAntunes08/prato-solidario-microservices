package br.com.fiap.ms.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de entrada da reserva.
 *
 * Nao recebe status nem dataReserva: o status inicial (ATIVA) e a data da
 * reserva sao definidos pelo sistema.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReservaRequestDTO {

	@NotNull(message = "O campo doacaoId e requerido")
	@Positive(message = "O campo doacaoId deve ser um numero positivo maior que zero")
	@Schema(example = "3")
	private Long doacaoId;

	@NotBlank(message = "Campo instituicao nao pode ser vazio, nulo ou em branco")
	@Size(min = 3, max = 100, message = "A instituicao deve ter entre 3 e 100 caracteres")
	@Schema(example = "ONG Mesa Cheia")
	private String instituicao;

	@NotBlank(message = "Campo responsavel nao pode ser vazio, nulo ou em branco")
	@Size(min = 3, max = 100, message = "O responsavel deve ter entre 3 e 100 caracteres")
	@Schema(example = "Maria Oliveira")
	private String responsavel;

	@NotNull(message = "O campo dataRetirada e requerido")
	@FutureOrPresent(message = "A dataRetirada nao pode estar no passado")
	@Schema(example = "2027-01-15")
	private LocalDate dataRetirada;
}
