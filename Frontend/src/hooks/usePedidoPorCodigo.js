import { useState, useEffect } from "react";
import { buscarPedidoPorCodigo } from "../api/pedidosApi";

export function usePedidoPorCodigo(codigo) {
  const [pedido, setPedido] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!codigo) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPedido(null);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);
    buscarPedidoPorCodigo(codigo)
      .then((data) => {
        setPedido(data);
        setError(null);
      })
      .catch((err) => {
        setPedido(null);
        setError(err.message || "Error al buscar el pedido");
      })
      .finally(() => setLoading(false));
  }, [codigo]);

  return { pedido, loading, error };
}