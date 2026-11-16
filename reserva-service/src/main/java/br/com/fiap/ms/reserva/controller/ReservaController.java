package br.com.fiap.ms.reserva.controller;

import br.com.fiap.ms.reserva.dto.ReservaRequestDTO;
import br.com.fiap.ms.reserva.dto.ReservaResponseDTO;
import br.com.fiap.ms.reserva.exception.ComunicacaoException;
import br.com.fiap.ms.reserva.exception.DoacaoIndisponivelException;
import br.com.fiap.ms.reserva.exception.ReservaAtivaException;
import br.com.fiap.ms.reserva.exception.ReservaFinalizadaException;
import br.com.fiap.ms.reserva.exception.ResourceNotFoundException;
import br.com.fiap.ms.reserva.service.ReservaService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas", description = "Reservas de doacoes feitas pelas instituicoes")
public class ReservaController {

	@Autowired
	private ReservaService reservaService;

	@GetMapping
	@Operation(summary = "Lista todas as reservas")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	public ResponseEntity<List<ReservaResponseDTO>> findAll() {
		return ResponseEntity.ok(reservaService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma reserva pelo identificador")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Reserva encontrada"),
			@ApiResponse(responseCode = "404", description = "Reserva nao encontrada")
	})
	public ResponseEntity<ReservaResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(reservaService.findById(id));
	}

	@PostMapping
	@CircuitBreaker(name = "atualizarDoacao", fallbackMethod = "fallbackCreateReserva")
	@Operation(summary = "Cria uma reserva",
			description = "Confere a doacao no doacao-service e a marca como RESERVADA "
					+ "pela comunicacao sincrona (OpenFeign)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Reserva criada; a URI vai no header Location"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Doacao nao esta DISPONIVEL"),
			@ApiResponse(responseCode = "422", description = "Dados invalidos"),
			@ApiResponse(responseCode = "503", description = "doacao-service indisponivel")
	})
	public ResponseEntity<ReservaResponseDTO> createReserva(
			@RequestBody @Valid ReservaRequestDTO requestDTO) {

		ReservaResponseDTO reservaDTO = reservaService.saveReserva(requestDTO);

		URI uri = ServletUriComponentsBuilder
				.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(reservaDTO.getId())
				.toUri();

		return ResponseEntity.created(uri).body(reservaDTO);
	}

	/**
	 * Fallback da criacao. Aqui a reserva ainda nao existe, entao nao ha o que
	 * marcar como pendente: a transacao sofreu rollback e nada foi gravado.
	 * Devolver 503 e honesto — nao da para prometer uma reserva sem conseguir
	 * reservar a doacao do outro lado.
	 */
	public ResponseEntity<ReservaResponseDTO> fallbackCreateReserva(ReservaRequestDTO requestDTO,
			Throwable e) {

		repassarSeErroDeNegocio(e);

		log.error("Falha ao criar reserva da doacao {}. Ativando fallback. Erro: {}",
				requestDTO.getDoacaoId(), e.getMessage());

		throw new ComunicacaoException("doacao-service indisponivel: a reserva da doacao "
				+ requestDTO.getDoacaoId() + " nao foi criada. Tente novamente em instantes.");
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza os dados de uma reserva",
			description = "Nao altera doacaoId nem status")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Reserva atualizada"),
			@ApiResponse(responseCode = "404", description = "Reserva nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Reserva ja concluida ou cancelada"),
			@ApiResponse(responseCode = "422", description = "Dados invalidos")
	})
	public ResponseEntity<ReservaResponseDTO> updateReserva(@PathVariable Long id,
			@RequestBody @Valid ReservaRequestDTO requestDTO) {

		return ResponseEntity.ok(reservaService.updateReserva(id, requestDTO));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Exclui uma reserva",
			description = "Reserva ATIVA precisa ser cancelada antes, para liberar a doacao")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Reserva excluida"),
			@ApiResponse(responseCode = "404", description = "Reserva nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Reserva ATIVA: cancele antes de excluir")
	})
	public ResponseEntity<Void> deleteReserva(@PathVariable Long id) {
		reservaService.deleteReserva(id);
		return ResponseEntity.noContent().build();
	}

	// ------------------------------------------------------------------
	// Alteracao de status por comunicacao sincrona, protegida por
	// Circuit Breaker + Fallback
	// ------------------------------------------------------------------

	@PatchMapping("/{id}/concluir")
	@CircuitBreaker(name = "atualizarDoacao", fallbackMethod = "fallbackConcluirPendente")
	@Operation(summary = "Conclui a reserva",
			description = "Marca a reserva como CONCLUIDA e manda o doacao-service "
					+ "marcar a doacao como RETIRADA")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Reserva concluida"),
			@ApiResponse(responseCode = "404", description = "Reserva ou doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Reserva ja concluida ou cancelada"),
			@ApiResponse(responseCode = "503", description = "doacao-service indisponivel: "
					+ "reserva marcada como PENDENTE_SINCRONIZACAO")
	})
	public ResponseEntity<ReservaResponseDTO> concluirReserva(@PathVariable Long id) {
		return ResponseEntity.ok(reservaService.concluirReserva(id));
	}

	public ResponseEntity<ReservaResponseDTO> fallbackConcluirPendente(Long id, Throwable e) {
		repassarSeErroDeNegocio(e);

		log.error("Falha ao concluir reserva {}. Ativando fallback. Erro: {}", id, e.getMessage());

		ReservaResponseDTO dto = reservaService.marcarPendenteSincronizacao(id);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(dto);
	}

	@PatchMapping("/{id}/cancelar")
	@CircuitBreaker(name = "atualizarDoacao", fallbackMethod = "fallbackCancelarPendente")
	@Operation(summary = "Cancela a reserva",
			description = "Marca a reserva como CANCELADA e manda o doacao-service "
					+ "devolver a doacao para DISPONIVEL")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Reserva cancelada"),
			@ApiResponse(responseCode = "404", description = "Reserva ou doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Reserva ja concluida ou cancelada"),
			@ApiResponse(responseCode = "503", description = "doacao-service indisponivel: "
					+ "reserva marcada como PENDENTE_SINCRONIZACAO")
	})
	public ResponseEntity<ReservaResponseDTO> cancelarReserva(@PathVariable Long id) {
		return ResponseEntity.ok(reservaService.cancelarReserva(id));
	}

	public ResponseEntity<ReservaResponseDTO> fallbackCancelarPendente(Long id, Throwable e) {
		repassarSeErroDeNegocio(e);

		log.error("Falha ao cancelar reserva {}. Ativando fallback. Erro: {}", id, e.getMessage());

		ReservaResponseDTO dto = reservaService.marcarPendenteSincronizacao(id);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(dto);
	}

	/**
	 * O Resilience4j chama o fallbackMethod para QUALQUER excecao: a lista
	 * ignore-exceptions so evita que o erro conte para abrir o circuito, nao
	 * evita o fallback. Sem este repasse, um 404 ou um 409 de negocio seria
	 * engolido pelo fallback e devolvido como 503.
	 */
	private void repassarSeErroDeNegocio(Throwable e) {
		switch (e) {
			case ResourceNotFoundException ex -> throw ex;
			case DoacaoIndisponivelException ex -> throw ex;
			case ReservaFinalizadaException ex -> throw ex;
			case ReservaAtivaException ex -> throw ex;
			default -> {
				// falha de comunicacao: segue para o fallback
			}
		}
	}
}
