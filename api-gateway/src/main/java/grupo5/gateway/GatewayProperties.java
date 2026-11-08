package grupo5.gateway;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs base de los servicios y allowlist pública del gateway.
 *
 * <p>{@code rutasPublicas} son las rutas que en la etapa 2 (F5) pasan sin token (D26, RI-GW-03),
 * con el formato {@code "MÉTODO /ruta"}. En la etapa 1 no se aplica: todas las rutas pasan sin
 * token.
 */
@ConfigurationProperties("donatrack.gateway")
public record GatewayProperties(Servicios servicios, List<String> rutasPublicas) {

  public record Servicios(
      String donaciones, String notificaciones, String incentivos, String logistica) {}
}
