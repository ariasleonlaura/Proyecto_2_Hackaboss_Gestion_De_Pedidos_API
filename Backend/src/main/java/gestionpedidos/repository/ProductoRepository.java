package gestionpedidos.repository;

import gestionpedidos.dto.ProductoMasVendidoDto;
import gestionpedidos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByNombre(String nombre);

    /* @Query te deja escribir la consulta a mano encima del método. Se usa el nombre de la clase (PedidoProducto)
    y no el de la tabla (pedidos_productos) para que Hibernate lo reconozca como entidad y la traduzca a SQL real por debajo */
    @Query("""
            SELECT new gestionpedidos.dto.ProductoMasVendidoDto(pp.producto.id, pp.producto.nombre, SUM(pp.cantidad))
            FROM PedidoProducto pp
            GROUP BY pp.producto.id, pp.producto.nombre
            ORDER BY SUM(pp.cantidad) DESC
            """)
    List<ProductoMasVendidoDto> obtenerProductosMasVendidos();
    /*
    Query explicada:
    - SELECT new gestionpedidos.dto.ProductoMasVendidoDto(pp.producto.nombre, SUM(pp.cantidad)) -> Por cada fila de resultado,
    construye directamente un objeto ProductoMasVendidoDto llamando a su constructor con los valores
    "pp.producto.nombre"(desde esta línea de pedido, ve al producto asociado, y coge su nombre) y
    "SUM(pp.cantidad)"(suma el campo cantidad de todas las filas que caigan en el mismo grupo), en este orden.
    - FROM PedidoProducto pp —> Voy a recorrer todas las filas de la tabla que corresponde a PedidoProducto
     y las voy a nombrar pp para referirme a ellas en el resto de la query.
    - GROUP BY pp.producto -> En vez de darme 50 filas sueltas, júntalas todas en un solo grupo por cada producto distinto
     y déjame aplicar SUM() dentro de cada grupo.
    - ORDER BY SUM(pp.cantidad) DESC -> Ordena el resultado de manera descendente.
    */
}
