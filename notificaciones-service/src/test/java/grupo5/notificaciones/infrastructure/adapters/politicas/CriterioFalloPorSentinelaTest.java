package grupo5.notificaciones.infrastructure.adapters.politicas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CriterioFalloPorSentinelaTest {

  private CriterioFalloPorSentinela criterio;

  @BeforeEach
  void setUp() {
    criterio = new CriterioFalloPorSentinela();
  }

  @Test
  @DisplayName("Debe fallar cuando el destinatario es nulo o vacío")
  void debeFallar_conDestinatarioNuloOVacio_retornaTrue() {
    assertTrue(criterio.debeFallar(null, "Mensaje válido"));
    assertTrue(criterio.debeFallar("", "Mensaje válido"));
    assertTrue(criterio.debeFallar("   ", "Mensaje válido"));
  }

  @Test
  @DisplayName("Debe fallar cuando el mensaje es nulo o vacío")
  void debeFallar_conMensajeNuloOVacio_retornaTrue() {
    assertTrue(criterio.debeFallar("test@donatrack.com", null));
    assertTrue(criterio.debeFallar("test@donatrack.com", ""));
    assertTrue(criterio.debeFallar("test@donatrack.com", "   "));
  }

  @Test
  @DisplayName("Debe fallar cuando el destinatario contiene la palabra 'fallo' o 'error'")
  void debeFallar_conSentinelaFalloOError_retornaTrue() {
    assertTrue(criterio.debeFallar("fallo@donatrack.com", "Mensaje"));
    assertTrue(criterio.debeFallar("usuario_error@donatrack.com", "Mensaje"));
    assertTrue(criterio.debeFallar("ERROR_CRITICO", "Mensaje"));
  }

  @Test
  @DisplayName("Debe fallar cuando el teléfono contiene el sentinela de ceros")
  void debeFallar_conSentinelaTelefonoInvalido_retornaTrue() {
    assertTrue(criterio.debeFallar("+540000000000", "Mensaje"));
  }

  @Test
  @DisplayName("Debe retornar false para datos válidos sin sentinelas")
  void debeFallar_conDatosValidos_retornaFalse() {
    assertFalse(criterio.debeFallar("donante@ejemplo.com", "Tu donación fue asignada"));
    assertFalse(criterio.debeFallar("+541155556666", "Tu donación está en camino"));
  }
}
