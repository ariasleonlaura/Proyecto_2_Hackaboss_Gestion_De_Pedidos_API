import { API_URL } from "../utils/constants";
import { fetchJSON } from "./fetchHelper";

const ESTADISTICAS_URL = `${API_URL}/estadisticas`;

export async function obtenerProductosMasVendidos() {
    return fetchJSON(`${ESTADISTICAS_URL}/productos-mas-vendidos`);
}
