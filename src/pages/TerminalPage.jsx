
import { useProductos } from '../hooks/useProductos';
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';

const TerminalPage = () => {
  // Llamamos al hook para traernos los productos del backend
  // Pasamos 'true' como primer parámetro si solo queremos los productos activos
  const { productos, loading, error } = useProductos(true);

  // Si está cargando, mostramos el componente Loading
  if (loading) return <Loading />;

  // Si falla la petición al backend, mostramos nuestro componente de error
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="terminal-container">
      <h2>Nuestra Carta</h2>
      
      <div className="productos-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
        {/* Recorremos el array de productos y pintamos una "tarjeta" básica por cada uno */}
        {productos.map((producto) => (
          <div key={producto.id} style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
            <h3>{producto.nombre}</h3>
            <p>Precio: {producto.precio} €</p>
            <p><small>{producto.nombreCategoria}</small></p>
            <button>Añadir al pedido</button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TerminalPage;