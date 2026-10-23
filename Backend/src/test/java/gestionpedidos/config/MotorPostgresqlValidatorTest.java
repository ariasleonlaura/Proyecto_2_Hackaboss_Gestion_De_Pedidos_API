package gestionpedidos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for MotorPostgresqlValidator.
 * Verifica que el validador rechaza URLs que no son PostgreSQL
 * y acepta las que lo son.
 */
class MotorPostgresqlValidatorTest {

    private final MotorPostgresqlValidator validator = new MotorPostgresqlValidator();

    @Test
    void debeRechazarMySQLYMostrarEsquema() {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:mysql://localhost:3306/mydb");
        sources.addFirst(new MapPropertySource("test-mysql", props));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            validator.postProcessEnvironment(env, new SpringApplication(gestionpedidos.GestionDePedidosApiApplication.class));
        });

        String message = exception.getMessage();
        assertTrue(message.contains("jdbc:mysql"),
            "El mensaje debe contener 'jdbc:mysql', pero fue: " + message);
        assertTrue(message.contains("Solo se admite PostgreSQL"),
            "El mensaje debe indicar que solo se admite PostgreSQL");
        // Verificar que no se incluye la URL completa ni credenciales
        assertFalse(message.contains("localhost:3306"),
            "El mensaje no debe contener la host:puerta completa");
        assertFalse(message.contains("mydb"),
            "El mensaje no debe contener el nombre de la base de datos");
    }

    @Test
    void debeRechazarPostgresUrlYMostrarEsquema() {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "postgres://localhost:5432/mydb");
        sources.addFirst(new MapPropertySource("test-postgres", props));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            validator.postProcessEnvironment(env, new SpringApplication(gestionpedidos.GestionDePedidosApiApplication.class));
        });

        String message = exception.getMessage();
        assertTrue(message.contains("postgres"),
            "El mensaje debe contener 'postgres', pero fue: " + message);
        assertTrue(message.contains("Solo se admite PostgreSQL"),
            "El mensaje debe indicar que solo se admite PostgreSQL");
        // Verificar que no se incluye la URL completa ni credenciales
        assertFalse(message.contains("localhost:5432"),
            "El mensaje no debe contener la host:puerta completa");
        assertFalse(message.contains("mydb"),
            "El mensaje no debe contener el nombre de la base de datos");
    }

    @Test
    void debeAceptarPostgreSQLSinLanzarExcepcion() {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/mydb");
        sources.addFirst(new MapPropertySource("test-postgres", props));

        // Debe lanzar excepción lanzada (no debería lanzar nada)
        validator.postProcessEnvironment(env, new SpringApplication(gestionpedidos.GestionDePedidosApiApplication.class)); // expected: no exception
    }
}