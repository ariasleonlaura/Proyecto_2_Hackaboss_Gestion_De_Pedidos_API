import { API_URL } from "../utils/constants";
import { fetchJSON } from "./fetchHelper";

const PEDIDOS_URL = `${API_URL}/pedidos`;

export async function registrarPedido(crearPedidoDto) {
    return fetchJSON(PEDIDOS_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(crearPedidoDto)
    });
}

export async function agregarProductoAPedido(pedidoId, dto) {
    return fetchJSON(`${PEDIDOS_URL}/${pedidoId}/productos`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });
}

export async function eliminarProductoDePedido(pedidoId, productoId, cantidad) {
    return fetchJSON(
        `${PEDIDOS_URL}/${pedidoId}/eliminar-producto/${productoId}?cantidad=${cantidad}`,
        { method: "DELETE" }
    );
}

export async function cambiarEstadoDelPedido(pedidoId, dto) {
    return fetchJSON(`${PEDIDOS_URL}/${pedidoId}/estado`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });
}

export async function buscarPedidoPorCodigo(codigo) {
    return fetchJSON(`${PEDIDOS_URL}/codigo/${codigo}`);
}

export async function listarPedidosYPorEstado(estado) {
    // En función de si recibe el query param que no es obligatorio, la url es una u otra
    const url = estado ? `${PEDIDOS_URL}?estado=${estado}` : PEDIDOS_URL;

    return fetchJSON(url);
}
