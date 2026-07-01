package grupo5.logistica.models.entities.chofer;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Chofer implements AggregateRoot {
  private final UUID id;
  private final String nombre;
  private final String apellido;
  private final String licencia;
  private final String telefonoContacto;

  public Chofer(String nombre, String apellido, String licencia, String telefonoContacto) {
    validarTexto(nombre);
    validarTexto(apellido);
    validarTexto(licencia);
    validarTexto(telefonoContacto);

    this.id = UUID.randomUUID();
    this.nombre = nombre.trim();
    this.apellido = apellido.trim();
    this.licencia = licencia.trim();
    this.telefonoContacto = telefonoContacto.trim();
  }

  private void validarTexto(String valor) {
    if (Objects.isNull(valor) || valor.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
