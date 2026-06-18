package grupo5.donaciones.models.entities.donantes;

import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Archivo implements AggregateRoot {
  private final UUID id;
  private String path;
  private EstadoArchivo estado;

  public Archivo(String path) {
    this.id = UUID.randomUUID();
    this.path = path;
    this.estado = EstadoArchivo.PENDIENTE;
  }

  @Override
  public UUID getId() {
    return this.id;
  }
}
