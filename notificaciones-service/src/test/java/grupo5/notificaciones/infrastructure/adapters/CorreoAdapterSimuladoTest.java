package grupo5.notificaciones.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorreoAdapterSimuladoTest {

  private CriterioFalloSimulado criterioFallo;
  private CorreoAdapterSimulado adapter;

  @BeforeEach
  void setUp() {
    criterioFallo = mock(CriterioFalloSimulado.class);
    adapter =
        new CorreoAdapterSimulado(criterioFallo) {
          @Override
          protected void simularLatenciaDeRed() {
            // no-op para tests rápidos
          }
        };
  }

  @Test
  @DisplayName("enviarMail retorna true cuando la política no indica fallo")
  void enviarMail_exitoso_retornaTrue() {
    String destinatario = "donante@test.com";
    String mensaje = "Hola donante";
    when(criterioFallo.debeFallar(destinatario, mensaje)).thenReturn(false);

    boolean resultado = adapter.enviarMail(destinatario, mensaje);

    assertTrue(resultado);
    verify(criterioFallo).debeFallar(destinatario, mensaje);
  }

  @Test
  @DisplayName("enviarMail retorna false cuando la política indica fallo simulado")
  void enviarMail_conFalloSimulado_retornaFalse() {
    String destinatario = "fallo@test.com";
    String mensaje = "Hola donante";
    when(criterioFallo.debeFallar(destinatario, mensaje)).thenReturn(true);

    boolean resultado = adapter.enviarMail(destinatario, mensaje);

    assertFalse(resultado);
    verify(criterioFallo).debeFallar(destinatario, mensaje);
  }

  @Test
  @DisplayName("simularLatenciaDeRed ejecuta sin error y restaura interrupción")
  void metodosDeSimulacion_ejecutanCorrectamente() {
    CorreoAdapterSimulado realAdapter = new CorreoAdapterSimulado(criterioFallo);

    assertDoesNotThrow(realAdapter::simularLatenciaDeRed);

    Thread.currentThread().interrupt();
    try {
      realAdapter.simularLatenciaDeRed();
      assertTrue(Thread.interrupted());
    } finally {
      Thread.interrupted();
    }
  }
}
