package grupo5.common.logging;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.UUID;
import org.slf4j.MDC;

public class FeignTraceRequestInterceptor implements RequestInterceptor {

  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String MDC_TRACE_KEY = "traceId";

  @Override
  public void apply(RequestTemplate template) {
    String traceId = MDC.get(MDC_TRACE_KEY);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString().replace("-", "");
    }
    template.header(TRACE_HEADER, traceId);
  }
}
