package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DomainServicesConfigTest {

  @Test
  void domainServicesConfig_deberiaProveerBeansNoNulos() {
    DomainServicesConfig config = new DomainServicesConfig();

    assertNotNull(config.gestorDeInactivos());
    assertNotNull(config.gestorDeRankings());
  }
}
