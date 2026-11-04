import { useState, useMemo } from "react";
import { useProductos } from "../hooks/useProductos";
import { useTerminales } from "../hooks/useTerminales";
import { useProductosMasVendidos } from "../hooks/useProductosMasVendidos";
import { registrarPedido } from "../api/pedidosApi";
import ErrorMessage from "../components/common/ErrorMessage";
import { Loading } from "../components/common/Loading";
import "../styles/TerminalPage.css";
import { toast } from "sonner";

const TerminalPage = () => {
  // Terminal que el cliente ha seleccionado al entrar (null = aún no ha elegido)
  const [terminalActiva, setTerminalActiva] = useState(null);

  // Productos que el cliente ha añadido al pedido, con cantidad y subtotal calculado
  const [carrito, setCarrito] = useState([]);

  // Controla qué categorías están desplegadas en la carta
  const [categoriasAbiertas, setCategoriasAbiertas] = useState({});

  // Traemos los productos activos (true) y las terminales disponibles del backend
  const {
    productos,
    loading: loadingProd,
    error: errorProd,
  } = useProductos(true);
  const {
    terminales,
    loading: loadingTerm,
    error: errorTerm,
  } = useTerminales();
  // Los más vendidos cargan en paralelo y no bloquean la página (loading y
  // error propios): mientras llegan, su sección simplemente no se pinta
  const {
    datos: masVendidos,
    loading: loadingMasVendidos,
    error: errorMasVendidos,
  } = useProductosMasVendidos();

  /* Agrupamos la lista plana de productos en un objeto por categoría:
  - { "Bebidas": [...], "Comida": [...] }
  - useMemo evita recalcular esto en cada render si "productos" no ha cambiado */
  const productosAgrupados = useMemo(() => {
    if (!productos) return {};
    return productos.reduce((grupos, producto) => {
      const nombreCat = producto.nombreCategoria || "Sin Categoría";
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
      [nombreCategoria]: !estadoAnterior[nombreCategoria],
    }));
  };

  /* Añade un producto al carrito:
  - Si ya existía, suma 1 a la cantidad y recalcula el subtotal
  - Si es nuevo, lo añade con cantidad 1 */
  const agregarAlCarrito = (producto) => {
    setCarrito((carritoActual) => {
      const existe = carritoActual.find((p) => p.productoId === producto.id);
      if (existe) {
        return carritoActual.map((p) =>
          p.productoId === producto.id
            ? {
                ...p,
                cantidad: p.cantidad + 1,
                subtotal: (p.cantidad + 1) * p.precioUnitario,
              }
            : p,
        );
      } else {
        return [
          ...carritoActual,
          {
            productoId: producto.id,
            nombreProducto: producto.nombre,
            cantidad: 1,
            precioUnitario: producto.precio,
            subtotal: producto.precio,
          },
        ];
      }
    });
  };

  // Sube o baja la cantidad de un producto ya en el carrito
  const cambiarCantidad = (productoId, delta) => {
    setCarrito((carritoActual) => {
      return carritoActual
        .map((p) =>
          p.productoId === productoId
            ? // delta será +1 o -1 según el botón que pulse
              {
                ...p,
                cantidad: p.cantidad + delta,
                subtotal: (p.cantidad + delta) * p.precioUnitario,
              }
            : p,
        )
        .filter((p) => p.cantidad > 0); // si llega a 0, se elimina
    });
  };

  // Quita un producto del carrito por completo (no resta cantidad, lo elimina entero)
  const eliminarDelCarrito = (productoId) => {
    setCarrito((carritoActual) =>
      carritoActual.filter((item) => item.productoId !== productoId),
    );
  };

  /* Se filtra primero, y se corta después evitando el caso de que no se muestre alguno de los 5 
  más vendidos si algunó se desactivó */
  const masVendidosVisibles = useMemo(() => {
    return masVendidos
      .map((mv) => ({
        ...mv,
        producto: productos.find((p) => p.id === mv.productoId),
      }))
      .filter((mv) => mv.producto)
      .slice(0, 5);
  }, [masVendidos, productos]);

  // Suma los subtotales de todos los productos del carrito para mostrar el total
  const totalPedido = carrito.reduce((total, item) => total + item.subtotal, 0);

  /* Envía el pedido al backend en una sola llamada, tal como exige CrearPedidoDto:
  el backend recibe terminalId y un Map<Long, Integer> con los productos y cantidades */
  const handleEnviarPedido = async () => {
    try {
      // 1. Convertimos el carrito en el formato que pide el backend: { idProducto: cantidad }
      const productosParaBackend = {};
      carrito.forEach((item) => {
        productosParaBackend[item.productoId] = item.cantidad;
      });

      // 2. Armamos el DTO completo con la terminal y los productos
      const dto = {
        terminalId: terminalActiva.id,
        productosComprados: productosParaBackend,
      };

      // 3. Una sola llamada crea el pedido con todos sus productos
      const pedidoCreado = await registrarPedido(dto);

      // 4. Mostramos el código generado, que el cliente usará para recoger su pedido
      toast.success(`¡Pedido enviado! Código: ${pedidoCreado.codigo}`);

      setCarrito([]); // vaciamos el carrito para el siguiente cliente
    } catch (e) {
      toast.error("Error al enviar el pedido: " + e.message);
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
      <div className="terminal-seleccion">
        <h2>Selecciona tu puesto de trabajo</h2>
        <div className="terminal-seleccion__terminales">
          {terminales.map((terminal) => (
            <button
              key={terminal.id}
              onClick={() => setTerminalActiva(terminal)}
              className="terminal-seleccion__btn"
            >
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
        <button
          onClick={() => {
            setTerminalActiva(null);
            setCarrito([]);
          }}
          className="terminal-seleccion__btn--cambio"
        >
          Cambiar Terminal (Vaciará el carrito)
        </button>
      </div>

      <hr />

      <div className="terminal-layout">
        {/* LA CARTA: productos agrupados por categoría en acordeones plegables */}
        <div className="terminal-layout__carta">
          {/* Sin error & sin productos en la lista -> mensaje */}
          {!loadingMasVendidos &&
            !errorMasVendidos &&
            masVendidosVisibles.length === 0 && (
              <div className="mas-vendidos">
                <h2>Los más vendidos</h2>
                <p>No hay productos en la lista</p>
              </div>
            )}

          {/* Sin error & con productos en la lista -> mostrar lista */}
          {!loadingMasVendidos &&
            !errorMasVendidos &&
            masVendidosVisibles.length > 0 && (
              <div className="categoria-card mas-vendidos">
                <h2>Los más vendidos</h2>
                <div className="categoria-card__productos">
                  {masVendidosVisibles.map((mv) => (
                    <div key={mv.productoId} className="producto-card">
                      <p>{mv.nombreProducto}</p>
                      <div className="producto-card__footer">
                        <p>
                          <strong>{mv.producto.precio.toFixed(2)} €</strong>
                        </p>
                        <p>Total vendido: {mv.totalVendido}</p>
                        <button
                          onClick={() => agregarAlCarrito(mv.producto)}
                          className="producto-card__btn"
                        >
                          Añadir
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

          <h2>Categorías</h2>
          {Object.entries(productosAgrupados).map(([nombreCat, listaProd]) => (
            <div key={nombreCat} className="categoria-card">
              <button
                onClick={() => toggleCategoria(nombreCat)}
                className="categoria-card__toggle"
              >
                {nombreCat} ({listaProd.length} productos){" "}
                {categoriasAbiertas[nombreCat] ? "[-]" : "[+]"}
              </button>

              {/* Solo se pintan los productos si la categoría está abierta */}
              {categoriasAbiertas[nombreCat] && (
                <div className="categoria-card__productos">
                  {listaProd.map((producto) => (
                    <div key={producto.id} className="producto-card">
                      <p>{producto.nombre}</p>
                      <div className="producto-card__footer">
                        <p>
                          <strong>{producto.precio.toFixed(2)} €</strong>
                        </p>
                        <button
                          onClick={() => agregarAlCarrito(producto)}
                          className="producto-card__btn"
                        >
                          Añadir
                        </button>
                      </div>
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
                    <div className="ticket__item-info">
                      <p>
                        {item.cantidad}x {item.nombreProducto}
                      </p>
                      <p>
                        <strong>{item.subtotal.toFixed(2)} €</strong>
                      </p>
                    </div>
                    <div className="ticket__item-acciones">
                      <button
                        onClick={() => cambiarCantidad(item.productoId, +1)}
                        className="ticket__btn-cantidad"
                      >
                        +
                      </button>
                      <button
                        onClick={() => cambiarCantidad(item.productoId, -1)}
                        className="ticket__btn-cantidad"
                      >
                        -
                      </button>
                      <button
                        onClick={() => eliminarDelCarrito(item.productoId)}
                        className="ticket__btn-eliminar"
                      >
                        X
                      </button>
                    </div>
                  </li>
                ))}
              </ul>

              <h3>Total: {totalPedido.toFixed(2)} €</h3>

              <button
                onClick={handleEnviarPedido}
                className="ticket__btn-finalizar"
              >
                FINALIZAR PEDIDO
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default TerminalPage;
