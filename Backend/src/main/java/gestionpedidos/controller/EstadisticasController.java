package gestionpedidos.controller;

import gestionpedidos.dto.ProductoMasVendidoDto;
import gestionpedidos.dto.TerminalMasUtilizadaDto;
import gestionpedidos.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping("/ranking-terminales")
    public ResponseEntity<List<TerminalMasUtilizadaDto>> obtenerRankingDeTerminales() {
        return ResponseEntity.ok(estadisticasService.obtenerRankingDeTerminales());
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoDto>> obtenerProductosMasVendidos() {
        return ResponseEntity.ok(estadisticasService.obtenerProductosMasVendidos());
    }
}
