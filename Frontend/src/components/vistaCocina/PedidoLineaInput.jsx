import { useState } from "react";
import { agregarProductoAPedido, eliminarProductoDePedido } from "../../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../../utils/constants";
import { toast } from "sonner";
import { Minus, Plus } from "lucide-react";

export default function PedidoLineaInput({ pedidoId, productoId, cantidadActual, pedidoEstado, onCambio }) {
  const [modificando, setModificando] = useState(false);

  const modificarCantidad = async (nuevaCantidad) => {
    if (pedidoEstado !== ESTADOS_PEDIDO.CREADO) {
      toast.error("No se pueden modificar pedidos que ya avanzaron de estado");
      return;
    }
    if (nuevaCantidad === cantidadActual) return;
    setModificando(true);
    try {
      if (nuevaCantidad > cantidadActual) {
        await agregarProductoAPedido(pedidoId, { productoId, cantidad: nuevaCantidad - cantidadActual });
        toast.success(`Unidad(es) actualizada(s) en producto: +${nuevaCantidad - cantidadActual}`);
      } else {
        await eliminarProductoDePedido(pedidoId, productoId, cantidadActual - nuevaCantidad);
        toast.success(`Unidad(es) actualizada(s) en producto: -${cantidadActual - nuevaCantidad}`);
      }
      onCambio();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Error al actualizar");
    } finally {
      setModificando(false);
    }
  };

  /* Deshabilita los botones en los pedidos de la columa "En preparación" */
  if (pedidoEstado !== ESTADOS_PEDIDO.CREADO) {
    return (
      <div className="pedido-linea-input-disabled">
        <span className="pedido-linea-input-cantidad"> {cantidadActual} ×</span>
      </div>
    );
  }

  return (
    <div className="pedido-linea-input">
      <button
        className="pedido-linea-input-btn-plus"
        onClick={() => modificarCantidad(cantidadActual + 1)}
        disabled={modificando}
        title="Añadir unidad"
      >
        <Plus size={14} />
      </button>
      <button
        className="pedido-linea-input-btn-minus"
        onClick={() => modificarCantidad(Math.max(0, cantidadActual - 1))}
        disabled={modificando}
        title="Quitar unidad"
      >
        <Minus size={14} />
      </button>
      <span className="pedido-linea-input-cantidad">{cantidadActual} ×</span>
    </div>
  );
}
