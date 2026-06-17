import { API_URL } from "../utils/constants";

const PRODUCTOS_URL = `${API_URL}/productos`;

export async function crearProducto(dto) {
    const response = await fetch(PRODUCTOS_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });

    return response.json();
}

export async function listarProductos(activo, categoriaId, orden, tipoOrden) {
    // Con URLSearcParams se construyen query params automáticamente: 
    // "activo=true" y "orden=precio" genera "activo=true&orden=precio"
    const params = new URLSearchParams();

    // activo puede ser false (producto inactivo), por eso comprobamos undefined y null explícitamente
    if (activo !== undefined && activo !== null) params.append("activo", activo);
    if (categoriaId) params.append("categoriaId", categoriaId);
    if (orden) params.append("orden", orden);
    if (tipoOrden) params.append("tipoOrden", tipoOrden);

    // Si params tiene algo, construimos la URL con los query params
    // Si no viene ningún filtro, usamos la URL base y el backend devuelve todos los productos
    // Ejemplo con filtros:  http://localhost:8080/api/productos?activo=true&orden=precio
    // Ejemplo sin filtros:  http://localhost:8080/api/productos
    const url = params.toString() ? `${PRODUCTOS_URL}?${params}` : PRODUCTOS_URL;
    const response = await fetch(url);

    return response.json();
}

export async function actualizarProducto(id, dto) {
    const response = await fetch(`${PRODUCTOS_URL}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dto)
    });

    return response.json();
}

export async function cambiarEstadoDeProducto(id, activo) {
    const response = await fetch(`${PRODUCTOS_URL}/${id}/estado?activo=${activo}`,
        { method: "PATCH" });

    return response.json();
}