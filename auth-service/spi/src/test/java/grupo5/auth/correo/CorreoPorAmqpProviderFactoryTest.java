package grupo5.auth.correo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.keycloak.email.EmailTemplateProviderFactory;

class CorreoPorAmqpProviderFactoryTest {

  private static final String SERVICIO =
      "META-INF/services/org.keycloak.email.EmailTemplateProviderFactory";

  @Test
  void elIdEsElQueSeElige() {
    assertEquals("donatrack-amqp", new CorreoPorAmqpProviderFactory().getId());
    assertEquals("donatrack-amqp", CorreoPorAmqpProviderFactory.ID);
  }

  @Test
  void noPisaAlProveedorPorDefectoDeKeycloak() {
    assertNotEquals("freemarker", new CorreoPorAmqpProviderFactory().getId());
  }

  /**
   * Keycloak trae su propio archivo de servicios para este proveedor, así que hay varias copias en
   * el classpath: alcanza con que una de ellas liste la fábrica de este proyecto.
   */
  @Test
  void keycloakLaEncuentraPorElArchivoDeServicios() throws Exception {
    List<String> declaradas = new ArrayList<>();
    for (URL recurso : Collections.list(getClass().getClassLoader().getResources(SERVICIO))) {
      try (var flujo = recurso.openStream()) {
        for (String linea : new String(flujo.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
          if (!linea.isBlank() && !linea.startsWith("#")) {
            declaradas.add(linea.trim());
          }
        }
      }
    }

    String nombre = CorreoPorAmqpProviderFactory.class.getName();
    assertTrue(declaradas.contains(nombre), "no está declarada: " + declaradas);
    assertTrue(EmailTemplateProviderFactory.class.isAssignableFrom(Class.forName(nombre)));
  }
}
