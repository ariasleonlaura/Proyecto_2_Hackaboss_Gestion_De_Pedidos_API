package gestionpedidos.repository;

import gestionpedidos.dto.TerminalMasUtilizadaDto;
import gestionpedidos.model.Pedido;
import gestionpedidos.model.Terminal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TerminalRepository terminalRepository;

    // *** MÉTODOS FACTORÍA ***

    // Solo con los argumentos extrictamente necesarios para hacer el test
    private Pedido crearPedido(String codigo, Terminal terminal) {
        Pedido pedido = new Pedido();
        pedido.setCodigo(codigo);
        pedido.setTerminal(terminal);
        return pedido;
    }

    // Sin ID, ya que es autogenerado y así el nombre no depende de él
    private Terminal crearTerminal(String nombre) {
        Terminal terminal = new Terminal();
        terminal.setNombre(nombre);
        return terminal;
    }

    // *** TESTS ***

    @Test
    void obtenerRankingDeTerminalesDeberiaOrdenarPorCantidadTotalDescendente() {
        // Arrange
        Terminal terminalMenosUsada = terminalRepository.save(crearTerminal("Terminal 1"));
        Terminal terminalMasUsada = terminalRepository.save(crearTerminal("Terminal 2"));

        // La más usada se crea la segunda (id mayor): si el ORDER BY fallara, el test lo detectaría
        pedidoRepository.save(crearPedido("PED-0001", terminalMenosUsada));
        pedidoRepository.save(crearPedido("PED-0002", terminalMasUsada));
        pedidoRepository.save(crearPedido("PED-0003", terminalMasUsada));

        // Act
        List<TerminalMasUtilizadaDto> resultado = pedidoRepository.obtenerRankingDeTerminales();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNombreTerminal()).isEqualTo(terminalMasUsada.getNombre());
        assertThat(resultado.get(0).getTotalPedidos()).isEqualTo(2L);
        assertThat(resultado.get(1).getNombreTerminal()).isEqualTo(terminalMenosUsada.getNombre());
        assertThat(resultado.get(1).getTotalPedidos()).isEqualTo(1L);
    }
}
