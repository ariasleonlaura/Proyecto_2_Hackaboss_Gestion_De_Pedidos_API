package gestionpedidos.service;

import gestionpedidos.dto.CategoriaDto;
import gestionpedidos.dto.CrearCategoriaDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.Categoria;
import gestionpedidos.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // Lombok genera el constructor con los campos final
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // Listar categorias
    public List<CategoriaDto> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    // Crear categorias
    public CategoriaDto crearCategoria(CrearCategoriaDto crearCategoriaDto) {
        // Evita categorías con el mismo nombre
        if (categoriaRepository.existsByNombre(crearCategoriaDto.getNombre())) {
            throw new PedidoStateException("Ya existe una categoría con el nombre " + crearCategoriaDto.getNombre());
        }

        Categoria nuevaCategoria = new Categoria();
        nuevaCategoria.setNombre(crearCategoriaDto.getNombre());
        Categoria categoriaGuardada = categoriaRepository.save(nuevaCategoria);
        return toDto(categoriaGuardada);    // Este dto devolvemos
    }

    // Obtener categorias por id
    public CategoriaDto obtenerPorId(Long id) {
        return toDto(categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La categoría con ID " + id + " no existe")));
    }

    // *** MÉTODOS DE MAPEO ***

    private CategoriaDto toDto(Categoria categoria) {
        CategoriaDto dto = new CategoriaDto();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        return dto;
    }
}
