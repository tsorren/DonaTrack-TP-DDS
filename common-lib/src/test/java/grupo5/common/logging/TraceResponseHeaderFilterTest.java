package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceResponseHeaderFilterTest {

  private TraceResponseHeaderFilter filter;

  @BeforeEach
  void setUp() {
    filter = new TraceResponseHeaderFilter();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void doFilter_conHeaderExistente_deberiaPropagarloEnResponse()
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Trace-Id", "incoming-trace-abc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilter(request, response, filterChain);

    assertEquals("incoming-trace-abc", response.getHeader("X-Trace-Id"));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_sinHeader_deberiaGenerarNuevoTraceIdYSetearloEnResponse()
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilter(request, response, filterChain);

    assertNotNull(response.getHeader("X-Trace-Id"));
    assertEquals(32, response.getHeader("X-Trace-Id").length());
    verify(filterChain).doFilter(request, response);
  }
}
