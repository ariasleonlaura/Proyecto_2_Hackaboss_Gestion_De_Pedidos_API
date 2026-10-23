# Spec 002 — Buscador de pedido por código en Recogida
Estado: implementada

## Contexto y objetivo
En la pantalla de Recogida, el personal localiza hoy un pedido recorriendo la lista de pedidos pendientes. Cuando hay muchos pedidos, o cuando el cliente ya trae su código, ese recorrido es lento. El objetivo es poder localizar un pedido directamente por su código y cobrarlo o entregarlo desde el resultado, sin renunciar a la lista.

Nota: esta spec se ha redactado después de la implementación, para documentar el comportamiento ya existente.

## Usuarios
- Personal de caja y recogida, que atiende al cliente y gestiona el cobro y la entrega.
- El cliente interviene de forma indirecta: es quien aporta el código de su pedido.

## Historias de usuario
- HU-1. Como personal de recogida, quiero buscar un pedido por su código para atender al cliente sin recorrer la lista.
- HU-2. Como personal de recogida, quiero cobrar o entregar el pedido encontrado desde el propio resultado para ahorrar pasos.
- HU-3. Como personal de recogida, quiero ver el estado de un pedido que aún no está listo para informar al cliente.

## Definiciones
- **Código de pedido**: identificador único y legible, con formato "PED-" seguido de caracteres, que se genera al registrar el pedido y se muestra al cliente.
- **Pedidos pendientes**: pedidos en estado LISTO (pendientes de cobro) o PAGADO (pendientes de entrega). Son los que muestra la lista de Recogida.
- **Tarjeta de resultado**: la tarjeta que muestra el pedido encontrado por la búsqueda.

## Requisitos funcionales
- RF-1: EL SISTEMA mostrará en la pantalla de Recogida un campo de texto para introducir un código y un botón "Buscar", sin retirar la lista de pedidos pendientes.
- RF-2: CUANDO el usuario pulse "Buscar" con un código, EL SISTEMA buscará el pedido ignorando los espacios al principio y al final del texto introducido.
- RF-3: CUANDO exista un pedido con ese código, EL SISTEMA mostrará una tarjeta de resultado con el código, el importe total y el estado del pedido, y un aviso "Pedido encontrado: <código>".
- RF-4: SI el campo está vacío o solo contiene espacios, ENTONCES EL SISTEMA no realizará la búsqueda, mostrará el aviso "Introduce un código de pedido" y no mostrará ninguna tarjeta de resultado.
- RF-5: SI no existe ningún pedido con ese código, ENTONCES EL SISTEMA mostrará el aviso "Pedido no encontrado con ese código" y ocultará la tarjeta de resultado, si había una abierta.
- RF-6: MIENTRAS el pedido encontrado esté en estado LISTO, EL SISTEMA ofrecerá en la tarjeta la acción "Cobrar Pedido", que lo pasa a PAGADO.
- RF-7: MIENTRAS el pedido encontrado esté en estado PAGADO, EL SISTEMA ofrecerá en la tarjeta la acción "Entregado al Cliente", que lo pasa a ENTREGADO.
- RF-8: MIENTRAS el pedido encontrado esté en un estado distinto de LISTO y PAGADO (CREADO, PREPARACION o ENTREGADO), EL SISTEMA mostrará su estado con un aspecto neutro, distinto al de los pedidos pendientes, y no ofrecerá ninguna acción de cobro ni de entrega.
- RF-9: CUANDO la acción de la tarjeta se complete con éxito, EL SISTEMA cerrará la tarjeta de resultado y actualizará la lista de pedidos pendientes.
- RF-10: SI la acción de la tarjeta falla, ENTONCES EL SISTEMA mostrará un aviso de error y mantendrá la tarjeta abierta.
- RF-11: CUANDO el usuario pulse "Cancelar" en la tarjeta, EL SISTEMA la cerrará sin modificar ningún pedido.
- RF-12: EL SISTEMA mostrará la tarjeta de resultado con el mismo aspecto que las tarjetas de la lista de pendientes: mismo fondo según el estado (LISTO o PAGADO) y misma distribución de la información.

## Requisitos no funcionales
- Todos los textos y avisos están en español.
- La búsqueda no modifica ningún dato: solo las acciones de cobro y entrega lo hacen.
- La actualización automática de la lista de pendientes no cierra ni sustituye la tarjeta de resultado abierta.
- Como el resto de la aplicación, la pantalla no requiere autenticación.
- El diseño en pantallas pequeñas no se ha verificado a fondo (limitación conocida del proyecto).

## Casos límite
- Código con espacios al principio o al final (por ejemplo, al pegarlo desde otro sitio): se ignoran (RF-2).
- Código escrito en minúsculas: ver dudas abiertas.
- Pedido que ya está ENTREGADO, o que aún no está LISTO: se muestra sin acciones (RF-8).
- Pedido que cambia de estado desde otro puesto mientras la tarjeta está abierta: la tarjeta sigue mostrando el estado del momento de la búsqueda. Si la acción ya no es válida, falla y se aplica RF-10.
- Nueva búsqueda con una tarjeta abierta: la tarjeta anterior se sustituye por la nueva, o se oculta si no hay resultado (RF-5).
- Servidor no disponible: ver dudas abiertas.

## Fuera de alcance
- Búsqueda parcial, o por otros datos (terminal, fecha, importe), y resultados múltiples.
- Lectura de códigos QR o de barras.
- Modificar un pedido desde Recogida.
- Buscador en las pantallas de Terminal o Cocina.
- Historial de búsquedas.

## Criterios de finalización
- Un pedido existente en LISTO se encuentra por su código y se cobra desde la tarjeta; pasa a PAGADO.
- Un pedido en PAGADO se encuentra y se entrega desde la tarjeta; pasa a ENTREGADO.
- Un código inexistente muestra el aviso y ninguna tarjeta.
- Un campo vacío no lanza la búsqueda y muestra el aviso.
- Un pedido en CREADO, PREPARACION o ENTREGADO se muestra sin ninguna acción.
- Un código con un espacio al final se encuentra igual.
- "Cancelar" cierra la tarjeta sin cambiar ningún pedido.
- La lista de pendientes sigue visible y se actualiza sola con la tarjeta abierta.
- La tarjeta tiene el mismo aspecto que las de la lista.

## Dudas abiertas
- [NECESITA ACLARACIÓN] ¿Debe encontrarse el pedido si el código se escribe en minúsculas? Hoy depende de cómo compare textos la base de datos, y con el cambio previsto a PostgreSQL el comportamiento podría cambiar.
- [NECESITA ACLARACIÓN] Cualquier fallo de la búsqueda, incluido el servidor no disponible, muestra el mismo aviso "Pedido no encontrado con ese código". ¿Debe distinguirse un fallo de conexión de un código inexistente?
- [NECESITA ACLARACIÓN] ¿Debe refrescarse el estado de la tarjeta abierta cuando el pedido cambia desde otro puesto?