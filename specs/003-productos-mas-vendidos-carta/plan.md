# Plan de implementación — Productos más vendidos en la carta

> Documento retrospectivo: la funcionalidad ya está implementada. Este plan describe
> cómo se construyó, archivo por archivo, para dejar constancia de las decisiones tomadas.
> Commits principales: `64b5630` (endpoint de estadísticas en backend) y `d00cdba` (sección en la carta + query/DTO/tests).

## 1. Archivos afectados

| Archivo | Responsabilidad | RF cubierta |
|---|---|---|
| `Backend/.../controller/EstadisticasController.java` | Endpoint `GET /api/estadisticas/productos-mas-vendidos`. Devuelve `List<ProductoMasVendidoDto>` directo. | RF-2 |
| `Backend/.../service/EstadisticasService.java` | `obtenerProductosMasVendidos()`: delega en el repositorio. | RF-2 |
| `Backend/.../repository/ProductoRepository.java` | `@Query` con `SUM(pp.cantidad)`, `GROUP BY` producto y `ORDER BY ... DESC`. | RF-2 |
| `Backend/.../dto/ProductoMasVendidoDto.java` | DTO de agregación: `productoId`, `nombreProducto`, `totalVendido` (`Long`, porque `SUM()` en JPQL devuelve `Long`). | RF-2, RF-6 |
| `Frontend/src/api/estadisticasApi.js` | `obtenerProductosMasVendidos()`: llamada al endpoint vía `fetchJSON`. | RF-2 |
| `Frontend/src/hooks/useProductosMasVendidos.js` | Hook que carga el ranking al entrar en la pantalla y expone `{ datos, loading, error }`. | RF-8, RF-9 |
| `Frontend/src/pages/TerminalPage.jsx` | Sección "Los más vendidos" encima de las categorías, con tarjetas de producto y botón "Añadir". | RF-1, RF-3, RF-4, RF-5, RF-6, RF-7, RF-10 |
| `Frontend/src/styles/TerminalPage.css` | Estilos de `.mas-vendidos` (mismo aspecto que `.categoria-card`). | RF-1 |

## 2. Decisiones técnicas justificadas

| Decisión | Justificación | Alternativa descartada |
|---|---|---|
| **Reutilizar el endpoint de estadísticas existente** | `GET /api/estadisticas/productos-mas-vendidos` ya se creó en `64b5630` junto a `ranking-terminales`; la sección de la carta lo consume tal cual. | Crear un endpoint nuevo específico para la carta (duplicaría la query de agregación). |
| **DTO de agregación con `Long` para `totalVendido`** | En JPQL, `SUM()` sobre un entero devuelve `Long`; el DTO lo declara así para cualquier `@Query` de agregación. | Usar `Integer`/`BigDecimal` (fallaría el mapeo del constructor). |
| **Filtrar desactivados y cortar a 5 en frontend** | El ranking del backend no filtra por `activo`; al cruzar con la lista de productos (`.filter((mv) => mv.producto)`) se descartan los desactivados y se toman los 5 primeros (RF-5). | Filtrar en la query con un `JOIN` a producto y `WHERE activo = true` (la query es de agregación y complica el `GROUP BY`). |
| **Clave por `productoId`, no por nombre (RF-6)** | Dos productos pueden llamarse igual; la `key` de React y la identidad del ranking son por id. | Usar el nombre como key (rompería con duplicados). |
| **Ocultar la sección si falla el ranking (RF-8)** | Si no se puede obtener el ranking, no se muestra la sección y el resto de la carta funciona con normalidad. | Mostrar un error en pantalla (la carta debe seguir usable). |
| **No mostrar la sección mientras carga (RF-9)** | Se condiciona el render a `!loadingMasVendidos`, igual que el resto de la carta. | Mostrar un skeleton (no era necesario para el alcance). |
| **El ranking no se actualiza automáticamente** | Se obtiene al entrar en la pantalla; no hay polling (RF del spec: "no se actualiza automáticamente mientras permanece abierta"). | Añadir polling (no pedido; añadiría carga innecesaria). |

## 3. Cómo se pinta en la interfaz

1. **Sección** (`.mas-vendidos`): un recuadro con el mismo aspecto que `.categoria-card`, situado encima de las categorías dentro de `terminal-layout__carta` (RF-1).
2. **Estados de la sección** (en `TerminalPage`):
   - Cargando → no se muestra (RF-9).
   - Error → no se muestra; el resto de la carta funciona (RF-8).
   - Sin ventas → se muestra el recuadro con "No hay productos en la lista" (RF-7).
   - Con ventas → se muestran hasta 5 tarjetas de producto (RF-2).
3. **Tarjeta de producto** (`.producto-card`): nombre, precio con 2 decimales y €, "Total vendido: N" y botón "Añadir" (RF-3, RF-10).
4. **"Añadir"** llama a `agregarAlCarrito(mv.producto)`, la misma función que usan las categorías (RF-4).
5. **Cruce con productos activos** (`masVendidosVisibles`): se hace `map` + `filter` (descarta desactivados) + `slice(0, 5)` (RF-5, RF-2).

## 4. Estrategia de tests y verificación

- **Backend**: `ProductoRepositoryTest` y `EstadisticasServiceTest` cubren la query de agregación y la delegación (mejorados en `d00cdba`).
- **Frontend**: verificación manual con Chrome DevTools (con ventas, sin ventas, producto desactivado, fallo del ranking) y `npm run lint` + `npm run build` sin errores.
- **Casos límite cubiertos**: sin pedidos (RF-7), producto desactivado sustituido (RF-5), menos de 5 productos, dos productos con el mismo nombre (RF-6), fallo del ranking (RF-8).

## 5. Notas retrospectivas

- El endpoint de estadísticas (`ranking-terminales` y `productos-mas-vendidos`) se creó en `64b5630`; la sección de la carta se consumió en `d00cdba`.
- El ranking suma las unidades de **todos** los pedidos, también los no cobrados (ver duda abierta en el spec).
- La sección no tiene polling: se carga al entrar en la pantalla y no cambia hasta volver a entrar (ver duda abierta en el spec).
