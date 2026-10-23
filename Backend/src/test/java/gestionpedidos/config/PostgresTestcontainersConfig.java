package gestionpedidos.config;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Configuración de Testcontainers para los tests de base de datos.
 * Provee un contenedor PostgreSQL compartido entre todos los tests del perfil "test".
 * El contenedor es estático (campo público estático) para que todos los tests
 * compartan la misma instancia y no se arranque uno por test.
 * Spring Boot 4.1.0 detecta el campo @Container y configura el DataSource automáticamente.
 */
@Testcontainers
public class PostgresTestcontainersConfig {

    // Contenedor PostgreSQL compartido estáticamente.
    // El campo estático y @Container hacen que comparta instancia entre tests.
    // Spring Boot detecta este container y configura el DataSource para @DataJpaTest, etc.
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        // Iniciar el contenedor cuando se carga la clase (una sola vez para todo el ciclo de tests)
        postgres.start();
    }
}