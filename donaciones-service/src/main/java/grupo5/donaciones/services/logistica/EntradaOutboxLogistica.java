package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

/**
 * Un intento de envío de un pedido de entrega a un proveedor. Guarda solo datos (nada de código),
 * para que la implementación del outbox pueda pasar a una tabla sin cambiar el broker.
 */
@Getter
public class EntradaOutboxLogistica {

  private final UUID id;
  private final UUID solicitudId;
  private final String proveedorId;
  private final DatosEntregaLogistica datos;
  private final String traceId;
  private final int maxIntentos;
  private final TipoEntradaOutbox tipo;
  private int intentos;
  private LocalDateTime proximoIntento;
  private EstadoEntradaOutbox estado;

  private EntradaOutboxLogistica(
      UUID solicitudId,
      String proveedorId,
      DatosEntregaLogistica datos,
      String traceId,
      int maxIntentos,
      LocalDateTime disponibleDesde,
      TipoEntradaOutbox tipo) {
    this.id = UUID.randomUUID();
    this.solicitudId = solicitudId;
    this.proveedorId = proveedorId;
    this.datos = datos;
    this.traceId = traceId;
    this.maxIntentos = maxIntentos;
    this.intentos = 0;
    this.proximoIntento = disponibleDesde;
    this.estado = EstadoEntradaOutbox.PENDIENTE;
    this.tipo = tipo;
  }

  private EntradaOutboxLogistica(RegistroEntradaOutbox registro) {
    this.id = registro.id();
    this.solicitudId = registro.solicitudId();
    this.proveedorId = registro.proveedorId();
    this.datos = registro.datos();
    this.traceId = registro.traceId();
    this.maxIntentos = registro.maxIntentos();
    this.intentos = registro.intentos();
    this.proximoIntento = registro.proximoIntento();
    this.estado = registro.estado();
    this.tipo = registro.tipo() != null ? registro.tipo() : TipoEntradaOutbox.SOLICITUD;
  }

  /** Reconstituye una entrada guardada (adaptador JPA del outbox): no valida. */
  public static EntradaOutboxLogistica reconstituir(RegistroEntradaOutbox registro) {
    return new EntradaOutboxLogistica(registro);
  }

  /**
   * @param disponibleDesde momento a partir del cual el relay puede enviarla (ahora, o más tarde si
   *     es el primer intento de una nueva ronda)
   */
  public static EntradaOutboxLogistica nueva(
      UUID solicitudId,
      String proveedorId,
      DatosEntregaLogistica datos,
      String traceId,
      int maxIntentos,
      LocalDateTime disponibleDesde) {
    return new EntradaOutboxLogistica(
        solicitudId,
        proveedorId,
        datos,
        traceId,
        maxIntentos,
        disponibleDesde,
        TipoEntradaOutbox.SOLICITUD);
  }

  /**
   * Cancelación de la entrega de una solicitud (HB-19). {@code datos} lleva la donación; el
   * proveedor al que se manda se resuelve al enviarla, con la solicitud ya {@code ENVIADA}.
   */
  public static EntradaOutboxLogistica cancelacion(
      UUID solicitudId,
      String proveedorId,
      DatosEntregaLogistica datos,
      String traceId,
      int maxIntentos,
      LocalDateTime disponibleDesde) {
    return new EntradaOutboxLogistica(
        solicitudId,
        proveedorId,
        datos,
        traceId,
        maxIntentos,
        disponibleDesde,
        TipoEntradaOutbox.CANCELACION);
  }

  public boolean esCancelacion() {
    return tipo == TipoEntradaOutbox.CANCELACION;
  }

  /** La cancelación espera a que la solicitud quede {@code ENVIADA}: no gasta un intento. */
  public void posponer(LocalDateTime hasta) {
    this.proximoIntento = hasta;
  }

  public boolean estaPendiente() {
    return estado == EstadoEntradaOutbox.PENDIENTE;
  }

  public boolean estaListaPara(LocalDateTime ahora) {
    return estaPendiente() && !proximoIntento.isAfter(ahora);
  }

  public void marcarPublicada() {
    this.estado = EstadoEntradaOutbox.PUBLICADO;
  }

  public void marcarFallida() {
    this.estado = EstadoEntradaOutbox.FALLIDO;
  }

  /**
   * Registra un intento incierto. Si se agotaron los intentos la entrada queda {@code FALLIDO}; si
   * no, se reprograma con backoff exponencial ({@code base · 2^intentos}), igual que el outbox
   * existente del servicio.
   */
  public void registrarIntentoIncierto(LocalDateTime ahora, long backoffBaseSegundos) {
    intentos++;
    if (intentos >= maxIntentos) {
      marcarFallida();
      return;
    }
    proximoIntento = ahora.plusSeconds(backoffBaseSegundos * (1L << intentos));
  }
}
