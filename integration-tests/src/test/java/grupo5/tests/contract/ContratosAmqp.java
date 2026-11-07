package grupo5.tests.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import grupo5.tests.utils.EspiaAmqp;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;

/**
 * Valida mensajes AMQP contra los schemas {@code evento-*} de {@code
 * docs/arquitectura/contratos/schemas/} (JSON Schema draft 2020-12), y aplica la política H de D5
 * (SPEC-05): con {@code CONTRATOS_ESTRICTO=true} (post-merge y corte de entrega) una violación
 * falla; sin él (PR) se reporta como {@code ::warning} y no falla.
 */
public final class ContratosAmqp {

  public static final boolean ESTRICTO =
      Boolean.parseBoolean(
          System.getProperty(
              "contratos.estricto", System.getenv().getOrDefault("CONTRATOS_ESTRICTO", "")));

  /** Avisos del modo no estricto (relativo al módulo integration-tests). */
  public static final Path AVISOS = Path.of("target", "contratos-amqp-avisos.txt");

  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  // En 2020-12 "format" es solo anotación por defecto; los schemas lo usan como regla (uuid, fecha)
  private static final SchemaValidatorsConfig CONFIG =
      SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ContratosAmqp() {}

  /** Schema de un tipo: {@code donacion.asignada.v1} → {@code evento-donacion-asignada-v1}. */
  public static Path schemaDe(String tipo) {
    return directorioSchemas().resolve("evento-" + tipo.replace('.', '-') + ".schema.json");
  }

  /** Violaciones del mensaje contra su schema; un tipo sin schema es una violación. */
  public static List<String> validar(String tipo, byte[] cuerpo) {
    Path schema = schemaDe(tipo);
    if (!Files.exists(schema)) {
      return List.of(tipo + ": no hay schema " + schema.getFileName());
    }
    try (InputStream in = Files.newInputStream(schema)) {
      JsonSchema validador = FACTORY.getSchema(in, CONFIG);
      return validador.validate(MAPPER.readTree(cuerpo)).stream()
          .map(m -> tipo + ": " + m.getMessage())
          .toList();
    } catch (IOException e) {
      return List.of(tipo + ": no se pudo leer el mensaje o el schema (" + e.getMessage() + ")");
    }
  }

  /** Valida todos los mensajes capturados. Sin mensajes también es una violación. */
  public static List<String> validar(List<EspiaAmqp.Mensaje> mensajes) {
    List<String> violaciones = new ArrayList<>();
    if (mensajes.isEmpty()) {
      violaciones.add("el espía no capturó ningún mensaje AMQP");
    }
    for (EspiaAmqp.Mensaje m : mensajes) {
      validar(m.tipo(), m.cuerpo()).stream()
          .map(v -> v + " [" + m.exchange() + " " + m.routingKey() + "]")
          .forEach(violaciones::add);
    }
    return violaciones;
  }

  /** Tipos esperados que el espía no vio: un evento que deja de publicarse también es violación. */
  public static List<String> faltantes(List<EspiaAmqp.Mensaje> mensajes, Set<String> esperados) {
    Set<String> vistos = mensajes.stream().map(EspiaAmqp.Mensaje::tipo).collect(Collectors.toSet());
    return esperados.stream()
        .filter(tipo -> !vistos.contains(tipo))
        .sorted()
        .map(tipo -> tipo + ": el flujo no lo publicó")
        .toList();
  }

  /** Schemas {@code evento-*} que ningún mensaje capturado ejercitó (cobertura de contratos). */
  public static List<String> sinEjercitar(List<EspiaAmqp.Mensaje> mensajes) throws IOException {
    Set<Path> usados =
        mensajes.stream().map(m -> schemaDe(m.tipo()).getFileName()).collect(Collectors.toSet());
    try (Stream<Path> schemas = Files.list(directorioSchemas())) {
      return schemas
          .map(Path::getFileName)
          .filter(p -> p.toString().startsWith("evento-") && !usados.contains(p))
          .map(Path::toString)
          .sorted()
          .toList();
    }
  }

  /**
   * Política H de D5: falla en modo estricto; si no, deja un aviso por violación en {@link
   * #AVISOS}, que run-preprod-tests.sh imprime al final (en GitHub Actions, una anotación).
   */
  public static void reportar(List<String> violaciones) throws IOException {
    if (violaciones.isEmpty()) {
      return;
    }
    if (ESTRICTO) {
      Assertions.fail(
          "Contratos AMQP incumplidos (CONTRATOS_ESTRICTO=true):\n  "
              + String.join("\n  ", violaciones));
    }
    Files.createDirectories(AVISOS.getParent());
    Files.write(
        AVISOS,
        violaciones.stream().map(v -> "::warning::Contrato AMQP: " + v).toList(),
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
  }

  static Path directorioSchemas() {
    Path desdeModulo = Path.of("../docs/arquitectura/contratos/schemas");
    return Files.isDirectory(desdeModulo)
        ? desdeModulo
        : Path.of("docs/arquitectura/contratos/schemas");
  }
}
