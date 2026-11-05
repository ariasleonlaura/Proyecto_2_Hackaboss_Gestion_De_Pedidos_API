# Spec 006 — CI con GitHub Actions: verificar cada cambio y publicar imágenes
Estado: aprobada

## Contexto y objetivo
Hoy la verificación del proyecto (compilar el backend, pasar sus pruebas, lint y build del frontend, validez de Docker Compose y construcción de imágenes) se hace a mano en local antes de fusionar. El objetivo es automatizar esas comprobaciones con GitHub Actions en cada cambio, para detectar errores antes de fusionar, y dejar publicadas las imágenes de backend y frontend en el registro de contenedores de GitHub cuando el código llega verde a main. Así, cualquier persona que clone el proyecto (o evalúe su calidad) ve el estado de las comprobaciones de un vistazo y dispone de imágenes construidas a partir de código verificado.

El repositorio es un fork: Actions debe estar activado a mano una vez (ver "Pasos manuales previos").

## Usuarios
- Desarrollador del proyecto (o quien continúe el trabajo) que quiere detectar fallos antes de fusionar y no repetir comprobaciones locales para cada PR.
- Persona que evalúa el proyecto: ve el estado en la insignia del README y puede consultar las imágenes publicadas.

## Historias de usuario
- HU-1. Como desarrollador, quiero que cada PR y cada push a main comprueben backend, frontend y Docker automáticamente, para detectar errores antes de fusionar.
- HU-2. Como desarrollador, quiero que solo el código verde llegado a main publique las imágenes, para que las etiquetas publicadas siempre correspondan a código verificado.
- HU-3. Como desarrollador, quiero que las ejecuciones sean rápidas y reproducibles (caché de dependencias y cancelación de ejecuciones antiguas en los pull requests), para no esperar de más ni ver resultados ambiguos.
- HU-4. Como autor del repositorio, quiero ver el estado de las comprobaciones en una insignia del README, para comprobar la salud del proyecto sin abrir Actions.

## Definiciones
- **Comprobaciones**: el conjunto de verificaciones que se ejecutan en cada ejecución: backend (compilar + todas las pruebas), frontend (instalación reproducible + lint + build) y Docker (validez de Compose + construcción de las dos imágenes).
- **Publicación**: subir las imágenes de backend y frontend al registro de contenedores de GitHub, con la etiqueta del commit y con `latest`.
- **Último commit de main**: el commit al que apunta la punta de la rama main en ese momento (cómo se comprueba se decide en el plan).
- **Valores ficticios**: datos claramente falsos y públicos (p. ej. una contraseña de base de datos "ci-dummy") que el CI usa solo para poder interpretar el Compose y construir; no son secretos y no se conectan a ninguna base de datos real.

## Requisitos funcionales

### Disparadores
- RF-1: CUANDO se abra o actualice un pull request hacia main, EL SISTEMA ejecutará todas las comprobaciones, incluidos los pull requests en borrador (draft).
- RF-2: CUANDO se confirme (push) un commit en main, EL SISTEMA ejecutará todas las comprobaciones y, si todas pasan, publicará las imágenes (RF-17).
- RF-3: CUANDO el autor lance manualmente la comprobación desde la pestaña Actions de GitHub, EL SISTEMA ejecutará todas las comprobaciones sin publicar imágenes. Lanzar la comprobación a mano desde la pestaña Actions ejecuta todas las comprobaciones y no publica ninguna imagen.
- RF-4: SI se hace push a una rama de trabajo que no tiene un pull request abierto hacia main, ENTONCES no se ejecutará ninguna comprobación (para no duplicar la del pull request).

### Verificación del backend
- RF-5: EN cada ejecución de comprobaciones, EL SISTEMA compilará el backend y ejecutará toda su suite de pruebas, incluidas las de base de datos con Testcontainers.
- RF-6: Las pruebas del backend se ejecutarán sin variables de entorno de base de datos configuradas y sin opciones manuales: la suite debe pasar tal como está, solo con el demonio de Docker disponible en el entorno de ejecución.
- RF-7: EL SISTEMA ejecutará el backend con la misma versión del proyecto: Java 17.

### Verificación del frontend
- RF-8: EN cada ejecución de comprobaciones, EL SISTEMA instalará las dependencias del frontend de forma reproducible a partir de su archivo de bloqueo de versiones (package-lock.json), ejecutará el lint y construirá el proyecto.
- RF-9: SI el lint encuentra errores, ENTONCES la ejecución fallará; los avisos (warnings) no bloquearán la ejecución.
- RF-10: EL SISTEMA ejecutará el frontend con la misma versión del proyecto: Node 22.
- RF-11: El trabajo del frontend quedará estructurado para poder añadir después un paso de pruebas de interfaz sin reestructurar el flujo.

