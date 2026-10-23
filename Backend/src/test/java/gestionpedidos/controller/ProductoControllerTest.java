package gestionpedidos.controller;

import gestionpedidos.dto.CrearProductoDto;
import gestionpedidos.dto.ProductoDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
public class ProductoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductoService productoService;

    @Autowired
    ObjectMapper objectMapper;

    // *** MÉTODOS FACTORÍA ***

    private ProductoDto crearProductoDto(Long id, String nombre, String precio, boolean activo, String nombreCategoria) {
        return new ProductoDto(id, nombre, new BigDecimal(precio), activo, nombreCategoria);
    }

    private CrearProductoDto crearCrearProductoDto(String nombre, String precio, boolean activo, Long categoriaId) {
        return new CrearProductoDto(nombre, new BigDecimal(precio), activo, categoriaId);
    }

    // *** TESTS ***

    // Test caso de éxito de crearProducto
    @Test
    void crearProductoDeberiaGuardarloConEstadoCreated() throws Exception {
        CrearProductoDto crearProductoDto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, 1L);
        ProductoDto productoDtoEsperado = crearProductoDto(1L, "Hamburguesa clásica", "8.50", true, "Hamburguesas");

        when(productoService.crearProducto(any(CrearProductoDto.class))).thenReturn(productoDtoEsperado);

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearProductoDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Producto creado correctamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Hamburguesa clásica"))
                /* Si el valor fuese más raro (ej: 10.33), tendríamos que usar un matcher de tolerancia de Hamcrest
                para evitar errores de precisión en la conversión a double de Jayway JsonPath -> .value(closeTo(8.50, 0.001)) */
                .andExpect(jsonPath("$.data.precio").value(8.50))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.nombreCategoria").value("Hamburguesas"));

        ArgumentCaptor<CrearProductoDto> captor = ArgumentCaptor.forClass(CrearProductoDto.class);
        verify(productoService).crearProducto(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Hamburguesa clásica");
        assertThat(captor.getValue().getPrecio()).isEqualByComparingTo("8.50");
        assertThat(captor.getValue().getActivo()).isEqualTo(true);
        assertThat(captor.getValue().getCategoriaId()).isEqualTo(1L);
    }

    // Test caso de error de crearProducto. Nombre ya existente
    @Test
    void crearProductoDeberiaDevolver409CuandoNombreYaExiste() throws Exception {
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, 1L);

        when(productoService.crearProducto(any(CrearProductoDto.class)))
                .thenThrow(new PedidoStateException("Ya existe un producto con el nombre " + dto.getNombre()));

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Ya existe un producto con el nombre " + dto.getNombre()));
    }

    // Test listarProductos: devuelve todos, ordenados por nombre por defecto, cuando no recibe argumentos
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorNombreCuandoNoRecibeArgumentosConEstadoOk() throws Exception {
        ProductoDto producto1 = crearProductoDto(1L, "Patatas fritas", "3.00", true, "Patatas");
        ProductoDto producto2 = crearProductoDto(2L, "Menú completo", "15.00", true, "Menús");

        when(productoService.listarProductos(null, null, null, null))
                .thenReturn(List.of(producto2, producto1));

        mvc.perform(get("/api/productos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nombre").value("Menú completo"))
                .andExpect(jsonPath("$[1].nombre").value("Patatas fritas"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test listarProductos: filtra por categoría además de por estado activo
    @Test
    void listarProductosDeberiaDevolverTodosFiltradosPorActivoYCategoriaConEstadoOk() throws Exception {
        ProductoDto p1 = crearProductoDto(1L, "Hamburguesa clásica", "8.50", true, "Hamburguesas");

        when(productoService.listarProductos(true, 1L, null, null)).thenReturn(List.of(p1));

        mvc.perform(get("/api/productos")
                        // Enviamos los query params en la petición. Siempre recibe String
                        .param("activo", "true")
                        .param("categoriaId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nombre").value("Hamburguesa clásica"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // Test listarProductos: ordena por precio ascendente por defecto
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorPrecioAscendentePorDefectoConEstadoOk() throws Exception {
        ProductoDto barato = crearProductoDto(1L, "Patatas fritas", "3.00", true, "Patatas");
        ProductoDto caro = crearProductoDto(2L, "Menú completo", "15.00", true, "Menús");

        when(productoService.listarProductos(null, null, "precio", null)).thenReturn(List.of(barato, caro));

        mvc.perform(get("/api/productos")
                        .param("orden", "precio"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].precio").value(3.00))
                .andExpect(jsonPath("$[1].precio").value(15.00))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test listarProductos: ordena por precio, pero en descendente cuando se pide DESC
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorPrecioDescendenteCuandoSePideConEstadoOk() throws Exception {
        ProductoDto barato = crearProductoDto(1L, "Patatas fritas", "3.00", true, "Patatas");
        ProductoDto caro = crearProductoDto(2L, "Menú completo", "15.00", true, "Menús");

        // Ahora el orden de los objetos de la lista debe ser al revés
        when(productoService.listarProductos(null, null, "precio", "DESC")).thenReturn(List.of(caro, barato));

        mvc.perform(get("/api/productos")
                        .param("orden", "precio")
                        .param("tipoOrden", "DESC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].precio").value(15.00))
                .andExpect(jsonPath("$[1].precio").value(3.00))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test caso de éxito de actualizarProducto
    @Test
    void actualizarProductoDeberiaGuardarloCuandoIdExisteConEstadoOk() throws Exception {
        ProductoDto dtoActualizadoEsperado = crearProductoDto(1L, "Hamburguesa premium", "12.50", true, "Hamburguesas");
        CrearProductoDto dtoActualizado = crearCrearProductoDto("Hamburguesa premium", "12.50", true, 1L);

        /* Hay que usar eq() en el id porque si un argumento de la llamada usa matcher (en este caso, any() es el necesario),
        todos los argumentos de la llamada deben usar matchers */
        when(productoService.actualizarProducto(eq(dtoActualizadoEsperado.getId()), any(CrearProductoDto.class))).thenReturn(dtoActualizadoEsperado);

        mvc.perform(put("/api/productos/{id}", dtoActualizadoEsperado.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoActualizado)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Producto actualizado correctamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Hamburguesa premium"))
                .andExpect(jsonPath("$.data.precio").value(12.50));
    }

    // Test caso de error de actualizarProducto. Producto no encontrado
    @Test
    void actualizarProductoDeberiaDevolver404CuandoProductoNoSeEncuentra() throws Exception {
        Long id = 1L;
        CrearProductoDto dtoActualizado = crearCrearProductoDto("Hamburguesa premium", "12.50", true, 1L);

        when(productoService.actualizarProducto(eq(id), any(CrearProductoDto.class)))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mvc.perform(put("/api/productos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoActualizado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']").value("Producto no encontrado"));
    }

    // Test caso de error de actualizarProducto. Categoría no encontrada
    @Test
    void actualizarProductoDeberiaDevolver404CuandoCategoriaNoSeEncuentra() throws Exception {
        Long id = 1L;
        CrearProductoDto dtoActualizado = crearCrearProductoDto("Hamburguesa premium", "12.50", true, 1L);

        when(productoService.actualizarProducto(eq(id), any(CrearProductoDto.class)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada"));

        mvc.perform(put("/api/productos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoActualizado)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']").value("Categoría no encontrada"));
    }

    // Test caso de éxito de cambiarEstado
    @Test
    void cambiarEstadoDeberiaCambiarloCuandoExiste() throws Exception {
        /* No hace falta generar el objeto entero porque no vamos a hacer nada con él.
        Con el id y el activo nos vale ya que el endpoint solo devuelve un mensaje */
        Long id = 1L;
        boolean nuevoEstado = false;

        // cambiarEstado es void. No hace falta ningún when(...), el mock no hace nada por defecto al llamarlo

        mvc.perform(patch("/api/productos/{id}/estado", id)
                        .param("activo", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Producto desactivado"));

        /* Al no haber ningún when() que comprobar, la única forma de confirmar que el controller le pasó al Service
         el id y activo correctos es verificando la llamada directamente */
        verify(productoService).cambiarEstado(id, nuevoEstado);
    }

    // Test caso de error de cambiarEstado. Producto no existente
    @Test
    void cambiarEstadoDeberiaDevolver404CuandoElProductoNoExiste() throws Exception {
        Long id = 1L;
        boolean nuevoEstado = false;

        // Sintaxis correcta para simular una excepción en un método void
        doThrow(new ResourceNotFoundException("El producto con ID " + id + " no existe"))
                .when(productoService)
                .cambiarEstado(id, nuevoEstado);

        mvc.perform(patch("/api/productos/{id}/estado", id)
                        .param("activo", "false"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']")
                        .value("El producto con ID " + id + " no existe"));
    }
}
