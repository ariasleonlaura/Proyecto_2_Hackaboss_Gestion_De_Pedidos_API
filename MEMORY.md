# MEMORY.md — Gestión de Pedidos (Backend + Frontend)

Memoria del proyecto entre sesiones (máximo ~50 líneas): resume o elimina lo que ya no aporte.

## Estado actual

- Backend completo: CRUD Categoría/Producto/Terminal, ciclo de vida de Pedido, Estadísticas (productos más vendidos, ranking terminales).
- Tests backend: **124 en verde** (`mvn -f Backend/pom.xml test -Dspring.profiles.active=test`), incluidos integración y repositorio contra PostgreSQL real con Testcontainers (arreglo de T9, spec 005) y el test de migraciones (T11, spec 004). Frontend: flujo Terminal/Cocina/Recogida funcionando, "Los más vendidos" en carta de Terminal, sin tests automáticos de UI.
- Spec 005 Docker: T1-T9 completadas (T5-T7 las ejecuta el usuario con Docker Desktop).

## Decisiones (y por qué)

- Catálogo (CRUD producto/categoría/terminal) vive en backend/Swagger, no en frontend — separación backoffice vs. operativo.
- DTOs siempre clase Lombok, nunca `record` ni MapStruct — coherencia con el resto del proyecto.
- `listarProductos` sigue en memoria (streams), no `Specification` — rehacer tests ya verificados no compensaba; mejora opcional.
- Paquete simplificado a `gestionpedidos` (sin `com.empresa`) — proyecto de portfolio, no librería a publicar.
- Rediseño visual completado: tokens unificados (`tokens.css`), dark mode por SO, iconos Lucide, responsive en las 3 páginas.
- Duplicados de catálogo insensibles a mayúsculas (`existsByNombreIgnoreCase`); el texto se guarda y muestra tal como se escribió.

## Aprendizajes y errores a evitar

- Spring Boot 4.1 reubicó `@WebMvcTest`/`@DataJpaTest`/`TestRestTemplate` a paquetes nuevos.
- `GROUP BY` incluye todas las columnas no agregadas del `SELECT`; `BigDecimal` → `isEqualByComparingTo`; mocks `any(Dto.class)`.
- Pedido vacío se elimina al quitar la última línea; no crear `Backend/.env` (Spring no lo lee).
- Ruta del proyecto: solo la de este repositorio. No sobreescribas archivos enteros sin necesidad.

### Tests con Testcontainers (arreglo T9)

- En clase de test funciona `@Import(PostgresTestcontainersConfig.class)` (lo procesa `ImportsContextCustomizer` de Spring Boot). `@ContextConfiguration(classes=...)` con una clase que NO es `@TestConfiguration` sustituye la config de la app → `contextLoads` pasa vacío (falso verde).
- No hardcodear `spring.datasource.url/username/password` en `application-test.properties`: pisa a `@ServiceConnection` y el contenedor publica un puerto aleatorio → "Connection refused". Driver e dialect los infieren Boot/Hibernate; en tests `spring.flyway.enabled=false` con `ddl-auto=create`.
- Testcontainers 2.x renombró artefactos (`postgresql` → `testcontainers-postgresql`); el BOM de Spring Boot solo gestiona los nuevos → nunca mezclar módulos 1.x con core 2.x.
- `MotorPostgresqlValidator` captura `IllegalArgumentException` de `${DB_URL}` sin definir: no debe romper el arranque.
- Migraciones probadas aparte (`MigracionFlywayTest`): perfil `test` + `@SpringBootTest(properties={flyway=true, ddl-auto=validate})` contra un contenedor vacío. Habilitar Flyway en `application-test.properties` rompería los tests (V2 siembra catálogo con nombres únicos).

## Próximos pasos

- Opcional: migrar `listarProductos` a `Specification`.
- Verificación visual opcional pendiente: limpieza de código muerto de Recogida (hook `usePedidoPorCodigo`, bloque CSS `.pedido-encontrado-card`, clase `pedido-listo-card--sin-accion`) no cambia aspecto por diseño, pero no se revisó en navegador con backend arrancado.

## Specs anteriores

- **004**: PostgreSQL + Flyway V1/V2, chequeo del motor al arranque, insensibilidad a mayúsculas, Testcontainers, documentación actualizada; hueco del arranque con Flyway+validate cerrado con `MigracionFlywayTest` (T11).
- **005 Docker**: dos modos (`docker compose up --build` / `up --wait db`); compose sin clave `version`; `${DB_PASSWORD:?...}` obligatoria antes de crear contenedores; BD publicada solo en `127.0.0.1`; etiqueta `postgres:17-alpine` coherente con tests; `VITE_API_URL` se fija al construir; `.env` real en raíz ignorado y `.env.example` versionada. Las advertencias de casos límite (puerto 8080 con IDE, contraseña distinta de BD creada, cambio de versión mayor, caracteres especiales, primer arranque lento, modos comparten datos) están documentadas en README y AGENTS.md.
