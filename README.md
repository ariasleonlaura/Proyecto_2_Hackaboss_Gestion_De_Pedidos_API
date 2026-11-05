# Gestión de Pedidos API

[![CI](https://github.com/daninorbert14/APIGestionDePedidosProyecto2HackABossMejorado/actions/workflows/ci.yml/badge.svg)](https://github.com/daninorbert14/APIGestionDePedidosProyecto2HackABossMejorado/actions/workflows/ci.yml)

API REST y aplicación web de terminales de autoservicio: Terminal → Cocina → Recogida.

## Demo

Sin demo pública aún; el proyecto está en fase de desarrollo activo. Capturas y vídeos próximos.

## Arquitectura

```text
Frontend (React + Vite)                Backend (Spring Boot)
         │                                  │
         └── HTTP ──────────────────────────┘
                                    │
                              PostgreSQL
```

## Stack, de un vistazo

**Backend**: Java 17, Spring Boot 4.1.0, Spring Web, Spring Data JPA, Hibernate, PostgreSQL, Lombok, Swagger / SpringDoc OpenAPI.

**Frontend**: React 19, Vite 8, React Router 7, Sonner, Lucide (iconos), CSS BEM, tokens unificados.

## Ejecutar con un solo comando

```bash
# Modo "todo en Docker" (levantar base de datos, backend y frontend):
#   1) Copia la configuración de ejemplo:
#      cp .env.example .env
#      # Define DB_PASSWORD en el .env creado
#   2) Arranca el sistema:
#      docker compose up --build

# Modo "solo base de datos" (base de datos en Docker, backend y frontend desde el IDE):
#   docker compose up --wait db

# Tests del backend (necesitan el demonio de Docker en marcha):
#   mvn -f Backend/pom.xml test
```

## Puertos

- Por defecto: **5432** (base de datos, solo 127.0.0.1), **8080** (backend), **8081** (frontend).
- Se pueden cambiar editando el archivo `.env` real en la raíz (nunca `.env.example`).
- **ADVERTENCIA**: El backend del IDE y el de Docker comparten el puerto 8080; no pueden estar activos a la vez (RF-16).

## Configuración

1. **Primera vez**: copia `.env.example` a `.env` y define una contraseña `DB_PASSWORD`.
2. El archivo `.env` real queda fuera de git (ignorado por `.gitignore`); la configuración de ejemplo `.env.example` sí está versionada.
3. Si el backend del IDE ya está en marcha en el puerto 8080, `docker compose up` fallará por conflicto de puerto.
4. Las contraseñas con caracteres especiales no se garantizan; se recomienda una contraseña sencilla.
5. Cambiar la versión mayor de PostgreSQL sobre datos existentes impide que la base arranque; hay que borrar el volumen y empezar de cero (RF-20).
6. Si la contraseña de `.env` es distinta a la de una base de datos ya creada, el backend fallará al autenticarse; hay que borrar los datos (`docker compose down -v`) o cambiarla dentro de la base (RF-21).
7. El primer arranque construye imágenes y descarga dependencias: es lento y necesita internet.
8. `VITE_API_URL` se fija al construir la imagen del frontend; termina en `/api` y se pasa como argumento de construcción (RF-15).
9. `docker compose down -v` también necesita el archivo `.env` para funcionar; no lo elimine después del primer arranque si quiere conservar los datos.
10. Los dos modos comparten los datos de la base de datos: lo guardado con uno se ve desde el otro.

## Documentación y código

- **Swagger UI**: http://localhost:8080/doc/swagger-ui.html (después de arrancar el backend)
- **Colección Postman**: `Backend/postman/` — `API de gestión de pedidos - Hamburguesería.postman_collection.json`
- **Código fuente**: [Backend/](Backend/README.md) (Java 17, Spring Boot) y [Frontend/](Frontend/README.md) (React 19, Vite)

## Integración continua

El repositorio tiene un flujo de GitHub Actions (`.github/workflows/ci.yml`) que se dispara en cada pull request hacia `main` (incluidos los drafts), en cada push a `main` y manualmente con `workflow_dispatch`.

**Qué verifica el flujo** (tres comprobaciones que corren en paralelo):

- **Backend**: compila y pasa todas las pruebas (`mvn -B -f Backend/pom.xml test`, con Testcontainers; no necesita variables `DB_*`).
- **Frontend**: instalación reproducible desde el lockfile (`npm ci`), lint (`npm run lint`) y build (`npm run build`).
- **Docker**: validez del `docker-compose.yml` (incluido que la contraseña siga siendo obligatoria) y construcción de las dos imágenes (backend y frontend), sin publicarlas.

**Publicación de imágenes**: la subida a GHCR ocurre **solo en un push verde a `main`**, cuando los tres trabajos anteriores terminan bien. Se publican las dos imágenes con la etiqueta del commit y con `latest` (este último solo si el commit sigue siendo la punta de `main`). **Nunca** se publica en pull requests ni en una ejecución manual.

**Pasos manuales** (solo la primera vez):

1. Activar GitHub Actions en el fork (pestaña *Actions* → *Enable actions on this repository*), una sola vez.
2. Tras la primera publicación, hacer públicos **a mano los DOS paquetes** de imágenes (backend y frontend) desde la pestaña *Packages* del repositorio; hasta entonces permanecen privados.

> **AVISO — no hacer `docker push` manual a ghcr.io antes de la primera publicación del CI**: una publicación manual crearía los paquetes fuera del flujo, sin pasar las comprobaciones, y condiciona la primera ejecución. Hay que esperar a que el flujo publique por sí mismo.

> **AVISO sobre la imagen del frontend (RF-14)**: la imagen del frontend que construye el CI lleva incrustada `http://localhost:8080/api`, la misma dirección por defecto que usa Docker Compose; por eso solo es válida en local con Docker Compose. Una eventual demo se construirá aparte, con su dirección pública.

## Mejoras futuras (consolidado)

- Filtros en base de datos (push filtering en lugar de Java streams)
- Paginación en listados de productos y pedidos
- Autenticación con Spring Security + JWT (roles terminal/cocina/recogida)
- Gestión de imágenes de producto (S3 o almacenamiento similar)
- Mejoras de accesibilidad
- Diseño responsive avanzado
- Dashboard con estadísticas de ventas
- Tests unitarios con Vitest (frontend)
- Migración del polling de 15 s a WebSockets para actualización en tiempo real
- Mejoras de manejo de errores HTTP diferenciados
- Persistencia de terminal seleccionada mediante localStorage

## Autores

Laura Arias, Daniel Norbert, Rafael Porcel  
Proyecto desarrollado como parte de un bootcamp de programación full stack y posteriormente ampliado por Daniel Norbert.