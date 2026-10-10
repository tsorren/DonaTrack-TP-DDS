package grupo5.notificaciones.mothers;

import grupo5.notificaciones.models.entities.notificaciones.eventos.DonanteInactivo;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EntregaFallida;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.time.Month;

/**
 * Object Mother genérico para la jerarquía de {@code EventoNotificable} (Oleada 8). Un solo mother
 * para varios subtipos en vez de uno por clase, a criterio de esta oleada — la jerarquía tiene 8
 * subclases concretas y no todas necesitan fixtures dedicados todavía; se agregan métodos acá a
 * medida que un test los necesita, no de antemano. Nunca sufijo {@code *Test}.
 */
public final class EventoNotificableMother {

  public static final LocalDateTime FECHA_DE_PRUEBA = LocalDateTime.of(2026, Month.JULY, 2, 12, 0);

  private EventoNotificableMother() {}

  public static EntregaFallida entregaFallida(
      Persona donante, Persona beneficiario, Persona admin, boolean replanificable) {
    return new EntregaFallida(
        donante,
        beneficiario,
        admin,
        "10 cajas de leche",
        "Nadie respondió",
        replanificable,
        FECHA_DE_PRUEBA);
  }

  public static DonanteInactivo donanteInactivo(Persona donante) {
    return new DonanteInactivo(donante, 30, FECHA_DE_PRUEBA);
  }
}
