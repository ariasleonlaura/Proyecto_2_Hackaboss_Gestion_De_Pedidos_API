import { useState, useEffect, useCallback } from "react";
import { listarProductos } from "../api/productosApi";

// Los parámetros son opcionales. Si no se pasan, listarProductos devuelve todos los productos
export function useProductos(activo, categoriaId, orden, tipoOrden) {
    // Estado que almacena la lista de productos recibida del backend
    const [productos, setProductos] = useState([]);

    // Controla si los datos están cargando para mostrar un spinner o mensaje de espera
    const [loading, setLoading] = useState(true);

    // Almacena el mensaje de error si la llamada al backend falla
    const [error, setError] = useState(null);

    // useCallback memoriza fetchProductos y solo la recrea si cambian los filtros
    // Sin esto, se crearía una función nueva en cada render provocando un bucle infinito
    const fetchProductos = useCallback(async () => {
        try {
            // Pasamos los filtros a listarProductos. Los que sean undefined se ignoran en la URL
            const data = await listarProductos(activo, categoriaId, orden, tipoOrden);
            setProductos(data);
            setError(null);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }, [activo, categoriaId, orden, tipoOrden]); // fetchProductos solo cambia si cambian los filtros

    useEffect(() => {
        fetchProductos();
    }, [fetchProductos]); // se ejecuta cuando fetchProductos cambia, es decir cuando cambian los filtros

    // Devolvemos los tres estados para que cualquier componente que use este hook
    // pueda acceder a los productos, saber si está cargando y gestionar errores
    return { productos, loading, error };
}