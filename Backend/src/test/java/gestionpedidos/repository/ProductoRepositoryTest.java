package gestionpedidos.repository;

import gestionpedidos.dto.ProductoMasVendidoDto;
import gestionpedidos.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TerminalRepository terminalRepository;

    // *** MÉTODOS FACTORÍA ***

    // Solo con los argumentos estrictamente necesarios para hacer el test y los obligatorios aunque la query no los use
    private Categoria crearCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categoria;
    }

    private Pedido crearPedido(String codigo, Terminal terminal) {
        Pedido pedido = new Pedido();
        pedido.setCodigo(codigo);
        pedido.setTerminal(terminal);
        pedido.setLineasPedido(new ArrayList<>()); // mutable: las líneas se añaden a esta misma lista
        return pedido;
    }

    private Producto crearProducto(String nombre, String precio, boolean activo, Categoria categoria) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setActivo(activo);
        producto.setCategoria(categoria);
        return producto;
    }

    private Terminal crearTerminal(String nombre) {
        Terminal terminal = new Terminal();
        terminal.setNombre(nombre);
        return terminal;
    }

    // Rellena los DOS lados de la relación: la línea apunta al pedido (lado dueño de la FK) y el pedido contiene la línea (activa la cascada)
    private void agregarLineaPedido(Pedido pedido, Producto producto, int cantidad) {
        PedidoProducto linea = new PedidoProducto();
        linea.setPedido(pedido);
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(producto.getPrecio()); // obligatorio (nullable = false), aunque la query no lo use
        pedido.getLineasPedido().add(linea);
    }

    // *** TESTS ***

    @Test
    void obtenerProductosMasVendidosDeberiaOrdenarPorCantidadTotalDescendente() {
        // Arrange: aquí SÍ hay que crear y persistir datos reales, no mocks
        Categoria categoriaHamburguesas = categoriaRepository.save(crearCategoria("Hamburguesas"));
        Categoria categoriaPatatas = categoriaRepository.save(crearCategoria("Patatas"));

        /* El menos vendido se guarda primero y el más vendido después (id mayor):
        si el ORDER BY fallara, el orden natural por id no coincidiría con el esperado y el test lo detectaría */
        Producto patatasFritas = productoRepository.save(crearProducto("Patatas fritas", "3.00", true, categoriaPatatas));
        Producto hamburguesaClasica = productoRepository.save(crearProducto("Hamburguesa clásica", "8.50", true, categoriaHamburguesas));

        Terminal terminal = terminalRepository.save(crearTerminal("Terminal 1"));

        /* La hamburguesa aparece en dos pedidos (3 + 2 = 5) para que el SUM agregue de verdad
        varias líneas del mismo producto y no una sola */
        Pedido pedido1 = crearPedido("PED-0001", terminal);
        agregarLineaPedido(pedido1, hamburguesaClasica, 3);
        agregarLineaPedido(pedido1, patatasFritas, 1);

        Pedido pedido2 = crearPedido("PED-0002", terminal);
        agregarLineaPedido(pedido2, hamburguesaClasica, 2);

        // Un único save por pedido, con las líneas ya dentro: la cascada las guarda
        pedidoRepository.save(pedido1);
        pedidoRepository.save(pedido2);

        // Act: llamamos al método REAL, no mockeado
        List<ProductoMasVendidoDto> resultado = productoRepository.obtenerProductosMasVendidos();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getProductoId()).isEqualTo(hamburguesaClasica.getId());
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo(hamburguesaClasica.getNombre());
        assertThat(resultado.get(0).getTotalVendido()).isEqualTo(5L);
        assertThat(resultado.get(1).getProductoId()).isEqualTo(patatasFritas.getId());
        assertThat(resultado.get(1).getNombreProducto()).isEqualTo(patatasFritas.getNombre());
        assertThat(resultado.get(1).getTotalVendido()).isEqualTo(1L);
    }

    // El ranking agrupa por id, no por nombre: dos productos con el mismo nombre salen separados
    @Test
    void obtenerProductosMasVendidosDeberiaSepararProductosConElMismoNombre() {
        Categoria categoria = categoriaRepository.save(crearCategoria("Hamburguesas"));
        Producto menosVendido = productoRepository.save(crearProducto("Hamburguesa clásica", "9.00", true, categoria));
        Producto masVendido = productoRepository.save(crearProducto("Hamburguesa clásica", "8.50", true, categoria));
        Terminal terminal = terminalRepository.save(crearTerminal("Terminal 1"));

        Pedido pedido = crearPedido("PED-0001", terminal);
        agregarLineaPedido(pedido, menosVendido, 1);
        agregarLineaPedido(pedido, masVendido, 3);
        pedidoRepository.save(pedido);

        List<ProductoMasVendidoDto> resultado = productoRepository.obtenerProductosMasVendidos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getProductoId()).isEqualTo(masVendido.getId());
        assertThat(resultado.get(0).getTotalVendido()).isEqualTo(3L);
        assertThat(resultado.get(1).getProductoId()).isEqualTo(menosVendido.getId());
        assertThat(resultado.get(1).getTotalVendido()).isEqualTo(1L);
    }
}