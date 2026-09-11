package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InactividadConfigTest {

  @Test
  void inactividadConfig_deberiaProveerCriterioNoNulo() {
    InactividadConfig config = new InactividadConfig();

    assertNotNull(config.inactividadPorDonaciones(30));
  }
}
