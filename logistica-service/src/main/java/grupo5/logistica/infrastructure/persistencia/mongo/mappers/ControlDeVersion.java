package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;

/** Bloqueo optimista al sincronizar un agregado sobre el documento que ya está guardado. */
final class ControlDeVersion {

  private ControlDeVersion() {}

  /**
   * Falla si el agregado se leyó con una versión distinta de la que tiene el documento guardado.
   */
  static void verificar(
      Long versionDelAgregado, Long versionGuardada, Class<?> documento, UUID id) {
    if (versionDelAgregado != null && !versionDelAgregado.equals(versionGuardada)) {
      throw new OptimisticLockingFailureException(
          "%s %s cambió desde que se leyó (versión %d, guardada %s)"
              .formatted(documento.getSimpleName(), id, versionDelAgregado, versionGuardada));
    }
  }

  /** Un documento nuevo va sin versión para que Spring Data lo inserte en vez de actualizarlo. */
  static Long versionInicial(Long versionDelAgregado) {
    return versionDelAgregado != null && versionDelAgregado > 0L ? versionDelAgregado : null;
  }
}
