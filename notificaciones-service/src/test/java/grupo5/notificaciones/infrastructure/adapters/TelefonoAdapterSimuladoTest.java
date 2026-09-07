package grupo5.notificaciones.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    adapter = new TelefonoAdapterSimulado(criterioFallo);
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
}
