package gestionpedidos.controller;

import gestionpedidos.dto.CrearTerminalDto;
import gestionpedidos.dto.TerminalDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.service.TerminalService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(TerminalController.class)
public class TerminalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TerminalService terminalService;

    @Autowired
    private ObjectMapper objectMapper;

    // *** MÉTODOS FACTORÍA ***

    /* @AllArgsConstructor de la clase me pide justo los campos que me interesan,
    por lo que podemos usar el constructor directamente */
    private TerminalDto crearTerminalDto(Long id, String nombre) {
        return new TerminalDto(id, nombre);
    }

    private CrearTerminalDto crearCrearTerminalDto(String nombre) {
        return new CrearTerminalDto(nombre);
    }

    // *** TESTS ***

    // Test de caso de éxito de listarTerminales
    @Test
    void listarTerminalesDeberiaDevolverLasTerminalesConEstadoOk() throws Exception {
        // Given
        TerminalDto terminal1 = crearTerminalDto(1L, "Terminal 1");
        TerminalDto terminal2 = crearTerminalDto(2L, "Terminal 2");

        // Se mockea el Service, no la BBDD
        when(terminalService.listarTerminales()).thenReturn(List.of(terminal1, terminal2));

        // When
        mvc.perform(get("/api/terminales"))
                // Then
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Comprobamos sus campos
                .andExpect(jsonPath("$[0].nombre").value("Terminal 1"))
                .andExpect(jsonPath("$[1].nombre").value("Terminal 2"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Test de caso de éxito de terminalPorId
    @Test
    void terminalPorIdDeberiaDevolverLaTerminalConEstadoOk() throws Exception {
        TerminalDto terminalDto = crearTerminalDto(1L, "Terminal 1");

        when(terminalService.buscarTerminalPorId(terminalDto.getId())).thenReturn(terminalDto);

        // Se usa un placeholder {id} en el String y el valor real como argumento aparte
        mvc.perform(get("/api/terminales/{id}", terminalDto.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                /* Cuando Jackson serializa un Long a JSON, el número queda sin ningún tipo explícito (1, sin más).
                Al leerlo de vuelta, la librería jsonPath que usa Spring internamente lo interpreta como Integer,
                no como Long */
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Terminal 1"));
    }

    // Test de caso de error de terminalPorId. Terminal no encontrada
    @Test
    void terminalPorIdDeberiaDevolver404CuandoNoExiste() throws Exception {
        Long id = 1L;

        when(terminalService.buscarTerminalPorId(id))
                .thenThrow(new ResourceNotFoundException("La terminal con ID " + id + " no existe"));

        mvc.perform(get("/api/terminales/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                // Hay que copiar el GlobalExceptionHandler letra por letra y el mensaje del Service
                .andExpect(jsonPath("$['Error: ']").value("La terminal con ID " + id + " no existe"));
    }

    // Test de caso de éxito de crearTerminal
    @Test
    void crearTerminalDeberiaGuardarLaTerminalConEstadoCreated() throws Exception {
        CrearTerminalDto crearTerminalDto = crearCrearTerminalDto("Terminal 1");
        TerminalDto terminalDtoEsperado = crearTerminalDto(1L, "Terminal 1");

        /* MockMvc deserializa el JSON del body en un objeto NUEVO, distinto en memoria del "dto" de aquí arriba.
        Como CrearTerminalDto no tiene equals() propio, when(terminalService.crearTerminal(dto)) nunca
        coincidiría con la llamada real — por eso usamos any() en vez del objeto literal */
        when(terminalService.crearTerminal(any(CrearTerminalDto.class))).thenReturn(terminalDtoEsperado);

        mvc.perform(post("/api/terminales")
                        /* Aquí sí enviamos un JSON en el body de la petición. contentType() le dice a Spring
                        qué formato tiene ese cuerpo para poder deserializarlo correctamente en el
                        @RequestBody CrearTerminalDto del controller */
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearTerminalDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Terminal creada correctamente"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Terminal 1"));

        /* any() no comprueba el contenido del CrearTerminalDto que llegó realmente al Service.
        Capturamos el argumento de verdad para confirmar que el "nombre" del JSON se deserializó bien */
        ArgumentCaptor<CrearTerminalDto> captor = ArgumentCaptor.forClass(CrearTerminalDto.class);
        verify(terminalService).crearTerminal(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Terminal 1");
    }

    // Test de caso de error de crearTerminal. Nombre ya existente
    @Test
    void crearTerminalDeberiaDevolver409CuandoNombreYaExiste() throws Exception {
        CrearTerminalDto crearTerminalDto = crearCrearTerminalDto("Terminal 1");

        when(terminalService.crearTerminal(any(CrearTerminalDto.class)))
                .thenThrow(new PedidoStateException("Ya existe una terminal con el nombre " + crearTerminalDto.getNombre()));

        mvc.perform(post("/api/terminales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearTerminalDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$['Error: ']")
                        .value("Ya existe una terminal con el nombre " + crearTerminalDto.getNombre()));
    }
}
