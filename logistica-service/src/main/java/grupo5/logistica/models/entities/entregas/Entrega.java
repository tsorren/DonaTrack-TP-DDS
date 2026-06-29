package grupo5.logistica.models.entities.entregas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Entrega {
  private final UUID id;
  private final UUID idRuta;
  private final UUID idDonacion;
  private final UUID idBeneficiaria;
  private final Direccion destino;
  private EstadoEntrega estadoActual;
  private final List<CambioEstadoEntrega> historialEstado;
  private LocalDateTime horaArribo;
  private LocalDateTime horaSalida;
  private String fotoRecepcionUrl;
  private Boolean confirmacionEntrega;

  public Entrega(UUID idRuta, UUID idDonacion, UUID idBeneficiaria, Direccion destino) {
    this.id = UUID.randomUUID();
    this.idRuta = idRuta;
    this.idDonacion = idDonacion;
    this.idBeneficiaria = idBeneficiaria;
    this.destino = destino;
    this.estadoActual = EstadoEntrega.PENDIENTE;
    this.historialEstado = new ArrayList<>();
    this.confirmacionEntrega = false;

    registrarCambioEstado(null, EstadoEntrega.PENDIENTE, "ADMINISTRADOR");
  }

  public void iniciarRuta(String chofer) {
    if (this.estadoActual != EstadoEntrega.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.EN_TRASLADO, chofer);
    this.horaSalida = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void confirmarEntrega(String entidad) {
    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.ENTREGADA, entidad);
    this.confirmacionEntrega = true;
    this.horaArribo = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void adjuntarFotoRecepcion(String fotoURL) {
    if (this.estadoActual != EstadoEntrega.ENTREGADA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    this.fotoRecepcionUrl = fotoURL;
  }

  public void negarEntrega(String entidad) {
    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.NO_RECIBIDA, entidad);
    this.confirmacionEntrega = false;

    mandarARevision("SISTEMA_LOGISTICA");
  }

  private void mandarARevision(String actor) {
    if (this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.REVISION, actor);
  }

  public void regresarAlDeposito(String administrador) {
    if (this.estadoActual != EstadoEntrega.REVISION
        && this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.PENDIENTE, administrador);
    this.confirmacionEntrega = false;
    this.horaArribo = null;
    this.horaSalida = null;
  }

  private void actualizarEstado(EstadoEntrega nuevoEstado, String actor) {
    EstadoEntrega estadoAnterior = this.estadoActual;
    this.estadoActual = nuevoEstado;
    registrarCambioEstado(estadoAnterior, nuevoEstado, actor);
  }

  private void registrarCambioEstado(EstadoEntrega anterior, EstadoEntrega nuevo, String actor) {
    CambioEstadoEntrega cambio =
        new CambioEstadoEntrega(anterior, nuevo, LocalDateTime.now(ZoneId.of("UTC")), actor);
    this.historialEstado.add(cambio);
  }
}
