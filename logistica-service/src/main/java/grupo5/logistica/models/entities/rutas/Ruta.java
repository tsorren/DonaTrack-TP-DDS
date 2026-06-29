package grupo5.logistica.models.entities.rutas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Ruta implements AggregateRoot {
  private final UUID id;
  private final LocalDate fecha;
  private final List<UUID> entregas;
  private final UUID choferId;
  private final UUID camionId;
  private EstadoRuta estado;
  private LocalDateTime horaInicioReal;
  private LocalDateTime horaFinReal;

  public Ruta(LocalDate fecha, UUID chofer, UUID camion) {
    this.id = UUID.randomUUID();
    this.fecha = fecha;
    this.choferId = chofer;
    this.camionId = camion;
    this.estado = EstadoRuta.PENDIENTE;
    this.entregas = new ArrayList<>();
  }

  public void iniciarRuta() {
    if (this.estado != EstadoRuta.PENDIENTE) {
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

  public void agregarEntrega(UUID entrega) {
    if (this.estado != EstadoRuta.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_RUTA_TRANSICION_INVALIDA);
    }
    this.entregas.add(entrega);
  }

  public List<UUID> obtenerEntregas() {
    return this.entregas;
  }
}
