package grupo5.donaciones.models.entities.bienes;

@Getter
@Setter
public class Categoria {
  private String nombre;
  private Boolean conUso;
  private Boolean conVencimiento;
  private Unidad tipoUnidad;

  public Categoria(String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {

    validarCategoria(nombre, conUso, conVencimiento, tipoUnidad);

    this.nombre = nombre;
    this.conUso = conUso;
    this.conVencimiento = conVencimiento;
    this.tipoUnidad = tipoUnidad;
  }

  private void validarCategoria(
      String nombre, Boolean conUso, Boolean conVencimiento, Unidad tipoUnidad) {

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("La categoría debe tener un nombre.");
    }

    if (conUso == null) {
      throw new IllegalArgumentException("Debe indicarse si la categoría contempla uso.");
    }

    if (conVencimiento == null) {
      throw new IllegalArgumentException("Debe indicarse si la categoría contempla vencimiento.");
    }

    if (tipoUnidad == null) {
      throw new IllegalArgumentException("La categoría debe tener una unidad definida.");
    }
  }
}
