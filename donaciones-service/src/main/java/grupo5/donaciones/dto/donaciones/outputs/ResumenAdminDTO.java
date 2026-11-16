package grupo5.donaciones.dto.donaciones.outputs;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Las cifras del inicio del admin (R11-01), cada una contada en la base. Sin las entidades
 * pendientes de aprobación: vienen de Auth.
 *
 * @param donacionesPorEstado cuántas donaciones independientes hay en cada estado; los 8 siempre
 *     presentes
 * @param entregadasEnElMes las que pasaron a ENTREGADA en el mes en curso
 * @param porVencer las que siguen en depósito con algún bien que vence en los próximos días
 * @param donantes cuántos donantes hay y cuántos están activos
 * @param entidadesActivas cuántas entidades beneficiarias están activas
 * @param propuestasPendientes cuántas propuestas de asignación esperan respuesta
 * @param calculadoEn momento del cálculo
 */
public record ResumenAdminDTO(
    Map<String, Long> donacionesPorEstado,
    EntregadasEnElMes entregadasEnElMes,
    PorVencer porVencer,
    Donantes donantes,
    long entidadesActivas,
    long propuestasPendientes,
    OffsetDateTime calculadoEn) {

  /** {@code mes} en formato {@code AAAA-MM}. */
  public record EntregadasEnElMes(String mes, long cantidad) {}

  /** {@code proxima} es la de vencimiento más cercano, o {@code null} si no hay ninguna. */
  public record PorVencer(int dias, long cantidad, Proxima proxima) {}

  public record Proxima(UUID id, String descripcion, LocalDate fechaVencimiento) {}

  public record Donantes(long total, long activos) {}
}
