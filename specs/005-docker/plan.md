# Plan 005 — Docker: levantar el sistema completo con un comando

Spec: `specs/005-docker/spec.md` (Estado: aprobada). Este plan describe el **cómo**; el qué está en la spec. Depende de la spec 004 (PostgreSQL y Flyway), que ya dejó hecho `application.properties`, el validador del motor y `spring.factories`.

> **Cambio RF-22 (aprobado)**: la mejora de velocidad de construcción del backend se describe en la sección final **«Cambio RF-22»**. Las secciones 1 a 7 siguientes describen la implementación original, ya hecha y sigue vigente tal cual; no se borran.

## 1. Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `.gitignore` (raíz) | Ignora `.env`. **No** ignora `.env.example`. | RF-8, RF-9 |
| `.env.example` | Configuración de ejemplo versionada (contenido en 4.4). | RF-8, RF-10, RF-11 |
| `Backend/Dockerfile` | Compila con Maven y ejecuta con JRE 17, sin root (4.1). | RF-1 |
| `Backend/.dockerignore` | Deja fuera `target/`, archivos del IDE y `.env` del contexto de construcción. | RF-1 |
| `Frontend/Dockerfile` | Construye con Node 22 y sirve con nginx; recibe `VITE_API_URL` al construir (4.2). | RF-1, RF-15 |
| `Frontend/nginx.conf` | Devuelve la aplicación al abrir o recargar cualquier ruta (4.2). | RF-6 |
| `Frontend/.dockerignore` | Deja fuera `node_modules/`, `dist/` y `.env` del contexto. | RF-1 |
| `docker-compose.yml` | Base de datos, backend y frontend; contraseña obligatoria; comprobación de salud; puertos; modos (4.3). | RF-1 a RF-5, RF-7, RF-12 a RF-15, RF-17, RF-19 |
| `README.md` (raíz), `AGENTS.md`, `MEMORY.md` | Documentación de los dos comandos, los puertos y las advertencias de los casos límite. | RF-16, RF-20, RF-21 |

**No cambian**: `application.properties`, `MotorPostgresqlValidator`, `spring.factories`, ni ningún archivo de `src/` del backend o del frontend.

## 2. Cambios por capa

- **Backend**: solo un `Dockerfile` y su `.dockerignore`. Sin código nuevo.
- **Frontend**: un `Dockerfile`, un `nginx.conf` y su `.dockerignore`. Sin cambios en `src/`. `Frontend/.env` queda solo para el modo local con `npm run dev`; en Docker, `VITE_API_URL` se pasa al construir y el `.dockerignore` impide copiar ese archivo.
- **Infraestructura**: `docker-compose.yml`, `.env.example` y `.gitignore`.

## 3. Contrato de la API

No cambia. Rutas, verbos, códigos HTTP y formas de respuesta son los de hoy. El navegador llama al backend directamente, en `VITE_API_URL`, y el backend acepta ese origen por `FRONTEND_URL` (CORS ya existente). No se usa proxy inverso (fuera de alcance de la spec).

## 4. Archivos de referencia

### 4.1 `Backend/Dockerfile` (el contexto de construcción es `./Backend`)

```dockerfile
FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`Backend/.dockerignore`: `target/`, `.idea/`, `.vscode/`, `*.iml`, `postman/`, `.env`, `.git`.

### 4.2 Frontend (el contexto de construcción es `./Frontend`)

`Frontend/Dockerfile`:

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Vite incrusta VITE_API_URL al construir: tiene que llegar como argumento
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

`Frontend/nginx.conf`:

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index index.html;
  # React Router: lo que no sea un archivo devuelve index.html (RF-6)
  location / {
    try_files $uri /index.html;
  }
}
```

`Frontend/.dockerignore`: `node_modules/`, `dist/`, `.env`, `.git`.

### 4.3 `docker-compose.yml` (en la raíz; sin clave `version`)

