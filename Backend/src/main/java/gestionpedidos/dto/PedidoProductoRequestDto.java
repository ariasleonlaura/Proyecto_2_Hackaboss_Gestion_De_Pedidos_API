package gestionpedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

// Este DTO se utiliza como datos de entrada que suprime algunos datos innecesarios de ProductosPedidoDto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoProductoRequestDto {

        @NotNull(message = "El id del producto es obligatorio")
        private Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        // @Min(1) es la versión para enteros de @DecimalMin. Rechaza 0 y negativos
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
}