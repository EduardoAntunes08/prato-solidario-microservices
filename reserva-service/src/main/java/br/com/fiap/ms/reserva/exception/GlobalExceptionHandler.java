package br.com.fiap.ms.reserva.exception;

import br.com.fiap.ms.reserva.dto.CustomErrorDTO;
import br.com.fiap.ms.reserva.dto.ValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<CustomErrorDTO> handleResourceNotFound(ResourceNotFoundException e,
			HttpServletRequest request) {

		return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
	}

	@ExceptionHandler(DoacaoIndisponivelException.class)
	public ResponseEntity<CustomErrorDTO> handleDoacaoIndisponivel(DoacaoIndisponivelException e,
			HttpServletRequest request) {

		return build(HttpStatus.CONFLICT, e.getMessage(), request);
	}

	@ExceptionHandler(ReservaFinalizadaException.class)
	public ResponseEntity<CustomErrorDTO> handleReservaFinalizada(ReservaFinalizadaException e,
			HttpServletRequest request) {

		return build(HttpStatus.CONFLICT, e.getMessage(), request);
	}

	@ExceptionHandler(ReservaAtivaException.class)
	public ResponseEntity<CustomErrorDTO> handleReservaAtiva(ReservaAtivaException e,
			HttpServletRequest request) {

		return build(HttpStatus.CONFLICT, e.getMessage(), request);
	}

	@ExceptionHandler(ComunicacaoException.class)
	public ResponseEntity<CustomErrorDTO> handleComunicacao(ComunicacaoException e,
			HttpServletRequest request) {

		return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomErrorDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpServletRequest request) {

		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
		ValidationErrorDTO err = new ValidationErrorDTO(Instant.now(), status.value(),
				"Dados invalidos", request.getRequestURI());

		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			err.addError(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return ResponseEntity.status(status).body(err);
	}

	private ResponseEntity<CustomErrorDTO> build(HttpStatus status, String message,
			HttpServletRequest request) {

		CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
				message, request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}
}
