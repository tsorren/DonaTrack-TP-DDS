package grupo5.auth.correo;

import org.keycloak.Config;

/** Cómo conectarse al broker. Se lee de la configuración del proveedor de Keycloak. */
public record ConfiguracionAmqp(
    String host,
    int puerto,
    String vhost,
    String usuario,
    String clave,
    String exchange,
    int timeoutMs) {

  public static final String EXCHANGE_POR_DEFECTO = "auth.exchange";

  /**
   * Lee {@code host}, {@code port}, {@code vhost}, {@code user}, {@code password}, {@code
   * exchange}.
   */
  public static ConfiguracionAmqp desde(Config.Scope scope) {
    return new ConfiguracionAmqp(
        scope.get("host", "localhost"),
        scope.getInt("port", 5672),
        scope.get("vhost", "/"),
        scope.get("user", "guest"),
        scope.get("password"),
        scope.get("exchange", EXCHANGE_POR_DEFECTO),
        scope.getInt("timeout-ms", 5000));
  }

  /** Sin clave no se puede publicar: se avisa al arrancar y se corta en cada intento. */
  public boolean completa() {
    return clave != null && !clave.isBlank();
  }
}
