package br.com.fiap.ms.reserva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tb_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Referencia logica a doacao no doacao-service.
	 * Nao existe chave estrangeira: cada microsservico tem banco proprio
	 * e independente, sem schema ou tabela compartilhada.
	 */
	@Column(name = "doacao_id", nullable = false)
	private Long doacaoId;

	@Column(nullable = false, length = 100)
	private String instituicao;

	@Column(nullable = false, length = 100)
	private String responsavel;

	@Column(name = "data_reserva", nullable = false)
	private LocalDate dataReserva;

	@Column(name = "data_retirada", nullable = false)
	private LocalDate dataRetirada;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StatusReserva status;
}
