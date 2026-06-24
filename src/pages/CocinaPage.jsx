import { toast } from 'sonner';
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';
import { usePedidos } from "../hooks/usePedidos";
import { cambiarEstadoDelPedido } from "../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../utils/constants";
import ListaPedidos from "../components/vistaCocina/ListaPedidos";
import "../styles/CocinaPage.css";

export default function CocinaPage() {
  // Traemos las dos listas de pedidos, el estado de carga, errores,
  // la hora del último refresco y la función para refrescar manualmente
  const {
    pedidosCreados,
    pedidosEnPreparacion,
    loading,
    error,
    lastUpdated,
    refetch,
  } = usePedidos();

  // Avanza el estado de un pedido al siguiente paso:
  // CREADO → PREPARACION → LISTO
  // Comprueba en qué lista está el pedido para saber a qué estado mandarlo
  async function handleAvanzar(pedidoId) {
    const estaCreado = pedidosCreados.some((p) => p.id === pedidoId);
    const nuevoEstado = estaCreado ? ESTADOS_PEDIDO.PREPARACION : ESTADOS_PEDIDO.LISTO;
    try {
      await cambiarEstadoDelPedido(pedidoId, { estado: nuevoEstado });
      toast.success("Estado actualizado");
      await refetch(); // recargamos las listas para que el pedido desaparezca de la columna actual
    } catch (err) {
      toast.error("Error al actualizar el estado del pedido");
    }
  }

  // Mientras carga cocina, mostramos el spinner
  if (loading) return <Loading />;

  return (
    <div className="cocina-page">
      <header className="cocina-header">
        <h1 className="cocina-header__titulo">Vista Cocina</h1>
        <div className="cocina-header__controls">
          {/* Solo mostramos la hora si ya se ha hecho al menos una carga */}
          {lastUpdated && (
            <span className="cocina-header__timestamp">
              Actualizado: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          {/* Botón de refresco manual; se desactiva mientras está cargando para evitar doble click */}
          <button
            className="cocina-header__btn-actualizar"
            onClick={refetch}
            disabled={loading}
          >
            {loading ? "Cargando…" : "⟳ Refrescar"}
          </button>
        </div>
      </header>

      {/* Si hay error al cargar los pedidos, mostramos el componente estándar de error */}
      {error && <ErrorMessage message={error} />}

      {/* Dos columnas: una por cada estado que gestiona cocina */}
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