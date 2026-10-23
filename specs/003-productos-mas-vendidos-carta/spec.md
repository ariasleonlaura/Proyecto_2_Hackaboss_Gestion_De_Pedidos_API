# Spec 003 — Productos más vendidos en la carta
Estado: implementada

## Contexto y objetivo
En la pantalla de Terminal, el cliente elige los productos navegando por las categorías de la carta. Los productos más pedidos suelen ser siempre los mismos, y el sistema ya calcula cuáles son. El objetivo es mostrar esos productos en una sección propia, al principio de la carta, desde la que se puedan añadir directamente al carrito, para agilizar el pedido.

Nota: esta spec se ha redactado después de la implementación, para documentar el comportamiento ya existente.

## Usuarios
- Cliente que hace su pedido en la terminal de autoservicio.
- Personal de caja que registra el pedido en una terminal.

## Historias de usuario
- HU-1. Como cliente, quiero ver los productos más vendidos al principio de la carta para elegir rápido lo más popular.
- HU-2. Como cliente, quiero añadirlos al carrito desde esa misma sección para no tener que buscar su categoría.
- HU-3. Como personal de caja, quiero que no se ofrezcan productos que ya no se venden para evitar pedidos que serán rechazados.

## Definiciones
- **Carta**: listado de productos de la pantalla de Terminal, visible una vez elegida la terminal.
- **Más vendidos**: productos ordenados de mayor a menor por el total de unidades vendidas, sumando las líneas de todos los pedidos registrados.
- **Producto activo**: producto disponible para la venta. Es el que se ofrece en la carta.

## Requisitos funcionales
- RF-1: MIENTRAS haya una terminal seleccionada, EL SISTEMA mostrará en la carta una sección "Los más vendidos", situada encima de las categorías, con un recuadro del mismo aspecto que el de cada categoría.
- RF-2: EL SISTEMA mostrará en la sección, como máximo, los 5 productos activos con más unidades vendidas, ordenados de mayor a menor.
- RF-3: EL SISTEMA mostrará, de cada producto de la sección, su nombre, su precio actual, el total de unidades vendidas y un botón "Añadir".
- RF-4: CUANDO el usuario pulse "Añadir" en un producto de la sección, EL SISTEMA añadirá ese producto al carrito con el mismo efecto que el botón "Añadir" de las categorías.
- RF-5: EL SISTEMA no mostrará en la sección productos desactivados, aunque figuren entre los más vendidos; los sustituirá por los siguientes productos activos del ranking hasta completar 5.
- RF-6: EL SISTEMA identificará cada producto del ranking por un identificador y no por su nombre, de modo que dos productos con el mismo nombre se distingan.
- RF-7: SI no hay ningún producto activo con ventas, ENTONCES EL SISTEMA mostrará dentro del recuadro el mensaje "No hay productos en la lista".
- RF-8: SI no se puede obtener el ranking, ENTONCES EL SISTEMA ocultará la sección y el resto de la carta seguirá funcionando con normalidad.
- RF-9: MIENTRAS se carga el ranking, EL SISTEMA no mostrará la sección.
- RF-10: EL SISTEMA mostrará el precio de cada producto con dos decimales y el símbolo €.

## Requisitos no funcionales
- Todos los textos están en español.
- La sección no debe retrasar ni impedir que aparezcan las categorías.
- El ranking se obtiene al entrar en la pantalla y no se actualiza automáticamente mientras permanece abierta.
- La sección se adapta a pantallas estrechas igual que las categorías.
- Como el resto de la aplicación, la pantalla no requiere autenticación.

## Casos límite
- Instalación sin ningún pedido registrado: se muestra el mensaje de RF-7.
- Un producto muy vendido que se desactiva: no aparece y otro activo ocupa su lugar (RF-5).
- Menos de 5 productos activos con ventas: se muestran los que haya.
- Empate de unidades vendidas: el orden entre los empatados no está especificado.
- Dos productos con el mismo nombre: aparecen como entradas distintas, cada una con sus unidades (RF-6).
- Un pedido que se elimina por quedarse sin líneas deja de contar en el ranking.
- Producto ya presente en el carrito: añadirlo desde esta sección sigue la misma regla que desde las categorías.
- Falla el ranking pero cargan los productos: la sección se oculta y la carta funciona (RF-8).
- Se registra un pedido con la pantalla abierta: el ranking no cambia hasta volver a entrar en la pantalla.

## Fuera de alcance
- Ranking por periodo (hoy, semana, mes) o por categoría.
- Mostrar la posición, gráficos o un panel completo de estadísticas de ventas.
- Personalizar los productos por cliente.
- Cambiar el número de productos mostrados por configuración.
- Ranking por ingresos en vez de por unidades.

## Criterios de finalización
- Con ventas registradas, la sección aparece encima de las categorías con hasta 5 productos activos, de mayor a menor.
- "Añadir" en la sección añade el producto al carrito igual que en las categorías.
- Un producto desactivado no aparece y otro activo ocupa su lugar.
- Sin ventas, aparece "No hay productos en la lista".
- Con el ranking no disponible, la sección no aparece y las categorías funcionan con normalidad.
- Dos productos con el mismo nombre aparecen como entradas distintas, con sus propias unidades.
- El recuadro de la sección tiene el mismo aspecto que el de las categorías.

## Dudas abiertas
- [NECESITA ACLARACIÓN] Hoy el ranking suma las unidades de todos los pedidos, también los que aún no se han cobrado. ¿Debe contar solo lo cobrado o entregado?
- [NECESITA ACLARACIÓN] ¿Debe actualizarse el ranking mientras la pantalla está abierta, o basta con que se actualice al volver a entrar?
- [NECESITA ACLARACIÓN] ¿Debe ser configurable el límite de 5 productos?