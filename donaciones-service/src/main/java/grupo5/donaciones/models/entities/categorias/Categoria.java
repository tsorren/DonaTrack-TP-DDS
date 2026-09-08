package grupo5.donaciones.models.entities.categorias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Categoria implements AggregateRoot {
  private final UUID id;
  private String nombre;
  private Boolean conUso;
  private Boolean conVencimiento;
  private Unidad tipoUnidad;

  public void actualizar(String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {
    validarCategoria(nombre, conUso, conVencimiento, tipoUnidad);
    this.nombre = nombre;
    this.conUso = conUso;
    this.conVencimiento = conVencimiento;
    this.tipoUnidad = tipoUnidad;
  }

  public Categoria(String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {
    this(UUID.randomUUID(), nombre, conUso, conVencimiento, tipoUnidad);
  }

  public Categoria(
      UUID id, String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {
    if (id == null) {
      throw new IllegalArgumentException("El id de la categoría no puede ser nulo");
    }

    validarCategoria(nombre, conUso, conVencimiento, tipoUnidad);

    this.id = id;
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
