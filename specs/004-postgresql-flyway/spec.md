# Spec 004 — Migrar la base de datos a PostgreSQL y versionar el esquema
Estado: implementada

## Contexto y objetivo
La aplicación hoy usa MySQL en desarrollo y una base en memoria para las pruebas. Para llevar el proyecto a producción se quiere un único motor (PostgreSQL), de modo que desarrollo, pruebas y despliegue usen el mismo, y que el esquema quede versionado y se aplique de forma reproducible. El objetivo de esta spec es: (1) que una base de datos vacía se prepare sola al arrancar —esquema y datos de ejemplo— sin pasos manuales y sin duplicar el catálogo al reiniciar; (2) que el esquema esté versionado y se valide al arrancar, rechazando con un error claro cualquier desajuste; (3) que el comportamiento de la API y de los rankings no cambie (salvo la insensibilidad a las tildes, que no se conserva); y (4) que las pruebas que tocan base de datos se ejecuten contra el mismo motor que la aplicación y la suite completa quede en verde.

Decisiones cerradas con el usuario:
- **Solo PostgreSQL**: se retira MySQL; la aplicación no debe poder arrancar contra él. Al arrancar, la aplicación comprueba que la conexión es a PostgreSQL y, si no lo es, rechaza el arranque con un mensaje claro que indique que solo se admite PostgreSQL y cuál es el formato esperado. El mensaje no muestra la URL completa ni credenciales; como mucho, el esquema recibido.
- **Mayúsculas/minúsculas**: se conserva la insensibilidad en dos comportamientos: la búsqueda de pedido por código y la detección de nombres duplicados (categorías, terminales y productos). El texto guardado y mostrado no se modifica. No se exige insensibilidad a las tildes.
- **Datos de una base recién creada**: solo el catálogo (terminales, categorías, productos); no se cargan pedidos de ejemplo.
- **Cuándo se inserta el catálogo**: el catálogo de ejemplo se inserta una sola vez, en el primer arranque sobre una base vacía. Los arranques posteriores no lo modifican: un producto que se borre, renombre o desactive no reaparece ni se duplica. Para restaurar los datos de ejemplo se parte de una base de datos vacía.
- **Desincronización del esquema**: la aplicación rechaza el arranque con un error claro que indique qué no coincide (tabla o columna que falta, o tipo incompatible), sin modificar la base de datos. La aplicación nunca corrige el esquema por su cuenta. No se exige detectar cambios en restricciones o índices, ni columnas o tablas añadidas a mano; sí se rechaza una migración ya aplicada que se haya modificado.
- **Documentación**: además del código, se actualiza la documentación del proyecto (AGENTS.md, MEMORY.md, los README y la constitución).

## Usuarios
- **Operador / equipo de desarrollo**: arranca la aplicación en un entorno nuevo o de despliegue y quiere que la base se prepare sola y que los desajustes de esquema se detecten al arrancar.
- **Usuarios finales (Terminal, Cocina, Recogida)**: se benefician de que la API y los rankings sigan comportándose igual que antes.
- **Desarrollador**: ejecuta la suite de pruebas y quiere que los tests que tocan base de datos usen el mismo motor que la aplicación.

## Historias de usuario
- HU-1. Como operador, quiero que una base de datos vacía se prepare sola al arrancar, para no tener que ejecutar pasos manuales.
- HU-2. Como operador, quiero que reiniciar la aplicación no duplique ni modifique el catálogo, para que el estado inicial sea estable.
- HU-3. Como operador, quiero que el esquema esté versionado, para que cada entorno aplique los mismos cambios de forma reproducible.
- HU-4. Como operador, quiero que la aplicación avise claramente si el esquema no coincide, para detectar el problema al arrancar y no en mitad de la operación.
- HU-5. Como operador, quiero que la aplicación no arranque contra MySQL, para no mantener dos motores.
- HU-6. Como usuario final, quiero que la API y los rankings sigan comportándose igual, para no romper los flujos existentes.
- HU-7. Como desarrollador, quiero que las pruebas que tocan base de datos usen el mismo motor, para que los tests sean representativos.

## Definiciones
- **Catálogo**: el conjunto de terminales, categorías y productos de ejemplo que debe estar presente en una base recién creada.
- **Esquema**: la estructura de tablas y columnas que la aplicación espera encontrar en la base de datos.
- **Migración**: un cambio versionado y reproducible del esquema, aplicado una sola vez y registrado.
- **Arranque**: el proceso de puesta en marcha de la aplicación, desde que se conecta a la base hasta que queda lista para atender peticiones.
- **Productos más vendidos**: productos ordenados de mayor a menor por el total de unidades vendidas, sumando las líneas de todos los pedidos registrados.
- **Ranking de terminales**: terminales ordenadas de mayor a menor por el número de pedidos registrados en cada una.

