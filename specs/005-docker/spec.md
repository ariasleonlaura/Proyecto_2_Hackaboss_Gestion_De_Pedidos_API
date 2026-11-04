# Spec 005 — Docker: levantar el sistema completo con un comando
Estado: implementada

## Contexto y objetivo
Hoy, para ejecutar el proyecto hay que instalar y configurar a mano PostgreSQL, el backend y el frontend. El objetivo es poder levantar el sistema completo (base de datos, backend y frontend) con un solo comando y sin instalar nada más que Docker, de modo que cualquiera pueda ejecutarlo igual que su autor y quede preparado el despliegue. También debe poder levantarse solo la base de datos, para trabajar con el backend y el frontend desde el IDE.

Depende de la spec 004 (PostgreSQL y Flyway), que es la que prepara la base de datos vacía con el esquema y los datos de ejemplo.

**Cambio en revisión (rendimiento de construcción)**: la base de esta spec ya está implementada; este cambio añade mejoras de velocidad de reconstrucción de las imágenes —sobre todo la del backend, que hoy vuelve a descargar todas las dependencias de Maven cada vez que cambia solo el código— sin alterar en nada el comportamiento observable en runtime: todos los requisitos de esta spec (RF-1 a RF-21) siguen vigentes tal cual. Los archivos existentes se mejoran a mano.

## Usuarios
- Desarrollador (el autor, o quien clone el repositorio) que quiere ejecutar o probar el sistema.
- Persona que evalúa el proyecto y quiere verlo funcionando sin una configuración compleja.

## Historias de usuario
- HU-1. Como persona que clona el proyecto, quiero levantarlo todo con un único comando para verlo funcionando sin instalar PostgreSQL, Java ni Node.
- HU-2. Como desarrollador, quiero levantar solo la base de datos para ejecutar el backend y el frontend desde el IDE.
- HU-3. Como desarrollador, quiero que mis datos se conserven entre reinicios y poder empezar de cero cuando quiera.
- HU-4. Como autor del repositorio, quiero que las contraseñas no se suban a git para no exponer credenciales.
- HU-5. Como desarrollador, quiero que reconstruir la imagen tras cambiar solo el código no vuelva a descargar todas las dependencias, para iterar rápido sin conexión lenta.

## Definiciones
- **Modo "todo en Docker"**: base de datos, backend y frontend se ejecutan en contenedores.
- **Modo "solo base de datos"**: solo la base de datos se ejecuta en un contenedor; backend y frontend se ejecutan desde el IDE.
- **Base de datos lista**: la base de datos acepta conexiones.
- **Configuración real**: archivo con las credenciales y los puertos que usa quien ejecuta el sistema. No se sube a git.
- **Configuración de ejemplo**: archivo versionado que sirve de plantilla de la configuración real.
- **Datos de ejemplo**: terminales, categorías y productos que carga la spec 004 en una base de datos vacía.

## Requisitos funcionales

### Arranque
- RF-1: CUANDO el usuario ejecute el comando del modo "todo en Docker", EL SISTEMA construirá y levantará la base de datos, el backend y el frontend.
- RF-2: CUANDO el usuario ejecute el comando del modo "solo base de datos", EL SISTEMA levantará únicamente la base de datos, sin construir ni arrancar el backend ni el frontend.
- RF-3: CUANDO el sistema arranque sobre una base de datos vacía, EL SISTEMA la dejará preparada con el esquema y los datos de ejemplo, sin pasos manuales.
- RF-4: MIENTRAS la base de datos no esté lista, EL SISTEMA no arrancará el backend en el modo "todo en Docker".
- RF-5: CUANDO el usuario ejecute el comando del modo "solo base de datos", EL SISTEMA no devolverá el control hasta que la base de datos esté lista.
- RF-6: CUANDO el usuario abra o recargue directamente una ruta de la aplicación (por ejemplo, la pantalla de Cocina), EL SISTEMA mostrará esa pantalla y no un error de página no encontrada.

### Configuración y credenciales
- RF-7: SI la contraseña de la base de datos no está definida o está vacía, ENTONCES EL SISTEMA fallará inmediatamente, antes de crear o arrancar ningún contenedor ni volumen, con un código de salida distinto de cero y un mensaje que nombre el dato que falta e indique cómo corregirlo, sin mostrar ningún valor. Esto aplica a los dos modos, al primer arranque y a los reinicios.
- RF-8: EL SISTEMA leerá las credenciales y los puertos de un único archivo de configuración real que queda fuera del control de versiones, y el repositorio incluirá una configuración de ejemplo con la contraseña vacía.
- RF-9: EL SISTEMA no mostrará la configuración real como pendiente de subir a git, y la configuración de ejemplo estará siempre versionada.
- RF-10: EL SISTEMA incluirá en la configuración de ejemplo: una cabecera con las instrucciones de uso; la contraseña vacía y marcada como obligatoria; el usuario y el nombre de la base de datos con valores por defecto funcionales; los puertos con sus valores por defecto y la indicación de que se pueden cambiar; un comentario por variable (qué es, si es obligatoria u opcional y su valor por defecto); y la indicación de que, en el modo "solo base de datos", el IDE debe usar el mismo usuario y la misma contraseña.
- RF-11: CUANDO el usuario copie la configuración de ejemplo como configuración real y defina una contraseña, EL SISTEMA arrancará con un único comando, sin necesidad de consultar documentación externa.

