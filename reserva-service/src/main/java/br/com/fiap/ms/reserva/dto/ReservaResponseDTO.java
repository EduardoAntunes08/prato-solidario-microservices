package br.com.fiap.ms.reserva.dto;

import br.com.fiap.ms.reserva.entity.Reserva;
import br.com.fiap.ms.reserva.entity.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de saida da reserva. A entidade Reserva nunca e exposta nos endpoints.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReservaResponseDTO {

	private Long id;
	private Long doacaoId;
	private String instituicao;
	private String responsavel;
	private LocalDate dataReserva;
	private LocalDate dataRetirada;
	private StatusReserva status;

	public ReservaResponseDTO(Reserva reserva) {
		id = reserva.getId();
		doacaoId = reserva.getDoacaoId();
		instituicao = reserva.getInstituicao();
		responsavel = reserva.getResponsavel();
		dataReserva = reserva.getDataReserva();
		dataRetirada = reserva.getDataRetirada();
		status = reserva.getStatus();
	}
}
