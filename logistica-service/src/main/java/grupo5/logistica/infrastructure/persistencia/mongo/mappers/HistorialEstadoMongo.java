package grupo5.logistica.infrastructure.persistencia.mongo.mappers; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CambioEstadoSubdocument;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Historial de estados embebido, con la misma forma en camiones, choferes, rutas y entregas. */
final class HistorialEstadoMongo {

  private HistorialEstadoMongo() {}

  /** El historial es append-only: conserva lo guardado y agrega solo los cambios nuevos. */
  static <C> List<CambioEstadoSubdocument> conCambiosNuevos(
      List<CambioEstadoSubdocument> guardado,
      List<C> delDominio,
      Function<C, CambioEstadoSubdocument> aSubdocumento) {
    List<CambioEstadoSubdocument> historial = new ArrayList<>(sinNulos(guardado));
    delDominio.stream().skip(historial.size()).map(aSubdocumento).forEach(historial::add);
    return historial;
  }

  static <C> List<C> aDominio(
      List<CambioEstadoSubdocument> historial, Function<CambioEstadoSubdocument, C> aCambio) {
    return sinNulos(historial).stream().map(aCambio).toList();
  }

  static CambioEstadoSubdocument cambio(Enum<?> anterior, Enum<?> nuevo, LocalDateTime cuando) {
    return cambio(anterior, nuevo, cuando, null);
  }

  static CambioEstadoSubdocument cambio(
      Enum<?> anterior, Enum<?> nuevo, LocalDateTime cuando, String actor) {
    return CambioEstadoSubdocument.builder()
        .estadoAnterior(nombre(anterior))
        .estadoNuevo(nombre(nuevo))
        .timestamp(FechasUtc.aInstant(cuando))
        .actor(actor)
        .build();
  }

  static <E extends Enum<E>> E estado(Class<E> tipo, String nombre) {
    return nombre != null ? Enum.valueOf(tipo, nombre) : null;
  }

  private static String nombre(Enum<?> estado) {
    return estado != null ? estado.name() : null;
  }

  private static List<CambioEstadoSubdocument> sinNulos(List<CambioEstadoSubdocument> historial) {
    return Objects.requireNonNullElse(historial, List.of());
  }
}
