package grupo5.notificaciones.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.exceptions.ProveedorMensajeriaException;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhatsAppAdapterSimuladoTest {

  private CriterioFalloSimulado criterioFallo;
  private WhatsAppAdapterSimulado adapter;

  @BeforeEach
  void setUp() {
    criterioFallo = mock(CriterioFalloSimulado.class);
    adapter =
        new WhatsAppAdapterSimulado(criterioFallo) {
          @Override
          protected boolean simularFalloTemporalAleatorio() {
            return false;
          }

          @Override
          protected void simularLatenciaDeRed() {
            // no-op para tests
          }
        };
  }

  @Test
  @DisplayName("enviarWhatsApp retorna true cuando la política no indica fallo")
  void enviarWhatsApp_exitoso_retornaTrue() {
    String telefono = "+541199998888";
    String mensaje = "Mensaje whatsapp";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(false);

    boolean resultado = adapter.enviarWhatsApp(telefono, mensaje);

    assertTrue(resultado);
    verify(criterioFallo).debeFallar(telefono, mensaje);
  }

  @Test
  @DisplayName("enviarWhatsApp retorna false cuando la política indica fallo simulado")
  void enviarWhatsApp_conFalloSimulado_retornaFalse() {
    String telefono = "+540000000000";
    String mensaje = "Mensaje whatsapp";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(true);

    boolean resultado = adapter.enviarWhatsApp(telefono, mensaje);

    assertFalse(resultado);
    verify(criterioFallo).debeFallar(telefono, mensaje);
  }

  @Test
  @DisplayName(
      "enviarWhatsApp lanza ProveedorMensajeriaException cuando ocurre fallo temporal aleatorio")
  void enviarWhatsApp_conFalloTemporalAleatorio_lanzaExcepcion() {
    String telefono = "+541199998888";
    String mensaje = "Mensaje whatsapp";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(false);

    WhatsAppAdapterSimulado adapterConFalloTemporal =
        new WhatsAppAdapterSimulado(criterioFallo) {
          @Override
          protected boolean simularFalloTemporalAleatorio() {
            return true;
          }

          @Override
          protected void simularLatenciaDeRed() {
            // no-op para tests
          }
        };

    assertThrows(
        ProveedorMensajeriaException.class,
        () -> adapterConFalloTemporal.enviarWhatsApp(telefono, mensaje));
  }

  @Test
  @DisplayName(
      "simularFalloTemporalAleatorio y simularLatenciaDeRed ejecutan sin error y restauran interrupción")
  void metodosDeSimulacion_ejecutanCorrectamente() {
    WhatsAppAdapterSimulado realAdapter = new WhatsAppAdapterSimulado(criterioFallo);

    assertDoesNotThrow(realAdapter::simularFalloTemporalAleatorio);
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
