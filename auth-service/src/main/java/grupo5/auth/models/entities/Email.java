package grupo5.auth.models.entities;

import java.util.Locale;
import java.util.regex.Pattern;

/** Email normalizado (trim + minúsculas) y con formato válido. */
public record Email(String valor) {

  private static final int MAX_LARGO = 254;
  private static final Pattern FORMATO =
      Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)+$");

  public Email {
    if (valor == null) {
      throw new DominioAuthException("Email inválido");
    }
    valor = valor.trim().toLowerCase(Locale.ROOT);
    if (valor.length() > MAX_LARGO || !FORMATO.matcher(valor).matches()) {
      throw new DominioAuthException("Email inválido");
    }
  }

  /** Evita filtrar el email en logs accidentales. */
  @Override
  public String toString() {
    return "Email[***]";
  }
}
