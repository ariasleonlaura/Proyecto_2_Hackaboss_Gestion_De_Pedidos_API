package gestionpedidos.dto;

import gestionpedidos.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// Se utiliza como datos de entrada en PedidoController y poder mandar el estado del pedido al que queremos cambiar
public class EstadoPedidoRequestDto {
    private EstadoPedido estado;
}
