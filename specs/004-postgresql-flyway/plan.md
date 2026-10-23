# Plan de implementación — Migrar la base de datos a PostgreSQL y versionar el esquema

Spec aprobada: `specs/004-postgresql-flyway/spec.md`. Este plan solo describe el **cómo**; el
qué está en la spec. Las decisiones técnicas (versionado V1/V2, `ddl-auto=validate`,
Testcontainers, chequeo del motor, retirada de MySQL) ya están cerradas por el usuario.

## 0. Prerrequisito de entorno (importante)

- **Docker debe estar en marcha** para ejecutar `mvn test`: los tests que tocan base de datos usan
  Testcontainers. En este equipo Docker Desktop está instalado (`/Applications/Docker.app`) pero el
  daemon **no está arrancado** ahora mismo. Antes de la verificación final hay que arrancarlo.
  El daemon de Docker es lo único de infraestructura que entra en alcance (los tests lo necesitan);
  Docker Compose, CI y despliegue siguen fuera de alcance.
- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` pasan a apuntar a una instancia PostgreSQL local
  (cómo obtenerla se documenta en `Backend/README_Backend.md`). Docker Compose queda fuera de alcance.

## 1. Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `Backend/pom.xml` | Sustituir el driver MySQL por el de PostgreSQL; añadir Flyway; añadir Testcontainers (test); retirar H2. | RF-1, RF-4, RF-6, RF-11, RF-13 |
| `Backend/src/main/resources/application.properties` | Driver PostgreSQL, `ddl-auto=validate`, eliminar `spring.sql.init.mode` y `defer-datasource-initialization`, configuración Flyway. | RF-1, RF-4, RF-5, RF-10, RF-13 |
| `Backend/src/main/resources/db/migration/V1__crear_esquema.sql` | Crear las 5 tablas con los tipos y nulabilidad que Hibernate espera (para `validate`). | RF-1, RF-5, RF-10, RF-13 |
| `Backend/src/main/resources/db/migration/V2__catalogo_ejemplo.sql` | Insertar el catálogo (terminales, categorías, productos) con IDs explícitos y reiniciar las secuencias. No inserta pedidos. | RF-1, RF-2, RF-3, RF-13 |
| `Backend/src/main/resources/data.sql` | **Eliminar** (su contenido pasa a V2). | RF-1, RF-2 |
| `Backend/src/main/java/gestionpedidos/config/MotorPostgresqlValidator.java` | Comprobar que `spring.datasource.url` empieza por `jdbc:postgresql:` antes de crear la conexión. | RF-4 |
| `Backend/src/main/resources/META-INF/spring.factories` | Registrar el `EnvironmentPostProcessor` anterior. | RF-4 |
| `Backend/src/main/java/gestionpedidos/repository/PedidoRepository.java` | `findByCodigo` → `findByCodigoIgnoreCase`. | RF-7 |
| `Backend/src/main/java/gestionpedidos/repository/CategoriaRepository.java` | `existsByNombre` → `existsByNombreIgnoreCase`. | RF-8 |
| `Backend/src/main/java/gestionpedidos/repository/TerminalRepository.java` | `existsByNombre` → `existsByNombreIgnoreCase`. | RF-8 |
| `Backend/src/main/java/gestionpedidos/repository/ProductoRepository.java` | `existsByNombre` → `existsByNombreIgnoreCase`. | RF-8 |
| `Backend/src/main/java/gestionpedidos/service/PedidoService.java` | Normalizar (trim) el código antes de buscar; el mensaje de no encontrado se mantiene. | RF-7, RF-9 |
| `Backend/src/main/java/gestionpedidos/service/CategoriaService.java` | Llamar a `existsByNombreIgnoreCase`. | RF-8, RF-9 |
| `Backend/src/main/java/gestionpedidos/service/TerminalService.java` | Llamar a `existsByNombreIgnoreCase`. | RF-8, RF-9 |
| `Backend/src/main/java/gestionpedidos/service/ProductoService.java` | Llamar a `existsByNombreIgnoreCase`. | RF-8, RF-9 |
| `Backend/src/test/java/gestionpedidos/config/PostgresTestcontainersConfig.java` | Contenedor PostgreSQL compartido con `@ServiceConnection`. | RF-11 |
| `Backend/src/test/resources/application-test.properties` | Quitar H2; `spring.test.database.replace=none` y URL PostgreSQL de relleno para el validador. | RF-11 |
| `Backend/src/test/java/gestionpedidos/GestionDePedidosApiApplicationTests.java` | Contexto completo contra el contenedor (Flyway + validate). | RF-1, RF-5, RF-11, RF-13 |
| `Backend/src/test/java/gestionpedidos/PedidoIntegrationTest.java` | Flujo HTTP completo contra el contenedor; añade búsqueda por código en minúsculas. | RF-7, RF-11, RF-12 |
| `Backend/src/test/java/gestionpedidos/repository/PedidoRepositoryTest.java` | `@DataJpaTest` + contenedor; añade `findByCodigoIgnoreCase`. | RF-7, RF-11, RF-13 |
| `Backend/src/test/java/gestionpedidos/repository/ProductoRepositoryTest.java` | `@DataJpaTest` + contenedor; añade `existsByNombreIgnoreCase`. | RF-8, RF-11, RF-13 |
| `Backend/src/test/java/gestionpedidos/service/{Pedido,Categoria,Terminal,Producto}ServiceTest.java` | Actualizar stubs a `...IgnoreCase` y añadir casos de minúsculas/trim. | RF-7, RF-8, RF-9 |
| `Backend/src/test/java/gestionpedidos/config/MotorPostgresqlValidatorTest.java` | Verificar que se rechazan URLs que no son `jdbc:postgresql:`. | RF-4 |
| `AGENTS.md`, `MEMORY.md`, `Backend/README_Backend.md`, `Frontend/README_Frontend.md`, `docs/constitution.md` | Documentar el motor único, Flyway, Testcontainers y el nuevo entorno. | Criterios de finalización |

**No cambian**: `controller/*`, `dto/*`, `model/*`, `exception/*` (salvo que `validate` revele un
ajuste de tipos, que se resolvería en V1) y **todo el frontend** (RF-12: el contrato no cambia).

## 2. Cambios por capa

### Backend

- **model**: sin cambios. `GenerationType.IDENTITY` sigue igual; V1 crea las columnas como
  `GENERATED BY DEFAULT AS IDENTITY` para que V2 pueda insertar IDs explícitos.
- **repository**: `PedidoRepository.findByCodigoIgnoreCase`, `CategoriaRepository.existsByNombreIgnoreCase`,
  `TerminalRepository.existsByNombreIgnoreCase`, `ProductoRepository.existsByNombreIgnoreCase`.
  Las `@Query` de agregación (`obtenerProductosMasVendidos`, `obtenerRankingDeTerminales`) no cambian.
- **service**: `PedidoService.obtenerPedidoPorCodigo` normaliza con `trim()`; los tres servicios de
  catálogo usan el nuevo método `...IgnoreCase`. La lógica de estados, totales y líneas no se toca.
- **controller**: sin cambios.
- **dto**: sin cambios.
- **exception**: sin cambios (el chequeo del motor lanza un error de arranque, no una excepción HTTP).
- **config/recursos**: nuevo `MotorPostgresqlValidator` + `spring.factories`; `application.properties`;
  V1 y V2 en `db/migration`; eliminación de `data.sql`.

### Configuración y recursos

`application.properties` (producción/desarrollo):

```properties
spring.application.name=Gestion_de_pedidos_API

# Datos de conexión a PostgreSQL
spring.datasource.url=${DB_URL}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# La aplicación valida el esquema, no lo crea ni lo modifica
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway aplica el esquema versionado (V1, V2) una sola vez
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration  (valor por defecto)

# Swagger y CORS (sin cambios)
...
app.cors.allowed-origin=${FRONTEND_URL:http://localhost:5173}
```

Se eliminan `spring.sql.init.mode`, `spring.jpa.defer-datasource-initialization` y el driver MySQL.

### Frontend

Sin cambios de código. Solo se actualiza `Frontend/README_Frontend.md` para reflejar que el backend
ya no usa MySQL y que los tests requieren Docker.

## 3. Contrato de la API

**No cambia (RF-12).** Rutas, verbos, códigos HTTP y formas de respuesta se mantienen exactamente
como hoy:

- `POST`/`PUT` de entidad de primer nivel → `{"mensaje": "...", "data": {...}}`.
- `GET` de listados → array directo.
- Sub-recursos de pedido (`agregarProductoAPedido`, `obtenerPedidoPorCodigo`,
  `cambiarEstadoDelPedido`) → DTO directo; `eliminarProductoDePedido` → DTO de la línea o `204`.
- Errores: `ResourceNotFoundException`→404, `BadRequestException`→400, `PedidoStateException`→409,
  todos `{"Error: ": "..."}` (con el espacio en la clave); `@Valid` fallido → `{"Error": "...",
  "detalles": [...]}`; catch-all → `{"Error inesperado: ": "..."}`.
- Los rankings (`GET /api/estadisticas/productos-mas-vendidos` y
  `GET /api/estadisticas/ranking-terminales`) devuelven el mismo resultado y orden.

El chequeo del motor no produce una respuesta HTTP: falla el arranque antes de servir peticiones.

## 4. Algoritmo en pseudocódigo

### 4.1 Comprobación del motor (RF-4)

```
función validarMotorPostgresql(entorno):
    url = entorno.obtenerPropiedad("spring.datasource.url")   # puede ser nula si no hay DB_URL
    si url es nula o url.trim() está vacía:
        retornar                                  # sin URL no hay nada que validar; fallará al conectar
    si no url.startsWith("jdbc:postgresql:"):
        esquema = url.substring(0, url.indexOf(":"))   # p. ej. "jdbc:mysql" o "postgres"
        lanzar ErrorDeArranque(
            "Solo se admite PostgreSQL. Formato esperado: " +
            "jdbc:postgresql://<host>:<puerto>/<base-de-datos>. " +
            "Esquema recibido: " + esquema)       # nunca la URL completa ni credenciales
```

Se ejecuta como `EnvironmentPostProcessor` (antes de crear el `DataSource`), de modo que el rechazo
ocurre aunque el driver MySQL ya no esté en el classpath. Solo se imprime el esquema recibido.

### 4.2 Arranque y validación (RF-1, RF-5, RF-6, RF-10, RF-13)

```
al arrancar:
    1. validarMotorPostgresql(entorno)             # RF-4: antes de conectar
    2. Flyway.migrate()                            # aplica V1 y V2 una sola vez
         - migración ya aplicada y modificada → error claro; no se aplica nada   (RF-6)
         - migración pendiente → se aplica                                            (RF-6)
    3. Hibernate con ddl-auto=validate             # compara entidades con el esquema
         - tabla/columna que falta o tipo incompatible → error que nombra lo que no coincide (RF-5)
         - no crea ni altera nada                                                   (RF-10)
    4. aplicación lista para servir peticiones
```

Flyway deja su historial en `flyway_schema_history`; por eso V2 se ejecuta una sola vez (RF-2, RF-3)
y un cambio posterior de una migración aplicada se detecta por desajuste de checksum (RF-6).

### 4.3 Identificadores y secuencias (caso límite del catálogo y altas nuevas)

`data.sql` insertaba IDs fijos (terminales 1-3, categorías 1-7, productos 1-30). Para conservarlos y,
a la vez, que las altas nuevas por API no choquen con la secuencia, V2 hace:

```sql
INSERT INTO terminales (id, nombre) VALUES (1, 'Caja Principal'), ...;
INSERT INTO categorias (id, nombre) VALUES (1, 'Entrantes para compartir'), ...;
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (1, 'Patatas Fritas Clásicas', 2.90, true, 1), ...;

SELECT setval(pg_get_serial_sequence('terminales', 'id'),  (SELECT MAX(id) FROM terminales));
SELECT setval(pg_get_serial_sequence('categorias', 'id'),  (SELECT MAX(id) FROM categorias));
SELECT setval(pg_get_serial_sequence('productos',  'id'),  (SELECT MAX(id) FROM productos));
```

Así `nextval` continúa en `MAX(id)+1` y crear una categoría/terminal/producto por la API funciona sin
fallo de identificador repetido. `pedidos` y `pedidos_productos` no se siembran, por lo que no
necesitan `setval`.

### 4.4 Insensibilidad a mayúsculas (RF-7, RF-8, RF-9)

PostgreSQL es case-sensitive por defecto (MySQL no lo era), de ahí que haya que añadirla explícitamente:

```
# búsqueda por código (RF-7)
función obtenerPedidoPorCodigo(codigo):
    codigoNormalizado = codigo.trim()                 #RF-7: admite espacios alrededor
    pedido = pedidoRepository.findByCodigoIgnoreCase(codigoNormalizado)
    si no existe → ResourceNotFoundException("Pedido con código " + codigo + " no encontrado")  # RF-7: mismo error

# duplicados (RF-8)
crearCategoria / crearTerminal / crearProducto:
    si repositorio.existsByNombreIgnoreCase(nombre) → PedidoStateException("Ya existe ... con el nombre " + nombre)
```

`...IgnoreCase` es una consulta derivada de Spring Data que genera `UPPER(nombre) = UPPER(?)`,
válida en cualquier motor; el `trim()` se hace en el servicio, que es donde es testeable. El texto se
guarda y se muestra tal como se escribió (RF-9); no se exige insensibilidad a las tildes.

## 5. Decisiones justificadas

| Decisión | Justificación | Alternativa descartada |
|---|---|---|
| Flyway con V1 (esquema) + V2 (catálogo) en lugar de `data.sql` + `ddl-auto=update` | Versionado y reproducible; el esquema se crea y se valida, no se improvisa en cada arranque. | Mantener `data.sql` y `ddl-auto=update`: no versiona, no detecta migraciones modificadas y no es reproducible. |
| `ddl-auto=validate` | La app no crea ni modifica el esquema; detecta desajustes al arranque (RF-5, RF-10). | `update`/`create-drop`: modificarían la base, incumpliendo RF-10. |
| V2 con IDs explícitos + `setval` | Conserva los IDs actuales del catálogo y evita colisiones al dar de alta por API (caso límite). | Dejar que la secuencia asigne los IDs (pierde los IDs estables) o insertar IDs explícitos sin reiniciar la secuencia (colisión al crear por API). |
| `EnvironmentPostProcessor` para el chequeo del motor | Falla antes de crear la conexión y con un mensaje claro, aunque el driver MySQL ya no exista. | Comprobarlo en un `ApplicationRunner`: demasiado tarde (la conexión fallaría antes); o en `main()`: duplica la lectura de configuración y se sale del ciclo de vida de Spring. |
| `findByCodigoIgnoreCase` + `trim()` en el servicio | Insensibilidad a mayúsculas sin tocar el texto guardado y testeable en el servicio. | `@Query` con `LOWER/TRIM` (más SQL manual) o `ILIKE` (solo PostgreSQL, rompe la portabilidad del repositorio). |
| `existsByNombreIgnoreCase` en los tres repositorios | Misma solución uniforme en categorías, terminales y productos. | Índice funcional `UPPER(nombre)` (más complejidad y DDL) o cambiar la collation de la base (afecta a todo y es difícil de versionar). |
| Testcontainers (PostgreSQL) para los tests que tocan base de datos | Mismo motor que la app: tests representativos (RF-11). | H2 (diferencias de SQL/collation) o una PostgreSQL externa (dependencia del entorno y credenciales). |
| `spring.test.database.replace=none` + `@ServiceConnection` | Los `@DataJpaTest` usan el contenedor en lugar de H2 embebida, sin anotar cada clase. | Anotar cada test con `@AutoConfigureTestDatabase(replace=NONE)` (más repetitivo y fácil de olvidar). |
| Eliminar H2 | H2 deja de usarse; no se mantienen dos motores en los tests. | Mantener H2 para algún test: rompe la regla de "mismo motor" y la suite quedaría menos representativa. |
| Actualizar los 5 documentos | Criterio de finalización de la spec; evita documentación obsoleta sobre MySQL/H2. | No actualizarlos: la documentación mentiría sobre el entorno y los comandos. |

## 6. Estrategia de tests

**Motor**: todas las pruebas que arrancan base de datos (repositorio, integración y contexto completo)
usan PostgreSQL en un contenedor (`postgres:17-alpine`) vía Testcontainers. Service y Controller no
tocan base de datos y no cambian de enfoque.

| Parte | Qué se prueba | RF |
|---|---|---|
| `MotorPostgresqlValidatorTest` (nuevo) | Rechaza `jdbc:mysql://...` y `postgres://...`; acepta `jdbc:postgresql://...`; el mensaje no incluye credenciales. | RF-4 |
| Service tests (Mockito, sin BD) | Actualizan los stubs a `...IgnoreCase`; nuevos casos: búsqueda por código en minúsculas y con espacios; duplicados que solo difieren en mayúsculas (categoría, terminal, producto). | RF-7, RF-8, RF-9 |
| Controller tests (`@WebMvcTest`) | **Sin cambios**: el contrato y los códigos HTTP siguen igual. | RF-12 |
| Repository tests (`@DataJpaTest` + contenedor) | Las `@Query` de agregación (`obtenerProductosMasVendidos`, `obtenerRankingDeTerminales`) y los nuevos `findByCodigoIgnoreCase` / `existsByNombreIgnoreCase`. | RF-7, RF-8, RF-11, RF-12, RF-13 |
| Integración (`TestRestTemplate` + contenedor) | Flujo completo HTTP contra PostgreSQL; añade búsqueda por código en minúsculas. | RF-7, RF-11, RF-12 |
| Contexto completo (`@SpringBootTest` + contenedor) | Flyway aplica V1+V2 y `validate` acepta el esquema (arranca la app completa). | RF-1, RF-5, RF-11, RF-13 |
| Verificación final | `mvn test` en verde con Docker en marcha; sin errores nuevos. | RF-11, RF-12 |

Los tests de repositorio son transaccionales (`@DataJpaTest` hace rollback), así que comparten contenedor
entre ellos sin interferir; el test de integración usa su propio contexto/contenedor. El catálogo
sembrado por V2 no choca con los nombres que crean los tests existentes.

**Casos límite que deben quedar cubiertos**: base vacía → esquema + catálogo (contexto completo);
reinicio sobre base ya inicializada → no duplica (Flyway no repite V2); código en minúsculas y con
espacios; duplicados solo por mayúsculas en las tres entidades; alta nueva tras el catálogo sin colisión
de IDs; URL no PostgreSQL rechazada.

## 7. Riesgos y notas

- **Docker**: `mvn test` falla si el daemon no está arrancado. Hay que arrancar Docker Desktop antes
  de la verificación. Si no se quiere depender de Docker para los tests de Service/Controller, estos no
  lo necesitan (no tocan BD).
- **`validate` y los tipos**: V1 debe reflejar con exactitud los tipos y nulabilidad del modelo
  (`numeric(19,2)` para `BigDecimal`, `timestamp(6)` para `LocalDateTime`, `varchar(255)` para el enum).
  Si `validate` da un falso positivo de tipo, se ajusta V1, no el modelo.
- **`IgnoreCase` y tildes**: no se exige insensibilidad a tildes; el comportamiento puede cambiar respecto
  a MySQL y eso está aceptado por la spec.
- **`data.sql` y dependencias**: eliminar `data.sql` y añadir dependencias (PostgreSQL, Flyway,
  Testcontainers) están en la lista de "⚠️ Pregunta antes" de `AGENTS.md`, pero el usuario ya los aprobó
  en esta spec.
