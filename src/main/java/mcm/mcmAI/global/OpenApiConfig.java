package mcm.mcmAI.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.api.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI();
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.addServersItem(new Server().url(serverUrl));
        }
        return openAPI;
    }
}
