package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.UUID;

public final class NecesidadMother {

  public static final LocalDate FECHA_INICIO = LocalDate.now(ZoneId.systemDefault()).minusDays(5);

  private NecesidadMother() {}

  public static NecesidadExtraordinaria extraordinaria(UUID subcategoriaId, int cantidad) {
    return new NecesidadExtraordinaria(
        subcategoriaId, cantidad, "Necesidad de prueba extraordinaria");
  }

  public static NecesidadExtraordinaria extraordinaria(
      UUID entidadId, UUID subcategoriaId, int cantidad) {
    NecesidadExtraordinaria nec =
        new NecesidadExtraordinaria(
            subcategoriaId, cantidad, "Necesidad extraordinaria para entidad");
    nec.asociarAEntidad(entidadId);
    return nec;
  }

  public static NecesidadRecurrente recurrenteSemanal(UUID subcategoriaId, int cantidad) {
    return new NecesidadRecurrente(
        subcategoriaId, cantidad, "Necesidad recurrente semanal", Period.ofWeeks(1), FECHA_INICIO);
  }

  public static NecesidadRecurrente recurrenteSemanal(
      UUID entidadId, UUID subcategoriaId, int cantidad) {
    NecesidadRecurrente nec =
        new NecesidadRecurrente(
            subcategoriaId,
            cantidad,
            "Necesidad recurrente semanal",
            Period.ofWeeks(1),
            FECHA_INICIO);
    nec.asociarAEntidad(entidadId);
    return nec;
  }

  public static NecesidadRecurrente recurrenteMensual(UUID subcategoriaId, int cantidad) {
    return new NecesidadRecurrente(
        subcategoriaId, cantidad, "Necesidad recurrente mensual", Period.ofMonths(1), FECHA_INICIO);
  }

  public static NecesidadRecurrente recurrenteConFecha(
      UUID subcategoriaId, int cantidad, LocalDate fechaInicio, Period periodicidad) {
    return new NecesidadRecurrente(
        subcategoriaId, cantidad, "Necesidad recurrente personalizada", periodicidad, fechaInicio);
  }
}
