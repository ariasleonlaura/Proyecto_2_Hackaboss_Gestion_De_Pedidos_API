# Plan de implementación — Funcionalidad modificarPedidoCocina

## 1. Archivos afectados

| Archivo | Responsabilidad | RF cubierta |
|---|---|---|
| `Backend/src/main/java/gestionpedidos/service/PedidoService.java` | Lógica pura de negocio: valida que el pedido esté en estado CREADO y delega en el backend las operaciones `agregarProductoAPedido` y `eliminarProductoDePedido` (ya implementadas en el backend). Aplica reglas de acumulación y eliminación de líneas. | RF-1, RF-2, RF-3 |
| `Backend/src/main/java/gestionpedidos/controller/PedidoController.java` | Reutiliza el endpoint existente `POST /api/pedidos/{pedidoId}/productos` con el `dto` `agregarProductoAPedido` o `eliminarProductoDePedido`. Recibe el `dto` con la operación y devuelve el `PedidoDto` actualizado con el nuevo total. Valida la respuesta del service y mapea al formato de respuesta del contrato REST (`{"mensaje": "...", "data": {...}}`). | RF-1, RF-2, RF-3, RF-5 |
| `Frontend/src/api/pedidosApi.js` | **Descomentar y utilizar** las 2 funciones ya existentes en el archivo (`agregarProductoAPedido` y `eliminarProductoDePedido`). **No crear funciones nuevas.** Cada una retorna `Promise<PedidoDto>` con el pedido actualizado. Estas funciones son llamadas puras desde los componentes de UI; no contienen `useState` ni `toast` directamente (el componente gestiona ese efecto). | RF-1, RF-2, RF-3 |
| `Frontend/src/components/pedido-linea-input.jsx` | **Crear** Componente de entrada de cantidad por línea de pedido. Recibe `productoId`, `cantidadActual`, `pedidoEstado` y `onCambio`. Muestra un input numérico deshabilitado cuando `pedidoEstado !== 'CREADO'`. Invoca `agregarProductoAPedido` o `eliminarProductoDePedido` por `onChange` o botón +/−. Muestra un toast con el resultado del cambio. | RF-4, RF-5 |
| `Frontend/src/pages/CocinaPage.jsx` | **Modificar** (o crear si no existe) la renderización del listado de pedidos para incluir el componente `PedidoLineaInput` en cada fila. Accede al estado `pedido.estado` desde `ESTADOS_PEDIDO` y pasa el prop `deshabilitado` correspondiente. | RF-4 |
| `Frontend/src/hooks/usePedidoModificaciones.js` | **Nuevo Hook** (o modificar el existente) que agrupa la lógica de llamada a las API: `useAgregarProducto`, `useEliminarProducto`. Devuelve `{ datos, loading, error }` y un `refetch` manual. Centraliza el manejo de toasts de éxito/ERROR según el contrato `{"Error": "...", "detalles": [...]}`. **Cubre RF-1 a RF-5** al centralizar el patrón de llamada y respuesta. | RF-1, RF-2, RF-3, RF-4, RF-5 |

## 2. Funciones puras de lógica (reutilizando las 2 existentes)

Basado en tus indicaciones, el frontend reutiliza estas 2 funciones que ya estaban comentadas en `pedidosApi.js`:

1. `agregarProductoAPedido(pedidoId, dto)`
   - **Pre**: `pedidoActual` debe estar en estado CREADO (validado antes por el caller, RF-4)
   - **Post**: Retorna nuevo objeto `PedidoDto` con la línea de producto actualizada. **Si la cantidad resultante es 0, la línea se elimina automáticamente y el total del pedido se recalcula.** **Si se anulan todas las líneas, el total del pedido queda en 0** (RF-5).
   - **Lógica**: Busca el índice de la línea en `pedidoActual.lines`; si existe, suma la cantidad indicada en el `dto`; si no existe, crea una nueva línea (RF-3). **NOTA:** Si el producto ya estaba en el pedido pero en una línea distinta, el comportamiento por RF-1 es sumar a la línea objetivo (no crear línea duplicada).

2. `eliminarProductoDePedido(pedidoId, productoId, cantidad)`
   - **Pre**: `pedidoActual` debe estar en estado CREADO (validado antes por el caller, RF-4)
   - **Post**: Si `cantidadActual - cantidadRestar <= 0`, retorna nuevo `PedidoDto` sin esa línea (línea eliminada) y `total: 0` si fue la última. Si `> 0`, retorna con `nuevaCantidad = cantidadActual - cantidadRestar`.
   - **Lógica**: Validación interna `if (resultado <= 0) eliminarLinea; else actualizarCantidad(resultado)`. **No hay restricción contra modificaciones repetidas** mientras el pedido esté en CREADO; se pueden modificar tantas veces como se quiera, y cada cambio sobrescribe el anterior.

## 3. Algoritmo del mapa (pseudocódigo)

