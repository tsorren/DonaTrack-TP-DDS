package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.OrigenEntidad;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.UUID;

/**
 * El registro de actividad de administración (R11-26, spec R11-p2-actividad). Los métodos que
 * registran se llaman después de que la acción salió bien y solo dejan una fila si la llamada trae
 * un token de ADMIN ({@link AdministradorQueActua}); con otro token no hacen nada.
 */
public interface IActividadService {

  /** Una página del registro, de la más reciente a la más vieja, con los filtros resueltos. */
  Pagina<ActividadDTO> listar(FiltroActividad filtro, ParametrosListado listado);

  /**
   * Anota una acción que pasó fuera de Donaciones (Keycloak o Logística): solo las cinco del sitio.
   * El autor es el de la llamada.
   */
  ActividadDTO anotar(AnotacionActividadDTO anotacion);

  /** Registra una acción de Donaciones con lo que tocó y su rótulo. */
  void registrar(AccionDeAdministracion accion, String objetoId, String detalle);

  /** Aprobar o descartar una propuesta, con la razón social de su organización. */
  void propuestaDecidida(UUID propuestaId, EstadoPropuesta estado);

  /** Un donante registrado: la razón social si es persona jurídica; de la humana, nada. */
  void donanteRegistrado(DonanteOutputDTO donante);

  /** Una organización aprobada (desde una solicitud) o registrada por la administración. */
  void organizacionCreada(EntidadBeneficiariaOutputDTO entidad, OrigenEntidad origen);
}
