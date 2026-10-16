import { toast } from 'sonner';
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';
import { usePedidosPorEstados } from "../hooks/usePedidosPorEstados";
import { cambiarEstadoDelPedido } from "../api/pedidosApi";
import { ESTADOS_PEDIDO } from "../utils/constants";
import ListaPedidos from "../components/vistaCocina/ListaPedidos";
import { RotateCw } from "lucide-react";
import "../styles/CocinaPage.css";

export default function CocinaPage() {
  // Traemos las listas de pedidos, el estado de carga, errores,
  // la hora del último refresco y la función para refrescar manualmente
  const { pedidos, loading, error, lastUpdated, refetch } = usePedidosPorEstados(['CREADO', 'PREPARACION']);

  const pedidosCreados = pedidos.filter(p => p.estado === ESTADOS_PEDIDO.CREADO);
  const pedidosEnPreparacion = pedidos.filter(p => p.estado === ESTADOS_PEDIDO.PREPARACION);

  // Avanza el estado de un pedido al siguiente paso:
  // CREADO → PREPARACION → LISTO
  // Filtra por estado del objeto pedido
  async function handleAvanzar(pedidoId, estadoActual) {
    const nuevoEstado = estadoActual === ESTADOS_PEDIDO.CREADO
      ? ESTADOS_PEDIDO.PREPARACION
      : ESTADOS_PEDIDO.LISTO;
    try {
      await cambiarEstadoDelPedido(pedidoId, { estado: nuevoEstado });
      toast.success("Estado actualizado");
      await refetch(); // recargamos las listas para que el pedido desaparezca de la columna actual
    } catch {
      toast.error("Error al actualizar el estado del pedido");
    }
  }

  // Mientras carga cocina, mostramos el spinner
  if (loading) return <Loading />;

  return (
    <div className="cocina-page">
      <header className="cocina-header">
        <h1 className="cocina-header__titulo">Pantalla de Cocina</h1>
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
            {loading ? (
              "Cargando…"
            ) : (
              <>
                <RotateCw size={16} /> Refrescar
              </>
            )}
          </button>
        </div>
      </header>

      {/* Si hay error al cargar los pedidos, mostramos el componente estándar de error */}
      {error && <ErrorMessage message={error} />}

      {/* Dos columnas: una por cada estado que gestiona cocina */}
      <div className="cocina-grid">
        <ListaPedidos
          titulo="En espera"
          pedidos={pedidosCreados}
          onAvanzar={handleAvanzar}
          onRefetch={refetch}
          colorClass="cocina-columna--espera"
        />
        <ListaPedidos
          titulo="En preparación"
          pedidos={pedidosEnPreparacion}
          onAvanzar={handleAvanzar}
          onRefetch={refetch}
          colorClass="cocina-columna--preparacion"
        />
      </div>
    </div>
  );
}