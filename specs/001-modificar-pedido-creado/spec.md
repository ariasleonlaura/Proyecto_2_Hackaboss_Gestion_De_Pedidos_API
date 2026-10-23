# Spec 001 — Modificar un pedido ya creado en Cocina
Estado: implementada

## Contexto y objetivo
Permitir al cocinero ajustar la cantidad de productos en un pedido ya creado, mientras el pedido sigue en estado CREADO (antes de avanzar a PREPARACIÓN). El backend ya expone las operaciones `agregarProductoAPedido` y `eliminarProductoDePedido`. El endpoint de eliminar usa `DELETE /api/pedidos/{pedidoId}/productos` con body `PedidoProductoRequestDto` (productoId + cantidad).

## Usuarios
- **Cocinero**: Ajusta unidades de productos en pedidos para corregir errores o cambios de última hora antes de la preparación.

## Historias de usuario
- Como cocinero, quiero añadir una unidad más de un producto que ya pedí, para no perder el pedido ni crear uno nuevo.
- Como cocinero, quiero quitar o reducir unidades de un producto ya pedido, con acumulación sobre la línea existente.
- Como cocinero, quiero que el sistema me impida modificar pedidos que ya avanzaron de estado (no CREADO).

## Requisitos funcionales
RF-1: El cocinero puede incrementar la cantidad de una línea de producto existente en un pedido en estado CREADO, y la cantidad se acumula sobre la cantidad ya registrada de esa línea. **Si el producto ya existía en el pedido pero en una línea distinta, el comportamiento es el mismo: se suma a la línea objetivo, no se crea una nueva.**

RF-2: El cocinero puede decrementar la cantidad de una línea de producto existente en un pedido en estado CREADO; la cantidad no puede ser negativa. **Si la cantidad resultante es 0, la línea se elimina automáticamente y el total del pedido se recalcula.**

RF-3: El cocinero puede añadir una línea completamente nueva de un producto que no estaba en el pedido original, enviando el producto y la cantidad al backend.

RF-4: El frontend deshabilita la acción de modificar el pedido si el pedido no está en estado CREADO (ej. ya está en PREPARACIÓN o LISTO), impidiendo la solicitud al backend.

RF-5: Al confirmar el cambio, el sistema muestra un toast con el detalle: "Unidad(es) actualizada(s) en [producto]: [cambio], total del pedido actualizado a $[total]". **Si se anulan todas las líneas, el total del pedido queda en 0.**

## Requisitos no funcionales
- **Respuesta esperada (backend)**: `{"mensaje": "...", "data": {...}}` con la línea de pedido actualizada y el nuevo total del pedido.
- **Errores esperados**: `BadRequestException` → `{"Error: ": "..."}` si la cantidad es inválida (negativa, o pedido no está en CREADO).
- **Interfaz**: Input numérico o selector de cantidad en cada línea del pedido, deshabilitado cuando el pedido no está en CREADO.
- **Estado**: Sin cambios en la máquina de estados del pedido; solo se modifica la cantidad de línea(s). **No hay restricción contra modificaciones repetidas mientras el pedido esté en CREADO.**
- **Stock**: El backend acepta cualquier entero positivo; no hay validación de inventario disponible.

## Casos límite
- **Cantidad cero o negativa después de la operación**: Si la operación resulta en cantidad 0, la línea se elimina y el total se recalcula. **Si la operación resulta en cantidad negativa, el backend retorna BadRequestException y muestra toast "Cantidad debe ser como mínimo 1".**
- **Modificar pedido en PREPARACIÓN/LISTO**: Frontend deshabilita el input; toast "No se pueden modificar pedidos ya en estado [PREPARACIÓN/LISTO]".
- **Modificaciones repetidas**: **No hay restricción contra modificaciones repetidas** mientras el pedido esté en CREADO; se pueden modificar tantas veces como se quiera, y cada cambio sobrescribe el anterior.
- **Añadir línea de producto ya existente**: El backend suma la cantidad a la línea existente objetivo; no crea una nueva línea duplicada.
- **Cantidad muy alta**: Sin límite superior definido; el backend acepta cualquier entero positivo.
- **Cambio rápido de opinión**: **Los cambios se sobrescriben** sucesivamente; el último cambio confirmado es el que permanece en el pedido.

## Fuera de alcance
- Modificar el estado del pedido (solo se ajusta cantidad, no se cambia de CREADO a PREPARACIÓN).
- Validaciones de negocio de superposición de líneas (mismo producto en líneas distintas).
- Historial de cambios ni auditoría de quién modificó qué y cuándo.
- Flujo de estado completo (solo CREADO → ajustes → sigue a PREPARACIÓN).

## Criterios de finalización
- [ ] RF-1 implementado: incrementar cantidad en línea existente con acumulación; si el producto ya existía, se suma a la línea objetivo.
- [ ] RF-2 implementado: decrementar cantidad; resultado 0 elimina la línea, resultado negativo da error.
- [ ] RF-3 implementado: añadir línea nueva de producto no incluido originalmente.
- [ ] RF-4 implementado: frontend deshabilita entrada si pedido no está en CREADO.
- [ ] RF-5 implementado: toast con detalle del cambio y total actualizado; total 0 si se eliminan todas las líneas.
- [ ] Pruebas unitarias y de integración para las nuevas operaciones en `pedidosApi.js`.
- [ ] Sin errores nuevos en `npm run lint` ni `mvn test`.

## Dudas abiertas
- [NECESITA ACLARACIÓN] Límite máximo de cantidad por línea: ¿hay que definir un tope o el backend acepta cualquier entero positivo? *(Confirmado: sin tope, entero positivo).*
- [NECESITA ACLARACIÓN] Si el producto ya estaba en el pedido pero en una línea distinta, ¿se suma a la línea objetivo o se crea una nueva? *(Confirmado: se suma a la línea objetivo).*
- [NECESITA ACLARACIÓN] Confirmación doble antes de reducir cantidad a niveles muy bajos: ¿es necesario o el usuario puede hacerlo directamente? *(Confirmado: El usuario puede hacerlo directamente).*