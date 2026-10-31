package grupo5.logistica.models.entities.entregas;

import java.util.List;

public record ConfirmacionRecepcion(Entrega entrega, String actor, List<String> fotosRecepcion)
    implements SolicitudTransicionEntrega {

  public ConfirmacionRecepcion {
    fotosRecepcion = fotosRecepcion != null ? List.copyOf(fotosRecepcion) : List.of();
  }

  public ConfirmacionRecepcion(Entrega entrega, String actor, String fotoRecepcionUrl) {
    this(
        entrega,
        actor,
        fotoRecepcionUrl != null && !fotoRecepcionUrl.isBlank()
            ? List.of(fotoRecepcionUrl)
            : List.of());
  }

  public String fotoRecepcionUrl() {
    return fotosRecepcion.isEmpty() ? null : fotosRecepcion.getFirst();
  }
}
