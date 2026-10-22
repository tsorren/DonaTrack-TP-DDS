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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

  @Getter(AccessLevel.NONE)
  private final List<String> fotosRecepcion;

  private final float pesoTotalKG;
  private final float volumenTotalM3;

  /** Para Optimistic Locking futuro — gestionado por el adaptador JPA, no por el dominio. */
  private Long version;

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
    this.fotosRecepcion = new ArrayList<>();
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

  /**
   * Constructor de reconstitución para el adaptador de persistencia — hidrata el objeto desde la DB
   * sin ejecutar validaciones de negocio ni generar un nuevo UUID.
   */
  @SuppressWarnings("java:S107")
  public Entrega(
      UUID id,
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      Direccion destino,
      EstadoEntrega estadoActual,
      List<CambioEstadoEntrega> historialEstado,
      LocalDateTime horaArribo,
      LocalDateTime horaSalida,
      List<String> fotosRecepcion,
      float pesoTotalKG,
      float volumenTotalM3,
      Long version) {
    this.id = id;
    this.idRuta = idRuta;
    this.idDonacion = idDonacion;
    this.idBeneficiaria = idBeneficiaria;
    this.destino = destino;
    this.estadoActual = estadoActual;
    this.historialEstado =
        historialEstado != null ? new ArrayList<>(historialEstado) : new ArrayList<>();
    this.horaArribo = horaArribo;
    this.horaSalida = horaSalida;
    this.fotosRecepcion = normalizarFotosReconstituidas(fotosRecepcion);
    this.pesoTotalKG = pesoTotalKG;
    this.volumenTotalM3 = volumenTotalM3;
    this.version = version;
  }

  /**
   * Sobrecarga de reconstitución compatible con una única referencia fotográfica.
   */
  @SuppressWarnings("java:S107")
  public Entrega(
      UUID id,
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      Direccion destino,
      EstadoEntrega estadoActual,
      List<CambioEstadoEntrega> historialEstado,
      LocalDateTime horaArribo,
      LocalDateTime horaSalida,
      String fotoRecepcionUrl,
      float pesoTotalKG,
      float volumenTotalM3,
      Long version) {
    this(
        id,
        idRuta,
        idDonacion,
        idBeneficiaria,
        destino,
        estadoActual,
        historialEstado,
        horaArribo,
        horaSalida,
        fotoRecepcionUrl != null && !fotoRecepcionUrl.isBlank()
            ? List.of(fotoRecepcionUrl)
            : List.of(),
        pesoTotalKG,
        volumenTotalM3,
        version);
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

  public void confirmarEntrega(String entidad, String fotoRecepcionUrl) {
    confirmarEntrega(
        entidad, fotoRecepcionUrl != null ? List.of(fotoRecepcionUrl) : null);
  }

  public void confirmarEntrega(String entidad, List<String> fotosRecepcion) {
    validarActor(entidad);
    List<String> fotosNormalizadas = normalizarFotosObligatorias(fotosRecepcion);

    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    this.fotosRecepcion.clear();
    this.fotosRecepcion.addAll(fotosNormalizadas);
    actualizarEstado(EstadoEntrega.ENTREGADA, entidad);
    this.horaArribo = LocalDateTime.now(ZoneId.of("UTC"));
    registrarEvento(new EntregaConfirmada(this.id, this.idDonacion, this.idRuta));
  }

  public void adjuntarFotoRecepcion(String fotoURL) {
    adjuntarFotosRecepcion(fotoURL != null ? List.of(fotoURL) : null);
  }

  public void adjuntarFotosRecepcion(List<String> nuevasFotos) {
    List<String> fotosNormalizadas = normalizarFotosObligatorias(nuevasFotos);

    if (this.estadoActual != EstadoEntrega.ENTREGADA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    for (String foto : fotosNormalizadas) {
      if (!this.fotosRecepcion.contains(foto)) {
        this.fotosRecepcion.add(foto);
      }
    }
  }

  public void negarEntrega(String entidad, String justificacion, boolean replanificable) {
    validarActor(entidad);
    validarJustificacion(justificacion);

    if (this.estadoActual != EstadoEntrega.EN_TRASLADO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.NO_RECIBIDA, entidad);
    registrarEvento(
        new EntregaFallida(this.id, this.idDonacion, justificacion.trim(), replanificable));
  }

  public void mandarARevision(String administrador) {
    validarActor(administrador);

    if (this.estadoActual != EstadoEntrega.NO_RECIBIDA) {
      throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
    }

    actualizarEstado(EstadoEntrega.REVISION, administrador);
  }

  public void regresarAlDeposito(String administrador) {
    validarActor(administrador);

    if (this.estadoActual != EstadoEntrega.REVISION) {
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

  public List<String> getFotosRecepcion() {
    return List.copyOf(this.fotosRecepcion);
  }

  public String getFotoRecepcionUrl() {
    return this.fotosRecepcion.isEmpty() ? null : this.fotosRecepcion.getFirst();
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

  private static void validarJustificacion(String justificacion) {
    if (Objects.isNull(justificacion) || justificacion.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static List<String> normalizarFotosObligatorias(List<String> fotos) {
    if (fotos == null || fotos.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    Set<String> unicas = new LinkedHashSet<>();
    for (String foto : fotos) {
      if (foto == null || foto.isBlank()) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }
      unicas.add(foto.trim());
    }
    return List.copyOf(unicas);
  }

  private static List<String> normalizarFotosReconstituidas(List<String> fotos) {
    if (fotos == null || fotos.isEmpty()) {
      return new ArrayList<>();
    }
    Set<String> unicas = new LinkedHashSet<>();
    for (String foto : fotos) {
      if (foto != null && !foto.isBlank()) {
        unicas.add(foto.trim());
      }
    }
    return new ArrayList<>(unicas);
  }
}
