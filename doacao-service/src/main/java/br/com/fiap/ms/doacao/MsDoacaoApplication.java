package br.com.fiap.ms.doacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsDoacaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsDoacaoApplication.class, args);
	}

}
