package grupo5.incentivos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;

/**
 * Reintenta una lectura-modificación-escritura de un donante cuando otra escritura ganó la carrera
 * (optimistic locking con {@code @Version}). Cada intento tiene que volver a leer el agregado: el
 * que falló quedó con una versión vieja. Agotados los intentos, la excepción se propaga.
 */
final class ReintentoPorConcurrencia {

  static final int INTENTOS = 3;

  private static final Logger log = LoggerFactory.getLogger(ReintentoPorConcurrencia.class);

  private ReintentoPorConcurrencia() {}

  static void ejecutar(String descripcion, Runnable leerModificarGuardar) {
    for (int intento = 1; ; intento++) {
      try {
        leerModificarGuardar.run();
        return;
      } catch (ConcurrencyFailureException e) {
        if (intento >= INTENTOS) {
          throw e;
        }
        log.warn(
            "[CONCURRENCIA] {}: otra escritura ganó (intento {}/{}); se relee y se reintenta",
            descripcion,
            intento,
            INTENTOS);
      }
    }
  }
}