## Requisitos funcionales
RF-1: CUANDO se arranca la aplicación contra una base de datos vacía, EL SISTEMA crea el esquema completo y carga el catálogo de ejemplo sin necesidad de pasos manuales.

RF-2: CUANDO la aplicación arranca sobre una base que ya está inicializada, EL SISTEMA no vuelve a cargar el catálogo de ejemplo ni lo duplica; el catálogo existente se mantiene tal cual.

RF-3: MIENTRAS la aplicación está en marcha, EL SISTEMA no inserta, modifica, borra ni restaura datos del catálogo; el catálogo solo se escribe en el primer arranque sobre una base vacía.

RF-4: SI la base de datos no es PostgreSQL, ENTONCES EL SISTEMA rechaza el arranque con un error claro que indique que solo se admite PostgreSQL y cuál es el formato esperado, sin mostrar la URL completa ni credenciales (como mucho, el esquema recibido), y no sirve peticiones.

RF-5: SI el esquema de la base no coincide con el esperado (falta una tabla, falta una columna o una columna no puede guardar el tipo de dato lógico que la aplicación usa), ENTONCES EL SISTEMA rechaza el arranque con un error que indica qué no coincide, sin modificar la base de datos.

RF-6: SI una migración ya aplicada se ha modificado, ENTONCES EL SISTEMA rechaza el arranque con un error claro, sin aplicar ningún cambio; una migración aún pendiente de aplicar no es un error y se aplica al arranque.

RF-7: CUANDO se busca un pedido por su código introducido en minúsculas, EL SISTEMA encuentra el pedido cuyo código es ese, aunque el código guardado tenga mayúsculas.

RF-8: CUANDO se intenta crear una categoría, terminal o producto cuyo nombre solo difiere en mayúsculas o minúsculas de otro ya existente, EL SISTEMA lo rechaza como duplicado.

RF-9: MIENTRAS se conserva la insensibilidad a mayúsculas, EL SISTEMA guarda y muestra el texto tal como se escribió, sin cambiar sus mayúsculas.

RF-10: EL SISTEMA no corrige el esquema por su cuenta; solo las migraciones lo hacen.

RF-11: CUANDO se ejecutan las pruebas que tocan base de datos, EL SISTEMA las ejecuta contra el mismo motor que la aplicación y la suite completa queda en verde.

RF-12: EL SISTEMA mantiene sin cambios las rutas, los códigos de estado y las formas de respuesta de la API, y los resultados de los rankings (productos más vendidos y ranking de terminales), respecto a su comportamiento actual, salvo la insensibilidad a las tildes, que no se conserva.

RF-13: EL SISTEMA versiona los cambios del esquema, de modo que todos los entornos apliquen el mismo conjunto de cambios, en el mismo orden, y lleguen al mismo esquema final.

## Requisitos no funcionales
- **Un único motor**: la aplicación solo funciona con PostgreSQL; no debe poder arrancar contra MySQL.
- **Idempotencia**: arrancar varias veces sobre la misma base deja el catálogo exactamente una vez y no produce errores.
- **Fallo rápido**: los desajustes de esquema y las migraciones modificadas se detectan al arrancar, no en mitad de una operación.
- **No modificación durante la validación**: validar el esquema no altera la base de datos.
- **Reproducibilidad**: el mismo conjunto de cambios de esquema produce el mismo esquema en cualquier entorno.
- **Pruebas representativas**: las pruebas que tocan base de datos usan el mismo motor que la aplicación.
- **Comportamiento observable estable**: rutas, códigos de estado y formas de respuesta de la API no cambian; los resultados de los rankings no cambian.

