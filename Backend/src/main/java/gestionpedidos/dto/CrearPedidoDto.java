package gestionpedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoDto {

    @NotNull(message = "El id de la terminal es obligatorio")
    private Long terminalId;

    // @NotEmpty en un Map verifica que no sea null ni esté vacío (sin entradas)
    @NotEmpty(message = "El pedido debe tener al menos un producto")
    private Map<Long, Integer> productosComprados;
}