package grupo5.donaciones.models.ports;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lee el padrón CSV de donantes por partes, sin tenerlo entero en memoria (R11-B08, DTI-40 ítem 4).
 * Cada método lee el stream hasta el final y lo cierra al terminar o al fallar.
 */
public interface CargadorDonantes {

  /**
   * Cuenta las líneas no vacías después del encabezado, también las mal formadas, sin guardarlas.
   * Es el mismo número de filas que entrega {@link #leerPorTandas}.
   */
  int contarFilas(InputStream contenido);

  /**
   * Entrega las filas de a tandas de, como mucho, {@code tamanioTanda}: una fila por línea no vacía
   * después del encabezado, también las mal formadas (con {@code campos} nulos), para que el
   * importador las cuente como error (HB-14). Cada tanda se entrega antes de leer la siguiente; si
   * {@code alLeerTanda} falla, la lectura se corta con esa excepción.
   */
  void leerPorTandas(
      InputStream contenido, int tamanioTanda, Consumer<List<FilaPadron>> alLeerTanda);

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
