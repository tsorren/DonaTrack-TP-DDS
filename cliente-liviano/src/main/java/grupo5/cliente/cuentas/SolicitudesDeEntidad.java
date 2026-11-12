package grupo5.cliente.cuentas;

import static java.util.stream.Collectors.toMap;

import grupo5.cliente.puertos.DirectorioDeCuentas;
import grupo5.cliente.puertos.DirectorioDeCuentas.SolicitudDeEntidad;
import grupo5.cliente.puertos.DonacionesComoUsuario;
import grupo5.cliente.puertos.DonacionesComoUsuario.Juridica;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  /**
   * Lo que ve quien aprueba: qué entidad es, con qué cuenta pidió sumarse y su persona jurídica.
   */
  public record Pendiente(String cuentaId, String razonSocial, String correo, UUID personaId) {}

  private final DonacionesComoUsuario donaciones;
  private final DirectorioDeCuentas cuentas;

  SolicitudesDeEntidad(DonacionesComoUsuario donaciones, DirectorioDeCuentas cuentas) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
  }

  /**
   * Las que esperan, con la razón social de su jurídica: se leen solo las de las solicitudes, por
   * su identificador (R11-01), no todas las jurídicas para cruzarlas.
   */
  public List<Pendiente> pendientes() {
    List<SolicitudDeEntidad> solicitudes = cuentas.solicitudesDeEntidad();
    List<UUID> ids =
        solicitudes.stream()
            .map(SolicitudDeEntidad::personaId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<UUID, String> nombres =
        ids.isEmpty()
            ? Map.of()
            : donaciones.juridicas(ids).stream()
                .filter(j -> j.id() != null && j.razonSocial() != null)
                .collect(toMap(Juridica::id, Juridica::razonSocial, (a, b) -> a));
    return solicitudes.stream().map(solicitud -> pendiente(solicitud, nombres)).toList();
  }

  private static Pendiente pendiente(SolicitudDeEntidad solicitud, Map<UUID, String> nombres) {
    String razonSocial =
        solicitud.personaId() == null
            ? SIN_NOMBRE
            : nombres.getOrDefault(solicitud.personaId(), SIN_NOMBRE);
    return new Pendiente(
        solicitud.cuentaId(), razonSocial, solicitud.correo(), solicitud.personaId());
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
