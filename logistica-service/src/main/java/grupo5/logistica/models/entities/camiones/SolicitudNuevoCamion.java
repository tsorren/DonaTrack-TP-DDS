package grupo5.logistica.models.entities.camiones;

import java.util.List;

public record SolicitudNuevoCamion(
    String patente,
    Float capacidadVolumen,
    Float altura,
    Float capacidadKG,
    List<String> patentesExistentes) {

  public SolicitudNuevoCamion {
    patentesExistentes = patentesExistentes == null ? List.of() : List.copyOf(patentesExistentes);
  }
}
