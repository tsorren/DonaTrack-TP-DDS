package grupo5.donaciones.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.common.testutils.OpenApiTestUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * El esquema GraphQL tiene dos copias (SPEC-11, D14): la publicada en {@code
 * docs/arquitectura/contratos/donaciones.graphqls} y la que lee el servicio en {@code
 * classpath:graphql/}. Este test falla si difieren o si el servicio carga otro archivo de esquema
 * que el contrato no tiene.
 */
@Tag("contract")
class GraphqlEsquemaContractTest {

  private static final String ARCHIVO = "donaciones.graphqls";
  private static final String EN_EL_SERVICIO = "graphql/" + ARCHIVO;

  @Test
  void laCopiaDelServicioEsIdenticaALaDelContrato() throws IOException {
    String contrato = Files.readString(Path.of(OpenApiTestUtils.resolveContractPath(ARCHIVO)));
    String servicio = leerDelClasspath(EN_EL_SERVICIO);

    assertThat(normalizar(servicio))
        .as(
            "resources/%s tiene que ser idéntico a docs/arquitectura/contratos/%s",
            EN_EL_SERVICIO, ARCHIVO)
        .isEqualTo(normalizar(contrato));
  }

  @Test
  void elServicioCargaUnSoloArchivoDeEsquema() throws IOException {
    // Spring GraphQL lee classpath*:graphql/**/*.graphqls (y *.gqls): todo lo que cargue tiene que
    // estar en el contrato publicado.
    Resource[] recursos =
        new PathMatchingResourcePatternResolver().getResources("classpath*:graphql/**/*");

    assertThat(
            Arrays.stream(recursos)
                .map(Resource::getFilename)
                .filter(n -> n != null && (n.endsWith(".graphqls") || n.endsWith(".gqls"))))
        .containsExactly(ARCHIVO);
  }

  private static String leerDelClasspath(String ruta) throws IOException {
    try (InputStream in =
        GraphqlEsquemaContractTest.class.getClassLoader().getResourceAsStream(ruta)) {
      assertThat(in).as("falta classpath:%s", ruta).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Solo iguala los finales de línea: cualquier otro cambio, incluso de espacios, es diferencia.
   */
  private static String normalizar(String texto) {
    return texto.replace("\r\n", "\n");
  }
}
