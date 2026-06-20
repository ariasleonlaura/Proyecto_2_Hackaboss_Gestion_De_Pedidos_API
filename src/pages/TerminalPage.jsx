import { useState, useMemo } from 'react';
import { useProductos } from '../hooks/useProductos';
import { useTerminales } from '../hooks/useTerminales';
import { registrarPedido, agregarProductoAPedido } from '../api/pedidosApi';
import ErrorMessage from '../components/common/ErrorMessage';
import { Loading } from '../components/common/Loading';
import '../styles/TerminalPage.css';

const TerminalPage = () => {
  // Terminal que el cliente ha seleccionado al entrar (null = aún no ha elegido)
  const [terminalActiva, setTerminalActiva] = useState(null);

  // Productos que el cliente ha añadido al pedido, con cantidad y subtotal calculado
  const [carrito, setCarrito] = useState([]);

  // Controla qué categorías están desplegadas en la carta
  const [categoriasAbiertas, setCategoriasAbiertas] = useState({});

  // Traemos los productos activos (true) y las terminales disponibles del backend
  const { productos, loading: loadingProd, error: errorProd } = useProductos(true);
  const { terminales, loading: loadingTerm, error: errorTerm } = useTerminales();

  // Agrupamos la lista plana de productos en un objeto por categoría:
  // useMemo evita recalcular esto en cada render si "productos" no ha cambiado
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

  // Abre o cierra el acordeón de una categoría concreta sin afectar a las demás
  const toggleCategoria = (nombreCategoria) => {
    setCategoriasAbiertas((estadoAnterior) => ({
      ...estadoAnterior,
      [nombreCategoria]: !estadoAnterior[nombreCategoria]
    }));
  };

  // Añade un producto al carrito
  // Si ya existía, suma 1 a la cantidad y recalcula el subtotal
  // Si es nuevo, lo añade con cantidad 1
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

  // Quita un producto del carrito por completo (no resta cantidad, lo elimina entero)
  const eliminarDelCarrito = (productoId) => {
    setCarrito((carritoActual) => carritoActual.filter(item => item.productoId !== productoId));
  };

  // Suma los subtotales de todos los productos del carrito para mostrar el total
  const totalPedido = carrito.reduce((total, item) => total + item.subtotal, 0);

  // Envía el pedido al backend en dos pasos:
  // 1) crea el pedido vacío asociado a la terminal
  // 2) añade cada producto del carrito uno a uno al pedido ya creado
  const handleEnviarPedido = async () => {
    try {
      const pedidoCreado = await registrarPedido({ terminalId: terminalActiva.id });

      // for...of en vez de forEach porque necesitamos esperar (await) cada llamada
      // antes de lanzar la siguiente, ya que todas dependen del mismo pedidoId
      for (const item of carrito) {
        await agregarProductoAPedido(pedidoCreado.id, {
          productoId: item.productoId,
          cantidad: item.cantidad,
        });
      }

      // Mostramos el código generado, que el cliente usará para recoger su pedido
      alert(`¡Pedido enviado! Tu código es: ${pedidoCreado.codigo}`);
      setCarrito([]); // vaciamos el carrito para el siguiente pedido
    } catch (e) {
      alert("Error al enviar el pedido: " + e.message);
    }
  };

  // Mientras cargan terminales o productos, mostramos el spinner
  if (loadingTerm || loadingProd) return <Loading />;
  // Si alguna de las dos llamadas falla, mostramos el mensaje de error
  if (errorTerm) return <ErrorMessage message={errorTerm} />;
  if (errorProd) return <ErrorMessage message={errorProd} />;

  // PANTALLA 1: si todavía no se ha elegido terminal, solo mostramos los botones para seleccionarla
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

  // PANTALLA 2: terminal ya seleccionada, mostramos la carta y el ticket
  return (
    <div>
      <div>
        <h2>Caja Activa: {terminalActiva.nombre}</h2>
        {/* Permite volver a elegir terminal. Vacía el carrito para no arrastrar pedidos a medias */}
        <button onClick={() => { setTerminalActiva(null); setCarrito([]); }}>
          Cambiar Terminal (Vaciará el carrito)
        </button>
      </div>

      <hr />

      <div className="terminal-layout">

        {/* LA CARTA: productos agrupados por categoría en acordeones plegables */}
        <div className="terminal-layout__carta">
          <h2>Categorías</h2>
          {Object.entries(productosAgrupados).map(([nombreCat, listaProd]) => (
            <div key={nombreCat} className="categoria-card">
              <button onClick={() => toggleCategoria(nombreCat)} className="categoria-card__toggle">
                {nombreCat} ({listaProd.length} productos) {categoriasAbiertas[nombreCat] ? '[-]' : '[+]'}
              </button>

              {/* Solo se pintan los productos si la categoría está abierta */}
              {categoriasAbiertas[nombreCat] && (
                <div className="categoria-card__productos">
                  {listaProd.map((producto) => (
                    <div key={producto.id} className="producto-card">
                      <p>{producto.nombre} - <strong>{producto.precio.toFixed(2)} €</strong></p>
                      <button onClick={() => agregarAlCarrito(producto)}>Añadir</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* EL TICKET: resumen del carrito con opción de quitar items y finalizar */}
        <div className="terminal-layout__ticket">
          <h2>Ticket de Pedido</h2>

          {carrito.length === 0 ? (
            <p>El ticket está vacío.</p>
          ) : (
            <>
              <ul>
                {carrito.map((item) => (
                  <li key={item.productoId} className="ticket__item">
                    {item.cantidad}x {item.nombreProducto} - {item.subtotal.toFixed(2)} €
                    <button
                      onClick={() => eliminarDelCarrito(item.productoId)}
                      className="ticket__btn-eliminar"
                    >
                      X
                    </button>
                  </li>
                ))}
              </ul>

              <h3>Total: {totalPedido.toFixed(2)} €</h3>

              <button
                onClick={handleEnviarPedido}
                className="ticket__btn-finalizar"
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