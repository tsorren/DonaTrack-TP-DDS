package grupo5.donaciones.models.entities.personas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class Humana extends Persona {
  private String nombre;
  private String apellido;
  private Genero genero;
  private LocalDate fechaNacimiento;

  public Humana(String nombre, String apellido, LocalDate fechaNacimiento) {
    super();
    validarDatosHumanos(nombre, apellido, fechaNacimiento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
  }

  public Humana(String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    super();
    validarDatosHumanos(nombre, apellido, fechaNacimiento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
    this.genero = genero;
  }

  /** Constructor con id fijo, exclusivamente para seeding (para persona admin). */
  public Humana(UUID id, String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    super(id);
    validarDatosHumanos(nombre, apellido, fechaNacimiento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
    this.genero = genero;
  }

  public void actualizar(String nombre, String apellido, LocalDate fechaNacimiento, Genero genero) {
    validarDatosHumanos(nombre, apellido, fechaNacimiento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
    this.genero = genero;
  }

  private static void validarDatosHumanos(
      String nombre, String apellido, LocalDate fechaNacimiento) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.HUMANA_NOMBRE_VACIO);
    }
    if (apellido == null || apellido.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.HUMANA_APELLIDO_VACIO);
    }
    if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now(ZoneId.of("UTC")))) {
      throw new ValidationException(ErrorCatalog.HUMANA_FECHA_NACIMIENTO_FUTURA);
    }
  }

  @Override
  public TipoPersona getTipoPersona() {
    return TipoPersona.HUMANA;
  }

  @Override
  public String getNombreCompleto() {
    return this.nombre + " " + this.apellido;
  }

  @Override
  public void anonimizar() {
    this.nombre = Anonimizable.VALOR_STRING;
    this.apellido = Anonimizable.VALOR_STRING;
    this.genero = null;
    this.fechaNacimiento = null;
    this.actualizarDocumento(null, null);
    this.getMediosDeContacto().forEach(Anonimizable::anonimizar);
    if (this.getDireccion() != null) {
      this.actualizarDireccion(
          new Direccion(
              Anonimizable.VALOR_STRING,
              1,
              0,
              Anonimizable.VALOR_STRING,
              Anonimizable.VALOR_STRING,
              new Localidad(
                  Anonimizable.VALOR_STRING,
                  new Provincia(Anonimizable.VALOR_STRING, new Pais(Anonimizable.VALOR_STRING)))));
    }
  }
}
