package grupo5.gateway;

import grupo5.common.seguridad.ReglasDeAcceso;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * La allowlist pública del gateway ({@code donatrack.gateway.rutas-publicas}, formato {@code
 * "MÉTODO /ruta"}) como las rutas sin token de la cadena de seguridad de {@code common-lib}.
 *
 * <p>La cadena abre la ruta para cualquier método, igual que las reglas de los servicios (por
 * ejemplo {@code ReglasDeAccesoNotificaciones}): el método lo decide el {@code @PreAuthorize} del
 * servicio, que es la segunda capa. Por eso {@code GET /api/contacto} pasa el gateway sin token y
 * lo rechaza Notificaciones. Una entrada mal escrita corta el arranque en vez de dejar una ruta
 * abierta o cerrada por error.
 */
@Component
public class ReglasDeAccesoGateway implements ReglasDeAcceso {

  private static final Set<String> METODOS =
      Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  private final List<String> rutas;

  public ReglasDeAccesoGateway(GatewayProperties props) {
    this.rutas =
        Objects.requireNonNullElse(props.rutasPublicas(), List.<String>of()).stream()
            .map(ReglasDeAccesoGateway::ruta)
            .distinct()
            .toList();
  }

  @Override
  public List<String> rutasPublicas() {
    return rutas;
  }

  static String ruta(String entrada) {
    String[] partes = entrada.trim().split("\\s+");
    if (partes.length != 2 || !METODOS.contains(partes[0]) || !partes[1].startsWith("/")) {
      throw new IllegalStateException(
          "Ruta pública inválida, se espera \"MÉTODO /ruta\": \"" + entrada + "\"");
    }
    return partes[1];
  }
}
