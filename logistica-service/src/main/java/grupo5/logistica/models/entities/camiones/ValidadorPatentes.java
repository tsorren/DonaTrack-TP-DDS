package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.List;

public final class ValidadorPatentes {

  private static final String REGEX_PATENTE = "^[A-Z]{3}\\d{3}$|^[A-Z]{2}\\d{3}[A-Z]{2}$";

  private ValidadorPatentes() {}

  public static void validar(String patente, List<String> patentesExistentes) {
    validarFormato(patente);
    validarUnicidad(patente, patentesExistentes);
  }

  public static String normalizar(String patente) {
    if (patente == null || patente.isBlank()) {
      throw new ValidationException(ErrorCatalog.CAMION_PATENTE_VACIA);
    }
    return patente.strip().toUpperCase().replace("-", "").replace(" ", "");
  }

  private static void validarFormato(String patente) {
    if (!normalizar(patente).matches(REGEX_PATENTE)) {
      throw new ValidationException(ErrorCatalog.CAMION_PATENTE_FORMATO_INVALIDO);
    }
  }

  private static void validarUnicidad(String patente, List<String> patentesExistentes) {
    if (patentesExistentes == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    String patenteNormalizada = normalizar(patente);
    boolean duplicada =
        patentesExistentes.stream()
            .map(ValidadorPatentes::normalizar)
            .anyMatch(patenteNormalizada::equals);

    if (duplicada) {
      throw new BusinessStateException(ErrorCatalog.CAMION_PATENTE_DUPLICADA);
    }
  }
}
