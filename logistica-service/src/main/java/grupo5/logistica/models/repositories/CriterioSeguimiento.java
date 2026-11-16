package grupo5.logistica.models.repositories;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

/**
 * Una página del seguimiento de entregas (R11-43): las de una organización ({@code idBeneficiaria})
 * o las de un donante ({@code donanteId}), con los estados y las donaciones pedidas. Un conjunto
 * vacío no filtra. {@code pagina} empieza en 0.
 *
 * <p>El orden ({@link #ORDEN}): primero las que todavía no llegaron (sin hora de arribo), después
 * las que llegaron; dentro de cada grupo, de la más reciente a la más vieja por la hora de arribo y
 * después por la de salida (sin hora, primero), y a igual hora, por id.
 */
public record CriterioSeguimiento(
    UUID idBeneficiaria,
    UUID donanteId,
    Set<EstadoEntrega> estados,
    Set<UUID> idsDonacion,
    int pagina,
    int tamanio) {

  public static final Comparator<Entrega> ORDEN =
      Comparator.comparing((Entrega e) -> e.getHoraArribo() != null)
          .thenComparing(
              Entrega::getHoraArribo,
              Comparator.nullsFirst(Comparator.<LocalDateTime>reverseOrder()))
          .thenComparing(
              Entrega::getHoraSalida,
              Comparator.nullsFirst(Comparator.<LocalDateTime>reverseOrder()))
          .thenComparing(Entrega::getId);

  public CriterioSeguimiento {
    estados = estados == null ? Set.of() : Set.copyOf(estados);
    idsDonacion = idsDonacion == null ? Set.of() : Set.copyOf(idsDonacion);
  }

  /** Cuántas se saltean antes de la página. */
  public long desplazamiento() {
    return (long) pagina * tamanio;
  }

  /** Si la entrega cumple el criterio (lo usan los repositorios que filtran en memoria). */
  public boolean cumple(Entrega e) {
    return (idBeneficiaria == null || idBeneficiaria.equals(e.getIdBeneficiaria()))
        && (donanteId == null || donanteId.equals(e.getDonanteId()))
        && (estados.isEmpty() || estados.contains(e.getEstadoActual()))
        && (idsDonacion.isEmpty() || idsDonacion.contains(e.getIdDonacion()));
  }
}
