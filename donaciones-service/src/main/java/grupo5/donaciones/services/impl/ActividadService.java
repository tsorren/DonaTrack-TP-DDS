package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.OrigenEntidad;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.repositories.IActividadRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioActividad;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.AdministradorQueActua;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.ParametrosListado;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * El registro de actividad de administración (R11-26). El autor sale siempre del token de la
 * llamada ({@link AdministradorQueActua}); la hora, del {@link Clock} del servicio (Buenos Aires).
 * Sin datos de personas humanas: el detalle es un rótulo de una organización, un archivo, un camión
 * o una cantidad.
 */
@Service
public class ActividadService implements IActividadService {

  static final int MAXIMO_DE_AUTORES = 20;
  static final int LARGO_DE_CUENTA = 64;
  static final int LARGO_DE_PATENTE = 20;

  private final IActividadRepository repositorio;
  private final AdministradorQueActua administrador;
  private final Clock reloj;
  private final IPropuestasRepository propuestas;
  private final INecesidadesRepository necesidades;
  private final IPersonasRepository personas;

  public ActividadService(
      IActividadRepository repositorio,
      AdministradorQueActua administrador,
      Clock reloj,
      IPropuestasRepository propuestas,
      INecesidadesRepository necesidades,
      IPersonasRepository personas) {
    this.repositorio = repositorio;
    this.administrador = administrador;
    this.reloj = reloj;
    this.propuestas = propuestas;
    this.necesidades = necesidades;
    this.personas = personas;
  }

  @Override
  public Pagina<ActividadDTO> listar(FiltroActividad filtro, ParametrosListado listado) {
    FiltroActividad pedido = filtro == null ? FiltroActividad.ninguno() : filtro;
    LocalDate desde = pedido.desde();
    LocalDate hasta = pedido.hasta();
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw invalido();
    }
    Tramo<Actividad> tramo =
        repositorio.buscar(
            new CriterioActividad(
                autores(pedido.autor()),
                acciones(pedido.accion()),
                desde,
                hasta,
                listado.paginado()));
    return new Pagina<>(
        tramo.elementos().stream().map(ActividadDTO::de).toList(), Math.toIntExact(tramo.total()));
  }

  @Override
  public ActividadDTO anotar(AnotacionActividadDTO anotacion) {
    if (anotacion == null) {
      throw invalido();
    }
    AccionDeAdministracion accion = accion(anotacion.accion());
    if (!accion.laAnotaElSitio()) {
      throw invalido();
    }
    String objetoId = anotacion.objetoId() == null ? "" : anotacion.objetoId().strip();
    if (objetoId.isEmpty() || objetoId.length() > LARGO_DE_CUENTA) {
      throw invalido();
    }
    String detalle = anotacion.detalle() == null ? null : anotacion.detalle().strip();
    boolean conDetalle = detalle != null && !detalle.isEmpty();
    if (conDetalle && (accion.esSobreUnaCuenta() || detalle.length() > LARGO_DE_PATENTE)) {
      throw invalido();
    }
    // Sin token de ADMIN (solo con la seguridad apagada) no hay autor real: no se guarda nada.
    String autor = administrador.cuenta().orElseThrow(ActividadService::invalido);
    Actividad actividad =
        Actividad.nueva(ahora(), autor, accion, objetoId, conDetalle ? detalle : null);
    repositorio.guardar(actividad);
    return ActividadDTO.de(actividad);
  }

  @Override
  public void registrar(AccionDeAdministracion accion, String objetoId, String detalle) {
    administrador
        .cuenta()
        .ifPresent(
            autor ->
                repositorio.guardar(Actividad.nueva(ahora(), autor, accion, objetoId, detalle)));
  }

  @Override
  public void propuestaDecidida(UUID propuestaId, EstadoPropuesta estado) {
    if (administrador.cuenta().isEmpty()) {
      return;
    }
    AccionDeAdministracion accion =
        estado == EstadoPropuesta.APROBADA
            ? AccionDeAdministracion.PROPUESTA_APROBADA
            : AccionDeAdministracion.PROPUESTA_DESCARTADA;
    registrar(accion, propuestaId.toString(), organizacionDeLaPropuesta(propuestaId));
  }

  @Override
  public void donanteRegistrado(DonanteOutputDTO donante) {
    String razonSocial =
        donante.persona() instanceof JuridicaOutputDTO juridica ? juridica.razonSocial() : null;
    registrar(
        AccionDeAdministracion.DONANTE_REGISTRADO, donante.idDonante().toString(), razonSocial);
  }

  @Override
  public void organizacionCreada(EntidadBeneficiariaOutputDTO entidad, OrigenEntidad origen) {
    AccionDeAdministracion accion =
        origen == OrigenEntidad.SOLICITUD
            ? AccionDeAdministracion.ORGANIZACION_APROBADA
            : AccionDeAdministracion.ORGANIZACION_REGISTRADA;
    registrar(
        accion,
        entidad.id().toString(),
        entidad.juridica() == null ? null : entidad.juridica().razonSocial());
  }

  /** La razón social de la organización de la necesidad de la propuesta, si se encuentra. */
  private String organizacionDeLaPropuesta(UUID propuestaId) {
    return propuestas
        .findById(propuestaId)
        .flatMap(p -> necesidades.findById(p.getNecesidadQueSatisfaceId()))
        .map(Necesidad::getEntidadId)
        .flatMap(personas::findById)
        .filter(Juridica.class::isInstance)
        .map(persona -> ((Juridica) persona).getRazonSocial())
        .orElse(null);
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(reloj);
  }

  private static Set<String> autores(List<String> pedidos) {
    if (pedidos == null) {
      return Set.of();
    }
    Set<String> autores = new HashSet<>();
    for (String pedido : pedidos) {
      String autor = pedido == null ? "" : pedido.strip();
      if (autor.isEmpty() || autor.length() > LARGO_DE_CUENTA) {
        throw invalido();
      }
      autores.add(autor);
    }
    if (autores.size() > MAXIMO_DE_AUTORES) {
      throw invalido();
    }
    return autores;
  }

  private static Set<AccionDeAdministracion> acciones(List<String> pedidas) {
    if (pedidas == null || pedidas.isEmpty()) {
      return Set.of();
    }
    Set<AccionDeAdministracion> acciones = EnumSet.noneOf(AccionDeAdministracion.class);
    pedidas.stream().map(ActividadService::accion).forEach(acciones::add);
    return acciones;
  }

  private static AccionDeAdministracion accion(String pedida) {
    if (pedida == null || pedida.isBlank()) {
      throw invalido();
    }
    try {
      return AccionDeAdministracion.valueOf(pedida.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException noExiste) {
      throw invalido();
    }
  }

  private static ValidationException invalido() {
    return new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
  }
}
