package grupo5.cliente.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cada pedido del navegador recibe un {@code traceId} nuevo (ADR-E6-02: lo genera el cliente, con
 * el formato de common-lib). Queda en el MDC para los logs y las llamadas a las APIs, en el request
 * para el código de soporte de las páginas de error, y en la respuesta.
 */
@Component
public class TraceIdFiltro extends OncePerRequestFilter {

  public static final String CABECERA = "X-Trace-Id";
  public static final String CLAVE = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().replace("-", "");
    pedido.setAttribute(CLAVE, traceId);
    respuesta.setHeader(CABECERA, traceId);
    MDC.put(CLAVE, traceId);
    try {
      cadena.doFilter(pedido, respuesta);
    } finally {
      MDC.remove(CLAVE);
    }
  }
}
