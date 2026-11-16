package br.com.fiap.ms.reserva.service;

import br.com.fiap.ms.reserva.client.DoacaoClient;
import br.com.fiap.ms.reserva.client.dto.DoacaoClientResponse;
import br.com.fiap.ms.reserva.dto.ReservaRequestDTO;
import br.com.fiap.ms.reserva.dto.ReservaResponseDTO;
import br.com.fiap.ms.reserva.entity.Reserva;
import br.com.fiap.ms.reserva.entity.StatusReserva;
import br.com.fiap.ms.reserva.exception.ComunicacaoException;
import br.com.fiap.ms.reserva.exception.DoacaoIndisponivelException;
import br.com.fiap.ms.reserva.exception.ReservaAtivaException;
import br.com.fiap.ms.reserva.exception.ReservaFinalizadaException;
import br.com.fiap.ms.reserva.exception.ResourceNotFoundException;
import br.com.fiap.ms.reserva.repository.ReservaRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Regras de negocio das reservas, incluindo a comunicacao sincrona com o
 * doacao-service. A controller nunca fala com o repository nem com o client.
 */
@Service
public class ReservaService {

	private static final String STATUS_DISPONIVEL = "DISPONIVEL";

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private DoacaoClient doacaoClient;

	@Transactional(readOnly = true)
	public List<ReservaResponseDTO> findAll() {
		return reservaRepository.findAll()
				.stream()
				.map(ReservaResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public ReservaResponseDTO findById(Long id) {
		return new ReservaResponseDTO(buscarReserva(id));
	}

	/**
	 * Cria a reserva e, pela comunicacao sincrona, marca a doacao como RESERVADA.
	 * A doacao e conferida antes: so uma doacao DISPONIVEL pode ser reservada.
	 */
	@Transactional
	public ReservaResponseDTO saveReserva(ReservaRequestDTO requestDTO) {
		Long doacaoId = requestDTO.getDoacaoId();

		DoacaoClientResponse doacao = chamarDoacaoService(
				() -> doacaoClient.buscarPorId(doacaoId), doacaoId);

		if (!STATUS_DISPONIVEL.equals(doacao.getStatus())) {
			throw new DoacaoIndisponivelException(String.format(
					"Doacao id %d esta %s e nao pode ser reservada", doacaoId, doacao.getStatus()));
		}

		Reserva reserva = new Reserva();
		copyDtoToReserva(requestDTO, reserva);
		// data e status iniciais sao definidos pelo sistema, nunca recebidos no DTO
		reserva.setDataReserva(LocalDate.now());
		reserva.setStatus(StatusReserva.ATIVA);
		reserva = reservaRepository.save(reserva);

		chamarDoacaoService(() -> doacaoClient.reservar(doacaoId), doacaoId);

		return new ReservaResponseDTO(reserva);
	}

	@Transactional
	public ReservaResponseDTO updateReserva(Long id, ReservaRequestDTO requestDTO) {
		try {
			Reserva reserva = reservaRepository.getReferenceById(id);

			if (reserva.getStatus() != StatusReserva.ATIVA) {
				throw new ReservaFinalizadaException(String.format(
						"Reserva id %d esta %s e nao pode ser alterada", id, reserva.getStatus()));
			}

			// doacaoId e status nao sao alterados por aqui
			reserva.setInstituicao(requestDTO.getInstituicao());
			reserva.setResponsavel(requestDTO.getResponsavel());
			reserva.setDataRetirada(requestDTO.getDataRetirada());

			reserva = reservaRepository.save(reserva);
			return new ReservaResponseDTO(reserva);
		} catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Reserva nao encontrada. ID: " + id);
		}
	}

	/**
	 * Exclui a reserva. Uma reserva ATIVA segura uma doacao do outro lado, entao
	 * precisa ser cancelada antes — assim a doacao volta a ficar DISPONIVEL e os
	 * dois microsservicos continuam coerentes.
	 */
	@Transactional
	public void deleteReserva(Long id) {
		Reserva reserva = buscarReserva(id);

		if (reserva.getStatus() == StatusReserva.ATIVA
				|| reserva.getStatus() == StatusReserva.PENDENTE_SINCRONIZACAO) {
			throw new ReservaAtivaException(String.format(
					"Reserva id %d esta %s e ainda segura uma doacao. Cancele a reserva "
							+ "antes de exclui-la, para que a doacao volte a ficar DISPONIVEL",
					id, reserva.getStatus()));
		}

		reservaRepository.deleteById(reserva.getId());
	}

	// ------------------------------------------------------------------
	// Transicoes de status — disparam a comunicacao sincrona
	// ------------------------------------------------------------------

	/**
	 * Conclui a reserva: altera o proprio status para CONCLUIDA e manda o
	 * doacao-service marcar a doacao como RETIRADA.
	 */
	@Transactional
	public ReservaResponseDTO concluirReserva(Long id) {
		Reserva reserva = buscarReservaEmAberto(id);
		final Long doacaoId = reserva.getDoacaoId();

		reserva.setStatus(StatusReserva.CONCLUIDA);
		reserva = reservaRepository.save(reserva);

		chamarDoacaoService(() -> doacaoClient.retirar(doacaoId), doacaoId);

		return new ReservaResponseDTO(reserva);
	}

	/**
	 * Cancela a reserva: altera o proprio status para CANCELADA e manda o
	 * doacao-service devolver a doacao para DISPONIVEL.
	 */
	@Transactional
	public ReservaResponseDTO cancelarReserva(Long id) {
		Reserva reserva = buscarReservaEmAberto(id);
		final Long doacaoId = reserva.getDoacaoId();

		reserva.setStatus(StatusReserva.CANCELADA);
		reserva = reservaRepository.save(reserva);

		chamarDoacaoService(() -> doacaoClient.liberar(doacaoId), doacaoId);

		return new ReservaResponseDTO(reserva);
	}

	/**
	 * Usado pelo fallback do Circuit Breaker: o doacao-service esta fora do ar,
	 * entao a reserva fica marcada para reconciliacao posterior.
	 *
	 * Roda em transacao propria, depois que a transacao original sofreu rollback.
	 */
	@Transactional
	public ReservaResponseDTO marcarPendenteSincronizacao(Long id) {
		Reserva reserva = buscarReserva(id);
		reserva.setStatus(StatusReserva.PENDENTE_SINCRONIZACAO);
		reserva = reservaRepository.save(reserva);
		return new ReservaResponseDTO(reserva);
	}

	// ------------------------------------------------------------------
	// Apoio
	// ------------------------------------------------------------------

	private Reserva buscarReserva(Long id) {
		return reservaRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("Reserva nao encontrada. ID: " + id)
		);
	}