```yaml
# Uso, desde la raíz y con un .env creado a partir de .env.example:
#   Todo en Docker:  docker compose up --build
#   Solo la base de datos (backend y frontend desde el IDE):  docker compose up --wait db
services:
  db:
    image: postgres:17-alpine   # misma etiqueta que Testcontainers (ver Backend/src/test)
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: "${DB_PASSWORD:?Falta DB_PASSWORD. Copia .env.example a .env y define una contraseña}"
      POSTGRES_DB: ${DB_NAME:-gestion_pedidos}
    volumes:
      - pg_data:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:${DB_PORT:-5432}:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 20

  backend:
    build: ./Backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://db:5432/${DB_NAME:-gestion_pedidos}
      DB_USERNAME: ${DB_USERNAME:-postgres}
      DB_PASSWORD: "${DB_PASSWORD:?Falta DB_PASSWORD. Copia .env.example a .env y define una contraseña}"
      FRONTEND_URL: http://localhost:${FRONTEND_PORT:-8081}
    ports:
      - "${BACKEND_PORT:-8080}:8080"

  frontend:
    build:
      context: ./Frontend
      args:
        VITE_API_URL: http://localhost:${BACKEND_PORT:-8080}/api
    restart: unless-stopped
    ports:
      - "${FRONTEND_PORT:-8081}:80"

volumes:
  pg_data:
```

Detalles que importan:
- El mensaje de `:?` va entre comillas porque lleva un punto y espacios, y en YAML un texto sin comillas con `: ` se interpreta mal.
- `$$` en el healthcheck evita que Compose sustituya la variable antes de tiempo: la lee el shell del contenedor.
- Con `:?`, **cualquier** comando de Compose exige `DB_PASSWORD`, incluido `docker compose down -v`.
- Cambiar `BACKEND_PORT` obliga a reconstruir el frontend (`--build`), porque la dirección se incrusta al construir (RF-15).

### 4.4 `.env.example`

```
# Configuración para Docker Compose.
# 1) Copia este archivo como .env  2) Define DB_PASSWORD  3) Ejecuta: docker compose up --build
# El .env real no se sube a git. Usa una contraseña sencilla, sin comillas ni símbolos especiales.

# OBLIGATORIA: contraseña de la base de datos. Vacía aquí a propósito.
DB_PASSWORD=

# Opcionales (el valor que aparece es el valor por defecto)
# Usuario de la base de datos
DB_USERNAME=postgres
# Nombre de la base de datos
DB_NAME=gestion_pedidos
# Puerto de la base de datos (solo accesible desde tu máquina)
DB_PORT=5432
# Puerto del backend
BACKEND_PORT=8080
# Puerto del frontend
FRONTEND_PORT=8081

# Si ejecutas el backend desde el IDE con "docker compose up --wait db",
# el IDE debe usar el mismo DB_USERNAME y DB_PASSWORD
# (DB_URL=jdbc:postgresql://localhost:<DB_PORT>/<DB_NAME>).
```

## 5. Decisiones justificadas

| Decisión | Justificación | Alternativa descartada |
|---|---|---|
| Un único archivo de Compose y dos comandos | Cubre los dos modos sin duplicar configuración. | Perfiles de Compose: añaden una opción al comando principal. |
| Contraseña obligatoria con `${DB_PASSWORD:?...}` | Falla antes de crear nada, sin código. | Comprobarla en Java: el backend ya habría arrancado, y no detecta el modo "solo base de datos". |
| Comprobación de salud de PostgreSQL y `depends_on: service_healthy`; `up --wait db` | El backend arranca con la base lista, y el segundo modo no devuelve el control hasta entonces. | `sleep` o reintentos manuales; `up -d` a secas, que no espera. |
| nginx con `try_files` y sin proxy inverso | Las rutas de React Router funcionan y los estáticos se sirven tal cual. | Un `rewrite` global (convierte todos los archivos en `index.html`) y `proxy_pass` a `/api` (fuera de alcance). |
| `VITE_API_URL` como argumento de construcción | Vite la lee al construir, no al ejecutar. | Variable de entorno del contenedor: no tendría ningún efecto. |
| Puerto de la base de datos en `127.0.0.1` | La base de datos no queda expuesta a la red. | `5432:5432`: escucha en todas las interfaces. |
| Misma etiqueta `postgres:17-alpine` en Docker y Testcontainers, con comprobación manual | Barato y suficiente: comentario cruzado más un `grep`. | Un test automático que compare las dos etiquetas: desproporcionado. |

