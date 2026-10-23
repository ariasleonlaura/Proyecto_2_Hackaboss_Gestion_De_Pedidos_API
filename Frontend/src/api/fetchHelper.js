// Wrapper centralizado alrededor de fetch que gestiona response.ok y parsea el JSON.
// Todas las funciones de api/ lo usan para que sea imposible olvidarse de comprobar errores.
export async function fetchJSON(url, options = {}, timeoutMs = 8000) {
    // AbortController permite cancelar el fetch si tarda demasiado
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
        const response = await fetch(url, {
            ...options,
            signal: controller.signal
        });

        if (response.status === 204) {
            return null;
        }

        const data = await response.json();

        if (!response.ok) {
            // El backend puede devolver el mensaje de error con distintas claves según el tipo de error
            const mensaje =
                data["error"] ||
                data["Error: "] ||
                data["Error inesperado: "] ||
                data["Error"] ||
                "Error desconocido";
            throw new Error(mensaje);
        }

        return data;
        
    } catch (e) {
        // Si el fetch fue cancelado por timeout, lanzamos un mensaje claro
        if (e.name === "AbortError") {
            throw new Error("El servidor no responde. Inténtalo de nuevo más tarde.", { cause: e });
        }
        throw e; // cualquier otro error se propaga igual
    } finally {
        clearTimeout(timeoutId); // limpiamos el timeout pase lo que pase
    }
}