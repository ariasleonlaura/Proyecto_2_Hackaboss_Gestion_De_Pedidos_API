package gestionpedidos.controller;

import gestionpedidos.dto.*;
import gestionpedidos.model.EstadoPedido;
import gestionpedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor // Lombok genera el constructor con los campos final
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoDto>> listarPedidosYPorEstado(@RequestParam(required = false) EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.listarPedidos(estado));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarPedido(@Valid @RequestBody CrearPedidoDto crearPedidoDto) {
        PedidoDto pedidoDto = pedidoService.registrarPedido(crearPedidoDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Pedido registrado correctamente",
                "data", pedidoDto));
    }

    @PostMapping("/{pedidoId}/productos")
    public ResponseEntity<ProductosPedidoDto> agregarProductoAPedido(@PathVariable Long pedidoId, @Valid @RequestBody PedidoProductoRequestDto dto) {
        return ResponseEntity.ok(pedidoService.agregarProductosAPedido(pedidoId, dto));
    }

    @DeleteMapping("/{pedidoId}/eliminar-producto/{productoId}")
    public ResponseEntity<ProductosPedidoDto> eliminarProductoDePedido(@PathVariable Long pedidoId, @PathVariable Long productoId, @RequestParam Integer cantidad) {
        ProductosPedidoDto resultado = pedidoService.eliminarProductoDePedido(pedidoId, productoId, cantidad);
        if (resultado == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PedidoDto> obtenerPedidoPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(pedidoService.obtenerPedidoPorCodigo(codigo));
    }

    @PatchMapping("/{pedidoId}/estado")
    public ResponseEntity<PedidoDto> cambiarEstadoDelPedido(@PathVariable Long pedidoId, @RequestBody EstadoPedidoRequestDto dto) {
        return ResponseEntity.ok(pedidoService.cambiarEstadoDelPedido(pedidoId, dto.getEstado()));
    }
}
