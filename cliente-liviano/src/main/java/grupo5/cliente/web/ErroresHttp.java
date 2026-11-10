package grupo5.cliente.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Un error de una API lleva a su página (RF-CL-08): 401 a Ingresar, 403 y 404 a las suyas, y
 * cualquier otra falla, o una API que no responde, a la de falla nuestra con 502. Spring Boot elige
 * templates/error/&lt;código&gt;; el código de soporte es el atributo {@code traceId} del pedido.
 */
@ControllerAdvice
class ErroresHttp {

  @ExceptionHandler(RestClientException.class)
  String falloUnaApi(RestClientException error, HttpServletResponse respuesta) throws IOException {
    int estado =
        error instanceof RestClientResponseException conRespuesta
            ? conRespuesta.getStatusCode().value()
            : 0;
    if (estado == HttpStatus.UNAUTHORIZED.value()) {
      return "redirect:/publico-ingresar";
    }
    boolean propia =
        estado == HttpStatus.FORBIDDEN.value() || estado == HttpStatus.NOT_FOUND.value();
    respuesta.sendError(propia ? estado : HttpStatus.BAD_GATEWAY.value());
    return null;
  }
}
