package gestionpedidos.service;

import gestionpedidos.dto.CrearPedidoDto;
import gestionpedidos.dto.PedidoDto;
import gestionpedidos.dto.PedidoProductoRequestDto;
import gestionpedidos.dto.ProductosPedidoDto;
import gestionpedidos.exception.BadRequestException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.*;
import gestionpedidos.repository.PedidoRepository;
import gestionpedidos.repository.ProductoRepository;
import gestionpedidos.repository.TerminalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private TerminalRepository terminalRepository;

    @InjectMocks
    private PedidoService pedidoService;

    // *** MÉTODOS FACTORÍA — devuelven una instancia nueva cada vez, nunca comparten referencia entre tests ***

    private Terminal crearTerminal(Long id) {
        Terminal terminal = new Terminal();
        terminal.setId(id);
        terminal.setNombre("Terminal " + id);
        return terminal;
    }

    private Producto crearProducto(Long id, String nombre, String precio, boolean activo) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setActivo(activo);
        return producto;
    }

    private Pedido crearPedido(Long id, String codigo, EstadoPedido estado, String total, List<PedidoProducto> lineas) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setCodigo(codigo);
        pedido.setEstadoPedido(estado);
        pedido.setTotal(new BigDecimal(total));
        pedido.setLineasPedido(new ArrayList<>(lineas)); // mutable por defecto, se puede rellenar después si el test lo necesita
        return pedido;
    }

    private PedidoProducto crearLineaPedido(Producto producto, int cantidad) {
        PedidoProducto linea = new PedidoProducto();
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(producto.getPrecio());
        return linea;
    }

    private CrearPedidoDto crearPedidoDtoConUnProducto(Long terminalId, Long productoId, int cantidad) {
        Map<Long, Integer> productosComprados = new HashMap<>();
        productosComprados.put(productoId, cantidad);

        CrearPedidoDto dto = new CrearPedidoDto();
        dto.setTerminalId(terminalId);
        dto.setProductosComprados(productosComprados);
        return dto;
    }

    private PedidoProductoRequestDto crearPedidoProductoRequestDto(Long productoId, int cantidad) {
        return new PedidoProductoRequestDto(productoId, cantidad);
    }

    // *** TESTS ***

    // Test de listarPedidos sin filtrar
    @Test
    void listarPedidosDeberiaDevolverTodosLosPedidosSinFiltrar() {
        // Arrange
        Pedido pedido1 = crearPedido(1L, "PED-0001", EstadoPedido.PREPARACION, "15.50", List.of());
        pedido1.setFecha(LocalDateTime.of(2026, 8, 8, 12, 0));
        pedido1.setTerminal(crearTerminal(1L));

        // Dos pedidos para comprobar que el método mapea correctamente una lista completa, no solo un elemento
        Pedido pedido2 = crearPedido(2L, "PED-0002", EstadoPedido.CREADO, "15.50", List.of());
        pedido2.setFecha(LocalDateTime.of(2026, 8, 9, 12, 0));
        pedido2.setTerminal(pedido1.getTerminal());

        when(pedidoRepository.findAllByOrderByFechaAsc()).thenReturn(List.of(pedido1, pedido2));

        // Act
        List<PedidoDto> resultado = pedidoService.listarPedidos(null);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCodigo()).isEqualTo(pedido1.getCodigo());
        assertThat(resultado.get(1).getCodigo()).isEqualTo(pedido2.getCodigo());

        verify(pedidoRepository).findAllByOrderByFechaAsc();
        verify(pedidoRepository, never()).findByEstadoPedidoOrderByFechaAsc(any());
    }

    // Test de listarPedidos filtrados por el valor de "estado"
    @Test
    void listarPedidosDeberiaFiltrarPorEstado() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.PREPARACION, "15.50", List.of());
        pedido.setFecha(LocalDateTime.of(2026, 8, 8, 12, 0));
        pedido.setTerminal(crearTerminal(1L));

        when(pedidoRepository.findByEstadoPedidoOrderByFechaAsc(pedido.getEstadoPedido()))
                .thenReturn(List.of(pedido));

        List<PedidoDto> resultado = pedidoService.listarPedidos(pedido.getEstadoPedido());

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado).hasSize(1);
        // get(0) -> primer elemento de la lista. Luego seguimos con getCodigo (atributo)
        assertThat(resultado.get(0).getCodigo()).isEqualTo(pedido.getCodigo());
        assertThat(resultado.get(0).getEstado()).isEqualTo(pedido.getEstadoPedido().name());

        verify(pedidoRepository).findByEstadoPedidoOrderByFechaAsc(pedido.getEstadoPedido());
        verify(pedidoRepository, never()).findAllByOrderByFechaAsc();
    }

    // Test caso de éxito de registrarPedido
    @Test
    void registrarPedidoDeberiaGuardarlo() {
        Long productoId = 10L;
        CrearPedidoDto crearPedidoDto = crearPedidoDtoConUnProducto(1L, productoId, 2);

        Terminal terminal = crearTerminal(crearPedidoDto.getTerminalId());
        Producto producto = crearProducto(productoId, "Hamburguesa clásica", "8.50", true);

        when(terminalRepository.findById(terminal.getId())).thenReturn(Optional.of(terminal));
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        PedidoDto resultado = pedidoService.registrarPedido(crearPedidoDto);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado.getTerminalId()).isEqualTo(crearPedidoDto.getTerminalId());
        assertThat(resultado.getEstado()).isEqualTo("CREADO");
        assertThat(resultado.getTotal()).isEqualByComparingTo("17.00"); // 8.50 * 2
        assertThat(resultado.getCodigo()).startsWith("PED-");
        verify(pedidoRepository).save(any(Pedido.class));
    }

    // Test caso de error de registrarPedido. Terminal inexistente
    @Test
    void registrarPedidoDeberiaLanzarExcepcionCuandoTerminalNoExiste() {
        CrearPedidoDto crearPedidoDto = crearPedidoDtoConUnProducto(1L, 10L, 2);

        when(terminalRepository.findById(crearPedidoDto.getTerminalId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.registrarPedido(crearPedidoDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("La terminal con ID " + crearPedidoDto.getTerminalId() + " no existe");

        // Aquí falla en terminalRepository, no en pedidoRepository
        // System.out.println(mockingDetails(terminalRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de registrarPedido. Producto no encontrado
    @Test
    void registrarPedidoDeberiaLanzarExcepcionCuandoProductoNoSeEncuentra() {
        Long productoId = 10L;
        CrearPedidoDto crearPedidoDto = crearPedidoDtoConUnProducto(1L, productoId, 2);
        Terminal terminal = crearTerminal(crearPedidoDto.getTerminalId());

        when(terminalRepository.findById(terminal.getId())).thenReturn(Optional.of(terminal));
        when(productoRepository.findById(productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.registrarPedido(crearPedidoDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto con ID " + productoId + " no encontrado");

        // Aquí falla en productoRepository, no en pedidoRepository
        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de registrarPedido. Producto inactivo
    @Test
    void registrarPedidoDeberiaLanzarExcepcionCuandoProductoEstaInactivo() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", false);
        CrearPedidoDto crearPedidoDto = crearPedidoDtoConUnProducto(1L, producto.getId(), 2);
        Terminal terminal = crearTerminal(crearPedidoDto.getTerminalId());

        when(terminalRepository.findById(terminal.getId())).thenReturn(Optional.of(terminal));
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.registrarPedido(crearPedidoDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("El producto " + producto.getNombre() + " no está activo");

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de éxito de agregarProductosAPedido. Suma a la cantidad existente de producto
    @Test
    void agregarProductosAPedidoDeberiaAgregarloSumandoloALaCantidadAnteriorSiYaExiste() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", true);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        pedido.getLineasPedido().add(crearLineaPedido(producto, 1)); // ya había 1 unidad en el pedido

        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        ProductosPedidoDto resultado = pedidoService.agregarProductosAPedido(pedido.getId(), dto);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado.getProductoId()).isEqualTo(dto.getProductoId());
        assertThat(resultado.getCantidad()).isEqualTo(3); // 1 (ya existía) + 2 (se añade)
        assertThat(resultado.getSubtotal()).isEqualByComparingTo("25.50");
        verify(pedidoRepository).save(any(Pedido.class));
    }

    // Test caso de éxito de agregarProductosAPedido. Nueva línea de producto
    @Test
    void agregarProductosAPedidoDeberiaAgregarlosCreandoUnaNuevaLineaDeProductoSiNoExiste() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", true);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "0.00", List.of()); // sin líneas todavía

        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        ProductosPedidoDto resultado = pedidoService.agregarProductosAPedido(pedido.getId(), dto);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado.getProductoId()).isEqualTo(dto.getProductoId());
        assertThat(resultado.getCantidad()).isEqualTo(dto.getCantidad());
        assertThat(resultado.getSubtotal()).isEqualByComparingTo("17.00");
        verify(pedidoRepository).save(any(Pedido.class));
    }

    // Test caso de error de agregarProductosAPedido. Pedido no encontrado
    @Test
    void agregarProductosAPedidoDeberiaLanzarExcepcionCuandoNoSeEncuentraElPedido() {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(10L, 2);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.agregarProductosAPedido(pedidoId, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido con ID " + pedidoId + " no encontrado");

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de agregarProductosAPedido. Pedido no modificable
    @Test
    void agregarProductosAPedidoDeberiaLanzarExcepcionCuandoPedidoNoEsModificable() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.PREPARACION, "0.00", List.of());
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(10L, 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.agregarProductosAPedido(pedido.getId(), dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No se puede modificar un pedido que ya ha avanzado de estado (está en "
                        + pedido.getEstadoPedido() + ")");

        // El estado se comprueba ANTES de buscar el producto: ni se consulta ni se guarda nada
        verifyNoInteractions(productoRepository);
        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de agregarProductosAPedido. Producto no encontrado
    @Test
    void agregarProductosAPedidoDeberiaLanzarExcepcionCuandoNoSeEncuentraElProducto() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "0.00", List.of());
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(10L, 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(dto.getProductoId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.agregarProductosAPedido(pedido.getId(), dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto con ID " + dto.getProductoId() + " no encontrado");

        // Aquí falla en productoRepository, no en pedidoRepository
        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de agregarProductosAPedido. Producto inactivo
    @Test
    void agregarProductosAPedidoDeberiaLanzarExcepcionCuandoProductoEstaInactivo() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", false);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "0.00", List.of());
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.agregarProductosAPedido(pedido.getId(), dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("El producto " + producto.getNombre() + " no está activo");

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de éxito de eliminarProductoDePedido. Restar cantidad, pero mantener línea
    @Test
    void eliminarProductoDePedidoDeberiaRestarCantidadCuandoCantidadEsMenorQueElTotal() {
        Producto hamburguesa = crearProducto(10L, "Hamburguesa clásica", "8.50", true);
        Producto patatas = crearProducto(20L, "Patatas", "3.00", true);

        // Dos líneas para luego distinguir precios del Subtotal y Total del pedido
        PedidoProducto lineaHamburguesa = crearLineaPedido(hamburguesa, 2);
        PedidoProducto lineaPatatas = crearLineaPedido(patatas, 1);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "20.00",
                List.of(lineaHamburguesa, lineaPatatas));

        int cantidadAEliminar = 1; // cantidad < 2 -> restar cantidad y conservar la línea
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(hamburguesa.getId(), cantidadAEliminar);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        ProductosPedidoDto resultado = pedidoService.eliminarProductoDePedido(pedido.getId(), dto.getProductoId(), dto.getCantidad());

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        // El resultado refleja el estado ya mutado de la línea (2 - 1 = 1), a diferencia del caso "eliminar entera"
        assertThat(resultado.getProductoId()).isEqualTo(hamburguesa.getId());
        assertThat(resultado.getCantidad()).isEqualTo(1);
        // resultado.getSubtotal() sale de pedidoProductoToDto(linea), que calcula precioUnitario × cantidad de esa línea
        assertThat(resultado.getSubtotal()).isEqualByComparingTo("8.50");
        // La línea sigue en el pedido (no se elimina), con la cantidad ya actualizada
        assertThat(pedido.getLineasPedido()).containsExactly(lineaHamburguesa, lineaPatatas);
        assertThat(lineaHamburguesa.getCantidad()).isEqualTo(1);
        // pedido.getTotal() lo fija la línea pedido.setTotal(calcularTotalDelPedido(pedido)), suma todas las líneas del pedido
        assertThat(pedido.getTotal()).isEqualByComparingTo("11.50"); // 8.50 + 3.00, la suma de todas

        verify(pedidoRepository).save(any(Pedido.class));
    }

    // Test caso de éxito de eliminarProductoDePedido. Quitar una línea entera, quedando otras en el pedido
    @Test
    void eliminarProductoDePedidoDeberiaQuitarLaLineaEnteraYConservarElPedidoCuandoQuedanOtrasLineas() {
        Producto hamburguesa = crearProducto(10L, "Hamburguesa clásica", "8.50", true);
        Producto patatas = crearProducto(20L, "Patatas", "3.00", true);

        PedidoProducto lineaHamburguesa = crearLineaPedido(hamburguesa, 2);
        PedidoProducto lineaPatatas = crearLineaPedido(patatas, 1);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "20.00",
                List.of(lineaHamburguesa, lineaPatatas));

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        // cantidad (2) >= cantidad de la línea (2): se quita la línea entera
        ProductosPedidoDto resultado = pedidoService.eliminarProductoDePedido(pedido.getId(), hamburguesa.getId(), 2);

        // El DTO describe la línea tal y como estaba antes de quitarla
        assertThat(resultado.getProductoId()).isEqualTo(hamburguesa.getId());
        assertThat(resultado.getCantidad()).isEqualTo(2);
        // La línea ya no está, la otra sí
        assertThat(pedido.getLineasPedido()).containsExactly(lineaPatatas);
        // Total calculado a mano: solo quedan las patatas, 3.00 * 1
        assertThat(pedido.getTotal()).isEqualByComparingTo("3.00");
        verify(pedidoRepository).save(pedido);
        verify(pedidoRepository, never()).delete(any(Pedido.class));
    }

    // Test caso de éxito de eliminarProductoDePedido. Eliminar última línea entera (pedido queda vacío y se elimina)
    @Test
    void eliminarProductoDePedidoDeberiaEliminarElPedidoCuandoSeQuitaLaUltimaLinea() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", true);

        PedidoProducto linea = crearLineaPedido(producto, 2);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "17.00", List.of(linea));

        int cantidadAEliminar = 2;
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), cantidadAEliminar);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        ProductosPedidoDto resultado = pedidoService.eliminarProductoDePedido(pedido.getId(), dto.getProductoId(), dto.getCantidad());

        assertThat(resultado).isNull();
        assertThat(pedido.getLineasPedido()).isEmpty();
        verify(pedidoRepository).delete(pedido);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // Test caso de error de eliminarProductoDePedido. Cantidad inválida (0 y negativa)
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void eliminarProductoDePedidoDeberiaLanzarExcepcionCuandoLaCantidadEsInvalida(int cantidadInvalida) {
        assertThatThrownBy(() -> pedidoService.eliminarProductoDePedido(1L, 10L, cantidadInvalida))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("La cantidad debe ser al menos 1");

        // Falla antes de consultar la base de datos
        verifyNoInteractions(pedidoRepository);
    }

    // Test caso de error de eliminarProductoDePedido. Pedido inexistente
    @Test
    void eliminarProductoDePedidoDeberiaLanzarExcepcionCuandoElPedidoNoExiste() {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(10L, 1);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.eliminarProductoDePedido(pedidoId, dto.getProductoId(), dto.getCantidad()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido con ID " + pedidoId + " no encontrado");

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de eliminarProductoDePedido. Pedido no modificable
    @Test
    void eliminarProductoDePedidoDeberiaLanzarExcepcionCuandoPedidoNoEsModificable() {
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", true);

        PedidoProducto linea = crearLineaPedido(producto, 2);
        // La línea SÍ existe: así el fallo solo puede venir del estado, no de "no está en el pedido"
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.PREPARACION, "17.00", List.of(linea));

        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), 1);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.eliminarProductoDePedido(pedido.getId(), dto.getProductoId(), dto.getCantidad()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No se puede modificar un pedido que ya ha avanzado de estado (está en "
                        + pedido.getEstadoPedido() + ")");

        // No se tocó nada: la línea sigue igual y no se guardó
        assertThat(pedido.getLineasPedido()).hasSize(1);
        assertThat(linea.getCantidad()).isEqualTo(2);
        verify(pedidoRepository, never()).save(any());
        verify(pedidoRepository, never()).delete(any(Pedido.class));
    }

    // Test caso de error de eliminarProductoDePedido. Línea de producto no figura en el pedido
    @Test
    void eliminarProductoDePedidoDeberiaLanzarExcepcionCuandoLaLineaNoEstaEnElPedido() {
        Producto productoBuscado = crearProducto(10L, "Hamburguesa clásica", "8.50", true);
        Producto otroProducto = crearProducto(20L, "Patatas", "3.00", true);

        PedidoProducto lineaDeOtroProducto = crearLineaPedido(otroProducto, 1);
        /* Incluimos un producto distinto del buscado en la línea en vez de dejarla vacía
        para comprobar que el filter funciona de verdad */
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "17.00", List.of(lineaDeOtroProducto));

        int cantidadAEliminar = 1;
        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(productoBuscado.getId(), cantidadAEliminar);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.eliminarProductoDePedido(pedido.getId(), dto.getProductoId(), dto.getCantidad()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("El producto con ID " + dto.getProductoId() + " no está en el pedido");

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
        verify(pedidoRepository, never()).delete(any(Pedido.class));
    }

    // Test caso de éxito de eliminarProductoDePedido. Producto inactivo (quitar siempre debe poder hacerse)
    @Test
    void eliminarProductoDePedidoDeberiaPermitirQuitarUnProductoDesactivado() {
        // El producto se desactivó DESPUÉS de añadirse al pedido
        Producto producto = crearProducto(10L, "Hamburguesa clásica", "8.50", false);

        PedidoProducto linea = crearLineaPedido(producto, 2);
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "17.00", List.of(linea));

        PedidoProductoRequestDto dto = crearPedidoProductoRequestDto(producto.getId(), 2);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        ProductosPedidoDto resultado = pedidoService.eliminarProductoDePedido(pedido.getId(), dto.getProductoId(), dto.getCantidad());

        assertThat(resultado).isNull();
        assertThat(pedido.getLineasPedido()).isEmpty();
        verifyNoInteractions(productoRepository);
        verify(pedidoRepository).delete(pedido);
    }

    // Test caso de éxito de obtenerPedidoPorCodigo
    @Test
    void obtenerPedidoPorCodigoDeberiaDevolverloCuandoExiste() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        /* La terminal de un pedido, al ser un objeto con su propia llamada a factoría,
        no lo metemos como parámetro obligatorio de crearPedido() y  */
        pedido.setTerminal(crearTerminal(1L));

        when(pedidoRepository.findByCodigo(pedido.getCodigo())).thenReturn(Optional.of(pedido));

        PedidoDto resultado = pedidoService.obtenerPedidoPorCodigo(pedido.getCodigo());

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado.getId()).isEqualTo(pedido.getId());
        assertThat(resultado.getCodigo()).isEqualTo(pedido.getCodigo());
    }

    // Test caso de error de obtenerPedidoPorCodigo. Pedido no encontrado
    @Test
    void obtenerPedidoPorCodigoDeberiaLanzarExcepcionCuandoPedidoNoSeEncuentra() {
        String codigo = "PED-0001";

        when(pedidoRepository.findByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.obtenerPedidoPorCodigo(codigo))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido con código " + codigo + " no encontrado");

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());
    }

    // Test caso de éxito de cambiarEstadoDelPedido
    @Test
    void cambiarEstadoDelPedidoDeberiaCambiarCuandoElCambioSolicitadoEsValido() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        pedido.setTerminal(crearTerminal(1L));

        EstadoPedido nuevoEstado = EstadoPedido.PREPARACION;

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        PedidoDto resultado = pedidoService.cambiarEstadoDelPedido(pedido.getId(), nuevoEstado);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        assertThat(resultado.getId()).isEqualTo(pedido.getId());
        assertThat(resultado.getEstado()).isEqualTo(nuevoEstado.name());

        verify(pedidoRepository).save(any(Pedido.class));
    }

    // Test caso de error de cambiarEstadoDelPedido. Pedido no encontrado
    @Test
    void cambiarEstadoDelPedidoDeberiaLanzarExcepcionCuandoPedidoNoSeEncuentra() {
        Long pedidoId = 1L;
        EstadoPedido nuevoEstado = EstadoPedido.PREPARACION;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cambiarEstadoDelPedido(pedidoId, nuevoEstado))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido con ID " + pedidoId + " no encontrado");

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de cambiarEstadoDelPedido. Transición de estado no permitida
    @Test
    void cambiarEstadoDelPedidoDeberiaLanzarExcepcionCuandoTransicionDeEstadoNoEstaPermitida() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        pedido.setTerminal(crearTerminal(1L));

        EstadoPedido nuevoEstado = EstadoPedido.LISTO;

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstadoDelPedido(pedido.getId(), nuevoEstado))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + nuevoEstado);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }

    // Test caso de error de cambiarEstadoDelPedido. Caso ENTREGADO no tiene estado al que cambiar
    @Test
    void cambiarEstadoDelPedidoDeberiaLanzarExcepcionCuandoEstadoActualEsEntregado() {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.ENTREGADO, "8.50", List.of());
        pedido.setTerminal(crearTerminal(1L));

        /* A diferencia de la transición anterior, aquí el nuevoEstado elegido es irrelevante:
        la rama ENTREGADO del switch devuelve false sin comparar nada, así que cualquier valor fallaría igual */
        EstadoPedido nuevoEstado = EstadoPedido.LISTO;

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstadoDelPedido(pedido.getId(), nuevoEstado))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + nuevoEstado);

        // System.out.println(mockingDetails(pedidoRepository).printInvocations());

        verify(pedidoRepository, never()).save(any());
    }
}