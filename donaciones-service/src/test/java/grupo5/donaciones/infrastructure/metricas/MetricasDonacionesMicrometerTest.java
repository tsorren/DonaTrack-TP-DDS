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

  private double registradas(String perecedera) {
    var contador =
        registro.find("donatrack.donaciones.registradas").tag("perecedera", perecedera).counter();
    return contador == null ? 0 : contador.count();
  }
}
