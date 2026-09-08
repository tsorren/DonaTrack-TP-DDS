package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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

  public Propuesta() {
    this.id = UUID.randomUUID();
    this.posiblesFragmentaciones = new ArrayList<>();
    this.estado = EstadoPropuesta.PENDIENTE;
    this.fechaCreacion = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public Propuesta(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("El id de la propuesta no puede ser nulo");
    }
    this.id = id;
    this.posiblesFragmentaciones = new ArrayList<>();
    this.estado = EstadoPropuesta.PENDIENTE;
    this.fechaCreacion = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void asociarNecesidad(UUID necesidadId) {
    this.necesidadQueSatisfaceId = necesidadId;
  }

  public List<PosibleFragmentacion> getPosiblesFragmentaciones() {
    return Collections.unmodifiableList(posiblesFragmentaciones);
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

    String actorFinal = (actor == null || actor.isBlank()) ? "SISTEMA" : actor;

    this.estado = EstadoPropuesta.APROBADA;
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
    if (this.estado != EstadoPropuesta.PENDIENTE) {
      throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoPropuesta.DESCARTADA;
  }
}
