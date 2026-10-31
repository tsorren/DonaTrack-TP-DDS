package grupo5.donaciones.infrastructure.almacenamiento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.ports.StoragePort.AlmacenamientoException;
import grupo5.donaciones.models.ports.StoragePort.Metadatos;
import grupo5.donaciones.models.ports.StoragePort.Operacion;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StorageEnMemoriaTest {

  private static final String BUCKET = "donaciones-padrones";
  private final StoragePort storage = new StorageEnMemoria();

  @Test
  void deberiaDevolverLoGuardadoConSusMetadatos() throws Exception {
    byte[] contenido = "DNI,NOMBRE\n1,Ana\n".getBytes(StandardCharsets.UTF_8);

    storage.guardar(
        BUCKET, "padron.csv", new ByteArrayInputStream(contenido), contenido.length, "text/csv");

    try (InputStream leido = storage.abrir(BUCKET, "padron.csv")) {
      assertArrayEquals(contenido, leido.readAllBytes());
    }
    assertEquals(
        new Metadatos(contenido.length, "text/csv"),
        storage.metadatos(BUCKET, "padron.csv").orElseThrow());
  }

  @Test
  void unObjetoInexistenteNoDeberiaTenerMetadatosNiPoderAbrirse() {
    assertTrue(storage.metadatos(BUCKET, "no-existe.csv").isEmpty());
    assertThrows(AlmacenamientoException.class, () -> storage.abrir(BUCKET, "no-existe.csv"));
  }

  @Test
  void laMismaClaveEnOtroBucketEsOtroObjeto() {
    storage.guardar(BUCKET, "a.csv", new ByteArrayInputStream(new byte[] {1}), 1, "text/csv");

    assertTrue(storage.metadatos("otro-bucket", "a.csv").isEmpty());
  }

  @Test
  void laUrlTemporalDeberiaIdentificarElObjeto() {
    assertEquals(
        "memoria://donaciones-fotos-bienes/foto.jpg",
        storage.urlTemporal(
            "donaciones-fotos-bienes", "foto.jpg", Duration.ofMinutes(5), Operacion.DESCARGAR));
  }

  @Test
  void deberiaRechazarUnaClaveQueEscapaDelBucket() {
    ByteArrayInputStream contenido = new ByteArrayInputStream(new byte[] {1});

    assertThrows(
        IllegalArgumentException.class,
        () -> storage.guardar(BUCKET, "../otro-bucket/a.csv", contenido, 1, "text/csv"));
    assertThrows(IllegalArgumentException.class, () -> storage.abrir(BUCKET, "../a.csv"));
  }
}
