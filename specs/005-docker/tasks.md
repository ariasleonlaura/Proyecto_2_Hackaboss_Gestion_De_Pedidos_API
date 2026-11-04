# Tareas 005 — Docker: levantar el sistema completo con un comando

Las tareas T5, T6 y T7 son verificaciones que ejecuta el usuario (usan Docker y arrancan el sistema). El implementer prepara los archivos y, al llegar a ellas, se para y las pide.

- [x] **T1. [Infra] `.gitignore` y `.env.example`.** RF-8, RF-9, RF-10, RF-11
  Hecho cuando: `git check-ignore -v .env` muestra una regla del `.gitignore` de la raíz y `git check-ignore .env.example` no devuelve nada; el `.env.example` tiene la cabecera de instrucciones, `DB_PASSWORD` vacía y marcada como obligatoria, usuario, nombre de la base de datos y puertos con valor por defecto, un comentario por variable y la nota sobre el IDE.

- [x] **T2. [Backend] `Backend/Dockerfile` y `Backend/.dockerignore`.** RF-1
  Hecho cuando: `docker build -t backend-test Backend` termina sin errores; `docker run --rm --entrypoint id backend-test` muestra un usuario distinto de root; la imagen no contiene ningún `.env`.

- [x] **T3. [Frontend] `Frontend/Dockerfile`, `Frontend/nginx.conf` y `Frontend/.dockerignore`.** RF-1, RF-6, RF-15
  Hecho cuando: `docker build --build-arg VITE_API_URL=http://localhost:8080/api -t frontend-test Frontend` termina sin errores; con `docker run --rm -p 8081:80 frontend-test`, `curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/cocina` devuelve 200 y el JavaScript se sirve con tipo `application/javascript`.

- [x] **T4. [Infra] `docker-compose.yml`.** RF-1, RF-2, RF-4, RF-5, RF-7, RF-12, RF-13, RF-14, RF-15, RF-17, RF-19
  Hecho cuando: con un `.env` de prueba, `docker compose config` es válido y muestra los tres servicios; con `DB_PASSWORD` vacía, `docker compose config` falla con el mensaje del `:?` y un código de salida distinto de cero; el puerto de la base de datos se publica en `127.0.0.1`; los tres puertos se leen de `.env` con valor por defecto; el backend depende de la base de datos en estado sano; `VITE_API_URL` llega al frontend como argumento de construcción; no hay clave `version`; la cabecera incluye `up --build` y `up --wait db`.

- [x] **T5. [Infra] Verificar el modo "todo en Docker" (la ejecuta el usuario).** RF-1, RF-3, RF-4, RF-6, RF-7, RF-11, RF-17
  Hecho cuando: en una copia con el `.env` creado desde el ejemplo, `docker compose up --build` deja los tres servicios en marcha; `http://localhost:8081` muestra las 3 terminales y se completa un pedido de Terminal a Recogida; abrir `http://localhost:8081/cocina` directamente muestra Cocina; con `DB_PASSWORD` vacía el comando falla antes de crear contenedores; tras `docker compose down` y `up`, siguen 3 terminales sin duplicados.

- [x] **T6. [Infra] Verificar el modo "solo base de datos" (la ejecuta el usuario).** RF-2, RF-5, RF-13
  Hecho cuando: `docker compose up --wait db` no devuelve el control hasta que la base de datos está sana; `docker compose ps` muestra solo la base de datos; el backend ejecutado desde el IDE, con `DB_URL` apuntando a `localhost`, se conecta y ve los datos de ejemplo.

- [x] **T7. [Infra] Verificar datos, puertos y casos límite (la ejecuta el usuario).** RF-9, RF-14, RF-16, RF-18, RF-19, RF-20, RF-21
  Hecho cuando: `docker compose down -v` y `up` devuelven la base con los datos de ejemplo; cambiar un puerto en `.env` hace que el sistema arranque en ese puerto; con el backend del IDE en el 8080, `docker compose up` falla por el puerto; con una contraseña distinta de la de una base ya creada, el backend falla al autenticarse; con `postgres:18-alpine` sobre un volumen existente, la base no arranca (después se restaura la etiqueta); `git status` no muestra `.env`; `grep -rn "postgres:17" docker-compose.yml Backend/src/test` muestra la misma etiqueta en los dos sitios.

- [x] **T8. [Infra] Documentación: README, `AGENTS.md` y `MEMORY.md`.** RF-16, RF-20, RF-21
  Hecho cuando: el README de la raíz explica los dos comandos, los puertos y la copia de `.env.example` a `.env`; incluye las advertencias del puerto ocupado, la contraseña distinta de una base ya creada, el cambio de versión mayor, las contraseñas con caracteres especiales, el primer arranque lento con internet, que los dos modos comparten datos, que `VITE_API_URL` termina en `/api` y se fija al construir, y que `docker compose down -v` también necesita el `.env`; `AGENTS.md` recoge los comandos de Docker y el `.env` de la raíz (distinto de `Backend/.env`); `MEMORY.md` anota la decisión de Docker y la etiqueta `postgres:17-alpine`.

- [x] **T9. [Backend] Pruebas del backend en verde.**
  Hecho cuando: `mvn -f Backend/pom.xml test -Dspring.profiles.active=test`, con Docker en marcha, termina sin fallos, sin cambios en la lógica ni en las aserciones.

