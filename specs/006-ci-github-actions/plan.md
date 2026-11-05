# Plan 006 — CI con GitHub Actions: verificar cada cambio y publicar imágenes

Spec: `specs/006-ci-github-actions/spec.md` (Estado: aprobada). Este plan describe el **cómo**; el qué está en la spec. No hay cambios de código de negocio: no se tocan `Backend/src`, `Frontend/src`, `docker-compose.yml` ni la API.

## 1. Enfoque técnico

Un único workflow, `.github/workflows/ci.yml`, con **cuatro jobs** que se ejecutan en paralelo las comprobaciones y, solo en un push verde a main, un cuarto job que publica:

```text
pull_request (→ main, incl. drafts)  ─┐
push (→ main)                        ─┼─▶ [backend] ─┐
workflow_dispatch                    ─┤   [frontend] ─┼─▶ [publish]  (solo push a main y todos en verde)
                                     └   [docker]   ─┘
```

- **`backend`**: checkout → `actions/setup-java` (Temurin 17, caché Maven) → `mvn -B -f Backend/pom.xml test` (compila + toda la suite, Testcontainers incluidos, sin variables `DB_*`).
- **`frontend`**: checkout → `actions/setup-node` (Node 22, caché npm sobre `Frontend/package-lock.json`) → pasos separados `npm ci`, `npm run lint`, `npm run build` (directorio `Frontend`). Pasos separados = hueco para añadir `npm run test` después sin reestructurar (RF-11).
- **`docker`**: valida el Compose con `DB_PASSWORD=ci-dummy`, comprueba que el Compose **falla** con contraseña vacía, construye las dos imágenes con buildx (caché de capas GHA con ámbito propio por imagen — `scope=backend` y `scope=frontend` —, `push: false`) y, **solo en push a main**, las guarda en tar y las sube como artefacto.
- **`publish`**: `needs: [backend, frontend, docker]` + `if` de push a main → descarga el artefacto, hace login en GHCR con `GITHUB_TOKEN`, publica siempre la etiqueta del commit y `latest` solo si ese commit sigue siendo la punta de main.

**Por un solo workflow** (alternativa descartada: dos workflows ligados con `workflow_run`): la spec exige que la publicación ocurra "tras pasar todas las comprobaciones **de esa misma ejecución**" (RF-16) y con "las mismas imágenes que se construyeron en esa ejecución, sin reconstruirse" (RF-19); workflows separados añaden carreras y rompen esa atomicidad.

**Por pasar las imágenes como artefacto** (alternativas descartadas: reconstruirlas en `publish` con caché, viola RF-19 literalmente; un job único que comprueba y publica, pierde paralelismo y mezcla permisos de lectura con de escritura): así `docker` es una comprobación pura (en PRs no sube nada) y `publish` es el **único** job con `packages: write`, que es la decisión vinculante del usuario.

## 2. Decisiones sobre las dudas abiertas de la spec

### 2.1 Cómo se comprueba que un commit sigue siendo el último de main (RF-21)
**Decisión**: dentro de `publish`, después de subir la etiqueta del commit, obtener la punta actual de main con
`git ls-remote "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" refs/heads/main`,
con el token pasado al paso como variable de entorno (`env: GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}`), no interpolado dentro del script, y compararla con `${{ github.sha }}`. Si coinciden → se etiqueta y sube `latest` en las dos imágenes; si no → solo la etiqueta del commit, con un mensaje en el log explicando por qué.
**Por qué**: la comparación se hace en el instante de publicar (el que importa), sin checkout ni dependencias extra, usando solo el token que la ejecución ya tiene (RF-23). En una re-ejecución, `${{ github.sha }}` conserva el commit original mientras `ls-remote` devuelve la punta actual → `latest` no retrocede automáticamente (RF-21) y la etiqueta del commit se republica siempre (RF-20).
**Descartadas**: la API de GitHub `GET /repos/.../commits/main` (más pasos, mismo resultado); usar `github.event.before/after` (solo describen ese push, no la punta actual).

