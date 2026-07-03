package grupo5.logistica.models.entities.planificacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class SolicitudPlanificacion implements AggregateRoot {
  private final UUID id;
  private final UUID correlationId;
  private final LocalDate fecha;
  private EstadoSolicitud estado;
  private final Integer cantidadDonaciones;
  private final String callbackUrl;

  @Getter(AccessLevel.NONE)
  private final List<UUID> rutasGeneradas;

  private Integer intentosFallidos;
  private String motivoError;

  public SolicitudPlanificacion(LocalDate fecha, Integer cantidadDonaciones, String callbackUrl) {
    validarFecha(fecha);
    validarCantidadDonaciones(cantidadDonaciones);
    validarCallbackUrl(callbackUrl);

    this.id = UUID.randomUUID();
    this.correlationId = UUID.randomUUID();
    this.fecha = fecha;
    this.estado = EstadoSolicitud.PENDIENTE;
    this.cantidadDonaciones = cantidadDonaciones;
    this.callbackUrl = callbackUrl.trim();
    this.rutasGeneradas = new ArrayList<>();
    this.intentosFallidos = 0;
  }

  public void procesarResultados(List<UUID> rutasGeneradas) {
    if (Objects.isNull(rutasGeneradas)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    this.rutasGeneradas.clear();
    this.rutasGeneradas.addAll(rutasGeneradas);
    this.estado = EstadoSolicitud.PROCESADA;
    this.motivoError = null;
  }

  public void marcarError(String motivo) {
    if (Objects.isNull(motivo) || motivo.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    this.estado = EstadoSolicitud.ERROR;
    this.motivoError = motivo.trim();
    this.intentosFallidos++;
  }

  public void reintentar() {
    if (this.estado != EstadoSolicitud.ERROR) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    this.estado = EstadoSolicitud.PENDIENTE;
    this.motivoError = null;
  }

  public List<UUID> getRutasGeneradas() {
    return List.copyOf(this.rutasGeneradas);
  }

  private static void validarFecha(LocalDate fecha) {
    if (Objects.isNull(fecha)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  private static void validarCantidadDonaciones(Integer cantidadDonaciones) {
    if (Objects.isNull(cantidadDonaciones) || cantidadDonaciones <= 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static void validarCallbackUrl(String callbackUrl) {
    if (Objects.isNull(callbackUrl) || callbackUrl.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
