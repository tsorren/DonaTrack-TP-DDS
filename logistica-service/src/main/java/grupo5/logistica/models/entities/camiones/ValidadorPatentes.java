package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.List;

public final class ValidadorPatentes {

  private static final String REGEX_PATENTE = "^[A-Z]{3}\\d{3}$|^[A-Z]{2}\\d{3}[A-Z]{2}$";

  private final ICamionRepository camionRepository;

  public ValidadorPatentes(ICamionRepository camionRepository) {
    if (camionRepository == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.camionRepository = camionRepository;
  }

  public void validar(String patente) {
    validarFormato(patente);
    if (camionRepository.findByPatente(normalizar(patente)).isPresent()) {
      throw new BusinessStateException(ErrorCatalog.CAMION_PATENTE_DUPLICADA);
    }
  }

  public static boolean validar(String patente, List<String> patentesExistentes) {
    validarFormato(patente);
    return esUnica(patente, patentesExistentes);
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

  private static boolean esUnica(String patente, List<String> patentesExistentes) {
    if (patentesExistentes == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    String patenteNormalizada = normalizar(patente);
    return patentesExistentes.stream()
        .map(ValidadorPatentes::normalizar)
        .noneMatch(patenteNormalizada::equals);
  }
}
