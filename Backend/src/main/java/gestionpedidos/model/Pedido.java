package gestionpedidos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pedidos")
// Esta clase hace referencia al 'Ticket de compra' -> nuestro Pedido
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private LocalDateTime fecha;

    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    // @Enumerated para decir a la bbdd que este campo es una lista cerrada de pciones, puesto que es un enum
    // Y EnumType.STRING para decir a la bb que guarde el nombre del estado como texto, si no lo haría como un número
    private EstadoPedido estadoPedido;

    @ManyToOne // Relación muchos a uno, muchos pedidos pertenecen a una determinada terminal
    @JoinColumn(name = "terminal_id", nullable = false)
    //Para relacionarlo con el id de Terminal. Esta es la foreing key que se relaciona con la primary key de Terminal
    private Terminal terminal;

    // Un pedido puede tener muchos artículos (líneas de pedido).
    // mappedBy indica que la relación se controla desde el campo 'pedido' de la clase PedidoProducto.
    // CascadeType.ALL es para la base de datos, que si guardas/borras el Pedido, sus líneas se guardan/borran solas.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoProducto> lineasPedido; // Esta lista es el "carrito" o desglose de productos del ticket
}
