import { useState } from "react";
import { ESTADOS_PEDIDO } from "../../utils/constants";
import PedidoLineaInput from "./PedidoLineaInput";
import { Play, Check } from "lucide-react";

export default function PedidoCard({ pedido, onAvanzar, onRefetch }) {
  // El botón esta trabajando?
  const [advancing, setAdvancing] = useState(false);

  async function handleAvanzar() {
    setAdvancing(true);
    try {
      await onAvanzar(pedido.id, pedido.estado);
    } finally {
      setAdvancing(false);
    }
  }

  const labelBoton =
    pedido.estado === ESTADOS_PEDIDO.CREADO ? (
      <>
        <Play size={16} /> Iniciar preparación
      </>
    ) : (
      <>
        <Check size={16} /> Marcar como listo
      </>
    );

  return (
    <div className={`pedido-card pedido-card--${pedido.estado}`}>
      <header className="pedido-card__header">
        <span className="pedido-card__codigo">{pedido.codigo}</span>
        <span className={`pedido-card__badge pedido-card__badge--${pedido.estado.toLowerCase()}`}>
          {pedido.estado}
        </span>
      </header>

      <ul className="pedido-card__productos">
        {pedido.productos?.map((p) => (
          <li key={p.productoId} className="pedido-card__producto">
            <PedidoLineaInput
              pedidoId={pedido.id}
              productoId={p.productoId}
              cantidadActual={p.cantidad}
              pedidoEstado={pedido.estado}
              onCambio={onRefetch}
            />
            <span className="pedido-card__nombre">{p.nombreProducto}</span>
          </li>
        ))}
      </ul>

      <button
        className="pedido-card__btn"
        onClick={handleAvanzar}
        disabled={advancing}
      >
        {advancing ? "Actualizando…" : labelBoton}
      </button>
    </div>
  );
}
