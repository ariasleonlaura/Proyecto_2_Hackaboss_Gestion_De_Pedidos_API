import { API_URL } from "../utils/constants";

const PEDIDOS_URL = `${API_URL}/pedidos`;

export async function registrarPedido(crearPedidoDto) {
    const response = await fetch(PEDIDOS_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(crearPedidoDto)
    });

    return response.json();
}

export async function agregarProductoAPedido(pedidoId, dto) {
    const response = await fetch(`${PEDIDOS_URL}/${pedidoId}/productos`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });

    return response.json();
}

export async function eliminarProductoDePedido(pedidoId, productoId, cantidad) {
    const response = await fetch(`${PEDIDOS_URL}/${pedidoId}/productos/${productoId}?cantidad=${cantidad}`, 
        {method: "DELETE"});

        return response.json();
}

export async function cambiarEstadoDelPedido(pedidoId, dto) {
    const response = await fetch(`${PEDIDOS_URL}/${pedidoId}/estado`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });

    return response.json();
}

export async function buscarPedidoPorCodigo(codigo) {
    const response = await fetch(`${PEDIDOS_URL}/codigo/${codigo}`);

    return response.json();
}

export async function listarPedidosYPorEstado(estado) {
    // En función de si recibe el query param que no es obligatorio, la url es una u otra
    const url = estado ? `${PEDIDOS_URL}?estado=${estado}` : PEDIDOS_URL;

    const response = await fetch(url);

    return response.json();
}