### 2.2 Cómo se vincula cada paquete al repositorio (RF-17)
**Decisión**: publicar con el `GITHUB_TOKEN` de la propia ejecución y `permissions: packages: write` solo en `publish`. GitHub asocia automáticamente al repositorio los paquetes publicados desde un workflow de ese repositorio. Nombres: `ghcr.io/<dueño>/<repo>-backend` y `ghcr.io/<dueño>/<repo>-frontend` (todo en minúsculas, ver 2.4).
**Por qué**: cumple RF-23 (ningún secreto propio) y es el mecanismo soportado. El usuario verificará la vinculación en *Settings* del paquete tras la primera publicación (paso de verificación; si no apareciera, se vincula a mano desde la interfaz de GitHub — sin tokens de administración).
**Descartadas**: PAT o secret con permisos de paquete (rompe RF-23); vincular vía API (requiere permisos de administración, fuera de alcance).

### 2.3 ¿Comprobar además que el Compose falla con contraseña vacía? (opcional)
**Decisión**: **sí**, como paso del job `docker`: `DB_PASSWORD="" docker compose config` debe terminar con código distinto de cero (si termina en 0, el paso falla y marca la ejecución).
**Por qué**: protege el invariante `${DB_PASSWORD:?...}` que la spec 005 estableció (README, `docker-compose.yml`) contra regresiones silenciosas — p. ej. alguien cambia `:?` por `:-` y el sistema dejaría de exigir contraseña —; cuesta ~1 segundo, no levanta contenedores ni necesita red.
**Descartada**: omitirla para ceñirse al pedido literal — el ahorro es irrelevante a cambio de perder una red de seguridad barata.

### 2.4 Nombre de imagen en minúsculas
**Decisión**: primer paso de `publish`:
`echo "IMAGE_PREFIX=ghcr.io/$(printf '%s' "$GITHUB_REPOSITORY" | tr '[:upper:]' '[:lower:]')" >> "$GITHUB_ENV"`;
todos los tags usan `$IMAGE_PREFIX-backend` / `$IMAGE_PREFIX-frontend`.
**Por qué**: GHCR rechaza nombres con mayúsculas y el repositorio (`APIGestionDePedidos...`) las lleva; `tr` es explícito y funciona en cualquier shell; calcularlo desde `GITHUB_REPOSITORY` funciona también en forks sin tocar el fichero.
**Descartadas**: `${GITHUB_REPOSITORY,,}` (limitado a bash, menos legible); hardcodear el nombre (rompe si se renombra el repo o se hace fork con otro nombre).

### 2.5 Permisos mínimos del token
**Decisión**: a nivel de workflow, `permissions: contents: read`; a nivel de job, `permissions: { contents: read, packages: write }` **solo** en `publish`. Todos los demás jobs heredan la lectura de código.
**Por qué**: mínimo privilegio explícito (RF-23): las comprobaciones solo leen el código; solo quien publica escribe paquetes. La declaración explícita evita que un cambio futuro en la configuración de permisos por defecto del repositorio amplíe lo que el flujo usa.
**Descartadas**: no declarar permisos (depende del default del repo, que puede ser de escritura); dar `packages: write` a todos los jobs (permiso de más en las comprobaciones de PR).

## 3. Ficheros que creará/el implementador tocará

| Fichero | Acción | Responsabilidad | RF |
|---|---|---|---|
| `.github/workflows/ci.yml` | **crear** | Único workflow: triggers, permisos, concurrencia y los 4 jobs (contenido en §4). | RF-1 a RF-26 |
| `README.md` | **editar** (mínimo) | Insignia del estado + sección "Integración continua": qué verifica el flujo, publicación solo en main, pasos manuales (activar Actions en el fork; hacer públicos los dos paquetes tras la primera publicación), el aviso de **no hacer `docker push` manual a ghcr.io antes de la primera publicación del CI** (una publicación manual crearía los paquetes fuera del flujo, sin pasar las comprobaciones, y condiciona la primera ejecución) y la advertencia de RF-14 (imagen del frontend con `http://localhost:8080/api`, solo válida en local con Compose; la demo futura se construirá aparte). | RF-14 (doc), RF-27, RF-28 |
| `MEMORY.md` | **editar** (al final) | Anotar la decisión de CI y los aprendizajes (máx. ~50 líneas). | — |

