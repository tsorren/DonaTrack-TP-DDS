package grupo5.cliente.rest;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.web.client.RestClientException;

/**
 * Quién hizo cada cambio del historial de una donación, como lo lee el admin (R11-10, ficha
 * docs/interfaz/flujos/r11-p1-admins.md, C8). Donaciones guarda la cuenta (el {@code sub} del
 * token); acá se cambia por el nombre de esa cuenta, o su correo si no tiene nombre cargado. Un
 * cambio sin autor ("SISTEMA" o vacío) no dice quién; un texto viejo que no es una cuenta se
 * muestra como está. Si el proveedor de ingreso no responde o la cuenta ya no existe, dice "una
 * cuenta de administración". Cada cuenta se busca una sola vez por detalle.
 */
final class AutoresDelHistorial {

  static final String SIN_NOMBRE = "una cuenta de administración";
  private static final String SISTEMA = "SISTEMA";
  private static final int LARGO_DE_CUENTA = 36;

  private final Function<String, Optional<Administrador>> buscar;
  private final Map<String, String> vistos = new HashMap<>();

  private AutoresDelHistorial(Function<String, Optional<Administrador>> buscar) {
    this.buscar = buscar;
  }

  /** Con el directorio de cuentas; sin directorio, las cuentas se muestran sin nombre. */
  static AutoresDelHistorial con(DirectorioDeAdministradores directorio) {
    return new AutoresDelHistorial(
        directorio == null ? cuenta -> Optional.empty() : directorio::cuenta);
  }

  /** El autor para mostrar; vacío si el cambio no tiene uno. */
  String de(String actor) {
    if (actor == null || actor.isBlank() || SISTEMA.equals(actor.strip())) {
      return "";
    }
    String limpio = actor.strip();
    if (!esCuenta(limpio)) {
      return limpio;
    }
    return vistos.computeIfAbsent(limpio, this::nombreDe);
  }

  private String nombreDe(String cuenta) {
    try {
      return buscar.apply(cuenta).map(AutoresDelHistorial::visible).orElse(SIN_NOMBRE);
    } catch (RestClientException e) {
      return SIN_NOMBRE;
    }
  }

  private static String visible(Administrador cuenta) {
    String nombre = cuenta.nombreCompleto();
    if (nombre != null && !nombre.isBlank()) {
      return nombre;
    }
    return cuenta.correo() == null || cuenta.correo().isBlank() ? SIN_NOMBRE : cuenta.correo();
  }

  private static boolean esCuenta(String actor) {
    if (actor.length() != LARGO_DE_CUENTA) {
      return false;
    }
    try {
      UUID.fromString(actor);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
