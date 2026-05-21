package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import lombok.Data;

@Data
public class Direccion {
  private String calle;
  private Integer altura;
  private Integer piso;
  private String departamento;
  private String zona;
  private String localidad;

  public Direccion(
          String calle,
          Integer altura,
          Integer piso,
          String departamento,
          String zona,
          String localidad) {

    validarDireccion(calle, altura, zona, localidad);

    this.calle = calle;
    this.altura = altura;
    this.piso = piso;
    this.departamento = departamento;
    this.zona = zona;
    this.localidad = localidad;
  }

  private void validarDireccion(
          String calle,
          Integer altura,
          String zona,
          String localidad) {

    if (calle == null || calle.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La dirección debe tener una calle.");
    }

    if (altura == null || altura <= 0) {
      throw new IllegalArgumentException(
              "La altura debe ser mayor a cero.");
    }

    if (zona == null || zona.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La dirección debe tener una zona.");
    }

    if (localidad == null || localidad.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La dirección debe tener una localidad.");
    }
  }
}
