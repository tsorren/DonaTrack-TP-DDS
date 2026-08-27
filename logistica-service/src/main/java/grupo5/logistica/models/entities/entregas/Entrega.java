package grupo5.logistica.models.entities.entregas;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.entregas.eventos.EventoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Entrega extends AgregadoConEventos<EventoEntrega> {

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
      UUID idDonacion,
      UUID idBeneficiaria,
      Direccion destino,
      float pesoTotalKG,
      float volumenTotalM3) {
    validarIdentificador(idDonacion);
    validarIdentificador(idBeneficiaria);
    validarDestino(destino);
    validarMagnitudPositiva(pesoTotalKG);
    validarMagnitudPositiva(volumenTotalM3);

    this.id = UUID.randomUUID();
    this.idRuta = null;
    this.idDonacion = idDonacion;
    this.idBeneficiaria = idBeneficiaria;
    this.destino = destino;
    this.estadoActual = EstadoEntrega.PENDIENTE;
    this.historialEstado = new ArrayList<>();
    this.pesoTotalKG = pesoTotalKG;
    this.volumenTotalM3 = volumenTotalM3;
  }

  public Entrega(
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      Direccion destino,
      float pesoTotalKG,
      float volumenTotalM3) {
    this(idDonacion, idBeneficiaria, destino, pesoTotalKG, volumenTotalM3);
    asignarRuta(idRuta);
  }

  public void asignarRuta(UUID idRuta) {
    validarIdentificador(idRuta);

    if (this.idRuta != null) {
      throw new ValidationException(ErrorCatalog.ENTREGA_YA_ASIGNADA_A_RUTA);
    }

    if (this.estadoActual != EstadoEntrega.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    this.idRuta = idRuta;
  }

  public void iniciarRuta(String chofer) {
    validarActor(chofer);

    if (this.estadoActual != EstadoEntrega.PENDIENTE) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.EN_TRASLADO, chofer);
    this.horaSalida = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public void confirmarEntrega(String entidad) {
    validarActor(entidad);

    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.ENTREGADA, entidad);
    this.horaArribo = LocalDateTime.now(ZoneId.of("UTC"));
    registrarEvento(new EntregaConfirmada(this.id, this.idDonacion, this.idRuta));
  }

  public void adjuntarFotoRecepcion(String fotoURL) {
    if (Objects.isNull(fotoURL) || fotoURL.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    if (this.estadoActual != EstadoEntrega.ENTREGADA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    this.fotoRecepcionUrl = fotoURL.trim();
  }

  public void negarEntrega(String entidad, String justificacion, boolean replanificable) {
    validarActor(entidad);

    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.NO_RECIBIDA, entidad);
    registrarEvento(new EntregaFallida(this.id, this.idDonacion, justificacion, replanificable));
    mandarARevision("SISTEMA_LOGISTICA");
  }

  private void mandarARevision(String actor) {
    validarActor(actor);

    if (this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.REVISION, actor);
  }

  public void regresarAlDeposito(String administrador) {
    validarActor(administrador);

    if (this.estadoActual != EstadoEntrega.REVISION
        && this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.PENDIENTE, administrador);
    this.horaArribo = null;
    this.horaSalida = null;
    this.idRuta = null;
  }

  public List<CambioEstadoEntrega> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
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

  private static void validarIdentificador(UUID id) {
    if (Objects.isNull(id)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  private static void validarDestino(Direccion destino) {
    if (Objects.isNull(destino)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  private static void validarMagnitudPositiva(float valor) {
    if (valor <= 0) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static void validarActor(String actor) {
    if (Objects.isNull(actor) || actor.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
