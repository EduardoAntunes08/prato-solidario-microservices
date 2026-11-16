package br.com.fiap.ms.doacao.controller;

import br.com.fiap.ms.doacao.dto.DoacaoRequestDTO;
import br.com.fiap.ms.doacao.dto.DoacaoResponseDTO;
import br.com.fiap.ms.doacao.service.DoacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doacoes")
@Tag(name = "Doacoes", description = "Cadastro de excedentes de alimentos doados pelos comercios")
public class DoacaoController {

	@Autowired
	private DoacaoService doacaoService;

	@GetMapping
	@Operation(summary = "Lista todas as doacoes")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	public ResponseEntity<List<DoacaoResponseDTO>> findAll() {
		return ResponseEntity.ok(doacaoService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca uma doacao pelo identificador")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Doacao encontrada"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada")
	})
	public ResponseEntity<DoacaoResponseDTO> findById(@PathVariable Long id) {
		return ResponseEntity.ok(doacaoService.findById(id));
	}

	@PostMapping
	@Operation(summary = "Cadastra uma doacao",
			description = "O status inicial DISPONIVEL e definido pelo sistema")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Doacao criada; a URI vai no header Location"),
			@ApiResponse(responseCode = "422", description = "Dados invalidos")
	})
	public ResponseEntity<DoacaoResponseDTO> createDoacao(
			@RequestBody @Valid DoacaoRequestDTO requestDTO) {

		DoacaoResponseDTO doacaoDTO = doacaoService.saveDoacao(requestDTO);

		URI uri = ServletUriComponentsBuilder
				.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(doacaoDTO.getId())
				.toUri();

		return ResponseEntity.created(uri).body(doacaoDTO);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza os dados de uma doacao",
			description = "Nao altera o status: isso so acontece pela comunicacao sincrona")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Doacao atualizada"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "422", description = "Dados invalidos")
	})
	public ResponseEntity<DoacaoResponseDTO> updateDoacao(@PathVariable Long id,
			@RequestBody @Valid DoacaoRequestDTO requestDTO) {

		return ResponseEntity.ok(doacaoService.updateDoacao(id, requestDTO));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Exclui uma doacao")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Doacao excluida"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Doacao RESERVADA nao pode ser excluida")
	})
	public ResponseEntity<Void> deleteDoacao(@PathVariable Long id) {
		doacaoService.deleteDoacao(id);
		return ResponseEntity.noContent().build();
	}

	// ------------------------------------------------------------------
	// Endpoints consumidos pelo reserva-service via OpenFeign
	// ------------------------------------------------------------------

	@PutMapping("/{id}/reservar")
	@Operation(summary = "Marca a doacao como RESERVADA",
			description = "Chamado pelo reserva-service via OpenFeign ao criar uma reserva")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Doacao reservada"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Doacao nao esta DISPONIVEL")
	})
	public ResponseEntity<DoacaoResponseDTO> reservar(@PathVariable Long id) {
		return ResponseEntity.ok(doacaoService.reservar(id));
	}

	@PutMapping("/{id}/retirar")
	@Operation(summary = "Marca a doacao como RETIRADA",
			description = "Chamado pelo reserva-service via OpenFeign ao concluir uma reserva")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Doacao retirada"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Doacao nao esta RESERVADA")
	})
	public ResponseEntity<DoacaoResponseDTO> retirar(@PathVariable Long id) {
		return ResponseEntity.ok(doacaoService.retirar(id));
	}

	@PutMapping("/{id}/liberar")
	@Operation(summary = "Devolve a doacao para DISPONIVEL",
			description = "Chamado pelo reserva-service via OpenFeign ao cancelar uma reserva")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Doacao liberada"),
			@ApiResponse(responseCode = "404", description = "Doacao nao encontrada"),
			@ApiResponse(responseCode = "409", description = "Doacao nao esta RESERVADA")
	})
	public ResponseEntity<DoacaoResponseDTO> liberar(@PathVariable Long id) {
		return ResponseEntity.ok(doacaoService.liberar(id));
	}
}
