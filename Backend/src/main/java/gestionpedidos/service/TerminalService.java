package gestionpedidos.service;

import gestionpedidos.dto.CrearTerminalDto;
import gestionpedidos.dto.TerminalDto;
import gestionpedidos.exception.PedidoStateException;
import gestionpedidos.exception.ResourceNotFoundException;
import gestionpedidos.model.Terminal;
import gestionpedidos.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminalService {
    private final TerminalRepository terminalRepository;

    // Método para listar todas las terminales
    public List<TerminalDto> listarTerminales() {
        return terminalRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    // Método para buscar una terminal mediante su Id
    public TerminalDto buscarTerminalPorId(Long id) {
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La terminal con ID " + id + " no existe"));
        return toDto(terminal);
    }

    // Método para guardar una nueva terminal. Recibe un dto y devuelve otro de salida
    public TerminalDto crearTerminal(CrearTerminalDto nuevaTerminalDto) {
        // Evita terminales con el mismo nombre
        if (terminalRepository.existsByNombre(nuevaTerminalDto.getNombre())) {
            throw new PedidoStateException("Ya existe una terminal con el nombre " + nuevaTerminalDto.getNombre());
        }

        Terminal terminal = new Terminal();
        terminal.setNombre(nuevaTerminalDto.getNombre());
        Terminal guardada = terminalRepository.save(terminal);
        return toDto(guardada);
    }


    // *** MÉTODO DE MAPEO ***
    private TerminalDto toDto(Terminal terminal) {
        TerminalDto dto = new TerminalDto();
        dto.setId(terminal.getId());
        dto.setNombre(terminal.getNombre());
        return dto;
    }

}
