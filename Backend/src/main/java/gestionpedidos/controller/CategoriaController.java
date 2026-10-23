package gestionpedidos.controller;

import gestionpedidos.dto.CategoriaDto;
import gestionpedidos.dto.CrearCategoriaDto;
import gestionpedidos.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor // Lombok genera el constructor con los campos final
public class CategoriaController {
    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearCategoria(@Valid @RequestBody CrearCategoriaDto nuevaCategoriaDto) {
        CategoriaDto categoriaDto = categoriaService.crearCategoria(nuevaCategoriaDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Categoría creada correctamente",
                "data", categoriaDto));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDto>> listarCategorias() {
        List<CategoriaDto> listaCategoriasDto = categoriaService.listarCategorias();
        return ResponseEntity.ok(listaCategoriasDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDto> obtenerPorId(@PathVariable Long id) {
        CategoriaDto categoriaDto = categoriaService.obtenerPorId(id);
        return ResponseEntity.ok(categoriaDto);
    }
}