---

## Cambio RF-22 — Reconstruir el backend sin re-descargar dependencias (completo: T10–T14)

Detalle en `specs/005-docker/plan.md`, sección «Cambio RF-22». T12 y T13 las ejecuta el usuario porque son pruebas pesadas y destructivas (construcción en frío, docker compose down -v), no por falta de herramienta.

- [x] **T10. [Decisión] Conclusión sobre el jar en capas en Spring Boot 4.1.** Duda [PENDIENTE DE PLAN] de la spec (no es un requisito)
  Hecho cuando: queda escrito en el plan (sección C5) y en `MEMORY.md` que: (a) Spring Boot 4.0/4.1/4.2 **sí documenta un mecanismo soportado** para extraer las capas del jar — `java -Djarmode=tools -jar app.jar extract --layers` (`layertools` fue su antecesor y se eliminó); (b) **no se implanta en este cambio**: no es necesario para RF-22 y alteraría el contenido y el arranque de la imagen de ejecución, lo que el invariante RF-1–RF-21 prohíbe; (c) queda anotada como **mejora opcional futura** (para una eventual spec que sí pueda tocar la etapa de ejecución).
  **Conclusión (verificada en la implementación de T10):**
  - **(a) Existe mecanismo soportado — verificado.** `Backend/pom.xml` usa `spring-boot-starter-parent` **4.1.0** (la versión exacta del proyecto). La referencia oficial de Spring Boot 4.1 (`docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html`, documentación viva de la rama 4.1) documenta `java -Djarmode=tools -jar my-app.jar` con los comandos `extract` y `list-layers`, y un ejemplo de Dockerfile con `RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted`. El antecesor `-Djarmode=layertools` (con `extract` simple) aparece en la documentación archivada de Spring Boot 3.2.x y ya **no aparece** en la referencia 4.x: es el mecanismo retirado.
  - **(b) No se implanta en este cambio.** Motivos: (1) no es necesario para RF-22 — la capa `dependency:go-offline` de la sección C1 del plan cumple el requisito sin tocar la imagen de ejecución; (2) alteraría el contenido y el arranque de la imagen de ejecución (jar extraído en capas en vez de `app.jar` copiado tal cual), lo que el invariante RF-1–RF-21 prohíbe.
  - **(c) Queda como mejora opcional futura.** La conclusión ya está escrita en `plan.md`, sección C5. La anotación en `MEMORY.md` la hace la **T14** (esta tarea no pide editar `MEMORY.md`; solo se marca y concluye aquí).

- [x] **T11. [Backend/Docker] Capa de dependencias en `Backend/Dockerfile`.** RF-22
  Hecho cuando: el Dockerfile tiene `COPY pom.xml .`, después `RUN mvn -B dependency:go-offline` y después `COPY src ./src` + `RUN mvn -B -DskipTests package`, con comentarios en español que explican el porqué; la etapa de ejecución (JRE, usuario sin privilegios, `EXPOSE`, `ENTRYPOINT`) y `Backend/.dockerignore` no cambian; `docker-compose.yml`, `Frontend/**` y `Backend/src/**` intactos (diff de git).

- [x] **T12. [Backend/Docker] Verificar RF-22 con Docker real (la ejecuta el usuario).** RF-22
  Hecho cuando: `docker build --progress=plain -t backend-rf22 Backend` en frío muestra las descargas de dependencias; tras añadir un comentario en un archivo de `Backend/src/` (o un archivo temporal dentro de `src/`), la segunda construcción muestra el paso `dependency:go-offline` como `CACHED` y el paso `package` sin líneas de descarga de dependencias; se revierte el cambio (`git checkout` o borrado) y `git status` queda limpio.

- [x] **T13. [Infra] Verificar la regresión RF-1 a RF-21 con Docker real (la ejecuta el usuario).** RF-1 a RF-21
  Hecho cuando: con `.env`, `docker compose up --build` deja los tres servicios en marcha; `http://localhost:8081` muestra las 3 terminales y se completa un pedido; abrir `http://localhost:8081/cocina` directamente muestra Cocina; sin `DB_PASSWORD` el comando falla antes de crear contenedores; `docker compose up --wait db` no devuelve el control hasta que la base está sana. No hace falta repetir `mvn test`: no se toca `Backend/src`.

- [x] **T14. [Infra] Documentación del cambio.** —
  Hecho cuando: `MEMORY.md` anota: (1) el mecanismo elegido (capa `dependency:go-offline`; por qué no la montura `--mount=type=cache`, por su limitación conocida de no exportarse a la caché remota del CI); (2) que, **al implementar el job `docker` de la spec 006, su `cache-to` debe llevar `mode=max`** para que la capa de dependencias del backend llegue a la caché, **con un comentario junto a esa línea del workflow**; (3) la conclusión del jar en capas (T10) como mejora opcional futura. En este cambio **no se toca `specs/006-ci-github-actions/` ni `.github/`**. El diff en código e infraestructura se limita a `Backend/Dockerfile` y `MEMORY.md`.
  Hecho: implementado en `MEMORY.md` (los 3 puntos anteriores: capa `dependency:go-offline` y por qué no `--mount=type=cache`, nota de `cache-to mode=max` para la spec 006, y conclusión del jar en capas).
