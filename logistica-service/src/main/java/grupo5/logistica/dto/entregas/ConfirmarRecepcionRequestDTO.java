package grupo5.logistica.dto.entregas;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public record ConfirmarRecepcionRequestDTO(
    @NotBlank String actor, List<String> fotosRecepcion, String fotoRecepcionUrl) {

  public ConfirmarRecepcionRequestDTO(String actor) {
    this(actor, null, null);
  }

  public ConfirmarRecepcionRequestDTO(String actor, List<String> fotosRecepcion) {
    this(actor, fotosRecepcion, null);
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
