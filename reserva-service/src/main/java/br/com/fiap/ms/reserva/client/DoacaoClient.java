package br.com.fiap.ms.reserva.client;

import br.com.fiap.ms.reserva.client.dto.DoacaoClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Cliente HTTP declarativo do doacao-service.
 *
 * O destino e o NOME do servico registrado no Eureka, nunca uma URL fixa:
 * o Spring Cloud LoadBalancer resolve o nome para uma das instancias no ar.
 */
@FeignClient("doacao-service")
public interface DoacaoClient {

	@GetMapping("/api/v1/doacoes/{id}")
	DoacaoClientResponse buscarPorId(@PathVariable Long id);

	@PutMapping("/api/v1/doacoes/{id}/reservar")
	DoacaoClientResponse reservar(@PathVariable Long id);

	@PutMapping("/api/v1/doacoes/{id}/retirar")
	DoacaoClientResponse retirar(@PathVariable Long id);

	@PutMapping("/api/v1/doacoes/{id}/liberar")
	DoacaoClientResponse liberar(@PathVariable Long id);
}
