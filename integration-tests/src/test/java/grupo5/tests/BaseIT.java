package grupo5.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class BaseIT {

  protected static final String DONACIONES_URL =
      System.getProperty("donaciones.url", "http://localhost:8080");
  protected static final String NOTIFICACIONES_URL =
      System.getProperty("notificaciones.url", "http://localhost:8081");
  protected static final String INCENTIVOS_URL =
      System.getProperty("incentivos.url", "http://localhost:8082");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Carga un fixture JSON desde src/test/resources/fixtures/{path} como Map mutable.
   * Los campos dinamicos (ej: idDonante) se sobreescriben via map.put() en el test.
   */
  protected Map<String, Object> fixture(String path) {
    String resourcePath = "/fixtures/" + path;
    try (InputStream is = BaseIT.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Fixture no encontrado: " + resourcePath);
      }
      return MAPPER.readValue(is, new TypeReference<>() {});
    } catch (IOException e) {
      throw new RuntimeException("Error al leer fixture: " + resourcePath, e);
    }
  }
}
