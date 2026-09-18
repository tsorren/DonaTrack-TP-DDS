package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class ControllerLoggingInterceptorTest {

  private final ControllerLoggingInterceptor interceptor = new ControllerLoggingInterceptor();

  static class DummyController {
    public void dummyMethod() {
      // no-op para reflejo en test
    }
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void preHandle_conHandlerMethod_deberiaProcesarYLimpiarMdc() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    DummyController controller = new DummyController();
    Method method = DummyController.class.getMethod("dummyMethod");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    assertNull(MDC.get("eventType"), "eventType debe removerse en el finally de preHandle");
    assertEquals("GET", MDC.get("httpMethod"));
    assertEquals("/api/test", MDC.get("endpoint"));

    interceptor.afterCompletion(request, response, handlerMethod, null);
    assertNull(MDC.get("httpMethod"), "httpMethod debe removerse en afterCompletion");
    assertNull(MDC.get("endpoint"), "endpoint debe removerse en afterCompletion");
  }

  @Test
  void preHandle_conHandlerNoMethod_deberiaRetornarTrueSinErrores() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/other");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNull(MDC.get("eventType"));
  }
}
