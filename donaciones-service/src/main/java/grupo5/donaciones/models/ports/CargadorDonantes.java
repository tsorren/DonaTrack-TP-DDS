package grupo5.donaciones.models.ports;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface CargadorDonantes {
  /**
   * Lee el padrón CSV completo del stream y lo cierra al terminar. Devuelve una fila por línea no
   * vacía después del encabezado, también las mal formadas (con {@code campos} nulos), para que el
   * importador las cuente como error (HB-14).
   */
  List<FilaPadron> cargarDonantes(InputStream contenido);

  /**
   * Una línea del padrón.
   *
   * @param linea número de línea del CSV, contando el encabezado como línea 1
   * @param campos columna → valor; {@code null} si la línea no respeta el formato del encabezado
   */
  record FilaPadron(int linea, Map<String, String> campos) {
    public boolean formatoValido() {
      return campos != null;
    }
  }
}
