package grupo5.donaciones.models.entities.donantes;

import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Archivo implements AggregateRoot {
  private final UUID id;
  private String path;
  private EstadoArchivo estado;

  public Archivo(String path) {
    this.id = UUID.randomUUID();
    this.path = path;
    this.estado = EstadoArchivo.PENDIENTE;
  }

  public void marcarComoProcesando() {
    this.estado = EstadoArchivo.PROCESANDO;
  }

  public void marcarComoProcesado() {
    this.estado = EstadoArchivo.PROCESADO;
  }

  public void marcarComoError() {
    this.estado = EstadoArchivo.ERROR;
  }

  @Override
  public UUID getId() {
    return this.id;
  }
}
