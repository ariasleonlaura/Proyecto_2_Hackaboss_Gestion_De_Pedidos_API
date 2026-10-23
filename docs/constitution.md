# Constitución — Gestión de Pedidos (Backend + Frontend)

Principios innegociables para sesionesfuture OpenCode:

1. **Capas obligatorias**: Controller → Service → Repository → Entidad. No saltar niveles; los DTOs aíslan entidades en las fronteras.

2. **Contrato REST coherente**: `POST`/`PUT` entidades → `{"mensaje": "...", "data": {...}}`; listados → array directo; sub-recursos de pedido → DTO directo sin envoltorio. Errores `GlobalExceptionHandler`: `{"Error: ": "..."}` (con espacio).

3. **Frontend / Backend separation**: Frontend (React + Vite) consume `/api`, sin autenticación, validaciones solo servidor. Gestión de catálogo (CRUD producto/categoría/terminal) es backend/Swagger, no frontend.

4. **Cobertura de tests exigida**: Servicio con Mockito `@Mock` + `@InjectMocks`; controlador con `@WebMvcTest`; repositorio con `@DataJpaTest` sobre PostgreSQL con Testcontainers; integración con `TestRestTemplate` sin mocks. La suite no requiere MySQL arrancado.

5. **Spring Boot 4.1 compatibility**: `@WebMvcTest` import `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`; `@DataJpaTest` import `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`; `TestRestTemplate` import `org.springframework.boot.resttestclient.TestRestTemplate` + `@AutoConfigureTestRestTemplate`.

6. **Configuración por variables de entorno**: Backend usa `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL` en Run Config del IDE; **no crear `Backend/.env`**. Frontend usa `Frontend/.env` con `VITE_API_URL`.

7. **Versionado de esquema con Flyway**: Migraciones versionadas (V1/V2) gestionan el esquema de PostgreSQL; `spring.sql.init.mode=never` para evitar inserciones automáticas. Columnas `@Column(unique = true)` en entidades. Tests parten de base de datos limpia cada ejecución (limpieza por Testcontainers).

8. **Idioma español**: Código, comentarios y textos de interfaz (`ESTADOS_PEDIDO`, mensajes de toast, errorres del handler) en español. Máquina de estados: `CREADO → PREPARACIÓN → LISTO → PAGADO → ENTREGADO`. No se permiten saltos de estado.

Máximo ~45 líneas. Cumplir estos principios evita sorpreses en entrevistas técnicas y mantiene el proyecto consistente independientemente de quién continúe.