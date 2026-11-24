package grupo5.donaciones.infrastructure.metricas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.services.logistica.MotivoDescarteEvento;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MetricasDonacionesMicrometerTest {

  private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
  private final MetricasDonacionesMicrometer metricas = new MetricasDonacionesMicrometer(registro);

  @Test
  void donacionRegistrada_conBienPerecedero_cuentaConPerecederaSi() {
    var donacion =
        DonacionMother.conItem(UUID.randomUUID(), BienMother.alimentoPerecedero("Leche"), 1);

    metricas.donacionRegistrada(donacion);

    assertEquals(1, registradas("si"));
    assertEquals(0, registradas("no"));
  }

  @Test
  void donacionRegistrada_sinBienPerecedero_cuentaConPerecederaNo() {
    var donacion = DonacionMother.conItem(UUID.randomUUID(), BienMother.mueble("Mesa"), 1);

    metricas.donacionRegistrada(donacion);

    assertEquals(0, registradas("si"));
    assertEquals(1, registradas("no"));
  }

  @Test
  void entregaFallida_replanificable_cuentaConReplanificableSi() {
    metricas.entregaFallida(true);

    assertEquals(1, fallidas("si"));
    assertEquals(0, fallidas("no"));
  }

  @Test
  void entregaFallida_noReplanificable_cuentaConReplanificableNo() {
    metricas.entregaFallida(false);

    assertEquals(0, fallidas("si"));
    assertEquals(1, fallidas("no"));
  }

  @ParameterizedTest
  @CsvSource({
    "SIN_PROVEEDOR, sin_proveedor",
    "SIN_TOKEN_CONFIGURADO, sin_token_configurado",
    "TOKEN_INCORRECTO, token_incorrecto",
    "DONACION_AJENA, donacion_ajena"
  })
  void eventoDeProveedorDescartado_cuentaConElMotivoEnMinuscula(
      MotivoDescarteEvento motivo, String etiqueta) {
    metricas.eventoDeProveedorDescartado(motivo);

    assertEquals(1, descartados(etiqueta));
  }

  private double registradas(String perecedera) {
    return contados("donatrack.donaciones.registradas", "perecedera", perecedera);
  }

  private double fallidas(String replanificable) {
    return contados("donatrack.donaciones.entregas.fallidas", "replanificable", replanificable);
  }

  private double descartados(String motivo) {
    return contados("donatrack.donaciones.eventos.proveedor.descartados", "motivo", motivo);
  }

  private double contados(String metrica, String etiqueta, String valor) {
    var contador = registro.find(metrica).tag(etiqueta, valor).counter();
    return contador == null ? 0 : contador.count();
  }
}
