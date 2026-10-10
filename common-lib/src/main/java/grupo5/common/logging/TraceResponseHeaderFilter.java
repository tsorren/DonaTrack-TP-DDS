package grupo5.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceResponseHeaderFilter extends OncePerRequestFilter {

  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_TRACE_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = resolveTraceId(request);

    boolean mdcSetLocally = false;
    if (MDC.get(MDC_TRACE_KEY) == null) {
      MDC.put(MDC_TRACE_KEY, traceId);
      mdcSetLocally = true;
    }

    response.setHeader(TRACE_HEADER, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      if (mdcSetLocally) {
        MDC.remove(MDC_TRACE_KEY);
      }
    }
  }

  private static String resolveTraceId(HttpServletRequest request) {
    String mdcTraceId = MDC.get(MDC_TRACE_KEY);
    if (mdcTraceId != null && !mdcTraceId.isBlank()) {
      return mdcTraceId;
    }

    String headerTraceId = request.getHeader(TRACE_HEADER);
    if (headerTraceId != null && !headerTraceId.isBlank()) {
      return headerTraceId;
    }

    String headerRequestId = request.getHeader(REQUEST_ID_HEADER);
    if (headerRequestId != null && !headerRequestId.isBlank()) {
      return headerRequestId;
    }

    return UUID.randomUUID().toString().replace("-", "");
  }
}
