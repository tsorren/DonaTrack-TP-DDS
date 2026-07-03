package grupo5.logistica.models.entities.rutas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
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
public class Ruta implements AggregateRoot {

  private final UUID id;
  private final LocalDate fecha;

  @Getter(AccessLevel.NONE)
  private final List<UUID> entregas;

  private final UUID choferId;
  private final UUID camionId;
  private EstadoRuta estado;
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
  }

  public void iniciarRuta() {
    if (this.estado != EstadoRuta.PENDIENTE || this.entregas.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    this.estado = EstadoRuta.EN_TRASLADO;
    this.horaInicioReal = LocalDateTime.now(ZoneId.of("UTC"));
    this.horaFinReal = null;
  }

  public void completarRuta() {
    if (this.estado != EstadoRuta.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }

    this.estado = EstadoRuta.COMPLETADA;
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
  }

  public List<UUID> obtenerEntregas() {
    return List.copyOf(this.entregas);
  }

  public List<UUID> getEntregas() {
    return obtenerEntregas();
  }

  public List<UUID> getEntregaIds() {
    return obtenerEntregas();
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
