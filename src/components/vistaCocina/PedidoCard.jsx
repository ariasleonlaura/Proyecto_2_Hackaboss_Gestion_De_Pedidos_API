import { useState } from "react";
import { cambiarEstadoDelPedido } from "../../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../../utils/constants";

export default function PedidoCard({ pedido, onAvanzar }) {
  //el botón esta trabajando?
  const [advancing, setAdvancing] = useState(false);

  async function handleAvanzar() {
    setAdvancing(true);
    try {
      await onAvanzar(pedido.id);
    } finally {
      setAdvancing(false);
    }
  }

  const labelBoton =
    pedido.estado === ESTADOS_PEDIDO.CREADO
      ? "▶ Iniciar preparación"
      : "✔ Marcar como listo";

  return (
    <div className="pedido-card">
      <header className="pedido-card__header">
        <span className="pedido-card__codigo">{pedido.codigo}</span>
        <span className={`pedido-card__badge pedido-card__badge--${pedido.estado.toLowerCase()}`}>
          {pedido.estado}
        </span>
      </header>

      <ul className="pedido-card__productos">
        {pedido.productos?.map((p) => (
          <li key={p.productoId} className="pedido-card__producto">
            <span className="pedido-card__cantidad">{p.cantidad}×</span>
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