## Casos límite
- **Base totalmente vacía**: se crea el esquema y el catálogo en el mismo arranque.
- **Base ya inicializada**: no se duplica ni se modifica el catálogo; el arranque es correcto.
- **Catálogo modificado a mano**: un producto del catálogo que se borre, renombre o desactive no reaparece tras reiniciar.
- **Reinicios repetidos**: tras varios arranques el catálogo sigue apareciendo una sola vez.
- **Esquema con una tabla o columna que falta**: el arranque falla con un error que nombra lo que falta.
- **Esquema con un tipo de columna incompatible**: el arranque falla con un error que nombra la columna.
- **Migración aplicada modificada**: el arranque falla con un error claro.
- **Migración pendiente de aplicar**: se aplica al arranque sin error.
- **Búsqueda por código en minúsculas, con o sin espacios al principio o al final**: encuentra el pedido; la búsqueda por código inexistente sigue devolviendo el error de no encontrado actual.
- **Duplicados que solo difieren en mayúsculas**: "Bebidas" y "bebidas" chocan; "Hamburguesa clásica" y "hamburguesa clásica" chocan, tanto en categorías y terminales como en productos.
- **Tildes**: no se exige insensibilidad a las tildes; un nombre con tilde y sin tilde no tienen por qué tratarse como duplicados. El comportamiento respecto al motor anterior puede cambiar.
- **Base que no es PostgreSQL**: el arranque se rechaza con un error claro que no revela la URL ni las credenciales.
- **Rankings sin datos**: en la carta, la sección de más vendidos muestra "No hay productos en la lista".
- **Pedidos de ejemplo**: no se insertan en una base recién creada.
- **Datos del motor anterior**: no se migran; se parte de una base vacía.
- **Identificadores del catálogo y altas nuevas**: tras cargar el catálogo de ejemplo, crear una nueva categoría, terminal o producto por la API debe funcionar sin fallo por identificador repetido.
- **Duplicados concurrentes que solo difieren en mayúsculas**: la unicidad de nombres la comprueba la aplicación; en dos creaciones simultáneas que difieran solo en mayúsculas podría admitirse un duplicado. No se exige evitarlo.
- **Dos arranques simultáneos sobre la misma base vacía**: el comportamiento en ese caso no está garantizado.

## Fuera de alcance
- Empaquetado, integración continua y despliegue con contenedores (Docker).
- Migrar los datos existentes del motor anterior (se parte de cero).
- Detectar cambios en restricciones o índices, ni columnas o tablas añadidas a mano.
- Conservar la insensibilidad a las tildes.
- Cambiar el comportamiento de la API, los rankings o la máquina de estados.
- Garantizar el arranque simultáneo de varias instancias sobre la misma base vacía.

## Criterios de finalización
- [ ] RF-1: una base vacía queda con esquema y catálogo tras arrancar, sin pasos manuales.
- [ ] RF-2: arrancar sobre una base ya inicializada no duplica ni modifica el catálogo.
- [ ] RF-3: mientras la aplicación está en marcha no se inserta, modifica, borra ni restaura el catálogo.
- [ ] RF-4: arrancar contra una base que no sea PostgreSQL se rechaza con un error claro que no revela la URL ni las credenciales.
- [ ] RF-5: un esquema con tabla/columna faltante o tipo incompatible rechaza el arranque con un error que indica qué no coincide, sin modificar la base.
- [ ] RF-6: una migración ya aplicada y modificada rechaza el arranque; una migración pendiente se aplica sin error.
- [ ] RF-7: la búsqueda por código en minúsculas encuentra el pedido.
- [ ] RF-8: los duplicados que solo difieren en mayúsculas se rechazan en categorías, terminales y productos.
- [ ] RF-9: el texto guardado y mostrado no cambia de mayúsculas.
- [ ] RF-10: la aplicación no corrige el esquema por sí misma.
- [ ] RF-11: las pruebas que tocan base de datos usan el mismo motor y la suite completa queda en verde.
- [ ] RF-12: el comportamiento de la API y de los rankings no cambia, salvo la insensibilidad a las tildes.
- [ ] RF-13: cada entorno reproduce el mismo esquema desde una base vacía.
- [ ] Tras cargar el catálogo de ejemplo, crear una nueva categoría, terminal o producto por la API no falla por identificador repetido.
- [ ] No se insertan pedidos de ejemplo en una base recién creada.
- [ ] No se migran datos del motor anterior.
- [ ] Sin errores nuevos en el resto de la suite.
- [ ] Documentación actualizada: AGENTS.md, MEMORY.md, Backend/README_Backend.md, Frontend/README_Frontend.md y docs/constitution.md.

## Dudas abiertas
- [RESUELTA] Datos existentes del motor anterior: no se migran. Se parte de una base vacía que se prepara sola con los datos de ejemplo actuales.
- [RESUELTA] Catálogo: se carga una sola vez, en el primer arranque sobre una base vacía; los arranques posteriores no lo modifican. Para restaurar los datos de ejemplo se parte de una base vacía.
- [RESUELTA] Comprobación del motor: al arrancar se verifica que la conexión sea PostgreSQL; si no, se rechaza el arranque. Cómo se implementa se decide en el plan.
- [RESUELTA] Documentación: actualizar AGENTS.md, MEMORY.md, los README y la constitución.
- [RESUELTA] Fuera de alcance: empaquetado, integración continua y despliegue con contenedores.
- [RESUELTA] La infraestructura necesaria para ejecutar las pruebas que tocan base de datos se decide en el plan; no es objeto de esta spec.
