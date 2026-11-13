package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.vistas.DonantePerfilVista;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * El perfil de la empresa de ejemplo, el que dibujaba la plantilla, o el de la persona de ejemplo
 * si la sesión eligió {@code ?cuenta=persona} ({@link CuentaDeEjemplo}). La cuenta de la maqueta es
 * una empresa: la versión de persona la elige {@link CuentaDeEjemplo}. Guardar no cambia nada.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonantePerfilMaqueta implements DonantePerfilPuerto {

  private static final String CIUDAD = "Ciudad de Buenos Aires";

  private static final DonantePerfilVista EMPRESA =
      new DonantePerfilVista(
          "Distribuidora Ejemplo SA",
          "Empresa donante · contacto@distribuidora-ejemplo.test",
          "Empresa donante · Sostenedor",
          false,
          "Distribución de alimentos",
          "30-00000000-0",
          "Avenida Ejemplo",
          "1234",
          "3.º B",
          "C1000",
          CIUDAD,
          CIUDAD,
          "contacto@distribuidora-ejemplo.test",
          "11 5555-0100",
          null,
          "Aceptaste la versión 1 el 1 de noviembre de 2026.",
          null,
          null,
          null);

  private static final DonantePerfilVista PERSONA =
      new DonantePerfilVista(
          "Martina Ruiz",
          "Persona donante · martina.ruiz@correo-ejemplo.test",
          "Donante Sostenedor",
          false,
          null,
          null,
          "Calle Ejemplo",
          "560",
          null,
          "C1406",
          "Caballito",
          CIUDAD,
          "martina.ruiz@correo-ejemplo.test",
          "11 5555-0142",
          null,
          "Aceptaste la versión 1 el 1 de noviembre de 2026.",
          "Martina",
          "Ruiz",
          "DNI 00.000.000");

  @Override
  public DonantePerfilVista perfil(UUID donante) {
    return esPersona() ? PERSONA : EMPRESA;
  }

  @Override
  public Tipo tipo(UUID donante) {
    return Tipo.EMPRESA;
  }

  @Override
  public void mostrarNombre(UUID donante, boolean mostrar) {
    // La maqueta no guarda: el formulario lo atiende SitioController.
  }

  /** La sesión eligió la persona de ejemplo; se lee igual que en {@link CuentaDeEjemplo}. */
  private static boolean esPersona() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido)) {
      return false;
    }
    String elegida = pedido.getRequest().getParameter("cuenta");
    if (elegida != null) {
      return "persona".equals(elegida);
    }
    HttpSession sesion = pedido.getRequest().getSession(false);
    return sesion != null && "persona".equals(sesion.getAttribute("cuenta"));
  }
}