**No se tocan**: `docker-compose.yml`, `Backend/**`, `Frontend/**`, `.env*`, `AGENTS.md`, `docs/`. El flujo no introduce secretos ni variables de entorno en el repositorio (RF-23).

### Insignia (RF-27)

Arriba del README:
`[![CI](https://github.com/<dueño>/<repo>/actions/workflows/ci.yml/badge.svg)](https://github.com/<dueño>/<repo>/actions/workflows/ci.yml)`
con el slug real obtenido de `git remote get-url origin` (no hardcodear "owner" inventado). **Descartada**: badge genérica sin dueño/repo (no funciona en GitHub).

## 4. Contenido del workflow (esqueleto de referencia)

```yaml
name: CI
on:
  pull_request:
    branches: [main]        # abre/actualiza PR → main; no se filtran types → drafts incluidos (RF-1)
  push:
    branches: [main]        # solo main: pushes a ramas sin PR no ejecutan nada (RF-2, RF-4)
  workflow_dispatch:        # manual: todas las comprobaciones, sin publicar (RF-3)
permissions:
  contents: read            # mínimo a nivel global (RF-23, duda 2.5)
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}   # solo PRs: main no se cancela porque puede estar publicando (RF-25)
jobs:
  backend:                  # RF-5, RF-6, RF-7, RF-24
    runs-on: ubuntu-latest
    timeout-minutes: 20     # tope por job para que nada se cuelgue
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "17", cache: maven }
      - run: mvn -B -f Backend/pom.xml test      # sin variables DB_* (RF-6)
  frontend:                 # RF-8, RF-9, RF-10, RF-11, RF-24
    runs-on: ubuntu-latest
    timeout-minutes: 20     # tope por job para que nada se cuelgue
    defaults: { run: { working-directory: Frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: "22", cache: npm, cache-dependency-path: Frontend/package-lock.json }
      - run: npm ci         # reproducible desde package-lock.json
      - run: npm run lint   # sin --max-warnings: los avisos no bloquean (RF-9)
      - run: npm run build
      # RF-11: aquí se añadirá después "- run: npm run test" sin reestructurar
  docker:                   # RF-12, RF-13, RF-14, RF-24
    runs-on: ubuntu-latest
    timeout-minutes: 20     # tope por job para que nada se cuelgue
    steps:
      - uses: actions/checkout@v4
      - name: Validar Compose con valor ficticio
        run: DB_PASSWORD=ci-dummy docker compose config --quiet
      - name: El Compose debe fallar sin contraseña
        run: |
          if DB_PASSWORD="" docker compose config >/dev/null 2>&1; then
            echo "::error::docker compose config no falla con DB_PASSWORD vacía"; exit 1
          fi
      - uses: docker/setup-buildx-action@v3
      - uses: docker/build-push-action@v6        # push: false (en PRs no se publica nada)
        with:
          context: ./Backend
          push: false
          load: true
          tags: img/backend:ci
          labels: org.opencontainers.image.source=${{ github.server_url }}/${{ github.repository }}
          cache-from: type=gha,scope=backend
          cache-to: type=gha,mode=max,ignore-error=true,scope=backend   # mode=max: necesario para que la capa de dependencias del backend (spec 005, go-offline) llegue a la caché
      - uses: docker/build-push-action@v6
        with:
          context: ./Frontend
          push: false
          load: true
          tags: img/frontend:ci
          build-args: VITE_API_URL=http://localhost:8080/api   # RF-14: igual que el default del Compose
          labels: org.opencontainers.image.source=${{ github.server_url }}/${{ github.repository }}
          cache-from: type=gha,scope=frontend
          cache-to: type=gha,mode=max,ignore-error=true,scope=frontend
      - name: Guardar imágenes (solo push a main)
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        run: docker save img/backend:ci -o backend.tar && docker save img/frontend:ci -o frontend.tar
      - uses: actions/upload-artifact@v4         # RF-19: no se sube en PRs (ahorra tiempo)
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        with: { name: "images-${{ github.sha }}", path: "*.tar", overwrite: true }
  publish:                  # RF-16 a RF-23
    needs: [backend, frontend, docker]           # si alguno falla, se salta → RF-15
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'   # nunca en PR ni dispatch → RF-3, RF-16
    runs-on: ubuntu-latest
    timeout-minutes: 20     # tope por job para que nada se cuelgue
    permissions:
      contents: read
      packages: write       # solo aquí (duda 2.5)
    steps:
      - uses: actions/download-artifact@v4
        with: { name: "images-${{ github.sha }}" }
      - run: docker load -i backend.tar && docker load -i frontend.tar
      - name: Prefijo en minúsculas (duda 2.4)
        run: echo "IMAGE_PREFIX=ghcr.io/$(printf '%s' "$GITHUB_REPOSITORY" | tr '[:upper:]' '[:lower:]')" >> "$GITHUB_ENV"
      - uses: docker/login-action@v3
        with: { registry: ghcr.io, username: "${{ github.actor }}", password: "${{ secrets.GITHUB_TOKEN }}" }
      - name: Etiquetar y publicar (latest condicional, duda 2.1)
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}   # vía env del paso, no interpolado en el script
        run: |
          docker tag img/backend:ci  "$IMAGE_PREFIX-backend:$GITHUB_SHA"
          docker tag img/frontend:ci "$IMAGE_PREFIX-frontend:$GITHUB_SHA"
          docker push "$IMAGE_PREFIX-backend:$GITHUB_SHA"     # RF-20: siempre, también en re-ejecución
          docker push "$IMAGE_PREFIX-frontend:$GITHUB_SHA"
          TIP=$(git ls-remote "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" refs/heads/main | cut -f1)
          if [ "$TIP" = "$GITHUB_SHA" ]; then
            docker tag img/backend:ci  "$IMAGE_PREFIX-backend:latest"
            docker tag img/frontend:ci "$IMAGE_PREFIX-frontend:latest"
            docker push "$IMAGE_PREFIX-backend:latest"       # RF-18/RF-21: solo el último commit mueve latest
            docker push "$IMAGE_PREFIX-frontend:latest"
          else
            echo "Este commit ya no es la punta de main: no se mueve latest"
          fi
```

