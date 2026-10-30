package grupo5.donaciones;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.infrastructure.almacenamiento.MinioStorageAdapter;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.IFotosBienesService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Arranca la aplicación con el perfil {@code postgres} y MinIO encendido, contra Postgres y MinIO
 * reales. Complementa a {@link DonacionesPostgresContextTests} (mismo perfil, sin MinIO): prueba el
 * cableado del almacenamiento, la creación de buckets al arrancar, el padrón CSV de punta a punta y
 * el ciclo de una foto (subida prefirmada, validación y URL de descarga). Credenciales sintéticas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
@DisabledIfDockerUnavailable
class DonacionesPostgresMinioContextTests {

  private static final String USUARIO_MINIO = "minio-test";
  private static final String CLAVE_MINIO = "minio-test-secreto";
  private static final Duration ESPERA_IMPORTACION = Duration.ofSeconds(30);

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>(
              DockerImageName.parse("scnd/minio-mirror:RELEASE.2025-09-07T16-13-09Z"))
          .withEnv("MINIO_ROOT_USER", USUARIO_MINIO)
          .withEnv("MINIO_ROOT_PASSWORD", CLAVE_MINIO)
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("donaciones.minio.enabled", () -> "true");
    registry.add("donaciones.minio.endpoint", DonacionesPostgresMinioContextTests::endpointMinio);
    registry.add(
        "donaciones.minio.public-endpoint", DonacionesPostgresMinioContextTests::endpointMinio);
    registry.add("donaciones.minio.access-key", () -> USUARIO_MINIO);
    registry.add("donaciones.minio.secret-key", () -> CLAVE_MINIO);
  }

  private static String endpointMinio() {
    return "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private StoragePort storage;
  @Autowired private MinioClient minioClient;
  @Autowired private AlmacenamientoProperties almacenamiento;
  @Autowired private IArchivoDonantesRepository archivos;
  @Autowired private IFotosBienesService fotosBienes;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient http = HttpClient.newHttpClient();

  @Test
  void conMinioDeberiaUsarElAdapterDeMinioYCrearLosBucketsAlArrancar() throws Exception {
    assertInstanceOf(MinioStorageAdapter.class, storage);
    assertTrue(
        minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(almacenamiento.bucketPadrones()).build()));
    assertTrue(
        minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(almacenamiento.bucketFotos()).build()));
  }

  @Test
  void unPadronSubidoPorMultipartDeberiaGuardarseEnMinioEImportarse() throws Exception {
    byte[] csv =
        ("﻿TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
                + "HUMANA,DNI,90000001,Prueba Minio,prueba.minio@example.com,+54 11 0000-0000\n")
            .getBytes(StandardCharsets.UTF_8);

    String respuesta =
        mockMvc
            .perform(
                multipart("/api/donantes/archivos")
                    .file(new MockMultipartFile("archivo", "padron.csv", "text/csv", csv)))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode archivo = objectMapper.readTree(respuesta);
    UUID archivoId = UUID.fromString(archivo.get("id").asText());
    String clave = archivo.get("path").asText();

    try (InputStream guardado = storage.abrir(almacenamiento.bucketPadrones(), clave)) {
      assertArrayEquals(csv, guardado.readAllBytes());
    }
    // ERROR sería no poder leer el padrón desde MinIO. Sin RabbitMQ en este test, el alta de la
    // persona falla al notificar y la fila cuenta como error de negocio: el padrón sí se leyó.
    EstadoArchivo estado = esperarFinDeImportacion(archivoId);
    assertTrue(
        estado == EstadoArchivo.PROCESADO || estado == EstadoArchivo.PROCESADO_CON_ERRORES,
        "Estado final: " + estado);
  }

  @Test
  void unaFotoSubidaConLaUrlPrefirmadaDeberiaValidarseYMostrarseConUrlDeDescarga()
      throws Exception {
    String respuesta =
        mockMvc
            .perform(
                post("/api/donaciones/fotos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tipoContenido\":\"image/png\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode subida = objectMapper.readTree(respuesta);
    String referencia = subida.get("referencia").asText();
    byte[] foto = {(byte) 0x89, 'P', 'N', 'G'};

    HttpResponse<Void> put =
        http.send(
            HttpRequest.newBuilder(URI.create(subida.get("uploadUrl").asText()))
                .header("Content-Type", subida.get("tipoContenido").asText())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(foto))
                .build(),
            HttpResponse.BodyHandlers.discarding());
    assertEquals(200, put.statusCode());

    fotosBienes.validarFoto(referencia);
    HttpResponse<byte[]> descarga =
        http.send(
            HttpRequest.newBuilder(URI.create(fotosBienes.urlParaMostrar(referencia))).build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, descarga.statusCode());
    assertArrayEquals(foto, descarga.body());
  }

  private EstadoArchivo esperarFinDeImportacion(UUID archivoId) {
    Instant limite = Instant.now().plus(ESPERA_IMPORTACION);
    EstadoArchivo estado = archivos.findById(archivoId).map(Archivo::getEstado).orElseThrow();
    while ((estado == EstadoArchivo.PENDIENTE || estado == EstadoArchivo.PROCESANDO)
        && Instant.now().isBefore(limite)) {
      LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
      estado = archivos.findById(archivoId).map(Archivo::getEstado).orElseThrow();
    }
    return estado;
  }
}
