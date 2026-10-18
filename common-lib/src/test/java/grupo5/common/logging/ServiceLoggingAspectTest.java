package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ServiceLoggingAspectTest {

  private final ServiceLoggingAspect aspect = new ServiceLoggingAspect();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void logServiceMethodSuccess_deberiaRegistrarYLimpiarMdc() {
    JoinPoint joinPoint = mock(JoinPoint.class);
    Signature signature = mock(Signature.class);
    when(signature.getName()).thenReturn("calcularPuntos");
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getTarget()).thenReturn(new Object());

    aspect.logServiceMethodSuccess(joinPoint, "resultadoOK");

    assertNull(MDC.get("eventType"), "eventType debe removerse en el bloque finally");
  }

  @Test
  void logServiceMethodError_deberiaRegistrarYLimpiarMdc() {
    JoinPoint joinPoint = mock(JoinPoint.class);
    Signature signature = mock(Signature.class);
    when(signature.getName()).thenReturn("procesarDonacion");
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.getTarget()).thenReturn(new Object());

    aspect.logServiceMethodError(joinPoint, new RuntimeException("Error simulado"));

    assertNull(MDC.get("eventType"), "eventType debe removerse en el bloque finally");
  }
}
