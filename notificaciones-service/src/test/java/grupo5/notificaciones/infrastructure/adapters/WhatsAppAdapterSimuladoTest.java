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

class WhatsAppAdapterSimuladoTest {

  private CriterioFalloSimulado criterioFallo;
  private WhatsAppAdapterSimulado adapter;

  @BeforeEach
  void setUp() {
    criterioFallo = mock(CriterioFalloSimulado.class);
    adapter = new WhatsAppAdapterSimulado(criterioFallo);
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
}
