# Etapa 1: compilar el jar con Maven. Esta imagen no llega a producción.
FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
# El pom primero y el código después: Docker reutiliza la caché de capas
# y no vuelve a descargar dependencias si solo cambia el código.
COPY pom.xml .
# Capa de dependencias: solo depende del pom, no del código → si el pom no
# cambia, esta capa queda de caché y el .m2 de la imagen ya tiene todo
# descargado; así, un cambio de código solo reconstruye el `package` sin
# volver a bajar nada (RF-22).
RUN mvn -B dependency:go-offline
COPY src ./src
# Los tests se ejecutan en el CI, no al construir la imagen.
RUN mvn -B -DskipTests package

# Etapa 2: solo el JRE, sin Maven ni código fuente.
# No se usa la variante -alpine: no existe para arm64 (Mac con Apple Silicon).
FROM eclipse-temurin:17-jre
WORKDIR /app
# Usuario sin privilegios: la aplicación no se ejecuta como root.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]