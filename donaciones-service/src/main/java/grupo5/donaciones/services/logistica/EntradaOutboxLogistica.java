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
  private int intentos;
  private LocalDateTime proximoIntento;
  private EstadoEntradaOutbox estado;

  private EntradaOutboxLogistica(
      UUID solicitudId,
      String proveedorId,
      DatosEntregaLogistica datos,
      String traceId,
      int maxIntentos,
      LocalDateTime ahora) {
    this.id = UUID.randomUUID();
    this.solicitudId = solicitudId;
    this.proveedorId = proveedorId;
    this.datos = datos;
    this.traceId = traceId;
    this.maxIntentos = maxIntentos;
    this.intentos = 0;
    this.proximoIntento = ahora;
    this.estado = EstadoEntradaOutbox.PENDIENTE;
  }

  public static EntradaOutboxLogistica nueva(
      UUID solicitudId,
      String proveedorId,
      DatosEntregaLogistica datos,
      String traceId,
      int maxIntentos,
      LocalDateTime ahora) {
    return new EntradaOutboxLogistica(solicitudId, proveedorId, datos, traceId, maxIntentos, ahora);
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