### Verificación de Docker
- RF-12: EN cada ejecución de comprobaciones, EL SISTEMA comprobará que el archivo de Docker Compose es válido y que las imágenes de backend y de frontend se construyen correctamente.
- RF-13: Para interpretar el Compose y construir las imágenes, EL SISTEMA usará valores ficticios públicos (como la contraseña "ci-dummy"); no se guardará ningún secreto en el repositorio ni se conectará a ninguna base de datos real, y el archivo de Docker Compose no se modificará para el CI.
- RF-14: La imagen del frontend construida en el CI llevará incrustada la misma dirección del backend por defecto que usa Docker Compose (http://localhost:8080/api); la documentación advertirá de que esa imagen solo es válida en local con Docker Compose y de que una eventual demo se construirá aparte con su dirección pública.

### Fallo y publicación
- RF-15: SI alguna comprobación falla, ENTONCES la ejecución quedará marcada como fallida y no se publicará ninguna imagen.
- RF-16: La publicación solo ocurrirá en un push a main y únicamente tras pasar todas las comprobaciones de esa misma ejecución.
- RF-17: Tras pasar todas las comprobaciones en un push a main, EL SISTEMA publicará las imágenes de backend y de frontend en el registro de contenedores de GitHub, asociadas al repositorio.
- RF-18: Las imágenes publicadas llevarán dos etiquetas: la del commit exacto que se publica y `latest`.
- RF-19: Las imágenes publicadas en una ejecución serán las mismas que se construyeron en esa ejecución, tras pasar las comprobaciones, sin reconstruirse en el paso de publicación.

### Re-ejecuciones y etiquetas
- RF-20: CUANDO se vuelva a ejecutar la comprobación de un commit de main ya publicado, EL SISTEMA volverá a publicar la etiqueta de ese commit, sin fallar ni saltarse el paso (la imagen se reconstruye a partir del mismo código).
- RF-21: SI el commit re-ejecutado ya no es el último de main, ENTONCES la etiqueta `latest` no se moverá: re-ejecutar un commit antiguo nunca hará que `latest` apunte a una imagen anterior. Solo el último commit de main mueve `latest`.
- RF-22: SI la publicación de una de las dos imágenes falla, ENTONCES volver a ejecutar bastará para completar lo que faltaba, sin dañar lo ya publicado.

### Credenciales
- RF-23: EL SISTEMA no requerirá credenciales propias, secretos del repositorio ni variables de entorno configuradas a mano: se limitará a los permisos que el token de GitHub ya otorga a las ejecuciones del propio repositorio.

### Rapidez y reproducibilidad
- RF-24: EL SISTEMA cacheará las dependencias entre ejecuciones (tanto las del backend como las del frontend) para acelerarlas.
- RF-25: CUANDO se lance una ejecución nueva para la rama de un pull request mientras otra de la misma rama sigue en curso, EL SISTEMA cancelará la ejecución antigua de ese pull request. Las ejecuciones de main no se cancelan porque pueden estar publicando imágenes.
- RF-26: Las ejecuciones serán reproducibles: dos ejecuciones sobre el mismo commit dan las mismas comprobaciones y, en main, las mismas imágenes.

### Documentación
- RF-27: EL SISTEMA mostrará el estado de las comprobaciones mediante una insignia (badge) visible en el README del repositorio.
- RF-28: El README documentará: la existencia del flujo de comprobación y publicación, el paso manual de hacer públicos los dos paquetes de imágenes (backend y frontend) tras la primera publicación, y la advertencia de RF-14 sobre la dirección incrustada en la imagen del frontend.

## Requisitos no funcionales
- Las comprobaciones de un PR típico deben ser lo bastante rápidas como para ser útiles en el día a día (caché de dependencias y cancelación de ejecuciones obsoletas del propio PR).
- El flujo no introduce credenciales ni secretos en el repositorio, en las imágenes ni en los logs.
- Las versiones usadas en el CI coinciden con las del proyecto: Java 17 y Node 22.
- Las imágenes publicadas provienen exclusivamente de código que superó todas las comprobaciones.

## Casos límite
- Pull request en borrador (draft): ejecuta las comprobaciones igual que uno abierto (RF-1).
- Push a una rama sin PR abierto: no ejecuta nada (RF-4).
- Push directo a main (sin PR previo): ejecuta comprobaciones y publica si todo pasa.
- Re-ejecutar un commit antiguo de main ya publicado: se re-publica la etiqueta de ese commit y `latest` no retrocede (RF-20, RF-21).
- Fallo parcial de publicación (una sola imagen): la re-ejecución completa lo que faltaba (RF-22).
- Varias entregas (pushes) seguidas a la rama de un pull request: las ejecuciones antiguas de ese pull request se cancelan; en main no se cancela ninguna (RF-25).
- El fork tiene Actions desactivado: las comprobaciones no se ejecutan hasta que el autor las active (paso manual).
- El Compose exige una contraseña de base de datos: el CI la satisface con el valor ficticio sin modificar el archivo (RF-13).
- El demonio de Docker no está disponible en el entorno de ejecución: las pruebas con Testcontainers y la construcción de imágenes fallarían; los entornos de ejecución de GitHub lo incluyen por defecto.
- El registro de contenedores aún no tiene los paquetes creados: la primera publicación desde main los crea; la visibilidad pública se configura después a mano (no bloquea la publicación).

## Fuera de alcance
- Despliegue (servidores, VMs, plataformas gestionadas) y cualquier orquestación posterior a publicar imágenes.
- Kubernetes u otras herramientas de orquestación de contenedores.
- Protección de ramas (rulesets) en main u otras ramas.
- Notificaciones (correo, Slack, issues automáticos) del resultado de las ejecuciones.
- Cambiar automáticamente la visibilidad de los paquetes de imágenes: es un paso manual y único del usuario; el flujo no usa tokens con permisos de administración de paquetes.
- Pruebas de interfaz (Vitest) en el frontend: especificación futura aparte; aquí solo se deja el hueco estructurado (RF-11).
- Publicación en registros distintos del registro de contenedores de GitHub.
- Desactivar o modificar el archivo de Docker Compose del proyecto.

## Criterios de finalización
- Un PR abierto (incluido un draft) hacia main muestra las comprobaciones en curso y su resultado; un push a una rama sin PR no genera ejecución.
- Un PR con un fallo intencional (p. ej. un test roto) queda marcado como fallido y ninguna imagen se publica.
- Un push verde a main publica los dos paquetes (backend y frontend) con la etiqueta del commit y `latest`.
- Re-ejecutar en Actions un commit antiguo de main republica la etiqueta de ese commit y `latest` sigue apuntando al último commit de main.
- Las pruebas del backend pasan en el CI sin configurar credenciales ni variables de entorno de base de datos.
- El lint del frontend solo falla la ejecución si hay errores, no por avisos.
- El archivo de Compose se valida y las dos imágenes se construyen en cada ejecución, usando el valor ficticio y sin tocar el Compose.
- Dos ejecuciones seguidas del mismo commit no descargan dependencias desde cero (caché efectiva) y, en los pull requests, un push nuevo cancela la ejecución anterior de esa rama.
- El README muestra la insignia del estado y documenta los pasos manuales y la advertencia de la imagen del frontend.
- No existen secretos configurados en el repositorio para que el flujo funcione.

## Pasos manuales previos (del usuario, una sola vez)
1. Activar GitHub Actions en el fork (por defecto pueden estar desactivados las acciones en un fork).
2. Tras la primera publicación desde main, hacer públicos a mano, en GitHub, los DOS paquetes de imágenes (backend y frontend). El flujo no intenta cambiar la visibilidad ni usa tokens de administración de paquetes; publicar funciona igual estando los paquetes privados y el criterio de finalización no exige que sean públicos.

## Dudas abiertas
- [PARA EL PLAN] Cómo se comprueba que un commit sigue siendo el último de main para mover o no `latest` (RF-21).
- [PARA EL PLAN] Cómo se vincula cada paquete de imágenes al repositorio en el registro de GitHub.
- [PARA EL PLAN] Si, además de validar el Compose, el CI comprueba explícitamente que el Compose falla cuando la contraseña está vacía (valorado como opcional).
- [PARA EL PLAN] El nombre de las imágenes en el registro de GitHub debe ir en minúsculas, y el nombre del repositorio lleva mayúsculas (APIGestionDePedidos...): el flujo tiene que convertirlo.
- [PARA EL PLAN] El flujo declara de forma explícita los permisos mínimos del token (lectura del código y escritura de paquetes solo en el trabajo que publica).
- [RESUELTA] La ejecución manual (RF-3) ejecuta todas las comprobaciones, incluida la construcción de las imágenes de Docker, y no publica.