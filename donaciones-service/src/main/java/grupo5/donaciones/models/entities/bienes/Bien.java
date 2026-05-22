package grupo5.donaciones.models.entities.bienes;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bien {
  private String descripcion;
  private String fotoUrl;
  private LocalDate fechaVencimiento;
  private Estado estado;
  private SubCategoria subcategoria;

  public Bien(
      String descripcion,
      String fotoUrl,
      LocalDate fechaVencimiento,
      Estado estado,
      SubCategoria subcategoria) {
    this.descripcion = descripcion;
    this.fotoUrl = fotoUrl;
    this.fechaVencimiento = fechaVencimiento;
    this.estado = estado;
    this.subcategoria = subcategoria;

    validarReglasDeNegocio();
  }

  private void validarReglasDeNegocio() {
    // 1. Validar que la descripción no sea vacía
    if (this.descripcion == null || this.descripcion.trim().isEmpty()) {
      throw new IllegalArgumentException("La descripción del bien no puede estar vacía.");
    }
    // 2. Validar que la subcategoría exista
    if (this.subcategoria == null) {
      throw new IllegalArgumentException("El bien debe tener una subcategoría asignada.");
    }

    Categoria categoria = this.subcategoria.getCategoria();
    if (categoria != null) {
      // 3. Validar bienes cuya categoría requiera vencimiento
      if (Boolean.TRUE.equals(categoria.getConVencimiento()) && this.fechaVencimiento == null) {
        throw new IllegalArgumentException(
            "La categoría exige una fecha de vencimiento (es perecedero).");
      }

      // 4. Validar bienes cuya categoría no requiera vencimiento
      if (Boolean.FALSE.equals(categoria.getConVencimiento()) && this.fechaVencimiento != null) {
        throw new IllegalArgumentException(
            "La categoría NO permite registrar una fecha de vencimiento.");
      }

      // 5. Validar bienes cuya categoría requiera uso (estado)
      if (Boolean.TRUE.equals(categoria.getConUso()) && this.estado == null) {
        throw new IllegalArgumentException(
            "La categoría exige especificar el estado del bien (ej: NUEVO, USADO).");
      }
    }
  }

  // metodos
  public boolean estaVencido() {
    if (this.fechaVencimiento == null) return false;
    return this.fechaVencimiento.isBefore(LocalDate.now());
  }
}
