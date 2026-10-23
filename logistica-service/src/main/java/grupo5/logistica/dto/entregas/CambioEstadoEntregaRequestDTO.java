package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public record CambioEstadoEntregaRequestDTO(
    @NotNull EstadoEntrega estado,
    @NotBlank String actor,
    String justificacion, // Utilizado cuando el estado es NO_RECIBIDA
    Boolean replanificable, // Utilizado cuando el estado es NO_RECIBIDA
    List<String> fotosRecepcion, // Obligatorio cuando el estado es ENTREGADA
    String fotoRecepcionUrl // Compatibilidad con envío de una única URL
    ) {

  public CambioEstadoEntregaRequestDTO(
      EstadoEntrega estado, String actor, String justificacion, Boolean replanificable) {
    this(estado, actor, justificacion, replanificable, null, null);
  }

  public CambioEstadoEntregaRequestDTO(
      EstadoEntrega estado,
      String actor,
      String justificacion,
      Boolean replanificable,
      List<String> fotosRecepcion) {
    this(estado, actor, justificacion, replanificable, fotosRecepcion, null);
  }

  public List<String> obtenerFotosRecepcion() {
    List<String> resultado = new ArrayList<>();
    if (fotosRecepcion != null) {
      resultado.addAll(fotosRecepcion);
    }
    if (fotoRecepcionUrl != null) {
      resultado.add(fotoRecepcionUrl);
    }
    return resultado;
  }
}
