package gestionpedidos.controller;

import gestionpedidos.dto.ProductoMasVendidoDto;
import gestionpedidos.dto.TerminalMasUtilizadaDto;
import gestionpedidos.service.EstadisticasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EstadisticasController.class)
public class EstadisticasControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstadisticasService estadisticasService;

    @Test
    void obtenerRankingDeTerminalesDeberiaDevolverloConEstadoOk() throws Exception {
        TerminalMasUtilizadaDto dto = new TerminalMasUtilizadaDto("Terminal 1", 20L);

        when(estadisticasService.obtenerRankingDeTerminales()).thenReturn(List.of(dto));

        mvc.perform(get("/api/estadisticas/ranking-terminales"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nombreTerminal").value("Terminal 1"))
                .andExpect(jsonPath("$[0].totalPedidos").value(20))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void obtenerProductosMasVendidosDeberiaDevolverloConEstadoOk() throws Exception {
        ProductoMasVendidoDto dto = new ProductoMasVendidoDto(1L, "Hamburguesa clásica", 25L);

        when(estadisticasService.obtenerProductosMasVendidos()).thenReturn(List.of(dto));

        mvc.perform(get("/api/estadisticas/productos-mas-vendidos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Hamburguesa clásica"))
                .andExpect(jsonPath("$[0].totalVendido").value(25))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void obtenerProductosMasVendidosDeberiaDevolverMultiplesProductos() throws Exception {
        ProductoMasVendidoDto dto1 = new ProductoMasVendidoDto(1L, "Hamburguesa clásica", 25L);
        ProductoMasVendidoDto dto2 = new ProductoMasVendidoDto(2L, "Patatas fritas", 18L);

        when(estadisticasService.obtenerProductosMasVendidos()).thenReturn(List.of(dto1, dto2));

        mvc.perform(get("/api/estadisticas/productos-mas-vendidos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].nombreProducto").value("Hamburguesa clásica"))
                .andExpect(jsonPath("$[0].totalVendido").value(25))
                .andExpect(jsonPath("$[1].productoId").value(2))
                .andExpect(jsonPath("$[1].nombreProducto").value("Patatas fritas"))
                .andExpect(jsonPath("$[1].totalVendido").value(18))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void obtenerProductosMasVendidosDeberiaDevolverListaVaciaCuandoNoHayVentas() throws Exception {
        when(estadisticasService.obtenerProductosMasVendidos()).thenReturn(List.of());

        mvc.perform(get("/api/estadisticas/productos-mas-vendidos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
