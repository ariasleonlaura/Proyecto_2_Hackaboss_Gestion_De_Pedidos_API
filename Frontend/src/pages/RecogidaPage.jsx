import { useState } from "react";
import { usePedidosPorEstados } from "../hooks/usePedidosPorEstados";
import {
  buscarPedidoPorCodigo,
  cambiarEstadoDelPedido,
} from "../api/pedidosApi";
import { Loading } from "../components/common/Loading";
import ErrorMessage from "../components/common/ErrorMessage";
import { Euro, Package } from "lucide-react";
import "../styles/RecogidaPage.css";
import { toast } from "sonner";
import { ESTADOS_PEDIDO } from "../utils/constants";

// Clase extra de la tarjeta de búsqueda según el estado: solo PAGADO tiene
// estilo propio; LISTO y los estados no gestionables aquí (CREADO, PREPARACION,
// ENTREGADO) se quedan con el estilo base y solo se distinguen por el texto
const claseSegunEstado = (estado) => {
  if (estado === ESTADOS_PEDIDO.PAGADO) return "pedido-listo-card--pagado";
  return "";
};

// Cobro y entrega son los dos únicos saltos que gestiona Recogida:
// LISTO → PAGADO (cobrar) y PAGADO → ENTREGADO (entregar). Devuelve true si
// la petición fue bien para que la vista oculte la tarjeta de búsqueda
const handleAccion = async (id, estadoActual) => {
  try {
    const nuevoEstado = estadoActual === ESTADOS_PEDIDO.LISTO ? ESTADOS_PEDIDO.PAGADO : ESTADOS_PEDIDO.ENTREGADO;
    await cambiarEstadoDelPedido(id, { estado: nuevoEstado });
    toast.success(
      estadoActual === ESTADOS_PEDIDO.LISTO
        ? "¡Pedido cobrado!"
        : "¡Pedido entregado con éxito!",
    );
    return true;
  } catch {
    toast.error("Error al actualizar el pedido");
    return false;
  }
};

const RecogidaPage = () => {
  // Tras la acción no se llama a refetch: la lista se refresca sola con el
  // polling de usePedidosPorEstados (cada 15 s), que sigue corriendo aquí
  const { pedidos, loading, error } = usePedidosPorEstados([ESTADOS_PEDIDO.LISTO, ESTADOS_PEDIDO.PAGADO]);
  const [codigoBusqueda, setCodigoBusqueda] = useState("");
  const [mostrarPedidoEncontrado, setMostrarPedidoEncontrado] = useState(false);
  const [pedidoEncontrado, setPedidoEncontrado] = useState(null);
  // Solo LISTO y PAGADO se gestionan desde esta pantalla; un pedido encontrado
  // por código en otro estado se muestra en solo lectura (sin botón de acción)
  const puedeGestionarse = [
    ESTADOS_PEDIDO.LISTO,
    ESTADOS_PEDIDO.PAGADO,
  ].includes(pedidoEncontrado?.estado);

  const handleBuscar = async () => {
    // Se recorta una vez por si hubiese espacios en blanco por error y se usa en todo el método
    const codigo = codigoBusqueda.trim();

    if (!codigo) {
      toast.warning("Introduce un código de pedido");
      setMostrarPedidoEncontrado(false);
      return;
    }
    try {
      const data = await buscarPedidoPorCodigo(codigo);
      setPedidoEncontrado(data);
      setMostrarPedidoEncontrado(true);
      toast.success(`Pedido encontrado: ${data.codigo}`);
    } catch {
      toast.error("Pedido no encontrado con ese código");
      setMostrarPedidoEncontrado(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="recogida-container">
      <h2>Pantalla de Cobro y Recogida</h2>

      <div className="busqueda-bar">
        <input
          type="text"
          placeholder="Buscar por código del pedido (ej. PED-AB12CD34)"
          value={codigoBusqueda}
          onChange={(e) => setCodigoBusqueda(e.target.value)}
          className="busqueda-input"
        />
        <button className="btn-buscar" onClick={handleBuscar}>
          Buscar
        </button>
      </div>

      {/* Modo: solo muestra el pedido encontrado, oculta la lista */}
      {mostrarPedidoEncontrado && pedidoEncontrado && (
        <div
          className={`pedido-listo-card pedido-listo-card--busqueda ${claseSegunEstado(pedidoEncontrado.estado)}`}
        >
          <h1>{pedidoEncontrado.codigo}</h1>
          <p className="pedido-listo-card__total">
            Total: <strong>{pedidoEncontrado.total.toFixed(2)} €</strong>
          </p>
          <div className="acciones-card">
            {puedeGestionarse ? (
              <button
                className="btn-entregar"
                onClick={async () => {
                  const exito = await handleAccion(
                    pedidoEncontrado.id,
                    pedidoEncontrado.estado,
                  );
                  if (exito) setMostrarPedidoEncontrado(false);
                }}
              >
                {pedidoEncontrado.estado === ESTADOS_PEDIDO.LISTO
                  ? "💶 Cobrar Pedido"
                  : "Entregado al Cliente ✓"}
              </button>
            ) : (
              <p>
                Estado: {pedidoEncontrado.estado} (no se gestiona desde
                Recogida)
              </p>
            )}
            <button
              className="btn-cancelar-busqueda"
              onClick={() => setMostrarPedidoEncontrado(false)}
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      {/* Lista normal de pedidos por estados (visible cuando no hay búsqueda activa) */}
      <p>Los siguientes pedidos están pendientes de pago o entrega:</p>

      <div className="pedidos-listos-grid">
        {pedidos.length === 0 ? (
          <p className="pedidos-listos-grid__vacio">
            No hay pedidos en esta zona en este momento.
          </p>
        ) : (
          pedidos.map((pedido) => (
            <div
              key={pedido.id}
              className={`pedido-listo-card ${pedido.estado === ESTADOS_PEDIDO.PAGADO ? "pedido-listo-card--pagado" : "pedido-listo-card--listo"}`}
            >
              <h1>{pedido.codigo}</h1>
              <p className="pedido-listo-card__total">
                Total: <strong>{pedido.total.toFixed(2)} €</strong>
              </p>
              <button
                className="btn-entregar"
                onClick={() => handleAccion(pedido.id, pedido.estado)}
              >
                {pedido.estado === ESTADOS_PEDIDO.LISTO ? (
                  <>
                    <Euro size={16} /> Cobrar Pedido
                  </>
                ) : (
                  <>
                    <Package size={16} /> Entregado al Cliente
                  </>
                )}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default RecogidaPage;
