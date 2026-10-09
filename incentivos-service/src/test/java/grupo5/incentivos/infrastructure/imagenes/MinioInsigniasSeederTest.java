package grupo5.incentivos.infrastructure.imagenes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.factory.MisionFactory;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Usa las imágenes reales de {@code src/main/resources/insignias} y un MinioClient mockeado. */
class MinioInsigniasSeederTest {

  private static final Set<String> IMAGENES_ESPERADAS =
      Set.of(
          "racha-colaborador.png",
          "explorador.png",
          "gran-aporte.png",
          "constancia.png",
          "impacto-real.png",
          "diversidad.png");

  private final MinioClient client = mock(MinioClient.class);
  private final MinioInsigniasSeeder seeder = new MinioInsigniasSeeder(client, "insignias");

  @Test
  void deberiaCrearElBucketAplicarLaPoliticaYSubirLasSeisImagenes() throws Exception {
    when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    doThrow(objetoInexistente()).when(client).statObject(any(StatObjectArgs.class));

    seeder.sembrar();

    verify(client).makeBucket(any(MakeBucketArgs.class));
    verify(client).setBucketPolicy(any(SetBucketPolicyArgs.class));
    assertEquals(IMAGENES_ESPERADAS, clavesSubidas(6));
  }

  @Test
  void noDeberiaRecrearNiResubirNadaSiTodoYaExiste() throws Exception {
    when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    // statObject sin stub devuelve null: el objeto "existe".

    seeder.sembrar();

    verify(client, never()).makeBucket(any(MakeBucketArgs.class));
    verify(client, never()).putObject(any(PutObjectArgs.class));
    verify(client).setBucketPolicy(any(SetBucketPolicyArgs.class));
  }

  @Test
  void deberiaSubirSoloLasImagenesQueFaltan() throws Exception {
    when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    doThrow(objetoInexistente())
        .when(client)
        .statObject(argThat(args -> "explorador.png".equals(args.object())));

    seeder.sembrar();

    assertEquals(Set.of("explorador.png"), clavesSubidas(1));
  }

  @Test
  void noDeberiaPropagarElErrorSiMinioNoResponde() throws Exception {
    when(client.bucketExists(any(BucketExistsArgs.class))).thenThrow(new IOException("sin MinIO"));

    assertDoesNotThrow(() -> seeder.run(null));
    verify(client, never()).putObject(any(PutObjectArgs.class));
  }

  @Test
  void lasImagenesSembradasCubrenTodasLasInsigniasDeLasMisionesEstandar() throws Exception {
    when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    doThrow(objetoInexistente()).when(client).statObject(any(StatObjectArgs.class));
    seeder.sembrar();
    Set<String> sembradas = clavesSubidas(6);

    List<Mision> misiones = MisionFactory.crearMisionesEstandar();

    assertEquals(6, misiones.size());
    for (Mision mision : misiones) {
      String referencia = mision.getInsignia().imagenUrl();
      assertTrue(
          referencia.startsWith(MinioImagenesInsigniasAdapter.PREFIJO_REFERENCIA), referencia);
      String clave =
          referencia.substring(MinioImagenesInsigniasAdapter.PREFIJO_REFERENCIA.length());
      assertTrue(sembradas.contains(clave), "Falta la imagen sembrada para " + referencia);
    }
  }

  private Set<String> clavesSubidas(int cantidadEsperada) throws Exception {
    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(client, times(cantidadEsperada)).putObject(captor.capture());
    return captor.getAllValues().stream().map(PutObjectArgs::object).collect(Collectors.toSet());
  }

  private static ErrorResponseException objetoInexistente() {
    ErrorResponse error = mock(ErrorResponse.class);
    when(error.code()).thenReturn("NoSuchKey");
    ErrorResponseException excepcion = mock(ErrorResponseException.class);
    when(excepcion.errorResponse()).thenReturn(error);
    return excepcion;
  }
}
