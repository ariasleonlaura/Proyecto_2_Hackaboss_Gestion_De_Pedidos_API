package gestionpedidos;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GestionDePedidosApiApplication {

    public static void main(String[] args) {

        SpringApplication.run(GestionDePedidosApiApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Gestión de Pedidos - Hackaboss")
                .version("1.0.0")
                .description("Sistema de gestión de pedidos para terminales de autoservicio. Permite administrar productos, categorías y el flujo de estados de los tickets.")
        );
    }


}
