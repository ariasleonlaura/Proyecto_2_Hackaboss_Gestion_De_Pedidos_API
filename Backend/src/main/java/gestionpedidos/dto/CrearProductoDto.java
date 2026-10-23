package gestionpedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearProductoDto {

    // @NotBlank — rechaza null, "" y "   " (solo espacios) en Strings
    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    // @NotNull — rechaza null pero permite valores vacíos en tipos no-String
    @NotNull(message = "El precio es obligatorio")
    // @DecimalMin("0.01") — el número debe ser ≥ 0.01. Funciona con BigDecimal, Double, Integer, etc
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
    private BigDecimal precio;

    @NotNull(message = "El campo activo es obligatorio")
    private Boolean activo;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;
}