package grupo5.logistica.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.repositories.ICamionRepository;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPatentes {

  private static final String REGEX_PATENTE = "^[A-Z]{3}\\d{3}$|^[A-Z]{2}\\d{3}[A-Z]{2}$";

  private final ICamionRepository camionRepository;

  public ValidadorPatentes(ICamionRepository camionRepository) {
    this.camionRepository = camionRepository;
  }

  public void validar(String patente) {
    validarFormato(patente);
    validarUnicidad(patente);
  }

  private static void validarFormato(String patente) {
    if (patente == null || patente.isBlank()) {
      throw new ValidationException(ErrorCatalog.CAMION_PATENTE_VACIA);
    }

    String normalizada = patente.strip().toUpperCase().replace("-", "").replace(" ", "");
    if (!normalizada.matches(REGEX_PATENTE)) {
      throw new ValidationException(ErrorCatalog.CAMION_PATENTE_FORMATO_INVALIDO);
    }
  }

  private void validarUnicidad(String patente) {

    boolean duplicada =
        camionRepository.findAll().stream().anyMatch(c -> c.getPatente().equalsIgnoreCase(patente));

    if (duplicada) {
      throw new BusinessStateException(ErrorCatalog.CAMION_PATENTE_DUPLICADA);
    }
  }
}
