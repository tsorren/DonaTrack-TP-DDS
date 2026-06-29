package grupo5.donaciones.models.entities.categorias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Categoria implements AggregateRoot {
  private final UUID id;
  private String nombre;
  private Boolean conUso;
  private Boolean conVencimiento;
  private Unidad tipoUnidad;

  public Categoria(String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {
    this.id = UUID.randomUUID();

    validarCategoria(nombre, conUso, conVencimiento, tipoUnidad);

    this.nombre = nombre;
    this.conUso = conUso;
    this.conVencimiento = conVencimiento;
    this.tipoUnidad = tipoUnidad;
  }

  private static void validarCategoria(
      String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.CATEGORIA_SIN_NOMBRE);
    }

    if (conUso == null) {
      throw new ValidationException(ErrorCatalog.CATEGORIA_SIN_USO_DEFINIDO);
    }

    if (conVencimiento == null) {
      throw new ValidationException(ErrorCatalog.CATEGORIA_SIN_VENCIMIENTO_DEFINIDO);
    }

    if (tipoUnidad == null) {
      throw new ValidationException(ErrorCatalog.CATEGORIA_SIN_UNIDAD);
    }
  }
}
