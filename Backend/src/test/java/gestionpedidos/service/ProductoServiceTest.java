package gestionpedidos.service;

import gestionpedidos.dto.CrearProductoDto;
import gestionpedidos.dto.ProductoDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.Categoria;
import gestionpedidos.model.Producto;
import gestionpedidos.repository.CategoriaRepository;
import gestionpedidos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;

    // *** MÉTODOS FACTORÍA ***

    private Categoria crearCategoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Producto crearProducto(Long id, String nombre, String precio, boolean activo, Categoria categoria) {
        return new Producto(id, nombre, new BigDecimal(precio), activo, categoria);
    }

    private CrearProductoDto crearCrearProductoDto(String nombre, String precio, boolean activo, Long categoriaId) {
        return new CrearProductoDto(nombre, new BigDecimal(precio), activo, categoriaId);
    }

    // *** TESTS ***

    // Test caso de éxito de crearProducto
    @Test
    void crearProductoDeberiaGuardarloCuandoNombreNoExiste() {
        // Arrange
        Categoria categoria = crearCategoria(1L, "Hamburguesas");
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, categoria.getId());

        when(productoRepository.existsByNombre(dto.getNombre())).thenReturn(false);
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        /* El objeto se construye dentro del método que se prueba y no hay forma de referenciarlo desde fuera
           por lo que se accede a él mediante thenAnswer(invocation) */
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act
        ProductoDto resultado = productoService.crearProducto(dto);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        // Assert
        assertThat(resultado.getNombre()).isEqualTo(dto.getNombre());
        assertThat(resultado.getPrecio()).isEqualByComparingTo(dto.getPrecio());
        assertThat(resultado.getNombreCategoria()).isEqualTo(categoria.getNombre());

        verify(productoRepository).save(any(Producto.class));
    }

    // Test caso de error de crearProducto. Nombre ya existente
    @Test
    void crearProductoDeberiaLanzarExcepcionCuandoNombreYaExiste() {
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, 1L);

        when(productoRepository.existsByNombre(dto.getNombre())).thenReturn(true);

        assertThatThrownBy(() -> productoService.crearProducto(dto))
                .isInstanceOf(PedidoStateException.class)
                .hasMessageContaining("Ya existe un producto con el nombre " + dto.getNombre());

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(productoRepository, never()).save(any());
    }

    // Test listarProductos: devuelve todos, ordenados por nombre por defecto, cuando no recibe argumentos
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorNombreCuandoNoRecibeArgumentos() {
        Producto producto1 = crearProducto(1L, "Patatas", "3.00", true, crearCategoria(1L, "Snacks"));
        Producto producto2 = crearProducto(2L, "Menú completo", "15.00", true, crearCategoria(2L, "Menús"));

        when(productoRepository.findAll()).thenReturn(List.of(producto1, producto2));

        List<ProductoDto> resultado = productoService.listarProductos(null, null, null, null);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        assertThat(resultado).hasSize(2);
        // Orden por defecto: alfabético por nombre -> "Menú completo" antes que "Patatas"
        assertThat(resultado.get(0).getId()).isEqualTo(producto2.getId());
        assertThat(resultado.get(1).getId()).isEqualTo(producto1.getId());
    }

    // Test listarProductos: filtra por categoría además de por estado activo
    @Test
    void listarProductosDeberiaDevolverTodosFiltradosPorActivoYCategoria() {
        Categoria hamburguesas = crearCategoria(1L, "Hamburguesas");
        Categoria bebidas = crearCategoria(2L, "Bebidas");

        Producto p1 = crearProducto(1L, "Hamburguesa clásica", "8.50", true, hamburguesas);
        Producto p2 = crearProducto(2L, "Hamburguesa vegana", "9.50", false, hamburguesas); // inactivo: no debería aparecer
        Producto p3 = crearProducto(3L, "Coca-Cola", "2.00", true, bebidas); // otra categoría: no debería aparecer

        when(productoRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        // Act: pedimos solo los activos de la categoría 1 (Hamburguesas)
        List<ProductoDto> resultado = productoService.listarProductos(true, hamburguesas.getId(), null, null);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo(p1.getNombre());
    }

    // Test listarProductos: ordena por precio ascendente por defecto
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorPrecioAscendentePorDefecto() {
        Producto barato = crearProducto(1L, "Patatas", "3.00", true, crearCategoria(1L, "Snacks"));
        Producto caro = crearProducto(2L, "Menú completo", "15.00", true, crearCategoria(2L, "Menús"));

        // Los devolvemos "desordenados" a propósito, para comprobar que el método sí ordena
        when(productoRepository.findAll()).thenReturn(List.of(caro, barato));

        List<ProductoDto> resultado = productoService.listarProductos(null, null, "precio", null);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        assertThat(resultado).extracting(ProductoDto::getNombre)
                .containsExactly(barato.getNombre(), caro.getNombre());
    }

    // Test listarProductos: ordena por precio, pero en descendente cuando se pide DESC
    @Test
    void listarProductosDeberiaDevolverTodosOrdenadosPorPrecioDescendenteCuandoSePide() {
        Producto barato = crearProducto(1L, "Patatas", "3.00", true, crearCategoria(1L, "Snacks"));
        Producto caro = crearProducto(2L, "Menú completo", "15.00", true, crearCategoria(2L, "Menús"));

        when(productoRepository.findAll()).thenReturn(List.of(barato, caro));

        List<ProductoDto> resultado = productoService.listarProductos(null, null, "precio", "DESC");

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        assertThat(resultado).extracting(ProductoDto::getNombre)
                .containsExactly(caro.getNombre(), barato.getNombre());
    }

    // Test caso de éxito de actualizarProducto
    @Test
    void actualizarProductoDeberiaGuardarloCuandoIdExiste() {
        // El producto tal y como estaba guardado ANTES de la actualización
        Categoria categoriaAntigua = crearCategoria(2L, "Sándwiches");
        Producto producto = crearProducto(1L, "Hamburguesa clásica", "8.50", true, categoriaAntigua);

        // La categoría que el DTO pide como NUEVA categoría
        Categoria categoriaNueva = crearCategoria(1L, "Hamburguesas");

        // Datos "nuevos" que llegan en el DTO para actualizar
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa premium", "12.50", true, categoriaNueva.getId());

        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(dto.getCategoriaId())).thenReturn(Optional.of(categoriaNueva));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductoDto resultado = productoService.actualizarProducto(producto.getId(), dto);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        assertThat(resultado.getNombre()).isEqualTo(dto.getNombre());
        assertThat(resultado.getPrecio()).isEqualByComparingTo(dto.getPrecio());
        assertThat(resultado.getNombreCategoria()).isEqualTo(categoriaNueva.getNombre());

        verify(productoRepository).save(any(Producto.class));
    }

    // Test caso de error de actualizarProducto. Producto no encontrado
    @Test
    void actualizarProductoDeberiaLanzarExcepcionCuandoProductoNoSeEncuentra() {
        Long id = 1L;
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, 1L);

        /* En los tests de "caso de error", el mock siempre debe simular el mismo id/nombre que usa el Act
        devolviendo el valor "vacío" correspondiente (Optional.empty(), false, etc.) — nunca un id distinto al que se llama */
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizarProducto(id, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto no encontrado");

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(productoRepository, never()).save(any());
    }

    // Test caso de error de actualizarProducto. Categoría no encontrada
    @Test
    void actualizarProductoDeberiaLanzarExcepcionCuandoCategoriaNoSeEncuentra() {
        Categoria categoriaProducto = crearCategoria(1L, "Hamburguesas");
        Producto producto = crearProducto(1L, "Hamburguesa clásica", "8.50", true, categoriaProducto);

        Categoria categoriaBuscada = crearCategoria(2L, "Patatas");
        CrearProductoDto dto = crearCrearProductoDto("Hamburguesa clásica", "8.50", true, categoriaBuscada.getId());

        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(categoriaBuscada.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizarProducto(producto.getId(), dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Categoría no encontrada");

        // Aquí falla en categoriaRepository, no en productoRepository
        // System.out.println(mockingDetails(categoriaRepository).printInvocations());

        verify(productoRepository, never()).save(any());
    }

    // Test caso de éxito de cambiarEstado
    @Test
    void cambiarEstadoDeberiaCambiarloCuandoExiste() {
        boolean nuevoEstado = false; // lo desactivamos, para que el cambio sea visible

        Producto producto = crearProducto(1L, "Hamburguesa clásica", "8.50", true, null); // estado inicial: activo

        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        /* El objeto esperado por save() ya existe como variable creada antes del Act, entonces se usa thenReturn */
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        productoService.cambiarEstado(producto.getId(), nuevoEstado);

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        // Mismo objeto, ya mutado. Al ser un método void, reutilizar la misma instancia que se mockeó sirve como comprobación
        assertThat(producto.isActivo()).isFalse();

        verify(productoRepository).save(producto);
    }

    // Test caso de error de cambiarEstado. Producto no existente
    @Test
    void cambiarEstadoDeberiaLanzarExcepcionCuandoNoExiste() {
        Long id = 1L;
        boolean activo = true;

        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.cambiarEstado(id, activo))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("El producto con ID " + id + " no existe");

        // System.out.println(mockingDetails(productoRepository).printInvocations());

        verify(productoRepository, never()).save(any());
    }
}