package br.com.fiap.ms.doacao.dto;

import br.com.fiap.ms.doacao.entity.CategoriaAlimento;
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
 * DTO de entrada do cadastro e da atualizacao de uma doacao.
 *
 * O campo status nao e recebido: quem define o status inicial (DISPONIVEL)
 * e o sistema, e as transicoes seguintes acontecem apenas pela comunicacao
 * sincrona vinda do reserva-service.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DoacaoRequestDTO {

	@NotBlank(message = "Campo doador nao pode ser vazio, nulo ou em branco")
	@Size(min = 3, max = 100, message = "O doador deve ter entre 3 e 100 caracteres")
	@Schema(example = "Padaria Pao Quente")
	private String doador;

	@NotBlank(message = "Campo alimento nao pode ser vazio, nulo ou em branco")
	@Size(min = 3, max = 100, message = "O alimento deve ter entre 3 e 100 caracteres")
	@Schema(example = "Paes do dia anterior")
	private String alimento;

	@NotNull(message = "O campo categoria e requerido")
	@Schema(example = "PAES")
	private CategoriaAlimento categoria;

	@NotNull(message = "O campo quantidadeKg e requerido")
	@Positive(message = "O campo quantidadeKg deve ser um numero positivo maior que zero")
	@Schema(example = "12.5")
	private Double quantidadeKg;

	@NotNull(message = "O campo dataValidade e requerido")
	@FutureOrPresent(message = "A dataValidade nao pode estar no passado")
	@Schema(example = "2027-03-15")
	private LocalDate dataValidade;
}
