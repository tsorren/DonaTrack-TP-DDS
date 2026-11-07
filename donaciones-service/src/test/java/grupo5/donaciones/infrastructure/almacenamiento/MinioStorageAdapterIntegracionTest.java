package grupo5.donaciones.infrastructure.almacenamiento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.ports.StoragePort.AlmacenamientoException;
import grupo5.donaciones.models.ports.StoragePort.Metadatos;
import grupo5.donaciones.models.ports.StoragePort.Operacion;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Contra un MinIO real (misma imagen que el test de insignias de Incentivos). Se saltea si no hay
 * Docker. Las credenciales son sintéticas y solo existen dentro del contenedor.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class MinioStorageAdapterIntegracionTest {

  private static final String PADRONES = "donaciones-padrones";
  private static final String FOTOS = "donaciones-fotos-bienes";
  private static final String USUARIO = "minio-test";
  private static final String CLAVE = "minio-test-secreto";

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>(
              DockerImageName.parse("scnd/minio-mirror:RELEASE.2025-09-07T16-13-09Z"))
          .withEnv("MINIO_ROOT_USER", USUARIO)
          .withEnv("MINIO_ROOT_PASSWORD", CLAVE)
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  private static MinioClient cliente;
  private static StoragePort storage;
  private static final HttpClient http = HttpClient.newHttpClient();

  @BeforeAll
  static void conectar() throws Exception {
    String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    cliente =
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(USUARIO, CLAVE)
            .region("us-east-1")
            .build();
    storage = new MinioStorageAdapter(cliente, cliente);
    new InicializadorBuckets(cliente, List.of(PADRONES, FOTOS)).crearBuckets();
  }

  @Test
  void elInicializadorDeberiaCrearLosBucketsYSerIdempotente() throws Exception {
    new InicializadorBuckets(cliente, List.of(PADRONES, FOTOS)).crearBuckets();

    assertTrue(cliente.bucketExists(BucketExistsArgs.builder().bucket(PADRONES).build()));
    assertTrue(cliente.bucketExists(BucketExistsArgs.builder().bucket(FOTOS).build()));
  }

  @Test
  void deberiaGuardarYLeerUnPadronComoStream() throws Exception {
    byte[] contenido = "﻿DNI,NOMBRE\n1,Ana\n".getBytes(StandardCharsets.UTF_8);

    storage.guardar(
        PADRONES, "padron.csv", new ByteArrayInputStream(contenido), contenido.length, "text/csv");

    try (InputStream leido = storage.abrir(PADRONES, "padron.csv")) {
      assertArrayEquals(contenido, leido.readAllBytes());
    }
    assertEquals(
        new Metadatos(contenido.length, "text/csv"),
        storage.metadatos(PADRONES, "padron.csv").orElseThrow());
  }

  @Test
  void unObjetoInexistenteNoDeberiaTenerMetadatosNiPoderAbrirse() {
    assertTrue(storage.metadatos(PADRONES, "no-existe.csv").isEmpty());
    assertThrows(AlmacenamientoException.class, () -> storage.abrir(PADRONES, "no-existe.csv"));
  }

  @Test
  void conLaUrlDeSubidaElClienteDeberiaSubirSinCredencialesYLuegoDescargar() throws Exception {
    byte[] foto = {(byte) 0x89, 'P', 'N', 'G'};
    String urlSubida =
        storage.urlTemporal(FOTOS, "fotos/bien.png", Duration.ofMinutes(5), Operacion.SUBIR);

    HttpResponse<Void> subida =
        http.send(
            HttpRequest.newBuilder(URI.create(urlSubida))
                .header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(foto))
                .build(),
            HttpResponse.BodyHandlers.discarding());
    assertEquals(200, subida.statusCode());
    assertEquals(
        new Metadatos(foto.length, "image/png"),
        storage.metadatos(FOTOS, "fotos/bien.png").orElseThrow());

    String urlDescarga =
        storage.urlTemporal(FOTOS, "fotos/bien.png", Duration.ofMinutes(5), Operacion.DESCARGAR);
    HttpResponse<byte[]> descarga = get(urlDescarga);
    assertEquals(200, descarga.statusCode());
    assertArrayEquals(foto, descarga.body());
  }

  @Test
  void losBucketsSonPrivadosSinUrlFirmada() throws Exception {
    storage.guardar(
        PADRONES, "privado.csv", new ByteArrayInputStream(new byte[] {1}), 1, "text/csv");
    String urlFirmada =
        storage.urlTemporal(PADRONES, "privado.csv", Duration.ofMinutes(5), Operacion.DESCARGAR);
    String urlSinFirma = urlFirmada.substring(0, urlFirmada.indexOf('?'));

    assertNotEquals(200, get(urlSinFirma).statusCode());
  }

  @Test
  void unaClaveConPuntoPuntoNoDeberiaEscaparDelBucket() {
    ByteArrayInputStream contenido = new ByteArrayInputStream(new byte[] {1});

    assertThrows(
        IllegalArgumentException.class,
        () -> storage.guardar(PADRONES, "../" + FOTOS + "/intruso.csv", contenido, 1, "text/csv"));
    assertThrows(
        IllegalArgumentException.class,
        () -> storage.urlTemporal(FOTOS, "../x", Duration.ofMinutes(1), Operacion.SUBIR));
    assertTrue(storage.metadatos(FOTOS, "intruso.csv").isEmpty());
  }

  private static HttpResponse<byte[]> get(String url) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create(url)).GET().build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }
}
