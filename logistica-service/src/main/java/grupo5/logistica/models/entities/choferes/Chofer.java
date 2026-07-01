package grupo5.logistica.models.entities.choferes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Chofer implements AggregateRoot {
  private final UUID id;
  private final String nombre;
  private final String apellido;
  private String licencia;
  private String telefonoContacto;

  public Chofer(String nombre, String apellido, String licencia, String telefonoContacto) {
    validarDatos(nombre, apellido, licencia, telefonoContacto);
    this.id = UUID.randomUUID();
    this.nombre = nombre;
    this.apellido = apellido;
    this.licencia = licencia;
    this.telefonoContacto = telefonoContacto;
  }

  public void actualizarLicencia(String nuevaLicencia) {
    if (nuevaLicencia == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nuevaLicencia.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.licencia = nuevaLicencia;
  }

  public void actualizarTelefonoContacto(String nuevoTelefono) {
    if (nuevoTelefono == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nuevoTelefono.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    this.telefonoContacto = nuevoTelefono;
  }

  private void validarDatos(
      String nombre, String apellido, String licencia, String telefonoContacto) {
    if (nombre == null || apellido == null || licencia == null || telefonoContacto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (nombre.trim().isEmpty()
        || apellido.trim().isEmpty()
        || licencia.trim().isEmpty()
        || telefonoContacto.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
