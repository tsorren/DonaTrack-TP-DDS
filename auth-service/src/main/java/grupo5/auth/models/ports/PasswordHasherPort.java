package grupo5.auth.models.ports;

public interface PasswordHasherPort {
  String codificar(String contraseniaPlana);

  boolean coincide(String contraseniaPlana, String hash);
}
