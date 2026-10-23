package gestionpedidos.repository;

import gestionpedidos.dto.TerminalMasUtilizadaDto;
import gestionpedidos.model.EstadoPedido;
import gestionpedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByFechaAsc();

    List<Pedido> findByEstadoPedidoOrderByFechaAsc(EstadoPedido estado);

    Optional<Pedido> findByCodigo(String codigo);

    // El """ se usa para escribir texto multilínea sin concatenar ni escapar comillas
    @Query("""
            SELECT new gestionpedidos.dto.TerminalMasUtilizadaDto(p.terminal.nombre, COUNT(p))
            FROM Pedido p
            GROUP BY p.terminal.id, p.terminal.nombre
            ORDER BY COUNT(p) DESC
            """)
    List<TerminalMasUtilizadaDto> obtenerRankingDeTerminales();
    // Aquí en vez de SUM usamos COUNT (cuenta cuántos pedidos cae en cada grupo de terminal)
}
