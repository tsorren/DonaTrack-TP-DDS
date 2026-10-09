package grupo5.incentivos.infrastructure.imagenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MinioImagenesInsigniasAdapterTest {

  private final MinioImagenesInsigniasAdapter adapter =
      new MinioImagenesInsigniasAdapter("http://localhost:9000", "insignias");

  @Test
  void deberiaResolverLaReferenciaDeInsigniaALaUrlPublicaDelBucket() {
    assertEquals(
        "http://localhost:9000/insignias/explorador.png",
        adapter.urlPublica("/insignias/explorador.png"));
  }

  @Test
  void deberiaIgnorarLaBarraFinalDeLaUrlBase() {
    MinioImagenesInsigniasAdapter conBarra =
        new MinioImagenesInsigniasAdapter("https://cdn.donatrack.org/", "insignias");

    assertEquals(
        "https://cdn.donatrack.org/insignias/gran-aporte.png",
        conBarra.urlPublica("/insignias/gran-aporte.png"));
  }

  @Test
  void deberiaSerIdempotenteConUrlsYaAbsolutas() {
    String yaResuelta = "http://localhost:9000/insignias/explorador.png";

    assertEquals(yaResuelta, adapter.urlPublica(yaResuelta));
    assertEquals(yaResuelta, adapter.urlPublica(adapter.urlPublica("/insignias/explorador.png")));
  }

  @Test
  void deberiaConvertirLaUrlPublicaDeVueltaALaReferenciaParaPersistir() {
    assertEquals(
        "/insignias/explorador.png",
        adapter.referencia("http://localhost:9000/insignias/explorador.png"));
    assertEquals("/insignias/explorador.png", adapter.referencia("/insignias/explorador.png"));
    assertEquals(
        "http://otro-host/insignias/x.png", adapter.referencia("http://otro-host/insignias/x.png"));
    assertNull(adapter.referencia(null));
  }

  @Test
  void deberiaDejarSinCambiosLosValoresQueNoSonReferenciasDeInsignia() {
    assertEquals("/icon.png", adapter.urlPublica("/icon.png"));
    assertEquals("http://img.png", adapter.urlPublica("http://img.png"));
    assertNull(adapter.urlPublica(null));
  }
}
