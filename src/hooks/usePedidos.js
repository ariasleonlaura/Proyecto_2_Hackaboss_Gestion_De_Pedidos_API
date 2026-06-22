import { useState, useEffect, useCallback } from "react";
import { listarPedidosYPorEstado } from "../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../utils/constants";
 
const POLL_INTERVAL_MS = 15000; // refresco automático cada 15 segundos
 
export function usePedidos() {
  const [pedidosCreados, setPedidosCreados] = useState([]);
  const [pedidosEnPreparacion, setPedidosEnPreparacion] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
 
  const fetchPedidos = useCallback(async () => {
    try {
      // Dos llamadas en paralelo: una por cada estado que muestra cocina
      const [creados, enPreparacion] = await Promise.all([
        listarPedidosYPorEstado(ESTADOS_PEDIDO.CREADO),
        listarPedidosYPorEstado(ESTADOS_PEDIDO.PREPARACION),
      ]);
      setPedidosCreados(creados);
      setPedidosEnPreparacion(enPreparacion);
      setError(null);
      setLastUpdated(new Date());
    } catch (err) {
      setError("Servidor no disponible. Inténtalo de nuevo más tarde.");
    } finally {
      setLoading(false);
    }
  }, []);
 
  // Carga inicial
  useEffect(() => {
    fetchPedidos();
  }, [fetchPedidos]);
 
  // Polling automático cada 15 s
  useEffect(() => {
    const interval = setInterval(fetchPedidos, POLL_INTERVAL_MS);
    return () => clearInterval(interval); // limpia al desmontar
  }, [fetchPedidos]);
 
  return {
    pedidosCreados,
    pedidosEnPreparacion,
    loading,
    error,
    lastUpdated,
    refetch: fetchPedidos, // para el botón manual
  };
}