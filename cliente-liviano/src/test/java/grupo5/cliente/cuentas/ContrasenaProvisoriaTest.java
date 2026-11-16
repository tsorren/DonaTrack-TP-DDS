package grupo5.cliente.cuentas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContrasenaProvisoriaTest {

  @Test
  void son16CaracteresEnGruposDe4() {
    String contrasena = ContrasenaProvisoria.nueva();

    assertThat(contrasena).matches("[^-]{4}-[^-]{4}-[^-]{4}-[^-]{4}");
    assertThat(contrasena.replace("-", "")).hasSize(16);
  }

  @Test
  void noUsaCaracteresQueSeConfunden() {
    for (int i = 0; i < 200; i++) {
      assertThat(ContrasenaProvisoria.nueva()).doesNotContainPattern("[0O1lIio]");
    }
  }

  @Test
  void cumpleElLargoMinimoDelRealm() {
    assertThat(ContrasenaProvisoria.nueva().length()).isBetween(10, 128);
  }

  @Test
  void cadaUnaEsDistinta() {
    Set<String> generadas = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      generadas.add(ContrasenaProvisoria.nueva());
    }
    assertThat(generadas).hasSize(100);
  }
}
