package br.com.fiap.ms.doacao.repository;

import br.com.fiap.ms.doacao.entity.Doacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {
}
