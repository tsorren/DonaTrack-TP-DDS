package grupo5.donaciones.models.repositories.listados;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Donaciones en depósito con bienes por vencer: cuántas son y la de vencimiento más cercano ({@code
 * proximaId} y {@code proximaVence} son nulos si no hay ninguna).
 */
public record PorVencer(long cantidad, UUID proximaId, LocalDate proximaVence) {

  public static PorVencer ninguna() {
    return new PorVencer(0, null, null);
  }
}
