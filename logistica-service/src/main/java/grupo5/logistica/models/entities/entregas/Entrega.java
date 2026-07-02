package grupo5.logistica.models.entities.entregas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Entrega implements AggregateRoot {
  private final UUID id;
  private UUID idRuta;
  private final UUID idDonacion;
  private final UUID idBeneficiaria;
  private final Direccion destino;
  private EstadoEntrega estadoActual;

  @Getter(AccessLevel.NONE)
  private final List<CambioEstadoEntrega> historialEstado;

  private LocalDateTime horaArribo;
  private LocalDateTime horaSalida;
  private String fotoRecepcionUrl;
  private final float pesoTotalKG;
  private final float volumenTotalM3;

  public Entrega(
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      Direccion destino,
      float pesoTotalKG,
      float volumenTotalM3) {
    if (idDonacion == null || idBeneficiaria == null || destino == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (pesoTotalKG <= 0 || volumenTotalM3 <= 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    this.id = UUID.randomUUID();
    this.idRuta = idRuta;
    this.idDonacion = idDonacion;
    this.idBeneficiaria = idBeneficiaria;
    this.destino = destino;
    this.estadoActual = EstadoEntrega.PENDIENTE;
    this.historialEstado = new ArrayList<>();
    this.pesoTotalKG = pesoTotalKG;
    this.volumenTotalM3 = volumenTotalM3;
  }

  public List<CambioEstadoEntrega> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
  }

  /**
   * Asocia esta entrega a la ruta que la incluirá en el reparto. Una entrega se crea al momento de
   * asignarse la donación a una entidad beneficiaria (aún sin ruta) y recién se vincula a una ruta
   * concreta cuando el {@code GeneradorDeRutas} la agrupa junto a otras entregas para un camión
   * determinado.
   */
  public void asignarRuta(UUID idRuta) {
    if (idRuta == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (this.idRuta != null) {
      throw new ValidationException(ErrorCatalog.ENTREGA_YA_ASIGNADA_A_RUTA);
    }
    this.idRuta = idRuta;
  }

  public void iniciarRuta(String chofer) {
    if (chofer == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (chofer.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (this.estadoActual != EstadoEntrega.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.EN_TRASLADO, chofer);
    this.horaSalida = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void confirmarEntrega(String entidad) {
    if (entidad == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (entidad.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.ENTREGADA, entidad);
    this.horaArribo = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void adjuntarFotoRecepcion(String fotoURL) {
    if (fotoURL == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (fotoURL.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (this.estadoActual != EstadoEntrega.ENTREGADA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    this.fotoRecepcionUrl = fotoURL;
  }

  public void negarEntrega(String entidad) {
    if (entidad == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (entidad.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.NO_RECIBIDA, entidad);

    mandarARevision("SISTEMA_LOGISTICA");
  }

  private void mandarARevision(String actor) {
    if (this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.REVISION, actor);
  }

  public void regresarAlDeposito(String administrador) {
    if (administrador == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (administrador.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (this.estadoActual != EstadoEntrega.REVISION
        && this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }
    actualizarEstado(EstadoEntrega.PENDIENTE, administrador);
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

  @Override
  public UUID getId() {
    return this.id;
  }
}
