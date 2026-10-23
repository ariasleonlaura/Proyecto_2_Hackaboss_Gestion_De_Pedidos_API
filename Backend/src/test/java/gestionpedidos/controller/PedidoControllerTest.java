package gestionpedidos.controller;

import gestionpedidos.dto.*;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.exception.BadRequestException;
import gestionpedidos.model.*;
import gestionpedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
public class PedidoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoService pedidoService;

    @Autowired
    ObjectMapper objectMapper;

    // *** MÉTODOS FACTORÍA ***

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

    private PedidoDto crearPedidoDto(Long id, String codigo, String estado, String total, List<ProductosPedidoDto> productos) {
        PedidoDto pedidoDto = new PedidoDto();
        pedidoDto.setId(id);
        pedidoDto.setCodigo(codigo);
        pedidoDto.setEstado(estado);
        pedidoDto.setTotal(new BigDecimal(total));
        pedidoDto.setProductos(productos);
        return pedidoDto;
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

    // Test caso de error general. La ruta no existe
    @Test
    void unaRutaInexistenteDeberiaDevolver404() throws Exception {
        mvc.perform(get("/api/ruta-que-no-existe"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']").value("Ruta no encontrada: /api/ruta-que-no-existe"));
    }

    // Test de listarPedidos sin filtrar
    @Test
    void listarPedidosDeberiaDevolverTodosLosPedidosSinFiltrarConEstadoOk() throws Exception {
        PedidoDto pedido1 = crearPedidoDto(1L, "PED-0001", "PREPARACION", "10.00", List.of());
        PedidoDto pedido2 = crearPedidoDto(2L, "PED-0002", "CREADO", "10.00", List.of());

        when(pedidoService.listarPedidos(null)).thenReturn(List.of(pedido1, pedido2));

        mvc.perform(get("/api/pedidos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].codigo").value("PED-0001"))
                .andExpect(jsonPath("$[1].codigo").value("PED-0002"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test de listarPedidos filtrados por el valor de "estado"
    @Test
    void listarPedidosDeberiaDevolverlosFiltradosPorEstadoConEstadoOk() throws Exception {
        PedidoDto pedido1 = crearPedidoDto(1L, "PED-0001", "PREPARACION", "10.00", List.of());

        when(pedidoService.listarPedidos(EstadoPedido.PREPARACION)).thenReturn(List.of(pedido1));

        mvc.perform(get("/api/pedidos")
                        .param("estado", "PREPARACION"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].codigo").value("PED-0001"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // Test caso de éxito de registrarPedido
    @Test
    void registrarPedidoDeberiaGuardarloConEstadoCreated() throws Exception {
        CrearPedidoDto dtoEnviado = crearPedidoDtoConUnProducto(1L, 1L, 2);
        ProductosPedidoDto linea = new ProductosPedidoDto(1L, "Hamburguesa clásica", 2,
                new BigDecimal("4.25"), new BigDecimal("8.50"));
        PedidoDto dtoEsperado = crearPedidoDto(1L, "PED-0001", "PREPARACION", "8.50", List.of(linea));

        when(pedidoService.registrarPedido(any(CrearPedidoDto.class))).thenReturn(dtoEsperado);

        mvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Pedido registrado correctamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.codigo").value("PED-0001"))
                .andExpect(jsonPath("$.data.estado").value("PREPARACION"))
                .andExpect(jsonPath("$.data.total").value(8.50))
                .andExpect(jsonPath("$.data.productos", hasSize(1)))
                .andExpect(jsonPath("$.data.productos[0].productoId").value(1))
                .andExpect(jsonPath("$.data.productos[0].cantidad").value(2));

        ArgumentCaptor<CrearPedidoDto> captor = ArgumentCaptor.forClass(CrearPedidoDto.class);
        verify(pedidoService).registrarPedido(captor.capture());
        assertThat(captor.getValue().getTerminalId()).isEqualTo(1L);
        assertThat(captor.getValue().getProductosComprados()).containsEntry(1L, 2);
    }

    // Test caso de error de registrarPedido. Terminal inexistente
    @Test
    void registrarPedidoDeberiaDevolver404CuandoTerminalNoExiste() throws Exception {
        CrearPedidoDto dtoEnviado = crearPedidoDtoConUnProducto(1L, 1L, 2);

        when(pedidoService.registrarPedido(any(CrearPedidoDto.class)))
                .thenThrow(new ResourceNotFoundException("La terminal con ID " + dtoEnviado.getTerminalId() + " no existe"));

        mvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("La terminal con ID " + dtoEnviado.getTerminalId() + " no existe"));
    }

    // Test caso de error de registrarPedido. Producto no encontrado
    @Test
    void registrarPedidoDeberiaDevolver404CuandoProductoNoSeEncuentra() throws Exception {
        Long productoId = 1L;
        CrearPedidoDto dtoEnviado = crearPedidoDtoConUnProducto(1L, 1L, 2);

        when(pedidoService.registrarPedido(any(CrearPedidoDto.class)))
                .thenThrow(new ResourceNotFoundException("Producto con ID " + productoId + " no encontrado"));

        mvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Producto con ID " + productoId + " no encontrado"));
    }

    // Test caso de error de registrarPedido. Producto inactivo
    @Test
    void registrarPedidoDeberiaDevolver400CuandoProductoEstaInactivo() throws Exception {
        Producto producto = crearProducto(1L, "Hamburguesa", "8.50", false);
        CrearPedidoDto dtoEnviado = crearPedidoDtoConUnProducto(1L, 1L, 2);

        when(pedidoService.registrarPedido(any(CrearPedidoDto.class)))
                .thenThrow(new BadRequestException("El producto " + producto.getNombre() + " no está activo"));

        mvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("El producto " + producto.getNombre() + " no está activo"));
    }

    // Test caso de éxito de agregarProductosAPedido. Suma a la cantidad existente de producto
    @Test
    void agregarProductosAPedidoDeberiaAgregarloSumandoloALaCantidadAnteriorSiYaExisteConEstadoOk() throws Exception {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1); // se añade 1 unidad más

        // Resultado YA sumado: 1 (había) + 1 (se añade) = 2. Subtotal: 8.50 * 2 = 17.00
        ProductosPedidoDto lineaResultado = new ProductosPedidoDto(1L, "Hamburguesa clásica", 2,
                new BigDecimal("8.50"), new BigDecimal("17.00"));

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class))).thenReturn(lineaResultado);

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.subtotal").value(17.00));
    }

    // Test caso de éxito de agregarProductosAPedido. Nueva línea de producto
    @Test
    void agregarProductosAPedidoDeberiaAgregarlosCreandoUnaNuevaLineaDeProductoSiNoExisteConEstadoOk() throws Exception {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1);
        ProductosPedidoDto lineaResultado = new ProductosPedidoDto(1L, "Hamburguesa clásica", 1,
                new BigDecimal("8.50"), new BigDecimal("8.50"));

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class))).thenReturn(lineaResultado);

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.subtotal").value(8.50));
    }

    // Test caso de éxito de agregarProductosAPedido. El body es JSON mal formado
    @Test
    void agregarProductosAPedidoDeberiaDevolver400CuandoElJsonEstaMalFormado() throws Exception {
        mvc.perform(post("/api/pedidos/{pedidoId}/productos", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("El cuerpo de la petición no es válido: JSON mal formado o valor no permitido"));

        verifyNoInteractions(pedidoService);
    }

    // Test caso de error de agregarProductosAPedido. Pedido no encontrado
    @Test
    void agregarProductosAPedidoDeberiaDevolver404CuandoNoSeEncuentraElPedido() throws Exception {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1);

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Pedido con ID " + pedidoId + " no encontrado"));

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Pedido con ID " + pedidoId + " no encontrado"));
    }

    // Test caso de error de agregarProductosAPedido. Pedido no modificable
    @Test
    void agregarProductosAPedidoDeberiaDevolver400CuandoElPedidoNoEsModificable() throws Exception {
        Long pedidoId = 1L;
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1);
        String mensaje = "No se puede modificar un pedido que ya ha avanzado de estado (está en "
                + EstadoPedido.PREPARACION + ")";

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class)))
                .thenThrow(new BadRequestException(mensaje));

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']").value(mensaje));
    }

    // Test caso de error de agregarProductosAPedido. Producto no encontrado
    @Test
    void agregarProductosAPedidoDeberiaDevolver404CuandoNoSeEncuentraElProducto() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1);

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Producto con ID " + productoId + " no encontrado"));

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Producto con ID " + productoId + " no encontrado"));
    }

    // Test caso de error de agregarProductosAPedido. Producto inactivo
    @Test
    void agregarProductosAPedidoDeberiaDevolver400CuandoProductoEstaInactivo() throws Exception {
        Long pedidoId = 1L;
        Producto producto = crearProducto(1L, "Hamburguesa", "8.50", false);
        PedidoProductoRequestDto dtoEnviado = crearPedidoProductoRequestDto(1L, 1);

        when(pedidoService.agregarProductosAPedido(eq(pedidoId), any(PedidoProductoRequestDto.class)))
                .thenThrow(new BadRequestException("El producto " + producto.getNombre() + " no está activo"));

        mvc.perform(post("/api/pedidos/{pedidoId}/productos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("El producto " + producto.getNombre() + " no está activo"));
    }

    // Test caso de éxito de eliminarProductoDePedido. Eliminar línea entera
    @Test
    void eliminarProductoDePedidoDeberiaEliminarLaLineaEnteraCuandoCantidadEsMayorOIgualQueLaCantidadExistenteConEstadoOk() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 2;
        ProductosPedidoDto dtoEsperado = new ProductosPedidoDto(1L, "Hamburguesa", 2,
                new BigDecimal("8.50"), new BigDecimal("17.00"));

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenReturn(dtoEsperado);

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.subtotal").value(17.00));

        verify(pedidoService).eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad));
    }

    // Test caso de éxito de eliminarProductoDePedido. Restar cantidad, pero mantener línea
    @Test
    void eliminarProductoDePedidoDeberiaRestarCantidadCuandoCantidadEsMenorQueElTotalConEstadoOk() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 1;
        ProductosPedidoDto dtoEsperado = new ProductosPedidoDto(1L, "Hamburguesa", 1,
                new BigDecimal("8.50"), new BigDecimal("8.50"));

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenReturn(dtoEsperado);

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.subtotal").value(8.50));

        verify(pedidoService).eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad));
    }

    // Test caso de éxito de eliminarProductoDePedido. Pedido eliminado (vacío)
    @Test
    void eliminarProductoDePedidoDeberiaDevolver204CuandoElPedidoQuedaVacioYElimina() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 2;

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenReturn(null);

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(pedidoService).eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad));
    }

    // Test caso de error de eliminarProductoDePedido. Falta el parámetro obligatorio
    @Test
    void eliminarProductoDePedidoDeberiaDevolver400CuandoFaltaLaCantidad() throws Exception {
        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", 1L, 1L))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']").value("Falta el parámetro obligatorio 'cantidad'"));

        verifyNoInteractions(pedidoService);
    }

    // Test caso de error de eliminarProductoDePedido. El parámetro tiene el tipo equivocado
    @Test
    void eliminarProductoDePedidoDeberiaDevolver400CuandoLaCantidadNoEsUnNumero() throws Exception {
        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", 1L, 1L)
                        .param("cantidad", "abc"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']").value("El valor 'abc' no es válido para 'cantidad'"));

        verifyNoInteractions(pedidoService);
    }

    // Test caso de error de eliminarProductoDePedido. Pedido inexistente
    @Test
    void eliminarProductoDePedidoDeberiaDevolver404CuandoElPedidoNoExiste() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 2;

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenThrow(new ResourceNotFoundException("Pedido con ID " + pedidoId + " no encontrado"));

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Error: ']")
                        .value("Pedido con ID " + pedidoId + " no encontrado"));
    }

    // Test caso de error de eliminarProductoDePedido. Pedido no modificable
    @Test
    void eliminarProductoDePedidoDeberiaDevolver400CuandoElPedidoNoEsModificable() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 1;
        String mensaje = "No se puede modificar un pedido que ya ha avanzado de estado (está en "
                + EstadoPedido.PREPARACION + ")";

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenThrow(new BadRequestException(mensaje));

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']").value(mensaje));
    }

    // Test caso de error de eliminarProductoDePedido. Línea de producto no figura en el pedido
    @Test
    void eliminarProductoDePedidoDeberiaDevolver404CuandoLaLineaNoEstaEnElPedido() throws Exception {
        Long pedidoId = 1L;
        Long productoId = 1L;
        Integer cantidad = 2;

        when(pedidoService.eliminarProductoDePedido(eq(pedidoId), eq(productoId), eq(cantidad)))
                .thenThrow(new ResourceNotFoundException("El producto con ID " + productoId + " no está en el pedido"));

        mvc.perform(delete("/api/pedidos/{pedidoId}/eliminar-producto/{productoId}", pedidoId, productoId)
                        .param("cantidad", String.valueOf(cantidad)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Error: ']")
                        .value("El producto con ID " + productoId + " no está en el pedido"));
    }

    // Test caso de éxito de obtenerPedidoPorCodigo
    @Test
    void obtenerPedidoPorCodigoDeberiaDevolverloCuandoExisteConEstadoOk() throws Exception {
        PedidoDto pedidoDto = crearPedidoDto(1L, "PED-0001", "CREADO", "0.00", List.of());

        when(pedidoService.obtenerPedidoPorCodigo(pedidoDto.getCodigo())).thenReturn(pedidoDto);

        mvc.perform(get("/api/pedidos/codigo/{codigo}", pedidoDto.getCodigo()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("PED-0001"));
    }

    // Test caso de error de obtenerPedidoPorCodigo. Pedido no encontrado
    @Test
    void obtenerPedidoPorCodigoDeberiaDevolver404CuandoPedidoNoSeEncuentra() throws Exception {
        String codigo = "PED-0001";

        when(pedidoService.obtenerPedidoPorCodigo(codigo))
                .thenThrow(new ResourceNotFoundException("Pedido con código " + codigo + " no encontrado"));

        mvc.perform(get("/api/pedidos/codigo/{codigo}", codigo))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Pedido con código " + codigo + " no encontrado"));
    }

    // Test caso de éxito de cambiarEstadoDelPedido
    @Test
    void cambiarEstadoDelPedidoDeberiaCambiarCuandoElCambioSolicitadoEsValidoConEstadoOk() throws Exception {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        EstadoPedidoRequestDto dtoEnviado = new EstadoPedidoRequestDto(EstadoPedido.PREPARACION);
        PedidoDto dtoEsperado = crearPedidoDto(1L, "PED-0001", "PREPARACION", "8.50", List.of());

        when(pedidoService.cambiarEstadoDelPedido(pedido.getId(), dtoEnviado.getEstado())).thenReturn(dtoEsperado);

        mvc.perform(patch("/api/pedidos/{pedidoId}/estado", pedido.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PREPARACION"));
    }

    // El body es JSON válido pero con un estado que no existe en el enum
    @Test
    void cambiarEstadoDelPedidoDeberiaDevolver400CuandoElEstadoNoExiste() throws Exception {
        mvc.perform(patch("/api/pedidos/{pedidoId}/estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INVENTADO\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("El cuerpo de la petición no es válido: JSON mal formado o valor no permitido"));

        verifyNoInteractions(pedidoService);
    }

    // Test caso de error de cambiarEstadoDelPedido. Pedido no encontrado
    @Test
    void cambiarEstadoDelPedidoDeberiaDevolver404CuandoPedidoNoSeEncuentra() throws Exception {
        Long pedidoId = 1L;
        EstadoPedidoRequestDto dtoEnviado = new EstadoPedidoRequestDto(EstadoPedido.PREPARACION);

        when(pedidoService.cambiarEstadoDelPedido(pedidoId, dtoEnviado.getEstado()))
                .thenThrow(new ResourceNotFoundException("Pedido con ID " + pedidoId + " no encontrado"));

        mvc.perform(patch("/api/pedidos/{pedidoId}/estado", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Pedido con ID " + pedidoId + " no encontrado"));
    }

    // Test caso de error de cambiarEstadoDelPedido. Transición de estado no permitida
    @Test
    void cambiarEstadoDelPedidoDeberiaDevolver400CuandoTransicionDeEstadoNoEstaPermitida() throws Exception {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.CREADO, "8.50", List.of());
        EstadoPedidoRequestDto dtoEnviado = new EstadoPedidoRequestDto(EstadoPedido.LISTO);

        when(pedidoService.cambiarEstadoDelPedido(pedido.getId(), dtoEnviado.getEstado()))
                .thenThrow(new BadRequestException("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + dtoEnviado.getEstado()));

        mvc.perform(patch("/api/pedidos/{pedidoId}/estado", pedido.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + dtoEnviado.getEstado()));
    }

    // Test caso de error de cambiarEstadoDelPedido. Caso ENTREGADO no tiene estado al que cambiar
    @Test
    void cambiarEstadoDelPedidoDeberiaDevolver400CuandoEstadoActualEsEntregado() throws Exception {
        Pedido pedido = crearPedido(1L, "PED-0001", EstadoPedido.ENTREGADO, "8.50", List.of());
        EstadoPedidoRequestDto dtoEnviado = new EstadoPedidoRequestDto(EstadoPedido.LISTO);

        when(pedidoService.cambiarEstadoDelPedido(pedido.getId(), dtoEnviado.getEstado()))
                .thenThrow(new BadRequestException("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + dtoEnviado.getEstado()));

        mvc.perform(patch("/api/pedidos/{pedidoId}/estado", pedido.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEnviado)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Transición de estado no permitida: " + pedido.getEstadoPedido() + " → " + dtoEnviado.getEstado()));
    }
}
