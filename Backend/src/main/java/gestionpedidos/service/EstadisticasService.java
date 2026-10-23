package gestionpedidos.service;

import gestionpedidos.dto.ProductoMasVendidoDto;
import gestionpedidos.dto.TerminalMasUtilizadaDto;
import gestionpedidos.repository.PedidoRepository;
import gestionpedidos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadisticasService {

    /* EstadisticasService no necesita la lógica de negocio de ProductoService ni TerminalService,
     solo la query cruda del repositorio */
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;

    public List<TerminalMasUtilizadaDto> obtenerRankingDeTerminales() {
        return pedidoRepository.obtenerRankingDeTerminales();
    }

    public List<ProductoMasVendidoDto> obtenerProductosMasVendidos() {
        return productoRepository.obtenerProductosMasVendidos();
    }
}
