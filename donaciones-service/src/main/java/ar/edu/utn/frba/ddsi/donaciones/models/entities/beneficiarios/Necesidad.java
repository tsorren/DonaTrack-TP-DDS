package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.SubCategoria;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
  private SubCategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;
  protected List<DonacionAsignada> donacionesAsignadas = new ArrayList<>();

  public Necesidad(
          SubCategoria subcategoria,
          Integer cantidadNecesitada,
          String descripcion) {

    validarNecesidad(subcategoria, cantidadNecesitada, descripcion);

    this.subcategoria = subcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
  }
// constructor vacio para compatibilidad con test, borrar cuando se adapten los test al constructor validado
  public Necesidad() {

  }

  private void validarNecesidad(
          SubCategoria subcategoria,
          Integer cantidadNecesitada,
          String descripcion) {

    if (subcategoria == null) {
      throw new IllegalArgumentException(
              "La necesidad debe tener una subcategoría.");
    }

    if (cantidadNecesitada == null || cantidadNecesitada <= 0) {
      throw new IllegalArgumentException(
              "La cantidad necesitada debe ser mayor a cero.");
    }

    if (descripcion == null || descripcion.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La descripción de la necesidad no puede estar vacía.");
    }
  }

  public void asignarDonacion(DonacionAsignada donacionAsignada) {
    if (donacionAsignada == null) {
      throw new IllegalArgumentException(
              "La donación asignada no puede ser nula.");
    }

    if (this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException(
              "La donación ya fue asignada a esta necesidad.");
    }

    this.donacionesAsignadas.add(donacionAsignada);
  }

  // Añadir excepcion si no está el elemento en la lista
  public void quitarDonacion(DonacionAsignada donacionAsignada) {
    if (!this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException(
              "La donación no pertenece a esta necesidad.");
    }

    this.donacionesAsignadas.remove(donacionAsignada);
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadNecesitada;
  }

  public abstract Integer cantidadAcumulada();
}
