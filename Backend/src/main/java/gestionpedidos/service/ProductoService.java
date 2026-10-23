package gestionpedidos.service;

import gestionpedidos.dto.CrearProductoDto;
import gestionpedidos.dto.ProductoDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.Categoria;
import gestionpedidos.model.Producto;
import gestionpedidos.repository.CategoriaRepository;
import gestionpedidos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    // Crear producto
    public ProductoDto crearProducto(CrearProductoDto dto) {
        // Evita productos con el mismo nombre
        if (productoRepository.existsByNombre(dto.getNombre())) {
            throw new PedidoStateException("Ya existe un producto con el nombre " + dto.getNombre());
        }

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setActivo(dto.getActivo());
        producto.setCategoria(categoria);

        Producto guardado = productoRepository.save(producto);
        return toDto(guardado);
    }

    // Listar
    public List<ProductoDto> listarProductos(Boolean activo, Long categoriaId, String orden, String tipoOrden) {

        List<Producto> productos = productoRepository.findAll();
        Stream<Producto> stream = productos.stream()
                // No filtramos a no ser que queramos seguro los activos.
                .filter(p -> activo == null || p.isActivo() == activo)
                // Filtramos por categoria
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId));
        // Por defecto ordena por nombre
        Comparator<Producto> comparator = Comparator.comparing(Producto::getNombre);
        // Hacemos un menu para elegir forma de ordenar
        if (orden != null) {
            switch (orden) {
                case "precio":
                    // Ordenamos por precio
                    comparator = Comparator.comparing(Producto::getPrecio);
                    break;
                // Ordenamos por categoria
                case "categoria":
                    comparator = Comparator.comparing(p -> p.getCategoria().getNombre());
                    break;
                // Ordenamos por nombre
                case "nombre":
                default:
                    comparator = Comparator.comparing(Producto::getNombre);
            }
        }
        // Por defecto ordena en ascendente, a no ser que marquemos DESC, que ordena en descendente.
        if ("DESC".equalsIgnoreCase(tipoOrden)) {
            comparator = comparator.reversed();
        }
        return stream
                .sorted(comparator)
                .map(this::toDto)
                .toList();
    }

    // actualizarProducto recibe un CrearProductoDto para no duplicar
    public ProductoDto actualizarProducto(Long id, CrearProductoDto dto) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setActivo(dto.getActivo());
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        producto.setCategoria(categoria);
        Producto guardado = productoRepository.save(producto);
        return toDto(guardado);
    }

    // Desactiva un producto(borrado logico) o activa.
    public void cambiarEstado(Long id, boolean activo) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto con ID " + id + " no existe"));
        producto.setActivo(activo);
        productoRepository.save(producto);
    }

    // Conversor a DTO
    private ProductoDto toDto(Producto producto) {
        ProductoDto dto = new ProductoDto();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setPrecio(producto.getPrecio());
        dto.setActivo(producto.isActivo());
        if (producto.getCategoria() != null) {
            dto.setNombreCategoria(producto.getCategoria().getNombre());
        }
        return dto;
    }
}

