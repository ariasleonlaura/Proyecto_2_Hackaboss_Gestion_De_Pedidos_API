import { usePedidos } from "../hooks/usePedidos";
import { cambiarEstadoDelPedido } from "../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../utils/constants";
import ListaPedidos from "../components/VistaCocina/ListaPedidos";

export default function CocinaPage() {
  const {
    pedidosCreados,
    pedidosEnPreparacion,
    loading,
    error,
    lastUpdated,
    refetch,
  } = usePedidos();

  async function handleAvanzar(pedidoId) {
    //este pedido esta en la lista de pedidos creados?
    const estaCreado = pedidosCreados.some((p) => p.id === pedidoId);
    const nuevoEstado = estaCreado ? ESTADOS_PEDIDO.PREPARACION : ESTADOS_PEDIDO.LISTO;
    await cambiarEstadoDelPedido(pedidoId, { estado: nuevoEstado });
    await refetch();
  }

  return (
    <div className="cocina-page">
      <header className="cocina-header">
        <h1 className="cocina-header__titulo">Vista Cocina</h1>
        <div className="cocina-header__controls">
          {lastUpdated && (
            <span className="cocina-header__timestamp">
              Actualizado: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          <button
            className="cocina-header__btn-refetch"
            onClick={refetch}
            disabled={loading}
          >
            {loading ? "Cargando…" : "⟳ Refrescar"}
          </button>
        </div>
      </header>

      {error && (
        <div className="cocina-error">
          ⚠ Error al cargar pedidos: {error}
        </div>
      )}

      <div className="cocina-grid">
        <ListaPedidos
          titulo="Nuevos"
          pedidos={pedidosCreados}
          onAvanzar={handleAvanzar}
          colorClass="cocina-columna--nuevos"
        />
        <ListaPedidos
          titulo="En preparación"
          pedidos={pedidosEnPreparacion}
          onAvanzar={handleAvanzar}
          colorClass="cocina-columna--preparacion"
        />
      </div>
    </div>
  );
}
