package gestionpedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearCategoriaDto {

    @NotBlank(message = "El nombre de la categoría no puede estar vacío")
    private String nombre;
}