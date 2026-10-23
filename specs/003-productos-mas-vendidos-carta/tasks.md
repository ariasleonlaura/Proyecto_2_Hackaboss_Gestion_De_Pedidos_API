# Tasks — Productos más vendidos en la carta

> Documento retrospectivo: todas las tareas están completadas (commits `64b5630` y `d00cdba`).
> Tareas pequeñas en orden de dependencia, con RF cubierta.

- [x] **T1: Backend — endpoint `GET /api/estadisticas/productos-mas-vendidos`**
  - Acción: Añadir en `EstadisticasController` el endpoint; en `EstadisticasService`, el método `obtenerProductosMasVendidos()`; en `ProductoRepository`, la `@Query` con `SUM`/`GROUP BY`/`ORDER BY DESC`; y el DTO `ProductoMasVendidoDto` (`totalVendido` como `Long`).
  - RF: RF-2
  - Hecho cuando: El endpoint devuelve el ranking de productos por unidades vendidas, de mayor a menor. Hecho: implementado en `64b5630` y cubierto por `ProductoRepositoryTest` y `EstadisticasServiceTest`.

- [x] **T2: Frontend — `obtenerProductosMasVendidos` en `estadisticasApi.js`**
  - Acción: Añadir la función que llama a `GET /api/estadisticas/productos-mas-vendidos` vía `fetchJSON`.
  - RF: RF-2
  - Hecho cuando: La función es exportable y devuelve el listado del ranking. Hecho: implementado en `d00cdba`.

- [x] **T3: Frontend — hook `useProductosMasVendidos`**
  - Acción: Crear el hook que carga el ranking al entrar en la pantalla y expone `{ datos, loading, error }`. Si falla, limpia los datos y guarda el error.
  - RF: RF-8, RF-9
  - Hecho cuando: El hook gestiona carga, éxito y error. Hecho: creado en `d00cdba`.

- [x] **T4: Frontend — sección "Los más vendidos" encima de las categorías**
  - Acción: Añadir en `TerminalPage` la sección `.mas-vendidos` al principio de `terminal-layout__carta`, con el mismo aspecto que `.categoria-card`.
  - RF: RF-1
  - Hecho cuando: La sección aparece encima de las categorías. Hecho: implementado.

- [x] **T5: Frontend — top 5 y filtrado de desactivados**
  - Acción: Calcular `masVendidosVisibles` cruzando el ranking con la lista de productos (`.filter((mv) => mv.producto)`) y cortando a 5 (`.slice(0, 5)`), para que un desactivado no ocupe hueco.
  - RF: RF-2, RF-5
  - Hecho cuando: Se muestran como máximo 5 productos activos; un desactivado se sustituye por el siguiente. Hecho: implementado.

- [x] **T6: Frontend — tarjeta de producto**
  - Acción: Mostrar nombre, precio con 2 decimales y €, "Total vendido: N" y el botón "Añadir".
  - RF: RF-3, RF-10
  - Hecho cuando: Cada tarjeta muestra los cuatro elementos. Hecho: implementado.

- [x] **T7: Frontend — "Añadir" añade al carrito**
  - Acción: El botón "Añadir" llama a `agregarAlCarrito(mv.producto)`, la misma función que las categorías.
  - RF: RF-4
  - Hecho cuando: Añadir desde la sección tiene el mismo efecto que desde las categorías. Hecho: implementado.

- [x] **T8: Frontend — clave por id y estados vacío/error/carga**
  - Acción: Usar `mv.productoId` como `key` de React; mostrar "No hay productos en la lista" si no hay ventas; ocultar la sección si falla el ranking; y no mostrar la sección mientras carga.
  - RF: RF-6, RF-7, RF-8, RF-9
  - Hecho cuando: Los cuatro comportamientos se cumplen. Hecho: implementado.

- [x] **T9: Frontend — estilos de la sección**
  - Acción: Añadir en `TerminalPage.css` los estilos de `.mas-vendidos` para que tenga el mismo aspecto que las categorías.
  - RF: RF-1
  - Hecho cuando: El recuadro de la sección se ve igual que el de una categoría. Hecho: implementado.
