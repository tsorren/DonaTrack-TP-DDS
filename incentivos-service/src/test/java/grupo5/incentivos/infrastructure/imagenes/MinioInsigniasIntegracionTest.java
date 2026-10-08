package grupo5.incentivos.infrastructure.imagenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.incentivos.infrastructure.persistencia.mappers.MisionPersistenciaMapper;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.factory.MisionFactory;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Contra un MinIO real (misma imagen que docker-compose). Se saltea si no hay Docker. */
@Testcontainers
@DisabledIfDockerUnavailable
class MinioInsigniasIntegracionTest {

  private static final String BUCKET = "insignias";

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>(
              DockerImageName.parse("scnd/minio-mirror:RELEASE.2025-09-07T16-13-09Z"))
          .withEnv("MINIO_ROOT_USER", "minioadmin")
          .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  private static String endpoint;
  private static MinioClient client;

  @BeforeAll
  static void conectar() {
    endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    client =
        MinioClient.builder().endpoint(endpoint).credentials("minioadmin", "minioadmin").build();
  }

  @Test
  void deberiaPublicarLasImagenesYServirlasSinCredenciales() throws Exception {
    new MinioInsigniasSeeder(client, BUCKET).sembrar();
    MisionPersistenciaMapper mapper =
        new MisionPersistenciaMapper(new MinioImagenesInsigniasAdapter(endpoint, BUCKET));

    for (Mision mision : MisionFactory.crearMisionesEstandar()) {
      String url = mapper.toEntity(mision).getInsignia().getImagenUrl();

      assertTrue(url.startsWith(endpoint + "/" + BUCKET + "/"), url);
      HttpResponse<byte[]> respuesta = get(url);
      assertEquals(200, respuesta.statusCode(), url);
      assertEquals("image/png", respuesta.headers().firstValue("Content-Type").orElse(""));
      assertEquals((byte) 0x89, respuesta.body()[0]);
      assertEquals((byte) 'P', respuesta.body()[1]);
    }
  }

  @Test
  void elSeedDeberiaSerIdempotente() throws Exception {
    MinioInsigniasSeeder seeder = new MinioInsigniasSeeder(client, BUCKET);
    seeder.sembrar();
    StatObjectResponse antes = stat("explorador.png");

    seeder.sembrar();
    StatObjectResponse despues = stat("explorador.png");

    assertEquals(antes.etag(), despues.etag());
    assertEquals(antes.lastModified(), despues.lastModified());
  }

  private static StatObjectResponse stat(String clave) throws Exception {
    return client.statObject(StatObjectArgs.builder().bucket(BUCKET).object(clave).build());
  }

  private static HttpResponse<byte[]> get(String url) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
  }
}