Notas de implementación:
- Las versiones de los actions van por tag mayor (`@v4`, `@v6`, `@v3`): legibilidad para un proyecto didáctico; el pin exacto por SHA se descarta (menos legible, sin requisito en la spec).
- `npm run lint` **sin** `--max-warnings 0`: `eslint .` falla solo con errores (RF-9).
- El job `publish` sin `if: always()` solo corre si los tres `needs` terminaron en verde (RF-15).
- La construcción con buildx replica los contextos y el `VITE_API_URL` del Compose; el paso `docker compose config` garantiza que el propio Compose siga siendo válido (RF-12/RF-13). Si algún día cambian los `build:` del Compose, hay que reflejarlo en el flujo (anotado en riesgos).

## 5. Estrategia de verificación

**Verificable en local (sin GitHub)**:
1. Sintaxis del YAML: validador YAML (`python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"`) o `docker run --rm -v "$PWD":/repo --workdir /repo rhysd/actionlint:latest -color` si hay red/Docker.
2. Comandos de cada job, tal cual: `mvn -B -f Backend/pom.xml test`; `npm --prefix Frontend ci && npm --prefix Frontend run lint && npm --prefix Frontend run build`; `DB_PASSWORD=ci-dummy docker compose config --quiet` (OK) y `DB_PASSWORD="" docker compose config` (debe fallar); `docker build` de los dos contextos con el build-arg.
3. Simulación de la lógica de `latest`: script bash **temporal y fuera del repositorio** (no debe aparecer en `git status`) con `$GITHUB_SHA` y una "punta" falsas que ejercita los tres casos (igual → sube `latest`; distinto → no lo sube; re-ejecución de commit antiguo → no lo sube).
4. Revisión de disparadores, `permissions`, `needs`/`if` y `concurrency` por lectura del fichero: comprobar que `cancel-in-progress` es la expresión `${{ github.event_name == 'pull_request' }}` (solo se cancelan ejecuciones de PR; las de main no, RF-25).

