package grupo5.logistica.models.entities.solicitudes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Representa un ciclo de ejecución del {@code PlanificadorDeEntregas}: un lote de, como máximo, 100
 * donaciones (restricción de negocio) que debe planificarse en rutas. Actúa como registro de
 * auditoría persistente de cada corrida, permitiendo saber qué se planificó, cuándo, con qué
 * resultado y cuántas veces se reintentó ante fallas operativas.
 */
@Getter
public class SolicitudPlanificacion implements AggregateRoot {

  /** Máximo de donaciones que el negocio permite procesar en una única solicitud/lote. */
  public static final int MAX_DONACIONES_POR_LOTE = 100;

  private final UUID id;
  private final LocalDate fecha;
  private EstadoSolicitud estado;
  private final Integer cantidadDonaciones;
  private final String callbackUrl;

  @Getter(AccessLevel.NONE)
  private final List<UUID> entregaIds;

  @Getter(AccessLevel.NONE)
  private final List<UUID> camionIds;

  @Getter(AccessLevel.NONE)
  private final List<UUID> choferIds;

  @Getter(AccessLevel.NONE)
  private final List<UUID> rutasGeneradas;

  @Getter(AccessLevel.NONE)
  private final List<UUID> entregasNoAsignadas;

  private Integer intentosFallidos;
  private String motivoError;

  public SolicitudPlanificacion(
      LocalDate fecha,
      List<UUID> entregaIds,
      List<UUID> camionIds,
      List<UUID> choferIds,
      String callbackUrl) {
    this(UUID.randomUUID(), fecha, entregaIds, camionIds, choferIds, callbackUrl);
  }

  public SolicitudPlanificacion(
      UUID id,
      LocalDate fecha,
      List<UUID> entregaIds,
      List<UUID> camionIds,
      List<UUID> choferIds,
      String callbackUrl) {
    if (id == null
        || fecha == null
        || entregaIds == null
        || camionIds == null
        || choferIds == null
        || callbackUrl == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (callbackUrl.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_CALLBACK_VACIO);
    }
    validarIdentificadores(entregaIds);
    validarIdentificadores(camionIds);
    validarIdentificadores(choferIds);
    if (entregaIds.isEmpty() || camionIds.isEmpty() || choferIds.isEmpty()) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_CANTIDAD_INVALIDA);
    }
    if (entregaIds.size() > MAX_DONACIONES_POR_LOTE) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_LOTE_EXCEDIDO);
    }

    this.id = id;
    this.fecha = fecha;
    this.entregaIds = List.copyOf(entregaIds);
    this.camionIds = List.copyOf(camionIds);
    this.choferIds = List.copyOf(choferIds);
    this.cantidadDonaciones = entregaIds.size();
    this.callbackUrl = callbackUrl;
    this.estado = EstadoSolicitud.PENDIENTE;
    this.rutasGeneradas = new ArrayList<>();
    this.entregasNoAsignadas = new ArrayList<>(entregaIds);
    this.intentosFallidos = 0;
    this.motivoError = null;
  }

  public List<UUID> getEntregaIds() {
    return List.copyOf(entregaIds);
  }

  public List<UUID> getCamionIds() {
    return List.copyOf(camionIds);
  }

  public List<UUID> getChoferIds() {
    return List.copyOf(choferIds);
  }

  public List<UUID> getRutasGeneradas() {
    return List.copyOf(this.rutasGeneradas);
  }

  public List<UUID> getEntregasNoAsignadas() {
    return List.copyOf(entregasNoAsignadas);
  }

  /**
   * Registra el resultado exitoso de la ejecución: las rutas generadas para este lote y marca la
   * solicitud como {@link EstadoSolicitud#PROCESADA}.
   */
  public void procesarResultados(List<UUID> rutasGeneradas, List<UUID> entregasAsignadas) {
    if (rutasGeneradas == null || entregasAsignadas == null) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_RESULTADO_NULO);
    }
    if (this.estado != EstadoSolicitud.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_TRANSICION_INVALIDA);
    }
    validarIdentificadores(rutasGeneradas);
    validarIdentificadores(entregasAsignadas);
    if (rutasGeneradas.isEmpty()
        || entregasAsignadas.isEmpty()
        || !new HashSet<>(entregaIds).containsAll(entregasAsignadas)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.rutasGeneradas.clear();
    this.rutasGeneradas.addAll(rutasGeneradas);
    this.entregasNoAsignadas.clear();
    this.entregasNoAsignadas.addAll(
        entregaIds.stream().filter(id -> !entregasAsignadas.contains(id)).toList());
    this.estado =
        entregasNoAsignadas.isEmpty() ? EstadoSolicitud.PROCESADA : EstadoSolicitud.PARCIAL;
    this.motivoError = null;
  }

  /**
   * Marca la solicitud como fallida ante un error operativo (por ejemplo, una excepción durante la
   * generación de rutas), incrementando el contador de intentos fallidos para su posterior
   * auditoría o reintento.
   */
  public void marcarError(String motivo) {
    if (motivo == null || motivo.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (this.estado == EstadoSolicitud.PROCESADA || this.estado == EstadoSolicitud.PARCIAL) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoSolicitud.ERROR;
    this.motivoError = motivo;
    this.intentosFallidos = this.intentosFallidos + 1;
  }

  /**
   * Habilita un nuevo intento de planificación sobre esta misma solicitud, volviendo su estado a
   * {@link EstadoSolicitud#PENDIENTE}. Solo puede reintentarse una solicitud que haya finalizado en
   * error.
   */
  public void reintentar() {
    if (this.estado != EstadoSolicitud.ERROR) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_TRANSICION_INVALIDA);
    }
    this.estado = EstadoSolicitud.PENDIENTE;
  }

  private static void validarIdentificadores(List<UUID> ids) {
    Set<UUID> idsUnicos = new HashSet<>();
    if (ids.stream().anyMatch(id -> id == null || !idsUnicos.add(id))) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
