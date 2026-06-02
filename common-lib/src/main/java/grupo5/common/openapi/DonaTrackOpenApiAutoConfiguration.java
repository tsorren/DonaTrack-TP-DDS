package grupo5.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(DonaTrackOpenApiProperties.class)
public class DonaTrackOpenApiAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public OpenAPI donaTrackOpenApi(DonaTrackOpenApiProperties properties, Environment environment) {
    String applicationName = environment.getProperty("spring.application.name", "donaTrack-service");
    String serverPort = environment.getProperty("server.port", "8080");

    String title = valueOrDefault(properties.getTitle(), applicationName);
    String serverUrl = valueOrDefault(properties.getServerUrl(), "http://localhost:" + serverPort);

    return new OpenAPI()
        .info(new Info()
            .title(title)
            .version(properties.getVersion())
            .description(properties.getDescription()))
        .servers(List.of(new Server()
            .url(serverUrl)
            .description(properties.getServerDescription())));
  }

  private String valueOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }
}