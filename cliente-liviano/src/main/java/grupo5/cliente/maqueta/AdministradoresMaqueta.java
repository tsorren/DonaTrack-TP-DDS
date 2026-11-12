package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Las cuentas de administración de ejemplo (R11-10): tres, la propia (Ana Pérez) primero. Guardar
 * no cambia nada. {@code ?escenario=error} simula que el proveedor de ingreso no responde y {@code
 * ?escenario=vacio}, la cuenta propia sin nombre cargado (el admin del seed). En el alta, el correo
 * {@code usado@ejemplo.org} ya tiene cuenta y {@code falla@ejemplo.org} simula un alta que no
 * termina. También es la cuenta de administración de la sesión de la maqueta.
 */
@Component
@Profile("maqueta")
class AdministradoresMaqueta implements DirectorioDeAdministradores, AdministradorActual {

  static final String PROPIA = "cuenta-ana";

  private static final LocalDate CREADA = LocalDate.of(2026, 11, 12);
  private static final List<Administrador> CUENTAS =
      List.of(
          new Administrador(PROPIA, "Ana", "Pérez", "ana.perez@ejemplo.org", CREADA, true),
          new Administrador(
              "cuenta-bruno", "Bruno", "Díaz", "bruno.diaz@ejemplo.org", CREADA, false),
          new Administrador(
              "cuenta-carla", "Carla", "Gómez", "carla.gomez@ejemplo.org", CREADA, true));

  @Override
  public Optional<String> cuenta(HttpServletRequest pedido) {
    return Optional.of(PROPIA);
  }

  @Override
  public long total() {
    fallaSiHayError();
    return CUENTAS.size();
  }

  @Override
  public List<Administrador> pagina(int pagina) {
    fallaSiHayError();
    return pagina == 1 ? CUENTAS : List.of();
  }

  @Override
  public Optional<Administrador> cuenta(String id) {
    fallaSiHayError();
    if (PROPIA.equals(id) && Escenario.actual() == Escenario.VACIO) {
      return Optional.of(
          new Administrador(PROPIA, null, null, "ana.perez@ejemplo.org", CREADA, true));
    }
    return CUENTAS.stream().filter(c -> c.id().equals(id)).findFirst();
  }

  @Override
  public void cambiarNombre(String id, String nombre, String apellido) {
    fallaSiHayError();
  }

  @Override
  public String alta(Alta alta, String contrasena, String altaPor) {
    fallaSiHayError();
    return switch (alta.correo()) {
      case "usado@ejemplo.org" -> throw new CorreoEnUso();
      case "falla@ejemplo.org" -> throw new AltaIncompleta(new RestClientException("ejemplo"));
      default -> "cuenta-nueva";
    };
  }

  @Override
  public boolean otraContrasena(String id, String contrasena) {
    fallaSiHayError();
    return cuenta(id).filter(c -> !c.entro()).isPresent();
  }

  private static void fallaSiHayError() {
    if (Escenario.actual() == Escenario.ERROR) {
      throw new RestClientException("El proveedor de ingreso no responde (ejemplo)");
    }
  }
}
