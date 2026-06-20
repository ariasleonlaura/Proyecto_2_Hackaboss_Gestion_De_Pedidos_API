import { useState, useMemo } from 'react';
import { useProductos } from '../hooks/useProductos';
import { useTerminales } from '../hooks/useTerminales';
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';

const TerminalPage = () => {
  // --- 1. ESTADOS DE LA APLICACIÓN ---
  const [terminalActiva, setTerminalActiva] = useState(null); 
  const [carrito, setCarrito] = useState([]); 
  const [categoriasAbiertas, setCategoriasAbiertas] = useState({}); // Controla qué acordeón está abierto

  // --- 2. LLAMADAS AL BACKEND ---
  const { productos, loading: loadingProd, error: errorProd } = useProductos(true);
  const { terminales, loading: loadingTerm, error: errorTerm } = useTerminales();

  // --- 3. LÓGICA: AGRUPAR PRODUCTOS POR CATEGORÍA ---
  // Convierte la lista plana de productos en un objeto: { "Bebida": [...], "Comida": [...] }
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

  // --- 4. LÓGICA: ABRIR/CERRAR CATEGORÍAS ---
  const toggleCategoria = (nombreCategoria) => {
    setCategoriasAbiertas((estadoAnterior) => ({
      ...estadoAnterior,
      [nombreCategoria]: !estadoAnterior[nombreCategoria]
    }));
  };

  // --- 5. LÓGICA: GESTIÓN DEL CARRITO ---
  const agregarAlCarrito = (producto) => {
    setCarrito((carritoActual) => {
      const existe = carritoActual.find(p => p.productoId === producto.id);
      if (existe) {
        // Si existe, sumamos 1 a la cantidad y recalculamos subtotal
        return carritoActual.map(p => 
          p.productoId === producto.id 
            ? { ...p, cantidad: p.cantidad + 1, subtotal: (p.cantidad + 1) * p.precioUnitario }
            : p
        );
      } else {
        // Si es nuevo, lo metemos con cantidad 1
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

  const totalPedido = carrito.reduce((total, item) => total + item.subtotal, 0);

  // --- CONTROL DE CARGA Y ERRORES ---
  if (loadingTerm || loadingProd) return <Loading />;
  if (errorTerm) return <ErrorMessage message={errorTerm} />;
  if (errorProd) return <ErrorMessage message={errorProd} />;

  // =========================================================================
  // RENDER PANTALLA 1: ELEGIR TERMINAL
  // =========================================================================
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

  // =========================================================================
  // RENDER PANTALLA 2: CARTA Y CARRITO
  // =========================================================================
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
        
        {/* LADO IZQUIERDO: CARTA CON ACORDEONES */}
        <div style={{ flex: '2' }}>
          <h2>Categorías</h2>
          
          {/* Recorremos el objeto de productos agrupados */}
          {Object.entries(productosAgrupados).map(([nombreCat, listaProd]) => (
            <div key={nombreCat} style={{ marginBottom: '1rem', border: '1px solid black', padding: '1rem' }}>
              
              {/* Botón para desplegar/ocultar */}
              <button onClick={() => toggleCategoria(nombreCat)} style={{ width: '100%', textAlign: 'left', fontWeight: 'bold' }}>
                {nombreCat} ({listaProd.length} productos) {categoriasAbiertas[nombreCat] ? '[-]' : '[+]'}
              </button>

              {/* Lista de productos (Solo se ve si está abierto) */}
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

        {/* LADO DERECHO: TICKET */}
        <div style={{ flex: '1', borderLeft: '2px solid black', paddingLeft: '1rem' }}>
          <h2>Ticket de Pedido</h2>
          
          {carrito.length === 0 ? (
            <p>El ticket está vacío.</p>
          ) : (
            <>
              <ul>
                {carrito.map((item) => (
                  <li key={item.productoId}>
                    {item.cantidad}x {item.nombreProducto} - {item.subtotal.toFixed(2)} €
                  </li>
                ))}
              </ul>
              
              <h3>Total: {totalPedido.toFixed(2)} €</h3>
              
              <button>
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