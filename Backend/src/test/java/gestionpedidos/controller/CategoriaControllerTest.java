package gestionpedidos.controller;

import gestionpedidos.dto.CategoriaDto;
import gestionpedidos.dto.CrearCategoriaDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.service.CategoriaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
public class CategoriaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @Autowired
    private ObjectMapper objectMapper;

    // *** MÉTODOS FACTORÍA ***

    private CategoriaDto crearCategoriaDto(Long id, String nombre) {
        return new CategoriaDto(id, nombre);
    }

    private CrearCategoriaDto crearCrearCategoriaDto(String nombre) {
        return new CrearCategoriaDto(nombre);
    }

    // *** TESTS ***

    // Test del método listarCategorias
    @Test
    void listarCategoriasDeberiaDevolverLasCategoriasConEstadoOk() throws Exception {
        // Given
        CategoriaDto dto1 = crearCategoriaDto(1L, "Hamburguesas");
        CategoriaDto dto2 = crearCategoriaDto(2L, "Patatas fritas");

        when(categoriaService.listarCategorias()).thenReturn(List.of(dto1, dto2));

        // When
        mvc.perform(get("/api/categorias"))
                // Then
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nombre").value("Hamburguesas"))
                .andExpect(jsonPath("$[1].nombre").value("Patatas fritas"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test caso de éxito del método crearCategoria
    @Test
    void crearCategoriaDeberiaGuardarlaConEstadoCreated() throws Exception {
        CrearCategoriaDto crearCategoriaDto = crearCrearCategoriaDto("Hamburguesas");
        CategoriaDto categoriaDtoEsperado = crearCategoriaDto(1L, "Hamburguesas");

        when(categoriaService.crearCategoria(any(CrearCategoriaDto.class))).thenReturn(categoriaDtoEsperado);

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearCategoriaDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Categoría creada correctamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Hamburguesas"));

        // Capturamos el argumento de verdad para las verificaciones
        ArgumentCaptor<CrearCategoriaDto> captor = ArgumentCaptor.forClass(CrearCategoriaDto.class);
        verify(categoriaService).crearCategoria(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Hamburguesas");
    }

    // Test caso de error del método crearCategoria. Nombre ya existente
    @Test
    void crearCategoriaDeberiaDevolver409CuandoNombreYaExiste() throws Exception {
        CrearCategoriaDto crearCategoriaDto = crearCrearCategoriaDto("Hamburguesas");

        when(categoriaService.crearCategoria(any(CrearCategoriaDto.class)))
                .thenThrow(new PedidoStateException("Ya existe una categoría con el nombre " + crearCategoriaDto.getNombre()));

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearCategoriaDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Ya existe una categoría con el nombre " + crearCategoriaDto.getNombre()));
    }

    // Test caso de éxito del método obtenerPorId
    @Test
    void obtenerPorIdDeberiaDevolverLaCategoriaConEstadoOk() throws Exception {
        CategoriaDto categoriaDto = crearCategoriaDto(1L, "Hamburguesas");

        when(categoriaService.obtenerPorId(categoriaDto.getId())).thenReturn(categoriaDto);

        mvc.perform(get("/api/categorias/{id}", categoriaDto.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Hamburguesas"));
    }

    // Test caso de error del método obtenerPorId. Categoría no encontrada
    @Test
    void obtenerPorIdDeberiaDevolver404CuandoNoExiste() throws Exception {
        Long id = 1L;

        when(categoriaService.obtenerPorId(id))
                .thenThrow(new ResourceNotFoundException("La categoría con ID " + id + " no existe"));

        mvc.perform(get("/api/categorias/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['Error: ']").value("La categoría con ID " + id + " no existe"));
    }
}
