package br.com.fiap.ms.doacao.service;

import br.com.fiap.ms.doacao.dto.DoacaoRequestDTO;
import br.com.fiap.ms.doacao.dto.DoacaoResponseDTO;
import br.com.fiap.ms.doacao.entity.Doacao;
import br.com.fiap.ms.doacao.entity.StatusDoacao;
import br.com.fiap.ms.doacao.exception.DatabaseException;
import br.com.fiap.ms.doacao.exception.ResourceNotFoundException;
import br.com.fiap.ms.doacao.exception.StatusInvalidoException;
import br.com.fiap.ms.doacao.repository.DoacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negocio das doacoes. A controller nunca fala com o repository.
 */
@Service
public class DoacaoService {

	@Autowired
	private DoacaoRepository doacaoRepository;

	@Transactional(readOnly = true)
	public List<DoacaoResponseDTO> findAll() {
		return doacaoRepository.findAll()
				.stream()
				.map(DoacaoResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public DoacaoResponseDTO findById(Long id) {
		Doacao doacao = doacaoRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("Doacao nao encontrada. ID: " + id)
		);
		return new DoacaoResponseDTO(doacao);
	}

	@Transactional
	public DoacaoResponseDTO saveDoacao(DoacaoRequestDTO requestDTO) {
		Doacao doacao = new Doacao();
		copyDtoToDoacao(requestDTO, doacao);
		// o status inicial e definido pelo sistema, nunca recebido no DTO de entrada
		doacao.setStatus(StatusDoacao.DISPONIVEL);
		doacao = doacaoRepository.save(doacao);
		return new DoacaoResponseDTO(doacao);
	}

	@Transactional
	public DoacaoResponseDTO updateDoacao(Long id, DoacaoRequestDTO requestDTO) {
		try {
			Doacao doacao = doacaoRepository.getReferenceById(id);
			// o status nao e alterado por aqui: so pela comunicacao sincrona
			copyDtoToDoacao(requestDTO, doacao);
			doacao = doacaoRepository.save(doacao);
			return new DoacaoResponseDTO(doacao);
		} catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Doacao nao encontrada. ID: " + id);
		}
	}

	@Transactional
	public void deleteDoacao(Long id) {
		Doacao doacao = doacaoRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("Doacao nao encontrada. ID: " + id)
		);

		if (doacao.getStatus() == StatusDoacao.RESERVADA) {
			throw new StatusInvalidoException(
					"Doacao id " + id + " esta RESERVADA e nao pode ser excluida");
		}

		try {
			doacaoRepository.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Falha de integridade referencial ao excluir a doacao. ID: " + id);
		}
	}

	// ------------------------------------------------------------------
	// Transicoes de status — alvos da comunicacao sincrona (OpenFeign)
	// ------------------------------------------------------------------

	@Transactional
	public DoacaoResponseDTO reservar(Long id) {
		return alterarStatus(id, StatusDoacao.DISPONIVEL, StatusDoacao.RESERVADA);
	}

	@Transactional
	public DoacaoResponseDTO retirar(Long id) {
		return alterarStatus(id, StatusDoacao.RESERVADA, StatusDoacao.RETIRADA);
	}

	@Transactional
	public DoacaoResponseDTO liberar(Long id) {
		return alterarStatus(id, StatusDoacao.RESERVADA, StatusDoacao.DISPONIVEL);
	}

	private DoacaoResponseDTO alterarStatus(Long id, StatusDoacao esperado, StatusDoacao novo) {
		Doacao doacao = doacaoRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("Doacao nao encontrada. ID: " + id)
		);

		if (doacao.getStatus() != esperado) {
			throw new StatusInvalidoException(String.format(
					"Doacao id %d esta %s e nao pode mudar para %s (era esperado %s)",
					id, doacao.getStatus(), novo, esperado));
		}

		doacao.setStatus(novo);
		doacao = doacaoRepository.save(doacao);
		return new DoacaoResponseDTO(doacao);
	}

	private void copyDtoToDoacao(DoacaoRequestDTO requestDTO, Doacao doacao) {
		doacao.setDoador(requestDTO.getDoador());
		doacao.setAlimento(requestDTO.getAlimento());
		doacao.setCategoria(requestDTO.getCategoria());
		doacao.setQuantidadeKg(requestDTO.getQuantidadeKg());
		doacao.setDataValidade(requestDTO.getDataValidade());
	}
}
