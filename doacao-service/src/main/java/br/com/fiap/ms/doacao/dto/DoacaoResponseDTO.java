package br.com.fiap.ms.doacao.dto;

import br.com.fiap.ms.doacao.entity.CategoriaAlimento;
import br.com.fiap.ms.doacao.entity.Doacao;
import br.com.fiap.ms.doacao.entity.StatusDoacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de saida de uma doacao. A entidade Doacao nunca e exposta nos endpoints.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DoacaoResponseDTO {

	private Long id;
	private String doador;
	private String alimento;
	private CategoriaAlimento categoria;
	private Double quantidadeKg;
	private LocalDate dataValidade;
	private StatusDoacao status;

	public DoacaoResponseDTO(Doacao doacao) {
		id = doacao.getId();
		doador = doacao.getDoador();
		alimento = doacao.getAlimento();
		categoria = doacao.getCategoria();
		quantidadeKg = doacao.getQuantidadeKg();
		dataValidade = doacao.getDataValidade();
		status = doacao.getStatus();
	}
}
