import { useState, useEffect, useCallback } from "react";
import { listarTerminales } from "../api/terminalesApi";

// Hook que carga la lista de terminales disponibles desde el backend
export function useTerminales() {
    // Estado que almacena la lista de terminales recibida del backend
    const [terminales, setTerminales] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // useCallback memoriza fetchTerminales para que no se recree en cada render
    const fetchTerminales = useCallback(async () => {
        try {
            const data = await listarTerminales();
            setTerminales(data);
            setError(null);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }, []); // sin dependencias, no hay filtros que puedan cambiar

    useEffect(() => {
        fetchTerminales();
    }, [fetchTerminales]); // se ejecuta una vez al montar el componente

    // Devolvemos los tres estados para que cualquier componente que use este hook
    // pueda acceder a las terminales, saber si está cargando y gestionar errores
    return { terminales, loading, error };
}