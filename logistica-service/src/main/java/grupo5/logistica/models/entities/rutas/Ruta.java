package grupo5.logistica.models.entities.rutas;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.eventos.EventoRuta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaIniciada;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Ruta extends AgregadoConEventos<EventoRuta> {

  private final UUID id;
  private final LocalDate fecha;

  @Getter(AccessLevel.NONE)
  private final List<UUID> entregas;

  private final UUID choferId;
  private final UUID camionId;
  private EstadoRuta estado;

  @Getter(AccessLevel.NONE)
  private final List<CambioEstadoRuta> historialEstado;

  private LocalDateTime horaInicioReal;
  private LocalDateTime horaFinReal;

  public Ruta(LocalDate fecha, UUID choferId, UUID camionId) {
    validarFecha(fecha);
    validarIdentificador(choferId);
    validarIdentificador(camionId);

    this.id = UUID.randomUUID();
    this.fecha = fecha;
    this.choferId = choferId;
    this.camionId = camionId;
    this.estado = EstadoRuta.PENDIENTE;
    this.entregas = new ArrayList<>();
    this.historialEstado = new ArrayList<>();
  }

  public void iniciarRuta() {
    if (this.estado != EstadoRuta.PENDIENTE || this.entregas.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoRuta.EN_TRASLADO);
    this.horaInicioReal = LocalDateTime.now(ZoneId.of("UTC"));
    this.horaFinReal = null;
    registrarEvento(
        new EventoRutaIniciada(this.id, this.camionId, this.entregas, this.horaInicioReal));
  }

  public void completarRuta() {
    if (this.estado != EstadoRuta.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoRuta.COMPLETADA);
    this.horaFinReal = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void agregarEntrega(UUID entregaId) {
    validarIdentificador(entregaId);

    if (this.estado != EstadoRuta.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    if (this.entregas.contains(entregaId)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    this.entregas.add(entregaId);
    registrarEvento(new EventoRutaAsignada(this.id, entregaId));
  }

  public List<UUID> getEntregaIds() {
    return List.copyOf(this.entregas);
  }

  public boolean tieneSeguimientoDisponible() {
    return this.estado != EstadoRuta.PENDIENTE;
  }

  public List<CambioEstadoRuta> getHistorialEstado() {
    return List.copyOf(historialEstado);
  }

  private void actualizarEstado(EstadoRuta estadoNuevo) {
    EstadoRuta estadoAnterior = this.estado;
    this.estado = estadoNuevo;
    this.historialEstado.add(
        new CambioEstadoRuta(estadoAnterior, estadoNuevo, LocalDateTime.now(ZoneId.of("UTC"))));
  }

  private static void validarFecha(LocalDate fecha) {
    if (Objects.isNull(fecha)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  private static void validarIdentificador(UUID id) {
    if (Objects.isNull(id)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }
}