```
# Mapa de operación: qué hacer según el tipo y estado

función mapaOperacion(operacion, pedido, datos):
    segun operacion:
        caso "agregar":
            if pedido.estado != CREADO:
                lanzar error "NoModificable"
            idx = buscarLinea(pedido.lineas, datos.productoId)
            if idx existe:
                nuevaCantidad = pedido.lineas[idx].cantidad + datos.cantidad
                pedido.lineas[idx].cantidad = nuevaCantidad
            sino:
                # RF-1: si el producto ya estaba en línea distinta, se suma a la objetivo
                # modo por defecto: crear nueva línea (según como esté implementado el backend)
                anyadirLinea(pedido, datos.productoId, datos.cantidad)
            recalcularTotal(pedido)
            retornar pedido

        caso "eliminar":
            if pedido.estado != CREADO:
                lanzar error "NoModificable"
            idx = buscarLinea(pedido.lineas, datos.productoId)
            si no idx:
                lanzar error "ProductoNoEnPedido"
            nuevaCantidad = pedido.lineas[idx].cantidad - datos.cantidad
            if nuevaCantidad <= 0:
                borrarLinea(pedido, idx)
                recalcularTotal(pedido)
                si todas_líneas_vacias:
                    total = 0
            sino:
                pedido.lineas[idx].cantidad = nuevaCantidad
                recalcularTotal(pedido)
            retornar pedido
```

## 4. Cómo se pinta en la interfaz

1. **CocinaPage** itera sobre `pedidosPorEstados(['CREADO'])` (o usa `usePedidosPorEstados(['CREADO'])`).
2. **Cada fila de pedido** incluye `<PedidoLineaInput productoId={producto.id} cantidadActual={linea.cantidad} pedidoEstado={pedido.estado} />`.
3. **PedidoLineaInput**:
   - Si `pedidoEstado !== 'CREADO'`: muestra `input readonly` con la cantidad y un toast informativo "Modificación imposible: pedido ya avanzó de estado".
   - Si `pedidoEstado === 'CREADO'`:
     - Muestra `input type="number"` con valor `cantidadActual` y botones +/−.
     - `onChange` dispara `agregarProductoAPedido` (para incrementar) o `eliminarProductoDePedido` (para decrementar).
   - **Resultado**: Al confirmar, el componente recibe del hook `usePedidoModificaciones` el `pedidoActualizado` y vuelve a renderizar la fila con el nuevo total; también muestra `toast.success("Unidad(es) actualizada(s) en [producto]: [cambio], total del pedido actualizado a $[total]")`.
   - **Caso especial**: Si el total queda en 0 (última línea eliminada), el toast muestra el total 0 y la fila puede ocultarse o mostrar "Pedido vacío".
4. **Modificaciones repetidas**: El usuario puede clicar +/− cuantas veces quiera mientras el pedido esté en CREADO; cada cambio sobrescribe el anterior.

## 5. Decisiones técnicas justificadas

| Decisión | Justificación | Alternativa descartada |
|---|---|---|
| **Reutilizar funciones comentadas en lugar de crear nuevas** | El proyecto ya tenía `agregarProductoAPedido` y `eliminarProductoDePedido` comentadas en `pedidosApi.js`; reutilizarlas evita duplicar lógica y respeta el principio "no crear código muerto sin implementar". | Crear 3 funciones nuevas desde cero (más código, más mantenimiento). |
| **Lógica en service, no en controller** | La Constitución manda "Controller → Service → Repository". La lógica de negocio (validación CREADO, acumulación, eliminación) está en el service para posible testing con Mockito. | Poner toda la lógica en el controller (acoplamiento a formato DTO). |
| **Frontend deshabilita input si estado !== CREADO** (RF-4) | La Constitución máquina de estados: "No se permiten saltos; desde ENTREGADO ninguna transición es válida". El CREADO es la única ventana de modificación válida. | Permitir edición en cualquier estado y confiar en validaciones de backend. |
| **Total 0 cuando se eliminan todas las líneas** (RF-5) | Consistente con que un pedido sin líneas no tiene sentido monetario; el toast muestra el total real (0). | Mantener el último total conocido o un mínimo fijo. |
| **Tests con `node --test`** | El proyecto usa Vite + React + Node; los tests de la nueva funcionalidad seguirán el mismo patrón de tests unitarios/integración que el resto del frontend. | Tests only on backend o only on frontend. |

## 6. Estrategia de tests con node --test

Dado que el proyecto usa `node --test` para tests frontend (Vite 8), la estrategia es:

- **Archivo: `specs/001-modificar-pedido-creado/pedidosApi.test.jsx`**
  - `describe('agregarProductoAPedido', () => { test('RF-1: acumula sobre línea existente', async () => { ... }) })
  - `describe('eliminarProductoDePedido', () => { test('RF-2: resultado 0 elimina línea, total 0 si última', async () => { ... }) })
  - `describe('RF-3', () => { test('nueva línea de producto no incluido originalmente', async () => { ... }) })`
  
- **Archivo: `specs/001-modificar-pedido-creado/CocinaPage.test.jsx`**
  - `describe('modificarPedidoCocina', () => { test('RF-4: input deshabilitado cuando estado !== CREADO', async () => { ... }) })
  - `test('RF-5: toast y total actualizado; total 0 si se eliminan todas las líneas', async () => { ... })`

- **Integración continua**: `npm test` (mvn test backend + npm build+linter frontend) en el checklist de `Criterios de finalización`.

- **Cobertura mínima**: Cada RF tiene al menos un test unitario (node --test) y un test de integración (backend en H2).