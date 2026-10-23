# MEMORY.md — Gestión de Pedidos (Backend + Frontend)

Memoria del proyecto entre sesiones (máximo ~50 líneas): resume o elimina lo que ya no aporte.

## Estado actual

- Backend completo: CRUD Categoría/Producto/Terminal, ciclo de vida de Pedido, Estadísticas (productos más vendidos, ranking terminales).
- Tests completos: Service (Mockito), Controller (`@WebMvcTest`), Repository (`@DataJpaTest` sobre las `@Query` de agregación), integración (`TestRestTemplate`). Tests nuevos contra PostgreSQL con Testcontainers verifican búsqueda por código en minúsculas, duplicados rechazados en categoría/terminal/producto, suite en verde con BD real y altas nuevas sin colisión de IDs tras catálogo sembrado. Total: 121 tests (54 controller + 55 service + 6 repository + 3 integración + 3 capa superior).
- Frontend: flujo Terminal/Cocina/Recogida funcionando. `buscarPedidoPorCodigo` implementado en RecogidaPage. Modificación de pedidos en Cocina implementada: `PedidoLineaInput` permite +/− unidades en pedidos CREADO. "Los más vendidos" en la carta de Terminal implementado (top 5, oculto si no hay ventas). Tests: 108 en total (54 controller + 50 service + 3 repository + 1 integración).

## Decisiones (y por qué)

- Catálogo (CRUD producto/categoría/terminal) vive en backend/Swagger, no en frontend — separación backoffice vs. operativo.
- DTOs siempre clase Lombok, nunca `record` ni MapStruct — coherencia con el resto del proyecto.
- `listarProductos` sigue en memoria (streams), no `Specification` — rehacer tests ya verificados no compensaba; mejora opcional.
- Paquete simplificado a `gestionpedidos` (sin `com.empresa`) — proyecto de portfolio, no librería a publicar.
- Rediseño visual completado: tokens unificados (`tokens.css`), dark mode por SO, iconos Lucide, responsive en las 3 páginas.
- Duplicados de catálogo insensibles a mayúsculas: los repositorios usan `existsByNombreIgnoreCase` para rechazar nombres que solo difieran en mayúsculas/minúsculas, mientras el texto se guarda y muestra tal como se escribió. Tests actualizados con casos "Bebidas"/"bebidas" y "Hamburguesa clásica"/"hamburguesa clásica".

## Aprendizajes y errores a evitar

- Spring Boot 4.1 reubicó `@WebMvcTest`/`@DataJpaTest`/`TestRestTemplate` a paquetes nuevos.
- `GROUP BY` debe incluir todas las columnas no agregadas del `SELECT` — PostgreSQL exige agrupación completa.
- `BigDecimal` en tests: `isEqualByComparingTo`, nunca `isEqualTo`.
- Mocks de `@RequestBody`: `any(Dto.class)`, nunca el objeto literal.
- Pedido vacío se elimina automáticamente al remover la última línea (lógica en `PedidoService`).
- No crear `Backend/.env` — Spring no lo lee.
- La ruta del proyecto es `/Users/daninorbert/Desktop/Programación/Proyectos de portfolio/APIGestionDePedidosProyecto2HackABossMejorado`. Ninguna otra.
- No sobreescribas archivos enteros cuando no sea necesario. Detecta las líneas que hay que cambiar, sustituir, añadir o suprimir y actúa.

## Próximos pasos

- Frontend: actualizar README (errores HTTP ya resueltos, accesibilidad, polling, catálogo como decisión de diseño).
- Opcional: migrar `listarProductos` a `Specification`.

## Decisiones tomadas en spec 004

Motor único PostgreSQL, Versionado Flyway (V1/V2), Chequeo del motor al arranque, Insensibilidad a mayúsculas, Testcontainers para tests, Documentación actualizada