import { API_URL } from "../utils/constants";
import { fetchJSON } from "./fetchHelper";

const TERMINALES_URL = `${API_URL}/terminales`;

export async function listarTerminales() {
    return fetchJSON(`${TERMINALES_URL}`);
}