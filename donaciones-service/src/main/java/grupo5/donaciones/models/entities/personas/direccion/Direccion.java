package grupo5.donaciones.models.entities.personas.direccion;

import grupo5.donaciones.models.privacidad.Anonimizable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Direccion implements Anonimizable {
  private Localidad localidad;
  private String calle;
  private Integer altura;
  private Integer piso;
  private String departamento;
  private String codigoPostal;

  public Direccion(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      Localidad localidad) {

    validarDireccion(calle, altura, codigoPostal, localidad);

    this.calle = calle;
    this.altura = altura;
    this.piso = piso;
    this.departamento = departamento;
    this.codigoPostal = codigoPostal;
    this.localidad = localidad;
  }

  private static void validarDireccion(
      String calle, Integer altura, String codigoPostal, Localidad localidad) {

    if (calle == null || calle.trim().isEmpty()) {
      throw new IllegalArgumentException("La dirección debe tener una calle.");
    }

    if (altura == null || altura <= 0) {
      throw new IllegalArgumentException("La altura debe ser mayor a cero.");
    }

    if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
      throw new IllegalArgumentException("La dirección debe tener un código postal.");
    }

    if (localidad == null) {
      throw new IllegalArgumentException("La dirección debe tener una localidad.");
    }
  }

  @Override
  public void anonimizar() {
    this.calle = Anonimizable.VALOR_STRING;
    this.altura = 0;
    this.codigoPostal = Anonimizable.VALOR_STRING;
    this.departamento = Anonimizable.VALOR_STRING;
    this.piso = 0;
    this.localidad.anonimizar();
  }
}
