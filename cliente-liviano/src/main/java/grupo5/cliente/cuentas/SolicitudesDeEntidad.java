package grupo5.cliente.cuentas;

import static java.util.stream.Collectors.toMap;

import grupo5.cliente.puertos.DirectorioDeCuentas;
import grupo5.cliente.puertos.DirectorioDeCuentas.SolicitudDeEntidad;
import grupo5.cliente.puertos.DonacionesComoUsuario;
import grupo5.cliente.puertos.DonacionesComoUsuario.Juridica;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Las entidades que pidieron sumarse y esperan al equipo. Aprobar a una da de alta su entidad
 * beneficiaria en Donaciones con el token de quien aprueba (el alta es solo de ADMIN) y habilita su
 * cuenta: el rol, la plantilla de quien responde por ella y el fin de la solicitud.
 */
@Service
@Profile("!maqueta")
public class SolicitudesDeEntidad {

  private static final String ROL = "ENTIDAD";
  private static final String PLANTILLA = "RESPONSABLE_ENTIDAD";
  private static final String SOLICITUD = "solicitudEntidad";
  private static final String SIN_NOMBRE = "Entidad sin razón social registrada";

  /** Lo que ve quien aprueba: qué entidad es y con qué cuenta pidió sumarse. */
  public record Pendiente(String cuentaId, String razonSocial, String correo) {}

  private final DonacionesComoUsuario donaciones;
  private final DirectorioDeCuentas cuentas;

  SolicitudesDeEntidad(DonacionesComoUsuario donaciones, DirectorioDeCuentas cuentas) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
  }

  public List<Pendiente> pendientes() {
    Map<UUID, String> nombres =
        donaciones.juridicas().stream().collect(toMap(Juridica::id, Juridica::razonSocial));
    return cuentas.solicitudesDeEntidad().stream()
        .map(solicitud -> pendiente(solicitud, nombres))
        .toList();
  }

  private static Pendiente pendiente(SolicitudDeEntidad solicitud, Map<UUID, String> nombres) {
    String razonSocial = nombres.getOrDefault(solicitud.personaId(), SIN_NOMBRE);
    return new Pendiente(solicitud.cuentaId(), razonSocial, solicitud.correo());
  }

  public void aprobar(String cuentaId) {
    SolicitudDeEntidad solicitud =
        cuentas.solicitudesDeEntidad().stream()
            .filter(s -> s.cuentaId().equals(cuentaId))
            .findFirst()
            .orElseThrow();
    donaciones.registrarEntidad(solicitud.personaId());
    cuentas.asignarRol(cuentaId, ROL);
    cuentas.asignarPlantilla(cuentaId, PLANTILLA);
    cuentas.quitarAtributo(cuentaId, SOLICITUD);
  }
}
