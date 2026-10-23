package gestionpedidos;

import gestionpedidos.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/* Ya configura el TestRestTemplate con la URL base correcta por debajo. Internamente le mete un RootUriTemplateHandler
que sabe en qué puerto arrancó el servidor embebido y antepone esa base a cualquier ruta relativa que le pases */
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class PedidoIntegrationTest {

    @Autowired
    private TestRestTemplate client;

    @SuppressWarnings("unchecked")
    @Test
    void flujoCompletoDeRegistrarPedidoDeberiaFuncionarDeExtremoAExtremo() {
        // 1. Creamos una categoría de verdad, vía HTTP, contra la app entera
        CrearCategoriaDto categoriaDto = new CrearCategoriaDto("Hamburguesas");
        /* Los endpoints POST de creación de cosas del proyecto devuelven un Map con el mensaje y los datos,
        de ahí que el tipo de la respuesta deba ser Map.class */
        ResponseEntity<Map> responseCategoria = client.postForEntity(
                "/api/categorias", categoriaDto, Map.class);
        assertThat(responseCategoria.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Necesitaremos el ID de la categoría para crear el producto después
        Long categoriaId = Long.valueOf(
                ((Map) responseCategoria.getBody().get("data")).get("id").toString());

        // 2. Creamos un producto real, usando esa categoría
        CrearProductoDto productoDto = new CrearProductoDto(
                "Hamburguesa clásica", new BigDecimal("8.50"), true, categoriaId);
        // Map raw es la única forma de usar postForEntity con una respuesta de tipo Map
        ResponseEntity<Map> responseProducto = client.postForEntity(
                "/api/productos", productoDto, Map.class);
        assertThat(responseProducto.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long productoId = Long.valueOf(
                ((Map) responseProducto.getBody().get("data")).get("id").toString()); // Para crear el pedido

        // 3. Creamos una terminal real
        CrearTerminalDto terminalDto = new CrearTerminalDto("Terminal 1");
        ResponseEntity<Map> responseTerminal = client.postForEntity(
                "/api/terminales", terminalDto, Map.class);
        Long terminalId = Long.valueOf(
                ((Map) responseTerminal.getBody().get("data")).get("id").toString()); // Para crear el pedido

        // 4. Registramos el pedido de verdad, contra la base de datos H2 real
        Map<Long, Integer> productosComprados = new HashMap<>();
        productosComprados.put(productoId, 2);
        CrearPedidoDto pedidoDto = new CrearPedidoDto(terminalId, productosComprados);

        ResponseEntity<Map> responsePedido = client.postForEntity(
                "/api/pedidos", pedidoDto, Map.class);

        // 5. Comprobamos la respuesta HTTP completa
        assertThat(responsePedido.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> pedidoCreado = (Map<String, Object>) responsePedido.getBody().get("data");
        assertThat(pedidoCreado.get("estado")).isEqualTo("CREADO");
        assertThat(pedidoCreado.get("total")).isEqualTo(17.00); // 8.50 * 2, calculado de verdad por el Service real

        // 6. Y confirmamos que quedó guardado de verdad, recuperándolo por su código
        String codigo = (String) pedidoCreado.get("codigo");
        ResponseEntity<PedidoDto> responseConsulta = client.getForEntity(
                "/api/pedidos/codigo/{codigo}", PedidoDto.class, codigo);
        assertThat(responseConsulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseConsulta.getBody().getCodigo()).isEqualTo(codigo);
    }
}