### Conectividad y puertos
- RF-12: MIENTRAS el sistema se ejecute en el modo "todo en Docker", EL SISTEMA conectará el backend con la base de datos mediante el nombre de su servicio y aceptará las peticiones del frontend servido en Docker.
- RF-13: EL SISTEMA publicará el puerto de la base de datos únicamente para la propia máquina, de modo que, en el modo "solo base de datos", el backend ejecutado desde el IDE se conecte a ella en localhost.
- RF-14: EL SISTEMA publicará la base de datos, el backend y el frontend en los puertos 5432, 8080 y 8081 por defecto, y permitirá cambiarlos desde la configuración real cuando estén ocupados. El de la base de datos es el único restringido a la propia máquina.
- RF-15: EL SISTEMA usará la misma dirección del backend en los dos modos, fijada al construir el frontend, de modo que cambiarla exige reconstruirlo.
- RF-16: SI un puerto publicado ya está ocupado en la máquina (por ejemplo, por el backend del IDE en el 8080), ENTONCES el arranque fallará por ese conflicto, y la documentación advertirá de que el backend del IDE y el de Docker no pueden estar activos a la vez.

### Datos y versión
- RF-17: CUANDO el usuario detenga los contenedores, EL SISTEMA conservará los datos de la base de datos, en los dos modos.
- RF-18: CUANDO el usuario solicite explícitamente borrar los datos, EL SISTEMA los eliminará, y el siguiente arranque partirá de una base de datos vacía (RF-3).
- RF-19: EL SISTEMA usará la misma versión mayor y la misma variante de PostgreSQL en Docker y en las pruebas.
- RF-20: SI se cambia la versión mayor de PostgreSQL sobre datos ya creados, ENTONCES la base de datos no arrancará con esos datos, y la documentación explicará que hay que borrarlos y empezar de cero.
- RF-21: SI la contraseña de la configuración real es distinta de la de una base de datos ya creada, ENTONCES EL SISTEMA no modificará la de la base existente, el backend no podrá conectarse, y la documentación explicará que hay que borrar los datos o cambiarla dentro de la base.

### Construcción de las imágenes
- RF-22: CUANDO el usuario reconstruya la imagen del backend tras un cambio solo en el código fuente, EL SISTEMA no volverá a descargar las dependencias de Maven durante la construcción; el resultado de la imagen y su comportamiento en runtime no cambian.

## Requisitos no funcionales
- Los mensajes que genere el proyecto están en español.
- Ninguna contraseña se guarda en el repositorio, en las imágenes ni en los mensajes de error.
- La base de datos no queda expuesta a la red: solo es accesible desde la propia máquina.
- Los servicios de la aplicación no se ejecutan con privilegios de administrador dentro de su contenedor.
- El resultado es reproducible: dos ejecuciones sobre una copia nueva del repositorio dan el mismo sistema.
- La optimización de construcción no altera el comportamiento observable en runtime: los RF-1 a RF-21 se cumplen exactamente igual que antes del cambio.
- Se verifica con Docker Desktop en macOS.

## Casos límite
- Puerto ocupado, por ejemplo con el backend del IDE en marcha y el modo "todo en Docker" (RF-16).
- Backend del IDE arrancado antes de que la base de datos esté lista: falla al arrancar con un error de conexión, sin dañar datos, y basta con repetirlo.
- Contraseña distinta de la de una base de datos ya creada (RF-21).
- Cambio de versión mayor de PostgreSQL con datos existentes (RF-20).
- Contraseña con caracteres especiales: no se garantiza su funcionamiento; se recomienda una contraseña sencilla.
- Primer arranque: la descarga de imágenes y la construcción son más lentas y requieren conexión a internet.
- Docker no está en marcha: el error es el propio de Docker, fuera del control del proyecto.
- Los dos modos comparten los mismos datos: lo guardado con uno se ve desde el otro.
- Reconstruir sin cambiar nada: la caché evita el trabajo, pero el resultado es el mismo; no es un requisito medir el tiempo.
- Borrar la caché de construcción de Docker (`docker builder prune` o `--no-cache`): la siguiente construcción vuelve a descargar dependencias una vez; el sistema resultante es idéntico.

## Fuera de alcance
- Optimizar el tiempo de arranque en runtime o el de la base de datos: solo la velocidad de construcción de las imágenes.
- Integración continua, publicación de imágenes en un registro y despliegue.
- El modo mixto: backend en Docker con el frontend en el IDE.
- Recarga en caliente dentro de los contenedores.
- Copias de seguridad y migración de datos desde MySQL.
- HTTPS, dominios propios o proxy inverso.
- Cualquier otro motor de base de datos distinto de PostgreSQL.

