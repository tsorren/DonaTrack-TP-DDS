package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InstanceIdPropertyDefinerTest {

  @AfterEach
  void tearDown() {
    System.clearProperty("instance.id");
  }

  @Test
  void getPropertyValue_deberiaRetornarValorPorDefectoValido() {
    InstanceIdPropertyDefiner definer = new InstanceIdPropertyDefiner();
    String value = definer.getPropertyValue();

    assertNotNull(value);
    assertFalse(value.trim().isEmpty());
  }

  @Test
  void getPropertyValue_deberiaPriorizarSystemPropertySiExiste() {
    System.setProperty("instance.id", "custom-instance-42");
    InstanceIdPropertyDefiner definer = new InstanceIdPropertyDefiner();

    assertEquals("custom-instance-42", definer.getPropertyValue());
  }
}
