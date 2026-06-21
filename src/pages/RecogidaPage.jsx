import { useState, useEffect } from 'react';
import { listarPedidosYPorEstado, cambiarEstadoDelPedido } from '../api/pedidosApi';
import { Loading } from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import '../styles/RecogidaPage.css';

const RecogidaPage = () => {
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Traemos TANTO los que están listos para cobrar, como los que están pagados y listos para entregar
  const fetchPedidos = async () => {
    try {
      const [listos, pagados] = await Promise.all([
        listarPedidosYPorEstado('LISTO'),
        listarPedidosYPorEstado('PAGADO')
      ]);
      // Los juntamos todos en la misma pantalla
      setPedidos([...listos, ...pagados]);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPedidos();
    const interval = setInterval(fetchPedidos, 15000);
    return () => clearInterval(interval);
  }, []);

  // Función dinámica: si está LISTO lo pasa a PAGADO. Si está PAGADO lo pasa a ENTREGADO.
  const handleAccion = async (id, estadoActual) => {
    try {
      const nuevoEstado = estadoActual === 'LISTO' ? 'PAGADO' : 'ENTREGADO';
      await cambiarEstadoDelPedido(id, { estado: nuevoEstado });
      fetchPedidos(); 
    } catch (err) {
      alert("Error al actualizar el pedido: " + err.message);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="recogida-container">
      <h2>Pantalla de Recogida y Cobro</h2>
      <p>Pedidos pendientes de pago o entrega:</p>
      
      <div className="pedidos-listos-grid">
        {pedidos.length === 0 ? (
          <p style={{ gridColumn: '1 / -1', fontSize: '1.5rem', color: '#666' }}>
            No hay pedidos en esta zona en este momento.
          </p>
        ) : (
          pedidos.map(pedido => (
            <div 
              key={pedido.id} 
              // Le añadimos una clase extra si ya está pagado para poder cambiarle el color en CSS
              className={`pedido-listo-card ${pedido.estado === 'PAGADO' ? 'pagado' : ''}`}
            >
              <h1>{pedido.codigo}</h1>
              {/* Mostramos el total para que el camarero sepa cuánto cobrar */}
              <p style={{ fontSize: '1.5rem', marginBottom: '1rem', color: 'white' }}>
                Total: <strong>{pedido.total.toFixed(2)} €</strong>
              </p>
              
              <button 
                className="btn-entregar" 
                onClick={() => handleAccion(pedido.id, pedido.estado)}
              >
                {/* El texto del botón cambia según la fase */}
                {pedido.estado === 'LISTO' ? '💶 Cobrar Pedido' : '✓ Entregar al Cliente'}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default RecogidaPage;