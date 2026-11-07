package grupo5.donaciones.models.ports;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface CargadorDonantes {
  /** Lee el padrón CSV completo del stream y lo cierra al terminar. */
  List<Map<String, String>> cargarDonantes(InputStream contenido);
}
