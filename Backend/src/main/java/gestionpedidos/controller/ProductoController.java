package gestionpedidos.controller;

import gestionpedidos.dto.CrearProductoDto;
import gestionpedidos.dto.ProductoDto;
import gestionpedidos.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor // Lombok genera el constructor con los campos final
public class ProductoController {

    private final ProductoService productoService;

    // Crear
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearProducto(@Valid @RequestBody CrearProductoDto dto) {
        ProductoDto productoDto = productoService.crearProducto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Producto creado correctamente",
                "data", productoDto));

    }

    // List con filtros
    @GetMapping
    public ResponseEntity<List<ProductoDto>> listarProductos(
            // Es un filtro optcional del  estado del producto
            // Si es null devuelve activos e inactivos
            // Si es true solo activos
            // Si es false solo inactivos
            @RequestParam(required = false) Boolean activo,
            // Es un filtro opcional de la categoría del producto
            // Se usa el ID de la categoría
            // Si es null no filtra por categoría
            @RequestParam(required = false) Long categoriaId,
            // El orden es opcional:
            //Se puede ordenar porprecio, nombre o categoria
            // Si es null ordena por nombre por defecto
            @RequestParam(required = false) String orden,
            // Se puede elegir el tipo de ordenacion:
            // ASC  ascendente
            // DESC descendente
            // Si es null ASC por defecto
            @RequestParam(required = false) String tipoOrden
    ) {
        List<ProductoDto> lista = productoService.listarProductos(activo, categoriaId, orden, tipoOrden);
        return ResponseEntity.ok(lista);
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody CrearProductoDto dto) {
        ProductoDto producto = productoService.actualizarProducto(id, dto);
        return ResponseEntity.ok(Map.of("mensaje", "Producto actualizado correctamente",
                        "data", producto));
    }

    // Desactivar producto. Borrado logico. O activar
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Map<String, String>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        productoService.cambiarEstado(id, activo);
        return ResponseEntity.ok(Map.of(
                "mensaje",
                activo ? "Producto activado" : "Producto desactivado"));
    }
}

