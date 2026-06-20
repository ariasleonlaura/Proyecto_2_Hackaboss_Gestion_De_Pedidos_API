import { useState, useMemo } from 'react';
import { useProductos } from '../hooks/useProductos';
import { useTerminales } from '../hooks/useTerminales';
import { registrarPedido } from '../api/pedidosApi'; // <-- TAREA 1: Importamos la llamada al backend
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';

const TerminalPage = () => {
  const [terminalActiva, setTerminalActiva] = useState(null); 
  const [carrito, setCarrito] = useState([]); 
  const [categoriasAbiertas, setCategoriasAbiertas] = useState({});

  const { productos, loading: loadingProd, error: errorProd } = useProductos(true);
  const { terminales, loading: loadingTerm, error: errorTerm } = useTerminales();

  const productosAgrupados = useMemo(() => {
    if (!productos) return {};
    return productos.reduce((grupos, producto) => {
      const nombreCat = producto.nombreCategoria || 'Sin Categoría';
      if (!grupos[nombreCat]) {
        grupos[nombreCat] = [];
      }
      grupos[nombreCat].push(producto);
      return grupos;
    }, {});
  }, [productos]);

  const toggleCategoria = (nombreCategoria) => {
    setCategoriasAbiertas((estadoAnterior) => ({
      ...estadoAnterior,
      [nombreCategoria]: !estadoAnterior[nombreCategoria]
    }));
  };

  const agregarAlCarrito = (producto) => {
    setCarrito((carritoActual) => {
      const existe = carritoActual.find(p => p.productoId === producto.id);
      if (existe) {
        return carritoActual.map(p => 
          p.productoId === producto.id 
            ? { ...p, cantidad: p.cantidad + 1, subtotal: (p.cantidad + 1) * p.precioUnitario }
            : p
        );
      } else {
        return [...carritoActual, { 
          productoId: producto.id, 
          nombreProducto: producto.nombre, 
          cantidad: 1, 
          precioUnitario: producto.precio,
          subtotal: producto.precio
        }];
      }
    });
  };

  // --- TAREA 2: LÓGICA PARA ELIMINAR DEL CARRITO ---
  const eliminarDelCarrito = (productoId) => {
    setCarrito((carritoActual) => carritoActual.filter(item => item.productoId !== productoId));
  };

  const totalPedido = carrito.reduce((total, item) => total + item.subtotal, 0);

  // --- TAREA 1: LÓGICA PARA ENVIAR EL PEDIDO AL BACKEND ---
  const handleEnviarPedido = async () => {
    try {
      // Preparamos el Map<Long, Integer> que exige el backend
      const productosParaBackend = {};
      carrito.forEach(item => {
        productosParaBackend[item.productoId] = item.cantidad;
      });

      const nuevoPedido = {
        terminalId: terminalActiva.id,
        productosComprados: productosParaBackend
      };

      // Llamamos a la API
      await registrarPedido(nuevoPedido);
      
      alert("¡Pedido enviado a cocina con éxito! 🍔");
      setCarrito([]); // Vaciamos la libreta
    } catch (err) {
      alert("Error al enviar el pedido: " + err.message);
    }
  };

  if (loadingTerm || loadingProd) return <Loading />;
  if (errorTerm) return <ErrorMessage message={errorTerm} />;
  if (errorProd) return <ErrorMessage message={errorProd} />;

  if (!terminalActiva) {
    return (
      <div>
        <h2>Selecciona tu puesto de trabajo</h2>
        <div>
          {terminales.map(terminal => (
            <button key={terminal.id} onClick={() => setTerminalActiva(terminal)}>
              {terminal.nombre}
            </button>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div>
        <h2>Caja Activa: {terminalActiva.nombre}</h2>
        <button onClick={() => { setTerminalActiva(null); setCarrito([]); }}>
          Cambiar Terminal (Vaciará el carrito)
        </button>
      </div>

      <hr />

      <div style={{ display: 'flex', gap: '2rem' }}>
        
        {/* LA CARTA */}
        <div style={{ flex: '2' }}>
          <h2>Categorías</h2>
          {Object.entries(productosAgrupados).map(([nombreCat, listaProd]) => (
            <div key={nombreCat} style={{ marginBottom: '1rem', border: '1px solid black', padding: '1rem' }}>
              <button onClick={() => toggleCategoria(nombreCat)} style={{ width: '100%', textAlign: 'left', fontWeight: 'bold' }}>
                {nombreCat} ({listaProd.length} productos) {categoriasAbiertas[nombreCat] ? '[-]' : '[+]'}
              </button>

              {categoriasAbiertas[nombreCat] && (
                <div style={{ marginTop: '1rem', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  {listaProd.map((producto) => (
                    <div key={producto.id} style={{ border: '1px dotted gray', padding: '0.5rem' }}>
                      <p>{producto.nombre} - <strong>{producto.precio.toFixed(2)} €</strong></p>
                      <button onClick={() => agregarAlCarrito(producto)}>Añadir</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* EL TICKET */}
        <div style={{ flex: '1', borderLeft: '2px solid black', paddingLeft: '1rem' }}>
          <h2>Ticket de Pedido</h2>
          
          {carrito.length === 0 ? (
            <p>El ticket está vacío.</p>
          ) : (
            <>
              <ul>
                {carrito.map((item) => (
                  <li key={item.productoId} style={{ marginBottom: '0.5rem' }}>
                    {item.cantidad}x {item.nombreProducto} - {item.subtotal.toFixed(2)} €
                    <button 
                      onClick={() => eliminarDelCarrito(item.productoId)} 
                      style={{ marginLeft: '10px', color: 'red', cursor: 'pointer' }}
                    >
                      X
                    </button>
                  </li>
                ))}
              </ul>
              
              <h3>Total: {totalPedido.toFixed(2)} €</h3>
              
              <button 
                onClick={handleEnviarPedido} 
                style={{ padding: '1rem', backgroundColor: '#aa3bff', color: 'white', fontWeight: 'bold', cursor: 'pointer' }}
              >
                FINALIZAR Y ENVIAR A COCINA
              </button>
            </>
          )}
        </div>

      </div>
    </div>
  );
};

export default TerminalPage;