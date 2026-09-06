package grupo5.tests.builders;

import grupo5.tests.dto.NecesidadTestDTO;
import java.util.UUID;

public class NecesidadTestDataBuilder {
  private String tipo = "EXTRAORDINARIA";
  private UUID idEntidad;
  private UUID idSubcategoria;
  private Integer cantidadNecesitada = 10;
  private String descripcion = "Necesidad estándar";
  private String fechaInicio = "2026-06-18";
  private String fechaFin = null;

  public static NecesidadTestDataBuilder extraordinaria(
      UUID entidadId, UUID subcategoriaId, int cantidad) {
    NecesidadTestDataBuilder b = new NecesidadTestDataBuilder();
    b.idEntidad = entidadId;
    b.idSubcategoria = subcategoriaId;
    b.cantidadNecesitada = cantidad;
    return b;
  }

  public NecesidadTestDataBuilder conDescripcion(String desc) {
    this.descripcion = desc;
    return this;
  }

  public NecesidadTestDataBuilder conTipo(String tipo) {
    this.tipo = tipo;
    return this;
  }

  public NecesidadTestDTO build() {
    return new NecesidadTestDTO(
        tipo, idEntidad, idSubcategoria, cantidadNecesitada, descripcion, fechaInicio, fechaFin);
  }
}
