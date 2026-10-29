package grupo5.auth.models.entities;

import java.util.Locale;
import java.util.Objects;

/** Identidad en un proveedor externo (p. ej. GOOGLE + subject del proveedor). */
public record IdentidadSocial(String proveedor, String subjectExterno) {

  public IdentidadSocial {
    Objects.requireNonNull(proveedor, "proveedor");
    Objects.requireNonNull(subjectExterno, "subjectExterno");
    if (proveedor.isBlank() || subjectExterno.isBlank()) {
      throw new DominioAuthException("Identidad social incompleta");
    }
    proveedor = proveedor.trim().toUpperCase(Locale.ROOT);
  }
}
