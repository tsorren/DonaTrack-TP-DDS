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

class TelefonoAdapterSimuladoTest {

  private CriterioFalloSimulado criterioFallo;
  private TelefonoAdapterSimulado adapter;

  @BeforeEach
  void setUp() {
    criterioFallo = mock(CriterioFalloSimulado.class);
    adapter =
        new TelefonoAdapterSimulado(criterioFallo) {
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
  @DisplayName("enviarSms retorna true cuando la política no indica fallo")
  void enviarSms_exitoso_retornaTrue() {
    String telefono = "+541155556666";
    String mensaje = "Mensaje de prueba";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(false);

    boolean resultado = adapter.enviarSms(telefono, mensaje);

    assertTrue(resultado);
    verify(criterioFallo).debeFallar(telefono, mensaje);
  }

  @Test
  @DisplayName("enviarSms retorna false cuando la política indica fallo simulado")
  void enviarSms_conFalloSimulado_retornaFalse() {
    String telefono = "+540000000000";
    String mensaje = "Mensaje de prueba";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(true);

    boolean resultado = adapter.enviarSms(telefono, mensaje);

    assertFalse(resultado);
    verify(criterioFallo).debeFallar(telefono, mensaje);
  }

  @Test
  @DisplayName(
      "enviarSms lanza ProveedorMensajeriaException cuando ocurre fallo temporal aleatorio")
  void enviarSms_conFalloTemporalAleatorio_lanzaExcepcion() {
    String telefono = "+541155556666";
    String mensaje = "Mensaje de prueba";
    when(criterioFallo.debeFallar(telefono, mensaje)).thenReturn(false);

    TelefonoAdapterSimulado adapterConFalloTemporal =
        new TelefonoAdapterSimulado(criterioFallo) {
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
        () -> adapterConFalloTemporal.enviarSms(telefono, mensaje));
  }

  @Test
  @DisplayName(
      "simularFalloTemporalAleatorio y simularLatenciaDeRed ejecutan sin error y restauran interrupción")
  void metodosDeSimulacion_ejecutanCorrectamente() {
    TelefonoAdapterSimulado realAdapter = new TelefonoAdapterSimulado(criterioFallo);

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
