# AGENTS.md — Gestión de Pedidos (Backend + Frontend)
Sistema de gestión de pedidos para terminales de autoservicio (Terminal → Cocina → Recogida). Backend didáctico (comentarios explicativos abundantes); frontend simple, sin estado global ni autenticación.

## Stack y estructura
- **Backend** (`Backend/`): Java 17, Spring Boot 4.1.0, JPA + PostgreSQL (dev) / Testcontainers (test), Lombok. Paquetes bajo `gestionpedidos/`: `model`, `repository`, `service`, `controller`, `dto`, `exception`.
- **Frontend** (`Frontend/`): React 19 + Vite 8 + React Router 7 + `sonner`. Sin Redux/Context: estado en hooks + props. Carpetas: `api`, `components`, `hooks`, `pages`, `router`, `styles`, `utils`.

## Forma de las respuestas (backend) — no es uniforme
- `POST`/`PUT` de una entidad de primer nivel → `{"mensaje": "...", "data": {...}}`. `GET` de listados → array directo.
- Sub-recursos de pedido (`agregarProductoAPedido`, `obtenerPedidoPorCodigo`, `cambiarEstadoDelPedido`) → DTO directo. `eliminarProductoDePedido` → `DELETE /api/pedidos/{pedidoId}/eliminar-producto/{productoId}?cantidad=N`: devuelve el DTO de la línea, o `204` si el pedido se queda sin líneas y se elimina.
- Errores: `ResourceNotFoundException`→404, `BadRequestException`→400, `PedidoStateException`→409, todos `{"Error: ": "..."}` (con el espacio en la clave); igual los de petición de Spring (parámetro que falta, tipo incorrecto, JSON ilegible → 400; ruta inexistente → 404). `@Valid` fallido → `{"Error": "...", "detalles": [...]}`. Catch-all → `{"Error inesperado: ": "..."}`. Copia los mensajes de excepción letra por letra si testeas contra ellos.

## Frontend
- Un hook por necesidad de datos → `{ datos, loading, error }` (+ `lastUpdated`/`refetch` si hay polling). Páginas: `if (loading) return <Loading />`, `{error && <ErrorMessage message={error} />}`.
- Usa `ESTADOS_PEDIDO` (`utils/constants.js`), nunca strings sueltos.
- CSS con BEM, un archivo por página. Tokens unificados en `styles/tokens.css` (claro/oscuro por `prefers-color-scheme`).
- Iconos: `lucide-react` (tree-shakeable). Nunca emoji como iconos.
- Dark mode: solo vía `prefers-color-scheme`, sin toggle manual.

## Entorno
- Backend: variables de entorno reales (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `FRONTEND_URL`, opcional: por defecto `http://localhost:5173`) en la Run Config del IDE. **No crear `Backend/.env`**: Spring no lo lee y su `.gitignore` no lo excluye.
- Frontend: `Frontend/.env` con `VITE_API_URL` (no hardcodeado).

## Lógica de negocio — fácil equivocarse
- Estados: `CREADO → PREPARACION → LISTO → PAGADO → ENTREGADO`. Sin saltos; desde `ENTREGADO` no hay transición válida.
- Solo se pueden añadir o quitar productos de un pedido en `CREADO`. Si se quita la última línea, el backend borra el pedido (`204`).
- `GROUP BY` en `@Query` debe incluir todas las columnas no agregadas del `SELECT` (PostgreSQL exige agrupación completa). `SUM()`/`COUNT()` en JPQL devuelven `Long`.

## Comandos y tests
- Backend: `mvn -f Backend/pom.xml test` (usa Testcontainers con PostgreSQL). Arrancar: `mvn spring-boot:run` desde `Backend/` — ver Límites.
- Frontend: `npm --prefix Frontend run dev`; verificar con `npm --prefix Frontend run build` y `npm --prefix Frontend run lint`.
- Spring Boot 4.1 reubicó las anotaciones de test (los imports de Spring Boot 3 no existen aquí): `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`; `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`; `TestRestTemplate` → `org.springframework.boot.resttestclient` (+ `@AutoConfigureTestRestTemplate`).
- Compara `BigDecimal` con `isEqualByComparingTo(...)`, nunca `isEqualTo(...)`.

## Estadísticas en el frontend

- El frontend consume `GET /api/estadisticas/productos-mas-vendidos` para mostrar "Los más vendidos" en la carta de Terminal (top 5, muestra "No hay productos en la lista" si no hay ventas activas; oculto si falla la llamada).

## Verificación (frontend)
No hay tests automáticos de UI. Tras cada cambio de frontend, verifica con el MCP de Chrome DevTools (abre su propia ventana de Chrome, no la vista embebida de VS Code):
1. Con el backend ya arrancado por el usuario (si falta, PARA y pídelo) y `npm --prefix Frontend run dev` en marcha, navega a `http://localhost:5173` (o el puerto que indique Vite).
2. Ejercita el flujo del cambio solo con pedidos creados para la prueba: no modifiques ni borres datos ajenos.
3. Revisa la consola (cero errores o warnings nuevos) y `Network` (`/api/...` con código y cuerpo esperados).
4. Comprueba la vista móvil (el responsive no está verificado a fondo: anota lo que se rompa).

## Memoria
- Al empezar, lee `MEMORY.md`. Al terminar cada tarea actualízalo: estado actual, decisiones importantes (con su porqué) y errores a evitar.
- Máximo ~50 líneas: resume o elimina lo que ya no aporte. Si algo pasa a ser regla permanente, propón moverlo a `AGENTS.md`. Nunca guardes datos sensibles.

## Límites
- ✅ Siempre: leer `docs/constitution.md` y la spec activa (`specs/NNN-*/`) antes de tocar código; seguir las convenciones de nombres y de forma de respuesta; hacer solo lo que se pide, con cambios pequeños y enfocados.
- ⚠️ Pregunta antes: arrancar el backend; cambiar un DTO que ya consume el frontend; añadir una dependencia; consultar el esquema de la entidad.
- 🚫 Nunca: crear o commitear `Backend/.env`; implementar gestión de catálogo (CRUD producto/categoría/terminal, va por backend/Swagger a propósito) en el frontend sin que se pida.