package grupo5.logistica.infrastructure.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.dto.entregas.FotoRecepcionSubidaDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.impl.FotosRecepcionService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Subida y lectura de fotos de recepción contra un MinIO real (misma imagen que el test de
 * almacenamiento de Donaciones). Se saltea si no hay Docker. Las credenciales son sintéticas y solo
 * existen dentro del contenedor.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class MinioAlmacenFotosRecepcionIntegracionTest {

  private static final String USUARIO = "minio-test";
  private static final String CLAVE = "minio-test-secreto";
  private static final UUID ID_ENTREGA = UUID.randomUUID();

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>(
              DockerImageName.parse("scnd/minio-mirror:RELEASE.2025-09-07T16-13-09Z"))
          .withEnv("MINIO_ROOT_USER", USUARIO)
          .withEnv("MINIO_ROOT_PASSWORD", CLAVE)
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static MinioClient cliente;
  private static MinioAlmacenFotosRecepcion almacen;
  private static FotosRecepcionService servicio;

  @BeforeAll
  static void conectar() {
    String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    cliente =
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(USUARIO, CLAVE)
            .region("us-east-1")
            .build();
    almacen = new MinioAlmacenFotosRecepcion(cliente, cliente);
    almacen.crearBucketSiFalta();

    IEntregasRepository repositorio = mock(IEntregasRepository.class);
    Entrega entrega = mock(Entrega.class);
    when(entrega.getEstadoActual()).thenReturn(EstadoEntrega.EN_TRASLADO);
    when(repositorio.findById(ID_ENTREGA)).thenReturn(Optional.of(entrega));
    servicio = new FotosRecepcionService(repositorio, almacen, "http://gateway.test");
  }

  @Test
  void elBucketSeCreaYCrearloDeNuevoNoFalla() throws Exception {
    almacen.crearBucketSiFalta();

    assertThat(
            cliente.bucketExists(
                BucketExistsArgs.builder().bucket(MinioAlmacenFotosRecepcion.BUCKET).build()))
        .isTrue();
  }

  @Test
  void laSubidaGuardaConLaClaveEsperada_yLaUrlFirmadaLaLeeYVenceEnCincoMinutos() throws Exception {
    FotoRecepcionSubidaDTO subida = servicio.subir(ID_ENTREGA, List.of(png())).getFirst();

    assertThat(almacen.existe("entregas/" + ID_ENTREGA + "/" + subida.fotoId() + ".png")).isTrue();

    String firmada = servicio.urlFirmada(ID_ENTREGA, subida.fotoId());
    assertThat(firmada).contains("X-Amz-Expires=300");
    HttpResponse<byte[]> lectura =
        HTTP.send(
            HttpRequest.newBuilder(URI.create(firmada)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(lectura.statusCode()).isEqualTo(200);
    assertThat(lectura.headers().firstValue("Content-Type")).contains("image/png");
    assertThat(ImageIO.read(new ByteArrayInputStream(lectura.body())).getWidth()).isEqualTo(4);
  }

  @Test
  void sinFirmaElBucketNoSeLee() throws Exception {
    FotoRecepcionSubidaDTO subida = servicio.subir(ID_ENTREGA, List.of(png())).getFirst();
    String sinFirma =
        "http://"
            + minio.getHost()
            + ":"
            + minio.getMappedPort(9000)
            + "/"
            + MinioAlmacenFotosRecepcion.BUCKET
            + "/entregas/"
            + ID_ENTREGA
            + "/"
            + subida.fotoId()
            + ".png";

    HttpResponse<Void> lectura =
        HTTP.send(
            HttpRequest.newBuilder(URI.create(sinFirma)).GET().build(),
            HttpResponse.BodyHandlers.discarding());

    assertThat(lectura.statusCode()).isEqualTo(403);
  }

  @Test
  void unaClaveInexistenteNoExiste() {
    assertThat(almacen.existe("entregas/" + ID_ENTREGA + "/" + UUID.randomUUID() + ".jpg"))
        .isFalse();
  }

  private static byte[] png() throws Exception {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", salida);
    return salida.toByteArray();
  }
}
