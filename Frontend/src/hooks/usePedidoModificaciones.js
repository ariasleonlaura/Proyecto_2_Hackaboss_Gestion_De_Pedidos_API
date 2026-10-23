import { useState } from "react";
import { agregarProductoAPedido, eliminarProductoDePedido } from "../api/pedidosApi";
import { toast } from "sonner";

export function usePedidoModificaciones() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const incrementarCantidad = async (pedidoId, productoId) => {
    setLoading(true);
    setError(null);
    try {
      const dto = { productoId, cantidad: 1 }; // Incrementar en 1 unidad
      await agregarProductoAPedido(pedidoId, dto);
      toast.success("Unidad añadida al pedido");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al incrementar");
      toast.error("Error al incrementar unidad");
    } finally {
      setLoading(false);
    }
  };

  const decrementarCantidad = async (pedidoId, productoId, cantidadActual) => {
    setLoading(true);
    setError(null);
    try {
      const dto = { productoId, cantidad: 1 };
      await eliminarProductoDePedido(pedidoId, dto);
      if (cantidadActual <= 1) {
        toast.success("Línea eliminada del pedido");
      } else {
        toast.success("Unidad quitada del pedido");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al decrementar");
      toast.error("Error al quitar unidad");
    } finally {
      setLoading(false);
    }
  };

  return { loading, error, incrementarCantidad, decrementarCantidad };
}