## 6. Estrategia de verificación

No hay tests Java nuevos: la spec describe comportamiento del entorno, y se comprueba con comandos.

| Criterio de la spec | Comprobación |
|---|---|
| Un solo comando levanta todo (RF-1, RF-11) | `docker compose up --build` en una copia con `.env`; los tres servicios en marcha |
| Datos de ejemplo al primer arranque (RF-3) | `http://localhost:8081` muestra las 3 terminales; `curl http://localhost:8080/api/terminales` |
| Contraseña ausente o vacía (RF-7) | `DB_PASSWORD= docker compose up`: falla antes de crear contenedores; `docker compose ps -a` vacío |
| Recargar una ruta (RF-6) | Abrir `http://localhost:8081/cocina` directamente: muestra Cocina |
| Segundo arranque (RF-17) | `docker compose down` y `up`: siguen 3 terminales, sin duplicados |
| Borrar datos (RF-18) | `docker compose down -v` y `up`: vuelven los datos de ejemplo |
| Modo "solo base de datos" (RF-2, RF-5, RF-13) | `docker compose up --wait db` no devuelve el control hasta que está sana; el backend del IDE conecta a `localhost` |
| Puertos (RF-14, RF-16) | Cambiar un puerto en `.env`; con el backend del IDE en el 8080, `docker compose up` falla por el puerto |
| Configuración fuera de git (RF-9) | `git check-ignore -v .env` muestra la regla; `git check-ignore .env.example` no devuelve nada |
| Misma etiqueta (RF-19) | `grep -rn "postgres:17" docker-compose.yml Backend/src/test` |
| Pruebas del backend | `mvn -f Backend/pom.xml test -Dspring.profiles.active=test` en verde, con Docker en marcha |

## 7. Riesgos y notas

- El primer arranque construye imágenes y descarga dependencias: es lento y necesita internet.
- Contraseñas con caracteres especiales: no se garantizan; se recomienda una sencilla.
- La contraseña de una base ya creada no cambia al editar `.env`: el backend fallará al autenticarse. Hay que borrar los datos (`docker compose down -v`) o cambiarla dentro de la base.
- Cambiar la versión mayor de PostgreSQL con datos existentes impide que la base arranque: hay que borrar el volumen.
- El backend del IDE y el de Docker comparten el puerto 8080: no pueden estar activos a la vez.
- Se verifica en macOS con Docker Desktop. En el equipo, los comandos `docker` piden confirmación a los agentes: las verificaciones de las tareas T5 a T7 las ejecuta el usuario.

---

## Cambio RF-22 — Reconstruir el backend sin re-descargar dependencias (sección nueva)

Spec: `specs/005-docker/spec.md` (Estado: aprobada, con RF-22). Solo se describe el cambio; lo anterior sigue vigente.

**Invariante**: RF-1 a RF-21 y el comportamiento observable de `docker compose up` no cambian. El único archivo de código que se toca es `Backend/Dockerfile` (y `MEMORY.md` para documentar); `docker-compose.yml`, `Backend/.dockerignore`, `Frontend/**` y `Backend/src/**` quedan intactos.

### C1. Enfoque técnico elegido: capa `dependency:go-offline`

Separar la descarga de dependencias de la compilación en su propia capa dentro de la etapa de build de `Backend/Dockerfile`, de modo que solo dependa del `pom.xml`:

```dockerfile
FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Capa de dependencias: solo depende del pom → cachada si no cambia el pom
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package
```

Al cambiar solo el código (`src/`), las capas `COPY pom.xml` y `dependency:go-offline` siguen de caché, el `.m2` de la imagen ya contiene las dependencias y `package` no vuelve a descargar nada → **RF-22**. La etapa de ejecución (JRE, usuario sin privilegios, `EXPOSE`, `ENTRYPOINT`) no se toca.

