package gestionpedidos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/* Chequeo del motor al arranque (spec 004): si la URL no apunta a PostgreSQL
(p. ej. alguien cambia a MySQL u H2), la aplicación muere aquí con un mensaje
claro en vez de fallar más tarde con un error de Hibernate confuso.
Es EnvironmentPostProcessor a propósito: corre al preparar el entorno, antes
de montar el contexto y de intentar la primera conexión. */
public class MotorPostgresqlValidator implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url;
        try {

            url = environment.getProperty("spring.datasource.url");
            // Si la URL no está configurada (nula o vacía), no hay nada que validar;
            // fallará más tarde al conectar, pero nuestra comprobación no es responsable.
        } catch (IllegalArgumentException e) {
            // Variable sin definir (p. ej. DB_URL): no hay URL que validar; fallará después con su propio error
            return;
        }
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        if (!url.startsWith("jdbc:postgresql:")) {
            // Extraemos solo el esquema: todo lo que hay antes del primer dos puntos.
            // jdbc:mysql://... → "jdbc:mysql"
            // postgres://... → "postgres"
            // jdbc:postgresql://... → aceptado, no llegamos aquí.
            String esquema = url.split("://")[0];
            throw new RuntimeException(
                "Solo se admite PostgreSQL. Formato esperado: jdbc:postgresql://<host>:<puerto>/<base-de-datos>. " +
                "Esquema recibido: " + esquema
            );
        }
    }
}