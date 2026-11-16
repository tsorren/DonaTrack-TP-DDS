package grupo5.cliente.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * El marco de las páginas de error (docs/interfaz/flujos/raices-paginas-de-error.md, R11-05): con
 * la sesión iniciada, Spring Boot las arma con el marco privado del rol y "Volver a mi inicio"; sin
 * ella, con el público. Solo agrega al modelo de {@code /error}; {@link Pantallas} decide el marco.
 */
@ControllerAdvice(assignableTypes = BasicErrorController.class)
class MarcoDeLasPaginasDeError {

  private final ObjectProvider<Pantallas> pantallas;

  MarcoDeLasPaginasDeError(ObjectProvider<Pantallas> pantallas) {
    this.pantallas = pantallas;
  }

  @ModelAttribute
  void marco(HttpServletRequest pedido, Model model) {
    Pantallas disponibles = pantallas.getIfAvailable();
    if (disponibles != null) {
      disponibles.conMarcoDeLaCuenta(pedido, model);
    }
  }
}
