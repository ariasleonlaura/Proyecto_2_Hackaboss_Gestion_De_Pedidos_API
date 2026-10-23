package gestionpedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.ContextConfiguration;
import gestionpedidos.config.PostgresTestcontainersConfig;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ContextConfiguration(classes = {PostgresTestcontainersConfig.class})
class GestionDePedidosApiApplicationTests {

    @Test
    void contextLoads() {
    }
}