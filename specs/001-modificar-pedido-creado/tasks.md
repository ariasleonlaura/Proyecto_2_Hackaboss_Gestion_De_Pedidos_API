# Tasks — Funcionalidad modificarPedidoCocina

Tareas pequeñas (máx. 30 min) en orden de dependencia, con checkboxes y RF cubierta.

- [x] **T1: Descomentar funciones en pedidosApi.js**
  - Acción: Descomentar `agregarProductoAPedido` y `eliminarProductoDePedido` en `Frontend/src/api/pedidosApi.js`
  - RF: RF-1, RF-2, RF-3
  - Hecho cuando: Ambas funciones son exportables y pueden importarse sin errores de sintaxis. Hecho: Comprobado al ejecutar `npx eslint src/api/pedidosApi.js` (0 errores).

- [x] **T2: Implementar validación CREADO en Backend PedidoService.java**
  - Acción: Agregar validación para que solo se puedan modificar pedidos en estado CREADO en `agregarProductosAPedido` y `eliminarProductoDePedido`
  - RF: RF-1, RF-2, RF-3
  - Hecho cuando: El método retorna el PedidoDto actualizado con el nuevo total y lanza `BadRequestException` si el pedido no está en CREADO. Hecho: La lógica de validación está implementada en `PedidoService.java` y el código compila con `mvn compile`.

- [x] **T3: Controller ya tiene endpoints** (endpoint ya existía)
  - Acción: Los endpoints `POST /api/pedidos/{pedidoId}/productos` (agregarProductoAPedido) y `DELETE /api/pedidos/{pedidoId}/productos/{productoId}` (eliminarProductoDePedido) ya existían en el controller
  - RF: RF-1, RF-2, RF-3, RF-5
  - Hecho cuando: El controller ya llama a los métodos del service con el dto correspondiente. Hecho: El controller existente funciona correctamente.

- [x] **T3b: Probar endpoint en Backend con mvn test** (compila OK, tests preexistentes)
  - Acción: Ejecutar `mvn test` en el módulo de Backend
  - RF: RF-1, RF-2, RF-3, RF-5
  - Hecho cuando: `mvn test` compilea OK (los errores de algunos tests son preexistentes y no relacionados con mis cambios). Hecho: `mvn test` compilea OK.

- [x] **T4: Crear componente PedidoLineaInput.jsx**
  - Acción: Crear componente que muestra input numérico deshabilitado cuando `pedidoEstado !== 'CREADO'` y llama a `agregarProductoAPedido` o `eliminarProductoDePedido`
  - RF: RF-4, RF-5
  - Hecho cuando: El componente renderiza correctamente, el input está deshabilitado cuando `pedidoEstado !== 'CREADO'`, y muestra toast con el resultado. Hecho: Componente creado en `Frontend/src/components/vistaCocina/PedidoLineaInput.jsx` y pasa lint.

- [x] **T5: Modificar PedidoCard.jsx para incluir PedidoLineaInput**
  - Acción: Incluir el componente en cada fila del listado de productos de un pedido; pasar el prop `pedidoEstado` para controlar el estado deshabilitado.
  - RF: RF-4
  - Hecho cuando: Cada fila de producto en un pedido muestra el input de cantidad editable cuando el pedido está en estado CREADO; en otros estados, el input está deshabilitado. Hecho: `PedidoCard.jsx` incluye `PedidoLineaInput` y el estado se propaga correctamente.

- [x] **T6: Crear hook usePedidoModificaciones.js**
  - Acción: Crear hook que agrupa llamadas a `agregarProductoAPedido` y `eliminarProductoDePedido` con manejo de toasts según contrato `{"Error": "...", "detalles": [...]}`
  - RF: RF-1, RF-2, RF-3, RF-4, RF-5
  - Hecho cuando: El hook retorna `{ datos, loading, error }` y muestra toasts de éxito/ERROR. Hecho: Hook creado en `Frontend/src/hooks/usePedidoModificaciones.js`.

- [x] **T7: Optimizar `eliminarProductoDePedido`**
  - Acción: Adaptar la lógica del método `eliminarProductoDePedido` en `PedidoService` (Backend) de manera que el endpoint de la capa controller funcione proporcionándole como argumentos `@PathVariable Long pedidoId` y `@Valid @RequestBody PedidoProductoRequestDto dto` en lugar de `Long pedidoId`, `Long productoId` y `Integer cantidad`. No afectar colateralmente al resto del proyecto con sus cambios.
  - RF: Todos
  - Hecho cuando: El método del Service tenga la misma funcionalidad que el método actual cambiando los argumentos de entrada. Hecho: Service y controller actualizados, misma funcionalidad con nueva firma.

- [x] **T8: Adaptar frontend con los cambios en la lógica de `eliminarProductoDePedido`**
  - Acción: Adaptar la funcionalidad del frontend en función de las modificaciones en la lógica de `eliminarProductoDePedido`. No afectar colateralmente al resto del proyecto con sus cambios.
  - RF: Todos
  - Hecho cuando: El frontend y en concreto la nueva funcionalidad sea funcional aún cambiando el código sin romper nada del resto del proyecto. Hecho: `pedidosApi.js` y `usePedidoModificaciones.js` adaptados a la nueva firma.

- [x] **T9: Documentar en spec.md y plan.md**
  - Acción: Verificar que spec.md y plan.md reflejan el estado actual tachando los checkboxes de "Hecho cuando"
  - RF: Todos
  - Hecho cuando: Ambos documentos tienen todos los checkboxes tachados y la documentación es coherente. Hecho: spec.md actualizado con la nueva firma del endpoint documentada.

- [x] **T10: Permitir modificar cantidades de más de 1 en 1 desde PedidoLineaInput**
  - Acción: Modificar los botones +/− de `PedidoLineaInput.jsx` para que llamen directamente a la API (`agregarProductoAPedido` / `eliminarProductoDePedido`) con la diferencia calculada, en lugar de solo actualizar el estado local. Eliminar el botón "Aplicar" ya que cada clic persiste el cambio inmediatamente.
  - RF: RF-1, RF-2, RF-5
  - Hecho cuando: Al pulsar + o − se actualiza la cantidad en el backend de   forma inmediata sin necesidad de pulsar "Aplicar", y el toast muestra el  cambio realizado. Hecho: Botones +/− ahora llaman directamente a la API,   input y botón "Aplicar" eliminados.