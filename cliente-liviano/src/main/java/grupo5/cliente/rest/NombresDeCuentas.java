package grupo5.cliente.rest;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.client.RestClientException;

/**
 * Los nombres de las cuentas de una página del registro de actividad (R11-26), leídos del proveedor
 * de ingreso al mostrar, como {@link AutoresDelHistorial}: cada cuenta distinta se lee una sola vez
 * por pedido. El nombre y apellido; sin nombre cargado, el correo. Si el proveedor no responde, no
 * se le vuelve a preguntar en este pedido y las cuentas quedan sin nombre.
 */
final class NombresDeCuentas {

  /** Lo que se sabe de una cuenta. */
  enum Estado {
    CONOCIDA,
    NO_EXISTE,
    SIN_RESPUESTA
  }

  /** {@code nombre}: solo si es CONOCIDA. */
  record Cuenta(String nombre, Estado estado) {}

  private static final Cuenta SIN_RESPUESTA = new Cuenta(null, Estado.SIN_RESPUESTA);
  private static final Cuenta NO_EXISTE = new Cuenta(null, Estado.NO_EXISTE);

  private final DirectorioDeAdministradores directorio;
  private final Map<String, Cuenta> vistas = new HashMap<>();
  private boolean sinRespuesta;

  NombresDeCuentas(DirectorioDeAdministradores directorio) {
    this.directorio = directorio;
  }

  /** Lo que se sabe de {@code cuenta}; null o en blanco, una cuenta que no existe. */
  Cuenta de(String cuenta) {
    if (cuenta == null || cuenta.isBlank()) {
      return NO_EXISTE;
    }
    Cuenta vista = vistas.get(cuenta);
    if (vista != null) {
      return vista;
    }
    if (sinRespuesta) {
      return SIN_RESPUESTA;
    }
    Cuenta leida;
    try {
      leida = leer(directorio.cuenta(cuenta));
    } catch (RestClientException noRespondio) {
      sinRespuesta = true;
      return SIN_RESPUESTA;
    }
    vistas.put(cuenta, leida);
    return leida;
  }

  /** Si el proveedor de ingreso dejó de responder en este pedido. */
  boolean sinRespuesta() {
    return sinRespuesta;
  }

  /** Lo anota como que el proveedor no respondió (la búsqueda por nombre ya falló). */
  void noResponde() {
    sinRespuesta = true;
  }

  private static Cuenta leer(Optional<Administrador> cuenta) {
    return cuenta
        .map(
            c -> {
              String nombre = c.nombreCompleto();
              if (nombre == null && c.correo() != null && !c.correo().isBlank()) {
                nombre = c.correo();
              }
              return nombre == null ? NO_EXISTE : new Cuenta(nombre, Estado.CONOCIDA);
            })
        .orElse(NO_EXISTE);
  }
}
