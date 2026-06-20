import { useState, useEffect } from 'react';
import { listarPedidosYPorEstado, cambiarEstadoDelPedido } from '../api/pedidosApi';
import { Loading } from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import '../styles/RecogidaPage.css';

const RecogidaPage = () => {
  const [pedidosListos, setPedidosListos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Función para traer los pedidos "LISTOS"
  const fetchPedidosListos = async () => {
    try {
      const data = await listarPedidosYPorEstado('LISTO');
      setPedidosListos(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Se ejecuta al cargar la página y luego cada 15 segundos (polling)
  useEffect(() => {
    fetchPedidosListos();
    const interval = setInterval(fetchPedidosListos, 15000);
    return () => clearInterval(interval);
  }, []);

  // Al pulsar entregar, cambiamos el estado a "ENTREGADO" y recargamos
  const handleEntregar = async (id) => {
    try {
      await cambiarEstadoDelPedido(id, { estado: 'ENTREGADO' });
      fetchPedidosListos(); // Recargamos para que desaparezca de la pantalla
    } catch (err) {
      alert("Error al entregar: " + err.message);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="recogida-container">
      <h2>Pantalla de Recogida</h2>
      <p>Los siguientes pedidos están listos para ser recogidos:</p>
      
      <div className="pedidos-listos-grid">
        {pedidosListos.length === 0 ? (
          <p style={{ gridColumn: '1 / -1', fontSize: '1.5rem', color: '#666' }}>
            No hay pedidos listos en este momento.
          </p>
        ) : (
          pedidosListos.map(pedido => (
            <div key={pedido.id} className="pedido-listo-card">
              <h1>{pedido.codigo}</h1>
              <button className="btn-entregar" onClick={() => handleEntregar(pedido.id)}>
                Entregado al Cliente ✓
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default RecogidaPage;