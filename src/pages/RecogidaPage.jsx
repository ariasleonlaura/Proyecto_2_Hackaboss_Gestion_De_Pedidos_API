import { useState, useEffect } from 'react';
import { listarPedidosYPorEstado, cambiarEstadoDelPedido } from '../api/pedidosApi';
import { Loading } from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import '../styles/RecogidaPage.css';
import { toast } from 'sonner';

const RecogidaPage = () => {
  const [pedidos, setPedidos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPedidos = async () => {
    try {
      const [listos, pagados] = await Promise.all([
        listarPedidosYPorEstado('LISTO'),
        listarPedidosYPorEstado('PAGADO')
      ]);
      setPedidos([...listos, ...pagados]);
      setError(null);
    } catch (err) {
      setError("Servidor no disponible. Inténtalo de nuevo más tarde.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPedidos();
    const interval = setInterval(fetchPedidos, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleAccion = async (id, estadoActual) => {
    try {
      const nuevoEstado = estadoActual === 'LISTO' ? 'PAGADO' : 'ENTREGADO';
      await cambiarEstadoDelPedido(id, { estado: nuevoEstado });
      
      toast.success(estadoActual === 'LISTO' ? '¡Pedido cobrado!' : '¡Pedido entregado con éxito!');
      
      fetchPedidos(); 
    } catch (err) {
      toast.error("Error al actualizar el pedido");
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="recogida-container">
      <h2>Pantalla de Recogida y Cobro</h2>
      <p>Los siguientes pedidos están pendientes de pago o entrega:</p>
      
      <div className="pedidos-listos-grid">
        {pedidos.length === 0 ? (
          <p style={{ gridColumn: '1 / -1', fontSize: '1.5rem', color: '#666' }}>
            No hay pedidos en esta zona en este momento.
          </p>
        ) : (
          pedidos.map(pedido => (
            <div 
              key={pedido.id} 
              className="pedido-listo-card"
              style={pedido.estado === 'PAGADO' ? { backgroundColor: '#aa3bff' } : {}}
            >
              <h1>{pedido.codigo}</h1>
              
              <p style={{ fontSize: '1.5rem', marginBottom: '1.5rem', color: 'white' }}>
                Total: <strong>{pedido.total.toFixed(2)} €</strong>
              </p>

              <button 
                className="btn-entregar" 
                onClick={() => handleAccion(pedido.id, pedido.estado)}
              >
                {pedido.estado === 'LISTO' ? '💶 Cobrar Pedido' : 'Entregado al Cliente ✓'}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default RecogidaPage;