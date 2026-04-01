package fingenie.com.fingenie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinGenie API")
                        .version("v2")
                        .description("Financial Management AI Assistant API")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .security(List.of(
                    new SecurityRequirement().addList("bearerAuth")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .securitySchemes(Map.of(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                        )
                ));
    }

    @GetMapping("/api-docs")
    public ResponseEntity<String> apiDocs() {
        return ResponseEntity.ok("Swagger UI available at: /swagger-ui/index.html");
    }
}
