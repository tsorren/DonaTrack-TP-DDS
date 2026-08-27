package grupo5.tests.dto;

public record MedioContactoTestDTO(
    String tipo, boolean esPredeterminado, String direccionCorreo, String numeroTelefono) {
  public static MedioContactoTestDTO email(String email) {
    return new MedioContactoTestDTO("CORREO", true, email, null);
  }
}
