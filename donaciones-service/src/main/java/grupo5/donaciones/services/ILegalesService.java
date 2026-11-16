package grupo5.donaciones.services;

import grupo5.donaciones.dto.personas.LegalesDTO;
import java.util.UUID;

/** Constancia de la aceptación de los legales (HB-20, ADR 20261102). */
public interface ILegalesService {

  LegalesDTO consultar(UUID personaId);

  /** Idempotente: aceptar de nuevo la misma versión conserva la fecha de la primera vez. */
  LegalesDTO aceptar(UUID personaId, int version);
}
