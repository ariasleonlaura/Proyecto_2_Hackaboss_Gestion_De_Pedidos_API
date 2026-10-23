package gestionpedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TerminalMasUtilizadaDto {
    private String nombreTerminal;
    // en JPQL, SUM() sobre un entero da Long, entonces lo usamos para cualquier @Query de agregación
    private Long totalPedidos;
}
