import { useState, useEffect, useCallback } from "react";
import { listarPedidosYPorEstado } from "../api/pedidosApi";

const POLL_INTERVAL_MS = 15000; // refresco automático cada 15 segundos

// Hook genérico que carga pedidos de uno o varios estados y los refresca cada 10s.
// Recibe un array de estados: usePedidosPorEstados(['CREADO', 'PREPARACION'])
export function usePedidosPorEstados(estados) {
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  // Quien llama pasa un array literal, que es un objeto NUEVO en cada render. Si fuera la
  // dependencia, fetchPedidos cambiaría en cada render y el efecto volvería a lanzar la
  // petición sin parar. Con su contenido en texto ("CREADO,PREPARACION") es estable.
  const clave = estados.join(",");

  const fetchPedidos = useCallback(async () => {
    try {
      // Conserva el comportamiento de antes si alguna vez se pasa un array vacío: sin peticiones, en vez de pedir todos los pedidos.
      const lista = clave ? clave.split(",") : [];
      // Lanza una llamada por cada estado en paralelo y fusiona los resultados
      const listas = await Promise.all(lista.map((estado) => listarPedidosYPorEstado(estado)));
      setPedidos(listas.flat());
      setError(null);
      setLastUpdated(new Date());
    } catch {
      setError("Servidor no disponible. Inténtalo de nuevo más tarde.");
    } finally {
      setLoading(false);
    }
  }, [clave]);

  // Polling automático cada 15 s
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchPedidos();
    const interval = setInterval(fetchPedidos, POLL_INTERVAL_MS);
    return () => clearInterval(interval); // limpia al desmontar
  }, [fetchPedidos]);

  return { pedidos, loading, error, lastUpdated, refetch: fetchPedidos };
}