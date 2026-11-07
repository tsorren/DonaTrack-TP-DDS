package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;

public record AdjuntarFotoRecepcionRequestDTO(
    String fotoRecepcionUrl, List<String> fotosRecepcion) {

  public AdjuntarFotoRecepcionRequestDTO(String fotoRecepcionUrl) {
    this(fotoRecepcionUrl, null);
  }

  public AdjuntarFotoRecepcionRequestDTO(List<String> fotosRecepcion) {
    this(null, fotosRecepcion);
  }

  @AssertTrue(message = "Debe proporcionar al menos una foto de recepción válida")
  @JsonIgnore
  public boolean isFotoValidaPresente() {
    boolean urlValida = fotoRecepcionUrl != null && !fotoRecepcionUrl.isBlank();
    boolean listaValida =
        fotosRecepcion != null
            && !fotosRecepcion.isEmpty()
            && fotosRecepcion.stream().allMatch(f -> f != null && !f.isBlank());
    if (fotoRecepcionUrl != null && fotoRecepcionUrl.isBlank()) {
      return false;
    }
    if (fotosRecepcion != null
        && (fotosRecepcion.isEmpty()
            || fotosRecepcion.stream().anyMatch(f -> f == null || f.isBlank()))) {
      return false;
    }
    return urlValida || listaValida;
  }

  public List<String> obtenerFotos() {
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
