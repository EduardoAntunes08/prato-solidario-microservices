package br.com.fiap.ms.reserva.repository;

import br.com.fiap.ms.reserva.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}
