package gestionpedidos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class MotorPostgresqlValidator implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        // Si la URL no está configurada (nula o vacía), no hay nada que validar;
        // fallará más tarde al conectar, pero nuestra comprobación no es responsable.
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