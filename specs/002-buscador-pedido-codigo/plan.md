# Plan de implementación — Buscador de pedido por código en Recogida

> Documento retrospectivo: la funcionalidad ya está implementada. Este plan describe
> cómo se construyó, archivo por archivo, para dejar constancia de las decisiones tomadas.
> Commit principal: `20d4914` (backend + frontend del buscador).

## 1. Archivos afectados

| Archivo | Responsabilidad | RF cubierta |
|---|---|---|
| `Backend/.../controller/PedidoController.java` | Endpoint `GET /api/pedidos/codigo/{codigo}`. Recibe el código como path variable y devuelve el `PedidoDto` (DTO directo, sin envoltorio). | RF-3 |
| `Backend/.../service/PedidoService.java` | `obtenerPedidoPorCodigo(codigo)`: busca por código y, si no existe, lanza `ResourceNotFoundException` (→ 404). | RF-3, RF-5 |
| `Backend/.../repository/PedidoRepository.java` | `findByCodigo(String codigo)` → `Optional<Pedido>`. | RF-3, RF-5 |
| `Frontend/src/api/pedidosApi.js` | `buscarPedidoPorCodigo(codigo)`: llamada al endpoint vía `fetchJSON`. | RF-3 |
| `Frontend/src/hooks/usePedidoPorCodigo.js` | Hook que, dado un código, dispara la búsqueda y expone `{ pedido, loading, error }`. | RF-3, RF-5 |
| `Frontend/src/pages/RecogidaPage.jsx` | Campo de texto + botón "Buscar", tarjeta de resultado, acciones de cobro/entrega y "Cancelar". | RF-1, RF-2, RF-4, RF-6, RF-7, RF-8, RF-9, RF-10, RF-11 |
| `Frontend/src/styles/RecogidaPage.css` | Estilos de `.busqueda-bar`, `.busqueda-input`, `.btn-buscar` y de la tarjeta de resultado. | RF-12 |

## 2. Decisiones técnicas justificadas

| Decisión | Justificación | Alternativa descartada |
|---|---|---|
| **Endpoint propio `GET /api/pedidos/codigo/{codigo}`** | El código es el identificador único y legible del pedido; un endpoint dedicado es más claro que reutilizar el listado con filtro. | Filtrar en frontend sobre la lista de pendientes (no serviría para pedidos en otros estados, RF-8). |
| **DTO directo como respuesta (sin `{"mensaje", "data"}`)** | Es un sub-recurso de pedido; el contrato REST del proyecto devuelve el DTO directo en estos casos. | Envolverlo como un `POST`/`PUT` de primer nivel (rompería el contrato). |
| **`ResourceNotFoundException` → 404** | Un código inexistente es un recurso no encontrado; reutiliza el manejador global ya existente. | Devolver 400 o un 200 con `null` (menos semántico). |
| **Búsqueda con `trim()` en frontend (RF-2)** | El código se pega a menudo desde otro sitio; recortar espacios evita fallos "tontos". El backend no recorta (duda abierta sobre minúsculas). | Recortar solo en backend (habría que elegir dónde; se hace en el punto de entrada del usuario). |
| **Tarjeta reutiliza `pedido-listo-card` + `claseSegunEstado`** | RF-12 pide el mismo aspecto que la lista; reutilizar la clase y el modificador por estado lo garantiza sin duplicar CSS. | Crear una tarjeta nueva con estilos propios (duplicaría el aspecto). |
| **Acciones solo en LISTO/PAGADO (`puedeGestionarse`)** | La máquina de estados solo permite cobrar (LISTO→PAGADO) y entregar (PAGADO→ENTREGADO). Otros estados se muestran sin acción (RF-8). | Ofrecer siempre la acción y que el backend rechace (peor UX). |
| **La lista se actualiza por polling, no tras la acción** | `usePedidosPorEstados` ya refresca la lista cada 15 s; al cerrar la tarjeta (RF-9) la lista vuelve a verse y se actualiza sola. | Forzar un `refetch` inmediato tras cada acción (dependencia extra; el polling ya cubre RF-9). |

## 3. Cómo se pinta en la interfaz

1. **Barra de búsqueda** (`.busqueda-bar`): un `input` (`.busqueda-input`) y un botón "Buscar" (`.btn-buscar`). La lista de pendientes **no** se retira (RF-1).
2. **Al pulsar "Buscar"** (`handleBuscar`):
   - Se hace `codigoBusqueda.trim()` (RF-2).
   - Si queda vacío → `toast.warning("Introduce un código de pedido")` y no se muestra tarjeta (RF-4).
   - Si existe → se muestra la tarjeta y `toast.success("Pedido encontrado: <código>")` (RF-3).
   - Si no existe → `toast.error("Pedido no encontrado con ese código")` y se oculta la tarjeta (RF-5).
3. **Tarjeta de resultado** (`.pedido-listo-card--busqueda`): código, total con 2 decimales y €, y según el estado:
   - `LISTO` → botón "Cobrar Pedido" (RF-6).
   - `PAGADO` → botón "Entregado al Cliente" (RF-7).
   - Otro estado → texto "Estado: X (no se gestiona desde Recogida)", sin acciones (RF-8).
   - Botón "Cancelar" que cierra la tarjeta sin tocar nada (RF-11).
4. **Tras una acción con éxito** se cierra la tarjeta (`setMostrarPedidoEncontrado(false)`) y la lista se actualiza con el siguiente refresco automático (RF-9). Si falla, `toast.error` y la tarjeta permanece (RF-10).

## 4. Estrategia de tests y verificación

- **Backend**: el 404 de `obtenerPedidoPorCodigo` queda cubierto por `PedidoControllerTest` (el handler global de `ResourceNotFoundException` ya estaba testeado).
- **Frontend**: verificación manual con Chrome DevTools (campo vacío, código inexistente, código con espacios, cobrar y entregar desde la tarjeta, cancelar) y `npm run lint` + `npm run build` sin errores.
- **Casos límite cubiertos**: código con espacios al final (RF-2), pedido en CREADO/PREPARACION/ENTREGADO sin acciones (RF-8), código inexistente (RF-5), campo vacío (RF-4).

## 5. Notas retrospectivas

- El buscador se implementó en el commit `20d4914`, junto con la modificación de pedidos en Cocina (spec 001).
- La tarjeta de resultado convive con la lista de pendientes: cuando hay una búsqueda activa, la lista queda oculta y solo se ve la tarjeta; al cancelar o completar una acción, la lista vuelve a mostrarse.
- La actualización de la lista tras una acción depende del polling de `usePedidosPorEstados` (15 s), no de un refresco forzado (ver RF-9 y la duda abierta sobre el refresco de la tarjeta).