## Criterios de finalización
- En una copia nueva del repositorio, copiar la configuración de ejemplo como real, definir una contraseña y ejecutar un único comando levanta el sistema: el frontend, en el puerto 8081, muestra las terminales de ejemplo y se puede completar un pedido de Terminal a Recogida.
- Sin contraseña (ausente o vacía), el comando falla antes de crear ningún contenedor, con un mensaje claro y un código de salida distinto de cero, en los dos modos.
- Un segundo arranque conserva los datos y no duplica nada.
- El modo "solo base de datos" no devuelve el control hasta que la base está lista, y el backend del IDE conecta a ella.
- Borrar los datos deja una base vacía que se prepara sola en el siguiente arranque.
- Recargar la pantalla de Cocina en el navegador muestra Cocina.
- Cambiar un puerto en la configuración real hace que el sistema arranque en ese puerto.
- La configuración real no aparece en el estado de git y la de ejemplo está versionada.
- La etiqueta de PostgreSQL coincide en Docker y en las pruebas.
- La documentación describe los dos comandos, los puertos y las advertencias de los casos límite.
- Las pruebas del backend siguen en verde.
- Tras cambiar un archivo en `Backend/src/` y volver a construir, los logs de la construcción no muestran una nueva descarga de dependencias de Maven (RF-22), y el sistema levantado se comporta igual que antes: todos los criterios de esta lista siguen cumpliéndose.

## Dudas abiertas
- [RESUELTA] Un único archivo de configuración de Compose y dos comandos, sin perfiles y sin la clave "version". Modo "todo en Docker": construir y levantar todo. Modo "solo base de datos": levantar solo ese servicio y esperar a que esté lista (el comando es up --wait db; con -d a secas no espera).
- [RESUELTA] El archivo de Compose es el definitivo y se versiona; no hay uno "de ejemplo". Lo único con versión de ejemplo es la configuración (.env.example), y el .env real, en la raíz, queda fuera de git. No es el Backend/.env, que Spring no lee. El .gitignore ignora .env pero no .env.example.
- [RESUELTA] Contraseña obligatoria: la comprobación de RF-7 se hace con la propia herramienta de Compose (variable obligatoria con mensaje) antes de crear nada. El archivo de ejemplo deja la contraseña vacía a propósito.
- [RESUELTA] Las variables no cambian de nombre: DB_URL (con el nombre del servicio de la base de datos en Docker y con localhost en local), DB_USERNAME, DB_PASSWORD, FRONTEND_URL (http://localhost:8081 en Docker, http://localhost:5173 en local) y VITE_API_URL (http://localhost:8080/api en los dos modos, fijada al construir el frontend; debe terminar en /api). La validación de que DB_URL empiece por jdbc:postgresql: es de la spec 004 y no se repite aquí.
- [RESUELTA] Comprobación de salud: la base de datos tiene una comprobación de salud de PostgreSQL, y de ella dependen el arranque ordenado del backend (RF-4) y la espera del modo "solo base de datos" (RF-5).
- [RESUELTA] Imagen de la base de datos: postgres:17-alpine, con la misma etiqueta en Docker y en Testcontainers. Sin imagen personalizada y sin fijar la versión menor. El plan decide si se añade una comprobación automática de que las dos etiquetas no divergen.
- [RESUELTA] Para el plan, imágenes: el backend se construye en dos etapas (Maven y JRE 17) y se ejecuta sin privilegios de administrador; el frontend se construye en dos etapas (Node 22 y nginx) con reescritura de rutas hacia la página principal.
- [RESUELTA] Para el plan, base de datos: el puerto se publica solo en 127.0.0.1, con volumen para los datos y comprobación de salud.
- [RESUELTA] Para el plan, tests: el comando de las pruebas del backend sigue siendo mvn -f Backend/pom.xml test -Dspring.profiles.active=test, y se valora que no haga falta pasar el perfil a mano.
- [RESUELTA] `docker init` no se usa como generador de los Dockerfiles ni del Compose: solo como referencia de ideas. Los archivos existentes se mejoran a mano para no romper las decisiones ya tomadas (imágenes base, `:?`, healthchecks, puertos, etc.).
- [RESUELTA] Mecanismo de RF-22: elegida la capa de dependencias con `dependency:go-offline` (plan.md, sección C1). No se usa la caché montada (`RUN --mount=type=cache`) por su limitación conocida de no exportarse a la caché remota del CI de la spec 006 (y sería incompatible con la capa: sombrearía `/root/.m2`), según el trade-off analizado en plan.md, sección C2.
- [RESUELTA] Jar en capas (investigación, no requisito): Spring Boot 4.1 **sí** permite extraer capas del jar de forma soportada (`java -Djarmode=tools -jar app.jar extract --layers`), pero no se implanta en este cambio; queda como mejora opcional futura (plan.md, sección C5; T10). Sobre el frontend, no hace falta mejora análoga: su instalación de dependencias ya queda cacheada por capas (plan.md, sección C3).