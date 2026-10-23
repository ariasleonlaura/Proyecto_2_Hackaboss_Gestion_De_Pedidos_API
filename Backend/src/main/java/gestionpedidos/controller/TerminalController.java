package gestionpedidos.controller;

import gestionpedidos.dto.CrearTerminalDto;
import gestionpedidos.dto.TerminalDto;
import gestionpedidos.service.TerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminales")
@RequiredArgsConstructor // Lombok genera el constructor con los campos final
public class TerminalController {
    private final TerminalService terminalService;

    @GetMapping
    public ResponseEntity<List<TerminalDto>> listarTerminales() {
        return ResponseEntity.ok(terminalService.listarTerminales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TerminalDto> terminalPorId(@PathVariable Long id) {
        return ResponseEntity.ok(terminalService.buscarTerminalPorId(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearTerminal(@Valid @RequestBody CrearTerminalDto nuevaTerminalDto) {
        TerminalDto terminalDto = terminalService.crearTerminal(nuevaTerminalDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Terminal creada correctamente",
                "data", terminalDto));
    }
}
