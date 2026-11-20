package grupo5.donaciones.infrastructure.metricas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetricasDonacionesMicrometerTest {

  private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
  private final MetricasDonacionesMicrometer metricas = new MetricasDonacionesMicrometer(registro);

  @Test
  void donacionRegistrada_conBienPerecedero_cuentaConPerecedaraSi() {
    var donacion =
        DonacionMother.conItem(UUID.randomUUID(), BienMother.alimentoPerecedero("Leche"), 1);

    metricas.donacionRegistrada(donacion);

    assertEquals(1, registradas("si"));
    assertEquals(0, registradas("no"));
  }

  @Test
  void donacionRegistrada_sinBienPerecedero_cuentaConPerecedaraNo() {
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

  private double registradas(String perecedera) {
    return contados("donatrack.donaciones.registradas", "perecedera", perecedera);
  }

  private double fallidas(String replanificable) {
    return contados("donatrack.donaciones.entregas.fallidas", "replanificable", replanificable);
  }

  private double contados(String metrica, String etiqueta, String valor) {
    var contador = registro.find(metrica).tag(etiqueta, valor).counter();
    return contador == null ? 0 : contador.count();
  }
}
