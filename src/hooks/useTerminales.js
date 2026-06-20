import { useState, useEffect, useCallback } from "react";
import { listarTerminales } from "../api/terminalesApi";

export function useTerminales() {
    const [terminales, setTerminales] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

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
    }, []);

    useEffect(() => {
        fetchTerminales();
    }, [fetchTerminales]);

    return { terminales, loading, error };
}