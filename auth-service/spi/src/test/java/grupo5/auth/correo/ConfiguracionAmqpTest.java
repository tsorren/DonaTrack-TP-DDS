package grupo5.auth.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;

class ConfiguracionAmqpTest {

  /** Un Config.Scope con solo lo que lee la configuración: get y getInt. */
  private static Config.Scope scope(Map<String, String> valores) {
    return (Config.Scope)
        Proxy.newProxyInstance(
            Config.Scope.class.getClassLoader(),
            new Class<?>[] {Config.Scope.class},
            (proxy, metodo, args) -> {
              String valor = valores.get((String) args[0]);
              boolean conDefecto = args.length == 2;
              return switch (metodo.getName()) {
                case "get" -> valor != null ? valor : conDefecto ? args[1] : null;
                case "getInt" -> valor != null ? Integer.valueOf(valor) : args[1];
                default -> null;
              };
            });
  }

  @Test
  void sinNadaUsaLosValoresPorDefectoYNoEstaCompleta() {
    ConfiguracionAmqp config = ConfiguracionAmqp.desde(scope(Map.of()));

    assertEquals("localhost", config.host());
    assertEquals(5672, config.puerto());
    assertEquals("/", config.vhost());
    assertEquals("guest", config.usuario());
    assertEquals("auth.exchange", config.exchange());
    assertEquals(5000, config.timeoutMs());
    assertFalse(config.completa());
  }

  @Test
  void leeCadaValorDeLaConfiguracionDelProveedor() {
    ConfiguracionAmqp config =
        ConfiguracionAmqp.desde(
            scope(
                Map.of(
                    "host", "rabbitmq",
                    "port", "5673",
                    "vhost", "donatrack",
                    "user", "donatrack",
                    "password", "secreta",
                    "exchange", "otro.exchange",
                    "timeout-ms", "2500")));

    assertEquals("rabbitmq", config.host());
    assertEquals(5673, config.puerto());
    assertEquals("donatrack", config.vhost());
    assertEquals("donatrack", config.usuario());
    assertEquals("secreta", config.clave());
    assertEquals("otro.exchange", config.exchange());
    assertEquals(2500, config.timeoutMs());
    assertTrue(config.completa());
  }

  @Test
  void unaClaveEnBlancoNoCuentaComoCompleta() {
    assertFalse(ConfiguracionAmqp.desde(scope(Map.of("password", "  "))).completa());
  }
}
