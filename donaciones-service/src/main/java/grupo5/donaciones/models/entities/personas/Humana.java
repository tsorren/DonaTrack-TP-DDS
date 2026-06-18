package grupo5.donaciones.models.entities.personas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// final para indicar que ninguno más hereda (sino rompe el switch
public final class Humana extends Persona {
  private String nombre;
  private String apellido;
  private Genero genero;
  private LocalDate fechaNacimiento;

  public Humana(String nombre, String apellido, LocalDate fechaNacimiento) {
    validarDatosHumanos(nombre, apellido, fechaNacimiento);

    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
  }

  private static void validarDatosHumanos(
      String nombre, String apellido, LocalDate fechaNacimiento) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.HUMANA_NOMBRE_VACIO);
    }
    if (apellido == null || apellido.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.HUMANA_APELLIDO_VACIO);
    }
    if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now(ZoneId.systemDefault()))) {
      throw new ValidationException(ErrorCatalog.HUMANA_FECHA_NACIMIENTO_FUTURA);
    }
  }

  @Override
  public TipoPersona getTipoPersona() {
    return TipoPersona.HUMANA;
  }

  @Override
  public void anonimizar() {
    // El documento o DNI se ofusca o limpia

    this.nombre = Anonimizable.VALOR_STRING;
    this.apellido = Anonimizable.VALOR_STRING;
    this.genero = null;
    this.fechaNacimiento = null;
    this.setDocumento(null);
    if (this.getDireccion() != null) {
      this.getDireccion().anonimizar();
    }
  }
}
