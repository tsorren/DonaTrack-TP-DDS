package grupo5.cliente.puertos;

import java.util.UUID;

/**
 * Las altas y bajas de personas y donantes que el sitio hace en Donaciones con su token técnico.
 */
public interface PersonasDeDonaciones {

  /** Da de alta la persona y devuelve su id. */
  UUID crear(PersonaNueva persona);

  void registrarDonante(UUID personaId);

  /**
   * Deja la constancia de que la persona aceptó los legales vigentes (HB-20): el registro los pide
   * con una casilla obligatoria.
   */
  void aceptarLegalesVigentes(UUID personaId);

  void eliminar(UUID personaId);
}
