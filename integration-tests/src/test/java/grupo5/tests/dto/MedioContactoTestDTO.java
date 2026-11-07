package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// NON_NULL: un medio de correo no manda numeroTelefono, que la API de Donaciones no declara
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MedioContactoTestDTO(
    String tipo, boolean esPredeterminado, String direccionCorreo, String numeroTelefono) {
  public static MedioContactoTestDTO email(String email) {
    return new MedioContactoTestDTO("CORREO", true, email, null);
  }
}
