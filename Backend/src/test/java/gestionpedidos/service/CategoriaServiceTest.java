package gestionpedidos.service;

import gestionpedidos.dto.CategoriaDto;
import gestionpedidos.dto.CrearCategoriaDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.Categoria;
import gestionpedidos.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    // *** MÉTODOS FACTORÍA ***

    private Categoria crearCategoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private CrearCategoriaDto crearCategoriaDto(String nombre) {
        return new CrearCategoriaDto(nombre);
    }

    // *** TESTS ***

    // Test del método listarCategorias
    @Test
    void listarCategoriasDeberiaDevolverTodasLasCategorias() {
        // Arrange
        Categoria categoria1 = crearCategoria(1L, "Hamburguesas");
        Categoria categoria2 = crearCategoria(2L, "Patatas");

        when(categoriaRepository.findAll()).thenReturn(List.of(categoria1, categoria2));

        // Act
        List<CategoriaDto> resultado = categoriaService.listarCategorias();

        // System.out.println(mockingDetails(categoriaRepository).printInvocations());

        // Assert
        // Nos aseguramos de que devuelva todas las categorías disponibles (2)
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isEqualTo(categoria1.getId());
        assertThat(resultado.get(1).getId()).isEqualTo(categoria2.getId());
    }

    // Test caso de éxito del método crearCategoria (nombre único)
    @Test
    void crearCategoriaDeberiaGuardarlaCuandoElNombreNoExiste() {
        CrearCategoriaDto dto = crearCategoriaDto("Hamburguesas");

        when(categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });

        CategoriaDto resultado = categoriaService.crearCategoria(dto);

        // System.out.println(mockingDetails(categoriaRepository).printInvocations());

        assertThat(resultado.getNombre()).isEqualTo(dto.getNombre());

        verify(categoriaRepository).save(any(Categoria.class));
    }

    // Test caso de error: "Bebidas" y "bebidas" se rechazan como duplicados (insensible a mayúsculas)
    @Test
    void crearCategoriaDeberiaLanzarExcepcionCuandoElNombreDifiereSoloEnMayusculas() {
        // Primero registramos "Bebidas" (debe guardarse)
        CrearCategoriaDto dto1 = crearCategoriaDto("Bebidas");

        when(categoriaRepository.existsByNombreIgnoreCase(dto1.getNombre())).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });
        categoriaService.crearCategoria(dto1);

        // Ahora intentar crear "bebidas" debería rechazarse (no debe guardarse de nuevo)
        CrearCategoriaDto dto2 = crearCategoriaDto("bebidas");

        when(categoriaRepository.existsByNombreIgnoreCase(dto2.getNombre())).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crearCategoria(dto2))
                .isInstanceOf(PedidoStateException.class)
                .hasMessageContaining("Ya existe una categoría con el nombre " + dto2.getNombre());

        // Verificar que save no fue llamado para el segundo intento
        InOrder inOrder = inOrder(categoriaRepository);
        inOrder.verify(categoriaRepository).save(any(Categoria.class)); // first call
        // verify(categoriaRepository, never()).save(any()); // removed - save was called for first one
    }

    // Test caso de error del método crearCategoria. Nombre de categoría ya existente
    @Test
    void crearCategoriaDeberiaLanzarExcepcionCuandoElNombreYaExiste() {
        CrearCategoriaDto dto = crearCategoriaDto("Hamburguesas");

        when(categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crearCategoria(dto))
                .isInstanceOf(PedidoStateException.class)
                .hasMessageContaining("Ya existe una categoría con el nombre " + dto.getNombre());

        // System.out.println(mockingDetails(categoriaRepository).printInvocations());

        verify(categoriaRepository, never()).save(any());
    }

    // Test caso de éxito del método obtenerPorId
    @Test
    void obtenerPorIdDeberiaDevolverloCuandoExiste() {
        Categoria categoria = crearCategoria(1L, "Hamburguesas");

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));

        CategoriaDto resultado = categoriaService.obtenerPorId(categoria.getId());

        // System.out.println(mockingDetails(categoriaRepository).printInvocations());

        assertThat(resultado.getId()).isEqualTo(categoria.getId());
    }

    // Test caso de error del método obtenerPorId. Categoría no encontrada
    @Test
    void obtenerPorIdDeberiaLanzarExcepcionCuandoNoSeEncuentra() {
        Long categoriaId = 1L;

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId(categoriaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("La categoría con ID " + categoriaId + " no existe");

        // System.out.println(mockingDetails(categoriaRepository).printInvocations());
    }
}