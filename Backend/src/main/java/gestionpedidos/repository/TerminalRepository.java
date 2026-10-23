package gestionpedidos.repository;

import gestionpedidos.model.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    boolean existsByNombre(String nombre);
}
