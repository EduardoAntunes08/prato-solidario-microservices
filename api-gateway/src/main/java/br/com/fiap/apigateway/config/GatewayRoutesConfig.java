package br.com.fiap.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rotas do API Gateway.
 *
 * O destino e declarado como lb://nome-do-servico: o "lb" aciona o
 * Spring Cloud LoadBalancer, que pergunta ao Eureka quais instancias
 * daquele servico estao no ar e escolhe uma. Nenhuma URL fixa e usada.
 *
 * stripPrefix(1) remove o primeiro segmento do caminho antes de repassar:
 * /doacao-service/api/v1/doacoes chega ao microsservico como /api/v1/doacoes.
 *
 * Como o prefixo some, o microsservico nao teria como montar sozinho a URI
 * publica do recurso criado: o header Location do POST sairia apontando para
 * a porta aleatoria da instancia. Quem resolve isso e o X-Forwarded-Prefix,
 * calculado automaticamente pelo gateway a partir do trecho removido aqui e
 * enviado junto com Host, Port e Proto — ver trusted-proxies no
 * application.properties.
 */
@Configuration
public class GatewayRoutesConfig {

	@Bean
	public RouteLocator pratoSolidarioRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("doacao-service", r -> r
						.path("/doacao-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://doacao-service"))
				.route("reserva-service", r -> r
						.path("/reserva-service/**")
						.filters(f -> f.stripPrefix(1))
						.uri("lb://reserva-service"))
				.build();
	}
}
