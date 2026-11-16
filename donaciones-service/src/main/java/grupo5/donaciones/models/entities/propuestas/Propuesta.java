package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Propuesta extends AgregadoConEventos<PropuestaAprobada> {
  private UUID id;
  private UUID necesidadQueSatisfaceId;

  @Getter(AccessLevel.NONE)
  private List<PosibleFragmentacion> posiblesFragmentaciones;

  private EstadoPropuesta estado;
  private LocalDateTime fechaCreacion;

  /** Id de la cuenta que la aprobó o descartó ("SISTEMA" si no hubo cuenta); null si pendiente. */
  private String decididaPor;

  private LocalDateTime fechaDecision;

  /**
   * Por qué se descartó (R11-35), recortado; null si se aprobó, si no se anotó o si es de antes.
   */
  private String motivoDescarte;

  /** Qué algoritmos la sugirieron y en qué puesto (R11-24); vacía en las anteriores a V24. */
  @Getter(AccessLevel.NONE)
  private List<SugerenciaDeAlgoritmo> sugerencias = new ArrayList<>();

  public Propuesta() {
    this.id = UUID.randomUUID();
    this.posiblesFragmentaciones = new ArrayList<>();
    this.estado = EstadoPropuesta.PENDIENTE;
    this.fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
  }

  public Propuesta(UUID id) {
    this();
    this.id = id;
  }

  /** Constructor de reconstitución para el adaptador JPA: no valida ni registra eventos. */
  public Propuesta(
      UUID id,
      UUID necesidadQueSatisfaceId,
      EstadoPropuesta estado,
      LocalDateTime fechaCreacion,
      List<PosibleFragmentacion> posiblesFragmentaciones) {
    this(id, necesidadQueSatisfaceId, estado, fechaCreacion, posiblesFragmentaciones, null, null);
  }

  /** Constructor de reconstitución con la decisión ya tomada (quién y cuándo). */
  public Propuesta(
      UUID id,
      UUID necesidadQueSatisfaceId,
      EstadoPropuesta estado,
      LocalDateTime fechaCreacion,
      List<PosibleFragmentacion> posiblesFragmentaciones,
      String decididaPor,
      LocalDateTime fechaDecision) {
    this.id = id;
    this.necesidadQueSatisfaceId = necesidadQueSatisfaceId;
    this.estado = estado;
    this.fechaCreacion = fechaCreacion;
    this.posiblesFragmentaciones = new ArrayList<>(posiblesFragmentaciones);
    this.decididaPor = decididaPor;
    this.fechaDecision = fechaDecision;
  }

  public void asociarNecesidad(UUID necesidadId) {
    this.necesidadQueSatisfaceId = necesidadId;
  }

  public List<PosibleFragmentacion> getPosiblesFragmentaciones() {
    return List.copyOf(posiblesFragmentaciones);
  }

  public List<SugerenciaDeAlgoritmo> getSugerencias() {
    return List.copyOf(sugerencias);
  }

  /**
   * Registra que {@code sugerencia.algoritmo()} la sugirió. Un algoritmo aparece una sola vez: una
   * segunda sugerencia suya reemplaza a la primera.
   */
  public void registrarSugerencia(SugerenciaDeAlgoritmo sugerencia) {
    if (sugerencia == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    sugerencias.removeIf(s -> s.algoritmo() == sugerencia.algoritmo());
    sugerencias.add(sugerencia);
  }

  /** Lo hidrata el adaptador de persistencia, sin validar (R11-35). */
  public void reconstituirMotivoDescarte(String guardado) {
    this.motivoDescarte = guardado;
  }

  /** Lo hidrata el adaptador de persistencia, sin validar. */
  public void reconstituirSugerencias(List<SugerenciaDeAlgoritmo> guardadas) {
    this.sugerencias = new ArrayList<>(guardadas);
  }

  void setId(UUID id) {
    this.id = id;
  }

  void setEstado(EstadoPropuesta estado) {
    this.estado = estado;
  }

  void setFechaCreacion(LocalDateTime fechaCreacion) {
    this.fechaCreacion = fechaCreacion;
  }

  public void agregarFragmentacion(DonacionIndependiente donacion, int cantidad) {
    if (donacion == null) {
      throw new ValidationException(ErrorCatalog.PROPUESTA_FRAGMENTACION_DONACION_NULA);
    }
    if (cantidad <= 0) {
      throw new ValidationException(ErrorCatalog.PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA);
    }
    if (posiblesFragmentaciones == null) {
      posiblesFragmentaciones = new ArrayList<>();
    }
    PosibleFragmentacion f = new PosibleFragmentacion();
    f.setDonacionOriginalId(donacion.getId());
    f.setCantidadNecesaria(cantidad);
    posiblesFragmentaciones.add(f);
  }

  public boolean estaActiva() {
    return this.estado != null && this.estado != EstadoPropuesta.DESCARTADA;
  }

  public void aceptar(String actor) {
    if (this.estado != EstadoPropuesta.PENDIENTE) {
      throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }

    if (this.necesidadQueSatisfaceId == null) {
      throw new ValidationException(ErrorCatalog.PROPUESTA_CONFIRMAR_SIN_NECESIDAD);
    }

    String actorFinal = actorODefecto(actor);

    this.estado = EstadoPropuesta.APROBADA;
    registrarDecision(actorFinal);
    this.registrarEvento(
        new PropuestaAprobada(
            this.id,
            this.necesidadQueSatisfaceId,
            this.posiblesFragmentaciones != null
                ? List.copyOf(this.posiblesFragmentaciones)
                : List.of(),
            actorFinal));
  }

  public void rechazar() {
    rechazar(null);
  }

  public void rechazar(String actor) {
    if (this.estado != EstadoPropuesta.PENDIENTE) {
      throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoPropuesta.DESCARTADA;
    registrarDecision(actorODefecto(actor));
  }

  /** Descarta y guarda por qué (R11-35): el texto recortado; vacío o nulo, sin motivo. */
  public void rechazar(String actor, String motivo) {
    rechazar(actor);
    this.motivoDescarte = motivo == null || motivo.isBlank() ? null : motivo.strip();
  }

  private void registrarDecision(String actor) {
    this.decididaPor = actor;
    this.fechaDecision = LocalDateTime.now(ZoneId.systemDefault());
  }

  private static String actorODefecto(String actor) {
    return (actor == null || actor.isBlank()) ? "SISTEMA" : actor;
  }
}
