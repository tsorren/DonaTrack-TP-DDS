package grupo5.cliente.maqueta;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import grupo5.cliente.web.Pantallas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Las páginas de error propias (RF-CL-08, docs/interfaz/flujos/raices-paginas-de-error.md): {@code
 * ?respuesta=404}, {@code 403} o {@code 500} en cualquier pantalla responde con ese código, y
 * Spring Boot muestra templates/error/404, 403 o 5xx, antes de que el controller llame a una API.
 */
final class RespuestaDeEjemplo implements HandlerInterceptor {

  private static final Map<String, HttpStatus> RESPUESTAS =
      Map.of("404", NOT_FOUND, "403", FORBIDDEN, "500", INTERNAL_SERVER_ERROR);

  @Override
  public boolean preHandle(
      HttpServletRequest pedido, HttpServletResponse respuesta, Object controller) {
    HttpStatus estado = RESPUESTAS.get(String.valueOf(pedido.getParameter("respuesta")));
    if (estado != null && pedido.getRequestURI().matches("/" + Pantallas.NOMBRE)) {
      throw new ResponseStatusException(estado, "Respuesta de ejemplo");
    }
    return true;
  }
}
