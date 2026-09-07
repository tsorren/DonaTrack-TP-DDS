package grupo5.donaciones.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEntry {

  private final UUID id;
  private final String descripcion;
  private final Runnable accion;
  private int intentos;
  private final int maxIntentos;
  private LocalDateTime proximoIntento;

  private OutboxEntry(String descripcion, Runnable accion, int maxIntentos) {
    this.id = UUID.randomUUID();
    this.descripcion = descripcion;
    this.accion = accion;
    this.intentos = 0;
    this.maxIntentos = maxIntentos;
    this.proximoIntento = LocalDateTime.now();
  }

  public static OutboxEntry nuevo(String descripcion, Runnable accion) {
    return new OutboxEntry(descripcion, accion, 5);
  }

  public UUID getId() {
    return id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public Runnable getAccion() {
    return accion;
  }

  public int getIntentos() {
    return intentos;
  }

  public int getMaxIntentos() {
    return maxIntentos;
  }

  public LocalDateTime getProximoIntento() {
    return proximoIntento;
  }

  public void registrarFallo() {
    intentos++;
    long delaySegundos = 30L * (1L << intentos);
    proximoIntento = LocalDateTime.now().plusSeconds(delaySegundos);
  }

  public boolean agotoIntentos() {
    return intentos >= maxIntentos;
  }
}
