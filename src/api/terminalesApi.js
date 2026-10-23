import { API_URL } from "../utils/constants";

const TERMINALES_URL = `${API_URL}/terminales`;

export async function listarTerminales() {
    const response = await fetch(`${TERMINALES_URL}`);

    return response.json();
}

export async function buscarTerminalPorId(id) {
    const response = await fetch(`${TERMINALES_URL}/${id}`);

    return response.json();
}

export async function crearTerminal(nuevaTerminalDto) {
    const response = await fetch(`${TERMINALES_URL}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(nuevaTerminalDto)
    });

    return response.json();
}