### C2. Por qué esta y no la caché montada (trade-off con el CI de la spec 006)

| Mecanismo | Local | En el CI de la spec 006 (job `docker` según su plan, aún sin implementar: `cache-from/to: type=gha,mode=max`) | Veredicto |
|---|---|---|---|
| **Capa `dependency:go-offline` (elegida)** | Sin re-descarga al cambiar solo el código | La caché de capas **sí se exporta** a la caché remota GHA (`mode=max` incluye capas intermedias): en el runner se restaura la capa y tampoco se descarga | ✅ Elegida |
| Caché montada `RUN --mount=type=cache` | Rápida y sobrevive incluso a `--no-cache` y a actualizaciones de la imagen base | **No se exporta** a la caché remota (limitación conocida de las cachés montadas de BuildKit): el runner arranca con la montura vacía y en cada ejecución re-descarga todas las dependencias | ❌ Descartada |
| Ambas a la vez | — | **Incompatibles**: la montura sobre `/root/.m2` *sombrea* el contenido de la capa. `go-offline` dejaría sus descargas dentro de la montura (invisible para la capa y para CI) y `package` vería una montura vacía en CI → se perdería justo el beneficio de la capa | ❌ Descartada |

La capa elegida, además, funciona con cualquier constructor (sin sintaxis específica de BuildKit). Descartada también `mvn dependency:resolve`: mismo efecto, pero `go-offline` está diseñado para este caso (baja también las dependencias de los plugins).

**Compensación en CI**: no hace falta tocar `specs/006-ci-github-actions/`: según el plan de la spec 006 (aún sin implementar), su job `docker` usará `cache-from: type=gha` + `cache-to: type=gha,mode=max`, que exportará e importará la capa nueva automáticamente. Acoplamiento a vigilar: si en ese plan se bajara a `mode=min`, solo se exportarían las capas finales y la capa de dependencias dejaría de llegar al CI (anotado en C8).

### C3. Frontend: sin cambios

`Frontend/Dockerfile` ya copia `package.json`/`package-lock.json` antes de `npm ci`, así que la instalación queda cacheada y una reconstrucción por cambio de código no la repite. No hay nada que mejorar ahí; la RF-22 es solo del backend.

### C4. Archivos del cambio

| Archivo | Acción | Responsabilidad | RF |
|---|---|---|---|
| `Backend/Dockerfile` | **editar** (solo la etapa de build) | Capa `dependency:go-offline` entre `COPY pom.xml` y `COPY src` (C1), con comentarios en español. | RF-22 |
| `MEMORY.md` | **editar** (al final) | Decisión del mecanismo (y por qué no la montura), requisito de `mode=max` para el CI de la spec 006 y conclusión sobre el jar en capas (mejora opcional futura). | — |

**No se tocan**: `docker-compose.yml`, `Backend/.dockerignore` (ya cubre lo relevante; descartado en la spec), `Frontend/**`, `Backend/src/**`, `specs/006-ci-github-actions/*`.

### C5. Jar en capas: conclusión de la investigación (duda [PENDIENTE DE PLAN], no requisito)

**Conclusión (ya comprobada)**: Spring Boot 4.0/4.1/4.2 **sí documenta un mecanismo soportado** para extraer las capas del jar: `java -Djarmode=tools -jar app.jar extract --layers` (`layertools` fue su antecesor y se eliminó; `-Djarmode=tools` es el sustituto vigente).

**Decisión**: **no se implanta en este cambio**, por dos motivos:

1. No es necesario para RF-22: la capa `dependency:go-offline` (C1) ya cumple el requisito sin tocar la imagen de ejecución.
2. Alteraría el contenido y el arranque de la imagen de ejecución (jar extraído en capas en vez de `app.jar` copiado tal cual), lo que el invariante RF-1–RF-21 prohíbe: ese comportamiento observable no cambia en este cambio.