**Solo verificable con una ejecución real (paso del usuario, primera ejecución)**:
- Activar GitHub Actions en el fork; abrir un PR (incl. draft) → los tres checks aparecen y pasan; un push a rama sin PR → no genera ejecución.
- Push verde a main → los dos paquetes aparecen en GHCR con la etiqueta del commit y `latest`.
- Re-ejecutar en Actions un commit antiguo de main → republica su etiqueta y `latest` no retrocede; re-ejecutar tras un fallo parcial completa lo que faltaba (RF-22).
- Dos ejecuciones seguidas del mismo commit → la caché evita descargar dependencias desde cero; en un pull request, un push nuevo cancela la ejecución anterior de esa rama (RF-24, RF-25); en main no se cancela nada (RF-25).
- Insignia en verde en el README; sin secretos configurados en el repositorio.
- Tras la primera publicación: el usuario hace públicos a mano los dos paquetes (paso manual de la spec).

## 6. Riesgos y cómo se mitigan

| Riesgo | Mitigación |
|---|---|
| Fork con Actions desactivado → nada se ejecuta | Paso manual del usuario (ya en la spec); se recuerda en README. |
| Artefactos de imagen (~100-300 MB) solo en push a main → la ejecución de main tarda más | No se suben artefactos en PRs; solo el job `publish` los consume. |
| Artefacto caducado (retención por defecto 90 días) al re-ejecutar | Re-ejecutar *todos los jobs* (RF-22 lo cubre); anotado en README/MEMORY si hiciera falta. |
| Dos ejecuciones de main solapadas (RF-25 ya no cancela en main) | Aceptado: con RF-25 las ejecuciones de main no se cancelan entre sí, así que ninguna se corta a mitad de `publish`; `latest` sigue protegido por la lógica de 2.1 (en el instante de publicar, solo la punta actual lo mueve y la ejecución antigua solo republica su etiqueta). |
| Caché GHA de buildx no escribible en PRs de forks → fallo del paso | `cache-to: ... ignore-error=true` (con `scope=backend` y `scope=frontend` separados, para que las dos imágenes no se pisen la caché) para que un fallo de caché no marque la ejecución. |
| `docker push` manual a ghcr.io antes de la primera publicación del CI → los paquetes se crean fuera del flujo, sin pasar las comprobaciones, y se condiciona la primera ejecución real | Aviso explícito en README (fila de §3) exigido en el "Hecho cuando" de T6: no hacer `docker push` manual a ghcr.io hasta que el CI publique por primera vez. |
| GHCR rechaza el push por mayúsculas en el nombre | Prefijo calculado con `tr` (2.4). |
| `VITE_API_URL` del flujo = default del Compose (`8080`); si cambia el default, divergen | Anotado en README/MEMORY; el paso `compose config` y la revisión del PR lo detectan (RF-14). |
| Comandos del CI distintos de los documentados en AGENTS.md | Se usan literalmente los de AGENTS.md/README (`mvn -B -f Backend/pom.xml test`, `npm ci/lint/build`). |
| Paquetes privados hasta que el usuario los haga públicos | No bloquea publicar (la spec lo exige); se documenta como paso manual. |

## 7. Cobertura de RF (resumen)

- Disparadores y concurrencia: RF-1 a RF-4, RF-25 (cancelación de la ejecución antigua solo en pull requests; main nunca se cancela) → triggers y `concurrency` (§4).
- Backend: RF-5, RF-6, RF-7 → job `backend`. Frontend: RF-8 a RF-11 → job `frontend`.
- Docker: RF-12, RF-13, RF-14 → job `docker` (+ README para la parte documental de RF-14).
- Fallo/publicación: RF-15 a RF-19 → `needs`/`if` + `publish`. Re-ejecuciones: RF-20 a RF-22 → etiqueta siempre + `latest` condicional.
- Credenciales: RF-23 → `permissions` explícitos y `GITHUB_TOKEN`. Rapidez: RF-24, RF-26 → cachés (Maven, npm, capas GHA) y builds desde código fijado.
- Documentación: RF-27, RF-28 → README.
