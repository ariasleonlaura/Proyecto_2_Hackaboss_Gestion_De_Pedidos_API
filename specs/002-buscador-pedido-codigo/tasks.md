# Tasks — Buscador de pedido por código en Recogida

> Documento retrospectivo: T1-T9 en el commit `20d4914`; T10-T12 son corrección y limpieza posteriores a la revisión (sin cambio funcional).

- [x] **T1: Backend — endpoint `GET /api/pedidos/codigo/{codigo}`**
  - Acción: Añadir en `PedidoController` el endpoint que recibe el código como `@PathVariable` y devuelve el `PedidoDto` directo. En `PedidoService`, añadir `obtenerPedidoPorCodigo(codigo)` que busque por código y lance `ResourceNotFoundException` si no existe. En `PedidoRepository`, añadir `findByCodigo(String codigo)`.
  - RF: RF-3, RF-5
  - Hecho cuando: Un código existente devuelve 200 con el `PedidoDto`; un código inexistente devuelve 404 con `{"Error: ": "..."}`. Hecho: implementado y cubierto por `PedidoControllerTest`.

- [x] **T2: Frontend — `buscarPedidoPorCodigo` en `pedidosApi.js`**
  - Acción: Añadir la función que llama a `GET /api/pedidos/codigo/{codigo}` vía `fetchJSON`.
  - RF: RF-3
  - Hecho cuando: La función es exportable y devuelve el `PedidoDto`. Hecho: implementado y sin errores de lint.

- [x] **T3: Frontend — hook `usePedidoPorCodigo`**
  - Acción: Crear el hook que, dado un código, dispara la búsqueda y expone `{ pedido, loading, error }`. Si el código está vacío, no busca y limpia el estado.
  - RF: RF-3, RF-5
  - Hecho cuando: El hook gestiona carga, éxito y error. Hecho: creado en `Frontend/src/hooks/usePedidoPorCodigo.js`.

- [x] **T4: Frontend — campo de texto + botón "Buscar"**
  - Acción: Añadir en `RecogidaPage` la barra de búsqueda (`.busqueda-bar`) con un `input` y un botón "Buscar", sin retirar la lista de pendientes.
  - RF: RF-1
  - Hecho cuando: El campo y el botón se muestran siempre, con o sin lista visible. Hecho: implementado.

- [x] **T5: Frontend — recortar espacios y avisar si está vacío**
  - Acción: En `handleBuscar`, hacer `codigoBusqueda.trim()`; si el resultado es vacío, mostrar `toast.warning("Introduce un código de pedido")` y no mostrar tarjeta.
  - RF: RF-2, RF-4
  - Hecho cuando: Un código con espacios alrededor se busca igual; un campo vacío o solo espacios no lanza la búsqueda. Hecho: implementado.

- [x] **T6: Frontend — tarjeta de resultado**
  - Acción: Mostrar una tarjeta con el código, el total (2 decimales y €) y el estado, y `toast.success("Pedido encontrado: <código>")`.
  - RF: RF-3
  - Hecho cuando: La tarjeta muestra los datos del pedido encontrado. Hecho: implementado.

- [x] **T7: Frontend — acciones según el estado**
  - Acción: En la tarjeta, ofrecer "Cobrar Pedido" si el estado es LISTO y "Entregado al Cliente" si es PAGADO; para cualquier otro estado, mostrar un texto neutro sin acciones.
  - RF: RF-6, RF-7, RF-8
  - Hecho cuando: Solo LISTO y PAGADO ofrecen acción; CREADO/PREPARACION/ENTREGADO se muestran sin acciones. Hecho: implementado.

- [x] **T8: Frontend — cierre tras éxito, fallo y "Cancelar"**
  - Acción: Tras una acción con éxito, cerrar la tarjeta (la lista se actualiza con el siguiente refresco); si la acción falla, mostrar error y mantener la tarjeta; el botón "Cancelar" cierra la tarjeta sin modificar nada.
  - RF: RF-9, RF-10, RF-11
  - Hecho cuando: Los tres comportamientos se cumplen. Hecho: implementado.

- [x] **T9: Frontend — estilos de la barra y de la tarjeta**
  - Acción: Añadir en `RecogidaPage.css` los estilos de `.busqueda-bar`, `.busqueda-input`, `.btn-buscar` y de la tarjeta de resultado, reutilizando `pedido-listo-card` para que tenga el mismo aspecto que la lista.
  - RF: RF-12
  - Hecho cuando: La tarjeta tiene el mismo fondo según el estado que las tarjetas de la lista. Hecho: implementado.

- [x] **T10: Frontend — corrección de desestructuración en `TerminalPage.jsx`**
  - Acción: Corregir la desestructuración de los hooks para usar alias correctos (`error: errorProd`, `error: errorTerm`) en lugar de nombres duplicados/incorrectos, evitando colisión de variables `error`.
  - RF: sin RF directa (corrección post-revisión; soporta la integridad del flujo de Terminal).
  - Hecho cuando: No hay conflicto de nombres de `error` en el componente y los errores de cada hook se renderizan por separado (`errorTerm`, `errorProd`, `errorMasVendidos`). Hecho: implementado.

- [x] **T11: Frontend — limpieza de estilos muertos en `TerminalPage.css`**
  - Acción: Eliminar reglas CSS no utilizadas por `TerminalPage.jsx` (estilos huérfanos residuales).
  - RF: sin RF directa (limpieza).
  - Hecho cuando: `TerminalPage.css` solo contiene reglas usadas por la página. Hecho: implementado.

- [x] **T12: Frontend — tokens unificados en `TerminalPage.css`**
  - Acción: Sustituir valores hardcodeados por los tokens de `styles/tokens.css` (espaciados, colores, radios, tipografía), manteniendo el soporte de dark mode vía `prefers-color-scheme`.
  - RF: sin RF directa (consistencia visual / dark mode).
  - Hecho cuando: El archivo usa `var(--...)` para espaciado, superficie, tinta, radio y tipografía. Hecho: implementado.