	/**
	 * Reserva que ainda admite transicao: ATIVA, ou PENDENTE_SINCRONIZACAO.
	 *
	 * O estado pendente precisa continuar aceitando concluir/cancelar — e
	 * justamente assim que a reserva deixada em limbo pelo fallback volta a
	 * ficar coerente quando o doacao-service retorna. Sem isso, o estado de
	 * reconciliacao nunca poderia ser reconciliado.
	 */
	private Reserva buscarReservaEmAberto(Long id) {
		Reserva reserva = buscarReserva(id);

		if (reserva.getStatus() != StatusReserva.ATIVA
				&& reserva.getStatus() != StatusReserva.PENDENTE_SINCRONIZACAO) {
			throw new ReservaFinalizadaException(String.format(
					"Reserva id %d esta %s e nao aceita nova transicao", id, reserva.getStatus()));
		}

		return reserva;
	}

	/**
	 * Traduz as falhas do OpenFeign:
	 *   404 do doacao-service  -> ResourceNotFoundException   (404 aqui)
	 *   409 do doacao-service  -> DoacaoIndisponivelException  (409 aqui)
	 *   qualquer outra falha   -> ComunicacaoException         (503, conta para o Circuit Breaker)
	 */
	private DoacaoClientResponse chamarDoacaoService(Supplier<DoacaoClientResponse> chamada, Long doacaoId) {
		try {
			return chamada.get();
		} catch (FeignException.NotFound e) {
			throw new ResourceNotFoundException("Doacao nao encontrada. ID: " + doacaoId);
		} catch (FeignException.Conflict e) {
			throw new DoacaoIndisponivelException(
					"O doacao-service recusou a transicao da doacao ID: " + doacaoId);
		} catch (Exception e) {
			throw new ComunicacaoException(
					"Falha ao comunicar com doacao-service: " + e.getMessage(), e);
		}
	}

	private void copyDtoToReserva(ReservaRequestDTO requestDTO, Reserva reserva) {
		reserva.setDoacaoId(requestDTO.getDoacaoId());
		reserva.setInstituicao(requestDTO.getInstituicao());
		reserva.setResponsavel(requestDTO.getResponsavel());
		reserva.setDataRetirada(requestDTO.getDataRetirada());
	}
}
