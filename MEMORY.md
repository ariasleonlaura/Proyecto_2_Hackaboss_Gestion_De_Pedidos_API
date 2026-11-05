# MEMORY.md — Gestión de Pedidos (Backend + Frontend)

Memoria del proyecto entre sesiones (máximo ~50 líneas): resume o elimina lo que ya no aporte.

## Estado actual

- Backend completo: CRUD Categoría/Producto/Terminal, ciclo de vida de Pedido, Estadísticas (productos más vendidos, ranking terminales).
- Tests backend: **124 en verde** (`mvn -f Backend/pom.xml test -Dspring.profiles.active=test`), incluidos integración y repositorio contra PostgreSQL real con Testcontainers (arreglo de T9, spec 005) y el test de migraciones (T11, spec 004). Frontend: flujo Terminal/Cocina/Recogida funcionando, "Los más vendidos" en carta de Terminal, sin tests automáticos de UI.
- Spec 005 Docker: T1-T9 completadas (T5-T7 las ejecuta el usuario con Docker Desktop).
- Rama fix/frontend-codigo-y-buscador: toast de Terminal lee ya pedidoCreado.data.codigo (POST de primer nivel → sobre {mensaje, data}); build+lint verdes y E2E en navegador verificado (toast con código real, POST 201, consola limpia, móvil OK); a la vez se añadieron id+name al input de búsqueda de RecogidaPage (aviso de a11y de Chrome desaparecido); y RecogidaPage llama a refetch tras cobrar/entregar (antes solo polling de 15 s: 6,7 s/5,8 s → 64 ms/22 ms).

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
- El backend de Docker en el puerto 8080 usa FRONTEND_URL=http://localhost:8081: desde el dev en 5173 todo falla por CORS ("No Access-Control-Allow-Origin"). Diagnosticar abriendo el endpoint en una pestaña (la navegación no aplica CORS) y probando el frontend del 8081; solución: docker compose stop backend y backend del IDE en el 8080.
- Tras una acción que cambia datos, llamar a refetch() en el mismo onClick (patrón de CocinaPage); fiarse solo del polling de 15 s deja la vista desactualizada hasta el siguiente tick.

### Tests con Testcontainers (arreglo T9)

- En clase de test funciona `@Import(PostgresTestcontainersConfig.class)` (lo procesa `ImportsContextCustomizer` de Spring Boot). `@ContextConfiguration(classes=...)` con una clase que NO es `@TestConfiguration` sustituye la config de la app → `contextLoads` pasa vacío (falso verde).
- No hardcodear `spring.datasource.url/username/password` en `application-test.properties`: pisa a `@ServiceConnection` y el contenedor publica un puerto aleatorio → "Connection refused". Driver e dialect los infieren Boot/Hibernate; en tests `spring.flyway.enabled=false` con `ddl-auto=create`.
- Testcontainers 2.x renombró artefactos (`postgresql` → `testcontainers-postgresql`); el BOM de Spring Boot solo gestiona los nuevos → nunca mezclar módulos 1.x con core 2.x.
- `MotorPostgresqlValidator` captura `IllegalArgumentException` de `${DB_URL}` sin definir: no debe romper el arranque.
- Migraciones probadas aparte (`MigracionFlywayTest`): perfil `test` + `@SpringBootTest(properties={flyway=true, ddl-auto=validate})` contra un contenedor vacío. Habilitar Flyway en `application-test.properties` rompería los tests (V2 siembra catálogo con nombres únicos).

## Próximos pasos

- Opcional: migrar `listarProductos` a `Specification`.

## Specs anteriores

- **004**: PostgreSQL + Flyway V1/V2, chequeo del motor al arranque, insensibilidad a mayúsculas, Testcontainers, documentación actualizada; hueco del arranque con Flyway+validate cerrado con `MigracionFlywayTest` (T11).
- **005 Docker**: dos modos (`docker compose up --build` / `up --wait db`); compose sin clave `version`; `${DB_PASSWORD:?...}` obligatoria antes de crear contenedores; BD publicada solo en `127.0.0.1`; etiqueta `postgres:17-alpine` coherente con tests; `VITE_API_URL` se fija al construir; `.env` real en raíz ignorado y `.env.example` versionada. Las advertencias de casos límite (puerto 8080 con IDE, contraseña distinta de BD creada, cambio de versión mayor, caracteres especiales, primer arranque lento, modos comparten datos) están documentadas en README y AGENTS.md.