Se anota en `MEMORY.md` como **mejora opcional futura** (T10/T14), para una eventual spec aparte que sí pueda tocar la etapa de ejecución.

### C6. Pasos, en orden

1. **T10** — Conclusión sobre el jar en capas (C5): sí existe mecanismo soportado en SB 4.1, no se implanta en este cambio.
2. **T11** — Editar `Backend/Dockerfile` con la capa `go-offline` (C1).
3. **T12** — T12 y T13 las ejecuta el usuario porque son pruebas pesadas y destructivas (construcción en frío, docker compose down -v), no por falta de herramienta.
4. **T13** — Regresión de RF-1 a RF-21 con Docker real (**la ejecuta el usuario**).
5. **T14** — Documentación en `MEMORY.md`.

### C7. Verificación de RF-22 sin romper RF-1–RF-21

**Estático (implementador, sin Docker)**: el Dockerfile tiene `COPY pom.xml` → `RUN mvn -B dependency:go-offline` → `COPY src` → `RUN mvn -B -DskipTests package`; la etapa de ejecución, `.dockerignore`, `docker-compose.yml` y `Frontend/**` sin cambios (diff de git).

**Con Docker real (usuario)**:

1. **Frío**: `docker build --progress=plain -t backend-rf22 Backend` → en la primera ejecución se ven las descargas.
2. **Cambio solo de código**: añadir un comentario a un archivo de `Backend/src/` (o un archivo temporal dentro de `src/`) y reconstruir con `--progress=plain` → el paso `dependency:go-offline` aparece `CACHED` y el paso `package` no muestra líneas de descarga de dependencias (`Downloaded from`) → **RF-22**.
3. **Revertir** el cambio (`git checkout` / borrado) y comprobar `git status` limpio. No hace falta repetir `mvn test`: no se toca la lógica (criterio "pruebas en verde" intacto por construcción).
4. **Regresión (RF-1–RF-21)**: con `.env`, `docker compose up --build` → tres servicios; `http://localhost:8081` muestra las 3 terminales; `/cocina` directo muestra Cocina; sin `DB_PASSWORD` el comando falla antes de crear contenedores; `docker compose up --wait db` espera a la base sana. Es el mismo smoke test de T5–T7, repetido para confirmar que la imagen nueva se comporta igual.

### C8. Riesgos y cómo se mitigan

| Riesgo | Mitigación |
|---|---|
| `dependency:go-offline` no baja algún artefacto puntual → `package` descarga algo suelto | Se revisan los logs de la T12; si el residual es significativo, se para y se propone al usuario un plan B (montura local con el coste de CI explícito) antes de implantarlo. |
| `dependency:go-offline` falla con este pom (resolución rara de un plugin) | El fallo es visible en el build; se ajusta el comando o se propone plan B. |
| Primer build algo más lento por el paso extra | Mismo total de descargas, solo cambia el orden; sin efecto en RF-1. |
| Actualización de la imagen base `maven:3-eclipse-temurin-17` invalida toda la etapa → una re-descarga local | Aceptado (raro y puntual); con `go-offline` igualmente se re-descarga una sola vez. |
| Alguien cambia `mode=max` por `mode=min` en el job `docker` del plan de la spec 006 (aún sin implementar) | La capa intermedia dejaría de exportarse y CI perdería la caché; T14 deja anotado en `MEMORY.md` el requisito de `mode=max` con su comentario junto a la línea del workflow. |
| Docker no está disponible para el agente | Verificación T12/T13 en manos del usuario (como T5–T7). |
| Regresión invisible en runtime | Etapa de ejecución y Compose intactos + smoke test de la T13. |

### C9. Cobertura de RF

- **RF-22** → T11 (Dockerfile) + T12 (verificación con Docker).
- **Invariante RF-1 a RF-21** → T13 (regresión) + diff estático de T11.
- **Duda [PENDIENTE DE PLAN] del jar en capas** → T10 (conclusión: sí existe mecanismo soportado en SB 4.1, no se implanta en este cambio; queda como mejora opcional futura).
- **Documentación** → T14 (`MEMORY.md`).
